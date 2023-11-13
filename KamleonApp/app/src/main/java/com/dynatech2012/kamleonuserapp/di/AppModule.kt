package com.dynatech2012.kamleonuserapp.di

import com.dynatech2012.kamleonuserapp.repositories.AuthRepository
import com.dynatech2012.kamleonuserapp.repositories.FirestoreRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun loginRepositoryProvider(): AuthRepository = AuthRepository()
    @Provides
    @Singleton
    fun firestoreRepositoryProvider(): FirestoreRepository = FirestoreRepository()
}