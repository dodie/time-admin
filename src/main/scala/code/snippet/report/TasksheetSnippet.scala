package code.snippet

import scala.xml.NodeSeq
import code.model.User
import code.service.{ReportService, TaskSheetItem}
import code.snippet.mixin.DateFunctions
import net.liftweb.util.BindHelpers.strToCssBindPromoter
import net.liftweb.http.S
import com.github.nscala_time.time.Imports._
import code.util.ListToFoldedMap._
import org.joda.time.ReadablePartial

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
    val taskSheet = ReportService.taskSheetData(
      User.currentUser.get,
      new YearMonth(S.param("date").map(s => DateTime.parse(s)).getOrElse(DateTime.now())).toInterval,
      d => new MonthDay(d)
    )

    (
      ".dayHeader" #> dates(taskSheet).map(d => ".dayHeader *" #> d.get(d.getFieldTypes.last)) &
        ".TaskRow" #> tasks(taskSheet).map { t =>
        ".taskFullName *" #> t.name & ".taskFullName [title]" #> t.name &
        ".dailyData" #> dates(taskSheet)
          .map(d => taskSheet.get(d).flatMap(_.get(t)).map(_.minutes.toString).getOrElse(""))
          .map(".dailyData *" #> _) &
        ".taskSum *" #> sumByTasks(taskSheet)(t).minutes
      } &
      ".dailySum" #> dates(taskSheet).map(d => ".dailySum *" #> sumByDates(taskSheet)(d).minutes) &
      ".totalSum *" #> sum(taskSheet).minutes & ".totalSum [title]" #> sum(taskSheet).hours
    )(in)
  }

  def dates[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]]): List[RD] =
    ts.keys.toList.sorted

  def sumByDates[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]]): Map[RD, Duration] =
    ts.mapValues(m => m.values.foldLeft(Duration.millis(0))(_ + _))

  def tasks[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]]): List[TaskSheetItem] =
    ts.values.flatMap(_.keySet).toSet.toList.sortBy((t: TaskSheetItem) => t.name)

  def sumByTasks[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]]): Map[TaskSheetItem, Duration] =
    ts.values.flatMap(_.toList).toList.foldedMap(Duration.millis(0))(_ + _)

  def sum[RD <: ReadablePartial](ts: Map[RD, Map[TaskSheetItem, Duration]]): Duration =
    sumByDates(ts).values.foldLeft(Duration.millis(0))(_ + _)
}
