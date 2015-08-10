package code
package service

import code.model.Project
import code.model.Task
import net.liftweb.mapper.By
import net.liftweb.common.{Box, Full, Empty}

object ProjectService {

  def getDisplayName(project: Project): String = {
    def loop(z: List[Project], box: Box[Project]): List[Project] = box match {
      case Full(p) => loop(p :: z, Project.findByKey(p.parent.get))
      case _ => z
    }
    loop(Nil, Full(project)).map(_.name).mkString("-")
  }

  def move(what: Project, newParent: Project) = what.parent(newParent).save

  def moveToRoot(what: Project) = what.parent(Empty).save

  def isEmpty(project: Project) = Task.findAll(By(Task.parent, project)).isEmpty && Project.findAll(By(Project.parent, project)).isEmpty

  def delete(project: Project) = {
    if (isEmpty(project)) {
      project.delete_!
    } else {
      throw new IllegalArgumentException("Projects with Tasks or Subprojects can not be deleted.");
    }
  }

}
