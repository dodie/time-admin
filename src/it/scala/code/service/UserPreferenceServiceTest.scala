package code.service

import code.test.utils.{DefaultUser, DbSpec}
import net.liftweb.mockweb.WebSpec

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

  //TODO: write tests for the case when no logged in user
}
