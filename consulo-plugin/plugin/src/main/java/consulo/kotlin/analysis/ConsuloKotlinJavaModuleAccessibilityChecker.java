package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityChecker;
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityError;
import org.jetbrains.kotlin.name.FqName;

/** Java module (JPMS) accessibility of referenced declarations: not checked yet, everything is accessible. */
@Singleton
@ServiceImpl
public class ConsuloKotlinJavaModuleAccessibilityChecker implements KotlinJavaModuleAccessibilityChecker {
    @Override
    public KotlinJavaModuleAccessibilityError checkAccessibility(VirtualFile useSiteFile, VirtualFile referencedFile, FqName referencedPackage) {
        return null;
    }
}
