package sampleclean

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import sampleclean.api.SampleCleanContext;
import sampleclean.api.SampleCleanAQP;
import sampleclean.parse.SampleCleanParser;
import org.apache.spark.sql.hive.HiveContext

import sampleclean.clean.deduplication.join._
import sampleclean.clean.deduplication.blocker._
import sampleclean.clean.deduplication.matcher._
import sampleclean.clean.deduplication._
import sampleclean.clean.deduplication.EntityResolution._

import sampleclean.activeml._
import sampleclean.api.{SampleCleanAQP, SampleCleanContext, SampleCleanQuery}
import sampleclean.clean.algorithm.{AlgorithmParameters, SampleCleanAlgorithm, SampleCleanPipeline}
import sampleclean.clean.deduplication.{ActiveLearningStrategy, CrowdsourcingStrategy, _}
import sampleclean.crowd.{CrowdConfiguration, CrowdTaskConfiguration}
import sampleclean.clean.featurize.SimilarityFeaturizer
import sampleclean.clean.featurize.AnnotatedSimilarityFeaturizer._
import sampleclean.clean.featurize.LearningSimilarityFeaturizer
import sampleclean.clean.featurize.Tokenizer._
import sampleclean.clean.extraction.LearningSplitExtraction

import sampleclean.clean.deduplication.EntityResolution._
import sampleclean.eval.Evaluator

/**
* This object provides the main driver for the SampleClean
* application. We execute commands read from the command 
* line.
*/
object Deduptest {

  /**
   * Main function
   */
  def main(args: Array[String]) {

    val conf = new SparkConf();
    conf.setAppName("SampleClean Spark Driver");
    conf.setMaster("local[4]");
    conf.set("spark.executor.memory", "4g");

    val sc = new SparkContext(conf);
    val scc = new SampleCleanContext(sc);
    val saqp = new SampleCleanAQP();

    //var algorithm1 = EntityResolution.textAttributeAutomatic(scc, "paper_aff_sample", "affiliation", 0.9, false)
    //var algorithm2 = EntityResolution.textAttributeAutomatic(scc, "paper_aff_sample", "affiliation", 0.9, true)
    //val e = new Evaluator(scc, List(algorithm1,algorithm2))
    //e.addUnaryConstraint("81eb205e-6fe2-48ce-962e-394ffaedbe74", "affiliation", "Stanford University")
    //e.addBinaryConstraint("81eb205e-6fe2-48ce-962e-394ffaedbe74","41fb55cb-dd6a-4c43-bc0d-3e619ca07cc2" ,"affiliation", true)
    //e.evaluate("paper_aff_sample")

    
    println("Test 1. Test Automated Entity Resolution")
    var algorithm = EntityResolution.textAttributeAutomatic(scc, "paper_aff_sample", "affiliation", 0.9, false)
    algorithm.exec()

    println("Test 2. Test Active Learning Entity Resolution")
    algorithm = EntityResolution.textAttributeActiveLearning(scc, "paper_aff_sample", "affiliation", 0.9, false)
    algorithm.exec()
    
    
  }
  
}
