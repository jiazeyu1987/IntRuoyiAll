<!-- MES 生产工单表单 -->
<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="960px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
      :disabled="isDetail"
    >
      <el-row>
        <el-col :span="12">
          <el-form-item label="工单编码" prop="code">
            <el-input
              v-model="formData.code"
              placeholder="请输入工单编码"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="工单名称" prop="name">
            <el-input
              v-model="formData.name"
              placeholder="请输入工单名称"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="工单来源" prop="orderSourceType">
            <el-select
              v-model="formData.orderSourceType"
              placeholder="请选择工单来源"
              class="!w-1/1"
              :disabled="isHeaderReadonly"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.MES_PRO_WORK_ORDER_SOURCE_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工单类型" prop="type">
            <el-select
              v-model="formData.type"
              placeholder="请选择工单类型"
              class="!w-1/1"
              :disabled="isHeaderReadonly"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.MES_PRO_WORK_ORDER_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="产品" prop="productId">
            <MdItemSelect v-model="formData.productId" :disabled="isHeaderReadonly" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工单数量" prop="quantity">
            <el-input-number
              v-model="formData.quantity"
              :min="1"
              :precision="2"
              class="!w-1/1"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8" v-if="formData.orderSourceType === MesProWorkOrderSourceTypeEnum.ORDER">
          <el-form-item label="客户" prop="clientId">
            <MdClientSelect v-model="formData.clientId" :disabled="isHeaderReadonly" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col
          :span="8"
          v-if="
            formData.type === MesProWorkOrderTypeEnum.OUTSOURCE ||
            formData.type === MesProWorkOrderTypeEnum.PURCHASE
          "
        >
          <el-form-item label="供应商" prop="vendorId">
            <MdVendorSelect v-model="formData.vendorId" :disabled="isHeaderReadonly" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="批次号" prop="batchCode">
            <el-input
              v-model="formData.batchCode"
              placeholder="请输入批次号"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="需求日期" prop="requestDate">
            <el-date-picker
              v-model="formData.requestDate"
              type="date"
              placeholder="请选择需求日期"
              value-format="x"
              class="!w-1/1"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8" v-if="formType !== 'create'">
          <el-form-item label="工单状态" prop="status">
            <dict-tag :type="DICT_TYPE.MES_PRO_WORK_ORDER_STATUS" :value="workOrderStatusValue" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input
              type="textarea"
              v-model="formData.remark"
              placeholder="请输入备注"
              :disabled="isHeaderReadonly"
          />
        </el-form-item>
        </el-col>
      </el-row>
      <template v-if="isDetail">
        <el-row>
          <el-col :span="8">
            <el-form-item label="生产车间">
              <el-input v-model="formData.workshopName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="BOM版本">
              <el-input v-model="formData.bomVersion" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="冲领料">
              <el-input v-model="formData.pickMode" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="业务状态">
              <el-input v-model="formData.businessStatus" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="图号">
              <el-input v-model="formData.drawingNumber" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注1助记码">
              <el-input v-model="formData.auxiliaryCode" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="排产状态">
              <el-input v-model="formData.scheduleStatus" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计划开工时间">
              <el-input v-model="formData.plannedStartTime" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计划完工时间">
              <el-input v-model="formData.plannedEndTime" disabled />
            </el-form-item>
          </el-col>
        </el-row>
      </template>
    </el-form>

    <template v-if="formData.id">
      <el-tabs v-model="activeTab" class="mt-15px">
        <el-tab-pane label="工单BOM" name="bom">
          <WorkOrderBomList
            ref="workOrderBomListRef"
            :work-order-id="formData.id"
            :work-order="formData"
            :form-type="formType"
            @generate-work-order="handleGenerateWorkOrder"
          />
        </el-tab-pane>
        <el-tab-pane label="物料需求" name="item">
          <WorkOrderItemList ref="workOrderItemListRef" :work-order-id="formData.id" />
        </el-tab-pane>
      </el-tabs>
    </template>

    <template #footer>
      <el-button v-if="isEditable" @click="submitForm" type="primary" :disabled="formLoading">
        保存
      </el-button>
      <el-button
        v-if="isEditable && formData.status === MesProWorkOrderStatusEnum.PREPARE"
        @click="handleConfirm"
        type="warning"
        :disabled="formLoading"
      >
        确认
      </el-button>
      <el-button v-if="isConfirm" @click="handleConfirm" type="warning" :disabled="formLoading">
        确认
      </el-button>
      <el-button v-if="isFinish" @click="handleFinish" type="success" :disabled="formLoading">
        完成
      </el-button>
      <el-button
        v-if="formType === 'detail' && canSyncErpBom"
        v-hasPermi="['mes:pro-work-order:update']"
        type="warning"
        plain
        @click="handleSyncErpBom"
        :loading="erpBomSyncLoading"
      >
        ERP同步BOM
      </el-button>
      <el-button v-if="formType === 'detail' && formData.id" type="primary" plain @click="handleBarcode">
        查看条码
      </el-button>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </Dialog>

  <BarcodeDetail ref="barcodeDetailRef" />
</template>

<script setup lang="ts">
import type { FormRules } from 'element-plus'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { ProWorkOrderApi, ProWorkOrderVO } from '@/api/mes/pro/workorder'
import MdItemSelect from '@/views/mes/md/item/components/MdItemSelect.vue'
import MdClientSelect from '@/views/mes/md/client/components/MdClientSelect.vue'
import MdVendorSelect from '@/views/mes/md/vendor/components/MdVendorSelect.vue'
import WorkOrderBomList from './WorkOrderBomList.vue'
import WorkOrderItemList from './WorkOrderItemList.vue'
import {
  MesProWorkOrderSourceTypeEnum,
  MesProWorkOrderTypeEnum,
  MesProWorkOrderStatusEnum,
  BarcodeBizTypeEnum
} from '@/views/mes/utils/constants'
import { BarcodeDetail } from '@/views/mes/wm/barcode/components'

defineOptions({ name: 'WorkOrderForm' })
const emit = defineEmits(['success'])

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const erpBomSyncLoading = ref(false)
const formType = ref<string>('create')
const isEditable = computed(() => ['create', 'update'].includes(formType.value))
const isConfirm = computed(() => formType.value === 'confirm')
const isFinish = computed(() => formType.value === 'finish')
const isDetail = computed(() => ['detail', 'confirm', 'finish'].includes(formType.value))
const isHeaderReadonly = computed(() => ['confirm', 'finish', 'detail'].includes(formType.value))
const workOrderStatusValue = computed(() => formData.value.status as number)
const canSyncErpBom = computed(
  () =>
    !!formData.value.id &&
    [MesProWorkOrderStatusEnum.PREPARE, MesProWorkOrderStatusEnum.CONFIRMED].includes(
      formData.value.status as number
    )
)
const dialogTitle = computed(() => {
  if (['create', 'update'].includes(formType.value) && formData.value.parentId) {
    return formType.value === 'create' ? '新增子工单' : '编辑子工单'
  }
  const titles: Record<string, string> = {
    create: '新增工单',
    update: '编辑工单',
    confirm: '确认工单',
    finish: '完成工单',
    detail: '工单详情'
  }
  return titles[formType.value] || formType.value
})
const activeTab = ref('bom')
const workOrderBomListRef = ref<any>()
const workOrderItemListRef = ref<any>()

type WorkOrderFormData = Partial<ProWorkOrderVO>

const formData = ref<WorkOrderFormData>({
  id: undefined as number | undefined,
  parentId: undefined as number | undefined,
  code: undefined,
  name: undefined,
  type: undefined,
  orderSourceType: undefined,
  orderSourceCode: undefined,
  productId: undefined,
  quantity: undefined,
  clientId: undefined,
  vendorId: undefined,
  batchCode: undefined,
  workshopName: undefined,
  bomVersion: undefined,
  pickMode: undefined,
  auxiliaryCode: undefined,
  businessStatus: undefined,
  drawingNumber: undefined,
  scheduleStatus: undefined,
  plannedStartTime: undefined,
  plannedEndTime: undefined,
  requestDate: undefined,
  status: undefined as number | undefined,
  remark: undefined
})

const formRules = reactive<FormRules<WorkOrderFormData>>({
  code: [{ required: true, message: '工单编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '工单名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '工单类型不能为空', trigger: 'change' }],
  orderSourceType: [{ required: true, message: '工单来源不能为空', trigger: 'change' }],
  productId: [{ required: true, message: '产品不能为空', trigger: 'change' }],
  quantity: [{ required: true, message: '工单数量不能为空', trigger: 'blur' }],
  requestDate: [{ required: true, message: '需求日期不能为空', trigger: 'change' }]
})

const formRef = ref()
const barcodeDetailRef = ref()
const originalFormData = ref<string>('')

const handleBarcode = () => {
  barcodeDetailRef.value?.openByBusiness(
    formData.value.id!,
    BarcodeBizTypeEnum.WORKORDER,
    formData.value.code,
    formData.value.name
  )
}

const handleSyncErpBom = async () => {
  if (!formData.value.id) {
    return
  }
  erpBomSyncLoading.value = true
  try {
    const result = await ProWorkOrderApi.syncErpBom(formData.value.id)
    message.success(`ERP BOM同步成功：版本 ${result.erpBomVersion}，共 ${result.syncedBomCount} 条`)
    await Promise.all([workOrderBomListRef.value?.reload?.(), workOrderItemListRef.value?.reload?.()])
    emit('success')
  } finally {
    erpBomSyncLoading.value = false
  }
}

const open = async (type: string, id?: number, parentRow?: any) => {
  dialogVisible.value = true
  formType.value = type
  activeTab.value = 'bom'
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await ProWorkOrderApi.getWorkOrder(id)
    } finally {
      formLoading.value = false
    }
  }
  if (parentRow) {
    formData.value.parentId = parentRow.id
    formData.value.type = parentRow.type
    formData.value.orderSourceType = parentRow.orderSourceType
    formData.value.clientId = parentRow.clientId
    formData.value.vendorId = parentRow.vendorId
    formData.value.requestDate = parentRow.requestDate
  }
  originalFormData.value = JSON.stringify(formData.value)
}

const handleGenerateWorkOrder = (bomRow: any) => {
  const currentWorkOrder = { ...formData.value }
  resetForm()
  formType.value = 'create'
  activeTab.value = 'bom'
  formData.value.parentId = currentWorkOrder.id
  formData.value.type = currentWorkOrder.type
  formData.value.orderSourceType = currentWorkOrder.orderSourceType
  formData.value.clientId = currentWorkOrder.clientId
  formData.value.vendorId = currentWorkOrder.vendorId
  formData.value.requestDate = currentWorkOrder.requestDate
  formData.value.productId = bomRow.itemId
  formData.value.quantity = bomRow.quantity
  formData.value.name = `${bomRow.itemName}·${bomRow.quantity}·${bomRow.unitMeasureName || ''}`
  message.info('已从 BOM 物料预填子工单，请补充工单编码等信息后保存')
}

watch(
  () => formData.value.orderSourceType,
  (val) => {
    if (val !== MesProWorkOrderSourceTypeEnum.ORDER) {
      formData.value.orderSourceCode = undefined
      formData.value.clientId = undefined
    }
  }
)

watch(
  () => formData.value.type,
  (val) => {
    if (val !== MesProWorkOrderTypeEnum.OUTSOURCE && val !== MesProWorkOrderTypeEnum.PURCHASE) {
      formData.value.vendorId = undefined
    }
  }
)

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value as unknown as ProWorkOrderVO
    if (formType.value === 'create') {
      const res = await ProWorkOrderApi.createWorkOrder(data)
      message.success('新增成功')
      formData.value.id = res
      formData.value.status = MesProWorkOrderStatusEnum.PREPARE
      formType.value = 'update'
    } else {
      await ProWorkOrderApi.updateWorkOrder(data)
      message.success('修改成功')
    }
    originalFormData.value = JSON.stringify(formData.value)
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const handleConfirm = async () => {
  if (isEditable.value) {
    await formRef.value.validate()
  }
  try {
    await message.confirm('确认要完成工单编制吗？确认后将不能更改。')
    formLoading.value = true
    if (isEditable.value && JSON.stringify(formData.value) !== originalFormData.value) {
      const data = formData.value as unknown as ProWorkOrderVO
      await ProWorkOrderApi.updateWorkOrder(data)
    }
    await ProWorkOrderApi.confirmWorkOrder(formData.value.id!)
    message.success('工单已确认')
    dialogVisible.value = false
    emit('success')
  } catch {
  } finally {
    formLoading.value = false
  }
}

const handleFinish = async () => {
  try {
    await message.confirm('确认要完成该工单吗？')
    formLoading.value = true
    await ProWorkOrderApi.finishWorkOrder(formData.value.id!)
    message.success('工单已完成')
    dialogVisible.value = false
    emit('success')
  } catch {
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    parentId: undefined,
    code: undefined,
    name: undefined,
    type: undefined,
    orderSourceType: undefined,
    orderSourceCode: undefined,
    productId: undefined,
    quantity: undefined,
    clientId: undefined,
    vendorId: undefined,
    batchCode: undefined,
    workshopName: undefined,
    bomVersion: undefined,
    pickMode: undefined,
    auxiliaryCode: undefined,
    businessStatus: undefined,
    drawingNumber: undefined,
    scheduleStatus: undefined,
    plannedStartTime: undefined,
    plannedEndTime: undefined,
    requestDate: undefined,
    status: undefined,
    remark: undefined
  }
  formRef.value?.resetFields()
}

defineExpose({ open })
</script>
