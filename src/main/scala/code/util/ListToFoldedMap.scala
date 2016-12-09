package code.util

import scala.collection.immutable

import scala.language.implicitConversions

class ListToFoldedMap[A](underlying: List[A]) {

  def foldedMap[T, U, Z](z: Z)(f: (Z, U) => Z)(implicit ev: A <:< (T, U)): immutable.Map[T, Z] =
    underlying.groupBy(_._1).map{ case (k, v) => (k, v.map(_._2).foldLeft(z)(f)) }

  def reducedMap[T, U](z: U)(f: (U, U) => U)(implicit ev: A <:< (T, U)): immutable.Map[T, U] =
    ListToFoldedMap.listToFoldedMap(underlying).foldedMap[T,U,U](z)(f)
}

object ListToFoldedMap {

  implicit def listToFoldedMap[A](as: List[A]): ListToFoldedMap[A] = new ListToFoldedMap(as)
}
