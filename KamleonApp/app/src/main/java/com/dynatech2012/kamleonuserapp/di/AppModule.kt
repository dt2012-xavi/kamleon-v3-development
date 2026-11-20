package com.dynatech2012.kamleonuserapp.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
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
import com.dynatech2012.kamleonuserapp.repositories.RealtimeDataSource
import com.dynatech2012.kamleonuserapp.security.HibpPasswordBreachChecker
import com.dynatech2012.kamleonuserapp.security.PasswordPolicy
import com.dynatech2012.kamleonuserapp.security.ZxcvbnPasswordStrengthEstimator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import com.dynatech2012.kamleonuserapp.network.AndroidNetworkMonitor
import com.dynatech2012.kamleonuserapp.network.NetworkGate
import com.dynatech2012.kamleonuserapp.network.SessionWatcher
import com.dynatech2012.kamleonuserapp.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    //val intVectorTypeConverter = IntVectorTypeConverter()
    @Provides
    @Singleton
    fun networkMonitorProvider(@ApplicationContext appContext: Context): NetworkMonitor = AndroidNetworkMonitor(appContext)

    /**
     * One image loader for the whole app, so the avatar shares its caches across screens and refreshes.
     * No crossfade: the avatar views are CircleImageView, which copies the drawable once into its own
     * bitmap, and a fading drawable is still empty at that moment, so the photo would never show.
     */
    @Provides
    @Singleton
    fun imageLoaderProvider(@ApplicationContext appContext: Context): ImageLoader =
        ImageLoader.Builder(appContext).build()

    @Provides
    @Singleton
    fun networkGateProvider(monitor: NetworkMonitor, watcher: SessionWatcher): NetworkGate =
        NetworkGate(monitor, onSessionEnded = watcher::report)

    @Provides
    @Singleton
    fun sessionWatcherProvider(): SessionWatcher = SessionWatcher()

    @Provides
    @Singleton
    fun userRepositoryProvider(gate: NetworkGate): UserRepository = UserRepository(gate)
    @Provides
    @Singleton
    fun firestoreRepositoryProvider(userRepository: UserRepository, gate: NetworkGate): FirestoreDataSource = FirestoreDataSource(userRepository, gate)

    @Provides
    @Singleton
    fun realtimeRepositoryProvider(userRepository: UserRepository, gate: NetworkGate): RealtimeDataSource = RealtimeDataSource(userRepository, gate)

    @Provides
    @Singleton
    fun measuresRepositoryProvider(databaseDataSource: DatabaseDataSource, firestoreDataSource: FirestoreDataSource, realtimeRepository: RealtimeDataSource): MeasuresRepository = MeasuresRepository(databaseDataSource, firestoreDataSource, realtimeRepository)
    @Provides
    @Singleton
    fun databaseDataSourceProvider(measureDataDao: MeasureDataDao, dailyMeasureDataDao: AverageDailyMeasureDataDao, monthlyMeasureDataDao: AverageMonthlyMeasureDataDao): DatabaseDataSource = DatabaseDataSource(measureDataDao, dailyMeasureDataDao, monthlyMeasureDataDao)
    //fun databaseDataSourceProvider(/*measureDataDao: MeasureDataDao*/): DatabaseDataSource = DatabaseDataSource(/*measureDataDao*/)
    @Provides
    @Singleton
    fun clodFunctionsProvider(userRepository: UserRepository, gate: NetworkGate): CloudFunctions = CloudFunctions(userRepository, gate)


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
    fun providePasswordPolicy(): PasswordPolicy =
        PasswordPolicy(ZxcvbnPasswordStrengthEstimator(), HibpPasswordBreachChecker())

    @Provides
    @Singleton
    fun provideAnalizer(): QRCodeImageAnalyzerMLKitKotlin = QRCodeImageAnalyzerMLKitKotlin()
}