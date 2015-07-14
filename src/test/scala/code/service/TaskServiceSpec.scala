package code.snippet

import code.model.User
import net.liftweb.http.{ S, LiftSession }
import net.liftweb.mapper.BaseMetaMapper
import org.scalatest.FlatSpec
import org.scalatest._
import code.service.TaskService

class TaskServiceSpec extends FlatSpec with Matchers {

  "getColor" must "produce a deterministic color for a task" in {
    val taskName = "A Task"
    val projectName = "A Project"
    val active = true

    val color1 = TaskService.getColor(taskName, projectName, active)
    val color2 = TaskService.getColor(taskName, projectName, active)

    color1 should equal(color2)
  }

}

