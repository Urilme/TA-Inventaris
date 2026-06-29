package com.ta.konstruksi.presentation.viewmodel;

import com.ta.konstruksi.domain.usecase.mutasi.RecordMutasiUseCase;
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
public final class MutasiViewModel_Factory implements Factory<MutasiViewModel> {
  private final Provider<RecordMutasiUseCase> recordMutasiUseCaseProvider;

  private final Provider<SyncScheduler> syncSchedulerProvider;

  public MutasiViewModel_Factory(Provider<RecordMutasiUseCase> recordMutasiUseCaseProvider,
      Provider<SyncScheduler> syncSchedulerProvider) {
    this.recordMutasiUseCaseProvider = recordMutasiUseCaseProvider;
    this.syncSchedulerProvider = syncSchedulerProvider;
  }

  @Override
  public MutasiViewModel get() {
    return newInstance(recordMutasiUseCaseProvider.get(), syncSchedulerProvider.get());
  }

  public static MutasiViewModel_Factory create(
      Provider<RecordMutasiUseCase> recordMutasiUseCaseProvider,
      Provider<SyncScheduler> syncSchedulerProvider) {
    return new MutasiViewModel_Factory(recordMutasiUseCaseProvider, syncSchedulerProvider);
  }

  public static MutasiViewModel newInstance(RecordMutasiUseCase recordMutasiUseCase,
      SyncScheduler syncScheduler) {
    return new MutasiViewModel(recordMutasiUseCase, syncScheduler);
  }
}
