## Connection Test

This is a small project for testing the spray-can HttpServer and HttpClient implementations in a C50K scenario on
localhost.

The project consists of three parts:

1. A spray-can HttpServer with a very simple echo handler acting as test-fixture.

2. A load-generation application using a spray-can HttpClient that opens 50'000 connections against the server
   and runs simple ping/pong messages across these for some time before shutting down again.

3. A very simple [Gatling][1] simulation that fires 50'000 long-running requests to the server, which only complete
   after all connections have been established.


The goal of this project is to verify the feasibily of opening 50K outgoing client connections from one machine using
the spray-can HttpClient and to (very coarsely) compare against doing the same with Gatling.


### Running on your machine

In order to run the tests on your machine follow these steps:

1. "Loosen the belts" of your OS by increasing the limits on open file count (per process) as well as port availability.
   Check the respective section further down for details.

2. Clone this repository.

3. Make sure you have SBT installed and your launcher script gives SBT at least 1.5 GB of memory.

4. Start the server with `sbt "run-main Server"`

5. In another terminal run `sbt "run-main SprayCanClient"`

6. Lean back and watch the log.
   If everything is well the test will run through an the client will exit normally while the server keeps running.


Instead of generating the load with the `SprayCanClient` you can also use the included Gatling simulation.
Here is how to do it:

1. Follow steps 1 - 4 from above.

2. Download and unpack the gatling distribution ZIP.

3. Move the `GatlingSimulation.scala` file into the `user-files/simulations` directory underneath your gatling
   installation directory.

4. Increase the `gatling.http.connectionTimeout` and `gatling.http.requestTimeout` setting in the `conf/gatling.conf`
   config file underneath your gatling installation directory to `90000`.

5. Run the `bin/gatling.sh` file underneath your gatling installation directory.

6. Watch the gatling log output as well as the log on the server side.


### Differences between the SprayCanClient and Gatling tests

Due to limitations in Gatling the two tests are not directly comparable.
The SprayCanClient opens connections and regularly (every 5 seconds) fires ping messages across them, expecting the
server to respond with a respective response. With 50K connections open this means that ca. 10`000 requests are
fired and responded to every second.

Since Gatling has no way of directly using persistent connections for more than one request (connection pooling helps
but still offers too little control over connection management) the approach for this test is different:
Gatling fires 50K requests in 50 seconds that are responded to by the server 60 seconds after the request has come in.
This means that, after 50 s, 50K connections will be open with their requests pending. 10 seconds after the last
connection has been opened the first response goes out, with the other ones following behind thereby closing all
connections over time.


### Configuring your OS

You can easily run the tests on your machine, but you need to make sure that you increase certain limits with regard
to open file handles as well as port range in your OS.

Under Mac OS/X Lion you need to run the following commands in order to "unbuckle the belts":

    sudo sysctl -w kern.maxfilesperproc=200000
    sudo sysctl -w kern.maxfiles=200000
    sudo sysctl -w net.inet.ip.portrange.first=1024

Under Windows or Linux there will be similar tweaks required.