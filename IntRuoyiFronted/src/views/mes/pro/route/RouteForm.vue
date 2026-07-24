<!-- MES 工艺路线弹框外壳：新增/详情继续使用弹框，编辑长内容走 RouteEditPage -->
<template>
  <Dialog
    :title="dialogTitle"
    v-model="dialogVisible"
    :width="routeFormDialogWidth"
    class="route-form-dialog-width"
  >
    <RouteFormContent
      ref="contentRef"
      @success="handleSuccess"
      @request-upgrade="handleRequestUpgrade"
    />
    <template #footer>
      <el-button
        v-if="contentRef?.isEditable && !contentRef?.isProductTabActive"
        type="primary"
        @click="contentRef?.submitForm()"
        :disabled="contentRef?.formLoading"
      >
        保 存
      </el-button>
      <el-button
        v-if="contentRef?.isEnable"
        type="success"
        @click="contentRef?.handleEnable()"
        :disabled="contentRef?.formLoading"
      >
        确认启用
      </el-button>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import RouteFormContent from './RouteFormContent.vue'

defineOptions({ name: 'RouteForm' })

const emit = defineEmits(['success', 'request-upgrade'])

const dialogVisible = ref(false)
const formType = ref<string>('create')
const routeFormDialogWidth = 'calc(100vw - 32px)'
const contentRef = ref<InstanceType<typeof RouteFormContent>>()
const dialogTitle = computed(() => {
  const titles: Record<string, string> = {
    create: '新增工艺路线',
    update: '编辑工艺路线',
    enable: '启用工艺路线',
    detail: '工艺路线详情'
  }
  return titles[formType.value] || formType.value
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  formType.value = type
  await nextTick()
  await contentRef.value?.open(type, id)
}

const handleSuccess = () => {
  if (formType.value !== 'create') {
    dialogVisible.value = false
  }
  emit('success')
}

const handleRequestUpgrade = (payload: { routeName: string }) => {
  dialogVisible.value = false
  emit('request-upgrade', payload)
}

defineExpose({ open })
</script>
