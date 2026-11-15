package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.scope.DelegatingGlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.StandardClassIds;
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol;

import java.util.LinkedHashSet;
import java.util.Set;

/** The fallback built-ins: the .kotlin_builtins files of the stdlib bundled with the plugin. */
@Singleton
@ServiceImpl
public class ConsuloBuiltinsVirtualFileProvider extends BuiltinsVirtualFileProvider {
    private static final Logger LOG = Logger.getInstance(ConsuloBuiltinsVirtualFileProvider.class);

    private volatile Set<VirtualFile> myFiles;

    @Override
    public Set<VirtualFile> getBuiltinVirtualFiles() {
        Set<VirtualFile> files = myFiles;
        if (files == null) {
            myFiles = files = findBuiltinFiles();
        }
        return files;
    }

    @Override
    public GlobalSearchScope createBuiltinsScope(Project project) {
        return new DelegatingGlobalSearchScope(GlobalSearchScope.filesScope(project, getBuiltinVirtualFiles())) {
            @Override
            public boolean isSearchInModuleContent(Module aModule) {
                return false;
            }

            @Override
            public boolean isSearchInLibraries() {
                return true;
            }

            @Override
            public String getDisplayName() {
                return "Kotlin builtin files scope";
            }
        };
    }

    private static Set<VirtualFile> findBuiltinFiles() {
        Set<VirtualFile> files = new LinkedHashSet<>();
        VirtualFile root = KotlinBundledStdlib.root();
        if (root == null) {
            LOG.warn("Kotlin stdlib jar not found, no fallback built-ins");
            return files;
        }
        for (FqName packageFqName : StandardClassIds.INSTANCE.getBuiltInsPackages()) {
            String path = BuiltInSerializerProtocol.INSTANCE.getBuiltInsFilePath(packageFqName);
            VirtualFile file = root.findFileByRelativePath(path);
            if (file != null) {
                files.add(file);
            }
            else {
                LOG.warn("VirtualFile not found for builtin " + root.getUrl() + path);
            }
        }
        return files;
    }
}
