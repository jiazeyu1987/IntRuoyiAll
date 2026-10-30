<template>
  <section
    class="frontline-operator-panel pqc-trial"
    :class="{ 'is-pqc-fullscreen': maximized }"
    aria-label="一线PQC试填预览"
  >
    <p v-if="!maximized" class="trial-note">试填内容仅用于预览，不会修改规程或生成检验记录。</p>
    <div class="frontline-operator-screen is-pqc">
      <header class="frontline-operator-top is-pqc">
        <button
          class="frontline-top-card frontline-top-card--order-summary"
          type="button"
          @click="batchVisible = true"
        >
          <div class="frontline-production-order-summary"
            ><div class="frontline-production-order-summary__value"
              ><div class="frontline-production-order-summary__code">试填预览</div
              ><div class="frontline-production-order-summary__product">{{
                draft.regulationName
              }}</div></div
            ><div class="frontline-production-order-quantity">{{ batchQuantity }} 件</div></div
          >
        </button>
        <div
          class="frontline-top-card frontline-production-process-nav-card frontline-pqc-process-nav-card"
        >
          <button
            class="frontline-production-process-nav-button"
            type="button"
            aria-label="上一道检验工序"
            :disabled="processIndex === 0"
            @click="emit('select-process', processIndex - 1)"
            ><span
              class="frontline-production-process-nav-icon is-previous"
              aria-hidden="true"
            ></span
          ></button>
          <button
            class="frontline-production-process-current"
            type="button"
            aria-label="选择检验工序"
            @click="processVisible = true"
            ><div class="top-label">检验工序</div
            ><div class="top-value">{{ process?.processName }}</div></button
          >
          <button
            class="frontline-production-process-nav-button"
            type="button"
            aria-label="下一道检验工序"
            :disabled="processIndex >= draft.processes.length - 1"
            @click="emit('select-process', processIndex + 1)"
            ><span class="frontline-production-process-nav-icon is-next" aria-hidden="true"></span
          ></button>
        </div>
        <button
          class="frontline-top-card frontline-production-employee-card is-login-employee"
          type="button"
          disabled
          ><div class="top-label">员工</div
          ><div class="top-value">{{
            userStore.getUser.nickname || userStore.getUser.username
          }}</div></button
        >
        <div class="frontline-return-actions"
          ><button
            class="frontline-home-button frontline-pqc-fullscreen-toggle"
            type="button"
            @click="maximized = !maximized"
            >{{ maximized ? '退出最大化' : '最大化' }}</button
          ><button
            class="frontline-home-button frontline-pqc-facts-button"
            type="button"
            :disabled="!item"
            @click="factsVisible = true"
            >方法和标准</button
          ></div
        >
      </header>
      <main class="frontline-operator-main is-pqc">
        <section class="frontline-work-panel frontline-pqc-content-panel">
          <div class="frontline-pqc-inspection-list">
            <nav
              v-if="items.length"
              class="pqc-item-tabs"
              :style="{ '--pqc-item-columns': Math.min(items.length, 3) }"
              aria-label="PQC检验项目切换"
            >
              <h3 class="frontline-pqc-section-title">检验项目</h3
              ><button
                v-for="i in items"
                :key="i.itemCode"
                class="pqc-item-tab"
                :class="{ active: item?.itemCode === i.itemCode }"
                :aria-pressed="item?.itemCode === i.itemCode"
                type="button"
                @click="selectedCode = i.itemCode"
                ><strong>{{ i.itemName }}</strong></button
              >
            </nav>
            <article v-if="item && state" class="frontline-pqc-content-item">
              <div class="pqc-utility-strip"
                ><div
                  class="pqc-select-card"
                  :class="{ 'is-selected': !!state.equipment, 'is-empty': !state.equipment }"
                  ><span
                    ><strong>检验设备</strong><span>{{ selectedEquipmentLabel }}</span></span
                  ><em v-if="item.equipmentOptions.length" aria-hidden="true">&gt;</em
                  ><select
                    v-if="item.equipmentOptions.length"
                    v-model="state.equipment"
                    class="pqc-select-native"
                    aria-label="选择检验设备"
                    ><option value="">选择检验设备</option
                    ><option
                      v-for="e in item.equipmentOptions"
                      :key="`${e.equipmentId}/${e.equipmentNumber}`"
                      :value="`${e.equipmentId}/${e.equipmentNumber}`"
                      >{{ e.equipmentNumber }} / {{ e.equipmentName }}</option
                    ></select
                  ></div
                ></div
              >
              <h3 class="frontline-pqc-section-title">检验操作</h3>
              <div
                class="frontline-pqc-choice-actions"
                :class="{ 'is-number': item.resultType !== 'BOOLEAN' }"
              >
                <button
                  v-if="item.resultType === 'BOOLEAN'"
                  class="pass"
                  :class="{ active: resultSummary.startsWith(`合格 ${quantity} 件 · 不合格 0 件`) }"
                  type="button"
                  :disabled="!quantity"
                  @click="bulk('合格')"
                  >全部合格</button
                >
                <button
                  v-if="item.resultType === 'BOOLEAN'"
                  class="fail"
                  :class="{ active: resultSummary.startsWith(`合格 0 件 · 不合格 ${quantity} 件`) }"
                  type="button"
                  :disabled="!quantity"
                  @click="bulk('不合格')"
                  >全部不良</button
                >
                <button class="manual" type="button" :disabled="!quantity" @click="openPieces"
                  ><span>{{ item.resultType === 'BOOLEAN' ? '逐件选择' : '逐件填写' }}</span
                  ><em role="status" :aria-label="resultSummary"
                    >已完成 {{ completedCount }}/{{ quantity ?? 0 }}</em
                  ><strong aria-hidden="true">&gt;</strong></button
                >
              </div>
            </article>
            <div v-else class="frontline-pqc-empty-state">当前工序没有适用于此检验类型的项目</div>
          </div>
        </section>
        <section class="frontline-work-panel frontline-pqc-fill-panel">
          <h3 class="frontline-pqc-section-title">数量填写</h3>
          <div class="frontline-pqc-form-area">
            <div class="frontline-pqc-number-field">
              <label for="trialPqcInspectionQuantity">检验</label>
              <button type="button" aria-label="检验减少" @click="stepQuantity(-1)"> - </button>
              <input
                id="trialPqcInspectionQuantity"
                :value="quantity ?? ''"
                type="number"
                min="0"
                inputmode="numeric"
                data-trial-pqc-inspection-quantity
                aria-label="检验数量"
                @input="setQuantity($event)"
              />
              <button type="button" aria-label="检验增加" @click="stepQuantity(1)"> + </button>
              <span>件</span>
            </div>
            <div class="frontline-pqc-number-field">
              <label for="trialPqcScrapQuantity">损耗</label>
              <button
                type="button"
                aria-label="损耗减少"
                @click="scrapQuantity = Math.max(0, scrapQuantity - 1)"
              >
                -
              </button>
              <input
                id="trialPqcScrapQuantity"
                :value="scrapQuantity ?? ''"
                type="number"
                min="0"
                inputmode="numeric"
                data-trial-pqc-scrap-quantity
                @input="scrapQuantity = readQuantity($event) ?? 0"
              />
              <button
                type="button"
                aria-label="损耗增加"
                @click="scrapQuantity = scrapQuantity + 1"
              >
                +
              </button>
              <span>件</span>
            </div></div
          >
          <div
            class="frontline-inline-error-slot"
            :class="{ 'is-visible': !!trialMessage }"
            role="status"
            >{{ trialMessage }}</div
          >
        </section>
        <section class="frontline-work-panel frontline-pqc-rule-panel"
          ><div class="frontline-pqc-type-tabs"
            ><button
              v-for="rule in availableRules"
              :key="rule.key"
              type="button"
              :class="{ active: selectedRuleKey === rule.key }"
              :aria-pressed="selectedRuleKey === rule.key"
              @click="selectRule(rule.key)"
              >{{ rule.label }}</button
            ></div
          ></section
        >
        <footer class="frontline-pqc-submit-bar"
          ><button class="frontline-pqc-reset-button" type="button" @click="reset">重填</button
          ><button class="frontline-pqc-submit-button" type="button" @click="submitTrial"
            >提交</button
          ></footer
        >
      </main>
      <div
        v-if="factsVisible && item"
        class="frontline-pqc-fact-dialog"
        role="dialog"
        aria-modal="true"
        aria-label="方法和标准"
        @click.self="factsVisible = false"
        ><section class="frontline-pqc-fact-dialog__panel"
          ><header class="frontline-pqc-fact-dialog__header"
            ><div
              ><span class="frontline-pqc-fact-dialog__eyebrow">方法和标准</span
              ><h3>{{ item.itemName }}</h3></div
            ><button
              class="frontline-pqc-fact-dialog__close"
              aria-label="关闭方法和标准弹框"
              type="button"
              @click="factsVisible = false"
              >×</button
            ></header
          ><div class="frontline-pqc-fact-dialog__body is-combined"
            ><article
              v-for="fact in [
                { label: '接收标准', value: item.standardText },
                { label: '检验方法', value: item.inspectionMethod },
                { label: '抽样方案', value: item.samplingPlanText },
                { label: '检验器具及设备', value: item.inspectionTool }
              ]"
              :key="fact.label"
              class="frontline-pqc-fact-dialog__detail"
              ><span>{{ fact.label }}</span
              ><p>{{ fact.value }}</p></article
            ></div
          ><footer class="frontline-pqc-fact-dialog__footer"
            ><button type="button" @click="factsVisible = false">关闭</button></footer
          ></section
        ></div
      >
      <div
        v-if="pieceVisible && item && state && quantity"
        class="frontline-pqc-piece-modal"
        role="dialog"
        aria-modal="true"
        aria-label="逐件检验"
        @click.self="pieceVisible = false"
        ><section class="frontline-pqc-piece-dialog"
          ><h3>{{ item.itemName }}（{{ quantity }}件）</h3
          ><div class="frontline-pqc-piece-list"
            ><article v-for="n in visiblePieces" :key="n" class="frontline-pqc-piece-row"
              ><span>第 {{ n }} 件</span
              ><div v-if="item.resultType === 'NUMERIC'" class="frontline-pqc-piece-value-control"
                ><button type="button" :aria-label="`第${n}件减少`" @click="stepPiece(n - 1, -1)"
                  >-</button
                ><input
                  v-model.number="pieceValues[n - 1]"
                  type="number"
                  :step="10 ** -(item.standardPrecision ?? 2)"
                  :aria-label="`第${n}件测量值`"
                /><button type="button" :aria-label="`第${n}件增加`" @click="stepPiece(n - 1, 1)"
                  >+</button
                ><span>{{ item.standardUnit }}</span></div
              ><el-input
                v-else-if="item.resultType === 'TEXT'"
                v-model="pieceValues[n - 1]"
                :aria-label="`第${n}件文字记录`"
                type="textarea" /><div v-else class="frontline-pqc-piece-choice"
                ><el-switch
                  class="frontline-pqc-piece-switch"
                  inline-prompt
                  active-text="合格"
                  inactive-text="不合格"
                  :model-value="pieceValues[n - 1] === '合格'"
                  :aria-label="`第${n}件检验结果`"
                  @update:model-value="
                    pieceValues[n - 1] = $event ? '合格' : '不合格'
                  " /></div></article></div
          ><el-pagination
            v-model:current-page="piecePage"
            :page-size="20"
            :total="quantity"
            layout="prev, pager, next"
          /><footer class="frontline-pqc-piece-actions"
            ><button type="button" @click="pieceVisible = false">返回</button
            ><button class="primary" type="button" @click="confirmPieces">确认</button></footer
          ></section
        ></div
      >
    </div>
    <el-dialog v-model="processVisible" title="选择工序" width="600px" append-to-body
      ><div class="frontline-picker__options"
        ><button
          v-for="(p, index) in draft.processes"
          :key="p.processCode"
          type="button"
          :class="{ active: index === processIndex }"
          @click="
            emit('select-process', index)
            processVisible = false
          "
          >{{ index + 1 }}. {{ p.processName }}</button
        ></div
      ></el-dialog
    >
    <el-dialog v-model="batchVisible" title="试填批量" width="420px" append-to-body
      ><el-input-number v-model="batchQuantity" aria-label="试填批量" :min="1" :precision="0"
    /></el-dialog>
  </section>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useUserStore } from '@/store/modules/user'
import type { Draft } from '../simplePqc'
const props = defineProps<{ draft: Draft; processIndex: number }>()
const emit = defineEmits<{ 'select-process': [index: number] }>()
type InspectionType = 'FIRST' | 'PATROL' | 'FINAL'

const inspectionType = ref<InspectionType>('FIRST')
const selectedRuleKey = ref('FIRST')
const userStore = useUserStore()
const maximized = ref(false)
const processVisible = ref(false)
const batchVisible = ref(false)
const scrapQuantity = ref(0)
const trialMessage = ref('')
const quantityOverrides = reactive<Record<string, number | undefined>>({})
const pieceValues = ref<Array<string | number | undefined>>([])
const batchQuantity = ref(100)
const selectedCode = ref('')
const pieceVisible = ref(false)
const factsVisible = ref(false)
const piecePage = ref(1)
// Trial values are local to this component and never enter the publish draft.
const results = reactive<
  Record<string, { equipment: string; values: Array<string | number | undefined> }>
>({})
const process = computed(() => props.draft.processes[props.processIndex])
const availableTypes = computed(() =>
  (['FIRST', 'PATROL', 'FINAL'] as const).filter(
    (type) =>
      (type !== 'FINAL' || props.draft.finalInspectionApplicable) &&
      process.value?.items.some((i) => i.applicableInspectionTypes.includes(type))
  )
)
const items = computed(
  () =>
    process.value?.items.filter((i) =>
      i.applicableInspectionTypes.includes(inspectionType.value)
    ) ?? []
)
const item = computed(
  () => items.value.find((i) => i.itemCode === selectedCode.value) ?? items.value[0]
)
const quantity = computed(() => {
  const override = quantityOverrides[`${process.value?.processCode}/${selectedRuleKey.value}`]
  if (override != null) return override > 0 ? override : undefined
  if (!item.value) return undefined
  let count: number | undefined
  if (inspectionType.value === 'FIRST') count = item.value.firstInspectionQuantity
  else if (inspectionType.value === 'PATROL' && item.value.patrolInspectionRatio != null)
    count = Math.ceil((batchQuantity.value * item.value.patrolInspectionRatio) / 100)
  else if (inspectionType.value === 'FINAL')
    count = props.draft.inspectionTypeRules.find((r) => r.key === 'FINAL')?.fixedQuantity
  return count != null && count > 0 ? Math.min(batchQuantity.value, count) : undefined
})
const availableRules = computed(() =>
  props.draft.inspectionTypeRules.filter((r) =>
    availableTypes.value.includes(r.inspectionType as InspectionType)
  )
)
const selectedEquipmentLabel = computed(() => {
  const option = item.value?.equipmentOptions.find(
    (e) => `${e.equipmentId}/${e.equipmentNumber}` === state.value?.equipment
  )
  return option
    ? `${option.equipmentName} / ${option.equipmentNumber}`
    : item.value?.equipmentOptions.length
      ? '选择检验设备'
      : '无设备'
})
const key = computed(
  () =>
    `${process.value?.processCode}/${item.value?.itemCode}/${inspectionType.value}/${selectedRuleKey.value}`
)
const state = computed(() => results[key.value])
watch(
  availableTypes,
  (types) => {
    if (!types.includes(inspectionType.value) && types.length) inspectionType.value = types[0]
    if (!availableRules.value.some((r) => r.key === selectedRuleKey.value))
      selectedRuleKey.value = availableRules.value[0]?.key ?? ''
  },
  { immediate: true }
)
watch(
  [key, quantity],
  () => {
    pieceVisible.value = false
    trialMessage.value = ''
    piecePage.value = 1
    if (!item.value) return
    results[key.value] ??= { equipment: '', values: [] }
    // Changing the sample quantity invalidates only the trial results for this item/type.
    if (results[key.value].values.length !== (quantity.value ?? 0))
      results[key.value].values = Array(quantity.value ?? 0).fill(undefined)
  },
  { immediate: true }
)
const visiblePieces = computed(() =>
  Array.from(
    { length: Math.min(20, Math.max(0, (quantity.value ?? 0) - (piecePage.value - 1) * 20)) },
    (_, i) => (piecePage.value - 1) * 20 + i + 1
  )
)
const completedCount = computed(
  () => state.value?.values.filter((v) => v != null && String(v).trim()).length ?? 0
)
const resultSummary = computed(() => {
  if (!quantity.value || !state.value || !item.value) return '请先在编辑内容中配置检验数量'
  let pass = 0,
    fail = 0,
    pending = 0
  for (let n = 0; n < quantity.value; n++) {
    const value = state.value.values[n]
    if (value == null || (typeof value === 'string' && !value.trim())) pending++
    else if (item.value.resultType === 'NUMERIC') {
      if (item.value.standardLowerLimit == null || item.value.standardUpperLimit == null)
        return '请先在编辑内容中配置数值上下限'
      if (
        Number(value) >= item.value.standardLowerLimit &&
        Number(value) <= item.value.standardUpperLimit
      )
        pass++
      else fail++
    } else if (value === '合格') pass++
    else fail++
  }
  return item.value.resultType === 'TEXT'
    ? `已填写 ${quantity.value - pending} 件 · 未填写 ${pending} 件`
    : `合格 ${pass} 件 · 不合格 ${fail} 件 · 未填写 ${pending} 件`
})
function bulk(value: string) {
  if (state.value && quantity.value) state.value.values = Array(quantity.value).fill(value)
}
function reset() {
  if (state.value) {
    state.value.values = Array(quantity.value ?? 0).fill(undefined)
    state.value.equipment = ''
  }
}

function selectRule(key: string) {
  const rule = availableRules.value.find((r) => r.key === key)
  if (!rule) return
  selectedRuleKey.value = key
  inspectionType.value = rule.inspectionType as InspectionType
}
function readQuantity(event: Event) {
  const value = (event.target as HTMLInputElement).value
  return value === '' ? undefined : Math.max(0, Math.trunc(Number(value)))
}
function setQuantity(event: Event) {
  quantityOverrides[`${process.value?.processCode}/${selectedRuleKey.value}`] =
    readQuantity(event) ?? 0
}
function stepQuantity(delta: number) {
  quantityOverrides[`${process.value?.processCode}/${selectedRuleKey.value}`] = Math.max(
    0,
    (quantity.value ?? 0) + delta
  )
}
function openPieces() {
  pieceValues.value = [...(state.value?.values ?? [])]
  pieceVisible.value = true
}
function confirmPieces() {
  if (state.value) state.value.values = [...pieceValues.value]
  pieceVisible.value = false
}
function stepPiece(index: number, delta: number) {
  const precision = item.value?.standardPrecision ?? 2
  pieceValues.value[index] = Number(
    ((Number(pieceValues.value[index]) || 0) + delta * 10 ** -precision).toFixed(precision)
  )
}
function submitTrial() {
  trialMessage.value =
    !quantity.value || completedCount.value !== quantity.value
      ? '请完成当前检验项目后再试填提交'
      : '试填提交完成，仅供预览，不会生成检验记录'
}
</script>
<style scoped lang="scss">
@use '@/views/mes/pro/feedback/frontlineFixedTemplate.scss';
.trial-note {
  margin: 12px 0;
  color: var(--el-text-color-secondary);
}
.pqc-trial.is-pqc-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 2000;
}
</style>
