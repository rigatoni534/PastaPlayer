package rigatoni534.apps.md3

enum class Md3PaletteStyle(
    val storageKey: String,
    val label: String
) {
    TONAL_SPOT("tonal_spot", "Tonal Spot"),
    VIBRANT("vibrant", "Vibrant"),
    EXPRESSIVE("expressive", "Expressive"),
    FRUIT_SALAD("fruit_salad", "Fruit Salad");

    companion object {
        val default: Md3PaletteStyle = TONAL_SPOT

        fun fromStorageKey(value: String?): Md3PaletteStyle {
            return entries.firstOrNull { it.storageKey == value } ?: default
        }
    }
}
