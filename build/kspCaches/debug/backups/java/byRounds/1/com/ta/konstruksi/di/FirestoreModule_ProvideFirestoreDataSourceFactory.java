package com.ta.konstruksi.di;

import com.google.firebase.firestore.FirebaseFirestore;
import com.ta.konstruksi.data.remote.FirestoreDataSource;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class FirestoreModule_ProvideFirestoreDataSourceFactory implements Factory<FirestoreDataSource> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  public FirestoreModule_ProvideFirestoreDataSourceFactory(
      Provider<FirebaseFirestore> firestoreProvider) {
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public FirestoreDataSource get() {
    return provideFirestoreDataSource(firestoreProvider.get());
  }

  public static FirestoreModule_ProvideFirestoreDataSourceFactory create(
      Provider<FirebaseFirestore> firestoreProvider) {
    return new FirestoreModule_ProvideFirestoreDataSourceFactory(firestoreProvider);
  }

  public static FirestoreDataSource provideFirestoreDataSource(FirebaseFirestore firestore) {
    return Preconditions.checkNotNullFromProvides(FirestoreModule.INSTANCE.provideFirestoreDataSource(firestore));
  }
}
