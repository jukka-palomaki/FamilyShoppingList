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
import android.widget.TextView


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
        val checkBox: CheckBox = row.findViewById(R.id.checkToDoItem) as CheckBox
        val checkText: TextView = row.findViewById(R.id.labelToDoItem) as TextView
        checkText.text = currentItem.text.trim()
        checkBox.isChecked = currentItem.isComplete
        checkBox.isEnabled = true

        checkBox.setOnClickListener {
            checkBox.isEnabled = false
            currentItem.isComplete = !currentItem.isComplete
            var msg : String = ""
            if (currentItem.isComplete) {
                msg = "${currentItem.text} collected"
            } else {
                msg = "${currentItem.text} returned to shopping list"
            }

            updateData(currentItem, msg)
        }

        val checkBoxPriority = row.findViewById(R.id.checkPriority) as CheckBox
        checkBoxPriority.isChecked = currentItem.isHighPriority

        checkBoxPriority.setOnClickListener {
            currentItem.isHighPriority = checkBoxPriority.isChecked
            updateData(currentItem, "")
        }

        return row
    }

    private fun updateData(currentItem: ToDoItem, message: String) {
        if (mContext is ToDoActivity) {
            val activity = mContext as ToDoActivity
            activity.updateItem(currentItem, message)
        }
    }

}