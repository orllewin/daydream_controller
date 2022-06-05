package orllewin.daydreamcontroller

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import orllewin.coraclelib.AndroidRenderer
import orllewin.daydreamcontroller.bluetooth.Bluetooth
import orllewin.daydreamcontroller.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetooth: Bluetooth
    private lateinit var drawing: DaydreamTestDrawing

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        drawing = DaydreamTestDrawing()
        drawing.renderer(AndroidRenderer(binding.coracleView)).start()

        doPermissions()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun doPermissions(){
        val requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                //granted
                startBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth permissions required for this app", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("Bluetooth", "${it.key} = ${it.value}")

                //assume granted for now
                startBluetooth()
            }
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                requestMultiplePermissions.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
            }
            else -> {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(enableBtIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        bluetooth.clear()
    }

    var alreadyStarted = false

    private fun startBluetooth(){
        if(alreadyStarted) return
        alreadyStarted = true
        bluetooth = Bluetooth(this, { message ->
            runOnUiThread {
                binding.debugLog.append("$message\n")
            }

        }, { controllerEvent ->
            runOnUiThread {
                binding.changeLog.text = controllerEvent.toString()
                drawing.draw(controllerEvent)
            }
        })
        bluetooth.logDevices()
        bluetooth.findController { success, message ->
            when {
                success -> {
                    binding.debugLog.append("\nFound Daydream Controller\n")
                    bluetooth.connect()
                }
                else -> binding.debugLog.append("$message\n")
            }
        }
    }
}