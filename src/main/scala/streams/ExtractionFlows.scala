package streams

import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import dataFormats._
import dump.{TableArticleParser, ListArticleParser}
import extractors.ListMemberTypeExtractor
import it.cnr.isti.hpc.wikipedia.article.Article
import ratings.{RDFTableWrapper, TfIdfRating, TextEvidenceRating}
import scorer.Scorer
import tableExtraction.TableExtractor
import implicits.ConversionImplicits._
import util.LoggingUtils._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by nico on 14/07/15.
 */
object ExtractionFlows {
  val parallelCount = 8

  def completeFlow()(implicit materializer: Materializer) = Flow[Article]
    .via(convertArticle())
    .via(getTypesMap())
    .via(computeTfIdf())
//    .via(computeTextEvidence())
    .via(fuseResults())

  def tfIdfFlow()(implicit materializer: Materializer) = Flow[Article]
    .via(convertArticle())
    .via(getTypesMap())
    .via(computeTfIdf())

  def buildTableEntities(tablePage: WikiTablePage)(implicit extractor: TableExtractor): List[WikiLink] = {
    val tableMatcher = new RDFTableWrapper(tablePage)
    val rdfTables = tableMatcher.convertTables()
    extractor.extractTableEntities(rdfTables)
//    List()
  }

  def convertArticle()(implicit materializer: Materializer): Flow[Article, WikiListPage, Unit] = {
    implicit val extractor = new TableExtractor

    Flow[Article].mapConcat { article =>
      time("time for converting article:") {
        try {
          println(s"starting list for ${article.getTitleInWikistyle}")
          val parsedListPage = new ListArticleParser(article).parseArticle()

          println(s"starting table for ${article.getTitleInWikistyle}")
          val parsedTablePage = new TableArticleParser(article).parseArticle()

          val finalPage = (parsedListPage, parsedTablePage) match {
            case (Some(listPage), Some(tablePage)) => {
              Some(WikiListPage(
                listPage.listMembers ++ buildTableEntities(tablePage),
                listPage.title,
                listPage.wikiAbstract,
                listPage.categories
              ))
            }
            case (Some(listPage), _) => Some(listPage)
            case (_, Some(tablePage)) => {
              Some(WikiListPage(
                buildTableEntities(tablePage),
                tablePage.title,
                tablePage.wikiAbstract,
                tablePage.categories
              ))
            }
            case _ => None
          }

          finalPage.toList
        } catch {
          case e: Exception => println("parseTables exception: " + e); List()
        }
      }
    }
  }

  def getTypesMap()(implicit materializer: Materializer): Flow[WikiListPage, WikiListResult, Unit] = {
    val extractor = new ListMemberTypeExtractor
    Flow[WikiListPage].mapAsyncUnordered(parallelCount) { page =>
      println(s"starting: ${page.title} count: ${page.listMembers.size}")

      timeFuture("duration for getting types:") {
        extractor.getTypesMap(page.getEntityUris) map { typesMap =>
          if (typesMap.isEmpty) { println(s"${page.title} is empty!") }
          WikiListResult(page, typesMap, Map[Symbol, Map[String, Double]]().empty)
        }
      }
    }
  }

  def computeTfIdf()(implicit materializer: Materializer): Flow[WikiListResult, WikiListResult, Unit] = {
    val rating = new TfIdfRating
    Flow[WikiListResult].mapAsyncUnordered(parallelCount) { result =>
      timeFuture("duration for computing tf-idf:") {
        rating.getRating(result).map { resultMap =>
          WikiListResult(result.page, result.types, Map(TfIdfRating.name -> resultMap))
        }
      }
    }
  }

  def computeTextEvidence()(implicit materializer: Materializer): Flow[WikiListResult, WikiListResult, Unit] = {
    val rating = new TextEvidenceRating
    Flow[WikiListResult].mapAsyncUnordered(parallelCount) { result =>
      val entities = result.page.getEntityUris
      val types = result.getTypes
      timeFuture("duration for computing text evidence:") {
        rating.getRating(result).map { resultList =>
          val newScores = result.scores + (TextEvidenceRating.name -> resultList)
          WikiListResult(result.page, result.types, newScores)
        }
      }
    }
  }

  def fuseResults(): Flow[WikiListResult, WikiFusedResult, Unit] = {
    Flow[WikiListResult].map { result =>
      time("duration for computing fused results:") {
        WikiFusedResult(result.page, Scorer.fuseResult(result))
      }
    }
  }
}
