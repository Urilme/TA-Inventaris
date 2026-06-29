package com.ta.konstruksi.di;

import com.ta.konstruksi.data.local.KonstruksiDatabase;
import com.ta.konstruksi.data.local.dao.LogMutasiDao;
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
public final class DatabaseModule_ProvideLogMutasiDaoFactory implements Factory<LogMutasiDao> {
  private final Provider<KonstruksiDatabase> dbProvider;

  public DatabaseModule_ProvideLogMutasiDaoFactory(Provider<KonstruksiDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public LogMutasiDao get() {
    return provideLogMutasiDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideLogMutasiDaoFactory create(
      Provider<KonstruksiDatabase> dbProvider) {
    return new DatabaseModule_ProvideLogMutasiDaoFactory(dbProvider);
  }

  public static LogMutasiDao provideLogMutasiDao(KonstruksiDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideLogMutasiDao(db));
  }
}
