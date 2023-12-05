package com.dynatech2012.kamleonuserapp.di

import android.content.Context
import androidx.room.Room
import com.dynatech2012.kamleonuserapp.camera.QRCodeImageAnalyzerMLKitKotlin
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureDataDao
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureDatabase
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureDataDao
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureDatabase
import com.dynatech2012.kamleonuserapp.database.MeasureDataDao
import com.dynatech2012.kamleonuserapp.database.MeasureDatabase
import com.dynatech2012.kamleonuserapp.models.IntVectorTypeConverter
import com.dynatech2012.kamleonuserapp.repositories.CloudFunctions
import com.dynatech2012.kamleonuserapp.repositories.DatabaseDataSource
import com.dynatech2012.kamleonuserapp.repositories.UserRepository
import com.dynatech2012.kamleonuserapp.repositories.FirestoreDataSource
import com.dynatech2012.kamleonuserapp.repositories.MeasuresRepository
import com.dynatech2012.kamleonuserapp.repositories.RealtimeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.lang.reflect.Constructor
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    //val intVectorTypeConverter = IntVectorTypeConverter()
    @Provides
    @Singleton
    fun userRepositoryProvider(): UserRepository = UserRepository()
    @Provides
    @Singleton
    fun firestoreRepositoryProvider(userRepository: UserRepository): FirestoreDataSource = FirestoreDataSource(userRepository)

    @Provides
    @Singleton
    fun realtimeRepositoryProvider(userRepository: UserRepository): RealtimeRepository = RealtimeRepository(userRepository)

    @Provides
    @Singleton
    fun measuresRepositoryProvider(databaseDataSource: DatabaseDataSource, firestoreDataSource: FirestoreDataSource, realtimeRepository: RealtimeRepository): MeasuresRepository = MeasuresRepository(databaseDataSource, firestoreDataSource, realtimeRepository)
    @Provides
    @Singleton
    fun databaseDataSourceProvider(measureDataDao: MeasureDataDao, dailyMeasureDataDao: AverageDailyMeasureDataDao, monthlyMeasureDataDao: AverageMonthlyMeasureDataDao): DatabaseDataSource = DatabaseDataSource(measureDataDao, dailyMeasureDataDao, monthlyMeasureDataDao)
    //fun databaseDataSourceProvider(/*measureDataDao: MeasureDataDao*/): DatabaseDataSource = DatabaseDataSource(/*measureDataDao*/)
    @Provides
    @Singleton
    fun clodFunctionsProvider(userRepository: UserRepository): CloudFunctions = CloudFunctions(userRepository)


    @Provides
    @Singleton
    fun provideConverter(): IntVectorTypeConverter = IntVectorTypeConverter()
    @Singleton
    @Provides
    fun provideMeasureDataDao(measureDatabase: MeasureDatabase): MeasureDataDao {
        return measureDatabase.measureDao()
    }
    @Provides
    @Singleton
    fun provideMeasureDatabase(@ApplicationContext appContext: Context, intVectorTypeConverter: IntVectorTypeConverter): MeasureDatabase =
        Room.databaseBuilder(
            appContext,
            MeasureDatabase::class.java,
            "MeasuresReader"
        )
            .addTypeConverter(intVectorTypeConverter)
            .build()

    @Singleton
    @Provides
    fun provideAverageDailyDataDao(averageDailyMeasureDatabase: AverageDailyMeasureDatabase): AverageDailyMeasureDataDao {
        return averageDailyMeasureDatabase.averageDailyMeasureDao()
    }
    @Provides
    @Singleton
    fun provideAverageDailyMeasureDatabase(@ApplicationContext appContext: Context): AverageDailyMeasureDatabase =
        Room.databaseBuilder(
            appContext,
            AverageDailyMeasureDatabase::class.java,
            "AverageDailyReader"
        )
            .build()

    @Singleton
    @Provides
    fun provideAverageMonthlyDataDao(averageMonthlyMeasureDatabase: AverageMonthlyMeasureDatabase): AverageMonthlyMeasureDataDao {
        return averageMonthlyMeasureDatabase.averageMonthlyMeasureDao()
    }

    @Provides
    @Singleton
    fun provideAverageMonthlyMeasureDatabase(@ApplicationContext appContext: Context): AverageMonthlyMeasureDatabase =
        Room.databaseBuilder(
            appContext,
            AverageMonthlyMeasureDatabase::class.java,
            "AverageMonthReader"
        )
            .build()

    @Provides
    @Singleton
    fun provideAnalizer(): QRCodeImageAnalyzerMLKitKotlin = QRCodeImageAnalyzerMLKitKotlin()
}