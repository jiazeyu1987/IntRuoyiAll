<template>
  <div class="data-node">
    <details v-if="selectableTypes.length > 1" class="data-node__type-settings">
      <summary>内容类型：{{ types.find((type) => type.value === currentType)?.label }}</summary>
      <el-select :model-value="currentType" aria-label="内容类型" @update:model-value="changeType">
        <el-option
          v-for="type in selectableTypes"
          :key="type.value"
          :label="type.label"
          :value="type.value"
        />
      </el-select>
      <p class="data-node__type-hint">切换内容类型会清空当前内容，请确认后选择。</p>
    </details>
    <template v-if="isContainer">
      <div v-for="(child, key) in modelValue" :key="key" class="data-node__row">
        <div class="data-node__label">{{
          Array.isArray(modelValue) ? `第 ${Number(key) + 1} 项` : businessFieldLabel(String(key))
        }}</div>
        <div class="data-node__value">
          <BusinessJsonNode
            ref="childNodes"
            :label="
              Array.isArray(modelValue)
                ? `第 ${Number(key) + 1} 项`
                : businessFieldLabel(String(key))
            "
            :model-value="child"
            @update:model-value="replace(String(key), $event)"
          />
        </div>
        <el-button
          link
          type="danger"
          :aria-label="`删除${Array.isArray(modelValue) ? `第 ${Number(key) + 1} 项` : businessFieldLabel(String(key))}`"
          @click="remove(String(key))"
          >删除</el-button
        >
      </div>
      <div class="data-node__add">
        <el-select
          v-if="!Array.isArray(modelValue)"
          v-model="newKey"
          filterable
          allow-create
          default-first-option
          placeholder="选择新增字段"
          aria-label="新增字段"
        >
          <el-option
            v-for="field in businessFieldOptions"
            :key="field.value"
            :value="field.value"
            :label="field.label"
          />
        </el-select>
        <el-select v-model="newType" aria-label="新增内容类型">
          <el-option
            v-for="type in types"
            :key="type.value"
            :label="type.label"
            :value="type.value"
          />
        </el-select>
        <el-button @click="add">{{
          Array.isArray(modelValue) ? '添加条目' : '添加字段'
        }}</el-button>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
    </template>
    <el-switch
      v-else-if="typeof modelValue === 'boolean'"
      :model-value="modelValue"
      active-text="是"
      inactive-text="否"
      @update:model-value="emit('update:modelValue', $event)"
    />
    <el-input
      v-else-if="typeof modelValue === 'number'"
      v-model="numberText"
      inputmode="decimal"
      :aria-label="label || '数值'"
      @update:model-value="setNumber"
      @change="setNumber"
    />
    <span v-else-if="modelValue === null">未记录</span>
    <el-input
      v-else
      :model-value="String(modelValue)"
      type="textarea"
      :autosize="{ minRows: 1, maxRows: 5 }"
      :aria-label="label || '内容'"
      @update:model-value="emit('update:modelValue', $event)"
    />
    <p v-if="!isContainer && error" role="alert">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  businessFieldLabel,
  businessFieldOptions
} from '@/views/mes/pro/processpool/components/businessEvidenceDisplay'

defineOptions({ name: 'BusinessJsonNode' })
const props = defineProps<{ modelValue: any; label?: string; allowedTypes?: string[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: any] }>()
const childNodes = ref<Array<{ validate: () => string }>>([])
const newKey = ref('')
const newType = ref('text')
const error = ref('')
const numberText = ref(String(props.modelValue))
watch(
  () => props.modelValue,
  (value) => {
    numberText.value = String(value)
    error.value = ''
  }
)
const types = [
  { label: '未记录', value: 'null' },
  { label: '文本', value: 'text' },
  { label: '数值', value: 'number' },
  { label: '是或否', value: 'boolean' },
  { label: '明细分组', value: 'object' },
  { label: '条目列表', value: 'array' }
]
const isContainer = computed(
  () => props.modelValue !== null && typeof props.modelValue === 'object'
)
const selectableTypes = computed(() =>
  types.filter(
    (type) =>
      !props.allowedTypes ||
      props.allowedTypes.includes(type.value === 'text' ? 'string' : type.value)
  )
)
const currentType = computed(() =>
  props.modelValue === null
    ? 'null'
    : Array.isArray(props.modelValue)
      ? 'array'
      : typeof props.modelValue === 'string'
        ? 'text'
        : typeof props.modelValue
)
const initialValue = (type: string) =>
  type === 'null'
    ? null
    : type === 'object'
      ? {}
      : type === 'array'
        ? []
        : type === 'number'
          ? 0
          : type === 'boolean'
            ? false
            : ''
const changeType = (type: string) => {
  if (!selectableTypes.value.some((option) => option.value === type))
    throw new Error('当前资料不允许此内容类型。')
  error.value = ''
  emit('update:modelValue', initialValue(type))
}
const replace = (key: string, value: any) => {
  const next: any = Array.isArray(props.modelValue)
    ? [...props.modelValue]
    : { ...props.modelValue }
  next[key] = value
  emit('update:modelValue', next)
}
const remove = (key: string) => {
  if (Array.isArray(props.modelValue))
    emit(
      'update:modelValue',
      props.modelValue.filter((_: unknown, i: number) => i !== Number(key))
    )
  else {
    const next = { ...props.modelValue }
    delete next[key]
    emit('update:modelValue', next)
  }
}
const add = () => {
  error.value = ''
  const key = newKey.value.trim()
  if (
    !Array.isArray(props.modelValue) &&
    (!key || Object.prototype.hasOwnProperty.call(props.modelValue, key))
  ) {
    error.value = '请输入未使用的字段标识。'
    return
  }
  const value = initialValue(newType.value)
  if (Array.isArray(props.modelValue)) emit('update:modelValue', [...props.modelValue, value])
  else emit('update:modelValue', { ...props.modelValue, [key]: value })
  newKey.value = ''
}
const setNumber = (text: string) => {
  const value = Number(text)
  if (
    !text.trim() ||
    !Number.isFinite(value) ||
    (Number.isInteger(value) && !Number.isSafeInteger(value))
  ) {
    error.value = '请输入有效数值；超长编号请使用文本类型。'
    return
  }
  error.value = ''
  emit('update:modelValue', value)
}
const validate = (): string =>
  error.value || childNodes.value.map((child) => child.validate()).find(Boolean) || ''
defineExpose({ validate })
</script>

<style scoped>
.data-node {
  width: 100%;
  min-width: 0;
}
/* Keep nested groups full width: horizontal label columns compound at each depth. */
.data-node__type-settings .el-select {
  width: 140px;
  max-width: 100%;
}
.data-node__type-settings {
  margin-bottom: 8px;
}
.data-node__type-settings summary {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.data-node__type-hint {
  margin: 4px 0 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  overflow-wrap: anywhere;
}
.data-node__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px 8px;
  align-items: start;
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.data-node__label {
  grid-column: 1;
  grid-row: 1;
  min-width: 0;
  font-weight: 500;
  overflow-wrap: anywhere;
}
.data-node__row > .el-button {
  grid-column: 2;
  grid-row: 1;
  margin: 0;
}
.data-node__value {
  grid-column: 1 / -1;
  grid-row: 2;
  min-width: 0;
  padding-left: 12px;
  border-left: 1px solid var(--el-border-color-lighter);
}
.data-node__add {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.data-node__add .el-input {
  max-width: 100%;
}
.data-node__add .el-select {
  width: 140px;
  max-width: 100%;
}
</style>
