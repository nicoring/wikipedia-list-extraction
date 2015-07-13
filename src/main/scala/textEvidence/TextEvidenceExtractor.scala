package textEvidence

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.hp.hpl.jena.query.QuerySolution
import sparql.{JenaTitleAbstractDumpWrapper, JenaDumpWrapper, JenaFragmentsWrapper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.async.Async.{async, await}
import chalk.text.tokenize.SimpleEnglishTokenizer
import chalk.text.analyze.PorterStemmer

import scala.reflect.io.Directory

object TextEvidenceExtractor {

  val testSymbol = 'textEvidence

  val titleWeight : Double = 0.5
  val abstractWeight : Double = 0.5

  val tokenizer = SimpleEnglishTokenizer.V0()

  val camelCaseRegex = "([a-z](?=[A-Z]))".r

  val abstractQueryString =
    """
      select ?abstract from <http://dbpedia.org> where
      { ?uri dbpedia-owl:abstract ?abstract. FILTER (langMatches(lang(?abstract),'en')) }
    """

  val titleQueryString =
    """
      select ?title from <http://dbpedia.org> where
      { ?uri rdfs:label ?title. FILTER (langMatches(lang(?title),'en')) }
    """
}

class TextEvidenceExtractor extends JenaTitleAbstractDumpWrapper {
  import TextEvidenceExtractor._

  val titleTdbDirectory: String = "db/title"
  val abstractTdbDirectory: String = "db/abstracts"

  assert(!Directory(titleTdbDirectory).isEmpty)
  assert(!Directory(abstractTdbDirectory).isEmpty)

  def getAbstract(uri: String) : Future[List[String]] = {
    val abstractText = queryAbstractDumpWithUri(abstractQueryString, uri)
      .map ( _.map (_.getLiteral("abstract").getString))
      .map ( _.headOption.getOrElse("") )
      .map (text => tokenizeAndStemm(text))

    abstractText
  }

  def getTitle(uri: String) : Future[List[String]] = {
    val titleText = queryTitleDumpWithUri(titleQueryString, uri)
      .map(_.map(_.getLiteral("title").getString))
      .map(_.headOption.getOrElse(""))
      .map(text => tokenizeAndStemm(text))

    titleText
  }

    def cleanType(typeUri: String) : String = {
    /* "http://dbpedia.org/class/yago/LivingThing100004258" -> "living thing" */
    var typeLabel = typeUri.substring(typeUri.lastIndexOf('/') + 1)
    typeLabel = "\\d".r.replaceAllIn(typeLabel, "")
    camelCaseRegex.replaceAllIn(typeLabel, "$1 ").toLowerCase
  }

  def normalizeTypes(types: List[String]): List[(String, List[String])] = {
    types map { typeLabel: String =>
      val cleanedLabel = cleanType(typeLabel)

      typeLabel -> tokenizeAndStemm(cleanedLabel)
    }
  }

  def tokenizeAndStemm(text: String) : List[String] = {
    val tokenized = tokenizer(text.toLowerCase)
    tokenized.map(token => PorterStemmer(token)).toList
  }

  def getAbstractsAndTitle(uri: String): Future[(List[String], List[String])] = {
    async {
      val titleText = await(getTitle(uri))
      val abstractText = await(getAbstract(uri))

      (titleText, abstractText)
    }
  }

  def countType(typeLabels: List[String], text: List[String]) : Double = {
    val counts = typeLabels map { typeLabel: String =>
      var count = 0
      for (token <- text) {
        if (typeLabel == token) { count += 1 }
      }

      count
    }

    counts.min
  }

  def rateTypes(types: List[(String, List[String])], abstractsMapFuture: Future[List[(List[String], List[String])]]) : Future[List[(String, Double)]] = {
    val typeCounts = types map { typeTupel =>
      val typeLabel = typeTupel._1
      val normalizedType = typeTupel._2

      var titleCount: Double = 0
      var abstractCount: Double = 0

      abstractsMapFuture.map(abstractsMap => {
        for (abstractTupel <- abstractsMap) {
          val abstractTitle = abstractTupel._1
          val abstractText = abstractTupel._2

          titleCount += countType(normalizedType, abstractTitle)
          abstractCount += countType(normalizedType, abstractText)
        }

        typeLabel -> (titleCount * titleWeight + abstractCount * abstractWeight)
      })
    }

    Future.sequence(typeCounts)
  }

  /* Expects a list of uris for each list entity and a list of all types (generated by tf-idf) */
  def compute(uris: List[String], types: List[String])(implicit materializer: Materializer): Future[Map[String, Double]] = {

    val normalizedTypes = normalizeTypes(types)

    val abstractsMap = Source(uris)
      .mapAsyncUnordered(10)(getAbstractsAndTitle)
      .runFold[List[(List[String], List[String])]](List())((acc, elem) => elem :: acc)

    rateTypes(normalizedTypes, abstractsMap)
      .map(_.toMap)
  }
}
