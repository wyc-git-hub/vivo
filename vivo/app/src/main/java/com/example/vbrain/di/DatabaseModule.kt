package com.example.vbrain.di

import android.content.Context
import androidx.room.Room
import com.example.vbrain.data.local.VBrainDatabase
import com.example.vbrain.data.local.dao.KnowledgeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVBrainDatabase(@ApplicationContext context: Context): VBrainDatabase {
        return Room.databaseBuilder(
            context,
            VBrainDatabase::class.java,
            VBrainDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideKnowledgeDao(database: VBrainDatabase): KnowledgeDao {
        return database.knowledgeDao
    }
}

