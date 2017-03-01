package code.util

import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.time.{DayOfWeek, Month}

import com.ibm.icu.text.DateTimePatternGenerator
import net.liftweb.http.S
import net.liftweb.http.js.JE.{JsObj, _}
import net.liftweb.http.js.JsObj
import org.joda.time.{LocalDate, YearMonth}

protected object Localization {
  def pattern(skeleton: String): String =
    DateTimePatternGenerator.getInstance(S.locale).getBestPattern(skeleton)

  def dates: JsObj = {
    val days = DayOfWeek.values().toList span (_ != DayOfWeek.SUNDAY) match {
      case (end, start) => start ++ end
    }
    val months = Month.values().toList

    JsObj(
      "weekStart" -> Num((WeekFields.of(S.locale).getFirstDayOfWeek.getValue + 1) % 7 - 1),
      "months" -> JsArray(months map (_.getDisplayName(TextStyle.FULL, S.locale)) map Str),
      "monthsShort" -> JsArray(months map (_.getDisplayName(TextStyle.SHORT, S.locale)) map Str),
      "days" -> JsArray(days map (_.getDisplayName(TextStyle.FULL, S.locale)) map Str),
      "daysShort" -> JsArray(days map (_.getDisplayName(TextStyle.SHORT, S.locale)) map Str),
      "daysMin" -> JsArray(days map (_.getDisplayName(TextStyle.NARROW, S.locale)) map Str)
    )
  }
}

object DatePicker {

  def apply(input: String, alt: String, date: LocalDate): JsRaw = JsRaw(
    s"""
      | $$.fn.datepicker.dates['${S.locale.getLanguage}'] = ${Localization.dates.toString()};
      | $$('$input').datepicker(${configuration(input, date).toString()}).on('changeDate', function(e) {
      |   $$('$alt').val($$.format.date(e.date, 'yyyy-MM-dd'));
      |   $$('$alt').closest('form').submit();
      | });
    """.stripMargin
  )

  private def configuration(input: String, date: LocalDate): JsObj = {
    val format = Localization.pattern("yyMd")

    JsObj(
      "format" -> JsObj(
        "toDisplay" -> JsRaw(s"function(date, f, l) { return $$.format.date(date, '$format'); }"),
        "toValue" -> JsRaw("function(date, f, l) { return date; }")
      ),
      "defaultViewDate" -> JsObj(
        "year" -> Num(date.getYear),
        "month" -> Num(date.getMonthOfYear - 1),
        "day" -> Num(date.getDayOfMonth)
      ),
      "beforeShowDay" -> JsRaw(
        s"""
           | function(date) {
           |    if (date.getFullYear() === ${date.getYear} && date.getMonth() === (${date.getMonthOfYear} - 1) && date.getDate() === ${date.getDayOfMonth}) {
           |      $$('$input').val($$.format.date(date, '$format'));
           |      return { classes: 'active' };
           |      } else return {};
           | }
       """.stripMargin
      ),
      "todayHighlight" -> JsTrue,
      "endDate" -> JsRaw("new Date()"),
      "startView" -> Num(0),
      "minViewMode" -> Num(0),
      "maxViewMode" -> Num(2),
      "language" -> Str(S.locale.getLanguage)
    )
  }
}

object MonthPicker {

  def apply(input: String, alt: String, date: YearMonth): JsRaw = JsRaw(
    s"""
       | $$.fn.datepicker.dates['${S.locale.getLanguage}'] = ${Localization.dates.toString()};
       | $$('$input').datepicker(${configuration(input, date).toString()}).on('changeDate', function(e) {
       |   $$('$alt').val($$.format.date(e.date, 'yyyy-MM-dd'));
       |   $$('$alt').closest('form').submit();
       | });
    """.stripMargin
  )

  private def configuration(input: String, date: YearMonth): JsObj = {
    val format = Localization.pattern("yyM")

    JsObj(
      "format" -> JsObj(
        "toDisplay" -> JsRaw(s"function(date, f, l) { return $$.format.date(date, '$format'); }"),
        "toValue" -> JsRaw("function(date, f, l) { return date; }")
      ),
      "defaultViewDate" -> JsObj(
        "year" -> Num(date.getYear),
        "month" -> Num(date.getMonthOfYear - 1)
      ),
      "beforeShowMonth" -> JsRaw(
        s"""
           | function(date) {
           |    if (date.getFullYear() === ${date.getYear} && date.getMonth() === (${date.getMonthOfYear} - 1)) {
           |      $$('$input').val($$.format.date(date, '$format'));
           |      return { classes: 'active' };
           |    } else return {};
           | }
       """.stripMargin
      ),
      "todayHighlight" -> JsTrue,
      "endDate" -> JsRaw("new Date()"),
      "startView" -> Num(1),
      "minViewMode" -> Num(1),
      "maxViewMode" -> Num(2),
      "language" -> Str(S.locale.getLanguage)
    )
  }
}
