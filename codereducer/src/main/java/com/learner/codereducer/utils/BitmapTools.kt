package com.learner.codereducer.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

/** @author [Riz1Ahmed](https://fb.com/Riz1Ahmed)
 *
 * Date: 15/10/2020*/
object BitmapTools {

    private const val imageQuality = 70
    private const val picFolderName = "VideoOnFrame"

    fun saveBitmapToSDGetPath(
        context: Context, bitmap: Bitmap, name: String = "auto generate"
    ): String {
        var dateAndTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        if (name != "auto generate") dateAndTime = name
        val fileNameWithExt = "VideoOnFrame_$dateAndTime.png"

        resizeInto(bitmap, 1500).also {
            return (if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                saveImageAPI29Less(context, it, fileNameWithExt)
            else saveImageAPI29Plus(context, it, fileNameWithExt))
        }

    }

    @Suppress("DEPRECATION")
    private fun saveImageAPI29Less(
        context: Context, bitmap: Bitmap, fileNameWithExt: String
    ): String {
        val root = Environment.getExternalStorageDirectory().toString()
        val myDir = File("$root/Pictures/$picFolderName")
        myDir.mkdirs()
        val file = File(myDir, fileNameWithExt)
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, imageQuality, out)
            out.flush(); out.close()
        } catch (ignored: Exception) {
        }
        notifyGallery(context, Uri.fromFile(file))
        return "$root/Pictures/$picFolderName/$fileNameWithExt"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageAPI29Plus(
        context: Context, bitmap: Bitmap, fileNameWithExt: String
    ): String {
        val directory = Environment.DIRECTORY_PICTURES + "/" + picFolderName
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileNameWithExt)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
        val resolver = context.contentResolver
        var uri: Uri? = null
        try {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            uri = resolver.insert(contentUri, contentValues)!!
            val stream = resolver.openOutputStream(uri)
            bitmap.compress(Bitmap.CompressFormat.PNG, imageQuality, stream)
            notifyGallery(context, uri)
        } catch (e: IOException) {
            if (uri != null) resolver.delete(uri, null, null)
        }
        return "${Environment.DIRECTORY_PICTURES + "/" + picFolderName}/$fileNameWithExt"
    }

    fun saveImgToFileAndGetPath(
        context: Context, bitmap: Bitmap, mxSize: Int = 1500, folder: String = "Pictures"
    ): String {
        resizeInto(bitmap, mxSize).also {
            val nameWithType = System.currentTimeMillis().toString() + ".png"
            val file = File(context.filesDir, folder)
            file.mkdirs()
            val stream = FileOutputStream("$file/$nameWithType")
            it.compress(Bitmap.CompressFormat.PNG, imageQuality, stream)
            stream.close()
            return file.absolutePath + "/" + nameWithType
        }
    }

    fun saveBitmapToFile(bitmap: Bitmap, file: File) =
        FileUtils.writeToFile(file, bitmap)

    /**
     * Process:
     * 1. Create an empty [Bitmap] with given/view size.
     * 2. Initialize a [Canvas] with this bitmap.
     * 3. Draw the View on this canvas by '.draw' method.
     * 4. return the bitmap.
     */
    fun getBitmap(view: View, width: Int? = null, height: Int? = null): Bitmap {
        val returnedBitmap: Bitmap =
            Bitmap.createBitmap(width ?: view.width, height ?: view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap) //Bind a canvas to it
        view.background?.draw(canvas) //Draw background of view if available
        view.draw(canvas) // draw the view on the canvas
        return returnedBitmap
    }

    suspend fun getBitmapFromURL(src: String): Bitmap? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getBitmap(context: Context, resDrawable: Int): Bitmap {
        return ContextCompat.getDrawable(context, resDrawable)!!.toBitmap()
    }

    /**
     *
     */
    fun getBitmap(context: Context, assetFilePath: String): Bitmap? {
        var bitmap: Bitmap? = null
        val path = if (assetFilePath.contains("file:///android_asset/"))
            assetFilePath.replace("file:///android_asset/", "")
        else assetFilePath
        val inputStream: InputStream
        try {
            inputStream = context.assets.open(path)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
        }
        return bitmap
    }

    fun getFlippedBitmap(source: Bitmap, xFlip: Boolean, yFlip: Boolean): Bitmap {
        val matrix = Matrix().apply {
            postScale(
                if (xFlip) -1f else 1f,
                if (yFlip) -1f else 1f,
                source.width / 2f,
                source.height / 2f
            )
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /*fun getBitmapFromSVG(context: Context, assetSvgPath:String): Bitmap {
        LogD(assetSvgPath)
        val svg=SVGParser.getSVGFromAsset(context.assets,assetSvgPath)
        return svg.createPictureDrawable().toBitmap()
    }*/

    fun getBitmap(picture: Picture, size: Int = -1): Bitmap {
        val pd = PictureDrawable(picture)
        val width = if (size == -1) pd.intrinsicWidth else {
            if (pd.intrinsicWidth > pd.intrinsicHeight) size
            else pd.intrinsicHeight / (pd.intrinsicWidth * size)
        }
        val height = if (size == -1) pd.intrinsicHeight else {
            if (pd.intrinsicWidth < pd.intrinsicHeight) size
            else pd.intrinsicWidth / (pd.intrinsicHeight * size)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply { this.drawPicture(picture) }
        return bitmap
    }


    /**
     * @param filePath image file path.
     *
     * ex: cachePath or filePath or /storage/emulated/0/Download/image.jpg
     */
    fun getBitmap(filePath: String): Bitmap? = BitmapFactory.decodeFile(filePath)

    /**
     * Work only less api Q
     */
    fun getBitmap(context: Context, imageUri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
        else MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)

    }

    fun getBitmap(drawable: Drawable) = drawable.toBitmap()

    fun addLogo2LD(mainImage: Bitmap, logo: Bitmap): Bitmap {

        val width = mainImage.width
        val height = mainImage.height
        val logoImage = resizeInto(logo, width / 40)
        val finalImage = Bitmap.createBitmap(width, height, mainImage.config)
        val canvas = Canvas(finalImage)
        canvas.drawBitmap(mainImage, 0f, 0f, null)
        val padding = 15f
        canvas.drawBitmap(logoImage, padding, (canvas.height - logoImage.height - padding), null)
        return finalImage
    }

    fun addLogoToCenter(background: Bitmap, logo: Bitmap, logoRatio: Double): Bitmap {
        val bgWidth = background.width
        val bgHeight = background.height
        val logoResize = (bgWidth * logoRatio).toInt()
        val rLogo = Bitmap.createScaledBitmap(logo, logoResize, logoResize, true)
        val rWidth = rLogo.width
        val rHeight = rLogo.height

        val marginLeft = (bgWidth * 0.5 - rWidth * 0.5).toFloat()
        val marginTop = (bgHeight * 0.5 - rHeight * 0.5).toFloat()

        val finalBitmap = Bitmap.createBitmap(bgWidth, bgHeight, background.config)
        val canvas = Canvas(finalBitmap)
        //canvas.drawBitmap(background, Matrix(), null)
        canvas.drawBitmap(background, 0f, 0f, null)
        canvas.drawBitmap(rLogo, marginLeft, marginTop, null)
        return finalBitmap
    }

    fun resizeInto(bitmap: Bitmap, mxSize: Int): Bitmap {
        val height = bitmap.height.toFloat()
        val width = bitmap.width.toFloat()

        if (mxSize > max(height, width)) return bitmap
        //val ratio: Float = bitmap.width.toFloat() / bitmap.height.toFloat()
        //var mnSize = (mxSize / ratio).roundToInt()
        return if (width > height) {
            val mnSize = ((height / width) * mxSize).toInt()
            Bitmap.createScaledBitmap(bitmap, mxSize, mnSize, false)
        } else {
            val mnSize = ((width / height) * mxSize).toInt()
            Bitmap.createScaledBitmap(bitmap, mnSize, mxSize, false)
        }
    }

    fun resizeInto(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }


//    fun resizeInto(context: Context, photoPath: String, mxSize: Int) {
//        Glide.with(context).asBitmap().load(photoPath).into(object : CustomTarget<Bitmap>(
//            Target.SIZE_ORIGINAL,
//            mxSize
//        ) {
//            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                val stream = FileOutputStream(photoPath)
//                resource.compress(Bitmap.CompressFormat.PNG, imageQuality, stream)
//                stream.close()
//            }
//
//            override fun onLoadCleared(placeholder: Drawable?) {}
//        })
//    }
//
//    fun shareImageTo(context: Context, imagePath: String, type: String) {
//        ShareUtils.shareImage(context, imagePath, type)
//    }

    fun removeFile(file: File) {
        if (file.exists()) file.delete()
        file.exists().let { file.delete() }
    }

    fun removeFile(path: String) =
        removeFile(File(path))

    fun removeFileOrDir(fileOrDir: File) {
        if (fileOrDir.isDirectory)
            fileOrDir.listFiles()?.forEach { child ->
                removeFileOrDir(child)
            }
        if (fileOrDir.exists()) fileOrDir.delete()
    }

    fun notifyGallery(context: Context, uri: Uri) {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        else
            MediaScannerConnection.scanFile(context, arrayOf(uri.path), null) { _, _ -> }
    }

    fun createBitmap(string: String, width: Int, height: Int): Bitmap {
        val paint =
            Paint().apply { color = Color.GRAY; textSize = DeviceUtils.pxToDp(220).toFloat() }

        val textW = paint.measureText(string)
        val textH = -paint.ascent() + paint.descent()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawText(string, width / 2 - textW / 2, height - (height / 2 - textH / 2), paint)
        return bitmap
    }

}