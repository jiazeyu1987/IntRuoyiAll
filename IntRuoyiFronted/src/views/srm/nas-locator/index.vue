<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :inline="true"
      :model="queryParams"
      label-width="92px"
      @submit.prevent="handleQuery"
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          clearable
          class="!w-320px"
          placeholder="请输入关键词，支持 * 通配，例如 *MO13*.pdf"
        />
      </el-form-item>
      <el-form-item>
        <el-button native-type="submit" v-hasPermi="['srm:nas-locator:query']">
          <Icon icon="ep:search" class="mr-5px" /> 搜索
        </el-button>
        <el-button
          type="primary"
          native-type="button"
          class="nas-locator-toolbar-button"
          :loading="refreshLoading"
          @click="handleRefresh"
          v-hasPermi="['srm:nas-locator:refresh']"
        >
          <Icon icon="ep:refresh" class="mr-5px" /> 刷新
        </el-button>
        <el-button
          type="primary"
          plain
          native-type="button"
          class="nas-locator-toolbar-button"
          @click="openStatusDialog"
        >
          <Icon icon="ep:info-filled" class="mr-5px" /> 详情
        </el-button>
        <el-button
          type="primary"
          plain
          native-type="button"
          class="nas-locator-toolbar-button"
          @click="openBlacklistDialog"
          v-hasPermi="['srm:nas-locator:config']"
        >
          <Icon icon="ep:remove-filled" class="mr-5px" /> 黑名单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table
      v-loading="listLoading || statusLoading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="false"
      row-key="id"
      empty-text="暂无匹配文件"
    >
      <el-table-column label="文件名" min-width="220">
        <template #default="{ row }">
          <div class="nas-locator-file-name">{{ row.fileName }}</div>
        </template>
      </el-table-column>
      <el-table-column label="NAS目录" min-width="240">
        <template #default="{ row }">
          <div class="nas-locator-path-text">{{ row.directoryPath || '根目录' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="完整相对路径" min-width="320">
        <template #default="{ row }">
          <div class="nas-locator-path-text">{{ row.fullPath }}</div>
        </template>
      </el-table-column>
      <el-table-column label="修改时间" width="170">
        <template #default="{ row }">
          {{ formatStatusTime(row.modifiedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="大小" width="120" align="right">
        <template #default="{ row }">
          {{ formatReadableSize(row.size) }}
        </template>
      </el-table-column>
      <el-table-column label="下载" width="100" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :loading="downloadId === row.id"
            @click="handleDownload(row)"
            v-hasPermi="['srm:nas-locator:download']"
          >
            下载
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="nas-locator-table-footer">
      <div class="nas-locator-total-text">共 {{ total }} 个文件</div>
      <el-pagination
        v-show="total > 0"
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :background="true"
        :page-sizes="[20, 50, 100]"
        :small="false"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageSizeChange"
        @current-change="handleCurrentPageChange"
      />
    </div>
  </ContentWrap>

  <el-dialog
    v-model="statusDialogVisible"
    title="NAS索引状态"
    width="min(1080px, calc(100vw - 32px))"
    class="nas-locator-status-dialog"
    destroy-on-close
  >
    <div class="nas-locator-status-grid">
      <div class="nas-locator-status-card">
        <div class="nas-locator-status-label">共享范围</div>
        <div class="nas-locator-status-value">{{ status.scopeShare || '--' }}</div>
      </div>
      <div class="nas-locator-status-card">
        <div class="nas-locator-status-label">索引根路径</div>
        <div class="nas-locator-status-value">{{ status.rootPath || '共享根目录' }}</div>
      </div>
      <div class="nas-locator-status-card">
        <div class="nas-locator-status-label">最近成功刷新</div>
        <div class="nas-locator-status-value">{{ formatStatusTime(status.latestSuccessTime) }}</div>
      </div>
      <div class="nas-locator-status-card">
        <div class="nas-locator-status-label">目录数 / 文件数</div>
        <div class="nas-locator-status-value">{{ status.directoryCount }} / {{ status.fileCount }}</div>
      </div>
      <div class="nas-locator-status-card">
        <div class="nas-locator-status-label">最新任务状态</div>
        <div class="flex items-center gap-8px mt-8px">
          <el-tag :type="resolveStatusTagType(status.latestTaskStatus)">{{
            resolveStatusLabel(status.latestTaskStatus)
          }}</el-tag>
          <span class="nas-locator-status-helper">{{ status.latestTaskStatus || 'IDLE' }}</span>
        </div>
      </div>
      <div class="nas-locator-status-card nas-locator-progress-card">
        <div class="nas-locator-status-label">运行进度</div>
        <template v-if="status.latestTaskStatus === STATUS_RUNNING">
          <div class="nas-locator-progress-share">
            第 {{ status.runningShareIndex || 0 }} / {{ status.runningShareTotal || 0 }} 个共享
          </div>
          <el-progress
            :percentage="resolveRunningPercentage()"
            :stroke-width="10"
            :show-text="false"
            status="warning"
          />
          <div class="nas-locator-progress-meta">
            <div>
              <span class="nas-locator-progress-label">当前共享</span>
              <div class="nas-locator-progress-value">{{ status.runningShare || '--' }}</div>
            </div>
            <div>
              <span class="nas-locator-progress-label">当前目录</span>
              <div class="nas-locator-progress-value">{{ status.runningPath || '--' }}</div>
            </div>
            <div>
              <span class="nas-locator-progress-label">已扫描目录 / 文件</span>
              <div class="nas-locator-progress-value">
                {{ status.runningDirectoryCount ?? 0 }} / {{ status.runningFileCount ?? 0 }}
              </div>
            </div>
          </div>
        </template>
        <div v-else class="nas-locator-status-helper nas-locator-progress-idle">
          仅在刷新执行中显示当前共享、目录与已扫描数量
        </div>
      </div>
    </div>
    <div class="nas-locator-status-banner">
      <div class="nas-locator-status-message">{{ status.message || '请先刷新 NAS 索引' }}</div>
      <div class="nas-locator-status-tip">
        范围固定为 `\\172.30.30.4\质量体系文件` 与 `\\172.30.30.4\生产部` 两个共享根
      </div>
    </div>
  </el-dialog>

  <el-dialog
    v-model="blacklistDialogVisible"
    title="黑名单设置"
    width="min(680px, calc(100vw - 32px))"
    class="nas-locator-blacklist-dialog"
    destroy-on-close
  >
    <div class="nas-locator-blacklist-tip">
      每行一条文件名规则，仅匹配文件名。示例：`*.pyc`、`*MO13*.pdf`
    </div>
    <el-input
      v-model="blacklistForm.patternText"
      type="textarea"
      :rows="10"
      resize="vertical"
      placeholder="每行输入一条规则，例如&#10;*.pyc&#10;*MO13*.pdf"
    />
    <template #footer>
      <div class="nas-locator-blacklist-footer">
        <el-button @click="blacklistDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="blacklistSaving" @click="handleSaveBlacklist">
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { FormInstance } from 'element-plus'
import {
  downloadNasLocatorFile,
  getNasLocatorBlacklist,
  getNasLocatorPage,
  getNasLocatorStatus,
  refreshNasLocator,
  saveNasLocatorBlacklist,
  type SrmNasLocatorBlacklistSaveReqVO,
  type SrmNasLocatorFileRespVO
} from '@/api/srm/nas-locator'
import { formatDate } from '@/utils/formatTime'
import { formatFileSize } from '@/utils/file'

defineOptions({ name: 'SrmNasLocator' })

const STATUS_RUNNING = 'RUNNING'
const STATUS_SUCCESS = 'SUCCESS'
const STATUS_FAILED = 'FAILED'
const STATUS_IDLE = 'IDLE'

const message = useMessage()
const queryFormRef = ref<FormInstance>()
const statusLoading = ref(false)
const listLoading = ref(false)
const refreshLoading = ref(false)
const blacklistSaving = ref(false)
const downloadId = ref<number>()
const statusDialogVisible = ref(false)
const blacklistDialogVisible = ref(false)
const total = ref(0)
const list = ref<SrmNasLocatorFileRespVO[]>([])
const status = reactive({
  scopeShare: '',
  rootPath: '',
  latestTaskStatus: STATUS_IDLE,
  latestSuccessTime: undefined as number | undefined,
  fileCount: 0,
  directoryCount: 0,
  message: '请先刷新 NAS 索引',
  runningShare: undefined as string | undefined,
  runningPath: undefined as string | undefined,
  runningDirectoryCount: undefined as number | undefined,
  runningFileCount: undefined as number | undefined,
  runningShareIndex: undefined as number | undefined,
  runningShareTotal: undefined as number | undefined
})
const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: undefined as string | undefined
})
const blacklistForm = reactive({
  patternText: ''
})

const hasSuccessSnapshot = computed(() => status.latestSuccessTime !== undefined)

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveStatusLabel = (taskStatus?: string) => {
  if (taskStatus === STATUS_RUNNING) return '刷新中'
  if (taskStatus === STATUS_SUCCESS) return '成功'
  if (taskStatus === STATUS_FAILED) return '失败'
  return '待刷新'
}

const resolveStatusTagType = (taskStatus?: string) => {
  if (taskStatus === STATUS_RUNNING) return 'warning'
  if (taskStatus === STATUS_SUCCESS) return 'success'
  if (taskStatus === STATUS_FAILED) return 'danger'
  return 'info'
}

const resolveRunningPercentage = () => {
  if (status.latestTaskStatus !== STATUS_RUNNING) {
    return 0
  }
  const shareIndex = status.runningShareIndex ?? 0
  const shareTotal = status.runningShareTotal ?? 0
  if (shareTotal <= 0 || shareIndex <= 0) {
    return 5
  }
  const percentage = Math.round((shareIndex / shareTotal) * 100)
  return Math.max(5, Math.min(99, percentage))
}

const formatStatusTime = (value?: number) => {
  if (value === undefined) {
    return '--'
  }
  return formatDate(new Date(value), 'YYYY-MM-DD HH:mm')
}

const formatReadableSize = (value?: number) => {
  if (value === undefined || value === null) {
    return '--'
  }
  return formatFileSize(value)
}

const loadStatus = async () => {
  const previousStatus = status.latestTaskStatus
  statusLoading.value = true
  try {
    const data = await getNasLocatorStatus()
    Object.assign(status, data)
    if (previousStatus === STATUS_RUNNING && status.latestTaskStatus === STATUS_SUCCESS) {
      message.success('NAS 索引刷新成功')
      await getList()
    }
    if (previousStatus === STATUS_RUNNING && status.latestTaskStatus === STATUS_FAILED) {
      message.error(status.message || 'NAS 索引刷新失败')
    }
  } catch (error) {
    message.error(resolveErrorMessage(error, 'NAS 状态加载失败，请检查后端接口。'))
  } finally {
    statusLoading.value = false
  }
}

const getList = async () => {
  listLoading.value = true
  try {
    const data = await getNasLocatorPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    message.error(resolveErrorMessage(error, 'NAS 文件搜索失败，请检查索引状态后重试。'))
  } finally {
    listLoading.value = false
  }
}

const handleQuery = async () => {
  queryParams.pageNo = 1
  await getList()
}

const handleRefresh = async () => {
  refreshLoading.value = true
  try {
    await refreshNasLocator()
    message.success('NAS 索引刷新任务已启动')
    await loadStatus()
  } catch (error) {
    message.error(resolveErrorMessage(error, 'NAS 索引刷新启动失败，请稍后重试。'))
  } finally {
    refreshLoading.value = false
  }
}

const openStatusDialog = () => {
  statusDialogVisible.value = true
}

const openBlacklistDialog = async () => {
  try {
    const data = await getNasLocatorBlacklist()
    blacklistForm.patternText = (data.patterns || []).join('\n')
    blacklistDialogVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '黑名单加载失败，请稍后重试。'))
  }
}

const handleSaveBlacklist = async () => {
  blacklistSaving.value = true
  try {
    const payload: SrmNasLocatorBlacklistSaveReqVO = {
      patterns: blacklistForm.patternText.split(/\r?\n/).map((item) => item.trim())
    }
    await saveNasLocatorBlacklist(payload)
    blacklistDialogVisible.value = false
    message.success('黑名单已保存，刷新索引后生效')
  } catch (error) {
    message.error(resolveErrorMessage(error, '黑名单保存失败，请稍后重试。'))
  } finally {
    blacklistSaving.value = false
  }
}

const handleCurrentPageChange = async () => {
  await getList()
}

const handlePageSizeChange = async () => {
  queryParams.pageNo = 1
  await getList()
}

const handleDownload = async (row: SrmNasLocatorFileRespVO) => {
  downloadId.value = row.id
  try {
    const result = await downloadNasLocatorFile(row.id)
    message.success(`开始下载 ${result.fileName}`)
  } catch (error) {
    message.error(resolveErrorMessage(error, 'NAS 文件下载失败，请稍后重试。'))
  } finally {
    downloadId.value = undefined
  }
}

onMounted(async () => {
  await loadStatus()
  if (hasSuccessSnapshot.value) {
    await getList()
  }
})
</script>

<style scoped>
.nas-locator-status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.nas-locator-toolbar-button {
  min-width: 90px;
}

.nas-locator-status-card {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
  padding: 14px 16px;
  min-height: 94px;
}

.nas-locator-status-label {
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}

.nas-locator-status-value {
  margin-top: 8px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  word-break: break-all;
}

.nas-locator-status-helper {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.nas-locator-progress-card {
  min-height: 150px;
}

.nas-locator-progress-share {
  margin-top: 8px;
  margin-bottom: 10px;
  color: #9a6700;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
}

.nas-locator-progress-meta {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.nas-locator-progress-label {
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}

.nas-locator-progress-value {
  margin-top: 4px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  word-break: break-all;
}

.nas-locator-progress-idle {
  margin-top: 10px;
  display: block;
}

.nas-locator-status-banner {
  margin-top: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.nas-locator-status-message {
  color: #172033;
  font-size: 13px;
  line-height: 20px;
}

:deep(.nas-locator-status-dialog .el-dialog__body) {
  padding-top: 8px;
}

.nas-locator-status-tip {
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}

.nas-locator-blacklist-tip {
  margin-bottom: 12px;
  color: #4b5563;
  font-size: 13px;
  line-height: 20px;
}

.nas-locator-blacklist-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.nas-locator-file-name {
  color: #172033;
  font-weight: 600;
  line-height: 22px;
  word-break: break-all;
}

.nas-locator-path-text {
  color: #263247;
  line-height: 20px;
  white-space: normal;
  word-break: break-all;
}

.nas-locator-table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 14px;
}

.nas-locator-total-text {
  color: #4b5563;
  font-size: 13px;
  line-height: 20px;
}

@media (max-width: 768px) {
  .nas-locator-status-grid {
    grid-template-columns: 1fr;
  }

  .nas-locator-table-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
