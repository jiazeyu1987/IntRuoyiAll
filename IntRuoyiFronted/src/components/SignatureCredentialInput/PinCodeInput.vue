<template>
  <div class="pin-code-input" role="group" :aria-label="label" @paste.prevent="paste">
    <input v-for="(_, index) in digits" :key="index" :ref="el => inputs[index] = el as HTMLInputElement"
      :value="digits[index]" type="text" inputmode="numeric" pattern="[0-9]*" maxlength="1"
      autocomplete="off" autocorrect="off" autocapitalize="off" :spellcheck="false"
      data-lpignore="true" data-1p-ignore data-bwignore="true" :disabled="disabled" :aria-label="`${label}第 ${index + 1} 位`"
      @input="input(index, $event)" @keydown="keydown(index, $event)" @focus="($event.target as HTMLInputElement).select()" />
  </div>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
const props = withDefaults(defineProps<{ modelValue: string; disabled?: boolean; label?: string; completeOnly?: boolean }>(), { label: '六位 PIN 码' })
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const digits = ref<string[]>(Array(6).fill(''))
const inputs: HTMLInputElement[] = []
let emitting = false
let lastPublished: string | undefined
watch(() => props.modelValue, value => {
  if (!emitting && value !== lastPublished) digits.value = Array.from({ length: 6 }, (_, i) => value[i] || '')
}, { immediate: true, flush: 'sync' })
const publish = () => {
  emitting = true
  const value = digits.value.join('')
  lastPublished = props.completeOnly && !/^[0-9]{6}$/.test(value) ? '' : value
  emit('update:modelValue', lastPublished)
  emitting = false
}
const input = (index: number, event: Event) => {
  const target = event.target as HTMLInputElement
  const value = target.value.replace(/[^0-9]/g, '').slice(-1)
  digits.value[index] = value; target.value = value; publish()
  if (value && index < 5) inputs[index + 1]?.focus()
}
const keydown = (index: number, event: KeyboardEvent) => {
  if (event.key === 'Backspace' && !digits.value[index] && index > 0) {
    event.preventDefault(); digits.value[index - 1] = ''; publish(); inputs[index - 1]?.focus()
  }
  if (event.key === 'ArrowLeft' && index > 0) { event.preventDefault(); inputs[index - 1]?.focus() }
  if (event.key === 'ArrowRight' && index < 5) { event.preventDefault(); inputs[index + 1]?.focus() }
}
const paste = (event: ClipboardEvent) => {
  if (props.disabled) return
  const value = event.clipboardData?.getData('text') || ''
  if (!/^[0-9]{6}$/.test(value)) return
  digits.value = value.split(''); publish(); inputs[5]?.focus()
}
</script>
<style scoped>
.pin-code-input { display: flex; gap: 10px; max-width: 100%; }
/* PIN uses masked text fields so browsers do not treat each cell as a saved-login password. */
.pin-code-input input { -webkit-text-security: disc; box-sizing: border-box; width: 44px; min-width: 0; height: 48px; text-align: center; font-size: 22px; border: 1px solid var(--el-border-color); border-radius: 4px; background: var(--el-fill-color-blank); color: var(--el-text-color-primary); }
.pin-code-input input:focus { outline: 2px solid var(--el-color-primary); outline-offset: 1px; }
.pin-code-input input:disabled { background: var(--el-disabled-bg-color); cursor: not-allowed; }
</style>
