package com.learner.mediaapipractice

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.net.Uri
import android.util.TypedValue
import androidx.annotation.AnyRes
import androidx.annotation.RawRes


object DataLoader {

    /**
     * @param filePath asset path with f=folder name. e.g: video/sample.mp4
     * */
    fun loadFromAsset(context: Context, extractor: MediaExtractor, filePath: String) {
        extractor.setDataSource(context, Uri.parse(filePath), null)
    }

    fun loadFromAssetByFileDisc(context: Context, extractor: MediaExtractor, filePath: String) {
        val afd = context.assets.openFd(filePath)
        extractor.loadData(afd)
        //This is another working process
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) extractor.setDataSource(assetFd)
    }

    fun loadFromRaw(context: Context, extractor: MediaExtractor, @RawRes rawRes: Int) {
        val rawPath = "${rawPrefix(context)}/${context.resources.getResourceEntryName(rawRes)}"
        extractor.loadData(context, Uri.parse(rawPath))
    }

    fun loadFromRawByFileDisc(context: Context, extractor: MediaExtractor, @RawRes rawRes: Int) {
        val afd = context.resources.openRawResourceFd(rawRes)
        extractor.loadData(afd)

        //This is another working process
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) extractor.setDataSource(rawFd)
    }


    private fun getFileNameById(context: Context, @AnyRes idRes: Int): String {
        val value = TypedValue().also {
            context.resources.getValue(idRes, it, true)
        }
        val fileName = value.string.toString()
        return fileName
    }

    private fun rawPrefix(context: Context) = "android.resource://${context.packageName}/raw"

    private fun MediaExtractor.loadData(afd: AssetFileDescriptor) =
        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

    private fun MediaExtractor.loadData(context: Context, uri: Uri) =
        setDataSource(context, uri, null)

}