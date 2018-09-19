package com.palomaki.familyshoppinglist

/**
 * Represents an item in a ToDo list
 */
class ToDoItem {

    /**
     * Item text
     */
    /**
     * Returns the item text
     */
    /**
     * Sets the item text
     *
     * @param text
     * text to set
     */
    @com.google.gson.annotations.SerializedName("text")
    var text: String = ""

    /**
     * Item Id
     */
    /**
     * Returns the item id
     */
    /**
     * Sets the item id
     *
     * @param id
     * id to set
     */
    @com.google.gson.annotations.SerializedName("id")
    var id: String? = null

    /**
     * Indicates if the item is completed
     */
    /**
     * Indicates if the item is marked as completed
     */
    /**
     * Marks the item as completed or incompleted
     */
    @com.google.gson.annotations.SerializedName("complete")
    var isComplete: Boolean = false

    /**
     * userId
     */
    /**
     * Returns the item userId
     */
    /**
     * Sets the item userId
     *
     * @param userId
     * userId to set
     */
    @com.google.gson.annotations.SerializedName("userId")
    var userId: String = ""

    /**
     * ToDoItem constructor
     */
    constructor() {

    }

    override fun toString(): String {
        return text
    }

    /**
     * Initializes a new ToDoItem
     *
     * @param text
     * The item text
     * @param id
     * The item id
     */
    constructor(text: String, id: String) {
        this.text = text
        this.id = id
    }

    override fun equals(other: Any?): Boolean {
        return other is ToDoItem && other.id === this.id
    }
}