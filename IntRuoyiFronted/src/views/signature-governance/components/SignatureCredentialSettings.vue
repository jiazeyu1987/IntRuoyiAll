<template>
  <section v-loading="loading" class="signature-credentials">
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-form label-width="130px" @submit.prevent="save">
      <el-form-item label="签名方式">
        <el-radio-group v-model="credentialType" :disabled="!loaded || saving">
          <el-radio value="PIN">六位 PIN 码</el-radio>
          <el-radio value="PASSWORD">登录密码</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="credentialType === 'PIN'" label="新 PIN 码">
        <el-input v-model="newPin" type="password" show-password inputmode="numeric" maxlength="6" autocomplete="new-password" placeholder="留空保留当前 PIN，修改时输入六位数字" :disabled="!loaded || saving" />
      </el-form-item>
      <el-form-item v-if="credentialType === 'PIN'" label="确认新 PIN 码">
        <el-input v-model="confirmPin" type="password" show-password inputmode="numeric" maxlength="6" autocomplete="new-password" placeholder="再次输入新 PIN 码" :disabled="!loaded || saving" />
      </el-form-item>
      <el-form-item v-if="credentialType === 'PASSWORD'" label="密码签名">
        <span>签名时使用当前登录密码，修改登录密码后同步生效。</span>
      </el-form-item>
      <el-form-item label="当前登录密码" required>
        <el-input v-model="password" type="password" show-password autocomplete="current-password" placeholder="验证本人身份后保存设置" :disabled="!loaded || saving" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" :disabled="!loaded" native-type="submit">保存签名设置</el-button>
        <el-button v-if="credentialType === 'PASSWORD'" :disabled="!loaded || saving" @click="router.push({ path: '/user/profile', query: { tab: 'resetPwd' } })">修改登录密码</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>
<script lang="ts" setup>
import { getSignatureSettings, updateSignatureSettings, type SignatureCredentialType } from '@/api/system/user/profile'
const router = useRouter()
const message = useMessage()
const credentialType = ref<SignatureCredentialType>()
const newPin = ref('')
const confirmPin = ref('')
const password = ref('')
const loading = ref(false)
const saving = ref(false)
const loaded = ref(false)
const error = ref('')
watch(credentialType, () => {
  newPin.value = ''; confirmPin.value = ''; password.value = ''; error.value = ''
}, { flush: 'sync' })
const errorText = (cause: unknown) => cause instanceof Error ? cause.message : String(cause)
const load = async () => {
  loading.value = true
  try {
    const settings = await getSignatureSettings()
    if (!['PIN', 'PASSWORD'].includes(settings.credentialType)) throw new Error('签名方式缺失或无效，请联系管理员')
    credentialType.value = settings.credentialType
    loaded.value = true
  } catch (cause) { error.value = errorText(cause) }
  finally { loading.value = false }
}
const save = async () => {
  if (!loaded.value || !credentialType.value || saving.value) return
  error.value = ''
  if (!password.value) { error.value = '请输入当前登录密码'; return }
  if (credentialType.value === 'PIN' && (newPin.value || confirmPin.value) && (!/^[0-9]{6}$/.test(newPin.value) || newPin.value !== confirmPin.value)) {
    error.value = 'PIN 码必须为六位数字，且两次输入一致'; return
  }
  saving.value = true
  try {
    await updateSignatureSettings({ credentialType: credentialType.value, password: password.value, ...(credentialType.value === 'PIN' && newPin.value ? { newPin: newPin.value } : {}) })
    newPin.value = ''; confirmPin.value = ''; password.value = ''
    message.success('签名设置已保存，请按所选方式输入签名凭据')
  } catch (cause) { error.value = errorText(cause) }
  finally { saving.value = false; password.value = '' }
}
onMounted(load)
</script>
<style scoped>
.signature-credentials { max-width: 720px; margin-bottom: 24px; }
.signature-credentials .el-form { margin-top: 16px; }
</style>
