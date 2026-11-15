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
    requires consulo.kotlin.plugin;
    requires consulo.virtual.file.system.api;
    requires consulo.language.api;
    requires consulo.java.language.api;
    requires consulo.ui.api;
    requires consulo.module.api;
    requires consulo.module.content.api;
    requires consulo.disposer.api;
    requires consulo.project.content.api;
    requires consulo.application.content.api;
    requires kotlin.stdlib;
    requires consulo.component.api;
    requires consulo.document.api;
    requires consulo.logging.api;
    requires consulo.language.editor.api;
    requires consulo.language.impl;
}
