package consulo.kotlin.analysis;

import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.project.Project;
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleBase;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl;
import org.jetbrains.kotlin.platform.TargetPlatform;
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms;

import java.util.List;

/** The sources of a Consulo module, as an Analysis API module. */
public final class ConsuloKaSourceModule extends KaModuleBase implements KaSourceModule {
    private final Module myModule;
    private final ConsuloKotlinProjectStructureProvider myProvider;

    ConsuloKaSourceModule(Module module, ConsuloKotlinProjectStructureProvider provider) {
        myModule = module;
        myProvider = provider;
    }

    public Module getModule() {
        return myModule;
    }

    @Override
    public String getName() {
        return myModule.getName();
    }

    @Override
    public LanguageVersionSettings getLanguageVersionSettings() {
        return LanguageVersionSettingsImpl.DEFAULT;
    }

    @Override
    public List<KaModule> getDirectRegularDependencies() {
        return myProvider.getDependencies(myModule);
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
        return GlobalSearchScope.moduleScope(myModule);
    }

    @Override
    public TargetPlatform getTargetPlatform() {
        return JvmPlatforms.INSTANCE.getDefaultJvmPlatform();
    }

    @Override
    public Project getProject() {
        return myModule.getProject();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConsuloKaSourceModule other && other.myModule.equals(myModule);
    }

    @Override
    public int hashCode() {
        return myModule.hashCode();
    }

    @Override
    public String toString() {
        return "Source module " + myModule.getName();
    }
}
