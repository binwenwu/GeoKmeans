package cn.whu.geokmeans.action

import cn.whu.geokmeans.core.GeoVector
import cn.whu.geokmeans.kmeans.{KMeansDataElem, ParalleledKMeans}
import com.bc.ceres.core.ProgressMonitor
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}
import org.esa.snap.core.datamodel.{Band, Product, ProductData}
import org.esa.snap.core.util.ProductUtils
import org.esa.snap.dataio.geotiff.{GeoTiffProductReader, GeoTiffProductReaderPlugIn, GeoTiffProductWriter, GeoTiffProductWriterPlugIn}
import java.io.File
import java.util.Date


object ParalleledRSKMeansLivySession {

  def runMain(implicit sc: SparkContext, args: Array[String]): Unit = {

    /** 获得输入参数 * */
    // 指定输入影像数据路径、输出路径、K值、最大迭代次数、分区数量以及收敛阈值
    if (args.length < 5) {
      println("input <*inputPath> <*outputFile> <*k> <*maxIterations> <*partition> <epsilon>")
      return
    }
    val inputPath = args(0)
    val outputFile = args(1)
    val numK = args(2).toInt
    val maxIterations = args(3).toInt
    val partition = args(4).toInt
    val epsilon = {
      if (args.length == 6) {
        args(5).toDouble
      } else {
        1e-2
      }
    }

    // 设置日志级别
    sc.setLogLevel("ERROR")

    /** 记录开始时间 * */
    val startTime = new Date().getTime

    /** 读取影像数据 * */
    // 获取输入目录下的影像文件
    val tiffFile = new File(inputPath)


    // 初始化GeoTIFF读取器，读取影像的元数据信息
    val tifReaderPlugIn = new GeoTiffProductReaderPlugIn()
    val tifReader = new GeoTiffProductReader(tifReaderPlugIn)
    val sourceTifProduct = tifReader.readProductNodes(tiffFile.getAbsolutePath, null)
    val bandNum = sourceTifProduct.getNumBands // 波段数
    val width = sourceTifProduct.getSceneRasterWidth // 影像宽度
    val height = sourceTifProduct.getSceneRasterHeight // 影像高度

    println("波段数：" + bandNum + "，影像宽度：" + width + "，影像高度：" + height)

    // 初始化像素数据数组
    var arrPixelData: Array[KMeansDataElem] = null


    // 读取每个波段的像素数据并存储到数组中
    (0 until bandNum).indices.foreach(i => {
      val band = sourceTifProduct.getBandAt(i) // 获取波段
      if (i == 0) {
        // 初始化每个像素的KMeans数据元素，维度为波段数
        arrPixelData = Array.fill(width * height)(new KMeansDataElem(new GeoVector(bandNum)))
      }
      val pixels = new Array[Float](width * height) // 创建像素值数组
      band.readPixels(0, 0, width, height, pixels) // 读取像素值
      pixels.indices.foreach(j => {
        // 将像素值设置到相应的KMeans数据元素中
        arrPixelData(j).getVector.setValue(i, pixels(j))
      })
    })


    /** 运行KMeans算法 * */
    // 将像素数据并行化为RDD，并进行持久化
    val dataElemRDD = sc.parallelize(arrPixelData, partition)

    /*
      由于Spark RDD是惰性求值的，只有在遇到Action操作时才会真正执行计算。
      为了避免在迭代过程中多次读取数据，可以将RDD持久化到内存或磁盘中。
     */
    dataElemRDD.persist(StorageLevel.MEMORY_AND_DISK) // 将RDD存储到内存和磁盘中

    // 创建并行KMeans算法对象，并运行算法
    val paralleledKMeans = new ParalleledKMeans(dataElemRDD, numK, maxIterations, epsilon)
    val paralleledKMeansModel = paralleledKMeans.runAlgorithm() // 执行KMeans聚类
    paralleledKMeansModel.printModel // 打印模型信息

    /** 输出结果 * */
    // 初始化GeoTIFF写入器，准备输出文件
    val tifWriterPlugin = new GeoTiffProductWriterPlugIn()
    val tifProductWriter = new GeoTiffProductWriter(tifWriterPlugin)

    // 创建输出影像产品，并复制原始影像的元数据信息
    val outTifProduct = new Product("RSKMeansOutput", sourceTifProduct.getProductType, width, height)
    ProductUtils.copyProductNodes(sourceTifProduct, outTifProduct)
    sourceTifProduct.dispose() // 释放原始影像资源, 释放内存

    // 创建波段数据
    val productBand = new Band("band_1", ProductData.TYPE_UINT8, width, height)
    val productData = productBand.createCompatibleRasterData

    // 将聚类结果转化为灰度值，填充到波段数据中
    val arrClusterData = dataElemRDD.collect() // 将RDD数据收集到驱动程序
    val greySpace = 255 / numK // 将K类映射到灰度值区间[0, 255]
    arrClusterData.indices.foreach(i => {
      val dataElem = arrClusterData(i)
      val pixelValue = greySpace * dataElem.getClusterIndex // 计算每个像素的灰度值
      productData.setElemIntAt(i, pixelValue) // 设置该像素的灰度值
    })
    productBand.setData(productData)
    outTifProduct.addBand(productBand) // 将波段添加到输出产品中

    // 输出影像到GeoTIFF文件
    val outTifFile = new File(outputFile)
    outTifProduct.setProductWriter(tifProductWriter)
    // 写入影像文件头信息
    tifProductWriter.writeProductNodes(outTifProduct, outTifFile)
    // 写入波段数据
    tifProductWriter.writeBandRasterData(productBand, 0, 0, width, height, productData, ProgressMonitor.NULL)
    // 关闭写入器，完成文件写入
    tifProductWriter.close()

    /** 结束时间 * */
    val endTime = new Date().getTime
    println("任务耗时：" + (endTime - startTime) + "毫秒")

    sc.stop()
  }


  def main(args: Array[String]): Unit = {
    val inputPath = "data/GF.tif"
    val outputFile = "data/GF_Kmeans.tif"
    val numK = "10" // 聚类的类别数K
    val maxIterations = "20" // KMeans最大迭代次数
    val partition = "10" // 数据分区数
    val epsilon = "1e-2" // 收敛条件


    val parameters =  Array(inputPath, outputFile, numK, maxIterations, partition, epsilon)

    /** 初始化Spark * */
    // 配置Spark的运行参数，设置应用名、运行模式等
    val sparkConf = new SparkConf()
      .setAppName("GeoKmeans Example")
      .setMaster("local[*]") // 使用本地模式运行，[*]表示使用所有CPU核心

    // 创建Spark上下文对象
    val sc = new SparkContext(sparkConf)

    // 运行
    runMain(sc, parameters)
  }


}
