<template>
  <ContentWrap class="controlled-upload-page">
    <ControlledFileWorkbenchEntry class="mb-12px" />
    <el-alert class="mb-16px" type="info" :closable="false" title="用于登记已经受控的文件，可同时上传附件。本次登记时间、生效时间及上传人由系统记录。" />
    <el-alert v-if="error" class="mb-16px" type="error" :closable="false" :title="error" />
    <el-result v-if="result" icon="success" title="已受控文件上传成功">
      <template #sub-title>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="文件名称">{{ result.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件编号 / 版本">{{ fileNumber }} / {{ result.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="登记受控时间">{{ formatDateTimeValue(result.controlledTime) }}</el-descriptions-item>
          <el-descriptions-item label="生效时间">{{ formatDateTimeValue(result.effectiveTime) }}</el-descriptions-item>
          <el-descriptions-item label="本次上传人">{{ result.importedBy }}</el-descriptions-item>
          <el-descriptions-item label="本次上传时间">{{ formatDateTimeValue(result.importedTime) }}</el-descriptions-item>
          <el-descriptions-item label="上传来源">已受控文件上传</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #extra>
        <el-button type="primary" @click="openResult">查看文件</el-button>
        <el-button @click="reset">继续上传</el-button>
      </template>
    </el-result>
    <el-form v-else v-loading="loading" class="controlled-upload-form" label-width="96px" :disabled="submitting" @submit.prevent="submit">
      <div class="controlled-upload-layout">
        <div class="controlled-upload-fields">
          <section class="upload-card">
            <h2 class="upload-card-title">存放位置</h2>
            <el-form-item label="目标范围" required>
              <el-radio-group v-model="libraryScope" @change="loadScope">
                <el-radio-button value="PROJECT">项目</el-radio-button>
                <el-radio-button value="QMS">QMS 文件</el-radio-button>
                <el-radio-button value="GENERAL">通用文件</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="libraryScope === 'PROJECT'" label="目标项目" required>
              <el-select v-model="projectId" filterable clearable class="!w-full" placeholder="选择有编辑授权的项目" @change="loadProject">
                <el-option v-for="item in projects" :key="item.id" :value="String(item.id)" :label="`${item.projectName} / ${item.projectCode}`" />
              </el-select>
            </el-form-item>
            <el-form-item :label="libraryScope === 'PROJECT' ? '项目目录' : libraryScope === 'QMS' ? 'QMS 目录' : '通用目录'" required>
              <el-select v-model="folderId" filterable clearable class="!w-full" :disabled="!optionsReady" :placeholder="libraryScope === 'PROJECT' ? '选择项目目录' : libraryScope === 'QMS' ? '选择 QMS 文件目录' : '选择通用文件目录'">
                <el-option v-for="item in folderOptions" :key="item.id" :value="String(item.id)" :label="item.path" />
              </el-select>
            </el-form-item>
          </section>
          <section class="upload-card">
            <h2 class="upload-card-title">上传文件</h2>
            <el-form-item label="受控文件" required>
              <div class="main-file-row">
                <template v-if="file">
                  <Icon class="file-icon" icon="ep:document" :size="24" />
                  <span class="file-name">{{ file.name }}</span>
                </template>
                <el-upload ref="mainUpload" :auto-upload="false" :limit="1" :show-file-list="false" :file-list="files" :on-change="selectFile" :on-remove="removeFile" :on-exceed="replaceFile">
                  <el-button :text="!!file" :type="file ? 'primary' : 'default'">{{ file ? '更换' : '选择文件' }}</el-button>
                </el-upload>
                <el-button v-if="file" text type="danger" @click="removeFile">移除</el-button>
              </div>
            </el-form-item>
            <div class="attachment-section">
              <div class="attachment-header">
                <span>附件（{{ attachmentFiles.length }}）<span class="optional-hint">选填</span></span>
                <el-upload :auto-upload="false" multiple :show-file-list="false" :file-list="attachmentFiles" :on-change="selectAttachment" :on-remove="removeAttachment">
                  <el-button plain type="primary" size="small">添加附件</el-button>
                </el-upload>
              </div>
              <ul v-if="attachmentFiles.length" class="attachment-list">
                <li v-for="attachment in attachmentFiles" :key="attachment.uid" class="attachment-row">
                  <Icon class="file-icon" icon="ep:document" :size="20" />
                  <span class="file-name">{{ attachment.name }}</span>
                  <el-button text type="danger" @click="removeAttachment(attachment)">移除</el-button>
                </li>
              </ul>
            </div>
          </section>
          <section class="upload-card">
            <h2 class="upload-card-title">文件登记信息</h2>
            <el-form-item label="文件类型" required>
              <el-select v-model="categoryId" filterable clearable class="!w-full" :disabled="!optionsReady" placeholder="选择有编辑授权的文件类型">
                <el-option v-for="item in options?.fileTypes || []" :key="item.categoryId" :value="String(item.categoryId)" :label="item.label" :disabled="Boolean(item.uploadBlocker)" />
              </el-select>
            </el-form-item>
            <el-form-item label="文件编号" required><el-input v-model="fileNumber" maxlength="128" show-word-limit /></el-form-item>
            <el-form-item label="版本" required>
              <el-input v-model="versionNo" placeholder="如 A/1" />
              <div class="version-hint">填写文件现有的受控版本，按当前正式版本规范填写。</div>
            </el-form-item>
          </section>
        </div>
        <div class="controlled-upload-preview-area">
          <ControlledUploadPreview :file="file" />
        </div>
      </div>
      <ControlledUploadActionBar :loading="submitting" :disabled="loading || !optionsReady || submitting" @submit="submit" />
    </el-form>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import type { UploadFile, UploadInstance, UploadRawFile, UploadUserFile } from 'element-plus'
import { useRouter } from 'vue-router'
import { generateUUID } from '@/utils'
import { formatDateTimeValue } from '@/utils/formatTime'
import { buildDccBodyReadQuery } from '@/api/dcc/controlledFile/bodyReadContext'
import {
  getControlledImportOptions, getControlledImportLibrary, importControlledFile,
  type ControlledImportOptions, type ControlledImportProject, type ControlledImportResult
} from '@/api/dcc/controlledFile/controlledImports'
import ControlledFileWorkbenchEntry from '../shared/ControlledFileWorkbenchEntry.vue'
import ControlledUploadPreview from './ControlledUploadPreview.vue'
import ControlledUploadActionBar from './ControlledUploadActionBar.vue'

defineOptions({ name: 'DccControlledFileControlledUpload' })

const router = useRouter()
const projects = ref<ControlledImportProject[]>([])
const options = ref<ControlledImportOptions>()
const libraryScope = ref<'PROJECT' | 'QMS' | 'GENERAL'>('PROJECT')
const projectId = ref('')
const folderId = ref('')
const categoryId = ref('')
const fileNumber = ref('')
const versionNo = ref('')
const attachmentFiles = ref<UploadUserFile[]>([])
const files = ref<UploadUserFile[]>([])
const mainUpload = ref<UploadInstance>()
const file = ref<File>()
const error = ref('')
const loading = ref(false)
const submitting = ref(false)
const result = ref<ControlledImportResult>()
const idempotencyKey = ref(generateUUID())
let sequence = 0
const optionsReady = computed(() => !!projectId.value && !!options.value && !loading.value)
const folderOptions = ref<Array<{ id: number | string; path: string }>>([])
const buildFolderOptions = (folders: ControlledImportOptions['folders']) => {
  const nodes = new Map(folders.map(item => [String(item.id), item]))
  if (nodes.size !== folders.length) throw new Error('项目目录身份重复')
  return folders.map(item => {
    const names: string[] = [], visited = new Set<string>()
    let current: typeof item | undefined = item
    while (current) {
      const id = String(current.id)
      if (visited.has(id)) throw new Error('项目目录存在循环')
      visited.add(id); names.unshift(current.name)
      if (String(current.parentId) === '0') break
      current = nodes.get(String(current.parentId))
      if (!current) throw new Error('项目目录上级缺失')
    }
    return { id: item.id, path: names.join(' / ') }
  })
}
const selectFile = (value: UploadFile) => { file.value = value.raw; files.value = [value] }
const removeFile = () => { file.value = undefined; files.value = [] }
const replaceFile = (values: File[]) => {
  if (submitting.value) return
  mainUpload.value!.clearFiles()
  mainUpload.value!.handleStart(values[0] as UploadRawFile)
}

const selectAttachment = (_file: UploadFile, values: UploadFile[]) => { attachmentFiles.value = values }
const removeAttachment = (value: UploadUserFile) => {
  attachmentFiles.value = attachmentFiles.value.filter(item => item.uid !== value.uid)
}

const loadProject = async () => {
  const requestSequence = ++sequence
  options.value = undefined
  folderOptions.value = []
  folderId.value = ''; categoryId.value = ''
  error.value = ''
  loading.value = false
  if (!projectId.value) return
  loading.value = true
  try {
    const value = await getControlledImportOptions(projectId.value)
    if (requestSequence !== sequence) return
    folderOptions.value = buildFolderOptions(value.folders)
    options.value = value
  } catch (cause) {
    if (requestSequence === sequence) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (requestSequence === sequence) loading.value = false }
}

const submit = async () => {
  if (submitting.value) return
  error.value = ''
  const type = options.value?.fileTypes.find(item => String(item.categoryId) === categoryId.value)
  if (!optionsReady.value || !projects.value.some(item => String(item.id) === projectId.value) ||
      !options.value?.folders.some(item => String(item.id) === folderId.value) || !type) {
    error.value = '请选择当前授权的项目、目录及文件类型'; return
  }
  if (type.uploadBlocker) { error.value = type.uploadBlocker; return }
  if (!fileNumber.value.trim() || fileNumber.value.trim().length > 128 || !versionNo.value.trim()) {
    error.value = '请填写有效文件编号及版本'; return
  }
  if (!/^[A-HJ-NP-Z]+(?:\/[1-9])+$/.test(versionNo.value.trim().toUpperCase())) {
    error.value = '版本格式无效，请按正式版本规范填写（如 A/1，不含 I、O）'; return
  }
  if (!file.value?.size) { error.value = '请选择非空受控文件'; return }
  if (attachmentFiles.value.some(item => !item.raw?.size)) { error.value = '请选择非空附件'; return }
  const data = new FormData()
  data.append('idempotencyKey', idempotencyKey.value)
  data.append('dccProjectCodeId', projectId.value)
  data.append('projectFolderId', folderId.value)
  data.append('categoryId', categoryId.value)
  data.append('fileTypeTaxonomyId', String(type.fileTypeTaxonomyId))
  data.append('fileNumber', fileNumber.value.trim())
  data.append('versionNo', versionNo.value.trim())
  attachmentFiles.value.forEach(item => data.append('attachments', item.raw!, item.name))
  data.append('file', file.value, file.value.name)
  submitting.value = true
  try { result.value = await importControlledFile(data) }
  catch (cause) { error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { submitting.value = false }
}

const reset = () => {
  result.value = undefined; error.value = ''; fileNumber.value = ''; versionNo.value = ''
  attachmentFiles.value = []; removeFile()
  idempotencyKey.value = generateUUID()
}

const openResult = () => {
  const value = result.value
  if (!value) return
  return router.push({ path: `/dcc/controlled-file/detail/${value.id}`,
    query: { from: 'controlled-import', management: '1', returnTo: '/dcc/controlled-file/controlled-upload',
      ...buildDccBodyReadQuery(value.id, value.importSource === 'CONTROLLED_IMPORT' && value.status === 'CONTROLLED_PENDING_EFFECTIVE'
        ? { intent: 'VERSION_HISTORY', controlledFileId: String(value.id), expectedVersionNo: value.versionNo }
        : { intent: 'DAILY', controlledFileId: String(value.id) }) } })
}

const loadScope = async () => {
  sequence++
  const scope = libraryScope.value
  projects.value = []; projectId.value = ''; options.value = undefined; folderOptions.value = []
  folderId.value = ''; categoryId.value = ''; error.value = ''; loading.value = true
  try {
    if (scope === 'PROJECT') {
      const value = await getControlledImportOptions()
      if (libraryScope.value !== scope) return
      projects.value = value.projects
    } else {
      const library = await getControlledImportLibrary(scope)
      if (libraryScope.value !== scope) return
      projects.value = [library]; projectId.value = String(library.id)
      await loadProject()
    }
  } catch (cause) {
    if (libraryScope.value === scope) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (libraryScope.value === scope) loading.value = false }
}
onMounted(loadScope)
onBeforeUnmount(() => { sequence++ })
</script>

<style scoped lang="scss">
.controlled-upload-form {
  padding-bottom: 88px;
}

.controlled-upload-layout {
  display: grid;
  grid-template-columns: minmax(0, 45fr) minmax(0, 55fr);
  align-items: start;
  gap: 16px;
}

.controlled-upload-fields {
  display: grid;
  min-width: 0;
  gap: 12px;
}

.upload-card {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);

  :deep(.el-form-item) {
    margin-bottom: 12px;
  }

  :deep(.el-form-item:last-child) {
    margin-bottom: 0;
  }
}

.upload-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;

  &::before {
    width: 4px;
    height: 18px;
    border-radius: 2px;
    background: var(--el-color-primary);
    content: '';
  }
}

.main-file-row,
.attachment-row {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  gap: 8px;

  .el-button + .el-button {
    margin-left: 0;
  }
}

.file-icon {
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
}

.file-name {
  flex: 1;
  min-width: 0;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.attachment-section {
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.attachment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.optional-hint,
.version-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.optional-hint {
  margin-left: 8px;
}

.version-hint {
  margin-top: 4px;
  line-height: 1.5;
}

.attachment-list {
  max-height: 168px;
  padding: 0;
  margin: 10px 0 0;
  overflow-y: auto;
  list-style: none;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}

.attachment-row {
  box-sizing: border-box;
  padding: 6px 10px;

  & + & {
    border-top: 1px solid var(--el-border-color-lighter);
  }
}

.controlled-upload-preview-area {
  position: sticky;
  top: 12px;
  height: max(480px, calc(100vh - 300px));
  min-width: 0;
  overflow: auto;

  > :deep(*) {
    height: 100%;
    min-height: 0;
  }
}

@media (max-width: 1000px) {
  .controlled-upload-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .controlled-upload-preview-area {
    position: static;
    height: 600px;
  }
}

@media (max-width: 767px) {
  .controlled-upload-form {
    padding-bottom: calc(112px + env(safe-area-inset-bottom));
  }
}

@media (max-width: 480px) {
  .upload-card {
    padding: 12px;
  }

  .upload-card :deep(.el-form-item__label) {
    width: 80px !important;
  }

  .upload-card :deep(.el-radio-button__inner) {
    padding: 8px 10px;
  }

  .main-file-row {
    flex-wrap: wrap;
  }

  .file-name {
    flex-basis: 45%;
  }
}
</style>
