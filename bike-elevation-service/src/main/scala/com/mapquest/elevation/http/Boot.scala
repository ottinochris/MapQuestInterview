package com.mapquest.elevation.http

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

import scala.concurrent.Future

//Everything past here is not standard stuff.
import scala.util.{Success, Failure}
import spray.http._
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.event.Logging
import akka.io.IO
import spray.json.{JsonFormat, DefaultJsonProtocol}
import spray.json._
import spray.can.Http
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import spray.util._

//case class Elevation(location: Location, elevation: Double)
//case class Location(lat: Double, lng: Double)
//case class GoogleApiResult[T](status: String, results: List[T])

//case class RouteApiResult(maneuverIndexes:List[Double],shapePoints:List[Double], legIndexes:List[Double])
case class RouteApiResult(route: routeObject)
case class routeObject(shape: shapeObject)
case class shapeObject(maneuverIndexes:List[Double],shapePoints:List[Double], legIndexes:List[Double])

case class ElevationProfileElement(height: Double, distance: Double)
case class ElevationApiResult[T](elevationProfile:List[T], shapePoints:List[Double])


object RouteJsonProtocol extends DefaultJsonProtocol {
  implicit val shapeObjectFormat = jsonFormat3(shapeObject)
  implicit val routeObjectFormat = jsonFormat1(routeObject)
  implicit val RouteApiResultFormat = jsonFormat1(RouteApiResult)
}

object ElevationJsonProtocol extends DefaultJsonProtocol {
  implicit val elevationProfileElementFormat = jsonFormat2(ElevationProfileElement)
  implicit def ElevationApiResultFormat[T : JsonFormat] = jsonFormat2(ElevationApiResult.apply[T])
  // implicit val locationFormat = jsonFormat2(Location)
//  implicit val elevationFormat = jsonFormat2(Elevation)
  //implicit def googleApiResultFormat[T :JsonFormat] = jsonFormat2(GoogleApiResult.apply[T])
}


object Boot extends App {

    //create an actor system from the factory method within ActorSystem singleton.
    implicit val system = ActorSystem("interview")
    //create and start the service actor
    val service = system.actorOf(Props[ServiceActor], "elevation-service")

  import system.dispatcher //execution context for futures
    //Create an http server and tell it where to bind to.
    IO(Http) ! Http.Bind(
      listener = service,
      interface = "0.0.0.0",
      port = sys.env.get("PORT").map(_.toInt).getOrElse(8080)
    )

  //Try to get the points from the optimized route algorithm as it gives will limit to twenty-five points. Also use the biking feature since this
  //is for a biking app.
  val log = Logging(system, getClass)
  var routeQuery : String = "https://open.mapquestapi.com/directions/v2/route?key=Amjtd%7Cluu82q62ng%2C72%3Do5-lr8gu&out" +
    "Format=json&routeType=bicycle&timeType=1&enhancedNarrative=false&shapeFormat=raw&generalize=0&locale=en_US&unit=m&from="

  import RouteJsonProtocol._
  import SprayJsonSupport._

  val routePipeline = sendReceive ~> unmarshal[RouteApiResult]
  val routeResponseFuture = routePipeline{
       Get(routeQuery+"37.779585,-122.496498&to=37.759502,-122.496927")
      }

  var elevationQuery : String = "http://open.mapquestapi.com/elevation/v1/profile?key=Amjtd%7Cluu82q62ng%2C72%3Do5-lr8gu" +
    "&shapeFormat=raw&latLngCollection=" //I can't find a cool way so I am just going to manually build the query uri.

  routeResponseFuture onComplete{
    case Success(RouteApiResult(routeObject(shapeObject(x,y,z)))) =>
      //log.info(y.toString) //evens are lat and odds are lng. map these onto the collection of pts.
      val latlngCollection : String = (y.foldLeft("")((b,a) => b+","+a)).tail //tail is just to remove the leading ","
      elevationQuery = elevationQuery.concat(latlngCollection)
      elevationQuery = elevationQuery.concat(",,") //not really sure if this is doing anything necessary.
      //log.info(query)
      log.info("CALLING THE ELEVATION API NOW")
      queryElevation(elevationQuery)
     // shutdown()
    case Success(somethingUnexpected) =>
      log.info(somethingUnexpected.toString())
      shutdown()

    case Failure(error) =>
      log.error(error, "Couldn't get the route")
      shutdown()
  }

  //I have a feeling that the sessionId is how I am supposed to do this since data is coming from the app ?? look into that.
  def queryElevation(query:String) : Unit = {
    log.info("Trying to make a request with programatic data")
    import ElevationJsonProtocol._
    val pipeline = sendReceive ~> unmarshal[ElevationApiResult[ElevationProfileElement]]
    val responseFuture = pipeline{
//      Get("http://open.mapquestapi.com/elevation/v1/profile?key=Amjtd%7Cluu82q62ng%2C72%3Do5-lr8gu" +
//      "&shapeFormat=raw&latLngCollection=37.779585,-122.496498,37.759502,-122.496927,,")
      Get(query)
    }

    responseFuture onComplete {
      case Success(ElevationApiResult(e, s)) =>
        log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXSUCESSS MOTHAFUCKA")
       // log.info(s"${s.mkString(", ")}")
        //calculate totals for each uphill, downhill, and flat, respectively.
        val totals = e.tail.foldLeft((0.0, 0.0, 0.0, e.head)) {
         //do it this way so b._4 will be the last point.
         (b, a) => a match {
           case cur@ElevationProfileElement(x, y) =>
             //increasing since the current elevation is higher than the last.
             if (x > b._4.height) {
               (b._1 + Math.abs(y - b._4.distance), b._2, b._3, cur)

             } else if (x < b._4.height) {
               //decrease since the current is less than the last

               (b._1, b._2 + Math.abs(y - b._4.distance), b._3, cur)
             } else //else no change.
               (b._1, b._2, b._3 + Math.abs(y - b._4.distance), cur)
         }
       }
        val upTotal = totals._1 / totals._4.distance // take the total uphill distance and divide by total or e.last.
        val downTotal = totals._2 / totals._4.distance
        val flatTotal = totals._3 / totals._4.distance

        log.info("RESULTS")
        log.info(s"${upTotal}, ${downTotal}, ${flatTotal}")
      //disable shudown for now so that we can see if the http server stuff is working.
      //  shutdown()

      case Success(somethingUnexpected) =>
        log.warning("The API call went through but something unexpected happened", somethingUnexpected)
        log.info(somethingUnexpected.toString)
        shutdown()


      case Failure(error) =>
        log.error(error, "Couldn't get Elevation information")
        shutdown()
    }
  }
  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }

}
