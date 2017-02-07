package code
package service

import code.model._
import net.liftweb.common.Box
import net.liftweb.mapper.By

/**
 * User Preference service.
 * Handles preferences for the current user and defines possible preference types.
 * @author David Csakvari
 */
object UserPreferenceService {
  private lazy val userPreferenceTypes = List(
    UserPreferenceType(UserPreferenceNames.animation, "boolean", "true", UserPreferenceGroupNames.view),
    UserPreferenceType(UserPreferenceNames.timesheetLeaveOfftime, "boolean", "true", UserPreferenceGroupNames.timesheet),
    UserPreferenceType(UserPreferenceNames.timesheetLeaveAdditionalTime, "number", "0", UserPreferenceGroupNames.timesheet))

  def getType(key: String): Option[UserPreferenceType] = getType(UserPreferenceNames.withName(key))

  def getType(key: UserPreferenceNames.Value): Option[UserPreferenceType] = userPreferenceTypes.find(_.key == key)

  def getUserPreference(key: UserPreferenceNames.Value): String = {
    getUserPreference(key.toString)
  }

  def getUserPreference(k: String): String = {
    def preference(u: User.TheUserType): Box[UserPreference] = UserPreference.find(By(UserPreference.user, u), By(UserPreference.key, k))
    def defaultPreference: String = getType(k).get.defaultValue

    User.currentUser.flatMap(preference).map(_.value.get).getOrElse(defaultPreference)
  }

  def getUserPreference(preference: UserPreferenceType): String = getUserPreference(preference.key)

  def setUserPreference(k: UserPreferenceNames.Value, v: String): Boolean = {
    val currentUser = User.currentUser.openOrThrowException("Only logged in users can edit their preferences!")
    val preference = UserPreference.find(By(UserPreference.user, currentUser), By(UserPreference.key, k.toString))

    def overridePreference(p: UserPreference) = p.value(v).save()
    def createPreference = UserPreference.create.key(k.toString).value(v).user(currentUser).save()

    preference.map(overridePreference).getOrElse(createPreference)
  }

}

/**
 * Preference Type class.
 */
case class UserPreferenceType(key: UserPreferenceNames.Value, inputType: String, defaultValue: String, group: UserPreferenceGroupNames.Value) {
  val description = key + ".description"
}

/**
 * Enum of available preference names.
 */
object UserPreferenceNames extends Enumeration {
  val animation = Value("animation")
  val timesheetLeaveOfftime = Value("timesheet_leave_offtime")
  val timesheetLeaveAdditionalTime = Value("timesheet_leave_additionaltime")
}

/**
 * Enum of available preference group names.
 */
object UserPreferenceGroupNames extends Enumeration {
  val view = Value("view")
  val timesheet = Value("timesheet")
}
