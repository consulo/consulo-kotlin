package consulo.kotlin.analysis;

import consulo.annotation.component.ServiceImpl;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.project.content.ProjectRootModificationTracker;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProviderBase;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule;
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consulo modules, their libraries and SDKs as Analysis API modules. Built on demand and dropped when the project roots
 * change.
 */
@Singleton
@ServiceImpl
public class ConsuloKotlinProjectStructureProvider extends KotlinProjectStructureProviderBase {
    private final Project myProject;
    private final Map<Module, ConsuloKaSourceModule> mySourceModules = new ConcurrentHashMap<>();
    private final Map<Object, ConsuloKaLibraryModule> myLibraryModules = new ConcurrentHashMap<>();
    private volatile long myRootsModificationCount = -1;

    @Inject
    public ConsuloKotlinProjectStructureProvider(Project project) {
        myProject = project;
    }

    public static ConsuloKotlinProjectStructureProvider getInstance(Project project) {
        return (ConsuloKotlinProjectStructureProvider) project.getInstance(
            org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider.class);
    }

    private void checkRoots() {
        long count = ProjectRootModificationTracker.getInstance(myProject).getModificationCount();
        if (count != myRootsModificationCount) {
            mySourceModules.clear();
            myLibraryModules.clear();
            myRootsModificationCount = count;
        }
    }

    public ConsuloKaSourceModule getSourceModule(Module module) {
        checkRoots();
        return mySourceModules.computeIfAbsent(module, m -> new ConsuloKaSourceModule(m, this));
    }

    public List<ConsuloKaSourceModule> getAllSourceModules() {
        List<ConsuloKaSourceModule> result = new ArrayList<>();
        for (Module module : ModuleManager.getInstance(myProject).getModules()) {
            result.add(getSourceModule(module));
        }
        return result;
    }

    /** All library and SDK modules the project modules depend on. */
    public List<ConsuloKaLibraryModule> getAllLibraryModules() {
        List<ConsuloKaLibraryModule> result = new ArrayList<>();
        for (ConsuloKaSourceModule module : getAllSourceModules()) {
            for (KaModule dependency : module.getDirectRegularDependencies()) {
                if (dependency instanceof ConsuloKaLibraryModule library && !result.contains(library)) {
                    result.add(library);
                }
            }
        }
        return result;
    }

    List<KaModule> getDependencies(Module module) {
        List<KaModule> result = new ArrayList<>();
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            KaModule dependency = toModule(entry);
            if (dependency != null) {
                result.add(dependency);
            }
        }
        return result;
    }

    private KaModule toModule(OrderEntry entry) {
        if (entry instanceof ModuleOrderEntry moduleEntry) {
            Module module = moduleEntry.getModule();
            return module != null ? getSourceModule(module) : null;
        }
        if (entry instanceof LibraryOrderEntry libraryEntry) {
            Library library = libraryEntry.getLibrary();
            return library != null ? getLibraryModule(library) : null;
        }
        if (entry instanceof ModuleExtensionWithSdkOrderEntry sdkEntry) {
            Sdk sdk = sdkEntry.getSdk();
            return sdk != null ? getSdkModule(sdk) : null;
        }
        return null;
    }

    private ConsuloKaLibraryModule getLibraryModule(Library library) {
        checkRoots();
        return myLibraryModules.computeIfAbsent(library, key -> new ConsuloKaLibraryModule(myProject,
            String.valueOf(library.getName()), Arrays.asList(library.getFiles(BinariesOrderRootType.ID)), false));
    }

    private ConsuloKaLibraryModule getSdkModule(Sdk sdk) {
        checkRoots();
        return myLibraryModules.computeIfAbsent(sdk, key -> new ConsuloKaLibraryModule(myProject,
            sdk.getName(), Arrays.asList(sdk.getRootProvider().getFiles(BinariesOrderRootType.ID)), true));
    }

    @Override
    public KaModule getModule(PsiElement element, KaModule useSiteModule) {
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return getNotUnderContentRootModule(myProject);
        }
        KaModule special = computeSpecialModule(file);
        if (special != null) {
            return special;
        }
        VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
        if (virtualFile == null) {
            return new ConsuloKaNotUnderContentRootModule(myProject, file);
        }
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(myProject);
        if (fileIndex.isInLibrary(virtualFile)) {
            for (OrderEntry entry : fileIndex.getOrderEntriesForFile(virtualFile)) {
                if (entry instanceof LibraryOrderEntry || entry instanceof ModuleExtensionWithSdkOrderEntry) {
                    KaModule module = toModule(entry);
                    if (module != null) {
                        return module;
                    }
                }
            }
        }
        // like a source module's content scope: only source roots, other module content is not under a content root
        Module module = fileIndex.isInSourceContent(virtualFile) ? fileIndex.getModuleForFile(virtualFile) : null;
        if (module != null) {
            return getSourceModule(module);
        }
        return new ConsuloKaNotUnderContentRootModule(myProject, file);
    }

    @Override
    protected KaNotUnderContentRootModule getNotUnderContentRootModule(Project project) {
        return new ConsuloKaNotUnderContentRootModule(project, null);
    }

    @Override
    public List<KaModule> getImplementingModules(KaModule module) {
        return List.of();
    }

    @Override
    public LanguageVersionSettings getGlobalLanguageVersionSettings() {
        return LanguageVersionSettingsImpl.DEFAULT;
    }

    @Override
    public LanguageVersionSettings getLibraryLanguageVersionSettings() {
        return LanguageVersionSettingsImpl.DEFAULT;
    }
}
