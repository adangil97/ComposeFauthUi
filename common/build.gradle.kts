import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.vanniktech.publish)
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
    val iosConf = libs.versions.ios
    val pods = iosConf.pods
    val firebaseAuthUi = pods.auth.ui.name.get()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        name = iosConf.basename.get()
        summary = iosConf.summary.get()
        homepage = iosConf.homepage.get()
        version = iosConf.version.get()
        ios.deploymentTarget = iosConf.target.get()
        framework {
            baseName = iosConf.basename.get()
            isStatic = false
        }
        pod(firebaseAuthUi) {
            version = pods.auth.ui.version.get()
        }
        pod(pods.google.auth.ui.get()) {
            useInteropBindingFrom(firebaseAuthUi)
            version = pods.auth.ui.version.get()
        }
        pod(pods.facebook.auth.ui.get()) {
            useInteropBindingFrom(firebaseAuthUi)
            version = pods.auth.ui.version.get()
        }
        pod(pods.email.auth.ui.get()) {
            useInteropBindingFrom(firebaseAuthUi)
            version = pods.auth.ui.version.get()
        }
        pod(pods.phone.auth.ui.get()) {
            useInteropBindingFrom(firebaseAuthUi)
            version = pods.auth.ui.version.get()
        }
        pod(pods.fb.core.name.get()) {
            version = pods.fb.core.version.get()
            linkOnly = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.firebase.auth.ui)
            implementation(libs.facebook.sdk)
            implementation(libs.androidx.appcompat)
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
    namespace = "mx.empos.composefauthui"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates("mx.empos", "composefauthui", "1.0.5")

    pom {
        name.set("Compose Fauth Ui")
        description.set("Compose Fauth Ui is a Compose Multiplatform Library to use Firebase Auth Ui")
        inceptionYear.set("2025")
        url.set("https://github.com/adangil97/ComposeFauthUi/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("adangil97")
                name.set("Adán Castillo")
                url.set("https://github.com/adangil97/")
            }
        }
        scm {
            url.set("https://github.com/adangil97/ComposeFauthUi/")
            connection.set("scm:git:git://github.com/adangil97/ComposeFauthUi.git")
            developerConnection.set("scm:git:ssh://git@github.com/adangil97/ComposeFauthUi.git")
        }
    }
}