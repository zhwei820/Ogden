package com.example.ogdenkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResourcePacksTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun zipOf(vararg entries: Pair<String, String>): File {
        val file = tmp.newFile()
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, body) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun pack(id: String = "words-a", type: PackType = PackType.Words, version: Int = 1) =
        RemotePack(id, type, "t", "", version, 0, "", "/packs/$id.zip")

    @Test
    fun statusComparesRemoteWithInstalledVersion() {
        assertEquals(PackStatus.NotInstalled, packStatus(1, null))
        assertEquals(PackStatus.Installed, packStatus(2, 2))
        assertEquals(PackStatus.UpdateAvailable, packStatus(3, 2))
    }

    @Test
    fun manifestSkipsUnknownTypeAndUnsafeId() {
        val json = """{"packs": [
            {"id": "words-a", "type": "words", "title": "A", "version": 1, "size": 10, "sha256": "AB", "url": "/a.zip"},
            {"id": "x", "type": "video", "title": "X", "version": 1, "size": 10, "sha256": "ab", "url": "/x.zip"},
            {"id": "../evil", "type": "voice", "title": "E", "version": 1, "size": 10, "sha256": "ab", "url": "/e.zip"},
            {"id": "no-version", "type": "voice", "title": "N", "size": 10, "sha256": "ab", "url": "/n.zip"}
        ]}"""
        val packs = parseManifest(json)
        assertEquals(listOf("words-a"), packs.map { it.id })
        assertEquals("ab", packs.single().sha256)
    }

    @Test
    fun sha256MismatchIsRejected() {
        val digest = MessageDigest.getInstance("SHA-256").digest("hello".toByteArray())
        verifySha256(digest, "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")
        try {
            verifySha256(digest, "0".repeat(64))
            fail("错误的 sha256 应被拒绝")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("校验失败"))
        }
    }

    @Test
    fun zipEntryEscapingDestinationIsRejected() {
        val dest = tmp.newFolder("dest")
        try {
            unzipTo(zipOf("../escaped.txt" to "x"), dest)
            fail("越界路径应被拒绝")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("越界"))
        }
        assertFalse(File(dest.parentFile, "escaped.txt").exists())
    }

    @Test
    fun installedFilesAreFoundAndSurviveReloadUntilRemoved() {
        val root = tmp.newFolder("packs")
        PackStore(root).install(pack(), zipOf("words.json" to "[]", "audio/us/penguin.mp3" to "mp3"))

        val reloaded = PackStore(root)
        assertEquals(1, reloaded.installedVersion("words-a"))
        assertEquals("mp3", reloaded.findFile("audio/us/penguin.mp3")!!.readText())
        assertEquals(listOf("[]"), reloaded.readWordPackJsons())

        reloaded.remove("words-a")
        assertNull(reloaded.installedVersion("words-a"))
        assertNull(reloaded.findFile("audio/us/penguin.mp3"))
        assertFalse(File(root, "words-a").exists())
    }

    @Test
    fun wordPackWithoutWordsJsonIsNotInstalled() {
        val root = tmp.newFolder("packs")
        val store = PackStore(root)
        try {
            store.install(pack(), zipOf("audio/us/a.mp3" to "mp3"))
            fail("缺 words.json 的词包应安装失败")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("words.json"))
        }
        assertNull(store.installedVersion("words-a"))
        assertEquals(listOf("installed.json").filter { File(root, it).exists() }, root.list()!!.toList())
    }

    @Test
    fun voicePackFilesDoNotCountAsWords() {
        val root = tmp.newFolder("packs")
        val store = PackStore(root)
        store.install(pack("voice-a", PackType.Voice), zipOf("tts/en-US/abc.mp3" to "mp3"))
        assertTrue(store.readWordPackJsons().isEmpty())
        assertEquals("mp3", store.findFile("tts/en-US/abc.mp3")!!.readText())
    }
}
