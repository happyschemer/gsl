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
    $("#item").hide()

    Ajax.get(s"http://$host/$list").
      map(_.as[Shopping]).
      foreach { s =>
        shopping = s
        bindList
      }

    $("#add").klick {
      editing = None
      bindSingle
      $("#remove").hide()
      showSingle
    }

    $("#cancel").klick {
      showList
    }

    $("#title").kee {
      validate
    }

    $("#save").klick {
      val saved = editing match {
        case Some(existing) => Ajax.put(s"http://$host/$list/$item/${existing.title}", write(toItem))
        case None => Ajax.post(s"http://$host/$list/$item", write(toItem))
      }

      saved.map(_.as[Shopping]).foreach { s =>
        shopping = s
        rebindList
        showList
    }}

    $("#remove").klick {
      if (window.confirm("Are you sure?")) {
        editing match {
          case Some(existing) =>
            Ajax.delete(s"http://$host/$list/$item/${existing.title}").
              map(_.as[Shopping]).foreach { s =>
              shopping = s
              rebindList
              showList
            }
        }
      }
    }
  }}

  def validate: Unit = {
    val title = $("#title").string
    val missing = title.isEmpty
    val duplicated = editing match {
      case Some(item) => (shopping.titles - item.title).contains(title)
      case None       => shopping.has(title)
    }
    $("#save").prop("disabled", missing || duplicated)
  }

  def showSingle: Unit = {
    $("#list").hide()
    $("#item").show()
  }

  def showList: Unit = {
    $("#item").hide()
    $("#list").show()
  }

  def bindSingle: Unit = {
    $("#title").value(editing.map(_.title).orNull)
    $("#notes").value(editing.flatMap(_.notes).orNull)
  }

  def bindList: Unit = shopping.items.foreach { i =>
    $("#items").append(
      s"""
         |<li>
         |<span id="${i.title}" class="item">${i.title}</span>
         |<span id="${i.title}" class="purchase">${if (i.purchased) "[X]" else "[ ]"}</span>
         |</li>
         |""".
        stripMargin
    )

    $(".item").each { e: Element =>
      $(e).klick {
        editing = shopping.find(e.id)
        bindSingle
        validate
        $("#remove").show()
        showSingle
      }
    }

    $(".purchase").each { e: Element =>
      $(e).klick {
        shopping.find(e.id).foreach { it =>
          Ajax.put(s"http://$host/$list/$item/${it.title}", write(it.togglePurchased)).
            map(_.as[Shopping]).foreach { s =>
              shopping = s
              rebindList
              showList
        }}
      }
    }
  }

  def rebindList = {
    $("#items").empty()
    bindList
  }

  def toItem = Item($("#title").string, Option($("#notes").string).filterNot(_.isEmpty))

  implicit class JQueryExt(jq: JQuery) {
    def string() = jq.value().toString.trim
    def klick(any: => js.Any): JQuery = jq.click { _: JQueryEventObject => any }
    def kee(any: => js.Any): JQuery = jq.keyup { _: JQueryEventObject => any }
  }

  implicit class XMLHttpRequestExt(aj: XMLHttpRequest) {
    def as[M <: Msg : Reader]: M = read[M](aj.responseText)
  }
}
