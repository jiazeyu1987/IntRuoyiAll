<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="上级目录" prop="parentId">
        <el-tree-select
          v-model="formData.parentId"
          :data="directoryOptions"
          :props="defaultProps"
          check-strictly
          default-expand-all
          clearable
          node-key="id"
          placeholder="请选择上级目录"
        />
      </el-form-item>
      <el-form-item label="目录编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入目录编码" />
      </el-form-item>
      <el-form-item label="目录名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入目录名称" />
      </el-form-item>
      <el-form-item label="启用状态" prop="active">
        <el-radio-group v-model="formData.active">
          <el-radio
            v-for="item in ACTIVE_STATUS_OPTIONS"
            :key="String(item.value)"
            :value="item.value"
          >
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确定</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { FormRules } from 'element-plus'
import { defaultProps } from '@/utils/tree'
import {
  createDirectory,
  getDirectory,
  getDirectoryTree,
  type ControlledFileDirectoryVO,
  updateDirectory
} from '@/api/dcc/controlledFile/directories'
import { ACTIVE_STATUS_OPTIONS } from '../../shared/options'

defineOptions({ name: 'DccControlledFileDirectoryForm' })

const { t } = useI18n()
const message = useMessage()

interface DirectoryTreeOption extends ControlledFileDirectoryVO {
  children?: DirectoryTreeOption[]
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const directoryOptions = ref<DirectoryTreeOption[]>([])
const formData = ref<ControlledFileDirectoryVO>({
  parentId: undefined,
  code: '',
  name: '',
  active: true,
  sort: 0,
  remark: ''
})

const formRules = reactive<FormRules>({
  code: [{ required: true, message: '目录编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '目录名称不能为空', trigger: 'blur' }],
  active: [{ required: true, message: '启用状态不能为空', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'change' }]
})

const emit = defineEmits<{
  success: []
}>()

const open = async (
  type: 'create' | 'update',
  payload?: { id?: number; parentId?: number | null; directories?: DirectoryTreeOption[] }
) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新建目录' : '编辑目录'
  formType.value = type
  directoryOptions.value = payload?.directories ?? []
  resetForm()
  formLoading.value = true
  try {
    directoryOptions.value = await getDirectoryTree()
    if (type === 'create') {
      formData.value.parentId = payload?.parentId ?? undefined
      return
    }
    if (!payload?.id) {
      return
    }
    formData.value = await getDirectory(payload.id)
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })

const resetForm = () => {
  formData.value = {
    parentId: undefined,
    code: '',
    name: '',
    active: true,
    sort: 0,
    remark: ''
  }
  formRef.value?.resetFields()
}

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await createDirectory(formData.value)
      message.success(t('common.createSuccess'))
    } else if (formData.value.id) {
      await updateDirectory(formData.value.id, formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
