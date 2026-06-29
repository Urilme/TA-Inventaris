package com.ta.konstruksi.data.repository;

import com.ta.konstruksi.data.local.dao.LogMutasiDao;
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
public final class MutasiRepositoryImpl_Factory implements Factory<MutasiRepositoryImpl> {
  private final Provider<LogMutasiDao> mutasiDaoProvider;

  public MutasiRepositoryImpl_Factory(Provider<LogMutasiDao> mutasiDaoProvider) {
    this.mutasiDaoProvider = mutasiDaoProvider;
  }

  @Override
  public MutasiRepositoryImpl get() {
    return newInstance(mutasiDaoProvider.get());
  }

  public static MutasiRepositoryImpl_Factory create(Provider<LogMutasiDao> mutasiDaoProvider) {
    return new MutasiRepositoryImpl_Factory(mutasiDaoProvider);
  }

  public static MutasiRepositoryImpl newInstance(LogMutasiDao mutasiDao) {
    return new MutasiRepositoryImpl(mutasiDao);
  }
}
