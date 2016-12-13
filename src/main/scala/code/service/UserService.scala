package code.service

import code.model.{Role, User, UserRoles}
import net.liftweb.mapper.By

object UserService {
  def nonAdmin(u: User): Boolean = {
    val admin = Role.find(By(Role.name, "admin"))
    val client = Role.find(By(Role.name, "client"))
    UserRoles.findAll(By(UserRoles.role, admin), By(UserRoles.user, u)).isEmpty &&
      UserRoles.findAll(By(UserRoles.role, client), By(UserRoles.user, u)).nonEmpty
  }
}
