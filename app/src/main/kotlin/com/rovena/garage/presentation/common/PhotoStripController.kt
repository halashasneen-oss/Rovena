package com.rovena.garage.presentation.common

import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import coil.load
import com.rovena.garage.R
import java.io.File
import java.util.UUID

/**
 * Reusable "attach photos" strip (spec requires photo attachments on
 * maintenance, inspection, and expense receipts). Copies picked images into
 * internal storage immediately so they survive past the content:// URI's
 * transient read permission.
 *
 * Must be constructed as a Fragment property (not lazily inside
 * onViewCreated) so [ActivityResultLauncher] registration happens before the
 * fragment reaches STARTED, per the Activity Result API contract. Call
 * [bind] once the view is available.
 */
class PhotoStripController(
    private val fragment: Fragment,
    private val subDir: String = "photos",
    private val onChanged: (List<String>) -> Unit = {}
) {
    private val paths = mutableListOf<String>()
    private var container: LinearLayout? = null

    private val launcher: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            val destDir = File(fragment.requireContext().filesDir, subDir).apply { mkdirs() }
            uris.forEach { uri ->
                val destFile = File(destDir, "${UUID.randomUUID()}.jpg")
                runCatching {
                    fragment.requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }.onSuccess { paths.add(destFile.absolutePath) }
            }
            render()
            onChanged(paths.toList())
        }

    fun bind(container: LinearLayout, existing: List<String> = emptyList()) {
        this.container = container
        paths.clear()
        paths.addAll(existing)
        render()
    }

    fun currentPaths(): List<String> = paths.toList()

    fun pickPhotos() = launcher.launch("image/*")

    private fun render() {
        val container = container ?: return
        container.removeAllViews()
        val size = (72 * container.resources.displayMetrics.density).toInt()
        val margin = (8 * container.resources.displayMetrics.density).toInt()
        paths.forEach { path ->
            val iv = ImageView(fragment.requireContext())
            val params = LinearLayout.LayoutParams(size, size).apply { marginEnd = margin }
            iv.layoutParams = params
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.load(File(path))
            iv.setOnLongClickListener {
                paths.remove(path)
                render()
                onChanged(paths.toList())
                true
            }
            container.addView(iv)
        }
        val addButton = ImageView(fragment.requireContext())
        addButton.layoutParams = LinearLayout.LayoutParams(size, size)
        addButton.setImageResource(R.drawable.ic_add)
        addButton.setBackgroundResource(android.R.drawable.list_selector_background)
        addButton.setOnClickListener { pickPhotos() }
        container.addView(addButton)
    }
}
