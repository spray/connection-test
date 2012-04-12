import cc.spray.can.client.HttpClient
import java.net.InetSocketAddress
import akka.util.duration._
import cc.spray.can.model.{HttpResponse, HttpRequest}
import akka.actor._
import cc.spray.io.IoWorker.SendCompleted
import java.util.concurrent.atomic.{AtomicInteger, AtomicBoolean}
import cc.spray.io.{CleanClose, Handle, IoWorker}
import util.Random

object SprayCanClient extends App {
  implicit val system = ActorSystem()
  def log = system.log

  val ioWorker = new IoWorker(system).start()

  system.registerOnTermination {
    log.info("Shutting down...")
    ioWorker.stop()
  }

  val httpClient = system.actorOf(
    props = Props(new HttpClient(ioWorker)),
    name = "http-client"
  )

  args match {
    case Array(maxConn) =>
      system.actorOf {
        Props {
          new ConnActor(
            httpClient,
            maxConn.toInt,
            new AtomicInteger,
            new AtomicBoolean
          )
        }
      }
    case _ =>
      log.error("Usage: <executable> maxConn")
      system.shutdown()
  }
}

class ConnActor(httpClient: ActorRef, maxConn: Int, connCount: AtomicInteger,
                closing: AtomicBoolean) extends Actor with ActorLogging {
  import ConnActor._
  var handle: Handle = _
  var nonce = ""
  var reqCounter = 0
  val connNr = connCount.incrementAndGet()
  val doLog = connNr % 100 == 0

  if (doLog) log.info("Opening connection {}", connNr)
  httpClient ! HttpClient.Connect(ServerAddress)

  def handleError: Receive = {
    case Status.Failure(error) =>
      if (doLog) log.error("Connection {}: received error {}", connNr, error)
      context.system.shutdown()
    case HttpClient.Closed(_, reason) =>
      if (doLog) log.error("Connection {}: closed due to {}", connNr, reason)
      context.system.shutdown()
    case unhandled =>
      log.error("Connection {}: unhandled message {}", connNr, unhandled)
      context.system.shutdown()
  }

  def unconnected = ({
    case HttpClient.Connected(h) =>
      log.debug("Connection {} established", connNr)
      handle = h
      context.become(connected)
      self ! Ping
  }: Receive) orElse handleError

  val connected: Receive = ({
    case Ping =>
      log.debug("Connection {}: sending PING", connNr)
      nonce = Random.alphanumeric.take(8).mkString("/", "", "")
      reqCounter += 1
      handle.handler ! HttpRequest(uri = nonce)
      context.become(responsePending)
  }: Receive) orElse handleError

  val responsePending: Receive = ({
    case _: SendCompleted => // ignore
    case HttpResponse(200, _, body, _) =>
      log.debug("Connection {}: PONG", connNr)
      onResponse(new String(body, "ASCII"))
  }: Receive) orElse handleError

  def receive = unconnected

  def onResponse(response: String) {
    if (response == nonce) {
      context.become(connected)
      if (connNr == maxConn) {
        reqCounter match {
          case 1 =>
            log.info("Successfully reached {} concurrent connections", maxConn)
            schedulePing()
          case x if x < HoldFor =>
            log.info("Holding for another {} PINGs", HoldFor - x)
            schedulePing()
          case _ =>
            closing.set(true)
            shutdown()
        }
      } else if (closing.get) {
        shutdown()
      } else {
        schedulePing()
        if (reqCounter == 1)
          context.system.actorOf(Props(new ConnActor(httpClient, maxConn, connCount, closing)))
      }
    } else {
      log.error("Received incorrect response '{}', should have been '{}'", response, nonce)
      context.system.shutdown()
    }
  }

  def schedulePing() { context.system.scheduler.scheduleOnce(Delay, self, Ping) }

  def shutdown() {
    if (doLog) log.info("Shutting down connection {}", connNr)
    handle.handler ! HttpClient.Close(CleanClose)
    context.stop(self)
    if (connCount.decrementAndGet() == 0) context.system.shutdown()
  }
}

object ConnActor {
  val ServerAddress = new InetSocketAddress("localhost", 8765)
  val Delay = 4900.millis // 5 seconds
  val Ping = new {}
  val HoldFor = 10 // requests
}