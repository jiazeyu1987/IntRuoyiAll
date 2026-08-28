<template>
  <ContentWrap data-testid="registration-certificate-read-page">
    <el-tabs
      v-model="activeTab"
      data-testid="registration-certificate-tabs"
      @tab-change="handleTabChange"
    >
      <el-tab-pane name="current" label="注册证">
        <div data-testid="registration-certificate-current-tab">
          <UnifiedListTemplate
            table-key="dcc.registrationCertificate.current"
            :query-model="queryParams"
            label-width="82px"
            query-form-test-id="registration-certificate-current-filter-form"
            :filter-definitions="currentQuickFilterDefinitions"
            :quick-filter-state="currentQuickFilter.state"
            :selected-filter-definition="currentQuickFilter.selectedDefinition.value"
            :operator-options="currentQuickFilter.operatorOptions.value"
            :columns="currentColumns"
            :column-saving="currentColumnSaving"
            :show-column-reset="false"
            :total="total"
            v-model:page="queryParams.pageNo"
            v-model:limit="queryParams.pageSize"
            @update:quick-filter-state="currentQuickFilter.updateState"
            @quick-filter-query="currentQuickFilter.applyQuickFilter"
            @column-change="saveCurrentColumnConfig"
            @pagination="loadPage"
          >
            <template #actions>
              <el-button v-hasPermi="['dcc:registration-certificate:upload:create']" type="success" @click="openUploadDialog">
                <Icon icon="ep:upload" class="mr-5px" />上传注册证
              </el-button>
            </template>

            <template #table>
              <el-table
                v-loading="loading"
                data-user-table-column-explicit
                data-user-table-key="dcc.registrationCertificate.current"
                :data="list"
                border
                :stripe="true"
                :show-overflow-tooltip="true"
                row-key="certificateId"
                @header-dragend="handleCurrentHeaderDragend"
              >
                <el-table-column
                  v-if="isCurrentColumnVisible('certificateNo')"
                  label="注册证编号"
                  prop="certificateNo"
                  :min-width="getCurrentColumnMinWidthString('certificateNo', 180)"
                  :width="getCurrentColumnWidthString('certificateNo')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('ownerCompanyName')"
                  label="所属公司"
                  prop="ownerCompanyName"
                  :min-width="getCurrentColumnMinWidthString('ownerCompanyName', 180)"
                  :width="getCurrentColumnWidthString('ownerCompanyName')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('productName')"
                  label="产品"
                  prop="productName"
                  :min-width="getCurrentColumnMinWidthString('productName', 180)"
                  :width="getCurrentColumnWidthString('productName')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('projectCode')"
                  label="实际项目代码"
                  prop="projectCode"
                  :min-width="getCurrentColumnMinWidthString('projectCode', 150)"
                  :width="getCurrentColumnWidthString('projectCode')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('versionNo')"
                  label="版本"
                  prop="versionNo"
                  align="center"
                  :width="getCurrentColumnWidthString('versionNo', 90)"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('status')"
                  label="状态"
                  prop="status"
                  align="center"
                  :width="getCurrentColumnWidthString('status', 130)"
                >
                  <template #default="{ row }">
                    <el-tag :type="getRegistrationCertificateStatusTagType(row.status)">
                      {{ formatRegistrationCertificateStatus(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCurrentColumnVisible('reminder')"
                  label="提醒状态"
                  prop="visualState"
                  width="120"
                >
                  <template #default="{ row }">
                    <el-tag :type="getRegistrationCertificateReminderTagType(row.reminderColor)">
                      {{ formatRegistrationCertificateReminder(row.visualState) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCurrentColumnVisible('hasProjectCode')"
                  label="项目代码"
                  align="center"
                  :width="getCurrentColumnWidthString('hasProjectCode', 110)"
                >
                  <template #default="{ row }">
                    <el-tag :type="getMissingMarkerTagType(row.hasProjectCode)">
                      {{ formatMissingMarker(row.hasProjectCode) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCurrentColumnVisible('hasRegistrationFile')"
                  label="注册证文件"
                  align="center"
                  :width="getCurrentColumnWidthString('hasRegistrationFile', 120)"
                >
                  <template #default="{ row }">
                    <el-tag :type="getMissingMarkerTagType(row.hasRegistrationFile)">
                      {{ formatMissingMarker(row.hasRegistrationFile) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCurrentColumnVisible('approvalDate')"
                  label="批准日"
                  prop="approvalDate"
                  :width="getCurrentColumnWidthString('approvalDate', 120)"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('effectiveDate')"
                  label="生效日"
                  prop="effectiveDate"
                  :width="getCurrentColumnWidthString('effectiveDate', 120)"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('expiryDate')"
                  label="有效期至"
                  prop="expiryDate"
                  :width="getCurrentColumnWidthString('expiryDate', 120)"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('remark')"
                  label="备注"
                  prop="remark"
                  :min-width="getCurrentColumnMinWidthString('remark', 220)"
                  :width="getCurrentColumnWidthString('remark')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('actions')"
                  label="操作"
                  align="center"
                  fixed="right"
                  :width="getCurrentColumnWidthString('actions', 260)"
                >
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openDetail(row.certificateId)">
                      详情
                    </el-button>
                    <el-button
                      link
                      type="primary"
                      v-hasPermi="['dcc:registration-certificate:renewal:upload']"
                      @click="openRenewalDialog(row)"
                    >
                      延续
                    </el-button>
                    <el-button link type="primary" @click="openLinkedProductManagement(row.productMasterId)">
                      产品
                    </el-button>
                    <el-button
                      v-if="row.projectCodeId"
                      link
                      type="primary"
                      @click="openLinkedProjectCodeManagement(row.projectCodeId)"
                    >
                      项目代码
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </div>
      </el-tab-pane>

      <el-tab-pane name="old" label="老证">
        <div data-testid="registration-certificate-old-index">
          <UnifiedListTemplate
            table-key="dcc.registrationCertificate.old"
            :query-model="oldQueryParams"
            label-width="82px"
            query-form-test-id="registration-certificate-old-filter-form"
            :filter-definitions="oldQuickFilterDefinitions"
            :quick-filter-state="oldQuickFilter.state"
            :selected-filter-definition="oldQuickFilter.selectedDefinition.value"
            :operator-options="oldQuickFilter.operatorOptions.value"
            :columns="oldColumns"
            :column-saving="oldColumnSaving"
            :show-column-reset="false"
            :total="oldTotal"
            v-model:page="oldQueryParams.pageNo"
            v-model:limit="oldQueryParams.pageSize"
            @update:quick-filter-state="oldQuickFilter.updateState"
            @quick-filter-query="oldQuickFilter.applyQuickFilter"
            @column-change="saveOldColumnConfig"
            @pagination="loadOldIndexPage"
          >
            <template #table>
              <el-table
                v-loading="oldLoading"
                data-user-table-column-explicit
                data-user-table-key="dcc.registrationCertificate.old"
                :data="oldList"
                border
                :stripe="true"
                :show-overflow-tooltip="true"
                row-key="versionId"
                @header-dragend="handleOldHeaderDragend"
              >
                <el-table-column
                  v-if="isOldColumnVisible('certificateNo')"
                  label="注册证编号"
                  prop="certificateNo"
                  :min-width="getOldColumnMinWidthString('certificateNo', 180)"
                  :width="getOldColumnWidthString('certificateNo')"
                />
                <el-table-column
                  v-if="isOldColumnVisible('ownerCompanyName')"
                  label="所属公司"
                  prop="ownerCompanyName"
                  :min-width="getOldColumnMinWidthString('ownerCompanyName', 180)"
                  :width="getOldColumnWidthString('ownerCompanyName')"
                />
                <el-table-column
                  v-if="isOldColumnVisible('productName')"
                  label="产品"
                  prop="productName"
                  :min-width="getOldColumnMinWidthString('productName', 180)"
                  :width="getOldColumnWidthString('productName')"
                />
                <el-table-column
                  v-if="isOldColumnVisible('versionNo')"
                  label="版本"
                  prop="versionNo"
                  align="center"
                  :width="getOldColumnWidthString('versionNo', 90)"
                />
                <el-table-column
                  v-if="isOldColumnVisible('status')"
                  label="状态"
                  prop="status"
                  align="center"
                  :width="getOldColumnWidthString('status', 130)"
                >
                  <template #default="{ row }">
                    <el-tag :type="getRegistrationCertificateStatusTagType(row.status)">
                      {{ row.status === 'OLD' ? '已失效，失效日期 ' + (row.expiryDate || '—') : formatRegistrationCertificateStatus(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isOldColumnVisible('expiryDate')"
                  label="原有效期至"
                  prop="expiryDate"
                  :width="getOldColumnWidthString('expiryDate', 140)"
                />
                <el-table-column
                  v-if="isOldColumnVisible('actions')"
                  label="操作"
                  align="center"
                  fixed="right"
                  :width="getOldColumnWidthString('actions', 260)"
                >
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openOldDetail(row.certificateId)">
                      详情
                    </el-button>
                    <el-button link type="warning" @click="openOldAccessRequest(row.certificateId)">
                      申请查看
                    </el-button>
                    <el-button link type="primary" @click="openLinkedProductManagement(row.productMasterId)">
                      产品
                    </el-button>
                    <el-button
                      v-if="row.projectCodeId"
                      link
                      type="primary"
                      @click="openLinkedProjectCodeManagement(row.projectCodeId)"
                    >
                      项目代码
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </div>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <RegistrationCertificateUploadDialog
    data-testid="registration-certificate-upload-dialog"
    v-model="showUploadDialog"
    @saved="handleUploadSaved"
  />
  <RegistrationCertificateRenewalDialog
    v-model="showRenewalDialog"
    :certificate="selectedRenewalCertificate"
    @saved="handleRenewalSaved"
  />
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getRegistrationCertificateOldIndexPage,
  getRegistrationCertificatePage,
  type DccRegistrationCertificateOldIndexItemVO,
  type DccRegistrationCertificatePageItemVO,
  type DccRegistrationCertificatePageReqVO
} from '@/api/dcc/registrationCertificate'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import RegistrationCertificateUploadDialog from '../upload/UploadDialog.vue'
import RegistrationCertificateRenewalDialog from '../renewal/RenewalDialog.vue'
import {
  REGISTRATION_CERTIFICATE_STATUS_OPTIONS,
  formatMissingMarker,
  formatRegistrationCertificateReminder,
  formatRegistrationCertificateStatus,
  getMissingMarkerTagType,
  getRegistrationCertificateReminderTagType,
  getRegistrationCertificateStatusTagType
} from '../shared/state'

defineOptions({ name: 'DccRegistrationCertificateIndex' })

const router = useRouter()
const route = useRoute()
const REGISTRATION_CERTIFICATE_ROUTE_PATH = '/mdm/registration-certificate'

const isRegistrationCertificateRoute = () => route.path === REGISTRATION_CERTIFICATE_ROUTE_PATH

const activeTab = ref<'current' | 'old'>('current')
const loading = ref(false)
const oldLoading = ref(false)
const list = ref<DccRegistrationCertificatePageItemVO[]>([])
const oldList = ref<DccRegistrationCertificateOldIndexItemVO[]>([])
const total = ref(0)
const oldTotal = ref(0)
const showUploadDialog = ref(false)
const showRenewalDialog = ref(false)
const selectedRenewalCertificate = ref<DccRegistrationCertificatePageItemVO>()

type RegistrationCertificatePageQuery = DccRegistrationCertificatePageReqVO &
  Required<Pick<PageParam, 'pageNo' | 'pageSize'>>

const queryParams = reactive<RegistrationCertificatePageQuery>({ pageNo: 1, pageSize: 10 })
const oldQueryParams = reactive<RegistrationCertificatePageQuery>({ pageNo: 1, pageSize: 10 })

const currentColumnDefinitions: UserTableColumnDefinition[] = [
  { key: 'certificateNo', label: '注册证编号', minWidth: 180, sortable: false },
  { key: 'ownerCompanyName', label: '所属公司', minWidth: 180, sortable: false },
  { key: 'productName', label: '产品', minWidth: 180, sortable: false },
  { key: 'projectCode', label: '实际项目代码', minWidth: 150, sortable: false },
  { key: 'versionNo', label: '版本', width: 90, sortable: false },
  { key: 'status', label: '状态', width: 130, sortable: false },
  { key: 'reminder', label: '提醒状态', width: 120, sortable: false },
  { key: 'hasProjectCode', label: '项目代码', width: 110, sortable: false },
  { key: 'hasRegistrationFile', label: '注册证文件', width: 120, sortable: false },
  { key: 'approvalDate', label: '批准日', width: 120, sortable: false },
  { key: 'effectiveDate', label: '生效日', width: 120, sortable: false },
  { key: 'expiryDate', label: '有效期至', width: 120, sortable: false },
  { key: 'remark', label: '备注', minWidth: 220, sortable: false },
  { key: 'actions', label: '操作', width: 330, hideable: false, business: false, sortable: false }
]

const oldColumnDefinitions: UserTableColumnDefinition[] = [
  { key: 'certificateNo', label: '注册证编号', minWidth: 180, sortable: false },
  { key: 'ownerCompanyName', label: '所属公司', minWidth: 180, sortable: false },
  { key: 'productName', label: '产品', minWidth: 180, sortable: false },
  { key: 'versionNo', label: '版本', width: 90, sortable: false },
  { key: 'status', label: '状态', width: 130, sortable: false },
  { key: 'expiryDate', label: '原有效期至', width: 140, sortable: false },
  { key: 'actions', label: '操作', width: 260, hideable: false, business: false, sortable: false }
]

const {
  columns: currentColumns,
  saving: currentColumnSaving,
  isColumnVisible: isCurrentColumnVisible,
  getColumnWidthString: getCurrentColumnWidthString,
  getColumnMinWidthString: getCurrentColumnMinWidthString,
  handleHeaderDragend: handleCurrentHeaderDragend,
  saveConfig: saveCurrentColumnConfig
} = useUserTableColumns('dcc.registrationCertificate.current', currentColumnDefinitions)

const {
  columns: oldColumns,
  saving: oldColumnSaving,
  isColumnVisible: isOldColumnVisible,
  getColumnWidthString: getOldColumnWidthString,
  getColumnMinWidthString: getOldColumnMinWidthString,
  handleHeaderDragend: handleOldHeaderDragend,
  saveConfig: saveOldColumnConfig
} = useUserTableColumns('dcc.registrationCertificate.old', oldColumnDefinitions)

const currentQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'certificateNo',
    label: '注册证编号',
    type: 'text',
    queryParamKey: 'certificateNo',
    placeholder: '输入注册证编号'
  },
  {
    key: 'ownerCompanyName',
    label: '所属公司',
    type: 'text',
    queryParamKey: 'ownerCompanyName',
    placeholder: '输入所属公司'
  },
  {
    key: 'productName',
    label: '产品名称',
    type: 'text',
    queryParamKey: 'productName',
    placeholder: '输入产品名称'
  },
  {
    key: 'classification',
    label: '分类',
    type: 'text',
    queryParamKey: 'classification',
    placeholder: '输入分类'
  },
  {
    key: 'registrantName',
    label: '注册人',
    type: 'text',
    queryParamKey: 'registrantName',
    placeholder: '输入注册人'
  },
  {
    key: 'modelSpecification',
    label: '型号规格',
    type: 'text',
    queryParamKey: 'modelSpecification',
    placeholder: '输入型号规格'
  },
  {
    key: 'productionAddress',
    label: '生产地址',
    type: 'text',
    queryParamKey: 'productionAddress',
    placeholder: '输入生产地址'
  },
  {
    key: 'entrustedEnterpriseName',
    label: '受托企业',
    type: 'text',
    queryParamKey: 'entrustedEnterpriseName',
    placeholder: '输入受托企业'
  },
  {
    key: 'projectCode',
    label: '实际项目代码',
    type: 'text',
    queryParamKey: 'projectCode',
    placeholder: '输入实际项目代码'
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: REGISTRATION_CERTIFICATE_STATUS_OPTIONS
  },
  {
    key: 'missingProjectCode',
    label: '项目代码',
    type: 'select',
    queryParamKey: 'missingProjectCode',
    options: [
      { label: '已提供', value: false },
      { label: '缺失', value: true }
    ]
  },
  {
    key: 'missingFile',
    label: '注册证文件',
    type: 'select',
    queryParamKey: 'missingFile',
    options: [
      { label: '已提供', value: false },
      { label: '缺失', value: true }
    ]
  },
  {
    key: 'firstObtainedStart', label: '首次获证起始', type: 'date', queryParamKey: 'firstObtainedStart'
  },
  {
    key: 'firstObtainedEnd', label: '首次获证截止', type: 'date', queryParamKey: 'firstObtainedEnd'
  },
  {
    key: 'approvalStart', label: '批准日期起始', type: 'date', queryParamKey: 'approvalStart'
  },
  {
    key: 'approvalEnd', label: '批准日期截止', type: 'date', queryParamKey: 'approvalEnd'
  },
  {
    key: 'effectiveStart', label: '生效日期起始', type: 'date', queryParamKey: 'effectiveStart'
  },
  {
    key: 'effectiveEnd', label: '生效日期截止', type: 'date', queryParamKey: 'effectiveEnd'
  },
  {
    key: 'expiryStart', label: '有效期起始', type: 'date', queryParamKey: 'expiryStart'
  },
  {
    key: 'expiryEnd', label: '有效期截止', type: 'date', queryParamKey: 'expiryEnd'
  }
])

const oldQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'certificateNo',
    label: '注册证编号',
    type: 'text',
    queryParamKey: 'certificateNo',
    placeholder: '输入注册证编号'
  },
  {
    key: 'expiryStart', label: '有效期起始', type: 'date', queryParamKey: 'expiryStart'
  },
  {
    key: 'expiryEnd', label: '有效期截止', type: 'date', queryParamKey: 'expiryEnd'
  }
])

const resolveRouteQueryText = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  if (rawValue === undefined || rawValue === null) {
    return undefined
  }
  const text = String(rawValue).trim()
  return /^[1-9]\d*$/.test(text) ? text : undefined
}

const syncRegistrationCertificateQueryFromRoute = () => {
  queryParams.productMasterId = resolveRouteQueryText(route.query.productMasterId)
  queryParams.projectCodeId = resolveRouteQueryText(route.query.projectCodeId)
  oldQueryParams.productMasterId = resolveRouteQueryText(route.query.productMasterId)
  oldQueryParams.projectCodeId = resolveRouteQueryText(route.query.projectCodeId)
  if (queryParams.productMasterId || queryParams.projectCodeId) {
    queryParams.pageNo = 1
    oldQueryParams.pageNo = 1
  }
}

const loadPage = async () => {
  loading.value = true
  try {
    const page = await getRegistrationCertificatePage(queryParams)
    list.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

const loadOldIndexPage = async () => {
  oldLoading.value = true
  try {
    const page = await getRegistrationCertificateOldIndexPage(oldQueryParams)
    oldList.value = page.list
    oldTotal.value = page.total
  } finally {
    oldLoading.value = false
  }
}

const currentQuickFilter = useTableQuickFilter(
  'dcc.registrationCertificate.current',
  currentQuickFilterDefinitions,
  queryParams,
  loadPage
)

const oldQuickFilter = useTableQuickFilter(
  'dcc.registrationCertificate.old',
  oldQuickFilterDefinitions,
  oldQueryParams,
  loadOldIndexPage
)

const handleTabChange = (tabName: string | number) => {
  activeTab.value = tabName === 'old' ? 'old' : 'current'
  if (activeTab.value === 'old') {
    void loadOldIndexPage()
    return
  }
  void loadPage()
}

const openDetail = (certificateId: number | string) => {
  router.push(`/mdm/registration-certificate/detail/${certificateId}`)
}

const openOldDetail = (certificateId: number | string) => {
  router.push('/mdm/registration-certificate/detail/' + String(certificateId) + '?mode=old-detail')
}

const openOldAccessRequest = (certificateId: number | string) => {
  router.push('/mdm/registration-certificate/detail/' + String(certificateId) + '?mode=access-request')
}

const openLinkedProductManagement = (productMasterId: number | string) => {
  router.push({
    path: '/mdm/product',
    query: { productMasterId: String(productMasterId) }
  })
}

const openLinkedProjectCodeManagement = (projectCodeId: number | string) => {
  router.push({
    path: '/mdm/project-code',
    query: { projectCodeId: String(projectCodeId) }
  })
}

const openUploadDialog = () => {
  showUploadDialog.value = true
}

const openRenewalDialog = (row: DccRegistrationCertificatePageItemVO) => {
  selectedRenewalCertificate.value = row
  showRenewalDialog.value = true
}

const handleUploadSaved = async () => {
  showUploadDialog.value = false
  await router.push('/approval-center?moduleCode=DCC&viewType=TODO')
}

const handleRenewalSaved = async () => {
  showRenewalDialog.value = false
  await loadPage()
}

onMounted(() => {
  syncRegistrationCertificateQueryFromRoute()
  void loadPage()
})

let registrationCertificateInitialActivationHandled = false

onActivated(async () => {
  if (!isRegistrationCertificateRoute()) {
    return
  }
  if (!registrationCertificateInitialActivationHandled) {
    registrationCertificateInitialActivationHandled = true
    return
  }
  syncRegistrationCertificateQueryFromRoute()
  if (activeTab.value === 'old') {
    await loadOldIndexPage()
    return
  }
  await loadPage()
})

watch(
  () => [route.path, route.query.productMasterId, route.query.projectCodeId],
  async () => {
    if (!isRegistrationCertificateRoute()) {
      return
    }
    syncRegistrationCertificateQueryFromRoute()
    if (activeTab.value === 'old') {
      await loadOldIndexPage()
      return
    }
    await loadPage()
  }
)
</script>
