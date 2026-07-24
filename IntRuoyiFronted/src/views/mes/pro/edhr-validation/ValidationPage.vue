<template>
  <ContentWrap>
    <div class="edhr-validation">
      <section class="edhr-validation__toolbar">
        <div class="edhr-validation__title-row">
          <div>
            <h2>验证包</h2>
            <div class="edhr-validation__subtitle">
              CSV基础信息 / URS / FRS / 风险 / IQ / OQ / PQ / 追溯矩阵 / OQ Ready
            </div>
          </div>
          <el-tag :type="traceSummary?.oqReady ? 'success' : 'danger'" effect="plain">
            {{ traceSummary?.oqReady ? 'OQ Ready' : '阻塞' }}
          </el-tag>
        </div>

        <el-form :inline="true" :model="packageQueryParams" class="edhr-validation__form" @submit.prevent>
          <el-form-item label="验证包">
            <el-input
              v-model="packageQueryParams.packageName"
              clearable
              class="!w-190px"
              @keyup.enter="handlePackageQuery"
            />
          </el-form-item>
          <el-form-item label="客户项目">
            <el-input
              v-model="packageQueryParams.customerProjectName"
              clearable
              class="!w-170px"
              @keyup.enter="handlePackageQuery"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="packageQueryParams.validationStatus" clearable class="!w-130px">
              <el-option label="阻塞" value="BLOCKED" />
              <el-option label="已准备" value="PREPARED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handlePackageQuery" v-hasPermi="['mes:pro-edhr-validation:query']">
              <Icon icon="ep:search" class="mr-5px" />
              查询
            </el-button>
            <el-button @click="resetPackageQuery">
              <Icon icon="ep:refresh" class="mr-5px" />
              重置
            </el-button>
            <el-button type="success" @click="openCreatePackageDialog" v-hasPermi="['mes:pro-edhr-validation:create']">
              <Icon icon="ep:plus" class="mr-5px" />
              新建验证包
            </el-button>
          </el-form-item>
        </el-form>
      </section>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <section class="edhr-validation__packages">
        <div class="edhr-validation__section-title">
          <span>验证包</span>
          <span class="edhr-validation__muted">{{ selectedPackage?.packageCode || '未选择验证包' }}</span>
        </div>
        <el-table
          v-loading="packageLoading"
          :data="packageList"
          stripe
          highlight-current-row
          :show-overflow-tooltip="true"
          empty-text="暂无验证包"
          @row-click="handleSelectPackage"
        >
          <el-table-column label="验证包" min-width="230">
            <template #default="{ row }">
              <div class="edhr-validation__strong">{{ row.packageName }}</div>
              <div class="edhr-validation__muted">{{ row.packageCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="客户项目" prop="customerProjectName" min-width="160" />
          <el-table-column label="发布标签" prop="releaseTag" min-width="150" />
          <el-table-column label="schema版本" prop="schemaVersion" min-width="150" />
          <el-table-column label="目标环境" prop="targetEnvironment" min-width="130" />
          <el-table-column label="责任人" min-width="160">
            <template #default="{ row }">
              <div class="edhr-validation__strong">{{ row.validationOwnerName }}</div>
              <div class="edhr-validation__muted">QA：{{ row.qaOwnerName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.validationStatus === 'PREPARED' ? 'success' : 'danger'">
                {{ row.validationStatus || '--' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="OQ Ready" width="120">
            <template #default="{ row }">
              <el-tag :type="row.oqReady ? 'success' : 'danger'" effect="plain">
                {{ row.oqReady ? 'OQ Ready' : '阻塞' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="handleSelectPackage(row)" v-hasPermi="['mes:pro-edhr-validation:query']">
                <Icon icon="ep:view" class="mr-4px" />
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="packageTotal"
          v-model:page="packageQueryParams.pageNo"
          v-model:limit="packageQueryParams.pageSize"
          @pagination="getPackageList"
        />
      </section>

      <section v-if="selectedPackage" class="edhr-validation__csv">
        <div class="edhr-validation__section-title">
          <span>CSV基础信息</span>
          <span class="edhr-validation__muted">{{ selectedPackage.customerProjectName }}</span>
        </div>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="客户">{{ selectedPackage.customerName }}</el-descriptions-item>
          <el-descriptions-item label="现场">{{ selectedPackage.siteName }}</el-descriptions-item>
          <el-descriptions-item label="目标环境">{{ selectedPackage.targetEnvironment }}</el-descriptions-item>
          <el-descriptions-item label="系统范围">{{ selectedPackage.systemScope }}</el-descriptions-item>
          <el-descriptions-item label="验证范围">{{ selectedPackage.validationScope }}</el-descriptions-item>
          <el-descriptions-item label="阻断">{{ selectedPackage.blockedReason || '--' }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="edhr-validation__detail-grid">
        <div class="edhr-validation__items">
          <div class="edhr-validation__section-title">
            <span>验证条目</span>
            <span class="edhr-validation__muted">URS / FRS / 风险 / IQ / OQ / PQ</span>
            <el-button
              type="primary"
              size="small"
              :disabled="!selectedPackage"
              @click="openCreateItemDialog"
              v-hasPermi="['mes:pro-edhr-validation:create']"
            >
              <Icon icon="ep:plus" class="mr-4px" />
              登记条目
            </el-button>
          </div>
          <el-table
            v-loading="itemLoading"
            :data="itemList"
            stripe
            :show-overflow-tooltip="true"
            empty-text="请选择验证包后查看条目"
          >
            <el-table-column label="条目" min-width="210">
              <template #default="{ row }">
                <div class="edhr-validation__strong">{{ row.itemName }}</div>
                <div class="edhr-validation__muted">{{ row.itemCode }} / {{ row.itemVersion }}</div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="resolveItemTypeTag(row.itemType)" effect="plain">{{ row.itemType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="itemStatus" width="100" />
            <el-table-column label="责任人" prop="ownerName" width="120" />
            <el-table-column label="签核角色" prop="signoffRole" width="120" />
            <el-table-column label="来源文档" prop="sourceDocument" min-width="180" />
          </el-table>
        </div>

        <div class="edhr-validation__trace">
          <div class="edhr-validation__section-title">
            <span>追溯矩阵</span>
            <span class="edhr-validation__muted">{{ traceSummary?.traceStatus || '未评估' }}</span>
            <el-button
              type="primary"
              size="small"
              :disabled="!selectedPackage || itemList.length < 2"
              @click="openCreateTraceDialog"
              v-hasPermi="['mes:pro-edhr-validation:create']"
            >
              <Icon icon="ep:connection" class="mr-4px" />
              建立追溯
            </el-button>
            <el-button
              type="warning"
              size="small"
              :disabled="!selectedPackage"
              :loading="traceLoading"
              @click="handleEvaluateTrace"
              v-hasPermi="['mes:pro-edhr-validation:evaluate-trace']"
            >
              <Icon icon="ep:finished" class="mr-4px" />
              计算OQ Ready
            </el-button>
          </div>

          <el-descriptions v-if="traceSummary" :column="3" border class="edhr-validation__summary">
            <el-descriptions-item label="URS">{{ traceSummary.ursCount }}</el-descriptions-item>
            <el-descriptions-item label="FRS">{{ traceSummary.frsCount }}</el-descriptions-item>
            <el-descriptions-item label="风险">{{ traceSummary.riskCount }}</el-descriptions-item>
            <el-descriptions-item label="IQ">{{ traceSummary.iqCount }}</el-descriptions-item>
            <el-descriptions-item label="OQ">{{ traceSummary.oqCount }}</el-descriptions-item>
            <el-descriptions-item label="PQ">{{ traceSummary.pqCount }}</el-descriptions-item>
            <el-descriptions-item label="断裂明细">{{ traceSummary.brokenTraceCount }}</el-descriptions-item>
            <el-descriptions-item label="OQ Ready">
              <el-tag :type="traceSummary.oqReady ? 'success' : 'danger'">
                {{ traceSummary.oqReady ? 'OQ Ready' : '阻塞' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="下一步动作">{{ traceSummary.nextAction }}</el-descriptions-item>
          </el-descriptions>

          <el-table
            v-loading="traceLoading"
            :data="brokenItems"
            stripe
            :show-overflow-tooltip="true"
            empty-text="请选择验证包并评估追溯门禁"
          >
            <el-table-column label="URS" min-width="160">
              <template #default="{ row }">
                <div class="edhr-validation__strong">{{ row.sourceItemCode }}</div>
                <div class="edhr-validation__muted">{{ row.sourceItemType }}</div>
              </template>
            </el-table-column>
            <el-table-column label="缺失目标" min-width="160">
              <template #default="{ row }">{{ row.missingItemType }} / {{ row.missingItemName }}</template>
            </el-table-column>
            <el-table-column label="责任人" prop="ownerName" width="120" />
            <el-table-column label="签核角色" prop="signoffRole" width="120" />
            <el-table-column label="下一步动作" prop="nextAction" min-width="230" />
            <el-table-column label="阻断原因" prop="blockingReason" min-width="180" />
          </el-table>
        </div>
      </section>

      <el-dialog v-model="packageDialogVisible" title="新建验证包" width="780px">
        <el-form
          ref="packageFormRef"
          :model="packageForm"
          :rules="packageRules"
          label-width="120px"
          class="edhr-validation__dialog-form"
        >
          <el-form-item label="验证包名称" prop="packageName">
            <el-input v-model="packageForm.packageName" maxlength="128" />
          </el-form-item>
          <el-form-item label="客户项目" prop="customerProjectName">
            <el-input v-model="packageForm.customerProjectName" maxlength="128" />
          </el-form-item>
          <el-form-item label="客户名称" prop="customerName">
            <el-input v-model="packageForm.customerName" maxlength="128" />
          </el-form-item>
          <el-form-item label="客户现场" prop="siteName">
            <el-input v-model="packageForm.siteName" maxlength="128" />
          </el-form-item>
          <el-form-item label="系统范围" prop="systemScope">
            <el-input v-model="packageForm.systemScope" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
          <el-form-item label="验证范围" prop="validationScope">
            <el-input v-model="packageForm.validationScope" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
          <el-form-item label="发布标签" prop="releaseTag">
            <el-input v-model="packageForm.releaseTag" maxlength="64" />
          </el-form-item>
          <el-form-item label="schema版本" prop="schemaVersion">
            <el-input v-model="packageForm.schemaVersion" maxlength="64" />
          </el-form-item>
          <el-form-item label="目标环境" prop="targetEnvironment">
            <el-select v-model="packageForm.targetEnvironment" class="!w-220px">
              <el-option label="本地测试" value="local-test" />
              <el-option label="测试租户" value="test-tenant" />
              <el-option label="客户测试环境" value="customer-test" />
            </el-select>
          </el-form-item>
          <el-form-item label="验证负责人" prop="validationOwnerName">
            <el-input v-model="packageForm.validationOwnerName" maxlength="128" />
          </el-form-item>
          <el-form-item label="QA负责人" prop="qaOwnerName">
            <el-input v-model="packageForm.qaOwnerName" maxlength="128" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="packageForm.remark" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="packageDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleCreatePackage">创建</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="itemDialogVisible" title="登记验证条目" width="720px">
        <el-form
          ref="itemFormRef"
          :model="itemForm"
          :rules="itemRules"
          label-width="120px"
          class="edhr-validation__dialog-form"
        >
          <el-form-item label="条目类型" prop="itemType">
            <el-select v-model="itemForm.itemType" class="!w-220px">
              <el-option label="URS" value="URS" />
              <el-option label="FRS" value="FRS" />
              <el-option label="风险" value="RISK" />
              <el-option label="IQ" value="IQ" />
              <el-option label="OQ" value="OQ" />
              <el-option label="PQ" value="PQ" />
            </el-select>
          </el-form-item>
          <el-form-item label="条目编号" prop="itemCode">
            <el-input v-model="itemForm.itemCode" maxlength="64" />
          </el-form-item>
          <el-form-item label="条目名称" prop="itemName">
            <el-input v-model="itemForm.itemName" maxlength="128" />
          </el-form-item>
          <el-form-item label="版本" prop="itemVersion">
            <el-input v-model="itemForm.itemVersion" maxlength="64" />
          </el-form-item>
          <el-form-item label="状态" prop="itemStatus">
            <el-select v-model="itemForm.itemStatus" class="!w-220px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="有效" value="ACTIVE" />
              <el-option label="关闭" value="CLOSED" />
            </el-select>
          </el-form-item>
          <el-form-item label="责任人" prop="ownerName">
            <el-input v-model="itemForm.ownerName" maxlength="128" />
          </el-form-item>
          <el-form-item label="签核角色" prop="signoffRole">
            <el-input v-model="itemForm.signoffRole" maxlength="128" />
          </el-form-item>
          <el-form-item label="来源文档" prop="sourceDocument">
            <el-input v-model="itemForm.sourceDocument" maxlength="256" />
          </el-form-item>
          <el-form-item label="业务过程">
            <el-input v-model="itemForm.businessProcess" maxlength="256" />
          </el-form-item>
          <el-form-item label="验收标准">
            <el-input v-model="itemForm.acceptanceCriteria" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="itemDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleCreateItem">登记</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="traceDialogVisible" title="建立追溯关系" width="680px">
        <el-form
          ref="traceFormRef"
          :model="traceForm"
          :rules="traceRules"
          label-width="120px"
          class="edhr-validation__dialog-form"
        >
          <el-form-item label="来源URS" prop="sourceItemId">
            <el-select v-model="traceForm.sourceItemId" filterable class="!w-360px">
              <el-option
                v-for="item in ursItems"
                :key="item.id"
                :label="`${item.itemCode} ${item.itemName}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="目标条目" prop="targetItemId">
            <el-select v-model="traceForm.targetItemId" filterable class="!w-360px" @change="syncLinkType">
              <el-option
                v-for="item in traceTargetItems"
                :key="item.id"
                :label="`${item.itemType} ${item.itemCode} ${item.itemName}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="追溯类型" prop="linkType">
            <el-select v-model="traceForm.linkType" class="!w-220px">
              <el-option label="URS到FRS" value="URS_FRS" />
              <el-option label="URS到风险" value="URS_RISK" />
              <el-option label="URS到验证项" value="URS_VERIFICATION" />
            </el-select>
          </el-form-item>
          <el-form-item label="责任人" prop="ownerName">
            <el-input v-model="traceForm.ownerName" maxlength="128" />
          </el-form-item>
          <el-form-item label="下一步动作" prop="nextAction">
            <el-input v-model="traceForm.nextAction" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="traceForm.remark" type="textarea" :rows="2" maxlength="500" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="traceDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleCreateTraceLink">建立</el-button>
        </template>
      </el-dialog>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  createEdhrValidationPackage,
  createEdhrValidationRequirementItem,
  createEdhrValidationTraceLink,
  evaluateEdhrValidationTrace,
  getEdhrValidationPackageDetail,
  getEdhrValidationPackagePage,
  getEdhrValidationRequirementItemPage,
  type EdhrValidationItemType,
  type EdhrValidationPackageCreateReqVO,
  type EdhrValidationPackagePageReqVO,
  type EdhrValidationPackageRespVO,
  type EdhrValidationRequirementItemCreateReqVO,
  type EdhrValidationRequirementItemRespVO,
  type EdhrValidationTraceEvaluateRespVO,
  type EdhrValidationTraceIssueRespVO,
  type EdhrValidationTraceLinkCreateReqVO
} from '@/api/mes/pro/edhr/validation'

defineOptions({ name: 'MesProEdhrValidation' })

const packageLoading = ref(false)
const itemLoading = ref(false)
const traceLoading = ref(false)
const submitLoading = ref(false)
const loadError = ref('')

const packageList = ref<EdhrValidationPackageRespVO[]>([])
const packageTotal = ref(0)
const selectedPackage = ref<EdhrValidationPackageRespVO>()
const itemList = ref<EdhrValidationRequirementItemRespVO[]>([])
const traceSummary = ref<EdhrValidationTraceEvaluateRespVO>()

const packageDialogVisible = ref(false)
const itemDialogVisible = ref(false)
const traceDialogVisible = ref(false)
const packageFormRef = ref<FormInstance>()
const itemFormRef = ref<FormInstance>()
const traceFormRef = ref<FormInstance>()

const packageQueryParams = reactive<EdhrValidationPackagePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  packageName: '',
  customerProjectName: '',
  validationStatus: ''
})

const packageForm = reactive<EdhrValidationPackageCreateReqVO>({
  packageName: '',
  customerProjectName: '',
  customerName: '',
  siteName: '',
  systemScope: 'eDHR批记录、电子签名、审计追踪、放行与交付证据',
  validationScope: 'CSV基础信息、URS/FRS/风险/IQ/OQ/PQ追溯矩阵',
  releaseTag: '',
  schemaVersion: '',
  targetEnvironment: 'test-tenant',
  validationOwnerName: '',
  qaOwnerName: '',
  remark: ''
})

const itemForm = reactive<EdhrValidationRequirementItemCreateReqVO>({
  packageId: 0,
  itemCode: '',
  itemName: '',
  itemType: 'URS',
  itemVersion: 'v1',
  itemStatus: 'ACTIVE',
  ownerName: '',
  signoffRole: '',
  sourceDocument: '',
  businessProcess: '',
  acceptanceCriteria: '',
  sort: 0,
  remark: ''
})

const traceForm = reactive<EdhrValidationTraceLinkCreateReqVO>({
  packageId: 0,
  sourceItemId: 0,
  targetItemId: 0,
  linkType: 'URS_FRS',
  ownerName: '',
  nextAction: '追溯关系已登记，重新计算OQ Ready门禁',
  remark: ''
})

const packageRules: FormRules = {
  packageName: [{ required: true, message: '验证包名称不能为空', trigger: 'blur' }],
  customerProjectName: [{ required: true, message: '客户项目不能为空', trigger: 'blur' }],
  customerName: [{ required: true, message: '客户名称不能为空', trigger: 'blur' }],
  siteName: [{ required: true, message: '客户现场不能为空', trigger: 'blur' }],
  systemScope: [{ required: true, message: '系统范围不能为空', trigger: 'blur' }],
  validationScope: [{ required: true, message: '验证范围不能为空', trigger: 'blur' }],
  releaseTag: [{ required: true, message: '发布标签不能为空', trigger: 'blur' }],
  schemaVersion: [{ required: true, message: 'schema版本不能为空', trigger: 'blur' }],
  targetEnvironment: [{ required: true, message: '目标环境不能为空', trigger: 'change' }],
  validationOwnerName: [{ required: true, message: '验证负责人不能为空', trigger: 'blur' }],
  qaOwnerName: [{ required: true, message: 'QA负责人不能为空', trigger: 'blur' }]
}

const itemRules: FormRules = {
  itemType: [{ required: true, message: '条目类型不能为空', trigger: 'change' }],
  itemCode: [{ required: true, message: '条目编号不能为空', trigger: 'blur' }],
  itemName: [{ required: true, message: '条目名称不能为空', trigger: 'blur' }],
  itemVersion: [{ required: true, message: '版本不能为空', trigger: 'blur' }],
  itemStatus: [{ required: true, message: '状态不能为空', trigger: 'change' }],
  ownerName: [{ required: true, message: '责任人不能为空', trigger: 'blur' }],
  signoffRole: [{ required: true, message: '签核角色不能为空', trigger: 'blur' }],
  sourceDocument: [{ required: true, message: '来源文档不能为空', trigger: 'blur' }]
}

const traceRules: FormRules = {
  sourceItemId: [{ required: true, message: '来源URS不能为空', trigger: 'change' }],
  targetItemId: [{ required: true, message: '目标条目不能为空', trigger: 'change' }],
  linkType: [{ required: true, message: '追溯类型不能为空', trigger: 'change' }],
  ownerName: [{ required: true, message: '责任人不能为空', trigger: 'blur' }],
  nextAction: [{ required: true, message: '下一步动作不能为空', trigger: 'blur' }]
}

const brokenItems = computed<EdhrValidationTraceIssueRespVO[]>(() => traceSummary.value?.brokenItems || [])
const ursItems = computed(() => itemList.value.filter((item) => item.itemType === 'URS'))
const traceTargetItems = computed(() => itemList.value.filter((item) => item.itemType !== 'URS'))

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

const buildPackageQuery = (): EdhrValidationPackagePageReqVO => ({
  pageNo: packageQueryParams.pageNo,
  pageSize: packageQueryParams.pageSize,
  packageName: packageQueryParams.packageName?.trim() || undefined,
  customerProjectName: packageQueryParams.customerProjectName?.trim() || undefined,
  validationStatus: packageQueryParams.validationStatus || undefined
})

const getPackageList = async () => {
  packageLoading.value = true
  loadError.value = ''
  try {
    const page = assertPageResult<EdhrValidationPackageRespVO>(
      await getEdhrValidationPackagePage(buildPackageQuery()),
      '验证包'
    )
    packageList.value = page.list
    packageTotal.value = page.total
    if (!selectedPackage.value && page.list.length > 0) {
      await handleSelectPackage(page.list[0])
    }
  } catch (error) {
    packageList.value = []
    packageTotal.value = 0
    selectedPackage.value = undefined
    itemList.value = []
    traceSummary.value = undefined
    loadError.value = resolveErrorMessage(error, '验证包加载失败，请检查接口和权限。')
  } finally {
    packageLoading.value = false
  }
}

const getItemList = async () => {
  if (!selectedPackage.value) {
    itemList.value = []
    return
  }
  itemLoading.value = true
  loadError.value = ''
  try {
    const page = assertPageResult<EdhrValidationRequirementItemRespVO>(
      await getEdhrValidationRequirementItemPage({
        pageNo: 1,
        pageSize: 100,
        packageId: selectedPackage.value.id
      }),
      '验证条目'
    )
    itemList.value = page.list
  } catch (error) {
    itemList.value = []
    loadError.value = resolveErrorMessage(error, '验证条目加载失败，请检查验证包和权限。')
  } finally {
    itemLoading.value = false
  }
}

const refreshSelectedPackage = async () => {
  if (!selectedPackage.value) return
  try {
    const latestPackage = await getEdhrValidationPackageDetail(selectedPackage.value.id)
    selectedPackage.value = latestPackage
    syncPackageListRow(latestPackage)
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '验证包详情刷新失败，请重新选择验证包。')
  }
}

const syncPackageListRow = (latestPackage: EdhrValidationPackageRespVO) => {
  const packageIndex = packageList.value.findIndex((item) => item.id === latestPackage.id)
  if (packageIndex >= 0) {
    packageList.value.splice(packageIndex, 1, latestPackage)
  }
}

const handleSelectPackage = async (row: EdhrValidationPackageRespVO) => {
  selectedPackage.value = row
  traceSummary.value = undefined
  await getItemList()
}

const handlePackageQuery = () => {
  packageQueryParams.pageNo = 1
  selectedPackage.value = undefined
  itemList.value = []
  traceSummary.value = undefined
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

const resetPackageForm = () => {
  packageForm.packageName = ''
  packageForm.customerProjectName = ''
  packageForm.customerName = ''
  packageForm.siteName = ''
  packageForm.systemScope = 'eDHR批记录、电子签名、审计追踪、放行与交付证据'
  packageForm.validationScope = 'CSV基础信息、URS/FRS/风险/IQ/OQ/PQ追溯矩阵'
  packageForm.releaseTag = ''
  packageForm.schemaVersion = ''
  packageForm.targetEnvironment = 'test-tenant'
  packageForm.validationOwnerName = ''
  packageForm.qaOwnerName = ''
  packageForm.remark = ''
  packageFormRef.value?.clearValidate()
}

const resetItemForm = () => {
  itemForm.packageId = selectedPackage.value?.id || 0
  itemForm.itemCode = ''
  itemForm.itemName = ''
  itemForm.itemType = 'URS'
  itemForm.itemVersion = 'v1'
  itemForm.itemStatus = 'ACTIVE'
  itemForm.ownerName = selectedPackage.value?.validationOwnerName || ''
  itemForm.signoffRole = '验证负责人'
  itemForm.sourceDocument = ''
  itemForm.businessProcess = ''
  itemForm.acceptanceCriteria = ''
  itemForm.sort = itemList.value.length + 1
  itemForm.remark = ''
  itemFormRef.value?.clearValidate()
}

const resetTraceForm = () => {
  traceForm.packageId = selectedPackage.value?.id || 0
  traceForm.sourceItemId = ursItems.value[0]?.id || 0
  traceForm.targetItemId = traceTargetItems.value[0]?.id || 0
  traceForm.linkType = resolveLinkType(traceTargetItems.value[0]?.itemType)
  traceForm.ownerName = selectedPackage.value?.validationOwnerName || ''
  traceForm.nextAction = '追溯关系已登记，重新计算OQ Ready门禁'
  traceForm.remark = ''
  traceFormRef.value?.clearValidate()
}

const openCreatePackageDialog = () => {
  resetPackageForm()
  packageDialogVisible.value = true
}

const openCreateItemDialog = () => {
  if (!selectedPackage.value) {
    loadError.value = '请先选择验证包。'
    return
  }
  resetItemForm()
  itemDialogVisible.value = true
}

const openCreateTraceDialog = () => {
  if (!selectedPackage.value) {
    loadError.value = '请先选择验证包。'
    return
  }
  if (ursItems.value.length === 0 || traceTargetItems.value.length === 0) {
    loadError.value = '建立追溯前必须至少有一条URS和一条FRS/风险/IQ/OQ/PQ条目。'
    return
  }
  resetTraceForm()
  traceDialogVisible.value = true
}

const handleCreatePackage = async () => {
  loadError.value = ''
  try {
    await packageFormRef.value?.validate()
  } catch (error) {
    return
  }
  submitLoading.value = true
  try {
    const validationPackage = await createEdhrValidationPackage({ ...packageForm })
    ElMessage.success('验证包已创建，当前保持阻塞，需补齐追溯矩阵')
    packageDialogVisible.value = false
    selectedPackage.value = validationPackage
    await getPackageList()
    await handleSelectPackage(validationPackage)
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '验证包创建失败，请检查CSV基础信息、接口和权限。')
  } finally {
    submitLoading.value = false
  }
}

const handleCreateItem = async () => {
  if (!selectedPackage.value) {
    loadError.value = '请先选择验证包。'
    return
  }
  loadError.value = ''
  try {
    await itemFormRef.value?.validate()
  } catch (error) {
    return
  }
  submitLoading.value = true
  try {
    await createEdhrValidationRequirementItem({ ...itemForm, packageId: selectedPackage.value.id })
    ElMessage.success('验证条目已登记')
    itemDialogVisible.value = false
    traceSummary.value = undefined
    await getItemList()
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '验证条目登记失败，请检查条目类型、接口和权限。')
  } finally {
    submitLoading.value = false
  }
}

const handleCreateTraceLink = async () => {
  if (!selectedPackage.value) {
    loadError.value = '请先选择验证包。'
    return
  }
  loadError.value = ''
  try {
    await traceFormRef.value?.validate()
  } catch (error) {
    return
  }
  submitLoading.value = true
  try {
    await createEdhrValidationTraceLink({ ...traceForm, packageId: selectedPackage.value.id })
    ElMessage.success('追溯关系已建立')
    traceDialogVisible.value = false
    traceSummary.value = undefined
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '追溯关系建立失败，请确认来源为URS且目标类型匹配。')
  } finally {
    submitLoading.value = false
  }
}

const handleEvaluateTrace = async () => {
  if (!selectedPackage.value) {
    loadError.value = '请先选择验证包。'
    return
  }
  traceLoading.value = true
  loadError.value = ''
  try {
    traceSummary.value = await evaluateEdhrValidationTrace(selectedPackage.value.id)
    await refreshSelectedPackage()
    if (traceSummary.value.oqReady) {
      ElMessage.success('追溯矩阵完整，可进入OQ Ready准备状态')
    } else {
      loadError.value = traceSummary.value.blockedReason || '追溯矩阵存在断裂，OQ Ready保持阻塞。'
    }
  } catch (error) {
    traceSummary.value = undefined
    loadError.value = resolveErrorMessage(error, '追溯门禁评估失败，请检查验证条目和追溯关系。')
  } finally {
    traceLoading.value = false
  }
}

const resolveItemTypeTag = (itemType: EdhrValidationItemType) => {
  if (itemType === 'URS') return 'primary'
  if (itemType === 'FRS') return 'success'
  if (itemType === 'RISK') return 'danger'
  return 'warning'
}

const resolveLinkType = (targetType?: EdhrValidationItemType) => {
  if (targetType === 'FRS') return 'URS_FRS'
  if (targetType === 'RISK') return 'URS_RISK'
  return 'URS_VERIFICATION'
}

const syncLinkType = () => {
  const target = itemList.value.find((item) => item.id === traceForm.targetItemId)
  traceForm.linkType = resolveLinkType(target?.itemType)
}

onMounted(() => {
  getPackageList()
})
</script>

<style scoped>
.edhr-validation {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-validation__toolbar,
.edhr-validation__packages,
.edhr-validation__csv,
.edhr-validation__items,
.edhr-validation__trace {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-validation__toolbar {
  padding: 16px 16px 8px;
}

.edhr-validation__packages,
.edhr-validation__csv,
.edhr-validation__items,
.edhr-validation__trace {
  padding: 14px;
}

.edhr-validation__title-row,
.edhr-validation__section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.edhr-validation__title-row {
  margin-bottom: 14px;
}

.edhr-validation__title-row h2 {
  margin: 0;
  color: #1f2937;
  font-size: 20px;
  font-weight: 700;
}

.edhr-validation__subtitle,
.edhr-validation__muted {
  color: #64748b;
  font-size: 12px;
}

.edhr-validation__form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 0;
}

.edhr-validation__section-title {
  margin-bottom: 12px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
}

.edhr-validation__detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.9fr);
  gap: 16px;
}

.edhr-validation__strong {
  color: #1f2937;
  font-weight: 600;
}

.edhr-validation__summary {
  margin-bottom: 12px;
}

.edhr-validation :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.edhr-validation :deep(.el-table__row) {
  cursor: pointer;
}

.edhr-validation__dialog-form {
  padding-right: 12px;
}

@media (max-width: 1180px) {
  .edhr-validation__detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .edhr-validation__title-row,
  .edhr-validation__section-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
