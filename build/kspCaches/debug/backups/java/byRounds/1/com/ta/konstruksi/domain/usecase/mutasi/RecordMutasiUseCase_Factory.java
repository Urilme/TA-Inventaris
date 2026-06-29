package com.ta.konstruksi.domain.usecase.mutasi;

import com.ta.konstruksi.domain.repository.KatalogRepository;
import com.ta.konstruksi.domain.repository.MutasiRepository;
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
public final class RecordMutasiUseCase_Factory implements Factory<RecordMutasiUseCase> {
  private final Provider<MutasiRepository> mutasiRepositoryProvider;

  private final Provider<KatalogRepository> katalogRepositoryProvider;

  public RecordMutasiUseCase_Factory(Provider<MutasiRepository> mutasiRepositoryProvider,
      Provider<KatalogRepository> katalogRepositoryProvider) {
    this.mutasiRepositoryProvider = mutasiRepositoryProvider;
    this.katalogRepositoryProvider = katalogRepositoryProvider;
  }

  @Override
  public RecordMutasiUseCase get() {
    return newInstance(mutasiRepositoryProvider.get(), katalogRepositoryProvider.get());
  }

  public static RecordMutasiUseCase_Factory create(
      Provider<MutasiRepository> mutasiRepositoryProvider,
      Provider<KatalogRepository> katalogRepositoryProvider) {
    return new RecordMutasiUseCase_Factory(mutasiRepositoryProvider, katalogRepositoryProvider);
  }

  public static RecordMutasiUseCase newInstance(MutasiRepository mutasiRepository,
      KatalogRepository katalogRepository) {
    return new RecordMutasiUseCase(mutasiRepository, katalogRepository);
  }
}
