package code
package service

import code.model.Project
import code.model.Task
import net.liftweb.mapper.By
import net.liftweb.common.{Box, Full, Empty}

object ProjectService {

  def getDisplayName(p: Project): String = {
    def path(z: List[Project], box: Box[Project]): List[Project] = box match {
      case Full(pr) => path(pr :: z, Project.findByKey(pr.parent.get))
      case _ => z
    }
    path(Nil, Full(p)).map(_.name).mkString("-")
  }

  def move(p: Project, pp: Project) = p.parent(pp).save()

  def moveToRoot(p: Project) = p.parent(Empty).save()

  def isEmpty(p: Project) = Task.findAll(By(Task.parent, p)).isEmpty && Project.findAll(By(Project.parent, p)).isEmpty

  def delete(p: Project) =
    if (isEmpty(p)) p.delete_!
    else throw new IllegalArgumentException("Projects with tasks or subprojects can not be deleted.")

}
