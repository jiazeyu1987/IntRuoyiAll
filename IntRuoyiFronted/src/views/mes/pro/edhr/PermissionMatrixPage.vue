<template>
  <ContentWrap>
    <div class="edhr-permission-matrix">
      <div class="edhr-permission-matrix__toolbar">
        <div class="edhr-permission-matrix__section-head">
          <div>
            <div class="edhr-permission-matrix__section-title">权限范围</div>
          </div>
          <el-tag effect="plain">版本 {{ scopeForm.version || '新建' }}</el-tag>
        </div>
        <el-form :inline="true" :model="scopeForm" class="edhr-permission-matrix__form">
          <el-form-item label="权限范围ID">
            <el-input
              v-model="scopeForm.scopeId"
              clearable
              class="!w-130px"
            />
          </el-form-item>
          <el-form-item label="范围名称">
            <el-input v-model="scopeForm.scopeName" clearable class="!w-220px" />
          </el-form-item>
          <el-form-item label="对象类型">
            <el-input v-model="scopeForm.objectType" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="对象ID">
            <el-input v-model="scopeForm.objectId" clearable class="!w-180px" />
          </el-form-item>
          <el-form-item label="父范围ID">
            <el-input
              v-model="scopeForm.parentScopeId"
              clearable
              class="!w-130px"
            />
          </el-form-item>
          <el-form-item>
            <el-button :loading="detailLoading" @click="loadPermissionScope">读取规则</el-button>
            <el-button type="primary" :loading="saving" @click="savePermissionScope">保存规则</el-button>
            <el-button :loading="loading" @click="evaluatePermissions">评估</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div class="edhr-permission-matrix__rule-surface">
        <div class="edhr-permission-matrix__section-head">
          <div>
            <div class="edhr-permission-matrix__section-title">权限规则</div>
          </div>
          <el-button type="primary" link @click="addRule">
            <Icon icon="ep:plus" class="mr-5px" /> 添加规则
          </el-button>
        </div>
        <el-table
          :data="ruleRows"
          border
          stripe
          class="edhr-permission-matrix__rule-table"
          :show-overflow-tooltip="true"
        >
          <el-table-column label="主体类型" width="130">
            <template #default="{ row }">
              <el-select v-model="row.subjectType" @change="handleSubjectTypeChange(row)">
                <el-option label="用户" value="USER" />
                <el-option label="角色" value="ROLE" />
                <el-option label="部门" value="DEPT" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="主体" min-width="230">
            <template #default="{ row }">
              <el-select
                v-model="row.subjectId"
                filterable
                clearable
                :loading="subjectOptionsLoading"
                placeholder="请选择主体"
                class="!w-210px"
              >
                <el-option
                  v-for="option in getSubjectOptions(row.subjectType)"
                  :key="`${row.subjectType}-${option.id}`"
                  :label="formatSubjectOption(row.subjectType, option)"
                  :value="option.id"
                />
              </el-select>
              <div class="edhr-permission-matrix__subject-summary">
                {{ formatSubjectSummary(row) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="能力" width="160">
            <template #default="{ row }">
              <el-select v-model="row.ability">
                <el-option
                  v-for="option in abilityOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="决策" width="120">
            <template #default="{ row }">
              <el-select v-model="row.decision">
                <el-option label="允许" value="ALLOW" />
                <el-option label="拒绝" value="DENY" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.priority" :min="1" :controls="false" class="!w-90px" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-select v-model="row.status">
                <el-option label="启用" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="生效时间" width="190">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.effectiveFrom"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="!w-170px"
              />
            </template>
          </el-table-column>
          <el-table-column label="失效时间" width="190">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.effectiveTo"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="!w-170px"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeRule($index)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="edhr-permission-matrix__evaluate-toolbar">
        <div class="edhr-permission-matrix__section-head">
          <div>
            <div class="edhr-permission-matrix__section-title">评估条件</div>
          </div>
        </div>
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="执行ID">
            <el-input v-model="queryParams.executionId" clearable class="!w-130px" />
          </el-form-item>
          <el-form-item label="批次ID">
            <el-input
              v-model="queryParams.batchExecutionId"
              clearable
              class="!w-130px"
            />
          </el-form-item>
          <el-form-item label="评估能力">
            <el-checkbox-group v-model="selectedAbilities" class="edhr-permission-matrix__ability-group">
              <el-checkbox-button
                v-for="option in abilityOptions"
                :key="option.value"
                :label="option.value"
              >
                {{ option.label }}
              </el-checkbox-button>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item class="edhr-permission-matrix__advanced-evaluate">
            <el-collapse v-model="permissionAdvancedEvaluateNames">
              <el-collapse-item title="高级评估条件" name="advanced-evaluate">
                <div class="edhr-permission-matrix__advanced-evaluate-grid">
                  <el-form-item label="路线ID">
                    <el-input
                      v-model="queryParams.routeId"
                      clearable
                      class="!w-130px"
                    />
                  </el-form-item>
                  <el-form-item label="工序ID">
                    <el-input
                      v-model="queryParams.routeProcessId"
                      clearable
                      class="!w-130px"
                    />
                  </el-form-item>
                  <el-form-item label="记录表ID">
                    <el-input v-model="queryParams.reportId" clearable class="!w-170px" />
                  </el-form-item>
                  <el-form-item label="记录类型">
                    <el-select v-model="queryParams.recordCategory" clearable class="!w-150px">
                      <el-option label="批记录表" value="BATCH_RECORD" />
                      <el-option label="内部记录表" value="INTERNAL_RECORD" />
                    </el-select>
                  </el-form-item>
                </div>
              </el-collapse-item>
            </el-collapse>
          </el-form-item>
        </el-form>
      </div>

      <div class="edhr-permission-matrix__result">
        <el-empty v-if="!result" description="保存或读取对象权限规则后，可对当前登录用户执行后端评估" />
        <template v-else>
          <div class="edhr-permission-matrix__section-head">
            <div>
              <div class="edhr-permission-matrix__section-title">评估结论</div>
            </div>
          </div>

          <div class="edhr-permission-matrix__result-summary">
            <div class="edhr-permission-matrix__summary-item">
              <span>评估对象</span>
              <strong>{{ formatObjectSummary(result.objectType, result.objectId) }}</strong>
            </div>
            <div class="edhr-permission-matrix__summary-item">
              <span>允许能力</span>
              <strong>{{ decisionSummary.allowCount }}</strong>
            </div>
            <div class="edhr-permission-matrix__summary-item">
              <span>拒绝能力</span>
              <strong>{{ decisionSummary.denyCount }}</strong>
            </div>
            <div class="edhr-permission-matrix__summary-item">
              <span>命中规则</span>
              <strong>{{ decisionSummary.matchedRuleCount }}</strong>
            </div>
          </div>

          <el-table :data="decisionRows" stripe class="edhr-permission-matrix__decision-table">
            <el-table-column type="expand" width="40">
              <template #default="{ row }">
                <div class="edhr-permission-matrix__decision-evidence">
                  <div class="edhr-permission-matrix__decision-evidence-title">能力证据</div>
                  <div class="edhr-permission-matrix__evidence-grid">
                    <div class="edhr-permission-matrix__evidence-item">
                      <span>能力</span>
                      <strong>{{ row.label }}</strong>
                    </div>
                    <div class="edhr-permission-matrix__evidence-item">
                      <span>后端决策</span>
                      <strong>{{ formatDecisionLabel(row.decision) }}</strong>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="能力" prop="label" min-width="160" />
            <el-table-column label="后端决策" width="140">
              <template #default="{ row }">
                <el-tag :type="formatDecisionType(row.decision)">
                  {{ formatDecisionLabel(row.decision) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <el-collapse v-model="resultEvidenceNames" class="edhr-permission-matrix__evidence-collapse">
            <el-collapse-item title="评估证据" name="result-evidence">
              <div class="edhr-permission-matrix__evidence-grid">
                <div class="edhr-permission-matrix__evidence-item">
                  <span>权限范围ID</span>
                  <strong>{{ result.scopeId || '--' }}</strong>
                </div>
                <div class="edhr-permission-matrix__evidence-item">
                  <span>对象类型</span>
                  <strong>{{ resolveOperationAuditObjectTypeLabel(result.objectType) }}</strong>
                </div>
                <div class="edhr-permission-matrix__evidence-item">
                  <span>对象ID</span>
                  <strong>{{ result.objectId || '--' }}</strong>
                </div>
                <div class="edhr-permission-matrix__evidence-item">
                  <span>匹配规则</span>
                  <strong>{{ formatMatchedRuleIds(result.matchedRuleIds) }}</strong>
                </div>
                <div class="edhr-permission-matrix__evidence-item">
                  <span>审计事件ID</span>
                  <strong>{{ result.operationAuditEventId || '--' }}</strong>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </template>
      </div>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  EdhrPermissionApi,
  type EdhrPermissionAbility,
  type EdhrPermissionDecision,
  type EdhrPermissionEvaluateRespVO,
  type EdhrPermissionRuleSaveVO,
  type EdhrPermissionRuleStatus,
  type EdhrPermissionScopeDetailRespVO,
  type EdhrPermissionSubjectType
} from '@/api/mes/pro/edhr/permission'
import type { EdhrRecordCategory } from '@/api/mes/pro/edhr/batchExecution'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { getSimpleRoleList, type RoleVO } from '@/api/system/role'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { resolveOperationAuditObjectTypeLabel } from '@/views/mes/pro/edhr/shared/releaseCheckPresentation'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'MesProFeedbackEdhrPermissionMatrix' })

type RuleDraft = {
  subjectType: EdhrPermissionSubjectType
  subjectId?: number
  ability: EdhrPermissionAbility
  decision: EdhrPermissionDecision
  priority: number
  effectiveFrom?: string
  effectiveTo?: string
  status: EdhrPermissionRuleStatus
}

type SubjectOption = UserVO | RoleVO | DeptVO

const route = useRoute()
const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const subjectOptionsLoading = ref(false)
const loadError = ref('')
const result = ref<EdhrPermissionEvaluateRespVO>()
const resultEvidenceNames = ref<string[]>([])
const permissionAdvancedEvaluateNames = ref<string[]>([])
const userOptions = ref<UserVO[]>([])
const roleOptions = ref<RoleVO[]>([])
const deptOptions = ref<DeptVO[]>([])

const abilityOptions: Array<{ label: string; value: EdhrPermissionAbility }> = [
  { label: '查看', value: 'VIEW' },
  { label: '填写', value: 'FILL' },
  { label: '签名', value: 'SIGN' },
  { label: '审批', value: 'APPROVE' },
  { label: '归档', value: 'ARCHIVE' },
  { label: '审计查看', value: 'AUDIT_VIEW' },
  { label: '路线编辑', value: 'ROUTE_EDIT' },
  { label: '权限管理', value: 'PERMISSION_ADMIN' }
]

const scopeForm = reactive({
  scopeId: parsePositiveRouteQueryId(route.query.permissionScopeId || route.query.scopeId) || undefined,
  scopeName: typeof route.query.scopeName === 'string' ? route.query.scopeName : '',
  objectType: typeof route.query.objectType === 'string' ? route.query.objectType : '',
  objectId: typeof route.query.objectId === 'string' ? route.query.objectId : '',
  parentScopeId: parsePositiveRouteQueryId(route.query.parentScopeId) || undefined,
  version: undefined as number | undefined
})

const queryParams = reactive({
  batchExecutionId: parsePositiveRouteQueryId(route.query.batchExecutionId) || undefined,
  executionId: parsePositiveRouteQueryId(route.query.executionId) || undefined,
  workTaskId: parsePositiveRouteQueryId(route.query.workTaskId) || undefined,
  routeId: parsePositiveRouteQueryId(route.query.routeId) || undefined,
  routeProcessId: parsePositiveRouteQueryId(route.query.routeProcessId) || undefined,
  reportId: typeof route.query.reportId === 'string' ? route.query.reportId : '',
  recordCategory: undefined as EdhrRecordCategory | undefined
})

const selectedAbilities = ref<EdhrPermissionAbility[]>(abilityOptions.map((option) => option.value))

const defaultRule = (ability: EdhrPermissionAbility = 'VIEW', rowIndex = 0): RuleDraft => ({
  subjectType: 'USER',
  subjectId: undefined,
  ability,
  decision: 'ALLOW',
  priority: rowIndex + 10,
  status: 'ENABLED'
})

const ruleRows = ref<RuleDraft[]>([defaultRule()])

const cleanText = (value?: string) => value?.trim() || ''

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const formatSubjectTypeLabel = (subjectType: EdhrPermissionSubjectType) => {
  if (subjectType === 'USER') return '用户'
  if (subjectType === 'ROLE') return '角色'
  return '部门'
}

const getSubjectOptions = (subjectType: EdhrPermissionSubjectType): SubjectOption[] => {
  if (subjectType === 'USER') return userOptions.value
  if (subjectType === 'ROLE') return roleOptions.value
  return deptOptions.value
}

const formatSubjectOption = (subjectType: EdhrPermissionSubjectType, option: SubjectOption) => {
  if (subjectType === 'USER') {
    const user = option as UserVO
    const nickname = cleanText(user.nickname)
    const username = cleanText(user.username)
    const deptName = cleanText(user.deptName)
    const primaryName = nickname || username || `用户 #${user.id}`
    const accountName = username && username !== primaryName ? `（${username}）` : ''
    const deptSuffix = deptName ? ` - ${deptName}` : ''
    return `${primaryName}${accountName}${deptSuffix}`
  }
  if (subjectType === 'ROLE') {
    const role = option as RoleVO
    const name = cleanText(role.name) || `角色 #${role.id}`
    const code = cleanText(role.code)
    return code ? `${name}（${code}）` : name
  }
  const dept = option as DeptVO
  return cleanText(dept.name) || `部门 #${dept.id}`
}

const findSubjectOption = (subjectType: EdhrPermissionSubjectType, subjectId?: number) => {
  if (!subjectId) return undefined
  return getSubjectOptions(subjectType).find((option) => Number(option.id) === Number(subjectId))
}

const formatSubjectSummary = (row: RuleDraft) => {
  const subjectTypeLabel = formatSubjectTypeLabel(row.subjectType)
  if (!row.subjectId) return `未选择${subjectTypeLabel}主体`
  const option = findSubjectOption(row.subjectType, row.subjectId)
  if (!option) return `${subjectTypeLabel} #${row.subjectId} 未在当前选项中解析`
  return `${subjectTypeLabel}：${formatSubjectOption(row.subjectType, option)} #${row.subjectId}`
}

const handleSubjectTypeChange = (row: RuleDraft) => {
  row.subjectId = undefined
}

const loadSubjectOptions = async () => {
  subjectOptionsLoading.value = true
  try {
    const [users, roles, depts] = await Promise.all([
      getSimpleUserList(),
      getSimpleRoleList(),
      getSimpleDeptList()
    ])
    userOptions.value = users
    roleOptions.value = roles
    deptOptions.value = depts
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '加载权限主体选项失败。')
    throw error
  } finally {
    subjectOptionsLoading.value = false
  }
}

const assertScopeContext = () => {
  if (!scopeForm.scopeId && (!scopeForm.objectType.trim() || !scopeForm.objectId.trim())) {
    throw new Error('权限范围ID或对象类型/对象ID必须至少填写一组。')
  }
}

const assertSaveContext = () => {
  if (!scopeForm.scopeName.trim()) {
    throw new Error('权限范围名称不能为空。')
  }
  if (!scopeForm.objectType.trim() || !scopeForm.objectId.trim()) {
    throw new Error('对象类型和对象ID必须填写后才能保存对象级权限规则。')
  }
}

const assertPermissionContext = () => {
  if (!selectedAbilities.value.length) {
    throw new Error('至少选择一个对象能力后才能评估权限。')
  }
  assertScopeContext()
}

const normalizeRule = (rule: RuleDraft, index: number): EdhrPermissionRuleSaveVO => {
  if (!rule.subjectId || rule.subjectId <= 0) {
    throw new Error(`第 ${index + 1} 条权限规则缺少真实主体ID。`)
  }
  return {
    subjectType: rule.subjectType,
    subjectId: rule.subjectId,
    ability: rule.ability,
    decision: rule.decision,
    priority: rule.priority || 100,
    effectiveFrom: rule.effectiveFrom || undefined,
    effectiveTo: rule.effectiveTo || undefined,
    status: rule.status || 'ENABLED'
  }
}

const applyDetail = (detail: EdhrPermissionScopeDetailRespVO) => {
  scopeForm.scopeId = detail.scopeId == null ? undefined : String(detail.scopeId)
  scopeForm.scopeName = detail.scopeName || scopeForm.scopeName
  scopeForm.objectType = detail.objectType || scopeForm.objectType
  scopeForm.objectId = detail.objectId || scopeForm.objectId
  scopeForm.parentScopeId = detail.parentScopeId == null ? undefined : String(detail.parentScopeId)
  scopeForm.version = detail.version
  ruleRows.value = (detail.rules || []).map((rule) => ({
    subjectType: rule.subjectType,
    subjectId: rule.subjectId,
    ability: rule.ability,
    decision: rule.decision,
    priority: rule.priority || 100,
    effectiveFrom: rule.effectiveFrom,
    effectiveTo: rule.effectiveTo,
    status: rule.status || 'ENABLED'
  }))
  if (!ruleRows.value.length) {
    ruleRows.value = [defaultRule()]
  }
}

const loadPermissionScope = async () => {
  detailLoading.value = true
  loadError.value = ''
  try {
    assertScopeContext()
    const detail = await EdhrPermissionApi.get({
      scopeId: parsePositiveRouteQueryId(scopeForm.scopeId) || undefined,
      objectType: scopeForm.objectType.trim() || undefined,
      objectId: scopeForm.objectId.trim() || undefined
    })
    applyDetail(detail)
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '读取 eDHR 对象级权限规则失败。')
  } finally {
    detailLoading.value = false
  }
}

const savePermissionScope = async () => {
  saving.value = true
  loadError.value = ''
  try {
    assertSaveContext()
    const detail = await EdhrPermissionApi.save({
      scopeId: parsePositiveRouteQueryId(scopeForm.scopeId) || undefined,
      scopeName: scopeForm.scopeName.trim(),
      objectType: scopeForm.objectType.trim(),
      objectId: scopeForm.objectId.trim(),
      parentScopeId: parsePositiveRouteQueryId(scopeForm.parentScopeId) || undefined,
      expectedVersion: scopeForm.version,
      rules: ruleRows.value.map(normalizeRule)
    })
    applyDetail(detail)
    message.success('对象级权限规则保存成功')
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '保存 eDHR 对象级权限规则失败。')
  } finally {
    saving.value = false
  }
}

const evaluatePermissions = async () => {
  loading.value = true
  loadError.value = ''
  try {
    assertPermissionContext()
    result.value = await EdhrPermissionApi.evaluate({
      scopeId: parsePositiveRouteQueryId(scopeForm.scopeId) || undefined,
      objectType: scopeForm.objectType.trim() || undefined,
      objectId: scopeForm.objectId.trim() || undefined,
      batchExecutionId: parsePositiveRouteQueryId(queryParams.batchExecutionId) || undefined,
      executionId: parsePositiveRouteQueryId(queryParams.executionId) || undefined,
      workTaskId: parsePositiveRouteQueryId(queryParams.workTaskId) || undefined,
      routeId: parsePositiveRouteQueryId(queryParams.routeId) || undefined,
      routeProcessId: parsePositiveRouteQueryId(queryParams.routeProcessId) || undefined,
      reportId: queryParams.reportId.trim() || undefined,
      recordCategory: queryParams.recordCategory,
      abilities: selectedAbilities.value
    })
  } catch (error) {
    result.value = undefined
    loadError.value = resolveErrorMessage(error, 'eDHR 对象级权限评估失败。')
  } finally {
    loading.value = false
  }
}

const decisionRows = computed(() =>
  abilityOptions
    .filter((option) => selectedAbilities.value.includes(option.value))
    .map((option) => ({
      ability: option.value,
      label: option.label,
      decision: result.value?.decisions?.[option.value]
    }))
)

const decisionSummary = computed(() => {
  const rows = decisionRows.value
  return {
    allowCount: rows.filter((row) => row.decision === 'ALLOW').length,
    denyCount: rows.filter((row) => row.decision === 'DENY').length,
    matchedRuleCount: result.value?.matchedRuleIds?.length || 0
  }
})

const formatDecisionLabel = (decision?: EdhrPermissionDecision) => {
  if (decision === 'ALLOW') return '允许'
  if (decision === 'DENY') return '拒绝'
  return '未返回'
}

const formatDecisionType = (decision?: EdhrPermissionDecision) => {
  if (decision === 'ALLOW') return 'success'
  if (decision === 'DENY') return 'danger'
  return 'warning'
}

const formatMatchedRuleIds = (matchedRuleIds?: number[]) => {
  if (!matchedRuleIds?.length) return '--'
  return matchedRuleIds.join(', ')
}

const formatObjectSummary = (objectType?: string, objectId?: string | number) => {
  return `${resolveOperationAuditObjectTypeLabel(objectType)}（ID：${objectId || '--'}）`
}

const addRule = () => {
  ruleRows.value.push(defaultRule('VIEW', ruleRows.value.length))
}

const removeRule = (index: number) => {
  ruleRows.value.splice(index, 1)
}

const resetQuery = () => {
  scopeForm.scopeId = undefined
  scopeForm.scopeName = ''
  scopeForm.objectType = ''
  scopeForm.objectId = ''
  scopeForm.parentScopeId = undefined
  scopeForm.version = undefined
  queryParams.batchExecutionId = undefined
  queryParams.executionId = undefined
  queryParams.workTaskId = undefined
  queryParams.routeId = undefined
  queryParams.routeProcessId = undefined
  queryParams.reportId = ''
  queryParams.recordCategory = undefined
  selectedAbilities.value = abilityOptions.map((option) => option.value)
  ruleRows.value = [defaultRule()]
  result.value = undefined
  resultEvidenceNames.value = []
  loadError.value = ''
}

const initializePage = async () => {
  loadError.value = ''
  try {
    await loadSubjectOptions()
    if (scopeForm.scopeId || (scopeForm.objectType && scopeForm.objectId)) {
      await loadPermissionScope()
    }
  } catch (error) {
    if (!loadError.value) {
      loadError.value = resolveErrorMessage(error, '权限矩阵初始化失败。')
    }
  }
}

onMounted(() => {
  initializePage()
})
</script>

<style scoped>
.edhr-permission-matrix__toolbar,
.edhr-permission-matrix__rule-surface,
.edhr-permission-matrix__evaluate-toolbar,
.edhr-permission-matrix__result {
  padding: 16px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-permission-matrix__toolbar {
  border-radius: 8px 8px 0 0;
  border-bottom: 0;
  padding-bottom: 0;
}

.edhr-permission-matrix__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.edhr-permission-matrix__section-title {
  color: #172033;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
}

.edhr-permission-matrix__rule-surface,
.edhr-permission-matrix__evaluate-toolbar {
  border-bottom: 0;
}

.edhr-permission-matrix__result {
  border-radius: 0 0 8px 8px;
}

.edhr-permission-matrix__rule-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}

.edhr-permission-matrix__form {
  margin-bottom: 0;
}

.edhr-permission-matrix__rule-table :deep(.el-table__header th) {
  background: #f7f9fc;
  color: #263247;
  font-size: 0.9rem;
  font-weight: 600;
}

.edhr-permission-matrix__subject-summary {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.edhr-permission-matrix__ability-group {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
}

.edhr-permission-matrix__advanced-evaluate {
  display: block;
  width: 100%;
  margin-right: 0;
}

.edhr-permission-matrix__advanced-evaluate :deep(.el-form-item__content) {
  width: 100%;
}

.edhr-permission-matrix__advanced-evaluate :deep(.el-collapse) {
  width: 100%;
  border-top: 1px solid #edf1f6;
  border-bottom: 0;
}

.edhr-permission-matrix__advanced-evaluate :deep(.el-collapse-item__header) {
  min-height: 40px;
  color: #172033;
  font-weight: 600;
}

.edhr-permission-matrix__advanced-evaluate-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, max-content));
  gap: 0 12px;
}

.edhr-permission-matrix__decision-table {
  margin-top: 16px;
}

.edhr-permission-matrix__result-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.edhr-permission-matrix__summary-item,
.edhr-permission-matrix__evidence-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #f8fafc;
}

.edhr-permission-matrix__summary-item span,
.edhr-permission-matrix__evidence-item span {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-permission-matrix__summary-item strong,
.edhr-permission-matrix__evidence-item strong {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.edhr-permission-matrix__decision-evidence {
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.edhr-permission-matrix__decision-evidence-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-permission-matrix__evidence-collapse {
  margin-top: 12px;
}

.edhr-permission-matrix__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 10px;
}
</style>
