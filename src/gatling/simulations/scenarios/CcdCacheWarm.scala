package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import ccd._
import utils.Environment

object CcdCacheWarm {

  val userDetails = csv("UserCredentials.csv").circular

  val loadJurisdictionsToWarmCache = {

    feed(userDetails)

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
