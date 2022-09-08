package com.learner.mediaapipractice

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.learner.codereducer.local_tool.AppUtils.LogD
import com.learner.codereducer.utils.ConstValue

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val assetPath = "hart_sample.mp4"
        val assetRootPath = "file:///android_asset/hart_sample.mp4"
        val rawPath =
            "android.resource://${ConstValue.PackageName.myApp(this)}//raw/hart_sample.mp4"

        val extractor = MediaExtractor()
        //extractor.setDataSource(assets.openFd(assetPath))
        //extractor.setDataSource(this, Uri.parse(assetPath), null)
        extractor.setDataSource(this, Uri.parse(rawPath), null)

        for (i in 0 until extractor.trackCount) {
            val mediaFormat = extractor.getTrackFormat(i)
            val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME)
            LogD("Mime: $mimeType")
            if (mimeType?.startsWith(videoMime) == true) {
                val vWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
                val vHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
                val vDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION)
                val vRotate = try {
                    mediaFormat.getInteger(MediaFormat.KEY_ROTATION)
                } catch (e: Exception) {
                    0
                }

                LogD("${vWidth}x$vHeight, ${vDuration / 1000 / 1000}, $vRotate")
            } else if (mimeType == audioMime) {

            }
        }
    }

    companion object {
        const val videoMime = "video/"
        const val audioMime = "audio/"
    }
}