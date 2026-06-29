package com.ta.konstruksi.data.repository;

import com.ta.konstruksi.data.local.dao.UserSessionDao;
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<UserSessionDao> userSessionDaoProvider;

  public AuthRepositoryImpl_Factory(Provider<UserSessionDao> userSessionDaoProvider) {
    this.userSessionDaoProvider = userSessionDaoProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(userSessionDaoProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<UserSessionDao> userSessionDaoProvider) {
    return new AuthRepositoryImpl_Factory(userSessionDaoProvider);
  }

  public static AuthRepositoryImpl newInstance(UserSessionDao userSessionDao) {
    return new AuthRepositoryImpl(userSessionDao);
  }
}
