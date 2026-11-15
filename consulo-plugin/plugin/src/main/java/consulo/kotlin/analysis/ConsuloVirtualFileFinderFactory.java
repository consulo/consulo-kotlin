package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.idea.vfilefinder.IdeVirtualFileFinder;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory;

/**
 * Creates upstream's {@link IdeVirtualFileFinder} (Kotlin class file index). Upstream's own factory also overrides
 * {@code create(Project, ModuleInfo)}, which the compiler fork's {@link VirtualFileFinderFactory} does not declare.
 */
@Singleton
@ServiceImpl
public class ConsuloVirtualFileFinderFactory implements VirtualFileFinderFactory {
    private final Project myProject;

    @Inject
    public ConsuloVirtualFileFinderFactory(Project project) {
        myProject = project;
    }

    @Override
    public VirtualFileFinder create(GlobalSearchScope scope) {
        return new IdeVirtualFileFinder(scope, myProject);
    }

    @Override
    public VirtualFileFinder create(Project project) {
        return create(GlobalSearchScope.allScope(project));
    }
}
