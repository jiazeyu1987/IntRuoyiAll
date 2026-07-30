<template>
  <div class="scheduler-workbench" v-loading="loading">
    <Dialog v-model="schedulerSettingsDialogVisible" title="排产设置" width="1280px">
      <div class="scheduler-workbench__settings-dialog" v-loading="schedulerSettingsLoading">
        <div class="scheduler-workbench__panel-head">
          <span>排产</span>
          <small>班时策略</small>
        </div>
        <div class="scheduler-workbench__settings-grid">
        <el-form
          ref="shiftHoursFormRef"
          :model="shiftHoursForm"
          :rules="shiftHoursRules"
          class="scheduler-workbench__shift-form scheduler-workbench__settings-block"
          label-width="0"
        >
          <div class="scheduler-workbench__settings-block-head">
            <span>班时</span>
            <small>{{ shiftHoursSettingText }}</small>
          </div>
          <div class="scheduler-workbench__settings-row">
            <el-form-item
              label="1班"
              prop="shiftHours"
              class="scheduler-workbench__settings-field"
            >
              <div class="scheduler-workbench__input-with-unit">
                <el-input-number
                  v-model="shiftHoursForm.shiftHours"
                  :min="0.01"
                  :precision="2"
                  controls-position="right"
                  class="scheduler-workbench__settings-control"
                  :disabled="!canUpdateSettings"
                />
                <span class="scheduler-workbench__shift-unit">时</span>
              </div>
            </el-form-item>
            <div class="scheduler-workbench__settings-actions scheduler-workbench__settings-actions--compact">
              <el-button
                v-if="canUpdateSettings"
                type="primary"
                class="scheduler-workbench__settings-button"
                :loading="shiftHoursSaving"
                @click="saveShiftHoursSetting"
              >
                保存
              </el-button>
            </div>
          </div>
        </el-form>

        <el-form
          class="scheduler-workbench__schedule-rules-form scheduler-workbench__settings-block"
          label-width="0"
          v-loading="scheduleRulesLoading"
        >
          <div class="scheduler-workbench__settings-block-head">
            <span>排程规则</span>
            <small>同步日历休息日规则</small>
          </div>
          <div class="scheduler-workbench__settings-row">
            <el-form-item
              label="跳过法定节假日"
              class="scheduler-workbench__settings-field"
            >
              <div class="scheduler-workbench__settings-control scheduler-workbench__switch-control">
                <el-switch
                  v-model="scheduleRulesForm.skipStatutoryHolidays"
                  :disabled="!canUpdateSettings"
                />
              </div>
            </el-form-item>
            <el-form-item label="周末模式" class="scheduler-workbench__settings-field">
              <el-select
                v-model="scheduleRulesForm.weekendRestMode"
                class="scheduler-workbench__settings-control"
                :disabled="!canUpdateSettings"
              >
                <el-option label="双休" value="DOUBLE" />
                <el-option label="单休" value="SINGLE" />
                <el-option label="周末全上班" value="NONE" />
              </el-select>
            </el-form-item>
            <div class="scheduler-workbench__settings-actions scheduler-workbench__settings-actions--compact">
              <el-button
                v-if="canUpdateSettings"
                type="primary"
                class="scheduler-workbench__settings-button"
                :disabled="!scheduleRulesDirty"
                :loading="scheduleRulesSaving"
                @click="saveScheduleRules"
              >
                保存规则
              </el-button>
            </div>
          </div>
        </el-form>

        <el-form
          ref="policySettingsFormRef"
          :model="policySettingsForm"
          :rules="policySettingsRules"
          class="scheduler-workbench__policy-form scheduler-workbench__settings-block"
          label-width="0"
        >
          <div class="scheduler-workbench__settings-block-head">
            <span>策略</span>
            <small>同步重排优先保护</small>
          </div>
          <el-form-item
            label="同步时"
            prop="erpWorkOrderSyncTime"
            class="scheduler-workbench__policy-item"
          >
            <el-input
              v-model="policySettingsForm.erpWorkOrderSyncTime"
              placeholder="HH:mm"
              class="scheduler-workbench__settings-control"
              :disabled="!canUpdateSettings"
            />
          </el-form-item>
          <el-form-item
            label="重排时"
            prop="nightlyReplanTime"
            class="scheduler-workbench__policy-item"
          >
            <el-input
              v-model="policySettingsForm.nightlyReplanTime"
              placeholder="HH:mm"
              class="scheduler-workbench__settings-control"
              :disabled="!canUpdateSettings"
            />
          </el-form-item>
          <el-form-item
            label="优先级"
            prop="priorityRule"
            class="scheduler-workbench__policy-item scheduler-workbench__policy-item--priority"
          >
            <el-select
              v-model="policySettingsForm.priorityRule"
              class="scheduler-workbench__settings-control"
              :disabled="!canUpdateSettings"
            >
              <el-option label="交期优先" value="PROMISE_DATE" />
              <el-option label="订单优先" value="ORDER_PRIORITY" />
              <el-option label="创建优先" value="CREATED_TIME" />
            </el-select>
          </el-form-item>
          <el-form-item label="保护项" class="scheduler-workbench__policy-item scheduler-workbench__policy-checks">
            <el-checkbox v-model="policySettingsForm.protectReportedTasks" :disabled="!canUpdateSettings">
              保报工
            </el-checkbox>
            <el-checkbox v-model="policySettingsForm.protectCompletedTasks" :disabled="!canUpdateSettings">
              保完成
            </el-checkbox>
            <el-checkbox v-model="policySettingsForm.protectLockedTasks" :disabled="!canUpdateSettings">
              保锁定
            </el-checkbox>
          </el-form-item>
          <el-form-item
            label="智能排产"
            prop="defaultScheduleUseEnabled"
            class="scheduler-workbench__policy-item"
          >
            <el-switch
              v-model="policySettingsForm.defaultScheduleUseEnabled"
              :disabled="!canUpdateSettings"
              inline-prompt
              active-text="开启"
              inactive-text="关闭"
            />
          </el-form-item>
          <el-form-item
            label="产能模式"
            prop="defaultScheduleCapacityMode"
            class="scheduler-workbench__policy-item scheduler-workbench__policy-item--priority"
          >
            <el-select
              v-model="policySettingsForm.defaultScheduleCapacityMode"
              class="scheduler-workbench__settings-control"
              :disabled="!canUpdateSettings"
            >
              <el-option label="资源计算" value="RESOURCE_CALCULATED" />
              <el-option label="产能覆盖" value="MANUAL_OVERRIDE" />
              <el-option label="无限" value="INFINITE_FORMULA" />
            </el-select>
          </el-form-item>
          <el-form-item
            v-if="policySettingsForm.defaultScheduleCapacityMode === 'MANUAL_OVERRIDE'"
            label="产能覆盖(产能/h)"
            prop="defaultFiniteHourlyCapacity"
            class="scheduler-workbench__policy-item"
          >
            <el-input-number
              v-model="policySettingsForm.defaultFiniteHourlyCapacity"
              :min="0.000001"
              :precision="6"
              controls-position="right"
              class="scheduler-workbench__settings-control"
              :disabled="!canUpdateSettings"
            />
          </el-form-item>
          <el-form-item
            v-if="policySettingsForm.defaultScheduleCapacityMode === 'INFINITE_FORMULA'"
            label="默认系数 a(h/件)"
            prop="defaultInfiniteDurationQuantityFactorHours"
            class="scheduler-workbench__policy-item"
          >
            <el-input-number
              v-model="policySettingsForm.defaultInfiniteDurationQuantityFactorHours"
              :min="0.000001"
              :precision="6"
              controls-position="right"
              class="scheduler-workbench__settings-control"
              :disabled="!canUpdateSettings"
            />
          </el-form-item>
          <el-form-item
            v-if="policySettingsForm.defaultScheduleCapacityMode === 'INFINITE_FORMULA'"
            label="默认固定值 b(h)"
            prop="defaultInfiniteDurationBaseHours"
            class="scheduler-workbench__policy-item"
          >
            <el-input-number
              v-model="policySettingsForm.defaultInfiniteDurationBaseHours"
              :min="0"
              :precision="6"
              controls-position="right"
              class="scheduler-workbench__settings-control"
              :disabled="!canUpdateSettings"
            />
          </el-form-item>
          <el-form-item
            label="夜班"
            prop="defaultNightShiftEnabled"
            class="scheduler-workbench__policy-item"
          >
            <el-switch
              v-model="policySettingsForm.defaultNightShiftEnabled"
              :disabled="!canUpdateSettings"
              inline-prompt
              active-text="开启"
              inactive-text="关闭"
            />
          </el-form-item>
          <el-form-item
            label="人数"
            prop="defaultWorkerQuantity"
            class="scheduler-workbench__policy-item"
          >
            <el-input-number
              v-model="policySettingsForm.defaultWorkerQuantity"
              :min="1"
              :precision="0"
              controls-position="right"
              class="scheduler-workbench__settings-control"
              :disabled="!canUpdateSettings"
            />
          </el-form-item>
          <el-form-item
            label="人效h"
            prop="defaultWorkerSingleHourlyCapacity"
            class="scheduler-workbench__policy-item"
          >
            <el-input-number
              v-model="policySettingsForm.defaultWorkerSingleHourlyCapacity"
              :min="0.000001"
              :precision="6"
              controls-position="right"
              class="scheduler-workbench__settings-control"
              :disabled="!canUpdateSettings"
            />
          </el-form-item>
          <div class="scheduler-workbench__settings-actions">
            <el-button
              plain
              class="scheduler-workbench__settings-button"
              :loading="fullConfigExporting"
              @click="exportFullConfigPackage"
            >
              <Icon icon="ep:download" class="mr-5px" /> 导出全部数据包
            </el-button>
            <el-button
              v-if="canUpdateSettings"
              plain
              class="scheduler-workbench__settings-button"
              :loading="fullConfigImporting"
              @click="openFullConfigImport"
            >
              <Icon icon="ep:upload" class="mr-5px" /> 导入全部数据包
            </el-button>
            <el-button
              v-if="canUpdateSettings"
              type="primary"
              class="scheduler-workbench__settings-button"
              :loading="policySettingsSaving"
              @click="savePolicySettings"
            >
              保存策略
            </el-button>
          </div>
        </el-form>

      </div>
      </div>
    </Dialog>
    <input
      ref="fullConfigInputRef"
      type="file"
      class="scheduler-workbench__hidden-input"
      accept=".json,application/json"
      @change="handleFullConfigFileChange"
    />

    <div class="scheduler-workbench__side-panels">
      <section class="scheduler-workbench__panel scheduler-workbench__wip-tabs-panel">
        <el-tabs v-model="activeWipTab" class="scheduler-workbench__wip-tabs">
          <el-tab-pane label="工序列表" name="process-list">
            <el-alert
              v-if="processWipErrorMessage"
              title="加载工序在制统计失败"
              :description="processWipErrorMessage"
              type="error"
              show-icon
              :closable="false"
            />
            <ProcessWipTable
              v-loading="processWipLoading"
              :query-model="processWipQuickFilterParams"
              :filter-definitions="schedulerWorkbenchProcessWipQuickFilterDefinitions"
              :quick-filter-state="processWipQuickFilter.state"
              :selected-filter-definition="processWipQuickFilter.selectedDefinition.value"
              :operator-options="processWipQuickFilter.operatorOptions.value"
              :columns="schedulerWorkbenchProcessWipColumns"
              :column-saving="processWipColumnSaving"
              :total="processWipTotal"
              v-model:sort-state="processWipSortState"
              @update:page="processWipQuickFilterParams.pageNo = $event"
              @update:limit="processWipQuickFilterParams.pageSize = $event"
              @update:quick-filter-state="processWipQuickFilter.updateState"
              @quick-filter-query="processWipQuickFilter.applyQuickFilter"
              @column-change="saveProcessWipColumnConfig"
              @column-reset="resetProcessWipColumnConfig"
              @sort-change="handleProcessWipSortChange"
              @pagination="handleProcessWipPagination"
            >
              <template #actions>
                <el-button type="primary" @click="openSchedulerSettingsDialog">
                  排产设置
                </el-button>
              </template>
              <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
                <el-table
                  v-if="pagedProcessWipStatistics.length"
                  data-user-table-column-explicit
                  data-user-table-key="mes.pro.schedulerWorkbench.processWip"
                  :data="pagedProcessWipStatistics"
                  class="scheduler-workbench__process-wip-table"
                  border
                  :row-key="getProcessWipRowKey"
                  @row-click="openProcessWipOrders"
                  @header-dragend="handleProcessWipHeaderDragend"
                  @sort-change="handleTemplateSortChange"
                >
                  <el-table-column
                    v-if="isProcessWipColumnVisible('routeCode')"
                    label="工艺路线编码"
                    prop="routeCode"
                    :width="getProcessWipColumnLayoutWidthString('routeCode', 150)"
                    :min-width="getProcessWipColumnMinWidthString('routeCode', 136)"
                    show-overflow-tooltip
                    v-bind="sortColumnAttrs('routeCode')"
                  >
                    <template #default="{ row }">
                      {{ row.routeCode || '无路线编码' }}
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('routeName')"
                    label="工艺路线名称"
                    prop="routeName"
                    :width="getProcessWipColumnLayoutWidthString('routeName', 180)"
                    :min-width="getProcessWipColumnMinWidthString('routeName', 160)"
                    show-overflow-tooltip
                    v-bind="sortColumnAttrs('routeName')"
                  >
                    <template #default="{ row }">
                      {{ row.routeName || row.routeCode || '未命名工艺路线' }}
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('processCode')"
                    label="工序编号"
                    prop="processCode"
                    :width="getProcessWipColumnLayoutWidthString('processCode', 130)"
                    :min-width="getProcessWipColumnMinWidthString('processCode', 120)"
                    show-overflow-tooltip
                    v-bind="sortColumnAttrs('processCode')"
                  >
                    <template #default="{ row }">
                      <span class="scheduler-workbench__table-link">
                        {{ row.processCode || '无工序编码' }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('processName')"
                    label="工序名称"
                    prop="processName"
                    :width="getProcessWipColumnLayoutWidthString('processName', 160)"
                    :min-width="getProcessWipColumnMinWidthString('processName', 140)"
                    show-overflow-tooltip
                    v-bind="sortColumnAttrs('processName')"
                  >
                    <template #default="{ row }">
                      {{ row.processName || row.processCode || '未命名工序' }}
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('wipOrderCount')"
                    label="在制单数"
                    prop="wipOrderCount"
                    :width="getProcessWipColumnLayoutWidthString('wipOrderCount', 148)"
                    :min-width="getProcessWipColumnMinWidthString('wipOrderCount', 132)"
                    align="right"
                    v-bind="sortColumnAttrs('wipOrderCount')"
                  >
                    <template #default="{ row }">
                      {{ formatIntegerNumber(row.wipOrderCount) }} 个订单在做
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('shiftCapacityTotal')"
                    label="班次产能"
                    prop="shiftCapacityTotal"
                    :width="getProcessWipColumnLayoutWidthString('shiftCapacityTotal', 116)"
                    :min-width="getProcessWipColumnMinWidthString('shiftCapacityTotal', 104)"
                    align="right"
                    v-bind="sortColumnAttrs('shiftCapacityTotal')"
                  >
                    <template #default="{ row }">
                      <div class="scheduler-workbench__shift-capacity">
                        <button
                          type="button"
                          class="scheduler-workbench__capacity-source-link"
                          :title="getProcessWipCapacitySourceTooltip(row)"
                          @click.stop="openProcessWipCapacitySource(row)"
                        >
                          {{ formatProcessWipShiftCapacity(row.shiftCapacityTotal) }}
                        </button>
                        <el-tag
                          v-if="isProcessWipDoubleShift(row)"
                          size="small"
                          type="success"
                          class="scheduler-workbench__shift-capacity-multiplier"
                        >
                          X2
                        </el-tag>
                        <el-tag
                          v-if="isProcessWipResourceMissing(row)"
                          size="small"
                          type="warning"
                          class="scheduler-workbench__resource-status"
                        >
                          {{ row.resourceStatusReason || '资源缺失' }}
                        </el-tag>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('shiftStatus')"
                    label="班次状态"
                    prop="shiftStatus"
                    :width="getProcessWipColumnLayoutWidthString('shiftStatus', 104)"
                    :min-width="getProcessWipColumnMinWidthString('shiftStatus', 96)"
                    align="center"
                    v-bind="sortColumnAttrs('shiftStatus')"
                  >
                    <template #default="{ row }">
                      <el-tag size="small" :type="getProcessWipShiftStatusTagType()">
                        {{ getProcessWipShiftStatusText(row) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('nightShiftEnabled')"
                    label="夜班"
                    prop="nightShiftEnabled"
                    :width="getProcessWipColumnLayoutWidthString('nightShiftEnabled', 116)"
                    :min-width="getProcessWipColumnMinWidthString('nightShiftEnabled', 108)"
                    align="center"
                    v-bind="sortColumnAttrs('nightShiftEnabled')"
                  >
                    <template #default="{ row }">
                      <div class="scheduler-workbench__inline-control" @click.stop>
                        <el-switch
                          :model-value="Boolean(row.nightShiftEnabled)"
                          :disabled="processWipSettingsSavingId === getProcessWipRowKey(row)"
                          inline-prompt
                          active-text="开"
                          inactive-text="关"
                          @change="handleProcessWipNightShiftChange(row, Boolean($event))"
                        />
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('plannedStartDate')"
                    label="开排日期"
                    prop="plannedStartDate"
                    :width="getProcessWipColumnLayoutWidthString('plannedStartDate', 164)"
                    :min-width="getProcessWipColumnMinWidthString('plannedStartDate', 150)"
                    align="center"
                    v-bind="sortColumnAttrs('plannedStartDate')"
                  >
                    <template #default="{ row }">
                      <div class="scheduler-workbench__inline-control" @click.stop>
                        <el-date-picker
                          v-model="processWipPlannedStartDateDrafts[getProcessWipRowKey(row)]"
                          type="date"
                          value-format="YYYY-MM-DD"
                          placeholder="未设置"
                          :disabled="processWipSettingsSavingId === getProcessWipRowKey(row)"
                          class="scheduler-workbench__process-wip-date"
                          @change="handleProcessWipPlannedStartDateChange(row, $event)"
                        />
                        <el-tag v-if="row.plannedStartDateMixed" size="small" type="warning">混合</el-tag>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('unfinishedDemandQuantity')"
                    label="未完需求"
                    prop="unfinishedDemandQuantity"
                    :width="getProcessWipColumnLayoutWidthString('unfinishedDemandQuantity', 140)"
                    :min-width="getProcessWipColumnMinWidthString('unfinishedDemandQuantity', 128)"
                    align="right"
                    v-bind="sortColumnAttrs('unfinishedDemandQuantity')"
                  >
                    <template #default="{ row }">
                      {{ formatNumber(row.unfinishedDemandQuantity) }}
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('estimatedStartTime')"
                    label="预计开始"
                    prop="estimatedStartTime"
                    :width="getProcessWipColumnLayoutWidthString('estimatedStartTime', 168)"
                    :min-width="getProcessWipColumnMinWidthString('estimatedStartTime', 156)"
                    show-overflow-tooltip
                    v-bind="sortColumnAttrs('estimatedStartTime')"
                  >
                    <template #default="{ row }">
                      {{ formatEstimatedTime(row.estimatedStartTime) }}
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('estimatedCompletionTime')"
                    label="预计完工"
                    prop="estimatedCompletionTime"
                    :width="getProcessWipColumnLayoutWidthString('estimatedCompletionTime', 168)"
                    :min-width="getProcessWipColumnMinWidthString('estimatedCompletionTime', 156)"
                    show-overflow-tooltip
                    v-bind="sortColumnAttrs('estimatedCompletionTime')"
                  >
                    <template #default="{ row }">
                      {{ formatEstimatedTime(row.estimatedCompletionTime) }}
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isProcessWipColumnVisible('todayFeedbackQuantity')"
                    label="今日报工"
                    prop="todayFeedbackQuantity"
                    :width="getProcessWipColumnLayoutWidthString('todayFeedbackQuantity', 148)"
                    :min-width="getProcessWipColumnMinWidthString('todayFeedbackQuantity', 132)"
                    align="right"
                    v-bind="sortColumnAttrs('todayFeedbackQuantity')"
                  >
                    <template #default="{ row }">
                      {{ formatNumber(row.todayFeedbackQuantity) }}
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty
                  v-else-if="!processWipLoading"
                  description="暂无工序在制订单"
                  :image-size="48"
                />
              </template>
            </ProcessWipTable>
          </el-tab-pane>

          <el-tab-pane label="工艺路线在制订单" name="route-active">
            <div class="scheduler-workbench__tab-head">
              <span>工艺路线在制订单</span>
              <small>按工艺路线统计几个订单在做</small>
            </div>
            <UnifiedListTemplate
              table-key="mes.pro.schedulerWorkbench.routeActiveOrders"
              :query-model="standardListQueryModel"
              :filter-definitions="standardListFilterDefinitions"
              :quick-filter-state="standardListQuickFilterState"
              :operator-options="standardListOperatorOptions"
              :columns="routeActiveOrderColumns"
              :show-query-form="false"
              :show-column-settings="false"
              :total="0"
              :page="1"
              :limit="20"
            >
              <template #table>
                <el-table
                  :data="summary.routeActiveOrders || []"
                  data-user-table-column-explicit
                  data-user-table-key="mes.pro.schedulerWorkbench.routeActiveOrders"
                  border
                  size="small"
                  empty-text="暂无工艺路线在制订单"
                  @header-dragend="handleRouteActiveOrderHeaderDragend"
                >
                  <el-table-column
                    label="工艺路线"
                    prop="routeName"
                    :width="getRouteActiveOrderColumnWidthString('routeName', 220)"
                    :min-width="getRouteActiveOrderColumnMinWidthString('routeName', 180)"
                    show-overflow-tooltip
                  >
                    <template #default="{ row }">
                      {{ row.routeName || row.routeCode || '未命名工艺路线' }}
                    </template>
                  </el-table-column>
                  <el-table-column
                    label="在制订单"
                    prop="wipOrderCount"
                    :width="getRouteActiveOrderColumnWidthString('wipOrderCount', 116)"
                    :min-width="getRouteActiveOrderColumnMinWidthString('wipOrderCount', 104)"
                    align="right"
                  >
                    <template #default="{ row }">{{ row.wipOrderCount }} 个</template>
                  </el-table-column>
                  <el-table-column
                    label="路线编码"
                    prop="routeCode"
                    :width="getRouteActiveOrderColumnWidthString('routeCode', 160)"
                    :min-width="getRouteActiveOrderColumnMinWidthString('routeCode', 136)"
                    show-overflow-tooltip
                  >
                    <template #default="{ row }">{{ row.routeCode || '无路线编码' }}</template>
                  </el-table-column>
                  <el-table-column
                    label="关联产品"
                    prop="products"
                    :min-width="getRouteActiveOrderColumnMinWidthString('products', 280)"
                    show-overflow-tooltip
                  >
                    <template #default="{ row }">{{ routeActiveProductsText(row) }}</template>
                  </el-table-column>
                </el-table>
              </template>
            </UnifiedListTemplate>
          </el-tab-pane>

          <el-tab-pane label="排产逻辑" name="algorithm-guide">
            <div
              v-loading="replanExplanationLoading"
              class="scheduler-workbench__algorithm-guide"
            >
              <el-alert
                v-if="replanExplanationError"
                title="加载重排说明失败"
                :description="replanExplanationError"
                type="error"
                show-icon
                :closable="false"
              />
              <el-empty
                v-else-if="!replanExplanationLoading && !replanExplanation?.hasData"
                description="暂无已应用的重排记录"
                :image-size="64"
              />
              <template v-else-if="replanExplanation?.hasData">
                <div class="scheduler-workbench__algorithm-intro">
                  <div>
                    <strong>最近一次成功重排</strong>
                    <span>
                      {{ formatReplanTrigger(replanExplanation.triggerSource) }} ·
                      {{ formatExplanationDateTime(replanExplanation.appliedAt) }}
                    </span>
                  </div>
                  <div class="scheduler-workbench__algorithm-meta">
                    <el-tag size="small" type="primary">
                      {{ formatCapacityMode(replanExplanation.capacityMode) }}
                    </el-tag>
                    <span>操作人：{{ replanExplanation.operatorName || '—' }}</span>
                    <span>开始：{{ formatExplanationDateTime(replanExplanation.requestStartTime) }}</span>
                    <span>原因：{{ replanExplanation.reason || '未填写' }}</span>
                  </div>
                </div>

                <section class="scheduler-workbench__algorithm-section">
                  <div class="scheduler-workbench__algorithm-section-head">
                    <span class="scheduler-workbench__algorithm-step-no">1</span>
                    <div>
                      <strong>检查数据</strong>
                      <small>先确认订单、路线、工序和问题。</small>
                    </div>
                  </div>
                  <div class="scheduler-workbench__algorithm-metrics">
                    <div><span>排产订单</span><strong>{{ replanExplanation.summary?.scheduleOrderCount || 0 }}</strong></div>
                    <div><span>生产工单</span><strong>{{ replanExplanation.summary?.workOrderCount || 0 }}</strong></div>
                    <div><span>工艺路线</span><strong>{{ replanExplanation.summary?.routeCount || 0 }}</strong></div>
                    <div><span>工序</span><strong>{{ replanExplanation.summary?.processCount || 0 }}</strong></div>
                    <div><span>阻断问题</span><strong class="is-danger">{{ replanExplanation.summary?.blockingIssueCount || 0 }}</strong></div>
                    <div><span>提醒问题</span><strong class="is-warning">{{ replanExplanation.summary?.warningIssueCount || 0 }}</strong></div>
                  </div>
                </section>

                <section class="scheduler-workbench__algorithm-section">
                  <div class="scheduler-workbench__algorithm-section-head">
                    <span class="scheduler-workbench__algorithm-step-no">2</span>
                    <div>
                      <strong>订单顺序</strong>
                      <small>交期早、优先级数字小的排在前面。</small>
                    </div>
                  </div>
                  <UnifiedListTemplate
                    table-key="mes.pro.schedulerWorkbench.replan.orders"
                    :query-model="standardListQueryModel"
                    :filter-definitions="standardListFilterDefinitions"
                    :quick-filter-state="standardListQuickFilterState"
                    :operator-options="standardListOperatorOptions"
                    :columns="replanOrderColumns"
                    :show-query-form="false"
                    :show-column-settings="false"
                    :total="0"
                    :page="1"
                    :limit="20"
                  >
                    <template #table>
                      <el-table
                        :data="replanExplanation.orders || []"
                        data-user-table-column-explicit
                        data-user-table-key="mes.pro.schedulerWorkbench.replan.orders"
                        border
                        size="small"
                        empty-text="暂无订单顺序"
                        @header-dragend="handleReplanOrderHeaderDragend"
                      >
                        <el-table-column label="顺序" prop="rank" :width="getReplanOrderColumnWidthString('rank', 68)" :min-width="getReplanOrderColumnMinWidthString('rank', 60)" align="center" />
                        <el-table-column label="排产订单" prop="scheduleOrderCode" :width="getReplanOrderColumnWidthString('scheduleOrderCode', 150)" :min-width="getReplanOrderColumnMinWidthString('scheduleOrderCode', 132)" show-overflow-tooltip />
                        <el-table-column label="生产工单" prop="workOrderCode" :width="getReplanOrderColumnWidthString('workOrderCode', 150)" :min-width="getReplanOrderColumnMinWidthString('workOrderCode', 132)" show-overflow-tooltip />
                        <el-table-column label="产品" :width="getReplanOrderColumnWidthString('product', 190)" :min-width="getReplanOrderColumnMinWidthString('product', 170)" show-overflow-tooltip>
                          <template #default="{ row }">{{ row.productName || row.productCode || '—' }}</template>
                        </el-table-column>
                        <el-table-column label="数量" prop="quantity" :width="getReplanOrderColumnWidthString('quantity', 110)" :min-width="getReplanOrderColumnMinWidthString('quantity', 96)" align="right">
                          <template #default="{ row }">{{ formatExplanationNumber(row.quantity) }}</template>
                        </el-table-column>
                        <el-table-column label="交期" prop="promiseDate" :width="getReplanOrderColumnWidthString('promiseDate', 112)" :min-width="getReplanOrderColumnMinWidthString('promiseDate', 100)" />
                        <el-table-column label="优先级" prop="priorityNo" :width="getReplanOrderColumnWidthString('priorityNo', 88)" :min-width="getReplanOrderColumnMinWidthString('priorityNo', 78)" align="center" />
                        <el-table-column label="工序数" prop="processCount" :width="getReplanOrderColumnWidthString('processCount', 88)" :min-width="getReplanOrderColumnMinWidthString('processCount', 78)" align="right" />
                      </el-table>
                    </template>
                  </UnifiedListTemplate>
                </section>

                <section class="scheduler-workbench__algorithm-section">
                  <div class="scheduler-workbench__algorithm-section-head">
                    <span class="scheduler-workbench__algorithm-step-no">3</span>
                    <div>
                      <strong>拆分工序</strong>
                      <small>每个工单按工艺路线拆成需要依次完成的工序。</small>
                    </div>
                  </div>
                  <UnifiedListTemplate
                    table-key="mes.pro.schedulerWorkbench.replan.workOrders"
                    :query-model="standardListQueryModel"
                    :filter-definitions="standardListFilterDefinitions"
                    :quick-filter-state="standardListQuickFilterState"
                    :operator-options="standardListOperatorOptions"
                    :columns="replanWorkOrderColumns"
                    :show-query-form="false"
                    :show-column-settings="false"
                    :total="0"
                    :page="1"
                    :limit="20"
                  >
                    <template #table>
                      <el-table
                        :data="replanExplanation.workOrders || []"
                        data-user-table-column-explicit
                        data-user-table-key="mes.pro.schedulerWorkbench.replan.workOrders"
                        border
                        size="small"
                        empty-text="暂无工单拆分"
                        @header-dragend="handleReplanWorkOrderHeaderDragend"
                      >
                        <el-table-column label="生产工单" prop="workOrderCode" :width="getReplanWorkOrderColumnWidthString('workOrderCode', 160)" :min-width="getReplanWorkOrderColumnMinWidthString('workOrderCode', 140)" />
                        <el-table-column label="工艺路线" :width="getReplanWorkOrderColumnWidthString('route', 180)" :min-width="getReplanWorkOrderColumnMinWidthString('route', 160)" show-overflow-tooltip>
                          <template #default="{ row }">{{ row.routeName || row.routeCode || '—' }}</template>
                        </el-table-column>
                        <el-table-column label="工序数" :width="getReplanWorkOrderColumnWidthString('processCount', 88)" :min-width="getReplanWorkOrderColumnMinWidthString('processCount', 78)" align="right">
                          <template #default="{ row }">{{ row.processes?.length || 0 }}</template>
                        </el-table-column>
                        <el-table-column label="工序顺序" :min-width="getReplanWorkOrderColumnMinWidthString('processSequence', 280)">
                          <template #default="{ row }">
                            {{ (row.processes || []).map((process) => process.processName).join(' → ') || '—' }}
                          </template>
                        </el-table-column>
                      </el-table>
                    </template>
                  </UnifiedListTemplate>
                </section>

                <section class="scheduler-workbench__algorithm-section">
                  <div class="scheduler-workbench__algorithm-section-head">
                    <span class="scheduler-workbench__algorithm-step-no">4</span>
                    <div>
                      <strong>计算产能</strong>
                      <small>按班次、工作站、设备和人员计算每道工序需要的时间。</small>
                    </div>
                  </div>
                  <el-collapse class="scheduler-workbench__algorithm-collapse">
                    <el-collapse-item
                      v-for="workOrder in replanExplanation.workOrders || []"
                      :key="workOrder.workOrderId || workOrder.workOrderCode"
                      :name="workOrder.workOrderId || workOrder.workOrderCode"
                    >
                      <template #title>
                        <div class="scheduler-workbench__algorithm-collapse-title">
                          <strong>{{ workOrder.workOrderCode || '未编号工单' }}</strong>
                          <span>{{ workOrder.routeName || workOrder.routeCode || '未命名路线' }}</span>
                          <el-tag v-if="workOrder.bottleneckProcessName" size="small" type="warning">
                            瓶颈：{{ workOrder.bottleneckProcessName }}
                          </el-tag>
                        </div>
                      </template>
                      <UnifiedListTemplate
                        table-key="mes.pro.schedulerWorkbench.replan.processes"
                        :query-model="standardListQueryModel"
                        :filter-definitions="standardListFilterDefinitions"
                        :quick-filter-state="standardListQuickFilterState"
                        :operator-options="standardListOperatorOptions"
                        :columns="replanProcessColumns"
                        :show-query-form="false"
                        :show-column-settings="false"
                        :total="0"
                        :page="1"
                        :limit="20"
                      >
                        <template #table>
                          <el-table
                            :data="workOrder.processes || []"
                            data-user-table-column-explicit
                            data-user-table-key="mes.pro.schedulerWorkbench.replan.processes"
                            border
                            size="small"
                            empty-text="暂无工序产能"
                            @header-dragend="handleReplanProcessHeaderDragend"
                          >
                            <el-table-column label="序号" prop="processSort" :width="getReplanProcessColumnWidthString('processSort', 68)" :min-width="getReplanProcessColumnMinWidthString('processSort', 60)" align="center" />
                            <el-table-column label="工序" prop="processName" :width="getReplanProcessColumnWidthString('processName', 140)" :min-width="getReplanProcessColumnMinWidthString('processName', 120)" show-overflow-tooltip />
                            <el-table-column label="数量" prop="scheduledQuantity" :width="getReplanProcessColumnWidthString('scheduledQuantity', 96)" :min-width="getReplanProcessColumnMinWidthString('scheduledQuantity', 86)" align="right">
                              <template #default="{ row }">{{ formatExplanationNumber(row.scheduledQuantity) }}</template>
                            </el-table-column>
                            <el-table-column label="班次" :width="getReplanProcessColumnWidthString('shiftNames', 120)" :min-width="getReplanProcessColumnMinWidthString('shiftNames', 110)">
                              <template #default="{ row }">{{ (row.shiftNames || []).join('、') || '—' }}</template>
                            </el-table-column>
                            <el-table-column label="工作站" :width="getReplanProcessColumnWidthString('workstationNames', 170)" :min-width="getReplanProcessColumnMinWidthString('workstationNames', 150)" show-overflow-tooltip>
                              <template #default="{ row }">{{ (row.workstationNames || []).join('、') || '—' }}</template>
                            </el-table-column>
                            <el-table-column label="设备" prop="machineCount" :width="getReplanProcessColumnWidthString('machineCount', 76)" :min-width="getReplanProcessColumnMinWidthString('machineCount', 68)" align="right" />
                            <el-table-column label="人员" :width="getReplanProcessColumnWidthString('workers', 112)" :min-width="getReplanProcessColumnMinWidthString('workers', 96)" align="right">
                              <template #default="{ row }">
                                {{ row.currentWorkerCount || 0 }}/{{ row.configuredWorkerCount || 0 }}
                              </template>
                            </el-table-column>
                            <el-table-column label="班次产能" prop="effectiveHourlyCapacity" :width="getReplanProcessColumnWidthString('effectiveHourlyCapacity', 112)" :min-width="getReplanProcessColumnMinWidthString('effectiveHourlyCapacity', 100)" align="right">
                              <template #default="{ row }">{{ formatExplanationNumber(row.effectiveHourlyCapacity) }}/时</template>
                            </el-table-column>
                            <el-table-column label="预计时长" prop="plannedDurationMinutes" :width="getReplanProcessColumnWidthString('plannedDurationMinutes', 104)" :min-width="getReplanProcessColumnMinWidthString('plannedDurationMinutes', 96)" align="right">
                              <template #default="{ row }">{{ formatExplanationDuration(row.plannedDurationMinutes) }}</template>
                            </el-table-column>
                            <el-table-column label="时间" :min-width="getReplanProcessColumnMinWidthString('time', 220)">
                              <template #default="{ row }">
                                {{ formatExplanationDateTime(row.startTime) }} 至
                                {{ formatExplanationDateTime(row.endTime) }}
                              </template>
                            </el-table-column>
                            <el-table-column label="结果" :width="getReplanProcessColumnWidthString('result', 82)" :min-width="getReplanProcessColumnMinWidthString('result', 72)" align="center">
                              <template #default="{ row }">
                                <el-tag v-if="row.bottleneck" size="small" type="warning">瓶颈</el-tag>
                                <span v-else>正常</span>
                              </template>
                            </el-table-column>
                          </el-table>
                        </template>
                      </UnifiedListTemplate>
                    </el-collapse-item>
                  </el-collapse>
                </section>

                <section class="scheduler-workbench__algorithm-section">
                  <div class="scheduler-workbench__algorithm-section-head">
                    <span class="scheduler-workbench__algorithm-step-no">5</span>
                    <div>
                      <strong>受保护任务</strong>
                      <small>现场已经发生的任务不会被移动。</small>
                    </div>
                  </div>
                  <div class="scheduler-workbench__algorithm-metrics">
                    <div><span>合计</span><strong>{{ replanExplanation.protectionSummary?.totalCount || 0 }}</strong></div>
                    <div><span>已报工</span><strong>{{ replanExplanation.protectionSummary?.feedbackCount || 0 }}</strong></div>
                    <div><span>生产中</span><strong>{{ replanExplanation.protectionSummary?.inProgressCount || 0 }}</strong></div>
                    <div><span>已完成</span><strong>{{ replanExplanation.protectionSummary?.finishedCount || 0 }}</strong></div>
                    <div><span>已锁定</span><strong>{{ replanExplanation.protectionSummary?.lockedCount || 0 }}</strong></div>
                    <div><span>人工安排</span><strong>{{ replanExplanation.protectionSummary?.manualCount || 0 }}</strong></div>
                  </div>
                  <UnifiedListTemplate
                    table-key="mes.pro.schedulerWorkbench.replan.protectedTasks"
                    :query-model="standardListQueryModel"
                    :filter-definitions="standardListFilterDefinitions"
                    :quick-filter-state="standardListQuickFilterState"
                    :operator-options="standardListOperatorOptions"
                    :columns="protectedTaskColumns"
                    :show-query-form="false"
                    :show-column-settings="false"
                    :total="0"
                    :page="1"
                    :limit="20"
                  >
                    <template #table>
                      <el-table
                        :data="replanExplanation.protectedTasks || []"
                        data-user-table-column-explicit
                        data-user-table-key="mes.pro.schedulerWorkbench.replan.protectedTasks"
                        border
                        size="small"
                        empty-text="暂无受保护任务"
                        @header-dragend="handleProtectedTaskHeaderDragend"
                      >
                        <el-table-column label="任务" prop="taskCode" :width="getProtectedTaskColumnWidthString('taskCode', 150)" :min-width="getProtectedTaskColumnMinWidthString('taskCode', 130)" />
                        <el-table-column label="工单" prop="workOrderCode" :width="getProtectedTaskColumnWidthString('workOrderCode', 150)" :min-width="getProtectedTaskColumnMinWidthString('workOrderCode', 130)" />
                        <el-table-column label="工序" prop="processName" :width="getProtectedTaskColumnWidthString('processName', 130)" :min-width="getProtectedTaskColumnMinWidthString('processName', 110)" />
                        <el-table-column label="工作站" prop="workstationName" :width="getProtectedTaskColumnWidthString('workstationName', 140)" :min-width="getProtectedTaskColumnMinWidthString('workstationName', 120)" />
                        <el-table-column label="保护原因" :width="getProtectedTaskColumnWidthString('protectionReason', 104)" :min-width="getProtectedTaskColumnMinWidthString('protectionReason', 96)">
                          <template #default="{ row }">{{ formatProtectionReason(row.protectionReason) }}</template>
                        </el-table-column>
                      </el-table>
                    </template>
                  </UnifiedListTemplate>
                </section>

                <section class="scheduler-workbench__algorithm-section">
                  <div class="scheduler-workbench__algorithm-section-head">
                    <span class="scheduler-workbench__algorithm-step-no">6</span>
                    <div>
                      <strong>物料需求与其他问题</strong>
                      <small>需要多少、现在有多少、还缺多少。</small>
                    </div>
                  </div>
                  <UnifiedListTemplate
                    table-key="mes.pro.schedulerWorkbench.replan.materials"
                    :query-model="standardListQueryModel"
                    :filter-definitions="standardListFilterDefinitions"
                    :quick-filter-state="standardListQuickFilterState"
                    :operator-options="standardListOperatorOptions"
                    :columns="materialColumns"
                    :show-query-form="false"
                    :show-column-settings="false"
                    :total="0"
                    :page="1"
                    :limit="20"
                  >
                    <template #table>
                      <el-table
                        :data="replanExplanation.materials || []"
                        data-user-table-column-explicit
                        data-user-table-key="mes.pro.schedulerWorkbench.replan.materials"
                        border
                        size="small"
                        row-key="materialId"
                        empty-text="暂无物料需求"
                        @header-dragend="handleMaterialHeaderDragend"
                      >
                        <el-table-column type="expand" :width="getMaterialColumnWidthString('expand', 48)">
                          <template #default="{ row }">
                            <UnifiedListTemplate
                              table-key="mes.pro.schedulerWorkbench.replan.materialContributions"
                              :query-model="standardListQueryModel"
                              :filter-definitions="standardListFilterDefinitions"
                              :quick-filter-state="standardListQuickFilterState"
                              :operator-options="standardListOperatorOptions"
                              :columns="materialContributionColumns"
                              :show-query-form="false"
                              :show-column-settings="false"
                              :total="0"
                              :page="1"
                              :limit="20"
                            >
                              <template #table>
                                <el-table
                                  :data="row.orderContributions || []"
                                  data-user-table-column-explicit
                                  data-user-table-key="mes.pro.schedulerWorkbench.replan.materialContributions"
                                  size="small"
                                  empty-text="暂无订单需求拆分"
                                  @header-dragend="handleMaterialContributionHeaderDragend"
                                >
                                  <el-table-column label="排产订单" prop="scheduleOrderCode" :width="getMaterialContributionColumnWidthString('scheduleOrderCode', 160)" :min-width="getMaterialContributionColumnMinWidthString('scheduleOrderCode', 140)" />
                                  <el-table-column label="生产工单" prop="workOrderCode" :width="getMaterialContributionColumnWidthString('workOrderCode', 160)" :min-width="getMaterialContributionColumnMinWidthString('workOrderCode', 140)" />
                                  <el-table-column label="该订单需要" prop="requiredQty" :width="getMaterialContributionColumnWidthString('requiredQty', 140)" :min-width="getMaterialContributionColumnMinWidthString('requiredQty', 120)" align="right">
                                    <template #default="{ row: contribution }">
                                      {{ formatExplanationNumber(contribution.requiredQty) }}
                                    </template>
                                  </el-table-column>
                                </el-table>
                              </template>
                            </UnifiedListTemplate>
                          </template>
                        </el-table-column>
                        <el-table-column label="物料编码" prop="materialCode" :width="getMaterialColumnWidthString('materialCode', 150)" :min-width="getMaterialColumnMinWidthString('materialCode', 130)" />
                        <el-table-column label="物料名称" prop="materialName" :width="getMaterialColumnWidthString('materialName', 180)" :min-width="getMaterialColumnMinWidthString('materialName', 160)" />
                        <el-table-column label="需要数量" prop="requiredQty" :width="getMaterialColumnWidthString('requiredQty', 120)" :min-width="getMaterialColumnMinWidthString('requiredQty', 108)" align="right">
                          <template #default="{ row }">{{ formatExplanationNumber(row.requiredQty) }}</template>
                        </el-table-column>
                        <el-table-column label="可用数量" prop="availableQty" :width="getMaterialColumnWidthString('availableQty', 120)" :min-width="getMaterialColumnMinWidthString('availableQty', 108)" align="right">
                          <template #default="{ row }">{{ formatExplanationNumber(row.availableQty) }}</template>
                        </el-table-column>
                        <el-table-column label="缺少数量" prop="shortageQty" :width="getMaterialColumnWidthString('shortageQty', 120)" :min-width="getMaterialColumnMinWidthString('shortageQty', 108)" align="right">
                          <template #default="{ row }">
                            <strong :class="{ 'is-danger': Number(row.shortageQty || 0) > 0 }">
                              {{ formatExplanationNumber(row.shortageQty) }}
                            </strong>
                          </template>
                        </el-table-column>
                      </el-table>
                    </template>
                  </UnifiedListTemplate>
                  <UnifiedListTemplate
                    class="scheduler-workbench__algorithm-issues"
                    table-key="mes.pro.schedulerWorkbench.replan.issues"
                    :query-model="standardListQueryModel"
                    :filter-definitions="standardListFilterDefinitions"
                    :quick-filter-state="standardListQuickFilterState"
                    :operator-options="standardListOperatorOptions"
                    :columns="issueColumns"
                    :show-query-form="false"
                    :show-column-settings="false"
                    :total="0"
                    :page="1"
                    :limit="20"
                  >
                    <template #table>
                      <el-table
                        :data="nonMaterialReplanIssues"
                        data-user-table-column-explicit
                        data-user-table-key="mes.pro.schedulerWorkbench.replan.issues"
                        border
                        size="small"
                        empty-text="暂无其他问题"
                        @header-dragend="handleIssueHeaderDragend"
                      >
                        <el-table-column label="级别" :width="getIssueColumnWidthString('severity', 86)" :min-width="getIssueColumnMinWidthString('severity', 76)">
                          <template #default="{ row }">
                            <el-tag size="small" :type="row.severity === 'BLOCKING' ? 'danger' : 'warning'">
                              {{ row.severity === 'BLOCKING' ? '阻断' : '提醒' }}
                            </el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column label="工单" prop="workOrderCode" :width="getIssueColumnWidthString('workOrderCode', 140)" :min-width="getIssueColumnMinWidthString('workOrderCode', 120)" />
                        <el-table-column label="工序" prop="processName" :width="getIssueColumnWidthString('processName', 130)" :min-width="getIssueColumnMinWidthString('processName', 110)" />
                        <el-table-column label="问题" prop="message" :min-width="getIssueColumnMinWidthString('message', 260)" show-overflow-tooltip />
                      </el-table>
                    </template>
                  </UnifiedListTemplate>
                </section>

                <section class="scheduler-workbench__algorithm-section">
                  <div class="scheduler-workbench__algorithm-section-head">
                    <span class="scheduler-workbench__algorithm-step-no">7</span>
                    <div>
                      <strong>生成结果</strong>
                      <small>成功应用后，才成为正式排产任务。</small>
                    </div>
                  </div>
                  <div class="scheduler-workbench__algorithm-result">
                    <div><span>新增任务</span><strong>{{ replanExplanation.summary?.generatedTaskCount || 0 }}</strong></div>
                    <div><span>删除旧任务</span><strong>{{ replanExplanation.summary?.deletedTaskCount || 0 }}</strong></div>
                    <div><span>保留任务</span><strong>{{ replanExplanation.summary?.preservedTaskCount || 0 }}</strong></div>
                    <div>
                      <span>排产时间</span>
                      <strong>
                        {{ formatExplanationDateTime(replanExplanation.summary?.startTime) }} 至
                        {{ formatExplanationDateTime(replanExplanation.summary?.endTime) }}
                      </strong>
                    </div>
                  </div>
                </section>
              </template>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import download from '@/utils/download'
import { formatDateTimeValue } from '@/utils/formatTime'
import {
  SchedulerWorkbenchApi,
  type SchedulerWorkbenchFullConfigImportRespVO,
  type SchedulerWorkbenchPolicySettingsVO,
  type SchedulerWorkbenchRouteActiveOrderVO,
  type SchedulerWorkbenchShiftHoursVO,
  type SchedulerWorkbenchSummaryVO
} from '@/api/mes/pro/schedulerWorkbench'
import {
  ProScheduleCalendarApi,
  type ProScheduleCalendarRulesRespVO,
  type ProScheduleCalendarRulesUpdateReqVO
} from '@/api/mes/pro/scheduleCalendar'
import {
  MesProScheduleOrderApi,
  type MesProScheduleOrderProcessWipVO
} from '@/api/mes/pro/scheduleorder'
import {
  ProTaskAutoScheduleApi,
  type ProTaskAutoScheduleIssueVO,
  type ProTaskReplanExplanationRespVO
} from '@/api/mes/pro/task/autoSchedule'
import { checkPermi } from '@/utils/permission'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import ProcessWipTable from './components/ProcessWipTable.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesProSchedulerWorkbench' })

type SchedulerWorkbenchMetricSummary = Pick<
  SchedulerWorkbenchSummaryVO,
  | 'date'
  | 'pendingScheduleOrderCount'
  | 'todayScheduledTaskCount'
  | 'todayPlannedCapacity'
  | 'todayFeedbackCount'
  | 'todayFeedbackQuantity'
  | 'pendingApprovalFeedbackCount'
  | 'repairingMachineryCount'
  | 'resourceUnconfiguredCount'
  | 'blockingIssueCount'
  | 'materialShortageCount'
  | 'bottlenecks'
  | 'routeActiveOrders'
  | 'currentScheduleScopeText'
>

const loading = ref(false)
const processWipLoading = ref(false)
const processWipErrorMessage = ref('')
const shiftHoursSaving = ref(false)
const scheduleRulesLoading = ref(false)
const scheduleRulesSaving = ref(false)
const policySettingsSaving = ref(false)
const fullConfigExporting = ref(false)
const fullConfigImporting = ref(false)
const schedulerSettingsLoading = ref(false)
const schedulerSettingsLoaded = ref(false)
const schedulerSettingsDialogVisible = ref(false)
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const router = useRouter()
const shiftHoursFormRef = ref()
const policySettingsFormRef = ref()
const fullConfigInputRef = ref<HTMLInputElement>()
const processWipStatistics = ref<MesProScheduleOrderProcessWipVO[]>([])
const processWipPlannedStartDateDrafts = reactive<Record<string, string | undefined>>({})
const processWipSettingsSavingId = ref<string>()
const activeWipTab = ref('process-list')
const replanExplanationLoading = ref(false)
const replanExplanationError = ref('')
const replanExplanation = ref<ProTaskReplanExplanationRespVO | null>(null)
let replanExplanationRequest: Promise<void> | null = null
let schedulerWorkbenchRequestSerial = 0
let schedulerWorkbenchSecondaryFrameId: number | undefined

const isStaleSchedulerWorkbenchRequest = (requestSerial: number) =>
  requestSerial !== schedulerWorkbenchRequestSerial

const cancelDeferredSchedulerWorkbenchSecondaryLoad = () => {
  if (schedulerWorkbenchSecondaryFrameId === undefined) return
  cancelAnimationFrame(schedulerWorkbenchSecondaryFrameId)
  schedulerWorkbenchSecondaryFrameId = undefined
}

const resolveSchedulerWorkbenchErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (typeof error === 'string' && error.trim()) {
    return error.trim()
  }
  return defaultMessage
}

const nonMaterialReplanIssues = computed<ProTaskAutoScheduleIssueVO[]>(() =>
  (replanExplanation.value?.issues || []).filter(
    (issue) => !['MATERIAL', 'MATERIAL_DEMAND'].includes(issue.issueType)
  )
)
const schedulerWorkbenchProcessWipDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'routeCode', label: '工艺路线编码', width: 150, minWidth: 136 },
  { key: 'routeName', label: '工艺路线名称', width: 180, minWidth: 160 },
  { key: 'processCode', label: '工序编号', width: 130, minWidth: 120 },
  { key: 'processName', label: '工序名称', width: 160, minWidth: 140 },
  { key: 'wipOrderCount', label: '在制单数', width: 148, minWidth: 132 },
  { key: 'shiftCapacityTotal', label: '班次产能', width: 116, minWidth: 104 },
  { key: 'shiftStatus', label: '班次状态', width: 104, minWidth: 96 },
  { key: 'nightShiftEnabled', label: '夜班', width: 116, minWidth: 108 },
  { key: 'plannedStartDate', label: '开排日期', width: 164, minWidth: 150 },
  { key: 'unfinishedDemandQuantity', label: '未完需求', width: 140, minWidth: 128 },
  { key: 'estimatedStartTime', label: '预计开始', width: 168, minWidth: 156 },
  { key: 'estimatedCompletionTime', label: '预计完工', width: 168, minWidth: 156 },
  { key: 'todayFeedbackQuantity', label: '今日报工', width: 148, minWidth: 132 }
]
const {
  columns: schedulerWorkbenchProcessWipColumns,
  saving: processWipColumnSaving,
  isColumnVisible: isProcessWipColumnVisible,
  getColumnWidthString: getProcessWipColumnWidthString,
  getColumnMinWidthString: getProcessWipColumnMinWidthString,
  handleHeaderDragend: handleProcessWipHeaderDragend,
  saveConfig: saveProcessWipColumnConfig,
  resetConfig: resetProcessWipColumnConfig
} = useUserTableColumns(
  'mes.pro.schedulerWorkbench.processWip',
  schedulerWorkbenchProcessWipDefaultColumns
)
const processWipFlexibleColumnKey = computed(() => {
  const preferredKeys = ['processName', 'estimatedCompletionTime', 'todayFeedbackQuantity']
  const visibleKeys = schedulerWorkbenchProcessWipColumns.value
    .filter((column) => column.visible)
    .map((column) => column.key)
  return preferredKeys.find((key) => visibleKeys.includes(key)) || visibleKeys.at(-1)
})
const getProcessWipColumnLayoutWidthString = (key: string, fallback?: number) => {
  if (key === processWipFlexibleColumnKey.value) {
    return undefined
  }
  return getProcessWipColumnWidthString(key, fallback)
}
const standardListQueryModel = {}
const standardListFilterDefinitions: TableQuickFilterDefinition[] = []
const standardListQuickFilterState = {}
const standardListOperatorOptions = []

const schedulerWorkbenchRouteActiveOrderDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'routeName', label: '工艺路线', width: 220, minWidth: 180 },
  { key: 'wipOrderCount', label: '在制订单', width: 116, minWidth: 104 },
  { key: 'routeCode', label: '路线编码', width: 160, minWidth: 136 },
  { key: 'products', label: '关联产品', minWidth: 280 }
]
const {
  columns: routeActiveOrderColumns,
  getColumnWidthString: getRouteActiveOrderColumnWidthString,
  getColumnMinWidthString: getRouteActiveOrderColumnMinWidthString,
  handleHeaderDragend: handleRouteActiveOrderHeaderDragend
} = useUserTableColumns(
  'mes.pro.schedulerWorkbench.routeActiveOrders',
  schedulerWorkbenchRouteActiveOrderDefaultColumns
)

const schedulerWorkbenchReplanOrderDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'rank', label: '顺序', width: 68, minWidth: 60 },
  { key: 'scheduleOrderCode', label: '排产订单', width: 150, minWidth: 132 },
  { key: 'workOrderCode', label: '生产工单', width: 150, minWidth: 132 },
  { key: 'product', label: '产品', width: 190, minWidth: 170 },
  { key: 'quantity', label: '数量', width: 110, minWidth: 96 },
  { key: 'promiseDate', label: '交期', width: 112, minWidth: 100 },
  { key: 'priorityNo', label: '优先级', width: 88, minWidth: 78 },
  { key: 'processCount', label: '工序数', width: 88, minWidth: 78 }
]
const {
  columns: replanOrderColumns,
  getColumnWidthString: getReplanOrderColumnWidthString,
  getColumnMinWidthString: getReplanOrderColumnMinWidthString,
  handleHeaderDragend: handleReplanOrderHeaderDragend
} = useUserTableColumns(
  'mes.pro.schedulerWorkbench.replan.orders',
  schedulerWorkbenchReplanOrderDefaultColumns
)

const schedulerWorkbenchReplanWorkOrderDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'workOrderCode', label: '生产工单', width: 160, minWidth: 140 },
  { key: 'route', label: '工艺路线', width: 180, minWidth: 160 },
  { key: 'processCount', label: '工序数', width: 88, minWidth: 78 },
  { key: 'processSequence', label: '工序顺序', minWidth: 280 }
]
const {
  columns: replanWorkOrderColumns,
  getColumnWidthString: getReplanWorkOrderColumnWidthString,
  getColumnMinWidthString: getReplanWorkOrderColumnMinWidthString,
  handleHeaderDragend: handleReplanWorkOrderHeaderDragend
} = useUserTableColumns(
  'mes.pro.schedulerWorkbench.replan.workOrders',
  schedulerWorkbenchReplanWorkOrderDefaultColumns
)

const schedulerWorkbenchReplanProcessDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'processSort', label: '序号', width: 68, minWidth: 60 },
  { key: 'processName', label: '工序', width: 140, minWidth: 120 },
  { key: 'scheduledQuantity', label: '数量', width: 96, minWidth: 86 },
  { key: 'shiftNames', label: '班次', width: 120, minWidth: 110 },
  { key: 'workstationNames', label: '工作站', width: 170, minWidth: 150 },
  { key: 'machineCount', label: '设备', width: 76, minWidth: 68 },
  { key: 'workers', label: '人员', width: 112, minWidth: 96 },
  { key: 'effectiveHourlyCapacity', label: '班次产能', width: 112, minWidth: 100 },
  { key: 'plannedDurationMinutes', label: '预计时长', width: 104, minWidth: 96 },
  { key: 'time', label: '时间', minWidth: 220 },
  { key: 'result', label: '结果', width: 82, minWidth: 72 }
]
const {
  columns: replanProcessColumns,
  getColumnWidthString: getReplanProcessColumnWidthString,
  getColumnMinWidthString: getReplanProcessColumnMinWidthString,
  handleHeaderDragend: handleReplanProcessHeaderDragend
} = useUserTableColumns(
  'mes.pro.schedulerWorkbench.replan.processes',
  schedulerWorkbenchReplanProcessDefaultColumns
)

const schedulerWorkbenchProtectedTaskDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'taskCode', label: '任务', width: 150, minWidth: 130 },
  { key: 'workOrderCode', label: '工单', width: 150, minWidth: 130 },
  { key: 'processName', label: '工序', width: 130, minWidth: 110 },
  { key: 'workstationName', label: '工作站', width: 140, minWidth: 120 },
  { key: 'protectionReason', label: '保护原因', width: 104, minWidth: 96 }
]
const {
  columns: protectedTaskColumns,
  getColumnWidthString: getProtectedTaskColumnWidthString,
  getColumnMinWidthString: getProtectedTaskColumnMinWidthString,
  handleHeaderDragend: handleProtectedTaskHeaderDragend
} = useUserTableColumns(
  'mes.pro.schedulerWorkbench.replan.protectedTasks',
  schedulerWorkbenchProtectedTaskDefaultColumns
)

const schedulerWorkbenchMaterialDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'expand', label: '展开', width: 48, minWidth: 44, hideable: false, business: false },
  { key: 'materialCode', label: '物料编码', width: 150, minWidth: 130 },
  { key: 'materialName', label: '物料名称', width: 180, minWidth: 160 },
  { key: 'requiredQty', label: '需要数量', width: 120, minWidth: 108 },
  { key: 'availableQty', label: '可用数量', width: 120, minWidth: 108 },
  { key: 'shortageQty', label: '缺少数量', width: 120, minWidth: 108 }
]
const {
  columns: materialColumns,
  getColumnWidthString: getMaterialColumnWidthString,
  getColumnMinWidthString: getMaterialColumnMinWidthString,
  handleHeaderDragend: handleMaterialHeaderDragend
} = useUserTableColumns(
  'mes.pro.schedulerWorkbench.replan.materials',
  schedulerWorkbenchMaterialDefaultColumns
)

const schedulerWorkbenchMaterialContributionDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'scheduleOrderCode', label: '排产订单', width: 160, minWidth: 140 },
  { key: 'workOrderCode', label: '生产工单', width: 160, minWidth: 140 },
  { key: 'requiredQty', label: '该订单需要', width: 140, minWidth: 120 }
]
const {
  columns: materialContributionColumns,
  getColumnWidthString: getMaterialContributionColumnWidthString,
  getColumnMinWidthString: getMaterialContributionColumnMinWidthString,
  handleHeaderDragend: handleMaterialContributionHeaderDragend
} = useUserTableColumns(
  'mes.pro.schedulerWorkbench.replan.materialContributions',
  schedulerWorkbenchMaterialContributionDefaultColumns
)

const schedulerWorkbenchIssueDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'severity', label: '级别', width: 86, minWidth: 76 },
  { key: 'workOrderCode', label: '工单', width: 140, minWidth: 120 },
  { key: 'processName', label: '工序', width: 130, minWidth: 110 },
  { key: 'message', label: '问题', minWidth: 260 }
]
const {
  columns: issueColumns,
  getColumnWidthString: getIssueColumnWidthString,
  getColumnMinWidthString: getIssueColumnMinWidthString,
  handleHeaderDragend: handleIssueHeaderDragend
} = useUserTableColumns(
  'mes.pro.schedulerWorkbench.replan.issues',
  schedulerWorkbenchIssueDefaultColumns
)

const processWipQuickFilterParams = reactive<{
  pageNo: number
  pageSize: number
  quickFilter?: TableQuickFilterValue
}>({
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined
})
const processWipSortState = ref<{
  key?: string
  prop?: string
  order?: 'ascending' | 'descending' | null
}>({})
const schedulerWorkbenchProcessWipQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'routeCode', label: '工艺路线编码', type: 'text', placeholder: '请输入工艺路线编码' },
  { key: 'routeName', label: '工艺路线名称', type: 'text', placeholder: '请输入工艺路线名称' },
  { key: 'processCode', label: '工序编号', type: 'text', placeholder: '请输入工序编号' },
  { key: 'processName', label: '工序名称', type: 'text', placeholder: '请输入工序名称' },
  {
    key: 'shiftStatus',
    label: '班次状态',
    type: 'select',
    options: [
      { label: '白班', value: '白班' },
      { label: '白夜班', value: '白夜班' },
      { label: '夜班', value: '夜班' }
    ]
  },
  {
    key: 'nightShiftEnabled',
    label: '夜班',
    type: 'select',
    options: [
      { label: '开启', value: 'true' },
      { label: '关闭', value: 'false' }
    ]
  },
  { key: 'plannedStartDate', label: '开排日期', type: 'dateRange' },
  { key: 'estimatedStartTime', label: '预计开始', type: 'dateRange' },
  { key: 'estimatedCompletionTime', label: '预计完工', type: 'dateRange' }
]
const shiftHoursForm = reactive({
  shiftHours: undefined as number | undefined
})
const shiftHoursSetting = ref<SchedulerWorkbenchShiftHoursVO>({
  workstationCount: 0,
  configuredWorkstationCount: 0,
  missingWorkstationCount: 0,
  distinctShiftHoursCount: 0,
  updatedWorkstationCount: 0
})
const shiftHoursRules = {
  shiftHours: [{ required: true, message: '班次小时不能为空', trigger: 'blur' }]
}
const defaultScheduleRules = (): ProScheduleCalendarRulesRespVO => ({
  skipStatutoryHolidays: false,
  weekendRestMode: 'DOUBLE',
  dateShiftModeByDate: {},
  simulationCurrentDate: ''
})
const scheduleRulesForm = reactive(defaultScheduleRules())
const scheduleRulesSnapshot = ref('')
const policySettingsForm = reactive<SchedulerWorkbenchPolicySettingsVO>({
  erpWorkOrderSyncTime: '02:00',
  nightlyReplanTime: '02:00',
  priorityRule: 'PROMISE_DATE',
  protectReportedTasks: true,
  protectCompletedTasks: true,
  protectLockedTasks: true,
  defaultScheduleUseEnabled: true,
  defaultScheduleCapacityMode: 'RESOURCE_CALCULATED',
  defaultFiniteHourlyCapacity: undefined,
  defaultInfiniteDurationQuantityFactorHours: undefined,
  defaultInfiniteDurationBaseHours: undefined,
  defaultNightShiftEnabled: false,
  defaultWorkerQuantity: 5,
  defaultWorkerSingleHourlyCapacity: 30
})
const timeRule = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(value || '')) {
    callback(new Error('时间必须为 HH:mm'))
    return
  }
  callback()
}
const positiveIntegerRule = (label: string) => {
  return (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
    if (value === undefined || value === null || !Number.isInteger(Number(value)) || Number(value) <= 0) {
      callback(new Error(`${label}必须是大于 0 的整数`))
      return
    }
    callback()
  }
}
const positiveNumberRule = (label: string, min = 0, inclusive = false) => {
  return (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
    if (value === undefined || value === null || Number.isNaN(Number(value))) {
      callback(new Error(`${label}不能为空`))
      return
    }
    if ((inclusive && Number(value) < min) || (!inclusive && Number(value) <= min)) {
      callback(new Error(`${label}${inclusive ? '不能小于' : '必须大于'} ${min}`))
      return
    }
    callback()
  }
}
const policySettingsRules = {
  erpWorkOrderSyncTime: [{ validator: timeRule, trigger: 'blur' }],
  nightlyReplanTime: [{ validator: timeRule, trigger: 'blur' }],
  priorityRule: [{ required: true, message: '排产优先级规则不能为空', trigger: 'change' }],
  defaultScheduleCapacityMode: [{ required: true, message: '默认产能模式不能为空', trigger: 'change' }],
  defaultFiniteHourlyCapacity: [{ validator: positiveNumberRule('产能覆盖(产能/h)'), trigger: 'blur' }],
  defaultInfiniteDurationQuantityFactorHours: [
    { validator: positiveNumberRule('默认系数 a(h/件)'), trigger: 'blur' }
  ],
  defaultInfiniteDurationBaseHours: [
    { validator: positiveNumberRule('默认固定值 b(h)', 0, true), trigger: 'blur' }
  ],
  defaultWorkerQuantity: [{ validator: positiveIntegerRule('人数'), trigger: 'blur' }],
  defaultWorkerSingleHourlyCapacity: [
    { validator: positiveNumberRule('人效h'), trigger: 'blur' }
  ]
}
const summary = ref<SchedulerWorkbenchMetricSummary>({
  date: selectedDate.value,
  pendingScheduleOrderCount: 0,
  todayScheduledTaskCount: 0,
  todayPlannedCapacity: 0,
  todayFeedbackCount: 0,
  todayFeedbackQuantity: 0,
  pendingApprovalFeedbackCount: 0,
  repairingMachineryCount: 0,
  resourceUnconfiguredCount: 0,
  blockingIssueCount: 0,
  materialShortageCount: 0,
  bottlenecks: [],
  routeActiveOrders: [],
  currentScheduleScopeText: ''
})
const canUpdateSettings = computed(() => checkPermi(['mes:pro-scheduler-workbench:update']))
const scheduleRulesDirty = computed(
  () => JSON.stringify(buildScheduleRulesPayload()) !== scheduleRulesSnapshot.value
)

const shiftHoursSettingText = computed(() => {
  const setting = shiftHoursSetting.value
  if (setting.distinctShiftHoursCount > 1) {
    return `未统一 ${setting.configuredWorkstationCount}/${setting.workstationCount}`
  }
  if (setting.shiftHours) {
    return `当前${formatNumber(setting.shiftHours)}h`
  }
  return '未配置'
})

const loadSummary = async (requestSerial?: number) => {
  loading.value = true
  try {
    const nextSummary = await SchedulerWorkbenchApi.getSummary(selectedDate.value)
    if (requestSerial !== undefined && isStaleSchedulerWorkbenchRequest(requestSerial)) return
    summary.value = nextSummary
  } finally {
    if (requestSerial === undefined || !isStaleSchedulerWorkbenchRequest(requestSerial)) {
      loading.value = false
    }
  }
}

const syncProcessWipPlannedStartDateDrafts = (rows: MesProScheduleOrderProcessWipVO[]) => {
  const activeRowKeys = new Set<string>()
  rows.forEach((row) => {
    const rowKey = getProcessWipRowKey(row)
    activeRowKeys.add(rowKey)
    processWipPlannedStartDateDrafts[rowKey] = row.plannedStartDate || undefined
  })
  Object.keys(processWipPlannedStartDateDrafts).forEach((rowKey) => {
    if (!activeRowKeys.has(rowKey)) {
      delete processWipPlannedStartDateDrafts[rowKey]
    }
  })
}

const getProcessWipRowKey = (row: MesProScheduleOrderProcessWipVO) => {
  if (row.routeVersionId == null || row.routeProcessId == null) {
    throw new Error(`工序在制数据缺少路线工序标识，processId=${row.processId ?? '未知'}`)
  }
  return `${row.routeVersionId}:${row.routeProcessId}`
}

const loadProcessWipStatistics = async (requestSerial?: number) => {
  processWipLoading.value = true
  processWipErrorMessage.value = ''
  try {
    const rows = await MesProScheduleOrderApi.getProcessWipStatistics()
    if (requestSerial !== undefined && isStaleSchedulerWorkbenchRequest(requestSerial)) return
    rows.forEach(getProcessWipRowKey)
    if (requestSerial !== undefined && isStaleSchedulerWorkbenchRequest(requestSerial)) return
    syncProcessWipPlannedStartDateDrafts(rows)
    processWipStatistics.value = rows
  } catch (error) {
    if (requestSerial !== undefined && isStaleSchedulerWorkbenchRequest(requestSerial)) return
    const message = resolveSchedulerWorkbenchErrorMessage(
      error,
      '工序在制数据缺少路线工序标识'
    )
    processWipErrorMessage.value = message
    ElMessage.error(message)
    throw error
  } finally {
    if (requestSerial === undefined || !isStaleSchedulerWorkbenchRequest(requestSerial)) {
      processWipLoading.value = false
    }
  }
}

const normalizeQuickFilterText = (value: unknown) => String(value ?? '').trim().toLowerCase()

const isProcessWipDoubleShift = (row: MesProScheduleOrderProcessWipVO) =>
  Boolean(row.nightShiftEnabled)

const isProcessWipResourceMissing = (row: MesProScheduleOrderProcessWipVO) =>
  row.resourceStatus === 'CAPACITY_MISSING'

const getProcessWipShiftStatusText = (row: MesProScheduleOrderProcessWipVO) => {
  if (isProcessWipDoubleShift(row)) return '白夜班'
  return row.shiftStatus || '白班'
}

const getProcessWipShiftStatusTagType = () => 'success'

const applyProcessWipQuickFilter = async () => {
  processWipQuickFilterParams.pageNo = 1
}

const handleProcessWipPagination = () => {
  processWipQuickFilterParams.pageNo = processWipQuickFilterParams.pageNo || 1
}

const processWipQuickFilter = useTableQuickFilter(
  'mes.pro.schedulerWorkbench.processWip',
  schedulerWorkbenchProcessWipQuickFilterDefinitions,
  processWipQuickFilterParams,
  applyProcessWipQuickFilter
)

const filteredProcessWipStatistics = computed(() => {
  const quickFilter = processWipQuickFilterParams.quickFilter
  if (!quickFilter) return processWipStatistics.value
  return processWipStatistics.value.filter((item) => {
    if (
      quickFilter.fieldKey === 'estimatedStartTime' ||
      quickFilter.fieldKey === 'estimatedCompletionTime' ||
      quickFilter.fieldKey === 'plannedStartDate'
    ) {
      const value =
        quickFilter.fieldKey === 'estimatedStartTime'
          ? item.estimatedStartTime
          : quickFilter.fieldKey === 'estimatedCompletionTime'
            ? item.estimatedCompletionTime
            : item.plannedStartDate
      if (!value || !quickFilter.value || !quickFilter.valueEnd) return false
      const timestamp = dayjs(value).valueOf()
      const start = dayjs(String(quickFilter.value)).startOf('day').valueOf()
      const end = dayjs(String(quickFilter.valueEnd)).endOf('day').valueOf()
      return timestamp >= start && timestamp <= end
    }
    if (quickFilter.fieldKey === 'shiftStatus') {
      const actual = normalizeQuickFilterText(getProcessWipShiftStatusText(item))
      const expected = normalizeQuickFilterText(quickFilter.value)
      if (!expected) return true
      if (quickFilter.operator === 'eq') return actual === expected
      return actual.includes(expected)
    }
    if (quickFilter.fieldKey === 'nightShiftEnabled') {
      const expected = normalizeQuickFilterText(quickFilter.value)
      if (!expected) return true
      return String(Boolean(item.nightShiftEnabled)) === expected
    }
    const actual = normalizeQuickFilterText(
      item[quickFilter.fieldKey as keyof MesProScheduleOrderProcessWipVO]
    )
    const expected = normalizeQuickFilterText(quickFilter.value)
    if (!expected) return true
    if (quickFilter.operator === 'eq') return actual === expected
    return actual.includes(expected)
  })
})

const processWipTotal = computed(() => filteredProcessWipStatistics.value.length)

const processWipSortableKeys = new Set(schedulerWorkbenchProcessWipDefaultColumns.map((column) => column.key))
const processWipNumericSortKeys = new Set([
  'wipOrderCount',
  'shiftCapacityTotal',
  'unfinishedDemandQuantity',
  'todayFeedbackQuantity'
])
const processWipDateSortKeys = new Set([
  'plannedStartDate',
  'estimatedStartTime',
  'estimatedCompletionTime'
])

const normalizeProcessWipSortKey = (value?: string) => {
  if (!value) return undefined
  if (!processWipSortableKeys.has(value)) {
    throw new Error(`未知工序列表排序字段：${value}`)
  }
  return value
}

const getProcessWipSortValue = (row: MesProScheduleOrderProcessWipVO, sortKey: string) => {
  if (sortKey === 'shiftStatus') return getProcessWipShiftStatusText(row)
  if (sortKey === 'nightShiftEnabled') return Number(Boolean(row.nightShiftEnabled))
  if (processWipNumericSortKeys.has(sortKey)) {
    const value = row[sortKey as keyof MesProScheduleOrderProcessWipVO]
    return typeof value === 'number' && Number.isFinite(value) ? value : undefined
  }
  if (processWipDateSortKeys.has(sortKey)) {
    const value = row[sortKey as keyof MesProScheduleOrderProcessWipVO]
    const timestamp = value ? dayjs(String(value)).valueOf() : Number.NaN
    return Number.isFinite(timestamp) ? timestamp : undefined
  }
  return row[sortKey as keyof MesProScheduleOrderProcessWipVO]
}

const compareProcessWipSortValue = (left: unknown, right: unknown) => {
  const leftEmpty = left === undefined || left === null || left === ''
  const rightEmpty = right === undefined || right === null || right === ''
  if (leftEmpty && rightEmpty) return 0
  if (leftEmpty) return 1
  if (rightEmpty) return -1
  if (typeof left === 'number' && typeof right === 'number') {
    return left - right
  }
  return String(left).localeCompare(String(right), 'zh-CN', { numeric: true })
}

const sortedProcessWipStatistics = computed(() => {
  const sortKey = normalizeProcessWipSortKey(
    processWipSortState.value.prop || processWipSortState.value.key
  )
  const sortOrder = processWipSortState.value.order
  if (!sortKey || !sortOrder) return filteredProcessWipStatistics.value
  const direction = sortOrder === 'ascending' ? 1 : -1
  return [...filteredProcessWipStatistics.value].sort((left, right) => {
    return (
      compareProcessWipSortValue(
        getProcessWipSortValue(left, sortKey),
        getProcessWipSortValue(right, sortKey)
      ) * direction
    )
  })
})

const pagedProcessWipStatistics = computed(() => {
  const pageNo = processWipQuickFilterParams.pageNo || 1
  const pageSize = processWipQuickFilterParams.pageSize || 10
  const start = (pageNo - 1) * pageSize
  return sortedProcessWipStatistics.value.slice(start, start + pageSize)
})

const handleProcessWipSortChange = () => {
  processWipQuickFilterParams.pageNo = 1
}

const loadShiftHoursSetting = async () => {
  shiftHoursSetting.value = await SchedulerWorkbenchApi.getShiftHoursSetting()
  shiftHoursForm.shiftHours = shiftHoursSetting.value.shiftHours
}

const loadScheduleRules = async () => {
  scheduleRulesLoading.value = true
  try {
    const rules = await ProScheduleCalendarApi.getRules()
    Object.assign(scheduleRulesForm, defaultScheduleRules(), rules)
    scheduleRulesSnapshot.value = JSON.stringify(buildScheduleRulesPayload())
  } finally {
    scheduleRulesLoading.value = false
  }
}

const loadPolicySettings = async () => {
  Object.assign(policySettingsForm, await SchedulerWorkbenchApi.getPolicySettings())
}

const saveShiftHoursSetting = async () => {
  await shiftHoursFormRef.value.validate()
  if (!shiftHoursForm.shiftHours || shiftHoursForm.shiftHours <= 0) {
    throw new Error('班次小时必须大于 0')
  }
  shiftHoursSaving.value = true
  try {
    shiftHoursSetting.value = await SchedulerWorkbenchApi.saveShiftHoursSetting({
      shiftHours: shiftHoursForm.shiftHours
    })
    shiftHoursForm.shiftHours = shiftHoursSetting.value.shiftHours
    ElMessage.success('班次小时已统一保存')
  } finally {
    shiftHoursSaving.value = false
  }
}

const savePolicySettings = async () => {
  await policySettingsFormRef.value.validate()
  policySettingsSaving.value = true
  try {
    Object.assign(
      policySettingsForm,
      await SchedulerWorkbenchApi.savePolicySettings({ ...policySettingsForm })
    )
    ElMessage.success('排产策略已保存')
  } finally {
    policySettingsSaving.value = false
  }
}

const buildScheduleRulesPayload = (): ProScheduleCalendarRulesUpdateReqVO => ({
  skipStatutoryHolidays: scheduleRulesForm.skipStatutoryHolidays,
  weekendRestMode: scheduleRulesForm.weekendRestMode,
  dateShiftModeByDate: { ...(scheduleRulesForm.dateShiftModeByDate || {}) }
})

const saveScheduleRules = async () => {
  scheduleRulesSaving.value = true
  try {
    await ProScheduleCalendarApi.updateRules(buildScheduleRulesPayload())
    ElMessage.success('排程规则已更新，请重新生成预览后再发布排产')
    await loadScheduleRules()
  } finally {
    scheduleRulesSaving.value = false
  }
}

const unwrapDownloadedBlob = (payload: unknown, actionName: string): Blob => {
  if (payload instanceof Blob) {
    return payload
  }
  if (
    payload &&
    typeof payload === 'object' &&
    'data' in payload &&
    (payload as { data?: unknown }).data instanceof Blob
  ) {
    return (payload as { data: Blob }).data
  }
  if (payload && typeof payload === 'object') {
    return new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
  }
  throw new Error(`${actionName}返回的下载数据不是 Blob`)
}

const exportFullConfigPackage = async () => {
  fullConfigExporting.value = true
  try {
    const data = await SchedulerWorkbenchApi.exportFullConfigPackage()
    download.json(unwrapDownloadedBlob(data, '导出排产员工作台全部数据包'), '排产员工作台全部数据包.json')
    ElMessage.success('排产员工作台全部数据包已导出')
  } finally {
    fullConfigExporting.value = false
  }
}

const openFullConfigImport = () => {
  fullConfigInputRef.value?.click()
}

const handleFullConfigFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  fullConfigImporting.value = true
  try {
    const result: SchedulerWorkbenchFullConfigImportRespVO =
      await SchedulerWorkbenchApi.importFullConfigPackage(formData)
    ElMessage.success(
      `导入完成；用户角色绑定 ${result.userRoleBindingCount} 条；分配角色 ${result.assignedRoleCount} 条；手动重排主数据 ${result.replanMasterDataCount} 条；排产工单数据 ${result.replanScheduleOrderDataCount} 条；运行态数据 ${result.replanRuntimeDataCount} 条`
    )
  } finally {
    fullConfigImporting.value = false
  }
}

let schedulerSettingsRequest: Promise<void> | null = null
const ensureSchedulerSettingsLoaded = async () => {
  if (schedulerSettingsLoaded.value) {
    return
  }
  if (!schedulerSettingsRequest) {
    schedulerSettingsRequest = (async () => {
      schedulerSettingsLoading.value = true
      try {
        await Promise.all([
          loadShiftHoursSetting(),
          loadPolicySettings(),
          loadScheduleRules()
        ])
        schedulerSettingsLoaded.value = true
      } finally {
        schedulerSettingsLoading.value = false
        schedulerSettingsRequest = null
      }
    })()
  }
  await schedulerSettingsRequest
}

const openSchedulerSettingsDialog = async () => {
  schedulerSettingsDialogVisible.value = true
  await ensureSchedulerSettingsLoaded()
}

const openProcessWipOrders = (item: MesProScheduleOrderProcessWipVO) => {
  const processId = Number(item.processId)
  if (
    item.processId !== undefined &&
    item.processId !== null &&
    (!Number.isFinite(processId) || !Number.isInteger(processId))
  ) {
    throw new Error(`工序在制订单缺少有效工序编号: ${item.processId}`)
  }
  router.push({
    name: 'MesProScheduleOrder',
    query: {
      currentProcessId: Number.isFinite(processId) && processId > 0 ? String(processId) : undefined,
      currentProcessName: item.processName || undefined,
      source: 'scheduler-workbench'
    }
  })
}

type ProcessWipCapacitySourceFocus = 'resource' | 'schedule'

const resolveProcessWipCapacitySourceFocus = (
  item: MesProScheduleOrderProcessWipVO
): ProcessWipCapacitySourceFocus => {
  if (item.capacitySource === 'MACHINE') return 'resource'
  if (item.capacitySource === 'WORKER') return 'resource'
  if (item.capacitySource === 'MANUAL_OVERRIDE') return 'schedule'
  if (item.capacitySource === 'INFINITE_FORMULA') return 'schedule'
  if (item.capacityMode === 'MANUAL_OVERRIDE' || item.capacityMode === 'FINITE_HOURLY') {
    return 'schedule'
  }
  if (item.capacityMode === 'INFINITE_FORMULA') return 'schedule'
  throw new Error(`未知班次产能来源：${item.capacitySource || item.capacityMode || '未返回'}`)
}

const getProcessWipCapacitySourceTooltip = (item: MesProScheduleOrderProcessWipVO) => {
  if (item.capacitySource === 'MACHINE') return '查看设备产能来源'
  if (item.capacitySource === 'WORKER') return '查看人工产能来源'
  if (
    item.capacitySource === 'MANUAL_OVERRIDE' ||
    item.capacityMode === 'MANUAL_OVERRIDE' ||
    item.capacityMode === 'FINITE_HOURLY'
  ) {
    return '查看产能覆盖设置'
  }
  if (item.capacitySource === 'INFINITE_FORMULA' || item.capacityMode === 'INFINITE_FORMULA') {
    return '查看公式排产设置'
  }
  return '查看班次产能来源'
}

const buildProcessWipRouteVersionQuery = (item: MesProScheduleOrderProcessWipVO) => {
  if (!item.routeVersionId || !item.routeVersionNo || !item.routeVersionStatus) {
    throw new Error('缺少路线版本上下文，无法定位班次产能来源')
  }
  return {
    routeVersionId: String(item.routeVersionId),
    routeVersionNo: item.routeVersionNo,
    routeVersionStatus: item.routeVersionStatus
  }
}

const openProcessWipCapacitySource = (item: MesProScheduleOrderProcessWipVO) => {
  if (!item.routeId || !item.routeProcessId) {
    ElMessage.error('缺少路线或路线工序标识，无法定位班次产能来源')
    return
  }
  let capacitySourceFocus: ProcessWipCapacitySourceFocus
  let routeVersionQuery: ReturnType<typeof buildProcessWipRouteVersionQuery>
  try {
    capacitySourceFocus = resolveProcessWipCapacitySourceFocus(item)
    routeVersionQuery = buildProcessWipRouteVersionQuery(item)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '无法识别班次产能来源')
    throw error
  }
  router.push({
    name: 'MesProRouteEdit',
    params: { id: item.routeId },
    query: {
      tab: 'flow',
      routeProcessId: String(item.routeProcessId),
      capacitySourceFocus,
      ...routeVersionQuery,
      capacitySource: item.capacitySource || undefined,
      capacityMode: item.capacityMode || undefined,
      source: 'scheduler-workbench-capacity-source'
    }
  })
}

const buildProcessWipSettingsPayload = (
  row: MesProScheduleOrderProcessWipVO,
  overrides: Partial<MesProScheduleOrderProcessWipVO>
) => ({
  routeVersionId: row.routeVersionId,
  routeProcessId: row.routeProcessId,
  nightShiftEnabled:
    overrides.nightShiftEnabled === undefined
      ? Boolean(row.nightShiftEnabled)
      : Boolean(overrides.nightShiftEnabled),
  plannedStartDate:
    overrides.plannedStartDate === undefined
      ? row.plannedStartDate
      : overrides.plannedStartDate || undefined,
  reason: '排产员工作台工序在制列表维护'
})

const saveProcessWipSettings = async (
  row: MesProScheduleOrderProcessWipVO,
  overrides: Partial<MesProScheduleOrderProcessWipVO>
) => {
  processWipSettingsSavingId.value = getProcessWipRowKey(row)
  try {
    await MesProScheduleOrderApi.saveProcessWipSettings(
      buildProcessWipSettingsPayload(row, overrides)
    )
    await loadProcessWipStatistics()
    ElMessage.success('工序在制设置已保存')
  } finally {
    processWipSettingsSavingId.value = undefined
  }
}

const handleProcessWipNightShiftChange = async (
  row: MesProScheduleOrderProcessWipVO,
  nightShiftEnabled: boolean
) => {
  await saveProcessWipSettings(row, { nightShiftEnabled })
}

const handleProcessWipPlannedStartDateChange = async (
  row: MesProScheduleOrderProcessWipVO,
  plannedStartDate: string | undefined
) => {
  await saveProcessWipSettings(row, { plannedStartDate })
}

const routeActiveProductsText = (item: SchedulerWorkbenchRouteActiveOrderVO) => {
  const products = item.products || []
  if (products.length === 0) {
    return '未关联产品'
  }
  return products
    .map((product) => {
      const name = product.productName || product.productCode || '未命名产品'
      return `${name} ${product.wipOrderCount || 0} 单`
    })
    .join('；')
}

const formatNumber = (value?: number) => {
  if (value === undefined || value === null) {
    return '0'
  }
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}

const formatIntegerNumber = (value?: number) => {
  if (value === undefined || value === null) {
    return '0'
  }
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

const formatProcessWipShiftCapacity = (value?: number) => {
  if (value === undefined || value === null) {
    return '0'
  }
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

const formatEstimatedTime = (value?: string | number | Date) => {
  return formatDateTimeValue(value, '无法估算')
}

const formatExplanationNumber = (value?: number) => {
  if (value === undefined || value === null) {
    return '—'
  }
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}

const formatExplanationDateTime = (value?: string | number | Date) => {
  return formatDateTimeValue(value, '—')
}

const formatExplanationDuration = (minutes?: number) => {
  if (minutes === undefined || minutes === null) {
    return '—'
  }
  if (minutes < 60) {
    return `${minutes} 分钟`
  }
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  return remainingMinutes ? `${hours} 小时 ${remainingMinutes} 分` : `${hours} 小时`
}

const formatReplanTrigger = (triggerSource?: string) =>
  triggerSource === 'NIGHTLY' ? '夜间自动重排' : '人工重排'

const formatCapacityMode = (capacityMode?: string) =>
  capacityMode === 'ACTUAL' ? '实际产能' : '计划产能'

const formatProtectionReason = (reason?: string) => {
  const textMap: Record<string, string> = {
    FEEDBACK: '已报工',
    IN_PROGRESS: '生产中',
    FINISHED: '已完成',
    LOCKED: '已锁定',
    MANUAL: '人工安排'
  }
  return reason ? textMap[reason] || reason : '—'
}

const loadLatestReplanExplanation = async () => {
  if (replanExplanationRequest) {
    return replanExplanationRequest
  }
  replanExplanationRequest = (async () => {
    replanExplanationLoading.value = true
    replanExplanationError.value = ''
    replanExplanation.value = null
    try {
      replanExplanation.value = await ProTaskAutoScheduleApi.getLatestReplanExplanation()
    } catch (error) {
      replanExplanationError.value =
        error instanceof Error && error.message ? error.message : '请检查网络或联系管理员。'
    } finally {
      replanExplanationLoading.value = false
      replanExplanationRequest = null
    }
  })()
  return replanExplanationRequest
}

watch(activeWipTab, (tab) => {
  if (tab === 'algorithm-guide') {
    void loadLatestReplanExplanation()
  }
})

const handleReplanExplanationWindowFocus = () => {
  if (activeWipTab.value === 'algorithm-guide') {
    void loadLatestReplanExplanation()
  }
}

const loadSchedulerWorkbenchSecondaryData = async (requestSerial: number) => {
  try {
    await loadProcessWipStatistics(requestSerial)
  } catch (error) {
    if (isStaleSchedulerWorkbenchRequest(requestSerial)) return
    processWipErrorMessage.value = resolveSchedulerWorkbenchErrorMessage(
      error,
      '请检查工序、路线和排产工单数据。'
    )
  }
}

const deferSchedulerWorkbenchSecondaryLoad = (requestSerial: number) => {
  cancelDeferredSchedulerWorkbenchSecondaryLoad()
  processWipLoading.value = true
  processWipErrorMessage.value = ''
  schedulerWorkbenchSecondaryFrameId = requestAnimationFrame(() => {
    schedulerWorkbenchSecondaryFrameId = undefined
    if (isStaleSchedulerWorkbenchRequest(requestSerial)) return
    void loadSchedulerWorkbenchSecondaryData(requestSerial)
  })
}

onMounted(async () => {
  const requestSerial = ++schedulerWorkbenchRequestSerial
  window.addEventListener('focus', handleReplanExplanationWindowFocus)
  await loadSummary(requestSerial)
  if (isStaleSchedulerWorkbenchRequest(requestSerial)) return
  deferSchedulerWorkbenchSecondaryLoad(requestSerial)
})

onBeforeUnmount(() => {
  schedulerWorkbenchRequestSerial += 1
  cancelDeferredSchedulerWorkbenchSecondaryLoad()
  window.removeEventListener('focus', handleReplanExplanationWindowFocus)
})
</script>

<style scoped>
.scheduler-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.scheduler-workbench__panel {
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-bg-color);
  padding: 16px;
}

.scheduler-workbench__panel-head small {
  color: #4b5563;
  font-size: 12px;
  font-weight: 400;
}

.scheduler-workbench__settings-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scheduler-workbench__settings-dialog {
  --scheduler-settings-label-width: 124px;
  --scheduler-settings-control-height: 32px;
  --scheduler-settings-button-min-width: 136px;

  max-height: 70vh;
  overflow-y: auto;
  padding-right: 4px;
}

.scheduler-workbench__settings-block {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 14px;
}

.scheduler-workbench__settings-grid > :first-child {
  border-top: 0;
  padding-top: 0;
}

.scheduler-workbench__settings-block-head {
  display: flex;
  grid-column: 1 / -1;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  color: #263247;
  font-size: 14px;
  font-weight: 600;
}

.scheduler-workbench__settings-block-head small {
  color: #4b5563;
  font-size: 12px;
  font-weight: 400;
}

.scheduler-workbench__shift-form {
  display: grid;
  gap: 0;
}

.scheduler-workbench__settings-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(280px, 1fr));
  align-items: end;
  gap: 12px 18px;
}

.scheduler-workbench__shift-form :deep(.el-form-item),
.scheduler-workbench__schedule-rules-form :deep(.el-form-item),
.scheduler-workbench__policy-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.scheduler-workbench__settings-field {
  display: grid;
  grid-template-columns: var(--scheduler-settings-label-width) minmax(0, 1fr);
  align-items: center;
}

.scheduler-workbench__settings-field :deep(.el-form-item__label) {
  display: inline-flex;
  align-items: center;
  width: var(--scheduler-settings-label-width) !important;
  min-width: var(--scheduler-settings-label-width);
  height: var(--scheduler-settings-control-height);
  flex: 0 0 var(--scheduler-settings-label-width);
  overflow: visible;
  padding-right: 12px;
  color: #263247;
  line-height: var(--scheduler-settings-control-height);
  white-space: nowrap;
}

.scheduler-workbench__settings-field :deep(.el-form-item__content) {
  display: flex;
  min-width: 0;
  align-items: center;
  margin-left: 0 !important;
}

.scheduler-workbench__settings-control {
  width: 100%;
  min-height: var(--scheduler-settings-control-height);
}

.scheduler-workbench__settings-control :deep(.el-input__wrapper),
.scheduler-workbench__settings-control :deep(.el-select__wrapper),
.scheduler-workbench__settings-control :deep(.el-input-number__decrease),
.scheduler-workbench__settings-control :deep(.el-input-number__increase) {
  min-height: var(--scheduler-settings-control-height);
}

.scheduler-workbench__settings-button {
  min-width: var(--scheduler-settings-button-min-width);
  min-height: var(--scheduler-settings-control-height);
  border-radius: 6px;
  font-weight: 600;
}

.scheduler-workbench__input-with-unit {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.scheduler-workbench__shift-unit {
  color: #4b5563;
}

.scheduler-workbench__policy-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(280px, 1fr));
  gap: 12px 18px;
}

.scheduler-workbench__policy-item {
  display: grid;
  grid-template-columns: var(--scheduler-settings-label-width) minmax(0, 1fr);
  align-items: center;
}

.scheduler-workbench__policy-item :deep(.el-form-item__label) {
  display: inline-flex;
  align-items: center;
  width: var(--scheduler-settings-label-width) !important;
  min-width: var(--scheduler-settings-label-width);
  height: var(--scheduler-settings-control-height);
  flex: 0 0 var(--scheduler-settings-label-width);
  overflow: visible;
  padding-right: 12px;
  color: #263247;
  line-height: var(--scheduler-settings-control-height);
  white-space: nowrap;
}

.scheduler-workbench__policy-item :deep(.el-form-item__content) {
  display: flex;
  min-width: 0;
  align-items: center;
  margin-left: 0 !important;
}

.scheduler-workbench__policy-item--priority {
  min-width: 0;
}

.scheduler-workbench__policy-checks,
.scheduler-workbench__settings-actions {
  grid-column: 1 / -1;
}

.scheduler-workbench__policy-checks :deep(.el-form-item__label) {
  line-height: 22px;
  padding-top: 9px;
}

.scheduler-workbench__policy-checks :deep(.el-form-item__content) {
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px 18px;
  padding-top: 5px;
}

.scheduler-workbench__schedule-rules-form {
  display: grid;
  gap: 0;
}

.scheduler-workbench__switch-control {
  display: inline-flex;
  align-items: center;
}

.scheduler-workbench__settings-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.scheduler-workbench__settings-actions--compact {
  grid-column: auto;
  align-self: end;
}

.scheduler-workbench__hidden-input {
  display: none;
}

.scheduler-workbench__side-panels {
  display: grid;
  grid-template-columns: minmax(320px, 0.95fr) minmax(420px, 1.35fr);
  gap: 12px;
  align-items: start;
}

.scheduler-workbench__side-panels > .scheduler-workbench__panel {
  height: 100%;
}

.scheduler-workbench__wip-tabs-panel {
  grid-column: 1 / -1;
}

.scheduler-workbench__wip-tabs :deep(.el-tabs__header) {
  margin-bottom: 10px;
  border-bottom: 1px solid #dbe3ef;
}

.scheduler-workbench__wip-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 0;
}

.scheduler-workbench__wip-tabs :deep(.el-tabs__item) {
  height: 36px;
  color: #4b5563;
  font-size: 14px;
  font-weight: 600;
}

.scheduler-workbench__wip-tabs :deep(.el-tabs__item.is-active) {
  color: #1677ff;
}

.scheduler-workbench__tab-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  color: #263247;
  font-size: 14px;
  font-weight: 600;
}

.scheduler-workbench__tab-head small {
  color: #4b5563;
  font-size: 12px;
  font-weight: 400;
}

.scheduler-workbench__process-wip-table {
  width: 100%;
  font-size: 0.9rem;
}

.scheduler-workbench__process-wip-table :deep(.el-table__inner-wrapper),
.scheduler-workbench__process-wip-table :deep(.el-table__header-wrapper),
.scheduler-workbench__process-wip-table :deep(.el-table__body-wrapper),
.scheduler-workbench__process-wip-table :deep(.el-table__header),
.scheduler-workbench__process-wip-table :deep(.el-table__body) {
  width: 100% !important;
}

.scheduler-workbench__process-wip-table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
  color: #263247;
  font-weight: 600;
}

.scheduler-workbench__process-wip-table :deep(.el-table__row) {
  height: 52px;
  cursor: pointer;
}

.scheduler-workbench__process-wip-table :deep(.el-table__cell) {
  padding: 7px 10px;
  border-bottom-color: #edf1f6;
}

.scheduler-workbench__process-wip-table :deep(.cell) {
  font-variant-numeric: tabular-nums;
}

.scheduler-workbench__shift-capacity {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  width: 100%;
}

.scheduler-workbench__capacity-source-link {
  border: 0;
  background: transparent;
  color: #1677ff;
  cursor: pointer;
  font: inherit;
  font-variant-numeric: tabular-nums;
  line-height: 1.3;
  padding: 0;
}

.scheduler-workbench__capacity-source-link:hover {
  text-decoration: underline;
}

.scheduler-workbench__shift-capacity-multiplier {
  font-weight: 600;
}

.scheduler-workbench__resource-status {
  max-width: 96px;
}

.scheduler-workbench__resource-status :deep(.el-tag__content) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scheduler-workbench__inline-control {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
}

.scheduler-workbench__process-wip-date {
  width: 132px;
}

.scheduler-workbench__table-link {
  color: #1677ff;
  font-weight: 600;
}

.scheduler-workbench__route-active-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.scheduler-workbench__route-active-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fafcff;
  padding: 10px 12px;
}

.scheduler-workbench__route-active-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 12px;
  align-items: center;
}

.scheduler-workbench__route-active-name {
  overflow: hidden;
  color: #263247;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scheduler-workbench__route-active-main strong {
  color: #1677ff;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  text-align: right;
  white-space: nowrap;
}

.scheduler-workbench__route-active-main small,
.scheduler-workbench__route-active-products {
  grid-column: 1 / -1;
  overflow: hidden;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scheduler-workbench__algorithm-guide {
  display: grid;
  gap: 12px;
  min-height: 180px;
}

.scheduler-workbench__algorithm-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  padding: 10px 12px;
}

.scheduler-workbench__algorithm-intro > div:first-child,
.scheduler-workbench__algorithm-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.scheduler-workbench__algorithm-intro strong {
  color: #263247;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.scheduler-workbench__algorithm-intro span,
.scheduler-workbench__algorithm-meta {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.scheduler-workbench__algorithm-meta {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.scheduler-workbench__algorithm-section {
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  padding: 12px;
}

.scheduler-workbench__algorithm-step-no {
  display: inline-flex;
  min-width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #1677ff;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  padding: 0 6px;
}

.scheduler-workbench__algorithm-section-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
}

.scheduler-workbench__algorithm-section-head > div {
  display: grid;
  gap: 2px;
}

.scheduler-workbench__algorithm-section-head strong {
  color: #263247;
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
}

.scheduler-workbench__algorithm-section-head small {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.scheduler-workbench__algorithm-metrics,
.scheduler-workbench__algorithm-result {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 8px;
}

.scheduler-workbench__algorithm-metrics > div,
.scheduler-workbench__algorithm-result > div {
  display: grid;
  gap: 4px;
  min-width: 0;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #fafcff;
  padding: 8px 10px;
}

.scheduler-workbench__algorithm-metrics span,
.scheduler-workbench__algorithm-result span {
  color: #4b5563;
  font-size: 12px;
}

.scheduler-workbench__algorithm-metrics strong,
.scheduler-workbench__algorithm-result strong {
  color: #263247;
  font-size: 16px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.scheduler-workbench__algorithm-result > div:last-child {
  grid-column: span 2;
}

.scheduler-workbench__algorithm-result > div:last-child strong {
  font-size: 13px;
}

.scheduler-workbench__algorithm-collapse {
  border-top: 0;
  border-bottom: 0;
}

.scheduler-workbench__algorithm-collapse-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.scheduler-workbench__algorithm-collapse-title span {
  overflow: hidden;
  color: #4b5563;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scheduler-workbench__algorithm-issues {
  margin-top: 10px;
}

.is-danger {
  color: #c00000 !important;
}

.is-warning {
  color: #d97706 !important;
}

.scheduler-workbench__panel {
  min-width: 0;
}

.scheduler-workbench__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  color: #263247;
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .scheduler-workbench__settings-grid,
  .scheduler-workbench__shift-form,
  .scheduler-workbench__schedule-rules-form {
    align-items: stretch;
    flex-direction: column;
  }

  .scheduler-workbench__policy-form {
    grid-template-columns: 1fr;
  }

  .scheduler-workbench__side-panels {
    grid-template-columns: 1fr;
  }

  .scheduler-workbench__shift-form {
    grid-template-columns: 1fr;
  }

  .scheduler-workbench__algorithm-intro,
  .scheduler-workbench__algorithm-meta {
    grid-template-columns: 1fr;
  }

  .scheduler-workbench__algorithm-intro {
    align-items: flex-start;
    flex-direction: column;
  }

  .scheduler-workbench__algorithm-intro span {
    text-align: left;
  }

  .scheduler-workbench__algorithm-intro > div:first-child,
  .scheduler-workbench__algorithm-meta,
  .scheduler-workbench__algorithm-collapse-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .scheduler-workbench__algorithm-result > div:last-child {
    grid-column: auto;
  }

}
</style>
