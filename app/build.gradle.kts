plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.secrets_gradle_plugin") version "0.6"
}

android {
    compileSdkVersion(30)

    defaultConfig {
        applicationId  = "com.komeyama.gas.study.android"
        minSdkVersion(25)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            minifyEnabled(false)
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    android.compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    android.buildFeatures.viewBinding = true
}

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$ext.kotlinVersion")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")

    /** coroutines **/
    val coroutinesVersion = "1.4.1"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    /** life cycle **/
    val lifeCycleVersion = "2.3.0"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifeCycleVersion")

    /** google auth **/
    val signInVersion = "19.2.0"
    implementation("com.google.android.gms:play-services-auth:$signInVersion")

    /** moshi **/
    val moshiVersion = "1.12.0"
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")

    /** retrofit **/
    val retrofitVersion = "2.9.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")

    /** timber **/
    val timberVersion = "5.0.1"
    implementation("com.jakewharton.timber:timber:$timberVersion")

    testImplementation("junit:junit:4.+")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}