package com.ismaelcr.custodiaapp

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private val preferencesManager = PreferencesManager(context)

    // Se selecciona automáticamente según MODO_TESTING
    private val INTERSTITIAL_AD_UNIT_ID = if (Config.MODO_TESTING) {
        "ca-app-pub-3940256099942544/1033173712" // ID de prueba de Google
    } else {
        "ca-app-pub-3925937359059811/5636410337" // Tu ID real de producción
    }

    fun loadAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
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
