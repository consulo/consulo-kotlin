package consulo.kotlin.analysis;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.stub.IndexableSetContributor;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Set;

/**
 * The stdlib bundled with the plugin is not a project root but backs Kotlin modules without a stdlib of their own
 * (see {@link ConsuloKotlinProjectStructureProvider}), so the Kotlin indexes have to see its classes.
 */
@ExtensionImpl
public class KotlinBundledStdlibIndexableSetContributor extends IndexableSetContributor {
    @Override
    public Set<VirtualFile> getAdditionalRootsToIndex() {
        VirtualFile root = KotlinBundledStdlib.root();
        return root == null ? Set.of() : Set.of(root);
    }
}
