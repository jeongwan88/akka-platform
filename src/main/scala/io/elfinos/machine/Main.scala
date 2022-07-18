package io.elfinos.machine

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import com.softwaremill.macwire._
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

object Main {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "elfin-ap-data-service")
    try {
      init(system)
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization failure.", e)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_]): Unit = {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

    wire[MachineState].init(system)
    PublishEventsProjection.init(system)

    val config = system.settings.config

    val grpcInterface                     = config.getString("elfin-ap-data-service.grpc.interface")
    val grpcPort                          = config.getInt("elfin-ap-data-service.grpc.port")
    val grpcService: proto.MachineService = wire[MachineServiceImpl]

    MachineServer.start(grpcInterface, grpcPort, system, grpcService)
  }

}
