package code
package snippet

import code.commons.TimeUtils
import code.service._
import code.snippet.mixin.DateFunctions
import net.liftweb.common.Box.box2Option
import net.liftweb.http.S
import net.liftweb.util.BindHelpers.strToCssBindPromoter
import net.liftweb.util.Helpers.{AttrBindParam, strToSuperArrowAssoc}
import net.liftweb.util.{Helpers, PCDataXmlParser}

import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.{NodeSeq, Text}

/**
 * Task item editing and listing.
 * @author David Csakvari
 */
class TaskItemSnippet extends DateFunctions {

  /** All task items today for current user. */
  lazy val taskItems = TaskItemService.getTaskItems(TimeUtils.offsetToDailyInterval(offsetInDays))

  /** All tasks. */
  lazy val tasks = TaskService.getTaskArray()

  /**
   * Renders the currently selected task as an information text.
   */
  def actualTask(in: NodeSeq): NodeSeq = {
    if (offsetInDays == 0) {
      val actualTask = taskItems.lastOption
      val taskName = if (!actualTask.isEmpty) actualTask.get.taskName.getOrElse(S.?("tasks.pause")) else S.?("tasks.pause")
      val projectName = if (!actualTask.isEmpty && !actualTask.get.projectName.isEmpty) actualTask.get.projectName.get + "-" else ""
      val description = if (!actualTask.isEmpty && !actualTask.get.task.isEmpty) TaskService.getPreparedDescription(actualTask.get.task.get) else "<span></span>"

      (
        ".actualTaskName *" #> (projectName + taskName) &
        ".actualDescription *" #> PCDataXmlParser.apply(description)
      ).apply(in)
    } else {
      NodeSeq.Empty
    }
  }

  /**
   * Displays a data table from the current users's daily tasks.
   */
  def dataTable(in: NodeSeq): NodeSeq = {
    if (taskItems.isEmpty || taskItems.head.taskItem.task.get == 0) {
      <lift:embed what="no_data"/>
    } else {
      (".item *" #> taskItems.map(taskItemDto => {
        val active = (!taskItemDto.task.isEmpty)
        val fragBarStyle = {
          "background-color:rgba" + taskItemDto.color.toString + ";"
        }
        val fragBarProjectStyle = {
          "background-color:rgba" + taskItemDto.baseColor.toString + ";"
        }
        ".date *" #> getDateString(taskItemDto) &
        ".project *" #> taskItemDto.projectName.getOrElse("") &
        ".task *" #> taskItemDto.taskName.getOrElse("") &
        ".taskColorIndicator [style]" #> fragBarStyle &
        ".projectColorIndicator [style]" #> fragBarProjectStyle &
        (if (active)
          ".glyphicon-pause" #> NodeSeq.Empty
        else
          ".taskColorIndicator" #> NodeSeq.Empty) &
        "@taskitemid [value]" #> taskItemDto.taskItem.id.get &
        "@taskid [value]" #> taskItemDto.taskItem.task.get &
        "@time [value]" #> getDateString(taskItemDto)
      })).apply(in)
    }
  }

  /**
   * Displays a line chart from the current users's daily tasks.
   */
  def lineChart(in: NodeSeq): NodeSeq = {
    if (taskItems.isEmpty || taskItems.head.taskItem.task.get == 0) {
      <lift:embed what="no_data"/>
    } else {
      val diagramStartTime = taskItems.head.taskItem.start.get
      val diagramEndTime = taskItems.last.taskItem.start.get + taskItems.last.duration.getMillis
      val diagramTotalTime = diagramEndTime - diagramStartTime

      var odd = true;

      (
        ".frag" #> taskItems.map(
          taskItemDto => {
            val last = (taskItemDto.taskItem.id == taskItems.last.taskItem.id)
            val active = (!taskItemDto.task.isEmpty)
            odd = !odd;

            val lengthInPercent = {
              val value = ((taskItemDto.duration.getMillis.asInstanceOf[Double] / diagramTotalTime.asInstanceOf[Double]) * 100 * 100).asInstanceOf[Int] / 100D
              if (value < 0.1D || (last && !active)) {
                0D
              } else {
                value
              }
            }

            val fragStyle = "width:" + lengthInPercent + "%;"
            val fragBarStyle = {
              val color = taskItemDto.color
              (if (color.isDark) "color:white; " else "color:black; ") + "background-color:rgba" + color.toString + ";"
            }

            val fragBarProjectStyle = {
              val color = taskItemDto.baseColor
              (if (color.isDark) "color:white; " else "color:black; ") + "background-color:rgba" + color.toString + ";"
            }
            val fragTextStyle = if (odd) Text("top:-80px;") else Text("top:15px;")
            val fragBarClass = if (active && last) "fragBar fragBarContinued" else if (active && !last) "fragBar" else "fragBar noborder"

            ".ProjectName *" #> taskItemDto.projectName.getOrElse("") &
            ".ProjectName [style]" #> fragBarProjectStyle &
            ".TaskName *" #> taskItemDto.taskName.getOrElse("") &
            ".Duration *" #> (S.?("duration") + ": " + taskItemDto.duration.toStandardMinutes.getMinutes + " " + S.?("minute")) &
            ".fragText *" #> taskItemDto.timeString &
            ".frag [style]" #> fragStyle &
            ".fragBar [style]" #> fragBarStyle &
            ".fragBar [class]" #> fragBarClass &
            ".fragBarHover [style]" #> fragBarStyle &
            ".fragText [style]" #> fragTextStyle &
            "@taskitemid [value]" #> taskItemDto.taskItem.id.get &
            "@taskid [value]" #> taskItemDto.taskItem.task.get &
            "@time [value]" #> getDateString(taskItemDto)
          }
        )).apply(in)
    }
  }

  /**
   * Renders the tasks selectable by the user.
   */
  def selectableTasks(in: NodeSeq): NodeSeq = {
    if (tasks.isEmpty) {
      <lift:embed what="no_data"/>
    } else {
      tasks.toSeq.flatMap(
        showTaskData => {
          val color = Color.get(showTaskData.task.name.get, showTaskData.projectName, true)

          val taskStyleClass = if (showTaskData.task.specifiable.get) {
            Text("hiddenTask specifiable-task");
          } else {
            Text("hiddenTask");
          }

          Helpers.bind("task", in,
            AttrBindParam("taskstyleclass", taskStyleClass, "class"),
            AttrBindParam("placeholder",
              if (offsetInDays == 0) Text("hh:mm")
              else Text("hh:mm"), "placeholder"),
            AttrBindParam("colorindicator",
              Text("background-color:rgba" + color.toString),
              "style"),
            AttrBindParam("projectcolorindicator",
              Text("background-color:" + ProjectService.getRootProject(showTaskData.rootProject).color.get),
              "style"),
            // Form sending without submit button
            AttrBindParam("formid", Text("tskf_" + showTaskData.task.id.get), "id"),
            AttrBindParam("rowid", Text("tskr_" + showTaskData.task.id.get), "id"),
            AttrBindParam("taskid", Text(showTaskData.task.id.get + ""), "value"),
            AttrBindParam("onrowclick",
              if (offsetInDays == 0) Text("sendForm('tskf_" + showTaskData.task.id.get + "', true)")
              else Text("sendForm('tskf_" + showTaskData.task.id.get + "', false)"), "onclick"),

            "taskname" -> showTaskData.task.name.get,
            "projectname" -> showTaskData.projectName,
            "taskdescription" -> PCDataXmlParser.apply(TaskService.getPreparedDescription(showTaskData.task)).getOrElse(Text("")))
        }
      )
    }
  }

  /**
   * Renders today node, if the selected day is today,
   * otherwise renders past node.
   */
  def renderByDate(in: NodeSeq): NodeSeq = {
    val todayNode = (in \ "today")
    val pastNode = (in \ "past")
    if (offsetInDays == 0) {
      todayNode \ "_"
    } else {
      pastNode \ "_"
    }
  }

  /**
   * Process actions related to task items.
   * (Append, edit, delete, sum.)
   */
  def actions(in: NodeSeq): NodeSeq = {
    if (S.post_?) {
      // select task id param
      val selectedTaskId = S.param("selecttaskid").get.toLong

      // operation type
      val mode = S.param("mode").getOrElse("default")

      if (mode == "default") {
        /*
         * DEFAULT (smart) mode - Append to current day or insert to given time of the selected day.
         * The given timeoffset parameter can be blank, it will be interpreted as 0.
         *
         * If the parameter is a number,
         * 	the new taskitem's date will be the current date minus the given number of minutes.
         * 	the taskitem will be appended to today's tasks.
         * 	if not today is selected, rises an error.
         *
         * Else,
         * 	the parameter will be parsed as hh:mm, and will be interpreted as time of the selected day.
         * 	the taskitem will be inserted to the selected day at the given point of time
         */
        val offsetStringParameter = {
          val offsetParameterString = (S.param("timeoffset") getOrElse "0")
          if (0 < offsetParameterString.length())
            offsetParameterString
          else
            "0"
        }

        val time = try {
          val offsetParameter = {
            if (offsetStringParameter.contains(":")) {
              val hour = offsetStringParameter.substring(0, offsetStringParameter.indexOf(":")).toInt
              val min = offsetStringParameter.substring(offsetStringParameter.indexOf(":") + 1, offsetStringParameter.length).toInt
              TimeUtils.getDeltaFrom(hour, min, offsetInDays)
            } else {
              math.abs(offsetStringParameter.toInt) * 60L * 1000L
            }
          }
          Some(TimeUtils.currentTime - offsetParameter)
        } catch {
          case e: Exception => {
            None
          }
        }

        val preciseTimeMode = offsetStringParameter.contains(":")

        if (!time.isEmpty) {
          // valid parameters, make changes

          // Create new task if necessary
          val newTaskName = S.param("newtaskname").getOrElse("")

          val calculatedTaskId = if (!newTaskName.isEmpty) {
            TaskService.specify(TaskService.getTask(selectedTaskId).get, newTaskName).id.get
          } else {
            selectedTaskId;
          }

          if (preciseTimeMode) {
            // precise time given, inserting
            TaskItemService.insertTaskItem(calculatedTaskId, time.get)
          } else {
            // diff time given, appending
            if (offsetInDays != 0) {
              S.error(S.?("tasks.error.previous_day_precise_only"))
              S.redirectTo(S.uri)
            } else {
              TaskItemService.appendTaskItem(calculatedTaskId, time.get)
            }
          }

        } else {
          // invalid parameters, show error
          S.error(S.?("tasks.error.wrong_timeformat_general"))
          S.redirectTo(S.uri)
        }
      } else if (mode == "taskitemsplit" || mode == "taskitemedit") {
        /*
         * SPLIT or EDIT mode.
         * The given timeoffset parameter must be in hh:mm format, and will be interpreted as time of the current day.
         */

        // time offset parameter
        val offset = {
          val timeOffset = S.param("timeoffset")
          try {
            val offsetStringParameter = (timeOffset getOrElse "0")
            if (offsetStringParameter.contains(":")) {
              val hour = offsetStringParameter.substring(0, offsetStringParameter.indexOf(":")).toInt
              val min = offsetStringParameter.substring(offsetStringParameter.indexOf(":") + 1, offsetStringParameter.length).toInt
              Some(TimeUtils.getDeltaFrom(hour, min, offsetInDays))
            } else {
              None
            }
          } catch {
            case e: Exception => {
              None
            }
          }
        }

        if (!offset.isEmpty) {
          // valid parameters, make changes
          val selectedTime = TimeUtils.currentTime - offset.get

          val selectedTaskItemId = S.param("taskitemid")

          if (mode == "taskitemedit") {
            TaskItemService.editTaskItem(selectedTaskItemId.get.toLong, selectedTaskId, selectedTime)
          } else if (mode == "taskitemsplit") {
            TaskItemService.editTaskItem(selectedTaskItemId.get.toLong, selectedTaskId, selectedTime, true)
          }
        } else {
          // invalid parameters, show error
          S.error(S.?("tasks.error.wrong_timeformat_timestamp"))
          S.redirectTo(S.uri)
        }
      } else if (mode == "taskitemdelete") {
        /*
         * The specified taskitem will be deleted.
         */
        TaskItemService.deleteTaskItem(S.param("taskitemid").get.toLong)
      }

      TaskItemService.normalizeTaskItems(offsetInDays);

      S.redirectTo(S.uri)
    } else {
      NodeSeq.Empty
    }
  }

  /**
   * Constructs date stings.
   */
  private def getDateString(taskItemDto: TaskItemWithDuration) = {
    if (taskItemDto.taskItem.id.get == -1) {
      val firstOfTheDay = (taskItemDto.taskItem.id == taskItems.head.taskItem.id)
      if (firstOfTheDay) {
        S.?("tasks.from_prev_day")
      } else {
        S.?("tasks.to_next_day")
      }
    } else {
      taskItemDto.timeString
    }
  }
}
