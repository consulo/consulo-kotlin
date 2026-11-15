package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions;

/**
 * Analysis may run on the UI thread: Consulo resolves references there (e.g. Go to Declaration), and it has no
 * equivalent of IntelliJ's {@code allowAnalysisOnEdt} blocks around such actions. Analysis in write actions stays prohibited.
 */
@Singleton
@ServiceImpl
public class ConsuloKotlinAnalysisPermissionOptions implements KotlinAnalysisPermissionOptions {
    @Override
    public boolean getDefaultIsAnalysisAllowedOnEdt() {
        return true;
    }

    @Override
    public boolean getDefaultIsAnalysisAllowedInWriteAction() {
        return false;
    }
}
