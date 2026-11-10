package de.uhrwerksarchiv.watchaccurancy.data.model

import androidx.compose.ui.graphics.Color

/**
 * Uhrenlagen / Orientierungen nach historischem Standard (WatchCheck).
 */
enum class WatchPosition(
    val code: String,
    val germanTitle: String,
    val shortLabel: String,
    val description: String,
    val color: Color
) {
    WORN(
        code = "WORN",
        germanTitle = "Am Arm getragen",
        shortLabel = "Am Arm",
        description = "Uhr wird am Handgelenk getragen",
        color = Color(0xFF00B4D8)
    ),
    WINDER(
        code = "WINDER",
        germanTitle = "Uhrenbeweger",
        shortLabel = "Beweger",
        description = "Auf einem Uhrenbeweger im Umlauf",
        color = Color(0xFF7209B7)
    ),
    DU(
        code = "DU",
        germanTitle = "Zifferblatt oben (Dial Up)",
        shortLabel = "ZO (DU)",
        description = "Flach liegend, Zifferblatt nach oben",
        color = Color(0xFF2EC4B6)
    ),
    DD(
        code = "DD",
        germanTitle = "Zifferblatt unten (Dial Down)",
        shortLabel = "ZU (DD)",
        description = "Flach liegend, Zifferblatt nach unten",
        color = Color(0xFFFF9F1C)
    ),
    CU(
        code = "CU",
        germanTitle = "Krone oben (Crown Up)",
        shortLabel = "KO (CU)",
        description = "Vertikal, Krone nach oben (12 Uhr unten)",
        color = Color(0xFF9D4EDD)
    ),
    CD(
        code = "CD",
        germanTitle = "Krone unten (Crown Down)",
        shortLabel = "KU (CD)",
        description = "Vertikal, Krone nach unten (12 Uhr oben)",
        color = Color(0xFFE63946)
    ),
    CL(
        code = "CL",
        germanTitle = "Krone links (Crown Left)",
        shortLabel = "KL (CL)",
        description = "Vertikal, Krone nach links (9 Uhr unten)",
        color = Color(0xFF4361EE)
    ),
    CR(
        code = "CR",
        germanTitle = "Krone rechts (Crown Right)",
        shortLabel = "KR (CR)",
        description = "Vertikal, Krone nach rechts (3 Uhr unten)",
        color = Color(0xFF38B000)
    );

    companion object {
        fun fromCode(code: String?): WatchPosition {
            if (code.isNullOrBlank()) return WORN
            val trimmed = code.trim().uppercase()
            return entries.firstOrNull { it.code == trimmed } ?: WORN
        }
    }

    fun getLocalizedTitle(context: android.content.Context): String {
        val resId = when (this) {
            WORN -> com.example.R.string.pos_worn
            WINDER -> com.example.R.string.pos_winder
            DU -> com.example.R.string.pos_display_up
            DD -> com.example.R.string.pos_display_down
            CU -> com.example.R.string.pos_crown_up
            CD -> com.example.R.string.pos_crown_down
            CL -> com.example.R.string.pos_crown_left
            CR -> com.example.R.string.pos_crown_right
        }
        return context.getString(resId)
    }

    fun getLocalizedShortLabel(context: android.content.Context): String {
        val resId = when (this) {
            WORN -> com.example.R.string.pos_worn_short
            WINDER -> com.example.R.string.pos_winder_short
            DU -> com.example.R.string.pos_display_up_short
            DD -> com.example.R.string.pos_display_down_short
            CU -> com.example.R.string.pos_crown_up_short
            CD -> com.example.R.string.pos_crown_down_short
            CL -> com.example.R.string.pos_crown_left_short
            CR -> com.example.R.string.pos_crown_right_short
        }
        return context.getString(resId)
    }
}
