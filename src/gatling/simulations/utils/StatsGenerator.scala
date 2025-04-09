package utils

import com.google.gson.{JsonElement, JsonObject, JsonParser}
import java.io.{File, FileReader, PrintWriter}
import scala.jdk.CollectionConverters._

object StatsGenerator {

  // Main function to process stats.json file
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

    if (!jsonElement.isJsonObject) {
      println("[StatsGenerator] stats.json is not a JSON object")
      return
    }

    val rootObject = jsonElement.getAsJsonObject

    if (!rootObject.has("contents")) {
      println("[StatsGenerator] stats.json missing 'contents' section")
      return
    }

    val contents = rootObject.getAsJsonObject("contents")

    for (entry <- contents.entrySet().asScala) {
      val reqElement = entry.getValue

      val request = reqElement.getAsJsonObject
      val stats = request.getAsJsonObject("stats")
      val name = sanitizeName(stats.get("name").getAsString)

      val meanResponseTime = getStat(stats, "meanResponseTime", "total")
      val totalRequests = getStat(stats, "numberOfRequests", "total")

      println(s"[StatsGenerator] Request: $name, Mean Response Time: $meanResponseTime, Total Requests: $totalRequests")

      createFakeSimulation(name, meanResponseTime, totalRequests, "build/")
    }
  }

  // Helper method to sanitize request names (replace non-alphanumeric characters with underscores)
  private def sanitizeName(name: String): String = {
    name.replaceAll("[^a-zA-Z0-9-_]", "_")
  }

  // Helper method to safely extract stats values from the JSON
  private def getStat(stats: JsonObject, statKey: String, subKey: String): Int = {
    if (stats.has(statKey) && stats.getAsJsonObject(statKey).has(subKey)) {
      stats.getAsJsonObject(statKey).get(subKey).getAsInt
    } else {
      println(s"[StatsGenerator] Missing '$statKey' or '$subKey' for request")
      0 // Default to 0 if missing
    }
  }

  // Method to create a fake global stats file for each request
  private def createFakeSimulation(name: String, meanTime: Int, numRequests: Int, basePath: String): Unit = {
    val dirName = s"${basePath}${name}-simulation-20250409103100000"
    val dir = new File(dirName)
    dir.mkdirs()

    val outputFile = new File(dir, "global_stats.json")
    val writer = new PrintWriter(outputFile)

    // Sample structure for the global_stats.json file
    val json =
      s"""{
         |  "name": "All Requests",
         |  "numberOfRequests": { "total": $numRequests, "ok": $numRequests, "ko": 0 },
         |  "minResponseTime": { "total": $meanTime, "ok": $meanTime, "ko": 0 },
         |  "maxResponseTime": { "total": $meanTime, "ok": $meanTime, "ko": 0 },
         |  "meanResponseTime": { "total": $meanTime, "ok": $meanTime, "ko": 0 },
         |  "standardDeviation": { "total": $meanTime, "ok": $meanTime, "ko": 0 },
         |  "percentiles1": { "total": $meanTime, "ok": $meanTime, "ko": 0 },
         |  "percentiles2": { "total": $meanTime, "ok": $meanTime, "ko": 0 },
         |  "percentiles3": { "total": $meanTime, "ok": $meanTime, "ko": 0 },
         |  "percentiles4": { "total": $meanTime, "ok": $meanTime, "ko": 0 },
         |  "group1": { "name": "t < 800 ms", "htmlName": "t < 800 ms", "count": $numRequests, "percentage": 100 },
         |  "group2": { "name": "800 ms <= t < 1200 ms", "htmlName": "t >= 800 ms <br> t < 1200 ms", "count": 0, "percentage": 0 },
         |  "group3": { "name": "t >= 1200 ms", "htmlName": "t >= 1200 ms", "count": 0, "percentage": 0 },
         |  "group4": { "name": "failed", "htmlName": "failed", "count": 0, "percentage": 0 },
         |  "meanNumberOfRequestsPerSecond": { "total": 1.0, "ok": 1.0, "ko": 0.0 }
         |}""".stripMargin

    writer.write(json)
    writer.close()

    println(s"[PerRequestStatsGenerator] ✅ Created global_stats.json for [$name] in $dirName")
  }
}
