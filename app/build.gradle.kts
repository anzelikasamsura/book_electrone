plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.bookelectrone"
    compileSdk = 34
    compileOptions.encoding = "UTF-8"


    defaultConfig {
        applicationId = "com.example.bookelectrone"
        minSdk = 28
        targetSdk = 34
        multiDexEnabled = true
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude( "META-INF/NOTICE")
        exclude( "META-INF/LICENSE")
        exclude( "META-INF/LICENSE.txt")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/NOTICE.md")
        exclude("META-INF/LICENSE.md")
        exclude("META-INF/activation-api")
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.ui.text.android)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    // Дизайн
    implementation("com.google.android.material:material:1.9.0")

    // Работа с документами Word
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Работа с XML
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
    implementation("org.codehaus.woodstox:woodstox-core-asl:4.4.1")
    implementation("javax.xml.stream:stax-api:1.0-2")

    // PDF Viewer для выгрузки PDF
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.1")

    // Работа с тестами
    implementation("com.google.code.gson:gson:2.8.9")


}