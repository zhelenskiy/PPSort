package graph

class Graph(val adjacentList: List<List<Int>>) {
    val edgeList: Sequence<Pair<Int, Int>> = sequence {
        adjacentList.forEachIndexed { index, ints -> yieldAll(ints.asSequence().map { index to it }) }
    }
    val size: Int
        get() = adjacentList.size
}