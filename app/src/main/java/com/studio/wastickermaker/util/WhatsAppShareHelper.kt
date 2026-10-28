package com.studio.wastickermaker.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.studio.wastickermaker.BuildConfig
import java.io.File

/**
 * Helper for sending 512x512 .webp stickers to WhatsApp:
 * - Option 1 (Direct Single Share): Sends a 512x512 .webp (with EXIF metadata + alpha) via ACTION_SEND.
 * - Option 2 (Sticker Pack Integration): Builds the official `com.whatsapp.intent.action.ENABLE_STICKER_PACK`
 *   intent to be launched via ActivityResultLauncher / startActivityForResult so WhatsApp validates the calling package.
 */
object WhatsAppShareHelper {

    const val WHATSAPP_CONSUMER_PACKAGE = "com.whatsapp"
    const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    const val MIME_TYPE_WEBP = "image/webp"
    const val ADD_PACK_REQUEST_CODE = 200

    // Official WhatsApp Sticker Pack Intent Action & Extras
    const val ACTION_ENABLE_STICKER_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
    const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
    const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"

    /**
     * Option 1: Sends a single 512x512 .webp sticker directly to WhatsApp via Intent.ACTION_SEND.
     */
    fun shareSingleStickerToWhatsApp(
        context: Context,
        webpFile: File,
        preferWhatsAppBusiness: Boolean = false
    ): Result<Unit> {
        if (!webpFile.exists() || webpFile.length() == 0L) {
            return Result.failure(IllegalArgumentException("Sticker file is missing."))
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, webpFile)
            val targetPackage = resolveInstalledWhatsAppPackage(context, preferWhatsAppBusiness)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE_WEBP
                putExtra(Intent.EXTRA_STREAM, contentUri)
                clipData = ClipData.newRawUri("Sticker", contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (targetPackage != null) {
                    setPackage(targetPackage)
                }
            }

            if (targetPackage != null) {
                context.startActivity(shareIntent)
            } else {
                val chooser = Intent.createChooser(shareIntent, "Send Sticker to WhatsApp").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(chooser)
            }
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp is not installed on this device.", Toast.LENGTH_LONG).show()
            Result.failure(e)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not send sticker: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            Result.failure(e)
        }
    }

    /**
     * Option 2: Creates the official WhatsApp `ENABLE_STICKER_PACK` Intent.
     * IMPORTANT: Must be launched with `ActivityResultLauncher.launch(intent)` or
     * `activity.startActivityForResult(intent, ADD_PACK_REQUEST_CODE)` WITHOUT `FLAG_ACTIVITY_NEW_TASK`
     * so WhatsApp's `AddThirdPartyStickerPackActivity` receives a non-null `callingPackage`.
     */
    fun createAddStickerPackIntent(
        context: Context,
        packIdentifier: String = StickerContentProvider.DEFAULT_PACK_ID,
        packName: String = StickerContentProvider.DEFAULT_PACK_NAME
    ): Intent {
        val targetPackage = resolveInstalledWhatsAppPackage(context)
        return Intent(ACTION_ENABLE_STICKER_PACK).apply {
            putExtra(EXTRA_STICKER_PACK_ID, packIdentifier)
            putExtra(EXTRA_STICKER_PACK_AUTHORITY, BuildConfig.CONTENT_PROVIDER_AUTHORITY)
            putExtra(EXTRA_STICKER_PACK_NAME, packName)
            if (targetPackage != null) {
                setPackage(targetPackage)
            }
        }
    }

    fun resolveInstalledWhatsAppPackage(
        context: Context,
        preferBusiness: Boolean = false
    ): String? {
        val candidates = if (preferBusiness) {
            listOf(WHATSAPP_BUSINESS_PACKAGE, WHATSAPP_CONSUMER_PACKAGE)
        } else {
            listOf(WHATSAPP_CONSUMER_PACKAGE, WHATSAPP_BUSINESS_PACKAGE)
        }
        return candidates.firstOrNull { pkg -> isPackageInstalled(context.packageManager, pkg) }
    }

    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
