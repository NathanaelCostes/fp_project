
import org.apache.spark.sql.{SparkSession, SaveMode}

object ConnectMongo {

  def main(args: Array[String]): Unit = {
    // Define MongoDB Atlas connection URI
    val mongoUri = "mongodb+srv://nathanaelcostes7:fvLXLZSDbpTluV7h@cluster0.mongodb.net/myDatabase"
    // Error :  Failed looking up SRV record for '_mongodb._tcp.cluster0.mongodb.net'.

    // Initialize Spark Session
    val spark = SparkSession.builder()
      .appName("MongoDBSparkConnector")
      .config("spark.mongodb.write.connection.uri", mongoUri)
      .config("spark.mongodb.write.database", "myDatabase")
      .config("spark.mongodb.write.collection", "myCollection")
      .config("spark.master", "local[*]") // Change for cluster execution
      .getOrCreate()

    // Example DataFrame to Write
    val exampleData = Seq(
      ("John", 29, "Engineer"),
      ("Jane", 34, "Data Scientist"),
      ("Mike", 41, "Manager")
    )

    import spark.implicits._

    val df = exampleData.toDF("name", "age", "occupation")

    // Write the DataFrame to MongoDB
    df.write
      .format("mongodb")
      .mode(SaveMode.Append)
      .save()

    println("Data successfully written to MongoDB Atlas!")

    // Stop Spark
    spark.stop()
  }
}
