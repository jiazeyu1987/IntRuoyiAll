<template>
  <el-card class="sequence-setting" shadow="never">
    <template #header>一线生产工序顺序限制</template>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-form v-loading="loading" label-width="140px">
      <el-form-item label="限制工序提交顺序">
        <el-switch v-model="enabled" :disabled="!loaded || !canUpdate || saving" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" :disabled="!loaded || !canUpdate || enabled === saved" @click="save">保存</el-button>
        <el-button :disabled="saving || loading" @click="load">刷新</el-button>
      </el-form-item>
    </el-form>
    <p>仅对当前租户生效，保存后用于后续一线生产提交。关闭时不限制工序先后。</p>
    <p>开启后按订单锁定的工艺流程检查：串行工序须等待前置工序生产数量达到100%；并行分支可分别提交，汇合工序须等待全部前置工序达到100%。不要求PQC达到100%。</p>
  </el-card>
</template>
<script setup lang="ts">
import request from '@/config/axios'

defineProps<{ canUpdate: boolean }>()
const message = useMessage()
const enabled = ref(false)
const saved = ref(false)
const loaded = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const url = '/mes/pro/frontline-sequence-setting'
const describe = (failure: unknown, text: string) => failure instanceof Error ? failure.message : text
const load = async () => {
  loading.value = true
  loaded.value = false
  error.value = ''
  try {
    const result = await request.get({ url })
    if (typeof result?.enabled !== 'boolean') throw new Error('工序顺序配置返回无效。')
    enabled.value = saved.value = result.enabled
    loaded.value = true
  } catch (failure) {
    error.value = describe(failure, '工序顺序配置读取失败。')
  } finally {
    loading.value = false
  }
}
const save = async () => {
  saving.value = true
  error.value = ''
  try {
    await request.put({ url, data: { enabled: enabled.value } })
    saved.value = enabled.value
    message.success('当前租户工序顺序配置已保存')
  } catch (failure) {
    error.value = describe(failure, '工序顺序配置保存失败。')
  } finally {
    saving.value = false
  }
}
onMounted(load)
</script>
<style scoped>
.sequence-setting { margin-top: 16px; }
p { color: var(--el-text-color-secondary); line-height: 1.6; }
</style>
