package com.palomaki.familyshoppinglist

/**
 * Represents an item in a ToDo list
 */
data class ToDoItem(

        /**
         * Item text
         */
        @com.google.gson.annotations.SerializedName("text")
        var text: String,

        /**
         * Indicates if the item is completed
         */
        @com.google.gson.annotations.SerializedName("complete")
        var isComplete: Boolean = false,

        /**
         * userId
         */
        @com.google.gson.annotations.SerializedName("userId")
        val userId: String = ""

) : Comparable<ToDoItem> {

    /**
     * Item Id
     */
    @com.google.gson.annotations.SerializedName("id")
    var id: String? = null

    /**
     * Is the item highPriority or not
     */
    @com.google.gson.annotations.SerializedName("highPriority")
    var isHighPriority: Boolean = false

    override fun toString(): String {
        return text
    }

    override fun equals(other: Any?): Boolean {
        return other is ToDoItem && other.id === this.id
    }

    override operator fun compareTo(other: ToDoItem): Int {
        val completed = isComplete.compareTo(other.isComplete)
        return if (completed == 0) {
            val priority = other.isHighPriority.compareTo(isHighPriority)
            if (priority == 0) text.compareTo(other.text) else priority
        } else completed
    }

}