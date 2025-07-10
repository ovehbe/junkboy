package com.ovehbe.junkboy.database;

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
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile FilteredMessageDao _filteredMessageDao;

  private volatile AllowedSenderDao _allowedSenderDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `filtered_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sender` TEXT NOT NULL, `messageBody` TEXT NOT NULL, `receivedAt` INTEGER NOT NULL, `category` TEXT NOT NULL, `confidence` REAL NOT NULL, `filterType` TEXT NOT NULL, `isBlocked` INTEGER NOT NULL, `isUserOverride` INTEGER NOT NULL, `isRead` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `allowed_senders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `phoneNumber` TEXT NOT NULL, `displayName` TEXT, `addedAt` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '8ddffca0d2af9c3bd4094d1de78e9c95')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `filtered_messages`");
        db.execSQL("DROP TABLE IF EXISTS `allowed_senders`");
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
        final HashMap<String, TableInfo.Column> _columnsFilteredMessages = new HashMap<String, TableInfo.Column>(10);
        _columnsFilteredMessages.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilteredMessages.put("sender", new TableInfo.Column("sender", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilteredMessages.put("messageBody", new TableInfo.Column("messageBody", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilteredMessages.put("receivedAt", new TableInfo.Column("receivedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilteredMessages.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilteredMessages.put("confidence", new TableInfo.Column("confidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilteredMessages.put("filterType", new TableInfo.Column("filterType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilteredMessages.put("isBlocked", new TableInfo.Column("isBlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilteredMessages.put("isUserOverride", new TableInfo.Column("isUserOverride", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilteredMessages.put("isRead", new TableInfo.Column("isRead", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFilteredMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFilteredMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFilteredMessages = new TableInfo("filtered_messages", _columnsFilteredMessages, _foreignKeysFilteredMessages, _indicesFilteredMessages);
        final TableInfo _existingFilteredMessages = TableInfo.read(db, "filtered_messages");
        if (!_infoFilteredMessages.equals(_existingFilteredMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "filtered_messages(com.ovehbe.junkboy.database.FilteredMessage).\n"
                  + " Expected:\n" + _infoFilteredMessages + "\n"
                  + " Found:\n" + _existingFilteredMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsAllowedSenders = new HashMap<String, TableInfo.Column>(5);
        _columnsAllowedSenders.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAllowedSenders.put("phoneNumber", new TableInfo.Column("phoneNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAllowedSenders.put("displayName", new TableInfo.Column("displayName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAllowedSenders.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAllowedSenders.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAllowedSenders = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAllowedSenders = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAllowedSenders = new TableInfo("allowed_senders", _columnsAllowedSenders, _foreignKeysAllowedSenders, _indicesAllowedSenders);
        final TableInfo _existingAllowedSenders = TableInfo.read(db, "allowed_senders");
        if (!_infoAllowedSenders.equals(_existingAllowedSenders)) {
          return new RoomOpenHelper.ValidationResult(false, "allowed_senders(com.ovehbe.junkboy.database.AllowedSender).\n"
                  + " Expected:\n" + _infoAllowedSenders + "\n"
                  + " Found:\n" + _existingAllowedSenders);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "8ddffca0d2af9c3bd4094d1de78e9c95", "debf4ea7fd2d8155edb94d7cc732513a");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "filtered_messages","allowed_senders");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `filtered_messages`");
      _db.execSQL("DELETE FROM `allowed_senders`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(FilteredMessageDao.class, FilteredMessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AllowedSenderDao.class, AllowedSenderDao_Impl.getRequiredConverters());
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
  public FilteredMessageDao filteredMessageDao() {
    if (_filteredMessageDao != null) {
      return _filteredMessageDao;
    } else {
      synchronized(this) {
        if(_filteredMessageDao == null) {
          _filteredMessageDao = new FilteredMessageDao_Impl(this);
        }
        return _filteredMessageDao;
      }
    }
  }

  @Override
  public AllowedSenderDao allowedSenderDao() {
    if (_allowedSenderDao != null) {
      return _allowedSenderDao;
    } else {
      synchronized(this) {
        if(_allowedSenderDao == null) {
          _allowedSenderDao = new AllowedSenderDao_Impl(this);
        }
        return _allowedSenderDao;
      }
    }
  }
}
