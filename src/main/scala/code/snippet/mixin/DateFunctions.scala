package code
package snippet
package mixin

import java.util.Date

import code.commons.TimeUtils.{ ISO_DATE_FORMAT, parse, deltaInDays }

import scala.xml.NodeSeq
import scala.xml.Text

import code.commons.TimeUtils
import net.liftweb.common.Box.box2Option
import net.liftweb.http.S
import net.liftweb.util.Helpers.AttrBindParam
import net.liftweb.util.Helpers

import net.liftweb.http.js._
import JE._

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
      <input autocomplete="off" style="display:inline;" type="text" value={ TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentDayStartInMs(offsetInDays)) } name={ PARAM_DATE } id="dateSelector" onchange="this.form.submit();"/>
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
      "monthNamesShort" -> JsArray(TimeUtils.monthNamesShort.map(x => Str(x))),
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
      "monthNamesShort" -> JsArray(TimeUtils.monthNamesShort.map(x => Str(x))),
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
    <div style="display:inline;" class="monthSelector">
      <input type="hidden" id="intervalStart" name="intervalStart" value={S.param("intervalStart").getOrElse(TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentMonthStartInMs(0)))}/>
      <input type="hidden" id="intervalEnd" name="intervalEnd" value={S.param("intervalEnd").getOrElse(TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentMonthStartInMs(0)))}/>
      <div autocomplete="off" type="text" value={S.param("intervalStart").getOrElse(TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentMonthStartInMs(0)))} id="intervalStartSelector" onchange="$(this).closest('form').submit();"></div>
      <span> - </span>
      <div autocomplete="off" type="text" value={S.param("intervalEnd").getOrElse(TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentMonthStartInMs(0)))} id="intervalEndSelector" onchange="$(this).closest('form').submit();"></div>
      <script>
        $('#intervalStartSelector').datepicker({ monthIntervalSelectorConfiguration("intervalStartSelector", "intervalStart") });
        $('#intervalEndSelector').datepicker({ monthIntervalSelectorConfiguration("intervalEndSelector", "intervalEnd") });
      </script>
    </div>
  }

  private def monthIntervalSelectorConfiguration(name: String, valueName: String) = {
    JsObj(
      "dateFormat" -> "yy-mm-dd",
      "maxDate" -> (if (valueName == "intervalStart") S.param("intervalEnd").getOrElse(TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentMonthStartInMs(0))) else "2200-01-01").toString,
      "minDate" -> (if (valueName == "intervalEnd") S.param("intervalStart").getOrElse(TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentMonthStartInMs(0))) else "1900-01-01").toString,
      "firstDay" -> 1,
      "monthNames" -> JsArray(TimeUtils.monthNames.map(x => Str(x))),
      "monthNamesShort" -> JsArray(TimeUtils.monthNamesShort.map(x => Str(x))),
      "nextText" -> S.?("button.next"),
      "prevText" -> S.?("button.previous"),
      "changeMonth" -> true,
      "changeYear" -> true,
      "defaultDate" -> JsRaw("new Date('" + S.param(valueName).getOrElse(TimeUtils.format(ISO_DATE_FORMAT, TimeUtils.currentMonthStartInMs(0))) + "')"),
      "onChangeMonthYear" -> JsRaw(s"""
				function(year, month, inst) {
					if(month < 10) {
						month = "0" + month
					}
					document.getElementById('$valueName').value = year + "-" + month + "-01";
					$$(document.getElementById('$name')).closest('form').submit();
				}
        """)
    ).toString
  }

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

