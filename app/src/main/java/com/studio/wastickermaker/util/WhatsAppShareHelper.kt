package com.studio.wastickermaker.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.studio.wastickermaker.BuildConfig
import com.studio.wastickermaker.data.model.WhatsAppTarget

/**
 * Builds the official `com.whatsapp.intent.action.ENABLE_STICKER_PACK` intent
 * for launching WhatsApp's sticker pack importer.
 */
object WhatsAppShareHelper {

    const val WHATSAPP_CONSUMER_PACKAGE = "com.whatsapp"
    const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    // Official WhatsApp Sticker Pack Intent Action & Extras
    const val ACTION_ENABLE_STICKER_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
    const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
    const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"

    fun createAddStickerPackIntent(
        context: Context,
        packIdentifier: String = StickerContentProvider.STATIC_PACK_ID,
        packName: String = StickerContentProvider.STATIC_PACK_NAME,
        whatsAppTarget: WhatsAppTarget = WhatsAppTarget.AUTO
    ): Intent {
        val targetPackage = when (whatsAppTarget) {
            WhatsAppTarget.CONSUMER -> resolveInstalledWhatsAppPackage(context, preferBusiness = false)
            WhatsAppTarget.BUSINESS -> resolveInstalledWhatsAppPackage(context, preferBusiness = true)
            WhatsAppTarget.AUTO -> resolveInstalledWhatsAppPackage(context, preferBusiness = false)
        }
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
