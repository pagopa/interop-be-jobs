package it.pagopa.interop.tokenreader

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.util.Timeout
import akka.{actor => classic}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

package object system {

  implicit val actorSystem: ActorSystem[Nothing] =
    ActorSystem[Nothing](Behaviors.empty, "interop-be-token-reader")

  implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

  implicit val classicActorSystem: classic.ActorSystem = actorSystem.toClassic

  implicit val timeout: Timeout = 3.seconds

}
