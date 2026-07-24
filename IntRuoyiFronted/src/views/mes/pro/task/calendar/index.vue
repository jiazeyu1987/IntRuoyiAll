<template>
  <doc-alert
    title="【生产】生产排程、工序流转卡"
    url="https://doc.iocoder.cn/mes/pro/schedule-card/"
  />

  <div class="schedule-calendar-page">
    <div class="workspace-grid">
      <section class="calendar-column">
        <div class="calendar-toolbar">
          <div class="toolbar-block toolbar-block-left">
            <el-button @click="backToTaskPage">
              <Icon icon="ep:arrow-left" class="mr-5px" /> 返回排产
            </el-button>
            <div class="toolbar-title-group">
              <h2>{{ monthTitle }}</h2>
              <p>模拟日 {{ simulationDateLabel }}</p>
              <p v-if="previewStatusText" class="status-text warning">{{ previewStatusText }}</p>
              <div v-if="autoSchedulePreview" class="preview-context-cue">
                <el-tag :type="previewCalendarContextTagType" effect="light">
                  {{ previewCalendarContextTagText }}
                </el-tag>
                <span>{{ previewCalendarContextLabel }}</span>
                <el-button link type="primary" @click="refreshPreviewMaterialDemand">
                  刷新预览物料
                </el-button>
              </div>
            </div>
          </div>
          <div class="calendar-status-strip">
            <span class="status-chip">
              <label>任务总数</label>
              <strong>{{ monthData.currentScheduleStatus?.totalTaskCount ?? 0 }}</strong>
            </span>
            <span class="status-chip">
              <label>最近更新时间</label>
              <strong>{{ monthData.currentScheduleStatus?.updatedAt || '--' }}</strong>
            </span>
          </div>
          <div class="toolbar-block toolbar-block-right">
            <div class="month-switch">
              <el-button @click="changeMonth(-1)">
                <Icon icon="ep:arrow-left" />
              </el-button>
              <el-button @click="goToCurrentMonth">本月</el-button>
              <el-button @click="changeMonth(1)">
                <Icon icon="ep:arrow-right" />
              </el-button>
            </div>
            <div class="toolbar-stat-strip">
              <span class="toolbar-stat">
                <label>任务</label>
                <strong>{{ activeMonthStats.taskCount }}</strong>
              </span>
              <span class="toolbar-stat">
                <label>工单</label>
                <strong>{{ activeMonthStats.orderCount }}</strong>
              </span>
              <span class="toolbar-stat warning">
                <label>短缺</label>
                <strong>{{ activeMonthStats.shortageCount }}</strong>
              </span>
            </div>
          </div>
        </div>

        <div class="calendar-shell">
          <el-alert
            v-if="monthErrorMessage && !isPreviewCalendarOverlayActive"
            :title="monthErrorMessage"
            type="error"
            :closable="false"
            class="calendar-alert"
          />
          <div
            v-if="shouldShowCalendarRecovery('month') && !isPreviewCalendarOverlayActive"
            class="calendar-recovery-card"
          >
            <span>错误对象：{{ calendarRecoveryState?.objectLabel }}</span>
            <span>影响范围：{{ calendarRecoveryState?.impactScope }}</span>
            <span>恢复入口：{{ calendarRecoveryState?.recoveryHint }}</span>
            <el-button type="primary" link @click="openScheduleRouteRecovery">
              {{ calendarRecoveryState?.recoveryButtonText }}
            </el-button>
          </div>
          <div v-loading="monthLoading">
            <div class="weekday-row">
              <span v-for="item in weekdayLabels" :key="item">{{ item }}</span>
            </div>
            <div class="calendar-grid">
              <div
                v-for="cell in calendarCells"
                :key="cell.date"
                class="calendar-cell"
                :class="buildCalendarCellClass(cell)"
                :data-date="cell.date"
                @click="selectCalendarDate(cell.date)"
                @keydown.enter.prevent="selectCalendarDate(cell.date)"
                @keydown.space.prevent="selectCalendarDate(cell.date)"
                :title="resolveCalendarEditTooltip(cell.date)"
                role="button"
                tabindex="0"
              >
                <div class="calendar-cell-head">
                  <div class="calendar-day-meta">
                    <span class="calendar-day-number">{{ cell.dayNumber }}</span>
                    <span
                      v-if="cell.info?.date === simulationDateLabel"
                      class="calendar-day-badge is-primary"
                    >
                      模拟
                    </span>
                    <span v-else-if="cell.info?.holiday" class="calendar-day-badge is-warning">
                      节假
                    </span>
                    <span v-else-if="isWeekend(cell.date)" class="calendar-day-badge is-muted">
                      周末
                    </span>
                    <span
                      v-if="hasCalendarShiftOverride(cell.date)"
                      class="calendar-day-badge is-override"
                    >
                      已覆盖
                    </span>
                    <span
                      v-else-if="canEditCalendarDate(cell.date)"
                      class="calendar-day-badge is-editable"
                    >
                      可编辑
                    </span>
                  </div>
                  <span class="calendar-mode-text">
                    {{ buildShiftModeLabel(cell.info?.dateShiftMode) }}
                  </span>
                </div>

                <div class="calendar-metric-list">
                  <button
                    type="button"
                    class="calendar-metric-item calendar-metric-button"
                    @click.stop="openCalendarMetricDetail(cell.date, 'tasks')"
                  >
                    <label>任务</label>
                    <strong>{{ cell.info?.totalTaskCount ?? 0 }}</strong>
                  </button>
                  <button
                    type="button"
                    class="calendar-metric-item calendar-metric-button"
                    @click.stop="openCalendarMetricDetail(cell.date, 'orders')"
                  >
                    <label>工单</label>
                    <strong>{{ cell.info?.totalOrderCount ?? 0 }}</strong>
                  </button>
                  <div class="calendar-metric-item calendar-metric-item-readonly">
                    <label>白班</label>
                    <strong>{{ cell.info?.dayShiftTaskCount ?? 0 }}</strong>
                  </div>
                  <div class="calendar-metric-item calendar-metric-item-readonly">
                    <label>夜班</label>
                    <strong>{{ cell.info?.nightShiftTaskCount ?? 0 }}</strong>
                  </div>
                </div>

                <div class="calendar-cell-foot">
                  <button
                    type="button"
                    class="metric-inline warning calendar-metric-button calendar-shortage-button"
                    @click.stop="openCalendarMetricDetail(cell.date, 'shortages')"
                  >
                    短缺 {{ cell.info?.shortageCount ?? 0 }}
                  </button>
                  <button
                    v-if="canToggleCalendarShiftMode(cell.date)"
                    type="button"
                    class="metric-inline calendar-metric-button calendar-shift-toggle-button"
                    @click.stop="toggleCalendarShiftMode(cell.date)"
                  >
                    {{ resolveCalendarShiftToggleLabel(cell.date) }}
                  </button>
                </div>
                <div
                  v-if="calendarShiftEditorDate === cell.date && canEditCalendarDate(cell.date)"
                  class="calendar-shift-editor"
                  @click.stop
                >
                  <div
                    v-for="option in calendarShiftOptions"
                    :key="option.value"
                    role="button"
                    tabindex="0"
                    class="calendar-shift-editor__option"
                    :class="{
                      'is-active': activeSelectedDayDateShiftModeForCell(cell.date) === option.value
                    }"
                    @click.stop="applyCalendarShiftMode(cell.date, option.value)"
                  >
                    {{ option.label }}
                  </div>
                  <div
                    v-if="hasCalendarShiftOverride(cell.date)"
                    role="button"
                    tabindex="0"
                    class="calendar-shift-editor__option is-clear"
                    @click.stop="clearCalendarShiftMode(cell.date)"
                  >
                    恢复默认
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <aside class="sidebar-column">
        <el-tabs v-model="activeSidebarTab" class="sidebar-tabs">
          <el-tab-pane :label="selectedDayTitle" name="detail">
            <section v-loading="detailLoading" class="panel-shell">
              <div class="panel-body">
                <el-alert
                  v-if="detailErrorMessage && !isPreviewCalendarOverlayActive"
                  :title="detailErrorMessage"
                  type="error"
                  :closable="false"
                />
                <div
                  v-if="shouldShowCalendarRecovery('detail') && !isPreviewCalendarOverlayActive"
                  class="calendar-recovery-card"
                >
                  <span>错误对象：{{ calendarRecoveryState?.objectLabel }}</span>
                  <span>影响范围：{{ calendarRecoveryState?.impactScope }}</span>
                  <span>恢复入口：{{ calendarRecoveryState?.recoveryHint }}</span>
                  <el-button type="primary" link @click="openScheduleRouteRecovery">
                    {{ calendarRecoveryState?.recoveryButtonText }}
                  </el-button>
                </div>

                <el-alert
                  v-if="rulesStatusText"
                  :title="rulesStatusText"
                  type="warning"
                  :closable="false"
                  class="mb-12px"
                />
                <div class="action-row">
                  <el-button type="primary" :loading="rulesSaving" @click="saveRules">
                    保存班次规则
                  </el-button>
                  <el-button :loading="capacityGenerateLoading" @click="handleGenerateCapacityPlans">
                    生成产能
                  </el-button>
                  <el-button type="warning" plain @click="openIssueCreateDialog">异常登记</el-button>
                  <el-button
                    type="danger"
                    plain
                    :disabled="selectedDayOpenIssueCount === 0"
                    @click="openIssueResolveDialog"
                  >
                    关闭异常
                  </el-button>
                </div>
                <div v-if="selectedDayTasks[0]" class="preview-context-cue">
                  <el-tag :type="buildTaskExecutionStatusTag(selectedDayTasks[0])" effect="light">
                    {{ buildTaskExecutionStatusText(selectedDayTasks[0]) }}
                  </el-tag>
                  <span>首个任务执行状态</span>
                </div>
                <div class="detail-summary-grid">
                  <div class="detail-total-quantity-card" aria-label="当天当日工序量">
                    <label>当日工序量</label>
                    <strong>{{ buildQuantityLabel(selectedDayDailyProcessQuantity) }}</strong>
                    <span>件</span>
                    <small>总任务量 {{ buildQuantityLabel(selectedDayTotalProcessTaskQuantity) }} 件</small>
                  </div>
                  <button
                    type="button"
                    class="summary-chip summary-chip-button"
                    :class="{ 'is-clickable': isDaySummaryCardClickable('orders') }"
                    :disabled="!isDaySummaryCardClickable('orders')"
                    @click="openDaySummaryDetail('orders')"
                  >
                    <label>工单计划量</label>
                    <strong>{{ buildQuantityLabel(selectedDayWorkOrderPlanQuantity) }}</strong>
                  </button>
                  <button
                    type="button"
                    class="summary-chip summary-chip-button"
                    :class="{ 'is-clickable': isDaySummaryCardClickable('tasks') }"
                    :disabled="!isDaySummaryCardClickable('tasks')"
                    @click="openDaySummaryDetail('tasks')"
                  >
                    <label>任务</label>
                    <strong>{{ selectedDayTasks.length }}</strong>
                  </button>
                  <button
                    type="button"
                    class="summary-chip summary-chip-button"
                    :class="{ 'is-clickable': isDaySummaryCardClickable('orders') }"
                    :disabled="!isDaySummaryCardClickable('orders')"
                    @click="openDaySummaryDetail('orders')"
                  >
                    <label>工单</label>
                    <strong>{{ selectedDayOrderCount }}</strong>
                  </button>
                  <button
                    type="button"
                    class="summary-chip summary-chip-button"
                    :class="{ 'is-clickable': isDaySummaryCardClickable('dayShift') }"
                    :disabled="!isDaySummaryCardClickable('dayShift')"
                    @click="openDaySummaryDetail('dayShift')"
                  >
                    <label>白班</label>
                    <strong>{{ activeSelectedDayShiftTaskCount }}</strong>
                  </button>
                  <button
                    type="button"
                    class="summary-chip summary-chip-button"
                    :class="{ 'is-clickable': isDaySummaryCardClickable('nightShift') }"
                    :disabled="!isDaySummaryCardClickable('nightShift')"
                    @click="openDaySummaryDetail('nightShift')"
                  >
                    <label>夜班</label>
                    <strong>{{ activeSelectedDayNightShiftTaskCount }}</strong>
                  </button>
                  <button
                    type="button"
                    class="summary-chip summary-chip-button warning"
                    :class="{ 'is-clickable': isDaySummaryCardClickable('shortages') }"
                    :disabled="!isDaySummaryCardClickable('shortages')"
                    @click="openDaySummaryDetail('shortages')"
                  >
                    <label>短缺</label>
                    <strong>{{ activeSelectedDayShortageCount }}</strong>
                  </button>
                  <button
                    type="button"
                    class="summary-chip summary-chip-button"
                    :class="{ 'is-clickable': isDaySummaryCardClickable('materials') }"
                    :disabled="!isDaySummaryCardClickable('materials')"
                    @click="openDaySummaryDetail('materials')"
                  >
                    <label>物料</label>
                    <strong>{{ activeSelectedDayMaterialCount }}</strong>
                  </button>
                  <button
                    type="button"
                    class="summary-chip summary-chip-button danger"
                    :class="{ 'is-clickable': isDaySummaryCardClickable('issues') }"
                    :disabled="!isDaySummaryCardClickable('issues')"
                    @click="openDaySummaryDetail('issues')"
                  >
                    <label>异常</label>
                    <strong>{{ selectedDayOpenIssueCount }}</strong>
                  </button>
                  <button
                    type="button"
                    class="summary-chip summary-chip-button"
                    :class="{ 'is-clickable': isDaySummaryCardClickable('locked') }"
                    :disabled="!isDaySummaryCardClickable('locked')"
                    @click="openDaySummaryDetail('locked')"
                  >
                    <label>锁定</label>
                    <strong>{{ selectedDayLockedCount }}</strong>
                  </button>
                </div>
                <div class="process-capacity-section">
                  <div class="section-head process-capacity-head">
                    <span>工序产能利用</span>
                    <span class="section-tip">按当天实际排产任务汇总</span>
                  </div>
                  <div v-if="selectedDayProcessCapacityRows.length" class="process-capacity-list">
                    <div
                      v-for="row in selectedDayProcessCapacityRows"
                      :key="row.processId || row.processName"
                      class="process-capacity-row"
                    >
                      <div class="process-capacity-row-head">
                        <strong>{{ row.processName || `工序#${row.processId}` }}</strong>
                        <el-tag :type="buildProcessCapacityStatusType(row)" effect="light">
                          利用率 {{ buildPercentLabel(row.utilizationRate) }}
                        </el-tag>
                      </div>
                      <div class="process-capacity-metrics">
                        <span>
                          <label>最大产能</label>
                          <strong>{{ buildQuantityLabel(row.maxCapacity, '件') }}</strong>
                        </span>
                        <span>
                          <label>已排产能</label>
                          <strong>{{ buildQuantityLabel(row.scheduledQuantity, '件') }}</strong>
                        </span>
                        <span :class="{ 'is-overrun': isProcessCapacityOverrun(row) }">
                          <label>{{ isProcessCapacityOverrun(row) ? '超出产能' : '剩余产能' }}</label>
                          <strong>
                            {{
                              buildQuantityLabel(
                                isProcessCapacityOverrun(row)
                                  ? row.overCapacity
                                  : row.remainingCapacity,
                                '件'
                              )
                            }}
                          </strong>
                        </span>
                        <span>
                          <label>任务/工单</label>
                          <strong>{{ row.taskCount || 0 }} / {{ row.workOrderCount || 0 }}</strong>
                        </span>
                      </div>
                    </div>
                  </div>
                  <el-empty v-else description="暂无工序产能数据" :image-size="56" />
                </div>
                <p class="shift-rule-hint">白班夜班由排产员控制条决定</p>
              </div>
            </section>
          </el-tab-pane>
        </el-tabs>
      </aside>
    </div>
  </div>

  <Dialog title="异常登记" v-model="issueCreateDialogVisible" width="640px">
    <el-form :model="issueCreateForm" label-width="96px">
      <el-form-item label="异常类型">
        <el-input
          v-model="issueCreateForm.issueType"
          placeholder="如 CAPACITY / MATERIAL / MANUAL_EXCEPTION"
        />
      </el-form-item>
      <el-form-item label="严重级别">
        <el-select v-model="issueCreateForm.severity">
          <el-option label="阻塞" value="BLOCKING" />
          <el-option label="预警" value="WARNING" />
        </el-select>
      </el-form-item>
      <el-form-item label="工单ID">
        <el-input-number v-model="issueCreateForm.workOrderId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input
          v-model="issueCreateForm.message"
          type="textarea"
          :rows="4"
          placeholder="请说明异常原因、影响工序和期望处理方式"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="issueCreateDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="issueActionLoading" @click="submitIssueCreate"
        >登记异常</el-button
      >
    </template>
  </Dialog>

  <Dialog title="关闭异常" v-model="issueResolveDialogVisible" width="640px">
    <el-form :model="issueResolveForm" label-width="96px">
      <el-form-item label="异常ID">
        <el-input-number v-model="issueResolveForm.id" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="关闭原因">
        <el-input
          v-model="issueResolveForm.resolutionReason"
          type="textarea"
          :rows="4"
          placeholder="请说明处理结果、是否需要重排以及关闭依据"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="issueResolveDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="issueActionLoading" @click="submitIssueResolve"
        >关闭异常</el-button
      >
    </template>
  </Dialog>

  <Dialog :title="materialDialogTitle" v-model="materialDialogVisible" width="1180px">
    <div class="material-dialog">
      <div class="material-dialog-summary">
        <span>
          <label>累计工单</label>
          <strong>{{ selectedDayMaterialDemandSummary.workOrderCount }}</strong>
        </span>
        <span>
          <label>物料种类</label>
          <strong>{{ selectedDayMaterialDemandSummary.materialCount }}</strong>
        </span>
        <span class="warning">
          <label>缺失物料</label>
          <strong>{{ selectedDayMaterialShortageRows.length }}</strong>
        </span>
      </div>
      <el-tabs v-model="materialDialogTab">
        <el-tab-pane label="总物料" name="total">
          <el-table
            :data="selectedDayMaterialTotalRows"
            :stripe="true"
            :show-overflow-tooltip="true"
          >
            <el-table-column label="物料编码" min-width="160" prop="materialCode" />
            <el-table-column label="物料名称" min-width="180" prop="materialName" />
            <el-table-column label="累计需求" width="120" align="center">
              <template #default="{ row }">{{ buildQuantityLabel(row.requiredQty) }}</template>
            </el-table-column>
            <el-table-column label="可用库存" width="120" align="center">
              <template #default="{ row }">{{ buildQuantityLabel(row.availableQty) }}</template>
            </el-table-column>
            <el-table-column label="缺失数量" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="Number(row.shortageQty || 0) > 0 ? 'danger' : 'success'" effect="light">
                  {{ buildQuantityLabel(row.shortageQty) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="涉及工单" width="110" align="center">
              <template #default="{ row }">{{ row.affectedWorkOrderCount ?? 0 }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="订单物料" name="orders">
          <el-table
            :data="selectedDayMaterialWorkOrderRows"
            :stripe="true"
            :show-overflow-tooltip="true"
          >
            <el-table-column label="工单" min-width="160" prop="workOrderCode" />
            <el-table-column label="物料编码" min-width="160" prop="materialCode" />
            <el-table-column label="物料名称" min-width="180" prop="materialName" />
            <el-table-column label="订单需求" width="120" align="center">
              <template #default="{ row }">{{ buildQuantityLabel(row.requiredQty) }}</template>
            </el-table-column>
            <el-table-column label="可用库存" width="120" align="center">
              <template #default="{ row }">{{ buildQuantityLabel(row.availableQty) }}</template>
            </el-table-column>
            <el-table-column label="缺失数量" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="Number(row.shortageQty || 0) > 0 ? 'danger' : 'success'" effect="light">
                  {{ buildQuantityLabel(row.shortageQty) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="缺失物料" name="shortages">
          <el-table
            :data="selectedDayMaterialShortageRows"
            :stripe="true"
            :show-overflow-tooltip="true"
          >
            <el-table-column label="物料编码" min-width="160">
              <template #default="{ row }">{{ buildMaterialCodeLabel(row) }}</template>
            </el-table-column>
            <el-table-column label="物料名称" min-width="180">
              <template #default="{ row }">{{ buildMaterialNameLabel(row) }}</template>
            </el-table-column>
            <el-table-column label="当天需求" width="120" align="center">
              <template #default="{ row }">{{ buildQuantityLabel(row.scheduledUsageQty) }}</template>
            </el-table-column>
            <el-table-column label="累计需求" width="120" align="center">
              <template #default="{ row }">{{ buildQuantityLabel(row.requiredQty) }}</template>
            </el-table-column>
            <el-table-column label="可用库存" width="120" align="center">
              <template #default="{ row }">{{ buildQuantityLabel(row.availableQty) }}</template>
            </el-table-column>
            <el-table-column label="缺失数量" width="120" align="center">
              <template #default="{ row }">{{ buildQuantityLabel(row.shortageQty) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </Dialog>

  <Dialog :title="shortageDialogTitle" v-model="shortageDialogVisible" width="980px">
    <el-table :data="issueDialogVisibleRows" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="物料编码" min-width="160">
        <template #default="{ row }">
          {{ buildMaterialCodeLabel(row) }}
        </template>
      </el-table-column>
      <el-table-column label="物料名称" min-width="180">
        <template #default="{ row }">
          {{ buildMaterialNameLabel(row) }}
        </template>
      </el-table-column>
      <el-table-column label="缺口" width="120" align="center">
        <template #default="{ row }">
          {{ row.shortageQty ?? row.requiredQty ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="daySummaryDialogType === 'nightShift'"
        label="操作"
        width="120"
        align="center"
      >
        <template #default="{ row }">
          <el-button link type="danger" :disabled="!row.taskId" @click="cancelNightShiftTask(row)">
            取消夜班
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </Dialog>

  <Dialog :title="daySummaryDialogTitle" v-model="daySummaryDialogVisible" width="1120px">
    <el-table
      v-if="daySummaryDialogType === 'orders'"
      :data="daySummaryDialogOrderRows"
      :stripe="true"
      :show-overflow-tooltip="true"
    >
      <el-table-column label="工单" min-width="180">
        <template #default="{ row }">
          <el-button
            v-if="row.workOrderId"
            link
            type="primary"
            class="issue-link-button"
            @click="openWorkOrderAnalysis(row.workOrderId, row.workOrderCode)"
          >
            {{ row.workOrderCode }}
          </el-button>
          <span v-else>{{ row.workOrderCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="产品" prop="itemLabel" min-width="220" />
      <el-table-column label="任务数" prop="taskCount" width="100" align="center" />
      <el-table-column label="白班" prop="dayShiftTaskCount" width="90" align="center" />
      <el-table-column label="夜班" prop="nightShiftTaskCount" width="90" align="center" />
      <el-table-column label="锁定" prop="lockedTaskCount" width="90" align="center" />
      <el-table-column label="工单计划量" width="120" align="center">
        <template #default="{ row }">{{ buildQuantityLabel(row.totalQuantity) }}</template>
      </el-table-column>
      <el-table-column label="当日工序量" width="120" align="center">
        <template #default="{ row }">{{ buildQuantityLabel(row.dailyProcessQuantity) }}</template>
      </el-table-column>
      <el-table-column label="总任务量" width="120" align="center">
        <template #default="{ row }">{{ buildQuantityLabel(row.processTaskQuantity) }}</template>
      </el-table-column>
      <el-table-column label="车间" prop="workshopNames" min-width="180" />
      <el-table-column label="产线" prop="lineNames" min-width="180" />
    </el-table>
    <el-table
      v-else-if="daySummaryDialogType === 'issues'"
      :data="daySummaryDialogIssueRows"
      :stripe="true"
      :show-overflow-tooltip="true"
    >
      <el-table-column label="类型" prop="issueType" width="130" />
      <el-table-column label="严重级别" prop="severity" width="110" />
      <el-table-column label="工单" prop="workOrderCode" width="150" />
      <el-table-column label="任务" prop="taskId" width="110" />
      <el-table-column label="来源" prop="sourceType" width="120" />
      <el-table-column label="说明" prop="message" min-width="240" />
    </el-table>
    <div v-else class="day-summary-task-group-layout">
      <div class="day-summary-workorder-list">
        <div
          v-for="group in daySummaryTaskWorkOrderGroups"
          :key="group.key"
          class="day-summary-workorder-card"
          :class="{ active: group.key === selectedDaySummaryWorkOrderKey }"
          role="button"
          tabindex="0"
          @click="selectDaySummaryWorkOrder(group.key)"
          @keydown.enter.prevent="selectDaySummaryWorkOrder(group.key)"
          @keydown.space.prevent="selectDaySummaryWorkOrder(group.key)"
        >
          <span class="day-summary-workorder-card__line-name" :title="group.lineNames">
            {{ group.lineNames }}
          </span>
          <span class="day-summary-workorder-card__meta">
            {{ group.taskCount }} 道工序 /
            {{ buildQuantityLabel(group.workOrderPlanQuantity) }} 件计划 /
            {{ buildQuantityLabel(group.dailyProcessQuantity) }} 件当日 /
            {{ buildQuantityLabel(group.processTaskQuantity) }} 件总量
          </span>
          <el-button
            v-if="group.workOrderId"
            link
            type="primary"
            class="issue-link-button"
            :title="`${group.lineNames} 工单产线分析`"
            @click.stop="openWorkOrderAnalysis(group.workOrderId, group.workOrderCode)"
          >
            产线分析
          </el-button>
          <span class="day-summary-workorder-card__tags">
            <el-tag size="small" effect="light">白班 {{ group.dayShiftTaskCount }}</el-tag>
            <el-tag size="small" type="warning" effect="light">
              夜班 {{ group.nightShiftTaskCount }}
            </el-tag>
            <el-tag v-if="group.lockedTaskCount" size="small" type="danger" effect="light">
              锁定 {{ group.lockedTaskCount }}
            </el-tag>
          </span>
        </div>
      </div>
      <div class="day-summary-selected-task-table">
        <el-table :data="selectedDaySummaryTaskRows" :stripe="true" :show-overflow-tooltip="true">
          <el-table-column label="工序" min-width="160">
            <template #default="{ row }">
              <el-button
                v-if="row.routeId"
                link
                type="primary"
                @click="openRouteDetail(row.routeId, row.routeName)"
              >
                {{ buildTaskProcessLabel(row) }}
              </el-button>
              <span v-else>{{ buildTaskProcessLabel(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="产品编码" min-width="150">
            <template #default="{ row }">
              {{ buildTaskProductCodeLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column label="产品名称" min-width="180">
            <template #default="{ row }">
              {{ buildTaskProductNameLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column label="当日完成量" width="120" align="center">
            <template #default="{ row }">{{ buildQuantityLabel(resolveTaskDailyQuantity(row)) }}</template>
          </el-table-column>
          <el-table-column label="总任务量" width="100" align="center">
            <template #default="{ row }">{{ buildQuantityLabel(row.quantity) }}</template>
          </el-table-column>
          <el-table-column label="已报工" width="100" align="center">
            <template #default="{ row }">{{ buildQuantityLabel(row.reportedQuantity) }}</template>
          </el-table-column>
          <el-table-column label="锁定" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.locked ? 'warning' : 'info'" effect="light">
                {{ row.locked ? '已锁定' : '未锁定' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="排产冻结" min-width="140" align="center">
            <template #default="{ row }">
              <el-tag :type="row.scheduleOrderFrozen ? 'danger' : 'info'" effect="light">
                {{ buildTaskFreezeText(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="产线" min-width="180">
            <template #default="{ row }">
              {{ buildTaskLineNameLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column label="任务操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                link
                :type="row.locked ? 'warning' : 'primary'"
                :loading="taskActionLoadingId === row.taskId"
                @click="toggleTaskLock(row)"
              >
                {{ row.locked ? '解锁' : '锁定' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </Dialog>

  <Dialog :title="workOrderAnalysisDialogTitle" v-model="workOrderAnalysisVisible" width="1120px">
    <div v-loading="workOrderAnalysisLoading" class="work-order-analysis-dialog">
      <div class="panel-head">
        <div>
          <h3>工单产线分析</h3>
          <p>按工单数量、工序资源产能和当前排产结果汇总</p>
        </div>
        <el-button
          type="primary"
          plain
          :disabled="!workOrderAnalysis?.workOrderId"
          @click="
            openWorkOrderDetail(
              workOrderAnalysis?.workOrderId,
              workOrderAnalysis?.workOrderCode || workOrderAnalysisRequestedCode
            )
          "
        >
          查看工单主数据
        </el-button>
      </div>

      <el-alert
        v-if="workOrderAnalysisErrorMessage"
        :title="workOrderAnalysisErrorMessage"
        type="error"
        :closable="false"
        class="calendar-alert"
      />
      <div v-if="shouldShowCalendarRecovery('work-order-analysis')" class="calendar-recovery-card">
        <span>错误对象：{{ calendarRecoveryState?.objectLabel }}</span>
        <span>影响范围：{{ calendarRecoveryState?.impactScope }}</span>
        <span>恢复入口：{{ calendarRecoveryState?.recoveryHint }}</span>
        <el-button type="primary" link @click="openScheduleRouteRecovery">
          {{ calendarRecoveryState?.recoveryButtonText }}
        </el-button>
      </div>
      <el-alert
        v-else-if="workOrderAnalysis?.conflict"
        :title="workOrderAnalysis.conflictMessage || '当前工单存在跨产线冲突，无法生成唯一产线分析'"
        type="warning"
        :closable="false"
        class="calendar-alert"
      />
      <template v-else-if="workOrderAnalysis">
        <div class="analysis-summary-grid">
          <div class="summary-chip">
            <label>工单</label>
            <strong>{{ workOrderAnalysis.workOrderCode || '--' }}</strong>
          </div>
          <div class="summary-chip">
            <label>产品</label>
            <strong>{{ buildWorkOrderAnalysisProductLabel() }}</strong>
          </div>
          <div class="summary-chip">
            <label>数量</label>
            <strong>{{ buildQuantityLabel(workOrderAnalysis.quantity) }}</strong>
          </div>
          <div class="summary-chip">
            <label>归属产线</label>
            <strong>{{ buildWorkOrderAnalysisLineLabel() }}</strong>
          </div>
          <div class="summary-chip">
            <label>起止时间</label>
            <strong>{{ buildTaskTimeRange(workOrderAnalysis as any) }}</strong>
          </div>
          <div class="summary-chip warning">
            <label>瓶颈工序</label>
            <strong>{{ buildWorkOrderAnalysisBottleneckLabel() }}</strong>
          </div>
        </div>

        <el-table :data="workOrderAnalysisProcessRows" :stripe="true" :show-overflow-tooltip="true">
          <el-table-column label="工序" min-width="200">
            <template #default="{ row }">
              {{ row.processName || `工序#${row.processId}` }}
            </template>
          </el-table-column>
          <el-table-column label="数量" width="100" align="center">
            <template #default="{ row }">{{ buildQuantityLabel(row.scheduledQuantity) }}</template>
          </el-table-column>
          <el-table-column label="资源模式" width="100" align="center">
            <template #default="{ row }">
              {{ buildCapacitySourceLabel(row.capacitySource) }}
            </template>
          </el-table-column>
          <el-table-column label="工作站数" prop="workstationCount" width="100" align="center" />
          <el-table-column label="设备数" prop="machineCount" width="90" align="center" />
          <el-table-column
            label="配置人数"
            prop="configuredWorkerCount"
            width="100"
            align="center"
          />
          <el-table-column label="在岗人数" prop="currentWorkerCount" width="100" align="center" />
          <el-table-column label="有效小时产能" width="120" align="center">
            <template #default="{ row }">
              {{ buildCapacityLabel(row.effectiveHourlyCapacity, '件/小时') }}
            </template>
          </el-table-column>
          <el-table-column label="计划时长" width="100" align="center">
            <template #default="{ row }">
              {{ buildQuantityLabel(row.plannedDurationMinutes, '分钟') }}
            </template>
          </el-table-column>
          <el-table-column label="时间" width="140" align="center">
            <template #default="{ row }">
              {{ buildTaskTimeRange(row as any) }}
            </template>
          </el-table-column>
          <el-table-column label="工作站" min-width="220">
            <template #default="{ row }">
              {{ buildProcessWorkstationLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column label="瓶颈" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.bottleneck ? 'warning' : 'info'" effect="light">
                {{ row.bottleneck ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else description="暂无工单产线分析" />
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import {
  ProScheduleCalendarApi,
  type ProScheduleCalendarCapacityGenerateRespVO,
  type ProScheduleCalendarDateShiftMode,
  type ProScheduleCalendarDayDetailRespVO,
  type ProScheduleCalendarLineVO,
  type ProScheduleCalendarIssueItemVO,
  type ProScheduleCalendarMaterialDemandSummaryVO,
  type ProScheduleCalendarMaterialDemandTotalItemVO,
  type ProScheduleCalendarMaterialDemandWorkOrderItemVO,
  type ProScheduleCalendarMaterialShortageItemVO,
  type ProScheduleCalendarMonthDayVO,
  type ProScheduleCalendarMonthRespVO,
  type ProScheduleCalendarProcessCapacityItemVO,
  type ProScheduleCalendarRulesRespVO,
  type ProScheduleCalendarRulesUpdateReqVO,
  type ProScheduleCalendarWorkOrderAnalysisProcessVO,
  type ProScheduleCalendarWorkOrderAnalysisVO,
  type ProScheduleCalendarTaskVO,
  type ProScheduleCalendarWorkshopVO
} from '@/api/mes/pro/scheduleCalendar'
import {
  ProTaskAutoScheduleApi,
  type ProTaskAutoScheduleIssueVO,
  type ProTaskAutoSchedulePreviewRespVO
} from '@/api/mes/pro/task/autoSchedule'
import { ProWorkOrderBomApi, type ProWorkOrderItemVO } from '@/api/mes/pro/workorder/bom'
import { ProTaskApi } from '@/api/mes/pro/task'

defineOptions({ name: 'MesCalProScheduleCalendar' })

type ShortageDialogRow = ProTaskAutoScheduleIssueVO | ProScheduleCalendarMaterialShortageItemVO
type DaySummaryCardType =
  | 'tasks'
  | 'orders'
  | 'dayShift'
  | 'nightShift'
  | 'shortages'
  | 'materials'
  | 'issues'
  | 'locked'
type DaySummaryDialogType = Exclude<DaySummaryCardType, 'shortages' | 'materials'>
type CalendarMetricDialogType = 'tasks' | 'orders' | 'shortages'
type MaterialDialogTab = 'total' | 'orders' | 'shortages'
type CalendarRecoveryArea = 'month' | 'detail' | 'preview' | 'work-order-analysis'

interface CalendarCell {
  date: string
  dayNumber: string
  currentMonth: boolean
  today: boolean
  selected: boolean
  info?: ProScheduleCalendarMonthDayVO
}

interface DaySummaryTaskDetailRow extends ProScheduleCalendarTaskVO {
  workshopTitle: string
  lineTitle: string
  lineNameTitle: string
}

interface DaySummaryWorkOrderDetailRow {
  workOrderId?: number
  workOrderCode: string
  itemLabel: string
  taskCount: number
  dayShiftTaskCount: number
  nightShiftTaskCount: number
  lockedTaskCount: number
  totalQuantity: number
  dailyProcessQuantity: number
  processTaskQuantity: number
  workshopNames: string
  lineNames: string
}

interface DaySummaryTaskWorkOrderGroup {
  key: string
  workOrderId?: number
  workOrderCode: string
  lineNames: string
  taskCount: number
  dayShiftTaskCount: number
  nightShiftTaskCount: number
  lockedTaskCount: number
  workOrderPlanQuantity: number
  dailyProcessQuantity: number
  processTaskQuantity: number
  rows: DaySummaryTaskDetailRow[]
}

interface PreviewTaskSource {
  id?: string
  originalId?: number
  type?: number
  text?: string
  parent?: string
  workstation?: string
  process?: string
  line?: string
  quantity?: number
  dailyQuantity?: number
  scheduleSource?: string
  locked?: boolean
  riskStatus?: string
  startDate?: string | number | Date
  endDate?: string | number | Date
  duration?: number
}

interface PreviewMaterialAccumulator {
  materialId: number
  materialCode?: string
  materialName?: string
  scheduledUsageQty: number
  workOrderIds: Set<number>
}

interface CalendarRecoveryState {
  area: CalendarRecoveryArea
  title: string
  action: string
  objectLabel: string
  impactScope: string
  recoveryHint: string
  recoveryButtonText: string
  recoveryQuery: Record<string, string | undefined>
  rawMessage: string
}

const weekdayLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const calendarShiftOptions: Array<{ label: string; value: ProScheduleCalendarDateShiftMode }> = [
  { label: '白班', value: 'DAY' },
  { label: '休息', value: 'REST' }
]
const defaultRules = (): ProScheduleCalendarRulesRespVO => ({
  skipStatutoryHolidays: false,
  weekendRestMode: 'DOUBLE',
  dateShiftModeByDate: {},
  simulationCurrentDate: ''
})

const defaultMonthData = (): ProScheduleCalendarMonthRespVO => ({
  month: '',
  simulationCurrentDate: '',
  currentScheduleStatus: {
    hasCurrentSchedule: false,
    updatedAt: '',
    totalTaskCount: 0
  },
  days: []
})

const monthLoading = ref(false)
const detailLoading = ref(false)
const rulesLoading = ref(false)
const rulesSaving = ref(false)
const capacityGenerateLoading = ref(false)

const currentMonth = ref(dayjs().startOf('month'))
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const rulesForm = reactive(defaultRules())
const rulesSnapshot = ref('')
const calendarShiftEditorDate = ref('')
const activeSidebarTab = ref('detail')
const capacityGenerateDays = ref(30)

const monthData = ref<ProScheduleCalendarMonthRespVO>(defaultMonthData())
const dayDetail = ref<ProScheduleCalendarDayDetailRespVO | null>(null)
const capacityGenerateSummary = ref<ProScheduleCalendarCapacityGenerateRespVO | null>(null)
const autoSchedulePreview = ref<ProTaskAutoSchedulePreviewRespVO | null>(null)
const previewMaterialDemandByWorkOrderId = ref<Record<number, ProWorkOrderItemVO[]>>({})
const monthErrorMessage = ref('')
const detailErrorMessage = ref('')
const calendarRecoveryState = ref<CalendarRecoveryState | null>(null)

const shortageDialogVisible = ref(false)
const shortageDialogTitle = ref('短缺明细')
const shortageDialogRows = ref<ShortageDialogRow[]>([])
const materialDialogVisible = ref(false)
const materialDialogTab = ref<MaterialDialogTab>('total')
const daySummaryDialogVisible = ref(false)
const daySummaryDialogType = ref<DaySummaryDialogType>('tasks')
const selectedDaySummaryWorkOrderKey = ref('')
const issueCreateDialogVisible = ref(false)
const issueResolveDialogVisible = ref(false)
const issueActionLoading = ref(false)
const issueCreateForm = reactive({
  issueType: 'MANUAL_EXCEPTION',
  severity: 'BLOCKING',
  workOrderId: undefined as number | undefined,
  message: ''
})
const issueResolveForm = reactive({
  id: undefined as number | undefined,
  resolutionReason: ''
})
const workOrderAnalysisVisible = ref(false)
const workOrderAnalysisLoading = ref(false)
const workOrderAnalysisRequestedCode = ref('')
const workOrderAnalysisErrorMessage = ref('')
const workOrderAnalysis = ref<ProScheduleCalendarWorkOrderAnalysisVO | null>(null)
const taskActionLoadingId = ref<number | null>(null)

const route = useRoute()
const router = useRouter()
const { push } = router
const message = useMessage()

const rulesDirty = computed(() => JSON.stringify(buildRulesPayload()) !== rulesSnapshot.value)

const monthTitle = computed(() => currentMonth.value.format('YYYY年MM月'))

const simulationDateLabel = computed(() => {
  return normalizeDate(rulesForm.simulationCurrentDate) || selectedDate.value
})

const todayDateLabel = computed(() => dayjs().format('YYYY-MM-DD'))

const resolveRouteQueryText = (value: unknown) => {
  return Array.isArray(value) ? String(value[0] || '') : String(value || '')
}

const resolveCalendarQueryDate = () => {
  const queryDate = normalizeDate(resolveRouteQueryText(route.query.date))
  return /^\d{4}-\d{2}-\d{2}$/.test(queryDate) ? queryDate : ''
}

const shouldOpenShiftEditorFromQuery = () =>
  resolveRouteQueryText(route.query.openShiftEditor) === '1'

const isSimulatedCalendarContext = computed(() => {
  return simulationDateLabel.value !== todayDateLabel.value
})

const previewCalendarContextTagType = computed(() => {
  return isSimulatedCalendarContext.value ? 'warning' : 'primary'
})

const previewCalendarContextTagText = computed(() => {
  return isSimulatedCalendarContext.value ? '模拟日历预览' : '正式日历预览'
})

const previewCalendarContextLabel = computed(() => {
  return isSimulatedCalendarContext.value
    ? `当前预览绑定模拟日历，上下文日期 ${simulationDateLabel.value}`
    : `当前预览绑定正式日历，上下文日期 ${todayDateLabel.value}`
})

const selectedDayTitle = computed(() => `${selectedDate.value} 日详情`)

const capacityGenerateSummaryText = computed(() => {
  const summary = capacityGenerateSummary.value
  if (!summary) {
    return ''
  }
  return [
    `产能 ${summary.startDate} 至 ${summary.endDate}`,
    `产线 ${summary.lineCount}`,
    `生成 ${summary.generatedCount}`,
    `跳过已有 ${summary.skippedExistingCount}`,
    `休息日 ${summary.skippedRestCount}`,
    `无班次 ${summary.skippedNoShiftCount}`
  ].join(' / ')
})

const issueDialogVisibleRows = computed(() => {
  return shortageDialogRows.value
})

const isPreviewCalendarOverlayActive = computed(() => {
  return (autoSchedulePreview.value?.tasks || []).some(
    (task) => Number((task as PreviewTaskSource)?.type) === 303
  )
})

const previewParentTaskMap = computed(() => {
  const map = new Map<string, PreviewTaskSource>()
  ;(autoSchedulePreview.value?.tasks || []).forEach((task) => {
    const previewTask = task as PreviewTaskSource
    if (Number(previewTask.type) === 301 && previewTask.id) {
      map.set(previewTask.id, previewTask)
    }
  })
  return map
})

const previewWorkOrderCodeById = computed(() => {
  const map = new Map<number, string>()
  ;(autoSchedulePreview.value?.issues || []).forEach((issue) => {
    if (issue.workOrderId && issue.workOrderCode) {
      map.set(issue.workOrderId, issue.workOrderCode)
    }
  })
  return map
})

const previewWorkOrderAnalysisMap = computed(() => {
  const map = new Map<number, ProScheduleCalendarWorkOrderAnalysisVO>()
  ;(autoSchedulePreview.value?.workOrderAnalyses || []).forEach((analysis) => {
    if (analysis.workOrderId) {
      map.set(analysis.workOrderId, analysis)
    }
  })
  return map
})

const previewTaskRows = computed<DaySummaryTaskDetailRow[]>(() => {
  return (autoSchedulePreview.value?.tasks || [])
    .map((task) => task as PreviewTaskSource)
    .filter(
      (task): task is PreviewTaskSource & { originalId: number } =>
        Number(task.type) === 303 && typeof task.originalId === 'number'
    )
    .flatMap((task): DaySummaryTaskDetailRow[] => {
      const workOrderId = resolvePreviewWorkOrderId(task)
      if (!workOrderId) {
        return []
      }
      const parentTask = task.parent ? previewParentTaskMap.value.get(task.parent) : undefined
      const previewLineName = resolvePreviewTaskLineName(workOrderId)
      return [{
        taskId: task.originalId,
        taskCode: task.id || '',
        workOrderId,
        workOrderCode: resolvePreviewWorkOrderCode(workOrderId),
        routeName: previewLineName === '--' ? '预览任务' : `${previewLineName} · 预览任务`,
        processName: task.process,
        itemName: resolvePreviewParentProductName(parentTask),
        shiftCode: inferPreviewShiftCode(task.startDate),
        quantity: task.quantity,
        dailyQuantity: task.dailyQuantity,
        startTime: normalizeDateTimeText(task.startDate),
        endTime: normalizeDateTimeText(task.endDate),
        scheduleSource: task.scheduleSource,
        locked: Boolean(task.locked),
        riskStatus: task.riskStatus,
        reportedQuantity: 0,
        pendingInspectionQuantity: 0,
        executionStatus: 'PREVIEW',
        scheduleOrderFrozen: false,
        scheduleOrderFreezeReason: '',
        workshopTitle: '预览任务',
        lineTitle: previewLineName === '--' ? '未绑定产线' : previewLineName,
        lineNameTitle: previewLineName
      }]
    })
})

const previewTaskRowsByDate = computed(() => {
  const map = new Map<string, DaySummaryTaskDetailRow[]>()
  previewTaskRows.value.forEach((task) => {
    buildDateKeysForTask(task.startTime, task.endTime).forEach((date) => {
      if (!map.has(date)) {
        map.set(date, [])
      }
      map.get(date)!.push(task)
    })
  })
  return map
})

const previewWorkOrderFirstDateMap = computed(() => {
  const map = new Map<number, string>()
  previewTaskRows.value.forEach((task) => {
    if (!task.workOrderId || !task.startTime) {
      return
    }
    const date = normalizeDate(task.startTime)
    const current = map.get(task.workOrderId)
    if (!current || date < current) {
      map.set(task.workOrderId, date)
    }
  })
  return map
})

const previewMaterialIssueByMaterialId = computed(() => {
  const map = new Map<number, ProTaskAutoScheduleIssueVO>()
  ;(autoSchedulePreview.value?.issues || []).forEach((issue) => {
    if (issue.issueType === 'MATERIAL' && issue.materialId && !map.has(issue.materialId)) {
      map.set(issue.materialId, issue)
    }
  })
  return map
})

const previewMaterialRowsByDate = computed(() => {
  const accumulatorsByDate = new Map<string, Map<number, PreviewMaterialAccumulator>>()
  previewWorkOrderFirstDateMap.value.forEach((date, workOrderId) => {
    const demandItems = previewMaterialDemandByWorkOrderId.value[workOrderId] || []
    demandItems.forEach((item) => {
      if (!item.itemId || !previewMaterialIssueByMaterialId.value.has(item.itemId)) {
        return
      }
      if (!accumulatorsByDate.has(date)) {
        accumulatorsByDate.set(date, new Map())
      }
      const materialMap = accumulatorsByDate.get(date)!
      if (!materialMap.has(item.itemId)) {
        materialMap.set(item.itemId, {
          materialId: item.itemId,
          materialCode: item.itemCode,
          materialName: item.itemName,
          scheduledUsageQty: 0,
          workOrderIds: new Set<number>()
        })
      }
      const current = materialMap.get(item.itemId)!
      current.scheduledUsageQty += Number(item.quantity || 0)
      current.workOrderIds.add(workOrderId)
    })
  })

  const result = new Map<string, ProScheduleCalendarMaterialShortageItemVO[]>()
  const previewDates = [...accumulatorsByDate.keys()].sort()
  if (!previewDates.length) {
    return result
  }
  const cumulativeRequiredByMaterialId = new Map<number, number>()
  const cumulativeWorkOrderIdsByMaterialId = new Map<number, Set<number>>()
  let cursor = dayjs(previewDates[0])
  const endDate = currentMonth.value.endOf('month')
  while (cursor.isBefore(endDate) || cursor.isSame(endDate, 'day')) {
    const date = cursor.format('YYYY-MM-DD')
    const materialMap =
      accumulatorsByDate.get(date) || new Map<number, PreviewMaterialAccumulator>()
    materialMap.forEach((accumulator, materialId) => {
      const currentRequired = cumulativeRequiredByMaterialId.get(materialId) || 0
      cumulativeRequiredByMaterialId.set(
        materialId,
        currentRequired + accumulator.scheduledUsageQty
      )
      if (!cumulativeWorkOrderIdsByMaterialId.has(materialId)) {
        cumulativeWorkOrderIdsByMaterialId.set(materialId, new Set<number>())
      }
      const workOrderIds = cumulativeWorkOrderIdsByMaterialId.get(materialId)!
      accumulator.workOrderIds.forEach((workOrderId) => workOrderIds.add(workOrderId))
    })

    const rows: ProScheduleCalendarMaterialShortageItemVO[] = []
    cumulativeRequiredByMaterialId.forEach((cumulativeRequiredQty, materialId) => {
      const issue = previewMaterialIssueByMaterialId.value.get(materialId)
      if (!issue) {
        return
      }
      const accumulator = materialMap.get(materialId)
      const scheduledUsageQty = accumulator?.scheduledUsageQty || 0
      const totalAvailable = Number(issue.availableQty || 0)
      const requiredBeforeToday = cumulativeRequiredQty - scheduledUsageQty
      const remainingAvailableQty = Math.max(totalAvailable - requiredBeforeToday, 0)
      const currentShortageQty = Math.max(cumulativeRequiredQty - totalAvailable, 0)
      if (currentShortageQty <= 0) {
        return
      }
      rows.push({
        issueId: issue.id || 0,
        severity: issue.severity,
        materialId,
        materialCode: accumulator?.materialCode || issue.materialCode,
        materialName: accumulator?.materialName || issue.materialName,
        scheduledUsageQty,
        remainingAvailableQty,
        affectedWorkOrderCount: cumulativeWorkOrderIdsByMaterialId.get(materialId)?.size || 0,
        requiredQty: cumulativeRequiredQty,
        availableQty: totalAvailable,
        shortageQty: currentShortageQty,
        message: issue.message
      })
    })
    if (rows.length) {
      result.set(date, rows)
    }
    cursor = cursor.add(1, 'day')
  }
  return result
})

const formalMonthStats = computed(() => {
  return monthData.value.days.reduce(
    (summary, item) => {
      summary.taskCount += item.totalTaskCount || 0
      summary.orderCount += item.totalOrderCount || 0
      summary.shortageCount += item.shortageCount || 0
      return summary
    },
    {
      taskCount: 0,
      orderCount: 0,
      shortageCount: 0
    }
  )
})

const previewCalendarDayMap = computed(() => {
  const map = new Map<string, ProScheduleCalendarMonthDayVO>()
  calendarDayMap.value.forEach((item, date) => {
    map.set(date, {
      ...item,
      totalTaskCount: 0,
      totalOrderCount: 0,
      dayShiftTaskCount: 0,
      nightShiftTaskCount: 0,
      shortageCount: 0
    })
  })
  const previewDates = new Set<string>([
    ...previewTaskRowsByDate.value.keys(),
    ...previewMaterialRowsByDate.value.keys()
  ])
  previewDates.forEach((date) => {
    const rows = previewTaskRowsByDate.value.get(date) || []
    const formal = map.get(date) || {
      date,
      holiday: false,
      dateShiftMode: 'DAY' as ProScheduleCalendarDateShiftMode,
      totalTaskCount: 0,
      totalOrderCount: 0,
      dayShiftTaskCount: 0,
      nightShiftTaskCount: 0,
      shortageCount: 0
    }
    const orderCount = new Set(rows.map((row) => row.workOrderId || row.workOrderCode)).size
    const shortageRows = previewMaterialRowsByDate.value.get(date) || []
    map.set(date, {
      ...formal,
      totalTaskCount: rows.length,
      totalOrderCount: orderCount,
      dayShiftTaskCount: rows.filter((row) => row.shiftCode === 'DAY').length,
      nightShiftTaskCount: rows.filter((row) => row.shiftCode === 'NIGHT').length,
      shortageCount: shortageRows.filter((row) => Number(row.shortageQty || 0) > 0).length
    })
  })
  return map
})

const activeCalendarDayMap = computed(() => {
  const source = isPreviewCalendarOverlayActive.value
    ? previewCalendarDayMap.value
    : calendarDayMap.value
  const next = new Map<string, ProScheduleCalendarMonthDayVO>()
  source.forEach((item, date) => {
    next.set(date, {
      ...item,
      dateShiftMode: rulesForm.dateShiftModeByDate?.[date] || item.dateShiftMode
    })
  })
  Object.entries(rulesForm.dateShiftModeByDate || {}).forEach(([date, mode]) => {
    if (next.has(date)) {
      return
    }
    next.set(date, {
      date,
      holiday: false,
      dateShiftMode: mode,
      totalTaskCount: 0,
      totalOrderCount: 0,
      dayShiftTaskCount: 0,
      nightShiftTaskCount: 0,
      shortageCount: 0
    })
  })
  return next
})

const activeMonthStats = computed(() => {
  if (!isPreviewCalendarOverlayActive.value) {
    return formalMonthStats.value
  }
  return [...activeCalendarDayMap.value.values()].reduce(
    (summary, item) => {
      summary.taskCount += item.totalTaskCount || 0
      summary.orderCount += item.totalOrderCount || 0
      summary.shortageCount += item.shortageCount || 0
      return summary
    },
    {
      taskCount: 0,
      orderCount: 0,
      shortageCount: 0
    }
  )
})

const selectedDayTaskRows = computed<DaySummaryTaskDetailRow[]>(() => {
  if (isPreviewCalendarOverlayActive.value) {
    return previewTaskRowsByDate.value.get(selectedDate.value) || []
  }
  if (!dayDetail.value?.workshops?.length) {
    return []
  }
  return dayDetail.value.workshops.flatMap((workshop) =>
    workshop.lines.flatMap((line) =>
      (line.tasks || []).map((task) => ({
        ...task,
        workshopTitle: buildWorkshopTitle(workshop),
        lineTitle: buildLineTitle(line),
        lineNameTitle: buildLineNameTitle(line)
      }))
    )
  )
})

const selectedDayTasks = computed<ProScheduleCalendarTaskVO[]>(() => selectedDayTaskRows.value)

const selectedDayDailyProcessQuantity = computed(() => {
  return selectedDayTaskRows.value.reduce((total, task) => total + resolveTaskDailyQuantity(task), 0)
})

const selectedDayTotalProcessTaskQuantity = computed(() => {
  return selectedDayTaskRows.value.reduce((total, task) => total + Number(task.quantity || 0), 0)
})

const selectedDayProcessCapacityRows = computed<ProScheduleCalendarProcessCapacityItemVO[]>(() => {
  if (isPreviewCalendarOverlayActive.value) {
    return []
  }
  return dayDetail.value?.processCapacitySummary?.items || []
})

const selectedDayMaterialRows = computed<ProScheduleCalendarMaterialShortageItemVO[]>(() => {
  return isPreviewCalendarOverlayActive.value
    ? previewMaterialRowsByDate.value.get(selectedDate.value) || []
    : dayDetail.value?.materialShortageSummary?.items || []
})

const selectedDayShortages = computed<ProScheduleCalendarMaterialShortageItemVO[]>(() => {
  return selectedDayMaterialRows.value.filter((item) => Number(item.shortageQty || 0) > 0)
})

const selectedDayMaterialDemandSummary = computed<ProScheduleCalendarMaterialDemandSummaryVO>(() => {
  const summary = dayDetail.value?.materialDemandSummary
  if (!isPreviewCalendarOverlayActive.value && summary) {
    return {
      materialCount: summary.materialCount || 0,
      workOrderCount: summary.workOrderCount || 0,
      totalItems: summary.totalItems || [],
      workOrderItems: summary.workOrderItems || []
    }
  }
  return {
    materialCount: selectedDayMaterialRows.value.length,
    workOrderCount: selectedDayOrderCount.value,
    totalItems: selectedDayMaterialRows.value.map((row) => ({
      materialId: row.materialId,
      materialCode: row.materialCode,
      materialName: row.materialName,
      requiredQty: row.requiredQty,
      availableQty: row.availableQty,
      shortageQty: row.shortageQty,
      affectedWorkOrderCount: row.affectedWorkOrderCount
    })),
    workOrderItems: []
  }
})

const selectedDayMaterialTotalRows = computed<ProScheduleCalendarMaterialDemandTotalItemVO[]>(() => {
  return selectedDayMaterialDemandSummary.value.totalItems || []
})

const selectedDayMaterialWorkOrderRows = computed<
  ProScheduleCalendarMaterialDemandWorkOrderItemVO[]
>(() => {
  return selectedDayMaterialDemandSummary.value.workOrderItems || []
})

const selectedDayMaterialShortageRows = computed<ProScheduleCalendarMaterialShortageItemVO[]>(
  () => selectedDayShortages.value
)

const activeSelectedDayMaterialCount = computed(() => {
  return selectedDayMaterialDemandSummary.value.materialCount || selectedDayMaterialRows.value.length
})

const selectedDayScheduleIssues = computed<ProScheduleCalendarIssueItemVO[]>(() => {
  return isPreviewCalendarOverlayActive.value
    ? []
    : dayDetail.value?.scheduleIssueSummary?.items || []
})

const selectedDayOpenIssueCount = computed(() => {
  return isPreviewCalendarOverlayActive.value
    ? 0
    : dayDetail.value?.scheduleIssueSummary?.openIssueCount ||
        selectedDayScheduleIssues.value.length
})

const selectedDayWorkOrderRows = computed<DaySummaryWorkOrderDetailRow[]>(() => {
  const orderMap = new Map<string, DaySummaryWorkOrderDetailRow>()
  selectedDayTaskRows.value.forEach((task) => {
    const orderKey = String(
      task.workOrderId || task.workOrderCode || task.taskId || buildTaskKey(task)
    )
    const dailyQuantity = resolveTaskDailyQuantity(task)
    const totalTaskQuantity = Number(task.quantity || 0)
    if (!orderMap.has(orderKey)) {
      orderMap.set(orderKey, {
        workOrderId: task.workOrderId,
        workOrderCode: task.workOrderCode || '--',
        itemLabel: [task.itemCode, task.itemName].filter(Boolean).join(' / ') || '--',
        taskCount: 0,
        dayShiftTaskCount: 0,
        nightShiftTaskCount: 0,
        lockedTaskCount: 0,
        totalQuantity: totalTaskQuantity,
        dailyProcessQuantity: 0,
        processTaskQuantity: 0,
        workshopNames: '',
        lineNames: ''
      })
    }
    const current = orderMap.get(orderKey)!
    current.taskCount += 1
    if (task.shiftCode === 'DAY') {
      current.dayShiftTaskCount += 1
    }
    if (task.shiftCode === 'NIGHT') {
      current.nightShiftTaskCount += 1
    }
    if (task.locked) {
      current.lockedTaskCount += 1
    }
    current.dailyProcessQuantity += dailyQuantity
    current.processTaskQuantity += totalTaskQuantity
    current.totalQuantity = Math.max(current.totalQuantity, totalTaskQuantity)

    const workshopNames = new Set(current.workshopNames ? current.workshopNames.split(' / ') : [])
    const lineNames = new Set(current.lineNames ? current.lineNames.split(' / ') : [])
    if (task.workshopTitle) {
      workshopNames.add(task.workshopTitle)
    }
    if (task.lineNameTitle) {
      lineNames.add(task.lineNameTitle)
    }
    current.workshopNames = [...workshopNames].filter(Boolean).join(' / ')
    current.lineNames = [...lineNames].filter(Boolean).join(' / ')
  })
  return [...orderMap.values()]
})

const selectedDayWorkOrderPlanQuantity = computed(() => {
  return selectedDayWorkOrderRows.value.reduce(
    (total, row) => total + Number(row.totalQuantity || 0),
    0
  )
})

const selectedDayOrderCount = computed(() => {
  return selectedDayWorkOrderRows.value.length
})

const selectedDayLockedCount = computed(() => {
  return selectedDayTasks.value.filter((task) => task.locked).length
})

const daySummaryDialogTitle = computed(() => {
  const titleMap: Record<DaySummaryDialogType, string> = {
    tasks: '任务详情',
    orders: '工单详情',
    dayShift: '白班详情',
    nightShift: '夜班详情',
    issues: '异常详情',
    locked: '锁定详情'
  }
  return `${selectedDate.value} ${titleMap[daySummaryDialogType.value]}`
})

const daySummaryDialogTaskRows = computed<DaySummaryTaskDetailRow[]>(() => {
  switch (daySummaryDialogType.value) {
    case 'tasks':
      return selectedDayTaskRows.value
    case 'dayShift':
      return selectedDayTaskRows.value.filter((task) => task.shiftCode === 'DAY')
    case 'nightShift':
      return selectedDayTaskRows.value.filter((task) => task.shiftCode === 'NIGHT')
    case 'locked':
      return selectedDayTaskRows.value.filter((task) => task.locked)
    default:
      return []
  }
})

const daySummaryTaskWorkOrderGroups = computed<DaySummaryTaskWorkOrderGroup[]>(() => {
  const groupMap = new Map<string, DaySummaryTaskWorkOrderGroup>()
  daySummaryDialogTaskRows.value.forEach((task) => {
    const key = buildDaySummaryTaskWorkOrderKey(task)
    const group = groupMap.get(key)
    const dailyQuantity = resolveTaskDailyQuantity(task)
    const totalTaskQuantity = Number(task.quantity || 0)
    if (group) {
      group.taskCount += 1
      group.dailyProcessQuantity += dailyQuantity
      group.processTaskQuantity += totalTaskQuantity
      group.workOrderPlanQuantity = Math.max(
        group.workOrderPlanQuantity,
        totalTaskQuantity
      )
      if (task.shiftCode === 'DAY') {
        group.dayShiftTaskCount += 1
      }
      if (task.shiftCode === 'NIGHT') {
        group.nightShiftTaskCount += 1
      }
      if (task.locked) {
        group.lockedTaskCount += 1
      }
      const lineNames = new Set(group.lineNames ? group.lineNames.split(' / ') : [])
      if (task.lineNameTitle) {
        lineNames.add(task.lineNameTitle)
      }
      group.lineNames = [...lineNames].filter(Boolean).join(' / ')
      group.rows.push(task)
      return
    }
    groupMap.set(key, {
      key,
      workOrderId: task.workOrderId,
      workOrderCode: task.workOrderCode || '--',
      lineNames: task.lineNameTitle,
      taskCount: 1,
      dayShiftTaskCount: task.shiftCode === 'DAY' ? 1 : 0,
      nightShiftTaskCount: task.shiftCode === 'NIGHT' ? 1 : 0,
      lockedTaskCount: task.locked ? 1 : 0,
      workOrderPlanQuantity: totalTaskQuantity,
      dailyProcessQuantity: dailyQuantity,
      processTaskQuantity: totalTaskQuantity,
      rows: [task]
    })
  })
  return [...groupMap.values()]
})

const selectedDaySummaryTaskRows = computed<DaySummaryTaskDetailRow[]>(() => {
  const group =
    daySummaryTaskWorkOrderGroups.value.find(
      (item) => item.key === selectedDaySummaryWorkOrderKey.value
    ) || daySummaryTaskWorkOrderGroups.value[0]
  return group?.rows || []
})

watch(daySummaryDialogTaskRows, () => resetSelectedDaySummaryWorkOrder(), { immediate: true })
watch(daySummaryDialogType, () => resetSelectedDaySummaryWorkOrder())

const daySummaryDialogOrderRows = computed<DaySummaryWorkOrderDetailRow[]>(() => {
  return daySummaryDialogType.value === 'orders' ? selectedDayWorkOrderRows.value : []
})

const daySummaryDialogIssueRows = computed<ProScheduleCalendarIssueItemVO[]>(() => {
  return daySummaryDialogType.value === 'issues' ? selectedDayScheduleIssues.value : []
})

const workOrderAnalysisDialogTitle = computed(() => {
  const workOrderCode =
    workOrderAnalysis.value?.workOrderCode || workOrderAnalysisRequestedCode.value || '--'
  return `${workOrderCode} 工单产线分析`
})

const workOrderAnalysisProcessRows = computed<ProScheduleCalendarWorkOrderAnalysisProcessVO[]>(
  () => {
    return workOrderAnalysis.value?.processes || []
  }
)

const calendarDayMap = computed(() => {
  const map = new Map<string, ProScheduleCalendarMonthDayVO>()
  monthData.value.days.forEach((item) => {
    map.set(normalizeDate(item.date), {
      ...item,
      date: normalizeDate(item.date)
    })
  })
  return map
})

const calendarCells = computed<CalendarCell[]>(() => {
  const monthStart = currentMonth.value.startOf('month')
  const startOffset = (monthStart.day() + 6) % 7
  const gridStart = monthStart.subtract(startOffset, 'day')
  return Array.from({ length: 42 }, (_, index) => {
    const date = gridStart.add(index, 'day')
    const dateText = date.format('YYYY-MM-DD')
    return {
      date: dateText,
      dayNumber: date.format('D'),
      currentMonth: date.isSame(currentMonth.value, 'month'),
      today: date.isSame(dayjs(), 'day'),
      selected: dateText === selectedDate.value,
      info: activeCalendarDayMap.value.get(dateText)
    }
  })
})

const activeSelectedDayShiftTaskCount = computed(() => {
  return isPreviewCalendarOverlayActive.value
    ? selectedDayTasks.value.filter((task) => task.shiftCode === 'DAY').length
    : (dayDetail.value?.dayShiftTaskCount ?? 0)
})

const activeSelectedDayNightShiftTaskCount = computed(() => {
  return isPreviewCalendarOverlayActive.value
    ? selectedDayTasks.value.filter((task) => task.shiftCode === 'NIGHT').length
    : (dayDetail.value?.nightShiftTaskCount ?? 0)
})

const activeSelectedDayShortageCount = computed(() => {
  return selectedDayShortages.value.length
})

const rulesStatusText = computed(() => {
  if (!rulesDirty.value) {
    return ''
  }
  return autoSchedulePreview.value
    ? '班次规则已变更，请重新生成预览后重新排产'
    : '班次规则已变更，请先保存规则后重新排产'
})

const previewTokenMissing = computed(() => {
  return !!autoSchedulePreview.value && !autoSchedulePreview.value.calendarContextToken
})

const previewStatusText = computed(() => {
  if (!autoSchedulePreview.value) {
    return ''
  }
  if (previewTokenMissing.value) {
    return '当前预览缺少 calendar-context token，无法发布，请先同步后端切片。'
  }
  if (rulesDirty.value) {
    return '班次规则已变更，请重新生成预览后重新排产'
  }
  return ''
})

watch(
  () => [route.query.date, route.query.openShiftEditor],
  async () => {
    const queryDate = resolveCalendarQueryDate()
    if (!queryDate) {
      return
    }
    await openCalendarDateFromQuery(queryDate, shouldOpenShiftEditorFromQuery())
  }
)

onMounted(async () => {
  await initializePage()
})

async function initializePage() {
  await loadRules(true)
  applyCalendarQueryContext()
  await Promise.all([loadMonthCalendar(), loadDayDetail(selectedDate.value)])
  const queryDate = resolveCalendarQueryDate()
  if (queryDate) {
    await openCalendarDateFromQuery(queryDate, shouldOpenShiftEditorFromQuery())
  }
}

function applyCalendarQueryContext() {
  const queryDate = resolveCalendarQueryDate()
  if (!queryDate) {
    return
  }
  selectedDate.value = queryDate
  currentMonth.value = dayjs(queryDate).startOf('month')
  calendarShiftEditorDate.value =
    shouldOpenShiftEditorFromQuery() && canEditCalendarDate(queryDate) ? queryDate : ''
}

async function loadRules(syncSelection = false) {
  rulesLoading.value = true
  try {
    const data = await ProScheduleCalendarApi.getRules()
    Object.assign(rulesForm, defaultRules(), data)
    rulesForm.simulationCurrentDate = normalizeDate(data.simulationCurrentDate)
    rulesSnapshot.value = JSON.stringify(buildRulesPayload())
    if (syncSelection && rulesForm.simulationCurrentDate) {
      selectedDate.value = rulesForm.simulationCurrentDate
      currentMonth.value = dayjs(rulesForm.simulationCurrentDate).startOf('month')
    }
  } finally {
    rulesLoading.value = false
  }
}

async function loadMonthCalendar() {
  monthLoading.value = true
  monthErrorMessage.value = ''
  try {
    const data = await ProScheduleCalendarApi.getMonthCalendar({
      month: currentMonth.value.format('YYYY-MM')
    })
    monthData.value = {
      ...data,
      simulationCurrentDate: normalizeDate(data.simulationCurrentDate),
      days: (data.days || []).map((item) => ({
        ...item,
        date: normalizeDate(item.date)
      }))
    }
    if (monthData.value.simulationCurrentDate) {
      rulesForm.simulationCurrentDate = monthData.value.simulationCurrentDate
    }
    clearCalendarRecovery('month')
  } catch (error) {
    const recovery = buildCalendarRecoveryState(error, {
      area: 'month',
      action: '加载月排程',
      objectLabel: currentMonth.value.format('YYYY年MM月'),
      impactScope: '月视图无法确认正式排程状态，不能据此判断可发布性。'
    })
    monthData.value = defaultMonthData()
    monthData.value.month = currentMonth.value.format('YYYY-MM')
    monthErrorMessage.value = recovery.title
    calendarRecoveryState.value = recovery
  } finally {
    monthLoading.value = false
  }
}

async function loadDayDetail(date: string) {
  detailLoading.value = true
  detailErrorMessage.value = ''
  try {
    const data = await ProScheduleCalendarApi.getDayDetail({ date })
    dayDetail.value = {
      ...data,
      date: normalizeDate(data.date),
      simulationCurrentDate: normalizeDate(data.simulationCurrentDate),
      workshops: data.workshops || [],
      materialShortageSummary: data.materialShortageSummary || {
        shortageCount: 0,
        totalShortageQty: 0,
        items: []
      },
      scheduleIssueSummary: data.scheduleIssueSummary || {
        openIssueCount: 0,
        blockingIssueCount: 0,
        items: []
      },
      processCapacitySummary: data.processCapacitySummary || {
        processCount: 0,
        totalMaxCapacity: 0,
        totalScheduledQuantity: 0,
        totalRemainingCapacity: 0,
        items: []
      }
    }
    clearCalendarRecovery('detail')
  } catch (error) {
    const recovery = buildCalendarRecoveryState(error, {
      area: 'detail',
      action: '加载日详情',
      objectLabel: date,
      impactScope: '当日任务、工单、短缺和锁定状态不可用于演练判断。'
    })
    dayDetail.value = null
    detailErrorMessage.value = recovery.title
    calendarRecoveryState.value = recovery
  } finally {
    detailLoading.value = false
  }
}

async function saveRules() {
  rulesSaving.value = true
  try {
    await ProScheduleCalendarApi.updateRules(buildRulesPayload())
    calendarShiftEditorDate.value = ''
    clearPreview()
    message.success('排程规则已更新，请重新生成预览后再发布排产')
    await Promise.all([loadRules(), loadMonthCalendar(), loadDayDetail(selectedDate.value)])
  } finally {
    rulesSaving.value = false
  }
}

async function handleGenerateCapacityPlans() {
  capacityGenerateLoading.value = true
  try {
    const summary = await ProScheduleCalendarApi.generateCapacityPlans({
      startDate: simulationDateLabel.value,
      days: capacityGenerateDays.value
    })
    capacityGenerateSummary.value = summary
    message.success(capacityGenerateSummaryText.value)
    await Promise.all([loadMonthCalendar(), loadDayDetail(selectedDate.value)])
  } catch (error) {
    message.error(extractErrorMessage(error))
  } finally {
    capacityGenerateLoading.value = false
  }
}

async function selectCalendarDate(date: string) {
  const nextDate = normalizeDate(date)
  const nextMonth = dayjs(nextDate).startOf('month')
  activeSidebarTab.value = 'detail'
  calendarShiftEditorDate.value = ''
  selectedDate.value = nextDate
  if (!nextMonth.isSame(currentMonth.value, 'month')) {
    currentMonth.value = nextMonth
    await loadMonthCalendar()
  }
  await loadDayDetail(nextDate)
}

async function openCalendarMetricDetail(date: string, type: CalendarMetricDialogType) {
  await selectCalendarDate(date)
  if (type === 'shortages') {
    openShortageDialog('短缺明细', selectedDayShortages.value)
    return
  }
  showDaySummaryDialog(type)
}

async function openCalendarDateFromQuery(date: string, openShiftEditor: boolean) {
  const nextDate = normalizeDate(date)
  if (!nextDate) {
    return
  }
  const nextMonth = dayjs(nextDate).startOf('month')
  selectedDate.value = nextDate
  if (!nextMonth.isSame(currentMonth.value, 'month')) {
    currentMonth.value = nextMonth
    await loadMonthCalendar()
  }
  calendarShiftEditorDate.value = openShiftEditor && canEditCalendarDate(nextDate) ? nextDate : ''
  if (!dayDetail.value || dayDetail.value.date !== nextDate) {
    await loadDayDetail(nextDate)
  }
}

async function changeMonth(offset: number) {
  currentMonth.value = currentMonth.value.add(offset, 'month').startOf('month')
  selectedDate.value = currentMonth.value.format('YYYY-MM-DD')
  calendarShiftEditorDate.value = ''
  await Promise.all([loadMonthCalendar(), loadDayDetail(selectedDate.value)])
}

async function goToCurrentMonth() {
  currentMonth.value = dayjs().startOf('month')
  selectedDate.value = dayjs().format('YYYY-MM-DD')
  calendarShiftEditorDate.value = ''
  await Promise.all([loadMonthCalendar(), loadDayDetail(selectedDate.value)])
}

async function toggleTaskLock(task: ProScheduleCalendarTaskVO) {
  if (!task.taskId) {
    return
  }
  taskActionLoadingId.value = task.taskId
  try {
    if (task.locked) {
      await message.confirm('确认解锁当前任务吗？')
      await ProTaskApi.unlockTask(task.taskId)
      message.success('任务已解锁')
    } else {
      const result = await message.prompt('请输入锁定原因', '锁定任务')
      await ProTaskApi.lockTask({
        taskId: task.taskId,
        lockedReason: result.value || 'MANUAL_LOCK'
      })
      message.success('任务已锁定')
    }
    clearPreview()
    calendarShiftEditorDate.value = ''
    await Promise.all([loadMonthCalendar(), loadDayDetail(selectedDate.value)])
  } finally {
    taskActionLoadingId.value = null
  }
}

function buildRulesPayload(): ProScheduleCalendarRulesUpdateReqVO {
  return {
    skipStatutoryHolidays: rulesForm.skipStatutoryHolidays,
    weekendRestMode: rulesForm.weekendRestMode,
    dateShiftModeByDate: { ...(rulesForm.dateShiftModeByDate || {}) }
  }
}

function clearPreview() {
  autoSchedulePreview.value = null
  previewMaterialDemandByWorkOrderId.value = {}
}

function backToTaskPage() {
  push({ name: 'MesProTask' })
}

function openShortageDialog(title: string, rows: ShortageDialogRow[]) {
  shortageDialogTitle.value = title
  shortageDialogRows.value = [...rows]
  shortageDialogVisible.value = true
}

function openIssueCreateDialog() {
  const firstOrder = selectedDayWorkOrderRows.value[0]
  issueCreateForm.issueType = 'MANUAL_EXCEPTION'
  issueCreateForm.severity = 'BLOCKING'
  issueCreateForm.workOrderId = firstOrder?.workOrderId
  issueCreateForm.message = ''
  issueCreateDialogVisible.value = true
}

function openIssueResolveDialog() {
  const firstIssue = selectedDayScheduleIssues.value[0]
  issueResolveForm.id = firstIssue?.id
  issueResolveForm.resolutionReason = ''
  issueResolveDialogVisible.value = true
}

async function submitIssueCreate() {
  if (!issueCreateForm.message.trim()) {
    message.error('请填写异常说明')
    return
  }
  issueActionLoading.value = true
  try {
    await ProTaskAutoScheduleApi.createIssue({
      issueType: issueCreateForm.issueType,
      severity: issueCreateForm.severity,
      workOrderId: issueCreateForm.workOrderId,
      occurredAt: selectedDate.value,
      sourceType: 'CALENDAR',
      message: issueCreateForm.message.trim()
    })
    issueCreateDialogVisible.value = false
    clearPreview()
    await Promise.all([loadMonthCalendar(), loadDayDetail(selectedDate.value)])
    message.success('异常已登记，请重新生成预览或执行重排')
  } finally {
    issueActionLoading.value = false
  }
}

async function cancelNightShiftTask(row: DaySummaryTaskDetailRow) {
  if (!row.taskId) {
    return
  }
  const result = await message.prompt('确认取消该夜班任务？', '取消夜班')
  await ProTaskAutoScheduleApi.cancelNightShift({
    taskId: row.taskId,
    reason: result.value || 'MANUAL_CANCEL'
  })
  clearPreview()
  await Promise.all([loadMonthCalendar(), loadDayDetail(selectedDate.value)])
  message.success('夜班任务已取消')
}

async function submitIssueResolve() {
  if (!issueResolveForm.id) {
    message.error('请选择要关闭的异常')
    return
  }
  if (!issueResolveForm.resolutionReason.trim()) {
    message.error('请填写关闭原因')
    return
  }
  issueActionLoading.value = true
  try {
    await ProTaskAutoScheduleApi.resolveIssue({
      id: issueResolveForm.id,
      resolutionReason: issueResolveForm.resolutionReason.trim()
    })
    issueResolveDialogVisible.value = false
    clearPreview()
    await Promise.all([loadMonthCalendar(), loadDayDetail(selectedDate.value)])
    message.success('异常已关闭，请按需重新生成预览或重排')
  } finally {
    issueActionLoading.value = false
  }
}

function getDaySummaryCardCount(type: DaySummaryCardType) {
  switch (type) {
    case 'tasks':
      return selectedDayTasks.value.length
    case 'orders':
      return selectedDayOrderCount.value
    case 'dayShift':
      return activeSelectedDayShiftTaskCount.value
    case 'nightShift':
      return activeSelectedDayNightShiftTaskCount.value
    case 'shortages':
      return selectedDayShortages.value.length
    case 'materials':
      return activeSelectedDayMaterialCount.value
    case 'issues':
      return selectedDayOpenIssueCount.value
    case 'locked':
      return selectedDayLockedCount.value
    default:
      return 0
  }
}

async function loadPreviewMaterialDemandMap(workOrderIds: number[]) {
  const uniqueIds = [...new Set((workOrderIds || []).filter((id) => Number.isFinite(id)))]
  const entries = await Promise.all(
    uniqueIds.map(async (workOrderId) => {
      const items = await ProWorkOrderBomApi.getWorkOrderBomItemListByWorkOrderId(workOrderId)
      return [workOrderId, items || []] as const
    })
  )
  previewMaterialDemandByWorkOrderId.value = Object.fromEntries(entries)
}

async function refreshPreviewMaterialDemand() {
  const workOrderIds = [
    ...new Set(
      (autoSchedulePreview.value?.tasks || [])
        .map((task) => resolvePreviewWorkOrderId(task as PreviewTaskSource))
        .filter((workOrderId): workOrderId is number => typeof workOrderId === 'number')
    )
  ]
  const request = { workOrderIds }
  await loadPreviewMaterialDemandMap(request.workOrderIds)
}

function canEditCalendarDate(dateText: string) {
  return normalizeDate(dateText) >= todayDateLabel.value
}

function hasCalendarShiftOverride(dateText: string) {
  return Boolean(rulesForm.dateShiftModeByDate?.[normalizeDate(dateText)])
}

function resolveEffectiveCalendarShiftMode(dateText: string): ProScheduleCalendarDateShiftMode {
  return (
    rulesForm.dateShiftModeByDate?.[normalizeDate(dateText)] ||
    activeCalendarDayMap.value.get(normalizeDate(dateText))?.dateShiftMode ||
    'DAY'
  )
}

function canToggleCalendarShiftMode(dateText: string) {
  return canEditCalendarDate(dateText)
}

function resolveCalendarShiftToggleMode(dateText: string): ProScheduleCalendarDateShiftMode {
  const currentMode = resolveEffectiveCalendarShiftMode(dateText)
  return currentMode === 'REST' ? 'DAY' : 'REST'
}

function resolveCalendarShiftToggleLabel(dateText: string) {
  const currentMode = resolveEffectiveCalendarShiftMode(dateText)
  return currentMode === 'REST' ? '上班' : '休息'
}

function activeSelectedDayDateShiftModeForCell(dateText: string) {
  return (
    rulesForm.dateShiftModeByDate?.[normalizeDate(dateText)] ||
    activeCalendarDayMap.value.get(normalizeDate(dateText))?.dateShiftMode
  )
}

function toggleCalendarShiftMode(dateText: string) {
  if (!canToggleCalendarShiftMode(dateText)) {
    return
  }
  const nextMode = resolveCalendarShiftToggleMode(dateText)
  applyCalendarShiftMode(dateText, nextMode)
}

function applyCalendarShiftMode(dateText: string, mode: ProScheduleCalendarDateShiftMode) {
  if (!canEditCalendarDate(dateText)) {
    return
  }
  rulesForm.dateShiftModeByDate = {
    ...rulesForm.dateShiftModeByDate,
    [normalizeDate(dateText)]: mode
  }
  calendarShiftEditorDate.value = ''
}

function clearCalendarShiftMode(dateText: string) {
  const normalizedDate = normalizeDate(dateText)
  if (!hasCalendarShiftOverride(normalizedDate)) {
    return
  }
  const next = { ...rulesForm.dateShiftModeByDate }
  delete next[normalizedDate]
  rulesForm.dateShiftModeByDate = next
  calendarShiftEditorDate.value = ''
}

function resolveCalendarEditTooltip(dateText: string) {
  return canEditCalendarDate(dateText) ? '点击设置班次' : '仅可修改今天及未来日期'
}

function isDaySummaryCardClickable(type: DaySummaryCardType) {
  return getDaySummaryCardCount(type) > 0
}

function showDaySummaryDialog(type: DaySummaryDialogType) {
  daySummaryDialogType.value = type
  daySummaryDialogVisible.value = true
}

function openDaySummaryDetail(type: DaySummaryCardType) {
  if (!isDaySummaryCardClickable(type)) {
    return
  }
  if (type === 'materials') {
    openMaterialDialog('total')
    return
  }
  if (type === 'shortages') {
    openShortageDialog('短缺明细', selectedDayShortages.value)
    return
  }
  showDaySummaryDialog(type)
}

const materialDialogTitle = computed(() => {
  return `${selectedDate.value} 物料需求`
})

function openMaterialDialog(tab: MaterialDialogTab = 'total') {
  materialDialogTab.value = tab
  materialDialogVisible.value = true
}

async function openWorkOrderAnalysis(workOrderId?: number, workOrderCode?: string) {
  if (!workOrderId) {
    return
  }
  workOrderAnalysisVisible.value = true
  workOrderAnalysisRequestedCode.value = workOrderCode || ''
  workOrderAnalysisErrorMessage.value = ''
  const previewAnalysis = previewWorkOrderAnalysisMap.value.get(workOrderId)
  if (previewAnalysis) {
    workOrderAnalysis.value = previewAnalysis
    return
  }
  workOrderAnalysis.value = null
  workOrderAnalysisLoading.value = true
  try {
    workOrderAnalysis.value = await ProScheduleCalendarApi.getWorkOrderAnalysis({ workOrderId })
    clearCalendarRecovery('work-order-analysis')
  } catch (error) {
    const recovery = buildCalendarRecoveryState(error, {
      area: 'work-order-analysis',
      action: '加载工单产线分析',
      objectLabel: workOrderCode || `工单#${workOrderId}`,
      impactScope: '无法确认该工单的工序资源、工作站和瓶颈归因。'
    })
    workOrderAnalysisErrorMessage.value = recovery.title
    calendarRecoveryState.value = recovery
  } finally {
    workOrderAnalysisLoading.value = false
  }
}

function openWorkOrderDetail(workOrderId?: number, workOrderCode?: string) {
  if (!workOrderId) {
    return
  }
  push({
    name: 'MesProWorkOrder',
    query: {
      code: workOrderCode || undefined,
      openId: String(workOrderId)
    }
  })
}

function openRouteDetail(routeId?: number, routeName?: string) {
  if (!routeId) {
    return
  }
  push({
    name: 'MesProRoute',
    query: {
      name: routeName || undefined,
      openId: String(routeId)
    }
  })
}

function openScheduleRouteRecovery() {
  if (!calendarRecoveryState.value) {
    throw new Error('排程日历恢复入口缺少错误上下文。')
  }
  push({
    path: '/mes/pro/route',
    query: {
      ...calendarRecoveryState.value.recoveryQuery,
      routeId: calendarRecoveryState.value.recoveryQuery.routeId,
      tab: 'flow'
    }
  })
}

function shouldShowCalendarRecovery(area: CalendarRecoveryArea) {
  return calendarRecoveryState.value?.area === area
}

function clearCalendarRecovery(area: CalendarRecoveryArea) {
  if (calendarRecoveryState.value?.area === area) {
    calendarRecoveryState.value = null
  }
}

function buildCalendarCellClass(cell: CalendarCell) {
  return {
    'is-outside': !cell.currentMonth,
    'is-today': cell.today,
    'is-selected': cell.selected,
    'is-shortage': !!cell.info?.shortageCount,
    'is-rest': cell.info?.dateShiftMode === 'REST',
    'is-editable': canEditCalendarDate(cell.date),
    'is-readonly-past': !canEditCalendarDate(cell.date),
    'has-manual-shift': hasCalendarShiftOverride(cell.date)
  }
}

function buildShiftModeLabel(mode?: ProScheduleCalendarDateShiftMode | string) {
  switch (mode) {
    case 'REST':
      return '休息'
    case 'DAY':
      return '白班'
    default:
      return '--'
  }
}

function buildWorkshopTitle(workshop: ProScheduleCalendarWorkshopVO) {
  return [workshop.workshopCode, workshop.workshopName].filter(Boolean).join(' / ') || '未命名车间'
}

function buildLineTitle(line: ProScheduleCalendarLineVO) {
  return [line.lineCode, line.lineName].filter(Boolean).join(' / ') || '未命名产线'
}

function buildLineNameTitle(line: ProScheduleCalendarLineVO) {
  return line.lineName || '--'
}

function buildTaskKey(task: ProScheduleCalendarTaskVO) {
  return task.taskId || `${task.taskCode || task.workOrderCode}-${task.startTime || ''}`
}

function buildDaySummaryTaskWorkOrderKey(task: DaySummaryTaskDetailRow) {
  return String(task.workOrderId || task.workOrderCode || buildTaskKey(task))
}

function selectDaySummaryWorkOrder(key: string) {
  selectedDaySummaryWorkOrderKey.value = key
}

function resetSelectedDaySummaryWorkOrder() {
  selectedDaySummaryWorkOrderKey.value = daySummaryTaskWorkOrderGroups.value[0]?.key || ''
}

function resolveTaskDailyQuantity(task: ProScheduleCalendarTaskVO) {
  if (task.dailyQuantity !== undefined && task.dailyQuantity !== null) {
    return Number(task.dailyQuantity || 0)
  }
  return Number(task.quantity || 0)
}

function buildTaskProcessLabel(task: ProScheduleCalendarTaskVO) {
  return task.processName || '--'
}

function buildTaskProductCodeLabel(task: ProScheduleCalendarTaskVO) {
  return task.itemCode || '--'
}

function buildTaskProductNameLabel(task: ProScheduleCalendarTaskVO) {
  return task.itemName || '--'
}

function buildTaskLineNameLabel(task: DaySummaryTaskDetailRow) {
  return task.lineNameTitle || '--'
}

function buildTaskExecutionStatusText(task: ProScheduleCalendarTaskVO) {
  const textMap: Record<string, string> = {
    PREVIEW: '预览',
    NOT_STARTED: '未开始',
    IN_PROGRESS: '执行中',
    PENDING_INSPECTION: '待检',
    COMPLETED: '已完成',
    FROZEN: '已冻结'
  }
  return textMap[task.executionStatus || ''] || '未知'
}

function buildTaskExecutionStatusTag(task: ProScheduleCalendarTaskVO) {
  const tagMap: Record<string, string> = {
    PREVIEW: 'info',
    NOT_STARTED: 'info',
    IN_PROGRESS: 'warning',
    PENDING_INSPECTION: 'warning',
    COMPLETED: 'success',
    FROZEN: 'danger'
  }
  return tagMap[task.executionStatus || ''] || 'info'
}

function buildTaskFreezeText(task: ProScheduleCalendarTaskVO) {
  if (!task.scheduleOrderFrozen) {
    return '未冻结'
  }
  return task.scheduleOrderFreezeReason ? `已冻结：${task.scheduleOrderFreezeReason}` : '已冻结'
}

function buildWorkOrderAnalysisProductLabel() {
  return (
    [workOrderAnalysis.value?.productCode, workOrderAnalysis.value?.productName]
      .filter(Boolean)
      .join(' / ') || '--'
  )
}

function buildWorkOrderAnalysisLineLabel() {
  return workOrderAnalysis.value?.lineName || '--'
}

function buildWorkOrderAnalysisBottleneckLabel() {
  if (!workOrderAnalysis.value) {
    return '--'
  }
  return (
    [
      workOrderAnalysis.value.bottleneckProcessName,
      buildCapacityLabel(workOrderAnalysis.value.bottleneckHourlyCapacity, '件/小时')
    ]
      .filter(Boolean)
      .join(' / ') || '--'
  )
}

function buildProcessWorkstationLabel(process: ProScheduleCalendarWorkOrderAnalysisProcessVO) {
  return (process.workstationNames || []).filter(Boolean).join(' / ') || '--'
}

function buildCapacitySourceLabel(source?: string) {
  return source === 'MACHINE' ? '设备' : '人力'
}

function buildQuantityLabel(value?: number, suffix = '') {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '--'
  }
  return `${Math.round(Number(value))}${suffix}`
}

function buildCapacityLabel(value?: number, suffix = '') {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '--'
  }
  return `${Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 })}${suffix}`
}

function buildPercentLabel(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '--'
  }
  return `${Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}%`
}

function buildProcessCapacityStatusType(row: ProScheduleCalendarProcessCapacityItemVO) {
  const maxCapacity = Number(row.maxCapacity || 0)
  const utilizationRate = Number(row.utilizationRate || 0)
  if (maxCapacity <= 0) {
    return 'info'
  }
  if (utilizationRate > 100 || isProcessCapacityOverrun(row)) {
    return 'danger'
  }
  if (utilizationRate >= 95) {
    return 'success'
  }
  if (utilizationRate >= 80) {
    return 'warning'
  }
  return 'danger'
}

function isProcessCapacityOverrun(row: ProScheduleCalendarProcessCapacityItemVO) {
  const overCapacity = Number(row.overCapacity || 0)
  if (overCapacity > 0) {
    return true
  }
  const maxCapacity = Number(row.maxCapacity || 0)
  const scheduledQuantity = Number(row.scheduledQuantity || 0)
  return maxCapacity > 0 && scheduledQuantity > maxCapacity
}

function buildTaskTimeRange(task: ProScheduleCalendarTaskVO) {
  const start = task.startTime ? dayjs(task.startTime).format('HH:mm') : '--:--'
  const end = task.endTime ? dayjs(task.endTime).format('HH:mm') : '--:--'
  return `${start} - ${end}`
}

function buildMaterialCodeLabel(row: ShortageDialogRow) {
  return row.materialCode || '--'
}

function buildMaterialNameLabel(row: ShortageDialogRow) {
  return row.materialName || '--'
}

function resolvePreviewWorkOrderId(task: PreviewTaskSource) {
  const idText = task.parent || task.id || ''
  const parentMatch = idText.match(/(?:301_|\d+_preview_)(\d+)(?:_|$)/)
  return parentMatch ? Number(parentMatch[1]) : undefined
}

function resolvePreviewWorkOrderCode(workOrderId?: number) {
  if (!workOrderId) {
    return '--'
  }
  return previewWorkOrderCodeById.value.get(workOrderId) || `工单#${workOrderId}`
}

function resolvePreviewTaskLineName(workOrderId?: number) {
  if (!workOrderId) {
    return '--'
  }
  const analysisLineName = previewWorkOrderAnalysisMap.value.get(workOrderId)?.lineName
  return analysisLineName || '--'
}

function resolvePreviewParentProductName(parentTask?: PreviewTaskSource) {
  if (!parentTask?.text) {
    return ''
  }
  const quantityText = Math.round(Number(parentTask.quantity || 0)).toString()
  return quantityText && parentTask.text.endsWith(quantityText)
    ? parentTask.text.slice(0, -quantityText.length)
    : parentTask.text
}

function inferPreviewShiftCode(value?: string | number | Date) {
  const parsed = dayjs(value)
  if (!parsed.isValid()) {
    return undefined
  }
  const hour = parsed.hour()
  return hour >= 20 || hour < 8 ? 'NIGHT' : 'DAY'
}

function normalizeDateTimeText(value?: string | number | Date) {
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : ''
}

function buildDateKeysForTask(startTime?: string, endTime?: string) {
  const start = dayjs(startTime)
  const end = dayjs(endTime || startTime)
  if (!start.isValid() || !end.isValid()) {
    return []
  }
  const keys: string[] = []
  let cursor = start.startOf('day')
  const endDay = end.startOf('day')
  while (cursor.isBefore(endDay) || cursor.isSame(endDay, 'day')) {
    keys.push(cursor.format('YYYY-MM-DD'))
    cursor = cursor.add(1, 'day')
  }
  return keys
}

function normalizeDate(value?: string) {
  if (!value) {
    return ''
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : String(value).slice(0, 10)
}

function isWeekend(dateText: string) {
  const weekday = dayjs(dateText).day()
  return weekday === 0 || weekday === 6
}

function extractErrorMessage(error: unknown) {
  const fallback = '排程日历加载失败'
  if (!error || typeof error !== 'object') {
    return fallback
  }
  const maybeMessage = (error as { message?: string }).message
  if (maybeMessage) {
    return maybeMessage
  }
  const maybeResponseMessage = (error as { response?: { data?: { msg?: string } } }).response?.data
    ?.msg
  return maybeResponseMessage || fallback
}

function buildCalendarRecoveryState(
  error: unknown,
  context: {
    area: CalendarRecoveryArea
    action: string
    objectLabel: string
    impactScope: string
    routeId?: number | string
    routeProcessId?: number | string
    processCode?: string
    processName?: string
  }
): CalendarRecoveryState {
  const rawMessage = extractErrorMessage(error)
  const workstationMissing = rawMessage.includes('工作站不存在')
  const recovery = workstationMissing ? 'workstation-missing' : 'route-flow-schedule-check'
  const recoveryHint = workstationMissing
    ? '去流转关系图工序设置核对工作站、设备、人员和班次产能。'
    : '检查流转关系图工序设置、工作站资源和排产前检查问题清单。'
  const recoveryButtonText = '打开流转关系图工序设置'
  return {
    area: context.area,
    title: `${context.action}失败：${rawMessage}`,
    action: context.action,
    objectLabel: context.objectLabel,
    impactScope: context.impactScope,
    recoveryHint,
    recoveryButtonText,
    recoveryQuery: {
      recovery,
      source: 'schedule-calendar',
      area: context.area,
      routeId: context.routeId ? String(context.routeId) : undefined,
      routeProcessId: context.routeProcessId ? String(context.routeProcessId) : undefined,
      processCode: context.processCode || undefined,
      processName: context.processName || undefined
    },
    rawMessage
  }
}
</script>

<style scoped>
.schedule-calendar-page {
  color: #172033;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) 420px;
  gap: 16px;
  align-items: start;
}

.calendar-column,
.sidebar-column {
  min-width: 0;
}

.sidebar-column {
  display: grid;
  gap: 16px;
}

.sidebar-tabs {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
}

.sidebar-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 14px;
  border-bottom: 1px solid #e5ebf3;
  background: #f7f9fc;
}

.sidebar-tabs :deep(.el-tabs__item) {
  height: 44px;
  color: #4b5563;
  font-weight: 600;
}

.sidebar-tabs :deep(.el-tabs__item.is-active) {
  color: #1677ff;
}

.sidebar-tabs :deep(.el-tabs__active-bar) {
  background-color: #1677ff;
}

.sidebar-tabs :deep(.el-tabs__content) {
  padding: 12px;
}

.sidebar-tabs .panel-shell {
  border-radius: 6px;
}

.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
  border-bottom: 0;
  background: #fff;
}

.toolbar-block {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.toolbar-block-left {
  flex: 0 0 auto;
}

.toolbar-block-right {
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar-title-group {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.toolbar-title-group h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
}

.toolbar-title-group p,
.panel-head p {
  margin: 0;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.month-switch,
.toolbar-stat-strip,
.action-row,
.simulation-grid,
.advance-days-box,
.calendar-cell-head,
.calendar-cell-foot,
.section-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-stat-strip {
  flex-wrap: wrap;
}

.toolbar-stat,
.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fafc;
  color: #263247;
  white-space: nowrap;
}

.toolbar-stat.warning,
.summary-chip.warning,
.calendar-cell.is-shortage .metric-inline {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #c2410c;
}

.toolbar-stat label,
.status-chip label,
.summary-chip label,
.calendar-metric-item label {
  color: #4b5563;
  font-size: 12px;
  line-height: 1;
}

.toolbar-stat strong,
.status-chip strong,
.summary-chip strong,
.calendar-metric-item strong {
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
}

.calendar-shell,
.panel-shell {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 0 0 8px 8px;
  background: #fff;
}

.panel-shell {
  border-radius: 8px;
}

.calendar-alert {
  margin: 16px 16px 0;
}

.calendar-recovery-card {
  display: grid;
  gap: 4px;
  margin: 10px 16px 0;
  border: 1px solid #f0c9c9;
  border-radius: 6px;
  background: #fff8f8;
  padding: 10px 12px;
}

.calendar-recovery-card span {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.calendar-status-strip {
  display: flex;
  flex: 1 1 360px;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  min-width: 280px;
  padding: 0;
}

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  margin-top: 0;
  border-top: 1px solid #e5ebf3;
  border-bottom: 1px solid #e5ebf3;
  background: #f7f9fc;
}

.weekday-row span {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #46546a;
  font-size: 13px;
  font-weight: 600;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  grid-auto-rows: minmax(134px, 1fr);
}

.calendar-cell {
  display: grid;
  gap: 10px;
  align-content: start;
  position: relative;
  min-height: 134px;
  padding: 12px;
  border: 0;
  border-right: 1px solid #edf1f6;
  border-bottom: 1px solid #edf1f6;
  background: #fff;
  text-align: left;
  transition:
    background-color 0.16s ease,
    box-shadow 0.16s ease;
}

.calendar-cell:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: -2px;
}

.calendar-cell:nth-child(7n) {
  border-right: 0;
}

.calendar-cell:hover {
  background: #fafcff;
}

.calendar-cell.is-editable {
  cursor: pointer;
}

.calendar-cell.is-editable:hover {
  background: #f5f9ff;
  box-shadow: inset 0 0 0 1px #dbeafe;
}

.calendar-cell.is-readonly-past {
  cursor: default;
  background: #f3f4f6;
}

.calendar-cell.is-readonly-past:hover {
  background: #f3f4f6;
}

.calendar-cell.is-selected {
  background: #f5f9ff;
  box-shadow: inset 0 0 0 2px #1677ff;
}

.calendar-cell.is-outside {
  background: #fcfdff;
}

.calendar-cell.is-outside .calendar-day-number {
  color: #9ca3af;
}

.calendar-cell.is-rest {
  background: #f9fafb;
}

.calendar-cell.has-manual-shift {
  box-shadow: inset 0 0 0 1px #93c5fd;
}

.calendar-cell-head {
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 6px 8px;
}

.calendar-day-meta {
  display: flex;
  flex: 1 1 72px;
  min-width: 0;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.calendar-day-number {
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
}

.calendar-day-badge {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 7px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.calendar-day-badge.is-primary {
  background: #e8f3ff;
  color: #1677ff;
}

.calendar-day-badge.is-warning {
  background: #fff7ed;
  color: #c2410c;
}

.calendar-day-badge.is-muted {
  background: #f3f4f6;
  color: #6b7280;
}

.calendar-day-badge.is-editable {
  background: #eef6ff;
  color: #2563eb;
}

.calendar-day-badge.is-override {
  background: #e8fff4;
  color: #059669;
}

.calendar-mode-text {
  flex: 0 0 auto;
  max-width: 100%;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
  line-height: 22px;
}

.calendar-metric-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.calendar-metric-item,
.summary-chip {
  display: grid;
  gap: 6px;
  box-sizing: border-box;
  min-width: 0;
  max-width: 100%;
  min-height: 58px;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #fafcff;
}

.calendar-metric-button {
  width: 100%;
  text-align: left;
  color: inherit;
  font: inherit;
  appearance: none;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    background-color 0.16s ease,
    box-shadow 0.16s ease;
}

.calendar-metric-button:hover {
  border-color: #bfdbfe;
  background: #f5f9ff;
  box-shadow: inset 0 0 0 1px #dbeafe;
}

.calendar-metric-button:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 1px;
}

.calendar-metric-item-readonly {
  cursor: default;
}

.summary-chip-button {
  width: 100%;
  text-align: left;
  color: inherit;
  font: inherit;
  appearance: none;
}

.summary-chip-button:disabled {
  cursor: default;
  opacity: 1;
}

.summary-chip-button.is-clickable {
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    background-color 0.16s ease,
    box-shadow 0.16s ease;
}

.process-capacity-section {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
}

.process-capacity-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.process-capacity-list {
  display: grid;
  gap: 8px;
}

.process-capacity-row {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 10px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #fafcff;
}

.process-capacity-row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.process-capacity-row-head strong {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-capacity-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.process-capacity-metrics span {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 8px;
  border-radius: 6px;
  background: #f7f9fc;
}

.process-capacity-metrics span.is-overrun {
  border: 1px solid #f5c2c7;
  background: #fff5f5;
}

.process-capacity-metrics span.is-overrun label,
.process-capacity-metrics span.is-overrun strong {
  color: #b42318;
}

.process-capacity-metrics label {
  color: #4b5563;
  font-size: 12px;
  line-height: 1;
}

.process-capacity-metrics strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #172033;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
}

.shift-rule-hint {
  margin: 10px 2px 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}

.material-dialog {
  display: grid;
  gap: 12px;
}

.material-dialog-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.material-dialog-summary span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fafc;
  color: #263247;
}

.material-dialog-summary span.warning {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #c2410c;
}

.material-dialog-summary label {
  color: #4b5563;
  font-size: 12px;
}

.material-dialog-summary strong {
  font-size: 15px;
  font-weight: 700;
}

.summary-chip-button.is-clickable:hover {
  border-color: #bfdbfe;
  background: #f5f9ff;
  box-shadow: inset 0 0 0 1px #dbeafe;
}

.calendar-cell-foot {
  justify-content: flex-end;
  gap: 8px;
}

.calendar-shift-editor {
  position: absolute;
  inset: 10px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  align-content: start;
  padding: 10px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 8px 18px rgba(23, 32, 51, 0.12);
  z-index: 2;
}

.calendar-shift-editor__option {
  min-height: 34px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fbff;
  color: #263247;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    background-color 0.16s ease,
    color 0.16s ease;
}

.calendar-shift-editor__option:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.calendar-shift-editor__option.is-active {
  border-color: #1677ff;
  background: #e8f3ff;
  color: #1677ff;
}

.calendar-shift-editor__option.is-clear {
  grid-column: 1 / -1;
  background: #fff7ed;
  border-color: #fed7aa;
  color: #c2410c;
}

.metric-inline {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  color: #4b5563;
  font-size: 12px;
  font-weight: 600;
}

.calendar-shortage-button {
  justify-content: center;
  flex: 1;
}

.calendar-shift-toggle-button {
  justify-content: center;
  flex: 1;
  border-color: #dbe3ef;
  background: #f7f9fc;
  color: #2563eb;
}

.calendar-shift-toggle-button:hover {
  border-color: #bfdbfe;
  background: #eef6ff;
}

.calendar-cell.is-rest .calendar-shift-toggle-button {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #c2410c;
}

.calendar-cell.is-rest .calendar-shift-toggle-button:hover {
  background: #ffedd5;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #e5ebf3;
  background: #fff;
}

.panel-head h3 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.2;
}

.panel-body {
  display: grid;
  gap: 16px;
  padding: 16px;
}

.setting-grid,
.preview-summary-grid,
.detail-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
  max-width: 100%;
}

.detail-total-quantity-card {
  grid-column: 1 / -1;
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  box-sizing: border-box;
  min-width: 0;
  max-width: 100%;
  padding: 14px 16px;
  border: 1px solid #dbe3ef;
  border-left: 4px solid #1677ff;
  border-radius: 8px;
  background: #fafcff;
}

.detail-total-quantity-card label {
  flex: 1 1 96px;
  min-width: 0;
  color: #4b5563;
  font-size: 13px;
  font-weight: 700;
}

.detail-total-quantity-card strong {
  flex: 0 1 auto;
  min-width: 0;
  max-width: 100%;
  color: #1677ff;
  font-size: 32px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  overflow-wrap: anywhere;
  text-align: right;
  word-break: break-word;
}

.detail-total-quantity-card span {
  flex: 0 0 auto;
  padding-bottom: 2px;
  color: #4b5563;
  font-size: 14px;
  font-weight: 700;
}

.detail-total-quantity-card small {
  flex: 1 1 100%;
  color: #4b5563;
  font-size: 12px;
  font-weight: 600;
}

.section-head {
  justify-content: space-between;
}

.section-head span:first-child {
  font-size: 14px;
  font-weight: 700;
}

.section-tip {
  color: #4b5563;
  font-size: 12px;
}

.override-editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
}

.override-list {
  min-height: 72px;
}

.override-item-list {
  display: grid;
  gap: 8px;
}

.override-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #fafcff;
}

.override-item strong {
  display: block;
  margin-bottom: 4px;
  font-size: 13px;
}

.override-item span {
  color: #4b5563;
  font-size: 12px;
}

.simulation-grid,
.advance-days-box {
  flex-wrap: wrap;
}

.action-row {
  justify-content: flex-start;
  flex-wrap: wrap;
  max-width: 100%;
}

.action-row :deep(.el-button) {
  margin: 0;
}

.preview-summary {
  display: grid;
  gap: 12px;
  padding-top: 4px;
  border-top: 1px solid #edf1f6;
}

.work-order-analysis-dialog {
  display: grid;
  gap: 16px;
}

.analysis-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.preview-actions {
  justify-content: space-between;
  flex-wrap: wrap;
}

.preview-context-cue {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.status-text {
  margin: 0;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.status-text.warning {
  color: #c2410c;
}

.issue-dialog-tabs {
  margin-bottom: 12px;
}

.issue-link-button {
  padding: 0;
  color: #1677ff;
}

.day-summary-task-group-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 12px;
  min-height: 420px;
}

.day-summary-workorder-list {
  display: grid;
  align-content: start;
  gap: 8px;
  max-height: 560px;
  padding: 4px;
  overflow: auto;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f7f9fc;
}

.day-summary-workorder-card {
  display: grid;
  gap: 6px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  color: #263247;
  text-align: left;
  background: #fff;
  cursor: pointer;
}

.day-summary-workorder-card:hover,
.day-summary-workorder-card.active {
  border-color: #1677ff;
  background: #fafcff;
}

.day-summary-workorder-card.active {
  box-shadow: inset 3px 0 0 #1677ff;
}

.day-summary-workorder-card__line-name {
  overflow: hidden;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.day-summary-workorder-card__meta {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.4;
}

.day-summary-workorder-card__tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.day-summary-selected-task-table {
  min-width: 0;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

@media (max-width: 1480px) {
  .workspace-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .calendar-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-block-right {
    justify-content: flex-start;
  }
}

@media (max-width: 960px) {
  .calendar-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .weekday-row {
    display: none;
  }

  .setting-grid,
  .preview-summary-grid,
  .analysis-summary-grid,
  .detail-summary-grid,
  .day-summary-task-group-layout,
  .override-editor {
    grid-template-columns: 1fr;
  }

  .detail-total-quantity-card {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .detail-total-quantity-card span {
    padding-bottom: 0;
  }

  .detail-total-quantity-card small {
    flex-basis: auto;
  }

  .process-capacity-metrics {
    grid-template-columns: 1fr;
  }

  .calendar-metric-list {
    grid-template-columns: 1fr;
  }
}
</style>
