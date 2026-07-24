<template>
  <ContentWrap>
    <div class="edhr-oq-pq">
      <section class="edhr-oq-pq__toolbar">
        <div class="edhr-oq-pq__title-row">
          <div>
            <h2>OQ/PQ执行台</h2>
            <div class="edhr-oq-pq__subtitle">
              绑定 OQ Ready 验证包，记录执行环境、真实业务路径、步骤结果、偏差、整改措施、复测结果、复核人和关闭签核。
            </div>
          </div>
          <el-tag type="warning">开放偏差阻断通过</el-tag>
        </div>

        <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" />

        <el-form :inline="true" :model="packageQueryParams" class="edhr-oq-pq__form" @submit.prevent>
          <el-form-item label="验证包">
            <el-input
              v-model="packageQueryParams.packageName"
              placeholder="验证包名称"
              clearable
              @keyup.enter="handlePackageQuery"
            />
          </el-form-item>
          <el-form-item label="客户项目">
            <el-input
              v-model="packageQueryParams.customerProjectName"
              placeholder="客户项目"
              clearable
              @keyup.enter="handlePackageQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handlePackageQuery" v-hasPermi="['mes:pro-edhr-oq-pq:query']">
              查询
            </el-button>
            <el-button @click="resetPackageQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="edhr-oq-pq__packages">
        <div class="edhr-oq-pq__section-title">
          <span>验证包</span>
          <span class="edhr-oq-pq__muted">{{ selectedPackage?.packageCode || '未选择验证包' }}</span>
        </div>
        <el-table
          v-loading="packageLoading"
          :data="packageList"
          height="250"
          row-key="id"
          empty-text="暂无验证包"
          highlight-current-row
          @row-click="handleSelectPackage"
        >
          <el-table-column label="验证包" min-width="240">
            <template #default="{ row }">
              <div class="edhr-oq-pq__strong">{{ row.packageName }}</div>
              <div class="edhr-oq-pq__muted">{{ row.packageCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="客户项目" prop="customerProjectName" min-width="180" show-overflow-tooltip />
          <el-table-column label="版本证据" min-width="210">
            <template #default="{ row }">
              <div>{{ row.releaseTag }}</div>
              <div class="edhr-oq-pq__muted">{{ row.schemaVersion }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="160">
            <template #default="{ row }">
              <el-tag :type="row.oqReady ? 'success' : 'danger'">
                {{ row.oqReady ? 'OQ Ready' : row.validationStatus }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下一步动作" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">{{ row.blockedReason }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="selectedPackage" class="edhr-oq-pq__summary">
        <el-descriptions :column="4" border>
          <el-descriptions-item label="客户项目">{{ selectedPackage.customerProjectName }}</el-descriptions-item>
          <el-descriptions-item label="发布标签">{{ selectedPackage.releaseTag }}</el-descriptions-item>
          <el-descriptions-item label="schema版本">{{ selectedPackage.schemaVersion }}</el-descriptions-item>
          <el-descriptions-item label="目标环境">{{ selectedPackage.targetEnvironment }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="edhr-oq-pq__grid">
        <div class="edhr-oq-pq__panel">
          <div class="edhr-oq-pq__section-title">
            <span>OQ/PQ用例</span>
            <el-button
              type="success"
              size="small"
              :disabled="!selectedPackage"
              @click="openCreateCaseDialog"
              v-hasPermi="['mes:pro-edhr-oq-pq:create']"
            >
              创建用例
            </el-button>
          </div>
          <el-table
            v-loading="caseLoading"
            :data="caseList"
            height="360"
            row-key="id"
            highlight-current-row
            empty-text="请选择验证包后查看OQ/PQ用例"
            @row-click="handleSelectCase"
          >
            <el-table-column label="用例" min-width="210">
              <template #default="{ row }">
                <div class="edhr-oq-pq__strong">{{ row.caseName }}</div>
                <div class="edhr-oq-pq__muted">{{ row.caseCode }} / {{ row.caseVersion }}</div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="row.caseType === 'PQ' ? 'warning' : 'primary'">{{ row.caseType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="步骤结果要求" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">{{ row.stepNo }} {{ row.stepTitle }}：{{ row.expectedResult }}</template>
            </el-table-column>
            <el-table-column label="证据要求" min-width="180" prop="evidenceRequirement" show-overflow-tooltip />
          </el-table>
        </div>

        <div class="edhr-oq-pq__panel">
          <div class="edhr-oq-pq__section-title">
            <span>执行记录</span>
            <div>
              <el-button
                type="success"
                size="small"
                :disabled="!selectedCase"
                @click="openCreateRunDialog"
                v-hasPermi="['mes:pro-edhr-oq-pq:create']"
              >
                创建执行
              </el-button>
              <el-button
                size="small"
                :disabled="!selectedRun"
                @click="handleSubmitFailedStep"
                v-hasPermi="['mes:pro-edhr-oq-pq:execute']"
              >
                提交失败步骤
              </el-button>
              <el-button
                size="small"
                :disabled="!selectedRun"
                @click="handleSubmitPassedStep"
                v-hasPermi="['mes:pro-edhr-oq-pq:execute']"
              >
                提交通过步骤
              </el-button>
              <el-button
                type="primary"
                size="small"
                :disabled="!selectedRun"
                @click="handleCompleteRun"
                v-hasPermi="['mes:pro-edhr-oq-pq:execute']"
              >
                完成执行
              </el-button>
            </div>
          </div>
          <el-table
            v-loading="runLoading"
            :data="runList"
            height="360"
            row-key="id"
            highlight-current-row
            empty-text="请选择用例后查看执行记录"
            @row-click="handleSelectRun"
          >
            <el-table-column label="执行编号" min-width="210" prop="runCode" show-overflow-tooltip />
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="runStatusTagType(row.runStatus)">{{ row.runStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="执行环境" min-width="160" prop="executionEnvironment" show-overflow-tooltip />
            <el-table-column label="证据" min-width="190" show-overflow-tooltip>
              <template #default="{ row }">{{ row.attachmentEvidence }} / {{ row.evidenceChecksum }}</template>
            </el-table-column>
            <el-table-column label="开放偏差" width="100">
              <template #default="{ row }">{{ row.openDeviationCount }}</template>
            </el-table-column>
            <el-table-column label="阻断" min-width="220" prop="blockedReason" show-overflow-tooltip />
          </el-table>
        </div>
      </section>

      <section class="edhr-oq-pq__deviation">
        <div class="edhr-oq-pq__section-title">
          <span>偏差整改复测</span>
          <span class="edhr-oq-pq__muted">{{ selectedRun?.runCode || '未选择执行记录' }}</span>
        </div>
        <el-table
          v-loading="deviationLoading"
          :data="deviationList"
          height="320"
          row-key="id"
          empty-text="请选择执行记录后查看偏差"
        >
          <el-table-column label="偏差" min-width="240">
            <template #default="{ row }">
              <div class="edhr-oq-pq__strong">{{ row.deviationCode }}</div>
              <div class="edhr-oq-pq__muted">{{ row.deviationTitle }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="deviationStatusTagType(row.deviationStatus)">{{ row.deviationStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="失败项" min-width="180" prop="failedActualResult" show-overflow-tooltip />
          <el-table-column label="下一步动作" min-width="260" prop="nextAction" show-overflow-tooltip />
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRemediateDialog(row)" v-hasPermi="['mes:pro-edhr-oq-pq:retest']">
                登记整改
              </el-button>
              <el-button link type="primary" @click="openRetestDialog(row)" v-hasPermi="['mes:pro-edhr-oq-pq:retest']">
                登记复测
              </el-button>
              <el-button link type="primary" @click="openCloseDialog(row)" v-hasPermi="['mes:pro-edhr-oq-pq:close']">
                关闭偏差
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <el-dialog v-model="caseDialogVisible" title="创建OQ/PQ用例" width="720px">
      <el-form ref="caseFormRef" :model="caseForm" :rules="caseRules" label-width="120px">
        <el-form-item label="用例类型" prop="caseType">
          <el-select v-model="caseForm.caseType">
            <el-option label="OQ" value="OQ" />
            <el-option label="PQ" value="PQ" />
          </el-select>
        </el-form-item>
        <el-form-item label="用例编号" prop="caseCode"><el-input v-model="caseForm.caseCode" /></el-form-item>
        <el-form-item label="用例名称" prop="caseName"><el-input v-model="caseForm.caseName" /></el-form-item>
        <el-form-item label="用例版本" prop="caseVersion"><el-input v-model="caseForm.caseVersion" /></el-form-item>
        <el-form-item label="步骤编号" prop="stepNo"><el-input v-model="caseForm.stepNo" /></el-form-item>
        <el-form-item label="步骤标题" prop="stepTitle"><el-input v-model="caseForm.stepTitle" /></el-form-item>
        <el-form-item label="预期结果" prop="expectedResult"><el-input v-model="caseForm.expectedResult" type="textarea" /></el-form-item>
        <el-form-item label="证据要求" prop="evidenceRequirement"><el-input v-model="caseForm.evidenceRequirement" /></el-form-item>
        <el-form-item label="责任人" prop="ownerName"><el-input v-model="caseForm.ownerName" /></el-form-item>
        <el-form-item label="复核人" prop="reviewerName"><el-input v-model="caseForm.reviewerName" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="caseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreateCase">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="runDialogVisible" title="创建执行记录" width="760px">
      <el-form ref="runFormRef" :model="runForm" :rules="runRules" label-width="140px">
        <el-form-item label="执行环境" prop="executionEnvironment"><el-input v-model="runForm.executionEnvironment" /></el-form-item>
        <el-form-item label="发布标签" prop="releaseTag"><el-input v-model="runForm.releaseTag" /></el-form-item>
        <el-form-item label="schema版本" prop="schemaVersion"><el-input v-model="runForm.schemaVersion" /></el-form-item>
        <el-form-item label="执行人" prop="executorName"><el-input v-model="runForm.executorName" /></el-form-item>
        <el-form-item label="复核人" prop="reviewerName"><el-input v-model="runForm.reviewerName" /></el-form-item>
        <el-form-item label="真实业务路径"><el-input v-model="runForm.realBusinessPath" type="textarea" /></el-form-item>
        <el-form-item label="真实测试数据来源"><el-input v-model="runForm.realTestDataSource" type="textarea" /></el-form-item>
        <el-form-item label="目标环境证明"><el-input v-model="runForm.targetEnvironmentProof" /></el-form-item>
        <el-form-item label="附件证据" prop="attachmentEvidence"><el-input v-model="runForm.attachmentEvidence" /></el-form-item>
        <el-form-item label="证据checksum" prop="evidenceChecksum"><el-input v-model="runForm.evidenceChecksum" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreateRun">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stepDialogVisible" title="提交步骤结果" width="680px">
      <el-form ref="stepFormRef" :model="stepForm" :rules="stepRules" label-width="120px">
        <el-form-item label="步骤结果" prop="stepResult">
          <el-select v-model="stepForm.stepResult">
            <el-option label="PASS" value="PASS" />
            <el-option label="FAIL" value="FAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="实际结果" prop="actualResult"><el-input v-model="stepForm.actualResult" type="textarea" /></el-form-item>
        <el-form-item label="附件证据" prop="attachmentEvidence"><el-input v-model="stepForm.attachmentEvidence" /></el-form-item>
        <el-form-item label="证据checksum" prop="evidenceChecksum"><el-input v-model="stepForm.evidenceChecksum" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stepDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitStepResult">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deviationDialogVisible" :title="deviationDialogTitle" width="700px">
      <el-form ref="deviationFormRef" :model="deviationForm" :rules="deviationRules" label-width="130px">
        <template v-if="deviationAction === 'remediate'">
          <el-form-item label="原因分析" prop="rootCause"><el-input v-model="deviationForm.rootCause" type="textarea" /></el-form-item>
          <el-form-item label="整改措施" prop="remediationAction"><el-input v-model="deviationForm.remediationAction" type="textarea" /></el-form-item>
          <el-form-item label="整改责任人" prop="remediationOwnerName"><el-input v-model="deviationForm.remediationOwnerName" /></el-form-item>
        </template>
        <template v-if="deviationAction === 'retest'">
          <el-form-item label="复测结果" prop="retestResult"><el-input v-model="deviationForm.retestResult" type="textarea" /></el-form-item>
          <el-form-item label="复测证据" prop="retestEvidence"><el-input v-model="deviationForm.retestEvidence" /></el-form-item>
          <el-form-item label="复测复核人" prop="retestReviewerName"><el-input v-model="deviationForm.retestReviewerName" /></el-form-item>
        </template>
        <template v-if="deviationAction === 'close'">
          <el-form-item label="关闭签核" prop="closeSignoffName"><el-input v-model="deviationForm.closeSignoffName" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="deviationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleDeviationAction">保存</el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getEdhrValidationPackagePage,
  type EdhrValidationPackagePageReqVO,
  type EdhrValidationPackageRespVO
} from '@/api/mes/pro/edhr/validation'
import {
  closeEdhrOqPqDeviation,
  completeEdhrOqPqRun,
  createEdhrOqPqCase,
  createEdhrOqPqRun,
  getEdhrOqPqCasePage,
  getEdhrOqPqDeviationPage,
  getEdhrOqPqRunPage,
  remediateEdhrOqPqDeviation,
  retestEdhrOqPqDeviation,
  submitEdhrOqPqStepResult,
  type EdhrOqPqCaseCreateReqVO,
  type EdhrOqPqCaseRespVO,
  type EdhrOqPqDeviationRespVO,
  type EdhrOqPqRunCreateReqVO,
  type EdhrOqPqRunRespVO,
  type EdhrOqPqStepSubmitReqVO
} from '@/api/mes/pro/edhr/oqPq'

defineOptions({ name: 'MesProEdhrOqPq' })

const packageLoading = ref(false)
const caseLoading = ref(false)
const runLoading = ref(false)
const deviationLoading = ref(false)
const submitLoading = ref(false)
const loadError = ref('')

const packageList = ref<EdhrValidationPackageRespVO[]>([])
const selectedPackage = ref<EdhrValidationPackageRespVO>()
const caseList = ref<EdhrOqPqCaseRespVO[]>([])
const selectedCase = ref<EdhrOqPqCaseRespVO>()
const runList = ref<EdhrOqPqRunRespVO[]>([])
const selectedRun = ref<EdhrOqPqRunRespVO>()
const deviationList = ref<EdhrOqPqDeviationRespVO[]>([])
const selectedDeviation = ref<EdhrOqPqDeviationRespVO>()

const caseDialogVisible = ref(false)
const runDialogVisible = ref(false)
const stepDialogVisible = ref(false)
const deviationDialogVisible = ref(false)
const deviationAction = ref<'remediate' | 'retest' | 'close'>('remediate')

const caseFormRef = ref<FormInstance>()
const runFormRef = ref<FormInstance>()
const stepFormRef = ref<FormInstance>()
const deviationFormRef = ref<FormInstance>()

const packageQueryParams = reactive<EdhrValidationPackagePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  packageName: '',
  customerProjectName: '',
  validationStatus: ''
})

const caseForm = reactive<EdhrOqPqCaseCreateReqVO>({
  packageId: 0,
  caseCode: '',
  caseName: '',
  caseType: 'OQ',
  caseVersion: 'v1',
  stepNo: '1',
  stepTitle: '',
  expectedResult: '',
  evidenceRequirement: '截图、执行日志或附件checksum',
  ownerName: '',
  reviewerName: '',
  sort: 0,
  remark: ''
})

const runForm = reactive<EdhrOqPqRunCreateReqVO>({
  packageId: 0,
  caseId: 0,
  executionEnvironment: 'local-test-tenant',
  releaseTag: '',
  schemaVersion: '',
  executorName: '',
  reviewerName: '',
  realBusinessPath: '',
  realTestDataSource: '',
  targetEnvironmentProof: '',
  attachmentEvidence: '',
  evidenceChecksum: '',
  remark: ''
})

const stepForm = reactive<EdhrOqPqStepSubmitReqVO>({
  runId: 0,
  actualResult: '',
  stepResult: 'FAIL',
  attachmentEvidence: '',
  evidenceChecksum: '',
  remark: ''
})

const deviationForm = reactive({
  deviationId: 0,
  rootCause: '',
  remediationAction: '',
  remediationOwnerName: '',
  retestResult: '',
  retestEvidence: '',
  retestReviewerName: '',
  closeSignoffName: ''
})

const caseRules: FormRules = {
  caseType: [{ required: true, message: '用例类型不能为空', trigger: 'change' }],
  caseCode: [{ required: true, message: '用例编号不能为空', trigger: 'blur' }],
  caseName: [{ required: true, message: '用例名称不能为空', trigger: 'blur' }],
  caseVersion: [{ required: true, message: '用例版本不能为空', trigger: 'blur' }],
  stepNo: [{ required: true, message: '步骤编号不能为空', trigger: 'blur' }],
  stepTitle: [{ required: true, message: '步骤标题不能为空', trigger: 'blur' }],
  expectedResult: [{ required: true, message: '预期结果不能为空', trigger: 'blur' }],
  evidenceRequirement: [{ required: true, message: '证据要求不能为空', trigger: 'blur' }],
  ownerName: [{ required: true, message: '责任人不能为空', trigger: 'blur' }],
  reviewerName: [{ required: true, message: '复核人不能为空', trigger: 'blur' }]
}

const runRules: FormRules = {
  executionEnvironment: [{ required: true, message: '执行环境不能为空', trigger: 'blur' }],
  releaseTag: [{ required: true, message: '发布标签不能为空', trigger: 'blur' }],
  schemaVersion: [{ required: true, message: 'schema版本不能为空', trigger: 'blur' }],
  executorName: [{ required: true, message: '执行人不能为空', trigger: 'blur' }],
  reviewerName: [{ required: true, message: '复核人不能为空', trigger: 'blur' }],
  attachmentEvidence: [{ required: true, message: '附件证据不能为空', trigger: 'blur' }],
  evidenceChecksum: [{ required: true, message: '证据checksum不能为空', trigger: 'blur' }]
}

const stepRules: FormRules = {
  stepResult: [{ required: true, message: '步骤结果不能为空', trigger: 'change' }],
  actualResult: [{ required: true, message: '实际结果不能为空', trigger: 'blur' }],
  attachmentEvidence: [{ required: true, message: '附件证据不能为空', trigger: 'blur' }],
  evidenceChecksum: [{ required: true, message: '证据checksum不能为空', trigger: 'blur' }]
}

const deviationRules: FormRules = {
  rootCause: [{ required: true, message: '原因分析不能为空', trigger: 'blur' }],
  remediationAction: [{ required: true, message: '整改措施不能为空', trigger: 'blur' }],
  remediationOwnerName: [{ required: true, message: '整改责任人不能为空', trigger: 'blur' }],
  retestResult: [{ required: true, message: '复测结果不能为空', trigger: 'blur' }],
  retestEvidence: [{ required: true, message: '复测证据不能为空', trigger: 'blur' }],
  retestReviewerName: [{ required: true, message: '复测复核人不能为空', trigger: 'blur' }],
  closeSignoffName: [{ required: true, message: '关闭签核不能为空', trigger: 'blur' }]
}

const deviationDialogTitle = computed(() => {
  if (deviationAction.value === 'remediate') return '登记整改措施'
  if (deviationAction.value === 'retest') return '登记复测结果'
  return '关闭偏差'
})

const resolveErrorMessage = (error: unknown) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return '操作失败，请检查接口、权限和必填证据。'
}

function assertPageResult<T>(data: unknown, label: string): PageResult<T[]> {
  const page = data as { list?: unknown; total?: unknown }
  if (!page || !Array.isArray(page.list) || typeof page.total !== 'number') {
    throw new Error(`${label}响应结构异常，缺少 list/total。`)
  }
  return page as PageResult<T[]>
}

const runStatusTagType = (status: string) => {
  if (status === 'PASSED') return 'success'
  if (status === 'DEVIATION_OPEN' || status === 'BLOCKED') return 'danger'
  return 'primary'
}

const deviationStatusTagType = (status: string) => {
  if (status === 'CLOSED') return 'success'
  if (status === 'OPEN') return 'danger'
  if (status === 'RETESTED') return 'warning'
  return 'primary'
}

const getPackageList = async () => {
  packageLoading.value = true
  loadError.value = ''
  try {
    const page = assertPageResult<EdhrValidationPackageRespVO>(
      await getEdhrValidationPackagePage({
        pageNo: packageQueryParams.pageNo,
        pageSize: packageQueryParams.pageSize,
        packageName: packageQueryParams.packageName?.trim() || undefined,
        customerProjectName: packageQueryParams.customerProjectName?.trim() || undefined,
        validationStatus: packageQueryParams.validationStatus || undefined
      }),
      '验证包'
    )
    packageList.value = page.list
  } catch (error) {
    packageList.value = []
    selectedPackage.value = undefined
    loadError.value = resolveErrorMessage(error)
  } finally {
    packageLoading.value = false
  }
}

const loadCaseList = async () => {
  if (!selectedPackage.value) {
    caseList.value = []
    return
  }
  caseLoading.value = true
  try {
    const page = assertPageResult<EdhrOqPqCaseRespVO>(
      await getEdhrOqPqCasePage({ pageNo: 1, pageSize: 100, packageId: selectedPackage.value.id }),
      'OQ/PQ用例'
    )
    caseList.value = page.list
  } catch (error) {
    caseList.value = []
    loadError.value = resolveErrorMessage(error)
  } finally {
    caseLoading.value = false
  }
}

const loadRunList = async () => {
  if (!selectedPackage.value) {
    runList.value = []
    return
  }
  runLoading.value = true
  try {
    const page = assertPageResult<EdhrOqPqRunRespVO>(
      await getEdhrOqPqRunPage({
        pageNo: 1,
        pageSize: 100,
        packageId: selectedPackage.value.id,
        caseId: selectedCase.value?.id
      }),
      '执行记录'
    )
    runList.value = page.list
  } catch (error) {
    runList.value = []
    loadError.value = resolveErrorMessage(error)
  } finally {
    runLoading.value = false
  }
}

const loadDeviationList = async () => {
  if (!selectedRun.value) {
    deviationList.value = []
    return
  }
  deviationLoading.value = true
  try {
    const page = assertPageResult<EdhrOqPqDeviationRespVO>(
      await getEdhrOqPqDeviationPage({ pageNo: 1, pageSize: 100, runId: selectedRun.value.id }),
      '偏差'
    )
    deviationList.value = page.list
  } catch (error) {
    deviationList.value = []
    loadError.value = resolveErrorMessage(error)
  } finally {
    deviationLoading.value = false
  }
}

const syncRunListRow = (latestRun: EdhrOqPqRunRespVO) => {
  const index = runList.value.findIndex((item) => item.id === latestRun.id)
  if (index >= 0) runList.value.splice(index, 1, latestRun)
  selectedRun.value = latestRun
}

const handlePackageQuery = () => {
  packageQueryParams.pageNo = 1
  selectedPackage.value = undefined
  selectedCase.value = undefined
  selectedRun.value = undefined
  caseList.value = []
  runList.value = []
  deviationList.value = []
  getPackageList()
}

const resetPackageQuery = () => {
  packageQueryParams.pageNo = 1
  packageQueryParams.pageSize = 10
  packageQueryParams.packageName = ''
  packageQueryParams.customerProjectName = ''
  packageQueryParams.validationStatus = ''
  handlePackageQuery()
}

const handleSelectPackage = async (row: EdhrValidationPackageRespVO) => {
  selectedPackage.value = row
  selectedCase.value = undefined
  selectedRun.value = undefined
  deviationList.value = []
  await loadCaseList()
  await loadRunList()
}

const handleSelectCase = async (row: EdhrOqPqCaseRespVO) => {
  selectedCase.value = row
  selectedRun.value = undefined
  deviationList.value = []
  await loadRunList()
}

const handleSelectRun = async (row: EdhrOqPqRunRespVO) => {
  selectedRun.value = row
  await loadDeviationList()
}

const openCreateCaseDialog = () => {
  if (!selectedPackage.value) return
  caseForm.packageId = selectedPackage.value.id
  caseForm.caseType = 'OQ'
  caseForm.caseCode = ''
  caseForm.caseName = ''
  caseForm.caseVersion = 'v1'
  caseForm.stepNo = '1'
  caseForm.stepTitle = ''
  caseForm.expectedResult = ''
  caseForm.evidenceRequirement = '截图、执行日志或附件checksum'
  caseForm.ownerName = selectedPackage.value.validationOwnerName
  caseForm.reviewerName = selectedPackage.value.qaOwnerName
  caseForm.sort = caseList.value.length + 1
  caseForm.remark = ''
  caseFormRef.value?.clearValidate()
  caseDialogVisible.value = true
}

const handleCreateCase = async () => {
  const valid = await caseFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const created = await createEdhrOqPqCase(caseForm)
    ElMessage.success('OQ/PQ用例已创建')
    caseDialogVisible.value = false
    selectedCase.value = created
    await loadCaseList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submitLoading.value = false
  }
}

const openCreateRunDialog = () => {
  if (!selectedPackage.value || !selectedCase.value) return
  runForm.packageId = selectedPackage.value.id
  runForm.caseId = selectedCase.value.id
  runForm.executionEnvironment = selectedPackage.value.targetEnvironment || 'local-test-tenant'
  runForm.releaseTag = selectedPackage.value.releaseTag
  runForm.schemaVersion = selectedPackage.value.schemaVersion
  runForm.executorName = selectedCase.value.ownerName
  runForm.reviewerName = selectedCase.value.reviewerName
  runForm.realBusinessPath = selectedCase.value.caseType === 'PQ' ? '批记录真实创建、执行、复核路径' : ''
  runForm.realTestDataSource = ''
  runForm.targetEnvironmentProof = selectedPackage.value.targetEnvironment
  runForm.attachmentEvidence = ''
  runForm.evidenceChecksum = ''
  runForm.remark = ''
  runFormRef.value?.clearValidate()
  runDialogVisible.value = true
}

const handleCreateRun = async () => {
  const valid = await runFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const created = await createEdhrOqPqRun(runForm)
    ElMessage.success('执行记录已创建')
    runDialogVisible.value = false
    syncRunListRow(created)
    await loadRunList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submitLoading.value = false
  }
}

const openStepDialog = (stepResult: 'PASS' | 'FAIL') => {
  if (!selectedRun.value) return
  stepForm.runId = selectedRun.value.id
  stepForm.stepResult = stepResult
  stepForm.actualResult = stepResult === 'FAIL' ? '实际结果与预期不一致，形成失败项' : '复测后实际结果与预期一致'
  stepForm.attachmentEvidence = stepResult === 'FAIL' ? 'oq-failure-evidence' : 'oq-retest-pass-evidence'
  stepForm.evidenceChecksum = stepResult === 'FAIL' ? 'sha256-oq-failure' : 'sha256-oq-retest-pass'
  stepForm.remark = ''
  stepFormRef.value?.clearValidate()
  stepDialogVisible.value = true
}

const handleSubmitFailedStep = () => openStepDialog('FAIL')

const handleSubmitPassedStep = () => openStepDialog('PASS')

const handleSubmitStepResult = async () => {
  const valid = await stepFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    await submitEdhrOqPqStepResult(stepForm)
    ElMessage.success(stepForm.stepResult === 'FAIL' ? '失败项已提交并生成偏差' : '通过步骤已提交')
    stepDialogVisible.value = false
    await loadRunList()
    const latestRun = runList.value.find((item) => item.id === stepForm.runId)
    if (latestRun) selectedRun.value = latestRun
    await loadDeviationList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submitLoading.value = false
  }
}

const handleCompleteRun = async () => {
  if (!selectedRun.value) return
  submitLoading.value = true
  try {
    const latestRun = await completeEdhrOqPqRun(selectedRun.value.id)
    syncRunListRow(latestRun)
    ElMessage.success('执行记录已完成')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
    await loadRunList()
  } finally {
    submitLoading.value = false
  }
}

const resetDeviationForm = (row: EdhrOqPqDeviationRespVO) => {
  selectedDeviation.value = row
  deviationForm.deviationId = row.id
  deviationForm.rootCause = row.rootCause || ''
  deviationForm.remediationAction = row.remediationAction || ''
  deviationForm.remediationOwnerName = row.remediationOwnerName || ''
  deviationForm.retestResult = row.retestResult || ''
  deviationForm.retestEvidence = row.retestEvidence || ''
  deviationForm.retestReviewerName = row.retestReviewerName || ''
  deviationForm.closeSignoffName = row.closeSignoffName || ''
  deviationFormRef.value?.clearValidate()
}

const openRemediateDialog = (row: EdhrOqPqDeviationRespVO) => {
  deviationAction.value = 'remediate'
  resetDeviationForm(row)
  deviationDialogVisible.value = true
}

const openRetestDialog = (row: EdhrOqPqDeviationRespVO) => {
  deviationAction.value = 'retest'
  resetDeviationForm(row)
  deviationDialogVisible.value = true
}

const openCloseDialog = (row: EdhrOqPqDeviationRespVO) => {
  deviationAction.value = 'close'
  resetDeviationForm(row)
  deviationDialogVisible.value = true
}

const handleRemediateDeviation = async () => {
  await remediateEdhrOqPqDeviation({
    deviationId: deviationForm.deviationId,
    rootCause: deviationForm.rootCause,
    remediationAction: deviationForm.remediationAction,
    remediationOwnerName: deviationForm.remediationOwnerName
  })
}

const handleRetestDeviation = async () => {
  await retestEdhrOqPqDeviation({
    deviationId: deviationForm.deviationId,
    retestResult: deviationForm.retestResult,
    retestEvidence: deviationForm.retestEvidence,
    retestReviewerName: deviationForm.retestReviewerName
  })
}

const handleCloseDeviation = async () => {
  await closeEdhrOqPqDeviation({
    deviationId: deviationForm.deviationId,
    closeSignoffName: deviationForm.closeSignoffName
  })
}

const handleDeviationAction = async () => {
  const valid = await deviationFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (deviationAction.value === 'remediate') await handleRemediateDeviation()
    if (deviationAction.value === 'retest') await handleRetestDeviation()
    if (deviationAction.value === 'close') await handleCloseDeviation()
    ElMessage.success('偏差状态已更新')
    deviationDialogVisible.value = false
    await loadDeviationList()
    await loadRunList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  getPackageList()
})
</script>

<style scoped>
.edhr-oq-pq {
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: #172033;
}

.edhr-oq-pq__toolbar,
.edhr-oq-pq__packages,
.edhr-oq-pq__summary,
.edhr-oq-pq__panel,
.edhr-oq-pq__deviation {
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  padding: 14px;
}

.edhr-oq-pq__toolbar {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.edhr-oq-pq__packages {
  border-top: 0;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}

.edhr-oq-pq__title-row,
.edhr-oq-pq__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.edhr-oq-pq__title-row h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.edhr-oq-pq__subtitle,
.edhr-oq-pq__muted {
  color: #6b7280;
  font-size: 13px;
}

.edhr-oq-pq__form {
  margin-top: 12px;
}

.edhr-oq-pq__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.35fr);
  gap: 12px;
}

.edhr-oq-pq__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-oq-pq :deep(.el-table__header th) {
  background: #f7f9fc;
  color: #263247;
  font-size: 13px;
}

.edhr-oq-pq :deep(.el-table__row) {
  height: 52px;
}

.edhr-oq-pq :deep(.el-form-item) {
  margin-bottom: 12px;
}

@media (max-width: 1200px) {
  .edhr-oq-pq__grid {
    grid-template-columns: 1fr;
  }

  .edhr-oq-pq__title-row,
  .edhr-oq-pq__section-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
