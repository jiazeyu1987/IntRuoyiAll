<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="1100px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="formLoading"
      :disabled="isDetail"
    >
      <el-row>
        <el-col :span="8">
          <el-form-item label="转移单编号" prop="code">
            <el-input
              v-model="formData.code"
              placeholder="请输入转移单编号"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="转移单名称" prop="name">
            <el-input
              v-model="formData.name"
              placeholder="请输入转移单名称"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="转移单类型" prop="type">
            <el-select
              v-model="formData.type"
              placeholder="请选择转移单类型"
              class="!w-full"
              :disabled="isHeaderReadonly"
            >
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.MES_WM_TRANSFER_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="转移日期" prop="transferDate">
            <el-date-picker
              v-model="formData.transferDate"
              type="date"
              value-format="YYYY-MM-DD 00:00:00"
              placeholder="请选择转移日期"
              class="!w-full"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col v-if="isOuterType" :span="8">
          <el-form-item label="是否配送" prop="deliveryFlag">
            <el-switch v-model="formData.deliveryFlag" :disabled="isHeaderReadonly" />
          </el-form-item>
        </el-col>
        <el-col v-if="isOuterType" :span="8">
          <el-form-item label="是否确认" prop="confirmFlag">
            <el-switch :model-value="formData.confirmFlag" disabled />
          </el-form-item>
        </el-col>
        <el-col v-if="showDeliveryFields" :span="8">
          <el-form-item label="收货人" prop="recipientName">
            <el-input
              v-model="formData.recipientName"
              placeholder="请输入收货人"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col v-if="showDeliveryFields" :span="8">
          <el-form-item label="联系电话" prop="recipientTelephone">
            <el-input
              v-model="formData.recipientTelephone"
              placeholder="请输入联系电话"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col v-if="showDeliveryFields" :span="8">
          <el-form-item label="承运商" prop="carrier">
            <el-input
              v-model="formData.carrier"
              placeholder="请输入承运商"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col v-if="showDeliveryFields" :span="8">
          <el-form-item label="运输单号" prop="shippingNumber">
            <el-input
              v-model="formData.shippingNumber"
              placeholder="请输入运输单号"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col v-if="showDeliveryFields" :span="16">
          <el-form-item label="目的地" prop="destinationAddress">
            <el-input
              v-model="formData.destinationAddress"
              placeholder="请输入目的地"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              placeholder="请输入备注"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <!-- 非新建模式展示行项目信息（调拨物料） -->
    <template v-if="formData.id">
      <el-divider content-position="center">物料信息</el-divider>
      <TransferLineList :transfer-id="formData.id" :form-type="formType" />
    </template>

    <template #footer>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { WmTransferApi } from '@/api/mes/wm/transfer'
import TransferLineList from './TransferLineList.vue'

defineOptions({ name: 'TransferForm' })
const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中
const formType = ref<string>('detail') // 调拨单由 ERP/正式库存链路生成，页面仅保留只读详情
const isDetail = computed(() => true) // 是否为详情模式
const isHeaderReadonly = computed(() => true) // 表头是否只读
const isOuterType = computed(() => !!formData.value.type && Number(formData.value.type) === 2)
const showDeliveryFields = computed(() => isOuterType.value && !!formData.value.deliveryFlag)
const dialogTitle = computed(() => '转移单详情')
const formData = ref({
  id: undefined as number | undefined,
  code: undefined,
  name: undefined,
  status: undefined as number | undefined,
  type: undefined as number | undefined,
  deliveryFlag: false,
  recipientName: undefined,
  recipientTelephone: undefined,
  destinationAddress: undefined,
  carrier: undefined,
  shippingNumber: undefined,
  confirmFlag: false,
  transferDate: undefined,
  remark: undefined
})
const formRules = reactive({
  code: [{ required: true, message: '转移单编号不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '转移单名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '转移单类型不能为空', trigger: 'change' }],
  transferDate: [{ required: true, message: '转移日期不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref

/** 打开弹窗 */
const open = async (_type: string, id?: number) => {
  dialogVisible.value = true
  formType.value = 'detail'
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await WmTransferApi.getTransfer(id)
    } finally {
      formLoading.value = false
    }
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    code: undefined,
    name: undefined,
    status: undefined,
    type: undefined,
    deliveryFlag: false,
    recipientName: undefined,
    recipientTelephone: undefined,
    destinationAddress: undefined,
    carrier: undefined,
    shippingNumber: undefined,
    confirmFlag: false,
    transferDate: undefined,
    remark: undefined
  }
  formRef.value?.resetFields()
}

defineExpose({ open })
</script>
