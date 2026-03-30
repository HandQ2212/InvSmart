package com.invsmart.app

import android.app.Application
import com.cloudinary.android.MediaManager
import com.invsmart.app.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class InvSmartApp : Application(){
	override fun onCreate() {
		super.onCreate()

		if (!isCloudinaryInitialized()) {
			val config = mapOf(
				"cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
				"secure" to true
			)
			MediaManager.init(this, config)
		}
	}

	private fun isCloudinaryInitialized(): Boolean {
		return runCatching { MediaManager.get() }.isSuccess
	}
}