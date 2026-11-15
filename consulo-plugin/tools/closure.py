"""File closure of upstream plugin classes across the modules of this repository, for filter-src include lists.

Usage: python3 consulo-plugin/tools/closure.py [--pom <pom.xml>] [--stop <module dir>]... <seed>... [@<file>]...
  seed: a fq class name or a src-relative file; @file: more arguments, one per line (# comments)
  --pom: write the include lists into the pom, between the closure:begin/end markers
Prints, per module (a directory with a non-test .iml and a src root), the files the seeds reach. A file is reached when
a reached file names one of its top-level declarations and can see it (same package, explicit import or star import).
Over-approximates (simple names, no scopes), which is fine for compiling. --stop modules are reported, not followed.
"""
import glob, os, re, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
TOP_LEVEL = re.compile(
    r'^(?:@\S+\s+)*(?:(?:public|internal|private|protected|open|abstract|sealed|data|enum|annotation|value|inline|'
    r'fun|external|expect|actual|const|final|static|operator|infix|tailrec|suspend|lateinit)\s+)*'
    r'(?:class|interface|object|typealias|fun|val|var|@interface|record)\s+(?:<[^>]*>\s*)?(?:[\w.<>?, ]+\.)?(\w+)',
    re.M)


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


def scan(path):
    text = open(path, errors='ignore').read()
    text = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
    text = re.sub(r'//[^\n]*', '', text)
    pkg = re.search(r'^package\s+([\w.]+)', text, re.M)
    names = set(TOP_LEVEL.findall(text))
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
    seeds = [a for i, a in enumerate(args) if a != '--stop' and (i == 0 or args[i - 1] != '--stop')]
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
    done = set()
    while todo:
        p = todo.pop()
        if p in done:
            continue
        done.add(p)
        if owner[p] in stops and p not in explicit:
            continue
        me = files[p]
        for ident in me['idents']:
            for o in declared.get(ident, ()):
                info = files[o]
                if info['pkg'] == me['pkg'] or f"{info['pkg']}.{ident}" in me['imports'] or info['pkg'] in me['stars']:
                    if o not in done:
                        todo.append(o)
    by_module = {}
    for p in done:
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
        text = open(pom).read()
        begin, end = '<!-- closure:begin -->', '<!-- closure:end -->'
        m = re.search(rf'([ \t]*){re.escape(begin)}.*?{re.escape(end)}', text, re.S)
        text = text[:m.start()] + f'{m.group(1)}{begin}\n{body}{m.group(1)}{end}' + text[m.end():]
        open(pom, 'w').write(text)
    for m in sorted(by_module):
        print(f'== {m} ({len(by_module[m])}){" [stop]" if m in stops else ""}')
        if '-v' in os.environ.get('CLOSURE', ''):
            for f in sorted(by_module[m]):
                print(f'    {f}')


main()
