plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    // ponytail: applicationId owns the installed identity; source packages stay stable until the pruning slice.
    namespace = "com.example.ava"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.mfuertes.biscuit.ava"
        minSdk = 22
        targetSdk = 36

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
        versionCode = if (project.ext.has("versionCode"))
            project.ext.get("versionCode").toString().toInt() else 31
        versionName = if (project.ext.has("versionName"))
            project.ext.get("versionName").toString() else "0.3.1"
        base.archivesName = "Ava-$versionName"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseKey = file(System.getProperty("user.home") + "/ava-key.jks")
    signingConfigs {
        if (releaseKey.exists()) {
            create("release") {
                storeFile = releaseKey
                storePassword = "123456"
                keyAlias = "ava"
                keyPassword = "123456"
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            // ponytail: prune real inputs; skip R8 until release-size pressure is proven.
            isMinifyEnabled = false
            isShrinkResources = false
            // ponytail: CI/dev boxes without the private key still produce an inspectable unsigned release APK.
            if (releaseKey.exists()) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    buildFeatures {
        aidl = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            pickFirsts.add("**/libc++_shared.so")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

}

dependencies {

    implementation(project(":esphomeproto"))
    implementation(project(":microfeatures"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.litert)
    implementation(libs.protobuf.kotlin)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.media3.exoplayer)
    
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)

}

val headlessSmokeScript = rootProject.file("scripts/smoke-headless.sh")

tasks.register<Exec>("smokeHeadless") {
    group = "verification"
    description = "Installs the appliance APK and verifies voice-service and ESPHome listener startup."
    dependsOn("assembleDebug")
    doFirst {
        // ponytail: archivesName changes the apk filename, so find it instead of hard-coding it.
        val apkDir = project.layout.buildDirectory.dir("outputs/apk/debug").get().asFile
        val apk = apkDir.listFiles { f -> f.name.endsWith(".apk") && !f.name.contains("androidTest") }
            ?.firstOrNull()
            ?: error("No debug APK found in $apkDir")
        commandLine("bash", headlessSmokeScript.absolutePath, "--apk", apk.absolutePath)
    }
}

tasks.register<Exec>("smokeHeadlessSelfCheck") {
    group = "verification"
    description = "Validates the headless appliance smoke script without a device."
    commandLine("bash", headlessSmokeScript.absolutePath, "--self-check")
}

tasks.named("check") {
    dependsOn("smokeHeadlessSelfCheck")
}
