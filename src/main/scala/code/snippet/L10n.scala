package code
package snippet

import net.liftweb._
import http._
import scala.xml._

object L10n extends DispatchSnippet {
  def dispatch: DispatchIt = {
    case "render" => ns => replace(ns)
    case "i" => ns => inject(ns)
    case str => ns => attr(ns, str)
  }

  def inject(ns: NodeSeq): NodeSeq = ns match {
    case Elem(prefix, label, attribs, scope, child @ _*) =>
      Elem(prefix, label, attribs, scope, true, S.loc(ns.text, Text(ns.text)): _*)
    case _ => ns
  }

  def replace(ns: NodeSeq): NodeSeq = S.loc(ns.text, Text(ns.text))

  def attr(ns: NodeSeq, attrName: String) = ns match {
    case Elem(prefix, label, attribs, scope, child @ _*) =>
      Elem(prefix, label, locAttrib(attribs, attrName), scope, true, child: _*)
    case _ => ns
  }

  private def locAttrib(attribs: MetaData, attrName: String) = attribs.get(attrName) match {
    case Some(Text(str)) =>
      attribs.append(new UnprefixedAttribute(attrName, S.loc(str, Text(str)), Null))
    case _ => attribs
  }
}
