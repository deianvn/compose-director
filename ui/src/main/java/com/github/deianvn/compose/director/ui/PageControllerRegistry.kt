package com.github.deianvn.compose.director.ui

import com.github.deianvn.compose.director.core.Scene
import kotlin.reflect.KClass

class PageControllerRegistry {

    private class Entry<S : Scene>(
        val factory: () -> PageController<S>
    )

    private val registry = mutableMapOf<KClass<out Scene>, Entry<out Scene>>()

    fun <S : Scene> register(sceneType: KClass<S>, sceneFactory: () -> PageController<S>) {
        registry[sceneType] = Entry(sceneFactory)
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : Scene> getController(scene: S): PageController<S>? {
        val entry = registry[scene::class] ?: return null
        return (entry as Entry<S>).factory()
    }

}