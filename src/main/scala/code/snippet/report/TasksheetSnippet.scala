package code.snippet

import scala.xml.NodeSeq
import code.commons.TimeUtils
import code.service.ReportService
import code.snippet.mixin.DateFunctions
import net.liftweb.util.BindHelpers.strToCssBindPromoter
import code.service.TaskService
import net.liftweb.http.S

/**
 * Tasksheet displaying component.
 * @author David Csakvari
 */
class TasksheetSnippet extends DateFunctions {

  /**
   * Blank tasksheet download link.
   */
  def blankTasksheetExportLink(in: NodeSeq): NodeSeq = {
    (
      "a [href]" #> ("/export/tasksheet/blank/" + offsetInDays)
    ).apply(in)
  }

  /**
   * Tasksheet download link.
   */
  def tasksheetExportLink(in: NodeSeq): NodeSeq = {
    (
      "a [href]" #> ("/export/tasksheet/" + offsetInDays)
    ).apply(in)
  }

  /**
   * Taskshet display.
   */
  def tasksheet(in: NodeSeq): NodeSeq = {
    val taskList = TaskService.getTaskArray(activeOnly = false)
    val monthStartOffset = TimeUtils.currentMonthStartInOffset(offsetInDays) + offsetInDays
    val monthEndOffset = TimeUtils.currentMonthEndInOffset(offsetInDays) + offsetInDays

    val data = ReportService.getTasksheetData(offsetInDays)

    val totalTimesPerDay = for (dayNum <- 1 to (math.abs(monthStartOffset - monthEndOffset) + 1)) yield {
      if (data.contains(dayNum)) {
        data(dayNum).filterKeys(_ != 0).values.foldLeft(0L) { (total, n) =>
          val minute = (n / 60000).toInt
          total + minute
        }.toInt
      } else {
        0
      }
    }

    val totalSum = totalTimesPerDay.sum

    if (!taskList.isEmpty) {
      val colClassNames = for (dayNum <- 1 to (math.abs(monthStartOffset - monthEndOffset) + 1)) yield {
        if (TimeUtils.isWeekend(monthStartOffset + dayNum - 1)) {
          "colWeekend"
        } else {
          "colWeekday"
        }
      }

      val dayHeaders = { for (dayNum <- 1 until (math.abs(monthStartOffset - monthEndOffset) + 2)) yield dayNum }
      (
        ".colDays" #> colClassNames.map(item => ".colDays [class]" #> item) &
        ".dayHeader" #> dayHeaders.map(item => ".dayHeader *" #> item) &
        ".TaskRow" #> taskList.toList.map(task => {
          var sum = 0
          val taskActivityForMonth = {
            for (dayNum <- 1 to math.abs(monthStartOffset - monthEndOffset) + 1) yield {
              val dayData = if (data.contains(dayNum) && data(dayNum).contains(task.task.id.get)) {
                val minute = (data(dayNum)(task.task.id.get) / 60000).toInt
                sum = sum + minute
                if (minute > 0) {
                  minute.toString
                } else {
                  ""
                }
              } else {
                ""
              }

              val dayType = if (TimeUtils.isWeekend(monthStartOffset + dayNum - 1)) {
                "colWeekend"
              } else {
                "colWeekday"
              }

              (dayData, dayType)
            }
          }

          ".taskFullName *" #> task.getFullName &
            ".taskFullName [title]" #> task.getFullName &
            ".dailyData" #> taskActivityForMonth.map(minute => {
              ".dailyData *" #> minute._1 &
                ".dailyData [class]" #> minute._2
            }) &
            ".taskSum *" #> sum
        }) &
        ".dailySum" #> totalTimesPerDay.map(minute => ".dailySum *" #> minute) &
        ".totalSum *" #> totalSum &
        ".totalSum [title]" #> (BigDecimal(totalSum.toDouble / 60).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble + " " + S.?("hour"))
      ).apply(in)
    } else {
      <lift:embed what="no_data"/>
    }
  }
}
