package gsl.web

import scalajs.js._
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.scalajs.dom._
import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery => $}
import org.scalajs.dom.ext.Ajax
import upickle.default._
import gsl.protocol._
import Path.{item, _}

import scala.scalajs.js

object GslApp extends JSApp {

  val host = window.location.host

  var shopping = Shopping()
  var editing: Option[Item] = None

  def main: Unit = { $ { () =>

    Ajax.get(s"http://$host/$list").
      map(_.as[Shopping]).foreach { s =>
        shopping = s
        bindList
      }

    $("#add").klick {
      editing = None
      bindShowOne
    }

    $("#cancel").klick {
      showList
    }

    $("#title").kee {
      validate
    }

    $("#save").klick {
      val it = Item($("#title").string, Option($("#notes").string).filterNot(_.isEmpty))
      editing match {
        case Some(_) => updateOne(it)
        case None => createOne(it)
      }
    }

    $("#remove").klick {
      if (window.confirm("Are you sure?")) {
        removeOne
      }
    }
  }}

  def bindShowOne: Unit = {
    $("#title").value(editing.map(_.title).orNull)
    $("#notes").value(editing.flatMap(_.notes).orNull)

    $("#list").hide()
    $("#one").show()

    editing match {
      case Some(_) => $("#remove").show()
      case None => $("#remove").hide()
    }
    validate
  }

  def bindList: Unit = {
    shopping.items.foreach { i =>
      $("#items").append(
        s"""
           |<li>
           |<span id="${i.title}" class="item">${i.title}</span>
           |<span id="${i.title}" class="purchase">${if (i.purchased) "[X]" else "[ ]"}</span>
           |</li>
           |""".
          stripMargin
      )
    }

    $(".item").each { e: Element =>
      $(e).klick {
        editing = shopping.find(e.id)
        bindShowOne
      }
    }

    $(".purchase").each { e: Element =>
      $(e).klick {
        editing = shopping.find(e.id)
        updateOne(editing.get.togglePurchased)
      }
    }
  }

  def showList: Unit = {
    $("#one").hide()
    $("#list").show()
  }

  def validate: Unit = {
    val title = $("#title").string
    val missing = title.isEmpty
    val duplicated = editing match {
      case Some(item) => (shopping.titles - item.title).contains(title)
      case None       => shopping.has(title)
    }
    $("#save").prop("disabled", missing || duplicated)
  }

  def createOne(toCreate: => Item): Unit = {
    Ajax.post(s"http://$host/$list/$item", write(toCreate)).
      map(_.as[Shopping]).foreach { s =>
        shopping = s
        rebindList
        showList
    }
  }

  def updateOne(toUpdate: => Item): Unit = {
    editing.foreach { before =>
      Ajax.put(s"http://$host/$list/$item/${before.title}", write(toUpdate)).
        map(_.as[Shopping]).foreach { s =>
          shopping = s
          editing = None
          rebindList
          showList
      }}
  }

  def removeOne: Unit = {
    editing.foreach { existing =>
      Ajax.delete(s"http://$host/$list/$item/${existing.title}").
        map(_.as[Shopping]).foreach { s =>
          shopping = s
          editing = None
          rebindList
          showList
      }}
  }

  def rebindList: Unit = {
    $("#items").empty()
    bindList
  }

  implicit class JQueryExt(jq: JQuery) {
    def string() = jq.value().toString.trim
    def klick(any: => js.Any): JQuery = jq.click { _: JQueryEventObject => any }
    def kee(any: => js.Any): JQuery = jq.keyup { _: JQueryEventObject => any }
  }

  implicit class XMLHttpRequestExt(aj: XMLHttpRequest) {
    def as[M <: Msg : Reader]: M = read[M](aj.responseText)
  }
}
