package code
package snippet

import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.NodeSeq
import scala.xml.Text

import code.service.UserPreferenceGroupNames
import code.service.UserPreferenceNames
import code.service.UserPreferenceService
import code.service.UserPreferenceType
import net.liftweb.http.S
import net.liftweb.util.Helpers.AttrBindParam
import net.liftweb.util.Helpers.strToSuperArrowAssoc
import net.liftweb.util.Helpers

/**
 * User settings and preferences.
 * @author David Csakvari
 */
class UserPreferencesSnippet {

  /**
   * Renders a radio input for the given preference.
   */
  private def renderRadio(name: String, values: List[(String, String)], selected: String, tooltip: String): NodeSeq = {
    <span class="radioContainer" data-hint={ tooltip }>
      {
        for (value <- values) yield if (selected equalsIgnoreCase value._1)
          <input type="radio" name={ name } value={ value._1 } checked="true"/><span> { value._2 } </span>
        else
          <input type="radio" name={ name } value={ value._1 }/><span> { value._2 } </span>
      }
    </span>
  }

  /**
   * Renders a text input for the given preference.
   */
  private def renderCheckbox(name: String, checked: Boolean, tooltip: String): NodeSeq = {
    <input type="checkbox" id={ name } name={ name } checked={ checked.toString } data-hint={ tooltip }/>
  }

  /**
   * Renders a text input for the given preference.
   */
  private def renderInput(name: String, value: String, placeholder: String, inputType: String = "text"): NodeSeq = {
    <input type={ inputType } id={ name } name={ name } value={ value } placeholder={ placeholder } data-hint={ placeholder }/>
  }

  /**
   * Renders a field for the given preference.
   */
  private def renderField(preferenceType: UserPreferenceType, preferenceValue: String): NodeSeq = {
    val preferenceName = preferenceType.key.toString
    val information = S.?("userpreference." + preferenceType.description)

    if (preferenceType.inputType == "boolean") {
      renderRadio(preferenceType.key.toString,
        List(("true", "✓"), ("false", "✘")),
        preferenceValue,
        information)
    } else if (preferenceType.inputType == "number") {
      renderInput(preferenceName, preferenceValue, information, "number")
    } else {
      renderInput(preferenceName, preferenceValue, information)
    }
  }

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

  /**
   * Render property or properties.
   *
   * If a "property" attribute is specified, only the given UserPreference will be rendered.
   * Example usage:
   * <span class="lift:userSettingsSnippet:render" property="my-property-key"></span>
   *
   *
   * If the parameter is absent, all of the preferences will be rendered in an editor form templated by the input html.
   * Example usage:
   * <span class="lift:userSettingsSnippet:render">
   * 		<span preference:datahint="">
   * 			<preference:name/><preference:input/>
   * 		</span>
   * </span>
   */
  def render(in: NodeSeq): NodeSeq = {
    val propertyName = in \ "@property"

    if (propertyName.isEmpty) {

      (for (group <- UserPreferenceGroupNames.values) yield <tr><td colspan="2" class="PreferenceGroup">{ S.?("userpreference.group." + group.toString) } <span class="lift:help.quickHelp">{ "userpreference.group." + group.toString + ".help" }</span></td></tr> ++
        (for (
          preference <- UserPreferenceNames.values.toList;
          preferenceType = UserPreferenceService.getType(preference).get;
          preferenceValue = UserPreferenceService.getUserPreference(preferenceType);
          preferenceName = S.?("userpreference." + preferenceType.key.toString);
          preferenceDescription = S.?("userpreference." + preferenceType.description) if preferenceType.group == group
        ) yield Helpers.bind("preference", in,
          "name" -> Text(preferenceName),
          "input" -> renderField(preferenceType, preferenceValue),
          AttrBindParam("datahint", Text(preferenceDescription), "data-hint"))).toSeq.flatMap(a => a)).toSeq.flatMap(a => a)
    } else {
      val preferenceType = UserPreferenceService.getType(propertyName.text).get
      val preferenceValue = UserPreferenceService.getUserPreference(preferenceType)

      renderField(preferenceType, preferenceValue)
    }
  }

  /**
   * Renders content based on specified by key and value for a current user preference.
   *
   * Example usage:
   *
   * <lift-hidden class="lift:userSettingsSnippet.renderConditional" property="my-property-key" value="my-condition-value">
   * 		<iftrue>
   * 			<!-- Content to be rendered if the current user's 'my-property-key' preference equals 'my-condition-value' string. -->
   * 		</iftrue>
   * 		<iffalse>
   * 			<!-- Content to be rendered otherwise. -->
   * 		</iffalse>
   * 	</lift-hidden>
   */
  def renderConditional(in: NodeSeq): NodeSeq = {
    val propertyName = in \ "@property"
    val propertyValue = in \ "@value"

    val ifTrueNode = in \ "iftrue"
    val ifFalseNode = in \ "iffalse"

    if (propertyName.nonEmpty && propertyValue.nonEmpty && UserPreferenceService.getUserPreference(propertyName.text) == propertyValue.text) {
      ifTrueNode \ "_"
    } else {
      ifFalseNode \ "_"
    }
  }
}

