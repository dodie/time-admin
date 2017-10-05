package code
package model

import net.liftweb._
import mapper._
import common._

object ExtSession extends ExtSession with MetaProtoExtendedSession[ExtSession] {
  override def dbTableName = "ext_session"

  val TOKEN_TYPE_WEB = ""
  val TOKEN_TYPE_CLIENT_API = "CLIENT-API"

  def logUserIdIn(uid: String): Unit = User.logUserIdIn(uid)

  def recoverUserId: Box[String] = User.currentUserId

  type UserType = User
}

class ExtSession extends ProtoExtendedSession[ExtSession] {
  object tokentype extends MappedString[ExtSession](this, 140)
  def getSingleton = ExtSession
}
