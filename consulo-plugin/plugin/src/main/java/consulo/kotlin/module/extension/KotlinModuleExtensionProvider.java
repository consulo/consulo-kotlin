package consulo.kotlin.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.kotlin.icon.KotlinIconGroup;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;

@ExtensionImpl
public class KotlinModuleExtensionProvider implements ModuleExtensionProvider<KotlinModuleExtension> {
    @Override
    public String getId() {
        return "kotlin";
    }

    @Override
    public String getParentId() {
        return "java";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Kotlin");
    }

    @Override
    public Image getIcon() {
        return KotlinIconGroup.kotlin();
    }

    @Override
    public ModuleExtension<KotlinModuleExtension> createImmutableExtension(ModuleRootLayer moduleRootLayer) {
        return new KotlinModuleExtension(getId(), moduleRootLayer);
    }

    @Override
    public MutableModuleExtension<KotlinModuleExtension> createMutableExtension(ModuleRootLayer moduleRootLayer) {
        return new KotlinMutableModuleExtension(getId(), moduleRootLayer);
    }
}
