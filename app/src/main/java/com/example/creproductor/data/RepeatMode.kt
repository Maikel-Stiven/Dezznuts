package com.example.creproductor.data

enum class RepeatMode {
    OFF,
    ALL,
    ONE,
    STOP_AFTER_CURRENT
}

fun RepeatMode.next(): RepeatMode = when (this){
    RepeatMode.OFF -> RepeatMode.ALL
    RepeatMode.ALL -> RepeatMode.ONE
    RepeatMode.ONE -> RepeatMode.STOP_AFTER_CURRENT
    RepeatMode.STOP_AFTER_CURRENT -> RepeatMode.OFF
}