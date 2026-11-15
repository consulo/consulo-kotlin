// The members of base/facet TargetPlatformDetector.kt that the upstream subset uses. IntelliJ's come from Kotlin
// facets and compiler settings; here a Kotlin element's platform is its Analysis API module's, and Consulo Kotlin
// modules are JVM ones.
package org.jetbrains.kotlin.idea.base.facet.platform

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.projectStructure.kaModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtElement

val KtElement.platform: TargetPlatform
    get() = containingKtFile.kaModule(null).targetPlatform

val Project.platform: TargetPlatform?
    get() = JvmPlatforms.defaultJvmPlatform

val Module.platform: TargetPlatform
    get() = JvmPlatforms.defaultJvmPlatform
