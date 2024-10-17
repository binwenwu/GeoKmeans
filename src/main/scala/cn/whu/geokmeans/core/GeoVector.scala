package cn.whu.geokmeans.core

/**
  * 向量类
  *
  * @param dimension 向量的维度
  */
class GeoVector(dimension: Int) extends Serializable {

  private val mDim: Int = dimension
  private val mArrData: Array[Float] = new Array[Float](dimension)

  /**
    * 判断两个向量是否相同
    *
    * @param vector 向量
    * @return
    */
  def isEqualTo(vector: GeoVector): Boolean = {
    if (this.getDimension != vector.getDimension) {
      false
    } else {
      (0 until this.getDimension).foreach(i => {
        val v1 = this.getValue(i)
        val v2 = vector.getValue(i)
        if (v1 != v2) {
          return false
        }
      })
      true
    }
  }

  /**
    * 加法运算
    *
    * @param vector 同维向量
    * @return
    */
  def +(vector: GeoVector): GeoVector = {
    if (this.getDimension != vector.getDimension) {
      new GeoVector(0)
    } else {
      val rsVector = new GeoVector(this.getDimension)
      (0 until this.getDimension).foreach(i => {
        val v1 = this.getValue(i)
        val v2 = vector.getValue(i)
        rsVector.setValue(i, v1 + v2)
      })
      rsVector
    }
  }

  /**
    * 为向量某一维度赋值
    *
    * @param index 维度的索引
    * @param value 该维度的值
    * @return
    */
  def setValue(index: Int, value: Float): Boolean = {
    if (index >= this.getDimension || index < 0) {
      false
    } else {
      this.mArrData(index) = value
      true
    }
  }

  /**
    * 获得向量某一维度的值
    *
    * @param index 维度的索引
    * @return
    */
  def getValue(index: Int): Float = {
    if (index >= this.getDimension || index < 0) {
      Float.MinValue
    } else {
      this.mArrData(index)
    }
  }

  def getDataArray: Array[Float] = this.mArrData

  /**
    * 获得向量维度
    *
    * @return
    */
  def getDimension: Int = {
    this.mDim
  }

  /**
    * 获得该向量的复制
    *
    * @return
    */
  def getCopy: GeoVector = {
    val newVector = new GeoVector(this.getDimension)
    (0 until this.getDimension).foreach(i => newVector.setValue(i, this.getValue(i)))
    newVector
  }

  /**
    * 减法运算
    *
    * @param vector 同维向量
    * @return
    */
  def -(vector: GeoVector): GeoVector = {
    if (this.getDimension != vector.getDimension) {
      new GeoVector(0)
    } else {
      val rsVector = new GeoVector(this.getDimension)
      (0 until this.getDimension).foreach(i => {
        val v1 = this.getValue(i)
        val v2 = vector.getValue(i)
        rsVector.setValue(i, v1 - v2)
      })
      rsVector
    }
  }

  /**
    * 除法运算
    *
    * @param divisor 除数
    * @return
    */
  def /(divisor: Float): GeoVector = {
    if (divisor == 0) {
      new GeoVector(0)
    } else {
      val rsVector = new GeoVector(this.getDimension)
      (0 until this.getDimension).foreach(i => {
        val v1: Float = this.getValue(i)
        rsVector.setValue(i, v1 / divisor)
      })
      rsVector
    }
  }

  override def toString = s"GeoVector($mDim, $mArrData)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[GeoVector]

  override def equals(other: Any): Boolean = other match {
    case that: GeoVector =>
      (that canEqual this) &&
        mDim == that.mDim &&
        mArrData.sameElements(that.mArrData)
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(mDim, mArrData)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}


// 单例对象
object GeoVector {
  /**
    * 计算到另一个同维向量的欧式距离
    * EuclideanDist = (a1-a2){2} + (b1-b2){2} =  a1{2} + b12 + a2{2} + b2{2} - 2(a1a2+b1b2)
    *
    * @param vector_1 向量1
    * @param vector_2 向量2
    * @return
    */
  def euclideanDist(vector_1: GeoVector, vector_2: GeoVector): Double = {
    if (vector_1.getDimension != vector_2.getDimension) {
      Double.MinValue
    } else {
      var sum = 0.0
      (0 until vector_1.getDimension).foreach(i => {
        val v1 = vector_1.getValue(i)
        val v2 = vector_2.getValue(i)
        sum += (v1 - v2) * (v1 - v2)
      })
      sum
    }
  }
}
