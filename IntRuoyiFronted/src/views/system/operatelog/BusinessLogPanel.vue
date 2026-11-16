<template>
  <ContentWrap>
    <el-form inline @submit.prevent="search">
      <el-form-item v-for="field in source.fields" :key="field.key" :label="field.label" :required="field.required">
        <el-select v-if="field.options" v-model="query[field.key]" class="!w-240px" clearable>
          <el-option v-for="option in field.options" :key="option.value" v-bind="option" />
        </el-select>
        <el-input v-else v-model="query[field.key]" clearable :placeholder="`请输入${field.label}`" @keyup.enter="search" />
      </el-form-item>
      <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button></el-form-item>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-12px" />
    <el-alert v-else-if="!queried" title="请输入查询条件，查看对应业务对象的历史日志。" type="info" :closable="false" class="mb-12px" />
    <el-table :data="rows" v-loading="loading" border max-height="640">
      <el-table-column v-for="key in columns" :key="key" :prop="key" :label="columnLabels[key]" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ cellText(key, row[key]) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }"><el-button link type="primary" @click="showDetail(row)">详情</el-button></template>
      </el-table-column>
    </el-table>
    <Pagination v-if="total > query.pageSize" v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="load" />
  </ContentWrap>
  <Dialog v-model="detailVisible" :title="`${source.label}详情`" width="min(1100px, 95vw)">
    <ReadableData v-if="detail" :source="JSON.stringify(detail, null, 2)" title="完整操作记录" />
  </Dialog>
</template>
<script setup lang="ts">
import { hasPermission } from '@/directives/permission/hasPermi'
import ReadableData from '@/components/BusinessData/ReadableData.vue'
import { formatDate } from '@/utils/formatTime'
import { businessValueLabel } from '@/views/mes/pro/processpool/components/businessEvidenceDisplay'
import type { LogSource } from './logSources'
const props = defineProps<{ source: LogSource }>()
const route = useRoute()
const query = reactive<Record<string, any>>({ pageNo: 1, pageSize: 20 })
const rows = ref<Record<string, any>[]>([])
const total = ref(0), loading = ref(false), queried = ref(false), error = ref('')
const detail = ref<Record<string, any>>(), detailVisible = ref(false)
const columnLabels: Record<string, string> = {
  id: '记录编号', eventId: '事件编号', eventNo: '事件单号', operationId: '操作编号',
  operationType: '操作类型', eventType: '事件类型', actionName: '操作名称', action: '操作内容',
  content: '内容', summary: '摘要', reason: '操作原因',
  operatorName: '操作人', actorName: '操作人', actorDisplayName: '操作人', userName: '操作人',
  operatorUsername: '操作账号', actorUsername: '操作账号', username: '操作账号', userNickname: '用户',
  resultStatus: '操作结果', result: '结果', status: '状态', success: '是否成功',
  occurredAt: '发生时间', serverOccurredAt: '服务器记录时间', createTime: '创建时间',
  operationTime: '操作时间', operatorTime: '操作时间', beginTime: '开始时间', endTime: '结束时间',
  actorUserId: '操作人编号', operatorUserId: '操作人编号', actorId: '操作人编号', userId: '用户编号',
  bizId: '业务编号', businessId: '业务编号', objectId: '对象编号', objectType: '对象类型',
  logType: '日志类型', recordCategory: '记录类型', moduleName: '操作模块', fileName: '文件名称',
  scheduleOrderId: '排产工单编号', batchExecutionId: '批次执行编号', executionId: '执行编号',
  itemType: '变更对象', flowInstanceId: '流程实例编号', grantId: '授权编号'
}
const resultLabels: Record<string, string> = { SUCCESS: '成功', FAILED: '失败', FAILURE: '失败', CANCELED: '已取消', CANCELLED: '已取消', BLOCKED: '已拦截' }
const summaryKeys = ['id', 'operationType', 'eventType', 'actionName', 'action', 'content', 'reason', 'operatorName', 'actorName', 'actorDisplayName', 'operatorUsername', 'resultStatus', 'occurredAt', 'serverOccurredAt', 'createTime']
const columns = computed(() => {
  const keys = [...new Set(rows.value.flatMap(row => Object.keys(row)))]
  return [...summaryKeys.filter(key => keys.includes(key)), ...keys.filter(key => !summaryKeys.includes(key))]
    .filter(key => columnLabels[key] && rows.value.some(row => row[key] != null && typeof row[key] !== 'object')).slice(0, 12)
})
const cellText = (key: string, value: unknown) => {
  if (value == null) return '—'
  if (['resultStatus', 'result', 'status'].includes(key) && typeof value === 'string' && resultLabels[value]) return resultLabels[value]
  if (/(Time|At)$/.test(key) && (typeof value === 'string' || typeof value === 'number')) return formatDate(new Date(value))
  return businessValueLabel(key, value)
}
let loadedQuery: Record<string, any> | undefined
let querySequence = 0, detailSequence = 0
const assertPermission = () => {
  if (props.source.permissions.length && !hasPermission(props.source.permissions)) throw new Error('当前账号无权查询此日志来源')
}
const load = async () => {
  const sequence = ++querySequence
  ++detailSequence
  loadedQuery = undefined
  rows.value = []; total.value = 0; error.value = ''; detailVisible.value = false; detail.value = undefined
  loading.value = true
  try {
    assertPermission()
    for (const field of props.source.fields ?? []) {
      if (field.required && !String(query[field.key] ?? '').trim()) throw new Error(`请输入${field.label}`)
    }
    if (!props.source.read) throw new Error('未配置日志查询来源')
    const params = Object.fromEntries(Object.entries(query).filter(([, value]) => value !== '' && value != null))
    const result = await props.source.read(params)
    if (sequence !== querySequence) return
    rows.value = result.list; total.value = result.total; loadedQuery = { ...params }; queried.value = true
  } catch (cause) {
    if (sequence === querySequence) error.value = cause instanceof Error ? cause.message : '日志查询失败，请检查权限及服务状态'
  } finally { if (sequence === querySequence) loading.value = false }
}
const search = async () => { query.pageNo = 1; await load() }
const showDetail = async (row: Record<string, any>) => {
  const sequence = ++detailSequence, queryAtOpen = querySequence
  detail.value = undefined; detailVisible.value = false; error.value = ''
  try {
    assertPermission()
    const result = props.source.detail ? await props.source.detail(row, loadedQuery!) : row
    if (sequence !== detailSequence || queryAtOpen !== querySequence) return
    if (!result || typeof result !== 'object') throw new Error('日志详情不存在或不可见')
    detail.value = result; detailVisible.value = true
  } catch (cause) {
    if (sequence === detailSequence && queryAtOpen === querySequence) error.value = cause instanceof Error ? cause.message : '日志详情读取失败'
  }
}
onMounted(async () => {
  for (const field of props.source.fields ?? []) query[field.key] = typeof route.query[field.key] === 'string' ? route.query[field.key] : ''
  if ((props.source.fields ?? []).every(field => !field.required || query[field.key])) await load()
})
</script>
