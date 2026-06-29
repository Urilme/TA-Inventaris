package com.ta.konstruksi.di;

import com.ta.konstruksi.data.local.KonstruksiDatabase;
import com.ta.konstruksi.data.local.dao.MasterKatalogDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideMasterKatalogDaoFactory implements Factory<MasterKatalogDao> {
  private final Provider<KonstruksiDatabase> dbProvider;

  public DatabaseModule_ProvideMasterKatalogDaoFactory(Provider<KonstruksiDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public MasterKatalogDao get() {
    return provideMasterKatalogDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideMasterKatalogDaoFactory create(
      Provider<KonstruksiDatabase> dbProvider) {
    return new DatabaseModule_ProvideMasterKatalogDaoFactory(dbProvider);
  }

  public static MasterKatalogDao provideMasterKatalogDao(KonstruksiDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMasterKatalogDao(db));
  }
}
