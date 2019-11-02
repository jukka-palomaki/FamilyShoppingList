package fi.jukka.mvvmbasicstut.ui.quotes

import androidx.lifecycle.ViewModel
import fi.jukka.mvvmbasicstut.data.Quote
import fi.jukka.mvvmbasicstut.data.ShopItemRepository

// ShopItemRepository dependency will again be passed in the
// constructor using dependency injection
class ShopItemViewModel(private val shopItemRepository: ShopItemRepository)
    : ViewModel() {

    fun getShopItem() = shopItemRepository.getShopItems()

    fun addShopItem(quote: Quote) = shopItemRepository.addShopItem(quote)
}