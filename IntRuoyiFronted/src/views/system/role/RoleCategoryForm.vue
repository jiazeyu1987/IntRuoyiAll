<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="90px"
    >
      <el-form-item label="分类名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入分类名称" />
      </el-form-item>
      <el-form-item label="分类标识" prop="code">
        <el-input v-model="formData.code" placeholder="请输入分类标识，如 showroom" />
      </el-form-item>
      <el-form-item label="显示顺序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="formData.status" placeholder="请选择状态">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import * as RoleApi from '@/api/system/role'

defineOptions({ name: 'SystemRoleCategoryForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formData = ref({
  id: undefined,
  name: '',
  code: '',
  sort: 0,
  status: CommonStatusEnum.ENABLE,
  remark: ''
})
const formRules = reactive({
  name: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '分类标识不能为空', trigger: 'blur' }],
  sort: [{ required: true, message: '显示顺序不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})
const formRef = ref()

const open = (type: 'create' | 'update', row?: RoleApi.RoleCategoryVO) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增角色分类' : '编辑角色分类'
  formType.value = type
  resetForm(row)
}

const resetForm = (row?: RoleApi.RoleCategoryVO) => {
  formData.value = row
    ? ({ ...row } as any)
    : {
        id: undefined,
        name: '',
        code: '',
        sort: 0,
        status: CommonStatusEnum.ENABLE,
        remark: ''
      }
  formRef.value?.resetFields()
}

defineExpose({ open })

const emit = defineEmits(['success'])

const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = formData.value as unknown as RoleApi.RoleCategoryVO
    if (formType.value === 'create') {
      await RoleApi.createRoleCategory(data)
      message.success(t('common.createSuccess'))
    } else {
      await RoleApi.updateRoleCategory(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
