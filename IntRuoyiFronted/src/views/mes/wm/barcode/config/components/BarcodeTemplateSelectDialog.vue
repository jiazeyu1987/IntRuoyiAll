<template>
  <Dialog title="选择打印模板" v-model="dialogVisible" width="80%">
    <ContentWrap class="!mb-0">
      <el-alert
        title="可在下方浏览积木报表列表，再输入或粘贴目标模板的 reportId、/jmreport/index/ 路径或完整 URL。"
        type="info"
        :closable="false"
        show-icon
        class="!mb-12px"
      />
      <el-form label-width="100px">
        <el-form-item label="模板路径">
          <el-input
            v-model="draftValue"
            placeholder="支持 reportId、/jmreport/index/reportId 或完整 URL"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="openReportListInNewTab">
            <Icon icon="ep:link" class="mr-5px" /> 新窗口打开报表设计器
          </el-button>
          <el-button @click="usePreviewPath" :disabled="!previewPath">
            <Icon icon="ep:position" class="mr-5px" /> 使用当前模板路径
          </el-button>
          <el-button @click="draftValue = ''">
            <Icon icon="ep:delete" class="mr-5px" /> 清空模板
          </el-button>
        </el-form-item>
      </el-form>
      <div class="template-selector-hint">
        当前将保存为：{{ normalizedTemplateValue || '未设置默认打印模板' }}
      </div>
    </ContentWrap>

    <ContentWrap :bodyStyle="{ padding: '0px' }" class="!mt-0 !mb-0">
      <IFrame :src="iframeSrc" />
    </ContentWrap>

    <template #footer>
      <el-button type="primary" @click="confirmSelect">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { getRefreshToken } from '@/utils/auth'

defineOptions({ name: 'BarcodeTemplateSelectDialog' })

const message = useMessage()
const emit = defineEmits<{
  selected: [value: string]
}>()

const dialogVisible = ref(false)
const draftValue = ref('')

const defaultDesignerSrc = computed(
  () => import.meta.env.VITE_BASE_URL + '/jmreport/list?token=' + getRefreshToken()
)

const normalizeTemplateValue = (value?: string) => {
  const trimmed = value?.trim() || ''
  if (!trimmed) {
    return ''
  }
  if (/^https?:\/\//i.test(trimmed)) {
    try {
      const url = new URL(trimmed)
      return normalizeTemplateValue(`${url.pathname}${url.search}`)
    } catch {
      return trimmed
    }
  }
  if (trimmed.startsWith('/jmreport/index/')) {
    return trimmed
  }
  if (trimmed.startsWith('jmreport/index/')) {
    return `/${trimmed}`
  }
  if (/^[A-Za-z0-9_-]+$/.test(trimmed)) {
    return `/jmreport/index/${trimmed}`
  }
  return trimmed
}

const normalizedTemplateValue = computed(() => normalizeTemplateValue(draftValue.value))
const previewPath = computed(() =>
  /^\/jmreport\/index\/[^/?#]+/.test(normalizedTemplateValue.value)
    ? normalizedTemplateValue.value
    : ''
)
const iframeSrc = computed(() => {
  if (!previewPath.value) {
    return defaultDesignerSrc.value
  }
  const separator = previewPath.value.includes('?') ? '&' : '?'
  return (
    import.meta.env.VITE_BASE_URL +
    previewPath.value +
    `${separator}token=${encodeURIComponent(getRefreshToken() || '')}`
  )
})

const open = (currentValue?: string) => {
  draftValue.value = currentValue || ''
  dialogVisible.value = true
}

const openReportListInNewTab = () => {
  window.open(defaultDesignerSrc.value, '_blank')
}

const usePreviewPath = () => {
  if (!previewPath.value) {
    return
  }
  draftValue.value = previewPath.value
}

const confirmSelect = () => {
  if (draftValue.value.trim() && !previewPath.value) {
    message.warning('请输入有效的积木报表模板路径、完整 URL 或 reportId')
    return
  }
  emit('selected', previewPath.value)
  dialogVisible.value = false
}

defineExpose({ open })
</script>

<style scoped>
.template-selector-hint {
  color: #4b5563;
  font-size: 0.9rem;
}
</style>
