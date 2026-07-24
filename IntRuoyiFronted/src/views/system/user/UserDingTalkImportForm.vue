<template>
  <Dialog v-model="dialogVisible" title="钉钉导入" width="420">
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :auto-upload="false"
      :disabled="formLoading"
      :limit="1"
      :on-exceed="handleExceed"
      accept=".xlsx, .xls"
      action="none"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <span>仅允许导入钉钉在职员工的 xls、xlsx 文件。</span>
        </div>
      </template>
    </el-upload>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import type { UploadUserFile } from 'element-plus'
import * as UserApi from '@/api/system/user'

defineOptions({ name: 'SystemUserDingTalkImportForm' })

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])

const emits = defineEmits(['success'])

/** 打开弹窗 */
const open = async () => {
  dialogVisible.value = true
  await resetForm()
}
defineExpose({ open })

/** 提交表单 */
const submitForm = async () => {
  if (fileList.value.length === 0) {
    message.error('请上传文件')
    return
  }
  formLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', fileList.value[0].raw as Blob)
    const response = await UserApi.importDingTalkUsers(formData)
    submitFormSuccess(response)
  } catch {
    submitFormError()
  } finally {
    formLoading.value = false
  }
}

/** 文件上传成功 */
const submitFormSuccess = (response: any) => {
  if (response.code !== 0) {
    message.error(response.msg)
    return
  }
  const data = response.data
  let text = '创建用户数量：' + data.createUsernames.length + ';'
  for (const username of data.createUsernames) {
    text += '< ' + username + ' >'
  }
  text += '导入失败数量：' + Object.keys(data.failureUsernames).length + ';'
  for (const username in data.failureUsernames) {
    text += '< ' + username + ': ' + data.failureUsernames[username] + ' >'
  }
  text += '新建部门数量：' + data.createdDeptPaths.length + ';'
  for (const deptPath of data.createdDeptPaths) {
    text += '< ' + deptPath + ' >'
  }
  text += '启用部门数量：' + data.enabledDeptPaths.length + ';'
  for (const deptPath of data.enabledDeptPaths) {
    text += '< ' + deptPath + ' >'
  }
  text += '负责人回填数量：' + Object.keys(data.leaderAssignedDeptPaths).length + ';'
  for (const deptPath in data.leaderAssignedDeptPaths) {
    text += '< ' + deptPath + ': ' + data.leaderAssignedDeptPaths[deptPath] + ' >'
  }
  text += '负责人跳过数量：' + Object.keys(data.leaderSkippedDeptPaths).length + ';'
  for (const deptPath in data.leaderSkippedDeptPaths) {
    text += '< ' + deptPath + ': ' + data.leaderSkippedDeptPaths[deptPath] + ' >'
  }
  message.alert(text)
  dialogVisible.value = false
  emits('success')
}

/** 上传错误提示 */
const submitFormError = () => {
  message.error('上传失败，请您重新上传！')
}

/** 重置表单 */
const resetForm = async () => {
  fileList.value = []
  await nextTick()
  uploadRef.value?.clearFiles()
}

/** 文件数超出提示 */
const handleExceed = () => {
  message.error('最多只能上传一个文件！')
}
</script>
