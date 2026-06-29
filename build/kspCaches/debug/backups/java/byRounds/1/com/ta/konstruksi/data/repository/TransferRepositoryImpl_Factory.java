package com.ta.konstruksi.data.repository;

import com.ta.konstruksi.data.local.dao.TransferProyekDao;
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
public final class TransferRepositoryImpl_Factory implements Factory<TransferRepositoryImpl> {
  private final Provider<TransferProyekDao> transferDaoProvider;

  public TransferRepositoryImpl_Factory(Provider<TransferProyekDao> transferDaoProvider) {
    this.transferDaoProvider = transferDaoProvider;
  }

  @Override
  public TransferRepositoryImpl get() {
    return newInstance(transferDaoProvider.get());
  }

  public static TransferRepositoryImpl_Factory create(
      Provider<TransferProyekDao> transferDaoProvider) {
    return new TransferRepositoryImpl_Factory(transferDaoProvider);
  }

  public static TransferRepositoryImpl newInstance(TransferProyekDao transferDao) {
    return new TransferRepositoryImpl(transferDao);
  }
}
