package code
package snippet

import net.liftweb._
import http._
import scala.xml._

class JsL10n {

  def addJsLoc(in: NodeSeq): NodeSeq = {
    <script>loc.add('{ in.text.trim }', '{ S.?(in.text.trim) }')</script>
  }

}
