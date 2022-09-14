package com.learner.codereducer.utils

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.learner.codereducer.local_tool.AppUtils.logD
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/** @author [Riz1Ahmed](https://fb.com/Riz1Ahmed)
 *
 * Date: 12/12/2021*/
object FileUtils {

    private const val imageQuality = 70
    private const val folderName = "VideoOnFrame"
    private val videoMimes = arrayOf(
        "video/3gpp", "video/dl", "video/dv", "video/fli", "video/m4v",
        "video/mpeg", "video/mp4", "video/quicktime", "video/vnd.mpegurl",
        "video/x-la-asf", "video/x-mng", "video/x-ms-asf", "video/x-ms-wm",
        "video/x-ms-wmx", "video/x-ms-wvx", "video/x-msvideo", "video/x-webex"
    )

    fun notifyGallery(context: Context, uri: Uri) {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        else
            MediaScannerConnection.scanFile(context, arrayOf(uri.path), null) { _, _ -> }
    }

    fun getVideoThumb(context: Context, videoUri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var mmr: MediaMetadataRetriever? = null
            var bitmap: Bitmap? = null
            try {
                mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, videoUri)
                bitmap = mmr.getFrameAtTime(1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mmr?.release()
            }
            return bitmap
            /*ThumbnailUtils.createVideoThumbnail(
                videoUri.toFile(), Size(320, 240), CancellationSignal()
            )*/
        } else {
            @Suppress("DEPRECATION")
            ThumbnailUtils.createVideoThumbnail(
                videoUri.path!!, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
            )
        }
    }

    /**
     * @param nameWithExt if not pass this value than By default
     * image name set as current timestamp with png format.
     * @return Created cache image path
     */
    fun saveImgToCacheAndGetPath(
        context: Context, bitmap: Bitmap, nameWithExt: String? = null
    ): String {
        BitmapTools.resizeInto(bitmap, 1500).also {
            val nameWithType = nameWithExt ?: System.currentTimeMillis().toString() + ".png"
            val file = File(context.cacheDir, "Pictures")
            file.mkdirs()
            val stream = FileOutputStream("$file/$nameWithType")
            it.compress(Bitmap.CompressFormat.PNG, imageQuality, stream)
            stream.close()
            return file.absolutePath + "/" + nameWithType
        }
    }

    fun writeToFile(file: File, bitmap: Bitmap) {
        val categoryPath = file.absolutePath.replaceAfterLast("/", "")
        File(categoryPath).mkdirs()
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, imageQuality, stream)
    }

    @SuppressLint("Range")
    fun getFileName(context: Context, fileUri: Uri): String? {
        context.contentResolver.query(fileUri, null, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst())
                return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
        return null
    }

    fun getACacheFile(context: Context, nameWithExt: String = "file.png") =
        File(context.cacheDir, nameWithExt)

    /**
     * Create a file on cache folder of your app. Later you should manually delete it.
     * @param context any context of this app
     * @param fileNameWithExt With which name file will be create. Ex: video.mp4
     */
    fun createTempOutputFile(context: Context, fileNameWithExt: String) =
        context.cacheDir.absolutePath + "/" + fileNameWithExt

    fun openDefaultVideoPicker(activity: ComponentActivity, block: (Uri) -> Unit) {
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) result?.data?.data?.let { block(it) }
        }.launch(Intent(Intent.ACTION_GET_CONTENT, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .apply { type = "video/*" })
    }

    /**
     * Copy any file from cache memory to local storage.
     * @param cacheVideoPath video file full path with name and ext. eg: [Context.getCacheDir]/video.mp4
     *@suppress "Inappropriate blocking method call" Because, Detected this is MainThread
     * (though this method not from mein thread, used [suspend]). That's why showing warning
     * So, do not remove [suspend] keyword.
     */
    suspend fun copyCacheToInternal(context: Context, cacheVideoPath: String): String {
        val dateAndTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val fileName = "VideoOnFrame_$dateAndTime.mp4"
        val outputStream: OutputStream
        val filePath: String
        try {
            logD("FileName: $fileName")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                val mimeType = "video/mp4"
                val directory = Environment.DIRECTORY_MOVIES + "/$folderName"
                val mediaContentUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Video.Media.RELATIVE_PATH, directory)
                }
                context.contentResolver.run {
                    val uri = insert(mediaContentUri, values)!!
                    outputStream = openOutputStream(uri)!!
                }
                filePath = "$directory/$fileName"
                notifyGallery(context, mediaContentUri)
            } else {
                val appVideoDirectory =
                    File(Environment.getExternalStorageDirectory(), folderName)
                if (!appVideoDirectory.exists()) appVideoDirectory.mkdir()
                val fileToSave = File(appVideoDirectory, fileName)
                if (!fileToSave.exists()) fileToSave.delete()
                outputStream = FileOutputStream(fileToSave)
                filePath = fileToSave.absolutePath
                notifyGallery(context, Uri.fromFile(fileToSave))
            }
            /**Copy file*/
            val inputStream = FileInputStream(File(cacheVideoPath))
            val buffer = ByteArray(1024)
            var len: Int
            while (inputStream.read(buffer).also { len = it } > 0) {
                logD("Copy file...")
                outputStream.write(buffer, 0, len)
            }
            logD("Copied file")
            inputStream.close()
            outputStream.close()
            return filePath
        } catch (e: IOException) {
            e.printStackTrace()
            logD("Exception ${e.stackTrace}")
            return "FAILED"
        }
    }

    fun renameFile(from: File, to: File) = from.renameTo(to)

    fun createAVideoFileWith(): String {
        /**Create folder*/
        val categoryPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                .absolutePath + '/' + folderName
        File(categoryPath).apply { if (!exists()) mkdirs() }

        /**createFile*/
        val dateAndTime = SimpleDateFormat("MMddHHmmss", Locale.getDefault()).format(Date())
        var fileName = "${dateAndTime}_VideoOnFrame.mp4"

        /**File exist check*/
        if (File("$categoryPath/$fileName").exists()) {
            val ext = ".mp4"
            val pref = "${dateAndTime}_VideoOnFrame"
            var cnt = 1
            while (File("$categoryPath/$pref($cnt)$ext").exists()) cnt++
            fileName = "$pref($cnt)$ext"
        }

        return "$categoryPath/$fileName"
    }

    /**
     * Save image file from any url to [Context.getFilesDir]/[folderName]
     * of this app.
     * @param url url path of image file
     * @param folderName only the folder name of [Context.getFilesDir].
     * @return first check the file on give folder. if not available, then
     * download the image file to this folder. then return the image as [File]
     */
    suspend fun backup2FileDirAndGetFile(
        context: Context, url: String, folderName: String
    ): File? {
        val fileNameWithExt = url.replaceBeforeLast('/', "")
        val cachePath = "$folderName/$fileNameWithExt"

        val file = File(context.filesDir.absolutePath + "/" + cachePath)
        if (!file.exists() || file.length() == 0L) downloadFile(context, url, file)
        return if (file.exists() && file.length() > 0) file
        else file.delete().let { null }
    }

    /**
     * Download any file from url ro file.
     * @suppress "Inappropriate blocking method call"->
     * The IDE remembering us that, this method called from Main Thread.
     * But this is actually suspend function, not from Main Thread
     * @return true if download successfully, otherwise false.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun downloadFile(context: Context, url: String, file: File): Boolean {
        /*val name = getNameFromUri(url) // uri = template.backgroundVideoUri
        val dir = File(context.nmn mfilesDir.absolutePath,type) // type = backgroundVideos
        if(!dir.exists()) dir.mkdir()
        val file = File(dir,name)
        if(file.exists()) return true*/
        logD("Downloading: ${file.name}")
        logD("Url: $url")
        val categoryPath = file.absolutePath.replaceAfterLast("/", "")
        File(categoryPath).mkdirs()

        /**
         * First download as temporary file with adding 'cc' to suffix.
         * When download complete, than remove this 'cc' from this
         * file (actually rename with original name).
         *
         * Why this technique: So that the file don't save before download complete.
         * */
        val cacheName = "cc${file.name}"
        val cacheTFile = File(context.filesDir.absolutePath, cacheName)
        if (cacheTFile.exists()) cacheTFile.delete()
        try {
            val fileUrl = URL(url)
            val connection = fileUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()
            val fOut = FileOutputStream(cacheTFile)
            val fIn = connection.inputStream
            val buffer = ByteArray(1024)
            var len: Int
            var downloaded = 0L
            val totalSize = connection.contentLength
            while (fIn.read(buffer).also { len = it } > 0) {
                fOut.write(buffer, 0, len)

                downloaded += len
                if (totalSize > 0) {
                    val percent: Int = ((downloaded * 100) / totalSize).toInt()
                }
            }
            fOut.close()
            fIn.close()
            connection.disconnect()
            cacheTFile.renameTo(file)
            logD("Download complete the ${file.name}")
            return true
        } catch (e: Exception) {
            Log.d("download status: ", "Download Failed, ${file.name}. Exception: $e")
            e.printStackTrace()
            if (file.exists()) file.delete()
            if (cacheTFile.exists()) cacheTFile.delete()
            return false
        }
    }

    /*private fun saveVideo(context: Context, videoTitleWithExt: String) {
        PermissionsController.check(context, arrayListOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ), object : PermissionListener {
            override fun allGranted() {
                CoroutineScope(IO).launch {
                    copyCacheToInternal(context, videoTitleWithExt)
                }
            }

            override fun allNotGranted(deniedList: ArrayList<String>) {
                super.allNotGranted(deniedList)
                context.customToast("Please grant permission for save your video")
            }
        })
    }*/
}