package repo

import com.cryptoutility.protocol.Events.UserInfo

import scala.concurrent.Future


trait UserRepository {

  def add(user: UserInfo): Future[Unit] = ???

  def getByClientId(id: String): Future[UserInfo] = ???
}
