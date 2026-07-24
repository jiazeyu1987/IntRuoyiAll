<template>
  <el-dialog v-model="visible" title="产品整单指派" width="560px">
    <el-form label-width="100px">
      <el-form-item label="产品">
        <el-input :model-value="productLabel" disabled />
      </el-form-item>
      <el-form-item label="指派给">
        <el-select
          v-model="assigneeUserId"
          clearable
          filterable
          placeholder="请输入昵称或用户名定位用户"
        >
          <el-option
            v-for="user in userOptions"
            :key="user.id"
            :label="`${user.nickname} / ${user.username}`"
            :value="user.id"
          />
        </el-select>
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
import {
  SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE,
  type ProductAssignmentUserOption
} from './contracts'

defineOptions({ name: 'ProductWholeAssignmentDialog' })

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    product: Record<string, unknown> | null
    userOptions?: ProductAssignmentUserOption[]
  }>(),
  {
    product: null,
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
const assigneeUserId = ref<number | null>(null)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const productLabel = computed(() => {
  if (!props.product) {
    return ''
  }
  const productCode = props.product.productCode ? String(props.product.productCode) : '未命名产品'
  const nameCn = props.product.productCode ? String(props.product.nameCn || '') : ''
  return nameCn ? `${productCode} · ${nameCn}` : productCode
})

watch(
  () => props.modelValue,
  (visibleNow) => {
    if (visibleNow) {
      assigneeUserId.value = null
    }
  }
)

const handleSave = async () => {
  if (!props.product?.productId) {
    throw new Error('当前产品缺失，无法创建整单指派')
  }
  if (!userStore.getUser.id) {
    throw new Error('当前登录用户缺失，无法创建整单指派')
  }
  if (!assigneeUserId.value) {
    throw new Error('请选择被指派用户')
  }
  saving.value = true
  try {
    await ShowroomAdminApi.createAssignment({
      targetType: 'PRODUCT',
      targetId: Number(props.product.productId),
      fieldCode: SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE,
      assigneeUserId: assigneeUserId.value,
      assignedBy: userStore.getUser.id
    })
    message.success('产品整单指派已创建')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
