/* eslint-disable no-console */
const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const cleanMode = process.argv.includes("--clean");

function resolveServiceAccountPath() {
  const envPath = process.env.SERVICE_ACCOUNT_PATH;
  if (!envPath) return null;
  const resolved = path.resolve(envPath);
  if (!fs.existsSync(resolved)) {
    throw new Error(`SERVICE_ACCOUNT_PATH not found: ${resolved}`);
  }
  return resolved;
}

function resolveProjectId() {
  if (process.env.FIREBASE_PROJECT_ID) {
    return process.env.FIREBASE_PROJECT_ID;
  }

  const googleServicesPath = path.resolve(__dirname, "../../app/google-services.json");
  if (fs.existsSync(googleServicesPath)) {
    const googleServices = JSON.parse(fs.readFileSync(googleServicesPath, "utf8"));
    return googleServices?.project_info?.project_id || undefined;
  }

  return undefined;
}

function initializeFirebase() {
  const serviceAccountPath = resolveServiceAccountPath();
  const projectId = resolveProjectId();

  if (serviceAccountPath) {
    const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf8"));
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId: projectId || serviceAccount.project_id,
    });
    return;
  }

  admin.initializeApp({
    projectId,
  });
}

const now = admin.firestore.Timestamp.now();

const seedAuthUsers = [
  {
    uid: "manager_master",
    email: "master@invsmart.dev",
    password: "InvSmart@123",
    displayName: "Master Manager",
  },
  {
    uid: "manager_ops",
    email: "manager@invsmart.dev",
    password: "InvSmart@123",
    displayName: "Operation Manager",
  },
  {
    uid: "staff_active",
    email: "staff1@invsmart.dev",
    password: "InvSmart@123",
    displayName: "Active Staff",
  },
  {
    uid: "staff_pending",
    email: "staff2@invsmart.dev",
    password: "InvSmart@123",
    displayName: "Pending Staff",
  },
];

const seedDocs = {
  users: {
    manager_master: {
      uid: "manager_master",
      username: "master.manager",
      usernameLower: "master.manager",
      email: "master@invsmart.dev",
      role: "master",
      roleGlobal: "master",
      isMaster: true,
      status: "active",
      accessStatus: "active",
      createdAt: now,
      updatedAt: now,
    },
    manager_ops: {
      uid: "manager_ops",
      username: "operation.manager",
      usernameLower: "operation.manager",
      email: "manager@invsmart.dev",
      role: "manager",
      roleGlobal: "manager",
      isMaster: false,
      status: "active",
      accessStatus: "active",
      createdAt: now,
      updatedAt: now,
    },
    staff_active: {
      uid: "staff_active",
      username: "active.staff",
      usernameLower: "active.staff",
      email: "staff1@invsmart.dev",
      role: "staff",
      roleGlobal: "staff",
      isMaster: false,
      status: "active",
      accessStatus: "active",
      createdAt: now,
      updatedAt: now,
    },
    staff_pending: {
      uid: "staff_pending",
      username: "pending.staff",
      usernameLower: "pending.staff",
      email: "staff2@invsmart.dev",
      role: "staff",
      roleGlobal: "staff",
      isMaster: false,
      status: "active",
      accessStatus: "active",
      createdAt: now,
      updatedAt: now,
    },
  },

  products: {
    p_apple_01: {
      productId: "p_apple_01",
      teamId: "",
      sku: "APL-001",
      name: "Apple",
      category: "Fruit",
      unit: "kg",
      price: 120000,
      stockQty: 40,
      imageUrl: null,
      isActive: true,
      createdBy: "manager_master",
      createdAt: now,
      updatedAt: now,
    },
    p_orange_01: {
      productId: "p_orange_01",
      teamId: "",
      sku: "ORG-001",
      name: "Orange",
      category: "Fruit",
      unit: "kg",
      price: 90000,
      stockQty: 35,
      imageUrl: null,
      isActive: true,
      createdBy: "manager_master",
      createdAt: now,
      updatedAt: now,
    },
  },

  orders: {
    ord_0001: {
      orderId: "ord_0001",
      staffUid: "staff_active",
      staffName: "Active Staff",
      orderType: "sale",
      status: "paid",
      totalAmount: 210000,
      currency: "VND",
      paymentMethod: "qr",
      paidAt: now,
      items: [
        {
          productId: "p_apple_01",
          productName: "Apple",
          quantity: 1,
          priceAtTime: 120000,
        },
        {
          productId: "p_orange_01",
          productName: "Orange",
          quantity: 1,
          priceAtTime: 90000,
        },
      ],
      totalQuantity: 2,
      createdAt: now,
      updatedAt: now,
    },
  },

  payments: {
    pay_0001: {
      paymentId: "pay_0001",
      orderId: "ord_0001",
      amount: 210000,
      method: "qr",
      status: "success",
      gatewayTxnId: null,
      failureReason: null,
      createdBy: "staff_active",
      createdAt: now,
      updatedAt: now,
    },
  },
};

const orderItems = {};

async function writeSeed(db) {
  const batch = db.batch();

  for (const [collection, docs] of Object.entries(seedDocs)) {
    for (const [docId, docData] of Object.entries(docs)) {
      const ref = db.collection(collection).doc(docId);
      batch.set(ref, docData, { merge: true });
    }
  }

  await batch.commit();

  for (const [orderId, items] of Object.entries(orderItems)) {
    const subBatch = db.batch();
    for (const [itemId, itemData] of Object.entries(items)) {
      const ref = db.collection("orders").doc(orderId).collection("items").doc(itemId);
      subBatch.set(ref, itemData, { merge: true });
    }
    await subBatch.commit();
  }
}

async function ensureAuthUsers() {
  const auth = admin.auth();

  for (const user of seedAuthUsers) {
    try {
      await auth.getUser(user.uid);
      await auth.updateUser(user.uid, {
        email: user.email,
        password: user.password,
        displayName: user.displayName,
        emailVerified: true,
        disabled: false,
      });
      console.log(`Updated auth user: ${user.email}`);
    } catch (error) {
      if (error && error.code === "auth/user-not-found") {
        await auth.createUser({
          uid: user.uid,
          email: user.email,
          password: user.password,
          displayName: user.displayName,
          emailVerified: true,
          disabled: false,
        });
        console.log(`Created auth user: ${user.email}`);
      } else {
        throw error;
      }
    }
  }
}

async function cleanSeed(db) {
  const docIdsByCollection = Object.fromEntries(
    Object.entries(seedDocs).map(([collection, docs]) => [collection, Object.keys(docs)])
  );

  for (const [collection, docIds] of Object.entries(docIdsByCollection)) {
    const batch = db.batch();
    for (const docId of docIds) {
      batch.delete(db.collection(collection).doc(docId));
    }
    await batch.commit();
  }

  for (const [orderId, items] of Object.entries(orderItems)) {
    const batch = db.batch();
    for (const itemId of Object.keys(items)) {
      batch.delete(db.collection("orders").doc(orderId).collection("items").doc(itemId));
    }
    await batch.commit();
  }
}

async function main() {
  initializeFirebase();
  const db = admin.firestore();

  if (cleanMode) {
    await cleanSeed(db);
    console.log("Seeded documents were removed.");
    return;
  }

  await ensureAuthUsers();
  await writeSeed(db);
  console.log("Firestore seed completed successfully.");
}

main()
  .catch((error) => {
    console.error("Seed failed:", error);
    process.exitCode = 1;
  })
  .finally(async () => {
    if (admin.apps.length > 0) {
      await admin.app().delete();
    }
  });
