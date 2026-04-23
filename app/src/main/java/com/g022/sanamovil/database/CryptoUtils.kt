import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val key = SecretKeySpec("1234567890123456".toByteArray(), "AES") // Usa una clave de 16 caracteres
    private val iv = IvParameterSpec(ByteArray(16)) // Vector de inicialización

    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encrypted = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(encryptedData: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
        return String(cipher.doFinal(decoded))
    }
}