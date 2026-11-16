<template>
  <div class="data-node">
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
const props = defineProps<{ modelValue: any; label?: string }>()
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
  }
)
const types = [
  { label: '文本', value: 'text' },
  { label: '数值', value: 'number' },
  { label: '是或否', value: 'boolean' },
  { label: '明细分组', value: 'object' },
  { label: '条目列表', value: 'array' }
]
const isContainer = computed(
  () => props.modelValue !== null && typeof props.modelValue === 'object'
)
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
  const value =
    newType.value === 'object'
      ? {}
      : newType.value === 'array'
        ? []
        : newType.value === 'number'
          ? 0
          : newType.value === 'boolean'
            ? false
            : ''
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
.data-node__row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.data-node__label {
  flex: 0 0 120px;
  overflow-wrap: anywhere;
}
.data-node__value {
  flex: 1;
  min-width: 0;
}
.data-node__add {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.data-node__add .el-input {
  max-width: 180px;
}
.data-node__add .el-select {
  width: 130px;
}
@media (max-width: 600px) {
  .data-node__row {
    flex-wrap: wrap;
  }
  .data-node__label {
    flex-basis: 100%;
  }
}
</style>
