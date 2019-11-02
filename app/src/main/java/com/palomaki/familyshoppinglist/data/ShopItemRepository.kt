package fi.jukka.mvvmbasicstut.data

// ShopItemDao must be passed in - it is a dependency
// You could also instantiate the DAO right inside the class without all the fuss, right?
// No. This would break testability - you need to be able to pass a mock version of a DAO
// to the repository (e.g. one that upon calling getShopItems() returns a dummy list of quotes for testing)
// This is the core idea behind DEPENDENCY INJECTION - making things completely modular and independent.
class ShopItemRepository private constructor(private val quoteDao: ShopItemDao) {

    // This may seem redundant.
    // Imagine a code which also updates and checks the backend.
    fun addShopItem(quote: Quote) {
        quoteDao.addShopItem(quote)
    }

    fun getShopItems() = quoteDao.getShopItems()

    companion object {
        // Singleton instantiation you already know and love
        @Volatile private var instance: ShopItemRepository? = null

        fun getInstance(quoteDao: ShopItemDao) =
            instance ?: synchronized(this) {
                instance ?: ShopItemRepository(quoteDao).also { instance = it }
            }
    }
}