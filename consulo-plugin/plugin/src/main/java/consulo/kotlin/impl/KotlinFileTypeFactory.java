package consulo.kotlin.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.kotlin.compiler.idea.KotlinFileType;
import consulo.kotlin.compiler.idea.KotlinModuleFileType;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType;

/**
 * Registers the Kotlin file types of the remapped compiler: sources (.kt, .kts), and the binary files the indexes of
 * libraries read (.kotlin_module package mappings, .kotlin_builtins).
 */
@ExtensionImpl
public class KotlinFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(FileTypeConsumer consumer) {
        consumer.consume(KotlinFileType.INSTANCE, KotlinFileType.EXTENSION + ";" + KotlinFileType.SCRIPT_EXTENSION);
        consumer.consume(KotlinModuleFileType.INSTANCE, KotlinModuleFileType.EXTENSION);
        consumer.consume(KotlinBuiltInFileType.INSTANCE, KotlinBuiltInFileType.INSTANCE.getDefaultExtension());
    }
}
