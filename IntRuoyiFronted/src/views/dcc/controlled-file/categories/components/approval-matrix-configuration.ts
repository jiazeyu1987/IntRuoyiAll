/** 公共类型矩阵的只读配置合同；申请两份部门名单由上传申请独立保存。 */
export type MatrixIdentity = number | string
export interface MatrixConfigurationRule {
  stageType: 'SIGNOFF' | 'APPROVAL'
  active: boolean
  marker?: string | null
  subjectType?: string
  subjectId?: MatrixIdentity
  subjectLabel?: string
  subjectName?: string
  subjectDepartmentPath?: string
  remark?: string
}

export interface MatrixConfigurationSelection {
  categoryId: MatrixIdentity
  fileTypeTaxonomyId: MatrixIdentity
  routeId: MatrixIdentity
  routeVersionNo: number
  effectiveTime: string
  defaultSignoffDepartmentIds: MatrixIdentity[]
  rules: MatrixConfigurationRule[]
  remark?: string
}

const positiveId = (value: unknown): value is MatrixIdentity =>
  (typeof value === 'number' && Number.isSafeInteger(value) && value > 0) ||
  (typeof value === 'string' &&
    /^[1-9][0-9]*$/.test(value) &&
    BigInt(value) <= 9223372036854775807n)

export function copyMatrixSelection(
  selection: MatrixConfigurationSelection
): MatrixConfigurationSelection {
  if (
    !selection ||
    !positiveId(selection.categoryId) ||
    !positiveId(selection.fileTypeTaxonomyId) ||
    !positiveId(selection.routeId) ||
    !positiveId(selection.routeVersionNo)
  ) {
    throw new Error('类型审批配置身份或版本缺失')
  }
  if (!/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(selection.effectiveTime)) {
    throw new Error('类型审批配置生效时间缺失或格式无效')
  }
  const [year, month, day, hour, minute, second] = selection.effectiveTime
    .split(/[- :]/)
    .map(Number)
  const calendar = new Date(Date.UTC(year, month - 1, day, hour, minute, second))
  if (
    year < 1000 ||
    calendar.getUTCFullYear() !== year ||
    calendar.getUTCMonth() !== month - 1 ||
    calendar.getUTCDate() !== day ||
    calendar.getUTCHours() !== hour ||
    calendar.getUTCMinutes() !== minute ||
    calendar.getUTCSeconds() !== second
  ) {
    throw new Error('类型审批配置生效时间不是有效日历时间')
  }
  if (
    !Array.isArray(selection.defaultSignoffDepartmentIds) ||
    !selection.defaultSignoffDepartmentIds.length ||
    selection.defaultSignoffDepartmentIds.some((id) => !positiveId(id))
  ) {
    throw new Error('默认会签部门必须为至少一个有效部门身份')
  }
  if (
    new Set(selection.defaultSignoffDepartmentIds.map(String)).size !==
    selection.defaultSignoffDepartmentIds.length
  ) {
    throw new Error('默认会签部门不能重复')
  }
  if (!Array.isArray(selection.rules) || !selection.rules.length)
    throw new Error('类型审批配置规则缺失')
  for (const rule of selection.rules) {
    if (
      !rule ||
      !['SIGNOFF', 'APPROVAL'].includes(rule.stageType) ||
      typeof rule.active !== 'boolean'
    ) {
      throw new Error('类型审批配置阶段或启用事实无效')
    }
    if (
      rule.active &&
      (!positiveId(rule.subjectId) ||
        !['USER', 'DEPT', 'ROLE', 'POST', 'DCC_POSITION', 'POSITION'].includes(
          rule.subjectType || ''
        ))
    ) {
      throw new Error('类型审批配置启用规则主体缺失')
    }
  }
  for (const stage of ['SIGNOFF', 'APPROVAL']) {
    if (!selection.rules.some((rule) => rule.active && rule.stageType === stage)) {
      throw new Error(stage === 'SIGNOFF' ? '启用会签规则缺失' : '启用批准规则缺失')
    }
  }
  return {
    categoryId: selection.categoryId,
    fileTypeTaxonomyId: selection.fileTypeTaxonomyId,
    routeId: selection.routeId,
    routeVersionNo: selection.routeVersionNo,
    effectiveTime: selection.effectiveTime,
    remark: selection.remark,
    defaultSignoffDepartmentIds: [...selection.defaultSignoffDepartmentIds],
    rules: selection.rules.map((rule) => ({
      stageType: rule.stageType,
      active: rule.active,
      marker: rule.marker,
      subjectType: rule.subjectType,
      subjectId: rule.subjectId,
      subjectLabel: rule.subjectLabel,
      subjectName: rule.subjectName,
      subjectDepartmentPath: rule.subjectDepartmentPath,
      remark: rule.remark
    }))
  }
}

export function initializeApplicationDepartments(selection: MatrixConfigurationSelection) {
  const copy = copyMatrixSelection(selection)
  return {
    signoffDepartmentIds: [...copy.defaultSignoffDepartmentIds],
    distributionDepartmentIds: [...copy.defaultSignoffDepartmentIds]
  }
}

export interface MatrixConfigurationChange {
  field: keyof MatrixConfigurationSelection
  before: unknown
  after: unknown
}

export function compareMatrixSelection(
  saved: MatrixConfigurationSelection,
  current: MatrixConfigurationSelection
) {
  const before = copyMatrixSelection(saved),
    after = copyMatrixSelection(current)
  const changes: MatrixConfigurationChange[] = []
  for (const field of Object.keys(after) as Array<keyof MatrixConfigurationSelection>) {
    if (JSON.stringify(before[field]) !== JSON.stringify(after[field])) {
      changes.push({ field, before: before[field], after: after[field] })
    }
  }
  return { requiresConfirmation: changes.length > 0, changes }
}

/** 仅选择正式快照，调用方仍须冻结上下文并由服务器提交事务再次核对身份。 */
export function selectMatrixForApplication(input: {
  kind: 'FIRST_SUBMIT' | 'REWORK'
  selectedFileTypeTaxonomyId: MatrixIdentity
  saved?: MatrixConfigurationSelection
  current?: MatrixConfigurationSelection
}) {
  if (
    !['FIRST_SUBMIT', 'REWORK'].includes(input.kind) ||
    !positiveId(input.selectedFileTypeTaxonomyId)
  ) {
    throw new Error('申请场景或文件类型身份无效')
  }
  const sameType =
    input.saved &&
    String(input.saved.fileTypeTaxonomyId) === String(input.selectedFileTypeTaxonomyId)
  if (input.kind === 'REWORK' && sameType) {
    return {
      selection: copyMatrixSelection(input.saved!),
      requiresConfirmation: false,
      changes: []
    }
  }
  if (
    !input.current ||
    String(input.current.fileTypeTaxonomyId) !== String(input.selectedFileTypeTaxonomyId)
  ) {
    throw new Error('当前审批配置与所选文件类型不一致')
  }
  const selection = copyMatrixSelection(input.current)
  const comparison = input.saved
    ? compareMatrixSelection(input.saved, selection)
    : { requiresConfirmation: false, changes: [] }
  return { selection, ...comparison }
}

/** 原始公共配置证据，不接受申请名单覆盖后的 resolved readiness 节点。 */
export interface RawPublicMatrixRule {
  id: MatrixIdentity
  stageNo: number
  stageCode: string | null
  stageName: string
  stageOrder: number | null
  sort: number
  candidateSourceType: string
  candidateSourceId: MatrixIdentity | null
  /** 数据库存储的原CSV，不能从解析后的候选/用户集合重新拼接。 */
  candidateSourceIds: string | null
  approveMethod: string
  approveRatio: number | null
  requireAllApprovals: boolean
  required: boolean
  stageType: string | null
  subjectLabel: string | null
  marker: string | null
  subjectType: string | null
  subjectId: MatrixIdentity | null
  subjectName: string | null
  subjectDepartmentPath: string | null
  ruleRemark: string | null
}

export interface RawPublicMatrixConfiguration {
  formatVersion: 'DCC_APPROVAL_MATRIX_BASELINE_V2'
  tenantId: MatrixIdentity
  categoryId: MatrixIdentity
  fileTypeTaxonomyId: MatrixIdentity
  routeId: MatrixIdentity
  routeVersionNo: number
  actionType: string | null
  /** 正式LocalDateTime文本；null保留历史缺失，不补当前时间或默认生效。 */
  effectiveTime: string | null
  routeRemark: string | null
  nodes: RawPublicMatrixRule[]
}

type CanonicalValue = null | string | number | boolean | CanonicalValue[] | CanonicalObject
type CanonicalObject = { [key: string]: CanonicalValue }
const rawRootFields = [
  'formatVersion',
  'tenantId',
  'categoryId',
  'fileTypeTaxonomyId',
  'routeId',
  'routeVersionNo',
  'actionType',
  'effectiveTime',
  'routeRemark',
  'nodes'
] as const
const rawRuleFields = [
  'id',
  'stageNo',
  'stageCode',
  'stageName',
  'stageOrder',
  'sort',
  'candidateSourceType',
  'candidateSourceId',
  'candidateSourceIds',
  'approveMethod',
  'approveRatio',
  'requireAllApprovals',
  'required',
  'stageType',
  'subjectLabel',
  'marker',
  'subjectType',
  'subjectId',
  'subjectName',
  'subjectDepartmentPath',
  'ruleRemark'
] as const

function exactRawFields(value: unknown, expected: readonly string[], scope: string) {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    throw new Error(`${scope}字段对象缺失`)
  const raw = value as Record<string, unknown>
  const fields = Object.keys(raw)
  if (
    fields.length !== expected.length ||
    fields.some((field) => !expected.includes(field)) ||
    expected.some((field) => !Object.hasOwn(raw, field) || raw[field] === undefined)
  ) {
    throw new Error(`${scope}字段缺失或存在尚未纳入摘要合同的新字段`)
  }
  return raw
}

function rawIdentity(value: unknown, label: string): string {
  if (!positiveId(value)) throw new Error(`${label}身份必须为精确有效十进制Long`)
  return String(value)
}

function rawText(value: unknown, label: string, nullable = true): string | null {
  if (value === null && nullable) return null
  if (typeof value !== 'string') throw new Error(`${label}文本字段必须显式为字符串或允许的null`)
  for (let index = 0; index < value.length; index++) {
    const code = value.charCodeAt(index)
    if (code >= 0xd800 && code <= 0xdbff) {
      const next = value.charCodeAt(++index)
      if (!(next >= 0xdc00 && next <= 0xdfff)) throw new Error(`${label}文本存在不完整Unicode字符`)
    } else if (code >= 0xdc00 && code <= 0xdfff)
      throw new Error(`${label}文本存在不完整Unicode字符`)
  }
  return value
}

function rawInteger(value: unknown, label: string): number {
  if (
    typeof value !== 'number' ||
    !Number.isInteger(value) ||
    value < -2147483648 ||
    value > 2147483647
  ) {
    throw new Error(`${label}字段必须为正式Integer`)
  }
  return value
}

function rawBoolean(value: unknown, label: string): boolean {
  if (typeof value !== 'boolean') throw new Error(`${label}字段必须为原始布尔事实`)
  return value
}

function canonicalMatrixTime(value: unknown): string | null {
  if (value === null) return null
  if (typeof value !== 'string') throw new Error('矩阵生效时间必须来自正式LocalDateTime文本')
  const parts = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,9}))?$/.exec(value)
  if (!parts) throw new Error('矩阵生效时间格式无效')
  const [year, month, day, hour, minute, second] = parts.slice(1, 7).map(Number)
  const calendar = new Date(Date.UTC(year, month - 1, day, hour, minute, second))
  if (
    year < 1000 ||
    calendar.getUTCFullYear() !== year ||
    calendar.getUTCMonth() !== month - 1 ||
    calendar.getUTCDate() !== day ||
    calendar.getUTCHours() !== hour ||
    calendar.getUTCMinutes() !== minute ||
    calendar.getUTCSeconds() !== second
  ) {
    throw new Error('矩阵生效时间不是有效日历时间')
  }
  return `${parts[1]}-${parts[2]}-${parts[3]}T${parts[4]}:${parts[5]}:${parts[6]}.${(parts[7] || '').padEnd(9, '0')}`
}

function canonicalRawRule(value: unknown): CanonicalObject {
  const raw = exactRawFields(value, rawRuleFields, '矩阵原规则')
  const result: CanonicalObject = {
    id: rawIdentity(raw.id, '原规则')
  }
  for (const field of ['stageNo', 'sort']) result[field] = rawInteger(raw[field], field)
  for (const field of ['stageOrder', 'approveRatio'])
    result[field] = raw[field] === null ? null : rawInteger(raw[field], field)
  for (const field of ['candidateSourceId', 'subjectId'])
    result[field] = raw[field] === null ? null : rawIdentity(raw[field], field)
  for (const field of ['stageName', 'candidateSourceType', 'approveMethod'])
    result[field] = rawText(raw[field], field, false)
  for (const field of [
    'stageCode',
    'candidateSourceIds',
    'stageType',
    'subjectLabel',
    'marker',
    'subjectType',
    'subjectName',
    'subjectDepartmentPath',
    'ruleRemark'
  ]) {
    result[field] = rawText(raw[field], field)
  }
  for (const field of ['requireAllApprovals', 'required'])
    result[field] = rawBoolean(raw[field], field)
  return result
}

function compareDecimalIdentity(left: string, right: string) {
  return left.length - right.length || (left < right ? -1 : left > right ? 1 : 0)
}

function orderedCanonicalValue(value: CanonicalValue): CanonicalValue {
  if (Array.isArray(value)) return value.map(orderedCanonicalValue)
  if (value !== null && typeof value === 'object') {
    const result: CanonicalObject = {}
    for (const field of Object.keys(value).sort())
      result[field] = orderedCanonicalValue(value[field])
    return result
  }
  return value
}

/** 版本化、UTF-8 JSON预像；无trim/大小写/标记/空值补齐，不修改输入对象。 */
export function canonicalizePublicMatrix(configuration: RawPublicMatrixConfiguration): string {
  const raw = exactRawFields(configuration, rawRootFields, '公共矩阵原配置')
  if (raw.formatVersion !== 'DCC_APPROVAL_MATRIX_BASELINE_V2')
    throw new Error('公共矩阵摘要合同版本不受支持')
  const routeId = rawIdentity(raw.routeId, '公共矩阵route')
  const taxonomyId = rawIdentity(raw.fileTypeTaxonomyId, '公共矩阵taxonomy')
  const version = rawInteger(raw.routeVersionNo, '公共矩阵版本')
  if (version <= 0) throw new Error('公共矩阵版本字段必须为正整数')
  if (!Array.isArray(raw.nodes) || !raw.nodes.length) throw new Error('公共矩阵原规则字段缺失')
  const rules = raw.nodes
    .map((rule) => canonicalRawRule(rule))
    .sort((left, right) => {
      for (const field of ['stageOrder', 'sort', 'stageNo']) {
        if (left[field] === right[field]) continue
        if (left[field] === null) return 1
        if (right[field] === null) return -1
        return Number(left[field]) - Number(right[field])
      }
      return compareDecimalIdentity(String(left.id), String(right.id))
    })
  if (new Set(rules.map((rule) => rule.id)).size !== rules.length)
    throw new Error('公共矩阵原规则身份重复')
  const canonical: CanonicalObject = {
    formatVersion: 'DCC_APPROVAL_MATRIX_BASELINE_V2',
    tenantId: rawIdentity(raw.tenantId, '公共矩阵tenant'),
    categoryId: rawIdentity(raw.categoryId, '公共矩阵category'),
    fileTypeTaxonomyId: taxonomyId,
    routeId,
    routeVersionNo: version,
    actionType: rawText(raw.actionType, '公共矩阵actionType'),
    effectiveTime: canonicalMatrixTime(raw.effectiveTime),
    routeRemark: rawText(raw.routeRemark, '公共矩阵remark'),
    nodes: rules
  }
  return JSON.stringify(orderedCanonicalValue(canonical))
}

/** 注入正式WebCrypto SHA-256实现；输出64位小写hex，格式版本独立绑定，不提供降级摘要。 */
export async function buildPublicMatrixDigest(
  configuration: RawPublicMatrixConfiguration,
  crypto: Pick<SubtleCrypto, 'digest'>
): Promise<string> {
  if (!crypto || typeof crypto.digest !== 'function') throw new Error('公共矩阵SHA-256依赖不可用')
  const canonical = canonicalizePublicMatrix(configuration)
  const result = await crypto.digest('SHA-256', new TextEncoder().encode(canonical))
  if (result.byteLength !== 32) throw new Error('公共矩阵SHA-256输出长度无效')
  return Array.from(new Uint8Array(result), (byte) => byte.toString(16).padStart(2, '0')).join('')
}
