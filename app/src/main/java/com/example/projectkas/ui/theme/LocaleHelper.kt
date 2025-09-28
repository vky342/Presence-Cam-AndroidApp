package com.example.projectkas.ui.theme

import android.content.Context
import android.content.ContextWrapper
import java.util.Locale
import android.content.res.Configuration

object LocaleHelper {
    fun wrap(context: Context, language: String): ContextWrapper {
        var ctx = context
        val locale = Locale(language)
        Locale.setDefault(locale)

        val res = ctx.resources
        val config = Configuration(res.configuration)

        config.setLocale(locale)
        ctx = ctx.createConfigurationContext(config)

        return ContextWrapper(ctx)
    }
}
