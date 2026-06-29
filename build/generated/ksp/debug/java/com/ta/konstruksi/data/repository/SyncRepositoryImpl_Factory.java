package com.ta.konstruksi.data.repository;

import com.ta.konstruksi.data.local.dao.LogMutasiDao;
import com.ta.konstruksi.data.local.dao.MasterKatalogDao;
import com.ta.konstruksi.data.local.dao.TransferProyekDao;
import com.ta.konstruksi.data.local.dao.UserSessionDao;
import com.ta.konstruksi.data.remote.FirestoreDataSource;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class SyncRepositoryImpl_Factory implements Factory<SyncRepositoryImpl> {
  private final Provider<LogMutasiDao> mutasiDaoProvider;

  private final Provider<TransferProyekDao> transferDaoProvider;

  private final Provider<MasterKatalogDao> katalogDaoProvider;

  private final Provider<UserSessionDao> userSessionDaoProvider;

  private final Provider<FirestoreDataSource> firestoreProvider;

  public SyncRepositoryImpl_Factory(Provider<LogMutasiDao> mutasiDaoProvider,
      Provider<TransferProyekDao> transferDaoProvider,
      Provider<MasterKatalogDao> katalogDaoProvider,
      Provider<UserSessionDao> userSessionDaoProvider,
      Provider<FirestoreDataSource> firestoreProvider) {
    this.mutasiDaoProvider = mutasiDaoProvider;
    this.transferDaoProvider = transferDaoProvider;
    this.katalogDaoProvider = katalogDaoProvider;
    this.userSessionDaoProvider = userSessionDaoProvider;
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public SyncRepositoryImpl get() {
    return newInstance(mutasiDaoProvider.get(), transferDaoProvider.get(), katalogDaoProvider.get(), userSessionDaoProvider.get(), firestoreProvider.get());
  }

  public static SyncRepositoryImpl_Factory create(Provider<LogMutasiDao> mutasiDaoProvider,
      Provider<TransferProyekDao> transferDaoProvider,
      Provider<MasterKatalogDao> katalogDaoProvider,
      Provider<UserSessionDao> userSessionDaoProvider,
      Provider<FirestoreDataSource> firestoreProvider) {
    return new SyncRepositoryImpl_Factory(mutasiDaoProvider, transferDaoProvider, katalogDaoProvider, userSessionDaoProvider, firestoreProvider);
  }

  public static SyncRepositoryImpl newInstance(LogMutasiDao mutasiDao,
      TransferProyekDao transferDao, MasterKatalogDao katalogDao, UserSessionDao userSessionDao,
      FirestoreDataSource firestore) {
    return new SyncRepositoryImpl(mutasiDao, transferDao, katalogDao, userSessionDao, firestore);
  }
}
