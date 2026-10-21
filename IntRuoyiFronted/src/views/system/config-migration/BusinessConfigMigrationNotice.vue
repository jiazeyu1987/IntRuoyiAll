<template>
  <el-alert
    title="业务配置已迁移到所属模块，请选择明确的配置入口。"
    type="info"
    :closable="false"
    show-icon
  />
  <div class="config-migration-links">
    <el-button v-if="canMes" @click="configRouter.push({ name: 'MesEdhrGlobalConfig' })"
      >MES · eDHR配置（记录本 / 放行资料）</el-button
    >
    <el-button v-if="canErp" @click="configRouter.push({ name: 'ErpTableAutoSyncConfig' })"
      >ERP · 自动同步配置</el-button
    >
    <el-button v-if="canDcc" @click="configRouter.push({ name: 'DccRegistrationCertificateConfig' })"
      >DCC · 注册证配置</el-button
    >
    <el-button v-if="canInfra" @click="configRouter.push({ name: 'InfraBasicConfig' })"
      >基础设施 · 基础配置</el-button
    >
  </div>
  <el-alert
    v-if="!canMes && !canErp && !canDcc && !canInfra"
    title="当前账号没有业务配置查看权限。"
    type="warning"
    :closable="false"
  />
</template>
<script setup lang="ts">
const configRouter = useRouter()
import {
  GOLDEN_FINGER_PERMISSION,
  hasExplicitConfigPermission,
  hasInfraConfigPermission,
  hasErpConfigCapability
} from '@/utils/businessConfigAccess'
const canMes = computed(() => hasExplicitConfigPermission(GOLDEN_FINGER_PERMISSION))
const canErp = computed(() => hasErpConfigCapability(['infra:job:query', 'erp:kingdee-sync:query']))
const canDcc = computed(() =>
  hasExplicitConfigPermission('dcc:registration-certificate:config:query')
)
const canInfra = computed(() => hasInfraConfigPermission('infra:config:query'))
</script>
<style scoped>
.config-migration-links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 16px 0;
}
.config-migration-links :deep(.el-button) {
  margin-left: 0;
}
</style>
