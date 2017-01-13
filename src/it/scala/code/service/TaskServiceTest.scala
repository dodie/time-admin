package code.service

import code.model.{Project, Task, TaskItem}
import code.test.utils.BaseSuite
import net.liftweb.mapper.By

class TaskServiceTest extends BaseSuite {

  it("only specifiable tasks can be specified") {
    val project = Project.create.name("Any Project")
    project.save()
    val unspecifiableTask = Task.create.name("Any Task").parent(project).active(true)
    unspecifiableTask.save()

    intercept[RuntimeException] {
      TaskService.specify(unspecifiableTask, "Any Specified Task")
    }
  }

  it("specify task for the first time") {
    val project = Project.create.name("My Project")
    project.save()
    val genericSupportTask = Task.create.name("Support").parent(project).active(true).specifiable(true)
    genericSupportTask.save()

    val specificSupportTask = TaskService.specify(genericSupportTask, "fix random bug")

    assert(specificSupportTask.name == "fix random bug")
    assert(getProject(specificSupportTask.parent.get).name == "Support")
    assert(getProject(getProject(specificSupportTask.parent.get).parent.get) == project)
  }

  it("specify multiple subtasks") {
    val project = Project.create.name("My Project")
    project.save()
    val genericSupportTask = Task.create.name("Support").parent(project).active(true).specifiable(true)
    genericSupportTask.save()

    val specificSupportTask = TaskService.specify(genericSupportTask, "fix random bug")
    assert(specificSupportTask.name == "fix random bug")
    assert(getProject(specificSupportTask.parent.get).name == "Support")
    assert(getProject(getProject(specificSupportTask.parent.get).parent.get) == project)

    val anotherSpecificSupportTask = TaskService.specify(genericSupportTask, "fix another bug")
    assert(anotherSpecificSupportTask.name == "fix another bug")
    assert(getProject(anotherSpecificSupportTask.parent.get) == getProject(specificSupportTask.parent.get))
  }

  it("specify task with same name multiple times") {
    val project = Project.create.name("My Project")
    project.save()
    val genericSupportTask = Task.create.name("Support").parent(project).active(true).specifiable(true)
    genericSupportTask.save()

    val specificSupportTask = TaskService.specify(genericSupportTask, "fix random bug")

    assert(specificSupportTask.name == "fix random bug")
    assert(getProject(specificSupportTask.parent.get).name == "Support")
    assert(getProject(getProject(specificSupportTask.parent.get).parent.get) == project)

    val accidentallyReSpecifiedSupportTask = TaskService.specify(genericSupportTask, "fix random bug")

    assert(specificSupportTask == accidentallyReSpecifiedSupportTask)
  }

  it("merge removes merged Task") {
    val project = Project.create.name("My Project")
    project.save()
    val mainTask = Task.create.name("Main task").parent(project)
    mainTask.save()
    val taskToBeMerged = Task.create.name("Task to be merged").parent(project)
    taskToBeMerged.save()

    TaskService.merge(taskToBeMerged, mainTask)

    assert(Task.find(By(Task.id, taskToBeMerged.id.get)).isEmpty)
  }

  it("merge transfers TaskItems to the target Task") {
    val project = Project.create.name("My Project")
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

  def getProject(id: Long): Project = Project.find(By(Project.id, id)).openOrThrowException("project not found")
}
