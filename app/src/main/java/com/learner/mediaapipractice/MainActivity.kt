package com.learner.mediaapipractice

import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.learner.codereducer.local_tool.AppUtils.logD
import com.learner.mediaapipractice.databinding.ActivityMainBinding
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val extractor = MediaExtractor()
        DataLoader.loadFromRaw(this, extractor, R.raw.hart_sample)

        for (i in 0 until extractor.trackCount) {
            val mediaFormat = extractor.getTrackFormat(i)
            val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME)
            logD("Mime: $mimeType")
            if (mimeType?.startsWith(videoMime) == true) {
                val vWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
                val vHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
                val vDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION)
                //val vRotate = try { mediaFormat.getInteger(MediaFormat.KEY_ROTATION) } catch (e: Exception) { 0 }
                //logD("${vWidth}x$vHeight, ${vDuration / 1000 / 1000}, $vRotate")

                extractor.selectTrack(i)
                extractFramesFromSelectedVideo(extractor)
            }
        }
    }

    private fun extractFramesFromSelectedVideo(extractor: MediaExtractor) {
        val chunkSize = 1024 * 1024
        val buffer = ByteBuffer.allocate(chunkSize)
        while (extractor.readSampleData(buffer, 0) > 0) {
            //Do something with buffer
            val imageBytes = ByteArray(buffer.remaining())
            buffer.get(imageBytes)
            val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imageView.setImageBitmap(bmp)
            logD("Buffer: $buffer")
            extractor.advance()
        }
    }

    companion object {
        const val videoMime = "video/"
        const val audioMime = "audio/"
    }
}