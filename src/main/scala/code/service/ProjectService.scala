package code
package service

import code.model.{Project, Task}
import net.liftweb.common.Empty
import net.liftweb.mapper.By

object ProjectService {
  def move(project: Project, parent: Project): Boolean = project.parent(parent).save()

  def moveToRoot(project: Project): Boolean = project.parent(Empty).save()

  def isEmpty(project: Project): Boolean = Task.findAll(By(Task.parent, project)).isEmpty && Project.findAll(By(Project.parent, project)).isEmpty

  def delete(project: Project): Boolean =
    if (project.active.get)
      project.active(false).save
    else if (isEmpty(project))
      project.delete_!
    else
      throw new IllegalArgumentException("Projects with tasks or subprojects can not be deleted.")

}
