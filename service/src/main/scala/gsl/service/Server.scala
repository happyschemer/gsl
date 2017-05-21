package gsl
package service

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import gsl.service.ShoppingActor.Message

object Server {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("gsl-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(5 seconds)

    import protocol._
    import Msg._
    import upickle.default._
    import protocol.Path._
    import ShoppingActor.Message._

    val gsl = system.actorOf(ShoppingActor.props, "gsl")

    def reply(reply: Any) = {
      complete(HttpEntity(`application/json`, write(reply.asInstanceOf[Msg])))
    }

    def askReply(m: Message) = onSuccess(gsl ? m) {
      case None => complete(NotFound)
      case Some(msg) => reply(msg)
      case msg => reply(msg)
    }

    val route =
      pathSingleSlash {
        getFromResource("index.html")
      } ~
      path(list) { get { askReply(GetList) }} ~
      path(list / item) { post { entity(as[String]) { ent =>
        askReply(AddItem(read[Item](ent)))
      }}} ~
      path(list / item / Segment) { title =>
        get {
          askReply(GetItem(title))
        } ~
        delete {
          askReply(RemoveItem(title))
        } ~
        put { entity(as[String]) { ent =>
          askReply(EditItem(title, read[Item](ent)))
        }}
      } ~
      path(Remaining) {
        getFromResource
      }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", ConfigFactory.load().getInt("gsl.port"))

    while (true) Thread.sleep(60000)

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ ⇒ system.terminate())
  }
}
