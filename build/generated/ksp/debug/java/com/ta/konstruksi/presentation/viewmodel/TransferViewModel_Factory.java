package com.ta.konstruksi.presentation.viewmodel;

import com.ta.konstruksi.domain.repository.KatalogRepository;
import com.ta.konstruksi.domain.repository.TransferRepository;
import com.ta.konstruksi.sync.SyncScheduler;
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
public final class TransferViewModel_Factory implements Factory<TransferViewModel> {
  private final Provider<TransferRepository> transferRepositoryProvider;

  private final Provider<KatalogRepository> katalogRepositoryProvider;

  private final Provider<SyncScheduler> syncSchedulerProvider;

  public TransferViewModel_Factory(Provider<TransferRepository> transferRepositoryProvider,
      Provider<KatalogRepository> katalogRepositoryProvider,
      Provider<SyncScheduler> syncSchedulerProvider) {
    this.transferRepositoryProvider = transferRepositoryProvider;
    this.katalogRepositoryProvider = katalogRepositoryProvider;
    this.syncSchedulerProvider = syncSchedulerProvider;
  }

  @Override
  public TransferViewModel get() {
    return newInstance(transferRepositoryProvider.get(), katalogRepositoryProvider.get(), syncSchedulerProvider.get());
  }

  public static TransferViewModel_Factory create(
      Provider<TransferRepository> transferRepositoryProvider,
      Provider<KatalogRepository> katalogRepositoryProvider,
      Provider<SyncScheduler> syncSchedulerProvider) {
    return new TransferViewModel_Factory(transferRepositoryProvider, katalogRepositoryProvider, syncSchedulerProvider);
  }

  public static TransferViewModel newInstance(TransferRepository transferRepository,
      KatalogRepository katalogRepository, SyncScheduler syncScheduler) {
    return new TransferViewModel(transferRepository, katalogRepository, syncScheduler);
  }
}
