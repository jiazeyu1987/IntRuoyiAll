<!-- MES 排产工单池 -->
<template>
  <div class="schedule-order-pool">
    <ContentWrap
      class="schedule-order-pool__content"
      :body-style="{ height: '100%', padding: '10px', display: 'flex', flexDirection: 'column' }"
    >
      <el-tabs
        v-model="scheduleOrderActiveTab"
        class="schedule-order-pool__tabs"
        @tab-change="handleScheduleOrderTabChange"
      >
        <el-tab-pane label="排产工单" name="scheduleOrders">
      <ScheduleOrderMainList
        :query-model="scheduleOrderQueryParams"
        :filter-definitions="scheduleOrderQuickFilterDefinitions"
        :quick-filter-state="scheduleOrderQuickFilter.state"
        :selected-filter-definition="scheduleOrderQuickFilter.selectedDefinition.value"
        :operator-options="scheduleOrderQuickFilter.operatorOptions.value"
        :show-multi-filter="true"
        :multi-filter-definitions="scheduleOrderMultiFilterDefinitions"
        :multi-filter-state="scheduleOrderMultiFilter.state"
        :show-multi-filter-operators="false"
        :columns="scheduleOrderColumns"
        :column-saving="scheduleOrderColumnSaving"
        :total="scheduleOrderTotal"
        @update:page="scheduleOrderQueryParams.pageNo = $event"
        @update:limit="scheduleOrderQueryParams.pageSize = $event"
        @update:quick-filter-state="scheduleOrderQuickFilter.updateState"
        @quick-filter-query="scheduleOrderQuickFilter.applyQuickFilter"
        @update:multi-filter-state="scheduleOrderMultiFilter.updateState"
        @multi-filter-query="scheduleOrderMultiFilter.applyMultiFilter"
        @multi-filter-reset="scheduleOrderMultiFilter.resetMultiFilter"
        @multi-filter-remove="scheduleOrderMultiFilter.removeCondition"
        @column-change="saveScheduleOrderColumnConfig"
        @column-reset="resetScheduleOrderColumnConfig"
        @pagination="getScheduleOrderList"
      >
        <template #actions>
          <div class="schedule-order-pool__tab-actions">
            <div
              class="schedule-order-pool__last-success-time"
              :class="{ 'schedule-order-pool__last-success-time--error': latestSuccessfulScheduleApplyError }"
              :title="latestSuccessfulScheduleApplyTooltip"
            >
              <Icon icon="ep:clock" class="mr-5px" />
              <span>最近一次成功排产时间</span>
              <strong>{{ latestSuccessfulScheduleApplyTimeText }}</strong>
            </div>
            <div class="schedule-order-pool__toolbar-group schedule-order-pool__toolbar-group--primary">
              <el-button
                v-hasPermi="['mes:pro-schedule-order:export']"
                :loading="scheduleOrderExporting"
                plain
                @click="openScheduleOrderExportDialog"
              >
                <Icon icon="ep:download" class="mr-5px" /> 导出
              </el-button>
              <el-tooltip
                :disabled="selectedScheduleOrders.length > 0"
                content="请先勾选排产工单"
                placement="top"
              >
                <span class="schedule-order-pool__toolbar-inline">
                  <el-button
                    v-hasPermi="['mes:pro-auto-schedule:replan']"
                    type="warning"
                    :disabled="!selectedScheduleOrders.length"
                    @click="openReplanDrawer"
                  >
                    <Icon icon="ep:refresh" class="mr-5px" /> 手动重排
                  </el-button>
                </span>
              </el-tooltip>
            </div>
            <UserTableColumnSettings
              class="schedule-order-pool__tab-column-settings"
              :columns="scheduleOrderColumns"
              :saving="scheduleOrderColumnSaving"
              :show-reset="false"
              @change="saveScheduleOrderColumnConfig"
              @reset="resetScheduleOrderColumnConfig"
            />
          </div>
        </template>
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
          <el-table
            ref="scheduleOrderTableRef"
            v-loading="scheduleOrderLoading"
            data-user-table-column-explicit
            data-user-table-key="mes.pro.scheduleOrder.main"
            :data="scheduleOrderList"
            :height="scheduleOrderTableHeight"
            border
            :stripe="true"
            :show-overflow-tooltip="true"
            :cell-class-name="getMainTableCellClassName"
            :row-class-name="getScheduleOrderRowClassName"
            row-key="id"
            @selection-change="handleScheduleOrderSelectionChange"
            @header-dragend="handleScheduleOrderHeaderDragend"
            @sort-change="handleTemplateSortChange"
          >
        <el-table-column
          type="selection"
          width="48"
          fixed="left"
          :selectable="isScheduleOrderSelectable"
        />
        <el-table-column label="重排状态" width="104" fixed="left" align="center">
          <template #default="{ row }">
            <span
              v-if="!isScheduleOrderReplanable(row)"
              class="schedule-order-pool__replan-block-reason"
              role="status"
              :aria-label="`不可重排：${getScheduleOrderReplanBlockReason(row)}`"
            >
              <Icon icon="ep:warning-filled" :size="13" aria-hidden="true" />
              <span>不可重排</span>
              <small>{{ getScheduleOrderReplanBlockReason(row) }}</small>
            </span>
            <span
              v-else
              class="schedule-order-pool__replan-available"
              role="status"
              aria-label="可重排"
            >
              <Icon icon="ep:circle-check-filled" :size="13" aria-hidden="true" />
              可重排
            </span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isScheduleOrderColumnVisible('code')"
          label="排产工单号"
          prop="code"
          :width="getScheduleOrderColumnWidthString('code', 180)"
          v-bind="sortColumnAttrs('code')"
        >
          <template #default="{ row }">
            <span class="schedule-order-pool__main-table-text">{{ row.code || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isScheduleOrderColumnVisible('erpWorkOrderCode')"
          label="来源生产工单号"
          prop="erpWorkOrderCode"
          :width="getScheduleOrderColumnWidthString('erpWorkOrderCode', 180)"
          v-bind="sortColumnAttrs('erpWorkOrderCode')"
        >
          <template #default="{ row }">
            <div class="schedule-order-pool__work-order-ref">
              <el-button
                v-if="row.erpWorkOrderCode"
                link
                type="primary"
                class="schedule-order-pool__inline-link"
                @click="openWorkOrder(row)"
              >
                {{ getScheduleOrderSourceCodeText(row) }}
              </el-button>
              <span v-else>--</span>
              <el-tooltip
                v-if="Number(row.blockingIssueCount || 0) > 0"
                effect="dark"
                placement="top"
                :content="row.latestBlockingIssueMessage || '存在阻断问题'"
              >
                <span class="schedule-order-pool__blocking-reason">
                  阻断：{{ row.latestBlockingIssueMessage || '存在阻断问题' }}
                </span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isScheduleOrderColumnVisible('productCode')"
          label="产品编号"
          prop="productCode"
          :min-width="getScheduleOrderColumnMinWidthString('productCode', 120)"
          v-bind="sortColumnAttrs('productCode')"
        >
          <template #default="{ row }">
            <span
              :class="[
                getScheduleOrderProductCodeClass(row),
                'schedule-order-pool__main-table-text'
              ]"
            >
              {{ row.productCode || '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isScheduleOrderColumnVisible('productName')"
          label="产品名称"
          prop="productName"
          :min-width="getScheduleOrderColumnMinWidthString('productName', 150)"
          v-bind="sortColumnAttrs('productName')"
        />
        <el-table-column
          v-if="isScheduleOrderColumnVisible('productSpecification')"
          label="规格型号"
          prop="productSpecification"
          :min-width="getScheduleOrderColumnMinWidthString('productSpecification', 130)"
          v-bind="sortColumnAttrs('productSpecification')"
        />
        <el-table-column
          v-if="isScheduleOrderColumnVisible('progressPercent')"
          label="数量/进度"
          prop="progressPercent"
          :width="getScheduleOrderColumnWidthString('progressPercent', 170)"
          v-bind="sortColumnAttrs('progressPercent')"
        >
          <template #default="{ row }">
            <div class="schedule-order-pool__quantity-progress">
              <div class="schedule-order-pool__quantity-main">
                <span>总量 {{ formatQuantity(row.totalQuantity ?? row.quantity) }}</span>
                <strong>{{ formatPercent(row.progressPercent) }}%</strong>
              </div>
              <el-progress
                :percentage="normalizePercent(row.progressPercent)"
                :show-text="false"
                :stroke-width="6"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isScheduleOrderColumnVisible('promiseDate')"
          label="承诺交期"
          prop="promiseDate"
          :width="getScheduleOrderColumnWidthString('promiseDate', 130)"
          align="center"
          v-bind="sortColumnAttrs('promiseDate')"
        />
        <el-table-column
          v-if="isScheduleOrderColumnVisible('latestStartTime')"
          label="最晚开工"
          prop="latestStartTime"
          :width="getScheduleOrderColumnWidthString('latestStartTime', 160)"
          align="center"
          v-bind="sortColumnAttrs('latestStartTime')"
        >
          <template #default="{ row }">{{ formatDateTime(row.latestStartTime) }}</template>
        </el-table-column>
        <el-table-column
          v-if="isScheduleOrderColumnVisible('plannedStartTime')"
          label="计划开工"
          prop="plannedStartTime"
          :width="getScheduleOrderColumnWidthString('plannedStartTime', 160)"
          align="center"
          v-bind="sortColumnAttrs('plannedStartTime')"
        >
          <template #default="{ row }">
            <div class="schedule-order-pool__risk-cell">
              <span :class="{ 'schedule-order-pool__risk-text': isStartRisk(row) }">
                {{ formatDateTime(row.plannedStartTime) }}
              </span>
              <span
                v-if="getStartRiskText(row)"
                class="schedule-order-pool__risk-indicator schedule-order-pool__risk-indicator--critical"
                role="status"
                :aria-label="getStartRiskText(row)"
              >
                <Icon icon="ep:warning-filled" :size="13" aria-hidden="true" />
                {{ getStartRiskText(row) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isScheduleOrderColumnVisible('plannedEndTime')"
          label="计划完成"
          prop="plannedEndTime"
          :width="getScheduleOrderColumnWidthString('plannedEndTime', 160)"
          align="center"
          v-bind="sortColumnAttrs('plannedEndTime')"
        >
          <template #default="{ row }">
            <div class="schedule-order-pool__risk-cell">
              <span :class="{ 'schedule-order-pool__warning-text': isDeliveryRisk(row) }">
                {{ formatDateTime(row.plannedEndTime) }}
              </span>
              <span
                v-if="getDeliveryRiskText(row)"
                class="schedule-order-pool__risk-indicator"
                role="status"
                :aria-label="getDeliveryRiskText(row)"
              >
                <Icon icon="ep:warning-filled" :size="13" aria-hidden="true" />
                {{ getDeliveryRiskText(row) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isScheduleOrderColumnVisible('priorityNo')"
          label="优先级"
          prop="priorityNo"
          :width="getScheduleOrderColumnWidthString('priorityNo', 100)"
          align="center"
          v-bind="sortColumnAttrs('priorityNo')"
        />
        <el-table-column
          v-if="isScheduleOrderColumnVisible('productionMaterialList')"
          label="生产用料清单"
          prop="productionMaterialList"
          :min-width="getScheduleOrderColumnMinWidthString('productionMaterialList', 190)"
          align="center"
          v-bind="sortColumnAttrs('productionMaterialList')"
        >
          <template #default="{ row }">
            <el-link
              v-if="row.productionMaterialListCount > 0"
              type="primary"
              @click="handleOpenProductionMaterialList(row)"
            >
              {{
                row.productionMaterialListSummary || `共 ${row.productionMaterialListCount} 张`
              }}
            </el-link>
            <el-tooltip
              v-else
              :content="MISSING_MATERIAL_LIST_HINT"
              effect="dark"
              placement="top"
              popper-class="schedule-order-pool__missing-value-popper"
            >
              <span
                class="schedule-order-pool__missing-value-hint schedule-order-pool__material-missing"
                tabindex="0"
                :aria-label="MISSING_MATERIAL_LIST_HINT"
              >
                <span>缺失</span>
                <Icon icon="ep:question-filled" :size="14" aria-hidden="true" />
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isScheduleOrderColumnVisible('currentProcessName')"
          label="当前工序"
          prop="currentProcessName"
          :min-width="getScheduleOrderColumnMinWidthString('currentProcessName', 170)"
          v-bind="sortColumnAttrs('currentProcessName')"
        >
          <template #default="{ row }">
            <div v-if="row.currentProcessId" class="schedule-order-pool__current-process">
              <el-button
                v-if="row.routeId && row.currentRouteProcessId"
                link
                type="primary"
                class="schedule-order-pool__inline-link"
                @click="openCurrentProcessRouteDetail(row)"
              >
                {{ row.currentProcessName || row.currentProcessCode || row.currentProcessId }}
              </el-button>
              <span v-else>{{
                row.currentProcessName || row.currentProcessCode || row.currentProcessId
              }}</span>
              <span>{{ formatPercent(row.currentProcessProgressPercent) }}%</span>
            </div>
            <el-tooltip
              v-else
              :content="MISSING_CURRENT_PROCESS_HINT"
              effect="dark"
              placement="top"
              popper-class="schedule-order-pool__missing-value-popper"
            >
              <span
                class="schedule-order-pool__missing-value-hint schedule-order-pool__current-process-missing"
                tabindex="0"
                :aria-label="MISSING_CURRENT_PROCESS_HINT"
              >
                <span>-</span>
                <Icon icon="ep:question-filled" :size="14" aria-hidden="true" />
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isScheduleOrderColumnVisible('createTime')"
          label="创建时间"
          prop="createTime"
          :formatter="dateFormatter"
          :width="getScheduleOrderColumnWidthString('createTime', 170)"
          align="center"
          v-bind="sortColumnAttrs('createTime')"
        />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <div v-if="row.frozen" class="schedule-order-pool__row-actions">
              <el-button
                v-hasPermi="['mes:pro-schedule-order:update']"
                link
                type="primary"
                @click="openUnfreezeDialog(row)"
              >
                解冻
              </el-button>
            </div>
            <div v-else class="schedule-order-pool__row-actions">
              <el-button link type="primary" @click="openProcessDialog(row)"> 查看 </el-button>
              <el-button
                v-hasPermi="['mes:pro-schedule-order:update']"
                link
                type="primary"
                @click="openPriorityDialog(row)"
              >
                调整
              </el-button>
              <el-button
                v-hasPermi="['mes:pro-schedule-order:update']"
                link
                type="primary"
                @click="openPromiseDateDialog(row)"
              >
                交期
              </el-button>
              <el-button
                v-hasPermi="['mes:pro-schedule-order:update']"
                link
                type="warning"
                @click="openFreezeDialog(row)"
              >
                冻结
              </el-button>
              <el-button
                v-if="!row.manualFinished && row.status !== SCHEDULE_ORDER_STATUS_FINISHED"
                v-hasPermi="['mes:pro-schedule-order:manual-finish']"
                link
                type="success"
                @click="openManualFinishDialog(row)"
              >
                强制完成
              </el-button>
              <el-button
                v-if="row.manualFinished"
                v-hasPermi="['mes:pro-schedule-order:revoke-complete']"
                link
                type="danger"
                :title="buildManualFinishTooltip(row)"
                @click="openRevokeManualFinishDialog(row)"
              >
                撤销强制完成
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
        </template>
      </ScheduleOrderMainList>
        </el-tab-pane>
        <el-tab-pane label="同步工单" name="workOrderAdmission">
          <div class="schedule-order-pool__admission-tab">
            <UnifiedListTemplate
              class="schedule-order-pool__admission-template"
              table-key="mes.pro.scheduleOrder.admissionDiff"
              :query-model="workOrderAdmissionQueryParams"
              label-width="88px"
              :filter-definitions="[]"
              :show-quick-filter="false"
              :quick-filter-state="{}"
              :operator-options="[]"
              :show-multi-filter="true"
              :multi-filter-definitions="workOrderAdmissionMultiFilterDefinitions"
              :multi-filter-state="workOrderAdmissionMultiFilter.state"
              :show-multi-filter-operators="false"
              :columns="workOrderAdmissionColumns"
              :column-saving="workOrderAdmissionColumnSaving"
              :show-column-settings="false"
              :show-column-reset="false"
              :total="workOrderAdmissionTotal"
              v-model:page="workOrderAdmissionQueryParams.pageNo"
              v-model:limit="workOrderAdmissionQueryParams.pageSize"
              @update:multi-filter-state="workOrderAdmissionMultiFilter.updateState"
              @multi-filter-query="workOrderAdmissionMultiFilter.applyMultiFilter"
              @multi-filter-reset="workOrderAdmissionMultiFilter.resetMultiFilter"
              @multi-filter-remove="workOrderAdmissionMultiFilter.removeCondition"
              @column-change="saveWorkOrderAdmissionColumnConfig"
              @column-reset="resetWorkOrderAdmissionColumnConfig"
              @pagination="getWorkOrderAdmissionList"
            >
              <template #actions>
                <div class="schedule-order-pool__admission-actions schedule-order-pool__admission-bar">
                  <el-button
                    type="primary"
                    :loading="workOrderAdmissionSaving"
                    @click="submitWorkOrderAdmission"
                  >
                    <Icon icon="ep:check" class="mr-5px" /> 选中工单加入排产工单池
                  </el-button>
                  <UserTableColumnSettings
                    class="schedule-order-pool__tab-column-settings"
                    :columns="workOrderAdmissionColumns"
                    :saving="workOrderAdmissionColumnSaving"
                    :show-reset="false"
                    @change="saveWorkOrderAdmissionColumnConfig"
                    @reset="resetWorkOrderAdmissionColumnConfig"
                  />
                </div>
              </template>

              <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
                <div class="schedule-order-pool__admission-table-shell">
                  <el-table
                    v-loading="workOrderAdmissionLoading"
                    data-user-table-column-explicit
                    data-user-table-key="mes.pro.scheduleOrder.admissionDiff"
                    class="schedule-order-pool__admission-table"
                    :data="workOrderAdmissionList"
                    border
                    :stripe="true"
                    :show-overflow-tooltip="true"
                    :cell-class-name="getAdmissionCellClassName"
                    row-key="workOrderId"
                    :height="scheduleOrderTableHeight"
                    style="width: 100%"
                    @selection-change="handleWorkOrderAdmissionSelectionChange"
                    @header-dragend="handleWorkOrderAdmissionHeaderDragend"
                    @sort-change="handleTemplateSortChange"
                  >
                    <el-table-column type="selection" width="48" :selectable="isAdmissionRowSelectable" />
                    <el-table-column
                      v-if="isWorkOrderAdmissionColumnVisible('workOrderCode')"
                      label="工单编码"
                      prop="workOrderCode"
                      :width="getWorkOrderAdmissionColumnWidthString('workOrderCode')"
                      :min-width="getWorkOrderAdmissionColumnMinWidthString('workOrderCode', 160)"
                      v-bind="sortColumnAttrs('workOrderCode')"
                    >
                      <template #default="{ row }">
                        <span
                          :class="[
                            getAdmissionWorkOrderCodeClass(row),
                            'schedule-order-pool__admission-cell-text'
                          ]"
                        >
                          {{ row.workOrderCode || '--' }}
                        </span>
                      </template>
                    </el-table-column>
                    <el-table-column
                      v-if="isWorkOrderAdmissionColumnVisible('productCode')"
                      label="产品编号"
                      prop="productCode"
                      :width="getWorkOrderAdmissionColumnWidthString('productCode')"
                      :min-width="getWorkOrderAdmissionColumnMinWidthString('productCode', 130)"
                      v-bind="sortColumnAttrs('productCode')"
                    >
                      <template #default="{ row }">
                        <span
                          :class="[
                            getAdmissionProductCodeClass(row),
                            'schedule-order-pool__admission-cell-text'
                          ]"
                        >
                          {{ row.productCode || '--' }}
                        </span>
                      </template>
                    </el-table-column>
                    <el-table-column
                      v-if="isWorkOrderAdmissionColumnVisible('productName')"
                      label="产品名称"
                      prop="productName"
                      :width="getWorkOrderAdmissionColumnWidthString('productName')"
                      :min-width="getWorkOrderAdmissionColumnMinWidthString('productName', 150)"
                      v-bind="sortColumnAttrs('productName')"
                    >
                      <template #default="{ row }">
                        <span class="schedule-order-pool__admission-cell-text">{{
                          row.productName || '--'
                        }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column
                      v-if="isWorkOrderAdmissionColumnVisible('productSpecification')"
                      label="规格型号"
                      prop="productSpecification"
                      :width="getWorkOrderAdmissionColumnWidthString('productSpecification')"
                      :min-width="getWorkOrderAdmissionColumnMinWidthString('productSpecification', 140)"
                      v-bind="sortColumnAttrs('productSpecification')"
                    >
                      <template #default="{ row }">
                        <span class="schedule-order-pool__admission-cell-text">
                          {{ row.productSpecification || '--' }}
                        </span>
                      </template>
                    </el-table-column>
                    <el-table-column
                      v-if="isWorkOrderAdmissionColumnVisible('quantity')"
                      label="总数量"
                      prop="quantity"
                      :width="getWorkOrderAdmissionColumnWidthString('quantity', 110)"
                      align="right"
                      v-bind="sortColumnAttrs('quantity')"
                    >
                      <template #default="{ row }">{{ formatQuantity(row.quantity) }}</template>
                    </el-table-column>
                    <el-table-column
                      v-if="isWorkOrderAdmissionColumnVisible('requestDate')"
                      label="需求日期"
                      prop="requestDate"
                      :width="getWorkOrderAdmissionColumnWidthString('requestDate', 160)"
                      align="center"
                      v-bind="sortColumnAttrs('requestDate')"
                    >
                      <template #default="{ row }">{{ formatDateTime(row.requestDate) }}</template>
                    </el-table-column>
                    <el-table-column
                      v-if="isWorkOrderAdmissionColumnVisible('admissionStatus')"
                      label="入池状态"
                      prop="admissionStatus"
                      :width="getWorkOrderAdmissionColumnWidthString('admissionStatus', 120)"
                      align="center"
                      v-bind="sortColumnAttrs('admissionStatus')"
                    >
                      <template #default="{ row }">
                        <el-tag :type="getAdmissionStatusTag(row.admissionStatus, row.severity)" effect="light">
                          {{ getAdmissionStatusText(row.admissionStatus) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column
                      v-if="isWorkOrderAdmissionColumnVisible('message')"
                      label="不可排原因"
                      prop="message"
                      :min-width="getWorkOrderAdmissionColumnMinWidthString('message', 240)"
                      v-bind="sortColumnAttrs('message')"
                    >
                      <template #default="{ row }">
                        <div
                          class="schedule-order-pool__reason-cell schedule-order-pool__admission-message-cell"
                        >
                          <el-tag
                            v-if="row.reasonCode"
                            :type="getAdmissionStatusTag(row.admissionStatus, row.severity)"
                            effect="plain"
                            size="small"
                          >
                            {{ getReasonCodeText(row.reasonCode) }}
                          </el-tag>
                          <span
                            :class="[
                              'schedule-order-pool__admission-cell-text',
                              { 'schedule-order-pool__risk-text': row.severity === 'BLOCKED' }
                            ]"
                          >
                            {{ row.message || getReasonCodeText(row.reasonCode) }}
                          </span>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column
                      v-if="isWorkOrderAdmissionColumnVisible('ownerRole')"
                      label="建议处理"
                      prop="ownerRole"
                      :width="getWorkOrderAdmissionColumnWidthString('ownerRole', 120)"
                      v-bind="sortColumnAttrs('ownerRole')"
                    />
                    <el-table-column
                      v-if="isWorkOrderAdmissionColumnVisible('operation')"
                      label="操作"
                      prop="operation"
                      :width="getWorkOrderAdmissionColumnWidthString('operation', 190)"
                      align="center"
                    >
                      <template #default="{ row }">
                        <template v-if="row.actions?.length">
                          <template v-for="action in row.actions" :key="action.actionLabel">
                            <el-button
                              v-if="canOpenIssueAction(action)"
                              link
                              type="primary"
                              @click="openIssueAction(action)"
                            >
                              {{ action.actionLabel }}
                            </el-button>
                            <el-tag v-else type="info" effect="light"
                              >缺失权限 {{ action.requiredPermission }}</el-tag
                            >
                          </template>
                        </template>
                        <span v-else>-</span>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </template>
            </UnifiedListTemplate>
          </div>
        </el-tab-pane>
      </el-tabs>

      <Dialog v-model="scheduleOrderExportVisible" title="导出排产工单" width="520px">
        <div class="schedule-order-pool__export-dialog">
          <div class="schedule-order-pool__export-hint">
            默认导出当前列表可见业务列，可按需取消不需要的列。
          </div>
          <el-checkbox-group
            v-model="scheduleOrderExportColumns"
            class="schedule-order-pool__export-columns"
          >
            <el-checkbox
              v-for="column in scheduleOrderExportColumnOptions"
              :key="column.key"
              :label="column.key"
            >
              {{ column.label }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <template #footer>
          <el-button @click="scheduleOrderExportVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="scheduleOrderExporting"
            @click="submitScheduleOrderExport"
          >
            导出
          </el-button>
        </template>
      </Dialog>
    </ContentWrap>

    <Dialog v-model="priorityDialogVisible" title="调整优先级" width="420px">
      <el-form label-width="88px">
        <el-form-item label="排产工单号">
          <span>{{ priorityTarget?.code || '-' }}</span>
        </el-form-item>
        <el-form-item label="来源生产工单号">
          <span>{{ priorityTarget?.erpWorkOrderCode || '-' }}</span>
        </el-form-item>
        <el-form-item label="当前优先级">
          <span>{{ priorityTarget?.priorityNo || 1 }}</span>
        </el-form-item>
        <el-form-item label="新优先级">
          <el-input-number
            v-model="priorityForm.priorityNo"
            :min="1"
            :step="1"
            :precision="0"
            controls-position="right"
            class="!w-180px"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priorityDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="prioritySaving" @click="submitPriorityAdjust">
          保存
        </el-button>
      </template>
    </Dialog>

    <Dialog v-model="promiseDateDialogVisible" title="设置承诺交期" width="460px">
      <el-form label-width="88px">
        <el-form-item label="排产工单号">
          <span>{{ promiseDateTarget?.code || '-' }}</span>
        </el-form-item>
        <el-form-item label="来源生产工单号">
          <span>{{ promiseDateTarget?.erpWorkOrderCode || '-' }}</span>
        </el-form-item>
        <el-form-item label="当前交期">
          <span>{{ promiseDateTarget?.promiseDate || '-' }}</span>
        </el-form-item>
        <el-form-item label="承诺交期">
          <el-date-picker
            v-model="promiseDateForm.promiseDate"
            value-format="YYYY-MM-DD"
            type="date"
            class="!w-220px"
          />
        </el-form-item>
        <el-form-item label="修改原因">
          <el-input v-model="promiseDateForm.reason" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="promiseDateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="promiseDateSaving" @click="submitPromiseDateReset">
          保存
        </el-button>
      </template>
    </Dialog>

    <Dialog v-model="freezeDialogVisible" title="冻结排产工单" width="460px">
      <el-form label-width="88px">
        <el-form-item label="排产工单号">
          <span>{{ batchActionRows.map((item) => item.code).join('，') || '-' }}</span>
        </el-form-item>
        <el-form-item label="冻结原因">
          <el-input v-model="batchActionReason" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="freezeDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="batchActionSaving" @click="submitScheduleOrderFreeze">
          冻结
        </el-button>
      </template>
    </Dialog>

    <Dialog v-model="unfreezeDialogVisible" title="解冻排产工单" width="460px">
      <el-form label-width="88px">
        <el-form-item label="排产工单号">
          <span>{{ batchActionRows.map((item) => item.code).join('，') || '-' }}</span>
        </el-form-item>
        <el-form-item label="解冻原因">
          <el-input v-model="batchActionReason" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="unfreezeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchActionSaving" @click="submitScheduleOrderUnfreeze">
          解冻
        </el-button>
      </template>
    </Dialog>

    <Dialog v-model="deleteDialogVisible" title="删除排产工单" width="460px">
      <el-form label-width="88px">
        <el-form-item label="排产工单号">
          <span>{{ batchActionRows.map((item) => item.code).join('，') || '-' }}</span>
        </el-form-item>
        <el-form-item label="删除原因">
          <el-input v-model="batchActionReason" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="batchActionSaving" @click="submitScheduleOrderDelete">
          删除
        </el-button>
      </template>
    </Dialog>

    <Dialog
      v-model="manualFinishDialogVisible"
      :title="manualFinishDialogMode === 'MANUAL_FINISH' ? '排产工单强制完成' : '撤销排产工单强制完成'"
      width="460px"
    >
      <el-alert
        v-if="manualFinishDialogMode === 'MANUAL_FINISH'"
        class="mb-16px"
        type="warning"
        :closable="false"
        show-icon
        title="这是有权限人员执行的强制关闭操作。强制完成后汇总按 100% 展示，真实工序进度仍保留，可撤销。"
      />
      <el-alert
        v-else
        class="mb-16px"
        type="info"
        :closable="false"
        show-icon
        title="撤销后将根据真实工序进度恢复汇总状态。"
      />
      <el-form label-width="128px">
        <el-form-item label="排产工单号">
          <span>{{ manualFinishTarget?.code || '-' }}</span>
        </el-form-item>
        <el-form-item label="来源生产工单号">
          <span>{{ manualFinishTarget?.erpWorkOrderCode || '-' }}</span>
        </el-form-item>
        <el-form-item
          :label="manualFinishDialogMode === 'MANUAL_FINISH' ? '强制完成原因' : '撤销强制完成原因'"
        >
          <el-input
            v-model="manualFinishReason"
            type="textarea"
            :rows="3"
            maxlength="500"
            :placeholder="
              manualFinishDialogMode === 'MANUAL_FINISH'
                ? '请填写强制完成原因'
                : '请填写撤销强制完成原因'
            "
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualFinishDialogVisible = false">取消</el-button>
        <el-button
          :type="manualFinishDialogMode === 'MANUAL_FINISH' ? 'success' : 'danger'"
          :loading="manualFinishSaving"
          @click="submitManualFinishAction"
        >
          {{ manualFinishDialogMode === 'MANUAL_FINISH' ? '强制完成' : '撤销强制完成' }}
        </el-button>
      </template>
    </Dialog>

    <Dialog
      v-model="operationLogDialogVisible"
      title="排产工单追溯"
      width="min(1120px, calc(100vw - 24px))"
    >
      <div v-loading="operationLogLoading" class="schedule-order-pool__trace-dialog">
        <div class="schedule-order-pool__trace-summary">
          <div class="schedule-order-pool__trace-summary-item">
            <span>排产工单号</span>
            <strong>{{ operationLogSummary.scheduleOrderCode }}</strong>
          </div>
          <div class="schedule-order-pool__trace-summary-item">
            <span>日志数量</span>
            <strong>{{ operationLogSummary.totalCount }}</strong>
          </div>
          <div class="schedule-order-pool__trace-summary-item">
            <span>最近操作</span>
            <strong>{{ operationLogSummary.latestOperationType }}</strong>
          </div>
          <div class="schedule-order-pool__trace-summary-item">
            <span>最近时间</span>
            <strong>{{ operationLogSummary.latestTime }}</strong>
          </div>
        </div>

        <UnifiedListTemplate
          class="schedule-order-pool__operation-log-list"
          table-key="mes.pro.scheduleOrder.operationLog"
          :query-model="scheduleOrderViewListQueryModel"
          :filter-definitions="scheduleOrderViewListFilterDefinitions"
          :quick-filter-state="scheduleOrderViewListQuickFilterState"
          :operator-options="scheduleOrderViewListOperatorOptions"
          :columns="operationLogColumns"
          :show-query-form="false"
          :show-column-settings="false"
          :total="0"
          :page="1"
          :limit="20"
        >
          <template #table>
            <el-table
              v-loading="operationLogLoading"
              :data="operationLogList"
              data-user-table-column-explicit
              data-user-table-key="mes.pro.scheduleOrder.operationLog"
              border
              size="small"
              :stripe="true"
              :show-overflow-tooltip="true"
              row-key="id"
              empty-text="暂无追溯记录"
              @header-dragend="handleOperationLogHeaderDragend"
            >
              <el-table-column
                v-if="isOperationLogColumnVisible('expand')"
                type="expand"
                column-key="expand"
                :width="getOperationLogColumnWidthString('expand', 48)"
              >
                <template #default="{ row }">
                  <div class="schedule-order-pool__trace-diff">
                    <div class="schedule-order-pool__trace-diff-head">
                      <span>字段差异</span>
                      <small>{{ buildOperationLogDiffRows(row).length }} 项变化</small>
                    </div>
                    <UnifiedListTemplate
                      class="schedule-order-pool__operation-log-diff-list"
                      table-key="mes.pro.scheduleOrder.operationLogDiff"
                      :query-model="scheduleOrderViewListQueryModel"
                      :filter-definitions="scheduleOrderViewListFilterDefinitions"
                      :quick-filter-state="scheduleOrderViewListQuickFilterState"
                      :operator-options="scheduleOrderViewListOperatorOptions"
                      :columns="operationLogDiffColumns"
                      :show-query-form="false"
                      :show-column-settings="false"
                      :total="0"
                      :page="1"
                      :limit="20"
                    >
                      <template #table>
                        <el-table
                          :data="buildOperationLogDiffRows(row)"
                          data-user-table-column-explicit
                          data-user-table-key="mes.pro.scheduleOrder.operationLogDiff"
                          border
                          size="small"
                          row-key="field"
                          empty-text="暂无字段变化"
                          @header-dragend="handleOperationLogDiffHeaderDragend"
                        >
                          <el-table-column
                            v-if="isOperationLogDiffColumnVisible('fieldLabel')"
                            label="字段"
                            prop="fieldLabel"
                            :width="getOperationLogDiffColumnWidthString('fieldLabel', 150)"
                            :min-width="getOperationLogDiffColumnMinWidthString('fieldLabel', 130)"
                          />
                          <el-table-column
                            v-if="isOperationLogDiffColumnVisible('beforeValue')"
                            label="旧值"
                            prop="beforeValue"
                            :width="getOperationLogDiffColumnWidthString('beforeValue')"
                            :min-width="getOperationLogDiffColumnMinWidthString('beforeValue', 260)"
                          >
                            <template #default="{ row: diffRow }">
                              <span class="schedule-order-pool__trace-value">
                                {{ diffRow.beforeValue }}
                              </span>
                            </template>
                          </el-table-column>
                          <el-table-column
                            v-if="isOperationLogDiffColumnVisible('afterValue')"
                            label="新值"
                            prop="afterValue"
                            :width="getOperationLogDiffColumnWidthString('afterValue')"
                            :min-width="getOperationLogDiffColumnMinWidthString('afterValue', 260)"
                          >
                            <template #default="{ row: diffRow }">
                              <span class="schedule-order-pool__trace-value">
                                {{ diffRow.afterValue }}
                              </span>
                            </template>
                          </el-table-column>
                        </el-table>
                      </template>
                    </UnifiedListTemplate>
                  </div>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isOperationLogColumnVisible('operationType')"
                label="操作类型"
                prop="operationType"
                :width="getOperationLogColumnWidthString('operationType', 120)"
                :min-width="getOperationLogColumnMinWidthString('operationType', 108)"
                align="center"
              >
                <template #default="{ row }">
                  <el-tag size="small" effect="plain">
                    {{ getOperationTypeText(row.operationType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isOperationLogColumnVisible('operatorName')"
                label="操作人"
                prop="operatorName"
                :width="getOperationLogColumnWidthString('operatorName', 120)"
                :min-width="getOperationLogColumnMinWidthString('operatorName', 104)"
              >
                <template #default="{ row }">{{ row.operatorName || '-' }}</template>
              </el-table-column>
              <el-table-column
                v-if="isOperationLogColumnVisible('createTime')"
                label="操作时间"
                prop="createTime"
                :width="getOperationLogColumnWidthString('createTime', 170)"
                :min-width="getOperationLogColumnMinWidthString('createTime', 150)"
                align="center"
              >
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column
                v-if="isOperationLogColumnVisible('scheduleOrderCode')"
                label="排产工单号"
                prop="scheduleOrderCode"
                :width="getOperationLogColumnWidthString('scheduleOrderCode', 180)"
                :min-width="getOperationLogColumnMinWidthString('scheduleOrderCode', 160)"
              >
                <template #default="{ row }">
                  {{ row.scheduleOrderCode || operationLogSummary.scheduleOrderCode }}
                </template>
              </el-table-column>
              <el-table-column
                v-if="isOperationLogColumnVisible('reason')"
                label="操作原因"
                prop="reason"
                :width="getOperationLogColumnWidthString('reason')"
                :min-width="getOperationLogColumnMinWidthString('reason', 240)"
              >
                <template #default="{ row }">{{ row.reason || '未填写' }}</template>
              </el-table-column>
              <el-table-column
                v-if="isOperationLogColumnVisible('diffCount')"
                label="字段差异"
                prop="diffCount"
                :width="getOperationLogColumnWidthString('diffCount', 100)"
                :min-width="getOperationLogColumnMinWidthString('diffCount', 90)"
                align="right"
              >
                <template #default="{ row }">{{ buildOperationLogDiffRows(row).length }} 项</template>
              </el-table-column>
            </el-table>
          </template>
        </UnifiedListTemplate>
      </div>
    </Dialog>

    <ScheduleOrderProcessDetail v-model="processDialogVisible">
      <div v-if="currentScheduleOrder" class="schedule-order-pool__dialog-actions">
        <el-button @click="openDailyCompareDialog(currentScheduleOrder)">报工对比</el-button>
        <el-button type="primary" plain @click="openOperationLogDialog(currentScheduleOrder)">
          操作追溯
        </el-button>
      </div>
      <el-alert
        v-if="currentScheduleOrder?.manualFinished"
        class="mb-12px"
        type="warning"
        :closable="false"
        show-icon
        title="该工单已由有权限人员强制关闭；汇总按 100% 展示，以下工序仍保留真实进度，可撤销强制完成。"
      />
      <UnifiedListTemplate
        table-key="mes.pro.scheduleOrder.processRoute"
        :query-model="processRouteQuickFilterParams"
        :filter-definitions="processRouteQuickFilterDefinitions"
        :show-quick-filter-label="false"
        :quick-filter-state="processRouteQuickFilter.state"
        :selected-filter-definition="processRouteQuickFilter.selectedDefinition.value"
        :operator-options="processRouteQuickFilter.operatorOptions.value"
        :columns="processRouteColumns"
        :column-saving="processRouteColumnSaving"
        :show-column-reset="true"
        :total="processRouteFilteredTotal"
        v-model:page="processRouteQuickFilterParams.pageNo"
        v-model:limit="processRouteQuickFilterParams.pageSize"
        @update:quick-filter-state="processRouteQuickFilter.updateState"
        @quick-filter-query="processRouteQuickFilter.applyQuickFilter"
        @column-change="saveProcessRouteColumnConfig"
        @column-reset="resetProcessRouteColumnConfig"
        @pagination="handleProcessRoutePagination"
      >
        <template #actions>
          <el-button @click="processRouteQuickFilter.resetQuickFilter">重置</el-button>
        </template>
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
          <div class="schedule-order-pool__process-dialog-table">
            <el-table
              class="schedule-order-pool__process-summary-table"
              data-user-table-column-explicit
              data-user-table-key="mes.pro.scheduleOrder.processRoute"
              v-loading="processLoading"
              :data="processRouteFilteredList"
              max-height="calc(100vh - 292px)"
              :stripe="true"
              :show-overflow-tooltip="true"
              :row-class-name="getProcessProgressRowClass"
              row-key="id"
              @header-dragend="handleProcessRouteHeaderDragend"
              @sort-change="handleTemplateSortChange"
            >
              <el-table-column
                v-if="isProcessRouteColumnVisible('expand')"
                type="expand"
                column-key="expand"
                :width="getProcessRouteColumnWidthString('expand', 44)"
              >
                <template #default="{ row }">
                  <div class="schedule-order-pool__capacity-snapshot">
                    <div class="schedule-order-pool__capacity-snapshot-title">
                      产能快照
                      <span>{{ getProcessCapacityModeText(row.capacityMode) }}</span>
                    </div>
                    <div class="schedule-order-pool__capacity-snapshot-grid">
                      <span>策略</span>
                      <strong>{{ getProcessCapacityModeText(row.capacityMode) }}</strong>
                      <span>来源</span>
                      <strong>{{ getCapacitySourceText(row.capacitySource) }}</strong>
                      <span>小时产能</span>
                      <strong>{{ formatCapacityIntegerNumber(row.hourlyCapacityTotal) }}</strong>
                      <span>班次产能</span>
                      <strong>{{ formatCapacityIntegerNumber(row.shiftCapacityTotal) }}</strong>
                    </div>
                    <div
                      v-if="getProcessResourceRows(row).length > 0"
                      class="schedule-order-pool__capacity-snapshot-resources"
                    >
                      <el-tag
                        v-for="(resource, index) in getProcessResourceRows(row)"
                        :key="resource.workstationId || resource.workstationCode || resource.workstationName || index"
                        effect="light"
                        size="small"
                      >
                        {{ resource.workstationName || resource.workstationCode || resource.workstationId }}
                        · {{ getCapacitySourceText(resource.resourceType) }}
                        · {{ formatCapacityIntegerNumber(resource.hourlyCapacity) }}/h
                      </el-tag>
                    </div>
                  </div>
                  <div class="schedule-order-pool__feedback-history">
                    <div class="schedule-order-pool__feedback-history-title">
                      历史报工明细
                      <span>共 {{ row.feedbackCount || 0 }} 次</span>
                    </div>
                    <UnifiedListTemplate
                      class="schedule-order-pool__feedback-history-list"
                      table-key="mes.pro.scheduleOrder.feedbackHistory"
                      :query-model="feedbackHistoryTemplateQueryModel"
                      :filter-definitions="feedbackHistoryQuickFilterDefinitions"
                      :quick-filter-state="feedbackHistoryQuickFilterState"
                      :operator-options="feedbackHistoryOperatorOptions"
                      :columns="feedbackHistoryColumns"
                      :show-query-form="false"
                      :show-column-settings="false"
                      :total="0"
                      :page="1"
                      :limit="20"
                    >
                      <template #table>
                        <el-table
                          :data="row.feedbackHistoryList || []"
                          data-user-table-column-explicit
                          data-user-table-key="mes.pro.scheduleOrder.feedbackHistory"
                          border
                          :stripe="true"
                          :show-overflow-tooltip="true"
                          size="small"
                          row-key="id"
                          empty-text="暂无报工记录"
                          @header-dragend="handleFeedbackHistoryHeaderDragend"
                        >
                          <el-table-column
                            label="报工单号"
                            prop="code"
                            :width="getFeedbackHistoryColumnWidthString('code', 160)"
                            :min-width="getFeedbackHistoryColumnMinWidthString('code', 140)"
                          >
                            <template #default="{ row: feedback }">{{ feedback.code || '-' }}</template>
                          </el-table-column>
                          <el-table-column
                            label="报工时间"
                            prop="feedbackTime"
                            :width="getFeedbackHistoryColumnWidthString('feedbackTime', 170)"
                            :min-width="getFeedbackHistoryColumnMinWidthString('feedbackTime', 150)"
                            align="center"
                          >
                            <template #default="{ row: feedback }">
                              {{ formatDateTime(feedback.feedbackTime) }}
                            </template>
                          </el-table-column>
                          <el-table-column
                            label="本次数量"
                            prop="feedbackQuantity"
                            :width="getFeedbackHistoryColumnWidthString('feedbackQuantity', 110)"
                            :min-width="getFeedbackHistoryColumnMinWidthString('feedbackQuantity', 96)"
                            align="right"
                          >
                            <template #default="{ row: feedback }">
                              {{ formatQuantity(feedback.feedbackQuantity) }}
                            </template>
                          </el-table-column>
                          <el-table-column
                            label="合格数"
                            prop="qualifiedQuantity"
                            :width="getFeedbackHistoryColumnWidthString('qualifiedQuantity', 100)"
                            :min-width="getFeedbackHistoryColumnMinWidthString('qualifiedQuantity', 88)"
                            align="right"
                          >
                            <template #default="{ row: feedback }">
                              {{ formatQuantity(feedback.qualifiedQuantity) }}
                            </template>
                          </el-table-column>
                          <el-table-column
                            label="报工人"
                            prop="feedbackUserNickname"
                            :width="getFeedbackHistoryColumnWidthString('feedbackUserNickname', 120)"
                            :min-width="getFeedbackHistoryColumnMinWidthString('feedbackUserNickname', 104)"
                          >
                            <template #default="{ row: feedback }">
                              {{ feedback.feedbackUserNickname || feedback.feedbackUserId || '-' }}
                            </template>
                          </el-table-column>
                        </el-table>
                      </template>
                    </UnifiedListTemplate>
                  </div>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('processCode')"
                label="工序编号"
                prop="processCode"
                :width="getProcessRouteColumnWidthString('processCode', 112)"
                :min-width="getProcessRouteColumnMinWidthString('processCode', 104)"
                v-bind="sortColumnAttrs('processCode')"
              />
              <el-table-column
                v-if="isProcessRouteColumnVisible('processName')"
                label="工序名称"
                prop="processName"
                :width="getProcessRouteColumnWidthString('processName', 152)"
                :min-width="getProcessRouteColumnMinWidthString('processName', 136)"
                v-bind="sortColumnAttrs('processName')"
              />
              <el-table-column
                v-if="isProcessRouteColumnVisible('capacityMode')"
                label="排产策略"
                prop="capacityMode"
                :width="getProcessRouteColumnWidthString('capacityMode', 112)"
                :min-width="getProcessRouteColumnMinWidthString('capacityMode', 104)"
                align="center"
                v-bind="sortColumnAttrs('capacityMode')"
              >
                <template #default="{ row }">
                  <el-tag effect="light" size="small">
                    {{ getProcessCapacityModeText(row.capacityMode) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('hourlyCapacityTotal')"
                label="小时产能"
                prop="hourlyCapacityTotal"
                :width="getProcessRouteColumnWidthString('hourlyCapacityTotal', 104)"
                :min-width="getProcessRouteColumnMinWidthString('hourlyCapacityTotal', 96)"
                align="right"
                v-bind="sortColumnAttrs('hourlyCapacityTotal')"
              >
                <template #default="{ row }">{{ formatCapacityIntegerNumber(row.hourlyCapacityTotal) }}</template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('capacitySource')"
                label="产能来源"
                prop="capacitySource"
                :width="getProcessRouteColumnWidthString('capacitySource', 104)"
                :min-width="getProcessRouteColumnMinWidthString('capacitySource', 96)"
                align="center"
                v-bind="sortColumnAttrs('capacitySource')"
              >
                <template #default="{ row }">{{ getCapacitySourceText(row.capacitySource) }}</template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('shiftCapacityTotal')"
                label="班次产能"
                prop="shiftCapacityTotal"
                :width="getProcessRouteColumnWidthString('shiftCapacityTotal', 104)"
                :min-width="getProcessRouteColumnMinWidthString('shiftCapacityTotal', 96)"
                align="right"
                v-bind="sortColumnAttrs('shiftCapacityTotal')"
              >
                <template #default="{ row }">{{ formatCapacityIntegerNumber(row.shiftCapacityTotal) }}</template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('plannedQuantity')"
                label="需要多少个"
                prop="plannedQuantity"
                :width="getProcessRouteColumnWidthString('plannedQuantity', 104)"
                :min-width="getProcessRouteColumnMinWidthString('plannedQuantity', 96)"
                align="right"
                v-bind="sortColumnAttrs('plannedQuantity')"
              >
                <template #default="{ row }">{{ formatQuantity(row.plannedQuantity) }}</template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('effectiveCompletedQuantity')"
                label="做了多少个"
                prop="effectiveCompletedQuantity"
                :width="getProcessRouteColumnWidthString('effectiveCompletedQuantity', 104)"
                :min-width="getProcessRouteColumnMinWidthString('effectiveCompletedQuantity', 96)"
                align="right"
                v-bind="sortColumnAttrs('effectiveCompletedQuantity')"
              >
                <template #default="{ row }">{{
                  formatQuantity(row.effectiveCompletedQuantity)
                }}</template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('status')"
                label="状态"
                prop="status"
                :width="getProcessRouteColumnWidthString('status', 96)"
                :min-width="getProcessRouteColumnMinWidthString('status', 88)"
                align="center"
                v-bind="sortColumnAttrs('status')"
              >
                <template #default="{ row }">
                  <el-tag :type="getProcessProgressStatusTag(row)" effect="light">
                    {{ getProcessProgressStatusText(row) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('shiftStatus')"
                label="班次状态"
                prop="shiftStatus"
                :width="getProcessRouteColumnWidthString('shiftStatus', 104)"
                :min-width="getProcessRouteColumnMinWidthString('shiftStatus', 96)"
                align="center"
                v-bind="sortColumnAttrs('shiftStatus')"
              >
                <template #default="{ row }">
                  <el-tag
                    size="small"
                    :type="getProcessRouteShiftStatusText(row) === '夜班' ? 'warning' : 'success'"
                  >
                    {{ getProcessRouteShiftStatusText(row) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('feedbackCount')"
                label="报工次数"
                prop="feedbackCount"
                :width="getProcessRouteColumnWidthString('feedbackCount', 88)"
                :min-width="getProcessRouteColumnMinWidthString('feedbackCount', 80)"
                align="right"
                v-bind="sortColumnAttrs('feedbackCount')"
              >
                <template #default="{ row }">{{ row.feedbackCount || 0 }}</template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('latestFeedbackTime')"
                label="最近报工时间"
                prop="latestFeedbackTime"
                :width="getProcessRouteColumnWidthString('latestFeedbackTime', 146)"
                :min-width="getProcessRouteColumnMinWidthString('latestFeedbackTime', 136)"
                align="center"
                v-bind="sortColumnAttrs('latestFeedbackTime')"
              >
                <template #default="{ row }">{{ formatDateTime(row.latestFeedbackTime) }}</template>
              </el-table-column>
              <el-table-column
                v-if="isProcessRouteColumnVisible('estimatedCompletionTime')"
                label="预计结束"
                prop="estimatedCompletionTime"
                :width="getProcessRouteColumnWidthString('estimatedCompletionTime', 156)"
                :min-width="getProcessRouteColumnMinWidthString('estimatedCompletionTime', 146)"
                align="center"
                v-bind="sortColumnAttrs('estimatedCompletionTime')"
              >
                <template #default="{ row }">{{ getProcessRouteEstimatedCompletionTime(row) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </UnifiedListTemplate>
    </ScheduleOrderProcessDetail>

    <Dialog v-model="dailyCompareDialogVisible" title="报工计划对比" width="920px">
      <div class="schedule-order-pool__compare-toolbar">
        <el-date-picker
          v-model="dailyCompareRange"
          value-format="YYYY-MM-DD"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="!w-280px"
        />
        <el-button type="primary" @click="loadDailyCompare">
          <Icon icon="ep:search" class="mr-5px" /> 查询
        </el-button>
      </div>
      <UnifiedListTemplate
        table-key="mes.pro.scheduleOrder.dailyCompare"
        :query-model="scheduleOrderViewListQueryModel"
        :filter-definitions="scheduleOrderViewListFilterDefinitions"
        :quick-filter-state="scheduleOrderViewListQuickFilterState"
        :operator-options="scheduleOrderViewListOperatorOptions"
        :columns="dailyCompareColumns"
        :show-query-form="false"
        :show-column-settings="false"
        :total="0"
        :page="1"
        :limit="20"
      >
        <template #table>
          <el-table
            v-loading="dailyCompareLoading"
            :data="dailyCompareList"
            data-user-table-column-explicit
            data-user-table-key="mes.pro.scheduleOrder.dailyCompare"
            border
            :stripe="true"
            :show-overflow-tooltip="true"
            :row-key="getDailyCompareRowKey"
            empty-text="暂无报工计划对比"
            @header-dragend="handleDailyCompareHeaderDragend"
          >
            <el-table-column
              v-if="isDailyCompareColumnVisible('planDate')"
              label="日期"
              prop="planDate"
              :width="getDailyCompareColumnWidthString('planDate', 130)"
              :min-width="getDailyCompareColumnMinWidthString('planDate', 118)"
              align="center"
            />
            <el-table-column
              v-if="isDailyCompareColumnVisible('plannedQuantity')"
              label="计划数量"
              prop="plannedQuantity"
              :width="getDailyCompareColumnWidthString('plannedQuantity', 130)"
              :min-width="getDailyCompareColumnMinWidthString('plannedQuantity', 112)"
              align="right"
            >
              <template #default="{ row }">{{ formatQuantity(row.plannedQuantity) }}</template>
            </el-table-column>
            <el-table-column
              v-if="isDailyCompareColumnVisible('actualQuantity')"
              label="实际报工"
              prop="actualQuantity"
              :width="getDailyCompareColumnWidthString('actualQuantity', 130)"
              :min-width="getDailyCompareColumnMinWidthString('actualQuantity', 112)"
              align="right"
            >
              <template #default="{ row }">{{ formatQuantity(row.actualQuantity) }}</template>
            </el-table-column>
            <el-table-column
              v-if="isDailyCompareColumnVisible('diffQuantity')"
              label="差异"
              prop="diffQuantity"
              :width="getDailyCompareColumnWidthString('diffQuantity', 130)"
              :min-width="getDailyCompareColumnMinWidthString('diffQuantity', 112)"
              align="right"
            >
              <template #default="{ row }">
                <span :class="getDailyCompareDiffClass(row.diffQuantity)">
                  {{ formatQuantity(row.diffQuantity) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isDailyCompareColumnVisible('status')"
              label="状态"
              prop="status"
              :width="getDailyCompareColumnWidthString('status', 120)"
              :min-width="getDailyCompareColumnMinWidthString('status', 104)"
              align="center"
            >
              <template #default="{ row }">
                <el-tag :type="getDailyCompareStatusTag(row.status)" effect="light">
                  {{ row.statusLabel || getDailyCompareStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isDailyCompareColumnVisible('remark')"
              label="偏差说明"
              prop="remark"
              :width="getDailyCompareColumnWidthString('remark')"
              :min-width="getDailyCompareColumnMinWidthString('remark', 220)"
            />
          </el-table>
        </template>
      </UnifiedListTemplate>
    </Dialog>

    <ScheduleOrderReplanDrawer>
    <Dialog v-model="replanSettingsDialogVisible" title="重排设置" width="640px">
      <div class="schedule-order-pool__replan-settings">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            <div class="schedule-order-pool__capacity-alert">
              <span>
                排产前检查是只读诊断；手动重排会生成变更预览。可归因到工单的阻断会跳过该工单，其余可排工单继续应用；全局阻断仍会停止应用。
              </span>
              <span>{{ runtimeCapacityBasisDifferenceText }}</span>
            </div>
          </template>
        </el-alert>
        <el-form label-width="96px">
          <el-form-item label="重排开始">
            <el-date-picker
              v-model="replanForm.startTime"
              value-format="YYYY-MM-DD"
              type="date"
              placeholder="请选择预览开始日期"
              class="!w-260px"
            />
          </el-form-item>
          <el-form-item label="产能口径">
            <el-radio-group v-model="replanForm.runtimeCapacityBasis">
              <el-radio-button label="PLANNED">计划产能</el-radio-button>
              <el-radio-button label="ACTUAL">实际产能</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="手工锁定">
            <el-switch v-model="replanForm.preserveManualLockedTasks" />
          </el-form-item>
          <el-form-item label="重排原因">
            <el-input
              v-model="replanForm.reason"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              placeholder="可选填写本次重排的业务原因"
            />
          </el-form-item>
        </el-form>
        <el-alert
          :title="replanScopeSummaryText"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>
    </Dialog>

    <el-drawer v-model="replanDrawerVisible" title="排产前检查 / 手动重排" size="720px">
      <div class="schedule-order-pool__replan">
        <div class="schedule-order-pool__preflight-panel">
          <div class="schedule-order-pool__preflight-head">
            <div>
              <span class="schedule-order-pool__preflight-title">排产前检查</span>
              <span class="schedule-order-pool__preflight-time">
                {{
                  preflightResult?.checkedAt ? formatDateTime(preflightResult.checkedAt) : '未检查'
                }}
              </span>
            </div>
            <el-button :loading="preflightLoading" @click="runPreflight">
              <Icon icon="ep:refresh" class="mr-5px" /> 重新检查
            </el-button>
          </div>
          <div v-if="preflightResult" class="schedule-order-pool__preflight-summary">
            <el-tag :type="getPreflightResultTag(preflightResult.result)" effect="light">
              {{ getPreflightResultText(preflightResult.result) }}
            </el-tag>
            <el-tag type="success" effect="light"
              >通过 {{ preflightResult.summary?.passCount ?? 0 }}</el-tag
            >
            <el-tag type="warning" effect="light"
              >警告 {{ preflightResult.summary?.warnCount ?? 0 }}</el-tag
            >
            <el-tag type="danger" effect="light"
              >阻断 {{ preflightResult.summary?.blockedCount ?? 0 }}</el-tag
            >
          </div>
          <el-alert
            v-if="preflightStale"
            title="检查范围或参数已变化，请重新检查。"
            type="warning"
            :closable="false"
            show-icon
          />
          <el-alert
            v-if="preflightHasBlockedIssue"
            :title="
              preflightHasGlobalBlockedIssue
                ? '存在无法归因到工单的阻断问题，不能应用重排。'
                : '存在部分工单阻断；应用时将跳过问题工单，其余可排工单可继续重排。'
            "
            :type="preflightHasGlobalBlockedIssue ? 'error' : 'warning'"
            :closable="false"
            show-icon
          />
          <el-table
            v-if="preflightResult?.issues?.length"
            data-user-table-column-explicit
            class="mt-12px"
            :data="preflightResult.issues"
            :show-overflow-tooltip="true"
          >
            <el-table-column label="严重度" prop="severity" width="90">
              <template #default="{ row }">
                <el-tag :type="getPreflightResultTag(row.severity)" effect="light">
                  {{ getPreflightResultText(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="产品/编号" min-width="180">
              <template #default="{ row }">
                <div class="schedule-order-pool__issue-product">
                  <span>{{ getPreflightIssueProductName(row) }}</span>
                  <small>{{ getPreflightIssueProductCode(row) }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="不可排原因" prop="message" min-width="250">
              <template #default="{ row }">{{
                row.message || getReasonCodeText(row.reasonCode)
              }}</template>
            </el-table-column>
            <el-table-column label="建议处理" prop="ownerRole" width="110" />
            <el-table-column label="操作" width="170" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="canOpenIssueAction(row.action)"
                  link
                  type="primary"
                  @click="openIssueAction(row.action)"
                >
                  {{ row.action.actionLabel }}
                </el-button>
                <el-tag v-else-if="row.action?.requiredPermission" type="info" effect="light">
                  缺失权限 {{ row.action.requiredPermission }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="schedule-order-pool__replan-actions">
          <el-button
            v-if="hasReplanPermission"
            type="primary"
            :loading="replanApplyLoading || replanPreviewLoading"
            :disabled="!canApplyReplan"
            @click="applyReplan"
          >
            <Icon icon="ep:refresh" class="mr-5px" /> 开始重排
          </el-button>
          <span
            v-if="hasReplanPermission && replanProjectionState.disabled"
            class="schedule-order-pool__replan-blocker"
            :title="replanProjectionState.blockerMessage"
          >
            {{ replanProjectionState.blockerMessage }}
          </span>
          <span
            v-show="showReplanApplyProgress"
            class="schedule-order-pool__replan-progress"
            aria-live="polite"
          >
            <span>重排进度 {{ replanApplyProgressPercent }}%</span>
            <el-progress
              :percentage="replanApplyProgressPercent"
              :stroke-width="6"
              :show-text="false"
            />
          </span>
          <el-button v-if="hasReplanPermission" @click="openReplanSettingsDialog">
            <Icon icon="ep:setting" class="mr-5px" /> 设置
          </el-button>
        </div>

        <el-alert
          v-if="replanPreviewStale"
          title="重排参数已变化，开始重排时会重新检查并生成预览。"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-alert
          v-if="replanPreviewHasBlockedIssue"
          :title="
            replanPreviewHasGlobalBlockedIssue
              ? '重排预览存在无法归因到工单的阻断问题，不能应用重排。'
              : '重排预览存在部分工单阻断；确认后将仅应用其余可排工单。'
          "
          :type="replanPreviewHasGlobalBlockedIssue ? 'error' : 'warning'"
          :closable="false"
          show-icon
        />

        <div v-if="replanPreview" class="schedule-order-pool__replan-summary">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="工单数">
              {{ replanPreview.summary?.workOrderCount ?? 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="生成任务">
              {{ replanPreview.summary?.generatedTaskCount ?? 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="冻结保护">
              {{ formatPreservedTaskSummary(replanPreview.summary?.preservedTaskCount) }}
            </el-descriptions-item>
            <el-descriptions-item label="报工保护">
              <el-button
                type="primary"
                link
                :disabled="!replanFeedbackProtectionCount"
                @click="replanFeedbackProtectionDialogVisible = true"
              >
                报工保护({{ replanFeedbackProtectionCount }})
              </el-button>
            </el-descriptions-item>
            <el-descriptions-item label="阻塞问题">
              {{ replanPreview.summary?.blockingIssueCount ?? 0 }}
            </el-descriptions-item>
          </el-descriptions>
          <el-table
            v-if="replanIssueRows.length"
            data-user-table-column-explicit
            class="mt-12px"
            :data="replanIssueRows"
            :show-overflow-tooltip="true"
          >
            <el-table-column label="严重度" prop="severity" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getIssueSeverityTag(row.severity)" effect="light">
                  {{ getIssueSeverityText(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="问题" prop="problem" min-width="220" />
            <el-table-column label="备注" min-width="320">
              <template #default="{ row }">
                <div class="schedule-order-pool__issue-remark">
                  <el-button
                    v-if="row.issueType === 'MATERIAL'"
                    link
                    type="primary"
                    @click="openMaterialShortageDialog"
                  >
                    查看缺料
                  </el-button>
                  <span v-for="part in row.remarkParts" :key="part">{{ part }}</span>
                  <el-button
                    v-if="row.sourceIssue && canOpenReplanIssueCalendar(row.sourceIssue)"
                    link
                    type="primary"
                    @click="openReplanIssueCalendar(row.sourceIssue)"
                  >
                    跳到班次
                  </el-button>
                  <span
                    v-if="
                      !row.remarkParts.length && row.issueType !== 'MATERIAL' && !row.sourceIssue
                    "
                    >-</span
                  >
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>

    <Dialog v-model="replanFeedbackProtectionDialogVisible" title="报工保护明细" width="680px">
      <el-table
        :data="replanFeedbackProtectedTasks"
        :show-overflow-tooltip="true"
      >
        <el-table-column label="任务" min-width="260">
          <template #default="{ row }">
            {{ formatProtectedTaskLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="保护原因" width="140">
          <template #default="{ row }">
            {{ formatProtectionReason(row.protectionReason) }}
          </template>
        </el-table-column>
      </el-table>
    </Dialog>

    <Dialog v-model="replanStartDateDialogVisible" title="开始重排日期" width="460px">
      <div class="schedule-order-pool__replan-start-date">
        <el-alert
          title="开始重排会按所选日期整天重新检查、预览并直接应用，应用成功后正式排程立即更新。"
          type="warning"
          :closable="false"
          show-icon
        />
        <div class="schedule-order-pool__replan-start-field">
          <div class="schedule-order-pool__replan-start-label">请选择开始重排日期</div>
          <el-date-picker
            v-model="replanStartDate"
            value-format="YYYY-MM-DD"
            type="date"
            placeholder="请选择开始日期"
            class="!w-100%"
          />
        </div>
        <div class="schedule-order-pool__replan-start-hint">
          当前选择日期：{{ replanStartDate || '未选择' }}，起排时间 {{ replanStartDateStartTime }}
        </div>
        <div class="schedule-order-pool__dialog-footer">
          <el-button @click="replanStartDateDialogVisible = false">取消</el-button>
          <el-button
            type="warning"
            :loading="replanApplyLoading"
            @click="confirmApplyReplanStartChoice"
          >
            确认应用重排
          </el-button>
        </div>
      </div>
    </Dialog>
    </ScheduleOrderReplanDrawer>

    <Dialog v-model="materialShortageDialogVisible" title="物料缺料明细" width="720px">
      <el-table
        :data="materialShortageIssues"
        :stripe="true"
        :show-overflow-tooltip="true"
        row-key="id"
      >
        <el-table-column label="物料" prop="materialName" min-width="180" />
        <el-table-column label="编码" prop="materialCode" min-width="220" />
        <el-table-column label="缺少数量" prop="shortageQty" width="140" align="right">
          <template #default="{ row }">{{ formatQuantity(row.shortageQty) }}</template>
        </el-table-column>
      </el-table>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { ElNotification } from 'element-plus'
import download from '@/utils/download'
import { generateUUID } from '@/utils'
import { dateFormatter, formatDate, formatDateTimeValue } from '@/utils/formatTime'
import {
  MesProScheduleOrderApi,
  type MesProScheduleOrderAdmissionDiffRowVO,
  type MesProScheduleOrderDailyCompareVO,
  type MesProScheduleOrderIssueActionVO,
  type MesProScheduleOrderPreflightIssueVO,
  type MesProScheduleOrderPreflightReqVO,
  type MesProScheduleOrderPreflightRespVO,
  type MesProScheduleOrderOperationLogVO,
  type MesProScheduleOrderProcessVO,
  type MesProScheduleOrderVO
} from '@/api/mes/pro/scheduleorder'
import {
  ProTaskAutoScheduleApi,
  type ProTaskAutoScheduleIssueVO,
  type ProTaskAutoSchedulePreviewReqVO,
  type ProTaskAutoScheduleApplyRespVO,
  type ProTaskLatestScheduleApplyRespVO,
  type ProTaskAutoScheduleReplanPreviewRespVO
} from '@/api/mes/pro/task/autoSchedule'
import { MesProWorkOrderStatusEnum } from '@/views/mes/utils/constants'
import { checkPermi } from '@/utils/permission'
import { useEmitt } from '@/hooks/web/useEmitt'
import {
  MES_PRO_TASK_GANTT_REFRESH_EVENT,
  MES_SCHEDULE_ORDER_REFRESH_EVENT,
  type MesScheduleOrderRefreshPayload
} from '../shared/scheduleEvents'
import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import {
  useTableMultiFilter,
  type ListMultiFilterDefinition
} from '@/hooks/web/useTableMultiFilter'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import BaseScheduleOrderMainList from './components/ScheduleOrderMainList.vue'
import ScheduleOrderProcessDetail from './components/ScheduleOrderProcessDetail.vue'
import ScheduleOrderReplanDrawer from './components/ScheduleOrderReplanDrawer.vue'
import {
  resolveControlledActionProjection,
  resolveProjectionErrorMessage
} from '@/api/form-center/actionProjection'

defineOptions({ name: 'MesProScheduleOrder' })

type ScheduleOrderTemplateSortOrder = 'ascending' | 'descending' | null

type ScheduleOrderTemplateSortState = {
  key?: string
  prop?: string
  order?: ScheduleOrderTemplateSortOrder
}

type ScheduleOrderTemplateSortableColumn = {
  key: string
  prop: string
  sortable?: boolean | 'custom'
  sortOrders?: ScheduleOrderTemplateSortOrder[]
}

type ScheduleOrderTemplateSortColumnAttrs = (columnKeyOrConfig: string | {
  key: string
  prop?: string
  sortable?: boolean | 'custom'
  sortOrders?: ScheduleOrderTemplateSortOrder[]
}) => {
  sortable: boolean | 'custom'
  sortOrders: ScheduleOrderTemplateSortOrder[]
}

type ScheduleOrderTemplateSortChangeHandler = (payload?: {
  prop?: string
  order?: string | null
  column?: any
}) => void

type ScheduleOrderMainListSlotProps = {
  sortState: ScheduleOrderTemplateSortState
  sortableColumns: ScheduleOrderTemplateSortableColumn[]
  sortableColumnMap: Map<string, ScheduleOrderTemplateSortableColumn>
  sortColumnAttrs: ScheduleOrderTemplateSortColumnAttrs
  handleSortChange: ScheduleOrderTemplateSortChangeHandler
}

const ScheduleOrderMainList = BaseScheduleOrderMainList as typeof BaseScheduleOrderMainList & {
  new (): {
    $slots: {
      actions?: () => any
      table?: (props: ScheduleOrderMainListSlotProps) => any
    }
  }
}

const SCHEDULE_ORDER_STATUS_FINISHED = 3
const SCHEDULE_ORDER_STATUS_CANCELED = 4
const MISSING_MATERIAL_LIST_HINT =
  '未查询到生产用料清单。仍可调整优先级、设置承诺交期和冻结/解冻；入池与手动重排以正式排产检查结果为准。'
const MISSING_CURRENT_PROCESS_HINT =
  '当前列表未解析出可显示的未完成工序，该展示值不作为统一禁用判据。仍可调整优先级、设置承诺交期和冻结/解冻；入池与手动重排以正式排产检查结果为准。'
const { emitter } = useEmitt()

const scheduleOrderTableHeight = '100%'
const scheduleOrderDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '排产工单号', width: 180 },
  { key: 'erpWorkOrderCode', label: '来源生产工单号', width: 180 },
  { key: 'productCode', label: '产品编号', minWidth: 120 },
  { key: 'productName', label: '产品名称', minWidth: 150 },
  { key: 'productSpecification', label: '规格型号', minWidth: 130 },
  { key: 'progressPercent', label: '数量/进度', width: 170 },
  { key: 'promiseDate', label: '承诺交期', width: 130 },
  { key: 'latestStartTime', label: '最晚开工', width: 160 },
  { key: 'plannedStartTime', label: '计划开工', width: 160 },
  { key: 'plannedEndTime', label: '计划完成', width: 160 },
  { key: 'priorityNo', label: '优先级', width: 100 },
  { key: 'productionMaterialList', label: '生产用料清单', minWidth: 190 },
  { key: 'currentProcessName', label: '当前工序', minWidth: 170 },
  { key: 'createTime', label: '创建时间', width: 170 },
  { key: 'operation', label: '操作', width: 140, hideable: false, business: false }
]
const {
  columns: scheduleOrderColumns,
  saving: scheduleOrderColumnSaving,
  isColumnVisible: isScheduleOrderColumnVisible,
  getColumnWidthString: getScheduleOrderColumnWidthString,
  getColumnMinWidthString: getScheduleOrderColumnMinWidthString,
  handleHeaderDragend: handleScheduleOrderHeaderDragend,
  saveConfig: saveScheduleOrderColumnConfig,
  resetConfig: resetScheduleOrderColumnConfig
} = useUserTableColumns('mes.pro.scheduleOrder.main', scheduleOrderDefaultColumns)

const workOrderAdmissionDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'workOrderCode', label: '工单编码', minWidth: 160 },
  { key: 'productCode', label: '产品编号', minWidth: 130 },
  { key: 'productName', label: '产品名称', minWidth: 150 },
  { key: 'productSpecification', label: '规格型号', minWidth: 140 },
  { key: 'quantity', label: '总数量', width: 110 },
  { key: 'requestDate', label: '需求日期', width: 160 },
  { key: 'admissionStatus', label: '入池状态', width: 120 },
  { key: 'message', label: '不可排原因', minWidth: 240 },
  { key: 'ownerRole', label: '建议处理', width: 120 },
  { key: 'operation', label: '操作', width: 190, hideable: false, business: false }
]
const {
  columns: workOrderAdmissionColumns,
  saving: workOrderAdmissionColumnSaving,
  isColumnVisible: isWorkOrderAdmissionColumnVisible,
  getColumnWidthString: getWorkOrderAdmissionColumnWidthString,
  getColumnMinWidthString: getWorkOrderAdmissionColumnMinWidthString,
  handleHeaderDragend: handleWorkOrderAdmissionHeaderDragend,
  saveConfig: saveWorkOrderAdmissionColumnConfig,
  resetConfig: resetWorkOrderAdmissionColumnConfig
} = useUserTableColumns('mes.pro.scheduleOrder.admissionDiff', workOrderAdmissionDefaultColumns)

const processRouteDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'expand', label: '展开', width: 44, hideable: false, business: false },
  { key: 'processCode', label: '工序编号', width: 112, minWidth: 104 },
  { key: 'processName', label: '工序名称', width: 152, minWidth: 136 },
  { key: 'capacityMode', label: '排产策略', width: 112, minWidth: 104 },
  { key: 'hourlyCapacityTotal', label: '小时产能', width: 104, minWidth: 96 },
  { key: 'capacitySource', label: '产能来源', width: 104, minWidth: 96 },
  { key: 'shiftCapacityTotal', label: '班次产能', width: 104, minWidth: 96 },
  { key: 'plannedQuantity', label: '需要多少个', width: 104, minWidth: 96 },
  { key: 'effectiveCompletedQuantity', label: '做了多少个', width: 104, minWidth: 96 },
  { key: 'status', label: '状态', width: 96, minWidth: 88 },
  { key: 'shiftStatus', label: '班次状态', width: 104, minWidth: 96 },
  { key: 'feedbackCount', label: '报工次数', width: 88, minWidth: 80 },
  { key: 'latestFeedbackTime', label: '最近报工时间', width: 146, minWidth: 136 },
  { key: 'estimatedCompletionTime', label: '预计结束', width: 156, minWidth: 146 }
]
const {
  columns: processRouteColumns,
  saving: processRouteColumnSaving,
  isColumnVisible: isProcessRouteColumnVisible,
  getColumnWidthString: getProcessRouteColumnWidthString,
  getColumnMinWidthString: getProcessRouteColumnMinWidthString,
  handleHeaderDragend: handleProcessRouteHeaderDragend,
  saveConfig: saveProcessRouteColumnConfig,
  resetConfig: resetProcessRouteColumnConfig
} = useUserTableColumns('mes.pro.scheduleOrder.processRoute', processRouteDefaultColumns)

const feedbackHistoryDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '报工单号', width: 160, minWidth: 140 },
  { key: 'feedbackTime', label: '报工时间', width: 170, minWidth: 150 },
  { key: 'feedbackQuantity', label: '本次数量', width: 110, minWidth: 96 },
  { key: 'qualifiedQuantity', label: '合格数', width: 100, minWidth: 88 },
  { key: 'feedbackUserNickname', label: '报工人', width: 120, minWidth: 104 }
]
const {
  columns: feedbackHistoryColumns,
  getColumnWidthString: getFeedbackHistoryColumnWidthString,
  getColumnMinWidthString: getFeedbackHistoryColumnMinWidthString,
  handleHeaderDragend: handleFeedbackHistoryHeaderDragend
} = useUserTableColumns('mes.pro.scheduleOrder.feedbackHistory', feedbackHistoryDefaultColumns)
const feedbackHistoryTemplateQueryModel = {}
const feedbackHistoryQuickFilterDefinitions: TableQuickFilterDefinition[] = []
const feedbackHistoryQuickFilterState = {}
const feedbackHistoryOperatorOptions = []
const scheduleOrderViewListQueryModel = {}
const scheduleOrderViewListFilterDefinitions: TableQuickFilterDefinition[] = []
const scheduleOrderViewListQuickFilterState = {}
const scheduleOrderViewListOperatorOptions = []

const dailyCompareDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'planDate', label: '日期', width: 130, minWidth: 118 },
  { key: 'plannedQuantity', label: '计划数量', width: 130, minWidth: 112 },
  { key: 'actualQuantity', label: '实际报工', width: 130, minWidth: 112 },
  { key: 'diffQuantity', label: '差异', width: 130, minWidth: 112 },
  { key: 'status', label: '状态', width: 120, minWidth: 104 },
  { key: 'remark', label: '偏差说明', minWidth: 220 }
]
const {
  columns: dailyCompareColumns,
  isColumnVisible: isDailyCompareColumnVisible,
  getColumnWidthString: getDailyCompareColumnWidthString,
  getColumnMinWidthString: getDailyCompareColumnMinWidthString,
  handleHeaderDragend: handleDailyCompareHeaderDragend
} = useUserTableColumns('mes.pro.scheduleOrder.dailyCompare', dailyCompareDefaultColumns)

const operationLogDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'expand', label: '展开', width: 48, minWidth: 44, hideable: false, business: false },
  { key: 'operationType', label: '操作类型', width: 120, minWidth: 108 },
  { key: 'operatorName', label: '操作人', width: 120, minWidth: 104 },
  { key: 'createTime', label: '操作时间', width: 170, minWidth: 150 },
  { key: 'scheduleOrderCode', label: '排产工单号', width: 180, minWidth: 160 },
  { key: 'reason', label: '操作原因', minWidth: 240 },
  { key: 'diffCount', label: '字段差异', width: 100, minWidth: 90 }
]
const {
  columns: operationLogColumns,
  isColumnVisible: isOperationLogColumnVisible,
  getColumnWidthString: getOperationLogColumnWidthString,
  getColumnMinWidthString: getOperationLogColumnMinWidthString,
  handleHeaderDragend: handleOperationLogHeaderDragend
} = useUserTableColumns('mes.pro.scheduleOrder.operationLog', operationLogDefaultColumns)

const operationLogDiffDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'fieldLabel', label: '字段', width: 150, minWidth: 130 },
  { key: 'beforeValue', label: '旧值', minWidth: 260 },
  { key: 'afterValue', label: '新值', minWidth: 260 }
]
const {
  columns: operationLogDiffColumns,
  isColumnVisible: isOperationLogDiffColumnVisible,
  getColumnWidthString: getOperationLogDiffColumnWidthString,
  getColumnMinWidthString: getOperationLogDiffColumnMinWidthString,
  handleHeaderDragend: handleOperationLogDiffHeaderDragend
} = useUserTableColumns('mes.pro.scheduleOrder.operationLogDiff', operationLogDiffDefaultColumns)

const message = useMessage()
const router = useRouter()
const route = useRoute()
const scheduleOrderLoading = ref(false)
const scheduleOrderTableRef = ref<{ clearSelection?: () => void }>()
const scheduleOrderList = ref<MesProScheduleOrderVO[]>([])
const selectedScheduleOrders = ref<MesProScheduleOrderVO[]>([])
const scheduleOrderTotal = ref(0)
const scheduleOrderExportVisible = ref(false)
const scheduleOrderExporting = ref(false)
const scheduleOrderExportColumnOptions = [
  { key: 'code', label: '排产工单号' },
  { key: 'erpWorkOrderCode', label: '来源生产工单号' },
  { key: 'productCode', label: '产品编号' },
  { key: 'productName', label: '产品名称' },
  { key: 'productSpecification', label: '规格型号' },
  { key: 'quantityProgress', label: '数量/进度' },
  { key: 'promiseDate', label: '承诺交期' },
  { key: 'latestStartTime', label: '最晚开工' },
  { key: 'plannedStartTime', label: '计划开工' },
  { key: 'plannedEndTime', label: '计划完成' },
  { key: 'priorityNo', label: '优先级' },
  { key: 'productionMaterialListSummary', label: '生产用料清单' },
  { key: 'currentProcessName', label: '当前工序' },
  { key: 'createTime', label: '创建时间' }
]
const defaultScheduleOrderExportColumns = scheduleOrderExportColumnOptions.map((column) => column.key)
const scheduleOrderExportColumns = ref<string[]>([...defaultScheduleOrderExportColumns])
const scheduleOrderQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined as string | undefined,
  erpWorkOrderCode: undefined as string | undefined,
  currentProcessId: undefined as number | undefined,
  completionFilter: undefined as 'INCOMPLETE' | 'ALL' | 'COMPLETED' | undefined,
  promiseDate: undefined as string[] | undefined,
  quickFilter: undefined as any
})
const scheduleOrderCompletionFilterOptions = [
  { label: '未完成', value: 'INCOMPLETE' },
  { label: '全部', value: 'ALL' },
  { label: '已完成', value: 'COMPLETED' }
]
const scheduleOrderQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'code', label: '排产工单号', type: 'text', placeholder: '请输入排产工单号' },
  {
    key: 'completionFilter',
    label: '完成状态',
    type: 'select',
    queryParamKey: 'completionFilter',
    options: scheduleOrderCompletionFilterOptions
  },
  { key: 'erpWorkOrderCode', label: '来源生产工单号', type: 'text', placeholder: '请输入来源生产工单号' },
  { key: 'productName', label: '产品名称', type: 'text', placeholder: '请输入产品名称' },
  { key: 'productSpecification', label: '规格型号', type: 'text', placeholder: '请输入规格型号' },
  { key: 'promiseDate', label: '承诺交期', type: 'dateRange' }
]
const scheduleOrderMultiFilterDefinitions: ListMultiFilterDefinition[] = [
  {
    key: 'code',
    label: '排产工单号',
    type: 'text',
    queryParamKey: 'code',
    placeholder: '请输入排产工单号'
  },
  {
    key: 'erpWorkOrderCode',
    label: '来源生产工单号',
    type: 'text',
    queryParamKey: 'erpWorkOrderCode',
    placeholder: '请输入来源生产工单号'
  },
  {
    key: 'completionFilter',
    label: '完成状态',
    type: 'select',
    queryParamKey: 'completionFilter',
    options: scheduleOrderCompletionFilterOptions
  },
  {
    key: 'promiseDate',
    label: '承诺交期',
    type: 'dateRange',
    queryParamKey: 'promiseDate'
  }
]

const processDialogVisible = ref(false)
const processLoading = ref(false)
const processList = ref<MesProScheduleOrderProcessVO[]>([])
const processRouteQuickFilterParams = reactive<{
  pageNo: number
  pageSize: number
  quickFilter?: TableQuickFilterValue
}>({
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined
})
const processRouteQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'processCode', label: '工序编号', type: 'text', placeholder: '请输入工序编号' },
  { key: 'processName', label: '工序名称', type: 'text', placeholder: '请输入工序名称' },
  {
    key: 'shiftStatus',
    label: '班次状态',
    type: 'select',
    options: [
      { label: '白班', value: '白班' },
      { label: '夜班', value: '夜班' }
    ]
  },
  { key: 'estimatedCompletionTime', label: '预计完成时间', type: 'dateRange' }
]
const dailyCompareDialogVisible = ref(false)
const dailyCompareLoading = ref(false)
const dailyCompareList = ref<MesProScheduleOrderDailyCompareVO[]>([])
const dailyCompareRange = ref<string[] | undefined>()
const currentScheduleOrder = ref<MesProScheduleOrderVO>()
const priorityDialogVisible = ref(false)
const prioritySaving = ref(false)
const priorityTarget = ref<MesProScheduleOrderVO>()
const priorityForm = reactive({
  id: undefined as number | undefined,
  priorityNo: 1
})
const promiseDateDialogVisible = ref(false)
const promiseDateSaving = ref(false)
const promiseDateTarget = ref<MesProScheduleOrderVO>()
const promiseDateForm = reactive({
  id: undefined as number | undefined,
  promiseDate: '',
  priorityNo: 1,
  remark: '',
  reason: ''
})
const freezeDialogVisible = ref(false)
const unfreezeDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const batchActionSaving = ref(false)
const batchActionRows = ref<MesProScheduleOrderVO[]>([])
const batchActionReason = ref('')
const manualFinishDialogVisible = ref(false)
const manualFinishSaving = ref(false)
const manualFinishTarget = ref<MesProScheduleOrderVO>()
const manualFinishReason = ref('')
const manualFinishDialogMode = ref<'MANUAL_FINISH' | 'REVOKE_MANUAL_FINISH'>('MANUAL_FINISH')
const latestSuccessfulScheduleApply = ref<ProTaskLatestScheduleApplyRespVO | null>(null)
const latestSuccessfulScheduleApplyLoading = ref(false)
const latestSuccessfulScheduleApplyError = ref('')
const operationLogDialogVisible = ref(false)
const operationLogLoading = ref(false)
const operationLogList = ref<MesProScheduleOrderOperationLogVO[]>([])
const operationLogTarget = ref<MesProScheduleOrderVO>()
const scheduleOrderActiveTab = ref<'scheduleOrders' | 'workOrderAdmission'>('scheduleOrders')
const workOrderAdmissionLoading = ref(false)
const workOrderAdmissionSaving = ref(false)
const workOrderAdmissionList = ref<MesProScheduleOrderAdmissionDiffRowVO[]>([])
const selectedWorkOrders = ref<MesProScheduleOrderAdmissionDiffRowVO[]>([])
const workOrderAdmissionTotal = ref(0)
let workOrderAdmissionRequestSerial = 0
const workOrderAdmissionQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  workOrderCode: undefined as string | undefined,
  productCode: undefined as string | undefined,
  admissionStatus: undefined as string | undefined,
  requestDate: undefined as string[] | undefined
})
const workOrderAdmissionMultiFilterDefinitions: ListMultiFilterDefinition[] = [
  {
    key: 'workOrderCode',
    label: '工单编码',
    type: 'text',
    queryParamKey: 'workOrderCode',
    placeholder: '请输入工单编码'
  },
  {
    key: 'productCode',
    label: '产品编号',
    type: 'text',
    queryParamKey: 'productCode',
    placeholder: '请输入产品编号'
  },
  {
    key: 'admissionStatus',
    label: '入池状态',
    type: 'select',
    queryParamKey: 'admissionStatus',
    options: [
      { label: '可入池', value: 'READY_TO_ADMIT' },
      { label: '已入池', value: 'ALREADY_ADMITTED' },
      { label: '阻断', value: 'BLOCKED' }
    ]
  },
  {
    key: 'requestDate',
    label: '需求日期',
    type: 'dateRange',
    queryParamKey: 'requestDate'
  }
]
const replanDrawerVisible = ref(false)
const replanSettingsDialogVisible = ref(false)
const preflightLoading = ref(false)
const replanPreviewLoading = ref(false)
const replanApplyLoading = ref(false)
const replanApplyProgressVisible = ref(false)
const replanApplyProgressPercent = ref(0)
const preflightResult = ref<MesProScheduleOrderPreflightRespVO | null>(null)
const lastPreflightRequest = ref<MesProScheduleOrderPreflightReqVO | null>(null)
const replanPreview = ref<ProTaskAutoScheduleReplanPreviewRespVO | null>(null)
const lastReplanRequest = ref<ProTaskAutoSchedulePreviewReqVO | null>(null)
const lastReplanParticipatingScheduleOrderIds = ref<number[]>([])
const materialShortageDialogVisible = ref(false)
const replanFeedbackProtectionDialogVisible = ref(false)
const replanStartDateDialogVisible = ref(false)
const replanStartDate = ref('')
const replanForm = reactive({
  startTime: '',
  runtimeCapacityBasis: 'PLANNED' as 'PLANNED' | 'ACTUAL',
  preserveManualLockedTasks: true,
  reason: ''
})

const runtimeCapacityBasisDifferenceText = computed(() => {
  const selectedModeText =
    replanForm.runtimeCapacityBasis === 'PLANNED' ? '当前按计划产能预估。' : '当前按实际产能预估。'
  return `${selectedModeText}计划产能是排产日历中维护的班次可用产能；实际产能是根据已报工或实际完成记录统计出的真实产出能力。`
})

const replanScopeRows = computed(() => selectedScheduleOrders.value)
const replanScopeSummaryText = computed(() => {
  const count = replanScopeRows.value.length
  return count
    ? `当前仅重排已勾选的 ${count} 个排产工单，未勾选工单不会参与检查、预览或应用。`
    : '请先在排产工单列表勾选需要重排的工单；未明确选择时禁止检查、预览或应用。'
})

const getDefaultReplanStartDate = () => dayjs().add(1, 'day').format('YYYY-MM-DD')

const buildWholeDayReplanStartTime = (date: string): string => {
  const startDate = dayjs(date)
  if (!startDate.isValid()) {
    throw new Error('请选择有效的重排开始日期')
  }
  return startDate.startOf('day').format('YYYY-MM-DD HH:mm:ss')
}

const replanStartDateStartTime = computed(() =>
  replanStartDate.value ? buildWholeDayReplanStartTime(replanStartDate.value) : '未选择'
)

const buildReplanRequest = (startTime?: string): ProTaskAutoSchedulePreviewReqVO => {
  const resolvedStartTime = startTime || buildWholeDayReplanStartTime(replanForm.startTime)
  if (!resolvedStartTime) {
    throw new Error('请选择重排开始日期')
  }
  const scopeRows = replanScopeRows.value
  const scheduleOrderIds = scopeRows.map((item) => item.id).filter(Boolean)
  if (!scheduleOrderIds.length) {
    throw new Error('请先勾选需要重排的排产工单')
  }
  const blockedRows = scopeRows.filter((item) => !isScheduleOrderReplanable(item))
  if (blockedRows.length) {
    throw new Error(
      `排产工单不可重排：${blockedRows
        .map((item) => `${item.code || item.id}（${getScheduleOrderReplanBlockReason(item)}）`)
        .join('，')}`
    )
  }
  return {
    scheduleOrderIds,
    startTime: resolvedStartTime,
    runtimeCapacityBasis: replanForm.runtimeCapacityBasis,
    preserveManualLockedTasks: replanForm.preserveManualLockedTasks
  }
}

const buildPreflightRequestByReplanRequest = (
  request: ProTaskAutoSchedulePreviewReqVO
): MesProScheduleOrderPreflightReqVO => {
  return {
    scopeType: 'SELECTED',
    scheduleOrderIds: request.scheduleOrderIds,
    includeAdmissionDiff: false,
    startTime: request.startTime,
    capacityMode: request.runtimeCapacityBasis
  }
}

const replanPreviewStale = computed(() => {
  const currentRequest = buildReplanRequestIfReady()
  return Boolean(
    replanPreview.value &&
      lastReplanRequest.value &&
      (!currentRequest ||
        JSON.stringify(lastReplanRequest.value) !== JSON.stringify(currentRequest))
  )
})

const preflightStale = computed(() => {
  const currentRequest = buildPreflightRequestIfReady()
  return Boolean(
    preflightResult.value &&
      lastPreflightRequest.value &&
      (!currentRequest ||
        JSON.stringify(lastPreflightRequest.value) !== JSON.stringify(currentRequest))
  )
})

const preflightHasBlockedIssue = computed(() => {
  return Boolean(
    preflightResult.value?.issues?.some(
      (issue: MesProScheduleOrderPreflightIssueVO) => issue.severity === 'BLOCKED'
    )
  )
})

const isPreflightIssueAttributableToWorkOrder = (
  issue: MesProScheduleOrderPreflightIssueVO
) => {
  if (issue.workOrderId || issue.scheduleOrderId || issue.workOrderCode || issue.scheduleOrderCode) {
    return true
  }
  return (
    Boolean(issue.objectId) &&
    ['WORK_ORDER', 'SCHEDULE_ORDER'].includes(String(issue.objectType || '').toUpperCase())
  )
}

const preflightHasGlobalBlockedIssue = computed(() => {
  return Boolean(
    preflightResult.value?.issues?.some(
      (issue: MesProScheduleOrderPreflightIssueVO) =>
        issue.severity === 'BLOCKED' && !isPreflightIssueAttributableToWorkOrder(issue)
    )
  )
})

const findScheduleOrderByPreflightIssue = (issue: MesProScheduleOrderPreflightIssueVO) => {
  if (!issue.scheduleOrderId) {
    return undefined
  }
  const scheduleOrderId = Number(issue.scheduleOrderId)
  return (
    selectedScheduleOrders.value.find((item) => Number(item.id) === scheduleOrderId) ||
    scheduleOrderList.value.find((item) => Number(item.id) === scheduleOrderId)
  )
}

const getPreflightIssueProductName = (issue: MesProScheduleOrderPreflightIssueVO) => {
  return issue.productName || findScheduleOrderByPreflightIssue(issue)?.productName || '-'
}

const getPreflightIssueProductCode = (issue: MesProScheduleOrderPreflightIssueVO) => {
  return (
    issue.productCode ||
    findScheduleOrderByPreflightIssue(issue)?.productCode ||
    (issue.productId ? String(issue.productId) : '-')
  )
}

const replanPreviewHasBlockedIssue = computed(() => {
  return Boolean(
    (replanPreview.value?.summary?.blockingIssueCount ?? 0) > 0 ||
      replanPreview.value?.issues?.some((issue) => issue.severity === 'BLOCKING')
  )
})

const isAutoScheduleIssueAttributableToWorkOrder = (issue: ProTaskAutoScheduleIssueVO) => {
  return Boolean(issue.workOrderId || issue.workOrderCode)
}

const hasGlobalReplanBlockingIssue = (
  preview: ProTaskAutoScheduleReplanPreviewRespVO | null | undefined
) => {
  const blockingIssues = (preview?.issues || []).filter((issue) => issue.severity === 'BLOCKING')
  if ((preview?.summary?.blockingIssueCount ?? 0) > 0 && !blockingIssues.length) {
    return true
  }
  return blockingIssues.some((issue) => !isAutoScheduleIssueAttributableToWorkOrder(issue))
}

const replanPreviewHasGlobalBlockedIssue = computed(() => {
  return hasGlobalReplanBlockingIssue(replanPreview.value)
})

const replanFeedbackProtectedTasks = computed(() => {
  return (replanPreview.value?.protectedTasks || []).filter(
    (task) => task.protectionReason === 'FEEDBACK'
  )
})

const replanFeedbackProtectionCount = computed(() => replanFeedbackProtectedTasks.value.length)

const validateMaterialShortageIssue = (issue: ProTaskAutoScheduleIssueVO) => {
  if (
    !issue.materialName ||
    !issue.materialCode ||
    issue.shortageQty === undefined ||
    issue.shortageQty === null
  ) {
    throw new Error('物料缺料接口缺少物料名称、编码或缺少数量')
  }
  return issue
}

const materialShortageIssues = computed(() => {
  return (replanPreview.value?.issues || [])
    .filter((issue) => issue.issueType === 'MATERIAL')
    .map(validateMaterialShortageIssue)
})

type ReplanIssueRow = {
  key: string
  issueType: string
  severity: string
  problem: string
  remarkParts: string[]
  sourceIssue?: ProTaskAutoScheduleIssueVO
}

const replanIssueRows = computed<ReplanIssueRow[]>(() => {
  const issues = replanPreview.value?.issues || []
  const rows: ReplanIssueRow[] = []
  const materialIssues = issues
    .filter((issue) => issue.issueType === 'MATERIAL')
    .map(validateMaterialShortageIssue)
  if (materialIssues.length) {
    rows.push({
      key: 'MATERIAL',
      issueType: 'MATERIAL',
      severity: resolveIssueSeverity(materialIssues),
      problem: '物料缺料',
      remarkParts: []
    })
  }

  const latestStartIssues = issues.filter((issue) => issue.issueType === 'LATEST_START')
  if (latestStartIssues.length) {
    rows.push({
      key: 'LATEST_START',
      issueType: 'LATEST_START',
      severity: resolveIssueSeverity(latestStartIssues),
      problem: '计划开工时间晚于最晚开工时间',
      remarkParts: [`工单：${formatIssueWorkOrders(latestStartIssues)}`]
    })
  }

  const materialDemandIssues = issues.filter((issue) => issue.issueType === 'MATERIAL_DEMAND')
  if (materialDemandIssues.length) {
    rows.push({
      key: 'MATERIAL_DEMAND',
      issueType: 'MATERIAL_DEMAND',
      severity: resolveIssueSeverity(materialDemandIssues),
      problem: '工单缺少物料需求',
      remarkParts: [`工单：${formatIssueWorkOrders(materialDemandIssues)}`]
    })
  }

  issues
    .filter((issue) => !['MATERIAL', 'LATEST_START', 'MATERIAL_DEMAND'].includes(issue.issueType))
    .forEach((issue, index) => {
      rows.push({
        key: `${issue.issueType}-${issue.id || index}`,
        issueType: issue.issueType,
        severity: issue.severity,
        problem: issue.message || issue.issueType,
        remarkParts: buildIssueRemarkParts(issue),
        sourceIssue: issue
      })
    })

  return rows
})

type SkippedSelectedReplanRow = {
  code: string
  reason: string
}

const escapeHtml = (value: string | number | undefined | null) => {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const buildSkippedSelectedReplanRows = (preview: ProTaskAutoScheduleReplanPreviewRespVO) => {
  const taskWorkOrderIds = new Set(
    (preview.tasks || [])
      .map((task: any) => String(task.parent || ''))
      .filter((parent) => parent.startsWith('301_'))
      .map((parent) => Number(parent.replace('301_', '')))
      .filter((workOrderId) => Number.isFinite(workOrderId))
  )
  const issues = preview.issues || []
  return selectedScheduleOrders.value
    .filter((row) => row.workOrderId && !taskWorkOrderIds.has(Number(row.workOrderId)))
    .map((row) => {
      const matchedIssues = issues.filter((issue) => issue.workOrderId === row.workOrderId)
      const reason = matchedIssues.length
        ? matchedIssues
            .map((issue) => issue.message || issue.issueType)
            .filter(Boolean)
            .join('；')
        : '预览未生成任务，请检查路线、日历产能或已保护任务'
      return {
        code: row.erpWorkOrderCode || row.code,
        reason
      }
    })
}

const notifySkippedSelectedReplanRows = (preview: ProTaskAutoScheduleReplanPreviewRespVO) => {
  const skippedRows = buildSkippedSelectedReplanRows(preview)
  if (!skippedRows.length) {
    return
  }
  const rowHtml = skippedRows
    .slice(0, 6)
    .map(
      (row, index) =>
        `<li><strong>${index + 1}. 工单：${escapeHtml(row.code)}</strong>；原因：${escapeHtml(
          row.reason
        )}</li>`
    )
    .join('')
  const moreText =
    skippedRows.length > 6
      ? `<p>另有 ${skippedRows.length - 6} 个工单未参与，请在标红行查看原因。</p>`
      : ''
  ElNotification({
    title: '存在未参与排产的工单',
    message: `<div class="schedule-order-pool__skipped-notice">
        <p>以下工单本次被阻断：</p>
        <ul>${rowHtml}</ul>
        ${moreText}
        <p>系统将直接应用其余可排工单，阻断工单会标红。</p>
      </div>`,
    dangerouslyUseHTMLString: true,
    type: 'warning',
    duration: 9000
  })
}

const canApplyReplan = computed(() => {
  return Boolean(
      replanProjectionState.value.allowed &&
      !replanPreviewLoading.value &&
      !replanApplyLoading.value
  )
})
const resolveScheduleReplanProjection = () => {
  const scopeRows = replanScopeRows.value
  const blockedRows = scopeRows.filter((item) => !isScheduleOrderReplanable(item))
  const blockedScopeReason = blockedRows.length
    ? `排产工单不可重排：${blockedRows
        .map((item) => `${item.code || item.id}（${getScheduleOrderReplanBlockReason(item)}）`)
        .join('，')}`
    : ''
  const blockerMessage =
    (!hasReplanPermission.value && '当前账号没有手动重排权限') ||
    (!scopeRows.length && '请先勾选需要重排的排产工单') ||
    blockedScopeReason ||
    (preflightHasGlobalBlockedIssue.value &&
      '排产前检查存在无法归因到工单的阻断问题，不能应用重排') ||
    (replanPreviewHasGlobalBlockedIssue.value &&
      '重排预览存在无法归因到工单的阻断问题，不能应用重排') ||
    '当前重排动作暂不可用。'
  return resolveControlledActionProjection(
    {
      actionCode: 'REPLAN_APPLY',
      actionLabel: '手动重排',
      allowed:
        hasReplanPermission.value &&
        scopeRows.length > 0 &&
        blockedRows.length === 0 &&
        !preflightHasGlobalBlockedIssue.value &&
        !replanPreviewHasGlobalBlockedIssue.value,
      permissionGranted: hasReplanPermission.value,
      locked: preflightHasGlobalBlockedIssue.value || replanPreviewHasGlobalBlockedIssue.value,
      lockReason: blockerMessage,
      disabledReason: blockerMessage
    },
    '手动重排'
  )
}
const replanProjectionState = computed(resolveScheduleReplanProjection)
const scheduleReplanActionProjection = computed(() => replanProjectionState.value)

let replanApplyProgressTimer: number | null = null

const clearReplanApplyProgressTimer = () => {
  if (!replanApplyProgressTimer) {
    return
  }
  window.clearInterval(replanApplyProgressTimer)
  replanApplyProgressTimer = null
}

const resetReplanApplyProgress = () => {
  clearReplanApplyProgressTimer()
  replanApplyProgressVisible.value = false
  replanApplyProgressPercent.value = 0
}

const startReplanApplyProgress = () => {
  clearReplanApplyProgressTimer()
  replanApplyProgressVisible.value = true
  replanApplyProgressPercent.value = 8
  replanApplyProgressTimer = window.setInterval(() => {
    if (replanApplyProgressPercent.value >= 90) {
      return
    }
    const step = Math.max(1, Math.ceil((90 - replanApplyProgressPercent.value) * 0.18))
    replanApplyProgressPercent.value = Math.min(90, replanApplyProgressPercent.value + step)
  }, 400)
}

const finishReplanApplyProgress = async () => {
  clearReplanApplyProgressTimer()
  replanApplyProgressVisible.value = true
  replanApplyProgressPercent.value = 100
  await new Promise<void>((resolve) => window.setTimeout(resolve, 650))
}

const showReplanApplyProgress = computed(
  () =>
    replanApplyLoading.value ||
    replanApplyProgressVisible.value ||
    replanApplyProgressPercent.value > 0
)
const hasReplanPermission = computed(() => checkPermi(['mes:pro-auto-schedule:replan']))
const latestSuccessfulScheduleApplyTimeText = computed(() => {
  if (latestSuccessfulScheduleApplyLoading.value) {
    return '加载中'
  }
  if (latestSuccessfulScheduleApplyError.value) {
    return '加载失败'
  }
  if (latestSuccessfulScheduleApply.value?.hasData && latestSuccessfulScheduleApply.value?.appliedAt) {
    return formatDateTime(latestSuccessfulScheduleApply.value.appliedAt)
  }
  return '暂无成功排产'
})
const latestSuccessfulScheduleApplyTooltip = computed(() => {
  if (latestSuccessfulScheduleApplyError.value) {
    return latestSuccessfulScheduleApplyError.value
  }
  if (latestSuccessfulScheduleApply.value?.hasData && latestSuccessfulScheduleApply.value?.appliedAt) {
    const operationTypeText =
      latestSuccessfulScheduleApply.value.operationType === 'AUTO_APPLY' ? '自动排产' : '手动重排'
    const operator = latestSuccessfulScheduleApply.value.operatorName
      ? `，操作人：${latestSuccessfulScheduleApply.value.operatorName}`
      : ''
    return `来自最近一次${operationTypeText}成功事件${operator}`
  }
  return '暂无成功排产记录'
})
const operationLogSummary = computed(() => {
  const latestLog = operationLogList.value[0]
  return {
    scheduleOrderCode:
      latestLog?.scheduleOrderCode ||
      operationLogTarget.value?.code ||
      operationLogTarget.value?.erpWorkOrderCode ||
      '-',
    totalCount: operationLogList.value.length,
    latestOperationType: latestLog ? getOperationTypeText(latestLog.operationType) : '-',
    latestTime: latestLog ? formatDateTime(latestLog.createTime) : '-'
  }
})

const PROTECTION_REASON_TEXT_MAP: Record<string, string> = {
  FEEDBACK: '已报工',
  FINISHED: '已完成',
  IN_PROGRESS: '进行中',
  LOCKED: '已锁定',
  MANUAL: '人工任务'
}

const formatProtectedTaskLabel = (task: {
  workOrderCode?: string
  processName?: string
  taskCode?: string
}) => {
  const workOrderCode = String(task.workOrderCode || '').trim()
  const processName = String(task.processName || '').trim()
  if (workOrderCode && processName) {
    return `${workOrderCode} / ${processName}`
  }
  if (workOrderCode) {
    return workOrderCode
  }
  if (processName) {
    return processName
  }
  return String(task.taskCode || '-').trim() || '-'
}

const formatProtectionReason = (reason?: string) => {
  const normalized = String(reason || '').trim()
  if (!normalized) {
    return '-'
  }
  return PROTECTION_REASON_TEXT_MAP[normalized] || normalized
}

const buildReplanRequestIfReady = (): ProTaskAutoSchedulePreviewReqVO | undefined => {
  if (!replanForm.startTime) {
    return undefined
  }
  const scopeRows = replanScopeRows.value
  const scheduleOrderIds = scopeRows.map((item) => item.id).filter(Boolean)
  if (!scheduleOrderIds.length) {
    return undefined
  }
  if (scopeRows.some((item) => !isScheduleOrderReplanable(item))) {
    return undefined
  }
  return {
    scheduleOrderIds,
    startTime: buildWholeDayReplanStartTime(replanForm.startTime),
    runtimeCapacityBasis: replanForm.runtimeCapacityBasis,
    preserveManualLockedTasks: replanForm.preserveManualLockedTasks
  }
}

const buildPreflightRequestIfReady = (): MesProScheduleOrderPreflightReqVO | undefined => {
  const currentRequest = buildReplanRequestIfReady()
  if (!currentRequest) {
    return undefined
  }
  return {
    scopeType: 'SELECTED',
    scheduleOrderIds: currentRequest.scheduleOrderIds,
    includeAdmissionDiff: false,
    startTime: currentRequest.startTime,
    capacityMode: currentRequest.runtimeCapacityBasis
  }
}

const getScheduleOrderList = async () => {
  scheduleOrderLoading.value = true
  try {
    const data = await MesProScheduleOrderApi.getScheduleOrderPage(scheduleOrderQueryParams)
    scheduleOrderList.value = sortScheduleOrderListForDisplay(data.list || [])
    scheduleOrderTotal.value = data.total
  } finally {
    scheduleOrderLoading.value = false
  }
}

async function loadLatestSuccessfulScheduleApplyTime() {
  latestSuccessfulScheduleApplyLoading.value = true
  latestSuccessfulScheduleApplyError.value = ''
  try {
    latestSuccessfulScheduleApply.value = await ProTaskAutoScheduleApi.getLatestSuccessfulScheduleApply()
  } catch (error) {
    latestSuccessfulScheduleApply.value = null
    latestSuccessfulScheduleApplyError.value = `加载最近成功排产时间失败：${error instanceof Error ? error.message : String(error)}`
    console.error('[MES] 加载最近成功排产时间失败', error)
    message.error(latestSuccessfulScheduleApplyError.value)
  } finally {
    latestSuccessfulScheduleApplyLoading.value = false
  }
}
const scheduleOrderQuickFilter = useTableQuickFilter(
  'mes.pro.scheduleOrder.main',
  scheduleOrderQuickFilterDefinitions,
  scheduleOrderQueryParams,
  getScheduleOrderList
)
const scheduleOrderMultiFilter = useTableMultiFilter(
  'mes.pro.scheduleOrder.main',
  scheduleOrderMultiFilterDefinitions,
  scheduleOrderQueryParams,
  getScheduleOrderList
)

const applyProcessRouteQuickFilter = async () => {
  processRouteQuickFilterParams.pageNo = 1
}

const handleProcessRoutePagination = () => {
  processRouteQuickFilterParams.pageNo = processRouteQuickFilterParams.pageNo || 1
}

const processRouteQuickFilter = useTableQuickFilter(
  'mes.pro.scheduleOrder.processRoute',
  processRouteQuickFilterDefinitions,
  processRouteQuickFilterParams,
  applyProcessRouteQuickFilter
)

const normalizeProcessRouteFilterText = (value: unknown) => String(value ?? '').trim().toLowerCase()

const processCapacityModeTextMap: Record<string, string> = {
  RESOURCE_CALCULATED: '资源计算',
  MANUAL_OVERRIDE: '产能覆盖',
  FINITE_HOURLY: '小时产能',
  INFINITE_FORMULA: '无限公式'
}

const capacitySourceTextMap: Record<string, string> = {
  MACHINE: '设备',
  WORKER: '人工',
  MANUAL_OVERRIDE: '产能覆盖',
  INFINITE_FORMULA: '无限公式',
  UNCONFIGURED: '未配置'
}

type ProcessResourceSnapshot = {
  capacitySource?: string
  hourlyCapacityTotal?: number
  shiftCapacityTotal?: number
  workstationRows?: Array<Record<string, any>>
  resources?: Array<Record<string, any>>
}

const getProcessCapacityModeText = (mode?: string) =>
  mode ? processCapacityModeTextMap[mode] || mode : '-'

const getCapacitySourceText = (source?: string) =>
  source ? capacitySourceTextMap[source] || source : '-'

const parseProcessResourceSnapshot = (row: MesProScheduleOrderProcessVO) => {
  if (!row.resourceSnapshotJson) return null
  const snapshot = JSON.parse(row.resourceSnapshotJson)
  return snapshot && typeof snapshot === 'object' ? (snapshot as ProcessResourceSnapshot) : null
}

const getProcessResourceRows = (row: MesProScheduleOrderProcessVO) => {
  const snapshot = parseProcessResourceSnapshot(row)
  if (!snapshot) return []
  if (Array.isArray(snapshot.workstationRows)) return snapshot.workstationRows
  if (Array.isArray(snapshot.resources)) return snapshot.resources
  return []
}

const getProcessRouteShiftStatusText = (row: MesProScheduleOrderProcessVO) => {
  if (row.nightShiftEnabled === true) return '夜班'
  if (row.nightShiftEnabled === false) return '白班'
  return '-'
}

const getProcessRouteEstimatedCompletionTime = (row: MesProScheduleOrderProcessVO) =>
  formatDateTime(row.plannedEndTime)

const processRouteMatchedList = computed(() => {
  const quickFilter = processRouteQuickFilterParams.quickFilter
  if (!quickFilter) return processList.value
  return processList.value.filter((item) => {
    if (quickFilter.fieldKey === 'estimatedCompletionTime') {
      if (!item.plannedEndTime || !quickFilter.value || !quickFilter.valueEnd) return false
      const timestamp = dayjs(item.plannedEndTime).valueOf()
      const start = dayjs(String(quickFilter.value)).startOf('day').valueOf()
      const end = dayjs(String(quickFilter.valueEnd)).endOf('day').valueOf()
      return timestamp >= start && timestamp <= end
    }
    if (quickFilter.fieldKey === 'shiftStatus') {
      return getProcessRouteShiftStatusText(item) === String(quickFilter.value)
    }
    const actual = normalizeProcessRouteFilterText(
      item[quickFilter.fieldKey as keyof MesProScheduleOrderProcessVO]
    )
    const expected = normalizeProcessRouteFilterText(quickFilter.value)
    if (!expected) return true
    if (quickFilter.operator === 'eq') return actual === expected
    return actual.includes(expected)
  })
})

const processRouteFilteredTotal = computed(() => processRouteMatchedList.value.length)

const processRouteFilteredList = computed(() => {
  const pageNo = Math.max(1, Number(processRouteQuickFilterParams.pageNo || 1))
  const pageSize = Math.max(1, Number(processRouteQuickFilterParams.pageSize || 10))
  const start = (pageNo - 1) * pageSize
  return processRouteMatchedList.value.slice(start, start + pageSize)
})

const getScheduleOrderDisplaySortWeight = (row: MesProScheduleOrderVO) => {
  if (row.manualFinished || row.status === SCHEDULE_ORDER_STATUS_FINISHED) {
    return 3
  }
  if (row.frozen) {
    return 2
  }
  return row.plannedStartTime || row.plannedEndTime ? 1 : 0
}

const sortScheduleOrderListForDisplay = (rows: MesProScheduleOrderVO[]) => {
  return [...rows].sort((left, right) => {
    const weightDiff =
      getScheduleOrderDisplaySortWeight(left) - getScheduleOrderDisplaySortWeight(right)
    if (weightDiff !== 0) {
      return weightDiff
    }
    const leftPriority = Number(left.priorityNo || 0)
    const rightPriority = Number(right.priorityNo || 0)
    if (leftPriority !== rightPriority) {
      return leftPriority - rightPriority
    }
    return String(left.code || left.erpWorkOrderCode || '').localeCompare(
      String(right.code || right.erpWorkOrderCode || '')
    )
  })
}

const openScheduleOrderExportDialog = () => {
  scheduleOrderExportColumns.value = [...defaultScheduleOrderExportColumns]
  scheduleOrderExportVisible.value = true
}

const submitScheduleOrderExport = async () => {
  if (!scheduleOrderExportColumns.value.length) {
    message.warning('请至少选择一个导出列')
    return
  }
  scheduleOrderExporting.value = true
  try {
    const data = await MesProScheduleOrderApi.exportScheduleOrderExcel({
      ...scheduleOrderQueryParams,
      exportColumns: scheduleOrderExportColumns.value
    })
    download.excel(data, '排产工单.xls')
    scheduleOrderExportVisible.value = false
  } finally {
    scheduleOrderExporting.value = false
  }
}

const handleScheduleOrderSelectionChange = (rows: MesProScheduleOrderVO[]) => {
  selectedScheduleOrders.value = rows.filter((item) => isScheduleOrderReplanable(item))
  preflightResult.value = null
  lastPreflightRequest.value = null
  replanPreview.value = null
  lastReplanRequest.value = null
}

const clearScheduleOrderSelection = () => {
  selectedScheduleOrders.value = []
  scheduleOrderTableRef.value?.clearSelection?.()
}

const openProcessDialog = async (row: MesProScheduleOrderVO) => {
  currentScheduleOrder.value = row
  processDialogVisible.value = true
  processRouteQuickFilterParams.pageNo = 1
  delete processRouteQuickFilterParams.quickFilter
  processLoading.value = true
  try {
    processList.value = await MesProScheduleOrderApi.getProcessList(row.id)
  } finally {
    processLoading.value = false
  }
}

const openPriorityDialog = (row: MesProScheduleOrderVO) => {
  if (row.frozen) {
    message.warning('排产工单已冻结，不能调整优先级')
    return
  }
  priorityTarget.value = row
  priorityForm.id = row.id
  priorityForm.priorityNo = Number(row.priorityNo || 1)
  priorityDialogVisible.value = true
}

const submitPriorityAdjust = async () => {
  if (!priorityForm.id) {
    message.warning('排产工单编号不能为空')
    return
  }
  if (!Number.isInteger(priorityForm.priorityNo) || priorityForm.priorityNo < 1) {
    message.warning('优先级必须大于等于 1')
    return
  }
  prioritySaving.value = true
  try {
    await MesProScheduleOrderApi.updatePriority({
      id: priorityForm.id,
      priorityNo: priorityForm.priorityNo
    })
    message.success('优先级已调整')
    priorityDialogVisible.value = false
    await getScheduleOrderList()
  } finally {
    prioritySaving.value = false
  }
}

const openPromiseDateDialog = (row: MesProScheduleOrderVO) => {
  if (row.frozen) {
    message.warning('排产工单已冻结，不能设置承诺交期')
    return
  }
  promiseDateTarget.value = row
  promiseDateForm.id = row.id
  promiseDateForm.promiseDate = row.promiseDate || ''
  promiseDateForm.priorityNo = Number(row.priorityNo || 1)
  promiseDateForm.remark = row.remark || ''
  promiseDateForm.reason = ''
  promiseDateDialogVisible.value = true
}

const submitPromiseDateReset = async () => {
  if (!promiseDateForm.id) {
    message.warning('排产工单编号不能为空')
    return
  }
  if (!promiseDateForm.promiseDate) {
    message.warning('承诺交期不能为空')
    return
  }
  if (!Number.isInteger(promiseDateForm.priorityNo) || promiseDateForm.priorityNo < 1) {
    message.warning('优先级必须大于等于 1')
    return
  }
  if (!promiseDateForm.reason?.trim()) {
    message.warning('修改原因不能为空')
    return
  }
  promiseDateSaving.value = true
  try {
    await MesProScheduleOrderApi.updateScheduleOrder({
      id: promiseDateForm.id,
      promiseDate: promiseDateForm.promiseDate,
      priorityNo: promiseDateForm.priorityNo,
      remark: promiseDateForm.remark,
      reason: promiseDateForm.reason
    })
    message.success('承诺交期已更新')
    promiseDateDialogVisible.value = false
    await getScheduleOrderList()
  } finally {
    promiseDateSaving.value = false
  }
}

const openFreezeDialog = (row: MesProScheduleOrderVO) => {
  openFreezeRows([row])
}

const openFreezeRows = (rows: MesProScheduleOrderVO[]) => {
  const availableRows = rows.filter((item) => !item.frozen)
  if (!availableRows.length) {
    message.warning('请选择未冻结的排产工单')
    return
  }
  batchActionRows.value = availableRows
  batchActionReason.value = ''
  freezeDialogVisible.value = true
}

const submitScheduleOrderFreeze = async () => {
  if (!batchActionRows.value.length) {
    message.warning('排产工单不能为空')
    return
  }
  if (!batchActionReason.value.trim()) {
    message.warning('冻结原因不能为空')
    return
  }
  batchActionSaving.value = true
  try {
    await MesProScheduleOrderApi.freezeScheduleOrders({
      ids: batchActionRows.value.map((item) => item.id),
      reason: batchActionReason.value
    })
    message.success('排产工单已冻结')
    freezeDialogVisible.value = false
    await getScheduleOrderList()
  } finally {
    batchActionSaving.value = false
  }
}

const openUnfreezeDialog = (row: MesProScheduleOrderVO) => {
  openUnfreezeRows([row])
}

const openUnfreezeRows = (rows: MesProScheduleOrderVO[]) => {
  const availableRows = rows.filter((item) => item.frozen)
  if (!availableRows.length) {
    message.warning('请选择已冻结的排产工单')
    return
  }
  batchActionRows.value = availableRows
  batchActionReason.value = ''
  unfreezeDialogVisible.value = true
}

const submitScheduleOrderUnfreeze = async () => {
  if (!batchActionRows.value.length) {
    message.warning('排产工单不能为空')
    return
  }
  if (!batchActionReason.value.trim()) {
    message.warning('解冻原因不能为空')
    return
  }
  batchActionSaving.value = true
  try {
    await MesProScheduleOrderApi.unfreezeScheduleOrders({
      ids: batchActionRows.value.map((item) => item.id),
      reason: batchActionReason.value
    })
    message.success('排产工单已解冻')
    unfreezeDialogVisible.value = false
    await getScheduleOrderList()
  } finally {
    batchActionSaving.value = false
  }
}

const submitScheduleOrderDelete = async () => {
  if (!batchActionRows.value.length) {
    message.warning('排产工单不能为空')
    return
  }
  if (!batchActionReason.value.trim()) {
    message.warning('删除原因不能为空')
    return
  }
  batchActionSaving.value = true
  try {
    await MesProScheduleOrderApi.deleteScheduleOrders({
      ids: batchActionRows.value.map((item) => item.id),
      reason: batchActionReason.value
    })
    message.success('排产工单已删除')
    deleteDialogVisible.value = false
    await getScheduleOrderList()
  } finally {
    batchActionSaving.value = false
  }
}

const openManualFinishDialog = (row: MesProScheduleOrderVO) => {
  manualFinishTarget.value = row
  manualFinishReason.value = ''
  manualFinishDialogMode.value = 'MANUAL_FINISH'
  manualFinishDialogVisible.value = true
}

const openRevokeManualFinishDialog = (row: MesProScheduleOrderVO) => {
  manualFinishTarget.value = row
  manualFinishReason.value = ''
  manualFinishDialogMode.value = 'REVOKE_MANUAL_FINISH'
  manualFinishDialogVisible.value = true
}

const submitManualFinishAction = async () => {
  if (!manualFinishTarget.value?.id) {
    message.warning('排产工单编号不能为空')
    return
  }
  if (!manualFinishReason.value.trim()) {
    message.warning(
      manualFinishDialogMode.value === 'MANUAL_FINISH'
        ? '强制完成原因不能为空'
        : '撤销强制完成原因不能为空'
    )
    return
  }
  const confirmText =
    manualFinishDialogMode.value === 'MANUAL_FINISH'
      ? '确认强制完成该排产工单吗？这是有权限人员执行的强制关闭操作；强制完成后汇总按 100% 展示，真实工序进度仍保留，可撤销。'
      : '确认撤销该排产工单的强制完成吗？撤销后将根据真实工序进度恢复汇总状态。'
  await message.confirm(confirmText)
  manualFinishSaving.value = true
  try {
    if (manualFinishDialogMode.value === 'MANUAL_FINISH') {
      await MesProScheduleOrderApi.manualFinishScheduleOrder({
        id: manualFinishTarget.value.id,
        reason: manualFinishReason.value
      })
      message.success('排产工单已强制完成')
    } else {
      await MesProScheduleOrderApi.revokeManualFinishScheduleOrder({
        id: manualFinishTarget.value.id,
        reason: manualFinishReason.value
      })
      message.success('排产工单已撤销强制完成')
    }
    manualFinishDialogVisible.value = false
    await getScheduleOrderList()
  } finally {
    manualFinishSaving.value = false
  }
}

const openOperationLogDialog = async (row: MesProScheduleOrderVO) => {
  operationLogDialogVisible.value = true
  operationLogTarget.value = row
  operationLogList.value = []
  operationLogLoading.value = true
  try {
    operationLogList.value = await MesProScheduleOrderApi.getOperationLog(row.id)
  } finally {
    operationLogLoading.value = false
  }
}

const operationTraceFieldLabelMap: Record<string, string> = {
  code: '排产工单号',
  workOrderId: '生产工单',
  workOrderCode: '来源生产工单号',
  promiseDate: '承诺交期',
  priorityNo: '优先级',
  frozen: '冻结状态',
  frozenTime: '冻结时间',
  frozenBy: '冻结人',
  freezeReason: '冻结原因',
  manualFinished: '强制完成',
  manualFinishedTime: '强制完成时间',
  manualFinishedBy: '强制完成人',
  manualFinishedReason: '强制完成原因',
  status: '状态',
  remark: '备注'
}

type OperationLogDiffRow = {
  field: string
  fieldLabel: string
  beforeValue: string
  afterValue: string
}

const parseOperationSnapshot = (snapshotJson?: string) => {
  if (!snapshotJson) {
    return {}
  }
  try {
    const parsed = JSON.parse(snapshotJson)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('快照 JSON 不是对象')
    }
    return parsed as Record<string, unknown>
  } catch (error) {
    throw new Error(`快照 JSON 解析失败：${resolveOperationTraceErrorMessage(error, snapshotJson)}`)
  }
}

const resolveOperationTraceErrorMessage = (error: unknown, fallback?: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback || '未知错误'
}

const getOperationFieldLabel = (field: string) => {
  return operationTraceFieldLabelMap[field] || field
}

const formatOperationSnapshotValue = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

const buildOperationLogDiffRows = (row: MesProScheduleOrderOperationLogVO): OperationLogDiffRow[] => {
  const beforeSnapshot = parseOperationSnapshot(row.beforeSnapshotJson)
  const afterSnapshot = parseOperationSnapshot(row.afterSnapshotJson)
  const fields = Array.from(
    new Set([...Object.keys(beforeSnapshot), ...Object.keys(afterSnapshot)])
  ).sort()
  const diffRows = fields
    .filter((field) => JSON.stringify(beforeSnapshot[field]) !== JSON.stringify(afterSnapshot[field]))
    .map((field) => ({
      field,
      fieldLabel: getOperationFieldLabel(field),
      beforeValue: formatOperationSnapshotValue(beforeSnapshot[field]),
      afterValue: formatOperationSnapshotValue(afterSnapshot[field])
    }))
  return diffRows.length
    ? diffRows
    : [{ field: '-', fieldLabel: '无字段变化', beforeValue: '-', afterValue: '-' }]
}

const handleScheduleOrderTabChange = async (tabName: string | number) => {
  if (tabName !== 'workOrderAdmission') {
    return
  }
  selectedWorkOrders.value = []
  await getWorkOrderAdmissionList()
}

const getWorkOrderAdmissionList = async () => {
  const requestSerial = ++workOrderAdmissionRequestSerial
  workOrderAdmissionLoading.value = true
  try {
    const data = await MesProScheduleOrderApi.getAdmissionDiff(workOrderAdmissionQueryParams)
    if (requestSerial !== workOrderAdmissionRequestSerial) {
      return
    }
    workOrderAdmissionList.value = data.list || []
    workOrderAdmissionTotal.value = data.total || 0
  } catch (error) {
    if (requestSerial === workOrderAdmissionRequestSerial) {
      console.error('[MES] 加载同步工单列表失败', error)
      message.error(`加载同步工单列表失败：${error instanceof Error ? error.message : String(error)}`)
    }
  } finally {
    if (requestSerial === workOrderAdmissionRequestSerial) {
      workOrderAdmissionLoading.value = false
    }
  }
}

const workOrderAdmissionMultiFilter = useTableMultiFilter(
  'mes.pro.scheduleOrder.admissionDiff',
  workOrderAdmissionMultiFilterDefinitions,
  workOrderAdmissionQueryParams,
  getWorkOrderAdmissionList
)

const handleWorkOrderAdmissionSelectionChange = (rows: MesProScheduleOrderAdmissionDiffRowVO[]) => {
  selectedWorkOrders.value = rows
}

const getMainTableCellClassName = ({
  column
}: {
  column: { property?: string }
}) => {
  const wrapColumns = new Set(['erpWorkOrderCode', 'productCode'])
  return wrapColumns.has(column.property || '') ? 'schedule-order-pool__main-table__cell--wrap' : ''
}

const getScheduleOrderRowClassName = ({ row }: { row: MesProScheduleOrderVO }) => {
  const classes: string[] = []
  if (row.frozen) {
    classes.push('schedule-order-pool__row--frozen')
  }
  if (Number(row.blockingIssueCount || 0) > 0) {
    classes.push('schedule-order-pool__row--blocked')
  }
  return classes.join(' ')
}

const getScheduleOrderReplanBlockReason = (row: MesProScheduleOrderVO) => {
  if (row.frozen) return '已冻结'
  if (Number(row.status) === SCHEDULE_ORDER_STATUS_FINISHED) return '已完成'
  if (Number(row.status) === SCHEDULE_ORDER_STATUS_CANCELED) return '已取消'
  return '不满足重排条件'
}

const isScheduleOrderReplanable = (row: MesProScheduleOrderVO) => {
  const status = Number(row.status)
  return (
    !row.frozen &&
    status !== SCHEDULE_ORDER_STATUS_FINISHED &&
    status !== SCHEDULE_ORDER_STATUS_CANCELED
  )
}

const getLastReplanParticipatingScheduleOrderIdSet = () => {
  return new Set(
    lastReplanParticipatingScheduleOrderIds.value
      .map((id) => Number(id))
      .filter((id) => Number.isFinite(id) && id > 0)
  )
}

const normalizePositiveId = (value: unknown) => {
  const id = Number(value)
  return Number.isFinite(id) && id > 0 ? id : undefined
}

const resolveReplanTaskWorkOrderId = (task: any) => {
  const directWorkOrderId = normalizePositiveId(task?.workOrderId)
  if (directWorkOrderId) {
    return directWorkOrderId
  }
  const parent = String(task?.parent || '')
  if (!parent.startsWith('301_')) {
    return undefined
  }
  return normalizePositiveId(parent.replace('301_', ''))
}

const resolveReplanParticipatingScheduleOrderIds = (
  preview: ProTaskAutoScheduleReplanPreviewRespVO
) => {
  const workOrderIds = new Set<number>()
  const scheduleOrderIds = new Set<number>()

  const previewTasks = preview.tasks || []
  const workOrderAnalyses = preview.workOrderAnalyses || []
  const protectedTasks = preview.protectedTasks || []

  previewTasks.forEach((task: any) => {
    const workOrderId = resolveReplanTaskWorkOrderId(task)
    if (workOrderId) {
      workOrderIds.add(workOrderId)
    }
  })
  workOrderAnalyses.forEach((analysis) => {
    const workOrderId = normalizePositiveId(analysis.workOrderId)
    if (workOrderId) {
      workOrderIds.add(workOrderId)
    }
  })
  protectedTasks.forEach((task) => {
    const scheduleOrderId = normalizePositiveId(task.scheduleOrderId)
    if (scheduleOrderId) {
      scheduleOrderIds.add(scheduleOrderId)
    }
  })

  const candidateRows = [...selectedScheduleOrders.value, ...scheduleOrderList.value]
  candidateRows.forEach((row) => {
    const scheduleOrderId = normalizePositiveId(row.id)
    if (!scheduleOrderId) {
      return
    }
    const workOrderId = normalizePositiveId(row.workOrderId)
    if (scheduleOrderIds.has(scheduleOrderId) || (workOrderId && workOrderIds.has(workOrderId))) {
      scheduleOrderIds.add(scheduleOrderId)
    }
  })

  return Array.from(scheduleOrderIds)
}

const updateLastReplanParticipatingScheduleOrders = (
  preview: ProTaskAutoScheduleReplanPreviewRespVO
) => {
  lastReplanParticipatingScheduleOrderIds.value = resolveReplanParticipatingScheduleOrderIds(preview)
}

const isScheduleOrderParticipatingInLastReplan = (row: MesProScheduleOrderVO) => {
  const scheduleOrderId = normalizePositiveId(row.id)
  return Boolean(
    scheduleOrderId && getLastReplanParticipatingScheduleOrderIdSet().has(scheduleOrderId)
  )
}

const getScheduleOrderSourceCodeText = (row: MesProScheduleOrderVO) => {
  const completedSuffix =
    row.manualFinished || Number(row.status) === SCHEDULE_ORDER_STATUS_FINISHED ? '(已完成)' : ''
  return `${row.erpWorkOrderCode}${completedSuffix}`
}

const getScheduleOrderProductCodeClass = (row: MesProScheduleOrderVO) => {
  return isScheduleOrderParticipatingInLastReplan(row)
    ? 'schedule-order-pool__product-code schedule-order-pool__product-code--scheduled'
    : 'schedule-order-pool__product-code schedule-order-pool__product-code--unscheduled'
}

const isScheduleOrderSelectable = (row: MesProScheduleOrderVO) => {
  return isScheduleOrderReplanable(row)
}

const isAdmissionRowSelectable = (row: MesProScheduleOrderAdmissionDiffRowVO) => {
  return Boolean(row.selectable && row.admissionStatus === 'READY_TO_ADMIT')
}

const isAdmissionRowAdmitted = (row: MesProScheduleOrderAdmissionDiffRowVO) => {
  return Boolean(row.scheduleOrderId || row.admissionStatus === 'ALREADY_ADMITTED')
}

const getAdmissionCellClassName = ({
  column
}: {
  column: { property?: string }
}) => {
  const wrapColumns = new Set([
    'workOrderCode',
    'productCode',
    'productName',
    'productSpecification',
    'message'
  ])
  return wrapColumns.has(column.property || '') ? 'schedule-order-pool__admission-table__cell--wrap' : ''
}

const submitWorkOrderAdmission = async () => {
  const rows = selectedWorkOrders.value.filter(isAdmissionRowSelectable)
  if (rows.length === 0) {
    message.warning('请先选择需要加入排产工单池的生产工单')
    return
  }
  workOrderAdmissionSaving.value = true
  try {
    await MesProScheduleOrderApi.createFromWorkOrders({
      workOrderIds: rows.map((workOrder) => workOrder.workOrderId)
    })
    message.success(`已将 ${rows.length} 个生产工单加入排产工单池`)
    selectedWorkOrders.value = []
    await getWorkOrderAdmissionList()
    await getScheduleOrderList()
  } finally {
    workOrderAdmissionSaving.value = false
  }
}

const openReplanDrawer = () => {
  if (!hasReplanPermission.value) {
    message.warning(replanProjectionState.value.blockerMessage || '当前账号没有手动重排权限')
    return
  }
  replanDrawerVisible.value = true
  replanSettingsDialogVisible.value = false
  preflightResult.value = null
  lastPreflightRequest.value = null
  replanPreview.value = null
  lastReplanRequest.value = null
  replanForm.startTime = getDefaultReplanStartDate()
}

const openReplanSettingsDialog = () => {
  replanSettingsDialogVisible.value = true
}

const isAutoOpenReplanQuery = (value: unknown) => {
  return Array.isArray(value) ? value.includes('1') : value === '1'
}

const applyRouteProcessFilter = () => {
  const currentProcessId = Array.isArray(route.query.currentProcessId)
    ? route.query.currentProcessId[0]
    : route.query.currentProcessId
  if (!currentProcessId) {
    return
  }
  const processId = Number(currentProcessId)
  if (processId === 0) {
    return
  }
  if (!Number.isFinite(processId) || !Number.isInteger(processId) || processId < 0) {
    throw new Error(`当前工序筛选参数不是有效数字: ${currentProcessId}`)
  }
  scheduleOrderQueryParams.currentProcessId = processId
  scheduleOrderQueryParams.pageNo = 1
}

watch(
  () => route.query.autoOpenReplan,
  (autoOpenReplan) => {
    if (isAutoOpenReplanQuery(autoOpenReplan)) {
      openReplanDrawer()
    }
  }
)

watch(replanDrawerVisible, (visible) => {
  if (!visible) {
    replanSettingsDialogVisible.value = false
    replanStartDateDialogVisible.value = false
    resetReplanApplyProgress()
  }
})

const normalizeScheduleOrderRefreshCode = (value: unknown) => String(value ?? '').trim()

const shouldRefreshScheduleOrderList = (payload?: MesScheduleOrderRefreshPayload) => {
  const scheduleOrderCodes = new Set(
    (payload?.scheduleOrderCodes || []).map(normalizeScheduleOrderRefreshCode).filter(Boolean)
  )
  const workOrderCodes = new Set(
    (payload?.workOrderCodes || []).map(normalizeScheduleOrderRefreshCode).filter(Boolean)
  )
  if (!scheduleOrderCodes.size && !workOrderCodes.size) {
    return true
  }
  return scheduleOrderList.value.some((row) => {
    const scheduleOrderCode = normalizeScheduleOrderRefreshCode(row.code)
    const workOrderCode = normalizeScheduleOrderRefreshCode(row.erpWorkOrderCode)
    return (
      (scheduleOrderCode && scheduleOrderCodes.has(scheduleOrderCode)) ||
      (workOrderCode && workOrderCodes.has(workOrderCode))
    )
  })
}

const handleScheduleOrderRefresh = async (payload?: MesScheduleOrderRefreshPayload) => {
  if (!shouldRefreshScheduleOrderList(payload)) {
    return
  }
  await getScheduleOrderList()
  await loadLatestSuccessfulScheduleApplyTime()
}

useEmitt({
  name: MES_SCHEDULE_ORDER_REFRESH_EVENT,
  callback: handleScheduleOrderRefresh
})

const runPreflightForRequest = async (request: ProTaskAutoSchedulePreviewReqVO) => {
  preflightLoading.value = true
  try {
    const preflightRequest = buildPreflightRequestByReplanRequest(request)
    preflightResult.value = await MesProScheduleOrderApi.preflightScheduleOrders(preflightRequest)
    lastPreflightRequest.value = preflightRequest
    if (preflightResult.value.result === 'BLOCKED') {
      message.warning('排产前检查存在阻断问题')
    } else {
      message.success('排产前检查已完成')
    }
    return preflightResult.value
  } finally {
    preflightLoading.value = false
  }
}

const runPreflight = async () => {
  await runPreflightForRequest(buildReplanRequest())
}

const previewReplanForRequest = async (request: ProTaskAutoSchedulePreviewReqVO) => {
  replanPreviewLoading.value = true
  try {
    replanPreview.value = await ProTaskAutoScheduleApi.replanPreview(request)
    lastReplanRequest.value = request
    message.success('重排预览已生成')
    return replanPreview.value
  } finally {
    replanPreviewLoading.value = false
  }
}

const previewReplan = async () => {
  try {
    const request = buildReplanRequest()
    await runPreflightForRequest(request)
    if (preflightHasGlobalBlockedIssue.value) {
      message.error('排产前检查存在无法归因到工单的阻断问题，不能生成重排预览')
      return
    }
    await previewReplanForRequest(request)
  } catch (error) {
    console.error('[MES] 重排预览失败', error)
    message.error(error instanceof Error ? error.message : '重排预览失败，请查看接口返回信息')
  }
}

const buildReplanApplyIdempotencyKey = (request: ProTaskAutoSchedulePreviewReqVO) => {
  const scopeKey = [...request.scheduleOrderIds].sort((left, right) => left - right).join('-')
  const startKey = dayjs(request.startTime).format('YYYYMMDDHHmmss')
  return `MES-SCHEDULE-REPLAN-${scopeKey}-${startKey}-${generateUUID()}`
}

const buildReplanApplySuccessMessage = (result: ProTaskAutoScheduleApplyRespVO) => {
  const createdCount = result.createdTaskIds?.length ?? 0
  const deletedCount = result.deletedTaskIds?.length ?? 0
  const preservedCount = result.preservedTaskIds?.length ?? 0
  const appliedWorkOrderCount = result.summary?.appliedWorkOrderCount
  const blockedWorkOrderCount = result.summary?.blockedWorkOrderCount
  const skippedWorkOrderCount = result.summary?.skippedWorkOrderCount
  const workOrderSummary = [
    appliedWorkOrderCount !== undefined ? `应用工单 ${appliedWorkOrderCount} 个` : '',
    blockedWorkOrderCount !== undefined ? `标记阻断 ${blockedWorkOrderCount} 个` : '',
    skippedWorkOrderCount !== undefined ? `跳过 ${skippedWorkOrderCount} 个` : ''
  ]
    .filter(Boolean)
    .join('，')
  return `应用重排成功：${workOrderSummary ? `${workOrderSummary}，` : ''}正式排程已更新，新增任务 ${createdCount} 个，删除任务 ${deletedCount} 个，保留任务 ${preservedCount} 个。`
}

const applyReplan = async () => {
  const defaultStartDate = getDefaultReplanStartDate()
  try {
    buildReplanRequest(buildWholeDayReplanStartTime(defaultStartDate))
  } catch (error) {
    message.error(error instanceof Error ? error.message : '请先确认重排范围')
    return
  }
  replanStartDate.value = defaultStartDate
  replanStartDateDialogVisible.value = true
}

const confirmApplyReplanStartChoice = async () => {
  const applyRequest = buildReplanRequest(buildWholeDayReplanStartTime(replanStartDate.value))
  replanForm.startTime = dayjs(applyRequest.startTime).format('YYYY-MM-DD')
  replanApplyLoading.value = true
  startReplanApplyProgress()
  try {
    const preflight = await runPreflightForRequest(applyRequest)
    if (preflight.result === 'BLOCKED' && preflightHasGlobalBlockedIssue.value) {
      throw new Error('排产前检查存在无法归因到工单的阻断问题，不能应用重排')
    }
    const freshPreview = await previewReplanForRequest(applyRequest)
    if (!freshPreview?.calendarContextToken) {
      throw new Error('重排预览缺少日历上下文，不能应用重排')
    }
    if (hasGlobalReplanBlockingIssue(freshPreview)) {
      throw new Error('重排预览存在无法归因到工单的阻断问题，不能应用重排')
    }
    notifySkippedSelectedReplanRows(freshPreview)
    const applyResult = await ProTaskAutoScheduleApi.replanApply({
      ...applyRequest,
      reason: replanForm.reason?.trim() || undefined,
      calendarContextToken: freshPreview.calendarContextToken,
      idempotencyKey: buildReplanApplyIdempotencyKey(applyRequest)
    })
    updateLastReplanParticipatingScheduleOrders(freshPreview)
    message.success(buildReplanApplySuccessMessage(applyResult))
    await finishReplanApplyProgress()
    replanStartDateDialogVisible.value = false
    replanSettingsDialogVisible.value = false
    replanDrawerVisible.value = false
    clearScheduleOrderSelection()
    await getScheduleOrderList()
    await loadLatestSuccessfulScheduleApplyTime()
    emitter.emit(MES_PRO_TASK_GANTT_REFRESH_EVENT, {
      source: 'REPLAN_APPLY',
      scheduleOrderIds: lastReplanParticipatingScheduleOrderIds.value
    })
  } catch (error) {
    resetReplanApplyProgress()
    console.error('[MES] 应用重排失败', error)
    message.error(error instanceof Error ? error.message : '应用重排失败，请查看接口返回信息')
  } finally {
    replanApplyLoading.value = false
  }
}

const openDailyCompareDialog = async (row: MesProScheduleOrderVO) => {
  currentScheduleOrder.value = row
  dailyCompareDialogVisible.value = true
  dailyCompareRange.value = undefined
  await loadDailyCompare()
}

const loadDailyCompare = async () => {
  if (!currentScheduleOrder.value) return
  dailyCompareLoading.value = true
  try {
    dailyCompareList.value = await MesProScheduleOrderApi.getDailyCompare({
      scheduleOrderId: currentScheduleOrder.value.id,
      startDate: dailyCompareRange.value?.[0],
      endDate: dailyCompareRange.value?.[1]
    })
  } finally {
    dailyCompareLoading.value = false
  }
}

const openRouteDetail = (row: MesProScheduleOrderVO) => {
  router.push({
    name: 'MesProRouteEdit',
    params: {
      id: row.routeId
    },
    query: {
      tab: 'flow',
      routeProcessId: row.currentRouteProcessId ? String(row.currentRouteProcessId) : undefined
    }
  })
}

const openCurrentProcessRouteDetail = (row: MesProScheduleOrderVO) => {
  if (!row.routeId || !row.currentRouteProcessId) {
    message.warning('当前工序缺少路线节点，无法直达')
    return
  }
  openRouteDetail(row)
}

const openWorkOrder = (row: MesProScheduleOrderVO) => {
  if (!row.erpWorkOrderCode) {
    return
  }
  router.push({
    path: '/mes/pro/work-order',
    query: { code: row.erpWorkOrderCode }
  })
}

const handleOpenProductionMaterialList = (row: MesProScheduleOrderVO) => {
  router.push({
    path: '/erp/production/material-list',
    query: { productionOrderNo: row.erpWorkOrderCode || row.code }
  })
}

const canOpenIssueAction = (action?: MesProScheduleOrderIssueActionVO) => {
  if (!action?.targetRouteName) {
    return false
  }
  return !action.requiredPermission || checkPermi([action.requiredPermission])
}

const openIssueAction = (action?: MesProScheduleOrderIssueActionVO) => {
  if (!action?.targetRouteName) {
    message.warning('入口未配置')
    return
  }
  if (!canOpenIssueAction(action)) {
    message.warning(`缺失权限 ${action.requiredPermission}`)
    return
  }
  const targetQuery = { ...(action.targetQuery || {}) }
  const routeId = targetQuery.routeId
  if (action.targetRouteName === 'MesProRouteEdit') {
    if (!routeId) {
      message.warning('入口缺少路线编号')
      return
    }
    delete targetQuery.routeId
    router.push({
      name: action.targetRouteName,
      params: { id: routeId },
      query: targetQuery
    })
    return
  }
  router.push({
    name: action.targetRouteName,
    query: targetQuery
  })
}

const canOpenReplanIssueCalendar = (issue?: ProTaskAutoScheduleIssueVO) => {
  return Boolean(issue?.calendarDate)
}

const openReplanIssueCalendar = (issue?: ProTaskAutoScheduleIssueVO) => {
  if (!issue?.calendarDate) {
    message.warning('缺少班次日期，无法跳转')
    return
  }
  router.push({
    name: 'MesProScheduleCalendar',
    query: {
      date: formatIssueDate(issue.calendarDate),
      openShiftEditor: '1',
      shiftId: issue.shiftId ? String(issue.shiftId) : undefined,
      processId: issue.processId ? String(issue.processId) : undefined,
      workOrderId: issue.workOrderId ? String(issue.workOrderId) : undefined
    }
  })
}

const openMaterialShortageDialog = () => {
  if (!materialShortageIssues.value.length) {
    throw new Error('缺少物料缺料明细')
  }
  materialShortageDialogVisible.value = true
}

const resolveIssueSeverity = (issues: ProTaskAutoScheduleIssueVO[]) => {
  if (issues.some((issue) => issue.severity === 'BLOCKING' || issue.severity === 'BLOCKED')) {
    return 'BLOCKING'
  }
  if (issues.some((issue) => issue.severity === 'WARN' || issue.severity === 'WARNING')) {
    return 'WARNING'
  }
  return issues[0]?.severity || 'WARNING'
}

const formatIssueWorkOrders = (issues: ProTaskAutoScheduleIssueVO[]) => {
  const codes = issues
    .map((issue) => issue.workOrderCode || (issue.workOrderId ? String(issue.workOrderId) : ''))
    .filter(Boolean)
  return Array.from(new Set(codes)).join('，') || '-'
}

const buildIssueRemarkParts = (issue: ProTaskAutoScheduleIssueVO) => {
  const parts: string[] = []
  if (issue.workOrderCode || issue.workOrderId) {
    parts.push(`工单：${issue.workOrderCode || issue.workOrderId}`)
  }
  if (issue.calendarDate) {
    parts.push(`日期：${formatIssueDate(issue.calendarDate)}`)
  }
  if (issue.shiftName || issue.shiftId) {
    parts.push(`班次：${issue.shiftName || issue.shiftId}`)
  }
  if (issue.processName || issue.processId) {
    parts.push(`工序：${issue.processName || issue.processId}`)
  }
  if (issue.workstationName || issue.workstationId) {
    parts.push(`工作站：${issue.workstationName || issue.workstationId}`)
  }
  return parts
}

const normalizePercent = (value?: number) => {
  const percent = Number(value || 0)
  return Math.max(0, Math.min(100, Number(percent.toFixed(2))))
}

const formatPercent = (value?: number) => normalizePercent(value).toFixed(2)

const formatQuantity = (value?: number) => {
  const quantity = Number(value || 0)
  return Number.isFinite(quantity) ? String(Math.round(quantity)) : '0'
}

const formatCapacityIntegerNumber = (value?: number) => {
  const capacity = Number(value || 0)
  return Number.isFinite(capacity)
    ? capacity.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
    : '0'
}

const getProcessProgressStatus = (row: MesProScheduleOrderProcessVO) => {
  const plannedQuantity = Number(row.plannedQuantity || 0)
  const effectiveCompletedQuantity = Number(row.effectiveCompletedQuantity || 0)
  if (plannedQuantity > 0 && effectiveCompletedQuantity >= plannedQuantity) {
    return 'finished'
  }
  if (effectiveCompletedQuantity <= 0) {
    return row.plannedEndTime ? 'scheduled-not-started' : 'unscheduled'
  }
  return 'in-progress'
}

const getProcessProgressStatusText = (row: MesProScheduleOrderProcessVO) => {
  return (
    {
      finished: '已完成',
      'scheduled-not-started': '已排产未开始',
      unscheduled: '未排产',
      'in-progress': '进行中'
    }[getProcessProgressStatus(row)] || '未知'
  )
}

const getProcessProgressStatusTag = (row: MesProScheduleOrderProcessVO) => {
  return (
    {
      finished: 'success',
      'scheduled-not-started': 'primary',
      unscheduled: 'danger',
      'in-progress': 'warning'
    }[getProcessProgressStatus(row)] || 'info'
  )
}

const getProcessProgressRowClass = ({ row }: { row: MesProScheduleOrderProcessVO }) => {
  return `schedule-order-pool__process-row--${getProcessProgressStatus(row)}`
}

const formatDateTime = (value?: string | number | Date) => {
  return formatDateTimeValue(value, '-')
}

const formatIssueDate = (value?: string) => {
  return value ? formatDate(new Date(value), 'YYYY-MM-DD') : '-'
}

const getStartRiskText = (row: MesProScheduleOrderVO) => {
  if (!row.plannedStartTime || !row.latestStartTime) return ''
  const plannedStart = dayjs(row.plannedStartTime)
  const latestStart = dayjs(row.latestStartTime)
  if (!plannedStart.isValid() || !latestStart.isValid() || !plannedStart.isAfter(latestStart)) {
    return ''
  }

  const overdueMinutes = Math.max(1, Math.ceil(plannedStart.diff(latestStart, 'minute', true)))
  if (overdueMinutes < 60) return `晚于最晚开工 ${overdueMinutes} 分钟`
  if (overdueMinutes < 24 * 60) return `晚于最晚开工 ${Math.ceil(overdueMinutes / 60)} 小时`
  return `晚于最晚开工 ${Math.ceil(overdueMinutes / (24 * 60))} 天`
}

const isStartRisk = (row: MesProScheduleOrderVO) => Boolean(getStartRiskText(row))

const getDeliveryRiskText = (row: MesProScheduleOrderVO) => {
  if (!row.plannedEndTime || !row.promiseDate) return ''
  const plannedEndDate = dayjs(row.plannedEndTime).startOf('day')
  const promiseDate = dayjs(row.promiseDate).startOf('day')
  if (!plannedEndDate.isValid() || !promiseDate.isValid()) return ''

  const overdueDays = plannedEndDate.diff(promiseDate, 'day')
  return overdueDays > 0 ? `逾承诺交期 ${overdueDays} 天` : ''
}

const isDeliveryRisk = (row: MesProScheduleOrderVO) => Boolean(getDeliveryRiskText(row))

const buildManualFinishTooltip = (row: MesProScheduleOrderVO) => {
  return [
    row.manualFinishedTime ? `强制完成时间：${formatDateTime(row.manualFinishedTime)}` : '',
    row.manualFinishedReason ? `强制完成原因：${row.manualFinishedReason}` : ''
  ]
    .filter(Boolean)
    .join('\n')
}

const getAdmissionStatusText = (status: string) => {
  return (
    {
      READY_TO_ADMIT: '可入池',
      ALREADY_ADMITTED: '已入池',
      BLOCKED: '阻断',
      WARN: '警告'
    }[status] || '未知'
  )
}

const getAdmissionStatusTag = (status: string, severity?: string) => {
  if (severity === 'BLOCKED') return 'danger'
  if (severity === 'WARN') return 'warning'
  return (
    {
      READY_TO_ADMIT: 'success',
      ALREADY_ADMITTED: 'info',
      BLOCKED: 'danger',
      WARN: 'warning'
    }[status] || 'info'
  )
}

const getAdmissionWorkOrderCodeClass = (row: MesProScheduleOrderAdmissionDiffRowVO) => {
  if (row.workOrderStatus === MesProWorkOrderStatusEnum.FINISHED) {
    return 'schedule-order-pool__work-order-code schedule-order-pool__work-order-code--finished'
  }
  return isAdmissionRowAdmitted(row)
    ? 'schedule-order-pool__work-order-code schedule-order-pool__work-order-code--scheduled'
    : 'schedule-order-pool__work-order-code schedule-order-pool__work-order-code--unscheduled'
}

const getAdmissionProductCodeClass = (row: MesProScheduleOrderAdmissionDiffRowVO) => {
  return isAdmissionRowAdmitted(row)
    ? 'schedule-order-pool__product-code schedule-order-pool__product-code--scheduled'
    : 'schedule-order-pool__product-code schedule-order-pool__product-code--unscheduled'
}

const getReasonCodeText = (reasonCode?: string) => {
  return (
    {
      READY_TO_ADMIT: '可入池',
      ALREADY_ADMITTED: '已入池',
      BLOCKED_WORK_ORDER_FROZEN: '工单已冻结',
      BLOCKED_WORK_ORDER_STATUS: '工单未确认',
      BLOCKED_MISSING_ROUTE: '缺路线',
      BLOCKED_ROUTE_DISABLED: '路线未启用',
      BLOCKED_ROUTE_VERSION_MISSING: '缺路线版本',
      BLOCKED_ROUTE_PROCESS_MISSING: '缺路线工序',
      BLOCKED_ROUTE_PROCESS_SCHEDULE_USE_MISSING: '缺智能排产用途',
      BLOCKED_ROUTE_PROCESS_DISABLED_FOR_SCHEDULE: '智能排产已关闭',
      BLOCKED_ROUTE_SCHEDULE_CONFIG_MISSING: '缺排产策略',
      BLOCKED_SHIFT_HOURS_REQUIRED: '缺班次小时',
      BLOCKED_INVALID_FINITE_CAPACITY: '缺产能',
      BLOCKED_INVALID_INFINITE_FORMULA: '缺无限产能公式',
      BLOCKED_CALENDAR_RULE_MISSING: '缺日历',
      BLOCKED_ERP_SYNC_RECORD_MISSING: '缺 ERP 正式订单',
      WARN_ERP_SYNC_RECORD_MISSING: '缺 ERP 同步证据'
    }[reasonCode || ''] || '未知原因'
  )
}

const getPreflightResultText = (result: string) => {
  return (
    {
      PASS: '通过',
      WARN: '警告',
      BLOCKED: '阻断'
    }[result] || '未知'
  )
}

const getPreflightResultTag = (result: string) => {
  return (
    {
      PASS: 'success',
      WARN: 'warning',
      BLOCKED: 'danger'
    }[result] || 'info'
  )
}

const getIssueSeverityText = (severity?: string) => {
  return (
    {
      PASS: '通过',
      WARN: '警告',
      WARNING: '警告',
      BLOCKED: '阻断',
      BLOCKING: '阻断',
      ERROR: '错误'
    }[severity || ''] || '未知'
  )
}

const getIssueSeverityTag = (severity?: string) => {
  return (
    {
      PASS: 'success',
      WARN: 'warning',
      WARNING: 'warning',
      BLOCKED: 'danger',
      BLOCKING: 'danger',
      ERROR: 'danger'
    }[severity || ''] || 'info'
  )
}

const getDailyCompareStatusText = (status: number) => {
  return (
    {
      0: '正常',
      1: '提前',
      2: '滞后',
      3: '无计划有报工',
      4: '有计划无报工'
    }[status] || '未知'
  )
}

const getDailyCompareStatusTag = (status: number) => {
  return (
    {
      0: 'success',
      1: 'primary',
      2: 'warning',
      3: 'info',
      4: 'danger'
    }[status] || 'info'
  )
}

const formatPreservedTaskSummary = (value?: number) => {
  const count = Number(value || 0)
  return `${count} 个受保护任务（已完成、已报工、手工锁定部分冻结保留）`
}

const getDailyCompareDiffClass = (value?: number) => {
  const diff = Number(value || 0)
  if (diff > 0) return 'schedule-order-pool__ahead-text'
  if (diff < 0) return 'schedule-order-pool__warning-text'
  return ''
}

const getDailyCompareRowKey = (row: MesProScheduleOrderDailyCompareVO) => {
  return `${row.planDate}-${row.scheduleOrderProcessId}`
}

const getOperationTypeText = (type: string) => {
  const textMap: Record<string, string> = {
    FREEZE: '冻结',
    UNFREEZE: '解冻',
    UPDATE: '修改',
    DELETE: '删除',
    MANUAL_FINISH: '强制完成',
    REVOKE_MANUAL_FINISH: '撤销强制完成',
    SYNC_PROGRESS: '同步进度'
  }
  return textMap[type] || type || '-'
}

onBeforeUnmount(() => {
  resetReplanApplyProgress()
})

onMounted(async () => {
  applyRouteProcessFilter()
  await getScheduleOrderList()
  await loadLatestSuccessfulScheduleApplyTime()
  if (isAutoOpenReplanQuery(route.query.autoOpenReplan)) {
    openReplanDrawer()
  }
})
</script>

<style scoped>
.schedule-order-pool {
  height: calc(100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-footer-height) - 32px);
  min-height: 0;
}

.schedule-order-pool__content {
  height: 100%;
  margin-bottom: 0 !important;
}

.schedule-order-pool__content :deep(.el-card__body) {
  min-height: 0;
  overflow: hidden;
}

.schedule-order-pool :deep(.el-table th.el-table__cell) {
  background: #f7f9fc;
}

.schedule-order-pool :deep(.el-table .cell) {
  line-height: 22px;
}

.schedule-order-pool :deep(.el-card__header > .flex) {
  width: 100%;
  min-width: 0;
}

.schedule-order-pool :deep(.el-card__header > .flex > .flex-grow) {
  min-width: 0;
}

.schedule-order-pool__header-main {
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-columns: minmax(620px, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.schedule-order-pool__header-filter {
  width: 100%;
  min-width: 0;
  max-width: 780px;
  justify-self: start;
}

.schedule-order-pool__header-filter :deep(.table-quick-filter) {
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-columns: 150px 96px minmax(180px, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.schedule-order-pool__header-filter :deep(.table-quick-filter__value) {
  width: 100%;
  min-width: 0;
}

.schedule-order-pool__header-filter :deep(.el-button) {
  min-height: 36px;
  padding-inline: 16px;
}

.schedule-order-pool__header-actions {
  display: flex;
  min-width: 0;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  justify-self: end;
}

.schedule-order-pool__toolbar-group {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 10px;
}

.schedule-order-pool__toolbar-group--batch {
  margin-left: 0;
}

.schedule-order-pool__toolbar-inline {
  display: inline-flex;
}

.schedule-order-pool__tabs {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
}

.schedule-order-pool__tabs :deep(.el-tabs__header) {
  flex: 0 0 auto;
}

.schedule-order-pool__tabs :deep(.el-tabs__content) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.schedule-order-pool__tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
}

.schedule-order-pool__schedule-template,
.schedule-order-pool__admission-tab,
.schedule-order-pool__admission-template {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  flex-direction: column;
}

.schedule-order-pool__schedule-template :deep(.unified-list-template__query-form) {
  flex-wrap: nowrap;
  align-items: center;
}

.schedule-order-pool__schedule-template :deep(.unified-list-template__table-shell),
.schedule-order-pool__admission-template :deep(.unified-list-template__table-shell) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.schedule-order-pool__tab-actions {
  display: flex;
  width: 100%;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.schedule-order-pool__tab-column-settings {
  white-space: nowrap;
}

.schedule-order-pool__last-success-time {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  min-width: 274px;
  margin-right: auto;
  padding: 0 12px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fafcff;
  color: #4b5563;
  font-size: 13px;
  line-height: 1;
  white-space: nowrap;
}

.schedule-order-pool__last-success-time strong {
  margin-left: 4px;
  color: #172033;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.schedule-order-pool__last-success-time--error {
  border-color: #f5c2c7;
  background: #fff5f5;
  color: #c00000;
}

.schedule-order-pool__last-success-time--error strong {
  color: #c00000;
}

.schedule-order-pool__toolbar-group :deep(.el-button) {
  min-height: 36px;
  padding-inline: 16px;
}

.schedule-order-pool__export-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.schedule-order-pool__export-hint {
  color: #606266;
  line-height: 22px;
}

.schedule-order-pool__export-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

:global(.schedule-order-pool__skipped-notice ul) {
  margin: 8px 0;
  padding-left: 18px;
}

:global(.schedule-order-pool__skipped-notice li) {
  margin: 6px 0;
  line-height: 1.5;
}

@media (max-width: 1360px) {
  .schedule-order-pool__header-main {
    grid-template-columns: minmax(0, 1fr);
  }

  .schedule-order-pool__header-filter {
    width: 100%;
    max-width: none;
  }

  .schedule-order-pool__header-filter :deep(.table-quick-filter) {
    grid-template-columns: 150px 96px minmax(180px, 1fr) auto;
  }

  .schedule-order-pool__header-actions {
    width: 100%;
    flex-wrap: wrap;
    justify-content: flex-start;
    justify-self: start;
  }

  .schedule-order-pool__toolbar-group {
    flex-wrap: wrap;
  }
}

@media (max-width: 760px) {
  .schedule-order-pool__header-filter :deep(.table-quick-filter) {
    grid-template-columns: minmax(0, 1fr);
  }
}

.schedule-order-pool__row-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px 8px;
  width: 124px;
  max-height: 52px;
  margin: 0 auto;
  min-width: 0;
  white-space: nowrap;
}

.schedule-order-pool__row-actions :deep(.el-button) {
  width: 34px;
  justify-content: center;
  margin-left: 0;
  padding: 0;
}

.schedule-order-pool__main-table-text {
  display: inline-block;
  min-width: 0;
  white-space: normal;
  word-break: break-all;
  overflow-wrap: anywhere;
  line-height: 18px;
}

.schedule-order-pool__replan-block-reason {
  display: inline-flex;
  max-width: 100%;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  color: #b42318;
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
  white-space: normal;
  word-break: break-word;
}

.schedule-order-pool__replan-block-reason small {
  font-size: 11px;
  font-weight: 500;
}

.schedule-order-pool__replan-available {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #237804;
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
  white-space: nowrap;
}

.schedule-order-pool :deep(.schedule-order-pool__main-table__cell--wrap .cell) {
  white-space: normal;
  word-break: break-all;
  overflow-wrap: anywhere;
  text-overflow: clip;
  line-height: 18px;
}

.schedule-order-pool :deep(.schedule-order-pool__row--frozen td.el-table__cell) {
  background: #fff7e6 !important;
}

.schedule-order-pool :deep(.schedule-order-pool__row--frozen .cell) {
  color: #5f3b00;
  font-weight: 600;
}

.schedule-order-pool :deep(.schedule-order-pool__row--blocked td.el-table__cell) {
  background: #fff1f0 !important;
}

.schedule-order-pool :deep(.schedule-order-pool__row--blocked .cell) {
  color: #8a1f11;
  font-weight: 600;
}

.schedule-order-pool__work-order-ref {
  display: inline-flex;
  max-width: 100%;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.schedule-order-pool__blocking-reason {
  display: inline-block;
  max-width: 100%;
  color: #cf1322;
  font-size: 12px;
  line-height: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-order-pool__freeze-badge {
  display: inline-flex;
  min-width: 72px;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  color: #667085;
  font-size: 12px;
  font-weight: 500;
  line-height: 22px;
}

.schedule-order-pool__freeze-badge--active {
  border-color: #fa8c16;
  background: #fa8c16;
  box-shadow: 0 0 0 2px #fff1d6;
  color: #ffffff;
  font-weight: 700;
}

.schedule-order-pool__freeze-icon {
  font-size: 14px;
}

.schedule-order-pool__process-dialog-table {
  width: 100%;
  overflow-x: auto;
}

.schedule-order-pool__process-dialog-table :deep(.schedule-order-pool__process-summary-table) {
  min-width: 1120px;
}

.schedule-order-pool__feedback-history {
  padding: 12px 18px 14px;
  background: #fafcff;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}

.schedule-order-pool__feedback-history-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #172033;
  font-size: 13px;
  font-weight: 700;
}

.schedule-order-pool__feedback-history-title span {
  color: #4b5563;
  font-weight: 500;
}

.schedule-order-pool__feedback-history-list :deep(.unified-list-template__table-shell) {
  overflow: hidden;
  border-color: #dbe3ef;
  border-radius: 6px;
}

.schedule-order-pool__feedback-history-list :deep(.el-table) {
  font-size: 13px;
}

.schedule-order-pool__capacity-snapshot {
  margin-bottom: 10px;
  padding: 12px 18px 14px;
  background: #fffdfa;
  border: 1px solid #f0dfbf;
  border-radius: 6px;
}

.schedule-order-pool__capacity-snapshot-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #172033;
  font-size: 13px;
  font-weight: 700;
}

.schedule-order-pool__capacity-snapshot-title span {
  color: #8a5a12;
  font-weight: 600;
}

.schedule-order-pool__capacity-snapshot-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(72px, auto));
  gap: 8px 12px;
  align-items: center;
  color: #4b5563;
  font-size: 13px;
}

.schedule-order-pool__capacity-snapshot-grid strong {
  color: #172033;
  font-weight: 600;
}

.schedule-order-pool__capacity-snapshot-resources {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.schedule-order-pool__work-order-code {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.schedule-order-pool__work-order-code--scheduled {
  color: #d46b08;
}

.schedule-order-pool__work-order-code--unscheduled {
  color: #172033;
}

.schedule-order-pool__work-order-code--finished {
  color: #389e0d;
}

.schedule-order-pool__product-code {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.schedule-order-pool__product-code--scheduled {
  color: #d46b08;
}

.schedule-order-pool__product-code--unscheduled {
  color: #172033;
}

.schedule-order-pool__quantity-progress {
  display: flex;
  min-width: 190px;
  flex-direction: column;
  gap: 6px;
}

.schedule-order-pool__quantity-main {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.schedule-order-pool__quantity-main span {
  color: #263247;
  font-size: 13px;
}

.schedule-order-pool__quantity-main strong {
  color: #1677ff;
  font-variant-numeric: tabular-nums;
}

.schedule-order-pool__quantity-progress :deep(.el-progress) {
  flex: 1;
}

.schedule-order-pool__progress {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 120px;
}

.schedule-order-pool__progress span {
  width: 42px;
  color: #263247;
  font-variant-numeric: tabular-nums;
  text-align: right;
}

.schedule-order-pool__progress :deep(.el-progress) {
  flex: 1;
}

.schedule-order-pool :deep(.schedule-order-pool__process-row--finished td.el-table__cell) {
  background: #f0f9eb;
}

.schedule-order-pool :deep(.schedule-order-pool__process-row--finished .cell) {
  color: #237804;
}

.schedule-order-pool
  :deep(.schedule-order-pool__process-row--scheduled-not-started td.el-table__cell) {
  background: #e6f4ff;
}

.schedule-order-pool :deep(.schedule-order-pool__process-row--scheduled-not-started .cell) {
  color: #0958d9;
}

.schedule-order-pool :deep(.schedule-order-pool__process-row--unscheduled td.el-table__cell) {
  background: #fff1f0;
}

.schedule-order-pool :deep(.schedule-order-pool__process-row--unscheduled .cell) {
  color: #cf1322;
}

.schedule-order-pool :deep(.schedule-order-pool__process-row--in-progress td.el-table__cell) {
  background: #fff7e6;
}

.schedule-order-pool :deep(.schedule-order-pool__process-row--in-progress .cell) {
  color: #d46b08;
}

.schedule-order-pool__issue-product {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.schedule-order-pool__issue-product span,
.schedule-order-pool__issue-product small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-order-pool__issue-product span {
  color: #263247;
}

.schedule-order-pool__issue-product small {
  color: #6b7280;
  font-variant-numeric: tabular-nums;
}

.schedule-order-pool__issue-remark {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 10px;
  color: #4b5563;
}

.schedule-order-pool__issue-remark span {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-order-pool__current-process {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 18px;
}

.schedule-order-pool__inline-link {
  justify-content: flex-start;
  padding: 0;
}

.schedule-order-pool__warning-text {
  color: #d46b08;
  font-weight: 600;
}

.schedule-order-pool__risk-text {
  color: #cf1322;
  font-weight: 600;
}

.schedule-order-pool__risk-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.schedule-order-pool__risk-indicator {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: #ad4e00;
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  white-space: normal;
  word-break: break-word;
}

.schedule-order-pool__risk-indicator--critical {
  color: #b42318;
}

.schedule-order-pool__material-missing {
  color: #cf1322;
  font-weight: 600;
}

.schedule-order-pool__missing-value-hint {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  gap: 4px;
  line-height: 20px;
  white-space: nowrap;
  cursor: help;
}

.schedule-order-pool__missing-value-hint:focus-visible {
  border-radius: 2px;
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.schedule-order-pool__missing-value-hint .icon {
  flex: 0 0 auto;
}

:global(.schedule-order-pool__missing-value-popper) {
  max-width: 360px;
  line-height: 20px;
  white-space: normal;
  word-break: break-word;
}

.schedule-order-pool__current-process-missing {
  color: var(--el-text-color-secondary);
}

.schedule-order-pool__reason-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.schedule-order-pool__reason-cell span:last-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.schedule-order-pool__admission-message-cell {
  align-items: flex-start;
  flex-wrap: wrap;
}

.schedule-order-pool__trace-dialog {
  min-height: 180px;
}

.schedule-order-pool__trace-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.schedule-order-pool__trace-summary-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.schedule-order-pool__trace-summary-item span {
  color: #6b7280;
  font-size: 12px;
}

.schedule-order-pool__trace-summary-item strong {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-order-pool__trace-empty {
  padding: 24px 0 18px;
  border: 1px dashed #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.schedule-order-pool__trace-timeline {
  display: flex;
  max-height: min(620px, calc(100vh - 300px));
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding-right: 4px;
}

.schedule-order-pool__trace-card {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.schedule-order-pool__trace-card-line {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 10px;
  padding: 14px;
}

.schedule-order-pool__trace-dot {
  width: 10px;
  height: 10px;
  margin-top: 7px;
  border: 2px solid #ffffff;
  border-radius: 50%;
  background: #1677ff;
  box-shadow: 0 0 0 2px #d6e8ff;
}

.schedule-order-pool__trace-card-main {
  min-width: 0;
}

.schedule-order-pool__trace-card-head,
.schedule-order-pool__trace-card-head > div,
.schedule-order-pool__trace-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.schedule-order-pool__trace-card-head {
  justify-content: space-between;
  color: #172033;
}

.schedule-order-pool__trace-card-head strong {
  color: #263247;
}

.schedule-order-pool__trace-card-head time,
.schedule-order-pool__trace-card-meta {
  color: #6b7280;
  font-size: 12px;
}

.schedule-order-pool__trace-card-meta {
  flex-wrap: wrap;
  margin-top: 8px;
}

.schedule-order-pool__trace-card-reason {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #f7f9fc;
}

.schedule-order-pool__trace-card-reason span {
  color: #6b7280;
  font-size: 12px;
}

.schedule-order-pool__trace-card-reason p {
  margin: 0;
  color: #263247;
  line-height: 20px;
  overflow-wrap: anywhere;
}

.schedule-order-pool__trace-diff {
  margin-top: 12px;
}

.schedule-order-pool__trace-diff-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #172033;
  font-size: 13px;
  font-weight: 700;
}

.schedule-order-pool__trace-diff-head small {
  color: #6b7280;
  font-size: 12px;
  font-weight: 400;
}

.schedule-order-pool__trace-value {
  display: block;
  min-height: 28px;
  padding: 5px 8px;
  border-radius: 6px;
  background: #f7f9fc;
  color: #263247;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

@media (max-width: 960px) {
  .schedule-order-pool__trace-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .schedule-order-pool__trace-summary {
    grid-template-columns: 1fr;
  }

  .schedule-order-pool__trace-card-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .schedule-order-pool__trace-card-reason {
    grid-template-columns: 1fr;
  }
}

.schedule-order-pool__admission-cell-text {
  display: inline-block;
  min-width: 0;
  overflow: visible;
  text-overflow: clip;
  white-space: normal;
  word-break: break-all;
  overflow-wrap: anywhere;
  line-height: 18px;
}

.schedule-order-pool__admission-template :deep(.unified-list-template__query-form) {
  flex-wrap: nowrap;
  align-items: center;
}

.schedule-order-pool__admission-actions {
  display: flex;
  width: 100%;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.schedule-order-pool :deep(.schedule-order-pool__admission-table__cell--wrap .cell) {
  white-space: normal;
  word-break: break-all;
  overflow-wrap: anywhere;
  text-overflow: clip;
  line-height: 18px;
}

.schedule-order-pool__admission-table-shell {
  height: 100%;
  max-width: 100%;
  overflow-x: auto;
}

.schedule-order-pool__admission-table {
  min-width: 1568px;
}

.schedule-order-pool__ahead-text {
  color: #1677ff;
  font-weight: 600;
}

.schedule-order-pool__missing-route {
  color: #722ed1;
  font-weight: 600;
}

.schedule-order-pool__compare-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.schedule-order-pool__admission-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}

.schedule-order-pool__preflight-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.schedule-order-pool__replan {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.schedule-order-pool__replan-settings {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.schedule-order-pool__capacity-alert {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #4b5563;
  line-height: 22px;
}

.schedule-order-pool__capacity-alert span:last-child {
  color: #263247;
  font-weight: 600;
}

.schedule-order-pool__replan-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.schedule-order-pool__replan-blocker {
  max-width: min(520px, 100%);
  color: var(--el-color-warning-dark-2);
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.schedule-order-pool__replan-progress {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 172px;
  color: #4b5563;
  font-size: 13px;
}

.schedule-order-pool__replan-progress :deep(.el-progress) {
  width: 108px;
  flex: 0 0 108px;
}

.schedule-order-pool__replan-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schedule-order-pool__replan-start-date {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.schedule-order-pool__replan-start-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.schedule-order-pool__replan-start-label {
  font-size: 13px;
  font-weight: 600;
  color: #263247;
}

.schedule-order-pool__replan-start-hint {
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  color: #263247;
  font-size: 13px;
  line-height: 20px;
}

.schedule-order-pool__dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.schedule-order-pool__preflight-panel {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fafafa;
}

.schedule-order-pool__preflight-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.schedule-order-pool__preflight-title {
  margin-right: 10px;
  font-weight: 600;
  color: #263247;
}

.schedule-order-pool__preflight-time {
  color: #6b7280;
  font-size: 12px;
}
</style>
