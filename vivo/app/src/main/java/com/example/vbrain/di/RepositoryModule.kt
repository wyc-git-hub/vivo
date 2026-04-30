package com.example.vbrain.di

import com.example.vbrain.data.repository.KnowledgeRepositoryImpl
import com.example.vbrain.domain.repository.KnowledgeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindKnowledgeRepository(
        knowledgeRepositoryImpl: KnowledgeRepositoryImpl
    ): KnowledgeRepository
}

