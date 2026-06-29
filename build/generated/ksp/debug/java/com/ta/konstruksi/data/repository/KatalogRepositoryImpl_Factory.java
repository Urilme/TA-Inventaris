package com.ta.konstruksi.data.repository;

import com.ta.konstruksi.data.local.dao.MasterKatalogDao;
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
public final class KatalogRepositoryImpl_Factory implements Factory<KatalogRepositoryImpl> {
  private final Provider<MasterKatalogDao> katalogDaoProvider;

  public KatalogRepositoryImpl_Factory(Provider<MasterKatalogDao> katalogDaoProvider) {
    this.katalogDaoProvider = katalogDaoProvider;
  }

  @Override
  public KatalogRepositoryImpl get() {
    return newInstance(katalogDaoProvider.get());
  }

  public static KatalogRepositoryImpl_Factory create(
      Provider<MasterKatalogDao> katalogDaoProvider) {
    return new KatalogRepositoryImpl_Factory(katalogDaoProvider);
  }

  public static KatalogRepositoryImpl newInstance(MasterKatalogDao katalogDao) {
    return new KatalogRepositoryImpl(katalogDao);
  }
}
