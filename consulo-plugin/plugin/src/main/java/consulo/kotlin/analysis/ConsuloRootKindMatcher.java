package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.idea.base.projectStructure.RootKindFilter;
import org.jetbrains.kotlin.idea.base.projectStructure.RootKindMatcher;

/**
 * Which kind of root a file is under (project sources, library classes or library sources), for the search scopes of
 * the upstream plugin. IntelliJ's matcher also places Kotlin scripts, which Consulo does not support.
 */
@Singleton
@ServiceImpl
public class ConsuloRootKindMatcher implements RootKindMatcher {
    private final Project myProject;

    @Inject
    public ConsuloRootKindMatcher(Project project) {
        myProject = project;
    }

    @Override
    public boolean matches(RootKindFilter filter, VirtualFile virtualFile) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(myProject);
        if (fileIndex.isInSourceContent(virtualFile)) {
            return filter.getIncludeProjectSourceFiles();
        }
        VirtualFile bundledStdlib = KotlinBundledStdlib.root();
        boolean libraryClass = fileIndex.isInLibraryClasses(virtualFile)
            || bundledStdlib != null && VirtualFileUtil.isAncestor(bundledStdlib, virtualFile, false);
        if (libraryClass && filter.getIncludeLibraryClassFiles()) {
            return true;
        }
        return filter.getIncludeLibrarySourceFiles() && fileIndex.isInLibrarySource(virtualFile);
    }
}
