package com.ta.konstruksi.presentation.viewmodel;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SyncViewModel_Factory implements Factory<SyncViewModel> {
  private final Provider<Context> contextProvider;

  public SyncViewModel_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SyncViewModel get() {
    return newInstance(contextProvider.get());
  }

  public static SyncViewModel_Factory create(Provider<Context> contextProvider) {
    return new SyncViewModel_Factory(contextProvider);
  }

  public static SyncViewModel newInstance(Context context) {
    return new SyncViewModel(context);
  }
}
