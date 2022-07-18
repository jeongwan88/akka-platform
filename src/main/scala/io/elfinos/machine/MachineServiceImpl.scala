package io.elfinos.machine

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import io.elfinos.machine.proto._
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class MachineServiceImpl(
    system: ActorSystem[_],
) extends proto.MachineService {

  implicit val timeout: Timeout = Timeout(5.seconds)
  val logger: Logger            = LoggerFactory.getLogger(getClass)

  override def addRawData(in: RawData): Future[ResBody] = ???

  override def getCurrentMachineInfo(in: ReqMachineDataBody): Future[ResMachineDataBody] = ???
}
