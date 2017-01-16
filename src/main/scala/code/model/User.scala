package code
package model

import net.liftweb.mapper._
import net.liftweb.common._
import scala.xml.NodeSeq

import net.liftweb.http._
import scala.xml.{NodeSeq, Node, Text, Elem}
import net.liftweb.util.Helpers._
import net.liftweb.common._
import S._
import net.liftweb.util.Helpers._

object User extends User with MetaMegaProtoUser[User] with ManyToMany {
  override def dbTableName = "users"

  override def screenWrap = Full(<lift:surround with="client" at="content"><lift:bind /></lift:surround>)

  override def localForm(user: TheUserType, ignorePassword: Boolean, fields: List[FieldPointerType]): NodeSeq = {
    for {
      pointer <- fields
      field <- computeFieldFromPointer(user, pointer).toList
      if field.show_? && (!ignorePassword || !pointer.isPasswordField_?)
      form <- field.toForm.toList
      fieldId <- field.uniqueFieldId
    } yield {
      if (field == password) {
        <div>
          <div class="form-group">
            <label for={fieldId}>{field.displayName}</label>
            {form.map(e => (((e.asInstanceOf[Elem] \\ "input").head).asInstanceOf[Elem]) % ("class" -> "form-control"))}
            <span><lift:Msg id={fieldId} errorClass="edit_error_class"></lift:Msg></span>
          </div>
          <div class="form-group">
            <label for={fieldId}>{S.?("password.repeat")}</label>
            {form.map(e => (((e.asInstanceOf[Elem] \\ "input").drop(1).head).asInstanceOf[Elem]) % ("class" -> "form-control"))}
            <span><lift:Msg id={fieldId} errorClass="edit_error_class"></lift:Msg></span>
          </div>
        </div>
      } else {
        <div class="form-group">
          <label for={fieldId}>{field.displayName}</label>
          {form.map(_.asInstanceOf[Elem] % ("class" -> "form-control"))}
          <span><lift:Msg id={fieldId} errorClass="edit_error_class"></lift:Msg></span>
        </div>
      }
    }
  }

  override def signupFields: List[FieldPointerType] = List(firstName, lastName, email, locale, password)

  override def editFields: List[FieldPointerType] = List(firstName, lastName, email, locale)

  override def signupXhtml(user: TheUserType) = {
      (<form class="form-user" method="post" role="form" action={S.uri}>
        <h1>{ S.?("sign.up") }</h1>
        {localForm(user, false, signupFields)}
        <div class="form-group">
          <input class="btn btn-primary" type="submit" />
        </div>
    </form>)
  }

  // Why do I need to copy-paste this to enable validation?
  override def signup = {
    val theUser: TheUserType = mutateUserOnSignup(createNewUserInstance())
    val theName = signUpPath.mkString("")

    def testSignup() {
      validateSignup(theUser) match {
        case Nil =>
          actionsAfterSignup(theUser, () => S.redirectTo(homePage))

        case xs => S.error(xs) ; signupFunc(Full(innerSignup _))
      }
    }

    def innerSignup = {
      ("type=submit" #> signupSubmitButton(S ? "sign.up", testSignup _)) apply signupXhtml(theUser)
    }

    innerSignup
  }

  override def editXhtml(user: TheUserType) = {
    (<form class="form-user" method="post" role="form" action={S.uri}>
        <h1>{ S.?("edit") }</h1>
        {localForm(user, true, editFields)}
        <div class="form-group">
          <input class="btn btn-primary" type="submit" />
        </div>
    </form>)
  }

  // Why do I need to copy-paste this to enable validation?
  override def edit = {
    val theUser: TheUserType =
      mutateUserOnEdit(currentUser.openOrThrowException("we know we're logged in"))

    val theName = editPath.mkString("")

    def testEdit() {
      theUser.validate match {
        case Nil =>
          theUser.save
          S.notice(S.?("profile.updated"))
          S.redirectTo(homePage)

        case xs => S.error(xs) ; editFunc(Full(innerEdit _))
      }
    }

    def innerEdit = {
      ("type=submit" #> editSubmitButton(S.?("save"), testEdit _)) apply editXhtml(theUser)
    }

    innerEdit
  }

  def edit(user: User) = {
    val theUser: TheUserType =
      mutateUserOnEdit(user)

    val theName = editPath.mkString("")

    def testEdit() {
      theUser.validate match {
        case Nil =>
          theUser.save
          S.notice(S.?("user.profile.updated"))
          S.redirectTo("/admin/users")

        case xs => S.error(xs) ; editFunc(Full(innerEdit _))
      }
    }

    def innerEdit = {
      ("type=submit" #> editSubmitButton(S.?("save"), testEdit _)) apply editXhtml(theUser)
    }

    innerEdit
  }

  override def changePasswordXhtml = {
    (<form class="form-user" method="post" role="form" action={S.uri}>
        <h1>{ S.?("change.password") }</h1>
        <div class="form-group">
            <label>{S.?("old.password")}</label><input type="password" class="old-password form-control" />
        </div>
        <div class="form-group">
            <label>{S.?("new.password")}</label><input type="password" class="new-password form-control" />
        </div>
        <div class="form-group">
            <label>{S.?("repeat.password")}</label><input type="password" class="new-password form-control" />
        </div>
        <div class="form-group">
          <input class="btn btn-primary" type="submit" />
        </div>
    </form>)
  }

  // Why do I need to copy-paste this to enable validation?
  override def changePassword = {
    val user = currentUser.openOrThrowException("we can do this because the logged in test has happened")
    var oldPassword = ""
    var newPassword: List[String] = Nil

    def testAndSet() {
      if (!user.testPassword(Full(oldPassword))) S.error(S.?("wrong.old.password"))
      else {
        user.setPasswordFromListString(newPassword)
        user.validate match {
          case Nil => user.save; S.notice(S.?("password.changed")); S.redirectTo(homePage)
          case xs => S.error(xs)
        }
      }
    }

    val bind = {
      // Use the same password input for both new password fields.
      val passwordInput = SHtml.password_*("", LFuncHolder(s => newPassword = s))

      ".old-password" #> SHtml.password("", s => oldPassword = s) &
      ".new-password" #> passwordInput &
      "type=submit" #> changePasswordSubmitButton(S.?("change"), testAndSet _)
    }

    bind(changePasswordXhtml)
  }

  override def lostPasswordXhtml = {
    (<form class="form-user" method="post" role="form" action={S.uri}>
        <h1>{ S.?("recover.password") }</h1>
        <div class="form-group">
          <label class="recover-description">{S.?("enter.email")}</label>
        </div>
        <div class="form-group">
          <label>{userNameFieldString}</label>
          <input type="text" class="email form-control" />
        </div>
        <div class="form-group">
          <input class="btn btn-primary" type="submit" />
        </div>
     </form>)
  }

  // Why do I need to copy-paste this to enable validation?
  override def lostPassword = {
    val bind =
      ".email" #> SHtml.text("", sendPasswordReset _) &
      "type=submit" #> lostPasswordSubmitButton(S.?("send.it"))

    bind(lostPasswordXhtml)
  }

  override def loginXhtml = {
      (<form class="form-signin" method="post" action={S.uri}>
        <h2 class="form-signin-heading">{S.?("log.in.header")}</h2>
        <label for="inputEmail" class="sr-only">Email address</label>
        <input name="username" class="email form-control" type="email" id="inputEmail" placeholder={userNameFieldString} required="" autofocus=""/>
        <label for="inputPassword" class="sr-only">Password</label>
        <input name="password" type="password" id="inputPassword" class="password form-control" placeholder={S.?("password")} required=""/>
        <button class="btn btn-lg btn-primary btn-block" type="submit">Sign in</button>
        <div class="checkbox">
          <label>
            <input type="checkbox" name ="rememberme" value="remember-me" checked="true"/> {S.?("rememberme")}
          </label>
        </div>
        <div class="to-registration">
          <a href="/user_mgt/sign_up">{S.?("page.registration")}</a>
        </div>
        <div class="lost-password">
          <a href={lostPasswordPath.mkString("/", "/", "")}>{S.?("recover.password")}</a>
        </div>
      </form>)
  }

  override def login = {
    if (S.post_?) {
      S.param("username").
      flatMap(username => findUserByUserName(username)) match {
        case Full(user) if user.validated_? &&
          user.testPassword(S.param("password")) => {
            val preLoginState = capturePreLoginState()
            val redir = loginRedirect.get match {
              case Full(url) =>
                loginRedirect(Empty)
              url
              case _ =>
                homePage
            }

            logUserIn(user, () => {
              S.notice(S.?("logged.in"))
              preLoginState()
              S.param("rememberme") foreach {_ => ExtSession.userDidLogin(user)}
              S.redirectTo(redir)
            })
          }

        case Full(user) if !user.validated_? =>
          S.error(S.?("account.validation.error"))

        case _ => S.error(S.?("invalid.credentials"))
      }
    }

    val bind =
      "type=submit" #> loginSubmitButton(S.?("log.in"))

    bind(loginXhtml)
  }

  override def passwordResetXhtml = {
    (<form class="form-user" method="post" role="form" action={S.uri}>
        <h1>{ S.?("reset.your.password") }</h1>
        <div class="form-group">
          <label>{S.?("enter.your.new.password")}</label>
          <input type="password" class="form-control" />
        </div>
        <div class="form-group">
          <label>{ S.?("repeat.your.new.password") }</label>
          <input type="password" class="form-control" />
        </div>
        <div class="form-group">
          <input class="btn btn-primary" type="submit" />
        </div>
     </form>)
  }

  // Why do I need to copy-paste this to enable validation?
  override def passwordReset(id: String) =
  findUserByUniqueId(id) match {
    case Full(user) =>
      def finishSet() {
        user.validate match {
          case Nil => S.notice(S.?("password.changed"))
            user.resetUniqueId().save
            logUserIn(user, () => S.redirectTo(homePage))

          case xs => S.error(xs)
        }
      }

      val passwordInput = SHtml.password_*("",
        (p: List[String]) => user.setPasswordFromListString(p))


      val bind = {
        "type=password" #> passwordInput &
        "type=submit" #> resetPasswordSubmitButton(S.?("set.password"), finishSet _)
      }

      bind(passwordResetXhtml)
    case _ => S.error(S.?("password.link.invalid")); S.redirectTo(homePage)
  }

  override def fieldOrder = List(id, firstName, lastName, email, locale, timezone, password)

  override def skipEmailValidation = true

  object roles extends MappedManyToMany(UserRoles, UserRoles.user, UserRoles.role, Role)

  onLogOut = List(ExtSession.userDidLogout(_))
}

class User extends MegaProtoUser[User] {
  def getSingleton = User
}

