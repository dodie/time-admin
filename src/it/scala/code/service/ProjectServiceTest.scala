package code.service

import code.model.Project
import code.test.utils.DbSpec
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
