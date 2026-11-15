"""Register the upstream plugin classes of consulo-plugin/upstream as Consulo services, extensions and topic listeners.

Usage: python3 consulo-plugin/tools/genservices.py (after consulo-plugin/upstream compiled once)
Reads the IntelliJ plugin descriptors (resources/*.xml) of the upstream modules that consulo-plugin/upstream/pom.xml
compiles, and IntelliJ light services (@Service), and rewrites consulo-plugin/remap/remap.xml between the genservices
markers: @ServiceAPI on plugin-side service interfaces, and an InjectingBinding per implementation. Only implementations
that are compiled are bound, and only extensions of extension points with a Consulo counterpart (EXTENSION_POINTS);
the rest is reported.
Also relocates upstream classes whose package the compiler jar has too (a JPMS split package; IntelliJ loads both from
one class path) into a ".plugin" subpackage.
"""
import glob, os, re, zipfile
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
UPSTREAM_POM = f'{ROOT}/consulo-plugin/upstream/pom.xml'
REMAP = f'{ROOT}/consulo-plugin/remap/remap.xml'
COMPILER_REMAP = os.path.expanduser('~/.m2/repository/consulo/kotlin/consulo.kotlin.remapped/3-SNAPSHOT')
UPSTREAM_CLASSES = f'{ROOT}/consulo-plugin/upstream/target/classes'

# IntelliJ extension point -> (Consulo extension API, its component scope)
EXTENSION_POINTS = {
    'com.intellij.stubIndex': ('consulo.language.psi.stub.StubIndexExtension', 'APPLICATION'),
    'com.intellij.fileBasedIndex': ('consulo.language.psi.stub.FileBasedIndexExtension', 'APPLICATION'),
    'com.intellij.indexedRootsProvider': ('consulo.language.psi.stub.IndexableSetContributor', 'APPLICATION'),
    'com.intellij.psi.treeChangePreprocessor': ('consulo.kotlin.compat.psi.PsiTreeChangePreprocessor', 'PROJECT'),
    'org.jetbrains.kotlin.psiReferenceProvider': ('org.jetbrains.kotlin.references.KotlinPsiReferenceProviderContributor', 'PROJECT'),
}
# extension points with no meaning in Consulo (or with a Consulo replacement), not reported
IGNORED = {'com.intellij.registryKey', 'com.intellij.notificationGroup'}
# analysis statistics are always off in Consulo
SKIP = re.compile(r'[Ss]tatistics|OpenTelemetry')


def compiled_files():
    """src-root-relative paths of the compiled upstream files, by module directory."""
    text = open(UPSTREAM_POM).read()
    files = {}
    for m in re.finditer(r'<directory>\$\{plugin\.root\}/(.+?)/src</directory>\s*<includes>(.*?)</includes>', text, re.S):
        files[m.group(1)] = re.findall(r'<include>(.+?)</include>', m.group(2))
    return files


def compiled_classes(files):
    """Fully qualified names (with $ for nested) of the classes declared in the compiled files."""
    names = set()
    for module, rels in files.items():
        for rel in rels:
            path = f'{ROOT}/{module}/src/{rel}'
            text = open(path, errors='ignore').read()
            pkg = re.search(r'^package\s+([\w.]+)', text, re.M)
            pkg = pkg.group(1) if pkg else ''
            # nesting by indentation: top level at column 0, nested one level deeper
            stack = []
            for line in text.splitlines():
                m = re.match(r'^(\s*)(?:[\w@()".=, ]+\s+)?(?:class|interface|object|enum class|annotation class)\s+(\w+)', line)
                if not m or re.match(r'^\s*(//|\*)', line):
                    continue
                depth = len(m.group(1).expandtabs(4)) // 4
                stack = stack[:depth] + [m.group(2)]
                names.add(f'{pkg}.' + '$'.join(stack))
    return names


def light_services(files):
    """(class, scope) of @Service(...) classes in the compiled files."""
    result = []
    for module, rels in files.items():
        for rel in rels:
            text = open(f'{ROOT}/{module}/src/{rel}', errors='ignore').read()
            pkg = re.search(r'^package\s+([\w.]+)', text, re.M).group(1)
            for m in re.finditer(r'@Service\(([^)]*)\)\s*(?:@\w+(?:\([^)]*\))?\s*)*(?:\w+\s+)*class\s+(\w+)', text):
                scope = 'PROJECT' if 'PROJECT' in m.group(1) else 'APPLICATION'
                result.append((f'{pkg}.{m.group(2)}', scope))
    return result


def compiler_service_apis():
    """Service interfaces the compiler port annotates (its remap.xml modifications)."""
    jars = glob.glob(f'{COMPILER_REMAP}/*-mapping.xml')
    text = open(max(jars, key=os.path.getmtime)).read() if jars else ''
    return set(re.findall(r'<class name="([\w.$]+)">\s*<addAnnotation type="consulo.annotation.component.ServiceAPI"', text))


def split_package_rules():
    """Class rules moving upstream classes out of packages that the compiler jar has too."""
    jar = f'{COMPILER_REMAP}/consulo.kotlin.remapped-3-SNAPSHOT.jar'
    compiler_packages = {n.rsplit('/', 1)[0] for n in zipfile.ZipFile(jar).namelist() if n.endswith('.class') and '/' in n}
    rules = []
    for path in sorted(glob.glob(f'{UPSTREAM_CLASSES}/**/*.class', recursive=True)):
        name = os.path.relpath(path, UPSTREAM_CLASSES)[:-6]
        pkg, _, simple = name.rpartition('/')
        if pkg in compiler_packages:
            rules.append((name.replace('/', '.'), f'{pkg}/plugin/{simple}'.replace('/', '.')))
    if not os.path.isdir(UPSTREAM_CLASSES):
        print('warning: consulo-plugin/upstream is not compiled, split packages are not relocated')
    return rules


def replace_between(text, tag, body):
    begin, end = f'<!-- genservices:{tag}:begin -->', f'<!-- genservices:{tag}:end -->'
    m = re.search(rf'([ \t]*){re.escape(begin)}.*?{re.escape(end)}', text, re.S)
    if not m:
        raise SystemExit(f'remap.xml has no {begin} marker')
    return text[:m.start()] + f'{m.group(1)}{begin}\n{body}{m.group(1)}{end}' + text[m.end():]


def main():
    files = compiled_files()
    classes = compiled_classes(files)
    compiler_apis = compiler_service_apis()
    services, extensions, listeners, plugin_apis = [], [], [], {}
    skipped, unmapped = [], {}
    for module in files:
        for descriptor in glob.glob(f'{ROOT}/{module}/resources/*.xml'):
            root = ET.parse(descriptor).getroot()
            for ext in root.iter('extensions'):
                ns = ext.get('defaultExtensionNs')
                for e in ext:
                    point = f'{ns}.{e.tag}'
                    if e.tag in ('projectService', 'applicationService'):
                        impl = e.get('serviceImplementation')
                        if impl not in classes or SKIP.search(impl):
                            skipped.append(impl)
                            continue
                        api = e.get('serviceInterface') or impl
                        scope = 'PROJECT' if e.tag == 'projectService' else 'APPLICATION'
                        services.append((impl, api, scope))
                        if api in classes:
                            plugin_apis[api] = scope
                        elif api not in compiler_apis:
                            print(f'warning: {api} is not a Consulo service API (compiler remap.xml)')
                        continue
                    impl = e.get('implementation') or e.get('implementationClass')
                    if impl not in classes:
                        if impl:
                            skipped.append(impl)
                        continue
                    if point in EXTENSION_POINTS:
                        extensions.append((impl,) + EXTENSION_POINTS[point])
                    elif point not in IGNORED:
                        unmapped.setdefault(point, []).append(impl)
            for tag, scope in (('projectListeners', 'PROJECT'), ('applicationListeners', 'APPLICATION')):
                for listeners_el in root.iter(tag):
                    for l in listeners_el:
                        impl = l.get('class')
                        if impl in classes:
                            listeners.append((impl, l.get('topic'), scope))
                        else:
                            skipped.append(impl)
    for impl, scope in light_services(files):
        services.append((impl, impl, scope))
        plugin_apis[impl] = scope

    mods = ''.join(f'''        <class name="{api}">
            <addAnnotation type="consulo.annotation.component.ServiceAPI">
                <enum name="value" type="consulo.annotation.component.ComponentScope" value="{scope}"/>
            </addAnnotation>
        </class>
''' for api, scope in sorted(plugin_apis.items()))
    binds = ''.join(f'            <binding impl="{impl}" api="{api}" scope="{scope}" type="service"/>\n' for impl, api, scope in sorted(set(services)))
    binds += ''.join(f'            <binding impl="{impl}" api="{api}" scope="{scope}"/>\n' for impl, api, scope in sorted(set(extensions)))
    binds += ''.join(f'            <binding impl="{impl}" api="{api}" scope="{scope}" type="topic"/>\n' for impl, api, scope in sorted(set(listeners)))
    splits = split_package_rules()
    text = open(REMAP).read()
    text = replace_between(text, 'splitpackages', ''.join(f'        <rule from="{a}" to="{b}"/>\n' for a, b in splits))
    text = replace_between(text, 'modifications', mods)
    text = replace_between(text, 'bindings', binds)
    open(REMAP, 'w').write(text)
    print(f'{len(set(services))} services, {len(set(extensions))} extensions, {len(set(listeners))} listeners, '
          f'{len(splits)} relocated classes (split packages); '
          f'{len(set(skipped))} registrations of classes that are not compiled')
    for point, impls in sorted(unmapped.items()):
        print(f'no Consulo extension point for {point}: {", ".join(sorted(impls))}')


main()
