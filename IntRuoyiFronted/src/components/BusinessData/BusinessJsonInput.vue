<template>
  <div class="business-json-input">
    <label class="business-json-input__file">
      {{ fileOnly ? '选择正式资料文件' : '从文件导入资料' }}
      <input
        type="file"
        accept=".json,application/json"
        :aria-label="`${label}文件`"
        @change="importFile"
      />
    </label>
    <p>{{
      hint ||
      (fileOnly
        ? '请选择业务系统导出的正式资料，导入后可核对内容。'
        : '按字段填写资料，也可导入已有资料文件。')
    }}</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <ReadableData v-if="fileOnly && modelValue" :title="label" :source="modelValue" />
    <BusinessJsonNode
      ref="nodeEditor"
      v-else-if="modelValue && !error"
      :model-value="parsed"
      @update:model-value="update"
    />
    <el-button v-if="!fileOnly && !modelValue" @click="update(array ? [] : {})"
      >开始填写{{ label }}</el-button
    >
    <details v-if="!fileOnly && modelValue"
      ><summary>技术详情（原始数据）</summary><pre>{{ modelValue }}</pre>
    </details>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import BusinessJsonNode from './BusinessJsonNode.vue'
import ReadableData from './ReadableData.vue'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    label: string
    hint?: string
    fileOnly?: boolean
    array?: boolean
  }>(),
  { modelValue: '' }
)
const emit = defineEmits<{ 'update:modelValue': [value: string]; imported: [file: File] }>()
const nodeEditor = ref<InstanceType<typeof BusinessJsonNode>>()
const fileError = ref('')
const parseResult = computed(() => {
  if (!props.modelValue) return { value: undefined, error: '' }
  try {
    const value = JSON.parse(props.modelValue)
    if (
      value === null ||
      typeof value !== 'object' ||
      Array.isArray(value) !== Boolean(props.array)
    ) {
      throw new Error(props.array ? '资料应包含条目列表。' : '资料应包含业务字段。')
    }
    if (!props.fileOnly) validateEditableNumbers(value)
    return { value, error: '' }
  } catch (error) {
    return {
      value: undefined,
      error:
        error instanceof SyntaxError
          ? '资料格式错误，请选择有效的 JSON 资料文件。'
          : (error as Error).message
    }
  }
})
const parsed = computed(() => parseResult.value.value)
const error = computed(() => fileError.value || parseResult.value.error)
function validateEditableNumbers(value: unknown) {
  if (
    typeof value === 'number' &&
    (!Number.isFinite(value) || (Number.isInteger(value) && !Number.isSafeInteger(value)))
  )
    throw new Error('资料包含超出精度范围的数值，无法安全编辑，请核查源文件中的编号类型。')
  if (value !== null && typeof value === 'object')
    Object.values(value).forEach(validateEditableNumbers)
}
const update = (value: unknown) => {
  fileError.value = ''
  emit('update:modelValue', JSON.stringify(value))
}
const importFile = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  fileError.value = ''
  try {
    const source = await file.text()
    const value = JSON.parse(source)
    if (!value || typeof value !== 'object' || Array.isArray(value) !== Boolean(props.array))
      throw new Error(props.array ? '资料应包含条目列表。' : '资料应包含业务字段。')
    if (!props.fileOnly) validateEditableNumbers(value)
    emit('update:modelValue', source)
    emit('imported', file)
  } catch (error) {
    emit('update:modelValue', '')
    fileError.value =
      error instanceof SyntaxError
        ? '资料格式错误，请重新选择有效的 JSON 资料文件。'
        : `资料导入失败：${(error as Error).message}`
  } finally {
    input.value = ''
  }
}
const validate = (required = true): string => {
  if (error.value) return error.value
  if (required && !props.modelValue.trim()) return `请填写或导入${props.label}`
  return nodeEditor.value?.validate() || ''
}
defineExpose({ validate })
</script>

<style scoped>
.business-json-input {
  width: 100%;
  min-width: 0;
}
.business-json-input__file {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.business-json-input__file input {
  max-width: 100%;
}
.business-json-input p,
summary {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
summary {
  cursor: pointer;
}
pre {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  max-height: 200px;
  overflow: auto;
}
</style>
