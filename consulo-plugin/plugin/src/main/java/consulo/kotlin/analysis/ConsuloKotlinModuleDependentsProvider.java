package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProviderBase;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule;

import java.util.HashSet;
import java.util.Set;

@Singleton
@ServiceImpl
public class ConsuloKotlinModuleDependentsProvider extends KotlinModuleDependentsProviderBase {
    private final Project myProject;

    @Inject
    public ConsuloKotlinModuleDependentsProvider(Project project) {
        myProject = project;
    }

    @Override
    public Set<KaModule> getDirectDependents(KaModule module) {
        Set<KaModule> result = new HashSet<>();
        for (ConsuloKaSourceModule sourceModule : ConsuloKotlinProjectStructureProvider.getInstance(myProject).getAllSourceModules()) {
            if (sourceModule.getDirectRegularDependencies().contains(module)) {
                result.add(sourceModule);
            }
        }
        return result;
    }

    @Override
    public Set<KaModule> getRefinementDependents(KaModule module) {
        return Set.of();
    }
}
