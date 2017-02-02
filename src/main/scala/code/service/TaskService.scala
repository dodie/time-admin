package code
package service

import java.text.Collator

import code.model.{Task, TaskItem}
import code.service.HierarchicalItemService.path
import net.liftweb.common.{Box, Full}
import net.liftweb.http.S
import net.liftweb.mapper.By
import net.liftweb.util.Props

import scala.language.postfixOps

/**
 * Task data transformation service.
 * @author David Csakvari
 */
object TaskService {
  def getTask(id: Long): Box[Task] = Task.findByKey(id)

  def getAllActiveTasks: List[ShowTaskData] = {
    val ts = Task.findAll(By(Task.active, true))

    ts map { t =>
      ShowTaskData(t, path(Nil, t.parent.box, ts))
    } filter { t =>
      t.path forall(_.active.get)
    } sorted
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

  def move(what: Task, newParent: Task): Boolean = {
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

    val targetTask = Task.find(By(Task.parent, task.id.get), By(Task.name, taskName))
    if (targetTask.isEmpty) {
      val specifiedTask = Task.create.name(taskName).active(true).specifiable(true).selectable(true).parent(task)
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
 * Task wrapper that contains the display name of the whole parent structure, and comparable.
 */
abstract class TaskDto[T <: TaskDto[_]](task: Box[Task], path: List[Task]) extends Ordered[T] {
  lazy val taskName: String = task map (_.name.get) getOrElse ""
  lazy val projectName: String = path map (_.name.get) mkString "-"
  lazy val fullName: String = task map { t => s"$projectName-${t.name.get}" } getOrElse ""

  lazy val color: Color = Color.get(taskName, projectName, task exists (_.active.get))
  lazy val baseColor: Color = path.headOption map (_.color.get) flatMap Color.parse getOrElse Color.transparent

  private lazy val collator = Collator.getInstance(S.locale)
  def compare(that: T): Int = collator.compare(fullName, that.fullName)
}

case class ShowTaskData(task: Task, path: List[Task]) extends TaskDto[ShowTaskData](Full(task), path)

