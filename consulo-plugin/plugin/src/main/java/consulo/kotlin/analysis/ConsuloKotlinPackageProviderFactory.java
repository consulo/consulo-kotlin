package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider;
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderBase;
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Kotlin packages of a scope: the packages of its Kotlin files and their parents. */
@Singleton
@ServiceImpl
public class ConsuloKotlinPackageProviderFactory implements KotlinPackageProviderFactory {
    private final Project myProject;

    @Inject
    public ConsuloKotlinPackageProviderFactory(Project project) {
        myProject = project;
    }

    @Override
    public KotlinPackageProvider createPackageProvider(GlobalSearchScope searchScope) {
        return new FilePackageProvider(myProject, searchScope);
    }

    private static final class FilePackageProvider extends KotlinPackageProviderBase {
        private volatile Map<FqName, Set<Name>> mySubpackages;

        FilePackageProvider(Project project, GlobalSearchScope scope) {
            super(project, scope);
        }

        private Map<FqName, Set<Name>> subpackages() {
            Map<FqName, Set<Name>> result = mySubpackages;
            if (result == null) {
                result = new HashMap<>();
                result.computeIfAbsent(FqName.ROOT, k -> new HashSet<>());
                for (KtFile file : KotlinFilesInScope.find(getProject(), getSearchScope())) {
                    FqName fqName = file.getPackageFqName();
                    result.computeIfAbsent(fqName, k -> new HashSet<>());
                    while (!fqName.isRoot()) {
                        FqName parent = fqName.parent();
                        result.computeIfAbsent(parent, k -> new HashSet<>()).add(fqName.shortName());
                        fqName = parent;
                    }
                }
                mySubpackages = result;
            }
            return result;
        }

        @Override
        public boolean doesKotlinOnlyPackageExist(FqName packageFqName) {
            return subpackages().containsKey(packageFqName);
        }

        @Override
        public Set<Name> getKotlinOnlySubpackageNames(FqName packageFqName) {
            return subpackages().getOrDefault(packageFqName, Set.of());
        }
    }
}
