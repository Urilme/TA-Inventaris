package com.ta.konstruksi.presentation.viewmodel;

import com.ta.konstruksi.domain.usecase.auth.LoginUseCase;
import com.ta.konstruksi.domain.usecase.auth.LogoutUseCase;
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<LoginUseCase> loginUseCaseProvider;

  private final Provider<LogoutUseCase> logoutUseCaseProvider;

  public AuthViewModel_Factory(Provider<LoginUseCase> loginUseCaseProvider,
      Provider<LogoutUseCase> logoutUseCaseProvider) {
    this.loginUseCaseProvider = loginUseCaseProvider;
    this.logoutUseCaseProvider = logoutUseCaseProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(loginUseCaseProvider.get(), logoutUseCaseProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<LoginUseCase> loginUseCaseProvider,
      Provider<LogoutUseCase> logoutUseCaseProvider) {
    return new AuthViewModel_Factory(loginUseCaseProvider, logoutUseCaseProvider);
  }

  public static AuthViewModel newInstance(LoginUseCase loginUseCase, LogoutUseCase logoutUseCase) {
    return new AuthViewModel(loginUseCase, logoutUseCase);
  }
}
