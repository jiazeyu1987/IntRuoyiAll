<template>
  <ContentWrap class="backup-plan-page">
    <el-card shadow="never" class="backup-plan-card" v-loading="statusLoading">
      <template #header>
        <div class="backup-plan-card__header">
          <div>
            <div class="backup-plan-card__title">当前自动备份计划</div>
            <div class="backup-plan-card__hint">
              管理员只需要选择开关、频率和时间，系统会按计划自动备份。
            </div>
          </div>
          <el-button
            plain
            type="primary"
            :loading="backupNowLoading"
            @click="handleBackupNow"
            v-hasPermi="['system:backup-plan:execute']"
          >
            现在备份一次
          </el-button>
        </div>
      </template>

      <div class="backup-plan-status-grid">
        <div class="backup-plan-status-item">
          <span class="backup-plan-status-item__label">状态</span>
          <el-tag :type="healthStatusType">{{ displayHealthStatus }}</el-tag>
        </div>
        <div class="backup-plan-status-item">
          <span class="backup-plan-status-item__label">自动备份</span>
          <span>{{ status?.planStatus || '已关闭' }}</span>
        </div>
        <div class="backup-plan-status-item">
          <span class="backup-plan-status-item__label">多久备份一次</span>
          <span>{{ formatFrequency(scheduleForm.frequency) }}</span>
        </div>
        <div class="backup-plan-status-item">
          <span class="backup-plan-status-item__label">几点备份</span>
          <span>{{ scheduleForm.time || '--' }}</span>
        </div>
        <div class="backup-plan-status-item">
          <span class="backup-plan-status-item__label">下次备份时间</span>
          <span>{{ formatDateTime(status?.nextRunTime) }}</span>
        </div>
        <div class="backup-plan-status-item">
          <span class="backup-plan-status-item__label">上次备份结果</span>
          <span>{{ formatLastResult(status) }}</span>
        </div>
      </div>

      <el-alert
        v-if="status?.blockedReason"
        class="mt-16px"
        type="error"
        title="配置异常"
        :description="status.blockedReason"
        show-icon
        :closable="false"
      />

      <el-divider />

      <el-form
        class="backup-plan-form"
        label-width="96px"
        :model="scheduleForm"
        @submit.prevent
      >
        <el-form-item label="自动备份">
          <el-switch
            v-model="scheduleForm.autoBackup"
            :loading="toggleLoading"
            @change="handleAutoBackupChange"
            v-hasPermi="['system:backup-plan:update']"
          />
          <span class="backup-plan-form__tip">
            打开后，系统会按下面设置的时间自动备份。
          </span>
        </el-form-item>
        <el-form-item label="备份频率">
          <el-radio-group v-model="scheduleForm.frequency">
            <el-radio-button label="DAILY">每天</el-radio-button>
            <el-radio-button label="WEEKLY">每周</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="scheduleForm.frequency === 'WEEKLY'" label="星期几">
          <el-select v-model="scheduleForm.weekday" class="!w-180px" placeholder="请选择星期">
            <el-option
              v-for="item in weekdayOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备份时间">
          <el-time-picker
            v-model="scheduleForm.time"
            class="!w-180px"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="请选择时间"
          />
          <span class="backup-plan-form__tip">建议选择业务低峰时间，例如 01:30。</span>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="saveLoading"
            @click="handleSaveSchedule"
            v-hasPermi="['system:backup-plan:update']"
          >
            保存计划
          </el-button>
          <el-button :loading="statusLoading" @click="loadAll">刷新状态</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="backup-plan-card mt-16px">
      <template #header>
        <div class="backup-plan-card__header">
          <div>
            <div class="backup-plan-card__title">备份包历史</div>
            <div class="backup-plan-card__hint">
              这里展示已经生成的备份包，恢复操作仍在运行控制台中处理。
            </div>
          </div>
        </div>
      </template>

      <UnifiedListTemplate
        table-key="system.backup-plan.history"
        :query-model="queryParams"
        :filter-definitions="[]"
        :quick-filter-state="quickFilterState"
        :operator-options="[]"
        :columns="historyColumns"
        :column-saving="false"
        :show-column-settings="false"
        :show-quick-filter="false"
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @update:quick-filter-state="handleQuickFilterStateUpdate"
        @pagination="getHistoryList"
      >
        <template #actions>
          <el-button :loading="historyLoading" @click="getHistoryList">刷新列表</el-button>
        </template>
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
          <el-table
            v-loading="historyLoading"
            :data="historyList"
            border
            @sort-change="handleTemplateSortChange"
          >
            <el-table-column
              label="备份编号"
              prop="backupId"
              min-width="190"
              show-overflow-tooltip
              v-bind="sortColumnAttrs('backupId')"
            />
            <el-table-column
              label="生成时间"
              prop="backupId"
              width="180"
              v-bind="sortColumnAttrs('backupId')"
            >
              <template #default="{ row }">
                {{ formatBackupGeneratedTime(row) }}
              </template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">{{ formatBackupType(row) }}</template>
            </el-table-column>
            <el-table-column label="结果" width="120">
              <template #default="{ row }">
                <el-tag :type="row.recoverabilityStatus === 'RECOVERABLE' ? 'success' : 'warning'">
                  {{ row.recoverabilityStatus === 'RECOVERABLE' ? '成功' : '需检查' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="是否可恢复" width="130">
              <template #default="{ row }">
                {{ row.recoverabilityStatus === 'RECOVERABLE' ? '可恢复' : '不可恢复' }}
              </template>
            </el-table-column>
            <el-table-column label="保存位置" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">{{ formatBackupStorageLabel(row) }}</template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="110" align="center">
              <template #default="{ row }">
                <el-button type="primary" link @click="openBackupDetail(row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </UnifiedListTemplate>
    </el-card>

    <el-dialog v-model="detailDialog.visible" title="备份包详情" width="680px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="备份编号">
          {{ detailDialog.item?.backupId || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="生成时间">
          {{ detailDialog.item ? formatBackupGeneratedTime(detailDialog.item) : '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="是否可恢复">
          {{
            detailDialog.item?.recoverabilityStatus === 'RECOVERABLE' ? '可恢复' : '不可恢复'
          }}
        </el-descriptions-item>
        <el-descriptions-item label="保存位置">
          {{ detailDialog.item ? formatBackupStorageLabel(detailDialog.item) : '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="检查说明">
          <div v-if="detailDialog.item?.unrecoverableReasons?.length" class="backup-plan-reasons">
            <div v-for="reason in detailDialog.item.unrecoverableReasons" :key="reason">
              {{ reason }}
            </div>
          </div>
          <span v-else>未发现异常。</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { formatDateTimeValue } from '@/utils/formatTime'
import type { UserTableColumnState } from '@/hooks/web/useUserTableColumns'
import {
  backupNow,
  disableBackupPlan,
  enableBackupPlan,
  getBackupPlanHistoryPage,
  getBackupPlanStatus,
  saveBackupPlanSchedule,
  type BackupPlanBackupPointVO,
  type BackupPlanFrequency,
  type BackupPlanHealthStatus,
  type BackupPlanStatusVO,
  type BackupPlanWeekday
} from '@/api/system/backupPlan'

defineOptions({ name: 'SystemBackupPlan' })

const message = useMessage()

const simpleHealthStatusLabels: BackupPlanHealthStatus[] = ['正常', '已关闭', '上次失败', '配置异常']

const weekdayOptions: Array<{ label: string; value: BackupPlanWeekday }> = [
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' }
]

const historyColumns: UserTableColumnState[] = [
  { key: 'backupId', label: '备份编号', visible: true, minWidth: 190 },
  { key: 'generatedTime', label: '生成时间', visible: true, width: 180, sortProp: 'backupId' },
  { key: 'backupMode', label: '类型', visible: true, width: 120, sortable: false },
  { key: 'result', label: '结果', visible: true, width: 120, sortable: false },
  { key: 'recoverabilityStatus', label: '是否可恢复', visible: true, width: 130 },
  { key: 'manifestPath', label: '保存位置', visible: true, minWidth: 240 },
  { key: 'actions', label: '操作', visible: true, width: 110, hideable: false, business: false, sortable: false }
]

const statusLoading = ref(false)
const historyLoading = ref(false)
const saveLoading = ref(false)
const toggleLoading = ref(false)
const backupNowLoading = ref(false)
const status = ref<BackupPlanStatusVO>()
const historyList = ref<BackupPlanBackupPointVO[]>([])
const total = ref(0)
const quickFilterState = reactive({})
const queryParams = reactive<PageParam>({
  pageNo: 1,
  pageSize: 10
})
const scheduleForm = reactive({
  autoBackup: false,
  frequency: 'DAILY' as BackupPlanFrequency,
  time: '01:30',
  weekday: 'MON' as BackupPlanWeekday
})
const detailDialog = reactive<{
  visible: boolean
  item?: BackupPlanBackupPointVO
}>({
  visible: false,
  item: undefined
})

const displayHealthStatus = computed(() =>
  simpleHealthStatusLabels.includes(status.value?.healthStatus as BackupPlanHealthStatus)
    ? status.value?.healthStatus
    : '配置异常'
)

const healthStatusType = computed(() => {
  if (displayHealthStatus.value === '正常') return 'success'
  if (displayHealthStatus.value === '已关闭') return 'info'
  if (displayHealthStatus.value === '上次失败') return 'danger'
  return 'warning'
})

const handleQuickFilterStateUpdate = (state: Record<string, unknown>) => {
  Object.assign(quickFilterState, state)
}

const formatDateTime = (value?: string) => formatDateTimeValue(value, '--')

const formatFrequency = (frequency?: string) => (frequency === 'WEEKLY' ? '每周' : '每天')

const formatLastResult = (currentStatus?: BackupPlanStatusVO) => {
  if (!currentStatus?.lastRunTime) return '暂无记录'
  if (currentStatus.lastResultCode === 0) return `成功（${formatDateTime(currentStatus.lastRunTime)}）`
  return `失败（${formatDateTime(currentStatus.lastRunTime)}）`
}

const formatBackupType = (row: BackupPlanBackupPointVO) => {
  if (row.backupMode === 'full' || row.dccBackupMode === 'baseline') return '完整备份'
  if (row.backupMode === 'incremental' || row.dccBackupMode === 'incremental') return '增量备份'
  return row.backupMode || row.dccBackupMode || '备份包'
}

const parseBackupIdTime = (backupId?: string) => {
  const match = backupId?.match(/(20\d{2})[-_]?(\d{2})[-_]?(\d{2})[-_T]?(\d{2})[-_]?(\d{2})[-_]?(\d{2})?/)
  if (!match) return undefined
  const [, year, month, day, hour, minute, second = '00'] = match
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

const formatBackupGeneratedTime = (row: BackupPlanBackupPointVO) =>
  parseBackupIdTime(row.backupId) || formatDateTime(row.lastVerifiedAt)

const formatBackupStorageLabel = (row: BackupPlanBackupPointVO) => {
  if (row.backupId) return `备份包：${row.backupId}`
  return '备份包'
}

const syncScheduleForm = (data: BackupPlanStatusVO) => {
  scheduleForm.autoBackup = data.planStatus === '已开启'
  scheduleForm.frequency = data.frequency === 'WEEKLY' ? 'WEEKLY' : 'DAILY'
  scheduleForm.time = data.time || '01:30'
  scheduleForm.weekday = data.weekday || 'MON'
}

const buildSchedulePayload = () => {
  if (!scheduleForm.time) {
    throw new Error('请选择备份时间')
  }
  return {
    frequency: scheduleForm.frequency,
    time: scheduleForm.time,
    weekday: scheduleForm.frequency === 'WEEKLY' ? scheduleForm.weekday : undefined
  }
}

const loadStatus = async () => {
  statusLoading.value = true
  try {
    const data = await getBackupPlanStatus()
    status.value = data
    syncScheduleForm(data)
  } finally {
    statusLoading.value = false
  }
}

const getHistoryList = async () => {
  historyLoading.value = true
  try {
    const data = await getBackupPlanHistoryPage(queryParams)
    historyList.value = data.list || []
    total.value = data.total || 0
  } finally {
    historyLoading.value = false
  }
}

const loadAll = async () => {
  await Promise.all([loadStatus(), getHistoryList()])
}

const handleSaveSchedule = async () => {
  saveLoading.value = true
  try {
    const data = await saveBackupPlanSchedule(buildSchedulePayload())
    status.value = data
    syncScheduleForm(data)
    message.success('备份计划已保存')
  } catch (error: any) {
    message.error(error?.message || '备份计划保存失败')
  } finally {
    saveLoading.value = false
  }
}

const handleAutoBackupChange = async (enabled: string | number | boolean) => {
  const targetEnabled = Boolean(enabled)
  toggleLoading.value = true
  try {
    let data: BackupPlanStatusVO
    if (targetEnabled) {
      await saveBackupPlanSchedule(buildSchedulePayload())
      data = await enableBackupPlan()
      message.success('自动备份已开启')
    } else {
      data = await disableBackupPlan()
      message.success('自动备份已关闭')
    }
    status.value = data
    syncScheduleForm(data)
  } catch (error: any) {
    scheduleForm.autoBackup = !targetEnabled
    message.error(error?.message || '自动备份状态修改失败')
    await loadStatus()
  } finally {
    toggleLoading.value = false
  }
}

const handleBackupNow = async () => {
  await ElMessageBox.confirm('会立即备份正式服数据，可能需要几分钟，是否继续？', '立即备份确认', {
    confirmButtonText: '继续备份',
    cancelButtonText: '取消',
    type: 'warning'
  })
  backupNowLoading.value = true
  try {
    const result = await backupNow()
    message.success(result.operationId ? `已提交备份任务：${result.operationId}` : '已提交备份任务')
    await loadAll()
  } catch (error: any) {
    message.error(error?.message || '立即备份提交失败')
  } finally {
    backupNowLoading.value = false
  }
}

const openBackupDetail = (row: BackupPlanBackupPointVO) => {
  detailDialog.item = row
  detailDialog.visible = true
}

onMounted(loadAll)
</script>

<style scoped>
.backup-plan-page {
  display: flex;
  flex-direction: column;
}

.backup-plan-card {
  border-radius: 10px;
}

.backup-plan-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.backup-plan-card__title {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.backup-plan-card__hint {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
  line-height: 20px;
}

.backup-plan-status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.backup-plan-status-item {
  min-height: 72px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
  padding: 12px;
}

.backup-plan-status-item__label {
  display: block;
  margin-bottom: 8px;
  color: #6b7280;
  font-size: 12px;
}

.backup-plan-form {
  max-width: 760px;
}

.backup-plan-form__tip {
  margin-left: 10px;
  color: #6b7280;
  font-size: 12px;
}

.backup-plan-reasons {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #b42318;
}

@media (max-width: 900px) {
  .backup-plan-card__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .backup-plan-status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
