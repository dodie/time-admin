package code.service

import code.model.{Project, Task, TaskItem, User}
import code.test.utils.BaseSuite
import code.util.TaskSheetUtils.{dates, sum, tasks}
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.mapper.By
import com.github.nscala_time.time.Imports._

class ReportServiceTest extends BaseSuite {

  describe("Report task sheet data for default user and single day") {
    lazy val ts = ReportService.taskSheetData(date(2016, 1, 30).toInterval(), identity, defaultUser())

    it("the summary should be 8 hours") {
      sum(ts) shouldBe 8.hours.toDuration
    }

    it("should contain all tasks for the interval") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p1-t1", "p1-t2", "p1-p11-t3")
    }

    it("should contain all dates for the interval") {
      dates(ts) should contain theSameElementsAs List(date(2016, 1, 30))
    }
  }

  describe("Report task sheet data for default user and overflowed day") {
    lazy val ts = ReportService.taskSheetData(date(2016, 1, 31).toInterval(), identity, defaultUser())

    it("the summary should be 8 and a half hours") {
      sum(ts) shouldBe (8.hours + 30.minutes).toDuration
    }

    it("should contain all tasks for the interval") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p1-t1", "p1-t2", "p1-p11-t3", "p2-t7")
    }

    it("should contain all dates for the interval") {
      dates(ts) should contain theSameElementsAs List(date(2016, 1, 31))
    }
  }

  describe("Report task sheet data for default user and day with slipped tasks") {
    lazy val ts = ReportService.taskSheetData(date(2016, 2, 1).toInterval(), identity, defaultUser())

    it("the summary should be 8 and a half hours") {
      sum(ts) shouldBe (8.hours + 30.minutes).toDuration
    }

    it("should contain all tasks for the interval") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p2-t7", "p1-p12-t4", "p2-t5", "p2-t6")
    }

    it("should contain all dates for the interval") {
      dates(ts) should contain theSameElementsAs List(date(2016, 2, 1))
    }
  }

  describe("Report task sheet data for default user and single month") {
    lazy val ts = ReportService.taskSheetData(new YearMonth(2016, 1).toInterval(), identity, defaultUser())

    it("the summary should be 16 and a half hours") {
      sum(ts) shouldBe (16.hours + 30.minutes).toDuration
    }

    it("should contain all tasks for the interval") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p1-t1", "p1-t2", "p1-p11-t3", "p2-t7")
    }

    it("should contain all dates for the interval") {
      dates(ts) should contain theSameElementsAs Stream.iterate(date(2016, 1, 1))(_.plusDays(1)).take(31)
    }
  }

  def defaultUser(): Box[User] = User.find(By(User.email, "default@mail.com"))

  given {
    val u1 :: u2 :: _ = givenUsers("default", "other") map (_.saveMe())

    val p1 :: p11 :: p12 :: _ = givenProjects("p1", "p11", "p12") map (_.saveMe())
    val p2 :: _ = givenProjects("p2") map (_.saveMe())

    val t1 :: t2 :: t3 :: t4 :: t5 :: t6 :: t7 :: _ = givenTasks("t1" -> p1, "t2" -> p1, "t3" -> p11, "t4" -> p12, "t5" -> p2, "t6" -> p2, "t7" -> p2) map (_.saveMe()) map (Full(_))

    val pause = Empty

    givenTaskItems(u1, date(2016, 1, 30),
      t1 -> time(8, 30), t2 -> time(10, 25), pause -> time(12, 0), t3 -> time(12, 30), pause -> time(17, 0)
    ) :::
    givenTaskItems(u1, date(2016, 1, 31),
      t1 -> time(8, 30), t2 -> time(10, 25), pause -> time(12, 0), t3 -> time(12, 30), pause -> time(17, 0), t7 -> time(23, 30)
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

  def givenProjects(name: String, children: String*): List[Project] =
    List(Project.create.name(name).active(true)) flatMap { p =>
      p :: { children map (n => Project.create.name(n).parent(p).active(true)) toList }
    }

  def givenTasks(ts: (String, Project)*): List[Task] =
    ts map { case (t, p) => Task.create.name(t).parent(p).active(true) } toList

  def givenTaskItems(u: User, ld: LocalDate, tis: (Box[Task], LocalTime)*): List[TaskItem] =
    tis map { case (t, lt) => TaskItem.create.user(u).task(t).start(ld.toDateTime(lt).getMillis) } toList

  def date(year: Integer, monthOfYear: Integer, dayOfMonth: Integer): LocalDate = new LocalDate(year, monthOfYear, dayOfMonth)

  def time(hourOfDay: Integer, minuteOfHour: Integer): LocalTime = new LocalTime(hourOfDay, minuteOfHour)
}
