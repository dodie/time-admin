package code
package snippet

import code.service.{UserPreferenceGroupNames, UserPreferenceNames, UserPreferenceService}
import net.liftweb.http.S
import net.liftweb.util.BindHelpers.strToCssBindPromoter

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq

/**
 * User settings and preferences.
 * @author David Csakvari
 */
class UserPreferencesSnippet {
  /**
   * Actions. Processes user preference updates.
   */
  def actions(in: NodeSeq): NodeSeq = {
    for {
      k <- UserPreferenceNames.values
      v <- S.param(k.toString)
    } yield UserPreferenceService.setUserPreference(k, v)
    <div style="display:none;">No action.</div>
  }

  def render(in: NodeSeq): NodeSeq = {
    "tbody" #> {
      UserPreferenceGroupNames.values.map { g =>
        ".PreferenceGroup" #> {
          ".PreferenceGroupName *" #> S.?("userpreference.group." + g.toString) &
            ".QuickHelp [data-hint]" #> ("userpreference.group." + g.toString + ".help")
        } &
        ".Preference" #> UserPreferenceNames.values.filter { p =>
          UserPreferenceService.getType(p).get.group == g
        }.map { p =>
          val pType = UserPreferenceService.getType(p).get
          val value = UserPreferenceService.getUserPreference(pType)
          ".PreferenceName" #> {
            "span [data-hint]" #> S.?("userpreference." + pType.description) &
            "span *" #> S.?("userpreference." + pType.key.toString)
          } &
          ".PreferenceValue *" #> {
            if (pType.inputType == "boolean") switch(pType.key.toString, value == "true", S.?("userpreference." + pType.description))
            else if (pType.inputType == "number") number(pType.key.toString, value, S.?("userpreference." + pType.description))
            else text(pType.key.toString, value, S.?("userpreference." + pType.description))
          }
        }
      }
    } apply in
  }

  def text(name: String, value: String, info: String): NodeSeq =
    <input type="text" name={name} value={value} placeholder={info} data-hint={info}></input>

  def number(name: String, value: String, info: String): NodeSeq =
    <input type="number" name={name} value={value} placeholder={info} data-hint={info}></input>

  def switch(name: String, value: Boolean, info: String): NodeSeq =
    <span class="radioContainer" data-hint={info}>{
      if (value) {
        <input type="radio" name={name} value={true.toString} checked="true"></input> <span>✓</span>
        <input type="radio" name={name} value={false.toString}></input> <span>✘</span>
      } else {
        <input type="radio" name={name} value={true.toString}></input> <span>✓</span>
        <input type="radio" name={name} value={false.toString} checked="true"></input> <span>✘</span>
      }
    }</span>
}

