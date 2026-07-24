<template>
  <Dialog v-model="dialogVisible" title="导入表单模板" width="520">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="96px">
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
        <div class="mt-4px text-12px text-gray-500">
          版本号由系统自动生成；选择已有模板将自动提交升版审批。
        </div>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="formData.remark"
          maxlength="200"
          placeholder="请输入备注"
          type="textarea"
        />
      </el-form-item>
      <el-form-item label="模板文件" prop="file">
        <el-upload
          ref="uploadRef"
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
      <el-button :disabled="loading" @click="dialogVisible = false">取消</el-button>
      <el-button :loading="loading" type="primary" @click="submitForm">导入</el-button>
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
  const selected = templateOptions.value.find((item) => item.templateId === formData.selectedTemplateId)
  if (!selected || selected.templateName !== formData.templateName) {
    formData.selectedTemplateId = undefined
  }
}

const resolveImportErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as { response?: { data?: { msg?: string } } })?.response?.data?.msg
  const directMessage = (error as { message?: string })?.message
  return responseMessage || directMessage || fallback
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
    const successMessage =
      result.importAction === 'UPGRADE'
        ? `已生成 ${result.versionNo} 并提交升版审批`
        : `导入成功，版本 ${result.versionNo}`
    message.success(successMessage)
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
