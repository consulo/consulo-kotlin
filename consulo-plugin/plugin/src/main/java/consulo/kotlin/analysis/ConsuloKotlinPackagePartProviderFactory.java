package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kotlin.Unit;
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory;
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartProviderBase;
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider;
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion;
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping;
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Package parts of the libraries in a scope, from their META-INF/*.kotlin_module mappings. */
@Singleton
@ServiceImpl
public class ConsuloKotlinPackagePartProviderFactory implements KotlinPackagePartProviderFactory {
    private final Project myProject;

    @Inject
    public ConsuloKotlinPackagePartProviderFactory(Project project) {
        myProject = project;
    }

    @Override
    public PackagePartProvider createPackagePartProvider(GlobalSearchScope scope) {
        List<VirtualFile> roots = new ArrayList<>();
        for (ConsuloKaLibraryModule library : ConsuloKotlinProjectStructureProvider.getInstance(myProject).getAllLibraryModules()) {
            for (VirtualFile root : library.getBinaryVirtualFiles()) {
                VirtualFile metaInf = root.findChild("META-INF");
                if (metaInf != null && (scope.contains(root) || scope.contains(metaInf))) {
                    roots.add(root);
                }
            }
        }
        return new LibraryPackagePartProvider(roots);
    }

    static ModuleMapping load(VirtualFile file) {
        try {
            return ModuleMapping.Companion.loadModuleMapping(file.contentsToByteArray(), file.getPath(), false, true,
                MetadataVersion.INSTANCE, version -> Unit.INSTANCE);
        }
        catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static final class LibraryPackagePartProvider extends JvmPackagePartProviderBase<VirtualFile> {
        private final List<ModuleMappingInfo<VirtualFile>> myModules = new ArrayList<>();

        LibraryPackagePartProvider(List<VirtualFile> roots) {
            for (VirtualFile root : roots) {
                VirtualFile metaInf = root.findChild("META-INF");
                for (VirtualFile file : metaInf.getChildren()) {
                    if (!file.getName().endsWith(".kotlin_module")) {
                        continue;
                    }
                    ModuleMapping mapping = load(file);
                    if (mapping != null) {
                        myModules.add(new ModuleMappingInfo<>(root, mapping, file.getNameWithoutExtension()));
                    }
                }
            }
        }

        @Override
        protected List<ModuleMappingInfo<VirtualFile>> getLoadedModules() {
            return myModules;
        }

        @Override
        public DeserializationConfiguration getDeserializationConfiguration() {
            return DeserializationConfiguration.Default.INSTANCE;
        }
    }
}
