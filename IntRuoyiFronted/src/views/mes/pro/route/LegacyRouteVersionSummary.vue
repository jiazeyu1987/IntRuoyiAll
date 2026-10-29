<template>
  <section data-testid="legacy-route-version-summary">
    <el-alert
      title="历史摘要不完整，不可执行"
      :description="LEGACY_ROUTE_VERSION_NOTICE"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-descriptions class="mt-16px" :column="2" border>
      <el-descriptions-item label="版本">{{ version.versionNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">历史不完整（不可执行）</el-descriptions-item>
      <el-descriptions-item label="版本编号">{{ version.id }}</el-descriptions-item>
      <el-descriptions-item label="路线编号">{{ version.routeId }}</el-descriptions-item>
      <el-descriptions-item label="摘要格式">{{
        version.routeSnapshotFormatVersion
      }}</el-descriptions-item>
    </el-descriptions>
    <el-descriptions class="mt-16px" title="原摘要中保存的字段" :column="2" border>
      <el-descriptions-item label="路线编码">{{ savedField('routeCode') }}</el-descriptions-item>
      <el-descriptions-item label="路线名称">{{ savedField('routeName') }}</el-descriptions-item>
      <el-descriptions-item label="原状态值">{{ savedField('status') }}</el-descriptions-item>
      <el-descriptions-item label="说明">{{ savedField('description') }}</el-descriptions-item>
      <el-descriptions-item label="备注">{{ savedField('remark') }}</el-descriptions-item>
    </el-descriptions>
    <p>原始历史摘要（只读）</p>
    <pre class="legacy-route-summary__raw" data-testid="legacy-route-version-raw">{{
      rawSummary
    }}</pre>
    <el-button class="mt-16px" @click="$emit('back')">返回</el-button>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProRouteVersionVO } from '@/api/mes/pro/route'
import {
  LEGACY_ROUTE_VERSION_NOTICE,
  requireLegacyRouteSummary,
  readLegacyRouteSummaryFields,
  formatLegacyRouteSummaryField
} from './routeVersionHistory'

const props = defineProps<{ version: ProRouteVersionVO }>()
defineEmits(['back'])
const rawSummary = computed(() => requireLegacyRouteSummary(props.version))
const savedFields = computed(() => readLegacyRouteSummaryFields(props.version))
const savedField = (field: string) => formatLegacyRouteSummaryField(savedFields.value, field)
</script>

<style scoped>
.legacy-route-summary__raw {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  padding: 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  max-height: 480px;
  overflow: auto;
}
</style>
