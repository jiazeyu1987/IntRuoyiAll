<template>
  <el-button v-hasPermi="[erpSyncPermission(props.syncType)]" type="primary" plain @click="open">
    按编号同步
  </el-button>
  <el-dialog v-model="visible" title="按编号同步ERP数据" width="460px" :close-on-click-modal="!syncing" :close-on-press-escape="!syncing" :show-close="!syncing">
    <el-form @submit.prevent="submit">
      <el-form-item :label="codeLabel">
        <el-input v-model="code" :placeholder="`请输入${codeLabel}`" maxlength="100" :disabled="syncing" @keyup.enter="submit" />
      </el-form-item>
    </el-form>
    <el-alert v-if="resultMessage" :title="resultMessage" :type="resultType" :closable="false" show-icon />
    <template #footer>
      <el-button :disabled="syncing" @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="syncing" :disabled="!code.trim()" @click="submit">同步</el-button>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { erpSyncPermission } from '@/utils/erpDataPermission'
import request from '@/config/axios'

const props = defineProps<{
  syncType: string
  codeLabel: string
  onSynced: (code: string) => Promise<void>
  successMessage?: string
}>()
const visible = ref(false)
const syncing = ref(false)
const code = ref('')
const resultMessage = ref('')
const resultType = ref<'success' | 'error'>('success')
const open = () => {
  code.value = ''
  resultMessage.value = ''
  visible.value = true
}
const submit = async () => {
  if (syncing.value || !code.value.trim()) return
  syncing.value = true
  resultMessage.value = ''
  let synchronized = false
  try {
    const result = await request.post({
      url: '/mes/erp-code-sync',
      data: { syncType: props.syncType, code: code.value.trim() },
      timeout: 180000
    })
    synchronized = true
    await props.onSynced(result.code)
    resultType.value = 'success'
    resultMessage.value = `同步成功：${result.code}，${props.successMessage || '列表已仅显示对应数据。'}`
  } catch (error: any) {
    resultType.value = 'error'
    const reason = typeof error === 'string' ? error : error?.message
    const resultUnknown = !synchronized && props.syncType === 'PRODUCTION_ORDER' &&
      (error?.code === 'ECONNABORTED' || error?.code === 'ETIMEDOUT' ||
        error?.code === 'ERR_NETWORK' || /timeout|network error/i.test(reason || '') ||
        Number(error?.response?.status) >= 500)
    resultMessage.value = resultUnknown
      ? `同步结果未知：请求未取得可靠结果，服务端可能仍在执行。请先核实工单及同步记录，勿重复提交。${reason || ''}`
      : `${synchronized ? 'ERP同步成功，但列表刷新失败' : '同步失败'}：${reason || '请求失败，请查看错误提示'}`
  } finally {
    syncing.value = false
  }
}
</script>
