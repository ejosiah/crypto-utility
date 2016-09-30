package services

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import client.Client
import client.Client.UserIsOffline
import com.google.inject.{Inject, Singleton}
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.pattern.ask


@Singleton
class FindClientById @Inject() (implicit system: ActorSystem, ec: ExecutionContext) {

  implicit val timeout = Timeout(5 seconds)

  lazy val registry = system.actorSelection("/user/client-registry").resolveOne()

  def apply(clientId: String): Future[ActorRef] = {
    registry.flatMap(reg =>  reg ? Client.Get(clientId)).map{
      case ref: ActorRef => ref
      case UserIsOffline => throw new RuntimeException(s"client with id $clientId is currently offline")
    }
  }
}