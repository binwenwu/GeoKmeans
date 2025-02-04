package cn.whu.geokmeans.kmeans

import cn.whu.geokmeans.core.GeoVector
import org.apache.spark.rdd.RDD
import org.apache.spark.util.DoubleAccumulator

/**
  * Spark并行化KMeans
  * @param dataElemRDD 数据RDD
  * @param K 类别数
  * @param maxIterations 最大迭代次数
  * @param epsilon 精度
  */
class ParalleledKMeans(dataElemRDD: RDD[KMeansDataElem], K: Int, maxIterations: Int, epsilon: Double = 1e-2) {

  private val mDataElemRDD = dataElemRDD
  private val mK: Int = K
  private val mMaxItr: Int = maxIterations
  private val mEpsilon: Double = epsilon
  private var mCost: Double = Double.MaxValue

  /**
    * 获得数据RDD
    *
    * @return
    */
  def getDataElemRDD: RDD[KMeansDataElem] = this.mDataElemRDD

  /**
    * 获得K
    *
    * @return
    */
  def getK: Int = this.mK

  /**
    * 获得聚类得到的Cost
    *
    * @return
    */
  def getCost: Double = this.mCost

  /**
    * 获得最大迭代次数
    *
    * @return
    */
  def getMaxIterations: Int = this.mMaxItr

  /**
    * 获得迭代精度
    *
    * @return
    */
  def getEpsilon: Double = this.mEpsilon

  /**
    * 运行算法，获得最终聚类中心
    *
    * @return
    */
  def runAlgorithm(): KMeansModel = {
    val dataElemRDD = this.getDataElemRDD
    val sparkContext = dataElemRDD.sparkContext
    val k = this.getK
    // 获得初始聚类中心
    val arrCenters = dataElemRDD.takeSample(withReplacement = true, this.getK, 123L)
    println("初始化聚类中心")
    // 迭代运算
    var iteration: Int = 0
    var isContinue = true
    while (iteration < maxIterations && isContinue) {
      // 广播聚类中心
      val bcArrCenters = sparkContext.broadcast(arrCenters)
      // 累加器
      val costAccumulator: DoubleAccumulator = sparkContext.doubleAccumulator("Cost Accumulator")
      // 对每个分区的数据进行聚类
      val clusterElemRDD = dataElemRDD.mapPartitions(partition => {
        val centers = bcArrCenters.value // 聚类中心点数组
        val arrSumNumber: Array[Long] = Array.fill(k)(0L) // 每个聚类向量数量
        val dim = centers(0).getVector.getDimension
        val arrSumVectors: Array[GeoVector] = Array.fill(k)(new GeoVector(dim)) // 每个聚类向量和
        partition.foreach(dataElem => {
          val (index, cost) = KMeans.findClosest(dataElem, centers)
          arrSumNumber(index) += 1
          arrSumVectors(index) = arrSumVectors(index) + dataElem.getVector
          dataElem.setClusterIndex(index)
          costAccumulator.add(cost)
        })
        val arrCentersInPartition = for (i <- 0 until k) yield {
          if (arrSumNumber(i) == 0) {
            arrSumNumber(i) = 1
          }
          (i, (arrSumVectors(i),arrSumNumber(i))) // 每个聚类的各维度累加值和数量，
          // 如（1,({300,300,...},3)）表示聚类1有3个点，向量和为{300,300,...}
        }
        arrCentersInPartition.iterator
      })
      //合并各个分区的聚类累计值并平均后得到各个类别的新聚类中心
      val newArrCentersWithIndex = clusterElemRDD.reduceByKey(
        (v1, v2) => (v1._1 + v2._1, v1._2+ v2._2) // 将多个分区对应ID的聚类中心分类结果累加
        // v._1代表上述arrSumVectors(i)向量和，v._2代表上述arrSumNumber(i)聚类点数
      ).collect()
      bcArrCenters.unpersist(blocking = false)
      newArrCentersWithIndex.indices.foreach(i => {
        val newCenter =
          newArrCentersWithIndex(i)._2._1 /
            newArrCentersWithIndex(i)._2._2.toFloat // 计算平均值为新聚类中心
        arrCenters(i).setVector(newCenter)
        arrCenters(i).setClusterIndex(i)
      })
      this.mCost = costAccumulator.value
      // 迭代数加1
      iteration += 1
      println("第"+iteration+"次迭代")
    }
    new KMeansModel(arrCenters)
  }

}
