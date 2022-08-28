package com.learner.codereducer.utils

import android.graphics.*
import android.graphics.drawable.ColorDrawable
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * RGB = Red, Green, Blue
 *          https://en.wikipedia.org/wiki/RGB_color_model
 * CMYK = Cyan, Magenta, Yellow, Black
 *          https://en.wikipedia.org/wiki/CMYK_color_model
 * HSL = Hue, Saturation, Lightness
 * HSV = Hue, Saturation, Value
 *          https://en.wikipedia.org/wiki/HSL_and_HSV
 */
object ColorUtils {
    @ColorInt
    const val TRANSPARENT: Int = 0x00000000

    @ColorInt
    const val GREEN: Int = -0xFF0100

    @ColorInt
    const val WHITE: Int = 0xFFFFFFFF.toInt()

    @ColorInt
    const val GOLDEN: Int = 0xFFFF8300.toInt()

    //@ColorInt const val ACTIVE_COLOR_LIGHT: Int = 0xFFF35448.toInt()
    @ColorInt
    const val TEXT_IC_COLOR: Int = 0xFF696A72.toInt()

    @ColorInt
    const val ACTIVE_COLOR: Int = 0xFF8F2DBD.toInt()

    @ColorInt
    const val INACTIVE_COLOR = TEXT_IC_COLOR


    fun rgb2cmyk(r: Int, g: Int, b: Int): Array<Int> {
        val percentageR = r / 255.0 * 100
        val percentageG = g / 255.0 * 100
        val percentageB = b / 255.0 * 100
        val k = 100 - max(max(percentageR, percentageG), percentageB)
        if (k == 100.0) return arrayOf(0, 0, 0, 100)
        val c = ((100 - percentageR - k) / (100 - k) * 100).toInt()
        val m = ((100 - percentageG - k) / (100 - k) * 100).toInt()
        val y = ((100 - percentageB - k) / (100 - k) * 100).toInt()
        return arrayOf(c, m, y, k.toInt())
    }

    fun cmyk2rgb(c: Int, m: Int, y: Int, k: Int): Array<Int> {
        val r = 255 * (1 - c / 100) * (1 - k / 100)
        val g = 255 * (1 - m / 100) * (1 - k / 100)
        val b = 255 * (1 - y / 100) * (1 - k / 100)
        return arrayOf(r, g, b)
    }

    fun rgb2hsv(r: Int, g: Int, b: Int): FloatArray {
        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        return hsv
    }

    fun hsv2rgb(h: Float, s: Float, v: Float): Int {
        return Color.HSVToColor(floatArrayOf(h, s, v))
    }

    fun rgb2hsv(color: Int): FloatArray {
        val hsv = FloatArray(3)
        Color.colorToHSV(Color.alpha(color), hsv)
        return hsv
    }

    fun hsl2rgb(h: Float, s: Float, l: Float): Array<Int> {
        val r: Float
        val g: Float
        val b: Float
        if (s == 0f) {
            b = l
            g = b
            r = g // achromatic
        } else {
            val q = if (l < 0.5) l * (1 + s) else l + s - l * s
            val p = 2 * l - q
            r = hue2rgb(p, q, h + 1 / 3)
            g = hue2rgb(p, q, h)
            b = hue2rgb(p, q, h - 1 / 3)
        }
        return arrayOf((r * 255).roundToInt(), (g * 255).roundToInt(), (b * 255).roundToInt())
    }

    private fun hue2rgb(p: Float, q: Float, h: Float): Float {
        val h1 = if (h < 0) h + 1f else h - 1f
        return when {
            6 * h1 < 1 -> p + (q - p) * 6 * h1
            2 * h1 < 1 -> q
            3 * h1 < 2 -> p + (q - p) * 6 * (2.0f / 3.0f - h1)
            else -> p
        }
    }

    /**
     * Process: Get every pixel HSV value and add the hue value to
     * the H(H means 'Hue' and it's index is 0). and then mod the H value
     * with 360 as it is a like circle. Actually here change the color of every pixel
     * NB: It's take almost 3 to 5 second to apply as nested loop. so please call it from thread
     * @param src Source bitmap that want to change hue.
     * @param hue hue value from 0 to 360. 0 means no change.
     * @return A bitmap after changed src bitmap hue.
     */
    fun hueApply(src: Bitmap, @IntRange(from = 0, to = 360) hue: Int): Bitmap {
        return src.copy(src.config, true).apply {
            for (x in 0 until width)
                for (y in 0 until height)
                    setPixel(x, y, hueChange(src.getPixel(x, y), hue))
        }
    }

    ///Hue changing process here.
    private fun hueChange(pixel: Int, hue: Int): Int {
        val hsv = FloatArray(3) //array to store HSV values
        Color.colorToHSV(pixel, hsv) //get HSV values of the pixel
        hsv[0] = (hsv[0] + hue) % 360 //add hue to shift the value and mod
        return Color.HSVToColor(Color.alpha(pixel), hsv)
    }

    private fun changeBitmapColor(bitmap: Bitmap, color: Int): Bitmap {
        return bitmap.copy(bitmap.config, true).also { colorBitmap ->
            val paint = Paint().apply { colorFilter = LightingColorFilter(color, 1) }
            val canvas = Canvas(colorBitmap)
            canvas.drawBitmap(colorBitmap, Matrix(), paint)
        }
    }

    fun int2HexColor(@ColorInt color: Int): String {
        //Integer.toHexString(bgDrawable.getColor()!!) // example for green color FF00FF0
        //LogD("COl$color to $hex")
        return Integer.toHexString(color)
    }

    fun getColoredBitmap(@ColorInt colorInt: Int): Bitmap {
        return ColorDrawable(colorInt).toBitmap(1000, 1000)
    }

}