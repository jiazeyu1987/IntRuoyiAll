<template>
  <ContentWrap>
    <div class="edhr-dhr-template">
      <el-form :inline="true" :model="queryParams" class="edhr-dhr-template__toolbar">
        <el-form-item label="DHR目录">
          <el-select v-model="queryParams.catalogId" clearable filterable class="!w-220px">
            <el-option
              v-for="catalog in catalogList"
              :key="catalog.id"
              :label="`${catalog.catalogCode} / ${catalog.catalogName}`"
              :value="catalog.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="模板编码">
          <el-input v-model="queryParams.templateCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="模板名称">
          <el-input v-model="queryParams.templateName" clearable class="!w-220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable class="!w-170px">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button
            v-hasPermi="['mes:pro-edhr-dhr-template:create']"
            type="primary"
            plain
            @click="openTemplateDialog"
          >
            新建DHR模板
          </el-button>
          <el-button
            v-hasPermi="['mes:pro-edhr-dhr-template:create']"
            plain
            @click="openCatalogDialog"
          >
            新建DHR目录
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div class="edhr-dhr-template__table">
        <el-table
          v-loading="loading"
          :data="templateList"
          row-key="id"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无DHR模板"
        >
          <el-table-column type="expand" width="48">
            <template #default="{ row }">
              <div class="edhr-dhr-template__expand">
                <div>
                  <div class="edhr-dhr-template__section-title">模板版本</div>
                  <el-table :data="row.versions || []" size="small" border empty-text="暂无模板版本">
                    <el-table-column label="模板版本" prop="versionNo" width="140" />
                    <el-table-column label="变更摘要" prop="changeSummary" min-width="220" />
                    <el-table-column label="创建时间" prop="createTime" width="180" />
                    <el-table-column label="快照" min-width="240">
                      <template #default="scope">
                        <span class="edhr-dhr-template__json">{{ scope.row.templateSnapshotJson || '--' }}</span>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
                <div>
                  <div class="edhr-dhr-template__section-title">绑定对象</div>
                  <el-table :data="row.bindings || []" size="small" border empty-text="暂无绑定对象">
                    <el-table-column label="绑定类型" width="120">
                      <template #default="scope">
                        {{ resolveBindingTypeLabel(scope.row.bindingType) }}
                      </template>
                    </el-table-column>
                    <el-table-column label="绑定编码" prop="bindingObjectCode" min-width="160" />
                    <el-table-column label="绑定名称" prop="bindingObjectName" min-width="160" />
                  </el-table>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="DHR模板" min-width="260">
            <template #default="{ row }">
              <div class="edhr-dhr-template__strong">{{ row.templateCode }} / {{ row.templateName }}</div>
              <div class="edhr-dhr-template__muted">
                DHR目录 {{ resolveCatalogName(row.catalogId) }} · 模板版本 {{ row.currentVersion }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="resolveStatusTagType(row.status)">{{ resolveStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核状态" width="120">
            <template #default="{ row }">
              {{ resolveReviewStatusLabel(row.reviewStatus) }}
            </template>
          </el-table-column>
          <el-table-column label="签核状态" width="120">
            <template #default="{ row }">
              {{ resolveSignoffStatusLabel(row.signoffStatus) }}
            </template>
          </el-table-column>
          <el-table-column label="绑定产品" width="140">
            <template #default="{ row }">{{ resolveBindingCode(row, 'PRODUCT') }}</template>
          </el-table-column>
          <el-table-column label="绑定路线" width="140">
            <template #default="{ row }">{{ resolveBindingCode(row, 'ROUTE') }}</template>
          </el-table-column>
          <el-table-column label="绑定工序" width="140">
            <template #default="{ row }">{{ resolveBindingCode(row, 'PROCESS') }}</template>
          </el-table-column>
          <el-table-column label="批次类型" width="140">
            <template #default="{ row }">{{ resolveBindingCode(row, 'BATCH_TYPE') }}</template>
          </el-table-column>
          <el-table-column label="完整性问题" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="(row.integrityIssueCount || 0) > 0 ? 'danger' : 'success'">
                {{ row.integrityIssueCount || 0 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="签核证据" min-width="220">
            <template #default="{ row }">
              <span class="edhr-dhr-template__hash">{{ row.signoffEvidenceHash || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="360" fixed="right">
            <template #default="{ row }">
              <div class="edhr-dhr-template__row-actions">
                <el-button
                  v-hasPermi="['mes:pro-edhr-dhr-template:check']"
                  link
                  type="primary"
                  @click="runIntegrityCheck(row)"
                >
                  完整性检查
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-dhr-template:approve']"
                  link
                  type="primary"
                  @click="approveTemplate(row)"
                >
                  审核
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-dhr-template:signoff']"
                  link
                  type="primary"
                  @click="openSignoffDialog(row)"
                >
                  签核
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-dhr-template:activate']"
                  link
                  type="success"
                  @click="activateTemplate(row)"
                >
                  生效
                </el-button>
                <el-button link type="primary" @click="openImpactDrawer(row)">影响范围</el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-dhr-template:retire']"
                  link
                  type="warning"
                  @click="openImpactDialog(row, 'RETIRE')"
                >
                  停用
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-dhr-template:void']"
                  link
                  type="danger"
                  @click="openImpactDialog(row, 'VOID')"
                >
                  作废
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="templateTotal"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="loadTemplateList"
        />
      </div>
    </div>

    <Dialog title="新建DHR目录" v-model="catalogDialogVisible" width="560px">
      <el-form ref="catalogFormRef" :model="catalogForm" :rules="catalogRules" label-width="96px">
        <el-form-item label="目录编码" prop="catalogCode">
          <el-input v-model="catalogForm.catalogCode" maxlength="64" />
        </el-form-item>
        <el-form-item label="目录名称" prop="catalogName">
          <el-input v-model="catalogForm.catalogName" maxlength="255" />
        </el-form-item>
        <el-form-item label="上级目录">
          <el-select v-model="catalogForm.parentCatalogId" clearable filterable class="!w-100%">
            <el-option
              v-for="catalog in catalogList"
              :key="catalog.id"
              :label="`${catalog.catalogCode} / ${catalog.catalogName}`"
              :value="catalog.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="catalogForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catalogDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="catalogSaving" @click="submitCatalog">保存目录</el-button>
      </template>
    </Dialog>

    <Dialog title="新建DHR模板" v-model="templateDialogVisible" width="760px">
      <el-alert v-if="templateError" :title="templateError" type="error" :closable="false" show-icon />
      <el-form
        ref="templateFormRef"
        :model="templateForm"
        :rules="templateRules"
        label-width="112px"
        class="mt-12px"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="DHR目录" prop="catalogId">
              <el-select v-model="templateForm.catalogId" filterable class="!w-100%">
                <el-option
                  v-for="catalog in catalogList"
                  :key="catalog.id"
                  :label="`${catalog.catalogCode} / ${catalog.catalogName}`"
                  :value="catalog.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板版本" prop="currentVersion">
              <el-input v-model="templateForm.currentVersion" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="templateForm.templateCode" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="templateForm.templateName" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绑定产品" prop="productCode">
              <el-input v-model="templateForm.productCode" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绑定路线" prop="routeCode">
              <el-input v-model="templateForm.routeCode" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绑定工序" prop="processCode">
              <el-input v-model="templateForm.processCode" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="批次类型" prop="batchType">
              <el-input v-model="templateForm.batchType" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="模板快照" prop="templateSnapshotJson">
              <el-input v-model="templateForm.templateSnapshotJson" type="textarea" :rows="6" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="templateForm.remark" type="textarea" :rows="2" maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="templateSaving" @click="submitTemplate">保存模板</el-button>
      </template>
    </Dialog>

    <Dialog title="DHR模板签核" v-model="signoffDialogVisible" width="560px">
      <el-form ref="signoffFormRef" :model="signoffForm" :rules="signoffRules" label-width="128px">
        <el-form-item label="DHR模板">
          <el-input :model-value="currentTemplateLabel" disabled />
        </el-form-item>
        <el-form-item label="签核证据Hash" prop="signoffEvidenceHash">
          <el-input v-model="signoffForm.signoffEvidenceHash" maxlength="128" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="signoffDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="signoffTemplate">确认签核</el-button>
      </template>
    </Dialog>

    <Dialog :title="impactDialogTitle" v-model="impactDialogVisible" width="640px">
      <el-alert v-if="impactError" :title="impactError" type="error" :closable="false" show-icon />
      <el-form ref="impactFormRef" :model="impactForm" :rules="impactRules" label-width="112px" class="mt-12px">
        <el-form-item label="DHR模板">
          <el-input :model-value="currentTemplateLabel" disabled />
        </el-form-item>
        <el-form-item label="影响范围" prop="impactScopeJson">
          <el-input v-model="impactForm.impactScopeJson" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="确认影响" prop="impactConfirmed">
          <el-checkbox v-model="impactForm.impactConfirmed">已确认影响范围</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="impactDialogVisible = false">取消</el-button>
        <el-button
          :type="impactMode === 'VOID' ? 'danger' : 'warning'"
          :loading="actionLoading"
          @click="impactMode === 'VOID' ? submitVoidTemplate() : submitRetireTemplate()"
        >
          {{ impactMode === 'VOID' ? '确认作废' : '确认停用' }}
        </el-button>
      </template>
    </Dialog>

    <el-drawer v-model="impactDrawerVisible" title="影响范围记录" size="60%">
      <el-alert v-if="impactError" :title="impactError" type="error" :closable="false" show-icon />
      <div class="edhr-dhr-template__drawer-head">
        <div>
          <div class="edhr-dhr-template__strong">{{ currentTemplateLabel }}</div>
          <div class="edhr-dhr-template__muted">影响范围记录来自后端停用/作废操作</div>
        </div>
        <el-button :loading="impactLoading" @click="loadImpactList">刷新</el-button>
      </div>
      <el-table
        v-loading="impactLoading"
        :data="impactList"
        stripe
        :show-overflow-tooltip="true"
        empty-text="暂无影响范围记录"
      >
        <el-table-column label="动作" width="100">
          <template #default="{ row }">
            {{ row.actionType === 'VOID' ? '作废' : '停用' }}
          </template>
        </el-table-column>
        <el-table-column label="确认影响" width="100">
          <template #default="{ row }">
            <el-tag :type="row.impactConfirmed ? 'success' : 'danger'">
              {{ row.impactConfirmed ? '已确认' : '未确认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="影响范围" prop="impactScopeJson" min-width="320" />
        <el-table-column label="确认人" prop="confirmedBy" width="120" />
        <el-table-column label="确认时间" prop="confirmedAt" width="180" />
      </el-table>
      <Pagination
        :total="impactTotal"
        v-model:page="impactQuery.pageNo"
        v-model:limit="impactQuery.pageSize"
        @pagination="loadImpactList"
      />
    </el-drawer>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  EdhrDhrTemplateApi,
  approveTemplate as approveTemplateApi,
  activateTemplate as activateTemplateApi,
  createCatalog,
  createTemplate,
  getCatalogPage,
  getImpactPage,
  getTemplatePage,
  retireTemplate,
  runIntegrityCheck as runIntegrityCheckApi,
  signoffTemplate as signoffTemplateApi,
  voidTemplate,
  type EdhrDhrCatalogCreateReqVO,
  type EdhrDhrCatalogRespVO,
  type EdhrDhrTemplateCreateReqVO,
  type EdhrDhrTemplateImpactRespVO,
  type EdhrDhrTemplateRespVO,
  type EdhrDhrTemplateStatus
} from '@/api/mes/pro/edhr/dhrTemplate'

defineOptions({ name: 'MesProFeedbackEdhrDhrTemplate' })

const message = useMessage()

const loading = ref(false)
const actionLoading = ref(false)
const catalogSaving = ref(false)
const templateSaving = ref(false)
const impactLoading = ref(false)
const loadError = ref('')
const templateError = ref('')
const impactError = ref('')
const catalogList = ref<EdhrDhrCatalogRespVO[]>([])
const templateList = ref<EdhrDhrTemplateRespVO[]>([])
const impactList = ref<EdhrDhrTemplateImpactRespVO[]>([])
const templateTotal = ref(0)
const impactTotal = ref(0)

const catalogDialogVisible = ref(false)
const templateDialogVisible = ref(false)
const signoffDialogVisible = ref(false)
const impactDialogVisible = ref(false)
const impactDrawerVisible = ref(false)
const catalogFormRef = ref()
const templateFormRef = ref()
const signoffFormRef = ref()
const impactFormRef = ref()
const currentTemplate = ref<EdhrDhrTemplateRespVO>()
const impactMode = ref<'RETIRE' | 'VOID'>('RETIRE')

const statusOptions: Array<{ label: string; value: EdhrDhrTemplateStatus }> = [
  { label: '草稿', value: 'DRAFT' },
  { label: '预检失败', value: 'PRECHECK_FAILED' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已审核', value: 'APPROVED' },
  { label: '待生效', value: 'SIGNOFF_PENDING' },
  { label: '已生效', value: 'EFFECTIVE' },
  { label: '已暂停', value: 'SUSPENDED' },
  { label: '已停用', value: 'RETIRED' },
  { label: '已作废', value: 'OBSOLETE' }
]

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  catalogId: undefined as number | undefined,
  templateCode: '',
  templateName: '',
  status: undefined as EdhrDhrTemplateStatus | undefined
})

const catalogForm = reactive<EdhrDhrCatalogCreateReqVO>({
  catalogCode: '',
  catalogName: '',
  parentCatalogId: undefined,
  remark: ''
})

const templateForm = reactive<EdhrDhrTemplateCreateReqVO>({
  catalogId: undefined as unknown as number,
  templateCode: '',
  templateName: '',
  currentVersion: '',
  templateSnapshotJson: '{"sections":[]}',
  productCode: '',
  routeCode: '',
  processCode: '',
  batchType: '',
  remark: ''
})

const signoffForm = reactive({
  signoffEvidenceHash: ''
})

const impactForm = reactive({
  impactScopeJson: '',
  impactConfirmed: false
})

const impactQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  templateId: undefined as number | undefined
})

const catalogRules = {
  catalogCode: [{ required: true, message: '目录编码不能为空', trigger: 'blur' }],
  catalogName: [{ required: true, message: '目录名称不能为空', trigger: 'blur' }]
}

const templateRules = {
  catalogId: [{ required: true, message: 'DHR目录不能为空', trigger: 'change' }],
  templateCode: [{ required: true, message: '模板编码不能为空', trigger: 'blur' }],
  templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
  currentVersion: [{ required: true, message: '模板版本不能为空', trigger: 'blur' }],
  productCode: [{ required: true, message: '绑定产品不能为空', trigger: 'blur' }],
  routeCode: [{ required: true, message: '绑定路线不能为空', trigger: 'blur' }],
  processCode: [{ required: true, message: '绑定工序不能为空', trigger: 'blur' }],
  batchType: [{ required: true, message: '批次类型不能为空', trigger: 'blur' }],
  templateSnapshotJson: [{ required: true, message: '模板快照不能为空', trigger: 'blur' }]
}

const signoffRules = {
  signoffEvidenceHash: [{ required: true, message: '签核证据Hash不能为空', trigger: 'blur' }]
}

const impactRules = {
  impactScopeJson: [{ required: true, message: '影响范围不能为空', trigger: 'blur' }],
  impactConfirmed: [
    {
      validator: (_rule: unknown, value: boolean, callback: (error?: Error) => void) => {
        if (value) {
          callback()
          return
        }
        callback(new Error('必须确认影响范围'))
      },
      trigger: 'change'
    }
  ]
}

const currentTemplateLabel = computed(() => {
  if (!currentTemplate.value) return '--'
  return `${currentTemplate.value.templateCode} / ${currentTemplate.value.templateName}`
})

const impactDialogTitle = computed(() => {
  return impactMode.value === 'VOID' ? '作废DHR模板' : '停用DHR模板'
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const buildTemplateQuery = () => ({
  ...queryParams,
  templateCode: queryParams.templateCode.trim() || undefined,
  templateName: queryParams.templateName.trim() || undefined
})

const loadCatalogList = async () => {
  try {
    const data = await getCatalogPage({ pageNo: 1, pageSize: 200 })
    catalogList.value = data.list || []
  } catch (error) {
    loadError.value = resolveErrorMessage(error, 'DHR目录加载失败，请检查接口和权限。')
    message.error(resolveErrorMessage(error, 'DHR目录加载失败，请检查接口和权限。'))
  }
}

const loadTemplateList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getTemplatePage(buildTemplateQuery())
    templateList.value = data.list || []
    templateTotal.value = data.total || 0
  } catch (error) {
    templateList.value = []
    templateTotal.value = 0
    loadError.value = resolveErrorMessage(error, 'DHR模板加载失败，请检查接口、权限和菜单绑定。')
  } finally {
    loading.value = false
  }
}

const loadImpactList = async () => {
  if (!impactQuery.templateId) {
    impactList.value = []
    impactTotal.value = 0
    return
  }
  impactLoading.value = true
  impactError.value = ''
  try {
    const data = await getImpactPage(impactQuery)
    impactList.value = data.list || []
    impactTotal.value = data.total || 0
  } catch (error) {
    impactList.value = []
    impactTotal.value = 0
    impactError.value = resolveErrorMessage(error, '影响范围记录加载失败，请检查接口和权限。')
    message.error(resolveErrorMessage(error, '影响范围记录加载失败，请检查接口和权限。'))
  } finally {
    impactLoading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  loadTemplateList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.catalogId = undefined
  queryParams.templateCode = ''
  queryParams.templateName = ''
  queryParams.status = undefined
  loadTemplateList()
}

const openCatalogDialog = () => {
  catalogForm.catalogCode = ''
  catalogForm.catalogName = ''
  catalogForm.parentCatalogId = undefined
  catalogForm.remark = ''
  catalogDialogVisible.value = true
}

const submitCatalog = async () => {
  await catalogFormRef.value?.validate()
  catalogSaving.value = true
  try {
    await createCatalog({
      catalogCode: catalogForm.catalogCode.trim(),
      catalogName: catalogForm.catalogName.trim(),
      parentCatalogId: catalogForm.parentCatalogId,
      remark: catalogForm.remark?.trim() || undefined
    })
    catalogDialogVisible.value = false
    message.success('DHR目录保存成功')
    await loadCatalogList()
  } catch (error) {
    message.error(resolveErrorMessage(error, 'DHR目录保存失败，请检查编码唯一性和权限。'))
  } finally {
    catalogSaving.value = false
  }
}

const openTemplateDialog = () => {
  templateError.value = ''
  templateForm.catalogId = queryParams.catalogId || (catalogList.value[0]?.id as number)
  templateForm.templateCode = ''
  templateForm.templateName = ''
  templateForm.currentVersion = ''
  templateForm.templateSnapshotJson = '{"sections":[]}'
  templateForm.productCode = ''
  templateForm.routeCode = ''
  templateForm.processCode = ''
  templateForm.batchType = ''
  templateForm.remark = ''
  templateDialogVisible.value = true
}

const submitTemplate = async () => {
  await templateFormRef.value?.validate()
  templateSaving.value = true
  templateError.value = ''
  try {
    await createTemplate({
      catalogId: templateForm.catalogId,
      templateCode: templateForm.templateCode.trim(),
      templateName: templateForm.templateName.trim(),
      currentVersion: templateForm.currentVersion.trim(),
      templateSnapshotJson: templateForm.templateSnapshotJson.trim(),
      productCode: templateForm.productCode?.trim() || undefined,
      routeCode: templateForm.routeCode?.trim() || undefined,
      processCode: templateForm.processCode?.trim() || undefined,
      batchType: templateForm.batchType?.trim() || undefined,
      remark: templateForm.remark?.trim() || undefined
    })
    templateDialogVisible.value = false
    message.success('DHR模板保存成功')
    await loadTemplateList()
  } catch (error) {
    templateError.value = resolveErrorMessage(error, 'DHR模板保存失败，请检查绑定、版本和权限。')
    message.error(resolveErrorMessage(error, 'DHR模板保存失败，请检查绑定、版本和权限。'))
  } finally {
    templateSaving.value = false
  }
}

const runIntegrityCheck = async (row: EdhrDhrTemplateRespVO) => {
  actionLoading.value = true
  try {
    const result = await runIntegrityCheckApi({ id: row.id })
    if ((result.integrityIssueCount || 0) > 0) {
      message.warning(`完整性检查完成，发现 ${result.integrityIssueCount} 个问题。`)
    } else {
      message.success('完整性检查通过')
    }
    await loadTemplateList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '完整性检查失败，请查看后端错误信息。'))
  } finally {
    actionLoading.value = false
  }
}

const approveTemplate = async (row: EdhrDhrTemplateRespVO) => {
  actionLoading.value = true
  try {
    await approveTemplateApi({ id: row.id })
    message.success('DHR模板审核通过')
    await loadTemplateList()
  } catch (error) {
    message.error(resolveErrorMessage(error, 'DHR模板审核失败，请先完成完整性检查。'))
  } finally {
    actionLoading.value = false
  }
}

const openSignoffDialog = (row: EdhrDhrTemplateRespVO) => {
  currentTemplate.value = row
  signoffForm.signoffEvidenceHash = row.signoffEvidenceHash || ''
  signoffDialogVisible.value = true
}

const signoffTemplate = async () => {
  await signoffFormRef.value?.validate()
  if (!currentTemplate.value) return
  actionLoading.value = true
  try {
    await signoffTemplateApi({
      id: currentTemplate.value.id,
      signoffEvidenceHash: signoffForm.signoffEvidenceHash.trim()
    })
    signoffDialogVisible.value = false
    message.success('DHR模板签核完成')
    await loadTemplateList()
  } catch (error) {
    message.error(resolveErrorMessage(error, 'DHR模板签核失败，请检查审核状态和签核证据。'))
  } finally {
    actionLoading.value = false
  }
}

const activateTemplate = async (row: EdhrDhrTemplateRespVO) => {
  actionLoading.value = true
  try {
    await activateTemplateApi({ id: row.id })
    message.success('DHR模板已生效')
    await loadTemplateList()
  } catch (error) {
    message.error(resolveErrorMessage(error, 'DHR模板生效失败，请检查完整性、审核和签核状态。'))
  } finally {
    actionLoading.value = false
  }
}

const openImpactDialog = (row: EdhrDhrTemplateRespVO, mode: 'RETIRE' | 'VOID') => {
  currentTemplate.value = row
  impactMode.value = mode
  impactError.value = ''
  impactForm.impactScopeJson = ''
  impactForm.impactConfirmed = false
  impactDialogVisible.value = true
}

const submitRetireTemplate = async () => {
  await submitImpact(retireTemplate, 'DHR模板已停用')
}

const submitVoidTemplate = async () => {
  await submitImpact(voidTemplate, 'DHR模板已作废')
}

const submitImpact = async (
  action: typeof EdhrDhrTemplateApi.retireTemplate,
  successMessage: string
) => {
  await impactFormRef.value?.validate()
  if (!currentTemplate.value) return
  actionLoading.value = true
  impactError.value = ''
  try {
    await action({
      id: currentTemplate.value.id,
      impactScopeJson: impactForm.impactScopeJson.trim(),
      impactConfirmed: impactForm.impactConfirmed
    })
    impactDialogVisible.value = false
    message.success(successMessage)
    await loadTemplateList()
  } catch (error) {
    impactError.value = resolveErrorMessage(error, '影响范围确认失败，请检查模板状态和权限。')
    message.error(resolveErrorMessage(error, '影响范围确认失败，请检查模板状态和权限。'))
  } finally {
    actionLoading.value = false
  }
}

const openImpactDrawer = async (row: EdhrDhrTemplateRespVO) => {
  currentTemplate.value = row
  impactQuery.pageNo = 1
  impactQuery.templateId = row.id
  impactDrawerVisible.value = true
  await loadImpactList()
}

const resolveCatalogName = (catalogId?: number) => {
  const catalog = catalogList.value.find((item) => item.id === catalogId)
  return catalog ? `${catalog.catalogCode}` : catalogId || '--'
}

const resolveBindingCode = (row: EdhrDhrTemplateRespVO, bindingType: string) => {
  const binding = row.bindings?.find((item) => item.bindingType === bindingType)
  return binding?.bindingObjectCode || '--'
}

const resolveBindingTypeLabel = (bindingType?: string) => {
  if (bindingType === 'PRODUCT') return '绑定产品'
  if (bindingType === 'ROUTE') return '绑定路线'
  if (bindingType === 'PROCESS') return '绑定工序'
  if (bindingType === 'BATCH_TYPE') return '批次类型'
  return bindingType || '--'
}

const resolveStatusLabel = (status?: EdhrDhrTemplateStatus) => {
  return statusOptions.find((item) => item.value === status)?.label || status || '--'
}

const resolveStatusTagType = (status?: EdhrDhrTemplateStatus) => {
  if (status === 'PRECHECK_FAILED' || status === 'OBSOLETE') return 'danger'
  if (status === 'EFFECTIVE') return 'success'
  if (status === 'PENDING_REVIEW' || status === 'SIGNOFF_PENDING') return 'warning'
  if (status === 'RETIRED' || status === 'SUSPENDED') return 'info'
  return ''
}

const resolveReviewStatusLabel = (status?: string) => {
  if (status === 'APPROVED') return '已审核'
  if (status === 'REJECTED') return '已驳回'
  return '未提交'
}

const resolveSignoffStatusLabel = (status?: string) => {
  if (status === 'SIGNED') return '已签核'
  return '未签核'
}

onMounted(async () => {
  await loadCatalogList()
  await loadTemplateList()
})
</script>

<style scoped>
.edhr-dhr-template__toolbar,
.edhr-dhr-template__table {
  padding: 16px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-dhr-template__toolbar {
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  padding-bottom: 0;
}

.edhr-dhr-template__table {
  border-top: 0;
  border-radius: 0 0 8px 8px;
}

.edhr-dhr-template__table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-dhr-template__table :deep(.el-table__row) {
  min-height: 52px;
}

.edhr-dhr-template__expand {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  padding: 12px 24px;
  background: #f8fafc;
}

.edhr-dhr-template__section-title {
  margin-bottom: 8px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-dhr-template__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-dhr-template__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-dhr-template__hash,
.edhr-dhr-template__json {
  color: #263247;
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.edhr-dhr-template__row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.edhr-dhr-template__row-actions :deep(.el-button) {
  min-height: auto;
  padding: 0;
}

.edhr-dhr-template__drawer-head {
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

@media (max-width: 960px) {
  .edhr-dhr-template__expand {
    grid-template-columns: 1fr;
  }
}
</style>
