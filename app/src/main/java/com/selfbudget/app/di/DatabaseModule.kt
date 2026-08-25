package com.selfbudget.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.selfbudget.app.data.local.AccountDao
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.local.BudgetDao
import com.selfbudget.app.data.local.CategoryDao
import com.selfbudget.app.data.local.ExchangeRateDao
import com.selfbudget.app.data.local.GoalDao
import com.selfbudget.app.data.local.NetWorthDao
import com.selfbudget.app.data.local.RecurringDao
import com.selfbudget.app.data.local.TransactionDao
import com.selfbudget.app.data.local.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>,
        accountDaoProvider: Provider<AccountDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "self_budget.db"
        )
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    categoryDaoProvider.get().insertCategories(AppDatabase.DEFAULT_CATEGORIES)
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val catDao = categoryDaoProvider.get()
                    catDao.insertCategories(AppDatabase.DEFAULT_CATEGORIES)
                }
            }
        }).build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideRecurringDao(database: AppDatabase): RecurringDao = database.recurringDao()

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideNetWorthDao(database: AppDatabase): NetWorthDao = database.netWorthDao()

    @Provides
    fun provideExchangeRateDao(database: AppDatabase): ExchangeRateDao = database.exchangeRateDao()
}
