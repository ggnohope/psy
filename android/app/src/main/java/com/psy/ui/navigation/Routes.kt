package com.psy.ui.navigation

object Routes {
    const val HOME = "home"

    // Route pattern for NavHost (Task 6 uses this)
    const val ADD_EDIT_PATTERN = "addEdit?txId={txId}"
    const val ARG_TX_ID = "txId"

    const val SETTINGS = "settings"
    const val MANAGE_CATEGORIES = "manageCategories"
    const val MANAGE_ACCOUNTS = "manageAccounts"

    /** Builds a navigation route to the add/edit screen.
     *  txId = null → new transaction (-1L), otherwise edit. */
    fun addEdit(txId: Long? = null): String = "addEdit?txId=${txId ?: -1L}"
}
