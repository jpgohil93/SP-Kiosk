package com.screenpulse.kiosk.ui.admin

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.content.Intent
import android.text.InputType
import com.screenpulse.kiosk.core.config.ConfigManager

class PinDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        return inflater.inflate(com.screenpulse.kiosk.R.layout.dialog_pin, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val pinInput = view.findViewById<EditText>(com.screenpulse.kiosk.R.id.pinInput)
        val errorText = view.findViewById<android.widget.TextView>(com.screenpulse.kiosk.R.id.errorText)
        val unlockButton = view.findViewById<android.widget.Button>(com.screenpulse.kiosk.R.id.unlockButton)
        val cancelButton = view.findViewById<android.widget.Button>(com.screenpulse.kiosk.R.id.cancelButton)
        
        unlockButton.setOnClickListener {
            val pin = pinInput.text.toString()
            if (validatePin(pin)) {
                startActivity(Intent(requireContext(), AdminSettingsActivity::class.java))
                dismiss()
            } else {
                errorText.visibility = android.view.View.VISIBLE
                pinInput.setText("")
            }
        }
        
        cancelButton.setOnClickListener {
            dismiss()
        }
        
        // Auto-focus input
        pinInput.requestFocus()
        dialog?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        return dialog
    }

    private fun validatePin(pin: String): Boolean {
        val config = ConfigManager.getConfig(requireContext())
        return pin == config.adminPin
    }
}
