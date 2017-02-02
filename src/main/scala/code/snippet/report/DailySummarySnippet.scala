package code.snippet

import java.util.Date

import code.commons.TimeUtils
import code.service.TaskItemService.IntervalQuery
import code.service.{Color, ReportService, TaskItemService, TaskService}
import code.snippet.mixin.DateFunctions
import net.liftweb.common._
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

    // All task today for current user
    val taskItems = TaskItemService.getTaskItems(IntervalQuery(TimeUtils.offsetToDailyInterval(offsetInDays)))

    // aggregated data items
    val aggregatedArray = ReportService.createAggregatedDatas(taskItems)

    // diagram width (longest aggregated data)
    var diagramTotalTime: Long = 0
    for (aggregatedData <- aggregatedArray) {
      if (aggregatedData.duration > diagramTotalTime) {
        diagramTotalTime = aggregatedData.duration
      }
    }

    if (aggregatedArray.isEmpty) {
      NodeSeq.Empty
    } else {
      val pause = aggregatedArray.find(_.taskId == 0)
      val pauseDuration = if (pause.isEmpty) {
        0
      } else {
        pause.get.duration
      }
      val pauseTime = pauseDuration / 1000 / 60

      val last = taskItems.lastOption
      val first = taskItems.headOption

      val sumTime = {
        if (first.isEmpty || last.isEmpty) {
          "-"
        } else {
          val deltaInMs = last.get.taskItem.start.get - first.get.taskItem.start.get - ReportService.calculateTimeRemovalFromLeaveTime(pauseDuration)
          val h = deltaInMs / 1000 / 60 / 60
          val m = (deltaInMs / 1000 / 60) - (h * 60)
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
            val date = new Date(last.get.taskItem.start.get - ReportService.calculateTimeRemovalFromLeaveTime(pauseDuration))
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
            val date2 = new Date(last.get.taskItem.start.get - ReportService.calculateTimeRemovalFromLeaveTime(pauseDuration))

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
        ".fragWrapper *" #> aggregatedArray.map(aggregatedData => {
          val color = Color.get(aggregatedData.taskName, aggregatedData.projectName, !aggregatedData.isPause)

          val fragBarStyle = s"background-color:rgba${color.toString};"
          val fragBarProjectStyle = {
            Task.findByKey(aggregatedData.rootProjectId) match {
              case Full(project) => "background-color:" + project.color.get
              case _ => "background-color: white"
            }
          }

          ".minutes *" #> (math.round(aggregatedData.duration / 60D / 1000)) &
          ".name *" #> (if (aggregatedData.projectName.isEmpty) aggregatedData.taskName else aggregatedData.projectName + "-" + aggregatedData.taskName) &
          ".taskColorIndicator [style]" #> fragBarStyle &
          ".projectColorIndicator [style]" #> fragBarProjectStyle
        }) &
        ".SumTime *" #> sumTime &
        ".PauseTime *" #> pauseTime &
        ".FlowBreak *" #> flowBreak &
        ".Arrival *" #> arrival &
        ".Leave *" #> leave &
        ".RealLeave *" #> realLeave
      ).apply(in)
    }
  }

}
