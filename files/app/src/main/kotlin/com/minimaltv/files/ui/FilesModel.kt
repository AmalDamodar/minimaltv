package com.minimaltv.files.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** What one tile or row stands for. [where] is the folder a search hit lives in, relative to the searched folder. */
internal data class Entry(val file: File, val isDir: Boolean, val size: Long, val modified: Long, val where: String? = null)

internal enum class SortBy { NAME, SIZE, DATE, TYPE }

/** The view choices (tiles or list, sort order, hidden files) are remembered between runs. */
internal class ViewPrefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("view", Context.MODE_PRIVATE)
    var grid: Boolean
        get() = p.getBoolean("grid", true)
        set(v) = p.edit().putBoolean("grid", v).apply()
    var sort: SortBy
        get() = SortBy.entries.getOrElse(p.getInt("sort", 0)) { SortBy.NAME }
        set(v) = p.edit().putInt("sort", v.ordinal).apply()
    var descending: Boolean
        get() = p.getBoolean("desc", false)
        set(v) = p.edit().putBoolean("desc", v).apply()
    var hidden: Boolean
        get() = p.getBoolean("hidden", false)
        set(v) = p.edit().putBoolean("hidden", v).apply()
}

internal fun internalRoot(): File = Environment.getExternalStorageDirectory()

/** Other volumes such as USB drives; the built-in storage is separate. */
internal fun removableVolumes(): List<File> =
    File("/storage").listFiles()?.filter { it.isDirectory && it.name != "emulated" && it.name != "self" && it.canRead() }.orEmpty()

internal fun hasStorageAccess(ctx: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
    else ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

private fun entryOf(f: File, where: String? = null) =
    Entry(f, f.isDirectory, if (f.isFile) f.length() else 0L, f.lastModified(), where)

/** Folders first, then files, in the chosen order. Folders have no size, so they stay by name when sorting by size. */
internal fun sorted(entries: List<Entry>, by: SortBy, descending: Boolean): List<Entry> {
    val byName = compareBy<Entry> { it.file.name.lowercase() }
    val key: Comparator<Entry> = when (by) {
        SortBy.NAME -> byName
        SortBy.SIZE -> compareBy<Entry> { it.size }.thenComparing(byName)
        SortBy.DATE -> compareBy<Entry> { it.modified }.thenComparing(byName)
        SortBy.TYPE -> compareBy<Entry> { it.file.extension.lowercase() }.thenComparing(byName)
    }
    val (dirs, files) = entries.partition { it.isDir }
    val dirKey = if (by == SortBy.SIZE) byName else key
    return dirs.sortedWith(if (descending && by != SortBy.SIZE) dirKey.reversed() else dirKey) +
        files.sortedWith(if (descending) key.reversed() else key)
}

internal fun listDir(dir: File, by: SortBy, descending: Boolean, hidden: Boolean): List<Entry>? {
    val files = dir.listFiles() ?: return null
    return sorted(files.filter { hidden || !it.name.startsWith(".") }.map { entryOf(it) }, by, descending)
}

/** Everything under [root] whose name contains [query] (at most [limit] hits). */
internal fun searchTree(root: File, query: String, by: SortBy, descending: Boolean, hidden: Boolean, limit: Int = 400): List<Entry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val hits = ArrayList<Entry>()
    root.walkTopDown()
        .onEnter { hidden || it == root || !it.name.startsWith(".") }
        .filter { it != root && (hidden || !it.name.startsWith(".")) && it.name.lowercase().contains(q) }
        .take(limit)
        .forEach { f ->
            val rel = f.parentFile?.relativeToOrNull(root)?.path.orEmpty()
            hits += entryOf(f, rel)
        }
    return sorted(hits, by, descending)
}

internal enum class Kind { FOLDER, VIDEO, AUDIO, IMAGE, DOCUMENT, APK, ARCHIVE, OTHER }

internal fun kindOf(e: Entry): Kind {
    if (e.isDir) return Kind.FOLDER
    return when (e.file.extension.lowercase()) {
        "mp4", "mkv", "avi", "mov", "webm", "ts", "m4v", "3gp", "mpg", "mpeg", "wmv", "flv" -> Kind.VIDEO
        "mp3", "m4a", "flac", "wav", "ogg", "aac", "opus", "wma" -> Kind.AUDIO
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> Kind.IMAGE
        "pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "srt", "md", "epub" -> Kind.DOCUMENT
        "apk" -> Kind.APK
        "zip", "rar", "7z", "tar", "gz" -> Kind.ARCHIVE
        else -> Kind.OTHER
    }
}

internal fun mimeOf(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "apk" -> "application/vnd.android.package-archive"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}

/** Deletes a file or a folder with its contents, then tells the media database so it does not keep a ghost entry. */
internal fun deletePath(ctx: Context, target: File): Boolean {
    val paths = pathsIn(target) + target.absolutePath
    val ok = if (target.isDirectory) target.deleteRecursively() else target.delete()
    MediaScannerConnection.scanFile(ctx, paths.toTypedArray(), null, null)
    return ok
}

/** A copy or move waiting for a destination folder. */
internal class Clip(val file: File, val move: Boolean)

/** "name (1).ext" style name that does not exist in [dir] yet. */
internal fun uniqueTarget(dir: File, name: String): File {
    var target = File(dir, name)
    if (!target.exists()) return target
    val base = name.substringBeforeLast('.', name)
    val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
    var n = 1
    while (target.exists() && n < 10_000) { target = File(dir, "$base ($n)$ext"); n++ }
    return target
}

internal fun treeSize(f: File): Long = if (f.isFile) f.length() else f.walkTopDown().filter { it.isFile }.sumOf { it.length() }

internal fun itemCount(f: File): Int = f.listFiles()?.size ?: 0

/** Copies a file or folder, reporting bytes as they go; stops early when [cancel] is set. */
private fun copyTree(src: File, dst: File, cancel: AtomicBoolean, onBytes: (Long) -> Unit) {
    if (src.isDirectory) {
        dst.mkdirs()
        src.listFiles()?.forEach { if (!cancel.get()) copyTree(it, File(dst, it.name), cancel, onBytes) }
    } else {
        src.inputStream().use { input ->
            dst.outputStream().use { output ->
                val buf = ByteArray(256 * 1024)
                while (!cancel.get()) {
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    onBytes(n.toLong())
                }
            }
        }
    }
}

private fun pathsIn(f: File): List<String> =
    if (f.isDirectory) f.walkTopDown().filter { it.isFile }.take(5000).map { it.absolutePath }.toList() else listOf(f.absolutePath)

/** Tells the media database about changed files so it shows no ghost entries. */
private fun rescan(ctx: Context, paths: List<String>) {
    if (paths.isNotEmpty()) MediaScannerConnection.scanFile(ctx, paths.take(5000).toTypedArray(), null, null)
}

/** Copies or moves [clip] into [dir]. Returns the new file, or null if it failed or was cancelled. */
internal fun paste(ctx: Context, clip: Clip, dir: File, cancel: AtomicBoolean, onProgress: (Float) -> Unit): File? {
    val src = clip.file
    val target = uniqueTarget(dir, src.name)
    val before = pathsIn(src)
    if (clip.move && src.renameTo(target)) {
        rescan(ctx, before + pathsIn(target))
        return target
    }
    val total = treeSize(src).coerceAtLeast(1L)
    var done = 0L
    try {
        copyTree(src, target, cancel) { done += it; onProgress(done.toFloat() / total) }
    } catch (_: Exception) {
        target.deleteRecursively()
        return null
    }
    if (cancel.get()) { target.deleteRecursively(); return null }
    if (clip.move) src.deleteRecursively()
    rescan(ctx, before + pathsIn(target))
    return target
}

internal fun renameTo(ctx: Context, file: File, newName: String): File? {
    val target = File(file.parentFile, newName)
    val before = pathsIn(file)
    if (!file.renameTo(target)) return null
    rescan(ctx, before + pathsIn(target))
    return target
}

internal fun createFolder(dir: File, name: String): File? =
    File(dir, name).takeIf { !it.exists() && it.mkdir() }
