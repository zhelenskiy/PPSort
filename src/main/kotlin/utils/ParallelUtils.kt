package utils

import utils.ParallelIntArray.Companion.toParallelArray
import kotlinx.coroutines.*

object Config {
    const val pforChunk = 10_000
    const val psumsChunk = 240_000
    const val pfilterChunk = 20_000
    const val psortChunk = 100_000
}

suspend inline fun IntRange.pfor(crossinline action: suspend (Int) -> Unit) {
    val range = this@pfor
    coroutineScope {
        (range step Config.pforChunk).map { startIndex ->
            async(Dispatchers.Default) {
                for (i in startIndex..minOf(startIndex + Config.pforChunk - 1, endInclusive)) {
                    action(i)
                }
            }
        }.awaitAll()
    }
}

suspend inline fun <T> fork2join(vararg fs: suspend () -> T) = coroutineScope {
    fs.map { f -> async(Dispatchers.Default) { f() } }.awaitAll()
}

suspend fun ParallelIntArray.psums() {
    if (size < Config.psumsChunk || size == 0) {
        return serialSums()
    }
    val n = run {
        var n = 1
        while (n < size) {
            n = n shl 1
        }
        n
    }
    val fastArray = ParallelIntArray(n).apply {
        setEach { if (it < this@psums.size) this@psums[it] else 0 }
    }
    with(fastArray) {
        var step = 2
        while (step != size) {
            (1..size.div(step)).pfor {
                val i = it * step - 1
                this[i] += get(i - (step shr 1))
            }
            step = step shl 1
        }
        this[size - 1] = 0
        while (step > 1) {
            (1..size.div(step)).pfor {
                val i = it * step - 1
                val sibling = i - (step shr 1)
                val oldValue = this[i]
                this[i] = this[i] + get(sibling)
                this[sibling] = oldValue
            }
            step = step shr 1
        }
    }
    (0 until size).pfor { this[it] = fastArray[it] }

}

fun main() {
    runBlocking {
        println(List(9) { it + 1 }.toParallelArray().apply { psums() })
        println(List(9) { it + 1 }.toParallelArray().let { it.pfilter { it % 2 > 0 } })
    }
}

fun ParallelIntArray.serialSums() {
    var x = 0
    for (i in 0 until size) {
        this[i] = x.also { x += this[i] }
    }
}

suspend inline fun ParallelIntArray.pfilter(
    l: Int = 0,
    r: Int = size,
    crossinline predicate: (Int) -> Boolean,
): ParallelIntArray {
    if (r - l < Config.pfilterChunk) return asList().subList(l, r).filter(predicate).toParallelArray()
    val offsets = ParallelIntArray(r - l + 1)
    (0 until r - l).pfor { offsets[it] = if (predicate(this[l + it])) 1 else 0 }
    offsets[r - l] = 0
    offsets.psums()
    val res = ParallelIntArray(offsets[offsets.size - 1])
    (0 until r - l).pfor { if (offsets[it + 1] - offsets[it] == 1) res[offsets[it]] = this[l + it] }
    offsets.close()
    return res
}