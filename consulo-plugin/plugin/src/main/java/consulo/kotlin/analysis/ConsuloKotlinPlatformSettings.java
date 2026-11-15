package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin;
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings;

/** Libraries are analyzed from their class files (the stub-based decompiler is not ported). */
@Singleton
@ServiceImpl
public class ConsuloKotlinPlatformSettings implements KotlinPlatformSettings {
    @Override
    public KotlinDeserializedDeclarationsOrigin getDeserializedDeclarationsOrigin() {
        return KotlinDeserializedDeclarationsOrigin.BINARIES;
    }
}
