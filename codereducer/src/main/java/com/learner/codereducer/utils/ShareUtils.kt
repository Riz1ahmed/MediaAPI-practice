package com.learner.codereducer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {
    fun shareMedia(context: Context, mediaPath: String, packageId: String?) {
        shareMedia(context, providerUri(context, mediaPath), packageId)
    }

    /**
     * @param context Running Application context
     * @param uri sharing Media file uri.
     * @param packageId if share in a specific app give that app packageId.
     * For default share set this value as null.<\br>
     * .
     * .
     * Some common app PackageId:
     * YourApp = [BuildConfig.APPLICATION_ID]
     * youtube = [com.google.android.youtube]
     * facebook = [com.facebook.katana]
     * whatsApp = [com.whatsapp]
     * instagram = [com.instagram.android]
     * twitter = [com.twitter.android]
     * snapchat = [com.snapchat.android]
     * dropbox = [com.dropbox.android]
     * linkedIn = [com.linkedin.android]
     * linkedInLite = [com.linkedin.android.lite]
     */
    fun shareMedia(context: Context, uri: Uri, packageId: String?) {
        if (packageId != null && context.packageManager.getLaunchIntentForPackage(packageId) == null) {
            context.toast("Please first install the app")
            openUrl(context, "https://play.google.com/store/apps/details?id=$packageId")
        } else {
            try {
                val intent = Intent(Intent.ACTION_SEND)
                intent.setPackage(packageId)
                intent.type = "video/*"
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                context.startActivity(Intent.createChooser(intent, "Share Video to"))
            } catch (ex: Exception) {
            }
        }
    }

    fun getSupportedUri(context: Context, filePath: String): Uri {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) Uri.parse(filePath)
        else providerUri(context, filePath)
    }

    private fun providerUri(context: Context, filePath: String): Uri {
        return FileProvider.getUriForFile(
            context,
            context.packageName + ".riz1",//Here ".riz1" should be same as Manifest provider
            File(filePath)
        )
    }

    fun sharePlanText(context: Context, title: String, message: String) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, message)
        }, "Choose one"))
    }

    fun openEmail(context: Context, emails: Array<String>, subject: String, message: String) {
        context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, emails)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        })
    }

    fun openUrl(context: Context, url: String) =
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
