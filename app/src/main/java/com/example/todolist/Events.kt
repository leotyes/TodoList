package com.example.todolist

class DateVisibleEvent<T>(private val content: T) {
    private var handled = false

    fun main(): T? {
        return if (handled) {
            null
        } else {
            handled = true
            content
        }
    }

    fun reset() {
        handled = false
    }
}