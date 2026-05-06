package com.example.vbrain.di

import android.content.Context
import androidx.room.Room
import com.example.vbrain.data.local.VBrainDatabase
import com.example.vbrain.data.local.dao.KnowledgeDao
import com.example.vbrain.data.local.dao.TodoDao
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
        )
        .addMigrations(VBrainDatabase.MIGRATION_1_2, VBrainDatabase.MIGRATION_2_3, VBrainDatabase.MIGRATION_3_4)
        .build()
    }

    @Provides
    @Singleton
    fun provideKnowledgeDao(database: VBrainDatabase): KnowledgeDao {
        return database.knowledgeDao
    }

    @Provides
    @Singleton
    fun provideTodoDao(database: VBrainDatabase): TodoDao {
        return database.todoDao
    }
}
