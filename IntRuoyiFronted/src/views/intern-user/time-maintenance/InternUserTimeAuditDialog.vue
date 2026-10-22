<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="860px">
    <el-table v-loading="loading" :data="list">
      <el-table-column label="对象" min-width="180">
        <template #default="{ row }">
          {{ row.targetName || row.targetId }}
        </template>
      </el-table-column>
      <el-table-column label="字段" prop="fieldName" width="140" />
      <el-table-column label="原时间" prop="oldTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="新时间" prop="newTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作人" prop="operatorUserId" width="110" />
      <el-table-column label="操作时间" prop="createTime" width="180" :formatter="dateFormatter" />
    </el-table>
  </Dialog>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as TimeMaintenanceApi from '@/api/intern-user/timeMaintenance'

defineOptions({ name: 'InternUserTimeAuditDialog' })

type AuditKind = 'fileUploadTime' | 'dccPublishedTime' | 'dccObsoletedTime'

const dialogVisible = ref(false)
const dialogTitle = ref('时间修改审计')
const loading = ref(false)
const list = ref<TimeMaintenanceApi.InternUserTimeMaintenanceAuditVO[]>([])

const loadAuditList = async (kind: AuditKind, targetId: string) => {
  switch (kind) {
    case 'fileUploadTime':
      return TimeMaintenanceApi.getFileUploadTimeAudits(targetId)
    case 'dccPublishedTime':
      return TimeMaintenanceApi.getDccPublishedTimeAudits(targetId)
    case 'dccObsoletedTime':
      return TimeMaintenanceApi.getDccObsoletedTimeAudits(targetId)
    default:
      throw new Error(`未支持的实习用户时间审计类型：${kind}`)
  }
}

const open = async (kind: AuditKind, targetId: string, title: string) => {
  dialogTitle.value = title
  dialogVisible.value = true
  loading.value = true
  try {
    list.value = await loadAuditList(kind, targetId)
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
