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

      <div
        v-if="activePqcInspectionItem"
        class="frontline-pqc-piece-modal"
        data-pqc-piece-modal
        role="dialog"
        aria-modal="true"
        :aria-label="`${activePqcInspectionItem.label}逐件检验`"
        @click.self="closePqcPieceInspection(false)"
      >
        <section class="frontline-pqc-piece-dialog">
          <h3>{{ activePqcInspectionItem.label }}（{{ pqcInspectionQuantity }}件）</h3>
          <div class="frontline-pqc-piece-list" data-pqc-piece-list>
            <article
              v-for="pieceIndex in pqcInspectionQuantity"
              :key="pieceIndex"
              class="frontline-pqc-piece-row"
            >
              <strong>{{ pieceIndex }}</strong>
              <div
                v-if="activePqcInspectionItem.type === 'number'"
                class="frontline-pqc-piece-value-control"
              >
                <button
                  type="button"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}减少`"
                  @click="stepPqcPieceValue(pieceIndex - 1, -activePqcInspectionItem.step)"
                >
                  -
                </button>
                <input
                  :value="pqcPieceDraftValues[pieceIndex - 1]"
                  type="number"
                  :step="activePqcInspectionItem.step"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}`"
                  @input="updatePqcPieceDraftValue(pieceIndex - 1, $event)"
                />
                <button
                  type="button"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}增加`"
                  @click="stepPqcPieceValue(pieceIndex - 1, activePqcInspectionItem.step)"
                >
                  +
                </button>
                <span>{{ activePqcInspectionItem.unit }}</span>
              </div>
              <div v-else class="frontline-pqc-piece-choice">
                <button
                  type="button"
                  class="pass"
                  :class="{ active: pqcPieceDraftValues[pieceIndex - 1] === '合格' }"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}合格`"
                  @click="pqcPieceDraftValues[pieceIndex - 1] = '合格'"
                >
                  合格
                </button>
                <button
                  type="button"
                  class="fail"
                  :class="{ active: pqcPieceDraftValues[pieceIndex - 1] === '不合格' }"
                  :aria-label="`第 ${pieceIndex} 件${activePqcInspectionItem.label}不合格`"
                  @click="pqcPieceDraftValues[pieceIndex - 1] = '不合格'"
                >
                  不合格
                </button>
              </div>
            </article>
          </div>
          <footer class="frontline-pqc-piece-actions">
            <button type="button" @click="closePqcPieceInspection(false)">返回</button>
            <button type="button" class="primary" @click="closePqcPieceInspection(true)">
              完成
            </button>
          </footer>
        </section>
      </div>

      <main class="frontline-operator-main is-pqc">
        <section
          class="frontline-work-panel frontline-pqc-content-panel"
          data-frontline-pqc-inspection-content
        >
          <h3>检验内容</h3>
          <div class="frontline-pqc-inspection-list">
            <button
              class="frontline-pqc-content-item"
              type="button"
              data-pqc-inspection-entry="length"
              aria-label="长度（厘米）逐件检验"
              @click="openPqcPieceInspection('length')"
            >
              <span>长度</span>
              <em>{{ getPqcProgressText('length') }}</em>
              <strong aria-hidden="true">&gt;</strong>
            </button>

            <div
              class="frontline-pqc-choice-item"
              data-pqc-inspection-entry="appearance"
              data-pqc-inspection-group="appearance"
            >
              <div class="frontline-pqc-choice-title">外观</div>
              <div class="frontline-pqc-choice-actions">
                <button
                  type="button"
                  class="pass"
                  :class="{ active: isPqcBulkChoiceActive('appearance', '合格') }"
                  @click="applyPqcBulkChoice('appearance', '合格')"
                >
                  全部合格
                </button>
                <button
                  type="button"
                  class="fail"
                  :class="{ active: isPqcBulkChoiceActive('appearance', '不合格') }"
                  @click="applyPqcBulkChoice('appearance', '不合格')"
                >
                  全部不良
                </button>
                <button
                  type="button"
                  class="manual"
                  :class="{ active: isPqcManualChoiceActive('appearance') }"
                  @click="openPqcPieceInspection('appearance')"
                >
                  <span>逐件选择</span>
                  <em>{{ getPqcProgressText('appearance') }}</em>
                  <strong aria-hidden="true">&gt;</strong>
                </button>
              </div>
            </div>

            <div
              class="frontline-pqc-choice-item"
              data-pqc-inspection-entry="seal"
              data-pqc-inspection-group="seal"
            >
              <div class="frontline-pqc-choice-title">密封</div>
              <div class="frontline-pqc-choice-actions">
                <button
                  type="button"
                  class="pass"
                  :class="{ active: isPqcBulkChoiceActive('seal', '合格') }"
                  @click="applyPqcBulkChoice('seal', '合格')"
                >
                  全部合格
                </button>
                <button
                  type="button"
                  class="fail"
                  :class="{ active: isPqcBulkChoiceActive('seal', '不合格') }"
                  @click="applyPqcBulkChoice('seal', '不合格')"
                >
                  全部不良
                </button>
                <button
                  type="button"
                  class="manual"
                  :class="{ active: isPqcManualChoiceActive('seal') }"
                  @click="openPqcPieceInspection('seal')"
                >
                  <span>逐件选择</span>
                  <em>{{ getPqcProgressText('seal') }}</em>
                  <strong aria-hidden="true">&gt;</strong>
                </button>
              </div>
            </div>

            <button
              class="frontline-pqc-content-item"
              type="button"
              data-pqc-inspection-entry="pressure"
              aria-label="压力（MPa）逐件检验"
              @click="openPqcPieceInspection('pressure')"
            >
              <span>压力</span>
              <em>{{ getPqcProgressText('pressure') }}</em>
              <strong aria-hidden="true">&gt;</strong>
            </button>
          </div>
        </section>

        <section class="frontline-work-panel frontline-pqc-fill-panel">
          <h3>填检验</h3>
          <div class="frontline-pqc-type-tabs">
            <button
              type="button"
              :class="{ active: pqcDraft.inspectionType === 'FIRST' }"
              @click="selectPqcInspectionType('FIRST')"
            >
              首检
            </button>
            <button
              type="button"
              :class="{ active: pqcDraft.inspectionType === 'PATROL' }"
              @click="selectPqcInspectionType('PATROL')"
            >
              巡检
            </button>
            <button
              type="button"
              :class="{ active: pqcDraft.inspectionType === 'FINAL' }"
              @click="selectPqcInspectionType('FINAL')"
            >
              末检
            </button>
          </div>
          <div
            class="frontline-pqc-round-tabs"
            :style="{ gridTemplateColumns: `repeat(${pqcVisibleRounds.length}, minmax(0, 1fr))` }"
          >
            <button
              v-for="round in pqcVisibleRounds"
              :key="round.value"
              type="button"
              :class="{ active: pqcDraft.patrolRound === round.value }"
              @click="pqcDraft.patrolRound = round.value"
            >
              {{ round.label }}
            </button>
          </div>
          <div class="frontline-pqc-form-area">
            <div class="frontline-pqc-number-field">
              <label for="frontlinePqcInspectionQuantity">检验数量</label>
              <button
                type="button"
                aria-label="检验数量减少"
                @click="adjustPqcQuantity('inspectionQuantity', -1)"
              >
                -
              </button>
              <input
                id="frontlinePqcInspectionQuantity"
                :value="pqcDraft.inspectionQuantity ?? ''"
                type="number"
                min="0"
                inputmode="numeric"
                @input="updatePqcQuantity('inspectionQuantity', $event)"
              />
              <button
                type="button"
                aria-label="检验数量增加"
                @click="adjustPqcQuantity('inspectionQuantity', 1)"
              >
                +
              </button>
              <span>件</span>
            </div>
            <div class="frontline-pqc-number-field">
              <label for="frontlinePqcScrapQuantity">损耗数量</label>
              <button
                type="button"
                aria-label="损耗数量减少"
                @click="adjustPqcQuantity('scrapQuantity', -1)"
              >
                -
              </button>
              <input
                id="frontlinePqcScrapQuantity"
                :value="pqcDraft.scrapQuantity ?? ''"
                type="number"
                min="0"
                inputmode="numeric"
                @input="updatePqcQuantity('scrapQuantity', $event)"
              />
              <button
                type="button"
                aria-label="损耗数量增加"
                @click="adjustPqcQuantity('scrapQuantity', 1)"
              >
                +
              </button>
              <span>件</span>
            </div>
          </div>
        </section>
      </main>

      <footer class="frontline-pqc-submit-bar">
        <button
          class="frontline-pqc-reset-button"
          type="button"
          @click="handleResetPqc"
        >
          重填
        </button>
        <button
          class="frontline-pqc-submit-button"
          type="button"
          :disabled="isSubmitBlocked"
          @click="handleValidate"
        >
          {{ payloadLoading ? '提交中' : '提交' }}
        </button>
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

      <main
        class="frontline-operator-main frontline-production-main"
        :class="{ 'is-no-device': !visibleDeviceCards.length }"
      >
        <section
          class="frontline-work-panel frontline-production-quantity-panel"
          :class="{ 'is-no-device': !visibleDeviceCards.length }"
          aria-label="数量与不良"
        >
          <h3>填数量</h3>
          <div class="frontline-production-quantity-body">
            <div class="frontline-production-quantity-fields">
              <div class="frontline-production-number-field">
                <label for="frontlineProductionOutputQuantity">完成数量</label>
                <button
                  type="button"
                  aria-label="完成数量减少"
                  @click="adjustProductionOutputQuantity(-1)"
                >
                  -
                </button>
                <input
                  id="frontlineProductionOutputQuantity"
                  :value="productionDraft.outputQuantity ?? ''"
                  inputmode="numeric"
                  @input="updateProductionOutputQuantity"
                />
                <button
                  type="button"
                  aria-label="完成数量增加"
                  @click="adjustProductionOutputQuantity(1)"
                >
                  +
                </button>
                <span>件</span>
              </div>

              <div class="frontline-production-number-field is-total">
                <label for="frontlineProductionScrapQuantity">损耗数量</label>
                <input
                  id="frontlineProductionScrapQuantity"
                  :value="productionScrapQuantity"
                  inputmode="numeric"
                  readonly
                />
                <span>件</span>
              </div>
            </div>

            <section class="frontline-production-defect-section" aria-label="不良明细">
              <div class="frontline-production-defect-title">不良明细</div>
              <div class="frontline-production-defect-grid">
                <div
                  v-for="defect in productionDefects"
                  :key="defect.key"
                  class="frontline-production-defect-card"
                  :class="{ active: getProductionDefectQuantity(defect.key) > 0 }"
                  :data-defect-key="defect.key"
                >
                  <span class="frontline-production-defect-name">{{ defect.label }}</span>
                  <button
                    type="button"
                    class="frontline-production-defect-step"
                    :aria-label="`${defect.label}减少`"
                    @click="adjustProductionDefectQuantity(defect.key, -1)"
                  >
                    -
                  </button>
                  <input
                    class="frontline-production-defect-qty"
                    :value="getProductionDefectQuantity(defect.key)"
                    inputmode="numeric"
                    :aria-label="`${defect.label}数量`"
                    @input="updateProductionDefectQuantity(defect.key, $event)"
                  />
                  <button
                    type="button"
                    class="frontline-production-defect-step"
                    :aria-label="`${defect.label}增加`"
                    @click="adjustProductionDefectQuantity(defect.key, 1)"
                  >
                    +
                  </button>
                  <span class="frontline-production-defect-unit">件</span>
                </div>
              </div>
            </section>
          </div>
        </section>

        <section
          v-if="visibleDeviceCards.length"
          class="frontline-work-panel frontline-production-device-panel"
          aria-label="设备"
        >
          <h3>填设备</h3>
          <div class="frontline-production-device-tabs" role="tablist" aria-label="设备切换">
            <button
              v-for="device in visibleDeviceCards"
              :key="device.key"
              type="button"
              role="tab"
              :aria-selected="device.key === selectedProductionDeviceKey"
              :class="{ active: device.key === selectedProductionDeviceKey }"
              @click="selectedProductionDeviceKey = device.key"
            >
              {{ device.label }}
            </button>
          </div>
          <div v-if="activeProductionDevice" class="frontline-production-device-current">
            <div class="frontline-production-device-param">
              <label for="frontlineProductionDevicePressure">压力</label>
              <button
                type="button"
                aria-label="压力减少"
                @click="adjustProductionDeviceParameter(activeProductionDevice.key, 'pressure', -1)"
              >
                -
              </button>
              <input
                id="frontlineProductionDevicePressure"
                :value="getProductionDeviceParameter(activeProductionDevice.key, 'pressure')"
                inputmode="decimal"
                @input="updateProductionDeviceParameter(activeProductionDevice.key, 'pressure', $event)"
              />
              <button
                type="button"
                aria-label="压力增加"
                @click="adjustProductionDeviceParameter(activeProductionDevice.key, 'pressure', 1)"
              >
                +
              </button>
              <span>MPa</span>
            </div>

            <div class="frontline-production-device-param">
              <label for="frontlineProductionDeviceTime">时间</label>
              <button
                type="button"
                aria-label="时间减少"
                @click="adjustProductionDeviceParameter(activeProductionDevice.key, 'time', -1)"
              >
                -
              </button>
              <input
                id="frontlineProductionDeviceTime"
                :value="getProductionDeviceParameter(activeProductionDevice.key, 'time')"
                inputmode="numeric"
                @input="updateProductionDeviceParameter(activeProductionDevice.key, 'time', $event)"
              />
              <button
                type="button"
                aria-label="时间增加"
                @click="adjustProductionDeviceParameter(activeProductionDevice.key, 'time', 1)"
              >
                +
              </button>
              <span>秒</span>
            </div>
          </div>
        </section>
      </main>

      <footer class="frontline-production-submit-bar">
        <button
          class="frontline-production-reset-button"
          type="button"
          @click="handleResetProduction"
        >
          重填
        </button>
        <button
          class="frontline-production-submit-button"
          type="button"
          :disabled="isSubmitBlocked || payloadLoading"
          @click="handleValidate"
        >
          {{ payloadLoading ? '提交中' : '提交' }}
        </button>
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
type PqcInspectionItemKey = 'length' | 'appearance' | 'seal' | 'pressure'
type PqcChoiceResult = '合格' | '不合格'
type PqcQuantityField = 'inspectionQuantity' | 'scrapQuantity'
type ProductionDeviceParameterKey = 'pressure' | 'time'

interface PqcInspectionItem {
  label: string
  type: 'number' | 'choice'
  unit: string
  defaultValue: string
  step: number
}

const pqcInspectionItems: Record<PqcInspectionItemKey, PqcInspectionItem> = {
  length: {
    label: '长度',
    type: 'number',
    unit: '厘米',
    defaultValue: '32.5',
    step: 0.1
  },
  appearance: {
    label: '外观',
    type: 'choice',
    unit: '',
    defaultValue: '',
    step: 0
  },
  seal: {
    label: '密封',
    type: 'choice',
    unit: '',
    defaultValue: '',
    step: 0
  },
  pressure: {
    label: '压力',
    type: 'number',
    unit: 'MPa',
    defaultValue: '50',
    step: 1
  }
}

const pqcInspectionItemKeys = Object.keys(pqcInspectionItems) as PqcInspectionItemKey[]

const productionDefects = [
  { key: 'sealScratch', label: '密封件划伤' },
  { key: 'assembly', label: '装配不到位' },
  { key: 'appearance', label: '外观磕碰' },
  { key: 'dimension', label: '尺寸超差' },
  { key: 'leak', label: '泄漏' },
  { key: 'pressure', label: '压力异常' },
  { key: 'other', label: '其他不良' }
] as const

type ProductionDefectKey = (typeof productionDefects)[number]['key']
type ProductionDeviceParameterDraft = Partial<Record<ProductionDeviceParameterKey, number | undefined>>

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
  outputQuantity: undefined as number | undefined
})

const productionDefectDraft = reactive(
  productionDefects.reduce((draftValues, defect) => {
    draftValues[defect.key] = 0
    return draftValues
  }, {} as Record<ProductionDefectKey, number>)
)

const selectedProductionDeviceKey = ref<string>()
const deviceParameterDraft = reactive<Record<string, ProductionDeviceParameterDraft>>({})

const pqcDraft = reactive({
  inspectionType: 'PATROL' as InspectionType,
  patrolRound: 1,
  inspectionQuantity: undefined as number | undefined,
  scrapQuantity: undefined as number | undefined
})

const activePqcInspectionKey = ref<PqcInspectionItemKey>()
const pqcPieceDraftValues = ref<string[]>([])
const pqcPieceValues = reactive<Record<string, string[]>>({})

const productionOrderLabel = computed(() =>
  firstRouteQueryText(['productionOrderCode', 'workOrderCode', 'orderCode']) || '未选择订单'
)

const isPqcMode = computed(() => props.mode === 'pqc')

const selectedProcessLabel = computed(() => formatProcessLabel(deviceState.selectedProcess))

const selectedEmployeeLabel = computed(() => formatEmployeeLabel(deviceState.selectedEmployee))

const productionScrapQuantity = computed(() =>
  productionDefects.reduce(
    (total, defect) => total + (productionDefectDraft[defect.key] || 0),
    0
  )
)

const pqcInspectionQuantity = computed(() =>
  normalizePqcQuantity(pqcDraft.inspectionQuantity)
)

const activePqcInspectionItem = computed(() =>
  activePqcInspectionKey.value
    ? pqcInspectionItems[activePqcInspectionKey.value]
    : undefined
)

const pqcVisibleRounds = computed(() => {
  if (pqcDraft.inspectionType === 'FIRST') {
    return [{ value: 1, label: '首检' }]
  }
  if (pqcDraft.inspectionType === 'FINAL') {
    return [{ value: 1, label: '末检' }]
  }
  return [1, 2, 3].map((round) => ({
    value: round,
    label: `第 ${round} 次`
  }))
})

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
    const deviceId = Number(process.deviceId)
    if (!Number.isFinite(deviceId) || deviceId <= 0 || seen.has(deviceId)) {
      continue
    }
    seen.add(deviceId)
    cards.push({
      key: String(deviceId),
      label: process.deviceName || process.deviceCode || `设备 ${cards.length + 1}`
    })
  }
  return cards
})

const visibleDeviceCards = computed(() => selectedDeviceCards.value.slice(0, 3))

const activeProductionDevice = computed(() =>
  visibleDeviceCards.value.find((device) => device.key === selectedProductionDeviceKey.value) ||
  visibleDeviceCards.value[0]
)

const switchableProcessOptions = computed(() => {
  const seen = new Set<string>()
  return deviceState.processOptions.filter((process) => {
    const key = `${process.routeId}-${process.routeProcessId}-${process.processId}`
    if (seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
})

const pickerOptions = computed(() => {
  if (activePicker.value === 'process') {
    return switchableProcessOptions.value.map((process) => ({
      key: `${process.routeId}-${process.routeProcessId}-${process.processId}`,
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
  visibleDeviceCards,
  (devices) => {
    if (!devices.length) {
      selectedProductionDeviceKey.value = undefined
      return
    }
    if (!devices.some((device) => device.key === selectedProductionDeviceKey.value)) {
      selectedProductionDeviceKey.value = devices[0].key
    }
  },
  { immediate: true }
)

watch(
  [productionDraft, selectedDeviceCards, deviceParameterDraft, productionDefectDraft],
  () => {
    if (!isPqcMode.value) {
      Object.assign(draft.fieldValues, buildProductionFieldValues())
    }
  },
  { deep: true }
)

const normalizeProductionQuantity = (value: unknown) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return 0
  }
  return Math.max(0, Math.trunc(parsed))
}

const normalizeProductionParameter = (value: unknown) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return undefined
  }
  return Math.max(0, parsed)
}

const updateProductionOutputQuantity = (event: Event) => {
  const value = (event.target as HTMLInputElement).value.trim()
  productionDraft.outputQuantity = value === '' ? undefined : normalizeProductionQuantity(value)
}

const adjustProductionOutputQuantity = (delta: number) => {
  productionDraft.outputQuantity = normalizeProductionQuantity(productionDraft.outputQuantity) + delta
  if (productionDraft.outputQuantity < 0) {
    productionDraft.outputQuantity = 0
  }
}

const getProductionDefectQuantity = (defectKey: ProductionDefectKey) =>
  productionDefectDraft[defectKey] || 0

const updateProductionDefectQuantity = (
  defectKey: ProductionDefectKey,
  event: Event
) => {
  productionDefectDraft[defectKey] = normalizeProductionQuantity(
    (event.target as HTMLInputElement).value
  )
}

const adjustProductionDefectQuantity = (
  defectKey: ProductionDefectKey,
  delta: number
) => {
  productionDefectDraft[defectKey] = Math.max(
    0,
    getProductionDefectQuantity(defectKey) + delta
  )
}

const ensureProductionDeviceParameters = (deviceKey: string) => {
  if (!deviceParameterDraft[deviceKey]) {
    deviceParameterDraft[deviceKey] = {}
  }
  return deviceParameterDraft[deviceKey]
}

const getProductionDeviceParameter = (
  deviceKey: string,
  parameterKey: ProductionDeviceParameterKey
) => ensureProductionDeviceParameters(deviceKey)[parameterKey] ?? ''

const updateProductionDeviceParameter = (
  deviceKey: string,
  parameterKey: ProductionDeviceParameterKey,
  event: Event
) => {
  const value = (event.target as HTMLInputElement).value.trim()
  ensureProductionDeviceParameters(deviceKey)[parameterKey] =
    value === '' ? undefined : normalizeProductionParameter(value)
}

const adjustProductionDeviceParameter = (
  deviceKey: string,
  parameterKey: ProductionDeviceParameterKey,
  delta: number
) => {
  const params = ensureProductionDeviceParameters(deviceKey)
  params[parameterKey] = Math.max(0, Number(params[parameterKey] || 0) + delta)
}

const handleResetProduction = () => {
  productionDraft.outputQuantity = undefined
  for (const defect of productionDefects) {
    productionDefectDraft[defect.key] = 0
  }
  for (const deviceKey of Object.keys(deviceParameterDraft)) {
    delete deviceParameterDraft[deviceKey]
  }
}

const normalizePqcQuantity = (value?: number) => {
  if (!Number.isFinite(value)) {
    return 0
  }
  return Math.max(0, Math.trunc(Number(value)))
}

const getPqcPieceStateKey = (itemKey: PqcInspectionItemKey) => {
  const process = deviceState.selectedProcess
  if (!process) {
    return undefined
  }
  const inspectionType = pqcDraft.inspectionType
  const patrolRound = pqcDraft.patrolRound
  return [
    process.routeId,
    process.routeProcessId,
    process.processId,
    inspectionType,
    patrolRound,
    itemKey
  ].join(':')
}

const getPqcStoredPieceValues = (itemKey: PqcInspectionItemKey) => {
  const stateKey = getPqcPieceStateKey(itemKey)
  if (!stateKey) {
    return []
  }
  const item = pqcInspectionItems[itemKey]
  const quantity = pqcInspectionQuantity.value
  const values = pqcPieceValues[stateKey] || []
  while (values.length < quantity) {
    values.push(item.defaultValue)
  }
  pqcPieceValues[stateKey] = values
  return values
}

const getPqcCompletedCount = (itemKey: PqcInspectionItemKey) =>
  getPqcStoredPieceValues(itemKey)
    .slice(0, pqcInspectionQuantity.value)
    .filter((value) => value.trim().length > 0).length

const getPqcProgressText = (itemKey: PqcInspectionItemKey) =>
  `已填 ${getPqcCompletedCount(itemKey)}/${pqcInspectionQuantity.value}`

const getPqcCurrentChoiceValues = (itemKey: 'appearance' | 'seal') =>
  getPqcStoredPieceValues(itemKey).slice(0, pqcInspectionQuantity.value)

const isPqcBulkChoiceActive = (
  itemKey: 'appearance' | 'seal',
  result: PqcChoiceResult
) => {
  const values = getPqcCurrentChoiceValues(itemKey)
  return values.length > 0 && values.every((value) => value === result)
}

const isPqcManualChoiceActive = (itemKey: 'appearance' | 'seal') => {
  const values = getPqcCurrentChoiceValues(itemKey)
  const completed = values.filter((value) => value.trim().length > 0).length
  const allPass = values.length > 0 && values.every((value) => value === '合格')
  const allFail = values.length > 0 && values.every((value) => value === '不合格')
  return completed > 0 && !allPass && !allFail
}

const assertPqcPieceContext = () => {
  if (!deviceState.selectedProcess) {
    const error = new Error('请先选择工序，再填写逐件检验。')
    message.error(error.message)
    throw error
  }
  if (pqcInspectionQuantity.value <= 0) {
    const error = new Error('请先填写大于 0 的检验数量。')
    message.error(error.message)
    throw error
  }
}

const openPqcPieceInspection = (itemKey: PqcInspectionItemKey) => {
  assertPqcPieceContext()
  activePqcInspectionKey.value = itemKey
  pqcPieceDraftValues.value = getPqcStoredPieceValues(itemKey).slice()
}

const closePqcPieceInspection = (saveChanges: boolean) => {
  const itemKey = activePqcInspectionKey.value
  if (saveChanges && itemKey) {
    const stateKey = getPqcPieceStateKey(itemKey)
    if (!stateKey) {
      const error = new Error('当前工序上下文已失效，无法保存逐件检验。')
      message.error(error.message)
      throw error
    }
    pqcPieceValues[stateKey] = pqcPieceDraftValues.value.slice()
  }
  activePqcInspectionKey.value = undefined
  pqcPieceDraftValues.value = []
}

const applyPqcBulkChoice = (
  itemKey: 'appearance' | 'seal',
  result: PqcChoiceResult
) => {
  assertPqcPieceContext()
  const values = getPqcStoredPieceValues(itemKey)
  for (let index = 0; index < pqcInspectionQuantity.value; index += 1) {
    values[index] = result
  }
}

const stepPqcPieceValue = (index: number, delta: number) => {
  const item = activePqcInspectionItem.value
  if (!item || item.type !== 'number') {
    const error = new Error('当前检验项目不是数值项目，无法调整数值。')
    message.error(error.message)
    throw error
  }
  const current = Number(pqcPieceDraftValues.value[index] || item.defaultValue)
  const precision = item.step < 1 ? String(item.step).split('.')[1]?.length || 0 : 0
  pqcPieceDraftValues.value[index] = String(
    Number((current + delta).toFixed(precision))
  )
}

const updatePqcPieceDraftValue = (index: number, event: Event) => {
  pqcPieceDraftValues.value[index] = (event.target as HTMLInputElement).value
}

const selectPqcInspectionType = (inspectionType: InspectionType) => {
  pqcDraft.inspectionType = inspectionType
  pqcDraft.patrolRound = 1
}

const updatePqcQuantity = (field: PqcQuantityField, event: Event) => {
  const inputValue = (event.target as HTMLInputElement).value
  pqcDraft[field] = inputValue === '' ? undefined : normalizePqcQuantity(Number(inputValue))
}

const adjustPqcQuantity = (field: PqcQuantityField, delta: number) => {
  pqcDraft[field] = normalizePqcQuantity(pqcDraft[field]) + delta
  if (pqcDraft[field] < 0) {
    pqcDraft[field] = 0
  }
}

const handleResetPqc = () => {
  for (const itemKey of pqcInspectionItemKeys) {
    const stateKey = getPqcPieceStateKey(itemKey)
    if (stateKey) {
      delete pqcPieceValues[stateKey]
    }
  }
  activePqcInspectionKey.value = undefined
  pqcPieceDraftValues.value = []
}

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

const buildProductionDeviceParameterPayload = (deviceKey: string) => {
  const params = deviceParameterDraft[deviceKey] || {}
  return Object.fromEntries(
    (Object.entries(params) as Array<[ProductionDeviceParameterKey, number | undefined]>)
      .filter(([, value]) => value !== undefined)
  )
}

const buildProductionFieldValues = () => ({
  [FRONTLINE_FIELD_CODES.PREVIOUS_PROCESS_INPUT_QUANTITY]:
    productionDraft.previousProcessInputQuantity,
  [FRONTLINE_FIELD_CODES.DEVICE]: visibleDeviceCards.value.length
    ? visibleDeviceCards.value.map((device) => device.label).join('、')
    : '无设备',
  [FRONTLINE_FIELD_CODES.DEVICE_PARAMETERS]: Object.fromEntries(
    visibleDeviceCards.value.map((device) => [
      device.label,
      buildProductionDeviceParameterPayload(device.key)
    ])
  ),
  [FRONTLINE_FIELD_CODES.OUTPUT_QUANTITY]: productionDraft.outputQuantity,
  [FRONTLINE_FIELD_CODES.SCRAP_QUANTITY]: productionScrapQuantity.value
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
  productionDraft.previousProcessInputQuantity =
    firstRouteQueryNumber(['previousProcessInputQuantity', 'previousInputQuantity']) ??
    productionDraft.previousProcessInputQuantity
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
  const firstProcess = switchableProcessOptions.value[0]
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
  position: relative;
  border-radius: 18px;
  background: var(--frontline-bg);
  color: var(--frontline-ink);

  &.is-pqc {
    grid-template-rows: 130px minmax(0, 1fr) 126px;
    min-height: 860px;
  }
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
  grid-template-columns: 1050px minmax(0, 1fr);
  gap: 28px;
  min-height: 0;

  &.is-pqc {
    grid-template-columns: 780px minmax(0, 1fr);
  }

  &.frontline-production-main.is-no-device {
    grid-template-columns: 1fr;
  }
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

.frontline-production-quantity-panel {
  grid-template-rows: auto minmax(0, 1fr);
  gap: 16px;

  &.is-no-device {
    padding: 36px;
  }
}

.frontline-production-quantity-body {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 16px;
  min-width: 0;
  min-height: 0;
}

.frontline-production-quantity-panel.is-no-device .frontline-production-quantity-body {
  grid-template-rows: minmax(0, 1fr);
  grid-template-columns: 680px minmax(0, 1fr);
  gap: 36px;
}

.frontline-production-quantity-fields {
  display: grid;
  gap: 16px;
  align-content: start;
}

.frontline-production-quantity-panel.is-no-device .frontline-production-quantity-fields {
  grid-template-rows: 108px 108px;
  gap: 28px;
  align-content: center;
}

.frontline-production-number-field {
  display: grid;
  grid-template-columns: 250px 82px minmax(190px, 1fr) 82px 50px;
  gap: 16px;
  align-items: center;
  min-width: 0;

  &.is-total {
    grid-template-columns: 250px minmax(0, 1fr) 50px;
  }

  label {
    font-size: 36px;
    font-weight: 900;
    line-height: 1.15;
  }

  button,
  input {
    width: 100%;
    height: 96px;
    min-width: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 18px;
    background: #f8faf8;
    color: var(--frontline-ink);
    text-align: center;
    font-weight: 900;
  }

  button {
    padding: 0;
    font-size: 50px;
    cursor: pointer;
  }

  input {
    font-size: 52px;

    &[readonly] {
      background: #eef3ef;
    }
  }

  span {
    font-size: 34px;
    font-weight: 800;
  }
}

.frontline-production-quantity-panel.is-no-device .frontline-production-number-field {
  grid-template-columns: 230px 86px minmax(150px, 1fr) 86px 60px;
  gap: 18px;

  &.is-total {
    grid-template-columns: 230px minmax(0, 1fr) 60px;
  }

  label {
    font-size: 40px;
  }

  button,
  input {
    height: 108px;
    border-radius: 20px;
  }

  button {
    font-size: 56px;
  }

  input {
    font-size: 58px;
  }

  span {
    font-size: 38px;
    font-weight: 900;
  }
}

.frontline-production-defect-section {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
}

.frontline-production-defect-title {
  font-size: 32px;
  font-weight: 900;
  line-height: 1;
}

.frontline-production-quantity-panel.is-no-device .frontline-production-defect-title {
  font-size: 38px;
}

.frontline-production-defect-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(4, minmax(0, 1fr));
  gap: 10px;
  min-height: 0;
}

.frontline-production-quantity-panel.is-no-device .frontline-production-defect-grid {
  gap: 12px;
}

.frontline-production-defect-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 58px 76px 58px 34px;
  gap: 8px;
  align-items: center;
  min-width: 0;
  min-height: 0;
  padding: 0 10px;
  border: 3px solid var(--frontline-line);
  border-radius: 16px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font-weight: 900;
  text-align: left;

  &.active {
    border-color: #15815f;
    background: #dff2ea;
  }
}

.frontline-production-quantity-panel.is-no-device .frontline-production-defect-card {
  grid-template-columns: minmax(0, 1fr) 66px 88px 66px 40px;
  gap: 10px;
  padding: 0 14px;
  border-radius: 18px;
}

.frontline-production-defect-name {
  min-width: 0;
  font-size: 24px;
  line-height: 1.15;
}

.frontline-production-quantity-panel.is-no-device .frontline-production-defect-name {
  font-size: 28px;
}

.frontline-production-defect-step,
.frontline-production-defect-qty {
  width: 100%;
  height: 54px;
  min-width: 0;
  border: 3px solid var(--frontline-line);
  border-radius: 12px;
  background: #ffffff;
  color: var(--frontline-ink);
  text-align: center;
  font-weight: 900;
}

.frontline-production-defect-step {
  padding: 0;
  font-size: 34px;
  cursor: pointer;
}

.frontline-production-defect-qty {
  font-size: 30px;
}

.frontline-production-defect-unit {
  font-size: 24px;
  font-weight: 900;
  white-space: nowrap;
}

.frontline-production-quantity-panel.is-no-device {
  .frontline-production-defect-step,
  .frontline-production-defect-qty {
    height: 64px;
    border-radius: 14px;
  }

  .frontline-production-defect-step {
    font-size: 40px;
  }

  .frontline-production-defect-qty {
    font-size: 34px;
  }

  .frontline-production-defect-unit {
    font-size: 28px;
  }
}

.frontline-production-device-panel {
  grid-template-rows: auto 98px minmax(0, 1fr);
  gap: 18px;
  overflow: hidden;
}

.frontline-production-device-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-width: 0;

  button {
    min-width: 0;
    height: 98px;
    padding: 0 8px;
    overflow: hidden;
    border: 3px solid var(--frontline-line);
    border-radius: 20px;
    background: #f8faf8;
    color: var(--frontline-ink);
    font-size: 34px;
    font-weight: 900;
    text-overflow: ellipsis;
    white-space: nowrap;
    cursor: pointer;

    &.active {
      border-color: var(--frontline-dark);
      background: var(--frontline-dark);
      color: #ffffff;
    }
  }
}

.frontline-production-device-current {
  display: grid;
  align-content: start;
  gap: 24px;
  min-width: 0;
  min-height: 0;
  padding: 26px;
  border: 3px solid var(--frontline-line);
  border-radius: 24px;
  background: #fbfdfb;
}

.frontline-production-device-param {
  display: grid;
  grid-template-columns: 150px 82px minmax(0, 1fr) 82px 78px;
  gap: 14px;
  align-items: center;
  min-width: 0;

  label {
    font-size: 38px;
    font-weight: 900;
  }

  button,
  input {
    width: 100%;
    height: 96px;
    min-width: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 16px;
    background: #f8faf8;
    color: var(--frontline-ink);
    text-align: center;
    font-weight: 900;
  }

  button {
    padding: 0;
    font-size: 50px;
    cursor: pointer;
  }

  input {
    font-size: 52px;
  }

  span {
    font-size: 34px;
    font-weight: 900;
  }
}

.frontline-production-submit-bar {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 24px;
}

.frontline-production-reset-button,
.frontline-production-submit-button {
  border-radius: 28px;
  font-size: 54px;
  font-weight: 900;
  cursor: pointer;
}

.frontline-production-reset-button {
  border: 3px solid var(--frontline-line);
  background: #ffffff;
  color: var(--frontline-ink);
}

.frontline-production-submit-button {
  border: 0;
  background: #15815f;
  color: #ffffff;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.48;
  }
}

.frontline-pqc-inspection-list {
  display: grid;
  gap: 14px;
}

.frontline-pqc-content-panel {
  gap: 18px;
}

.frontline-pqc-content-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto 48px;
  gap: 18px;
  align-items: center;
  width: 100%;
  min-width: 0;
  min-height: 100px;
  padding: 0 22px;
  border: 3px solid var(--frontline-line);
  border-radius: 20px;
  background: #f8faf8;
  color: var(--frontline-ink);
  text-align: left;
  cursor: pointer;

  span {
    font-size: 40px;
    font-weight: 900;
  }

  em {
    color: var(--frontline-muted);
    font-size: 30px;
    font-style: normal;
    font-weight: 900;
    white-space: nowrap;
  }

  strong {
    font-size: 48px;
    line-height: 1;
    text-align: right;
  }

  &:focus-visible {
    outline: 5px solid #86c8ad;
    outline-offset: 2px;
  }
}

.frontline-pqc-choice-item {
  min-height: 142px;
  overflow: hidden;
  border: 3px solid var(--frontline-line);
  border-radius: 20px;
  background: #f8faf8;
}

.frontline-pqc-choice-title {
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 18px;
  border-bottom: 3px solid var(--frontline-line);
  background: #ffffff;
  font-size: 32px;
  font-weight: 900;
  line-height: 1;
}

.frontline-pqc-choice-actions {
  display: grid;
  grid-template-columns: 1fr 1fr 1.5fr;
  min-height: 88px;

  > button {
    min-width: 0;
    padding: 10px 12px;
    border: 0;
    border-right: 3px solid var(--frontline-line);
    background: #f8faf8;
    color: var(--frontline-ink);
    font-size: 31px;
    font-weight: 900;
    white-space: nowrap;
    cursor: pointer;

    &:last-child {
      border-right: 0;
    }

    &:focus-visible {
      outline: 5px solid #86c8ad;
      outline-offset: -6px;
    }

    &.pass.active {
      background: #dff2ea;
      color: #15815f;
    }

    &.fail.active {
      background: #f8dfdc;
      color: #b9382f;
    }
  }

  .manual {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 38px;
    grid-template-rows: auto auto;
    gap: 4px 10px;
    align-items: center;
    padding: 10px 16px;
    text-align: left;

    &.active {
      background: #e7f0eb;
    }

    span {
      font-size: 30px;
      line-height: 1;
    }

    em {
      color: var(--frontline-muted);
      font-size: 25px;
      font-style: normal;
      white-space: nowrap;
    }

    strong {
      grid-column: 2;
      grid-row: 1 / span 2;
      font-size: 40px;
      line-height: 1;
    }
  }
}

.frontline-pqc-fill-panel {
  grid-template-rows: auto 86px 104px minmax(0, 1fr);
  gap: 14px;
  overflow: hidden;
}

.frontline-pqc-type-tabs,
.frontline-pqc-round-tabs {
  display: grid;
  gap: 14px;
  min-width: 0;

  button {
    min-width: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 20px;
    background: #f8faf8;
    color: var(--frontline-ink);
    font-weight: 900;
    cursor: pointer;

    &.active {
      border-color: var(--frontline-dark);
      background: var(--frontline-dark);
      color: #ffffff;
    }
  }
}

.frontline-pqc-type-tabs {
  grid-template-columns: repeat(3, minmax(0, 1fr));

  button {
    font-size: 36px;
  }
}

.frontline-pqc-round-tabs {
  button {
    padding: 0 14px;
    font-size: 36px;
  }
}

.frontline-pqc-form-area {
  display: grid;
  align-content: start;
  gap: 14px;
  min-width: 0;
  padding: 20px;
  border: 3px solid var(--frontline-line);
  border-radius: 24px;
  background: #fbfdfb;
}

.frontline-pqc-number-field {
  display: grid;
  grid-template-columns: 190px 82px minmax(0, 1fr) 82px 70px;
  gap: 14px;
  align-items: center;
  min-width: 0;

  label {
    font-size: 34px;
    font-weight: 900;
  }

  button,
  input {
    width: 100%;
    height: 76px;
    min-width: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 16px;
    background: #f8faf8;
    color: var(--frontline-ink);
    text-align: center;
    font-weight: 900;
  }

  button {
    font-size: 44px;
    cursor: pointer;
  }

  input {
    font-size: 42px;
  }

  span {
    font-size: 30px;
    font-weight: 900;
  }
}

.frontline-pqc-submit-bar {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 24px;
}

.frontline-pqc-reset-button,
.frontline-pqc-submit-button {
  border: 0;
  border-radius: 28px;
  font-size: 54px;
  font-weight: 900;
  cursor: pointer;
}

.frontline-pqc-reset-button {
  border: 3px solid var(--frontline-line);
  background: #ffffff;
  color: var(--frontline-ink);
}

.frontline-pqc-submit-button {
  background: #15815f;
  color: #ffffff;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.48;
  }
}

.frontline-pqc-piece-modal {
  position: absolute;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  background: rgba(17, 26, 21, 0.5);
}

.frontline-pqc-piece-dialog {
  display: grid;
  grid-template-rows: 86px minmax(0, 1fr) 96px;
  gap: 14px;
  width: min(1580px, calc(100% - 48px));
  height: min(930px, calc(100% - 48px));
  min-height: 0;
  padding: 24px;
  overflow: hidden;
  border: 3px solid var(--frontline-line);
  border-radius: 28px;
  background: #ffffff;

  h3 {
    display: flex;
    align-items: center;
    margin: 0;
    font-size: 48px;
    font-weight: 900;
    line-height: 1;
  }
}

.frontline-pqc-piece-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  grid-auto-rows: minmax(100px, 1fr);
  gap: 10px;
  align-content: start;
  min-height: 0;
  padding-right: 8px;
  overflow-y: auto;
}

.frontline-pqc-piece-row {
  display: grid;
  grid-template-rows: 24px 52px;
  gap: 4px;
  align-items: center;
  min-width: 0;
  min-height: 100px;
  padding: 6px 10px;
  border: 3px solid var(--frontline-line);
  border-radius: 16px;
  background: #f8faf8;

  > strong {
    font-size: 24px;
    font-weight: 900;
  }
}

.frontline-pqc-piece-value-control {
  display: grid;
  grid-template-columns: 44px minmax(80px, 1fr) 44px 52px;
  gap: 6px;
  align-items: center;
  min-width: 0;

  button,
  input {
    width: 100%;
    height: 50px;
    min-width: 0;
    padding: 0;
    border: 3px solid var(--frontline-line);
    border-radius: 12px;
    background: #ffffff;
    color: var(--frontline-ink);
    text-align: center;
    font-size: 30px;
    font-weight: 900;
  }

  button {
    cursor: pointer;
  }

  span {
    font-size: 22px;
    font-weight: 900;
    white-space: nowrap;
  }
}

.frontline-pqc-piece-choice {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  min-width: 0;

  button {
    height: 56px;
    border: 3px solid var(--frontline-line);
    border-radius: 12px;
    background: #ffffff;
    color: var(--frontline-ink);
    font-size: 24px;
    font-weight: 900;
    cursor: pointer;

    &.pass.active {
      border-color: #86c8ad;
      background: #dff2ea;
      color: #15815f;
    }

    &.fail.active {
      border-color: #dfa8a2;
      background: #f8dfdc;
      color: #b9382f;
    }
  }
}

.frontline-pqc-piece-actions {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 18px;

  button {
    border: 3px solid var(--frontline-line);
    border-radius: 22px;
    background: #ffffff;
    color: var(--frontline-ink);
    font-size: 40px;
    font-weight: 900;
    cursor: pointer;

    &.primary {
      border-color: #15815f;
      background: #15815f;
      color: #ffffff;
    }
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

  .frontline-pqc-choice-actions,
  .frontline-pqc-type-tabs,
  .frontline-pqc-round-tabs,
  .frontline-pqc-number-field,
  .frontline-pqc-submit-bar {
    grid-template-columns: 1fr !important;
  }

  .frontline-pqc-piece-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .frontline-pqc-piece-actions {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
