package gsl

package object protocol {
  object Path {
    val list = "list"
    val item = "item"
  }
  sealed trait Msg

  final case class Item(title: String, notes: Option[String] = None, purchased: Boolean = false) extends Msg
  final case class Shopping(items: List[Item] = List()) extends Msg {
    def add(item: Item): Shopping = Shopping(items :+ item)
    def find(title: String): Option[Item] = items.find(_.title == title)
    def titles: Set[String] = items.map(_.title).toSet
    def has(title: String): Boolean = titles.contains(title)
    def remove(title: String): Shopping = Shopping(items.filterNot(_.title == title))
    def edit(title: String, item: Item): Shopping = Shopping(items.collect {
        case i if i.title == title => item
        case i => i
      })
  }

  object Msg {
    import upickle.default._

    implicit val readWriter: ReadWriter[Msg] =
      macroRW[Item] merge macroRW[Shopping]
  }
}
