package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinFileBasedDeclarationProvider;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Kotlin source declarations of a scope, read from the PSI of its Kotlin files (TODO: stub indexes), and the names of the
 * library declarations in it.
 */
@Singleton
@ServiceImpl
public class ConsuloKotlinDeclarationProviderFactory implements KotlinDeclarationProviderFactory {
    private final Project myProject;

    @Inject
    public ConsuloKotlinDeclarationProviderFactory(Project project) {
        myProject = project;
    }

    @Override
    public KotlinDeclarationProvider createDeclarationProvider(GlobalSearchScope scope, KaModule contextualModule) {
        List<KotlinDeclarationProvider> providers = new ArrayList<>();
        for (KtFile file : KotlinFilesInScope.find(myProject, scope)) {
            providers.add(new KotlinFileBasedDeclarationProvider(file));
        }
        List<VirtualFile> libraryRoots = new ArrayList<>();
        for (ConsuloKaLibraryModule library : ConsuloKotlinProjectStructureProvider.getInstance(myProject).getAllLibraryModules()) {
            for (VirtualFile root : library.getBinaryVirtualFiles()) {
                if (root.findChild("META-INF") != null && scope.contains(root)) {
                    libraryRoots.add(root);
                }
            }
        }
        if (!libraryRoots.isEmpty()) {
            providers.add(new LibraryDeclarationProvider(libraryRoots));
        }
        return KotlinCompositeDeclarationProvider.Companion.create(providers);
    }
}
