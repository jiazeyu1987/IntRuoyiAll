<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="1080px">
    <div v-loading="loading" class="view-matrix-dialog">
      <el-alert
        v-if="departmentLoadError"
        class="mb-12px"
        :title="departmentLoadError"
        type="error"
        :closable="false"
      />

      <div class="view-matrix-dialog__header">
        <div>
          <div class="view-matrix-dialog__title">{{ currentCategory?.code || '-' }}</div>
          <div class="view-matrix-dialog__meta">{{ currentCategory?.name || '-' }}</div>
        </div>
        <el-button type="primary" plain data-testid="dcc-view-matrix-add-rule" @click="addRule">
          新增规则
        </el-button>
      </div>

      <el-table
        :data="formRules"
        class="view-matrix-rule-table"
        data-testid="dcc-view-matrix-rule-editor"
      >
        <el-table-column label="启用" width="78" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.active" />
          </template>
        </el-table-column>
        <el-table-column label="主体标签" min-width="180">
          <template #default="{ row }">
            <el-input
              v-model="row.subjectLabel"
              placeholder="如 QA / QMS / 文档管理员"
              @change="() => handleSubjectLabelChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="标记" width="100">
          <template #default="{ row }">
            <el-select v-model="row.marker" clearable placeholder="●/▲">
              <el-option label="● 全员" value="●" />
              <el-option label="▲ 主管及以上" value="▲" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="主体类型" min-width="150">
          <template #default="{ row }">
            <el-select
              v-model="row.subjectType"
              clearable
              placeholder="请选择"
              @change="() => handleSubjectTypeChange(row)"
            >
              <el-option label="用户" value="USER" />
              <el-option label="部门" value="DEPT" />
              <el-option label="组织角色" value="POST" />
              <el-option label="权限角色" value="ROLE" />
              <el-option label="审批角色" value="DCC_POSITION" />
              <el-option label="未映射来源" value="UNMAPPED_EXCEL" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="对应部门" min-width="240">
          <template #default="{ row }">
            <div class="view-matrix-dialog__department-cell">
              <el-tree-select
                :model-value="getDepartmentSelectValue(row)"
                class="view-matrix-dialog__department-select"
                :data="getDepartmentTreeForRule(row)"
                :disabled="Boolean(departmentLoadError)"
                :props="defaultProps"
                check-strictly
                clearable
                filterable
                node-key="id"
                placeholder="请选择对应部门"
                :render-after-expand="false"
                @change="(deptId) => handleDepartmentChange(row, deptId)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="170">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="解析依据或未决说明" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeRule($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="view-matrix-preview-title">
        <span>有效权限预览</span>
        <span>只展示最终解析到的实际用户；阻塞风险仍在保存前校验。</span>
      </div>
      <el-alert
        v-if="previewError"
        class="mb-12px"
        :title="previewError"
        type="error"
        :closable="false"
      />
      <el-alert
        v-else-if="previewData?.blocking"
        class="mb-12px"
        title="当前查看矩阵存在阻塞风险，必须修正后才能保存。"
        type="error"
        :closable="false"
      />
      <div
        v-loading="previewLoading"
        class="view-matrix-user-preview"
        data-testid="dcc-view-matrix-dialog-effective-users"
      >
        <div
          v-if="previewUnresolvedRules.length"
          class="view-matrix-unresolved-rules"
          data-testid="dcc-view-matrix-dialog-unresolved-rules"
        >
          <span>未解析到用户</span>
          <el-tag
            v-for="rule in previewUnresolvedRules"
            :key="rule.key"
            size="small"
            type="danger"
            effect="plain"
            :title="rule.reason"
          >
            {{ rule.label }}
          </el-tag>
        </div>
        <div
          v-if="previewUserGroups.length"
          class="view-matrix-user-groups"
          data-testid="dcc-view-matrix-dialog-effective-user-groups"
        >
          <div
            v-for="group in previewUserGroups"
            :key="group.key"
            class="view-matrix-user-group"
          >
            <div class="view-matrix-user-group__title" :title="group.title">
              <span>{{ group.title }}</span>
              <el-tag size="small" type="info">{{ group.users.length }} 人</el-tag>
            </div>
            <div class="view-matrix-user-grid" data-testid="dcc-view-matrix-dialog-effective-user-grid">
              <span
                v-for="userName in group.users"
                :key="`${group.key}-${userName}`"
                class="view-matrix-user-chip"
                :title="userName"
              >
                {{ userName }}
              </span>
            </div>
          </div>
        </div>
        <el-empty
          v-else-if="!previewUnresolvedRules.length"
          description="尚未生成有效权限预览"
          :image-size="52"
        />
      </div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="saving"
        data-testid="dcc-view-matrix-dialog-save"
        @click="submitForm"
      >
        保存查看矩阵
      </el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import {
  previewCategoryViewMatrixEffectiveAccess,
  saveCategoryViewMatrix,
  type ControlledFileCategoryViewMatrixEffectivePreviewVO,
  type ControlledFileCategoryViewMatrixRowVO,
  type ControlledFileCategoryViewMatrixRuleVO,
  type ControlledFileCategoryViewMatrixSubjectVO
} from '@/api/dcc/controlledFile/fileCategories'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { defaultProps } from '@/utils/tree'
import { applyDepartmentAutoMatchToViewMatrixRules } from './viewMatrixDepartmentMatcher'
import {
  createDepartmentByIdMap,
  getCompanyChildDepartmentTree,
  resolveDepartmentCompanyRootId
} from './departmentTreeScope'

defineOptions({ name: 'CategoryViewMatrixDialog' })

const emit = defineEmits<{
  success: []
}>()

const dialogVisible = ref(false)
const loading = ref(false)
const previewLoading = ref(false)
const saving = ref(false)
const currentCategory = ref<ControlledFileCategoryViewMatrixRowVO | null>(null)
const formRules = ref<ControlledFileCategoryViewMatrixRuleVO[]>([])
const previewData = ref<ControlledFileCategoryViewMatrixEffectivePreviewVO | null>(null)
const previewError = ref('')
const departmentOptions = ref<DeptVO[]>([])
const departmentLoadError = ref('')
const previewAutoRefreshReady = ref(false)
let previewRefreshTimer: ReturnType<typeof setTimeout> | undefined
const message = useMessage()

const dialogTitle = computed(() => {
  const name = currentCategory.value?.name || '文件类型'
  return `查看矩阵维护：${name}`
})

const resolvePreviewSubjectRule = (
  subject: ControlledFileCategoryViewMatrixSubjectVO
) => {
  return formRules.value.find((rule) => {
    const sameSubjectId = subject.subjectId && rule.subjectId === subject.subjectId
    const sameLabel = normalizeSubjectLabel(rule.subjectLabel) === normalizeSubjectLabel(subject.subjectLabel)
    const sameColumn =
      !subject.excelColumnLetter ||
      !rule.excelColumnLetter ||
      rule.excelColumnLetter === subject.excelColumnLetter
    return (sameSubjectId || sameLabel) && sameColumn
  })
}

const resolvePreviewSubjectGroupTitle = (
  subject: ControlledFileCategoryViewMatrixSubjectVO
) => {
  const rule = resolvePreviewSubjectRule(subject)
  return (
    normalizeText(rule?.subjectDepartmentPath) ||
    normalizeText(rule?.subjectName) ||
    normalizeText(subject.subjectLabel) ||
    normalizeText(rule?.subjectLabel) ||
    '未标记来源'
  )
}

const previewUserGroups = computed(() => {
  const groups = new Map<string, { key: string; title: string; users: string[] }>()
  for (const subject of previewData.value?.viewSubjects || []) {
    const userName = subject.userName || (subject.userId ? `用户#${subject.userId}` : '')
    if (!userName) {
      continue
    }
    const title = resolvePreviewSubjectGroupTitle(subject)
    const key = `${title}-${subject.subjectType || ''}-${subject.subjectId || subject.subjectLabel || ''}`
    if (!groups.has(key)) {
      groups.set(key, { key, title, users: [] })
    }
    const group = groups.get(key)
    if (group && !group.users.includes(userName)) {
      group.users.push(userName)
    }
  }
  return [...groups.values()]
})

const previewUnresolvedRules = computed(() => {
  if (!previewData.value) {
    return []
  }
  const resolvedKeys = new Set(
    (previewData.value?.viewSubjects || [])
      .map((subject) => {
        const rule = resolvePreviewSubjectRule(subject)
        return rule ? buildRuleResolutionKey(rule) : ''
      })
      .filter(Boolean)
  )
  return formRules.value
    .filter((rule) => rule.active !== false)
    .filter((rule) => !resolvedKeys.has(buildRuleResolutionKey(rule)))
    .map((rule, index) => ({
      key: `${buildRuleResolutionKey(rule)}-${index}`,
      label:
        normalizeText(rule.subjectDepartmentPath) ||
        normalizeText(rule.subjectName) ||
        normalizeText(rule.subjectLabel) ||
        '未标记来源',
      reason: '该规则未解析到实际用户，请检查对应部门是否有可查阅人员或负责人。'
    }))
})

const open = async (row: ControlledFileCategoryViewMatrixRowVO) => {
  previewAutoRefreshReady.value = false
  clearScheduledPreviewRefresh()
  currentCategory.value = row
  formRules.value = cloneRules(row.rules || [])
  previewData.value = null
  previewError.value = ''
  departmentLoadError.value = ''
  dialogVisible.value = true
  loading.value = true
  try {
    await loadDepartmentOptions()
    runDepartmentAutoMatch({ silent: true })
    await nextTick()
    previewAutoRefreshReady.value = true
    await refreshPreview()
  } catch (error) {
    departmentLoadError.value = '部门树加载失败，无法维护对应部门，请刷新后重试。'
    previewError.value = departmentLoadError.value
    message.error(departmentLoadError.value)
  } finally {
    loading.value = false
  }
}

const cloneRules = (rules: ControlledFileCategoryViewMatrixRuleVO[]) =>
  rules.map((rule) => ({
    ...rule,
    active: Boolean(rule.active)
  }))

const loadDepartmentOptions = async () => {
  if (departmentOptions.value.length > 0) {
    return
  }
  const departments = await getSimpleDeptList()
  if (!departments.length) {
    throw new Error('当前租户没有可选择部门')
  }
  departmentOptions.value = departments
}

const addRule = () => {
  formRules.value.push({
    active: true,
    excelFileName: '电子文控系统推进计划及需求表.xlsx'
  })
  previewData.value = null
  previewError.value = ''
}

const removeRule = (index: number) => {
  formRules.value.splice(index, 1)
  runDepartmentAutoMatch({ silent: true })
  previewData.value = null
  previewError.value = ''
}

const buildSaveData = () => ({
  rules: formRules.value.map((rule) => {
    const { subjectDepartmentPath: _subjectDepartmentPath, subjectName: _subjectName, ...editableRule } = rule
    return {
      ...editableRule,
      active: Boolean(rule.active),
      excelFileName: normalizeText(rule.excelFileName),
      excelColumnLetter: normalizeText(rule.excelColumnLetter),
      subjectLabel: normalizeText(rule.subjectLabel),
      subjectTopHeader: normalizeText(rule.subjectTopHeader),
      subjectSubHeader: normalizeText(rule.subjectSubHeader),
      remark: normalizeText(rule.remark)
    }
  })
})

const normalizeText = (value?: string) => {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

const normalizeSubjectLabel = (value?: string) => normalizeText(value)?.toLocaleLowerCase() || ''

const buildRuleResolutionKey = (rule: ControlledFileCategoryViewMatrixRuleVO) =>
  [
    normalizeSubjectLabel(rule.subjectLabel),
    rule.subjectType || '',
    rule.subjectId || '',
    rule.marker || '',
    rule.scopeType || ''
  ].join('|')

const departmentById = computed(() => {
  return createDepartmentByIdMap(departmentOptions.value)
})

const resolveRuleDepartmentId = (row: ControlledFileCategoryViewMatrixRuleVO) => {
  if (row.subjectType === 'DEPT' && row.subjectId) {
    return row.subjectId
  }
  const label = normalizeSubjectLabel(row.subjectLabel)
  if (!label) {
    return undefined
  }
  return formRules.value.find(
    (rule) =>
      rule !== row &&
      normalizeSubjectLabel(rule.subjectLabel) === label &&
      rule.subjectType === 'DEPT' &&
      Boolean(rule.subjectId)
  )?.subjectId
}

const resolveCategoryCompanyRootId = (row: ControlledFileCategoryViewMatrixRuleVO) => {
  const uniqueCompanyRootIds = [
    ...new Set(
      formRules.value
        .filter((rule) => rule !== row && rule.subjectType === 'DEPT' && Boolean(rule.subjectId))
        .map((rule) => resolveDepartmentCompanyRootId(departmentById.value, rule.subjectId))
        .filter((companyRootId): companyRootId is number => Boolean(companyRootId))
    )
  ]
  return uniqueCompanyRootIds.length === 1 ? uniqueCompanyRootIds[0] : undefined
}

const resolveViewMatrixCompanyRootId = (row: ControlledFileCategoryViewMatrixRuleVO) => {
  return (
    resolveDepartmentCompanyRootId(departmentById.value, resolveRuleDepartmentId(row)) ||
    resolveCategoryCompanyRootId(row)
  )
}

const getDepartmentTreeForRule = (row: ControlledFileCategoryViewMatrixRuleVO) => {
  const companyRootId = resolveViewMatrixCompanyRootId(row)
  return getCompanyChildDepartmentTree(departmentOptions.value, departmentById.value, companyRootId)
}

const getDepartmentSelectValue = (row: ControlledFileCategoryViewMatrixRuleVO) => {
  return row.subjectType === 'DEPT' ? row.subjectId : undefined
}

const buildDepartmentPath = (deptId?: number) => {
  if (!deptId) {
    return undefined
  }
  const names: string[] = []
  const visited = new Set<number>()
  let currentId: number | undefined = deptId
  while (currentId && currentId > 0 && !visited.has(currentId)) {
    visited.add(currentId)
    const department = departmentById.value.get(currentId)
    if (!department?.name) {
      return `部门不存在（${currentId}）`
    }
    names.push(department.name)
    currentId = department.parentId
  }
  return names.reverse().join('-')
}

const resetPreviewState = () => {
  previewData.value = null
  previewError.value = ''
}

const clearScheduledPreviewRefresh = () => {
  if (previewRefreshTimer) {
    clearTimeout(previewRefreshTimer)
    previewRefreshTimer = undefined
  }
}

const schedulePreviewRefresh = () => {
  if (
    !previewAutoRefreshReady.value ||
    !dialogVisible.value ||
    !currentCategory.value ||
    loading.value ||
    departmentLoadError.value
  ) {
    return
  }
  clearScheduledPreviewRefresh()
  previewRefreshTimer = setTimeout(() => {
    previewRefreshTimer = undefined
    if (previewLoading.value) {
      schedulePreviewRefresh()
      return
    }
    refreshPreview()
  }, 500)
}

const runDepartmentAutoMatch = (options: { silent?: boolean } = {}) => {
  if (!departmentOptions.value.length) {
    if (!options.silent) {
      message.warning('部门树尚未加载，无法初步对应。')
    }
    return
  }
  const result = applyDepartmentAutoMatchToViewMatrixRules(formRules.value, departmentOptions.value)
  formRules.value = result.rules as ControlledFileCategoryViewMatrixRuleVO[]
  resetPreviewState()
  if (!options.silent) {
    message.success('已完成查看矩阵可查阅名称的部门初步对应')
  }
}

const syncSameLabelDepartment = (sourceRule: ControlledFileCategoryViewMatrixRuleVO) => {
  const label = normalizeSubjectLabel(sourceRule.subjectLabel)
  if (!label) {
    return
  }
  for (const rule of formRules.value) {
    if (normalizeSubjectLabel(rule.subjectLabel) === label) {
      rule.subjectType = 'DEPT'
      rule.subjectId = sourceRule.subjectId
      rule.subjectDepartmentPath = sourceRule.subjectDepartmentPath
    }
  }
}

const handleDepartmentChange = (
  row: ControlledFileCategoryViewMatrixRuleVO,
  deptId?: number | string
) => {
  const selectedDeptId = Number(deptId)
  row.subjectType = 'DEPT'
  row.subjectId = Number.isFinite(selectedDeptId) && selectedDeptId > 0 ? selectedDeptId : undefined
  row.subjectDepartmentPath = buildDepartmentPath(row.subjectId)
  syncSameLabelDepartment(row)
  runDepartmentAutoMatch({ silent: true })
  resetPreviewState()
}

const applyExistingDepartmentForLabel = (row: ControlledFileCategoryViewMatrixRuleVO) => {
  const label = normalizeSubjectLabel(row.subjectLabel)
  if (!label) {
    return
  }
  const existing = formRules.value.find(
    (rule) =>
      rule !== row &&
      normalizeSubjectLabel(rule.subjectLabel) === label &&
      rule.subjectType === 'DEPT' &&
      Boolean(rule.subjectId)
  )
  if (!existing) {
    return
  }
  row.subjectType = 'DEPT'
  row.subjectId = existing.subjectId
  row.subjectDepartmentPath = existing.subjectDepartmentPath || buildDepartmentPath(existing.subjectId)
  syncSameLabelDepartment(row)
  resetPreviewState()
}

const handleSubjectLabelChange = (row: ControlledFileCategoryViewMatrixRuleVO) => {
  applyExistingDepartmentForLabel(row)
  runDepartmentAutoMatch({ silent: true })
}

const handleSubjectTypeChange = (row: ControlledFileCategoryViewMatrixRuleVO) => {
  if (row.subjectType === 'DEPT') {
    applyExistingDepartmentForLabel(row)
    runDepartmentAutoMatch({ silent: true })
    return
  }
  row.subjectId = undefined
  row.subjectDepartmentPath = undefined
  runDepartmentAutoMatch({ silent: true })
  resetPreviewState()
}

const refreshPreview = async () => {
  clearScheduledPreviewRefresh()
  if (!currentCategory.value) {
    previewError.value = '缺少文件类型，无法预览查看矩阵。'
    return null
  }
  if (departmentLoadError.value) {
    previewError.value = departmentLoadError.value
    message.error(departmentLoadError.value)
    return null
  }
  previewLoading.value = true
  previewError.value = ''
  try {
    previewData.value = await previewCategoryViewMatrixEffectiveAccess(
      currentCategory.value.categoryId,
      buildSaveData()
    )
    return previewData.value
  } catch (error) {
    previewData.value = null
    previewError.value =
      error instanceof Error && error.message && error.message !== 'error'
        ? error.message
        : '有效权限预览失败，请按后端阻塞原因修正查看矩阵规则。'
    return null
  } finally {
    previewLoading.value = false
  }
}

const submitForm = async () => {
  if (!currentCategory.value) {
    message.error('缺少文件类型，无法保存查看矩阵。')
    return
  }
  if (departmentLoadError.value) {
    message.error(departmentLoadError.value)
    return
  }
  const preview = await refreshPreview()
  if (!preview) {
    return
  }
  if (preview.blocking) {
    message.error('查看矩阵存在阻塞风险，已停止保存。')
    return
  }
  saving.value = true
  try {
    await saveCategoryViewMatrix(currentCategory.value.categoryId, buildSaveData())
    message.success('查看矩阵已保存')
    dialogVisible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

watch(
  formRules,
  () => {
    schedulePreviewRefresh()
  },
  { deep: true }
)

watch(dialogVisible, (visible) => {
  if (!visible) {
    previewAutoRefreshReady.value = false
    clearScheduledPreviewRefresh()
  }
})

onBeforeUnmount(() => {
  clearScheduledPreviewRefresh()
})

defineExpose({ open })
</script>

<style scoped>
.view-matrix-dialog {
  display: grid;
  gap: 14px;
}

.view-matrix-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.view-matrix-dialog__title {
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.view-matrix-dialog__meta {
  margin-top: 3px;
  color: #667085;
  font-size: 12px;
}

.view-matrix-rule-table :deep(.el-input-number .el-input__inner) {
  text-align: left;
}

.view-matrix-dialog__department-cell {
  display: block;
}

.view-matrix-dialog__department-select {
  width: 100%;
}

.view-matrix-preview-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-top: 2px;
}

.view-matrix-preview-title span:first-child {
  color: #172033;
  font-weight: 700;
}

.view-matrix-preview-title span:last-child {
  color: #667085;
  font-size: 12px;
}

.view-matrix-user-preview {
  max-height: 330px;
  min-height: 96px;
  overflow-y: auto;
  padding: 12px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #ffffff;
}

.view-matrix-unresolved-rules {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  padding: 8px;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  background: #fff7f7;
  color: #b42318;
  font-size: 12px;
  font-weight: 600;
}

.view-matrix-user-groups {
  display: grid;
  gap: 10px;
}

.view-matrix-user-group {
  padding: 10px;
  border: 1px solid #edf1f6;
  border-radius: 8px;
  background: #fafcff;
}

.view-matrix-user-group__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: #172033;
  font-size: 13px;
  font-weight: 700;
}

.view-matrix-user-group__title span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.view-matrix-user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(88px, 1fr));
  gap: 8px;
}

.view-matrix-user-chip {
  overflow: hidden;
  padding: 5px 9px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  color: #263247;
  font-size: 13px;
  line-height: 20px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
