import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.vanniktech.maven.publish.SonatypeHost

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
    namespace = "mx.empos.composefauthui"
    compileSdk = 34
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

    coordinates("mx.empos", "composefauthui", "1.0.0")

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