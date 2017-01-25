package code
package commons

import org.joda.time._
import java.util.Date
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import java.text.SimpleDateFormat

import net.liftweb.http.S

import scala.language.reflectiveCalls

/**
 * Common time handling functions.
 *
 * @author David Csakvari
 */
object TimeUtils {

  val TIME_FORMAT = "HH:mm"

  val ISO_DATE_FORMAT = "yyyy-MM-dd"

  val YEAR_FORMAT = "yyyy"

  def format(format: String, time: Long) = {
    new SimpleDateFormat(format).format(time).toString
  }

  def parse(format: String, data: String) = {
    new SimpleDateFormat(format).parse(data)
  }

  def currentTime = new Date().getTime

  def currentDayStartInMs: Long = currentDayStartInMs(0)

  def getOffset(time: Long) = {
    deltaInDays(new Date(dayStartInMs(time)), new Date(currentDayStartInMs))
  }

  def currentDayStartInMs(offsetInDays: Int): Long = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DATE, offsetInDays)
    cal.getTime().getTime
  }

  def currentDayEndInMs(offsetInDays: Int): Long = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date)
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    cal.add(Calendar.DATE, offsetInDays)
    cal.getTime().getTime
  }

  def dayStartInMs(time: Long): Long = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date(time))
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.getTime().getTime
  }

  def dayEndInMs(time: Long): Long = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date(time))
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    cal.getTime().getTime
  }

  def getDeltaFrom(hour: Int, min: Int): Long = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date())
    val timeNow = cal.getTimeInMillis
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, min)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    timeNow - cal.getTimeInMillis
  }

  def getDeltaFrom(hour: Int, min: Int, offsetInDays: Int): Long = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date())
    val timeNow = cal.getTimeInMillis
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, min)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DAY_OF_YEAR, offsetInDays)

    timeNow - cal.getTimeInMillis
  }

  def chopToMinute(time: Long): Long = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date(time))
    val timeNow = cal.getTimeInMillis

    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    cal.getTimeInMillis
  }

  def deltaInDays(from: Date, to: Date): Int = {
    Days.daysBetween(new DateTime(from), new DateTime(to)).getDays()
  }

  def currentMonthStartInMs(offsetInDays: Int): Long = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DATE, offsetInDays)
    cal.set(Calendar.DAY_OF_MONTH, 1)

    cal.getTimeInMillis
  }

  def currentMonthStartInOffset(offsetInDays: Int): Int = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DATE, offsetInDays)
    cal.set(Calendar.DAY_OF_MONTH, 1)

    deltaInDays(new Date(currentDayStartInMs(offsetInDays)), cal.getTime())
  }

  def currentMonthEndInOffset(offsetInDays: Int): Int = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DATE, offsetInDays)
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))

    deltaInDays(new Date(currentDayStartInMs(offsetInDays)), cal.getTime())
  }

  def currentMonth(offsetInDays: Int): Int = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DATE, offsetInDays)
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    cal.get(Calendar.MONTH)
  }

  def currentYear(offsetInDays: Int): Int = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DATE, offsetInDays)
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    cal.get(Calendar.YEAR)
  }

  def currentDayOfWeek(offsetInDays: Int): Int = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DATE, offsetInDays)
    cal.get(Calendar.DAY_OF_WEEK)
  }

  def currentHour(): Int = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date)
    cal.get(Calendar.HOUR_OF_DAY)
  }

  def currentMinute(): Int = {
    val cal: Calendar = new GregorianCalendar
    cal.setTime(new Date)
    cal.get(Calendar.MINUTE)
  }

  def isWeekend(dt: DateTime): Boolean = {
    (dt.getDayOfWeek() == DateTimeConstants.SATURDAY || dt.getDayOfWeek() == DateTimeConstants.SUNDAY)
  }

  def isWeekend(offsetInDays: Int): Boolean = {
    isWeekend(new DateTime().withTime(0, 0, 0, 0).plusDays(offsetInDays))
  }

  def getLastDayOfMonth(dt: DateTime): Int = {
    dt.dayOfMonth().withMaximumValue().getDayOfMonth();
  }

  def getPreviousMonthOffset(): Int = {
    val today: DateTime = new DateTime(currentTime).withDayOfMonth(1);
    deltaInDays(today.toDate, today.minusMonths(1).toDate);
  }

  def offsetToDailyInterval[D <: ReadablePartial](offset: Int): Interval = new Interval(currentDayStartInMs(offset), currentDayEndInMs(offset))

  def intervalFrom[D <: ReadablePartial](d: D): Interval = d match {
    case d: { def toInterval: Interval } => d.toInterval
  }

  lazy val monthNames = List(S.?("date.month.january"),
    S.?("date.month.february"),
    S.?("date.month.march"),
    S.?("date.month.april"),
    S.?("date.month.may"),
    S.?("date.month.june"),
    S.?("date.month.july"),
    S.?("date.month.august"),
    S.?("date.month.september"),
    S.?("date.month.october"),
    S.?("date.month.november"),
    S.?("date.month.december"))

  lazy val monthNamesShort = List(S.?("date.month.short.january"),
    S.?("date.month.short.february"),
    S.?("date.month.short.march"),
    S.?("date.month.short.april"),
    S.?("date.month.short.may"),
    S.?("date.month.short.june"),
    S.?("date.month.short.july"),
    S.?("date.month.short.august"),
    S.?("date.month.short.september"),
    S.?("date.month.short.october"),
    S.?("date.month.short.november"),
    S.?("date.month.short.december"))

  lazy val dayNames = List(S.?("date.day.sunday"),
    S.?("date.day.monday"),
    S.?("date.day.tuesday"),
    S.?("date.day.wednesday"),
    S.?("date.day.thursday"),
    S.?("date.day.friday"),
    S.?("date.day.saturday"))

  lazy val dayNamesShort = List(S.?("date.day.short.sunday"),
    S.?("date.day.short.monday"),
    S.?("date.day.short.tuesday"),
    S.?("date.day.short.wednesday"),
    S.?("date.day.short.thursday"),
    S.?("date.day.short.friday"),
    S.?("date.day.short.saturday"))

  def monthNumberToText(monthNumber: Int): String = {
    monthNames(monthNumber)
  }

  def dayNumberToText(dayNumber: Int): String = {
    dayNames(dayNumber - 1)
  }
}
