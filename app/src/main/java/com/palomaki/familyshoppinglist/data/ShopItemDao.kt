package fi.jukka.mvvmbasicstut.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class ShopItemDao {
    // A fake database table
    private val shopItemList = mutableListOf<Quote>()
    // MutableLiveData is from the Architecture Components Library
    // LiveData can be observed for changes
    private val shopItems = MutableLiveData<List<Quote>>()

    init {
        // Immediately connect the now empty shopItemList
        // to the MutableLiveData which can be observed
        shopItems.value = shopItemList
    }

    fun addShopItem(quote: Quote) {
        shopItemList.add(quote)
        // After adding a quote to the "database",
        // update the value of MutableLiveData
        // which will notify its active observers
        shopItems.value = shopItemList
    }

    // Casting MutableLiveData to LiveData because its value
    // shouldn't be changed from other classes
    fun getShopItems() = shopItems as LiveData<List<Quote>>
}