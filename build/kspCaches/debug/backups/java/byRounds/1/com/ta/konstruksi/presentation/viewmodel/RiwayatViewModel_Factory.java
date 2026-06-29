package com.ta.konstruksi.presentation.viewmodel;

import com.ta.konstruksi.domain.repository.KatalogRepository;
import com.ta.konstruksi.domain.repository.MutasiRepository;
import com.ta.konstruksi.domain.repository.TransferRepository;
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
public final class RiwayatViewModel_Factory implements Factory<RiwayatViewModel> {
  private final Provider<MutasiRepository> mutasiRepositoryProvider;

  private final Provider<TransferRepository> transferRepositoryProvider;

  private final Provider<KatalogRepository> katalogRepositoryProvider;

  public RiwayatViewModel_Factory(Provider<MutasiRepository> mutasiRepositoryProvider,
      Provider<TransferRepository> transferRepositoryProvider,
      Provider<KatalogRepository> katalogRepositoryProvider) {
    this.mutasiRepositoryProvider = mutasiRepositoryProvider;
    this.transferRepositoryProvider = transferRepositoryProvider;
    this.katalogRepositoryProvider = katalogRepositoryProvider;
  }

  @Override
  public RiwayatViewModel get() {
    return newInstance(mutasiRepositoryProvider.get(), transferRepositoryProvider.get(), katalogRepositoryProvider.get());
  }

  public static RiwayatViewModel_Factory create(Provider<MutasiRepository> mutasiRepositoryProvider,
      Provider<TransferRepository> transferRepositoryProvider,
      Provider<KatalogRepository> katalogRepositoryProvider) {
    return new RiwayatViewModel_Factory(mutasiRepositoryProvider, transferRepositoryProvider, katalogRepositoryProvider);
  }

  public static RiwayatViewModel newInstance(MutasiRepository mutasiRepository,
      TransferRepository transferRepository, KatalogRepository katalogRepository) {
    return new RiwayatViewModel(mutasiRepository, transferRepository, katalogRepository);
  }
}
