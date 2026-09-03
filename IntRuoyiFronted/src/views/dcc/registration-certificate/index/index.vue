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
            class="registration-certificate-current-list"
            :table-key="CURRENT_TABLE_KEY"
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
            single-line-toolbar
            v-model:page="queryParams.pageNo"
            v-model:limit="queryParams.pageSize"
            @update:quick-filter-state="currentQuickFilter.updateState"
            @quick-filter-query="currentQuickFilter.applyQuickFilter"
            @column-change="saveCurrentColumnConfig"
            @pagination="loadPage"
            v-model:sort-state="currentSortState"
            @sort-change="handleCurrentSortChange"
          >
            <template #actions>
              <el-button
                v-hasPermi="['dcc:registration-certificate:config:query']"
                @click="openReminderConfig"
              >
                <Icon icon="ep:bell" class="mr-5px" />通知设置
              </el-button>
              <el-button v-hasPermi="['dcc:registration-certificate:upload:create']" type="success" @click="openUploadDialog">
                <Icon icon="ep:upload" class="mr-5px" />上传注册证
              </el-button>
            </template>

            <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
              <div class="registration-certificate-current-table-scroll-region">
                <el-table
                  v-loading="loading"
                  class="registration-certificate-current-table"
                  data-user-table-column-explicit
                  :data-user-table-key="CURRENT_TABLE_KEY"
                  :data="list"
                  height="100%"
                  border
                  :stripe="true"
                  :show-overflow-tooltip="true"
                  scrollbar-always-on
                  row-key="certificateId"
                  @header-dragend="handleCurrentHeaderDragend"
                  @sort-change="handleTemplateSortChange"
                >
                <el-table-column
                  v-if="isCurrentColumnVisible('certificateNo')"
                  label="注册证编号"
                  prop="certificateNo"
                  :min-width="getCurrentColumnMinWidthString('certificateNo', 180)"
                  :width="getCurrentColumnWidthString('certificateNo')"
                  v-bind="sortColumnAttrs('certificateNo')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('ownerCompanyName')"
                  label="所属公司"
                  prop="ownerCompanyName"
                  :min-width="getCurrentColumnMinWidthString('ownerCompanyName', 180)"
                  :width="getCurrentColumnWidthString('ownerCompanyName')"
                  v-bind="sortColumnAttrs('ownerCompanyName')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('productName')"
                  label="产品"
                  prop="productName"
                  :min-width="getCurrentColumnMinWidthString('productName', 180)"
                  :width="getCurrentColumnWidthString('productName')"
                  v-bind="sortColumnAttrs('productName')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('classification')"
                  label="分类"
                  prop="classification"
                  :min-width="getCurrentColumnMinWidthString('classification', 110)"
                  :width="getCurrentColumnWidthString('classification')"
                  v-bind="sortColumnAttrs('classification')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('projectCode')"
                  label="实际项目代码"
                  prop="projectCode"
                  :min-width="getCurrentColumnMinWidthString('projectCode', 150)"
                  :width="getCurrentColumnWidthString('projectCode')"
                  v-bind="sortColumnAttrs('projectCode')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('versionNo')"
                  label="版本"
                  prop="versionNo"
                  align="center"
                  :width="getCurrentColumnWidthString('versionNo', 90)"
                  v-bind="sortColumnAttrs('versionNo')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('status')"
                  label="状态"
                  prop="status"
                  align="center"
                  :width="getCurrentColumnWidthString('status', 130)"
                  v-bind="sortColumnAttrs('status')"
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
                  prop="reminder"
                  :width="getCurrentColumnWidthString('reminder', 120)"
                  v-bind="sortColumnAttrs('reminder')"
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
                  prop="hasProjectCode"
                  align="center"
                  :width="getCurrentColumnWidthString('hasProjectCode', 110)"
                  v-bind="sortColumnAttrs('hasProjectCode')"
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
                  prop="hasRegistrationFile"
                  align="center"
                  :width="getCurrentColumnWidthString('hasRegistrationFile', 120)"
                  v-bind="sortColumnAttrs('hasRegistrationFile')"
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
                  v-bind="sortColumnAttrs('approvalDate')"
                >
                  <template #default="{ row }">
                    {{ formatRegistrationCertificateDate(row.approvalDate) }}
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCurrentColumnVisible('effectiveDate')"
                  label="生效日"
                  prop="effectiveDate"
                  :width="getCurrentColumnWidthString('effectiveDate', 120)"
                  v-bind="sortColumnAttrs('effectiveDate')"
                >
                  <template #default="{ row }">
                    {{ formatRegistrationCertificateDate(row.effectiveDate) }}
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCurrentColumnVisible('expiryDate')"
                  label="有效期至"
                  prop="expiryDate"
                  :width="getCurrentColumnWidthString('expiryDate', 120)"
                  v-bind="sortColumnAttrs('expiryDate')"
                >
                  <template #default="{ row }">
                    {{ formatRegistrationCertificateDate(row.expiryDate) }}
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCurrentColumnVisible('remark')"
                  label="备注"
                  prop="remark"
                  :min-width="getCurrentColumnMinWidthString('remark', 220)"
                  :width="getCurrentColumnWidthString('remark')"
                  v-bind="sortColumnAttrs('remark')"
                />
                <el-table-column
                  v-if="isCurrentColumnVisible('actions')"
                  label="操作"
                  align="center"
                  fixed="right"
                  :width="getCurrentColumnWidthString('actions', 280)"
                >
                  <template #default="{ row }">
                    <div class="registration-certificate-row-actions registration-certificate-row-actions--compact">
                      <el-button link type="primary" @click="openDetail(row.certificateId)">
                        详细
                      </el-button>
                      <el-button
                        v-if="row.status === 'CURRENT'"
                        link
                        type="primary"
                        v-hasPermi="['dcc:registration-certificate:renewal:upload']"
                        @click="openRenewalDialog(row)"
                      >
                        延续
                      </el-button>
                      <el-button
                        v-if="row.status === 'CURRENT' && row.hasPendingChange === false"
                        link
                        type="primary"
                        v-hasPermi="['dcc:registration-certificate:change:submit']"
                        @click="openChange(row)"
                      >
                        变更
                      </el-button>
                    </div>
                  </template>
                </el-table-column>
                </el-table>
              </div>
            </template>
          </UnifiedListTemplate>
        </div>
      </el-tab-pane>

      <el-tab-pane name="old" label="老证">
        <div data-testid="registration-certificate-old-index">
          <UnifiedListTemplate
            :table-key="OLD_TABLE_KEY"
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
            v-model:sort-state="oldSortState"
            @sort-change="handleOldSortChange"
          >
            <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
              <el-table
                v-loading="oldLoading"
                data-user-table-column-explicit
                :data-user-table-key="OLD_TABLE_KEY"
                :data="oldList"
                border
                :stripe="true"
                :show-overflow-tooltip="true"
                row-key="versionId"
                @header-dragend="handleOldHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
                <el-table-column
                  v-if="isOldColumnVisible('certificateNo')"
                  label="注册证编号"
                  prop="certificateNo"
                  :min-width="getOldColumnMinWidthString('certificateNo', 180)"
                  :width="getOldColumnWidthString('certificateNo')"
                  v-bind="sortColumnAttrs('certificateNo')"
                />
                <el-table-column
                  v-if="isOldColumnVisible('ownerCompanyName')"
                  label="所属公司"
                  prop="ownerCompanyName"
                  :min-width="getOldColumnMinWidthString('ownerCompanyName', 180)"
                  :width="getOldColumnWidthString('ownerCompanyName')"
                  v-bind="sortColumnAttrs('ownerCompanyName')"
                />
                <el-table-column
                  v-if="isOldColumnVisible('productName')"
                  label="产品"
                  prop="productName"
                  :min-width="getOldColumnMinWidthString('productName', 180)"
                  :width="getOldColumnWidthString('productName')"
                  v-bind="sortColumnAttrs('productName')"
                />
                <el-table-column
                  v-if="isOldColumnVisible('classification')"
                  label="分类"
                  prop="classification"
                  :min-width="getOldColumnMinWidthString('classification', 110)"
                  :width="getOldColumnWidthString('classification')"
                  v-bind="sortColumnAttrs('classification')"
                />
                <el-table-column
                  v-if="isOldColumnVisible('versionNo')"
                  label="版本"
                  prop="versionNo"
                  align="center"
                  :width="getOldColumnWidthString('versionNo', 90)"
                  v-bind="sortColumnAttrs('versionNo')"
                />
                <el-table-column
                  v-if="isOldColumnVisible('status')"
                  label="状态"
                  prop="status"
                  align="center"
                  :width="getOldColumnWidthString('status', 130)"
                  v-bind="sortColumnAttrs('status')"
                >
                  <template #default="{ row }">
                    <el-tag :type="getRegistrationCertificateStatusTagType(row.status)">
                      {{ row.status === 'OLD' ? '已失效，失效日期 ' + formatRegistrationCertificateDate(row.expiryDate) : formatRegistrationCertificateStatus(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isOldColumnVisible('expiryDate')"
                  label="原有效期至"
                  prop="expiryDate"
                  :width="getOldColumnWidthString('expiryDate', 140)"
                  v-bind="sortColumnAttrs('expiryDate')"
                >
                  <template #default="{ row }">
                    {{ formatRegistrationCertificateDate(row.expiryDate) }}
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isOldColumnVisible('actions')"
                  label="操作"
                  align="center"
                  fixed="right"
                  :width="getOldColumnWidthString('actions', 140)"
                >
                  <template #default="{ row }">
                    <div class="registration-certificate-row-actions registration-certificate-row-actions--compact registration-certificate-row-actions--old-manager-view">
                      <el-button link type="primary" @click="openOldDetail(row.certificateId, row.versionId)">
                        查看
                      </el-button>
                    </div>
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
  <RegistrationCertificateChangeDialog
    v-model="showChangeDialog"
    :certificate="selectedChangeCertificate"
    @saved="handleChangeSaved"
  />
  <RegistrationCertificateReminderConfigDialog ref="reminderConfigDialogRef" />
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  getRegistrationCertificateOldIndexPage,
  getRegistrationCertificatePage,
  type DccRegistrationCertificateOldIndexItemVO,
  type DccRegistrationCertificatePageItemVO,
  type DccRegistrationCertificatePageReqVO,
  type RegistrationCertificateSortField
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
import RegistrationCertificateChangeDialog from '../change/ChangeDialog.vue'
import RegistrationCertificateReminderConfigDialog from '../config/ReminderConfigDialog.vue'
import {
  REGISTRATION_CERTIFICATE_REMINDER_FILTER_OPTIONS,
  REGISTRATION_CERTIFICATE_STATUS_OPTIONS,
  formatMissingMarker,
  formatRegistrationCertificateDate,
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
const showChangeDialog = ref(false)
const selectedChangeCertificate = ref<DccRegistrationCertificatePageItemVO>()
const reminderConfigDialogRef = ref<InstanceType<typeof RegistrationCertificateReminderConfigDialog>>()
const CURRENT_TABLE_KEY = 'dcc.registrationCertificate.current.actionsDoubleWidthV1'
const OLD_TABLE_KEY = 'dcc.registrationCertificate.old.unifiedViewActionV1'

type RegistrationCertificatePageQuery = DccRegistrationCertificatePageReqVO &
  Required<Pick<PageParam, 'pageNo' | 'pageSize'>>
type RegistrationCertificateSortOrder = 'ascending' | 'descending' | null
type RegistrationCertificateSortState = {
  key?: string
  prop?: string
  order?: RegistrationCertificateSortOrder
}
type RegistrationCertificateSortChange = RegistrationCertificateSortState & {
  column?: unknown
}

const queryParams = reactive<RegistrationCertificatePageQuery>({ pageNo: 1, pageSize: 10 })
const oldQueryParams = reactive<RegistrationCertificatePageQuery>({ pageNo: 1, pageSize: 10 })
const currentSortState = ref<RegistrationCertificateSortState>({})
const oldSortState = ref<RegistrationCertificateSortState>({})

const CURRENT_SERVER_SORT_FIELDS = new Set<RegistrationCertificateSortField>([
  'certificateNo',
  'ownerCompanyName',
  'productName',
  'classification',
  'projectCode',
  'versionNo',
  'status',
  'hasProjectCode',
  'hasRegistrationFile',
  'approvalDate',
  'effectiveDate',
  'expiryDate',
  'reminder',
  'remark'
])

const OLD_SERVER_SORT_FIELDS = new Set<RegistrationCertificateSortField>([
  'certificateNo',
  'ownerCompanyName',
  'productName',
  'classification',
  'versionNo',
  'status',
  'expiryDate'
])

const currentColumnDefinitions: UserTableColumnDefinition[] = [
  { key: 'certificateNo', label: '注册证编号', minWidth: 180, sortable: 'custom' },
  { key: 'ownerCompanyName', label: '所属公司', minWidth: 180, sortable: 'custom' },
  { key: 'productName', label: '产品', minWidth: 180, sortable: 'custom' },
  { key: 'classification', label: '分类', minWidth: 110, sortable: 'custom' },
  { key: 'projectCode', label: '实际项目代码', minWidth: 150, sortable: 'custom' },
  { key: 'versionNo', label: '版本', width: 90, sortable: 'custom' },
  { key: 'status', label: '状态', width: 130, sortable: 'custom' },
  { key: 'reminder', label: '提醒状态', width: 120, sortable: 'custom' },
  { key: 'hasProjectCode', label: '项目代码', width: 110, sortable: 'custom' },
  { key: 'hasRegistrationFile', label: '注册证文件', width: 120, sortable: 'custom' },
  { key: 'approvalDate', label: '批准日', width: 120, sortable: 'custom' },
  { key: 'effectiveDate', label: '生效日', width: 120, sortable: 'custom' },
  { key: 'expiryDate', label: '有效期至', width: 120, sortable: 'custom' },
  { key: 'remark', label: '备注', minWidth: 220, sortable: 'custom' },
  { key: 'actions', label: '操作', width: 280, hideable: false, business: false, sortable: false }
]

const oldColumnDefinitions: UserTableColumnDefinition[] = [
  { key: 'certificateNo', label: '注册证编号', minWidth: 180, sortable: 'custom' },
  { key: 'ownerCompanyName', label: '所属公司', minWidth: 180, sortable: 'custom' },
  { key: 'productName', label: '产品', minWidth: 180, sortable: 'custom' },
  { key: 'classification', label: '分类', minWidth: 110, sortable: 'custom' },
  { key: 'versionNo', label: '版本', width: 90, sortable: 'custom' },
  { key: 'status', label: '状态', width: 130, sortable: 'custom' },
  { key: 'expiryDate', label: '原有效期至', width: 140, sortable: 'custom' },
  { key: 'actions', label: '操作', width: 140, hideable: false, business: false, sortable: false }
]

const {
  columns: currentColumns,
  saving: currentColumnSaving,
  isColumnVisible: isCurrentColumnVisible,
  getColumnWidthString: getCurrentColumnWidthString,
  getColumnMinWidthString: getCurrentColumnMinWidthString,
  handleHeaderDragend: handleCurrentHeaderDragend,
  saveConfig: saveCurrentColumnConfig
} = useUserTableColumns(CURRENT_TABLE_KEY, currentColumnDefinitions)

const {
  columns: oldColumns,
  saving: oldColumnSaving,
  isColumnVisible: isOldColumnVisible,
  getColumnWidthString: getOldColumnWidthString,
  getColumnMinWidthString: getOldColumnMinWidthString,
  handleHeaderDragend: handleOldHeaderDragend,
  saveConfig: saveOldColumnConfig
} = useUserTableColumns(OLD_TABLE_KEY, oldColumnDefinitions)

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
    key: 'reminderState',
    label: '提醒状态',
    type: 'select',
    queryParamKey: 'reminderState',
    options: REGISTRATION_CERTIFICATE_REMINDER_FILTER_OPTIONS
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

const applyRegistrationCertificateSort = (
  payload: RegistrationCertificateSortChange,
  query: RegistrationCertificatePageQuery,
  updateSortState: (state: RegistrationCertificateSortState) => void,
  allowedFields: Set<RegistrationCertificateSortField>,
  load: () => Promise<void>
) => {
  const sortField = payload.prop || payload.key
  const order = payload.order
  if (!order) {
    delete query.sortField
    delete query.sortOrder
    query.pageNo = 1
    updateSortState({})
    void load()
    return
  }
  if (!sortField || !allowedFields.has(sortField as RegistrationCertificateSortField)) {
    throw new Error(`注册证列表排序字段未配置：${sortField || '-'}`)
  }
  query.sortField = sortField as RegistrationCertificateSortField
  query.sortOrder = order === 'ascending' ? 'asc' : 'desc'
  query.pageNo = 1
  updateSortState({ key: sortField, prop: sortField, order })
  void load()
}

const handleCurrentSortChange = (payload: RegistrationCertificateSortChange) => {
  applyRegistrationCertificateSort(
    payload,
    queryParams,
    (state) => {
      currentSortState.value = state
    },
    CURRENT_SERVER_SORT_FIELDS,
    loadPage
  )
}

const handleOldSortChange = (payload: RegistrationCertificateSortChange) => {
  applyRegistrationCertificateSort(
    payload,
    oldQueryParams,
    (state) => {
      oldSortState.value = state
    },
    OLD_SERVER_SORT_FIELDS,
    loadOldIndexPage
  )
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

const openChange = (row: DccRegistrationCertificatePageItemVO) => {
  selectedChangeCertificate.value = row
  showChangeDialog.value = true
}

const openOldDetail = (certificateId: number | string, versionId: number | string) => {
  router.push({
    path: '/mdm/registration-certificate/detail/' + String(certificateId),
    query: { mode: 'old-detail', versionId: String(versionId) }
  })
}

const openUploadDialog = () => {
  showUploadDialog.value = true
}

const openReminderConfig = () => {
  reminderConfigDialogRef.value?.open()
}

const openRenewalDialog = (row: DccRegistrationCertificatePageItemVO) => {
  if (row.hasPendingRenewal) {
    ElMessage.warning('该注册证已有待审批或待生效的延续，请勿重复提交')
    return
  }
  selectedRenewalCertificate.value = row
  showRenewalDialog.value = true
}

const handleUploadSaved = async () => {
  showUploadDialog.value = false
  await router.push('/approval-center/todo?viewType=TODO')
}

const handleRenewalSaved = async () => {
  showRenewalDialog.value = false
  await loadPage()
}

const handleChangeSaved = async () => {
  showChangeDialog.value = false
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

<style scoped>
.registration-certificate-current-list {
  height: calc(100vh - 180px);
  min-height: 520px;
  overflow: hidden;
}

.registration-certificate-current-list :deep(.unified-list-template__query-form) {
  flex: 0 0 auto;
}

.registration-certificate-current-list :deep(.unified-list-template__table-shell) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.registration-certificate-current-list :deep(.el-pagination) {
  flex: 0 0 auto;
}

.registration-certificate-current-table-scroll-region {
  display: flex;
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.registration-certificate-current-table {
  width: 100%;
}

.registration-certificate-current-table :deep(.el-table__body-wrapper) {
  overflow-y: auto;
}

.registration-certificate-current-table :deep(.el-scrollbar__bar.is-horizontal) {
  display: block;
  height: 8px;
  opacity: 1;
}

.registration-certificate-current-table :deep(.el-scrollbar__bar.is-horizontal > div) {
  background-color: #9caec4;
}

@media (min-width: 1181px) {
  .registration-certificate-current-list.unified-list-template--single-line-toolbar
    :deep(.unified-list-template__query-form) {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .registration-certificate-current-list.unified-list-template--single-line-toolbar
    :deep(.unified-list-template__multi-filter) {
    min-width: 0;
  }

  .registration-certificate-current-list.unified-list-template--single-line-toolbar
    :deep(.table-multi-filter),
  .registration-certificate-current-list.unified-list-template--single-line-toolbar
    :deep(.table-multi-filter__tabs-empty) {
    min-width: 0;
  }

  .registration-certificate-current-list.unified-list-template--single-line-toolbar
    :deep(.unified-list-template__toolbar) {
    white-space: nowrap;
  }
}

.registration-certificate-row-actions {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: center;
  justify-items: center;
  gap: 4px 8px;
}

.registration-certificate-row-actions--compact {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
}

.registration-certificate-test-tab { padding: 16px 0; }
.registration-certificate-business-time-controls { display: flex; align-items: center; gap: 12px; }
.registration-certificate-business-time-result { margin-top: 12px; color: var(--el-color-success); }

.registration-certificate-row-actions--old-manager-view {
  grid-template-columns: minmax(0, 1fr);
  gap: 4px;
}

.registration-certificate-row-actions :deep(.el-button) {
  margin-left: 0;
  white-space: nowrap;
}
</style>
