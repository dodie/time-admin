package code
package snippet
package mixin

import java.time.{DayOfWeek, Month}
import java.time.format.TextStyle
import java.util.Date

import code.commons.TimeUtils
import code.commons.TimeUtils.{ISO_DATE_FORMAT, deltaInDays, monthNamesShort, parse}
import code.snippet.Params.parseMonths
import code.util.{DatePicker, MonthPicker}
import com.ibm.icu.text.DateTimePatternGenerator
import net.liftweb.common.Box.box2Option
import net.liftweb.http.S
import net.liftweb.http.js.JE._
import net.liftweb.util.Helpers._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{LocalDate, YearMonth}

import scala.xml.NodeSeq

trait DateFunctions {

  private val PARAM_OFFSET = "offset"
  private val PARAM_DATE = "date"

  /**
   * Returns the day offset parameter.
   */
  def offsetInDays: Int = {
    val offset = S.param(PARAM_OFFSET).map(_.toInt)
      .orElse(S.param(PARAM_DATE).map(s => deltaInDays(new Date(), parse(ISO_DATE_FORMAT, s))))
    offset.foreach(i => S.setSessionAttribute(PARAM_OFFSET, i.toString))
    offset.orElse(S.getSessionAttribute(PARAM_OFFSET).map(_.toInt))
      .map(i => if (i > 0) 0 else i).getOrElse(0)
  }

  /**
   * Step to previous page.
   */
  def pagingPrev(in: NodeSeq): NodeSeq = ("a [href]" #> s"?$PARAM_OFFSET=${offsetInDays - 1}") apply in

  /**
   * Step to next page.
   */
  def pagingNext(in: NodeSeq): NodeSeq =
    if (offsetInDays + 1 > 0) ("a [title]" #> S.?("no_data")) apply in
    else ("a [href]" #> s"?$PARAM_OFFSET=${offsetInDays + 1}") apply in

  /**
   * Step to today's page.
   */
  def pagingCurrent(in: NodeSeq): NodeSeq = ("a [href]" #> s"?$PARAM_OFFSET=0") apply in

  /**
   * Day selector component.
   */
  def selectedDay(in: NodeSeq): NodeSeq = {
    <form style="display:inline;">
      <input class="input-sm" autocomplete="off" type="text" value={ currentFormattedDate } name="date" id="dateSelector" onchange="this.form.submit();"/>
      <script>
        $('#dateSelector').datepicker({ DatePicker.configuration.toString() });
      </script>
      <span class="DayText">{ currentFormattedDayOfWeek }</span>
    </form>
  }

  private def currentFormattedDayOfWeek: String =
    DayOfWeek.of(LocalDate.now().plusDays(offsetInDays).getDayOfWeek).getDisplayName(TextStyle.FULL, S.locale)

  private def currentFormattedDate: String =
    DateTimeFormat.shortDate().withLocale(S.locale).print(LocalDate.now().plusDays(offsetInDays))

  /**
   * Month selector component.
   */
  def selectedMonth(in: NodeSeq): NodeSeq = {
    <form style="display:inline;" class="monthSelector">
      <input type="hidden" id="dateSelectorValue" name="date" value={ currentFormattedDate }/>
      <div autocomplete="off" type="text" value={ currentFormattedDate } id="dateSelector" onchange="$(this).closest('form').submit();"></div>
      <script>
        $('#dateSelector').datepicker({
          val conf = MonthPicker.configuration
          (conf +* JsObj(
            "onChangeMonthYear" -> JsRaw(s"""
              function(year, month, inst) {
                var format = ${conf.props.toMap.get("dateFormat").map(_.toJsCmd).getOrElse("")};
                $$('#dateSelectorValue').val($$.datepicker.formatDate(format, new Date(year, month, 1)));
                $$('#dateSelector').closest('form').submit();
              }
            """)
          )).toString()
        });
      </script>
    </form>
  }

  def monthIntervalPicker(in: NodeSeq): NodeSeq = {
    val months = parseMonths() getOrElse List(YearMonth.now())
    val pattern = DateTimeFormat.forPattern("yyyy. MM.")

    val yearMonths = Month.values().toList map (m => m.getValue -> m.getDisplayName(TextStyle.SHORT, S.locale))

    val map =
      ".date-range-input-field [value]" #> { months mkString ";" } &
      ".date-range-input-display-from [data-value]" #> { months.headOption map pattern.print getOrElse "" } &
      ".date-range-input-display-to [data-value]" #> { months.tail.headOption map pattern.print getOrElse "" } &
      ".month-selector" #> { ".month " #> { yearMonths.map { case (num, text) =>
        ".month [data-month]" #> num & ".month *" #> text
      }}}

    map(in)
  }

  def currentYearMonth(in: NodeSeq): NodeSeq = {
    val generator = DateTimePatternGenerator.getInstance(S.locale)
    val pattern = generator.getBestPattern("yyyyMMMM")
    <span>{ DateTimeFormat.forPattern(pattern).withLocale(S.locale).print(new YearMonth(LocalDate.now().plusDays(offsetInDays))) }</span>
  }

}

