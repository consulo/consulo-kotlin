package consulo.kotlin.analysis;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.component.ProcessCanceledException;
import consulo.kotlin.compiler.idea.KotlinLanguage;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import kotlin.jvm.functions.Function1;
import org.jetbrains.kotlin.analysis.api.KaSession;
import org.jetbrains.kotlin.analysis.api.components.KaCompletionExtensionCandidateChecker;
import org.jetbrains.kotlin.analysis.api.components.KaExtensionApplicabilityResult;
import org.jetbrains.kotlin.analysis.api.components.KaScopeContext;
import org.jetbrains.kotlin.analysis.api.components.KaScopeWithKind;
import org.jetbrains.kotlin.analysis.api.components.KaUseSiteVisibilityChecker;
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource;
import org.jetbrains.kotlin.analysis.api.scopes.KaScope;
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider;
import org.jetbrains.kotlin.analysis.api.symbols.*;
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol;
import org.jetbrains.kotlin.analysis.api.types.KaType;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtQualifiedExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.resolution.KtResolvable;
import org.jetbrains.kotlin.types.Variance;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Basic name completion from the Analysis API: after a dot, the members of the receiver's type and the applicable
 * extensions in scope, or the static members of a class / the declarations of a package; elsewhere, everything
 * the scopes at the caret declare (locals, members of implicit receivers, file, package, imports and default imports).
 */
@ExtensionImpl
public class KotlinCompletionContributor extends CompletionContributor {
    private static final Logger LOG = Logger.getInstance(KotlinCompletionContributor.class);

    public KotlinCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider() {
            @Override
            public void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
                complete(parameters, result);
            }
        });
    }

    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    private static void complete(CompletionParameters parameters, CompletionResultSet result) {
        PsiElement position = parameters.getPosition();
        if (!(position.getParent() instanceof KtSimpleNameExpression nameExpression)
            || !(position.getContainingFile() instanceof KtFile file)
            || !KotlinAnalysisScope.isEnabled(parameters.getOriginalFile())) {
            return;
        }
        KtExpression receiver = nameExpression.getParent() instanceof KtQualifiedExpression qualified && qualified.getSelectorExpression() == nameExpression
            ? qualified.getReceiverExpression()
            : null;
        PrefixMatcher matcher = result.getPrefixMatcher();
        Function1<Name, Boolean> nameFilter = name -> !name.isSpecial() && matcher.prefixMatches(name.asString());
        try {
            KaSessionProvider provider = KaSessionProvider.Companion.getInstance(file.getProject());
            KaSession session = provider.getAnalysisSession(file);
            provider.beforeEnteringAnalysis(session, file);
            try {
                Set<String> seen = new HashSet<>();
                KaUseSiteVisibilityChecker visibility = session.createUseSiteVisibilityChecker(session.getSymbol(file), receiver, nameExpression);
                if (receiver != null) {
                    if (!addQualifierMembers(session, receiver, nameFilter, visibility, seen, result)) {
                        KaType type = session.getExpressionType(receiver);
                        if (type != null) {
                            addScope(session, session.getDeclarationScope(session.getScope(type)), nameFilter, false, visibility, seen, result);
                            addExtensions(session, file, nameExpression, receiver, nameFilter, visibility, seen, result);
                        }
                    }
                }
                else {
                    KaScopeContext scopeContext = session.scopeContext(file, nameExpression);
                    for (KaScopeWithKind scope : scopeContext.getScopes()) {
                        addScope(session, scope.getScope(), nameFilter, true, visibility, seen, result);
                    }
                }
            }
            finally {
                provider.afterLeavingAnalysis(session, file);
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error("Kotlin completion failed", e);
        }
    }

    /** Extension callables of the scopes at the caret that apply to the receiver. */
    private static void addExtensions(KaSession session, KtFile file, KtSimpleNameExpression nameExpression, KtExpression receiver,
                                      Function1<Name, Boolean> nameFilter, KaUseSiteVisibilityChecker visibility, Set<String> seen,
                                      CompletionResultSet result) {
        KaCompletionExtensionCandidateChecker checker = session.createExtensionCandidateChecker(file, nameExpression, receiver);
        for (KaScopeWithKind scope : session.scopeContext(file, nameExpression).getScopes()) {
            for (Iterator<KaCallableSymbol> it = scope.getScope().callables(nameFilter).iterator(); it.hasNext(); ) {
                KaCallableSymbol symbol = it.next();
                if (symbol.getReceiverParameter() == null || !visibility.isVisible(symbol)
                    || !(checker.computeApplicability(symbol) instanceof KaExtensionApplicabilityResult.Applicable)) {
                    continue;
                }
                LookupElement element = callable(session, symbol, seen);
                if (element != null) {
                    result.addElement(element);
                }
            }
        }
    }

    /**
     * A receiver that names a class (its static members, nested classes and the members of the object itself or its
     * companion) or a package (its declarations).
     *
     * @return whether the receiver is such a qualifier
     */
    private static boolean addQualifierMembers(KaSession session, KtExpression receiver, Function1<Name, Boolean> nameFilter,
                                               KaUseSiteVisibilityChecker visibility, Set<String> seen, CompletionResultSet result) {
        KtExpression qualifier = receiver instanceof KtQualifiedExpression qualified ? qualified.getSelectorExpression() : receiver;
        if (!(qualifier instanceof KtResolvable resolvable)) {
            return false;
        }
        KaSymbol symbol = session.resolveSymbol(resolvable);
        if (symbol instanceof KaClassSymbol classSymbol) {
            addScope(session, session.getStaticMemberScope(classSymbol), nameFilter, true, visibility, seen, result);
            KaClassSymbol object = classSymbol.getClassKind().isObject() ? classSymbol
                : classSymbol instanceof KaNamedClassSymbol named ? named.getCompanionObject() : null;
            if (object != null) {
                addScope(session, session.getMemberScope(object), nameFilter, false, visibility, seen, result);
            }
            return true;
        }
        if (symbol instanceof KaPackageSymbol packageSymbol) {
            addScope(session, session.getPackageScope(packageSymbol), nameFilter, true, visibility, seen, result);
            return true;
        }
        return false;
    }

    private static void addScope(KaSession session, KaScope scope, Function1<Name, Boolean> nameFilter, boolean classifiers,
                                 KaUseSiteVisibilityChecker visibility, Set<String> seen, CompletionResultSet result) {
        for (Iterator<KaCallableSymbol> it = scope.callables(nameFilter).iterator(); it.hasNext(); ) {
            KaCallableSymbol symbol = it.next();
            // extensions need an applicable receiver: only through addExtensions
            if (symbol.getReceiverParameter() != null || !visibility.isVisible(symbol)) {
                continue;
            }
            LookupElement element = callable(session, symbol, seen);
            if (element != null) {
                result.addElement(element);
            }
        }
        if (classifiers) {
            for (Iterator<KaClassifierSymbol> it = scope.classifiers(nameFilter).iterator(); it.hasNext(); ) {
                KaClassifierSymbol symbol = it.next();
                if (symbol instanceof KaDeclarationSymbol declaration && !visibility.isVisible(declaration)) {
                    continue;
                }
                if (symbol instanceof KaNamedSymbol named && seen.add("class:" + named.getName())) {
                    Image icon = symbol instanceof KaClassSymbol cls && cls.getClassKind().isObject() ? PlatformIconGroup.nodesClass()
                        : symbol instanceof KaClassSymbol cls && cls.getClassKind() == KaClassKind.INTERFACE ? PlatformIconGroup.nodesInterface()
                        : symbol instanceof KaClassSymbol cls && cls.getClassKind() == KaClassKind.ENUM_CLASS ? PlatformIconGroup.nodesEnum()
                        : PlatformIconGroup.nodesClass();
                    result.addElement(LookupElementBuilder.create(named.getName().asString()).withIcon(icon));
                }
            }
        }
    }

    private static LookupElement callable(KaSession session, KaCallableSymbol symbol, Set<String> seen) {
        if (!(symbol instanceof KaNamedSymbol named)) {
            return null;
        }
        String name = named.getName().asString();
        String type = render(session, symbol.getReturnType());
        if (symbol instanceof KaFunctionSymbol function) {
            StringJoiner tail = new StringJoiner(", ", "(", ")");
            for (KaValueParameterSymbol parameter : function.getValueParameters()) {
                tail.add(parameter.getName().asString() + ": " + render(session, parameter.getReturnType()));
            }
            if (!seen.add(name + tail)) {
                return null;
            }
            boolean hasParameters = !function.getValueParameters().isEmpty();
            return LookupElementBuilder.create(name)
                .withIcon(PlatformIconGroup.nodesMethod())
                .withTailText(tail.toString(), true)
                .withTypeText(type)
                .withInsertHandler((InsertionContext context, LookupElement item) -> insertParentheses(context, hasParameters));
        }
        if (!seen.add(name)) {
            return null;
        }
        Image icon = symbol instanceof KaLocalVariableSymbol ? PlatformIconGroup.nodesVariable()
            : symbol instanceof KaValueParameterSymbol ? PlatformIconGroup.nodesParameter()
            : symbol instanceof KaJavaFieldSymbol ? PlatformIconGroup.nodesField()
            : PlatformIconGroup.nodesProperty();
        return LookupElementBuilder.create(name).withIcon(icon).withTypeText(type);
    }

    private static void insertParentheses(InsertionContext context, boolean hasParameters) {
        int tail = context.getTailOffset();
        CharSequence text = context.getDocument().getCharsSequence();
        if (tail < text.length() && text.charAt(tail) == '(') {
            return;
        }
        context.getDocument().insertString(tail, "()");
        context.getEditor().getCaretModel().moveToOffset(tail + (hasParameters ? 1 : 2));
    }

    private static String render(KaSession session, KaType type) {
        return session.render(type, KaTypeRendererForSource.INSTANCE.getWITH_SHORT_NAMES(), Variance.INVARIANT);
    }
}
