// Replaces base/analysis KotlinUastOutOfCodeBlockModificationTracker.kt (seeds.txt --replace): IntelliJ's adds the
// trackers of the UAST languages, Consulo has no UAST. The other JVM language whose declarations Kotlin code sees is
// Java, so Java changes count instead.
package org.jetbrains.kotlin.idea.base.analysis

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalSourceModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalSourceOutOfBlockModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventListener
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleOutOfBlockModificationEvent

@Service(Service.Level.PROJECT)
class KotlinUastOutOfCodeBlockModificationTracker internal constructor(project: Project) : ModificationTracker {
    private val modificationTracker = SimpleModificationTracker()
    private val javaTracker = PsiModificationTracker.getInstance(project).forLanguage(JavaLanguage.INSTANCE)

    init {
        project.analysisMessageBus.connect(project).subscribe(
            KotlinModificationEvent.TOPIC,
            KotlinModificationEventListener { event ->
                when (event) {
                    is KotlinModuleOutOfBlockModificationEvent,
                    KotlinGlobalSourceModuleStateModificationEvent,
                    KotlinGlobalSourceOutOfBlockModificationEvent,
                        -> modificationTracker.incModificationCount()

                    else -> {}
                }
            }
        )
    }

    override fun getModificationCount(): Long = modificationTracker.modificationCount + javaTracker.modificationCount

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinUastOutOfCodeBlockModificationTracker =
            project.service()
    }
}
