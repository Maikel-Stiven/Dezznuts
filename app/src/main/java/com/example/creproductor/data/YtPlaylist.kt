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
        // Una entrada rota se salta sola: si se perdiera el resto, el siguiente guardado borraría tus listas.
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        for (i in 0 until array.length()) runCatching {
            val obj = array.getJSONObject(i)
            val tracksArray = obj.optJSONArray("tracks")
            val tracks = (0 until (tracksArray?.length() ?: 0)).mapNotNull { j ->
                runCatching {
                    val tObj = tracksArray!!.getJSONObject(j)
                    YtTrack(
                        url = tObj.getString("url"),
                        title = tObj.optString("title"),
                        artist = tObj.optString("artist"),
                        durationSec = tObj.optLong("durationSec", 0L),
                        thumbnail = tObj.optString("thumbnail").takeIf { it.isNotBlank() }
                    )
                }.getOrNull()
            }
            list.add(YtPlaylist(obj.optString("id").ifBlank { UUID.randomUUID().toString() }, obj.optString("name"), tracks))
        }
        return list
    }
}