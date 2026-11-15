package consulo.kotlin.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.kotlin.core.helper.KotlinLocalizeService;
import consulo.localize.LocalizeValue;
import jakarta.inject.Singleton;

/**
 * Display texts the remapped KotlinLanguage/KotlinFileType ask for (see remap.xml in consulo/kotlin).
 */
@Singleton
@ServiceImpl
public class KotlinLocalizeServiceImpl implements KotlinLocalizeService {
    @Override
    public LocalizeValue getKotlinLanguageDisplayName() {
        return LocalizeValue.of("Kotlin");
    }

    @Override
    public LocalizeValue getKotlinFileTypeDescription() {
        return LocalizeValue.localizeTODO("Kotlin source files");
    }
}
