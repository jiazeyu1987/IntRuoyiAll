<template>
  <ContentWrap>
    <div class="portal-hero">
      <div>
        <p class="portal-hero__eyebrow">SRM Supplier Portal</p>
        <h1 class="portal-hero__title">采购订单协同确认</h1>
        <p class="portal-hero__desc">
          请逐笔确认交付数量、承诺交期和补充说明。未确认前，采购侧无法继续执行后续到货与变更协同。
        </p>
      </div>
      <el-tag type="warning" effect="dark" size="large">真实供应商路径</el-tag>
    </div>

    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="76px">
      <el-form-item label="订单编号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" clearable class="!w-190px" placeholder="请输入订单编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="orderStatus">
        <el-select v-model="queryParams.orderStatus" clearable class="!w-160px" placeholder="全部状态">
          <el-option
            v-for="item in srmPurchaseOrderStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="订单编号" prop="orderNo" width="170" />
      <el-table-column label="来源计划" prop="sourcePlanNo" width="170" />
      <el-table-column label="状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStatusType(row.orderStatus)">{{ row.orderStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="采购备注" prop="orderRemark" min-width="220" />
      <el-table-column label="最新变更" min-width="240">
        <template #default="{ row }">
          <template v-if="row.latestChange">
            <div>{{ row.latestChange.changeNo }}</div>
            <el-tag size="small" class="mt-4px" :type="row.orderStatus === 'CHANGE_PENDING' ? 'warning' : 'info'">
              {{ row.latestChange.changeStatusLabel }}
            </el-tag>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="确认人" prop="confirmedName" width="120" />
      <el-table-column label="确认时间" prop="confirmedTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button
            link
            type="success"
            :disabled="row.orderStatus !== 'PENDING_CONFIRM' && row.orderStatus !== 'CHANGE_PENDING'"
            @click="openConfirmDialog(row)"
          >
            确认
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="row.orderStatus !== 'CHANGE_PENDING' || !row.latestChange?.id"
            @click="openRejectDialog(row)"
          >
            拒绝变更
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="detailVisible" title="采购订单协同详情" width="920px">
    <template v-if="currentDetail">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="订单编号">{{ currentDetail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="来源计划">{{ currentDetail.sourcePlanNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentDetail.orderStatusLabel }}</el-descriptions-item>
        <el-descriptions-item label="采购备注" :span="3">{{ currentDetail.orderRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="确认备注" :span="3">{{ currentDetail.confirmRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentDetail.latestChange" label="最新变更单">{{ currentDetail.latestChange.changeNo }}</el-descriptions-item>
        <el-descriptions-item v-if="currentDetail.latestChange" label="变更状态">{{ currentDetail.latestChange.changeStatusLabel }}</el-descriptions-item>
        <el-descriptions-item v-if="currentDetail.latestChange" label="变更原因">{{ currentDetail.latestChange.changeReason }}</el-descriptions-item>
      </el-descriptions>
      <el-table class="mt-16px" :data="currentDetail.lines" border size="small">
        <el-table-column label="行号" prop="lineNo" width="140" />
        <el-table-column label="物料编码" prop="materialCode" width="140" />
        <el-table-column label="物料名称" prop="materialName" min-width="180" />
        <el-table-column label="需求数量" prop="requestedQuantity" width="110" align="right" />
        <el-table-column label="需求交期" prop="requestedDeliveryDate" width="120" />
        <el-table-column label="确认数量" prop="confirmedQuantity" width="110" align="right" />
        <el-table-column label="确认交期" prop="confirmedDeliveryDate" width="120" />
        <el-table-column label="供应商备注" prop="supplierRemark" min-width="180" />
        <el-table-column label="待变更数量" prop="pendingChangedQuantity" width="110" align="right" />
        <el-table-column label="待变更交期" prop="pendingChangedDeliveryDate" width="120" />
        <el-table-column label="待变更备注" prop="pendingChangedRemark" min-width="180" />
      </el-table>
    </template>
  </Dialog>

  <Dialog v-model="confirmDialogVisible" title="确认采购订单协同单" width="980px">
    <el-alert
      class="mb-16px"
      title="请按真实可交付能力填写确认数量与交期；提交后会留下确认人、确认时间和逐行备注。"
      type="warning"
      :closable="false"
    />
    <el-form ref="confirmFormRef" v-loading="confirmLoading" :model="confirmFormData" :rules="confirmRules" label-width="92px">
      <el-form-item label="确认备注" prop="confirmRemark">
        <el-input v-model="confirmFormData.confirmRemark" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
      <el-table :data="confirmFormData.lines" border size="small">
        <el-table-column label="物料编码" prop="materialCode" width="140" />
        <el-table-column label="物料名称" prop="materialName" min-width="180" />
        <el-table-column label="需求数量" prop="requestedQuantity" width="110" align="right" />
        <el-table-column label="需求交期" prop="requestedDeliveryDate" width="120" />
        <el-table-column label="确认数量" width="140">
          <template #default="{ row }">
            <el-input-number
              v-model="row.confirmedQuantity"
              :min="0.01"
              :precision="2"
              class="!w-1/1"
              controls-position="right"
            />
          </template>
        </el-table-column>
        <el-table-column label="确认交期" width="160">
          <template #default="{ row }">
            <el-date-picker
              v-model="row.confirmedDeliveryDate"
              type="date"
              value-format="YYYY-MM-DD"
              class="!w-1/1"
            />
          </template>
        </el-table-column>
        <el-table-column label="供应商备注" min-width="200">
          <template #default="{ row }">
            <el-input v-model="row.supplierRemark" maxlength="500" placeholder="请输入补充说明" />
          </template>
        </el-table-column>
      </el-table>
    </el-form>
    <template #footer>
      <el-button @click="confirmDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="confirmLoading" @click="submitConfirm">提交确认</el-button>
    </template>
  </Dialog>

  <Dialog v-model="rejectDialogVisible" title="拒绝订单变更" width="520px">
    <el-form ref="rejectFormRef" v-loading="rejectLoading" :model="rejectFormData" :rules="rejectRules" label-width="88px">
      <el-form-item label="拒绝原因" prop="rejectRemark">
        <el-input v-model="rejectFormData.rejectRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rejectDialogVisible = false">取消</el-button>
      <el-button type="danger" :loading="rejectLoading" @click="submitRejectChange">确认拒绝</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import { dateFormatter } from '@/utils/formatTime'
import {
  SrmPurchaseOrderApi,
  srmPurchaseOrderStatusOptions,
  type SrmPurchaseOrderConfirmReqVO,
  type SrmPurchaseOrderVO
} from '@/api/srm/purchase-order'

defineOptions({ name: 'SrmMyPurchaseOrder' })

const message = useMessage()
const loading = ref(false)
const list = ref<SrmPurchaseOrderVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: undefined as string | undefined,
  orderStatus: undefined as string | undefined
})

const detailVisible = ref(false)
const currentDetail = ref<SrmPurchaseOrderVO>()
const confirmDialogVisible = ref(false)
const confirmLoading = ref(false)
const confirmFormRef = ref<FormInstance>()
const confirmFormData = reactive<
  SrmPurchaseOrderConfirmReqVO & {
    lines: Array<{
      orderLineId: number
      materialCode?: string
      materialName?: string
      requestedQuantity?: number
      requestedDeliveryDate?: string
      confirmedQuantity: number
      confirmedDeliveryDate: string
      supplierRemark?: string
    }>
  }
>({
  id: undefined as unknown as number,
  confirmRemark: '',
  lines: []
})
const confirmRules = reactive<FormRules>({
  confirmRemark: [{ required: true, message: '请填写确认备注', trigger: 'blur' }]
})
const rejectDialogVisible = ref(false)
const rejectLoading = ref(false)
const rejectFormRef = ref<FormInstance>()
const rejectFormData = reactive({
  changeId: undefined as unknown as number,
  rejectRemark: ''
})
const rejectRules = reactive<FormRules>({
  rejectRemark: [{ required: true, message: '请填写拒绝原因', trigger: 'blur' }]
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveStatusType = (status?: string) => {
  if (status === 'CONFIRMED') {
    return 'success'
  }
  if (status === 'PENDING_CONFIRM' || status === 'CHANGE_PENDING') {
    return 'warning'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  return 'info'
}

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmPurchaseOrderApi.getMyPurchaseOrderPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购订单协同列表加载失败，请检查供应商登录与后端接口。'))
    throw error
  } finally {
    loading.value = false
  }
}

const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery(true)
}

const openDetail = async (row: SrmPurchaseOrderVO) => {
  if (!row.id) return
  try {
    currentDetail.value = await SrmPurchaseOrderApi.getMyPurchaseOrder(row.id)
    detailVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购订单协同详情加载失败。'))
    throw error
  }
}

const openConfirmDialog = async (row: SrmPurchaseOrderVO) => {
  if (!row.id) return
  const detail = await SrmPurchaseOrderApi.getMyPurchaseOrder(row.id)
  Object.assign(confirmFormData, {
    id: detail.id,
    confirmRemark: detail.confirmRemark || '',
    lines: (detail.lines || []).map((item) => ({
      orderLineId: item.id!,
      materialCode: item.materialCode,
      materialName: item.materialName,
      requestedQuantity: item.requestedQuantity,
      requestedDeliveryDate: item.requestedDeliveryDate,
      confirmedQuantity: item.confirmedQuantity || item.requestedQuantity || 0,
      confirmedDeliveryDate: item.confirmedDeliveryDate || item.requestedDeliveryDate || '',
      supplierRemark: item.supplierRemark || ''
    }))
  })
  confirmDialogVisible.value = true
}

const openRejectDialog = (row: SrmPurchaseOrderVO) => {
  if (!row.latestChange?.id) return
  Object.assign(rejectFormData, {
    changeId: row.latestChange.id,
    rejectRemark: ''
  })
  rejectDialogVisible.value = true
}

const submitConfirm = async () => {
  await confirmFormRef.value?.validate()
  if (!confirmFormData.id) return
  const invalidLine = confirmFormData.lines.some((item) => !item.confirmedQuantity || !item.confirmedDeliveryDate)
  if (invalidLine) {
    message.warning('请完整填写每一行的确认数量和确认交期')
    return
  }
  confirmLoading.value = true
  try {
    await SrmPurchaseOrderApi.confirmMyPurchaseOrder({
      id: confirmFormData.id,
      confirmRemark: confirmFormData.confirmRemark,
      lines: confirmFormData.lines.map((item) => ({
        orderLineId: item.orderLineId,
        confirmedQuantity: item.confirmedQuantity,
        confirmedDeliveryDate: item.confirmedDeliveryDate,
        supplierRemark: item.supplierRemark
      }))
    })
    confirmDialogVisible.value = false
    message.success('采购订单协同单已确认')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购订单协同确认失败。'))
    throw error
  } finally {
    confirmLoading.value = false
  }
}

const submitRejectChange = async () => {
  await rejectFormRef.value?.validate()
  rejectLoading.value = true
  try {
    await SrmPurchaseOrderApi.rejectMyOrderChange({
      changeId: rejectFormData.changeId,
      rejectRemark: rejectFormData.rejectRemark
    })
    rejectDialogVisible.value = false
    message.success('订单变更已拒绝')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '订单变更拒绝失败。'))
    throw error
  } finally {
    rejectLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.portal-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  margin-bottom: 20px;
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(12, 90, 166, 0.12), rgba(18, 167, 145, 0.1)),
    #fff;
}

.portal-hero__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #0c5aa6;
}

.portal-hero__title {
  margin: 0 0 10px;
  font-size: 28px;
  line-height: 1.2;
  color: #10233a;
}

.portal-hero__desc {
  margin: 0;
  max-width: 760px;
  color: #5c6b7a;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .portal-hero {
    flex-direction: column;
  }
}
</style>
