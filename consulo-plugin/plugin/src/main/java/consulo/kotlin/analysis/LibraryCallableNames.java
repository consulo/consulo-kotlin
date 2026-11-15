package consulo.kotlin.analysis;

import consulo.virtualFileSystem.VirtualFile;
import kotlin.Pair;
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache;
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil;
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping;
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Top-level callable names of the Kotlin packages of a library root, read from its .kotlin_module package parts and their
 * class metadata. Analysis lists the declarations of a package (e.g. for star imports in completion) through these names;
 * IntelliJ keeps them in file indexes. Cached per root and package, refreshed when the root's jar changes.
 */
final class LibraryCallableNames {
    private record RootKey(String url, long stamp) {
    }

    private static final Map<RootKey, Map<String, Set<Name>>> ourCache = new ConcurrentHashMap<>();

    private LibraryCallableNames() {
    }

    static Set<Name> get(VirtualFile root, FqName packageFqName) {
        Map<String, Set<Name>> byPackage = ourCache.computeIfAbsent(new RootKey(root.getUrl(), root.getModificationStamp()), k -> new ConcurrentHashMap<>());
        return byPackage.computeIfAbsent(packageFqName.asString(), p -> compute(root, p));
    }

    private static Set<Name> compute(VirtualFile root, String packageFqName) {
        VirtualFile metaInf = root.findChild("META-INF");
        if (metaInf == null) {
            return Set.of();
        }
        Set<Name> names = new HashSet<>();
        for (VirtualFile file : metaInf.getChildren()) {
            if (!file.getName().endsWith(".kotlin_module")) {
                continue;
            }
            ModuleMapping mapping = ConsuloKotlinPackagePartProviderFactory.load(file);
            PackageParts parts = mapping == null ? null : mapping.findPackageParts(packageFqName);
            if (parts == null) {
                continue;
            }
            for (String part : parts.getParts()) {
                VirtualFile classFile = root.findFileByRelativePath(part + ".class");
                if (classFile != null) {
                    addNames(classFile, names);
                }
            }
        }
        return names;
    }

    private static void addNames(VirtualFile classFile, Set<Name> names) {
        KotlinClassFinder.Result result = KotlinBinaryClassCache.Companion.getKotlinBinaryClassOrClassFileContent(
            classFile, MetadataVersion.INSTANCE, null, null);
        if (!(result instanceof KotlinClassFinder.Result.KotlinClass kotlinClass)) {
            return;
        }
        KotlinClassHeader header = kotlinClass.getKotlinJvmBinaryClass().getClassHeader();
        String[] data = header.getData();
        String[] strings = header.getStrings();
        if (data == null || strings == null) {
            return;
        }
        Pair<JvmNameResolver, ProtoBuf.Package> packageData = JvmProtoBufUtil.readPackageDataFrom(data, strings);
        JvmNameResolver nameResolver = packageData.getFirst();
        for (ProtoBuf.Function function : packageData.getSecond().getFunctionList()) {
            names.add(Name.guessByFirstCharacter(nameResolver.getString(function.getName())));
        }
        for (ProtoBuf.Property property : packageData.getSecond().getPropertyList()) {
            names.add(Name.guessByFirstCharacter(nameResolver.getString(property.getName())));
        }
    }
}
