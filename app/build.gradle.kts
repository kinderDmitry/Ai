plugins { id("com.android.application") }

android {
    namespace = "com.jarvis.nextgen"
    compileSdk = 36
    defaultConfig { applicationId = "com.jarvis.nextgen"; minSdk = 26; targetSdk = 36; versionCode = 53; versionName = "1.53.1" }
    buildTypes { release { isMinifyEnabled = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    dependencies {
        implementation("androidx.core:core:1.17.0")
        implementation("com.google.mlkit:text-recognition:16.0.1")
        testImplementation("junit:junit:4.13.2")
    }

    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
