package consulo.kotlin.analysis;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ProcessCanceledException;
import consulo.document.util.TextRange;
import consulo.kotlin.compiler.idea.KotlinLanguage;
import consulo.language.Language;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import org.jetbrains.kotlin.analysis.api.KaSession;
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter;
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi;
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider;
import org.jetbrains.kotlin.psi.KtFile;

/** Compiler diagnostics of a Kotlin file, from the Analysis API. */
@ExtensionImpl
public class KotlinDiagnosticsAnnotatorFactory implements AnnotatorFactory {
    private static final Logger LOG = Logger.getInstance(KotlinDiagnosticsAnnotatorFactory.class);

    @Override
    public Annotator createAnnotator() {
        return KotlinDiagnosticsAnnotatorFactory::annotate;
    }

    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    private static void annotate(PsiElement element, AnnotationHolder holder) {
        if (!(element instanceof KtFile file)) {
            return;
        }
        try {
            KaSessionProvider provider = KaSessionProvider.Companion.getInstance(file.getProject());
            KaSession session = provider.getAnalysisSession(file);
            provider.beforeEnteringAnalysis(session, file);
            try {
                for (KaDiagnosticWithPsi<?> diagnostic : session.collectDiagnostics(file, KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)) {
                    HighlightSeverity severity = switch (diagnostic.getSeverity()) {
                        case ERROR -> HighlightSeverity.ERROR;
                        case WARNING -> HighlightSeverity.WARNING;
                        default -> HighlightSeverity.WEAK_WARNING;
                    };
                    for (TextRange range : diagnostic.getTextRanges()) {
                        holder.newAnnotation(severity, diagnostic.getDefaultMessage()).range(range).create();
                    }
                }
            }
            finally {
                provider.afterLeavingAnalysis(session, file);
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error("Kotlin analysis failed for " + file.getName(), e);
        }
    }
}
