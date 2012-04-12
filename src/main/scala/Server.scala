/*
 * Copyright (C) 2011 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import cc.spray.can.model.{HttpResponse, HttpRequest}
import cc.spray.io.pipelines.MessageHandlerDispatch
import cc.spray.io.IoWorker
import cc.spray.can.server.HttpServer
import akka.actor._

object Server extends App {
  // we need an ActorSystem to host our application in
  val system = ActorSystem("server")

  // the handler actor replies to incoming HttpRequests
  val handler = system.actorOf {
    Props {
      ctx => {
        case x: HttpRequest =>
          if (x.uri != "/stop") ctx.sender ! HttpResponse(200, body = x.uri.getBytes("ISO-8859-1"))
          else system.shutdown()
      }
    }
  }

  // every spray-can HttpServer (and HttpClient) needs an IoWorker for low-level network IO
  // (but several servers and/or clients can share one)
  val ioWorker = new IoWorker(system).start()

  // create and start the spray-can HttpServer, telling it that we want requests to be
  // handled by our singleton handler
  val server = system.actorOf(
    props = Props(new HttpServer(ioWorker, MessageHandlerDispatch.SingletonHandler(handler))),
    name = "http-server"
  )

  // a running HttpServer can be bound, unbound and rebound
  // initially to need to tell it where to bind to
  server ! HttpServer.Bind("localhost", 8765)

  // finally we drop the main thread but hook the shutdown of
  // our IoWorker into the shutdown of the applications ActorSystem
  system.registerOnTermination {
    ioWorker.stop()
  }
}