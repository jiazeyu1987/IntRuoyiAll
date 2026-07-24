<template>
  <el-dialog v-model="visible" title="补充指派" width="720px">
    <el-form label-width="110px">
      <el-form-item label="目标类型">
        <el-select v-model="form.targetType">
          <el-option label="公司" value="COMPANY" />
          <el-option label="产品" value="PRODUCT" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标对象">
        <el-select v-model="form.targetId" filterable placeholder="请选择目标对象">
          <el-option
            v-for="option in targetOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="字段">
        <el-select v-model="form.fieldCode" filterable placeholder="请选择字段">
          <el-option
            v-for="option in fieldOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="指派给">
        <el-select v-model="form.assigneeUserId" filterable placeholder="请选择编辑人">
          <el-option
            v-for="user in userOptions"
            :key="user.id"
            :label="`${user.nickname} #${user.id}`"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="说明">
        <el-input
          v-model="form.note"
          :rows="4"
          placeholder="补充指派会同步创建站内信，并在完成后自动进入提交流程"
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :loading="saving" type="primary" @click="handleSave">创建指派</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ShowroomAdminApi } from '@/api/showroom-admin'
import { useUserStore } from '@/store/modules/user'
import { buildFieldOptions, buildTargetOptions } from './contracts'

defineOptions({ name: 'FieldAssignmentDialog' })

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    companyCurrent?: Record<string, unknown> | null
    products?: unknown[]
    userOptions?: Array<{ id: number; nickname: string; deptId: number }>
  }>(),
  {
    companyCurrent: null,
    products: () => [],
    userOptions: () => []
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'saved'): void
}>()

const message = useMessage()
const userStore = useUserStore()
const saving = ref(false)

const form = reactive({
  targetType: 'PRODUCT' as 'COMPANY' | 'PRODUCT',
  targetId: null as number | null,
  fieldCode: '',
  assigneeUserId: null as number | null,
  note: ''
})

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const targetOptions = computed(() => {
  const normalized = buildTargetOptions(props.companyCurrent, props.products)
  return normalized[form.targetType]
})

const fieldOptions = computed(() => buildFieldOptions(form.targetType))

const resetForm = () => {
  form.targetType = 'PRODUCT'
  form.targetId = targetOptions.value[0]?.value ?? null
  form.fieldCode = fieldOptions.value[0]?.value ?? ''
  form.assigneeUserId = null
  form.note = ''
}

watch(
  () => props.modelValue,
  (nextValue) => {
    if (nextValue) {
      resetForm()
    }
  }
)

watch(
  () => form.targetType,
  () => {
    form.targetId = targetOptions.value[0]?.value ?? null
    form.fieldCode = fieldOptions.value[0]?.value ?? ''
  }
)

watch(
  targetOptions,
  (nextValue) => {
    if (!nextValue.find((item) => item.value === form.targetId)) {
      form.targetId = nextValue[0]?.value ?? null
    }
  },
  { immediate: true }
)

watch(
  fieldOptions,
  (nextValue) => {
    if (!nextValue.find((item) => item.value === form.fieldCode)) {
      form.fieldCode = nextValue[0]?.value ?? ''
    }
  },
  { immediate: true }
)

const handleSave = async () => {
  if (!userStore.getUser.id) {
    throw new Error('当前登录用户缺失，无法发起补充指派')
  }
  if (!form.targetId || !form.fieldCode || !form.assigneeUserId) {
    throw new Error('目标对象、字段、指派人均为必填项')
  }
  saving.value = true
  try {
    await ShowroomAdminApi.createAssignment({
      targetType: form.targetType,
      targetId: form.targetId,
      fieldCode: form.fieldCode,
      assigneeUserId: form.assigneeUserId,
      assignedBy: userStore.getUser.id
    })
    message.success('补充指派已创建')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
