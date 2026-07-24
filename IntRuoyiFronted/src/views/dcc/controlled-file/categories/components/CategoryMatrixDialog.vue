<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="1080px">
    <div v-loading="loading" class="review-matrix-dialog">
      <el-alert
        class="mb-16px"
        title="第 1 / 4 层文控继续固定，仅维护第 2 层审核与第 3 层批准规则；点击自动解析人员后将按标签初步匹配部门并刷新预览。"
        type="info"
        :closable="false"
      />

      <div class="review-matrix-dialog__header">
        <div>
          <div class="review-matrix-dialog__title">{{ currentCategory?.code || '-' }}</div>
          <div class="review-matrix-dialog__meta">{{ currentCategory?.name || '-' }}</div>
        </div>
        <div class="review-matrix-dialog__actions">
          <el-button plain :disabled="isPreviewMode" @click="runDepartmentAutoMatch">
            自动解析人员
          </el-button>
          <el-button type="primary" plain :disabled="isPreviewMode" @click="addRule">
            新增规则
          </el-button>
        </div>
      </div>

      <div class="matrix-grid">
        <el-form-item label="生效时间" required>
          <el-date-picker
            v-model="formData.effectiveTime"
            class="w-full"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled="isPreviewMode"
            placeholder="请选择审阅矩阵生效时间"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            :disabled="isPreviewMode"
            placeholder="填写本次矩阵调整说明"
          />
        </el-form-item>
      </div>

      <el-table
        :data="formData.rules"
        class="view-matrix-rule-table"
        data-testid="dcc-review-matrix-rule-editor"
      >
        <el-table-column label="启用" width="78" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.active" :disabled="isPreviewMode" />
          </template>
        </el-table-column>
        <el-table-column label="阶段" min-width="110">
          <template #default="{ row }">
            <el-select v-model="row.stageType" :disabled="isPreviewMode">
              <el-option label="审核" value="SIGNOFF" />
              <el-option label="批准" value="APPROVAL" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="主体标签" min-width="180">
          <template #default="{ row }">
            <el-input
              v-model="row.subjectLabel"
              :disabled="isPreviewMode"
              placeholder="如 QMS / 新品开发部"
              @change="() => handleSubjectLabelChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="标记" width="92" align="center">
          <template #default>
            <span class="review-matrix-dialog__marker">▲</span>
          </template>
        </el-table-column>
        <el-table-column label="主体类型" min-width="150">
          <template #default="{ row }">
            <el-select
              v-model="row.subjectType"
              clearable
              :disabled="isPreviewMode"
              placeholder="请选择"
              @change="() => handleSubjectTypeChange(row)"
            >
              <el-option label="用户" value="USER" />
              <el-option label="部门" value="DEPT" />
              <el-option label="权限角色" value="ROLE" />
              <el-option label="组织角色" value="POST" />
              <el-option label="审批角色" value="DCC_POSITION" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="对应部门" min-width="260">
          <template #default="{ row }">
            <UserSelect
              v-if="row.subjectType === 'USER'"
              :model-value="row.subjectId"
              :disabled="isPreviewMode"
              placeholder="请选择系统用户"
              @change="(user) => handleUserChange(row, user)"
            />
            <el-tree-select
              v-else-if="row.subjectType === 'DEPT'"
              :model-value="getDepartmentSelectValue(row)"
              class="review-matrix-dialog__department-select"
              :data="getDepartmentTreeForRule(row)"
              :disabled="isPreviewMode"
              :props="defaultProps"
              check-strictly
              clearable
              filterable
              node-key="id"
              placeholder="请选择对应部门"
              :render-after-expand="false"
              @change="(deptId) => handleDepartmentChange(row, deptId)"
            />
            <RoleSelect
              v-else-if="row.subjectType === 'ROLE'"
              :model-value="row.subjectId"
              :disabled="isPreviewMode"
              placeholder="请选择权限角色"
              @change="(role) => handleRoleChange(row, role)"
            />
            <el-select
              v-else-if="row.subjectType === 'POST'"
              :model-value="row.subjectId"
              class="w-full"
              clearable
              filterable
              :disabled="isPreviewMode"
              placeholder="请选择组织角色"
              @change="(postId) => handlePostChange(row, postId)"
            >
              <el-option
                v-for="item in postOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
            <el-select
              v-else-if="row.subjectType === 'DCC_POSITION'"
              :model-value="row.subjectId"
              class="w-full"
              clearable
              filterable
              :disabled="isPreviewMode"
              placeholder="请选择审批角色"
              @change="(positionId) => handleDccPositionChange(row, positionId)"
            >
              <el-option
                v-for="item in approvalPositionOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
            <span v-else class="review-matrix-dialog__placeholder">请选择主体类型后配置主体</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" :disabled="isPreviewMode" @click="removeRule($index)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="preview-title-row">
        <span class="preview-title">有效权限预览</span>
        <span class="preview-caption">
          保存前按统一真源解析最终可查阅用户、待审查看边界和下载规则风险，管理端展示与运行时口径保持一致。
        </span>
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
        title="当前预览存在阻塞风险，必须先修正后才能保存。"
        type="warning"
        :closable="false"
      />
      <div
        v-if="previewData?.risks?.length"
        class="preview-risk-list mb-12px"
        data-testid="dcc-matrix-preview-risks"
      >
        <el-tag
          v-for="risk in previewData.risks"
          :key="`${risk.code}-${risk.message}`"
          :type="risk.blocking ? 'danger' : 'warning'"
          size="small"
        >
          {{ risk.code }}
        </el-tag>
      </div>
      <el-table
        :data="previewData?.stages || []"
        :loading="previewLoading"
        empty-text="当前尚未生成可用预览"
        data-testid="dcc-matrix-effective-preview"
      >
        <el-table-column label="阶段" width="90" prop="stageNo" align="center" />
        <el-table-column label="阶段名称" min-width="160" prop="stageName" />
        <el-table-column label="审批方式" width="120">
          <template #default="{ row }">
            {{ formatApproveMethod(row.approveMethod) }}
          </template>
        </el-table-column>
        <el-table-column label="主体集合" min-width="260">
          <template #default="{ row }">
            {{ row.positionNames?.length ? row.positionNames.join(' / ') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="解析实际用户" min-width="280">
          <template #default="{ row }">
            {{ formatResolvedSubjects(row.resolvedSubjects) }}
          </template>
        </el-table-column>
        <el-table-column label="解析规则" min-width="180" prop="sourceRule" show-overflow-tooltip />
      </el-table>

      <div class="preview-title-row mt-16px">
        <span class="preview-title">最终实际用户</span>
        <span class="preview-caption">按审核/批准对应规则分组，仅展示最终解析到的真实用户。</span>
      </div>
      <div
        v-loading="previewLoading"
        class="view-matrix-user-preview"
        data-testid="dcc-review-matrix-effective-user-groups"
      >
        <div
          v-if="previewUnresolvedRules.length"
          class="view-matrix-unresolved-rules"
          data-testid="dcc-review-matrix-unresolved-rules"
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
        <div v-if="previewUserGroups.length" class="view-matrix-user-groups">
          <div v-for="group in previewUserGroups" :key="group.key" class="view-matrix-user-group">
            <div class="view-matrix-user-group__title" :title="group.title">
              <span>{{ group.title }}</span>
              <el-tag size="small" type="info">{{ group.users.length }} 人</el-tag>
            </div>
            <div class="view-matrix-user-grid">
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
        <el-empty v-else-if="!previewUnresolvedRules.length" description="尚未生成有效权限预览" :image-size="52" />
      </div>

      <div v-if="previewData?.downloadRuleSummary" class="preview-download-summary mt-16px">
        <div class="preview-download-summary__title">下载规则说明</div>
        <div class="preview-download-summary__value">
          {{ previewData.downloadRuleSummary }}
        </div>
        <div class="preview-download-summary__meta">
          {{ previewData.downloadRuleSubjects?.length ? previewData.downloadRuleSubjects.join(' / ') : '未配置 DOWNLOAD 主体' }}
        </div>
      </div>

      <el-table
        v-if="previewData?.risks?.length"
        class="mt-16px"
        :data="previewData.risks"
        data-testid="dcc-matrix-risk-table"
      >
        <el-table-column label="风险级别" width="120">
          <template #default="{ row }">
            <el-tag :type="row.blocking ? 'danger' : 'warning'" size="small">
              {{ row.blocking ? '阻塞' : '提示' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险编码" width="220" prop="code" />
        <el-table-column label="风险说明" min-width="360">
          <template #default="{ row }">
            {{ row.message }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button plain :loading="previewLoading" @click="refreshPreview">刷新预览</el-button>
      <el-button v-if="!isPreviewMode" type="primary" :loading="saving" @click="submitForm">
        保存矩阵
      </el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import {
  getCategoryApprovalMatrix,
  previewCategoryApprovalMatrixEffectiveAccess,
  saveCategoryApprovalMatrix,
  type ControlledFileCategoryApprovalMatrixVO,
  type ControlledFileCategoryReviewMatrixEffectivePreviewVO,
  type ControlledFileCategoryReviewMatrixRuleVO,
  type ControlledFileCategoryReviewMatrixSubjectVO,
  type ControlledFileCategoryVO
} from '@/api/dcc/controlledFile/fileCategories'
import {
  getApprovalPositionList,
  type ControlledFileApprovalPositionVO
} from '@/api/dcc/controlledFile/approvalPositions'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { getSimplePostList, type PostVO } from '@/api/system/post'
import type { RoleVO } from '@/api/system/role'
import type { UserVO } from '@/api/system/user'
import { defaultProps } from '@/utils/tree'
import RoleSelect from '@/views/system/role/components/RoleSelect.vue'
import UserSelect from '@/views/system/user/components/UserSelect.vue'
import { applyDepartmentAutoMatchToViewMatrixRules } from './viewMatrixDepartmentMatcher'
import { createApprovalMatrixDraft, type ApprovalMatrixDraft, type ReviewMatrixRuleDraft } from '../governance'
import {
  createDepartmentByIdMap,
  findExactMatchedDepartmentIdByLabel,
  getCompanyChildDepartmentTree,
  resolveDepartmentCompanyRootId
} from './departmentTreeScope'

defineOptions({ name: 'CategoryMatrixDialog' })

type MatrixDialogMode = 'create' | 'edit' | 'preview'
type PreviewSubject = ControlledFileCategoryReviewMatrixEffectivePreviewVO['viewSubjects'][number]

const emit = defineEmits<{
  success: []
}>()

const dialogVisible = ref(false)
const dialogTitle = ref('审阅矩阵配置')
const loading = ref(false)
const previewLoading = ref(false)
const saving = ref(false)
const currentCategory = ref<ControlledFileCategoryVO>()
const previewData = ref<ControlledFileCategoryReviewMatrixEffectivePreviewVO | null>(null)
const previewError = ref('')
const dialogMode = ref<MatrixDialogMode>('edit')
const departmentOptions = ref<DeptVO[]>([])
const postOptions = ref<PostVO[]>([])
const approvalPositionOptions = ref<ControlledFileApprovalPositionVO[]>([])
const formData = reactive<ApprovalMatrixDraft>(createApprovalMatrixDraft())
const isPreviewMode = computed(() => dialogMode.value === 'preview')
const message = useMessage()
const departmentById = computed(() => createDepartmentByIdMap(departmentOptions.value))

const normalizeRuleMarker = () => '▲' as const

const normalizeText = (value?: string) => {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

const normalizeSubjectLabel = (value?: string) => normalizeText(value)?.toLocaleLowerCase() || ''

const normalizeRule = (rule?: Partial<ControlledFileCategoryReviewMatrixRuleVO>): ReviewMatrixRuleDraft => ({
  stageType: rule?.stageType === 'APPROVAL' ? 'APPROVAL' : 'SIGNOFF',
  active: rule?.active ?? true,
  subjectLabel: normalizeText(rule?.subjectLabel),
  marker: normalizeRuleMarker(),
  subjectType:
    rule?.subjectType === 'POSITION'
      ? 'DCC_POSITION'
      : rule?.subjectType === 'USER' ||
          rule?.subjectType === 'DEPT' ||
          rule?.subjectType === 'ROLE' ||
          rule?.subjectType === 'POST' ||
          rule?.subjectType === 'DCC_POSITION'
        ? rule.subjectType
        : undefined,
  subjectId: rule?.subjectId,
  subjectName: normalizeText(rule?.subjectName),
  subjectDepartmentPath: normalizeText(rule?.subjectDepartmentPath)
})

const buildRulePayload = (rule: ReviewMatrixRuleDraft) => ({
  stageType: rule.stageType,
  active: rule.active,
  subjectLabel: normalizeText(rule.subjectLabel),
  marker: normalizeRuleMarker(),
  subjectType: rule.subjectType,
  subjectId: rule.subjectId,
  subjectName: normalizeText(rule.subjectName),
  subjectDepartmentPath: normalizeText(rule.subjectDepartmentPath)
})

const applyMatrix = (matrix?: Partial<ControlledFileCategoryApprovalMatrixVO>) => {
  const next = createApprovalMatrixDraft(matrix)
  formData.effectiveTime = next.effectiveTime
  formData.remark = next.remark
  formData.rules = next.rules.map((rule) => normalizeRule(rule))
}

const ensureLookupData = async () => {
  const tasks: Promise<unknown>[] = []
  if (departmentOptions.value.length === 0) {
    tasks.push(
      getSimpleDeptList().then((departments) => {
        departmentOptions.value = departments
      })
    )
  }
  if (postOptions.value.length === 0) {
    tasks.push(
      getSimplePostList().then((posts) => {
        postOptions.value = posts
      })
    )
  }
  if (approvalPositionOptions.value.length === 0) {
    tasks.push(
      getApprovalPositionList().then((positions) => {
        approvalPositionOptions.value = positions.filter((position) => position.active !== false)
      })
    )
  }
  if (tasks.length > 0) {
    await Promise.all(tasks)
  }
}

const loadMatrix = async () => {
  if (!currentCategory.value?.id) {
    return
  }
  loading.value = true
  try {
    const matrix = await getCategoryApprovalMatrix(currentCategory.value.id)
    applyMatrix(matrix)
  } finally {
    loading.value = false
  }
}

const canPreviewCurrentDraft = () => {
  if (!formData.effectiveTime) {
    previewError.value = '请选择审阅矩阵生效时间后再预览。'
    previewData.value = null
    return false
  }
  if (!formData.rules.some((rule) => rule.active !== false && rule.stageType === 'SIGNOFF')) {
    previewError.value = '至少保留 1 条启用的审核规则后才能预览。'
    previewData.value = null
    return false
  }
  if (!formData.rules.some((rule) => rule.active !== false && rule.stageType === 'APPROVAL')) {
    previewError.value = '至少保留 1 条启用的批准规则后才能预览。'
    previewData.value = null
    return false
  }
  return true
}

const refreshPreview = async () => {
  if (!currentCategory.value?.id) {
    return
  }
  if (!canPreviewCurrentDraft()) {
    return
  }
  previewLoading.value = true
  previewError.value = ''
  try {
    previewData.value = await previewCategoryApprovalMatrixEffectiveAccess(currentCategory.value.id, {
      effectiveTime: formData.effectiveTime,
      remark: formData.remark.trim() || undefined,
      rules: formData.rules.map((rule) => buildRulePayload(rule))
    })
  } catch (error) {
    previewData.value = null
    previewError.value = resolveErrorMessage(
      error,
      '有效权限预览失败，请根据后端错误提示检查主体配置、下载规则和历史 snapshot 边界。'
    )
  } finally {
    previewLoading.value = false
  }
}

const validateForm = () => {
  if (!formData.effectiveTime) {
    message.warning('请选择审阅矩阵生效时间')
    return false
  }
  if (!formData.rules.some((rule) => rule.active !== false && rule.stageType === 'SIGNOFF')) {
    message.warning('至少保留 1 条启用的审核规则')
    return false
  }
  if (!formData.rules.some((rule) => rule.active !== false && rule.stageType === 'APPROVAL')) {
    message.warning('至少保留 1 条启用的批准规则')
    return false
  }
  const invalidRule = formData.rules.find(
    (rule) =>
      rule.active !== false &&
      (!normalizeText(rule.subjectLabel) || !rule.subjectType || !rule.subjectId)
  )
  if (invalidRule) {
    message.warning('启用规则必须完整填写主体标签、主体类型和对应主体')
    return false
  }
  return true
}

const submitForm = async () => {
  if (!currentCategory.value?.id || !validateForm()) {
    return
  }
  saving.value = true
  previewError.value = ''
  try {
    await refreshPreview()
    if (previewError.value || previewData.value?.blocking) {
      message.error(previewError.value || '当前矩阵存在阻塞风险，请先修正预览中的问题。')
      return
    }
    await saveCategoryApprovalMatrix(currentCategory.value.id, {
      effectiveTime: formData.effectiveTime,
      remark: formData.remark.trim() || undefined,
      rules: formData.rules.map((rule) => buildRulePayload(rule))
    })
    message.success('审阅矩阵已保存')
    await loadMatrix()
    await refreshPreview()
    emit('success')
  } catch (error) {
    previewError.value = resolveErrorMessage(
      error,
      '审阅矩阵保存失败，请根据后端错误提示修正后重试。'
    )
  } finally {
    saving.value = false
  }
}

const formatApproveMethod = (approveMethod?: string) => {
  if (approveMethod === 'ALL') {
    return '全部同意'
  }
  if (approveMethod === 'ANY') {
    return '任意一个同意'
  }
  return approveMethod || '-'
}

const formatResolvedSubjects = (subjects?: ControlledFileCategoryReviewMatrixSubjectVO[]) => {
  const names = (subjects || [])
    .map((subject) => subject.userName || subject.subjectLabel || (subject.userId ? `用户#${subject.userId}` : ''))
    .filter(Boolean)
  return names.length ? names.join(' / ') : '-'
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}

const buildDialogTitle = (categoryName: string) => {
  const actionLabel =
    dialogMode.value === 'create' ? '新增' : dialogMode.value === 'preview' ? '预览' : '编辑'
  return `审阅矩阵配置 - ${actionLabel} - ${categoryName}`
}

const buildRuleResolutionKey = (rule: ReviewMatrixRuleDraft) =>
  [
    rule.stageType || '',
    normalizeSubjectLabel(rule.subjectLabel),
    rule.subjectType || '',
    rule.subjectId || '',
    normalizeRuleMarker()
  ].join('|')

const addRule = () => {
  formData.rules.push({
    stageType: 'SIGNOFF',
    active: true,
    marker: normalizeRuleMarker()
  })
}

const removeRule = (index: number) => {
  formData.rules.splice(index, 1)
}

const runDepartmentAutoMatch = () => {
  const result = applyDepartmentAutoMatchToViewMatrixRules(
    formData.rules.map((rule) => ({ ...rule, marker: normalizeRuleMarker() })),
    departmentOptions.value
  )
  formData.rules = result.rules.map((rule) => normalizeRule(rule)) as ApprovalMatrixDraft['rules']
  message.success('已完成主体标签的部门初步对应')
  refreshPreview()
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
      break
    }
    names.push(department.name)
    currentId = department.parentId
  }
  return names.reverse().join('-')
}

const getDepartmentSelectValue = (row: ReviewMatrixRuleDraft) => {
  return row.subjectType === 'DEPT' ? row.subjectId : undefined
}

const resolveRuleDepartmentId = (row: ReviewMatrixRuleDraft) => {
  if (row.subjectType === 'DEPT' && row.subjectId) {
    return row.subjectId
  }
  const label = normalizeSubjectLabel(row.subjectLabel)
  if (!label) {
    return undefined
  }
  return formData.rules.find(
    (rule) =>
      rule !== row &&
      rule.subjectType === 'DEPT' &&
      Boolean(rule.subjectId) &&
      normalizeSubjectLabel(rule.subjectLabel) === label
  )?.subjectId
}

const resolveCategoryCompanyRootId = (row: ReviewMatrixRuleDraft) => {
  const uniqueCompanyRootIds = [
    ...new Set(
      formData.rules
        .filter((rule) => rule !== row && rule.subjectType === 'DEPT' && Boolean(rule.subjectId))
        .map((rule) => resolveDepartmentCompanyRootId(departmentById.value, rule.subjectId))
        .filter((companyRootId): companyRootId is number => Boolean(companyRootId))
    )
  ]
  return uniqueCompanyRootIds.length === 1 ? uniqueCompanyRootIds[0] : undefined
}

const resolveReviewMatrixCompanyRootId = (row: ReviewMatrixRuleDraft) => {
  const currentSelectedCompanyRootId = resolveDepartmentCompanyRootId(
    departmentById.value,
    row.subjectType === 'DEPT' ? row.subjectId : undefined
  )
  if (currentSelectedCompanyRootId) {
    return currentSelectedCompanyRootId
  }

  const sameLabelSelectedCompanyRootId = resolveDepartmentCompanyRootId(
    departmentById.value,
    resolveRuleDepartmentId(row)
  )
  if (sameLabelSelectedCompanyRootId) {
    return sameLabelSelectedCompanyRootId
  }

  const exactMatchedDepartmentId = findExactMatchedDepartmentIdByLabel(
    departmentOptions.value,
    normalizeSubjectLabel,
    row.subjectLabel
  )
  const exactMatchedCompanyRootId = resolveDepartmentCompanyRootId(
    departmentById.value,
    exactMatchedDepartmentId
  )
  if (exactMatchedCompanyRootId) {
    return exactMatchedCompanyRootId
  }

  return resolveCategoryCompanyRootId(row)
}

const getDepartmentTreeForRule = (row: ReviewMatrixRuleDraft) => {
  const companyRootId = resolveReviewMatrixCompanyRootId(row)
  return getCompanyChildDepartmentTree(departmentOptions.value, departmentById.value, companyRootId)
}

const clearSubjectMapping = (row: ReviewMatrixRuleDraft) => {
  row.subjectId = undefined
  row.subjectName = undefined
  row.subjectDepartmentPath = undefined
  row.marker = normalizeRuleMarker()
}

const syncSameLabelDepartment = (sourceRule: ReviewMatrixRuleDraft) => {
  if (sourceRule.subjectType !== 'DEPT' || !sourceRule.subjectId) {
    return
  }
  const label = normalizeSubjectLabel(sourceRule.subjectLabel)
  if (!label) {
    return
  }
  for (const rule of formData.rules) {
    if (rule !== sourceRule && normalizeSubjectLabel(rule.subjectLabel) === label && rule.subjectType === 'DEPT') {
      rule.subjectId = sourceRule.subjectId
      rule.subjectName = sourceRule.subjectName
      rule.subjectDepartmentPath = sourceRule.subjectDepartmentPath
      rule.marker = normalizeRuleMarker()
    }
  }
}

const handleDepartmentChange = (row: ReviewMatrixRuleDraft, deptId?: number | string) => {
  const selectedDeptId = Number(deptId)
  row.subjectType = 'DEPT'
  row.subjectId = Number.isFinite(selectedDeptId) && selectedDeptId > 0 ? selectedDeptId : undefined
  row.subjectName = row.subjectId ? departmentById.value.get(row.subjectId)?.name : undefined
  row.subjectDepartmentPath = buildDepartmentPath(row.subjectId)
  row.marker = normalizeRuleMarker()
  syncSameLabelDepartment(row)
}

const handleUserChange = (row: ReviewMatrixRuleDraft, user?: UserVO) => {
  row.subjectType = 'USER'
  row.subjectId = user?.id
  row.subjectName = user?.nickname || user?.username
  row.subjectDepartmentPath = undefined
  row.marker = normalizeRuleMarker()
}

const handleRoleChange = (row: ReviewMatrixRuleDraft, role?: RoleVO) => {
  row.subjectType = 'ROLE'
  row.subjectId = role?.id
  row.subjectName = role?.name
  row.subjectDepartmentPath = undefined
  row.marker = normalizeRuleMarker()
}

const handlePostChange = (row: ReviewMatrixRuleDraft, postId?: number | string) => {
  const selectedPostId = Number(postId)
  row.subjectType = 'POST'
  row.subjectId = Number.isFinite(selectedPostId) && selectedPostId > 0 ? selectedPostId : undefined
  row.subjectName = postOptions.value.find((item) => item.id === row.subjectId)?.name
  row.subjectDepartmentPath = undefined
  row.marker = normalizeRuleMarker()
}

const handleDccPositionChange = (row: ReviewMatrixRuleDraft, positionId?: number | string) => {
  const selectedPositionId = Number(positionId)
  row.subjectType = 'DCC_POSITION'
  row.subjectId = Number.isFinite(selectedPositionId) && selectedPositionId > 0 ? selectedPositionId : undefined
  row.subjectName = approvalPositionOptions.value.find((item) => item.id === row.subjectId)?.name
  row.subjectDepartmentPath = undefined
  row.marker = normalizeRuleMarker()
}

const handleSubjectLabelChange = (row: ReviewMatrixRuleDraft) => {
  if (row.subjectType === 'DEPT') {
    syncSameLabelDepartment(row)
  }
}

const handleSubjectTypeChange = (row: ReviewMatrixRuleDraft) => {
  const nextType = row.subjectType
  clearSubjectMapping(row)
  row.subjectType = nextType
  if (row.subjectType === 'DEPT') {
    const sameLabelRule = formData.rules.find(
      (rule) =>
        rule !== row &&
        rule.subjectType === 'DEPT' &&
        Boolean(rule.subjectId) &&
        normalizeSubjectLabel(rule.subjectLabel) === normalizeSubjectLabel(row.subjectLabel)
    )
    if (sameLabelRule) {
      row.subjectId = sameLabelRule.subjectId
      row.subjectName = sameLabelRule.subjectName
      row.subjectDepartmentPath = sameLabelRule.subjectDepartmentPath
    }
  }
}

const resolvePreviewSubjectRule = (subject: PreviewSubject) => {
  return formData.rules.find((rule) => {
    const sameStage = !subject.stageType || subject.stageType === rule.stageType
    const sameSubjectId = subject.subjectId && rule.subjectId === subject.subjectId
    const sameLabel = normalizeSubjectLabel(rule.subjectLabel) === normalizeSubjectLabel(subject.subjectLabel)
    return sameStage && (sameSubjectId || sameLabel)
  })
}

const previewResolvedSubjects = computed<PreviewSubject[]>(() =>
  (previewData.value?.stages || []).flatMap((stage) => stage.resolvedSubjects || [])
)

const previewUserGroups = computed(() => {
  const groups = new Map<string, { key: string; title: string; users: string[] }>()
  for (const subject of previewResolvedSubjects.value) {
    const rule = resolvePreviewSubjectRule(subject)
    const title =
      normalizeText(rule?.subjectDepartmentPath) ||
      normalizeText(rule?.subjectName) ||
      normalizeText(subject.subjectLabel) ||
      normalizeText(rule?.subjectLabel) ||
      '未标记来源'
    const stageLabel = rule?.stageType === 'APPROVAL' ? '批准' : '审核'
    const key = `${stageLabel}-${title}`
    if (!groups.has(key)) {
      groups.set(key, { key, title: `${stageLabel} · ${title}`, users: [] })
    }
    const group = groups.get(key)
    const userName = subject.userName || (subject.userId ? `用户#${subject.userId}` : '')
    if (group && userName && !group.users.includes(userName)) {
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
    previewResolvedSubjects.value
      .map((subject) => {
        const rule = resolvePreviewSubjectRule(subject)
        return rule ? buildRuleResolutionKey(rule) : ''
      })
      .filter(Boolean)
  )
  return formData.rules
    .filter((rule) => rule.active !== false)
    .filter((rule) => !resolvedKeys.has(buildRuleResolutionKey(rule)))
    .map((rule, index) => ({
      key: `${buildRuleResolutionKey(rule)}-${index}`,
      label:
        normalizeText(rule.subjectDepartmentPath) ||
        normalizeText(rule.subjectName) ||
        normalizeText(rule.subjectLabel) ||
        (rule.stageType === 'APPROVAL' ? '批准规则' : '审核规则'),
      reason: '该规则未解析到实际用户，请检查主体类型、对应主体与权限成员是否有效。'
    }))
})

const open = async (category: ControlledFileCategoryVO, mode: MatrixDialogMode = 'edit') => {
  currentCategory.value = category
  dialogMode.value = mode
  dialogTitle.value = buildDialogTitle(category.name)
  dialogVisible.value = true
  previewData.value = null
  previewError.value = ''
  applyMatrix()
  await ensureLookupData()
  await loadMatrix()
  if (mode !== 'create') {
    await refreshPreview()
  }
}

defineExpose({ open })
</script>

<style scoped>
.review-matrix-dialog {
  display: grid;
  gap: 14px;
}

.review-matrix-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.review-matrix-dialog__title {
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.review-matrix-dialog__meta {
  margin-top: 3px;
  color: #667085;
  font-size: 12px;
}

.review-matrix-dialog__actions {
  display: flex;
  gap: 10px;
}

.matrix-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.preview-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 16px 0 12px;
}

.preview-title {
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.preview-caption {
  color: #4b5563;
  font-size: 12px;
}

.preview-risk-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.review-matrix-dialog__marker {
  color: #172033;
  font-weight: 700;
}

.review-matrix-dialog__department-select {
  width: 100%;
}

.review-matrix-dialog__placeholder {
  color: #98a2b3;
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
  gap: 10px;
  margin-bottom: 10px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.view-matrix-user-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.view-matrix-user-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
  color: #263247;
  font-size: 12px;
}

.preview-download-summary {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.preview-download-summary__title {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.preview-download-summary__value {
  color: #263247;
  font-size: 13px;
  line-height: 20px;
}

.preview-download-summary__meta {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

@media (max-width: 960px) {
  .matrix-grid {
    grid-template-columns: 1fr;
  }

  .preview-title-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
