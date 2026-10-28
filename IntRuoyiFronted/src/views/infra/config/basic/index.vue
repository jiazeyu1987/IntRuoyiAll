<template>
  <IdleLogoutSetting v-if="canQuery" :can-update="canUpdate" />
  <GxpAuditModeSetting v-if="canQuery" :can-update="canUpdate" />
  <el-alert v-else title="无权查看基础配置。" type="error" :closable="false" show-icon />
  <el-card v-if="canQuery && testMode && (canMes || canErp || canDcc)" class="config-shortcuts" shadow="never">
    <template #header>配置快捷导航</template>
    <div class="config-shortcuts__links">
      <el-button v-if="canMes" class="app-test-button" @click="router.push({ name: 'MesEdhrGlobalConfig' })">eDHR配置</el-button>
      <el-button v-if="canErp" class="app-test-button" @click="router.push({ name: 'ErpTableAutoSyncConfig' })">自动同步配置</el-button>
      <el-button v-if="canDcc" class="app-test-button" @click="router.push({ name: 'DccRegistrationCertificateConfig' })">注册证配置</el-button>
    </div>
  </el-card>
</template>
<script setup lang="ts">
import { GOLDEN_FINGER_PERMISSION, hasExplicitConfigPermission, hasInfraConfigPermission, hasErpConfigCapability } from '@/utils/businessConfigAccess'
import { useTestMode } from '@/store/modules/testMode'
import IdleLogoutSetting from './components/IdleLogoutSetting.vue'
import GxpAuditModeSetting from './components/GxpAuditModeSetting.vue'
defineOptions({ name: 'InfraBasicConfig' })
const router = useRouter()
const testMode = useTestMode()
const canMes = computed(() => hasExplicitConfigPermission(GOLDEN_FINGER_PERMISSION))
const canErp = computed(() => hasErpConfigCapability(['infra:job:query', 'erp:kingdee-sync:query']))
const canDcc = computed(() => hasExplicitConfigPermission('dcc:registration-certificate:config:query'))
const canQuery = computed(() => hasInfraConfigPermission('infra:config:query'))
const canUpdate = computed(() => hasInfraConfigPermission('infra:config:update'))
</script>
<style scoped>
.config-shortcuts {
  margin-top: 16px;
}
.config-shortcuts__links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.config-shortcuts__links :deep(.el-button) {
  margin-left: 0;
}
</style>
