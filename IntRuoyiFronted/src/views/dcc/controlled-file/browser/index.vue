<template>
  <el-row :gutter="16" class="browser-page-layout">
    <el-col :span="6">
      <ContentWrap class="browser-directory-wrap">
        <div class="mb-12px flex items-center justify-between gap-8px">
          <el-input
            v-model="directoryKeyword"
            class="flex-1"
            clearable
            placeholder="搜索目录"
          >
            <template #prefix>
              <Icon icon="ep:search" />
            </template>
          </el-input>
          <el-button :loading="directoryLoading" @click="refreshDirectories">
            <Icon icon="ep:refresh-right" class="mr-5px" />
            刷新
          </el-button>
        </div>

        <div class="browser-directory-scroll">
          <div v-if="directoryKeyword.trim()" class="browser-directory-search">
            <div v-loading="directorySearchLoading" class="browser-directory-search__list">
              <el-empty
                v-if="!directorySearchLoading && !directorySearchResults.length"
                description="未找到匹配目录"
              />
              <button
                v-for="item in directorySearchResults"
                :key="item.id"
                class="browser-directory-search__item"
                type="button"
                @click="handleDirectorySearchSelect(item)"
              >
                <span class="browser-directory-search__name">{{ item.name }}</span>
                <span class="browser-directory-search__path">
                  {{ item.directoryPath || item.name }}
                </span>
              </button>
            </div>
          </div>

          <el-tree
            ref="directoryTreeRef"
            v-loading="directoryLoading"
            :data="directories"
            :props="directoryTreeProps"
            empty-text="暂无可见目录"
            :expand-on-click-node="false"
            highlight-current
            node-key="id"
            @node-click="handleDirectoryClick"
            @node-expand="handleDirectoryNodeExpand"
            @node-collapse="handleDirectoryNodeCollapse"
          >
            <template #default="{ data }">
              <div class="browser-directory-node">
                <span class="browser-directory-node__name">{{ data.name }}</span>
              </div>
            </template>
          </el-tree>
        </div>
      </ContentWrap>
    </el-col>

    <el-col :span="18">
      <ContentWrap class="browser-list-wrap">
        <div
          class="browser-filter-summary"
          data-testid="dcc-controlled-browser-filter-summary"
        >
          <div class="browser-filter-summary__header">
            <span>当前筛选条件</span>
            <el-tag type="success" effect="plain">普通受控浏览默认仅展示当前有效版</el-tag>
          </div>
          <div class="browser-filter-summary__items">
            <div
              v-for="item in browserFilterSummaryItems"
              :key="item.label"
              class="browser-filter-summary__item"
            >
              <span class="browser-filter-summary__label">{{ item.label }}</span>
              <span class="browser-filter-summary__value">{{ item.value }}</span>
            </div>
          </div>
          <div class="browser-filter-summary__hint">
            草稿/历史失效版不会在受控浏览默认入口展示；如需核对历史或签核证据，请使用“查看版本追溯”。
          </div>
        </div>
        <UnifiedListTemplate
          class="browser-list-template"
          :table-key="DCC_BROWSER_COLUMN_TABLE_KEY"
          :query-model="queryParams"
          label-width="68px"
          :filter-definitions="dccBrowserQuickFilterDefinitions"
          :quick-filter-state="dccBrowserQuickFilter.state"
          :selected-filter-definition="dccBrowserQuickFilter.selectedDefinition.value"
          :operator-options="dccBrowserQuickFilter.operatorOptions.value"
          :columns="dccBrowserColumns"
          :column-saving="dccBrowserColumnSaving"
          :show-column-reset="false"
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @update:quick-filter-state="dccBrowserQuickFilter.updateState"
          @quick-filter-query="dccBrowserQuickFilter.applyQuickFilter"
          @column-change="saveDccBrowserColumnConfig"
          @column-reset="resetDccBrowserColumnConfig"
          @pagination="handlePagination"
        >
          <template #extra-filters>
          <el-form-item class="browser-search-scope-filter" label-width="0">
            <el-segmented
              v-model="searchScope"
              :options="browserSearchScopeOptions"
              @change="handleSearchScopeChange"
            />
          </el-form-item>
          </template>
          <template #actions>
            <el-form-item v-if="canEditMetadata">
              <el-popover
                trigger="click"
                v-model:visible="advancedActionsVisible"
                placement="bottom-end"
                width="360"
                popper-class="browser-advanced-actions-popover"
              >
                <div class="browser-advanced-actions">
                  <el-button
                    v-if="canEditMetadata"
                    plain
                    type="primary"
                    :loading="metadataExporting"
                    @click="handleAdvancedAction(handleMetadataExport)"
                  >
                    <Icon icon="ep:download" class="mr-5px" />
                    导出名编
                  </el-button>
                  <el-button
                    v-if="canEditMetadata"
                    plain
                    @click="handleAdvancedAction(openMetadataImportDialog)"
                  >
                    <Icon icon="ep:upload" class="mr-5px" />
                    导入名编
                  </el-button>
                  <el-button
                    v-if="canEditMetadata"
                    plain
                    type="primary"
                    data-testid="dcc-browser-recognition-record-export"
                    :loading="recognitionRecordExporting"
                    @click="handleAdvancedAction(handleRecognitionRecordExport)"
                  >
                    <Icon icon="ep:download" class="mr-5px" />
                    导出记录
                  </el-button>
                  <el-button
                    v-if="canEditMetadata"
                    plain
                    type="primary"
                    data-testid="dcc-browser-recognition-migration-export"
                    :loading="recognitionMigrationExporting"
                    @click="handleAdvancedAction(handleRecognitionMigrationExport)"
                  >
                    <Icon icon="ep:download" class="mr-5px" />
                    导出迁移
                  </el-button>
                  <el-button
                    v-if="canEditMetadata"
                    plain
                    data-testid="dcc-browser-recognition-migration-import"
                    @click="handleAdvancedAction(openRecognitionMigrationImportDialog)"
                  >
                    <Icon icon="ep:upload" class="mr-5px" />
                    导入迁移
                  </el-button>
                  <el-button
                    v-if="canEditMetadata"
                    plain
                    type="primary"
                    data-testid="dcc-browser-batch-recognition-trigger"
                    :loading="batchRecognitionCreating"
                    @click="handleAdvancedAction(openBatchRecognitionDialog)"
                  >
                    <Icon icon="ep:operation" class="mr-5px" />
                    批量识别
                  </el-button>
                  <el-button
                    plain
                    data-testid="dcc-browser-extension-blacklist-open"
                    @click="handleAdvancedAction(openExtensionBlacklistDialog)"
                  >
                    <Icon icon="ep:hide" class="mr-5px" />
                    后缀黑名单
                  </el-button>
                  <el-button
                    v-if="canEditMetadata"
                    plain
                    type="primary"
                    data-testid="dcc-browser-file-number-recognition-trigger"
                    :loading="batchRecognitionCreating"
                    @click="handleAdvancedAction(openFileNumberRecognitionDialog)"
                  >
                    <Icon icon="ep:operation" class="mr-5px" />
                    识别编号
                  </el-button>
                </div>
                <template #reference>
                  <el-button type="primary" plain>
                    <Icon icon="ep:more-filled" class="mr-5px" />
                    高级
                  </el-button>
                </template>
              </el-popover>
            </el-form-item>
          </template>

          <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          border
          data-user-table-column-explicit
          :data-user-table-key="DCC_BROWSER_COLUMN_TABLE_KEY"
          :data="list"
          :empty-text="tableEmptyText"
          height="100%"
          @header-dragend="handleDccBrowserHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <template #empty>
            <div class="browser-permission-empty-state" data-testid="dcc-browser-permission-empty-state">
              <div class="browser-permission-empty-state__title">{{ tableEmptyText }}</div>
              <div class="browser-permission-empty-state__description">{{ tableEmptyHint }}</div>
            </div>
          </template>
          <el-table-column
            v-if="isDccBrowserColumnVisible('fileName')"
            label="文件名称"
            prop="fileName"
            :min-width="getDccBrowserColumnMinWidthString('fileName', 280)"
            v-bind="sortColumnAttrs('fileName')"
          >
            <template #default="{ row }">
              <el-tooltip
                :content="getBrowserFileNameTooltip(row)"
                placement="top"
                :show-after="300"
              >
                <span class="browser-file-name-wrapper">
                  <el-button
                    v-if="getBrowserRowActionState(getSelectedVersion(row)).canPreview"
                    class="browser-file-name browser-file-name--link"
                    link
                    type="primary"
                    @click="openPreview(getSelectedVersion(row).id)"
                  >
                    <span class="browser-file-name__text">{{ getBrowserFileDisplayName(row) }}</span>
                  </el-button>
                  <span v-else class="browser-file-name">
                    <span class="browser-file-name__text">{{ getBrowserFileDisplayName(row) }}</span>
                  </span>
                </span>
              </el-tooltip>
              <div
                v-for="metadata in [getBrowserCurrentActiveRowSummary(row)]"
                :key="`${metadata.versionNo}-${metadata.directoryPath}`"
                class="browser-current-active-row-summary"
                data-testid="dcc-browser-current-active-row-summary"
              >
                <el-tag size="small" type="success" effect="dark">当前有效版</el-tag>
                <span>版本号：{{ metadata.versionNo }}</span>
                <span>目录路径：{{ metadata.directoryPath }}</span>
                <span>发布文件：{{ metadata.publishedFileStatus }}</span>
                <span>盖章文件：{{ metadata.stampedFileStatus }}</span>
                <span>{{ metadata.currentVersionSource }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isDccBrowserColumnVisible('fileNumber')"
            label="文件编号"
            prop="fileNumber"
            :min-width="getDccBrowserColumnMinWidthString('fileNumber', 160)"
            v-bind="sortColumnAttrs('fileNumber')"
          >
            <template #default="{ row }">
              <div
                v-if="getSelectedVersion(row).fileNumber"
                class="browser-file-number-cell"
              >
                <el-tooltip :content="`查看版本追溯：${getSelectedVersion(row).fileNumber}`" placement="top">
                  <el-button
                    class="browser-file-number browser-file-number--link"
                    data-testid="dcc-browser-file-number-detail-link"
                    link
                    type="primary"
                    @click="openDetail(getSelectedVersion(row).id)"
                  >
                    {{ getSelectedVersion(row).fileNumber }}
                  </el-button>
                </el-tooltip>
                <el-tooltip content="复制文件编号" placement="top">
                  <el-button
                    class="browser-file-number-copy"
                    data-testid="dcc-browser-file-number-copy"
                    link
                    type="primary"
                    @click.stop="copyFileNumber(getSelectedVersion(row).fileNumber)"
                  >
                    <Icon icon="ep:copy-document" />
                  </el-button>
                </el-tooltip>
              </div>
              <span v-else class="browser-file-number">-</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isDccBrowserColumnVisible('directory')"
            label="所在目录"
            prop="directory"
            :min-width="getDccBrowserColumnMinWidthString('directory', 220)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('directory')"
          >
            <template #default="{ row }">
              {{ getBrowserDirectoryPath(row.directoryId) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isDccBrowserColumnVisible('productName')"
            label="产品名称"
            prop="productName"
            :min-width="getDccBrowserColumnMinWidthString('productName', 150)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('productName')"
          >
            <template #default="{ row }">
              {{ row.productName || '-' }}
            </template>
          </el-table-column>
          <el-table-column v-if="isDccBrowserColumnVisible('category')" label="类别" prop="category" :min-width="getDccBrowserColumnMinWidthString('category', 160)" v-bind="sortColumnAttrs('category')">
            <template #default="{ row }">
              {{ categoryNameMap.get(row.categoryId) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="版本摘要" v-if="isDccBrowserColumnVisible('versionSummary')" prop="versionSummary" :min-width="getDccBrowserColumnMinWidthString('versionSummary', 310)" v-bind="sortColumnAttrs('versionSummary')">
            <template #default="{ row }">
              <div
                v-for="summary in [
                  getBrowserVersionSummary(
                    getSelectedVersion(row),
                    isLatestVersionSelected(row),
                    isSelectedVersionModifying(row)
                  )
                ]"
                :key="`${summary.versionText}-${summary.statusLabel}`"
                class="browser-version-summary"
                data-testid="dcc-browser-version-summary"
              >
                <div class="browser-version-summary__main">
                  <el-select
                    v-model="row.selectedVersionId"
                    class="browser-version-summary__select"
                    placeholder="选择版本"
                    @change="handleVersionChange(row)"
                  >
                    <el-option
                      v-for="item in getVersionOptions(row)"
                      :key="item.id"
                      :label="item.versionNo"
                      :value="item.id"
                    />
                  </el-select>
                  <div class="browser-version-summary__tags">
                    <el-tag :type="summary.statusTagType">{{ summary.statusLabel }}</el-tag>
                    <el-tag :type="summary.versionKindTagType">
                      {{ summary.versionKindText }}
                    </el-tag>
                    <el-tag v-if="summary.isCurrentActiveVersion" type="success" effect="dark">
                      当前有效版 / ACTIVE / {{ summary.versionText }}
                    </el-tag>
                    <el-tag v-if="summary.modifying" type="warning">修改中</el-tag>
                  </div>
                </div>
                <div class="browser-version-summary__dates">
                  <span>{{ summary.effectiveText }}</span>
                  <span>{{ summary.publishedText }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="备注" v-if="isDccBrowserColumnVisible('remark')" prop="remark" :min-width="getDccBrowserColumnMinWidthString('remark', 220)" show-overflow-tooltip v-bind="sortColumnAttrs('remark')">
            <template #default="{ row }">
              {{ getSelectedVersion(row).remark || '-' }}
            </template>
          </el-table-column>
          <el-table-column v-if="isDccBrowserColumnVisible('operation')" label="操作" prop="operation" align="center" fixed="right" :width="getDccBrowserColumnWidthString('operation', 320)">
            <template #default="{ row }">
              <div class="browser-row-actions">
                <el-button
                  v-if="getBrowserRowActionState(getSelectedVersion(row)).canPreview"
                  link
                  type="primary"
                  @click="openPreview(getSelectedVersion(row).id)"
                >
                  预览当前有效版
                </el-button>
                <el-button
                  v-if="getSelectedVersion(row).id"
                  link
                  type="primary"
                  @click="openDetail(getSelectedVersion(row).id)"
                >
                  查看版本追溯
                </el-button>
                <el-button
                  v-if="getSelectedVersion(row).id"
                  link
                  type="primary"
                  @click="openSignatureEvidence(getSelectedVersion(row).id)"
                >
                  查看签核证据
                </el-button>
                <el-button
                  v-if="getBrowserRowActionState(getSelectedVersion(row)).canDownload"
                  link
                  type="primary"
                  :loading="downloadLoadingId === getSelectedVersion(row).id"
                  @click="openDownload(getSelectedVersion(row).id)"
                >
                  下载
                </el-button>
                <el-button
                  v-if="getBrowserRowActionState(getSelectedVersion(row)).canPrint"
                  v-hasPermi="['dcc:controlled-file:print']"
                  link
                  type="primary"
                  @click="openControlledPrintDialog(getSelectedVersion(row))"
                >
                  受控打印
                </el-button>
                <el-dropdown
                  v-if="hasBrowserMoreActions(row)"
                  trigger="click"
                  @command="(command) => handleBrowserRowCommand(command, row)"
                >
                  <el-button link type="primary">
                    更多
                    <Icon icon="ep:arrow-down" class="ml-4px" />
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="metadata">修改基础信息</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <span
                  v-if="!hasBrowserRowActions(row)"
                  class="browser-row-actions__empty"
                  :title="getBrowserRowActionBlockReason(row)"
                >
                  {{ getBrowserRowActionBlockReason(row) || '暂无可用操作' }}
                </span>
              </div>
            </template>
          </el-table-column>
        </el-table>
          </template>
        </UnifiedListTemplate>
      </ContentWrap>
    </el-col>
  </el-row>

  <el-dialog
    v-model="metadataImportVisible"
    title="导入文件名/文件编号"
    width="980px"
    destroy-on-close
  >
    <div class="metadata-import-toolbar">
      <el-upload
        v-model:file-list="metadataImportFileList"
        accept=".xlsx,.xls"
        :auto-upload="false"
        :limit="1"
        :on-change="handleMetadataImportFileChange"
        :on-remove="handleMetadataImportFileRemove"
      >
        <el-button>
          <Icon icon="ep:folder-opened" class="mr-5px" />
          选择文件
        </el-button>
      </el-upload>
      <el-button :loading="metadataTemplateDownloading" @click="handleMetadataTemplateDownload">
        <Icon icon="ep:document" class="mr-5px" />
        模板
      </el-button>
      <el-button
        type="primary"
        :loading="metadataImportPreviewLoading"
        @click="handleMetadataImportPreview"
      >
        <Icon icon="ep:view" class="mr-5px" />
        预览
      </el-button>
      <el-button
        type="success"
        :disabled="!metadataImportPreview || metadataImportPreview.failureCount > 0"
        :loading="metadataImportConfirmLoading"
        @click="handleMetadataImportConfirm"
      >
        <Icon icon="ep:circle-check" class="mr-5px" />
        确认导入
      </el-button>
    </div>

    <div v-if="metadataImportPreview" class="metadata-import-summary">
      <el-tag>总数 {{ metadataImportPreview.totalCount }}</el-tag>
      <el-tag type="warning">更新 {{ metadataImportPreview.updateCount }}</el-tag>
      <el-tag>不变 {{ metadataImportPreview.unchangedCount }}</el-tag>
      <el-tag :type="metadataImportPreview.failureCount > 0 ? 'danger' : 'success'">
        失败 {{ metadataImportPreview.failureCount }}
      </el-tag>
    </div>

    <el-table
      v-if="metadataImportPreview"
      :data="metadataImportRows"
      :show-overflow-tooltip="true"
      height="420"
    >
      <el-table-column label="行号" prop="rowNo" width="80" />
      <el-table-column label="受控文件ID" prop="controlledFileId" width="120" />
      <el-table-column label="动作" prop="importAction" width="110">
        <template #default="{ row }">
          <el-tag :type="metadataImportActionTagType(row.importAction)">
            {{ formatMetadataImportAction(row.importAction) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件名称" prop="fileName" min-width="220" />
      <el-table-column label="文件编号" prop="fileNumber" min-width="180" />
      <el-table-column label="失败原因" prop="failureReason" min-width="260">
        <template #default="{ row }">{{ row.failureReason || '-' }}</template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog
    v-model="recognitionMigrationImportVisible"
    title="导入识别迁移包"
    width="1180px"
    destroy-on-close
  >
    <div class="metadata-import-toolbar">
      <el-upload
        v-model:file-list="recognitionMigrationImportFileList"
        accept=".xlsx,.xls"
        :auto-upload="false"
        :limit="1"
        :on-change="handleRecognitionMigrationImportFileChange"
        :on-remove="handleRecognitionMigrationImportFileRemove"
      >
        <el-button>
          <Icon icon="ep:folder-opened" class="mr-5px" />
          选择迁移包
        </el-button>
      </el-upload>
      <el-button
        type="primary"
        data-testid="dcc-browser-recognition-migration-preview"
        :loading="recognitionMigrationImportPreviewLoading"
        @click="handleRecognitionMigrationImportPreview"
      >
        <Icon icon="ep:view" class="mr-5px" />
        预览
      </el-button>
      <el-button
        type="success"
        data-testid="dcc-browser-recognition-migration-confirm"
        :disabled="!recognitionMigrationImportPreview || recognitionMigrationImportPreview.applicableCount <= 0"
        :loading="recognitionMigrationImportConfirmLoading"
        @click="handleRecognitionMigrationImportConfirm"
      >
        <Icon icon="ep:circle-check" class="mr-5px" />
        确认导入
      </el-button>
    </div>

    <div v-if="recognitionMigrationImportPreview" class="metadata-import-summary">
      <el-tag>总数 {{ recognitionMigrationImportPreview.totalCount }}</el-tag>
      <el-tag type="success">可应用 {{ recognitionMigrationImportPreview.applicableCount }}</el-tag>
      <el-tag :type="recognitionMigrationImportPreview.blockedCount > 0 ? 'danger' : 'success'">
        不可应用 {{ recognitionMigrationImportPreview.blockedCount }}
      </el-tag>
      <el-tag :type="recognitionMigrationImportPreview.failedRecognitionCount > 0 ? 'danger' : 'success'">
        识别失败 {{ recognitionMigrationImportPreview.failedRecognitionCount }}
      </el-tag>
      <el-tag v-if="recognitionMigrationImportPreview.appliedCount !== undefined" type="success">
        已应用 {{ recognitionMigrationImportPreview.appliedCount || 0 }}
      </el-tag>
    </div>

    <el-table
      v-if="recognitionMigrationImportPreview"
      :data="recognitionMigrationImportRows"
      :show-overflow-tooltip="true"
      height="460"
    >
      <el-table-column label="行号" prop="rowNo" width="70" />
      <el-table-column label="动作" prop="importAction" width="110">
        <template #default="{ row }">
          <el-tag :type="recognitionMigrationImportActionTagType(row.importAction)">
            {{ formatRecognitionMigrationImportAction(row.importAction) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目录路径" prop="directoryPath" min-width="180" />
      <el-table-column label="文件名称" prop="fileName" min-width="180" />
      <el-table-column label="文件编号" prop="fileNumber" min-width="140" />
      <el-table-column label="正式服文件ID" prop="targetControlledFileId" width="130" />
      <el-table-column label="识别状态" prop="recognitionStatus" width="130">
        <template #default="{ row }">
          {{ formatRecognitionStatus(row.recognitionStatus) }}
        </template>
      </el-table-column>
      <el-table-column label="产品编码" prop="productCode" min-width="150" />
      <el-table-column label="产品名称" prop="productName" min-width="150" />
      <el-table-column label="项目编码" prop="projectCode" min-width="130" />
      <el-table-column label="项目名称" prop="projectName" min-width="150" />
      <el-table-column label="文件类型" min-width="220">
        <template #default="{ row }">
          {{ formatRecognitionMigrationFileTypes(row) }}
        </template>
      </el-table-column>
      <el-table-column label="失败原因" prop="failureReason" min-width="260">
        <template #default="{ row }">{{ row.failureReason || '-' }}</template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog
    v-model="batchRecognitionConfirmVisible"
    title="识别当前文件夹及子文件夹"
    width="680px"
    destroy-on-close
  >
    <div class="batch-recognition-dialog">
      <div class="batch-recognition-dialog__section">
        <div class="batch-recognition-dialog__title">执行上下文</div>
        <div class="batch-recognition-dialog__grid">
          <div class="batch-recognition-dialog__item">
            <span class="batch-recognition-dialog__label">范围</span>
            <span class="batch-recognition-dialog__value">{{ batchRecognitionScopeLabel }}</span>
          </div>
          <div
            v-if="isCurrentDirectorySearch"
            class="batch-recognition-dialog__item batch-recognition-dialog__item--full"
          >
            <span class="batch-recognition-dialog__label">当前目录</span>
            <span class="batch-recognition-dialog__value">{{ selectedDirectoryPath || '-' }}</span>
          </div>
          <div class="batch-recognition-dialog__item">
            <span class="batch-recognition-dialog__label">关键字</span>
            <span class="batch-recognition-dialog__value">{{ batchRecognitionKeywordLabel }}</span>
          </div>
          <div class="batch-recognition-dialog__item">
            <span class="batch-recognition-dialog__label">状态</span>
            <span class="batch-recognition-dialog__value">{{ batchRecognitionStatusFilterLabel }}</span>
          </div>
          <div class="batch-recognition-dialog__item">
            <span class="batch-recognition-dialog__label">类别</span>
            <span class="batch-recognition-dialog__value">{{ batchRecognitionCategoryLabel }}</span>
          </div>
          <div class="batch-recognition-dialog__item batch-recognition-dialog__item--full">
            <span class="batch-recognition-dialog__label">文件名/title 同步</span>
            <span class="batch-recognition-dialog__value">
              识别成功后自动同步 fileName/title/productName/productCode/dccProjectCodeId
            </span>
          </div>
        </div>
      </div>

      <div class="batch-recognition-dialog__section">
        <div class="batch-recognition-dialog__title">并发 Codex 数量</div>
        <el-input-number
          v-model="batchRecognitionForm.workerCount"
          data-testid="dcc-browser-batch-recognition-worker-count"
          :min="1"
          :max="20"
          :step="1"
          :precision="0"
          controls-position="right"
        />
        <div class="batch-recognition-dialog__hint">
          同一个批量任务内按该数量并发识别，后台通过共享 claim 防止多个 Codex 抢同一个文件。
        </div>
      </div>

      <div class="batch-recognition-dialog__section">
        <div class="batch-recognition-dialog__title">覆盖策略</div>
        <el-radio-group
          v-model="batchRecognitionForm.existingRecordPolicy"
          class="batch-recognition-dialog__policy-group"
          data-testid="dcc-browser-batch-recognition-policy"
        >
          <el-radio
            v-for="item in batchRecognitionExistingRecordPolicyOptions"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </el-radio>
        </el-radio-group>
        <div class="batch-recognition-dialog__hint">
          {{ batchRecognitionSelectedPolicyHint }}
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="batchRecognitionConfirmVisible = false">取消</el-button>
      <el-button
        type="primary"
        data-testid="dcc-browser-batch-recognition-confirm"
        :loading="batchRecognitionCreating"
        @click="confirmBatchRecognition"
      >
        开始识别
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="batchRecognitionProgressVisible"
    title="批量识别进度"
    width="760px"
  >
    <div v-if="batchRecognitionTask" class="batch-recognition-progress">
      <div class="batch-recognition-progress__header">
        <div class="batch-recognition-progress__context">
          <div class="batch-recognition-progress__context-line">
            <span class="batch-recognition-progress__context-label">范围</span>
            <span class="batch-recognition-progress__context-value">
              {{ batchRecognitionTaskScopeLabel }}
            </span>
          </div>
          <div
            v-if="batchRecognitionTask.scope === BATCH_RECOGNITION_SCOPE_CURRENT"
            class="batch-recognition-progress__context-line"
          >
            <span class="batch-recognition-progress__context-label">目录</span>
            <span class="batch-recognition-progress__context-value">
              {{ batchRecognitionTask.directoryPath || '-' }}
            </span>
          </div>
          <div class="batch-recognition-progress__context-line">
            <span class="batch-recognition-progress__context-label">筛选条件</span>
            <span class="batch-recognition-progress__context-value">
              {{ batchRecognitionTaskFilterSummary }}
            </span>
          </div>
          <div class="batch-recognition-progress__context-line">
            <span class="batch-recognition-progress__context-label">覆盖策略</span>
            <span class="batch-recognition-progress__context-value">
              {{ batchRecognitionTaskExistingRecordPolicyText }}
            </span>
          </div>
        </div>
        <el-tag :type="batchRecognitionStatusTagType" effect="light">
          {{ batchRecognitionStatusText }}
        </el-tag>
      </div>

      <el-progress
        class="batch-recognition-progress__bar"
        :percentage="batchRecognitionProgressPercentage"
        :stroke-width="6"
      />

      <div class="batch-recognition-progress__grid">
        <div class="batch-recognition-progress__item">
          <span class="batch-recognition-progress__label">总数</span>
          <strong class="batch-recognition-progress__value">{{ batchRecognitionTask.totalCount }}</strong>
        </div>
        <div class="batch-recognition-progress__item">
          <span class="batch-recognition-progress__label">配置 Codex</span>
          <strong class="batch-recognition-progress__value">
            {{ batchRecognitionTask.workerCount || 0 }}
          </strong>
        </div>
        <div class="batch-recognition-progress__item">
          <span class="batch-recognition-progress__label">运行 Codex</span>
          <strong class="batch-recognition-progress__value">
            {{ batchRecognitionTask.activeWorkerCount || 0 }}
          </strong>
        </div>
        <div class="batch-recognition-progress__item">
          <span class="batch-recognition-progress__label">已记录文件</span>
          <strong class="batch-recognition-progress__value">
            {{ batchRecognitionTask.recordedFileCount || 0 }}
          </strong>
        </div>
        <div class="batch-recognition-progress__item">
          <span class="batch-recognition-progress__label">已处理</span>
          <strong class="batch-recognition-progress__value">
            {{ batchRecognitionTask.processedCount }}
          </strong>
        </div>
        <button
          class="batch-recognition-progress__item batch-recognition-progress__item--action"
          type="button"
          data-testid="dcc-browser-batch-recognition-success-records"
          :disabled="!batchRecognitionTask.successCount"
          @click="showBatchRecognitionRecords('SUCCESS')"
        >
          <span class="batch-recognition-progress__label">成功</span>
          <strong class="batch-recognition-progress__value">{{ batchRecognitionTask.successCount }}</strong>
        </button>
        <button
          class="batch-recognition-progress__item batch-recognition-progress__item--action"
          type="button"
          data-testid="dcc-browser-batch-recognition-failed-records"
          :disabled="!batchRecognitionTask.failedCount"
          @click="showBatchRecognitionRecords('FAILED')"
        >
          <span class="batch-recognition-progress__label">失败</span>
          <strong class="batch-recognition-progress__value">{{ batchRecognitionTask.failedCount }}</strong>
        </button>
        <div class="batch-recognition-progress__item">
          <span class="batch-recognition-progress__label">成功 + 失败 = 总数</span>
          <strong class="batch-recognition-progress__value">
            {{ batchRecognitionTask.successCount + batchRecognitionTask.failedCount }}
          </strong>
        </div>
        <div class="batch-recognition-progress__item">
          <span class="batch-recognition-progress__label">剩余</span>
          <strong class="batch-recognition-progress__value">
            {{ batchRecognitionTask.remainingCount }}
          </strong>
        </div>
        <div class="batch-recognition-progress__item">
          <span class="batch-recognition-progress__label">当前状态</span>
          <strong class="batch-recognition-progress__value">{{ batchRecognitionStatusText }}</strong>
        </div>
        <div class="batch-recognition-progress__item batch-recognition-progress__item--full">
          <span class="batch-recognition-progress__label">最后错误</span>
          <strong class="batch-recognition-progress__value batch-recognition-progress__value--error">
            {{ batchRecognitionTask.lastFailureMessage || '-' }}
          </strong>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="batchRecognitionProgressVisible = false">
        {{ isBatchRecognitionTaskActive(batchRecognitionTask) ? '隐藏进度' : '关闭' }}
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="extensionBlacklistDialogVisible"
    title="文件后缀黑名单"
    width="520px"
    :close-on-click-modal="false"
  >
    <div v-loading="extensionBlacklistLoading" class="browser-extension-blacklist">
      <el-input
        v-model="extensionBlacklistText"
        data-testid="dcc-browser-extension-blacklist-input"
        type="textarea"
        :rows="6"
        placeholder="例如：*.db、*.pyc"
      />
      <div class="browser-extension-blacklist__hint">
        可用换行、逗号或空格分隔；保存后命中后缀的文件不会出现在文件查阅列表。
      </div>
    </div>
    <template #footer>
      <el-button @click="extensionBlacklistDialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        data-testid="dcc-browser-extension-blacklist-save"
        :loading="extensionBlacklistSaving"
        @click="saveExtensionBlacklist"
      >
        保存
      </el-button>
    </template>
  </el-dialog>

  <ControlledFileMetadataDialog
    v-if="metadataDialogMounted"
    v-model="metadataDialogVisible"
    :file="metadataEditingFile"
    :categories="categories"
    :directories="directories"
    load-directories-on-open
    @saved="handleMetadataSaved"
  />
</template>

<script lang="ts" setup>
import { isSearchModelInputEmpty } from '@/utils/search'
import { ElMessageBox, type ElTree } from 'element-plus'
import { useClipboard } from '@vueuse/core'
import download from '@/utils/download'
import { getFileCategoryList, type ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import {
  getDirectory,
  getDirectoryTree,
  searchDirectories,
  type ControlledFileDirectoryVO
} from '@/api/dcc/controlledFile/directories'
import {
  confirmControlledFileMetadataImport,
  confirmControlledFileRecognitionMigrationImport,
  createControlledFileBatchRecognitionTask,
  exportControlledFileMetadataExcel,
  exportControlledFileRecognitionMigrationExcel,
  exportControlledFileRecognitionRecordExcel,
  getControlledFileMetadataImportTemplate,
  getControlledFileBatchRecognitionTask,
  getControlledFileBrowserExtensionBlacklist,
  getControlledFileBrowserPage,
  previewControlledFileRecognitionMigrationImport,
  previewControlledFileMetadataImport,
  saveControlledFileBrowserExtensionBlacklist,
  triggerControlledFileDownload,
  type ControlledFileBatchRecognitionCreateReqVO,
  type ControlledFileBatchRecognitionTaskRespVO,
  type ControlledFileMetadataImportPreviewRespVO,
  type ControlledFileMetadataImportRowRespVO,
  type ControlledFilePageReqVO,
  type ControlledFileRecognitionMigrationImportPreviewRespVO,
  type ControlledFileRecognitionMigrationImportRowRespVO,
  type ControlledFileVersionHistoryVO,
  type ControlledFileVO
} from '@/api/dcc/controlledFile/workflow'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import { useUserStore } from '@/store/modules/user'
import { openControlledFileTraceability } from '../shared/viewer-navigation'
import {
  hasDccControlledFileActionProjection,
  isDccControlledFileActionUnlocked
} from '../shared/lifecycle'
import { buildControlledFileViewerPath } from '../view/presentation'
import {
  DCC_BROWSER_STATE_CACHE_SCHEMA_VERSION,
  buildDccBrowserCacheContext,
  clearDccBrowserRememberedState,
  readDccBrowserMetadataCache,
  readDccBrowserRememberedState,
  writeDccBrowserMetadataCache,
  writeDccBrowserRememberedState,
  type BrowserSearchScopeValue,
  type DccBrowserMetadataDirectoryNode,
  type DccBrowserRememberedState
} from './state-cache'
import {
  BROWSER_STATUS_FILTER_OPTIONS,
  getBrowserVersionSummary,
  getBrowserRowActionState,
  getBrowserPublishedFileStatusText,
  getBrowserStampedFileStatusText,
  getBrowserCurrentVersionSourceText
} from './presentation'
import { parsePositiveRouteQueryId, sameRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'DccControlledFileBrowser' })

const ControlledFileMetadataDialog = defineAsyncComponent(
  () => import('../shared/ControlledFileMetadataDialog.vue')
)

const route = useRoute()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const { copy: copyToClipboard } = useClipboard({ legacy: true })
const DCC_BROWSER_COLUMN_TABLE_KEY = 'dcc.controlledFile.browser.adminStyle'
const dccBrowserDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'fileName', label: '文件名称', minWidth: 280 },
  { key: 'fileNumber', label: '文件编号', minWidth: 160 },
  { key: 'directory', label: '所在目录', minWidth: 220, visible: false },
  { key: 'productName', label: '产品名称', minWidth: 150, visible: false },
  { key: 'category', label: '类别', minWidth: 160, visible: false },
  { key: 'versionSummary', label: '版本摘要', minWidth: 310, visible: false },
  { key: 'remark', label: '备注', minWidth: 220, visible: false },
  { key: 'operation', label: '操作', width: 320, hideable: false, business: false }
]
const dccBrowserQueryInputFields = ['keyword', 'status', 'categoryId']
const {
  columns: dccBrowserColumns,
  saving: dccBrowserColumnSaving,
  isColumnVisible: isDccBrowserColumnVisible,
  getColumnWidthString: getDccBrowserColumnWidthString,
  getColumnMinWidthString: getDccBrowserColumnMinWidthString,
  handleHeaderDragend: handleDccBrowserHeaderDragend,
  saveConfig: saveDccBrowserColumnConfig,
  resetConfig: resetDccBrowserColumnConfig
} = useUserTableColumns(DCC_BROWSER_COLUMN_TABLE_KEY, dccBrowserDefaultColumns)

const DOC_CONTROL_ROLE_CODE = 'doc_control'
const hasMetadataEditorRole = (roles: string[]) => roles.includes(DOC_CONTROL_ROLE_CODE)
const BROWSER_SEARCH_SCOPE_CURRENT = 'current'
const BROWSER_SEARCH_SCOPE_GLOBAL = 'global'
const DCC_BROWSER_ROUTE_PATH = '/dcc/controlled-file/browser'
const BATCH_RECOGNITION_SCOPE_CURRENT = 'CURRENT'
const BATCH_RECOGNITION_SCOPE_GLOBAL = 'GLOBAL'
const BATCH_RECOGNITION_STATUS_WAITING = 'WAITING'
const BATCH_RECOGNITION_STATUS_RUNNING = 'RUNNING'
const BATCH_RECOGNITION_STATUS_COMPLETED = 'COMPLETED'
const BATCH_RECOGNITION_STATUS_FAILED = 'FAILED'
const BATCH_RECOGNITION_STATUS_STOPPED = 'STOPPED'
const BATCH_RECOGNITION_POLL_INTERVAL = 2000
const BATCH_RECOGNITION_POLICY_SKIP_ALL_EXISTING = 'SKIP_ALL_EXISTING'
const BATCH_RECOGNITION_POLICY_RETRY_FAILED = 'RETRY_FAILED'
const BATCH_RECOGNITION_POLICY_OVERWRITE_ALL = 'OVERWRITE_ALL'
const FILE_NUMBER_RECOGNITION_TYPE = 'FILE_NUMBER'
const RECOGNITION_STATUS_UNKNOWN_DCC_BASIC_DATA = 'UNKNOWN_DCC_BASIC_DATA'
const RECOGNITION_STATUS_UNRECOGNIZED_PROJECT_NAME = 'UNRECOGNIZED_PROJECT_NAME'
const RECOGNITION_STATUS_LABELS: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失败',
  NO_MATCH: '未知基础数据',
  [RECOGNITION_STATUS_UNKNOWN_DCC_BASIC_DATA]: '未知基础数据',
  [RECOGNITION_STATUS_UNRECOGNIZED_PROJECT_NAME]: '未识别项目名称'
}

const formatRecognitionStatus = (status?: string | null) => {
  if (!status) {
    return '-'
  }
  return RECOGNITION_STATUS_LABELS[status] || status
}

const resolveBrowserErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}

type BrowserSearchScope = BrowserSearchScopeValue

const browserSearchScopeOptions = [
  { label: '当前目录', value: BROWSER_SEARCH_SCOPE_CURRENT },
  { label: '全域', value: BROWSER_SEARCH_SCOPE_GLOBAL }
]
const batchRecognitionExistingRecordPolicyOptions = [
  {
    label: '跳过已有记录',
    value: BATCH_RECOGNITION_POLICY_SKIP_ALL_EXISTING,
    hint: '按文件、识别范围和识别版本防重复；已有成功或失败记录会计入对应成功/失败数量。'
  },
  {
    label: '跳过成功，重试失败和未识别',
    value: BATCH_RECOGNITION_POLICY_RETRY_FAILED,
    hint: '已经识别成功的文件不会重复调用 Codex；之前失败、未知基础数据、未识别项目名称、未匹配和没有台账的文件会重新识别。'
  },
  {
    label: '覆盖全部已有值',
    value: BATCH_RECOGNITION_POLICY_OVERWRITE_ALL,
    hint: '本次会重新调用 Codex，并重写已有产品名称、产品编号和项目编码。'
  }
]

const directoryLoading = ref(false)
const loading = ref(false)
const total = ref(0)
const directories = ref<ControlledFileDirectoryNode[]>([])
const categories = ref<ControlledFileCategoryVO[]>([])
const downloadLoadingId = ref<number>()
const metadataExporting = ref(false)
const recognitionRecordExporting = ref(false)
const recognitionMigrationExporting = ref(false)
const metadataTemplateDownloading = ref(false)
const metadataImportVisible = ref(false)
const metadataImportPreviewLoading = ref(false)
const metadataImportConfirmLoading = ref(false)
const metadataImportFileList = ref<any[]>([])
const metadataImportFile = ref<File | null>(null)
const metadataImportPreview = ref<ControlledFileMetadataImportPreviewRespVO | null>(null)
const recognitionMigrationImportVisible = ref(false)
const recognitionMigrationImportPreviewLoading = ref(false)
const recognitionMigrationImportConfirmLoading = ref(false)
const recognitionMigrationImportFileList = ref<any[]>([])
const recognitionMigrationImportFile = ref<File | null>(null)
const recognitionMigrationImportPreview =
  ref<ControlledFileRecognitionMigrationImportPreviewRespVO | null>(null)
const metadataDialogVisible = ref(false)
const metadataDialogMounted = ref(false)
const metadataEditingFile = ref<ControlledFileVO>()
const batchRecognitionConfirmVisible = ref(false)
const batchRecognitionProgressVisible = ref(false)
const batchRecognitionCreating = ref(false)
const batchRecognitionPolling = ref(false)
const batchRecognitionTask = ref<ControlledFileBatchRecognitionTaskRespVO>()
const advancedActionsVisible = ref(false)
const extensionBlacklistDialogVisible = ref(false)
const extensionBlacklistLoading = ref(false)
const extensionBlacklistSaving = ref(false)
const extensionBlacklistText = ref('')

type ControlledFileBrowserRow = ControlledFileVO & {
  selectedVersionId?: number
}

type ControlledFileBrowserVersion = ControlledFileVersionHistoryVO &
  Partial<Pick<ControlledFileVO, 'directoryId'>> &
  Pick<ControlledFileVO, 'actionProjection' | 'canPreview' | 'canDownload' | 'canPrint'>

type ControlledFileDirectoryNode = ControlledFileDirectoryVO & {
  leaf?: boolean
  children?: ControlledFileDirectoryNode[]
}

const list = ref<ControlledFileBrowserRow[]>([])
const metadataImportRows = computed<ControlledFileMetadataImportRowRespVO[]>(
  () => metadataImportPreview.value?.rows || []
)
const recognitionMigrationImportRows = computed<ControlledFileRecognitionMigrationImportRowRespVO[]>(
  () => recognitionMigrationImportPreview.value?.rows || []
)

const directoryKeyword = ref('')
const selectedDirectoryId = ref<number>()
const selectedDirectory = ref<ControlledFileDirectoryNode>()
const searchScope = ref<BrowserSearchScope>(BROWSER_SEARCH_SCOPE_CURRENT)
const directorySearchLoading = ref(false)
const directorySearchResults = ref<ControlledFileDirectoryNode[]>([])
const directoryTreeRef = ref<InstanceType<typeof ElTree>>()
const directoryPathMap = ref(new Map<number, string>())
const directoryNodeById = ref(new Map<number, ControlledFileDirectoryNode>())
const directoryChildrenCache = ref(new Map<number | 'root', ControlledFileDirectoryNode[]>())
const expandedDirectoryIds = ref<Set<number>>(new Set())
let directorySearchSeq = 0

const getBrowserCacheContext = () => {
  const userId = userStore.getUser.id
  if (!userId) {
    throw new Error('DCC 受控浏览本地缓存缺少登录用户上下文，无法继续。')
  }
  return buildDccBrowserCacheContext({
    tenantId: getTenantId(),
    visitTenantId: getVisitTenantId(),
    userId,
    schemaVersion: DCC_BROWSER_STATE_CACHE_SCHEMA_VERSION
  })
}

const DCC_BROWSER_DEFAULT_PAGE_SIZE = 20
const DCC_BROWSER_MIN_PAGE_SIZE = 20

const queryParams = reactive({
  pageNo: 1,
  pageSize: DCC_BROWSER_DEFAULT_PAGE_SIZE,
  categoryId: undefined as number | undefined,
  status: undefined as string | undefined,
  keyword: undefined as string | undefined,
  recognitionStatus: undefined as string | undefined,
  batchRecognitionTaskId: undefined as number | undefined,
  quickFilter: undefined as any
})
const batchRecognitionForm = reactive({
  existingRecordPolicy: BATCH_RECOGNITION_POLICY_RETRY_FAILED,
  workerCount: 5
})
let batchRecognitionPollingTimer: number | undefined
let batchRecognitionPollingInFlight = false
let batchRecognitionTerminalHandled = false

const directoryTreeProps = {
  children: 'children',
  label: 'name',
  isLeaf: 'leaf'
}
const isValidBrowserOptionId = (value: unknown): value is number =>
  typeof value === 'number' && Number.isFinite(value)
const categoryOptions = computed(() =>
  categories.value.filter(
    (item): item is ControlledFileCategoryVO & { id: number } =>
      item.active && isValidBrowserOptionId(item.id)
  )
)
const categoryNameMap = computed(
  () => new Map(categories.value.map((item) => [item.id as number, item.name]))
)
const dccBrowserQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  { key: 'keyword', label: '全文关键字', type: 'text', placeholder: '请输入文件名称/编号' },
  { key: 'fileName', label: '文件名称', type: 'text', placeholder: '请输入文件名称' },
  { key: 'fileNumber', label: '文件编号', type: 'text', placeholder: '请输入文件编号' },
  { key: 'status', label: '状态', type: 'select', options: BROWSER_STATUS_FILTER_OPTIONS },
  {
    key: 'categoryId',
    label: '类别',
    type: 'select',
    options: categoryOptions.value.map((item) => ({ label: item.name, value: item.id as number }))
  }
])
const canEditMetadata = computed(() => hasMetadataEditorRole(userStore.getRoles))
const isCurrentDirectorySearch = computed(() => searchScope.value === BROWSER_SEARCH_SCOPE_CURRENT)
const isGlobalSearch = computed(() => searchScope.value === BROWSER_SEARCH_SCOPE_GLOBAL)
const isQueryDisabled = computed(() => isCurrentDirectorySearch.value && !selectedDirectoryId.value)
const tableEmptyText = computed(() => {
  if (isCurrentDirectorySearch.value && !selectedDirectoryId.value) {
    return '请先选择受控浏览目录'
  }
  return '无权限或无匹配当前有效文件'
})
const selectedDirectoryPath = computed(
  () => selectedDirectory.value?.directoryPath || selectedDirectory.value?.name || ''
)
const browserDirectoryScopeText = computed(() => {
  if (isGlobalSearch.value) {
    return '全域受控浏览'
  }
  return selectedDirectoryPath.value || '未选择目录'
})
const browserKeywordText = computed(() => normalizeKeyword(queryParams.keyword) || '未设置')
const browserStatusText = computed(
  () => BROWSER_STATUS_FILTER_OPTIONS.find((item) => item.value === queryParams.status)?.label || '全部状态'
)
const browserCategoryText = computed(() => {
  if (!queryParams.categoryId) {
    return '全部类别'
  }
  return categoryNameMap.value.get(queryParams.categoryId) || `类别 #${queryParams.categoryId}`
})
const browserFilterSummaryItems = computed(() => [
  {
    label: '受控浏览目录路径',
    value: browserDirectoryScopeText.value
  },
  {
    label: '分类',
    value: browserCategoryText.value
  },
  {
    label: '项目代码/文件编号关键字',
    value: browserKeywordText.value
  },
  {
    label: '当前有效状态',
    value: browserStatusText.value
  }
])
const tableEmptyHint = computed(() => {
  if (isCurrentDirectorySearch.value && !selectedDirectoryId.value) {
    return '请选择左侧目录后再按目录/分类/项目代码定位当前有效文件。'
  }
  return `当前筛选条件：目录 ${browserDirectoryScopeText.value}；分类 ${browserCategoryText.value}；关键字 ${browserKeywordText.value}。若目标 ACTIVE 文件已发布但不可见，通常表示当前账号无权限或筛选条件下无匹配当前有效文件。`
})
const batchRecognitionScopeLabel = computed(() =>
  isCurrentDirectorySearch.value ? '当前目录 + 子目录' : '全域'
)
const batchRecognitionKeywordLabel = computed(() => normalizeKeyword(queryParams.keyword) || '未设置')
const batchRecognitionStatusFilterLabel = computed(
  () =>
    BROWSER_STATUS_FILTER_OPTIONS.find((item) => item.value === queryParams.status)?.label || '全部状态'
)
const batchRecognitionCategoryLabel = computed(() => {
  if (!queryParams.categoryId) {
    return '全部类别'
  }
  return categoryNameMap.value.get(queryParams.categoryId) || `类别 #${queryParams.categoryId}`
})
const handleAdvancedAction = async (action: () => void | Promise<void>) => {
  advancedActionsVisible.value = false
  await action()
}
const parseExtensionBlacklistText = () =>
  extensionBlacklistText.value
    .split(/[\s,，;；]+/)
    .map((item) => item.trim())
    .filter(Boolean)
const openExtensionBlacklistDialog = async () => {
  extensionBlacklistDialogVisible.value = true
  extensionBlacklistLoading.value = true
  try {
    const data = await getControlledFileBrowserExtensionBlacklist()
    extensionBlacklistText.value = (data.extensionPatterns || []).join('\n')
  } finally {
    extensionBlacklistLoading.value = false
  }
}
const saveExtensionBlacklist = async () => {
  extensionBlacklistSaving.value = true
  try {
    await saveControlledFileBrowserExtensionBlacklist({ extensionPatterns: parseExtensionBlacklistText() })
    message.success('黑名单已保存')
    extensionBlacklistDialogVisible.value = false
    await getList()
  } finally {
    extensionBlacklistSaving.value = false
  }
}
const batchRecognitionTaskScopeLabel = computed(() =>
  batchRecognitionTask.value?.scope === BATCH_RECOGNITION_SCOPE_CURRENT ? '当前目录 + 子目录' : '全域'
)
const batchRecognitionTaskFilterSummary = computed(() => {
  const task = batchRecognitionTask.value
  if (!task) {
    return '-'
  }
  const statusLabel =
    BROWSER_STATUS_FILTER_OPTIONS.find((item) => item.value === task.statusFilter)?.label || '全部状态'
  const categoryLabel = task.categoryId
    ? categoryNameMap.value.get(task.categoryId) || `类别 #${task.categoryId}`
    : '全部类别'
  return `关键字：${task.keyword || '未设置'} / 状态：${statusLabel} / 类别：${categoryLabel}`
})
const getBatchRecognitionExistingRecordPolicyOption = (policy?: string | null) =>
  batchRecognitionExistingRecordPolicyOptions.find((item) => item.value === policy)
const batchRecognitionSelectedPolicyHint = computed(() =>
  getBatchRecognitionExistingRecordPolicyOption(batchRecognitionForm.existingRecordPolicy)?.hint || ''
)
const batchRecognitionTaskExistingRecordPolicyText = computed(() =>
  getBatchRecognitionExistingRecordPolicyOption(batchRecognitionTask.value?.existingRecordPolicy)?.label || '-'
)
const batchRecognitionProgressPercentage = computed(() => {
  const task = batchRecognitionTask.value
  if (!task) {
    return 0
  }
  if (task.totalCount <= 0) {
    return task.status === BATCH_RECOGNITION_STATUS_COMPLETED ? 100 : 0
  }
  return Math.min(100, Math.round((task.processedCount / task.totalCount) * 100))
})
const batchRecognitionStatusText = computed(() =>
  getBatchRecognitionStatusText(batchRecognitionTask.value?.status)
)
const batchRecognitionStatusTagType = computed(() =>
  getBatchRecognitionStatusTagType(batchRecognitionTask.value?.status)
)

const buildCurrentVersionOption = (row: ControlledFileVO): ControlledFileBrowserVersion => ({
  id: row.id,
  title: row.title,
  fileNumber: row.fileNumber || '',
  versionNo: row.versionNo,
  status: row.status,
  effectiveDate: row.effectiveDate,
  publishedTime: row.publishedTime,
  obsoletedTime: row.obsoletedTime,
  supersededByFileId: row.supersededByFileId,
  remark: row.remark,
  directoryId: row.directoryId,
  canPreview: row.canPreview,
  canDownload: row.canDownload,
  canPrint: row.canPrint,
  modifying: row.modifying,
  actionProjection: row.actionProjection
})

const getVersionOptions = (row: ControlledFileBrowserRow): ControlledFileBrowserVersion[] => {
  const historyOptions = (row.versionHistory || []).filter(
    (item): item is ControlledFileBrowserVersion => isValidBrowserOptionId(item.id)
  )
  if (historyOptions.length) {
    return historyOptions
  }
  return isValidBrowserOptionId(row.id) ? [buildCurrentVersionOption(row)] : []
}

const resolveInitialSelectedVersionId = (row: ControlledFileVO): number | undefined => {
  return getVersionOptions(row as ControlledFileBrowserRow)[0]?.id
}

const getSelectedVersion = (row: ControlledFileBrowserRow): ControlledFileBrowserVersion => {
  return getVersionOptions(row).find((item) => item.id === row.selectedVersionId) || getVersionOptions(row)[0] || buildCurrentVersionOption(row)
}

const handleVersionChange = (row: ControlledFileBrowserRow) => {
  const selectedVersionId = getSelectedVersion(row).id
  row.selectedVersionId = isValidBrowserOptionId(selectedVersionId)
    ? selectedVersionId
    : resolveInitialSelectedVersionId(row)
}

const getBrowserDirectoryPath = (directoryId?: number | null) => {
  if (!directoryId) {
    return '-'
  }
  if (directoryId === selectedDirectoryId.value && selectedDirectoryPath.value) {
    return selectedDirectoryPath.value
  }
  return directoryPathMap.value.get(directoryId) || '-'
}

const getBrowserFileDisplayName = (row: ControlledFileBrowserRow) => {
  return getSelectedVersion(row).title || row.fileName || '-'
}

const getBrowserFileNameTooltip = (row: ControlledFileBrowserRow) => {
  const displayName = getBrowserFileDisplayName(row)
  const sourceName = String(row.fileName || '').trim()
  if (sourceName && sourceName !== displayName) {
    return `文件名称：${displayName}\n源文件名：${sourceName}`
  }
  return displayName
}

const getBrowserCurrentActiveRowSummary = (row: ControlledFileBrowserRow) => {
  const selectedVersion = getSelectedVersion(row)
  return {
    versionNo: selectedVersion.versionNo || row.versionNo || '-',
    directoryPath: getBrowserDirectoryPath(selectedVersion.directoryId || row.directoryId),
    publishedFileStatus: getBrowserPublishedFileStatusText(selectedVersion),
    stampedFileStatus: getBrowserStampedFileStatusText(selectedVersion),
    currentVersionSource: getBrowserCurrentVersionSourceText(selectedVersion)
  }
}

const hasBrowserMoreActions = (row: ControlledFileBrowserRow) =>
  canEditMetadata.value &&
  isLatestVersionSelected(row) &&
  hasDccControlledFileActionProjection(getSelectedVersion(row)) &&
  isDccControlledFileActionUnlocked(getSelectedVersion(row))

const hasBrowserRowActions = (row: ControlledFileBrowserRow) => {
  const actionState = getBrowserRowActionState(getSelectedVersion(row))
  return Boolean(
    actionState.canPreview ||
      actionState.canDownload ||
      actionState.canPrint ||
      getSelectedVersion(row).id ||
      hasBrowserMoreActions(row)
  )
}

const getBrowserRowActionBlockReason = (row: ControlledFileBrowserRow) => {
  if (hasBrowserRowActions(row)) {
    return ''
  }
  return getBrowserRowActionState(getSelectedVersion(row)).actionReadonlyReason
}

const handleBrowserRowCommand = (command: string, row: ControlledFileBrowserRow) => {
  if (command === 'metadata') {
    openMetadataDialog(row)
  }
}

const isLatestVersionSelected = (row: ControlledFileBrowserRow) => row.selectedVersionId === row.id

const isSelectedVersionModifying = (row: ControlledFileBrowserRow) =>
  isLatestVersionSelected(row) && !!row.modifying

const parsePositiveNumber = (value: unknown) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const resolveBrowserPageSize = (value: unknown) => {
  const parsedPageSize = parsePositiveNumber(value)
  if (!parsedPageSize) {
    return DCC_BROWSER_DEFAULT_PAGE_SIZE
  }
  return Math.max(parsedPageSize, DCC_BROWSER_MIN_PAGE_SIZE)
}

const parseQueryString = (value: unknown) => {
  const firstValue = Array.isArray(value) ? value[0] : value
  const parsed = typeof firstValue === 'string' ? firstValue.trim() : ''
  return parsed || undefined
}

const normalizeKeyword = (value?: string) => {
  const normalized = String(value || '').trim()
  return normalized || undefined
}

const toDirectoryCacheNode = (
  node: ControlledFileDirectoryNode
): DccBrowserMetadataDirectoryNode => ({
  id: node.id,
  parentId: node.parentId,
  code: node.code,
  name: node.name,
  active: node.active,
  sort: node.sort,
  hasChildren: node.hasChildren,
  directoryPath: node.directoryPath,
  remark: node.remark,
  createTime: node.createTime
})

const buildDirectoryChildrenCacheRecord = () => {
  const cache: Record<string, DccBrowserMetadataDirectoryNode[]> = {}
  directoryChildrenCache.value.forEach((value, key) => {
    cache[String(key)] = value.map(toDirectoryCacheNode)
  })
  return cache
}

const buildExpandedDirectoryIdsCacheRecord = () => Array.from(expandedDirectoryIds.value)

const restoreExpandedDirectoryIdsCacheRecord = (directoryIds?: number[]) => {
  expandedDirectoryIds.value = new Set(directoryIds || [])
}

const syncDirectoryTreeExpandedState = () => {
  const treeStoreNodesMap = directoryTreeRef.value?.store?.nodesMap
  if (!treeStoreNodesMap) {
    return
  }
  Object.values(treeStoreNodesMap).forEach((treeNode: any) => {
    const directoryId = Number(treeNode?.data?.id)
    if (!Number.isFinite(directoryId) || directoryId <= 0 || treeNode.level <= 0) {
      return
    }
    const shouldExpand = expandedDirectoryIds.value.has(directoryId)
    if (shouldExpand && !treeNode.expanded) {
      treeNode.expand()
      return
    }
    if (!shouldExpand && treeNode.expanded) {
      treeNode.collapse()
    }
  })
}

const normalizeDirectoryChildrenCacheRecord = (
  cache: Record<string, DccBrowserMetadataDirectoryNode[]>
) => {
  const normalizedCache = new Map<number | 'root', DccBrowserMetadataDirectoryNode[]>()
  Object.entries(cache).forEach(([key, value]) => {
    if (!Array.isArray(value)) {
      throw new Error(`DCC 受控浏览目录缓存值非法：${key}`)
    }
    if (key === 'root') {
      normalizedCache.set('root', value)
      return
    }
    const normalizedKey = Number(key)
    if (!Number.isFinite(normalizedKey) || normalizedKey <= 0) {
      throw new Error(`DCC 受控浏览目录缓存键非法：${key}`)
    }
    normalizedCache.set(normalizedKey, value)
  })
  return normalizedCache
}

const buildDirectoryTreeFromCacheRecord = (
  cacheRecord: Map<number | 'root', DccBrowserMetadataDirectoryNode[]>
) => {
  const buildNodes = (parentKey: number | 'root'): ControlledFileDirectoryNode[] => {
    const children = cacheRecord.get(parentKey) || []
    return children.map((item) => {
      const childNodes = item.id ? buildNodes(item.id) : []
      return {
        ...item,
        children: childNodes,
        leaf: item.hasChildren === undefined ? !(childNodes.length) : !item.hasChildren
      }
    })
  }
  return buildNodes('root')
}

const rememberExpandedDirectoryId = (directoryId?: number) => {
  if (!directoryId) {
    return false
  }
  const nextExpandedDirectoryIds = new Set(expandedDirectoryIds.value)
  const previousSize = nextExpandedDirectoryIds.size
  nextExpandedDirectoryIds.add(directoryId)
  if (nextExpandedDirectoryIds.size === previousSize) {
    return false
  }
  expandedDirectoryIds.value = nextExpandedDirectoryIds
  return true
}

const forgetExpandedDirectoryId = (directoryId?: number) => {
  if (!directoryId) {
    return false
  }
  const nextExpandedDirectoryIds = new Set(expandedDirectoryIds.value)
  const existed = nextExpandedDirectoryIds.delete(directoryId)
  if (!existed) {
    return false
  }
  expandedDirectoryIds.value = nextExpandedDirectoryIds
  return true
}

const buildBrowserRememberedState = (): DccBrowserRememberedState => ({
  scope: searchScope.value,
  directoryId: selectedDirectoryId.value,
  lastOpenedDirectoryId: selectedDirectoryId.value,
  pageNo: queryParams.pageNo,
  pageSize: queryParams.pageSize,
  categoryId: queryParams.categoryId,
  status: normalizeKeyword(queryParams.status),
  keyword: normalizeKeyword(queryParams.keyword),
  recognitionStatus: normalizeKeyword(queryParams.recognitionStatus),
  batchRecognitionTaskId: queryParams.batchRecognitionTaskId
})

const isDefaultEmptyBrowserRememberedState = (state: DccBrowserRememberedState) => {
  return (
    (state.scope || BROWSER_SEARCH_SCOPE_CURRENT) === BROWSER_SEARCH_SCOPE_CURRENT &&
    !state.directoryId &&
    !state.lastOpenedDirectoryId &&
    (state.pageNo || 1) === 1 &&
    resolveBrowserPageSize(state.pageSize) === DCC_BROWSER_DEFAULT_PAGE_SIZE &&
    !state.categoryId &&
    !normalizeKeyword(state.status) &&
    !normalizeKeyword(state.keyword) &&
    !normalizeKeyword(state.recognitionStatus) &&
    !state.batchRecognitionTaskId
  )
}

const resolveRememberedDirectoryId = (rememberedState?: DccBrowserRememberedState) =>
  rememberedState?.lastOpenedDirectoryId || rememberedState?.directoryId

const buildBrowserRouteQueryFromRememberedState = (state: DccBrowserRememberedState) => {
  const query: Record<string, string> = {
    pageNo: String(state.pageNo || 1),
    pageSize: String(resolveBrowserPageSize(state.pageSize)),
    scope: state.scope || BROWSER_SEARCH_SCOPE_CURRENT
  }
  if (state.directoryId) {
    query.directoryId = String(state.directoryId)
  }
  if (state.categoryId) {
    query.categoryId = String(state.categoryId)
  }
  if (state.status) {
    query.status = state.status
  }
  if (state.recognitionStatus) {
    query.recognitionStatus = state.recognitionStatus
  }
  if (state.batchRecognitionTaskId) {
    query.batchRecognitionTaskId = String(state.batchRecognitionTaskId)
  }
  const keyword = normalizeKeyword(state.keyword)
  if (keyword) {
    query.keyword = keyword
  }
  return query
}

const hasBrowserRouteQuery = () => {
  const browserRouteKeys = [
    'directoryId',
    'pageNo',
    'pageSize',
    'categoryId',
    'status',
    'recognitionStatus',
    'batchRecognitionTaskId',
    'keyword',
    'scope'
  ]
  return browserRouteKeys.some((key) => route.query[key] !== undefined)
}

const buildBrowserRememberedStateFromRoute = (): DccBrowserRememberedState => {
  const routeDirectoryId = parsePositiveNumber(route.query.directoryId)
  return {
    scope: parseBrowserSearchScope(route.query.scope),
    directoryId: routeDirectoryId,
    lastOpenedDirectoryId: routeDirectoryId,
    pageNo: parsePositiveNumber(route.query.pageNo) || 1,
    pageSize: resolveBrowserPageSize(route.query.pageSize),
    categoryId: parsePositiveNumber(route.query.categoryId),
    status: parseQueryString(route.query.status),
    keyword: parseQueryString(route.query.keyword),
    recognitionStatus: parseQueryString(route.query.recognitionStatus),
    batchRecognitionTaskId: parsePositiveNumber(route.query.batchRecognitionTaskId)
  }
}

const applyBrowserRememberedState = (state: DccBrowserRememberedState) => {
  selectedDirectoryId.value = state.directoryId
  selectedDirectory.value = undefined
  queryParams.pageNo = state.pageNo || 1
  queryParams.pageSize = resolveBrowserPageSize(state.pageSize)
  queryParams.categoryId = state.categoryId
  queryParams.status = normalizeKeyword(state.status)
  queryParams.keyword = normalizeKeyword(state.keyword)
  queryParams.recognitionStatus = normalizeKeyword(state.recognitionStatus)
  queryParams.batchRecognitionTaskId = state.batchRecognitionTaskId
  searchScope.value = state.scope || BROWSER_SEARCH_SCOPE_CURRENT
}

const raiseBrowserCacheReadError = (error: unknown): never => {
  message.error('DCC 受控浏览本地缓存读取失败，请检查浏览器本地存储权限。')
  throw error instanceof Error ? error : new Error(String(error || 'DCC browser cache read failed'))
}

const raiseBrowserCacheWriteError = (error: unknown): never => {
  message.error('DCC 受控浏览本地缓存写入失败，请检查浏览器本地存储权限。')
  throw error instanceof Error ? error : new Error(String(error || 'DCC browser cache write failed'))
}

const readBrowserRememberedState = () => {
  try {
    return readDccBrowserRememberedState(getBrowserCacheContext())
  } catch (error) {
    raiseBrowserCacheReadError(error)
  }
}

const mergeBrowserRouteStateWithRememberedDirectory = (
  rememberedState?: DccBrowserRememberedState
) => {
  const rememberedDirectoryId = resolveRememberedDirectoryId(rememberedState)
  const routeState = buildBrowserRememberedStateFromRoute()
  const explicitScope = parseQueryString(route.query.scope)
  if (
    !routeState.directoryId &&
    rememberedDirectoryId &&
    explicitScope !== BROWSER_SEARCH_SCOPE_GLOBAL
  ) {
    routeState.scope = BROWSER_SEARCH_SCOPE_CURRENT
    routeState.directoryId = rememberedDirectoryId
    routeState.lastOpenedDirectoryId = rememberedDirectoryId
  }
  return routeState
}

const mergeBrowserRouteQueryWithRememberedDirectory = (
  rememberedState?: DccBrowserRememberedState
) => buildBrowserRouteQueryFromRememberedState(
  mergeBrowserRouteStateWithRememberedDirectory(rememberedState)
)

const restoreBrowserRouteFromRememberedState = async (
  rememberedState = readBrowserRememberedState()
) => {
  try {
    if (!rememberedState) {
      return false
    }
    const rememberedDirectoryId = resolveRememberedDirectoryId(rememberedState)
    const restoredState: DccBrowserRememberedState = {
      ...rememberedState,
      scope: rememberedDirectoryId ? BROWSER_SEARCH_SCOPE_CURRENT : rememberedState.scope,
      directoryId: rememberedDirectoryId,
      lastOpenedDirectoryId: rememberedDirectoryId
    }
    await withBrowserRouteSyncGuard(() =>
      router.replace({
        path: route.path,
        query: buildBrowserRouteQueryFromRememberedState(restoredState)
      })
    )
    applyBrowserRememberedState(restoredState)
    return true
  } catch (error) {
    raiseBrowserCacheReadError(error)
  }
}

const restoreBrowserInitialRouteState = async () => {
  const rememberedState = readBrowserRememberedState()
  if (hasBrowserRouteQuery()) {
    const mergedRouteState = mergeBrowserRouteStateWithRememberedDirectory(rememberedState)
    const mergedRouteQuery = buildBrowserRouteQueryFromRememberedState(mergedRouteState)
    if (JSON.stringify(mergedRouteQuery) !== JSON.stringify(buildBrowserRouteQueryFromRoute())) {
      await withBrowserRouteSyncGuard(() =>
        router.replace({
          path: route.path,
          query: mergedRouteQuery
        })
      )
    }
    applyBrowserRememberedState(mergedRouteState)
    persistBrowserRememberedState()
    return true
  }
  if (await restoreBrowserRouteFromRememberedState(rememberedState)) {
    persistBrowserRememberedState()
    return true
  }
  syncQueryFromRoute()
  return false
}

const persistBrowserRememberedState = () => {
  try {
    const rememberedState = buildBrowserRememberedState()
    if (isDefaultEmptyBrowserRememberedState(rememberedState)) {
      clearDccBrowserRememberedState(getBrowserCacheContext())
      return
    }
    writeDccBrowserRememberedState(getBrowserCacheContext(), rememberedState)
  } catch (error) {
    raiseBrowserCacheWriteError(error)
  }
}

const clearBrowserRememberedState = () => {
  try {
    clearDccBrowserRememberedState(getBrowserCacheContext())
  } catch (error) {
    raiseBrowserCacheWriteError(error)
  }
}

const restoreBrowserMetadataCache = () => {
  try {
    const metadataCache = readDccBrowserMetadataCache(getBrowserCacheContext())
    if (!metadataCache) {
      return
    }
    if (metadataCache.categories?.length) {
      categories.value = metadataCache.categories
    }
    restoreExpandedDirectoryIdsCacheRecord(metadataCache.expandedDirectoryIds)
    if (metadataCache.directoryChildrenByParentKey) {
      const normalizedCacheRecord = normalizeDirectoryChildrenCacheRecord(
        metadataCache.directoryChildrenByParentKey
      )
      const rootDirectories = buildDirectoryTreeFromCacheRecord(normalizedCacheRecord)
      if (rootDirectories?.length) {
        applyDirectoryTree(rootDirectories)
      }
    }
  } catch (error) {
    raiseBrowserCacheReadError(error)
  }
}

const persistBrowserMetadataCache = () => {
  try {
    writeDccBrowserMetadataCache(getBrowserCacheContext(), {
      categories: categories.value,
      directoryChildrenByParentKey: buildDirectoryChildrenCacheRecord(),
      expandedDirectoryIds: buildExpandedDirectoryIdsCacheRecord()
    })
  } catch (error) {
    raiseBrowserCacheWriteError(error)
  }
}

const getBatchRecognitionStatusText = (status?: string) => {
  switch (status) {
    case BATCH_RECOGNITION_STATUS_WAITING:
      return '等待执行'
    case BATCH_RECOGNITION_STATUS_RUNNING:
      return '识别中'
    case BATCH_RECOGNITION_STATUS_COMPLETED:
      return '已完成'
    case BATCH_RECOGNITION_STATUS_FAILED:
      return '任务失败'
    case BATCH_RECOGNITION_STATUS_STOPPED:
      return '已停止'
    default:
      return '-'
  }
}

const getBatchRecognitionStatusTagType = (status?: string) => {
  switch (status) {
    case BATCH_RECOGNITION_STATUS_WAITING:
      return 'info'
    case BATCH_RECOGNITION_STATUS_RUNNING:
      return 'primary'
    case BATCH_RECOGNITION_STATUS_COMPLETED:
      return 'success'
    case BATCH_RECOGNITION_STATUS_FAILED:
      return 'danger'
    case BATCH_RECOGNITION_STATUS_STOPPED:
      return 'warning'
    default:
      return 'info'
  }
}

const isBatchRecognitionTaskActive = (
  task?: ControlledFileBatchRecognitionTaskRespVO | null
) => {
  return (
    task?.status === BATCH_RECOGNITION_STATUS_WAITING ||
    task?.status === BATCH_RECOGNITION_STATUS_RUNNING
  )
}

const isBatchRecognitionTaskTerminal = (
  task?: ControlledFileBatchRecognitionTaskRespVO | null
) => {
  return (
    task?.status === BATCH_RECOGNITION_STATUS_COMPLETED ||
    task?.status === BATCH_RECOGNITION_STATUS_FAILED ||
    task?.status === BATCH_RECOGNITION_STATUS_STOPPED
  )
}

const toDirectoryNode = (directory: ControlledFileDirectoryVO): ControlledFileDirectoryNode => ({
  ...directory,
  children: directory.children?.map(toDirectoryNode),
  leaf: directory.hasChildren === undefined
    ? !(directory.children?.length)
    : !directory.hasChildren
})

const cacheDirectoryNodes = (nodes: ControlledFileDirectoryNode[]) => {
  nodes.forEach((node) => {
    if (node.id) {
      directoryPathMap.value.set(node.id, node.directoryPath || node.name)
      directoryNodeById.value.set(node.id, node)
    }
    if (node.children?.length) {
      cacheDirectoryNodes(node.children)
    }
  })
}

const cacheDirectoryTreeStructure = (nodes: ControlledFileDirectoryNode[]) => {
  const nextCache = new Map<number | 'root', ControlledFileDirectoryNode[]>()
  nextCache.set('root', nodes)
  const visit = (treeNodes: ControlledFileDirectoryNode[]) => {
    treeNodes.forEach((node) => {
      if (node.id) {
        nextCache.set(node.id, node.children || [])
      }
      if (node.children?.length) {
        visit(node.children)
      }
    })
  }
  visit(nodes)
  directoryChildrenCache.value = nextCache
}

const applyDirectoryTree = (rootDirectories: ControlledFileDirectoryNode[]) => {
  directoryPathMap.value = new Map()
  directoryNodeById.value = new Map()
  cacheDirectoryNodes(rootDirectories)
  cacheDirectoryTreeStructure(rootDirectories)
  directories.value = rootDirectories
}

const findDirectoryNode = (
  nodes: ControlledFileDirectoryNode[],
  directoryId: number
): ControlledFileDirectoryNode | undefined => {
  for (const node of nodes) {
    if (node.id === directoryId) {
      return node
    }
    const child = node.children?.length ? findDirectoryNode(node.children, directoryId) : undefined
    if (child) {
      return child
    }
  }
  return undefined
}

const resolveSelectedDirectory = async () => {
  if (!selectedDirectoryId.value) {
    return
  }
  const mappedDirectory = directoryNodeById.value.get(selectedDirectoryId.value)
  if (mappedDirectory) {
    selectedDirectory.value = mappedDirectory
    return
  }
  const cachedDirectory = findDirectoryNode(directories.value, selectedDirectoryId.value)
  if (cachedDirectory) {
    selectedDirectory.value = cachedDirectory
    return
  }
  const directory = toDirectoryNode(await getDirectory(selectedDirectoryId.value))
  cacheDirectoryNodes([directory])
  selectedDirectory.value = directory
}

const openRememberedDirectoryInTree = async () => {
  await resolveSelectedDirectory()
  const expandedChanged = rememberDirectoryAncestorChain(selectedDirectoryId.value)
  await nextTick()
  syncDirectoryTreeExpandedState()
  if (selectedDirectoryId.value) {
    directoryTreeRef.value?.setCurrentKey(selectedDirectoryId.value)
  } else {
    directoryTreeRef.value?.setCurrentKey(undefined)
  }
  return expandedChanged
}

const rememberDirectoryAncestorChain = (directoryId?: number) => {
  let currentDirectoryId = directoryId
  const visitedDirectoryIds = new Set<number>()
  let changed = false
  while (currentDirectoryId) {
    if (visitedDirectoryIds.has(currentDirectoryId)) {
      throw new Error(`DCC 受控浏览目录祖先链存在循环：${currentDirectoryId}`)
    }
    visitedDirectoryIds.add(currentDirectoryId)
    changed = rememberExpandedDirectoryId(currentDirectoryId) || changed
    const currentDirectory = directoryNodeById.value.get(currentDirectoryId)
    currentDirectoryId = currentDirectory?.parentId || undefined
  }
  return changed
}

const clearSelectedDirectory = () => {
  selectedDirectoryId.value = undefined
  selectedDirectory.value = undefined
  list.value = []
  total.value = 0
}

const parseBrowserSearchScope = (value: unknown): BrowserSearchScope => {
  return parseQueryString(value) === BROWSER_SEARCH_SCOPE_GLOBAL
    ? BROWSER_SEARCH_SCOPE_GLOBAL
    : BROWSER_SEARCH_SCOPE_CURRENT
}

const syncQueryFromRoute = () => {
  clearSelectedDirectory()
  applyBrowserRememberedState(buildBrowserRememberedStateFromRoute())
}

const buildBrowserRouteQuery = () => {
  return buildBrowserRouteQueryFromRememberedState(buildBrowserRememberedState())
}

const buildBrowserRouteQueryFromRoute = () =>
  buildBrowserRouteQueryFromRememberedState(buildBrowserRememberedStateFromRoute())

let browserRouteSyncing = false

async function withBrowserRouteSyncGuard(action: () => Promise<unknown>) {
  browserRouteSyncing = true
  try {
    return await action()
  } finally {
    browserRouteSyncing = false
  }
}

const syncRouteFromBrowserState = async () => {
  const browserRouteQuery = buildBrowserRouteQuery()
  if (JSON.stringify(browserRouteQuery) === JSON.stringify(buildBrowserRouteQueryFromRoute())) {
    return
  }
  await withBrowserRouteSyncGuard(() =>
    router.replace({
      path: route.path,
      query: browserRouteQuery
    })
  )
}

const restoreBrowserDirectoryTreeAndList = async () => {
  const restoredFromQueryOrCache = await restoreBrowserInitialRouteState()
  await loadDirectories()
  await getList()
  if (restoredFromQueryOrCache) {
    persistBrowserRememberedState()
  }
}

const buildBrowserReturnPath = () => {
  const query = new URLSearchParams(buildBrowserRouteQuery())
  const queryString = query.toString()
  return queryString ? `${route.path}?${queryString}` : route.path
}

const getList = async () => {
  if (isCurrentDirectorySearch.value && !selectedDirectoryId.value) {
    list.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const data = await getControlledFileBrowserPage(buildBrowserRequestParams())
    list.value = data.list.map((item) => ({
      ...item,
      selectedVersionId: resolveInitialSelectedVersionId(item)
    }))
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const dccBrowserQuickFilter = useTableQuickFilter(
  'dcc.controlledFile.browser.main',
  dccBrowserQuickFilterDefinitions,
  queryParams,
  getList
)

const loadDirectories = async () => {
  directoryLoading.value = true
  try {
    restoreBrowserMetadataCache()
    if (directories.value.length) {
      if (await openRememberedDirectoryInTree()) {
        persistBrowserMetadataCache()
      }
    }
    const rootDirectories = (await getDirectoryTree()).map(toDirectoryNode)
    applyDirectoryTree(rootDirectories)
    await openRememberedDirectoryInTree()
    persistBrowserMetadataCache()
  } finally {
    directoryLoading.value = false
  }
}

const loadCategories = async () => {
  restoreBrowserMetadataCache()
  categories.value = await getFileCategoryList()
  persistBrowserMetadataCache()
}

const selectDirectoryAndLoad = async (data: ControlledFileDirectoryVO) => {
  if (!data.id) {
    return
  }
  const directory = directoryNodeById.value.get(data.id) || toDirectoryNode(data)
  cacheDirectoryNodes([directory])
  selectedDirectoryId.value = directory.id
  selectedDirectory.value = directory
  searchScope.value = BROWSER_SEARCH_SCOPE_CURRENT
  queryParams.pageNo = 1
  if (rememberDirectoryAncestorChain(directory.id)) {
    persistBrowserMetadataCache()
  }
  await nextTick()
  syncDirectoryTreeExpandedState()
  directoryTreeRef.value?.setCurrentKey(directory.id)
  await syncRouteFromBrowserState()
  await getList()
  persistBrowserRememberedState()
}

const expandCollapsedDirectoryNode = (node: any) => {
  if (!node) {
    throw new Error('目录树节点上下文缺失，无法判断展开状态。')
  }
  if (node.expanded || node.isLeaf) {
    return
  }
  rememberExpandedDirectoryId(node.data?.id)
  node.expand()
}

const handleDirectoryNodeExpand = (data: ControlledFileDirectoryVO) => {
  if (rememberExpandedDirectoryId(data.id)) {
    persistBrowserMetadataCache()
  }
}

const handleDirectoryNodeCollapse = (data: ControlledFileDirectoryVO) => {
  if (forgetExpandedDirectoryId(data.id)) {
    persistBrowserMetadataCache()
  }
}

const isDirectoryExpandIconClick = (event?: MouseEvent) => {
  const target = event?.target
  if (!(target instanceof HTMLElement)) {
    return false
  }
  return Boolean(target.closest('.el-tree-node__expand-icon'))
}

const handleDirectoryClick = async (
  data: ControlledFileDirectoryVO,
  node: any,
  _component?: unknown,
  event?: MouseEvent
) => {
  if (isDirectoryExpandIconClick(event)) {
    return
  }
  expandCollapsedDirectoryNode(node)
  await selectDirectoryAndLoad(data)
}

const handleDirectorySearchSelect = async (data: ControlledFileDirectoryVO) => {
  directoryKeyword.value = ''
  directorySearchResults.value = []
  await selectDirectoryAndLoad(data)
}

watch(directoryKeyword, async (value) => {
  const keyword = normalizeKeyword(value)
  const currentSeq = ++directorySearchSeq
  if (!keyword) {
    directorySearchResults.value = []
    directorySearchLoading.value = false
    return
  }
  directorySearchLoading.value = true
  try {
    const results = (await searchDirectories(keyword, 50)).map(toDirectoryNode)
    cacheDirectoryNodes(results)
    if (currentSeq === directorySearchSeq) {
      directorySearchResults.value = results
    }
  } finally {
    if (currentSeq === directorySearchSeq) {
      directorySearchLoading.value = false
    }
  }
})

watch(
  () => route.fullPath,
  async (nextFullPath, previousFullPath) => {
    if (!previousFullPath || nextFullPath === previousFullPath || browserRouteSyncing) {
      return
    }
    if (route.path !== DCC_BROWSER_ROUTE_PATH) {
      return
    }
    await restoreBrowserDirectoryTreeAndList()
  }
)

const handleQuery = async (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchModelInputEmpty(queryParams, dccBrowserQueryInputFields)) {
    await resetQuery()
    return
  }
  queryParams.pageNo = 1
  await syncRouteFromBrowserState()
  await getList()
  persistBrowserRememberedState()
}

const handleSearchScopeChange = async () => {
  queryParams.pageNo = 1
  await syncRouteFromBrowserState()
  await getList()
  persistBrowserRememberedState()
}

const resetQuery = async () => {
  clearSelectedDirectory()
  queryParams.categoryId = undefined
  queryParams.status = undefined
  queryParams.keyword = undefined
  queryParams.recognitionStatus = undefined
  queryParams.batchRecognitionTaskId = undefined
  searchScope.value = BROWSER_SEARCH_SCOPE_CURRENT
  queryParams.pageSize = DCC_BROWSER_DEFAULT_PAGE_SIZE
  directoryTreeRef.value?.setCurrentKey()
  clearBrowserRememberedState()
  await dccBrowserQuickFilter.resetQuickFilter()
  await syncRouteFromBrowserState()
}

const refreshDirectories = async () => {
  await loadDirectories()
  await syncRouteFromBrowserState()
  persistBrowserRememberedState()
}

const refreshList = async () => {
  await syncRouteFromBrowserState()
  await Promise.all([loadCategories(), getList()])
  persistBrowserRememberedState()
}

const buildBrowserRequestParams = (): ControlledFilePageReqVO => {
  const requestParams: ControlledFilePageReqVO = {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    categoryId: queryParams.categoryId,
    status: queryParams.status,
    keyword: normalizeKeyword(queryParams.keyword),
    recognitionStatus: queryParams.recognitionStatus,
    batchRecognitionTaskId: queryParams.batchRecognitionTaskId,
    quickFilter: queryParams.quickFilter,
    latestVersionOnly: true
  }
  if (isCurrentDirectorySearch.value) {
    requestParams.directoryId = selectedDirectoryId.value
    requestParams.includeDescendantDirectories = false
  }
  return requestParams
}

const openMetadataImportDialog = () => {
  metadataImportVisible.value = true
  metadataImportFileList.value = []
  metadataImportFile.value = null
  metadataImportPreview.value = null
}

const handleMetadataImportFileChange = (uploadFile: any) => {
  metadataImportFile.value = uploadFile.raw || null
  metadataImportPreview.value = null
}

const handleMetadataImportFileRemove = () => {
  metadataImportFile.value = null
  metadataImportPreview.value = null
}

const openRecognitionMigrationImportDialog = () => {
  recognitionMigrationImportVisible.value = true
  recognitionMigrationImportFileList.value = []
  recognitionMigrationImportFile.value = null
  recognitionMigrationImportPreview.value = null
}

const handleRecognitionMigrationImportFileChange = (uploadFile: any) => {
  recognitionMigrationImportFile.value = uploadFile.raw || null
  recognitionMigrationImportPreview.value = null
}

const handleRecognitionMigrationImportFileRemove = () => {
  recognitionMigrationImportFile.value = null
  recognitionMigrationImportPreview.value = null
}

const handleMetadataTemplateDownload = async () => {
  metadataTemplateDownloading.value = true
  try {
    const data = await getControlledFileMetadataImportTemplate()
    download.excel(data, '受控文件基础信息导入模板.xlsx')
  } finally {
    metadataTemplateDownloading.value = false
  }
}

const handleMetadataExport = async () => {
  if (isCurrentDirectorySearch.value && !selectedDirectoryId.value) {
    message.warning('请先选择目录')
    return
  }
  metadataExporting.value = true
  try {
    const data = await exportControlledFileMetadataExcel(buildBrowserRequestParams())
    download.excel(data, '受控文件基础信息.xlsx')
    message.success('文件名/文件编号已导出')
  } finally {
    metadataExporting.value = false
  }
}

const handleRecognitionRecordExport = async () => {
  if (isCurrentDirectorySearch.value && !selectedDirectoryId.value) {
    message.warning('请先选择目录')
    return
  }
  recognitionRecordExporting.value = true
  try {
    const data = await exportControlledFileRecognitionRecordExcel(buildBrowserRequestParams())
    download.excel(data, '受控文件识别记录.xlsx')
    message.success('识别记录已导出')
  } finally {
    recognitionRecordExporting.value = false
  }
}

const handleRecognitionMigrationExport = async () => {
  if (isCurrentDirectorySearch.value && !selectedDirectoryId.value) {
    message.warning('请先选择目录')
    return
  }
  recognitionMigrationExporting.value = true
  try {
    const data = await exportControlledFileRecognitionMigrationExcel(buildBrowserRequestParams())
    download.excel(data, '受控文件识别结果迁移包.xlsx')
    message.success('识别迁移包已导出')
  } finally {
    recognitionMigrationExporting.value = false
  }
}

const handleMetadataImportPreview = async () => {
  if (!metadataImportFile.value) {
    message.error('请选择文件名/文件编号导入文件')
    return
  }
  metadataImportPreviewLoading.value = true
  try {
    metadataImportPreview.value = await previewControlledFileMetadataImport(metadataImportFile.value)
  } finally {
    metadataImportPreviewLoading.value = false
  }
}

const handleMetadataImportConfirm = async () => {
  if (!metadataImportFile.value || !metadataImportPreview.value) {
    return
  }
  metadataImportConfirmLoading.value = true
  try {
    metadataImportPreview.value = await confirmControlledFileMetadataImport(metadataImportFile.value)
    message.success('文件名/文件编号导入完成')
    await refreshList()
  } finally {
    metadataImportConfirmLoading.value = false
  }
}

const handleRecognitionMigrationImportPreview = async () => {
  if (!recognitionMigrationImportFile.value) {
    message.error('请选择识别迁移包')
    return
  }
  recognitionMigrationImportPreviewLoading.value = true
  try {
    recognitionMigrationImportPreview.value = await previewControlledFileRecognitionMigrationImport(recognitionMigrationImportFile.value)
  } finally {
    recognitionMigrationImportPreviewLoading.value = false
  }
}

const handleRecognitionMigrationImportConfirm = async () => {
  if (!recognitionMigrationImportFile.value || !recognitionMigrationImportPreview.value) {
    return
  }
  recognitionMigrationImportConfirmLoading.value = true
  try {
    recognitionMigrationImportPreview.value = await confirmControlledFileRecognitionMigrationImport(recognitionMigrationImportFile.value)
    message.success('识别迁移包导入完成')
    await refreshList()
  } finally {
    recognitionMigrationImportConfirmLoading.value = false
  }
}

const buildBatchRecognitionReq = (): ControlledFileBatchRecognitionCreateReqVO => ({
  recognitionType: 'BASIC_INFO',
  scope: isCurrentDirectorySearch.value
    ? BATCH_RECOGNITION_SCOPE_CURRENT
    : BATCH_RECOGNITION_SCOPE_GLOBAL,
  directoryId: isCurrentDirectorySearch.value ? selectedDirectoryId.value : undefined,
  includeDescendantDirectories: isCurrentDirectorySearch.value,
  keyword: normalizeKeyword(queryParams.keyword),
  status: queryParams.status,
  categoryId: queryParams.categoryId,
  overwriteExisting: batchRecognitionForm.existingRecordPolicy === BATCH_RECOGNITION_POLICY_OVERWRITE_ALL,
  existingRecordPolicy: batchRecognitionForm.existingRecordPolicy,
  syncFileNameTitle: true,
  workerCount: batchRecognitionForm.workerCount
})

const buildFileNumberRecognitionReq = (existingRecordPolicy: string): ControlledFileBatchRecognitionCreateReqVO => ({
  recognitionType: FILE_NUMBER_RECOGNITION_TYPE,
  scope: isCurrentDirectorySearch.value
    ? BATCH_RECOGNITION_SCOPE_CURRENT
    : BATCH_RECOGNITION_SCOPE_GLOBAL,
  directoryId: isCurrentDirectorySearch.value ? selectedDirectoryId.value : undefined,
  includeDescendantDirectories: isCurrentDirectorySearch.value,
  keyword: normalizeKeyword(queryParams.keyword),
  status: queryParams.status,
  categoryId: queryParams.categoryId,
  overwriteExisting: existingRecordPolicy === BATCH_RECOGNITION_POLICY_OVERWRITE_ALL,
  existingRecordPolicy,
  syncFileNameTitle: false,
  workerCount: 1
})

const stopBatchRecognitionPolling = () => {
  if (batchRecognitionPollingTimer) {
    window.clearTimeout(batchRecognitionPollingTimer)
    batchRecognitionPollingTimer = undefined
  }
  batchRecognitionPolling.value = false
  batchRecognitionPollingInFlight = false
}

const loadBatchRecognitionTaskSnapshot = async (taskId: number) => {
  const task = await getControlledFileBatchRecognitionTask(taskId)
  batchRecognitionTask.value = task
  return task
}

const scheduleBatchRecognitionPolling = (taskId: number) => {
  batchRecognitionPollingTimer = window.setTimeout(() => {
    void pollBatchRecognitionTask(taskId)
  }, BATCH_RECOGNITION_POLL_INTERVAL)
}

const handleBatchRecognitionTaskTerminal = async (
  task: ControlledFileBatchRecognitionTaskRespVO
) => {
  if (batchRecognitionTerminalHandled) {
    return
  }
  batchRecognitionTerminalHandled = true
  stopBatchRecognitionPolling()
  try {
    await refreshList()
  } catch (error) {
    console.error(error)
    message.error('批量识别已结束，但刷新列表失败，请手动刷新。')
  }
  if (task.status === BATCH_RECOGNITION_STATUS_FAILED) {
    message.error(task.lastFailureMessage || '批量识别任务失败，请查看最后错误。')
    return
  }
  if (task.totalCount === 0) {
    message.success('批量识别完成：当前筛选条件下没有待识别文件。')
    return
  }
  if (task.failedCount > 0) {
    message.warning(
      `批量识别完成：成功 ${task.successCount}，失败 ${task.failedCount}，总数 ${task.totalCount}。`
    )
    return
  }
  message.success(
    `批量识别完成：成功 ${task.successCount}，失败 ${task.failedCount}，总数 ${task.totalCount}。`
  )
}

const syncBatchRecognitionTask = async (taskId: number) => {
  const task = await loadBatchRecognitionTaskSnapshot(taskId)
  if (isBatchRecognitionTaskTerminal(task)) {
    await handleBatchRecognitionTaskTerminal(task)
  }
  return task
}

const pollBatchRecognitionTask = async (taskId: number) => {
  if (!batchRecognitionPolling.value || batchRecognitionPollingInFlight) {
    return
  }
  batchRecognitionPollingInFlight = true
  try {
    const task = await syncBatchRecognitionTask(taskId)
    if (isBatchRecognitionTaskActive(task)) {
      scheduleBatchRecognitionPolling(taskId)
    }
  } catch (error) {
    stopBatchRecognitionPolling()
    console.error(error)
    message.error('批量识别进度同步失败，请稍后点击按钮重试。')
  } finally {
    batchRecognitionPollingInFlight = false
  }
}

const startBatchRecognitionPolling = (taskId: number) => {
  stopBatchRecognitionPolling()
  batchRecognitionTerminalHandled = false
  batchRecognitionPolling.value = true
  void pollBatchRecognitionTask(taskId)
}

const openBatchRecognitionDialog = () => {
  if (isCurrentDirectorySearch.value && !selectedDirectoryId.value) {
    message.warning('请先选择目录')
    return
  }
  if (isBatchRecognitionTaskActive(batchRecognitionTask.value)) {
    batchRecognitionProgressVisible.value = true
    if (!batchRecognitionPolling.value && batchRecognitionTask.value?.taskId) {
      startBatchRecognitionPolling(batchRecognitionTask.value.taskId)
    }
    return
  }
  batchRecognitionForm.existingRecordPolicy = BATCH_RECOGNITION_POLICY_RETRY_FAILED
  batchRecognitionForm.workerCount = 5
  batchRecognitionConfirmVisible.value = true
}

const openFileNumberRecognitionDialog = async () => {
  if (isCurrentDirectorySearch.value && !selectedDirectoryId.value) {
    message.warning('请先选择目录')
    return
  }
  if (isBatchRecognitionTaskActive(batchRecognitionTask.value)) {
    batchRecognitionProgressVisible.value = true
    if (!batchRecognitionPolling.value && batchRecognitionTask.value?.taskId) {
      startBatchRecognitionPolling(batchRecognitionTask.value.taskId)
    }
    return
  }
  let existingRecordPolicy = BATCH_RECOGNITION_POLICY_OVERWRITE_ALL
  try {
    await ElMessageBox.confirm(
      '将按当前筛选范围批量识别文件编号：文件名去扩展名并 trim 空格后，包含 DCC 项目代码或项目名称即写入对应项目代码，匹配不到清空文件编号。请选择已有文件编号的处理方式。',
      '批量识别文件编号',
      {
        confirmButtonText: '覆盖已有',
        cancelButtonText: '跳过已有',
        distinguishCancelAndClose: true,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        type: 'warning'
      }
    )
  } catch (error) {
    if (error === 'cancel') {
      existingRecordPolicy = BATCH_RECOGNITION_POLICY_SKIP_ALL_EXISTING
    } else {
      return
    }
  }
  await clearBatchRecognitionRecordFilters()
  batchRecognitionCreating.value = true
  try {
    const task = await createControlledFileBatchRecognitionTask(buildFileNumberRecognitionReq(existingRecordPolicy))
    batchRecognitionTask.value = task
    batchRecognitionTerminalHandled = false
    batchRecognitionProgressVisible.value = true
    if (isBatchRecognitionTaskActive(task)) {
      startBatchRecognitionPolling(task.taskId)
      return
    }
    await handleBatchRecognitionTaskTerminal(task)
  } finally {
    batchRecognitionCreating.value = false
  }
}

const showBatchRecognitionRecords = async (recognitionStatus: 'SUCCESS' | 'FAILED') => {
  const taskId = batchRecognitionTask.value?.taskId
  if (!taskId) {
    return
  }
  queryParams.recognitionStatus = recognitionStatus
  queryParams.batchRecognitionTaskId = taskId
  queryParams.pageNo = 1
  batchRecognitionProgressVisible.value = false
  await syncRouteFromBrowserState()
  await getList()
  persistBrowserRememberedState()
  message.success(recognitionStatus === 'SUCCESS' ? '已显示本次识别成功记录' : '已显示本次识别失败记录')
}

const clearBatchRecognitionRecordFilters = async () => {
  if (!queryParams.recognitionStatus && !queryParams.batchRecognitionTaskId) {
    return
  }
  queryParams.recognitionStatus = undefined
  queryParams.batchRecognitionTaskId = undefined
  await syncRouteFromBrowserState()
  persistBrowserRememberedState()
}

const clearBatchRecognitionFiltersForReadonlyUser = async () => {
  if (canEditMetadata.value || (!queryParams.recognitionStatus && !queryParams.batchRecognitionTaskId)) {
    return false
  }
  queryParams.recognitionStatus = undefined
  queryParams.batchRecognitionTaskId = undefined
  await syncRouteFromBrowserState()
  return true
}

const confirmBatchRecognition = async () => {
  if (isCurrentDirectorySearch.value && !selectedDirectoryId.value) {
    message.warning('请先选择目录')
    return
  }
  await clearBatchRecognitionRecordFilters()
  batchRecognitionCreating.value = true
  try {
    const task = await createControlledFileBatchRecognitionTask(buildBatchRecognitionReq())
    batchRecognitionTask.value = task
    batchRecognitionTerminalHandled = false
    batchRecognitionConfirmVisible.value = false
    batchRecognitionProgressVisible.value = true
    if (isBatchRecognitionTaskActive(task)) {
      startBatchRecognitionPolling(task.taskId)
      return
    }
    await handleBatchRecognitionTaskTerminal(task)
  } finally {
    batchRecognitionCreating.value = false
  }
}

const handlePagination = async () => {
  const normalizedPageSize = resolveBrowserPageSize(queryParams.pageSize)
  if (queryParams.pageSize !== normalizedPageSize) {
    queryParams.pageSize = normalizedPageSize
  }
  await syncRouteFromBrowserState()
  await getList()
  persistBrowserRememberedState()
}

const openPreview = (id: number | string) => {
  window.open(buildControlledFileViewerPath(id, 'browser', buildBrowserReturnPath()), '_blank')
}

const openDetail = (id: number | string) => {
  openControlledFileTraceability(router, route, id, 'browser')
}

const openSignatureEvidence = (id: number | string) => {
  openControlledFileTraceability(router, route, id, 'browser')
}

const openControlledPrintDialog = (file: ControlledFileBrowserVersion) => {
  const normalizedId = String(file?.id || '').trim()
  if (!normalizedId) {
    message.error('受控打印缺少文件 ID，无法打开打印申请。')
    return
  }
  const query = new URLSearchParams({
    traceability: '1',
    from: 'browser',
    controlledPrint: '1',
    returnTo: encodeURIComponent(buildBrowserReturnPath())
  })
  router.push(`/dcc/controlled-file/detail/${normalizedId}?${query.toString()}`)
}

const copyFileNumber = async (fileNumber?: string) => {
  const normalizedFileNumber = String(fileNumber || '').trim()
  if (!normalizedFileNumber) {
    throw new Error('文件编号为空，无法复制。')
  }
  try {
    await copyToClipboard(normalizedFileNumber)
    message.success('文件编号已复制')
  } catch (error) {
    message.error('文件编号复制失败，请检查浏览器剪贴板权限或浏览器限制。')
    throw error
  }
}

const openDownload = async (id: number | string) => {
  const normalizedId = Number(id)
  downloadLoadingId.value = Number.isFinite(normalizedId) ? normalizedId : undefined
  try {
    await triggerControlledFileDownload(id)
  } catch (error) {
    message.error(resolveBrowserErrorMessage(error, '下载失败，请查看错误提示后重试。'))
  } finally {
    if (downloadLoadingId.value === normalizedId) {
      downloadLoadingId.value = undefined
    }
  }
}

const openMetadataDialog = async (file: ControlledFileVO) => {
  metadataDialogMounted.value = true
  await import('../shared/ControlledFileMetadataDialog.vue')
  await nextTick()
  metadataEditingFile.value = file
  metadataDialogVisible.value = true
}

const handleMetadataSaved = async () => {
  await getList()
}

const formatMetadataImportAction = (action: string) => {
  const labels: Record<string, string> = {
    UPDATE: '更新',
    UNCHANGED: '不变',
    INVALID: '失败'
  }
  return labels[action] || action
}

const metadataImportActionTagType = (
  action: string
): 'success' | 'warning' | 'info' | 'danger' | undefined => {
  const types: Record<string, 'success' | 'warning' | 'info' | 'danger' | undefined> = {
    UPDATE: 'warning',
    UNCHANGED: undefined,
    INVALID: 'danger'
  }
  return types[action]
}

const formatRecognitionMigrationImportAction = (action: string) => {
  const labels: Record<string, string> = {
    APPLICABLE: '可应用',
    BLOCKED: '不可应用'
  }
  return labels[action] || action
}

const recognitionMigrationImportActionTagType = (
  action: string
): 'success' | 'warning' | 'info' | 'danger' | undefined => {
  const types: Record<string, 'success' | 'warning' | 'info' | 'danger' | undefined> = {
    APPLICABLE: 'success',
    BLOCKED: 'danger'
  }
  return types[action]
}

const formatRecognitionMigrationFileTypes = (
  row: ControlledFileRecognitionMigrationImportRowRespVO
) => {
  return [
    row.fileTypeLevel1,
    row.fileTypeLevel2,
    row.fileTypeLevel3,
    row.fileTypeLevel4,
    row.fileTypeLevel5
  ]
    .filter(Boolean)
    .join(' / ') || '-'
}

onMounted(async () => {
  const restoredFromQueryOrCache = await restoreBrowserInitialRouteState()
  const clearedReadonlyBatchFilters = await clearBatchRecognitionFiltersForReadonlyUser()
  await Promise.all([loadCategories(), loadDirectories()])
  if (queryParams.batchRecognitionTaskId && canEditMetadata.value) {
    const task = await loadBatchRecognitionTaskSnapshot(queryParams.batchRecognitionTaskId)
    if (isBatchRecognitionTaskActive(task)) {
      startBatchRecognitionPolling(task.taskId)
    }
  }
  await getList()
  if (restoredFromQueryOrCache || clearedReadonlyBatchFilters) {
    persistBrowserRememberedState()
  }
})

onBeforeUnmount(() => {
  stopBatchRecognitionPolling()
})
</script>

<style lang="scss" scoped>
.browser-page-layout {
  display: flex;
  height: calc(100vh - 120px);
  min-height: 520px;
  align-items: stretch;
  overflow: hidden;
}

.browser-page-layout > :deep(.el-col) {
  display: flex;
  height: 100%;
  min-height: 0;
}

.browser-page-layout > :deep(.el-col:first-child) {
  flex: 0 0 25%;
  max-width: 25%;
}

.browser-page-layout > :deep(.el-col:last-child) {
  flex: 0 0 75%;
  max-width: 75%;
}

.browser-directory-wrap {
  width: 100%;
  height: 100%;
  margin-bottom: 0 !important;
  overflow: hidden;

  :deep(.el-card__body) {
    display: flex;
    height: 100%;
    min-height: 0;
    flex-direction: column;
  }
}

.browser-list-wrap {
  width: 100%;
  height: 100%;
  margin-bottom: 0 !important;
  overflow: hidden;

  :deep(.el-card__body) {
    display: flex;
    height: 100%;
    min-height: 0;
    flex-direction: column;
  }
}

.browser-list-template {
  display: flex;
  flex: 1 1 auto;
  height: auto;
  min-height: 0;
  flex-direction: column;
}

.browser-list-template :deep(.unified-list-template__table-shell) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.browser-list-template :deep(.unified-list-template__query-form),
.browser-list-template :deep(.el-pagination) {
  flex: 0 0 auto;
}

.browser-list-template :deep(.unified-list-template__table-shell .el-table) {
  height: 100%;
}

.browser-filter-summary {
  flex: 0 0 auto;
  margin-bottom: 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  background: #f8fbff;
  padding: 10px 12px;
}

.browser-filter-summary__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.browser-filter-summary__items {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 8px;
}

.browser-filter-summary__item {
  min-width: 0;
  border-radius: 8px;
  background: #fff;
  padding: 8px;
}

.browser-filter-summary__label {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 16px;
}

.browser-filter-summary__value {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.browser-filter-summary__hint {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.browser-permission-empty-state {
  display: grid;
  gap: 8px;
  padding: 42px 24px;
  text-align: center;
}

.browser-permission-empty-state__title {
  color: #172033;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}

.browser-permission-empty-state__description {
  max-width: 620px;
  margin: 0 auto;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
}

.browser-directory-scroll {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 2px;
}

.browser-directory-search {
  margin-bottom: 12px;
}

.browser-directory-search__list {
  min-height: 72px;
  padding: 6px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fafcff;
}

.browser-directory-search__item {
  display: flex;
  width: 100%;
  min-width: 0;
  padding: 8px;
  border: 0;
  border-radius: 4px;
  margin: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  flex-direction: column;
  gap: 2px;
  text-align: left;
}

.browser-directory-search__item:hover {
  background: #edf4ff;
}

.browser-directory-search__name,
.browser-directory-search__path {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.browser-directory-search__name {
  color: #172033;
  font-size: 13px;
  font-weight: 500;
  line-height: 18px;
}

.browser-directory-search__path {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 16px;
}

.browser-directory-node {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-right: 8px;
}

.browser-directory-node__name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.browser-search-input {
  width: 260px;
}

.browser-file-name-wrapper {
  display: inline-flex;
  max-width: 100%;
  min-width: 0;
}

.browser-file-name {
  display: inline-flex;
  max-width: 100%;
  min-width: 0;
  padding: 0;
  align-items: flex-start;
  white-space: normal;
  text-align: left;
  line-height: 18px;
}

.browser-file-name__text {
  display: -webkit-box;
  max-height: 36px;
  overflow: hidden;
  line-height: 18px;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.browser-current-active-row-summary {
  display: flex;
  max-width: 100%;
  flex-wrap: wrap;
  gap: 4px 8px;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.browser-current-active-row-summary span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.browser-file-number-cell {
  display: flex;
  max-width: 100%;
  min-width: 0;
  align-items: center;
  gap: 4px;
}

.browser-file-number {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.browser-file-number--link {
  min-width: 0;
}

.browser-file-number-copy {
  flex: 0 0 auto;
  padding: 0 2px;
}

.browser-version-summary {
  min-width: 0;
}

.browser-version-summary__main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.browser-version-summary__select {
  width: 108px;
  flex: 0 0 108px;
}

.browser-version-summary__tags {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 4px;
}

.browser-version-summary__dates {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 4px 10px;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.browser-row-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 4px 8px;
}

.browser-row-actions__empty {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  line-height: 22px;
}

.browser-advanced-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.browser-advanced-actions :deep(.el-button) {
  width: 100%;
  margin-left: 0;
  justify-content: center;
}

.browser-list-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.metadata-import-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
}

.metadata-import-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.browser-list-title {
  color: #172033;
  font-size: 18px;
  font-weight: 600;
  line-height: 26px;
}

.browser-list-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.batch-recognition-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.batch-recognition-dialog__section {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.batch-recognition-dialog__title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.batch-recognition-dialog__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.batch-recognition-dialog__item {
  display: flex;
  min-width: 0;
  gap: 8px;
}

.batch-recognition-dialog__item--full {
  grid-column: 1 / -1;
}

.batch-recognition-dialog__label {
  color: #4b5563;
  font-size: 12px;
  line-height: 20px;
  flex: 0 0 auto;
}

.batch-recognition-dialog__value {
  min-width: 0;
  color: #172033;
  font-size: 13px;
  line-height: 20px;
  word-break: break-all;
}

.batch-recognition-dialog__hint {
  margin-top: 8px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.batch-recognition-dialog__policy-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.batch-recognition-progress {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.batch-recognition-progress__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.batch-recognition-progress__context {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 6px;
}

.batch-recognition-progress__context-line {
  display: flex;
  min-width: 0;
  gap: 8px;
}

.batch-recognition-progress__context-label,
.batch-recognition-progress__label {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
  flex: 0 0 auto;
}

.batch-recognition-progress__context-value,
.batch-recognition-progress__value {
  min-width: 0;
  color: #172033;
  font-size: 13px;
  line-height: 18px;
  word-break: break-all;
}

.batch-recognition-progress__bar {
  padding: 0 2px;
}

.batch-recognition-progress__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.batch-recognition-progress__item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #fff;
}

.batch-recognition-progress__item--action {
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.batch-recognition-progress__item--action:hover:not(:disabled) {
  border-color: var(--el-color-primary);
  box-shadow: 0 6px 18px rgb(40 85 140 / 10%);
}

.batch-recognition-progress__item--action:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.batch-recognition-progress__item--full {
  grid-column: 1 / -1;
}

.batch-recognition-progress__value {
  font-variant-numeric: tabular-nums;
}

.batch-recognition-progress__value--error {
  color: #c45656;
}

.browser-extension-blacklist {
  display: grid;
  gap: 10px;
}

.browser-extension-blacklist__hint {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .batch-recognition-dialog__grid,
  .batch-recognition-progress__grid {
    grid-template-columns: 1fr;
  }

  .batch-recognition-progress__header {
    flex-direction: column;
  }
}
</style>
