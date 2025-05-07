package simulations

import io.gatling.core.Predef._
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.http.Predef._
import io.gatling.core.scenario.Simulation
import scenarios._

import scala.concurrent.duration._

class CacheWarm_Simulation extends Simulation {

  val cacheUsers = 60
  val cacheDurationMins = 1

  val httpProtocol = http

  val environment = scala.util.Properties.envOrElse("ENVIRONMENT", "perftest")

  /* ******************************** */
  /* ADDITIONAL COMMAND LINE ARGUMENT OPTIONS */
  val debugMode = System.getProperty("debug", "off") //runs a single user e.g. ./gradle gatlingRun -Ddebug=on (default: off)
  val env = System.getProperty("env", environment) //manually override the environment aat|perftest e.g. ./gradle gatlingRun -Denv=aat
  /* ******************************** */

  val CacheWarm = scenario( "CcdCacheWarm")
    .exec(_.set("env", s"${env}"))
    .exitBlockOnFail {
      exec(CcdCacheWarm.loadJurisdictionsToWarmCache)
    }

  //defines the Gatling simulation model, based on the inputs
  def simulationProfile(numberOfUsers: Int, testDurationMins: Int): Seq[OpenInjectionStep] = {
    if (debugMode == "off") {
      Seq(
        rampUsers(numberOfUsers).during(testDurationMins.minutes)
      )
    }
    else{
      Seq(atOnceUsers(1))
    }
  }

  setUp(
    CacheWarm.inject(simulationProfile(cacheUsers, cacheDurationMins))
  ).protocols(httpProtocol)
    .assertions(forAll.successfulRequests.percent.gte(80))

}
