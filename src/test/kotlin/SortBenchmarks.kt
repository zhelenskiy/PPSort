import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertIterableEquals
import sort.parallelSort
import sort.sequentialSort
import utils.ParallelIntArray
import utils.ParallelIntArray.Companion.toParallelArray
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

fun ParallelIntArray.validateAsSorted(source: IntArray) {
    assertIterableEquals(source.sorted(), asList())
}

@ExperimentalTime
fun bench(data: IntArray, action: ParallelIntArray.() -> Unit): Double {
    val mut = runBlocking { data.asList().toParallelArray() }
    val time = measureTime { mut.action() }
    mut.validateAsSorted(data)
    return time.toDouble(DurationUnit.MILLISECONDS)
}

@ExperimentalTime
fun parallelVersusSequential(size: Int, max: Int) {
    val n = 5
    val (seq, par) = (1..n)
        .map {
            val init = generate(size, max)
            val seq = bench(init) { sequentialSort(0, size) }
            println("seq #$it: ${seq}ms")
            System.gc()
            val par = bench(init) { runBlocking { parallelSort() } }
            println("par #$it: ${par}ms")
            System.gc()
            seq to par
        }
        .let { pairs -> pairs.map { it.first }.average() to pairs.map { it.second }.average() }
    println("Sequential:\t${seq}ms\nParallel:\t${par}ms\nRate:\t${seq / par}")
}

fun generate(size: Int, max: Int) = IntArray(size) { Random.nextInt(max) }

@ExperimentalTime
fun main() {
    parallelVersusSequential(50_000_000, 10_000_000)
}