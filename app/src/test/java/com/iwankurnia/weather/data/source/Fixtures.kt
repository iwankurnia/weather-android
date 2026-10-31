package com.iwankurnia.weather.data.source

/** Reads a captured response from `app/src/test/resources`. */
internal fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.getResource("/$name")) { "Missing fixture $name" }.readText()
