<template>
  <DesignerWrapper v-if="isDesignerMode" />
  <ContentWrap v-else :body-style="{ padding: '0px' }" class="!mb-0 batch-record-form-page">
    <input
      ref="wordImportFileInputRef"
      type="file"
      :accept="wordImportFileAccept"
      class="batch-record-form-word-import-input"
      @change="handleImportFileChange"
    />
    <div class="batch-record-form-layout">
      <section class="batch-record-form-layout__list">
        <el-alert
          v-if="listErrorMessage"
          :title="listErrorMessage"
          type="error"
          :closable="false"
          show-icon
          class="batch-record-form-layout__alert"
        />
        <UnifiedListTemplate
          table-key="mes.pro.edhrBatch.recordFormList.projectCodeV1"
          :query-model="queryParams"
          :filter-definitions="recordFormQuickFilterDefinitions"
          :show-quick-filter-label="false"
          :quick-filter-state="recordFormQuickFilter.state"
          :selected-filter-definition="recordFormQuickFilter.selectedDefinition.value"
          :operator-options="recordFormQuickFilter.operatorOptions.value"
          :columns="recordFormColumns"
          :column-saving="recordFormColumnSaving"
          :show-column-reset="false"
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @update:quick-filter-state="recordFormQuickFilter.updateState"
          @quick-filter-query="recordFormQuickFilter.applyQuickFilter"
          @column-change="saveRecordFormColumnConfig"
          @pagination="getList"
        >
          <template #actions>
            <el-button
              class="batch-record-form-toolbar__import-button"
              type="primary"
              plain
              :loading="wordImporting"
              @click="openWordImportDialog"
            >
              <Icon icon="ep:upload" class="mr-5px" />
              导入
            </el-button>
            <div class="batch-record-form-toolbar__latest-version-switch">
              <span class="batch-record-form-toolbar__latest-version-label">最新版本</span>
              <el-switch
                v-model="queryParams.latestVersionOnly"
                @change="handleLatestVersionOnlyChange"
              />
            </div>
          </template>
          <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
            <el-table
              v-loading="listLoading"
              :data="list"
              row-key="rowKey"
              height="calc(100vh - 260px)"
              highlight-current-row
              border
              @row-click="selectReport"
              @header-dragend="handleRecordFormHeaderDragend"
              @sort-change="handleTemplateSortChange"
            >
              <el-table-column
                v-if="isRecordFormColumnVisible('productName')"
                label="产品名称"
                prop="productName"
                :width="getRecordFormColumnWidthString('productName', 180)"
                :min-width="getRecordFormColumnMinWidthString('productName', 160)"
                show-overflow-tooltip
                v-bind="sortColumnAttrs('productName')"
              >
                <template #default="{ row }">
                  <span>{{ row.productName || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isRecordFormColumnVisible('projectCode')"
                label="项目代码"
                prop="projectCode"
                :width="getRecordFormColumnWidthString('projectCode', 140)"
                :min-width="getRecordFormColumnMinWidthString('projectCode', 120)"
                show-overflow-tooltip
                v-bind="sortColumnAttrs('projectCode')"
              >
                <template #default="{ row }">
                  <span>{{ row.projectCode || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isRecordFormColumnVisible('reportName')"
                label="表单名称"
                prop="reportName"
                :min-width="getRecordFormColumnMinWidthString('reportName', 220)"
                show-overflow-tooltip
                v-bind="sortColumnAttrs('reportName')"
              />
              <el-table-column
                v-if="isRecordFormColumnVisible('formSlotType')"
                label="类型"
                prop="formSlotType"
                :width="getRecordFormColumnWidthString('formSlotType', 120)"
                v-bind="sortColumnAttrs('formSlotType')"
              >
                <template #default="{ row }">
                  <el-tag effect="plain" type="info">{{ resolveFormSlotTypeLabel(row.formSlotType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isRecordFormColumnVisible('versionNo')"
                label="版本"
                prop="versionNo"
                :width="getRecordFormColumnWidthString('versionNo', 110)"
                show-overflow-tooltip
                v-bind="sortColumnAttrs('versionNo')"
              >
                <template #default="{ row }">
                  <span>{{ row.versionNo || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isRecordFormColumnVisible('versionStatus')"
                label="状态"
                prop="versionStatus"
                :width="getRecordFormColumnWidthString('versionStatus', 110)"
                v-bind="sortColumnAttrs('versionStatus')"
              >
                <template #default="{ row }">
                  <el-tag
                    effect="plain"
                    :type="resolveVersionStatusPresentation(row.versionStatus).type"
                  >
                    {{ resolveVersionStatusPresentation(row.versionStatus).label }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isRecordFormColumnVisible('updateTime')"
                label="更新时间"
                prop="updateTime"
                :width="getRecordFormColumnWidthString('updateTime', 180)"
                v-bind="sortColumnAttrs('updateTime')"
              >
                <template #default="{ row }">
                  <span>{{ formatNullableDate(row.updateTime) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </UnifiedListTemplate>
      </section>

      <section class="batch-record-form-preview">
        <div class="batch-record-form-preview__header">
          <div class="batch-record-form-preview__heading">
            <span class="batch-record-form-preview__title">{{ selectedReport?.reportName || '未选择表单' }}</span>
          </div>
          <div v-if="selectedReport" class="batch-record-form-preview__actions">
            <el-button
              link
              type="primary"
              :disabled="!templatePreview.formViewModel || templatePreview.loading"
              @click="enterPreviewMaximize"
            >
              最大化
            </el-button>
            <el-button link type="primary" @click="openDesigner(selectedReport.reportId, 'preview')">打开</el-button>
            <el-button link type="primary" @click="openDesigner(selectedReport.reportId, 'edit')">编辑</el-button>
            <el-button link type="primary" @click="openSimulate(selectedReport)">填写</el-button>
            <el-button link type="primary" @click="openTemplateAction(selectedReport, 'signature')">签名</el-button>
            <el-button link type="primary" @click="openTemplateAction(selectedReport, 'cellRules')">填写配置</el-button>
            <el-button link type="primary" @click="handleCellLinks(selectedReport)">链接</el-button>
            <el-button link type="primary" @click="handleRename(selectedReport)">重命名</el-button>
            <el-button link type="danger" @click="handleDelete(selectedReport)">删除</el-button>
          </div>
        </div>
        <div v-loading="templatePreview.loading" class="batch-record-form-preview__body">
          <el-alert
            v-if="templatePreview.errorMessage"
            :title="templatePreview.errorMessage"
            type="error"
            :closable="false"
            show-icon
          />
          <div v-else-if="templatePreview.formViewModel" class="batch-record-form-preview__frame">
            <EdhrExecutionReadonlyForm
              :form-view-model="templatePreview.formViewModel"
              :signature-records="templatePreview.signatureRecords"
              fit-to-viewport
              fit-mode="width"
              embedded
            />
          </div>
          <el-empty v-else description="请选择左侧表单查看预览" />
        </div>
      </section>
    </div>

    <Teleport to="body">
      <div v-if="previewMaximized" class="batch-record-form-focused-preview">
        <aside class="batch-record-form-focused-preview__control">
          <div class="batch-record-form-focused-preview__title-block">
            <span class="batch-record-form-focused-preview__label">当前表单</span>
            <strong class="batch-record-form-focused-preview__form-name">
              {{ selectedReport?.reportName || '未选择表单' }}
            </strong>
          </div>
          <div class="batch-record-form-focused-preview__buttons">
            <el-button plain :disabled="!canPreviewPrevious" @click="selectPreviewNeighbor(-1)">
              上一张
            </el-button>
            <el-button plain :disabled="!canPreviewNext" @click="selectPreviewNeighbor(1)">
              下一张
            </el-button>
            <el-button plain type="primary" @click="restorePreviewLayout">恢复</el-button>
            <el-button
              plain
              :type="previewFitMode === 'height' ? 'primary' : 'default'"
              @click="setPreviewFitMode('height')"
            >
              高度自适应
            </el-button>
            <el-button
              plain
              :type="previewFitMode === 'width' ? 'primary' : 'default'"
              @click="setPreviewFitMode('width')"
            >
              宽度自适应
            </el-button>
          </div>
        </aside>
        <main class="batch-record-form-focused-preview__stage">
          <div
            v-loading="templatePreview.loading"
            class="batch-record-form-focused-preview__body"
            :class="{ 'is-height-fit': previewFitMode === 'height' }"
          >
            <el-alert
              v-if="templatePreview.errorMessage"
              :title="templatePreview.errorMessage"
              type="error"
              :closable="false"
              show-icon
            />
            <div
              v-else-if="templatePreview.formViewModel"
              class="batch-record-form-focused-preview__frame"
              :class="{ 'is-height-fit': previewFitMode === 'height' }"
            >
              <EdhrExecutionReadonlyForm
                :form-view-model="templatePreview.formViewModel"
                :signature-records="templatePreview.signatureRecords"
                fit-to-viewport
                :fit-mode="previewFitMode"
                embedded
              />
            </div>
            <el-empty v-else description="请选择左侧表单查看预览" />
          </div>
        </main>
      </div>
    </Teleport>

    <el-dialog
      v-model="wordImportDialog.visible"
      title="导入 Word"
      width="520px"
      destroy-on-close
      @closed="resetWordImportDialog"
    >
      <el-form label-width="120px" class="batch-record-word-import-form">
        <el-form-item label="导入类型" required>
          <el-radio-group
            v-model="wordImportDialog.selectedFormSlotType"
            @change="handleWordImportTypeChange"
          >
            <el-radio-button value="MAIN">批记录</el-radio-button>
            <el-radio-button value="FORM">表单</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="isMainWordImport" label="产品名称" required>
          <el-select
            v-model="wordImportDialog.selectedDccProjectCodeId"
            class="batch-record-word-import-form__project-select"
            filterable
            remote
            clearable
            :remote-method="loadWordImportProjectOptions"
            :loading="wordImportDialog.projectLoading"
            placeholder="请选择 DCC 项目代码页签中的产品名称"
          >
            <el-option
              v-for="item in wordImportDialog.projectOptions"
              :key="item.id"
              :label="item.projectName"
              :value="item.id"
            >
              <div class="batch-record-word-import-form__project-option">
                <span class="batch-record-word-import-form__project-name">{{ item.projectName }}</span>
                <span v-if="item.projectCode" class="batch-record-word-import-form__project-code">
                  {{ item.projectCode }}
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-else label="表单名称" required>
          <el-input
            v-model="wordImportDialog.formName"
            class="batch-record-word-import-form__name-input"
            maxlength="100"
            clearable
            placeholder="请输入表单名称"
            @input="handleUnifiedFormNameInput"
          />
        </el-form-item>
        <el-form-item label="Word 文件" required>
          <div class="batch-record-word-import-form__file-row">
            <el-button
              plain
              :disabled="!canSelectWordImportFile"
              @click="handleWordImportFileSelect"
            >
              选择文件
            </el-button>
            <div
              v-if="wordImportDialog.file || wordImportDialog.preflightLoading || wordImportDialog.preflightErrorMessage"
              class="batch-record-word-import-form__file-state"
              :class="{ 'is-error': wordImportDialog.preflightErrorMessage }"
            >
              <Icon icon="ep:document" class="batch-record-word-import-form__file-icon" />
              <span class="batch-record-word-import-form__file-name">
                已选择 Word 文件：{{ wordImportDialog.file?.name || '-' }}
              </span>
              <el-tag v-if="wordImportDialog.preflightLoading" type="info" effect="plain">
                正在预检 Word 文件
              </el-tag>
              <el-tag v-else-if="wordImportDialog.preflightErrorMessage" type="danger" effect="plain">
                预检失败
              </el-tag>
              <el-tag v-else-if="isMainWordImport && wordImportDialog.preflight" type="success" effect="plain">
                预检完成
              </el-tag>
              <el-tag v-else type="success" effect="plain">已选择</el-tag>
            </div>
          </div>
          <el-alert
            v-if="wordImportDialog.preflightErrorMessage"
            class="batch-record-word-import-form__file-error"
            :title="wordImportDialog.preflightErrorMessage"
            type="error"
            :closable="false"
            show-icon
          />
        </el-form-item>
        <el-form-item v-if="isMainWordImport" label="导入内容">
          <div
            v-loading="wordImportDialog.preflightLoading"
            class="batch-record-word-import-form__preflight"
          >
            <template v-if="wordImportDialog.preflight">
              <div class="batch-record-word-import-form__version-grid">
                <div class="batch-record-word-import-form__version-item">
                  <span>最新批记录版本</span>
                  <strong>
                    {{ formatWordImportLatestBatchRecordVersion(wordImportDialog.preflight) }}
                  </strong>
                </div>
                <div class="batch-record-word-import-form__version-item">
                  <span>当前工艺流程版本</span>
                  <strong>
                    {{ formatWordImportVersion(wordImportDialog.preflight.currentRouteVersionNo) }}
                  </strong>
                </div>
              </div>
              <el-alert
                v-if="wordImportDialog.preflight.routeGovernanceStatus === 'DUPLICATE_BLOCKED'"
                type="error"
                :closable="false"
                show-icon
                class="batch-record-word-import-form__reference-alert"
                title="所选 DCC 项目代码存在多条正式路线绑定，请先人工确定/清理唯一保留路线。"
              />
              <el-alert
                v-if="wordImportDialog.preflight.routeGovernanceStatus === 'CREATE_REQUIRED'
                  && wordImportDialog.selectedRouteProductOptionKeys.length > 0"
                type="info"
                :closable="false"
                show-icon
                class="batch-record-word-import-form__reference-alert"
                title="所选 DCC 项目代码尚未绑定工艺路线，确认后将新建路线并写入正式绑定。"
              />
              <el-alert
                v-if="wordImportDialog.preflight.routeUpgradeRequired
                  && wordImportDialog.selectedRouteProductOptionKeys.length > 0
                  && !wordImportDialog.preflight.currentRouteCandidateVersionId"
                type="warning"
                :closable="false"
                show-icon
                class="batch-record-word-import-form__reference-alert"
                :title="`所选 DCC 项目代码已正式绑定工艺路线“${wordImportDialog.preflight.currentRouteName || wordImportDialog.preflight.batchRecordName}”。本次勾选“工艺流程”后将按 Word 工序顺序生成/更新路线候选版本，发布后才生效；当前生效路线不会被覆盖。`"
              />
              <el-alert
                v-if="wordImportDialog.preflight.routeUpgradeRequired
                  && wordImportDialog.rebuildBatchRecord
                  && wordImportDialog.selectedRouteProductOptionKeys.length === 0"
                type="info"
                :closable="false"
                show-icon
                class="batch-record-word-import-form__reference-alert"
                title="未勾选“工艺流程”时，本次仅生成/更新批记录表单绑定候选；候选沿用当前工艺流程节点和流程关系，不按 Word 重建工艺流程，发布后才生效。"
              />
              <el-alert
                v-if="wordImportDialog.preflight.routeRestoreRequired
                  && wordImportDialog.preflight.routeGovernanceStatus === 'UPGRADE_REQUIRED'"
                type="warning"
                :closable="false"
                show-icon
                class="batch-record-word-import-form__reference-alert"
                title="当前唯一工艺路线已禁用，确认后将先恢复路线，再生成/更新候选版本。"
              />
              <el-alert
                v-if="wordImportDialog.preflight.currentRouteCandidateVersionStatus === 'DRAFT'"
                type="warning"
                :closable="false"
                show-icon
                class="batch-record-word-import-form__reference-alert"
                :title="`当前已有 ${wordImportDialog.preflight.currentRouteCandidateVersionNo || '候选版本'} 草稿，本次导入将更新现有 ${wordImportDialog.preflight.currentRouteCandidateVersionNo || '候选版本'} 草稿，不会创建下一版本；草稿待发布后生效。`"
              />
              <el-alert
                v-else-if="isWordImportRouteCandidateLocked(wordImportDialog.preflight)"
                type="error"
                :closable="false"
                show-icon
                class="batch-record-word-import-form__reference-alert"
                :title="`工艺路线候选版本 ${wordImportDialog.preflight.currentRouteCandidateVersionNo || ''} 当前为${wordImportDialog.preflight.currentRouteCandidateVersionStatus === 'PENDING_APPROVAL' ? '待审批' : '待发布'}状态，请先撤回、取消或完成发布后再导入。`"
              />
              <div class="batch-record-word-import-form__action-row">
                <span class="batch-record-word-import-form__action-label">导入动作</span>
                <el-radio-group v-model="wordImportDialog.selectedAction">
                  <el-radio-button
                    value="REBUILD_V1"
                    :disabled="!isWordImportActionAllowed('REBUILD_V1')"
                  >
                    重建 V1.0
                  </el-radio-button>
                  <el-radio-button
                    value="UPGRADE"
                    :disabled="!isWordImportActionAllowed('UPGRADE')"
                  >
                    升版导入 {{ wordImportDialog.preflight.nextVersionNo || '' }}
                  </el-radio-button>
                </el-radio-group>
              </div>
              <el-checkbox v-model="wordImportDialog.rebuildBatchRecord">
                批记录表单
              </el-checkbox>
              <div class="batch-record-word-import-form__route-list">
                <div class="batch-record-word-import-form__route-title">工艺流程</div>
                <el-checkbox-group v-model="wordImportDialog.selectedRouteProductOptionKeys">
                  <el-checkbox
                    v-for="option in wordImportDialog.preflight.routeProductOptions"
                    :key="option.optionKey"
                    :value="option.optionKey"
                    :disabled="isWordImportRouteCandidateLocked(wordImportDialog.preflight)"
                    class="batch-record-word-import-form__route-option"
                  >
                    <span>{{ option.productName }}</span>
                    <small>当前版本 {{ formatWordImportVersion(option.routeVersionNo) }}</small>
                  </el-checkbox>
                </el-checkbox-group>
              </div>
            </template>
            <span v-else class="batch-record-word-import-form__empty">
              选择产品名称和 Word 文件后显示批记录表单与工艺流程选择
            </span>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="cancelWordImportDialog">取消</el-button>
        <el-button
          type="primary"
          :loading="wordImportDialog.confirming"
          :disabled="wordImportDialog.preflightLoading || (isMainWordImport && wordImportDialog.preflight && !hasWordImportAllowedAction)"
          @click="confirmWordImportDialog"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <BatchRecordCellRulesConfirmDialog
      v-model="cellRulesDialog.visible"
      :report="cellRulesDialog.report"
      :can-navigate-previous="canNavigateCellRulesPrevious"
      :can-navigate-next="canNavigateCellRulesNext"
      :navigation-loading="cellRulesNavigation.loading"
      :navigation-error-message="cellRulesNavigation.errorMessage"
      :navigation-label="cellRulesNavigation.label"
      @confirmed="handleCellRulesConfirmed"
      @navigate="navigateCellRulesDialog"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElLoading, ElMessageBox } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { formatDateTimeValue } from '@/utils/formatTime'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  BatchRecordReportApi,
  type BatchRecordFormSlotType,
  type BatchRecordReportCellRuleVO,
  type BatchRecordReportCellRulesRespVO,
  type BatchRecordReportImportResultVO,
  type BatchRecordReportImportPreflightVO,
  type BatchRecordReportImportRouteProductOptionVO,
  type BatchRecordWordImportAction,
  type BatchRecordReportVO
} from '@/api/mes/pro/batchrecordreport'
import {
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import {
  cleanedAttachmentRule,
  normalizeCellRule,
  type TemplateRawLayout
} from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'
import DesignerWrapper from '@/views/mes/pro/batchrecord-shared/DesignerWrapper.vue'
import BatchRecordCellRulesConfirmDialog from '@/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue'
import EdhrExecutionReadonlyForm from '@/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'
import type {
  EdhrBatchExecutionReviewFormViewModel,
  EdhrBatchExecutionReviewSignatureRecord
} from '@/api/mes/pro/edhr/batchExecution'

defineOptions({ name: 'MesProBatchRecordFormList' })

type RecordFormListRow = BatchRecordReportVO & {
  rowKey: string
}

const route = useRoute()
const router = useRouter()
const message = useMessage()
const BATCH_RECORD_FORM_LIST_PATH = '/mes/pro/batch-record-form-list'
const PROCESS_POOL_REPORT_SOURCE_REPORT_ID = 'PROCESS_POOL_REPORT'
const isBatchRecordFormListPath = () => route.path === BATCH_RECORD_FORM_LIST_PATH
const isDesignerMode = computed(() => route.query.mode === 'designer')
const isMainBatchRecordReport = (row: BatchRecordReportVO) => row.formSlotType === 'MAIN'
const normalizeRouteQueryText = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' && rawValue.trim() ? rawValue.trim() : ''
}
const buildBatchRecordFormListRouteStateKey = () =>
  JSON.stringify({
    reportId: normalizeRouteQueryText(route.query.reportId),
    action: normalizeRouteQueryText(route.query.action),
    mode: normalizeRouteQueryText(route.query.mode)
  })
const listLoading = ref(false)
const listErrorMessage = ref('')
const list = ref<RecordFormListRow[]>([])
const total = ref(0)
const batchRecordFormListHasLoadedRouteState = ref(false)
let batchRecordFormListLastLoadedRouteStateKey = ''
const shouldKeepBatchRecordFormListLoadedState = (targetStateKey: string) =>
  batchRecordFormListHasLoadedRouteState.value &&
  batchRecordFormListLastLoadedRouteStateKey === targetStateKey &&
  !listLoading.value
const selectedReportId = ref('')
const previewMaximized = ref(false)
const previewFitMode = ref<'width' | 'height'>('width')
const wordImporting = ref(false)
const wordImportFileInputRef = ref<HTMLInputElement>()
const lastWordImportResult = ref<BatchRecordReportImportResultVO>()
const WORD_IMPORT_PROJECT_OPTION_PAGE_SIZE = 200
const CELL_RULES_NAVIGATION_PAGE_SIZE = 200
const DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE: BatchRecordFormSlotType = 'MAIN'
const UNIFIED_FORM_WORD_IMPORT_FORM_SLOT_TYPE: BatchRecordFormSlotType = 'FORM'
const consumedCellRulesActionKey = ref('')
const cellRulesDialog = reactive<{
  visible: boolean
  report?: RecordFormListRow
}>({
  visible: false,
  report: undefined
})
const cellRulesNavigation = reactive({
  loading: false,
  errorMessage: '',
  label: '',
  reports: [] as RecordFormListRow[]
})

const wordImportDialog = reactive({
  visible: false,
  confirming: false,
  selectedFormSlotType: DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE as BatchRecordFormSlotType,
  selectedDccProjectCodeId: undefined as number | undefined,
  formName: '',
  projectOptions: [] as DccProjectCodeRespVO[],
  projectLoading: false,
  preflightLoading: false,
  preflightErrorMessage: '',
  preflight: undefined as BatchRecordReportImportPreflightVO | undefined,
  selectedAction: 'REBUILD_V1' as BatchRecordWordImportAction,
  rebuildBatchRecord: true,
  selectedRouteProductOptionKeys: [] as string[],
  file: undefined as File | undefined
})
let preserveWordImportDialogStateOnClose = false

const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  name: '',
  productName: '',
  versionNo: normalizeRouteQueryText(route.query.versionNo),
  formSlotType: undefined as BatchRecordFormSlotType | undefined,
  latestVersionOnly: false,
  quickFilter: undefined as any
})

const recordFormDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'productName', label: '产品名称', width: 180 },
  { key: 'projectCode', label: '项目代码', width: 140 },
  { key: 'reportName', label: '表单名称', minWidth: 220 },
  { key: 'formSlotType', label: '类型', width: 120 },
  { key: 'versionNo', label: '版本', width: 110 },
  { key: 'versionStatus', label: '状态', width: 110 },
  { key: 'updateTime', label: '更新时间', width: 180 }
]

const {
  columns: recordFormColumns,
  saving: recordFormColumnSaving,
  isColumnVisible: isRecordFormColumnVisible,
  getColumnWidthString: getRecordFormColumnWidthString,
  getColumnMinWidthString: getRecordFormColumnMinWidthString,
  handleHeaderDragend: handleRecordFormHeaderDragend,
  saveConfig: saveRecordFormColumnConfig
} = useUserTableColumns('mes.pro.edhrBatch.recordFormList.projectCodeV1', recordFormDefaultColumns)

const formSlotTypeLabels: Record<BatchRecordFormSlotType, string> = {
  MAIN: '批记录',
  FORM: '表单',
  LOSS_REPORT: '损耗单',
  PROCESS_INSPECTION: '过程检验单',
  PARAMETER_RECORD: '参数记录表'
}

type VersionStatusTagType = 'success' | 'warning' | 'danger' | 'info' | 'primary'

const queryRecordFormProductNameSuggestions = async (
  queryString: string,
  callback: (items: Array<{ value: string }>) => void
) => {
  const data = await BatchRecordReportApi.getProductNameOptions(
    queryString,
    queryParams.latestVersionOnly
  )
  callback((data || []).map((productName) => ({ value: productName })))
}

const isMainWordImport = computed(() => wordImportDialog.selectedFormSlotType === 'MAIN')
const isUnifiedFormWordImport = computed(
  () => wordImportDialog.selectedFormSlotType === UNIFIED_FORM_WORD_IMPORT_FORM_SLOT_TYPE
)
const canSelectWordImportFile = computed(() =>
  isMainWordImport.value
    ? Boolean(wordImportDialog.selectedDccProjectCodeId)
    : Boolean(wordImportDialog.formName.trim())
)
const wordImportFileAccept = '.doc,.docx'

const recordFormQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'productName',
    label: '产品名称',
    type: 'autocomplete',
    queryParamKey: 'productName',
    placeholder: '请输入产品名称',
    triggerOnFocus: true,
    fetchSuggestions: queryRecordFormProductNameSuggestions
  },
  { key: 'name', label: '表单名称', type: 'text', queryParamKey: 'name', placeholder: '请输入表单名称' },
  {
    key: 'formSlotType',
    label: '类型',
    type: 'select',
    queryParamKey: 'formSlotType',
    options: [
      { label: '批记录', value: 'MAIN' },
      { label: '表单', value: 'FORM' },
      { label: '损耗单', value: 'LOSS_REPORT' },
      { label: '过程检验单', value: 'PROCESS_INSPECTION' },
      { label: '参数记录表', value: 'PARAMETER_RECORD' }
    ]
  },
  { key: 'versionNo', label: '版本', type: 'text', queryParamKey: 'versionNo', placeholder: '请输入版本号' }
]

const selectedReport = computed(
  () =>
    list.value.find((item) => item.reportId === selectedReportId.value) ||
    cellRulesNavigation.reports.find((item) => item.reportId === selectedReportId.value)
)
const selectedReportIndex = computed(() =>
  list.value.findIndex((item) => item.reportId === selectedReportId.value)
)
const canPreviewPrevious = computed(() => selectedReportIndex.value > 0)
const canPreviewNext = computed(() => selectedReportIndex.value >= 0 && selectedReportIndex.value < list.value.length - 1)
const cellRulesNavigationIndex = computed(() =>
  cellRulesNavigation.reports.findIndex(
    (item) => item.reportId === cellRulesDialog.report?.reportId
  )
)
const canNavigateCellRulesPrevious = computed(() => cellRulesNavigationIndex.value > 0)
const canNavigateCellRulesNext = computed(
  () =>
    cellRulesNavigationIndex.value >= 0 &&
    cellRulesNavigationIndex.value < cellRulesNavigation.reports.length - 1
)

const templatePreview = reactive({
  loading: false,
  reportId: '',
  errorMessage: '',
  formViewModel: undefined as EdhrBatchExecutionReviewFormViewModel | undefined,
  signatureRecords: [] as EdhrBatchExecutionReviewSignatureRecord[]
})
let recordFormListRequestSerial = 0
let recordFormSecondaryFrameId: number | undefined
let templatePreviewRequestSerial = 0
let cellRulesNavigationRequestSerial = 0

const isStaleRecordFormListRequest = (requestSerial: number) =>
  requestSerial !== recordFormListRequestSerial

const isStaleCellRulesNavigationRequest = (requestSerial: number) =>
  requestSerial !== cellRulesNavigationRequestSerial

const cancelDeferredRecordFormSecondaryLoad = () => {
  if (recordFormSecondaryFrameId === undefined) return
  cancelAnimationFrame(recordFormSecondaryFrameId)
  recordFormSecondaryFrameId = undefined
}

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (typeof error === 'string' && error.trim()) {
    return error
  }
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage
  }
  const dataMessage = (error as any)?.msg || (error as any)?.message
  if (typeof dataMessage === 'string' && dataMessage.trim()) {
    return dataMessage
  }
  return defaultMessage
}

const clearTemplatePreview = () => {
  templatePreviewRequestSerial += 1
  templatePreview.loading = false
  templatePreview.reportId = ''
  templatePreview.errorMessage = ''
  templatePreview.formViewModel = undefined
  templatePreview.signatureRecords = []
}

const buildTemplatePreviewCellValues = (
  rules: BatchRecordReportCellRuleVO[],
  sheetLayoutJson: string
) => {
  let layout: TemplateRawLayout | undefined
  try {
    layout = sheetLayoutJson.trim() ? (JSON.parse(sheetLayoutJson) as TemplateRawLayout) : undefined
  } catch (error) {
    throw new Error(resolveErrorMessage(error, '电子批记录模板布局 JSON 解析失败。'))
  }
  return rules
    .filter((rule) => {
      const rawText = layout?.rows?.[String(rule.rowIndex)]?.cells?.[
        String(rule.columnIndex)
      ]?.text
      return typeof rawText !== 'string' || !rawText.trim()
    })
    .map((rule) => ({
      rowIndex: rule.rowIndex,
      columnIndex: rule.columnIndex,
      valueType: 'STRING',
      value: '?',
      valueDisplay: '?'
    }))
}

const loadSelectedReportTemplate = async (
  row: BatchRecordReportVO,
  listRequestSerial?: number
) => {
  if (
    selectedReportId.value !== row.reportId ||
    (listRequestSerial !== undefined && isStaleRecordFormListRequest(listRequestSerial))
  ) {
    return
  }
  const requestSerial = ++templatePreviewRequestSerial
  templatePreview.loading = true
  templatePreview.reportId = row.reportId
  templatePreview.errorMessage = ''
  templatePreview.formViewModel = undefined
  templatePreview.signatureRecords = []
  try {
    const [cellRuleResp, markerResp] = await Promise.all([
      BatchRecordReportApi.getCellRules(row.reportId),
      BatchRecordReportApi.getSignatureCellMarkers(row.reportId)
    ])
    if (
      requestSerial !== templatePreviewRequestSerial ||
      selectedReportId.value !== row.reportId ||
      (listRequestSerial !== undefined && isStaleRecordFormListRequest(listRequestSerial))
    ) {
      return
    }
    const rawRules = cellRuleResp.suggestions?.length ? cellRuleResp.suggestions : cellRuleResp.rules || []
    const rules = rawRules
      .map(normalizeCellRule)
      .sort((left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex)
    const sheetLayoutJson = cellRuleResp.sheetLayoutJson || markerResp.sheetLayoutJson || ''
    if (!sheetLayoutJson.trim()) {
      throw new Error('缺少电子批记录模板布局，无法显示表单模板。')
    }
    if (!rules.length) {
      throw new Error('模板缺少单元格规则，无法显示表单模板。')
    }
    templatePreview.formViewModel = {
      sheetLayoutJson,
      executionSnapshotJson: JSON.stringify({
        fields: rules
          .filter((rule) => Boolean(cleanedAttachmentRule(rule.attachmentRule)))
          .map((rule) => ({
            rowIndex: rule.rowIndex,
            columnIndex: rule.columnIndex,
            attachmentRule: cleanedAttachmentRule(rule.attachmentRule)
          }))
      }),
      cellValuesJson: JSON.stringify(buildTemplatePreviewCellValues(rules, sheetLayoutJson)),
      remark: '',
      signatureCellMarkers: markerResp.markers || []
    }
    templatePreview.signatureRecords = []
  } catch (error) {
    if (
      requestSerial !== templatePreviewRequestSerial ||
      selectedReportId.value !== row.reportId ||
      (listRequestSerial !== undefined && isStaleRecordFormListRequest(listRequestSerial))
    ) {
      return
    }
    templatePreview.errorMessage = resolveErrorMessage(
      error,
      '表单模板加载失败，请联系管理员检查模板布局和单元格规则。'
    )
  } finally {
    if (
      requestSerial === templatePreviewRequestSerial &&
      selectedReportId.value === row.reportId &&
      (listRequestSerial === undefined || !isStaleRecordFormListRequest(listRequestSerial))
    ) {
      templatePreview.loading = false
    }
  }
}

type BatchRecordTemplateAction = 'signature' | 'cellRules'

const normalizeTemplateAction = (value: unknown): BatchRecordTemplateAction | '' => {
  const action = normalizeRouteQueryText(value)
  return action === 'signature' || action === 'cellRules' ? action : ''
}

const buildCellRulesActionKey = (reportId: string) => `${reportId}:cellRules`

const normalizeCellRulesNavigationText = (value: unknown) =>
  typeof value === 'string' && value.trim() ? value.trim() : ''

const buildCellRulesNavigationLabel = (sourceReport: BatchRecordReportVO) => {
  const productName = normalizeCellRulesNavigationText(sourceReport.productName)
  const versionNo = normalizeCellRulesNavigationText(sourceReport.versionNo)
  if (productName && versionNo) return `${productName} / ${versionNo}`
  return sourceReport.reportName || sourceReport.batchRecordName || sourceReport.reportId || '同版本表单'
}

const resetCellRulesNavigation = (sourceReport?: BatchRecordReportVO) => {
  cellRulesNavigation.loading = false
  cellRulesNavigation.errorMessage = ''
  cellRulesNavigation.label = sourceReport ? buildCellRulesNavigationLabel(sourceReport) : ''
  cellRulesNavigation.reports = []
}

const loadCellRulesNavigationReports = async (sourceReport: BatchRecordReportVO) => {
  const requestSerial = ++cellRulesNavigationRequestSerial
  resetCellRulesNavigation(sourceReport)
  const productName = normalizeCellRulesNavigationText(sourceReport.productName)
  const versionNo = normalizeCellRulesNavigationText(sourceReport.versionNo)
  if (!productName || !versionNo) {
    cellRulesNavigation.errorMessage = '当前表单缺少产品名称或版本号，无法切换同版本表单。'
    return
  }

  cellRulesNavigation.loading = true
  try {
    const sourceBatchRecordVersionId = Number(sourceReport.batchRecordVersionId)
    const shouldFilterByBatchRecordVersionId =
      Number.isFinite(sourceBatchRecordVersionId) && sourceBatchRecordVersionId > 0
    const allReports: RecordFormListRow[] = []
    let pageNo = 1
    let total = 0
    do {
      const data = await BatchRecordReportApi.getGeneratedReportPage({
        pageNo,
        pageSize: CELL_RULES_NAVIGATION_PAGE_SIZE,
        productName: sourceReport.productName,
        versionNo: sourceReport.versionNo
      })
      if (isStaleCellRulesNavigationRequest(requestSerial)) return
      if (!Array.isArray(data.list)) {
        throw new Error('同产品同版本表单列表响应缺少 list。')
      }
      const rows = data.list
      allReports.push(...rows.map((row, index) => toRecordFormRow(row, allReports.length + index)))
      total = Number(data.total) || allReports.length
      if (rows.length === 0) break
      pageNo += 1
    } while (allReports.length < total)

    const reportMap = new Map<string, RecordFormListRow>()
    allReports
      .filter((row) => normalizeCellRulesNavigationText(row.productName) === productName)
      .filter((row) => normalizeCellRulesNavigationText(row.versionNo) === versionNo)
      .filter(
        (row) =>
          !shouldFilterByBatchRecordVersionId ||
          Number(row.batchRecordVersionId) === sourceBatchRecordVersionId
      )
      .forEach((row) => {
        if (row.reportId && !reportMap.has(row.reportId)) {
          reportMap.set(row.reportId, row)
        }
      })

    const nextReports = Array.from(reportMap.values())
    if (!nextReports.some((row) => row.reportId === sourceReport.reportId)) {
      throw new Error('同产品同版本候选列表中未包含当前表单，无法安全切换。')
    }
    if (isStaleCellRulesNavigationRequest(requestSerial)) return
    cellRulesNavigation.reports = nextReports
  } catch (error) {
    if (isStaleCellRulesNavigationRequest(requestSerial)) return
    cellRulesNavigation.reports = []
    cellRulesNavigation.errorMessage = resolveErrorMessage(
      error,
      '同产品同版本表单列表加载失败，无法切换。'
    )
  } finally {
    if (!isStaleCellRulesNavigationRequest(requestSerial)) {
      cellRulesNavigation.loading = false
    }
  }
}

const openCellRulesDialog = (row: BatchRecordReportVO) => {
  const reportId = String(row.reportId || '').trim()
  if (!reportId) {
    throw new Error('缺少有效表单ID，无法确认填写规则。')
  }
  consumedCellRulesActionKey.value = buildCellRulesActionKey(reportId)
  cellRulesDialog.report = row as RecordFormListRow
  cellRulesDialog.visible = true
  void loadCellRulesNavigationReports(row)
}

const handleTemplateActionQuery = async () => {
  const action = normalizeTemplateAction(route.query.action)
  if (action !== 'cellRules') {
    consumedCellRulesActionKey.value = ''
    return
  }
  const reportId = normalizeRouteQueryText(route.query.reportId)
  if (!reportId) return
  const actionKey = buildCellRulesActionKey(reportId)
  if (consumedCellRulesActionKey.value === actionKey) return
  const row = list.value.find((item) => item.reportId === reportId)
  if (!row) return
  openCellRulesDialog(row)
}

const handleCellRulesConfirmed = async (data: BatchRecordReportCellRulesRespVO) => {
  const report =
    list.value.find((item) => item.reportId === data.reportId) ||
    cellRulesNavigation.reports.find((item) => item.reportId === data.reportId)
  if (report) {
    await loadSelectedReportTemplate(report)
  }
}

const navigateCellRulesDialog = async (offset: -1 | 1) => {
  if (cellRulesNavigation.loading) return
  const nextReport = cellRulesNavigation.reports[cellRulesNavigationIndex.value + offset]
  if (!nextReport) return
  cellRulesDialog.report = nextReport
  selectedReportId.value = nextReport.reportId
  await loadSelectedReportTemplate(nextReport)
}

const toRecordFormRow = (row: BatchRecordReportVO, index: number): RecordFormListRow => ({
  ...row,
  rowKey: `${row.reportId || 'report'}:${row.productName || 'no-product'}:${index}`
})

const loadRecordFormSecondaryData = async (
  selectedRow: RecordFormListRow | undefined,
  requestSerial: number
) => {
  try {
    await Promise.all([
      selectedRow
        ? loadSelectedReportTemplate(selectedRow, requestSerial)
        : Promise.resolve(),
      handleTemplateActionQuery()
    ])
  } catch (error) {
    if (isStaleRecordFormListRequest(requestSerial)) return
    templatePreview.errorMessage = resolveErrorMessage(
      error,
      '批记录表单辅助数据加载失败，请联系管理员检查报表预览链路。'
    )
  }
}

const deferRecordFormSecondaryLoad = (
  selectedRow: RecordFormListRow | undefined,
  requestSerial: number
) => {
  cancelDeferredRecordFormSecondaryLoad()
  recordFormSecondaryFrameId = requestAnimationFrame(() => {
    recordFormSecondaryFrameId = undefined
    if (isStaleRecordFormListRequest(requestSerial)) return
    void loadRecordFormSecondaryData(selectedRow, requestSerial)
  })
}

const getList = async () => {
  const requestSerial = ++recordFormListRequestSerial
  const targetRouteStateKey = buildBatchRecordFormListRouteStateKey()
  cancelDeferredRecordFormSecondaryLoad()
  listLoading.value = true
  listErrorMessage.value = ''
  try {
    const data = await BatchRecordReportApi.getGeneratedReportPage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      reportId: normalizeRouteQueryText(route.query.reportId) || undefined,
      name: queryParams.name || undefined,
      productName: queryParams.productName || undefined,
      versionNo: queryParams.versionNo || undefined,
      formSlotType: queryParams.formSlotType || undefined,
      latestVersionOnly: queryParams.latestVersionOnly || undefined
    })
    if (isStaleRecordFormListRequest(requestSerial)) return
    const nextList = (Array.isArray(data.list) ? data.list : []).map(toRecordFormRow)
    list.value = nextList
    total.value = Number(data.total) || 0
    const nextSelected =
      nextList.find((item) => item.reportId === selectedReportId.value) || nextList[0]
    if (nextSelected) {
      selectedReportId.value = nextSelected.reportId
      clearTemplatePreview()
      templatePreview.loading = true
      templatePreview.reportId = nextSelected.reportId
    } else {
      selectedReportId.value = ''
      clearTemplatePreview()
    }
    deferRecordFormSecondaryLoad(nextSelected, requestSerial)
    batchRecordFormListLastLoadedRouteStateKey = targetRouteStateKey
    batchRecordFormListHasLoadedRouteState.value = true
  } catch (error) {
    if (isStaleRecordFormListRequest(requestSerial)) return
    list.value = []
    total.value = 0
    selectedReportId.value = ''
    clearTemplatePreview()
    listErrorMessage.value = resolveErrorMessage(error, '批记录表单列表加载失败，请联系管理员检查报表目录链路。')
  } finally {
    if (!isStaleRecordFormListRequest(requestSerial)) {
      listLoading.value = false
    }
  }
}

const recordFormQuickFilter = useTableQuickFilter(
  'mes.pro.edhrBatch.recordFormList',
  recordFormQuickFilterDefinitions,
  queryParams,
  getList
)

const handleLatestVersionOnlyChange = async () => {
  queryParams.pageNo = 1
  await getList()
}

const enterPreviewMaximize = () => {
  if (!selectedReport.value || !templatePreview.formViewModel || templatePreview.loading) return
  previewFitMode.value = 'width'
  previewMaximized.value = true
}

const restorePreviewLayout = () => {
  previewMaximized.value = false
}

const setPreviewFitMode = (mode: 'width' | 'height') => {
  previewFitMode.value = mode
}

const selectPreviewNeighbor = async (offset: number) => {
  const nextReport = list.value[selectedReportIndex.value + offset]
  if (!nextReport) return
  await selectReport(nextReport)
}

const selectReport = async (row: RecordFormListRow) => {
  selectedReportId.value = row.reportId
  await loadSelectedReportTemplate(row)
}

const resolveFormSlotTypeLabel = (formSlotType?: BatchRecordFormSlotType) =>
  formSlotTypeLabels[formSlotType || 'MAIN'] || formSlotType || '-'

const formatNullableDate = (value?: Date | string | number) => formatDateTimeValue(value, '-')

const resolveVersionStatusPresentation = (status?: string): { label: string; type: VersionStatusTagType } => {
  const statusMap: Record<string, { label: string; type: VersionStatusTagType }> = {
    DRAFT: { label: '审批中', type: 'warning' },
    PRECHECK_PASSED: { label: '审批中', type: 'warning' },
    PENDING_APPROVAL: { label: '审批中', type: 'warning' },
    APPROVED: { label: '已发布', type: 'success' },
    REJECTED: { label: '已驳回', type: 'danger' },
    VOIDED: { label: '已作废', type: 'info' },
    OBSOLETE: { label: '已作废', type: 'info' }
  }
  return status ? statusMap[status] || { label: status, type: 'info' } : { label: '-', type: 'info' }
}

const resolveVersionStatusText = (status?: string) => resolveVersionStatusPresentation(status).label

const submitImportedVersionApproval = (
  result: BatchRecordReportImportResultVO,
  subjectName = '批记录'
) => {
  if (!result.batchRecordVersionId || !result.sourceBatchRecordVersionId) {
    return ''
  }
  const upgradeSubject = `${subjectName}升版`
  if (result.versionStatus === 'APPROVED') {
    return `，${upgradeSubject} ${result.versionNo || result.batchRecordVersionId} 已生效（${resolveVersionStatusText(result.versionStatus)}）`
  }
  if (result.versionStatus !== 'PENDING_APPROVAL') {
    throw new Error(
      `${upgradeSubject}导入未自动提交升版审批，当前状态为 ${resolveVersionStatusText(result.versionStatus)}。`
    )
  }
  return `，已自动提交升版审批 ${result.versionNo || result.batchRecordVersionId}（${resolveVersionStatusText(result.versionStatus)}）`
}

const resolveWordImportRouteKey = (fileName: string) => {
  if (/[（(\s-]E\s*1[）)\s-]/i.test(` ${fileName} `)) {
    return 'E'
  }
  return 'B'
}

const formatWordImportVersion = (version?: string) => version || '无'

const hasVisibleCurrentBatchRecordVersion = (preflight?: BatchRecordReportImportPreflightVO) =>
  Boolean(preflight?.currentBatchRecordVersionNo && preflight.currentBatchRecordVersionId)

const formatWordImportCurrentBatchRecordVersion = (
  preflight?: BatchRecordReportImportPreflightVO
) =>
  hasVisibleCurrentBatchRecordVersion(preflight)
    ? formatWordImportVersion(preflight?.currentBatchRecordVersionNo)
    : '无'

const hasVisibleLatestBatchRecordVersion = (preflight?: BatchRecordReportImportPreflightVO) =>
  Boolean(preflight?.latestBatchRecordVersionNo && preflight.latestBatchRecordVersionId)

const formatWordImportLatestBatchRecordVersion = (
  preflight?: BatchRecordReportImportPreflightVO
) =>
  hasVisibleLatestBatchRecordVersion(preflight)
    ? formatWordImportVersion(preflight?.latestBatchRecordVersionNo)
    : formatWordImportCurrentBatchRecordVersion(preflight)

const isWordImportActionAllowed = (action: BatchRecordWordImportAction) =>
  wordImportDialog.preflight?.allowedActions?.includes(action) ?? action === 'REBUILD_V1'

const hasWordImportAllowedAction = computed(() =>
  Boolean(wordImportDialog.preflight?.allowedActions?.length)
)

const resolveWordImportActionLockedMessage = (preflight?: BatchRecordReportImportPreflightVO) => {
  if (isWordImportRouteCandidateLocked(preflight)) {
    return `工艺路线候选版本 ${preflight?.currentRouteCandidateVersionNo || ''} 当前为${preflight?.currentRouteCandidateVersionStatus === 'PENDING_APPROVAL' ? '待审批' : '待发布'}状态，请先撤回、取消或完成发布后再导入。`
  }
  if (preflight?.latestBatchRecordVersionStatus === 'PENDING_APPROVAL') {
    return '当前批记录存在待审批升版申请，只能等待审批完成或撤回升版申请。'
  }
  return '当前预检状态不允许继续导入，请先处理对应状态后重试。'
}

const resolveWordImportUpgradeVersionMessage = (
  batchRecordName: string,
  preflight: BatchRecordReportImportPreflightVO
) => {
  const currentVersion = formatWordImportCurrentBatchRecordVersion(preflight)
  const latestVersion = formatWordImportLatestBatchRecordVersion(preflight)
  const nextVersion = preflight.nextVersionNo || '下一版本'
  if (latestVersion !== currentVersion) {
    return `批记录「${batchRecordName}」已存在同名批记录。是否升版本：最新批记录版本为 ${latestVersion}，当前生效源版本为 ${currentVersion}，确认后将生成 ${nextVersion}。`
  }
  return `批记录「${batchRecordName}」已存在同名批记录。是否升版本：当前版本为 ${currentVersion}，确认后将生成 ${nextVersion}。`
}

const isWordImportRouteDuplicateBlocked = (preflight?: BatchRecordReportImportPreflightVO) =>
  preflight?.routeGovernanceStatus === 'DUPLICATE_BLOCKED'

const isWordImportRouteCandidateLocked = (preflight?: BatchRecordReportImportPreflightVO) =>
  preflight?.currentRouteCandidateVersionStatus === 'PENDING_APPROVAL'
  || preflight?.currentRouteCandidateVersionStatus === 'READY_TO_PUBLISH'

const isWordImportRouteDraftCandidate = (preflight?: BatchRecordReportImportPreflightVO) =>
  preflight?.currentRouteCandidateVersionStatus === 'DRAFT'

const formatWordImportDuplicateRoutes = (preflight?: BatchRecordReportImportPreflightVO) =>
  preflight?.duplicateRoutes
    ?.map((route) => route.routeCode || route.routeId)
    .filter(Boolean)
    .join('、') || '无'

const resolveWordImportRouteUpgradeMessage = (
  batchRecordName: string,
  preflight: BatchRecordReportImportPreflightVO
) => {
  if (isWordImportRouteDraftCandidate(preflight)) {
    const candidateVersionNo = preflight.currentRouteCandidateVersionNo || '候选版本'
    return `工艺路线“${preflight.currentRouteName || batchRecordName}”当前已有 ${candidateVersionNo} 草稿。确认后将按 Word 工序顺序更新现有 ${candidateVersionNo} 草稿，不会创建下一版本；草稿发布后才生效，当前生效路线不会被覆盖。`
  }
  return `所选 DCC 项目代码已正式绑定工艺路线“${preflight.currentRouteName || batchRecordName}”（${preflight.currentRouteCode || '无编码'}，${preflight.currentRouteVersionNo || '无版本'}）。确认后将按 Word 工序顺序生成/更新路线候选版本，发布后才生效；当前生效路线不会被覆盖。`
}

const resolveWordImportRouteUpgradeDialogTitle = (preflight?: BatchRecordReportImportPreflightVO) =>
  isWordImportRouteDraftCandidate(preflight) ? '确认更新路线草稿' : '确认生成路线候选版本'

const resolveWordImportRouteUpgradeConfirmText = (preflight?: BatchRecordReportImportPreflightVO) =>
  isWordImportRouteDraftCandidate(preflight)
    ? `更新 ${preflight?.currentRouteCandidateVersionNo || '候选版本'} 草稿`
    : '生成候选版本'

const resolveWordImportBatchRecordBindingCandidateMessage = (
  batchRecordName: string,
  preflight: BatchRecordReportImportPreflightVO
) => {
  const candidateVersionNo = preflight.currentRouteCandidateVersionNo || '候选版本'
  const routeName = preflight.currentRouteName || batchRecordName
  if (isWordImportRouteDraftCandidate(preflight)) {
    return `工艺路线“${routeName}”当前已有 ${candidateVersionNo} 草稿。本次未勾选“工艺流程”，确认后仅更新批记录表单绑定候选；候选沿用当前工艺流程节点和流程关系，不按 Word 重建工艺流程，发布后才生效。`
  }
  return `所选 DCC 项目代码已正式绑定工艺路线“${routeName}”。本次未勾选“工艺流程”，确认后仅生成批记录表单绑定候选；候选沿用当前工艺流程节点和流程关系，不按 Word 重建工艺流程，发布后才生效。`
}

const resolveWordImportBatchRecordBindingCandidateDialogTitle = (
  preflight?: BatchRecordReportImportPreflightVO
) => isWordImportRouteDraftCandidate(preflight) ? '确认更新批记录绑定草稿' : '确认生成批记录绑定候选'

const resolveWordImportBatchRecordBindingCandidateConfirmText = (
  preflight?: BatchRecordReportImportPreflightVO
) => isWordImportRouteDraftCandidate(preflight) ? '更新绑定草稿' : '生成绑定候选'

const addWordImportRouteUpgradeKey = (
  routeUpgradeKeys: Set<string>,
  keyType: string,
  keyValue?: number | string
) => {
  const normalizedValue = keyValue === undefined || keyValue === null ? '' : String(keyValue).trim()
  if (normalizedValue) {
    routeUpgradeKeys.add(`${keyType}:${normalizedValue}`)
  }
}

const addWordImportRouteNameVersionKey = (
  routeUpgradeKeys: Set<string>,
  routeName?: string,
  routeVersionNo?: string
) => {
  const normalizedRouteName = routeName?.trim()
  const normalizedRouteVersionNo = routeVersionNo?.trim()
  if (normalizedRouteName && normalizedRouteVersionNo) {
    routeUpgradeKeys.add(`route-name-version:${normalizedRouteName}:${normalizedRouteVersionNo}`)
  }
}

const collectWordImportCurrentRouteUpgradeKeys = (
  preflight?: BatchRecordReportImportPreflightVO
) => {
  const routeUpgradeKeys = new Set<string>()
  if (!preflight) {
    return []
  }
  addWordImportRouteUpgradeKey(routeUpgradeKeys, 'route-version-id', preflight.currentRouteVersionId)
  addWordImportRouteUpgradeKey(routeUpgradeKeys, 'route-id', preflight.currentRouteId)
  addWordImportRouteNameVersionKey(
    routeUpgradeKeys,
    preflight.currentRouteName || preflight.batchRecordName,
    preflight.currentRouteVersionNo
  )
  if (preflight.currentRouteCode && preflight.currentRouteVersionNo) {
    routeUpgradeKeys.add(`route-code-version:${preflight.currentRouteCode}:${preflight.currentRouteVersionNo}`)
  }
  return Array.from(routeUpgradeKeys)
}

const collectWordImportRouteProductUpgradeKeys = (
  option: BatchRecordReportImportRouteProductOptionVO
) => {
  const routeUpgradeKeys = new Set<string>()
  addWordImportRouteUpgradeKey(routeUpgradeKeys, 'route-version-id', option.routeVersionId)
  addWordImportRouteUpgradeKey(routeUpgradeKeys, 'route-id', option.routeId)
  addWordImportRouteNameVersionKey(
    routeUpgradeKeys,
    option.routeName || option.productName,
    option.routeVersionNo
  )
  if (option.routeCode && option.routeVersionNo) {
    routeUpgradeKeys.add(`route-code-version:${option.routeCode}:${option.routeVersionNo}`)
  }
  return Array.from(routeUpgradeKeys)
}

const resetWordImportPreflightState = () => {
  wordImportDialog.preflightLoading = false
  wordImportDialog.preflightErrorMessage = ''
  wordImportDialog.preflight = undefined
  wordImportDialog.selectedAction = 'REBUILD_V1'
  wordImportDialog.rebuildBatchRecord = true
  wordImportDialog.selectedRouteProductOptionKeys = []
}

const resetWordImportDialog = () => {
  if (preserveWordImportDialogStateOnClose) {
    preserveWordImportDialogStateOnClose = false
    return
  }
  if (wordImportDialog.visible) {
    return
  }
  wordImportDialog.confirming = false
  wordImportDialog.selectedFormSlotType = DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE
  wordImportDialog.selectedDccProjectCodeId = undefined
  wordImportDialog.formName = ''
  wordImportDialog.projectOptions = []
  wordImportDialog.projectLoading = false
  resetWordImportPreflightState()
  wordImportDialog.file = undefined
}

const closeWordImportDialogBeforeMessageBox = async () => {
  if (!wordImportDialog.visible) {
    return false
  }
  preserveWordImportDialogStateOnClose = true
  wordImportDialog.visible = false
  await nextTick()
  await new Promise<void>((resolve) => window.setTimeout(resolve, 320))
  return true
}

const clearWordImportState = () => {
  if (wordImportFileInputRef.value) {
    wordImportFileInputRef.value.value = ''
  }
  wordImportDialog.file = undefined
}

const getSelectedWordImportProject = () => {
  const selectedId = wordImportDialog.selectedDccProjectCodeId
  if (!selectedId) {
    return undefined
  }
  return wordImportDialog.projectOptions.find((item) => item.id === selectedId)
}

const resolveWordImportSubjectName = () => {
  if (isMainWordImport.value) {
    return getSelectedWordImportProject()?.projectName?.trim() || ''
  }
  return wordImportDialog.formName.trim()
}

const resolveWordImportSubjectRequiredMessage = () =>
  isMainWordImport.value ? '请选择产品名称。' : '请输入表单名称。'

const resolveWordImportSubjectTooLongMessage = () =>
  isMainWordImport.value ? '产品名称不能超过 100 个字符。' : '表单名称不能超过 100 个字符。'

const resolveExtraFormSlotSubjectLabel = (formSlotType: BatchRecordFormSlotType) =>
  formSlotType === UNIFIED_FORM_WORD_IMPORT_FORM_SLOT_TYPE ? '表单名称' : '产品名称'

const loadWordImportProjectOptions = async (keyword = '') => {
  const trimmedKeyword = keyword.trim()
  wordImportDialog.projectLoading = true
  try {
    const options: DccProjectCodeRespVO[] = []
    let currentPageNo = 1
    let totalCount = 0

    do {
      const data = await getProjectCodePage({
        pageNo: currentPageNo,
        pageSize: WORD_IMPORT_PROJECT_OPTION_PAGE_SIZE,
        projectName: trimmedKeyword || undefined
      })
      const pageList = Array.isArray(data.list) ? data.list : []
      totalCount = Number(data.total) || 0
      options.push(...pageList)
      if (!pageList.length) break
      currentPageNo += 1
    } while (options.length < totalCount)

    const selectedProject = getSelectedWordImportProject()
    const optionMap = new Map<number, DccProjectCodeRespVO>()
    if (selectedProject?.id) {
      optionMap.set(selectedProject.id, selectedProject)
    }
    options.forEach((item) => {
      if (item.id && !optionMap.has(item.id)) {
        optionMap.set(item.id, item)
      }
    })
    wordImportDialog.projectOptions = Array.from(optionMap.values())
  } catch (error) {
    message.error(resolveErrorMessage(error, 'DCC 项目名称候选加载失败，请联系管理员。'))
  } finally {
    wordImportDialog.projectLoading = false
  }
}

const handleWordImportTypeChange = async () => {
  clearWordImportState()
  resetWordImportPreflightState()
  if (isMainWordImport.value && wordImportDialog.visible && wordImportDialog.projectOptions.length === 0) {
    await loadWordImportProjectOptions()
  }
}

const handleUnifiedFormNameInput = () => {
  if (!isUnifiedFormWordImport.value) {
    return
  }
  clearWordImportState()
  resetWordImportPreflightState()
}

const loadWordImportPreflight = async () => {
  const file = wordImportDialog.file
  const selectedProject = getSelectedWordImportProject()
  const selectedProjectName = selectedProject?.projectName?.trim() || ''
  const selectedDccProjectCodeId = selectedProject?.id
  if (!isMainWordImport.value || !file || !selectedProjectName || !selectedDccProjectCodeId) {
    resetWordImportPreflightState()
    return
  }
  wordImportDialog.preflightLoading = true
  wordImportDialog.preflightErrorMessage = ''
  try {
    const productNames = [selectedProjectName]
    const preflight = await BatchRecordReportApi.preflightUploadedRoute(
      resolveWordImportRouteKey(file.name),
      selectedProjectName,
      selectedDccProjectCodeId,
      productNames
    )
    wordImportDialog.preflight = preflight
    wordImportDialog.selectedAction = preflight.recommendedAction || 'REBUILD_V1'
    wordImportDialog.rebuildBatchRecord = true
    wordImportDialog.selectedRouteProductOptionKeys = []
  } catch (error) {
    const errorMessage = resolveErrorMessage(error, '导入 Word 预检失败，请联系管理员。')
    wordImportDialog.preflight = undefined
    wordImportDialog.preflightErrorMessage = errorMessage
    message.error(errorMessage)
  } finally {
    wordImportDialog.preflightLoading = false
  }
}

const buildWordImportSelection = () => {
  const preflight = wordImportDialog.preflight
  const selectedKeys = new Set(wordImportDialog.selectedRouteProductOptionKeys)
  const selectedOptions = preflight?.routeProductOptions.filter((option) => selectedKeys.has(option.optionKey)) || []
  const selectedRouteProductIds = selectedOptions
    .map((option) => option.routeProductId)
    .filter((routeProductId): routeProductId is number => typeof routeProductId === 'number')
  const selectedProductNames = selectedOptions
    .filter((option) => !option.routeProductId)
    .map((option) => option.productName)
    .filter(Boolean)
  return {
    importAction: wordImportDialog.selectedAction,
    expectedSourceVersionId: wordImportDialog.selectedAction === 'UPGRADE'
      ? wordImportDialog.preflight?.currentBatchRecordVersionId
      : undefined,
    expectedTargetVersionNo: wordImportDialog.selectedAction === 'UPGRADE'
      ? wordImportDialog.preflight?.nextVersionNo
      : undefined,
    rebuildBatchRecord: wordImportDialog.rebuildBatchRecord,
    routeUpgradeRequired: wordImportDialog.preflight?.routeUpgradeRequired,
    expectedRouteId: wordImportDialog.preflight?.currentRouteId,
    expectedRouteVersionId: wordImportDialog.preflight?.currentRouteVersionId,
    expectedRouteCandidateVersionId: wordImportDialog.preflight?.currentRouteCandidateVersionId,
    selectedOptions,
    selectedRouteProductIds,
    selectedProductNames
  }
}

type WordImportSelection = ReturnType<typeof buildWordImportSelection>

type WordImportConfirmedSelection = {
  importAction: BatchRecordWordImportAction
  expectedSourceVersionId?: number
  expectedTargetVersionNo?: string
  rebuildBatchRecord: boolean
  routeUpgradeConfirmed?: boolean
  expectedRouteId?: number
  expectedRouteVersionId?: number
  expectedRouteCandidateVersionId?: number
  selectedRouteProductIds: number[]
  selectedProductNames: string[]
}

const buildWordImportConfirmedSelection = (
  selection: WordImportSelection,
  rebuildBatchRecord: boolean,
  selectedOptions: BatchRecordReportImportRouteProductOptionVO[]
): WordImportConfirmedSelection => {
  const routeFlowRebuildRequested = selectedOptions.length > 0
  const batchRecordBindingCandidateRequested = Boolean(
    selection.routeUpgradeRequired && rebuildBatchRecord && !routeFlowRebuildRequested
  )
  const shouldConfirmRouteUpgrade = Boolean(
    selection.routeUpgradeRequired && (routeFlowRebuildRequested || batchRecordBindingCandidateRequested)
  )
  return {
    importAction: selection.importAction,
    expectedSourceVersionId: selection.expectedSourceVersionId,
    expectedTargetVersionNo: selection.expectedTargetVersionNo,
    rebuildBatchRecord,
    routeUpgradeConfirmed: shouldConfirmRouteUpgrade,
    expectedRouteId: shouldConfirmRouteUpgrade ? selection.expectedRouteId : undefined,
    expectedRouteVersionId: shouldConfirmRouteUpgrade ? selection.expectedRouteVersionId : undefined,
    expectedRouteCandidateVersionId: shouldConfirmRouteUpgrade
      ? selection.expectedRouteCandidateVersionId
      : undefined,
    selectedRouteProductIds: selectedOptions
      .map((option) => option.routeProductId)
      .filter((routeProductId): routeProductId is number => typeof routeProductId === 'number'),
    selectedProductNames: selectedOptions
      .filter((option) => !option.routeProductId)
      .map((option) => option.productName)
      .filter(Boolean)
  }
}

const confirmWordImportUpgradeSelections = async (
  batchRecordName: string,
  selection: WordImportSelection
) => {
  const preflight = wordImportDialog.preflight
  if (!preflight) {
    return false
  }
  const rebuildBatchRecord = selection.rebuildBatchRecord
  const routeFlowRebuildRequested = selection.selectedOptions.length > 0
  const batchRecordBindingCandidateRequested = Boolean(
    selection.routeUpgradeRequired && rebuildBatchRecord && !routeFlowRebuildRequested
  )
  const selectedOptions: BatchRecordReportImportRouteProductOptionVO[] = []
  const confirmedRouteUpgradeKeys = new Set<string>()
  const skippedRouteUpgradeKeys = new Set<string>()
  const shouldConfirmRouteUpgrade = Boolean(
    selection.routeUpgradeRequired && (routeFlowRebuildRequested || batchRecordBindingCandidateRequested)
  )
  if (isWordImportRouteDuplicateBlocked(preflight)) {
    message.warning(`所选 DCC 项目代码存在多条正式路线绑定：${formatWordImportDuplicateRoutes(preflight)}，请先清理为唯一绑定。`)
    return false
  }
  if (isWordImportRouteCandidateLocked(preflight)) {
    message.warning(resolveWordImportActionLockedMessage(preflight))
    return false
  }
  if (shouldConfirmRouteUpgrade) {
    try {
      await ElMessageBox.confirm(
        batchRecordBindingCandidateRequested
          ? resolveWordImportBatchRecordBindingCandidateMessage(batchRecordName, preflight)
          : resolveWordImportRouteUpgradeMessage(batchRecordName, preflight),
        batchRecordBindingCandidateRequested
          ? resolveWordImportBatchRecordBindingCandidateDialogTitle(preflight)
          : resolveWordImportRouteUpgradeDialogTitle(preflight),
        {
          confirmButtonText: batchRecordBindingCandidateRequested
            ? resolveWordImportBatchRecordBindingCandidateConfirmText(preflight)
            : resolveWordImportRouteUpgradeConfirmText(preflight),
          cancelButtonText: '退出导入',
          distinguishCancelAndClose: true,
          type: 'warning'
        }
      )
      if (routeFlowRebuildRequested) {
        collectWordImportCurrentRouteUpgradeKeys(wordImportDialog.preflight).forEach((routeUpgradeKey) => {
          confirmedRouteUpgradeKeys.add(routeUpgradeKey)
        })
      }
    } catch {
      return false
    }
  }
  if (rebuildBatchRecord && wordImportDialog.selectedAction === 'REBUILD_V1'
    && wordImportDialog.preflight?.currentBatchRecordHasMainReports) {
    message.warning(`批记录「${batchRecordName}」主批记录已上传，请先删除后重新上传。`)
    return false
  }
  if (rebuildBatchRecord && wordImportDialog.selectedAction === 'UPGRADE'
    && wordImportDialog.preflight?.currentBatchRecordVersionNo) {
    try {
      await ElMessageBox.confirm(
        resolveWordImportUpgradeVersionMessage(batchRecordName, wordImportDialog.preflight),
        '确认批记录升版',
        {
          confirmButtonText: '升版',
          cancelButtonText: '退出导入',
          distinguishCancelAndClose: true,
          type: 'warning'
        }
      )
    } catch {
      return false
    }
  }
  for (const option of selection.selectedOptions) {
    if (!option.routeVersionNo) {
      selectedOptions.push(option)
      continue
    }
    const routeUpgradeKeys = collectWordImportRouteProductUpgradeKeys(option)
    if (routeUpgradeKeys.some((routeUpgradeKey) => confirmedRouteUpgradeKeys.has(routeUpgradeKey))) {
      selectedOptions.push(option)
      continue
    }
    if (routeUpgradeKeys.some((routeUpgradeKey) => skippedRouteUpgradeKeys.has(routeUpgradeKey))) {
      continue
    }
    try {
      await ElMessageBox.confirm(
        `产线「${option.productName}」当前版本为 ${option.routeVersionNo}，确认后将生成路线候选版本，待审批/发布后生效。`,
        '确认生成路线候选版本',
        {
          confirmButtonText: '生成候选版本',
          cancelButtonText: '跳过该项',
          distinguishCancelAndClose: true,
          type: 'warning'
        }
      )
      selectedOptions.push(option)
      routeUpgradeKeys.forEach((routeUpgradeKey) => {
        confirmedRouteUpgradeKeys.add(routeUpgradeKey)
      })
    } catch (action) {
      if (action === 'close') {
        return false
      }
      routeUpgradeKeys.forEach((routeUpgradeKey) => {
        skippedRouteUpgradeKeys.add(routeUpgradeKey)
      })
    }
  }
  return buildWordImportConfirmedSelection({
    ...selection,
    importAction: selection.importAction,
    expectedSourceVersionId: selection.expectedSourceVersionId,
    expectedTargetVersionNo: selection.expectedTargetVersionNo
  }, rebuildBatchRecord, selectedOptions)
}

const openWordImportDialog = async () => {
  wordImportDialog.selectedFormSlotType = DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE
  wordImportDialog.selectedDccProjectCodeId = undefined
  wordImportDialog.formName = ''
  wordImportDialog.projectOptions = []
  clearWordImportState()
  resetWordImportPreflightState()
  wordImportDialog.visible = true
  await loadWordImportProjectOptions()
}

const cancelWordImportDialog = () => {
  wordImportDialog.visible = false
  clearWordImportState()
}

const confirmWordImportDialog = async () => {
  const selectedProject = getSelectedWordImportProject()
  const selectedSubjectName = resolveWordImportSubjectName()
  if (!wordImportDialog.selectedFormSlotType) {
    message.warning('请选择表单类型。')
    return
  }
  if (!selectedSubjectName) {
    message.warning(resolveWordImportSubjectRequiredMessage())
    return
  }
  const file = wordImportDialog.file
  if (!file) {
    message.warning('请选择 Word 文件。')
    return
  }
  const batchRecordName = selectedSubjectName
  const productNames = [selectedSubjectName]
  if (batchRecordName.length > 100) {
    message.warning(resolveWordImportSubjectTooLongMessage())
    return
  }
  if (!isMainWordImport.value) {
    await runUploadedExtraFormSlotImport(file, selectedSubjectName)
    return
  }
  const selectedDccProjectCodeId = selectedProject?.id
  if (!selectedDccProjectCodeId) {
    message.warning('请选择产品名称。')
    return
  }
  if (!wordImportDialog.preflight) {
    await loadWordImportPreflight()
    if (!wordImportDialog.preflight) {
      return
    }
  }
  if (!isWordImportActionAllowed(wordImportDialog.selectedAction)) {
    message.warning(resolveWordImportActionLockedMessage(wordImportDialog.preflight))
    return
  }
  if (isWordImportRouteDuplicateBlocked(wordImportDialog.preflight)) {
    message.warning(`所选 DCC 项目代码存在多条正式路线绑定：${formatWordImportDuplicateRoutes(wordImportDialog.preflight)}，请先清理为唯一绑定。`)
    return
  }
  if (wordImportDialog.selectedAction === 'REBUILD_V1'
    && wordImportDialog.rebuildBatchRecord
    && wordImportDialog.preflight.currentBatchRecordHasMainReports) {
    message.warning(`批记录「${batchRecordName}」主批记录已上传，请先删除后重新上传。`)
    return
  }
  const selection = buildWordImportSelection()
  if (!selection.rebuildBatchRecord && selection.selectedOptions.length === 0) {
    message.info('未选择重建内容，已退出导入。')
    wordImportDialog.visible = false
    clearWordImportState()
    return false
  }
  const dialogClosed = await closeWordImportDialogBeforeMessageBox()
  const confirmedSelection = await confirmWordImportUpgradeSelections(batchRecordName, selection)
  if (!confirmedSelection) {
    if (dialogClosed) {
      wordImportDialog.visible = true
    }
    return
  }
  if (!confirmedSelection.rebuildBatchRecord && confirmedSelection.selectedRouteProductIds.length === 0
    && confirmedSelection.selectedProductNames.length === 0) {
    message.info('未选择重建内容，已退出导入。')
    wordImportDialog.visible = false
    clearWordImportState()
    return false
  }
  if (confirmedSelection) {
    wordImportDialog.confirming = true
    try {
      await runUploadedWordImport(file, batchRecordName, selectedDccProjectCodeId, productNames, confirmedSelection)
      wordImportDialog.visible = false
    } finally {
      wordImportDialog.confirming = false
    }
  }
}

const handleWordImportFileSelect = () => {
  if (!canSelectWordImportFile.value) {
    message.warning(resolveWordImportSubjectRequiredMessage())
    return
  }
  if (!wordImportFileInputRef.value) {
    message.error('Word 文件选择器未初始化，请刷新页面后重试。')
    return
  }
  wordImportFileInputRef.value.value = ''
  wordImportFileInputRef.value.click()
}

const isSupportedWordImportFile = (file: File) => {
  const lowerFileName = file.name.toLowerCase()
  return lowerFileName.endsWith('.doc') || lowerFileName.endsWith('.docx')
}

const handleImportFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    clearWordImportState()
    return
  }
  if (!isSupportedWordImportFile(file)) {
    clearWordImportState()
    message.error('仅支持选择 .doc 或 .docx Word 文件。')
    return
  }

  wordImportDialog.file = file
  await loadWordImportPreflight()
}

const runUploadedWordImport = async (
  file: File,
  batchRecordName: string,
  dccProjectCodeId: number,
  productNames: string[],
  selection: {
    importAction: BatchRecordWordImportAction
    expectedSourceVersionId?: number
    expectedTargetVersionNo?: string
    rebuildBatchRecord: boolean
    routeUpgradeConfirmed?: boolean
    expectedRouteId?: number
    expectedRouteVersionId?: number
    expectedRouteCandidateVersionId?: number
    selectedRouteProductIds: number[]
    selectedProductNames: string[]
  }
) => {
  const wordImportRouteKey = resolveWordImportRouteKey(file.name)
  const upgrade = selection.importAction === 'UPGRADE' 
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '正在导入 Word，请稍候...',
    background: 'rgba(0, 0, 0, 0.35)'
  })
  try {
    wordImporting.value = true
    const result = await BatchRecordReportApi.recognizeUploadedRoute(
      file,
      wordImportRouteKey,
      batchRecordName,
      dccProjectCodeId,
      upgrade,
      productNames,
      selection.rebuildBatchRecord,
      selection.selectedRouteProductIds,
      selection.selectedProductNames,
      selection.importAction,
      selection.expectedSourceVersionId,
      selection.expectedTargetVersionNo,
      Boolean(selection.routeUpgradeConfirmed),
      selection.expectedRouteId,
      selection.expectedRouteVersionId,
      selection.expectedRouteCandidateVersionId
    )
    lastWordImportResult.value = result
    const productSummary =
      result.routeCode && result.boundProductCodeCount !== undefined
        ? `，产品绑定 ${result.boundProductCodeCount || 0} 个`
        : ''
    const skippedSummary = result.skippedProductNames?.length
      ? `，跳过未匹配产品：${result.skippedProductNames.join('、')}`
      : ''
    const routeSummary = result.routeCode
      ? `，已生成工艺路线 ${result.routeCode}，路线工序 ${result.routeProcessCount || 0} 道，批记录绑定 ${result.batchRecordRouteBindingCount || 0} 个${productSummary}${skippedSummary}`
      : ''
    const versionSummary = submitImportedVersionApproval(result)
    message.success(
      `批记录名称「${batchRecordName}」路线 ${wordImportRouteKey} 解析完成：共 ${result.importedCount} 份，新建 ${result.createdCount} 份，更新 ${result.updatedCount} 份${routeSummary}${versionSummary}。`
    )
    clearWordImportState()
    queryParams.pageNo = 1
    selectedReportId.value = ''
    clearTemplatePreview()
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, `电子批记录 Word 文件导入失败，请联系管理员检查 ${wordImportRouteKey} Word COM 解析链路。`))
  } finally {
    loadingInstance.close()
    wordImporting.value = false
  }
}

const findExistingExtraFormSlotReport = async (
  selectedSubjectName: string,
  formSlotType: BatchRecordFormSlotType
) => {
  const localReport = list.value.find(
    (item) =>
      item.batchRecordName === selectedSubjectName &&
      (item.formSlotType || 'MAIN') === formSlotType
  )
  if (localReport) {
    return localReport
  }
  const data = await BatchRecordReportApi.getGeneratedReportPage({
    pageNo: 1,
    pageSize: 200,
    productName: selectedSubjectName,
    formSlotType
  })
  return (Array.isArray(data.list) ? data.list : []).find(
    (item) =>
      item.batchRecordName === selectedSubjectName &&
      (item.formSlotType || 'MAIN') === formSlotType
  )
}

const confirmExtraFormSlotVersionUpgrade = async (
  selectedSubjectName: string,
  formSlotType: BatchRecordFormSlotType,
  existingReport: BatchRecordReportVO
) => {
  const formSlotLabel = resolveFormSlotTypeLabel(formSlotType)
  const subjectLabel = resolveExtraFormSlotSubjectLabel(formSlotType)
  const currentVersionNo = existingReport.versionNo || '无版本'
  const dialogClosed = await closeWordImportDialogBeforeMessageBox()
  try {
    await ElMessageBox.confirm(
      `${subjectLabel}「${selectedSubjectName}」${formSlotLabel}当前版本为 ${currentVersionNo}，确认后将生成新版本，旧版本保留。`,
      `确认${formSlotLabel}升版`,
      {
        confirmButtonText: '升版导入',
        cancelButtonText: '退出导入',
        distinguishCancelAndClose: true,
        type: 'warning'
      }
    )
    return true
  } catch {
    if (dialogClosed) {
      wordImportDialog.visible = true
    }
    return false
  }
}

const runUploadedExtraFormSlotImport = async (file: File, selectedSubjectName: string) => {
  const existingReport = await findExistingExtraFormSlotReport(
    selectedSubjectName,
    wordImportDialog.selectedFormSlotType
  )
  if (existingReport) {
    const confirmed = await confirmExtraFormSlotVersionUpgrade(
      selectedSubjectName,
      wordImportDialog.selectedFormSlotType,
      existingReport
    )
    if (!confirmed) {
      return
    }
  }
  const formSlotLabel = resolveFormSlotTypeLabel(wordImportDialog.selectedFormSlotType)
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '正在导入 Word，请稍候...',
    background: 'rgba(0, 0, 0, 0.35)'
  })
  wordImportDialog.confirming = true
  wordImporting.value = true
  try {
    const result = await BatchRecordReportApi.uploadExtraFormSlot(
      file,
      selectedSubjectName,
      wordImportDialog.selectedFormSlotType
    )
    const versionSummary = submitImportedVersionApproval(result, resolveFormSlotTypeLabel(wordImportDialog.selectedFormSlotType))
    const subjectLabel = resolveExtraFormSlotSubjectLabel(wordImportDialog.selectedFormSlotType)
    message.success(
      `${subjectLabel}「${selectedSubjectName}」${formSlotLabel}解析完成：共 ${result.importedCount} 份，新建 ${result.createdCount} 份，更新 ${result.updatedCount} 份${versionSummary}。`
    )
    wordImportDialog.visible = false
    clearWordImportState()
    queryParams.pageNo = 1
    selectedReportId.value = ''
    clearTemplatePreview()
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, `${resolveFormSlotTypeLabel(wordImportDialog.selectedFormSlotType)}上传解析失败。`))
  } finally {
    loadingInstance.close()
    wordImporting.value = false
    wordImportDialog.confirming = false
  }
}

const openDesigner = async (reportId: string, reportMode: 'preview' | 'edit' = 'preview') => {
  await router.push({
    path: route.path,
    query: {
      mode: 'designer',
      reportId,
      reportMode
    }
  })
}

const openSimulate = async (row: BatchRecordReportVO) => {
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-execution/template-simulate',
    query: {
      reportId: row.reportId,
      reportName: row.reportName,
      batchRecordName: row.batchRecordName,
      returnTo: route.fullPath,
      returnLabel: '返回'
    }
  })
}

const openTemplateAction = async (row: BatchRecordReportVO, action: 'signature' | 'cellRules') => {
  if (action === 'cellRules') {
    openCellRulesDialog(row)
    return
  }
  await router.push({
    path: '/mes/pro/batch-record-form-list',
    query: {
      reportId: row.reportId,
      action
    }
  })
}

const handleCellLinks = async (row: BatchRecordReportVO) => {
  const cellLinkRouteId = normalizeRouteQueryText(route.query.routeId)
  const cellLinkRouteProcessId = normalizeRouteQueryText(route.query.routeProcessId)
  await router.push({
    path: '/mes/pro/batch-record-cell-link',
    query: {
      definitionId: row.batchRecordDefinitionId ? String(row.batchRecordDefinitionId) : undefined,
      versionId: row.batchRecordVersionId ? String(row.batchRecordVersionId) : undefined,
      routeId: cellLinkRouteId || undefined,
      routeProcessId: cellLinkRouteProcessId || undefined,
      sourceReportId: isMainBatchRecordReport(row) ? PROCESS_POOL_REPORT_SOURCE_REPORT_ID : undefined,
      dccProjectCodeId: isMainBatchRecordReport(row) && row.dccProjectCodeId ? String(row.dccProjectCodeId) : undefined,
      dccProjectCode: isMainBatchRecordReport(row) && row.dccProjectCodeId ? row.projectCode : undefined,
      targetReportId: row.reportId
    }
  })
}

const handleRename = async (row: RecordFormListRow) => {
  let nextName = ''
  try {
    const result = await ElMessageBox.prompt('请输入新的报表名称', '重命名报表', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: row.reportName,
      inputValidator: (value) => {
        const trimmed = String(value || '').trim()
        if (!trimmed) return '报表名称不能为空'
        if (trimmed.length > 50) return '报表名称长度不能超过 50 个字符'
        return true
      }
    })
    nextName = String(result.value || '').trim()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
  if (!nextName || nextName === row.reportName) {
    return
  }
  try {
    await BatchRecordReportApi.renameGeneratedReport({
      reportId: row.reportId,
      reportName: nextName
    })
    message.success('重命名成功')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '电子批记录报表重命名失败，请联系管理员。'))
  }
}

const handleDelete = async (row: RecordFormListRow) => {
  try {
    await message.delConfirm('确认删除当前电子批记录报表吗？')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
  try {
    await BatchRecordReportApi.deleteGeneratedReport(row.reportId)
    message.success('删除成功')
    if (selectedReportId.value === row.reportId) {
      selectedReportId.value = ''
      clearTemplatePreview()
    }
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '电子批记录报表删除失败，请联系管理员。'))
  }
}

onMounted(() => {
  if (isBatchRecordFormListPath() && !isDesignerMode.value) {
    getList()
  }
})

onBeforeUnmount(() => {
  recordFormListRequestSerial += 1
  cancelDeferredRecordFormSecondaryLoad()
  templatePreviewRequestSerial += 1
})

watch(
  () => wordImportDialog.selectedDccProjectCodeId,
  () => {
    if (!wordImportDialog.visible || !isMainWordImport.value) {
      return
    }
    clearWordImportState()
    resetWordImportPreflightState()
  }
)

watch(
  () => [route.query.reportId, route.query.action, route.query.mode] as const,
  async ([reportId]) => {
    if (!isBatchRecordFormListPath()) {
      return
    }
    if (isDesignerMode.value) {
      recordFormListRequestSerial += 1
      cancelDeferredRecordFormSecondaryLoad()
      clearTemplatePreview()
      return
    }
    const targetRouteStateKey = buildBatchRecordFormListRouteStateKey()
    if (shouldKeepBatchRecordFormListLoadedState(targetRouteStateKey)) {
      return
    }
    if (typeof reportId === 'string' && reportId.trim()) {
      selectedReportId.value = reportId.trim()
    }
    await getList()
  }
)
</script>

<style scoped>
.batch-record-form-page {
  border: none;
  background: transparent;
}

.batch-record-form-page :deep(.el-card__body) {
  padding: 0 !important;
}

.batch-record-form-word-import-input {
  display: none;
}

.batch-record-word-import-form__project-select {
  width: 100%;
}

.batch-record-word-import-form__name-input {
  width: 100%;
}

.batch-record-word-import-form__project-option {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.batch-record-word-import-form__project-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-word-import-form__project-code {
  flex: 0 0 auto;
  color: #6b7280;
  font-size: 12px;
}

.batch-record-word-import-form__file-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.batch-record-word-import-form__file-state {
  display: inline-flex;
  min-width: 0;
  max-width: 100%;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fafc;
  color: #263247;
  font-size: 13px;
}

.batch-record-word-import-form__file-state.is-error {
  border-color: #f2c6c6;
  background: #fff7f7;
}

.batch-record-word-import-form__file-icon {
  flex: 0 0 auto;
  color: #1677ff;
}

.batch-record-word-import-form__file-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-word-import-form__file-error {
  margin-top: 8px;
}

.batch-record-word-import-form__preflight {
  display: flex;
  width: 100%;
  min-height: 56px;
  flex-direction: column;
  gap: 10px;
}

.batch-record-word-import-form__version-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.batch-record-word-import-form__version-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fafc;
}

.batch-record-word-import-form__version-item span,
.batch-record-word-import-form__route-option small,
.batch-record-word-import-form__empty {
  color: #64748b;
  font-size: 12px;
}

.batch-record-word-import-form__version-item strong {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-word-import-form__reference-alert {
  margin-bottom: 2px;
}

.batch-record-word-import-form__action-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.batch-record-word-import-form__action-label {
  flex: 0 0 auto;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.batch-record-word-import-form__route-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.batch-record-word-import-form__route-title {
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.batch-record-word-import-form__route-option {
  display: flex;
  min-height: 26px;
  align-items: center;
}

.batch-record-word-import-form__route-option :deep(.el-checkbox__label) {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.batch-record-form-toolbar__import-button {
  white-space: nowrap;
}

.batch-record-form-toolbar__latest-version-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: var(--el-fill-color-blank);
  white-space: nowrap;
}

.batch-record-form-toolbar__latest-version-label {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.batch-record-form-layout {
  display: grid;
  grid-template-columns: minmax(640px, 58%) minmax(420px, 42%);
  gap: 14px;
  min-height: 640px;
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.batch-record-form-layout__list,
.batch-record-form-preview {
  min-width: 0;
}

.batch-record-form-layout__alert {
  margin-bottom: 12px;
}

.batch-record-form-preview {
  display: flex;
  flex-direction: column;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.batch-record-form-preview__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px 14px;
  min-height: 46px;
  padding: 10px 12px;
  border-bottom: 1px solid #edf1f6;
  background: #f7f9fc;
  color: #172033;
  font-size: 14px;
  font-weight: 700;
}

.batch-record-form-preview__heading {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.batch-record-form-preview__title {
  min-width: 0;
  overflow: hidden;
  color: #4b5563;
  font-size: 12px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-form-preview__actions {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 2px 8px;
  flex-wrap: wrap;
}

.batch-record-form-preview__actions :deep(.el-button) {
  margin-left: 0;
}

.batch-record-form-preview__body {
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.batch-record-form-preview__frame {
  min-width: 0;
}

.batch-record-form-focused-preview {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  background: #f7f9fc;
  color: #172033;
}

.batch-record-form-focused-preview__control {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 16px;
  padding: 18px 14px;
  border-right: 1px solid #dbe3ef;
  background: #ffffff;
}

.batch-record-form-focused-preview__title-block {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf1f6;
}

.batch-record-form-focused-preview__label {
  color: #64748b;
  font-size: 12px;
}

.batch-record-form-focused-preview__form-name {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 15px;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-form-focused-preview__buttons {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
}

.batch-record-form-focused-preview__buttons :deep(.el-button) {
  width: 100%;
  justify-content: flex-start;
  margin-left: 0;
  border-radius: 6px;
}

.batch-record-form-focused-preview__stage {
  min-width: 0;
  min-height: 0;
  padding: 14px;
  overflow: hidden;
}

.batch-record-form-focused-preview__body {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.batch-record-form-focused-preview__body.is-height-fit {
  overflow: hidden;
}

.batch-record-form-focused-preview__frame {
  min-width: 0;
}

.batch-record-form-focused-preview__frame.is-height-fit {
  height: 100%;
  min-height: 0;
}

@media (max-width: 1280px) {
  .batch-record-form-layout {
    grid-template-columns: 1fr;
  }
}
</style>
