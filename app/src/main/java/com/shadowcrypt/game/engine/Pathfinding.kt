package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.DungeonFloor
import com.shadowcrypt.game.model.Position
import java.util.PriorityQueue

object Pathfinding {

    fun findPath(
        start: Position,
        goal: Position,
        dungeon: DungeonFloor,
        blocked: Set<Position> = emptySet(),
        maxDistance: Int = 30,
        tileCost: ((Position) -> Int)? = null
    ): List<Position>? {
        if (!dungeon.isWalkable(goal) && goal !in blocked) return null
        if (start == goal) return emptyList()

        data class Node(val pos: Position, val f: Int) : Comparable<Node> {
            override fun compareTo(other: Node) = f.compareTo(other.f)
        }

        val openSet = PriorityQueue<Node>()
        val cameFrom = mutableMapOf<Position, Position>()
        val gScore = mutableMapOf(start to 0)

        openSet.add(Node(start, start.distanceTo(goal)))

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()?.pos ?: break

            if (current == goal) {
                val path = mutableListOf<Position>()
                var node = goal
                while (node != start) {
                    path.add(node)
                    node = cameFrom[node] ?: break
                }
                return path.reversed()
            }

            val currentG = gScore[current] ?: continue
            if (currentG > maxDistance) continue

            for (neighbor in current.cardinalNeighbors()) {
                if (!dungeon.isWalkable(neighbor)) continue
                if (neighbor in blocked && neighbor != goal) continue

                val moveCost = tileCost?.invoke(neighbor) ?: 1
                val tentativeG = currentG + moveCost
                if (tentativeG < (gScore[neighbor] ?: Int.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeG
                    val f = tentativeG + neighbor.distanceTo(goal)
                    openSet.add(Node(neighbor, f))
                }
            }
        }

        return null
    }
}
