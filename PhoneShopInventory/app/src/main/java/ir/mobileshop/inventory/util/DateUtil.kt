package ir.mobileshop.inventory.util

import ir.huri.jcal.JalaliCalendar
import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    /** e.g. ۱۴۰۳/۰۷/۱۵ */
    fun jalali(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val j = JalaliCalendar(cal)
        return "%04d/%02d/%02d".format(j.year, j.month, j.day)
    }

    /** e.g. 2024/10/06 */
    fun gregorian(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        return sdf.format(Date(timestamp))
    }

    /** Returns both: "۱۴۰۳/۰۷/۱۵  •  2024/10/06" */
    fun both(timestamp: Long): String = "${jalali(timestamp)}  •  ${gregorian(timestamp)}"
}
