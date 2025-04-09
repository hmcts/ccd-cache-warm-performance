package utils

import com.google.gson.{JsonElement, JsonObject, JsonParser}
import java.io.{File, FileReader, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Date
import scala.jdk.CollectionConverters._

/*
THIS CODE READS THE STATS.JSON FROM THE TEST AND GENERATORS STATS PER GATLING TRANSACTION
THIS WILL REPLACE THE AGGREGATED METRICS USED BY THE GATLING JENKINS PLUGIN WITH INDIVIDUAL METRICS PER TRANSACTION
 */
object StatsGenerator {

  private val timestamp: String = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())

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

    //move the original simulation to a different folder location, so the global_stats isn't picked up by the Jenkins plugin
    val statsRootDir = new File("build/reports/gatling")
    moveOriginalSimulation(statsRootDir)

    for (entry <- contents.entrySet().asScala) {
      val reqElement = entry.getValue

      val request = reqElement.getAsJsonObject
      val stats = request.getAsJsonObject("stats")
      val name = sanitizeName(stats.get("name").getAsString)

      val meanResponseTime = getStat(stats, "meanResponseTime", "total")
      val totalRequests = getStat(stats, "numberOfRequests", "total")

      println(s"[StatsGenerator] Request: $name, Mean Response Time: $meanResponseTime, Total Requests: $totalRequests")

      createFakeSimulation(name, meanResponseTime, totalRequests, "build/reports/gatling/")
    }
  }

  private def sanitizeName(name: String): String = {
    val parts = name.split("_", 2)
    val stripped = if (parts.length > 1) parts(1) else name
    stripped.replaceAll("[^a-zA-Z0-9-_]", "_")
  }

  private def getStat(stats: JsonObject, statKey: String, subKey: String): Int = {
    if (stats.has(statKey) && stats.getAsJsonObject(statKey).has(subKey)) {
      stats.getAsJsonObject(statKey).get(subKey).getAsInt
    } else {
      println(s"[StatsGenerator] Missing '$statKey' or '$subKey' for request")
      0
    }
  }

  private def createFakeSimulation(name: String, meanTime: Int, numRequests: Int, basePath: String): Unit = {
    val dirName = s"${basePath}${name}-simulation-transactionStats/js"
    val dir = new File(dirName)
    dir.mkdirs()

    val outputFile = new File(dir, "global_stats.json")
    val writer = new PrintWriter(outputFile)

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

    println(s"[StatsGenerator] ✅ Created global_stats.json for [$name] in $dirName")
  }

  private def moveOriginalSimulation(simRoot: File): Unit = {
    if (!simRoot.exists()) return

    val originalsDir = new File(simRoot, "originals")
    originalsDir.mkdirs()

    simRoot.listFiles()
      .filter(f => f.isDirectory && f.getName.matches(".*-simulation-\\d{17}"))
      .foreach { dir =>
        val targetDir = new File(originalsDir, dir.getName)
        val success = dir.renameTo(targetDir)
        if (success)
          println(s"[StatsGenerator] 🔒 Moved original simulation report to: ${targetDir.getPath}")
        else
          println(s"[StatsGenerator] ⚠️ Failed to move original simulation folder: ${dir.getPath}")
      }
  }
}
