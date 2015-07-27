package code.test.utils

import code.model.{UserRoles, Role, User}
import net.liftweb.mapper.By

object DefaultUser {
  def login(): Unit = {
    User.logUserIn(User.find(By(User.email, "default@tar.hu")).getOrElse {
      val adminRole = Role.find(By(Role.name, "admin")).getOrElse(Role.create.name("admin").saveMe)
      val clientRole = Role.find(By(Role.name, "client")).getOrElse(Role.create.name("client").saveMe)

      val defaultUser = User.create
        .firstName("DEFAULT")
        .lastName("DEFAULT")
        .email("default@tar.hu")
        .password("abc123")
        .validated(true)
        .superUser(true)
        .saveMe()
      UserRoles.create.user(defaultUser).role(adminRole).save
      UserRoles.create.user(defaultUser).role(clientRole).save
      defaultUser
    })
  }
}
