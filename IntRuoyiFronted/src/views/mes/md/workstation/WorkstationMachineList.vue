<template>
  <div>
    <el-button
      v-if="!isDetail"
      type="primary"
      plain
      size="small"
      class="mb-10px"
      @click="openForm('create')"
    >
      <Icon icon="ep:plus" class="mr-5px" /> 添加设备
    </el-button>

    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" border>
      <el-table-column label="设备编号" align="center" prop="machineryCode">
        <template #default="scope">
          <el-link type="primary" @click="openMachineryDetail(scope.row.machineryId)">
            {{ scope.row.machineryCode }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="设备名称" align="center" prop="machineryName" />
      <el-table-column label="数量" align="center" prop="quantity" width="100" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column v-if="!isDetail" label="操作" align="center" width="80">
        <template #default="scope">
          <el-button link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
        v-loading="formLoading"
      >
        <el-form-item label="设备" prop="machineryId">
          <DvMachinerySelect
            v-model="formData.machineryId"
            placeholder="请选择设备"
            class="!w-1/1"
          />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number
            v-model="formData.quantity"
            :min="1"
            controls-position="right"
            class="!w-1/1"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :disabled="formLoading" @click="submitForm">确定</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
      </template>
    </Dialog>

    <MachineryForm ref="machineryFormRef" @success="getList" />
  </div>
</template>

<script setup lang="ts">
import { MdWorkstationMachineApi, MdWorkstationMachineVO } from '@/api/mes/md/workstation/machine'
import DvMachinerySelect from '@/views/mes/dv/machinery/components/DvMachinerySelect.vue'
import MachineryForm from '@/views/mes/dv/machinery/MachineryForm.vue'

defineOptions({ name: 'WorkstationMachineList' })

const props = defineProps<{
  workstationId: number
  formType: string
}>()

const { t } = useI18n()
const message = useMessage()
const isDetail = computed(() => props.formType === 'detail')

const loading = ref(false)
const list = ref<MdWorkstationMachineVO[]>([])

const getList = async () => {
  loading.value = true
  try {
    list.value = await MdWorkstationMachineApi.getWorkstationMachineList(props.workstationId)
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogFormType = ref('')
const formLoading = ref(false)
const formRef = ref()
const machineryFormRef = ref()
const formData = ref({
  id: undefined as number | undefined,
  workstationId: undefined as number | undefined,
  machineryId: undefined as number | undefined,
  quantity: 1,
  remark: undefined as string | undefined
})
const formRules = reactive({
  machineryId: [{ required: true, message: '设备不能为空', trigger: 'blur' }],
  quantity: [{ required: true, message: '数量不能为空', trigger: 'blur' }]
})

const openForm = (type: string) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  dialogFormType.value = type
  resetForm()
}

const openMachineryDetail = (machineryId: number) => {
  machineryFormRef.value?.open('detail', machineryId)
}

const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = formData.value as unknown as MdWorkstationMachineVO
    await MdWorkstationMachineApi.createWorkstationMachine(data)
    message.success(t('common.createSuccess'))
    dialogVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    workstationId: props.workstationId,
    machineryId: undefined,
    quantity: 1,
    remark: undefined
  }
  formRef.value?.resetFields()
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await MdWorkstationMachineApi.deleteWorkstationMachine(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

watch(
  () => props.workstationId,
  (val) => {
    if (val) {
      getList()
    }
  },
  { immediate: true }
)
</script>
