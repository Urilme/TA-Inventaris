package com.ta.konstruksi;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ta.konstruksi.data.local.KonstruksiDatabase;
import com.ta.konstruksi.data.local.dao.LogMutasiDao;
import com.ta.konstruksi.data.local.dao.MasterKatalogDao;
import com.ta.konstruksi.data.local.dao.TransferProyekDao;
import com.ta.konstruksi.data.local.dao.UserSessionDao;
import com.ta.konstruksi.data.remote.FirestoreDataSource;
import com.ta.konstruksi.data.repository.AuthRepositoryImpl;
import com.ta.konstruksi.data.repository.KatalogRepositoryImpl;
import com.ta.konstruksi.data.repository.MutasiRepositoryImpl;
import com.ta.konstruksi.data.repository.SyncRepositoryImpl;
import com.ta.konstruksi.data.repository.TransferRepositoryImpl;
import com.ta.konstruksi.di.DatabaseModule_ProvideDatabaseFactory;
import com.ta.konstruksi.di.DatabaseModule_ProvideLogMutasiDaoFactory;
import com.ta.konstruksi.di.DatabaseModule_ProvideMasterKatalogDaoFactory;
import com.ta.konstruksi.di.DatabaseModule_ProvideTransferProyekDaoFactory;
import com.ta.konstruksi.di.DatabaseModule_ProvideUserSessionDaoFactory;
import com.ta.konstruksi.di.FirestoreModule_ProvideFirestoreDataSourceFactory;
import com.ta.konstruksi.di.FirestoreModule_ProvideFirestoreFactory;
import com.ta.konstruksi.domain.repository.AuthRepository;
import com.ta.konstruksi.domain.repository.KatalogRepository;
import com.ta.konstruksi.domain.repository.MutasiRepository;
import com.ta.konstruksi.domain.repository.SyncRepository;
import com.ta.konstruksi.domain.repository.TransferRepository;
import com.ta.konstruksi.domain.usecase.auth.LoginUseCase;
import com.ta.konstruksi.domain.usecase.auth.LogoutUseCase;
import com.ta.konstruksi.domain.usecase.mutasi.RecordMutasiUseCase;
import com.ta.konstruksi.domain.usecase.sync.TriggerSyncUseCase;
import com.ta.konstruksi.presentation.viewmodel.AuthViewModel;
import com.ta.konstruksi.presentation.viewmodel.AuthViewModel_HiltModules;
import com.ta.konstruksi.presentation.viewmodel.DashboardViewModel;
import com.ta.konstruksi.presentation.viewmodel.DashboardViewModel_HiltModules;
import com.ta.konstruksi.presentation.viewmodel.KatalogViewModel;
import com.ta.konstruksi.presentation.viewmodel.KatalogViewModel_HiltModules;
import com.ta.konstruksi.presentation.viewmodel.MutasiViewModel;
import com.ta.konstruksi.presentation.viewmodel.MutasiViewModel_HiltModules;
import com.ta.konstruksi.presentation.viewmodel.RiwayatViewModel;
import com.ta.konstruksi.presentation.viewmodel.RiwayatViewModel_HiltModules;
import com.ta.konstruksi.presentation.viewmodel.SyncViewModel;
import com.ta.konstruksi.presentation.viewmodel.SyncViewModel_HiltModules;
import com.ta.konstruksi.presentation.viewmodel.TransferViewModel;
import com.ta.konstruksi.presentation.viewmodel.TransferViewModel_HiltModules;
import com.ta.konstruksi.sync.SyncScheduler;
import com.ta.konstruksi.sync.worker.SyncWorker;
import com.ta.konstruksi.sync.worker.SyncWorker_AssistedFactory;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerKonstruksiApp_HiltComponents_SingletonC {
  private DaggerKonstruksiApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public KonstruksiApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements KonstruksiApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public KonstruksiApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements KonstruksiApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public KonstruksiApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements KonstruksiApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public KonstruksiApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements KonstruksiApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public KonstruksiApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements KonstruksiApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public KonstruksiApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements KonstruksiApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public KonstruksiApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements KonstruksiApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public KonstruksiApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends KonstruksiApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends KonstruksiApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends KonstruksiApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends KonstruksiApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(7).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_AuthViewModel, AuthViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_DashboardViewModel, DashboardViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_KatalogViewModel, KatalogViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_MutasiViewModel, MutasiViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_RiwayatViewModel, RiwayatViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_SyncViewModel, SyncViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_TransferViewModel, TransferViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_ta_konstruksi_presentation_viewmodel_AuthViewModel = "com.ta.konstruksi.presentation.viewmodel.AuthViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_KatalogViewModel = "com.ta.konstruksi.presentation.viewmodel.KatalogViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_TransferViewModel = "com.ta.konstruksi.presentation.viewmodel.TransferViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_MutasiViewModel = "com.ta.konstruksi.presentation.viewmodel.MutasiViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_SyncViewModel = "com.ta.konstruksi.presentation.viewmodel.SyncViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_RiwayatViewModel = "com.ta.konstruksi.presentation.viewmodel.RiwayatViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_DashboardViewModel = "com.ta.konstruksi.presentation.viewmodel.DashboardViewModel";

      @KeepFieldType
      AuthViewModel com_ta_konstruksi_presentation_viewmodel_AuthViewModel2;

      @KeepFieldType
      KatalogViewModel com_ta_konstruksi_presentation_viewmodel_KatalogViewModel2;

      @KeepFieldType
      TransferViewModel com_ta_konstruksi_presentation_viewmodel_TransferViewModel2;

      @KeepFieldType
      MutasiViewModel com_ta_konstruksi_presentation_viewmodel_MutasiViewModel2;

      @KeepFieldType
      SyncViewModel com_ta_konstruksi_presentation_viewmodel_SyncViewModel2;

      @KeepFieldType
      RiwayatViewModel com_ta_konstruksi_presentation_viewmodel_RiwayatViewModel2;

      @KeepFieldType
      DashboardViewModel com_ta_konstruksi_presentation_viewmodel_DashboardViewModel2;
    }
  }

  private static final class ViewModelCImpl extends KonstruksiApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<DashboardViewModel> dashboardViewModelProvider;

    private Provider<KatalogViewModel> katalogViewModelProvider;

    private Provider<MutasiViewModel> mutasiViewModelProvider;

    private Provider<RiwayatViewModel> riwayatViewModelProvider;

    private Provider<SyncViewModel> syncViewModelProvider;

    private Provider<TransferViewModel> transferViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private LoginUseCase loginUseCase() {
      return new LoginUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private LogoutUseCase logoutUseCase() {
      return new LogoutUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private RecordMutasiUseCase recordMutasiUseCase() {
      return new RecordMutasiUseCase(singletonCImpl.bindMutasiRepositoryProvider.get(), singletonCImpl.bindKatalogRepositoryProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.dashboardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.katalogViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.mutasiViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.riwayatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.syncViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.transferViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(7).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_AuthViewModel, ((Provider) authViewModelProvider)).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_DashboardViewModel, ((Provider) dashboardViewModelProvider)).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_KatalogViewModel, ((Provider) katalogViewModelProvider)).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_MutasiViewModel, ((Provider) mutasiViewModelProvider)).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_RiwayatViewModel, ((Provider) riwayatViewModelProvider)).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_SyncViewModel, ((Provider) syncViewModelProvider)).put(LazyClassKeyProvider.com_ta_konstruksi_presentation_viewmodel_TransferViewModel, ((Provider) transferViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_ta_konstruksi_presentation_viewmodel_RiwayatViewModel = "com.ta.konstruksi.presentation.viewmodel.RiwayatViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_TransferViewModel = "com.ta.konstruksi.presentation.viewmodel.TransferViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_AuthViewModel = "com.ta.konstruksi.presentation.viewmodel.AuthViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_MutasiViewModel = "com.ta.konstruksi.presentation.viewmodel.MutasiViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_SyncViewModel = "com.ta.konstruksi.presentation.viewmodel.SyncViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_KatalogViewModel = "com.ta.konstruksi.presentation.viewmodel.KatalogViewModel";

      static String com_ta_konstruksi_presentation_viewmodel_DashboardViewModel = "com.ta.konstruksi.presentation.viewmodel.DashboardViewModel";

      @KeepFieldType
      RiwayatViewModel com_ta_konstruksi_presentation_viewmodel_RiwayatViewModel2;

      @KeepFieldType
      TransferViewModel com_ta_konstruksi_presentation_viewmodel_TransferViewModel2;

      @KeepFieldType
      AuthViewModel com_ta_konstruksi_presentation_viewmodel_AuthViewModel2;

      @KeepFieldType
      MutasiViewModel com_ta_konstruksi_presentation_viewmodel_MutasiViewModel2;

      @KeepFieldType
      SyncViewModel com_ta_konstruksi_presentation_viewmodel_SyncViewModel2;

      @KeepFieldType
      KatalogViewModel com_ta_konstruksi_presentation_viewmodel_KatalogViewModel2;

      @KeepFieldType
      DashboardViewModel com_ta_konstruksi_presentation_viewmodel_DashboardViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.ta.konstruksi.presentation.viewmodel.AuthViewModel 
          return (T) new AuthViewModel(viewModelCImpl.loginUseCase(), viewModelCImpl.logoutUseCase());

          case 1: // com.ta.konstruksi.presentation.viewmodel.DashboardViewModel 
          return (T) new DashboardViewModel(singletonCImpl.bindKatalogRepositoryProvider.get(), singletonCImpl.bindMutasiRepositoryProvider.get(), singletonCImpl.bindTransferRepositoryProvider.get(), singletonCImpl.bindAuthRepositoryProvider.get(), singletonCImpl.syncSchedulerProvider.get());

          case 2: // com.ta.konstruksi.presentation.viewmodel.KatalogViewModel 
          return (T) new KatalogViewModel(singletonCImpl.bindKatalogRepositoryProvider.get(), singletonCImpl.syncSchedulerProvider.get());

          case 3: // com.ta.konstruksi.presentation.viewmodel.MutasiViewModel 
          return (T) new MutasiViewModel(viewModelCImpl.recordMutasiUseCase(), singletonCImpl.syncSchedulerProvider.get());

          case 4: // com.ta.konstruksi.presentation.viewmodel.RiwayatViewModel 
          return (T) new RiwayatViewModel(singletonCImpl.bindMutasiRepositoryProvider.get(), singletonCImpl.bindTransferRepositoryProvider.get(), singletonCImpl.bindKatalogRepositoryProvider.get());

          case 5: // com.ta.konstruksi.presentation.viewmodel.SyncViewModel 
          return (T) new SyncViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.ta.konstruksi.presentation.viewmodel.TransferViewModel 
          return (T) new TransferViewModel(singletonCImpl.bindTransferRepositoryProvider.get(), singletonCImpl.bindKatalogRepositoryProvider.get(), singletonCImpl.syncSchedulerProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends KonstruksiApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends KonstruksiApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends KonstruksiApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<KonstruksiDatabase> provideDatabaseProvider;

    private Provider<FirebaseFirestore> provideFirestoreProvider;

    private Provider<FirestoreDataSource> provideFirestoreDataSourceProvider;

    private Provider<SyncRepositoryImpl> syncRepositoryImplProvider;

    private Provider<SyncRepository> bindSyncRepositoryProvider;

    private Provider<SyncWorker_AssistedFactory> syncWorker_AssistedFactoryProvider;

    private Provider<AuthRepositoryImpl> authRepositoryImplProvider;

    private Provider<AuthRepository> bindAuthRepositoryProvider;

    private Provider<KatalogRepositoryImpl> katalogRepositoryImplProvider;

    private Provider<KatalogRepository> bindKatalogRepositoryProvider;

    private Provider<MutasiRepositoryImpl> mutasiRepositoryImplProvider;

    private Provider<MutasiRepository> bindMutasiRepositoryProvider;

    private Provider<TransferRepositoryImpl> transferRepositoryImplProvider;

    private Provider<TransferRepository> bindTransferRepositoryProvider;

    private Provider<SyncScheduler> syncSchedulerProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private LogMutasiDao logMutasiDao() {
      return DatabaseModule_ProvideLogMutasiDaoFactory.provideLogMutasiDao(provideDatabaseProvider.get());
    }

    private TransferProyekDao transferProyekDao() {
      return DatabaseModule_ProvideTransferProyekDaoFactory.provideTransferProyekDao(provideDatabaseProvider.get());
    }

    private MasterKatalogDao masterKatalogDao() {
      return DatabaseModule_ProvideMasterKatalogDaoFactory.provideMasterKatalogDao(provideDatabaseProvider.get());
    }

    private UserSessionDao userSessionDao() {
      return DatabaseModule_ProvideUserSessionDaoFactory.provideUserSessionDao(provideDatabaseProvider.get());
    }

    private TriggerSyncUseCase triggerSyncUseCase() {
      return new TriggerSyncUseCase(bindSyncRepositoryProvider.get());
    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return ImmutableMap.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>of("com.ta.konstruksi.sync.worker.SyncWorker", ((Provider) syncWorker_AssistedFactoryProvider));
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<KonstruksiDatabase>(singletonCImpl, 2));
      this.provideFirestoreProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseFirestore>(singletonCImpl, 4));
      this.provideFirestoreDataSourceProvider = DoubleCheck.provider(new SwitchingProvider<FirestoreDataSource>(singletonCImpl, 3));
      this.syncRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 1);
      this.bindSyncRepositoryProvider = DoubleCheck.provider((Provider) syncRepositoryImplProvider);
      this.syncWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<SyncWorker_AssistedFactory>(singletonCImpl, 0));
      this.authRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 5);
      this.bindAuthRepositoryProvider = DoubleCheck.provider((Provider) authRepositoryImplProvider);
      this.katalogRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 6);
      this.bindKatalogRepositoryProvider = DoubleCheck.provider((Provider) katalogRepositoryImplProvider);
      this.mutasiRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 7);
      this.bindMutasiRepositoryProvider = DoubleCheck.provider((Provider) mutasiRepositoryImplProvider);
      this.transferRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 8);
      this.bindTransferRepositoryProvider = DoubleCheck.provider((Provider) transferRepositoryImplProvider);
      this.syncSchedulerProvider = DoubleCheck.provider(new SwitchingProvider<SyncScheduler>(singletonCImpl, 9));
    }

    @Override
    public void injectKonstruksiApp(KonstruksiApp konstruksiApp) {
      injectKonstruksiApp2(konstruksiApp);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @CanIgnoreReturnValue
    private KonstruksiApp injectKonstruksiApp2(KonstruksiApp instance) {
      KonstruksiApp_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.ta.konstruksi.sync.worker.SyncWorker_AssistedFactory 
          return (T) new SyncWorker_AssistedFactory() {
            @Override
            public SyncWorker create(Context context, WorkerParameters workerParams) {
              return new SyncWorker(context, workerParams, singletonCImpl.triggerSyncUseCase());
            }
          };

          case 1: // com.ta.konstruksi.data.repository.SyncRepositoryImpl 
          return (T) new SyncRepositoryImpl(singletonCImpl.logMutasiDao(), singletonCImpl.transferProyekDao(), singletonCImpl.masterKatalogDao(), singletonCImpl.userSessionDao(), singletonCImpl.provideFirestoreDataSourceProvider.get());

          case 2: // com.ta.konstruksi.data.local.KonstruksiDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.ta.konstruksi.data.remote.FirestoreDataSource 
          return (T) FirestoreModule_ProvideFirestoreDataSourceFactory.provideFirestoreDataSource(singletonCImpl.provideFirestoreProvider.get());

          case 4: // com.google.firebase.firestore.FirebaseFirestore 
          return (T) FirestoreModule_ProvideFirestoreFactory.provideFirestore();

          case 5: // com.ta.konstruksi.data.repository.AuthRepositoryImpl 
          return (T) new AuthRepositoryImpl(singletonCImpl.userSessionDao());

          case 6: // com.ta.konstruksi.data.repository.KatalogRepositoryImpl 
          return (T) new KatalogRepositoryImpl(singletonCImpl.masterKatalogDao());

          case 7: // com.ta.konstruksi.data.repository.MutasiRepositoryImpl 
          return (T) new MutasiRepositoryImpl(singletonCImpl.logMutasiDao());

          case 8: // com.ta.konstruksi.data.repository.TransferRepositoryImpl 
          return (T) new TransferRepositoryImpl(singletonCImpl.transferProyekDao());

          case 9: // com.ta.konstruksi.sync.SyncScheduler 
          return (T) new SyncScheduler(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
