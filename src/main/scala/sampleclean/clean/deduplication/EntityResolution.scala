package sampleclean.clean.deduplication

import sampleclean.api.SampleCleanContext
import sampleclean.clean.algorithm.SampleCleanAlgorithm
import org.apache.spark.SparkContext._
import org.apache.spark.sql.SQLContext
import sampleclean.clean.algorithm.AlgorithmParameters
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SchemaRDD, Row}
import org.apache.spark.graphx._
import sampleclean.clean.deduplication.join.BlockerMatcherSelfJoinSequence

import sampleclean.clean.deduplication.join._
import sampleclean.clean.featurize.AnnotatedSimilarityFeaturizer._
import sampleclean.clean.featurize.Tokenizer._
import sampleclean.clean.deduplication.matcher._
import sampleclean.clean.featurize._

/* This is the abstract class for attribute deduplication
 * it implements many basic structure and the error handling 
 * for the class.
 */
class EntityResolution(params:AlgorithmParameters, 
							scc: SampleCleanContext,
              sampleTableName:String,
              components: BlockerMatcherSelfJoinSequence
              ) extends SampleCleanAlgorithm(params, scc, sampleTableName) {

    //validate params before starting
    validateParameters()

    //there are the read-only class variables that subclasses can use (validated at execution time)
    val attr = params.get("attr").asInstanceOf[String]
    val mergeStrategy = params.get("mergeStrategy").asInstanceOf[String]
    
    //these are dynamic class variables
    var attrCol = 1
    var hashCol = 0

    var graphXGraph:Graph[(String, Set[String]), Double] = null

    /*
      Sets the dynamic variables at exec time
     */
    def setTableParameters(sampleTableName: String) = {
        attrCol = scc.getColAsIndex(sampleTableName,attr)
        hashCol = scc.getColAsIndex(sampleTableName,"hash")
        //curSampleTableName = sampleTableName
    }

    /*
      This function validates the parameters of the class
     */
    def validateParameters() = {
        if(!params.exists("attr"))
          throw new RuntimeException("Attribute deduplication is specified on a single attribute, you need to provide this as a parameter: attr")

        if(!params.exists("mergeStrategy"))
          throw new RuntimeException("You need to specify a strategy to resolve a set of duplicated attributes to a canonical value: mergeStrategy")
    }

    def exec() = {
        validateParameters()
        setTableParameters(sampleTableName)

        val sampleTableRDD = scc.getCleanSample(sampleTableName)
        val attrCountGroup = sampleTableRDD.map(x => 
                                          (x(attrCol).asInstanceOf[String],
                                           x(hashCol).asInstanceOf[String])).
                                          groupByKey()
        val attrCountRdd  = attrCountGroup.map(x => Row(x._1, x._2.size.toLong))
        val vertexRDD = attrCountGroup.map(x => (x._1.hashCode().toLong,
                               (x._1, x._2.toSet)))

        components.updateContext(List(attr,"count"))

        val edgeRDD: RDD[(Long, Long, Double)] = scc.getSparkContext().parallelize(List())
        graphXGraph = GraphXInterface.buildGraph(vertexRDD, edgeRDD)

        components.printPipeline()

        components.setOnReceiveNewMatches(apply)

        apply(components.blockAndMatch(attrCountRdd))
    }

    /*
      Apply function implementation for the AbstractDedup Class
     */
	  def apply(candidatePairs: RDD[(Row, Row)]):Unit = {
       val sampleTableRDD = scc.getCleanSample(sampleTableName)
    	 apply(candidatePairs.collect(), 
                sampleTableRDD)
	  }

     /* TODO fix!
      Apply function implementation for the AbstractDedup Class
     */	
  	def apply(candidatePairs: Array[(Row, Row)], 
                sampleTableRDD:RDD[Row]):Unit = {

    var resultRDD = sampleTableRDD.map(x =>
      (x(hashCol).asInstanceOf[String], x(attrCol).asInstanceOf[String]))

    // Add new edges to the graph
    val edges = candidatePairs.map( x => (x._1(0).asInstanceOf[String].hashCode.toLong,
      x._2(0).asInstanceOf[String].hashCode.toLong, 1.0) )

    graphXGraph = GraphXInterface.addEdges(graphXGraph, scc.getSparkContext().parallelize(edges))

    // Run the connected components algorithm
    def merge_vertices(v1: (String, Set[String]), v2: (String, Set[String])): (String, Set[String]) = {
      val winner:String = mergeStrategy.toLowerCase.trim match {
        case "mostconcise" => if (v1._1.length < v2._1.length) v1._1 else v2._1
        case "mostfrequent" => if (v1._2.size > v2._2.size) v1._1 else v2._1
        case _ => throw new RuntimeException("Invalid merge strategy: " + mergeStrategy)
      }
      (winner, v1._2 ++ v2._2)
    }
    
    val connectedPairs = GraphXInterface.connectedComponents(graphXGraph, merge_vertices)
    
    println("[Sampleclean] Merging values from "
      + connectedPairs.map(v => (v._2, 1)).reduceByKey(_ + _).filter(x => x._2 > 1).count
      + " components...")

    // Join with the old data to merge in new values.
    val flatPairs = connectedPairs.flatMap(vertex => vertex._2._2.map((_, vertex._2._1)))
    val newAttrs = flatPairs.asInstanceOf[RDD[(String, String)]].reduceByKey((x, y) => x)
    val joined = resultRDD.leftOuterJoin(newAttrs).mapValues(tuple => {
      tuple._2 match {
        case Some(newAttr) => {
          if (tuple._1 != newAttr) println(tuple._1 + " => " + newAttr)
          newAttr
        }
        case None => tuple._1
      }
    })
    
    scc.updateTableAttrValue(sampleTableName, attr, joined)
    this.onUpdateNotify()

  	}

}

object EntityResolution {

    def textAttributeAutomatic(scc:SampleCleanContext,
                               sampleName:String, 
                               attribute: String, 
                               threshold:Double=0.9,
                               weighting:Boolean =true):EntityResolution = {

        val algoPara = new AlgorithmParameters()
        algoPara.put("attr", attribute)
        algoPara.put("mergeStrategy", "mostFrequent")

        val similarity = new WeightedJaccardSimilarity(List(attribute), 
                                                   scc.getTableContext(sampleName),
                                                   WordTokenizer(), 
                                                   threshold)

        val join = new BroadcastJoin(scc.getSparkContext(), similarity, weighting)
        val matcher = new AllMatcher(scc, sampleName)
        val blockerMatcher = new BlockerMatcherSelfJoinSequence(scc,sampleName, join, List(matcher))
        return new EntityResolution(algoPara, scc, sampleName, blockerMatcher)
    }

    def textAttributeActiveLearning(scc:SampleCleanContext,
                               sampleName:String, 
                               attribute: String, 
                               threshold:Double=0.9,
                               weighting:Boolean =true):EntityResolution = {

        val algoPara = new AlgorithmParameters()
        algoPara.put("attr", attribute)
        algoPara.put("mergeStrategy", "mostFrequent")

        val cols = List("affiliation")
        val baseFeaturizer = new SimilarityFeaturizer(cols, 
                                                      scc.getTableContext(sampleName), 
                                                      List("Levenshtein", "JaroWinkler"))

        val alStrategy = new ActiveLearningStrategy(cols, baseFeaturizer)
        val matcher = new ActiveLeaningMatcher(scc, sampleName, alStrategy)
        val similarity = new WeightedJaccardSimilarity(List(attribute), 
                                                   scc.getTableContext(sampleName),
                                                   WordTokenizer(), 
                                                   threshold)

        val join = new BroadcastJoin(scc.getSparkContext(), similarity, weighting)
        val blockerMatcher = new BlockerMatcherSelfJoinSequence(scc,sampleName, join, List(matcher))
        return new EntityResolution(algoPara, scc, sampleName, blockerMatcher)
    }

}
