<!-- MES 生产报工表单 -->
<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="960px">
    <div v-if="isApprovalContextVisible" class="feedback-form-approval-context">
      <strong>A5 审批职责：提交正式报工后回写排产进度；审批仅确认质量/合规结果。</strong>
      <span>审批影响：{{ formData.approvalImpactText || '通过后进入质检/入库/完成判断，不会改变归属工序或重复扣减剩余数量。' }}</span>
      <span>当前审批人：{{ formData.approveUserNickname || formData.approveUserId || '-' }}</span>
      <span v-if="formData.sourceImportFileName">来源文件：{{ formData.sourceImportFileName }}</span>
      <span v-if="formData.sourceImportRecordId">
        来源导入行：#{{ formData.sourceImportRecordId }}
        {{ formData.sourceImportSheetName || '-' }} / {{ formData.sourceImportRowNo || '-' }}
      </span>
    </div>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
      :disabled="isDetail"
    >
      <!-- 基本信息 -->
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="报工单号" prop="code">
            <el-input
              v-model="formData.code"
              placeholder="请输入报工单号"
              :disabled="isHeaderReadonly"
            >
              <template #append>
                <el-button @click="generateCode"> 生成 </el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="报工类型" prop="type">
            <el-select
              v-model="formData.type"
              placeholder="请选择报工类型"
              :disabled="isHeaderReadonly"
              class="!w-1/1"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.MES_PRO_FEEDBACK_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 工单 / 任务 / 工作站 -->
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="生产工单" prop="workOrderId">
            <ProWorkOrderSelect
              v-model="formData.workOrderId"
              :status="MesProWorkOrderStatusEnum.CONFIRMED"
              :disabled="isHeaderReadonly"
              placeholder="请选择工单"
              @change="handleWorkOrderChange"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="生产任务" prop="taskId">
            <ProTaskSelect
              v-model="formData.taskId"
              :workOrderId="formData.workOrderId"
              :workstationId="formData.workstationId"
              :statuses="[
                MesProTaskStatusEnum.PREPARE
              ]"
              :disabled="isHeaderReadonly || !formData.workOrderId"
              placeholder="请选择任务"
              @change="handleTaskChange"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工作站" prop="workstationId">
            <MdWorkstationSelect
              v-model="formData.workstationId"
              :disabled="isHeaderReadonly"
              placeholder="请选择工作站"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 产品信息（只读展示） -->
      <el-row :gutter="20" v-if="productInfo.itemCode">
        <el-col :span="8">
          <el-form-item label="产品编码">
            <el-input :model-value="productInfo.itemCode" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="产品名称">
            <el-input :model-value="productInfo.itemName" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="单位" label-width="60px">
            <el-input :model-value="productInfo.unitMeasureName" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="规格" label-width="60px">
            <el-input :model-value="productInfo.itemSpecification" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 数量区域 -->
      <el-divider content-position="left">报工数量</el-divider>
      <!-- 非质检工序：报工数量(只读自动计算) + 合格品 + 不良品 -->
      <el-row :gutter="20" v-if="!checkFlag">
        <el-col :span="8">
          <el-form-item label="报工数量" prop="feedbackQuantity">
            <el-input-number
              v-model="formData.feedbackQuantity"
              :min="0"
              :precision="2"
              disabled
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="合格品数量" prop="qualifiedQuantity">
            <el-input-number
              v-model="formData.qualifiedQuantity"
              :min="0"
              :precision="2"
              class="!w-1/1"
              @change="handleQuantityChanged"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="不良品数量" prop="unqualifiedQuantity">
            <el-input-number
              v-model="formData.unqualifiedQuantity"
              :min="0"
              :precision="2"
              class="!w-1/1"
              @change="handleQuantityChanged"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 质检工序：只填报工数量 -->
      <el-row :gutter="20" v-else>
        <el-col :span="8">
          <el-form-item label="报工数量" prop="feedbackQuantity">
            <el-input-number
              v-model="formData.feedbackQuantity"
              :min="0"
              :precision="2"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 废品分类（非质检工序 且 不良品>0 时展开） -->
      <el-row :gutter="20" v-if="!checkFlag && formData.unqualifiedQuantity > 0">
        <el-col :span="8">
          <el-form-item label="工废数量">
            <el-input-number
              v-model="formData.laborScrapQuantity"
              :min="0"
              :precision="2"
              class="!w-1/1"
              @change="handleScrapChanged"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="料废数量">
            <el-input-number
              v-model="formData.materialScrapQuantity"
              :min="0"
              :precision="2"
              class="!w-1/1"
              @change="handleScrapChanged"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="其他废品">
            <el-input-number
              v-model="formData.otherScrapQuantity"
              :min="0"
              :precision="2"
              class="!w-1/1"
              @change="handleScrapChanged"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 报工人 / 报工时间 / 当前审批人 -->
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="报工人" prop="feedbackUserId">
            <UserSelectV2
              v-model="formData.feedbackUserId"
              :disabled="isHeaderReadonly"
              placeholder="请选择报工人"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="报工时间" prop="feedbackTime">
            <el-date-picker
              v-model="formData.feedbackTime"
              type="datetime"
              value-format="x"
              placeholder="请选择报工时间"
              :disabled="isHeaderReadonly"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="当前审批人" prop="approveUserId">
            <UserSelectV2
              v-model="formData.approveUserId"
              :disabled="isHeaderReadonly"
              placeholder="请选择当前审批人"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 备注 -->
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <!-- BOM 物资消耗 / 产品产出 Tab：仅非草稿、非审批中时显示 -->
    <el-tabs
      v-if="
        formData.id &&
        formData.status !== MesProFeedbackStatusEnum.PREPARE &&
        formData.status !== MesProFeedbackStatusEnum.APPROVING
      "
      type="border-card"
      class="mt-10px"
    >
      <el-tab-pane label="BOM 物资消耗">
        <ItemConsumeList :feedbackId="formData.id" />
      </el-tab-pane>
      <el-tab-pane label="产品产出">
        <ProductProduceList :feedbackId="formData.id" />
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <el-button
        v-if="formType !== 'approve'"
        @click="handleOpenEdhrBatchExecution"
        :disabled="formLoading"
      >
        查看批次执行
      </el-button>
      <el-button
        v-if="formType !== 'approve'"
        type="primary"
        plain
        @click="handleOpenEdhr"
        :loading="edhrOpening"
        :disabled="formLoading"
      >
        打开 eDHR
      </el-button>
      <el-button v-if="isEditable" @click="submitForm" type="primary" :disabled="formLoading">
        保 存
      </el-button>
      <el-button
        v-if="isEditable && formData.status === MesProFeedbackStatusEnum.PREPARE"
        @click="handleSubmit"
        type="warning"
        :disabled="formLoading"
      >
        提 交
      </el-button>
      <el-button
        v-if="formType === 'approve'"
        type="success"
        @click="handleApprove"
        :disabled="formLoading"
      >
        通 过
      </el-button>
      <el-button
        v-if="formType === 'approve'"
        type="danger"
        @click="handleReject"
        :disabled="formLoading"
      >
        不通过
      </el-button>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import {
  ProFeedbackApi,
  type ProFeedbackEdhrEntryContextReqVO,
  type ProFeedbackVO
} from '@/api/mes/pro/feedback'
import { AutoCodeRecordApi } from '@/api/mes/md/autocode/record'
import { ProRouteProcessApi } from '@/api/mes/pro/route/process'
import { ProWorkOrderApi, type ProWorkOrderVO } from '@/api/mes/pro/workorder'
import { openOrCreateManualEdhrBatchExecution } from '@/api/mes/pro/edhr/batchExecution'
import ProWorkOrderSelect from '@/views/mes/pro/workorder/components/ProWorkOrderSelect.vue'
import ProTaskSelect from '@/views/mes/pro/task/components/ProTaskSelect.vue'
import MdWorkstationSelect from '@/views/mes/md/workstation/components/MdWorkstationSelect.vue'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import ItemConsumeList from './ItemConsumeList.vue'
import ProductProduceList from './ProductProduceList.vue'
import { useUserStore } from '@/store/modules/user'
import {
  MesAutoCodeRuleCode,
  MesProFeedbackStatusEnum,
  MesProTaskStatusEnum,
  MesProWorkOrderStatusEnum
} from '@/views/mes/utils/constants'

defineOptions({ name: 'FeedbackForm' })
const emit = defineEmits(['success'])

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const router = useRouter()

const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中
const edhrOpening = ref(false)
const formType = ref<string>('create') // 表单的类型：create / update / submit / approve / detail
const isEditable = computed(() => ['create', 'update', 'submit'].includes(formType.value))
const isDetail = computed(() => ['detail', 'approve'].includes(formType.value))
const isHeaderReadonly = computed(() => ['submit', 'detail', 'approve'].includes(formType.value))
const isApprovalContextVisible = computed(() => ['approve', 'detail'].includes(formType.value))
const isImportedPrepareDraft = computed(
  () =>
    !!formData.value.sourceImportRecordId &&
    formData.value.status === MesProFeedbackStatusEnum.PREPARE
)
const dialogTitle = computed(() => {
  const titles: Record<string, string> = {
    create: '新增生产报工',
    update: '编辑生产报工',
    submit: '提交生产报工',
    approve: '审批生产报工',
    detail: '生产报工详情'
  }
  return titles[formType.value] || formType.value
})
const formData = ref<Record<string, any>>({
  id: undefined,
  code: undefined,
  type: undefined,
  workstationId: undefined,
  workstationCode: undefined,
  workstationName: undefined,
  routeId: undefined,
  routeCode: undefined,
  routeName: undefined,
  processId: undefined,
  processCode: undefined,
  processName: undefined,
  workOrderId: undefined,
  workOrderCode: undefined,
  workOrderName: undefined,
  batchCode: undefined,
  taskId: undefined,
  taskCode: undefined,
  itemId: undefined,
  expireDate: undefined,
  feedbackQuantity: 0,
  qualifiedQuantity: 0,
  unqualifiedQuantity: 0,
  uncheckQuantity: 0,
  laborScrapQuantity: 0,
  materialScrapQuantity: 0,
  otherScrapQuantity: 0,
  feedbackUserId: undefined,
  feedbackTime: undefined,
  approveUserId: undefined,
  status: undefined,
  remark: undefined,
  sourceImportRecordId: undefined,
  sourceImportFileName: undefined,
  sourceImportSheetName: undefined,
  sourceImportRowNo: undefined,
  sourceImportAttributionTime: undefined,
  approvalImpactText: undefined
})
const formRules = reactive({
  code: [{ required: true, message: '报工单号不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '报工类型不能为空', trigger: 'change' }],
  workOrderId: [{ required: true, message: '生产工单不能为空', trigger: 'change' }],
  taskId: [{ required: true, message: '生产任务不能为空', trigger: 'change' }],
  workstationId: [{ required: true, message: '工作站不能为空', trigger: 'change' }],
  feedbackQuantity: [{ required: true, message: '报工数量不能为空', trigger: 'blur' }],
  feedbackUserId: [{ required: true, message: '报工人不能为空', trigger: 'change' }],
  feedbackTime: [{ required: true, message: '报工时间不能为空', trigger: 'change' }],
  approveUserId: [{ required: true, message: '当前审批人不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref
const originalFormData = ref<string>('') // 原始表单数据快照，用于脏检查
const checkFlag = ref(true) // 是否需要检验（默认 true，未选任务时只展示报工数量）
// 产品信息展示（只读）
const productInfo = ref({
  itemCode: '',
  itemName: '',
  unitMeasureName: '',
  itemSpecification: ''
})
const edhrRequiredFieldLabels: Record<string, string> = {
  workOrderId: '生产工单',
  taskId: '生产任务',
  routeId: '工艺路线',
  processId: '工序',
  workstationId: '工作站',
  batchCode: '批次号'
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  if (typeof error === 'string' && error.trim()) {
    return error
  }
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage
  }
  return fallback
}

const isUserCancelError = (error: unknown) => error === 'cancel' || error === 'close'

const formatFeedbackQuantity = (quantity: unknown) => {
  const numberValue = Number(quantity)
  if (!Number.isFinite(numberValue)) {
    throw new Error('报工成功提示生成失败：缺少有效完成数量。')
  }
  return Number.isInteger(numberValue) ? String(numberValue) : String(numberValue)
}

const buildFeedbackSuccessMessage = (feedback: Record<string, any>) => {
  const workOrderLabel = [feedback.workOrderCode, feedback.workOrderName]
    .filter((item) => typeof item === 'string' && item.trim())
    .join(' / ')
  const processLabel = [feedback.processCode, feedback.processName]
    .filter((item) => typeof item === 'string' && item.trim())
    .join(' / ')

  if (!workOrderLabel) {
    throw new Error('报工成功提示生成失败：缺少生产工单。')
  }
  if (!processLabel) {
    throw new Error('报工成功提示生成失败：缺少工序。')
  }

  return `报工成功：生产工单 ${workOrderLabel}，工序 ${processLabel}，完成数量 ${formatFeedbackQuantity(feedback.feedbackQuantity)} 个`
}

/** 生成报工单编号 */
const generateCode = async () => {
  formData.value.code = await AutoCodeRecordApi.generateAutoCode(
    MesAutoCodeRuleCode.PRO_FEEDBACK_CODE
  )
}

/** 加载工序的 checkFlag */
const loadCheckFlag = async (routeId?: number, processId?: number) => {
  if (!routeId || !processId) {
    checkFlag.value = true
    return
  }
  try {
    const routeProcess = await ProRouteProcessApi.getRouteProcessByRouteAndProcess(
      routeId,
      processId
    )
    checkFlag.value = routeProcess?.checkFlag ?? false
  } catch (error) {
    message.error(resolveErrorMessage(error, '加载工序检验配置失败，请检查工艺路线配置。'))
    throw error
  }
}

/** 工单变更：清空任务相关字段 */
const handleWorkOrderChange = (workOrder?: ProWorkOrderVO) => {
  formData.value.taskId = undefined
  formData.value.routeId = undefined
  formData.value.processId = undefined
  formData.value.processCode = undefined
  formData.value.processName = undefined
  formData.value.workstationId = undefined
  formData.value.workstationCode = undefined
  formData.value.workstationName = undefined
  formData.value.itemId = undefined
  formData.value.workOrderCode = workOrder?.code || undefined
  formData.value.workOrderName = workOrder?.name || undefined
  formData.value.batchCode = workOrder?.batchCode || undefined
  checkFlag.value = true
  productInfo.value = { itemCode: '', itemName: '', unitMeasureName: '', itemSpecification: '' }
}

/** 任务变更：自动填充关联字段 + 产品信息 */
const handleTaskChange = async (task: any) => {
  if (!task) {
    return
  }
  formData.value.routeId = task.routeId
  formData.value.routeCode = task.routeCode
  formData.value.routeName = task.routeName
  formData.value.processId = task.processId
  formData.value.processCode = task.processCode
  formData.value.processName = task.processName
  formData.value.workstationId = task.workstationId
  formData.value.workstationCode = task.workstationCode
  formData.value.workstationName = task.workstationName
  formData.value.taskCode = task.taskCode
  formData.value.itemId = task.itemId
  // 填充产品信息展示
  productInfo.value = {
    itemCode: task.itemCode || '',
    itemName: task.itemName || '',
    unitMeasureName: task.unitMeasureName || '',
    itemSpecification: task.itemSpecification || ''
  }
  await loadCheckFlag(task.routeId, task.processId)
}

const ensureBatchCodeFromWorkOrder = async () => {
  if (typeof formData.value.batchCode === 'string' && formData.value.batchCode.trim()) {
    return formData.value.batchCode.trim()
  }
  if (!formData.value.workOrderId) {
    return undefined
  }
  const workOrder = await ProWorkOrderApi.getWorkOrder(formData.value.workOrderId)
  const batchCode = typeof workOrder?.batchCode === 'string' ? workOrder.batchCode.trim() : ''
  formData.value.batchCode = batchCode || undefined
  return formData.value.batchCode
}

const ensureWorkOrderContextFromWorkOrder = async () => {
  await ensureBatchCodeFromWorkOrder()
  if (
    typeof formData.value.workOrderCode === 'string' &&
    formData.value.workOrderCode.trim() &&
    typeof formData.value.batchCode === 'string' &&
    formData.value.batchCode.trim()
  ) {
    return {
      workOrderCode: formData.value.workOrderCode.trim(),
      batchCode: formData.value.batchCode.trim()
    }
  }
  if (!formData.value.workOrderId) {
    throw new Error('查看批次执行失败：缺少生产工单，无法定位 eDHR 批次记录。')
  }
  const workOrder = await ProWorkOrderApi.getWorkOrder(formData.value.workOrderId)
  const workOrderCode = typeof workOrder?.code === 'string' ? workOrder.code.trim() : ''
  const batchCode = typeof workOrder?.batchCode === 'string' ? workOrder.batchCode.trim() : ''
  formData.value.workOrderCode = workOrderCode || formData.value.workOrderCode
  formData.value.workOrderName = workOrder?.name || formData.value.workOrderName
  formData.value.batchCode = batchCode || formData.value.batchCode
  if (!workOrderCode) {
    throw new Error('查看批次执行失败：生产工单缺少工单编码，无法定位 eDHR 批次记录。')
  }
  if (!formData.value.batchCode?.trim()) {
    throw new Error('查看批次执行失败：生产工单缺少批次号，无法定位 eDHR 批次记录。')
  }
  return {
    workOrderCode,
    batchCode: formData.value.batchCode.trim()
  }
}

const buildEdhrEntryContext = async (): Promise<ProFeedbackEdhrEntryContextReqVO> => {
  await ensureBatchCodeFromWorkOrder()

  for (const fieldName of Object.keys(edhrRequiredFieldLabels)) {
    const value = formData.value[fieldName]
    const missingString = typeof value === 'string' && !value.trim()
    if (value == null || missingString) {
      throw new Error(`打开 eDHR 失败：缺少${edhrRequiredFieldLabels[fieldName]}，请先补齐报工上下文。`)
    }
  }

  return {
    workOrderId: Number(formData.value.workOrderId),
    taskId: Number(formData.value.taskId),
    routeId: Number(formData.value.routeId),
    processId: Number(formData.value.processId),
    workstationId: Number(formData.value.workstationId),
    batchCode: String(formData.value.batchCode).trim()
  }
}

const buildEdhrBatchExecutionQuery = async () => {
  const workOrderContext = await ensureWorkOrderContextFromWorkOrder()
  return {
    workOrderCode: workOrderContext.workOrderCode,
    batchCode: workOrderContext.batchCode,
    feedbackId: formData.value.id ? String(formData.value.id) : undefined
  }
}

const handleOpenEdhrBatchExecution = async () => {
  try {
    await router.push({
      path: '/mes/pro/feedback/edhr-batch-execution',
      query: await buildEdhrBatchExecutionQuery()
    })
  } catch (error) {
    message.error(resolveErrorMessage(error, '打开 eDHR 批次执行失败，请联系管理员。'))
  }
}

const handleOpenEdhr = async () => {
  edhrOpening.value = true
  try {
    const entryRequest = await buildEdhrEntryContext()
    const resolvedBatchCode = entryRequest.batchCode
    if (!resolvedBatchCode?.trim()) {
      throw new Error('eDHR 入口未返回批次号，无法打开执行页。')
    }
    formData.value.batchCode = resolvedBatchCode
    const batch = await openOrCreateManualEdhrBatchExecution({
      workOrderId: entryRequest.workOrderId,
      routeId: entryRequest.routeId,
      batchCode: resolvedBatchCode
    })
    const batchExecutionId = Number(batch?.id)
    if (!Number.isFinite(batchExecutionId) || batchExecutionId <= 0) {
      throw new Error('eDHR 入口未返回有效批次执行 ID，无法跳转批次详情。')
    }

    await router.push({
      path: '/mes/pro/feedback/edhr-batch-execution/detail',
      query: {
        id: String(batchExecutionId),
        feedbackId: formData.value.id ? String(formData.value.id) : undefined,
        workOrderId: String(entryRequest.workOrderId),
        taskId: String(entryRequest.taskId),
        batchCode: resolvedBatchCode,
        routeId: String(entryRequest.routeId),
        processId: String(entryRequest.processId)
      }
    })
  } catch (error) {
    message.error(resolveErrorMessage(error, '打开 eDHR 失败，请联系管理员。'))
  } finally {
    edhrOpening.value = false
  }
}

/** 合格品/不良品变更：自动计算报工数量 = 合格 + 不良 */
const handleQuantityChanged = () => {
  formData.value.feedbackQuantity =
    (formData.value.qualifiedQuantity || 0) + (formData.value.unqualifiedQuantity || 0)
}

/** 废品明细变更：自动计算不良品数量 = 工废 + 料废 + 其他 */
const handleScrapChanged = () => {
  formData.value.unqualifiedQuantity =
    (formData.value.laborScrapQuantity || 0) +
    (formData.value.materialScrapQuantity || 0) +
    (formData.value.otherScrapQuantity || 0)
  handleQuantityChanged()
}

/** 对齐数量：提交前根据 checkFlag 设置待检/合格/不良数量 */
const alignQuantity = (data: ProFeedbackVO) => {
  if (checkFlag.value) {
    // 质检工序：报工数量即待检数量，合格/不良/废品归零
    data.uncheckQuantity = data.feedbackQuantity
    data.qualifiedQuantity = 0
    data.unqualifiedQuantity = 0
    data.laborScrapQuantity = 0
    data.materialScrapQuantity = 0
    data.otherScrapQuantity = 0
  } else {
    // 非质检工序：报工数量 = 合格 + 不良，待检归零
    data.feedbackQuantity = (data.qualifiedQuantity || 0) + (data.unqualifiedQuantity || 0)
    data.uncheckQuantity = 0
  }
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  formType.value = type
  resetForm()
  // 修改/提交/审批/详情时，加载数据
  if (id) {
    formLoading.value = true
    try {
      const data = await ProFeedbackApi.getFeedback(id)
      formData.value = data as any
      await loadCheckFlag(data.routeId, data.processId)
      // 填充产品信息展示
      productInfo.value = {
        itemCode: data.itemCode || '',
        itemName: data.itemName || '',
        unitMeasureName: data.unitMeasureName || '',
        itemSpecification: data.itemSpecification || ''
      }
    } finally {
      formLoading.value = false
    }
  } else {
    // 创建模式：默认报工人为当前用户，报工时间为当前时间
    formData.value.feedbackUserId = useUserStore().getUser.id
    formData.value.feedbackTime = new Date()
    // 自动生成报工单号
    await generateCode()
  }
  // 保存原始数据快照
  originalFormData.value = JSON.stringify(formData.value)
}

/** 提交表单（保存 create/update） */
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value as unknown as ProFeedbackVO
    alignQuantity(data)
    if (formType.value === 'create') {
      const res = await ProFeedbackApi.createFeedback(data)
      message.success(t('common.createSuccess'))
      // 创建成功后，切换为编辑模式
      formData.value.id = res
      formData.value.status = MesProFeedbackStatusEnum.PREPARE
      formType.value = 'update'
    } else {
      await ProFeedbackApi.updateFeedback(data)
      message.success(t('common.updateSuccess'))
    }
    // 更新快照
    originalFormData.value = JSON.stringify(formData.value)
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 提交操作：表单修改过则先保存，再提交 */
const handleSubmit = async () => {
  if (isImportedPrepareDraft.value) {
    await message.alertWarning('导入来源的草稿正式报工请返回待归属页批量确认提交')
    return
  }
  await formRef.value.validate()
  try {
    await message.confirm('确认提交该报工单？【提交后将不能修改】')
    formLoading.value = true
    const data = formData.value as unknown as ProFeedbackVO
    alignQuantity(data)
    // 1. 表单有修改时，先保存
    if (JSON.stringify(formData.value) !== originalFormData.value) {
      if (formType.value === 'create') {
        const res = await ProFeedbackApi.createFeedback(data)
        formData.value.id = res
      } else {
        await ProFeedbackApi.updateFeedback(data)
      }
    }
    // 2. 提交报工单
    await ProFeedbackApi.submitFeedback(formData.value.id!)
    message.success(buildFeedbackSuccessMessage(formData.value))
    dialogVisible.value = false
    emit('success')
  } catch (error) {
    if (isUserCancelError(error)) {
      return
    }
    message.error(resolveErrorMessage(error, '提交报工单失败，请检查后端接口。'))
    throw error
  } finally {
    formLoading.value = false
  }
}

/** 审批通过 */
const handleApprove = async () => {
  formLoading.value = true
  try {
    const finished = await ProFeedbackApi.approveFeedback(formData.value.id!)
    if (finished) {
      message.success('报工单已审批完成')
    } else {
      message.success('报工成功，请等待质量检验完成！')
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 审批不通过（驳回） */
const handleReject = async () => {
  formLoading.value = true
  try {
    await ProFeedbackApi.rejectFeedback(formData.value.id!)
    message.success('报工单已驳回')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    code: undefined,
    type: undefined,
    workstationId: undefined,
    routeId: undefined,
    processId: undefined,
    workOrderId: undefined,
    batchCode: undefined,
    taskId: undefined,
    itemId: undefined,
    expireDate: undefined,
    feedbackQuantity: 0,
    qualifiedQuantity: 0,
    unqualifiedQuantity: 0,
    uncheckQuantity: 0,
    laborScrapQuantity: 0,
    materialScrapQuantity: 0,
    otherScrapQuantity: 0,
    feedbackUserId: undefined,
    feedbackTime: undefined,
    approveUserId: undefined,
    status: undefined,
    remark: undefined,
    sourceImportRecordId: undefined,
    sourceImportFileName: undefined,
    sourceImportSheetName: undefined,
    sourceImportRowNo: undefined,
    sourceImportAttributionTime: undefined,
    approvalImpactText: undefined
  }
  checkFlag.value = true
  productInfo.value = { itemCode: '', itemName: '', unitMeasureName: '', itemSpecification: '' }
  formRef.value?.resetFields()
}

defineExpose({ open })
</script>

<style scoped>
.feedback-form-approval-context {
  display: grid;
  gap: 4px;
  margin-bottom: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  padding: 10px 12px;
}

.feedback-form-approval-context strong {
  color: #263247;
  font-size: 13px;
}

.feedback-form-approval-context span {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}
</style>
