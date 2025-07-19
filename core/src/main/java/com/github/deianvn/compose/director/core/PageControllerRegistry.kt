package com.github.deianvn.compose.director.core

import kotlin.reflect.KClass


class PageControllerRegistry {

    private val registry = mutableMapOf<KClass<out Scene>, (Scene) -> PageController<out Scene>>()

    fun register(sceneType: KClass<Scene>, scenefactory: () -> PageController<Scene>) {
        registry[sceneType] = scenefactory
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : Scene> getController(scene: S): PageController<S>? {
        return registry[scene::class]?.invoke(scene) as? PageController<S>
    }

}
