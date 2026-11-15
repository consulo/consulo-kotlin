package consulo.kotlin.impl;

import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFileClassProvider;

/**
 * Upstream registers KtFileClassProviderImpl (light classes) as a project service.
 * TODO: light classes; until then Kotlin files expose no Java classes.
 */
@Singleton
@ServiceImpl
public class KtFileClassProviderImpl implements KtFileClassProvider {
    @Override
    public PsiClass[] getFileClasses(KtFile file) {
        return PsiClass.EMPTY_ARRAY;
    }
}
