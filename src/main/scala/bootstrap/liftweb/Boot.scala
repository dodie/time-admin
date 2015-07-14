package bootstrap.liftweb

import java.net.URI

import net.liftweb._
import util._
import Helpers._
import common._
import sitemap._
import Loc._
import mapper._
import code.model._
import code.export.ExcelExport
import code.commons.TimeUtils

import net.liftweb.http.provider._
import net.liftweb.http._
import java.util.Locale

/**
 * Allows the application to modify lift's environment based on the configuration.
 * @author David Csakvari
 */
class Boot {
  def boot() {
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

    // Entities (Mapper)
    Schemifier.schemify(true, Schemifier.infoF _, User)
    Schemifier.schemify(true, Schemifier.infoF _, Project)
    Schemifier.schemify(true, Schemifier.infoF _, Role)
    Schemifier.schemify(true, Schemifier.infoF _, Task)
    Schemifier.schemify(true, Schemifier.infoF _, TaskItem)
    Schemifier.schemify(true, Schemifier.infoF _, UserPreference)
    Schemifier.schemify(true, Schemifier.infoF _, UserRoles)
    Schemifier.schemify(true, Schemifier.infoF _, ExtSession)

    // Use extended session
    LiftRules.earlyInStateful.append(ExtSession.testCookieEarlyInStateful)

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
    def notAnonymousUser = !anonymousUser
    def adminUser = notAnonymousUser && !UserRoles.findAll(By(UserRoles.role, adminRole), By(UserRoles.user, User.currentUser.get)).isEmpty
    def clientUser = notAnonymousUser && !UserRoles.findAll(By(UserRoles.role, clientRole), By(UserRoles.user, User.currentUser.get)).isEmpty
    def freshUser = notAnonymousUser && !adminUser && !clientUser
    def falsyUser = false

    // Build SiteMap.
    def sitemap = SiteMap(
      // login page for anonymous
      Menu.i("page.main") / "index" >> If(anonymousUser _, S ? { if (clientUser) S.redirectTo("/client/tasks") else if (adminUser) S.redirectTo("/admin/projects") else S.redirectTo("/freshuser") }),

      // fresh user page (no roles)
      Menu.i("page.welcome") / "freshuser" >> If(freshUser _, S ? S.redirectTo("/index")),

      // client pages
      Menu(S ? "page.tasks") / "client" / "tasks" >> If(clientUser _, S ? "no_permission"),

      // report pages
      Menu(S ? "page.dailysummary") / "report" / "dailysummary" >> If(clientUser _, S ? "no_permission"),
      Menu(S ? "page.tasksheet") / "report" / "tasksheet" >> If(clientUser _, S ? "no_permission"),
      Menu(S ? "page.timesheet") / "report" / "timesheet" >> If(clientUser _, S ? "no_permission"),

      // admin pages
      Menu(S ? "page.projects") / "admin" / "projects" >> If(adminUser _, S ? "no_permission"),
      Menu(S ? "page.users") / "admin" / "users" >> If(adminUser _, S ? "no_permission"),
      Menu(S ? "page.edituser") / "admin" / "user" >> If(adminUser _, S ? "no_permission") >> Hidden,
      //Menu(S ? "page.stats") / "admin" / "usertasks" >> If(adminUser _, S ? "no_permission"), // admin statistics page - refactor needed

      // user pages
      Menu(S ? "page.settings") / "client" / "settings" >> If(notAnonymousUser _, S ? "not_logged_in") >> Hidden,
      Menu(S ? "page.userspages") / "userpages" >> Hidden >> User.AddUserMenusUnder
    )

    def sitemapMutators = User.sitemapMutator

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemapMutators(sitemap))

    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQueryArtifacts

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

    LiftRules.liftRequest.append {
      case Req("classpath" :: _, _, _) => true
      case Req("ajax_request" :: _, _, _) => true
      case Req("comet_request" :: _, _, _) => true
      case Req("favicon" :: Nil, "ico", GetRequest) => false
      case Req(_, "css", GetRequest) => false
      case Req(_, "js", GetRequest) => false
    }

    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)

    // REST API
    LiftRules.dispatch.append {
      // personal timesheet export
      case Req("export" :: "timesheet" :: offset :: Nil, "", GetRequest) =>
        () => {
          // access control
          if (!clientUser) {
            Box(RedirectResponse("/"))
          } else {
            for {
              stream <- tryo(ExcelExport.exportTimesheet(User.currentUser.get.id.get, offset.toInt))
              if null ne stream
            } yield StreamingResponse(stream, () => stream.close,
              stream.available, List("Content-Type" -> "application/vnd.ms-excel", "Content-Disposition" -> ("attachment; filename=\"timesheet_" + TimeUtils.currentYear(offset.toInt) + "-" + (TimeUtils.currentMonth(offset.toInt) + 1) + ".xls\"")), Nil, 200)
          }
        }

        // blank tasksheet export
        case Req("export" :: "tasksheet" :: "blank" :: offset :: Nil, "", GetRequest) =>
        () => {
          // access control
          if (!adminUser) {
            Box(RedirectResponse("/"))
          } else {
            for {
              stream <- tryo(ExcelExport.exportTasksheet(true, offset.toInt))
              if null ne stream
            } yield StreamingResponse(stream, () => stream.close,
              stream.available, List("Content-Type" -> "application/vnd.ms-excel", "Content-Disposition" -> ("attachment; filename=\"tasksheet_" + TimeUtils.currentYear(offset.toInt) + "-" + (TimeUtils.currentMonth(offset.toInt) + 1) + "_" + User.currentUser.get.firstName.get.toLowerCase + User.currentUser.get.lastName.get.toLowerCase + ".xls\"")), Nil, 200)
          }
        }
        // personal tasksheet export
        case Req("export" :: "tasksheet" :: offset :: Nil, "", GetRequest) =>
        () => {
          // access control
          if (!clientUser) {
            Box(RedirectResponse("/"))
          } else {
            for {
              stream <- tryo(ExcelExport.exportTasksheet(false, offset.toInt))
              if null ne stream
            } yield StreamingResponse(stream, () => stream.close,
              stream.available, List("Content-Type" -> "application/vnd.ms-excel", "Content-Disposition" -> ("attachment; filename=\"tasksheet_" + TimeUtils.currentYear(offset.toInt) + "-" + (TimeUtils.currentMonth(offset.toInt) + 1) + "_" + User.currentUser.get.firstName.get.toLowerCase + User.currentUser.get.lastName.get.toLowerCase + ".xls\"")), Nil, 200)
          }
        }
    }
  }
}
