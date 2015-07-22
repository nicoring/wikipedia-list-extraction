package runnables

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import dataFormats.{WikiListResult, WikiFusedResult}
import dump.RecordReaderWrapper
import it.cnr.isti.hpc.wikipedia.article.Article

import implicits.ConversionImplicits._
import streams.{RdfWriter, ExtractionFlows, JsonWriter}
import util.LoggingUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps


/**
 * Created by nico on 05/07/15.
 */
object FlowSpike {
  def main(args: Array[String]) {


//    val filename = "data/json/scientists.json"
    val filename = "data/json/random1000.json"
//    val filename = "/Users/nico/Studium/KnowMin/datasets/data/json/karateka-list.json"

    implicit val actorSys = ActorSystem("wikilist-extraction")
    implicit val materializer = ActorMaterializer()

    val rdfWriter = new RdfWriter()
    val reader = new RecordReaderWrapper(filename)
    val articles: Iterator[Article] = reader.iterator

//    val printSink = Sink.foreach[WikiListResult](result => println(s"finished: ${result.page.title} count:${result.scores}"))

//    val mapSink = Sink.fold[Map[String, List[String]], WikiListResult](Map[String, List[String]]()) { (resultTypes, result) =>
//      resultTypes + (result.page.title -> result.types.keys.toList)
//    }

//    val tfIdfSink = Sink.fold[List[WikiListResult], WikiListResult](List()) { (list, result) =>
//      result :: list
//    }

    // val typeSink = Sink.fold[List[WikiFusedResult], WikiFusedResult](List()) { (list, result) => result :: list }
    // val printSink = Sink.foreach[WikiFusedResult](result => println(s"finished: ${result.page.title} count:${result.types}"))


//    val typeSinkTfIdf = Sink.fold[List[WikiListResult], WikiListResult](List()) { (list, result) => result :: list }
//
//    val gtf = Source(() => articles)
//      .via(ExtractionFlows.tfIdfFlow())
//      .runWith(typeSinkTfIdf)
//
//
//    timeFuture("completeDuration")(gtf)
//
//    gtf foreach { res =>
//      val json = JsonWriter.createTfIdfJson(res)
//      JsonWriter.write(json, "data/results/scientists-tfidf.json")
//      materializer.shutdown()
//      actorSys.shutdown()
//    }

    val typeSink = Sink.fold[List[WikiFusedResult], WikiFusedResult](List()) { (list, result) => result :: list }

    val g = Source(() => articles)
      .via(ExtractionFlows.completeFlow())
      .runWith(typeSink)

    g foreach { res =>
      res foreach { fusedResults => rdfWriter.addTypeStatementsFor(fusedResults, "data/ttl/scientists.ttl") }
      val json = JsonWriter.createResultJson(res)
      JsonWriter.write(json, "data/results/random1000-5.json")
      materializer.shutdown()
      actorSys.shutdown()
    }

  }
}
