package io.github.ketarro.credivaro.ui

enum class Appearance(val stored: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");
    companion object {
        fun fromStored(value: String?) = entries.firstOrNull { it.stored == value } ?: SYSTEM
    }
}
enum class AppLanguage(val tag: String) {
    SYSTEM(""), RUSSIAN("ru"), ENGLISH("en");
    companion object {
        fun fromTag(value: String?) = entries.firstOrNull { it.tag == value } ?: SYSTEM
    }
}
