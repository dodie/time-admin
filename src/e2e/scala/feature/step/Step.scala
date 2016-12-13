package feature.step

import feature.FeatureTest
import feature.FeatureTest.driver
import feature.FeatureTest.baseUrl
import cucumber.api.scala.{ ScalaDsl, EN }
import org.scalatest.Matchers
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import cucumber.api.PendingException
import collection.JavaConversions._
import org.openqa.selenium.Keys
import feature.support.RandomData.randomString
import feature.support.RandomData.randomEmail
import code.model._

class Step extends ScalaDsl with EN with Matchers {

  var userEmail: String = null
  var password: String = null

  Given("""^I am an Unauthenticated user$""") { () =>
    userEmail = null
    password = null
    driver.manage.deleteAllCookies
  }

  Given("""^I am a New user$""") { () =>
    driver.manage.deleteAllCookies
    userEmail = randomEmail
    password = "abc123"
    createNewUser(userEmail, password)
  }

  Given("""^I am a User$""") { () =>
    driver.manage.deleteAllCookies
    userEmail = randomEmail
    password = "abc123"
    createNewUser(userEmail, password)
  }

  When("""^I log in$""") { () =>
    login(userEmail, password)
  }

  When("""^I go to Timeadmin$""") { () =>
    driver.get(baseUrl)
  }

  Then("""^I see the Login page$""") { () =>
    driver.findElement(By.cssSelector(".loginScreen"));
  }

  Then("""^I can not see the Client pages$""") { () =>
    mainMenu should contain noneOf ("Tasks", "Timesheet", "Tasksheet")
  }

  Then("""^I can see the Client pages$""") { () =>
    mainMenu should contain allOf ("Tasks", "Timesheet", "Tasksheet")
  }

  Then("""^I can not see the Admin pages$""") { () =>
    mainMenu should contain noneOf ("Projects", "Users")
  }

  Then("""^I can see the Admin pages$""") { () =>
    mainMenu should contain allOf ("Projects", "Users")
  }

  Then("""^I can not see the User pages$""") { () =>
    userMenu should contain noneOf ("Settings", "Change password", "My profile", "Logout")
  }

  Then("""^I can see the User pages$""") { () =>
    userMenu should contain allOf ("Settings", "Change password", "My profile", "Logout")
  }

  Then("""^I can see the Registration page$""") { () =>
    userMenu should contain("Registration")
  }

  Then("""^I see a nice welcome message$""") { () =>
    driver.findElement(By.cssSelector(".freshUserWarning"));
  }

  When("""^I register an account$""") { () =>
    createNewUser(randomEmail, "abc123")
  }

  When("""^an administrator grant me Client permission$""") { () =>
    login("default@tar.hu", "abc123")
    navigateInMainMenu("Users")
    switchClientRole(userEmail)
    submit
  }

  When("""^an administrator grant me Admin permission$""") { () =>
    login("default@tar.hu", "abc123")
    navigateInMainMenu("Users")
    switchAdminRole(userEmail)
    submit
  }

  def mainMenu =
    elementsAsText("#navbar .navbar-nav > li *")

  def userMenu = {
    openUserMenu
    elementsAsText("#navbar .userPageLink")
  }
  def elementsAsText(cssSelector: String) =
    for (menuItem <- driver.findElements(By.cssSelector(cssSelector))) yield menuItem.getAttribute("innerHTML")

  def openUserMenu =
    driver.findElement(By.cssSelector("#navbar > ul.nav.navbar-nav.navbar-right .dropdown .dropdown-toggle")).click

  def createNewUser(email: String, password: String) = {
    driver.get(baseUrl)
    navigateInUserMenu("Registration")
    driver.findElement(By.id("txtFirstName")).sendKeys(randomString)
    driver.findElement(By.id("txtLastName")).sendKeys(randomString)
    driver.findElement(By.id("txtEmail")).sendKeys(email)
    driver.findElement(By.id("txtLocale")).sendKeys("english")

    for (passwordField <- driver.findElements(By.xpath("//input[@type='password']"))) {
      passwordField.clear
      passwordField.sendKeys(password)
    }

    submit
  }

  def login(email: String, password: String) = {
    driver.manage.deleteAllCookies
    driver.get(baseUrl)
    driver.findElement(By.cssSelector("input[name='username']")).sendKeys(email)
    driver.findElement(By.cssSelector("input[type='password']")).sendKeys(password)
    submit
  }

  def navigateInMainMenu(pageName: String) =
    driver.findElement(By.cssSelector("#navbar .navbar-nav")).findElement(By.xpath(".//a[contains(text(), '" + pageName + "')]")).click

  def navigateInUserMenu(pageName: String) = {
    openUserMenu
    driver.findElement(By.cssSelector("#navbar .navbar-right .dropdown-menu")).findElement(By.xpath(".//a[contains(text(), '" + pageName + "')]")).click
  }

  def switchAdminRole(email: String) =
    driver.findElement(By.xpath("//a[contains(text(), '" + email + "')]/../..//td[position()=2]/input")).click

  def switchClientRole(email: String) =
    driver.findElement(By.xpath("//a[contains(text(), '" + email + "')]/../..//td[position()=3]/input")).click

  def submit =
    driver.findElement(By.xpath("//input[@type='submit']")).click

  def setText(webElement: WebElement, text: String) = {
    webElement.clear
    webElement.sendKeys(text)
  }

  var newFirstName: String = null
  var newLastName: String = null
  When("""^I change my first and last name$""") { () =>
    navigateInUserMenu("My profile")
    newFirstName = randomString
    newLastName = randomString
    setText(driver.findElement(By.id("txtFirstName")), newFirstName)
    setText(driver.findElement(By.id("txtLastName")), newLastName)
    submit
  }

  Then("""^my first and last name should be updated$""") { () =>
    val displayedName = driver.findElement(By.cssSelector(".ActualUserName")).getAttribute("innerHTML")
    displayedName should include(newFirstName + " " + newLastName)
  }

  When("""^I change the localization to Hungarian$""") { () =>
    navigateInUserMenu("My profile")
    driver.findElement(By.id("txtLocale")).sendKeys("magyar")
    submit
  }

  Then("""^the text on the user interface should appear in that language$""") { () =>
    userMenu should contain("Kilépés")
    userMenu should not contain ("Logout")
  }

  When("""^I change my e-mail address$""") { () =>
    userEmail = randomEmail
    navigateInUserMenu("My profile")
    setText(driver.findElement(By.id("txtEmail")), userEmail)
    submit
  }
  Then("""^I can log in with my new e-mail address$""") { () =>
    login(userEmail, password)
    userMenu should contain("Logout")
  }

  When("""^I change my password$""") { () =>
    navigateInUserMenu("Change password")
    val passwordFields = driver.findElements(By.xpath("//input[@type='password']"))
    setText(passwordFields.get(0), password)
    password = randomString
    setText(passwordFields.get(1), password)
    setText(passwordFields.get(2), password)
    submit
  }

  Then("""^I can log in with my new password$""") { () =>
    login(userEmail, password)
    userMenu should contain("Logout")
  }

  lazy val adminRole = Role.find(net.liftweb.mapper.By(Role.name, "admin")).openOrThrowException("No admin role!")
  lazy val clientRole = Role.find(net.liftweb.mapper.By(Role.name, "client")).openOrThrowException("No admin role!")
  var someUsers: List[User] = null
  Given("""^that there are Registered users$""") { () =>
    val someAdmin = randomUser
    UserRoles.create.user(someAdmin).role(adminRole).save

    val someClient = randomUser
    UserRoles.create.user(someClient).role(clientRole).save

    val someAdminClient = randomUser
    UserRoles.create.user(someAdminClient).role(adminRole).save
    UserRoles.create.user(someAdminClient).role(clientRole).save

    val someNew = randomUser
    someUsers = List(someAdmin, someClient, someAdminClient, someNew)
  }

  When("""^I am on the Users page$""") { () =>
    login("default@tar.hu", "abc123")
    navigateInMainMenu("Users")
  }

  Then("""^I see the registered users$""") { () =>
    for (user <- someUsers) {
      driver.findElement(By.xpath("//a[contains(text(), '" + user.email + "')]"))
    }
  }

  var someUser: User = null
  Given("""^there is a Registered user$""") { () =>
    someUser = randomUser
  }

  When("""^I select the User$""") { () =>
    driver.findElement(By.xpath("//a[contains(text(), '" + someUser.email + "')]")).click
  }

  Then("""^I can see the details of that user$""") { () =>
    driver.findElement(By.cssSelector(".usersPage"))
    val firstNameFieldValue = driver.findElement(By.id("txtFirstName")).getAttribute("value")
    firstNameFieldValue should equal(someUser.firstName.get)
  }

  var usersNewFirstName: String = null
  When("""^I modify the User$""") { () =>
    usersNewFirstName = randomString
    setText(driver.findElement(By.id("txtFirstName")), usersNewFirstName)
    submit
  }

  Then("""^the user data should be modified$""") { () =>
    driver.findElement(By.xpath("//a[contains(text(), '" + usersNewFirstName + "')]"))
  }

  var someNewUser: User = null
  Given("""^there is a New user$""") { () =>
    someNewUser = randomUser
  }

  When("""^I add Client role to the User$""") { () =>
    switchClientRole(someNewUser.email.get)
    submit
  }

  Then("""^the user can see the Client pages$""") { () =>
    login(someNewUser.email.get, "abc123")
    mainMenu should contain allOf ("Tasks", "Tasksheet", "Timesheet")
  }

  When("""^I add Admin role to the User$""") { () =>
    switchAdminRole(someNewUser.email.get)
    submit
  }

  Then("""^the user can see the Admin pages$""") { () =>
    login(someNewUser.email.get, "abc123")
    mainMenu should contain allOf ("Projects", "Users")
  }

  Given("""^there is a Client user$""") { () =>
    someNewUser = randomUser
    UserRoles.create.user(someNewUser).role(clientRole).save
  }

  When("""^I revoke the User's Client role$""") { () =>
    switchClientRole(someNewUser.email.get)
    submit
  }

  Then("""^the user can not see the Client pages$""") { () =>
    login(someNewUser.email.get, "abc123")
    mainMenu should contain noneOf ("Tasks", "Tasksheet", "Timesheet")
  }

  Given("""^there is an Admin user$""") { () =>
    someNewUser = randomUser
    UserRoles.create.user(someNewUser).role(adminRole).save
  }

  When("""^I revoke the User's Admin role$""") { () =>
    switchAdminRole(someNewUser.email.get)
    submit
  }

  Then("""^the user can not see the Admin pages$""") { () =>
    login(someNewUser.email.get, "abc123")
    mainMenu should contain noneOf ("Projects", "Users")
  }

  def randomUser = {
    User.create
      .firstName(randomString)
      .lastName(randomString)
      .email(randomEmail)
      .password("abc123")
      .validated(true)
      .superUser(true)
      .saveMe()
  }

}
