<template>
  <div class="trial-preview">
    <p class="trial-note">试填内容仅用于预览，不会修改模板或生成生产记录。</p>
    <div class="form-parser-frontline-shell">
      <div class="form-parser-frontline-header">
        <div class="form-parser-process-switcher">
          <button
            type="button"
            class="form-parser-nav-button"
            aria-label="上一道工序"
            :disabled="processIndex === 0"
            @click="emit('select-process', processIndex - 1)"
            ><Icon icon="ep:arrow-left-bold"
          /></button>
          <button type="button" class="form-parser-process-card" @click="selectorVisible = true"
            ><span>工序</span
            ><strong>{{ processIndex + 1 }}. {{ process.name || '未命名工序' }}</strong></button
          >
          <button
            type="button"
            class="form-parser-nav-button"
            aria-label="下一道工序"
            :disabled="processIndex === processes.length - 1"
            @click="emit('select-process', processIndex + 1)"
            ><Icon icon="ep:arrow-right-bold"
          /></button>
        </div>
      </div>
      <div class="form-parser-frontline-body">
        <div class="form-parser-production-panel">
          <div class="form-parser-quantity-row"
            ><span>完成数量</span
            ><el-input-number
              v-model="quantity"
              aria-label="完成数量"
              :min="0"
              :precision="0"
              controls-position="right"
            /><b>件</b></div
          >
          <div class="form-parser-quantity-row form-parser-quantity-row--readonly"
            ><span>损耗数量</span><strong>0</strong><b>件</b></div
          >
          <div
            v-for="direction in directions"
            :key="direction.key"
            class="form-parser-material-block"
          >
            <div class="form-parser-block-title">{{ direction.label }}</div>
            <div v-if="process[direction.key]?.length" class="form-parser-material-list"
              ><span
                v-for="(item, i) in process[direction.key]"
                :key="i"
                class="form-parser-material-chip"
                :class="{ 'form-parser-material-chip--output': direction.key === 'outputs' }"
                >{{ nameCode(item) }}</span
              ></div
            >
            <div v-else class="form-parser-empty-text">未识别{{ direction.label }}</div>
          </div>
        </div>
        <div class="form-parser-equipment-panel">
          <div class="form-parser-confirm-strip"
            ><button
              v-for="(label, i) in ['清场', '物料', '清洁']"
              :key="label"
              type="button"
              class="form-parser-confirm-card"
              :class="{ 'form-parser-confirm-card--active': checks[i] }"
              :aria-pressed="checks[i]"
              @click="checks[i] = !checks[i]"
              ><Icon icon="ep:select" />{{ label }}</button
            ></div
          >
          <div v-if="devices.length" class="form-parser-device-list">
            <div class="form-parser-device-tabs" aria-label="设备选择">
              <div
                v-for="device in devices"
                :key="device.key"
                class="form-parser-device-tab-card"
                :class="{ active: selected.includes(device.key) }"
              >
                <button
                  type="button"
                  class="form-parser-device-tab"
                  role="checkbox"
                  :aria-checked="selected.includes(device.key)"
                  :title="device.label"
                  @click="toggleDevice(device)"
                  ><span class="form-parser-device-tab-selection" aria-hidden="true">{{
                    selected.includes(device.key) ? '✓' : ''
                  }}</span
                  ><span class="form-parser-device-tab-code">{{ device.label }}</span></button
                >
                <div class="form-parser-device-metering-validity"
                  ><span aria-hidden="true">✓</span><em>计量状态：未记录</em></div
                >
              </div>
            </div>
            <div v-if="selected.length > 1" class="trial-selected-devices"
              ><el-button
                v-for="device in devices.filter((item) => selected.includes(item.key))"
                :key="device.key"
                :type="activeKey === device.key ? 'primary' : 'default'"
                @click="activeKey = device.key"
                >{{ device.label }}</el-button
              ></div
            >
            <div v-if="!activeDevice" class="form-parser-empty-text form-parser-empty-text--panel"
              >请选择设备</div
            >
          </div>
          <div
            v-else-if="!parameterOnlyGroups.length"
            class="form-parser-empty-text form-parser-empty-text--panel"
            >未识别设备</div
          >
          <div v-for="card in parameterCards" :key="card.key" class="form-parser-device-current">
            <div v-if="card.parameters?.length" class="form-parser-parameter-list">
              <div
                v-for="(parameter, p) in card.parameters"
                :key="p"
                class="form-parser-parameter-field"
              >
                <div class="form-parser-parameter-field__label"
                  ><span>{{ parameter.ui?.displayName || parameter.name }}</span
                  ><em v-if="targetRange(parameter)">目标范围：{{ targetRange(parameter) }}</em
                  ><em>默认值：{{ parameter.ui?.defaultValue ?? '未配置' }}</em></div
                >
                <div class="form-parser-parameter-field__control">
                  <el-alert
                    v-if="checkParameter(parameter).length"
                    :title="checkParameter(parameter).join('；')"
                    type="warning"
                    :closable="false"
                  />
                  <el-input-number
                    v-else-if="parameter.ui?.control === 'number'"
                    v-model="values[`${card.key}:${p}`]"
                    :aria-label="parameter.ui?.displayName || parameter.name"
                    :min="number(parameter.ui.min)"
                    :max="number(parameter.ui.max)"
                    :step="number(parameter.ui.step)"
                  />
                  <el-select
                    v-else-if="parameter.ui?.control === 'select'"
                    v-model="values[`${card.key}:${p}`]"
                    :aria-label="parameter.ui?.displayName || parameter.name"
                    clearable
                    ><el-option
                      v-for="(option, o) in parameter.ui.options"
                      :key="o"
                      :label="String(option)"
                      :value="optionValue(option)"
                  /></el-select>
                  <el-input
                    v-else-if="['text', 'input'].includes(parameter.ui?.control ?? '')"
                    v-model="values[`${card.key}:${p}`]"
                    :aria-label="parameter.ui?.displayName || parameter.name"
                  />
                  <el-alert v-else title="填写类型待配置" type="warning" :closable="false" />
                  <b v-if="parameter.ui?.unit">{{ parameter.ui.unit }}</b>
                </div>
              </div>
            </div>
            <div v-else class="form-parser-empty-text">未识别设备参数</div>
          </div>
        </div>
      </div>
      <div class="form-parser-frontline-footer"
        ><button type="button" class="form-parser-footer-button" @click="reset">重填</button
        ><button
          type="button"
          class="form-parser-footer-button form-parser-footer-button--primary"
          disabled
          title="试填预览不提交生产记录"
          >正式提交</button
        ></div
      >
    </div>
    <el-dialog v-model="selectorVisible" title="选择工序" width="560px" append-to-body
      ><div class="trial-process-options"
        ><el-button
          v-for="(item, index) in processes"
          :key="index"
          :type="index === processIndex ? 'primary' : 'default'"
          @click="chooseProcess(index)"
          >{{ index + 1 }}. {{ item.name || '未命名工序' }}</el-button
        ></div
      ></el-dialog
    >
  </div>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { checkParameter } from '../simpleRecognition'
import type {
  BatchRecordTotalRecognitionProcess as Process,
  BatchRecordTotalRecognitionParameter as Parameter
} from '../productionRecognition'
const props = defineProps<{ process: Process; processIndex: number; processes: Process[] }>()
const emit = defineEmits<{ 'select-process': [index: number] }>()
const quantity = ref<number>()
const values = reactive<Record<string, any>>({})
const selected = ref<string[]>([])
const activeKey = ref('')
const checks = ref([false, false, false])
const selectorVisible = ref(false)
function chooseProcess(index: number) {
  selectorVisible.value = false
  emit('select-process', index)
}
const directions = [
  { key: 'inputs', label: '输入物料' },
  { key: 'outputs', label: '输出物料' }
] as const
const number = (v: unknown): number | undefined => (v == null || v === '' ? undefined : Number(v))
const optionValue = (value: unknown) => value as string | number
const nameCode = (item: { name?: string; code?: string | null }) =>
  `${item.name || '未命名'}${item.code ? `(${item.code})` : ''}`
function targetRange(parameter: Parameter) {
  const min = number(parameter.ui?.min)
  const max = number(parameter.ui?.max)
  if (min !== undefined && max !== undefined)
    return `${min} - ${max}${parameter.ui?.unit ? ` ${parameter.ui.unit}` : ''}`
  return /[-~～至±]/.test(parameter.referenceValue || '') ? parameter.referenceValue : ''
}
const devices = computed(() =>
  (props.process.equipmentGroups || []).flatMap((group, g) =>
    (group.equipmentOptions || []).map((equipment, e) => ({
      key: `${g}:${e}`,
      groupIndex: g,
      label: nameCode(equipment),
      parameters: group.parameters
    }))
  )
)
type Device = (typeof devices.value)[number]
const parameterOnlyGroups = computed(() =>
  (props.process.equipmentGroups || []).flatMap((group, g) =>
    !group.equipmentOptions?.length && group.parameters?.length
      ? [{ key: `${g}:-1`, parameters: group.parameters }]
      : []
  )
)
const activeDevice = computed(() =>
  devices.value.find((item) => item.key === activeKey.value && selected.value.includes(item.key))
)
const parameterCards = computed(() => [
  ...(activeDevice.value ? [activeDevice.value] : []),
  ...parameterOnlyGroups.value
])
function toggleDevice(device: Device) {
  if (selected.value.includes(device.key)) {
    selected.value = selected.value.filter((key) => key !== device.key)
    if (activeKey.value === device.key) activeKey.value = selected.value[0] || ''
    return
  }
  if (props.process.equipmentGroups?.[device.groupIndex].selectionMode === 'SINGLE') {
    const groupKeys = devices.value
      .filter((item) => item.groupIndex === device.groupIndex)
      .map((item) => item.key)
    selected.value = selected.value.filter((key) => !groupKeys.includes(key))
  }
  selected.value.push(device.key)
  activeKey.value = device.key
}
function reset() {
  quantity.value = undefined
  checks.value = [false, false, false]
  selected.value = devices.value.length ? [devices.value[0].key] : []
  activeKey.value = selected.value[0] || ''
  Object.keys(values).forEach((key) => delete values[key])
  for (const card of [...devices.value, ...parameterOnlyGroups.value]) {
    card.parameters?.forEach((parameter, p) => {
      values[`${card.key}:${p}`] =
        parameter.ui?.control === 'number'
          ? number(parameter.ui.defaultValue)
          : parameter.ui?.defaultValue
    })
  }
}
watch(() => props.process, reset, { immediate: true })
</script>
<style scoped lang="scss">
@use './frontlinePreview.scss';
.trial-preview {
  container-type: inline-size;
}
.trial-note {
  margin: 0 0 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.form-parser-device-tabs {
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}
.form-parser-device-current {
  margin-top: 12px;
}
.form-parser-quantity-row {
  grid-template-columns: 130px minmax(0, 1fr) 24px;
}
.form-parser-parameter-field {
  grid-template-columns: minmax(100px, 0.45fr) minmax(0, 0.55fr);
}
.form-parser-material-chip {
  cursor: default;
}
.form-parser-footer-button:not(:disabled) {
  cursor: pointer;
}
.trial-selected-devices,
.trial-process-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  .el-button {
    margin: 0;
  }
}
@container (max-width: 760px) {
  .form-parser-frontline-body {
    grid-template-columns: 1fr;
  }
  .form-parser-production-panel,
  .form-parser-equipment-panel {
    min-height: auto;
  }
}
@container (max-width: 420px) {
  .form-parser-process-switcher {
    grid-template-columns: 44px minmax(0, 1fr) 44px;
  }
  .form-parser-process-card strong {
    font-size: 20px;
  }
  .form-parser-quantity-row {
    grid-template-columns: 90px minmax(0, 1fr) 24px;
    span {
      font-size: 16px;
    }
  }
  .form-parser-parameter-field,
  .form-parser-frontline-footer {
    grid-template-columns: 1fr;
  }
  .form-parser-parameter-list {
    padding: 0;
  }
}
</style>
