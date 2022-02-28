package scenarios

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils.Environment

object CcdCacheWarm {

  val RpeAPIURL = Environment.rpeAPIURL
  val IdamAPIURL = Environment.idamAPIURL
  val CcdAPIURL = Environment.ccdAPIURL

  val clientSecret = ConfigFactory.load.getString("auth.clientSecret")

  val userDetails = csv("UserCredentials.csv").circular

  val getServiceToken =

    exec(http("CCDCacheWarm_000_Auth")
      .post(RpeAPIURL + "/testing-support/lease")
      .body(StringBody("""{"microservice":"ccd_data"}""")).asJson
      .check(regex("(.+)").saveAs("authToken")))

    .pause(1)

  val getBearerToken =

    feed(userDetails)
    .exec(http("CCDCacheWarm_000_GetBearerToken")
      .post(IdamAPIURL + "/o/token")
      .formParam("grant_type", "password")
      .formParam("username", "${username}")
      .formParam("password", "${password}")
      .formParam("client_id", "ccd_gateway")
      .formParam("client_secret", clientSecret)
      .formParam("scope", "openid profile roles openid roles profile")
      .header("Content-Type", "application/x-www-form-urlencoded")
      .check(jsonPath("$.access_token").saveAs("bearerToken")))

    .pause(1)

  val getIdamId =

    exec(http("CCDCacheWarm_000_GetIdamID")
      .get(IdamAPIURL + "/details")
      .header("Authorization", "Bearer ${bearerToken}")
      .check(jsonPath("$.id").saveAs("idamId")))

    .pause(1)

  val loadJurisdictionsToWarmCache =

    exec(http("CCDCacheWarm_000_LoadJurisdictions")
      .get(CcdAPIURL + "/aggregated/caseworkers/${idamId}/jurisdictions?access=read")
      .header("Authorization", "Bearer ${bearerToken}")
      .header("ServiceAuthorization", "${authToken}")
      .header("Content-Type", "application/json")
      .check(jsonPath("$[0].id")))

    .pause(1)

}
