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
      data.toSeq.flatMap(project => renderProject(project)(in))
    }
  }

  private def renderProject(project: Project) = {
    val displayName =
      if (project.active.get)
        project.name.get
      else
        project.name.get + " (" + S.?("projects.inactive") + ")"

    var rootClass = "project"
    if (selectedProject.get == project) rootClass += " selected"

    var innerClass = "projectName"
    if (!project.active.get) innerClass += " inactive"

    // TODO Remove snippet based recursion.
    val subsCssSel:CssSel = ".parentId *" #> project.id.toString

    ".name" #> displayName &
    ".root-class [class]" #> rootClass &
    ".inner-class [class]" #> innerClass &
    ".edit [onclick]" #> SHtml.ajaxInvoke(() => editor(project)).toJsCmd &
    ".add-subproject [onclick]" #> SHtml.ajaxInvoke(() => addChild(project, isProject = true)).toJsCmd &
    ".add-subtask [onclick]" #> SHtml.ajaxInvoke(() => addChild(project, isProject = false)).toJsCmd &
    ".delete [onclick]" #> SHtml.ajaxInvoke(() => deleteProject(project)).toJsCmd &
    ".select [onclick]" #> SHtml.ajaxInvoke(() => selectProject(project)).toJsCmd &
    ".moveto [onclick]" #> SHtml.ajaxInvoke(() => moveToProject(project)).toJsCmd &
    ".subprojects *" #> subsCssSel(projectTemplate) &
    ".subtasks *" #> subsCssSel(taskTemplate)
  }

  private val projectTemplate: NodeSeq =
    <div class="lift:projectsSnippet.projectList">
      <div class="root-class">
        <div class="parentId" style="display:none;"></div>
        <span class="inner-class">
          <span class="dropdown">
            <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
              <span class="name"></span>
              <span class="caret"></span>
            </a>
            <ul class="dropdown-menu">
              <li><a class="edit"><lift:loc>projects.edit</lift:loc></a></li>
              <li><a class="add-subproject"><lift:loc>projects.add_subproject</lift:loc></a></li>
              <li><a class="add-subtask"><lift:loc>projects.add_task</lift:loc></a></li>
              <li><a class="delete"><lift:loc>projects.delete</lift:loc></a></li>
              <li><a class="select"><lift:loc>projects.select</lift:loc></a></li>
              <li><a class="moveto"><lift:loc>projects.moveto</lift:loc></a></li>
            </ul>
          </span>
        </span>
        <div class="subprojects"></div>
        <div class="subtasks"></div>
      </div>
    </div>

  private val taskTemplate: NodeSeq =
    <div class="lift:projectsSnippet.taskList">
      <div class="task-root">
        <div class="parentId" style="display:none;"></div>
        <span class="task-inner">
          <span class="dropdown">
            <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">
              <span class="name"></span>
              <span class="caret"></span>
            </a>
            <ul class="dropdown-menu">
              <li><a class="edit"><lift:loc>projects.edit</lift:loc></a></li>
              <li><a class="delete"><lift:loc>projects.delete</lift:loc></a></li>
              <li><a class="select"><lift:loc>projects.select</lift:loc></a></li>
              <li><a class="merge"><lift:loc>projects.mergeinto</lift:loc></a></li>
            </ul>
          </span>
        </span>
      </div>
    </div>

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

      data.toSeq.flatMap(task => renderTask(task)(in))
    } else {
      <div class="childTask"></div>
    }
  }

  private def renderTask(task: Task):CssSel = {
    val displayName =
      if (task.active.get)
        task.name.get
      else
        task.name.get + " (" + S.?("projects.inactive") + ")"

    val rootClass =
      if (selectedTask.get == task)
        "task selected"
      else
        "task"

    val innerClass =
      if (task.active.get)
        "taskName"
      else
        "taskName inactive"

    ".name" #> displayName &
    ".task-root [class]" #> rootClass &
    ".task-inner [class]" #> innerClass &
    ".edit [onclick]" #> SHtml.ajaxInvoke(() => editor(task)).toJsCmd &
    ".delete [onclick]" #> SHtml.ajaxInvoke(() => deleteTask(task)).toJsCmd &
    ".select [onclick]" #> SHtml.ajaxInvoke(() => selectTask(task)).toJsCmd &
    ".merge [onclick]" #> SHtml.ajaxInvoke(() => mergeTask(task)).toJsCmd
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
      renderProperty(
        ".name *" #> S.?("projects.popup.name") &
        ".field" #> SHtml.textElem(name, "class" -> "form-control")) ++
      renderProperty(
        ".name *" #> S.?("projects.popup.description") &
        ".field" #> SHtml.textElem(description, "class" -> "form-control"))

    val fieldBindigsWithColor =
      if (!hierarchicalItem.parent.defined_?)
        defaultFieldBindings ++
        renderProperty(
          ".name *" #> S.?("projects.popup.color") &
          ".field" #> SHtml.textElem(color, "type" -> "color"))
      else
        defaultFieldBindings

    val fieldbindingsWithActive =
      if (hierarchicalItem.active.get)
        fieldBindigsWithColor
      else
        fieldBindigsWithColor ++
        renderProperty(
          ".name *" #> S.?("projects.popup.active") &
          ".field" #> SHtml.checkboxElem(active))

    val fieldBindings =
      hierarchicalItem match {
        case _: Project =>
          fieldbindingsWithActive
        case _: Task =>
          fieldbindingsWithActive ++
          renderProperty(
              ".name *" #> S.?("projects.popup.specifiable") &
              ".field" #> SHtml.checkboxElem(specifiable))
      }

    SetHtml("inject",
      (
        ".fields *" #> fieldBindings &
        ".title *" #> S.?("projects.edit") &
        ".submit-button" #> SHtml.ajaxSubmit(S.?("button.save"), submit _, "class" -> "btn btn-primary") &
        ".close-button" #> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default")
      )(editorTemplate)
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
      (
        ".fields *" #>
          (
            renderProperty(
              ".name *" #> S.?("projects.popup.name") &
              ".field" #> SHtml.textElem(name, "class" -> "form-control")) ++
            renderProperty(
              ".name *" #> S.?("projects.popup.description") &
              ".field" #> SHtml.textElem(description, "class" -> "form-control")) ++
            renderProperty(
              ".name *" #> S.?("projects.popup.color") &
              ".field" #> SHtml.textElem(color, "class" -> "form-control", "type" -> "color"))
            ) &
        ".title *" #> S.?("projects.new_project") &
        ".submit-button" #> SHtml.ajaxSubmit(S.?("button.save"), submit _, "class" -> "btn btn-primary") &
        ".close-button" #> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default")

      )(editorTemplate)
    ) &
    openDialog
  }

  def renderProperty(cssSel: CssSel): NodeSeq = {
    cssSel(editorPropertyTemplate)
  }

  val editorPropertyTemplate: NodeSeq =
    <div class="form-group">
      <label class="name"></label>
      <input class="field"/>
    </div>


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
      (
        ".fields *" #>
          (
            renderProperty(
              ".name *" #> S.?("projects.popup.name") &
              ".field" #> SHtml.textElem(name, "class" -> "form-control")) ++
            renderProperty(
              ".name" #> S.?("projects.popup.description") &
              ".field" #> SHtml.textElem(description, "class" -> "form-control"))) &
        ".title *" #> (if (isProject) S.?("projects.add_subproject") else S.?("projects.add_task")) &
        ".submit-button" #> SHtml.ajaxSubmit(S.?("button.save"), submit _, "class" -> "btn btn-primary") &
        ".close-button" #> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default")
      )(editorTemplate)
    ) &
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
      (
        ".fields *" #>
          (
            renderProperty(
              ".name *" #> S.?("projects.popup.name") &
              ".field" #> project.name) ++
            renderProperty(
              ".name *" #> S.?("projects.popup.description") &
              ".field" #> project.description)) &
        ".title *" #> S.?("projects.delete") &
        ".submit-button" #> SHtml.ajaxSubmit(S.?("button.delete"), submit _, "class" -> "btn btn-primary") &
        ".close-button" #> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default")
      )(editorTemplate)
    ) &
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
      (
        ".fields *" #>
          (
            renderProperty(
              ".name *" #> S.?("projects.popup.name") &
              ".field" #> task.name) ++
            renderProperty(
              ".name" #> S.?("projects.popup.description") &
              ".field" #> task.description)) &
        ".title *" #> S.?("projects.delete") &
        ".submit-button" #> SHtml.ajaxSubmit(S.?("button.delete"), submit _, "class" -> "btn btn-primary") &
        ".close-button" #> SHtml.ajaxSubmit(S.?("button.close"), closeDialog _, "class" -> "btn btn-default")

      )(editorTemplate)
    ) &
    openDialog
  }


  def closeDialog: JsCmd = JsRaw("$('.modal').modal('hide')").cmd

  def openDialog: JsCmd = JsRaw("$('.modal').modal()").cmd

  def rerenderProjectTree: JsCmd = SetHtml("project-tree", projects(NodeSeq.Empty))


  val editorTemplate: NodeSeq =
    <div class="modal fade" data-backdrop="static" data-keyboard="false">
      <div class="modal-dialog">
        <form class="lift:form.ajax" role="form">
          <div class="modal-content">
            <div class="modal-header">
              <h4 class="modal-title title"></h4>
            </div>
            <div class="modal-body fields"></div>
            <div class="modal-footer">
              <input class="submit-button"/>
              <input class="close-button"/>
            </div>
          </div>
        </form>
      </div>
    </div>

}
