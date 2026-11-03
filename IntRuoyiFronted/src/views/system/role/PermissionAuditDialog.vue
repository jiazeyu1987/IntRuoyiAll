<template>
  <Dialog v-model="visible" title="系统权限审计" width="1100">
    <el-alert
      v-if="runtimeMode && !runtimeMode.auditEnabled"
      class="mb-16px"
      title="当前环境未启用审计，无法生成或查询权限审计记录。"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-form
      :model="query"
      :disabled="!runtimeMode?.auditEnabled"
      inline
      @submit.prevent="handleQuery"
    >
      <el-form-item label="权限操作">
        <el-select v-model="query.operationId" class="!w-220px" @change="handleQuery">
          <el-option
            v-for="operation in operations"
            :key="operation.value"
            :label="operation.label"
            :value="operation.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="对象编号">
        <el-input
          v-model="query.subjectId"
          clearable
          placeholder="如 SYSTEM_ROLE:123"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleQuery">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" border max-height="440">
      <el-table-column prop="id" label="事件编号" min-width="100" />
      <el-table-column prop="serverOccurredAt" label="发生时间（UTC）" min-width="190" />
      <el-table-column label="对象" min-width="160"
        ><template #default="{ row }">{{
          permissionAuditSubjectLabel(row.subjectId)
        }}</template></el-table-column
      >
      <el-table-column prop="actorUsername" label="操作者账号" min-width="120" />
      <el-table-column prop="actorDisplayName" label="操作者姓名" min-width="120" />
      <el-table-column prop="reason" label="变更原因" min-width="180" show-overflow-tooltip />
      <el-table-column label="结果" width="110">
        <template #default="{ row }">{{ permissionAuditResultLabel(row.resultStatus) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-if="runtimeMode?.auditEnabled"
      v-model:page="query.pageNo"
      v-model:limit="query.pageSize"
      :total="total"
      @pagination="loadRows"
    />
  </Dialog>
  <Dialog v-model="detailVisible" title="权限审计详情" width="900">
    <div v-loading="detailLoading">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="事件编号">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{
          operations.find((item) => item.value === detail?.operationId)?.label || '未识别权限操作'
        }}</el-descriptions-item>
        <el-descriptions-item label="对象">{{
          permissionAuditSubjectLabel(detail.subjectId)
        }}</el-descriptions-item>
        <el-descriptions-item label="操作者"
          >{{ detail.actorDisplayName }}（{{ detail.actorUsername }}）</el-descriptions-item
        >
        <el-descriptions-item label="时间（UTC）">{{
          detail.serverOccurredAt
        }}</el-descriptions-item>
        <el-descriptions-item label="结果">{{
          permissionAuditResultLabel(detail.resultStatus)
        }}</el-descriptions-item>
        <el-descriptions-item label="变更原因" :span="2">{{ detail.reason }}</el-descriptions-item>
        <el-descriptions-item label="变更前" :span="2"
          ><ReadableData title="授权前" :source="detail.beforeStateJson"
        /></el-descriptions-item>
        <el-descriptions-item label="变更后" :span="2"
          ><ReadableData title="授权后" :source="detail.afterStateJson"
        /></el-descriptions-item>
        <el-descriptions-item label="技术详情" :span="2">
          <details class="permission-audit-state"
            ><summary>查看操作码、请求标识与校验码</summary> <p>操作码：{{ detail.operationId }}</p
            ><p>结果码：{{ detail.resultStatus }}</p>
            <p>对象标识：{{ detail.subjectId }}</p>
            <p>请求编号：{{ detail.requestId }}</p
            ><p>幂等键：{{ detail.idempotencyKey }}</p>
            <p>事件校验码：{{ detail.eventHash }}</p>
          </details>
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </Dialog>
</template>
<script setup lang="ts">
import {
  getGxpAuditEvent,
  getGxpAuditEventPage,
  type GxpAuditEvent
} from '@/api/system/gxpAuditEvent'
import { getGxpAuditRuntimeMode, type GxpAuditRuntimeMode } from '@/api/system/gxpAuditMode'
import ReadableData from '@/components/BusinessData/ReadableData.vue'
import { permissionAuditResultLabel, permissionAuditSubjectLabel } from './permissionAuditDisplay'

const operations = [
  { label: '角色菜单授权', value: 'system.permission.role-menu.assign' },
  { label: '用户角色授权', value: 'system.permission.user-role.assign' },
  { label: '角色数据权限', value: 'system.permission.role-data-scope.assign' }
]
const visible = ref(false)
const loading = ref(false)
const rows = ref<GxpAuditEvent[]>([])
const total = ref(0)
const runtimeMode = ref<GxpAuditRuntimeMode>()
const query = reactive({ pageNo: 1, pageSize: 10, operationId: operations[0].value, subjectId: '' })

const loadRows = async () => {
  loading.value = true
  rows.value = []
  total.value = 0
  try {
    const result = await getGxpAuditEventPage({
      ...query,
      subjectId: query.subjectId.trim() || undefined
    })
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}
const handleQuery = async () => {
  query.pageNo = 1
  await loadRows()
}
const open = async () => {
  runtimeMode.value = undefined
  runtimeMode.value = await getGxpAuditRuntimeMode()
  query.pageNo = 1
  rows.value = []
  total.value = 0
  visible.value = true
  if (runtimeMode.value.auditEnabled) await loadRows()
}
defineExpose({ open })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<GxpAuditEvent | null>(null)
const showDetail = async (id: number) => {
  detail.value = null
  detailLoading.value = true
  try {
    detail.value = await getGxpAuditEvent(id)
    if (!detail.value) throw new Error('该审计事件不存在或当前租户不可见')
    detailVisible.value = true
  } finally {
    detailLoading.value = false
  }
}
</script>
<style scoped>
.permission-audit-state {
  margin: 0;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  font-size: 12px;
}
</style>
