import org.apache.spark.sql.{SaveMode, SparkSession}

object Neo4jTp {
  def main(args: Array[String]): Unit = {

    // Configuration de Neo4j
    val url = "neo4j://localhost:7474"
    val username = "neo4j"
    val password = "ELN"
    val dbname = "neo4j"

    // Création de SparkSession avec configuration Neo4j
    val spark = SparkSession.builder
      .config("neo4j.url", url)
      .config("neo4j.authentication.basic.username", username)
      .config("neo4j.authentication.basic.password", password)
      .config("neo4j.database", dbname)
      .appName("Neo4j Vulnerability TP")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Mock Data étendues
    val extendedMockData = Seq(
      ("CVE-2023-1001", "Vuln 1 - Cross-Site Scripting", 7.5, "HIGH", 3.2, 4.0, 2023),
      ("CVE-2023-1002", "Vuln 2 - SQL Injection", 6.0, "MEDIUM", 2.5, 3.8, 2023),
      ("CVE-2023-1003", "Vuln 3 - Remote Code Execution", 9.0, "CRITICAL", 5.0, 5.0, 2023),
      ("CVE-2023-1004", "Vuln 4 - DoS Attack", 5.0, "LOW", 1.5, 2.0, 2023),
      ("CVE-2023-1005", "Vuln 5 - Buffer Overflow", 8.5, "HIGH", 4.0, 4.5, 2023),
      ("CVE-2024-2001", "Vuln 6 - Cross-Site Scripting", 6.5, "MEDIUM", 3.0, 3.6, 2024),
      ("CVE-2024-2002", "Vuln 7 - SQL Injection", 8.0, "HIGH", 4.2, 4.8, 2024),
      ("CVE-2024-2003", "Vuln 8 - Privilege Escalation", 9.5, "CRITICAL", 5.5, 5.8, 2024),
      ("CVE-2024-2004", "Vuln 9 - Code Injection", 7.0, "HIGH", 3.5, 4.0, 2024),
      ("CVE-2024-2005", "Vuln 10 - Information Disclosure", 5.5, "LOW", 2.0, 2.5, 2024)
    ).toDF("cveID", "Description", "baseScore", "baseSeverity", "exploitabilityScore", "impactScore", "year")

    println("=== Extended Mock Data ===")
    extendedMockData.show()

    // Écriture des nœuds dans Neo4j
    println("=== Écriture des nœuds dans Neo4j ===")
    extendedMockData.write
      .format("org.neo4j.spark.DataSource")
      .mode(SaveMode.Overwrite)
      .option("labels", "Vulnerability")
      .option("node.keys", "cveID")
      .save()

    // Query pour le Top 5 des impacts pour 2023
    println("=== Top 5 Impact Scores for 2023 ===")
    val top2023 = spark.read
      .format("org.neo4j.spark.DataSource")
      .option("query", """
        MATCH (v:Vulnerability)
        WHERE v.year = 2023
        RETURN v.cveID AS ID, v.Description AS Description, v.impactScore AS Impact
        ORDER BY v.impactScore DESC
        LIMIT 5
      """)
      .load()

    top2023.show()

    // Query pour le Top 5 des impacts pour 2024
    println("=== Top 5 Impact Scores for 2024 ===")
    val top2024 = spark.read
      .format("org.neo4j.spark.DataSource")
      .option("query", """
        MATCH (v:Vulnerability)
        WHERE v.year = 2024
        RETURN v.cveID AS ID, v.Description AS Description, v.impactScore AS Impact
        ORDER BY v.impactScore DESC
        LIMIT 5
      """)
      .load()

    top2024.show()

  }
}