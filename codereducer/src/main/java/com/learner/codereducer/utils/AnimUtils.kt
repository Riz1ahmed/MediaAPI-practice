package com.learner.codereducer.utils

import android.animation.LayoutTransition
import android.content.Context
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.learner.codereducer.R

object AnimUtils {
    fun scaleUp(context: Context): Animation =
        AnimationUtils.loadAnimation(context, R.anim.scale_up)

    fun scaleDown(context: Context): Animation =
        AnimationUtils.loadAnimation(context, R.anim.scale_down)

    fun slideFromBottom(context: Context): Animation =
        AnimationUtils.loadAnimation(context, R.anim.slide_from_bottom)

    fun slideToDown(context: Context): Animation =
        AnimationUtils.loadAnimation(context, R.anim.slide_to_bottom)

    fun slideFromRight(context: Context): Animation =
        AnimationUtils.loadAnimation(context, R.anim.slide_from_right)

    fun slideToRight(context: Context): Animation =
        AnimationUtils.loadAnimation(context, R.anim.slide_to_right)

    fun slideFromLeft(context: Context): Animation =
        AnimationUtils.loadAnimation(context, R.anim.slide_from_left)

    fun slideToLeft(context: Context): Animation =
        AnimationUtils.loadAnimation(context, R.anim.slide_to_left)


    fun replaceFragWithSlideDown(
        fragmentManager: FragmentManager, @IdRes frameLayoutId: Int, fragment: Fragment
    ) {
        fragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_from_bottom, R.anim.slide_to_top)
            .replace(frameLayoutId, fragment).commitAllowingStateLoss()
    }
    fun replaceFragWithSlideUp(
        fragmentManager: FragmentManager, @IdRes frameLayoutId: Int, fragment: Fragment
    ) {
        fragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_from_top, R.anim.slide_to_bottom)
            .replace(frameLayoutId, fragment).commit()
    }

    /**
     * 1st add " android:animateLayoutChanges="true" in this view at xml"
     * Otherwise will get NullPointerException
     */
    fun enableDefaultAnimationOfView(viewGroup: ViewGroup)=
        viewGroup.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

}