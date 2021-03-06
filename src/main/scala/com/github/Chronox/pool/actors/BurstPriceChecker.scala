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

case class updateBurstPriceInfo()
case class BurstPriceInfo(price_usd:String, price_btc:String)

class BurstPriceChecker extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)

  def receive() = {
    case updateBurstPriceInfo() => {
      http.singleRequest(HttpRequest(uri = Config.PRICE_ADDRESS)).pipeTo(self)
    }
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        Global.burstPriceInfo = parse(body.utf8String).extract[BurstPriceInfo]  
      } 
    case resp @ HttpResponse(code, _, _, _) => {
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
    }
  }
}