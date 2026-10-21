<template>
  <IdleLogoutSetting v-if="canQuery" :can-update="canUpdate" />
  <el-alert v-else title="无权查看基础配置。" type="error" :closable="false" show-icon />
</template>
<script setup lang="ts">
import { hasInfraConfigPermission } from '@/utils/businessConfigAccess'
import IdleLogoutSetting from './components/IdleLogoutSetting.vue'
defineOptions({ name: 'InfraBasicConfig' })
const canQuery = computed(() => hasInfraConfigPermission('infra:config:query'))
const canUpdate = computed(() => hasInfraConfigPermission('infra:config:update'))
</script>
