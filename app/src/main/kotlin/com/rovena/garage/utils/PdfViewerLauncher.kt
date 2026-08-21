package com.rovena.garage.utils

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

object PdfViewerLauncher {
    fun open(fragment: Fragment, file: File) {
        val context = fragment.requireContext()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { fragment.startActivity(intent) }
    }
}
