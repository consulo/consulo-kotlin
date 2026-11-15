// Replaces formatter/minimal KotlinCommonCodeStyleSettings.java (seeds.txt --replace): IntelliJ's adapts private parts
// of IntelliJ's CommonCodeStyleSettings (reflection, its diff filter and provider-based serialization). This keeps its
// public shape on Consulo's CommonCodeStyleSettings: the chosen code style defaults, applied when created and read.
package org.jetbrains.kotlin.idea.formatter;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettings;
import org.jdom.Element;
import org.jetbrains.kotlin.idea.KotlinLanguage;

public class KotlinCommonCodeStyleSettings extends CommonCodeStyleSettings {
    public String CODE_STYLE_DEFAULTS = null;

    private final boolean isTempForDeserialize;

    public KotlinCommonCodeStyleSettings() {
        this(false);
    }

    private KotlinCommonCodeStyleSettings(boolean isTempForDeserialize) {
        super(KotlinLanguage.INSTANCE);
        this.isTempForDeserialize = isTempForDeserialize;

        if (!isTempForDeserialize) {
            // By default, use the Kotlin official style guide
            applyKotlinCodeStyle(KotlinOfficialStyleGuide.CODE_STYLE_ID, this, false);
        }
    }

    @Override
    public void readExternal(Element element) {
        if (isTempForDeserialize) {
            super.readExternal(element);
            return;
        }

        KotlinCommonCodeStyleSettings tempDeserialize = new KotlinCommonCodeStyleSettings(true);
        tempDeserialize.readExternal(element);

        applyKotlinCodeStyle(tempDeserialize.CODE_STYLE_DEFAULTS, this, true);

        super.readExternal(element);
    }

    /** Consulo's clone makes a plain CommonCodeStyleSettings; this one keeps the Kotlin settings and their defaults. */
    @Override
    public CommonCodeStyleSettings clone(CodeStyleSettings rootSettings) {
        KotlinCommonCodeStyleSettings commonSettings = new KotlinCommonCodeStyleSettings(true);
        commonSettings.copyFrom(this);
        commonSettings.setForceArrangeMenuAvailable(isForceArrangeMenuAvailable());

        IndentOptions indentOptions = getIndentOptions();
        if (indentOptions != null) {
            commonSettings.initIndentOptions().copyFrom(indentOptions);
        }

        ArrangementSettings arrangementSettings = getArrangementSettings();
        if (arrangementSettings != null) {
            commonSettings.setArrangementSettings(arrangementSettings.clone());
        }
        return commonSettings;
    }

    private static void applyKotlinCodeStyle(String codeStyleId, CommonCodeStyleSettings codeStyleSettings, boolean modifyCodeStyle) {
        if (KotlinOfficialStyleGuide.CODE_STYLE_ID.equals(codeStyleId)) {
            KotlinOfficialStyleGuide.INSTANCE.applyToCommonSettings(codeStyleSettings, modifyCodeStyle);
        }
        else if (KotlinObsoleteStyleGuide.CODE_STYLE_ID.equals(codeStyleId)) {
            KotlinObsoleteStyleGuide.INSTANCE.applyToCommonSettings(codeStyleSettings, modifyCodeStyle);
        }
    }
}
