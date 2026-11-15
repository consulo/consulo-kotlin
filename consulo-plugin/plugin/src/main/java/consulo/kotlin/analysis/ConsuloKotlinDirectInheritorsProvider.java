package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.scope.GlobalSearchScope;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDirectInheritorsProvider;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;

import java.util.List;

/** Direct inheritors (sealed class hierarchies). TODO: an inheritors index. */
@Singleton
@ServiceImpl
public class ConsuloKotlinDirectInheritorsProvider implements KotlinDirectInheritorsProvider {
    @Override
    public Iterable<KtClassOrObject> getDirectKotlinInheritors(KtClass ktClass, GlobalSearchScope scope, boolean includeLocalInheritors) {
        return List.of();
    }
}
