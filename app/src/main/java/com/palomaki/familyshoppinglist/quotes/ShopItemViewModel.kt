package fi.jukka.mvvmbasicstut.ui.quotes

import androidx.lifecycle.ViewModel
import fi.jukka.mvvmbasicstut.data.Quote
import fi.jukka.mvvmbasicstut.data.ShopItemRepository

// ShopItemRepository dependency will again be passed in the
// constructor using dependency injection
class ShopItemViewModel(private val shopItemRepository: ShopItemRepository)
    : ViewModel() {

    fun getQuotes() = shopItemRepository.getQuotes()

    fun addQuote(quote: Quote) = shopItemRepository.addQuote(quote)
}