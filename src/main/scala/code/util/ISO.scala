package code.util

import org.joda.time.{Interval, YearMonth}

object ISO {
  object Dates {

    def print(i: Interval): String = {
      val start = new YearMonth(i.getStart)
      val end = new YearMonth(i.getEnd.minusDays(1))
      if (start == end) start.toString
      else s"${start.toString} - ${end.toString}"
    }
  }
}
