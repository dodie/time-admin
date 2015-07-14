package code
package service

import code.model._
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

  def getType(key: UserPreferenceNames.Value): Option[UserPreferenceType] = userPreferenceTypes.filter(_.key == key).headOption

  def getUserPreference(key: UserPreferenceNames.Value): String = {
    getUserPreference(key.toString)
  }

  def getUserPreference(key: String): String = {
    if (User.currentUser.isEmpty) {
      getType(key).get.defaultValue
    } else {
      val pref = UserPreference.find(
        By(UserPreference.user, User.currentUser.get),
        By(UserPreference.key, key))

      if (pref.isEmpty) {
        getType(key).get.defaultValue
      } else {
        pref.get.value.get
      }
    }
  }

  def getUserPreference(preference: UserPreferenceType): String = getUserPreference(preference.key)

  def setUserPreference(key: UserPreferenceNames.Value, value: String) = {
    val preference = UserPreference.find(By(UserPreference.user, User.currentUser.get), By(UserPreference.key, key.toString))
    if (preference.isEmpty) {
      UserPreference.create.key(key.toString).value(value).user(User.currentUser.get).save
    } else {
      preference.get.value(value).save
    }
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
