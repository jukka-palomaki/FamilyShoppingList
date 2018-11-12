package com.palomaki.familyshoppinglist

/**
 * Represents an item in a ToDo list
 */
class ToDoItem : Comparable<ToDoItem> {

    /**
     * Item text
     */
    @com.google.gson.annotations.SerializedName("text")
    var text: String = ""

    /**
     * Item Id
     */
    @com.google.gson.annotations.SerializedName("id")
    var id: String? = null

    /**
     * Indicates if the item is completed
     */
    @com.google.gson.annotations.SerializedName("complete")
    var isComplete: Boolean = false

    /**
     * userId
     */
    @com.google.gson.annotations.SerializedName("userId")
    var userId: String = ""

    /**
     * Is the item highPriority or not
     */
    @com.google.gson.annotations.SerializedName("highPriority")
    var isHighPriority: Boolean = false


    /**
     * ToDoItem constructor
     */
    constructor()

    override fun toString(): String {
        return text
    }

    override fun equals(other: Any?): Boolean {
        return other is ToDoItem && other.id === this.id
    }

    override operator fun compareTo(f: ToDoItem): Int {
        val prio = f.isHighPriority.compareTo(isHighPriority)
        return if (prio == 0) text.compareTo(f.text) else prio
    }


}