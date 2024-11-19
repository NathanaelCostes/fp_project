import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

object Mainbak {

  def main(args: Array[String]): Unit = {
    // Initialize Spark session
    val spark = SparkSession.builder()
      .appName("SparkExercises")
      .master("local[*]") // Use all available cores
      .getOrCreate()

    val exercise1CSVPath = "data/stackoverflow.csv"
    val exercise2CSVPath = "data/exercise2.csv"
    val exercise3InputDir = "data/cve_data"

    // Exercise1.execute(spark, exercise1CSVPath)
    // Exercise2.execute(spark, exercise2CSVPath)

    // Exercise3 merged here
    try {
      var mergedData: DataFrame = spark.emptyDataFrame

      for (year <- 2023 to 2024) {
        val filePath = s"$exercise3InputDir/nvdcve-1.1-$year.json"
        println(s"Reading file: $filePath")

        // Read the JSON file and handle multi-line JSON
        val jsonDF = spark.read.option("multiLine", "true").json(filePath)

        // Print schema to debug the structure
        jsonDF.printSchema()

        // Check if CVE_Items exists in the schema
        if (jsonDF.columns.contains("CVE_Items")) {
          // Explode the CVE_Items array and extract required fields
          val cveDF = jsonDF
            .select(explode(col("CVE_Items")).as("CVE_Item"))
            .select(
              col("CVE_Item.cve.CVE_data_meta.ID").as("ID"),
              expr("filter(CVE_Item.cve.description.description_data, x -> x.lang = 'en')[0].value").as("Description"),
              expr("filter(CVE_Item.cve.problemtype.problemtype_data[0].description, x -> x.lang = 'en')[0].value").as("ProblemType"),
              // Handle both baseMetricV3 and baseMetricV2
              when(col("CVE_Item.impact.baseMetricV3").isNotNull,
                col("CVE_Item.impact.baseMetricV3.cvssV3.baseScore")
              ).otherwise(
                col("CVE_Item.impact.baseMetricV2.cvssV2.baseScore")
              ).as("BaseScore"),
              when(col("CVE_Item.impact.baseMetricV3").isNotNull,
                col("CVE_Item.impact.baseMetricV3.cvssV3.baseSeverity")
              ).otherwise(
                col("CVE_Item.impact.baseMetricV2.severity")
              ).as("Severity"),
              when(col("CVE_Item.impact.baseMetricV3").isNotNull,
                col("CVE_Item.impact.baseMetricV3.exploitabilityScore")
              ).otherwise(
                col("CVE_Item.impact.baseMetricV2.exploitabilityScore")
              ).as("ExploitabilityScore"),
              when(col("CVE_Item.impact.baseMetricV3").isNotNull,
                col("CVE_Item.impact.baseMetricV3.impactScore")
              ).otherwise(
                col("CVE_Item.impact.baseMetricV2.impactScore")
              ).as("ImpactScore"),
              col("CVE_Item.publishedDate").as("PublishedDate"),
              col("CVE_Item.lastModifiedDate").as("LastModifiedDate")
            )
            .filter(col("ID").isNotNull)

          // Merge data from each year
          if (mergedData.isEmpty) {
            mergedData = cveDF
          } else {
            mergedData = mergedData.union(cveDF)
          }
        } else {
          println(s"Warning: No 'CVE_Items' found in file $filePath")
        }
      }

      if (mergedData.isEmpty) {
        println("No data found.")
        return
      }

      // Sort the data by ImpactScore in descending order
      val sortedData = mergedData.orderBy(desc("ImpactScore"))

      println("Merged and Sorted Data:")
      sortedData.show(20, truncate = false)

      // Write the merged and sorted data to an output file
      val outputPath = s"$exercise3InputDir/merged_cve_data.json"
      sortedData.write.mode("overwrite").json(outputPath)
      println(s"Data written to $outputPath")

    } catch {
      case e: Exception =>
        println(s"Error: ${e.getMessage}")
        e.printStackTrace()
    }

    spark.stop()
  }
}