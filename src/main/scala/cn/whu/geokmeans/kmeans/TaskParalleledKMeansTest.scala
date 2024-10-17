package cn.whu.geokmeans.kmeans

import cn.whu.geokmeans.core.GeoVector
import org.apache.spark.{SparkConf, SparkContext}

import java.util.Date

object TaskParalleledKMeansTest {

  def main(args: Array[String]): Unit = {

    // 开始
    val startTime = new Date().getTime

    if (args.length < 4) {
      println("input <*inputPath> <*degOfParal> <*k> <*Iterations>")
      return
    }
    val inputPath = args(0)
    val degOfParal = args(1).toInt
    val numK = args(2).toInt
    val iterations = args(3).toInt

    // 获取数据
    val bufferedSource = scala.io.Source.fromFile(inputPath)
    val linesList: List[String] = bufferedSource.getLines().toList
    var dataList: List[KMeansDataElem] = Nil
    linesList.foreach(line => {
      val arrLine = line.split(",")
      val vector = new GeoVector(arrLine.size)
      (0 until arrLine.size).foreach(i => {
        val value = arrLine(i).toFloat
        vector.setValue(i, value)
      })
      val data = new KMeansDataElem(vector)
      dataList = dataList ::: List(data)
    })

    val dataReadTime = new Date().getTime

    // 配置参数，获得Spark上下文
    val sparkConf = new SparkConf()
      .setAppName("TaskParalleledKMeansTest")
      .set("spark.driver.maxResultSize", "4g")
    val sc = new SparkContext(sparkConf)
    val bcDataList = sc.broadcast(dataList)
    val arrKMeans = (1 to degOfParal).toArray

    val kMeansRDD = sc.parallelize(arrKMeans, degOfParal)
    val kMeansModelRDD = kMeansRDD.map(i => {
      val dataList = bcDataList.value
      val kMeans = new KMeans(dataList.toArray, numK, iterations)
      val kMeansModel = kMeans.runAlgorithm()
      (kMeans.getCost, kMeansModel)
    })
    kMeansModelRDD.sortByKey().take(1).foreach(data => {
      data._2.getCentersArray.foreach(center => {
        print(center.getClusterIndex + ":\t")
        center.getVector.getDataArray.foreach(v => print(v + ","))
        print("\n")
      })
    })

    //结束
    val endTime = new Date().getTime
    println("耗时：" + (endTime - startTime) + "毫秒")
    println("数据读取时间：" + (dataReadTime - startTime) + "毫秒")
  }

}
