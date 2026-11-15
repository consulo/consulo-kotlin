package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerByEventFactoryBase;

/** Modification trackers that follow the published Kotlin modification events. */
@Singleton
@ServiceImpl
public class ConsuloKotlinModificationTrackerFactory extends KotlinModificationTrackerByEventFactoryBase {
    @Inject
    public ConsuloKotlinModificationTrackerFactory(Project project) {
        super(project);
    }
}
