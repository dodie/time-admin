package feature

import cucumber.api.junit.Cucumber
import cucumber.api.CucumberOptions
import org.junit.runner.RunWith
import org.junit.BeforeClass
import org.junit.AfterClass

import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.server.{ Handler, Server }
import org.eclipse.jetty.webapp.WebAppContext
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.WebDriver
import java.util.concurrent.TimeUnit

@RunWith(classOf[Cucumber])
@CucumberOptions(
  features = Array("src/e2e/resources/features"),
  glue = Array("feature.step"),
  format = Array("pretty", "html:target/cucumber-report")
)
class FeatureTest {
}

object FeatureTest {

  def driver = webDriver
  val port = 8081
  val baseUrl = "http://localhost:" + port.toString

  private var webDriver: WebDriver = null
  private val server: Server = new Server

  @BeforeClass
  def startup() {
    FirefoxOptions options = new FirefoxOptions();
    options.setHeadless(true);
    webDriver = new FirefoxDriver(options)

    val scc = new SelectChannelConnector
    scc.setPort(port)
    server.setConnectors(Array(scc))

    val context = new WebAppContext()
    context.setServer(server)
    context.setWar("src/main/webapp")

    val context0: ContextHandler = new ContextHandler();
    context0.setHandler(context)
    server.setHandler(context0)

    server.start()

    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
  }

  @AfterClass
  def shutdown() {
    driver.close()
    server.stop()
    server.join()
  }

}
