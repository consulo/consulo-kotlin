package consulo.kotlin.analysis;

import consulo.logging.Logger;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import kotlin.Unit;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/** The Kotlin stdlib bundled with the plugin: the fallback built-ins, and the stdlib of Kotlin modules that have none. */
public final class KotlinBundledStdlib {
    private static final Logger LOG = Logger.getInstance(KotlinBundledStdlib.class);

    private KotlinBundledStdlib() {
    }

    /** The root of the stdlib jar in the JAR file system, or null. */
    public static VirtualFile root() {
        File jar = jar();
        if (jar == null) {
            return null;
        }
        String url = StandardFileSystems.JAR_PROTOCOL_PREFIX + jar.getAbsolutePath().replace(File.separatorChar, '/') + StandardFileSystems.JAR_SEPARATOR;
        return VirtualFileManager.getInstance().findFileByUrl(url);
    }

    /** Whether a library root is a Kotlin stdlib. */
    public static boolean isStdlib(VirtualFile root) {
        return root.findFileByRelativePath("kotlin/Unit.class") != null;
    }

    private static File jar() {
        Module module = Unit.class.getModule();
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
            LOG.warn("Kotlin stdlib jar not found", e);
            return null;
        }
    }
}
