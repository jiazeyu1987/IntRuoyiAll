<template>
  <el-button :disabled="!scope || disabled" @click="open">修改签名密码</el-button>
  <el-dialog v-model="visible" title="修改员工签名密码" width="440px" :close-on-click-modal="false" @closed="clear">
    <p>{{ employeeName }}：首次使用或重置后，请先设置个人签名密码。</p>
    <el-form label-width="100px" @submit.prevent>
      <el-form-item label="当前签名密码"><el-input v-model="currentPassword" type="password" show-password autocomplete="off" /></el-form-item>
      <el-form-item label="新签名密码"><el-input v-model="newPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
      <el-form-item label="确认新密码"><el-input v-model="confirmation" type="password" show-password autocomplete="new-password" /></el-form-item>
    </el-form>
    <p v-if="error" role="alert">{{ error }}</p>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存签名密码</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { changeEmployeeSignaturePassword, type EmployeeSignatureScope } from '@/api/mes/pro/employeeSignatureCredential'

const props = defineProps<{ scope?: EmployeeSignatureScope; employeeName: string; disabled: boolean }>()
const message = useMessage()
const visible = ref(false)
const saving = ref(false)
const currentPassword = ref('')
const newPassword = ref('')
const confirmation = ref('')
const error = ref('')
const clear = () => {
  currentPassword.value = ''
  newPassword.value = ''
  confirmation.value = ''
  error.value = ''
}
const open = () => { clear(); visible.value = true }
watch(() => JSON.stringify(props.scope), () => { visible.value = false; clear() })
const save = async () => {
  if (!props.scope || saving.value) return
  if (!currentPassword.value || !newPassword.value || newPassword.value !== confirmation.value) {
    error.value = '请填写当前密码和新密码，并确保两次新密码一致。'
    return
  }
  const scope = { ...props.scope }
  saving.value = true
  error.value = ''
  try {
    await changeEmployeeSignaturePassword({ ...scope, currentPassword: currentPassword.value, newPassword: newPassword.value })
    visible.value = false
    message.success('签名密码已更新，请使用新密码提交。')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    currentPassword.value = ''
    newPassword.value = ''
    confirmation.value = ''
    saving.value = false
  }
}
</script>
