package dataFormats

trait WikiPage {
  val title: String
  val wikiAbstract: String
  val categories: List[WikiLink]
}

case class WikiListPage(
                         listMembers: List[WikiLink],
                         title: String,
                         wikiAbstract: String,
                         categories: List[WikiLink]) extends WikiPage {
  def getEntityUris: List[String] = listMembers.map(_.toUri)
}


case class WikiTablePage(
                          tables: List[WikiTable],
                          title: String,
                          wikiAbstract: String,
                          categories: List[WikiLink]) extends WikiPage


trait Entry
case class WikiLink(label: String, uri: String) extends Entry {
  def toUri: String = "http://dbpedia.org/resource/" + uri
}
case class Literal(raw: String, dataType: String) extends Entry

case class WikiTable(header: List[String], rows: List[TableRow], name: String)
case class TableRow(cells : List[TableCell])
case class TableCell(entry : Entry)


case class WikiListResult(page: WikiListPage, types: Map[String, Int], scores: Map[Symbol, Map[String, Double]]) {
  def getTypes: List[String] = types.keys.toList
}