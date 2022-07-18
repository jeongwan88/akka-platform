package io.elfinos.machine

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect
import akka.persistence.typed.scaladsl.RetentionCriteria
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.DurationInt

object MachineState {
  val typeKey: EntityTypeKey[MachineCommand] = EntityTypeKey[MachineCommand]("MachineAggregate")

  val tags: Seq[String] = Vector.tabulate(20)(i => s"ncId-$i")

  val zoneId           = "Asia/Seoul"
  val resetHour: Int   = sys.env("RESET_HOUR").toInt
  val resetMinute: Int = sys.env("RESET_MINUTE").toInt

  def getDate: String = {
    val zoneDateTime = ZonedDateTime.now(ZoneId.of(MachineState.zoneId))
    if (
      zoneDateTime.getHour < resetHour ||
      (zoneDateTime.getHour <= resetHour && zoneDateTime.getMinute < resetMinute)
    ) {
      zoneDateTime.minusDays(1).format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))
    } else {
      zoneDateTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))
    }
  }
}

class MachineState(
) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def empty: State = State()

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[MachineCommand]] = {
    val behaviorFactory: EntityContext[MachineCommand] => Behavior[MachineCommand] = { entityContext =>
      val i           = math.abs(entityContext.entityId.hashCode % MachineState.tags.size)
      val selectedTag = MachineState.tags(i)
      apply(entityContext.entityId, selectedTag)
    }

    ClusterSharding(system).init(Entity(MachineState.typeKey)(behaviorFactory))
  }

  def apply(ncId: String, projectionTag: String): Behavior[MachineCommand] = {
    Behaviors
      .supervise(
        Behaviors.withTimers[MachineCommand] { timers =>
          Behaviors.setup[MachineCommand] { _ =>
            EventSourcedBehavior
              .withEnforcedReplies[MachineCommand, MachineEvent, State](
                persistenceId = PersistenceId(MachineState.typeKey.name, ncId),
                emptyState = empty,
                commandHandler = (state, command) => handleCommand(state, command),
                eventHandler = (state, event) => handleEvent(state, event)
              )
              .withTagger(_ => Set(projectionTag))
              .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 1000))
              .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))

          }
        }
      )
      .onFailure[Exception](SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
  }

  def handleCommand(
      state: State,
      cmd: MachineCommand
  ): ReplyEffect[MachineEvent, State] = {
    ???
  }

  def handleEvent(state: State, evt: MachineEvent): State = ???

}
