package utils

import java.io.File

object PostProcessing {
  def main(args: Array[String]): Unit = {
    // Get the base directory for Gatling reports
    val baseDir = new File("build/reports/gatling/")

    // Check if the base directory exists
    if (!baseDir.exists()) {
      println(s"The base directory does not exist: $baseDir")
      return
    }

    // List all directories in the base directory
    val subDirs = baseDir.listFiles.filter(_.isDirectory)

    // If no subdirectories exist, exit
    if (subDirs.isEmpty) {
      println(s"No subdirectories found under: $baseDir")
      return
    }

    // Sort directories by last modified date to get the latest one
    val latestDir = subDirs.maxBy(_.lastModified)

    // Construct the path to stats.json within the latest directory
    val statsJsonPath = new File(latestDir, "js/stats.json")

    // Check if stats.json exists in the latest directory
    if (!statsJsonPath.exists()) {
      println(s"stats.json not found at: $statsJsonPath")
      return
    }

    println(s"Successfully loaded stats.json from: ${statsJsonPath.getAbsolutePath}")

    StatsGenerator.run(statsJsonPath)
  }
}
