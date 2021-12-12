package graph

import kotlin.math.pow

class CubeGraphLazyList(private val edgeSize: Int) : AbstractList<List<Int>>() {
    init {
        require(edgeSize > 0)
        require(edgeSize.toDouble() <= Int.MAX_VALUE.toDouble().pow(1.0 / 3))
    }

    private fun Int.toXYZ(): Triple<Int, Int, Int> {
        require(this in indices)
        return Triple((this / edgeSize / edgeSize) % edgeSize, (this / edgeSize) % edgeSize, this % edgeSize)
    }

    private fun Triple<Int, Int, Int>.toIndex(): Int = first * edgeSize * edgeSize + second * edgeSize + third

    override fun get(index: Int): List<Int> {
        val (x, y, z) = index.toXYZ()
        return buildList(6) {
            for ((newX, newY, newZ) in listOf(
                Triple(x - 1, y, z),
                Triple(x + 1, y, z),
                Triple(x, y - 1, z),
                Triple(x, y + 1, z),
                Triple(x, y, z - 1),
                Triple(x, y, z + 1),
            )) {
                if ((x != newX || y != newY || z != newZ) &&
                    newX in 0 until edgeSize &&
                    newY in 0 until edgeSize &&
                    newZ in 0 until edgeSize
                ) {
                    add(Triple(newX, newY, newZ).toIndex())
                }
            }
        }.toIntArray().asList()
    }

    override val size: Int
        get() = edgeSize * edgeSize * edgeSize
}