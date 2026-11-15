// The members of base/project-structure that the upstream subset uses. IntelliJ's module is built on the workspace
// model, facets and compiler settings; here they come from the Analysis API project structure (Consulo's
// KotlinProjectStructureProvider in consulo-plugin/plugin).
package org.jetbrains.kotlin.idea.base.projectStructure

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.kaModule
import org.jetbrains.kotlin.config.LanguageVersionSettings

fun PsiElement.getKaModule(project: Project, useSiteModule: KaModule?): KaModule =
    kaModule(useSiteModule)

val PsiElement.languageVersionSettings: LanguageVersionSettings
    get() = getKaModule(project, useSiteModule = null).languageVersionSettings

val KaModule.languageVersionSettings: LanguageVersionSettings
    get() = when (this) {
        is KaDanglingFileModule -> contextModule.languageVersionSettings
        is KaSourceModule -> languageVersionSettings
        is KaScriptModule -> languageVersionSettings
        is KaLibraryModule -> KotlinProjectStructureProvider.getInstance(project).libraryLanguageVersionSettings
        else -> KotlinProjectStructureProvider.getInstance(project).globalLanguageVersionSettings
    }

val Project.languageVersionSettings: LanguageVersionSettings
    get() = KotlinProjectStructureProvider.getInstance(this).globalLanguageVersionSettings

val Module.languageVersionSettings: LanguageVersionSettings
    get() = project.languageVersionSettings
