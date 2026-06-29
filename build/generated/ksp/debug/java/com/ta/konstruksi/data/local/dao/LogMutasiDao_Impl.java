package com.ta.konstruksi.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.ta.konstruksi.data.local.entity.LogMutasiEntity;
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
public final class LogMutasiDao_Impl implements LogMutasiDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LogMutasiEntity> __insertionAdapterOfLogMutasiEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  public LogMutasiDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLogMutasiEntity = new EntityInsertionAdapter<LogMutasiEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `log_mutasi` (`id_transaksi`,`sku_id`,`jenis_mutasi`,`jumlah`,`catatan`,`local_timestamp`,`sync_status`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LogMutasiEntity entity) {
        statement.bindString(1, entity.getId_transaksi());
        statement.bindString(2, entity.getSku_id());
        statement.bindString(3, entity.getJenis_mutasi());
        statement.bindLong(4, entity.getJumlah());
        if (entity.getCatatan() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCatatan());
        }
        statement.bindLong(6, entity.getLocal_timestamp());
        statement.bindString(7, entity.getSync_status());
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE log_mutasi SET sync_status = 'SYNCED' WHERE id_transaksi = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final LogMutasiEntity mutasi, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLogMutasiEntity.insert(mutasi);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
  public Flow<List<LogMutasiEntity>> getAllMutasi() {
    final String _sql = "SELECT * FROM log_mutasi ORDER BY local_timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"log_mutasi"}, new Callable<List<LogMutasiEntity>>() {
      @Override
      @NonNull
      public List<LogMutasiEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfIdTransaksi = CursorUtil.getColumnIndexOrThrow(_cursor, "id_transaksi");
          final int _cursorIndexOfSkuId = CursorUtil.getColumnIndexOrThrow(_cursor, "sku_id");
          final int _cursorIndexOfJenisMutasi = CursorUtil.getColumnIndexOrThrow(_cursor, "jenis_mutasi");
          final int _cursorIndexOfJumlah = CursorUtil.getColumnIndexOrThrow(_cursor, "jumlah");
          final int _cursorIndexOfCatatan = CursorUtil.getColumnIndexOrThrow(_cursor, "catatan");
          final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "local_timestamp");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_status");
          final List<LogMutasiEntity> _result = new ArrayList<LogMutasiEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogMutasiEntity _item;
            final String _tmpId_transaksi;
            _tmpId_transaksi = _cursor.getString(_cursorIndexOfIdTransaksi);
            final String _tmpSku_id;
            _tmpSku_id = _cursor.getString(_cursorIndexOfSkuId);
            final String _tmpJenis_mutasi;
            _tmpJenis_mutasi = _cursor.getString(_cursorIndexOfJenisMutasi);
            final int _tmpJumlah;
            _tmpJumlah = _cursor.getInt(_cursorIndexOfJumlah);
            final String _tmpCatatan;
            if (_cursor.isNull(_cursorIndexOfCatatan)) {
              _tmpCatatan = null;
            } else {
              _tmpCatatan = _cursor.getString(_cursorIndexOfCatatan);
            }
            final long _tmpLocal_timestamp;
            _tmpLocal_timestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
            final String _tmpSync_status;
            _tmpSync_status = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new LogMutasiEntity(_tmpId_transaksi,_tmpSku_id,_tmpJenis_mutasi,_tmpJumlah,_tmpCatatan,_tmpLocal_timestamp,_tmpSync_status);
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
  public Object getPending(final Continuation<? super List<LogMutasiEntity>> $completion) {
    final String _sql = "SELECT * FROM log_mutasi WHERE sync_status = 'PENDING'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LogMutasiEntity>>() {
      @Override
      @NonNull
      public List<LogMutasiEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfIdTransaksi = CursorUtil.getColumnIndexOrThrow(_cursor, "id_transaksi");
          final int _cursorIndexOfSkuId = CursorUtil.getColumnIndexOrThrow(_cursor, "sku_id");
          final int _cursorIndexOfJenisMutasi = CursorUtil.getColumnIndexOrThrow(_cursor, "jenis_mutasi");
          final int _cursorIndexOfJumlah = CursorUtil.getColumnIndexOrThrow(_cursor, "jumlah");
          final int _cursorIndexOfCatatan = CursorUtil.getColumnIndexOrThrow(_cursor, "catatan");
          final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "local_timestamp");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_status");
          final List<LogMutasiEntity> _result = new ArrayList<LogMutasiEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LogMutasiEntity _item;
            final String _tmpId_transaksi;
            _tmpId_transaksi = _cursor.getString(_cursorIndexOfIdTransaksi);
            final String _tmpSku_id;
            _tmpSku_id = _cursor.getString(_cursorIndexOfSkuId);
            final String _tmpJenis_mutasi;
            _tmpJenis_mutasi = _cursor.getString(_cursorIndexOfJenisMutasi);
            final int _tmpJumlah;
            _tmpJumlah = _cursor.getInt(_cursorIndexOfJumlah);
            final String _tmpCatatan;
            if (_cursor.isNull(_cursorIndexOfCatatan)) {
              _tmpCatatan = null;
            } else {
              _tmpCatatan = _cursor.getString(_cursorIndexOfCatatan);
            }
            final long _tmpLocal_timestamp;
            _tmpLocal_timestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
            final String _tmpSync_status;
            _tmpSync_status = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new LogMutasiEntity(_tmpId_transaksi,_tmpSku_id,_tmpJenis_mutasi,_tmpJumlah,_tmpCatatan,_tmpLocal_timestamp,_tmpSync_status);
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
