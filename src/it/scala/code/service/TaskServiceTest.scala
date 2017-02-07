package code.service

import code.model.{Task, TaskItem}
import code.test.utils.BaseSuite
import net.liftweb.mapper.By

import scala.language.postfixOps

class TaskServiceTest extends BaseSuite {

  it("Get all active tasks") {
    val ts = TaskService.getAllActiveTasks
    ts map (_.fullName) shouldBe List("p1-p12-t4", "p1-t1", "p1-t2", "p2-t5", "p2-t6")
  }

  it("only specifiable tasks can be specified") {
    val project = Task.create.name("Any Project")
    project.save()
    val unspecifiableTask = Task.create.name("Any Task").parent(project).active(true)
    unspecifiableTask.save()

    intercept[RuntimeException] {
      TaskService.specify(unspecifiableTask, "Any Specified Task")
    }
  }

  it("specify task for the first time") {
    val project = Task.create.name("My Project")
    project.save()
    val genericSupportTask = Task.create.name("Support").parent(project).active(true).specifiable(true)
    genericSupportTask.save()

    val specificSupportTask = TaskService.specify(genericSupportTask, "fix random bug")

    assert(specificSupportTask.name.get == "fix random bug")
    assert(getProject(specificSupportTask.parent.get).name.get == "Support")
    assert(getProject(getProject(specificSupportTask.parent.get).parent.get) == project)
  }

  it("specify multiple subtasks") {
    val project = Task.create.name("My Project")
    project.save()
    val genericSupportTask = Task.create.name("Support").parent(project).active(true).specifiable(true)
    genericSupportTask.save()

    val specificSupportTask = TaskService.specify(genericSupportTask, "fix random bug")
    assert(specificSupportTask.name.get == "fix random bug")
    assert(getProject(specificSupportTask.parent.get).name.get == "Support")
    assert(getProject(getProject(specificSupportTask.parent.get).parent.get) == project)

    val anotherSpecificSupportTask = TaskService.specify(genericSupportTask, "fix another bug")
    assert(anotherSpecificSupportTask.name.get == "fix another bug")
    assert(getProject(anotherSpecificSupportTask.parent.get) == getProject(specificSupportTask.parent.get))
  }

  it("specify task with same name multiple times") {
    val project = Task.create.name("My Project")
    project.save()
    val genericSupportTask = Task.create.name("Support").parent(project).active(true).specifiable(true)
    genericSupportTask.save()

    val specificSupportTask = TaskService.specify(genericSupportTask, "fix random bug")

    assert(specificSupportTask.name.get == "fix random bug")
    assert(getProject(specificSupportTask.parent.get).name.get == "Support")
    assert(getProject(getProject(specificSupportTask.parent.get).parent.get) == project)

    val accidentallyReSpecifiedSupportTask = TaskService.specify(genericSupportTask, "fix random bug")

    assert(specificSupportTask == accidentallyReSpecifiedSupportTask)
  }

  it("merge removes merged Task") {
    val project = Task.create.name("My Project")
    project.save()
    val mainTask = Task.create.name("Main task").parent(project)
    mainTask.save()
    val taskToBeMerged = Task.create.name("Task to be merged").parent(project)
    taskToBeMerged.save()

    TaskService.merge(taskToBeMerged, mainTask)

    assert(Task.find(By(Task.id, taskToBeMerged.id.get)).isEmpty)
  }

  it("merge transfers TaskItems to the target Task") {
    val project = Task.create.name("My Project")
    project.save()
    val mainTask = Task.create.name("Main task").parent(project)
    mainTask.save()
    val taskToBeMerged = Task.create.name("Task to be merged").parent(project)
    taskToBeMerged.save()
    val taskItem = TaskItem.create.task(mainTask)
    taskItem.save

    TaskService.merge(taskToBeMerged, taskToBeMerged)

    assert(taskItem.task.get == mainTask.id.get)
  }

  it("The top level project is not empty") {
    assert(!TaskService.isEmpty(projectByName("top")))
  }

  it("The bottom level project is empty") {
    assert(TaskService.isEmpty(projectByName("bottom")))
  }

  it("Delete a bottom level project") {
    assert(TaskService.delete(projectByName("bottom")))
  }

  it("Delete an active top level project") {
    TaskService.delete(projectByName("top"))
    assert(!projectByName("top").active.get)
  }

  it("Try to delete an inactivated top level project") {
    val me = projectByName("top").active(false).saveMe()
    intercept[IllegalArgumentException] {
      TaskService.delete(me)
    }
  }

  def projectByName(n: String): Task = Task.find(By(Task.name, n)).openOrThrowException("Test entity must be presented!")


  def getProject(id: Long): Task = Task.find(By(Task.id, id)).openOrThrowException("project not found")

  given {
    traverse(
      project("top",
        project("any project"),
        project("middle",
          project("bottom"))),
      project("p1",
        task("t1"),
        task("t2"),
        project("p11", active = false,
          task("t3")),
        project("p12",
          task("t4"))),
      project("p2",
        task("t5"),
        task("t6"),
        task("t7", active = false))) foreach (_.save())
  }
}
