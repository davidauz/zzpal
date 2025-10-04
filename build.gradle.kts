// Top-level build file where you can add configuration options common to all sub-projects/modules.
//Root-level build.gradle.kts
//Location: project-root/build.gradle.kts
//Purpose: Configuration that applies to the entire project (all modules)
//Buildscript dependencies (Kotlin plugin, Android plugin, etc.)
//Repository definitions
//Project-wide properties
plugins {
    alias(libs.plugins.android.application) apply false // Root file: Declares plugins but
    alias(libs.plugins.kotlin.android) apply false // doesn't apply them (apply false)
}