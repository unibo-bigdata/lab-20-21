// 301-2 Spark warm-up

val rddCapra = sc.textFile("hdfs:/bigdata/dataset/capra/capra.txt")
val rddDC = sc.textFile("hdfs:/bigdata/dataset/divinacommedia")

rddCapra.count()
rddCapra.collect()

val rddCapraWords1 = rddCapra.map( x => x.split(" ") )
rddCapraWords1.collect()
val rddCapraWords2 = rddCapra.flatMap( x => x.split(" ") )
rddCapraWords2.collect()

val rddDcWords1 = rddDC.map( x => x.split(" ") )
rddDcWords1.collect()
val rddDcWords2 = rddDC.flatMap( x => x.split(" ") )
rddDcWords2.collect()

// 301-3 From MapReduce to Spark > Word count

val rddMap = rddCapraWords2.map( x => (x,1))
rddMap.take(5)

val rddReduce = rddMap.reduceByKey((x,y) => x + y)
rddReduce.collect()

val rddMap = rddDcWords2.map( x => (x,1))
rddMap.take(5)

val rddReduce = rddMap.reduceByKey((x,y) => x + y)
rddReduce.collect()

// 301-3 From MapReduce to Spark > Word length count

val rddMap = rddDcWords2.map( x => (x.length,1))
rddMap.collect()

val rddReduce = rddMap.reduceByKey((x,y) => x + y)
rddReduce.collect()

// 301-3 From MapReduce to Spark > Average word length by initial

val rddMap = rddDcWords2.
  filter( _.length>0 ).
  map( x => (x.substring(0,1), x.length))
rddMap.collect()

val rddReduce = rddMap.aggregateByKey((0.0,0.0))((a,v)=>(a._1+v,a._2+1), (a1,a2)=>(a1._1+a2._1,a1._2+a2._2))
rddReduce.collect()

val rddFinal = rddReduce.mapValues(v => v._1/v._2)
rddFinal.collect()

// 301-3 From MapReduce to Spark > Inverted index

val rddMap = rddDcWords2.zipWithIndex()
rddMap.collect()

val rddGroup = rddMap.groupByKey()
rddGroup.collect()

// 301-3 From MapReduce to Spark > Sort an RDD by key

rdd.sortByKey()

// 301-3 From MapReduce to Spark > Sort an RDD by value

rdd.map({case(k,v) => (v,k)}).sortByKey()

// 301-3 From MapReduce to Spark > Visualize the RDD lineage

val rddL = sc.
   textFile("hdfs:/bigdata/dataset/capra/capra.txt").
   flatMap( x => x.split(" ") ).
   map(x => (x,1)).
   reduceByKey((x,y)=>x+y)
rddL.toDebugString