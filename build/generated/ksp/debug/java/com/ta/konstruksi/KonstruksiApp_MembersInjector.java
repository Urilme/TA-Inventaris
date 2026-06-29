package com.ta.konstruksi;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class KonstruksiApp_MembersInjector implements MembersInjector<KonstruksiApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public KonstruksiApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<KonstruksiApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new KonstruksiApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(KonstruksiApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.ta.konstruksi.KonstruksiApp.workerFactory")
  public static void injectWorkerFactory(KonstruksiApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
