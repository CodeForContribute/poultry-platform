package com.poultry.buyer.di

import com.poultry.buyer.BuildConfig
import com.poultry.buyer.core.data.preferences.SecureTokenManager
import com.poultry.buyer.core.network.ApiService
import com.poultry.buyer.core.network.AuthInterceptor
import com.poultry.buyer.core.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Production API hostname
    private const val PRODUCTION_HOST = "api.poultry-platform.com"
    private const val STAGING_HOST = "staging-api.poultry-platform.com"

    /**
     * Certificate pins for SSL pinning.
     *
     * IMPORTANT: Replace these placeholder values with actual SHA-256 hashes of your certificates.
     *
     * To extract the hash from your certificate:
     * openssl s_client -servername api.poultry-platform.com -connect api.poultry-platform.com:443 | \
     * openssl x509 -pubkey -noout | \
     * openssl pkey -pubin -outform der | \
     * openssl dgst -sha256 -binary | \
     * openssl enc -base64
     *
     * Include multiple pins for:
     * 1. Current production certificate
     * 2. Backup/rotation certificate
     * 3. Root CA certificate (for emergency situations)
     */
    private object CertificatePins {
        // Production certificate pins
        const val PRODUCTION_PIN_1 = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        const val PRODUCTION_PIN_2 = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        const val PRODUCTION_PIN_3 = "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="

        // Staging certificate pins
        const val STAGING_PIN_1 = "sha256/DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD="
        const val STAGING_PIN_2 = "sha256/EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE="

        // Let's Encrypt Root CA pins (ISRG Root X1 and X2)
        const val LETS_ENCRYPT_ROOT_X1 = "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M="
        const val LETS_ENCRYPT_ROOT_X2 = "sha256/diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvVFZE8zmgzI="
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: SecureTokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            // Production API pins
            .add(PRODUCTION_HOST, CertificatePins.PRODUCTION_PIN_1)
            .add(PRODUCTION_HOST, CertificatePins.PRODUCTION_PIN_2)
            .add(PRODUCTION_HOST, CertificatePins.PRODUCTION_PIN_3)
            .add(PRODUCTION_HOST, CertificatePins.LETS_ENCRYPT_ROOT_X1)
            .add(PRODUCTION_HOST, CertificatePins.LETS_ENCRYPT_ROOT_X2)
            // Staging API pins
            .add(STAGING_HOST, CertificatePins.STAGING_PIN_1)
            .add(STAGING_HOST, CertificatePins.STAGING_PIN_2)
            .add(STAGING_HOST, CertificatePins.LETS_ENCRYPT_ROOT_X1)
            .add(STAGING_HOST, CertificatePins.LETS_ENCRYPT_ROOT_X2)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        certificatePinner: CertificatePinner,
        securityManager: SecurityManager
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Add security interceptor for tamper detection
        builder.addInterceptor { chain ->
            // Perform security checks before each request
            if (!BuildConfig.DEBUG) {
                val securityStatus = securityManager.performSecurityCheck()
                if (!securityStatus.isSecure) {
                    throw SecurityException("Security check failed: ${securityStatus.failureReason}")
                }
            }
            chain.proceed(chain.request())
        }

        // Only apply certificate pinning in release builds
        // This allows debugging with proxy tools like Charles in debug mode
        if (!BuildConfig.DEBUG) {
            builder.certificatePinner(certificatePinner)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
