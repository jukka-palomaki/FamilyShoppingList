package com.palomaki.familyshoppinglist

/**
 * Represents an item in a ToDo list
 */
class UserGroup {

    /**
     * Unique id for the group
     */
    @com.google.gson.annotations.SerializedName("id")
    var id: String? = null

    /**
     * List all sid ids with comma (,) as a separator
     */
    @com.google.gson.annotations.SerializedName("userIds")
    var userIds: String = ""


    /**
     * UserGroup constructor
     */
    constructor()

}