<template>
  <el-button :disabled="!scope || disabled" @click="open">签名设置</el-button>
  <el-dialog v-model="visible" title="员工电子签名设置" width="520px" :close-on-click-modal="false" @closed="clear">
    <p>{{ employeeName }}：默认使用六位 PIN 码，初始值 888888。PIN 允许任意六位数字，错误输入不计次、不锁定。</p>
    <p>当前方式：{{ currentType === 'PIN' ? '六位 PIN 码' : currentType === 'PASSWORD' ? '签名密码' : '读取中' }}。修改设置须验证当前签名凭据。</p>
    <el-form v-loading="loading" label-width="130px" @submit.prevent>
      <el-form-item label="签名方式"><el-radio-group v-model="credentialType" :disabled="loading || saving || !currentType"><el-radio value="PIN">六位 PIN 码</el-radio><el-radio value="PASSWORD">签名密码</el-radio></el-radio-group></el-form-item>
      <el-form-item label="当前签名凭据"><SignatureCredentialInput v-if="currentType" v-model="currentPassword" :credential-type="currentType" password-label="签名密码" :disabled="loading || saving" /></el-form-item>
      <el-form-item label="新 PIN 码"><PinCodeInput v-model="newPin" :disabled="loading || saving" /></el-form-item>
      <el-form-item label="确认新 PIN 码"><PinCodeInput v-model="confirmPin" :disabled="loading || saving" /></el-form-item>
      <el-form-item label="新签名密码"><el-input v-model="newPassword" type="password" show-password autocomplete="new-password" placeholder="留空保留当前签名密码" /></el-form-item>
      <el-form-item label="确认新密码"><el-input v-model="confirmation" type="password" show-password autocomplete="new-password" /></el-form-item>
    </el-form>
    <p>新密码沿用现有密码规则：至少八位，包含大小写字母、数字和特殊字符。</p>
    <p v-if="error" role="alert">{{ error }}</p>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="loading || !currentType" :loading="saving" @click="save">保存签名设置</el-button>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import SignatureCredentialInput from '@/components/SignatureCredentialInput/SignatureCredentialInput.vue'
import PinCodeInput from '@/components/SignatureCredentialInput/PinCodeInput.vue'
import { changeEmployeeSignaturePassword, getEmployeeSignatureQualification, type EmployeeSignatureScope } from '@/api/mes/pro/employeeSignatureCredential'
const props = defineProps<{ scope?: EmployeeSignatureScope; employeeName: string; disabled: boolean }>()
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const currentType = ref<'PIN' | 'PASSWORD'>()
const credentialType = ref<'PIN' | 'PASSWORD'>()
const currentPassword = ref('')
const newPassword = ref('')
const confirmation = ref('')
const newPin = ref('')
const confirmPin = ref('')
const error = ref('')
const clear = () => {
  currentPassword.value = ''; newPassword.value = ''; confirmation.value = ''; newPin.value = ''; confirmPin.value = ''; error.value = ''
  currentType.value = undefined; credentialType.value = undefined
}
const open = async () => {
  if (!props.scope) return
  clear(); visible.value = true; loading.value = true
  const scope = { ...props.scope }
  try {
    const qualification = await getEmployeeSignatureQualification(scope)
    if (!visible.value || JSON.stringify(scope) !== JSON.stringify(props.scope)) return
    if (!['PIN', 'PASSWORD'].includes(qualification.credentialType)) throw new Error('签名方式缺失或无效')
    currentType.value = qualification.credentialType; credentialType.value = qualification.credentialType
  } catch (cause) { error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { loading.value = false }
}
watch(() => JSON.stringify(props.scope), () => { visible.value = false; clear() })
const save = async () => {
  if (!props.scope || !currentType.value || !credentialType.value || saving.value) return
  if (!currentPassword.value || newPassword.value !== confirmation.value) {
    error.value = '请填写当前签名凭据，并确保两次新密码一致。'; return
  }
  if ((newPin.value || confirmPin.value) && (!/^[0-9]{6}$/.test(newPin.value) || newPin.value !== confirmPin.value)) {
    error.value = 'PIN 码必须为六位数字，且两次输入一致。'; return
  }
  saving.value = true; error.value = ''
  try {
    await changeEmployeeSignaturePassword({ ...props.scope, currentPassword: currentPassword.value, credentialType: credentialType.value,
      ...(newPassword.value ? { newPassword: newPassword.value } : {}), ...(newPin.value ? { newPin: newPin.value } : {}) })
    window.dispatchEvent(new Event('signature-settings-changed'))
    visible.value = false; message.success('签名设置已更新，请按所选方式提交。')
  } catch (cause) { error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { currentPassword.value = ''; newPassword.value = ''; confirmation.value = ''; newPin.value = ''; confirmPin.value = ''; saving.value = false }
}
</script>
