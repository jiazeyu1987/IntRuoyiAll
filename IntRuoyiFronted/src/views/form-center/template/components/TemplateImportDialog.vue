<template>
  <Dialog
    v-model="dialogVisible"
    class="form-template-import-dialog scheme-d-form-control"
    title="导入表单模板"
    width="640px"
  >
    <el-form
      ref="formRef"
      class="form-template-import-dialog__form"
      :model="formData"
      :rules="formRules"
      label-position="top"
    >
      <el-form-item label="模板名称" prop="templateName">
        <el-autocomplete
          v-model="formData.templateName"
          :fetch-suggestions="queryTemplateSuggestions"
          :maxlength="80"
          :loading="templateOptionsLoading"
          clearable
          placeholder="输入新模板名称，或选择已有模板升版"
          value-key="value"
          @input="handleTemplateNameInput"
          @select="handleTemplateOptionSelect"
        >
          <template #default="{ item }">
            <div class="flex items-center justify-between gap-12px">
              <span>{{ item.templateName }}</span>
              <span class="text-12px text-gray-400">{{ item.versionNo }}</span>
            </div>
          </template>
        </el-autocomplete>
        <div class="form-template-import-dialog__hint">
          版本号由系统自动生成；导入时会自动进行代码规则识别。选择已有模板将生成升版版本，并按审批流自动发布或等待审批后发布。
        </div>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="formData.remark"
          maxlength="200"
          placeholder="请输入备注"
          :rows="3"
          type="textarea"
        />
      </el-form-item>
      <el-form-item label="模板文件" prop="file">
        <el-upload
          ref="uploadRef"
          class="form-template-import-dialog__upload"
          data-testid="form-template-import-upload"
          v-model:file-list="fileList"
          :auto-upload="false"
          :limit="1"
          :on-exceed="handleExceed"
          accept=".doc,.docx"
          drag
        >
          <Icon icon="ep:upload" />
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button
          class="scheme-d-btn scheme-d-btn--neutral"
          :disabled="loading"
          @click="dialogVisible = false"
        >
          取消
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--primary"
          :loading="loading"
          type="primary"
          @click="submitForm"
        >
          导入
        </el-button>
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import * as TemplateApi from '@/api/form-center/template'
import type { FormTemplateListItemVO } from '@/api/form-center/template'

defineOptions({ name: 'FormCenterTemplateImportDialog' })

const emit = defineEmits<{
  success: []
}>()

const message = useMessage()
const dialogVisible = ref(false)
const loading = ref(false)
const formRef = ref()
const uploadRef = ref()
const fileList = ref<any[]>([])
const templateOptions = ref<FormTemplateListItemVO[]>([])
const templateOptionsLoading = ref(false)
const formData = reactive({
  templateName: '',
  selectedTemplateId: undefined as number | undefined,
  remark: ''
})
const formRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

const open = async (template?: FormTemplateListItemVO) => {
  dialogVisible.value = true
  formData.templateName = template?.templateName || ''
  formData.selectedTemplateId = template?.templateId
  formData.remark = template?.remark || ''
  fileList.value = []
  await loadTemplateOptions()
  nextTick(() => uploadRef.value?.clearFiles())
}

defineExpose({ open })

const handleExceed = () => {
  message.error('最多只能上传一个 doc/docx 文件')
}

const loadTemplateOptions = async () => {
  templateOptionsLoading.value = true
  try {
    const data = await TemplateApi.getTemplatePool({ pageNo: 1, pageSize: 100 })
    const uniqueOptions = new Map<number, FormTemplateListItemVO>()
    for (const item of data.list || []) {
      if (!uniqueOptions.has(item.templateId)) {
        uniqueOptions.set(item.templateId, item)
      }
    }
    templateOptions.value = Array.from(uniqueOptions.values())
  } catch (error) {
    message.error(resolveImportErrorMessage(error, '模板池加载失败，无法选择已有模板升版'))
    throw error
  } finally {
    templateOptionsLoading.value = false
  }
}

const queryTemplateSuggestions = (
  queryString: string,
  callback: (items: Array<FormTemplateListItemVO & { value: string }>) => void
) => {
  const keyword = queryString.trim().toLowerCase()
  const matches = templateOptions.value
    .filter((item) => !keyword || item.templateName.toLowerCase().includes(keyword))
    .map((item) => ({ ...item, value: item.templateName }))
  callback(matches)
}

const handleTemplateOptionSelect = (item: FormTemplateListItemVO & { value: string }) => {
  formData.templateName = item.templateName
  formData.selectedTemplateId = item.templateId
}

const handleTemplateNameInput = () => {
  const selected = templateOptions.value.find(
    (item) => item.templateId === formData.selectedTemplateId
  )
  if (!selected || selected.templateName !== formData.templateName) {
    formData.selectedTemplateId = undefined
  }
}

const resolveImportErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as { response?: { data?: { msg?: string } } })?.response?.data?.msg
  const directMessage = (error as { message?: string })?.message
  return responseMessage || directMessage || fallback
}

const resolveImportSuccessMessage = (result: TemplateApi.FormTemplateImportRespVO) => {
  if (result.status === 'PUBLISHED') {
    return `已导入并自动发布 ${result.versionNo}，请使用发布版本测试`
  }
  if (result.status === 'PENDING_APPROVAL') {
    return `已导入并提交升版审批，审批通过后自动发布 ${result.versionNo}`
  }
  if (result.status === 'DRAFT') {
    return `已导入并生成草稿 ${result.versionNo}`
  }
  return `已导入 ${result.versionNo}，当前状态：${result.status}`
}

const submitForm = async () => {
  await formRef.value?.validate()
  if (!fileList.value.length) {
    message.error('请上传 doc/docx 文件')
    return
  }
  const file = fileList.value[0]?.raw
  if (!file) {
    message.error('文件读取失败')
    return
  }
  const payload = new FormData()
  payload.append('templateName', formData.templateName.trim())
  if (formData.selectedTemplateId) {
    payload.append('selectedTemplateId', String(formData.selectedTemplateId))
  }
  payload.append('remark', formData.remark || '')
  payload.append('file', file)
  loading.value = true
  try {
    const result = await TemplateApi.importTemplateDoc(payload)
    message.success(resolveImportSuccessMessage(result))
    dialogVisible.value = false
    emit('success')
  } catch (error) {
    message.error(resolveImportErrorMessage(error, '导入失败，请检查模板文件和识别结果'))
    throw error
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
.form-template-import-dialog.el-dialog {
  max-width: calc(100vw - 32px);
}

.form-template-import-dialog {
  .el-dialog__body {
    max-height: calc(100vh - 172px);
    padding: 24px 28px !important;
    overflow-y: auto;
  }

  .el-dialog__footer {
    padding: 16px 28px 20px;
  }

  .form-template-import-dialog__form {
    .el-form-item {
      margin-bottom: 20px;
    }

    .el-form-item:last-child {
      margin-bottom: 0;
    }

    .el-form-item__label {
      height: auto;
      padding: 0;
      margin-bottom: 8px;
      color: #263247;
      font-weight: 600;
      line-height: 20px;
    }

    :where(.el-autocomplete, .el-input, .el-textarea) {
      width: 100%;
    }

    .el-textarea__inner {
      resize: vertical;
    }
  }

  .form-template-import-dialog__hint {
    margin-top: 6px;
    color: #7b8496;
    font-size: 12px;
    line-height: 18px;
  }

  .form-template-import-dialog__upload {
    display: block;
    width: 100%;

    .el-upload {
      display: block;
      width: 100%;
    }

    .el-upload-dragger {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 100%;
      min-height: 136px;
      padding: 24px 20px;
      background: #fafcff;
      border-color: #cbd6e4;
      border-radius: 6px;
      transition:
        background-color 0.2s ease,
        border-color 0.2s ease;
    }

    .el-upload-dragger:hover,
    .el-upload-dragger.is-dragover {
      background: #f4f8ff;
      border-color: var(--scheme-d-primary);
    }

    .iconify {
      width: 30px;
      height: 30px;
      margin-bottom: 10px;
      color: var(--scheme-d-primary);
    }

    .el-upload__text {
      color: #5d6879;
      line-height: 22px;
      text-align: center;
    }

    .el-upload-list {
      margin-top: 10px;
    }

    .el-upload-list__item {
      height: auto;
      min-height: 34px;
      padding: 6px 28px 6px 8px;
      line-height: 20px;
    }

    :where(.el-upload-list__item-name, .el-upload-list__item-file-name) {
      min-width: 0;
      white-space: normal;
      overflow-wrap: anywhere;
    }
  }
}

@media (max-width: 600px) {
  .form-template-import-dialog.el-dialog {
    max-width: calc(100vw - 24px);
  }

  .form-template-import-dialog {
    .el-dialog__body {
      max-height: calc(100dvh - 154px);
      padding: 20px 18px !important;
    }

    .el-dialog__footer {
      padding: 14px 18px 16px;
    }

    .form-template-import-dialog__upload .el-upload-dragger {
      min-height: 120px;
      padding: 20px 12px;
    }
  }
}
</style>
