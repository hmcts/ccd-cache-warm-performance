package utils

import elasticSearchFeeder._

object UserElasticSearchFeederConfig extends ElasticSearchFeederConfigDefaultValues(
  RECORDS_REQUIRED_OVERRIDE = 20
)
