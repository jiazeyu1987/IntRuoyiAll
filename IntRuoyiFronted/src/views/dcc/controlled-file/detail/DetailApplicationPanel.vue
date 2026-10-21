<template>
  <ContentWrap>
    <el-button v-if="!opened" type="primary" plain @click="open">{{
      file.status === 'WORKING' ? '准备本次申请' : '发起局部 / 换版变更'
    }}</el-button>
    <section v-else v-loading="loading" aria-label="本次文件申请">
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <CustomApplicationMatrixEditor v-if="snapshot?.approvalSourceMode === 'APPLICATION_CUSTOM'"
        v-model="applicationCustomMatrix" v-model:selected-signoff-department-ids="selectedDepartments"
        approval-source-mode="APPLICATION_CUSTOM" :departments="departments" :users="approvalAccounts"
        :disabled="busy || submitted" :loading="loading" @update:model-value="refreshCustomPanel" @update:selected-signoff-department-ids="refreshCustomPanel" />
      <el-alert v-if="snapshot && applicationSelectionError" :title="applicationSelectionError" type="warning" :closable="false" />
      <el-alert v-if="preparationBlockers.length" :title="'默认审批路线有待处理项；提交时将按本次会签部门重新校验：' + preparationBlockers.join('；')" type="warning" :closable="false" />
      <template v-if="snapshot">
        <el-button v-if="canChangeWorkingType" data-testid="dcc-working-type-change-open" :disabled="busy || submitted || loading" @click="beginTypeChange">主动变更文件类型</el-button>
        <el-button v-if="newActionPreparationEligible && !preparedNewActionBaseline" :disabled="busy || submitted" @click="prepareNewActionBaseline">为本次升版准备审批配置</el-button>
        <el-alert v-if="preparedNewActionBaseline" title="本次新升版配置仅已预览，尚未保存；请明确保存本次申请草稿。" type="info" :closable="false" />
        <p
          >{{ file.sourceOriginalFileName || file.fileName || file.title }} ·
          {{ getDccVersionDisplayText(file.versionNo) }}</p
        >
        <p v-if="selectedIterationId">本次正文：{{ getDccVersionDisplayText(revisionOptions?.iterations.find(row => String(row.id) === selectedIterationId)?.versionNo) }} · 文件 #{{ selectedIterationId }}</p>
        <el-alert
          v-if="departmentChoiceMissing"
          title="草稿未保存会签部门，请重新明确选择本次部门。"
          type="warning"
          :closable="false"
        />
        <el-form label-width="120px" :disabled="busy || submitted">
          <el-form-item label="生效方式">
            <div>
              <el-radio-group v-model="effectiveMode" @change="handleEffectiveModeChange">
                <el-radio value="AFTER_STANDARDIZATION_SIGNATURE">文控审核后立即生效</el-radio>
                <el-radio value="DELAYED">延迟生效</el-radio>
                <el-radio v-if="effectiveMode === 'PRESET_DATE'" value="PRESET_DATE">按预设日期生效（历史申请）</el-radio>
              </el-radio-group>
              <div v-if="effectiveMode === null">生效方式未记录，请明确选择。</div>
              <div v-if="effectiveMode === 'DELAYED'">文控审核及必要培训完成后，由原编制人选择生效时间并电子签名确认。</div>
            </div>
          </el-form-item>
          <el-form-item v-if="effectiveMode === 'PRESET_DATE'" label="预设生效日期"
            ><el-date-picker v-model="effectiveDate" type="date" value-format="YYYY-MM-DD"
          /></el-form-item>
          <el-form-item v-if="snapshot.approvalSourceMode !== 'APPLICATION_CUSTOM'" label="会签部门">
            <el-select v-model="selectedDepartments" multiple filterable>
              <el-option
                v-for="department in departments"
                :key="String(department.id)"
                :label="department.name"
                :value="String(department.id)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="下发部门">
            <div>
              <el-select v-model="selectedDistributionDepartments" multiple filterable>
                <el-option v-for="department in departments" :key="String(department.id)" :label="department.name" :value="String(department.id)" />
              </el-select>
              <div v-if="selectedDistributionDepartments === null">草稿下发部门未记录，请明确本次名单。</div>
              <div>两份部门名单独立调整；受控且已生效后向所选下发部门的全部人员下发。</div>
              <el-button v-if="selectedDistributionDepartments === null" link @click="selectedDistributionDepartments = []">本次不选择下发部门</el-button>
            </div>
          </el-form-item>
          <el-form-item label="批准人">
            <div>
              {{ approverNames }}
              <div v-for="group in approvalGroups" :key="group.sourceNodeId" data-testid="dcc-application-approval-rule-group">原批准组 {{ group.sourceNodeId }}：{{ group.summary }}</div>
              <div v-for="(limitation, index) in approvalCapabilityLimitations" :key="index">运行能力说明：{{ limitation }}</div>
            </div>
          </el-form-item>
          <el-form-item label="培训"
            ><el-radio-group v-model="needTraining"
              ><el-radio :value="false">不需要</el-radio
              ><el-radio :value="true">需要（本次上传人上传线下培训记录）</el-radio></el-radio-group
            ></el-form-item
          >
        </el-form>
        <p>本次实际属性</p>
        <ProjectAttributesFields
          :model-value="snapshot.actual"
          :readonly="busy || submitted"
          @update:model-value="updateActual"
        />
        <el-button v-if="canEditSourceDraft" :disabled="busy || submitted" @click="saveAttributes">保存本次申请草稿</el-button>
        <el-button v-if="selectedIntent !== 'REPLACEMENT'" :disabled="busy" @click="restoreAttributes"
          >恢复{{ working || selectedIterationId ? '草稿原始' : '项目当前' }}默认属性</el-button
        >
        <el-collapse class="my-12px"
          ><el-collapse-item title="本次默认来源（只读）" name="defaults">
            <ProjectAttributesFields
              :model-value="snapshot.defaultSource"
              readonly
            /> </el-collapse-item
        ></el-collapse>
        <template v-if="initial">
          <el-form-item label="初始申请说明"
            ><el-input v-model="description" type="textarea" :disabled="busy"
          /></el-form-item>
          <el-button type="primary" :loading="busy" :disabled="!canSubmit || !applicationFacts || submitted" @click="submitInitial"
            >核对并提交初始申请</el-button
          >
        </template>
      </template>
        <DccRevisionPanel
          v-if="!initial && revisionOptions"
          :key="idempotencyKey"
          :baseline-id="revisionOptions.controlledBaselineId"
          :baseline-version-no="revisionOptions.baselineVersionNo"
          :partial-target="revisionOptions.partialTarget"
          :replacement-target="revisionOptions.replacementTarget"
          :idempotency-key="idempotencyKey"
          :application-facts="applicationFacts"
          :application-ready="Boolean(applicationFacts && !selectionLoading && canSubmit && !submitted && selectedIterationId)"
          :initial-selected-iteration-id="selectedIterationId"
          @selected-iteration-changed="selectIteration"
          @intent-changed="selectIntent"
          :facts="file"
          :iterations="revisionOptions.iterations"
          :can-partial="canPartial"
          :can-replacement="canReplacement"
          :checked-out-by="revisionOptions.checkedOutBy ?? undefined"
          :checked-out-by-name="revisionOptions.checkedOutByName ?? undefined"
          :checked-out-time="checkoutTime"
          :locked-reason="revisionOptions.lockedReason ?? undefined"
          :submit-revision="submitRevision"
        >
          <template #selected-body="{ selectedIterationId: bodyIterationId }">
            <el-button link type="primary" :disabled="selectionLoading" @click="openSelectedBodyPreview(bodyIterationId)"
              >预览所选正文</el-button
            >
          </template>
        </DccRevisionPanel>
    </section>
    <el-dialog v-model="typeChangeVisible" title="主动变更文件类型" width="760px" :close-on-click-modal="false" :close-on-press-escape="!typeChangeConfirming" :show-close="!typeChangeConfirming" @closed="cancelTypeChange">
      <el-alert v-if="typeChangeError" data-testid="dcc-working-type-change-error" :title="typeChangeError" type="error" :closable="false" />
      <p>仅变更本工作稿的已维护文件类型。原驳回记录保留；取消不改变草稿或其他已填内容。</p>
      <el-form label-width="120px" :disabled="typeChangeBusy">
        <el-form-item label="目标文件类型">
          <el-select v-model="typeChangeTarget" data-testid="dcc-working-type-change-target" filterable class="w-full" @change="invalidateTypeChangePreview">
            <el-option v-for="option in typeChangeOptions" :key="option.id" :value="option.id" :label="option.label" />
          </el-select>
        </el-form-item>
        <el-button data-testid="dcc-working-type-change-preview" :disabled="!typeChangeTarget" @click="loadTypeChangePreview">核对目标配置</el-button>
        <template v-if="typeChangePreview">
          <p>原执行配置（版本 {{ typeChangePreview.beforeMatrix.routeVersionNo }}）：{{ typeChangeRules(typeChangePreview.beforeMatrix) }}</p>
          <p data-testid="dcc-working-type-change-target-summary">目标：{{ typeChangePreview.targetTypePath }}；公共配置版本 {{ typeChangePreview.targetMatrix.routeVersionNo }}。{{ typeChangeRules(typeChangePreview.targetMatrix) }}</p>
          <el-form-item label="目标会签部门">
            <el-select v-model="typeChangeSignoff" multiple filterable class="w-full">
              <el-option v-for="department in departments" :key="department.id" :value="department.id" :label="department.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="本次下发部门">
            <el-select v-model="typeChangeDistribution" multiple filterable class="w-full">
              <el-option v-for="department in departments" :key="department.id" :value="department.id" :label="department.name" />
            </el-select>
          </el-form-item>
          <p v-if="typeChangeRosterDirty">名单已修改，请再次点击“核对目标配置”。</p>
        </template>
        <el-form-item label="变更原因" required>
          <el-input v-model="typeChangeReason" data-testid="dcc-working-type-change-reason" type="textarea" maxlength="512" />
        </el-form-item>
      </el-form>
      <p>确认前请先保存其他修改；成功后重新核对目标矩阵及两份独立部门名单。</p>
      <template #footer>
        <el-button data-testid="dcc-working-type-change-cancel" :disabled="typeChangeConfirming" @click="cancelTypeChange">取消</el-button>
        <el-button data-testid="dcc-working-type-change-confirm" type="primary" :loading="typeChangeBusy" :disabled="!typeChangePreview || typeChangeRosterDirty || !typeChangeReason.trim()" @click="confirmTypeChange">明确确认类型及配置</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="previewVisible" title="本次送审正文" width="88vw" destroy-on-close>
      <ProtectedPdfViewer v-if="previewSource" :preview-source="previewSource" title="本次送审正文" />
    </el-dialog>
  </ContentWrap>
</template>
<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount, markRaw } from 'vue'
import { ElMessageBox } from 'element-plus'
import { generateUUID } from '@/utils'
import { getSimpleDeptList } from '@/api/system/dept'
import { getSimpleUserList } from '@/api/system/user'
import { checkControlledFileRouteReadiness } from '@/api/dcc/controlledFile/workflow'
import { previewWorkingApplicationTypeChange, confirmWorkingApplicationTypeChange, type WorkingApplicationTypeChangePreview, type WorkingApplicationTypeChangeCommand, type ApprovalMatrixBaselineVO } from '@/api/dcc/controlledFile/workflow'
import { getFileTypeTaxonomyUploadOptions, type DccFileTypeTaxonomyUploadOption } from '@/api/dcc/controlledFile/fileTypeTaxonomies'
import { getProjectDefaults } from '@/api/dcc/controlledFile/projectAttributes'
import {
  getWorkingApplicationAttributes,
  getControlledFileReplacementAttributes,
  restoreWorkingApplicationAttributes,
  saveWorkingApplicationAttributes,
  getControlledFileRevisionOptions,
  type ControlledFileRevisionOptions,
  type WorkingApplicationSelection
} from '@/api/dcc/controlledFile/applicationRead'
import {
  submitControlledFileWorkingIteration,
  getControlledFile,
  type ControlledFileVO
} from '@/api/dcc/controlledFile/workflow'
import type { DccEffectiveMode } from '@/api/dcc/controlledFile/workflow'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { UploadApprovalMatrixState, isNewActionBaselinePreparationAllowed, isApprovalMatrixStaleError, matrixRuntimeBlockerMessage, refreshApprovalMatrixRejection } from '../upload/approval-matrix'
import type { ApprovalMatrixScope } from '../upload/approval-matrix'
import type { WorkingApplicationAttributes } from '@/api/dcc/controlledFile/applicationRead'
import { buildUploadEffectiveChoice, resolveUploadTodayDate } from '../upload/submitter'
import { getDccVersionDisplayText } from '../shared/version-display'
import CustomApplicationMatrixEditor from '../upload/CustomApplicationMatrixEditor.vue'
import type { ApplicationCustomMatrix } from '@/api/dcc/controlledFile/workflow'
import { readApplicationOrdinaryAttachmentExpectation } from '../revision/revision-model'
import { signoffIdentity, normalizeSignoffDepartments, normalizeDraftSignoffDepartments, normalizeDistributionDepartments, initialUploadDepartmentSelection,
  signoffDepartmentOptions, approvalUserOptions, resolvedUploadApprovers } from '../upload/signoff-departments'
import type { UploadApprovalAccount, UploadApprovalGroup } from '../upload/signoff-departments'
import ProjectAttributesFields from '../project-attributes/ProjectAttributesFields.vue'
import {
  validateAttributes,
  type AttributeSnapshot,
  type ProjectAttributes
} from '../project-attributes/state'
import DccRevisionPanel from '../revision/DccRevisionPanel.vue'
import { buildInitialCommand, type RevisionCommand, type RevisionIntent } from '../revision/revision-model'
import { isWorkflowId } from '../workflow/workflow-actions'
import ProtectedPdfViewer from '../view/index.vue'
import { buildDccControlledFilePreviewSource, type OnlineFilePreviewSource } from '@/api/common/filePreview'
import { formatControlledFileDateTime } from './presentation'

const props = defineProps<{ file: ControlledFileVO }>()
const emit = defineEmits<{ submitted: [fileId: string] }>()
const message = useMessage()
const opened = ref(false),
  loading = ref(false),
  busy = ref(false),
  error = ref('')
type PanelSnapshot = AttributeSnapshot & Pick<Partial<WorkingApplicationAttributes>, 'approvalSourceMode' | 'applicationCustomMatrix' | 'applicationContextKey' | 'attributeRound' | 'applicationCustomMatrixDigest'>
const isPublicMatrixExecution = (value: unknown) => value === 'PUBLIC_MATRIX_NATIVE_V1' || value === 'PUBLIC_MATRIX_NATIVE_V2'
const snapshot = ref<PanelSnapshot>(),
  selectedApplicationFile = ref<ControlledFileVO>(),
  revisionOptions = ref<ControlledFileRevisionOptions>()
const preparationBlockers = ref<string[]>([])
const effectiveMode = ref<DccEffectiveMode | null>(null), effectiveDate = ref<string | null>(null),
  needTraining = ref<boolean | undefined>(false),
  selectedDepartments = ref<string[] | null>(null),
  selectedDistributionDepartments = ref<string[] | null>(null),
  description = ref('')
const submitted = ref(false)
const applicationCustomMatrix = ref<ApplicationCustomMatrix | null>(null)
const refreshCustomPanel = () => { void refreshPanelMatrix(false).then(() => { error.value = '' }).catch(cause => { error.value = cause instanceof Error ? cause.message : String(cause) }) }
const approvalMatrixState = ref<UploadApprovalMatrixState>()
const newActionPreparationEligible = ref(false), preparedNewActionBaseline = ref(false)
const newActionPreparationFacts = ref<WorkingApplicationAttributes>()
const departmentChoiceMissing = ref(false)
const departments = ref<Array<{ id: string; name: string }>>([]),
  approvalAccounts = ref<UploadApprovalAccount[]>([]),
  approverNames = ref(''),
  approvalRule = ref(''),
  canSubmit = ref(false)
const approvalGroups = ref<UploadApprovalGroup[]>([]), approvalCapabilityLimitations = ref<string[]>([])
const idempotencyKey = ref(''),
  previewSource = ref<OnlineFilePreviewSource>()
const selectedIterationId = ref(''), selectionLoading = ref(false)
const selectedIntent = ref<RevisionIntent>()
const snapshotIntent = ref<RevisionIntent>()
const canEditSourceDraft = computed(() => initial.value || (Boolean(selectedIterationId.value) && selectedIntent.value !== 'REPLACEMENT' && snapshotIntent.value !== 'REPLACEMENT'))
let selectionGeneration = 0
let generation = 0
onBeforeUnmount(() => {
  generation++
})
const working = computed(() => props.file.status === 'WORKING')
const initial = computed(() => snapshot.value?.applicationType === 'UPLOAD')
const canPartial = computed(
  () => revisionOptions.value?.iterations.some((row) => row.canPartial) === true
)
const canReplacement = computed(
  () =>
    revisionOptions.value?.iterations.some((row) => row.canReplacement) === true
)
const checkoutTime = computed(() =>
  revisionOptions.value?.checkedOutTime
    ? formatControlledFileDateTime(revisionOptions.value.checkedOutTime)
    : undefined
)
const previewVisible = computed({
  get: () => Boolean(previewSource.value),
  set: (value) => {
    if (!value) previewSource.value = undefined
  }
})
const openSelectedBodyPreview = (id: string) => {
  previewSource.value = undefined
  try {
    const file = selectedApplicationFile.value
    if (selectionLoading.value || !file || String(file.id) !== String(id)
      || selectedIterationId.value !== String(id) || typeof file.versionNo !== 'string')
      throw new Error('所选正文详情尚未核验，请重新选择本次工作正文')
    previewSource.value = buildDccControlledFilePreviewSource(String(file.id), {
      intent: 'AUTHORING', controlledFileId: String(file.id), expectedVersionNo: file.versionNo
    })
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause)
  }
}
const applicationProjection = computed(() => {
  if (!snapshot.value || typeof needTraining.value !== 'boolean' || !Array.isArray(selectedDepartments.value)
    || !Array.isArray(selectedDistributionDepartments.value)) return { facts: undefined, error: '请完成本次属性、会签部门、下发部门和培训选择；未记录值不会使用默认值补齐' }
  try {
    const effect = buildUploadEffectiveChoice(effectiveMode.value, effectiveDate.value, resolveUploadTodayDate())
    return { facts: {
        projectAttributes: snapshot.value.actual,
        selectedSignoffDepartmentIds: normalizeSignoffDepartments(selectedDepartments.value)!,
        selectedDistributionDepartmentIds: normalizeDistributionDepartments(selectedDistributionDepartments.value),
        needTraining: needTraining.value,
        ...effect
      }, error: '' }
  } catch (cause) { return { facts: undefined, error: cause instanceof Error ? cause.message : String(cause) } }
})
const applicationFacts = computed(() => applicationProjection.value.facts)
const applicationSelectionError = computed(() => applicationProjection.value.error)
const typeChangeVisible = ref(false), typeChangeBusy = ref(false), typeChangeError = ref(''), typeChangeTarget = ref(''), typeChangeReason = ref('')
const typeChangeConfirming = ref(false)
const typeChangeOptions = ref<Array<{ id: string; label: string }>>([]), typeChangePreview = ref<WorkingApplicationTypeChangePreview>()
const typeChangeSignoff = ref<string[]>([]), typeChangeDistribution = ref<string[]>([]), typeChangeRequestId = ref(''), savedTypeChangeFormKey = ref('')
let typeChangeSequence = 0
const canChangeWorkingType = computed(() => working.value && initial.value && props.file.changeType === 'NEW' && Boolean(props.file.predecessorControlledFileId)
  && snapshot.value?.approvalSourceMode !== 'APPLICATION_CUSTOM')
const typeChangeRosterDirty = computed(() => !typeChangePreview.value || departmentSelectionKey(typeChangeSignoff.value) !== departmentSelectionKey(typeChangePreview.value.selectedSignoffDepartmentIds)
  || departmentSelectionKey(typeChangeDistribution.value) !== departmentSelectionKey(typeChangePreview.value.selectedDistributionDepartmentIds))
const typeChangeFormKey = () => JSON.stringify([snapshot.value?.actual, effectiveMode.value, effectiveDate.value, needTraining.value,
  departmentSelectionKey(selectedDepartments.value), departmentSelectionKey(selectedDistributionDepartments.value), description.value])
const typeChangeRules = (matrix: ApprovalMatrixBaselineVO) => matrix.nodes.map(node =>
  `${node.stageType === 'SIGNOFF' ? '会签' : node.stageType === 'APPROVAL' ? '批准' : '文控'}：${node.subjectName || node.subjectLabel || '主体未记录'}`).join('；')
const cancelTypeChange = () => { if (typeChangeConfirming.value) return; typeChangeSequence++; typeChangeVisible.value = false; typeChangeBusy.value = false }
const invalidateTypeChangePreview = () => { typeChangeSequence++; typeChangePreview.value = undefined; typeChangeError.value = ''; typeChangeSignoff.value = []; typeChangeDistribution.value = []; typeChangeRequestId.value = '' }
const beginTypeChange = async () => {
  if (!canChangeWorkingType.value || busy.value || submitted.value) return
  const token = ++typeChangeSequence, context = generation, fileId = String(props.file.id)
  typeChangeVisible.value = true; typeChangeBusy.value = true; typeChangeError.value = ''; typeChangeTarget.value = ''; typeChangeReason.value = ''; typeChangePreview.value = undefined
  typeChangeSignoff.value = []; typeChangeDistribution.value = []
  try {
    const [rows, saved] = await Promise.all([getFileTypeTaxonomyUploadOptions(), getWorkingApplicationAttributes(props.file.id)])
    if (token !== typeChangeSequence || context !== generation || fileId !== String(props.file.id)) return
    if (String(saved.controlledFileId) !== fileId || saved.applicationType !== 'UPLOAD') throw new Error('原工作稿属性身份不一致，请重新读取')
    savedTypeChangeFormKey.value = JSON.stringify([saved.actual, saved.effectiveMode, saved.effectiveDate, saved.needTraining,
      departmentSelectionKey(saved.selectedSignoffDepartmentIds), departmentSelectionKey(saved.selectedDistributionDepartmentIds), saved.changeDescription || ''])
    const all: DccFileTypeTaxonomyUploadOption[] = []
    const collect = (items: DccFileTypeTaxonomyUploadOption[]) => { for (const row of items) { all.push(row); if (row.children) collect(row.children) } }
    collect(rows)
    const byId = new Map(all.map(row => [String(row.id), row]))
    typeChangeOptions.value = all.filter(row => row.levelNo === 5 && row.active && row.approvalSourceMode === 'PUBLIC_MATRIX' && row.uploadBlocker === null && String(row.id) !== String(props.file.fileTypeTaxonomyId))
      .map(row => {
        const names = [row.name], seen = new Set([String(row.id)]); let parentId = row.parentId
        while (parentId != null && String(parentId) !== '0') {
          const parent = byId.get(String(parentId)); if (!parent || seen.has(String(parentId))) throw new Error('目标类型路径缺失或循环')
          seen.add(String(parentId)); names.unshift(parent.name); parentId = parent.parentId
        }
        return { id: String(row.id), label: names.join(' / ') }
      })
    if (!typeChangeOptions.value.length) throw new Error('没有可用的目标公共类型')
  } catch (cause) { if (token === typeChangeSequence && context === generation) typeChangeError.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === typeChangeSequence && context === generation) typeChangeBusy.value = false }
}
const loadTypeChangePreview = async () => {
  if (!typeChangeTarget.value || typeChangeBusy.value) return
  const token = ++typeChangeSequence, context = generation, fileId = String(props.file.id), reviewLists = Boolean(typeChangePreview.value)
  typeChangeBusy.value = true; typeChangeError.value = ''
  try {
    let response = await previewWorkingApplicationTypeChange(props.file.id, { targetFileTypeTaxonomyId: typeChangeTarget.value,
      ...(reviewLists ? { selectedSignoffDepartmentIds: [...typeChangeSignoff.value], selectedDistributionDepartmentIds: [...typeChangeDistribution.value] }
        : {}) })
    if (token !== typeChangeSequence || context !== generation || fileId !== String(props.file.id)) return
    if (!reviewLists) {
      if (String(response.controlledFileId) !== fileId || String(response.targetTaxonomyId) !== typeChangeTarget.value || !response.readiness.ready)
        throw new Error('目标类型或审批路线未核对通过')
      const first = response, defaults = [...first.selectedSignoffDepartmentIds]
      response = await previewWorkingApplicationTypeChange(props.file.id, { targetFileTypeTaxonomyId: typeChangeTarget.value,
        selectedSignoffDepartmentIds: defaults, selectedDistributionDepartmentIds: [...defaults] })
      if (response.targetMatrix.matrixDigest !== first.targetMatrix.matrixDigest || response.sourceSha256 !== first.sourceSha256
        || response.sourceAttributeRound !== first.sourceAttributeRound) throw new Error('核对期间工作稿或目标配置已变化，请重新核对')
    }
    if (token !== typeChangeSequence || context !== generation || fileId !== String(props.file.id)) return
    if (String(response.controlledFileId) !== fileId || String(response.targetTaxonomyId) !== typeChangeTarget.value || !response.readiness.ready)
      throw new Error('目标类型或审批路线未核对通过')
    if (!response.beforeMatrix?.nodes?.length || !response.targetMatrix?.nodes?.length || !Number.isInteger(response.sourceAttributeRound)
      || response.sourceAttributeRound <= 0 || !/^[a-f0-9]{64}$/.test(response.sourceSha256) || !/^[a-f0-9]{64}$/.test(response.targetMatrix.matrixDigest)
      || String(response.beforeMatrix.categoryId) !== String(response.sourceCategoryId) || String(response.beforeMatrix.fileTypeTaxonomyId) !== String(response.sourceTaxonomyId)
      || String(response.targetMatrix.categoryId) !== String(response.targetCategoryId) || String(response.targetMatrix.fileTypeTaxonomyId) !== String(response.targetTaxonomyId))
      throw new Error('目标类型配置或原工作稿正式数据不完整，请重新核对')
    typeChangePreview.value = response; typeChangeSignoff.value = [...response.selectedSignoffDepartmentIds]; typeChangeDistribution.value = [...response.selectedDistributionDepartmentIds]
    typeChangeRequestId.value = generateUUID()
  } catch (cause) { if (token === typeChangeSequence && context === generation) typeChangeError.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === typeChangeSequence && context === generation) typeChangeBusy.value = false }
}
const confirmTypeChange = async () => {
  const preview = typeChangePreview.value
  if (!preview || typeChangeBusy.value) return
  if (typeChangeRosterDirty.value || !typeChangeReason.value.trim()) { typeChangeError.value = '请核对名单并填写变更原因'; return }
  if (typeChangeFormKey() !== savedTypeChangeFormKey.value) { typeChangeError.value = '请先保存其他修改，再确认文件类型变更；取消会保留当前已填内容'; return }
  const token = typeChangeSequence, context = generation, fileId = String(props.file.id)
  const command: WorkingApplicationTypeChangeCommand = { targetFileTypeTaxonomyId: preview.targetTaxonomyId, confirmed: true, requestId: typeChangeRequestId.value,
    expectedSourceAttributeRound: preview.sourceAttributeRound, expectedSourceCategoryId: preview.sourceCategoryId, expectedSourceTaxonomyId: preview.sourceTaxonomyId,
    expectedSourceSha256: preview.sourceSha256, expectedSourceMatrixDigest: preview.sourceMatrixDigest,
    expectedApprovalRouteId: preview.targetMatrix.routeId, expectedApprovalRouteVersionNo: preview.targetMatrix.routeVersionNo, expectedApprovalMatrixDigest: preview.targetMatrix.matrixDigest,
    selectedSignoffDepartmentIds: [...preview.selectedSignoffDepartmentIds], selectedDistributionDepartmentIds: [...preview.selectedDistributionDepartmentIds], changeReason: typeChangeReason.value.trim() }
  typeChangeBusy.value = true; typeChangeConfirming.value = true; typeChangeError.value = ''
  try {
    const response = await confirmWorkingApplicationTypeChange(props.file.id, command)
    if (context !== generation || fileId !== String(props.file.id)) { message.warning('文件类型变更已保存，请重新打开原工作稿核对'); return }
    if (String(response.controlledFileId) !== fileId || String(response.targetTaxonomyId) !== command.targetFileTypeTaxonomyId
      || response.targetMatrix.matrixDigest !== command.expectedApprovalMatrixDigest) throw new Error('变更已提交，但正式回显身份不一致，请重新读取原工作稿')
    message.success('文件类型已变更，请核对目标矩阵和部门名单')
    window.location.reload()
  } catch (cause) { if (token === typeChangeSequence && context === generation) typeChangeError.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === typeChangeSequence && context === generation) { typeChangeBusy.value = false; typeChangeConfirming.value = false } }
}
const handleEffectiveModeChange = () => {
  if (effectiveMode.value === 'AFTER_STANDARDIZATION_SIGNATURE' || effectiveMode.value === 'DELAYED') effectiveDate.value = null
}
watch(
  () => [String(props.file.id), props.file.status, props.file.processInstanceId],
  () => {
    generation++
    typeChangeSequence++; typeChangeVisible.value = false; typeChangeBusy.value = false; typeChangeConfirming.value = false; typeChangePreview.value = undefined
    opened.value = false
    loading.value = false
    busy.value = false
    submitted.value = false
    effectiveMode.value = null; effectiveDate.value = null
    selectedDepartments.value = null; selectedDistributionDepartments.value = null
    approvalGroups.value = []; approvalCapabilityLimitations.value = []
    approvalMatrixState.value = undefined
    newActionPreparationEligible.value = false; preparedNewActionBaseline.value = false; newActionPreparationFacts.value = undefined
    error.value = ''
    preparationBlockers.value = []
    snapshot.value = undefined
    selectedApplicationFile.value = undefined
    revisionOptions.value = undefined
    selectedIterationId.value = ''; selectedIntent.value = undefined; snapshotIntent.value = undefined; selectionGeneration++; selectionLoading.value = false
    previewSource.value = undefined
  },
  { flush: 'sync' }
)
const applicationMatrixScope = (file: ControlledFileVO, action: 'NEW' | 'REVISION'): ApprovalMatrixScope => ({
  requesterId: file.requesterId == null ? undefined : String(file.requesterId),
  tenantId: String(getVisitTenantId() ?? getTenantId()), projectId: String(file.dccProjectCodeId),
  categoryId: String(file.categoryId), taxonomyId: String(file.fileTypeTaxonomyId), actionType: action,
  controlledFileId: String(file.id), applicationContextKey: idempotencyKey.value
})

const displaySavedNewAction = async (saved: WorkingApplicationAttributes, file: ControlledFileVO,
  matrix: UploadApprovalMatrixState, token: number, selectionToken: number) => {
  const [deptRows, userRows] = await Promise.all([getSimpleDeptList(), getSimpleUserList()])
  if (token !== generation || selectionToken !== selectionGeneration) return
  if (!revisionOptions.value) {
    const baselineId = file.revisionBaseActiveControlledFileId
    if (!baselineId) throw new Error('新升版正文缺少正式受控基线身份')
    const options = await getControlledFileRevisionOptions(baselineId)
    if (token !== generation || selectionToken !== selectionGeneration) return
    if (String(options.masterId) !== String(file.masterId) || String(options.controlledBaselineId) !== String(baselineId))
      throw new Error('新升版准备与当前逻辑文件或受控基线不一致')
    revisionOptions.value = options
  }
  departments.value = signoffDepartmentOptions(deptRows); approvalAccounts.value = approvalUserOptions(userRows)
  snapshot.value = JSON.parse(JSON.stringify(saved)); selectedApplicationFile.value = file
  selectedDepartments.value = normalizeDraftSignoffDepartments(saved.selectedSignoffDepartmentIds)
  selectedDistributionDepartments.value = saved.selectedDistributionDepartmentIds === null ? null : normalizeDistributionDepartments(saved.selectedDistributionDepartmentIds)
  effectiveMode.value = saved.effectiveMode; effectiveDate.value = saved.effectiveDate
  needTraining.value = saved.needTraining ?? undefined; description.value = saved.changeDescription || ''
  selectedIterationId.value = String(file.id)
  approvalMatrixState.value = matrix; newActionPreparationFacts.value = JSON.parse(JSON.stringify(saved))
  newActionPreparationEligible.value = true; preparedNewActionBaseline.value = false
  canSubmit.value = false; error.value = saved.unavailableReason || '本次新升版尚未明确准备审批配置'
}

const displaySavedCustom = async (saved: WorkingApplicationAttributes, file: ControlledFileVO, context: number, selection: number) => {
  if (!saved.applicationContextKey || !Number.isInteger(saved.attributeRound) || !saved.applicationCustomMatrixDigest)
    throw new Error('已保存自定义申请缺少真实上下文、轮次或配置摘要')
  idempotencyKey.value = saved.applicationContextKey
  applicationCustomMatrix.value = saved.applicationCustomMatrix ?? null
  snapshot.value = JSON.parse(JSON.stringify(saved)); selectedApplicationFile.value = file
  selectedDepartments.value = saved.selectedSignoffDepartmentIds === null ? null : [...saved.selectedSignoffDepartmentIds]
  selectedDistributionDepartments.value = saved.selectedDistributionDepartmentIds === null ? null : [...saved.selectedDistributionDepartmentIds]
  effectiveMode.value = saved.effectiveMode; effectiveDate.value = saved.effectiveDate; needTraining.value = saved.needTraining ?? undefined; description.value = saved.changeDescription || ''
  if (saved.unavailableReason?.startsWith('APPLICATION_APPROVAL_SOURCE_CHANGED')) {
    const [deptRows, users] = await Promise.all([getSimpleDeptList(), getSimpleUserList()])
    if (context !== generation || selection !== selectionGeneration) return
    departments.value = signoffDepartmentOptions(deptRows); approvalAccounts.value = approvalUserOptions(users); canSubmit.value = false; error.value = saved.unavailableReason
    return
  }
  const matrix = new UploadApprovalMatrixState({ ...applicationMatrixScope(file, saved.applicationType === 'UPLOAD' ? 'NEW' : 'REVISION'), approvalSourceMode: 'APPLICATION_CUSTOM', attributeRound: saved.attributeRound ?? undefined }, null,
    saved.executionContract === 'PUBLIC_MATRIX_NATIVE_V1' || saved.executionContract === 'PUBLIC_MATRIX_NATIVE_V2' ? saved : undefined)
  matrix.setApplicationCustomMatrix(applicationCustomMatrix.value); approvalMatrixState.value = markRaw(matrix)
  const request = matrix.begin(saved.selectedSignoffDepartmentIds ?? [])
  const [deptRows, users, response] = await Promise.all([getSimpleDeptList(), getSimpleUserList(), checkControlledFileRouteReadiness(request.request)])
  if (context !== generation || selection !== selectionGeneration || matrix !== approvalMatrixState.value || !matrix.accept(request, response)) return
  snapshot.value = JSON.parse(JSON.stringify(saved)); selectedApplicationFile.value = file
  departments.value = signoffDepartmentOptions(deptRows); approvalAccounts.value = approvalUserOptions(users)
  selectedDepartments.value = saved.selectedSignoffDepartmentIds === null ? null : [...saved.selectedSignoffDepartmentIds]
  selectedDistributionDepartments.value = saved.selectedDistributionDepartmentIds === null ? null : [...saved.selectedDistributionDepartmentIds]
  effectiveMode.value = saved.effectiveMode; effectiveDate.value = saved.effectiveDate; needTraining.value = saved.needTraining ?? undefined; description.value = saved.changeDescription || ''
  if (response.nodes.some(node => node.stageCode === 'MATRIX_APPROVAL')) { const facts = resolvedUploadApprovers(response, approvalAccounts.value); approverNames.value = facts.accounts.map(row => row.name).join('、'); approvalRule.value = facts.rule }
  approvalGroups.value = []; approvalCapabilityLimitations.value = []; canSubmit.value = saved.canSubmit && response.ready
  if (saved.unavailableReason) error.value = saved.unavailableReason
  if (saved.applicationType === 'REVISION') {
    if (!file.revisionBaseActiveControlledFileId) throw new Error('草稿缺少正式受控基线身份')
    const options = await getControlledFileRevisionOptions(file.revisionBaseActiveControlledFileId)
    if (context !== generation || selection !== selectionGeneration) return
    if (String(options.masterId) !== String(file.masterId)) throw new Error('升版准备与当前逻辑文件不一致')
    revisionOptions.value = options; selectedIterationId.value = String(file.id)
  }
}
const prepareNewActionBaseline = async () => {
  if (busy.value || submitted.value || !newActionPreparationEligible.value || preparedNewActionBaseline.value) return
  const facts = newActionPreparationFacts.value, file = selectedApplicationFile.value
  if (!facts || !file || !idempotencyKey.value) { error.value = '本次升版准备上下文未核验'; return }
  const token = generation, selectionToken = selectionGeneration
  const scope = applicationMatrixScope(file, 'REVISION')
  const matrix = new UploadApprovalMatrixState(scope)
  const current = () => token === generation && selectionToken === selectionGeneration
    && idempotencyKey.value === scope.applicationContextKey && selectedApplicationFile.value
    && JSON.stringify(applicationMatrixScope(selectedApplicationFile.value, 'REVISION')) === JSON.stringify(scope)
  busy.value = true; error.value = ''
  try {
    const request = matrix.prepareNewActionBaseline(facts, selectedDepartments.value ?? undefined)
    const response = await checkControlledFileRouteReadiness(request.request)
    if (!current() || !matrix.accept(request, response)) return
    if (!response.ready && !response.nodes.length) throw new Error(matrixRuntimeBlockerMessage(response))
    const approvers = resolvedUploadApprovers(response, approvalAccounts.value)
    const defaults = initialUploadDepartmentSelection(response)
    const previewSignoff = selectedDepartments.value === null ? defaults.selectedSignoffDepartmentIds : selectedDepartments.value
    const previewDistribution = selectedDistributionDepartments.value === null ? defaults.selectedDistributionDepartmentIds : selectedDistributionDepartments.value
    if ([...previewSignoff, ...previewDistribution].some(id => !departments.value.some(row => row.id === id)))
      throw new Error('本次升版准备返回的部门不在正式启用目录中，请核对配置')
    await ElMessageBox.confirm([
      '这是为本次新升版选择的审批配置，并非恢复原上传申请基线；确认这里只保留预览状态，仍须明确保存工作稿。',
      ...matrix.summary().map(row => `${row.label}：${row.value}`),
      ...approvers.groups.map(group => `原批准组 ${group.sourceNodeId}：${group.summary}`),
      `本次会签部门：${previewSignoff.map(id => departments.value.find(row => row.id === id)!.name).join('、') || '未填写'}`,
      `本次下发部门：${previewDistribution.map(id => departments.value.find(row => row.id === id)!.name).join('、') || '未选择下发部门'}；受控且已生效后向所选部门全部人员下发`,
      ...approvers.capabilityLimitations.map(value => `运行能力说明：${value}`)
    ].join('\n'), '为本次升版准备审批配置', { confirmButtonText: '保留本次准备', cancelButtonText: '取消', type: 'warning' })
    if (!current() || newActionPreparationFacts.value?.controlledFileId !== facts.controlledFileId) return
    if (selectedDepartments.value === null) selectedDepartments.value = defaults.selectedSignoffDepartmentIds
    if (selectedDistributionDepartments.value === null) selectedDistributionDepartments.value = defaults.selectedDistributionDepartmentIds
    approvalMatrixState.value = matrix; preparedNewActionBaseline.value = true
    approverNames.value = approvers.accounts.map(account => account.name).join('、'); approvalRule.value = approvers.rule
    approvalGroups.value = approvers.groups; approvalCapabilityLimitations.value = approvers.capabilityLimitations
    preparationBlockers.value = response.blockers.map(blocker => blocker.message)
    canSubmit.value = false
  } catch (cause) {
    if (token === generation && selectionToken === selectionGeneration && cause !== 'cancel' && cause !== 'close')
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (token === generation) busy.value = false }
}

const open = async () => {
  if (busy.value || submitted.value) return
  const token = ++generation,
    file = props.file
  opened.value = true
  loading.value = true
  error.value = ''
  preparationBlockers.value = []
  canSubmit.value = false
  try {
    if (!file.dccProjectCodeId) throw new Error('文件未绑定正式项目，无法准备申请')
    const saved = working.value ? await getWorkingApplicationAttributes(file.id) : undefined
    if (token !== generation) return
    const action = saved?.applicationType || 'REVISION'
    if (saved && (String(saved.projectId) !== String(file.dccProjectCodeId) || String(saved.controlledFileId) !== String(file.id)))
      throw new Error('草稿属性与当前项目不一致')
    if (saved?.approvalSourceMode === 'APPLICATION_CUSTOM') { await displaySavedCustom(saved, file, token, selectionGeneration); return }
    if (saved && saved.draftApprovalMatrix == null) {
      idempotencyKey.value = generateUUID()
      const scope = applicationMatrixScope(file, saved.applicationType === 'UPLOAD' ? 'NEW' : 'REVISION')
      if (!isNewActionBaselinePreparationAllowed(saved, scope)) throw new Error('原申请审批矩阵基线未记录，不能用当前配置替代，请核对正式草稿来源')
      await displaySavedNewAction(saved, file, new UploadApprovalMatrixState(scope), token, selectionGeneration)
      return
    }
    const defaults = saved
      ? saved.defaultSource
      : await getProjectDefaults(file.dccProjectCodeId, action)
    if (token !== generation) return
    idempotencyKey.value = generateUUID()
    const matrix = new UploadApprovalMatrixState({ tenantId: String(getVisitTenantId() ?? getTenantId()),
      projectId: String(file.dccProjectCodeId), categoryId: String(file.categoryId), taxonomyId: String(file.fileTypeTaxonomyId),
      actionType: action === 'UPLOAD' ? 'NEW' : 'REVISION', controlledFileId: saved ? String(file.id) : undefined,
      applicationContextKey: idempotencyKey.value }, saved?.draftApprovalMatrix,
      saved && isPublicMatrixExecution(saved.executionContract) ? saved : undefined)
    approvalMatrixState.value = matrix
    const matrixRequest = matrix.begin(saved?.selectedSignoffDepartmentIds ?? undefined)
    const [deptRows, users, readiness] = await Promise.all([
      getSimpleDeptList(),
      getSimpleUserList(),
      checkControlledFileRouteReadiness(matrixRequest.request)
    ])
    if (token !== generation) return
    if (!matrix.accept(matrixRequest, readiness)) return
    selectedApplicationFile.value = saved ? file : undefined
    preparationBlockers.value = readiness.blockers.map(blocker => blocker.message)
    if (!readiness.ready && !readiness.nodes.length) throw new Error(matrixRuntimeBlockerMessage(readiness))
    const initialDepartments = saved ? undefined : initialUploadDepartmentSelection(readiness)
    const frozenNodes = saved && file.routeSnapshots?.length ? file.routeSnapshots : undefined
    approvalAccounts.value = approvalUserOptions(users)
    const approvers = resolvedUploadApprovers(isPublicMatrixExecution(readiness.executionContract) ? readiness : frozenNodes ? { nodes: frozenNodes } : readiness, approvalAccounts.value)
    approvalGroups.value = approvers.groups; approvalCapabilityLimitations.value = approvers.capabilityLimitations
    snapshot.value = JSON.parse(
      JSON.stringify(
        saved || {
          projectId: String(file.dccProjectCodeId),
          applicationType: action,
          defaultSource: defaults,
          actual: defaults
        }
      )
    )
    departments.value = signoffDepartmentOptions(deptRows)
    departmentChoiceMissing.value = Boolean(saved && saved.selectedSignoffDepartmentIds == null)
    selectedDepartments.value = saved
      ? normalizeDraftSignoffDepartments(saved.selectedSignoffDepartmentIds)
      : initialDepartments!.selectedSignoffDepartmentIds
    selectedDistributionDepartments.value = saved
      ? (saved.selectedDistributionDepartmentIds === null ? null : normalizeDistributionDepartments(saved.selectedDistributionDepartmentIds))
      : initialDepartments!.selectedDistributionDepartmentIds
    effectiveMode.value = saved ? saved.effectiveMode : null
    effectiveDate.value = saved ? saved.effectiveDate : null
    needTraining.value = saved ? (saved.needTraining ?? undefined) : false
    description.value = saved?.changeDescription || ''
    approverNames.value = approvers.accounts.map(account => account.name).join('、')
    approvalRule.value = approvers.rule
    if (action === 'REVISION') {
      const baselineId = working.value ? file.revisionBaseActiveControlledFileId : file.id
      if (!baselineId) throw new Error('草稿缺少正式受控基线身份')
      const options = await getControlledFileRevisionOptions(baselineId)
      if (token !== generation) return
      if (String(options.masterId) !== String(file.masterId))
        throw new Error('升版准备与当前逻辑文件不一致')
      revisionOptions.value = options
      if (saved) selectedIterationId.value = String(file.id)
    }
    canSubmit.value = saved?.canSubmit === true
    if (saved?.unavailableReason) error.value = saved.unavailableReason
  } catch (cause) {
    if (token === generation) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (token === generation) loading.value = false
  }
}
const selectIntent = async (intent: RevisionIntent | undefined) => {
  if (busy.value || submitted.value) return
  selectedIntent.value = intent
  if (selectedIterationId.value) await selectIteration(selectedIterationId.value)
}
const selectIteration = async (id: string) => {
  if (busy.value || submitted.value) return
  const token = ++selectionGeneration, context = generation
  const intent = selectedIntent.value === 'REPLACEMENT' ? 'REPLACEMENT' : 'PARTIAL'
  selectedIterationId.value = id
  newActionPreparationEligible.value = false; preparedNewActionBaseline.value = false; newActionPreparationFacts.value = undefined
  snapshot.value = undefined; selectedApplicationFile.value = undefined; canSubmit.value = false; error.value = ''; selectionLoading.value = true; previewSource.value = undefined
  try {
    const body = revisionOptions.value?.iterations.find(row => String(row.id) === id)
    if (!body || !(body.canPartial || body.canReplacement)) throw new Error('请选择合法的本次工作正文')
    if (!(intent === 'REPLACEMENT' ? body.canReplacement : body.canPartial)) throw new Error('当前正文无所选变更操作资格')
    const [saved, selectedFile] = await Promise.all([
      intent === 'REPLACEMENT' ? getControlledFileReplacementAttributes(id, revisionOptions.value!.controlledBaselineId) : getWorkingApplicationAttributes(id),
      getControlledFile(id)
    ])
    if (context !== generation || token !== selectionGeneration) return
    if (String(saved.controlledFileId) !== id || String(saved.projectId) !== String(props.file.dccProjectCodeId) || saved.applicationType !== 'REVISION')
      throw new Error('所选正文的申请属性身份或类型不一致')
    if (String(selectedFile.id) !== id || String(selectedFile.masterId) !== String(props.file.masterId)
      || String(selectedFile.dccProjectCodeId) !== saved.projectId) throw new Error('所选正文详情与本次申请身份不一致')
    selectedApplicationFile.value = selectedFile
    if (saved.approvalSourceMode === 'APPLICATION_CUSTOM') { snapshotIntent.value = intent; await displaySavedCustom(saved, selectedFile, context, token); return }
    if (saved.draftApprovalMatrix == null) {
      const scope = applicationMatrixScope(selectedFile, 'REVISION')
      if (!isNewActionBaselinePreparationAllowed(saved, scope)) throw new Error('所选工作稿原审批矩阵基线未记录，请核对正式保存来源')
      snapshotIntent.value = intent
      await displaySavedNewAction(saved, selectedFile, new UploadApprovalMatrixState(scope), context, token)
      return
    }
    const matrix = new UploadApprovalMatrixState({ tenantId: String(getVisitTenantId() ?? getTenantId()),
      projectId: saved.projectId, categoryId: String(selectedFile.categoryId), taxonomyId: String(selectedFile.fileTypeTaxonomyId),
      controlledFileId: id, actionType: 'REVISION', applicationContextKey: idempotencyKey.value }, saved.draftApprovalMatrix,
      isPublicMatrixExecution(saved.executionContract) ? saved : undefined)
    approvalMatrixState.value = matrix
    const matrixRequest = matrix.begin(saved.selectedSignoffDepartmentIds ?? undefined)
    const matrixReadiness = await checkControlledFileRouteReadiness(matrixRequest.request)
    if (context !== generation || token !== selectionGeneration || !matrix.accept(matrixRequest, matrixReadiness)) return
    if (isPublicMatrixExecution(matrixReadiness.executionContract) || selectedFile.routeSnapshots?.length) {
      const approvers = resolvedUploadApprovers(isPublicMatrixExecution(matrixReadiness.executionContract) ? matrixReadiness : { nodes: selectedFile.routeSnapshots }, approvalAccounts.value)
      approverNames.value = approvers.accounts.map(account => account.name).join('、')
      approvalRule.value = approvers.rule
      approvalGroups.value = approvers.groups; approvalCapabilityLimitations.value = approvers.capabilityLimitations
    }
    snapshot.value = JSON.parse(JSON.stringify(saved))
    snapshotIntent.value = intent
    selectedDepartments.value = normalizeDraftSignoffDepartments(saved.selectedSignoffDepartmentIds)
    selectedDistributionDepartments.value = saved.selectedDistributionDepartmentIds === null ? null : normalizeDistributionDepartments(saved.selectedDistributionDepartmentIds)
    departmentChoiceMissing.value = saved.selectedSignoffDepartmentIds === null
    effectiveMode.value = saved.effectiveMode; effectiveDate.value = saved.effectiveDate; needTraining.value = saved.needTraining ?? undefined
    description.value = saved.changeDescription || ''; canSubmit.value = saved.canSubmit
    if (saved.unavailableReason) error.value = saved.unavailableReason
  } catch (cause) {
    if (context === generation && token === selectionGeneration) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (context === generation && token === selectionGeneration) selectionLoading.value = false
  }
}
const updateActual = (value: ProjectAttributes) => {
  if (snapshot.value && !busy.value) snapshot.value.actual = value
}
const departmentSelectionKey = (ids: Array<string | number> | null | undefined) =>
  JSON.stringify(ids == null ? ids : ids.map(signoffIdentity).sort())
const saveAttributes = async () => {
  if (selectedIntent.value === 'REPLACEMENT' || snapshotIntent.value === 'REPLACEMENT') { error.value = '独立换版申请不能保存或恢复原工作草稿'; return }
  if (!snapshot.value || busy.value || submitted.value) return
  const token = generation, selectionToken = selectionGeneration
  busy.value = true
  error.value = ''
  let saved = false
  try {
    const sourceId = selectedIterationId.value || props.file.id
    const actual: ProjectAttributes = JSON.parse(JSON.stringify(validateAttributes(snapshot.value.actual)))
    if (!(await preparePanelMatrix(false))) return
    const selection: WorkingApplicationSelection = {
      ...(preparedNewActionBaseline.value ? approvalMatrixState.value!.newActionSaveSelection() : approvalMatrixState.value!.selection('SAVE')),
      effectiveMode: effectiveMode.value, effectiveDate: effectiveDate.value,
      selectedSignoffDepartmentIds: selectedDepartments.value === null ? null : [...selectedDepartments.value],
      selectedDistributionDepartmentIds: selectedDistributionDepartments.value === null ? null : [...selectedDistributionDepartments.value],
      needTraining: needTraining.value ?? null, changeDescription: description.value
    }
    await saveWorkingApplicationAttributes(sourceId, actual, selection)
    saved = true
    const response = await getWorkingApplicationAttributes(sourceId)
    if (token !== generation || selectionToken !== selectionGeneration) return
    if (String(response.controlledFileId) !== String(sourceId) || response.projectId !== snapshot.value.projectId
      || JSON.stringify(response.actual) !== JSON.stringify(actual)
      || response.effectiveMode !== selection.effectiveMode || response.effectiveDate !== selection.effectiveDate
      || departmentSelectionKey(response.selectedSignoffDepartmentIds) !== departmentSelectionKey(selection.selectedSignoffDepartmentIds)
      || departmentSelectionKey(response.selectedDistributionDepartmentIds) !== departmentSelectionKey(selection.selectedDistributionDepartmentIds)
      || response.needTraining !== selection.needTraining || response.changeDescription !== selection.changeDescription)
      throw new Error('草稿已保存，但正式回显与本次选择不一致，请核对后继续')
    if (preparedNewActionBaseline.value) {
      if (!response.draftApprovalMatrix || response.newActionBaselineRequired !== false || response.newActionBaselinePreparationAllowed !== false)
        throw new Error('草稿已保存，但本次升版基线或资格回显未确认；请重新读取正式工作稿')
      approvalMatrixState.value!.newActionBaselineSaved(response.draftApprovalMatrix)
      newActionPreparationEligible.value = false; preparedNewActionBaseline.value = false
      newActionPreparationFacts.value = undefined
    }
    snapshot.value = structuredClone(response)
    canSubmit.value = response.canSubmit
    if (response.unavailableReason) error.value = response.unavailableReason
    message.success('本次申请草稿已保存')
  } catch (cause) {
    if (isApprovalMatrixStaleError(cause)) {
      if (token === generation && selectionToken === selectionGeneration) {
        error.value = await refreshApprovalMatrixRejection(approvalMatrixState.value, cause, () => refreshPanelMatrix(false))
      }
      return
    }
    if (token === generation && selectionToken === selectionGeneration) {
      const reason = cause instanceof Error ? cause.message : String(cause)
      error.value = saved && !reason.startsWith('草稿已保存') ? `草稿已保存，但读取核对失败：${reason}` : reason
    }
  } finally {
    if (token === generation) busy.value = false
  }
}
const restoreAttributes = async () => {
  if (selectedIntent.value === 'REPLACEMENT' || snapshotIntent.value === 'REPLACEMENT') { error.value = '独立换版申请不能保存或恢复原工作草稿'; return }
  if (!snapshot.value || busy.value || submitted.value) return
  const token = generation, selectionToken = selectionGeneration
  try {
    await ElMessageBox.confirm('确认替换本次已填写的属性？', '恢复默认属性')
    if (token !== generation || selectionToken !== selectionGeneration) return
    busy.value = true
    if (working.value || selectedIterationId.value) {
      const restored = await restoreWorkingApplicationAttributes(selectedIterationId.value || props.file.id)
      if (token === generation && selectionToken === selectionGeneration) snapshot.value = restored
    } else {
      const defaults = await getProjectDefaults(snapshot.value.projectId, 'REVISION')
      if (token === generation && selectionToken === selectionGeneration)
        snapshot.value = {
          ...snapshot.value,
          defaultSource: structuredClone(defaults),
          actual: structuredClone(defaults)
        }
    }
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close' && token === generation && selectionToken === selectionGeneration)
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (token === generation) busy.value = false
  }
}
const refreshPanelMatrix = async (requireRuntime = true) => {
  const matrix = approvalMatrixState.value
  if (!matrix) throw new Error('本次正式审批矩阵尚未读取')
  const context = generation, selection = selectionGeneration
  if (snapshot.value?.approvalSourceMode === 'APPLICATION_CUSTOM') matrix.setApplicationCustomMatrix(applicationCustomMatrix.value)
  const request = matrix.begin(selectedDepartments.value ?? undefined)
  const response = await checkControlledFileRouteReadiness(request.request)
  if (context !== generation || selection !== selectionGeneration || matrix !== approvalMatrixState.value || !matrix.accept(request, response))
    throw new Error('申请或正文上下文已变化，请重新读取正式矩阵')
  const blocker = matrixRuntimeBlockerMessage(response)
  let approvers: ReturnType<typeof resolvedUploadApprovers> | undefined
  if (response.nodes.some(node => node.stageCode === 'MATRIX_APPROVAL')) {
    approvers = resolvedUploadApprovers(response, approvalAccounts.value)
    approvalGroups.value = approvers.groups; approvalCapabilityLimitations.value = approvers.capabilityLimitations
  }
  if (!response.ready) {
    if (snapshot.value?.approvalSourceMode === 'APPLICATION_CUSTOM') canSubmit.value = false
    if (requireRuntime) throw new Error(blocker)
    return blocker
  }
  if (!approvers) approvers = resolvedUploadApprovers(response, approvalAccounts.value)
  if (snapshot.value?.approvalSourceMode === 'APPLICATION_CUSTOM') canSubmit.value = true
  approverNames.value = approvers.accounts.map(account => account.name).join('、')
  approvalRule.value = approvers.rule
  approvalGroups.value = approvers.groups; approvalCapabilityLimitations.value = approvers.capabilityLimitations
  return ''
}
const preparePanelMatrix = async (requireRuntime = true) => {
  await refreshPanelMatrix(requireRuntime)
  const matrix = approvalMatrixState.value!
  return await matrix.confirmChange(async rows => {
    try { await ElMessageBox.confirm(rows.map(row => `${row.label}：${row.value}`).join('\n'),
      '公共审批配置已变化，请确认本次使用的配置', { confirmButtonText: '确认当前配置', cancelButtonText: '取消', type: 'warning' }); return true }
    catch (cause) { if (cause === 'cancel' || cause === 'close') return false; throw cause }
  })
}

const readRevisionTargetVersion = (intent: RevisionIntent): string => {
  const target = intent === 'PARTIAL' ? revisionOptions.value?.partialTarget : revisionOptions.value?.replacementTarget
  if (!target || target.unavailableReason || typeof target.versionNo !== 'string'
    || !/^[A-Z]+\/[1-9]\d*$/.test(target.versionNo)) throw new Error('目标受控版本当前不可分配，请刷新正式变更选项')
  return target.versionNo
}
const confirmation = async (intent: string, body: string, details: string,
  revisionVersions?: { baselineVersionNo: string; targetVersionNo: string }) => {
  if (!snapshot.value || !canSubmit.value) throw new Error('当前申请条件未就绪')
  validateAttributes(snapshot.value.actual)
  const signoff = normalizeSignoffDepartments(selectedDepartments.value)!
  const distribution = normalizeDistributionDepartments(selectedDistributionDepartments.value)
  if (signoff.some((id) => !departments.value.some((row) => String(row.id) === id)))
    throw new Error('请选择有效的本次会签部门')
  if (distribution.some(id => !departments.value.some(row => String(row.id) === id)))
    throw new Error('请选择有效的本次下发部门')
  const effective = buildUploadEffectiveChoice(effectiveMode.value, effectiveDate.value, resolveUploadTodayDate())
  if (typeof needTraining.value !== 'boolean') throw new Error('请选择本次是否需要培训')
  const attributes = snapshot.value.actual
  const file = selectedApplicationFile.value || props.file
  readApplicationOrdinaryAttachmentExpectation(file)
  const choiceNames: Record<string, string> = { Y: '是', N: '否', NA: '不适用' }
  const rows = [
    `文件名称：${file.sourceOriginalFileName || file.fileName || file.title}；编号：${file.fileNumber || '未记录'}；版本：${getDccVersionDisplayText(file.versionNo)}`,
    ...(revisionVersions ? [`来源受控版本：${getDccVersionDisplayText(revisionVersions.baselineVersionNo)}；送审工作正文：${getDccVersionDisplayText(body)}；目标受控版本：${getDccVersionDisplayText(revisionVersions.targetVersionNo)}`] : []),
    `本次申请：${intent}；正文：${getDccVersionDisplayText(body)}；项目：${file.projectName || `项目ID ${snapshot.value.projectId}`}；项目文件夹ID：${file.projectFolderId ?? '未记录'}；文件类型：${[file.fileTypeLevel1, file.fileTypeLevel2, file.fileTypeLevel3, file.fileTypeLevel4, file.fileTypeLevel5].filter(Boolean).join(' / ') || `类别ID ${file.categoryId}`}`,
    `编制人账号ID：${file.requesterId ?? '未记录'}；直属主管来自用户管理中的已配置关系，本轮按正式来源冻结并由主管本人独立签名`,
    `生效方式：${effective.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE' ? '文控审核后立即生效；生效时间来自本轮文控真实电子签名时间' : effective.effectiveMode === 'DELAYED' ? '延迟生效；文控审核及必要培训完成后，由原编制人选择生效时间并电子签名确认' : `按预设日期生效（历史申请）；预设生效日期：${effective.effectiveDate}`}`,
    `会签部门：${signoff.map(id => departments.value.find(row => String(row.id) === id)!.name).join('、')}`,
    `下发部门：${distribution.length ? distribution.map(id => departments.value.find(row => String(row.id) === id)!.name).join('、') : '未选择下发部门'}；受控且已生效后向所选下发部门全部人员下发`,
    `批准人：${approverNames.value}；批准规则：${approvalRule.value || '未记录'}；培训：${needTraining.value ? '需要，本次上传人上传记录' : '不需要'}`,
    `本次已保存附件：${file.attachments!.length ? file.attachments!.map(row => `${row.fileName}（${row.attachmentPurpose === 'ATTACHMENT' ? '普通附件' : '培训记录'}）`).join('；') : '无附件'}`,
    ...approvalGroups.value.map(group => `原批准组 ${group.sourceNodeId}：${group.summary}`),
    ...approvalCapabilityLimitations.value.map(limitation => `运行能力说明：${limitation}`),
    `目标市场：${attributes.targetMarkets.join('、')}${attributes.otherMarket ? `（${attributes.otherMarket}）` : ''}；注册人：${choiceNames[attributes.licenseHolder!] || '未记录'}；生产方：${choiceNames[attributes.actualManufacturer!] || '未记录'}；文件转移：${choiceNames[attributes.documentTransfer!] || '未记录'}${attributes.transferTo ? `；转移至：${attributes.transferTo}` : ''}`,
    `已保存关联：${file.relatedFiles?.length ? file.relatedFiles.map(related => `${related.fileName} · ${related.versionNo}`).join('；') : '未记录关联'}；申请说明：${details}`,
    '生效与受控分别记录；新版受控且已生效时才启用并作废旧版。取消保留已填写内容。'
  ]
  await ElMessageBox.confirm(
    rows.join('\n'),
    '确认提交申请',
    { type: 'warning' }
  )
}
const submitRevision = async (command: RevisionCommand) => {
  if (busy.value || submitted.value) throw new Error('本次申请正在提交或已提交，请勿重复操作')
  const token = generation,
    fingerprint = JSON.stringify([selectedIterationId.value, selectedIntent.value, applicationFacts.value])
  const body = revisionOptions.value?.iterations.find(
    (row) => String(row.id) === command.selectedIterationId
  )
  if (!body || String(revisionOptions.value?.controlledBaselineId) !== command.controlledBaselineId)
    throw new Error('所选正文或受控基线已变更')
  const targetVersionNo = readRevisionTargetVersion(command.revisionChangeType)
  const baselineVersionNo = revisionOptions.value?.baselineVersionNo
  if (!baselineVersionNo || !/^[A-Z]+\/[1-9]\d*$/.test(baselineVersionNo))
    throw new Error('来源受控版本未记录，无法确认本次升版')
  if (selectedIterationId.value !== command.selectedIterationId || selectionLoading.value ||
      command.revisionChangeType !== snapshotIntent.value || JSON.stringify(command.projectAttributes) !== JSON.stringify(snapshot.value?.actual))
    throw new Error('请先读取并核对所选正文的已保存申请属性')
  const facts = applicationFacts.value
  if (!facts || command.effectiveMode !== facts.effectiveMode || command.effectiveDate !== facts.effectiveDate
    || JSON.stringify(command.selectedSignoffDepartmentIds) !== JSON.stringify(facts.selectedSignoffDepartmentIds)
    || JSON.stringify(command.selectedDistributionDepartmentIds) !== JSON.stringify(facts.selectedDistributionDepartmentIds)
    || command.needTraining !== facts.needTraining) throw new Error('提交载荷与本次生效方式、部门或培训选择不一致')
  const frozenCommand: RevisionCommand = JSON.parse(JSON.stringify(command))
  let confirmedExecutionSelection: ReturnType<UploadApprovalMatrixState['selection']>
  let confirmedAttachments: ReturnType<typeof readApplicationOrdinaryAttachmentExpectation>
  let confirmedGroupFacts: string
  busy.value = true
  try {
    if (!(await preparePanelMatrix())) throw new Error('已取消矩阵配置确认，填写内容已保留')
    confirmedExecutionSelection = approvalMatrixState.value!.selection()
    confirmedAttachments = readApplicationOrdinaryAttachmentExpectation(selectedApplicationFile.value || props.file)
    confirmedGroupFacts = JSON.stringify([approvalGroups.value, approvalCapabilityLimitations.value])
    await confirmation(
      command.revisionChangeType === 'PARTIAL' ? '局部变更' : '换版变更',
      body.versionNo,
      command.changeDescription,
      { baselineVersionNo, targetVersionNo }
    )
  } catch (cause) {
    if (token === generation) busy.value = false
    if (cause === 'cancel' || cause === 'close') throw new Error('已取消提交，填写内容已保留')
    throw cause
  }
  if (token !== generation || fingerprint !== JSON.stringify([selectedIterationId.value, selectedIntent.value, applicationFacts.value]))
    { if (token === generation) busy.value = false; throw new Error('申请内容已变更，请重新确认') }
  try {
    if (readRevisionTargetVersion(frozenCommand.revisionChangeType) !== targetVersionNo
      || revisionOptions.value?.baselineVersionNo !== baselineVersionNo)
      throw new Error('目标受控版本或正式基线已变化，请重新确认本次升版')
    buildUploadEffectiveChoice(frozenCommand.effectiveMode, frozenCommand.effectiveDate, resolveUploadTodayDate(), confirmedExecutionSelection.executionContract)
    if (JSON.stringify(confirmedExecutionSelection) !== JSON.stringify(approvalMatrixState.value!.selection()))
      throw new Error('流程准备或审批配置已变化，请重新核对并确认本次申请')
    if (confirmedGroupFacts !== JSON.stringify([approvalGroups.value, approvalCapabilityLimitations.value]))
      throw new Error('本次原批准组事实已变化，请重新核对并确认')
    const { controlledBaselineId: _baseline, selectedIterationId, ...business } = frozenCommand
    const actualAttachments = readApplicationOrdinaryAttachmentExpectation(selectedApplicationFile.value || props.file)
    if (JSON.stringify(actualAttachments) !== JSON.stringify(confirmedAttachments)) throw new Error('本次附件清单已变化，请重新核对确认')
    const payload = { ...business, ...confirmedExecutionSelection,
      expectedOrdinaryAttachmentIds: confirmedAttachments.expectedOrdinaryAttachmentIds,
      expectedOrdinaryAttachmentManifestHash: confirmedAttachments.expectedOrdinaryAttachmentManifestHash }
    const result = await submitControlledFileWorkingIteration(selectedIterationId, payload)
    if (!isWorkflowId(result)) throw new Error('申请提交未返回正式文件身份，请刷新核对提交结果')
    if (token === generation) {
      canSubmit.value = false
      submitted.value = true
      error.value = ''
      emit('submitted', String(result))
    }
  } catch (cause) {
    if (isApprovalMatrixStaleError(cause)) {
      if (token === generation) {
        error.value = await refreshApprovalMatrixRejection(approvalMatrixState.value, cause, () => refreshPanelMatrix(false))
      }
    }
    throw cause
  } finally {
    if (token === generation) busy.value = false
  }
}
const submitInitial = async () => {
  if (busy.value || submitted.value || !canSubmit.value) return
  const token = generation
  try {
    if (!applicationFacts.value) throw new Error(applicationSelectionError.value)
    const payload = buildInitialCommand(
      description.value,
      idempotencyKey.value,
      applicationFacts.value
    )
    const fingerprint = JSON.stringify([applicationFacts.value, description.value])
    busy.value = true
    if (!(await preparePanelMatrix())) return
    const confirmedExecutionSelection = approvalMatrixState.value!.selection()
    const confirmedAttachments = readApplicationOrdinaryAttachmentExpectation(props.file)
    const confirmedGroupFacts = JSON.stringify([approvalGroups.value, approvalCapabilityLimitations.value])
    await confirmation('初始上传', props.file.versionNo, payload.changeDescription)
    if (
      token !== generation ||
      fingerprint !== JSON.stringify([applicationFacts.value, description.value])
    )
      throw new Error('申请内容已变更，请重新确认')
    buildUploadEffectiveChoice(payload.effectiveMode, payload.effectiveDate, resolveUploadTodayDate(), confirmedExecutionSelection.executionContract)
    if (JSON.stringify(confirmedExecutionSelection) !== JSON.stringify(approvalMatrixState.value!.selection()))
      throw new Error('流程准备或审批配置已变化，请重新核对并确认本次申请')
    if (confirmedGroupFacts !== JSON.stringify([approvalGroups.value, approvalCapabilityLimitations.value]))
      throw new Error('本次原批准组事实已变化，请重新核对并确认')
    if (JSON.stringify(readApplicationOrdinaryAttachmentExpectation(props.file)) !== JSON.stringify(confirmedAttachments)) throw new Error('本次附件清单已变化，请重新核对确认')
    const result = await submitControlledFileWorkingIteration(props.file.id, { ...payload, ...confirmedExecutionSelection,
      expectedOrdinaryAttachmentIds: confirmedAttachments.expectedOrdinaryAttachmentIds,
      expectedOrdinaryAttachmentManifestHash: confirmedAttachments.expectedOrdinaryAttachmentManifestHash })
    if (!isWorkflowId(result)) throw new Error('申请提交未返回正式文件身份，请刷新核对提交结果')
    if (token === generation) {
      canSubmit.value = false
      submitted.value = true
      error.value = ''
      emit('submitted', String(result))
    }
  } catch (cause) {
    if (isApprovalMatrixStaleError(cause)) {
      if (token === generation) {
        error.value = await refreshApprovalMatrixRejection(approvalMatrixState.value, cause, () => refreshPanelMatrix(false))
      }
      return
    }
    if (cause !== 'cancel' && cause !== 'close' && token === generation)
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (token === generation) busy.value = false
  }
}
</script>
