# Firestore Seed (InvSmart)

This folder contains a one-command Firestore seed for baseline collections:
- users
- teams
- team_members
- team_invites
- products
- orders (+ subcollection items)
- payments

## Prerequisites

- Node.js 18+
- A Firebase service account JSON file with Firestore access

## Setup

1. Open terminal in this folder.
2. Install dependencies:

```bash
npm install
```

3. Provide service account path.

PowerShell:

```powershell
$env:SERVICE_ACCOUNT_PATH="C:/path/to/service-account.json"
```

Optional project id override:

```powershell
$env:FIREBASE_PROJECT_ID="your-project-id"
```

## Seed data

```bash
npm run seed
```

## Remove seeded data

```bash
npm run seed:clean
```

## Notes

- Documents are written with deterministic IDs, so running seed multiple times updates/merges baseline docs.
- `orders/ord_0001/items/*` is seeded as a subcollection.
