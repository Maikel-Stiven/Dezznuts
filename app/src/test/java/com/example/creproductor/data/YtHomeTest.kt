package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class YtHomeTest {
    // Esqueleto reducido de FEmusic_home: chips de ánimo, un estante de canciones y otro de álbumes.
    private val sample = """{"contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{"sectionListRenderer":{
      "header":{"chipCloudRenderer":{"chips":[
        {"chipCloudChipRenderer":{"text":{"runs":[{"text":"Relax"}]},"navigationEndpoint":{"browseEndpoint":{"params":"ggMPOg"}}}},
        {"chipCloudChipRenderer":{"text":{"runs":[{"text":"Sin params"}]},"navigationEndpoint":{}}}]}},
      "contents":[
       {"musicCarouselShelfRenderer":{"header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Selección rápida"}]}}},"contents":[
         {"musicResponsiveListItemRenderer":{"playlistItemData":{"videoId":"abc123"},
            "flexColumns":[{"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Canción uno"}]}}},
                           {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Artista uno"}]}}}]}},
         {"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"Dos"}]},"subtitle":{"runs":[{"text":"Canción"},{"text":" • "},{"text":"Artista dos"}]},
            "thumbnailRenderer":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"https://i/t.jpg"}]}}},
            "navigationEndpoint":{"watchEndpoint":{"videoId":"def456"}}}}]}},
       {"musicCarouselShelfRenderer":{"header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Álbumes para ti"}]}}},"contents":[
         {"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"Disco"}]},"subtitle":{"runs":[{"text":"Álbum • Banda"}]},
            "navigationEndpoint":{"browseEndpoint":{"browseId":"MPREb_xyz"}}}},
         {"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"Artista"}]},"navigationEndpoint":{"browseEndpoint":{"browseId":"UCxyz"}}}}]}},
       {"musicCarouselShelfRenderer":{"contents":[]}}
      ]}}}}]}}}"""

    @Test fun leeEstadosDeAnimoYDescartaLosQueNoTienenParams() {
        val f = YtHome.parse(sample)
        assertEquals(listOf(YtMood("Relax", "ggMPOg")), f.moods)
    }

    @Test fun leeLosEstantesConCancionesYAlbumes() {
        val f = YtHome.parse(sample)
        assertEquals(listOf("Selección rápida", "Álbumes para ti"), f.shelves.map { it.title })
        val songs = f.shelves[0].items
        assertEquals("https://music.youtube.com/watch?v=abc123", songs[0].track?.url)
        assertEquals("Artista uno", songs[0].track?.artist)
        assertEquals("Artista dos", songs[1].track?.artist)   // el "Canción" del subtítulo no es el artista
        assertEquals("https://i/t.jpg", songs[1].thumbnail)
        val albums = f.shelves[1].items
        assertEquals(1, albums.size)                            // un perfil de artista (UC…) no se abre como lista
        assertNull(albums[0].track); assertEquals("MPREb_xyz", albums[0].browseId)
    }

    @Test fun jsonRotoDaInicioVacio() { val f = YtHome.parse("<html>"); assertNotNull(f); assertEquals(0, f.shelves.size) }

    @Test fun artistaDelSubtitulo() {
        assertEquals("Banda", YtHome.artistOf("Álbum • Banda • 2020"))
        assertEquals("", YtHome.artistOf("Canción"))
    }
}
