package com.u1145h.books.core.config

/**
 * Central server configuration. The active server URL is entered on first run,
 * verified, and stored in DataStore; this object only holds tuning values and
 * a placeholder default. There is no in-app way to change the server later —
 * the user must clear app data and set up again.
 */
object ServerConfig {

    /** Placeholder shown in the first-run field; users enter their own server. */
    const val DEFAULT_SERVER_URL = "http://192.168.1.100:5000"

    /** Retrofit base path under the server host. */
    const val API_PATH = "/api/"

    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 60L
    const val WRITE_TIMEOUT_SECONDS = 60L

    /** Name of the Kavita generic API key used for long-lived authenticated calls. */
    const val GENERIC_API_KEY_NAME = "Generic"
}
