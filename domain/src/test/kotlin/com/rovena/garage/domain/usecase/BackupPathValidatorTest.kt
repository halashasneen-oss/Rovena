package com.rovena.garage.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BackupPathValidatorTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `plain relative entry name is safe`() {
        assertTrue(BackupPathValidator.isSafeEntryName("files/photos/car.jpg"))
        assertTrue(BackupPathValidator.isSafeEntryName("manifest.json"))
        assertTrue(BackupPathValidator.isSafeEntryName("files/documents/"))
    }

    @Test
    fun `parent directory traversal is unsafe`() {
        assertTrue(!BackupPathValidator.isSafeEntryName("../../evil.txt"))
        assertTrue(!BackupPathValidator.isSafeEntryName("../../../database.db"))
        assertTrue(!BackupPathValidator.isSafeEntryName("files/../../evil.txt"))
    }

    @Test
    fun `windows style traversal is unsafe`() {
        assertTrue(!BackupPathValidator.isSafeEntryName("..\\..\\evil.txt"))
        assertTrue(!BackupPathValidator.isSafeEntryName("files\\..\\..\\evil.txt"))
    }

    @Test
    fun `absolute paths are unsafe`() {
        assertTrue(!BackupPathValidator.isSafeEntryName("/absolute/path/file"))
        assertTrue(!BackupPathValidator.isSafeEntryName("C:/Windows/System32/evil.dll"))
        assertTrue(!BackupPathValidator.isSafeEntryName("C:\\Windows\\evil.dll"))
    }

    @Test
    fun `blank entry name is unsafe`() {
        assertTrue(!BackupPathValidator.isSafeEntryName(""))
        assertTrue(!BackupPathValidator.isSafeEntryName("   "))
    }

    @Test
    fun `resolveSafeEntry accepts a normal nested entry within root`() {
        val resolved = BackupPathValidator.resolveSafeEntry(tempDir, "files/photos/car.jpg")
        assertNotNull(resolved)
        assertTrue(resolved!!.canonicalFile.path.startsWith(tempDir.canonicalFile.path))
    }

    @Test
    fun `resolveSafeEntry rejects traversal attempts`() {
        assertNull(BackupPathValidator.resolveSafeEntry(tempDir, "../../evil.txt"))
        assertNull(BackupPathValidator.resolveSafeEntry(tempDir, "../../../database.db"))
        assertNull(BackupPathValidator.resolveSafeEntry(tempDir, "..\\..\\evil.txt"))
        assertNull(BackupPathValidator.resolveSafeEntry(tempDir, "/absolute/path/file"))
    }

    @Test
    fun `resolveSafeEntry rejects an entry that escapes via a deceptive relative prefix`() {
        // A sibling directory that merely shares a name prefix with root must not be
        // treated as "inside root" by a naive startsWith(root.path) string check.
        val decoyRoot = File(tempDir, "restore_abc").apply { mkdirs() }
        val decoySibling = File(tempDir, "restore_abc_evil")
        assertEquals(false, decoySibling.canonicalFile.path.startsWith(decoyRoot.canonicalFile.path + File.separator))
        assertNull(BackupPathValidator.resolveSafeEntry(decoyRoot, "../restore_abc_evil/evil.txt"))
    }
}
