<template>
  <ContentWrap class="edhr-batch-detail__content-wrap">
    <div v-loading="loading" class="edhr-batch-detail">
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <el-alert v-if="releaseActionError" :title="releaseActionError" type="error" :closable="false" show-icon />

      <div v-if="workbench?.stageBlockers?.length" class="edhr-batch-detail__blockers">
        <div class="edhr-batch-detail__section-title">当前待处理事项</div>
        <div class="edhr-batch-detail__blocker-list">
          <div
            v-for="(blocker, index) in workbench.stageBlockers"
            :key="`stage-${index}-${blocker}`"
            class="edhr-batch-detail__blocker-item"
          >
            {{ blocker }}
          </div>
        </div>
      </div>


      <section class="edhr-batch-detail__review" aria-label="工序复盘">
        <el-alert
          v-if="secondaryLoadError"
          :title="secondaryLoadError"
          type="error"
          :closable="false"
          show-icon
        />
        <el-alert v-if="reviewError" :title="reviewError" type="error" :closable="false" show-icon />
        <div class="edhr-batch-detail__review-workbench">
          <nav class="edhr-batch-detail__process-panel edhr-batch-detail__process-list edhr-batch-detail__review-list" aria-label="工序列表">
            <el-empty
              v-if="
                !processTaskGroups.length &&
                !preProcessSpecialTaskEntries.length &&
                !postProcessSpecialTaskEntries.length
              "
              description="当前批次暂无工序表单任务"
            />
            <template v-for="task in preProcessSpecialTaskEntries" :key="String(task.id)">
              <div
                class="edhr-batch-detail__process-task-group edhr-batch-detail__special-process-task-group"
                :class="{ 'is-active': String(task.id) === selectedTaskId }"
              >
                <button
                  type="button"
                  class="edhr-batch-detail__process-task-group-head"
                  @click="selectProcessTask(task)"
                >
                  <span class="edhr-batch-detail__review-main">
                    <span class="edhr-batch-detail__process-sort">
                      {{ resolvePendingTaskSortText(task) }}
                    </span>
                    <span
                      class="edhr-batch-detail__process-code edhr-batch-detail__review-process-name"
                      :title="resolvePendingTaskTitle(task)"
                    >
                      {{ resolvePendingTaskTitle(task) }}
                    </span>
                  </span>
                </button>
              </div>
            </template>
            <div
              v-for="processGroup in processTaskGroups"
              :key="processGroup.key"
              class="edhr-batch-detail__process-task-group"
              :class="[
                resolveProcessGroupStateClass(processGroup),
                { 'is-active': isProcessGroupActive(processGroup) }
              ]"
            >
              <button
                type="button"
                class="edhr-batch-detail__process-task-group-head"
                @click="selectProcessTask(processGroup.primaryTask)"
              >
                <span class="edhr-batch-detail__review-main">
                  <span class="edhr-batch-detail__process-sort">{{ processGroup.routeProcessSort || '--' }}</span>
                  <span
                    class="edhr-batch-detail__process-code edhr-batch-detail__review-process-name"
                    :title="processGroup.processName || '--'"
                  >
                    {{ processGroup.processName || '--' }}
                  </span>
                </span>
              </button>
            </div>
            <template v-for="task in postProcessSpecialTaskEntries" :key="String(task.id)">
              <div
                class="edhr-batch-detail__process-task-group edhr-batch-detail__special-process-task-group"
                :class="{ 'is-active': String(task.id) === selectedTaskId }"
              >
                <button
                  type="button"
                  class="edhr-batch-detail__process-task-group-head"
                  @click="selectProcessTask(task)"
                >
                  <span class="edhr-batch-detail__review-main">
                    <span class="edhr-batch-detail__process-sort">
                      {{ resolvePendingTaskSortText(task) }}
                    </span>
                    <span
                      class="edhr-batch-detail__process-code edhr-batch-detail__review-process-name"
                      :title="resolvePendingTaskTitle(task)"
                    >
                      {{ resolvePendingTaskTitle(task) }}
                    </span>
                  </span>
                </button>
              </div>
            </template>
            <button
              type="button"
              class="edhr-batch-detail__review-item edhr-batch-detail__release-process-item"
              :class="{ 'is-active': isReleaseProcessSelected }"
              @click="selectReleaseProcess"
            >
              <span class="edhr-batch-detail__review-main">
                <span class="edhr-batch-detail__process-sort">{{ RELEASE_VIRTUAL_PROCESS.sort }}</span>
                <span class="edhr-batch-detail__process-code edhr-batch-detail__review-process-name">
                  {{ RELEASE_VIRTUAL_PROCESS.label }}
                </span>
              </span>
              <el-tag size="small" :type="resolveBatchStatusType(detail?.status)">
                {{ resolveBatchStatusLabel(detail?.status) }}
              </el-tag>
            </button>
          </nav>

          <div
            class="edhr-batch-detail__form-panel edhr-batch-detail__review-preview"
            aria-label="当前工序表单"
          >
            <div
              v-if="selectedProcessContext && !isReleaseProcessSelected"
              class="edhr-batch-detail__preview-header"
              :class="{
                'is-batch-record': currentProcessFillCarrier === 'FORM',
                'is-recordbook': currentProcessFillCarrier === 'RECORDBOOK'
              }"
            >
              <div class="edhr-batch-detail__preview-context" aria-label="当前批记录上下文">
                <span :title="detail?.workOrderCode || ''">{{ detail?.workOrderCode || '--' }}</span>
                <span :title="resolveCurrentBatchRecordNo()">{{ resolveCurrentBatchRecordNo() }}</span>
              </div>
              <div class="edhr-batch-detail__preview-actions" aria-label="批记录操作">
                <button
                  type="button"
                  class="edhr-batch-detail__preview-route-link"
                  :disabled="!batchProcessRouteId"
                  :title="batchProcessRouteTitle"
                  :aria-label="batchProcessRouteTitle"
                  @click.stop="openBatchProcessRoute"
                >
                  工艺流程：{{ batchProcessRouteLabel }}
                </button>
                <el-button
                  type="primary"
                  size="small"
                  :loading="syncLoading"
                  class="edhr-batch-detail__preview-sync"
                  @click.stop="handleSync"
                >
                  同步状态
                </el-button>
              </div>
              <div class="edhr-batch-detail__preview-extra" aria-label="批记录附加操作">
                <span
                  v-if="currentFormVersionNo"
                  class="edhr-batch-detail__preview-form-version"
                  :title="`版本号：${currentFormVersionNo}`"
                  aria-label="当前表单版本号"
                >
                  版本：{{ currentFormVersionNo }}
                </span>
                <div
                  v-if="selectedTaskForEvidence && !isSpecialNode(selectedTaskForEvidence) && isGlobalRecordbookEnabled"
                  class="edhr-batch-detail__preview-carrier"
                  aria-label="填写载体"
                  @click.stop
                >
                  <div class="edhr-batch-detail__preview-carrier-control">
                    <button
                      type="button"
                      class="edhr-batch-detail__preview-carrier-option"
                      :class="{ 'is-active': currentProcessFillCarrier === 'FORM' }"
                      :aria-pressed="currentProcessFillCarrier === 'FORM'"
                      aria-label="选择批记录填写"
                      @click.stop="selectFillCarrier('FORM')"
                    >
                      批记录
                    </button>
                    <button
                      v-if="isRecordbookEnabledForCurrentTask"
                      type="button"
                      class="edhr-batch-detail__preview-carrier-option"
                      :class="{ 'is-active': currentProcessFillCarrier === 'RECORDBOOK' }"
                      :aria-pressed="currentProcessFillCarrier === 'RECORDBOOK'"
                      aria-label="选择记录本填写"
                      @click.stop="selectFillCarrier('RECORDBOOK')"
                    >
                      记录本
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div
              v-if="isReleaseProcessSelected"
              aria-label="放行预检工作区"
              class="edhr-batch-detail__release-main-workspace edhr-batch-detail__release-precheck-workspace"
            >
              <div class="edhr-batch-detail__release-precheck-card">
                <div class="edhr-batch-detail__release-precheck-head">
                  <div>
                    <div class="edhr-batch-detail__release-precheck-title">放行预检</div>
                    <div class="edhr-batch-detail__muted">
                      {{ workbench?.releaseSummary?.precheckSummary || '尚未执行放行预检。' }}
                    </div>
                  </div>
                  <div class="edhr-batch-detail__release-precheck-toolbar">
                    <el-button
                      type="primary"
                      :disabled="isViewedReleaseStageReadonly || !canRunReleasePrecheck"
                      :loading="releasePrecheckLoading"
                      @click="handleReleasePrecheck"
                    >
                      预检
                    </el-button>
                    <el-button @click="openTraceRecordGroup">追溯记录</el-button>
                  </div>
                </div>
                <el-alert
                  v-if="releaseActionError"
                  :title="releaseActionError"
                  type="error"
                  :closable="false"
                  show-icon
                  class="edhr-batch-detail__dialog-alert"
                />
                <el-table
                  v-loading="releaseCheckLoading"
                  :data="releaseCheckItems"
                  stripe
                  empty-text="暂无放行预检项"
                >
                  <el-table-column label="预检项" min-width="220">
                    <template #default="{ row }">
                      <div class="edhr-batch-detail__task-name">{{ resolveReleaseCheckCodeLabel(row.checkCode) }}</div>
                      <div class="edhr-batch-detail__muted">
                        分类：{{ resolveReleaseCheckCategoryLabel(row.checkCategory) }}
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="结果" width="115">
                    <template #default="{ row }">
                      <el-tag :type="resolveReleaseCheckResultTagType(row.checkResult)">
                        {{ resolveReleaseCheckResultLabel(row.checkResult) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="责任模块" width="120" prop="responsibilityModule" />
                  <el-table-column label="源对象" min-width="180">
                    <template #default="{ row }">
                      <div>{{ row.sourceObjectCode || '--' }}</div>
                      <div class="edhr-batch-detail__muted">
                        {{ resolveReleaseCheckSourceObjectTypeLabel(row.sourceObjectType) }}
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="失败原因" min-width="240" prop="failureReason" />
                  <el-table-column label="处理建议" min-width="240" prop="remediationSuggestion" />
                </el-table>
              </div>
            </div>
            <el-empty v-else-if="!selectedProcessContext" description="请选择左侧工序查看表单" />
            <div
              v-else-if="selectedSpecialNodeForEvidence"
              class="edhr-batch-detail__special-node-attachments"
              aria-label="当前节点附件"
            >
              <div class="edhr-batch-detail__special-node-attachments-head">
                <div>
                  <div class="edhr-batch-detail__special-node-attachments-title">当前节点附件</div>
                  <div class="edhr-batch-detail__muted">
                    {{ resolveTaskDisplayName(selectedSpecialNodeForEvidence) }}
                  </div>
                </div>
                <el-tag size="small" :type="resolveTaskStatusType(selectedSpecialNodeForEvidence)">
                  {{ resolveTaskStatusLabel(selectedSpecialNodeForEvidence) }}
                </el-tag>
              </div>
              <section class="edhr-batch-detail__special-node-attachment-section" aria-label="待提交附件">
                <div class="edhr-batch-detail__special-node-attachment-section-head">
                  <span>待提交附件</span>
                  <el-tag size="small" effect="plain">
                    {{ selectedSpecialNodePendingAttachments.length }} 个
                  </el-tag>
                </div>
                <div
                  v-if="selectedSpecialNodePendingAttachments.length"
                  class="edhr-batch-detail__special-node-file-list"
                >
                  <div
                    v-for="attachment in selectedSpecialNodePendingAttachments"
                    :key="buildSpecialNodeAttachmentKey(attachment)"
                    class="edhr-batch-detail__special-node-file-row"
                  >
                    <button
                      type="button"
                      class="edhr-batch-detail__special-node-file-name"
                      :title="attachment.fileName"
                      @click="previewSpecialNodeAttachment(attachment)"
                    >
                      {{ attachment.fileName }}
                    </button>
                    <span class="edhr-batch-detail__special-node-file-meta">
                      {{ formatSpecialNodeFileSize(attachment.fileSize) }}
                    </span>
                    <span class="edhr-batch-detail__special-node-file-status">待提交</span>
                    <button
                      type="button"
                      class="edhr-batch-detail__special-node-file-action"
                      @click="previewSpecialNodeAttachment(attachment)"
                    >
                      预览
                    </button>
                    <button
                      type="button"
                      class="edhr-batch-detail__special-node-file-action is-danger"
                      @click="removeSelectedSpecialNodePendingAttachment(attachment)"
                    >
                      删除
                    </button>
                  </div>
                </div>
                <el-empty
                  v-else
                  description="暂无待提交附件，可在右侧点击上传文件"
                  :image-size="52"
                />
              </section>
              <section class="edhr-batch-detail__special-node-attachment-section" aria-label="已入账附件">
                <div class="edhr-batch-detail__special-node-attachment-section-head">
                  <span>已入账附件</span>
                  <el-tag size="small" effect="plain">
                    {{ selectedSpecialNodePersistedAttachments.length }} 个
                  </el-tag>
                </div>
                <div
                  v-if="selectedSpecialNodePersistedAttachments.length"
                  class="edhr-batch-detail__special-node-file-list"
                >
                  <div
                    v-for="attachment in selectedSpecialNodePersistedAttachments"
                    :key="buildSpecialNodeAttachmentKey(attachment)"
                    class="edhr-batch-detail__special-node-file-row is-readonly"
                  >
                    <button
                      type="button"
                      class="edhr-batch-detail__special-node-file-name"
                      :title="attachment.fileName || attachment.storagePath || '--'"
                      @click="previewSpecialNodeAttachment(attachment)"
                    >
                      {{ attachment.fileName || attachment.storagePath || '--' }}
                    </button>
                    <span class="edhr-batch-detail__special-node-file-meta">
                      {{ formatSpecialNodeFileSize(attachment.fileSize) }}
                    </span>
                    <span class="edhr-batch-detail__special-node-file-status is-persisted">已入账</span>
                    <button
                      type="button"
                      class="edhr-batch-detail__special-node-file-action"
                      @click="previewSpecialNodeAttachment(attachment)"
                    >
                      预览
                    </button>
                  </div>
                </div>
                <el-empty v-else description="暂无已入账附件" :image-size="52" />
              </section>
            </div>
            <div v-else class="edhr-batch-detail__review-card">
              <div class="edhr-batch-detail__form-surface" aria-label="已填写批记录">
                <section
                  v-if="effectiveDetailPreviewAssistMode"
                  class="edhr-batch-detail__assist-preview"
                  aria-label="辅助模式只读预览"
                >
                  <div class="edhr-batch-detail__assist-preview-head">
                    <div>
                      <strong>辅助模式</strong>
                      <span>只读预览，不提供保存、提交、签名或上传动作。</span>
                    </div>
                    <el-tag type="info" effect="plain">{{ selectedPreviewAssistFields.length }} 项</el-tag>
                  </div>
                  <el-alert
                    v-if="selectedPreviewAssistGridErrors.length"
                    :title="selectedPreviewAssistGridErrors.join('；')"
                    type="error"
                    :closable="false"
                    show-icon
                  />
                  <div v-else class="edhr-batch-detail__assist-grid-list">
                    <section
                      v-for="grid in selectedPreviewAssistGrids"
                      :key="grid.subjectKey"
                      class="edhr-batch-detail__assist-grid-group"
                    >
                      <div class="edhr-batch-detail__assist-grid-meta">
                        <strong>{{ grid.subjectLabel }}</strong>
                        <el-tag size="small" effect="plain">
                          辅助表格 {{ grid.rowCount }} × {{ grid.columnCount }}
                        </el-tag>
                      </div>
                      <div class="edhr-batch-detail__assist-grid-surface">
                        <table class="edhr-batch-detail__assist-grid">
                          <tbody>
                            <tr v-for="gridRow in grid.rows" :key="gridRow.rowIndex">
                              <td v-for="gridCell in gridRow.cells" :key="gridCell.key">
                                <div
                                  class="edhr-batch-detail__assist-grid-cell"
                                  :class="gridCell.field ? 'is-mapped' : 'is-empty'"
                                  :data-assist-grid-cell="gridCell.key"
                                  :title="gridCell.field?.location || '未映射'"
                                >
                                  <span>{{ gridCell.field?.label || '未映射' }}</span>
                                  <small v-if="gridCell.field">
                                    当前值：{{ gridCell.field.displayValue }}
                                  </small>
                                  <small v-else>未映射</small>
                                  <em v-if="gridCell.field">{{ gridCell.field.typeLabel }}</em>
                                </div>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </section>
                  </div>
                  <el-empty
                    v-if="
                      selectedPreviewAssistFields.length === 0 &&
                      selectedPreviewAssistGridErrors.length === 0
                    "
                    description="未配置辅助模式"
                    :image-size="52"
                  />
                </section>
                <EdhrExecutionReadonlyForm
                  v-else-if="selectedExecution"
                  :form-view-model="selectedExecution.formViewModel"
                  :signature-records="selectedExecution.signatureRecords"
                  fit-to-viewport
                />
                <el-empty v-else description="暂无已提交批记录内容" />
              </div>
            </div>
          </div>

          <aside class="edhr-batch-detail__review-rail" aria-label="当前工序摘要">
            <template v-if="isReleaseProcessSelected">
              <div class="edhr-batch-detail__release-rail-head">
                <div class="edhr-batch-detail__rail-task-title">{{ viewedReleaseStageViewModel.stageLabel }}</div>
                <div class="edhr-batch-detail__release-owner-hint" aria-label="当前放行负责人">
                  当前放行负责人：{{ releaseStageOwnerLabel }}
                </div>
                <div v-if="isViewedReleaseStageReadonly" class="edhr-batch-detail__muted">
                  {{ releaseStagePanelHint }}
                </div>
              </div>
              <el-alert
                v-if="isViewedReleaseStageReadonly"
                :title="`只读查看：${viewedReleaseStageReadonlyMessage}`"
                type="info"
                :closable="false"
                show-icon
              />
              <el-alert
                v-if="batchActionLocked"
                :title="batchActionLockMessage"
                type="warning"
                :closable="false"
                show-icon
              />
              <el-alert
                v-if="isQualityTerminalStage"
                title="误拒收选择“申请重开原记录”；真拒收重做选择“重新执行同批号”，系统保留原拒收证据并建立追溯关系。"
                type="warning"
                :closable="false"
                show-icon
              />
              <div class="edhr-batch-detail__release-rail-actions" aria-label="放行工序参数">
                <template v-for="action in releaseStageActionItems" :key="action.key">
                  <button
                    v-if="action.permission"
                    v-hasPermi="action.permission"
                    type="button"
                    class="edhr-batch-detail__release-image-action"
                    :class="[`is-${action.key}`, action.type ? `is-${action.type}` : 'is-default', { 'is-loading': action.loading }]"
                    :disabled="action.disabled"
                    :aria-busy="action.loading ? 'true' : 'false'"
                    @click="action.onClick"
                  >
                    <span class="edhr-batch-detail__release-image-action-visual" aria-hidden="true"></span>
                    <span class="edhr-batch-detail__release-image-action-label">{{ action.label }}</span>
                    <span v-if="action.loading" class="edhr-batch-detail__release-image-action-status">处理中...</span>
                  </button>
                  <button
                    v-else
                    type="button"
                    class="edhr-batch-detail__release-image-action"
                    :class="[`is-${action.key}`, action.type ? `is-${action.type}` : 'is-default', { 'is-loading': action.loading }]"
                    :disabled="action.disabled"
                    :aria-busy="action.loading ? 'true' : 'false'"
                    @click="action.onClick"
                  >
                    <span class="edhr-batch-detail__release-image-action-visual" aria-hidden="true"></span>
                    <span class="edhr-batch-detail__release-image-action-label">{{ action.label }}</span>
                    <span v-if="action.loading" class="edhr-batch-detail__release-image-action-status">处理中...</span>
                  </button>
                </template>
              </div>
            </template>
            <template v-else>
              <div
                v-if="selectedProcessTaskGroup"
                class="edhr-batch-detail__preview-mode-switch"
                :title="selectedPreviewAssistRowsConfigured ? '切换中间预览模式' : '未配置辅助模式'"
              >
                <span class="edhr-batch-detail__preview-mode-label">原表模式</span>
                <el-switch
                  v-model="detailPreviewAssistMode"
                  size="small"
                  :disabled="!selectedPreviewAssistRowsConfigured"
                  aria-label="详情页辅助模式切换"
                />
                <span class="edhr-batch-detail__preview-mode-label">辅助模式</span>
                <span
                  v-if="!selectedPreviewAssistRowsConfigured"
                  class="edhr-batch-detail__preview-mode-disabled"
                >
                  未配置辅助模式
                </span>
              </div>
              <div
                v-if="selectedProcessTaskGroup"
                class="edhr-batch-detail__rail-process-forms"
                aria-label="当前工序表单列表"
              >
                <div v-if="selectedProcessTasks.length" class="edhr-batch-detail__rail-process-form-list">
                  <div
                    v-for="task in selectedProcessTasks"
                    :key="String(task.id)"
                    role="button"
                    tabindex="0"
                    class="edhr-batch-detail__rail-process-form-item"
                    :class="{ 'is-active': String(task.id) === selectedTaskId }"
                    @click="selectProcessTask(task)"
                    @keydown.enter.prevent="selectProcessTask(task)"
                    @keydown.space.prevent="selectProcessTask(task)"
                  >
                    <div
                      class="edhr-batch-detail__rail-process-form-name"
                      :title="resolveTaskCardDisplayName(task)"
                    >
                      {{ resolveTaskCardDisplayName(task) }}
                    </div>
                    <div class="edhr-batch-detail__rail-process-form-head">
                      <span class="edhr-batch-detail__rail-process-form-slot">
                        {{ resolveFormSlotTypeLabel(task) }}
                      </span>
                      <div class="edhr-batch-detail__rail-process-form-state-tags">
                        <el-tag
                          v-if="isOptionalTask(task)"
                          size="small"
                          type="warning"
                          effect="plain"
                          class="edhr-batch-detail__rail-process-form-optional-tag"
                        >
                          可选填写
                        </el-tag>
                        <el-tag size="small" :type="resolveTaskStatusType(task)">
                          {{ resolveTaskStatusLabel(task) }}
                        </el-tag>
                      </div>
                    </div>
                    <div
                      class="edhr-batch-detail__rail-process-form-filler"
                      :title="resolveTaskCardFillersText(task)"
                    >
                      <span>填写人</span>
                      <strong>{{ resolveTaskCardFillersText(task) }}</strong>
                    </div>
                    <div
                      v-if="resolveTaskGateText(task)"
                      class="edhr-batch-detail__rail-process-form-gate"
                    >
                      {{ resolveTaskGateText(task) }}
                    </div>
                    <div class="edhr-batch-detail__rail-process-form-actions">
                      <button
                        type="button"
                        class="edhr-batch-detail__rail-process-form-action"
                        :disabled="!canHandlePendingTask(task)"
                        @click.stop="handleSelectedPendingTaskAction(task)"
                      >
                        {{ resolvePendingTaskActionLabel(task) }}
                      </button>
                      <button
                        v-if="isOptionalTask(task) && canOpenTask(task)"
                        type="button"
                        class="edhr-batch-detail__rail-process-form-action is-skip"
                        :disabled="!canSkipOptionalTask(task)"
                        @click.stop="handleSkipOptionalTask(task)"
                      >
                        跳过表单
                      </button>
                      <button
                        v-if="canTakeOverFillTask(task)"
                        type="button"
                        class="edhr-batch-detail__rail-process-form-action is-takeover"
                        :disabled="fillTaskTakeoverLoading === task.activeWorkTaskId"
                        @click.stop="handleTakeOverFillTask(task)"
                      >
                        {{
                          fillTaskTakeoverLoading === task.activeWorkTaskId
                            ? '接管中...'
                            : '管理员接管并填写'
                        }}
                      </button>
                    </div>
                  </div>
                </div>
                <el-empty v-else description="当前工序未配置表单" :image-size="44" />
              </div>
              <div
                v-if="selectedTaskForEvidence && isSpecialNode(selectedTaskForEvidence)"
                class="edhr-batch-detail__special-node-action-grid"
                aria-label="特殊节点操作"
              >
                <div
                  class="edhr-batch-detail__special-node-filler edhr-batch-detail__rail-process-form-filler"
                  :title="resolveTaskCardFillersText(selectedTaskForEvidence)"
                  aria-label="特殊节点填写人"
                >
                  <span>填写人</span>
                  <strong>{{ resolveTaskCardFillersText(selectedTaskForEvidence) }}</strong>
                </div>
                <el-upload
                  ref="specialNodeRailUploadRef"
                  class="edhr-batch-detail__special-node-hidden-upload"
                  :show-file-list="false"
                  :http-request="uploadSelectedSpecialNodeAttachment"
                  :disabled="!canUploadSpecialNodeAttachment(selectedTaskForEvidence) || specialNodeAttachmentUploading"
                  multiple
                >
                  <button
                    type="button"
                    class="edhr-batch-detail__special-node-hidden-upload-trigger"
                    tabindex="-1"
                  >
                    上传文件
                  </button>
                </el-upload>
                <el-button
                  type="primary"
                  :loading="specialNodeAttachmentUploading"
                  :disabled="!canUploadSpecialNodeAttachment(selectedTaskForEvidence)"
                  class="edhr-batch-detail__rail-task-action"
                  @click.stop="triggerSelectedSpecialNodeUpload"
                >
                  上传文件
                </el-button>
                <el-button
                  type="warning"
                  :disabled="!canOperateSpecialNode(selectedTaskForEvidence)"
                  :loading="specialNodeActionLoading[selectedTaskForEvidence.id] === 'skip'"
                  class="edhr-batch-detail__rail-task-action"
                  @click.stop="handleSkipSpecialNode(selectedTaskForEvidence)"
                >
                  跳过节点
                </el-button>
                <el-button
                  type="success"
                  :disabled="!canOperateSpecialNode(selectedTaskForEvidence)"
                  :loading="specialNodeActionLoading[selectedTaskForEvidence.id] === 'complete'"
                  class="edhr-batch-detail__rail-task-action"
                  @click.stop="handleCompleteSpecialNode(selectedTaskForEvidence)"
                >
                  完成节点
                </el-button>
              </div>
            </template>
          </aside>

        </div>
      </section>

    </div>

    <Dialog title="详情" v-model="processDetailDialogVisible" width="420px">
      <div class="edhr-batch-detail__process-detail-dialog">
        <div class="edhr-batch-detail__process-detail-actions" aria-label="当前工序控制按钮">
          <div class="edhr-batch-detail__process-evidence" aria-label="当前工序证据链">
            <el-empty v-if="!selectedProcessContext" description="请选择左侧工序查看控制按钮" />
            <div v-else class="edhr-batch-detail__process-evidence-groups">
              <div class="edhr-batch-detail__process-evidence-context" aria-label="工序上下文">
                <div>
                  <span class="edhr-batch-detail__process-evidence-context-title">当前工序操作台</span>
                  <small>仅作用于当前选中的工序</small>
                </div>
                <small class="edhr-batch-detail__process-evidence-context-entry">完整明细入口</small>
              </div>
              <section
                v-for="group in selectedProcessEvidenceGroups"
                :key="group.key"
                class="edhr-batch-detail__process-evidence-group"
                :aria-label="group.label"
              >
                <div class="edhr-batch-detail__process-evidence-group-title">
                  <span>{{ group.label }}</span>
                  <small>{{ group.description }}</small>
                </div>
                <div class="edhr-batch-detail__process-evidence-grid">
                  <button
                    v-for="item in group.items"
                    :key="item.key"
                    type="button"
                    class="edhr-batch-detail__process-evidence-item"
                    :disabled="item.disabled"
                    @click="openSelectedProcessEvidence(item)"
                  >
                    <span>{{ item.label }}</span>
                    <small>{{ item.description }}</small>
                  </button>
                </div>
              </section>
            </div>
          </div>
        </div>
      </div>
    </Dialog>

    <Dialog title="申请重开电子批记录批次" v-model="reopenDialogVisible" width="560px">
      <el-alert
        v-if="reopenError"
        :title="reopenError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <el-form label-width="96px">
        <el-form-item label="批次编号">
          {{ detail?.batchExecutionCode || detail?.id || '--' }}
        </el-form-item>
        <el-form-item label="原因分类" required>
          <el-select v-model="reopenForm.reasonCategory" class="!w-1/1" placeholder="请选择原因分类">
            <el-option
              v-for="option in reopenReasonOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="原因说明" required>
          <el-input
            v-model="reopenForm.reasonText"
            type="textarea"
            :rows="3"
            placeholder="请输入重开原因"
          />
        </el-form-item>
        <el-form-item label="签名密码" required>
          <el-input
            v-model="reopenForm.password"
            type="password"
            show-password
            placeholder="请输入当前账号密码"
            @keyup.enter="submitReopenBatch"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="reopenForm.comment"
            type="textarea"
            :rows="2"
            placeholder="请输入备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reopenDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="reopenLoading" @click="submitReopenBatch">提交申请</el-button>
      </template>
    </Dialog>

    <Dialog title="重新执行同生产批号" v-model="reexecuteDialogVisible" width="560px">
      <el-alert
        v-if="reexecuteError"
        :title="reexecuteError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <el-alert
        title="仅用于真拒收后的同批号重做：原拒收批次保持质量已拒收，新建第 N 次执行尝试并建立追溯关系。"
        type="warning"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <el-form label-width="116px">
        <el-form-item label="来源拒收批次">
          {{ detail?.batchExecutionCode || detail?.id || '--' }}
        </el-form-item>
        <el-form-item label="生产批号">
          {{ detail?.batchCode || '--' }}
        </el-form-item>
        <el-form-item label="当前尝试">
          {{ detail?.attemptNo ? `第 ${detail.attemptNo} 次` : '第 1 次' }}
        </el-form-item>
        <el-form-item label="重做原因" required>
          <el-input
            v-model="reexecuteForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入真拒收后同生产批号重做原因"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="reexecuteForm.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reexecuteDialogVisible = false">取 消</el-button>
        <el-button type="danger" :loading="reexecuteLoading" @click="submitReexecuteRejectedBatch">
          创建新执行尝试
        </el-button>
      </template>
    </Dialog>

    <Dialog title="质量拒收电子批记录批次" v-model="qualityRejectDialogVisible" width="560px">
      <el-alert
        v-if="qualityRejectError"
        :title="qualityRejectError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <el-form label-width="108px">
        <el-form-item label="批次编号">
          {{ detail?.batchExecutionCode || detail?.id || '--' }}
        </el-form-item>
        <el-form-item label="质量拒收原因" required>
          <el-input v-model="qualityRejectForm.reason" type="textarea" :rows="3" placeholder="请输入质量拒收原因" />
        </el-form-item>
        <el-form-item label="签名密码" required>
          <el-input
            v-model="qualityRejectForm.password"
            type="password"
            show-password
            placeholder="请输入当前账号密码"
            @keyup.enter="submitQualityReject"
          />
        </el-form-item>
        <el-divider content-position="left">签名显示时间</el-divider>
        <el-form-item label="签名时间">
          <el-date-picker
            v-model="qualityRejectSignatureTimeForm.selectedSignedAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="可选择人工签名时间"
            class="!w-1/1"
          />
        </el-form-item>
        <el-form-item label="签名时区">
          <el-input v-model="qualityRejectSignatureTimeForm.selectedTimeZone" placeholder="例如 Asia/Shanghai" />
        </el-form-item>
        <el-form-item label="时间原因">
          <el-input
            v-model="qualityRejectSignatureTimeForm.selectedTimeReason"
            type="textarea"
            :rows="2"
            placeholder="选择人工签名时间时必须说明原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="qualityRejectDialogVisible = false">取 消</el-button>
        <el-button type="danger" :loading="qualityRejectLoading" @click="submitQualityReject">确认拒收</el-button>
      </template>
    </Dialog>

    <Dialog :title="currentSkipDialogTitle" v-model="specialNodeSkipDialogVisible" width="560px">
      <el-alert
        v-if="specialNodeSkipError"
        :title="specialNodeSkipError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <el-form label-width="108px">
        <el-form-item :label="currentSkipTaskIsOptional ? '表单名称' : '节点名称'">
          {{ currentSpecialNode ? resolveTaskDisplayName(currentSpecialNode) : '--' }}
        </el-form-item>
        <el-form-item label="跳过原因" required>
          <el-input
            v-model="specialNodeSkipForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入跳过原因，便于归档和审计复核"
          />
        </el-form-item>
        <el-form-item label="签名密码" required>
          <el-input
            v-model="specialNodeSkipForm.password"
            type="password"
            show-password
            placeholder="请输入当前账号密码"
            @keyup.enter="submitSpecialNodeSkip"
          />
        </el-form-item>
        <el-form-item v-if="!currentSkipTaskIsOptional" label="待提交附件">
          <div class="edhr-batch-detail__dialog-attachment-list">
            <el-tag
              v-for="attachment in currentSpecialNodePendingAttachments"
              :key="attachment.uploadToken"
              class="edhr-batch-detail__attachment-tag"
              type="info"
            >
              {{ attachment.fileName }}
            </el-tag>
            <span v-if="!currentSpecialNodePendingAttachments.length" class="edhr-batch-detail__muted">
              暂无附件，可先在右侧点击上传文件。
            </span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="specialNodeSkipDialogVisible = false">取 消</el-button>
        <el-button type="warning" :loading="specialNodeSkipLoading" @click="submitSpecialNodeSkip">
          签名并跳过
        </el-button>
      </template>
    </Dialog>

    <Dialog title="完成特殊节点" v-model="specialNodeCompleteDialogVisible" width="520px">
      <el-alert
        v-if="specialNodeCompleteError"
        :title="specialNodeCompleteError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <el-form label-width="108px">
        <el-form-item label="节点名称">
          {{ currentSpecialNode ? resolveTaskDisplayName(currentSpecialNode) : '--' }}
        </el-form-item>
        <el-form-item v-if="isSterilizationNode(currentSpecialNode)" label="灭菌批次" required>
          <el-input
            v-model="specialNodeCompleteForm.sterilizationBatchNo"
            placeholder="请输入灭菌批次"
            @keyup.enter="submitSpecialNodeComplete"
          />
        </el-form-item>
        <el-form-item label="待提交附件">
          <div class="edhr-batch-detail__dialog-attachment-list">
            <el-tag
              v-for="attachment in currentSpecialNodePendingAttachments"
              :key="attachment.uploadToken"
              class="edhr-batch-detail__attachment-tag"
              type="info"
            >
              {{ attachment.fileName }}
            </el-tag>
            <span v-if="!currentSpecialNodePendingAttachments.length" class="edhr-batch-detail__muted">
              暂无附件，可先在右侧点击上传文件。
            </span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="specialNodeCompleteDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="specialNodeCompleteLoading" @click="submitSpecialNodeComplete">确 认</el-button>
      </template>
    </Dialog>

    <el-drawer v-model="archivePrintDrawerVisible" title="归档打印" size="520px">
      <div class="edhr-batch-detail__group-actions">
        <el-button v-hasPermi="['mes:pro-edhr-batch-execution-archive:create']" type="primary" :disabled="isViewedReleaseStageReadonly || !canGenerateArchive" @click="handleGenerateArchive">生成归档</el-button>
        <el-button v-hasPermi="['mes:pro-edhr-batch-execution-archive:create']" :disabled="isViewedReleaseStageReadonly || !canGenerateArchive" @click="handleGenerateArchive">重新生成</el-button>
        <el-button v-hasPermi="['mes:pro-edhr-batch-execution-archive:download']" :disabled="isViewedReleaseStageReadonly" @click="handleDownloadArchive">
          下载打印版 PDF
        </el-button>
        <el-button v-hasPermi="['mes:pro-edhr-batch-execution-archive:download']" :disabled="isViewedReleaseStageReadonly" @click="handlePrintArchive">
          打印
        </el-button>
      </div>
    </el-drawer>

    <el-drawer v-model="uxChecklistDrawerVisible" title="电子批记录页面体验检查清单" size="72%">
      <div class="edhr-batch-detail__precheck-summary">
        用于填写和收尾前逐项检查页面体验问题；发现不合理项应进入优化文档，不通过查库或口头说明绕过。
      </div>
      <el-table :data="UX_CHECKLIST_ITEMS" empty-text="暂无体验检查项">
        <el-table-column label="检查项" prop="label" width="160" />
        <el-table-column label="验收要点" prop="check" min-width="300" />
        <el-table-column label="不通过示例" prop="risk" min-width="260" />
      </el-table>
    </el-drawer>

    <el-drawer v-model="traceRecordDrawerVisible" title="追溯记录" size="72%">
      <el-alert
        v-if="releaseActionError"
        :title="releaseActionError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <el-tabs v-model="traceRecordTab">
        <el-tab-pane label="放行事件" name="release">
          <ReleaseEventListPane
            v-if="traceRecordReleaseTransactionId"
            :release-transaction-id="traceRecordReleaseTransactionId"
          />
          <el-empty v-else description="当前批次尚未生成放行事务，暂无事务事件" />
        </el-tab-pane>
        <el-tab-pane label="变更记录" name="change">
          <FormTraceChangeTab
            v-if="traceRecordBatchExecutionId"
            :batch-execution-id="traceRecordBatchExecutionId"
            target-scope="BATCH"
          />
          <el-empty v-else description="当前批次不存在，暂无变更记录" />
        </el-tab-pane>
        <el-tab-pane label="操作审计" name="audit">
          <OperationAuditListPane
            v-if="traceRecordBatchExecutionId"
            object-type="BATCH_EXECUTION"
            :object-id="traceRecordBatchExecutionId"
            :batch-execution-id="traceRecordBatchExecutionId"
            :show-object-filters="false"
            :hide-recordbook-mode="!isRecordbookEnabledForCurrentTask"
          />
          <el-empty v-else description="当前批次不存在，暂无操作审计记录" />
        </el-tab-pane>
        <el-tab-pane
          v-if="showFieldResponsibilityTab"
          label="字段责任"
          name="fieldResponsibility"
        >
          <div class="edhr-batch-detail__trace-field-responsibility">
            <div class="edhr-batch-detail__trace-field-responsibility-head">
              <div>
                <div class="edhr-batch-detail__trace-field-responsibility-title">单元填写责任</div>
                <div class="edhr-batch-detail__muted">
                  按真实执行记录进入现有字段责任汇总，查看首次有效填写人、当前值最后操作人和填写时间。
                </div>
              </div>
              <el-tag size="small" effect="plain">
                {{ fieldResponsibilityEntries.length }} 张表单
              </el-tag>
            </div>
            <div
              v-if="fieldResponsibilityEntries.length"
              class="edhr-batch-detail__process-evidence-grid"
            >
              <button
                v-for="entry in fieldResponsibilityEntries"
                :key="entry.key"
                type="button"
                class="edhr-batch-detail__process-evidence-item"
                @click="openFieldResponsibility(entry)"
              >
                <span>{{ entry.processName || entry.batchRecordReportName || entry.executionCode }}</span>
                <small>{{ entry.batchRecordReportName || '--' }} / {{ entry.executionCode }}</small>
                <small>{{ entry.statusLabel }} / 提交 {{ entry.submittedAtText }}</small>
                <small>查看当前责任汇总</small>
              </button>
            </div>
            <el-empty v-else description="当前批次暂无可查看字段责任的执行记录" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="域追溯" name="domain">
          <DomainTraceListPane
            v-if="traceRecordBatchExecutionId"
            :work-order-code="traceRecordWorkOrderCode"
            :batch-code="traceRecordBatchCode"
          />
          <el-empty v-else description="当前批次不存在，暂无域追溯记录" />
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <el-drawer
      v-model="routeFormDrawerVisible"
      :title="routeFormDrawerTitle"
      size="72%"
      destroy-on-close
    >
      <el-alert
        v-if="routeFormReadonly"
        title="当前账号仅有查看权限，表单保存、提交、重提和放弃操作已禁用。"
        type="info"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <el-alert
        v-if="!routeFormBusinessActionContext"
        title="当前任务缺少表单中心动作上下文，无法打开动态表单。"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <ActionFormPanel
        v-else
        :context="routeFormBusinessActionContext"
        :form-data="routeFormPanelData"
        :idempotency-key="routeFormIdempotencyKey"
        :disabled="routeFormReadonly"
        :initial-instance-id="routeFormInitialInstanceId"
        :initial-instance-code="routeFormInitialInstanceCode"
        :initial-instance-status="routeFormInitialInstanceStatus"
      />
    </el-drawer>

    <Dialog title="电子签名确认" v-model="releaseSignatureConfirmVisible" width="460px">
      <el-alert
        v-if="releaseSignatureError"
        :title="releaseSignatureError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <el-alert
        title="确认后将直接提交放行，请使用当前账号电子签名密码。"
        type="warning"
        :closable="false"
        show-icon
        class="edhr-batch-detail__dialog-alert"
      />
      <el-form label-width="110px">
        <el-form-item label="电子签名" required>
          <el-input
            v-model="releaseSignatureForm.password"
            type="password"
            show-password
            placeholder="请输入负责人电子签名密码"
            @keyup.enter="confirmReleaseSignatureSubmit"
          />
        </el-form-item>
        <el-form-item label="放行说明">
          <el-input
            v-model="releaseSignatureForm.approvalOpinion"
            type="textarea"
            :rows="3"
            placeholder="可填写放行说明，默认使用负责人电子签名放行"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="releaseSignatureConfirmVisible = false">取 消</el-button>
        <el-button type="primary" :loading="releaseSignatureSubmitting" @click="confirmReleaseSignatureSubmit">
          确认放行
        </el-button>
      </template>
    </Dialog>

    <Dialog
      :title="selectedSpecialNodePreviewTitle || '附件在线预览'"
      v-model="specialNodePreviewDialogVisible"
      width="1120px"
      destroy-on-close
    >
      <ProtectedPdfViewer
        v-if="selectedSpecialNodePreviewSource"
        :preview-source="selectedSpecialNodePreviewSource"
        :title="selectedSpecialNodePreviewTitle || '附件在线预览'"
      />
      <el-empty v-else description="暂无可预览附件" />
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessageBox, type UploadInstance, type UploadRequestOptions } from 'element-plus'
import {
  EDHR_BATCH_ARCHIVE_ARTIFACT_FINAL_PDF,
  EDHR_BATCH_STATUS_ARCHIVED,
  EDHR_BATCH_STATUS_CLOSED,
  EDHR_BATCH_STATUS_CREATED,
  EDHR_BATCH_STATUS_IN_PROGRESS,
  EDHR_BATCH_STATUS_READY_TO_CLOSE,
  EDHR_BATCH_STATUS_REWORK_REQUIRED,
  EDHR_BATCH_STATUS_REJECTED,
  EDHR_BATCH_STATUS_VOIDED,
  EDHR_BATCH_NODE_ROUTE_FORM,
  EDHR_BATCH_NODE_INCOMING_INSPECTION_REPORT,
  EDHR_BATCH_NODE_STERILIZATION_REPORT,
  EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_REPORT,
  EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_RECORD,
  EDHR_BATCH_TASK_STATUS_APPROVED,
  EDHR_BATCH_TASK_STATUS_BLOCKED,
  EDHR_BATCH_TASK_STATUS_DRAFT,
  EDHR_BATCH_TASK_STATUS_REJECTED,
  EDHR_BATCH_TASK_STATUS_REWORK_REQUIRED,
  EDHR_BATCH_TASK_STATUS_SKIPPED,
  EDHR_BATCH_TASK_STATUS_SUBMITTED,
  EDHR_BATCH_TASK_STATUS_WAITING,
  completeEdhrBatchSpecialNode,
  downloadEdhrBatchArchive,
  generateEdhrBatchArchive,
  getLatestEdhrBatchArchive,
  getEdhrBatchExecution,
  getEdhrBatchReviewTimeline,
  getEdhrBatchWorkbench,
  openEdhrBatchTask,
  deleteEdhrBatchSpecialNodePendingAttachment,
  prepareEdhrBatchSpecialNodeAttachmentUpload,
  savePendingEdhrBatchSpecialNodeAttachments,
  printEdhrBatchArchive,
  qualityRejectEdhrBatchExecution,
  reexecuteRejectedEdhrBatchExecution,
  skipEdhrBatchSpecialNode,
  syncEdhrBatchExecutionStatus,
  type EdhrBatchExecutionReviewFormViewModel,
  type EdhrBatchExecutionReviewExecutionRespVO,
  type EdhrBatchExecutionReviewSignatureRecord,
  type EdhrBatchReviewTimelineRespVO,
  type EdhrBatchWorkbenchRespVO,
  type EdhrBatchExecutionRespVO,
  type EdhrBatchExecutionTaskOpenRespVO,
  type EdhrBatchExecutionTaskRespVO,
  type EdhrBatchSpecialNodeAttachment
} from '@/api/mes/pro/edhr/batchExecution'
import {
  getEdhrReleaseCheckItemPage,
  precheckEdhrRelease,
  submitEdhrRelease,
  type EdhrReleaseCheckItemVO,
  type EdhrReleaseEventRespVO
} from '@/api/mes/pro/edhr/release'
import dayjs from 'dayjs'
import { requestReopenBatch } from '@/api/mes/pro/edhr/change'
import {
  buildEdhrSpecialNodeAttachmentPreviewSource,
  type OnlineFilePreviewSource
} from '@/api/common/filePreview'
import ProtectedPdfViewer from '@/views/dcc/controlled-file/view/index.vue'
import EdhrExecutionReadonlyForm from '@/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'
import DomainTraceListPane from '@/views/mes/pro/edhr/components/DomainTraceListPane.vue'
import OperationAuditListPane from '@/views/mes/pro/edhr/components/OperationAuditListPane.vue'
import ReleaseEventListPane from '@/views/mes/pro/edhr/components/ReleaseEventListPane.vue'
import FormTraceChangeTab from '@/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue'
import ActionFormPanel from '@/views/form-center/business-action/ActionFormPanel.vue'
import {
  resolveReleaseCheckCategoryLabel,
  resolveReleaseCheckCodeLabel,
  resolveReleaseCheckResultLabel,
  resolveReleaseCheckResultTagType,
  resolveReleaseCheckSourceObjectTypeLabel
} from '@/views/mes/pro/edhr/shared/releaseCheckPresentation'
import {
  isOptionalRouteFormTask,
  isRequiredBatchRecordTask,
  resolveBatchRequiredProgressText
} from './progress'
import { buildSignatureTimePayload, createSignatureTimeForm, type EdhrSignatureTimeForm } from '../edhr/signatureTime'
import { type BusinessActionContextVO } from '@/api/form-center/businessAction'
import {
  resolveControlledActionProjection,
  type ControlledActionProjectionVO
} from '@/api/form-center/actionProjection'
import { submitTransferIntervention } from '@/api/mes/pro/edhr/flowIntervention'
import { getEdhrRecordbookGlobalSetting } from '@/api/mes/pro/edhr/recordbookGlobalSetting'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { generateUUID } from '@/utils'
import { stringifyEdhrExecutionPageQuery } from '@/utils/edhrWorkTaskNavigation'
import { parsePositiveRouteQueryId, sameRouteQueryId } from '@/utils/routeQueryId'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'MesProEdhrBatchExecutionDetail' })

const RELEASE_VIRTUAL_PROCESS = {
  key: 'release',
  sort: '99',
  label: '放行',
  description: '收尾/放行归档'
}
const TRACE_RECORD_FIELD_RESPONSIBILITY_STATUSES = [
  EDHR_BATCH_STATUS_CLOSED,
  EDHR_BATCH_STATUS_ARCHIVED,
  EDHR_BATCH_STATUS_REJECTED,
  EDHR_BATCH_STATUS_VOIDED
] as const

const route = useRoute()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const BATCH_DETAIL_PAGE_BODY_CLASS = 'edhr-batch-detail-page'
const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'
const FLOW_TRANSFER_PERMISSION = 'mes:pro-edhr-flow-intervention:transfer'
const FLOW_TRANSFER_ADMIN_ROLES = ['super_admin']
const hasGoldenFingerPermission = computed(() => userStore.permissions.has(GOLDEN_FINGER_PERMISSION))
const hasGoldenFingerActionBypass = computed(() => hasGoldenFingerPermission.value)
const canUseFlowTransferIntervention = computed(
  () =>
    userStore.permissions.has(FLOW_TRANSFER_PERMISSION) ||
    userStore.permissions.has('*:*:*') ||
    FLOW_TRANSFER_ADMIN_ROLES.some((role) => userStore.roles.includes(role))
)
type EdhrBatchExecutionDetailFocus = 'process' | 'precheck' | 'approval'
type TraceRecordTab = 'release' | 'change' | 'audit' | 'domain' | 'fieldResponsibility'

const loading = ref(false)
const syncLoading = ref(false)
const reopenLoading = ref(false)
const reexecuteLoading = ref(false)
const qualityRejectLoading = ref(false)
const specialNodeSkipLoading = ref(false)
const specialNodeCompleteLoading = ref(false)
const specialNodeAttachmentUploading = ref(false)
const loadError = ref('')
const secondaryLoadError = ref('')
const recordbookGlobalEnabled = ref(true)
const reopenError = ref('')
const reexecuteError = ref('')
const qualityRejectError = ref('')
const specialNodeSkipError = ref('')
const specialNodeCompleteError = ref('')
const releaseActionError = ref('')
const releaseSignatureError = ref('')
const RELEASE_ACTION_ERROR_AUTO_HIDE_DELAY_MS = 5000
let releaseActionErrorAutoHideTimer: number | undefined
let routeFormAutoOpenKey = ''
const detail = ref<EdhrBatchExecutionRespVO>()
const workbench = ref<EdhrBatchWorkbenchRespVO>()
const archivePrintDrawerVisible = ref(false)
const traceRecordDrawerVisible = ref(false)
const uxChecklistDrawerVisible = ref(false)
const routeFormDrawerVisible = ref(false)
const routeFormReadonly = ref(false)
const routeFormOpenedTask = ref<EdhrBatchExecutionTaskRespVO>()
const routeFormOpenResp = ref<EdhrBatchExecutionTaskOpenRespVO>()
const releasePrecheckLoading = ref(false)
const releaseSignatureConfirmVisible = ref(false)
const releaseSignatureSubmitting = ref(false)
const releaseCheckLoading = ref(false)
const traceRecordTab = ref<TraceRecordTab>('release')
const processDetailDialogVisible = ref(false)
const reviewLoading = ref(false)
const reviewError = ref('')
const reviewTimeline = ref<EdhrBatchReviewTimelineRespVO>()
const selectedExecutionId = ref('')
const selectedTaskId = ref('')
const detailPreviewAssistMode = ref(false)
const selectedReleaseStep = ref(false)
const viewedReleaseStageKey = ref<Exclude<ReleaseStageKey, 'unknown'> | undefined>(undefined)
const releaseCheckItems = ref<EdhrReleaseCheckItemVO[]>([])
const specialNodeRailUploadRef = ref<UploadInstance>()
const fillTaskTakeoverLoading = ref<number>()
let batchDetailRequestSerial = 0
let batchDetailSecondaryFrameId: number | undefined

const UX_CHECKLIST_ITEMS = [
  {
    label: '签名清晰度',
    check: '签名人、签名时间、时区和原因在表格、弹窗和 PDF 中可读。',
    risk: '签名看不清、签名时间缺失、默认时区原因不可追溯。'
  },
  {
    label: '文字遮挡',
    check: '表格、弹窗、按钮、签名格在常用分辨率下不遮挡、不截断关键状态。',
    risk: '长工序名、批次号、错误提示被遮挡。'
  },
  {
    label: '按钮命名',
    check: '按钮文案能说明动作结果和责任边界，例如预检、追溯、打印。',
    risk: '按钮可点击但不知道会写入、只读还是跳转。'
  },
  {
    label: '单位/输入类型',
    check: '日期使用日期控件，数量使用数字控件，单位和业务语义一致。',
    risk: '日期设置成数字、批号自由文本缺少格式提示。'
  },
  {
    label: '历史记录入口',
    check: '批处理、审批、归档动作有可见历史记录或操作轨迹入口。',
    risk: '批处理没有历史记录，只能查库复盘。'
  }
]
const reopenDialogVisible = ref(false)
const reexecuteDialogVisible = ref(false)
const qualityRejectDialogVisible = ref(false)
const specialNodeSkipDialogVisible = ref(false)
const specialNodeCompleteDialogVisible = ref(false)
const currentSpecialNode = ref<EdhrBatchExecutionTaskRespVO>()
const specialNodeActionLoading = reactive<Record<number, 'skip' | 'complete' | undefined>>({})
const specialNodePendingAttachments = reactive<Record<number, EdhrBatchSpecialNodeAttachment[]>>({})
const specialNodePreviewDialogVisible = ref(false)
const selectedSpecialNodePreviewSource = ref<OnlineFilePreviewSource | null>(null)
const selectedSpecialNodePreviewTitle = ref('')
const reopenForm = reactive({
  reasonCategory: '',
  reasonText: '',
  password: '',
  comment: ''
})
const reexecuteForm = reactive({
  reason: '',
  remark: ''
})
const qualityRejectForm = reactive({
  reason: '',
  password: ''
})
const qualityRejectSignatureTimeForm = reactive<EdhrSignatureTimeForm>(createSignatureTimeForm())
const specialNodeSkipForm = reactive({
  reason: '',
  password: ''
})
const specialNodeCompleteForm = reactive({
  sterilizationBatchNo: ''
})
const reopenReasonOptions = [
  { label: '偏差处理', value: 'PROCESS_DEVIATION' },
  { label: '质量复核', value: 'QUALITY_REVIEW' },
  { label: '归档复核', value: 'ARCHIVE_REVIEW' },
  { label: '生产补充', value: 'PRODUCTION_SUPPLEMENT' }
]

const parseRouteQueryText = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' ? rawValue.trim() : ''
}

const resolveDetailFocus = (): EdhrBatchExecutionDetailFocus | undefined => {
  const focus = parseRouteQueryText(route.query.focus)
  if (focus === 'process' || focus === 'precheck' || focus === 'approval') return focus
  return undefined
}

const batchExecutionId = computed(() => parsePositiveRouteQueryId(route.query.id))
const traceRecordBatchExecutionId = computed(() =>
  parsePositiveRouteQueryId(detail.value?.id) || batchExecutionId.value
)
const traceRecordReleaseTransactionId = computed(() => workbench.value?.releaseSummary?.releaseTransactionId)
const traceRecordWorkOrderCode = computed(() => detail.value?.workOrderCode || workbench.value?.workOrderCode || '')
const traceRecordBatchCode = computed(() => detail.value?.batchCode || workbench.value?.batchCode || '')
const archiveWorkTaskId = computed(() => parsePositiveRouteQueryId(route.query.workTaskId))
const batchStatus = computed(() => Number(detail.value?.status))

type ProcessTaskGroup = {
  key: string
  routeProcessId?: number
  routeProcessSort?: number
  processCode?: string
  processName?: string
  executionMode?: string
  tasks: EdhrBatchExecutionTaskRespVO[]
  primaryTask: EdhrBatchExecutionTaskRespVO
}

const PRODUCT_INFO_PROCESS_SORT = 80
const PRODUCT_INFO_PROCESS_NAME = '产品信息'

const containsProductInfoTitle = (text?: string | number | null) => {
  const normalized = String(text ?? '')
    .trim()
    .replace(/\s+/g, '')
  return Boolean(normalized) && (normalized.includes('产品信息') || normalized.toLowerCase().includes('productinformation'))
}

const isProductInfoProcessTask = (task: EdhrBatchExecutionTaskRespVO) =>
  task.nodeType === EDHR_BATCH_NODE_ROUTE_FORM &&
  task.formSlotType === 'MAIN' &&
  task.recordCategory === 'BATCH_RECORD' &&
  (task.batchRecordSort === PRODUCT_INFO_PROCESS_SORT ||
    containsProductInfoTitle(task.batchRecordReportName) ||
    containsProductInfoTitle(task.batchRecordReportCode) ||
    containsProductInfoTitle(task.batchRecordReportId))

const buildProcessTaskGroupKey = (task: EdhrBatchExecutionTaskRespVO) => {
  if (isProductInfoProcessTask(task)) return `product-info:${task.batchRecordReportId || task.id}`
  return String(task.routeProcessId || task.routeProcessSort || task.id)
}

const resolveProcessTaskGroupName = (task: EdhrBatchExecutionTaskRespVO) =>
  isProductInfoProcessTask(task) ? PRODUCT_INFO_PROCESS_NAME : task.processName

const resolveProcessTaskGroupSort = (task: EdhrBatchExecutionTaskRespVO) =>
  isProductInfoProcessTask(task) ? PRODUCT_INFO_PROCESS_SORT : task.routeProcessSort

const sortedTasks = computed(() =>
  [...(detail.value?.tasks || [])].sort(
    (first, second) =>
      (first.routeProcessSort || 0) - (second.routeProcessSort || 0) ||
      (first.batchRecordSort || 0) - (second.batchRecordSort || 0)
  )
)
const processTaskGroups = computed<ProcessTaskGroup[]>(() => {
  const groups = new Map<string, Omit<ProcessTaskGroup, 'primaryTask'>>()
  for (const task of sortedTasks.value) {
    if (isSpecialNode(task)) continue
    const key = buildProcessTaskGroupKey(task)
    const group = groups.get(key)
    if (group) {
      group.tasks.push(task)
      continue
    }
    groups.set(key, {
      key,
      routeProcessId: task.routeProcessId,
      routeProcessSort: resolveProcessTaskGroupSort(task),
      processCode: task.processCode,
      processName: resolveProcessTaskGroupName(task),
      executionMode: task.executionMode,
      tasks: [task]
    })
  }
  return [...groups.values()]
    .map((group) => {
      const tasks = [...group.tasks].sort(
        (first, second) =>
          (first.batchRecordSort || 0) - (second.batchRecordSort || 0) ||
          first.id - second.id
      )
      return {
        ...group,
        tasks,
        primaryTask: tasks.find((task) => task.formSlotType === 'MAIN') || tasks[0]
      }
    })
    .sort(
      (first, second) =>
        (first.routeProcessSort || 0) - (second.routeProcessSort || 0) ||
        first.key.localeCompare(second.key)
    )
})
const selectedProcessTaskGroup = computed(() => {
  if (isReleaseProcessSelected.value) return undefined
  const selectedTask = selectedTaskForEvidence.value
  if (!selectedTask || isSpecialNode(selectedTask)) return undefined
  const selectedGroupKey = buildProcessTaskGroupKey(selectedTask)
  return processTaskGroups.value.find(
    (group) =>
      group.key === selectedGroupKey ||
      group.tasks.some((task) => task.id === selectedTask.id) ||
      (!isProductInfoProcessTask(selectedTask) &&
        selectedTask.routeProcessId != null &&
        group.routeProcessId === selectedTask.routeProcessId) ||
      (selectedTask.routeProcessId == null &&
        group.routeProcessSort === selectedTask.routeProcessSort &&
        group.processCode === selectedTask.processCode)
  )
})
const selectedProcessTasks = computed(() => selectedProcessTaskGroup.value?.tasks || [])
const executionReviews = computed<EdhrBatchExecutionReviewExecutionRespVO[]>(() =>
  [...(reviewTimeline.value?.executionReviews || [])].sort(
    (left, right) => (left.routeProcessSort || 0) - (right.routeProcessSort || 0)
  )
)
const SUBMITTED_EXECUTION_REVIEW_STATUSES = new Set([2, 3, 4])
const isSubmittedExecutionReview = (
  execution?: EdhrBatchExecutionReviewExecutionRespVO
): execution is EdhrBatchExecutionReviewExecutionRespVO =>
  Boolean(execution && SUBMITTED_EXECUTION_REVIEW_STATUSES.has(Number(execution.status)))
const submittedExecutionReviews = computed(() => executionReviews.value.filter(isSubmittedExecutionReview))
const isReleaseProcessSelected = computed(() => selectedReleaseStep.value)
const selectedExecution = computed(() => {
  if (isReleaseProcessSelected.value) return undefined
  return submittedExecutionReviews.value.find((execution) => String(execution.executionId) === selectedExecutionId.value)
})

type DetailPreviewSnapshotField = {
  fieldIdentity: string
  rowIndex: number
  columnIndex: number
  label: string
  helpText: string
  typeLabel: string
  required: boolean
  signature: boolean
  value: unknown
  defaultValue: unknown
}

type DetailPreviewAssistRow = {
  rowKey: string
  description: string
  sort: number
  fields: Array<{ rowIndex: number; columnIndex: number }>
}

type DetailPreviewAssistField = DetailPreviewSnapshotField & {
  assistDescription: string
  location: string
  displayValue: string
  completed: boolean
}

type DetailPreviewAssistSubjectType = 'USERS' | 'ROLE'

type DetailPreviewAssistGridKey = {
  subjectType: DetailPreviewAssistSubjectType
  subjectId: number
  subjectKey: string
  rowIndex: number
  columnIndex: number
}

type DetailPreviewAssistGridCell = {
  key: string
  rowIndex: number
  columnIndex: number
  field?: DetailPreviewAssistField
}

type DetailPreviewAssistGridRow = {
  rowIndex: number
  cells: DetailPreviewAssistGridCell[]
}

type DetailPreviewAssistGrid = {
  subjectType: DetailPreviewAssistSubjectType
  subjectId: number
  subjectKey: string
  subjectLabel: string
  rowCount: number
  columnCount: number
  rows: DetailPreviewAssistGridRow[]
}

const detailPreviewCellKey = (rowIndex: number, columnIndex: number) => `${rowIndex}:${columnIndex}`

const parseDetailPreviewNonNegativeInteger = (value: unknown) => {
  const numericValue = Number(value)
  return Number.isInteger(numericValue) && numericValue >= 0 ? numericValue : undefined
}

const parseDetailPreviewPositiveInteger = (value: unknown) => {
  const numericValue = Number(value)
  return Number.isInteger(numericValue) && numericValue > 0 ? numericValue : undefined
}

const selectedPreviewFormViewModel = computed<EdhrBatchExecutionReviewFormViewModel | undefined>(
  () => selectedExecution.value?.formViewModel
)

const selectedPreviewSnapshot = computed(() => {
  const rawSnapshot = selectedPreviewFormViewModel.value?.executionSnapshotJson
  if (typeof rawSnapshot !== 'string' || !rawSnapshot.trim()) return undefined
  try {
    return JSON.parse(rawSnapshot) as Record<string, unknown>
  } catch {
    return undefined
  }
})

const selectedPreviewCellValueMap = computed(() => {
  const map = new Map<string, unknown>()
  const rawCellValues = selectedPreviewFormViewModel.value?.cellValuesJson
  if (typeof rawCellValues !== 'string' || !rawCellValues.trim()) return map
  try {
    const parsed = JSON.parse(rawCellValues)
    if (!Array.isArray(parsed)) return map
    parsed.forEach((item) => {
      const rowIndex = parseDetailPreviewNonNegativeInteger((item as any)?.rowIndex)
      const columnIndex = parseDetailPreviewNonNegativeInteger((item as any)?.columnIndex)
      if (rowIndex == null || columnIndex == null) return
      map.set(detailPreviewCellKey(rowIndex, columnIndex), (item as any)?.value)
    })
  } catch {
    return map
  }
  return map
})

const readDetailPreviewText = (value: unknown) =>
  typeof value === 'string' && value.trim() ? value.trim() : ''

const resolveDetailPreviewFieldTypeLabel = (
  field: Record<string, unknown>,
  rawComponent: string,
  signature: boolean
) => {
  if (signature) return '签名'
  const valueType = String(field.valueType || (field as any)?.edhrCellRule?.valueType || '').toUpperCase()
  if (valueType === 'NUMBER' || rawComponent.includes('number')) return '数字'
  if (valueType === 'DATE' || rawComponent === 'date') return '日期'
  if (valueType === 'DATETIME' || rawComponent.includes('datetime')) return '日期时间'
  if (valueType === 'BOOLEAN' || rawComponent.includes('checkbox')) return '选择'
  if (rawComponent.includes('upload')) return '附件'
  return '文本'
}

const normalizeDetailPreviewSnapshotField = (
  field: Record<string, unknown>
): DetailPreviewSnapshotField | undefined => {
  const rowIndex = parseDetailPreviewNonNegativeInteger(field.rowIndex ?? (field.position as any)?.rowIndex)
  const columnIndex = parseDetailPreviewNonNegativeInteger(
    field.columnIndex ?? (field.position as any)?.columnIndex
  )
  if (rowIndex == null || columnIndex == null) return undefined
  const fieldKey = readDetailPreviewText(field.fieldKey) || `R${rowIndex + 1}C${columnIndex + 1}`
  const fieldPath = readDetailPreviewText(field.fieldPath) || `rows[${rowIndex}].cells[${columnIndex}]`
  const label =
    readDetailPreviewText(field.label) ||
    readDetailPreviewText(field.name) ||
    readDetailPreviewText(field.title) ||
    fieldKey
  const rule = (field as any)?.edhrCellRule
  const helpText = readDetailPreviewText(field.helpText) || readDetailPreviewText(rule?.helpText)
  const rawComponent = String(
    field.component || field.componentFlag || field.componentType || field.inputType || field.type || ''
  ).toLowerCase()
  const signature =
    String(field.valueType || '').toUpperCase() === 'SIGNATURE' ||
    rawComponent.includes('signature') ||
    rawComponent.includes('sign')
  const storedValue = selectedPreviewCellValueMap.value.get(detailPreviewCellKey(rowIndex, columnIndex))
  return {
    fieldIdentity: `${fieldPath}::${fieldKey}::${rowIndex}:${columnIndex}`,
    rowIndex,
    columnIndex,
    label,
    helpText,
    typeLabel: resolveDetailPreviewFieldTypeLabel(field, rawComponent, signature),
    required: field.required === true || rule?.required === true,
    signature,
    value: storedValue ?? field.value,
    defaultValue: field.defaultValue
  }
}

const selectedPreviewSnapshotFields = computed<DetailPreviewSnapshotField[]>(() => {
  const fields = selectedPreviewSnapshot.value?.fields
  if (!Array.isArray(fields)) return []
  return fields
    .map((field) =>
      field && typeof field === 'object'
        ? normalizeDetailPreviewSnapshotField(field as Record<string, unknown>)
        : undefined
    )
    .filter((field): field is DetailPreviewSnapshotField => Boolean(field))
})

const normalizeDetailPreviewAssistRows = (rows: unknown): DetailPreviewAssistRow[] => {
  if (!Array.isArray(rows)) return []
  return rows
    .map((row, index) => {
      const record = row as Record<string, unknown>
      const fields = Array.isArray(record.fields)
        ? record.fields
            .map((field) => {
              const rowIndex = parseDetailPreviewNonNegativeInteger((field as any)?.rowIndex)
              const columnIndex = parseDetailPreviewNonNegativeInteger((field as any)?.columnIndex)
              return rowIndex == null || columnIndex == null ? undefined : { rowIndex, columnIndex }
            })
            .filter((field): field is { rowIndex: number; columnIndex: number } => Boolean(field))
        : []
      return {
        rowKey: readDetailPreviewText(record.rowKey) || `ASSIST_ROW_${index + 1}`,
        description: readDetailPreviewText(record.description),
        sort: Number.isFinite(Number(record.sort)) ? Number(record.sort) : index + 1,
        fields
      }
    })
    .filter((row) => row.rowKey && row.fields.length > 0)
    .sort((left, right) => left.sort - right.sort)
}

const selectedPreviewAssistRows = computed(() =>
  normalizeDetailPreviewAssistRows(selectedPreviewSnapshot.value?.assistRows)
)

const selectedPreviewAssistRowsConfigured = computed(() => selectedPreviewAssistRows.value.length > 0)

const parseDetailPreviewAssistGridRowKey = (rowKey: string) => {
  const normalizedRowKey = String(rowKey || '').trim()
  const subjectMatch = normalizedRowKey.match(/^ASSIST_GRID_(USERS|ROLE)(\d+)_R(\d+)_C(\d+)$/)
  const legacyUserMatch = normalizedRowKey.match(/^ASSIST_GRID_U(\d+)_R(\d+)_C(\d+)$/)
  if (!subjectMatch && !legacyUserMatch) return undefined
  const subjectType: DetailPreviewAssistSubjectType = subjectMatch
    ? (subjectMatch[1] as DetailPreviewAssistSubjectType)
    : 'USERS'
  const subjectId = Number(subjectMatch ? subjectMatch[2] : legacyUserMatch?.[1])
  const rowIndex = Number(subjectMatch ? subjectMatch[3] : legacyUserMatch?.[2])
  const columnIndex = Number(subjectMatch ? subjectMatch[4] : legacyUserMatch?.[3])
  if (!Number.isInteger(subjectId) || subjectId <= 0) return undefined
  if (!Number.isInteger(rowIndex) || rowIndex < 0) return undefined
  if (!Number.isInteger(columnIndex) || columnIndex < 0) return undefined
  return {
    subjectType,
    subjectId,
    subjectKey: `${subjectType}:${subjectId}`,
    rowIndex,
    columnIndex
  } satisfies DetailPreviewAssistGridKey
}

const selectedPreviewAssistGridRows = computed(() =>
  selectedPreviewAssistRows.value.map((row) => ({
    row,
    gridKey: parseDetailPreviewAssistGridRowKey(row.rowKey)
  }))
)

const selectedPreviewAssistGridSize = computed(() => {
  const snapshotRowCount = parseDetailPreviewPositiveInteger(
    selectedPreviewSnapshot.value?.assistGridRowCount
  )
  const snapshotColumnCount = parseDetailPreviewPositiveInteger(
    selectedPreviewSnapshot.value?.assistGridColumnCount
  )
  if (snapshotRowCount && snapshotColumnCount) {
    return {
      rowCount: snapshotRowCount,
      columnCount: snapshotColumnCount
    }
  }
  const gridKeys = selectedPreviewAssistGridRows.value
    .map((item) => item.gridKey)
    .filter((gridKey): gridKey is DetailPreviewAssistGridKey => Boolean(gridKey))
  return {
    rowCount: gridKeys.length ? Math.max(...gridKeys.map((gridKey) => gridKey.rowIndex)) + 1 : 0,
    columnCount: gridKeys.length
      ? Math.max(...gridKeys.map((gridKey) => gridKey.columnIndex)) + 1
      : 0
  }
})

const hasDetailPreviewValue = (value: unknown) => {
  if (value == null) return false
  if (Array.isArray(value)) return value.length > 0
  if (typeof value === 'string') return value.trim().length > 0
  return true
}

const formatDetailPreviewValue = (value: unknown) => {
  if (!hasDetailPreviewValue(value)) return '--'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value)
    } catch {
      return String(value)
    }
  }
  return String(value)
}

const selectedPreviewAssistFields = computed<DetailPreviewAssistField[]>(() => {
  const fieldMap = new Map(
    selectedPreviewSnapshotFields.value.map((field) => [
      detailPreviewCellKey(field.rowIndex, field.columnIndex),
      field
    ])
  )
  const result: DetailPreviewAssistField[] = []
  selectedPreviewAssistRows.value.forEach((row) => {
    row.fields.forEach((cell) => {
      const field = fieldMap.get(detailPreviewCellKey(cell.rowIndex, cell.columnIndex))
      if (!field) return
      const value = hasDetailPreviewValue(field.value) ? field.value : field.defaultValue
      result.push({
        ...field,
        assistDescription: row.description,
        location: `第 ${field.rowIndex + 1} 行第 ${field.columnIndex + 1} 列`,
        displayValue: formatDetailPreviewValue(value),
        completed: !field.required || hasDetailPreviewValue(value)
      })
    })
  })
  return result
})

const selectedPreviewAssistFieldMap = computed(
  () => new Map(selectedPreviewAssistFields.value.map((field) => [
    detailPreviewCellKey(field.rowIndex, field.columnIndex),
    field
  ]))
)

const selectedPreviewAssistGridErrors = computed(() => {
  const errors: string[] = []
  const occupiedGridCells = new Set<string>()
  selectedPreviewAssistGridRows.value.forEach(({ row, gridKey }) => {
    if (!gridKey) {
      errors.push(`辅助配置 ${row.rowKey} 缺少正式辅助表格坐标`)
      return
    }
    if (row.fields.length !== 1) {
      errors.push(`辅助格 ${row.rowKey} 必须且只能映射一个原表字段`)
      return
    }
    const { rowCount, columnCount } = selectedPreviewAssistGridSize.value
    if (gridKey.rowIndex >= rowCount || gridKey.columnIndex >= columnCount) {
      errors.push(`辅助格 ${row.rowKey} 超出辅助表格尺寸`)
      return
    }
    const occupiedKey = `${gridKey.subjectKey}:${gridKey.rowIndex}:${gridKey.columnIndex}`
    if (occupiedGridCells.has(occupiedKey)) {
      errors.push(`辅助格 ${row.rowKey} 坐标重复`)
      return
    }
    occupiedGridCells.add(occupiedKey)
    const sourceField = row.fields[0]
    if (!selectedPreviewAssistFieldMap.value.has(
      detailPreviewCellKey(sourceField.rowIndex, sourceField.columnIndex)
    )) {
      errors.push(`辅助格 ${row.rowKey} 对应的原表字段不存在`)
    }
  })
  return Array.from(new Set(errors))
})

const resolveDetailPreviewAssistSubjectLabel = (
  subjectType: DetailPreviewAssistSubjectType,
  subjectId: number
) => {
  if (subjectType === 'USERS') {
    const user = selectedProcessTasks.value
      .flatMap((task) => task.fillableUsers || [])
      .find((item) => Number(item.userId) === subjectId)
    return user?.displayName || `个人 ${subjectId}`
  }
  return `角色 ${subjectId}`
}

const buildDetailPreviewAssistGridCellKey = (
  subjectType: DetailPreviewAssistSubjectType,
  subjectId: number,
  rowIndex: number,
  columnIndex: number
) => `ASSIST_GRID_${subjectType}${subjectId}_R${rowIndex}_C${columnIndex}`

const selectedPreviewAssistGrids = computed<DetailPreviewAssistGrid[]>(() => {
  if (selectedPreviewAssistGridErrors.value.length) return []
  const { rowCount, columnCount } = selectedPreviewAssistGridSize.value
  if (!rowCount || !columnCount) return []
  const subjects = new Map<
    string,
    {
      subjectType: DetailPreviewAssistSubjectType
      subjectId: number
      fieldMap: Map<string, DetailPreviewAssistField>
    }
  >()
  selectedPreviewAssistGridRows.value.forEach(({ row, gridKey }) => {
    if (!gridKey) return
    const sourceField = row.fields[0]
    const field = selectedPreviewAssistFieldMap.value.get(
      detailPreviewCellKey(sourceField.rowIndex, sourceField.columnIndex)
    )
    if (!field) return
    if (!subjects.has(gridKey.subjectKey)) {
      subjects.set(gridKey.subjectKey, {
        subjectType: gridKey.subjectType,
        subjectId: gridKey.subjectId,
        fieldMap: new Map()
      })
    }
    subjects.get(gridKey.subjectKey)?.fieldMap.set(
      detailPreviewCellKey(gridKey.rowIndex, gridKey.columnIndex),
      field
    )
  })
  return Array.from(subjects.entries()).map(([subjectKey, subject]) => ({
    subjectType: subject.subjectType,
    subjectId: subject.subjectId,
    subjectKey,
    subjectLabel: resolveDetailPreviewAssistSubjectLabel(subject.subjectType, subject.subjectId),
    rowCount,
    columnCount,
    rows: Array.from({ length: rowCount }, (_, rowIndex) => ({
      rowIndex,
      cells: Array.from({ length: columnCount }, (_, columnIndex) => ({
        key: buildDetailPreviewAssistGridCellKey(
          subject.subjectType,
          subject.subjectId,
          rowIndex,
          columnIndex
        ),
        rowIndex,
        columnIndex,
        field: subject.fieldMap.get(detailPreviewCellKey(rowIndex, columnIndex))
      }))
    }))
  }))
})

const effectiveDetailPreviewAssistMode = computed(
  () => detailPreviewAssistMode.value && selectedPreviewAssistRowsConfigured.value
)

const isSameRouteFormTask = (
  task: EdhrBatchExecutionTaskRespVO,
  execution: EdhrBatchExecutionReviewExecutionRespVO
) =>
  task.executionId === execution.executionId ||
  Boolean(
    task.formBindingKey &&
      execution.formBindingKey &&
      task.routeProcessSort === execution.routeProcessSort &&
      task.formBindingKey === execution.formBindingKey
  ) ||
  Boolean(
    task.batchRecordReportId &&
      execution.batchRecordReportId &&
      task.routeProcessSort === execution.routeProcessSort &&
      task.batchRecordReportId === execution.batchRecordReportId
  ) ||
  Boolean(
    task.batchRecordReportId &&
      execution.batchRecordReportId &&
      task.processCode === execution.processCode &&
      task.batchRecordReportId === execution.batchRecordReportId
  )
const selectedTaskForExecution = computed(() => {
  const execution = selectedExecution.value
  if (!execution) return undefined
  return sortedTasks.value.find((task) => isSameRouteFormTask(task, execution))
})
const selectedTaskForEvidence = computed(() => {
  if (isReleaseProcessSelected.value) return undefined
  if (selectedTaskId.value) {
    return sortedTasks.value.find((task) => String(task.id) === selectedTaskId.value)
  }
  return selectedTaskForExecution.value || sortedTasks.value[0]
})
const specialTaskEntries = computed(() =>
  sortedTasks.value.filter((task) => isSpecialNode(task))
)
const preProcessSpecialTaskEntries = computed(() =>
  specialTaskEntries.value.filter(
    (task) => task.nodeType === EDHR_BATCH_NODE_INCOMING_INSPECTION_REPORT
  )
)
const postProcessSpecialTaskEntries = computed(() =>
  specialTaskEntries.value.filter(
    (task) => task.nodeType !== EDHR_BATCH_NODE_INCOMING_INSPECTION_REPORT
  )
)
const selectedOpenableTask = computed(() => {
  if (isReleaseProcessSelected.value) return undefined
  const task = selectedTaskForExecution.value || selectedTaskForEvidence.value
  return task && canOpenTask(task) ? task : undefined
})
const selectedProcessContext = computed(() => selectedExecution.value || selectedTaskForEvidence.value)
const routeFormInitialInstanceId = computed(
  () => routeFormOpenResp.value?.formCenterInstanceId || routeFormOpenedTask.value?.formCenterInstanceId
)
const routeFormInitialInstanceCode = computed(() =>
  routeFormInitialInstanceId.value ? String(routeFormInitialInstanceId.value) : ''
)
const routeFormInitialInstanceStatus = computed(() => 'DRAFT' as const)
const routeFormDrawerTitle = computed(() => {
  const task = routeFormOpenedTask.value
  if (!task) return routeFormReadonly.value ? '查看动态表单' : '填写动态表单'
  return `${routeFormReadonly.value ? '查看表单' : '填写表单'}：${resolveTaskDisplayName(task)}`
})
const routeFormBusinessActionContext = computed<BusinessActionContextVO | null>(() => {
  const batch = detail.value
  const task = routeFormOpenedTask.value
  const formBindingKey = routeFormOpenResp.value?.formBindingKey || task?.formBindingKey
  if (!batch?.routeVersionId || !task?.id || !formBindingKey) return null
  return {
    dataDomain: 'MES',
    systemCode: 'MES',
    objectType: 'EDHR_ROUTE_FORM',
    objectId: String(task.id),
    objectVersion: String(batch.routeVersionId),
    actionCode: `EDHR_RF_${batch.routeVersionId}_${formBindingKey}`,
    objectState: 'ACTIVE',
    orgCode: '',
    deptCode: String(userStore.user?.deptId || ''),
    roleCodes: userStore.roles,
    productCode: batch.productCode || '',
    categoryCode: '',
    reason: 'eDHR route form fill'
  }
})
const routeFormPanelData = computed<Record<string, unknown>>(() => {
  const batch = detail.value
  const task = routeFormOpenedTask.value
  const opened = routeFormOpenResp.value
  return {
    batchExecutionId: batch?.id,
    batchExecutionCode: batch?.batchExecutionCode,
    batchCode: batch?.batchCode,
    workOrderCode: batch?.workOrderCode,
    routeId: batch?.routeId,
    routeVersionId: batch?.routeVersionId,
    routeProcessId: task?.routeProcessId || opened?.routeProcessId,
    processCode: task?.processCode,
    processName: task?.processName,
    batchTaskId: task?.id || opened?.taskId,
    workTaskId: opened?.workTaskId,
    formBindingKey: opened?.formBindingKey || task?.formBindingKey,
    formTemplateId: opened?.formTemplateId || task?.formTemplateId,
    formTemplateName: opened?.formTemplateName || task?.formTemplateName,
    formTemplateVersionId: opened?.formTemplateVersionId || task?.formTemplateVersionId,
    formTemplateVersionNo: opened?.formTemplateVersionNo || task?.formTemplateVersionNo,
    formTemplateJimuSchemaJson: opened?.formTemplateJimuSchemaJson,
    formTemplateRecognizedFields: opened?.formTemplateRecognizedFields,
    formCenterInstanceId: routeFormInitialInstanceId.value
  }
})
const routeFormIdempotencyKey = computed(() => {
  const task = routeFormOpenedTask.value
  const formBindingKey = routeFormOpenResp.value?.formBindingKey || task?.formBindingKey || 'FORM'
  return `EDHR_ROUTE_FORM:${detail.value?.id || batchExecutionId.value}:${task?.id || 'TASK'}:${formBindingKey}`
})
const hasReleaseTransaction = computed(() => Boolean(workbench.value?.releaseSummary?.releaseTransactionId))
const showFieldResponsibilityTab = computed(
  () =>
    TRACE_RECORD_FIELD_RESPONSIBILITY_STATUSES.includes(
      batchStatus.value as (typeof TRACE_RECORD_FIELD_RESPONSIBILITY_STATUSES)[number]
    ) || hasReleaseTransaction.value
)
const releaseStatus = computed(() => workbench.value?.releaseSummary?.releaseStatus || 'PRECHECK_REQUIRED')
const releasePrecheckPassed = computed(() => releaseStatus.value === 'PRECHECK_PASSED')
const releasePendingApproval = computed(() => releaseStatus.value === 'PENDING_APPROVAL')
const releaseCanSubmitBatchStatus = computed(
  () =>
    batchStatus.value === EDHR_BATCH_STATUS_READY_TO_CLOSE ||
    batchStatus.value === EDHR_BATCH_STATUS_CLOSED
)
const RELEASE_ACTION_LOCKED_MESSAGE = '放行审批中，只能处理放行审批或撤回放行。'
const releaseActionLocked = computed(
  () =>
    !hasGoldenFingerActionBypass.value &&
    (detail.value?.releaseActionLocked === true || releasePendingApproval.value)
)
const releaseActionLockMessage = computed(
  () => detail.value?.releaseActionLockReason || RELEASE_ACTION_LOCKED_MESSAGE
)
const PENDING_VOID_ACTION_LOCKED_MESSAGE = '作废申请待处理，只能撤回作废申请。'
const pendingVoidActionLocked = computed(() => Boolean(detail.value?.pendingVoidChangeEventId))
const pendingVoidActionLockMessage = computed(() =>
  detail.value?.pendingVoidChangeCode
    ? `当前批次已有作废申请 ${detail.value.pendingVoidChangeCode} 正在审批，只能撤回作废申请。`
    : PENDING_VOID_ACTION_LOCKED_MESSAGE
)
const edhrReleaseActionProjection = computed(() =>
  resolveEdhrBatchActionProjection('EDHR_RELEASE', '提交放行', hasReleaseTransaction.value && releasePrecheckPassed.value && releaseCanSubmitBatchStatus.value, {
    locked: releaseActionLocked.value,
    pending: releasePendingApproval.value,
    pendingInstanceId:
      releasePendingApproval.value
        ? workbench.value?.releaseSummary?.releaseTransactionId
        : undefined,
    pendingStatus: releasePendingApproval.value ? releaseStatus.value : undefined,
    lockReason: releaseActionLockMessage.value,
    disabledReason: hasReleaseTransaction.value
      ? releaseCanSubmitBatchStatus.value
        ? releaseActionLockMessage.value
        : '当前批次尚未满足放行状态。'
      : '当前批次尚未生成放行事务。'
  })
)
const edhrVoidActionProjection = computed(() =>
  resolveEdhrBatchActionProjection('EDHR_BATCH_VOID', '作废申请', !pendingVoidActionLocked.value, {
    pending: pendingVoidActionLocked.value,
    pendingInstanceId: detail.value?.pendingVoidChangeEventId,
    pendingStatus: detail.value?.pendingVoidChangeStatus,
    disabledReason: pendingVoidActionLockMessage.value
  })
)
const batchActionLocked = computed(
  () => !hasGoldenFingerActionBypass.value && (pendingVoidActionLocked.value || releaseActionLocked.value)
)
const batchActionLockMessage = computed(() =>
  pendingVoidActionLocked.value
    ? edhrVoidActionProjection.value.blockerMessage
    : edhrReleaseActionProjection.value.blockerMessage || releaseActionLockMessage.value
)
const resolveEdhrBatchActionProjection = (
  actionCode: string,
  actionLabel: string,
  allowed: boolean,
  overrides: Partial<ControlledActionProjectionVO> = {}
) => {
  const needsSharedBlocker =
    batchActionLocked.value &&
    !overrides.blockerReason &&
    !overrides.lockReason &&
    !overrides.disabledReason
  return resolveControlledActionProjection(
    {
      actionCode,
      actionLabel,
      allowed,
      permissionGranted: allowed,
      locked: batchActionLocked.value,
      pending: batchActionLocked.value,
      blockerReason: needsSharedBlocker ? batchActionLockMessage.value : undefined,
      ...overrides
    },
    actionLabel
  )
}
const archiveProjectionState = computed(() =>
  resolveEdhrBatchActionProjection(
    'ARCHIVE',
    '归档',
    detail.value?.canArchive === true && batchStatus.value !== EDHR_BATCH_STATUS_ARCHIVED
  )
)
const qualityRejectProjectionState = computed(() =>
  resolveEdhrBatchActionProjection(
    'QUALITY_REJECT',
    '质量拒收',
    Boolean(detail.value?.id) &&
      batchStatus.value !== EDHR_BATCH_STATUS_ARCHIVED &&
      batchStatus.value !== EDHR_BATCH_STATUS_REJECTED
  )
)
const canGenerateArchive = computed(
  () => archiveProjectionState.value.allowed
)
const canQualityReject = computed(
  () =>
    qualityRejectProjectionState.value.allowed &&
    releaseStatus.value !== 'RELEASED' &&
    (resolveReleaseStageKey() === 'precheck' || resolveReleaseStageKey() === 'release-approval')
)
const isQualityTerminalStage = computed(() => batchStatus.value === EDHR_BATCH_STATUS_REJECTED)
const canRequestReopen = computed(
  () =>
    Boolean(detail.value?.id) &&
    (!batchActionLocked.value || hasGoldenFingerActionBypass.value)
)
const canReexecuteRejectedBatch = computed(
  () =>
    Boolean(detail.value?.id) &&
    isQualityTerminalStage.value &&
    (!batchActionLocked.value || hasGoldenFingerActionBypass.value)
)
const canOpenArchivePrintDrawer = computed(() => !batchActionLocked.value || hasGoldenFingerActionBypass.value)
const canRunReleasePrecheck = computed(
  () =>
    Boolean(detail.value?.id) &&
    ![
      EDHR_BATCH_STATUS_ARCHIVED,
      EDHR_BATCH_STATUS_REJECTED,
      EDHR_BATCH_STATUS_VOIDED
    ].includes(batchStatus.value) &&
    !['PENDING_APPROVAL', 'RELEASED'].includes(String(releaseStatus.value || '')) &&
    (!batchActionLocked.value || hasGoldenFingerActionBypass.value)
)
const canSubmitRelease = computed(() => edhrReleaseActionProjection.value.allowed)

type ReleaseStageKey =
  | 'quality-terminal'
  | 'archived'
  | 'release-approval'
  | 'archive'
  | 'precheck'
  | 'unknown'

type ReleaseStageViewModel = {
  key: ReleaseStageKey
  stageLabel: string
  badgeLabel: string
  tagType: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  description: string
  releaseStatusTitle: string
  releaseStatusLabel: string
  nextOwnerLabel: string
  nextStepText: string
}

type BatchCurrentPositionDiagnosis = {
  blockerLabel: string
  blockerReason: string
  nextActionText: string
}


type ReleaseStageActionItem = {
  key: string
  label: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  permission?: string[]
  disabled?: boolean
  loading?: boolean
  readonlyAllowed?: boolean
  onClick: () => void | Promise<void>
}

const RELEASE_OWNER_ROLE_LABELS: Record<string, string> = {
  PRODUCTION: '生产负责人',
  QUALITY: '质量负责人',
  EQUIPMENT: '设备负责人',
  QA: '质量放行责任人',
  ARCHIVE: '归档员',
  RELEASE_APPROVE: '放行审批负责人'
}

const resolveReleaseOwnerRoleLabel = (role?: string | null) => {
  const normalizedRole = String(role || '').trim()
  if (!normalizedRole) return ''
  return RELEASE_OWNER_ROLE_LABELS[normalizedRole] || normalizedRole
}

const resolveStageOwnerLabel = (defaultOwner: string) =>
  resolveReleaseOwnerRoleLabel(workbench.value?.stageOwnerRole || detail.value?.stageOwnerRole) || defaultOwner

const resolveReleaseOwnerLabel = () => {
  const releaseOwnerLabel = String(workbench.value?.releaseSummary?.releaseOwnerLabel || '').trim()
  return releaseOwnerLabel || '放行责任人未配置'
}

const resolveReleaseStageKey = (): ReleaseStageKey => {
  if (batchStatus.value === EDHR_BATCH_STATUS_REJECTED) return 'quality-terminal'
  if (batchStatus.value === EDHR_BATCH_STATUS_ARCHIVED) return 'archived'
  if (releaseStatus.value === 'RELEASED') return 'archive'
  if (releaseStatus.value === 'PENDING_APPROVAL' || releasePrecheckPassed.value) return 'release-approval'
  if (
    batchStatus.value === EDHR_BATCH_STATUS_CLOSED ||
    batchStatus.value === EDHR_BATCH_STATUS_READY_TO_CLOSE
  ) return 'precheck'
  if (
    batchStatus.value === EDHR_BATCH_STATUS_IN_PROGRESS ||
    batchStatus.value === EDHR_BATCH_STATUS_REWORK_REQUIRED ||
    batchStatus.value === EDHR_BATCH_STATUS_CREATED
  ) {
    return 'precheck'
  }
  return 'unknown'
}

const resolveStageAwareReleaseStatusTitle = (stageKey: ReleaseStageKey = resolveReleaseStageKey()) =>
  stageKey === 'unknown' ? '当前阶段状态' : '当前放行状态'

const actualReleaseStageKey = computed(resolveReleaseStageKey)

const resolveReleaseStageViewModel = (stageKey: ReleaseStageKey): ReleaseStageViewModel => {
  const releaseStatusLabel = resolveStageAwareReleaseStatusSummary(stageKey)
  const releaseStatusTitle = resolveStageAwareReleaseStatusTitle(stageKey)
  const currentStageOwner = resolveStageOwnerLabel('当前阶段责任人')
  switch (stageKey) {
    case 'quality-terminal':
      return {
        key: 'quality-terminal',
        stageLabel: '质量已拒收',
        badgeLabel: resolveBatchStatusLabel(batchStatus.value),
        tagType: 'danger',
        description: '质量已拒收，批次停止普通放行审批。',
        releaseStatusTitle,
        releaseStatusLabel,
        nextOwnerLabel: resolveStageOwnerLabel('质量负责人'),
        nextStepText: '如需推进，请联系质量负责人确认拒收结论；如属误拒收，按重开流程处理。'
      }
    case 'archived':
      return {
        key: 'archived',
        stageLabel: '已归档',
        badgeLabel: resolveBatchStatusLabel(batchStatus.value),
        tagType: 'success',
        description: '批次已完成归档，当前阶段只需要查看归档和追溯证据。',
        releaseStatusTitle,
        releaseStatusLabel,
        nextOwnerLabel: resolveStageOwnerLabel('归档员'),
        nextStepText: '如需打印、下载或核对证据，请联系归档员或记录管理员。'
      }
    case 'release-approval':
      return {
        key: 'release-approval',
        stageLabel: '放行',
        badgeLabel: canSubmitRelease.value ? '可放行' : releaseStatusLabel,
        tagType: 'primary',
        description: '预检已通过，当前重点是核对预检项并执行拒收或电子签名放行。',
        releaseStatusTitle,
        releaseStatusLabel,
        nextOwnerLabel: resolveReleaseOwnerLabel(),
        nextStepText: '请确认预检列表后在右侧执行“拒收”或“放行”。'
      }
    case 'archive':
      return {
        key: 'archive',
        stageLabel: '归档打印',
        badgeLabel: resolveArchiveStatusSummary(),
        tagType: 'success',
        description: '放行已完成，当前阶段重点是生成、打印或下载最终归档。',
        releaseStatusTitle,
        releaseStatusLabel,
        nextOwnerLabel: resolveStageOwnerLabel('归档员'),
        nextStepText: '请联系归档员处理最终归档任务，并在追溯记录中核对归档证据。'
      }
    case 'precheck':
      return {
        key: 'precheck',
        stageLabel: '放行预检',
        badgeLabel: releaseStatusLabel,
        tagType: 'warning',
        description: '先执行放行预检；预检失败时继续修订后重跑。',
        releaseStatusTitle,
        releaseStatusLabel: resolveStageAwareReleaseStatusSummary('precheck'),
        nextOwnerLabel: resolveReleaseOwnerLabel(),
        nextStepText: '请联系质量放行责任人处理预检失败项或执行放行预检。'
      }
    default:
      return {
        key: 'unknown',
        stageLabel: '状态待确认',
        badgeLabel: resolveBatchStatusLabel(batchStatus.value),
        tagType: 'info',
        description: '当前批次状态未被放行阶段识别，需要先核对后端状态和工作台摘要。',
        releaseStatusTitle,
        releaseStatusLabel,
        nextOwnerLabel: currentStageOwner,
        nextStepText: '请联系当前阶段责任人或系统管理员核对状态数据后再推进。'
      }
  }
}

const releaseStageViewModel = computed<ReleaseStageViewModel>(() =>
  resolveReleaseStageViewModel(actualReleaseStageKey.value)
)
const viewedReleaseStageViewModel = computed<ReleaseStageViewModel>(() =>
  resolveReleaseStageViewModel(viewedReleaseStageKey.value || actualReleaseStageKey.value)
)
const isViewedReleaseStageReadonly = computed(
  () => isReleaseProcessSelected.value && viewedReleaseStageViewModel.value.key !== actualReleaseStageKey.value
)
const viewedReleaseStageReadonlyMessage = computed(
  () =>
    `当前查看的是${viewedReleaseStageViewModel.value.stageLabel}阶段，不是当前流程阶段；只能查看，不能操作。`
)
const canUseViewedReleaseStageActions = computed(() => !isViewedReleaseStageReadonly.value)
const releaseStagePanelHint = computed(() =>
  isViewedReleaseStageReadonly.value
    ? '只读查看：非当前流程阶段不会开放操作按钮。'
    : ''
)
const releaseStageOwnerLabel = computed(
  () =>
    viewedReleaseStageViewModel.value.nextOwnerLabel ||
    '当前阶段责任人'
)

const clearReleaseActionErrorAutoHideTimer = () => {
  if (!releaseActionErrorAutoHideTimer) return
  window.clearTimeout(releaseActionErrorAutoHideTimer)
  releaseActionErrorAutoHideTimer = undefined
}

const clearReleaseActionError = () => {
  clearReleaseActionErrorAutoHideTimer()
  releaseActionError.value = ''
}

const showReleaseActionError = (errorText: string) => {
  clearReleaseActionErrorAutoHideTimer()
  releaseActionError.value = errorText
  releaseActionErrorAutoHideTimer = window.setTimeout(() => {
    if (releaseActionError.value === errorText) {
      clearReleaseActionError()
    }
  }, RELEASE_ACTION_ERROR_AUTO_HIDE_DELAY_MS)
}

const ensureViewedReleaseStageWritable = (targetText = '当前动作') => {
  if (canUseViewedReleaseStageActions.value) return true
  const errorText = `${viewedReleaseStageReadonlyMessage.value}${targetText}仅允许在当前流程阶段执行。`
  showReleaseActionError(errorText)
  releaseSignatureError.value = errorText
  message.error(errorText)
  return false
}

const normalizePositionText = (value: unknown) => (value == null ? '' : String(value).trim())

const resolveNameWithCode = (name: unknown, code: unknown) => {
  const normalizedName = normalizePositionText(name)
  const normalizedCode = normalizePositionText(code)
  if (normalizedName && normalizedCode && normalizedName !== normalizedCode) {
    return `${normalizedName}（${normalizedCode}）`
  }
  return normalizedName || normalizedCode
}

const resolveCurrentProcessStepLabel = () => {
  const currentProcessLabel = resolveNameWithCode(detail.value?.currentProcessName, detail.value?.currentProcessCode)
  if (currentProcessLabel) return currentProcessLabel

  const selectedTask = selectedTaskForEvidence.value
  if (selectedTask) {
    const selectedTaskProcessLabel = resolveNameWithCode(selectedTask.processName, selectedTask.processCode)
    if (selectedTaskProcessLabel) return selectedTaskProcessLabel
    const selectedTaskTitle = resolvePendingTaskTitle(selectedTask)
    if (selectedTaskTitle && selectedTaskTitle !== '--') return selectedTaskTitle
  }

  return releaseStageViewModel.value.stageLabel || '--'
}

const resolveCurrentPositionTaskLabel = () => {
  const selectedTask = selectedTaskForEvidence.value
  if (selectedTask) {
    const selectedTaskName = resolveTaskDisplayName(selectedTask)
    if (selectedTaskName && selectedTaskName !== '--') return selectedTaskName
  }

  const selectedExecutionName = normalizePositionText(
    selectedExecution.value?.batchRecordReportName ||
      selectedExecution.value?.batchRecordReportCode ||
      selectedExecution.value?.batchRecordReportId
  )
  if (selectedExecutionName) return selectedExecutionName

  if (isReleaseProcessSelected.value) return RELEASE_VIRTUAL_PROCESS.label
  return '--'
}

const resolveCurrentProcessOwnerGroupsText = () => {
  const groups = [
    ['生产', detail.value?.currentProcessProductionFillers],
    ['设备', detail.value?.currentProcessEquipmentFillers],
    ['质量', detail.value?.currentProcessQualityFillers]
  ]
    .map(([label, fillers]) => {
      const names = resolveFillerNames(fillers as EdhrBatchExecutionRespVO['currentProcessProductionFillers'])
      return names ? `${label}：${names}` : ''
    })
    .filter(Boolean)
  return groups.join('；')
}

const resolveCurrentPositionOwnerLabel = () => {
  const processOwnerLabel = resolveCurrentProcessOwnerGroupsText()
  return (
    releaseStageViewModel.value.nextOwnerLabel ||
    processOwnerLabel ||
    resolveReleaseOwnerRoleLabel(workbench.value?.stageOwnerRole || detail.value?.stageOwnerRole) ||
    '当前阶段责任人'
  )
}

const compactPositionText = (items: unknown[]) => {
  const normalized = items
    .flatMap((item) => (Array.isArray(item) ? item : [item]))
    .map((item) => normalizePositionText(item))
    .filter(Boolean)
  return Array.from(new Set(normalized))
}

const resolveCurrentPositionBlockers = () =>
  compactPositionText([
    detail.value?.closeBlockers,
    detail.value?.stageBlockers,
    workbench.value?.stageBlockers
  ])

const hasCurrentPositionBlockers = () => resolveCurrentPositionBlockers().length > 0

const resolveCurrentPositionBlockerReason = (fallbackReason: string) => {
  const blockers = resolveCurrentPositionBlockers()
  if (!blockers.length) return fallbackReason
  const visibleBlockers = blockers.slice(0, 2).join('；')
  return blockers.length > 2 ? `${visibleBlockers}；另有 ${blockers.length - 2} 项阻塞` : visibleBlockers
}

const closeBlockerStatePattern = /未完成|未提交|未签名|未处理|待处理|缺失|不完整|阻塞|失败|异常|PENDING|BLOCKED/i

const normalizeCurrentBlockingStepCandidate = (rawText: string) => {
  let candidate = normalizePositionText(rawText)
  if (!candidate) return ''

  const statusIndex = candidate.search(closeBlockerStatePattern)
  if (statusIndex > 0) candidate = candidate.slice(0, statusIndex)
  candidate = normalizePositionText(candidate.replace(/[：:].*$/, ''))
  candidate = normalizePositionText(candidate.replace(/(卷宗项|检查项|工序|待办|任务|表单)$/g, ''))

  if (!candidate || candidate.length > 20 || /批次|存在|证据|关闭/.test(candidate)) return ''
  return candidate
}

const resolveCurrentBlockingStepFromText = (blocker: string) => {
  const blockerText = normalizePositionText(blocker)
  if (!blockerText) return ''

  const segments = blockerText
    .split(/[；;，,。]/)
    .map((segment) => normalizePositionText(segment))
    .filter(Boolean)
  const matchedSegments = segments.filter((segment) => closeBlockerStatePattern.test(segment))
  const sourceSegment = matchedSegments.length
    ? matchedSegments[matchedSegments.length - 1]
    : segments[segments.length - 1] || blockerText
  const detailSegment = sourceSegment.includes('：')
    ? sourceSegment.slice(sourceSegment.lastIndexOf('：') + 1)
    : sourceSegment

  return normalizeCurrentBlockingStepCandidate(detailSegment)
}

const resolveCurrentBlockingStepLabel = () => {
  // 示例：成品检卷宗项未完成: PENDING -> 成品检。
  for (const blocker of resolveCurrentPositionBlockers()) {
    const stepLabel = resolveCurrentBlockingStepFromText(blocker)
    if (stepLabel) return stepLabel
  }
  return '预检前检查'
}

const resolveCurrentPositionDiagnosis = (currentStepLabel: string): BatchCurrentPositionDiagnosis => {
  const stageKey = releaseStageViewModel.value.key
  const currentStepSuffix =
    currentStepLabel && currentStepLabel !== '--' && currentStepLabel !== releaseStageViewModel.value.stageLabel
      ? `：${currentStepLabel}`
      : ''

  if (stageKey === 'quality-terminal') {
    return {
      blockerLabel: '质量已拒收',
      blockerReason: '质量已拒收，不能继续普通放行审批。',
      nextActionText: '联系质量负责人确认拒收结论；如属误拒收，按重开流程处理。'
    }
  }

  if (stageKey === 'archived') {
    return {
      blockerLabel: '流程已完成',
      blockerReason: '批次已归档，普通放行链路已结束。',
      nextActionText: '如需核对证据，请查看右侧追溯记录或归档文件。'
    }
  }

  if (stageKey === 'release-approval') {
    return {
      blockerLabel: '等待放行',
      blockerReason: '预检已通过，当前可执行拒收或电子签名放行。',
      nextActionText: '确认预检列表后，在右侧点击“拒收”或“放行”。'
    }
  }

  if (stageKey === 'archive') {
    return {
      blockerLabel: '等待归档打印',
      blockerReason: '放行已完成，最终归档还未生成或封存。',
      nextActionText: '联系归档员在右侧“归档打印”中生成、打印或下载归档。'
    }
  }

  if (stageKey === 'precheck') {
    if (batchStatus.value === EDHR_BATCH_STATUS_READY_TO_CLOSE && hasCurrentPositionBlockers()) {
      return {
        blockerLabel: resolveCurrentBlockingStepLabel(),
        blockerReason: resolveCurrentPositionBlockerReason('批次还有预检前置项未完成，暂时不能进入正式放行。'),
        nextActionText: '先处理上述阻塞项；处理完成后点击“同步状态”，再执行主区域“预检”并按结果放行。'
      }
    }

    if (batchStatus.value === EDHR_BATCH_STATUS_REWORK_REQUIRED) {
      return {
        blockerLabel: `返工收尾未完成${currentStepSuffix}`,
        blockerReason: resolveCurrentPositionBlockerReason('批次存在返工或需修订任务，尚未满足预检条件。'),
        nextActionText: '先处理返工/修订任务；完成后点击“同步状态”，满足条件后执行预检/放行。'
      }
    }

    if (
      batchStatus.value === EDHR_BATCH_STATUS_IN_PROGRESS ||
      batchStatus.value === EDHR_BATCH_STATUS_CREATED
    ) {
      return {
        blockerLabel: `工序收尾未完成${currentStepSuffix}`,
        blockerReason: resolveCurrentPositionBlockerReason('当前工序或必填表单还未全部收尾，批次尚未满足预检条件。'),
        nextActionText: '先完成当前工序/表单；完成后点击“同步状态”，满足条件后执行预检/放行。'
      }
    }

    return {
      blockerLabel: '等待放行预检',
      blockerReason: resolveCurrentPositionBlockerReason(resolveReleasePrecheckSummary() || '尚未通过放行预检。'),
      nextActionText: '点击主区域“预检”，处理失败项后重新执行预检；通过后在右侧放行。'
    }
  }

  return {
    blockerLabel: '状态待确认',
    blockerReason: '当前批次状态未被放行阶段识别，需要先核对后端状态和工作台摘要。',
    nextActionText: '联系当前阶段责任人或系统管理员核对状态数据后再推进。'
  }
}

const qualityTerminalReleaseActionItems = (): ReleaseStageActionItem[] => [
  {
    key: 'reopen-original-rejected-batch',
    label: '申请重开原记录',
    type: 'warning',
    permission: ['mes:pro-edhr-change:reopen'],
    disabled: !canRequestReopen.value,
    onClick: openReopenBatchDialog
  },
  {
    key: 'reexecute-rejected-same-batch',
    label: '重新执行同批号',
    type: 'danger',
    permission: ['mes:pro-edhr-batch-execution:create'],
    disabled: !canReexecuteRejectedBatch.value,
    onClick: openReexecuteRejectedBatchDialog
  }
]

const buildReleaseStageActionItems = (stageKey: ReleaseStageKey): ReleaseStageActionItem[] => {
  if (stageKey === 'quality-terminal') {
    return qualityTerminalReleaseActionItems()
  }
  if (stageKey === 'archived') {
    return [
      {
        key: 'archive-print',
        label: '归档打印',
        type: 'primary',
        readonlyAllowed: true,
        disabled: !canOpenArchivePrintDrawer.value,
        onClick: openArchivePrintDrawer
      }
    ]
  }
  if (stageKey === 'archive') {
    return [
      {
        key: 'archive-print',
        label: '归档打印',
        type: 'primary',
        readonlyAllowed: true,
        disabled: !canOpenArchivePrintDrawer.value,
        onClick: openArchivePrintDrawer
      }
    ]
  }
  if (stageKey === 'precheck' || stageKey === 'release-approval') {
    return buildReleaseDecisionActionItems()
  }
  return []
}

function buildReleaseDecisionActionItems(): ReleaseStageActionItem[] {
  return [
    {
      key: 'release-reject',
      label: '拒收',
      type: 'danger',
      permission: ['mes:pro-edhr-batch-execution:quality-reject'],
      disabled: !canQualityReject.value,
      onClick: openQualityRejectDialog
    },
    {
      key: 'release-signature',
      label: '放行',
      type: 'primary',
      permission: ['mes:pro-edhr-release:submit'],
      disabled: !canSubmitRelease.value,
      onClick: openReleaseSignatureConfirmDialog
    }
  ]
}

const releaseStageActionItems = computed<ReleaseStageActionItem[]>(() => {
  const actions = buildReleaseStageActionItems(viewedReleaseStageViewModel.value.key)
  if (!isViewedReleaseStageReadonly.value) return actions
  return actions.map((action) =>
    !action.readonlyAllowed
      ? {
          ...action,
          disabled: true
        }
      : action
  )
})
type ProcessEvidenceItem = {
  key: string
  label: string
  description: string
  path?: string
  query?: Record<string, string>
  disabled?: boolean
}

type ProcessEvidenceGroup = {
  key: string
  label: string
  description: string
  itemKeys: string[]
  items: ProcessEvidenceItem[]
}

type SignoffSummaryRecord = {
  key: string
  actorName: string
  actionType: string
  signedAtText: string
  signedAtSort: number
}


type TraceRecordFieldResponsibilityEntry = {
  key: string
  executionId: number
  executionCode: string
  routeProcessSort?: number
  processName: string
  batchRecordReportName: string
  statusLabel: string
  submittedAtText: string
}

type SpecialNodeAttachmentView = Partial<EdhrBatchSpecialNodeAttachment> & {
  attachmentHash?: string
  operatedAt?: string
}

type FillCarrier = 'FORM' | 'RECORDBOOK' | 'UNCONFIGURED'
const RECORDBOOK_UNRESTRICTED_FILL_MODE = 'RECORDBOOK_UNRESTRICTED'
const selectedFillCarrier = ref<Exclude<FillCarrier, 'UNCONFIGURED'>>()

const FILL_SIGNOFF_ACTION_TYPES = ['FIELD_CHANGE', 'SUBMIT']
const SUBMIT_SIGNOFF_ACTION_TYPES = ['SUBMIT']

const appendDefinedQuery = (query: Record<string, string>, key: string, value: unknown) => {
  if (value == null || value === '') return
  query[key] = String(value)
}

type RecordbookEnabledContext = {
  recordCategory?: string | null
  recordbookEnabled?: boolean | null
}

const resolveRecordCategoryFromContext = () =>
  selectedTaskForEvidence.value?.recordCategory ||
  selectedTaskForExecution.value?.recordCategory ||
  selectedExecution.value?.recordCategory

const isRecordbookEnabledForContext = (context?: RecordbookEnabledContext) => {
  if (!context) return false
  if (context.recordCategory === 'INTERNAL_RECORD') return context.recordbookEnabled !== false
  if (context.recordCategory !== 'BATCH_RECORD') return false
  return context.recordbookEnabled !== false
}

const resolveRecordbookEnabledFromContext = () => {
  const contexts: Array<RecordbookEnabledContext | undefined> = [
    selectedTaskForEvidence.value,
    selectedTaskForExecution.value,
    selectedExecution.value
  ]
  return contexts.some(isRecordbookEnabledForContext)
}

const isRecordbookEnabledForCurrentTask = computed(() => resolveRecordbookEnabledFromContext())
const isGlobalRecordbookEnabled = computed(
  () => recordbookGlobalEnabled.value && resolveRecordbookEnabledFromContext()
)

const resolveFillCarrier = (recordCategory?: string, recordbookEnabled = false): FillCarrier => {
  if (recordCategory === 'BATCH_RECORD') return recordbookEnabled ? 'RECORDBOOK' : 'FORM'
  if (recordCategory === 'INTERNAL_RECORD') return 'RECORDBOOK'
  return 'UNCONFIGURED'
}

const currentProcessFillCarrier = computed<FillCarrier>(() => {
  if (!isGlobalRecordbookEnabled.value) return 'FORM'
  if (selectedFillCarrier.value === 'RECORDBOOK' && !isRecordbookEnabledForCurrentTask.value) {
    return 'FORM'
  }
  if (selectedFillCarrier.value) return selectedFillCarrier.value
  return resolveFillCarrier(resolveRecordCategoryFromContext(), isRecordbookEnabledForCurrentTask.value)
})

const selectFillCarrier = (fillCarrier: Exclude<FillCarrier, 'UNCONFIGURED'>) => {
  const row = selectedTaskForEvidence.value
  if (!row || isSpecialNode(row)) return
  if (fillCarrier === 'RECORDBOOK' && !isGlobalRecordbookEnabled.value) {
    message.error('记录本全局开关已关闭，只能使用批记录模式。')
    return
  }
  if (fillCarrier === 'RECORDBOOK' && !isRecordbookEnabledForCurrentTask.value) {
    message.error('当前任务已禁用记录本，只能使用批记录模式。')
    return
  }
  selectedFillCarrier.value = fillCarrier
}

const batchProcessRouteId = computed(() => {
  const routeId = Number(detail.value?.routeId)
  return Number.isFinite(routeId) && routeId > 0 ? routeId : undefined
})

const batchProcessRouteLabel = computed(
  () =>
    String(detail.value?.routeName || '').trim() ||
    String(detail.value?.routeCode || '').trim() ||
    '未关联工艺流程'
)

const batchProcessRouteTitle = computed(() =>
  batchProcessRouteId.value
    ? `打开工艺流程：${batchProcessRouteLabel.value}`
    : '当前批次未关联工艺流程'
)

const currentFormVersionNo = computed(() => {
  const versionNo = selectedTaskForEvidence.value?.batchRecordVersionNo
  return versionNo == null ? '' : String(versionNo).trim()
})

const openBatchProcessRoute = async () => {
  const routeId = batchProcessRouteId.value
  if (!routeId) {
    message.error('当前批次未关联有效工艺流程。')
    return
  }
  await router.push({
    name: 'MesProRouteEdit',
    params: { id: String(routeId) },
    query: { tab: 'flow' }
  })
}

const resolveRecordCategoryByFillCarrier = (fillCarrier: Exclude<FillCarrier, 'UNCONFIGURED'>) => {
  if (fillCarrier === 'FORM') return 'BATCH_RECORD'
  return 'INTERNAL_RECORD'
}

const resolveFillCarrierLabel = (fillCarrier?: FillCarrier) => {
  if (fillCarrier === 'FORM') return '表单'
  if (fillCarrier === 'RECORDBOOK') return '记录本'
  return '未配置'
}

const resolveCurrentBatchRecordNo = () => {
  const selectedTask = selectedTaskForEvidence.value
  if (selectedTask && isSpecialNode(selectedTask)) {
    const selectedTaskName = resolveTaskDisplayName(selectedTask)
    if (selectedTaskName && selectedTaskName !== '--') return selectedTaskName
  }

  const candidates = [
    selectedExecution.value?.batchRecordReportName,
    selectedTaskForEvidence.value?.batchRecordReportName,
    selectedExecution.value?.batchRecordReportCode,
    selectedTaskForEvidence.value?.batchRecordReportCode,
    selectedExecution.value?.batchRecordReportId,
    selectedTaskForEvidence.value?.batchRecordReportId
  ]
  return candidates.map((item) => (item == null ? '' : String(item).trim())).find(Boolean) || '--'
}

const buildFillCarrierExecutionQuery = (fillCarrier: Exclude<FillCarrier, 'UNCONFIGURED'>) => {
  const query: Record<string, string> = {
    fillCarrier,
    recordCategory: resolveRecordCategoryByFillCarrier(fillCarrier)
  }
  if (fillCarrier === 'RECORDBOOK') {
    query.fillMode = RECORDBOOK_UNRESTRICTED_FILL_MODE
  }
  return query
}

const resolveOpenProcessEvidenceLabel = () => {
  if (currentProcessFillCarrier.value === 'RECORDBOOK') return '填写记录本'
  if (currentProcessFillCarrier.value === 'FORM') return '打开表单'
  return '配置填写方式'
}

const resolveOpenProcessEvidenceDescription = () => {
  const fillCarrierLabel = resolveFillCarrierLabel(currentProcessFillCarrier.value)
  if (currentProcessFillCarrier.value === 'RECORDBOOK') return `进入当前工序${fillCarrierLabel}不受控填写。`
  if (currentProcessFillCarrier.value === 'FORM') return `进入当前工序${fillCarrierLabel}和填写页。`
  return '当前工序未配置填写方式，请先配置表单或记录本。'
}

const buildSelectedProcessEvidenceQuery = (extraQuery: Record<string, unknown> = {}) => {
  const execution = selectedExecution.value
  const task = selectedTaskForEvidence.value
  const query: Record<string, string> = {}
  appendDefinedQuery(query, 'batchExecutionId', detail.value?.id || batchExecutionId.value)
  appendDefinedQuery(query, 'batchExecutionCode', detail.value?.batchExecutionCode)
  appendDefinedQuery(query, 'workOrderCode', detail.value?.workOrderCode)
  appendDefinedQuery(query, 'batchCode', detail.value?.batchCode)
  appendDefinedQuery(query, 'productCode', detail.value?.productCode)
  appendDefinedQuery(query, 'routeCode', detail.value?.routeCode)
  appendDefinedQuery(query, 'routeProcessId', task?.routeProcessId || execution?.routeProcessId)
  appendDefinedQuery(query, 'routeProcessSort', task?.routeProcessSort || execution?.routeProcessSort)
  appendDefinedQuery(query, 'processCode', task?.processCode || execution?.processCode)
  appendDefinedQuery(query, 'processName', task?.processName || execution?.processName)
  appendDefinedQuery(query, 'executionId', execution?.executionId || task?.executionId)
  appendDefinedQuery(query, 'executionCode', execution?.executionCode)
  appendDefinedQuery(query, 'workTaskId', route.query.workTaskId)
  appendDefinedQuery(query, 'batchTaskId', task?.id)
  appendDefinedQuery(query, 'reportId', task?.batchRecordReportId || execution?.batchRecordReportId)
  appendDefinedQuery(query, 'batchRecordReportId', task?.batchRecordReportId || execution?.batchRecordReportId)
  appendDefinedQuery(query, 'recordCategory', resolveRecordCategoryFromContext())
  appendDefinedQuery(query, 'fillCarrier', currentProcessFillCarrier.value)
  for (const [key, value] of Object.entries(extraQuery)) {
    appendDefinedQuery(query, key, value)
  }
  return query
}

const selectedProcessEvidenceItems = computed<ProcessEvidenceItem[]>(() => {
  const context = selectedProcessContext.value
  if (!context) return []
  const execution = selectedExecution.value
  const task = selectedTaskForEvidence.value
  const executionId = execution?.executionId || task?.executionId
  const baseQuery = buildSelectedProcessEvidenceQuery()
  const executionRequired = !executionId
  return [
    {
      key: 'open-process',
      label: resolveOpenProcessEvidenceLabel(),
      description: resolveOpenProcessEvidenceDescription(),
      disabled: currentProcessFillCarrier.value === 'UNCONFIGURED' || (currentProcessFillCarrier.value === 'FORM' && !selectedOpenableTask.value)
    },
    {
      key: 'work-task',
      label: '工作任务',
      description: '当前工序待办、返工和归档任务。',
      path: '/mes/pro/feedback/edhr-work-task',
      query: buildSelectedProcessEvidenceQuery()
    },
    {
      key: 'signature',
      label: '签名记录',
      description: '当前工序密码签名、签名人和时间证据。',
      path: '/signature-governance/batch-signatures',
      query: buildSelectedProcessEvidenceQuery({
        executionId
      }),
      disabled: executionRequired
    },
    {
      key: 'approval',
      label: '审批记录',
      description: '当前工序提交、通过、驳回和返工审批。',
      path: '/mes/pro/feedback/edhr-approval/detail',
      query: buildSelectedProcessEvidenceQuery({
        id: executionId,
        focus: 'approval',
        returnPath: '/mes/pro/feedback/edhr-batch-execution/detail'
      }),
      disabled: executionRequired
    },
    {
      key: 'single-archive',
      label: '单表归档',
      description: '当前工序单张表单归档和打印入口。',
      path: '/mes/pro/feedback/edhr-execution/form',
      query: buildSelectedProcessEvidenceQuery({
        id: executionId,
        executionId,
        fromBatchDetail: '1',
        viewMode: 'tracking',
        returnPath: '/mes/pro/feedback/edhr-batch-execution/detail'
      }),
      disabled: executionRequired
    },
    {
      key: 'field-audit',
      label: '字段审计',
      description: '展示当前工序字段修改、修改原因和哈希链证据。',
      path: '/mes/pro/feedback/edhr-field-audit',
      query: buildSelectedProcessEvidenceQuery({ executionId }),
      disabled: executionRequired
    },
    {
      key: 'operation-audit',
      label: '操作审计',
      description: '当前工序对象级操作、权限决策和结果。',
      path: '/mes/pro/feedback/edhr-operation-audit',
      query: buildSelectedProcessEvidenceQuery({
        objectType: 'BATCH_RECORD_EXECUTION',
        objectId: executionId,
        hideRecordbookMode: !isRecordbookEnabledForCurrentTask.value ? 'true' : undefined
      }),
      disabled: executionRequired
    },
    {
      key: 'record-change',
      label: '变更记录',
      description: '当前工序补录、作废、重开等变更。',
      path: '/mes/pro/feedback/edhr-form-trace',
      query: buildSelectedProcessEvidenceQuery({
        tab: 'change',
        targetScope: 'EXECUTION',
        executionId
      })
    },
    {
      key: 'unified-change',
      label: '统一变更',
      description: '当前工序受控对象的统一变更台账。',
      path: '/mes/pro/feedback/edhr-unified-change',
      query: buildSelectedProcessEvidenceQuery({
        controlledObjectType: 'BATCH_RECORD_EXECUTION',
        controlledObjectId: executionId,
        controlledObjectCode: execution?.executionCode || baseQuery.processCode
      }),
      disabled: executionRequired
    },
    {
      key: 'domain-trace',
      label: '主数据追溯',
      description: '当前工序物料、设备、人员和模板版本追溯。',
      path: '/mes/pro/feedback/edhr-domain-trace/detail',
      query: buildSelectedProcessEvidenceQuery({ executionId }),
      disabled: executionRequired
    },
    {
      key: 'history',
      label: '历史同工序',
      description: '同批次/同路线历史批记录参考。',
      path: '/mes/pro/feedback/edhr-batch-history',
      query: buildSelectedProcessEvidenceQuery()
    },
    {
      key: 'independent-form',
      label: '独立表单',
      description: '与当前工序绑定的独立表单引用。',
      path: '/mes/pro/feedback/edhr-form',
      query: buildSelectedProcessEvidenceQuery()
    },
    ...(
      isRecordbookEnabledForCurrentTask.value
        ? [
            {
              key: 'recordbook',
              label: '记录本填写',
              description: '在当前批次执行表单中按记录本方式不受控填写。',
              path: '/mes/pro/feedback/edhr-execution/form',
              query: buildSelectedProcessEvidenceQuery({
                id: executionId,
                executionId,
                ...buildFillCarrierExecutionQuery('RECORDBOOK'),
                returnPath: '/mes/pro/feedback/edhr-batch-execution/detail'
              }),
              disabled: executionRequired
            }
          ]
        : []
    )
  ]
})

const resolveSignoffDisplayTime = (record: EdhrBatchExecutionReviewSignatureRecord) =>
  record.signatureDisplayAt || record.selectedSignedAt || record.signedAt

const normalizeSignoffRecord = (
  record: EdhrBatchExecutionReviewSignatureRecord
): SignoffSummaryRecord => {
  const displayTime = resolveSignoffDisplayTime(record)
  const parsedTime = displayTime ? dayjs(displayTime) : undefined
  return {
    key: String(record.id || `${record.actionType || 'SIGN'}-${record.actorId || record.actorName || 'UNKNOWN'}-${displayTime || 'NO_TIME'}`),
    actorName: record.actorName || '未知人员',
    actionType: record.actionType || '',
    signedAtText: formatReviewTime(displayTime),
    signedAtSort: parsedTime?.isValid() ? parsedTime.valueOf() : Number.MAX_SAFE_INTEGER
  }
}

const compareSignoffRecords = (left: SignoffSummaryRecord, right: SignoffSummaryRecord) =>
  left.signedAtSort - right.signedAtSort || left.key.localeCompare(right.key)

const fillSignoffRecords = computed<SignoffSummaryRecord[]>(() => {
  const records = selectedExecution.value?.signatureRecords || []
  return records
    .filter((record) => FILL_SIGNOFF_ACTION_TYPES.includes(record.actionType || ''))
    .map(normalizeSignoffRecord)
    .sort(compareSignoffRecords)
})

const submitSignoffRecords = computed<SignoffSummaryRecord[]>(() => {
  const records = selectedExecution.value?.signatureRecords || []
  return records
    .filter((record) => SUBMIT_SIGNOFF_ACTION_TYPES.includes(record.actionType || ''))
    .map(normalizeSignoffRecord)
    .sort(compareSignoffRecords)
})

const selectedProcessEvidenceGroups = computed<ProcessEvidenceGroup[]>(() => {
  const evidenceItems = selectedProcessEvidenceItems.value
  const resolveItems = (itemKeys: string[]) =>
    itemKeys
      .map((itemKey) => evidenceItems.find((item) => item.key === itemKey))
      .filter((item): item is ProcessEvidenceItem => Boolean(item))

  return [
    {
      key: 'execution',
      label: '工序执行',
      description: '打开当前工序并查看待办任务。',
      itemKeys: ['open-process', 'work-task'],
      items: resolveItems(['open-process', 'work-task'])
    },
    {
      key: 'signoff',
      label: '审签归档',
      description: '当前工序签名、审批和单表归档。',
      itemKeys: ['signature', 'approval', 'single-archive'],
      items: resolveItems(['signature', 'approval', 'single-archive'])
    },
    {
      key: 'audit-trace',
      label: '审计追溯',
      description: '字段、操作、变更与主数据追溯证据。',
      itemKeys: ['field-audit', 'operation-audit', 'record-change', 'unified-change', 'domain-trace'],
      items: resolveItems(['field-audit', 'operation-audit', 'record-change', 'unified-change', 'domain-trace'])
    },
    {
      key: 'references',
      label: '关联引用',
      description: '历史同工序、独立表单和记录本填写。',
      itemKeys: ['history', 'independent-form', 'recordbook'],
      items: resolveItems(['history', 'independent-form', 'recordbook'])
    }
  ]
})

const findTaskForReviewExecution = (execution: EdhrBatchExecutionReviewExecutionRespVO) =>
  sortedTasks.value.find(
    (task) =>
      task.executionId === execution.executionId ||
      (task.routeProcessSort === execution.routeProcessSort &&
        task.batchRecordReportId === execution.batchRecordReportId) ||
      (task.processCode === execution.processCode && task.batchRecordReportId === execution.batchRecordReportId)
  )

const fieldResponsibilityEntries = computed<TraceRecordFieldResponsibilityEntry[]>(() => {
  const entries = new Map<number, TraceRecordFieldResponsibilityEntry>()
  const appendEntry = (
    source: Partial<EdhrBatchExecutionReviewExecutionRespVO & EdhrBatchExecutionTaskRespVO>,
    task?: EdhrBatchExecutionTaskRespVO
  ) => {
    const executionId = Number(source.executionId || task?.executionId)
    if (!Number.isFinite(executionId) || executionId <= 0 || entries.has(executionId)) return
    entries.set(executionId, {
      key: String(executionId),
      executionId,
      executionCode: String(source.executionCode || task?.executionCode || `#${executionId}`),
      routeProcessSort: source.routeProcessSort || task?.routeProcessSort,
      processName: String(source.processName || task?.processName || '').trim(),
      batchRecordReportName: String(source.batchRecordReportName || task?.batchRecordReportName || '').trim(),
      statusLabel: task ? resolveTaskStatusLabel(task) : source.status == null ? '--' : `状态 ${source.status}`,
      submittedAtText: formatReviewTime(source.submittedAt)
    })
  }

  for (const execution of executionReviews.value) {
    appendEntry(execution, findTaskForReviewExecution(execution))
  }
  for (const task of sortedTasks.value) {
    appendEntry(task, task)
  }

  return [...entries.values()].sort(
    (left, right) =>
      (left.routeProcessSort || 0) - (right.routeProcessSort || 0) ||
      left.executionId - right.executionId
  )
})

const releaseTransactionForm = reactive({
  idempotencyKey: '',
  password: '',
  approvalOpinion: '',
  rejectReason: '',
  withdrawReason: ''
})
const releaseSignatureForm = reactive({
  idempotencyKey: '',
  password: '',
  approvalOpinion: ''
})
const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const isStaleBatchDetailRequest = (requestSerial?: number) =>
  requestSerial !== undefined && requestSerial !== batchDetailRequestSerial

const assertBatchExecutionId = () => {
  if (!batchExecutionId.value) {
    throw new Error('URL 缺少有效批次执行ID。')
  }
  return batchExecutionId.value
}

const assertArchiveWorkTaskId = () => {
  if (!archiveWorkTaskId.value) {
    throw new Error('地址缺少有效归档工作任务ID，请从电子批记录工作任务看板进入最终归档。')
  }
  return archiveWorkTaskId.value
}

const resolveBatchStatusLabel = (status?: number) => {
  const labels: Record<number, string> = {
    [EDHR_BATCH_STATUS_CREATED]: '已创建',
    [EDHR_BATCH_STATUS_IN_PROGRESS]: '执行中',
    [EDHR_BATCH_STATUS_READY_TO_CLOSE]: '待关闭',
    [EDHR_BATCH_STATUS_REWORK_REQUIRED]: '需返工/需修订',
    [EDHR_BATCH_STATUS_CLOSED]: '已关闭',
    [EDHR_BATCH_STATUS_ARCHIVED]: '已归档',
    [EDHR_BATCH_STATUS_REJECTED]: '质量已拒收'
  }
  return status == null ? '--' : labels[status] || String(status)
}

const resolveBatchStatusType = (status?: number) => {
  if (status === EDHR_BATCH_STATUS_ARCHIVED || status === EDHR_BATCH_STATUS_CLOSED) return 'success'
  if (status === EDHR_BATCH_STATUS_REJECTED) return 'danger'
  if (status === EDHR_BATCH_STATUS_READY_TO_CLOSE || status === EDHR_BATCH_STATUS_REWORK_REQUIRED) return 'warning'
  if (status === EDHR_BATCH_STATUS_IN_PROGRESS) return 'primary'
  return 'info'
}

const specialNodeLabels: Record<string, string> = {
  [EDHR_BATCH_NODE_INCOMING_INSPECTION_REPORT]: '来料检报告',
  [EDHR_BATCH_NODE_STERILIZATION_REPORT]: '灭菌报告',
  [EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_REPORT]: '成品检报告',
  [EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_RECORD]: '成品检记录'
}

const specialNodeDisplaySorts: Record<string, string> = {
  [EDHR_BATCH_NODE_STERILIZATION_REPORT]: '90',
  [EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_REPORT]: '91',
  [EDHR_BATCH_NODE_FINISHED_PRODUCT_INSPECTION_RECORD]: '92'
}

const isSpecialNode = (row?: EdhrBatchExecutionTaskRespVO) =>
  Boolean(row?.nodeType && row.nodeType !== EDHR_BATCH_NODE_ROUTE_FORM)

const isSterilizationNode = (row?: EdhrBatchExecutionTaskRespVO) =>
  row?.nodeType === EDHR_BATCH_NODE_STERILIZATION_REPORT

const resolveTaskDisplayName = (row: EdhrBatchExecutionTaskRespVO) =>
  (isSpecialNode(row) && row.nodeType ? specialNodeLabels[row.nodeType] : '') ||
  row.formTemplateName ||
  row.batchRecordReportName ||
  row.batchRecordReportCode ||
  row.batchRecordReportId ||
  (row.nodeType ? specialNodeLabels[row.nodeType] : '') ||
  row.processName ||
  '--'

const resolveTaskCardDisplayName = (row: EdhrBatchExecutionTaskRespVO) => {
  const name = resolveTaskDisplayName(row)
  const isDraft = row.status === EDHR_BATCH_TASK_STATUS_DRAFT
  if (!isDraft || name === '--') return name
  return `${name}*`
}

const resolvePendingTaskTitle = (row: EdhrBatchExecutionTaskRespVO) =>
  isSpecialNode(row)
    ? specialNodeLabels[row.nodeType || ''] || row.processName || row.processCode || '--'
    : row.processName || row.processCode || resolveTaskDisplayName(row)

const resolvePendingTaskSortText = (row: EdhrBatchExecutionTaskRespVO) => {
  const specialNodeDisplaySort = specialNodeDisplaySorts[row.nodeType || '']
  if (specialNodeDisplaySort) return specialNodeDisplaySort
  if (isSpecialNode(row) && row.routeProcessSort === 0) return '--'
  return row.routeProcessSort == null ? '--' : String(row.routeProcessSort)
}

const FORM_SLOT_LABELS: Record<string, string> = {
  MAIN: '主生产表',
  PROCESS_INSPECTION: '过程检验单',
  LOSS_REPORT: '损耗单',
  PARAMETER_RECORD: '参数记录表',
  OTHER_INTERNAL: '其他内部记录'
}

const resolveFormSlotTypeLabel = (row: EdhrBatchExecutionTaskRespVO) =>
  row.formTemplateId
    ? '动态表单'
    : row.formSlotType
      ? FORM_SLOT_LABELS[row.formSlotType] || row.formSlotType
      : '缺失配置'

const resolveTaskSlotBlocker = (row: EdhrBatchExecutionTaskRespVO) => {
  if (isSpecialNode(row)) return ''
  if (row.slotBlockerMessage) return row.slotBlockerMessage
  if (row.formTemplateId) {
    if (!row.formBindingKey) return '缺少表单绑定标识'
    if (!row.formTemplateVersionId || !row.formTemplateVersionNo) return '缺少表单模板版本快照'
    if (!row.formCenterInstanceId) return '缺少表单中心实例'
    return ''
  }
  if (!row.formSlotType) return '缺少槽位类型'
  if (!row.recordCategory) return '缺少记录分类'
  if (!row.validationProfile) return '缺少校验策略'
  if (!row.slotConfigSnapshotHash) return '缺少槽位快照'
  return ''
}

const HIDDEN_FILL_ACCESS_REASON_KEYWORDS = ['待处理任务', '权限范围', 'permissionScopeId']

const normalizeTaskAccessReason = (reason?: string | null) => {
  const text = String(reason || '').trim()
  if (!text) return ''
  if (HIDDEN_FILL_ACCESS_REASON_KEYWORDS.some((keyword) => text.includes(keyword))) return ''
  return text
}

const resolveTaskGateText = (row: EdhrBatchExecutionTaskRespVO) =>
  resolveTaskSlotBlocker(row) ||
  normalizeTaskAccessReason(row.disabledReason) ||
  normalizeTaskAccessReason(row.gateMessage)

const resolveFillerNames = (
  fillers?: EdhrBatchExecutionRespVO['currentProcessProductionFillers']
) =>
  (fillers || [])
    .map((user) => user.displayName || (user.userId == null ? '' : String(user.userId)))
    .filter(Boolean)
    .join('、')

const selectedTaskBelongsToCurrentProcess = (row: EdhrBatchExecutionTaskRespVO) => {
  if (isProductInfoProcessTask(row)) return false
  const currentRouteProcessId = detail.value?.currentProcessRouteProcessId
  if (currentRouteProcessId != null && row.routeProcessId === currentRouteProcessId) return true
  const currentProcessCode = String(detail.value?.currentProcessCode || '').trim()
  const currentProcessName = String(detail.value?.currentProcessName || '').trim()
  if (currentProcessCode && row.processCode === currentProcessCode) return true
  if (currentProcessName && row.processName === currentProcessName) return true
  return false
}

const resolveSelectedTaskFillerGroupsText = (row: EdhrBatchExecutionTaskRespVO) => {
  if (!selectedTaskBelongsToCurrentProcess(row)) return '未配置'
  const groups = [
    ['生产', detail.value?.currentProcessProductionFillers],
    ['设备', detail.value?.currentProcessEquipmentFillers],
    ['质量', detail.value?.currentProcessQualityFillers]
  ]
    .map(([label, fillers]) => {
      const names = resolveFillerNames(fillers as EdhrBatchExecutionRespVO['currentProcessProductionFillers'])
      return names ? `${label}：${names}` : ''
    })
    .filter(Boolean)
  return groups.length ? groups.join('；') : '未配置'
}

const resolvePendingTaskFillableUsersText = (row: EdhrBatchExecutionTaskRespVO) => {
  const names = (row.fillableUsers || [])
    .map((user) => user.displayName || (user.userId == null ? '' : String(user.userId)))
    .filter(Boolean)
  return names.length ? names.join('、') : resolveSelectedTaskFillerGroupsText(row)
}

const resolveTaskCardFillersText = (row: EdhrBatchExecutionTaskRespVO) => {
  const names = compactPositionText(
    (row.fillableUsers || []).map((user) =>
      user.displayName || (user.userId == null ? '' : String(user.userId))
    )
  )
  return names.length ? names.join('、') : '未配置'
}


const selectedSpecialNodeForEvidence = computed(() => {
  const task = selectedTaskForEvidence.value
  return task && isSpecialNode(task) ? task : undefined
})

const currentSpecialNodePendingAttachments = computed(() =>
  currentSpecialNode.value?.id ? specialNodePendingAttachments[currentSpecialNode.value.id] || [] : []
)

const selectedSpecialNodePendingAttachments = computed(() =>
  selectedSpecialNodeForEvidence.value?.id
    ? specialNodePendingAttachments[selectedSpecialNodeForEvidence.value.id] || []
    : []
)

const pendingSpecialNodeAttachmentCount = computed(() =>
  Object.values(specialNodePendingAttachments).reduce(
    (total, attachments) => total + attachments.length,
    0
  )
)

const pendingSpecialNodeAttachmentTaskCount = computed(() =>
  Object.values(specialNodePendingAttachments).filter((attachments) => attachments.length > 0).length
)

const parseSpecialNodePayload = (row: EdhrBatchExecutionTaskRespVO) => {
  if (!row.specialPayloadJson?.trim()) return {}
  try {
    return JSON.parse(row.specialPayloadJson)
  } catch (error) {
    throw new Error(`特殊节点「${resolveTaskDisplayName(row)}」附件元数据解析失败。`)
  }
}

const selectedSpecialNodePersistedAttachments = computed<SpecialNodeAttachmentView[]>(() => {
  const task = selectedSpecialNodeForEvidence.value
  if (!task) return []
  const payload = parseSpecialNodePayload(task) as { attachments?: SpecialNodeAttachmentView[] }
  return Array.isArray(payload.attachments) ? payload.attachments : []
})

const normalizeSpecialNodeAttachmentFileName = (fileName?: string | null) =>
  (fileName || '').trim().toLocaleLowerCase()

const buildSpecialNodeAttachmentKey = (attachment: SpecialNodeAttachmentView) =>
  String(
    attachment.uploadToken ||
      attachment.attachmentHash ||
      attachment.sha256 ||
      attachment.fileId ||
      attachment.storagePath ||
      attachment.fileName ||
      'UNKNOWN_ATTACHMENT'
  )

const formatSpecialNodeFileSize = (value?: number | null) => {
  if (value == null) return '--'
  if (value <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = value
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex += 1
  }
  return `${Number(size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1))} ${units[unitIndex]}`
}

const previewSpecialNodeAttachment = (attachment: SpecialNodeAttachmentView) => {
  if (attachment.fileId == null || String(attachment.fileId).trim() === '') {
    message.error('附件缺少文件编号，无法在线预览。')
    return
  }
  selectedSpecialNodePreviewSource.value = buildEdhrSpecialNodeAttachmentPreviewSource(attachment.fileId)
  selectedSpecialNodePreviewTitle.value =
    attachment.fileName || attachment.storagePath || '特殊节点附件预览'
  specialNodePreviewDialogVisible.value = true
}

const upsertSpecialNodePendingAttachment = (
  taskId: number,
  attachment: EdhrBatchSpecialNodeAttachment
) => {
  const normalizedFileName = normalizeSpecialNodeAttachmentFileName(attachment.fileName)
  if (!normalizedFileName) {
    throw new Error('特殊节点附件缺少文件名，无法加入待提交列表。')
  }
  const existingAttachments = specialNodePendingAttachments[taskId] || []
  const retainedAttachments = existingAttachments.filter(
    (candidate) => normalizeSpecialNodeAttachmentFileName(candidate.fileName) !== normalizedFileName
  )
  specialNodePendingAttachments[taskId] = [...retainedAttachments, attachment]
}

const removeSpecialNodePendingAttachment = (
  taskId: number,
  attachment: EdhrBatchSpecialNodeAttachment
) => {
  const normalizedFileName = normalizeSpecialNodeAttachmentFileName(attachment.fileName)
  specialNodePendingAttachments[taskId] = (specialNodePendingAttachments[taskId] || []).filter(
    (candidate) =>
      candidate.uploadToken !== attachment.uploadToken &&
      normalizeSpecialNodeAttachmentFileName(candidate.fileName) !== normalizedFileName
  )
}

const removeSelectedSpecialNodePendingAttachment = async (attachment: EdhrBatchSpecialNodeAttachment) => {
  const taskId = selectedSpecialNodeForEvidence.value?.id
  if (!taskId) {
    message.error('当前特殊节点不存在，无法删除待提交附件。')
    return
  }
  let reason = ''
  try {
    const promptResult = await ElMessageBox.prompt(
      `请输入删除待提交附件“${attachment.fileName || attachment.storagePath || attachment.fileId}”的原因。`,
      '删除待提交附件',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：文件选择错误，重新上传正确附件',
        inputValidator: (value) => Boolean(String(value || '').trim()),
        inputErrorMessage: '删除原因不能为空'
      }
    )
    reason = String(promptResult.value || '').trim()
  } catch (error) {
    message.warning('已取消删除待提交附件')
    return
  }
  try {
    await deleteEdhrBatchSpecialNodePendingAttachment({ taskId, attachment, reason })
    removeSpecialNodePendingAttachment(taskId, attachment)
    message.success('待提交附件已删除')
  } catch (error) {
    message.error(resolveErrorMessage(error, '待提交附件删除失败。'))
  }
}

const buildSpecialNodeSubmitAttachments = (taskId: number) => [
  ...(specialNodePendingAttachments[taskId] || [])
]

const ensurePendingSpecialNodeAttachmentsSavedBeforeRelease = async () => {
  if (pendingSpecialNodeAttachmentCount.value <= 0) return true
  const batchExecutionId = detail.value?.id
  if (!batchExecutionId) {
    const errorText = '当前批次不存在，无法保存待提交附件。'
    showReleaseActionError(errorText)
    message.error(errorText)
    return false
  }
  try {
    await ElMessageBox.confirm(
      `当前批次有 ${pendingSpecialNodeAttachmentTaskCount.value} 个特殊节点共 ${pendingSpecialNodeAttachmentCount.value} 个附件已上传但尚未保存。保存后将进入追溯和归档证据，是否现在保存并继续放行？`,
      '保存待提交附件',
      {
        confirmButtonText: '保存并继续',
        cancelButtonText: '取消放行',
        type: 'warning'
      }
    )
  } catch (error) {
    const warningText = '已取消放行操作，待提交附件尚未保存。'
    showReleaseActionError(warningText)
    message.warning(warningText)
    return false
  }
  try {
    await savePendingEdhrBatchSpecialNodeAttachments({
      batchExecutionId,
      reason: '放行前保存待提交特殊节点附件'
    })
    message.success('待提交特殊节点附件已保存')
    await loadDetail()
    return true
  } catch (error) {
    const errorText = resolveErrorMessage(error, '待提交特殊节点附件保存失败，已中止放行操作。')
    showReleaseActionError(errorText)
    message.error(errorText)
    return false
  }
}

const clearSpecialNodePendingAttachments = (taskId: number) => {
  delete specialNodePendingAttachments[taskId]
}

const syncSpecialNodePendingAttachmentsFromDetail = (nextDetail?: EdhrBatchExecutionRespVO) => {
  const nextTaskIds = new Set<number>()
  for (const task of nextDetail?.tasks || []) {
    if (!isSpecialNode(task)) continue
    nextTaskIds.add(task.id)
    const pendingAttachments = Array.isArray(task.pendingSpecialNodeAttachments)
      ? task.pendingSpecialNodeAttachments
      : []
    if (pendingAttachments.length) {
      specialNodePendingAttachments[task.id] = [...pendingAttachments]
    } else {
      delete specialNodePendingAttachments[task.id]
    }
  }
  for (const taskId of Object.keys(specialNodePendingAttachments)) {
    if (!nextTaskIds.has(Number(taskId))) {
      delete specialNodePendingAttachments[Number(taskId)]
    }
  }
}

const isOptionalTask = (row: EdhrBatchExecutionTaskRespVO) =>
  !isSpecialNode(row) && isOptionalRouteFormTask(row)

const currentSkipTaskIsOptional = computed(() =>
  currentSpecialNode.value ? isOptionalTask(currentSpecialNode.value) : false
)

const currentSkipDialogTitle = computed(() =>
  currentSkipTaskIsOptional.value ? '跳过可选表单' : '跳过特殊节点'
)

const resolveTaskStatusLabel = (row: EdhrBatchExecutionTaskRespVO) => {
  const status = row.status
  if (isOptionalTask(row) && (status == null || status === EDHR_BATCH_TASK_STATUS_WAITING)) {
    return '可选填写'
  }
  const labels: Record<number, string> = {
    [EDHR_BATCH_TASK_STATUS_WAITING]: '待打开',
    [EDHR_BATCH_TASK_STATUS_DRAFT]: '草稿',
    [EDHR_BATCH_TASK_STATUS_SUBMITTED]: '已提交',
    [EDHR_BATCH_TASK_STATUS_REJECTED]: '已驳回',
    [EDHR_BATCH_TASK_STATUS_REWORK_REQUIRED]: '需返工',
    [EDHR_BATCH_TASK_STATUS_APPROVED]: '填写完成',
    [EDHR_BATCH_TASK_STATUS_SKIPPED]: '已跳过',
    [EDHR_BATCH_TASK_STATUS_BLOCKED]: '阻塞'
  }
  return status == null ? '--' : labels[status] || String(status)
}

const resolveTaskStatusType = (row: EdhrBatchExecutionTaskRespVO) => {
  const status = row.status
  if (status === EDHR_BATCH_TASK_STATUS_APPROVED || status === EDHR_BATCH_TASK_STATUS_SKIPPED) return 'success'
  if (status === EDHR_BATCH_TASK_STATUS_REJECTED || status === EDHR_BATCH_TASK_STATUS_BLOCKED) return 'danger'
  if (status === EDHR_BATCH_TASK_STATUS_SUBMITTED || status === EDHR_BATCH_TASK_STATUS_REWORK_REQUIRED) return 'warning'
  if (status === EDHR_BATCH_TASK_STATUS_DRAFT) return 'primary'
  return 'info'
}

const isCompletedProcessTask = (row: EdhrBatchExecutionTaskRespVO) =>
  isOptionalTask(row) ||
  row.status === EDHR_BATCH_TASK_STATUS_APPROVED ||
  row.status === EDHR_BATCH_TASK_STATUS_SKIPPED

const normalizeProcessIdentityText = (value?: string | number | null) => String(value ?? '').trim()

const isProductInfoProcessGroup = (group: ProcessTaskGroup) =>
  group.tasks.some(isProductInfoProcessTask)

const isCurrentExecutableProcessGroup = (group: ProcessTaskGroup) => {
  if (isProductInfoProcessGroup(group)) return false
  return group.tasks.some(
    (task) => task.available === true && !isCompletedProcessTask(task) && !isOptionalTask(task)
  )
}

const isCurrentProcessGroup = (group: ProcessTaskGroup) => {
  if (isProductInfoProcessGroup(group)) return false
  const currentRouteProcessId = detail.value?.currentProcessRouteProcessId
  if (currentRouteProcessId != null && group.routeProcessId === currentRouteProcessId) return true
  const currentProcessCode = normalizeProcessIdentityText(detail.value?.currentProcessCode)
  if (currentProcessCode && currentProcessCode === normalizeProcessIdentityText(group.processCode)) return true
  const currentProcessName = normalizeProcessIdentityText(detail.value?.currentProcessName)
  if (currentProcessName && currentProcessName === normalizeProcessIdentityText(group.processName)) return true
  return false
}

const resolveProcessGroupStateClass = (group: ProcessTaskGroup) => {
  const requiredTasks = group.tasks.filter((task) => !isOptionalTask(task))
  if (!requiredTasks.length || requiredTasks.every(isCompletedProcessTask)) return 'is-completed'
  if (isCurrentExecutableProcessGroup(group) || isCurrentProcessGroup(group)) return 'is-in-progress'
  const hasStartedTask = requiredTasks.some(
    (task) => task.status != null && task.status !== EDHR_BATCH_TASK_STATUS_WAITING
  )
  return hasStartedTask ? 'is-in-progress' : 'is-not-started'
}

const isProcessGroupActive = (group: ProcessTaskGroup) =>
  !isReleaseProcessSelected.value &&
  group.tasks.some(
    (task) =>
      String(task.id) === selectedTaskId.value ||
      (task.executionId != null && String(task.executionId) === selectedExecutionId.value)
  )

const hasActiveWorkTask = (row: EdhrBatchExecutionTaskRespVO) => Boolean(row.activeWorkTaskId)

const resolveEdhrTaskActionProjection = (row: EdhrBatchExecutionTaskRespVO, action: string) => {
  if (!Array.isArray(row.allowedActions)) {
    return resolveControlledActionProjection(undefined, action)
  }
  const allowed = row.allowedActions.includes(action)
  return resolveControlledActionProjection(
    {
      actionCode: action,
      actionLabel: action,
      allowed,
      permissionGranted: allowed
    },
    action
  )
}

const hasAllowedTaskAction = (row: EdhrBatchExecutionTaskRespVO, action: string) =>
  resolveEdhrTaskActionProjection(row, action).allowed

const canOpenTask = (row: EdhrBatchExecutionTaskRespVO) =>
  !isSpecialNode(row) &&
  !resolveTaskSlotBlocker(row) &&
  row.available !== false &&
  row.status !== EDHR_BATCH_TASK_STATUS_BLOCKED &&
  row.status !== EDHR_BATCH_TASK_STATUS_SKIPPED &&
  hasActiveWorkTask(row) &&
  hasAllowedTaskAction(row, 'OPEN_FORM')

const canAutoOpenRouteFormTask = (row: EdhrBatchExecutionTaskRespVO) =>
  !isSpecialNode(row) &&
  !resolveTaskSlotBlocker(row) &&
  row.available !== false &&
  row.status !== EDHR_BATCH_TASK_STATUS_BLOCKED &&
  row.status !== EDHR_BATCH_TASK_STATUS_SKIPPED &&
  hasActiveWorkTask(row)

const canTakeOverFillTask = (row: EdhrBatchExecutionTaskRespVO) =>
  canUseFlowTransferIntervention.value &&
  !isSpecialNode(row) &&
  !resolveTaskSlotBlocker(row) &&
  row.available !== false &&
  row.status !== EDHR_BATCH_TASK_STATUS_BLOCKED &&
  row.status !== EDHR_BATCH_TASK_STATUS_APPROVED &&
  row.status !== EDHR_BATCH_TASK_STATUS_SKIPPED &&
  row.activeWorkTaskId &&
  !hasAllowedTaskAction(row, 'OPEN_FORM')

const resolveTakeoverFillCarrier = (row: EdhrBatchExecutionTaskRespVO): Exclude<FillCarrier, 'UNCONFIGURED'> => {
  if (currentProcessFillCarrier.value === 'FORM' || currentProcessFillCarrier.value === 'RECORDBOOK') {
    return currentProcessFillCarrier.value
  }
  return resolveFillCarrier(row.recordCategory) === 'RECORDBOOK' ? 'RECORDBOOK' : 'FORM'
}

const canSkipOptionalTask = (row: EdhrBatchExecutionTaskRespVO) =>
  isOptionalTask(row) &&
  !resolveTaskSlotBlocker(row) &&
  row.available !== false &&
  row.status !== EDHR_BATCH_TASK_STATUS_APPROVED &&
  row.status !== EDHR_BATCH_TASK_STATUS_SKIPPED &&
  row.status !== EDHR_BATCH_TASK_STATUS_BLOCKED &&
  hasActiveWorkTask(row) &&
  hasAllowedTaskAction(row, 'SKIP') &&
  detail.value?.status !== EDHR_BATCH_STATUS_CLOSED &&
  detail.value?.status !== EDHR_BATCH_STATUS_ARCHIVED &&
  detail.value?.status !== EDHR_BATCH_STATUS_REJECTED

const canViewRouteFormTask = (row: EdhrBatchExecutionTaskRespVO) =>
  !isSpecialNode(row) &&
  !resolveTaskSlotBlocker(row) &&
  Boolean(row.formTemplateId || row.batchRecordReportId || row.formCenterInstanceId || row.executionId)

const canHandlePendingTask = (row: EdhrBatchExecutionTaskRespVO) =>
  isSpecialNode(row)
    ? canOperateSpecialNode(row)
    : canOpenTask(row) || canSkipOptionalTask(row) || canViewRouteFormTask(row)

const canUploadSpecialNodeAttachment = (row: EdhrBatchExecutionTaskRespVO) =>
  isSpecialNode(row) &&
  !isOptionalTask(row) &&
  row.status !== EDHR_BATCH_TASK_STATUS_BLOCKED &&
  releaseStatus.value !== 'RELEASED' &&
  detail.value?.status !== EDHR_BATCH_STATUS_ARCHIVED &&
  detail.value?.status !== EDHR_BATCH_STATUS_REJECTED &&
  detail.value?.status !== EDHR_BATCH_STATUS_VOIDED

const canOperateSpecialNode = (row: EdhrBatchExecutionTaskRespVO) =>
  isSpecialNode(row) &&
  !isOptionalTask(row) &&
  hasAllowedTaskAction(row, 'CLOSE') &&
  row.status !== EDHR_BATCH_TASK_STATUS_APPROVED &&
  row.status !== EDHR_BATCH_TASK_STATUS_SKIPPED &&
  row.status !== EDHR_BATCH_TASK_STATUS_BLOCKED &&
  detail.value?.status !== EDHR_BATCH_STATUS_CLOSED &&
  detail.value?.status !== EDHR_BATCH_STATUS_ARCHIVED &&
  detail.value?.status !== EDHR_BATCH_STATUS_REJECTED

const resolveOpenTaskActionLabel = (row: EdhrBatchExecutionTaskRespVO) =>
  row.status === EDHR_BATCH_TASK_STATUS_REWORK_REQUIRED ? '打开返工' : '打开填写'

const resolvePendingTaskActionLabel = (row: EdhrBatchExecutionTaskRespVO) => {
  if (!isSpecialNode(row)) {
    if (!canOpenTask(row) && canSkipOptionalTask(row)) return '跳过表单'
    if (!canOpenTask(row) && canViewRouteFormTask(row)) return '查看表单'
    return resolveOpenTaskActionLabel(row)
  }
  if (isSterilizationNode(row)) return '完成节点'
  return '跳过节点'
}

const formatReviewTime = (value?: string | number | null) => {
  if (value == null || value === '') return '--'
  const candidate = dayjs(value)
  return candidate.isValid() ? candidate.format('YYYY-MM-DD HH:mm:ss') : String(value)
}

const resolveReleasePrecheckSummary = () => workbench.value?.releaseSummary?.precheckSummary || '尚未执行放行预检。'

const resolveReleaseStatusSummary = () => workbench.value?.releaseSummary?.releaseStatusLabel || '尚未进入放行审批。'

const resolveStageAwareReleaseStatusSummary = (stageKey: ReleaseStageKey = resolveReleaseStageKey()) => {
  if (stageKey === 'precheck' && releaseStatus.value === 'RELEASED') return '等待放行预检'
  return resolveReleaseStatusSummary()
}

const resolveArchiveStatusSummary = () => {
  const latestArchive = reviewTimeline.value?.archiveVersions?.[0]
  if (latestArchive?.archiveStatus) {
    const versionText = latestArchive.archiveVersion ? `V${latestArchive.archiveVersion}` : '最新版本'
    return `${versionText} ${latestArchive.archiveStatus}`
  }
  if (batchStatus.value === EDHR_BATCH_STATUS_ARCHIVED) return '已归档'
  return canGenerateArchive.value ? '可生成归档' : '暂无归档'
}

const selectProcessTask = (task: EdhrBatchExecutionTaskRespVO) => {
  selectedReleaseStep.value = false
  viewedReleaseStageKey.value = undefined
  selectedTaskId.value = String(task.id)
  const matchedExecution = submittedExecutionReviews.value.find((execution) => isSameRouteFormTask(task, execution))
  selectedExecutionId.value = matchedExecution ? String(matchedExecution.executionId || '') : ''
}

const selectReleaseProcess = () => {
  selectedReleaseStep.value = true
  viewedReleaseStageKey.value = undefined
  selectedTaskId.value = ''
  selectedExecutionId.value = ''
  void loadReleaseCheckItems()
}

watch(actualReleaseStageKey, (_nextStageKey, previousStageKey) => {
  if (viewedReleaseStageKey.value === previousStageKey) {
    viewedReleaseStageKey.value = undefined
  }
})

const resolveRouteQueryTaskSelection = () => {
  const queryBatchTaskId = parsePositiveRouteQueryId(route.query.batchTaskId)
  const queryWorkTaskId = parsePositiveRouteQueryId(route.query.workTaskId)
  const processCode = parseRouteQueryText(route.query.processCode)
  const processName = parseRouteQueryText(route.query.processName)
  return sortedTasks.value.find(
    (task) =>
      (queryBatchTaskId && sameRouteQueryId(task.id, queryBatchTaskId)) ||
      (queryWorkTaskId && sameRouteQueryId(task.activeWorkTaskId, queryWorkTaskId)) ||
      (processCode && task.processCode === processCode) ||
      (processName && task.processName === processName)
  )
}

const applyRouteFocus = () => {
  const focus = resolveDetailFocus()
  if (focus === 'process') {
    processDetailDialogVisible.value = true
    return
  }
  if (focus === 'precheck' || focus === 'approval') {
    void openReleaseCheckGroup()
  }
}

const resolveDefaultTaskSelection = () =>
  sortedTasks.value.find((task) => canOpenTask(task) && !isSpecialNode(task)) ||
  sortedTasks.value.find((task) => canOpenTask(task)) ||
  sortedTasks.value[0]

const applyInitialBatchTaskSelection = () => {
  const focus = resolveDetailFocus()
  if (focus === 'precheck' || focus === 'approval') {
    selectReleaseProcess()
    viewedReleaseStageKey.value = focus === 'precheck' ? 'precheck' : 'release-approval'
    return
  }
  selectedExecutionId.value = ''
  selectedTaskId.value = String(resolveRouteQueryTaskSelection()?.id || resolveDefaultTaskSelection()?.id || '')
  selectedReleaseStep.value = !selectedTaskId.value
}

const resolveRouteFormAutoOpenKey = () => {
  if (parseRouteQueryText(route.query.openRouteForm) !== '1') return ''
  const queryBatchTaskId = parsePositiveRouteQueryId(route.query.batchTaskId)
  const queryWorkTaskId = parsePositiveRouteQueryId(route.query.workTaskId)
  if (!queryBatchTaskId && !queryWorkTaskId) return ''
  return `${batchExecutionId.value || ''}:${queryBatchTaskId || ''}:${queryWorkTaskId || ''}`
}

const resolveRouteFormAssistUserId = (row: EdhrBatchExecutionTaskRespVO) => {
  if (parseRouteQueryText(route.query.openRouteForm) !== '1') return undefined
  const queryBatchTaskId = parsePositiveRouteQueryId(route.query.batchTaskId)
  const queryWorkTaskId = parsePositiveRouteQueryId(route.query.workTaskId)
  if (queryBatchTaskId && !sameRouteQueryId(row.id, queryBatchTaskId)) return undefined
  if (queryWorkTaskId && !sameRouteQueryId(row.activeWorkTaskId, queryWorkTaskId)) return undefined
  return parsePositiveRouteQueryId(route.query.assistUserId) || undefined
}

const autoOpenRouteFormFromRoute = async () => {
  if (parseRouteQueryText(route.query.openRouteForm) !== '1') return
  const routeQueryTask = resolveRouteQueryTaskSelection()
  if (!routeQueryTask || !canAutoOpenRouteFormTask(routeQueryTask)) return
  const autoOpenKey = resolveRouteFormAutoOpenKey()
  if (!autoOpenKey || routeFormAutoOpenKey === autoOpenKey) return
  routeFormAutoOpenKey = autoOpenKey
  await handleOpenTask(routeQueryTask)
}

const loadReviewTimeline = async (requestSerial?: number) => {
  if (isStaleBatchDetailRequest(requestSerial)) return
  reviewLoading.value = true
  reviewError.value = ''
  const previousSelectedExecutionId = selectedExecutionId.value
  try {
    const nextReviewTimeline = await getEdhrBatchReviewTimeline(assertBatchExecutionId())
    if (isStaleBatchDetailRequest(requestSerial)) return
    reviewTimeline.value = nextReviewTimeline
    const focus = resolveDetailFocus()
    if (focus === 'precheck' || focus === 'approval') {
      selectReleaseProcess()
      viewedReleaseStageKey.value = focus === 'precheck' ? 'precheck' : 'release-approval'
      return
    }
    const routeQueryTask = resolveRouteQueryTaskSelection()
    const defaultTask = resolveDefaultTaskSelection()
    const nextSelectedExecution = routeQueryTask
      ? undefined
      : submittedExecutionReviews.value.find(
        (execution) => String(execution.executionId) === previousSelectedExecutionId
      ) || (defaultTask && canOpenTask(defaultTask) ? undefined : submittedExecutionReviews.value[0])
    selectedExecutionId.value = nextSelectedExecution ? String(nextSelectedExecution.executionId) : ''
    selectedTaskId.value = routeQueryTask
      ? String(routeQueryTask.id)
      : nextSelectedExecution
        ? ''
        : String(defaultTask?.id || '')
    selectedReleaseStep.value = !routeQueryTask && !nextSelectedExecution && !defaultTask
  } catch (error) {
    if (isStaleBatchDetailRequest(requestSerial)) return
    reviewTimeline.value = undefined
    selectedExecutionId.value = ''
    const focus = resolveDetailFocus()
    if (focus === 'precheck' || focus === 'approval') {
      selectReleaseProcess()
      viewedReleaseStageKey.value = focus === 'precheck' ? 'precheck' : 'release-approval'
      return
    }
    selectedTaskId.value = String(resolveRouteQueryTaskSelection()?.id || resolveDefaultTaskSelection()?.id || '')
    selectedReleaseStep.value = !selectedTaskId.value
    reviewError.value = resolveErrorMessage(error, '电子批记录批次复盘时间线加载失败。')
  } finally {
    if (isStaleBatchDetailRequest(requestSerial)) return
    reviewLoading.value = false
  }
}

const cancelDeferredBatchDetailSecondaryLoad = () => {
  if (batchDetailSecondaryFrameId === undefined) return
  cancelAnimationFrame(batchDetailSecondaryFrameId)
  batchDetailSecondaryFrameId = undefined
}

const loadBatchDetailSecondaryData = async (id: string | number, requestSerial: number) => {
  secondaryLoadError.value = ''
  try {
    const nextWorkbench = await getEdhrBatchWorkbench(id)
    if (isStaleBatchDetailRequest(requestSerial)) return
    workbench.value = nextWorkbench
    await loadReviewTimeline(requestSerial)
    if (isStaleBatchDetailRequest(requestSerial)) return
    applyRouteFocus()
    if (selectedReleaseStep.value) {
      await loadReleaseCheckItems()
    }
    await autoOpenRouteFormFromRoute()
  } catch (error) {
    if (isStaleBatchDetailRequest(requestSerial)) return
    workbench.value = undefined
    reviewTimeline.value = undefined
    secondaryLoadError.value = resolveErrorMessage(error, '电子批记录批次辅助数据加载失败。')
  }
}

const deferInitialBatchDetailSecondaryLoad = (id: string | number, requestSerial: number) => {
  cancelDeferredBatchDetailSecondaryLoad()
  batchDetailSecondaryFrameId = requestAnimationFrame(() => {
    batchDetailSecondaryFrameId = undefined
    if (isStaleBatchDetailRequest(requestSerial)) return
    void loadBatchDetailSecondaryData(id, requestSerial)
  })
}

const loadDetail = async () => {
  const requestSerial = ++batchDetailRequestSerial
  cancelDeferredBatchDetailSecondaryLoad()
  loading.value = true
  loadError.value = ''
  secondaryLoadError.value = ''
  try {
    const id = assertBatchExecutionId()
    workbench.value = undefined
    reviewTimeline.value = undefined
    const nextDetail = await getEdhrBatchExecution(id)
    if (isStaleBatchDetailRequest(requestSerial)) return
    detail.value = nextDetail
    syncSpecialNodePendingAttachmentsFromDetail(nextDetail)
    applyInitialBatchTaskSelection()
    deferInitialBatchDetailSecondaryLoad(id, requestSerial)
  } catch (error) {
    if (isStaleBatchDetailRequest(requestSerial)) return
    detail.value = undefined
    syncSpecialNodePendingAttachmentsFromDetail()
    workbench.value = undefined
    reviewTimeline.value = undefined
    selectedExecutionId.value = ''
    selectedTaskId.value = ''
    loadError.value = resolveErrorMessage(error, '电子批记录批次详情加载失败。')
  } finally {
    if (!isStaleBatchDetailRequest(requestSerial)) {
      loading.value = false
    }
  }
}

const backToList = async () => {
  await router.push({ path: '/mes/pro/feedback/edhr-batch-execution' })
}

const handleSync = async () => {
  syncLoading.value = true
  try {
    detail.value = await syncEdhrBatchExecutionStatus(assertBatchExecutionId())
    syncSpecialNodePendingAttachmentsFromDetail(detail.value)
    message.success('批次状态已同步')
  } catch (error) {
    message.error(resolveErrorMessage(error, '批次状态同步失败。'))
  } finally {
    syncLoading.value = false
  }
}

const openArchivePrintDrawer = () => {
  if (!canOpenArchivePrintDrawer.value) {
    message.error(batchActionLockMessage.value)
    return
  }
  archivePrintDrawerVisible.value = true
}

const handleGenerateArchive = async () => {
  if (!ensureViewedReleaseStageWritable('生成归档')) return
  if (!canGenerateArchive.value) {
    message.error(batchActionLocked.value ? batchActionLockMessage.value : '当前批次不允许生成归档。')
    return
  }
  try {
    await generateEdhrBatchArchive({
      batchExecutionId: assertBatchExecutionId(),
      artifactType: EDHR_BATCH_ARCHIVE_ARTIFACT_FINAL_PDF,
      workTaskId: assertArchiveWorkTaskId()
    })
    message.success('批次最终归档生成已提交')
    await loadDetail()
  } catch (error) {
    message.error(resolveErrorMessage(error, '批次最终归档生成失败。'))
  }
}

const handleDownloadArchive = async () => {
  try {
    const archive = await getLatestEdhrBatchArchive(assertBatchExecutionId())
    if (!archive?.id) throw new Error('当前批次没有可下载的打印版 PDF 归档。')
    await downloadEdhrBatchArchive(archive.id, archive.fileName, archive.artifactType)
    message.success('打印版 PDF 下载已开始')
  } catch (error) {
    message.error(resolveErrorMessage(error, '打印版 PDF 下载失败。'))
  }
}

const handlePrintArchive = async () => {
  try {
    const archive = await getLatestEdhrBatchArchive(assertBatchExecutionId())
    if (!archive?.id) throw new Error('当前批次没有可打印的打印版 PDF。')
    await printEdhrBatchArchive(archive.id, archive.fileName)
    message.info('打印版 PDF 窗口已打开')
  } catch (error) {
    message.error(resolveErrorMessage(error, '打印版 PDF 打印入口打开失败。'))
  }
}

const resetReopenForm = () => {
  reopenForm.reasonCategory = ''
  reopenForm.reasonText = ''
  reopenForm.password = ''
  reopenForm.comment = ''
  reopenError.value = ''
}

const openReopenBatchDialog = () => {
  if (!ensureViewedReleaseStageWritable('申请重开')) return
  if (!canRequestReopen.value) {
    message.error(batchActionLocked.value ? batchActionLockMessage.value : '当前批次不存在，无法申请重开。')
    return
  }
  resetReopenForm()
  reopenDialogVisible.value = true
}

const submitReopenBatch = async () => {
  if (!ensureViewedReleaseStageWritable('申请重开')) return
  if (!detail.value?.id) {
    reopenError.value = '当前批次不存在，无法提交重开申请。'
    message.error(reopenError.value)
    return
  }
  if (!reopenForm.reasonCategory) {
    reopenError.value = '原因分类不能为空。'
    return
  }
  if (!reopenForm.reasonText.trim()) {
    reopenError.value = '原因说明不能为空。'
    return
  }
  if (!reopenForm.password.trim()) {
    reopenError.value = '签名密码不能为空。'
    return
  }

  reopenLoading.value = true
  reopenError.value = ''
  try {
    await requestReopenBatch({
      batchExecutionId: detail.value.id,
      reasonCategory: reopenForm.reasonCategory,
      reasonText: reopenForm.reasonText.trim(),
      password: reopenForm.password.trim(),
      comment: reopenForm.comment.trim() || undefined
    })
    reopenDialogVisible.value = false
    message.success('重开申请已提交')
    await loadDetail()
  } catch (error) {
    reopenError.value = resolveErrorMessage(error, '电子批记录批次重开申请提交失败。')
    message.error(reopenError.value)
  } finally {
    reopenLoading.value = false
  }
}

const resetReexecuteForm = () => {
  reexecuteForm.reason = ''
  reexecuteForm.remark = ''
  reexecuteError.value = ''
}

const openReexecuteRejectedBatchDialog = () => {
  if (!ensureViewedReleaseStageWritable('重新执行同批号')) return
  if (!canReexecuteRejectedBatch.value) {
    message.error(batchActionLocked.value ? batchActionLockMessage.value : '当前批次不是质量拒收终态，不能重新执行同批号。')
    return
  }
  resetReexecuteForm()
  reexecuteDialogVisible.value = true
}

const submitReexecuteRejectedBatch = async () => {
  if (!ensureViewedReleaseStageWritable('重新执行同批号')) return
  if (!canReexecuteRejectedBatch.value) {
    reexecuteError.value = batchActionLocked.value ? batchActionLockMessage.value : '当前批次不是质量拒收终态，不能重新执行同批号。'
    message.error(reexecuteError.value)
    return
  }
  if (!reexecuteForm.reason.trim()) {
    reexecuteError.value = '同批号重做原因不能为空。'
    return
  }

  reexecuteLoading.value = true
  reexecuteError.value = ''
  try {
    const reexecuted = await reexecuteRejectedEdhrBatchExecution({
      sourceRejectedBatchExecutionId: assertBatchExecutionId(),
      reason: reexecuteForm.reason.trim(),
      remark: reexecuteForm.remark.trim() || undefined
    })
    reexecuteDialogVisible.value = false
    message.success('同批号新执行尝试已创建，原拒收批次保持质量已拒收')
    if (reexecuted?.id) {
      await router.replace({
        path: route.path,
        query: {
          ...route.query,
          id: String(reexecuted.id),
          focus: 'approval'
        }
      })
    }
    await loadDetail()
  } catch (error) {
    reexecuteError.value = resolveErrorMessage(error, '同生产批号重新执行创建失败。')
    message.error(reexecuteError.value)
  } finally {
    reexecuteLoading.value = false
  }
}


const openBatchHistoryPage = async () => {
  if (!detail.value?.id) {
    message.error('当前批次不存在，无法查看历史时间线。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-history',
    query: {
      batchExecutionId: String(detail.value.id),
      ...(detail.value.batchExecutionCode ? { batchExecutionCode: detail.value.batchExecutionCode } : {}),
      ...(detail.value.workOrderCode ? { workOrderCode: detail.value.workOrderCode } : {}),
      ...(detail.value.batchCode ? { batchCode: detail.value.batchCode } : {})
    }
  })
}

const handleReleasePrecheck = async () => {
  if (!ensureViewedReleaseStageWritable('执行放行预检')) return
  if (!canRunReleasePrecheck.value) {
    const errorText = batchActionLocked.value ? batchActionLockMessage.value : '当前批次不存在，无法执行放行预检。'
    showReleaseActionError(errorText)
    message.error(errorText)
    return
  }
  const batchExecutionId = detail.value?.id
  if (!batchExecutionId) {
    const errorText = '当前批次不存在，无法执行放行预检。'
    showReleaseActionError(errorText)
    message.error(errorText)
    return
  }
  const pendingAttachmentsSaved = await ensurePendingSpecialNodeAttachmentsSavedBeforeRelease()
  if (!pendingAttachmentsSaved) return
  releasePrecheckLoading.value = true
  clearReleaseActionError()
  try {
    await precheckEdhrRelease({ batchExecutionId })
    message.success('放行预检已完成')
    await loadDetail()
    cancelDeferredBatchDetailSecondaryLoad()
    await loadBatchDetailSecondaryData(batchExecutionId, batchDetailRequestSerial)
    await loadReleaseCheckItems()
  } catch (error) {
    const errorText = resolveErrorMessage(error, '当前批次放行预检失败。')
    showReleaseActionError(errorText)
    message.error(errorText)
  } finally {
    releasePrecheckLoading.value = false
  }
}

const loadReleaseCheckItems = async () => {
  const releaseTransactionId = workbench.value?.releaseSummary?.releaseTransactionId
  if (!releaseTransactionId) {
    releaseCheckItems.value = []
    return
  }
  clearReleaseActionError()
  releaseCheckLoading.value = true
  try {
    const page = await getEdhrReleaseCheckItemPage({
      pageNo: 1,
      pageSize: 100,
      releaseTransactionId,
      itemStatus: 'OPEN',
      checkResult: ''
    })
    releaseCheckItems.value = page.list || []
  } catch (error) {
    releaseCheckItems.value = []
    const errorText = resolveErrorMessage(error, '放行预检项加载失败。')
    showReleaseActionError(errorText)
    message.error(errorText)
  } finally {
    releaseCheckLoading.value = false
  }
}

const openReleaseCheckGroup = async () => {
  selectReleaseProcess()
  await loadReleaseCheckItems()
}

const openTraceRecordGroup = () => {
  traceRecordTab.value = 'release'
  clearReleaseActionError()
  traceRecordDrawerVisible.value = true
}

const openFieldResponsibility = async (entry: TraceRecordFieldResponsibilityEntry) => {
  if (!entry.executionId) {
    throw new Error('字段责任入口缺少执行 ID，不能打开责任汇总。')
  }
  const query: Record<string, string> = {
    view: 'responsibility',
    executionId: String(entry.executionId),
    returnPath: '/mes/pro/feedback/edhr-batch-execution/detail'
  }
  appendDefinedQuery(query, 'batchExecutionId', traceRecordBatchExecutionId.value)
  appendDefinedQuery(query, 'batchExecutionCode', detail.value?.batchExecutionCode)
  await router.push({
    path: '/mes/pro/feedback/edhr-field-audit',
    query
  })
}

const openReleaseSignatureConfirmDialog = async () => {
  releaseSignatureError.value = ''
  if (!ensureViewedReleaseStageWritable('放行')) return
  if (!canSubmitRelease.value) {
    releaseSignatureError.value = edhrReleaseActionProjection.value.blockerMessage || '当前批次暂不能提交放行。'
    message.error(releaseSignatureError.value)
    return
  }
  const pendingAttachmentsSaved = await ensurePendingSpecialNodeAttachmentsSavedBeforeRelease()
  if (!pendingAttachmentsSaved) return
  const releaseTransactionId = workbench.value?.releaseSummary?.releaseTransactionId
  if (!releaseTransactionId) {
    releaseSignatureError.value = '当前批次缺少放行事务，无法执行放行动作。'
    message.error(releaseSignatureError.value)
    return
  }
  releaseSignatureForm.idempotencyKey = buildReleaseIdempotencyKey('submit')
  releaseSignatureForm.password = ''
  releaseSignatureForm.approvalOpinion = ''
  releaseSignatureConfirmVisible.value = true
}

const buildReleaseIdempotencyKey = (mode: 'submit') => {
  return `EDHR-RELEASE-${mode}-${workbench.value?.releaseSummary?.releaseTransactionId || 'NA'}-${Date.now()}`
}

const runReleaseSignatureConfirmAction = async (action: () => Promise<unknown>, successText: string) => {
  releaseSignatureSubmitting.value = true
  releaseSignatureError.value = ''
  try {
    await action()
    releaseSignatureConfirmVisible.value = false
    message.success(successText)
    await loadDetail()
  } catch (error) {
    releaseSignatureError.value = resolveErrorMessage(error, '当前批次放行失败。')
    message.error(releaseSignatureError.value)
  } finally {
    releaseSignatureSubmitting.value = false
  }
}

const submitReleaseByOwnerSignature = async (releaseTransactionId: number) => {
  const password = releaseTransactionForm.password.trim()
  if (!password) {
    throw new Error('负责人电子签名密码不能为空。')
  }
  await submitEdhrRelease({
    releaseTransactionId,
    idempotencyKey: releaseTransactionForm.idempotencyKey.trim(),
    password,
    submitReason: releaseTransactionForm.approvalOpinion.trim() || '负责人电子签名放行'
  })
}

const confirmReleaseSignatureSubmit = async () => {
  if (!ensureViewedReleaseStageWritable('放行确认')) return
  releaseSignatureError.value = ''
  const releaseTransactionId = workbench.value?.releaseSummary?.releaseTransactionId
  if (!releaseTransactionId) {
    releaseSignatureError.value = '当前批次缺少放行事务，无法执行放行动作。'
    message.error(releaseSignatureError.value)
    return
  }
  if (!releaseSignatureForm.idempotencyKey.trim()) {
    releaseSignatureError.value = '幂等键不能为空。'
    message.error(releaseSignatureError.value)
    return
  }
  if (!releaseSignatureForm.password.trim()) {
    releaseSignatureError.value = '负责人电子签名密码不能为空。'
    message.error(releaseSignatureError.value)
    return
  }
  releaseTransactionForm.password = releaseSignatureForm.password
  releaseTransactionForm.idempotencyKey = releaseSignatureForm.idempotencyKey
  releaseTransactionForm.approvalOpinion = releaseSignatureForm.approvalOpinion
  releaseTransactionForm.rejectReason = ''
  releaseTransactionForm.withdrawReason = ''
  await runReleaseSignatureConfirmAction(() => submitReleaseByOwnerSignature(releaseTransactionId), '放行已完成')
}

const resetQualityRejectForm = () => {
  qualityRejectForm.reason = ''
  qualityRejectForm.password = ''
  Object.assign(qualityRejectSignatureTimeForm, createSignatureTimeForm())
  qualityRejectError.value = ''
}

const openQualityRejectDialog = () => {
  if (!ensureViewedReleaseStageWritable('质量拒收')) return
  if (!canQualityReject.value) {
    message.error(batchActionLocked.value ? batchActionLockMessage.value : '当前批次不允许质量拒收。')
    return
  }
  resetQualityRejectForm()
  qualityRejectDialogVisible.value = true
}

const submitQualityReject = async () => {
  if (!ensureViewedReleaseStageWritable('质量拒收')) return
  if (!canQualityReject.value) {
    qualityRejectError.value = batchActionLocked.value ? batchActionLockMessage.value : '当前批次不允许质量拒收。'
    message.error(qualityRejectError.value)
    return
  }
  if (!qualityRejectForm.reason.trim()) {
    qualityRejectError.value = '拒收原因不能为空。'
    return
  }
  if (!qualityRejectForm.password.trim()) {
    qualityRejectError.value = '签名密码不能为空。'
    return
  }
  qualityRejectLoading.value = true
  qualityRejectError.value = ''
  try {
    await qualityRejectEdhrBatchExecution({
      id: assertBatchExecutionId(),
      reason: qualityRejectForm.reason.trim(),
      password: qualityRejectForm.password.trim(),
      signatureTime: buildSignatureTimePayload(qualityRejectSignatureTimeForm)
    })
    qualityRejectDialogVisible.value = false
    message.success('质量拒收已提交')
    await loadDetail()
  } catch (error) {
    qualityRejectError.value = resolveErrorMessage(error, '质量拒收失败。')
    message.error(qualityRejectError.value)
  } finally {
    qualityRejectLoading.value = false
  }
}

const handleOpenTask = async (
  row: EdhrBatchExecutionTaskRespVO,
  fillCarrier: Exclude<FillCarrier, 'UNCONFIGURED'> = 'FORM'
) => {
  const effectiveFillCarrier =
    !isGlobalRecordbookEnabled.value || (fillCarrier === 'RECORDBOOK' && !isRecordbookEnabledForContext(row))
      ? 'FORM'
      : fillCarrier
  try {
    routeFormReadonly.value = false
    if (!row.activeWorkTaskId) {
      throw new Error('当前工序缺少可填写工作任务，无法打开。')
    }
    const opened = await openEdhrBatchTask({
      batchExecutionId: assertBatchExecutionId(),
      taskId: row.id,
      workTaskId: row.activeWorkTaskId,
      assistUserId: resolveRouteFormAssistUserId(row)
    })
    if (
      opened.formCenterInstanceId &&
      opened.formTemplateId &&
      opened.instanceScope === 'BATCH_SHARED' &&
      opened.status === EDHR_BATCH_TASK_STATUS_APPROVED
    ) {
      message.success('共享表单已生效，当前任务已自动完成')
      await loadDetail()
      return
    }
    if (opened.formCenterInstanceId && opened.formTemplateId) {
      routeFormOpenedTask.value = {
        ...row,
        formBindingKey: opened.formBindingKey || row.formBindingKey,
        formTemplateId: opened.formTemplateId || row.formTemplateId,
        formTemplateName: opened.formTemplateName || row.formTemplateName,
        formTemplateVersionId: opened.formTemplateVersionId || row.formTemplateVersionId,
        formTemplateVersionNo: opened.formTemplateVersionNo || row.formTemplateVersionNo,
        formCenterInstanceId: opened.formCenterInstanceId || row.formCenterInstanceId
      }
      routeFormOpenResp.value = opened
      routeFormDrawerVisible.value = true
      return
    }
    if (!opened.executionId) throw new Error('打开工序任务后端未返回 executionId。')
    const openedWorkTaskId = opened.workTaskId || opened.executionPageQuery?.workTaskId || route.query.workTaskId
    await router.push({
      path: '/mes/pro/feedback/edhr-execution/form',
      query: {
        ...stringifyEdhrExecutionPageQuery(opened.executionPageQuery),
        id: String(opened.executionId),
        batchExecutionId: String(assertBatchExecutionId()),
        batchTaskId: String(row.id),
        executionId: String(opened.executionId),
        ...(openedWorkTaskId ? { workTaskId: String(openedWorkTaskId) } : {}),
        ...buildFillCarrierExecutionQuery(effectiveFillCarrier),
        returnPath: '/mes/pro/feedback/edhr-batch-execution/detail'
      }
    })
  } catch (error) {
    message.error(resolveErrorMessage(error, '工序任务打开失败。'))
  }
}

const openReadonlyRouteFormTask = (row: EdhrBatchExecutionTaskRespVO) => {
  selectProcessTask(row)
  if (row.formTemplateId || row.formCenterInstanceId) {
    routeFormReadonly.value = true
    routeFormOpenedTask.value = { ...row }
    routeFormOpenResp.value = undefined
    routeFormDrawerVisible.value = true
    return
  }
  message.info('已切换到只读预览。')
}

const buildSha256Hex = async (value: string) => {
  if (!window.crypto?.subtle) {
    throw new Error('当前浏览器不支持 SHA-256，无法生成流程干预签核证据。')
  }
  const digest = await window.crypto.subtle.digest('SHA-256', new TextEncoder().encode(value))
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('')
}

const buildTakeoverSignoffEvidenceHash = async (
  batchId: string | number,
  workTaskId: number,
  targetUserId: number,
  idempotencyKey: string
) => buildSha256Hex(`${batchId}|${workTaskId}|${targetUserId}|${idempotencyKey}|batch-detail-admin-takeover`)

const resolveCurrentUserId = () => {
  const userId = Number(userStore.user?.id)
  if (!Number.isFinite(userId) || userId <= 0) {
    throw new Error('当前登录用户缺少有效用户 ID，无法接管填写任务。')
  }
  return userId
}

const handleTakeOverFillTask = async (row: EdhrBatchExecutionTaskRespVO) => {
  if (!canTakeOverFillTask(row)) {
    message.error(resolveTaskGateText(row) || '当前任务不满足管理员接管条件。')
    return
  }
  const workTaskId = Number(row.activeWorkTaskId)
  const targetUserId = resolveCurrentUserId()
  const confirmed = await message.confirm(
    `当前任务填写人是 ${resolvePendingTaskFillableUsersText(row)}。确认后会通过流程干预转办给当前账号，并打开填写页。`,
    '管理员接管并填写'
  ).then(
    () => true,
    () => false
  )
  if (!confirmed) return

  fillTaskTakeoverLoading.value = workTaskId
  try {
    const batchId = assertBatchExecutionId()
    const idempotencyKey = `EDHR-FLOW-TRANSFER-${batchId}-${workTaskId}-TO-${targetUserId}-${generateUUID()}`
    const signoffEvidenceHash = await buildTakeoverSignoffEvidenceHash(batchId, workTaskId, targetUserId, idempotencyKey)
    await submitTransferIntervention({
      businessObjectType: 'WORK_TASK',
      businessObjectId: String(workTaskId),
      businessObjectCode: detail.value?.batchExecutionCode || detail.value?.batchCode || String(batchId),
      taskId: String(workTaskId),
      nodeKey: row.processName || row.processCode || resolveTaskDisplayName(row),
      fromStatus: 'TODO',
      toStatus: 'TODO',
      targetUserId,
      reasonCategory: 'ADMIN_BATCH_DETAIL_TAKEOVER',
      reason: `管理员在批次详情页接管当前填写任务：${resolveTaskDisplayName(row)}`,
      signoffEvidenceHash,
      idempotencyKey
    })
    message.success('管理员接管已生效，正在打开填写页')
    await loadDetail()
    const refreshedTask = sortedTasks.value.find((task) => task.id === row.id) || row
    await handleOpenTask(refreshedTask, resolveTakeoverFillCarrier(refreshedTask))
  } catch (error) {
    message.error(resolveErrorMessage(error, '管理员接管失败。'))
  } finally {
    fillTaskTakeoverLoading.value = undefined
  }
}

const openSkipTaskDialog = (row: EdhrBatchExecutionTaskRespVO) => {
  currentSpecialNode.value = row
  specialNodeSkipError.value = ''
  specialNodeSkipForm.reason = ''
  specialNodeSkipForm.password = ''
  specialNodeSkipDialogVisible.value = true
}

const handleSkipSpecialNode = async (row: EdhrBatchExecutionTaskRespVO) => {
  if (!canOperateSpecialNode(row)) {
    message.error('当前特殊节点不可跳过。')
    return
  }
  openSkipTaskDialog(row)
}

const handleSkipOptionalTask = async (row: EdhrBatchExecutionTaskRespVO) => {
  if (!canSkipOptionalTask(row)) {
    message.error(resolveTaskGateText(row) || '当前可选表单不可跳过。')
    return
  }
  openSkipTaskDialog(row)
}

const submitSpecialNodeSkip = async () => {
  if (!currentSpecialNode.value) {
    specialNodeSkipError.value = '当前待跳过任务不存在。'
    return
  }
  if (!specialNodeSkipForm.reason.trim()) {
    specialNodeSkipError.value = '跳过原因不能为空。'
    return
  }
  if (!specialNodeSkipForm.password.trim()) {
    specialNodeSkipError.value = '签名密码不能为空。'
    return
  }
  const taskId = currentSpecialNode.value.id
  specialNodeSkipLoading.value = true
  specialNodeActionLoading[taskId] = 'skip'
  specialNodeSkipError.value = ''
  try {
    await skipEdhrBatchSpecialNode({
      taskId,
      reason: specialNodeSkipForm.reason.trim(),
      password: specialNodeSkipForm.password.trim(),
      attachments: currentSkipTaskIsOptional.value ? [] : buildSpecialNodeSubmitAttachments(taskId)
    })
    specialNodeSkipDialogVisible.value = false
    message.success(currentSkipTaskIsOptional.value ? '可选表单已跳过' : '特殊节点已跳过')
    await loadDetail()
    clearSpecialNodePendingAttachments(taskId)
  } catch (error) {
    specialNodeSkipError.value = resolveErrorMessage(
      error,
      currentSkipTaskIsOptional.value ? '可选表单跳过失败。' : '特殊节点跳过失败。'
    )
    message.error(specialNodeSkipError.value)
  } finally {
    specialNodeSkipLoading.value = false
    specialNodeActionLoading[taskId] = undefined
  }
}

const handleCompleteSpecialNode = async (row: EdhrBatchExecutionTaskRespVO) => {
  if (!canOperateSpecialNode(row)) {
    message.error('当前特殊节点不可完成。')
    return
  }
  currentSpecialNode.value = row
  specialNodeCompleteError.value = ''
  specialNodeCompleteForm.sterilizationBatchNo = ''
  specialNodeCompleteDialogVisible.value = true
}

const triggerSelectedSpecialNodeUpload = () => {
  const task = selectedSpecialNodeForEvidence.value
  if (!task || !canUploadSpecialNodeAttachment(task)) {
    message.error('当前特殊节点在放行前才允许上传附件。')
    return
  }
  const uploadElement = (specialNodeRailUploadRef.value as unknown as { $el?: HTMLElement } | undefined)?.$el
  const fileInput = uploadElement?.querySelector<HTMLInputElement>('input[type="file"]')
  if (!fileInput) {
    throw new Error('特殊节点上传入口未渲染，无法选择文件。')
  }
  fileInput.click()
}

const uploadSelectedSpecialNodeAttachment = async (options: UploadRequestOptions) => {
  const task = selectedSpecialNodeForEvidence.value
  if (!task || !canUploadSpecialNodeAttachment(task)) {
    const error = new Error('当前特殊节点在放行前才允许上传附件。')
    options.onError(error as Parameters<UploadRequestOptions['onError']>[0])
    throw error
  }
  specialNodeAttachmentUploading.value = true
  try {
    const attachment = await prepareEdhrBatchSpecialNodeAttachmentUpload(
      {
        taskId: task.id,
        file: options.file
      },
      options.onProgress
    )
    upsertSpecialNodePendingAttachment(task.id, attachment)
    options.onSuccess(attachment)
    message.success('附件已加入待提交列表')
  } catch (error) {
    const messageText = resolveErrorMessage(error, '特殊节点附件上传失败。')
    if (currentSpecialNode.value?.id === task.id) {
      if (specialNodeSkipDialogVisible.value) specialNodeSkipError.value = messageText
      if (specialNodeCompleteDialogVisible.value) specialNodeCompleteError.value = messageText
    }
    message.error(messageText)
    options.onError(error as Parameters<UploadRequestOptions['onError']>[0])
    throw error
  } finally {
    specialNodeAttachmentUploading.value = false
  }
}

const submitSpecialNodeComplete = async () => {
  if (!currentSpecialNode.value) {
    specialNodeCompleteError.value = '当前特殊节点不存在，无法完成。'
    return
  }
  if (isSterilizationNode(currentSpecialNode.value) && !specialNodeCompleteForm.sterilizationBatchNo.trim()) {
    specialNodeCompleteError.value = '灭菌批次不能为空。'
    return
  }
  const taskId = currentSpecialNode.value.id
  specialNodeCompleteLoading.value = true
  specialNodeActionLoading[taskId] = 'complete'
  specialNodeCompleteError.value = ''
  try {
    await completeEdhrBatchSpecialNode({
      taskId,
      sterilizationBatchNo: specialNodeCompleteForm.sterilizationBatchNo.trim() || undefined,
      attachments: buildSpecialNodeSubmitAttachments(taskId)
    })
    specialNodeCompleteDialogVisible.value = false
    message.success('特殊节点已完成')
    await loadDetail()
    clearSpecialNodePendingAttachments(taskId)
  } catch (error) {
    specialNodeCompleteError.value = resolveErrorMessage(error, '特殊节点完成失败。')
    message.error(specialNodeCompleteError.value)
  } finally {
    specialNodeCompleteLoading.value = false
    specialNodeActionLoading[taskId] = undefined
  }
}


const handleOpenSelectedExecutionTask = async () => {
  if (currentProcessFillCarrier.value === 'UNCONFIGURED') {
    message.error('当前工序未配置填写方式，请先配置表单或记录本。')
    return
  }
  if (!selectedOpenableTask.value) {
    message.error('当前工序没有可打开的任务。')
    return
  }
  await handleOpenTask(selectedOpenableTask.value, currentProcessFillCarrier.value)
}

const openPendingTaskByFillCarrier = async (row: EdhrBatchExecutionTaskRespVO, fillCarrier: Exclude<FillCarrier, 'UNCONFIGURED'>) => {
  selectProcessTask(row)
  const slotBlocker = resolveTaskSlotBlocker(row)
  if (slotBlocker) {
    message.error(slotBlocker)
    return
  }
  if (!canOpenTask(row)) {
    message.error(resolveTaskGateText(row) || '当前工序尚未满足处理条件。')
    return
  }
  if (fillCarrier === 'RECORDBOOK' && isGlobalRecordbookEnabled.value && isRecordbookEnabledForContext(row)) {
    await handleOpenTask(row, 'RECORDBOOK')
    return
  }
  await handleOpenTask(row, 'FORM')
}

const handleSelectedPendingTaskAction = async (row: EdhrBatchExecutionTaskRespVO) => {
  if (isSpecialNode(row)) {
    await handlePendingTaskAction(row)
    return
  }
  if (!canOpenTask(row) && canSkipOptionalTask(row)) {
    await handleSkipOptionalTask(row)
    return
  }
  if (!canOpenTask(row) && canViewRouteFormTask(row)) {
    openReadonlyRouteFormTask(row)
    return
  }
  const fillCarrier = currentProcessFillCarrier.value
  if (fillCarrier === 'UNCONFIGURED') {
    message.error('请先选择批记录或记录本填写方式。')
    return
  }
  await openPendingTaskByFillCarrier(row, fillCarrier)
}

const handlePendingTaskAction = async (row: EdhrBatchExecutionTaskRespVO) => {
  if (!canHandlePendingTask(row)) {
    message.error(resolveTaskGateText(row) || '当前工序尚未满足处理条件。')
    return
  }
  if (!isSpecialNode(row)) {
    if (!canOpenTask(row) && canSkipOptionalTask(row)) {
      await handleSkipOptionalTask(row)
      return
    }
    if (!canOpenTask(row) && canViewRouteFormTask(row)) {
      openReadonlyRouteFormTask(row)
      return
    }
    await handleOpenTask(
      row,
      resolveFillCarrier(row.recordCategory, isRecordbookEnabledForContext(row)) === 'RECORDBOOK' ? 'RECORDBOOK' : 'FORM'
    )
    return
  }
  if (isSterilizationNode(row)) {
    await handleCompleteSpecialNode(row)
    return
  }
  await handleSkipSpecialNode(row)
}

const openSelectedProcessEvidence = async (item: ProcessEvidenceItem) => {
  if (item.key === 'open-process') {
    await handleOpenSelectedExecutionTask()
    return
  }
  if (item.disabled) {
    message.error('当前工序缺少执行记录，暂不能打开该证据明细。')
    return
  }
  if (!item.path || !item.query) {
    throw new Error(`当前工序证据入口缺少路由配置：${item.label}`)
  }
  await router.push({ path: item.path, query: item.query })
}

const activateFullHeightLayout = () => {
  document.body.classList.add(BATCH_DETAIL_PAGE_BODY_CLASS)
}

const deactivateFullHeightLayout = () => {
  document.body.classList.remove(BATCH_DETAIL_PAGE_BODY_CLASS)
}

const cleanupDeferredBatchDetailLoads = () => {
  cancelDeferredBatchDetailSecondaryLoad()
}

const loadRecordbookGlobalSetting = async () => {
  if (!hasGoldenFingerPermission.value) return
  const setting = await getEdhrRecordbookGlobalSetting()
  recordbookGlobalEnabled.value = setting.enabled === true
}

const initializeBatchDetailPage = async () => {
  activateFullHeightLayout()
  try {
    await loadRecordbookGlobalSetting()
    await loadDetail()
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '记录本全局开关加载失败。')
  }
}

onMounted(() => {
  void initializeBatchDetailPage()
})
onActivated(activateFullHeightLayout)
onDeactivated(deactivateFullHeightLayout)
onBeforeUnmount(deactivateFullHeightLayout)
onBeforeUnmount(cleanupDeferredBatchDetailLoads)
onBeforeUnmount(clearReleaseActionErrorAutoHideTimer)
watch(
  () => [route.name, route.query.id] as const,
  ([routeName, routeId]) => {
    if (
      routeName !== 'MesProEdhrBatchExecutionDetail' &&
      routeName !== 'MesProEdhrBatchExecutionReview'
    ) {
      return
    }
    const nextBatchExecutionId = parsePositiveRouteQueryId(routeId)
    if (!nextBatchExecutionId || sameRouteQueryId(nextBatchExecutionId, detail.value?.id)) {
      return
    }
    selectedFillCarrier.value = undefined
    loadDetail()
  }
)
</script>

<style scoped>
:global(body.edhr-batch-detail-page) {
  --app-footer-height: 0px;
}

.edhr-batch-detail__content-wrap {
  height: calc(
    100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding)
  );
  margin-bottom: 0 !important;
}

.edhr-batch-detail__content-wrap :deep(.el-card__body) {
  height: 100%;
  min-height: 0;
  display: flex;
}

.edhr-batch-detail {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.edhr-batch-detail__review {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.edhr-batch-detail__summary,
.edhr-batch-detail__blockers,
.edhr-batch-detail__review {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 16px;
}

.edhr-batch-detail__summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.edhr-batch-detail__title {
  color: #172033;
  font-size: 18px;
  font-weight: 600;
}

.edhr-batch-detail__meta,
.edhr-batch-detail__muted {
  color: #4b5563;
  font-size: 13px;
}

.edhr-batch-detail__commands,
.edhr-batch-detail__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.edhr-batch-detail__release-precheck {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-batch-detail__release-precheck-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
  padding: 12px 14px;
}

.edhr-batch-detail__release-precheck-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid #edf1f6;
  padding-top: 12px;
}

.edhr-batch-detail__task-name {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  line-height: 22px;
}

.edhr-batch-detail__task-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  line-height: 22px;
}

.edhr-batch-detail__section-title {
  margin-bottom: 10px;
  color: #172033;
  font-weight: 600;
}

.edhr-batch-detail__danger {
  color: #d03050;
}

.edhr-batch-detail__evidence {
  color: #166534;
  line-height: 1.5;
}

.edhr-batch-detail__blocker-list {
  display: grid;
  gap: 8px;
}

.edhr-batch-detail__blocker-item {
  color: #9f1239;
  background: #fff1f2;
  border: 1px solid #ffe4e6;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
}

.edhr-batch-detail__process-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  background: #f7f9fc;
  border-bottom: 1px solid #edf1f6;
  padding: 0 12px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-batch-detail__workbench {
  margin-top: 16px;
}

.edhr-batch-detail__workspace-bar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.edhr-batch-detail__workspace-group {
  border: 1px dashed #d6deea;
  border-radius: 8px;
  background: #fafcff;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edhr-batch-detail__workspace-title {
  color: #475467;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.edhr-batch-detail__workbench-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.edhr-batch-detail__workbench-card {
  border: 1px solid #e7edf5;
  border-radius: 8px;
  background: linear-gradient(180deg, #fbfdff 0%, #f5f8fc 100%);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edhr-batch-detail__workbench-label {
  color: #6b7280;
  font-size: 12px;
}

.edhr-batch-detail__workbench-value {
  color: #172033;
  font-size: 18px;
  font-weight: 600;
}

.edhr-batch-detail__card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.edhr-batch-detail__process-sort {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 24px;
  margin-right: 8px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  color: #172033;
  font-variant-numeric: tabular-nums;
}

.edhr-batch-detail__dialog-alert {
  margin-bottom: 12px;
}

.edhr-batch-detail__attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.edhr-batch-detail__attachment-tag {
  max-width: 100%;
}

.edhr-batch-detail__dialog-attachment-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
}


.edhr-batch-detail__section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.edhr-batch-detail__section-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.edhr-batch-detail__review-workbench {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 260px;
  gap: 12px;
  align-items: stretch;
  flex: 1;
  min-height: 0;
}

.edhr-batch-detail__review-list {
  --edhr-process-item-height: 48px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  height: 100%;
  max-height: none;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.edhr-batch-detail__process-panel,
.edhr-batch-detail__form-panel {
  min-width: 0;
}

.edhr-batch-detail__process-detail-dialog {
  max-height: 70vh;
  overflow: auto;
}

.edhr-batch-detail__process-detail-actions {
  min-width: 0;
}

.edhr-batch-detail__review-subtitle {
  color: #172033;
  font-size: 13px;
  font-weight: 700;
}

.edhr-batch-detail__review-item {
  box-sizing: border-box;
  flex: 0 0 var(--edhr-process-item-height);
  width: 100%;
  height: var(--edhr-process-item-height);
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  color: #172033;
  cursor: pointer;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 8px;
  min-height: var(--edhr-process-item-height);
  padding: 7px 8px;
  text-align: left;
}

.edhr-batch-detail__review-item:hover,
.edhr-batch-detail__review-item:focus-visible {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
  outline: none;
}

.edhr-batch-detail__review-item.is-active {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.edhr-batch-detail__review-main {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-weight: 600;
}

.edhr-batch-detail__review-main .edhr-batch-detail__process-sort,
.edhr-batch-detail__pending-task-main .edhr-batch-detail__process-sort {
  flex: 0 0 32px;
  margin-right: 0;
}

.edhr-batch-detail__process-code {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__review-process-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__pending-task-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__review-report {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.4;
}

.edhr-batch-detail__review-item > .el-tag {
  grid-column: 2;
  grid-row: 1;
  align-self: center;
  justify-self: end;
}

.edhr-batch-detail__pending-task-list {
  display: grid;
  flex: 0 0 auto;
  gap: 6px;
  margin-bottom: 0;
}

.edhr-batch-detail__pending-task-title {
  color: #172033;
  font-size: 13px;
  font-weight: 700;
}

.edhr-batch-detail__pending-task-item {
  box-sizing: border-box;
  flex: 0 0 var(--edhr-process-item-height);
  width: 100%;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f7f9fc;
  color: #172033;
  cursor: pointer;
  height: var(--edhr-process-item-height);
  min-height: var(--edhr-process-item-height);
  overflow: hidden;
  padding: 7px 8px;
  text-align: left;
  display: flex;
  align-items: center;
}

.edhr-batch-detail__pending-task-item:hover,
.edhr-batch-detail__pending-task-item:focus-visible,
.edhr-batch-detail__pending-task-item.is-active {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
  outline: none;
}

.edhr-batch-detail__pending-task-main {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  width: 100%;
  font-weight: 600;
}

.edhr-batch-detail__pending-task-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.edhr-batch-detail__pending-task-fillable {
  color: #263247;
  font-size: 12px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__process-task-group {
  --edhr-process-state-background: #f7f9fc;
  box-sizing: border-box;
  flex: 0 0 var(--edhr-process-item-height);
  height: var(--edhr-process-item-height);
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: var(--edhr-process-state-background);
}

.edhr-batch-detail__process-task-group.is-completed {
  --edhr-process-state-background: #f0f9eb;
}

.edhr-batch-detail__process-task-group.is-in-progress {
  --edhr-process-state-background: #fff8e6;
}

.edhr-batch-detail__process-task-group.is-active {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.edhr-batch-detail__process-task-group-head {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  border: 0;
  background: var(--edhr-process-state-background);
  color: #172033;
  cursor: pointer;
  padding: 7px 8px;
  text-align: left;
}

.edhr-batch-detail__process-task-group-head:hover,
.edhr-batch-detail__process-task-group-head:focus-visible {
  background: var(--edhr-process-state-background);
  outline: none;
}

.edhr-batch-detail__rail-process-forms {
  display: grid;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px solid #e5ebf3;
}

.edhr-batch-detail__preview-mode-switch {
  display: grid;
  grid-template-columns: auto auto auto;
  align-items: center;
  justify-content: end;
  column-gap: 8px;
  row-gap: 4px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  padding: 8px;
}

.edhr-batch-detail__preview-mode-label {
  color: #263247;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.edhr-batch-detail__preview-mode-disabled {
  grid-column: 1 / -1;
  justify-self: stretch;
  display: inline-flex;
  min-height: 22px;
  align-items: center;
  justify-content: center;
  border: 1px solid #dbe3ef;
  border-radius: 4px;
  background: #f8fafc;
  color: #475467;
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
  white-space: nowrap;
}

.edhr-batch-detail__assist-preview {
  display: grid;
  gap: 12px;
  padding: 16px;
  min-height: 360px;
  background: #f8fafc;
}

.edhr-batch-detail__assist-preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px 14px;
}

.edhr-batch-detail__assist-preview-head strong {
  display: block;
  color: #172033;
  font-size: 14px;
  line-height: 1.4;
}

.edhr-batch-detail__assist-preview-head span {
  display: block;
  margin-top: 4px;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-batch-detail__assist-grid-list {
  display: grid;
  gap: 16px;
}

.edhr-batch-detail__assist-grid-group {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.edhr-batch-detail__assist-grid-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #5a3d05;
  padding: 0 2px;
}

.edhr-batch-detail__assist-grid-meta strong {
  font-size: 13px;
  font-weight: 700;
}

.edhr-batch-detail__assist-grid-surface {
  min-width: 0;
  overflow: auto;
  border: 1px solid #f0d783;
  border-radius: 8px;
  background: #fff8d6;
  padding: 8px;
}

.edhr-batch-detail__assist-grid {
  min-width: 720px;
  width: 100%;
  border-collapse: separate;
  border-spacing: 8px;
  table-layout: fixed;
}

.edhr-batch-detail__assist-grid td {
  padding: 0;
  vertical-align: stretch;
}

.edhr-batch-detail__assist-grid-cell {
  display: flex;
  min-height: 96px;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  border: 1px solid #edd07b;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  color: #5a3d05;
  padding: 10px;
  text-align: left;
}

.edhr-batch-detail__assist-grid-cell.is-mapped {
  border-color: #d9a821;
  background: #ffffff;
}

.edhr-batch-detail__assist-grid-cell.is-empty {
  color: #9b7b2a;
}

.edhr-batch-detail__assist-grid-cell span {
  display: -webkit-box;
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.edhr-batch-detail__assist-grid-cell.is-empty span {
  color: #9b7b2a;
  font-weight: 600;
}

.edhr-batch-detail__assist-grid-cell small {
  min-width: 0;
  overflow: hidden;
  color: #8a6a1c;
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__assist-grid-cell em {
  align-self: flex-start;
  border-radius: 8px;
  background: #fff4c2;
  color: #785a0b;
  font-size: 11px;
  font-style: normal;
  padding: 2px 8px;
}

.edhr-batch-detail__rail-process-form-list {
  display: grid;
  gap: 6px;
}

.edhr-batch-detail__rail-process-form-item {
  position: relative;
  display: grid;
  gap: 5px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  color: #172033;
  cursor: pointer;
  padding: 8px;
}

.edhr-batch-detail__rail-process-form-item:hover,
.edhr-batch-detail__rail-process-form-item:focus-visible {
  border-color: #8bbcff;
  background: #f7fbff;
  outline: none;
}

.edhr-batch-detail__rail-process-form-item.is-active {
  border-color: #1677ff;
  background: #fff8e6;
  box-shadow: inset 3px 0 0 #1677ff;
}

.edhr-batch-detail__rail-process-form-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.edhr-batch-detail__rail-process-form-slot {
  min-width: 0;
  overflow: hidden;
  color: #263247;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__rail-process-form-state-tags {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 4px;
}

.edhr-batch-detail__rail-process-form-optional-tag {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #b54708;
}

.edhr-batch-detail__rail-process-form-name {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
  overflow-wrap: anywhere;
  white-space: normal;
}

.edhr-batch-detail__rail-process-form-filler {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 6px;
  color: #475467;
  font-size: 11px;
  line-height: 1.35;
}

.edhr-batch-detail__rail-process-form-filler span {
  color: #667085;
}

.edhr-batch-detail__rail-process-form-filler strong {
  min-width: 0;
  overflow: hidden;
  color: #344054;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__rail-process-form-gate {
  color: #b42318;
  font-size: 11px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.edhr-batch-detail__rail-process-form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.edhr-batch-detail__rail-process-form-action {
  justify-self: start;
  border: 0;
  background: transparent;
  color: #1677ff;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  padding: 0;
}

.edhr-batch-detail__rail-process-form-action.is-skip {
  color: #b54708;
}

.edhr-batch-detail__rail-process-form-action:disabled {
  color: #98a2b3;
  cursor: not-allowed;
}

.edhr-batch-detail__fill-carrier-control-wrap {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  border: 1px solid #d5dce8;
  border-radius: 999px;
  background: #ffffff;
  padding: 2px;
}

.edhr-batch-detail__fill-carrier-option {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 22px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #475467;
  cursor: pointer;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.edhr-batch-detail__fill-carrier-option:hover,
.edhr-batch-detail__fill-carrier-option:focus-visible {
  color: #1677ff;
  outline: none;
}

.edhr-batch-detail__fill-carrier-option.is-active {
  background: #1677ff;
  color: #0958d9;
  color: #ffffff;
}

.edhr-batch-detail__pending-task-action {
  flex-shrink: 0;
}

.edhr-batch-detail__rail-task-detail {
  display: grid;
  gap: 8px;
}

.edhr-batch-detail__rail-task-title {
  color: #172033;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.3;
}

.edhr-batch-detail__rail-task-action {
  width: 100%;
  margin-left: 0;
}

.edhr-batch-detail__special-node-action-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  flex-shrink: 0;
}

.edhr-batch-detail__special-node-hidden-upload {
  width: 0;
  height: 0;
  overflow: hidden;
  pointer-events: none;
}

.edhr-batch-detail__special-node-hidden-upload-trigger {
  width: 0;
  height: 0;
  padding: 0;
  border: 0;
  overflow: hidden;
}

.edhr-batch-detail__process-report {
  overflow-wrap: anywhere;
}

.edhr-batch-detail__review-preview {
  height: 100%;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-radius: 6px;
}

.edhr-batch-detail__preview-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 42px;
  flex-shrink: 0;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  padding: 7px 10px;
  transition: background-color 0.2s ease;
}

.edhr-batch-detail__preview-header.is-batch-record {
  background: #f2f7ff;
}

.edhr-batch-detail__preview-header.is-recordbook {
  background: #fff8e6;
}

.edhr-batch-detail__preview-context {
  display: flex;
  align-items: center;
  flex: 1 1 0;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 12px;
  line-height: 1.35;
}

.edhr-batch-detail__preview-context span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__preview-context span:first-child {
  flex: 0 1 auto;
  font-weight: 700;
}

.edhr-batch-detail__preview-context span:last-child {
  flex: 1 1 auto;
  color: #344054;
}

.edhr-batch-detail__preview-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  justify-self: center;
  gap: 12px;
  min-width: 0;
}

.edhr-batch-detail__preview-extra {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  justify-self: end;
  gap: 12px;
  min-width: 0;
}

.edhr-batch-detail__preview-route-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 1 auto;
  min-width: 0;
  max-width: min(360px, 100%);
  overflow: hidden;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #1677ff;
  cursor: pointer;
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__preview-route-link:hover,
.edhr-batch-detail__preview-route-link:focus-visible {
  color: #0958d9;
  text-decoration: underline;
  outline: none;
}

.edhr-batch-detail__preview-route-link:focus-visible {
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.18);
}

.edhr-batch-detail__preview-route-link:disabled {
  color: #98a2b3;
  cursor: not-allowed;
  text-decoration: none;
}

.edhr-batch-detail__preview-form-version {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  min-height: 22px;
  max-width: 140px;
  overflow: hidden;
  border: 1px solid #b7d5ff;
  border-radius: 999px;
  background: #ffffff;
  color: #0958d9;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__preview-carrier {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  flex: 0 1 auto;
  min-width: 0;
}

.edhr-batch-detail__preview-carrier-control {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  border: 1px solid #d5dce8;
  border-radius: 999px;
  background: #ffffff;
  padding: 2px;
}

.edhr-batch-detail__preview-carrier-option {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 22px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #475467;
  cursor: pointer;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.edhr-batch-detail__preview-carrier-option:hover,
.edhr-batch-detail__preview-carrier-option:focus-visible {
  color: #1677ff;
  outline: none;
}

.edhr-batch-detail__preview-carrier-option.is-active {
  background: #1677ff;
  color: #ffffff;
}

.edhr-batch-detail__review-rail {
  position: sticky;
  top: 12px;
  min-width: 0;
  height: 100%;
  min-height: 0;
  max-height: none;
  overflow: auto;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.edhr-batch-detail__release-rail-head {
  display: grid;
  gap: 4px;
}

.edhr-batch-detail__release-owner-hint {
  color: #263247;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.edhr-batch-detail__release-rail-actions {
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: repeat(auto-fit, minmax(132px, 1fr));
  gap: 10px;
  flex: 1;
  min-height: 0;
}

.edhr-batch-detail__release-image-action {
  position: relative;
  isolation: isolate;
  width: 100%;
  height: 100%;
  min-height: 132px;
  margin-left: 0;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 12px;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
  text-align: center;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease,
    opacity 0.2s ease;
}

.edhr-batch-detail__release-image-action::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background-image:
    radial-gradient(circle at 22% 18%, rgba(255, 255, 255, 0.34), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0));
}

.edhr-batch-detail__release-image-action:hover,
.edhr-batch-detail__release-image-action:focus-visible {
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.16);
  outline: none;
  transform: translateY(-1px);
}

.edhr-batch-detail__release-image-action:disabled {
  cursor: not-allowed;
  opacity: 0.58;
  transform: none;
  box-shadow: none;
}

.edhr-batch-detail__release-image-action.is-release-reject {
  border-color: #f87171;
  background-image: linear-gradient(160deg, #fb7185 0%, #ef4444 46%, #b91c1c 100%);
}

.edhr-batch-detail__release-image-action.is-release-signature {
  border-color: #2dd4bf;
  background-image: linear-gradient(160deg, #14b8a6 0%, #059669 52%, #047857 100%);
}

.edhr-batch-detail__release-image-action-visual {
  width: 58px;
  height: 58px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  border-radius: 18px;
  background-color: rgba(255, 255, 255, 0.18);
  background-position: center;
  background-repeat: no-repeat;
  background-size: 34px 34px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.24);
}

.edhr-batch-detail__release-image-action.is-release-reject .edhr-batch-detail__release-image-action-visual {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='64' height='64' viewBox='0 0 64 64'%3E%3Cpath fill='none' stroke='white' stroke-width='6' stroke-linecap='round' d='M19 19l26 26M45 19L19 45'/%3E%3C/svg%3E");
}

.edhr-batch-detail__release-image-action.is-release-signature .edhr-batch-detail__release-image-action-visual {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='64' height='64' viewBox='0 0 64 64'%3E%3Cpath fill='none' stroke='white' stroke-width='6' stroke-linecap='round' stroke-linejoin='round' d='M16 34l11 11 22-27'/%3E%3C/svg%3E");
}

.edhr-batch-detail__release-image-action-label {
  color: #ffffff;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.edhr-batch-detail__release-image-action-status {
  color: rgba(255, 255, 255, 0.84);
  font-size: 12px;
  font-weight: 600;
}

.edhr-batch-detail__release-stage-panel {
  display: grid;
  gap: 6px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 10px;
}

.edhr-batch-detail__release-stage-panel.is-quality-terminal {
  border-color: #f6c9c9;
  background: #fff7f7;
}

.edhr-batch-detail__release-stage-panel.is-release-approval,
.edhr-batch-detail__release-stage-panel.is-precheck {
  border-color: #bfdbfe;
  background: #f5f9ff;
}

.edhr-batch-detail__release-stage-panel.is-archived,
.edhr-batch-detail__release-stage-panel.is-archive {
  border-color: #b7ebc6;
  background: #f6fffa;
}

.edhr-batch-detail__release-stage-status {
  color: #172033;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.edhr-batch-detail__release-stage-contact {
  color: #263247;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.edhr-batch-detail__rail-summary {
  display: grid;
  gap: 8px;
}

.edhr-batch-detail__rail-item {
  min-width: 0;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #ffffff;
  padding: 8px;
}

.edhr-batch-detail__rail-label {
  margin-bottom: 4px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.3;
}

.edhr-batch-detail__rail-value {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.edhr-batch-detail__rail-item--signoff {
  padding-bottom: 10px;
}

.edhr-batch-detail__signoff-summary {
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
}

.edhr-batch-detail__signoff-trigger {
  display: flex;
  width: 100%;
  min-height: 30px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fafcff;
  padding: 5px 7px;
  color: #172033;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.edhr-batch-detail__signoff-trigger:hover,
.edhr-batch-detail__signoff-trigger:focus-visible {
  border-color: #1677ff;
  background: #eef6ff;
  outline: none;
}

.edhr-batch-detail__signoff-trigger.is-empty {
  color: #6b7280;
  background: #ffffff;
}

.edhr-batch-detail__signoff-label {
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
}

.edhr-batch-detail__signoff-count {
  flex: 0 0 auto;
  color: #1677ff;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.edhr-batch-detail__signoff-trigger.is-empty .edhr-batch-detail__signoff-count {
  color: #6b7280;
}

.edhr-batch-detail__signoff-detail {
  display: grid;
  gap: 10px;
}

.edhr-batch-detail__signoff-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-batch-detail__signoff-detail-list {
  display: grid;
  gap: 8px;
  max-height: 280px;
  overflow: auto;
}

.edhr-batch-detail__signoff-detail-item {
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #fafcff;
  padding: 8px;
}

.edhr-batch-detail__signoff-detail-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.edhr-batch-detail__signoff-person {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-batch-detail__signoff-time {
  flex: 0 0 auto;
  color: #4b5563;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.edhr-batch-detail__signoff-detail-meta {
  margin-top: 5px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.edhr-batch-detail__review-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.edhr-batch-detail__form-surface {
  flex: 0 0 auto;
  height: auto;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
}

.edhr-batch-detail__release-stage-next {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
}

.edhr-batch-detail__release-stage-owner {
  margin-bottom: 4px;
  color: #172033;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.edhr-batch-detail__release-main-workspace {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.edhr-batch-detail__special-node-attachments {
  display: grid;
  align-content: start;
  gap: 12px;
  height: 100%;
  min-height: 0;
  overflow: auto;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 14px;
}

.edhr-batch-detail__special-node-attachments-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #edf1f6;
  padding-bottom: 10px;
}

.edhr-batch-detail__special-node-attachments-title {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
}

.edhr-batch-detail__special-node-attachment-section {
  display: grid;
  gap: 8px;
  min-width: 0;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #fafcff;
  padding: 10px;
}

.edhr-batch-detail__special-node-attachment-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #263247;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
}

.edhr-batch-detail__special-node-file-list {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.edhr-batch-detail__special-node-file-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #ffffff;
  padding: 7px 8px;
}

.edhr-batch-detail__special-node-file-row.is-readonly {
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  background: #f7f9fc;
}

.edhr-batch-detail__special-node-file-name {
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.edhr-batch-detail__special-node-file-name:hover,
.edhr-batch-detail__special-node-file-name:focus-visible {
  color: #0958d9;
  text-decoration: underline;
  outline: none;
}

.edhr-batch-detail__special-node-file-meta {
  color: #6b7280;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  line-height: 1.35;
  white-space: nowrap;
}

.edhr-batch-detail__special-node-file-status {
  border: 1px solid #bfdbfe;
  border-radius: 5px;
  background: #eff6ff;
  color: #1677ff;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}

.edhr-batch-detail__special-node-file-status.is-persisted {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.edhr-batch-detail__special-node-file-action {
  padding: 0;
  border: 0;
  background: transparent;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
  white-space: nowrap;
  cursor: pointer;
}

.edhr-batch-detail__special-node-file-action:hover,
.edhr-batch-detail__special-node-file-action:focus-visible {
  color: #0958d9;
  text-decoration: underline;
  outline: none;
}

.edhr-batch-detail__special-node-file-action.is-danger {
  color: #c2410c;
}

.edhr-batch-detail__special-node-file-action.is-danger:hover,
.edhr-batch-detail__special-node-file-action.is-danger:focus-visible {
  color: #9a3412;
}

.edhr-batch-detail__release-precheck-card {
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #ffffff;
  padding: 14px;
  display: grid;
  gap: 12px;
  height: 100%;
  align-content: start;
}

.edhr-batch-detail__release-precheck-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.edhr-batch-detail__release-precheck-title {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
}

.edhr-batch-detail__release-precheck-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.edhr-batch-detail__action-surface {
  position: sticky;
  top: 12px;
}

.edhr-batch-detail__execution-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.edhr-batch-detail__process-evidence {
  border: 1px solid #edf1f6;
  border-radius: 8px;
  background: #fafcff;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-batch-detail__process-evidence-groups {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.edhr-batch-detail__process-evidence-context {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  padding: 8px 10px;
}

.edhr-batch-detail__process-evidence-context-title {
  display: block;
  color: #172033;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
}

.edhr-batch-detail__process-evidence-context small,
.edhr-batch-detail__process-evidence-context-entry {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.35;
}

.edhr-batch-detail__process-evidence-group {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 10px;
  min-width: 0;
}

.edhr-batch-detail__process-evidence-group-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.edhr-batch-detail__process-evidence-group-title span {
  color: #172033;
  font-weight: 700;
}

.edhr-batch-detail__process-evidence-group-title small {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-batch-detail__process-evidence-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.edhr-batch-detail__process-evidence-item {
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  color: #172033;
  cursor: pointer;
  min-height: 72px;
  padding: 10px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.edhr-batch-detail__process-evidence-item:hover,
.edhr-batch-detail__process-evidence-item:focus-visible {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
  outline: none;
}

.edhr-batch-detail__process-evidence-item:disabled {
  color: #9ca3af;
  background: #f7f9fc;
  cursor: not-allowed;
}

.edhr-batch-detail__process-evidence-item span {
  font-weight: 700;
}

.edhr-batch-detail__process-evidence-item small {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-batch-detail__trace-field-responsibility {
  display: grid;
  gap: 12px;
  padding-top: 4px;
}

.edhr-batch-detail__trace-field-responsibility-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
  padding: 10px 12px;
}

.edhr-batch-detail__trace-field-responsibility-title {
  color: #172033;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

@media (max-width: 1280px) {
  .edhr-batch-detail__workbench-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .edhr-batch-detail__process-evidence-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .edhr-batch-detail__content-wrap {
    height: auto;
    min-height: 0;
  }

  .edhr-batch-detail__summary-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .edhr-batch-detail__review-workbench {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 0;
  }

  .edhr-batch-detail__review-list {
    height: auto;
    max-height: none;
    min-height: 0;
  }

  .edhr-batch-detail__review-preview {
    height: auto;
  }

  .edhr-batch-detail__review-card {
    height: auto;
    max-height: none;
    overflow: visible;
    overscroll-behavior: auto;
    scrollbar-gutter: auto;
  }

  .edhr-batch-detail__preview-header {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .edhr-batch-detail__preview-actions {
    justify-self: start;
    flex-wrap: wrap;
  }

  .edhr-batch-detail__preview-extra {
    justify-self: start;
    justify-content: flex-start;
    flex-wrap: wrap;
    width: 100%;
  }

  .edhr-batch-detail__preview-route-link {
    align-self: flex-start;
    max-width: 100%;
  }

  .edhr-batch-detail__preview-carrier {
    justify-content: flex-start;
    width: 100%;
  }

  .edhr-batch-detail__review-rail {
    position: static;
    height: auto;
    max-height: none;
  }

  .edhr-batch-detail__workbench-grid {
    grid-template-columns: 1fr;
  }

  .edhr-batch-detail__process-evidence-groups {
    grid-template-columns: 1fr;
  }

  .edhr-batch-detail__process-evidence-grid {
    grid-template-columns: 1fr;
  }

  .edhr-batch-detail__release-precheck-head {
    flex-direction: column;
  }

  .edhr-batch-detail__release-precheck-toolbar {
    justify-content: flex-start;
  }
}
</style>
