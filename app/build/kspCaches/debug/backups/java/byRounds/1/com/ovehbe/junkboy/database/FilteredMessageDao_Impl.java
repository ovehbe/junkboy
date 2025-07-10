package com.ovehbe.junkboy.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FilteredMessageDao_Impl implements FilteredMessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FilteredMessage> __insertionAdapterOfFilteredMessage;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<FilteredMessage> __deletionAdapterOfFilteredMessage;

  private final EntityDeletionOrUpdateAdapter<FilteredMessage> __updateAdapterOfFilteredMessage;

  private final SharedSQLiteStatement __preparedStmtOfDeleteMessagesOlderThan;

  private final SharedSQLiteStatement __preparedStmtOfDeleteMessagesByCategory;

  private final SharedSQLiteStatement __preparedStmtOfUpdateBlockStatus;

  private final SharedSQLiteStatement __preparedStmtOfApplyUserOverride;

  private final SharedSQLiteStatement __preparedStmtOfMarkAsRead;

  private final SharedSQLiteStatement __preparedStmtOfMarkCategoryAsRead;

  public FilteredMessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFilteredMessage = new EntityInsertionAdapter<FilteredMessage>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `filtered_messages` (`id`,`sender`,`messageBody`,`receivedAt`,`category`,`confidence`,`filterType`,`isBlocked`,`isUserOverride`,`isRead`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FilteredMessage entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSender());
        statement.bindString(3, entity.getMessageBody());
        final Long _tmp = __converters.dateToTimestamp(entity.getReceivedAt());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp);
        }
        final String _tmp_1 = __converters.fromMessageCategory(entity.getCategory());
        statement.bindString(5, _tmp_1);
        statement.bindDouble(6, entity.getConfidence());
        final String _tmp_2 = __converters.fromFilterType(entity.getFilterType());
        statement.bindString(7, _tmp_2);
        final int _tmp_3 = entity.isBlocked() ? 1 : 0;
        statement.bindLong(8, _tmp_3);
        final int _tmp_4 = entity.isUserOverride() ? 1 : 0;
        statement.bindLong(9, _tmp_4);
        final int _tmp_5 = entity.isRead() ? 1 : 0;
        statement.bindLong(10, _tmp_5);
      }
    };
    this.__deletionAdapterOfFilteredMessage = new EntityDeletionOrUpdateAdapter<FilteredMessage>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `filtered_messages` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FilteredMessage entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfFilteredMessage = new EntityDeletionOrUpdateAdapter<FilteredMessage>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `filtered_messages` SET `id` = ?,`sender` = ?,`messageBody` = ?,`receivedAt` = ?,`category` = ?,`confidence` = ?,`filterType` = ?,`isBlocked` = ?,`isUserOverride` = ?,`isRead` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FilteredMessage entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSender());
        statement.bindString(3, entity.getMessageBody());
        final Long _tmp = __converters.dateToTimestamp(entity.getReceivedAt());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp);
        }
        final String _tmp_1 = __converters.fromMessageCategory(entity.getCategory());
        statement.bindString(5, _tmp_1);
        statement.bindDouble(6, entity.getConfidence());
        final String _tmp_2 = __converters.fromFilterType(entity.getFilterType());
        statement.bindString(7, _tmp_2);
        final int _tmp_3 = entity.isBlocked() ? 1 : 0;
        statement.bindLong(8, _tmp_3);
        final int _tmp_4 = entity.isUserOverride() ? 1 : 0;
        statement.bindLong(9, _tmp_4);
        final int _tmp_5 = entity.isRead() ? 1 : 0;
        statement.bindLong(10, _tmp_5);
        statement.bindLong(11, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteMessagesOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM filtered_messages WHERE receivedAt < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteMessagesByCategory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM filtered_messages WHERE category = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateBlockStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE filtered_messages SET isBlocked = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfApplyUserOverride = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE filtered_messages SET isUserOverride = 1, isBlocked = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkAsRead = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE filtered_messages SET isRead = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkCategoryAsRead = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE filtered_messages SET isRead = 1 WHERE category = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertMessage(final FilteredMessage message,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfFilteredMessage.insertAndReturnId(message);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMessages(final List<FilteredMessage> messages,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFilteredMessage.insert(messages);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMessage(final FilteredMessage message,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfFilteredMessage.handle(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMessage(final FilteredMessage message,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfFilteredMessage.handle(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMessagesOlderThan(final Date before,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteMessagesOlderThan.acquire();
        int _argIndex = 1;
        final Long _tmp = __converters.dateToTimestamp(before);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, _tmp);
        }
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteMessagesOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMessagesByCategory(final MessageCategory category,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteMessagesByCategory.acquire();
        int _argIndex = 1;
        final String _tmp = __converters.fromMessageCategory(category);
        _stmt.bindString(_argIndex, _tmp);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteMessagesByCategory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBlockStatus(final long id, final boolean isBlocked,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateBlockStatus.acquire();
        int _argIndex = 1;
        final int _tmp = isBlocked ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfUpdateBlockStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object applyUserOverride(final long id, final boolean isBlocked,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfApplyUserOverride.acquire();
        int _argIndex = 1;
        final int _tmp = isBlocked ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfApplyUserOverride.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markAsRead(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAsRead.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfMarkAsRead.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markCategoryAsRead(final MessageCategory category,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkCategoryAsRead.acquire();
        int _argIndex = 1;
        final String _tmp = __converters.fromMessageCategory(category);
        _stmt.bindString(_argIndex, _tmp);
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
          __preparedStmtOfMarkCategoryAsRead.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<FilteredMessage>> getAllMessages() {
    final String _sql = "SELECT * FROM filtered_messages ORDER BY receivedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"filtered_messages"}, new Callable<List<FilteredMessage>>() {
      @Override
      @NonNull
      public List<FilteredMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfMessageBody = CursorUtil.getColumnIndexOrThrow(_cursor, "messageBody");
          final int _cursorIndexOfReceivedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedAt");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfFilterType = CursorUtil.getColumnIndexOrThrow(_cursor, "filterType");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsUserOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "isUserOverride");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final List<FilteredMessage> _result = new ArrayList<FilteredMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FilteredMessage _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSender;
            _tmpSender = _cursor.getString(_cursorIndexOfSender);
            final String _tmpMessageBody;
            _tmpMessageBody = _cursor.getString(_cursorIndexOfMessageBody);
            final Date _tmpReceivedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfReceivedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfReceivedAt);
            }
            final Date _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpReceivedAt = _tmp_1;
            }
            final MessageCategory _tmpCategory;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfCategory);
            _tmpCategory = __converters.toMessageCategory(_tmp_2);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final FilterType _tmpFilterType;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfFilterType);
            _tmpFilterType = __converters.toFilterType(_tmp_3);
            final boolean _tmpIsBlocked;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_4 != 0;
            final boolean _tmpIsUserOverride;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsUserOverride);
            _tmpIsUserOverride = _tmp_5 != 0;
            final boolean _tmpIsRead;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp_6 != 0;
            _item = new FilteredMessage(_tmpId,_tmpSender,_tmpMessageBody,_tmpReceivedAt,_tmpCategory,_tmpConfidence,_tmpFilterType,_tmpIsBlocked,_tmpIsUserOverride,_tmpIsRead);
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
  public Flow<List<FilteredMessage>> getAllMessagesLimited(final int limit) {
    final String _sql = "SELECT * FROM filtered_messages ORDER BY receivedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"filtered_messages"}, new Callable<List<FilteredMessage>>() {
      @Override
      @NonNull
      public List<FilteredMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfMessageBody = CursorUtil.getColumnIndexOrThrow(_cursor, "messageBody");
          final int _cursorIndexOfReceivedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedAt");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfFilterType = CursorUtil.getColumnIndexOrThrow(_cursor, "filterType");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsUserOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "isUserOverride");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final List<FilteredMessage> _result = new ArrayList<FilteredMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FilteredMessage _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSender;
            _tmpSender = _cursor.getString(_cursorIndexOfSender);
            final String _tmpMessageBody;
            _tmpMessageBody = _cursor.getString(_cursorIndexOfMessageBody);
            final Date _tmpReceivedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfReceivedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfReceivedAt);
            }
            final Date _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpReceivedAt = _tmp_1;
            }
            final MessageCategory _tmpCategory;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfCategory);
            _tmpCategory = __converters.toMessageCategory(_tmp_2);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final FilterType _tmpFilterType;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfFilterType);
            _tmpFilterType = __converters.toFilterType(_tmp_3);
            final boolean _tmpIsBlocked;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_4 != 0;
            final boolean _tmpIsUserOverride;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsUserOverride);
            _tmpIsUserOverride = _tmp_5 != 0;
            final boolean _tmpIsRead;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp_6 != 0;
            _item = new FilteredMessage(_tmpId,_tmpSender,_tmpMessageBody,_tmpReceivedAt,_tmpCategory,_tmpConfidence,_tmpFilterType,_tmpIsBlocked,_tmpIsUserOverride,_tmpIsRead);
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
  public Flow<List<FilteredMessage>> getMessagesByCategory(final MessageCategory category) {
    final String _sql = "SELECT * FROM filtered_messages WHERE category = ? ORDER BY receivedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __converters.fromMessageCategory(category);
    _statement.bindString(_argIndex, _tmp);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"filtered_messages"}, new Callable<List<FilteredMessage>>() {
      @Override
      @NonNull
      public List<FilteredMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfMessageBody = CursorUtil.getColumnIndexOrThrow(_cursor, "messageBody");
          final int _cursorIndexOfReceivedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedAt");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfFilterType = CursorUtil.getColumnIndexOrThrow(_cursor, "filterType");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsUserOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "isUserOverride");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final List<FilteredMessage> _result = new ArrayList<FilteredMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FilteredMessage _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSender;
            _tmpSender = _cursor.getString(_cursorIndexOfSender);
            final String _tmpMessageBody;
            _tmpMessageBody = _cursor.getString(_cursorIndexOfMessageBody);
            final Date _tmpReceivedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfReceivedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfReceivedAt);
            }
            final Date _tmp_2 = __converters.fromTimestamp(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpReceivedAt = _tmp_2;
            }
            final MessageCategory _tmpCategory;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfCategory);
            _tmpCategory = __converters.toMessageCategory(_tmp_3);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final FilterType _tmpFilterType;
            final String _tmp_4;
            _tmp_4 = _cursor.getString(_cursorIndexOfFilterType);
            _tmpFilterType = __converters.toFilterType(_tmp_4);
            final boolean _tmpIsBlocked;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_5 != 0;
            final boolean _tmpIsUserOverride;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsUserOverride);
            _tmpIsUserOverride = _tmp_6 != 0;
            final boolean _tmpIsRead;
            final int _tmp_7;
            _tmp_7 = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp_7 != 0;
            _item = new FilteredMessage(_tmpId,_tmpSender,_tmpMessageBody,_tmpReceivedAt,_tmpCategory,_tmpConfidence,_tmpFilterType,_tmpIsBlocked,_tmpIsUserOverride,_tmpIsRead);
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
  public Flow<List<FilteredMessage>> getMessagesByCategoryLimited(final MessageCategory category,
      final int limit) {
    final String _sql = "SELECT * FROM filtered_messages WHERE category = ? ORDER BY receivedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final String _tmp = __converters.fromMessageCategory(category);
    _statement.bindString(_argIndex, _tmp);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"filtered_messages"}, new Callable<List<FilteredMessage>>() {
      @Override
      @NonNull
      public List<FilteredMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfMessageBody = CursorUtil.getColumnIndexOrThrow(_cursor, "messageBody");
          final int _cursorIndexOfReceivedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedAt");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfFilterType = CursorUtil.getColumnIndexOrThrow(_cursor, "filterType");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsUserOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "isUserOverride");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final List<FilteredMessage> _result = new ArrayList<FilteredMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FilteredMessage _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSender;
            _tmpSender = _cursor.getString(_cursorIndexOfSender);
            final String _tmpMessageBody;
            _tmpMessageBody = _cursor.getString(_cursorIndexOfMessageBody);
            final Date _tmpReceivedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfReceivedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfReceivedAt);
            }
            final Date _tmp_2 = __converters.fromTimestamp(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpReceivedAt = _tmp_2;
            }
            final MessageCategory _tmpCategory;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfCategory);
            _tmpCategory = __converters.toMessageCategory(_tmp_3);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final FilterType _tmpFilterType;
            final String _tmp_4;
            _tmp_4 = _cursor.getString(_cursorIndexOfFilterType);
            _tmpFilterType = __converters.toFilterType(_tmp_4);
            final boolean _tmpIsBlocked;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_5 != 0;
            final boolean _tmpIsUserOverride;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsUserOverride);
            _tmpIsUserOverride = _tmp_6 != 0;
            final boolean _tmpIsRead;
            final int _tmp_7;
            _tmp_7 = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp_7 != 0;
            _item = new FilteredMessage(_tmpId,_tmpSender,_tmpMessageBody,_tmpReceivedAt,_tmpCategory,_tmpConfidence,_tmpFilterType,_tmpIsBlocked,_tmpIsUserOverride,_tmpIsRead);
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
  public Flow<List<FilteredMessage>> getBlockedMessages() {
    final String _sql = "SELECT * FROM filtered_messages WHERE isBlocked = 1 ORDER BY receivedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"filtered_messages"}, new Callable<List<FilteredMessage>>() {
      @Override
      @NonNull
      public List<FilteredMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfMessageBody = CursorUtil.getColumnIndexOrThrow(_cursor, "messageBody");
          final int _cursorIndexOfReceivedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedAt");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfFilterType = CursorUtil.getColumnIndexOrThrow(_cursor, "filterType");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsUserOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "isUserOverride");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final List<FilteredMessage> _result = new ArrayList<FilteredMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FilteredMessage _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSender;
            _tmpSender = _cursor.getString(_cursorIndexOfSender);
            final String _tmpMessageBody;
            _tmpMessageBody = _cursor.getString(_cursorIndexOfMessageBody);
            final Date _tmpReceivedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfReceivedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfReceivedAt);
            }
            final Date _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpReceivedAt = _tmp_1;
            }
            final MessageCategory _tmpCategory;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfCategory);
            _tmpCategory = __converters.toMessageCategory(_tmp_2);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final FilterType _tmpFilterType;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfFilterType);
            _tmpFilterType = __converters.toFilterType(_tmp_3);
            final boolean _tmpIsBlocked;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_4 != 0;
            final boolean _tmpIsUserOverride;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsUserOverride);
            _tmpIsUserOverride = _tmp_5 != 0;
            final boolean _tmpIsRead;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp_6 != 0;
            _item = new FilteredMessage(_tmpId,_tmpSender,_tmpMessageBody,_tmpReceivedAt,_tmpCategory,_tmpConfidence,_tmpFilterType,_tmpIsBlocked,_tmpIsUserOverride,_tmpIsRead);
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
  public Flow<List<FilteredMessage>> getBlockedMessagesLimited(final int limit) {
    final String _sql = "SELECT * FROM filtered_messages WHERE isBlocked = 1 ORDER BY receivedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"filtered_messages"}, new Callable<List<FilteredMessage>>() {
      @Override
      @NonNull
      public List<FilteredMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfMessageBody = CursorUtil.getColumnIndexOrThrow(_cursor, "messageBody");
          final int _cursorIndexOfReceivedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedAt");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfFilterType = CursorUtil.getColumnIndexOrThrow(_cursor, "filterType");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsUserOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "isUserOverride");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final List<FilteredMessage> _result = new ArrayList<FilteredMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FilteredMessage _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSender;
            _tmpSender = _cursor.getString(_cursorIndexOfSender);
            final String _tmpMessageBody;
            _tmpMessageBody = _cursor.getString(_cursorIndexOfMessageBody);
            final Date _tmpReceivedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfReceivedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfReceivedAt);
            }
            final Date _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpReceivedAt = _tmp_1;
            }
            final MessageCategory _tmpCategory;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfCategory);
            _tmpCategory = __converters.toMessageCategory(_tmp_2);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final FilterType _tmpFilterType;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfFilterType);
            _tmpFilterType = __converters.toFilterType(_tmp_3);
            final boolean _tmpIsBlocked;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_4 != 0;
            final boolean _tmpIsUserOverride;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsUserOverride);
            _tmpIsUserOverride = _tmp_5 != 0;
            final boolean _tmpIsRead;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp_6 != 0;
            _item = new FilteredMessage(_tmpId,_tmpSender,_tmpMessageBody,_tmpReceivedAt,_tmpCategory,_tmpConfidence,_tmpFilterType,_tmpIsBlocked,_tmpIsUserOverride,_tmpIsRead);
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
  public Flow<List<FilteredMessage>> getMessagesAfter(final Date since) {
    final String _sql = "SELECT * FROM filtered_messages WHERE receivedAt >= ? ORDER BY receivedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final Long _tmp = __converters.dateToTimestamp(since);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"filtered_messages"}, new Callable<List<FilteredMessage>>() {
      @Override
      @NonNull
      public List<FilteredMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSender = CursorUtil.getColumnIndexOrThrow(_cursor, "sender");
          final int _cursorIndexOfMessageBody = CursorUtil.getColumnIndexOrThrow(_cursor, "messageBody");
          final int _cursorIndexOfReceivedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "receivedAt");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfFilterType = CursorUtil.getColumnIndexOrThrow(_cursor, "filterType");
          final int _cursorIndexOfIsBlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBlocked");
          final int _cursorIndexOfIsUserOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "isUserOverride");
          final int _cursorIndexOfIsRead = CursorUtil.getColumnIndexOrThrow(_cursor, "isRead");
          final List<FilteredMessage> _result = new ArrayList<FilteredMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FilteredMessage _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSender;
            _tmpSender = _cursor.getString(_cursorIndexOfSender);
            final String _tmpMessageBody;
            _tmpMessageBody = _cursor.getString(_cursorIndexOfMessageBody);
            final Date _tmpReceivedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfReceivedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfReceivedAt);
            }
            final Date _tmp_2 = __converters.fromTimestamp(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpReceivedAt = _tmp_2;
            }
            final MessageCategory _tmpCategory;
            final String _tmp_3;
            _tmp_3 = _cursor.getString(_cursorIndexOfCategory);
            _tmpCategory = __converters.toMessageCategory(_tmp_3);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final FilterType _tmpFilterType;
            final String _tmp_4;
            _tmp_4 = _cursor.getString(_cursorIndexOfFilterType);
            _tmpFilterType = __converters.toFilterType(_tmp_4);
            final boolean _tmpIsBlocked;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfIsBlocked);
            _tmpIsBlocked = _tmp_5 != 0;
            final boolean _tmpIsUserOverride;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfIsUserOverride);
            _tmpIsUserOverride = _tmp_6 != 0;
            final boolean _tmpIsRead;
            final int _tmp_7;
            _tmp_7 = _cursor.getInt(_cursorIndexOfIsRead);
            _tmpIsRead = _tmp_7 != 0;
            _item = new FilteredMessage(_tmpId,_tmpSender,_tmpMessageBody,_tmpReceivedAt,_tmpCategory,_tmpConfidence,_tmpFilterType,_tmpIsBlocked,_tmpIsUserOverride,_tmpIsRead);
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
  public Object getCountByCategory(final MessageCategory category,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM filtered_messages WHERE category = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __converters.fromMessageCategory(category);
    _statement.bindString(_argIndex, _tmp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(0);
            _result = _tmp_1;
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

  @Override
  public Object getCountByCategoryAfter(final MessageCategory category, final Date since,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM filtered_messages WHERE category = ? AND receivedAt >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final String _tmp = __converters.fromMessageCategory(category);
    _statement.bindString(_argIndex, _tmp);
    _argIndex = 2;
    final Long _tmp_1 = __converters.dateToTimestamp(since);
    if (_tmp_1 == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp_1);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(0);
            _result = _tmp_2;
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
