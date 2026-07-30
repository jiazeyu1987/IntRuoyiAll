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
  </section>
</template>

<script setup lang="ts">
import {
  FRONTLINE_FIELD_CODES,
  FRONTLINE_TEMPLATE_CODES,
  FrontlineTemplateApi,
  type FrontlineTemplateCode,
  type FrontlineTemplateDefinitionVO,
  type FrontlineTemplatePayloadVO
} from '@/api/mes/pro/feedbackFrontlineTemplate'
import type {
  FrontlineDeviceRouteProcessVO,
  FrontlineEmployeeCandidateVO
} from '@/api/mes/pro/feedback'
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
const activePicker = ref<PickerType>()
const deviceState = reactive(createFrontlineDeviceEmployeeState())
const employeeTemplateCode = ref<FrontlineTemplateCode>()

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

const inspectionTypeOptions: Array<{ value: InspectionType; label: string }> = [
  { value: 'FIRST', label: '首检' },
  { value: 'PATROL', label: '巡检' },
  { value: 'FINAL', label: '末检' }
]

const patrolRounds = [1, 2, 3]

const productionOrderLabel = computed(() =>
  firstRouteQueryText(['productionOrderCode', 'workOrderCode', 'orderCode']) || '未选择订单'
)

const isPqcMode = computed(() => props.mode === 'pqc')

const selectedProcessLabel = computed(() => formatProcessLabel(deviceState.selectedProcess))

const selectedEmployeeLabel = computed(() => formatEmployeeLabel(deviceState.selectedEmployee))

const templateModeMismatch = computed(() =>
  Boolean(employeeTemplateCode.value && employeeTemplateCode.value !== expectedTemplateCode.value)
)

const templateBindingMissing = computed(() =>
  Boolean(deviceState.selectedEmployee && !employeeTemplateCode.value)
)

const isSubmitBlocked = computed(() =>
  payloadLoading.value ||
  templateModeMismatch.value ||
  templateBindingMissing.value ||
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

const handleSelectProcess = async (process: FrontlineDeviceRouteProcessVO) => {
  await selectFrontlineProcess(deviceState, process)
  applyProcessToContext(process)
  employeeTemplateCode.value = undefined
  const firstEmployee = deviceState.employeeOptions[0]
  if (firstEmployee) {
    await handleSelectEmployee(firstEmployee)
  }
  closePicker()
}

const handleSelectEmployee = async (employee: FrontlineEmployeeCandidateVO) => {
  const result = await switchFrontlineActualEmployee(deviceState, employee.userId)
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
  if (isPqcMode.value) {
    const error = new Error('PQC 详细检验内容尚未纳入正式模板字段，无法按正式 payload 提交。')
    message.error(error.message)
    throw error
  }
  Object.assign(draft.fieldValues, buildProductionFieldValues())
  payloadLoading.value = true
  try {
    assertFormalPayloadContext()
    payloadPreview.value = await FrontlineTemplateApi.validatePayload(
      buildFrontlineTemplatePayload(context, draft.fieldValues)
    )
    message.success('已提交')
  } catch (error) {
    message.error(resolveErrorMessage(error))
    throw error
  } finally {
    payloadLoading.value = false
  }
}

const assertFormalPayloadContext = () => {
  const missingFields: string[] = []
  if (!context.workOrderId) {
    missingFields.push('订单上下文')
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

const applyProcessToContext = (process: FrontlineDeviceRouteProcessVO) => {
  context.routeId = process.routeId
  context.routeProcessId = process.routeProcessId
  context.processId = process.processId
}

const hydrateContextFromRoute = () => {
  context.workOrderId = firstRouteQueryNumber(['workOrderId', 'productionOrderId', 'orderId'])
  context.routeId = firstRouteQueryNumber(['routeId']) ?? context.routeId
  context.routeProcessId = firstRouteQueryNumber(['routeProcessId']) ?? context.routeProcessId
  context.processId = firstRouteQueryNumber(['processId']) ?? context.processId
  context.actualEmployeeId = firstRouteQueryNumber(['actualEmployeeId']) ?? context.actualEmployeeId
  const queryTemplateCode = resolveTemplateCode(firstRouteQueryText(['templateCode', 'templateNo']))
  employeeTemplateCode.value = queryTemplateCode
  context.templateCode = expectedTemplateCode.value
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
  left.routeId === right.routeId &&
  left.routeProcessId === right.routeProcessId &&
  left.processId === right.processId

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
  const firstProcess = deviceState.processOptions[0]
  if (firstProcess) {
    await handleSelectProcess(firstProcess)
  }
  Object.assign(draft.fieldValues, buildProductionFieldValues())
})
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
  grid-template-columns: 1fr 1fr 240px;
  gap: 20px;

  &.is-pqc {
    grid-template-columns: 380px 520px 1fr 240px;
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
