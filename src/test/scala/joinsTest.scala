/**
 * Created by juanmanuelsanchez on 12/8/14.
 */

package dedupTesting

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.expressions.Row

import org.scalatest.FunSuite
import org.apache.spark._

import org.apache.spark.SparkContext._
import org.apache.spark.util.Utils

import sampleclean.clean.deduplication._
import sampleclean.clean.deduplication.join.{SimilarityJoin, PassJoin, BroadcastJoin}
import sampleclean.clean.featurize.AnnotatedSimilarityFeaturizer.{WeightedCosineSimilarity, WeightedDiceSimilarity, WeightedOverlapSimilarity, WeightedJaccardSimilarity}
import sampleclean.clean.featurize.Tokenizer.{DelimiterTokenizer, WhiteSpaceTokenizer, WordTokenizer}
import sampleclean.clean.featurize.{AnnotatedSimilarityFeaturizer, Tokenizer, SimilarityFeaturizer}


class JoinsTest extends FunSuite with Serializable {

  val conf = new SparkConf()
    .setMaster("local[4]")
    .setAppName("SCUnitTest")
    //.set("spark.driver.memory","700m")
    //.set("spark.executor.memory","700m")
    //.set("spark.default.parallelism","16")
  val sc = new SparkContext(conf)



  /*test("broadcast self join accuracy") {

    val context = List("record")
    val colNames = List("record")
    val tok = new DelimiterTokenizer(" ")
    val path = "./src/test/resources"
    var blocker: AnnotatedSimilarityFeaturizer = null
    var bJoin: SimilarityJoin = null
    var rowRDDLarge: RDD[Row] = null
    //rowRDDLarge = sc.textFile(path + "/dirtyJaccard100dups").map(r => Row.fromSeq(r.split(":").toSeq))

    // 100 duplicates with Jaccard similarity = 0.5
    rowRDDLarge = sc.textFile(path + "/dirtyJaccard100dups").map(Row(_))
    blocker = new WeightedJaccardSimilarity(colNames,context,tok,0.51)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 0)
    blocker = new WeightedJaccardSimilarity(colNames,context,tok,0.5)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 100)

    // weighted Jaccard is ~0.465
    blocker = new WeightedJaccardSimilarity(colNames,context,tok,0.466)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 0)
    blocker = new WeightedJaccardSimilarity(colNames,context,tok,0.465)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 100)


    // 100 duplicates with Overlap similarity = 5
    rowRDDLarge = sc.textFile(path + "/dirtyOverlap100dups").map(Row(_))
    blocker = new WeightedOverlapSimilarity(colNames,context,tok,6)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 0)
    blocker = new WeightedOverlapSimilarity(colNames,context,tok,5)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 100)

    // weighted Overlap is 10
    blocker = new WeightedOverlapSimilarity(colNames,context,tok,10.1)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 0)
    blocker = new WeightedOverlapSimilarity(colNames,context,tok,10)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 100)

    // 100 duplicates with Dice similarity = 0.8
    rowRDDLarge = sc.textFile(path + "/dirtyDice100dups").map(Row(_))
    blocker = new WeightedDiceSimilarity(colNames,context,tok,0.81)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 0)
    blocker = new WeightedDiceSimilarity(colNames,context,tok,0.8)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 100)

    // weighted Dice is ~0.776
    blocker = new WeightedDiceSimilarity(colNames,context,tok,0.777)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 0)
    blocker = new WeightedDiceSimilarity(colNames,context,tok,0.776)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 100)

    // 100 duplicates with Cosine similarity = 0.5
    rowRDDLarge = sc.textFile(path + "/dirtyCosine100dups").map(Row(_))
    blocker = new WeightedCosineSimilarity(colNames,context,tok,0.51)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 0)
    blocker = new WeightedCosineSimilarity(colNames,context,tok,0.5)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 100)

    // weighted Cosine is ~0.473
    blocker = new WeightedCosineSimilarity(colNames,context,tok,0.474)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 0)
    blocker = new WeightedCosineSimilarity(colNames,context,tok,0.473)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDLarge,rowRDDLarge).count() == 100)


  }*/

  /*test("broadcast sample join accuracy"){
    val context = List("record")
    val colNames = List("record")
    val tok = new DelimiterTokenizer(" ")
    val path = "./src/test/resources"
    var blocker: AnnotatedSimilarityFeaturizer = null
    var bJoin: SimilarityJoin = null
    var rowRDDLarge: RDD[Row] = null
    var rowRDDSmall: RDD[Row] = null


    // 100 duplicates with Jaccard similarity = 0.5
    rowRDDLarge = sc.textFile(path + "/dirtyJaccard100dups").map(Row(_))
    rowRDDSmall = rowRDDLarge.sample(false,0.5).cache()
    blocker = new WeightedJaccardSimilarity(colNames,context,tok,0.51)
    bJoin = new BroadcastJoin(sc,blocker,false)
    // returns 0 dups + original pairs
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() == rowRDDSmall.count())
    blocker = new WeightedJaccardSimilarity(colNames,context,tok,0.5)
    bJoin = new BroadcastJoin(sc,blocker,false)
    // returns ~50 dups * 2 + original pairs
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() >= 45 * 2 + rowRDDSmall.count())

    // weighted Jaccard is ~0.465
    blocker = new WeightedJaccardSimilarity(colNames,context,tok,0.466)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() == rowRDDSmall.count())
    blocker = new WeightedJaccardSimilarity(colNames,context,tok,0.465)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() >= 45 * 2 + rowRDDSmall.count())


    // 100 duplicates with Overlap similarity = 5
    rowRDDLarge = sc.textFile(path + "/dirtyOverlap100dups").map(Row(_))
    rowRDDSmall = rowRDDLarge.sample(false,0.5).cache()
    blocker = new WeightedOverlapSimilarity(colNames,context,tok,6)
    bJoin = new BroadcastJoin(sc,blocker,false)
    // about half of self-pairs won't be considered duplicates
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() >= (rowRDDSmall.count()/2 - 8))
    blocker = new WeightedOverlapSimilarity(colNames,context,tok,5)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() >= 45 * 2 + rowRDDSmall.count()/2)

    // weighted Overlap is 10
    blocker = new WeightedOverlapSimilarity(colNames,context,tok,10.1)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() >= (rowRDDSmall.count()/2 - 8))
    blocker = new WeightedOverlapSimilarity(colNames,context,tok,10)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() >= 45 * 2 + rowRDDSmall.count()/2)

    // 100 duplicates with Dice similarity = 0.8
    rowRDDLarge = sc.textFile(path + "/dirtyDice100dups").map(Row(_))
    rowRDDSmall = rowRDDLarge.sample(false,0.5).cache()
    blocker = new WeightedDiceSimilarity(colNames,context,tok,0.81)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() == rowRDDSmall.count())
    blocker = new WeightedDiceSimilarity(colNames,context,tok,0.8)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() >= 45 * 2 + rowRDDSmall.count())

    // weighted Dice is ~0.776
    blocker = new WeightedDiceSimilarity(colNames,context,tok,0.777)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() == rowRDDSmall.count())
    blocker = new WeightedDiceSimilarity(colNames,context,tok,0.776)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() >= 45 * 2 + rowRDDSmall.count())

    // 100 duplicates with Cosine similarity = 0.5
    rowRDDLarge = sc.textFile(path + "/dirtyCosine100dups").map(Row(_))
    rowRDDSmall = rowRDDLarge.sample(false,0.5).cache()
    blocker = new WeightedCosineSimilarity(colNames,context,tok,0.51)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() == rowRDDSmall.count())
    blocker = new WeightedCosineSimilarity(colNames,context,tok,0.5)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() >= 45 * 2 + rowRDDSmall.count())

    // weighted Cosine is ~0.473
    blocker = new WeightedCosineSimilarity(colNames,context,tok,0.474)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() == rowRDDSmall.count())
    blocker = new WeightedCosineSimilarity(colNames,context,tok,0.473)
    bJoin = new BroadcastJoin(sc,blocker,true)
    assert(bJoin.join(rowRDDSmall,rowRDDLarge).count() >= 45 * 2 + rowRDDSmall.count())

  }*/

  test("broadcast join speed"){
    val context = List("record")
    val colNames = List("record")
    val tok = new DelimiterTokenizer(" ")
    val path = "/Users/juanmanuelsanchez/Documents/sampleCleanData"
    var blocker: AnnotatedSimilarityFeaturizer = null
    var bJoin: SimilarityJoin = null
    var rowRDDLarge: RDD[Row] = sc.textFile(path + "/1000testData").map(Row(_)).cache()
    //var rowRDDSmall: RDD[Row] = rowRDDLarge.sample(false,0.1).cache()
    rowRDDLarge.count()
    //rowRDDSmall.count()

    def time[R](block: => R, name:String, iterations:Int): R = {
      // 20 first iterations could be considered outliers
      val times = (1 to iterations + 20).map {i =>
        val t0 = System.nanoTime()
        val result = block // call-by-name
        val t1 = System.nanoTime()
        t1 - t0
      }
      //println(times.map(_.toDouble /1000000000 ))
      println("Elapsed time for " + name + ": " + (times.drop(20).sum.toDouble / times.drop(20).size) /1000000000 + "sec")
      block
    }


    blocker = new WeightedCosineSimilarity(colNames,context,tok,0.5)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(time(bJoin.join(rowRDDLarge,rowRDDLarge).count(), "cosine join",10) > 0)

    blocker = new WeightedOverlapSimilarity(colNames,context,tok,10)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(time(bJoin.join(rowRDDLarge,rowRDDLarge).count(), "overlap join",10) > 0)

    blocker = new WeightedDiceSimilarity(colNames,context,tok,0.8)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(time(bJoin.join(rowRDDLarge,rowRDDLarge).count(), "dice join",10) > 0)

    blocker = new WeightedJaccardSimilarity(colNames,context,tok,0.5)
    bJoin = new BroadcastJoin(sc,blocker,false)
    assert(time(bJoin.join(rowRDDLarge,rowRDDLarge).count(), "jaccard join",10) > 0)



  }



}
