package consulo.kotlin.analysis;

import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleBase;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule;
import org.jetbrains.kotlin.platform.TargetPlatform;
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms;

import java.util.List;

/** A Kotlin file outside of any module content. */
final class ConsuloKaNotUnderContentRootModule extends KaModuleBase implements KaNotUnderContentRootModule {
    private final Project myProject;
    private final PsiFile myFile;

    ConsuloKaNotUnderContentRootModule(Project project, PsiFile file) {
        myProject = project;
        myFile = file;
    }

    @Override
    public String getName() {
        return myFile != null ? myFile.getName() : "Not under content root";
    }

    @Override
    public PsiFile getFile() {
        return myFile;
    }

    @Override
    public String getModuleDescription() {
        return "Not under content root " + getName();
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
        return myFile != null ? GlobalSearchScope.fileScope(myFile) : GlobalSearchScope.EMPTY_SCOPE;
    }

    @Override
    public TargetPlatform getTargetPlatform() {
        return JvmPlatforms.INSTANCE.getDefaultJvmPlatform();
    }

    @Override
    public Project getProject() {
        return myProject;
    }
}
