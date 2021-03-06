package sort

import utils.Config
import utils.ParallelIntArray
import utils.ParallelIntArray.Companion.toParallelArray
import utils.fork2join
import kotlinx.coroutines.runBlocking
import utils.pfilter
import utils.pfor
import kotlin.random.Random
import kotlin.random.nextInt

fun ParallelIntArray.sequentialSort(l: Int, r: Int) {
    if (r - l < 1)
        return
    val pivot = get(Random.nextInt(l until r))
    val less = this.asList().subList(l, r).filter { it < pivot }
    val equal = this.asList().subList(l, r).filter { it == pivot }
    val greater = this.asList().subList(l, r).filter { it > pivot }
    for (i in less.indices)
        this[i + l] = less[i]
    for (i in equal.indices)
        this[i + less.size + l] = equal[i]
    for (i in greater.indices)
        this[i + less.size + equal.size + l] = greater[i]
    sequentialSort(l, l + less.size)
    sequentialSort(r - greater.size, r)
}

fun main() {
    repeat(3) {
        val array = runBlocking {
            (1..10).shuffled().toParallelArray()
        }
        println(array.apply { sequentialSort(0, size) })
        println(array.apply { runBlocking { parallelSort() } })
        println(array.apply { runBlocking { parallelSort() } })
        println()
    }
}

suspend fun ParallelIntArray.parallelSort(l: Int = 0, r: Int = size) {
    if (r - l < Config.psortChunk) return sequentialSort(l, r)
    val pivot = get((l until r).random())
    val (left, middle, right) = fork2join(
        { pfilter(l, r) { it < pivot } },
        { pfilter(l, r) { it == pivot } },
        { pfilter(l, r) { it > pivot } },
    )
    val offset1 = left.size
    val offset2 = offset1 + middle.size
    fork2join(
        { (0 until left.size).pfor { this[l + it] = left[it] } },
        { (0 until middle.size).pfor { this[l + offset1 + it] = middle[it] } },
        { (0 until right.size).pfor { this[l + offset2 + it] = right[it] } },
    )
    left.close()
    right.close()
    middle.close()
    fork2join({ parallelSort(l, l + offset1) }, { parallelSort(l + offset2, r) })
}