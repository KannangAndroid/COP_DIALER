package com.nibav.dialer.interfaces

interface RefreshItemsListener {
    fun refreshItems(callback: (() -> Unit)? = null)
}
