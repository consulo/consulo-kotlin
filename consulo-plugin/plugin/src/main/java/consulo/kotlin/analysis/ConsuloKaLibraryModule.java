package consulo.kotlin.analysis;

import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleBase;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule;
import org.jetbrains.kotlin.platform.TargetPlatform;
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** A library or an SDK of the project: its class roots, analyzed from binaries. */
public final class ConsuloKaLibraryModule extends KaModuleBase implements KaLibraryModule {
    private final Project myProject;
    private final String myName;
    private final List<VirtualFile> myRoots;
    private final boolean mySdk;

    ConsuloKaLibraryModule(Project project, String name, List<VirtualFile> roots, boolean sdk) {
        myProject = project;
        myName = name;
        myRoots = roots;
        mySdk = sdk;
    }

    @Override
    public String getLibraryName() {
        return myName;
    }

    @Override
    public Collection<Path> getBinaryRoots() {
        List<Path> paths = new ArrayList<>();
        for (VirtualFile root : myRoots) {
            VirtualFile local = ArchiveVfsUtil.getVirtualFileForArchive(root);
            paths.add(Path.of((local != null ? local : root).getPath()));
        }
        return paths;
    }

    @Override
    public Collection<VirtualFile> getBinaryVirtualFiles() {
        return myRoots;
    }

    @Override
    public KaLibrarySourceModule getLibrarySources() {
        return null;
    }

    @Override
    public boolean isSdk() {
        return mySdk;
    }

    @Override
    public List<KaModule> getDirectRegularDependencies() {
        return List.of();
    }

    @Override
    public List<KaModule> getDirectDependsOnDependencies() {
        return List.of();
    }

    @Override
    public List<KaModule> getDirectFriendDependencies() {
        return List.of();
    }

    @Override
    public GlobalSearchScope getBaseContentScope() {
        return myRoots.isEmpty()
            ? GlobalSearchScope.EMPTY_SCOPE
            : GlobalSearchScopesCore.directoriesScope(myProject, true, myRoots.toArray(VirtualFile[]::new));
    }

    @Override
    public TargetPlatform getTargetPlatform() {
        return JvmPlatforms.INSTANCE.getDefaultJvmPlatform();
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConsuloKaLibraryModule other && other.mySdk == mySdk && other.myName.equals(myName) && other.myRoots.equals(myRoots);
    }

    @Override
    public int hashCode() {
        return myName.hashCode() * 31 + myRoots.hashCode();
    }

    @Override
    public String toString() {
        return (mySdk ? "SDK " : "Library ") + myName;
    }
}
