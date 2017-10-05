package code.api

import net.liftweb.http.rest.RestHelper
import net.liftweb.json.JsonAST.JObject
import net.liftweb.util.Helpers.AsLong
import net.liftweb.json.JsonAST.JField
import net.liftweb.json.JsonAST.JString
import code.service.TaskService
import net.liftweb.json._
import net.liftweb.json.Extraction._
import code.service.Color
import code.service.TaskItemService
import code.service.TaskItemService.IntervalQuery
import code.model.User
import net.liftweb.mapper.By
import org.joda.time.LocalDate
import org.joda.time.Interval
import com.github.nscala_time.time.Imports._
import net.liftweb.common.Box
import code.model.ExtSession

/**
 * A basic REST API to provide access to Timeadmin functions.
 *
 * The endpoint can be toggled via the EXPOSE_TIMEADMIN_API environment variable.
 * By default, it's not enabled.
 * 
 * @see https://github.com/dodie/time-admin/blob/master/api-reference.md
 */
object Endpoints extends RestHelper with ClientsOnly {
  case class TaskDto(id: Long, taskName: String, projectName: String, fullName: String, color: Color)
  case class TaskItemDto(id: Long, taskId: Long, start: Long, duration: Long, user: Long)

  def getLong(jsonData: JValue, name: String) = {
    (jsonData \ name).asInstanceOf[JString].values.toLong
  }
  
  def getString(jsonData: JValue, name: String) = {
    (jsonData \ name).asInstanceOf[JString].values
  }
  
  val OK_RESPONSE = Some(JObject(JField("status", JString("OK"))))
  val ERROR_RESPONSE = Some(JObject(JField("status", JString("ERROR"))))
  
  val dateRange = """(\d\d\d\d)(\d\d)(\d\d)-(\d\d\d\d)(\d\d)(\d\d)""".r
  
  def date(year: String, monthOfYear: String, dayOfMonth: String): LocalDate = new LocalDate(year.toInt, monthOfYear.toInt, dayOfMonth.toInt)
  def interval(s : LocalDate, e:LocalDate) = new Interval(s.toInterval.start, e.toInterval.end)
  
  def user(): Box[User] = User.currentUser
  
  serve {
    clientsOnly {
      case "api" :: "login" :: Nil JsonPost ((jsonData, req)) => {
        val email = getString(jsonData, "email")
        val password = getString(jsonData, "password")
        
        if (User.canLogin(email, password)) {
          val extSession = ExtSession.create.userId(user.openOrThrowException("Current user must be defined!").userIdAsString).saveMe
      	  Some(JObject(JField("token", JString(extSession.cookieId.get))))
        } else {
          ERROR_RESPONSE
        }
      }
      
      case "api" :: "logout" :: Nil JsonPost ((jsonData, req)) => {
        val token = getString(jsonData, "token")
        
        ExtSession.find(By(ExtSession.cookieId, token)).foreach(_.delete_!)
        OK_RESPONSE
      }
      
      case "api" :: "tasks" :: Nil JsonGet req => {
        decompose(
            TaskService.getAllActiveTasks
              .map(task => TaskDto(task.task.id.get, task.taskName, task.projectName, task.fullName, task.color)))
      }
      
      case "api" :: "taskitems" :: dateRange(startYear, startMonth, startDay, endYear, endMonth, endDay) :: Nil JsonGet req => {
        val start = date(startYear, startMonth, startDay)
        val end = date(endYear, endMonth, endDay)
        val intervalQuery = IntervalQuery(interval(start, end))
        
        decompose(
            TaskItemService.getTaskItems(intervalQuery, user)
              .map(taskItem => TaskItemDto(taskItem.taskItem.id.get, taskItem.taskItem.task.get, taskItem.taskItem.start.get, taskItem.duration.getMillis, taskItem.taskItem.user.get)))
      }
      
      case "api" :: "taskitems" :: Nil JsonPost ((jsonData, req)) => {
        val taskId = getLong(jsonData, "taskId")
        val time = getLong(jsonData, "time")
        
        TaskItemService.insertTaskItem(taskId, time, user)
        OK_RESPONSE
      }
      
      case "api" :: "taskitems" :: AsLong(taskItemId) :: Nil JsonPut ((jsonData, req)) => {
        val taskId = getLong(jsonData, "taskId")
        val time = getLong(jsonData, "time")
        
        TaskItemService.editTaskItem(taskItemId, taskId, time, false, user)
        OK_RESPONSE
      }
      
      case "api" :: "taskitems" :: AsLong(taskItemId) :: Nil JsonDelete req => {
        TaskItemService.deleteTaskItem(taskItemId, user)
        OK_RESPONSE
      }
    }
  }
}
