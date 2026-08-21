package com.rovena.garage.utils

import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker

object DatePickerHelper {
    fun show(fragmentManager: FragmentManager, tag: String, initialMillis: Long?, onSelected: (Long) -> Unit) {
        val builder = MaterialDatePicker.Builder.datePicker()
        if (initialMillis != null) builder.setSelection(initialMillis)
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { onSelected(it) }
        picker.show(fragmentManager, tag)
    }
}
