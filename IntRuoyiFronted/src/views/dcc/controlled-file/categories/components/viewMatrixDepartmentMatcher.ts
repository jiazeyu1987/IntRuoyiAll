export const CANDIDATE_SCORE_THRESHOLD = 45
export const AUTO_APPLY_SCORE_THRESHOLD = 85
export const AUTO_APPLY_LEAD_THRESHOLD = 10

export type ViewMatrixDepartmentMatchStatus =
  | 'AUTO_APPLIED'
  | 'NEEDS_CONFIRMATION'
  | 'NO_SIMILARITY'
  | 'ROOT_ONLY'
  | 'EMPTY_LABEL'
  | 'EXISTING_DEPT'

export interface ViewMatrixDepartmentLike {
  id: number
  name?: string
  parentId?: number
}

export interface ViewMatrixRuleLike {
  subjectLabel?: string
  subjectType?: string
  subjectId?: number
  subjectDepartmentPath?: string
  active?: boolean
}

export interface ViewMatrixDepartmentCandidate {
  departmentId: number
  departmentName: string
  departmentPath: string
  score: number
  rootOnly: boolean
}

export interface ViewMatrixDepartmentMatchResult {
  label: string
  status: ViewMatrixDepartmentMatchStatus
  score: number
  reason: string
  departmentId?: number
  departmentName?: string
  departmentPath?: string
  candidates: ViewMatrixDepartmentCandidate[]
}

export type ViewMatrixDepartmentRecognitionStatus =
  | 'recognized-all'
  | 'recognized-partial'
  | 'recognized-none'

export interface ViewMatrixDepartmentAutoMatchResult<T extends ViewMatrixRuleLike> {
  rules: T[]
  matches: ViewMatrixDepartmentMatchResult[]
  summary: {
    autoApplied: number
    needsConfirmation: number
    noSimilarity: number
    rootOnly: number
    existingDept: number
  }
}

const normalizeText = (value?: string) =>
  (value || '')
    .trim()
    .toLocaleLowerCase()
    .replace(/[（）()【】\[\]{}·,，.。/\\\-_—\s]/g, '')

const displayLabel = (value?: string) => (value || '').trim()

const buildDepartmentById = (departments: ViewMatrixDepartmentLike[]) =>
  new Map(departments.map((department) => [department.id, department]))

const isCompanyRoot = (department: ViewMatrixDepartmentLike, departmentById: Map<number, ViewMatrixDepartmentLike>) => {
  const parentId = department.parentId
  return !parentId || parentId <= 0 || !departmentById.has(parentId)
}

export const buildViewMatrixDepartmentPath = (
  deptId: number | undefined,
  departments: ViewMatrixDepartmentLike[]
) => {
  if (!deptId) {
    return ''
  }
  const departmentById = buildDepartmentById(departments)
  const names: string[] = []
  const visited = new Set<number>()
  let currentId: number | undefined = deptId
  while (currentId && currentId > 0 && !visited.has(currentId)) {
    visited.add(currentId)
    const department = departmentById.get(currentId)
    if (!department?.name) {
      break
    }
    names.push(department.name)
    currentId = department.parentId
  }
  return names.reverse().join('-')
}

const longestCommonSubstringLength = (left: string, right: string) => {
  if (!left || !right) {
    return 0
  }
  const dp = Array.from({ length: left.length + 1 }, () => Array(right.length + 1).fill(0))
  let longest = 0
  for (let i = 1; i <= left.length; i += 1) {
    for (let j = 1; j <= right.length; j += 1) {
      if (left[i - 1] === right[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
        longest = Math.max(longest, dp[i][j])
      }
    }
  }
  return longest
}

const scoreDepartment = (label: string, department: ViewMatrixDepartmentLike, departmentPath: string) => {
  const normalizedLabel = normalizeText(label)
  const normalizedName = normalizeText(department.name)
  const normalizedPath = normalizeText(departmentPath)
  if (!normalizedLabel || !normalizedName) {
    return 0
  }
  if (normalizedLabel === normalizedName) {
    return 100
  }
  if (normalizedPath.split('-').some((segment) => normalizeText(segment) === normalizedLabel)) {
    return 100
  }
  if (normalizedName.includes(normalizedLabel) || normalizedLabel.includes(normalizedName)) {
    const shorter = Math.min(normalizedLabel.length, normalizedName.length)
    const longer = Math.max(normalizedLabel.length, normalizedName.length)
    return Math.max(70, Math.round((shorter / longer) * 95))
  }
  const commonLength = longestCommonSubstringLength(normalizedLabel, normalizedName)
  if (commonLength < 2) {
    return 0
  }
  return Math.round((commonLength * 2 * 100) / (normalizedLabel.length + normalizedName.length))
}

export const matchDepartmentForViewMatrixLabel = (
  label: string | undefined,
  departments: ViewMatrixDepartmentLike[]
): ViewMatrixDepartmentMatchResult => {
  const trimmedLabel = displayLabel(label)
  if (!trimmedLabel) {
    return {
      label: '',
      status: 'EMPTY_LABEL',
      score: 0,
      reason: '可查阅名称为空，无法进行部门初步对应。',
      candidates: []
    }
  }
  const departmentById = buildDepartmentById(departments)
  const candidates = departments
    .map((department) => {
      const departmentPath = buildViewMatrixDepartmentPath(department.id, departments)
      return {
        departmentId: department.id,
        departmentName: department.name || `部门#${department.id}`,
        departmentPath,
        score: scoreDepartment(trimmedLabel, department, departmentPath),
        rootOnly: isCompanyRoot(department, departmentById)
      }
    })
    .filter((candidate) => candidate.score >= CANDIDATE_SCORE_THRESHOLD)
    .sort((left, right) => right.score - left.score || left.departmentPath.localeCompare(right.departmentPath))

  if (!candidates.length) {
    return {
      label: trimmedLabel,
      status: 'NO_SIMILARITY',
      score: 0,
      reason: '用户管理部门树中没有达到相似度阈值的部门。',
      candidates: []
    }
  }

  const best = candidates[0]
  const second = candidates[1]
  if (best.rootOnly) {
    return {
      label: trimmedLabel,
      status: 'ROOT_ONLY',
      score: best.score,
      reason: '最高相似项是公司根节点，公司仅作为路径上下文，不可自动选择。',
      candidates,
      departmentName: best.departmentName,
      departmentPath: best.departmentPath
    }
  }

  const uniqueLead = !second || best.score - second.score >= AUTO_APPLY_LEAD_THRESHOLD
  const exactMatch = best.score === 100
  if (exactMatch || (best.score >= AUTO_APPLY_SCORE_THRESHOLD && uniqueLead)) {
    return {
      label: trimmedLabel,
      status: 'AUTO_APPLIED',
      score: best.score,
      reason: exactMatch ? '可查阅名称与部门名称完全匹配。' : '相似度达到自动对应阈值且候选唯一领先。',
      candidates,
      departmentId: best.departmentId,
      departmentName: best.departmentName,
      departmentPath: best.departmentPath
    }
  }

  return {
    label: trimmedLabel,
    status: 'NEEDS_CONFIRMATION',
    score: best.score,
    reason: '存在相似部门，但未达到自动对应条件，请管理员确认。',
    candidates,
    departmentName: best.departmentName,
    departmentPath: best.departmentPath
  }
}

const canAutoApplyRule = (rule: ViewMatrixRuleLike, departments: ViewMatrixDepartmentLike[]) => {
  if (rule.subjectType !== 'DEPT') {
    return true
  }
  if (!rule.subjectId) {
    return true
  }
  return !departments.some((department) => department.id === rule.subjectId)
}

export const applyDepartmentAutoMatchToViewMatrixRules = <T extends ViewMatrixRuleLike>(
  rules: T[],
  departments: ViewMatrixDepartmentLike[]
): ViewMatrixDepartmentAutoMatchResult<T> => {
  const nextRules = rules.map((rule) => ({ ...rule }))
  const matches: ViewMatrixDepartmentMatchResult[] = []
  const labelMatchCache = new Map<string, ViewMatrixDepartmentMatchResult>()

  for (const rule of nextRules) {
    const label = displayLabel(rule.subjectLabel)
    const normalizedLabel = normalizeText(label)
    const existingDept = rule.subjectType === 'DEPT' && rule.subjectId
    if (existingDept && departments.some((department) => department.id === rule.subjectId)) {
      const departmentPath = buildViewMatrixDepartmentPath(rule.subjectId, departments)
      matches.push({
        label,
        status: 'EXISTING_DEPT',
        score: 100,
        reason: '该规则已有有效对应部门，系统按当前规则展示初步对应；不依赖备注字段。',
        departmentId: rule.subjectId,
        departmentName: departmentPath.split('-').at(-1),
        departmentPath,
        candidates: []
      })
      continue
    }
    const match = labelMatchCache.get(normalizedLabel) || matchDepartmentForViewMatrixLabel(label, departments)
    labelMatchCache.set(normalizedLabel, match)
    matches.push(match)
    if (match.status === 'AUTO_APPLIED' && match.departmentId && canAutoApplyRule(rule, departments)) {
      rule.subjectType = 'DEPT'
      rule.subjectId = match.departmentId
      rule.subjectDepartmentPath = match.departmentPath
    }
  }

  return {
    rules: nextRules,
    matches,
    summary: {
      autoApplied: matches.filter((match) => match.status === 'AUTO_APPLIED' || match.status === 'EXISTING_DEPT').length,
      needsConfirmation: matches.filter((match) => match.status === 'NEEDS_CONFIRMATION').length,
      noSimilarity: matches.filter((match) => match.status === 'NO_SIMILARITY').length,
      rootOnly: matches.filter((match) => match.status === 'ROOT_ONLY').length,
      existingDept: matches.filter((match) => match.status === 'EXISTING_DEPT').length
    }
  }
}

export const resolveViewMatrixDepartmentRecognitionStatus = <T extends ViewMatrixRuleLike>(
  rules: T[],
  departments: ViewMatrixDepartmentLike[]
): ViewMatrixDepartmentRecognitionStatus => {
  const activeRules = rules.filter((rule) => rule.active !== false)
  if (!activeRules.length || activeRules.some((rule) => !displayLabel(rule.subjectLabel))) {
    return 'recognized-none'
  }

  const matchResult = applyDepartmentAutoMatchToViewMatrixRules(activeRules, departments)
  const recognizedCount = matchResult.matches.filter(
    (match) => match.status === 'AUTO_APPLIED' || match.status === 'EXISTING_DEPT'
  ).length
  if (!recognizedCount) {
    return 'recognized-none'
  }
  return recognizedCount === activeRules.length ? 'recognized-all' : 'recognized-partial'
}
