package com.gps.dashboard.data.buffer

class RingBuffer<T>(private val capacity: Int) {
    private val buffer = arrayOfNulls<Any>(capacity)
    private var head = 0
    private var count = 0

    val size: Int get() = count

    fun add(element: T) {
        buffer[head] = element
        head = (head + 1) % capacity
        if (count < capacity) count++
    }

    fun isEmpty(): Boolean = count == 0

    fun isFull(): Boolean = count == capacity

    @Suppress("UNCHECKED_CAST")
    fun toList(): List<T> {
        if (count == 0) return emptyList()
        val start = if (count < capacity) 0 else head
        return List(count) { i ->
            buffer[(start + i) % capacity] as T
        }
    }

    fun clear() {
        head = 0
        count = 0
    }
}
