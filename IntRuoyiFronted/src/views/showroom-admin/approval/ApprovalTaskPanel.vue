<template>
  <div class="showroom-approval-workbench">
    <div class="showroom-approval-workbench__toolbar">
      <div>
        <h3 class="showroom-approval-workbench__title">审批工作台</h3>
        <p class="showroom-approval-workbench__subtitle">
          聚合审批队列、差异明细与版本审计，所有动作都直接命中真实审批契约。
        </p>
      </div>
      <div class="showroom-approval-workbench__actions">
        <el-select v-model="filters.targetType" clearable placeholder="目标类型">
          <el-option label="公司" value="COMPANY" />
          <el-option label="展柜" value="HALL" />
          <el-option label="产品" value="PRODUCT" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="审批状态">
          <el-option label="主管审核中" value="PENDING_SUPERVISOR_REVIEW" />
          <el-option label="企宣审批中" value="PENDING_GAOXIN_APPROVAL" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已发布" value="PUBLISHED" />
        </el-select>
        <el-button :loading="loading" @click="loadApprovals">
          <Icon class="mr-5px" icon="ep:refresh" />
          刷新队列
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      :closable="false"
      show-icon
      title="审批工作台加载失败"
      type="error"
      :description="loadError"
    />

    <div class="showroom-approval-workbench__body">
      <div class="showroom-approval-workbench__list-shell">
        <div class="showroom-approval-workbench__section-title">审批队列</div>
        <el-table
          v-loading="loading"
          :data="filteredRows"
          highlight-current-row
          row-key="changeRequestId"
          @current-change="handleCurrentChange"
        >
          <el-table-column label="单号" min-width="92" prop="changeRequestId" />
          <el-table-column label="目标" width="92">
            <template #default="{ row }">
              <el-tag type="info">{{ resolveTargetTypeText(row.targetType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="对象ID" width="90" prop="targetId" />
          <el-table-column label="审批状态" min-width="120">
            <template #default="{ row }">
              <el-tag :type="resolveApprovalStatusTagType(row.status)">
                {{ resolveApprovalStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" min-width="110" prop="submissionSource" show-overflow-tooltip />
          <el-table-column label="提交人" width="92" prop="submittedBy" />
        </el-table>
      </div>

      <div class="showroom-approval-workbench__detail-shell">
        <div class="showroom-approval-workbench__detail-header">
          <div>
            <div class="showroom-approval-workbench__section-title">差异明细</div>
            <div v-if="activeDetail" class="showroom-approval-workbench__detail-meta">
              变更单 #{{ activeDetail.changeRequest.changeRequestId }} /
              {{ resolveTargetTypeText(activeDetail.changeRequest.targetType) }}
              {{ activeDetail.changeRequest.targetId }}
            </div>
          </div>
          <div v-if="activeDetail" class="showroom-approval-workbench__detail-actions">
            <el-button
              :disabled="!canApprove"
              :loading="actionLoading"
              type="primary"
              @click="openSignatureDialog('approve')"
            >
              {{ approveButtonText }}
            </el-button>
            <el-button
              :disabled="!canReject"
              :loading="actionLoading"
              type="danger"
              @click="openSignatureDialog('reject')"
            >
              驳回
            </el-button>
          </div>
        </div>

        <el-empty v-if="!activeDetail" description="请选择一条审批记录查看详情" />

        <template v-else>
          <div class="showroom-approval-workbench__summary">
            <div class="showroom-approval-workbench__summary-item">
              <span class="label">模块</span>
              <span>{{ activeDetail.changeRequest.moduleCode || '字段级变更' }}</span>
            </div>
            <div class="showroom-approval-workbench__summary-item">
              <span class="label">提交时间</span>
              <span>{{ activeDetail.changeRequest.submittedAt || '未记录' }}</span>
            </div>
            <div class="showroom-approval-workbench__summary-item">
              <span class="label">驳回原因</span>
              <span>{{ activeDetail.changeRequest.rejectionReason || '无' }}</span>
            </div>
          </div>

          <el-table :data="activeDetail.fieldDiffs" row-key="fieldCode">
            <el-table-column label="字段" min-width="150" prop="label" />
            <el-table-column label="旧值" min-width="200" prop="oldValue" show-overflow-tooltip />
            <el-table-column label="新值" min-width="200" prop="newValue" show-overflow-tooltip />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="resolveApprovalStatusTagType(row.approvalStatus)">
                  {{ resolveApprovalStatusText(row.approvalStatus) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="showroom-approval-workbench__section-title mt-16px">目标预览</div>
          <el-table :data="previewRows" row-key="fieldCode">
            <el-table-column label="字段" min-width="150" prop="label" />
            <el-table-column label="Live 值" min-width="220" prop="liveValue" show-overflow-tooltip />
            <el-table-column label="目标值" min-width="220" prop="targetValue" show-overflow-tooltip />
          </el-table>

          <div class="showroom-approval-workbench__section-title mt-16px">签名留痕</div>
          <el-table :data="activeDetail.signatureRecords" row-key="id" empty-text="暂无签名记录">
            <el-table-column label="阶段" width="120" prop="approvalStage" />
            <el-table-column label="动作" width="120" prop="actionType" />
            <el-table-column label="签名人" width="120" prop="actorId" />
            <el-table-column label="签名方式" width="120" prop="signatureMode" />
            <el-table-column label="签名意见" min-width="220" prop="comment" show-overflow-tooltip />
            <el-table-column label="签名时间" min-width="180" prop="signedAt" />
          </el-table>
        </template>
      </div>
    </div>

    <ShowroomApprovalSignatureDialog
      v-model="signatureDialogVisible"
      :loading="actionLoading"
      :mode="signatureDialogMode"
      :title="signatureDialogTitle"
      @confirm="handleSignatureConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ShowroomAdminApi } from '@/api/showroom-admin'
import { useUserStore } from '@/store/modules/user'
import ShowroomApprovalSignatureDialog from './ShowroomApprovalSignatureDialog.vue'
import {
  normalizeApprovalDetail,
  normalizeApprovalPage,
  resolveApprovalStatusTagType,
  resolveApprovalStatusText,
  resolveTargetTypeText,
  type ShowroomApprovalDetailRecord,
  type ShowroomChangeRequestRecord
} from './contracts'

defineOptions({ name: 'ApprovalTaskPanel' })

const message = useMessage()
const userStore = useUserStore()
const route = useRoute()
const currentUserId = computed(() => userStore.getUser.id)

const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')
const rows = ref<ShowroomChangeRequestRecord[]>([])
const activeId = ref<number | null>(null)
const activeDetail = ref<ShowroomApprovalDetailRecord | null>(null)
const signatureDialogVisible = ref(false)
const signatureDialogMode = ref<'approve' | 'reject'>('approve')
const approvalComment = ref('')

const filters = reactive({
  targetType: '',
  status: ''
})

const filteredRows = computed(() => {
  return rows.value.filter((row) => {
    const matchesTarget = !filters.targetType || row.targetType === filters.targetType
    const matchesStatus = !filters.status || row.status === filters.status
    return matchesTarget && matchesStatus
  })
})

const previewRows = computed(() => {
  return activeDetail.value?.targetPreview.rows || []
})

const canApprove = computed(() => {
  const status = activeDetail.value?.changeRequest.status || ''
  return status === 'PENDING_SUPERVISOR_REVIEW' || status === 'PENDING_GAOXIN_APPROVAL'
})

const canReject = computed(() => canApprove.value)

const approveButtonText = computed(() => {
  const status = activeDetail.value?.changeRequest.status || ''
  return status === 'PENDING_GAOXIN_APPROVAL' ? '企宣批准并发布' : '主管通过'
})

const signatureDialogTitle = computed(() => {
  const actionText = signatureDialogMode.value === 'reject' ? '驳回' : '签名'
  return `${approveButtonText.value}${actionText}`
})

const loadApprovalDetail = async (changeRequestId: number) => {
  const detail = await ShowroomAdminApi.getApproval(changeRequestId)
  activeDetail.value = normalizeApprovalDetail(detail)
}

const resolveRouteChangeRequestId = () => {
  const raw = route.query.changeRequestId
  const value = Array.isArray(raw) ? raw[0] : raw
  if (!value) {
    return null
  }
  const changeRequestId = Number(value)
  if (!Number.isInteger(changeRequestId) || changeRequestId <= 0) {
    throw new Error('统一审批中心传入的展厅审批单号无效')
  }
  return changeRequestId
}

const loadApprovals = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const page = await ShowroomAdminApi.getApprovalPage({ pageNo: 1, pageSize: 50 } as PageParam)
    rows.value = normalizeApprovalPage(page)
    const routeChangeRequestId = resolveRouteChangeRequestId()
    const nextId = activeId.value && rows.value.some((row) => row.changeRequestId === activeId.value)
      ? activeId.value
      : rows.value[0]?.changeRequestId || null
    activeId.value = routeChangeRequestId || nextId
    if (activeId.value) {
      await loadApprovalDetail(activeId.value)
    } else {
      activeDetail.value = null
    }
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    loadError.value = resolved.message
    message.error(`审批工作台加载失败：${resolved.message}`)
  } finally {
    loading.value = false
  }
}

const handleCurrentChange = async (row?: ShowroomChangeRequestRecord) => {
  if (!row) {
    activeId.value = null
    activeDetail.value = null
    return
  }
  activeId.value = row.changeRequestId
  await loadApprovalDetail(row.changeRequestId)
}

const refreshAfterAction = async () => {
  await loadApprovals()
  if (activeId.value) {
    await loadApprovalDetail(activeId.value)
  }
}

const openSignatureDialog = (mode: 'approve' | 'reject') => {
  if (!activeDetail.value || !currentUserId.value) {
    throw new Error('当前登录用户缺失，无法执行审批动作')
  }
  approvalComment.value = ''
  signatureDialogMode.value = mode
  signatureDialogVisible.value = true
}

const handleSignatureConfirm = async (payload: { password: string; comment: string }) => {
  if (!activeDetail.value || !currentUserId.value) {
    throw new Error('当前登录用户缺失，无法执行审批动作')
  }
  if (!payload.password) {
    message.error('请输入登录密码完成电子签名')
    return
  }
  approvalComment.value = payload.comment
  if (signatureDialogMode.value === 'reject' && !approvalComment.value) {
    message.error('请输入驳回原因')
    return
  }
  actionLoading.value = true
  try {
    const data = {
      id: activeDetail.value.changeRequest.changeRequestId,
      reviewerUserId: currentUserId.value,
      password: payload.password
    }
    const isPublicityStage = activeDetail.value.changeRequest.status === 'PENDING_GAOXIN_APPROVAL'
    if (signatureDialogMode.value === 'reject') {
      if (isPublicityStage) {
        await ShowroomAdminApi.gaoxinReject({
          ...data,
          reason: approvalComment.value
        })
      } else {
        await ShowroomAdminApi.supervisorReject({
          ...data,
          reason: approvalComment.value
        })
      }
      message.success('已驳回并退回发起人')
    } else if (isPublicityStage) {
      await ShowroomAdminApi.gaoxinApprove({
        ...data,
        comment: approvalComment.value || undefined
      })
      message.success('企宣审批已完成')
    } else {
      await ShowroomAdminApi.supervisorApprove({
        ...data,
        comment: approvalComment.value || undefined
      })
      message.success('主管审批已完成')
    }
    signatureDialogVisible.value = false
    await refreshAfterAction()
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  void loadApprovals()
})

watch(
  () => route.query.changeRequestId,
  () => {
    void loadApprovals()
  }
)
</script>

<style scoped>
.showroom-approval-workbench {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.showroom-approval-workbench__toolbar,
.showroom-approval-workbench__list-shell,
.showroom-approval-workbench__detail-shell {
  background: #ffffff;
  border: 1px solid #dbe3ef;
}

.showroom-approval-workbench__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 8px 8px 0 0;
}

.showroom-approval-workbench__title {
  margin: 0;
  color: #172033;
  font-size: 1.05rem;
}

.showroom-approval-workbench__subtitle {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-approval-workbench__actions {
  display: flex;
  gap: 8px;
}

.showroom-approval-workbench__body {
  display: grid;
  grid-template-columns: minmax(360px, 40%) minmax(0, 1fr);
  gap: 12px;
}

.showroom-approval-workbench__list-shell,
.showroom-approval-workbench__detail-shell {
  padding: 12px;
  border-radius: 0 0 8px 8px;
}

.showroom-approval-workbench__section-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 0.95rem;
  font-weight: 600;
}

.showroom-approval-workbench__detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.showroom-approval-workbench__detail-meta {
  margin-top: 4px;
  color: #4b5563;
  font-size: 0.85rem;
}

.showroom-approval-workbench__detail-actions {
  display: flex;
  gap: 8px;
}

.showroom-approval-workbench__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.showroom-approval-workbench__summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 68px;
  padding: 10px 12px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  color: #263247;
  font-size: 0.88rem;
}

.showroom-approval-workbench__summary-item .label {
  color: #4b5563;
  font-size: 0.8rem;
}

@media (max-width: 1100px) {
  .showroom-approval-workbench__body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .showroom-approval-workbench__toolbar,
  .showroom-approval-workbench__detail-header {
    align-items: stretch;
    flex-direction: column;
  }

  .showroom-approval-workbench__actions,
  .showroom-approval-workbench__detail-actions {
    flex-wrap: wrap;
  }

  .showroom-approval-workbench__summary {
    grid-template-columns: 1fr;
  }
}
</style>
