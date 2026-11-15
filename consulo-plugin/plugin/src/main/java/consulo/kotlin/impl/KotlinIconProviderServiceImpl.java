package consulo.kotlin.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.idea.KotlinIconProviderService;

/**
 * Consulo counterpart of upstream's KotlinIdeFileIconProviderService (registered in plugin.xml there).
 * TODO: Kotlin icon group instead of the generic text file icon.
 */
@Singleton
@ServiceImpl
public class KotlinIconProviderServiceImpl extends KotlinIconProviderService {
    @Override
    public Image getFileIcon() {
        return PlatformIconGroup.filetypesText();
    }

    @Override
    public Image getBuiltInFileIcon() {
        return PlatformIconGroup.filetypesText();
    }
}
