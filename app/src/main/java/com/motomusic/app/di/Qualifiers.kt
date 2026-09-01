package com.motomusic.app.di

import javax.inject.Qualifier

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

/** A scope that lives as long as the process, for work that must outlive any screen. */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope
