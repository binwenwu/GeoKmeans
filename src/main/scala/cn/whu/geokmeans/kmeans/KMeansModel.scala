package cn.whu.geokmeans.kmeans

/**
  * 利用KMean训练得到的模型
  *
  * @param centersArray 训练得到的聚类中心列表
  */
class KMeansModel(centersArray: Array[KMeansDataElem]) {

  private val mCentersArray: Array[KMeansDataElem] = centersArray

  /**
    * 获得模型的聚类中心
    *
    * @return
    */
  def getCentersArray: Array[KMeansDataElem] = this.mCentersArray

  /**
    * 预测一个元素属于哪个聚类
    * @param dataElem 要预测的元素
    * @return
    */
  def predict(dataElem: KMeansDataElem): Int = {
    KMeans.findClosest(dataElem, this.getCentersArray)._1
  }

  def printModel: Unit ={
    this.getCentersArray.foreach(center => {
      print(center.getClusterIndex + ":\t")
      center.getVector.getDataArray.foreach(v => print(v + ","))
      print("\n")
    })
  }

}
