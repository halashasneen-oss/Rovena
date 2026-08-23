package com.rovena.garage.domain.usecase

import java.io.File

/**
 * Zip Slip / path traversal defense for `.rovena` backup extraction. A
 * malicious archive can name an entry "../../../etc/passwd" (or an absolute
 * path, or a Windows-style "..\..\evil.txt") hoping `File(root, entry.name)`
 * will write outside the intended extraction directory. This validator is
 * pure JVM (no Android dependency) so it's fully unit-testable, and is used
 * as a hard gate before any bytes from an archive entry are written to disk.
 */
object BackupPathValidator {

    /**
     * Name-level check: rejects anything that looks like an escape attempt
     * without touching the filesystem. Cheap first line of defense before
     * the more expensive canonical-path check.
     */
    fun isSafeEntryName(entryName: String): Boolean {
        if (entryName.isBlank()) return false
        val normalized = entryName.replace('\\', '/')
        if (normalized.startsWith("/")) return false
        if (normalized.contains(":")) return false // Windows drive letters ("C:/...") or alternate data streams
        val segments = normalized.split("/")
        if (segments.any { it == ".." }) return false
        return true
    }

    /**
     * Resolves [entryName] against [root] and verifies - via canonical path
     * containment, which also defeats symlink tricks a name-only check would
     * miss - that the result cannot land outside [root]. Returns null when
     * the entry is unsafe by name or by resolved location; the caller must
     * treat that as "reject this backup", never crash or silently skip.
     */
    fun resolveSafeEntry(root: File, entryName: String): File? {
        if (!isSafeEntryName(entryName)) return null
        val canonicalRoot = root.canonicalFile
        val candidate = File(root, entryName)
        val canonicalCandidate = candidate.canonicalFile
        val withinRoot = canonicalCandidate == canonicalRoot ||
            canonicalCandidate.path.startsWith(canonicalRoot.path + File.separator)
        return if (withinRoot) candidate else null
    }
}
