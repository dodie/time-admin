package code
package service

import javax.mail.{Authenticator, PasswordAuthentication}
import javax.mail.internet.MimeMessage

import net.liftweb._
import common._
import util._

/*
 * A Mailer config object that uses Props and auto configures for gmail
 * if detected.
 */
object SmtpMailer extends Loggable {
  def init(): Unit = {

    var isAuth = Props.get("mail.smtp.auth", "false").toBoolean

    Mailer.customProperties = Props.get("mail.smtp.host", "localhost") match {
      case "smtp.gmail.com" => // auto configure for gmail
        isAuth = true
        Map(
          "mail.smtp.host" -> "smtp.gmail.com",
          "mail.smtp.port" -> "587",
          "mail.smtp.auth" -> "true",
          "mail.smtp.starttls.enable" -> "true"
        )
      case h => Map(
        "mail.smtp.host" -> h,
        "mail.smtp.port" -> Props.get("mail.smtp.port", "25"),
        "mail.smtp.auth" -> isAuth.toString
      )
    }

    //Mailer.devModeSend.default.set((m : MimeMessage) => logger.info("Sending Mime Message: "+m))

    if (isAuth) {
      (Props.get("mail.smtp.user"), Props.get("mail.smtp.pass")) match {
        case (Full(username), Full(password)) =>
          logger.info("Smtp user: %s".format(username))
          logger.info("Smtp password length: %s".format(password.length))
          Mailer.authenticator = Full(new Authenticator() {
            override def getPasswordAuthentication = new
              PasswordAuthentication(username, password)
          })
          logger.info("SmtpMailer inited")
        case _ => logger.error("Username/password not supplied for Mailer.")
      }
    }
  }
}
