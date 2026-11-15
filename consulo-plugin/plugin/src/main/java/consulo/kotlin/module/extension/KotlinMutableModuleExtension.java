package consulo.kotlin.module.extension;

import consulo.disposer.Disposable;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

public class KotlinMutableModuleExtension extends KotlinModuleExtension implements MutableModuleExtension<KotlinModuleExtension> {
    public KotlinMutableModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    @RequiredUIAccess
    @Override
    public Component createConfigurationComponent(Disposable disposable, Runnable updateOnCheck) {
        return null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        myIsEnabled = enabled;
    }

    @Override
    public boolean isModified(KotlinModuleExtension originalExtension) {
        return myIsEnabled != originalExtension.isEnabled();
    }
}
