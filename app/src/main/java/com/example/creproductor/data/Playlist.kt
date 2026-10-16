package com.example.creproductor.data

import java.util.UUID

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val coverUri: String? = null,
    val songIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
