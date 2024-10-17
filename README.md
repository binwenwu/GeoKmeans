## Introduction

**`GeoKmeans`** is an implementation of the K-Means clustering algorithm designed for geospatial data processing using `Apache Spark`. This project allows efficient parallelization of the `K-Means` algorithm, enabling it to handle large-scale geospatial datasets stored in formats such as `GeoTIFF`.

## Key Features

- **Geospatial Data Clustering**: Specially optimized to process large geospatial datasets, focusing on clustering geospatial points based on both spatial coordinates and feature values.
- **Parallel Processing with Spark**: Uses the distributed computing power of Apache Spark to parallelize the K-Means algorithm, significantly improving performance on large datasets.
- **Customizable Parameters**: Users can configure the number of clusters (K), iterations, and the partitioning scheme to suit their data and computational needs.
- **Input/Output Support**: Supports GeoTIFF file format as input and output for geospatial data clustering.

## Catalogue

- `ParalleledRSKMeansLocal.scala`：Local mode
- `ParalleledRSKMeansSparkSubmit.scala`：Spark submit submission method
- `ParalleledRSKMeansLivyBatch.scala`：Livy Batch submission method
- `ParalleledRSKMeansLocalLivySession.scala`：Livy Session submission method
- `GeoKmeans_jar`：Packaging results of workpieces

## Example usage

#### 1  Local mode

> Simply run the main function in a code editor such as IDEA

#### 2 Spark submit submission method

> Here, I take the yarn mode Spark cluster as an example

```bash
$SPARK_HOME/bin/spark-submit \
--master yarn \
--deploy-mode cluster \
--class cn.whu.geokmeans.action.ParalleledRSKMeansSparkSubmit \
--driver-memory 4g \
--driver-cores 10 \
--executor-memory 4g \
--executor-cores 10 \
--num-executors 5 \
--conf spark.executor.extraClassPath=/home/nfs-storage/spark_kmeans_example/GeoKmeans_jar/* \
--conf spark.driver.extraClassPath=/home/nfs-storage/spark_kmeans_example/GeoKmeans_jar/* \
/home/nfs-storage/spark_kmeans_example/GeoKmeans_jar/GeoKmeans.jar \
/home/nfs-storage/spark_kmeans_example/GF.tif \
/home/nfs-storage/spark_kmeans_example/GF_Kmeans.tif 10 20 4
```

#### 3 Livy Batch submission method

```bash
curl -X POST -H "Content-Type: application/json" -d '{
  "file": "local:/home/nfs-storage/spark_kmeans_example/GeoKmeans_jar/GeoKmeans.jar",
  "className": "cn.whu.geokmeans.action.ParalleledRSKMeansLivyBatch",
  "args": ["/home/nfs-storage/spark_kmeans_example/GF.tif", "/home/nfs-storage/spark_kmeans_example/GF_Kmeans.tif", "10", "20", "4"],
  "conf": {
    "spark.driver.cores": 10,
    "spark.driver.memory": "4g",
    "spark.executor.cores": 10,
    "spark.executor.memory": "4g",
    "spark.executor.instances": 5
  }
}' http://10.101.240.60:8998/batches
```

#### 4 Livy Session submission method

- First, initialize a session

> Here we have developed the` jar` packages that need to be run, and provided the dependent `jar` package paths required by `GeoKmeans.jar` for the driver and executor

```bash
curl -X POST -H "Content-Type: application/json" -d '{
  "kind": "spark",
  "jars": ["file:///home/nfs-storage/spark_kmeans_example/GeoKmeans_jar/GeoKmeans.jar"],
  "conf": {
    "spark.driver.cores": 2,
    "spark.driver.memory": "3g",
    "spark.executor.cores": 3,
    "spark.executor.memory": "3g",
    "spark.executor.instances": 4,
    "spark.driver.extraClassPath": "local:/home/nfs-storage/spark_kmeans_example/GeoKmeans_jar/*",
    "spark.executor.extraClassPath": "local:/home/nfs-storage/spark_kmeans_example/GeoKmeans_jar/*"
  }
}' http://10.101.240.60:8998/sessions
```

- Then we can specify which function to run

```bash
curl -X POST -H "Content-Type: application/json" -d '{
  "code": "cn.whu.geokmeans.action.ParalleledRSKMeansLivySession.runMain(sc, Array(\"/home/nfs-storage/spark_kmeans_example/GF.tif\", \"/home/nfs-storage/spark_kmeans_example/GF_Kmeans.tif\", \"10\", \"20\", \"4\"))",
  "kind": "spark"
}' http://10.101.240.60:8998/sessions/0/statements
```

#### 5 Operation results

> Original image and clustering result image

![image](https://cdn.jsdelivr.net/gh/binwenwu/picgo_demo/img/image.webp)
