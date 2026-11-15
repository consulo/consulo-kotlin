package consulo.kotlin.analysis;

import consulo.kotlin.compiler.idea.KotlinFileType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FileTypeIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.ArrayList;
import java.util.List;

/** The Kotlin source files of a search scope, from the file type index. */
final class KotlinFilesInScope {
    private KotlinFilesInScope() {
    }

    static List<KtFile> find(Project project, GlobalSearchScope scope) {
        PsiManager psiManager = PsiManager.getInstance(project);
        List<KtFile> files = new ArrayList<>();
        for (VirtualFile virtualFile : FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)) {
            PsiFile file = psiManager.findFile(virtualFile);
            if (file instanceof KtFile ktFile) {
                files.add(ktFile);
            }
        }
        return files;
    }
}
