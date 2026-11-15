package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider;
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderMerger;

import java.util.List;

@Singleton
@ServiceImpl
public class ConsuloKotlinDeclarationProviderMerger implements KotlinDeclarationProviderMerger {
    @Override
    public KotlinDeclarationProvider merge(List<? extends KotlinDeclarationProvider> providers) {
        return KotlinCompositeDeclarationProvider.Companion.create(providers);
    }
}
