package com.ta.konstruksi.presentation.viewmodel;

import com.ta.konstruksi.domain.repository.KatalogRepository;
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
public final class KatalogViewModel_Factory implements Factory<KatalogViewModel> {
  private final Provider<KatalogRepository> katalogRepositoryProvider;

  private final Provider<SyncScheduler> syncSchedulerProvider;

  public KatalogViewModel_Factory(Provider<KatalogRepository> katalogRepositoryProvider,
      Provider<SyncScheduler> syncSchedulerProvider) {
    this.katalogRepositoryProvider = katalogRepositoryProvider;
    this.syncSchedulerProvider = syncSchedulerProvider;
  }

  @Override
  public KatalogViewModel get() {
    return newInstance(katalogRepositoryProvider.get(), syncSchedulerProvider.get());
  }

  public static KatalogViewModel_Factory create(
      Provider<KatalogRepository> katalogRepositoryProvider,
      Provider<SyncScheduler> syncSchedulerProvider) {
    return new KatalogViewModel_Factory(katalogRepositoryProvider, syncSchedulerProvider);
  }

  public static KatalogViewModel newInstance(KatalogRepository katalogRepository,
      SyncScheduler syncScheduler) {
    return new KatalogViewModel(katalogRepository, syncScheduler);
  }
}
