package code
package snippet

import _root_.scala.xml.NodeSeq
import _root_.net.liftweb.util.Helpers
import code.model._
import net.liftweb.mapper.By
import net.liftweb.common.Full
import net.liftweb.http.js.JsCmds
import net.liftweb.util.BindHelpers
import net.liftweb.http.S
import Helpers._
import java.util.Date
import java.util.Random
import scala.xml._
import net.liftweb.util.BindHelpers.strToCssBindPromoter

/**
 * User data snippet.
 * @author David Csakvari
 */
class UsersSnippet {

  /**
   * Renders the current user name.
   */
  def currentUser(in: NodeSeq): NodeSeq = {
    if (!User.currentUser.isEmpty) {
      (
        ".ActualUserEmail *" #> User.currentUser.get.niceName
      ).apply(in)
    } else {
      NodeSeq.Empty
    }
  }

  /**
   * Renders the login box.
   */
  def loginBox(in: NodeSeq): NodeSeq = {
    if (User.currentUser.isEmpty) {
      User.login
    } else {
      (
        ".NiceName *" #> User.currentUser.get.niceName
      ).apply(in)
    }
  }

  /**
   * Renders links to the user management pages.
   */
  def userPages(in: NodeSeq): NodeSeq = {
    val menuItems = {
      if (!User.currentUser.isEmpty) {
        List(
          ("/client/settings", S.?("page.settings")),
          ("/user_mgt/change_password", S.?("page.change_password")),
          ("/user_mgt/edit", S.?("page.my_profile")),
          ("/user_mgt/logout", S.?("page.logout"))
        )
      } else {
        List(
          ("/user_mgt/sign_up", S.?("page.registration"))
        )
      }
    }

    (".userPageLink" #> menuItems.map(item => {
      ".userPageLink [href]" #> item._1 &
        ".userPageLink *" #> item._2
    })).apply(in)
  }

  /**
   * Renders the given NodeSeq if the default user is active.
   * The message should remind the user to disable this user.
   */
  def checkAndWarnDefaultUser(in: NodeSeq): NodeSeq = {
    if (!User.find(By(User.email, "default@tar.hu"), By(User.validated, true)).isEmpty) {
      in
    } else {
      NodeSeq.Empty
    }
  }

  /**
   * Renders a list of the users and permissions.
   */
  def listUsers(in: NodeSeq): NodeSeq = {
    val roles = Role.findAll.sortWith((a, b) => a.name.get.compareTo(b.name.get) < 0)
    val users = User.findAll.sortWith((a, b) => a.niceName.compareTo(b.niceName) < 0)

    (
      ".header" #> roles.map(role => ".header *" #> role.name.get) &
      ".user-row" #> {
        users.map(user => {
          ".userName *" #> user.niceName &
            ".userName [href]" #> ("/admin/user.html?edit=" + user.id.get) &
            ".userRole" #> roles.map(role => {
              "input [name]" #> (user.id.get + "_" + role.id.get) &
                (
                  if (!UserRoles.findAll(By(UserRoles.user, user), By(UserRoles.role, role)).isEmpty) {
                    "input [checked]" #> "true"
                  } else {
                    "input [unchecked]" #> "true"
                  }
                )
            })
        })
      }
    ).apply(in)
  }

  /**
   * Renders a user editor.
   */
  def userEditor(in: NodeSeq): NodeSeq = {
    val userIdBox = S.param("edit")

    if (!userIdBox.isEmpty) {
      val user = User.findByKey(userIdBox.get.toLong)
      user.get.toForm(Full("save"), "/admin/users")
    } else {
      <lift:embed what="no_data"/>
    }
  }

  /**
   * Process actions: save a user or modify user roles.
   */
  def actions(in: NodeSeq): NodeSeq = {
    if (S.post_?) {
      if (!S.param("useredit").isEmpty && !S.param("edit").isEmpty) {
        User.findByKey(S.param("edit").get.toLong).get.toForm(Full("save"), "/item/list")
      } else {
        val roles = Role.findAll.sortWith((a, b) => a.name.get.compareTo(b.name.get) < 0)
        val users = User.findAll.sortWith((a, b) => a.niceName.compareTo(b.niceName) < 0)

        var changed = false
        for (user <- users) {
          for (role <- roles) {
            val roleMapParamBox = S.param(user.id.get + "_" + role.id.get).getOrElse("off")
            val roleMapParam = roleMapParamBox.equalsIgnoreCase("on")
            val userRoles = UserRoles.findAll(By(UserRoles.user, user), By(UserRoles.role, role))
            if (roleMapParam) {
              if (userRoles.isEmpty) {
                changed = true
                UserRoles.create.user(user).role(role).save
              }
            } else {
              changed = true
              userRoles.foreach(_.delete_!)
            }
          }
        }

        if (changed) {
          S.redirectTo(S.uri)
        }
      }
    }
    NodeSeq.Empty
  }
}
