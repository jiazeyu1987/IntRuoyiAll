<template>
  <div v-if="canQuery" class="edhr-config">
    <RecordbookGlobalSetting />
    <ReleaseDossierRequirements />
  </div>
  <el-alert v-else title="无权查看 eDHR配置。" type="error" :closable="false" show-icon />
</template>
<script setup lang="ts">
import { GOLDEN_FINGER_PERMISSION, hasExplicitConfigPermission } from '@/utils/businessConfigAccess'
import RecordbookGlobalSetting from './components/RecordbookGlobalSetting.vue'
import ReleaseDossierRequirements from './components/ReleaseDossierRequirements.vue'
defineOptions({ name: 'MesEdhrGlobalConfig' })
const canQuery = computed(() => hasExplicitConfigPermission(GOLDEN_FINGER_PERMISSION))
</script>
<style scoped>
.edhr-config {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
