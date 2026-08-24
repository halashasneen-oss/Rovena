package com.rovena.garage.presentation.common

import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rovena.garage.R

/**
 * Generic "Delete this record?" confirmation for maintenance/fuel/expense/document/
 * inspection/reminder/part/note deletes - mirrors GarageFragment's vehicle-specific
 * confirmDelete() dialog, minus the "this also wipes every related record" wording
 * that only applies to deleting a whole vehicle.
 */
fun Fragment.confirmDelete(onConfirm: () -> Unit) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.delete_record_confirm_title)
        .setMessage(R.string.delete_record_confirm_message)
        .setPositiveButton(R.string.action_delete) { _, _ -> onConfirm() }
        .setNegativeButton(R.string.action_cancel, null)
        .show()
}

/**
 * Intercepts system back (button or gesture) while [isDirty] reports true and asks
 * the user to confirm discarding unsaved edits, instead of silently popping the form
 * and losing them - no form screen in this app guarded against that before. Scoped to
 * [viewLifecycleOwner] so it stops intercepting once the view is destroyed.
 */
fun Fragment.guardUnsavedChanges(isDirty: () -> Boolean) {
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
        if (isDirty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.discard_changes_confirm_title)
                .setMessage(R.string.discard_changes_confirm_message)
                .setPositiveButton(R.string.discard_changes_confirm_action) { _, _ ->
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        } else {
            isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}
