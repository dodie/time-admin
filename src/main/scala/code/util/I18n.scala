package code.util

import java.util.Locale

import com.ibm.icu.text.DateTimePatternGenerator
import net.liftweb.http.S
import org.joda.time.{Interval, YearMonth}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

object I18n {

  object Dates {

    def printLongForm(ym: YearMonth, locale: Locale): String =
      formatter("yMMMM", locale).print(ym)

    def printLongForm(i: Interval, locale: Locale): String = {
      val start = new YearMonth(i.getStart)
      val end = new YearMonth(i.getEnd.minusDays(1))
      if (start == end) printLongForm(start, S.locale)
      else s"${printLongForm(start, S.locale)} - ${printLongForm(end, S.locale)}"
    }

    private def formatter(skeleton: String, locale: Locale): DateTimeFormatter =
      DateTimeFormat.forPattern(generator(locale).getBestPattern(skeleton)).withLocale(locale)

    private def generator(locale: Locale): DateTimePatternGenerator =
      DateTimePatternGenerator.getInstance(locale)

  }

}
