package code
package service

import code.model.Project
import code.model.Task
import net.liftweb.mapper.By
import net.liftweb.common.{Box, Full, Empty}

object ProjectService {

  def getDisplayName(project: Project): String = projectPath(Nil, Full(project)).map(_.name).mkString("-")
  
  private def projectPath(z: List[Project], project: Box[Project]): List[Project] = project match {
    case Full(p) => projectPath(p :: z, Project.findByKey(p.parent.get))
    case _ => z
  }

  def move(project: Project, parent: Project) = project.parent(parent).save()

  def moveToRoot(project: Project) = project.parent(Empty).save()

  def isEmpty(project: Project) = Task.findAll(By(Task.parent, project)).isEmpty && Project.findAll(By(Project.parent, project)).isEmpty

  def delete(project: Project) =
    if (isEmpty(project)) project.delete_!
    else throw new IllegalArgumentException("Projects with tasks or subprojects can not be deleted.")

}
