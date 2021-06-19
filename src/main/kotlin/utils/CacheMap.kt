package utils

import Entity

class CacheMap<E: Entity>(private val maxSize: Int) {
    private val hashMap = hashMapOf<Int, E>()
    private val hashMapLazy = hashMapOf<Int, E>()
    private val queue = mutableSetOf<Int>()


    fun set(key: Int, value: E, withReferences: Boolean) {
        if (withReferences)
            hashMap[key] = value
        else
            hashMapLazy[key] = value
        queue.add(key)
        if (queue.size > maxSize)
            remove(queue.first())
    }

    operator fun get(key: Int, withReferences: Boolean = true) = if (withReferences || hashMap.containsKey(key)) hashMap[key] else hashMapLazy[key]

    fun add(entity: E, withReferences: Boolean = true) = set(entity.id, entity, withReferences)

    fun addAll(list: List<E>, withReferences: Boolean = true) = list.takeLast(maxSize).forEach { set(it.id, it, withReferences) }

    fun remove(key: Int) {
        hashMap.remove(key)
        hashMapLazy.remove(key)
        queue.remove(key)
    }

    fun clear() {
        hashMap.clear()
        hashMapLazy.clear()
        queue.clear()
    }
}
