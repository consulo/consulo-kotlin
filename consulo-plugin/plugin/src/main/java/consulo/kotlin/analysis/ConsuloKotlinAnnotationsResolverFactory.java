package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.scope.GlobalSearchScope;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolver;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.psi.KtAnnotated;

import java.util.Set;

/** Annotation-based declaration search (used by compiler plugins): none yet. */
@Singleton
@ServiceImpl
public class ConsuloKotlinAnnotationsResolverFactory implements KotlinAnnotationsResolverFactory {
    @Override
    public KotlinAnnotationsResolver createAnnotationResolver(GlobalSearchScope searchScope) {
        return new KotlinAnnotationsResolver() {
            @Override
            public Set<KtAnnotated> declarationsByAnnotation(ClassId annotationClassId) {
                return Set.of();
            }

            @Override
            public Set<ClassId> annotationsOnDeclaration(KtAnnotated declaration) {
                return Set.of();
            }
        };
    }
}
