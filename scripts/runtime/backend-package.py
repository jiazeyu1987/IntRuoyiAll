"""Build and verify the local backend package against its exact source inputs."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def fingerprint(root):
    import xml.etree.ElementTree as ET
    entries = []
    visited = set()
    def module(directory):
        directory = directory.resolve()
        if directory in visited:
            return
        directory.relative_to(root.resolve())
        visited.add(directory)
        pom = directory / 'pom.xml'
        entries.append((pom.relative_to(root).as_posix(), digest(pom)))
        for folder in (directory / 'src/main', directory / '.mvn'):
            if folder.is_dir():
                for base, dirs, files in os.walk(folder):
                    dirs.sort()
                    for name in sorted(files):
                        path = Path(base, name)
                        entries.append((path.relative_to(root).as_posix(), digest(path)))
        for child in ET.parse(pom).getroot().iter():
            if child.tag.split('}')[-1] == 'module' and child.text:
                module(directory / child.text.strip())
    module(root)
    if not entries:
        raise RuntimeError('No backend source inputs found')
    return hashlib.sha256(json.dumps(sorted(entries), ensure_ascii=False).encode()).hexdigest()


def verify(root, jar):
    manifest = jar.with_suffix('.provenance.json')
    if not manifest.is_file():
        raise RuntimeError('Missing package provenance; rebuild with backend-package.py build')
    data = json.loads(manifest.read_text(encoding='utf-8'))
    if data.get('version') != 1 or data.get('sourceSha256') != fingerprint(root):
        raise RuntimeError('Backend source differs from package; rebuild before startup')
    if data.get('jarSha256') != digest(jar):
        raise RuntimeError('Backend package differs from verified build; rebuild before startup')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['build', 'verify'])
    parser.add_argument('--root', type=Path, required=True)
    args = parser.parse_args()
    root = args.root.resolve()
    jar = root / 'yudao-server/target/yudao-server-exec.jar'
    if args.action == 'build':
        before = fingerprint(root)
        manifest = jar.with_suffix('.provenance.json')
        manifest.unlink(missing_ok=True)
        subprocess.run(['mvn.cmd' if os.name == 'nt' else 'mvn', '-pl', 'yudao-server', '-am', '-DskipTests', 'clean', 'package'], cwd=root, check=True)
        if fingerprint(root) != before:
            raise RuntimeError('Source changed during build; package cannot be certified')
        data = {'version': 1, 'sourceSha256': before, 'jarSha256': digest(jar)}
        manifest.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
    verify(root, jar)
    print('Backend package source and artifact verified')


if __name__ == '__main__':
    main()
