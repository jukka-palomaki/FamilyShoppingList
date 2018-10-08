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
                if (mContext is ToDoActivity) {
                    val activity = mContext as ToDoActivity
                    activity.checkItem(currentItem)
                }
            }
        }

        checkBox.setOnLongClickListener {
            displayAlert(checkBox)

            true
        }

        return row
    }

    fun displayAlert(checkBox: CheckBox){
        val alert = AlertDialog.Builder(mContext)
        val editTextUpdatedText = EditText(context)

        // Builder
        with (alert) {
            editTextUpdatedText.text = Editable.Factory.getInstance().newEditable(checkBox.text.toString())

            setPositiveButton("Update") {
                dialog, whichButton ->
                dialog.dismiss()
                checkBox.text = editTextUpdatedText.text.toString()
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
    }

}