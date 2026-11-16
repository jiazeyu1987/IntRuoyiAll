<template>
  <div class="numeric-entry" data-pqc-numeric-entry>
    <div class="entry-toolbar">
      <div role="group" aria-label="数值录入方式" class="entry-modes">
        <button
          v-for="option in modes"
          :key="option.key"
          type="button"
          :aria-pressed="mode === option.key"
          @click="mode = option.key"
          >{{ option.label }}</button
        >
      </div>
      <span role="status"
        >已填 {{ completed }} / {{ quantity }}，未填 {{ quantity - completed }}，超限
        {{ outOfRange }}，无效 {{ invalidCount }}</span
      >
      <button type="button" @click="locateMissing">定位未填</button>
    </div>
    <p class="entry-standard"
      >{{ standardText || '请按检验标准填写实际测量值' }} {{ unit ? `（${unit}）` : '' }}</p
    >
    <p v-if="notice" role="status">{{ notice }}</p>

    <div v-if="mode === 'continuous'" class="entry-continuous">
      <label :for="`${id}-current`">第 {{ current + 1 }} / {{ quantity }} 件</label>
      <div class="entry-current-control">
        <button type="button" :disabled="current === 0" aria-label="上一件" @click="move(-1)"
          >上一件</button
        >
        <input
          :id="`${id}-current`"
          ref="currentInput"
          type="text"
          inputmode="decimal"
          aria-label="当前件测量值"
          :value="valueAt(current)"
          :aria-invalid="status(current) === '数字无效'"
          @input="update(current, $event)"
          @keydown.enter.prevent="advance"
        />
        <span>{{ unit }}</span>
        <button
          type="button"
          :disabled="current === quantity - 1"
          aria-label="下一件"
          @click="advance"
          >下一件</button
        >
      </div>
      <p :class="{ 'entry-error': status(current) !== '已填' }"
        >{{ status(current) }}；输入后按 Enter 进入下一件，可点“上一件”修改。</p
      >
      <div class="entry-recent" aria-label="最近录入的件号">
        <button
          v-for="index in recentIndexes"
          :key="index"
          type="button"
          @click="selectPiece(index)"
        >
          第 {{ index + 1 }} 件：{{ valueAt(index) || '未填' }} {{ unit }}
        </button>
      </div>
      <p>数据暂存在本次填写中，点击“完成”保留；签名提交后才形成正式检验记录。</p>
    </div>

    <div v-else-if="mode === 'table'" class="entry-table-scroll">
      <table>
        <thead
          ><tr
            ><th>件号</th><th>测量值 {{ unit ? `（${unit}）` : '' }}</th
            ><th>状态</th></tr
          ></thead
        >
        <tbody
          ><tr
            v-for="piece in quantity"
            :key="piece"
            :class="{ 'entry-warning': ['超限', '数字无效'].includes(status(piece - 1)) }"
          >
            <td>{{ piece }}</td>
            <td
              ><input
                :id="`${id}-piece-${piece}`"
                type="text"
                inputmode="decimal"
                data-pqc-piece-number-input
                :aria-label="`第 ${piece} 件测量值`"
                :value="valueAt(piece - 1)"
                :aria-invalid="status(piece - 1) === '数字无效'"
                @input="update(piece - 1, $event)"
                @keydown.enter.prevent="focusTable(Math.min(piece + 1, quantity))"
            /></td>
            <td>{{ status(piece - 1) }}</td>
          </tr></tbody
        >
      </table>
    </div>

    <div v-else class="entry-paste">
      <label :for="`${id}-paste`"
        >从 Excel 复制一列，每行一个测量值，依次对应第 1～{{ quantity }} 件</label
      >
      <textarea
        :id="`${id}-paste`"
        v-model="pasteText"
        aria-label="粘贴整列测量值"
        placeholder="在此粘贴测量值，每行一个"
      ></textarea>
      <p role="status">预览 {{ preview.values.length }} / {{ quantity }} 行</p>
      <div v-if="pasteText && preview.errors.length" class="entry-error" role="alert">
        <p v-for="error in preview.errors" :key="error">{{ error }}</p>
      </div>
      <div v-if="pasteText" class="entry-preview"
        ><table>
          <thead
            ><tr><th>件号</th><th>测量值</th><th>状态</th></tr></thead
          >
          <tbody
            ><tr v-for="(value, index) in preview.values" :key="index"
              ><td>{{ index + 1 }}</td
              ><td>{{ value || '空行' }}</td
              ><td>{{ valueStatus(value) }}</td></tr
            ></tbody
          >
        </table></div
      >
      <label v-if="hasExisting" class="entry-overwrite"
        ><input v-model="confirmOverwrite" type="checkbox" />确认覆盖本项目已有测量值</label
      >
      <button
        type="button"
        class="entry-primary"
        :disabled="!pasteText || preview.errors.length > 0 || (hasExisting && !confirmOverwrite)"
        @click="applyPaste"
        >确认写入 {{ quantity }} 件</button
      >
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { isPqcNumericValue, parsePqcNumericColumn } from './pqcNumericEntry'

const props = defineProps<{
  modelValue: string[]
  quantity: number
  unit: string
  standardText: string
  lowerLimit?: number | string
  upperLimit?: number | string
  precision?: number
}>()
const emit = defineEmits<{ (event: 'update:modelValue', values: string[]): void }>()
const id = `pqc-numeric-${Math.random().toString(36).slice(2)}`
const modes = [
  { key: 'continuous', label: '连续录入' },
  { key: 'paste', label: '整列粘贴' },
  { key: 'table', label: '表格核对' }
] as const
const mode = ref<'continuous' | 'paste' | 'table'>('continuous')
const current = ref(0)
const currentInput = ref<HTMLInputElement>()
const pasteText = ref('')
const confirmOverwrite = ref(false)
const notice = ref('')
const valueAt = (index: number) => props.modelValue[index] ?? ''
const valueStatus = (value: string) => {
  if (!value.trim()) return '未填'
  if (!isPqcNumericValue(value, props.precision)) return '数字无效'
  const number = Number(value)
  if (
    (props.lowerLimit != null && number < Number(props.lowerLimit)) ||
    (props.upperLimit != null && number > Number(props.upperLimit))
  )
    return '超限'
  return '已填'
}
const status = (index: number) => valueStatus(valueAt(index))
const completed = computed(
  () =>
    Array.from({ length: props.quantity }, (_, i) => status(i)).filter(
      (s) => s === '已填' || s === '超限'
    ).length
)
const outOfRange = computed(
  () =>
    Array.from({ length: props.quantity }, (_, i) => status(i)).filter((s) => s === '超限').length
)
const invalidCount = computed(
  () =>
    Array.from({ length: props.quantity }, (_, i) => status(i)).filter((s) => s === '数字无效')
      .length
)
const hasExisting = computed(() =>
  props.modelValue.slice(0, props.quantity).some((value) => value.trim())
)
const preview = computed(() => parsePqcNumericColumn(pasteText.value, props.quantity, props.precision))
const recentIndexes = computed(() =>
  Array.from({ length: Math.min(current.value, 5) }, (_, i) => current.value - i - 1)
)
const focusCurrent = async () => {
  await nextTick()
  currentInput.value?.focus()
  currentInput.value?.select()
}
const selectPiece = (index: number) => {
  current.value = index
  notice.value = ''
  void focusCurrent()
}
const move = (delta: number) =>
  selectPiece(Math.max(0, Math.min(props.quantity - 1, current.value + delta)))
const update = (index: number, event: Event) => {
  const values = Array.from({ length: props.quantity }, (_, i) => valueAt(i))
  values[index] = (event.target as HTMLInputElement).value
  notice.value = ''
  emit('update:modelValue', values)
}
const advance = () => {
  if (!isPqcNumericValue(valueAt(current.value), props.precision)) {
    notice.value = props.precision === undefined
      ? '请先填写有效的普通小数。'
      : `请填写有效的普通小数，小数最多 ${props.precision} 位。`
    return
  }
  if (current.value === props.quantity - 1) {
    notice.value = '已到最后一件，请核对后点击“完成”。'
    return
  }
  move(1)
}
const focusTable = async (piece: number) => {
  await nextTick()
  const input = document.getElementById(`${id}-piece-${piece}`) as HTMLInputElement | null
  input?.focus()
  input?.select()
}
const locateMissing = () => {
  const index = Array.from({ length: props.quantity }, (_, i) => i).find(
    (i) => !isPqcNumericValue(valueAt(i), props.precision)
  )
  if (index === undefined) {
    notice.value = '全部测量值已填写。'
    return
  }
  mode.value = 'continuous'
  selectPiece(index)
}
const applyPaste = () => {
  if (
    !pasteText.value ||
    preview.value.errors.length ||
    (hasExisting.value && !confirmOverwrite.value)
  )
    return
  emit('update:modelValue', preview.value.values.slice())
  mode.value = 'table'
  pasteText.value = ''
}
watch(pasteText, () => {
  confirmOverwrite.value = false
})
watch(
  () => props.modelValue,
  () => {
    confirmOverwrite.value = false
  }
)
watch(mode, (value) => {
  if (value === 'continuous') void focusCurrent()
})
onMounted(locateMissing)
</script>

<style scoped>
.numeric-entry {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  gap: 12px;
  font-size: 18px;
  color: #20352b;
}
.entry-toolbar,
.entry-modes,
.entry-current-control {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.entry-toolbar {
  justify-content: space-between;
}
button,
input,
textarea {
  font: inherit;
  box-sizing: border-box;
  border: 1px solid #a9bcb1;
  border-radius: 8px;
  background: white;
  color: inherit;
}
button {
  padding: 10px 16px;
  cursor: pointer;
  min-height: 44px;
}
button[aria-pressed='true'],
.entry-primary {
  color: white;
  background: #22382d;
}
button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
input {
  padding: 10px;
  min-width: 0;
}
input:focus,
textarea:focus,
button:focus-visible {
  outline: 3px solid #23836a;
  outline-offset: 2px;
}
.entry-standard {
  margin: 0;
}
.entry-continuous {
  overflow: auto;
  padding: 8px 4px;
}
.entry-continuous > label {
  display: block;
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 16px;
}
.entry-current-control input {
  width: min(320px, 100%);
  font-size: 36px;
}
.entry-recent {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.entry-table-scroll,
.entry-paste {
  min-height: 0;
  overflow: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
th,
td {
  border-bottom: 1px solid #d8e1db;
  padding: 8px 14px;
}
th {
  position: sticky;
  top: 0;
  background: #edf3ef;
  z-index: 1;
}
td input {
  width: 100%;
}
.entry-warning {
  background: #fff0e5;
}
.entry-error {
  color: #b42318;
}
.entry-paste {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
textarea {
  width: 100%;
  min-height: 110px;
  padding: 12px;
  flex-shrink: 0;
}
.entry-paste p {
  margin: 0;
}
.entry-preview {
  max-height: 220px;
  overflow: auto;
  flex-shrink: 0;
}
.entry-overwrite {
  display: flex;
  align-items: center;
  gap: 8px;
}
.entry-overwrite input {
  width: 22px;
  height: 22px;
}
@media (max-width: 700px) {
  .numeric-entry {
    font-size: 16px;
  }
  th,
  td {
    padding: 6px;
  }
  .entry-current-control input {
    width: 100%;
  }
}
</style>
