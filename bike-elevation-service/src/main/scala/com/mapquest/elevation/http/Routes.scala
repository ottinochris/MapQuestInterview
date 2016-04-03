package com.mapquest.elevation.http

import spray.routing.HttpService

//case class Point(x: Double, y: Double)

trait Routes extends HttpService {
  //val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
 // val response: Future[HttpResponse] = pipeline(Get("http://spray.io/"))

  val routes =

    pathPrefix("v1") {
      path("elevation") {
        get {
          complete("Fuck Off!")
        }
        //maybe pull A and B points out of post data here.
      }~
      post{
        complete("hello chris")
      }
    }


}
