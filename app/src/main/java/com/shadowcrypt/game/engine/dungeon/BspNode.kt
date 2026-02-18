package com.shadowcrypt.game.engine.dungeon

class BspNode(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    var left: BspNode? = null
    var right: BspNode? = null
    var room: Room? = null

    val isLeaf: Boolean get() = left == null && right == null

    fun allRooms(): List<Room> {
        if (isLeaf) return listOfNotNull(room)
        return (left?.allRooms() ?: emptyList()) + (right?.allRooms() ?: emptyList())
    }

    fun closestRoom(): Room? {
        if (isLeaf) return room
        return left?.closestRoom() ?: right?.closestRoom()
    }

    fun farthestRoom(): Room? {
        if (isLeaf) return room
        return right?.farthestRoom() ?: left?.farthestRoom()
    }
}
