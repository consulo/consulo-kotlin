package consulo.kotlin.analysis;

import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider;
import org.jetbrains.kotlin.name.CallableId;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;

import java.util.*;

/**
 * Names of library declarations for a scope: the top-level callables of the library roots in it (see
 * {@link LibraryCallableNames}). Libraries are analyzed from class files, so there is no PSI to return.
 */
final class LibraryDeclarationProvider implements KotlinDeclarationProvider {
    private final List<VirtualFile> myRoots;

    LibraryDeclarationProvider(List<VirtualFile> roots) {
        myRoots = roots;
    }

    @Override
    public Set<Name> getTopLevelCallableNamesInPackage(FqName packageFqName) {
        Set<Name> names = new HashSet<>();
        for (VirtualFile root : myRoots) {
            names.addAll(LibraryCallableNames.get(root, packageFqName));
        }
        return names;
    }

    @Override
    public Set<Name> getTopLevelKotlinClassLikeDeclarationNamesInPackage(FqName packageFqName) {
        return Set.of();
    }

    @Override
    public KtClassLikeDeclaration getClassLikeDeclarationByClassId(ClassId classId) {
        return null;
    }

    @Override
    public Collection<KtClassOrObject> getAllClassesByClassId(ClassId classId) {
        return List.of();
    }

    @Override
    public Collection<KtTypeAlias> getAllTypeAliasesByClassId(ClassId classId) {
        return List.of();
    }

    @Override
    public Collection<KtProperty> getTopLevelProperties(CallableId callableId) {
        return List.of();
    }

    @Override
    public Collection<KtNamedFunction> getTopLevelFunctions(CallableId callableId) {
        return List.of();
    }

    @Override
    public Collection<KtFile> getTopLevelCallableFiles(CallableId callableId) {
        return List.of();
    }

    @Override
    public Collection<KtFile> findFilesForFacadeByPackage(FqName packageFqName) {
        return List.of();
    }

    @Override
    public Collection<KtFile> findFilesForFacade(FqName facadeFqName) {
        return List.of();
    }

    @Override
    public Collection<KtFile> findInternalFilesForFacade(FqName facadeFqName) {
        return List.of();
    }

    @Override
    public Collection<KtScript> findFilesForScript(FqName scriptFqName) {
        return List.of();
    }

    @Override
    public boolean getHasSpecificClassifierPackageNamesComputation() {
        return false;
    }

    @Override
    public boolean getHasSpecificCallablePackageNamesComputation() {
        return false;
    }
}
