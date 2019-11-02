package fi.jukka.mvvmbasicstut.ui.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fi.jukka.mvvmbasicstut.data.ShopItemRepository


// The same repository that's needed for ShopItemViewModel
// is also passed to the factory
class ShopItemViewModelFactory(private val shopItemRepository: ShopItemRepository)
    : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ShopItemViewModel(shopItemRepository) as T
    }
}