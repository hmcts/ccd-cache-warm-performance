package utils

object Environment {

  val idamAPIURL = "https://idam-api.#{env}.platform.hmcts.net"
  val rpeAPIURL = "http://rpe-service-auth-provider-#{env}.service.core-compute-#{env}.internal"
  val ccdAPIURL = "http://ccd-data-store-api-#{env}.service.core-compute-#{env}.internal"

}
