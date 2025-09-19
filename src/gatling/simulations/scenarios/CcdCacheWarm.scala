package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import ccd._
import io.gatling.core.session.Expression
import utils.Environment

object CcdCacheWarm {

  val userDetails = csv("UserCredentials.csv").circular

  def loadJurisdictionsToWarmCache(caseIdFeeder: Iterator[Map[String, Any]]) = {

    feed(caseIdFeeder)

    .exec {
      session =>
        println(session)
        session
    }

    .feed(userDetails)

    .exec(CcdHelper.authenticate("#{username}", "#{password}", CcdCaseTypes.CCD.microservice))

    .exec(http("CCDCacheWarm_000_LoadJurisdictions")
      .get(Environment.ccdAPIURL + "/aggregated/caseworkers/#{idamId}/jurisdictions?access=read")
      .header("Authorization", "Bearer #{bearerToken}")
      .header("ServiceAuthorization", "#{authToken}")
      .header("Content-Type", "application/json")
      .check(jsonPath("$[0].id")))

    .pause(1)
  }

}
