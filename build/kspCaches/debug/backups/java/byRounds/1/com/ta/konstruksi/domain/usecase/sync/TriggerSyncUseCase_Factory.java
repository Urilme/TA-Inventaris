package com.ta.konstruksi.domain.usecase.sync;

import com.ta.konstruksi.domain.repository.SyncRepository;
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
public final class TriggerSyncUseCase_Factory implements Factory<TriggerSyncUseCase> {
  private final Provider<SyncRepository> syncRepositoryProvider;

  public TriggerSyncUseCase_Factory(Provider<SyncRepository> syncRepositoryProvider) {
    this.syncRepositoryProvider = syncRepositoryProvider;
  }

  @Override
  public TriggerSyncUseCase get() {
    return newInstance(syncRepositoryProvider.get());
  }

  public static TriggerSyncUseCase_Factory create(Provider<SyncRepository> syncRepositoryProvider) {
    return new TriggerSyncUseCase_Factory(syncRepositoryProvider);
  }

  public static TriggerSyncUseCase newInstance(SyncRepository syncRepository) {
    return new TriggerSyncUseCase(syncRepository);
  }
}
