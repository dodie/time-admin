package code.snippet

import code.model._
import code.service.UserPreferenceService
import net.liftweb.mapper.By
import net.liftweb.mockweb.WebSpec

class UserPreferencesSnippetTest extends WebSpec with DbSpec {
  val anyUrl = "http://foo.com/test/settings"

  "Save user settings" withSFor (anyUrl + "?animation=false&timesheet_leave_offtime=true&timesheet_leave_additionaltime=30") in {
    loginDefaultUser()

    new UserPreferencesSnippet().actions(null)

    val animationIsDefined = UserPreferenceService.getUserPreference("animation") must_== "false"
    val leaveOffTimeIsDefined = UserPreferenceService.getUserPreference("timesheet_leave_offtime") must_== "true"
    val leaveAdditionalTimeIsDefined = UserPreferenceService.getUserPreference("timesheet_leave_additionaltime") must_== "30"

    animationIsDefined and leaveOffTimeIsDefined and leaveAdditionalTimeIsDefined
  }

  //FIXME: I would prefer a solution to login with default user where I don't have to call itt explicitly
  // or I can define the logged in user as the part of the test definition.
  def loginDefaultUser(): Unit = {
    User.logUserIn(User.find(By(User.email, "default@tar.hu")).getOrElse {
      val adminRole = Role.find(By(Role.name, "admin")).getOrElse(Role.create.name("admin").saveMe)
      val clientRole = Role.find(By(Role.name, "client")).getOrElse(Role.create.name("client").saveMe)

      val defaultUser = User.create
        .firstName("DEFAULT")
        .lastName("DEFAULT")
        .email("default@tar.hu")
        .password("abc123")
        .validated(true)
        .superUser(true)
        .saveMe()
      UserRoles.create.user(defaultUser).role(adminRole).save
      UserRoles.create.user(defaultUser).role(clientRole).save
      defaultUser
    })
  }
}
