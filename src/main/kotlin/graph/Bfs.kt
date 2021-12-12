package graph

import kotlinx.coroutines.runBlocking
import utils.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

inline fun Graph.sequentialBfs(
    startNode: Int,
    crossinline updater: (oldNode: Int, newNode: Int) -> Unit
) {
    require(startNode in 0 until size)
    val used = BooleanArray(size).apply { set(startNode, true) }
    val q = ArrayDeque<Int>().apply { add(startNode) }
    while (q.isNotEmpty()) {
        val cur = q.removeFirst()
        for (neighbour in adjacentList[cur]) {
            if (!used[neighbour]) {
                used[neighbour] = true
                updater(cur, neighbour)
                q.add(neighbour)
            }
        }
    }
}

inline fun Graph.parallelBfs(startNode: Int, crossinline updater: (oldNode: Int, newNode: Int) -> Unit) =
    runBlocking {
        require(startNode in 0 until size)
        val used: Array<AtomicBoolean> = arrayOfNulls<AtomicBoolean>(size).apply {
            this[0] = AtomicBoolean(true)
            (1 until size).pfor { this[it] = AtomicBoolean(false) }
        } as Array<AtomicBoolean>


        var frontier = ParallelIntArray(1).apply { set(0, startNode) }
        while (frontier.size != 0) {
            val offsets = ParallelIntArray(frontier.size).apply {
                setEach { adjacentList[frontier[it]].size }
                psums()
            }
            val frontierHolder =
                ParallelIntArray(offsets.asList().last() + adjacentList[frontier.asList().last()].size).apply {
                    setEach { -1 }
                }
            (0 until frontier.size).pfor { ind ->
                val v = frontier[ind]
                for (i in adjacentList[v].indices) {
                    val u = adjacentList[v][i]
                    if (used[u].compareAndSet(false, true)) {
                        frontierHolder[offsets[ind] + i] = u
                        updater(v, u)
                    }
                }
            }
            frontier.close()
            offsets.close()
            frontier = frontierHolder.pfilter { it >= 0 }
            frontierHolder.close()
        }
        frontier.close()
    }