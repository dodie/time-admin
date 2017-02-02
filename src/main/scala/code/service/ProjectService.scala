package code
package service

import code.model.Task
import net.liftweb.mapper.By
import net.liftweb.common.{Box, Full, Empty}

object ProjectService {

  def getDisplayName(project: Task): String = projectPath(Nil, Full(project)).map(_.name).mkString("-")

  private def projectPath(z: List[Task], project: Box[Task]): List[Task] = project match {
    case Full(p) => projectPath(p :: z, Task.findByKey(p.parent.get))
    case _ => z
  }

  def getRootProject(project: Task): Task = Task.findByKey(project.parent.get) match {
    case Full(parent) => getRootProject(parent)
    case _ => project
  }

  def move(project: Task, parent: Task): Boolean = project.parent(parent).save()

  def moveToRoot(project: Task): Boolean = project.parent(Empty).save()

  def isEmpty(project: Task): Boolean = Task.findAll(By(Task.parent, project)).isEmpty

  def delete(project: Task): Boolean =
    if (project.active.get)
      project.active(false).save
    else if (isEmpty(project))
      project.delete_!
    else
      throw new IllegalArgumentException("Projects with tasks or subprojects can not be deleted.")

}
