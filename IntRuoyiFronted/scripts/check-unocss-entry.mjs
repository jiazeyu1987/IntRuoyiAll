import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ts from 'typescript'

const root = resolve(import.meta.dirname, '..')
const mainPath = resolve(root, 'src/main.ts')
const source = readFileSync(mainPath, 'utf8')
const ast = ts.createSourceFile(mainPath, source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)

const imports = ast.statements
  .filter((statement) => ts.isImportDeclaration(statement))
  .map((statement) => ({
    module: statement.moduleSpecifier && ts.isStringLiteral(statement.moduleSpecifier)
      ? statement.moduleSpecifier.text
      : undefined,
    isSideEffect: !statement.importClause
  }))

const hasDirectUnoCssEntry = imports.some((entry) => entry.isSideEffect && entry.module === 'uno.css')
const hasIndirectUnoCssEntry = imports.some((entry) => entry.module === '@/plugins/unocss')

if (!hasDirectUnoCssEntry) {
  throw new Error('src/main.ts must directly import uno.css so the UnoCSS Vite plugin resolves the entry module.')
}

if (hasIndirectUnoCssEntry) {
  throw new Error('src/main.ts must not use the indirect @/plugins/unocss wrapper for the UnoCSS entry.')
}

console.log('UnoCSS entry import is declared directly in src/main.ts.')
