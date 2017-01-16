package code.service

import code.model.{Project, Task, TaskItem, User}
import code.test.utils.BaseSuite
import code.util.TaskSheetUtils.{dates, sum, tasks}
import net.liftweb.mapper.By
import org.joda.time.{DateTime, LocalDate, YearMonth}

class ReportServiceTest extends BaseSuite {

  describe("Task sheet data for default user and single day") {
    lazy val ts = ReportService.taskSheetData(
      new LocalDate(2016, 1, 30).toInterval(),
      d => d,
      User.find(By(User.email, "default@tar.hu"))
    )

    it("the summary should be 8 hours") {
      sum(ts).getStandardHours should be (8L)
    }

    it("should contain all tasks for the interval") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p1-t1", "p1-t2", "p1-p11-t3")
    }

    it("should contain all dates for the interval") {
      dates(ts) should contain theSameElementsAs List(new LocalDate(2016, 1, 30))
    }
  }

  describe("Task sheet data for default user and overflowed day") {
    lazy val ts = ReportService.taskSheetData(
      new LocalDate(2016, 1, 31).toInterval(),
      d => d,
      User.find(By(User.email, "default@tar.hu"))
    )

    it("the summary should be 8 and a half hours") {
      sum(ts).getStandardMinutes should be (8L * 60L + 30L)
    }

    it("should contain all tasks for the interval") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p1-t1", "p1-t2", "p1-p11-t3", "p2-t7")
    }

    it("should contain all dates for the interval") {
      dates(ts) should contain theSameElementsAs List(new LocalDate(2016, 1, 31))
    }
  }

  describe("Task sheet data for default user and day with slipped tasks") {
    lazy val ts = ReportService.taskSheetData(
      new LocalDate(2016, 2, 1).toInterval(),
      d => d,
      User.find(By(User.email, "default@tar.hu"))
    )

    it("the summary should be 8 and a half hours") {
      sum(ts).getStandardMinutes should be (8L * 60L + 30L)
    }

    it("should contain all tasks for the interval") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p2-t7", "p1-p12-t4", "p2-t5", "p2-t6")
    }

    it("should contain all dates for the interval") {
      dates(ts) should contain theSameElementsAs List(new LocalDate(2016, 2, 1))
    }
  }

  describe("Task sheet data for default user and single month") {
    lazy val ts = ReportService.taskSheetData(
      new YearMonth(2016, 1).toInterval(),
      d => d,
      User.find(By(User.email, "default@tar.hu"))
    )

    it("the summary should be 16 and a half hours") {
      sum(ts).getStandardMinutes should be (16L * 60L + 30L)
    }

    it("should contain all tasks for the interval") {
      tasks(ts) map (_.name) should contain theSameElementsAs List("p1-t1", "p1-t2", "p1-p11-t3", "p2-t7")
    }

    it("should contain all dates for the interval") {
      dates(ts) should contain theSameElementsAs Stream.iterate(new LocalDate(2016, 1, 1))(_.plusDays(1)).take(31)
    }
  }

  given {
    val u1 = User.create
      .firstName("DEFAULT")
      .lastName("DEFAULT")
      .email("default@tar.hu")
      .password("abc123")
      .validated(true)
      .superUser(true)
      .saveMe()

    val p1 = Project.create.name("p1").active(true).saveMe()
    val p11 = Project.create.name("p11").parent(p1).active(true).saveMe()
    val p12 = Project.create.name("p12").parent(p1).active(true).saveMe()
    val p2 = Project.create.name("p2").active(true).saveMe()


    val t1 = Task.create.name("t1").parent(p1).active(true).saveMe()
    val t2 = Task.create.name("t2").parent(p1).active(true).saveMe()
    val t3 = Task.create.name("t3").parent(p11).active(true).saveMe()
    val t4 = Task.create.name("t4").parent(p12).active(true).saveMe()
    val t5 = Task.create.name("t5").parent(p2).active(true).saveMe()
    val t6 = Task.create.name("t6").parent(p2).active(true).saveMe()
    val t7 = Task.create.name("t7").parent(p2).active(true).saveMe()

    TaskItem.create.user(u1).task(t1).start(new DateTime(2016, 1, 30, 8, 30).getMillis).save()
    TaskItem.create.user(u1).task(t2).start(new DateTime(2016, 1, 30, 10, 25).getMillis).save()
    TaskItem.create.user(u1).task(-1L).start(new DateTime(2016, 1, 30, 12, 0).getMillis).save()
    TaskItem.create.user(u1).task(t3).start(new DateTime(2016, 1, 30, 12, 30).getMillis).save()
    TaskItem.create.user(u1).task(-1L).start(new DateTime(2016, 1, 30, 17, 0).getMillis).save()

    TaskItem.create.user(u1).task(t1).start(new DateTime(2016, 1, 31, 8, 30).getMillis).save()
    TaskItem.create.user(u1).task(t2).start(new DateTime(2016, 1, 31, 10, 25).getMillis).save()
    TaskItem.create.user(u1).task(-1L).start(new DateTime(2016, 1, 31, 12, 0).getMillis).save()
    TaskItem.create.user(u1).task(t3).start(new DateTime(2016, 1, 31, 12, 30).getMillis).save()
    TaskItem.create.user(u1).task(-1L).start(new DateTime(2016, 1, 31, 17, 0).getMillis).save()

    TaskItem.create.user(u1).task(t7).start(new DateTime(2016, 1, 31, 23, 30).getMillis).save()
    TaskItem.create.user(u1).task(-1L).start(new DateTime(2016, 2, 1, 0, 30).getMillis).save()

    TaskItem.create.user(u1).task(t4).start(new DateTime(2016, 2, 1, 8, 30).getMillis).save()
    TaskItem.create.user(u1).task(t5).start(new DateTime(2016, 2, 1, 10, 25).getMillis).save()
    TaskItem.create.user(u1).task(-1L).start(new DateTime(2016, 2, 1, 12, 0).getMillis).save()
    TaskItem.create.user(u1).task(t6).start(new DateTime(2016, 2, 1, 12, 30).getMillis).save()
    TaskItem.create.user(u1).task(-1L).start(new DateTime(2016, 2, 1, 17, 0).getMillis).save()
  }
}
