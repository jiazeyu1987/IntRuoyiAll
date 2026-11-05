<template>
  <Dialog v-model="visible" :title="mode === 'leader' ? '导入生产组长' : '导入班组人员名单'" :width="result.length ? 'min(760px, calc(100vw - 32px))' : 'min(520px, calc(100vw - 32px))'" :fullscreen="false" :close-on-click-modal="!loading" :close-on-press-escape="!loading" :show-close="false">
    <el-upload
      ref="uploadRef" class="roster-upload" drag :show-file-list="false"
      v-model:file-list="files" :auto-upload="false" :limit="1" accept=".xlsx,.xls"
      :disabled="loading" :on-change="clearResult" :on-remove="clearResult"
      :on-exceed="replaceFile" aria-label="选择人员名单文件"
    >
      <span class="roster-upload__icon" aria-hidden="true">
        <Icon :icon="files.length ? 'ep:document-checked' : 'ep:upload-filled'" :size="36" />
      </span>
      <span class="roster-upload__name">{{ files[0]?.name || '选择文件' }}</span>
    </el-upload>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="mt-12px" />
    <el-table v-if="result.length" :data="result" border class="mt-12px" max-height="360">
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="role" label="人员角色" width="90" />
      <el-table-column prop="username" label="登录账号" min-width="150">
        <template #default="{ row }">{{ row.username || '临时工，无登录账号' }}</template>
      </el-table-column>
      <el-table-column prop="result" label="导入结果" min-width="200" />
    </el-table>
    <template #footer>
      <el-button @click="visible = false" :disabled="loading">关闭</el-button>
      <el-button type="primary" :loading="loading" :disabled="!files.length || !!result.length" @click="submit">确认导入</el-button>
    </template>
  </Dialog>
  <Dialog v-model="temporaryPasswordVisible" title="临时工电子签名密码" width="480px">
    <p class="mb-12px">临时工电子签名初始密码：<strong>Int@2025</strong></p>
    <p v-if="newTemporaryNames.length" class="mb-12px">本次新增临时工：{{ newTemporaryNames.join('、') }}。</p>
    <el-alert
      v-if="existingTemporaryCount"
      :title="`本次有 ${existingTemporaryCount} 名临时工已存在，保留原密码；如已修改密码，请使用修改后的密码。`"
      type="info" :closable="false" show-icon class="mb-12px"
    />
    <p>此密码用于临时工电子签名，临时工无系统登录账号。</p>
    <template #footer>
      <el-button type="primary" @click="temporaryPasswordVisible = false">我已知晓</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, genFileId, type UploadInstance, type UploadProps, type UploadUserFile } from 'element-plus'
import { Icon } from '@/components/Icon'
import { importTeamRoster, type TeamRosterImportPerson } from '@/api/mes/pro/processpool/teamRoster'

const emit = defineEmits<{ success: [] }>()
const visible = ref(false)
const loading = ref(false)
const mode = ref<'leader' | 'members'>('members')
const files = ref<UploadUserFile[]>([])
const uploadRef = ref<UploadInstance>()
const result = ref<TeamRosterImportPerson[]>([])
const temporaryPasswordVisible = ref(false)
const temporaryPeople = computed(() => result.value.filter(person => person.role === '组员' && !person.username))
const newTemporaryNames = computed(() => temporaryPeople.value
  .filter(person => person.result === '已导入人员管理').map(person => person.name))
const existingTemporaryCount = computed(() => temporaryPeople.value.length - newTemporaryNames.value.length)
const error = ref('')
const clearResult = () => { result.value = []; error.value = '' }
const replaceFile: UploadProps['onExceed'] = (selectedFiles) => {
  if (loading.value || !selectedFiles.length) return
  clearResult()
  uploadRef.value?.clearFiles()
  uploadRef.value?.handleStart(Object.assign(selectedFiles[0], { uid: genFileId() }))
}
const open = (target: 'leader' | 'members') => {
  if (loading.value) return
  mode.value = target
  temporaryPasswordVisible.value = false
  files.value = []
  clearResult()
  visible.value = true
}
defineExpose({ open })
const submit = async () => {
  const file = files.value[0]?.raw
  if (!file || loading.value) return
  loading.value = true
  error.value = ''
  try {
    result.value = await importTeamRoster(file, mode.value)
    ElMessage.success(`已完成 ${result.value.length} 人的导入`)
    emit('success')
    temporaryPasswordVisible.value = mode.value === 'members' && temporaryPeople.value.length > 0
  } catch (cause: any) {
    error.value = cause?.message || '名单导入失败，请核对名单及系统错误信息后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.roster-upload {
  padding: 12px 4px;

  :deep(.el-upload) {
    display: block;
  }

  :deep(.el-upload-dragger) {
    display: flex;
    min-height: 190px;
    padding: 28px 24px;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16px;
    background: #f8faff;
    border: 1px dashed #c9d5e8;
    border-radius: 10px;
    transition: border-color 0.2s, background-color 0.2s;
  }

  :deep(.el-upload-dragger:hover),
  :deep(.el-upload-dragger.is-dragover),
  :deep(.el-upload:focus-visible .el-upload-dragger) {
    background: #f0f6ff;
    border-color: #1677ff;
  }

  &__icon {
    display: flex;
    width: 64px;
    height: 64px;
    align-items: center;
    justify-content: center;
    color: #1677ff;
    background: #eaf2ff;
    border-radius: 16px;
  }

  &__name {
    max-width: 100%;
    color: #344054;
    font-size: 14px;
    font-weight: 500;
    overflow-wrap: anywhere;
  }
}
</style>
