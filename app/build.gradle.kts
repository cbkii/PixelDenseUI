import java.util.Properties

plugins {
    id("com.android.application")
}

val releasePropertiesFile = rootProject.file("keystore.properties")
val releaseProperties = Properties()
val hasReleaseSigning = releasePropertiesFile.isFile
if (hasReleaseSigning) {
    releasePropertiesFile.inputStream().use { releaseProperties.load(it) }
}

fun releaseProperty(name: String): String =
    releaseProperties.getProperty(name) ?: error("Missing release signing property: $name")

android {
    namespace = "dev.pixeldenseui"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.pixeldenseui"
        minSdk = 36
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.2"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseProperty("storeFile"))
                storePassword = releaseProperty("storePassword")
                keyAlias = releaseProperty("keyAlias")
                keyPassword = releaseProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:101.0.1")
    implementation("io.github.libxposed:service:101.0.0")
    testImplementation("junit:junit:4.13.2")
}
