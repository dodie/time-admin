package code
package snippet

import _root_.scala.xml.NodeSeq
import net.liftweb.http.S

/**
 * Help, description and tutorial displaying component.
 * @author David Csakvari
 */
class Help {

  /**
   * Renders localized quick help
   */
  def quickHelp(in: NodeSeq): NodeSeq = {
    <a class="QuickHelp" data-hint={ S.?(in.text.trim) }>?</a>
  }
}
