package code
package service

import java.util.Random
import java.text.Collator
import scala.collection.mutable.ListBuffer
import scala.util.Sorting
import code.model.TaskItem
import code.model.Project
import code.model.Task
import code.model.UserPreference
import net.liftweb.common.Box.box2Option
import net.liftweb.common.Box
import net.liftweb.mapper.MappedField.mapToType
import net.liftweb.mapper.MappedForeignKey.getObj
import net.liftweb.mapper.By
import net.liftweb.util.Props
import net.liftweb.http.S

/**
 * Task data transformation service.
 * @author David Csakvari
 */
object TaskService {

  def getTask(id: Long) = Task.findByKey(id)

  def getColor(taskName: String, projectsDisplayName: String, active: Boolean): (Int, Int, Int) = {
    val random = new Random((taskName.trim + projectsDisplayName.trim).hashCode)
    val red = if (active) random.nextInt(255) else 255
    val green = if (active) random.nextInt(255) else 255
    val blue = if (active) random.nextInt(255) else 255
    (red, green, blue)
  }

  private def getAllTasks(activeOnly: Boolean): List[ShowTaskData] = {
    val taskDtos = new ListBuffer[ShowTaskData]
    val rootProjects = Project.findAll.filter(_.parent.isEmpty)

    def addAllTasksForSubProject(project: Project, parentsName: String): Unit = {
      if (!activeOnly || project.active.get) {
        val tasks = if (activeOnly) {
          Task.findAll(By(Task.parent, Project.find(By(Project.id, project.id.get))), By(Task.active, true))
        } else {
          Task.findAll(By(Task.parent, Project.find(By(Project.id, project.id.get))))
        }

        for (task <- tasks) {
          taskDtos.append(ShowTaskData(task, parentsName))
        }
        for (project <- Project.findAll(By(Project.parent, Project.find(By(Project.id, project.id.get)).get))) {
          addAllTasksForSubProject(project, parentsName + "-" + project.name)
        }
      }
    }

    for (rootProject <- rootProjects) {
      addAllTasksForSubProject(rootProject, rootProject.name.get)
    }
    taskDtos.toList
  }

  def getTaskArray(taskFilter: String = "", activeOnly: Boolean = true) = {
    val taskList = getAllTasks(activeOnly)
      .filter(taskDto => {
        taskFilter
          .split(",")
          .exists(filter =>
            taskDto.task.name.get.toLowerCase.contains(filter.toLowerCase.trim) ||
              taskDto.projectName.toLowerCase.contains(filter.toLowerCase.trim))
      })
      .map(taskDto =>
        ShowTaskData(taskDto.task, taskDto.projectName))
      .toArray

    Sorting.quickSort(taskList)
    taskList
  }

  def getPreparedDescription(task: Task): String = {
    val pattern = "M#(\\d*)".r
    val mantisConfig = Props.get("mantis.bugs.view.url");
    val taskDescription = "<span>" + task.description.get + "</span>"

    if (mantisConfig.isEmpty) {
      taskDescription;
    } else {
      val mantisURL = mantisConfig.get
      pattern.replaceAllIn(taskDescription, m => ("<a href=\"" + mantisURL + m.group(1) + "\">Mantis #" + m.group(1) + "</a>"))
    }
  }

  def move(what: Task, newParent: Project) = {
    what.parent(newParent).save
  }

  def isEmpty(task: Task) = TaskItem.findAll(By(TaskItem.task, task)).isEmpty

  def delete(task: Task) = {
    if (isEmpty(task)) {
      task.delete_!
    } else {
      throw new IllegalArgumentException("Tasks with TaskItems can not be deleted.");
    }
  }
}

/**
 * Task wrapper that contains the display name of the whole parent project structure, and comparable.
 */
case class ShowTaskData(task: Task, projectName: String) extends Ordered[ShowTaskData] {
  def collator = Collator.getInstance(S.locale);
  def compare(that: ShowTaskData) = collator.compare(getFullName(), that.getFullName())
  def getFullName(): String = projectName + "-" + task.name.get
}

