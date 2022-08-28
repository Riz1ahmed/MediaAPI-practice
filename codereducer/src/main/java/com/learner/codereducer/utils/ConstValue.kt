package com.learner.codereducer.utils

import android.content.Context

object ConstValue {
    /**If want to get any data path from asset folder of this app.
     * Call this as prefix and then just add the file remaining path.
     * e.g: if a file inside asset/sticker/cat.png, than just write
     * "[ASSETS_ROOT_PATH]/sticker/cat.png",
     */
    const val ASSETS_ROOT_PATH = "file:///android_asset"

    object PackageName {
        fun myApp(context: Context) = context.packageName
        const val youtube = "com.google.android.youtube"
        const val facebook = "com.facebook.katana"
        const val whatsApp = "com.whatsapp"
        const val instagram = "com.instagram.android"
        const val twitter = "com.twitter.android"
        const val snapchat = "com.snapchat.android"
        const val dropbox = "com.dropbox.android"
        const val linkedIn = "com.linkedin.android"
        const val linkedInLite = "com.linkedin.android.lite"
    }

    const val PLAY_STORE_APP_PRE_URL = "https://play.google.com/store/apps/details?id="//+pkgName

}