package code
package service

import code.model.Project
import code.model.Task
import net.liftweb.mapper.By
import net.liftweb.common.Empty

object ProjectService {

  def getDisplayName(project: Project): String = {
    val parentProject = Project.findByKey(project.parent.get)
    if (parentProject.isEmpty) {
      project.name.get
    } else {
      getDisplayName(parentProject.get) + "-" + project.name
    }
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
