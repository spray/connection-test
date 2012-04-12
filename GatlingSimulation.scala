package conntest
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._

class GatlingSimulation extends Simulation {

	def apply = {

		val urlBase = "http://127.0.0.1:8765"

		val httpConf = httpConfig.baseURL(urlBase)

		val scn = scenario("Connection Test")
			.exec(
				http("request")
					.get("/gatling")
					.check(status.is(200)))

		List(scn.configure.users(50000).ramp(50).protocolConfig(httpConf))
	}
}
