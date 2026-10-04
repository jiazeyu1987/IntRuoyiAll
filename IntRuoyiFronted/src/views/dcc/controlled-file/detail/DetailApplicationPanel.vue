<template>
  <ContentWrap>
    <el-button v-if="!opened" type="primary" plain @click="open">{{
      file.status === 'WORKING' ? '准备本次申请' : '发起局部 / 换版变更'
    }}</el-button>
    <section v-else v-loading="loading" aria-label="本次文件申请">
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <template v-if="snapshot">
        <p
          >{{ file.sourceOriginalFileName || file.fileName || file.title }} ·
          {{ file.versionNo }}</p
        >
        <p v-if="selectedIterationId">本次正文：{{ revisionOptions?.iterations.find(row => String(row.id) === selectedIterationId)?.versionNo }} · 文件 #{{ selectedIterationId }}</p>
        <el-alert
          v-if="departmentChoiceMissing"
          title="草稿未保存会签部门，请重新明确选择本次部门。"
          type="warning"
          :closable="false"
        />
        <el-form label-width="120px" :disabled="busy">
          <el-form-item label="预设生效日期"
            ><el-date-picker v-model="effectiveDate" type="date" value-format="YYYY-MM-DD"
          /></el-form-item>
          <el-form-item label="会签部门">
            <el-select v-model="selectedDepartments" multiple filterable>
              <el-option
                v-for="department in departments"
                :key="String(department.id)"
                :label="department.name"
                :value="String(department.id)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="批准人">{{ approverNames }}</el-form-item>
          <el-form-item label="培训"
            ><el-radio-group v-model="needTraining"
              ><el-radio :value="false">不需要</el-radio
              ><el-radio :value="true">需要（文控上传本次线下培训文件）</el-radio></el-radio-group
            ></el-form-item
          >
        </el-form>
        <p>本次实际属性</p>
        <ProjectAttributesFields
          :model-value="snapshot.actual"
          :readonly="busy"
          @update:model-value="updateActual"
        />
        <el-button v-if="canEditSourceDraft" :disabled="busy" @click="saveAttributes">保存草稿属性</el-button>
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
          <el-button type="primary" :loading="busy" :disabled="!canSubmit" @click="submitInitial"
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
          :application-ready="Boolean(snapshot && !selectionLoading && canSubmit && selectedIterationId)"
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
            <el-button link type="primary" @click="previewId = String(bodyIterationId)"
              >预览所选正文</el-button
            >
          </template>
        </DccRevisionPanel>
    </section>
    <el-dialog v-model="previewVisible" title="本次送审正文" width="88vw" destroy-on-close>
      <ProtectedPdfViewer v-if="previewId" :controlled-file-id="previewId" title="本次送审正文" />
    </el-dialog>
  </ContentWrap>
</template>
<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { ElMessageBox } from 'element-plus'
import { generateUUID } from '@/utils'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { getSimpleUserList } from '@/api/system/user'
import { previewApprovalRoute } from '@/api/dcc/controlledFile/approvalRoutes'
import { getProjectDefaults } from '@/api/dcc/controlledFile/projectAttributes'
import {
  getWorkingApplicationAttributes,
  getControlledFileReplacementAttributes,
  restoreWorkingApplicationAttributes,
  saveWorkingApplicationAttributes,
  getControlledFileRevisionOptions,
  type ControlledFileRevisionOptions
} from '@/api/dcc/controlledFile/applicationRead'
import {
  submitControlledFileWorkingIteration,
  type ControlledFileVO
} from '@/api/dcc/controlledFile/workflow'
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
import { formatControlledFileDateTime } from './presentation'

const props = defineProps<{ file: ControlledFileVO }>()
const emit = defineEmits<{ submitted: [fileId: string] }>()
const message = useMessage()
const opened = ref(false),
  loading = ref(false),
  busy = ref(false),
  error = ref('')
const snapshot = ref<AttributeSnapshot>(),
  revisionOptions = ref<ControlledFileRevisionOptions>()
const effectiveDate = ref(''),
  needTraining = ref<boolean | undefined>(false),
  selectedDepartments = ref<string[]>([]),
  description = ref('')
const departmentChoiceMissing = ref(false)
const departments = ref<DeptVO[]>([]),
  approverNames = ref(''),
  canSubmit = ref(false)
const idempotencyKey = ref(''),
  previewId = ref('')
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
  get: () => Boolean(previewId.value),
  set: (value) => {
    if (!value) previewId.value = ''
  }
})
const applicationFacts = computed(() =>
  snapshot.value && typeof needTraining.value === 'boolean'
    ? {
        projectAttributes: snapshot.value.actual,
        selectedSignoffDepartmentIds: [...selectedDepartments.value],
        needTraining: needTraining.value,
        effectiveDate: effectiveDate.value
      }
    : undefined
)
watch(
  () => [String(props.file.id), props.file.status, props.file.processInstanceId],
  () => {
    generation++
    opened.value = false
    loading.value = false
    busy.value = false
    error.value = ''
    snapshot.value = undefined
    revisionOptions.value = undefined
    selectedIterationId.value = ''; selectedIntent.value = undefined; snapshotIntent.value = undefined; selectionGeneration++; selectionLoading.value = false
    previewId.value = ''
  },
  { flush: 'sync' }
)
const open = async () => {
  const token = ++generation,
    file = props.file
  opened.value = true
  loading.value = true
  error.value = ''
  canSubmit.value = false
  try {
    if (!file.dccProjectCodeId) throw new Error('文件未绑定正式项目，无法准备申请')
    const saved = working.value ? await getWorkingApplicationAttributes(file.id) : undefined
    if (token !== generation) return
    const action = saved?.applicationType || 'REVISION'
    if (saved && String(saved.projectId) !== String(file.dccProjectCodeId))
      throw new Error('草稿属性与当前项目不一致')
    const defaults = saved
      ? saved.defaultSource
      : await getProjectDefaults(file.dccProjectCodeId, action)
    const [deptRows, users, route] = await Promise.all([
      getSimpleDeptList(),
      getSimpleUserList(),
      previewApprovalRoute({
        categoryId: file.categoryId,
        actionType: action === 'UPLOAD' ? 'NEW' : 'REVISION'
      })
    ])
    if (token !== generation) return
    const review = route.find((row) => row.stageCode === 'MATRIX_REVIEW')
    const approval = route.find((row) => row.stageCode === 'MATRIX_APPROVAL')
    if (!review || review.candidateSourceType !== 'DEPT' || !approval?.resolvedUserIds.length)
      throw new Error('申请审批矩阵缺少正式会签部门或批准人')
    if (
      approval.resolvedUserIds.some((id) => !users.some((user) => String(user.id) === String(id)))
    )
      throw new Error('批准人目录不完整')
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
    departments.value = deptRows
    departmentChoiceMissing.value = saved?.selectedSignoffDepartmentIds === null
    selectedDepartments.value = saved
      ? saved.selectedSignoffDepartmentIds?.map(String) || []
      : review.candidateSourceIds.map(String)
    effectiveDate.value = saved?.effectiveDate || ''
    needTraining.value = saved ? (saved.needTraining ?? undefined) : false
    description.value = saved?.changeDescription || ''
    approverNames.value = approval.resolvedUserIds
      .map((id) => users.find((user) => String(user.id) === String(id))!.nickname)
      .join('、')
    idempotencyKey.value = generateUUID()
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
  selectedIntent.value = intent
  if (selectedIterationId.value) await selectIteration(selectedIterationId.value)
}
const selectIteration = async (id: string) => {
  const token = ++selectionGeneration, context = generation
  const intent = selectedIntent.value === 'REPLACEMENT' ? 'REPLACEMENT' : 'PARTIAL'
  selectedIterationId.value = id
  snapshot.value = undefined; canSubmit.value = false; error.value = ''; selectionLoading.value = true; previewId.value = ''
  try {
    const body = revisionOptions.value?.iterations.find(row => String(row.id) === id)
    if (!body || !(body.canPartial || body.canReplacement)) throw new Error('请选择合法的本次工作正文')
    if (!(intent === 'REPLACEMENT' ? body.canReplacement : body.canPartial)) throw new Error('当前正文无所选变更操作资格')
    const saved = intent === 'REPLACEMENT'
      ? await getControlledFileReplacementAttributes(id, revisionOptions.value!.controlledBaselineId)
      : await getWorkingApplicationAttributes(id)
    if (context !== generation || token !== selectionGeneration) return
    if (String(saved.controlledFileId) !== id || String(saved.projectId) !== String(props.file.dccProjectCodeId) || saved.applicationType !== 'REVISION')
      throw new Error('所选正文的申请属性身份或类型不一致')
    snapshot.value = JSON.parse(JSON.stringify(saved))
    snapshotIntent.value = intent
    selectedDepartments.value = saved.selectedSignoffDepartmentIds?.map(String) || []
    departmentChoiceMissing.value = saved.selectedSignoffDepartmentIds === null
    effectiveDate.value = saved.effectiveDate || ''; needTraining.value = saved.needTraining ?? undefined
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
const saveAttributes = async () => {
  if (selectedIntent.value === 'REPLACEMENT' || snapshotIntent.value === 'REPLACEMENT') { error.value = '独立换版申请不能保存或恢复原工作草稿'; return }
  if (!snapshot.value || busy.value) return
  const token = generation, selectionToken = selectionGeneration
  busy.value = true
  error.value = ''
  try {
    await saveWorkingApplicationAttributes(selectedIterationId.value || props.file.id, validateAttributes(snapshot.value.actual))
    if (token === generation && selectionToken === selectionGeneration) message.success('草稿属性已保存')
  } catch (cause) {
    if (token === generation && selectionToken === selectionGeneration) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (token === generation) busy.value = false
  }
}
const restoreAttributes = async () => {
  if (selectedIntent.value === 'REPLACEMENT' || snapshotIntent.value === 'REPLACEMENT') { error.value = '独立换版申请不能保存或恢复原工作草稿'; return }
  if (!snapshot.value || busy.value) return
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
const confirmation = async (intent: string, body: string, details: string) => {
  if (!snapshot.value || !canSubmit.value) throw new Error('当前申请条件未就绪')
  validateAttributes(snapshot.value.actual)
  if (
    !selectedDepartments.value.length ||
    selectedDepartments.value.some((id) => !departments.value.some((row) => String(row.id) === id))
  )
    throw new Error('请选择有效的本次会签部门')
  if (typeof needTraining.value !== 'boolean') throw new Error('请选择本次是否需要培训')
  const attributes = snapshot.value.actual
  await ElMessageBox.confirm(
    `${props.file.sourceOriginalFileName || props.file.title}；${intent}；正文 ${body}；生效日期 ${effectiveDate.value}；会签部门 ${selectedDepartments.value.map((id) => departments.value.find((row) => String(row.id) === id)!.name).join('、') || '无附加部门'}；批准人 ${approverNames.value}；培训 ${needTraining.value ? '需要' : '不需要'}；目标市场 ${attributes.targetMarkets.join('、')}${attributes.otherMarket ? `（${attributes.otherMarket}）` : ''}；注册人 ${attributes.licenseHolder}；生产方 ${attributes.actualManufacturer}；文件转移 ${attributes.documentTransfer}${attributes.transferTo ? `，转移至 ${attributes.transferTo}` : ''}；说明 ${details}。受控日期由系统记录，新版生效时才作废旧版。`,
    '确认提交申请',
    { type: 'warning' }
  )
}
const submitRevision = async (command: RevisionCommand) => {
  const token = generation,
    fingerprint = JSON.stringify([selectedIterationId.value, selectedIntent.value, applicationFacts.value])
  const body = revisionOptions.value?.iterations.find(
    (row) => String(row.id) === command.selectedIterationId
  )
  if (!body || String(revisionOptions.value?.controlledBaselineId) !== command.controlledBaselineId)
    throw new Error('所选正文或受控基线已变更')
  if (selectedIterationId.value !== command.selectedIterationId || selectionLoading.value ||
      command.revisionChangeType !== snapshotIntent.value || JSON.stringify(command.projectAttributes) !== JSON.stringify(snapshot.value?.actual))
    throw new Error('请先读取并核对所选正文的已保存申请属性')
  try {
    await confirmation(
      command.revisionChangeType === 'PARTIAL' ? '局部变更' : '换版变更',
      body.versionNo,
      command.changeDescription
    )
  } catch (cause) {
    if (cause === 'cancel' || cause === 'close') throw new Error('已取消提交，填写内容已保留')
    throw cause
  }
  if (token !== generation || fingerprint !== JSON.stringify([selectedIterationId.value, selectedIntent.value, applicationFacts.value]))
    throw new Error('申请内容已变更，请重新确认')
  busy.value = true
  try {
    const { controlledBaselineId: _baseline, selectedIterationId, ...payload } = command
    const result = await submitControlledFileWorkingIteration(selectedIterationId, payload)
    if (!isWorkflowId(result)) throw new Error('申请提交未返回正式文件身份，请刷新核对提交结果')
    if (token === generation) {
      canSubmit.value = false
      error.value = ''
      emit('submitted', String(result))
    }
  } finally {
    if (token === generation) busy.value = false
  }
}
const submitInitial = async () => {
  if (busy.value) return
  const token = generation
  try {
    if (!applicationFacts.value) throw new Error('请完成本次属性和培训选择')
    const payload = buildInitialCommand(
      description.value,
      idempotencyKey.value,
      applicationFacts.value
    )
    const fingerprint = JSON.stringify([applicationFacts.value, description.value])
    await confirmation('初始上传', props.file.versionNo, payload.changeDescription)
    if (
      token !== generation ||
      fingerprint !== JSON.stringify([applicationFacts.value, description.value])
    )
      throw new Error('申请内容已变更，请重新确认')
    busy.value = true
    const result = await submitControlledFileWorkingIteration(props.file.id, payload)
    if (!isWorkflowId(result)) throw new Error('申请提交未返回正式文件身份，请刷新核对提交结果')
    if (token === generation) {
      canSubmit.value = false
      error.value = ''
      emit('submitted', String(result))
    }
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close' && token === generation)
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (token === generation) busy.value = false
  }
}
</script>
