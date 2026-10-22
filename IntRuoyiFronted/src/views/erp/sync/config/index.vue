<template>
  <TableAutoSyncSetting v-if="canQuery" />
  <el-alert
    v-else
    title="无权查看自动同步配置，需要金手指、Job查询和ERP同步查询权限。"
    type="error"
    :closable="false"
    show-icon
  />
</template>
<script setup lang="ts">
import { hasErpConfigCapability } from '@/utils/businessConfigAccess'
import TableAutoSyncSetting from './components/TableAutoSyncSetting.vue'
defineOptions({ name: 'ErpTableAutoSyncConfig' })
const canQuery = computed(() =>
  hasErpConfigCapability(['infra:job:query', 'erp:kingdee-sync:query'])
)
</script>
