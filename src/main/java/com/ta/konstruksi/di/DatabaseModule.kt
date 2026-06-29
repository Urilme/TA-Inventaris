package com.ta.konstruksi.di

import android.content.Context
import androidx.room.Room
import com.ta.konstruksi.data.local.KonstruksiDatabase
import com.ta.konstruksi.data.local.dao.*
import com.ta.konstruksi.util.Constants
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
    fun provideDatabase(@ApplicationContext context: Context): KonstruksiDatabase =
        Room.databaseBuilder(context, KonstruksiDatabase::class.java, Constants.DATABASE_NAME)
            .build()

    @Provides fun provideUserSessionDao(db: KonstruksiDatabase): UserSessionDao = db.userSessionDao()
    @Provides fun provideMasterKatalogDao(db: KonstruksiDatabase): MasterKatalogDao = db.masterKatalogDao()
    @Provides fun provideLogMutasiDao(db: KonstruksiDatabase): LogMutasiDao = db.logMutasiDao()
    @Provides fun provideTransferProyekDao(db: KonstruksiDatabase): TransferProyekDao = db.transferProyekDao()
}
