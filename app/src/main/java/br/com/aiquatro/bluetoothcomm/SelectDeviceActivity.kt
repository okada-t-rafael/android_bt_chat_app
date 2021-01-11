package br.com.aiquatro.bluetoothcomm

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import java.io.IOException

class SelectDeviceActivity : AppCompatActivity() {

    private lateinit var mPairedDevicesListView: ListView
    private lateinit var mUpdateDevicesListButton: Button
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private lateinit var mBluetoothConnection: BluetoothConnectionService

    companion object {
        private const val ENABLE_BLUETOOTH_REQUEST = 1
        const val EXTRA_ADDRESS = "Device Address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device)

        mPairedDevicesListView = findViewById(R.id.selectDevice_pairedDevice_listView)
        mUpdateDevicesListButton = findViewById(R.id.selectDevice_updateDeviceList_button)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothConnection = BluetoothConnectionService()

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "This device does not support bluetooth.", Toast.LENGTH_LONG).show()
            return
        } else if (!mBluetoothAdapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, ENABLE_BLUETOOTH_REQUEST)
        }
        mUpdateDevicesListButton.setOnClickListener { fillPairedDevicesList() }
    }

    private fun fillPairedDevicesList() {
        val pairedDevices: Set<BluetoothDevice>
        val tmp: MutableList<BluetoothDevice> = mutableListOf()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tmp)

        try {
            pairedDevices = mBluetoothAdapter!!.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                pairedDevices.forEach { device: BluetoothDevice ->
                    tmp.add(device)
                    Log.i("Paired Device", "${device.address}: ${device.name?: "nameless"}")
                }
            } else {
                Toast.makeText(this, "No paired devices available.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) { e.printStackTrace() }

        mPairedDevicesListView.adapter = adapter
        mPairedDevicesListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, index, _, ->
            val device: BluetoothDevice = tmp[index]
            val address: String = device.address
            val connectIntent = Intent(this, SimpleChatActivity::class.java)
            connectIntent.putExtra(EXTRA_ADDRESS, address)
            startActivity(connectIntent)
        }
    }
}