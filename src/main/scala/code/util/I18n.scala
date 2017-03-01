package code.util

import java.util.Locale

import com.ibm.icu.text.DateTimePatternGenerator
import org.joda.time.YearMonth
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

object I18n {

  object Dates {

    def printLongForm(ym: YearMonth, locale: Locale): String =
      formatter("yMMMM", locale).print(ym)

    private def formatter(skeleton: String, locale: Locale): DateTimeFormatter =
      DateTimeFormat.forPattern(generator(locale).getBestPattern(skeleton)).withLocale(locale)

    private def generator(locale: Locale): DateTimePatternGenerator =
      DateTimePatternGenerator.getInstance(locale)
  }

}
