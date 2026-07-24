<template>
  <div class="my-notify-message-list" :class="{ 'my-notify-message-list--embedded': embedded }">
    <component :is="contentWrapper" class="my-notify-message-list__section">
      <UnifiedListTemplate
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
      >
        <template #actions>
          <el-button @click="handleUpdateAll">
            <Icon icon="ep:reading" class="mr-5px" /> 全部阅读
          </el-button>
        </template>

        <template #table>
          <el-table
            v-loading="loading"
            class="my-notify-message-list__table"
            data-user-table-column-explicit
            data-user-table-key="system.notify.my-message"
            :data="list"
            row-key="id"
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
              :formatter="dateFormatter"
            />
            <el-table-column
              v-if="isNotifyColumnVisible('templateType')"
              label="类型"
              align="center"
              prop="templateType"
              :width="getNotifyColumnWidthString('templateType', 180)"
            >
              <template #default="scope">
                <dict-tag
                  :type="DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE"
                  :value="scope.row.templateType"
                />
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
                <button
                  v-if="hasNotifyMessageTarget(scope.row)"
                  type="button"
                  class="my-notify-message-list__content-link"
                  role="link"
                  :title="scope.row.templateContent || '-'"
                  aria-label="打开消息内容"
                  @click.stop="handleNotifyContentClick(scope.row)"
                >
                  {{ scope.row.templateContent || '-' }}
                </button>
                <span
                  v-else
                  class="my-notify-message-list__content-text"
                  :title="scope.row.templateContent || '-'"
                >
                  {{ scope.row.templateContent || '-' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isNotifyColumnVisible('readStatus')"
              label="是否已读"
              align="center"
              prop="readStatus"
              :width="getNotifyColumnWidthString('readStatus', 160)"
            >
              <template #default="scope">
                <dict-tag :type="DICT_TYPE.INFRA_BOOLEAN_STRING" :value="scope.row.readStatus" />
              </template>
            </el-table-column>
            <el-table-column
              v-if="isNotifyColumnVisible('readTime')"
              label="阅读时间"
              align="center"
              prop="readTime"
              :width="getNotifyColumnWidthString('readTime', 200)"
              :formatter="dateFormatter"
            />
            <el-table-column
              v-if="isNotifyColumnVisible('operation')"
              label="操作"
              align="center"
              prop="operation"
              :width="getNotifyColumnWidthString('operation', 160)"
            >
              <template #default="scope">
                <el-button
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
import { DICT_TYPE, getBoolDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as NotifyMessageApi from '@/api/system/notify/message'
import MyNotifyMessageDetail from '../MyNotifyMessageDetail.vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
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
  }>(),
  {
    embedded: false
  }
)

const message = useMessage() // 消息
const router = useRouter()

const NOTIFY_TABLE_KEY = 'system.notify.my-message'

const embedded = computed(() => props.embedded)
const contentWrapper = computed(() => (props.embedded ? 'div' : 'ContentWrap'))

const notifyDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'templateNickname', label: '发送人', width: 180 },
  { key: 'createTime', label: '发送时间', width: 200 },
  { key: 'templateType', label: '类型', width: 180 },
  { key: 'templateContent', label: '消息内容', minWidth: 280 },
  { key: 'readStatus', label: '是否已读', width: 160 },
  { key: 'readTime', label: '阅读时间', width: 200 },
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
const list = ref<NotifyMessageApi.NotifyMessageVO[]>([]) // 列表的数据
const readTimeByMessageId = ref<Record<number, Date>>({})
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  readStatus: undefined
})

const notifyQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'readStatus',
    label: '是否已读',
    type: 'select',
    queryParamKey: 'readStatus',
    options: getBoolDictOptions(DICT_TYPE.INFRA_BOOLEAN_STRING).map((dict) => ({
      label: dict.label,
      value: dict.value as string | number | boolean
    }))
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
  loading.value = true
  try {
    const data = await NotifyMessageApi.getMyNotifyMessagePage(queryParams)
    list.value = mergeReadTimeOverrides(data.list)
    total.value = data.total
  } finally {
    loading.value = false
  }
}
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
  const updatedData = list.value.find((item) => item.id === id)
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
      if (!item.readStatus) {
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
onMounted(() => {
  getList()
})
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
  overflow: hidden;
  color: #1677ff;
  font: inherit;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.my-notify-message-list__content-text {
  display: block;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.my-notify-message-list__content-link:hover,
.my-notify-message-list__content-link:focus-visible {
  color: #0958d9;
  text-decoration: underline;
  outline: none;
}
</style>
