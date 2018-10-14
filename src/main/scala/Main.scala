import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.{Properties, UUID}

import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.{KStream => _, KTable => _, _}
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream.{KStream, _}
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}


object Main extends App with StrictLogging {
  val uuidSerdes: Serde[UUID] = Serdes.fromFn((uuid: UUID) => {
    val bb = ByteBuffer.wrap(new Array[Byte](16))
    bb.putLong(uuid.getMostSignificantBits)
    bb.putLong(uuid.getLeastSignificantBits)
    bb.array()
  }, (bytes: Array[Byte]) => {
    val bb = ByteBuffer.wrap(bytes)
    Some(new UUID(bb.getLong, bb.getLong))
  })

  implicit val consumed: Consumed[UUID, String] = Consumed.`with`(uuidSerdes, Serdes.String)
  implicit val joined: Joined[UUID, String, String] = Joined.`with`(uuidSerdes, Serdes.String, Serdes.String)
  implicit val produced: Produced[UUID, String] = Produced.`with`(uuidSerdes, Serdes.String)
  implicit val serialized: Serialized[UUID, String] = Serialized.`with`(uuidSerdes, Serdes.String)


  /*trait EntityCommand {
    val entityId: UUID
  }
  trait EntityEvent {
    val entityId: UUID
  }

  case class UpdateValue1Command(entityId: UUID, value: String) extends EntityCommand
  case class UpdateValue2Command(entityId: UUID, value: String) extends EntityCommand
  case class Value1Updated(entityId: UUID, value1: String) extends EntityEvent
  case class Value2Updated(entityId: UUID, value2: String) extends EntityEvent
  case class UnknownCommand(entityId: UUID, command: String) extends EntityEvent

  case class Entity(entityId: UUID, value1: String, value2: String) {
    def applyCommand(command: EntityCommand): List[EntityEvent] = {
      case c: UpdateValue1Command => List(Value1Updated(entityId, c.value))
      case c: UpdateValue2Command => List(Value2Updated(entityId, c.value))
      case _ => UnknownCommand(entityId, command.toString)
    }
  }*/

  val props: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "event-application")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092,localhost:9093,localhost:9094")
    p.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE)
    //p.put(KafkaAvroDeseria, "100")
    p
  }


  val builder: StreamsBuilder = new StreamsBuilder
  val commandStream: KStream[UUID, String] = builder.stream[UUID, String]("entity-commands")
    //.peek((id, command) => logger.info(s"$id: $command"))
  val eventStream: KStream[UUID, String] = builder.stream[UUID, String]("entity-events")

  val entityTable: KTable[UUID, String] =
    eventStream
      .groupByKey
      .reduce((currentState, event) =>
        Seq(currentState, event.replace("-ed", "")).mkString(" "))(Materialized.as(Stores.persistentKeyValueStore("Entity")))

  commandStream
    .leftJoin(entityTable)((command, entity) => s"$command-ed")
    .to("entity-events")



  /*val wordCounts: KTable[String, Long] = textLines
    .flatMapValues(textLine => {grouped key
      logger.info(s"Flatmapping: $textLine")
      textLine.toLowerCase.split("\\W+")
    })
    .groupBy((_, word) => {
      //logger.info(s"Groupby: $word")
      word
    })
    .count()(Materialized.as("counts-store"))
  wordCounts.toStream.peek((word, count) => logger.info(s"$word: $count")).to("WordsWithCountsTopic")*/

  /*commandStream.
    .flatMapValues(textLine => {
      //logger.info(s"Flatmapping: $textLine")
      textLine.toLowerCase.split("\\W+")
    }).peek((key, value) => logger.info(s"$key: $value"))*/

  val streams: KafkaStreams = new KafkaStreams(builder.build(), props)
  //logger.info("Running stream...")
  streams.start()

  sys.ShutdownHookThread {
    streams.close(10, TimeUnit.SECONDS)
    streams.cleanUp()
  }
}