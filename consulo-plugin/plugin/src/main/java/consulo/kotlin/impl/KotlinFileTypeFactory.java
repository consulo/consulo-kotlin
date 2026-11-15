package consulo.kotlin.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import org.jetbrains.kotlin.idea.KotlinFileType;

/**
 * Registers KotlinFileType (from the remapped compiler) for .kt and .kts files.
 */
@ExtensionImpl
public class KotlinFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(FileTypeConsumer consumer) {
        consumer.consume(KotlinFileType.INSTANCE, KotlinFileType.EXTENSION + ";" + KotlinFileType.SCRIPT_EXTENSION);
    }
}
