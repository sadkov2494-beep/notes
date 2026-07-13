-keep class com.notes.vault.data.model.** { *; }
-keep class com.notes.vault.data.export.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
