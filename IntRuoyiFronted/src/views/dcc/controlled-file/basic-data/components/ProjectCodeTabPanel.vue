<template>
  <ContentWrap class="scheme-d-basic-data-page scheme-d-basic-data-page--dcc-project-code">
    <div class="mb-16px flex items-center gap-8px">
      <span class="text-18px font-600 text-[var(--el-text-color-primary)]">基础数据 / DCC项目代码</span>
    </div>
    <UnifiedListTemplate
      class="dcc-project-code-list-template"
      table-key="dcc.projectCode.main"
      :query-model="queryParams"
      label-width="76px"
      :filter-definitions="projectCodeQuickFilterDefinitions"
      :quick-filter-state="projectCodeQuickFilter.state"
      :selected-filter-definition="projectCodeQuickFilter.selectedDefinition.value"
      :operator-options="projectCodeQuickFilter.operatorOptions.value"
      :columns="projectCodeColumns"
      :column-saving="projectCodeColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="projectCodeQuickFilter.updateState"
      @quick-filter-query="projectCodeQuickFilter.applyQuickFilter"
      @column-change="saveProjectCodeColumnConfig"
      @sort-change="handleSortChange"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          plain
          :disabled="batchAiCategoryRunning || listUnclassifiedAutoClassifyRunning"
          @click="openForm('create')"
          v-hasPermi="['dcc:project-code:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增项目代码
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--primary"
          type="primary"
          plain
          data-testid="dcc-product-onboarding-open"
          :disabled="batchAiCategoryRunning || listUnclassifiedAutoClassifyRunning"
          @click="openProductOnboardingDialog"
          v-hasPermi="['dcc:project-code:create']"
        >
          <Icon icon="ep:connection" class="mr-5px" />
          产品建档申请
        </el-button>
        <el-button
          v-if="canRunProjectCodeListNameAutoClassify"
          class="scheme-d-btn scheme-d-btn--primary"
          type="primary"
          plain
          data-testid="dcc-project-code-list-auto-classify-unclassified"
          :loading="listUnclassifiedAutoClassifyRunning"
          :disabled="
            loading ||
            exportLoading ||
            previewLoading ||
            confirmLoading ||
            aiCategoryRunning ||
            batchAiCategoryRunning ||
            listUnclassifiedAutoClassifyRunning ||
            unclassifiedAutoClassifyRunning
          "
          @click="handleListAutoClassifyUnclassifiedProjectCodes"
        >
          <Icon icon="ep:magic-stick" class="mr-5px" />
          按文件名归类未分类
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--primary"
          :disabled="batchAiCategoryRunning || listUnclassifiedAutoClassifyRunning"
          @click="openImportDialog"
          v-hasPermi="['dcc:project-code:import']"
        >
          <Icon icon="ep:upload" class="mr-5px" />
          导入
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--warning"
          :loading="exportLoading"
          :disabled="batchAiCategoryRunning || listUnclassifiedAutoClassifyRunning"
          @click="handleExport"
          v-hasPermi="['dcc:project-code:export']"
        >
          <Icon icon="ep:download" class="mr-5px" />
          导出
        </el-button>
        <el-button
          v-if="canRunBatchAiCategory"
          class="scheme-d-btn scheme-d-btn--primary"
          type="primary"
          plain
          data-testid="dcc-project-code-batch-ai-category"
          :loading="batchAiCategoryRunning"
          :disabled="
            loading ||
            exportLoading ||
            previewLoading ||
            confirmLoading ||
            aiCategoryRunning ||
            listUnclassifiedAutoClassifyRunning ||
            unclassifiedAutoClassifyRunning
          "
          @click="handleBatchAiCategoryProjectCodes"
        >
          <Icon icon="ep:magic-stick" class="mr-5px" />
          批量AI分类
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <div
          v-if="listUnclassifiedAutoClassifyProgressVisible"
          class="dcc-project-code-batch-ai-category-progress"
          data-testid="dcc-project-code-list-auto-classify-progress"
        >
          <div class="dcc-project-code-batch-ai-category-progress-head">
            <span>按文件名归类未分类进度</span>
            <div class="dcc-project-code-batch-ai-category-progress-head-actions">
              <span>
                已处理项目 {{ listUnclassifiedAutoClassifyProcessedProjects }}/{{
                  listUnclassifiedAutoClassifyTotalProjects
                }}
                ，已归类文件 {{ listUnclassifiedAutoClassifyProcessedFiles }} 份
              </span>
            </div>
          </div>
          <el-progress :percentage="listUnclassifiedAutoClassifyProgressPercent" :stroke-width="6" />
        </div>
        <div
          v-if="batchAiCategoryProgressVisible"
          class="dcc-project-code-batch-ai-category-progress"
          data-testid="dcc-project-code-batch-ai-category-progress"
        >
          <div class="dcc-project-code-batch-ai-category-progress-head">
            <span>批量AI分类进度</span>
            <div class="dcc-project-code-batch-ai-category-progress-head-actions">
              <span>
                已处理 {{ batchAiCategoryProcessed }}/{{ batchAiCategoryTotal }}
                ，状态：{{ batchAiCategoryStatusText }}
                ，失败文件 {{ batchAiCategoryFailedFileCount }}
              </span>
              <el-button
                link
                class="dcc-project-code-batch-ai-category-progress-close scheme-d-btn scheme-d-btn--danger scheme-d-row-action scheme-d-row-action--danger scheme-d-icon-button"
                data-testid="dcc-project-code-batch-ai-category-progress-close"
                aria-label="关闭批量AI分类进度"
                @click="handleCloseBatchAiCategoryProgress"
              >
                <Icon icon="ep:close" />
              </el-button>
            </div>
          </div>
          <el-progress :percentage="batchAiCategoryProgressPercent" :stroke-width="6" />
          <div class="dcc-project-code-batch-ai-category-progress-summary">
            已归类文件 {{ batchAiCategoryMatchedFileCount }} 个，保留未分类
            {{ batchAiCategoryUnclassifiedFileCount }} 个，歧义文件
            {{ batchAiCategoryAmbiguousFileCount }} 个，并发跳过
            {{ batchAiCategoryConflictFileCount }} 个，已有记录
            {{ batchAiCategorySkippedFileCount }} 个
          </div>
          <div
            v-if="batchAiCategoryFailedFileCount > 0"
            class="dcc-project-code-batch-ai-category-progress-actions"
          >
            <el-button
              link
              class="scheme-d-row-action scheme-d-row-action--danger"
              type="danger"
              data-testid="dcc-project-code-batch-ai-category-view-failures"
              @click="handleViewBatchAiCategoryFailures"
            >
              查看失败文件
            </el-button>
            <el-button
              link
              class="scheme-d-row-action scheme-d-row-action--warning"
              type="primary"
              data-testid="dcc-project-code-batch-ai-category-export-failures"
              :loading="batchAiCategoryFailureExporting"
              @click="handleExportBatchAiCategoryFailures"
            >
              导出失败明细
            </el-button>
          </div>
          <div
            v-if="batchAiCategoryFailureSummaries.length > 0"
            class="dcc-project-code-batch-ai-category-failure-summary"
            data-testid="dcc-project-code-batch-ai-category-failure-summary"
          >
            <span class="dcc-project-code-batch-ai-category-failure-summary-label">主要失败原因：</span>
            <span
              v-for="summary in batchAiCategoryFailureSummaries"
              :key="`${summary.stage}-${summary.code}-${summary.reason}`"
              class="dcc-project-code-batch-ai-category-failure-summary-item"
            >
              失败阶段：{{ formatBatchAiCategoryFailureStage(summary.stage) }}
              / {{ summary.code }}，{{ summary.reason }}（{{ summary.count }} 个）
            </span>
          </div>
          <div
            v-if="batchAiCategoryConsistencyMessage"
            class="dcc-project-code-batch-ai-category-progress-consistency"
            data-testid="dcc-project-code-batch-ai-category-consistency"
          >
            统计异常：{{ batchAiCategoryConsistencyMessage }}
          </div>
          <div
            v-if="batchAiCategoryInterruptionMessage"
            class="dcc-project-code-batch-ai-category-progress-interruption"
          >
            最近失败：{{ batchAiCategoryInterruptionMessage }}
          </div>
        </div>
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="dcc.projectCode.main"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="id"
          @header-dragend="handleProjectCodeHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isProjectCodeColumnVisible('docControlNo')"
            label="文控"
            prop="docControlNo"
            :width="getProjectCodeColumnWidthString('docControlNo')"
            :min-width="getProjectCodeColumnMinWidthString('docControlNo', 130)"
            v-bind="sortColumnAttrs('docControlNo')"
          >
            <template #default="{ row }">{{ row.docControlNo || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('primaryCode')"
            label="主编码"
            prop="primaryCode"
            :width="getProjectCodeColumnWidthString('primaryCode')"
            :min-width="getProjectCodeColumnMinWidthString('primaryCode', 100)"
            v-bind="sortColumnAttrs('primaryCode')"
          >
            <template #default>无</template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('projectName')"
            label="项目名称"
            prop="projectName"
            :width="getProjectCodeColumnWidthString('projectName')"
            :min-width="getProjectCodeColumnMinWidthString('projectName', 220)"
            v-bind="sortColumnAttrs('projectName')"
          />
          <el-table-column
            v-if="isProjectCodeColumnVisible('projectCode')"
            label="项目代码"
            prop="projectCode"
            :width="getProjectCodeColumnWidthString('projectCode')"
            :min-width="getProjectCodeColumnMinWidthString('projectCode', 120)"
            v-bind="sortColumnAttrs('projectCode')"
          >
            <template #default="{ row }">{{ row.projectCode || '' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('category')"
            label="类别"
            prop="category"
            :width="getProjectCodeColumnWidthString('category')"
            :min-width="getProjectCodeColumnMinWidthString('category', 120)"
            v-bind="sortColumnAttrs('category')"
          >
            <template #default="{ row }">{{ row.category || '-' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('associatedFileCount')"
            label="关联文件数"
            prop="associatedFileCount"
            :width="getProjectCodeColumnWidthString('associatedFileCount', 120)"
            v-bind="sortColumnAttrs('associatedFileCount')"
            align="right"
          >
            <template #default="{ row }">{{ row.associatedFileCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('routeStatus')"
            label="工艺路线"
            prop="routeStatus"
            :width="getProjectCodeColumnWidthString('routeStatus', 120)"
            v-bind="sortColumnAttrs('routeStatus')"
          >
            <template #default="{ row }">
              <div class="dcc-project-code-governance-cell">
                <el-tag
                  class="scheme-d-tag"
                  effect="plain"
                  :type="resolveDccProjectGovernanceTagType(getDccProjectGovernance(row.projectName)?.routeStatus)"
                  :title="getDccProjectGovernance(row.projectName)?.routeCodes?.join('、') || ''"
                >
                  {{ formatDccProjectGovernanceStatus(getDccProjectGovernance(row.projectName)?.routeStatus) }}
                </el-tag>
                <span
                  v-if="formatDccProjectGovernanceVersions(getDccProjectGovernance(row.projectName)?.routeVersionNos)"
                  class="dcc-project-code-governance-version"
                  data-testid="dcc-project-code-governance-version-route"
                >
                  版本 {{ formatDccProjectGovernanceVersions(getDccProjectGovernance(row.projectName)?.routeVersionNos) }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('mainBatchRecordStatus')"
            label="主批记录"
            prop="mainBatchRecordStatus"
            :width="getProjectCodeColumnWidthString('mainBatchRecordStatus', 120)"
            v-bind="sortColumnAttrs('mainBatchRecordStatus')"
          >
            <template #default="{ row }">
              <div class="dcc-project-code-governance-cell">
                <el-tag
                  class="scheme-d-tag"
                  effect="plain"
                  :type="resolveDccProjectGovernanceTagType(getDccProjectGovernance(row.projectName)?.mainBatchRecordStatus)"
                  :title="getDccProjectGovernance(row.projectName)?.mainBatchRecordVersionNos?.join('、') || ''"
                >
                  {{ formatDccProjectGovernanceStatus(getDccProjectGovernance(row.projectName)?.mainBatchRecordStatus) }}
                </el-tag>
                <span
                  v-if="formatDccProjectGovernanceVersions(getDccProjectGovernance(row.projectName)?.mainBatchRecordVersionNos)"
                  class="dcc-project-code-governance-version"
                  data-testid="dcc-project-code-governance-version-main-batch-record"
                >
                  版本 {{ formatDccProjectGovernanceVersions(getDccProjectGovernance(row.projectName)?.mainBatchRecordVersionNos) }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('lossReportStatus')"
            label="损耗单"
            prop="lossReportStatus"
            :width="getProjectCodeColumnWidthString('lossReportStatus', 110)"
            v-bind="sortColumnAttrs('lossReportStatus')"
          >
            <template #default="{ row }">
              <div class="dcc-project-code-governance-cell">
                <el-tag
                  class="scheme-d-tag"
                  effect="plain"
                  :type="resolveDccProjectGovernanceTagType(getDccProjectGovernance(row.projectName)?.lossReportStatus)"
                  :title="getDccProjectGovernance(row.projectName)?.lossReportCodes?.join('、') || ''"
                >
                  {{ formatDccProjectGovernanceStatus(getDccProjectGovernance(row.projectName)?.lossReportStatus) }}
                </el-tag>
                <span
                  v-if="formatDccProjectGovernanceVersions(getDccProjectGovernance(row.projectName)?.lossReportVersionNos)"
                  class="dcc-project-code-governance-version"
                  data-testid="dcc-project-code-governance-version-loss-report"
                >
                  版本 {{ formatDccProjectGovernanceVersions(getDccProjectGovernance(row.projectName)?.lossReportVersionNos) }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('processInspectionStatus')"
            label="过程检验单"
            prop="processInspectionStatus"
            :width="getProjectCodeColumnWidthString('processInspectionStatus', 130)"
            v-bind="sortColumnAttrs('processInspectionStatus')"
          >
            <template #default="{ row }">
              <div class="dcc-project-code-governance-cell">
                <el-tag
                  class="scheme-d-tag"
                  effect="plain"
                  :type="resolveDccProjectGovernanceTagType(getDccProjectGovernance(row.projectName)?.processInspectionStatus)"
                  :title="getDccProjectGovernance(row.projectName)?.processInspectionCodes?.join('、') || ''"
                >
                  {{ formatDccProjectGovernanceStatus(getDccProjectGovernance(row.projectName)?.processInspectionStatus) }}
                </el-tag>
                <span
                  v-if="formatDccProjectGovernanceVersions(getDccProjectGovernance(row.projectName)?.processInspectionVersionNos)"
                  class="dcc-project-code-governance-version"
                  data-testid="dcc-project-code-governance-version-process-inspection"
                >
                  版本 {{ formatDccProjectGovernanceVersions(getDccProjectGovernance(row.projectName)?.processInspectionVersionNos) }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('parameterRecordStatus')"
            label="参数记录表"
            prop="parameterRecordStatus"
            :width="getProjectCodeColumnWidthString('parameterRecordStatus', 130)"
            v-bind="sortColumnAttrs('parameterRecordStatus')"
          >
            <template #default="{ row }">
              <div class="dcc-project-code-governance-cell">
                <el-tag
                  class="scheme-d-tag"
                  effect="plain"
                  :type="resolveDccProjectGovernanceTagType(getDccProjectGovernance(row.projectName)?.parameterRecordStatus)"
                  :title="getDccProjectGovernance(row.projectName)?.parameterRecordCodes?.join('、') || ''"
                >
                  {{ formatDccProjectGovernanceStatus(getDccProjectGovernance(row.projectName)?.parameterRecordStatus) }}
                </el-tag>
                <span
                  v-if="formatDccProjectGovernanceVersions(getDccProjectGovernance(row.projectName)?.parameterRecordVersionNos)"
                  class="dcc-project-code-governance-version"
                  data-testid="dcc-project-code-governance-version-parameter-record"
                >
                  版本 {{ formatDccProjectGovernanceVersions(getDccProjectGovernance(row.projectName)?.parameterRecordVersionNos) }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('qaRegulationStatus')"
            label="QA规程"
            prop="qaRegulationStatus"
            :width="getProjectCodeColumnWidthString('qaRegulationStatus', 130)"
            v-bind="sortColumnAttrs('qaRegulationStatus')"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                data-testid="dcc-project-code-qa-regulation-link"
                @click="openQaRegulation(row)"
              >
                {{ formatQaRegulationStatus(row.id) }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProjectCodeColumnVisible('updateTime')"
            label="更新时间"
            prop="updateTime"
            :width="getProjectCodeColumnWidthString('updateTime', 180)"
            :formatter="dateFormatter2"
            v-bind="sortColumnAttrs('updateTime')"
          />
          <el-table-column
            v-if="isProjectCodeColumnVisible('actions')"
            label="关联文档"
            prop="actions"
            fixed="right"
            :width="getProjectCodeColumnWidthString('actions', 240)"
          >
            <template #default="{ row }">
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--primary"
                type="primary"
                @click="openForm('update', row)"
                v-hasPermi="['dcc:project-code:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--danger"
                type="danger"
                @click="handleDelete(row)"
                v-hasPermi="['dcc:project-code:delete']"
              >
                删除
              </el-button>
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--primary"
                type="primary"
                @click="openProjectCodeDetail(row)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <el-dialog
    v-model="importVisible"
    class="scheme-d-form-control"
    title="DCC基础数据导入"
    width="1080px"
    data-testid="dcc-project-code-import-dialog"
  >
    <div class="dcc-project-code-import-toolbar">
      <el-upload
        v-model:file-list="importFileList"
        accept=".xlsx,.xls"
        :auto-upload="false"
        :limit="1"
        :on-change="handleImportFileChange"
        :on-remove="handleImportFileRemove"
      >
        <el-button class="scheme-d-btn scheme-d-btn--neutral">
          <Icon icon="ep:folder-opened" class="mr-5px" />
          选择文件
        </el-button>
      </el-upload>
      <el-button class="scheme-d-btn scheme-d-btn--warning" @click="handleDownloadTemplate">
        <Icon icon="ep:document" class="mr-5px" />
        模板
      </el-button>
      <el-button
        class="scheme-d-btn scheme-d-btn--primary"
        type="primary"
        :loading="previewLoading"
        @click="handleImportPreview"
      >
        <Icon icon="ep:view" class="mr-5px" />
        预览
      </el-button>
      <el-button
        class="scheme-d-btn scheme-d-btn--success"
        type="success"
        :disabled="!previewResult || previewResult.failureCount > 0"
        :loading="confirmLoading"
        @click="handleImportConfirm"
      >
        <Icon icon="ep:circle-check" class="mr-5px" />
        确认导入
      </el-button>
    </div>

    <div
      v-if="previewResult"
      class="dcc-project-code-import-summary"
      data-testid="dcc-project-code-import-summary"
    >
      <el-tag class="scheme-d-tag">总数 {{ previewResult.totalCount }}</el-tag>
      <el-tag class="scheme-d-tag" type="success">新增 {{ previewResult.createCount }}</el-tag>
      <el-tag class="scheme-d-tag" type="warning">更新 {{ previewResult.updateCount }}</el-tag>
      <el-tag class="scheme-d-tag" type="info">停用 {{ previewResult.disableCount }}</el-tag>
      <el-tag class="scheme-d-tag">不变 {{ previewResult.unchangedCount }}</el-tag>
      <el-tag class="scheme-d-tag" :type="previewResult.failureCount > 0 ? 'danger' : 'success'">
        失败 {{ previewResult.failureCount }}
      </el-tag>
    </div>

    <el-table
      v-if="previewResult"
      :data="importRows"
      :show-overflow-tooltip="true"
      height="460"
    >
      <el-table-column label="行号" prop="rowNo" width="80" />
      <el-table-column label="动作" prop="importAction" width="110">
        <template #default="{ row }">
          <el-tag class="scheme-d-tag" :type="importActionTagType(row.importAction)">
            {{ formatImportAction(row.importAction) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="项目名称" prop="projectName" min-width="180" />
      <el-table-column label="项目代码" prop="projectCode" min-width="120" />
      <el-table-column label="类别" prop="category" min-width="120" />
      <el-table-column label="存放位置" prop="storageLocation" min-width="120" />
      <el-table-column label="优先级" prop="priority" min-width="100" />
      <el-table-column label="失败原因" prop="failureReason" min-width="240">
        <template #default="{ row }">{{ row.failureReason || '-' }}</template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <Dialog v-model="formVisible" class="scheme-d-form-control" title="项目代码维护" width="760px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="110px"
    >
      <el-form-item label="文控" prop="docControlNo">
        <el-input v-model="formData.docControlNo" placeholder="请输入文控" />
      </el-form-item>
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="formData.projectName" placeholder="请输入项目名称" />
      </el-form-item>
      <el-form-item label="项目代码" prop="projectCode">
        <el-input v-model="formData.projectCode" placeholder="请输入项目代码" />
      </el-form-item>
      <el-form-item label="类别" prop="category">
        <el-input v-model="formData.category" placeholder="请输入类别" />
      </el-form-item>
      <el-form-item label="委托生产" prop="commissionedProduction">
        <el-input v-model="formData.commissionedProduction" placeholder="请输入委托生产" />
      </el-form-item>
      <el-form-item label="项目组负责人" prop="projectLeader">
        <el-input v-model="formData.projectLeader" placeholder="请输入项目组负责人" />
      </el-form-item>
      <el-form-item label="项目工程师" prop="projectEngineer">
        <el-input v-model="formData.projectEngineer" placeholder="请输入项目工程师" />
      </el-form-item>
      <el-form-item label="存放位置" prop="storageLocation">
        <el-input v-model="formData.storageLocation" placeholder="请输入存放位置" />
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input v-model="formData.priority" placeholder="请输入优先级" />
      </el-form-item>
      <el-form-item label="启用状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="DCC_PROJECT_CODE_STATUS_ENABLE">启用</el-radio>
          <el-radio :value="DCC_PROJECT_CODE_STATUS_DISABLE">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          :disabled="formLoading"
          @click="submitForm"
        >
          确定
        </el-button>
        <el-button class="scheme-d-btn scheme-d-btn--neutral" @click="formVisible = false">取消</el-button>
      </div>
    </template>
  </Dialog>

  <Dialog
    v-model="productOnboardingVisible"
    class="scheme-d-form-control"
    title="产品建档申请"
    width="820px"
  >
    <el-alert
      class="mb-12px"
      type="info"
      :closable="false"
      title="审批通过后生成 DCC 项目代码并绑定 MDM 产品"
      show-icon
    />
    <el-form
      ref="productOnboardingFormRef"
      v-loading="productOnboardingLoading"
      :model="productOnboardingFormData"
      :rules="productOnboardingFormRules"
      label-width="130px"
    >
      <el-form-item label="关联 MDM 产品" prop="productMasterId">
        <el-select
          v-model="productOnboardingFormData.productMasterId"
          class="!w-full"
          filterable
          clearable
          :loading="productOnboardingProductLoading"
          placeholder="可选择已有 MDM 产品；未选择时填写下方产品信息"
          @change="handleProductOnboardingMdmProductChange"
        >
          <el-option
            v-for="product in productOnboardingProducts"
            :key="product.id"
            :label="`${product.nameCn} / ${product.dccProductCode || product.productCode}`"
            :value="product.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="产品编码" prop="productCode">
        <el-input v-model="productOnboardingFormData.productCode" placeholder="请输入 MDM 产品编码" />
      </el-form-item>
      <el-form-item label="DCC 产品编号" prop="dccProductCode">
        <el-input v-model="productOnboardingFormData.dccProductCode" placeholder="14 位字母或数字" />
      </el-form-item>
      <el-form-item label="产品中文名" prop="productNameCn">
        <el-input v-model="productOnboardingFormData.productNameCn" placeholder="请输入产品中文名" />
      </el-form-item>
      <el-form-item label="产品英文名" prop="productNameEn">
        <el-input v-model="productOnboardingFormData.productNameEn" placeholder="请输入产品英文名" />
      </el-form-item>
      <el-form-item label="型号规格" prop="modelSpecification">
        <el-input v-model="productOnboardingFormData.modelSpecification" placeholder="请输入型号规格" />
      </el-form-item>
      <el-form-item label="产品类别" prop="productCategory">
        <el-input v-model="productOnboardingFormData.productCategory" placeholder="请输入产品类别" />
      </el-form-item>
      <el-form-item label="文控" prop="docControlNo">
        <el-input v-model="productOnboardingFormData.docControlNo" placeholder="请输入文控" />
      </el-form-item>
      <el-form-item label="目标项目名称" prop="projectName">
        <el-input v-model="productOnboardingFormData.projectName" placeholder="审批通过后生成的项目名称" />
      </el-form-item>
      <el-form-item label="目标项目代码" prop="projectCode">
        <el-input v-model="productOnboardingFormData.projectCode" placeholder="审批通过后生成的项目代码" />
      </el-form-item>
      <el-form-item label="DCC 类别" prop="category">
        <el-input v-model="productOnboardingFormData.category" placeholder="请输入 DCC 类别" />
      </el-form-item>
      <el-form-item label="项目组负责人" prop="projectLeader">
        <el-input v-model="productOnboardingFormData.projectLeader" placeholder="请输入项目组负责人" />
      </el-form-item>
      <el-form-item label="项目工程师" prop="projectEngineer">
        <el-input v-model="productOnboardingFormData.projectEngineer" placeholder="请输入项目工程师" />
      </el-form-item>
      <el-form-item label="存放位置" prop="storageLocation">
        <el-input v-model="productOnboardingFormData.storageLocation" placeholder="请输入存放位置" />
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input v-model="productOnboardingFormData.priority" placeholder="请输入优先级" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button class="scheme-d-btn scheme-d-btn--neutral" @click="productOnboardingVisible = false">
          取消
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--primary"
          type="primary"
          data-testid="dcc-product-onboarding-submit"
          :loading="productOnboardingSubmitting"
          @click="submitProductOnboardingRequest"
        >
          提交申请
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="success"
          data-testid="dcc-product-onboarding-approve"
          :disabled="!productOnboardingCreatedRequestId"
          :loading="productOnboardingApproving"
          @click="approveProductOnboardingCreatedRequest"
        >
          审批通过
        </el-button>
      </div>
    </template>
  </Dialog>

  <el-drawer
    v-model="detailDrawerVisible"
    class="scheme-d-basic-data-page scheme-d-basic-data-page--dcc-project-code scheme-d-form-control"
    title="DCC基础条目"
    size="96%"
    data-testid="dcc-project-code-detail-drawer"
  >
    <div v-loading="detailLoading" class="dcc-project-code-detail">
      <el-descriptions v-if="selectedProjectCode" :column="2" border>
        <el-descriptions-item label="文控">{{ selectedProjectCode.docControlNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag class="scheme-d-tag" :type="selectedProjectCode.status === 'ENABLE' ? 'success' : 'info'">
            {{ formatStatus(selectedProjectCode.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ selectedProjectCode.projectName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目代码">{{ selectedProjectCode.projectCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类别">{{ selectedProjectCode.category || '-' }}</el-descriptions-item>
        <el-descriptions-item label="委托生产">
          {{ selectedProjectCode.commissionedProduction || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="存放位置">{{ selectedProjectCode.storageLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ selectedProjectCode.priority || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="dcc-project-code-associated-heading">
        <span>关联文档</span>
        <div class="dcc-project-code-associated-heading-actions">
          <span
            v-if="aiCategoryRunning"
            class="dcc-project-code-ai-category-percent"
            data-testid="dcc-project-code-ai-category-percent"
          >
            AI分类中 {{ aiCategoryProgressPercent }}%
          </span>
          <el-button
            v-if="canRunAiCategory"
            class="scheme-d-btn scheme-d-btn--primary"
            data-testid="dcc-project-code-ai-category"
            size="small"
            type="primary"
            :loading="aiCategoryRunning"
            :disabled="
              !selectedProjectCode?.id ||
              associatedFilesLoading ||
              batchAiCategoryRunning ||
              listUnclassifiedAutoClassifyRunning ||
              unclassifiedAutoClassifyRunning
            "
            @click="handleAiCategoryAssociatedFiles"
          >
            AI分类
          </el-button>
          <el-button
            v-if="canRunAssociatedNameAutoClassify"
            class="scheme-d-btn scheme-d-btn--primary"
            data-testid="dcc-project-code-auto-classify-unclassified"
            size="small"
            type="primary"
            plain
            :loading="unclassifiedAutoClassifyRunning"
            :disabled="
              !selectedProjectCode?.id ||
              associatedFilesLoading ||
              aiCategoryRunning ||
              batchAiCategoryRunning ||
              listUnclassifiedAutoClassifyRunning ||
              associatedUnclassifiedFileCount === 0
            "
            @click="handleAutoClassifyUnclassifiedAssociatedFiles"
          >
            按文件名归类未分类
          </el-button>
          <el-button
            class="scheme-d-btn scheme-d-btn--primary"
            data-testid="dcc-project-code-assignment-open"
            size="small"
            type="primary"
            plain
            :disabled="!selectedProjectCode?.id"
            @click="openAssignmentDialog"
            v-hasPermi="['dcc:project-code-assignment:assign']"
          >
            分配修正
          </el-button>
          <el-button
            class="scheme-d-btn scheme-d-btn--neutral"
            data-testid="dcc-project-code-assignment-records"
            size="small"
            plain
            @click="openAssignmentRecords"
            v-hasPermi="['dcc:project-code-assignment:query']"
          >
            分配记录
          </el-button>
          <el-tag class="scheme-d-tag" size="small" type="info">共 {{ associatedFilesTotal }} 份</el-tag>
        </div>
      </div>
      <div
        v-loading="associatedFilesLoading"
        class="dcc-project-code-associated-files"
        data-testid="dcc-project-code-associated-files"
      >
        <template v-if="associatedNavigationFiles.length > 0">
          <div class="dcc-project-code-associated-layout">
            <section class="dcc-project-code-associated-panel">
              <div class="dcc-project-code-associated-panel-title">阶段</div>
              <div
                class="dcc-project-code-associated-stage-list"
                data-testid="dcc-project-code-associated-stage-list"
              >
                <button
                  v-for="stage in associatedStageGroups"
                  :key="stage.key"
                  type="button"
                  class="dcc-project-code-associated-list-item"
                  :class="{ 'is-active': selectedAssociatedStageKey === stage.key }"
                  @click="selectAssociatedStage(stage.key)"
                >
                  <span class="dcc-project-code-associated-item-label">{{ stage.label }}</span>
                  <el-tag class="scheme-d-tag" size="small" type="info">{{ stage.count }} 份</el-tag>
                </button>
              </div>
            </section>

            <section class="dcc-project-code-associated-panel">
              <div class="dcc-project-code-associated-panel-title">文件类型</div>
              <div
                v-if="selectedAssociatedStageGroup?.types.length"
                class="dcc-project-code-associated-type-list"
                data-testid="dcc-project-code-associated-type-list"
              >
                <button
                  v-for="typeGroup in selectedAssociatedStageGroup.types"
                  :key="typeGroup.key"
                  type="button"
                  class="dcc-project-code-associated-list-item"
                  :class="{ 'is-active': selectedAssociatedTypeKey === typeGroup.key }"
                  @click="selectAssociatedType(typeGroup.key)"
                >
                  <span class="dcc-project-code-associated-item-label">{{ typeGroup.label }}</span>
                  <el-tag class="scheme-d-tag" size="small" type="info">{{ typeGroup.files.length }} 份</el-tag>
                </button>
              </div>
              <el-empty v-else description="当前阶段暂无文件类型" :image-size="64" />
            </section>

            <section
              class="dcc-project-code-associated-panel dcc-project-code-associated-file-table"
              data-testid="dcc-project-code-associated-file-table"
            >
              <div class="dcc-project-code-associated-panel-title">
                <span>{{ selectedAssociatedTypeGroup?.label || '文件列表' }}</span>
                <el-tag class="scheme-d-tag" size="small" type="info">
                  {{ selectedAssociatedFilesTotal }} 份
                </el-tag>
              </div>
              <el-table
                :data="selectedAssociatedPagedFiles"
                :show-overflow-tooltip="true"
                :row-class-name="resolveAssociatedFileRowClassName"
                @selection-change="handleAssociatedFileSelectionChange"
              >
                <el-table-column type="selection" width="48" />
                <el-table-column label="文件名称" prop="fileName" min-width="360">
                  <template #default="{ row }">
                    <el-link type="primary" @click="openControlledFileDetail(row)">
                      {{ row.fileName || row.title || '-' }}
                    </el-link>
                    <el-tag
                      v-if="isAssociatedRouteFocus(row)"
                      class="ml-6px scheme-d-tag"
                      data-testid="dcc-project-code-associated-route-focus"
                      size="small"
                      type="success"
                    >
                      当前联动
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="文件编号" prop="fileNumber" min-width="280" />
                <el-table-column label="版本" prop="versionNo" width="90" />
                <el-table-column label="状态" prop="status" width="120" />
                <el-table-column label="发布时间" prop="publishedTime" width="180">
                  <template #default="{ row }">
                    {{ formatControlledFileDateTime(row.publishedTime) }}
                  </template>
                </el-table-column>
              </el-table>
              <Pagination
                v-if="selectedAssociatedFilesTotal > 0"
                v-model:limit="associatedFilePage.pageSize"
                v-model:page="associatedFilePage.pageNo"
                :total="selectedAssociatedFilesTotal"
                class="dcc-project-code-associated-file-pagination"
                @pagination="handleAssociatedFilePagination"
              />
            </section>
          </div>
        </template>
        <el-table v-else :data="[]" :show-overflow-tooltip="true">
          <el-table-column label="文件名称" prop="fileName" min-width="360" />
          <el-table-column label="文件编号" prop="fileNumber" min-width="280" />
          <el-table-column label="版本" prop="versionNo" width="90" />
          <el-table-column label="状态" prop="status" width="120" />
          <el-table-column label="发布时间" prop="publishedTime" width="180" />
        </el-table>
      </div>
    </div>
  </el-drawer>

  <Dialog v-model="assignmentDialogVisible" class="scheme-d-form-control" title="分配修正任务" width="920px">
    <el-form label-width="96px">
      <el-form-item label="被分配人">
        <el-select
          v-model="assignmentForm.assigneeUserId"
          class="!w-full"
          filterable
          :loading="assignmentUsersLoading"
          placeholder="请选择用户"
        >
          <el-option
            v-for="user in assignmentUsers"
            :key="user.id"
            :label="`${user.nickname || user.username} / ${user.username}`"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="分配范围">
        <el-radio-group v-model="assignmentForm.scopeMode">
          <el-radio-button :label="DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_ALL">
            当前关联全部文件
          </el-radio-button>
          <el-radio-button :label="DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_SELECTED">
            当前选中文件
          </el-radio-button>
        </el-radio-group>
        <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
          <template v-if="assignmentForm.scopeMode === DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_SELECTED">
            将按全局候选中勾选的 {{ selectedAssignmentCandidateIds.length }} 份文件生成快照。
          </template>
          <template v-else>
            将按后端当前有效项目代码口径生成 {{ associatedFilesTotal }} 份文件快照。
          </template>
        </div>
      </el-form-item>
      <el-form-item
        v-if="assignmentForm.scopeMode === DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_SELECTED"
        label="选择文件"
      >
        <div class="w-full" data-testid="dcc-project-code-assignment-global-search">
          <div class="mb-8px flex gap-8px">
            <el-input
              v-model="assignmentCandidateQuery.keyword"
              clearable
              placeholder="全局搜索文件名称或编号"
              @keyup.enter="searchAssignmentCandidates"
            />
            <el-button
              class="scheme-d-btn scheme-d-btn--primary"
              type="primary"
              :loading="assignmentCandidatesLoading"
              @click="searchAssignmentCandidates"
            >
              搜索
            </el-button>
          </div>
          <el-table
            v-loading="assignmentCandidatesLoading"
            :data="assignmentCandidates"
            row-key="id"
            :row-class-name="resolveAssignmentCandidateRowClass"
            @selection-change="handleAssignmentCandidateSelectionChange"
          >
            <el-table-column type="selection" width="48" :selectable="isAssignmentCandidateSelectable" />
            <el-table-column label="文件" min-width="220">
              <template #default="{ row }">{{ row.fileName || row.fileNumber || '-' }}</template>
            </el-table-column>
            <el-table-column label="当前项目" min-width="190">
              <template #default="{ row }">
                {{ row.currentProjectName || '未归属' }}
                <span v-if="row.currentProjectCode"> / {{ row.currentProjectCode }}</span>
              </template>
            </el-table-column>
            <el-table-column label="版本" prop="versionNo" width="80" />
            <el-table-column label="状态" prop="status" width="220">
              <template #default="{ row }">
                <el-tag class="scheme-d-tag" :type="row.selectable === false ? 'warning' : 'success'">
                  {{ row.status }}
                </el-tag>
                <div v-if="row.selectable === false" class="mt-4px text-12px text-[var(--el-color-warning)]">
                  {{
                    row.disabledReason ||
                    '审批中的文件不可创建修正任务，请先撤回或完成审批后处理'
                  }}
                </div>
              </template>
            </el-table-column>
          </el-table>
          <Pagination
            v-if="assignmentCandidatesTotal > 0"
            v-model:limit="assignmentCandidateQuery.pageSize"
            v-model:page="assignmentCandidateQuery.pageNo"
            :total="assignmentCandidatesTotal"
            @pagination="loadAssignmentCandidates"
          />
        </div>
      </el-form-item>
      <el-form-item label="有效期">
        <el-date-picker
          v-model="assignmentForm.expireTime"
          class="!w-full"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="不填表示长期有效"
        />
      </el-form-item>
      <el-form-item label="说明">
        <el-input
          v-model="assignmentForm.assignmentReason"
          clearable
          maxlength="512"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 5 }"
          placeholder="请输入分配原因"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button class="scheme-d-btn scheme-d-btn--neutral" @click="assignmentDialogVisible = false">
          取消
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          :loading="assignmentSubmitting"
          @click="submitAssignmentDialog"
        >
          创建分配
        </el-button>
      </div>
    </template>
  </Dialog>

  <el-drawer
    v-model="assignmentRecordsVisible"
    class="scheme-d-basic-data-page scheme-d-basic-data-page--dcc-project-code"
    title="分配记录"
    size="920px"
    data-testid="dcc-project-code-assignment-records-drawer"
  >
    <el-table
      v-loading="assignmentRecordsLoading"
      :data="assignmentRecords"
      :show-overflow-tooltip="true"
    >
      <el-table-column label="任务编号" prop="assignmentNo" min-width="180" />
      <el-table-column label="产品名称" prop="projectName" min-width="180">
        <template #default="{ row }">{{ row.projectName || '-' }}</template>
      </el-table-column>
      <el-table-column label="产品编号" prop="projectCode" min-width="140">
        <template #default="{ row }">{{ row.projectCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="被分配人" prop="assigneeNickname" min-width="120">
        <template #default="{ row }">{{ row.assigneeNickname || row.assigneeUserId }}</template>
      </el-table-column>
      <el-table-column label="文件" prop="fileCount" width="80" />
      <el-table-column label="已改文件" prop="changedFileCount" width="90" />
      <el-table-column label="字段" prop="changedFieldCount" width="80" />
      <el-table-column label="状态" prop="status" width="100" />
      <el-table-column label="分配时间" prop="assignedTime" width="180">
        <template #default="{ row }">{{ formatControlledFileDateTime(row.assignedTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            class="scheme-d-row-action scheme-d-row-action--primary"
            type="primary"
            @click="goAssignmentAudit(row)"
            v-hasPermi="['dcc:project-code-assignment:audit:query']"
          >
            追溯
          </el-button>
          <el-button
            link
            class="scheme-d-row-action scheme-d-row-action--danger"
            type="danger"
            :disabled="row.status !== 'ACTIVE'"
            @click="handleRevokeAssignment(row)"
            v-hasPermi="['dcc:project-code-assignment:revoke']"
          >
            撤回
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      v-model:limit="assignmentRecordQuery.pageSize"
      v-model:page="assignmentRecordQuery.pageNo"
      :total="assignmentRecordsTotal"
      @pagination="loadAssignmentRecords"
    />
  </el-drawer>
</template>

<script lang="ts" setup>
import { dateFormatter2 } from '@/utils/formatTime'
import { checkPermi, checkRole } from '@/utils/permission'
import download from '@/utils/download'
import type { FormRules } from 'element-plus'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  createControlledFileBatchRecognitionTask,
  exportControlledFileRecognitionRecordExcel,
  getControlledFileBatchRecognitionTask,
  getLatestControlledFileBatchRecognitionTask,
  updateControlledFileMetadata,
  type ControlledFileBatchRecognitionTaskRespVO,
  type ControlledFileMetadataUpdateReqVO,
  type ControlledFileVO
} from '@/api/dcc/controlledFile/workflow'
import {
  getFileTypeTaxonomyList,
  type DccFileTypeTaxonomyVO
} from '@/api/dcc/controlledFile/fileTypeTaxonomies'
import type {
  DccProjectCodeAssociatedFileAiCategoryRespVO,
  DccProjectCodeImportPreviewRespVO,
  DccProjectCodeImportRowRespVO,
  DccProjectCodePageReqVO,
  DccProjectCodeRespVO,
  DccProjectCodeSaveReqVO,
  DccProjectCodeUpdateReqVO,
  DccProductOnboardingCreateReqVO
} from '@/api/dcc/controlledFile/projectCodes'
import {
  approveProductOnboardingRequest,
  classifyProjectCodeAssociatedFileByAi,
  createProjectCode,
  createProductOnboardingRequest,
  DCC_PROJECT_CODE_STATUS_DISABLE,
  DCC_PROJECT_CODE_STATUS_ENABLE,
  deleteProjectCode,
  exportProjectCodeExcel,
  getProjectCodeAssociatedFileAiCategoryCandidates,
  getProjectCode,
  getProjectCodeControlledFilesPage,
  getProjectCodeImportTemplate,
  getProjectCodePage,
  importProjectCodeConfirm,
  importProjectCodePreview,
  updateProjectCode
} from '@/api/dcc/controlledFile/projectCodes'
import {
  getDccProjectGovernanceStatus,
  type DccProjectGovernanceStatusVO
} from '@/api/mes/pro/dccProjectGovernance'
import {
  QcTemplateApi,
  type QaInspectionRegulationProjectStatusVO
} from '@/api/mes/qc/template'
import { formatControlledFileDateTime } from '../../detail/presentation'
import { openControlledFileViewer } from '../../shared/viewer-navigation'
import {
  DCC_TECHNICAL_DOCUMENT_ROOT_NAME,
  DCC_UNCLASSIFIED_TAXONOMY_STAGE,
  type DccFileTypeTaxonomyStageTypeOption,
  buildDccFileTypeTaxonomyStageNameMap,
  buildDccFileTypeTaxonomyStageTypeNameMap,
  buildDccFileTypeTaxonomyStageTypeOptionsMap,
  getDccFileTypeTaxonomyStageRows,
  resolveDccFileTypeTaxonomyStageName,
  resolveDccFileTypeTaxonomyStageTypeName,
  toDccFileTypeTaxonomyStageOptions
} from '../../shared/file-type-taxonomy-stage'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import {
  DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_ALL,
  DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_SELECTED,
  createProjectCodeAssignment,
  getProjectCodeAssignmentCandidatePage,
  getProjectCodeAssignmentPage,
  revokeProjectCodeAssignment,
  type DccProjectCodeAssignmentCandidateRespVO,
  type DccProjectCodeAssignmentCreateReqVO,
  type DccProjectCodeAssignmentRespVO
} from '@/api/dcc/controlledFile/projectCodeAssignments'
import {
  getProductSimpleList,
  MDM_PRODUCT_STATUS_ENABLE,
  type MdmProductSimpleRespVO
} from '@/api/mdm/product'

defineOptions({ name: 'ProjectCodeTabPanel' })

type AssociatedTypeGroup = {
  key: string
  label: string
  taxonomyId?: number
  files: ControlledFileVO[]
}

type AssociatedStageGroup = {
  key: string
  label: string
  count: number
  types: AssociatedTypeGroup[]
}

type AssignmentUserOption = Pick<UserVO, 'id' | 'nickname' | 'username'> &
  Partial<Pick<UserVO, 'status' | 'disabled'>>

type ProductOnboardingFormData = DccProductOnboardingCreateReqVO

const DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE = 200
const DCC_PROJECT_CODE_LIST_AUTO_CLASSIFY_PAGE_SIZE = 100
const DCC_PROJECT_CODE_UNCLASSIFIED_TYPE = '未分类文件类型'
const BATCH_AI_CATEGORY_POLL_INTERVAL_MS = 1000

const projectCodeQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'docControlNo',
    label: '文控',
    type: 'text',
    queryParamKey: 'keyword',
    operators: ['contains'],
    placeholder: '请输入文控'
  },
  {
    key: 'primaryCode',
    label: '主编码',
    type: 'text',
    queryParamKey: 'keyword',
    operators: ['contains'],
    placeholder: '请输入主编码'
  },
  {
    key: 'projectName',
    label: '项目名称',
    type: 'text',
    queryParamKey: 'projectName',
    operators: ['contains'],
    placeholder: '请输入项目名称'
  },
  {
    key: 'projectCode',
    label: '项目代码',
    type: 'text',
    queryParamKey: 'projectCode',
    operators: ['contains'],
    placeholder: '请输入项目代码'
  },
  {
    key: 'category',
    label: '类别',
    type: 'text',
    queryParamKey: 'category',
    operators: ['eq'],
    placeholder: '请输入类别'
  }
]

const projectCodeDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'docControlNo', label: '文控', minWidth: 130 },
  { key: 'primaryCode', label: '主编码', minWidth: 100 },
  { key: 'projectName', label: '项目名称', minWidth: 220 },
  { key: 'projectCode', label: '项目代码', minWidth: 120 },
  { key: 'category', label: '类别', minWidth: 120 },
  { key: 'associatedFileCount', label: '关联文件数', width: 120 },
  { key: 'routeStatus', label: '工艺路线', width: 140 },
  { key: 'mainBatchRecordStatus', label: '主批记录', width: 150 },
  { key: 'lossReportStatus', label: '损耗单', width: 130 },
  { key: 'processInspectionStatus', label: '过程检验单', width: 150 },
  { key: 'parameterRecordStatus', label: '参数记录表', width: 150 },
  { key: 'qaRegulationStatus', label: 'QA规程', width: 130 },
  { key: 'updateTime', label: '更新时间', width: 180 },
  { key: 'actions', label: '关联文档', width: 240, hideable: false, business: false }
]

const {
  columns: projectCodeColumns,
  saving: projectCodeColumnSaving,
  isColumnVisible: isProjectCodeColumnVisible,
  getColumnWidthString: getProjectCodeColumnWidthString,
  getColumnMinWidthString: getProjectCodeColumnMinWidthString,
  handleHeaderDragend: handleProjectCodeHeaderDragend,
  saveConfig: saveProjectCodeColumnConfig
} = useUserTableColumns('dcc.projectCode.main', projectCodeDefaultColumns)

const message = useMessage()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const exportLoading = ref(false)
const previewLoading = ref(false)
const confirmLoading = ref(false)
const detailLoading = ref(false)
const associatedFilesLoading = ref(false)
const importVisible = ref(false)
const detailDrawerVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const hasLoaded = ref(false)
const list = ref<DccProjectCodeRespVO[]>([])
const total = ref(0)
const fileTypeTaxonomies = ref<DccFileTypeTaxonomyVO[]>([])
const dccProjectGovernanceByProjectName = ref<Record<string, DccProjectGovernanceStatusVO>>({})
const qaRegulationStatusByDccProjectCodeId = ref<
  Record<number, QaInspectionRegulationProjectStatusVO>
>({})
const qaRegulationStatusPermissionDenied = ref(false)
const selectedProjectCode = ref<DccProjectCodeRespVO | null>(null)
const associatedNavigationFiles = ref<ControlledFileVO[]>([])
const associatedFilesTotal = ref(0)
const assignmentDialogVisible = ref(false)
const assignmentUsersLoading = ref(false)
const assignmentSubmitting = ref(false)
const assignmentCandidatesLoading = ref(false)
const assignmentCandidates = ref<DccProjectCodeAssignmentCandidateRespVO[]>([])
const assignmentCandidatesTotal = ref(0)
const selectedAssignmentCandidateIds = ref<Array<number | string>>([])
const assignmentUsers = ref<AssignmentUserOption[]>([])
const assignmentRecordsVisible = ref(false)
const assignmentRecordsLoading = ref(false)
const assignmentRecords = ref<DccProjectCodeAssignmentRespVO[]>([])
const assignmentRecordsTotal = ref(0)
const selectedAssociatedFileIds = ref<Array<number | string>>([])
const selectedAssociatedStageKey = ref('')
const selectedAssociatedTypeKey = ref('')
const focusedAssociatedFileId = ref<number | null>(null)
const aiCategoryRunning = ref(false)
const aiCategoryProcessed = ref(0)
const aiCategoryTotal = ref(0)
const unclassifiedAutoClassifyRunning = ref(false)
const listUnclassifiedAutoClassifyRunning = ref(false)
const listUnclassifiedAutoClassifyTotalProjects = ref(0)
const listUnclassifiedAutoClassifyProcessedProjects = ref(0)
const listUnclassifiedAutoClassifyProcessedFiles = ref(0)
const batchAiCategoryTask = ref<ControlledFileBatchRecognitionTaskRespVO | null>(null)
const batchAiCategoryDismissedTaskId = ref<number | null>(null)
const batchAiCategoryFailureExporting = ref(false)
let detailRequestSequence = 0
let qaRegulationStatusLoadSerial = 0
let batchAiCategoryPollTimer: ReturnType<typeof setTimeout> | null = null
let batchAiCategoryTerminalHandledTaskId: number | null = null
const importFileList = ref<any[]>([])
const importFile = ref<File | null>(null)
const previewResult = ref<DccProjectCodeImportPreviewRespVO | null>(null)
const importRows = computed<DccProjectCodeImportRowRespVO[]>(() => previewResult.value?.rows || [])
const productOnboardingVisible = ref(false)
const productOnboardingLoading = ref(false)
const productOnboardingSubmitting = ref(false)
const productOnboardingApproving = ref(false)
const productOnboardingProductLoading = ref(false)
const productOnboardingCreatedRequestId = ref<number | null>(null)
const productOnboardingFormRef = ref()
const productOnboardingProducts = ref<MdmProductSimpleRespVO[]>([])
const productOnboardingFormData = reactive<ProductOnboardingFormData>({
  productMasterId: undefined,
  productCode: '',
  dccProductCode: '',
  productNameCn: '',
  productNameEn: '',
  modelSpecification: '',
  productCategory: '',
  docControlNo: '',
  projectName: '',
  projectCode: '',
  category: '',
  commissionedProduction: '',
  projectLeader: '',
  projectEngineer: '',
  storageLocation: '',
  priority: ''
})
const productOnboardingFormRules = reactive<FormRules>({
  projectName: [{ required: true, message: '目标项目名称不能为空', trigger: 'blur' }],
  projectCode: [{ required: true, message: '目标项目代码不能为空', trigger: 'blur' }]
})
const assignmentForm = reactive<{
  assigneeUserId?: number
  scopeMode: DccProjectCodeAssignmentCreateReqVO['scopeMode']
  expireTime: string
  assignmentReason: string
}>({
  assigneeUserId: undefined as number | undefined,
  scopeMode: DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_ALL,
  expireTime: '',
  assignmentReason: ''
})
const assignmentRecordQuery = reactive({
  pageNo: 1,
  pageSize: 10
})
const assignmentCandidateQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: ''
})
const aiCategoryProgressPercent = computed(() =>
  aiCategoryTotal.value === 0 ? 0 : Math.floor((aiCategoryProcessed.value * 100) / aiCategoryTotal.value)
)
const canRunAiCategory = computed(
  () =>
    checkPermi(['dcc:project-code:update']) &&
    checkPermi(['dcc:controlled-file:update'])
)
const canRunAssociatedNameAutoClassify = computed(() => checkPermi(['dcc:controlled-file:update']))
const canRunProjectCodeListNameAutoClassify = computed(
  () => canRunAssociatedNameAutoClassify.value
)
const canRunBatchAiCategory = computed(
  () => canRunAiCategory.value && checkRole(['doc_control'])
)
const listUnclassifiedAutoClassifyProgressVisible = computed(
  () => listUnclassifiedAutoClassifyRunning.value
)
const listUnclassifiedAutoClassifyProgressPercent = computed(() =>
  listUnclassifiedAutoClassifyTotalProjects.value === 0
    ? 0
    : Math.floor(
        (listUnclassifiedAutoClassifyProcessedProjects.value * 100) /
          listUnclassifiedAutoClassifyTotalProjects.value
      )
)
const batchAiCategoryRunning = computed(() =>
  ['WAITING', 'RUNNING'].includes(batchAiCategoryTask.value?.status || '')
)
const batchAiCategoryProgressVisible = computed(() => {
  const task = batchAiCategoryTask.value
  return Boolean(
    task &&
      task.status !== 'COMPLETED' &&
      batchAiCategoryDismissedTaskId.value !== task.taskId
  )
})
const batchAiCategoryProcessed = computed(() => batchAiCategoryTask.value?.processedCount || 0)
const batchAiCategoryTotal = computed(() => batchAiCategoryTask.value?.totalCount || 0)
const batchAiCategoryMatchedFileCount = computed(() => batchAiCategoryTask.value?.successCount || 0)
const batchAiCategoryUnclassifiedFileCount = computed(
  () => batchAiCategoryTask.value?.unclassifiedCount || 0
)
const batchAiCategoryAmbiguousFileCount = computed(
  () => batchAiCategoryTask.value?.ambiguousCount || 0
)
const batchAiCategoryConflictFileCount = computed(() => batchAiCategoryTask.value?.conflictCount || 0)
const batchAiCategoryFailedFileCount = computed(() => batchAiCategoryTask.value?.failedCount || 0)
const batchAiCategorySkippedFileCount = computed(
  () => batchAiCategoryTask.value?.skippedExistingCount || 0
)
const batchAiCategoryFailureSummaries = computed(
  () => batchAiCategoryTask.value?.failureSummaries || []
)
const batchAiCategoryFailureStageLabels: Record<string, string> = {
  PRECONDITION: '前置校验',
  SOURCE_ACCESS: '源文件读取',
  RULE_MATCHING: '规则匹配',
  AI_CLASSIFICATION: 'AI 分类调用',
  RESULT_VALIDATION: '结果校验',
  PERSISTENCE: '结果保存',
  BATCH_ORCHESTRATION: '批量任务调度',
  UNCLASSIFIED: '历史数据未分类'
}
const formatBatchAiCategoryFailureStage = (stage: string) =>
  batchAiCategoryFailureStageLabels[stage] || stage
const batchAiCategoryOutcomeCount = computed(
  () =>
    batchAiCategoryMatchedFileCount.value +
    batchAiCategoryUnclassifiedFileCount.value +
    batchAiCategoryAmbiguousFileCount.value +
    batchAiCategoryConflictFileCount.value +
    batchAiCategoryFailedFileCount.value +
    batchAiCategorySkippedFileCount.value
)
const batchAiCategoryConsistencyMessage = computed(() => {
  if (!batchAiCategoryTask.value) {
    return ''
  }
  const processed = batchAiCategoryProcessed.value
  const total = batchAiCategoryTotal.value
  const outcomeCount = batchAiCategoryOutcomeCount.value
  if (processed > total) {
    return `已处理 ${processed} 超过总数 ${total}，结果合计 ${outcomeCount} 与已处理 ${processed} 不一致，请重新发起任务`
  }
  if (outcomeCount !== processed) {
    return `结果合计 ${outcomeCount} 与已处理 ${processed} 不一致，请重新发起任务`
  }
  return ''
})
const batchAiCategoryInterruptionMessage = computed(
  () => batchAiCategoryTask.value?.lastFailureMessage || ''
)
const batchAiCategoryStatusText = computed(() => {
  const labels: Record<string, string> = {
    WAITING: '等待执行',
    RUNNING: '执行中',
    COMPLETED: '已完成',
    FAILED: '失败',
    STOPPED: '已停止'
  }
  return labels[batchAiCategoryTask.value?.status || ''] || '未知'
})
const batchAiCategoryProgressPercent = computed(() =>
  batchAiCategoryTotal.value === 0
    ? 0
    : Math.floor((batchAiCategoryProcessed.value * 100) / batchAiCategoryTotal.value)
)
const resolveAiCategoryErrorMessage = (error: unknown) => {
  if (typeof error === 'string') {
    return error
  }
  if (error && typeof error === 'object') {
    const record = error as Record<string, any>
    return (
      record?.response?.data?.msg ||
      record?.response?.data?.message ||
      record?.data?.msg ||
      record?.data?.message ||
      record?.message ||
      '未知后端错误'
    )
  }
  return '未知后端错误'
}
const normalizeAssociatedLevel = (level: unknown) => String(level || '').trim()
const normalizeAutoClassifyText = (value: unknown) =>
  String(value || '')
    .normalize('NFKC')
    .toLowerCase()
    .replace(/\.[a-z0-9]{1,8}$/i, '')
    .replace(/[\s_\-—–/\\()[\]{}【】（）《》<>.,，。:：;；!！?？"'“”‘’]+/g, '')
    .trim()
const splitAutoClassifyTokens = (value: unknown) => {
  const normalized = normalizeAutoClassifyText(value)
  const tokens = new Set<string>()
  const words = normalized.match(/[a-z0-9]+|[\u4e00-\u9fa5]/gi) || []
  for (const word of words) {
    if (word) {
      tokens.add(word)
    }
  }
  for (const gramSize of [2, 3, 4]) {
    for (let index = 0; index <= normalized.length - gramSize; index += 1) {
      tokens.add(normalized.slice(index, index + gramSize))
    }
  }
  return Array.from(tokens)
}
const autoClassifyTextSimilarityScore = (left: string, right: string) => {
  if (!left || !right) {
    return 0
  }
  if (left === right) {
    return 1
  }
  const previous = Array.from({ length: right.length + 1 }, (_, index) => index)
  for (let leftIndex = 1; leftIndex <= left.length; leftIndex += 1) {
    const current = [leftIndex]
    for (let rightIndex = 1; rightIndex <= right.length; rightIndex += 1) {
      const substitutionCost = left[leftIndex - 1] === right[rightIndex - 1] ? 0 : 1
      current[rightIndex] = Math.min(
        current[rightIndex - 1] + 1,
        previous[rightIndex] + 1,
        previous[rightIndex - 1] + substitutionCost
      )
    }
    previous.splice(0, previous.length, ...current)
  }
  return 1 - previous[right.length] / Math.max(left.length, right.length)
}
const autoClassifyTokenOverlapScore = (left: string, right: string) => {
  const leftTokens = new Set(splitAutoClassifyTokens(left))
  const rightTokens = splitAutoClassifyTokens(right)
  if (leftTokens.size === 0 || rightTokens.length === 0) {
    return 0
  }
  const matched = rightTokens.filter((token) => leftTokens.has(token)).length
  return matched / rightTokens.length
}
const autoClassifySubstringScore = (left: string, right: string) => {
  if (!left || !right) {
    return 0
  }
  if (left.includes(right)) {
    return 1
  }
  if (right.includes(left)) {
    return left.length / right.length
  }
  return 0
}
const associatedTaxonomyStageRows = computed(() =>
  getDccFileTypeTaxonomyStageRows(fileTypeTaxonomies.value)
)
const associatedTaxonomyStageOptions = computed(() =>
  toDccFileTypeTaxonomyStageOptions(associatedTaxonomyStageRows.value)
)
const associatedTaxonomyStageNames = computed(
  () => new Set(associatedTaxonomyStageOptions.value.map((option) => option.value))
)
const associatedTaxonomyStageNameMap = computed(() =>
  buildDccFileTypeTaxonomyStageNameMap(fileTypeTaxonomies.value)
)
const associatedTaxonomyStageTypeNameMap = computed(() =>
  buildDccFileTypeTaxonomyStageTypeNameMap(fileTypeTaxonomies.value)
)
const associatedTaxonomyStageTypeOptionsMap = computed(() =>
  buildDccFileTypeTaxonomyStageTypeOptionsMap(fileTypeTaxonomies.value)
)
const associatedAutoClassifyTargetOptions = computed(() =>
  Array.from(associatedTaxonomyStageTypeOptionsMap.value.values()).flat()
)
const resolveAssociatedStageKey = (file: ControlledFileVO) => {
  const stage = normalizeAssociatedLevel(file.fileTypeLevel2)
  if (stage && associatedTaxonomyStageNames.value.has(stage)) {
    return stage
  }
  return (
    resolveDccFileTypeTaxonomyStageName(file, associatedTaxonomyStageNameMap.value) ||
    DCC_UNCLASSIFIED_TAXONOMY_STAGE
  )
}
const resolveAssociatedTypeName = (file: ControlledFileVO) => {
  const resolvedTaxonomyType = resolveDccFileTypeTaxonomyStageTypeName(
    file,
    associatedTaxonomyStageTypeNameMap.value
  )
  return (
    resolvedTaxonomyType?.typeName ||
    normalizeAssociatedLevel(file.fileTypeLevel3) ||
    DCC_PROJECT_CODE_UNCLASSIFIED_TYPE
  )
}
const isAssociatedFileUnclassified = (file: ControlledFileVO) =>
  resolveAssociatedStageKey(file) === DCC_UNCLASSIFIED_TAXONOMY_STAGE ||
  resolveAssociatedTypeName(file) === DCC_PROJECT_CODE_UNCLASSIFIED_TYPE
const associatedUnclassifiedFiles = computed(() =>
  associatedNavigationFiles.value.filter(isAssociatedFileUnclassified)
)
const associatedUnclassifiedFileCount = computed(() => associatedUnclassifiedFiles.value.length)
const calculateAutoClassifySimilarity = (
  file: ControlledFileVO,
  target: DccFileTypeTaxonomyStageTypeOption
) => {
  const fileText = normalizeAutoClassifyText(
    `${file.fileName || ''} ${file.title || ''} ${file.fileNumber || ''}`
  )
  const targetTypeText = normalizeAutoClassifyText(target.label)
  const targetPathText = normalizeAutoClassifyText(`${target.stageName} ${target.label}`)
  return (
    autoClassifySubstringScore(fileText, targetTypeText) * 0.45 +
    autoClassifyTextSimilarityScore(fileText, targetTypeText) * 0.25 +
    autoClassifyTokenOverlapScore(fileText, targetTypeText) * 0.2 +
    autoClassifyTokenOverlapScore(fileText, targetPathText) * 0.1
  )
}
const resolveBestAssociatedAutoClassifyTarget = (
  file: ControlledFileVO,
  targetOptions: DccFileTypeTaxonomyStageTypeOption[]
) => {
  if (targetOptions.length === 0) {
    return undefined
  }
  let bestTarget = targetOptions[0]
  let bestScore = Number.NEGATIVE_INFINITY
  for (const target of targetOptions) {
    const score = calculateAutoClassifySimilarity(file, target)
    if (score > bestScore) {
      bestTarget = target
      bestScore = score
    }
  }
  return bestTarget
}
const buildDccAssociatedFileAutoClassifyPayload = (
  file: ControlledFileVO,
  target: DccFileTypeTaxonomyStageTypeOption,
  projectCode?: DccProjectCodeRespVO | null
): ControlledFileMetadataUpdateReqVO => {
  const fileName = normalizeAssociatedLevel(file.fileName || file.title)
  if (!fileName) {
    throw new Error(`文件 ${file.id} 缺少文件名称，无法自动归类`)
  }
  if (!file.categoryId || !file.directoryId) {
    throw new Error(`文件 ${fileName} 缺少文件类别或目录，无法保存分类`)
  }
  const ownerProjectCode = projectCode || selectedProjectCode.value
  return {
    changeReason: `按文件名相似度自动归类未分类文件：${target.stageName}/${target.label}`,
    productMasterId: null,
    productName: normalizeAssociatedLevel(ownerProjectCode?.projectName || file.productName) || undefined,
    dccProjectCodeId: ownerProjectCode?.id || file.dccProjectCodeId || null,
    needTraining: Boolean(file.needTraining),
    fileTypeTaxonomyId: target.taxonomyId,
    fileTypeLevel1: DCC_TECHNICAL_DOCUMENT_ROOT_NAME,
    fileTypeLevel2: target.stageName,
    fileTypeLevel3: target.label,
    fileTypeLevel4: null,
    fileTypeLevel5: null,
    fileName,
    productCode: normalizeAssociatedLevel(ownerProjectCode?.projectCode || file.productCode) || undefined,
    fileNumber: normalizeAssociatedLevel(file.fileNumber) || null,
    categoryId: file.categoryId,
    directoryId: file.directoryId
  }
}
const createAssociatedStageGroup = (stageKey: string, label = stageKey): AssociatedStageGroup => {
  const associatedStageTypeOptions = associatedTaxonomyStageTypeOptionsMap.value.get(stageKey) || []
  const typeMap = new Map<string, AssociatedTypeGroup>()
  for (const option of associatedStageTypeOptions) {
    typeMap.set(option.value, {
      key: option.value,
      label: option.label,
      taxonomyId: option.taxonomyId,
      files: []
    })
  }
  return {
    key: stageKey,
    label,
    count: 0,
    types: Array.from(typeMap.values())
  }
}
const associatedStageGroups = computed<AssociatedStageGroup[]>(() => {
  const stageMap = new Map<string, AssociatedStageGroup>()
  for (const option of associatedTaxonomyStageOptions.value) {
    stageMap.set(option.value, createAssociatedStageGroup(option.value, option.label))
  }

  for (const file of associatedNavigationFiles.value) {
    const stageKey = resolveAssociatedStageKey(file)
    let stageGroup = stageMap.get(stageKey)
    if (!stageGroup) {
      stageGroup = createAssociatedStageGroup(stageKey)
      stageMap.set(stageKey, stageGroup)
    }
    const typeName = resolveAssociatedTypeName(file)
    let typeGroup = stageGroup.types.find((item) => item.key === typeName)
    if (!typeGroup) {
      typeGroup = { key: typeName, label: typeName, files: [] }
      stageGroup.types.push(typeGroup)
    }
    typeGroup.files.push(file)
    stageGroup.count += 1
  }

  return Array.from(stageMap.values()).filter(
    (stage) => associatedTaxonomyStageNames.value.has(stage.key) || stage.count > 0
  )
})
const selectedAssociatedStageGroup = computed(() =>
  associatedStageGroups.value.find((stage) => stage.key === selectedAssociatedStageKey.value)
)
const selectedAssociatedTypeGroup = computed(() =>
  selectedAssociatedStageGroup.value?.types.find(
    (typeGroup) => typeGroup.key === selectedAssociatedTypeKey.value
  )
)
const associatedFilePage = reactive({
  pageNo: 1,
  pageSize: 10
})
const selectedAssociatedFilesTotal = computed(() => selectedAssociatedTypeGroup.value?.files.length || 0)
const selectedAssociatedPagedFiles = computed(() => {
  const files = selectedAssociatedTypeGroup.value?.files || []
  const start = (associatedFilePage.pageNo - 1) * associatedFilePage.pageSize
  return files.slice(start, start + associatedFilePage.pageSize)
})
const resetAssociatedFilePage = () => {
  associatedFilePage.pageNo = 1
}
const handleAssociatedFilePagination = () => {
  const maxPage = Math.max(
    1,
    Math.ceil(selectedAssociatedFilesTotal.value / associatedFilePage.pageSize)
  )
  if (associatedFilePage.pageNo > maxPage) {
    associatedFilePage.pageNo = maxPage
  }
}
const resolveAssociatedInitialTypeKey = (stage: AssociatedStageGroup) =>
  stage.types.find((typeGroup) => typeGroup.files.length > 0)?.key || stage.types[0]?.key || ''
const ensureAssociatedSelection = () => {
  const stages = associatedStageGroups.value
  if (stages.length === 0) {
    selectedAssociatedStageKey.value = ''
    selectedAssociatedTypeKey.value = ''
    resetAssociatedFilePage()
    return
  }
  const currentStage = stages.find((stage) => stage.key === selectedAssociatedStageKey.value)
  const nextStage = currentStage || stages.find((stage) => stage.count > 0) || stages[0]
  const previousStageKey = selectedAssociatedStageKey.value
  const previousTypeKey = selectedAssociatedTypeKey.value
  selectedAssociatedStageKey.value = nextStage.key
  const currentType = nextStage.types.find((typeGroup) => typeGroup.key === selectedAssociatedTypeKey.value)
  selectedAssociatedTypeKey.value = currentType?.key || resolveAssociatedInitialTypeKey(nextStage)
  if (
    previousStageKey !== selectedAssociatedStageKey.value ||
    previousTypeKey !== selectedAssociatedTypeKey.value
  ) {
    resetAssociatedFilePage()
  }
  handleAssociatedFilePagination()
}
const selectAssociatedStage = (stageKey: string) => {
  selectedAssociatedStageKey.value = stageKey
  const stage = associatedStageGroups.value.find((item) => item.key === stageKey)
  selectedAssociatedTypeKey.value = stage ? resolveAssociatedInitialTypeKey(stage) : ''
  selectedAssociatedFileIds.value = []
  resetAssociatedFilePage()
}
const selectAssociatedType = (typeKey: string) => {
  selectedAssociatedTypeKey.value = typeKey
  selectedAssociatedFileIds.value = []
  resetAssociatedFilePage()
}

const isSameAssociatedFileId = (file: ControlledFileVO, fileId?: number | null) =>
  Boolean(fileId && Number(file.id) === Number(fileId))

const isAssociatedRouteFocus = (row: ControlledFileVO) =>
  isSameAssociatedFileId(row, focusedAssociatedFileId.value)

const resolveAssociatedFileRowClassName = ({ row }: { row: ControlledFileVO }) =>
  isAssociatedRouteFocus(row) ? 'is-associated-route-focus' : ''

const resolveAssociatedRouteFocusFile = () => {
  const associatedFileId = resolveQueryAssociatedFileId()
  if (associatedFileId) {
    return associatedNavigationFiles.value.find((file) => isSameAssociatedFileId(file, associatedFileId))
  }
  const taxonomyId = resolveQueryAssociatedTaxonomyId()
  if (taxonomyId) {
    return associatedNavigationFiles.value.find((file) => Number(file.fileTypeTaxonomyId) === taxonomyId)
  }
  return undefined
}

const applyAssociatedRouteFocus = () => {
  const associatedFocus = route.query.associatedFocus
  const associatedFileId = resolveQueryAssociatedFileId()
  const taxonomyId = resolveQueryAssociatedTaxonomyId()
  if (!associatedFocus && !associatedFileId && !taxonomyId) {
    focusedAssociatedFileId.value = null
    return false
  }
  focusedAssociatedFileId.value = associatedFileId ?? null
  const targetFile = resolveAssociatedRouteFocusFile()
  if (!targetFile) {
    return false
  }
  selectedAssociatedStageKey.value = resolveAssociatedStageKey(targetFile)
  selectedAssociatedTypeKey.value = resolveAssociatedTypeName(targetFile)
  resetAssociatedFilePage()
  const files = selectedAssociatedTypeGroup.value?.files || []
  const focusedIndex = files.findIndex((file) => isSameAssociatedFileId(file, associatedFileId))
  if (focusedIndex >= 0) {
    associatedFilePage.pageNo = Math.floor(focusedIndex / associatedFilePage.pageSize) + 1
  }
  handleAssociatedFilePagination()
  return true
}
const formData = ref<DccProjectCodeUpdateReqVO>({
  id: 0,
  productMasterId: undefined,
  docControlNo: '',
  projectName: '',
  projectCode: '',
  category: '',
  commissionedProduction: '',
  projectLeader: '',
  projectEngineer: '',
  storageLocation: '',
  priority: '',
  status: DCC_PROJECT_CODE_STATUS_ENABLE
})

const formRules = reactive<FormRules>({
  projectName: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '启用状态不能为空', trigger: 'change' }]
})

type DccProjectCodePageQuery = DccProjectCodePageReqVO & {
  pageNo: number
  pageSize: number
}

const queryParams = reactive<DccProjectCodePageQuery>({
  pageNo: 1,
  pageSize: 10,
  keyword: undefined,
  projectName: undefined,
  projectCode: undefined,
  category: undefined,
  priority: undefined,
  status: undefined
})

const resolveQueryProjectCodeId = () =>
  Array.isArray(route.query.projectCodeId) ? route.query.projectCodeId[0] : route.query.projectCodeId

const resolveQueryAssociatedFileId = () => {
  const raw = Array.isArray(route.query.associatedFileId)
    ? route.query.associatedFileId[0]
    : route.query.associatedFileId
  const value = Number(raw)
  return Number.isFinite(value) && value > 0 ? value : undefined
}

const resolveQueryAssociatedTaxonomyId = () => {
  const raw = Array.isArray(route.query.fileTypeTaxonomyId)
    ? route.query.fileTypeTaxonomyId[0]
    : route.query.fileTypeTaxonomyId
  const value = Number(raw)
  return Number.isFinite(value) && value > 0 ? value : undefined
}

const resetFormData = () => {
  formData.value = {
    id: 0,
    productMasterId: undefined,
    docControlNo: '',
    projectName: '',
    projectCode: '',
    category: '',
    commissionedProduction: '',
    projectLeader: '',
    projectEngineer: '',
    storageLocation: '',
    priority: '',
    status: DCC_PROJECT_CODE_STATUS_ENABLE
  }
  formRef.value?.resetFields()
}

const getDccProjectGovernance = (projectName?: string) =>
  projectName ? dccProjectGovernanceByProjectName.value[projectName] : undefined

const formatDccProjectGovernanceStatus = (status?: string) => {
  if (status === 'OK') {
    return '已配置'
  }
  if (status === 'DUPLICATE') {
    return '重复'
  }
  return '未配置'
}

const formatDccProjectGovernanceVersions = (versionNos?: string[] | null) => {
  const normalizedVersionNos = Array.from(
    new Set(
      (versionNos || [])
        .map((versionNo) => String(versionNo || '').trim())
        .filter(Boolean)
    )
  )
  return normalizedVersionNos.join('、')
}

const resolveDccProjectGovernanceTagType = (status?: string) => {
  if (status === 'OK') {
    return 'success'
  }
  if (status === 'DUPLICATE') {
    return 'danger'
  }
  return 'info'
}

const loadDccProjectGovernanceStatus = async (rows: DccProjectCodeRespVO[]) => {
  const projectNames = rows.map((row) => row.projectName).filter(Boolean)
  if (projectNames.length === 0) {
    dccProjectGovernanceByProjectName.value = {}
    return
  }
  const statuses = await getDccProjectGovernanceStatus(projectNames)
  dccProjectGovernanceByProjectName.value = Object.fromEntries(
    statuses.map((item) => [item.projectName, item])
  )
}

const loadQaRegulationStatuses = async (rows: DccProjectCodeRespVO[]) => {
  const loadSerial = ++qaRegulationStatusLoadSerial
  const dccProjectCodeIds = rows
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0)
  if (dccProjectCodeIds.length === 0) {
    if (loadSerial === qaRegulationStatusLoadSerial) {
      qaRegulationStatusPermissionDenied.value = false
      qaRegulationStatusByDccProjectCodeId.value = {}
    }
    return
  }
  if (!checkPermi(['mes:qc-template:query'])) {
    if (loadSerial === qaRegulationStatusLoadSerial) {
      qaRegulationStatusPermissionDenied.value = true
      qaRegulationStatusByDccProjectCodeId.value = {}
    }
    return
  }
  const statuses = await QcTemplateApi.getQaRegulationProjectStatuses(dccProjectCodeIds)
  if (loadSerial !== qaRegulationStatusLoadSerial) {
    return
  }
  qaRegulationStatusPermissionDenied.value = false
  qaRegulationStatusByDccProjectCodeId.value = Object.fromEntries(
    statuses.map((status) => [status.dccProjectCodeId, status])
  )
}

const formatQaRegulationStatus = (dccProjectCodeId?: number) => {
  if (qaRegulationStatusPermissionDenied.value) {
    return '无查询权限'
  }
  const status = dccProjectCodeId
    ? qaRegulationStatusByDccProjectCodeId.value[dccProjectCodeId]
    : undefined
  if (status?.lifecycleStatus === 'PUBLISHED') {
    return '已发布'
  }
  if (status?.configured) {
    return '草稿'
  }
  return '未配置'
}

const openQaRegulation = (row: DccProjectCodeRespVO) => {
  router.push({
    name: 'MesProProcessPoolQaRegulation',
    query: { dccProjectCodeId: String(row.id) }
  })
}

const loadFileTypeTaxonomies = async () => {
  fileTypeTaxonomies.value = await getFileTypeTaxonomyList()
}

const getList = async () => {
  loading.value = true
  try {
    const data = await getProjectCodePage(queryParams)
    list.value = data.list
    total.value = data.total
    await Promise.all([
      loadDccProjectGovernanceStatus(data.list),
      loadQaRegulationStatuses(data.list)
    ])
  } finally {
    loading.value = false
  }
}

const fetchAllFilteredProjectCodes = async () => {
  const fetchProjectCodePage = (pageNo: number) =>
    getProjectCodePage({
      ...queryParams,
      pageNo,
      pageSize: DCC_PROJECT_CODE_LIST_AUTO_CLASSIFY_PAGE_SIZE
    })
  const firstPage = await fetchProjectCodePage(1)
  const projectCodes = [...firstPage.list]
  const total = firstPage.total
  const pageCount = Math.ceil(total / DCC_PROJECT_CODE_LIST_AUTO_CLASSIFY_PAGE_SIZE)
  for (let pageNo = 2; pageNo <= pageCount; pageNo += 1) {
    const data = await fetchProjectCodePage(pageNo)
    projectCodes.push(...data.list)
  }
  return { projectCodes, total, pageCount }
}

const projectCodeQuickFilter = useTableQuickFilter(
  'dcc.projectCode.main',
  projectCodeQuickFilterDefinitions,
  queryParams,
  getList
)

const ensureLoaded = async () => {
  if (hasLoaded.value) {
    return
  }
  await Promise.all([getList(), loadFileTypeTaxonomies()])
  hasLoaded.value = true
}

const handleSortChange = ({ prop, order }: { prop?: string; order?: string | null }) => {
  queryParams.pageNo = 1
  if (prop !== 'associatedFileCount' || !order) {
    queryParams.fileCountSort = undefined
    getList()
    return
  }
  const sortOrder = order
  queryParams.fileCountSort = sortOrder === 'ascending' ? 'asc' : 'desc'
  getList()
}

const openForm = (type: 'create' | 'update', row?: DccProjectCodeRespVO) => {
  formVisible.value = true
  formType.value = type
  resetFormData()
  if (type === 'update' && row) {
    formData.value = {
      id: row.id,
      productMasterId: row.productMasterId,
      docControlNo: row.docControlNo || '',
      projectName: row.projectName,
      projectCode: row.projectCode || '',
      category: row.category || '',
      commissionedProduction: row.commissionedProduction || '',
      projectLeader: row.projectLeader || '',
      projectEngineer: row.projectEngineer || '',
      storageLocation: row.storageLocation || '',
      priority: row.priority || '',
      status: row.status
    }
  }
}

const buildSavePayload = (): DccProjectCodeSaveReqVO => ({
  productMasterId: formData.value.productMasterId,
  docControlNo: formData.value.docControlNo,
  projectName: formData.value.projectName,
  projectCode: formData.value.projectCode,
  category: formData.value.category,
  commissionedProduction: formData.value.commissionedProduction,
  projectLeader: formData.value.projectLeader,
  projectEngineer: formData.value.projectEngineer,
  storageLocation: formData.value.storageLocation,
  priority: formData.value.priority,
  status: formData.value.status
})

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await createProjectCode(buildSavePayload())
      message.success('新增项目代码成功')
    } else {
      await updateProjectCode({
        ...buildSavePayload(),
        id: formData.value.id
      })
      message.success('编辑项目代码成功')
    }
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const resetProductOnboardingFormData = () => {
  Object.assign(productOnboardingFormData, {
    productMasterId: undefined,
    productCode: '',
    dccProductCode: '',
    productNameCn: '',
    productNameEn: '',
    modelSpecification: '',
    productCategory: '',
    docControlNo: '',
    projectName: '',
    projectCode: '',
    category: '',
    commissionedProduction: '',
    projectLeader: '',
    projectEngineer: '',
    storageLocation: '',
    priority: ''
  })
  productOnboardingCreatedRequestId.value = null
  productOnboardingFormRef.value?.resetFields()
}

const loadProductOnboardingProducts = async () => {
  productOnboardingProductLoading.value = true
  try {
    productOnboardingProducts.value = await getProductSimpleList({
      status: MDM_PRODUCT_STATUS_ENABLE,
      requireDccProductCode: true
    })
  } finally {
    productOnboardingProductLoading.value = false
  }
}

const openProductOnboardingDialog = async () => {
  productOnboardingVisible.value = true
  productOnboardingLoading.value = true
  resetProductOnboardingFormData()
  try {
    await loadProductOnboardingProducts()
  } finally {
    productOnboardingLoading.value = false
  }
}

const handleProductOnboardingMdmProductChange = (productId?: number | string) => {
  const selectedProduct = productOnboardingProducts.value.find(
    (product) => Number(product.id) === Number(productId)
  )
  if (!selectedProduct) {
    return
  }
  productOnboardingFormData.productCode = selectedProduct.productCode
  productOnboardingFormData.dccProductCode = selectedProduct.dccProductCode || ''
  productOnboardingFormData.productNameCn = selectedProduct.nameCn
  productOnboardingFormData.productNameEn = selectedProduct.nameEn || ''
  productOnboardingFormData.modelSpecification = selectedProduct.modelSpecification || ''
  productOnboardingFormData.productCategory = selectedProduct.category || ''
  if (!productOnboardingFormData.projectName) {
    productOnboardingFormData.projectName = selectedProduct.nameCn
  }
  if (!productOnboardingFormData.projectCode && selectedProduct.dccProductCode) {
    productOnboardingFormData.projectCode = selectedProduct.dccProductCode
  }
}

const submitProductOnboardingRequest = async () => {
  const valid = await productOnboardingFormRef.value?.validate()
  if (!valid) {
    return
  }
  productOnboardingSubmitting.value = true
  try {
    productOnboardingCreatedRequestId.value = await createProductOnboardingRequest({
      ...productOnboardingFormData,
      productMasterId: productOnboardingFormData.productMasterId || undefined
    })
    message.success('产品建档申请已提交')
  } finally {
    productOnboardingSubmitting.value = false
  }
}

const approveProductOnboardingCreatedRequest = async () => {
  if (!productOnboardingCreatedRequestId.value) {
    return
  }
  productOnboardingApproving.value = true
  try {
    await approveProductOnboardingRequest(productOnboardingCreatedRequestId.value)
    message.success('产品建档申请已审批通过')
    productOnboardingVisible.value = false
    await getList()
  } finally {
    productOnboardingApproving.value = false
  }
}

const openImportDialog = () => {
  importVisible.value = true
  importFileList.value = []
  importFile.value = null
  previewResult.value = null
}

const handleImportFileChange = (uploadFile: any) => {
  importFile.value = uploadFile.raw || null
  previewResult.value = null
}

const handleImportFileRemove = () => {
  importFile.value = null
  previewResult.value = null
}

const handleDownloadTemplate = async () => {
  const data = await getProjectCodeImportTemplate()
  download.excel(data, '项目代码导入模板.xlsx')
}

const handleImportPreview = async () => {
  if (!importFile.value) {
    message.error('请选择项目代码 Excel 文件')
    return
  }
  previewLoading.value = true
  try {
    previewResult.value = await importProjectCodePreview(importFile.value)
  } finally {
    previewLoading.value = false
  }
}

const handleImportConfirm = async () => {
  if (!previewResult.value || previewResult.value.failureCount > 0) {
    return
  }
  confirmLoading.value = true
  try {
    previewResult.value = await importProjectCodeConfirm(previewResult.value.batchId)
    message.success('导入完成')
    await getList()
  } finally {
    confirmLoading.value = false
  }
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const data = await exportProjectCodeExcel(queryParams)
    download.excel(data, '项目代码.xlsx')
  } finally {
    exportLoading.value = false
  }
}

const handleDelete = async (row: DccProjectCodeRespVO) => {
  try {
    await message.delConfirm(`确认删除项目代码“${row.projectName}”吗？`)
  } catch {
    return
  }
  loading.value = true
  try {
    await deleteProjectCode(row.id)
    message.success('删除项目代码成功')
    await getList()
  } finally {
    loading.value = false
  }
}

const resetAssociatedFilesState = () => {
  associatedNavigationFiles.value = []
  associatedFilesTotal.value = 0
  selectedAssociatedStageKey.value = ''
  selectedAssociatedTypeKey.value = ''
  selectedAssociatedFileIds.value = []
  resetAssociatedFilePage()
}

const getAssociatedFiles = async (
  projectCodeIdOverride?: number | string,
  requestToken?: number
) => {
  const canApplyDetailResult = () =>
    typeof requestToken === 'undefined' || requestToken === detailRequestSequence
  const projectCodeId = projectCodeIdOverride ?? selectedProjectCode.value?.id
  if (!projectCodeId) {
    if (canApplyDetailResult()) {
      resetAssociatedFilesState()
    }
    return
  }
  if (canApplyDetailResult()) {
    associatedFilesLoading.value = true
  }
  try {
    await loadFileTypeTaxonomies()
    const navigationFiles: ControlledFileVO[] = []
    const fetchNavigationPage = (pageNo: number) =>
      getProjectCodeControlledFilesPage(
        projectCodeId,
        {
          pageNo,
          pageSize: DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE,
          keyword: undefined,
          status: undefined
        }
      )
    const firstPage = await fetchNavigationPage(1)
    navigationFiles.push(...firstPage.list)
    const total = firstPage.total
    const pageCount = Math.ceil(total / DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE)
    for (let pageNo = 2; pageNo <= pageCount; pageNo += 1) {
      const data = await fetchNavigationPage(pageNo)
      navigationFiles.push(...data.list)
    }
    if (!canApplyDetailResult()) {
      return
    }
    associatedNavigationFiles.value = navigationFiles
    associatedFilesTotal.value = total
    resetAssociatedFilePage()
    if (!applyAssociatedRouteFocus()) {
      ensureAssociatedSelection()
    }
  } finally {
    if (canApplyDetailResult()) {
      associatedFilesLoading.value = false
    }
  }
}

const fetchProjectCodeAssociatedFiles = async (projectCodeId: number | string) => {
  const associatedFiles: ControlledFileVO[] = []
  const fetchAssociatedPage = (pageNo: number) =>
    getProjectCodeControlledFilesPage(projectCodeId, {
      pageNo,
      pageSize: DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE,
      keyword: undefined,
      status: undefined
    })
  const firstPage = await fetchAssociatedPage(1)
  associatedFiles.push(...firstPage.list)
  const pageCount = Math.ceil(firstPage.total / DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE)
  for (let pageNo = 2; pageNo <= pageCount; pageNo += 1) {
    const data = await fetchAssociatedPage(pageNo)
    associatedFiles.push(...data.list)
  }
  return associatedFiles
}

const loadAssociatedFilesForDetail = async (projectCodeId: number | string, requestToken: number) => {
  await getAssociatedFiles(projectCodeId, requestToken)
}

const loadAssignmentUsers = async () => {
  assignmentUsersLoading.value = true
  try {
    assignmentUsers.value = (await getSimpleUserList()).filter(
      (user: AssignmentUserOption) =>
        user.disabled !== true && (typeof user.status === 'undefined' || user.status === 0)
    )
  } finally {
    assignmentUsersLoading.value = false
  }
}

const resetAssignmentForm = () => {
  assignmentForm.assigneeUserId = undefined
  assignmentForm.scopeMode = DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_ALL
  assignmentForm.expireTime = ''
  assignmentForm.assignmentReason = ''
  selectedAssignmentCandidateIds.value = []
  assignmentCandidateQuery.pageNo = 1
  assignmentCandidateQuery.keyword = ''
}

const handleAssociatedFileSelectionChange = (rows: ControlledFileVO[]) => {
  selectedAssociatedFileIds.value = rows
    .map((row) => row.id as number | string)
    .filter((id): id is number | string => id !== null && typeof id !== 'undefined' && String(id).length > 0)
}

const loadAssignmentCandidates = async () => {
  const projectCodeId = selectedProjectCode.value?.id
  if (!projectCodeId) {
    assignmentCandidates.value = []
    assignmentCandidatesTotal.value = 0
    return
  }
  assignmentCandidatesLoading.value = true
  try {
    const data = await getProjectCodeAssignmentCandidatePage(projectCodeId, assignmentCandidateQuery)
    assignmentCandidates.value = data.list
    assignmentCandidatesTotal.value = data.total
  } finally {
    assignmentCandidatesLoading.value = false
  }
}

const searchAssignmentCandidates = async () => {
  assignmentCandidateQuery.pageNo = 1
  await loadAssignmentCandidates()
}

const handleAssignmentCandidateSelectionChange = (
  rows: DccProjectCodeAssignmentCandidateRespVO[]
) => {
  selectedAssignmentCandidateIds.value = rows.filter((row) => row.selectable !== false).map((row) => row.id)
}

const isAssignmentCandidateSelectable = (row: DccProjectCodeAssignmentCandidateRespVO) =>
  row.selectable !== false

const resolveAssignmentCandidateRowClass = ({ row }: { row: DccProjectCodeAssignmentCandidateRespVO }) =>
  row.selectable === false ? 'is-disabled' : ''

const openAssignmentDialog = async () => {
  if (!selectedProjectCode.value?.id) {
    return
  }
  resetAssignmentForm()
  assignmentDialogVisible.value = true
  await Promise.all([loadAssignmentUsers(), loadAssignmentCandidates()])
}

const submitAssignmentDialog = async () => {
  const projectCodeId = selectedProjectCode.value?.id
  if (!projectCodeId || !assignmentForm.assigneeUserId) {
    message.error('请选择被分配人')
    return
  }
  if (
    assignmentForm.scopeMode === DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_SELECTED &&
    selectedAssignmentCandidateIds.value.length === 0
  ) {
    message.error('请选择需要分配的文件')
    return
  }
  assignmentSubmitting.value = true
  try {
    const payload: DccProjectCodeAssignmentCreateReqVO = {
      assigneeUserId: assignmentForm.assigneeUserId,
      scopeMode: assignmentForm.scopeMode,
      fileIds: selectedAssignmentCandidateIds.value,
      expireTime: assignmentForm.expireTime || null,
      assignmentReason: assignmentForm.assignmentReason.trim() || null
    }
    if (assignmentForm.scopeMode !== DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_SELECTED) {
      delete payload.fileIds
    }
    await createProjectCodeAssignment(projectCodeId, payload)
    message.success('分配修正任务已创建')
    assignmentDialogVisible.value = false
    await loadAssignmentRecords()
  } finally {
    assignmentSubmitting.value = false
  }
}

const goAssignmentAudit = (row: DccProjectCodeAssignmentRespVO) => {
  assignmentRecordsVisible.value = false
  router.push({
    path: '/dcc/controlled-file/logs',
    query: {
      logType: 'PROJECT_CODE_CHANGE',
      assignmentId: row.id,
      projectCodeId: row.projectCodeId
    }
  })
}

const loadAssignmentRecords = async () => {
  const projectCodeId = selectedProjectCode.value?.id
  if (!projectCodeId) {
    assignmentRecords.value = []
    assignmentRecordsTotal.value = 0
    return
  }
  assignmentRecordsLoading.value = true
  try {
    const data = await getProjectCodeAssignmentPage(projectCodeId, assignmentRecordQuery)
    assignmentRecords.value = data.list
    assignmentRecordsTotal.value = data.total
  } finally {
    assignmentRecordsLoading.value = false
  }
}

const openAssignmentRecords = async () => {
  assignmentRecordsVisible.value = true
  assignmentRecordQuery.pageNo = 1
  await loadAssignmentRecords()
}

const handleRevokeAssignment = async (row: DccProjectCodeAssignmentRespVO) => {
  try {
    const { value } = await message.prompt('请输入撤回原因', '撤回分配任务')
    const revokeReason = String(value || '').trim()
    if (!revokeReason) {
      message.warning('撤回原因不能为空')
      return
    }
    await revokeProjectCodeAssignment(row.id, revokeReason)
    message.success('分配任务已撤回')
    await loadAssignmentRecords()
  } catch (error) {
    if (isCancelError(error)) {
      return
    }
    throw error
  }
}

const isCancelError = (error: unknown) => error === 'cancel' || error === 'close'

const stopBatchAiCategoryPolling = () => {
  if (batchAiCategoryPollTimer) {
    clearTimeout(batchAiCategoryPollTimer)
    batchAiCategoryPollTimer = null
  }
}

const isBatchAiCategoryTaskActive = (task: ControlledFileBatchRecognitionTaskRespVO) =>
  task.status === 'WAITING' || task.status === 'RUNNING'

const handleBatchAiCategoryTaskTerminal = async (
  task: ControlledFileBatchRecognitionTaskRespVO,
  notify: boolean
) => {
  if (batchAiCategoryTerminalHandledTaskId === task.taskId) {
    return
  }
  batchAiCategoryTerminalHandledTaskId = task.taskId
  if (notify) {
    const summary =
      `已归类 ${task.successCount} 个，保留未分类 ${task.unclassifiedCount} 个，` +
      `歧义 ${task.ambiguousCount} 个，并发跳过 ${task.conflictCount} 个，失败 ${task.failedCount} 个`
    if (task.status === 'FAILED') {
      message.error(`批量AI分类失败：${task.lastFailureMessage || summary}`)
    } else if (task.failedCount > 0 || task.conflictCount > 0) {
      message.warning(`批量AI分类完成：${summary}`)
    } else {
      message.success(`批量AI分类完成：${summary}`)
    }
  }
  await getList()
  if (detailDrawerVisible.value && selectedProjectCode.value?.id) {
    await getAssociatedFiles()
  }
}

const pollBatchAiCategoryTask = async (taskId: number) => {
  try {
    const task = await getControlledFileBatchRecognitionTask(taskId)
    batchAiCategoryTask.value = task
    if (isBatchAiCategoryTaskActive(task)) {
      batchAiCategoryPollTimer = setTimeout(
        () => void pollBatchAiCategoryTask(taskId),
        BATCH_AI_CATEGORY_POLL_INTERVAL_MS
      )
      return
    }
    stopBatchAiCategoryPolling()
    await handleBatchAiCategoryTaskTerminal(task, true)
  } catch (error) {
    stopBatchAiCategoryPolling()
    message.error(`批量AI分类状态查询失败：${resolveAiCategoryErrorMessage(error)}`)
  }
}

const startBatchAiCategoryPolling = (taskId: number) => {
  stopBatchAiCategoryPolling()
  batchAiCategoryPollTimer = setTimeout(
    () => void pollBatchAiCategoryTask(taskId),
    BATCH_AI_CATEGORY_POLL_INTERVAL_MS
  )
}

const handleViewBatchAiCategoryFailures = async () => {
  const taskId = batchAiCategoryTask.value?.taskId
  if (!taskId) {
    return
  }
  await router.push({
    path: '/dcc/controlled-file/browser',
    query: {
      scope: 'GLOBAL',
      pageNo: '1',
      pageSize: '10',
      recognitionStatus: 'FAILED',
      batchRecognitionTaskId: String(taskId)
    }
  })
}

const handleExportBatchAiCategoryFailures = async () => {
  const taskId = batchAiCategoryTask.value?.taskId
  if (!taskId || batchAiCategoryFailureExporting.value) {
    return
  }
  batchAiCategoryFailureExporting.value = true
  try {
    const data = await exportControlledFileRecognitionRecordExcel({
      pageNo: 1,
      pageSize: 100,
      latestVersionOnly: true,
      recognitionStatus: 'FAILED',
      batchRecognitionTaskId: taskId
    })
    download.excel(data, 'DCC批量AI分类失败明细.xlsx')
  } catch (error) {
    message.error(`导出批量AI分类失败明细失败：${resolveAiCategoryErrorMessage(error)}`)
    throw error
  } finally {
    batchAiCategoryFailureExporting.value = false
  }
}

const handleCloseBatchAiCategoryProgress = () => {
  const taskId = batchAiCategoryTask.value?.taskId
  if (!taskId) {
    return
  }
  batchAiCategoryDismissedTaskId.value = taskId
}

const restoreLatestBatchAiCategoryTask = async () => {
  if (!canRunBatchAiCategory.value) {
    return
  }
  const task = await getLatestControlledFileBatchRecognitionTask('FILE_CATEGORY')
  if (!task) {
    return
  }
  batchAiCategoryTask.value = task
  if (isBatchAiCategoryTaskActive(task)) {
    batchAiCategoryTerminalHandledTaskId = null
    startBatchAiCategoryPolling(task.taskId)
  } else {
    await handleBatchAiCategoryTaskTerminal(task, false)
  }
}

const handleBatchAiCategoryProjectCodes = async () => {
  if (
    !canRunBatchAiCategory.value ||
    batchAiCategoryRunning.value ||
    aiCategoryRunning.value ||
    listUnclassifiedAutoClassifyRunning.value
  ) {
    return
  }
  const task = await createControlledFileBatchRecognitionTask({
    recognitionType: 'FILE_CATEGORY',
    scope: 'GLOBAL',
    overwriteExisting: false,
    existingRecordPolicy: 'RETRY_FAILED',
    syncFileNameTitle: false,
    workerCount: 5
  })
  batchAiCategoryTask.value = task
  batchAiCategoryDismissedTaskId.value = null
  batchAiCategoryTerminalHandledTaskId = null
  if (isBatchAiCategoryTaskActive(task)) {
    startBatchAiCategoryPolling(task.taskId)
    return
  }
  await handleBatchAiCategoryTaskTerminal(task, true)
}

const handleAiCategoryAssociatedFiles = async () => {
  const projectCodeId = selectedProjectCode.value?.id
  if (
    !projectCodeId ||
    aiCategoryRunning.value ||
    batchAiCategoryRunning.value ||
    listUnclassifiedAutoClassifyRunning.value
  ) {
    return
  }
  aiCategoryRunning.value = true
  aiCategoryProcessed.value = 0
  aiCategoryTotal.value = 0
  try {
    const candidates: DccProjectCodeAssociatedFileAiCategoryRespVO[] =
      await getProjectCodeAssociatedFileAiCategoryCandidates(projectCodeId)
    aiCategoryTotal.value = candidates.length
    if (candidates.length === 0) {
      message.info('没有需要分类的未分类标签')
      return
    }
    let matchedCount = 0
    let unclassifiedCount = 0
    let ambiguousCount = 0
    for (const candidate of candidates) {
      try {
        const result = await classifyProjectCodeAssociatedFileByAi(projectCodeId, candidate.fileId)
        if (result.classificationStatus === 'AMBIGUOUS') {
          ambiguousCount += 1
        } else if (result.matched) {
          matchedCount += 1
        } else {
          unclassifiedCount += 1
        }
        aiCategoryProcessed.value += 1
      } catch (error) {
        const failedFileName = candidate.fileName || `ID ${candidate.fileId}`
        message.error(
          `AI分类失败：已处理 ${aiCategoryProcessed.value}/${aiCategoryTotal.value}，失败文件 ${failedFileName}，后端错误：${resolveAiCategoryErrorMessage(error)}`
        )
        await getAssociatedFiles()
        throw error
      }
    }
    message.success(
      `AI分类完成：已归类 ${matchedCount} 个，保留未分类 ${unclassifiedCount} 个，歧义文件 ${ambiguousCount} 个`
    )
    await getAssociatedFiles()
  } finally {
    aiCategoryRunning.value = false
  }
}

const autoClassifyUnclassifiedFilesForProjectCode = async (
  projectCode: DccProjectCodeRespVO,
  targetOptions: DccFileTypeTaxonomyStageTypeOption[]
) => {
  const associatedFiles = await fetchProjectCodeAssociatedFiles(projectCode.id)
  const filesToClassify = associatedFiles.filter(isAssociatedFileUnclassified)
  let processedFileCount = 0
  for (const file of filesToClassify) {
    const target = resolveBestAssociatedAutoClassifyTarget(file, targetOptions)
    if (!target) {
      throw new Error('没有可用于归类的正式文件类型，请先维护 DCC 文件分类树')
    }
    const payload = buildDccAssociatedFileAutoClassifyPayload(file, target, projectCode)
    await updateControlledFileMetadata(file.id, payload)
    processedFileCount += 1
  }
  if (processedFileCount > 0) {
    const refreshedFiles = await fetchProjectCodeAssociatedFiles(projectCode.id)
    const remainingCount = refreshedFiles.filter(isAssociatedFileUnclassified).length
    if (remainingCount > 0) {
      throw new Error(
        `项目代码 ${projectCode.projectCode || projectCode.id} 自动归类后仍有 ${remainingCount} 份文件停留在未分类`
      )
    }
  }
  return processedFileCount
}

const resetListUnclassifiedAutoClassifyProgress = () => {
  listUnclassifiedAutoClassifyTotalProjects.value = 0
  listUnclassifiedAutoClassifyProcessedProjects.value = 0
  listUnclassifiedAutoClassifyProcessedFiles.value = 0
}

const handleListAutoClassifyUnclassifiedProjectCodes = async () => {
  if (
    !canRunProjectCodeListNameAutoClassify.value ||
    listUnclassifiedAutoClassifyRunning.value ||
    aiCategoryRunning.value ||
    batchAiCategoryRunning.value ||
    unclassifiedAutoClassifyRunning.value
  ) {
    return
  }
  resetListUnclassifiedAutoClassifyProgress()
  listUnclassifiedAutoClassifyRunning.value = true
  try {
    await loadFileTypeTaxonomies()
    const targetOptions = associatedAutoClassifyTargetOptions.value
    if (targetOptions.length === 0) {
      message.error('没有可用于归类的正式文件类型，请先维护 DCC 文件分类树')
      return
    }
    const { projectCodes, total } = await fetchAllFilteredProjectCodes()
    listUnclassifiedAutoClassifyTotalProjects.value = total
    if (projectCodes.length === 0) {
      message.info('当前筛选条件下没有项目代码')
      return
    }
    try {
      await message.confirm(
        `将按当前筛选条件处理 ${total} 个全部项目代码，包括未加载分页；系统会按文件名相似度归类每个项目代码下的未分类文件，不会只处理当前页。是否继续？`,
        '按文件名归类未分类'
      )
    } catch (error) {
      if (isCancelError(error)) {
        return
      }
      throw error
    }

    for (const projectCode of projectCodes) {
      const classifiedFileCount = await autoClassifyUnclassifiedFilesForProjectCode(
        projectCode,
        targetOptions
      )
      listUnclassifiedAutoClassifyProcessedFiles.value += classifiedFileCount
      listUnclassifiedAutoClassifyProcessedProjects.value += 1
    }
    await getList()
    if (detailDrawerVisible.value && selectedProjectCode.value?.id) {
      await getAssociatedFiles()
    }
    message.success(
      `已按当前筛选条件处理 ${listUnclassifiedAutoClassifyProcessedProjects.value} 个项目代码，归类 ${listUnclassifiedAutoClassifyProcessedFiles.value} 份未分类文件`
    )
  } catch (error) {
    message.error(
      `列表批量按文件名归类失败：已处理项目 ${listUnclassifiedAutoClassifyProcessedProjects.value}/${listUnclassifiedAutoClassifyTotalProjects.value}，已归类文件 ${listUnclassifiedAutoClassifyProcessedFiles.value} 份，后端错误：${resolveAiCategoryErrorMessage(error)}`
    )
    throw error
  } finally {
    listUnclassifiedAutoClassifyRunning.value = false
  }
}

const handleAutoClassifyUnclassifiedAssociatedFiles = async () => {
  const projectCodeId = selectedProjectCode.value?.id
  if (
    !projectCodeId ||
    unclassifiedAutoClassifyRunning.value ||
    aiCategoryRunning.value ||
    batchAiCategoryRunning.value ||
    listUnclassifiedAutoClassifyRunning.value
  ) {
    return
  }
  const filesToClassify = [...associatedUnclassifiedFiles.value]
  if (filesToClassify.length === 0) {
    message.info('当前产品没有未分类或未分类文件类型文件')
    return
  }
  const targetOptions = associatedAutoClassifyTargetOptions.value
  if (targetOptions.length === 0) {
    message.error('没有可用于归类的正式文件类型，请先维护 DCC 文件分类树')
    return
  }
  try {
    await message.confirm(
      `将按文件名相似度归类 ${filesToClassify.length} 份未分类文件，完成后不会保留在未分类或未分类文件类型中。是否继续？`,
      '按文件名归类未分类'
    )
  } catch {
    return
  }

  let processedCount = 0
  unclassifiedAutoClassifyRunning.value = true
  try {
    for (const file of filesToClassify) {
      const target = resolveBestAssociatedAutoClassifyTarget(file, targetOptions)
      if (!target) {
        throw new Error('没有可用于归类的正式文件类型，请先维护 DCC 文件分类树')
      }
      const payload = buildDccAssociatedFileAutoClassifyPayload(file, target)
      await updateControlledFileMetadata(file.id, payload)
      processedCount += 1
    }
    await getAssociatedFiles()
    await getList()
    if (associatedUnclassifiedFileCount.value > 0) {
      throw new Error(`自动归类完成后仍有 ${associatedUnclassifiedFileCount.value} 份文件停留在未分类，请检查文件元数据`)
    }
    message.success(`已按文件名归类 ${processedCount} 份未分类文件`)
  } catch (error) {
    message.error(
      `自动归类失败：已处理 ${processedCount}/${filesToClassify.length}，后端错误：${resolveAiCategoryErrorMessage(error)}`
    )
    await getAssociatedFiles()
    throw error
  } finally {
    unclassifiedAutoClassifyRunning.value = false
  }
}

const syncDetailFromRoute = async () => {
  const queryProjectCodeId = resolveQueryProjectCodeId()
  if (!queryProjectCodeId) {
    detailRequestSequence += 1
    detailDrawerVisible.value = false
    selectedProjectCode.value = null
    resetAssociatedFilesState()
    detailLoading.value = false
    associatedFilesLoading.value = false
    return
  }
  const id = Number(queryProjectCodeId)
  if (!Number.isFinite(id)) {
    detailRequestSequence += 1
    detailDrawerVisible.value = false
    detailLoading.value = false
    associatedFilesLoading.value = false
    return
  }
  const requestToken = ++detailRequestSequence
  detailDrawerVisible.value = true
  const hasCurrentProjectCode = Number(selectedProjectCode.value?.id) === id
  if (!hasCurrentProjectCode) {
    selectedProjectCode.value = null
    resetAssociatedFilesState()
    detailLoading.value = true
  }
  let projectCodeLoaded = false
  try {
    const projectCode = await getProjectCode(id)
    if (requestToken !== detailRequestSequence) {
      return
    }
    selectedProjectCode.value = projectCode
    projectCodeLoaded = true
  } finally {
    if (requestToken === detailRequestSequence) {
      detailLoading.value = false
      if (projectCodeLoaded) {
        void loadAssociatedFilesForDetail(id, requestToken)
      }
    }
  }
}

const openProjectCodeDetail = async (projectCode: DccProjectCodeRespVO | number | string) => {
  const projectCodeId =
    typeof projectCode === 'object' && projectCode !== null ? projectCode.id : projectCode
  const id = Number(projectCodeId)
  if (!Number.isFinite(id)) {
    return
  }
  if (typeof projectCode === 'object' && projectCode !== null) {
    selectedProjectCode.value = projectCode
    resetAssociatedFilesState()
    detailLoading.value = false
  }
  detailDrawerVisible.value = true
  await router.replace({
    path: '/mdm/project-code',
    query: { ...route.query, projectCodeId: String(id) }
  })
}

const openControlledFileDetail = (row: ControlledFileVO) => {
  openControlledFileViewer(router, route, row.id, 'project-code')
}

const formatStatus = (status: string) => {
  return status === 'ENABLE' ? '启用' : '停用'
}

const formatImportAction = (action: string) => {
  const labels: Record<string, string> = {
    CREATE: '新增',
    UPDATE: '更新',
    DISABLE: '停用',
    UNCHANGED: '不变',
    INVALID: '失败'
  }
  return labels[action] || action
}

const importActionTagType = (action: string) => {
  const types: Record<string, 'success' | 'warning' | 'info' | 'danger' | undefined> = {
    CREATE: 'success',
    UPDATE: 'warning',
    DISABLE: 'info',
    UNCHANGED: undefined,
    INVALID: 'danger'
  }
  return types[action]
}

onMounted(async () => {
  await ensureLoaded()
  await syncDetailFromRoute()
  await restoreLatestBatchAiCategoryTask()
})

onBeforeUnmount(() => {
  stopBatchAiCategoryPolling()
})

watch(
  () => route.query.projectCodeId,
  async () => {
    await syncDetailFromRoute()
  }
)

watch(
  () => [route.query.associatedFocus, route.query.associatedFileId, route.query.fileTypeTaxonomyId],
  () => {
    if (detailDrawerVisible.value) {
      if (!applyAssociatedRouteFocus()) {
        ensureAssociatedSelection()
      }
    }
  }
)
</script>

<style scoped>
.dcc-project-code-governance-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  line-height: 1.2;
}

.dcc-project-code-governance-version {
  color: var(--el-text-color-regular);
  font-size: 12px;
  white-space: normal;
}

.dcc-project-code-import-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
}

.dcc-project-code-import-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.dcc-project-code-batch-ai-category-progress {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  color: #263247;
  font-size: 13px;
  background: #fafcff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.dcc-project-code-batch-ai-category-progress-head {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  align-items: center;
  justify-content: space-between;
  color: #172033;
  font-weight: 600;
}

.dcc-project-code-batch-ai-category-progress-head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.dcc-project-code-batch-ai-category-progress-close {
  color: #4b5563;
}

.dcc-project-code-batch-ai-category-progress-summary {
  color: #4b5563;
  line-height: 1.5;
}

.dcc-project-code-batch-ai-category-progress-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  align-items: center;
}

.dcc-project-code-batch-ai-category-failure-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  align-items: baseline;
  color: #7f1d1d;
  line-height: 1.5;
}

.dcc-project-code-batch-ai-category-failure-summary-label {
  color: #c00000;
  font-weight: 600;
}

.dcc-project-code-batch-ai-category-failure-summary-item {
  overflow-wrap: anywhere;
}

.dcc-project-code-batch-ai-category-progress-consistency {
  color: #c00000;
  font-weight: 600;
  line-height: 1.5;
}

.dcc-project-code-batch-ai-category-progress-interruption {
  color: #c00000;
  font-weight: 600;
  line-height: 1.5;
}

.dcc-project-code-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  overflow-x: hidden;
}

.dcc-project-code-associated-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #172033;
  font-size: 15px;
  font-weight: 600;
}

.dcc-project-code-associated-heading-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dcc-project-code-ai-category-percent {
  color: #606266;
  font-size: 13px;
  font-weight: 500;
}

.dcc-project-code-associated-files {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  min-height: 120px;
}

.dcc-project-code-associated-layout {
  display: grid;
  grid-template-columns: minmax(150px, 0.7fr) minmax(180px, 0.8fr) minmax(0, 1.8fr);
  gap: 12px;
  align-items: stretch;
}

.dcc-project-code-associated-panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.dcc-project-code-associated-file-table {
  overflow-x: auto;
}

.dcc-project-code-associated-file-table :deep(.el-table) {
  min-width: 1030px;
}

.dcc-project-code-associated-file-pagination {
  padding: 10px 12px 12px;
  border-top: 1px solid #edf1f6;
}

.dcc-project-code-associated-panel-title {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  padding: 8px 12px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  background: #f7f9fc;
  border-bottom: 1px solid #edf1f6;
}

.dcc-project-code-associated-stage-list,
.dcc-project-code-associated-type-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 420px;
  padding: 10px;
  overflow-y: auto;
}

.dcc-project-code-associated-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 42px;
  gap: 10px;
  padding: 8px 10px;
  color: #263247;
  font-size: 13px;
  line-height: 1.35;
  text-align: left;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  transition:
    color 0.18s ease,
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.dcc-project-code-associated-list-item:hover {
  color: #1677ff;
  background: #fafcff;
  border-color: #b8d4ff;
}

.dcc-project-code-associated-list-item.is-active {
  color: #1677ff;
  background: #eef6ff;
  border-color: #1677ff;
}

.dcc-project-code-associated-file-table :deep(.is-associated-route-focus > td) {
  background: #f0f9eb !important;
}

.dcc-project-code-associated-item-label {
  min-width: 0;
  overflow: hidden;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dcc-project-code-associated-file-table :deep(.el-table__header th) {
  background: #f7f9fc;
}

@media (max-width: 1240px) {
  .dcc-project-code-associated-layout {
    grid-template-columns: minmax(150px, 0.7fr) minmax(180px, 0.8fr) minmax(0, 1.8fr);
  }
}

@media (max-width: 960px) {
  .dcc-project-code-associated-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
