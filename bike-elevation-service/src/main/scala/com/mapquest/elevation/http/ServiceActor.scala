package com.mapquest.elevation.http

import akka.actor.Actor
import scala.concurrent.ExecutionContext


class ServiceActor extends Actor with Routes {

  def ec: ExecutionContext = context.dispatcher
  def actorRefFactory = context
  def receive = runRoute(routes)
}
