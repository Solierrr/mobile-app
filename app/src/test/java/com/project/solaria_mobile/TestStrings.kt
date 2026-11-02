package com.project.solaria_mobile

import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import java.io.File

/**
 * Os recursos de texto em português do aplicativo, lidos de `strings.xml`, para os testes que conferem a mensagem
 * mostrada sem precisar de um aparelho. Só `getString` é simulado.
 */
object TestStrings {
    private val values: Map<Int, String> by lazy {
        val file = listOf("src/main/res/values/strings.xml", "app/src/main/res/values/strings.xml").map(::File).first { it.exists() }
        Regex("<string name=\"(\\w+)\"[^>]*>(.*?)</string>", RegexOption.DOT_MATCHES_ALL).findAll(file.readText()).mapNotNull { match ->
            val id = runCatching { R.string::class.java.getField(match.groupValues[1]).getInt(null) }.getOrNull() ?: return@mapNotNull null
            id to match.groupValues[2].trim().removeSurrounding("\"").replace("\\'", "'").replace("&amp;", "&").replace("&lt;", "<")
        }.toMap()
    }

    val resources: Resources = mockk {
        every { getString(any()) } answers { values.getValue(firstArg()) }
        every { getString(any(), *anyVararg()) } answers { values.getValue(firstArg()).format(*secondArg<Array<Any>>()) }
    }
}
