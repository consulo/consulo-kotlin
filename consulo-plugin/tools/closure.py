"""File closure of upstream plugin classes across the modules of this repository, for filter-src include lists.

Usage: python3 consulo-plugin/tools/closure.py [--pom <pom.xml>] [--stop <module dir>]... [--replace <file>]... <seed>... [@<file>]...
  seed: a fq class name or a src-relative file; @file: more arguments, one per line (# comments)
  --pom: write the include lists into the pom, between the closure:begin/end markers, and the message bundles and
         icons of the modules between the closure:resources:begin/end markers
  --replace: a src-relative file that consulo-plugin/upstream/src/main/kotlin replaces (neither included nor followed)
Prints, per module (a directory with a non-test .iml and a src root), the files the seeds reach. A file is reached when
a reached file names one of its top-level declarations and can see it (same package, explicit import or star import).
Over-approximates (simple names, no scopes) within the modules a file's module can see (.iml dependencies), which is
fine for compiling. --stop modules are reported, not followed.
CLOSURE=-v lists the files; CLOSURE=why:<file name>[,<file name>...] prints the chain of files (and names) that reaches a file.
"""
import glob, os, re, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
TOP_LEVEL = re.compile(
    r'^ ?(?:@\S+\s+)*(?:(?:public|internal|private|protected|open|abstract|sealed|data|enum|annotation|value|inline|'
    r'fun|external|expect|actual|const|final|static|operator|infix|tailrec|suspend|lateinit)\s+)*'
    r'(?:class|interface|object|typealias|fun|val|var|@interface|record)\s+(?:<[^>]*>\s*)?(?:[\w.<>?, ]+\.)?(\w+)',
    re.M)


# resources of the reached modules that the compiled classes load: message bundles and icons
RESOURCES = ['messages/**/*.properties', '**/*.svg', '**/*.png']


def modules():
    result = {}
    for iml in glob.glob(f'{ROOT}/**/*.iml', recursive=True):
        d = os.path.dirname(iml)
        rel = os.path.relpath(d, ROOT)
        if re.search(r'(^|/)(test|tests|testData|target|consulo-plugin)(/|$)', rel) or 'test' in os.path.basename(iml).lower():
            continue
        if os.path.isdir(f'{d}/src'):
            result[rel] = f'{d}/src'
    return result


def visible_modules(module_dirs):
    """Per module directory, the module directories its code can see: itself, its compile dependencies (.iml) and what
    those export, transitively. IntelliJ compiles each module separately, so declarations elsewhere are not visible."""
    name_to_dir, deps = {}, {}
    for rel in module_dirs:
        imls = [i for i in glob.glob(f'{ROOT}/{rel}/*.iml') if 'test' not in os.path.basename(i).lower()]
        if not imls:
            continue
        name_to_dir[os.path.basename(imls[0])[:-4]] = rel
        text = open(imls[0]).read()
        deps[rel] = [(m.group(1), 'exported=""' in m.group(0))
                     for m in re.finditer(r'<orderEntry type="module" module-name="([^"]+)"[^>]*>', text)
                     if 'scope="RUNTIME"' not in m.group(0) and 'scope="TEST"' not in m.group(0)]

    def exported(rel, seen):
        result = set()
        for name, is_exported in deps.get(rel, ()):
            d = name_to_dir.get(name)
            if d and is_exported and d not in seen:
                seen.add(d)
                result |= {d} | exported(d, seen)
        return result

    result = {}
    for rel in module_dirs:
        if rel not in deps:
            continue
        visible = {rel}
        for name, _ in deps[rel]:
            d = name_to_dir.get(name)
            if d:
                visible |= {d} | exported(d, {d})
        result[rel] = visible
    return result


def scan(path):
    text = open(path, errors='ignore').read()
    text = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
    text = re.sub(r'//[^\n]*', '', text)
    pkg = re.search(r'^package\s+([\w.]+)', text, re.M)
    # file-private declarations are not visible to other files
    names = {m.group(1) for m in TOP_LEVEL.finditer(text) if not re.match(r'^ ?(?:@\S+\s+)*private\s', m.group(0))}
    if path.endswith('.java'):
        names.add(os.path.basename(path)[:-5])
    imports = re.findall(r'^import\s+(?:static\s+)?([\w.]+)(\.\*)?', text, re.M)
    return {
        'pkg': pkg.group(1) if pkg else '',
        'names': names,
        'imports': {'.'.join(i.split('.')[:k]) for i, star in imports if not star for k in range(2, i.count('.') + 2)},
        'stars': {i for i, star in imports if star},
        'idents': set(re.findall(r'\b[A-Za-z_]\w*\b', text)),
    }


def main():
    args = []
    for a in sys.argv[1:]:
        if a.startswith('@'):
            for line in open(a[1:]):
                line = line.split('#')[0].strip()
                if line:
                    args += line.split()
        else:
            args.append(a)
    pom = args[args.index('--pom') + 1] if '--pom' in args else None
    if pom:
        i = args.index('--pom')
        args = args[:i] + args[i + 2:]
    stops = {args[i + 1] for i, a in enumerate(args) if a == '--stop'}
    replaced = {args[i + 1] for i, a in enumerate(args) if a == '--replace'}
    seeds = [a for i, a in enumerate(args) if a not in ('--stop', '--replace') and (i == 0 or args[i - 1] not in ('--stop', '--replace'))]
    files, owner = {}, {}
    for module, src in modules().items():
        for d, _, fs in os.walk(src):
            for f in fs:
                if f.endswith(('.kt', '.java')):
                    p = os.path.join(d, f)
                    files[p] = scan(p)
                    owner[p] = module
    declared = {}
    for p, info in files.items():
        for n in info['names']:
            declared.setdefault(n, []).append(p)
    todo = []
    explicit = set()  # files named as seeds: included even from a --stop module
    for s in seeds:
        if s.endswith(('.kt', '.java')):
            found = [p for p in files if p.endswith('/' + s)]
            todo += found
            explicit.update(found)
        else:
            pkg, name = s.rsplit('.', 1)
            todo += [p for p in declared.get(name.split('$')[0], []) if files[p]['pkg'] == pkg]
    visible = visible_modules(set(owner.values()))
    done = set()
    parent = {}  # reached file -> the file that reached it, for CLOSURE=why:<file name>
    while todo:
        p = todo.pop()
        if p in done:
            continue
        done.add(p)
        if owner[p] in stops and p not in explicit or any(p.endswith('/' + r) for r in replaced):
            continue
        me = files[p]
        for ident in me['idents']:
            for o in declared.get(ident, ()):
                info = files[o]
                if owner[p] in visible and owner[o] not in visible[owner[p]]:
                    continue  # another module the file's module does not depend on
                if info['pkg'] == me['pkg'] or f"{info['pkg']}.{ident}" in me['imports'] or info['pkg'] in me['stars']:
                    if o not in done:
                        parent.setdefault(o, (p, ident))
                        todo.append(o)
    by_module = {}
    for p in done:
        if any(p.endswith('/' + r) for r in replaced):
            continue
        by_module.setdefault(owner[p], []).append(os.path.relpath(p, modules()[owner[p]]))
    if pom:
        body = ''
        for m in sorted(by_module):
            if m in stops:
                by_module[m] = [f for f in by_module[m] if os.path.join(modules()[m], f) in explicit]
                if not by_module[m]:
                    continue
            body += f'                                <resource>\n                                    <directory>${{plugin.root}}/{m}/src</directory>\n                                    <includes>\n'
            body += ''.join(f'                                        <include>{f}</include>\n' for f in sorted(by_module[m]))
            body += '                                    </includes>\n                                </resource>\n'
        # message bundles and icons of the modules
        resources = ''
        for m in sorted(by_module):
            if m not in stops and os.path.isdir(f'{ROOT}/{m}/resources'):
                resources += f'            <resource>\n                <directory>${{plugin.root}}/{m}/resources</directory>\n                <includes>\n'
                resources += ''.join(f'                    <include>{i}</include>\n' for i in RESOURCES)
                resources += '                </includes>\n            </resource>\n'
        text = open(pom).read()
        for tag, content in (('closure', body), ('closure:resources', resources)):
            begin, end = f'<!-- {tag}:begin -->', f'<!-- {tag}:end -->'
            m = re.search(rf'([ \t]*){re.escape(begin)}.*?{re.escape(end)}', text, re.S)
            text = text[:m.start()] + f'{m.group(1)}{begin}\n{content}{m.group(1)}{end}' + text[m.end():]
        open(pom, 'w').write(text)
    why = re.search(r'why:(\S+)', os.environ.get('CLOSURE', ''))
    if why:
        for target in why.group(1).split(','):
            print(f'## {target}')
            for p in done:
                if p.endswith('/' + target):
                    while p in parent:
                        p, ident = parent[p]
                        print(f'  <- {os.path.relpath(p, ROOT)} ({ident})')
        return
    for m in sorted(by_module):
        print(f'== {m} ({len(by_module[m])}){" [stop]" if m in stops else ""}')
        if '-v' in os.environ.get('CLOSURE', ''):
            for f in sorted(by_module[m]):
                print(f'    {f}')


main()
