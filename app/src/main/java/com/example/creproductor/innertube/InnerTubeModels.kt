package com.example.creproductor.innertube


data class YtOnlineTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationText: String = ""
)

data class YtSection(
    val title: String,
    val tracks: List<YtOnlineTrack>
)