<template>
  <ContentWrap>
    <div class="edhr-batch-page">
      <EdhrBatchRecordTabs active-tab="execution" />
      <UnifiedListTemplate
        class="edhr-batch-page__list-template"
        table-key="mes.pro.edhrBatch.execution.main"
        :query-model="queryParams"
        :filter-definitions="edhrBatchQuickFilterDefinitions"
        :show-quick-filter-label="false"
        :quick-filter-state="edhrBatchQuickFilter.state"
        :selected-filter-definition="edhrBatchQuickFilter.selectedDefinition.value"
        :operator-options="edhrBatchQuickFilter.operatorOptions.value"
        :columns="edhrBatchExecutionColumns"
        :column-saving="edhrBatchExecutionColumnSaving"
        :show-column-settings="false"
        :show-column-reset="false"
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @update:quick-filter-state="edhrBatchQuickFilter.updateState"
        @quick-filter-query="edhrBatchQuickFilter.applyQuickFilter"
        @column-change="saveEdhrBatchExecutionColumnConfig"
        @column-reset="resetEdhrBatchExecutionColumnConfig"
        @pagination="getList"
      >
        <template #actions>
          <UserTableColumnSettings
            class="edhr-batch-page__column-settings"
            :columns="edhrBatchExecutionColumns"
            :saving="edhrBatchExecutionColumnSaving"
            :show-reset="false"
            @change="saveEdhrBatchExecutionColumnConfig"
            @reset="resetEdhrBatchExecutionColumnConfig"
          />
          <el-button
            v-hasPermi="['mes:pro-edhr-batch-execution:create']"
            type="primary"
            @click="openCreateDialog"
          >
            打开/创建
          </el-button>
          <el-button
            v-if="hasGoldenFingerPermission"
            v-hasPermi="[GOLDEN_FINGER_PERMISSION]"
            plain
            type="primary"
            :disabled="!selectableGoldenFingerBulkVoidCurrentPageCount"
            :loading="goldenFingerBulkVoidLoading"
            aria-label="批量作废"
            @click="openGoldenFingerBulkVoidDialog"
          >
            批量作废
          </el-button>
          <el-tag
            v-if="hasGoldenFingerPermission && selectedGoldenFingerBulkVoidIds.length"
            type="danger"
            effect="plain"
          >
            已勾选 {{ selectedGoldenFingerBulkVoidIds.length }} 个批次
          </el-tag>
        </template>

        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
          <el-alert
            v-if="loadError"
            :title="loadError"
            type="error"
            :closable="false"
            show-icon
            class="edhr-batch-page__list-alert"
          />
          <el-table
            v-loading="loading"
            data-user-table-column-explicit
            data-user-table-key="mes.pro.edhrBatch.execution.main"
            :data="list"
            row-key="id"
            stripe
            border
            :show-overflow-tooltip="true"
            @header-dragend="handleEdhrBatchExecutionHeaderDragend"
            @selection-change="handleBatchExecutionSelectionChange"
            @sort-change="handleTemplateSortChange"
          >
            <el-table-column
              v-if="hasGoldenFingerPermission"
              type="selection"
              :selectable="isGoldenFingerBulkVoidSelectableRow"
              width="48"
              fixed="left"
            />
            <el-table-column
              v-if="isEdhrBatchExecutionColumnVisible('batchExecutionCode')"
              label="批次执行编码"
              prop="batchExecutionCode"
              :min-width="getEdhrBatchExecutionColumnMinWidthString('batchExecutionCode', 190)"
              v-bind="sortColumnAttrs('batchExecutionCode')"
            >
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">{{ row.batchExecutionCode || row.id }}</el-button>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isEdhrBatchExecutionColumnVisible('workOrderCode')"
              label="工单"
              prop="workOrderCode"
              :min-width="getEdhrBatchExecutionColumnMinWidthString('workOrderCode', 150)"
              v-bind="sortColumnAttrs('workOrderCode')"
            >
              <template #default="{ row }">
                <el-button v-if="row.workOrderCode" link type="primary" @click="openDetail(row)">
                  {{ row.workOrderCode }}
                </el-button>
                <span v-else>--</span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isEdhrBatchExecutionColumnVisible('currentProcess')"
              label="当前工序"
              prop="currentProcess"
              :min-width="getEdhrBatchExecutionColumnMinWidthString('currentProcess', 140)"
              v-bind="sortColumnAttrs('currentProcess')"
            >
              <template #default="{ row }">
                <el-button
                  v-if="row.currentProcessName || row.currentProcessCode"
                  link
                  type="primary"
                  @click="openDetail(row, 'process')"
                >
                  {{ row.currentProcessName || row.currentProcessCode }}
                </el-button>
                <span v-else>--</span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isEdhrBatchExecutionColumnVisible('currentFillers')"
              label="当前填写人"
              prop="currentFillers"
              :min-width="getEdhrBatchExecutionColumnMinWidthString('currentFillers', 220)"
              v-bind="sortColumnAttrs('currentFillers')"
            >
              <template #default="{ row }">
                <div class="edhr-batch-page__filler-cell">
                  {{ resolveCurrentProcessFillerNames(row) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isEdhrBatchExecutionColumnVisible('product')"
              label="产品"
              prop="product"
              :min-width="getEdhrBatchExecutionColumnMinWidthString('product', 170)"
              v-bind="sortColumnAttrs('product')"
            >
              <template #default="{ row }">{{ row.productName || row.productCode || '--' }}</template>
            </el-table-column>
            <el-table-column
              v-if="isEdhrBatchExecutionColumnVisible('route')"
              label="路线"
              prop="route"
              :min-width="getEdhrBatchExecutionColumnMinWidthString('route', 170)"
              v-bind="sortColumnAttrs('route')"
            >
              <template #default="{ row }">
                <el-button v-if="row.routeName || row.routeCode" link type="primary" @click="openDetail(row)">
                  {{ row.routeName || row.routeCode }}
                </el-button>
                <span v-else>--</span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isEdhrBatchExecutionColumnVisible('status')"
              label="状态"
              prop="status"
              :width="getEdhrBatchExecutionColumnWidthString('status', 110)"
              align="center"
              v-bind="sortColumnAttrs('status')"
            >
              <template #default="{ row }">
                <div class="edhr-batch-page__stage-cell">
                  <el-tag :type="resolveBatchStatusType(row.status)">{{ resolveBatchStatusLabel(row.status) }}</el-tag>
                  <el-tag v-if="isPendingVoidBatch(row)" type="warning">作废申请中</el-tag>
                  <div class="edhr-batch-page__muted">{{ row.mainStageLabel || resolveBatchMainStageLabel(row) }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isEdhrBatchExecutionColumnVisible('progress')"
              label="完成进度"
              prop="progress"
              :width="getEdhrBatchExecutionColumnWidthString('progress', 150)"
              v-bind="sortColumnAttrs('progress')"
            >
              <template #default="{ row }">
                <div class="edhr-batch-page__progress">
                  <span>{{ resolveBatchRequiredProgress(row) }}%</span>
                  <el-progress :percentage="resolveBatchRequiredProgress(row)" :show-text="false" :stroke-width="6" />
                </div>
              </template>
            </el-table-column>
            <el-table-column v-if="isEdhrBatchExecutionColumnVisible('blockedCount')" label="阻塞数" prop="blockedCount" :width="getEdhrBatchExecutionColumnWidthString('blockedCount', 90)" align="center" v-bind="sortColumnAttrs('blockedCount')" />
            <el-table-column v-if="isEdhrBatchExecutionColumnVisible('updateTime')" label="最后更新时间" prop="updateTime" :width="getEdhrBatchExecutionColumnWidthString('updateTime', 180)" :formatter="edhrDateTimeFormatter" v-bind="sortColumnAttrs('updateTime')" />
            <el-table-column v-if="isEdhrBatchExecutionColumnVisible('operation')" label="操作" prop="operation" :width="getEdhrBatchExecutionColumnWidthString('operation', 180)" fixed="right">
              <template #default="{ row }">
                <div
                  v-if="resolveBatchVoidOperationState(row) === 'pending-withdrawable'"
                  class="edhr-batch-page__actions"
                >
                  <el-button
                    v-hasPermi="['mes:pro-edhr-change:void']"
                    link
                    type="warning"
                    @click="handleWithdrawVoidRequest(row)"
                  >
                    撤回作废申请
                  </el-button>
                </div>
                <div
                  v-else-if="resolveBatchVoidOperationState(row) === 'pending-readonly'"
                  class="edhr-batch-page__actions"
                >
                  <span class="edhr-batch-page__muted">作废申请中</span>
                </div>
                <div
                  v-else-if="resolveBatchVoidOperationState(row) === 'voided'"
                  class="edhr-batch-page__actions"
                >
                  <el-button link type="primary" @click="openDetail(row)">编辑</el-button>
                </div>
                <div
                  v-else-if="resolveBatchVoidOperationState(row) === 'release-locked'"
                  class="edhr-batch-page__actions"
                >
                  <el-button link type="primary" @click="openDetail(row)">编辑</el-button>
                </div>
                <div v-else class="edhr-batch-page__actions">
                  <el-button link type="primary" @click="openDetail(row)">编辑</el-button>
                  <el-button
                    v-hasPermi="['mes:pro-edhr-change:void']"
                    link
                    type="danger"
                    @click="openVoidDialog(row)"
                  >
                    作废
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </UnifiedListTemplate>
    </div>

    <Dialog title="打开或创建 eDHR 批次执行" v-model="createDialogVisible" width="520px">
      <el-alert
        v-if="createError"
        :title="createError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-page__dialog-alert"
      />
      <el-form label-width="96px">
        <el-form-item label="生产工单" required>
          <el-select
            v-model="createForm.workOrderId"
            filterable
            remote
            reserve-keyword
            clearable
            :remote-method="searchSelectableWorkOrders"
            :loading="workOrderLoading"
            popper-class="edhr-batch-page__work-order-select-popper"
            placeholder="输入工单号或产品名称搜索并选择未冻结工单"
            style="width: 100%"
            @change="handleWorkOrderChange"
            @clear="handleWorkOrderClear"
          >
            <el-option
              v-for="workOrder in selectableWorkOrders"
              :key="workOrder.id"
              :label="resolveWorkOrderOptionLabel(workOrder)"
              :value="workOrder.id"
            >
              <div class="edhr-batch-page__work-order-option">
                <div>
                  <div class="edhr-batch-page__work-order-code">{{ workOrder.code || '--' }}</div>
                  <div class="edhr-batch-page__muted">{{ workOrder.name || '--' }}</div>
                </div>
                <div class="edhr-batch-page__work-order-meta">
                  <span>{{ workOrder.productName || workOrder.productCode || '未维护产品' }}</span>
                  <span>{{ workOrder.batchCode || '未维护批次' }}</span>
                  <span>ID {{ workOrder.id }}</span>
                </div>
              </div>
            </el-option>
          </el-select>
          <div class="edhr-batch-page__field-hint">
            仅显示未取消且未临时冻结的生产工单。
          </div>
        </el-form-item>
        <el-form-item label="工艺路线" required>
          <el-select
            v-model="createForm.routeId"
            clearable
            :disabled="!createForm.workOrderId || createRouteOptionsLoading || !createRouteOptions.length"
            :loading="createRouteOptionsLoading"
            popper-class="edhr-batch-page__work-order-select-popper"
            placeholder="请先选择工单，再选择该产品绑定的工艺路线"
            style="width: 100%"
          >
            <el-option
              v-for="routeOption in createRouteOptions"
              :key="routeOption.routeId"
              :label="resolveRouteOptionLabel(routeOption)"
              :value="routeOption.routeId"
            >
              <div class="edhr-batch-page__work-order-option">
                <div>
                  <div class="edhr-batch-page__work-order-code">{{ routeOption.routeCode || '--' }}</div>
                  <div class="edhr-batch-page__muted">{{ routeOption.routeName || '--' }}</div>
                </div>
                <div class="edhr-batch-page__work-order-meta">
                  <span>ID {{ routeOption.routeId }}</span>
                  <span>{{ routeOption.batchRouteEnabled ? '批记录流程已启用' : '批记录流程未启用' }}</span>
                </div>
              </div>
            </el-option>
          </el-select>
          <div class="edhr-batch-page__field-hint">
            多条路线时必须明确选择，避免同一工单按错误路线创建批次。
          </div>
        </el-form-item>
        <el-form-item label="批次号" required>
          <el-input v-model="createForm.batchCode" placeholder="请输入真实批次号" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitOpenOrCreate">确 认</el-button>
      </template>
    </Dialog>

    <Dialog title="作废批次执行" v-model="voidDialogVisible" width="560px">
      <el-alert
        v-if="voidError"
        :title="voidError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-page__dialog-alert"
      />
      <el-descriptions v-if="selectedVoidBatch" :column="1" border class="edhr-batch-page__void-summary">
        <el-descriptions-item label="批次执行编码">
          {{ selectedVoidBatchCode }}
        </el-descriptions-item>
        <el-descriptions-item label="工单号">
          {{ selectedVoidWorkOrderCode }}
        </el-descriptions-item>
        <el-descriptions-item label="批次号">
          {{ selectedVoidBatch.batchCode || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          {{ resolveBatchStatusLabel(selectedVoidBatch.status) }}
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="104px" class="edhr-batch-page__void-form">
        <el-form-item label="原因分类" required>
          <el-select v-model="voidForm.reasonCategory" placeholder="请选择作废原因分类" style="width: 100%">
            <el-option label="订单取消" value="ORDER_CANCELLED" />
            <el-option label="数据错误" value="DATA_ERROR" />
            <el-option label="流程偏差" value="PROCESS_DEVIATION" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因说明" required>
          <el-input v-model="voidForm.reasonText" type="textarea" :rows="3" placeholder="请填写作废原因" />
        </el-form-item>
        <el-form-item label="电子签名密码" required>
          <el-input
            v-model="voidForm.password"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请输入电子签名密码"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="voidForm.comment" type="textarea" :rows="2" placeholder="可填写补充说明" />
        </el-form-item>
        <el-form-item
          v-for="task in voidStartUserSelectTasks"
          :key="task.id"
          :label="`${task.name}审批人`"
        >
          <UserSelectV2
            v-model="voidStartUserSelectAssignees[task.id]"
            multiple
            :placeholder="`请选择${task.name}审批人`"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="voidDialogVisible = false">取 消</el-button>
        <el-button type="danger" :loading="voidLoading" @click="submitVoidBatchExecution">提交作废流程</el-button>
      </template>
    </Dialog>

    <Dialog title="批量作废批次执行" v-model="goldenFingerBulkVoidDialogVisible" width="620px">
      <el-alert
        title="可先在表格复选批次或使用表头全选；未勾选时，将按当前筛选条件跨页作废所有可作废批次，直通生效，不进入审核流程。"
        type="warning"
        :closable="false"
        show-icon
        class="mb-12px"
      />
      <el-alert
        v-if="goldenFingerBulkVoidError"
        :title="goldenFingerBulkVoidError"
        type="error"
        :closable="false"
        show-icon
        class="mb-12px"
      />
      <el-form label-width="112px" class="edhr-batch-page__void-form">
        <el-form-item label="作废范围">
          <span v-if="selectedGoldenFingerBulkVoidIds.length">
            已勾选 {{ selectedGoldenFingerBulkVoidIds.length }} 个批次
          </span>
          <span v-else>当前筛选条件下跨页全部可作废批次</span>
        </el-form-item>
        <el-form-item label="原因分类" required>
          <el-select
            v-model="goldenFingerBulkVoidForm.reasonCategory"
            placeholder="请选择作废原因分类"
            style="width: 100%"
          >
            <el-option label="订单取消" value="ORDER_CANCELLED" />
            <el-option label="数据错误" value="DATA_ERROR" />
            <el-option label="流程偏差" value="PROCESS_DEVIATION" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因说明" required>
          <el-input
            v-model="goldenFingerBulkVoidForm.reasonText"
            type="textarea"
            :rows="3"
            placeholder="请填写批量作废原因"
          />
        </el-form-item>
        <el-form-item label="电子签名密码" required>
          <el-input
            v-model="goldenFingerBulkVoidForm.password"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请输入电子签名密码"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="goldenFingerBulkVoidForm.comment"
            type="textarea"
            :rows="2"
            placeholder="可填写补充说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="goldenFingerBulkVoidDialogVisible = false">取 消</el-button>
        <el-button type="danger" :loading="goldenFingerBulkVoidLoading" @click="submitGoldenFingerBulkVoid">
          确认一键作废
        </el-button>
      </template>
    </Dialog>
    <Dialog title="eDHR 演练预检" v-model="readinessDialogVisible" width="980px">
      <el-alert
        v-if="readinessError"
        :title="readinessError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-page__dialog-alert"
      />
      <el-form :inline="true" :model="readinessForm" class="edhr-batch-page__readiness-form">
        <el-form-item label="路线ID" required>
          <el-input v-model="readinessForm.routeId" placeholder="如 900022" class="!w-130px" />
        </el-form-item>
        <el-form-item label="执行人" required>
          <el-select
            v-model="readinessForm.executorUserId"
            :filterable="true"
            clearable
            :loading="readinessUserLoading"
            placeholder="选择执行人"
            class="!w-190px"
          >
            <el-option
              v-for="user in readinessUserOptions"
              :key="user.id"
              :label="resolveReadinessUserLabel(user)"
              :value="String(user.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="审批人" required>
          <el-select
            v-model="readinessForm.approverUserId"
            :filterable="true"
            clearable
            :loading="readinessUserLoading"
            placeholder="选择审批人"
            class="!w-190px"
          >
            <el-option
              v-for="user in readinessUserOptions"
              :key="user.id"
              :label="resolveReadinessUserLabel(user)"
              :value="String(user.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="归档员" required>
          <el-select
            v-model="readinessForm.archiverUserId"
            :filterable="true"
            clearable
            :loading="readinessUserLoading"
            placeholder="选择归档员"
            class="!w-190px"
          >
            <el-option
              v-for="user in readinessUserOptions"
              :key="user.id"
              :label="resolveReadinessUserLabel(user)"
              :value="String(user.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="readinessLoading" @click="submitReadinessCheck">
            开始预检
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="readinessResult" class="edhr-batch-page__readiness-summary">
        <div class="edhr-batch-page__readiness-status">
          <span class="edhr-batch-page__readiness-label">预检状态</span>
          <el-tag :type="readinessResult.overallStatus === 'PASS' ? 'success' : 'danger'">
            {{ readinessResult.overallStatus || '--' }}
          </el-tag>
        </div>
        <div class="edhr-batch-page__readiness-counts">
          <span>通过 {{ readinessPassCount }}</span>
          <span>阻塞 {{ readinessBlockerCount }}</span>
        </div>
      </div>

      <el-table
        v-if="readinessResult"
        :data="readinessResult.items || []"
        class="edhr-batch-page__readiness-table"
        empty-text="暂无预检项"
        :show-overflow-tooltip="true"
      >
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PASS' ? 'success' : 'danger'">{{ row.status || '--' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="代码" prop="code" min-width="210" />
        <el-table-column label="责任" width="110">
          <template #default="{ row }">{{ resolveReadinessRoleLabel(row.roleKey) }}</template>
        </el-table-column>
        <el-table-column label="对象" prop="subjectId" width="120" />
        <el-table-column label="业务动作" min-width="210">
          <template #default="{ row }">
            <div class="edhr-batch-page__business-action">
              <el-tag size="small" type="warning">{{ resolveReadinessBusinessGroup(row) }}</el-tag>
              <span>{{ resolveReadinessBusinessAction(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="message" min-width="260" />
        <el-table-column label="下一步" min-width="320">
          <template #default="{ row }">
            <div class="edhr-batch-page__next-step">
              <div>{{ resolveReadinessBusinessNextStep(row) }}</div>
              <div v-if="row.suggestion" class="edhr-batch-page__next-step-detail">
                {{ row.suggestion }}
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="edhr-batch-page__field-hint">
        预检只读取前置条件，不会修改角色、签名、BPM、路线或模板数据。
      </div>

      <template #footer>
        <el-button @click="readinessDialogVisible = false">关 闭</el-button>
      </template>
    </Dialog>

    <Dialog title="批次最终归档" v-model="archiveDialogVisible" width="620px">
      <el-alert
        v-if="archiveError"
        :title="archiveError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-page__dialog-alert"
      />
      <el-descriptions v-if="archivePreview" :column="1" border>
        <el-descriptions-item label="归档版本">V{{ archivePreview.archiveVersion || '--' }}</el-descriptions-item>
        <el-descriptions-item label="归档类型">{{ archivePreview.artifactType || '--' }}</el-descriptions-item>
        <el-descriptions-item label="归档状态">{{ archivePreview.archiveStatus || '--' }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ archivePreview.fileName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ archivePreview.fileSize || '--' }}</el-descriptions-item>
        <el-descriptions-item label="哈希">{{ archivePreview.contentHash || '--' }}</el-descriptions-item>
        <el-descriptions-item label="生成时间">{{ formatEdhrDateTime(archivePreview.generatedAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="archiveDialogVisible = false">关 闭</el-button>
        <el-button
          v-if="archivePreview"
          v-hasPermi="['mes:pro-edhr-batch-execution-archive:download']"
          type="primary"
          @click="handleDownloadArchiveByPreview"
        >
          下载打印版 PDF
        </el-button>
      </template>
    </Dialog>

    <Dialog title="批次流程追踪" v-model="batchFlowTraceDialogVisible" width="820px">
      <el-alert
        v-if="batchFlowTraceError"
        :title="batchFlowTraceError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-page__dialog-alert"
      />
      <div v-loading="batchFlowTraceLoading" class="edhr-batch-page__trace-panel">
        <div class="edhr-batch-page__trace-title">
          {{ selectedTraceBatch?.batchExecutionCode || selectedTraceBatch?.batchCode || '当前批次' }}
        </div>
        <el-timeline>
          <el-timeline-item
            v-for="item in batchFlowTraceItems"
            :key="item.key"
            :type="item.type"
            :timestamp="item.time"
          >
            <div class="edhr-batch-page__trace-item">
              <strong>{{ item.label }}</strong>
              <span>{{ item.description }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </Dialog>

    <Dialog title="批次操作轨迹 / 历史记录" v-model="operationHistoryDialogVisible" width="980px">
      <el-alert
        v-if="batchFlowTraceError"
        :title="batchFlowTraceError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-page__dialog-alert"
      />
      <el-table
        v-loading="batchFlowTraceLoading"
        :data="operationHistoryRows"
        empty-text="暂无操作轨迹"
        :show-overflow-tooltip="true"
      >
        <el-table-column label="类型" prop="type" width="120" />
        <el-table-column label="对象" prop="subject" min-width="180" />
        <el-table-column label="状态/动作" prop="action" min-width="180" />
        <el-table-column label="时间" prop="time" width="180" />
        <el-table-column label="说明" prop="description" min-width="260" />
      </el-table>
    </Dialog>

    <Dialog title="批次追溯" v-model="traceActionDialogVisible" width="680px">
      <div class="edhr-batch-page__trace-action-head">
        <div class="edhr-batch-page__trace-title">
          {{ selectedTraceBatch?.batchExecutionCode || selectedTraceBatch?.batchCode || '当前批次' }}
        </div>
        <div class="edhr-batch-page__field-hint">
          追溯入口聚合流程、操作和归档证据，避免列表行展示过多次级按钮。
        </div>
      </div>
      <div class="edhr-batch-page__trace-actions">
        <el-button type="primary" plain @click="selectedTraceBatch && openFlowTraceDialog(selectedTraceBatch)">
          流程追踪
        </el-button>
        <el-button type="primary" plain @click="selectedTraceBatch && openOperationHistoryDialog(selectedTraceBatch)">
          操作轨迹
        </el-button>
        <el-button type="primary" plain @click="selectedTraceBatch && handleViewArchive(selectedTraceBatch)">
          查看归档
        </el-button>
      </div>
    </Dialog>

  </ContentWrap>
</template>

<script setup lang="ts">
import {
  EDHR_BATCH_STATUS_ARCHIVED,
  EDHR_BATCH_STATUS_CLOSED,
  EDHR_BATCH_STATUS_CREATED,
  EDHR_BATCH_STATUS_FROZEN,
  EDHR_BATCH_STATUS_IN_PROGRESS,
  EDHR_BATCH_STATUS_READY_TO_CLOSE,
  EDHR_BATCH_STATUS_REWORK_REQUIRED,
  EDHR_BATCH_STATUS_REJECTED,
  EDHR_BATCH_STATUS_VOIDED,
  goldenFingerBulkVoidEdhrBatchExecutions,
  downloadEdhrBatchArchive,
  getEdhrRehearsalReadiness,
  getEdhrBatchReviewTimeline,
  getEdhrBatchExecutionRouteOptions,
  getLatestEdhrBatchArchive,
  getEdhrBatchExecutionPage,
  openOrCreateManualEdhrBatchExecution,
  type EdhrBatchExecutionArchiveRespVO,
  type EdhrBatchExecutionPageReqVO,
  type EdhrBatchExecutionRespVO,
  type EdhrBatchExecutionRouteOptionRespVO,
  type EdhrBatchReviewTimelineRespVO,
  type EdhrRehearsalReadinessItem,
  type EdhrRehearsalReadinessResult
} from '@/api/mes/pro/edhr/batchExecution'
import { ProWorkOrderApi, type ProWorkOrderVO } from '@/api/mes/pro/workorder'
import * as UserApi from '@/api/system/user'
import * as DefinitionApi from '@/api/bpm/definition'
import * as ProcessInstanceApi from '@/api/bpm/processInstance'
import { requestVoidBatchExecution, resolveVoidBatchExecutionApproval } from '@/api/mes/pro/edhr/change'
import { CandidateStrategy, NodeId } from '@/components/SimpleProcessDesignerV2/src/consts'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { MesProWorkOrderStatusEnum } from '@/views/mes/utils/constants'
import { resolveBatchRequiredProgress } from './progress'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'
import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import { generateUUID } from '@/utils'
import { useUserStore } from '@/store/modules/user'
import { edhrDateTimeFormatter, formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProEdhrBatchExecutionListPage' })

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()
const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'
const hasGoldenFingerPermission = computed(() => userStore.permissions.has(GOLDEN_FINGER_PERMISSION))
const hasGoldenFingerActionBypass = computed(() => hasGoldenFingerPermission.value)
type EdhrBatchExecutionDetailFocus = 'process'
const EDHR_BATCH_EXECUTION_TRACE_ONLY_STATUSES = [
  EDHR_BATCH_STATUS_ARCHIVED,
  EDHR_BATCH_STATUS_REJECTED
] as const
type BatchVoidOperationState =
  | 'normal'
  | 'pending-withdrawable'
  | 'pending-readonly'
  | 'voided'
  | 'release-locked'
const edhrBatchExecutionDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'batchExecutionCode', label: '批次执行编码', minWidth: 190 },
  { key: 'workOrderCode', label: '工单', minWidth: 150 },
  { key: 'currentProcess', label: '当前工序', minWidth: 140 },
  { key: 'currentFillers', label: '当前填写人', minWidth: 220 },
  { key: 'product', label: '产品', minWidth: 170 },
  { key: 'route', label: '路线', minWidth: 170 },
  { key: 'status', label: '状态', width: 110 },
  { key: 'progress', label: '完成进度', width: 150 },
  { key: 'blockedCount', label: '阻塞数', width: 90 },
  { key: 'updateTime', label: '最后更新时间', width: 180 },
  { key: 'operation', label: '操作', width: 180, hideable: false, business: false }
]
const {
  columns: edhrBatchExecutionColumns,
  saving: edhrBatchExecutionColumnSaving,
  isColumnVisible: isEdhrBatchExecutionColumnVisible,
  getColumnWidthString: getEdhrBatchExecutionColumnWidthString,
  getColumnMinWidthString: getEdhrBatchExecutionColumnMinWidthString,
  handleHeaderDragend: handleEdhrBatchExecutionHeaderDragend,
  saveConfig: saveEdhrBatchExecutionColumnConfig,
  resetConfig: resetEdhrBatchExecutionColumnConfig
} = useUserTableColumns('mes.pro.edhrBatch.execution.main', edhrBatchExecutionDefaultColumns)

const loading = ref(false)
const createLoading = ref(false)
const voidLoading = ref(false)
const goldenFingerBulkVoidLoading = ref(false)
const readinessLoading = ref(false)
const readinessUserLoading = ref(false)
const workOrderLoading = ref(false)
const createRouteOptionsLoading = ref(false)
const loadError = ref('')
const createError = ref('')
const voidError = ref('')
const goldenFingerBulkVoidError = ref('')
const readinessError = ref('')
const list = ref<EdhrBatchExecutionRespVO[]>([])
const selectedGoldenFingerBulkVoidRows = ref<EdhrBatchExecutionRespVO[]>([])
const isGoldenFingerBulkVoidSelectableRow = (row: EdhrBatchExecutionRespVO) =>
  hasGoldenFingerPermission.value && resolveBatchVoidOperationState(row) === 'normal'
const selectedGoldenFingerBulkVoidIds = computed(() =>
  selectedGoldenFingerBulkVoidRows.value
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0)
)
const selectableGoldenFingerBulkVoidCurrentPageCount = computed(() => list.value.filter(isGoldenFingerBulkVoidSelectableRow).length)
const handleBatchExecutionSelectionChange = (rows: EdhrBatchExecutionRespVO[]) => {
  selectedGoldenFingerBulkVoidRows.value = rows.filter(isGoldenFingerBulkVoidSelectableRow)
}
const readinessUserOptions = ref<UserApi.UserVO[]>([])
const selectableWorkOrders = ref<ProWorkOrderVO[]>([])
const createRouteOptions = ref<EdhrBatchExecutionRouteOptionRespVO[]>([])
const total = ref(0)
const createDialogVisible = ref(false)
const voidDialogVisible = ref(false)
const goldenFingerBulkVoidDialogVisible = ref(false)
const readinessDialogVisible = ref(false)
const archiveDialogVisible = ref(false)
const traceActionDialogVisible = ref(false)
const batchFlowTraceDialogVisible = ref(false)
const operationHistoryDialogVisible = ref(false)
const batchFlowTraceLoading = ref(false)
const archiveError = ref('')
const batchFlowTraceError = ref('')
const archivePreview = ref<EdhrBatchExecutionArchiveRespVO>()
const selectedTraceBatch = ref<EdhrBatchExecutionRespVO>()
const selectedVoidBatch = ref<EdhrBatchExecutionRespVO>()
const batchFlowTraceTimeline = ref<EdhrBatchReviewTimelineRespVO>()
const readinessResult = ref<EdhrRehearsalReadinessResult>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  batchExecutionCode: '',
  workOrderCode: '',
  batchCode: '',
  productCode: '',
  routeCode: '',
  status: undefined as number | undefined,
  createTime: undefined as string[] | undefined,
  quickFilter: undefined as any
})
const edhrBatchQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'batchExecutionCode', label: '批次执行编码', type: 'text', placeholder: '请输入批次执行编码' },
  { key: 'workOrderCode', label: '工单号', type: 'text', placeholder: '请输入工单号' },
  { key: 'batchCode', label: '批次号', type: 'text', placeholder: '请输入批次号' },
  { key: 'product', label: '产品', type: 'text', placeholder: '请输入产品编码或名称' },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '已创建', value: EDHR_BATCH_STATUS_CREATED },
      { label: '执行中', value: EDHR_BATCH_STATUS_IN_PROGRESS },
      { label: '冻结中', value: EDHR_BATCH_STATUS_FROZEN },
      { label: '待关闭', value: EDHR_BATCH_STATUS_READY_TO_CLOSE },
      { label: '需返工', value: EDHR_BATCH_STATUS_REWORK_REQUIRED },
      { label: '已关闭', value: EDHR_BATCH_STATUS_CLOSED }
    ]
  },
  { key: 'createTime', label: '创建时间', type: 'dateRange' }
]
const createForm = reactive({
  workOrderId: undefined as number | undefined,
  routeId: undefined as number | undefined,
  batchCode: '',
  remark: ''
})
const readinessForm = reactive({
  routeId: '',
  executorUserId: '',
  approverUserId: '',
  archiverUserId: ''
})
const voidForm = reactive({
  reasonCategory: '',
  reasonText: '',
  password: '',
  comment: '',
  idempotencyKey: ''
})
const goldenFingerBulkVoidForm = reactive({
  reasonCategory: '',
  reasonText: '',
  password: '',
  comment: ''
})
const voidStartUserSelectTasks = ref<ProcessInstanceApi.ApprovalNodeInfo[]>([])
const voidStartUserSelectAssignees = reactive<Record<string, number[]>>({})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const resetVoidStartUserSelectAssignees = () => {
  Object.keys(voidStartUserSelectAssignees).forEach((key) => {
    delete voidStartUserSelectAssignees[key]
  })
}

const loadVoidStartUserSelectTasks = async (row: EdhrBatchExecutionRespVO) => {
  voidStartUserSelectTasks.value = []
  resetVoidStartUserSelectAssignees()
  if (!row.id) {
    throw new Error('当前批次缺少平台作废动作上下文，无法解析作废审批人。')
  }
  const resolution = await resolveVoidBatchExecutionApproval({
    batchExecutionId: row.id
  })
  if (!resolution.requiresBpm || !resolution.bpmProcessKey) {
    return
  }
  const processDefinition = await DefinitionApi.getProcessDefinition(undefined, resolution.bpmProcessKey)
  if (!processDefinition?.id) {
    throw new Error('作废审批流程未配置，请联系管理员。')
  }
  const approvalDetail = await ProcessInstanceApi.getApprovalDetail({
    processDefinitionId: processDefinition.id,
    activityId: NodeId.START_USER_NODE_ID,
    processVariablesStr: JSON.stringify({
      batchExecutionId: row.id,
      actionCode: 'VOID'
    })
  })
  voidStartUserSelectTasks.value =
    approvalDetail?.activityNodes?.filter(
      (node: ProcessInstanceApi.ApprovalNodeInfo) =>
        CandidateStrategy.START_USER_SELECT === node.candidateStrategy
    ) || []
  for (const task of voidStartUserSelectTasks.value) {
    voidStartUserSelectAssignees[task.id] = []
  }
}

const requireVoidStartUserSelectAssignees = () => {
  for (const task of voidStartUserSelectTasks.value) {
    const assignees = voidStartUserSelectAssignees[task.id]
    if (!Array.isArray(assignees) || assignees.length === 0) {
      voidError.value = `请选择${task.name}审批人。`
      return false
    }
  }
  return true
}

const parseOptionalPositiveNumber = (value: string, label: string) => {
  if (!value.trim()) return undefined
  if (!/^\d+$/.test(value.trim()) || Number(value) <= 0) {
    throw new Error(`${label}必须为正整数。`)
  }
  return Number(value)
}

const parseRequiredPositiveNumber = (value: string, label: string) => {
  const parsed = parseOptionalPositiveNumber(value, label)
  if (parsed == null) throw new Error(`${label}不能为空。`)
  return parsed
}

const readinessBlockerCount = computed(
  () => readinessResult.value?.items?.filter((item) => item.status === 'BLOCKER').length || 0
)
const readinessPassCount = computed(
  () => readinessResult.value?.items?.filter((item) => item.status === 'PASS').length || 0
)
const formatTraceTime = (value?: string | number | null) => {
  return formatEdhrDateTime(value)
}

const batchFlowTraceItems = computed(() => {
  const timeline = batchFlowTraceTimeline.value
  const batch = selectedTraceBatch.value
  const executionCount = timeline?.executionReviews?.length || 0
  const approvalCount = timeline?.approvalRecords?.length || 0
  const archiveCount = timeline?.archiveVersions?.length || 0
  const latestArchive = timeline?.archiveVersions?.[0]
  const blockedTask = timeline?.taskEvents?.find((event) => event.blockerCode || event.blockerMessage)
  return [
    {
      key: 'execution',
      label: '执行',
      description: executionCount
        ? `责任人：执行任务签名人；状态：已产生 ${executionCount} 条执行/返工记录。`
        : '责任人：执行任务签名人；状态：暂无执行记录。',
      time: formatTraceTime(batch?.createTime),
      type: executionCount ? 'success' : 'info'
    },
    {
      key: 'approval',
      label: '审批',
      description: approvalCount
        ? `责任人：审批记录中的审批人；状态：已记录 ${approvalCount} 条审批动作。`
        : '责任人：审批记录中的审批人；状态：暂无审批记录。',
      time: formatTraceTime(timeline?.approvalRecords?.[0]?.signedAt),
      type: approvalCount ? 'success' : 'warning'
    },
    {
      key: 'close',
      label: '关闭',
      description: batch?.closedAt
        ? '责任人：关闭批次操作人；状态：批次已关闭，可进入归档阶段。'
        : `责任人：关闭批次操作人；卡点：${blockedTask?.blockerMessage || '批次尚未关闭或未返回关闭时间。'}`,
      time: formatTraceTime(batch?.closedAt),
      type: batch?.closedAt ? 'success' : 'warning'
    },
    {
      key: 'archive',
      label: '归档',
      description: archiveCount
        ? `责任人：归档待办派发人；状态：已有 ${archiveCount} 个归档版本。`
        : '责任人：归档待办派发人；状态：暂无最终归档版本。',
      time: formatTraceTime(latestArchive?.generatedAt),
      type: archiveCount ? 'success' : 'info'
    }
  ]
})

const operationHistoryRows = computed(() => {
  const timeline = batchFlowTraceTimeline.value
  return [
    ...(timeline?.batchEvents || []).map((event) => ({
      type: '批次',
      subject: event.batchExecutionCode || selectedTraceBatch.value?.batchExecutionCode || '--',
      action: resolveBatchStatusLabel(event.status),
      time: formatTraceTime(event.closedAt || event.createTime),
      description: event.aggregateHash ? `聚合哈希 ${event.aggregateHash}` : '批次状态事件'
    })),
    ...(timeline?.taskEvents || []).map((event) => ({
      type: '任务',
      subject: [event.processCode, event.processName].filter(Boolean).join(' ') || `任务 ${event.taskId || '--'}`,
      action: event.batchRecordReportName || event.batchRecordReportId || '--',
      time: formatTraceTime(event.approvedAt || event.submittedAt || event.openedAt),
      description: event.blockerMessage || `工序序号 ${event.routeProcessSort || '--'}`
    })),
    ...(timeline?.signatureRecords || []).map((record) => ({
      type: '签名',
      subject: record.actorName || (record.actorId ? `用户 ${record.actorId}` : '--'),
      action: record.actionType || '--',
      time: formatTraceTime(record.signedAt),
      description: record.comment || '签名记录'
    })),
    ...(timeline?.approvalRecords || []).map((record) => ({
      type: '审批',
      subject: record.actorName || '--',
      action: record.approvalResult || record.bpmTaskName || '--',
      time: formatTraceTime(record.signedAt),
      description: record.comment || '审批记录'
    })),
    ...(timeline?.archiveVersions || []).map((archive) => ({
      type: '归档',
      subject: archive.fileName || `归档 ${archive.id}`,
      action: archive.archiveStatus || archive.artifactType || '--',
      time: formatTraceTime(archive.generatedAt),
      description: archive.contentHash ? `内容哈希 ${archive.contentHash}` : '最终归档版本'
    }))
  ]
})
const selectedVoidBatchCode = computed(() => {
  if (!selectedVoidBatch.value) return '--'
  return selectedVoidBatch.value.batchExecutionCode || selectedVoidBatch.value.id
})
const selectedVoidWorkOrderCode = computed(() => {
  if (!selectedVoidBatch.value) return '--'
  return selectedVoidBatch.value.workOrderCode || '--'
})

const loadBatchFlowTrace = async (row: EdhrBatchExecutionRespVO) => {
  batchFlowTraceLoading.value = true
  batchFlowTraceError.value = ''
  selectedTraceBatch.value = row
  batchFlowTraceTimeline.value = undefined
  try {
    if (!row.id) throw new Error('当前批次缺少批次执行 ID，无法加载流程追踪。')
    batchFlowTraceTimeline.value = await getEdhrBatchReviewTimeline(row.id)
  } catch (error) {
    batchFlowTraceError.value = resolveErrorMessage(error, '批次流程追踪加载失败。')
  } finally {
    batchFlowTraceLoading.value = false
  }
}

const openFlowTraceDialog = async (row: EdhrBatchExecutionRespVO) => {
  traceActionDialogVisible.value = false
  batchFlowTraceDialogVisible.value = true
  await loadBatchFlowTrace(row)
}

const openOperationHistoryDialog = async (row: EdhrBatchExecutionRespVO) => {
  traceActionDialogVisible.value = false
  operationHistoryDialogVisible.value = true
  await loadBatchFlowTrace(row)
}

const READINESS_BUSINESS_ACTIONS: Record<string, { group: string; action: string; nextStep: string }> = {
  BPM_DEFINITION_MISSING: {
    group: '流程配置',
    action: '启用 eDHR 审批流程',
    nextStep: '请流程管理员发布并启用 mes-edhr-approval-v1，再重新预检。'
  },
  BPM_DEFINITION_INFO_MISSING: {
    group: '流程配置',
    action: '补齐流程扩展信息',
    nextStep: '请流程管理员补齐 BPM 流程定义扩展信息，确认只有一条有效记录。'
  },
  BPM_DEFINITION_INFO_MISMATCH: {
    group: '流程配置',
    action: '清理重复流程扩展信息',
    nextStep: '请流程管理员清理重复的 BPM 流程定义信息后再预检。'
  },
  BPM_START_USER_DENIED: {
    group: '流程配置',
    action: '补齐流程发起人范围',
    nextStep: '请流程管理员把执行人加入 eDHR 流程发起范围，或确认流程允许全员发起。'
  },
  BPM_NOTIFY_TEMPLATE_MISSING: {
    group: '流程配置',
    action: '补齐站内信模板',
    nextStep: '请系统管理员补齐 eDHR BPM 待办、通过、驳回、超时站内信模板。'
  },
  BPM_NOTIFY_TEMPLATE_DISABLED: {
    group: '流程配置',
    action: '启用站内信模板',
    nextStep: '请系统管理员启用 eDHR BPM 站内信模板后再演练。'
  },
  TEMPLATE_REPORT_MISSING: {
    group: '模板规则',
    action: '恢复或绑定批记录模板',
    nextStep: '请模板管理员确认路线批记录绑定和报表元数据存在。'
  },
  TEMPLATE_JSON_MISSING: {
    group: '模板规则',
    action: '恢复模板 JSON',
    nextStep: '请模板管理员恢复报表 JSON 后再预检。'
  },
  TEMPLATE_CELL_RULE_UNREVIEWED: {
    group: '模板规则',
    action: '确认填写规则',
    nextStep: '请模板管理员到批记录规则确认页面完成填写单元格规则确认。'
  },
  SIGNATURE_AUTH_MISSING: {
    group: '电子签名',
    action: '启用签名授权',
    nextStep: '请电子签名管理员为对应用户启用授权，并记录授权依据。'
  },
  MENU_PARENT_MISSING: {
    group: '权限矩阵',
    action: '补齐 eDHR 菜单入口',
    nextStep: '请权限管理员绑定 eDHR 批记录父菜单并刷新权限缓存。'
  },
  MENU_MISSING: {
    group: '权限矩阵',
    action: '补齐角色菜单权限',
    nextStep: '请权限管理员按角色模板补齐菜单权限，确认登录权限响应包含目标路由。'
  },
  PERMISSION_SCOPE_MISSING: {
    group: '权限矩阵',
    action: '绑定对象权限范围',
    nextStep: '请权限管理员为路线批记录绑定有效 permissionScopeId，并补齐对象规则。'
  },
  PERMISSION_RULE_MISSING: {
    group: '权限矩阵',
    action: '补齐对象 ALLOW 规则',
    nextStep: '请权限管理员在 eDHR 权限矩阵页面补齐 VIEW、FILL、SIGN 或 APPROVE 等 ALLOW 规则。'
  }
}

const resolveReadinessBusinessAction = (row: EdhrRehearsalReadinessItem) =>
  READINESS_BUSINESS_ACTIONS[row.code || '']?.action || '按预检建议处理'

const resolveReadinessBusinessGroup = (row: EdhrRehearsalReadinessItem) =>
  READINESS_BUSINESS_ACTIONS[row.code || '']?.group || resolveReadinessRoleLabel(row.roleKey)

const resolveReadinessBusinessNextStep = (row: EdhrRehearsalReadinessItem) =>
  READINESS_BUSINESS_ACTIONS[row.code || '']?.nextStep || '请根据说明和建议补齐前置条件后重新预检。'

const normalizeBatchStatusValue = (status: unknown) => {
  if (typeof status === 'number') return status
  const trimmed = String(status).trim()
  if (!trimmed || trimmed === 'undefined' || trimmed === 'null') return undefined
  if (/^-?\d+$/.test(trimmed)) return Number(trimmed)
  if (trimmed.toUpperCase() === 'VOIDED') return EDHR_BATCH_STATUS_VOIDED
  return undefined
}

const isVoidedBatchExecutionStatus = (status: unknown) =>
  normalizeBatchStatusValue(status) === EDHR_BATCH_STATUS_VOIDED

const isTraceOnlyBatchExecutionStatus = (status: unknown) => {
  const normalizedStatus = normalizeBatchStatusValue(status)
  return normalizedStatus != null && EDHR_BATCH_EXECUTION_TRACE_ONLY_STATUSES.includes(normalizedStatus as any)
}

const isVisibleBatchExecutionRow = (row: EdhrBatchExecutionRespVO) =>
  !isVoidedBatchExecutionStatus(row.status) && !isTraceOnlyBatchExecutionStatus(row.status)

const normalizeBatchExecutionQuery = () => {
  if (queryParams.status === EDHR_BATCH_STATUS_VOIDED || isTraceOnlyBatchExecutionStatus(queryParams.status)) {
    queryParams.status = undefined
  }
  if (
    queryParams.quickFilter?.fieldKey === 'status' &&
    (Number(queryParams.quickFilter.value) === EDHR_BATCH_STATUS_VOIDED ||
      isTraceOnlyBatchExecutionStatus(queryParams.quickFilter.value))
  ) {
    queryParams.quickFilter = undefined
  }
}

const buildQuery = () => {
  normalizeBatchExecutionQuery()
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    batchExecutionCode: queryParams.batchExecutionCode.trim() || undefined,
    workOrderCode: queryParams.workOrderCode.trim() || undefined,
    batchCode: queryParams.batchCode.trim() || undefined,
    productCode: queryParams.productCode.trim() || undefined,
    routeCode: queryParams.routeCode.trim() || undefined,
    status: queryParams.status,
    excludeStatuses: [...EDHR_BATCH_EXECUTION_TRACE_ONLY_STATUSES],
    excludeReleased: true,
    createTime: queryParams.createTime,
    quickFilter: queryParams.quickFilter
  }
}

const buildGoldenFingerBulkVoidFilter = (): EdhrBatchExecutionPageReqVO => {
  const query = buildQuery()
  const filter: EdhrBatchExecutionPageReqVO = {
    batchExecutionCode: query.batchExecutionCode,
    workOrderCode: query.workOrderCode,
    batchCode: query.batchCode,
    productCode: query.productCode,
    routeCode: query.routeCode,
    status: query.status,
    excludeStatuses: query.excludeStatuses,
    excludeReleased: query.excludeReleased,
    createTime: query.createTime,
    quickFilter: query.quickFilter
  }
  if (selectedGoldenFingerBulkVoidIds.value.length) {
    filter.batchExecutionIds = [...selectedGoldenFingerBulkVoidIds.value]
  }
  return filter
}
const resolveBatchStatusLabel = (status?: number | string | null) => {
  const normalizedStatus = normalizeBatchStatusValue(status)
  const labels: Record<number, string> = {
    [EDHR_BATCH_STATUS_CREATED]: '已创建',
    [EDHR_BATCH_STATUS_IN_PROGRESS]: '执行中',
    [EDHR_BATCH_STATUS_FROZEN]: '冻结中',
    [EDHR_BATCH_STATUS_READY_TO_CLOSE]: '待关闭',
    [EDHR_BATCH_STATUS_REWORK_REQUIRED]: '需返工',
    [EDHR_BATCH_STATUS_CLOSED]: '已关闭',
    [EDHR_BATCH_STATUS_ARCHIVED]: '已归档',
    [EDHR_BATCH_STATUS_REJECTED]: '质量终态',
    [EDHR_BATCH_STATUS_VOIDED]: '已作废'
  }
  return normalizedStatus == null ? '--' : labels[normalizedStatus] || String(normalizedStatus)
}

const resolveBatchVoidOperationState = (row: EdhrBatchExecutionRespVO): BatchVoidOperationState => {
  if (isVoidedBatchExecutionStatus(row.status)) return 'voided'
  if (row.pendingVoidChangeEventId) {
    if (row.canWithdrawVoidRequest === true) return 'pending-withdrawable'
    return 'pending-readonly'
  }
  if (row.releaseActionLocked === true && !hasGoldenFingerActionBypass.value) return 'release-locked'
  return 'normal'
}

const isPendingVoidBatch = (row: EdhrBatchExecutionRespVO) =>
  resolveBatchVoidOperationState(row).startsWith('pending-')

const resolveBatchMainStageLabel = (row: EdhrBatchExecutionRespVO) => {
  if (isPendingVoidBatch(row)) return '作废申请中'
  const status = normalizeBatchStatusValue(row.status)
  if (status === EDHR_BATCH_STATUS_ARCHIVED) return '已归档'
  if (status === EDHR_BATCH_STATUS_VOIDED) return '已作废'
  if (status === EDHR_BATCH_STATUS_FROZEN) return '不合格评审'
  if (status === EDHR_BATCH_STATUS_CLOSED) return '待放行'
  if (status === EDHR_BATCH_STATUS_READY_TO_CLOSE) return '待关闭'
  if (status === EDHR_BATCH_STATUS_REWORK_REQUIRED) return '待返工'
  if (status === EDHR_BATCH_STATUS_IN_PROGRESS) {
    return row.blockedCount && row.blockedCount > 0 ? '填写中/有阻塞' : '填写中'
  }
  if (status === EDHR_BATCH_STATUS_REJECTED) return '质量终态'
  if (status === EDHR_BATCH_STATUS_CREATED) return '待开始'
  return '--'
}

const resolveBatchStatusType = (status?: number | string | null) => {
  const normalizedStatus = normalizeBatchStatusValue(status)
  if (normalizedStatus === EDHR_BATCH_STATUS_ARCHIVED || normalizedStatus === EDHR_BATCH_STATUS_CLOSED) return 'success'
  if (normalizedStatus === EDHR_BATCH_STATUS_VOIDED) return 'danger'
  if (normalizedStatus === EDHR_BATCH_STATUS_REJECTED) return 'danger'
  if (normalizedStatus === EDHR_BATCH_STATUS_FROZEN) return 'warning'
  if (normalizedStatus === EDHR_BATCH_STATUS_READY_TO_CLOSE || normalizedStatus === EDHR_BATCH_STATUS_REWORK_REQUIRED) return 'warning'
  if (normalizedStatus === EDHR_BATCH_STATUS_IN_PROGRESS) return 'primary'
  return 'info'
}

const resolveCurrentProcessFillerNames = (row: EdhrBatchExecutionRespVO) => {
  const seenNames = new Set<string>()
  const names = [
    ...(row.currentProcessProductionFillers || []),
    ...(row.currentProcessEquipmentFillers || []),
    ...(row.currentProcessQualityFillers || [])
  ].reduce<string[]>((result, user) => {
    const name = user.displayName?.trim()
    if (!name || seenNames.has(name)) return result
    seenNames.add(name)
    result.push(name)
    return result
  }, [])
  return names.length ? names.join('、') : '--'
}

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrBatchExecutionPage(buildQuery())
    list.value = (data.list || []).filter(isVisibleBatchExecutionRow)
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'eDHR 批次执行列表加载失败。')
  } finally {
    loading.value = false
  }
}
const edhrBatchQuickFilter = useTableQuickFilter(
  'mes.pro.edhrBatch.execution.main',
  edhrBatchQuickFilterDefinitions,
  queryParams,
  getList
)

const normalizeRouteQueryText = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' ? rawValue.trim() : ''
}

const applyRouteQueryFilters = () => {
  const batchExecutionCode = normalizeRouteQueryText(route.query.batchExecutionCode)
  const workOrderCode = normalizeRouteQueryText(route.query.workOrderCode)
  const batchCode = normalizeRouteQueryText(route.query.batchCode)
  if (batchExecutionCode) queryParams.batchExecutionCode = batchExecutionCode
  if (workOrderCode) queryParams.workOrderCode = workOrderCode
  if (batchCode) queryParams.batchCode = batchCode
}

const resetCreateForm = () => {
  createForm.workOrderId = undefined
  createForm.routeId = undefined
  createForm.batchCode = ''
  createForm.remark = ''
  selectableWorkOrders.value = []
  createRouteOptions.value = []
}

const openCreateDialog = () => {
  createError.value = ''
  resetCreateForm()
  createDialogVisible.value = true
  const prefillWorkOrderCode = getPrefillWorkOrderCodeFromRoute()
  if (prefillWorkOrderCode) {
    prefillWorkOrderForCreateDialog(prefillWorkOrderCode)
    return
  }
  searchSelectableWorkOrders('')
}

const getPrefillWorkOrderCodeFromRoute = () => {
  return typeof route.query.prefillWorkOrderCode === 'string' ? route.query.prefillWorkOrderCode.trim() : ''
}

const prefillWorkOrderForCreateDialog = async (prefillWorkOrderCode: string) => {
  if (!prefillWorkOrderCode) return
  await searchSelectableWorkOrders(prefillWorkOrderCode)
  const matchedWorkOrder = selectableWorkOrders.value.find((workOrder) => workOrder.code === prefillWorkOrderCode)
  if (!matchedWorkOrder) {
    createError.value = `未找到可用于批次执行的生产工单：${prefillWorkOrderCode}`
    return
  }
  createForm.workOrderId = matchedWorkOrder.id
  createForm.batchCode = matchedWorkOrder.batchCode || createForm.batchCode
  await loadCreateRouteOptions(matchedWorkOrder.id)
}

const buildSelectableWorkOrderQueries = (keyword: string) => {
  const normalizedKeyword = keyword.trim()
  const baseQuery = {
    pageNo: 1,
    pageSize: 20,
    temporaryFrozen: false
  }
  if (!normalizedKeyword) return [baseQuery]
  return [
    {
      ...baseQuery,
      code: normalizedKeyword
    },
    {
      ...baseQuery,
      productNameKeyword: normalizedKeyword
    }
  ]
}

const dedupeSelectableWorkOrders = (workOrders: ProWorkOrderVO[]) => {
  const workOrderMap = new Map<number, ProWorkOrderVO>()
  workOrders.forEach((workOrder) => {
    if (!workOrderMap.has(workOrder.id)) {
      workOrderMap.set(workOrder.id, workOrder)
    }
  })
  return [...workOrderMap.values()]
}

const searchSelectableWorkOrders = async (keyword: string) => {
  workOrderLoading.value = true
  createError.value = ''
  try {
    const workOrderPages = await Promise.all(
      buildSelectableWorkOrderQueries(keyword).map((query) => ProWorkOrderApi.getWorkOrderPage(query))
    )
    selectableWorkOrders.value = dedupeSelectableWorkOrders(
      workOrderPages.flatMap((data) => data.list || [])
    ).filter(
      (workOrder) => workOrder.status !== MesProWorkOrderStatusEnum.CANCELED
    )
  } catch (error) {
    selectableWorkOrders.value = []
    createError.value = resolveErrorMessage(error, '有效生产工单查询失败。')
  } finally {
    workOrderLoading.value = false
  }
}

const loadCreateRouteOptions = async (workOrderId?: number) => {
  createRouteOptions.value = []
  createForm.routeId = undefined
  if (!workOrderId) return
  createRouteOptionsLoading.value = true
  try {
    createRouteOptions.value = await getEdhrBatchExecutionRouteOptions(workOrderId)
    if (createRouteOptions.value.length === 1) {
      createForm.routeId = createRouteOptions.value[0].routeId
    }
  } catch (error) {
    createRouteOptions.value = []
    createError.value = resolveErrorMessage(error, '工艺路线查询失败。')
  } finally {
    createRouteOptionsLoading.value = false
  }
}

const handleWorkOrderClear = () => {
  createForm.workOrderId = undefined
  createForm.routeId = undefined
  selectableWorkOrders.value = []
  createRouteOptions.value = []
}

const handleWorkOrderChange = async (workOrderId?: number) => {
  const selectedWorkOrder = selectableWorkOrders.value.find((workOrder) => workOrder.id === workOrderId)
  if (selectedWorkOrder?.batchCode) {
    createForm.batchCode = selectedWorkOrder.batchCode
  }
  await loadCreateRouteOptions(workOrderId)
}

const resolveWorkOrderOptionLabel = (workOrder: ProWorkOrderVO) => {
  return [workOrder.code, workOrder.name, workOrder.productName || workOrder.productCode]
    .filter(Boolean)
    .join(' / ')
}

const resolveRouteOptionLabel = (routeOption: EdhrBatchExecutionRouteOptionRespVO) => {
  return [routeOption.routeCode, routeOption.routeName, `ID ${routeOption.routeId}`]
    .filter(Boolean)
    .join(' / ')
}

const submitOpenOrCreate = async () => {
  createLoading.value = true
  try {
    if (createForm.workOrderId == null) throw new Error('请选择有效的未冻结生产工单。')
    if (createRouteOptionsLoading.value) throw new Error('工艺路线正在加载，请稍候再确认。')
    if (createForm.routeId == null) {
      if (!createError.value) createError.value = '请选择工艺路线。'
      return
    }
    if (!createForm.batchCode.trim()) throw new Error('批次号不能为空。')
    createError.value = ''
    const result = await openOrCreateManualEdhrBatchExecution({
      workOrderId: createForm.workOrderId,
      routeId: createForm.routeId,
      batchCode: createForm.batchCode.trim(),
      remark: createForm.remark.trim() || undefined
    })
    createDialogVisible.value = false
    message.success('已打开 eDHR 批次执行')
    await router.push({ path: '/mes/pro/feedback/edhr-batch-execution/detail', query: { id: String(result.id) } })
  } catch (error) {
    createError.value = resolveErrorMessage(error, '打开或创建 eDHR 批次执行失败。')
  } finally {
    createLoading.value = false
  }
}

const submitReadinessCheck = async () => {
  readinessLoading.value = true
  readinessError.value = ''
  readinessResult.value = undefined
  try {
    readinessResult.value = await getEdhrRehearsalReadiness({
      routeId: parseRequiredPositiveNumber(readinessForm.routeId, '路线ID'),
      executorUserId: parseRequiredPositiveNumber(readinessForm.executorUserId, '执行人ID'),
      approverUserId: parseRequiredPositiveNumber(readinessForm.approverUserId, '审批人ID'),
      archiverUserId: parseRequiredPositiveNumber(readinessForm.archiverUserId, '归档员ID')
    })
  } catch (error) {
    readinessError.value = resolveErrorMessage(error, 'eDHR 演练预检失败。')
  } finally {
    readinessLoading.value = false
  }
}

const resolveReadinessRoleLabel = (roleKey?: EdhrRehearsalReadinessItem['roleKey']) => {
  const labels: Record<string, string> = {
    executor: '执行人',
    approver: '审批人',
    archiver: '归档员',
    route: '路线',
    template: '模板'
  }
  return roleKey ? labels[roleKey] || roleKey : '--'
}

const resolveReadinessUserLabel = (user: UserApi.UserVO) => {
  return [user.username, user.nickname, `ID ${user.id}`].filter(Boolean).join(' / ')
}

const openDetail = async (row: EdhrBatchExecutionRespVO, focus?: EdhrBatchExecutionDetailFocus) => {
  const query: Record<string, string> = { id: String(row.id) }
  if (focus) query.focus = focus
  if (focus === 'process') {
    if (row.currentProcessCode) query.processCode = row.currentProcessCode
    if (row.currentProcessName) query.processName = row.currentProcessName
  }
  await router.push({ path: '/mes/pro/feedback/edhr-batch-execution/detail', query })
}

const openVoidDialog = async (row: EdhrBatchExecutionRespVO) => {
  if (isPendingVoidBatch(row)) {
    message.error('当前批次已提交作废申请，只能撤回作废申请。')
    return
  }
  if (row.releaseActionLocked === true && !hasGoldenFingerActionBypass.value) {
    message.error(row.releaseActionLockReason || '放行审批中，只能处理放行审批或撤回放行。')
    return
  }
  if (!row.id) {
    message.error('当前批次缺少批次执行 ID，无法发起作废流程。')
    return
  }
  selectedVoidBatch.value = row
  voidError.value = ''
  Object.assign(voidForm, {
    reasonCategory: '',
    reasonText: '',
    password: '',
    comment: '',
    idempotencyKey: `EDHR-BATCH-VOID-${row.id}-${generateUUID()}`
  })
  voidDialogVisible.value = true
  voidStartUserSelectTasks.value = []
  resetVoidStartUserSelectAssignees()
  try {
    await loadVoidStartUserSelectTasks(row)
  } catch (error) {
    voidError.value = resolveErrorMessage(error, '作废审批人加载失败，请查看后端错误后重试。')
  }
}

const handleWithdrawVoidRequest = async (row: EdhrBatchExecutionRespVO) => {
  if (!row.pendingVoidChangeEventId) {
    message.error('当前批次没有可撤回的作废申请。')
    return
  }
  if (row.canWithdrawVoidRequest !== true) {
    message.error('当前用户不是作废申请发起人，不能撤回该申请。')
    return
  }
  try {
    await message.confirm('确认撤回当前批次执行作废申请吗？')
    const processInstanceId = row.pendingVoidProcessInstanceId
    if (!processInstanceId) {
      throw new Error('当前批次缺少可撤回的表单中心 BPM 作废流程编号，请刷新列表后重试。')
    }
    await ProcessInstanceApi.cancelProcessInstanceByStartUser(processInstanceId, '申请人撤回作废申请')
    message.success('作废申请已撤回')
    await getList()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    message.error(resolveErrorMessage(error, '撤回作废申请失败，请查看后端错误信息。'))
  }
}

const resetGoldenFingerBulkVoidForm = () => {
  goldenFingerBulkVoidForm.reasonCategory = ''
  goldenFingerBulkVoidForm.reasonText = ''
  goldenFingerBulkVoidForm.password = ''
  goldenFingerBulkVoidForm.comment = ''
}

const openGoldenFingerBulkVoidDialog = () => {
  if (!hasGoldenFingerPermission.value) {
    message.error('只有金手指角色可以执行批量作废。')
    return
  }
  goldenFingerBulkVoidError.value = ''
  resetGoldenFingerBulkVoidForm()
  goldenFingerBulkVoidDialogVisible.value = true
}

const submitGoldenFingerBulkVoid = async () => {
  if (!goldenFingerBulkVoidForm.reasonCategory || !goldenFingerBulkVoidForm.reasonText.trim() || !goldenFingerBulkVoidForm.password.trim()) {
    goldenFingerBulkVoidError.value = '请填写原因分类、原因说明和电子签名密码。'
    return
  }
  goldenFingerBulkVoidLoading.value = true
  goldenFingerBulkVoidError.value = ''
  try {
    const result = await goldenFingerBulkVoidEdhrBatchExecutions({
      filter: buildGoldenFingerBulkVoidFilter(),
      reasonCategory: goldenFingerBulkVoidForm.reasonCategory,
      reasonText: goldenFingerBulkVoidForm.reasonText.trim(),
      password: goldenFingerBulkVoidForm.password,
      comment: goldenFingerBulkVoidForm.comment.trim() || undefined
    })
    goldenFingerBulkVoidDialogVisible.value = false
    message.success(
      `批量作废完成：已作废 ${result.voidedCount || 0} 个批次，跳过 ${result.skippedCount || 0} 个终态批次。`
    )
    await getList()
  } catch (error) {
    goldenFingerBulkVoidError.value = resolveErrorMessage(error, '批量作废失败，请查看后端错误信息。')
  } finally {
    goldenFingerBulkVoidLoading.value = false
  }
}
const submitVoidBatchExecution = async () => {
  if (!selectedVoidBatch.value?.id) {
    voidError.value = '当前批次缺少批次执行 ID，无法发起作废流程。'
    return
  }
  if (!voidForm.reasonCategory || !voidForm.reasonText.trim() || !voidForm.password.trim()) {
    voidError.value = '请填写原因分类、原因说明和电子签名密码。'
    return
  }
  voidLoading.value = true
  voidError.value = ''
  try {
    if (!requireVoidStartUserSelectAssignees()) {
      return
    }
    const startUserSelectAssignees = voidStartUserSelectTasks.value.length
      ? { ...voidStartUserSelectAssignees }
      : undefined
    await requestVoidBatchExecution({
      batchExecutionId: selectedVoidBatch.value.id,
      reasonCategory: voidForm.reasonCategory,
      reasonText: voidForm.reasonText.trim(),
      password: voidForm.password,
      comment: voidForm.comment.trim() || undefined,
      startUserSelectAssignees
    })
    voidDialogVisible.value = false
    message.success('已提交批次执行作废申请，等待审批通过后生效')
    await getList()
  } catch (error) {
    voidError.value = resolveErrorMessage(error, '批次执行作废流程提交失败，请查看后端错误信息。')
  } finally {
    voidLoading.value = false
  }
}

const handleViewArchive = async (row: EdhrBatchExecutionRespVO) => {
  traceActionDialogVisible.value = false
  archiveDialogVisible.value = true
  archiveError.value = ''
  archivePreview.value = undefined
  try {
    archivePreview.value = await getLatestEdhrBatchArchive(row.id)
  } catch (error) {
    archiveError.value = resolveErrorMessage(error, '批次最终归档加载失败。')
  }
}

const handleDownloadArchiveByPreview = async () => {
  if (!archivePreview.value?.id) {
    archiveError.value = '当前批次没有可下载的打印版 PDF 归档。'
    return
  }
  try {
    await downloadEdhrBatchArchive(
      archivePreview.value.id,
      archivePreview.value.fileName,
      archivePreview.value.artifactType
    )
    message.success('打印版 PDF 下载已开始')
  } catch (error) {
    archiveError.value = resolveErrorMessage(error, '打印版 PDF 下载失败。')
  }
}

onMounted(() => {
  applyRouteQueryFilters()
  getList()
  if (getPrefillWorkOrderCodeFromRoute()) {
    openCreateDialog()
  }
})
</script>

<style scoped>
.edhr-batch-page {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.edhr-batch-page__list-template {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 16px;
}

.edhr-batch-page__list-alert {
  margin-bottom: 12px;
}

.edhr-batch-page__progress {
  display: grid;
  grid-template-columns: 44px 1fr;
  align-items: center;
  gap: 8px;
  font-variant-numeric: tabular-nums;
}

.edhr-batch-page__stage-cell {
  display: grid;
  justify-items: center;
  gap: 6px;
}

.edhr-batch-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  white-space: nowrap;
}

.edhr-batch-page__dialog-alert {
  margin-bottom: 12px;
}

.edhr-batch-page__void-summary {
  margin-bottom: 14px;
}

.edhr-batch-page__void-form {
  padding-top: 2px;
}

.edhr-batch-page__trace-action-head {
  display: grid;
  gap: 6px;
  margin-bottom: 14px;
}

.edhr-batch-page__trace-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.edhr-batch-page__create-readiness {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  margin-top: 6px;
}

.edhr-batch-page__readiness-form {
  padding: 12px 12px 0;
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
  background: #ffffff;
}

.edhr-batch-page__readiness-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-right: 1px solid #dbe3ef;
  border-left: 1px solid #dbe3ef;
  background: #fafcff;
}

.edhr-batch-page__readiness-status,
.edhr-batch-page__readiness-counts {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #263247;
  font-size: 13px;
}

.edhr-batch-page__readiness-label {
  color: #4b5563;
}

.edhr-batch-page__readiness-table {
  border: 1px solid #dbe3ef;
}

.edhr-batch-page__business-action,
.edhr-batch-page__next-step {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #263247;
}

.edhr-batch-page__next-step-detail {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-batch-page__field-hint {
  margin-top: 6px;
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}

.edhr-batch-page__work-order-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.edhr-batch-page__work-order-code {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
}

.edhr-batch-page__work-order-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}

:global(.edhr-batch-page__work-order-select-popper) {
  width: min(640px, calc(100vw - 48px)) !important;
  min-width: min(640px, calc(100vw - 48px)) !important;
  max-width: calc(100vw - 48px);
}

:global(.edhr-batch-page__work-order-select-popper .el-select-dropdown__item) {
  height: auto;
  min-height: 60px;
  padding: 8px 12px;
  line-height: normal;
}

:global(.edhr-batch-page__work-order-select-popper .edhr-batch-page__work-order-option) {
  min-height: 52px;
}

.edhr-batch-page__muted {
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}
</style>
