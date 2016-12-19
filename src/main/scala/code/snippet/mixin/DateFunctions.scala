package code
package snippet
package mixin

import java.util.Date

import code.commons.TimeUtils.{ISO_DATE_FORMAT, deltaInDays, monthNamesShort, parse}

import scala.xml.NodeSeq
import scala.xml.Text
import code.commons.TimeUtils
import net.liftweb.common.Box.box2Option
import net.liftweb.http.S
import net.liftweb.util.Helpers.AttrBindParam
import net.liftweb.util.Helpers
import net.liftweb.http.js._
import JE._
import code.snippet.Params.{parseMonths, thisMonth}
import net.liftweb.util.BindHelpers.strToCssBindPromoter
import org.joda.time.YearMonth
import org.joda.time.format.DateTimeFormat

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
  def pagingPrev(in: NodeSeq): NodeSeq = Helpers.bind("paging", in, AttrBindParam("url", Text("?" + PARAM_OFFSET + "=" + (offsetInDays - 1)), "href"))

  /**
   * Step to next page.
   */
  def pagingNext(in: NodeSeq): NodeSeq = {
    if (offsetInDays + 1 > 0) {
      Helpers.bind("paging", in, AttrBindParam("url", Text(S.?("no_data")), "title"))
    } else {
      Helpers.bind("paging", in, AttrBindParam("url", Text("?" + PARAM_OFFSET + "=" + (offsetInDays + 1)), "href"))
    }
  }

  /**
   * Step to today's page.
   */
  def pagingCurrent(in: NodeSeq): NodeSeq = Helpers.bind("paging", in, AttrBindParam("url", Text("?" + PARAM_OFFSET + "=0"), "href"))

  /**
   * Day selector component.
   */
  def selectedDay(in: NodeSeq): NodeSeq = {
    <form style="display:inline;">
      <input class="input-sm" autocomplete="off" style="display:inline;" type="text" value={ TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentDayStartInMs(offsetInDays)) } name={ PARAM_DATE } id="dateSelector" onchange="this.form.submit();"/>
      <script>
        $('#dateSelector').datepicker({ daySelectorConfiguration }
        );
      </script>
      <span class="DayText">
        {
          TimeUtils.dayNumberToText(TimeUtils.currentDayOfWeek(offsetInDays))
        }
      </span>
    </form>
  }

  private def daySelectorConfiguration = {
    JsObj(
      "dateFormat" -> "yy-mm-dd",
      "maxDate" -> 0,
      "firstDay" -> 1,
      "monthNames" -> JsArray(TimeUtils.monthNames.map(x => Str(x))),
      "monthNamesShort" -> JsArray(monthNamesShort.map(x => Str(x))),
      "dayNames" -> JsArray(TimeUtils.dayNames.map(x => Str(x))),
      "dayNamesMin" -> JsArray(TimeUtils.dayNamesShort.map(x => Str(x))),
      "dayNamesShort" -> JsArray(TimeUtils.dayNamesShort.map(x => Str(x))),
      "nextText" -> S.?("button.next"),
      "prevText" -> S.?("button.previous")
    ).toString
  }

  /**
   * Month selector component.
   */
  def selectedMonth(in: NodeSeq): NodeSeq = {
    <form style="display:inline;" class="monthSelector">
      <input type="hidden" id="dateSelectorValue" name={ PARAM_DATE } value={ TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentMonthStartInMs(offsetInDays)) }/>
      <div autocomplete="off" type="text" value={ TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentMonthStartInMs(offsetInDays)) } id="dateSelector" onchange="$(this).closest('form').submit();"></div>
      <script>
        $('#dateSelector').datepicker({ monthSelectorConfiguration }
        );
      </script>
    </form>
  }

  private def monthSelectorConfiguration = {
    JsObj(
      "dateFormat" -> "yy-mm-dd",
      "maxDate" -> 0,
      "firstDay" -> 1,
      "monthNames" -> JsArray(TimeUtils.monthNames.map(x => Str(x))),
      "monthNamesShort" -> JsArray(monthNamesShort.map(x => Str(x))),
      "dayNames" -> JsArray(TimeUtils.dayNames.map(x => Str(x))),
      "dayNamesMin" -> JsArray(TimeUtils.dayNamesShort.map(x => Str(x))),
      "dayNamesShort" -> JsArray(TimeUtils.dayNamesShort.map(x => Str(x))),
      "nextText" -> S.?("button.next"),
      "prevText" -> S.?("button.previous"),
      "changeMonth" -> true,
      "changeYear" -> true,
      "defaultDate" -> JsRaw("new Date('" + TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentMonthStartInMs(offsetInDays)) + "')"),
      "onChangeMonthYear" -> JsRaw("""
				function(year, month, inst) {
					if(month < 10) {
						month = "0" + month
					}
					document.getElementById('dateSelectorValue').value = year + "-" + month + "-01";
					$(document.getElementById('dateSelector')).closest('form').submit();
				}
			""")
    ).toString
  }

  def selectedMonthInterval(in: NodeSeq): NodeSeq = {
    val months = parseMonths(S) getOrElse List(YearMonth.now())
    val pattern = DateTimeFormat.forPattern("yyyy. MM.")

    val map =
      ".date-range-input-field [value]" #> { months mkString ";" } &
      ".date-range-input-display-from [data-value]" #> { months.headOption map pattern.print getOrElse "" } &
      ".date-range-input-display-to [data-value]" #> { months.tail.headOption map pattern.print getOrElse "" } &
      ".month-selector" #> { ".month " #> { for (i <- 1 to 12) yield {
        ".month [data-month]" #> i & ".month *" #> monthNamesShort(i - 1)
      }}}

    map(dateRangeTemplate)
  }

  private def dateRangeTemplate: NodeSeq =
    <div class="date-range-container">
      <span class="date-range-input">
        <span class="date-range-input-display-from" data-value=""></span> <span class="date-range-input-display-to" data-value=""></span>
        <input name="interval" class="date-range-input-field" type="hidden" value=""/>
      </span>

      <div class="date-range-input-popup date-range-input-popup-template">
        <div class="year-selector">
          <div class="previous-year"></div>
          <div class="current-year" data-year=""></div>
          <div class="next-year"></div>
        </div>
        <div class="month-selector">
          <div class="month" data-month=""></div>
        </div>
      </div>

      <script src="/js/date-range.js"></script>
    </div>

  /**
   * Current date as text.
   */
  def currentDate(in: NodeSeq): NodeSeq = {
    <span> { TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentDayStartInMs(offsetInDays)) } </span>
  }

  /**
   * Current year as text.
   */
  def currentYear(in: NodeSeq): NodeSeq = {
    <span> { TimeUtils.format(TimeUtils.YEAR_FORMAT, TimeUtils.currentDayStartInMs(offsetInDays)) } </span>
  }

  /**
   * Current month name as text.
   */
  def currentMonth(in: NodeSeq): NodeSeq = {
    <span> { TimeUtils.monthNumberToText(TimeUtils.currentMonth(offsetInDays)) } </span>
  }

}

