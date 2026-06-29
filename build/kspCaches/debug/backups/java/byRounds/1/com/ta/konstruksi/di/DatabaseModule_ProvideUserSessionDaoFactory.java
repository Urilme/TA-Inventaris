package com.ta.konstruksi.di;

import com.ta.konstruksi.data.local.KonstruksiDatabase;
import com.ta.konstruksi.data.local.dao.UserSessionDao;
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
public final class DatabaseModule_ProvideUserSessionDaoFactory implements Factory<UserSessionDao> {
  private final Provider<KonstruksiDatabase> dbProvider;

  public DatabaseModule_ProvideUserSessionDaoFactory(Provider<KonstruksiDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public UserSessionDao get() {
    return provideUserSessionDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideUserSessionDaoFactory create(
      Provider<KonstruksiDatabase> dbProvider) {
    return new DatabaseModule_ProvideUserSessionDaoFactory(dbProvider);
  }

  public static UserSessionDao provideUserSessionDao(KonstruksiDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideUserSessionDao(db));
  }
}
