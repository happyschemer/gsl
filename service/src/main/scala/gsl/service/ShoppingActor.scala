package gsl.service

import akka.actor.Props
import akka.persistence.PersistentActor
import gsl.protocol._

class ShoppingActor extends PersistentActor {
  private var list: Shopping = Shopping(List())

  import context._

  override val persistenceId = "singleton-list"

  import ShoppingActor.Message
  import Message._

  val handleEvents: Message => Unit = {
    case AddItem(item)          => list = list.add(item); reply(list)
    case RemoveItem(title)      => list = list.remove(title); reply(list)
    case EditItem(title, item)  => list = list.edit(title, item); reply(list)
  }

  def reply(m: Msg): Unit = {
    val DeadLetters = system.deadLetters
    sender() match {
      case DeadLetters =>
      case s => s ! m
    }
  }

  override def receiveRecover = {
    case e: Message => handleEvents(e)
  }

  override def receiveCommand = {
    case GetList => sender() ! list
    case GetItem(title) => sender() ! list.find(title)
    case e: Message => persist(e)(handleEvents)
  }
}

object ShoppingActor {
  def props = Props(new ShoppingActor())

  trait Message
  object Message {
    object GetList extends Message
    case class AddItem(item: Item) extends Message
    case class GetItem(title: String) extends Message
    case class RemoveItem(title: String) extends Message
    case class EditItem(title: String, item: Item) extends Message
  }
}

