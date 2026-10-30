<template>
  <el-card class="profile-erp-table-sync" shadow="never">
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
    <el-alert v-if="latestRunsError" :title="latestRunsError" type="error" :closable="false" />

    <el-form
      class="profile-erp-table-sync__form"
      label-width="120px"
      :disabled="loading || saving || connectionSaving"
    >
      <div class="profile-erp-table-sync__top-settings">
        <div class="profile-erp-table-sync__schedule-settings">
          <el-form-item label="启用自动同步">
            <el-switch v-model="form.enabled" :disabled="!canSaveSchedule" />
          </el-form-item>
          <el-form-item label="每日开始时间">
            <el-time-picker
              v-model="form.dailyStartTime"
              :disabled="!canSaveSchedule"
              value-format="HH:mm:ss"
              format="HH:mm:ss"
              :clearable="false"
            />
            <span
              v-if="jobsLoaded && !uniformDailyStartTime"
              class="ml-3 text-[12px] text-orange-500"
            >
              当前任务计划不一致，保存后统一使用所选时间
            </span>
          </el-form-item>
        </div>

        <div
          v-show="activeList === 'configuration'"
          class="profile-erp-table-sync__schedule-actions"
        >
          <el-button
            type="primary"
            :loading="saving"
            :disabled="!canSaveSchedule"
            @click="handleSave"
            >保存配置</el-button
          >
          <el-button
            type="primary"
            plain
            :loading="running"
            :disabled="
              !canRunOnce ||
              selectedSyncTypes.length === 0 ||
              Boolean(incrementalSyncingType) ||
              Boolean(fullSyncingType)
            "
            @click="handleRunOnce"
          >
            <Icon icon="ep:refresh-right" class="mr-5px" /> 增量执行一次
          </el-button>
        </div>

        <section class="profile-erp-table-sync__connection-setting" aria-label="ERP连接">
          <div class="profile-erp-table-sync__connection-status">
            <span class="profile-erp-table-sync__connection-label">当前连接</span>
            <el-tag
              v-if="currentConnectionType"
              :type="currentConnectionType === 'PRODUCTION' ? 'danger' : 'success'"
              effect="plain"
            >
              {{ currentConnectionName }}
            </el-tag>
            <el-tag v-if="connectionDirty" type="warning" effect="plain">待保存</el-tag>
          </div>
          <div class="profile-erp-table-sync__connection-controls">
            <el-segmented
              v-model="selectedConnectionType"
              :options="segmentedConnectionOptions"
              :disabled="connectionOptions.length !== 2"
              aria-label="选择 ERP 连接"
            />
            <el-button
              class="profile-erp-table-sync__connection-save"
              type="primary"
              :loading="connectionSaving"
              :disabled="!canSaveConnection || !connectionDirty"
              @click="handleSaveConnection"
            >
              <Icon icon="ep:check" />
              保存连接
            </el-button>
          </div>
        </section>
      </div>
    </el-form>
    <el-tabs v-model="activeList" class="erp-list-tabs">
      <el-tab-pane label="同步对象" name="configuration" />
      <el-tab-pane label="运行中任务" name="running" />
    </el-tabs>
    <div class="profile-erp-table-sync__progress-toolbar">
      <span>
        页面可见时每 5 秒刷新；最近刷新：{{ progressUpdatedAtLabel }}
        <span v-if="progressRefreshError" class="text-red-500">（刷新失败）</span>
      </span>
      <el-button :loading="runLoading || runningJobLoading" @click="refreshVisibleRunProgress(false)">
        刷新进度
      </el-button>
    </div>
    <el-alert
      title="已处理行数包含新增、更新和跳过；待统计表示尚未上报数量。任务失败不等于已统计出失败行数。"
      type="info"
      :closable="false"
      show-icon
      class="mb-3"
    />
    <fieldset
      v-show="activeList === 'configuration'"
      :disabled="loading || saving || connectionSaving"
      style="border: 0; padding: 0; min-width: 0"
    >
      <div class="profile-erp-table-sync__list">
        <ErpLocalList
          table-key="erp.sync.configuration"
          :rows="syncTableRows"
          :default-columns="syncConfigurationColumns"
          :filter-definitions="syncConfigurationFilters"
        >
          <template #default="{ rows, isColumnVisible, getColumnWidthString, handleHeaderDragend }">
            <el-table
              ref="syncTableRef"
              v-loading="jobLoading || runLoading"
              :data="rows"
              row-key="syncType"
              class="profile-erp-table-sync__select-table"
              border
              @selection-change="handleSyncTableSelectionChange"
              data-user-table-column-explicit
              data-user-table-key="erp.sync.configuration"
              @header-dragend="handleHeaderDragend"
              height="100%"
            >
              <el-table-column
                type="selection"
                reserve-selection
                width="48"
                :selectable="isSyncRowSelectable"
              />
              <el-table-column
                v-if="isColumnVisible('erpTableName')"
                column-key="erpTableName"
                prop="erpTableName"
                label="ERP表格名称"
                :min-width="getColumnWidthString('erpTableName', 180)"
              />
              <el-table-column
                v-if="isColumnVisible('localTabName')"
                column-key="localTabName"
                prop="localTabName"
                label="本地页签名称"
                :min-width="getColumnWidthString('localTabName', 220)"
              />
              <el-table-column
                v-if="isColumnVisible('cronExpression')"
                column-key="cronExpression"
                label="当前计划（CRON）"
                :min-width="getColumnWidthString('cronExpression', 180)"
              >
                <template #default="{ row }">
                  {{ jobsByHandlerName[row.handlerName]?.cronExpression || '未加载' }}
                </template>
              </el-table-column>
              <el-table-column
                v-if="isColumnVisible('latestRunTime')"
                column-key="latestRunTime"
                label="最近执行时间"
                :min-width="getColumnWidthString('latestRunTime', 180)"
              >
                <template #default="{ row }">{{ resolveLatestRunTime(row.latestRun) }}</template>
              </el-table-column>
              <el-table-column
                v-if="isColumnVisible('createdCount')"
                column-key="createdCount"
                label="新增行数"
                :min-width="getColumnWidthString('createdCount', 110)"
              >
                <template #default="{ row }">{{ resolveRunCount(row.latestRun, 'createdCount') }}</template>
              </el-table-column>
              <el-table-column
                v-if="isColumnVisible('processedCount')"
                column-key="processedCount"
                label="已处理行数"
                :min-width="getColumnWidthString('processedCount', 110)"
              >
                <template #default="{ row }">{{ resolveProcessedCount(row.latestRun) }}</template>
              </el-table-column>
              <el-table-column
                v-if="isColumnVisible('updatedCount')"
                column-key="updatedCount"
                label="更新行数"
                :min-width="getColumnWidthString('updatedCount', 110)"
              >
                <template #default="{ row }">{{ resolveRunCount(row.latestRun, 'updatedCount') }}</template>
              </el-table-column>
              <el-table-column
                v-if="isColumnVisible('skippedCount')"
                column-key="skippedCount"
                label="跳过行数"
                :min-width="getColumnWidthString('skippedCount', 110)"
              >
                <template #default="{ row }">{{ resolveRunCount(row.latestRun, 'skippedCount') }}</template>
              </el-table-column>
              <el-table-column
                v-if="isColumnVisible('failedCount')"
                column-key="failedCount"
                label="失败行数"
                :min-width="getColumnWidthString('failedCount', 110)"
              >
                <template #default="{ row }">{{ resolveRunCount(row.latestRun, 'failedCount') }}</template>
              </el-table-column>
              <el-table-column
                v-if="isColumnVisible('latestStatus')"
                column-key="latestStatus"
                label="同步成功/失败"
                :min-width="getColumnWidthString('latestStatus', 140)"
              >
                <template #default="{ row }">
                  <el-tag :type="resolveLatestSyncStatusTagType(row.latestRun)">
                    {{ formatLatestSyncStatus(row.latestRun) }}
                  </el-tag>
                  <div class="profile-erp-table-sync__progress-hint">{{ describeSyncRunProgress(row.latestRun) }}</div>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isColumnVisible('failureMessage')"
                column-key="failureMessage"
                label="失败原因"
                show-overflow-tooltip
                :min-width="getColumnWidthString('failureMessage', 220)"
              >
                <template #default="{ row }">{{ resolveFailureReason(row.latestRun) }}</template>
              </el-table-column>
              <el-table-column
                column-key="actions"
                label="操作"
                fixed="right"
                :width="getColumnWidthString('actions', 190)"
              >
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    plain
                    :loading="incrementalSyncingType === row.syncType"
                    :disabled="
                      !canRunSync ||
                      running ||
                      Boolean(incrementalSyncingType) ||
                      Boolean(fullSyncingType)
                    "
                    @click="handleRunIncremental(row)"
                  >
                    <Icon icon="ep:refresh-right" class="mr-5px" /> 增量同步
                  </el-button>
                  <el-button
                    type="warning"
                    link
                    :loading="fullSyncingType === row.syncType"
                    :disabled="
                      !canRunSync ||
                      running ||
                      Boolean(incrementalSyncingType) ||
                      Boolean(fullSyncingType)
                    "
                    @click="handleRunFull(row)"
                  >
                    全量同步
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </ErpLocalList>
      </div>
    </fieldset>
    <div v-show="activeList === 'running'" class="profile-erp-table-sync__running-jobs">
      <el-alert v-if="runningRunsError" :title="runningRunsError" type="error" :closable="false" />
      <div class="profile-erp-table-sync__running-header">
        <div class="profile-erp-table-sync__running-title">正在进行的同步 Job</div>
        <div class="profile-erp-table-sync__running-subtitle"
          >显示运行记录标记为运行中的 ERP 任务，结束后自动移出此列表。</div
        >
      </div>
      <ErpLocalList
        table-key="erp.sync.running"
        :rows="runningSyncRuns"
        :default-columns="runningJobsColumns"
        :filter-definitions="runningJobsFilters"
      >
        <template #default="{ rows, isColumnVisible, getColumnWidthString, handleHeaderDragend }">
          <el-table
            v-loading="runningJobLoading"
            :data="rows"
            row-key="id"
            class="profile-erp-table-sync__running-table"
            border
            :empty-text="runningRunsLoaded ? '暂无正在进行的同步 Job' : '运行事实未知'"
            data-user-table-column-explicit
            data-user-table-key="erp.sync.running"
            @header-dragend="handleHeaderDragend"
            height="100%"
          >
            <el-table-column
              v-if="isColumnVisible('col0')"
              column-key="col0"
              label="ERP表格名称"
              :min-width="getColumnWidthString('col0', 180)"
            >
              <template #default="{ row }">{{ resolveSyncTypeName(row.syncType) }}</template>
            </el-table-column>
            <el-table-column
              v-if="isColumnVisible('col1')"
              column-key="col1"
              label="开始时间"
              :min-width="getColumnWidthString('col1', 180)"
            >
              <template #default="{ row }">{{ formatDateTimeValue(row.startedAt, '-') }}</template>
            </el-table-column>
            <el-table-column
              v-if="isColumnVisible('processedCount')"
              column-key="processedCount"
              label="已处理行数"
              :min-width="getColumnWidthString('processedCount', 110)"
            >
              <template #default="{ row }">{{ resolveProcessedCount(row) }}</template>
            </el-table-column>
            <el-table-column
              v-if="isColumnVisible('skippedCount')"
              column-key="skippedCount"
              label="跳过行数"
              :min-width="getColumnWidthString('skippedCount', 110)"
            >
              <template #default="{ row }">{{ resolveRunCount(row, 'skippedCount') }}</template>
            </el-table-column>
            <el-table-column
              v-if="isColumnVisible('progressStatus')"
              column-key="progressStatus"
              label="统计状态"
              :min-width="getColumnWidthString('progressStatus', 230)"
            >
              <template #default="{ row }">{{ describeSyncRunProgress(row) }}</template>
            </el-table-column>
            <el-table-column
              v-if="isColumnVisible('col2')"
              column-key="col2"
              label="新增行数"
              :min-width="getColumnWidthString('col2', 110)"
            >
              <template #default="{ row }">{{ resolveRunCount(row, 'createdCount') }}</template>
            </el-table-column>
            <el-table-column
              v-if="isColumnVisible('col3')"
              column-key="col3"
              label="更新行数"
              :min-width="getColumnWidthString('col3', 110)"
            >
              <template #default="{ row }">{{ resolveRunCount(row, 'updatedCount') }}</template>
            </el-table-column>
            <el-table-column
              v-if="isColumnVisible('col4')"
              column-key="col4"
              label="失败行数"
              :min-width="getColumnWidthString('col4', 110)"
            >
              <template #default="{ row }">{{ resolveRunCount(row, 'failedCount') }}</template>
            </el-table-column>
            <el-table-column label="中断处理" fixed="right" :min-width="180">
              <template #default="{ row }">
                <el-button
                  link
                  type="danger"
                  :disabled="!canRunSync"
                  :loading="recoveringRunId === row.id"
                  @click="handleRecoverRun(row)"
                >核验并结束中断记录</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </ErpLocalList>
    </div>
  </el-card>
</template>
<script setup lang="ts">
const activeList = ref('configuration')
import { formatErpListInteger } from '@/views/erp/utils/integerDisplay'
import { useDocumentVisibility, useTimeoutPoll } from '@vueuse/core'
import { describeSyncRunProgress, getSyncRunCount, getSyncRunProcessedCount } from '../../syncRunProgress'
import type { SyncRunCountKey } from '../../syncRunProgress'
import ErpLocalList from '@/views/erp/components/ErpLocalList.vue'
import type { UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import type { TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

const syncConfigurationColumns: UserTableColumnDefinition[] = [
  {
    key: 'erpTableName',
    label: 'ERP表格名称',
    width: 180,
    sortable: false
  },
  {
    key: 'localTabName',
    label: '本地页签名称',
    width: 220,
    sortable: false
  },
  {
    key: 'cronExpression',
    label: '当前计划（CRON）',
    width: 180,
    sortable: false
  },
  {
    key: 'latestRunTime',
    label: '最近执行时间',
    width: 180,
    sortable: false
  },
  {
    key: 'createdCount',
    label: '新增行数',
    width: 110,
    sortable: false
  },
  { key: 'processedCount', label: '已处理行数', width: 110, sortable: false },
  { key: 'updatedCount', label: '更新行数', width: 110, sortable: false },
  { key: 'skippedCount', label: '跳过行数', width: 110, sortable: false },
  { key: 'failedCount', label: '失败行数', width: 110, sortable: false },
  {
    key: 'latestStatus',
    label: '同步成功/失败',
    width: 140,
    sortable: false
  },
  {
    key: 'failureMessage',
    label: '失败原因',
    width: 220,
    sortable: false
  }
]
const syncConfigurationFilters: TableQuickFilterDefinition[] = [
  {
    key: 'erpTableName',
    label: 'ERP表格名称',
    type: 'text',
    queryParamKey: 'erpTableName',
    operators: ['contains']
  },
  {
    key: 'localTabName',
    label: '本地页签名称',
    type: 'text',
    queryParamKey: 'localTabName',
    operators: ['contains']
  }
]

const runningJobsColumns: UserTableColumnDefinition[] = [
  { key: 'processedCount', label: '已处理行数', width: 110, sortable: false },
  { key: 'skippedCount', label: '跳过行数', width: 110, sortable: false },
  { key: 'progressStatus', label: '统计状态', width: 230, sortable: false },
  {
    key: 'col0',
    label: 'ERP表格名称',
    width: 180,
    sortable: false
  },
  {
    key: 'col1',
    label: '开始时间',
    width: 180,
    sortable: false
  },
  {
    key: 'col2',
    label: '新增行数',
    width: 110,
    sortable: false
  },
  {
    key: 'col3',
    label: '更新行数',
    width: 110,
    sortable: false
  },
  {
    key: 'col4',
    label: '失败行数',
    width: 110,
    sortable: false
  }
]
const runningJobsFilters: TableQuickFilterDefinition[] = []

import { ElMessage, ElMessageBox } from 'element-plus'
import { hasErpConfigCapability } from '@/utils/businessConfigAccess'
import { ErpKingdeeConfigApi } from '@/api/erp/config'
import type {
  ErpKingdeeActiveConnectionVO,
  ErpKingdeeConnectionOptionVO,
  ErpKingdeeConnectionType
} from '@/api/erp/config'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'
import type { ErpKingdeeSyncRunVO } from '@/api/erp/sync'
import * as JobApi from '@/api/infra/job'
import { InfraJobStatusEnum } from '@/utils/constants'
import { formatDateTimeValue, toDateTimeValue } from '@/utils/formatTime'

interface ProfileErpSyncType {
  syncType: string
  erpTableName: string
  localTabName: string
  handlerName: string
}

interface ProfileErpTableSyncForm {
  enabled: boolean
  dailyStartTime: string
}

interface ProfileErpSyncTableRow extends ProfileErpSyncType {
  latestRun?: ErpKingdeeSyncRunVO
}

defineOptions({ name: 'TableAutoSyncSetting' })

const canSaveSchedule = computed(() =>
  hasErpConfigCapability(['infra:job:query', 'infra:job:update', 'erp:kingdee-sync:query'])
)
const canRunSync = computed(() => hasErpConfigCapability(['erp:kingdee-sync:query']))
const canRunOnce = computed(() =>
  hasErpConfigCapability(['infra:job:query', 'erp:kingdee-sync:query'])
)
const canSaveConnection = computed(() => hasErpConfigCapability([]))
const requireCapability = (allowed: boolean) => {
  if (allowed) return true
  ElMessage.error('无权执行此配置操作，请检查对应 Job/ERP 权限。')
  return false
}

const RUNNING_SYNC_STATUS = 10
const RUNNING_SYNC_RUN_PAGE_SIZE = 20
const SUBMITTED_RUN_POLL_INTERVAL_MS = 1000
const SUBMITTED_RUN_POLL_ATTEMPTS = 10
const REQUIRED_CONNECTION_OPTIONS: Record<ErpKingdeeConnectionType, string> = {
  TEST: '测试账套',
  PRODUCTION: '正式账套'
}
const syncTypes: ProfileErpSyncType[] = [
  {
    syncType: 'PRODUCT',
    erpTableName: '物料列表',
    localTabName: '物料',
    handlerName: 'kingdeeProductItemSyncJob'
  },
  {
    syncType: 'STOCK',
    erpTableName: 'ERP 库存',
    localTabName: '即时库存',
    handlerName: 'kingdeeStockSyncJob'
  },
  {
    syncType: 'STOCK_MOVE',
    erpTableName: '金蝶调拨单',
    localTabName: '金蝶调拨单',
    handlerName: 'kingdeeStockMoveSyncJob'
  },
  {
    syncType: 'PURCHASE_ORDER',
    erpTableName: '采购订单',
    localTabName: '采购订单',
    handlerName: 'kingdeePurchaseOrderSyncJob'
  },
  {
    syncType: 'SALE_ORDER',
    erpTableName: '销售订单',
    localTabName: '销售订单',
    handlerName: 'kingdeeSaleOrderSyncJob'
  },
  {
    syncType: 'PRODUCTION_ORDER',
    erpTableName: '生产工单',
    localTabName: '生产订单',
    handlerName: 'kingdeeProductionOrderSyncJob'
  },
  {
    syncType: 'PRODUCTION_PICK_LIST',
    erpTableName: '生产领料单列表',
    localTabName: '生产领料单',
    handlerName: 'kingdeeProductionPickListSyncJob'
  },
  {
    syncType: 'PRODUCTION_REPLENISHMENT_LIST',
    erpTableName: '生产补料单列表',
    localTabName: '生产补料单',
    handlerName: 'kingdeeProductionReplenishmentListSyncJob'
  },
  {
    syncType: 'PRODUCTION_MATERIAL_LIST',
    erpTableName: '生产用料清单',
    localTabName: '生产用料清单',
    handlerName: 'kingdeeProductionMaterialListSyncJob'
  },
  {
    syncType: 'BOM',
    erpTableName: '产品 BOM',
    localTabName: '物料清单',
    handlerName: 'kingdeeBomSyncJob'
  }
]

const loading = ref(false)
const jobLoading = ref(false)
const saving = ref(false)
const connectionSaving = ref(false)
const running = ref(false)
const runLoading = ref(false)
const runningJobLoading = ref(false)
const incrementalSyncingType = ref('')
const fullSyncingType = ref('')
const loadError = ref('')
const jobsLoaded = ref(false)
const latestRunsLoaded = ref(false)
const latestRunsError = ref('')
const runningRunsLoaded = ref(false)
const runningRunsError = ref('')
const latestRunsUpdatedAt = ref<number>()
const runningRunsUpdatedAt = ref<number>()
const progressRefreshError = computed(() =>
  activeList.value === 'running' ? runningRunsError.value : latestRunsError.value
)
const progressUpdatedAtLabel = computed(() => {
  const timestamp = activeList.value === 'running' ? runningRunsUpdatedAt.value : latestRunsUpdatedAt.value
  return timestamp === undefined ? '尚未成功读取' : formatDateTimeValue(timestamp, '-')
})
const latestRuns = ref<ErpKingdeeSyncRunVO[]>([])
const runningSyncRuns = ref<ErpKingdeeSyncRunVO[]>([])
const recoveringRunId = ref<number>()
const handleRecoverRun = async (row: ErpKingdeeSyncRunVO) => {
  if (!requireCapability(canRunSync.value)) return
  try {
    await ElMessageBox.confirm(
      '将检查整个 Quartz 集群。仅在没有该任务执行实例时，才将此记录标记为中断失败并保留已提交计数；仍在执行的任务会被拒绝。',
      '核验中断记录',
      { type: 'warning', confirmButtonText: '核验并结束', cancelButtonText: '取消' }
    )
  } catch (action) {
    if (action === 'cancel' || action === 'close') return
    throw action
  }
  recoveringRunId.value = row.id
  try {
    await ErpKingdeeSyncApi.recoverRun(row.id)
    ElMessage.success('已结束中断记录，已提交计数保留；可在同步对象中重新发起全量同步')
    await Promise.all([loadLatestRuns(), loadRunningSyncRuns()])
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '中断记录核验失败'))
  } finally {
    recoveringRunId.value = undefined
  }
}
const jobsByHandlerName = ref<Record<string, JobApi.JobVO>>({})
const selectedSyncTypes = ref<string[]>([])
const syncTableRef = ref()
const syncingTableSelection = ref(false)
const currentConnectionType = ref<ErpKingdeeConnectionType | ''>('')
const currentConnectionName = ref('')
const selectedConnectionType = ref<ErpKingdeeConnectionType | ''>('')
const connectionOptions = ref<ErpKingdeeConnectionOptionVO[]>([])
const form = reactive<ProfileErpTableSyncForm>({
  enabled: false,
  dailyStartTime: '02:00:00'
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message.trim()) return error.message
  const responseData =
    typeof error === 'object' && error !== null && 'response' in error
      ? (error as { response?: { data?: { msg?: unknown; message?: unknown } } }).response?.data
      : undefined
  const responseMessage = responseData?.msg || responseData?.message
  return typeof responseMessage === 'string' && responseMessage.trim()
    ? responseMessage
    : defaultMessage
}

const padTimePart = (value: string | number) => String(value).padStart(2, '0')

const toDailyCronExpression = (time: string) => {
  if (!/^\d{2}:\d{2}:\d{2}$/.test(time)) return ''
  const [hour, minute, second] = time.split(':').map((part) => Number(part))
  if (hour > 23 || minute > 59 || second > 59) return ''
  return `${second} ${minute} ${hour} * * ?`
}

const parseDailyCronExpression = (cronExpression?: string) => {
  const match = cronExpression?.trim().match(/^(\d{1,2})\s+(\d{1,2})\s+(\d{1,2})\s+\*\s+\*\s+\?$/)
  if (!match) return ''
  const [, second, minute, hour] = match
  const time = `${padTimePart(hour)}:${padTimePart(minute)}:${padTimePart(second)}`
  return toDailyCronExpression(time) ? time : ''
}

const dailyCronExpression = computed(() => toDailyCronExpression(form.dailyStartTime))
const segmentedConnectionOptions = computed(() =>
  connectionOptions.value.map((item) => ({
    label: item.connectionName,
    value: item.connectionType
  }))
)
const connectionDirty = computed(
  () =>
    Boolean(currentConnectionType.value) &&
    Boolean(selectedConnectionType.value) &&
    currentConnectionType.value !== selectedConnectionType.value
)
const latestRunBySyncType = computed<Record<string, ErpKingdeeSyncRunVO | undefined>>(() =>
  latestRuns.value.reduce<Record<string, ErpKingdeeSyncRunVO | undefined>>((acc, item) => {
    if (!acc[item.syncType]) {
      acc[item.syncType] = item
    }
    return acc
  }, {})
)
const syncTableRows = computed<ProfileErpSyncTableRow[]>(() =>
  syncTypes.map((item) => ({
    ...item,
    latestRun: latestRunBySyncType.value[item.syncType]
  }))
)

const isSyncRowSelectable = () =>
  !loading.value && !saving.value && !incrementalSyncingType.value && !fullSyncingType.value

const formatLatestSyncStatus = (latestRun?: ErpKingdeeSyncRunVO) => {
  if (!latestRunsLoaded.value) return latestRunsError.value ? '运行事实查询失败' : '运行事实未知'
  if (!latestRun) return '未执行'
  if (latestRun.status === 20) return '成功'
  if (latestRun.status === 30) return '失败'
  if (latestRun.status === 10) return '运行中'
  return `未知状态（${latestRun.status}）`
}

const resolveLatestSyncStatusTagType = (latestRun?: ErpKingdeeSyncRunVO) => {
  if (!latestRun) return 'info'
  if (latestRun.status === 20) return 'success'
  if (latestRun.status === 30) return 'danger'
  if (latestRun.status === 10) return 'warning'
  return 'info'
}

const resolveLatestRunTime = (latestRun?: ErpKingdeeSyncRunVO) => {
  if (!latestRun) return '-'
  return formatDateTimeValue(latestRun.endedAt || latestRun.startedAt, '-')
}

const resolveFailureReason = (latestRun?: ErpKingdeeSyncRunVO) => {
  if (!latestRun || latestRun.status !== 30) return '-'
  const failureMessage = latestRun.failureMessage?.trim()
  return failureMessage || '-'
}

const resolveSyncTypeName = (syncType: string) =>
  syncTypes.find((item) => item.syncType === syncType)?.erpTableName || `未知ERP表格（${syncType}）`

const resolveRunCount = (run: ErpKingdeeSyncRunVO | undefined, key: SyncRunCountKey) => {
  const value = getSyncRunCount(run, key)
  return typeof value === 'number' ? formatErpListInteger(value) : value
}

const resolveProcessedCount = (run?: ErpKingdeeSyncRunVO) => {
  const value = getSyncRunProcessedCount(run)
  return typeof value === 'number' ? formatErpListInteger(value) : value
}

const syncSelectedRows = async () => {
  await nextTick()
  const table = syncTableRef.value
  if (!table) return
  syncingTableSelection.value = true
  try {
    table.clearSelection()
    const selectedSyncTypeSet = new Set(selectedSyncTypes.value)
    syncTableRows.value.forEach((row) => {
      table.toggleRowSelection(row, selectedSyncTypeSet.has(row.syncType))
    })
  } finally {
    await nextTick()
    syncingTableSelection.value = false
  }
}

const handleSyncTableSelectionChange = (rows: ProfileErpSyncTableRow[]) => {
  if (syncingTableSelection.value || loading.value || jobLoading.value || saving.value) {
    return
  }
  selectedSyncTypes.value = rows.map((row) => row.syncType)
}

const fetchJobByHandlerName = async (handlerName: string) => {
  const page = await JobApi.getJobPage({
    pageNo: 1,
    pageSize: 1,
    handlerName
  } as PageParam & { handlerName: string })
  const job = (page.list || []).find((item: JobApi.JobVO) => item.handlerName === handlerName)
  if (!job) {
    throw new Error(`未找到同步任务处理器：${handlerName}`)
  }
  return job
}

const uniformDailyStartTime = computed(() => {
  const times = Object.values(jobsByHandlerName.value).map((job) =>
    parseDailyCronExpression(job.cronExpression)
  )
  return times.length && times.every((time) => time && time === times[0]) ? times[0] : ''
})

const loadJobs = async () => {
  jobsLoaded.value = false
  jobsByHandlerName.value = {}
  form.dailyStartTime = '02:00:00'
  jobLoading.value = true
  try {
    const entries = await Promise.all(
      syncTypes.map(
        async (item) => [item.handlerName, await fetchJobByHandlerName(item.handlerName)] as const
      )
    )
    const nextJobsByHandlerName = Object.fromEntries(entries)
    jobsByHandlerName.value = nextJobsByHandlerName
    selectedSyncTypes.value = syncTypes
      .filter(
        (item) => nextJobsByHandlerName[item.handlerName].status === InfraJobStatusEnum.NORMAL
      )
      .map((item) => item.syncType)
    form.enabled = selectedSyncTypes.value.length > 0
    form.dailyStartTime = uniformDailyStartTime.value || '02:00:00'
    jobsLoaded.value = true
  } finally {
    jobLoading.value = false
    await syncSelectedRows()
  }
}

let latestRunsRequest: Promise<void> | undefined
const loadLatestRuns = (background = false): Promise<void> => {
  if (latestRunsRequest) return latestRunsRequest
  latestRunsRequest = fetchLatestRuns(background).finally(() => {
    latestRunsRequest = undefined
  })
  return latestRunsRequest
}
const fetchLatestRuns = async (background: boolean) => {
  runLoading.value = !background
  try {
    const latestRunList = await Promise.all(
      syncTypes.map(async (item) => {
        const page = await ErpKingdeeSyncApi.getRunPage({
          pageNo: 1,
          pageSize: 1,
          syncType: item.syncType
        } as PageParam & { syncType: string })
        if (!Array.isArray(page?.list)) throw new Error('同步运行结果返回格式无效')
        return page.list[0] as ErpKingdeeSyncRunVO | undefined
      })
    )
    latestRuns.value = latestRunList.filter((item): item is ErpKingdeeSyncRunVO => Boolean(item))
    latestRunsLoaded.value = true
    latestRunsError.value = ''
    latestRunsUpdatedAt.value = Date.now()
  } catch (error) {
    latestRuns.value = []
    latestRunsLoaded.value = false
    latestRunsUpdatedAt.value = undefined
    latestRunsError.value = resolveErrorMessage(error, '同步运行结果加载失败')
    throw error
  } finally {
    runLoading.value = false
  }
}

let runningRunsRequest: Promise<void> | undefined
const loadRunningSyncRuns = (background = false): Promise<void> => {
  if (runningRunsRequest) return runningRunsRequest
  runningRunsRequest = fetchRunningSyncRuns(background).finally(() => {
    runningRunsRequest = undefined
  })
  return runningRunsRequest
}
const fetchRunningSyncRuns = async (background: boolean) => {
  runningJobLoading.value = !background
  try {
    const page = await ErpKingdeeSyncApi.getRunPage({
      pageNo: 1,
      pageSize: RUNNING_SYNC_RUN_PAGE_SIZE,
      status: RUNNING_SYNC_STATUS
    } as PageParam & { status: number })
    if (!Array.isArray(page?.list)) throw new Error('运行中的同步Job返回格式无效')
    runningSyncRuns.value = page.list
    runningRunsLoaded.value = true
    runningRunsError.value = ''
    runningRunsUpdatedAt.value = Date.now()
  } catch (error) {
    runningSyncRuns.value = []
    runningRunsLoaded.value = false
    runningRunsUpdatedAt.value = undefined
    runningRunsError.value = resolveErrorMessage(error, '运行中的同步 Job 加载失败')
    throw error
  } finally {
    runningJobLoading.value = false
  }
}

const resolveRunStartedTimestamp = (value: unknown) => {
  if (typeof value !== 'string' && typeof value !== 'number' && !(value instanceof Date)) {
    return undefined
  }
  return toDateTimeValue(value)?.getTime()
}

const applyActiveConnection = (data: ErpKingdeeActiveConnectionVO) => {
  const validActiveType = data?.activeConnectionType in REQUIRED_CONNECTION_OPTIONS
  const validOptions =
    Array.isArray(data?.options) &&
    data.options.length === 2 &&
    data.options.every(
      (item) =>
        item.connectionType in REQUIRED_CONNECTION_OPTIONS &&
        item.connectionName === REQUIRED_CONNECTION_OPTIONS[item.connectionType]
    )
  if (!validActiveType || !validOptions) {
    throw new Error('ERP 连接配置返回格式无效')
  }
  currentConnectionType.value = data.activeConnectionType
  currentConnectionName.value = REQUIRED_CONNECTION_OPTIONS[data.activeConnectionType]
  selectedConnectionType.value = data.activeConnectionType
  connectionOptions.value = data.options
}

const loadActiveConnection = async () => {
  const data = await ErpKingdeeConfigApi.getActiveConnection()
  applyActiveConnection(data)
}

const loadData = async () => {
  loading.value = true
  loadError.value = ''
  try {
    await Promise.all([loadActiveConnection(), loadJobs(), loadLatestRuns(), loadRunningSyncRuns()])
  } catch (error) {
    loadError.value = resolveErrorMessage(error, 'ERP表格自动同步配置加载失败')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
    await syncSelectedRows()
  }
}

const handleSaveConnection = async () => {
  if (!requireCapability(canSaveConnection.value)) return
  if (!selectedConnectionType.value || !connectionDirty.value) return
  connectionSaving.value = true
  try {
    const data = await ErpKingdeeConfigApi.updateActiveConnection({
      connectionType: selectedConnectionType.value
    })
    applyActiveConnection(data)
    ElMessage.success(`ERP 当前连接已切换为${currentConnectionName.value}`)
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'ERP 连接保存失败'))
  } finally {
    connectionSaving.value = false
  }
}

const refreshJobsBeforeMutation = async () => {
  const entries = await Promise.all(
    syncTypes.map(async (item) => [item, await fetchJobByHandlerName(item.handlerName)] as const)
  )
  for (const [item, job] of entries) {
    jobsByHandlerName.value[item.handlerName] = job
  }
  return entries
}

const updateJobForAutoSync = async (
  item: ProfileErpSyncType,
  job: JobApi.JobVO,
  selectedSyncTypeSet: Set<string>
) => {
  const targetStatus =
    form.enabled && selectedSyncTypeSet.has(item.syncType)
      ? InfraJobStatusEnum.NORMAL
      : InfraJobStatusEnum.STOP

  // Job 更新接口只允许运行状态；暂停任务保存时间后恢复暂停状态。
  const cronExpression = dailyCronExpression.value
  if (job.cronExpression !== cronExpression) {
    const wasStopped = job.status === InfraJobStatusEnum.STOP
    if (job.status !== InfraJobStatusEnum.NORMAL) {
      await JobApi.updateJobStatus(job.id, InfraJobStatusEnum.NORMAL)
      job.status = InfraJobStatusEnum.NORMAL
    }
    let updated = false
    try {
      await JobApi.updateJob({
        ...job,
        status: InfraJobStatusEnum.NORMAL,
        cronExpression
      })
      updated = true
    } finally {
      if (wasStopped && (!updated || targetStatus === InfraJobStatusEnum.STOP)) {
        await JobApi.updateJobStatus(job.id, InfraJobStatusEnum.STOP)
        job.status = InfraJobStatusEnum.STOP
      }
    }
  }
  if (job.status !== targetStatus) {
    await JobApi.updateJobStatus(job.id, targetStatus)
  }
}

const handleSave = async () => {
  if (!requireCapability(canSaveSchedule.value)) return
  if (!jobsLoaded.value || jobLoading.value || saving.value) {
    ElMessage.error('同步任务配置尚未有效加载，无法保存')
    return
  }
  if (!dailyCronExpression.value) {
    ElMessage.error('请选择有效的每日开始时间')
    return
  }
  if (form.enabled && selectedSyncTypes.value.length === 0) {
    ElMessage.error('启用自动同步时至少选择一个 ERP 表格')
    return
  }
  saving.value = true
  try {
    const selectedSyncTypeSet = new Set(selectedSyncTypes.value)
    const jobEntries = await refreshJobsBeforeMutation()
    await Promise.all(
      jobEntries.map(([item, job]) => updateJobForAutoSync(item, job, selectedSyncTypeSet))
    )
    ElMessage.success(
      `ERP表格自动同步配置已保存：${form.enabled ? selectedSyncTypeSet.size : 0}/${syncTypes.length}`
    )
    await loadJobs()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'ERP表格自动同步配置保存失败'))
  } finally {
    saving.value = false
  }
}

const handleRunOnce = async () => {
  if (!requireCapability(canRunOnce.value)) return
  const selectedItems = syncTypes.filter((item) => selectedSyncTypes.value.includes(item.syncType))
  if (selectedItems.length === 0) {
    ElMessage.error('请先选择至少一个 ERP 表格')
    return
  }
  running.value = true
  try {
    await refreshJobsBeforeMutation()
    await Promise.all(
      selectedItems.map((item) => ErpKingdeeSyncApi.runIncrementalSync(item.syncType))
    )
    ElMessage.success(`已提交 ${selectedItems.length} 个 ERP 增量同步任务`)
    try {
      await Promise.all([loadJobs(), loadLatestRuns(), loadRunningSyncRuns()])
    } catch (error) {
      ElMessage.error(
        `任务已提交，但配置或运行事实刷新失败：${resolveErrorMessage(error, '刷新失败')}`
      )
    }
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '增量执行一次失败'))
  } finally {
    running.value = false
  }
}

const handleRunIncremental = async (row: ProfileErpSyncTableRow) => {
  if (!requireCapability(canRunSync.value)) return
  incrementalSyncingType.value = row.syncType
  try {
    await ErpKingdeeSyncApi.runIncrementalSync(row.syncType)
    ElMessage.success(`已提交 ${row.erpTableName} 单表 ERP 增量同步任务`)
    await Promise.all([loadLatestRuns(), loadRunningSyncRuns()])
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '增量同步失败'))
  } finally {
    incrementalSyncingType.value = ''
  }
}

const waitForSubmittedRun = async (
  syncType: string,
  previousRunId: number | undefined,
  submittedAt: number
) => {
  for (let attempt = 0; attempt < SUBMITTED_RUN_POLL_ATTEMPTS; attempt++) {
    await Promise.all([loadLatestRuns(), loadRunningSyncRuns()])
    const latestRun = latestRuns.value.find((item) => item.syncType === syncType)
    const latestRunStartedAt = resolveRunStartedTimestamp(latestRun?.startedAt)
    const isNewRun =
      previousRunId !== undefined
        ? latestRun?.id !== previousRunId
        : typeof latestRunStartedAt === 'number' && latestRunStartedAt >= submittedAt
    if (latestRun && isNewRun) return true
    if (attempt < SUBMITTED_RUN_POLL_ATTEMPTS - 1) {
      await new Promise((resolve) => window.setTimeout(resolve, SUBMITTED_RUN_POLL_INTERVAL_MS))
    }
  }
  return false
}

const handleRunFull = async (row: ProfileErpSyncTableRow) => {
  if (!requireCapability(canRunSync.value)) return
  fullSyncingType.value = row.syncType
  try {
    const previousRunId = latestRunBySyncType.value[row.syncType]?.id
    const submittedAt = Date.now()
    await ErpKingdeeSyncApi.runFullSync(row.syncType)
    const runVisible = await waitForSubmittedRun(row.syncType, previousRunId, submittedAt)
    if (runVisible) {
      ElMessage.success(`已提交 ${row.erpTableName} 单表 ERP 全量同步任务`)
    } else {
      ElMessage.warning(
        `已提交 ${row.erpTableName} 全量同步，但运行记录尚未生成，请检查调度任务状态`
      )
    }
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '全量同步失败'))
  } finally {
    fullSyncingType.value = ''
  }
}

watch(
  [syncTableRows, selectedSyncTypes],
  () => {
    void syncSelectedRows()
  },
  { flush: 'post' }
)

let initialProgressLoaded = false
const progressViewActive = ref(false)
const documentVisibility = useDocumentVisibility()
const refreshVisibleRunProgress = async (background = true) => {
  const refresh = activeList.value === 'running' ? loadRunningSyncRuns : loadLatestRuns
  // Loaders expose errors and clear stale counters; polling must remain available after a failed read.
  const [result] = await Promise.allSettled([refresh(background)])
  if (result.status === 'rejected' && !background) {
    ElMessage.error(resolveErrorMessage(result.reason, '同步进度刷新失败'))
  }
}
const { pause: pauseProgressPolling, resume: resumeProgressPolling } = useTimeoutPoll(
  refreshVisibleRunProgress,
  5000
)
const updateProgressPolling = () => {
  if (
    initialProgressLoaded &&
    progressViewActive.value &&
    documentVisibility.value === 'visible' &&
    canRunSync.value
  ) {
    resumeProgressPolling()
  } else {
    pauseProgressPolling()
  }
}
onMounted(async () => {
  progressViewActive.value = true
  await loadData()
  initialProgressLoaded = true
  updateProgressPolling()
})
onActivated(() => {
  progressViewActive.value = true
  updateProgressPolling()
})
onDeactivated(() => {
  progressViewActive.value = false
  pauseProgressPolling()
})
onBeforeUnmount(() => {
  progressViewActive.value = false
  pauseProgressPolling()
})
watch([documentVisibility, canRunSync], updateProgressPolling)
watch(activeList, () => {
  if (progressViewActive.value && documentVisibility.value === 'visible') {
    void refreshVisibleRunProgress()
  }
})
</script>

<style scoped>
.profile-erp-table-sync__progress-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}
.profile-erp-table-sync__progress-hint {
  margin-top: 4px;
  color: #606266;
  font-size: 12px;
}

.profile-erp-table-sync {
  width: 100%;
  max-width: none;
}

.profile-erp-table-sync__form {
  margin-top: 16px;
}

.profile-erp-table-sync__top-settings {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(320px, 440px);
  gap: 24px;
  align-items: start;
  margin-bottom: 4px;
}

.profile-erp-table-sync__schedule-settings {
  min-width: 0;
}

.profile-erp-table-sync__schedule-actions {
  display: flex;
  align-items: center;
  padding-top: 50px;
}

.profile-erp-table-sync__connection-setting {
  grid-column: 3;
  min-width: 0;
  padding-left: 24px;
  border-left: 1px solid #dcdfe6;
}

.profile-erp-table-sync__connection-status {
  display: flex;
  min-height: 24px;
  align-items: center;
  gap: 8px;
}

.profile-erp-table-sync__connection-label {
  color: #606266;
  font-size: 14px;
}

.profile-erp-table-sync__connection-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}

.profile-erp-table-sync__connection-controls :deep(.el-segmented) {
  flex: 1;
  min-width: 260px;
}

.profile-erp-table-sync__connection-save {
  flex: none;
}

.profile-erp-table-sync__select-table {
  width: 100%;
}

.profile-erp-table-sync__running-jobs {
  margin-top: 20px;
}

.profile-erp-table-sync__running-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.profile-erp-table-sync__running-title {
  color: #1f2d3d;
  font-size: 15px;
  font-weight: 600;
}

.profile-erp-table-sync__running-subtitle {
  color: #8a94a6;
  font-size: 12px;
}

.profile-erp-table-sync__running-table {
  width: 100%;
}

@media (max-width: 1100px) {
  .profile-erp-table-sync__top-settings {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .profile-erp-table-sync__schedule-actions {
    padding-top: 0;
  }

  .profile-erp-table-sync__connection-setting {
    grid-column: 1;
    padding-top: 16px;
    padding-left: 0;
    border-top: 1px solid #dcdfe6;
    border-left: 0;
  }
}

@media (max-width: 768px) {
  .profile-erp-table-sync__connection-setting {
    width: 100%;
  }

  .profile-erp-table-sync__connection-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .profile-erp-table-sync__connection-controls :deep(.el-segmented) {
    width: 100%;
    min-width: 0;
  }

  .profile-erp-table-sync__connection-save {
    width: 100%;
  }

  .profile-erp-table-sync__form :deep(.el-form-item) {
    display: block;
  }

  .profile-erp-table-sync__form :deep(.el-form-item__label) {
    display: block;
    width: auto !important;
    text-align: left;
  }

  .profile-erp-table-sync__form :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
}
</style>
