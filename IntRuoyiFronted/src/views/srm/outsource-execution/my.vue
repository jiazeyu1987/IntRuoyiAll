<template>
  <ContentWrap>
    <div class="portal-hero">
      <div>
        <p class="portal-hero__eyebrow">SRM Supplier Portal</p>
        <h1 class="portal-hero__title">委外执行协同台</h1>
        <p class="portal-hero__desc">
          供应商可在测试租户下回传加工进度与送收货结果。页面会显式标出“测试租户受控模拟链路”，避免和真实 PDA / 仓储系统混淆。
        </p>
      </div>
      <el-tag type="warning" effect="dark" size="large">受控模拟链路</el-tag>
    </div>

    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="76px">
      <el-form-item label="委外单号" prop="executionNo">
        <el-input v-model="queryParams.executionNo" clearable class="!w-180px" placeholder="请输入委外单号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="采购订单" prop="purchaseOrderNo">
        <el-input
          v-model="queryParams.purchaseOrderNo"
          clearable
          class="!w-190px"
          placeholder="请输入采购订单号"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="executionStatus">
        <el-select v-model="queryParams.executionStatus" clearable class="!w-170px" placeholder="全部状态">
          <el-option v-for="item in srmOutsourceExecutionStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="委外单号" prop="executionNo" width="170" />
      <el-table-column label="采购订单" prop="sourcePurchaseOrderNo" width="170" />
      <el-table-column label="状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStatusType(row.executionStatus)">{{ row.executionStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="模拟来源" width="180">
        <template #default="{ row }">
          <el-tag type="warning" effect="plain">{{ row.simulationLabel || '测试租户受控模拟链路' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="计划 / 发料" width="130" align="right">
        <template #default="{ row }">{{ row.plannedQuantity || 0 }} / {{ row.issueQuantity || 0 }}</template>
      </el-table-column>
      <el-table-column label="进度" width="150">
        <template #default="{ row }">
          <div>{{ row.progressStage || '-' }}</div>
          <el-progress class="mt-6px" :percentage="Number(row.progressPercent || 0)" :stroke-width="10" />
        </template>
      </el-table-column>
      <el-table-column label="收货数量" prop="receivedQuantity" width="110" align="right" />
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="success" :disabled="row.executionStatus !== 'IN_PRODUCTION'" @click="openProgressDialog(row)">
            回传进度
          </el-button>
          <el-button link type="warning" :disabled="row.executionStatus !== 'IN_PRODUCTION'" @click="openReceiveDialog(row)">
            回传收货
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="progressVisible" title="回传加工进度" width="560px">
    <el-form ref="progressFormRef" :model="progressForm" :rules="progressRules" label-width="100px">
      <el-form-item label="进度百分比" prop="progressPercent">
        <el-input-number v-model="progressForm.progressPercent" :min="0" :max="100" :precision="0" class="!w-1/1" controls-position="right" />
      </el-form-item>
      <el-form-item label="进度阶段" prop="progressStage">
        <el-input v-model="progressForm.progressStage" placeholder="例如：加工中 / 已排产 / 待送货" />
      </el-form-item>
      <el-form-item label="补充说明" prop="progressRemark">
        <el-input v-model="progressForm.progressRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="progressVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitProgress">提交进度</el-button>
    </template>
  </Dialog>

  <Dialog v-model="receiveVisible" title="回传送收货结果" width="560px">
    <el-form ref="receiveFormRef" :model="receiveForm" :rules="receiveRules" label-width="100px">
      <el-form-item label="收货数量" prop="receivedQuantity">
        <el-input-number v-model="receiveForm.receivedQuantity" :min="0.01" :precision="2" class="!w-1/1" controls-position="right" />
      </el-form-item>
      <el-form-item label="补充说明" prop="receiveRemark">
        <el-input v-model="receiveForm.receiveRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="receiveVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitReceive">提交收货</el-button>
    </template>
  </Dialog>

  <Dialog v-model="detailVisible" title="委外执行详情" width="980px">
    <template v-if="currentDetail">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="委外单号">{{ currentDetail.executionNo }}</el-descriptions-item>
        <el-descriptions-item label="采购订单">{{ currentDetail.sourcePurchaseOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{ currentDetail.executionStatusLabel }}</el-descriptions-item>
        <el-descriptions-item label="模拟来源">{{ currentDetail.simulationLabel }}</el-descriptions-item>
        <el-descriptions-item label="进度">{{ currentDetail.progressStage || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货数量">{{ currentDetail.receivedQuantity || 0 }}</el-descriptions-item>
      </el-descriptions>
      <el-table class="mt-16px" :data="currentDetail.events" border size="small">
        <el-table-column label="事件单号" prop="eventNo" width="160" />
        <el-table-column label="事件类型" prop="eventTypeLabel" width="140" />
        <el-table-column label="操作人" prop="operatorName" width="120" />
        <el-table-column label="事件说明" prop="eventRemark" min-width="220" />
        <el-table-column label="事件时间" prop="eventTime" width="180" :formatter="dateTimeValueFormatter" />
      </el-table>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import { dateTimeValueFormatter } from '@/utils/formatTime'
import {
  SrmOutsourceExecutionApi,
  srmOutsourceExecutionStatusOptions,
  type SrmOutsourceExecutionVO
} from '@/api/srm/outsource-execution'

defineOptions({ name: 'SrmOutsourceExecutionMy' })

const message = useMessage()
const loading = ref(false)
const actionLoading = ref(false)
const list = ref<SrmOutsourceExecutionVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  executionNo: undefined as string | undefined,
  purchaseOrderNo: undefined as string | undefined,
  executionStatus: undefined as string | undefined
})

const progressVisible = ref(false)
const progressFormRef = ref<FormInstance>()
const progressForm = reactive({
  id: undefined as unknown as number,
  progressPercent: 0,
  progressStage: '加工中',
  progressRemark: '模拟进度回传'
})
const progressRules = reactive<FormRules>({
  progressPercent: [{ required: true, message: '请输入进度百分比', trigger: 'change' }],
  progressStage: [{ required: true, message: '请输入进度阶段', trigger: 'blur' }]
})

const receiveVisible = ref(false)
const receiveFormRef = ref<FormInstance>()
const receiveForm = reactive({
  id: undefined as unknown as number,
  receivedQuantity: 0,
  receiveRemark: '模拟收货回传'
})
const receiveRules = reactive<FormRules>({
  receivedQuantity: [{ required: true, message: '请输入收货数量', trigger: 'change' }]
})

const detailVisible = ref(false)
const currentDetail = ref<SrmOutsourceExecutionVO>()

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveStatusType = (status?: string) => {
  if (status === 'RECONCILED') return 'success'
  if (status === 'PENDING_ISSUE' || status === 'INSPECTED') return 'warning'
  if (status === 'IN_PRODUCTION' || status === 'DELIVERED') return 'primary'
  return 'info'
}

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmOutsourceExecutionApi.getMyOutsourceExecutionPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '委外执行列表加载失败，请检查供应商登录与后端接口。'))
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

const openProgressDialog = (row: SrmOutsourceExecutionVO) => {
  progressForm.id = row.id!
  progressForm.progressPercent = Number(row.progressPercent || 0)
  progressForm.progressStage = row.progressStage || '加工中'
  progressForm.progressRemark = '模拟进度回传'
  progressVisible.value = true
}

const submitProgress = async () => {
  await progressFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmOutsourceExecutionApi.updateProgress(progressForm)
    progressVisible.value = false
    message.success('加工进度已回传')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '加工进度回传失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openReceiveDialog = (row: SrmOutsourceExecutionVO) => {
  receiveForm.id = row.id!
  receiveForm.receivedQuantity = Number(row.issueQuantity || row.plannedQuantity || 0)
  receiveForm.receiveRemark = '模拟收货回传'
  receiveVisible.value = true
}

const submitReceive = async () => {
  await receiveFormRef.value?.validate()
  actionLoading.value = true
  try {
    await SrmOutsourceExecutionApi.receive(receiveForm)
    receiveVisible.value = false
    message.success('送收货结果已回传')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '送收货结果回传失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openDetail = async (row: SrmOutsourceExecutionVO) => {
  if (!row.id) return
  try {
    currentDetail.value = await SrmOutsourceExecutionApi.getMyOutsourceExecution(row.id)
    detailVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '委外执行详情加载失败。'))
    throw error
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
    linear-gradient(135deg, rgba(183, 109, 0, 0.14), rgba(210, 160, 34, 0.08)),
    #fff;
}

.portal-hero__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #9a5a00;
}

.portal-hero__title {
  margin: 0 0 10px;
  font-size: 28px;
  line-height: 1.2;
  color: #3b2a18;
}

.portal-hero__desc {
  margin: 0;
  max-width: 760px;
  color: #6b5a49;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .portal-hero {
    flex-direction: column;
  }
}
</style>
