package com.ta.konstruksi.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.ta.konstruksi.data.local.dao.LogMutasiDao;
import com.ta.konstruksi.data.local.dao.LogMutasiDao_Impl;
import com.ta.konstruksi.data.local.dao.MasterKatalogDao;
import com.ta.konstruksi.data.local.dao.MasterKatalogDao_Impl;
import com.ta.konstruksi.data.local.dao.TransferProyekDao;
import com.ta.konstruksi.data.local.dao.TransferProyekDao_Impl;
import com.ta.konstruksi.data.local.dao.UserSessionDao;
import com.ta.konstruksi.data.local.dao.UserSessionDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class KonstruksiDatabase_Impl extends KonstruksiDatabase {
  private volatile UserSessionDao _userSessionDao;

  private volatile MasterKatalogDao _masterKatalogDao;

  private volatile LogMutasiDao _logMutasiDao;

  private volatile TransferProyekDao _transferProyekDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_session` (`id_mandor` TEXT NOT NULL, `nama` TEXT NOT NULL, `email` TEXT NOT NULL, `role` TEXT NOT NULL, `auth_token` TEXT NOT NULL, `last_sync_timestamp` INTEGER NOT NULL, PRIMARY KEY(`id_mandor`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `master_katalog` (`sku_id` TEXT NOT NULL, `kategori` TEXT NOT NULL, `nama_barang` TEXT NOT NULL, `satuan` TEXT NOT NULL, `stok_lokal` INTEGER NOT NULL, `sync_status` TEXT NOT NULL, `local_timestamp` INTEGER NOT NULL, PRIMARY KEY(`sku_id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `log_mutasi` (`id_transaksi` TEXT NOT NULL, `sku_id` TEXT NOT NULL, `jenis_mutasi` TEXT NOT NULL, `jumlah` INTEGER NOT NULL, `catatan` TEXT, `local_timestamp` INTEGER NOT NULL, `sync_status` TEXT NOT NULL, PRIMARY KEY(`id_transaksi`), FOREIGN KEY(`sku_id`) REFERENCES `master_katalog`(`sku_id`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `transfer_proyek` (`id_transfer` TEXT NOT NULL, `sku_id` TEXT NOT NULL, `proyek_asal` TEXT NOT NULL, `proyek_tujuan` TEXT NOT NULL, `jumlah` INTEGER NOT NULL, `status_transfer` TEXT NOT NULL, `local_timestamp` INTEGER NOT NULL, `sync_status` TEXT NOT NULL, PRIMARY KEY(`id_transfer`), FOREIGN KEY(`sku_id`) REFERENCES `master_katalog`(`sku_id`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '706b0faf3bcb0273d3ce957074e64a7b')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `user_session`");
        db.execSQL("DROP TABLE IF EXISTS `master_katalog`");
        db.execSQL("DROP TABLE IF EXISTS `log_mutasi`");
        db.execSQL("DROP TABLE IF EXISTS `transfer_proyek`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUserSession = new HashMap<String, TableInfo.Column>(6);
        _columnsUserSession.put("id_mandor", new TableInfo.Column("id_mandor", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSession.put("nama", new TableInfo.Column("nama", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSession.put("email", new TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSession.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSession.put("auth_token", new TableInfo.Column("auth_token", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserSession.put("last_sync_timestamp", new TableInfo.Column("last_sync_timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserSession = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserSession = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserSession = new TableInfo("user_session", _columnsUserSession, _foreignKeysUserSession, _indicesUserSession);
        final TableInfo _existingUserSession = TableInfo.read(db, "user_session");
        if (!_infoUserSession.equals(_existingUserSession)) {
          return new RoomOpenHelper.ValidationResult(false, "user_session(com.ta.konstruksi.data.local.entity.UserSessionEntity).\n"
                  + " Expected:\n" + _infoUserSession + "\n"
                  + " Found:\n" + _existingUserSession);
        }
        final HashMap<String, TableInfo.Column> _columnsMasterKatalog = new HashMap<String, TableInfo.Column>(7);
        _columnsMasterKatalog.put("sku_id", new TableInfo.Column("sku_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMasterKatalog.put("kategori", new TableInfo.Column("kategori", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMasterKatalog.put("nama_barang", new TableInfo.Column("nama_barang", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMasterKatalog.put("satuan", new TableInfo.Column("satuan", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMasterKatalog.put("stok_lokal", new TableInfo.Column("stok_lokal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMasterKatalog.put("sync_status", new TableInfo.Column("sync_status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMasterKatalog.put("local_timestamp", new TableInfo.Column("local_timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMasterKatalog = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMasterKatalog = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMasterKatalog = new TableInfo("master_katalog", _columnsMasterKatalog, _foreignKeysMasterKatalog, _indicesMasterKatalog);
        final TableInfo _existingMasterKatalog = TableInfo.read(db, "master_katalog");
        if (!_infoMasterKatalog.equals(_existingMasterKatalog)) {
          return new RoomOpenHelper.ValidationResult(false, "master_katalog(com.ta.konstruksi.data.local.entity.MasterKatalogEntity).\n"
                  + " Expected:\n" + _infoMasterKatalog + "\n"
                  + " Found:\n" + _existingMasterKatalog);
        }
        final HashMap<String, TableInfo.Column> _columnsLogMutasi = new HashMap<String, TableInfo.Column>(7);
        _columnsLogMutasi.put("id_transaksi", new TableInfo.Column("id_transaksi", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogMutasi.put("sku_id", new TableInfo.Column("sku_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogMutasi.put("jenis_mutasi", new TableInfo.Column("jenis_mutasi", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogMutasi.put("jumlah", new TableInfo.Column("jumlah", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogMutasi.put("catatan", new TableInfo.Column("catatan", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogMutasi.put("local_timestamp", new TableInfo.Column("local_timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLogMutasi.put("sync_status", new TableInfo.Column("sync_status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLogMutasi = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysLogMutasi.add(new TableInfo.ForeignKey("master_katalog", "RESTRICT", "NO ACTION", Arrays.asList("sku_id"), Arrays.asList("sku_id")));
        final HashSet<TableInfo.Index> _indicesLogMutasi = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLogMutasi = new TableInfo("log_mutasi", _columnsLogMutasi, _foreignKeysLogMutasi, _indicesLogMutasi);
        final TableInfo _existingLogMutasi = TableInfo.read(db, "log_mutasi");
        if (!_infoLogMutasi.equals(_existingLogMutasi)) {
          return new RoomOpenHelper.ValidationResult(false, "log_mutasi(com.ta.konstruksi.data.local.entity.LogMutasiEntity).\n"
                  + " Expected:\n" + _infoLogMutasi + "\n"
                  + " Found:\n" + _existingLogMutasi);
        }
        final HashMap<String, TableInfo.Column> _columnsTransferProyek = new HashMap<String, TableInfo.Column>(8);
        _columnsTransferProyek.put("id_transfer", new TableInfo.Column("id_transfer", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransferProyek.put("sku_id", new TableInfo.Column("sku_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransferProyek.put("proyek_asal", new TableInfo.Column("proyek_asal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransferProyek.put("proyek_tujuan", new TableInfo.Column("proyek_tujuan", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransferProyek.put("jumlah", new TableInfo.Column("jumlah", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransferProyek.put("status_transfer", new TableInfo.Column("status_transfer", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransferProyek.put("local_timestamp", new TableInfo.Column("local_timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransferProyek.put("sync_status", new TableInfo.Column("sync_status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTransferProyek = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysTransferProyek.add(new TableInfo.ForeignKey("master_katalog", "RESTRICT", "NO ACTION", Arrays.asList("sku_id"), Arrays.asList("sku_id")));
        final HashSet<TableInfo.Index> _indicesTransferProyek = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTransferProyek = new TableInfo("transfer_proyek", _columnsTransferProyek, _foreignKeysTransferProyek, _indicesTransferProyek);
        final TableInfo _existingTransferProyek = TableInfo.read(db, "transfer_proyek");
        if (!_infoTransferProyek.equals(_existingTransferProyek)) {
          return new RoomOpenHelper.ValidationResult(false, "transfer_proyek(com.ta.konstruksi.data.local.entity.TransferProyekEntity).\n"
                  + " Expected:\n" + _infoTransferProyek + "\n"
                  + " Found:\n" + _existingTransferProyek);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "706b0faf3bcb0273d3ce957074e64a7b", "db3f626d90f6d5ee9561e4cb9a903d88");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "user_session","master_katalog","log_mutasi","transfer_proyek");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `user_session`");
      _db.execSQL("DELETE FROM `log_mutasi`");
      _db.execSQL("DELETE FROM `transfer_proyek`");
      _db.execSQL("DELETE FROM `master_katalog`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserSessionDao.class, UserSessionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MasterKatalogDao.class, MasterKatalogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LogMutasiDao.class, LogMutasiDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TransferProyekDao.class, TransferProyekDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserSessionDao userSessionDao() {
    if (_userSessionDao != null) {
      return _userSessionDao;
    } else {
      synchronized(this) {
        if(_userSessionDao == null) {
          _userSessionDao = new UserSessionDao_Impl(this);
        }
        return _userSessionDao;
      }
    }
  }

  @Override
  public MasterKatalogDao masterKatalogDao() {
    if (_masterKatalogDao != null) {
      return _masterKatalogDao;
    } else {
      synchronized(this) {
        if(_masterKatalogDao == null) {
          _masterKatalogDao = new MasterKatalogDao_Impl(this);
        }
        return _masterKatalogDao;
      }
    }
  }

  @Override
  public LogMutasiDao logMutasiDao() {
    if (_logMutasiDao != null) {
      return _logMutasiDao;
    } else {
      synchronized(this) {
        if(_logMutasiDao == null) {
          _logMutasiDao = new LogMutasiDao_Impl(this);
        }
        return _logMutasiDao;
      }
    }
  }

  @Override
  public TransferProyekDao transferProyekDao() {
    if (_transferProyekDao != null) {
      return _transferProyekDao;
    } else {
      synchronized(this) {
        if(_transferProyekDao == null) {
          _transferProyekDao = new TransferProyekDao_Impl(this);
        }
        return _transferProyekDao;
      }
    }
  }
}
