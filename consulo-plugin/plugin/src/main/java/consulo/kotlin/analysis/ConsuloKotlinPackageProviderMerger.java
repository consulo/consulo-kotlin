package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinCompositePackageProvider;
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider;
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderMerger;

import java.util.List;

@Singleton
@ServiceImpl
public class ConsuloKotlinPackageProviderMerger implements KotlinPackageProviderMerger {
    @Override
    public KotlinPackageProvider merge(List<? extends KotlinPackageProvider> providers) {
        return KotlinCompositePackageProvider.Companion.create(providers);
    }
}
