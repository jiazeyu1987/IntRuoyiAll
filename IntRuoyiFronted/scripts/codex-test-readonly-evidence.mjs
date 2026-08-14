import { spawnSync } from 'node:child_process'
import { existsSync, readdirSync } from 'node:fs'
import path from 'node:path'

const MAX_SEARCH_TERMS = 40
const MAX_EVIDENCE_FILES = 20
const MAX_EVIDENCE_LENGTH = 280000
const MAX_SNIPPET_LENGTH_PER_FILE = 20000
const NOISY_STRUCTURAL_TERMS = new Set([
  'TeamLeaderWorkbench',
  'MesProcessPoolTeamLeader',
  'ProductionEmployee',
  'employeeProfile',
  'employee-profile',
  'MesProcessPoolTeamEmployeeProfile',
  'MesTeamProductionEmployee',
  'signaturePassword'
])
const MAX_BACKEND_MODULE_DEPTH = 3
const EXCLUDED_BACKEND_DIRECTORY_NAMES = /^(?:\.git|\.idea|doc|docs|logs?|node_modules|output|target.*)$/iu
const GENERIC_TERMS = new Set([
  'API',
  '当前代码',
  '代码分析',
  '测试范围',
  '生产组长职责',
  '测试能够满足生产组长职责',
  '能够满足',
  '完整支持',
  '数据模型',
  '测试证据',
  '职责描述',
  '逐项拆解职责描述',
  '实时只读代码证据',
  '判定规则',
  '关键动作',
  '状态链路',
  '只支持相邻功能',
  '局部对象',
  '部分链路',
  '判定为不通过',
  '代码级设计证据',
  '已支持证据',
  '缺失证据'
])

export function resolveCodeReadonlySearchTerms(task) {
  const checkpoints = task.checkpoints || []
  const preferredValues = [
    String(task.caseName || '').split('-').at(-1),
    ...checkpoints.map((checkpoint) => checkpoint.name)
  ]
  const businessText = [
    task.testDataText,
    ...checkpoints.map((checkpoint) => checkpoint.expectedText)
  ]
    .filter(Boolean)
    .join('\n')
  const businessFragments = businessText.split(/[\s\-:：,，。；;、/|（）()]+/u)
  const semanticAliases = resolveSemanticAliases([
    task.caseName,
    task.methodText,
    businessText
  ].filter(Boolean).join('\n'))
  const semanticAliasSet = new Set(semanticAliases)
  const derivedTerms = businessFragments.flatMap((fragment) => {
    const parts = fragment.split(/[和或与及并]/u)
    return parts.flatMap((part) => {
      const normalized = part.replace(/^(可|能|支持|允许|维护|新增|修改|设置|启用)/u, '')
      return normalized && normalized !== part ? [part, normalized] : [part]
    })
  })
  const supportingFragments = [task.caseName, task.methodText]
    .filter(Boolean)
    .join('\n')
    .split(/[\s\-:：,，。；;、/|（）()]+/u)
  const terms = []
  for (const value of [
    ...preferredValues,
    ...semanticAliases,
    ...derivedTerms,
    ...businessFragments,
    ...supportingFragments
  ]) {
    const term = String(value || '').trim()
    const maxLength = semanticAliasSet.has(term) ? 64 : 24
    if (!isSearchTerm(term, maxLength) || GENERIC_TERMS.has(term) || terms.includes(term)) {
      continue
    }
    terms.push(term)
    if (terms.length >= MAX_SEARCH_TERMS) {
      break
    }
  }
  return terms
}

export function collectCodeReadonlyEvidence(task, projectRoot) {
  const searchRoots = resolveCodeReadonlySearchRoots(projectRoot)
  if (searchRoots.length === 0) {
    throw new Error(`CODE_READONLY source roots are missing under ${projectRoot}`)
  }
  const terms = resolveCodeReadonlySearchTerms(task)
  if (terms.length === 0) {
    throw new Error('CODE_READONLY task does not contain searchable business terms')
  }
  const pattern = terms.map(escapeRegex).join('|')
  const businessSnippetTerms = terms.filter((term) => !NOISY_STRUCTURAL_TERMS.has(term))
  const fileResult = runRg(projectRoot, [
    '-l',
    '--ignore-case',
    '--no-messages',
    '--color',
    'never',
    '--glob',
    '!**/node_modules/**',
    '--glob',
    '!**/target*/**',
    '--glob',
    '!**/.runtime/**',
    '--glob',
    '!**/output/**',
    '-e',
    pattern,
    ...searchRoots
  ], true)
  const matchedFiles = fileResult
    .split(/\r?\n/u)
    .map((filePath) => normalizeRelativePath(projectRoot, filePath))
    .filter(Boolean)
    .filter(isAllowedEvidenceFile)
  const selectedFiles = selectEvidenceFiles([...new Set(matchedFiles)])
  if (selectedFiles.length === 0) {
    return [
      `搜索词：${terms.join(' | ')}`,
      '白名单源码目录中没有匹配文件。'
    ].join('\n')
  }
  const snippetResult = selectedFiles
    .map((filePath) => {
      const snippetTerms = /^IntRuoyiFronted\/src\/router\//u.test(filePath)
        ? terms
        : businessSnippetTerms
      const result = runRg(projectRoot, [
      '-n',
      '-C',
      '4',
      '--max-count',
      '20',
      '--ignore-case',
      '--with-filename',
      '--no-messages',
      '--color',
      'never',
      '-e',
      snippetTerms.map(escapeRegex).join('|'),
      filePath
      ], true)
      return (result || `${filePath}: 仅通过结构标识入选，未命中业务行为片段。`)
        .slice(0, MAX_SNIPPET_LENGTH_PER_FILE)
    })
    .join('\n--\n')
  return [
    `搜索词：${terms.join(' | ')}`,
    '证据文件：',
    ...selectedFiles.map((filePath) => `- ${filePath}`),
    '实时 rg 片段：',
    snippetResult || '匹配文件存在，但限定上下文未返回内容。'
  ].join('\n').slice(0, MAX_EVIDENCE_LENGTH)
}

function resolveSemanticAliases(sourceText) {
  const aliases = []
  if (/生产组长/u.test(sourceText)) {
    aliases.push('TeamLeaderWorkbench', 'MesProcessPoolTeamLeader')
  }
  if (/生产人员|正式员工|正式工|临时工|员工管理|人员管理/u.test(sourceText)) {
    aliases.push(
      'ProductionEmployee',
      'employeeProfile',
      'employee-profile',
      'employee_profile',
      'MesProcessPoolTeamEmployeeProfile',
      'MesTeamProductionEmployee',
      'getProductionPersonnelList',
      'linkFormalTeamEmployee',
      'employee-profile/list',
      'employee-profile/formal/link'
    )
  }
  if (/临时工/u.test(sourceText)) {
    aliases.push('temporaryEmployee', 'createTemporaryEmployee', 'createTemporaryTeamEmployee', 'employee-profile/temporary/create')
  }
  if (/密码/u.test(sourceText)) {
    aliases.push(
      'signaturePassword',
      'signaturePasswordHash',
      'resetTemporaryEmployeeSignaturePassword',
      'resetTemporaryTeamEmployeeSignaturePassword',
      'employee-profile/temp-signature-password/reset',
      'signature_password_hash'
    )
  }
  if (/启用|禁用/u.test(sourceText)) {
    aliases.push('updateEmployeeStatus', 'updateTeamEmployeeStatus', 'updateEmployeeEnabled', 'employee-profile/status/update')
  }
  if (/数据模型|模型|持久化|临时工|正式员工|正式工/u.test(sourceText)) {
    aliases.push('20260805_mes_process_pool_production_personnel', 'mes_pro_process_pool_team_employee_profile')
  }
  return aliases
}

export function resolveCodeReadonlySearchRoots(projectRoot) {
  const searchRoots = [
    'IntRuoyiFronted/src',
    'IntRuoyiFronted/tests/e2e'
  ].filter((relativePath) => existsSync(path.join(projectRoot, relativePath)))
  const backendRoot = path.join(projectRoot, 'IntRuoyiBackend')
  if (existsSync(backendRoot)) {
    collectBackendSourceRoots(projectRoot, backendRoot, 0, searchRoots)
    const sqlRoot = 'IntRuoyiBackend/sql/mysql'
    if (existsSync(path.join(projectRoot, sqlRoot))) {
      searchRoots.push(sqlRoot)
    }
  }
  return searchRoots
}

function collectBackendSourceRoots(projectRoot, currentDirectory, depth, searchRoots) {
  if (depth > MAX_BACKEND_MODULE_DEPTH) return
  for (const sourceDirectory of ['src/main', 'src/test']) {
    const absoluteSourceDirectory = path.join(currentDirectory, sourceDirectory)
    if (existsSync(absoluteSourceDirectory)) {
      searchRoots.push(normalizeRelativePath(projectRoot, absoluteSourceDirectory))
    }
  }
  if (depth === MAX_BACKEND_MODULE_DEPTH) return
  const childDirectories = readdirSync(currentDirectory, { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && !EXCLUDED_BACKEND_DIRECTORY_NAMES.test(entry.name))
    .map((entry) => path.join(currentDirectory, entry.name))
  for (const childDirectory of childDirectories) {
    collectBackendSourceRoots(projectRoot, childDirectory, depth + 1, searchRoots)
  }
}

function isSearchTerm(term, maxLength = 24) {
  if (term.length < 3 || term.length > maxLength || /^\d+$/u.test(term)) {
    return false
  }
  return /[\p{Script=Han}A-Za-z]/u.test(term)
}

function escapeRegex(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&')
}

function runRg(projectRoot, args, allowNoMatches) {
  const result = spawnSync('rg', args, {
    cwd: projectRoot,
    encoding: 'utf8',
    windowsHide: true,
    maxBuffer: 4 * 1024 * 1024
  })
  if (result.error) {
    throw new Error(`CODE_READONLY requires rg: ${result.error.message}`)
  }
  if (result.status !== 0 && !(allowNoMatches && result.status === 1)) {
    throw new Error(`CODE_READONLY rg failed with exit ${result.status}: ${String(result.stderr || '').trim()}`)
  }
  return String(result.stdout || '').trim()
}

function normalizeRelativePath(projectRoot, filePath) {
  if (!filePath) return ''
  const relativePath = path.isAbsolute(filePath) ? path.relative(projectRoot, filePath) : filePath
  return relativePath.replace(/\\/gu, '/')
}

function isAllowedEvidenceFile(filePath) {
  return /^IntRuoyiFronted\/(src|tests\/e2e)\//u.test(filePath)
    || /^IntRuoyiBackend\/.*\/src\/(main|test)\//u.test(filePath)
    || /^IntRuoyiBackend\/sql\/mysql\//u.test(filePath)
}

function selectEvidenceFiles(files) {
  const categories = [
    { pattern: /^IntRuoyiFronted\/src\/views\//u, limit: 1 },
    { pattern: /^IntRuoyiFronted\/src\/api\//u, limit: 1 },
    { pattern: /^IntRuoyiFronted\/src\/router\//u, limit: 1 },
    { pattern: /^IntRuoyiBackend\/.*\/src\/main\/.*\/controller\/.*Controller\.java$/u, limit: 1 },
    { pattern: /^IntRuoyiBackend\/.*\/src\/main\/.*\/service\/.*Service(?:Impl)?\.java$/u, limit: 3 },
    { pattern: /^IntRuoyiBackend\/.*\/src\/main\/.*\/dal\//u, limit: 2 },
    { pattern: /^IntRuoyiBackend\/.*\/src\/main\//u, limit: 2 },
    { pattern: /^IntRuoyiFronted\/tests\/e2e\//u, limit: 3 },
    { pattern: /^IntRuoyiBackend\/.*\/src\/test\//u, limit: 4 },
    { pattern: /^IntRuoyiBackend\/sql\/mysql\//u, limit: 1 }
  ]
  const selected = []
  for (const category of categories) {
    const candidates = files
      .filter((filePath) => category.pattern.test(filePath) && !selected.includes(filePath))
      .sort(compareEvidencePaths)
      .slice(0, category.limit)
    for (const candidate of candidates) {
      if (!selected.includes(candidate) && selected.length < MAX_EVIDENCE_FILES) {
        selected.push(candidate)
      }
    }
  }
  return selected
}

function compareEvidencePaths(left, right) {
  return evidencePathScore(left) - evidencePathScore(right) || left.localeCompare(right)
}

function evidencePathScore(filePath) {
  let score = 0
  if (/^IntRuoyiFronted\/tests\/e2e\/production-personnel-management-real\.e2e\.js$/iu.test(filePath)) score -= 30
  if (/20260805_mes_process_pool_production_personnel/iu.test(filePath)) score -= 30
  if (/TeamLeaderWorkbench|ProductionPersonnel|MesProcessPoolTeamLeaderController|TeamLeaderRuntimeConfigService|TeamEmployeeProfile(?:DO|Mapper)/iu.test(filePath)) score -= 24
  if (/^IntRuoyiFronted\/src\/views\//u.test(filePath)) score -= 20
  if (/^IntRuoyiBackend\/yudao-module-mes\//u.test(filePath)) score -= 15
  if (/\/(?:controller|service|dal)\//u.test(filePath)) score -= 8
  if (/production.*personnel|employee|personnel|leader|profile|staff|worker|人员|员工/iu.test(filePath)) score -= 6
  if (/^IntRuoyiFronted\/tests\/e2e\/production-personnel/iu.test(filePath)) score -= 12
  if (/\.(?:spec|test)\.[cm]?[jt]s$/iu.test(filePath) && !/\/tests\//u.test(filePath)) score += 8
  return score
}
