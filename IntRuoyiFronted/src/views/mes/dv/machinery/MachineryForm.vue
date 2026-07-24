<!-- MES 设备台账表单 -->
<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="1080px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
      :disabled="isDetail"
    >
      <el-row>
        <el-col :span="8">
          <el-form-item label="设备编码" prop="code">
            <el-input v-model="formData.code" placeholder="请输入设备编码">
              <template #append>
                <el-button @click="generateCode" :disabled="formType !== 'create'">生成</el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="设备名称" prop="name">
            <el-input v-model="formData.name" placeholder="请输入设备名称" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="品牌" prop="brand">
            <el-input v-model="formData.brand" placeholder="请输入品牌" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row>
        <el-col :span="8">
          <el-form-item label="设备类型" prop="machineryTypeId">
            <DvMachineryTypeSelect v-model="formData.machineryTypeId" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所属车间" prop="workshopId">
            <MdWorkshopSelect v-model="formData.workshopId" placeholder="请选择所属车间" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="设备状态" prop="status">
            <el-select v-model="formData.status" placeholder="请选择设备状态" class="!w-1/1">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.MES_DV_MACHINERY_STATUS)"
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
          <el-form-item label="规格型号" prop="specification">
            <el-input v-model="formData.specification" placeholder="请输入规格型号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工序名称" prop="processName">
            <el-input v-model="formData.processName" placeholder="请输入工序名称" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="设备标准小时产能" prop="standardHourlyCapacity">
            <el-input-number
              v-model="formData.standardHourlyCapacity"
              :min="0.000001"
              :precision="6"
              controls-position="right"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row v-if="isDetail">
        <el-col :span="8">
          <el-form-item label="最近点检时间" prop="lastCheckTime">
            <el-date-picker
              v-model="formData.lastCheckTime"
              type="datetime"
              value-format="x"
              class="!w-1/1"
              disabled
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="最近保养时间" prop="lastMaintenTime">
            <el-date-picker
              v-model="formData.lastMaintenTime"
              type="datetime"
              value-format="x"
              class="!w-1/1"
              disabled
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-tabs v-if="formType !== 'create' && formData.id" v-model="activeTab" class="mt-10px">
      <el-tab-pane label="工序明细" name="process" lazy>
        <MachineryProcessList :machinery-id="formData.id" />
      </el-tab-pane>
      <el-tab-pane label="点检记录" name="check" lazy>
        <MachineryCheckRecordList :machinery-id="formData.id" />
      </el-tab-pane>
      <el-tab-pane label="保养记录" name="mainten" lazy>
        <MachineryMaintenRecordList :machinery-id="formData.id" />
      </el-tab-pane>
      <el-tab-pane label="维修记录" name="repair" lazy>
        <MachineryRepairList :machinery-id="formData.id" />
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button v-if="isDetail && formData.id" type="primary" plain @click="handleBarcode">
        查看条码
      </el-button>
      <el-button v-if="!isDetail" type="primary" :disabled="formLoading" @click="submitForm">
        确定
      </el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>

  <BarcodeDetail ref="barcodeDetailRef" />
</template>

<script setup lang="ts">
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { DvMachineryApi, DvMachineryVO } from '@/api/mes/dv/machinery'
import MdWorkshopSelect from '@/views/mes/md/workstation/components/MdWorkshopSelect.vue'
import DvMachineryTypeSelect from '@/views/mes/dv/machinery/type/components/DvMachineryTypeSelect.vue'
import MachineryProcessList from './MachineryProcessList.vue'
import MachineryCheckRecordList from './MachineryCheckRecordList.vue'
import MachineryMaintenRecordList from './MachineryMaintenRecordList.vue'
import MachineryRepairList from './MachineryRepairList.vue'
import {
  MesDvMachineryStatusEnum,
  MesAutoCodeRuleCode,
  BarcodeBizTypeEnum
} from '@/views/mes/utils/constants'
import { AutoCodeRecordApi } from '@/api/mes/md/autocode/record'
import { BarcodeDetail } from '@/views/mes/wm/barcode/components'

defineOptions({ name: 'MachineryForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const formType = ref('')
const isDetail = computed(() => formType.value === 'detail')
const dialogTitle = computed(() => {
  const titles: Record<string, string> = {
    create: '新增设备',
    update: '修改设备',
    detail: '查看设备'
  }
  return titles[formType.value] || formType.value
})
const activeTab = ref('process')
const formData = ref({
  id: undefined,
  code: undefined,
  name: undefined,
  brand: undefined,
  specification: undefined,
  machineryTypeId: undefined,
  workshopId: undefined,
  processName: undefined,
  standardHourlyCapacity: undefined,
  status: MesDvMachineryStatusEnum.STOP,
  lastCheckTime: undefined,
  lastMaintenTime: undefined,
  remark: undefined
})
const formRules = reactive({
  code: [{ required: true, message: '设备编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '设备名称不能为空', trigger: 'blur' }],
  machineryTypeId: [{ required: true, message: '设备类型不能为空', trigger: 'change' }],
  workshopId: [{ required: true, message: '所属车间不能为空', trigger: 'change' }],
  status: [{ required: true, message: '设备状态不能为空', trigger: 'change' }]
})
const formRef = ref()
const barcodeDetailRef = ref()

const handleBarcode = () => {
  barcodeDetailRef.value?.openByBusiness(
    formData.value.id!,
    BarcodeBizTypeEnum.MACHINERY,
    formData.value.code,
    formData.value.name
  )
}

const generateCode = async () => {
  formData.value.code = await AutoCodeRecordApi.generateAutoCode(MesAutoCodeRuleCode.DV_MACHINERY_CODE)
}

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await DvMachineryApi.getMachinery(id)
    } finally {
      formLoading.value = false
    }
    activeTab.value = 'process'
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = formData.value as unknown as DvMachineryVO
    if (formType.value === 'create') {
      await DvMachineryApi.createMachinery(data)
      message.success(t('common.createSuccess'))
    } else {
      await DvMachineryApi.updateMachinery(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    code: undefined,
    name: undefined,
    brand: undefined,
    specification: undefined,
    machineryTypeId: undefined,
    workshopId: undefined,
    processName: undefined,
    standardHourlyCapacity: undefined,
    status: MesDvMachineryStatusEnum.STOP,
    lastCheckTime: undefined,
    lastMaintenTime: undefined,
    remark: undefined
  }
  formRef.value?.resetFields()
  activeTab.value = 'process'
}
</script>
