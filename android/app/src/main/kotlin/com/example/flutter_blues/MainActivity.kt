package com.example.flutter_blues

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

@RequiresApi(VERSION_CODES.M)
class MainActivity: FlutterActivity() {
    private val _tag: String = MainActivity::class.java.simpleName
    private val _channelName = "sweet/bluetooth"
    private val _deviceNames = HashSet<String>()
    private lateinit var _bluetoothAdapter: BluetoothAdapter

    // Flutter entrypoint
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, _channelName).setMethodCallHandler {
                call, result ->
            if (call.method == "allDevices") {
                result.success(getAllBluetoothDevices())
            } else {
                result.notImplemented()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check permissions required to scan for bluetooth devices
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
            Log.w(_tag, "Bluetooth access denied")
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
            Log.w(_tag, "Bluetooth scan access denied")
        }

        @Suppress("DEPRECATION") // required for android API < 31
        _bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!_bluetoothAdapter.isEnabled) {
            Log.w(_tag, "The bluetooth adapter is not enabled")
            return
        }

        // show paired devices that may not be active right now
        val pairedDevices: Set<BluetoothDevice>? = _bluetoothAdapter.bondedDevices
        if (pairedDevices?.isNotEmpty() == true) {
            _deviceNames.addAll(pairedDevices.map{ device -> device.name })
            Log.d(_tag, "paired devices: " + _deviceNames.joinToString(","))
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        _bluetoothAdapter.startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        _deviceNames.add(device.name)
                        Log.d(_tag,"found device: " + device.name)
                    }
                }
            }
        }
    }

    private fun getAllBluetoothDevices(): List<String> {
        return _deviceNames.toList()
    }
}
