package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.content.DirectoryIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Finds the class files of library classes for the binary symbol providers. IntelliJ uses Kotlin file indexes for this;
 * here the class file is looked up in the package directories ({@link DirectoryIndex}) inside the search scope.
 * Kotlin metadata files (.kotlin_metadata of common code) are not looked up yet.
 */
@Singleton
@ServiceImpl
public class ConsuloVirtualFileFinderFactory implements VirtualFileFinderFactory {
    private final Project myProject;

    @Inject
    public ConsuloVirtualFileFinderFactory(Project project) {
        myProject = project;
    }

    @Override
    public VirtualFileFinder create(GlobalSearchScope scope) {
        return new Finder(myProject, scope);
    }

    @Override
    public VirtualFileFinder create(Project project) {
        return create(GlobalSearchScope.allScope(project));
    }

    private static final class Finder extends VirtualFileFinder {
        private final Project myProject;
        private final GlobalSearchScope myScope;

        Finder(Project project, GlobalSearchScope scope) {
            myProject = project;
            myScope = scope;
        }

        @Override
        public VirtualFile findVirtualFileWithHeader(ClassId classId) {
            if (classId.isLocal()) {
                return null;
            }
            // nested classes are separate class files: Outer$Inner.class
            String fileName = classId.getRelativeClassName().asString().replace('.', '$') + ".class";
            return findInPackage(classId.getPackageFqName(), fileName);
        }

        @Override
        public VirtualFile findSourceOrBinaryVirtualFile(ClassId classId) {
            return findVirtualFileWithHeader(classId);
        }

        @Override
        public InputStream findMetadata(ClassId classId) {
            return null;
        }

        @Override
        public Set<String> findMetadataTopLevelClassesInPackage(FqName packageFqName) {
            return null;
        }

        @Override
        public boolean hasMetadataPackage(FqName fqName) {
            return false;
        }

        @Override
        public InputStream findBuiltInsData(FqName packageFqName) {
            VirtualFile file = findInPackage(packageFqName, BuiltInSerializerProtocol.INSTANCE.getBuiltInsFileName(packageFqName));
            try {
                return file == null ? null : file.getInputStream();
            }
            catch (IOException e) {
                return null;
            }
        }

        private VirtualFile findInPackage(FqName packageFqName, String fileName) {
            VirtualFile[] found = {null};
            DirectoryIndex.getInstance(myProject).getDirectoriesByPackageName(packageFqName.asString(), false).forEach(directory -> {
                VirtualFile child = directory.findChild(fileName);
                if (child != null && myScope.contains(child)) {
                    found[0] = child;
                    return false;
                }
                return true;
            });
            return found[0];
        }
    }
}
