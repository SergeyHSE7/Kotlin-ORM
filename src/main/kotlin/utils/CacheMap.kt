package utils

import Entity

class CacheMap<E: Entity>(private val maxSize: Int) {
    private val hashMap = hashMapOf<Int, E>()
    private val queue = mutableSetOf<Int>()

    operator fun set(key: Int, value: E) {
        hashMap[key] = value
        queue.add(key)
        if (queue.size > maxSize)
            remove(queue.first())
    }

    operator fun get(key: Int) = hashMap[key]

    operator fun plusAssign(entity: E) = set(entity.id, entity)

    fun addAll(list: List<E>) = list.takeLast(maxSize).forEach { set(it.id, it) }

    fun remove(key: Int) {
        hashMap.remove(key)
        queue.remove(key)
    }

    fun clear() {
        hashMap.clear()
        queue.clear()
    }
}
