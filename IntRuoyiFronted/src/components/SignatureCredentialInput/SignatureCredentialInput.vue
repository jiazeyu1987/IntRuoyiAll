<template>
  <div class="signature-credential-input" v-bind="$attrs">
    <span v-if="error" role="alert" class="signature-credential-error">{{ error }}</span>
    <template v-else-if="resolvedType">
      <div class="signature-credential-method">{{ resolvedType === 'PIN' ? '六位 PIN 码' : (employeeScope ? '签名密码' : passwordLabel || '登录密码') }}</div>
      <span v-if="resolvedType === 'PIN' && modelValue && !/^[0-9]{6}$/.test(modelValue)" class="signature-credential-error" role="alert">请输入完整的六位数字 PIN 码</span>
      <PinCodeInput complete-only v-if="resolvedType === 'PIN'" :model-value="modelValue" :disabled="disabled" @update:model-value="update" />
      <el-input v-else :model-value="modelValue" :type="disableAutofill ? 'text' : 'password'" :show-password="!disableAutofill"
        :class="{ 'signature-manual-credential': disableAutofill }"
        :autocomplete="disableAutofill ? 'off' : 'current-password'"
        autocorrect="off" autocapitalize="off" :spellcheck="false"
        aria-label="签名凭据"
        :data-lpignore="disableAutofill ? 'true' : undefined" :data-1p-ignore="disableAutofill ? '' : undefined"
        :data-bwignore="disableAutofill ? 'true' : undefined"
        :placeholder="employeeScope ? '请输入本人签名密码' : `请输入本人${passwordLabel || '当前登录密码'}`" :disabled="disabled" @update:model-value="update" />
    </template>
    <span v-else>正在读取签名方式…</span>
  </div>
</template>
<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import PinCodeInput from './PinCodeInput.vue'
import { getSignatureSettings, type SignatureCredentialType } from '@/api/system/user/profile'
import { getEmployeeSignatureQualification, getFrontlineSignatureSettings, type EmployeeSignatureScope, type FrontlineSignatureScope } from '@/api/mes/pro/employeeSignatureCredential'
defineOptions({ inheritAttrs: false })
const props = defineProps<{ modelValue: string; disabled?: boolean; credentialType?: SignatureCredentialType; employeeScope?: EmployeeSignatureScope; frontlineScope?: FrontlineSignatureScope; passwordLabel?: string; disableAutofill?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const resolvedType = ref<SignatureCredentialType>()
const error = ref('')
let generation = 0
const update = (value: string) => emit('update:modelValue', value)
const load = async () => {
  const current = ++generation
  resolvedType.value = undefined; error.value = ''; update('')
  try {
    const type = props.credentialType || (props.frontlineScope
      ? (await getFrontlineSignatureSettings(props.frontlineScope)).credentialType
      : props.employeeScope
      ? (await getEmployeeSignatureQualification(props.employeeScope)).credentialType
      : (await getSignatureSettings()).credentialType)
    if (current !== generation) return
    if (type !== 'PIN' && type !== 'PASSWORD') throw new Error('签名方式缺失或无效，请核对个人签名设置')
    resolvedType.value = type
  } catch (cause) {
    if (current === generation) error.value = cause instanceof Error ? cause.message : String(cause)
  }
}
watch(() => [props.credentialType, props.employeeScope, props.frontlineScope], load, { immediate: true, deep: true })
onMounted(() => window.addEventListener('signature-settings-changed', load))
onBeforeUnmount(() => { generation++; window.removeEventListener('signature-settings-changed', load) })
</script>
<style scoped>
.signature-credential-input { width: 100%; }
.signature-credential-method { margin-bottom: 6px; font-size: 13px; color: var(--el-text-color-secondary); }
.signature-credential-error { color: var(--el-color-danger); }
/* 与 PIN 输入一致：手动签名使用遮蔽文本框，避免 Chrome 识别为已保存的登录密码。 */
.signature-manual-credential :deep(input) { -webkit-text-security: disc; }
</style>
