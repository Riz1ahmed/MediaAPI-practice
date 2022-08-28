package com.learner.codereducer.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.math.BigInteger
import java.net.*
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.Security
import java.util.*

object DeviceUtils {
    private const val NO_CARRIER = "nocarrier"
    private const val NO_TIMEZONE = "notimezone"
    private const val NO_BRAND = "nobrand"
    private const val NO_MODEL = "nomodel"

    fun showKeyboard(context: Context) =
        ((context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as? InputMethodManager)!!)
            .toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

    fun hideKeyboard(context: Context, view: View) =
        ((context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as? InputMethodManager)!!)
            .hideSoftInputFromWindow(view.windowToken, 0)

    // TCP/HTTP/DNS (depending on the port, 53=DNS, 80=HTTP, etc.)
    fun isOnline(): Boolean {
        return try {
            val timeoutMs = 1500
            val sock = Socket()
            val socketAddress: SocketAddress = InetSocketAddress("8.8.8.8", 53)
            sock.connect(socketAddress, timeoutMs)
            sock.close()
            true
        } catch (e: IOException) {
            false
        }
    }

    fun AppCompatActivity.copyToClipboard(path: String) {
        val clipboard = getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Image Path", path)
        clipboard.setPrimaryClip(clip)
    }

    val screenWidth: Int
        get() = Resources.getSystem().displayMetrics.widthPixels
    val screenHeight: Int
        get() = Resources.getSystem().displayMetrics.heightPixels

    fun getDeviceScreenHeightDp(context: Context): Float {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.heightPixels / displayMetrics.density
    }

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun Context.dpToPx(dp: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        )
    }

    fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }

    fun Context.pxToDp(px: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX, px.toFloat(), resources.displayMetrics
        )
    }

    fun isNetworkPresent(context: Context): Boolean {
        var isNetworkAvailable = false
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            if (netInfo != null) {
                isNetworkAvailable = netInfo.isConnectedOrConnecting
            }

            // check for wifi also
            if (!isNetworkAvailable) {
                val connect =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifi = cm.getNetworkInfo(1)!!.state
                isNetworkAvailable = (connect.isWifiEnabled
                        && wifi.toString().equals("CONNECTED", ignoreCase = true))
            }
        } catch (ex: Exception) {
            Log.e("Network Avail Error", ex.message!!)
        }
        return isNetworkAvailable
    }

    fun isInternetAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }
                }
            }
        }

        return result
    }

    /**
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return  mac address or empty string
     */
    fun getMACAddress(interfaceName: String?): String {
        try {
            val interfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (interfaceName != null) {
                    if (!intf.name.equals(interfaceName, ignoreCase = true)) continue
                }
                val mac = intf.hardwareAddress ?: return ""
                val buf = StringBuilder()
                for (aMac in mac) buf.append(String.format("%02X:", aMac))
                if (buf.isNotEmpty()) buf.deleteCharAt(buf.length - 1)
                return buf.toString()
            }
        } catch (ignored: Exception) {
        } // for now eat exceptions
        return "02:00:00:00:00:00" //exception
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    fun getIPAddress(useIPv4: Boolean): String {
        try {
            val interfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (useIPv4) {
                            if (isIPv4) return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                                return if (delim < 0) sAddr.toUpperCase() else sAddr.substring(
                                    0,
                                    delim
                                ).toUpperCase()
                            }
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        } // for now eat exceptions
        return ""
    }

    fun getAbi(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // on newer Android versions, we'll return only the most important Abi version
            Build.SUPPORTED_ABIS[0]
        } else {
            // on pre-Lollipop versions, we got only one Abi
            Build.CPU_ABI
        }
    }

    fun getWifiIPAddress(context: Context): String {
        var ipAddressString = ""
        try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            var ipAddress = wifiManager.connectionInfo.ipAddress

            // Convert little-endian to big-endianif needed
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                ipAddress = Integer.reverseBytes(ipAddress)
            }
            val ipByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()
            ipAddressString = InetAddress.getByAddress(ipByteArray).hostAddress
        } catch (ignored: Exception) {
        } // for now eat exceptions
        return ipAddressString
    }

    private var umtimezone: String? = null
    private val timeZone: String?
        get() {
            if (null != umtimezone && umtimezone!!.isNotEmpty()) return umtimezone

            umtimezone = NO_TIMEZONE
            try {
                umtimezone = TimeZone.getDefault().id
                    .replace("[^a-zA-Z0-9]+".toRegex(), "_")
            } catch (e: Exception) {
            }
            return umtimezone
        }

    private var ubrand: String? = null
    val brand: String?
        get() {
            if (null != ubrand && ubrand!!.isNotEmpty()) return ubrand
            ubrand = NO_BRAND
            ubrand = Build.BRAND.replace("[^a-zA-Z0-9]+".toRegex(), "_")
            return ubrand
        }

    private var umodel: String? = null
    val model: String?
        get() {
            if (null != umodel && umodel!!.isNotEmpty()) {
                return umodel
            }
            umodel = NO_MODEL
            umodel = Build.MODEL.replace("[^a-zA-Z0-9]+".toRegex(), "_")
            return umodel
        }

    private var udeviceversion: String? = null
    val deviceVersion: String?
        get() {
            if (null != udeviceversion && udeviceversion!!.isNotEmpty()) {
                return udeviceversion
            }
            udeviceversion = "0.0"
            udeviceversion = Build.VERSION.RELEASE.replace("[^.a-zA-Z0-9]+".toRegex(), "_")
            return udeviceversion
        }


    private var ucarriername: String? = null
    fun getCarrierName(mContext: Context): String? {
        if (null != ucarriername && ucarriername!!.isNotEmpty()) {
            return ucarriername
        }
        ucarriername = NO_CARRIER
        try {
            val manager = mContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            ucarriername = manager.networkOperatorName
            ucarriername = if (null != ucarriername && ucarriername!!.isNotEmpty()) {
                ucarriername!!.replace("[^a-zA-Z0-9]+".toRegex(), "_")
            } else NO_CARRIER
        } catch (e: Exception) {
        }
        return ucarriername
    }

    fun getUniqueID(mContext: Context): String {
        var m_szDevIDShort = ("" + getCarrierName(mContext)
                + "" + timeZone
                + "" + Build.VERSION.RELEASE
                + "" + Build.VERSION.INCREMENTAL
                + "" + Build.VERSION.SDK_INT
                + "" + Build.DEVICE
                + "" + Build.BOARD
                + "" + Build.BOOTLOADER
                + "" + Build.BRAND
                + "" + Build.CPU_ABI
                + "" + Build.CPU_ABI2
                + "" + Build.DISPLAY
                + "" + Build.FINGERPRINT
                + "" + Build.HARDWARE
                + "" + Build.HOST
                + "" + Build.ID
                + "" + Build.MANUFACTURER
                + "" + Build.MODEL
                + "" + Build.PRODUCT
                + "" + Build.SERIAL
                + "" + Build.TAGS
                + "" + Build.TIME
                + "" + Build.TYPE
                + "" + Build.UNKNOWN
                + "" + Build.USER
                + "" + getMACAddress("wlan0")
                + "" + getMACAddress("eth0")
                + "" + getWifiIPAddress(mContext)
                + "" + getIPAddress(true)
                + "" + getIPAddress(false))
        m_szDevIDShort = m_szDevIDShort.toLowerCase(Locale.ROOT)
        var m_szUniqueID = ""
        var mMessageDigest: MessageDigest? = null
        try {
            mMessageDigest = MessageDigest.getInstance("SHA-256")
        } catch (ex1: NoSuchAlgorithmException) {
            try {
                mMessageDigest = MessageDigest.getInstance("MD5")
            } catch (ex2: NoSuchAlgorithmException) {
                ex2.printStackTrace()
                val algorithms = Security.getAlgorithms("MessageDigest")
                var availableAlgo: String? = ""
                for (algo in algorithms) {
                    availableAlgo = algo
                    break
                }
                try {
                    mMessageDigest = MessageDigest.getInstance(availableAlgo)
                } catch (ex3: NoSuchAlgorithmException) {
                }
            }
        }
        if (null != mMessageDigest) {
            mMessageDigest.update(m_szDevIDShort.toByteArray(), 0, m_szDevIDShort.length)
            val p_md5Data = mMessageDigest.digest()
            for (i in p_md5Data.indices) {
                val b = 0xFF and p_md5Data[i].toInt()
                // if it is a single digit, make sure it have 0 in front (proper padding)
                if (b <= 0xF) m_szUniqueID += "0"
                // add number to string
                m_szUniqueID += Integer.toHexString(b)
            }
        } else {
            m_szUniqueID = generateLongString(m_szDevIDShort)
        }
        if (m_szUniqueID.length > 80) {
            m_szUniqueID = m_szUniqueID.substring(0, 79)
        }
        return m_szUniqueID
    }

    fun notifyGallery(){

    }

    fun generateLongString(inputString: String): String {
        val str1 = inputString.substring(0, inputString.length / 2)
        val str2 = inputString.substring(inputString.length / 2, inputString.length - 1)
        val hash1 = (str1.hashCode() and 0xffffffff.toInt() shl 16).toLong()
        val hash2 = (str2.hashCode() and 0xffffffff.toInt() shl 16).toLong()
        return hash1.toString(36) + hash2.toString(36)
    }
}