package code
package service

import java.util.Random
import java.text.Collator

import scala.collection.mutable.ListBuffer
import scala.util.Sorting
import code.model.TaskItem
import code.model.Project
import code.model.Task
import net.liftweb.common.Box
import net.liftweb.mapper.MappedForeignKey.getObj
import net.liftweb.mapper.By
import net.liftweb.util.Props
import net.liftweb.http.S

/**
 * Task data transformation service.
 * @author David Csakvari
 */
object TaskService {

  def getTask(id: Long): Box[Task] = Task.findByKey(id)

  private def getAllTasks(activeOnly: Boolean): List[ShowTaskData] = {
    val taskDtos = new ListBuffer[ShowTaskData]
    val rootProjects = Project.findAll.filter(_.parent.isEmpty)

    def addAllTasksForSubProject(rootProject: Project, project: Project, parentsName: String): Unit = {
      if (!activeOnly || project.active.get) {
        val tasks = if (activeOnly) {
          Task.findAll(By(Task.parent, Project.find(By(Project.id, project.id.get))), By(Task.active, true))
        } else {
          Task.findAll(By(Task.parent, Project.find(By(Project.id, project.id.get))))
        }

        for (task <- tasks) {
          taskDtos.append(ShowTaskData(task, rootProject, parentsName))
        }
        for (project <- Project.findAll(By(Project.parent, Project.find(By(Project.id, project.id.get)).openOrThrowException("Project must be defined!")))) {
          addAllTasksForSubProject(rootProject, project, parentsName + "-" + project.name)
        }
      }
    }

    for (rootProject <- rootProjects) {
      addAllTasksForSubProject(rootProject, rootProject, rootProject.name.get)
    }
    taskDtos.toList
  }

  def getTaskArray(taskFilter: String = "", activeOnly: Boolean = true): Array[ShowTaskData] = {
    val taskList = getAllTasks(activeOnly)
      .filter(taskDto => {
        taskFilter
          .split(",")
          .exists(filter =>
            taskDto.task.name.get.toLowerCase.contains(filter.toLowerCase.trim) ||
              taskDto.projectName.toLowerCase.contains(filter.toLowerCase.trim))
      })
      .map(taskDto =>
        ShowTaskData(taskDto.task, taskDto.rootProject, taskDto.projectName))
      .toArray

    Sorting.quickSort(taskList)
    taskList
  }

  def getPreparedDescription(task: Task): String = {
    val pattern = "M#(\\d*)".r
    val mantisConfig = Props.get("mantis.bugs.view.url")
    val taskDescription = "<span>" + task.description.get + "</span>"

    if (mantisConfig.isEmpty) {
      taskDescription
    } else {
      val mantisURL = mantisConfig.openOrThrowException("Props must be defined!")
      pattern.replaceAllIn(taskDescription, m => "<a href=\"" + mantisURL + m.group(1) + "\">Mantis #" + m.group(1) + "</a>")
    }
  }

  def move(what: Task, newParent: Project): Boolean = {
    what.parent(newParent).save
  }

  def isEmpty(task: Task): Boolean = TaskItem.findAll(By(TaskItem.task, task)).isEmpty

  def delete(task: Task): Boolean =
    if (task.active.get)
      task.active(false).save
    else if (isEmpty(task))
      task.delete_!
    else
      throw new IllegalArgumentException("Tasks with TaskItems can not be deleted.")

  def merge(what: Task, into: Task): Boolean = {
    TaskItem.findAll(By(TaskItem.task, what)).foreach((ti: TaskItem) => ti.task(into).save)
    delete(what)
  }

  def specify(task: Task, taskName: String): Task = {
    if (!task.specifiable.get) {
      throw new RuntimeException("Task is not specifiable!")
    }
    val parent = Project.find(By(Project.parent, task.parent.get), By(Project.name, task.name.get))
    val parentProject = if (parent.isEmpty) {
      val rootProject = Project.find(By(Project.id, task.parent.get)).openOrThrowException("Project must be defined!")
      val newParent = Project.create.name(task.name.get).active(true).parent(rootProject)
      newParent.save
      newParent
    } else {
      if (!parent.openOrThrowException("Project must be defined!").active.get) {
        parent.openOrThrowException("Project must be defined!").active(true).save
      }
      parent.openOrThrowException("Project must be defined!")
    }

    val targetTask = Task.find(By(Task.parent, parentProject.id.get), By(Task.name, taskName))
    if (targetTask.isEmpty) {
      val specifiedTask = Task.create.name(taskName).active(true).specifiable(true).parent(parentProject)
      specifiedTask.save
      specifiedTask
    } else {
      if (!targetTask.openOrThrowException("Task must be defined!").active.get) {
        targetTask.openOrThrowException("Task must be defined!").active(true).save
      }
      targetTask.openOrThrowException("Task must be defined!")
    }
  }
}

/**
 * Task wrapper that contains the display name of the whole parent project structure, and comparable.
 */
case class ShowTaskData(task: Task, rootProject: Project, projectName: String) extends Ordered[ShowTaskData] {
  def collator: Collator = Collator.getInstance(S.locale)
  def compare(that: ShowTaskData): Int = collator.compare(getFullName, that.getFullName)
  def getFullName: String = projectName + "-" + task.name.get
}

