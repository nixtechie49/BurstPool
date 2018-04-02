package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._

case class StateTick()
case class PayoutUsers()

class StateUpdater extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  override def preStart() {
    context.system.scheduler.schedule(0 seconds, 3 seconds, self, StateTick())
    context.system.scheduler.schedule(
      0 seconds, Config.PAY_TIME hours, self, PayoutUsers())
  }

  def receive() = {
    case StateTick() => {
      Global.burstPriceChecker ! updateBurstPriceInfo()
      Global.miningInfoUpdater ! getNewBlock()
      Global.userManager ! refreshUsers()
    }
    case PayoutUsers() => Global.rewardPayout ! PayoutRewards()
  }
}