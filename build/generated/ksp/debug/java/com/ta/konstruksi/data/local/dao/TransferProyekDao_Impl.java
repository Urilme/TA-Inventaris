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
import com.ta.konstruksi.data.local.entity.TransferProyekEntity;
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
public final class TransferProyekDao_Impl implements TransferProyekDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TransferProyekEntity> __insertionAdapterOfTransferProyekEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStatus;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  public TransferProyekDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransferProyekEntity = new EntityInsertionAdapter<TransferProyekEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transfer_proyek` (`id_transfer`,`sku_id`,`proyek_asal`,`proyek_tujuan`,`jumlah`,`status_transfer`,`local_timestamp`,`sync_status`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransferProyekEntity entity) {
        statement.bindString(1, entity.getId_transfer());
        statement.bindString(2, entity.getSku_id());
        statement.bindString(3, entity.getProyek_asal());
        statement.bindString(4, entity.getProyek_tujuan());
        statement.bindLong(5, entity.getJumlah());
        statement.bindString(6, entity.getStatus_transfer());
        statement.bindLong(7, entity.getLocal_timestamp());
        statement.bindString(8, entity.getSync_status());
      }
    };
    this.__preparedStmtOfUpdateStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE transfer_proyek SET status_transfer = ?, sync_status = 'PENDING' WHERE id_transfer = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE transfer_proyek SET sync_status = 'SYNCED' WHERE id_transfer = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final TransferProyekEntity transfer,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransferProyekEntity.insert(transfer);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStatus(final String id, final String status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
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
          __preparedStmtOfUpdateStatus.release(_stmt);
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
  public Flow<List<TransferProyekEntity>> getAllTransfer() {
    final String _sql = "SELECT * FROM transfer_proyek ORDER BY local_timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transfer_proyek"}, new Callable<List<TransferProyekEntity>>() {
      @Override
      @NonNull
      public List<TransferProyekEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfIdTransfer = CursorUtil.getColumnIndexOrThrow(_cursor, "id_transfer");
          final int _cursorIndexOfSkuId = CursorUtil.getColumnIndexOrThrow(_cursor, "sku_id");
          final int _cursorIndexOfProyekAsal = CursorUtil.getColumnIndexOrThrow(_cursor, "proyek_asal");
          final int _cursorIndexOfProyekTujuan = CursorUtil.getColumnIndexOrThrow(_cursor, "proyek_tujuan");
          final int _cursorIndexOfJumlah = CursorUtil.getColumnIndexOrThrow(_cursor, "jumlah");
          final int _cursorIndexOfStatusTransfer = CursorUtil.getColumnIndexOrThrow(_cursor, "status_transfer");
          final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "local_timestamp");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_status");
          final List<TransferProyekEntity> _result = new ArrayList<TransferProyekEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransferProyekEntity _item;
            final String _tmpId_transfer;
            _tmpId_transfer = _cursor.getString(_cursorIndexOfIdTransfer);
            final String _tmpSku_id;
            _tmpSku_id = _cursor.getString(_cursorIndexOfSkuId);
            final String _tmpProyek_asal;
            _tmpProyek_asal = _cursor.getString(_cursorIndexOfProyekAsal);
            final String _tmpProyek_tujuan;
            _tmpProyek_tujuan = _cursor.getString(_cursorIndexOfProyekTujuan);
            final int _tmpJumlah;
            _tmpJumlah = _cursor.getInt(_cursorIndexOfJumlah);
            final String _tmpStatus_transfer;
            _tmpStatus_transfer = _cursor.getString(_cursorIndexOfStatusTransfer);
            final long _tmpLocal_timestamp;
            _tmpLocal_timestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
            final String _tmpSync_status;
            _tmpSync_status = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new TransferProyekEntity(_tmpId_transfer,_tmpSku_id,_tmpProyek_asal,_tmpProyek_tujuan,_tmpJumlah,_tmpStatus_transfer,_tmpLocal_timestamp,_tmpSync_status);
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
  public Object getPending(final Continuation<? super List<TransferProyekEntity>> $completion) {
    final String _sql = "SELECT * FROM transfer_proyek WHERE sync_status = 'PENDING'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TransferProyekEntity>>() {
      @Override
      @NonNull
      public List<TransferProyekEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfIdTransfer = CursorUtil.getColumnIndexOrThrow(_cursor, "id_transfer");
          final int _cursorIndexOfSkuId = CursorUtil.getColumnIndexOrThrow(_cursor, "sku_id");
          final int _cursorIndexOfProyekAsal = CursorUtil.getColumnIndexOrThrow(_cursor, "proyek_asal");
          final int _cursorIndexOfProyekTujuan = CursorUtil.getColumnIndexOrThrow(_cursor, "proyek_tujuan");
          final int _cursorIndexOfJumlah = CursorUtil.getColumnIndexOrThrow(_cursor, "jumlah");
          final int _cursorIndexOfStatusTransfer = CursorUtil.getColumnIndexOrThrow(_cursor, "status_transfer");
          final int _cursorIndexOfLocalTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "local_timestamp");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "sync_status");
          final List<TransferProyekEntity> _result = new ArrayList<TransferProyekEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransferProyekEntity _item;
            final String _tmpId_transfer;
            _tmpId_transfer = _cursor.getString(_cursorIndexOfIdTransfer);
            final String _tmpSku_id;
            _tmpSku_id = _cursor.getString(_cursorIndexOfSkuId);
            final String _tmpProyek_asal;
            _tmpProyek_asal = _cursor.getString(_cursorIndexOfProyekAsal);
            final String _tmpProyek_tujuan;
            _tmpProyek_tujuan = _cursor.getString(_cursorIndexOfProyekTujuan);
            final int _tmpJumlah;
            _tmpJumlah = _cursor.getInt(_cursorIndexOfJumlah);
            final String _tmpStatus_transfer;
            _tmpStatus_transfer = _cursor.getString(_cursorIndexOfStatusTransfer);
            final long _tmpLocal_timestamp;
            _tmpLocal_timestamp = _cursor.getLong(_cursorIndexOfLocalTimestamp);
            final String _tmpSync_status;
            _tmpSync_status = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new TransferProyekEntity(_tmpId_transfer,_tmpSku_id,_tmpProyek_asal,_tmpProyek_tujuan,_tmpJumlah,_tmpStatus_transfer,_tmpLocal_timestamp,_tmpSync_status);
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
