/**
 * Consulo-native part of the Kotlin plugin. The Kotlin compiler and IDE code come remapped
 * in consulo.kotlin.remapped and consulo.kotlin.plugin.
 */
open module consulo.kotlin {
    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.localize.api;
    requires consulo.kotlin.core.helper;
}
