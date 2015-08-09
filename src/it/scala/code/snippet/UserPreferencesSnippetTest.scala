package code.snippet

import code.service.UserPreferenceService
import code.test.utils.{DefaultUser, DbSpec}
import net.liftweb.mockweb.WebSpec

class UserPreferencesSnippetTest extends WebSpec with DbSpec {
  val anyUrl = "http://foo.com/test/settings"

  "Save user settings" withSFor (anyUrl + "?animation=false&timesheet_leave_offtime=true&timesheet_leave_additionaltime=30") in {
    DefaultUser.login()

    new UserPreferencesSnippet().actions(null)

    val animationIsDefined = UserPreferenceService.getUserPreference("animation") must_== "false"
    val leaveOffTimeIsDefined = UserPreferenceService.getUserPreference("timesheet_leave_offtime") must_== "true"
    val leaveAdditionalTimeIsDefined = UserPreferenceService.getUserPreference("timesheet_leave_additionaltime") must_== "30"

    animationIsDefined and leaveOffTimeIsDefined and leaveAdditionalTimeIsDefined
  }
}
