<template>
  <doc-alert title="上传下载" url="https://doc.iocoder.cn/file/" />
  <!-- 搜索 -->
  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="文件路径" prop="path">
        <el-input
          v-model="queryParams.path"
          placeholder="请输入文件路径"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="文件类型" prop="type" width="80">
        <el-input
          v-model="queryParams.type"
          placeholder="请输入文件类型"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm">
          <Icon icon="ep:upload" class="mr-5px" /> 上传文件
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="checkedIds.length === 0 || hasProtectedCheckedFile"
          @click="handleDeleteBatch"
          v-hasPermi="['infra:file:delete']"
        >
          <Icon icon="ep:delete" class="mr-5px" /> 批量删除
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" @selection-change="handleRowCheckboxChange">
      <el-table-column type="selection" width="55" :selectable="isFileRowSelectable" />
      <el-table-column label="文件名" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="文件路径" align="center" prop="path" :show-overflow-tooltip="true">
        <template #default="{ row }">
          <div class="flex items-center justify-center gap-6px">
            <span class="truncate">{{ row.path }}</span>
            <el-tag
              v-if="isProtectedShowroomFile(row)"
              type="danger"
              size="small"
              effect="plain"
            >
              受保护
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="URL" align="center" prop="url" :show-overflow-tooltip="true" />
      <el-table-column
        label="文件大小"
        align="center"
        prop="size"
        width="120"
        :formatter="fileSizeFormatter"
      />
      <el-table-column label="文件类型" align="center" prop="type" width="180px" />
      <el-table-column label="文件内容" align="center" prop="url" width="110px">
        <template #default="{ row }">
          <el-button link type="primary" :loading="openingFileId === row.id" @click="openFile(row)">
            {{ canPreviewFile(row) ? '预览' : '下载' }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column
        label="上传时间"
        align="center"
        prop="createTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openUploadTimeForm(scope.row)"
            v-hasPermi="['intern-user:time-maintenance:file-upload-time:update']"
          >
            <Icon icon="ep:edit" class="mr-4px" />修改时间
          </el-button>

          <el-button
            link
            type="danger"
            :disabled="isProtectedShowroomFile(scope.row)"
            title="展厅文件配置 28 的 showroom/ 媒体受保护，禁止在文件管理页删除"
            @click="handleDelete(scope.row)"
            v-hasPermi="['infra:file:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <FileForm ref="formRef" @success="getList" />
  <FileUploadTimeForm ref="uploadTimeFormRef" @success="getList" />

  <el-dialog v-model="previewVisible" :title="previewName" width="80%" destroy-on-close @closed="clearPreview">
    <img v-if="previewType.startsWith('image/')" :src="previewUrl" :alt="previewName" class="max-h-[75vh] max-w-full mx-auto" />
    <iframe v-else :src="previewUrl" :title="previewName" class="w-full h-[75vh] border-0"></iframe>
  </el-dialog>
</template>
<script lang="ts" setup>
import { isSearchFormInputEmpty } from '@/utils/search'
import { fileSizeFormatter } from '@/utils'
import { dateFormatter } from '@/utils/formatTime'
import * as FileApi from '@/api/infra/file'
import FileForm from './FileForm.vue'
import FileUploadTimeForm from './FileUploadTimeForm.vue'
import InternUserTimeAuditDialog from '@/views/intern-user/time-maintenance/InternUserTimeAuditDialog.vue'
import { authContextRevision } from '@/utils/auth'

defineOptions({ name: 'InfraFile' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  type: undefined,
  path: undefined,
  createTime: []
})
const queryFormRef = ref() // 搜索的表单
const PROTECTED_SHOWROOM_FILE_CONFIG_ID = 28
const PROTECTED_SHOWROOM_PATH_PREFIX = 'showroom/'
const PROTECTED_SHOWROOM_FILE_MESSAGE =
  '展厅文件配置 28 的 showroom/ 媒体受保护，禁止在文件管理页删除'

const isProtectedShowroomFile = (row?: { configId?: number; path?: string }) => {
  return (
    row?.configId === PROTECTED_SHOWROOM_FILE_CONFIG_ID &&
    !!row?.path &&
    row.path.replace(/^tenants\/\d+\//, '').startsWith(PROTECTED_SHOWROOM_PATH_PREFIX)
  )
}

const isFileRowSelectable = (row) => !isProtectedShowroomFile(row)

/** 查询列表 */
const getList = async () => {
  loading.value = true
  const revision = authContextRevision.value
  try {
    const data = await FileApi.getFilePage(queryParams, () => revision === authContextRevision.value)
    if (revision !== authContextRevision.value) return
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery(true)
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = () => {
  formRef.value.open()
}

const uploadTimeFormRef = ref()
const openUploadTimeForm = (row) => {
  uploadTimeFormRef.value.open(row)
}
type FileRow = { id: number; configId: number; path: string; name: string; type?: string }
const openingFileId = ref<number>()
const previewVisible = ref(false)
const previewUrl = ref('')
const previewName = ref('')
const previewType = ref('')
let previewRequest = 0
const canPreviewFile = (row: FileRow) => row.type?.startsWith('image/') || row.type === 'application/pdf'
const clearPreview = () => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}
const openFile = async (row: FileRow) => {
  const requestId = ++previewRequest
  const revision = authContextRevision.value
  openingFileId.value = row.id
  try {
    const blob = await FileApi.readFileContent(row, () => revision === authContextRevision.value)
    if (revision !== authContextRevision.value || requestId !== previewRequest) return
    clearPreview()
    if (canPreviewFile(row)) {
      previewUrl.value = URL.createObjectURL(new Blob([blob], { type: row.type }))
      previewName.value = row.name
      previewType.value = row.type || ''
      previewVisible.value = true
    } else {
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = row.name
      anchor.click()
      setTimeout(() => URL.revokeObjectURL(url), 60000)
    }
  } finally {
    if (requestId === previewRequest) openingFileId.value = undefined
  }
}
watch(authContextRevision, () => {
  previewRequest += 1
  openingFileId.value = undefined
  previewVisible.value = false
  clearPreview()
  list.value = []
  total.value = 0
  checkedIds.value = []
  checkedFileRows.value = []
}, { flush: 'sync' })
onBeforeUnmount(() => {
  previewRequest += 1
  clearPreview()
})

/** 删除按钮操作 */
const handleDelete = async (row) => {
  if (isProtectedShowroomFile(row)) {
    message.warning(PROTECTED_SHOWROOM_FILE_MESSAGE)
    return
  }
  const revision = authContextRevision.value
  const isCurrentContext = () => revision === authContextRevision.value
  try {
    // 删除的二次确认
    await message.delConfirm()
    if (!isCurrentContext()) throw new Error('登录或租户已切换，请重新选择文件')
    // 发起删除
    await FileApi.deleteFile(row.id, isCurrentContext)
    if (!isCurrentContext()) return
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  }
}

/** 批量删除按钮操作 */
const checkedIds = ref<number[]>([])
const checkedFileRows = ref<any[]>([])
const hasProtectedCheckedFile = computed(() => checkedFileRows.value.some(isProtectedShowroomFile))
const handleRowCheckboxChange = (rows) => {
  checkedFileRows.value = rows
  checkedIds.value = rows.map((row) => row.id)
}

const handleDeleteBatch = async () => {
  if (hasProtectedCheckedFile.value) {
    message.warning(PROTECTED_SHOWROOM_FILE_MESSAGE)
    return
  }
  const revision = authContextRevision.value
  const isCurrentContext = () => revision === authContextRevision.value
  const ids = [...checkedIds.value]
  try {
    // 删除的二次确认
    await message.delConfirm()
    if (!isCurrentContext()) throw new Error('登录或租户已切换，请重新选择文件')
    // 发起批量删除
    await FileApi.deleteFileList(ids, isCurrentContext)
    if (!isCurrentContext()) return
    checkedIds.value = []
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
