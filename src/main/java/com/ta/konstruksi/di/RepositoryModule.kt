package com.ta.konstruksi.di

import com.ta.konstruksi.data.repository.*
import com.ta.konstruksi.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindKatalogRepository(impl: KatalogRepositoryImpl): KatalogRepository
    @Binds @Singleton abstract fun bindMutasiRepository(impl: MutasiRepositoryImpl): MutasiRepository
    @Binds @Singleton abstract fun bindTransferRepository(impl: TransferRepositoryImpl): TransferRepository
    @Binds @Singleton abstract fun bindProyekRepository(impl: ProyekRepositoryImpl): ProyekRepository
    @Binds @Singleton abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
}
