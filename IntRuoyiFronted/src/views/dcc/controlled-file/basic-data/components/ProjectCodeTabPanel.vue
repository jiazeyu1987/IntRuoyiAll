<template>
  <ContentWrap>
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
          type="primary"
          plain
          :disabled="batchAiCategoryRunning"
          @click="openForm('create')"
          v-hasPermi="['dcc:project-code:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增项目代码
        </el-button>
        <el-button
          :disabled="batchAiCategoryRunning"
          @click="openImportDialog"
          v-hasPermi="['dcc:project-code:import']"
        >
          <Icon icon="ep:upload" class="mr-5px" />
          导入
        </el-button>
        <el-button
          :loading="exportLoading"
          :disabled="batchAiCategoryRunning"
          @click="handleExport"
          v-hasPermi="['dcc:project-code:export']"
        >
          <Icon icon="ep:download" class="mr-5px" />
          导出
        </el-button>
        <el-button
          v-if="canRunBatchAiCategory"
          type="primary"
          plain
          data-testid="dcc-project-code-batch-ai-category"
          :loading="batchAiCategoryRunning"
          :disabled="loading || exportLoading || previewLoading || confirmLoading || aiCategoryRunning"
          @click="handleBatchAiCategoryProjectCodes"
        >
          <Icon icon="ep:magic-stick" class="mr-5px" />
          批量AI分类
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
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
                class="dcc-project-code-batch-ai-category-progress-close"
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
              type="danger"
              data-testid="dcc-project-code-batch-ai-category-view-failures"
              @click="handleViewBatchAiCategoryFailures"
            >
              查看失败文件
            </el-button>
            <el-button
              link
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
              <el-tag
                effect="plain"
                :type="resolveDccProjectGovernanceTagType(getDccProjectGovernance(row.projectName)?.routeStatus)"
                :title="getDccProjectGovernance(row.projectName)?.routeCodes?.join('、') || ''"
              >
                {{ formatDccProjectGovernanceStatus(getDccProjectGovernance(row.projectName)?.routeStatus) }}
              </el-tag>
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
              <el-tag
                effect="plain"
                :type="resolveDccProjectGovernanceTagType(getDccProjectGovernance(row.projectName)?.mainBatchRecordStatus)"
                :title="getDccProjectGovernance(row.projectName)?.mainBatchRecordVersionNos?.join('、') || ''"
              >
                {{ formatDccProjectGovernanceStatus(getDccProjectGovernance(row.projectName)?.mainBatchRecordStatus) }}
              </el-tag>
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
              <el-tag
                effect="plain"
                :type="resolveDccProjectGovernanceTagType(getDccProjectGovernance(row.projectName)?.lossReportStatus)"
                :title="getDccProjectGovernance(row.projectName)?.lossReportCodes?.join('、') || ''"
              >
                {{ formatDccProjectGovernanceStatus(getDccProjectGovernance(row.projectName)?.lossReportStatus) }}
              </el-tag>
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
              <el-tag
                effect="plain"
                :type="resolveDccProjectGovernanceTagType(getDccProjectGovernance(row.projectName)?.processInspectionStatus)"
                :title="getDccProjectGovernance(row.projectName)?.processInspectionCodes?.join('、') || ''"
              >
                {{ formatDccProjectGovernanceStatus(getDccProjectGovernance(row.projectName)?.processInspectionStatus) }}
              </el-tag>
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
              <el-tag
                effect="plain"
                :type="resolveDccProjectGovernanceTagType(getDccProjectGovernance(row.projectName)?.parameterRecordStatus)"
                :title="getDccProjectGovernance(row.projectName)?.parameterRecordCodes?.join('、') || ''"
              >
                {{ formatDccProjectGovernanceStatus(getDccProjectGovernance(row.projectName)?.parameterRecordStatus) }}
              </el-tag>
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
                type="primary"
                @click="openForm('update', row)"
                v-hasPermi="['dcc:project-code:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(row)"
                v-hasPermi="['dcc:project-code:delete']"
              >
                删除
              </el-button>
              <el-button link type="primary" @click="openProjectCodeDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <el-dialog
    v-model="importVisible"
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
        <el-button>
          <Icon icon="ep:folder-opened" class="mr-5px" />
          选择文件
        </el-button>
      </el-upload>
      <el-button @click="handleDownloadTemplate">
        <Icon icon="ep:document" class="mr-5px" />
        模板
      </el-button>
      <el-button type="primary" :loading="previewLoading" @click="handleImportPreview">
        <Icon icon="ep:view" class="mr-5px" />
        预览
      </el-button>
      <el-button
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
      <el-tag>总数 {{ previewResult.totalCount }}</el-tag>
      <el-tag type="success">新增 {{ previewResult.createCount }}</el-tag>
      <el-tag type="warning">更新 {{ previewResult.updateCount }}</el-tag>
      <el-tag type="info">停用 {{ previewResult.disableCount }}</el-tag>
      <el-tag>不变 {{ previewResult.unchangedCount }}</el-tag>
      <el-tag :type="previewResult.failureCount > 0 ? 'danger' : 'success'">
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
          <el-tag :type="importActionTagType(row.importAction)">
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

  <Dialog v-model="formVisible" title="项目代码维护" width="760px">
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
      <el-button type="primary" :disabled="formLoading" @click="submitForm">确定</el-button>
      <el-button @click="formVisible = false">取消</el-button>
    </template>
  </Dialog>

  <el-drawer
    v-model="detailDrawerVisible"
    title="DCC基础条目"
    size="96%"
    data-testid="dcc-project-code-detail-drawer"
  >
    <div v-loading="detailLoading" class="dcc-project-code-detail">
      <el-descriptions v-if="selectedProjectCode" :column="2" border>
        <el-descriptions-item label="文控">{{ selectedProjectCode.docControlNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedProjectCode.status === 'ENABLE' ? 'success' : 'info'">
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
            data-testid="dcc-project-code-ai-category"
            size="small"
            type="primary"
            :loading="aiCategoryRunning"
            :disabled="!selectedProjectCode?.id || associatedFilesLoading || batchAiCategoryRunning"
            @click="handleAiCategoryAssociatedFiles"
          >
            AI分类
          </el-button>
          <el-button
            data-testid="dcc-project-code-assignment-open"
            size="small"
            type="primary"
            plain
            :disabled="!selectedProjectCode?.id || associatedFilesTotal === 0"
            @click="openAssignmentDialog"
            v-hasPermi="['dcc:project-code-assignment:assign']"
          >
            分配修正
          </el-button>
          <el-button
            data-testid="dcc-project-code-assignment-records"
            size="small"
            plain
            @click="openAssignmentRecords"
            v-hasPermi="['dcc:project-code-assignment:query']"
          >
            分配记录
          </el-button>
          <el-tag size="small" type="info">共 {{ associatedFilesTotal }} 份</el-tag>
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
                  <el-tag size="small" type="info">{{ stage.count }} 份</el-tag>
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
                  <el-tag size="small" type="info">{{ typeGroup.files.length }} 份</el-tag>
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
                <el-tag size="small" type="info">
                  {{ selectedAssociatedFilesTotal }} 份
                </el-tag>
              </div>
              <el-table
                :data="selectedAssociatedPagedFiles"
                :show-overflow-tooltip="true"
                @selection-change="handleAssociatedFileSelectionChange"
              >
                <el-table-column type="selection" width="48" />
                <el-table-column label="文件名称" prop="fileName" min-width="360">
                  <template #default="{ row }">
                    <el-link type="primary" @click="openControlledFileDetail(row)">
                      {{ row.fileName || row.title || '-' }}
                    </el-link>
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

  <Dialog v-model="assignmentDialogVisible" title="分配修正任务" width="620px">
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
            将按当前勾选的 {{ selectedAssociatedFileIds.length }} 份文件生成快照。
          </template>
          <template v-else>
            将按后端当前有效项目代码口径生成 {{ associatedFilesTotal }} 份文件快照。
          </template>
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
      <el-button @click="assignmentDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="assignmentSubmitting" @click="submitAssignmentDialog">
        创建分配
      </el-button>
    </template>
  </Dialog>

  <el-drawer
    v-model="assignmentRecordsVisible"
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
            type="primary"
            @click="goAssignmentAudit(row)"
            v-hasPermi="['dcc:project-code-assignment:audit:query']"
          >
            追溯
          </el-button>
          <el-button
            link
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
  type ControlledFileBatchRecognitionTaskRespVO,
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
  DccProjectCodeUpdateReqVO
} from '@/api/dcc/controlledFile/projectCodes'
import {
  classifyProjectCodeAssociatedFileByAi,
  createProjectCode,
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
import { formatControlledFileDateTime } from '../../detail/presentation'
import { openControlledFileViewer } from '../../shared/viewer-navigation'
import {
  DCC_UNCLASSIFIED_TAXONOMY_STAGE,
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
  getProjectCodeAssignmentPage,
  revokeProjectCodeAssignment,
  type DccProjectCodeAssignmentCreateReqVO,
  type DccProjectCodeAssignmentRespVO
} from '@/api/dcc/controlledFile/projectCodeAssignments'

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

const DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE = 200
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
  { key: 'routeStatus', label: '工艺路线', width: 120 },
  { key: 'mainBatchRecordStatus', label: '主批记录', width: 120 },
  { key: 'lossReportStatus', label: '损耗单', width: 110 },
  { key: 'processInspectionStatus', label: '过程检验单', width: 130 },
  { key: 'parameterRecordStatus', label: '参数记录表', width: 130 },
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
const selectedProjectCode = ref<DccProjectCodeRespVO | null>(null)
const associatedNavigationFiles = ref<ControlledFileVO[]>([])
const associatedFilesTotal = ref(0)
const assignmentDialogVisible = ref(false)
const assignmentUsersLoading = ref(false)
const assignmentSubmitting = ref(false)
const assignmentUsers = ref<AssignmentUserOption[]>([])
const assignmentRecordsVisible = ref(false)
const assignmentRecordsLoading = ref(false)
const assignmentRecords = ref<DccProjectCodeAssignmentRespVO[]>([])
const assignmentRecordsTotal = ref(0)
const selectedAssociatedFileIds = ref<Array<number | string>>([])
const selectedAssociatedStageKey = ref('')
const selectedAssociatedTypeKey = ref('')
const aiCategoryRunning = ref(false)
const aiCategoryProcessed = ref(0)
const aiCategoryTotal = ref(0)
const batchAiCategoryTask = ref<ControlledFileBatchRecognitionTaskRespVO | null>(null)
const batchAiCategoryDismissedTaskId = ref<number | null>(null)
const batchAiCategoryFailureExporting = ref(false)
let detailRequestSequence = 0
let batchAiCategoryPollTimer: ReturnType<typeof setTimeout> | null = null
let batchAiCategoryTerminalHandledTaskId: number | null = null
const importFileList = ref<any[]>([])
const importFile = ref<File | null>(null)
const previewResult = ref<DccProjectCodeImportPreviewRespVO | null>(null)
const importRows = computed<DccProjectCodeImportRowRespVO[]>(() => previewResult.value?.rows || [])
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
const aiCategoryProgressPercent = computed(() =>
  aiCategoryTotal.value === 0 ? 0 : Math.floor((aiCategoryProcessed.value * 100) / aiCategoryTotal.value)
)
const canRunAiCategory = computed(
  () =>
    checkPermi(['dcc:project-code:update']) &&
    checkPermi(['dcc:controlled-file:update'])
)
const canRunBatchAiCategory = computed(
  () => canRunAiCategory.value && checkRole(['doc_control'])
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
const formData = ref<DccProjectCodeUpdateReqVO>({
  id: 0,
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

const resetFormData = () => {
  formData.value = {
    id: 0,
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

const loadFileTypeTaxonomies = async () => {
  fileTypeTaxonomies.value = await getFileTypeTaxonomyList()
}

const getList = async () => {
  loading.value = true
  try {
    const data = await getProjectCodePage(queryParams)
    list.value = data.list
    total.value = data.total
    await loadDccProjectGovernanceStatus(data.list)
  } finally {
    loading.value = false
  }
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
    ensureAssociatedSelection()
  } finally {
    if (canApplyDetailResult()) {
      associatedFilesLoading.value = false
    }
  }
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
}

const handleAssociatedFileSelectionChange = (rows: ControlledFileVO[]) => {
  selectedAssociatedFileIds.value = rows
    .map((row) => row.id as number | string)
    .filter((id): id is number | string => id !== null && typeof id !== 'undefined' && String(id).length > 0)
}

const openAssignmentDialog = async () => {
  if (!selectedProjectCode.value?.id) {
    return
  }
  if (associatedFilesTotal.value <= 0) {
    message.error('当前项目代码没有可分配文件')
    return
  }
  resetAssignmentForm()
  assignmentDialogVisible.value = true
  await loadAssignmentUsers()
}

const submitAssignmentDialog = async () => {
  const projectCodeId = selectedProjectCode.value?.id
  if (!projectCodeId || !assignmentForm.assigneeUserId) {
    message.error('请选择被分配人')
    return
  }
  if (
    assignmentForm.scopeMode === DCC_PROJECT_CODE_ASSIGNMENT_SCOPE_SELECTED &&
    selectedAssociatedFileIds.value.length === 0
  ) {
    message.error('请选择需要分配的文件')
    return
  }
  assignmentSubmitting.value = true
  try {
    const payload: DccProjectCodeAssignmentCreateReqVO = {
      assigneeUserId: assignmentForm.assigneeUserId,
      scopeMode: assignmentForm.scopeMode,
      fileIds: selectedAssociatedFileIds.value,
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
  if (!canRunBatchAiCategory.value || batchAiCategoryRunning.value || aiCategoryRunning.value) {
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
  if (!projectCodeId || aiCategoryRunning.value || batchAiCategoryRunning.value) {
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
</script>

<style scoped>
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
