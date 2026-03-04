// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false

    // 🔥 Firebase Google Services 플러그인 등록 (올바른 위치)
    id("com.google.gms.google-services") version "4.4.4" apply false


}
