<template>
  <section class="frontline-operator-panel">
    <div
      v-if="isPqcMode"
      class="frontline-operator-screen is-pqc"
      data-frontline-pqc-operator
    >
      <header class="frontline-operator-top is-pqc">
        <button class="frontline-top-card" type="button">
          <span>生产订单</span>
          <strong class="frontline-top-card__order">{{ productionOrderLabel }}</strong>
        </button>
        <button class="frontline-top-card" type="button" @click="openTaskDialog">
          <span>任务</span>
          <strong>{{ selectedTaskLabel }}</strong>
        </button>
        <button class="frontline-top-card" type="button" @click="openPicker('process')">
          <span>工序</span>
          <strong>{{ selectedProcessLabel }}</strong>
        </button>
        <button class="frontline-top-card" type="button" @click="openPicker('employee')">
          <span>员工</span>
          <strong>{{ selectedEmployeeLabel }}</strong>
        </button>
        <button class="frontline-home-button" type="button" @click="handleHome">主页</button>
      </header>

      <main class="frontline-operator-main">
        <section class="frontline-work-panel" data-frontline-pqc-inspection-content>
          <h3>检验内容</h3>
          <div class="frontline-inspection-list">
            <label class="frontline-inspection-row">
              <span>长度</span>
              <el-input-number
                v-model="pqcDraft.lengthCm"
                :min="0"
                :step="1"
                controls-position="right"
              />
              <em>厘米</em>
            </label>
            <label class="frontline-inspection-row">
              <span>外观</span>
              <el-radio-group v-model="pqcDraft.appearanceQualified">
                <el-radio-button :label="true">合格</el-radio-button>
                <el-radio-button :label="false">不合格</el-radio-button>
              </el-radio-group>
            </label>
            <label class="frontline-inspection-row">
              <span>密封</span>
              <el-radio-group v-model="pqcDraft.sealQualified">
                <el-radio-button :label="true">合格</el-radio-button>
                <el-radio-button :label="false">不合格</el-radio-button>
              </el-radio-group>
            </label>
            <label class="frontline-inspection-row">
              <span>压力</span>
              <el-input-number
                v-model="pqcDraft.pressureMpa"
                :min="0"
                :precision="1"
                :step="0.1"
                controls-position="right"
              />
              <em>MPa</em>
            </label>
          </div>
        </section>

        <section class="frontline-work-panel">
          <h3>填检验</h3>
          <div class="frontline-choice-row">
            <button
              v-for="inspectionType in inspectionTypeOptions"
              :key="inspectionType.value"
              type="button"
              :class="{ active: pqcDraft.inspectionType === inspectionType.value }"
              @click="pqcDraft.inspectionType = inspectionType.value"
            >
              {{ inspectionType.label }}
            </button>
          </div>
          <div v-if="pqcDraft.inspectionType === 'PATROL'" class="frontline-choice-row">
            <button
              v-for="round in patrolRounds"
              :key="round"
              type="button"
              :class="{ active: pqcDraft.patrolRound === round }"
              @click="pqcDraft.patrolRound = round"
            >
              第 {{ round }} 次
            </button>
          </div>
          <div class="frontline-number-grid">
            <label>
              <span>检验数量</span>
              <el-input-number v-model="pqcDraft.inspectionQuantity" :min="0" :step="1" />
            </label>
            <label>
              <span>损耗数量</span>
              <el-input-number v-model="pqcDraft.scrapQuantity" :min="0" :step="1" />
            </label>
          </div>
        </section>
      </main>

      <footer class="frontline-submit-bar">
        <span>{{ statusText }}</span>
        <el-button
          type="primary"
          size="large"
          :loading="payloadLoading"
          :disabled="isSubmitBlocked"
          @click="handleValidate"
        >
          提交
        </el-button>
      </footer>
    </div>

    <div
      v-else
      class="frontline-operator-screen"
      data-frontline-production-operator
    >
      <header class="frontline-operator-top">
        <button class="frontline-top-card" type="button" @click="openTaskDialog">
          <span>任务</span>
          <strong>{{ selectedTaskLabel }}</strong>
        </button>
        <button class="frontline-top-card" type="button" @click="openPicker('process')">
          <span>工序</span>
          <strong>{{ selectedProcessLabel }}</strong>
        </button>
        <button class="frontline-top-card" type="button" @click="openPicker('employee')">
          <span>员工</span>
          <strong>{{ selectedEmployeeLabel }}</strong>
        </button>
        <button class="frontline-home-button" type="button" @click="handleHome">主页</button>
      </header>

      <main class="frontline-operator-main">
        <section class="frontline-work-panel">
          <h3>填数量</h3>
          <label class="frontline-production-field">
            <span>上工序输入数量</span>
            <el-input-number
              v-model="productionDraft.previousProcessInputQuantity"
              :min="0"
              :step="1"
              controls-position="right"
            />
            <em>个</em>
          </label>
          <label class="frontline-production-field">
            <span>输出数量</span>
            <el-input-number
              v-model="productionDraft.outputQuantity"
              :min="0"
              :step="1"
              controls-position="right"
            />
            <em>个</em>
          </label>
          <label class="frontline-production-field">
            <span>损耗数量</span>
            <el-input-number
              v-model="productionDraft.scrapQuantity"
              :min="0"
              :step="1"
              controls-position="right"
            />
            <em>个</em>
          </label>
        </section>

        <section class="frontline-work-panel">
          <h3>设备参数</h3>
          <div v-if="visibleDeviceCards.length" class="frontline-device-grid">
            <label
              v-for="device in visibleDeviceCards"
              :key="device.key"
              class="frontline-device-card"
            >
              <span>{{ device.label }}</span>
              <el-input
                v-model="deviceParameterDraft[device.key]"
                placeholder="填参数"
                clearable
              />
            </label>
          </div>
          <div v-else class="frontline-no-device">本工序无设备，直接填数量</div>
        </section>
      </main>

      <footer class="frontline-submit-bar">
        <span>{{ statusText }}</span>
        <el-button
          type="primary"
          size="large"
          :loading="payloadLoading"
          :disabled="isSubmitBlocked"
          @click="handleValidate"
        >
          提交
        </el-button>
      </footer>
    </div>

    <el-dialog
      v-model="signatureDialogVisible"
      title="电子签名确认"
      width="460px"
      :close-on-click-modal="false"
      data-frontline-submit-signature-dialog
    >
      <el-alert
        title="提交后将同时写入报工、记录本和工序池，请使用实际员工电子签名密码。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form class="frontline-signature-form" label-width="110px">
        <el-form-item label="签名员工">
          <span>{{ selectedEmployeeLabel }}</span>
        </el-form-item>
        <el-form-item label="签名密码" required>
          <el-input
            v-model="signatureForm.password"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请输入电子签名密码"
            @keyup.enter="submitWithSignature"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="signatureForm.comment"
            maxlength="200"
            placeholder="可选，默认一线报工提交"
          />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="signatureError"
        :title="signatureError"
        type="error"
        :closable="false"
        show-icon
      />
      <template #footer>
        <el-button @click="signatureDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="payloadLoading" @click="submitWithSignature">
          确认提交
        </el-button>
      </template>
    </el-dialog>

    <div v-if="activePicker" class="frontline-picker" @click.self="closePicker">
      <section class="frontline-picker__card">
        <h3>{{ activePicker === 'process' ? '选择工序' : '选择员工' }}</h3>
        <div class="frontline-picker__options">
          <button
            v-for="option in pickerOptions"
            :key="option.key"
            type="button"
            :class="{ active: option.active }"
            @click="option.onClick"
          >
            {{ option.label }}
          </button>
        </div>
        <button class="frontline-picker__close" type="button" @click="closePicker">关闭</button>
      </section>
    </div>

    <ProTaskSelectDialog
      ref="taskDialogRef"
      :multiple="false"
      :statuses="frontlineTaskStatuses"
      @selected="handleTaskSelected"
    />
  </section>
</template>

<script setup lang="ts">
import {
  FRONTLINE_FIELD_CODES,
  FRONTLINE_PQC_RESULTS,
  FRONTLINE_TEMPLATE_CODES,
  FrontlineTemplateApi,
  type FrontlineTemplateCode,
  type FrontlineTemplateDefinitionVO,
  type FrontlineTemplatePayloadVO
} from '@/api/mes/pro/feedbackFrontlineTemplate'
import type {
  FrontlineDeviceRouteProcessVO,
  FrontlineEmployeeCandidateVO,
  FrontlineSubmitContextRespVO,
  ProFrontlineFeedbackSubmitReqVO
} from '@/api/mes/pro/feedback'
import type { ProTaskVO } from '@/api/mes/pro/task'
import { sameRouteQueryId } from '@/utils/routeQueryId'
import { MesProTaskStatusEnum } from '@/views/mes/utils/constants'
import { ProFeedbackApi } from '@/api/mes/pro/feedback'
import ProTaskSelectDialog from '@/views/mes/pro/task/components/ProTaskSelectDialog.vue'
import {
  buildFrontlineTemplatePayload,
  createFrontlineDefaultValues,
  resetFrontlineTemplateDraftForContext,
  resolveFrontlineContextKey,
  type FrontlineTemplateContext,
  type FrontlineTemplateDraft
} from './frontlineTemplate'
import {
  createFrontlineDeviceEmployeeState,
  loadFrontlineDeviceProcesses,
  selectFrontlineProcess,
  switchFrontlineActualEmployee
} from './frontlineDeviceEmployeeContext'

type PickerType = 'process' | 'employee'
type InspectionType = 'FIRST' | 'PATROL' | 'FINAL'

const props = withDefaults(defineProps<{ mode?: 'production' | 'pqc' }>(), {
  mode: 'production'
})

const message = useMessage()
const router = useRouter()
const route = useRoute()

const catalog = ref<FrontlineTemplateDefinitionVO[]>([])
const payloadLoading = ref(false)
const payloadPreview = ref<FrontlineTemplatePayloadVO>()
const submitResult = ref()
const activePicker = ref<PickerType>()
const contextLoading = ref(false)
const deviceState = reactive(createFrontlineDeviceEmployeeState())
const employeeTemplateCode = ref<FrontlineTemplateCode>()
const deviceAccountUserId = ref<number>()
const selectedTask = ref<ProTaskVO>()
const routeTaskId = ref<number>()
const submitContext = ref<FrontlineSubmitContextRespVO>()
const submitContextError = ref('')
const taskDialogRef = ref()
const signatureDialogVisible = ref(false)
const signatureError = ref('')
const frontlineTaskStatuses = [MesProTaskStatusEnum.PREPARE, MesProTaskStatusEnum.IN_PROGRESS]

const expectedTemplateCode = computed<FrontlineTemplateCode>(() =>
  props.mode === 'pqc'
    ? FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED
    : FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED
)

const context = reactive<FrontlineTemplateContext>({
  templateCode: expectedTemplateCode.value
})

const draft = reactive<FrontlineTemplateDraft>({
  fieldValues: createFrontlineDefaultValues(context.templateCode)
})

const productionDraft = reactive({
  previousProcessInputQuantity: undefined as number | undefined,
  outputQuantity: undefined as number | undefined,
  scrapQuantity: undefined as number | undefined
})

const deviceParameterDraft = reactive<Record<string, string>>({})

const pqcDraft = reactive({
  lengthCm: undefined as number | undefined,
  appearanceQualified: true,
  sealQualified: true,
  pressureMpa: undefined as number | undefined,
  inspectionType: 'FIRST' as InspectionType,
  patrolRound: 1,
  inspectionQuantity: undefined as number | undefined,
  scrapQuantity: undefined as number | undefined
})

const signatureForm = reactive({
  password: '',
  comment: ''
})

const inspectionTypeOptions: Array<{ value: InspectionType; label: string }> = [
  { value: 'FIRST', label: '首检' },
  { value: 'PATROL', label: '巡检' },
  { value: 'FINAL', label: '末检' }
]

const patrolRounds = [1, 2, 3]

const productionOrderLabel = computed(() =>
  submitContext.value?.workOrderCode ||
  selectedTask.value?.workOrderCode ||
  firstRouteQueryText(['productionOrderCode', 'workOrderCode', 'orderCode']) ||
  '未选择订单'
)

const isPqcMode = computed(() => props.mode === 'pqc')

const selectedProcessLabel = computed(() => formatProcessLabel(deviceState.selectedProcess))

const selectedEmployeeLabel = computed(() => formatEmployeeLabel(deviceState.selectedEmployee))

const selectedTaskId = computed(() => selectedTask.value?.id ?? routeTaskId.value)

const selectedTaskLabel = computed(() => {
  if (selectedTask.value?.code) {
    return selectedTask.value.code
  }
  const queryTaskCode = firstRouteQueryText(['taskCode', 'productionTaskCode'])
  if (queryTaskCode) {
    return queryTaskCode
  }
  return selectedTaskId.value ? `任务 ${selectedTaskId.value}` : '请选择'
})

const templateModeMismatch = computed(() =>
  Boolean(employeeTemplateCode.value && employeeTemplateCode.value !== expectedTemplateCode.value)
)

const templateBindingMissing = computed(() =>
  Boolean(deviceState.selectedEmployee && !employeeTemplateCode.value)
)

const formalSubmitContextMissingFields = computed(() => {
  const missingFields: string[] = []
  if (!submitContext.value?.workOrderId) {
    missingFields.push('生产工单')
  }
  if (!submitContext.value?.taskId) {
    missingFields.push('生产任务')
  }
  if (!submitContext.value?.itemId) {
    missingFields.push('产品物料')
  }
  if (!submitContext.value?.approveUserId) {
    missingFields.push('当前审批人')
  }
  if (!submitContext.value?.recordbookId) {
    missingFields.push('记录本')
  }
  if (!submitContext.value?.feedbackType) {
    missingFields.push('报工类型')
  }
  return missingFields
})

const isSubmitBlocked = computed(() =>
  payloadLoading.value ||
  contextLoading.value ||
  templateModeMismatch.value ||
  templateBindingMissing.value ||
  formalSubmitContextMissingFields.value.length > 0 ||
  !deviceState.selectedProcess ||
  !deviceState.selectedEmployee
)

const statusText = computed(() => {
  if (deviceState.lastError) {
    return deviceState.lastError
  }
  if (!deviceState.selectedProcess) {
    return '请选择工序'
  }
  if (!deviceState.selectedEmployee) {
    return '请选择员工'
  }
  if (templateBindingMissing.value) {
    return '当前员工缺少一线填写模板'
  }
  if (templateModeMismatch.value) {
    return `当前员工绑定的是${formatTemplateName(employeeTemplateCode.value)}，请切换${formatTemplateName(expectedTemplateCode.value)}员工`
  }
  if (contextLoading.value) {
    return '正在解析任务上下文'
  }
  if (submitContextError.value) {
    return submitContextError.value
  }
  if (formalSubmitContextMissingFields.value.length > 0) {
    return `缺少${formalSubmitContextMissingFields.value.join('、')}，请从正式报工入口进入`
  }
  return '准备提交'
})

const selectedDeviceCards = computed(() => {
  const selected = deviceState.selectedProcess
  if (!selected) {
    return []
  }
  const sameProcessDevices = deviceState.processOptions.filter((process) =>
    process.routeId === selected.routeId &&
    process.routeProcessId === selected.routeProcessId &&
    process.processId === selected.processId &&
    Number(process.deviceId || 0) > 0
  )
  const seen = new Set<number>()
  const cards: Array<{ key: string; label: string }> = []
  for (const process of sameProcessDevices) {
    if (seen.has(process.deviceId)) {
      continue
    }
    seen.add(process.deviceId)
    cards.push({
      key: String(process.deviceId),
      label: process.deviceName || process.deviceCode || `设备 ${cards.length + 1}`
    })
  }
  return cards
})

const visibleDeviceCards = computed(() => selectedDeviceCards.value.slice(0, 3))

const pickerOptions = computed(() => {
  if (activePicker.value === 'process') {
    return deviceState.processOptions.map((process) => ({
      key: `${process.routeId}-${process.routeProcessId}-${process.processId}-${process.deviceId}`,
      label: formatProcessLabel(process),
      active: isSameProcess(process, deviceState.selectedProcess),
      onClick: () => handleSelectProcess(process)
    }))
  }
  if (activePicker.value === 'employee') {
    return deviceState.employeeOptions.map((employee) => ({
      key: String(employee.userId),
      label: formatEmployeeLabel(employee),
      active: employee.userId === deviceState.selectedEmployee?.userId,
      onClick: () => handleSelectEmployee(employee)
    }))
  }
  return []
})

const frontlineContextKey = computed(() => resolveFrontlineContextKey(context))

watch(
  expectedTemplateCode,
  (templateCode) => {
    context.templateCode = templateCode
    Object.assign(draft.fieldValues, createFrontlineDefaultValues(templateCode))
    payloadPreview.value = undefined
  },
  { flush: 'sync' }
)

watch(
  frontlineContextKey,
  (nextKey, previousKey) => {
    const changed = resetFrontlineTemplateDraftForContext(previousKey, nextKey, draft)
    if (changed) {
      Object.assign(draft.fieldValues, createFrontlineDefaultValues(context.templateCode))
      payloadPreview.value = undefined
    }
  },
  { flush: 'sync' }
)

watch(
  [productionDraft, selectedDeviceCards, deviceParameterDraft],
  () => {
    if (!isPqcMode.value) {
      Object.assign(draft.fieldValues, buildProductionFieldValues())
    }
  },
  { deep: true }
)

const openPicker = (picker: PickerType) => {
  activePicker.value = picker
}

const closePicker = () => {
  activePicker.value = undefined
}

const handleHome = () => {
  router.push('/')
}

const openTaskDialog = () => {
  const selectedIds = selectedTaskId.value ? [selectedTaskId.value] : []
  taskDialogRef.value?.open(
    selectedIds,
    submitContext.value?.workOrderId ?? selectedTask.value?.workOrderId ?? context.workOrderId,
    deviceState.selectedProcess?.workstationId
  )
}

const handleTaskSelected = async (rows: ProTaskVO[]) => {
  const task = rows[0]
  if (!task) {
    return
  }
  selectedTask.value = task
  routeTaskId.value = task.id
  context.workOrderId = task.workOrderId
  await resolveSubmitContext()
}

const handleSelectProcess = async (process: FrontlineDeviceRouteProcessVO) => {
  await selectFrontlineProcess(deviceState, process)
  applyProcessToContext(process)
  employeeTemplateCode.value = undefined
  const firstEmployee = deviceState.employeeOptions[0]
  if (firstEmployee) {
    await handleSelectEmployee(firstEmployee)
  }
  await resolveSubmitContext()
  closePicker()
}

const handleSelectEmployee = async (employee: FrontlineEmployeeCandidateVO) => {
  const result = await switchFrontlineActualEmployee(deviceState, employee.userId)
  deviceAccountUserId.value = result.loginUserId
  context.actualEmployeeId = result.actualEmployeeId
  const templateCode = resolveTemplateCode(result.template?.templateNo, result.template?.templateType)
  employeeTemplateCode.value = templateCode
  closePicker()
}

const handleValidate = async () => {
  if (templateBindingMissing.value) {
    const error = new Error('当前员工缺少一线填写模板，无法提交。')
    message.error(error.message)
    throw error
  }
  if (templateModeMismatch.value) {
    const error = new Error(statusText.value)
    message.error(error.message)
    throw error
  }
  Object.assign(draft.fieldValues, isPqcMode.value ? buildPqcFieldValues() : buildProductionFieldValues())
  payloadLoading.value = true
  try {
    assertFormalPayloadContext()
    payloadPreview.value = await FrontlineTemplateApi.validatePayload(
      buildFrontlineTemplatePayload(context, draft.fieldValues)
    )
    signatureForm.password = ''
    signatureForm.comment = ''
    signatureError.value = ''
    signatureDialogVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error))
    throw error
  } finally {
    payloadLoading.value = false
  }
}

const submitWithSignature = async () => {
  const password = signatureForm.password.trim()
  if (!password) {
    signatureError.value = '电子签名密码不能为空。'
    return
  }
  payloadLoading.value = true
  signatureError.value = ''
  try {
    const req = buildFrontlineSubmitRequest(password)
    submitResult.value = await ProFeedbackApi.frontlineSubmit(req)
    signatureDialogVisible.value = false
    message.success('已提交')
  } catch (error) {
    const errorMessage = resolveErrorMessage(error)
    signatureError.value = errorMessage
    message.error(errorMessage)
    throw error
  } finally {
    payloadLoading.value = false
  }
}

const assertFormalPayloadContext = () => {
  const missingFields = [...formalSubmitContextMissingFields.value]
  if (!selectedTaskId.value) {
    missingFields.push('任务')
  }
  if (!context.routeId) {
    missingFields.push('路线')
  }
  if (!context.processId || !context.routeProcessId) {
    missingFields.push('工序')
  }
  if (!context.actualEmployeeId) {
    missingFields.push('员工')
  }
  if (missingFields.length) {
    throw new Error(`缺少${missingFields.join('、')}，无法提交。`)
  }
}

const buildProductionFieldValues = () => ({
  [FRONTLINE_FIELD_CODES.PREVIOUS_PROCESS_INPUT_QUANTITY]:
    productionDraft.previousProcessInputQuantity,
  [FRONTLINE_FIELD_CODES.DEVICE]: visibleDeviceCards.value.length
    ? visibleDeviceCards.value.map((device) => device.label).join('、')
    : '无设备',
  [FRONTLINE_FIELD_CODES.DEVICE_PARAMETERS]: Object.fromEntries(
    visibleDeviceCards.value.map((device) => [device.label, deviceParameterDraft[device.key] || ''])
  ),
  [FRONTLINE_FIELD_CODES.OUTPUT_QUANTITY]: productionDraft.outputQuantity,
  [FRONTLINE_FIELD_CODES.SCRAP_QUANTITY]: productionDraft.scrapQuantity
})

const buildPqcFieldValues = () => ({
  [FRONTLINE_FIELD_CODES.PQC_RESULT]:
    pqcDraft.appearanceQualified && pqcDraft.sealQualified
      ? FRONTLINE_PQC_RESULTS.DETECTION_SUCCESS
      : FRONTLINE_PQC_RESULTS.DETECTION_FAILED
})

const buildFrontlineSubmitRequest = (signaturePassword: string): ProFrontlineFeedbackSubmitReqVO => {
  Object.assign(draft.fieldValues, isPqcMode.value ? buildPqcFieldValues() : buildProductionFieldValues())
  const fieldValues = buildFrontlineTemplatePayload(context, draft.fieldValues).fieldValues
  const process = requireSelectedProcess()
  const formalContext = requireSubmitContext()
  const workOrderId = requireNumber(formalContext.workOrderId, '生产工单')
  const taskId = requireNumber(formalContext.taskId, '生产任务')
  const itemId = requireNumber(formalContext.itemId, '产品物料')
  const approveUserId = requireNumber(formalContext.approveUserId, '当前审批人')
  const recordbookId = requireNumber(formalContext.recordbookId, '记录本')
  const feedbackType = requireNumber(formalContext.feedbackType, '报工类型')
  const currentDeviceAccountUserId = requireNumber(
    deviceAccountUserId.value ?? firstRouteQueryNumber(['deviceAccountUserId', 'loginUserId']),
    '设备账号'
  )
  const outputQuantity = isPqcMode.value ? pqcDraft.inspectionQuantity : productionDraft.outputQuantity
  const lossQuantity = isPqcMode.value ? pqcDraft.scrapQuantity : productionDraft.scrapQuantity
  const previousProcessInputQuantity = isPqcMode.value
    ? pqcDraft.inspectionQuantity
    : productionDraft.previousProcessInputQuantity
  const equipmentParameters = isPqcMode.value ? buildPqcEquipmentParameters() : buildEquipmentParameters()
  const submitKey = [
    'frontline',
    workOrderId,
    taskId,
    process.routeProcessId,
    context.actualEmployeeId,
    Date.now()
  ].join('-')
  const rawPayload = buildRawPayload(fieldValues, equipmentParameters)

  return {
    feedbackPayload: {
      code: firstRouteQueryText(['feedbackCode']) || submitKey,
      type: feedbackType,
      workstationId: requireNumber(process.workstationId, '工作站'),
      routeId: requireNumber(process.routeId, '路线'),
      processId: requireNumber(process.processId, '工序'),
      workOrderId,
      taskId,
      scheduleOrderId: firstRouteQueryNumber(['scheduleOrderId']),
      scheduleOrderProcessId: firstRouteQueryNumber(['scheduleOrderProcessId']),
      itemId,
      expireDate: formalContext.expireDate ?? firstRouteQueryText(['expireDate']),
      scheduledQuantity: formalContext.scheduledQuantity ?? firstRouteQueryNumber(['scheduledQuantity']),
      outputQuantity: requireNumber(outputQuantity, isPqcMode.value ? '检验数量' : '输出数量'),
      lossQuantity: requireNonNegativeNumber(lossQuantity, '损耗数量'),
      laborScrapQuantity: undefined,
      materialScrapQuantity: undefined,
      otherScrapQuantity: undefined,
      approveUserId,
      remark: isPqcMode.value ? 'frontline PQC submit' : 'frontline production submit'
    },
    recordbookPayload: {
      recordbookId,
      entryTitle: isPqcMode.value ? 'PQC simplified original' : 'Production simplified original',
      entryContent: rawPayload,
      previousProcessInputQuantity: requireNumber(previousProcessInputQuantity, '上工序输入数量'),
      equipmentParameters,
      tagCodes: [isPqcMode.value ? 'FRONTLINE_PQC' : 'FRONTLINE_PRODUCTION'],
      idempotencyKey: submitKey,
      remark: isPqcMode.value ? 'PQC simplified original' : 'production simplified original'
    },
    processPoolContext: {
      workOrderId,
      taskId,
      routeId: requireNumber(formalContext.routeId, '路线'),
      routeProcessId: requireNumber(formalContext.routeProcessId, '路线工序'),
      processId: requireNumber(formalContext.processId, '工序'),
      workstationId: requireNumber(formalContext.workstationId, '工作站'),
      deviceId: requireNumber(formalContext.deviceId, '设备'),
      deviceAccountUserId: currentDeviceAccountUserId,
      templateType: String(context.templateCode)
    },
    actualEmployeeId: requireNumber(context.actualEmployeeId, '实际员工'),
    signaturePassword,
    signatureComment: signatureForm.comment.trim() || '一线报工提交',
    rawPayload
  }
}

const buildEquipmentParameters = (): Record<string, unknown> =>
  Object.fromEntries(
    visibleDeviceCards.value.map((device) => [device.label, deviceParameterDraft[device.key] || ''])
  )

const buildPqcEquipmentParameters = (): Record<string, unknown> => ({
  lengthCm: pqcDraft.lengthCm,
  pressureMpa: pqcDraft.pressureMpa,
  appearanceQualified: pqcDraft.appearanceQualified,
  sealQualified: pqcDraft.sealQualified,
  inspectionType: pqcDraft.inspectionType,
  patrolRound: pqcDraft.inspectionType === 'PATROL' ? pqcDraft.patrolRound : undefined
})

const buildRawPayload = (
  fieldValues: Record<string, unknown>,
  equipmentParameters: Record<string, unknown>
): Record<string, unknown> => ({
  mode: props.mode,
  templateType: context.templateCode,
  ...fieldValues,
  fieldValues,
  previousProcessInputQuantity: productionDraft.previousProcessInputQuantity,
  outputQuantity: productionDraft.outputQuantity,
  scrapQuantity: productionDraft.scrapQuantity,
  pqc: { ...pqcDraft },
  equipmentParameters,
  process: deviceState.selectedProcess,
  employee: deviceState.selectedEmployee
})

const applyProcessToContext = (process: FrontlineDeviceRouteProcessVO) => {
  context.routeId = process.routeId
  context.routeProcessId = process.routeProcessId
  context.processId = process.processId
  submitContext.value = undefined
  submitContextError.value = ''
}

const requireSelectedProcess = () => {
  if (!deviceState.selectedProcess) {
    throw new Error('请选择工序。')
  }
  return deviceState.selectedProcess
}

const requireSubmitContext = () => {
  if (!submitContext.value) {
    throw new Error('缺少正式报工上下文，无法提交。')
  }
  return submitContext.value
}

const requireNumber = (value: number | undefined, label: string) => {
  if (!Number.isFinite(value) || Number(value) <= 0) {
    throw new Error(`缺少${label}，无法提交。`)
  }
  return Number(value)
}

const requireNonNegativeNumber = (value: number | undefined, label: string) => {
  if (!Number.isFinite(value) || Number(value) < 0) {
    throw new Error(`缺少或无效${label}，无法提交。`)
  }
  return Number(value)
}

const hydrateContextFromRoute = () => {
  context.workOrderId = firstRouteQueryNumber(['workOrderId', 'productionOrderId', 'orderId'])
  context.routeId = firstRouteQueryNumber(['routeId']) ?? context.routeId
  context.routeProcessId = firstRouteQueryNumber(['routeProcessId']) ?? context.routeProcessId
  context.processId = firstRouteQueryNumber(['processId']) ?? context.processId
  context.actualEmployeeId = firstRouteQueryNumber(['actualEmployeeId']) ?? context.actualEmployeeId
  routeTaskId.value = firstRouteQueryNumber(['taskId']) ?? routeTaskId.value
  const queryTemplateCode = resolveTemplateCode(firstRouteQueryText(['templateCode', 'templateNo']))
  employeeTemplateCode.value = queryTemplateCode
  context.templateCode = expectedTemplateCode.value
}

const resolveSubmitContext = async () => {
  const process = deviceState.selectedProcess
  const taskId = selectedTaskId.value
  if (!process || !taskId) {
    submitContext.value = undefined
    submitContextError.value = ''
    return
  }
  contextLoading.value = true
  submitContextError.value = ''
  try {
    const resolved = await ProFeedbackApi.resolveFrontlineSubmitContext({
      taskId,
      routeId: requireNumber(process.routeId, '路线'),
      routeProcessId: requireNumber(process.routeProcessId, '路线工序'),
      processId: requireNumber(process.processId, '工序')
    })
    submitContext.value = resolved
    context.workOrderId = resolved.workOrderId
    context.routeId = resolved.routeId
    context.routeProcessId = resolved.routeProcessId
    context.processId = resolved.processId
  } catch (error) {
    submitContext.value = undefined
    submitContextError.value = resolveErrorMessage(error)
  } finally {
    contextLoading.value = false
  }
}

const firstRouteQueryText = (keys: string[]) => {
  for (const key of keys) {
    const value = route.query[key]
    const text = Array.isArray(value) ? value[0] : value
    if (text) {
      return String(text)
    }
  }
  return undefined
}

const firstRouteQueryNumber = (keys: string[]) => {
  const text = firstRouteQueryText(keys)
  if (!text) {
    return undefined
  }
  const value = Number(text)
  return Number.isFinite(value) && value > 0 ? value : undefined
}

const resolveTemplateCode = (
  templateNo?: string,
  templateType?: string
): FrontlineTemplateCode | undefined => {
  if (templateNo === FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED) {
    return FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED
  }
  if (templateNo === FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED) {
    return FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED
  }
  if (templateType === 'PRODUCTION') {
    return FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED
  }
  if (templateType === 'PQC') {
    return FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED
  }
  return undefined
}

const isSameProcess = (
  left?: FrontlineDeviceRouteProcessVO,
  right?: FrontlineDeviceRouteProcessVO
) =>
  Boolean(left && right) &&
  sameRouteQueryId(left.routeId, right.routeId) &&
  sameRouteQueryId(left.routeProcessId, right.routeProcessId) &&
  sameRouteQueryId(left.processId, right.processId)

const formatProcessLabel = (process?: FrontlineDeviceRouteProcessVO) => {
  if (!process) {
    return '未选择'
  }
  const sortText = process.sort ? `${process.sort}. ` : ''
  return `${sortText}${process.processName || process.processCode || process.processId}`
}

const formatEmployeeLabel = (employee?: FrontlineEmployeeCandidateVO) => {
  if (!employee) {
    return '未选择'
  }
  return employee.nickname || employee.username || String(employee.userId)
}

const formatTemplateName = (templateCode?: FrontlineTemplateCode) => {
  if (templateCode === FRONTLINE_TEMPLATE_CODES.PQC_SIMPLIFIED) {
    return 'PQC填写'
  }
  if (templateCode === FRONTLINE_TEMPLATE_CODES.PRODUCTION_SIMPLIFIED) {
    return '生产填写'
  }
  return '未知模板'
}

const resolveErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return '提交失败'
}

onMounted(async () => {
  hydrateContextFromRoute()
  catalog.value = await FrontlineTemplateApi.getCatalog()
  await loadFrontlineDeviceProcesses(deviceState)
  const firstProcess = findInitialProcess() ?? deviceState.processOptions[0]
  if (firstProcess) {
    await handleSelectProcess(firstProcess)
  }
  Object.assign(draft.fieldValues, buildProductionFieldValues())
})

const findInitialProcess = () =>
  deviceState.processOptions.find((process) =>
    (!context.routeId || sameRouteQueryId(process.routeId, context.routeId)) &&
    (!context.routeProcessId ||
      sameRouteQueryId(process.routeProcessId, context.routeProcessId)) &&
    (!context.processId || sameRouteQueryId(process.processId, context.processId))
  )
</script>

<style scoped lang="scss">
.frontline-operator-panel {
  position: relative;
  margin-bottom: 12px;
}

.frontline-operator-screen {
  --frontline-bg: #eef3ef;
  --frontline-panel: #ffffff;
  --frontline-ink: #111a15;
  --frontline-muted: #5b665f;
  --frontline-line: #cbd6ce;
  --frontline-dark: #24322b;
  display: grid;
  grid-template-rows: 130px minmax(0, 1fr) 110px;
  gap: 20px;
  min-height: min(1080px, calc(100vh - 180px));
  padding: 28px;
  overflow: hidden;
  border-radius: 18px;
  background: var(--frontline-bg);
  color: var(--frontline-ink);
}

.frontline-operator-top {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;

  &.is-pqc {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}

.frontline-top-card,
.frontline-home-button {
  min-width: 0;
  border: 3px solid var(--frontline-line);
  border-radius: 22px;
  font: inherit;
}

.frontline-top-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 22px 26px;
  background: var(--frontline-panel);
  text-align: left;
  cursor: pointer;

  span {
    color: var(--frontline-muted);
    font-size: 28px;
    font-weight: 700;
    line-height: 1;
  }

  strong {
    min-width: 0;
    margin-top: 12px;
    overflow: hidden;
    font-size: 42px;
    font-weight: 900;
    line-height: 1.1;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.frontline-top-card__order {
  font-size: 32px !important;
}

.frontline-home-button {
  background: var(--frontline-dark);
  color: #ffffff;
  font-size: 42px;
  font-weight: 900;
  cursor: pointer;
}

.frontline-operator-main {
  display: grid;
  grid-template-columns: 780px minmax(0, 1fr);
  gap: 28px;
  min-height: 0;
}

.frontline-work-panel {
  display: grid;
  align-content: start;
  gap: 22px;
  min-width: 0;
  padding: 26px;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: var(--frontline-panel);

  h3 {
    margin: 0;
    font-size: 48px;
    font-weight: 900;
    line-height: 1;
  }
}

.frontline-production-field,
.frontline-inspection-row {
  display: grid;
  align-items: center;
  min-width: 0;

  span {
    font-weight: 900;
  }

  em {
    color: var(--frontline-muted);
    font-style: normal;
    font-weight: 900;
  }
}

.frontline-production-field {
  grid-template-columns: 250px minmax(0, 1fr) 50px;
  gap: 16px;

  span {
    font-size: 36px;
    line-height: 1.15;
  }

  em {
    font-size: 30px;
  }
}

.frontline-device-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.frontline-device-card {
  display: grid;
  gap: 16px;
  min-width: 0;
  padding: 20px;
  border: 3px solid var(--frontline-line);
  border-radius: 20px;
  background: #f8faf8;

  span {
    overflow: hidden;
    font-size: 34px;
    font-weight: 900;
    line-height: 1.1;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.frontline-no-device {
  display: grid;
  place-items: center;
  min-height: 240px;
  border: 3px dashed var(--frontline-line);
  border-radius: 20px;
  color: var(--frontline-muted);
  font-size: 40px;
  font-weight: 900;
}

.frontline-inspection-list {
  display: grid;
  gap: 14px;
}

.frontline-inspection-row {
  grid-template-columns: 150px minmax(0, 1fr) auto;
  gap: 14px;
  padding: 14px 18px;
  border: 3px solid var(--frontline-line);
  border-radius: 20px;
  background: #f8faf8;

  span {
    color: var(--frontline-muted);
    font-size: 30px;
  }

  em {
    font-size: 28px;
  }
}

.frontline-choice-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;

  button {
    height: 92px;
    border: 3px solid var(--frontline-line);
    border-radius: 20px;
    background: #f8faf8;
    color: var(--frontline-ink);
    font-size: 38px;
    font-weight: 900;
    cursor: pointer;

    &.active {
      border-color: var(--frontline-dark);
      background: var(--frontline-dark);
      color: #ffffff;
    }
  }
}

.frontline-number-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;

  label {
    display: grid;
    gap: 12px;
    min-width: 0;
  }

  span {
    font-size: 32px;
    font-weight: 900;
  }
}

.frontline-submit-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: var(--frontline-panel);

  span {
    min-width: 0;
    overflow: hidden;
    color: var(--frontline-muted);
    font-size: 30px;
    font-weight: 800;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.el-button) {
    width: 100%;
    height: 72px;
    border-radius: 20px;
    font-size: 36px;
    font-weight: 900;
  }
}

.frontline-picker {
  position: absolute;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: rgba(17, 26, 21, 0.38);
}

.frontline-picker__card {
  display: grid;
  gap: 20px;
  width: min(760px, calc(100% - 80px));
  padding: 28px;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: var(--frontline-panel);

  h3 {
    margin: 0;
    font-size: 48px;
    font-weight: 900;
    line-height: 1;
  }
}

.frontline-picker__options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  max-height: 520px;
  overflow: auto;

  button {
    min-height: 112px;
    border: 3px solid var(--frontline-line);
    border-radius: 22px;
    background: #f8faf8;
    color: var(--frontline-ink);
    font-size: 34px;
    font-weight: 900;
    cursor: pointer;

    &.active {
      border-color: var(--frontline-dark);
      background: var(--frontline-dark);
      color: #ffffff;
    }
  }
}

.frontline-picker__close {
  height: 86px;
  border: 3px solid var(--frontline-line);
  border-radius: 22px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-size: 36px;
  font-weight: 900;
  cursor: pointer;
}

.frontline-operator-screen :deep(.el-input-number),
.frontline-operator-screen :deep(.el-input),
.frontline-operator-screen :deep(.el-radio-group) {
  width: 100%;
}

.frontline-operator-screen :deep(.el-input-number .el-input__wrapper),
.frontline-operator-screen :deep(.el-input .el-input__wrapper) {
  min-height: 76px;
  border-radius: 18px;
  font-size: 34px;
}

.frontline-operator-screen :deep(.el-radio-button) {
  flex: 1;
}

.frontline-operator-screen :deep(.el-radio-button__inner) {
  width: 100%;
  min-height: 76px;
  padding: 20px 18px;
  border-radius: 18px;
  font-size: 30px;
  font-weight: 900;
}

@media (max-width: 1280px) {
  .frontline-operator-screen {
    min-height: 860px;
  }

  .frontline-operator-top,
  .frontline-operator-top.is-pqc,
  .frontline-operator-main,
  .frontline-device-grid,
  .frontline-number-grid {
    grid-template-columns: 1fr;
  }
}
</style>
