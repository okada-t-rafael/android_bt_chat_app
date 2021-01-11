package br.com.aiquatro.bluetoothcomm

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import org.greenrobot.eventbus.EventBus

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.UUID

class BluetoothConnectionService(private val context: Context? = null) {

    companion object {
        private const val TAG = "BTConnServ"
        private const val appName = "MYAPP"
        private val MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    init {
        Log.d(TAG, "BluetoothConnectionService: $context")
    }

    private var mBluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    @Synchronized fun startServer() {
        Log.d(TAG, "BluetoothConnectionServer: start: Starting.")

        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }
        mInsecureAcceptThread = AcceptThread()
        mInsecureAcceptThread?.start()
    }

    fun stopServer() {
        Log.d(TAG, "BluetoothConnectionServer: stop: Stopping.")
        mInsecureAcceptThread?.cancel()
    }

    fun startClient(device: BluetoothDevice, uuid: UUID) {
        Log.d(TAG, "BluetoothConnectionServer: startClient: Starting client.")
        mConnectThread = ConnectThread(device, uuid)
        mConnectThread?.start()
    }

    fun stopClient() {
        Log.d(TAG, "BluetoothConnectionServer: stopClient: Stopping client.")
        mConnectThread?.cancel()
    }

    private fun connectSocket(socket: BluetoothSocket) {
        Log.d(TAG, "BluetoothConnectionServer: connected: Starting.")
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread?.start()
    }

    fun write(bytes: ByteArray) {
        mConnectedThread?.write(bytes)
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted (or
     * until cancelled).
     */
    private inner class AcceptThread: Thread() {
        private var mmServerSocket: BluetoothServerSocket

        init {
            lateinit var tmp: BluetoothServerSocket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE)
                Log.d(TAG, "AcceptThread: constructor: Setting up server using: $MY_UUID_INSECURE")
            } catch (e: IOException) {
                Log.e(TAG, "AcceptThread: constructor: IOException ${e.message}")
            }
            mmServerSocket = tmp
        }

        override fun run() {
            super.run()
            Log.d(TAG, "AcceptThread: run: AcceptThread running.")

            lateinit var socket : BluetoothSocket
            try {
                Log.d(TAG, "AcceptThread: run: RFCOM server socket start.")
                socket = mmServerSocket.accept()
                Log.d(TAG, "AcceptThread: run: RFCOM server socket accepted connection.")
            } catch (e: IOException) {
                Log.e(TAG, "AcceptThread: run: IOException ${e.message}")
            }
            connectSocket(socket)
        }

        fun cancel() {
            try {
                Log.d(TAG, "AcceptThread: cancel: Canceling AcceptThread.")
                mmServerSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "AcceptThread: cancel: IOException ${e.message}")
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection with a device.
     * It runs straight through; the connection either succeeds or fails.
     */
    private inner class ConnectThread(device: BluetoothDevice, uuid: UUID): Thread() {
        private lateinit var mmSocket: BluetoothSocket
        private var mmDevice: BluetoothDevice = device
        private var mmDeviceUUID: UUID = uuid

        override fun run() {
            super.run()
            Log.d(TAG, "ConnectThread: run: ConnectThread running.")
            lateinit var tmp: BluetoothSocket
            try {
                Log.d(TAG, "ConnectThread: run: Trying to create InsecureRfcommSocket using UUID: $MY_UUID_INSECURE")
                tmp = mmDevice.createRfcommSocketToServiceRecord(mmDeviceUUID)
            } catch (e: IOException) {
                Log.e(TAG, "ConnectThread: run: IOException ${e.message}")
            }
            mmSocket = tmp
            mBluetoothAdapter.cancelDiscovery()

            try {
                mmSocket.connect()
                Log.d(TAG, "ConnectThread: run: ConnectThread connected.")
            } catch (e : IOException) {
                try {
                    mmSocket.close()
                    Log.d(TAG, "ConnectThread: run: Closed Socket.")
                    Log.e(TAG, "IOException: ${e.message}")
                } catch (ee: IOException) {
                    Log.e(TAG, "ConnectThread: run: IOException ${ee.message}")
                }
                Log.d(TAG, "ConnectThread: run: Could not connect to UUID: $MY_UUID_INSECURE")
            }
            connectSocket(mmSocket)
        }

        fun cancel() {
            try {
                Log.d(TAG, "ConnectThread: cancel: Canceling ConnectThread.")
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "ConnectThread: cancel: IOException ${e.message}")
            }
        }
    }


    /**
     * Finally the ConnectedThread which is responsible for maintaining the BTConnection,
     * sending and receiving incoming data through input/output streams respectively.
     */
    private inner class ConnectedThread(socket: BluetoothSocket): Thread() {
        private var mmSocket: BluetoothSocket = socket
        private var mmInStream: InputStream
        private var mmOutStream: OutputStream

        init {
            Log.d(TAG, "ConnectedThread: constructor: Starting.")
            lateinit var tmpIn: InputStream
            lateinit var tmpOut: OutputStream

            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut

            EventBus.getDefault().post(BluetoothConnectionEvent(true))
        }

        override fun run() {
            super.run()
            val buffer = ByteArray(1024)

            try {
                while (true) {
                    val size: Int = mmInStream.read(buffer)
                    val message = String(buffer, 0, size)
                    Log.d(TAG, "ConnectedThread: run: Message: $message")

                    if (context != null) {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            val activity = context as Activity
                            val textView = activity.findViewById<TextView>(R.id.simpleChat_textView)
                            textView.text = message
                            Log.d(TAG, "ConnectedThread: Handler Post: $message")
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "ConnectedThread: run: Input stream was disconnected ${e.message}")
            } finally {
                cancel()
            }
        }

        fun write(bytes: ByteArray) {
            val text = String(bytes, Charset.defaultCharset())
            Log.d(TAG, "ConnectedThread: write: Writing to outputstream: $text")
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "ConnectedThread: write IOException ${e.message}")
            }
        }

        fun cancel() {
            try {
                Log.d(TAG, "ConnectedThread: cancel: Canceling ConnectedThread.")
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "ConnectedThread: cancel: IOException ${e.message}")
            } finally {
                EventBus.getDefault().post(BluetoothConnectionEvent(false))
            }
        }
    }
}