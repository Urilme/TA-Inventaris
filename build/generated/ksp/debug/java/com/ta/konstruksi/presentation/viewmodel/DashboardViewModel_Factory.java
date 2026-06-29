package com.ta.konstruksi.presentation.viewmodel;

import com.ta.konstruksi.domain.repository.AuthRepository;
import com.ta.konstruksi.domain.repository.KatalogRepository;
import com.ta.konstruksi.domain.repository.MutasiRepository;
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<KatalogRepository> katalogRepositoryProvider;

  private final Provider<MutasiRepository> mutasiRepositoryProvider;

  private final Provider<TransferRepository> transferRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SyncScheduler> syncSchedulerProvider;

  public DashboardViewModel_Factory(Provider<KatalogRepository> katalogRepositoryProvider,
      Provider<MutasiRepository> mutasiRepositoryProvider,
      Provider<TransferRepository> transferRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SyncScheduler> syncSchedulerProvider) {
    this.katalogRepositoryProvider = katalogRepositoryProvider;
    this.mutasiRepositoryProvider = mutasiRepositoryProvider;
    this.transferRepositoryProvider = transferRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.syncSchedulerProvider = syncSchedulerProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(katalogRepositoryProvider.get(), mutasiRepositoryProvider.get(), transferRepositoryProvider.get(), authRepositoryProvider.get(), syncSchedulerProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<KatalogRepository> katalogRepositoryProvider,
      Provider<MutasiRepository> mutasiRepositoryProvider,
      Provider<TransferRepository> transferRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SyncScheduler> syncSchedulerProvider) {
    return new DashboardViewModel_Factory(katalogRepositoryProvider, mutasiRepositoryProvider, transferRepositoryProvider, authRepositoryProvider, syncSchedulerProvider);
  }

  public static DashboardViewModel newInstance(KatalogRepository katalogRepository,
      MutasiRepository mutasiRepository, TransferRepository transferRepository,
      AuthRepository authRepository, SyncScheduler syncScheduler) {
    return new DashboardViewModel(katalogRepository, mutasiRepository, transferRepository, authRepository, syncScheduler);
  }
}
