package com.tusiglas.custodiaapp

import android.app.Activity
import android.content.Context
import com.example.custodiaapp.PreferencesManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private val preferencesManager = PreferencesManager(context)

    // ID de prueba de Google (cambiarás esto después por tus IDs reales)
    private val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    fun loadAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            TEST_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showAdIfNotPremium(activity: Activity, onAdClosed: () -> Unit) {
        // Si es Premium, ejecutar la acción directamente sin anuncio
        if (preferencesManager.isPremium()) {
            onAdClosed()
            return
        }

        // Si no hay anuncio cargado, ejecutar la acción directamente
        if (interstitialAd == null) {
            onAdClosed()
            loadAd() // Cargar uno nuevo para la próxima vez
            return
        }

        // Configurar qué pasa cuando se cierra el anuncio
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadAd() // Cargar uno nuevo
                onAdClosed() // Ejecutar la acción
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                onAdClosed() // Ejecutar la acción aunque falle
            }
        }

        // Mostrar el anuncio
        interstitialAd?.show(activity)
    }
}
