package br.com.aiquatro.bluetoothcomm

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.EventLog
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class SimpleChatActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SimpleChatActivity"
        private val MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private lateinit var mEditText: EditText
    private lateinit var mSendTextButton: Button
    private lateinit var mConnectButton: Button
    private lateinit var mDisconnectButton: Button

    private var mAddress: String? = null
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mBluetoothConnection: BluetoothConnectionService
    private lateinit var mDevice: BluetoothDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_chat)

        mEditText = findViewById(R.id.simpleChat_editText)
        mSendTextButton = findViewById(R.id.simpleChat_sendText_button)
        mConnectButton = findViewById(R.id.simpleChat_connect_button)
        mDisconnectButton = findViewById(R.id.simpleChat_disconnect_button)

        mAddress = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBluetoothConnection = BluetoothConnectionService(this)
        mDevice = mBluetoothAdapter.getRemoteDevice(mAddress)

        mSendTextButton.setOnClickListener { sendMessage() }
        mConnectButton.setOnClickListener { connectClient() }
        mDisconnectButton.setOnClickListener { disconnectClient() }

        mBluetoothConnection.startServer()

        mConnectButton.isEnabled = true
        mDisconnectButton.isEnabled = false
        mSendTextButton.isEnabled = false
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun connectClient() {
        mBluetoothConnection.startClient(mDevice, MY_UUID_INSECURE)
    }

    private fun disconnectClient() {
        mBluetoothConnection.stopClient()
    }

    private fun sendMessage() {
        mBluetoothConnection.write(mEditText.text.toString().toByteArray())
        mEditText.text.clear()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBluetoothConnectionEvent(event: BluetoothConnectionEvent) {
        Log.d(TAG, "SingleChatActivity: onBluetoothConnectionEvent: Subscribing.")
        mSendTextButton.isEnabled = event.connected!!
        mConnectButton.isEnabled = !event.connected
        mDisconnectButton.isEnabled = event.connected
    }
}