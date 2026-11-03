<template>
  <el-table :data="rows" empty-text="暂无历史记录">
    <el-table-column label="事件" min-width="160">
      <template #default="{ row }">
        <div>{{ certificateHistoryLabel(row.eventType) }}</div>
        <details
          ><summary>原始事件信息</summary> <p>事件码：{{ row.eventType }}</p
          ><p>对象码：{{ row.itemType }}</p>
          <p v-if="row.eventId">事件编号：{{ row.eventId }}</p>
        </details>
      </template>
    </el-table-column>
    <el-table-column label="变更对象" min-width="140">
      <template #default="{ row }">{{ certificateHistoryLabel(row.itemType) }}</template>
    </el-table-column>
    <el-table-column label="操作人" min-width="130">
      <template #default="{ row }">{{
        row.actorId == null ? '未记录' : `人员编号 ${row.actorId}`
      }}</template>
    </el-table-column>
    <el-table-column label="变更前" min-width="250">
      <template #default="{ row }"
        ><ReadableData title="变更前" :source="row.beforeValueJson"
      /></template>
    </el-table-column>
    <el-table-column label="变更后" min-width="250">
      <template #default="{ row }"
        ><ReadableData title="变更后" :source="row.afterValueJson"
      /></template>
    </el-table-column>
    <el-table-column label="批件文件" width="120">
      <template #default="{ row }"
        ><el-tag v-if="row.businessFileId" type="success">可申请下载</el-tag
        ><span v-else>未记录</span></template
      >
    </el-table-column>
  </el-table>
</template>
<script setup lang="ts">
import ReadableData from '@/components/BusinessData/ReadableData.vue'
import type { DccRegistrationCertificateHistoryItemVO } from '@/api/dcc/registrationCertificate'
import { certificateHistoryLabel } from './historyDisplay'
defineProps<{ rows: DccRegistrationCertificateHistoryItemVO[] }>()
</script>
<style scoped>
details {
  margin-top: 8px;
  overflow-wrap: anywhere;
}
summary {
  cursor: pointer;
  color: var(--el-text-color-secondary);
}
p {
  margin: 4px 0;
}
</style>
