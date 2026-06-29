package com.ta.konstruksi.sync.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.ta.konstruksi.domain.usecase.sync.TriggerSyncUseCase;
import dagger.internal.DaggerGenerated;
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
public final class SyncWorker_Factory {
  private final Provider<TriggerSyncUseCase> triggerSyncUseCaseProvider;

  public SyncWorker_Factory(Provider<TriggerSyncUseCase> triggerSyncUseCaseProvider) {
    this.triggerSyncUseCaseProvider = triggerSyncUseCaseProvider;
  }

  public SyncWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, triggerSyncUseCaseProvider.get());
  }

  public static SyncWorker_Factory create(Provider<TriggerSyncUseCase> triggerSyncUseCaseProvider) {
    return new SyncWorker_Factory(triggerSyncUseCaseProvider);
  }

  public static SyncWorker newInstance(Context context, WorkerParameters workerParams,
      TriggerSyncUseCase triggerSyncUseCase) {
    return new SyncWorker(context, workerParams, triggerSyncUseCase);
  }
}
