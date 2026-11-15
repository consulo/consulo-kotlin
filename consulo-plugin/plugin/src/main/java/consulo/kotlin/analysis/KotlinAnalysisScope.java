package consulo.kotlin.analysis;

import consulo.kotlin.module.extension.KotlinModuleExtension;
import consulo.language.psi.PsiElement;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;

/** Where semantic Kotlin features (analysis, completion) run: modules with the Kotlin extension, and files outside modules. */
public final class KotlinAnalysisScope {
    private KotlinAnalysisScope() {
    }

    public static boolean isEnabled(PsiElement element) {
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        return module == null || ModuleUtilCore.getExtension(module, KotlinModuleExtension.class) != null;
    }
}
