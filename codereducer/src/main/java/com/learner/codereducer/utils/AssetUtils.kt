package com.learner.codereducer.utils

import android.content.res.Resources

object AssetUtils {

    /**
     * @param folderPath The Folder path you want Item. If root folder than
     * just pass folder name, if nasted folder than pass with root folder.
     * > Eg: "frames/baby".
     * @return a array that contain the name of folder items
     */
    fun getItemList(resources: Resources, folderPath: String): Array<out String>? =
        resources.assets.list(folderPath)

    /**
     * @param itemPath  Eg: "frames/baby/1.png".
     * @return as string of the file path
     */
    fun getItemPath(itemPath: String) = "file:///android_asset/$itemPath"
}


