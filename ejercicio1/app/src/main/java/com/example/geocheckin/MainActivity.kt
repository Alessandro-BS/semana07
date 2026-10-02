package com.example.geocheckin

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.UUID
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var secureStorage: SecureStorageManager
    private lateinit var tvStatus: TextView
    private lateinit var btnCheckIn: Button

    // Se guarda como propiedad para poder cancelar la petición en onStop()
    private var cancellationTokenSource: CancellationTokenSource? = null

    // Contrato moderno para solicitar múltiples permisos en runtime
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Permiso de ubicación precisa concedido
                performSecureCheckIn()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Solo ubicación aproximada: no basta para validar la zona
                Toast.makeText(
                    this, R.string.msg_precise_required, Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                // Permisos denegados
                tvStatus.setText(R.string.status_denied)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Desde Android 15 la app se dibuja bajo las barras del sistema (edge-to-edge)
        val root = findViewById<View>(R.id.main)
        val basePadding = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                basePadding + bars.left,
                basePadding + bars.top,
                basePadding + bars.right,
                basePadding + bars.bottom
            )
            insets
        }

        tvStatus = findViewById(R.id.tvStatus)
        btnCheckIn = findViewById(R.id.btnCheckIn)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        secureStorage = SecureStorageManager(applicationContext)

        btnCheckIn.setOnClickListener {
            checkPermissionsAndExecute()
        }
    }

    override fun onStop() {
        super.onStop()
        // Cancelar la petición pendiente para no consumir GPS con la app en segundo plano
        cancellationTokenSource?.let {
            it.cancel()
            cancellationTokenSource = null
            btnCheckIn.isEnabled = true
            tvStatus.setText(R.string.status_initial)
        }
    }

    private fun checkPermissionsAndExecute() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                performSecureCheckIn()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Explicar el motivo antes de volver a pedir el permiso
                AlertDialog.Builder(this)
                    .setTitle(R.string.rationale_title)
                    .setMessage(R.string.rationale_message)
                    .setPositiveButton(R.string.btn_accept) { _, _ -> requestLocationPermissions() }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            }
            else -> {
                requestLocationPermissions()
            }
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission") // Solo se llama tras comprobar ACCESS_FINE_LOCATION
    private fun performSecureCheckIn() {
        tvStatus.setText(R.string.status_locating)
        btnCheckIn.isEnabled = false // Evita fichajes duplicados mientras se ubica

        // El Fused Location Provider combina GPS, Wi-Fi y red móvil;
        // con PRIORITY_HIGH_ACCURACY prioriza el GPS
        val tokenSource = CancellationTokenSource().also { cancellationTokenSource = it }

        // Los listeners ligados a la Activity se eliminan automáticamente en onStop()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            tokenSource.token
        ).addOnSuccessListener(this) { location ->
            if (location != null) {
                validateAndSaveCheckIn(location)
            } else {
                tvStatus.setText(R.string.status_no_location)
            }
        }.addOnFailureListener(this) { exception ->
            tvStatus.text = getString(R.string.status_error, exception.localizedMessage)
        }.addOnCompleteListener(this) {
            btnCheckIn.isEnabled = true
            cancellationTokenSource = null
        }
    }

    private fun validateAndSaveCheckIn(location: Location) {
        // 1. Rechazar ubicaciones simuladas (apps de GPS falso)
        if (isMockLocation(location)) {
            tvStatus.setText(R.string.status_mock)
            return
        }

        // 2. Validar que el usuario se encuentra dentro de la zona de fichaje
        val distance = location.distanceTo(WORK_ZONE).roundToInt()
        if (distance > WORK_ZONE_RADIUS_METERS) {
            tvStatus.text = getString(R.string.status_out_of_zone, distance)
            return
        }

        // 3. Generar el token y firmarlo con HMAC-SHA256 (clave del Android Keystore)
        val payload = "CHECKIN|${System.currentTimeMillis()}|${location.latitude}|" +
            "${location.longitude}|${UUID.randomUUID()}"
        val token = "$payload|${secureStorage.signPayload(payload)}"

        // 4. Guardarlo cifrado; en pantalla solo se muestra un fragmento, nunca el token completo
        secureStorage.saveCheckInToken(token)
        tvStatus.text = getString(R.string.status_success, distance, token.takeLast(6))
    }

    private fun isMockLocation(location: Location): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }

    companion object {
        // Zona de fichaje: coordenadas del centro de trabajo (reemplazar por las reales)
        private val WORK_ZONE = Location("work_zone").apply {
            latitude = -12.046374
            longitude = -77.042793
        }
        private const val WORK_ZONE_RADIUS_METERS = 100
    }
}
