<template>
  <div class="my-notify-message-list" :class="{ 'my-notify-message-list--embedded': embedded }">
    <ActiveOrderHandoffPanel
      v-if="canReadActiveOrderHandoffs"
      ref="handoffPanelRef"
      hide-when-empty
    />
    <component :is="contentWrapper" class="my-notify-message-list__section">
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <UnifiedListTemplate
        single-line-toolbar
        table-key="system.notify.my-message"
        :query-model="queryParams"
        label-width="68px"
        :filter-definitions="notifyQuickFilterDefinitions"
        :show-quick-filter-label="false"
        :quick-filter-state="notifyQuickFilter.state"
        :selected-filter-definition="notifyQuickFilter.selectedDefinition.value"
        :operator-options="notifyQuickFilter.operatorOptions.value"
        :columns="notifyColumns"
        :column-saving="notifyColumnSaving"
        :show-column-reset="false"
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @update:quick-filter-state="notifyQuickFilter.updateState"
        @quick-filter-query="handleQuery"
        @column-change="saveNotifyColumnConfig"
        @column-reset="resetNotifyColumnConfig"
        @pagination="getList"
        server-filter
        :server-filter-query="getList"
        :query-loading="loading"
        :column-filter-definitions="listColumnFilterDefinitions0"
      >
        <template #actions>
          <el-button :loading="loading" @click="getList">刷新</el-button>
          <el-button @click="handleUpdateAll">
            <Icon icon="ep:reading" class="mr-5px" /> 全部阅读
          </el-button>
        </template>

        <template #table="{ resolveRows }">
          <el-table
            ref="notifyTableRef"
            v-loading="loading"
            class="my-notify-message-list__table"
            data-user-table-column-explicit
            data-user-table-key="system.notify.my-message"
            :data="resolveRows(list)"
            row-key="rowKey"
            border
            :stripe="true"
            :show-overflow-tooltip="true"
            @header-dragend="handleNotifyHeaderDragend"
          >
            <el-table-column
              v-if="isNotifyColumnVisible('templateNickname')"
              label="发送人"
              align="center"
              prop="templateNickname"
              :width="getNotifyColumnWidthString('templateNickname', 180)"
            />
            <el-table-column
              v-if="isNotifyColumnVisible('createTime')"
              label="发送时间"
              align="center"
              prop="createTime"
              :width="getNotifyColumnWidthString('createTime', 200)"
            >
              <template #default="scope">
                {{
                  scope.row.kind === 'recovery'
                    ? '尚未送达'
                    : dateFormatter(scope.row, scope.column, scope.row.createTime)
                }}
              </template>
            </el-table-column>

            <el-table-column
              v-if="isNotifyColumnVisible('templateContent')"
              label="消息内容"
              align="center"
              prop="templateContent"
              :min-width="getNotifyColumnMinWidthString('templateContent', 280)"
            >
              <template #default="scope">
                <span
                  v-if="scope.row.kind === 'recovery'"
                  class="my-notify-message-list__content-text"
                >
                  {{ scope.row.templateContent }}
                </span>
                <template v-else>
                  <button
                    v-if="hasNotifyMessageTarget(scope.row)"
                    type="button"
                    class="my-notify-message-list__content-link"
                    role="link"
                    :title="scope.row.templateContent || '-'"
                    aria-label="打开消息内容"
                    @click.stop="handleNotifyContentClick(scope.row)"
                  >
                    <BusinessMessageContent
                      :description="describeNotifyMessage(scope.row)"
                      :show-context="false"
                    />
                  </button>
                  <span
                    v-else
                    class="my-notify-message-list__content-text"
                    :title="scope.row.templateContent || '-'"
                  >
                    <BusinessMessageContent
                      :description="describeNotifyMessage(scope.row)"
                      :show-context="false"
                    />
                  </span>
                  <BusinessMessageContext :fields="describeNotifyMessage(scope.row).fields" />
                </template>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isNotifyColumnVisible('readStatus')"
              label="消息状态"
              align="center"
              prop="readStatus"
              :width="getNotifyColumnWidthString('readStatus', 160)"
            >
              <template #default="scope">
                <el-tag v-if="scope.row.kind === 'recovery'" type="warning">待补发</el-tag>
                <el-tag v-else :type="scope.row.readStatus ? 'info' : 'success'">
                  {{ scope.row.readStatus ? '已读' : '未读' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
              v-if="isNotifyColumnVisible('operation')"
              label="操作"
              align="center"
              prop="operation"
              :width="getNotifyColumnWidthString('operation', 160)"
            >
              <template #default="scope">
                <ReleaseTaskNotificationEntry
                  v-if="scope.row.kind === 'recovery'"
                  :work-task-id="scope.row.workTaskId"
                  @delivery-change="handleDeliveryChange"
                  >查看回执并补发</ReleaseTaskNotificationEntry
                >
                <el-button
                  v-else
                  link
                  :type="scope.row.readStatus ? 'primary' : 'success'"
                  @click="openDetail(scope.row)"
                >
                  {{ scope.row.readStatus ? '详情' : '阅读' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </UnifiedListTemplate>
    </component>

    <!-- 表单弹窗：详情 -->
    <MyNotifyMessageDetail ref="detailRef" />
  </div>
</template>

<script lang="ts" setup>
import { buildListColumnFilters, filterListColumns, type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'

import type { TableInstance } from 'element-plus'
import { dateFormatter } from '@/utils/formatTime'
import * as NotifyMessageApi from '@/api/system/notify/message'
import MyNotifyMessageDetail from '../MyNotifyMessageDetail.vue'
import BusinessMessageContent from '@/components/BusinessMessageContent/BusinessMessageContent.vue'
import BusinessMessageContext from '@/components/BusinessMessageContent/BusinessMessageContext.vue'
import { describeNotifyMessage } from '@/utils/businessMessagePresentation'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import ActiveOrderHandoffPanel from '@/views/mes/pro/handoff/ActiveOrderHandoffPanel.vue'
import { hasPermission } from '@/directives/permission/hasPermi'
import { useUserStore } from '@/store/modules/user'
import { listOwnPendingReleaseOutcomes } from '@/api/mes/pro/productionRelease/notification'
import ReleaseTaskNotificationEntry from '@/views/mes/pro/production-release/components/ReleaseTaskNotificationEntry.vue'
import { loadInboxPage, toInboxMessages, toInboxRecoveries, type InboxRow } from '../notifyInbox'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  getNotifyMessageTarget,
  hasNotifyMessageTarget,
  navigateToNotifyMessageTarget
} from '@/utils/notifyMessageNavigation'

defineOptions({ name: 'MyNotifyMessageList' })

const emit = defineEmits<{
  (e: 'read-status-change'): void
}>()

const props = withDefaults(
  defineProps<{
    embedded?: boolean
    active?: boolean
  }>(),
  {
    embedded: false,
    active: true
  }
)

const message = useMessage() // 消息
const router = useRouter()
const userStore = useUserStore()
const canReadReleaseOutcomes = computed(() => hasPermission(['mes:pro-edhr-work-task:query']))
const notifyTableRef = ref<TableInstance>()
const handoffPanelRef = ref<{ refreshLayout: () => Promise<void> }>()
const refreshTableLayout = async () => {
  if (!props.active) return
  await nextTick()
  notifyTableRef.value?.doLayout()
  await handoffPanelRef.value?.refreshLayout()
}
watch(
  () => props.active,
  (active) => {
    if (active) void refreshTableLayout()
  },
  { flush: 'post' }
)

const NOTIFY_TABLE_KEY = 'system.notify.my-message'

const embedded = computed(() => props.embedded)
const contentWrapper = computed(() => (props.embedded ? 'div' : 'ContentWrap'))
const canReadActiveOrderHandoffs = computed(() =>
  hasPermission([
    'mes:pro-feedback:query',
    'mes:pro-process-pool-team-leader:query',
    'mes:pro-edhr-nonconformance-review:query',
    'mes:pro-edhr-work-task:query'
  ])
)

const notifyDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'templateNickname', label: '发送人', width: 180 },
  { key: 'createTime', label: '发送时间', width: 200 },
  { key: 'templateContent', label: '消息内容', minWidth: 280 },
  { key: 'readStatus', label: '消息状态', width: 160 },
  { key: 'operation', label: '操作', width: 160, hideable: false, business: false }
]
const {
  columns: notifyColumns,
  saving: notifyColumnSaving,
  isColumnVisible: isNotifyColumnVisible,
  getColumnWidthString: getNotifyColumnWidthString,
  getColumnMinWidthString: getNotifyColumnMinWidthString,
  handleHeaderDragend: handleNotifyHeaderDragend,
  saveConfig: saveNotifyColumnConfig,
  resetConfig: resetNotifyColumnConfig
} = useUserTableColumns(NOTIFY_TABLE_KEY, notifyDefaultColumns)

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref<InboxRow[]>([]) // 已送达站内信和本人待补发通知共用列表
const loadError = ref('')
let loadSequence = 0
const readTimeByMessageId = ref<Record<number, Date>>({})
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  listFilters: undefined as string | undefined,
  readStatus: undefined as boolean | string | undefined
})

const notifyQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'readStatus',
    label: '消息状态',
    type: 'select',
    queryParamKey: 'readStatus',
    options: [
      { label: '未读', value: false },
      { label: '已读', value: true },
      ...(canReadReleaseOutcomes.value ? [{ label: '待补发', value: 'pending' }] : [])
    ]
  }
])

const mergeReadTimeOverrides = (
  items: NotifyMessageApi.NotifyMessageVO[]
): NotifyMessageApi.NotifyMessageVO[] => {
  return items.map((item) => {
    const readTime = readTimeByMessageId.value[item.id]
    if (!readTime) {
      return item
    }
    return {
      ...item,
      readStatus: true,
      readTime: item.readTime || readTime
    }
  })
}

/** 查询列表 */
const getList = async () => {
  const sequence = ++loadSequence
  if (!props.active) return false
  loading.value = true
  loadError.value = ''
  list.value = []
  // 加载期间保留已知总数，避免分页器将目标页夹回第一页。
  const params = { ...queryParams }
  try {
    const pending =
      canReadReleaseOutcomes.value &&
      (params.readStatus === undefined || params.readStatus === 'pending')
        ? toInboxRecoveries(await listOwnPendingReleaseOutcomes())
        : []
    const conditions = params.listFilters ? JSON.parse(params.listFilters) : []
    const filteredPending = filterListColumns(pending, conditions,
      buildListColumnFilters(notifyDefaultColumns, listColumnFilterDefinitions0.value))
    const data = await loadInboxPage(filteredPending, params, async (pagination) => {
      const page = await NotifyMessageApi.getMyNotifyMessagePage(pagination)
      return { ...page, list: mergeReadTimeOverrides(page.list) }
    })
    if (sequence !== loadSequence) return false
    const lastPage = Math.max(1, Math.ceil(data.total / params.pageSize))
    if (params.pageNo > lastPage) {
      queryParams.pageNo = lastPage
      return await getList()
    }
    list.value = data.list
    total.value = data.total
    await refreshTableLayout()
    return sequence === loadSequence
  } catch (error) {
    if (sequence === loadSequence) {
      total.value = 0
      loadError.value = error instanceof Error ? error.message : String(error)
    }
    throw error
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}
const handleDeliveryChange = async () => {
  await getList()
  emit('read-status-change')
}
watch(
  () => [props.active, userStore.getUser.id, canReadReleaseOutcomes.value],
  (values, previous) => {
    if (previous && values[1] !== previous[1]) readTimeByMessageId.value = {}
    if (props.active) void getList()
    else {
      loadSequence += 1
      list.value = []
      total.value = 0
      loadError.value = ''
      loading.value = false
    }
  }
)
onBeforeUnmount(() => {
  loadSequence += 1
})
const notifyQuickFilter = useTableQuickFilter(
  NOTIFY_TABLE_KEY,
  notifyQuickFilterDefinitions,
  queryParams,
  getList
)

/** 快速过滤查询 */
const handleQuery = async () => {
  queryParams.pageNo = 1
  await notifyQuickFilter.applyQuickFilter()
}

/** 详情操作 */
const detailRef = ref()
const buildReadDetailData = (
  source: NotifyMessageApi.NotifyMessageVO,
  refreshed: NotifyMessageApi.NotifyMessageVO | undefined,
  readAt: Date
): NotifyMessageApi.NotifyMessageVO => {
  return {
    ...source,
    ...refreshed,
    readStatus: true,
    readTime: refreshed?.readTime || source.readTime || readAt
  }
}
const openDetail = async (data: NotifyMessageApi.NotifyMessageVO) => {
  if (!data.readStatus) {
    const { updatedData, readAt } = await handleReadOne(data.id)
    detailRef.value.open(buildReadDetailData(data, updatedData, readAt))
    return
  }
  detailRef.value.open(data)
}

/** 阅读一条站内信 */
const handleReadOne = async (
  id: number
): Promise<{ updatedData?: NotifyMessageApi.NotifyMessageVO; readAt: Date }> => {
  await NotifyMessageApi.updateNotifyMessageRead(id)
  const readAt = new Date()
  readTimeByMessageId.value = {
    ...readTimeByMessageId.value,
    [id]: readAt
  }
  await getList()
  emit('read-status-change')
  const updatedData = list.value.find((item) => item.kind === 'message' && item.id === id) as
    | NotifyMessageApi.NotifyMessageVO
    | undefined
  if (updatedData && !updatedData.readTime) {
    updatedData.readStatus = true
    updatedData.readTime = readAt
  }
  return { updatedData, readAt }
}

const handleNotifyContentClick = async (row: NotifyMessageApi.NotifyMessageVO) => {
  const target = getNotifyMessageTarget(row)
  if (!target) {
    return
  }
  if (!row.readStatus) {
    await NotifyMessageApi.updateNotifyMessageRead(row.id)
    emit('read-status-change')
  }
  await navigateToNotifyMessageTarget(router, target)
}

/** 阅读全部站内信 **/
const handleUpdateAll = async () => {
  await NotifyMessageApi.updateAllNotifyMessageRead()
  const readAt = new Date()
  readTimeByMessageId.value = list.value.reduce(
    (result, item) => {
      if (item.kind === 'message' && !item.readStatus) {
        result[item.id] = readAt
      }
      return result
    },
    { ...readTimeByMessageId.value }
  )
  message.success('全部阅读成功！')
  await getList()
  emit('read-status-change')
}

/** 初始化 **/
onMounted(getList)
let wasDeactivated = false
onDeactivated(() => {
  wasDeactivated = true
})
onActivated(() => {
  if (!wasDeactivated) return
  wasDeactivated = false
  return getList()
})


const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'readStatus',
    label: '消息状态',
    type: 'select',
    getValue: (row: InboxRow) => (row.kind === 'recovery' ? 'pending' : row.readStatus),
    options: [
      { label: '待补发', value: 'pending' },
      { label: '已读', value: true },
      { label: '未读', value: false }
    ]
  }
])
</script>

<style scoped>
.my-notify-message-list--embedded {
  width: 100%;
}

.my-notify-message-list__table {
  width: 100%;
}

.my-notify-message-list__content-link {
  display: block;
  width: 100%;
  max-width: 100%;
  padding: 0;
  color: #1677ff;
  font: inherit;
  text-align: left;
  white-space: normal;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.my-notify-message-list__content-text {
  display: block;
  width: 100%;
  max-width: 100%;
  text-align: left;
  white-space: normal;
}

.my-notify-message-list__content-link:hover,
.my-notify-message-list__content-link:focus-visible {
  color: #0958d9;
  text-decoration: underline;
  outline: none;
}
</style>
