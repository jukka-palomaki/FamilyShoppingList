package com.palomaki.familyshoppinglist

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Paint
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG




/**
 * Adapter to bind a ToDoItem List to a view
 */
class ToDoItemAdapter(


    /**
     * Adapter context
     */
    internal var mContext: Context,
    /**
     * Adapter View layout
     */
    internal var mLayoutResourceId: Int) : ArrayAdapter<ToDoItem>(mContext, mLayoutResourceId) {

    /**
     * Returns the view for a specific item on the list
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var row = convertView

        val currentItem = getItem(position)

        if (row == null) {
            val inflater = (mContext as Activity).layoutInflater
            row = inflater.inflate(mLayoutResourceId, parent, false)
            if (row == null) {
                return null //should never happen
            }
        }

        row.tag = currentItem
        val checkBox = row.findViewById<CheckBox>(R.id.checkToDoItem)
        checkBox.text = currentItem.text.trim()
        checkBox.isChecked = currentItem.isComplete
        checkBox.isEnabled = true
        checkBox.setPaintFlags(checkBox.getPaintFlags() or Paint.STRIKE_THRU_TEXT_FLAG)

        checkBox.setOnClickListener {
            checkBox.isEnabled = false
            currentItem.isComplete = !currentItem.isComplete
            updateData(currentItem)
        }

        checkBox.setOnLongClickListener {

            val alert = AlertDialog.Builder(mContext)
            val editTextUpdatedText = EditText(context)
            val dialog = alert.create()

            editTextUpdatedText.imeOptions = EditorInfo.IME_ACTION_DONE
            editTextUpdatedText.setSingleLine(true)

            fun doEditActions() {
                val newValue = editTextUpdatedText.text.toString().trim()

                if (newValue.isEmpty()) { // when updated to empty text that is same as delete task
                    currentItem.isComplete = true
                    checkBox.isChecked = true
                }
                checkBox.text = newValue
                currentItem.text = newValue
                updateData(currentItem)
                dialog.dismiss()
            }

            editTextUpdatedText.setOnEditorActionListener { v, actionId, event ->
                return@setOnEditorActionListener when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        doEditActions()
                        true
                    }
                    else -> false
                }
            }

            with (alert) {
                editTextUpdatedText.text = Editable.Factory.getInstance().newEditable(checkBox.text.toString())

                setPositiveButton("Update") {
                    dialog, whichButton ->
                        doEditActions()
                }

                setNegativeButton("Keep original") {
                    dialog, whichButton -> dialog.dismiss()
                }
            }

            dialog.setView(editTextUpdatedText)
            dialog.show()

            true
        }

        val checkBoxPriority = row.findViewById(R.id.checkPriority) as CheckBox
        checkBoxPriority.isChecked = currentItem.isHighPriority

        checkBoxPriority.setOnClickListener {
            currentItem.isHighPriority = checkBoxPriority.isChecked
            updateData(currentItem)
        }

        return row
    }

    private fun updateData(currentItem: ToDoItem) {
        if (mContext is ToDoActivity) {
            val activity = mContext as ToDoActivity
            activity.updateItem(currentItem)
        }
    }

}