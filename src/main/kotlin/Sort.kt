import ParallelIntArray.Companion.toParallelArray
import kotlinx.coroutines.runBlocking
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
        println(array.apply { runBlocking { parallelSort(1) } })
        println(array.apply { runBlocking { parallelSort(100) } })
        println()
    }
}

suspend fun ParallelIntArray.parallelSort(threshold: Int = 1000) {
    if (size < threshold) return sequentialSort(0, size)
    val pivot = get((0 until size).random())
    val (left, middle, right) = fork2join(
        { pfilter(threshold) { it < pivot } },
        { pfilter(threshold) { it == pivot } },
        { pfilter(threshold) { it > pivot } },
    )
    fork2join({ left.parallelSort(threshold) }, { right.parallelSort(threshold) })
    val offset1 = left.size
    val offset2 = offset1 + middle.size
    fork2join(
        { (0 until left.size).pfor { this[it] = left[it] } },
        { (0 until middle.size).pfor { this[offset1 + it] = middle[it] } },
        { (0 until right.size).pfor { this[offset2 + it] = right[it] } },
    )
    left.close()
    right.close()
    middle.close()
}