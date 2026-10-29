package com.studio.wastickermaker.ui.theme

import com.studio.wastickermaker.data.model.AppLanguage
import com.studio.wastickermaker.data.model.MediaInputType
import com.studio.wastickermaker.data.model.ScaleFitMode
import com.studio.wastickermaker.ui.state.StickerPackSegment

data class StickerStrings(
    val appTitle: String,
    val appSubtitle: String,
    val newButton: String,
    val settingsButtonDesc: String,
    val chooseMediaButton: String,
    val stickerTextLabel: String,
    val stickerTextPlaceholder: String,
    val trimVideoTitle: (Int) -> String,
    val trimSelectedSuffix: String,
    val previewTitle: String,
    val readyBadgeSuffix: String,
    val emptyPreviewPlaceholder: String,
    val saveToSegmentButton: (String) -> String,
    val savedInSegmentButton: (String) -> String,
    val savedToast: (String) -> String,
    val collectionTitle: String,
    val stickerCountBadge: (Int) -> String,
    val staticSegmentLabel: String,
    val animatedSegmentLabel: String,
    val collectionDesc: (String) -> String,
    val addPackToWhatsAppButton: (String, Int) -> String,
    val emptyCollectionPlaceholder: (String) -> String,
    val deletedToast: String,
    val packAddedToast: String,
    val whatsAppErrorToast: String,
    val switchToAnimatedNotice: String,
    val switchToStaticNotice: String,
    // Settings Screen
    val settingsTitle: String,
    val settingsSubtitle: String,
    val backButton: String,
    val languageSettingTitle: String,
    val languageSettingDesc: String,
    val themeSettingTitle: String,
    val themeSettingDesc: String,
    val resolutionSettingTitle: String,
    val resolutionSettingDesc: String,
    val durationSettingTitle: String,
    val durationSettingDesc: String,
    val secondsUnit: String,
    val defaultFitSettingTitle: String,
    val defaultFitSettingDesc: String,
    val whatsAppTargetTitle: String,
    val whatsAppTargetDesc: String,
    val publisherSettingTitle: String,
    val publisherSettingDesc: String,
    val publisherPlaceholder: String,
    val storageSettingTitle: String,
    val clearCacheButton: String,
    val cacheClearedToast: (String) -> String
) {
    fun segmentLabel(segment: StickerPackSegment): String = when (segment) {
        StickerPackSegment.STATIC -> staticSegmentLabel
        StickerPackSegment.ANIMATED -> animatedSegmentLabel
    }

    fun fitModeLabel(mode: ScaleFitMode, lang: AppLanguage): String = when (mode) {
        ScaleFitMode.CENTER_CROP -> if (lang == AppLanguage.INDONESIAN) "Penuhi Kotak" else "Fill Square"
        ScaleFitMode.FIT_CENTER -> if (lang == AppLanguage.INDONESIAN) "Utuh (Sesuai Gambar)" else "Fit Whole Image"
    }

    fun mediaTypeLabel(type: MediaInputType, lang: AppLanguage): String = when (type) {
        MediaInputType.STATIC_IMAGE -> if (lang == AppLanguage.INDONESIAN) "Foto" else "Photo"
        MediaInputType.VIDEO -> "Video"
        MediaInputType.ANIMATED_GIF -> "GIF"
    }
}

object AppStrings {
    private val indonesian = StickerStrings(
        appTitle = "Sticker Maker",
        appSubtitle = "Ubah foto, GIF, atau video menjadi stiker WhatsApp",
        newButton = "Baru",
        settingsButtonDesc = "Pengaturan",
        chooseMediaButton = "Pilih Foto, GIF, atau Video",
        stickerTextLabel = "Teks Stiker (Meme / Caption)",
        stickerTextPlaceholder = "Ketik teks untuk stiker...",
        trimVideoTitle = { maxSec -> "Potong Video (Maks $maxSec detik)" },
        trimSelectedSuffix = "dipilih",
        previewTitle = "Preview Stiker",
        readyBadgeSuffix = "Siap",
        emptyPreviewPlaceholder = "Stiker Anda akan tampil di sini",
        saveToSegmentButton = { seg -> "+ Simpan ke $seg" },
        savedInSegmentButton = { seg -> "Tersimpan di $seg ✓" },
        savedToast = { seg -> "Stiker berhasil disimpan ke $seg!" },
        collectionTitle = "Koleksi Stiker Saya",
        stickerCountBadge = { count -> "$count Stiker" },
        staticSegmentLabel = "Stiker Foto",
        animatedSegmentLabel = "Stiker Gerak",
        collectionDesc = { seg -> "Kelola ${seg.lowercase()}. Stiker di segmen ini otomatis masuk ke keyboard WhatsApp." },
        addPackToWhatsAppButton = { seg, count -> "Tambahkan $seg ($count) ke WhatsApp" },
        emptyCollectionPlaceholder = { seg -> "Belum ada ${seg.lowercase()} di koleksi.\nBuat stiker pertamamu di atas!" },
        deletedToast = "Stiker dihapus dari koleksi.",
        packAddedToast = "Koleksi stiker berhasil ditambahkan ke WhatsApp!",
        whatsAppErrorToast = "Tidak dapat membuka WhatsApp.",
        switchToAnimatedNotice = "Beralih ke segmen Stiker Gerak. Stiker foto Anda tetap aman tersimpan!",
        switchToStaticNotice = "Beralih ke segmen Stiker Foto. Stiker gerak Anda tetap aman tersimpan!",
        settingsTitle = "Pengaturan",
        settingsSubtitle = "Sesuaikan bahasa, kualitas stiker, batas durasi, dan tema aplikasi",
        backButton = "Kembali",
        languageSettingTitle = "Bahasa Aplikasi",
        languageSettingDesc = "Pilih bahasa antarmuka aplikasi",
        themeSettingTitle = "Tema Aplikasi",
        themeSettingDesc = "Pilih tampilan warna terang, gelap, atau ikuti sistem",
        resolutionSettingTitle = "Resolusi & Kualitas Stiker (%)",
        resolutionSettingDesc = "Resolusi lebih rendah menghasilkan ukuran KB lebih kecil & proses lebih cepat",
        durationSettingTitle = "Batas Durasi GIF & Video",
        durationSettingDesc = "Durasi maksimal untuk stiker animasi (GIF / Video)",
        secondsUnit = "detik",
        defaultFitSettingTitle = "Mode Ukuran Default",
        defaultFitSettingDesc = "Mode pemotongan otomatis saat memilih file baru",
        whatsAppTargetTitle = "Target Aplikasi WhatsApp",
        whatsAppTargetDesc = "Pilih aplikasi tujuan saat menambahkan koleksi stiker",
        publisherSettingTitle = "Nama Pembuat Stiker (Author)",
        publisherSettingDesc = "Ditampilkan di info stiker dalam aplikasi WhatsApp",
        publisherPlaceholder = "Contoh: Sticker Maker",
        storageSettingTitle = "Penyimpanan & Cache",
        clearCacheButton = "Bersihkan Cache Preview",
        cacheClearedToast = { kb -> "Cache preview dibersihkan ($kb KB dibebaskan)." }
    )

    private val english = StickerStrings(
        appTitle = "Sticker Maker",
        appSubtitle = "Turn any photo, GIF, or video into a WhatsApp sticker",
        newButton = "New",
        settingsButtonDesc = "Settings",
        chooseMediaButton = "Choose Photo, GIF, or Video",
        stickerTextLabel = "Sticker Text (Meme / Caption)",
        stickerTextPlaceholder = "Type caption text for your sticker...",
        trimVideoTitle = { maxSec -> "Trim Video (Max $maxSec seconds)" },
        trimSelectedSuffix = "selected",
        previewTitle = "Sticker Preview",
        readyBadgeSuffix = "Ready",
        emptyPreviewPlaceholder = "Your sticker will appear here",
        saveToSegmentButton = { seg -> "+ Save to $seg" },
        savedInSegmentButton = { seg -> "Saved in $seg ✓" },
        savedToast = { seg -> "Sticker saved to $seg!" },
        collectionTitle = "My Sticker Collection",
        stickerCountBadge = { count -> "$count Stickers" },
        staticSegmentLabel = "Photo Stickers",
        animatedSegmentLabel = "Animated Stickers",
        collectionDesc = { seg -> "Manage your ${seg.lowercase()}. Stickers here are exported as a pack to WhatsApp." },
        addPackToWhatsAppButton = { seg, count -> "Add $seg ($count) to WhatsApp" },
        emptyCollectionPlaceholder = { seg -> "No ${seg.lowercase()} in collection yet.\nCreate your first sticker above!" },
        deletedToast = "Sticker removed from collection.",
        packAddedToast = "Sticker Pack added to WhatsApp!",
        whatsAppErrorToast = "Could not open WhatsApp.",
        switchToAnimatedNotice = "Switched to Animated Stickers. Your photo stickers remain safely saved!",
        switchToStaticNotice = "Switched to Photo Stickers. Your animated stickers remain safely saved!",
        settingsTitle = "Settings",
        settingsSubtitle = "Customize language, sticker resolution, duration limit, and app theme",
        backButton = "Back",
        languageSettingTitle = "App Language",
        languageSettingDesc = "Choose the display language for the interface",
        themeSettingTitle = "App Theme",
        themeSettingDesc = "Switch between light, dark, or system default appearance",
        resolutionSettingTitle = "Sticker Resolution & Quality (%)",
        resolutionSettingDesc = "Lower resolution produces smaller KB files and faster encoding",
        durationSettingTitle = "GIF & Video Duration Limit",
        durationSettingDesc = "Maximum duration allowed for animated GIF & video stickers",
        secondsUnit = "sec",
        defaultFitSettingTitle = "Default Fit Mode",
        defaultFitSettingDesc = "Default scaling mode when picking a new media file",
        whatsAppTargetTitle = "Target WhatsApp App",
        whatsAppTargetDesc = "Choose which WhatsApp app receives your sticker packs",
        publisherSettingTitle = "Sticker Creator Name (Author)",
        publisherSettingDesc = "Displayed in sticker pack info inside WhatsApp",
        publisherPlaceholder = "e.g. Sticker Maker",
        storageSettingTitle = "Storage & Cache",
        clearCacheButton = "Clear Preview Cache",
        cacheClearedToast = { kb -> "Preview cache cleared ($kb KB freed)." }
    )

    fun forLanguage(language: AppLanguage): StickerStrings = when (language) {
        AppLanguage.INDONESIAN -> indonesian
        AppLanguage.ENGLISH -> english
    }
}
