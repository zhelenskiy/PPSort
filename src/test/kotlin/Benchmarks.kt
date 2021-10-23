import ParallelIntArray.Companion.toParallelArray
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
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
            println(seq)
            val par = bench(init) { runBlocking { parallelSort(100_000) } }
            println(par)
            seq to par
        }
        .let { pairs -> pairs.map { it.first }.average() to pairs.map { it.second }.average() }
    println("Sequential: ${seq}ms\nParallel: ${par}ms\n")
}

fun generate(size: Int, max: Int) = IntArray(size) { Random.nextInt(max) }

@ExperimentalTime
class Benchmarks {
    @Test
    fun `different`() {
        parallelVersusSequential(10_000_000, 10000)
    }
}