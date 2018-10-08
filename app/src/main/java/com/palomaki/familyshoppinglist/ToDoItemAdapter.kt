package com.palomaki.familyshoppinglist

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText


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
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = convertView

        val currentItem = getItem(position)

        if (row == null) {
            val inflater = (mContext as Activity).layoutInflater
            row = inflater.inflate(mLayoutResourceId, parent, false)
        }

        row!!.tag = currentItem
        val checkBox = row.findViewById(R.id.checkToDoItem) as CheckBox
        checkBox.text = currentItem!!.text.trim()
        checkBox.isChecked = false
        checkBox.isEnabled = true

        checkBox.setOnClickListener {
            if (checkBox.isChecked) {
                checkBox.isEnabled = false
                currentItem.isComplete = true
                updateData(currentItem)
            }
        }

        checkBox.setOnLongClickListener {

            val alert = AlertDialog.Builder(mContext)
            val editTextUpdatedText = EditText(context)

            // Builder
            with (alert) {
                editTextUpdatedText.text = Editable.Factory.getInstance().newEditable(checkBox.text.toString())

                setPositiveButton("Update") {
                    dialog, whichButton ->
                    dialog.dismiss()
                    val newValue = editTextUpdatedText.text.toString().trim()

                    if (newValue.isEmpty()) { // when updated to empty text that is same as delete task
                        currentItem.isComplete = true
                        checkBox.isChecked = true
                    }
                    checkBox.text = newValue
                    currentItem.text = newValue
                    updateData(currentItem)
                }

                setNegativeButton("Keep original") {
                    dialog, whichButton ->
                    dialog.dismiss()
                }
            }

            // Dialog
            val dialog = alert.create()
            dialog.setView(editTextUpdatedText)
            dialog.show()

            true
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