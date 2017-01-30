package code.snippet

import java.util.Date

import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.NodeSeq
import scala.xml.Node
import scala.xml.Text
import net.liftweb.common._
import code.model.Project
import code.service.TaskItemService
import code.service.ReportService
import code.service.TaskService
import code.snippet.mixin.DateFunctions
import code.commons.TimeUtils
import code.service.TaskItemService.IntervalQuery
import net.liftweb.mapper.MappedField.mapToType
import net.liftweb.util.Helpers.AttrBindParam
import net.liftweb.util.Helpers.strToSuperArrowAssoc
import net.liftweb.util.Helpers
import net.liftweb.util.Helpers._
import net.liftweb.http.S

/**
 * Creates daily reports.
 * @author David Csakvari
 */
class DailySummarySnippet extends DateFunctions {

  /**
   * Displays daily report for the current user.
   */
  def summaryForToday(in: NodeSeq): NodeSeq = {
    def attributeValueEquals(value: String)(node: Node) = {
      node.attributes.exists(_.value.text == value)
    }

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
      val pause = aggregatedArray.filter(_.taskId == 0).headOption
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
          val date = new Date(deltaInMs)

          val h = (deltaInMs / 1000 / 60 / 60)
          val m = (deltaInMs / 1000 / 60) - (h * 60)
          h + S.?("hour.short") + " " + m + S.?("minute.short")
        }
      }

      val flowBreak = {
        var counter = 0;
        var prevTaskItemId: java.lang.Long = 0;
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
          val (red, green, blue, alpha) = TaskService.getColor(aggregatedData.taskName, aggregatedData.projectName, !aggregatedData.isPause)

          val fragBarStyle = "background-color:rgba(" + red + " , " + green + " , " + blue + " ," + alpha + ");"
          val fragBarProjectStyle = {
            (Project.findByKey(aggregatedData.rootProjectId) match {
              case Full(project) => "background-color:" + project.color.get
              case _ => "background-color: white"
            })
          }

          ".minutes *" #> (math.round(aggregatedData.duration / 60D / 1000)) &
          ".name *" #> (if (aggregatedData.projectName.isEmpty) aggregatedData.taskName else (aggregatedData.projectName + "-" + aggregatedData.taskName)) &
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
