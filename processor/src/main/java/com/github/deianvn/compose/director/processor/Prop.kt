package com.github.deianvn.compose.director.processor


@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Prop(val persistent: Boolean = false)
