import graph.CubeGraphLazyList
import graph.Graph
import graph.parallelBfs
import graph.sequentialBfs
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private const val edgeSize = 500

private fun checkLogCorrectness(log: IntArray) {
    assertEquals(log.size, edgeSize * edgeSize * edgeSize)
    for (x in 0 until edgeSize)
        for (y in 0 until edgeSize)
            for (z in 0 until edgeSize)
                assertEquals(x + y + z, log[x * edgeSize * edgeSize + y * edgeSize + z])
}

@ExperimentalTime
fun main() { // run with -Xmx20G
    val lazyList = CubeGraphLazyList(edgeSize)
    val graph = Graph(lazyList.toList() /* make eager */)
    val log = IntArray(graph.size)
    val ratios = ArrayList<Double>()
    repeat(5) {
        System.gc()
        log.fill(0)
        val seq = measureTime {
            graph.sequentialBfs(0) { oldNode, newNode -> log[newNode] = log[oldNode] + 1 }
        }
        checkLogCorrectness(log)

        System.gc()
        log.fill(0)
        val par = measureTime {
            graph.parallelBfs(0) { oldNode, newNode -> log[newNode] = log[oldNode] + 1 }
        }
        checkLogCorrectness(log)
        ratios.add(seq / par)
        println("Seq: $seq, Par: $par, Ratio: ${ratios.last()}")
    }
    println()
    println("Average ratio: ${ratios.average()}")
}