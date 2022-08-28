package com.learner.codereducer.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

object GlideUtils {
    fun insertIfGlideAvailable(activity: Activity?, block: (RequestManager) -> Unit) {
        if ((activity != null) && !activity.isDestroyed && !activity.isFinishing)
            block.invoke(Glide.with(activity))
    }

    fun loadAsBitmap(context: Context?, imageFile: Any?, function: (Bitmap) -> Unit) {
        if (context.availableForGlide()) Glide.with(context!!).asBitmap().load(imageFile)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    function(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }
}

fun Context?.availableForGlide(): Boolean {
    if (this == null) return false
    if ((this is Activity) && (this.isDestroyed || this.isFinishing)) return false
    return true
}