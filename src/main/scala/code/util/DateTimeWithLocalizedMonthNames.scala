package code.util

import code.commons.TimeUtils
import org.joda.time.DateTime

import scala.language.implicitConversions

class DateTimeWithLocalizedMonthNames(d: DateTime) {
  def getMonthNameOfYear: String = TimeUtils.monthNumberToText(d.getMonthOfYear - 1)
}

object DateTimeWithLocalizedMonthNames {
  implicit def dateTimeWithLocalizedMonthNames(d: DateTime): DateTimeWithLocalizedMonthNames = new DateTimeWithLocalizedMonthNames(d)
}