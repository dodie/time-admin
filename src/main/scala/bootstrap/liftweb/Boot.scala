package bootstrap.liftweb

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import java.util.Locale

import code.export.{ExcelExport, TaskSheetExport}
import code.model._
import code.service.SmtpMailer
import code.service.TaskItemService.IntervalQuery
import code.service.UserService.nonAdmin
import code.snippet.Params.{parseInterval, parseUser}
import code.util.IO.{using, xlsxResponse}
import net.liftmodules.JQueryModule
import net.liftweb._
import net.liftweb.common._
import net.liftweb.db.DBLogEntry
import net.liftweb.http._
import net.liftweb.http.js.JE
import net.liftweb.http.provider._
import net.liftweb.mapper._
import net.liftweb.sitemap.Loc._
import net.liftweb.sitemap._
import net.liftweb.util._
import code.api.Endpoints
import java.io.FileInputStream



/**
 * Allows the application to modify lift's environment based on the configuration.
 * @author David Csakvari
 */
class Boot extends Loggable {
  def boot() {
    // Look for configuration options in the external properties file if provided via -DexternalConfig=<path>
    val externalConfig = System.getProperty("externalConfig")
    if (externalConfig != null) {
      logger.info("Using configuration from classpath and external file: " + externalConfig);
      Props.whereToLook = () => ((externalConfig, () => Full(new FileInputStream(externalConfig))) :: Nil)
    } else {
      logger.info("Using configuration from classpath");
    }

    // DB config
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = Option(System.getenv("DATABASE_URL")).map(new URI(_)).map { uri =>
        val (user, password) = uri.getUserInfo.split(':') match { case Array(a, b, _*) => (Full(a), Full(b)) }
        new StandardDBVendor(
          "org.postgresql.Driver",
          s"jdbc:postgresql://${uri.getHost}:${uri.getPort}${uri.getPath}",
          user, password
        )
      } getOrElse {
        new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
          Props.get("db.url") openOr "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
          Props.get("db.user"), Props.get("db.password")
        )
      }

      LiftRules.unloadHooks.append(vendor.closeAllConnections_!)

      DB.defineConnectionManager(mapper.DefaultConnectionIdentifier, vendor)
    }

    DB.addLogFunc {
      case (log, duration) => {
        logger.trace("Total query time : %d ms".format(duration))
        log.allEntries.foreach {
          case DBLogEntry(stmt, duration) => logger.trace("  %s in %d ms".format(stmt, duration))
        }
      }
    }

    // Entities (Mapper)
    Schemifier.schemify(true, Schemifier.infoF _, User)
    Schemifier.schemify(true, Schemifier.infoF _, Role)
    Schemifier.schemify(true, Schemifier.infoF _, Task)
    Schemifier.schemify(true, Schemifier.infoF _, TaskItem)
    Schemifier.schemify(true, Schemifier.infoF _, UserRoles)
    Schemifier.schemify(true, Schemifier.infoF _, ExtSession)

    // Use extended session
    LiftRules.earlyInStateful.append(req => {
      S.findCookie("ext_id").foreach(cookie => {
        if (User.currentUser.isEmpty) {
          ExtSession.find(By(ExtSession.cookieId, cookie.value openOr "")).foreach(session => {
            ExtSession.testCookieEarlyInStateful(req)
            if (session.tokentype == ExtSession.TOKEN_TYPE_CLIENT_API) {
              S.set("session_type", "client-api")
            }
          })
        }
      })
    })

    // Snippets
    LiftRules.addToPackages("code")

    // Authorization
    val adminRole = Role.find(By(Role.name, "admin")).getOrElse(Role.create.name("admin").saveMe)
    val clientRole = Role.find(By(Role.name, "client")).getOrElse(Role.create.name("client").saveMe)

    // Default user
    if (User.findAll.isEmpty) {
      val defaultUser = User.create.firstName("DEFAULT").lastName("DEFAULT").email("default@tar.hu").password("abc123").validated(true).superUser(true).saveMe()
      UserRoles.create.user(defaultUser).role(adminRole).save
      UserRoles.create.user(defaultUser).role(clientRole).save
    }

    def anonymousUser = User.currentUser.isEmpty

    def hasRole(role: Role) = {
      (for (user <- User.currentUser) yield UserRoles.findAll(By(UserRoles.role, role), By(UserRoles.user, user))) match {
        case Full(roleList) => !roleList.isEmpty
        case _ => false
      }
    }
    
    def isWebSession = !(S.get("session_type").getOrElse("") == "client-api")

    def adminUser = hasRole(adminRole) && isWebSession

    def clientUser = hasRole(clientRole) && isWebSession

    def freshUser = !anonymousUser && !adminUser && !clientUser

    // Build SiteMap.
    def sitemap = SiteMap(
      // login page for anonymous
      Menu.i("page.main") / "index" >> If(anonymousUser _, S ? { if (clientUser) S.redirectTo("/client/tasks") else if (adminUser) S.redirectTo("/admin/projects") else S.redirectTo("/freshuser") }),

      // fresh user page (no roles)
      Menu.i("page.welcome") / "freshuser" >> If(freshUser _, S ? S.redirectTo("/index")),

      // client pages
      Menu(S ? "page.tasks") / "client" / "tasks" >> If(clientUser _, S ? "no_permission"),
      Menu(S ? "page.timesheet") / "report" / "timesheet" >> If(clientUser _, S ? "no_permission"),
      Menu(S ? "page.tasksheet") / "report" / "tasksheet" >> If(clientUser _, S ? "no_permission"),
      Menu(S ? "page.tokens") / "client" / "tokens" >> If(clientUser _, S ? "no_permission") >> Hidden,
      Menu(S ? "page.collaborators") / "client" / "collaborators" >> If(clientUser _, S ? "no_permission"),
      
      // admin pages
      Menu(S ? "page.projects") / "admin" / "projects" >> If(adminUser _, S ? "no_permission"),
      Menu(S ? "page.users") / "admin" / "users" >> If(adminUser _, S ? "no_permission"),
      Menu(S ? "page.edituser") / "admin" / "user" >> If(adminUser _, S ? "no_permission") >> Hidden,

      // user pages
      Menu(S ? "page.userspages") / "userpages" >> Hidden >> User.AddUserMenusUnder
    )

    def sitemapMutators = User.sitemapMutator

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemapMutators(sitemap))

    // Use jQuery 1.4
    //LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQueryArtifacts
    JQueryModule.InitParam.JQuery=JQueryModule.JQuery1113
    JQueryModule.init()

    // Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart = Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd = Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Function to test if a user is logged in
    LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))

    // Use user locale
    val oldLocaleCalculator = LiftRules.localeCalculator
    LiftRules.localeCalculator = (request: Box[HTTPRequest]) => User.currentUser.map(u => new Locale(u.locale.get)) openOr oldLocaleCalculator(request)

    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)

    SmtpMailer.init

    // REST API
    LiftRules.dispatch.append {
      // personal timesheet export
      case Req("export" :: "timesheet" :: offset :: Nil, "", GetRequest) =>
        () => {
          // access control
          if (!clientUser) {
            Full(RedirectResponse("/"))
          } else {
            val user = User.currentUser filter nonAdmin or parseUser()
            val (contentStream, fileName) = ExcelExport.exportTimesheet(user, offset.toInt)
            Full(StreamingResponse(contentStream, () =>
              contentStream.close,
              contentStream.available,
              List(
                "Content-Type" -> "application/vnd.ms-excel",
                "Content-Disposition" -> ("attachment; filename=\"" + fileName + "\"")
                ), Nil, 200))
          }
        }

        // personal tasksheet export
      case Req("export" :: "tasksheet" :: Nil, "", GetRequest) =>
        () => {
          // access control
          if (!clientUser) {
            Full(RedirectResponse("/"))
          } else {
            val dimension = S.param("dimension").getOrElse("minutes");
            val taskFilter = S.param("taskFilter").getOrElse("")
            val interval = parseInterval getOrElse IntervalQuery.thisMonth()
            val user = User.currentUser filter nonAdmin or parseUser()
            val (xlsx, name) = TaskSheetExport.workbook(interval, user, dimension, taskFilter)

            val contentStream = using(new ByteArrayOutputStream()) { out =>
              xlsx.write(out)
              out.flush()
              new ByteArrayInputStream(out.toByteArray)
            }

            Full(xlsxResponse(contentStream, name.toLowerCase.replace(" ", "")))
          }
        }
    }

    LiftRules.securityRules = () => {
      SecurityRules(content = Some(ContentSecurityPolicy(
        scriptSources = List(
            ContentSourceRestriction.Self, ContentSourceRestriction.UnsafeInline),
        styleSources = List(
            ContentSourceRestriction.Self, ContentSourceRestriction.UnsafeInline)
            )))
    }

    val jsNotice =
      """$('#lift__noticesContainer___notice ul')
        |.addClass("alert alert-success alert-main alert-dismissible")
        |.prepend('<button type="button" class="close" data-dismiss="alert">&times;</button>')""".stripMargin

    val jsWarning =
      """$('#lift__noticesContainer___warning ul')
        |.addClass("alert alert-warning alert-main alert-dismissible")
        |.prepend('<button type="button" class="close" data-dismiss="alert">&times;</button>')""".stripMargin

    val jsError =
      """$('#lift__noticesContainer___error ul')
        |.addClass("alert alert-danger alert-main alert-dismissible")
        |.prepend('<button type="button" class="close" data-dismiss="alert">&times;</button>')""".stripMargin

    LiftRules.noticesEffects.default.set(
      Vendor.valToVendor((notice, _) =>
        notice.map(_.title) match {
          case Full("Notice") => Full(JE.JsRaw(jsNotice).cmd)
          case Full("Warning") => Full(JE.JsRaw(jsWarning).cmd)
          case Full("Error") => Full(JE.JsRaw(jsError).cmd)
          case _ => Full(JE.JsRaw(jsNotice).cmd)
        }
      ))
    
    val isApiEnabled = Props.get("api.enable") openOr "false"
    if (isApiEnabled == "true") {
      LiftRules.dispatch.append(Endpoints)
    }
  }
}
