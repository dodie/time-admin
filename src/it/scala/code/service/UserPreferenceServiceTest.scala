package code.service

import code.test.utils.BaseSuite
import code.test.utils.DefaultUser.defaultUser

class UserPreferenceServiceTest extends BaseSuite {

  it("Get returns with a smart default when no preference created previously") { withS(user = defaultUser) {
    UserPreferenceService.getUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime) should be ("0")
  }}

  it("Create a new user preference ") { withS(user = defaultUser) {
    UserPreferenceService.setUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime, "30")

    UserPreferenceService.getUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime) should be ("30")
  }}

  it("Override an existing user preference") { withS(user = defaultUser) {
    UserPreferenceService.setUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime, "30")

    UserPreferenceService.setUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime, "45")

    UserPreferenceService.getUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime) should be ("45")
  }}

  it("Set user preference with no logged in user ") { withS {
    val caught = intercept[NullPointerException] {
      UserPreferenceService.setUserPreference(UserPreferenceNames.timesheetLeaveAdditionalTime, "30")
    }
    caught.getMessage should include ("Only logged in users can edit their preferences!")
  }}
}
