package code.service

import code.model.{Task, TaskItem, User}
import code.service.TaskItemService.IntervalQuery
import code.test.utils.BaseSuite
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.mapper.By
import org.joda.time.{LocalDate, LocalTime, YearMonth}

import scala.language.postfixOps

class TimeSheetTest extends BaseSuite {
  describe("Time sheet data for any month") {
    lazy val ts = ReportService.getTimesheetData(IntervalQuery(yearMonth(2016, 1).toInterval))

    it("should have log entries subtracted by the breaks") { withS(Empty, defaultUser()) {
      ts map { t => (t._1, t._2, t._3, f"${t._4}%1.1f") } shouldBe List(
        ("29", "08:30", "16:30", "8.0"),
        ("30", "17:00", "23:29", "6.5"),
        ("31", "00:00", "00:30", "0.5")
      )
    }}
  }

  describe("Time sheet data for any month with disabled break subtraction") {
    lazy val ts = {
      User.currentUser map (_.subtractBreaks(true)) foreach (_.save())
      ReportService.getTimesheetData(IntervalQuery(yearMonth(2016, 1).toInterval))
    }

    it("should have log entries with breaks") { withS(Empty, defaultUser()) {
      ts map { t => (t._1, t._2, t._3, f"${t._4}%1.1f") } shouldBe List(
        ("29", "08:30", "17:00", "8.5"),
        ("30", "17:00", "23:59", "7.0"),
        ("31", "00:00", "00:30", "0.5")
      )
    }}
  }

  def defaultUser(): Box[User] = User.find(By(User.email, "default@mail.com"))

  given {
    val u1 :: _ = givenUsers("default") map (_.saveMe())

    val _ :: t1 :: t2 :: _ :: t3 :: _ :: t4 :: _ :: t5 :: t6 :: t7 :: _ = traverse(
      project("p1",
        task("t1"),
        task("t2"),
        project("p11",
          task("t3")),
        project("p12",
          task("t4"))),
      project("p2",
        task("t5"),
        task("t6"),
        task("t7"))) map (_.saveMe()) map (Full(_))

    val pause = Empty

    givenTaskItems(u1, date(2016, 1, 29),
      t1 -> time(8, 30), t2 -> time(10, 25), pause -> time(12, 0), t3 -> time(12, 30), pause -> time(17, 0)
    ) :::
      givenTaskItems(u1, date(2016, 1, 30),
      t3 -> time(17, 0), pause -> time(22, 30), t7 -> time(23, 0)
    ) :::
      givenTaskItems(u1, date(2016, 1, 31),
        pause -> time(0, 30)
      ) :::
      givenTaskItems(u1, date(2016, 2, 1),
        pause -> time(0, 30), t4 -> time(8, 30), t5 -> time(10, 25), pause -> time(12, 0), t6 -> time(12, 30), pause -> time(17, 0)
      ) foreach (_.save())
  }

  def givenUsers(names: String*): List[User] =
    names map { n => User.create.firstName(n).lastName(n).email(n + "@mail.com").password("abc123").validated(true).superUser(true) } toList

  def givenTaskItems(u: User, ld: LocalDate, tis: (Box[Task], LocalTime)*): List[TaskItem] =
    tis map { case (t, lt) => TaskItem.create.user(u).task(t).start(ld.toDateTime(lt).getMillis) } toList

  def date(year: Int, monthOfYear: Int, dayOfMonth: Int): LocalDate = new LocalDate(year, monthOfYear, dayOfMonth)

  def yearMonth(year: Int, monthOfYear: Int): YearMonth = new YearMonth(year, monthOfYear)

  def time(hourOfDay: Int, minuteOfHour: Int): LocalTime = new LocalTime(hourOfDay, minuteOfHour)
}
