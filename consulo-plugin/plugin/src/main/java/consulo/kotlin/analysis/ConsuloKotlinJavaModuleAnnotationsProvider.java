package consulo.kotlin.analysis;

import com.intellij.java.language.psi.PsiAnnotation;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModulePsiAnnotationsProvider;
import org.jetbrains.kotlin.name.ClassId;

import java.util.List;

/** Annotations of Java modules (module-info.java), for nullability defaults: none yet. */
@Singleton
@ServiceImpl
public class ConsuloKotlinJavaModuleAnnotationsProvider implements KotlinJavaModulePsiAnnotationsProvider {
    @Override
    public List<PsiAnnotation> getAnnotationsForModuleOwnerOfClass(ClassId classId) {
        return null;
    }
}
