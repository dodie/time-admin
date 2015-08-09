package code.service

import code.test.utils.{DefaultUser, DbSpec}
import net.liftweb.mockweb.WebSpec
import org.scalatest.Matchers

class UserPreferenceServiceTest extends WebSpec with DbSpec {
  val anyUrl = "http://foo.com/test/settings"


  "Get returns with a smart default when no preference created previously" withSFor anyUrl in {
    DefaultUser.login()

    UserPreferenceService.getUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime) must_== "0"
  }

  "Create a new user preference " withSFor anyUrl in {
    DefaultUser.login()

    UserPreferenceService.setUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime, "30")

    UserPreferenceService.getUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime) must_== "30"
  }

  "Override an existing user preference " withSFor anyUrl in {
    DefaultUser.login()

    UserPreferenceService.setUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime, "30")

    UserPreferenceService.setUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime, "45")

    UserPreferenceService.getUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime) must_== "45"
  }

  "Set user preference with no logged in user " withSFor anyUrl in {
    var errorMessage = ""
    try {
      UserPreferenceService.setUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime, "30")
    } catch {
      case e: NullPointerException => errorMessage = e.getMessage
    }
    errorMessage must contain("Only logged in users can edit their preferences!")
  }
}
