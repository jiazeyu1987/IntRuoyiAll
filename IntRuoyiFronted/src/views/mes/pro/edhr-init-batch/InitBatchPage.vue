<template>
  <ContentWrap>
    <div class="edhr-init-batch">
      <el-form :inline="true" :model="queryParams" class="edhr-init-batch__toolbar">
        <el-form-item label="项目编码">
          <el-input v-model="queryParams.projectCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="queryParams.projectName" clearable class="!w-220px" />
        </el-form-item>
        <el-form-item label="目标环境">
          <el-select v-model="queryParams.targetEnvironment" clearable class="!w-140px">
            <el-option label="本机" value="LOCAL" />
            <el-option label="测试" value="TEST" />
            <el-option label="生产" value="PROD" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable class="!w-170px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="预检失败" value="PRECHECK_FAILED" />
            <el-option label="预检通过" value="PRECHECK_PASSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button
            type="primary"
            v-hasPermi="['mes:pro-edhr-init-batch:create']"
            @click="openCreateDialog"
          >
            创建批次
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div class="edhr-init-batch__table">
        <el-table
          v-loading="loading"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无初始化批次"
        >
          <el-table-column label="初始化批次" min-width="280">
            <template #default="{ row }">
              <div class="edhr-init-batch__strong">{{ row.projectCode }} / {{ row.projectName }}</div>
              <div class="edhr-init-batch__muted">版本 {{ row.dataVersion }} · 租户 {{ row.targetTenantId }}</div>
            </template>
          </el-table-column>
          <el-table-column label="目标环境" prop="targetEnvironment" width="110" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="resolveStatusTagType(row.status)">{{ resolveStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="manifestHash" min-width="240">
            <template #default="{ row }">
              <span class="edhr-init-batch__hash">{{ row.latestManifestHash || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="manifest" width="110" align="center">
            <template #default="{ row }">
              {{ row.manifestCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="阻塞问题" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.blockingIssueCount > 0 ? 'danger' : 'success'">
                {{ row.blockingIssueCount || 0 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最后预检" prop="lastPrecheckAt" width="180">
            <template #default="{ row }">{{ formatDateTimeValue(row.lastPrecheckAt, '-') }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <div class="edhr-init-batch__row-actions">
                <el-button
                  link
                  type="primary"
                  v-hasPermi="['mes:pro-edhr-init-batch:create']"
                  @click="openUploadDialog(row)"
                >
                  上传 manifest
                </el-button>
                <el-button
                  link
                  type="primary"
                  v-hasPermi="['mes:pro-edhr-init-batch:precheck']"
                  @click="handleRunPrecheck(row)"
                >
                  执行预检
                </el-button>
                <el-button link type="primary" @click="openIssueDrawer(row)">预检问题</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </div>

    <Dialog title="创建批次" v-model="createDialogVisible" width="720px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="116px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目编码" prop="projectCode">
              <el-input v-model="createForm.projectCode" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input v-model="createForm.projectName" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标环境" prop="targetEnvironment">
              <el-select v-model="createForm.targetEnvironment" class="!w-100%">
                <el-option label="本机" value="LOCAL" />
                <el-option label="测试" value="TEST" />
                <el-option label="生产" value="PROD" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标租户" prop="targetTenantId">
              <el-input-number v-model="createForm.targetTenantId" :min="1" :controls="false" class="!w-100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据版本" prop="dataVersion">
              <el-input v-model="createForm.dataVersion" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="交付负责人" prop="ownerUserId">
              <el-input-number v-model="createForm.ownerUserId" :min="1" :controls="false" class="!w-100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="审批负责人" prop="approvalOwnerUserId">
              <el-input-number
                v-model="createForm.approvalOwnerUserId"
                :min="1"
                :controls="false"
                class="!w-100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="初始化范围" prop="initScopeJson">
              <el-input v-model="createForm.initScopeJson" type="textarea" :rows="4" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="createForm.remark" type="textarea" :rows="2" maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">确认创建</el-button>
      </template>
    </Dialog>

    <Dialog title="上传 manifest" v-model="uploadDialogVisible" width="760px">
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="116px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="批次">
              <el-input :model-value="currentBatchLabel" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="包类型" prop="packageType">
              <el-input v-model="uploadForm.packageType" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="manifestHash" prop="manifestHash">
              <el-input v-model="uploadForm.manifestHash" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="源文件" prop="sourceFileName">
              <el-input v-model="uploadForm.sourceFileName" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="源文件URL">
              <el-input v-model="uploadForm.sourceFileUrl" maxlength="512" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="checksum">
              <el-input v-model="uploadForm.checksumJson" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="manifestJson" prop="manifestJson">
              <el-input v-model="uploadForm.manifestJson" type="textarea" :rows="8" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" @click="handleUploadManifest">
          上传 manifest
        </el-button>
      </template>
    </Dialog>

    <el-drawer v-model="issueDrawerVisible" title="预检问题" size="70%">
      <div class="edhr-init-batch__issue-header">
        <div>
          <div class="edhr-init-batch__strong">{{ issueBatch?.projectCode || '--' }}</div>
          <div class="edhr-init-batch__muted">{{ issueBatch?.projectName || '--' }}</div>
        </div>
        <el-button :loading="issueLoading" @click="getIssueList">刷新</el-button>
      </div>
      <el-table
        v-loading="issueLoading"
        :data="issueList"
        stripe
        :show-overflow-tooltip="true"
        empty-text="暂无预检问题"
      >
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="row.issueLevel === 'BLOCKER' ? 'danger' : 'warning'">{{ row.issueLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="源文件" prop="sourceFileName" min-width="180" />
        <el-table-column label="行号" prop="sourceRowNo" width="90" />
        <el-table-column label="字段" prop="sourceFieldName" width="140" />
        <el-table-column label="责任人" min-width="130">
          <template #default="{ row }">
            {{ row.responsibleName || row.responsibleUserId || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="问题" prop="issueMessage" min-width="260" />
        <el-table-column label="下一步动作" prop="remediationSuggestion" min-width="260" />
      </el-table>
      <Pagination
        :total="issueTotal"
        v-model:page="issueQuery.pageNo"
        v-model:limit="issueQuery.pageSize"
        @pagination="getIssueList"
      />
    </el-drawer>
  </ContentWrap>
</template>

<script setup lang="ts">
import { formatDateTimeValue } from '@/utils/formatTime'
import {
  EdhrInitBatchApi,
  type EdhrInitBatchCreateReqVO,
  type EdhrInitBatchRespVO,
  type EdhrInitBatchStatus,
  type EdhrInitIssueRespVO,
  type EdhrInitIssueStatus,
  type EdhrInitManifestUploadReqVO
} from '@/api/mes/pro/edhr/initBatch'

defineOptions({ name: 'MesProEdhrInitBatchPage' })

const message = useMessage()

const loading = ref(false)
const createLoading = ref(false)
const uploadLoading = ref(false)
const issueLoading = ref(false)
const loadError = ref('')
const list = ref<EdhrInitBatchRespVO[]>([])
const total = ref(0)

const createDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const issueDrawerVisible = ref(false)
const currentBatch = ref<EdhrInitBatchRespVO>()
const issueBatch = ref<EdhrInitBatchRespVO>()
const issueList = ref<EdhrInitIssueRespVO[]>([])
const issueTotal = ref(0)
const createFormRef = ref()
const uploadFormRef = ref()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  projectCode: '',
  projectName: '',
  targetEnvironment: undefined as string | undefined,
  targetTenantId: undefined as number | undefined,
  dataVersion: '',
  status: undefined as EdhrInitBatchStatus | undefined
})

const createForm = reactive<EdhrInitBatchCreateReqVO>({
  projectCode: '',
  projectName: '',
  targetEnvironment: 'TEST',
  targetTenantId: 122,
  dataVersion: '',
  ownerUserId: undefined as unknown as number,
  approvalOwnerUserId: undefined as unknown as number,
  initScopeJson: '{"scope":["menu","permission","template","masterData"]}',
  remark: ''
})

const uploadForm = reactive<EdhrInitManifestUploadReqVO>({
  initBatchId: 0,
  packageType: 'COMMERCIAL_DELIVERY',
  manifestHash: '',
  sourceFileName: '',
  sourceFileUrl: '',
  checksumJson: '',
  manifestJson: ''
})

const issueQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  initBatchId: 0,
  issueStatus: 'OPEN' as EdhrInitIssueStatus
})

const createRules = {
  projectCode: [{ required: true, message: '项目编码不能为空', trigger: 'blur' }],
  projectName: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
  targetEnvironment: [{ required: true, message: '目标环境不能为空', trigger: 'change' }],
  targetTenantId: [{ required: true, message: '目标租户不能为空', trigger: 'change' }],
  dataVersion: [{ required: true, message: '数据版本不能为空', trigger: 'blur' }],
  ownerUserId: [{ required: true, message: '交付负责人不能为空', trigger: 'change' }],
  approvalOwnerUserId: [{ required: true, message: '审批负责人不能为空', trigger: 'change' }],
  initScopeJson: [{ required: true, message: '初始化范围不能为空', trigger: 'blur' }]
}

const uploadRules = {
  packageType: [{ required: true, message: '包类型不能为空', trigger: 'blur' }],
  manifestHash: [{ required: true, message: 'manifestHash 不能为空', trigger: 'blur' }],
  sourceFileName: [{ required: true, message: '源文件不能为空', trigger: 'blur' }],
  manifestJson: [{ required: true, message: 'manifestJson 不能为空', trigger: 'blur' }]
}

const currentBatchLabel = computed(() => {
  if (!currentBatch.value) return '--'
  return `${currentBatch.value.projectCode} / ${currentBatch.value.projectName}`
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const resolveStatusLabel = (status?: EdhrInitBatchStatus) => {
  if (status === 'PRECHECK_FAILED') return '预检失败'
  if (status === 'PRECHECK_PASSED') return '预检通过'
  return '草稿'
}

const resolveStatusTagType = (status?: EdhrInitBatchStatus) => {
  if (status === 'PRECHECK_FAILED') return 'danger'
  if (status === 'PRECHECK_PASSED') return 'success'
  return 'info'
}

const buildQuery = () => ({
  pageNo: queryParams.pageNo,
  pageSize: queryParams.pageSize,
  projectCode: queryParams.projectCode.trim() || undefined,
  projectName: queryParams.projectName.trim() || undefined,
  targetEnvironment: queryParams.targetEnvironment,
  targetTenantId: queryParams.targetTenantId,
  dataVersion: queryParams.dataVersion.trim() || undefined,
  status: queryParams.status
})

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await EdhrInitBatchApi.getPage(buildQuery())
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '初始化批次加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.projectCode = ''
  queryParams.projectName = ''
  queryParams.targetEnvironment = undefined
  queryParams.targetTenantId = undefined
  queryParams.dataVersion = ''
  queryParams.status = undefined
  getList()
}

const resetCreateForm = () => {
  createForm.projectCode = ''
  createForm.projectName = ''
  createForm.targetEnvironment = 'TEST'
  createForm.targetTenantId = 122
  createForm.dataVersion = ''
  createForm.ownerUserId = undefined as unknown as number
  createForm.approvalOwnerUserId = undefined as unknown as number
  createForm.initScopeJson = '{"scope":["menu","permission","template","masterData"]}'
  createForm.remark = ''
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const handleCreate = async () => {
  await createFormRef.value?.validate()
  createLoading.value = true
  try {
    await EdhrInitBatchApi.create({
      ...createForm,
      projectCode: createForm.projectCode.trim(),
      projectName: createForm.projectName.trim(),
      dataVersion: createForm.dataVersion.trim(),
      initScopeJson: createForm.initScopeJson.trim(),
      remark: createForm.remark?.trim() || undefined
    })
    createDialogVisible.value = false
    message.success('创建批次成功')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '创建批次失败，请联系管理员。'))
  } finally {
    createLoading.value = false
  }
}

const resetUploadForm = (row: EdhrInitBatchRespVO) => {
  uploadForm.initBatchId = row.id
  uploadForm.packageType = 'COMMERCIAL_DELIVERY'
  uploadForm.manifestHash = ''
  uploadForm.sourceFileName = ''
  uploadForm.sourceFileUrl = ''
  uploadForm.fileSize = undefined
  uploadForm.checksumJson = ''
  uploadForm.manifestJson = ''
}

const openUploadDialog = (row: EdhrInitBatchRespVO) => {
  currentBatch.value = row
  resetUploadForm(row)
  uploadDialogVisible.value = true
}

const handleUploadManifest = async () => {
  await uploadFormRef.value?.validate()
  uploadLoading.value = true
  try {
    await EdhrInitBatchApi.uploadManifest({
      ...uploadForm,
      packageType: uploadForm.packageType.trim(),
      manifestHash: uploadForm.manifestHash.trim(),
      sourceFileName: uploadForm.sourceFileName.trim(),
      sourceFileUrl: uploadForm.sourceFileUrl?.trim() || undefined,
      checksumJson: uploadForm.checksumJson?.trim() || undefined,
      manifestJson: uploadForm.manifestJson.trim()
    })
    uploadDialogVisible.value = false
    message.success('上传 manifest 成功')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '上传 manifest 失败，请联系管理员。'))
  } finally {
    uploadLoading.value = false
  }
}

const handleRunPrecheck = async (row: EdhrInitBatchRespVO) => {
  loading.value = true
  try {
    const result = await EdhrInitBatchApi.runPrecheck(row.id)
    if (result.blockingIssueCount > 0) {
      message.warning(`执行预检完成，发现 ${result.blockingIssueCount} 个阻塞问题。`)
    } else {
      message.success('执行预检通过')
    }
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '执行预检失败，请联系管理员。'))
  } finally {
    loading.value = false
  }
}

const openIssueDrawer = async (row: EdhrInitBatchRespVO) => {
  issueBatch.value = row
  issueQuery.pageNo = 1
  issueQuery.initBatchId = row.id
  issueDrawerVisible.value = true
  await getIssueList()
}

const getIssueList = async () => {
  if (!issueQuery.initBatchId) {
    issueList.value = []
    issueTotal.value = 0
    return
  }
  issueLoading.value = true
  try {
    const data = await EdhrInitBatchApi.getIssuePage(issueQuery)
    issueList.value = data.list || []
    issueTotal.value = data.total || 0
  } catch (error) {
    issueList.value = []
    issueTotal.value = 0
    message.error(resolveErrorMessage(error, '预检问题加载失败，请联系管理员。'))
  } finally {
    issueLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.edhr-init-batch__toolbar,
.edhr-init-batch__table {
  padding: 16px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-init-batch__toolbar {
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  padding-bottom: 0;
}

.edhr-init-batch__table {
  border-top: 0;
  border-radius: 0 0 8px 8px;
}

.edhr-init-batch__table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-init-batch__table :deep(.el-table__row) {
  height: 52px;
}

.edhr-init-batch__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-init-batch__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-init-batch__hash {
  color: #263247;
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.edhr-init-batch__row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.edhr-init-batch__row-actions :deep(.el-button) {
  min-height: auto;
  padding: 0;
}

.edhr-init-batch__issue-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  padding: 12px 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}
</style>
