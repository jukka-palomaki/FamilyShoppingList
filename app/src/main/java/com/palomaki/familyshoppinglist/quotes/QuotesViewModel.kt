package fi.jukka.mvvmbasicstut.ui.quotes

import androidx.lifecycle.ViewModel
import fi.jukka.mvvmbasicstut.data.Quote
import fi.jukka.mvvmbasicstut.data.QuoteRepository

// QuoteRepository dependency will again be passed in the
// constructor using dependency injection
class QuotesViewModel(private val quoteRepository: QuoteRepository)
    : ViewModel() {

    fun getQuotes() = quoteRepository.getQuotes()

    fun addQuote(quote: Quote) = quoteRepository.addQuote(quote)
}