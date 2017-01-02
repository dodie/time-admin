package code.snippet

import code.service.Color
import org.scalatest.{FlatSpec, _}

class ColorSpec extends FlatSpec with Matchers {

  "getColor" must "produce a deterministic color for a task" in {
    val taskName = "A Task"
    val projectName = "A Project"
    val active = true

    val color1 = Color.get(taskName, projectName, active)
    val color2 = Color.get(taskName, projectName, active)

    color1 should equal(color2)
  }
}