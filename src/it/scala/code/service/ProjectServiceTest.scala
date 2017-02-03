package code.service

import code.model.Task
import code.test.utils.BaseSuite
import net.liftweb.mapper.By

class ProjectServiceTest extends BaseSuite {
  describe("Project Service") {
    it("Display name of the top level project") {
      assert(TaskService.getDisplayName(projectByName("top")) == "top")
    }

    it("Display name of the middle level project") {
      assert(TaskService.getDisplayName(projectByName("middle")) == "top-middle")
    }

    it("Display name of the bottom level project") {
      assert(TaskService.getDisplayName(projectByName("bottom")) == "top-middle-bottom")
    }

    it("Move bottom project to any parent project") {
      TaskService.move(projectByName("bottom"), projectByName("any project"))

      assert(TaskService.getDisplayName(projectByName("bottom")) == "top-any project-bottom")
    }

    it("Move bottom project to root") {
      TaskService.moveToRoot(projectByName("bottom"))

      assert(TaskService.getDisplayName(projectByName("bottom")) == "bottom")
    }

    it("The top level project is not empty") {
      assert(!TaskService.isEmpty(projectByName("top")))
    }

    it("The bottom level project is empty") {
      assert(TaskService.isEmpty(projectByName("bottom")))
    }

    it("Delete a bottom level project") {
      assert(TaskService.delete(projectByName("bottom")))
    }

    it("Delete an active top level project") {
      TaskService.delete(projectByName("top"))
      assert(!projectByName("top").active.get)
    }

    it("Try to delete an inactivated top level project") {
      val me = projectByName("top").active(false).saveMe()
      intercept[IllegalArgumentException] {
        TaskService.delete(me)
      }
    }
  }

  given {
    traverse(
      project("top",
        project("any project"),
        project("middle",
          project("bottom")))) foreach(_.save())
  }

  def projectByName(n: String): Task = Task.find(By(Task.name, n)).openOrThrowException("Test entity must be presented!")

}
