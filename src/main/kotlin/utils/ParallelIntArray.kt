package utils

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