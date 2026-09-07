<template>
  <div class="followup-management" data-testid="dcc-publication-followup-management">
    <ContentWrap>
      <header class="management-header">
        <div>
          <h1>发布后续</h1>
        </div>
        <el-button :loading="loading" @click="loadPage">
          <Icon icon="ep:refresh-right" class="mr-5px" />刷新
        </el-button>
      </header>
      <el-form :model="filters" class="management-filters" label-position="top">
        <el-form-item label="文件编号"><el-input v-model="filters.fileNumber" clearable /></el-form-item>
        <el-form-item label="发布版本"><el-input v-model="filters.versionNo" clearable /></el-form-item>
        <el-form-item label="批次状态">
          <el-select v-model="filters.batchStatus" clearable><el-option v-for="item in batchStatuses" :key="item" :label="batchStatusLabel(item)" :value="item" /></el-select>
        </el-form-item>
        <el-form-item label="任务状态">
          <el-select v-model="filters.taskStatus" clearable><el-option v-for="item in taskStatuses" :key="item" :label="impactTaskStatusLabel(item)" :value="item" /></el-select>
        </el-form-item>
        <el-form-item label="通知状态">
          <el-select v-model="filters.notificationStatus" clearable><el-option v-for="item in notificationStatuses" :key="item" :label="notificationStatusLabel(item)" :value="item" /></el-select>
        </el-form-item>
        <el-form-item label="负责人 ID"><el-input v-model="filters.assigneeUserId" clearable /></el-form-item>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </el-form>
    </ContentWrap>

    <ContentWrap>
      <el-alert
        v-if="loadError"
        role="alert"
        type="error"
        :closable="false"
        show-icon
        title="发布后续加载失败"
        :description="loadError"
      />
      <el-alert
        v-if="actionError"
        role="alert"
        type="error"
        :closable="false"
        show-icon
        title="发布后续操作失败"
        :description="actionError"
      />
      <el-table v-loading="loading" :data="rows" empty-text="暂无发布后续记录" row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expanded-grid">
              <section>
                <h2>通知明细</h2>
                <el-table :data="row.notificationDeliveries" size="small" empty-text="无通知收件人">
                  <el-table-column label="收件人" min-width="150">
                    <template #default="scope">{{ scope.row.userName || scope.row.userId }}</template>
                  </el-table-column>
                  <el-table-column label="通知状态" width="110">
                    <template #default="scope">{{ notificationStatusLabel(scope.row.status) }}</template>
                  </el-table-column>
                  <el-table-column label="收件原因" min-width="220">
                    <template #default="scope">{{ scope.row.reasonSummaries.join('；') }}</template>
                  </el-table-column>
                  <el-table-column label="错误" min-width="180" prop="lastErrorSummary" />
                  <el-table-column label="操作" width="90">
                    <template #default="scope">
                      <el-button
                        v-if="scope.row.status === 'PENDING' || scope.row.status === 'FAILED'"
                        link
                        type="primary"
                        :loading="retryingDeliveryId === scope.row.id"
                        :disabled="Boolean(retryingDeliveryId)"
                        @click="retry(scope.row)"
                      >重试</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </section>
              <section>
                <h2>影响评估</h2>
                <el-table :data="row.impactTasks" size="small" empty-text="无需评估">
                  <el-table-column label="相关文件" min-width="180" prop="relatedFileName" />
                  <el-table-column label="方向" width="140">
                    <template #default="scope">{{ scope.row.relationDirections.map(relationDirectionLabel).join(' / ') }}</template>
                  </el-table-column>
                  <el-table-column label="负责人" min-width="130" prop="assigneeUserName" />
                  <el-table-column label="任务状态" width="120">
                    <template #default="scope">{{ impactTaskStatusLabel(scope.row.taskStatus) }}</template>
                  </el-table-column>
                  <el-table-column label="评估结论" min-width="140">
                    <template #default="scope">{{ scope.row.decision ? impactDecisionLabel(scope.row.decision) : '尚未提交' }}</template>
                  </el-table-column>
                  <el-table-column label="升版跟踪" width="140">
                    <template #default="scope">{{ revisionTrackingStatusLabel(scope.row.revisionTrackingStatus) }}</template>
                  </el-table-column>
                </el-table>
              </section>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文件编号" min-width="160" prop="fileNumber" />
        <el-table-column label="文件名称" min-width="220" prop="fileName" />
        <el-table-column label="发布版本" width="110" prop="versionNo" />
        <el-table-column label="批次状态" width="140">
          <template #default="{ row }">{{ batchStatusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column label="通知" width="100">
          <template #default="{ row }">{{ row.notificationDeliveries.length }}</template>
        </el-table-column>
        <el-table-column label="任务" width="100">
          <template #default="{ row }">{{ row.impactTasks.length }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openFile(row.publishedControlledFileId)">文件详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination v-model:page="pageNo" v-model:limit="pageSize" :total="total" @pagination="loadPage" />
    </ContentWrap>
  </div>
</template>

<script lang="ts" setup>
import { ElMessageBox } from 'element-plus'
import {
  getPublicationFollowupManagementPage,
  retryPublicationNotification,
  type DccPublicationFollowupVO,
  type DccPublicationNotificationDeliveryVO
} from '@/api/dcc/controlledFile/publicationFollowup'
import {
  batchStatusLabel,
  impactDecisionLabel,
  impactTaskStatusLabel,
  notificationStatusLabel,
  relationDirectionLabel,
  revisionTrackingStatusLabel
} from '../shared/publicationFollowupPresentation'

defineOptions({ name: 'DccControlledFilePublicationFollowup' })
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const loadError = ref('')
const actionError = ref('')
const retryingDeliveryId = ref('')
const rows = ref<DccPublicationFollowupVO[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const filters = reactive({ fileNumber: '', versionNo: '', batchStatus: '', taskStatus: '', notificationStatus: '', assigneeUserId: '' })
const batchStatuses = ['PROCESSING', 'READY', 'PARTIAL_FAILED', 'COMPLETED']
const taskStatuses = ['PENDING', 'UNASSIGNED', 'IN_REVIEW', 'COMPLETED']
const notificationStatuses = ['PENDING', 'SENT', 'FAILED']

const errorText = (error: unknown) => {
  const value = (error as any)?.response?.data?.msg || (error as any)?.message
  return typeof value === 'string' && value.trim() ? value : '无法读取发布后续'
}

const isPositiveLongId = (value: string) => {
  if (!/^[1-9]\d{0,18}$/.test(value)) return false
  return value.length < 19 || value <= '9223372036854775807'
}

const loadPage = async () => {
  loading.value = true
  loadError.value = ''
  try {
    if (filters.assigneeUserId && !isPositiveLongId(filters.assigneeUserId)) {
      loadError.value = '负责人 ID 必须是有效的正十进制整数'
      return
    }
    const result = await getPublicationFollowupManagementPage({
      pageNo: pageNo.value, pageSize: pageSize.value,
      fileNumber: filters.fileNumber || undefined, versionNo: filters.versionNo || undefined,
      batchStatus: filters.batchStatus || undefined, taskStatus: filters.taskStatus || undefined,
      notificationStatus: filters.notificationStatus || undefined,
      assigneeUserId: filters.assigneeUserId || undefined
    })
    rows.value = result.list || []
    total.value = result.total || 0
  } catch (error) {
    loadError.value = errorText(error)
  } finally {
    loading.value = false
  }
}

const search = () => { pageNo.value = 1; loadPage() }
const reset = () => { Object.assign(filters, { fileNumber: '', versionNo: '', batchStatus: '', taskStatus: '', notificationStatus: '', assigneeUserId: '' }); search() }
const openFile = (id: string) => router.push({ name: 'DccControlledFileDetail', params: { id }, query: { viewer: '1', from: 'followup-management' } })
const isMessageBoxCancel = (error: unknown) =>
  error === 'cancel' || error === 'close' ||
  (Boolean(error) && typeof error === 'object' && ['cancel', 'close'].includes(String((error as any).action)))
const retry = async (delivery: DccPublicationNotificationDeliveryVO) => {
  actionError.value = ''
  retryingDeliveryId.value = delivery.id
  try {
    const prompt = await ElMessageBox.prompt('请填写重试原因', '重试发布通知', {
      confirmButtonText: '重试', cancelButtonText: '取消',
      inputValidator: (value) => Boolean(value?.trim()), inputErrorMessage: '重试原因不能为空'
    })
    await retryPublicationNotification(delivery.id, {
      expectedVersion: delivery.rowVersion, reason: prompt.value.trim()
    })
    message.success('通知重试完成')
    await loadPage()
  } catch (error) {
    if (!isMessageBoxCancel(error)) actionError.value = errorText(error)
  } finally {
    retryingDeliveryId.value = ''
  }
}

onMounted(loadPage)
</script>

<style scoped>
.followup-management { display: grid; gap: 16px; }
.management-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.management-header h1 { margin: 0; font-size: 20px; line-height: 28px; color: #172033; }
.management-filters { display: grid; grid-template-columns: repeat(3, minmax(160px, 1fr)); gap: 4px 14px; margin-top: 14px; }
.filter-actions { display: flex; align-items: flex-end; gap: 8px; padding-bottom: 18px; }
.expanded-grid { display: grid; gap: 18px; padding: 8px 16px 16px; }
.expanded-grid h2 { margin: 0 0 8px; font-size: 14px; line-height: 22px; }
@media (max-width: 768px) {
  .management-header { flex-direction: column; }
  .management-filters { grid-template-columns: 1fr; }
  .followup-management :deep(.el-table) { min-width: 720px; }
  .followup-management :deep(.el-card__body) { overflow-x: auto; }
}
</style>
