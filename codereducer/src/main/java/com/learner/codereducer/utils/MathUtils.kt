package com.learner.codereducer.utils

import android.content.res.Resources
import android.graphics.PointF
import android.util.Log
import android.util.Size
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import kotlin.math.*
import kotlin.random.Random

object MathUtils {
    fun bangla2EglishNo(value: String) = value
        .replace("০", "0")
        .replace("১", "1")
        .replace("২", "2")
        .replace("৩", "3")
        .replace("৪", "4")
        .replace("৫", "5")
        .replace("৬", "6")
        .replace("৭", "7")
        .replace("৮", "8")
        .replace("৯", "9")

    fun randomFloat(min: Int, max: Int) =
        (Random((min..max).random()).nextFloat() * (min..max).random()).setDecimalPlace(2)

    fun randomLong(min: Long, max: Long) = (min..max).random()

    private fun distance(a: PointF, b: PointF) =
        sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y))

    private fun getNewCordiAfterRotate(pivot: PointF, a: PointF, degree: Int): PointF {
        val x = a.x - pivot.x
        val y = a.y - pivot.y
        val theta = Math.toRadians(-degree.toDouble()).toFloat()
        return PointF(
            x * cos(theta) - y * sin(theta) + pivot.x,
            x * sin(theta) + y * cos(theta) + pivot.y
        )
    }

    private fun getRotatedPoints(
        from: Array<PointF>, to: Array<PointF>, pivot: PointF, degree: Int
    ) {

        Log.d(
            "xyz", "getRotatedPoint: ${
                getNewCordiAfterRotate(PointF(0f, 0f), PointF(0f, 20f), 90)
            }"
        )
        to[0] = getNewCordiAfterRotate(pivot, from[0], degree)
        to[1] = getNewCordiAfterRotate(pivot, from[1], degree)
        to[2] = getNewCordiAfterRotate(pivot, from[2], degree)
        to[3] = getNewCordiAfterRotate(pivot, from[3], degree)
    }

    fun newHeightWidthAfterRotate(size: Size, angle: Int): Pair<Float, Float> {

        val pivot = PointF(size.width / 2f, size.height / 2f)

        val newTL = getNewCordiAfterRotate(
            pivot, PointF(0f, size.height.toFloat()), angle
        )
        val newTR = getNewCordiAfterRotate(
            pivot, PointF(size.width.toFloat(), size.height.toFloat()), angle
        )
        Log.d("xyz", "NewXYPoint: $newTL * $newTR")

        val xDistance = abs(newTR.x - size.width)
        val yDistance = abs(newTL.y - size.height)
        Log.d("xyz", "xyDistance: $xDistance * $yDistance")

        return Pair(xDistance, yDistance)
    }

    fun diagonalDistanceOf(videoSize: Size) = distance(
        PointF(0f, 0f), PointF(videoSize.width.toFloat(), videoSize.height.toFloat())
    )

    /**
     * Mull be one value pass null and others non-null
     * The value that you give null that value will return.
     */
    fun ratioValue(ratio: Size, width: Int?, height: Int?): Int {
        return if (width != null) (width * ratio.height) / ratio.width
        else (height!! * ratio.width) / ratio.height
    }
}

fun String.replaceB2ENo() = this
    .replace('০', '0')
    .replace('১', '1')
    .replace('২', '2')
    .replace('৩', '3')
    .replace('৪', '4')
    .replace('৫', '5')
    .replace('৬', '6')
    .replace('৭', '7')
    .replace('৮', '8')
    .replace('৯', '9')

/** Fixed digit after decimal point. Ex: 5=5.00, 6.123=6.12*/
fun Double.setDecimalPlace(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier).toInt() / multiplier
    //return String.format("%.${decimals}f", this).toDouble()
}

/** Fixed digit after decimal point. Ex: 5=5.00, 6.123=6.12*/
fun Float.setDecimalPlace(decimals: Int) =
    toDouble().setDecimalPlace(decimals).toFloat()


val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

/**
 * @param size how many size/length will be.
 *      Ex: value=5,  size=4, then 00005
 *          value=13, size=4, then 00013
 */
fun Int.fixDigitTo(size: Int) = "%0${size}d".format(this)

fun Int.toPercentOf(value: Int): Int {
    return if (value != 0) (this * 100) / value else 0
}

fun Int.percentToMainOf(value: Int) = (this * value) / 100

fun TextView.changeColor(@ColorRes resColorId: Int) {
    ContextCompat.getColor(this.context, resColorId).let { color ->
        setTextColor(color)
        for (drawable in this.compoundDrawables) drawable?.changeColor(color)
    }
}

/**
 * x:y
 * This method return the another ratio value by reference by [a]:[b]
 *
 * @param a The first original ratio value
 * @param b The second original ratio value
 * @param ratioValue return the value of [b] if this value if reference of [a].
 * otherwise the value of [a]
 */
fun getRatioValueOf(a: Int, b: Int, ratioValue: Int) {

}