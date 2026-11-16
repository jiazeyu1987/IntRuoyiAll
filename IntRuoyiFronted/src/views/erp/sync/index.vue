<template>
  <div class="erp-configuration-screen">
    <el-alert
      v-if="!canConfigure"
      title="当前未开启测试模式或缺少ERP同步配置权限。"
      type="error"
      :closable="false"
    />
    <TableAutoSyncSetting v-if="canConfigure" class="erp-configuration-content" />
  </div>
</template>
<script setup lang="ts">
import '@/views/erp/components/viewport-list.css'
import TableAutoSyncSetting from './config/components/TableAutoSyncSetting.vue'
import { hasErpConfigCapability } from '@/utils/businessConfigAccess'
import { useTestMode } from '@/store/modules/testMode'

defineOptions({ name: 'ErpKingdeeSync' })
const testMode = useTestMode()
const canConfigure = computed(
  () => testMode.value && hasErpConfigCapability(['infra:job:query', 'erp:kingdee-sync:query'])
)
</script>
