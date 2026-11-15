/**
 * Consulo-native part of the Kotlin plugin. The Kotlin compiler and IDE code come remapped
 * in consulo.kotlin.remapped (compiler) and consulo.kotlin.plugin.
 */
open module consulo.kotlin {
    exports consulo.kotlin.icon;
    exports consulo.kotlin.module.extension;

    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.localize.api;
    requires consulo.kotlin.core.helper;
    requires consulo.kotlin.remapped;
    requires consulo.virtual.file.system.api;
    requires consulo.language.api;
    requires consulo.java.language.api;
    requires consulo.ui.api;
    requires consulo.module.api;
    requires consulo.module.content.api;
    requires consulo.disposer.api;
}
