package code
package snippet

import java.util.Date
import java.util.Random
import java.text.Collator
import scala.util.Sorting
import scala.xml._
import code.service._
import code.model._
import net.liftweb.mapper.By
import _root_.scala.xml.NodeSeq
import _root_.net.liftweb.util.Helpers
import Helpers._
import net.liftweb.http.js.JsCmds
import net.liftweb.util.BindHelpers
import net.liftweb.http.S
import net.liftweb.util.Helpers._
import code.commons.TimeUtils

import net.liftweb._
import http._
import util.Helpers._
import scala.xml.NodeSeq
import net.liftweb.http.js.JE.JsRaw

import net.liftweb.util.Helpers._
import net.liftweb.http.{ SHtml, Templates }
import net.liftweb.http.js.JsCmds.{ SetHtml, Noop }
import net.liftweb.http.js.JsCmd
import code.model.mixin.HierarchicalItem

/**
 * Project snippet.
 * @author David Csakvari
 */
class ProjectsSnippet {

  private var template: NodeSeq = null

  private var projectTemplate: NodeSeq = null

  private var taskTemplate: NodeSeq = null

  private val listSubprojectsEmptyNode = <div class="subproject"></div>

  private val listTasksEmptyNode = <div class="childTask"></div>

  object showInactiveProjectsAndTasks extends SessionVar(false)

  object selectedProject extends SessionVar[Project](null)
  object selectedTask extends SessionVar[Task](null)

  def toggleInactiveView = {
    "type=submit [value]" #> (if (showInactiveProjectsAndTasks.get) S.?("projects.hide_inactive") else S.?("projects.show_inactive")) &
      "type=submit" #> SHtml.onSubmitUnit(() => {
        showInactiveProjectsAndTasks.set(!showInactiveProjectsAndTasks.get)
        net.liftweb.http.js.JsCmds.Reload
      })
  }

  def addRoot(in: NodeSeq): NodeSeq = {
    Helpers.bind("project", in,
      AttrBindParam("onclick", SHtml.ajaxInvoke(addRootEditor _).toJsCmd, "onclick"))
  }

  def moveToRoot = {
    def moveProjectToRoot: JsCmd = {
      if (selectedProject.get != null) {
        ProjectService.moveToRoot(selectedProject.get)
        selectedProject.set(null)
      }
      rerenderProjectTree
    }

    "a [onclick]" #> SHtml.ajaxInvoke(moveProjectToRoot _).toJsCmd
  }

  def projects(in: NodeSeq): NodeSeq = {
    // TODO: change snippet-based recursion
    projectTemplate = <div class="lift:projectsSnippet.projectList">{ (in \ "div").head }</div>
    taskTemplate = <div class="lift:projectsSnippet.taskList">{ (in \ "div").tail.head }</div>
    template = in

    projectTemplate
  }

  def projectList(in: NodeSeq): NodeSeq = {
    val parentId: Long = {
      try {
        (in \ "div" \ "div").filter(_.attribute("class").get.text == "parentId").text.toLong
      } catch {
        case e: Exception => -1
      }
    }

    var data = (if (parentId == -1) {
      if (showInactiveProjectsAndTasks.get) {
        Project.findAll.filter(_.parent.isEmpty).toSeq
      } else {
        Project.findAll(By(Project.active, true)).filter(_.parent.isEmpty).toSeq
      }
    } else {
      if (showInactiveProjectsAndTasks.get) {
        Project.findAll(By(Project.parent, Project.find(By(Project.id, parentId)).get)).toSeq
      } else {
        Project.findAll(By(Project.parent, Project.find(By(Project.id, parentId)).get), By(Project.active, true)).toSeq
      }
    }).toArray

    if (parentId == -1 && data.isEmpty) {
      <lift:embed what="no_data"/>
    } else {
      Sorting.quickSort(data)(new Ordering[Project] {
        def compare(x: Project, y: Project) = {
          x.name.get compare y.name.get
        }
      })
      data.toSeq.flatMap(project => renderProject(project, in))
    }
  }

  private def renderProject(project: Project, in: NodeSeq) = {
    var className = "projectName"
    if (!project.active.get) className += " inactive"

    var rootClassName = "project"
    if (selectedProject.get == project) rootClassName += " selected"

    Helpers.bind("project", in,
      "name" -> {
        if (project.active.get) {
          project.name.get
        } else {
          project.name.get + " (" + S.?("projects.inactive") + ")"
        }
      },
      "subprojects" -> Helpers.bind("project", projectTemplate, "id" -> project.id.toString),
      "tasks" -> Helpers.bind("project", taskTemplate, "id" -> project.id.toString),
      AttrBindParam("rootclass", rootClassName, "class"),
      AttrBindParam("class", className, "class"),
      AttrBindParam("onclick", SHtml.ajaxInvoke(() => editor(project)).toJsCmd, "onclick"),
      AttrBindParam("subprojectonclick", SHtml.ajaxInvoke(() => addChild(project, true)).toJsCmd, "onclick"),
      AttrBindParam("subtaskonclick", SHtml.ajaxInvoke(() => addChild(project, false)).toJsCmd, "onclick"),
      AttrBindParam("deleteprojectonclick", SHtml.ajaxInvoke(() => deleteProject(project)).toJsCmd, "onclick"),
      AttrBindParam("selectonclick", SHtml.ajaxInvoke(() => selectProject(project)).toJsCmd, "onclick"),
      AttrBindParam("movetoonclick", SHtml.ajaxInvoke(() => moveToProject(project)).toJsCmd, "onclick"))
  }

  def taskList(in: NodeSeq): NodeSeq = {

    val parentId: Long = {
      try {
        (in \ "div" \ "div").filter(_.attribute("class").get.text == "parentId").text.toLong
      } catch {
        case e: Exception => -1
      }
    }

    if (parentId != -1L) {
      val parentProject = Project.find(By(Project.id, parentId))

      val tasks = if (!showInactiveProjectsAndTasks.get) {
        Task.findAll(By(Task.parent, parentProject), By(Task.active, true))
      } else {
        Task.findAll(By(Task.parent, parentProject))
      }

      val data = tasks.toArray

      Sorting.quickSort(data)(new Ordering[Task] {
        def compare(x: Task, y: Task) = {
          x.name.get compare y.name.get
        }
      })

      data.toSeq.flatMap(task => renderTask(task, in))
    } else {
      listTasksEmptyNode
    }
  }

  private def renderTask(task: Task, in: NodeSeq) = {
    var rootClassName = "task"
    if (selectedTask.get == task) rootClassName += " selected"
    Helpers.bind("task", in,
      "name" -> { if (task.active.get) { task.name.get } else { task.name.get + " (" + S.?("projects.inactive") + ")" } },
      AttrBindParam("rootclass", rootClassName, "class"),
      AttrBindParam("class", { if (task.active.get) "taskName" else "taskName inactive" }, "class"),
      AttrBindParam("onclick", SHtml.ajaxInvoke(() => editor(task)).toJsCmd, "onclick"),
      AttrBindParam("deletetaskonclick", SHtml.ajaxInvoke(() => deleteTask(task)).toJsCmd, "onclick"),
      AttrBindParam("selectonclick", SHtml.ajaxInvoke(() => selectTask(task)).toJsCmd, "onclick"))
  }

  private def editor(hierarchicalItem: HierarchicalItem[_]): JsCmd = {
    object name extends TransientRequestVar(hierarchicalItem.name.get)
    object description extends TransientRequestVar(hierarchicalItem.description.get)
    object color extends TransientRequestVar(hierarchicalItem.color.get)
    object active extends TransientRequestVar(hierarchicalItem.active.get)

    def submit: JsCmd = {
      hierarchicalItem match {
        case _: Project => {
          Project.findByKey(hierarchicalItem.id.get.toLong).get
            .name(name.get)
            .description(description.get)
            .color(color.get)
            .active(active.get)
            .save
        }
        case _: Task => {
          Task.findByKey(hierarchicalItem.id.get.toLong).get
            .name(name.get)
            .description(description.get)
            .color(color.get)
            .active(active.get)
            .save
        }
      }
      rerenderProjectTree &
      closeDialog
    }

    SetHtml("inject",
      Helpers.bind("editor", editorTemplate,
        "fields" ->
          (Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.name"),
              "value" -> SHtml.textElem(name, "class" -> "form-control")) ++
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.description"),
              "value" -> SHtml.textElem(description, "class" -> "form-control")) ++
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.color"),
              "value" -> SHtml.textElem(color, "class" -> "form-control", "type" -> "color")) ++
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.active"),
              "value" -> SHtml.checkboxElem(active))),
        "title" -> S.?("projects.edit"),
        "submit" -> SHtml.ajaxSubmit(S.?("button.save"), submit _, "class" -> "btn btn-primary"),
        "close" -> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default"))
      ) &
    openDialog
  }

  private def addRootEditor: JsCmd = {
    object name extends TransientRequestVar("")
    object description extends TransientRequestVar("")
    object color extends TransientRequestVar("")

    def submit: JsCmd = {
      Project.create
        .name(name.get)
        .description(description.get)
        .color(color.get)
        .active(true)
        .save

      rerenderProjectTree &
      closeDialog
    }

    SetHtml("inject",
      Helpers.bind("editor", editorTemplate,
        "fields" ->
          (Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.name"),
              "value" -> SHtml.textElem(name, "class" -> "form-control")) ++
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.description"),
              "value" -> SHtml.textElem(description, "class" -> "form-control")) ++
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.color"),
              "value" -> SHtml.textElem(color, "class" -> "form-control", "type" -> "color"))
            ),
        "title" -> S.?("projects.new_project"),
        "submit" -> SHtml.ajaxSubmit(S.?("button.save"), submit _, "class" -> "btn btn-primary"),
        "close" -> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default"))) &
    openDialog
  }

  private def addChild(parent: Project, isProject: Boolean): JsCmd = {
    object name extends TransientRequestVar("")
    object description extends TransientRequestVar("")

    def submit: JsCmd = {
      if (isProject)
        Project.create
          .parent(parent)
          .name(name.get)
          .description(description.get)
          .active(true)
          .save
      else
        Task.create
          .parent(parent)
          .name(name.get)
          .description(description.get)
          .active(true)
          .save

      rerenderProjectTree &
      closeDialog
    }

    SetHtml("inject",
      Helpers.bind("editor", editorTemplate,
        "fields" ->
          (Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.name"),
              "value" -> SHtml.textElem(name, "class" -> "form-control")) ++
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.description"),
              "value" -> SHtml.textElem(description, "class" -> "form-control"))),
        "title" -> (if (isProject) S.?("projects.add_subproject") else S.?("projects.add_task")),
        "submit" -> SHtml.ajaxSubmit(S.?("button.save"), submit _, "class" -> "btn btn-primary"),
        "close" -> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default"))) &
    openDialog
  }

  private def selectProject(project: Project): JsCmd = {
    selectedTask.set(null)
    if (selectedProject.get == project) {
      selectedProject.set(null)
    } else {
      selectedProject.set(project)
    }
    rerenderProjectTree
  }

  private def moveToProject(project: Project): JsCmd = {
    if (selectedProject.get != null) {
      ProjectService.move(selectedProject.get, project)
      selectedProject.set(null)
    } else if (selectedTask.get != null) {
      TaskService.move(selectedTask.get, project)
      selectedTask.set(null)
    }
    rerenderProjectTree
  }

  private def selectTask(task: Task): JsCmd = {
    selectedProject.set(null)
    if (selectedTask.get == task) {
      selectedTask.set(null)
    } else {
      selectedTask.set(task)
    }
    rerenderProjectTree
  }

  private def deleteProject(project: Project): JsCmd = {
    def submit: JsCmd = {
      try {
        ProjectService.delete(project)
        rerenderProjectTree &
        closeDialog
      } catch {
        case e: Exception => net.liftweb.http.js.JsCmds.Alert(e.getMessage)
      }
    }

    SetHtml("inject",
      Helpers.bind("editor", editorTemplate,
        "fields" ->
          (Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.name"),
              "value" -> project.name) ++
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.description"),
              "value" -> project.description)),
        "title" -> S.?("projects.delete"),
        "submit" -> SHtml.ajaxSubmit(S.?("button.delete"), submit _, "class" -> "btn btn-primary"),
        "close" -> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default"))) &
    openDialog
  }

  private def deleteTask(task: Task): JsCmd = {
    def submit: JsCmd = {
      try {
        TaskService.delete(task)
        rerenderProjectTree &
        closeDialog
      } catch {
        case e: Exception => net.liftweb.http.js.JsCmds.Alert(e.getMessage)
      }
    }

    SetHtml("inject",
      Helpers.bind("editor", editorTemplate,
        "fields" ->
          (Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.name"),
              "value" -> task.name) ++
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.description"),
              "value" -> task.description)),
        "title" -> S.?("projects.delete"),
        "submit" -> SHtml.ajaxSubmit(S.?("button.delete"), submit _, "class" -> "btn btn-primary"),
        "close" -> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default"))) &
    openDialog
  }

  val editorTemplate: NodeSeq = Templates("templates-hidden/editor_fragment" :: Nil).get

  val editorPropertyTemplate: NodeSeq = Templates("templates-hidden/editor_property_fragment" :: Nil).get

  def closeDialog: JsCmd = JsRaw("$('.modal').modal('hide')").cmd

  def openDialog: JsCmd = JsRaw("$('.modal').modal()").cmd

  def rerenderProjectTree: JsCmd = SetHtml("project-tree", projects(template))
}
