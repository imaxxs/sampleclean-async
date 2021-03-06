package sampleclean.clean.featurize
import java.util.StringTokenizer
import scala.collection.mutable.ArrayBuffer
import org.apache.spark.sql.{SchemaRDD, Row}

/**
 * This is a tokenizer super-class.
 */
abstract class Tokenizer{
  
  def tokenize(row: Row, cols:List[Int]): Seq[String] = {

      var stringA = ""
      var tokSeq: Seq[String] = Seq()
      for (col <- cols){
        tokSeq = tokSeq ++ tokenSet(row.getString(col))
        //stringA = stringA + " " + row(col)
      }
      //return tokenSet(stringA.trim)
      tokSeq
    }
  
  def tokenSet(text: String): Seq[String]
}

object Tokenizer {
/**
 * This class tokenizes a string based on user-specified delimiters.
 * @param delimiters string of delimiters to be used for splitting. Accepts regex expressions.
 */
case class DelimiterTokenizer(delimiters: String = ".,?!\t ") extends Tokenizer {

  def tokenSet(str: String) = {
    val st = new StringTokenizer(str, delimiters)
    val tokens = new ArrayBuffer[String]
    while (st.hasMoreTokens()) {
      tokens += st.nextToken()
    }
    tokens.toList
  }
}

/**
 * This class tokenizes a string based on words.
 */
case class WordTokenizer() extends Tokenizer {
  def tokenSet(str: String) = str.split("\\W+").toList.filter(_!="")
}

/**
 * 
 */
case class NullTokenizer() extends Tokenizer {
  def tokenSet(str: String) = List(str).toSeq.filter(_!="")
}

/**
 * This class tokenizes a string based on white spaces.
 */
case class WhiteSpaceTokenizer() extends Tokenizer {
  def tokenSet(str: String) = str.split("\\s+").toSeq.filter(_!="")
}

/**
 * This class tokenizes a string based on grams.
 * @param gramSize size of gram.
 */
case class GramTokenizer(gramSize: Int) extends Tokenizer {
  def tokenSet(str: String) =  str.sliding(gramSize).toSeq
}

/**
 * This class tokenizes a string based on white space punctuation.
 */
case class WhiteSpacePunctuationTokenizer() extends Tokenizer {
  def tokenSet(str: String) =  str.trim.split("([.,!?:;'\"-]|\\s)+").toSeq.filter(_!="")
}
}