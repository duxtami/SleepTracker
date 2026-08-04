package com.sleeptracker.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.sleeptracker.app.data.local.Converters;
import com.sleeptracker.app.data.local.entity.SleepSessionEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
public final class SleepSessionDao_Impl implements SleepSessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SleepSessionEntity> __insertionAdapterOfSleepSessionEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<SleepSessionEntity> __deletionAdapterOfSleepSessionEntity;

  private final EntityDeletionOrUpdateAdapter<SleepSessionEntity> __updateAdapterOfSleepSessionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public SleepSessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSleepSessionEntity = new EntityInsertionAdapter<SleepSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `sleep_sessions` (`id`,`startEpochMillis`,`endEpochMillis`,`timeZoneId`,`mood`,`notes`,`tags`,`isManualEntry`,`awakeMinutes`,`qualityRating`,`startDelayMinutesUsed`,`pausedAtEpochMillis`,`totalPausedMillis`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SleepSessionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getStartEpochMillis());
        if (entity.getEndEpochMillis() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getEndEpochMillis());
        }
        statement.bindString(4, entity.getTimeZoneId());
        if (entity.getMood() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getMood());
        }
        statement.bindString(6, entity.getNotes());
        final String _tmp = __converters.fromTagList(entity.getTags());
        statement.bindString(7, _tmp);
        final int _tmp_1 = entity.isManualEntry() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
        statement.bindLong(9, entity.getAwakeMinutes());
        if (entity.getQualityRating() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getQualityRating());
        }
        statement.bindLong(11, entity.getStartDelayMinutesUsed());
        if (entity.getPausedAtEpochMillis() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getPausedAtEpochMillis());
        }
        statement.bindLong(13, entity.getTotalPausedMillis());
      }
    };
    this.__deletionAdapterOfSleepSessionEntity = new EntityDeletionOrUpdateAdapter<SleepSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `sleep_sessions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SleepSessionEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSleepSessionEntity = new EntityDeletionOrUpdateAdapter<SleepSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sleep_sessions` SET `id` = ?,`startEpochMillis` = ?,`endEpochMillis` = ?,`timeZoneId` = ?,`mood` = ?,`notes` = ?,`tags` = ?,`isManualEntry` = ?,`awakeMinutes` = ?,`qualityRating` = ?,`startDelayMinutesUsed` = ?,`pausedAtEpochMillis` = ?,`totalPausedMillis` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SleepSessionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getStartEpochMillis());
        if (entity.getEndEpochMillis() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getEndEpochMillis());
        }
        statement.bindString(4, entity.getTimeZoneId());
        if (entity.getMood() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getMood());
        }
        statement.bindString(6, entity.getNotes());
        final String _tmp = __converters.fromTagList(entity.getTags());
        statement.bindString(7, _tmp);
        final int _tmp_1 = entity.isManualEntry() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
        statement.bindLong(9, entity.getAwakeMinutes());
        if (entity.getQualityRating() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getQualityRating());
        }
        statement.bindLong(11, entity.getStartDelayMinutesUsed());
        if (entity.getPausedAtEpochMillis() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getPausedAtEpochMillis());
        }
        statement.bindLong(13, entity.getTotalPausedMillis());
        statement.bindLong(14, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sleep_sessions";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SleepSessionEntity session,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSleepSessionEntity.insertAndReturnId(session);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final SleepSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSleepSessionEntity.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final SleepSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSleepSessionEntity.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SleepSessionEntity>> observeAll() {
    final String _sql = "SELECT * FROM sleep_sessions ORDER BY startEpochMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sleep_sessions"}, new Callable<List<SleepSessionEntity>>() {
      @Override
      @NonNull
      public List<SleepSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpochMillis");
          final int _cursorIndexOfEndEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpochMillis");
          final int _cursorIndexOfTimeZoneId = CursorUtil.getColumnIndexOrThrow(_cursor, "timeZoneId");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfAwakeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "awakeMinutes");
          final int _cursorIndexOfQualityRating = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityRating");
          final int _cursorIndexOfStartDelayMinutesUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "startDelayMinutesUsed");
          final int _cursorIndexOfPausedAtEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedAtEpochMillis");
          final int _cursorIndexOfTotalPausedMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPausedMillis");
          final List<SleepSessionEntity> _result = new ArrayList<SleepSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SleepSessionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartEpochMillis;
            _tmpStartEpochMillis = _cursor.getLong(_cursorIndexOfStartEpochMillis);
            final Long _tmpEndEpochMillis;
            if (_cursor.isNull(_cursorIndexOfEndEpochMillis)) {
              _tmpEndEpochMillis = null;
            } else {
              _tmpEndEpochMillis = _cursor.getLong(_cursorIndexOfEndEpochMillis);
            }
            final String _tmpTimeZoneId;
            _tmpTimeZoneId = _cursor.getString(_cursorIndexOfTimeZoneId);
            final String _tmpMood;
            if (_cursor.isNull(_cursorIndexOfMood)) {
              _tmpMood = null;
            } else {
              _tmpMood = _cursor.getString(_cursorIndexOfMood);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final List<String> _tmpTags;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTags);
            _tmpTags = __converters.toTagList(_tmp);
            final boolean _tmpIsManualEntry;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp_1 != 0;
            final int _tmpAwakeMinutes;
            _tmpAwakeMinutes = _cursor.getInt(_cursorIndexOfAwakeMinutes);
            final Integer _tmpQualityRating;
            if (_cursor.isNull(_cursorIndexOfQualityRating)) {
              _tmpQualityRating = null;
            } else {
              _tmpQualityRating = _cursor.getInt(_cursorIndexOfQualityRating);
            }
            final int _tmpStartDelayMinutesUsed;
            _tmpStartDelayMinutesUsed = _cursor.getInt(_cursorIndexOfStartDelayMinutesUsed);
            final Long _tmpPausedAtEpochMillis;
            if (_cursor.isNull(_cursorIndexOfPausedAtEpochMillis)) {
              _tmpPausedAtEpochMillis = null;
            } else {
              _tmpPausedAtEpochMillis = _cursor.getLong(_cursorIndexOfPausedAtEpochMillis);
            }
            final long _tmpTotalPausedMillis;
            _tmpTotalPausedMillis = _cursor.getLong(_cursorIndexOfTotalPausedMillis);
            _item = new SleepSessionEntity(_tmpId,_tmpStartEpochMillis,_tmpEndEpochMillis,_tmpTimeZoneId,_tmpMood,_tmpNotes,_tmpTags,_tmpIsManualEntry,_tmpAwakeMinutes,_tmpQualityRating,_tmpStartDelayMinutesUsed,_tmpPausedAtEpochMillis,_tmpTotalPausedMillis);
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
  public Flow<SleepSessionEntity> observeActiveSession() {
    final String _sql = "SELECT * FROM sleep_sessions WHERE endEpochMillis IS NULL LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sleep_sessions"}, new Callable<SleepSessionEntity>() {
      @Override
      @Nullable
      public SleepSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpochMillis");
          final int _cursorIndexOfEndEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpochMillis");
          final int _cursorIndexOfTimeZoneId = CursorUtil.getColumnIndexOrThrow(_cursor, "timeZoneId");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfAwakeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "awakeMinutes");
          final int _cursorIndexOfQualityRating = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityRating");
          final int _cursorIndexOfStartDelayMinutesUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "startDelayMinutesUsed");
          final int _cursorIndexOfPausedAtEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedAtEpochMillis");
          final int _cursorIndexOfTotalPausedMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPausedMillis");
          final SleepSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartEpochMillis;
            _tmpStartEpochMillis = _cursor.getLong(_cursorIndexOfStartEpochMillis);
            final Long _tmpEndEpochMillis;
            if (_cursor.isNull(_cursorIndexOfEndEpochMillis)) {
              _tmpEndEpochMillis = null;
            } else {
              _tmpEndEpochMillis = _cursor.getLong(_cursorIndexOfEndEpochMillis);
            }
            final String _tmpTimeZoneId;
            _tmpTimeZoneId = _cursor.getString(_cursorIndexOfTimeZoneId);
            final String _tmpMood;
            if (_cursor.isNull(_cursorIndexOfMood)) {
              _tmpMood = null;
            } else {
              _tmpMood = _cursor.getString(_cursorIndexOfMood);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final List<String> _tmpTags;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTags);
            _tmpTags = __converters.toTagList(_tmp);
            final boolean _tmpIsManualEntry;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp_1 != 0;
            final int _tmpAwakeMinutes;
            _tmpAwakeMinutes = _cursor.getInt(_cursorIndexOfAwakeMinutes);
            final Integer _tmpQualityRating;
            if (_cursor.isNull(_cursorIndexOfQualityRating)) {
              _tmpQualityRating = null;
            } else {
              _tmpQualityRating = _cursor.getInt(_cursorIndexOfQualityRating);
            }
            final int _tmpStartDelayMinutesUsed;
            _tmpStartDelayMinutesUsed = _cursor.getInt(_cursorIndexOfStartDelayMinutesUsed);
            final Long _tmpPausedAtEpochMillis;
            if (_cursor.isNull(_cursorIndexOfPausedAtEpochMillis)) {
              _tmpPausedAtEpochMillis = null;
            } else {
              _tmpPausedAtEpochMillis = _cursor.getLong(_cursorIndexOfPausedAtEpochMillis);
            }
            final long _tmpTotalPausedMillis;
            _tmpTotalPausedMillis = _cursor.getLong(_cursorIndexOfTotalPausedMillis);
            _result = new SleepSessionEntity(_tmpId,_tmpStartEpochMillis,_tmpEndEpochMillis,_tmpTimeZoneId,_tmpMood,_tmpNotes,_tmpTags,_tmpIsManualEntry,_tmpAwakeMinutes,_tmpQualityRating,_tmpStartDelayMinutesUsed,_tmpPausedAtEpochMillis,_tmpTotalPausedMillis);
          } else {
            _result = null;
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
  public Object getActiveSessionOnce(final Continuation<? super SleepSessionEntity> $completion) {
    final String _sql = "SELECT * FROM sleep_sessions WHERE endEpochMillis IS NULL LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SleepSessionEntity>() {
      @Override
      @Nullable
      public SleepSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpochMillis");
          final int _cursorIndexOfEndEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpochMillis");
          final int _cursorIndexOfTimeZoneId = CursorUtil.getColumnIndexOrThrow(_cursor, "timeZoneId");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfAwakeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "awakeMinutes");
          final int _cursorIndexOfQualityRating = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityRating");
          final int _cursorIndexOfStartDelayMinutesUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "startDelayMinutesUsed");
          final int _cursorIndexOfPausedAtEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedAtEpochMillis");
          final int _cursorIndexOfTotalPausedMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPausedMillis");
          final SleepSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartEpochMillis;
            _tmpStartEpochMillis = _cursor.getLong(_cursorIndexOfStartEpochMillis);
            final Long _tmpEndEpochMillis;
            if (_cursor.isNull(_cursorIndexOfEndEpochMillis)) {
              _tmpEndEpochMillis = null;
            } else {
              _tmpEndEpochMillis = _cursor.getLong(_cursorIndexOfEndEpochMillis);
            }
            final String _tmpTimeZoneId;
            _tmpTimeZoneId = _cursor.getString(_cursorIndexOfTimeZoneId);
            final String _tmpMood;
            if (_cursor.isNull(_cursorIndexOfMood)) {
              _tmpMood = null;
            } else {
              _tmpMood = _cursor.getString(_cursorIndexOfMood);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final List<String> _tmpTags;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTags);
            _tmpTags = __converters.toTagList(_tmp);
            final boolean _tmpIsManualEntry;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp_1 != 0;
            final int _tmpAwakeMinutes;
            _tmpAwakeMinutes = _cursor.getInt(_cursorIndexOfAwakeMinutes);
            final Integer _tmpQualityRating;
            if (_cursor.isNull(_cursorIndexOfQualityRating)) {
              _tmpQualityRating = null;
            } else {
              _tmpQualityRating = _cursor.getInt(_cursorIndexOfQualityRating);
            }
            final int _tmpStartDelayMinutesUsed;
            _tmpStartDelayMinutesUsed = _cursor.getInt(_cursorIndexOfStartDelayMinutesUsed);
            final Long _tmpPausedAtEpochMillis;
            if (_cursor.isNull(_cursorIndexOfPausedAtEpochMillis)) {
              _tmpPausedAtEpochMillis = null;
            } else {
              _tmpPausedAtEpochMillis = _cursor.getLong(_cursorIndexOfPausedAtEpochMillis);
            }
            final long _tmpTotalPausedMillis;
            _tmpTotalPausedMillis = _cursor.getLong(_cursorIndexOfTotalPausedMillis);
            _result = new SleepSessionEntity(_tmpId,_tmpStartEpochMillis,_tmpEndEpochMillis,_tmpTimeZoneId,_tmpMood,_tmpNotes,_tmpTags,_tmpIsManualEntry,_tmpAwakeMinutes,_tmpQualityRating,_tmpStartDelayMinutesUsed,_tmpPausedAtEpochMillis,_tmpTotalPausedMillis);
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
  public Object getById(final long id, final Continuation<? super SleepSessionEntity> $completion) {
    final String _sql = "SELECT * FROM sleep_sessions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SleepSessionEntity>() {
      @Override
      @Nullable
      public SleepSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpochMillis");
          final int _cursorIndexOfEndEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpochMillis");
          final int _cursorIndexOfTimeZoneId = CursorUtil.getColumnIndexOrThrow(_cursor, "timeZoneId");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfAwakeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "awakeMinutes");
          final int _cursorIndexOfQualityRating = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityRating");
          final int _cursorIndexOfStartDelayMinutesUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "startDelayMinutesUsed");
          final int _cursorIndexOfPausedAtEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedAtEpochMillis");
          final int _cursorIndexOfTotalPausedMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPausedMillis");
          final SleepSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartEpochMillis;
            _tmpStartEpochMillis = _cursor.getLong(_cursorIndexOfStartEpochMillis);
            final Long _tmpEndEpochMillis;
            if (_cursor.isNull(_cursorIndexOfEndEpochMillis)) {
              _tmpEndEpochMillis = null;
            } else {
              _tmpEndEpochMillis = _cursor.getLong(_cursorIndexOfEndEpochMillis);
            }
            final String _tmpTimeZoneId;
            _tmpTimeZoneId = _cursor.getString(_cursorIndexOfTimeZoneId);
            final String _tmpMood;
            if (_cursor.isNull(_cursorIndexOfMood)) {
              _tmpMood = null;
            } else {
              _tmpMood = _cursor.getString(_cursorIndexOfMood);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final List<String> _tmpTags;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTags);
            _tmpTags = __converters.toTagList(_tmp);
            final boolean _tmpIsManualEntry;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp_1 != 0;
            final int _tmpAwakeMinutes;
            _tmpAwakeMinutes = _cursor.getInt(_cursorIndexOfAwakeMinutes);
            final Integer _tmpQualityRating;
            if (_cursor.isNull(_cursorIndexOfQualityRating)) {
              _tmpQualityRating = null;
            } else {
              _tmpQualityRating = _cursor.getInt(_cursorIndexOfQualityRating);
            }
            final int _tmpStartDelayMinutesUsed;
            _tmpStartDelayMinutesUsed = _cursor.getInt(_cursorIndexOfStartDelayMinutesUsed);
            final Long _tmpPausedAtEpochMillis;
            if (_cursor.isNull(_cursorIndexOfPausedAtEpochMillis)) {
              _tmpPausedAtEpochMillis = null;
            } else {
              _tmpPausedAtEpochMillis = _cursor.getLong(_cursorIndexOfPausedAtEpochMillis);
            }
            final long _tmpTotalPausedMillis;
            _tmpTotalPausedMillis = _cursor.getLong(_cursorIndexOfTotalPausedMillis);
            _result = new SleepSessionEntity(_tmpId,_tmpStartEpochMillis,_tmpEndEpochMillis,_tmpTimeZoneId,_tmpMood,_tmpNotes,_tmpTags,_tmpIsManualEntry,_tmpAwakeMinutes,_tmpQualityRating,_tmpStartDelayMinutesUsed,_tmpPausedAtEpochMillis,_tmpTotalPausedMillis);
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
  public Flow<SleepSessionEntity> observeLastCompletedSession() {
    final String _sql = "SELECT * FROM sleep_sessions WHERE endEpochMillis IS NOT NULL ORDER BY startEpochMillis DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sleep_sessions"}, new Callable<SleepSessionEntity>() {
      @Override
      @Nullable
      public SleepSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpochMillis");
          final int _cursorIndexOfEndEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpochMillis");
          final int _cursorIndexOfTimeZoneId = CursorUtil.getColumnIndexOrThrow(_cursor, "timeZoneId");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfAwakeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "awakeMinutes");
          final int _cursorIndexOfQualityRating = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityRating");
          final int _cursorIndexOfStartDelayMinutesUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "startDelayMinutesUsed");
          final int _cursorIndexOfPausedAtEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedAtEpochMillis");
          final int _cursorIndexOfTotalPausedMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPausedMillis");
          final SleepSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartEpochMillis;
            _tmpStartEpochMillis = _cursor.getLong(_cursorIndexOfStartEpochMillis);
            final Long _tmpEndEpochMillis;
            if (_cursor.isNull(_cursorIndexOfEndEpochMillis)) {
              _tmpEndEpochMillis = null;
            } else {
              _tmpEndEpochMillis = _cursor.getLong(_cursorIndexOfEndEpochMillis);
            }
            final String _tmpTimeZoneId;
            _tmpTimeZoneId = _cursor.getString(_cursorIndexOfTimeZoneId);
            final String _tmpMood;
            if (_cursor.isNull(_cursorIndexOfMood)) {
              _tmpMood = null;
            } else {
              _tmpMood = _cursor.getString(_cursorIndexOfMood);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final List<String> _tmpTags;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTags);
            _tmpTags = __converters.toTagList(_tmp);
            final boolean _tmpIsManualEntry;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp_1 != 0;
            final int _tmpAwakeMinutes;
            _tmpAwakeMinutes = _cursor.getInt(_cursorIndexOfAwakeMinutes);
            final Integer _tmpQualityRating;
            if (_cursor.isNull(_cursorIndexOfQualityRating)) {
              _tmpQualityRating = null;
            } else {
              _tmpQualityRating = _cursor.getInt(_cursorIndexOfQualityRating);
            }
            final int _tmpStartDelayMinutesUsed;
            _tmpStartDelayMinutesUsed = _cursor.getInt(_cursorIndexOfStartDelayMinutesUsed);
            final Long _tmpPausedAtEpochMillis;
            if (_cursor.isNull(_cursorIndexOfPausedAtEpochMillis)) {
              _tmpPausedAtEpochMillis = null;
            } else {
              _tmpPausedAtEpochMillis = _cursor.getLong(_cursorIndexOfPausedAtEpochMillis);
            }
            final long _tmpTotalPausedMillis;
            _tmpTotalPausedMillis = _cursor.getLong(_cursorIndexOfTotalPausedMillis);
            _result = new SleepSessionEntity(_tmpId,_tmpStartEpochMillis,_tmpEndEpochMillis,_tmpTimeZoneId,_tmpMood,_tmpNotes,_tmpTags,_tmpIsManualEntry,_tmpAwakeMinutes,_tmpQualityRating,_tmpStartDelayMinutesUsed,_tmpPausedAtEpochMillis,_tmpTotalPausedMillis);
          } else {
            _result = null;
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
  public Flow<List<SleepSessionEntity>> observeSince(final long fromEpochMillis) {
    final String _sql = "SELECT * FROM sleep_sessions WHERE endEpochMillis IS NOT NULL AND startEpochMillis >= ? ORDER BY startEpochMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, fromEpochMillis);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sleep_sessions"}, new Callable<List<SleepSessionEntity>>() {
      @Override
      @NonNull
      public List<SleepSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpochMillis");
          final int _cursorIndexOfEndEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpochMillis");
          final int _cursorIndexOfTimeZoneId = CursorUtil.getColumnIndexOrThrow(_cursor, "timeZoneId");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfIsManualEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isManualEntry");
          final int _cursorIndexOfAwakeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "awakeMinutes");
          final int _cursorIndexOfQualityRating = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityRating");
          final int _cursorIndexOfStartDelayMinutesUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "startDelayMinutesUsed");
          final int _cursorIndexOfPausedAtEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedAtEpochMillis");
          final int _cursorIndexOfTotalPausedMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPausedMillis");
          final List<SleepSessionEntity> _result = new ArrayList<SleepSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SleepSessionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartEpochMillis;
            _tmpStartEpochMillis = _cursor.getLong(_cursorIndexOfStartEpochMillis);
            final Long _tmpEndEpochMillis;
            if (_cursor.isNull(_cursorIndexOfEndEpochMillis)) {
              _tmpEndEpochMillis = null;
            } else {
              _tmpEndEpochMillis = _cursor.getLong(_cursorIndexOfEndEpochMillis);
            }
            final String _tmpTimeZoneId;
            _tmpTimeZoneId = _cursor.getString(_cursorIndexOfTimeZoneId);
            final String _tmpMood;
            if (_cursor.isNull(_cursorIndexOfMood)) {
              _tmpMood = null;
            } else {
              _tmpMood = _cursor.getString(_cursorIndexOfMood);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final List<String> _tmpTags;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTags);
            _tmpTags = __converters.toTagList(_tmp);
            final boolean _tmpIsManualEntry;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsManualEntry);
            _tmpIsManualEntry = _tmp_1 != 0;
            final int _tmpAwakeMinutes;
            _tmpAwakeMinutes = _cursor.getInt(_cursorIndexOfAwakeMinutes);
            final Integer _tmpQualityRating;
            if (_cursor.isNull(_cursorIndexOfQualityRating)) {
              _tmpQualityRating = null;
            } else {
              _tmpQualityRating = _cursor.getInt(_cursorIndexOfQualityRating);
            }
            final int _tmpStartDelayMinutesUsed;
            _tmpStartDelayMinutesUsed = _cursor.getInt(_cursorIndexOfStartDelayMinutesUsed);
            final Long _tmpPausedAtEpochMillis;
            if (_cursor.isNull(_cursorIndexOfPausedAtEpochMillis)) {
              _tmpPausedAtEpochMillis = null;
            } else {
              _tmpPausedAtEpochMillis = _cursor.getLong(_cursorIndexOfPausedAtEpochMillis);
            }
            final long _tmpTotalPausedMillis;
            _tmpTotalPausedMillis = _cursor.getLong(_cursorIndexOfTotalPausedMillis);
            _item = new SleepSessionEntity(_tmpId,_tmpStartEpochMillis,_tmpEndEpochMillis,_tmpTimeZoneId,_tmpMood,_tmpNotes,_tmpTags,_tmpIsManualEntry,_tmpAwakeMinutes,_tmpQualityRating,_tmpStartDelayMinutesUsed,_tmpPausedAtEpochMillis,_tmpTotalPausedMillis);
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
  public Object activeSessionCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM sleep_sessions WHERE endEpochMillis IS NULL";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
