package com.learner.codereducer.utils

import android.app.DatePickerDialog
import android.content.Context
import androidx.annotation.IntRange
import com.learner.codereducer.local_tool.AppUtils.LogD
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    const val ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"
    const val SIMPLE_DATE_FORMAT = "dd-MM-yyyy"
    const val SIMPLE_TIME_FORMAT = "HH:mm:ss"
    const val SIMPLE_DATE_TIME_FORMAT = "$SIMPLE_DATE_FORMAT, $SIMPLE_TIME_FORMAT"
    const val INVOICE_TIME_FORMAT = "yy/MM/dd, hh:mma"

    fun currentDjangoTime() = getCurrentDAndTFormat(ISO_DATETIME_FORMAT)
    fun currentTimeMillis() = System.currentTimeMillis()
    fun currentTimeMillis(addDay: Int) =
        System.currentTimeMillis() + (1000 * 60 * 60 * 24) * addDay.toLong()

    fun dateConversion(dateString: String, fromPattern: String, toPattern: String): String? {
        val simpleDateFormat = SimpleDateFormat(fromPattern, Locale.US)
        return try {
            simpleDateFormat.parse(dateString)?.let { date ->
                SimpleDateFormat(toPattern, Locale.US).format(date)
            }
        } catch (e: Exception) {
            LogD("dateConversion: $e")
            null
        }
    }

    fun getDateAsString(currentTimeMillis: Long?): String {
        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) // "August 19, 2021"
        return formatter.format(Date(currentTimeMillis ?: System.currentTimeMillis()))
    }

    /**If pass null value return current time*/
    fun getTimeAsString(currentTimeMillis: Long?): String {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault()) //"07:20 pm"
        return formatter.format(Date(currentTimeMillis ?: System.currentTimeMillis()))
    }

    fun getMonthName(@IntRange(from = 1, to = 12) monthNo: Int): String {
        arrayOf(
            "", "January", "February", "March", "April",
            "May", "June", "July", "August", "September",
            "October", "November", "December"
        ).let { return it[monthNo] }
    }

    /** if not found return -1*/
    fun getMonthNoOf(monthName: String): Int {
        arrayOf(
            "+", "january", "february", "march", "april",
            "may", "june", "july", "august", "september",
            "october", "november", "december"
        ).let { return it.indexOf(monthName.lowercase()) }
    }


    /**
     * @param format an Example type: "yy/MM/dd, hh:mm a"
     */
    fun getCurrentDAndTFormat(format: String? = null, timeInMillis: Long? = null): String {
        return SimpleDateFormat(
            format ?: "yy/MM/dd, hh:mm a", Locale.getDefault()
        ).format(Date(timeInMillis ?: System.currentTimeMillis()))
    }

    fun getCurrentDAndTFormat(format: String?, djangoTime: String): String {
        return djangoTime
    }

    /**Must be container size greater then 3 as hour, minutes and second*/
    fun getHMS(from: Int, container: Array<Int>) {
        var ms = from
        val h = ms / (3600 * 1000); ms %= 3600 * 1000
        val m = ms / (60 * 1000); ms %= 60 * 1000
        val s = ms / 1000; ms %= 1000
        val f = DecimalFormat("00")
        container[0] = h
        container[1] = m
        container[2] = s
        container[3] = ms
        //"${f.format(h)}:${f.format(m)}:${f.format(s)}.${ms / 100},"
    }

    fun datePickerOpenAndGet(
        context: Context,
        @IntRange(from = 1, to = 21) initDay: Int?,
        @IntRange(from = 1, to = 12) initMonth: Int?,
        @IntRange(from = 2018, to = 2050) initYear: Int?,
        listener: DatePickerListener
    ) {
        val calendar = Calendar.getInstance()
        val initY = initYear ?: calendar.get(Calendar.YEAR)
        val initM = initMonth ?: calendar.get(Calendar.MONTH)
        val initD = initDay ?: calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(context, { _, pickedYear, pickedMonth, pickedDay ->
            listener.datePicked(pickedDay, pickedMonth + 1, pickedYear)
        }, initY, initM, initD).show()
    }

    /**
     * @param timeInMillis if this time's date match with today's date return
     * true else false
     * */
    fun itsToday(timeInMillis: Long): Boolean {
        val format = "ddMMyy"
        return getCurrentDAndTFormat(format, timeInMillis) ==
                getCurrentDAndTFormat(format, null)
    }

    fun itsToday(djangoTIme: String): Boolean {
        val itemTime = dateConversion(
            djangoTIme,
            ISO_DATETIME_FORMAT,
            SIMPLE_DATE_FORMAT
        )?.replaceB2ENo()
        val curTime = getCurrentDAndTFormat(SIMPLE_DATE_FORMAT).replaceB2ENo()
        //LogD("CompTime: $itemTime. CurTime: $curTime")
        return itemTime == curTime
    }


    interface DatePickerListener {
        fun datePicked(day: Int, month: Int, year: Int)
    }
}