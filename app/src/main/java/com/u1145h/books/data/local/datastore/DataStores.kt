package com.u1145h.books.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

internal val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session",
)
