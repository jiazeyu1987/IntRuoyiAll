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
              <el-button type="success" @click="openCreateDraft">
                <Icon icon="ep:plus" class="mr-5px" />新增注册证
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
                  v-if="isCurrentColumnVisible('actions')"
                  label="操作"
                  align="center"
                  fixed="right"
                  :width="getCurrentColumnWidthString('actions', 160)"
                >
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openDetail(row.certificateId)">
                      详情
                    </el-button>
                    <el-button link type="primary" @click="openDetail(row.certificateId)">
                      维护
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
                      {{ formatRegistrationCertificateStatus(row.status) }}
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
                  :width="getOldColumnWidthString('actions', 160)"
                >
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openDetail(row.certificateId)">
                      详情
                    </el-button>
                    <el-button link type="warning" @click="openDetail(row.certificateId)">
                      申请查看
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

  <RegistrationCertificateActionPanel v-if="showCreateDraft" />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
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
import RegistrationCertificateActionPanel from '../workflow/ActionPanel.vue'
import {
  REGISTRATION_CERTIFICATE_STATUS_OPTIONS,
  formatMissingMarker,
  formatRegistrationCertificateStatus,
  getMissingMarkerTagType,
  getRegistrationCertificateStatusTagType
} from '../shared/state'

defineOptions({ name: 'DccRegistrationCertificateIndex' })

const router = useRouter()
const activeTab = ref<'current' | 'old'>('current')
const loading = ref(false)
const oldLoading = ref(false)
const list = ref<DccRegistrationCertificatePageItemVO[]>([])
const oldList = ref<DccRegistrationCertificateOldIndexItemVO[]>([])
const total = ref(0)
const oldTotal = ref(0)
const showCreateDraft = ref(false)

const queryParams = reactive<DccRegistrationCertificatePageReqVO>({ pageNo: 1, pageSize: 10 })
const oldQueryParams = reactive<DccRegistrationCertificatePageReqVO>({ pageNo: 1, pageSize: 10 })

const currentColumnDefinitions: UserTableColumnDefinition[] = [
  { key: 'certificateNo', label: '注册证编号', minWidth: 180, sortable: false },
  { key: 'ownerCompanyName', label: '所属公司', minWidth: 180, sortable: false },
  { key: 'productName', label: '产品', minWidth: 180, sortable: false },
  { key: 'versionNo', label: '版本', width: 90, sortable: false },
  { key: 'status', label: '状态', width: 130, sortable: false },
  { key: 'hasProjectCode', label: '项目代码', width: 110, sortable: false },
  { key: 'hasRegistrationFile', label: '注册证文件', width: 120, sortable: false },
  { key: 'approvalDate', label: '批准日', width: 120, sortable: false },
  { key: 'effectiveDate', label: '生效日', width: 120, sortable: false },
  { key: 'expiryDate', label: '有效期至', width: 120, sortable: false },
  { key: 'actions', label: '操作', width: 96, hideable: false, business: false, sortable: false }
]

const oldColumnDefinitions: UserTableColumnDefinition[] = [
  { key: 'certificateNo', label: '注册证编号', minWidth: 180, sortable: false },
  { key: 'ownerCompanyName', label: '所属公司', minWidth: 180, sortable: false },
  { key: 'productName', label: '产品', minWidth: 180, sortable: false },
  { key: 'versionNo', label: '版本', width: 90, sortable: false },
  { key: 'status', label: '状态', width: 130, sortable: false },
  { key: 'expiryDate', label: '原有效期至', width: 140, sortable: false },
  { key: 'actions', label: '操作', width: 96, hideable: false, business: false, sortable: false }
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

const openCreateDraft = () => {
  showCreateDraft.value = true
}

onMounted(() => {
  void loadPage()
})
</script>
