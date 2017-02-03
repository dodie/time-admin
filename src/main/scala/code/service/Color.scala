package code.service

import java.util.Random

import net.liftweb.common.Box
import net.liftweb.util.ControlHelpers.tryo

case class Color(red: Int, green: Int, blue: Int, alpha: Int = 1) {
  val isDark: Boolean = (((red * 299) + (green * 587) + (blue * 114)) / 1000) < 128

  override def toString: String = s"($red,$green,$blue,$alpha)"
}

object Color {
  val transparent: Color = Color(0, 0, 0, 0)

  def get(taskName: String, parentsDisplayName: String, active: Boolean): Color = {
    val random = new Random((taskName.trim + parentsDisplayName.trim).hashCode)
    Color(
      red = if (active) random.nextInt(255) else 255,
      green = if (active) random.nextInt(255) else 255,
      blue = if (active) random.nextInt(255) else 255,
      alpha = if (active) 1 else 1
    )
  }

  def parse(color: String): Box[Color] = tryo {
    Color(
      red = Integer.valueOf(color.substring(1, 3), 16),
      green = Integer.valueOf(color.substring(3, 5), 16),
      blue = Integer.valueOf(color.substring(5, 7), 16)
    )
  }
}

