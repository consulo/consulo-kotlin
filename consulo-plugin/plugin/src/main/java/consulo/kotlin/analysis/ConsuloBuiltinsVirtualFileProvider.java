package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.scope.DelegatingGlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Singleton;
import kotlin.Unit;
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.StandardClassIds;
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
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
        File jar = stdlibJar();
        if (jar == null) {
            LOG.warn("Kotlin stdlib jar not found, no fallback built-ins");
            return files;
        }
        String root = StandardFileSystems.JAR_PROTOCOL_PREFIX + jar.getAbsolutePath().replace(File.separatorChar, '/') + StandardFileSystems.JAR_SEPARATOR;
        for (FqName packageFqName : StandardClassIds.INSTANCE.getBuiltInsPackages()) {
            String path = BuiltInSerializerProtocol.INSTANCE.getBuiltInsFilePath(packageFqName);
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(root + path);
            if (file != null) {
                files.add(file);
            }
            else {
                LOG.warn("VirtualFile not found for builtin " + root + path);
            }
        }
        return files;
    }

    private static File stdlibJar() {
        java.lang.Module module = Unit.class.getModule();
        ModuleLayer layer = module.getLayer();
        if (layer != null) {
            Optional<URI> location = layer.configuration().findModule(module.getName()).flatMap(m -> m.reference().location());
            if (location.isPresent() && "file".equals(location.get().getScheme())) {
                return Path.of(location.get()).toFile();
            }
        }
        try {
            return Path.of(Unit.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
        }
        catch (Exception e) {
            LOG.warn(e);
            return null;
        }
    }
}
