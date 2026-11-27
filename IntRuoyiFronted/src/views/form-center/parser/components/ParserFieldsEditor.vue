<template>
  <div ref="root" class="parser-fields-editor">
    <BusinessJsonInput
      ref="editor"
      v-model="value"
      label="解析结果"
      hint="按字段核对或修改解析结果，完成后点击应用更新右侧预览。"
    />
  </div>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import BusinessJsonInput from '@/components/BusinessData/BusinessJsonInput.vue'
const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const root = ref<HTMLElement>()
const editor = ref<InstanceType<typeof BusinessJsonInput>>()
const value = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})
function findMatches(keyword: string) {
  const nodes = Array.from(
    root.value?.querySelectorAll<HTMLElement>(
      '.data-node__label, .data-node > input, .data-node > span, .data-node .el-input__inner, .data-node .el-textarea__inner, .data-node > .el-switch'
    ) || []
  )
  return nodes.filter((node) => {
    if (node.closest('.data-node__add')) return false
    const text =
      node instanceof HTMLInputElement || node instanceof HTMLTextAreaElement
        ? node.value
        : node.textContent || ''
    return text.includes(keyword)
  })
}
function locate(keyword: string, index: number) {
  const field = findMatches(keyword)[index]
  if (field) {
    field.scrollIntoView({ block: 'center', behavior: 'smooth' })
    field.focus()
  }
}
defineExpose({ locate, findMatches, validate: () => editor.value?.validate() })
</script>
<style scoped>
.parser-fields-editor {
  max-height: 880px;
  overflow: auto;
}
</style>
