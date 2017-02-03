package code.service

import code.model.Project
import code.test.utils.BaseSuite
import net.liftweb.mapper.By

class ProjectServiceTest extends BaseSuite {
  describe("Project Service") {
    it("The top level project is not empty") {
      assert(!ProjectService.isEmpty(projectByName("top")))
    }

    it("The bottom level project is empty") {
      assert(ProjectService.isEmpty(projectByName("bottom")))
    }

    it("Delete a bottom level project") {
      assert(ProjectService.delete(projectByName("bottom")))
    }

    it("Delete an active top level project") {
      ProjectService.delete(projectByName("top"))
      assert(!projectByName("top").active.get)
    }

    it("Try to delete an inactivated top level project") {
      val me = projectByName("top").active(false).saveMe()
      intercept[IllegalArgumentException] {
        ProjectService.delete(me)
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

  def projectByName(n: String): Project = Project.find(By(Project.name, n)).openOrThrowException("Test entity must be presented!")

}
