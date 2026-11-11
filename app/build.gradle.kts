plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sky22333.skyadb"
    compileSdk = 37
    val ciVersionCode = providers.environmentVariable("VERSION_CODE").map { it.toInt() }.orNull
    val ciVersionName = providers.environmentVariable("VERSION_NAME").orNull
    val signingStoreFile = providers.environmentVariable("SIGNING_STORE_FILE").orNull
    val keyAliasValue = providers.environmentVariable("KEY_ALIAS").orNull
    val keyStorePasswordValue = providers.environmentVariable("KEY_STORE_PASSWORD").orNull
    val keyPasswordValue = providers.environmentVariable("KEY_PASSWORD").orNull
    val releaseSigningEnabled = !signingStoreFile.isNullOrBlank() &&
        !keyAliasValue.isNullOrBlank() &&
        !keyStorePasswordValue.isNullOrBlank() &&
        !keyPasswordValue.isNullOrBlank()

    defaultConfig {
        applicationId = "com.sky22333.skyadb"
        minSdk = 24
        targetSdk = 37
        versionCode = ciVersionCode ?: 10000
        versionName = ciVersionName ?: "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseSigningEnabled) {
                storeFile = file(signingStoreFile!!)
                storeType = "PKCS12"
                keyAlias = keyAliasValue!!
                storePassword = keyStorePasswordValue!!
                keyPassword = keyPasswordValue!!
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningEnabled) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // Bouncy Castle's Java multi-release OSGi descriptors are not Android runtime resources.
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            merges += "META-INF/LICENSE.md"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kadb)
    implementation(libs.kadb.mdns)
    implementation(libs.fastboot.java)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.okio)
    implementation(libs.timber)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Build a tiny shell-side DEX using the installed SDK; it is not part of the phone app classes.
val helperClasses = layout.buildDirectory.dir("tvHelper/classes")
val helperAssets = layout.buildDirectory.dir("generated/tvHelperAssets")
val helperBootClasspath = androidComponents.sdkComponents.bootClasspath
val compileTvHelper by tasks.registering(JavaCompile::class) {
    source(fileTree("src/tvHelper/java") { include("**/*.java") })
    classpath = files(helperBootClasspath)
    destinationDirectory.set(helperClasses)
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}
val dexTvHelper by tasks.registering(JavaExec::class) {
    dependsOn(compileTvHelper)
    classpath = files(androidComponents.sdkComponents.sdkDirectory.map {
        it.file("build-tools/37.0.0/lib/d8.jar")
    })
    mainClass.set("com.android.tools.r8.D8")
    inputs.dir(helperClasses)
    outputs.dir(helperAssets)
    doFirst {
        val output = helperAssets.get().asFile
        output.mkdirs()
        args = listOf("--min-api", "24", "--output", output.absolutePath) +
            helperBootClasspath.get().flatMap { listOf("--lib", it.asFile.absolutePath) } +
            fileTree(helperClasses).matching { include("**/*.class") }.files.map { it.absolutePath }
    }
}
android.sourceSets.getByName("main").assets.srcDir(helperAssets.get().asFile)
tasks.named("preBuild") { dependsOn(dexTvHelper) }
