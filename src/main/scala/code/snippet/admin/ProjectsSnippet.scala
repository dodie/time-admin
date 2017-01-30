package code
package snippet

import java.text.Collator

import _root_.net.liftweb.util.{CssSel, Helpers}
import code.model._
import code.model.mixin.HierarchicalItem
import code.service._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.{S, SHtml, Templates, _}
import net.liftweb.mapper.By
import net.liftweb.util.Helpers._

import scala.util.Sorting
import scala.xml.NodeSeq

/**
 * Project snippet.
 * @author David Csakvari
 */
class ProjectsSnippet {

  private val collator: Collator = Collator.getInstance(S.locale)

  private var template: NodeSeq = _

  private var projectTemplate: NodeSeq = _

  private var taskTemplate: NodeSeq = _

  private val listTasksEmptyNode = <div class="childTask"></div>

  object showInactiveProjectsAndTasks extends SessionVar(false)

  object selectedProject extends SessionVar[Project](null)

  object selectedTask extends SessionVar[Task](null)

  def toggleInactiveView: CssSel = {
    "type=submit [value]" #> (if (showInactiveProjectsAndTasks.get) S.?("projects.hide_inactive") else S.?("projects.show_inactive")) &
      "type=submit" #> SHtml.onSubmitUnit(() => {
        showInactiveProjectsAndTasks.set(!showInactiveProjectsAndTasks.get)
        net.liftweb.http.js.JsCmds.Reload
      })
  }

  def addRoot: CssSel = {
    ".add-root [onclick]" #> SHtml.ajaxInvoke(addRootEditor).toJsCmd
  }

  def moveToRoot: CssSel = {
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
        case _: Exception => -1
      }
    }

    val data = (if (parentId == -1) {
      if (showInactiveProjectsAndTasks.get) {
        Project.findAll.filter(_.parent.isEmpty)
      } else {
        Project.findAll(By(Project.active, true)).filter(_.parent.isEmpty)
      }
    } else {
      if (showInactiveProjectsAndTasks.get) {
        Project.findAll(By(Project.parent, Project.find(By(Project.id, parentId)).openOrThrowException("Project must be defined!")))
      } else {
        Project.findAll(By(Project.parent, Project.find(By(Project.id, parentId)).openOrThrowException("Project must be defined!")), By(Project.active, true))
      }
    }).toArray

    if (parentId == -1 && data.isEmpty) {
      <lift:embed what="no_data"/>
    } else {
      Sorting.quickSort(data)(new Ordering[Project] {
        def compare(x: Project, y: Project): Int = {
          collator.compare(x.name.get, y.name.get)
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
      AttrBindParam("subprojectonclick", SHtml.ajaxInvoke(() => addChild(project, isProject = true)).toJsCmd, "onclick"),
      AttrBindParam("subtaskonclick", SHtml.ajaxInvoke(() => addChild(project, isProject = false)).toJsCmd, "onclick"),
      AttrBindParam("deleteprojectonclick", SHtml.ajaxInvoke(() => deleteProject(project)).toJsCmd, "onclick"),
      AttrBindParam("selectonclick", SHtml.ajaxInvoke(() => selectProject(project)).toJsCmd, "onclick"),
      AttrBindParam("movetoonclick", SHtml.ajaxInvoke(() => moveToProject(project)).toJsCmd, "onclick"))
  }

  def taskList(in: NodeSeq): NodeSeq = {

    val parentId: Long = {
      try {
        (in \ "div" \ "div").filter(_.attribute("class").get.text == "parentId").text.toLong
      } catch {
        case _: Exception => -1
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
        def compare(x: Task, y: Task): Int = {
          collator.compare(x.name.get, y.name.get)
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
      AttrBindParam("selectonclick", SHtml.ajaxInvoke(() => selectTask(task)).toJsCmd, "onclick"),
      AttrBindParam("mergeintoonclick", SHtml.ajaxInvoke(() => mergeTask(task)).toJsCmd, "onclick"))
  }

  private def editor(hierarchicalItem: HierarchicalItem[_]): JsCmd = {
    object name extends TransientRequestVar(hierarchicalItem.name.get)
    object description extends TransientRequestVar(hierarchicalItem.description.get)
    object color extends TransientRequestVar(hierarchicalItem.color.get)
    object active extends TransientRequestVar(hierarchicalItem.active.get)
    object specifiable extends TransientRequestVar(hierarchicalItem.specifiable.get)

    def submit: JsCmd = {
      hierarchicalItem match {
        case _: Project =>
          Project.findByKey(hierarchicalItem.id.get).openOrThrowException("Project must be defined!")
            .name(name.get)
            .description(description.get)
            .color(color.get)
            .active(active.get)
            .specifiable(specifiable.get)
            .save
        case _: Task =>
          Task.findByKey(hierarchicalItem.id.get).openOrThrowException("Project must be defined!")
            .name(name.get)
            .description(description.get)
            .color(color.get)
            .active(active.get)
            .specifiable(specifiable.get)
            .save
      }
      rerenderProjectTree &
      closeDialog
    }

    val defaultFieldBindings =
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.name"),
              "value" -> SHtml.textElem(name, "class" -> "form-control")) ++
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.description"),
              "value" -> SHtml.textElem(description, "class" -> "form-control"))

    val fieldBindigsWithColor =
      if (!hierarchicalItem.parent.defined_?)
        defaultFieldBindings ++
        Helpers.bind("property", editorPropertyTemplate,
            "name" -> S.?("projects.popup.color"),
            "value" -> SHtml.textElem(color, "type" -> "color"))
      else
        defaultFieldBindings

    val fieldbindingsWithActive =
      if (hierarchicalItem.active.get)
        fieldBindigsWithColor
      else
        fieldBindigsWithColor ++
        Helpers.bind("property", editorPropertyTemplate,
            "name" -> S.?("projects.popup.active"),
            "value" -> SHtml.checkboxElem(active))

    val fieldBindings =
      hierarchicalItem match {
        case _: Project =>
          fieldbindingsWithActive
        case _: Task =>
          fieldbindingsWithActive ++
          Helpers.bind("property", editorPropertyTemplate,
              "name" -> S.?("projects.popup.specifiable"),
              "value" -> SHtml.checkboxElem(specifiable))
      }

    SetHtml("inject",
      Helpers.bind("editor", editorTemplate,
        "fields" -> fieldBindings,
        "title" -> S.?("projects.edit"),
        "submit" -> SHtml.ajaxSubmit(S.?("button.save"), submit _, "class" -> "btn btn-primary"),
        "close" -> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default"))
      ) &
    openDialog
  }

  private def addRootEditor(): JsCmd = {
    object name extends TransientRequestVar("")
    object description extends TransientRequestVar("")
    object color extends TransientRequestVar("")

    def submit: JsCmd = {
      Project.create
        .name(name.get)
        .description(description.get)
        .color(color.get)
        .active(true)
        .specifiable(true)
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
          .specifiable(true)
          .save
      else
        Task.create
          .parent(parent)
          .name(name.get)
          .description(description.get)
          .active(true)
          .specifiable(true)
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

  private def mergeTask(task: Task): JsCmd = {
    if (selectedTask.get != null) {
      TaskService.merge(selectedTask.get, task)
      selectedTask.set(null)
    } else {
      net.liftweb.http.js.JsCmds.Alert("No task selected!")
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

  val editorTemplate: NodeSeq =
    <div class="modal fade" data-backdrop="static" data-keyboard="false">
      <div class="modal-dialog">
        <form class="lift:form.ajax" role="form">
          <div class="modal-content">
            <div class="modal-header">
              <h4 class="modal-title"><editor:title></editor:title></h4>
            </div>
            <div class="modal-body">
              <editor:fields></editor:fields>
            </div>
            <div class="modal-footer">
              <editor:submit type="submit"></editor:submit>
              <editor:close type="submit"></editor:close>
            </div>
          </div>
        </form>
      </div>
    </div>

  val editorPropertyTemplate: NodeSeq =
    <div class="form-group">
      <label><property:name></property:name></label>
      <property:value></property:value>
    </div>

  def closeDialog: JsCmd = JsRaw("$('.modal').modal('hide')").cmd

  def openDialog: JsCmd = JsRaw("$('.modal').modal()").cmd

  def rerenderProjectTree: JsCmd = SetHtml("project-tree", projects(template))
}
