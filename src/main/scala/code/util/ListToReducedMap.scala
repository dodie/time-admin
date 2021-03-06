package code.util

import scala.collection.immutable

import scala.language.implicitConversions

class ListToReducedMap[A](underlying: List[A]) {

  def leftReducedMap[T, U](z: U)(f: (U, U) => U)(implicit ev: A <:< (T, U)): immutable.Map[T, U] =
    underlying.groupBy(_._1).map { case (k, v) => (k, v.map(_._2).foldLeft(z)(f)) }
}

object ListToReducedMap {

  implicit def listToFoldedMap[A](as: List[A]): ListToReducedMap[A] = new ListToReducedMap(as)
}
