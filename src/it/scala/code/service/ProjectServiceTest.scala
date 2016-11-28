package code.service

import code.model.Project
import code.test.utils.DbSpec
import net.liftweb.common.Box
import net.liftweb.mapper.By
import org.scalatest.{FunSuite, FunSpec}

class ProjectServiceTest extends FunSuite with DbSpec {

  test("Display name of the top level project") {
    givenSomeProjectData()

    assert(ProjectService.getDisplayName(project("top")) == "top")
  }

  test("Display name of the middle level project") {
    givenSomeProjectData()

    assert(ProjectService.getDisplayName(project("middle")) == "top-middle")
  }

  test("Display name of the bottom level project") {
    givenSomeProjectData()

    assert(ProjectService.getDisplayName(project("bottom")) == "top-middle-bottom")
  }

  test("Move bottom project to any parent project") {
    givenSomeProjectData()

    ProjectService.move(project("bottom"), project("any project"))

    assert(ProjectService.getDisplayName(project("bottom")) == "top-any project-bottom")
  }

  test("Move bottom project to root") {
    givenSomeProjectData()

    ProjectService.moveToRoot(project("bottom"))

    assert(ProjectService.getDisplayName(project("bottom")) == "bottom")
  }

  test("The top level project is not empty") {
    givenSomeProjectData()

    assert(!ProjectService.isEmpty(project("top")))
  }

  test("The bottom level project is empty") {
    givenSomeProjectData()

    assert(ProjectService.isEmpty(project("bottom")))
  }

  test("Delete a bottom level project") {
    givenSomeProjectData()

    assert(ProjectService.delete(project("bottom")))
  }

  test("Try to delete a top level project") {
    intercept[IllegalArgumentException] {
      ProjectService.delete(project("top"))
    }
  }

  def givenSomeProjectData(): Unit = {
    Project.create.name("top").save()
    lazy val top = Project.find(By(Project.name, "top"))
    Project.create.name("any project").parent(top).save()
    Project.create.name("middle").parent(top).save()
    lazy val middle = Project.find(By(Project.name, "middle"))
    Project.create.name("bottom").parent(middle).save()
  }

  def project(n: String): Project = Project.find(By(Project.name, n)).openOrThrowException("Test entity must be presented!")

}
