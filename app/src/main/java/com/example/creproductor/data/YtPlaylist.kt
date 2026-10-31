package com.example.creproductor.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class YtPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tracks: List<YtTrack> = emptyList()
)

object YtPlaylistSerializer {
    fun encode(playlists: List<YtPlaylist>): String{
        val array = JSONArray()
        for (p in playlists){
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)

            val tracksArray = JSONArray()
            for (t in p.tracks){
                val tObj = JSONObject()
                tObj.put("url", t.url)
                tObj.put("title", t.title)
                tObj.put("artist", t.artist)
                tObj.put("durationSec", t.durationSec)
                tObj.put("thumbnail", t.thumbnail ?: "")
                tracksArray.put(tObj)
            }
            obj.put("tracks", tracksArray)
            array.put(obj)
        }
        return  array.toString()
    }

    fun decode (raw: String): List<YtPlaylist> {
        if (raw.isBlank()) return  emptyList()
        val list = mutableListOf<YtPlaylist>()
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()){
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.getString("name")

                val tracks = mutableListOf<YtTrack>()
                val tracksArray = obj.optJSONArray("tracks")
                if (tracksArray != null){
                    for (j in 0  until tracksArray.length()){
                        val tObj = tracksArray.getJSONObject(j)
                        tracks.add(
                            YtTrack(
                                url = tObj.getString("url"),
                                title = tObj.getString("title"),
                                artist = tObj.getString("artist"),
                                durationSec = tObj.optLong("durationSec", 0L),
                                thumbnail = tObj.optString("thumbnail").takeIf { it.isNotBlank()}
                            )
                        )
                    }
                }
                list.add(YtPlaylist(id, name, tracks))
            }
        }
        return list
    }
}