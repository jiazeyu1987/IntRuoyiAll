<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="88px">
      <el-form-item label="订单编号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" clearable class="!w-190px" placeholder="请输入订单编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="来源计划" prop="sourcePlanNo">
        <el-input
          v-model="queryParams.sourcePlanNo"
          clearable
          class="!w-200px"
          placeholder="请输入采购计划编号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierName">
        <el-input
          v-model="queryParams.supplierName"
          clearable
          class="!w-200px"
          placeholder="请输入供应商名称"
          @keyup.enter="handleQuery"
        />
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
        <el-button type="primary" plain @click="openCreateDialog" v-hasPermi="['srm:purchase-order:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 生成协同单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="订单编号" prop="orderNo" width="170" />
      <el-table-column label="来源计划" prop="sourcePlanNo" width="170" />
      <el-table-column label="供应商" prop="supplierName" min-width="180" />
      <el-table-column label="状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStatusType(row.orderStatus)">{{ row.orderStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="确认人" prop="confirmedName" width="120" />
      <el-table-column label="确认时间" prop="confirmedTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="订单备注" prop="orderRemark" min-width="180" />
      <el-table-column label="最新变更" min-width="220">
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
      <el-table-column label="行数" width="80" align="center">
        <template #default="{ row }">{{ row.lines?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" width="100" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="warning" :disabled="row.orderStatus !== 'CONFIRMED'" @click="openChangeDialog(row)">
            发起变更
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="row.orderStatus !== 'CHANGE_PENDING' || !row.latestChange?.id"
            @click="openWithdrawDialog(row)"
          >
            撤回变更
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="createDialogVisible" title="生成采购订单协同单" width="720px">
    <el-alert
      class="mb-16px"
      title="只能从已审核通过或已生成项目的采购计划生成协同单；供应商必须已经通过准入并可用。"
      type="info"
      :closable="false"
    />
    <el-form ref="createFormRef" v-loading="createLoading" :model="createFormData" :rules="createRules" label-width="102px">
      <el-form-item label="来源采购计划" prop="sourcePlanId">
        <el-select
          v-model="createFormData.sourcePlanId"
          filterable
          class="!w-1/1"
          placeholder="请选择已审核采购计划"
        >
          <el-option
            v-for="item in selectablePlans"
            :key="item.id"
            :label="`${item.planNo} | ${item.planTitle}`"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="协同供应商" prop="supplierId">
        <el-select
          v-model="createFormData.supplierId"
          filterable
          remote
          reserve-keyword
          class="!w-1/1"
          placeholder="请输入供应商名称搜索"
          :remote-method="loadSuppliers"
          :loading="supplierLoading"
        >
          <el-option
            v-for="item in supplierOptions"
            :key="item.id"
            :label="`${item.name}（ID:${item.id}）`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="订单备注" prop="orderRemark">
        <el-input v-model="createFormData.orderRemark" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="createLoading" @click="submitCreate">生成</el-button>
    </template>
  </Dialog>

  <Dialog v-model="detailVisible" title="采购订单协同详情" width="960px">
    <template v-if="currentDetail">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="订单编号">{{ currentDetail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="来源计划">{{ currentDetail.sourcePlanNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ currentDetail.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentDetail.orderStatusLabel }}</el-descriptions-item>
        <el-descriptions-item label="确认人">{{ currentDetail.confirmedName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ currentDetail.confirmedTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单备注" :span="3">{{ currentDetail.orderRemark || '-' }}</el-descriptions-item>
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
        <el-table-column label="单位" prop="unit" width="80" align="center" />
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

  <Dialog v-model="changeDialogVisible" title="发起订单变更" width="980px">
    <el-alert
      class="mb-16px"
      title="变更申请会先保留原确认值，待供应商确认后才会正式覆盖交期和数量。"
      type="warning"
      :closable="false"
    />
    <el-form ref="changeFormRef" v-loading="changeLoading" :model="changeFormData" :rules="changeRules" label-width="102px">
      <el-form-item label="变更原因" prop="changeReason">
        <el-input v-model="changeFormData.changeReason" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="采购补充说明" prop="changeRemark">
        <el-input v-model="changeFormData.changeRemark" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
      <el-table :data="changeFormData.lines" border size="small">
        <el-table-column label="物料编码" prop="materialCode" width="140" />
        <el-table-column label="物料名称" prop="materialName" min-width="180" />
        <el-table-column label="原确认数量" prop="confirmedQuantity" width="110" align="right" />
        <el-table-column label="原确认交期" prop="confirmedDeliveryDate" width="120" />
        <el-table-column label="变更数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.changedQuantity" :min="0.01" :precision="2" class="!w-1/1" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="变更交期" width="160">
          <template #default="{ row }">
            <el-date-picker v-model="row.changedDeliveryDate" type="date" value-format="YYYY-MM-DD" class="!w-1/1" />
          </template>
        </el-table-column>
        <el-table-column label="变更备注" min-width="200">
          <template #default="{ row }">
            <el-input v-model="row.changedSupplierRemark" maxlength="500" placeholder="请输入变更备注" />
          </template>
        </el-table-column>
      </el-table>
    </el-form>
    <template #footer>
      <el-button @click="changeDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="changeLoading" @click="submitOrderChange">提交变更</el-button>
    </template>
  </Dialog>

  <Dialog v-model="withdrawDialogVisible" title="撤回订单变更" width="520px">
    <el-form ref="withdrawFormRef" v-loading="withdrawLoading" :model="withdrawFormData" :rules="withdrawRules" label-width="88px">
      <el-form-item label="撤回原因" prop="withdrawRemark">
        <el-input v-model="withdrawFormData.withdrawRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="withdrawDialogVisible = false">取消</el-button>
      <el-button type="danger" :loading="withdrawLoading" @click="submitWithdrawChange">确认撤回</el-button>
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
  type SrmPurchaseOrderChangeReqVO,
  type SrmPurchaseOrderCreateReqVO,
  type SrmPurchaseOrderVO
} from '@/api/srm/purchase-order'
import { SrmProcurementPlanApi, type SrmProcurementPlanVO } from '@/api/srm/procurement-plan'
import { SrmSupplierAccessApi, type SrmSupplierReferenceVO } from '@/api/srm/supplier-access'

defineOptions({ name: 'SrmPurchaseOrder' })

const message = useMessage()
const loading = ref(false)
const list = ref<SrmPurchaseOrderVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: undefined as string | undefined,
  sourcePlanNo: undefined as string | undefined,
  supplierName: undefined as string | undefined,
  orderStatus: undefined as string | undefined
})

const selectablePlans = ref<SrmProcurementPlanVO[]>([])
const supplierOptions = ref<SrmSupplierReferenceVO[]>([])
const supplierLoading = ref(false)
const createDialogVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createFormData = reactive<SrmPurchaseOrderCreateReqVO>({
  sourcePlanId: undefined as unknown as number,
  supplierId: undefined as unknown as number,
  orderRemark: ''
})
const createRules = reactive<FormRules>({
  sourcePlanId: [{ required: true, message: '请选择来源采购计划', trigger: 'change' }],
  supplierId: [{ required: true, message: '请选择协同供应商', trigger: 'change' }]
})

const detailVisible = ref(false)
const currentDetail = ref<SrmPurchaseOrderVO>()
const changeDialogVisible = ref(false)
const changeLoading = ref(false)
const changeFormRef = ref<FormInstance>()
const changeFormData = reactive<
  SrmPurchaseOrderChangeReqVO & {
    lines: Array<{
      orderLineId: number
      materialCode?: string
      materialName?: string
      confirmedQuantity?: number
      confirmedDeliveryDate?: string
      changedQuantity: number
      changedDeliveryDate: string
      changedSupplierRemark?: string
    }>
  }
>({
  orderId: undefined as unknown as number,
  changeReason: '',
  changeRemark: '',
  lines: []
})
const changeRules = reactive<FormRules>({
  changeReason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }]
})
const withdrawDialogVisible = ref(false)
const withdrawLoading = ref(false)
const withdrawFormRef = ref<FormInstance>()
const withdrawFormData = reactive({
  changeId: undefined as unknown as number,
  withdrawRemark: ''
})
const withdrawRules = reactive<FormRules>({
  withdrawRemark: [{ required: true, message: '请输入撤回原因', trigger: 'blur' }]
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
    const data = await SrmPurchaseOrderApi.getPurchaseOrderPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购订单协同列表加载失败，请检查后端接口。'))
    throw error
  } finally {
    loading.value = false
  }
}

const loadPlans = async () => {
  const data = await SrmProcurementPlanApi.getProcurementPlanPage({ pageNo: 1, pageSize: 100 })
  selectablePlans.value = (data.list || []).filter(
    (item) => item.planStatus === 'APPROVED' || item.planStatus === 'GENERATED'
  )
}

const loadSuppliers = async (keyword?: string) => {
  supplierLoading.value = true
  try {
    supplierOptions.value = await SrmSupplierAccessApi.getReferenceSuppliers(keyword)
  } finally {
    supplierLoading.value = false
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

const openCreateDialog = async () => {
  Object.assign(createFormData, {
    sourcePlanId: undefined,
    supplierId: undefined,
    orderRemark: ''
  })
  await Promise.all([loadPlans(), loadSuppliers('')])
  createDialogVisible.value = true
}

const submitCreate = async () => {
  await createFormRef.value?.validate()
  createLoading.value = true
  try {
    const id = await SrmPurchaseOrderApi.createFromPlan(createFormData)
    createDialogVisible.value = false
    message.success('采购订单协同单已生成')
    await getList()
    if (id) {
      currentDetail.value = await SrmPurchaseOrderApi.getPurchaseOrder(id)
      detailVisible.value = true
    }
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购订单协同单生成失败。'))
    throw error
  } finally {
    createLoading.value = false
  }
}

const openDetail = async (row: SrmPurchaseOrderVO) => {
  if (!row.id) return
  try {
    currentDetail.value = await SrmPurchaseOrderApi.getPurchaseOrder(row.id)
    detailVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '采购订单协同详情加载失败。'))
    throw error
  }
}

const openChangeDialog = async (row: SrmPurchaseOrderVO) => {
  if (!row.id) return
  const detail = await SrmPurchaseOrderApi.getPurchaseOrder(row.id)
  Object.assign(changeFormData, {
    orderId: detail.id,
    changeReason: '',
    changeRemark: '',
    lines: (detail.lines || []).map((item) => ({
      orderLineId: item.id!,
      materialCode: item.materialCode,
      materialName: item.materialName,
      confirmedQuantity: item.confirmedQuantity,
      confirmedDeliveryDate: item.confirmedDeliveryDate,
      changedQuantity: item.confirmedQuantity || item.requestedQuantity || 0,
      changedDeliveryDate: item.confirmedDeliveryDate || item.requestedDeliveryDate || '',
      changedSupplierRemark: item.pendingChangedRemark || item.supplierRemark || ''
    }))
  })
  changeDialogVisible.value = true
}

const submitOrderChange = async () => {
  await changeFormRef.value?.validate()
  const invalidLine = changeFormData.lines.some((item) => !item.changedQuantity || !item.changedDeliveryDate)
  if (invalidLine) {
    message.warning('请完整填写每一行的变更数量和变更交期')
    return
  }
  changeLoading.value = true
  try {
    await SrmPurchaseOrderApi.submitOrderChange({
      orderId: changeFormData.orderId,
      changeReason: changeFormData.changeReason,
      changeRemark: changeFormData.changeRemark,
      lines: changeFormData.lines.map((item) => ({
        orderLineId: item.orderLineId,
        changedQuantity: item.changedQuantity,
        changedDeliveryDate: item.changedDeliveryDate,
        changedSupplierRemark: item.changedSupplierRemark
      }))
    })
    changeDialogVisible.value = false
    message.success('订单变更申请已提交')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '订单变更提交失败。'))
    throw error
  } finally {
    changeLoading.value = false
  }
}

const openWithdrawDialog = (row: SrmPurchaseOrderVO) => {
  if (!row.latestChange?.id) return
  Object.assign(withdrawFormData, {
    changeId: row.latestChange.id,
    withdrawRemark: ''
  })
  withdrawDialogVisible.value = true
}

const submitWithdrawChange = async () => {
  await withdrawFormRef.value?.validate()
  withdrawLoading.value = true
  try {
    await SrmPurchaseOrderApi.withdrawOrderChange({
      changeId: withdrawFormData.changeId,
      withdrawRemark: withdrawFormData.withdrawRemark
    })
    withdrawDialogVisible.value = false
    message.success('订单变更已撤回')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '订单变更撤回失败。'))
    throw error
  } finally {
    withdrawLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
