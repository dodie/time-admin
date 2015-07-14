package feature.support

import scala.util.Random

object RandomData {

  private val random = new Random

  def randomString: String =
    randomAlphanumbericString(10)

  def randomAlphanumbericString(n: Int): String =
    randomString("abcdefghijklmnopqrstuvwxyz0123456789")(n)

  def randomAlphabetString(n: Int): String =
    randomString("abcdefghijklmnopqrstuvwxyz")(n)

  def randomEmail: String =
    randomString + "@" + randomString + "." + randomAlphabetString(3)

  def randomString(alphabet: String)(n: Int): String =
    Stream.continually(random.nextInt(alphabet.size)).map(alphabet).take(n).mkString

}
