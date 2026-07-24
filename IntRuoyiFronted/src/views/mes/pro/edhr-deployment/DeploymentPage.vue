<template>
  <ContentWrap>
    <div class="edhr-deployment">
      <section class="edhr-deployment__toolbar">
        <div class="edhr-deployment__title-row">
          <div>
            <h2>部署交付</h2>
            <div class="edhr-deployment__subtitle">
              环境检查 / 安装包版本 / releaseTag / schema版本 / 迁移清单 / required SQL / 授权许可 / 接口确认
            </div>
          </div>
          <el-tag :type="gatePassed ? 'success' : 'danger'" effect="plain">
            {{ gatePassed ? 'INTEGRATED' : '阻断' }}
          </el-tag>
        </div>

        <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

        <el-form :inline="true" :model="projectQueryParams" class="edhr-deployment__form" @submit.prevent>
          <el-form-item label="项目名称">
            <el-input
              v-model="projectQueryParams.projectName"
              clearable
              placeholder="交付项目"
              class="!w-180px"
              @keyup.enter="handleProjectQuery"
            />
          </el-form-item>
          <el-form-item label="客户">
            <el-input
              v-model="projectQueryParams.customerName"
              clearable
              placeholder="客户名称"
              class="!w-150px"
              @keyup.enter="handleProjectQuery"
            />
          </el-form-item>
          <el-form-item label="部署状态">
            <el-select v-model="deploymentQueryParams.deploymentStatus" clearable class="!w-160px" @change="loadDeploymentList">
              <el-option label="草稿" value="DELIVERY_DRAFT" />
              <el-option label="环境已检查" value="ENVIRONMENT_CHECKED" />
              <el-option label="已安装" value="INSTALLED" />
              <el-option label="阻断" value="DELIVERY_BLOCKED" />
              <el-option label="INTEGRATED" value="INTEGRATED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleProjectQuery" v-hasPermi="['mes:pro-edhr-deployment:query']">
              <Icon icon="ep:search" class="mr-5px" />
              查询
            </el-button>
            <el-button @click="resetProjectQuery">
              <Icon icon="ep:refresh" class="mr-5px" />
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="edhr-deployment__project">
        <div class="edhr-deployment__section-title">
          <span>交付项目</span>
          <span class="edhr-deployment__muted">{{ selectedProject?.projectCode || '未选择项目' }}</span>
        </div>
        <el-table
          v-loading="projectLoading"
          :data="projectList"
          stripe
          row-key="id"
          height="230"
          highlight-current-row
          :show-overflow-tooltip="true"
          empty-text="暂无交付项目"
          @row-click="handleSelectProject"
        >
          <el-table-column label="项目" min-width="250">
            <template #default="{ row }">
              <div class="edhr-deployment__strong">{{ row.projectName }}</div>
              <div class="edhr-deployment__muted">{{ row.projectCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="客户" prop="customerName" min-width="140" />
          <el-table-column label="目标环境" prop="targetEnvironment" min-width="150" />
          <el-table-column label="releaseTag" prop="releaseTag" min-width="150" />
          <el-table-column label="schema版本" prop="schemaVersion" min-width="150" />
          <el-table-column label="负责人" prop="ownerName" min-width="120" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.projectStatus === 'BLOCKED' ? 'danger' : 'success'">
                {{ row.projectStatus || '--' }}
              </el-tag>
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

      <section class="edhr-deployment__records">
        <div class="edhr-deployment__section-title">
          <span>部署记录</span>
          <div class="edhr-deployment__actions">
            <el-button
              type="success"
              size="small"
              :disabled="!selectedProject"
              @click="openCreateDialog"
              v-hasPermi="['mes:pro-edhr-deployment:create']"
            >
              <Icon icon="ep:plus" class="mr-4px" />
              创建部署记录
            </el-button>
            <el-button
              size="small"
              :disabled="!selectedEvidence"
              @click="openUpdateDialog"
              v-hasPermi="['mes:pro-edhr-deployment:update']"
            >
              补齐证据
            </el-button>
            <el-button
              type="primary"
              size="small"
              :disabled="!selectedEvidence"
              @click="handlePrecheckEvidence"
              v-hasPermi="['mes:pro-edhr-deployment:precheck']"
            >
              门禁预检
            </el-button>
          </div>
        </div>

        <el-table
          v-loading="deploymentLoading"
          :data="deploymentList"
          stripe
          row-key="id"
          height="300"
          highlight-current-row
          :show-overflow-tooltip="true"
          empty-text="暂无部署交付记录"
          @row-click="handleSelectEvidence"
        >
          <el-table-column label="部署证据" min-width="240">
            <template #default="{ row }">
              <div class="edhr-deployment__strong">{{ row.deploymentName }}</div>
              <div class="edhr-deployment__muted">{{ row.deploymentCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="环境授权" width="110">
            <template #default="{ row }">
              <el-tag :type="row.environmentAuthorized ? 'success' : 'danger'" effect="plain">
                {{ row.environmentAuthorized ? '已授权' : '缺失' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="版本" min-width="190">
            <template #default="{ row }">
              <div>{{ row.releaseTag }}</div>
              <div class="edhr-deployment__muted">{{ row.schemaVersion }}</div>
            </template>
          </el-table-column>
          <el-table-column label="安装包版本" prop="artifactVersion" min-width="150" />
          <el-table-column label="授权许可" min-width="180">
            <template #default="{ row }">
              {{ row.licenseScope || '缺少授权范围' }}
            </template>
          </el-table-column>
          <el-table-column label="接口范围" min-width="180">
            <template #default="{ row }">
              {{ row.interfaceScope || '缺少接口范围' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="deploymentStatusTagType(row.deploymentStatus)">
                {{ row.deploymentStatus || '--' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下一步动作" prop="nextAction" min-width="240" />
        </el-table>
      </section>

      <section class="edhr-deployment__detail-grid">
        <div class="edhr-deployment__panel">
          <div class="edhr-deployment__section-title">
            <span>环境检查与版本证据</span>
            <span class="edhr-deployment__muted">{{ selectedEvidence?.targetEnvironment || '未选择部署记录' }}</span>
          </div>
          <el-descriptions :column="2" border class="edhr-deployment__summary">
            <el-descriptions-item label="环境授权">
              {{ selectedEvidence?.environmentAuthorized ? '已授权' : '缺失' }}
            </el-descriptions-item>
            <el-descriptions-item label="环境检查">{{ selectedEvidence?.environmentCheckSummary || '--' }}</el-descriptions-item>
            <el-descriptions-item label="服务器">{{ selectedEvidence?.serverSummary || '--' }}</el-descriptions-item>
            <el-descriptions-item label="网络">{{ selectedEvidence?.networkSummary || '--' }}</el-descriptions-item>
            <el-descriptions-item label="对象存储">{{ selectedEvidence?.objectStorageSummary || '--' }}</el-descriptions-item>
            <el-descriptions-item label="容量">{{ selectedEvidence?.capacitySummary || '--' }}</el-descriptions-item>
            <el-descriptions-item label="权限">{{ selectedEvidence?.permissionSummary || '--' }}</el-descriptions-item>
            <el-descriptions-item label="安装包版本">{{ selectedEvidence?.artifactVersion || '--' }}</el-descriptions-item>
            <el-descriptions-item label="releaseTag">{{ selectedEvidence?.releaseTag || '--' }}</el-descriptions-item>
            <el-descriptions-item label="schema版本">{{ selectedEvidence?.schemaVersion || '--' }}</el-descriptions-item>
            <el-descriptions-item label="迁移清单">{{ selectedEvidence?.migrationManifest || '--' }}</el-descriptions-item>
            <el-descriptions-item label="required SQL">{{ selectedEvidence?.requiredSqlManifest || '--' }}</el-descriptions-item>
            <el-descriptions-item label="应用导入" :span="2">{{ selectedEvidence?.appImportResult || '--' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="edhr-deployment__panel">
          <div class="edhr-deployment__section-title">
            <span>授权许可与接口证据</span>
            <el-tag :type="gatePassed ? 'success' : 'danger'" effect="plain">
              {{ gatePassed ? 'INTEGRATED' : '阻断' }}
            </el-tag>
          </div>
          <el-descriptions :column="2" border class="edhr-deployment__summary">
            <el-descriptions-item label="授权范围">{{ selectedEvidence?.licenseScope || '--' }}</el-descriptions-item>
            <el-descriptions-item label="有效期">{{ selectedEvidence?.licenseValidUntil || '--' }}</el-descriptions-item>
            <el-descriptions-item label="授权文件">{{ selectedEvidence?.licenseFileEvidence || '--' }}</el-descriptions-item>
            <el-descriptions-item label="授权校验">{{ selectedEvidence?.licenseCheckResult || '--' }}</el-descriptions-item>
            <el-descriptions-item label="接口范围">{{ selectedEvidence?.interfaceScope || '--' }}</el-descriptions-item>
            <el-descriptions-item label="接口版本">{{ selectedEvidence?.interfaceVersion || '--' }}</el-descriptions-item>
            <el-descriptions-item label="联调环境">{{ selectedEvidence?.integrationEnvironment || '--' }}</el-descriptions-item>
            <el-descriptions-item label="失败项">{{ selectedEvidence?.interfaceFailureCount ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="真实请求">{{ selectedEvidence?.requestEvidence || '--' }}</el-descriptions-item>
            <el-descriptions-item label="真实响应">{{ responseEvidence || '--' }}</el-descriptions-item>
            <el-descriptions-item label="失败整改">{{ selectedEvidence?.remediationAction || '--' }}</el-descriptions-item>
            <el-descriptions-item label="复测证据">{{ selectedEvidence?.retestEvidence || '--' }}</el-descriptions-item>
            <el-descriptions-item label="缺失证据" :span="2">{{ blockedReason || '--' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </section>

      <section class="edhr-deployment__gate">
        <div class="edhr-deployment__section-title">
          <span>门禁项</span>
          <span class="edhr-deployment__muted">{{ selectedEvidence?.evidenceSnapshotChecksum || '无证据快照' }}</span>
        </div>
        <el-table
          v-loading="gateLoading"
          :data="gateItemList"
          stripe
          row-key="id"
          height="300"
          :show-overflow-tooltip="true"
          empty-text="请选择部署记录后查看门禁项"
        >
          <el-table-column label="门禁" min-width="220">
            <template #default="{ row }">
              <div class="edhr-deployment__strong">{{ row.gateName }}</div>
              <div class="edhr-deployment__muted">{{ row.gateCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.gateStatus === 'PASSED' ? 'success' : 'danger'">{{ row.gateStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="证据来源" prop="evidenceSource" min-width="220" />
          <el-table-column label="缺失证据" prop="missingEvidence" min-width="260" />
          <el-table-column label="责任人" prop="ownerName" width="120" />
          <el-table-column label="下一步动作" prop="nextAction" min-width="240" />
          <el-table-column label="签核影响" prop="signoffImpact" min-width="180" />
        </el-table>
      </section>
    </div>

    <el-dialog v-model="createDialogVisible" title="创建部署记录" width="820px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="125px" class="edhr-deployment__dialog-form">
        <el-form-item label="部署名称" prop="deploymentName">
          <el-input v-model="createForm.deploymentName" maxlength="128" />
        </el-form-item>
        <el-form-item label="目标环境" prop="targetEnvironment">
          <el-input v-model="createForm.targetEnvironment" maxlength="128" />
        </el-form-item>
        <el-form-item label="环境授权">
          <el-switch v-model="createForm.environmentAuthorized" active-text="已授权" inactive-text="未授权" />
        </el-form-item>
        <el-form-item label="环境检查">
          <el-input v-model="createForm.environmentCheckSummary" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="服务器">
          <el-input v-model="createForm.serverSummary" maxlength="500" />
        </el-form-item>
        <el-form-item label="网络">
          <el-input v-model="createForm.networkSummary" maxlength="500" />
        </el-form-item>
        <el-form-item label="对象存储">
          <el-input v-model="createForm.objectStorageSummary" maxlength="500" />
        </el-form-item>
        <el-form-item label="容量">
          <el-input v-model="createForm.capacitySummary" maxlength="500" />
        </el-form-item>
        <el-form-item label="权限">
          <el-input v-model="createForm.permissionSummary" maxlength="500" />
        </el-form-item>
        <el-form-item label="releaseTag" prop="releaseTag">
          <el-input v-model="createForm.releaseTag" maxlength="64" />
        </el-form-item>
        <el-form-item label="安装包版本">
          <el-input v-model="createForm.artifactVersion" maxlength="128" />
        </el-form-item>
        <el-form-item label="制品checksum">
          <el-input v-model="createForm.artifactChecksum" maxlength="128" />
        </el-form-item>
        <el-form-item label="schema版本" prop="schemaVersion">
          <el-input v-model="createForm.schemaVersion" maxlength="64" />
        </el-form-item>
        <el-form-item label="迁移清单">
          <el-input v-model="createForm.migrationManifest" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="required SQL">
          <el-input v-model="createForm.requiredSqlManifest" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="应用导入">
          <el-input v-model="createForm.appImportResult" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreateEvidence">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="updateDialogVisible" title="补齐部署授权接口证据" width="820px">
      <el-form ref="updateFormRef" :model="updateForm" :rules="updateRules" label-width="125px" class="edhr-deployment__dialog-form">
        <el-form-item label="目标环境">
          <el-input v-model="updateForm.targetEnvironment" maxlength="128" />
        </el-form-item>
        <el-form-item label="环境授权">
          <el-switch v-model="updateForm.environmentAuthorized" active-text="已授权" inactive-text="未授权" />
        </el-form-item>
        <el-form-item label="环境检查">
          <el-input v-model="updateForm.environmentCheckSummary" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="服务器">
          <el-input v-model="updateForm.serverSummary" maxlength="500" />
        </el-form-item>
        <el-form-item label="网络">
          <el-input v-model="updateForm.networkSummary" maxlength="500" />
        </el-form-item>
        <el-form-item label="对象存储">
          <el-input v-model="updateForm.objectStorageSummary" maxlength="500" />
        </el-form-item>
        <el-form-item label="容量">
          <el-input v-model="updateForm.capacitySummary" maxlength="500" />
        </el-form-item>
        <el-form-item label="权限">
          <el-input v-model="updateForm.permissionSummary" maxlength="500" />
        </el-form-item>
        <el-form-item label="releaseTag">
          <el-input v-model="updateForm.releaseTag" maxlength="64" />
        </el-form-item>
        <el-form-item label="安装包版本">
          <el-input v-model="updateForm.artifactVersion" maxlength="128" />
        </el-form-item>
        <el-form-item label="制品checksum">
          <el-input v-model="updateForm.artifactChecksum" maxlength="128" />
        </el-form-item>
        <el-form-item label="schema版本">
          <el-input v-model="updateForm.schemaVersion" maxlength="64" />
        </el-form-item>
        <el-form-item label="迁移清单">
          <el-input v-model="updateForm.migrationManifest" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="required SQL">
          <el-input v-model="updateForm.requiredSqlManifest" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="应用导入">
          <el-input v-model="updateForm.appImportResult" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="授权范围" prop="licenseScope">
          <el-input v-model="updateForm.licenseScope" maxlength="500" />
        </el-form-item>
        <el-form-item label="有效期" prop="licenseValidUntil">
          <el-date-picker v-model="updateForm.licenseValidUntil" type="date" value-format="YYYY-MM-DD" class="!w-220px" />
        </el-form-item>
        <el-form-item label="授权文件" prop="licenseFileEvidence">
          <el-input v-model="updateForm.licenseFileEvidence" maxlength="500" />
        </el-form-item>
        <el-form-item label="授权校验" prop="licenseCheckResult">
          <el-input v-model="updateForm.licenseCheckResult" maxlength="500" />
        </el-form-item>
        <el-form-item label="客户授权确认">
          <el-input v-model="updateForm.customerLicenseConfirmation" maxlength="500" />
        </el-form-item>
        <el-form-item label="接口范围" prop="interfaceScope">
          <el-input v-model="updateForm.interfaceScope" maxlength="500" />
        </el-form-item>
        <el-form-item label="接口版本" prop="interfaceVersion">
          <el-input v-model="updateForm.interfaceVersion" maxlength="128" />
        </el-form-item>
        <el-form-item label="联调环境" prop="integrationEnvironment">
          <el-input v-model="updateForm.integrationEnvironment" maxlength="500" />
        </el-form-item>
        <el-form-item label="真实请求" prop="requestEvidence">
          <el-input v-model="updateForm.requestEvidence" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="真实响应" prop="responseEvidence">
          <el-input v-model="updateForm.responseEvidence" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="失败项数量">
          <el-input-number v-model="updateForm.interfaceFailureCount" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="失败整改">
          <el-input v-model="updateForm.remediationAction" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="复测证据" prop="retestEvidence">
          <el-input v-model="updateForm.retestEvidence" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="接口确认人">
          <el-input v-model="updateForm.interfaceConfirmedBy" maxlength="128" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="updateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleUpdateEvidence">保存证据</el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import {
  getEdhrDeliveryProjectPage,
  type EdhrDeliveryProjectPageReqVO,
  type EdhrDeliveryProjectRespVO
} from '@/api/mes/pro/edhr/delivery'
import {
  createEdhrDeploymentEvidence,
  getEdhrDeploymentDetail,
  getEdhrDeploymentPage,
  precheckEdhrDeploymentEvidence,
  updateEdhrDeploymentEvidence,
  type EdhrDeploymentCreateReqVO,
  type EdhrDeploymentGateItemRespVO,
  type EdhrDeploymentPageReqVO,
  type EdhrDeploymentRespVO,
  type EdhrDeploymentUpdateReqVO
} from '@/api/mes/pro/edhr/deployment'

defineOptions({ name: 'MesProEdhrDeployment' })

const projectLoading = ref(false)
const deploymentLoading = ref(false)
const gateLoading = ref(false)
const submitLoading = ref(false)
const loadError = ref('')

const projectList = ref<EdhrDeliveryProjectRespVO[]>([])
const projectTotal = ref(0)
const selectedProject = ref<EdhrDeliveryProjectRespVO>()
const deploymentList = ref<EdhrDeploymentRespVO[]>([])
const selectedEvidence = ref<EdhrDeploymentRespVO>()

const createDialogVisible = ref(false)
const updateDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const updateFormRef = ref<FormInstance>()

const projectQueryParams = reactive<EdhrDeliveryProjectPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  projectName: '',
  customerName: '',
  projectStatus: ''
})

const deploymentQueryParams = reactive<EdhrDeploymentPageReqVO>({
  pageNo: 1,
  pageSize: 100,
  projectId: undefined,
  deploymentStatus: ''
})

const createForm = reactive<EdhrDeploymentCreateReqVO>({
  projectId: 0,
  deploymentName: '',
  customerProjectName: '',
  targetEnvironment: '',
  environmentAuthorized: false,
  environmentCheckSummary: '',
  serverSummary: '',
  networkSummary: '',
  objectStorageSummary: '',
  capacitySummary: '',
  permissionSummary: '',
  releaseTag: '',
  artifactVersion: '',
  artifactChecksum: '',
  schemaVersion: '',
  migrationManifest: '',
  requiredSqlManifest: '',
  appImportResult: '',
  remark: ''
})

const updateForm = reactive<EdhrDeploymentUpdateReqVO>({
  deploymentId: 0,
  targetEnvironment: '',
  environmentAuthorized: false,
  environmentCheckSummary: '',
  serverSummary: '',
  networkSummary: '',
  objectStorageSummary: '',
  capacitySummary: '',
  permissionSummary: '',
  releaseTag: '',
  artifactVersion: '',
  artifactChecksum: '',
  schemaVersion: '',
  migrationManifest: '',
  requiredSqlManifest: '',
  appImportResult: '',
  licenseScope: '',
  licenseValidUntil: '',
  licenseFileEvidence: '',
  licenseCheckResult: '',
  customerLicenseConfirmation: '',
  interfaceScope: '',
  interfaceVersion: '',
  integrationEnvironment: '',
  requestEvidence: '',
  responseEvidence: '',
  interfaceFailureCount: 0,
  remediationAction: '',
  retestEvidence: '',
  interfaceConfirmedBy: ''
})

const createRules: FormRules = {
  deploymentName: [{ required: true, message: '部署名称不能为空', trigger: 'blur' }],
  targetEnvironment: [{ required: true, message: '目标环境不能为空', trigger: 'blur' }],
  releaseTag: [{ required: true, message: 'releaseTag不能为空', trigger: 'blur' }],
  schemaVersion: [{ required: true, message: 'schema版本不能为空', trigger: 'blur' }]
}

const updateRules: FormRules = {
  licenseScope: [{ required: true, message: '授权范围不能为空', trigger: 'blur' }],
  licenseValidUntil: [{ required: true, message: '有效期不能为空', trigger: 'change' }],
  licenseFileEvidence: [{ required: true, message: '授权文件不能为空', trigger: 'blur' }],
  licenseCheckResult: [{ required: true, message: '授权校验不能为空', trigger: 'blur' }],
  interfaceScope: [{ required: true, message: '接口范围不能为空', trigger: 'blur' }],
  interfaceVersion: [{ required: true, message: '接口版本不能为空', trigger: 'blur' }],
  integrationEnvironment: [{ required: true, message: '联调环境不能为空', trigger: 'blur' }],
  requestEvidence: [{ required: true, message: '真实请求不能为空', trigger: 'blur' }],
  responseEvidence: [{ required: true, message: '真实响应不能为空', trigger: 'blur' }],
  retestEvidence: [{ required: true, message: '复测证据不能为空', trigger: 'blur' }]
}

const gateItemList = computed<EdhrDeploymentGateItemRespVO[]>(() => selectedEvidence.value?.gateItems || [])
const gatePassed = computed(() => Boolean(selectedEvidence.value?.gatePassed))
const blockedReason = computed(() => selectedEvidence.value?.blockedReason || '')
const responseEvidence = computed(() => selectedEvidence.value?.responseEvidence || '')

const resolveErrorMessage = (error: unknown) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return '操作失败，请检查接口、权限和部署证据。'
}

function assertPageResult<T>(data: unknown, label: string): PageResult<T[]> {
  const page = data as { list?: unknown; total?: unknown }
  if (!page || !Array.isArray(page.list) || typeof page.total !== 'number') {
    throw new Error(`${label}响应结构异常，缺少 list/total。`)
  }
  return page as PageResult<T[]>
}

const deploymentStatusTagType = (status: string) => {
  if (status === 'INTEGRATED') return 'success'
  if (status === 'DELIVERY_BLOCKED') return 'danger'
  if (status === 'INSTALLED' || status === 'ENVIRONMENT_CHECKED') return 'warning'
  return 'primary'
}

const getProjectList = async () => {
  projectLoading.value = true
  loadError.value = ''
  try {
    const page = assertPageResult<EdhrDeliveryProjectRespVO>(
      await getEdhrDeliveryProjectPage({
        pageNo: projectQueryParams.pageNo,
        pageSize: projectQueryParams.pageSize,
        projectName: projectQueryParams.projectName?.trim() || undefined,
        customerName: projectQueryParams.customerName?.trim() || undefined,
        projectStatus: projectQueryParams.projectStatus || undefined
      }),
      '交付项目'
    )
    projectList.value = page.list
    projectTotal.value = page.total
  } catch (error) {
    projectList.value = []
    projectTotal.value = 0
    selectedProject.value = undefined
    selectedEvidence.value = undefined
    deploymentList.value = []
    loadError.value = resolveErrorMessage(error)
  } finally {
    projectLoading.value = false
  }
}

const loadDeploymentList = async () => {
  if (!selectedProject.value) {
    deploymentList.value = []
    selectedEvidence.value = undefined
    return
  }
  deploymentLoading.value = true
  loadError.value = ''
  try {
    const page = assertPageResult<EdhrDeploymentRespVO>(
      await getEdhrDeploymentPage({
        pageNo: deploymentQueryParams.pageNo,
        pageSize: deploymentQueryParams.pageSize,
        projectId: selectedProject.value.id,
        deploymentStatus: deploymentQueryParams.deploymentStatus || undefined
      }),
      '部署记录'
    )
    deploymentList.value = page.list
    if (selectedEvidence.value) {
      selectedEvidence.value = page.list.find((item) => item.id === selectedEvidence.value?.id)
    }
  } catch (error) {
    deploymentList.value = []
    selectedEvidence.value = undefined
    loadError.value = resolveErrorMessage(error)
  } finally {
    deploymentLoading.value = false
  }
}

const syncEvidenceRow = (latest: EdhrDeploymentRespVO) => {
  const index = deploymentList.value.findIndex((item) => item.id === latest.id)
  if (index >= 0) deploymentList.value.splice(index, 1, latest)
  if (index < 0) deploymentList.value.unshift(latest)
  selectedEvidence.value = latest
}

const handleProjectQuery = () => {
  projectQueryParams.pageNo = 1
  selectedProject.value = undefined
  selectedEvidence.value = undefined
  deploymentList.value = []
  getProjectList()
}

const resetProjectQuery = () => {
  projectQueryParams.pageNo = 1
  projectQueryParams.pageSize = 10
  projectQueryParams.projectName = ''
  projectQueryParams.customerName = ''
  projectQueryParams.projectStatus = ''
  deploymentQueryParams.deploymentStatus = ''
  handleProjectQuery()
}

const handleSelectProject = async (row: EdhrDeliveryProjectRespVO) => {
  selectedProject.value = row
  selectedEvidence.value = undefined
  deploymentQueryParams.projectId = row.id
  await loadDeploymentList()
}

const handleSelectEvidence = async (row: EdhrDeploymentRespVO) => {
  gateLoading.value = true
  loadError.value = ''
  try {
    selectedEvidence.value = await getEdhrDeploymentDetail(row.id)
  } catch (error) {
    selectedEvidence.value = row
    loadError.value = resolveErrorMessage(error)
  } finally {
    gateLoading.value = false
  }
}

const openCreateDialog = () => {
  if (!selectedProject.value) return
  createForm.projectId = selectedProject.value.id
  createForm.deploymentName = `${selectedProject.value.projectName}-部署证据`
  createForm.customerProjectName = selectedProject.value.projectName
  createForm.targetEnvironment = selectedProject.value.targetEnvironment
  createForm.environmentAuthorized = false
  createForm.environmentCheckSummary = ''
  createForm.serverSummary = ''
  createForm.networkSummary = ''
  createForm.objectStorageSummary = ''
  createForm.capacitySummary = ''
  createForm.permissionSummary = ''
  createForm.releaseTag = selectedProject.value.releaseTag
  createForm.artifactVersion = ''
  createForm.artifactChecksum = ''
  createForm.schemaVersion = selectedProject.value.schemaVersion
  createForm.migrationManifest = ''
  createForm.requiredSqlManifest = ''
  createForm.appImportResult = ''
  createForm.remark = ''
  createFormRef.value?.clearValidate()
  createDialogVisible.value = true
}

const handleCreateEvidence = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const created = await createEdhrDeploymentEvidence(createForm)
    syncEvidenceRow(created)
    createDialogVisible.value = false
    ElMessage.success('部署证据已创建，缺失证据将保持阻断。')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submitLoading.value = false
  }
}

const openUpdateDialog = () => {
  if (!selectedEvidence.value) return
  updateForm.deploymentId = selectedEvidence.value.id
  updateForm.targetEnvironment = selectedEvidence.value.targetEnvironment || ''
  updateForm.environmentAuthorized = selectedEvidence.value.environmentAuthorized || false
  updateForm.environmentCheckSummary = selectedEvidence.value.environmentCheckSummary || ''
  updateForm.serverSummary = selectedEvidence.value.serverSummary || ''
  updateForm.networkSummary = selectedEvidence.value.networkSummary || ''
  updateForm.objectStorageSummary = selectedEvidence.value.objectStorageSummary || ''
  updateForm.capacitySummary = selectedEvidence.value.capacitySummary || ''
  updateForm.permissionSummary = selectedEvidence.value.permissionSummary || ''
  updateForm.releaseTag = selectedEvidence.value.releaseTag || ''
  updateForm.artifactVersion = selectedEvidence.value.artifactVersion || ''
  updateForm.artifactChecksum = selectedEvidence.value.artifactChecksum || ''
  updateForm.schemaVersion = selectedEvidence.value.schemaVersion || ''
  updateForm.migrationManifest = selectedEvidence.value.migrationManifest || ''
  updateForm.requiredSqlManifest = selectedEvidence.value.requiredSqlManifest || ''
  updateForm.appImportResult = selectedEvidence.value.appImportResult || ''
  updateForm.licenseScope = selectedEvidence.value.licenseScope || ''
  updateForm.licenseValidUntil = selectedEvidence.value.licenseValidUntil || ''
  updateForm.licenseFileEvidence = selectedEvidence.value.licenseFileEvidence || ''
  updateForm.licenseCheckResult = selectedEvidence.value.licenseCheckResult || ''
  updateForm.customerLicenseConfirmation = selectedEvidence.value.customerLicenseConfirmation || ''
  updateForm.interfaceScope = selectedEvidence.value.interfaceScope || ''
  updateForm.interfaceVersion = selectedEvidence.value.interfaceVersion || ''
  updateForm.integrationEnvironment = selectedEvidence.value.integrationEnvironment || ''
  updateForm.requestEvidence = selectedEvidence.value.requestEvidence || ''
  updateForm.responseEvidence = selectedEvidence.value.responseEvidence || ''
  updateForm.interfaceFailureCount = selectedEvidence.value.interfaceFailureCount || 0
  updateForm.remediationAction = selectedEvidence.value.remediationAction || ''
  updateForm.retestEvidence = selectedEvidence.value.retestEvidence || ''
  updateForm.interfaceConfirmedBy = selectedEvidence.value.interfaceConfirmedBy || ''
  updateFormRef.value?.clearValidate()
  updateDialogVisible.value = true
}

const handleUpdateEvidence = async () => {
  const valid = await updateFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const latest = await updateEdhrDeploymentEvidence(updateForm)
    syncEvidenceRow(latest)
    updateDialogVisible.value = false
    ElMessage.success('授权许可与接口证据已更新。')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submitLoading.value = false
  }
}

const handlePrecheckEvidence = async () => {
  if (!selectedEvidence.value) return
  gateLoading.value = true
  submitLoading.value = true
  try {
    await precheckEdhrDeploymentEvidence(selectedEvidence.value.id)
    const latest = await getEdhrDeploymentDetail(selectedEvidence.value.id)
    syncEvidenceRow(latest)
    ElMessage.success(latest.gatePassed ? '门禁预检通过，状态已进入 INTEGRATED。' : '门禁预检完成，仍存在阻断证据。')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submitLoading.value = false
    gateLoading.value = false
  }
}

onMounted(() => {
  getProjectList()
})
</script>

<style scoped>
.edhr-deployment {
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: #172033;
}

.edhr-deployment__toolbar,
.edhr-deployment__project,
.edhr-deployment__records,
.edhr-deployment__panel,
.edhr-deployment__gate {
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  padding: 14px;
}

.edhr-deployment__toolbar {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.edhr-deployment__project {
  border-top: 0;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}

.edhr-deployment__title-row,
.edhr-deployment__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.edhr-deployment__title-row h2 {
  margin: 0 0 4px;
  font-size: 20px;
  line-height: 1.3;
}

.edhr-deployment__subtitle,
.edhr-deployment__muted {
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-deployment__form {
  margin-top: 12px;
}

.edhr-deployment__strong {
  font-weight: 600;
  color: #111827;
}

.edhr-deployment__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.edhr-deployment__detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.edhr-deployment__summary :deep(.el-descriptions__label) {
  width: 110px;
  color: #334155;
}

.edhr-deployment__dialog-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 0 12px;
}

.edhr-deployment__dialog-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.edhr-deployment__dialog-form :deep(.el-textarea),
.edhr-deployment__dialog-form :deep(.el-input) {
  width: 100%;
}

@media (max-width: 1200px) {
  .edhr-deployment__detail-grid,
  .edhr-deployment__dialog-form {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .edhr-deployment__title-row,
  .edhr-deployment__section-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .edhr-deployment__actions {
    justify-content: flex-start;
  }
}
</style>
