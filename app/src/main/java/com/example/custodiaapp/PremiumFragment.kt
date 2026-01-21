package com.example.custodiaapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.TextView

class PremiumFragment : Fragment() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_premium, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager(requireContext())

        setupUI(view)
    }

    private fun setupUI(view: View) {
        // Textos informativos
        val tvTrialInfo = view.findViewById<TextView>(R.id.tvTrialInfo)
        val tvFeatures = view.findViewById<TextView>(R.id.tvFeaturesList)

        // Botones
        val btnBuyPremium = view.findViewById<MaterialButton>(R.id.btnBuyPremium)
        val btnRestorePurchase = view.findViewById<MaterialButton>(R.id.btnRestorePurchase)

        // Mostrar días restantes
        updateTrialInfo(tvTrialInfo)

        // Por ahora, botones de prueba
        btnBuyPremium.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Próximamente")
                .setMessage("El sistema de pagos estará disponible próximamente. Por ahora puedes probar todas las funciones Premium de forma gratuita durante 30 días.")
                .setPositiveButton("Entendido", null)
                .show()
        }

        btnRestorePurchase.setOnClickListener {
            // TODO: Restaurar compra real (Fase 5)
            showRestoreDialog()
        }

        // Botón oculto para testing (solo en debug)
        setupTestingButtons(view)
    }

    private fun updateTrialInfo(textView: TextView) {
        val daysRemaining = preferencesManager.getTrialDaysRemaining()

        val message = when {
            preferencesManager.isPremium() -> "✅ Tienes la versión Premium"
            daysRemaining > 0 -> "Periodo de prueba: $daysRemaining días restantes"
            else -> "⚠️ Tu periodo de prueba ha finalizado"
        }

        textView.text = message
    }

    private fun showTestPurchaseDialog() {
        // Obtener billingManager desde MainActivity
        val billingManager = (requireActivity() as MainActivity).getBillingManager()

        // Lanzar flujo de compra real de Google Play
        billingManager.launchPurchaseFlow()
    }


    private fun showRestoreDialog() {
        // Obtener billingManager desde MainActivity
        val billingManager = (requireActivity() as MainActivity).getBillingManager()

        // Verificar compras existentes en Google Play
        billingManager.queryPurchases()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restaurando compra...")
            .setMessage("Verificando tus compras en Google Play")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🎉 ¡Premium activado!")
            .setMessage("Ya tienes acceso a todas las funciones Premium")
            .setPositiveButton("OK") { _, _ ->
                requireActivity().onBackPressed()
            }
            .show()
    }

    private fun setupTestingButtons(view: View) {
        // Solo visible en modo debug para testing
        if (true) {
            android.util.Log.d("DEBUG_PREMIUM", "Los botones deberían aparecer")
            val tvTestingLabel = view.findViewById<TextView>(R.id.tvTestingLabel)
            val btnSimulate30Days = view.findViewById<MaterialButton>(R.id.btnSimulate30Days)
            val btnResetTrial = view.findViewById<MaterialButton>(R.id.btnResetTrial)
            val btnTogglePremium = view.findViewById<MaterialButton>(R.id.btnTogglePremium)

            tvTestingLabel.visibility = View.VISIBLE
            btnSimulate30Days.visibility = View.VISIBLE
            btnResetTrial.visibility = View.VISIBLE
            btnTogglePremium.visibility = View.VISIBLE

            btnSimulate30Days?.setOnClickListener {
                preferencesManager.simulateDaysPassedForTesting(30)
                updateTrialInfo(view.findViewById(R.id.tvTrialInfo))
                showTestMessage("Simulados 30 días. Periodo expirado.")
            }

            btnResetTrial?.setOnClickListener {
                preferencesManager.resetInstallDateForTesting()
                updateTrialInfo(view.findViewById(R.id.tvTrialInfo))
                showTestMessage("Periodo de prueba reseteado a 30 días")
            }

            btnTogglePremium?.setOnClickListener {
                val newStatus = !preferencesManager.isPremium()
                preferencesManager.setPremium(newStatus)
                updateTrialInfo(view.findViewById(R.id.tvTrialInfo))
                showTestMessage("Premium: ${if (newStatus) "ON" else "OFF"}")
            }
        }
    }

    private fun showTestMessage(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Testing")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar info cuando vuelve a esta pantalla
        view?.let { v ->
            updateTrialInfo(v.findViewById(R.id.tvTrialInfo))
        }
    }

}
