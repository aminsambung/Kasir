package com.tokosaya.kasir.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

object PrinterHelper {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    suspend fun cetak(macAddress: String, teks: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: return@withContext Result.failure(Exception("Bluetooth tidak tersedia"))
                val device = adapter.getRemoteDevice(macAddress)
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val out: OutputStream = socket.outputStream
                out.write(byteArrayOf(0x1B, 0x40)) // init
                out.write(teks.toByteArray())
                out.write(byteArrayOf(0x0A, 0x0A, 0x0A))
                out.write(byteArrayOf(0x1D, 0x56, 0x00)) // cut
                out.flush()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                runCatching { socket?.close() }
            }
        }

    fun buildStruk(
        namaToko: String,
        nomorTrx: String,
        items: List<Triple<String, Int, Long>>, // nama, qty, subtotal
        total: Long,
        bayar: Long,
        kembalian: Long
    ): String {
        val sb = StringBuilder()
        sb.appendLine(namaToko)
        sb.appendLine("================================")
        sb.appendLine("No: $nomorTrx")
        sb.appendLine("--------------------------------")
        items.forEach { (nama, qty, sub) ->
            sb.appendLine(nama)
            sb.appendLine("  ${qty} x ${formatRupiah(sub / qty)} = ${formatRupiah(sub)}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("TOTAL    : ${formatRupiah(total)}")
        sb.appendLine("BAYAR    : ${formatRupiah(bayar)}")
        sb.appendLine("KEMBALI  : ${formatRupiah(kembalian)}")
        sb.appendLine("================================")
        sb.appendLine("Terima kasih!")
        return sb.toString()
    }
}
