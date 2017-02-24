package code.snippet

import java.util.Date

import code.commons.TimeUtils
import code.service.TaskItemService.IntervalQuery
import code.service.{ReportService, TaskItemService, TaskItemWithDuration}
import code.snippet.mixin.DateFunctions
import com.github.nscala_time.time.Imports._
import net.liftweb.http.S
import net.liftweb.util.Helpers._

import code.model.Task
import scala.xml.NodeSeq

/**
 * Creates daily reports.
 * @author David Csakvari
 */
class DailySummarySnippet extends DateFunctions {

  /**
   * Displays daily report for the current user.
   */
  def summaryForToday(in: NodeSeq): NodeSeq = {

    val taskItems = TaskItemService.getTaskItems(IntervalQuery(TimeUtils.offsetToDailyInterval(offsetInDays)))
    // All task today for current user
    val aggregatedTaskItems = taskItems
      .groupBy(_.task)
      .mapValues { case h :: t =>
        t.foldLeft(h) { (z, t) =>
          TaskItemWithDuration(t.taskItem, t.path, z.duration + t.duration)
        }
      }.collect{ case (_, t) => t }.toList.sortBy(_.taskItem.start.get)

    if (aggregatedTaskItems.isEmpty) {
      NodeSeq.Empty
    } else {
      val pauseDuration = aggregatedTaskItems.find(_.task.isEmpty).map(_.duration).foldLeft(0.milli.toDuration)(_ + _)

      val sum = aggregatedTaskItems.filter(_.task.isDefined).foldLeft(0.milli.toDuration)((du, t) => du + t.duration)
      val sumTime = {
        if (sum == 0.milli.toDuration) {
          "-"
        } else {
          val h = sum.hours
          val m = sum.minutes - h * 60
          h + S.?("hour.short") + " " + m + S.?("minute.short")
        }
      }

      val flowBreak = {
        var counter = 0
        var prevTaskItemId: java.lang.Long = 0L
        for (item <- ReportService.trim(taskItems).filter(_.taskItem != 0).reverse) {
          if (item.taskItem.task.get != 0 && prevTaskItemId != 0) {
            counter = counter + 1
          }
          prevTaskItemId = item.taskItem.task.get
        }

        counter
      }

      val first = taskItems.headOption
      val last = taskItems.lastOption

      val arrival = {
        if (first.isEmpty) {
          "-"
        } else {
          val date = new Date(first.get.taskItem.start.get)
          TimeUtils.format(TimeUtils.TIME_FORMAT, date.getTime)
        }
      }

      val leave = {
        if (last.isEmpty) {
          "- (-)"
        } else {
          if (last.get.taskItem.task.get == 0) {
            val date = new Date(last.get.taskItem.start.get - ReportService.calculateTimeRemovalFromLeaveTime(pauseDuration.getMillis))
            TimeUtils.format(TimeUtils.TIME_FORMAT, date.getTime)
          } else {
            S.?("tasks.there_is_active_task")
          }
        }
      }

      val realLeave = {
        if (last.isEmpty) {
          ""
        } else {
          if (last.get.taskItem.task.get == 0) {
            val date = new Date(last.get.taskItem.start.get)
            val date2 = new Date(last.get.taskItem.start.get - ReportService.calculateTimeRemovalFromLeaveTime(pauseDuration.getMillis))

            if (date.getTime != date2.getTime) {
              "(" + S.?("dailysummary.time_of_leave_real") + TimeUtils.format(TimeUtils.TIME_FORMAT, date.getTime) + ")"
            } else {
              ""
            }
          } else {
            ""
          }
        }
      }

      (
        ".fragWrapper *" #> aggregatedTaskItems.sorted.map { t =>
          ".minutes *" #> t.duration.minutes &
          ".name *" #> (if (t.fullName.isEmpty) S.?("task.pause") else t.fullName) &
          ".taskColorIndicator [style]" #> s"background-color:rgba${t.color};" &
          ".projectColorIndicator [style]" #> s"background-color:rgba${t.baseColor};"
        } &
        ".SumTime *" #> sumTime &
        ".PauseTime *" #> pauseDuration.minutes &
        ".FlowBreak *" #> flowBreak &
        ".Arrival *" #> arrival &
        ".Leave *" #> leave &
        ".RealLeave *" #> realLeave
      ).apply(in)
    }
  }

}
