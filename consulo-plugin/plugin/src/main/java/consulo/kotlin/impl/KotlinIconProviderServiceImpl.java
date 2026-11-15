package consulo.kotlin.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.kotlin.icon.KotlinIconGroup;
import consulo.ui.image.Image;
import jakarta.inject.Singleton;
import consulo.kotlin.compiler.idea.KotlinIconProviderService;

/**
 * Consulo counterpart of upstream's KotlinIdeFileIconProviderService (registered in plugin.xml there).
 */
@Singleton
@ServiceImpl
public class KotlinIconProviderServiceImpl extends KotlinIconProviderService {
    @Override
    public Image getFileIcon() {
        return KotlinIconGroup.filetypesKotlin();
    }

    @Override
    public Image getBuiltInFileIcon() {
        return KotlinIconGroup.filetypesKotlin();
    }
}
