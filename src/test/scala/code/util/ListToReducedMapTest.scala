package code.util

import org.scalatest.{FunSpec, Matchers}
import code.util.ListToReducedMap._


class ListToReducedMapTest extends FunSpec with Matchers {

  describe("Reduce a list of pairs into a map") {
    it("that should have entries") {
      val reducedMap = List(1 -> "a", 2 -> "b", 1 -> "c", 3 -> "d", 2 -> "e").leftReducedMap("")(_ + _)
      reducedMap should contain theSameElementsAs Map(1 -> "ac", 2 -> "be", 3 -> "d")
    }
  }
}
