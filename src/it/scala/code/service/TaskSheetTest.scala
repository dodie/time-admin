package code.service

import code.model.{Task, TaskItem, User}
import code.service.ReportService.taskSheetData
import code.service.TaskItemService.IntervalQuery
import code.test.utils.BaseSuite
import code.util.TaskSheetUtils.{dates, sum, tasks}
import com.github.nscala_time.time.Imports._
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.mapper.By

import scala.collection.immutable.::
import scala.language.postfixOps

class TaskSheetTest extends BaseSuite {

  describe("Task sheet data report for any daily interval and user") {
    val interval = new Interval(date(2016, 1, 29).toInterval.start, date(2016, 1, 31).toInterval.end)
    lazy val ts = taskSheetData(IntervalQuery(interval), defaultUser())

    it("should contain all task items for the given interval") {
      ts mapValues { _.map { case (t, d) => t.name -> d } } should contain theSameElementsAs Map(
        date(2016, 1, 29) -> Map(),
        date(2016, 1, 30) -> Map(
          "p1-t1" -> (1.hours + 55.minutes).toDuration,
          "p1-t2" -> (1.hours + 35.minutes).toDuration,
          "p1-p11-t3" -> (4.hours + 30.minutes).toDuration,
          "p2-t7" -> 30.minutes.toDuration
        ),
        date(2016, 1, 31) -> Map(
          "p1-t1" -> (1.hours + 55.minutes).toDuration,
          "p1-t2" -> (1.hours + 35.minutes).toDuration,
          "p1-p11-t3" -> (4.hours + 30.minutes).toDuration,
          "p2-t7" -> 1.hour.toDuration
        )
      )
    }

    it("should have summary") {
      sum(ts) shouldBe (17.hours + 30.minutes).toDuration
    }

    it("should contain all tasks with full name") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p1-t1", "p1-t2", "p1-p11-t3", "p2-t7")
    }

    it("should contain all discrete dates for the interval") {
      dates(ts) should contain theSameElementsAs List(date(2016, 1, 29), date(2016, 1, 30), date(2016, 1, 31))
    }
  }

  describe("Task sheet data report for any monthly interval and user") {
    val interval = new Interval(yearMonth(2015, 12).toInterval.start, yearMonth(2016, 2).toInterval.end)
    lazy val ts = taskSheetData(IntervalQuery(interval, d => new YearMonth(d)), defaultUser())

    it("should contain all task items for the given interval") {
      ts mapValues { _.map { case (t, d) => t.name -> d } } should contain theSameElementsAs Map(
        yearMonth(2015, 12) -> Map(),
        yearMonth(2016, 1) -> Map(
          "p1-t1" -> (3.hours + 50.minutes).toDuration,
          "p1-t2" -> (3.hours + 10.minutes).toDuration,
          "p1-p11-t3" -> 9.hours.toDuration,
          "p2-t7" -> (1.hour + 30.minutes).toDuration
        ),
        yearMonth(2016, 2) -> Map(
          "p2-t7" -> 30.minutes.toDuration,
          "p1-p12-t4" -> (1.hours + 55.minutes).toDuration,
          "p2-t5" -> (1.hours + 35.minutes).toDuration,
          "p2-t6" -> (4.hours + 30.minutes).toDuration
        )
      )
    }

    it("should have summary") {
      sum(ts) shouldBe 26.hours.toDuration
    }

    it("should contain all tasks with full name") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p1-t1", "p1-t2", "p1-p11-t3", "p1-p12-t4", "p2-t5", "p2-t6", "p2-t7")
    }

    it("should contain all discrete dates for the interval") {
      dates(ts) should contain theSameElementsAs List(yearMonth(2015, 12), yearMonth(2016, 1), yearMonth(2016, 2))
    }
  }

  describe("Task sheet data report for any daily interval and all user") {
    val interval = new Interval(date(2016, 1, 29).toInterval.start, date(2016, 1, 31).toInterval.end)
    lazy val ts = taskSheetData(IntervalQuery(interval), Empty)

    it("should contain all task items for the given interval") {
      ts mapValues { _.map { case (t, d) => t.name -> d } } should contain theSameElementsAs Map(
        date(2016, 1, 29) -> Map(),
        date(2016, 1, 30) -> Map(
          "p1-t1" -> (3.hours + 50.minutes).toDuration,
          "p1-t2" -> (3.hours + 10.minutes).toDuration,
          "p1-p11-t3" -> 9.hours.toDuration,
          "p2-t7" -> 30.minutes.toDuration
        ),
        date(2016, 1, 31) -> Map(
          "p1-t1" -> (3.hours + 50.minutes).toDuration,
          "p1-t2" -> (3.hours + 10.minutes).toDuration,
          "p1-p11-t3" -> 9.hours.toDuration,
          "p2-t7" -> (1.hour + 30.minutes).toDuration
        )
      )
    }

    it("should have summary") {
      sum(ts) shouldBe 34.hours.toDuration
    }

    it("should contain all tasks with full name") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p1-t1", "p1-t2", "p1-p11-t3", "p2-t7")
    }

    it("should contain all discrete dates for the interval") {
      dates(ts) should contain theSameElementsAs List(date(2016, 1, 29), date(2016, 1, 30), date(2016, 1, 31))
    }
  }

  describe("Task sheet data report for any monthly interval and all user") {
    val interval = new Interval(yearMonth(2015, 12).toInterval.start, yearMonth(2016, 2).toInterval.end)
    lazy val ts = taskSheetData(IntervalQuery(interval, d => new YearMonth(d)), Empty)

    it("should contain all task items for the given interval") {
      ts mapValues { _.map { case (t, d) => t.name -> d } } should contain theSameElementsAs Map(
        yearMonth(2015, 12) -> Map(),
        yearMonth(2016, 1) -> Map(
          "p1-t1" -> (7.hours + 40.minutes).toDuration,
          "p1-t2" -> (6.hours + 20.minutes).toDuration,
          "p1-p11-t3" -> 18.hours.toDuration,
          "p2-t7" -> 2.hours.toDuration
        ),
        yearMonth(2016, 2) -> Map(
          "p2-t7" -> 1.hour.toDuration,
          "p1-p12-t4" -> (3.hours + 50.minutes).toDuration,
          "p2-t5" -> (3.hours + 10.minutes).toDuration,
          "p2-t6" -> 9.hours.toDuration
        )
      )
    }

    it("should have summary") {
      sum(ts) shouldBe 51.hours.toDuration
    }

    it("should contain all tasks with full name") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p1-t1", "p1-t2", "p1-p11-t3", "p1-p12-t4", "p2-t5", "p2-t6", "p2-t7")
    }

    it("should contain all discrete dates for the interval") {
      dates(ts) should contain theSameElementsAs List(yearMonth(2015, 12), yearMonth(2016, 1), yearMonth(2016, 2))
    }
  }

  def defaultUser(): Box[User] = User.find(By(User.email, "default@mail.com"))

  given {
    val u1 :: u2 :: _ = givenUsers("default", "other") map (_.saveMe())

    val t1 :: t2 :: t3 :: t4 :: t5 :: t6 :: t7 :: _ = traverse(
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
        task("t7"))) map (_.saveMe()) filter (_.selectable.get) map (Full(_))

    val pause = Empty

    givenTaskItems(u1, date(2016, 1, 30),
      t1 -> time(8, 30), t2 -> time(10, 25), pause -> time(12, 0), t3 -> time(12, 30), pause -> time(17, 0), t7 -> time(23, 30)
    ) :::
    givenTaskItems(u1, date(2016, 1, 31),
      pause -> time(0, 30), t1 -> time(8, 30), t2 -> time(10, 25), pause -> time(12, 0), t3 -> time(12, 30), pause -> time(17, 0), t7 -> time(23, 30)
    ) :::
    givenTaskItems(u1, date(2016, 2, 1),
      pause -> time(0, 30), t4 -> time(8, 30), t5 -> time(10, 25), pause -> time(12, 0), t6 -> time(12, 30), pause -> time(17, 0)
    ) :::
    givenTaskItems(u2, date(2016, 1, 30),
      t1 -> time(8, 30), t2 -> time(10, 25), pause -> time(12, 0), t3 -> time(12, 30), pause -> time(17, 0)
    ) :::
    givenTaskItems(u2, date(2016, 1, 31),
      t1 -> time(8, 30), t2 -> time(10, 25), pause -> time(12, 0), t3 -> time(12, 30), pause -> time(17, 0), t7 -> time(23, 30)
    ) :::
    givenTaskItems(u2, date(2016, 2, 1),
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
