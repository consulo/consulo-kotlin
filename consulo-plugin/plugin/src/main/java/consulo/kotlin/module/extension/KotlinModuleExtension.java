package consulo.kotlin.module.extension;

import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionBase;
import consulo.module.extension.ModuleExtension;

/**
 * Marks a module as a Kotlin module. Kotlin/JVM modules build on the Java module extension (its SDK and language level).
 */
public class KotlinModuleExtension extends ModuleExtensionBase<KotlinModuleExtension> implements ModuleExtension<KotlinModuleExtension> {
    public KotlinModuleExtension(String id, ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }
}
