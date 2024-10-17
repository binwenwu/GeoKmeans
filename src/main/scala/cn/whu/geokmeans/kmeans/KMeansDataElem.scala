package cn.whu.geokmeans.kmeans

import cn.whu.geokmeans.core.GeoVector

/**
  * KMean基本数据单元
  *
  * @param vector 向量
  */
class KMeansDataElem(vector: GeoVector) extends Serializable{

  private var mClusterIndex: Int = 0
  private var mVector: GeoVector = vector

  def getClusterIndex: Int = this.mClusterIndex

  def getVector: GeoVector = this.mVector

  def setClusterIndex(clusterIndex: Int): Unit = {
    this.mClusterIndex = clusterIndex
  }

  def setVector(vector: GeoVector) = {
    this.mVector = vector
  }

  override def toString = s"KMeansDataElem($mClusterIndex, $mVector)"
}
