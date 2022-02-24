package utils

import Entity

internal class CacheMap<E: Entity>(private val maxSize: Int) {
    private val hashMap = hashMapOf<Int, E>()
    private val hashMapLazy = hashMapOf<Int, E>()
    private val queue = mutableSetOf<Int>()


    private fun set(key: Int, value: E, withReferences: Boolean) {
        if (withReferences)
            hashMap[key] = value
        else
            hashMapLazy[key] = value
        queue.add(key)
        if (queue.size > maxSize)
            remove(queue.first())
    }

    internal operator fun get(key: Int, withReferences: Boolean) = if (withReferences || hashMap.containsKey(key)) hashMap[key] else hashMapLazy[key]

    internal fun add(entity: E, withReferences: Boolean) = set(entity.id, entity, withReferences)

    internal fun addAll(list: List<E>, withReferences: Boolean) = list.takeLast(maxSize).forEach { set(it.id, it, withReferences) }

    internal fun remove(key: Int) {
        hashMap.remove(key)
        hashMapLazy.remove(key)
        queue.remove(key)
    }

    internal fun clear() {
        hashMap.clear()
        hashMapLazy.clear()
        queue.clear()
    }
}
