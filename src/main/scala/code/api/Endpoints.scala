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

// TODO: autentikáció, taskitemek szűkítése aktuális userre
// TODO: hibakezelés, stb.
object Endpoints extends RestHelper {
  case class TaskDto(id: Long, taskName: String, projectName: String, fullName: String, color: Color)
  case class TaskItemDto(id: Long, taskId: Long, start: Long, duration: Long, user: Long)

  def getLong(jsonData: JValue, name: String) = {
    (jsonData \ name).asInstanceOf[JString].values.toLong
  }
  
  val dateRange = """(\d\d\d\d)(\d\d)(\d\d)-(\d\d\d\d)(\d\d)(\d\d)""".r
  
  def date(year: String, monthOfYear: String, dayOfMonth: String): LocalDate = new LocalDate(year.toInt, monthOfYear.toInt, dayOfMonth.toInt)
  def interval(s : LocalDate, e:LocalDate) = new Interval(s.toInterval.start, e.toInterval.end)
  
  serve {
    //curl http://localhost:8080/api/tasks
    case "api" :: "tasks" :: Nil JsonGet req => {
      decompose(
          TaskService.getAllActiveTasks
            .map(task => TaskDto(task.task.id.get, task.taskName, task.projectName, task.fullName, task.color)))
    }
    
    //curl http://localhost:8080/api/taskitems/20170601-20180602
    case "api" :: "taskitems" :: dateRange(startYear, startMonth, startDay, endYear, endMonth, endDay) :: Nil JsonGet req => {
      val start = date(startYear, startMonth, startDay)
      val end = date(endYear, endMonth, endDay)
      val intervalQuery = IntervalQuery(interval(start, end))
      
      val userId = 1L// TODO: from actual user
      val user = User.find(By(User.id, userId))
      
      decompose(
          TaskItemService.getTaskItems(intervalQuery, user)
            .map(taskItem => TaskItemDto(taskItem.taskItem.id.get, taskItem.taskItem.task.get, taskItem.taskItem.start.get, taskItem.duration.getMillis, taskItem.taskItem.user.get)))
    }
    
    //curl -X POST -H "Content-Type: application/json" -d '{"taskId":"2", "time":"1506601655521"}' http://localhost:8080/api/taskitems
    case "api" :: "taskitems" :: Nil JsonPost ((jsonData, req)) => {
      val userId = 1L// TODO: from actual user
      val taskId = getLong(jsonData, "taskId")
      val time = getLong(jsonData, "time")
      
      TaskItemService.insertTaskItem(taskId, time, User.find(By(User.id, userId)))
      Some(JObject(JField("status", JString("OK"))))
    }
    
    //curl -X PUT -H "Content-Type: application/json" -d '{"taskId":"2", "time":"1506601655521"}' http://localhost:8080/api/taskitems/123
    case "api" :: "taskitems" :: AsLong(taskItemId) :: Nil JsonPut ((jsonData, req)) => {
      val userId = 1L // TODO: from actual user
      val taskId = getLong(jsonData, "taskId")
      val time = getLong(jsonData, "time")
      
      TaskItemService.editTaskItem(taskItemId, taskId, time, false, User.find(By(User.id, userId)))
      Some(JObject(JField("status", JString("OK"))))
    }
    
    //curl -X DELETE http://localhost:8080/api/taskitems/123
    case "api" :: "taskitems" :: AsLong(taskItemId) :: Nil JsonDelete req => {
      val userId = 1L// TODO: from actual user
      val user = User.find(By(User.id, userId))
      TaskItemService.deleteTaskItem(taskItemId, user)
      Some(JObject(JField("status", JString("OK"))))
    }
  }
}