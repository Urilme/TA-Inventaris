package com.ta.konstruksi.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.ta.konstruksi.data.local.entity.MasterKatalogEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MasterKatalogDao_Impl implements MasterKatalogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MasterKatalogEntity> __insertionAdapterOfMasterKatalogEntity;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStok;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  public MasterKatalogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMasterKatalogEntity = new EntityInsertionAdapter<MasterKatalogEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `master_katalog` (`sku_id`,`kategori`,`nama_barang`,`satuan`,`stok_lokal`,`sync_status`,`local_timestamp`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MasterKatalogEntity entity) {
        statement.bindString(1, entity.getSku_id());
        statement.bindString(2, entity.getKategori());
        statement.bindString(3, entity.getNama_barang());
        statement.bindString(4, entity.getSatuan());
        statement.bindLong(5, entity.getStok_lokal());
        statement.bindString(6, entity.getSync_status());
        statement.bindLong(7, entity.getLocal_timestamp());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM master_katalog WHERE sku_id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateStok = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE master_katalog SET stok_lokal = stok_lokal + ?, sync_status = 'PENDING' WHERE sku_id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE master_katalog SET sync_status = 'SYNCED' WHERE sku_id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final MasterKatalogEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMasterKatalogEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final String skuId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, skuId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStok(final String skuId, final int delta,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStok.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, delta);
        _argIndex = 2;
        _stmt.bindString(_argIndex, skuId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateStok.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final String skuId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, skuId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MasterKatalogEntity>> getAllKatalog() {
    final String _sql = "SELECT * FROM master_katalog ORDER BY nama_barang ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"master_katalog"}, new Callable<List<MasterKatalogEntity>>() {
      @Override
      @NonNull
      public List<MasterKatalogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSkuId = CursorUtil.getColumnIndexOrThrow(_cursor, "sku_id");
          final int _cursorIndexOfKategori = CursorUtil.getColumnIndexOrThrow(_cursor, "kategori");
          final int _cursorIndexOfNamaBarang = CursorUtil.getColumnIndexOrThrow(_cursor, "nama_barang");
          final int _cursorIndexOfSatuan = CursorUtil.getColumnIndexOrThrow(_cursor, "satuan");
          final int _cursorIndexOfStokLokal = CursorUtil.getColumnIndexOrThrow(_cursor, "stok_lokal");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_status");
          final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "local_timestamp");
          final List<MasterKatalogEntity> _result = new ArrayList<MasterKatalogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MasterKatalogEntity _item;
            final String _tmpSku_id;
            _tmpSku_id = _cursor.getString(_cursorIndexOfSkuId);
            final String _tmpKategori;
            _tmpKategori = _cursor.getString(_cursorIndexOfKategori);
            final String _tmpNama_barang;
            _tmpNama_barang = _cursor.getString(_cursorIndexOfNamaBarang);
            final String _tmpSatuan;
            _tmpSatuan = _cursor.getString(_cursorIndexOfSatuan);
            final int _tmpStok_lokal;
            _tmpStok_lokal = _cursor.getInt(_cursorIndexOfStokLokal);
            final String _tmpSync_status;
            _tmpSync_status = _cursor.getString(_cursorIndexOfSyncStatus);
            final long _tmpLocal_timestamp;
            _tmpLocal_timestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
            _item = new MasterKatalogEntity(_tmpSku_id,_tmpKategori,_tmpNama_barang,_tmpSatuan,_tmpStok_lokal,_tmpSync_status,_tmpLocal_timestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getKatalogBySku(final String skuId,
      final Continuation<? super MasterKatalogEntity> $completion) {
    final String _sql = "SELECT * FROM master_katalog WHERE sku_id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, skuId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MasterKatalogEntity>() {
      @Override
      @Nullable
      public MasterKatalogEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSkuId = CursorUtil.getColumnIndexOrThrow(_cursor, "sku_id");
          final int _cursorIndexOfKategori = CursorUtil.getColumnIndexOrThrow(_cursor, "kategori");
          final int _cursorIndexOfNamaBarang = CursorUtil.getColumnIndexOrThrow(_cursor, "nama_barang");
          final int _cursorIndexOfSatuan = CursorUtil.getColumnIndexOrThrow(_cursor, "satuan");
          final int _cursorIndexOfStokLokal = CursorUtil.getColumnIndexOrThrow(_cursor, "stok_lokal");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_status");
          final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "local_timestamp");
          final MasterKatalogEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpSku_id;
            _tmpSku_id = _cursor.getString(_cursorIndexOfSkuId);
            final String _tmpKategori;
            _tmpKategori = _cursor.getString(_cursorIndexOfKategori);
            final String _tmpNama_barang;
            _tmpNama_barang = _cursor.getString(_cursorIndexOfNamaBarang);
            final String _tmpSatuan;
            _tmpSatuan = _cursor.getString(_cursorIndexOfSatuan);
            final int _tmpStok_lokal;
            _tmpStok_lokal = _cursor.getInt(_cursorIndexOfStokLokal);
            final String _tmpSync_status;
            _tmpSync_status = _cursor.getString(_cursorIndexOfSyncStatus);
            final long _tmpLocal_timestamp;
            _tmpLocal_timestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
            _result = new MasterKatalogEntity(_tmpSku_id,_tmpKategori,_tmpNama_barang,_tmpSatuan,_tmpStok_lokal,_tmpSync_status,_tmpLocal_timestamp);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingSync(final Continuation<? super List<MasterKatalogEntity>> $completion) {
    final String _sql = "SELECT * FROM master_katalog WHERE sync_status = 'PENDING'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MasterKatalogEntity>>() {
      @Override
      @NonNull
      public List<MasterKatalogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSkuId = CursorUtil.getColumnIndexOrThrow(_cursor, "sku_id");
          final int _cursorIndexOfKategori = CursorUtil.getColumnIndexOrThrow(_cursor, "kategori");
          final int _cursorIndexOfNamaBarang = CursorUtil.getColumnIndexOrThrow(_cursor, "nama_barang");
          final int _cursorIndexOfSatuan = CursorUtil.getColumnIndexOrThrow(_cursor, "satuan");
          final int _cursorIndexOfStokLokal = CursorUtil.getColumnIndexOrThrow(_cursor, "stok_lokal");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_status");
          final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "local_timestamp");
          final List<MasterKatalogEntity> _result = new ArrayList<MasterKatalogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MasterKatalogEntity _item;
            final String _tmpSku_id;
            _tmpSku_id = _cursor.getString(_cursorIndexOfSkuId);
            final String _tmpKategori;
            _tmpKategori = _cursor.getString(_cursorIndexOfKategori);
            final String _tmpNama_barang;
            _tmpNama_barang = _cursor.getString(_cursorIndexOfNamaBarang);
            final String _tmpSatuan;
            _tmpSatuan = _cursor.getString(_cursorIndexOfSatuan);
            final int _tmpStok_lokal;
            _tmpStok_lokal = _cursor.getInt(_cursorIndexOfStokLokal);
            final String _tmpSync_status;
            _tmpSync_status = _cursor.getString(_cursorIndexOfSyncStatus);
            final long _tmpLocal_timestamp;
            _tmpLocal_timestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
            _item = new MasterKatalogEntity(_tmpSku_id,_tmpKategori,_tmpNama_barang,_tmpSatuan,_tmpStok_lokal,_tmpSync_status,_tmpLocal_timestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
