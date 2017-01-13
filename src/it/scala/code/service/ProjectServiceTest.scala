package code.service

import code.model.Project
import code.test.utils.BaseContext
import net.liftweb.mapper.By

class ProjectServiceTest extends BaseContext {

  it("Display name of the top level project") {
    assert(ProjectService.getDisplayName(project("top")) == "top")
  }

  it("Display name of the middle level project") {
    assert(ProjectService.getDisplayName(project("middle")) == "top-middle")
  }

  it("Display name of the bottom level project") {
    assert(ProjectService.getDisplayName(project("bottom")) == "top-middle-bottom")
  }

  it("Move bottom project to any parent project") {
    ProjectService.move(project("bottom"), project("any project"))

    assert(ProjectService.getDisplayName(project("bottom")) == "top-any project-bottom")
  }

  it("Move bottom project to root") {
    ProjectService.moveToRoot(project("bottom"))

    assert(ProjectService.getDisplayName(project("bottom")) == "bottom")
  }

  it("The top level project is not empty") {
    assert(!ProjectService.isEmpty(project("top")))
  }

  it("The bottom level project is empty") {
    assert(ProjectService.isEmpty(project("bottom")))
  }

  it("Delete a bottom level project") {
    assert(ProjectService.delete(project("bottom")))
  }

  it("Try to delete a top level project") {
    intercept[IllegalArgumentException] {
      ProjectService.delete(project("top"))
    }
  }

  given {
    Project.create.name("top").save()
    lazy val top = Project.find(By(Project.name, "top"))
    Project.create.name("any project").parent(top).save()
    Project.create.name("middle").parent(top).save()
    lazy val middle = Project.find(By(Project.name, "middle"))
    Project.create.name("bottom").parent(middle).save()
  }

  def project(n: String): Project = Project.find(By(Project.name, n)).openOrThrowException("Test entity must be presented!")

}
