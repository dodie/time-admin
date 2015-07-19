import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.webapp.WebAppContext

import scala.util.Try

object JettyLauncher extends App {

  val server = new Server
  val scc = new SelectChannelConnector
  scc.setPort(Option(System.getenv("PORT")).flatMap(s => Try(s.toInt).toOption).getOrElse(8080))
  server.setConnectors(Array(scc))

  val context = new WebAppContext()
  context.setServer(server)
  context.setWar("src/main/webapp")

  val contextHandler = new ContextHandler()
  contextHandler.setHandler(context)
  server.setHandler(contextHandler)

  server.start()
  server.join()
}
