<template>
  <div class="edhr-release-event-pane">
    <UnifiedListTemplate
      table-key="mes.pro.edhr.traceDrawer.releaseEvents"
      :query-model="queryParams"
      :filter-definitions="releaseEventQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="releaseEventQuickFilter.state"
      :selected-filter-definition="releaseEventQuickFilter.selectedDefinition.value"
      :operator-options="releaseEventQuickFilter.operatorOptions.value"
      :columns="releaseEventColumns"
      :column-saving="releaseEventColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="releaseEventQuickFilter.updateState"
      @quick-filter-query="releaseEventQuickFilter.applyQuickFilter"
      @column-change="saveReleaseEventColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button :loading="loading" type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-alert
          v-if="loadError"
          :title="loadError"
          type="error"
          :closable="false"
          show-icon
          class="edhr-release-event-pane__alert"
        />
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.edhr.traceDrawer.releaseEvents"
          :data="list"
          border
          stripe
          row-key="id"
          :show-overflow-tooltip="true"
          empty-text="暂无放行事务事件"
          @header-dragend="handleReleaseEventHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isReleaseEventColumnVisible('eventType')"
            label="事件"
            prop="eventType"
            :width="getReleaseEventColumnWidthString('eventType', 120)"
            v-bind="sortColumnAttrs('eventType')"
          >
            <template #default="{ row }">
              <el-tag>{{ resolveReleaseEventLabel(row.eventType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReleaseEventColumnVisible('statusChange')"
            label="状态变化"
            prop="statusChange"
            :min-width="getReleaseEventColumnMinWidthString('statusChange', 190)"
            v-bind="sortColumnAttrs({ key: 'statusChange', prop: 'toStatus' })"
          >
            <template #default="{ row }">
              {{ resolveReleaseStatusLabel(row.fromStatus) }} → {{ resolveReleaseStatusLabel(row.toStatus) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReleaseEventColumnVisible('reasonOpinion')"
            label="原因/意见"
            prop="reasonOpinion"
            :min-width="getReleaseEventColumnMinWidthString('reasonOpinion', 260)"
            v-bind="sortColumnAttrs('reasonOpinion')"
          >
            <template #default="{ row }">
              {{ row.reason || row.opinion || '--' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReleaseEventColumnVisible('idempotencyKey')"
            label="幂等键"
            prop="idempotencyKey"
            :min-width="getReleaseEventColumnMinWidthString('idempotencyKey', 230)"
            v-bind="sortColumnAttrs('idempotencyKey')"
          />
          <el-table-column
            v-if="isReleaseEventColumnVisible('signoffEvidenceHash')"
            label="签核证据"
            prop="signoffEvidenceHash"
            :min-width="getReleaseEventColumnMinWidthString('signoffEvidenceHash', 230)"
            v-bind="sortColumnAttrs('signoffEvidenceHash')"
          />
          <el-table-column
            v-if="isReleaseEventColumnVisible('actorUserId')"
            label="操作人"
            prop="actorUserId"
            :width="getReleaseEventColumnWidthString('actorUserId', 110)"
            v-bind="sortColumnAttrs('actorUserId')"
          >
            <template #default="{ row }">
              {{ row.actorUserId || '--' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReleaseEventColumnVisible('occurredAt')"
            label="发生时间"
            prop="occurredAt"
            :width="getReleaseEventColumnWidthString('occurredAt', 180)"
            v-bind="sortColumnAttrs('occurredAt')"
          >
            <template #default="{ row }">
              {{ formatDateTime(row.occurredAt) }}
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </div>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  getEdhrReleaseEventPage,
  type EdhrReleaseEventRespVO,
  type EdhrReleaseEventType
} from '@/api/mes/pro/edhr/release'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  resolveReleaseEventLabel,
  resolveReleaseStatusLabel
} from '@/views/mes/pro/edhr/shared/releaseCheckPresentation'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProEdhrReleaseEventListPane' })

const props = withDefaults(
  defineProps<{
    releaseTransactionId?: string | number
    autoLoad?: boolean
    pageSize?: number
  }>(),
  {
    autoLoad: true,
    pageSize: 10
  }
)

const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrReleaseEventRespVO[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNo: 1,
  pageSize: props.pageSize,
  eventType: '' as EdhrReleaseEventType | ''
})

const releaseEventDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'eventType', label: '事件', width: 120 },
  { key: 'statusChange', label: '状态变化', minWidth: 190 },
  { key: 'reasonOpinion', label: '原因/意见', minWidth: 260 },
  { key: 'idempotencyKey', label: '幂等键', minWidth: 230 },
  { key: 'signoffEvidenceHash', label: '签核证据', minWidth: 230 },
  { key: 'actorUserId', label: '操作人', width: 110 },
  { key: 'occurredAt', label: '发生时间', width: 180 }
]

const {
  columns: releaseEventColumns,
  saving: releaseEventColumnSaving,
  isColumnVisible: isReleaseEventColumnVisible,
  getColumnWidthString: getReleaseEventColumnWidthString,
  getColumnMinWidthString: getReleaseEventColumnMinWidthString,
  handleHeaderDragend: handleReleaseEventHeaderDragend,
  saveConfig: saveReleaseEventColumnConfig
} = useUserTableColumns('mes.pro.edhr.traceDrawer.releaseEvents', releaseEventDefaultColumns)

const releaseEventOptions: Array<{ label: string; value: EdhrReleaseEventType }> = [
  { label: '提交', value: 'SUBMIT' },
  { label: '批准', value: 'APPROVE' },
  { label: '驳回', value: 'REJECT' },
  { label: '撤回', value: 'WITHDRAW' }
]

const releaseEventQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'eventType',
    label: '事件类型',
    type: 'select',
    queryParamKey: 'eventType',
    options: releaseEventOptions
  }
]

const releaseEventQuickFilter = useTableQuickFilter(
  'mes.pro.edhr.traceDrawer.releaseEvents',
  releaseEventQuickFilterDefinitions,
  queryParams,
  getList
)

const parsePositiveJsonLong = (value: unknown) => {
  const normalized = typeof value === 'string' ? value.trim() : String(value || '').trim()
  return /^[1-9]\d*$/.test(normalized) ? normalized : undefined
}

const currentReleaseTransactionId = computed(() => parsePositiveJsonLong(props.releaseTransactionId))

const formatDateTime = (value?: string | number) => {
  return formatEdhrDateTime(value)
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

async function getList() {
  const releaseTransactionId = currentReleaseTransactionId.value
  if (!releaseTransactionId) {
    list.value = []
    total.value = 0
    loadError.value = ''
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrReleaseEventPage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      releaseTransactionId,
      eventType: queryParams.eventType || undefined
    })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '放行事务事件加载失败。')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = async () => {
  queryParams.pageNo = 1
  queryParams.pageSize = props.pageSize
  queryParams.eventType = ''
  await releaseEventQuickFilter.resetQuickFilter()
}

watch(
  () => [props.releaseTransactionId, props.autoLoad],
  async () => {
    if (props.autoLoad) {
      queryParams.pageNo = 1
      await getList()
    }
  },
  { immediate: true }
)

defineExpose({ reload: getList })
</script>

<style scoped>
.edhr-release-event-pane {
  min-height: 0;
}

.edhr-release-event-pane__alert {
  margin: 12px;
}

.edhr-release-event-pane :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-release-event-pane :deep(.el-table__row) {
  height: 52px;
}
</style>
