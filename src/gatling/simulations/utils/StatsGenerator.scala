package utils

import com.google.gson.{JsonElement, JsonObject, JsonParser}
import java.io.{File, FileReader, PrintWriter}
import scala.jdk.CollectionConverters._

/*
THIS CODE READS THE STATS.JSON FROM THE TEST AND GENERATES STATS PER GATLING TRANSACTION
THIS WILL REPLACE THE AGGREGATED METRICS USED BY THE GATLING JENKINS PLUGIN WITH INDIVIDUAL METRICS PER TRANSACTION
 */
object StatsGenerator {

  // Define the list of custom transaction names you want to graph in Jenkins
  // Note, the Jenkins plugin will also graph the aggregated simulation metrics by default
  val transactionNamesToGraph = Set("CCDCacheWarm_000_Auth", "CCDCacheWarm_000_LoadJurisdictions")

  def run(statsFile: File): Unit = {
    if (!statsFile.exists()) {
      println(s"[StatsGenerator] stats.json not found: ${statsFile.getAbsolutePath}")
      return
    }

    println(s"[StatsGenerator] Reading stats.json from: ${statsFile.getAbsolutePath}")

    val jsonElement: JsonElement = try {
      JsonParser.parseReader(new FileReader(statsFile))
    } catch {
      case e: Exception =>
        println(s"[StatsGenerator] Failed to parse stats.json: ${e.getMessage}")
        return
    }

    if (!jsonElement.isJsonObject || !jsonElement.getAsJsonObject.has("contents")) {
      println("[StatsGenerator] Invalid stats.json format")
      return
    }

    val contents = jsonElement.getAsJsonObject.getAsJsonObject("contents")

    for (entry <- contents.entrySet().asScala) {
      val reqElement = entry.getValue

      val request = reqElement.getAsJsonObject
      val stats = request.getAsJsonObject("stats")
      val name = sanitizeName(stats.get("name").getAsString)

      // Check if the transaction name is in the list of custom transaction names to graph
      if (transactionNamesToGraph.contains(name)) {
        // Extract all the necessary metrics for total, ok, ko, count, percentage, etc.
        val numberOfRequests = getStatsForMetric(stats, "numberOfRequests")
        val minResponseTime = getStatsForMetric(stats, "minResponseTime")
        val maxResponseTime = getStatsForMetric(stats, "maxResponseTime")
        val meanResponseTime = getStatsForMetric(stats, "meanResponseTime")
        val standardDeviation = getStatsForMetric(stats, "standardDeviation")
        val percentiles1 = getStatsForMetric(stats, "percentiles1")
        val percentiles2 = getStatsForMetric(stats, "percentiles2")
        val percentiles3 = getStatsForMetric(stats, "percentiles3")
        val percentiles4 = getStatsForMetric(stats, "percentiles4")

        // Grouping stats based on response times
        val group1 = getStatsForGroup(stats, "group1")
        val group2 = getStatsForGroup(stats, "group2")
        val group3 = getStatsForGroup(stats, "group3")
        val group4 = getStatsForGroup(stats, "group4")

        // Extract meanNumberOfRequestsPerSecond using getStatsForMetric method
        val meanNumberOfRequestsPerSecond = getStatsForMetric(stats, "meanNumberOfRequestsPerSecond")

        // Create the fake global stats JSON file
        createFakeSimulation(name, numberOfRequests, minResponseTime, maxResponseTime, meanResponseTime, standardDeviation,
          percentiles1, percentiles2, percentiles3, percentiles4, group1, group2, group3, group4,
          meanNumberOfRequestsPerSecond, "build/reports/gatling/")
      }
    }
  }

  private def sanitizeName(name: String): String = {
    name.replaceAll("[^a-zA-Z0-9-_]", "_")
  }

  private def getStat(stats: JsonObject, statKey: String, subKey: String): Double = {
    if (stats.has(statKey) && stats.getAsJsonObject(statKey).has(subKey)) {
      stats.getAsJsonObject(statKey).get(subKey).getAsDouble
    } else {
      println(s"[StatsGenerator] Missing '$statKey' or '$subKey' for request")
      0.0
    }
  }

  // Extracts the total, ok, and ko values for a specific metric from stats
  private def getStatsForMetric(stats: JsonObject, metric: String): MetricValues = {
    val total = getStat(stats, metric, "total")
    val ok = getStat(stats, metric, "ok")
    val ko = getStat(stats, metric, "ko")
    MetricValues(total, ok, ko)
  }

  // Extracts the count and percentage for a specific group
  private def getStatsForGroup(stats: JsonObject, group: String): GroupValues = {
    val count = getStat(stats, group, "count").toInt
    val percentage = getStat(stats, group, "percentage")
    GroupValues(count, percentage)
  }

  // Data structure to hold values for total, ok, and ko
  case class MetricValues(total: Double, ok: Double, ko: Double)

  // Data structure to hold values for count and percentage
  case class GroupValues(count: Int, percentage: Double)

  // Updated method to create the fake global stats JSON file with all the extracted stats
  private def createFakeSimulation(name: String, numberOfRequests: MetricValues, minResponseTime: MetricValues,
                                   maxResponseTime: MetricValues, meanResponseTime: MetricValues,
                                   standardDeviation: MetricValues, percentiles1: MetricValues,
                                   percentiles2: MetricValues, percentiles3: MetricValues,
                                   percentiles4: MetricValues, group1: GroupValues,
                                   group2: GroupValues, group3: GroupValues, group4: GroupValues,
                                   meanNumberOfRequestsPerSecond: MetricValues, basePath: String): Unit = {
    val dirName = s"${basePath}${name}-simulation-txnStats/js"
    val dir = new File(dirName)
    dir.mkdirs()

    val outputFile = new File(dir, "global_stats.json")
    val writer = new PrintWriter(outputFile)

    val json =
      s"""{
         |  "name": "All Requests",
         |  "numberOfRequests": { "total": ${numberOfRequests.total}, "ok": ${numberOfRequests.ok}, "ko": ${numberOfRequests.ko} },
         |  "minResponseTime": { "total": ${minResponseTime.total}, "ok": ${minResponseTime.ok}, "ko": ${minResponseTime.ko} },
         |  "maxResponseTime": { "total": ${maxResponseTime.total}, "ok": ${maxResponseTime.ok}, "ko": ${maxResponseTime.ko} },
         |  "meanResponseTime": { "total": ${meanResponseTime.total}, "ok": ${meanResponseTime.ok}, "ko": ${meanResponseTime.ko} },
         |  "standardDeviation": { "total": ${standardDeviation.total}, "ok": ${standardDeviation.ok}, "ko": ${standardDeviation.ko} },
         |  "percentiles1": { "total": ${percentiles1.total}, "ok": ${percentiles1.ok}, "ko": ${percentiles1.ko} },
         |  "percentiles2": { "total": ${percentiles2.total}, "ok": ${percentiles2.ok}, "ko": ${percentiles2.ko} },
         |  "percentiles3": { "total": ${percentiles3.total}, "ok": ${percentiles3.ok}, "ko": ${percentiles3.ko} },
         |  "percentiles4": { "total": ${percentiles4.total}, "ok": ${percentiles4.ok}, "ko": ${percentiles4.ko} },
         |  "group1": { "name": "t < 800 ms", "htmlName": "t < 800 ms", "count": ${group1.count}, "percentage": ${group1.percentage} },
         |  "group2": { "name": "800 ms <= t < 1200 ms", "htmlName": "t >= 800 ms <br> t < 1200 ms", "count": ${group2.count}, "percentage": ${group2.percentage} },
         |  "group3": { "name": "t >= 1200 ms", "htmlName": "t >= 1200 ms", "count": ${group3.count}, "percentage": ${group3.percentage} },
         |  "group4": { "name": "failed", "htmlName": "failed", "count": ${group4.count}, "percentage": ${group4.percentage} },
         |  "meanNumberOfRequestsPerSecond": { "total": ${meanNumberOfRequestsPerSecond.total}, "ok": ${meanNumberOfRequestsPerSecond.ok}, "ko": ${meanNumberOfRequestsPerSecond.ko} }
         |}""".stripMargin

    writer.write(json)
    writer.close()

    println(s"[StatsGenerator] ✅ Created global_stats.json for [$name] in $dirName")
  }
}
