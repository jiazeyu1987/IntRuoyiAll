<template>
  <el-card class="profile-erp-table-sync" shadow="never">
    <template #header>
      <div class="profile-erp-table-sync__header">
        <div>
          <div class="profile-erp-table-sync__title">ERP表格自动同步</div>
          <div class="profile-erp-table-sync__subtitle">
            统一配置每天自动拉取哪些 ERP 表格、几点开始；复用生产工单同款 Job 增量同步链路。
          </div>
        </div>
        <el-tag type="success">infra/job</el-tag>
      </div>
    </template>

    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

    <el-form
      class="profile-erp-table-sync__form"
      label-width="120px"
      :disabled="loading || saving || connectionSaving"
    >
      <div class="profile-erp-table-sync__top-settings">
        <div class="profile-erp-table-sync__schedule-settings">
          <el-form-item label="启用自动同步">
            <el-switch v-model="form.enabled" />
          </el-form-item>
          <el-form-item label="每日开始时间">
            <el-time-picker
              v-model="form.dailyStartTime"
              value-format="HH:mm:ss"
              format="HH:mm:ss"
              :clearable="false"
            />
          </el-form-item>
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
              :disabled="!connectionDirty"
              @click="handleSaveConnection"
            >
              <Icon icon="ep:check" />
              保存连接
            </el-button>
          </div>
        </section>
      </div>
      <el-form-item label="ERP 表格">
        <el-table
          ref="syncTableRef"
          v-loading="jobLoading || runLoading"
          :data="syncTableRows"
          row-key="syncType"
          class="profile-erp-table-sync__select-table"
          border
          @selection-change="handleSyncTableSelectionChange"
        >
          <el-table-column type="selection" width="48" :selectable="isSyncRowSelectable" />
          <el-table-column prop="erpTableName" label="ERP表格名称" min-width="180" />
          <el-table-column prop="localTabName" label="本地页签名称" min-width="220" />
          <el-table-column label="最近执行时间" min-width="180">
            <template #default="{ row }">{{ resolveLatestRunTime(row.latestRun) }}</template>
          </el-table-column>
          <el-table-column label="新增行数" min-width="110">
            <template #default="{ row }">{{ resolveCreatedCount(row.latestRun) }}</template>
          </el-table-column>
          <el-table-column label="同步成功/失败" min-width="140">
            <template #default="{ row }">
              <el-tag :type="resolveLatestSyncStatusTagType(row.latestRun)">
                {{ formatLatestSyncStatus(row.latestRun) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="失败原因" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ resolveFailureReason(row.latestRun) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                :loading="manualSyncingType === row.syncType"
                :disabled="running || Boolean(manualSyncingType)"
                @click="handleRunSingle(row)"
              >
                手动同步
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        <el-button
          type="success"
          :loading="running"
          :disabled="selectedSyncTypes.length === 0 || Boolean(manualSyncingType)"
          @click="handleRunOnce"
        >
          立即执行一次
        </el-button>
      </el-form-item>
    </el-form>

    <div class="profile-erp-table-sync__running-jobs">
      <div class="profile-erp-table-sync__running-header">
        <div class="profile-erp-table-sync__running-title">正在进行的同步 Job</div>
        <div class="profile-erp-table-sync__running-subtitle"
          >仅显示当前运行中的 ERP 同步任务。</div
        >
      </div>
      <el-table
        v-loading="runningJobLoading"
        :data="runningSyncRuns"
        row-key="id"
        class="profile-erp-table-sync__running-table"
        border
        empty-text="暂无正在进行的同步 Job"
      >
        <el-table-column label="ERP表格名称" min-width="180">
          <template #default="{ row }">{{ resolveSyncTypeName(row.syncType) }}</template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="180">
          <template #default="{ row }">{{ formatDateTimeValue(row.startedAt, '-') }}</template>
        </el-table-column>
        <el-table-column label="新增行数" min-width="110">
          <template #default="{ row }">{{ resolveRunCount(row.createdCount) }}</template>
        </el-table-column>
        <el-table-column label="更新行数" min-width="110">
          <template #default="{ row }">{{ resolveRunCount(row.updatedCount) }}</template>
        </el-table-column>
        <el-table-column label="失败行数" min-width="110">
          <template #default="{ row }">{{ resolveRunCount(row.failedCount) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
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
import { formatDateTimeValue } from '@/utils/formatTime'

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

defineOptions({ name: 'ProfileErpTableAutoSyncSetting' })

const DEFAULT_DAILY_START_TIME = '02:00:00'
const RUNNING_SYNC_STATUS = 10
const RUNNING_SYNC_RUN_PAGE_SIZE = 20
const REQUIRED_CONNECTION_OPTIONS: Record<ErpKingdeeConnectionType, string> = {
  TEST: '测试账套',
  PRODUCTION: '正式账套'
}
const syncTypes: ProfileErpSyncType[] = [
  {
    syncType: 'PRODUCT',
    erpTableName: 'ERP 商品',
    localTabName: 'ERP商品 / MES物料产品',
    handlerName: 'kingdeeProductItemSyncJob'
  },
  {
    syncType: 'STOCK',
    erpTableName: 'ERP 库存',
    localTabName: 'ERP库存',
    handlerName: 'kingdeeStockSyncJob'
  },
  {
    syncType: 'PURCHASE_ORDER',
    erpTableName: '采购订单',
    localTabName: 'ERP采购订单',
    handlerName: 'kingdeePurchaseOrderSyncJob'
  },
  {
    syncType: 'SALE_ORDER',
    erpTableName: '销售订单',
    localTabName: 'ERP销售订单',
    handlerName: 'kingdeeSaleOrderSyncJob'
  },
  {
    syncType: 'PRODUCTION_ORDER',
    erpTableName: '生产工单',
    localTabName: 'MES生产工单',
    handlerName: 'kingdeeProductionOrderSyncJob'
  },
  {
    syncType: 'PRODUCTION_MATERIAL_LIST',
    erpTableName: '生产用料清单',
    localTabName: 'ERP生产用料清单',
    handlerName: 'kingdeeProductionMaterialListSyncJob'
  },
  {
    syncType: 'BOM',
    erpTableName: '产品 BOM',
    localTabName: 'ERP产品BOM',
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
const manualSyncingType = ref('')
const loadError = ref('')
const latestRuns = ref<ErpKingdeeSyncRunVO[]>([])
const runningSyncRuns = ref<ErpKingdeeSyncRunVO[]>([])
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
  dailyStartTime: DEFAULT_DAILY_START_TIME
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
  if ([hour, minute, second].some((part) => Number.isNaN(part))) return ''
  return `${second} ${minute} ${hour} * * ?`
}

const parseDailyCronExpression = (cronExpression?: string) => {
  const match = cronExpression?.trim().match(/^(\d{1,2})\s+(\d{1,2})\s+(\d{1,2})\s+\*\s+\*\s+\?$/)
  if (!match) return ''
  const [, second, minute, hour] = match
  return `${padTimePart(hour)}:${padTimePart(minute)}:${padTimePart(second)}`
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

const isSyncRowSelectable = () => !loading.value && !saving.value && !manualSyncingType.value

const formatLatestSyncStatus = (latestRun?: ErpKingdeeSyncRunVO) => {
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

const resolveCreatedCount = (latestRun?: ErpKingdeeSyncRunVO) => {
  if (!latestRun) return '-'
  if (typeof latestRun.createdCount === 'number') return latestRun.createdCount
  return '-'
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

const resolveRunCount = (value?: number) => (typeof value === 'number' ? value : '-')

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
  if (syncingTableSelection.value) return
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

const loadJobs = async () => {
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
    form.dailyStartTime =
      syncTypes
        .map((item) =>
          parseDailyCronExpression(nextJobsByHandlerName[item.handlerName].cronExpression)
        )
        .find((time) => time) || DEFAULT_DAILY_START_TIME
  } finally {
    jobLoading.value = false
  }
}

const loadLatestRuns = async () => {
  runLoading.value = true
  try {
    const latestRunList = await Promise.all(
      syncTypes.map(async (item) => {
        const page = await ErpKingdeeSyncApi.getRunPage({
          pageNo: 1,
          pageSize: 1,
          syncType: item.syncType
        } as PageParam & { syncType: string })
        return page.list?.[0] as ErpKingdeeSyncRunVO | undefined
      })
    )
    latestRuns.value = latestRunList.filter((item): item is ErpKingdeeSyncRunVO => Boolean(item))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '同步运行结果加载失败'))
  } finally {
    runLoading.value = false
  }
}

const loadRunningSyncRuns = async () => {
  runningJobLoading.value = true
  try {
    const page = await ErpKingdeeSyncApi.getRunPage({
      pageNo: 1,
      pageSize: RUNNING_SYNC_RUN_PAGE_SIZE,
      status: RUNNING_SYNC_STATUS
    } as PageParam & { status: number })
    runningSyncRuns.value = page.list || []
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '运行中的同步 Job 加载失败'))
  } finally {
    runningJobLoading.value = false
  }
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
  }
}

const handleSaveConnection = async () => {
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
    syncTypes.map(
      async (item) =>
        [
          item,
          jobsByHandlerName.value[item.handlerName] ||
            (await fetchJobByHandlerName(item.handlerName))
        ] as const
    )
  )
  for (const [item, job] of entries) {
    jobsByHandlerName.value[item.handlerName] = job
  }
  return entries
}

const handleSave = async () => {
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
      jobEntries.map(async ([item, job]) => {
        await JobApi.updateJob({
          ...job,
          cronExpression: dailyCronExpression.value
        })
        const targetStatus =
          form.enabled && selectedSyncTypeSet.has(item.syncType)
            ? InfraJobStatusEnum.NORMAL
            : InfraJobStatusEnum.STOP
        if (job.status !== targetStatus) {
          await JobApi.updateJobStatus(job.id, targetStatus)
        }
      })
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
  const selectedItems = syncTypes.filter((item) => selectedSyncTypes.value.includes(item.syncType))
  if (selectedItems.length === 0) {
    ElMessage.error('请先选择至少一个 ERP 表格')
    return
  }
  running.value = true
  try {
    await refreshJobsBeforeMutation()
    await Promise.all(
      selectedItems.map((item) => ErpKingdeeSyncApi.runIncrementalSyncJob(item.handlerName))
    )
    ElMessage.success(`已提交 ${selectedItems.length} 个 ERP 增量同步任务`)
    await Promise.all([loadJobs(), loadLatestRuns(), loadRunningSyncRuns()])
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '立即执行一次失败'))
  } finally {
    running.value = false
  }
}

const handleRunSingle = async (row: ProfileErpSyncTableRow) => {
  manualSyncingType.value = row.syncType
  try {
    await ErpKingdeeSyncApi.runIncrementalSyncJob(row.handlerName)
    ElMessage.success(`已提交 ${row.erpTableName} 单表 ERP 增量同步任务`)
    await Promise.all([loadLatestRuns(), loadRunningSyncRuns()])
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '手动同步失败'))
  } finally {
    manualSyncingType.value = ''
  }
}

watch(
  [syncTableRows, selectedSyncTypes],
  () => {
    void syncSelectedRows()
  },
  { flush: 'post' }
)

onMounted(loadData)
</script>

<style scoped>
.profile-erp-table-sync {
  width: 100%;
  max-width: none;
}

.profile-erp-table-sync__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.profile-erp-table-sync__title {
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 600;
}

.profile-erp-table-sync__subtitle {
  margin-top: 4px;
  color: #6b778c;
  font-size: 13px;
}

.profile-erp-table-sync__form {
  margin-top: 16px;
}

.profile-erp-table-sync__top-settings {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(420px, 520px);
  gap: 32px;
  align-items: start;
  margin-bottom: 4px;
}

.profile-erp-table-sync__schedule-settings {
  min-width: 0;
}

.profile-erp-table-sync__connection-setting {
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

  .profile-erp-table-sync__connection-setting {
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
