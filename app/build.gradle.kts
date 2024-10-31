plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

android {
    namespace = "com.records.pesa"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.records.pesa"
        minSdk = 26
        targetSdk = 34
        versionCode = 120
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "license/*"
            excludes += "META-INF/{LICENSE*,NOTICE*,DEPENDENCIES,INDEX.LIST,README*}"
            excludes += "metadata_messages-defaults.properties"
            excludes += "properties-metadata.json"
            excludes += "jasperreports_extension.properties"
        }
    }
}

dependencies {

    // https://mvnrepository.com/artifact/com.opencsv/opencsv
    implementation("com.opencsv:opencsv:5.9")


    // https://mvnrepository.com/artifact/org.apache.commons/commons-csv
    implementation("org.apache.commons:commons-csv:1.11.0")


    // https://mvnrepository.com/artifact/org.mindrot/jbcrypt
    implementation("org.mindrot:jbcrypt:0.4")


    // https://mvnrepository.com/artifact/io.github.jan-tennert.supabase/postgrest-kt-android
    implementation("io.github.jan-tennert.supabase:postgrest-kt-android:2.6.1")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.4.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    implementation("io.github.jan-tennert.supabase:compose-auth-ui:2.6.1-dev")
    implementation("io.github.jan-tennert.supabase:compose-auth:2.6.1")



    implementation("io.ktor:ktor-client-cio:2.3.4")



    // https://mvnrepository.com/artifact/io.github.jan-tennert.supabase/gotrue-kt
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.6.1")

    // https://mvnrepository.com/artifact/com.jaspersoft/jrs-rest-java-client
//    implementation("com.jaspersoft:jrs-rest-java-client:9.0.0")

    // https://mvnrepository.com/artifact/com.itextpdf/itext7-core
    implementation("com.itextpdf:itext7-core:8.0.5")



//    // https://mvnrepository.com/artifact/org.eclipse.jdt/org.eclipse.jdt.core
//    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.38.0")
//
//    implementation("org.eclipse.jdt:org.eclipse.jdt.core.compiler:4.6.1")


//    implementation("org.eclipse.jdt.core.compiler:ecj:4.6.1")
//
//
//    implementation("net.sf.jasperreports:jasperreports-pdf:7.0.0")
//
//    // https://mvnrepository.com/artifact/net.sf.jasperreports/jasperreports
//    implementation("net.sf.jasperreports:jasperreports:7.0.0")

//    // https://mvnrepository.com/artifact/net.sf.jasperreports/jasperreports-jdt
//    implementation("net.sf.jasperreports:jasperreports-jdt:7.0.0")


    // https://mvnrepository.com/artifact/com.google.android.play/review-ktx
    implementation("com.google.android.play:review-ktx:2.0.1")

    // https://mvnrepository.com/artifact/net.sf.jasperreports/jasperreports-fonts
//    implementation("net.sf.jasperreports:jasperreports-fonts:7.0.0")

    // https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox
//    implementation("org.apache.pdfbox:pdfbox:3.0.3")

    // https://mvnrepository.com/artifact/androidx.work/work-runtime-ktx
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // https://mvnrepository.com/artifact/com.squareup.retrofit2/retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // https://mvnrepository.com/artifact/androidx.lifecycle/lifecycle-viewmodel-compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // https://mvnrepository.com/artifact/androidx.compose.runtime/runtime-livedata
    implementation("androidx.compose.runtime:runtime-livedata:1.6.8")

    // https://mvnrepository.com/artifact/com.google.accompanist/accompanist-permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")


    val vicoVersion = "2.0.0-alpha.25"

    // For Jetpack Compose.
    implementation("com.patrykandpatrick.vico:compose:$vicoVersion")

    // For `compose`. Creates a `ChartStyle` based on an M2 Material Theme.
    implementation("com.patrykandpatrick.vico:compose-m2:$vicoVersion")

    // For `compose`. Creates a `ChartStyle` based on an M3 Material Theme.
    implementation("com.patrykandpatrick.vico:compose-m3:$vicoVersion")

    // Houses the core logic for charts and other elements. Included in all other modules.
    implementation("com.patrykandpatrick.vico:core:$vicoVersion")

    //yChart
    implementation ("co.yml:ycharts:2.1.0")

    // horizontal pager
    implementation("com.google.accompanist:accompanist-pager:0.28.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.28.0")

    // Retrofit
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    //Gson
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    //SF
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    //navigation
    implementation("androidx.navigation:navigation-compose:2.6.0")

    //Date
    implementation ("androidx.compose.material3:material3:1.2.0-alpha02")

    //Viewmodel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    //Room
    implementation("androidx.room:room-runtime:${rootProject.extra["room_version"]}")
    ksp("androidx.room:room-compiler:${rootProject.extra["room_version"]}")
    implementation("androidx.room:room-ktx:${rootProject.extra["room_version"]}")

    //LiveData
    implementation ("androidx.compose.runtime:runtime-livedata")

    //Compose lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    // For Jetpack Compose.
    implementation("com.patrykandpatrick.vico:compose:2.0.0-alpha.25")

    // For `compose`. Creates a `ChartStyle` based on an M2 Material Theme.
    implementation("com.patrykandpatrick.vico:compose-m2:2.0.0-alpha.25")

    // For `compose`. Creates a `ChartStyle` based on an M3 Material Theme.
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.25")

    // Houses the core logic for charts and other elements. Included in all other modules.
    implementation("com.patrykandpatrick.vico:core:2.0.0-alpha.25")
    



    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}