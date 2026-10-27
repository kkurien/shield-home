pluginManagement {
    repositories {
        maven { url = uri("file:///opt/m2local") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        maven { url = uri("file:///opt/m2local") }
        google()
        mavenCentral()
    }
}
rootProject.name = "ShieldHome"
include(":app")
