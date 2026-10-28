<template>
  <el-card class="gxp-mode-setting" shadow="never" data-testid="gxp-mode-setting">
    <template #header>GxP 审计模式</template>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-form v-loading="loading" label-width="140px" :disabled="loading || saving">
      <el-form-item label="当前运行模式">
        <el-tag v-if="runtime" :type="runtime.mode === 'FORMAL' ? 'success' : 'warning'">
          {{ modeLabels[runtime.mode] }}
        </el-tag>
        <span v-else>尚未加载</span>
      </el-form-item>
      <el-form-item label="重启后使用模式">
        <el-select v-model="selectedMode" :disabled="!canUpdate || !configRow" aria-label="GxP 审计模式">
          <el-option v-for="(label, value) in modeLabels" :key="value" :label="label" :value="value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="configRow" label="已保存模式">
        <span>{{ modeLabels[configRow.value as GxpAuditMode] }}</span>
        <el-tag v-if="pendingRestart" class="gxp-mode-setting__pending" type="warning">待重启后端生效</el-tag>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" :disabled="!canUpdate || !configRow || selectedMode === configRow.value" @click="save">
          保存模式
        </el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </el-form-item>
    </el-form>
    <p class="gxp-mode-setting__description">
      配置对连接此数据库的所有租户生效，各后端实例重启后使用已保存模式；保存不会切换当前运行中的事务。
    </p>
    <p class="gxp-mode-setting__description">
      正式审计要求已批准并激活的匹配策略；本地策略审计会记录 LOCAL-DEV 审计；关闭审计会停止统一 GxP 记录和相关证据校验，保留业务权限及签名认证。
      本地策略审计仅允许用于符合数据库限制的 local 环境；关闭审计可用于开发阶段的服务器部署。不删除历史记录，也不自动补齐关闭期间的审计。
    </p>
    <el-alert v-if="!canUpdate" title="当前账号只有查看权限，不能修改 GxP 模式。" type="info" :closable="false" show-icon />
  </el-card>
</template>

<script setup lang="ts">
import * as ConfigApi from '@/api/infra/config'
import { getGxpAuditRuntimeMode, type GxpAuditMode, type GxpAuditRuntimeMode } from '@/api/system/gxpAuditMode'

defineOptions({ name: 'GxpAuditModeSetting' })
const props = defineProps<{ canUpdate: boolean }>()
const message = useMessage()
const modeLabels: Record<GxpAuditMode, string> = {
  FORMAL: '正式审计',
  LOCAL_POLICY: '本地策略审计',
  DEVELOPMENT_DISABLED: '关闭审计（开发模式）'
}
const configKey = 'system.gxp.audit-mode'
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const selectedMode = ref<GxpAuditMode>()
const configRow = ref<ConfigApi.ConfigVO>()
const runtime = ref<GxpAuditRuntimeMode>()
const pendingRestart = computed(() => runtime.value && configRow.value && runtime.value.mode !== configRow.value.value)

const errorMessage = (failure: unknown, text: string) => failure instanceof Error ? failure.message : text
const isMode = (value: string): value is GxpAuditMode => Object.hasOwn(modeLabels, value)

const load = async () => {
  if (loading.value || saving.value) return
  loading.value = true
  error.value = ''
  configRow.value = undefined
  runtime.value = undefined
  selectedMode.value = undefined
  try {
    const [page, current] = await Promise.all([
      ConfigApi.getConfigPage({ pageNo: 1, pageSize: 2, key: configKey } as PageParam & { key: string }),
      getGxpAuditRuntimeMode()
    ])
    const rows = page.list.filter((row: ConfigApi.ConfigVO) => row.key === configKey)
    if (rows.length !== 1 || rows[0].visible !== true || rows[0].type !== 1 || !isMode(rows[0].value)) {
      throw new Error('缺少唯一且有效的 GxP 模式配置，请先执行 GxP 模式配置迁移。')
    }
    if (!isMode(current.mode) || current.auditEnabled !== (current.mode !== 'DEVELOPMENT_DISABLED')) {
      throw new Error('GxP 运行模式返回无效，请检查后端。')
    }
    configRow.value = rows[0]
    runtime.value = current
    selectedMode.value = rows[0].value
  } catch (failure) {
    error.value = errorMessage(failure, 'GxP 模式读取失败。')
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (!props.canUpdate || !configRow.value || !selectedMode.value || saving.value) return
  saving.value = true
  error.value = ''
  try {
    await ConfigApi.updateConfig({ ...configRow.value, value: selectedMode.value })
    configRow.value = { ...configRow.value, value: selectedMode.value }
    message.success('GxP 模式已保存，重启后端后生效。')
  } catch (failure) {
    error.value = errorMessage(failure, 'GxP 模式保存失败。')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.gxp-mode-setting { max-width: 920px; margin-top: 16px; }
.gxp-mode-setting__pending { margin-left: 12px; }
.gxp-mode-setting__description { color: var(--el-text-color-secondary); line-height: 1.7; }
.gxp-mode-setting :deep(.el-select) { width: min(320px, 100%); }
</style>
