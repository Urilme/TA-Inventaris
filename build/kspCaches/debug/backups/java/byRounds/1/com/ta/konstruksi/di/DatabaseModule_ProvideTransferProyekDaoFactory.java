package com.ta.konstruksi.di;

import com.ta.konstruksi.data.local.KonstruksiDatabase;
import com.ta.konstruksi.data.local.dao.TransferProyekDao;
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
public final class DatabaseModule_ProvideTransferProyekDaoFactory implements Factory<TransferProyekDao> {
  private final Provider<KonstruksiDatabase> dbProvider;

  public DatabaseModule_ProvideTransferProyekDaoFactory(Provider<KonstruksiDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public TransferProyekDao get() {
    return provideTransferProyekDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideTransferProyekDaoFactory create(
      Provider<KonstruksiDatabase> dbProvider) {
    return new DatabaseModule_ProvideTransferProyekDaoFactory(dbProvider);
  }

  public static TransferProyekDao provideTransferProyekDao(KonstruksiDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTransferProyekDao(db));
  }
}
