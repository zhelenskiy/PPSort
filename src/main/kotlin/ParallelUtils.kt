import ParallelIntArray.Companion.toParallelArray
import kotlinx.coroutines.*
import sun.misc.Unsafe
import java.io.Closeable
import java.lang.reflect.Field

class ParallelIntArray(val size: Int) : Closeable {

    private val unsafe: Unsafe = try {
        val field: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        field.get(null) as Unsafe
    } catch (e: Exception) {
        throw AssertionError(e)
    }

    private val pointer = unsafe.allocateMemory(sizeBytes.toLong() * size)

    operator fun set(index: Int, value: Int) = unsafe.putInt(pointer + index * sizeBytes, value)

    operator fun get(index: Int): Int = unsafe.getInt(pointer + index * sizeBytes)

    override fun close() = unsafe.freeMemory(pointer)

    companion object {
        private const val sizeBytes = Int.SIZE_BYTES
        suspend fun List<Int>.toParallelArray() = ParallelIntArray(size).apply {
            setEach { this@toParallelArray[it] }
        }
    }

    suspend inline fun setEach(crossinline generator: (Int) -> Int) {
        (0 until size).pfor { this@ParallelIntArray[it] = generator(it) }
    }

    fun asList(): MutableList<Int> = object : AbstractMutableList<Int>() {
        override val size: Int
            get() = this@ParallelIntArray.size

        override fun get(index: Int): Int = this@ParallelIntArray[index]
        override fun add(index: Int, element: Int) {
            TODO("Not yet implemented")
        }

        override fun removeAt(index: Int): Int {
            TODO("Not yet implemented")
        }

        override fun set(index: Int, element: Int): Int = get(index).also { this@ParallelIntArray[index] = element }
    }

    override fun toString(): String = asList().toString()
}

object Config {
    const val pforChunk = 100_000
    const val psumsChunk = 2_000
    const val pfilterChunk = 10
    const val psortChunk = 100_000
}

suspend inline fun IntRange.pfor(crossinline action: (Int) -> Unit) {
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
    crossinline predicate: (Int) -> Boolean
): ParallelIntArray {
    if (size < Config.pfilterChunk) return asList().filter(predicate).toParallelArray()
    val offsets = ParallelIntArray(size + 1)
    (0 until size).pfor { offsets[it] = if (predicate(this[it])) 1 else 0 }
    offsets[size] = 0
    offsets.psums()
    val res = ParallelIntArray(offsets[offsets.size - 1])
    (0 until size).pfor { if (offsets[it + 1] - offsets[it] == 1) res[offsets[it]] = this[it] }
    offsets.close()
    return res
}