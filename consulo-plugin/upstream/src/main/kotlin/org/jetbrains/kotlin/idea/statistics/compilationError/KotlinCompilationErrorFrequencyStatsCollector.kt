// Replaces base/statistics KotlinCompilationErrorFrequencyStatsCollector.kt (seeds.txt --replace): IntelliJ's reports
// compilation error frequencies to its usage statistics, which are always off in Consulo.
package org.jetbrains.kotlin.idea.statistics.compilationError

import org.jetbrains.kotlin.psi.KtFile

object KotlinCompilationErrorFrequencyStatsCollector {
    @Suppress("UNUSED_PARAMETER")
    fun recordCompilationErrorsHappened(diagnosticsFactoryNames: Sequence<String>, psiFile: KtFile) {
    }
}
