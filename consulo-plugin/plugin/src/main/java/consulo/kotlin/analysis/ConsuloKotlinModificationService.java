package consulo.kotlin.analysis;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.event.ApplicationListener;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.modification.KaElementModificationType;
import org.jetbrains.kotlin.analysis.api.platform.modification.KaSourceModificationLocality;
import org.jetbrains.kotlin.analysis.api.platform.modification.KaSourceModificationService;
import org.jetbrains.kotlin.analysis.api.platform.modification.UtilsKt;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Turns PSI and project root changes into Analysis API invalidation: in-block and out-of-block modifications of source
 * files, and module state modifications on root changes. A port of IntelliJ's FirIdeOutOfBlockModificationService,
 * on the public PSI listener API (Consulo's event codes are internal).
 * <p>
 * Within one write action, a file whose module already got an out-of-block event is skipped, and after
 * {@link #FILE_PROCESSING_LIMIT} files a single global event replaces the per-file processing.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ConsuloKotlinModificationService {
    private static final int FILE_PROCESSING_LIMIT = 5;

    private static final Set<String> GLOBAL_PROPERTIES = Set.of(
        PsiTreeChangeEvent.PROP_ROOTS, PsiTreeChangeEvent.PROP_FILE_TYPES, PsiTreeChangeEvent.PROP_UNLOADED_PSI);

    private final Project myProject;

    /** Files processed in the current write action: true once their module got an out-of-block event. */
    private final Map<PsiFile, Boolean> myFileStates = new WeakHashMap<>();
    private volatile boolean myGlobalEventPublished;
    private int myWriteActionDepth;

    @Inject
    public ConsuloKotlinModificationService(Project project) {
        myProject = project;
        PsiManager.getInstance(project).addPsiTreeChangeListener(new TreeChangeListener(), project);
        ApplicationManager.getApplication().addApplicationListener(new StateResetListener(), project);
        project.getMessageBus().connect(project).subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                UtilsKt.publishGlobalModuleStateModificationEvent(myProject);
            }
        });
    }

    public static ConsuloKotlinModificationService getInstance(Project project) {
        return project.getInstance(ConsuloKotlinModificationService.class);
    }

    private synchronized void handle(PsiElement rootElement, PsiElement child, KaElementModificationType modificationType) {
        if (myGlobalEventPublished || rootElement == null || !rootElement.isPhysical()) {
            return;
        }
        PsiFile file = rootElement.getContainingFile();
        if (file == null) {
            processModification(rootElement, child, modificationType, null);
            return;
        }
        Boolean outOfBlockPublished = myFileStates.get(file);
        if (outOfBlockPublished == null) {
            if (myFileStates.size() >= FILE_PROCESSING_LIMIT) {
                publishGlobalModificationEvent();
                return;
            }
            myFileStates.put(file, false);
        }
        else if (outOfBlockPublished) {
            return;
        }
        processModification(rootElement, child, modificationType, file);
    }

    private void processModification(PsiElement rootElement, PsiElement child, KaElementModificationType modificationType, PsiFile file) {
        PsiElement target = child != null ? child : rootElement;
        KaSourceModificationService service = KaSourceModificationService.Companion.getInstance(myProject);
        KaSourceModificationLocality locality = service.detectLocality(target, modificationType);
        service.handleInvalidation(target, locality);
        if (file != null && locality instanceof KaSourceModificationLocality.OutOfBlock) {
            myFileStates.put(file, true);
        }
    }

    private synchronized void publishGlobalModificationEvent() {
        if (!myGlobalEventPublished) {
            UtilsKt.publishGlobalSourceOutOfBlockModificationEvent(myProject);
            myGlobalEventPublished = true;
        }
    }

    private synchronized void resetState() {
        myFileStates.clear();
        myGlobalEventPublished = false;
    }

    /** The event kinds IntelliJ's preprocessor handles; generic "children changed" events come with specific ones too. */
    private class TreeChangeListener extends PsiTreeChangeAdapter {
        @Override
        public void beforeChildRemoval(PsiTreeChangeEvent event) {
            handle(event.getParent(), event.getChild(), KaElementModificationType.Unknown.INSTANCE);
        }

        @Override
        public void beforeChildReplacement(PsiTreeChangeEvent event) {
            handle(event.getParent(), event.getOldChild(), KaElementModificationType.Unknown.INSTANCE);
        }

        @Override
        public void beforeChildMovement(PsiTreeChangeEvent event) {
            handle(event.getParent(), event.getChild(), KaElementModificationType.Unknown.INSTANCE);
        }

        @Override
        public void childAdded(PsiTreeChangeEvent event) {
            handle(event.getParent(), event.getChild(), KaElementModificationType.ElementAdded.INSTANCE);
        }

        @Override
        public void childRemoved(PsiTreeChangeEvent event) {
            if (event.getChild() != null) {
                handle(event.getParent(), null, new KaElementModificationType.ElementRemoved(event.getChild()));
            }
        }

        @Override
        public void childReplaced(PsiTreeChangeEvent event) {
            handle(event.getParent(), event.getNewChild(), KaElementModificationType.Unknown.INSTANCE);
        }

        @Override
        public void childMoved(PsiTreeChangeEvent event) {
            handle(event.getNewParent(), event.getChild(), KaElementModificationType.Unknown.INSTANCE);
        }

        @Override
        public void propertyChanged(PsiTreeChangeEvent event) {
            if (GLOBAL_PROPERTIES.contains(event.getPropertyName())) {
                publishGlobalModificationEvent();
            }
        }
    }

    /** The per-file state lives for one (outermost) write action. */
    private class StateResetListener implements ApplicationListener {
        @Override
        public void writeActionStarted(Object action) {
            synchronized (ConsuloKotlinModificationService.this) {
                if (myWriteActionDepth++ == 0) {
                    resetState();
                }
            }
        }

        @Override
        public void writeActionFinished(Object action) {
            synchronized (ConsuloKotlinModificationService.this) {
                if (--myWriteActionDepth == 0) {
                    resetState();
                }
            }
        }
    }
}
