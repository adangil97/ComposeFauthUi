import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "common"
            isStatic = true
        }
        val firebaseAuthUi = libs.versions.pod.auth.ui.get()
        pod(firebaseAuthUi) {
            version = "13.1.0"
        }
        pod(libs.versions.pod.google.auth.ui.get()) {
            useInteropBindingFrom(firebaseAuthUi)
            version = "13.1.0"
        }
        pod(libs.versions.pod.facebook.auth.ui.get()) {
            useInteropBindingFrom(firebaseAuthUi)
            version = "13.1.0"
        }
        pod(libs.versions.pod.email.auth.ui.get()) {
            useInteropBindingFrom(firebaseAuthUi)
            version = "13.1.0"
        }
        pod(libs.versions.pod.fb.core.get()) {
            version = "16.3.1"
            linkOnly = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.firebase.auth.ui)
            implementation(libs.facebook.sdk)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "io.empos.composefauthui"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}