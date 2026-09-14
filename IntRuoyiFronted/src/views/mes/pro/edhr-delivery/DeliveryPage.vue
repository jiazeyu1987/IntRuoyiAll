<template>
  <ContentWrap>
    <div class="edhr-delivery">
      <section class="edhr-delivery__toolbar">
        <div class="edhr-delivery__title-row">
          <div>
            <h2>交付驾驶舱</h2>
            <div class="edhr-delivery__subtitle">
              交付项目 / 证据包 / 缺失证据 / 恢复演练 / 培训覆盖 / 发布标签 / schema版本
            </div>
          </div>
          <el-tag :type="gateSummary?.signoffAllowed ? 'success' : 'danger'" effect="plain">
            {{ gateSummary?.signoffAllowed ? '允许签核' : '不允许签核' }}
          </el-tag>
        </div>

        <el-form :inline="true" :model="projectQueryParams" class="edhr-delivery__form" @submit.prevent>
          <el-form-item label="项目名称">
            <el-input
              v-model="projectQueryParams.projectName"
              clearable
              class="!w-190px"
              @keyup.enter="handleProjectQuery"
            />
          </el-form-item>
          <el-form-item label="客户">
            <el-input
              v-model="projectQueryParams.customerName"
              clearable
              class="!w-150px"
              @keyup.enter="handleProjectQuery"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="projectQueryParams.projectStatus" clearable class="!w-130px">
              <el-option label="阻断" value="BLOCKED" />
              <el-option label="就绪" value="READY" />
              <el-option label="已签核" value="SIGNED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleProjectQuery" v-hasPermi="['mes:pro-edhr-delivery:query']">
              <Icon icon="ep:search" class="mr-5px" />
              查询
            </el-button>
            <el-button @click="resetProjectQuery">
              <Icon icon="ep:refresh" class="mr-5px" />
              重置
            </el-button>
            <el-button type="success" @click="openCreateDialog" v-hasPermi="['mes:pro-edhr-delivery:create']">
              <Icon icon="ep:plus" class="mr-5px" />
              新建项目
            </el-button>
          </el-form-item>
        </el-form>
      </section>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <section class="edhr-delivery__project">
        <div class="edhr-delivery__section-title">
          <span>交付项目</span>
          <span class="edhr-delivery__muted">{{ selectedProject?.projectCode || '未选择项目' }}</span>
        </div>
        <el-table
          v-loading="projectLoading"
          :data="projectList"
          stripe
          highlight-current-row
          :show-overflow-tooltip="true"
          empty-text="暂无交付项目"
          @row-click="handleSelectProject"
        >
          <el-table-column label="项目" min-width="250">
            <template #default="{ row }">
              <div class="edhr-delivery__strong">{{ row.projectName }}</div>
              <div class="edhr-delivery__muted">{{ row.projectCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="客户" prop="customerName" min-width="140" />
          <el-table-column label="现场" prop="siteName" min-width="140" />
          <el-table-column label="发布标签" prop="releaseTag" min-width="150" />
          <el-table-column label="schema版本" prop="schemaVersion" min-width="150" />
          <el-table-column label="负责人" prop="ownerName" min-width="130" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.projectStatus === 'BLOCKED' ? 'danger' : 'success'">
                {{ row.projectStatus || '--' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="签核" width="120">
            <template #default="{ row }">
              <el-tag :type="row.signoffAllowed ? 'success' : 'danger'" effect="plain">
                {{ row.signoffAllowed ? '允许' : '不允许签核' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="handleSelectProject(row)" v-hasPermi="['mes:pro-edhr-delivery:query']">
                <Icon icon="ep:view" class="mr-4px" />
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="projectTotal"
          v-model:page="projectQueryParams.pageNo"
          v-model:limit="projectQueryParams.pageSize"
          @pagination="getProjectList"
        />
      </section>

      <section v-if="selectedProject && !selectedProject.signoffAllowed" class="edhr-delivery__blocked">
        <el-alert
          :title="selectedProject.blockedReason || '当前交付项目存在缺失证据，不允许签核。'"
          type="warning"
          :closable="false"
          show-icon
        />
      </section>

      <section class="edhr-delivery__detail-grid">
        <div class="edhr-delivery__packages">
          <div class="edhr-delivery__section-title">
            <span>证据包</span>
            <span class="edhr-delivery__muted">{{ packageList.length }} 项</span>
          </div>
          <el-alert
            v-if="packageError"
            :title="packageError"
            type="error"
            :closable="false"
            show-icon
          />
          <el-table
            v-loading="packageLoading"
            :data="packageList"
            stripe
            :show-overflow-tooltip="true"
            empty-text="请选择交付项目后查看证据包"
          >
            <el-table-column label="证据包" min-width="210">
              <template #default="{ row }">
                <div class="edhr-delivery__strong">{{ row.packageName }}</div>
                <div class="edhr-delivery__muted">{{ row.packageCode }}</div>
              </template>
            </el-table-column>
            <el-table-column label="证据状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.evidenceStatus === 'READY' ? 'success' : 'danger'">
                  {{ row.evidenceStatus || '--' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="责任人" prop="ownerName" width="120" />
            <el-table-column label="缺失证据" min-width="260">
              <template #default="{ row }">{{ formatEvidence(row.missingEvidenceJson) }}</template>
            </el-table-column>
            <el-table-column label="下一步动作" prop="nextAction" min-width="220" />
            <el-table-column label="签核影响" prop="signoffImpact" min-width="180" />
          </el-table>
        </div>

        <div class="edhr-delivery__gate">
          <div class="edhr-delivery__section-title">
            <span>门禁说明</span>
            <span class="edhr-delivery__muted">{{ gateSummary?.gateStatus || '未加载' }}</span>
          </div>
          <el-alert v-if="gateError" :title="gateError" type="error" :closable="false" show-icon />
          <el-descriptions v-if="gateSummary" :column="2" border class="edhr-delivery__summary">
            <el-descriptions-item label="证据包">{{ gateSummary.packageCount }}</el-descriptions-item>
            <el-descriptions-item label="门禁项">{{ gateSummary.gateCount }}</el-descriptions-item>
            <el-descriptions-item label="阻断项">{{ gateSummary.blockedCount }}</el-descriptions-item>
            <el-descriptions-item label="签核">
              <el-tag :type="gateSummary.signoffAllowed ? 'success' : 'danger'">
                {{ gateSummary.signoffAllowed ? '允许签核' : '不允许签核' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="说明" :span="2">
              {{ gateSummary.summary }}
            </el-descriptions-item>
          </el-descriptions>

          <el-table
            v-loading="gateLoading"
            :data="gateItems"
            stripe
            :show-overflow-tooltip="true"
            empty-text="请选择交付项目后查看门禁项"
          >
            <el-table-column label="门禁" min-width="170">
              <template #default="{ row }">
                <div class="edhr-delivery__strong">{{ row.gateName }}</div>
                <div class="edhr-delivery__muted">{{ row.gateCode }}</div>
              </template>
            </el-table-column>
            <el-table-column label="缺失证据" prop="missingEvidence" min-width="220" />
            <el-table-column label="责任人" prop="ownerName" width="120" />
            <el-table-column label="下一步动作" prop="nextAction" min-width="220" />
            <el-table-column label="签核影响" prop="signoffImpact" min-width="170" />
          </el-table>
        </div>
      </section>

      <el-dialog v-model="createDialogVisible" title="新建交付项目" width="760px">
        <el-alert v-if="createError" :title="createError" type="error" :closable="false" show-icon />
        <el-form
          ref="createFormRef"
          :model="createForm"
          :rules="createRules"
          label-width="110px"
          class="edhr-delivery__dialog-form"
        >
          <el-form-item label="项目名称" prop="projectName">
            <el-input v-model="createForm.projectName" maxlength="128" />
          </el-form-item>
          <el-form-item label="客户名称" prop="customerName">
            <el-input v-model="createForm.customerName" maxlength="128" />
          </el-form-item>
          <el-form-item label="客户现场" prop="siteName">
            <el-input v-model="createForm.siteName" maxlength="128" />
          </el-form-item>
          <el-form-item label="系统范围" prop="systemScope">
            <el-input v-model="createForm.systemScope" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
          <el-form-item label="验证范围" prop="validationScope">
            <el-input v-model="createForm.validationScope" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
          <el-form-item label="发布标签" prop="releaseTag">
            <el-input v-model="createForm.releaseTag" maxlength="64" />
          </el-form-item>
          <el-form-item label="schema版本" prop="schemaVersion">
            <el-input v-model="createForm.schemaVersion" maxlength="64" />
          </el-form-item>
          <el-form-item label="目标环境" prop="targetEnvironment">
            <el-select v-model="createForm.targetEnvironment" class="!w-220px">
              <el-option label="本地测试" value="local-test" />
              <el-option label="测试租户" value="test-tenant" />
              <el-option label="客户测试环境" value="customer-test" />
            </el-select>
          </el-form-item>
          <el-form-item label="负责人" prop="ownerName">
            <el-input v-model="createForm.ownerName" maxlength="128" />
          </el-form-item>
          <el-form-item label="负责部门">
            <el-input v-model="createForm.ownerDepartment" maxlength="128" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="createForm.remark" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleCreateProject">创建</el-button>
        </template>
      </el-dialog>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  createEdhrDeliveryProject,
  getEdhrDeliveryGateSummary,
  getEdhrDeliveryProjectPage,
  getEdhrEvidencePackagePage,
  type EdhrDeliveryGateItemRespVO,
  type EdhrDeliveryGateSummaryRespVO,
  type EdhrDeliveryProjectCreateReqVO,
  type EdhrDeliveryProjectPageReqVO,
  type EdhrDeliveryProjectRespVO,
  type EdhrEvidencePackageRespVO
} from '@/api/mes/pro/edhr/delivery'

defineOptions({ name: 'MesProEdhrDelivery' })

const projectLoading = ref(false)
const packageLoading = ref(false)
const gateLoading = ref(false)
const submitLoading = ref(false)
const loadError = ref('')
const packageError = ref('')
const gateError = ref('')
const createError = ref('')

const projectList = ref<EdhrDeliveryProjectRespVO[]>([])
const projectTotal = ref(0)
const selectedProject = ref<EdhrDeliveryProjectRespVO>()
const packageList = ref<EdhrEvidencePackageRespVO[]>([])
const gateSummary = ref<EdhrDeliveryGateSummaryRespVO>()

const createDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()

const projectQueryParams = reactive<EdhrDeliveryProjectPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  projectName: '',
  customerName: '',
  projectStatus: 'BLOCKED'
})

const createForm = reactive<EdhrDeliveryProjectCreateReqVO>({
  projectName: '',
  customerName: '',
  siteName: '',
  systemScope: 'eDHR批记录、证据包、签核门禁、接口联调和运维交接',
  validationScope: 'CSV验证、OQ/PQ、培训、部署授权、接口、恢复演练',
  releaseTag: '',
  schemaVersion: '',
  targetEnvironment: 'test-tenant',
  ownerName: '',
  ownerDepartment: '',
  remark: ''
})

const createRules: FormRules = {
  projectName: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
  customerName: [{ required: true, message: '客户名称不能为空', trigger: 'blur' }],
  siteName: [{ required: true, message: '客户现场不能为空', trigger: 'blur' }],
  systemScope: [{ required: true, message: '系统范围不能为空', trigger: 'blur' }],
  validationScope: [{ required: true, message: '验证范围不能为空', trigger: 'blur' }],
  releaseTag: [{ required: true, message: '发布标签不能为空', trigger: 'blur' }],
  schemaVersion: [{ required: true, message: 'schema版本不能为空', trigger: 'blur' }],
  targetEnvironment: [{ required: true, message: '目标环境不能为空', trigger: 'change' }],
  ownerName: [{ required: true, message: '负责人不能为空', trigger: 'blur' }]
}

const gateItems = computed<EdhrDeliveryGateItemRespVO[]>(() => gateSummary.value?.gateItems || [])

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

function assertPageResult<T>(data: unknown, label: string): PageResult<T[]> {
  const page = data as { list?: unknown; total?: unknown }
  if (!page || !Array.isArray(page.list) || typeof page.total !== 'number') {
    throw new Error(`${label}响应结构异常，缺少 list/total。`)
  }
  return page as PageResult<T[]>
}

const formatEvidence = (value?: string) => {
  if (!value) return '--'
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed.join('；')
  } catch (error) {
    return value
  }
  return value
}

const buildProjectQuery = (): EdhrDeliveryProjectPageReqVO => ({
  pageNo: projectQueryParams.pageNo,
  pageSize: projectQueryParams.pageSize,
  projectName: projectQueryParams.projectName?.trim() || undefined,
  customerName: projectQueryParams.customerName?.trim() || undefined,
  projectStatus: projectQueryParams.projectStatus || undefined
})

const getProjectList = async () => {
  projectLoading.value = true
  loadError.value = ''
  packageError.value = ''
  gateError.value = ''
  try {
    const page = assertPageResult<EdhrDeliveryProjectRespVO>(
      await getEdhrDeliveryProjectPage(buildProjectQuery()),
      '交付项目'
    )
    projectList.value = page.list
    projectTotal.value = page.total
    if (!selectedProject.value && page.list.length > 0) {
      await handleSelectProject(page.list[0])
    }
  } catch (error) {
    projectList.value = []
    projectTotal.value = 0
    selectedProject.value = undefined
    packageList.value = []
    gateSummary.value = undefined
    loadError.value = resolveErrorMessage(error, '交付项目加载失败，请检查接口和权限。')
  } finally {
    projectLoading.value = false
  }
}

const getPackageList = async () => {
  if (!selectedProject.value) {
    packageList.value = []
    packageError.value = ''
    return
  }
  packageLoading.value = true
  packageError.value = ''
  try {
    const page = assertPageResult<EdhrEvidencePackageRespVO>(
      await getEdhrEvidencePackagePage({
        pageNo: 1,
        pageSize: 20,
        projectId: selectedProject.value.id
      }),
      '交付证据包'
    )
    packageList.value = page.list
  } catch (error) {
    packageList.value = []
    packageError.value = resolveErrorMessage(error, '证据包加载失败，请检查项目和权限。')
  } finally {
    packageLoading.value = false
  }
}

const getGateSummary = async () => {
  if (!selectedProject.value) {
    gateSummary.value = undefined
    gateError.value = ''
    return
  }
  gateLoading.value = true
  gateError.value = ''
  try {
    gateSummary.value = await getEdhrDeliveryGateSummary(selectedProject.value.id)
  } catch (error) {
    gateSummary.value = undefined
    gateError.value = resolveErrorMessage(error, '门禁说明加载失败，请检查交付证据对象。')
  } finally {
    gateLoading.value = false
  }
}

const handleSelectProject = async (row: EdhrDeliveryProjectRespVO) => {
  selectedProject.value = row
  await getPackageList()
  await getGateSummary()
}

const handleProjectQuery = () => {
  projectQueryParams.pageNo = 1
  selectedProject.value = undefined
  packageList.value = []
  gateSummary.value = undefined
  packageError.value = ''
  gateError.value = ''
  getProjectList()
}

const resetProjectQuery = () => {
  projectQueryParams.pageNo = 1
  projectQueryParams.pageSize = 10
  projectQueryParams.projectName = ''
  projectQueryParams.customerName = ''
  projectQueryParams.projectStatus = 'BLOCKED'
  handleProjectQuery()
}

const resetCreateForm = () => {
  createForm.projectName = ''
  createForm.customerName = ''
  createForm.siteName = ''
  createForm.systemScope = 'eDHR批记录、证据包、签核门禁、接口联调和运维交接'
  createForm.validationScope = 'CSV验证、OQ/PQ、培训、部署授权、接口、恢复演练'
  createForm.releaseTag = ''
  createForm.schemaVersion = ''
  createForm.targetEnvironment = 'test-tenant'
  createForm.ownerName = ''
  createForm.ownerDepartment = ''
  createForm.remark = ''
  createFormRef.value?.clearValidate()
}

const openCreateDialog = () => {
  resetCreateForm()
  createError.value = ''
  createDialogVisible.value = true
}

const handleCreateProject = async () => {
  createError.value = ''
  try {
    await createFormRef.value?.validate()
  } catch (error) {
    return
  }
  submitLoading.value = true
  try {
    const project = await createEdhrDeliveryProject({ ...createForm })
    ElMessage.success('交付项目已创建，证据包与门禁项已初始化')
    createDialogVisible.value = false
    selectedProject.value = project
    await getProjectList()
    await handleSelectProject(project)
  } catch (error) {
    createError.value = resolveErrorMessage(error, '交付项目创建失败，请检查必填项、接口和权限。')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  getProjectList()
})
</script>

<style scoped>
.edhr-delivery {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-delivery__toolbar,
.edhr-delivery__project,
.edhr-delivery__packages,
.edhr-delivery__gate {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-delivery__toolbar {
  padding: 16px 16px 0;
}

.edhr-delivery__project,
.edhr-delivery__packages,
.edhr-delivery__gate {
  padding: 16px;
}

.edhr-delivery__title-row,
.edhr-delivery__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.edhr-delivery__title-row {
  margin-bottom: 12px;
}

.edhr-delivery__title-row h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.3;
}

.edhr-delivery__subtitle,
.edhr-delivery__muted {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-delivery__form {
  display: flex;
  flex-wrap: wrap;
}

.edhr-delivery__section-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.edhr-delivery__detail-grid {
  display: grid;
  grid-template-columns: minmax(460px, 1fr) minmax(520px, 1.05fr);
  gap: 16px;
}

.edhr-delivery__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-delivery__blocked {
  margin-top: -4px;
}

.edhr-delivery__summary {
  margin-bottom: 12px;
}

.edhr-delivery :deep(.el-table__header th) {
  height: 44px;
  background: #f7f9fc;
}

.edhr-delivery :deep(.el-table__row) {
  height: 52px;
}

.edhr-delivery__dialog-form {
  max-height: 64vh;
  overflow-y: auto;
  padding-right: 12px;
}

@media (max-width: 1280px) {
  .edhr-delivery__detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .edhr-delivery__title-row,
  .edhr-delivery__section-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
