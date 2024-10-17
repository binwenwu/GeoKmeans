package cn.whu.geokmeans.kmeans

import cn.whu.geokmeans.core.GeoVector

import scala.util.Random

/**
  * 普通KMeans类
  *
  * @param arrData       (向量，二范数) 数组
  * @param K             K的值
  * @param maxIterations 最大迭代次数
  * @param epsilon       精度,默认为1e-4
  */
class KMeans(arrData: Array[KMeansDataElem], K: Int, maxIterations: Int, epsilon: Double = 1e-2) {

  private val mArrData: Array[KMeansDataElem] = arrData
  private val mK: Int = K
  private val mMaxItr: Int = maxIterations
  private val mEpsilon: Double = epsilon
  private var mCost: Double = Double.MaxValue

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
    * 获得向量数组
    *
    * @return
    */
  def getDataArray: Array[KMeansDataElem] = this.mArrData

  /**
    * 获得迭代精度
    *
    * @return
    */
  def getEpsilon: Double = this.mEpsilon

  /**
    * 随机获取K个中心点
    *
    * @return
    */
  def sampleCentersArray(): Array[KMeansDataElem] = {
    val arrData = this.getDataArray
    // 随机抽取出K个中心点
    val rand = new Random(System.nanoTime())
    var numberList: List[Int] = Nil
    while (numberList.length < this.getK) {
      val randNum = rand.nextInt(arrData.length)
      var bExit = false
      numberList.foreach(number => {
        if (arrData(number).getVector.equals(arrData(randNum).getVector)) {
          bExit = true
        }
      })
      if (!bExit) {
        numberList = numberList ::: List(randNum)
      }
    }
    // 获得中心点数据
    val arrCenters = new Array[KMeansDataElem](this.getK)
    numberList.indices.foreach(i => {
      val number = numberList(i)
      arrCenters(i) = new KMeansDataElem(arrData(number).getVector.getCopy)
      arrCenters(i).setClusterIndex(i)
    })
    arrCenters
  }

  /**
    * 运行算法，获得最终聚类中心
    *
    * @return
    */
  def runAlgorithm(): KMeansModel = {
    val dimension = this.getDataArray.head.getVector.getDimension
    // 获得初始聚类中心
    val arrCenters = this.sampleCentersArray()
    // 迭代
    var iteration: Int = 0
    var isContinue = true
    while (iteration < maxIterations && isContinue) {
      this.mCost = 0.0
      val arrSumVectors: Array[GeoVector] = Array.fill(arrCenters.length)(new GeoVector(dimension))
      val arrSumClusters: Array[Int] = Array.fill(arrCenters.length)(0)
      // 遍历每个向量
      this.getDataArray.foreach(data => {
        // 获得聚类类别和距离聚类中心类别，收集信息
        val (index, cost) = KMeans.findClosest(data, arrCenters)
        this.mCost += cost
        data.setClusterIndex(index)
        arrSumClusters(index) += 1 // 统计每个类别向量数
        arrSumVectors(index) = arrSumVectors(index) + data.getVector // 计算聚类的向量和
      })
      // 更新聚类中心
      arrCenters.indices.foreach(i => {
        val center = arrCenters(i)
        val newCenterVector = arrSumVectors(i) / arrSumClusters(i).toFloat
        arrCenters(i).setVector(newCenterVector) // 更新向量和二范数
      })
      iteration += 1
    }
    println("iteration: " + iteration)
    new KMeansModel(arrCenters)
  }

}

object KMeans {
  /**
    * 寻找最近的聚类中心
    *
    * @param data       每个数据
    * @param arrCenters 聚类中心数组
    * @return
    */
  def findClosest(data: KMeansDataElem, arrCenters: Array[KMeansDataElem]): (Int, Double) = {
    assert(arrCenters.length > 0)
    var minDistance = GeoVector.euclideanDist(data.getVector, arrCenters(0).getVector)
    var index = 0
    arrCenters.indices.foreach(i => {
      val center = arrCenters(i)
      // 判断欧式距离是否小于当前最短距离
      val distance = GeoVector.euclideanDist(data.getVector, center.getVector)
      if (distance < minDistance) {
        minDistance = distance
        index = i
      }
    })
    (index, minDistance)
  }
}
