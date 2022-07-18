package io.elfinos.machine

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.SendProducer
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import com.google.protobuf.any.{ Any => ScalaPBAny }
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class PublishEventsProjectionHandler(
    system: ActorSystem[_],
    topic: String,
    sendProducer: SendProducer[String, Array[Byte]]
) extends Handler[EventEnvelope[MachineEvent]] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  private implicit val ec: ExecutionContext = system.executionContext

  override def process(envelope: EventEnvelope[MachineEvent]): Future[Done] = {
    val event = envelope.event

    val key            = event.ncId
    val producerRecord = new ProducerRecord(topic, key, serialize(event))
    val result = sendProducer.send(producerRecord).map { recordMetadata =>
      logger.info("Published event [{}] to topic/partition {}/{}", event, topic, recordMetadata.partition)
      Done
    }
    result
  }

  private def serialize(event: MachineEvent): Array[Byte] = {
    val protoMessage = ???
    // pack in Any so that type information is included for deserialization
    ScalaPBAny.pack(protoMessage, "machine-data-service").toByteArray
  }
}
