package code.service

case class TaskSheetItem(id: Long, name: String)

object TaskSheetItem {
  def apply(t: TaskItemWithDuration): TaskSheetItem = new TaskSheetItem(t.task.map(_.id.get).getOrElse(0L), t.projectName.getOrElse("") + "-" + t.taskName.getOrElse(""))
}
