# Samples for compliant and non compliant Spark applications.

### 1 Using an un-approved cryptographic algorithm on a FIPS compliant environment.

```scala

val textFile = sc.textFile("./")
val counts = textFile.flatMap(line => line.split(" "))
                 .map(word => (word, 1))
                 .reduceByKey(_ + _)
counts.saveAsTextFile("hdfs://...")
```
