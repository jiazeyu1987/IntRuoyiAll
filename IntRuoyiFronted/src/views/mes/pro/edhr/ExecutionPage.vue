<template>
  <ContentWrap
    class="edhr-execution-content-wrap"
    :body-style="isTrackingReadonlyMode ? { padding: '10px' } : { padding: '0' }"
  >
    <div class="edhr-page-shell">
      <div class="edhr-page-shell__toolbar">
        <div>
          <div class="edhr-page-shell__title">{{ executionPageTitle }}</div>
          <div class="edhr-page-shell__subtitle">{{ executionPageSubtitle }}</div>
        </div>
        <div class="edhr-page-shell__actions">
          <el-button @click="handleBackToList">
            {{ backToBatchLabel }}
          </el-button>
          <el-button
            v-if="!isTrackingReadonlyMode && !isReadonly"
            v-hasPermi="['mes:pro-batch-record-execution:field-audit-update', 'mes:pro-batch-record-execution:update', 'mes:pro-batch-record-execution:golden-finger']"
            :loading="fieldAuditSaveLoading"
            :disabled="!canSaveFieldAuditChanges"
            @click="handleSaveFieldAuditChanges"
          >
            保存变更
          </el-button>
          <el-button
            v-if="!isTrackingReadonlyMode && showFormReviewSignAction"
            v-hasPermi="['mes:pro-batch-record-execution:update']"
            :loading="formReviewSignLoading"
            :disabled="!canOpenFormReviewSignDialog"
            @click="openFormReviewSignDialog"
          >
            复核签名
          </el-button>
          <el-button
            v-if="!isTrackingReadonlyMode && !isReadonly"
            v-hasPermi="['mes:pro-batch-record-execution:update', 'mes:pro-batch-record-execution:golden-finger']"
            type="success"
            :loading="submitLoading"
            :disabled="hasSlotContextBlockers || hasPendingFieldChanges"
            @click="openSubmitDialog"
          >
            提交执行
          </el-button>
          <el-button type="primary" :loading="loading" @click="loadExecution">刷新</el-button>
        </div>
      </div>

      <el-alert
        v-if="isTrackingReadonlyMode && loadError"
        :title="loadError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-page-shell__alert"
      />

      <div
        v-loading="loading"
        class="edhr-page-shell__content"
        :class="{ 'is-fill-workspace': !isTrackingReadonlyMode }"
      >
        <template v-if="execution">
          <template v-if="isTrackingReadonlyMode">
            <div class="edhr-page-shell__tracking-detail">
              <aside class="edhr-page-shell__tracking-params">
                <div class="edhr-page-shell__section-title">执行参数</div>
                <el-descriptions :column="1" border class="edhr-page-shell__tracking-param-list">
                  <el-descriptions-item label="执行编号">
                    {{ execution.executionCode || execution.id || '--' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="状态">{{ executionStatusText }}</el-descriptions-item>
                  <el-descriptions-item label="工单">
                    {{ execution.workOrderCode || execution.workOrderId || '--' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="批次">
                    {{ execution.batchCode || '--' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="工序">{{ semanticSummary.process }}</el-descriptions-item>
                  <el-descriptions-item label="工作站">{{ semanticSummary.workstation }}</el-descriptions-item>
                  <el-descriptions-item label="提交时间">
                    {{ formatTrackingReadonlyDate(execution.submittedAt) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="完成时间">
                    {{ formatTrackingReadonlyDate(execution.closedAt || execution.approvedAt) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="关闭时间">
                    {{ formatTrackingReadonlyDate(execution.closedAt) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="签名次数">{{ signatureRows.length }}</el-descriptions-item>
                  <el-descriptions-item label="归档状态">
                    <el-tag :type="resolveArchiveStatusType(latestArchive?.archiveStatus)">
                      {{ resolveArchiveStatusLabel(latestArchive?.archiveStatus) }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>

                <div
                  v-if="hasArchiveQueryPermission"
                  v-hasPermi="['mes:pro-batch-record-execution-archive:query']"
                  v-loading="archiveLoading"
                  class="edhr-page-shell__tracking-archive"
                >
                  <div class="edhr-page-shell__section-head">
                    <div>
                      <div class="edhr-page-shell__section-title">单表归档</div>
                      <div class="edhr-page-shell__section-subtitle">
                        当前执行记录的归档状态、生成和下载
                      </div>
                    </div>
                  </div>
                  <el-alert
                    v-if="archiveError"
                    :title="archiveError"
                    type="error"
                    :closable="false"
                    show-icon
                    class="edhr-page-shell__archive-alert"
                  />
                  <div class="edhr-page-shell__tracking-archive-meta">
                    <div>
                      <span>归档版本</span>
                      <strong>{{ latestArchive?.archiveVersion ? `V${latestArchive.archiveVersion}` : '--' }}</strong>
                    </div>
                    <div>
                      <span>归档格式</span>
                      <strong>{{ latestArchive?.artifactType || archiveArtifactType }}</strong>
                    </div>
                    <div>
                      <span>生成时间</span>
                      <strong>{{ formatTrackingReadonlyDate(latestArchive?.generatedAt) }}</strong>
                    </div>
                    <div>
                      <span>封存时间</span>
                      <strong>{{ formatTrackingReadonlyDate(latestArchive?.sealedAt) }}</strong>
                    </div>
                    <div>
                      <span>审批快照ID</span>
                      <strong>{{ latestArchive?.approvalSnapshotId || '--' }}</strong>
                    </div>
                    <div>
                      <span>审批快照摘要</span>
                      <strong>{{ latestArchive?.approvalSnapshotHash || '--' }}</strong>
                    </div>
                    <div class="edhr-page-shell__tracking-archive-meta-wide">
                      <span>SHA-256</span>
                      <strong>{{ latestArchive?.sha256 || '--' }}</strong>
                    </div>
                  </div>
                  <el-alert
                    v-if="latestArchive?.failureReason"
                    :title="latestArchive.failureReason"
                    type="error"
                    :closable="false"
                    show-icon
                    class="edhr-page-shell__archive-alert"
                  />
                  <div class="edhr-page-shell__tracking-archive-actions">
                    <el-button
                      v-hasPermi="['mes:pro-batch-record-execution-archive:create']"
                      type="primary"
                      :loading="archiveGenerateLoading"
                      :disabled="!canGenerateCurrentArchive"
                      @click="openArchiveGenerateDialog"
                    >
                      {{ archiveGenerateActionLabel }}
                    </el-button>
                    <el-button
                      v-hasPermi="['mes:pro-batch-record-execution-archive:download']"
                      :loading="archiveDownloadLoading"
                      :disabled="!canDownloadCurrentArchive"
                      @click="handleDownloadArchive"
                    >
                      下载归档打印件
                    </el-button>
                  </div>
                  <div v-if="archiveGateHint" class="edhr-page-shell__tracking-archive-hint">
                    {{ archiveGateHint }}
                  </div>
                </div>
              </aside>

              <section class="edhr-page-shell__tracking-form">
                <div class="edhr-page-shell__section-head">
                  <div>
                    <div class="edhr-page-shell__section-title">电子批记录表单</div>
                    <div class="edhr-page-shell__section-subtitle">
                      按原始模板布局展示填写值和签名证据
                    </div>
                  </div>
                </div>
                <el-alert
                  v-if="trackingReadonlyFormRenderError"
                  :title="trackingReadonlyFormRenderError"
                  type="error"
                  :closable="false"
                  show-icon
                  class="edhr-page-shell__archive-alert"
                />
                <el-alert
                  v-else-if="signatureError"
                  :title="signatureError"
                  type="error"
                  :closable="false"
                  show-icon
                  class="edhr-page-shell__archive-alert"
                />
                <EdhrExecutionReadonlyForm
                  v-else
                  :form-view-model="trackingReadonlyFormViewModel"
                  :signature-records="signatureRows"
                />
              </section>

              <section class="edhr-page-shell__tracking-audit">
                <div class="edhr-page-shell__section-head">
                  <div>
                    <div class="edhr-page-shell__section-title">追踪时间线</div>
                    <div class="edhr-page-shell__section-subtitle">
                      从真实电子签名与流程事件生成，只读用于审计核对
                    </div>
                  </div>
                </div>
                <el-alert
                  v-if="trackingError"
                  :title="trackingError"
                  type="error"
                  :closable="false"
                  show-icon
                  class="edhr-page-shell__archive-alert"
                />
                <el-table :data="trackingTimeline" stripe :show-overflow-tooltip="true">
                  <el-table-column label="事件" prop="eventType" width="120" />
                  <el-table-column label="流程任务" prop="bpmTaskId" min-width="160" />
                  <el-table-column label="节点" prop="nodeName" min-width="140" />
                  <el-table-column label="处理人" prop="actorName" width="140" />
                  <el-table-column label="结果" prop="result" width="100" />
                  <el-table-column label="意见/原因" min-width="220">
                    <template #default="{ row }">
                      {{ row.rejectReason || row.comment || '--' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="处理时间" prop="occurredAt" width="180" :formatter="edhrDateTimeFormatter" />
                </el-table>
              </section>
            </div>
          </template>

          <template v-else>
            <div ref="fillWorkspaceRef" class="edhr-fill-workspace">
              <aside class="edhr-fill-workspace__rail">
                <div class="edhr-fill-workspace__rail-scroll">
                  <div class="edhr-fill-workspace__section">
                    <div class="edhr-fill-workspace__section-label">显示方式</div>
                    <div class="edhr-fill-workspace__fit-actions">
                      <button
                        type="button"
                        :class="{ 'is-active': fitMode === 'width' }"
                        :aria-pressed="fitMode === 'width'"
                        @click="fitMode = 'width'"
                      >
                        <Icon icon="ep:full-screen" />
                        <span>适应宽度</span>
                      </button>
                      <button
                        type="button"
                        :class="{ 'is-active': fitMode === 'height' }"
                        :aria-pressed="fitMode === 'height'"
                        @click="fitMode = 'height'"
                      >
                        <Icon icon="ep:rank" />
                        <span>适应高度</span>
                      </button>
                    </div>
                  </div>

                  <div class="edhr-fill-workspace__section">
                    <div class="edhr-fill-workspace__section-label">填写模式</div>
                    <div class="edhr-fill-workspace__view-actions">
                      <button
                        type="button"
                        :class="{ 'is-active': fillViewMode === 'assist' }"
                        :aria-pressed="fillViewMode === 'assist'"
                        @click="fillViewMode = 'assist'"
                      >
                        <Icon icon="ep:list" />
                        <span>填写辅助模式</span>
                      </button>
                      <button
                        type="button"
                        :class="{ 'is-active': fillViewMode === 'original' }"
                        :aria-pressed="fillViewMode === 'original'"
                        @click="fillViewMode = 'original'"
                      >
                        <Icon icon="ep:grid" />
                        <span>原表模式</span>
                      </button>
                    </div>
                  </div>

                  <div class="edhr-fill-workspace__change-summary">
                    <span>待保存变更</span>
                    <strong>{{ pendingFieldChanges.length + pendingAttachmentChanges.length }}</strong>
                  </div>

                  <el-alert
                    v-if="revisionLockNotice"
                    :title="revisionLockNotice"
                    :type="revisionLockNoticeType"
                    :closable="false"
                    show-icon
                  />
                  <el-alert
                    v-if="fieldAuditOpenGateError && hasPendingFieldAuditChanges"
                    :title="fieldAuditOpenGateError"
                    type="warning"
                    :closable="false"
                    show-icon
                  />
                  <el-alert
                    v-if="fieldAuditSaveError"
                    :title="fieldAuditSaveError"
                    type="error"
                    :closable="false"
                    show-icon
                  />
                </div>

                <div class="edhr-fill-workspace__rail-actions">
                  <el-button
                    v-if="!isReadonly"
                    class="edhr-fill-workspace__primary-action"
                    type="primary"
                    :loading="fieldAuditSaveLoading"
                    :disabled="!canSaveFieldAuditChanges"
                    @click="handleSaveFieldAuditChanges"
                  >
                    保存草稿
                  </el-button>
                  <el-button
                    v-if="!isReadonly"
                    v-hasPermi="['mes:pro-batch-record-execution:update', 'mes:pro-batch-record-execution:golden-finger']"
                    class="edhr-fill-workspace__submit-action"
                    type="success"
                    :loading="submitLoading"
                    :disabled="hasSlotContextBlockers || hasPendingFieldChanges"
                    @click="openSubmitDialog"
                  >
                    提交执行
                  </el-button>
                  <el-button
                    class="edhr-fill-workspace__fullscreen-action"
                    @click="toggleFillWorkspaceFullscreen"
                  >
                    <Icon :icon="isFillWorkspaceFullscreen ? 'ep:close' : 'ep:full-screen'" />
                    <span>{{ isFillWorkspaceFullscreen ? '退出全屏' : '最大化' }}</span>
                  </el-button>
                </div>
              </aside>

              <main
                class="edhr-fill-workspace__canvas"
                :class="`is-fit-${fitMode}`"
              >
                <el-alert
                  v-if="formRenderError"
                  :title="formRenderError"
                  type="error"
                  :closable="false"
                  show-icon
                  class="edhr-fill-workspace__form-error"
                />
                <template v-else>
                  <el-alert
                    v-if="cellLinkPrefillNotice"
                    :title="cellLinkPrefillNotice"
                    type="success"
                    :closable="false"
                    show-icon
                    class="edhr-fill-workspace__prefill-alert"
                  />
                  <el-alert
                    v-if="cellLinkPrefillConflictNotice"
                    :title="cellLinkPrefillConflictNotice"
                    type="warning"
                    :closable="false"
                    show-icon
                    class="edhr-fill-workspace__prefill-alert"
                  />
                  <el-alert
                    v-if="signatureError"
                    :title="signatureError"
                    type="error"
                    :closable="false"
                    show-icon
                    class="edhr-fill-workspace__prefill-alert"
                  />
                  <section v-if="fillViewMode === 'assist'" class="edhr-fill-workspace__assist-panel">
                    <div class="edhr-fill-workspace__assist-topbar">
                      <div class="edhr-fill-workspace__assist-heading">
                        <div class="edhr-fill-workspace__assist-title">我的填写项</div>
                      </div>
                      <div class="edhr-fill-workspace__assist-switch-grid">
                        <button
                          type="button"
                          class="edhr-fill-workspace__assist-switch"
                          @click="handleAssistTaskSwitch"
                        >
                          <span>任务 / 批次</span>
                          <strong>{{ assistTaskSwitchLabel }}</strong>
                          <em>切换</em>
                        </button>
                        <button
                          type="button"
                          class="edhr-fill-workspace__assist-switch"
                          @click="handleAssistProcessSwitch"
                        >
                          <span>工序</span>
                          <strong>{{ assistProcessSwitchLabel }}</strong>
                          <em>切换</em>
                        </button>
                        <button
                          type="button"
                          class="edhr-fill-workspace__assist-switch"
                          @click="handleAssistFillerSwitch"
                        >
                          <span>填写人</span>
                          <strong>{{ assistFillerSwitchLabel }}</strong>
                          <em>切换</em>
                        </button>
                      </div>
                      <button
                        type="button"
                        class="edhr-fill-workspace__assist-missing-jump"
                        :class="{ 'is-done': assistMissingFieldCount === 0 }"
                        @click="scrollToFirstIncompleteAssistField"
                      >
                        <span>还差 {{ assistMissingFieldCount }} 项</span>
                      </button>
                    </div>

                    <el-dialog
                      v-model="assistSwitchDialogVisible"
                      :title="assistSwitchDialogTitle"
                      width="680px"
                      :append-to-body="false"
                      align-center
                      destroy-on-close
                      class="edhr-fill-workspace__assist-switch-dialog"
                    >
                      <div
                        v-if="assistSwitchDialogType === 'task'"
                        class="edhr-fill-workspace__assist-switch-menu"
                        data-assist-switch-menu="task"
                      >
                        <div class="edhr-fill-workspace__assist-switch-menu-head">
                          <strong>选择任务 / 批次</strong>
                          <span>我的待填写 / 返工任务</span>
                        </div>
                        <el-alert
                          v-if="assistTaskSwitchError"
                          :title="assistTaskSwitchError"
                          type="error"
                          :closable="false"
                          show-icon
                        />
                        <div v-else-if="assistTaskSwitchLoading" class="edhr-fill-workspace__assist-switch-loading">
                          正在加载任务...
                        </div>
                        <el-empty
                          v-else-if="assistTaskSwitchItems.length === 0"
                          description="暂无可切换任务"
                          :image-size="64"
                        />
                        <template v-else>
                          <button
                            v-for="item in assistTaskSwitchItems"
                            :key="item.id"
                            type="button"
                            class="edhr-fill-workspace__assist-switch-option"
                            :class="{ 'is-active': isAssistWorkTaskActive(item) }"
                            @click="handleSelectAssistTaskSwitchItem(item)"
                          >
                            <span class="edhr-fill-workspace__assist-switch-option-main">
                              {{ resolveAssistWorkTaskPrimaryLabel(item) }}
                            </span>
                            <span class="edhr-fill-workspace__assist-switch-option-sub">
                              {{ resolveAssistWorkTaskSecondaryLabel(item) }}
                            </span>
                          </button>
                        </template>
                      </div>

                      <div
                        v-else-if="assistSwitchDialogType === 'process'"
                        class="edhr-fill-workspace__assist-switch-menu"
                        data-assist-switch-menu="process"
                      >
                        <div class="edhr-fill-workspace__assist-switch-menu-head">
                          <strong>选择当前批次工序</strong>
                          <span>仅显示当前批次可打开任务</span>
                        </div>
                        <el-alert
                          v-if="assistProcessSwitchError"
                          :title="assistProcessSwitchError"
                          type="error"
                          :closable="false"
                          show-icon
                        />
                        <div
                          v-else-if="assistProcessSwitchLoading"
                          class="edhr-fill-workspace__assist-switch-loading"
                        >
                          正在加载工序...
                        </div>
                        <el-empty
                          v-else-if="assistProcessSwitchItems.length === 0"
                          description="当前批次暂无可切换工序"
                          :image-size="64"
                        />
                        <template v-else>
                          <button
                            v-for="item in assistProcessSwitchItems"
                            :key="item.id"
                            type="button"
                            class="edhr-fill-workspace__assist-switch-option"
                            :class="{ 'is-active': isAssistBatchTaskActive(item) }"
                            @click="handleSelectAssistProcessSwitchItem(item)"
                          >
                            <span class="edhr-fill-workspace__assist-switch-option-main">
                              {{ resolveAssistBatchTaskPrimaryLabel(item) }}
                            </span>
                            <span class="edhr-fill-workspace__assist-switch-option-sub">
                              {{ resolveAssistBatchTaskSecondaryLabel(item) }}
                            </span>
                          </button>
                        </template>
                      </div>

                      <div
                        v-else
                        class="edhr-fill-workspace__assist-switch-menu"
                        data-assist-switch-menu="filler"
                      >
                        <div class="edhr-fill-workspace__assist-switch-menu-head">
                          <strong>选择当前工序填写人</strong>
                        </div>
                        <el-alert
                          v-if="assistFillerSwitchError"
                          :title="assistFillerSwitchError"
                          type="error"
                          :closable="false"
                          show-icon
                        />
                        <div
                          v-else-if="assistFillerSwitchLoading"
                          class="edhr-fill-workspace__assist-switch-loading"
                        >
                          正在加载填写任务...
                        </div>
                        <el-empty
                          v-else-if="assistFillerSwitchItems.length === 0"
                          description="当前工序暂无已解析填写人"
                          :image-size="64"
                        />
                        <template v-else>
                          <button
                            v-for="item in assistFillerSwitchItems"
                            :key="item.key"
                            type="button"
                            class="edhr-fill-workspace__assist-switch-option"
                            :class="{ 'is-active': isAssistFillerSwitchItemActive(item) }"
                            :disabled="!isAssistFillerSwitchItemSelectable(item)"
                            :data-assist-filler-task-id="item.task.id"
                            :data-assist-filler-user-id="item.userId"
                            @click="handleSelectAssistFillerSwitchItem(item)"
                          >
                            <span class="edhr-fill-workspace__assist-switch-option-main">
                              {{ item.displayName }}
                            </span>
                            <span class="edhr-fill-workspace__assist-switch-option-sub">
                              {{ resolveAssistFillerSwitchItemSecondaryLabel(item) }}
                            </span>
                          </button>
                        </template>
                      </div>

                      <template #footer>
                        <el-button @click="closeAssistSwitchDialog">取消</el-button>
                      </template>
                    </el-dialog>

                    <div
                      v-if="assistIncompleteItems.length > 0"
                      class="edhr-fill-workspace__assist-summary"
                    >
                      <span class="edhr-fill-workspace__assist-summary-title">未完成摘要</span>
                      <span
                        v-for="item in assistIncompleteItems.slice(0, 4)"
                        :key="item.fieldIdentity"
                        class="edhr-fill-workspace__assist-summary-item"
                      >
                        {{ item.label }}：{{ item.reason }}
                      </span>
                    </div>
                    <div v-else class="edhr-fill-workspace__assist-summary is-complete">
                      必填、附件和签名已完成，可以提交执行。
                    </div>

                    <el-empty
                      v-if="assistFillFields.length === 0"
                      description="当前没有可填写字段"
                    />

                    <div v-else class="edhr-fill-workspace__assist-list">
                      <article
                        v-for="field in assistFillFields"
                        :key="field.fieldIdentity"
                        class="edhr-fill-workspace__assist-row"
                        :class="{
                          'is-missing': isAssistFieldIncomplete(field),
                          'is-error': Boolean(resolveAssistFieldValidationMessage(field)),
                          'is-complete': isAssistFieldComplete(field),
                          'is-highlighted': highlightedAssistFieldIdentity === field.fieldIdentity
                        }"
                        :data-assist-field-id="field.fieldIdentity"
                      >
                        <div class="edhr-fill-workspace__assist-row-meta">
                          <div class="edhr-fill-workspace__assist-label">
                            <span>{{ field.label }}</span>
                            <el-tag
                              v-if="isFieldRequiredForCurrentMode(field) || field.componentKind === 'signature'"
                              size="small"
                              type="danger"
                              effect="plain"
                            >
                              {{ field.componentKind === 'signature' ? '签名' : '必填' }}
                            </el-tag>
                            <el-tag v-else size="small" type="info" effect="plain">可选</el-tag>
                            <el-tag
                              size="small"
                              :type="resolveAssistFieldStatusTagType(field)"
                              effect="plain"
                            >
                              {{ resolveAssistFieldStatusLabel(field) }}
                            </el-tag>
                          </div>
                          <div class="edhr-fill-workspace__assist-help">
                            {{ field.helpText || field.placeholder || '字段说明未配置' }}
                          </div>
                          <div class="edhr-fill-workspace__assist-source">
                            <span>位置：第 {{ field.rowIndex + 1 }} 行 / 第 {{ field.columnIndex + 1 }} 列</span>
                            <span v-if="field.unit">单位：{{ field.unit }}</span>
                          </div>
                        </div>

                        <div class="edhr-fill-workspace__assist-control">
                          <div
                            v-if="field.componentKind === 'choice-group'"
                            class="edhr-fill-workspace__choice-group"
                          >
                            <el-radio-group
                              :model-value="resolveAssistChoiceGroupValue(field)"
                              :disabled="isReadonly"
                              @change="(value) => updateAssistChoiceGroupValue(field, value)"
                            >
                              <el-radio
                                v-for="option in field.options"
                                :key="option.value"
                                :value="option.value"
                              >
                                {{ option.label }}
                              </el-radio>
                            </el-radio-group>
                            <el-button
                              v-if="!isReadonly && resolveAssistChoiceGroupValue(field)"
                              size="small"
                              text
                              type="primary"
                              @click="updateAssistChoiceGroupValue(field, '')"
                            >
                              清空
                            </el-button>
                          </div>
                          <el-select
                            v-else-if="field.componentKind === 'select'"
                            v-model="draftFieldValues[field.fieldIdentity]"
                            class="!w-1/1"
                            :disabled="isReadonly"
                            clearable
                            :placeholder="field.placeholder"
                          >
                            <el-option
                              v-for="option in field.options"
                              :key="String(option.value)"
                              :label="option.label"
                              :value="option.value"
                            />
                          </el-select>
                          <el-checkbox
                            v-else-if="field.componentKind === 'checkbox'"
                            v-model="draftFieldValues[field.fieldIdentity]"
                            :disabled="isReadonly"
                          >
                            勾选
                          </el-checkbox>
                          <div
                            v-else-if="field.componentKind === 'signature'"
                            class="edhr-fill-workspace__assist-signature"
                          >
                            <span>{{ resolveSignatureCellDisplay(field) }}</span>
                            <el-button
                              size="small"
                              type="primary"
                              plain
                              :disabled="
                                isReadonly || Boolean(resolveSignatureCellActionDisabledReason(field))
                              "
                              :title="resolveSignatureCellActionDisabledReason(field) || resolveSignatureCellActionLabel(field)"
                              @click="handleSignatureCellAction(field)"
                            >
                              {{ resolveSignatureCellActionLabel(field) }}
                            </el-button>
                          </div>
                          <div v-else-if="field.componentKind === 'number'" class="edhr-fill-workspace__assist-typed-input">
                            <el-input-number
                              v-model="draftFieldValues[field.fieldIdentity]"
                              class="!w-1/1"
                              :disabled="isReadonly"
                              :controls="false"
                              :placeholder="field.placeholder"
                            />
                            <span v-if="field.unit" class="edhr-page-shell__unit">{{ field.unit }}</span>
                          </div>
                          <el-date-picker
                            v-else-if="field.componentKind === 'date'"
                            v-model="draftFieldValues[field.fieldIdentity]"
                            type="date"
                            value-format="YYYY-MM-DD"
                            class="!w-1/1"
                            :disabled="isReadonly"
                            :placeholder="field.placeholder"
                          />
                          <el-date-picker
                            v-else-if="field.componentKind === 'datetime'"
                            v-model="draftFieldValues[field.fieldIdentity]"
                            type="datetime"
                            value-format="YYYY-MM-DD HH:mm:ss"
                            class="!w-1/1"
                            :disabled="isReadonly"
                            :placeholder="field.placeholder"
                          />
                          <el-input
                            v-else-if="field.componentKind === 'textarea'"
                            v-model="draftFieldValues[field.fieldIdentity]"
                            type="textarea"
                            :rows="2"
                            :disabled="isReadonly"
                            :placeholder="field.placeholder"
                          />
                          <UploadFile
                            v-else-if="field.componentKind === 'upload-file'"
                            v-model="draftAttachmentValues[field.fieldIdentity]"
                            :disabled="isReadonly"
                            :http-request="createEdhrAttachmentUploadRequest(field)"
                            :limit="1"
                            :is-show-tip="false"
                          />
                          <UploadImg
                            v-else-if="field.componentKind === 'upload-image'"
                            v-model="draftImageAttachmentValues[field.fieldIdentity]"
                            :disabled="isReadonly"
                            :http-request="createEdhrAttachmentUploadRequest(field)"
                            :show-btn-text="false"
                          />
                          <UploadImgs
                            v-else-if="field.componentKind === 'upload-images'"
                            v-model="draftAttachmentValues[field.fieldIdentity]"
                            :disabled="isReadonly"
                            :http-request="createEdhrAttachmentUploadRequest(field)"
                            :limit="parsePositiveNumber(field.constraints.maxCount) || 9"
                          />
                          <el-input
                            v-else
                            v-model="draftFieldValues[field.fieldIdentity]"
                            :disabled="isReadonly"
                            :placeholder="field.placeholder"
                          />
                          <div
                            v-if="resolveAssistFieldValidationMessage(field)"
                            class="edhr-fill-workspace__assist-validation"
                          >
                            {{ resolveAssistFieldValidationMessage(field) }}
                          </div>
                        </div>
                      </article>
                    </div>
                  </section>

                  <EdhrExecutionTemplateEditableForm
                    v-else
                    :sheet-layout-json="execution.sheetLayoutJson"
                    :cell-rules="templateCellRules"
                    :signature-markers="templateSignatureMarkers"
                    :model-value="templateModelValue"
                    :fit-mode="fitMode"
                    class="edhr-fill-workspace__form"
                  >
                    <template #field="{ context }">
                      <div
                        class="edhr-fill-workspace__field"
                        :class="{ 'is-out-of-scope': isTemplateContextOutOfCurrentFillScope(context) }"
                      >
                        <div class="edhr-fill-workspace__field-head">
                          <span>{{ resolveTemplateSnapshotField(context)?.label || context.label }}</span>
                          <span
                            v-if="
                              resolveTemplateSnapshotField(context) &&
                              isFieldRequiredForCurrentMode(requireTemplateSnapshotField(context))
                            "
                            class="edhr-fill-workspace__required"
                          >
                            必填
                          </span>
                          <el-tag
                            v-if="isTemplateContextOutOfCurrentFillScope(context)"
                            type="info"
                            effect="plain"
                            size="small"
                          >
                            范围外只读
                          </el-tag>
                          <el-tag
                            v-if="resolveCellLinkPrefill(context)"
                            type="success"
                            effect="plain"
                            size="small"
                            class="edhr-fill-workspace__cell-link-tag"
                            :title="formatCellLinkPrefillSource(resolveCellLinkPrefill(context))"
                          >
                            跨表单带入
                          </el-tag>
                        </div>

                      <el-select
                        v-if="resolveTemplateSnapshotField(context)?.componentKind === 'select'"
                        :model-value="resolveTemplateFieldValue(context)"
                        :disabled="isTemplateContextReadonlyForCurrentTask(context)"
                        clearable
                        size="small"
                        class="!w-1/1"
                        :placeholder="resolveTemplateSnapshotField(context)?.placeholder"
                        @update:model-value="(value) => updateTemplateFieldValue(context, value)"
                      >
                        <el-option
                          v-for="option in resolveTemplateSnapshotField(context)?.options || []"
                          :key="String(option.value)"
                          :label="option.label"
                          :value="option.value"
                        />
                      </el-select>
                      <el-checkbox
                        v-else-if="resolveTemplateSnapshotField(context)?.componentKind === 'checkbox'"
                        :model-value="Boolean(resolveTemplateFieldValue(context))"
                        :disabled="isTemplateContextReadonlyForCurrentTask(context)"
                        @update:model-value="(value) => updateTemplateFieldValue(context, value)"
                      >
                        勾选
                      </el-checkbox>
                      <div
                        v-else-if="resolveTemplateSnapshotField(context)?.componentKind === 'signature'"
                        class="edhr-fill-workspace__signature"
                      >
                        <span>{{ resolveSignatureCellDisplay(requireTemplateSnapshotField(context)) }}</span>
                        <el-button
                          size="small"
                          type="primary"
                          plain
                          :disabled="
                            isTemplateContextReadonlyForCurrentTask(context) ||
                            Boolean(
                              resolveSignatureCellActionDisabledReason(
                                requireTemplateSnapshotField(context)
                              )
                            )
                          "
                          @click="handleSignatureCellAction(requireTemplateSnapshotField(context))"
                        >
                          {{ resolveSignatureCellActionLabel(requireTemplateSnapshotField(context)) }}
                        </el-button>
                      </div>
                      <el-input-number
                        v-else-if="resolveTemplateSnapshotField(context)?.componentKind === 'number'"
                        :model-value="resolveTemplateNumberValue(context)"
                        :disabled="isTemplateContextReadonlyForCurrentTask(context)"
                        :controls="false"
                        size="small"
                        class="!w-1/1"
                        :placeholder="resolveTemplateSnapshotField(context)?.placeholder"
                        @update:model-value="
                          (value) => updateTemplateFieldValue(context, value == null ? null : value)
                        "
                      />
                      <el-date-picker
                        v-else-if="resolveTemplateSnapshotField(context)?.componentKind === 'date'"
                        :model-value="resolveTemplateFieldValue(context)"
                        :disabled="isTemplateContextReadonlyForCurrentTask(context)"
                        type="date"
                        value-format="YYYY-MM-DD"
                        size="small"
                        class="!w-1/1"
                        :placeholder="resolveTemplateSnapshotField(context)?.placeholder"
                        @update:model-value="(value) => updateTemplateFieldValue(context, value || '')"
                      />
                      <el-date-picker
                        v-else-if="resolveTemplateSnapshotField(context)?.componentKind === 'datetime'"
                        :model-value="resolveTemplateFieldValue(context)"
                        :disabled="isTemplateContextReadonlyForCurrentTask(context)"
                        type="datetime"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        size="small"
                        class="!w-1/1"
                        :placeholder="resolveTemplateSnapshotField(context)?.placeholder"
                        @update:model-value="(value) => updateTemplateFieldValue(context, value || '')"
                      />
                      <el-input
                        v-else-if="resolveTemplateSnapshotField(context)?.componentKind === 'textarea'"
                        :model-value="String(resolveTemplateFieldValue(context) ?? '')"
                        :disabled="isTemplateContextReadonlyForCurrentTask(context)"
                        type="textarea"
                        :rows="2"
                        size="small"
                        :placeholder="resolveTemplateSnapshotField(context)?.placeholder"
                        @update:model-value="(value) => updateTemplateFieldValue(context, value)"
                      />
                      <UploadFile
                        v-else-if="resolveTemplateSnapshotField(context)?.componentKind === 'upload-file'"
                        :model-value="resolveTemplateAttachmentValue(context)"
                        :disabled="isTemplateContextReadonlyForCurrentTask(context)"
                        :http-request="
                          createEdhrAttachmentUploadRequest(requireTemplateSnapshotField(context))
                        "
                        :limit="1"
                        :is-show-tip="false"
                        @update:model-value="
                          (value) => updateTemplateAttachmentValue(context, value)
                        "
                      />
                      <UploadImg
                        v-else-if="resolveTemplateSnapshotField(context)?.componentKind === 'upload-image'"
                        :model-value="resolveTemplateImageAttachmentValue(context)"
                        :disabled="isTemplateContextReadonlyForCurrentTask(context)"
                        :http-request="
                          createEdhrAttachmentUploadRequest(requireTemplateSnapshotField(context))
                        "
                        :show-btn-text="false"
                        @update:model-value="
                          (value) => updateTemplateImageAttachmentValue(context, value)
                        "
                      />
                      <UploadImgs
                        v-else-if="resolveTemplateSnapshotField(context)?.componentKind === 'upload-images'"
                        :model-value="resolveTemplateAttachmentValue(context)"
                        :disabled="isTemplateContextReadonlyForCurrentTask(context)"
                        :http-request="
                          createEdhrAttachmentUploadRequest(requireTemplateSnapshotField(context))
                        "
                        :limit="
                          parsePositiveNumber(
                            resolveTemplateSnapshotField(context)?.constraints.maxCount
                          ) || 9
                        "
                        @update:model-value="
                          (value) => updateTemplateAttachmentValue(context, value)
                        "
                      />
                      <el-input
                        v-else
                        :model-value="String(resolveTemplateFieldValue(context) ?? '')"
                        :disabled="isTemplateContextReadonlyForCurrentTask(context)"
                        size="small"
                        :placeholder="resolveTemplateSnapshotField(context)?.placeholder"
                        @update:model-value="(value) => updateTemplateFieldValue(context, value)"
                      />
                    </div>
                    </template>
                  </EdhrExecutionTemplateEditableForm>
                </template>
              </main>
            </div>

            <template v-if="false">
            <el-alert
            v-if="revisionLockNotice"
            :title="revisionLockNotice"
            :type="revisionLockNoticeType"
            :closable="false"
            show-icon
            class="edhr-page-shell__alert"
          />

          <el-alert
            v-if="formRenderError"
            :title="formRenderError"
            type="error"
            :closable="false"
            show-icon
          />

          <el-form
            v-else
            class="edhr-page-shell__legacy-form"
            label-width="120px"
            :disabled="isReadonly || fieldAuditSaveLoading || formReviewSignLoading || submitLoading"
          >
            <el-row :gutter="16">
              <el-col
                v-for="field in snapshotFields"
                :key="field.fieldIdentity"
                :span="
                  field.componentKind === 'textarea' || isAttachmentComponentKind(field.componentKind)
                    ? 24
                    : 12
                "
              >
                <el-form-item :label="field.label" :required="isFieldRequiredForCurrentMode(field)">
                  <el-select
                    v-if="field.componentKind === 'select'"
                    v-model="draftFieldValues[field.fieldIdentity]"
                    class="!w-1/1"
                    :placeholder="field.placeholder"
                    clearable
                  >
                    <el-option
                      v-for="option in field.options"
                      :key="String(option.value)"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                  <el-checkbox
                    v-else-if="field.componentKind === 'checkbox'"
                    v-model="draftFieldValues[field.fieldIdentity]"
                  />
                  <div
                    v-else-if="field.componentKind === 'signature'"
                    class="edhr-page-shell__signature-cell"
                  >
                    <div class="edhr-page-shell__signature-cell-status">
                      <span class="edhr-page-shell__signature-cell-text">
                        {{ resolveSignatureCellDisplay(field) }}
                      </span>
                      <span class="edhr-page-shell__signature-cell-hint">
                        签名格必须通过电子签名完成，不支持手动输入。
                      </span>
                    </div>
                    <el-button
                      size="small"
                      type="primary"
                      plain
                      :disabled="Boolean(resolveSignatureCellActionDisabledReason(field))"
                      :title="resolveSignatureCellActionDisabledReason(field) || resolveSignatureCellActionLabel(field)"
                      @click="handleSignatureCellAction(field)"
                    >
                      {{ resolveSignatureCellActionLabel(field) }}
                    </el-button>
                    <div
                      v-if="resolveSignatureCellActionDisabledReason(field)"
                      class="edhr-page-shell__signature-cell-reason"
                    >
                      {{ resolveSignatureCellActionDisabledReason(field) }}
                    </div>
                  </div>
                  <div v-else-if="field.componentKind === 'number'" class="edhr-page-shell__typed-input">
                    <el-input-number
                      v-model="draftFieldValues[field.fieldIdentity]"
                      class="!w-1/1"
                      :controls="false"
                    />
                    <span v-if="field.unit" class="edhr-page-shell__unit">{{ field.unit }}</span>
                  </div>
                  <el-date-picker
                    v-else-if="field.componentKind === 'date'"
                    v-model="draftFieldValues[field.fieldIdentity]"
                    type="date"
                    value-format="YYYY-MM-DD"
                    class="!w-1/1"
                    :placeholder="field.placeholder"
                  />
                  <el-date-picker
                    v-else-if="field.componentKind === 'datetime'"
                    v-model="draftFieldValues[field.fieldIdentity]"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    class="!w-1/1"
                    :placeholder="field.placeholder"
                  />
                  <el-input
                    v-else-if="field.componentKind === 'textarea'"
                    v-model="draftFieldValues[field.fieldIdentity]"
                    type="textarea"
                    :rows="3"
                    :placeholder="field.placeholder"
                  />
                  <div
                    v-else-if="field.componentKind === 'upload-file'"
                    class="edhr-page-shell__attachment-field"
                  >
                    <UploadFile
                      v-model="draftAttachmentValues[field.fieldIdentity]"
                      :disabled="isReadonly"
                      :http-request="createEdhrAttachmentUploadRequest(field)"
                      :limit="1"
                      :is-show-tip="false"
                    />
                    <div class="edhr-page-shell__attachment-hint">
                      eDHR 受控附件：上传结果需进入附件绑定、签名与审计链保存，不作为普通字段值提交。
                      <span v-if="field.attachmentRule" class="edhr-page-shell__attachment-rule">
                        {{ formatAttachmentRule(field.attachmentRule) }}
                      </span>
                    </div>
                  </div>
                  <div
                    v-else-if="field.componentKind === 'upload-image'"
                    class="edhr-page-shell__attachment-field"
                  >
                    <UploadImg
                      v-model="draftImageAttachmentValues[field.fieldIdentity]"
                      :disabled="isReadonly"
                      :http-request="createEdhrAttachmentUploadRequest(field)"
                      :show-btn-text="false"
                    />
                    <div class="edhr-page-shell__attachment-hint">
                      eDHR 受控附件：图片上传结果需进入附件绑定、签名与审计链保存。
                      <span v-if="field.attachmentRule" class="edhr-page-shell__attachment-rule">
                        {{ formatAttachmentRule(field.attachmentRule) }}
                      </span>
                    </div>
                  </div>
                  <div
                    v-else-if="field.componentKind === 'upload-images'"
                    class="edhr-page-shell__attachment-field"
                  >
                    <UploadImgs
                      v-model="draftAttachmentValues[field.fieldIdentity]"
                      :disabled="isReadonly"
                      :http-request="createEdhrAttachmentUploadRequest(field)"
                      :limit="parsePositiveNumber(field.constraints.maxCount) || 9"
                    />
                    <div class="edhr-page-shell__attachment-hint">
                      eDHR 受控附件：多图片上传结果需进入附件绑定、签名与审计链保存。
                      <span v-if="field.attachmentRule" class="edhr-page-shell__attachment-rule">
                        {{ formatAttachmentRule(field.attachmentRule) }}
                      </span>
                    </div>
                  </div>
                  <el-input
                    v-else
                    v-model="draftFieldValues[field.fieldIdentity]"
                    :placeholder="field.placeholder"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="备注">
              <el-input
                v-model="draftRemark"
                type="textarea"
                :rows="3"
                disabled
                placeholder="请输入执行备注"
              />
            </el-form-item>
          </el-form>

          <div class="edhr-page-shell__field-audit">
            <div class="edhr-page-shell__section-head">
              <div>
                <div class="edhr-page-shell__section-title">待保存变更</div>
                <div class="edhr-page-shell__section-subtitle">
                  保存前核对字段变更、原因和审计基准
                </div>
              </div>
              <div v-if="hasPendingFieldAuditChanges" class="edhr-page-shell__section-actions">
                <el-button
                  v-hasPermi="['mes:pro-batch-record-execution:field-audit-update', 'mes:pro-batch-record-execution:update', 'mes:pro-batch-record-execution:golden-finger']"
                  type="primary"
                  :loading="fieldAuditSaveLoading"
                  :disabled="!canSaveFieldAuditChanges"
                  @click="handleSaveFieldAuditChanges"
                >
                  保存变更
                </el-button>
              </div>
            </div>

            <el-alert
              v-if="fieldAuditOpenGateError && hasPendingFieldAuditChanges"
              :title="fieldAuditOpenGateError"
              type="warning"
              :closable="false"
              show-icon
              class="edhr-page-shell__archive-alert"
            />
            <el-alert
              v-if="fieldAuditSaveError"
              :title="fieldAuditSaveError"
              type="error"
              :closable="false"
              show-icon
              class="edhr-page-shell__archive-alert"
            />
            <el-alert
              v-if="fieldAuditLastResult"
              :title="fieldAuditLastSuccessTitle"
              type="success"
              :closable="false"
              show-icon
              class="edhr-page-shell__archive-alert"
            />

            <template v-if="hasPendingFieldAuditChanges">
              <el-form
                :inline="true"
                :model="fieldAuditReasonForm"
                class="edhr-page-shell__field-audit-reason"
              >
                <el-form-item label="原因分类" required>
                  <el-select v-model="fieldAuditReasonForm.reasonCategory" clearable class="!w-220px">
                    <el-option
                      v-for="option in EDHR_FIELD_CHANGE_REASON_OPTIONS"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="原因说明" required>
                  <el-input
                    v-model="fieldAuditReasonForm.reasonText"
                    clearable
                    class="!w-360px"
                    placeholder="请输入字段变更原因"
                  />
                </el-form-item>
              </el-form>

              <div v-if="hasPendingAttachmentChanges" class="edhr-page-shell__pending-attachments">
                <div class="edhr-page-shell__pending-attachments-title">待保存附件</div>
                <div class="edhr-page-shell__pending-attachments-list">
                  <div
                    v-for="change in pendingAttachmentChanges"
                    :key="change.fieldIdentity"
                    class="edhr-page-shell__pending-attachment-item"
                  >
                    <div class="edhr-page-shell__field-name">{{ change.fieldLabel }}</div>
                    <div class="edhr-page-shell__field-path">{{ change.fileName || change.fileUrl }}</div>
                    <el-tag :type="change.validationMessage ? 'danger' : 'success'">
                      {{ change.validationMessage || '可保存' }}
                    </el-tag>
                  </div>
                </div>
              </div>

              <el-table
                v-if="pendingFieldChanges.length > 0"
                :data="pendingFieldChanges"
                stripe
                :show-overflow-tooltip="true"
                class="edhr-page-shell__field-audit-table"
              >
                <el-table-column type="expand" width="44">
                  <template #default="{ row }">
                    <div class="edhr-page-shell__evidence">
                      <div class="edhr-page-shell__evidence-title">字段证据</div>
                      <div class="edhr-page-shell__evidence-grid">
                        <div class="edhr-page-shell__evidence-item">
                          <div class="edhr-page-shell__evidence-label">字段路径</div>
                          <div class="edhr-page-shell__evidence-value">{{ row.fieldPath || '--' }}</div>
                        </div>
                        <div class="edhr-page-shell__evidence-item">
                          <div class="edhr-page-shell__evidence-label">字段标识</div>
                          <div class="edhr-page-shell__evidence-value">{{ row.fieldKey || '--' }}</div>
                        </div>
                        <div class="edhr-page-shell__evidence-item">
                          <div class="edhr-page-shell__evidence-label">模板位置</div>
                          <div class="edhr-page-shell__evidence-value">{{ formatFieldPosition(row) }}</div>
                        </div>
                        <div class="edhr-page-shell__evidence-item">
                          <div class="edhr-page-shell__evidence-label">旧值 JSON</div>
                          <div class="edhr-page-shell__evidence-value">{{ resolveJsonPreview(row.oldValueJson) }}</div>
                        </div>
                        <div class="edhr-page-shell__evidence-item">
                          <div class="edhr-page-shell__evidence-label">旧值哈希</div>
                          <div class="edhr-page-shell__evidence-value">{{ row.expectedOldValueHash || '--' }}</div>
                        </div>
                        <div class="edhr-page-shell__evidence-item">
                          <div class="edhr-page-shell__evidence-label">新值 JSON</div>
                          <div class="edhr-page-shell__evidence-value">{{ resolveJsonPreview(row.newValueJson) }}</div>
                        </div>
                        <div class="edhr-page-shell__evidence-item">
                          <div class="edhr-page-shell__evidence-label">新值哈希</div>
                          <div class="edhr-page-shell__evidence-value">保存后由服务端计算</div>
                        </div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="字段" min-width="240">
                  <template #default="{ row }">
                    <div class="edhr-page-shell__field-name">{{ row.fieldLabel }}</div>
                    <div class="edhr-page-shell__field-path">{{ formatFieldPosition(row) }}</div>
                  </template>
                </el-table-column>
                <el-table-column label="变更值" min-width="280">
                  <template #default="{ row }">
                    <div class="edhr-page-shell__change-value">{{ row.oldValueDisplay }}</div>
                    <div class="edhr-page-shell__change-arrow">→</div>
                    <div class="edhr-page-shell__change-value">{{ row.newValueDisplay }}</div>
                  </template>
                </el-table-column>
                <el-table-column label="原因" min-width="220">
                  <template #default>
                    <div>{{ fieldAuditReasonForm.reasonCategory || '--' }}</div>
                    <div class="edhr-page-shell__field-path">{{ fieldAuditReasonForm.reasonText || '缺少变更原因' }}</div>
                  </template>
                </el-table-column>
                <el-table-column label="校验" width="140">
                  <template #default="{ row }">
                    <el-tag :type="row.validationMessage ? 'danger' : 'success'">
                      {{ row.validationMessage || '可保存' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="handleRevertPendingFieldChange(row)">
                      回滚
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <div v-else class="edhr-page-shell__field-audit-empty">
              <el-empty>
                <template #description>
                  <div class="edhr-page-shell__field-audit-empty-title">暂无待保存变更</div>
                  <div class="edhr-page-shell__field-audit-empty-desc">
                    修改字段或附件后，会在这里核对变更内容、原因和审计基准。
                  </div>
                </template>
              </el-empty>
            </div>
          </div>
            </template>
          </template>

        </template>

        <el-alert
          v-else-if="loadError"
          :title="loadError"
          type="error"
          :closable="false"
          show-icon
          class="edhr-fill-workspace__load-error"
        />
        <el-empty v-else-if="!loading" description="暂无可展示的 eDHR 执行数据" />
      </div>
    </div>

    <el-dialog
      class="edhr-fill-workspace__submit-sign-dialog"
      v-model="submitDialogVisible"
      width="520px"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      align-center
      destroy-on-close
    >
      <div class="edhr-fill-workspace__submit-sign-form">
        <div class="edhr-fill-workspace__submit-sign-row">
          <div class="edhr-fill-workspace__submit-sign-label">姓名</div>
          <div class="edhr-fill-workspace__submit-sign-name">{{ submitSignatureUserName }}</div>
        </div>
        <div class="edhr-fill-workspace__submit-sign-row">
          <div class="edhr-fill-workspace__submit-sign-label is-required">电子签名</div>
          <el-input
            v-model="submitForm.password"
            type="password"
            show-password
            @keyup.enter="handleSubmitExecution"
          />
        </div>
      </div>
      <template #footer>
        <el-button
          class="edhr-fill-workspace__submit-sign-confirm"
          type="primary"
          :loading="submitLoading"
          @click="handleSubmitExecution"
        >
          确认
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      class="edhr-fill-workspace__result-dialog"
      v-model="fillActionResultDialogVisible"
      width="720px"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      align-center
      destroy-on-close
    >
      <div
        class="edhr-fill-workspace__result-body"
        :class="`is-${fillActionResultDialog.tone}`"
      >
        <div class="edhr-fill-workspace__result-context">
          <div>
            <span>订单</span>
            <strong>{{ fillActionResultDialog.orderText }}</strong>
          </div>
          <div>
            <span>工序</span>
            <strong>{{ fillActionResultDialog.processText }}</strong>
          </div>
        </div>
        <div class="edhr-fill-workspace__result-status">
          <strong>{{ fillActionResultDialog.fillerText }}</strong>
          <strong>{{ fillActionResultDialog.statusText }}</strong>
        </div>
      </div>
      <template #footer>
        <el-button
          class="edhr-fill-workspace__result-confirm"
          type="primary"
          @click="fillActionResultDialogVisible = false"
        >
          确认
        </el-button>
      </template>
    </el-dialog>

    <Dialog title="表单复核签名" v-model="formReviewSignDialogVisible" width="620px">
      <el-alert
        v-if="formReviewSignError"
        :title="formReviewSignError"
        type="error"
        :closable="false"
        show-icon
        class="edhr-page-shell__archive-alert"
      />
      <el-descriptions :column="1" border class="edhr-page-shell__signature-summary">
        <el-descriptions-item label="签名动作">FORM_REVIEW</el-descriptions-item>
        <el-descriptions-item label="签名含义">表单复核</el-descriptions-item>
        <el-descriptions-item label="执行编号">
          {{ execution?.executionCode || execution?.id || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          {{ executionStatusText }}
        </el-descriptions-item>
        <el-descriptions-item label="单元格值哈希">
          {{ execution?.cellValuesHash || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="字段审计版本">
          {{ execution?.fieldAuditRevision ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="字段审计头哈希">
          {{ execution?.fieldAuditHeadHash || '--' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px" class="edhr-page-shell__signature-form">
        <el-form-item label="签名密码" required>
          <el-input
            v-model="formReviewSignForm.password"
            type="password"
            show-password
            placeholder="请输入当前账号密码"
            @keyup.enter="handleFormReviewSign"
          />
        </el-form-item>
        <el-form-item label="复核备注">
          <el-input
            v-model="formReviewSignForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入复核备注（可选）"
          />
        </el-form-item>
        <el-divider content-position="left">签名显示时间</el-divider>
        <el-form-item label="签名时间">
          <el-date-picker
            v-model="formReviewSignatureTimeForm.selectedSignedAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="可选择人工签名时间"
            class="!w-1/1"
          />
        </el-form-item>
        <el-form-item label="签名时区">
          <el-input v-model="formReviewSignatureTimeForm.selectedTimeZone" placeholder="例如 Asia/Shanghai" />
        </el-form-item>
        <el-form-item label="时间原因">
          <el-input
            v-model="formReviewSignatureTimeForm.selectedTimeReason"
            type="textarea"
            :rows="2"
            placeholder="选择人工签名时间时必须说明原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeFormReviewSignDialog">取 消</el-button>
        <el-button type="primary" :loading="formReviewSignLoading" @click="handleFormReviewSign">
          确 认 签 名
        </el-button>
      </template>
    </Dialog>

    <Dialog title="生成归档打印件" v-model="archiveDialogVisible" width="520px">
      <el-form label-width="96px">
        <el-form-item label="执行编号">
          <el-input :model-value="execution?.executionCode || execution?.id || '--'" disabled />
        </el-form-item>
        <el-form-item label="归档格式">
          <el-input :model-value="archiveArtifactType" disabled />
        </el-form-item>
        <el-form-item label="封存密码" required>
          <el-input
            v-model="archiveForm.sealPassword"
            type="password"
            show-password
            placeholder="请输入电子签名密码"
            @keyup.enter="handleGenerateArchive"
          />
        </el-form-item>
        <el-form-item label="归档备注">
          <el-input
            v-model="archiveForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入归档说明（可选）"
          />
        </el-form-item>
        <el-form-item label="重新生成">
          <el-switch v-model="archiveForm.regenerate" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeArchiveGenerateDialog">取 消</el-button>
        <el-button type="primary" :loading="archiveGenerateLoading" @click="handleGenerateArchive">
          确 认 生 成
        </el-button>
      </template>
    </Dialog>

    </ContentWrap>
</template>

<script setup lang="ts">
import {
  EDHR_EXECUTION_ARCHIVE_ARTIFACT_PDF,
  EDHR_EXECUTION_ARCHIVE_STATUS_FAILED,
  EDHR_EXECUTION_ARCHIVE_STATUS_GENERATING,
  EDHR_EXECUTION_ARCHIVE_STATUS_SEALED,
  downloadEdhrExecutionArchive,
  generateEdhrExecutionArchive,
  getLatestEdhrExecutionArchive,
  isEdhrExecutionArchiveNotExistsMessage,
  type ProFeedbackEdhrExecutionArchiveRespVO
} from '@/api/mes/pro/edhr/archive'
import { EDHR_EXECUTION_STATUS } from '@/api/mes/pro/edhr/approval'
import {
  EDHR_FIELD_CHANGE_REASON_OPTIONS,
  EDHR_HASH_STATUS_LABEL_MAP,
  saveEdhrFieldChanges,
  type EdhrFieldChangeItemReqVO,
  type EdhrFieldAttachmentChangeReqVO,
  type EdhrFieldChangeReasonCategory,
  type EdhrFieldChangeSaveRespVO,
  type EdhrFieldTypedJsonValue,
  type EdhrFieldValueType
} from '@/api/mes/pro/edhr/fieldAudit'
import {
  prepareEdhrAttachmentUpload,
  type EdhrAttachmentPrepareUploadRespVO
} from '@/api/mes/pro/edhr/attachment'
import { getEdhrExecutionSignaturePage, type EdhrSignatureSummaryVO } from '@/api/mes/pro/edhr/signatures'
import { getEdhrTrackingTimeline, type EdhrTrackingEventVO } from '@/api/mes/pro/edhr/tracking'
import { getEdhrRecordbookGlobalSetting } from '@/api/mes/pro/edhr/recordbookGlobalSetting'
import {
  ProFeedbackApi,
  type ProFeedbackEdhrExecutionSnapshotVO,
  type ProFeedbackEdhrExecutionVO,
  type ProFeedbackEdhrReviewAssigneeOptionVO,
  type ProFeedbackEdhrReviewAssigneeSelectionVO,
  type ProFeedbackEdhrReviewCandidateUserVO,
  type ProFeedbackEdhrSnapshotFieldVO
} from '@/api/mes/pro/feedback'
import type {
  BatchRecordReportCellRuleVO,
  BatchRecordReportCellValueType,
  BatchRecordReportSignatureCellMarkerVO
} from '@/api/mes/pro/batchrecordreport'
import {
  BatchRecordCellLinkApi,
  type BatchRecordCellLinkPrefillItemVO
} from '@/api/mes/pro/batchrecordcelllink'
import {
  EDHR_BATCH_NODE_ROUTE_FORM,
  EDHR_BATCH_TASK_STATUS_APPROVED,
  EDHR_BATCH_TASK_STATUS_BLOCKED,
  EDHR_BATCH_TASK_STATUS_SKIPPED,
  getEdhrBatchExecution,
  openEdhrBatchTask,
  type EdhrBatchExecutionTaskRespVO
} from '@/api/mes/pro/edhr/batchExecution'
import {
  EDHR_WORK_TASK_STATUS_TODO,
  EDHR_WORK_TASK_TYPE_FILL,
  EDHR_WORK_TASK_TYPE_REWORK,
  getEdhrWorkTaskMyPage,
  type EdhrWorkTaskRespVO
} from '@/api/mes/pro/edhr/workTask'
import { hasPermission } from '@/directives/permission/hasPermi'
import { useUserStore } from '@/store/modules/user'
import { navigateToEdhrWorkTask } from '@/utils/edhrWorkTaskNavigation'
import { parsePositiveRouteQueryId, sameRouteQueryId } from '@/utils/routeQueryId'
import {
  edhrDateTimeFormatter,
  formatEdhrDateTime,
  toEdhrDateTime
} from '@/views/mes/pro/edhr/shared/dateTime'
import { UploadFile, UploadImg, UploadImgs } from '@/components/UploadFile'
import type { UploadRequestOptions } from 'element-plus/es/components/upload/src/upload'
import type {
  TemplateEditableCellContext,
  TemplateSimulationValueMap
} from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'
import EdhrExecutionTemplateEditableForm from './components/EdhrExecutionTemplateEditableForm.vue'
import EdhrExecutionReadonlyForm from './components/EdhrExecutionReadonlyForm.vue'
import { buildSignatureTimePayload, createSignatureTimeForm, type EdhrSignatureTimeForm } from './signatureTime'

defineOptions({ name: 'MesProFeedbackEdhrExecutionForm' })

type DraftFieldValue = string | number | boolean | null
type DraftAttachmentValue = string | string[]

type SnapshotFieldOption = {
  label: string
  value: string
}

type SnapshotAttachmentRule = {
  required: boolean
  minCount: number
  maxCount?: number
  attachmentType?: string
  groupKey?: string
}

type RawSignatureCellMarker = {
  rowIndex?: unknown
  columnIndex?: unknown
  enabled?: unknown
  actionType?: unknown
  label?: unknown
  displayFormat?: unknown
  signatureCellKey?: unknown
}

type RawExecutionSnapshotCell = {
  text?: unknown
  value?: unknown
  content?: unknown
  displayText?: unknown
  merge?: unknown
  fillForm?: Record<string, unknown>
  edhrCellRule?: Record<string, unknown>
  edhrSignature?: RawSignatureCellMarker
}

type RawExecutionSnapshotLayout = {
  rows?: Record<string, { cells?: Record<string, RawExecutionSnapshotCell> }>
}

type NormalizedSignatureCellMarker = {
  rowIndex: number
  columnIndex: number
  enabled: boolean
  actionType?: string
  label?: string
  displayFormat?: string
  signatureCellKey?: string
}

type NormalizedSnapshotField = {
  fieldIdentity: string
  fieldKey: string
  fieldPath: string
  rowIndex: number
  columnIndex: number
  label: string
  placeholder: string
  helpText: string
  required: boolean
  readonly: boolean
  valueType: EdhrFieldValueType
  componentKind:
    | 'text'
    | 'textarea'
    | 'number'
    | 'date'
    | 'datetime'
    | 'select'
    | 'checkbox'
    | 'signature'
    | 'upload-file'
    | 'upload-image'
    | 'upload-images'
  options: SnapshotFieldOption[]
  constraints: Record<string, unknown>
  attachmentRule?: SnapshotAttachmentRule
  unit?: string
  defaultValue: DraftFieldValue
  expectedOldValueHash?: string
  signatureMarker?: NormalizedSignatureCellMarker
  signatureActionType?: string
  signatureLabel?: string
  signatureCellKey?: string
}

type AssistChoiceGroupOption = SnapshotFieldOption & {
  fieldIdentity: string
  rowIndex: number
  columnIndex: number
}

type AssistChoiceGroupField = Omit<
  NormalizedSnapshotField,
  'componentKind' | 'valueType' | 'defaultValue' | 'options'
> & {
  type: 'choice-group'
  componentKind: 'choice-group'
  valueType: 'STRING'
  defaultValue: string
  options: AssistChoiceGroupOption[]
  fields: NormalizedSnapshotField[]
  storageMode: 'multiple-checkbox' | 'single-choice-value'
}

type AssistFillField = NormalizedSnapshotField | AssistChoiceGroupField

const isAssistChoiceGroupField = (field: AssistFillField): field is AssistChoiceGroupField =>
  (field as AssistChoiceGroupField).type === 'choice-group'

type PendingFieldChange = EdhrFieldChangeItemReqVO & {
  fieldIdentity: string
  fieldLabel: string
  oldValueDisplay: string
  oldValueJson: EdhrFieldTypedJsonValue
  expectedOldValueHash?: string
  validationMessage: string
}

type PendingAttachmentChange = Omit<EdhrFieldAttachmentChangeReqVO, 'workTaskId'> & {
  workTaskId?: string | number
  fieldIdentity: string
  fieldLabel: string
  fileUrl: string
  validationMessage: string
}

type AssistIncompleteItem = {
  fieldIdentity: string
  label: string
  reason: string
}

type FillActionResultType = 'save-success' | 'submit-success' | 'submit-failed'
type FillActionResultTone = 'success' | 'danger'

type FillActionResultDialogState = {
  statusText: string
  tone: FillActionResultTone
  orderText: string
  processText: string
  fillerText: string
}

type BatchSharedFillScopeRange = {
  sourceTableIndex: number
  startRow: number
  endRow: number
}

type BatchSharedFillScopeParseResult = {
  ranges: BatchSharedFillScopeRange[]
  error: string
}

const ARCHIVE_QUERY_PERMISSION = 'mes:pro-batch-record-execution-archive:query'
const ARCHIVE_CREATE_PERMISSION = 'mes:pro-batch-record-execution-archive:create'
const ARCHIVE_DOWNLOAD_PERMISSION = 'mes:pro-batch-record-execution-archive:download'
const EXECUTION_UPDATE_PERMISSION = 'mes:pro-batch-record-execution:update'
const FIELD_AUDIT_UPDATE_PERMISSION = 'mes:pro-batch-record-execution:field-audit-update'
const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'

const ARCHIVE_STATUS_LABEL_MAP: Record<string, string> = {
  GENERATING: '生成中',
  SEALED: '已封存',
  FAILED: '生成失败'
}

const FORM_REVIEW_ALLOWED_STATUSES: number[] = [
  EDHR_EXECUTION_STATUS.DRAFT,
  EDHR_EXECUTION_STATUS.SUBMITTED,
  EDHR_EXECUTION_STATUS.REJECTED
]

const SIGNATURE_ACTION_LABELS: Record<string, string> = {
  FIELD_CHANGE: '字段变更电子签名',
  FORM_REVIEW: '表单复核签名',
  SUBMIT: '提交签名',
  APPROVE: '审批签名',
  REJECT: '驳回签名',
  ARCHIVE_SEAL: '归档封存签名'
}

const FILL_ACTION_RESULT_MAP: Record<
  FillActionResultType,
  Pick<FillActionResultDialogState, 'statusText' | 'tone'>
> = {
  'save-success': {
    statusText: '已保存',
    tone: 'success'
  },
  'submit-success': {
    statusText: '已提交',
    tone: 'success'
  },
  'submit-failed': {
    statusText: '提交失败',
    tone: 'danger'
  }
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const message = useMessage()
const loading = ref(false)
const fitMode = ref<'width' | 'height'>('width')
const fillViewMode = ref<'assist' | 'original'>('assist')
const fillWorkspaceRef = ref<HTMLElement>()
const highlightedAssistFieldIdentity = ref('')
let assistHighlightTimer: number | undefined
const isFillWorkspaceFullscreen = ref(false)
const fieldAuditSaveLoading = ref(false)
const formReviewSignLoading = ref(false)
const submitLoading = ref(false)
const archiveLoading = ref(false)
const archiveGenerateLoading = ref(false)
const archiveDownloadLoading = ref(false)
const loadError = ref('')
const recordbookGlobalEnabled = ref(true)
const archiveError = ref('')
const formReviewSignError = ref('')
const trackingError = ref('')
const signatureError = ref('')
const execution = ref<ProFeedbackEdhrExecutionVO>()
const loadedExecutionContextKey = ref('')
const latestArchive = ref<ProFeedbackEdhrExecutionArchiveRespVO>()
const trackingTimeline = ref<EdhrTrackingEventVO[]>([])
const signatureRows = ref<EdhrSignatureSummaryVO[]>([])
const draftFieldValues = ref<Record<string, DraftFieldValue>>({})
const draftAttachmentValues = ref<Record<string, DraftAttachmentValue>>({})
const draftImageAttachmentValues = ref<Record<string, string>>({})
const attachmentMetadataByUrl = ref<Record<string, EdhrAttachmentPrepareUploadRespVO>>({})
const baselineFieldValues = ref<Record<string, DraftFieldValue>>({})
const baselineFieldValueHashes = ref<Record<string, string>>({})
const cellLinkPrefills = ref<BatchRecordCellLinkPrefillItemVO[]>([])
const cellLinkConflicts = ref<BatchRecordCellLinkPrefillItemVO[]>([])
const draftRemark = ref('')
const submitDialogVisible = ref(false)
const fillActionResultDialogVisible = ref(false)
const formReviewSignDialogVisible = ref(false)
const archiveDialogVisible = ref(false)
const fillActionResultDialog = reactive<FillActionResultDialogState>({
  statusText: '',
  tone: 'success',
  orderText: '--',
  processText: '--',
  fillerText: '--'
})
const DEFAULT_FIELD_AUDIT_DRAFT_REASON_CATEGORY: EdhrFieldChangeReasonCategory = 'OPERATOR_ENTRY'
const DEFAULT_FIELD_AUDIT_DRAFT_REASON_TEXT = '保存草稿'
const fieldAuditReasonForm = reactive({
  reasonCategory: undefined as EdhrFieldChangeReasonCategory | undefined,
  reasonText: ''
})
const fieldAuditSaveError = ref('')
const fieldAuditLastResult = ref<EdhrFieldChangeSaveRespVO>()
const fieldAuditLastSuccessTitle = computed(() => {
  const result = fieldAuditLastResult.value
  return result
    ? `字段审计批次 ${result.auditBatchId} 已保存，链路校验：${resolveHashStatusLabel(result.hashVerification.status)}`
    : ''
})
const fieldAuditIdempotencyKey = ref('')
const submitForm = reactive({
  password: ''
})
const submitReviewAssigneeSelections = reactive<Record<string, number | undefined>>({})
const formReviewSignForm = reactive({
  password: '',
  comment: ''
})
const formReviewSignatureTimeForm = reactive<EdhrSignatureTimeForm>(createSignatureTimeForm())
const archiveForm = reactive({
  sealPassword: '',
  comment: '',
  regenerate: false
})
const archiveArtifactType = EDHR_EXECUTION_ARCHIVE_ARTIFACT_PDF
const TRACKING_VIEW_MODE = 'tracking'
const RECORDBOOK_UNRESTRICTED_FILL_MODE = 'RECORDBOOK_UNRESTRICTED'
const BATCH_SHARED_INSTANCE_SCOPE = 'BATCH_SHARED'
const ASSIST_SWITCH_PAGE_SIZE = 50
let executionPageRequestSerial = 0
let executionSecondaryFrameId: number | undefined

const isGlobalRecordbookEnabled = computed(
  () => recordbookGlobalEnabled.value && execution.value?.recordbookEnabled === true
)

type AssistSwitchDialogType = 'task' | 'process' | 'filler'

type AssistFillerSwitchItem = {
  key: string
  task: EdhrBatchExecutionTaskRespVO
  userId: number
  displayName: string
}

const assistSwitchDialogVisible = ref(false)
const assistSwitchDialogType = ref<AssistSwitchDialogType>('task')
const assistTaskSwitchLoading = ref(false)
const assistTaskSwitchError = ref('')
const assistTaskSwitchItems = ref<EdhrWorkTaskRespVO[]>([])
const assistProcessSwitchLoading = ref(false)
const assistProcessSwitchError = ref('')
const assistProcessSwitchItems = ref<EdhrBatchExecutionTaskRespVO[]>([])
const assistFillerSwitchLoading = ref(false)
const assistFillerSwitchError = ref('')
const assistFillerSwitchItems = ref<AssistFillerSwitchItem[]>([])

const assistSwitchDialogTitle = computed(() => {
  if (assistSwitchDialogType.value === 'process') return '切换工序'
  if (assistSwitchDialogType.value === 'filler') return '切换填写人'
  return '切换任务 / 批次'
})

const parsePositiveNumber = (value: unknown) => {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) && numericValue > 0 ? numericValue : undefined
}

const parseNonNegativeInteger = (value: unknown) => {
  const numericValue = Number(value)
  return Number.isInteger(numericValue) && numericValue >= 0 ? numericValue : undefined
}

const readRouteQueryString = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' && rawValue.trim() ? rawValue.trim() : ''
}

const parseSharedFillScopeJson = (fillableScopeJson: string): BatchSharedFillScopeParseResult => {
  if (!fillableScopeJson.trim()) {
    return { ranges: [], error: '共享表单填写范围 fillableScopeJson 缺失。' }
  }
  try {
    const parsed = JSON.parse(fillableScopeJson) as { ranges?: unknown }
    if (!Array.isArray(parsed.ranges) || parsed.ranges.length === 0) {
      return { ranges: [], error: '共享表单填写范围必须包含 ranges 数组。' }
    }
    const ranges: BatchSharedFillScopeRange[] = []
    for (const range of parsed.ranges) {
      const sourceTableIndex = parseNonNegativeInteger((range as any)?.sourceTableIndex)
      const startRow = parseNonNegativeInteger((range as any)?.startRow)
      const endRow = parseNonNegativeInteger((range as any)?.endRow)
      if (sourceTableIndex === undefined || startRow === undefined || endRow === undefined) {
        return { ranges: [], error: '共享表单填写范围必须包含 sourceTableIndex、startRow、endRow。' }
      }
      if (startRow > endRow) {
        return { ranges: [], error: '共享表单填写范围 startRow 不能大于 endRow。' }
      }
      ranges.push({ sourceTableIndex, startRow, endRow })
    }
    return { ranges, error: '' }
  } catch (error) {
    const message = error instanceof Error && error.message.trim() ? error.message : '未知解析错误'
    return { ranges: [], error: `共享表单填写范围解析失败：${message}` }
  }
}

const resolveSharedExecutionSourceTableIndex = () => {
  const metaJson = execution.value?.metaJson
  if (typeof metaJson !== 'string' || !metaJson.trim()) {
    return undefined
  }
  try {
    const parsed = JSON.parse(metaJson) as { sourceTableIndex?: unknown }
    return parseNonNegativeInteger(parsed.sourceTableIndex)
  } catch {
    return undefined
  }
}

const currentTaskInstanceScope = computed(() =>
  readRouteQueryString(route.query.instanceScope).toUpperCase()
)
const isBatchSharedExecutionTask = computed(
  () => currentTaskInstanceScope.value === BATCH_SHARED_INSTANCE_SCOPE
)
const currentFillableScopeJson = computed(() => readRouteQueryString(route.query.fillableScopeJson))
const sharedFillScopeParseResult = computed(() =>
  parseSharedFillScopeJson(currentFillableScopeJson.value)
)
const sharedExecutionSourceTableIndex = computed(() => resolveSharedExecutionSourceTableIndex())
const sharedFillScopeGateError = computed(() => {
  if (!isBatchSharedExecutionTask.value || isTrackingReadonlyMode.value) {
    return ''
  }
  if (hasGoldenFingerPermission.value) {
    return ''
  }
  if (!currentFillableScopeJson.value) {
    return '共享表单填写范围 fillableScopeJson 缺失，不能打开填写页。'
  }
  if (sharedFillScopeParseResult.value.error) {
    return sharedFillScopeParseResult.value.error
  }
  if (sharedExecutionSourceTableIndex.value === undefined) {
    return '共享表单执行快照缺少 sourceTableIndex，不能打开填写页。'
  }
  return ''
})

const resetSignatureTimeForm = (form: EdhrSignatureTimeForm) => {
  Object.assign(form, createSignatureTimeForm())
}

const slotContextBlockers = computed(() => {
  const current = execution.value
  if (!current || isTrackingReadonlyMode.value) return [] as string[]
  return [
    !current.formSlotType ? '缺少槽位类型' : '',
    !current.recordCategory ? '缺少记录分类' : '',
    !current.validationProfile ? '缺少校验策略' : '',
    !current.slotConfigSnapshotHash ? '缺少槽位快照' : '',
    sharedFillScopeGateError.value
  ].filter(Boolean)
})

const hasSlotContextBlockers = computed(() => slotContextBlockers.value.length > 0)

const recordbookGlobalDisabledNotice = computed(() => {
  if (
    route.query.fillCarrier === 'RECORDBOOK' &&
    route.query.fillMode === RECORDBOOK_UNRESTRICTED_FILL_MODE &&
    isGlobalRecordbookEnabled.value === false
  ) {
    return '记录本全局开关已关闭或当前任务未启用记录本，不能进入记录本不受控填写。'
  }
  return ''
})

const isRecordbookUnrestrictedMode = computed(() => {
  if (recordbookGlobalDisabledNotice.value) return false
  return (
    execution.value?.recordbookEnabled === true &&
    isGlobalRecordbookEnabled.value === true &&
    route.query.fillCarrier === 'RECORDBOOK' &&
    route.query.fillMode === RECORDBOOK_UNRESTRICTED_FILL_MODE
  )
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  if (typeof error === 'string' && error.trim()) {
    return error
  }
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage
  }
  return defaultMessage
}

const resolveSnapshotFieldLabel = (
  field: ProFeedbackEdhrSnapshotFieldVO,
  rowIndex: number,
  columnIndex: number
) => {
  for (const candidate of [field.label, field.name, field.title, field.fieldKey]) {
    if (typeof candidate === 'string' && candidate.trim()) {
      return candidate.trim()
    }
  }
  return `字段(${rowIndex},${columnIndex})`
}

const resolveSnapshotFieldPlaceholder = (
  field: ProFeedbackEdhrSnapshotFieldVO,
  label: string,
  componentKind: NormalizedSnapshotField['componentKind']
) => {
  if (typeof field.placeholder === 'string' && field.placeholder.trim()) {
    return field.placeholder.trim()
  }
  if (componentKind === 'select' || componentKind === 'date' || componentKind === 'datetime') {
    return `请选择${label}`
  }
  return `请输入${label}`
}

const resolveSnapshotFieldHelpText = (field: ProFeedbackEdhrSnapshotFieldVO) => {
  if (typeof field.helpText === 'string' && field.helpText.trim()) {
    return field.helpText.trim()
  }
  const rule = (field as any)?.edhrCellRule
  if (rule && typeof rule.helpText === 'string' && rule.helpText.trim()) {
    return rule.helpText.trim()
  }
  return ''
}

const resolveSnapshotFieldOptions = (field: ProFeedbackEdhrSnapshotFieldVO): SnapshotFieldOption[] => {
  const source = Array.isArray(field.options) ? field.options : []
  return source
    .map((option) => {
      if (option == null) {
        return null
      }
      if (typeof option === 'string' || typeof option === 'number' || typeof option === 'boolean') {
        return {
          label: String(option),
          value: String(option)
        }
      }
      const optionValue = option.value ?? option.id ?? option.code ?? option.key ?? option.label
      const optionLabel = option.label ?? option.name ?? option.text ?? optionValue
      if (optionValue == null || optionLabel == null) {
        return null
      }
      return {
        label: String(optionLabel),
        value: String(optionValue)
      }
    })
    .filter((option): option is SnapshotFieldOption => Boolean(option))
}

const resolveSnapshotAttachmentRule = (
  field: ProFeedbackEdhrSnapshotFieldVO
): SnapshotAttachmentRule | undefined => {
  const rawRule = field.attachmentRule
  if (!rawRule || typeof rawRule !== 'object' || Array.isArray(rawRule)) {
    return undefined
  }
  const rule = rawRule as Record<string, unknown>
  const required = rule.required === true
  const minCount = parsePositiveNumber(rule.minCount) || (required ? 1 : 0)
  const maxCount = parsePositiveNumber(rule.maxCount)
  const attachmentType = typeof rule.attachmentType === 'string' ? rule.attachmentType.trim() : ''
  const groupKey = typeof rule.groupKey === 'string' ? rule.groupKey.trim() : ''
  if (!required && !minCount && !maxCount && !attachmentType && !groupKey) {
    return undefined
  }
  return {
    required,
    minCount,
    maxCount: maxCount || undefined,
    attachmentType: attachmentType || undefined,
    groupKey: groupKey || undefined
  }
}

const normalizeSignatureCellMarker = (
  marker: RawSignatureCellMarker | undefined,
  rowIndex: number,
  columnIndex: number
): NormalizedSignatureCellMarker | undefined => {
  if (!marker || marker.enabled !== true) {
    return undefined
  }
  const rawActionType = typeof marker.actionType === 'string' ? marker.actionType.trim().toUpperCase() : ''
  const label = typeof marker.label === 'string' && marker.label.trim() ? marker.label.trim() : undefined
  const displayFormat =
    typeof marker.displayFormat === 'string' && marker.displayFormat.trim()
      ? marker.displayFormat.trim()
      : undefined
  const signatureCellKey =
    typeof marker.signatureCellKey === 'string' && marker.signatureCellKey.trim()
      ? marker.signatureCellKey.trim()
      : `R${rowIndex + 1}C${columnIndex + 1}`
  return {
    rowIndex,
    columnIndex,
    enabled: true,
    actionType: rawActionType || undefined,
    label,
    displayFormat,
    signatureCellKey
  }
}

const resolveSnapshotLayoutSignatureMarker = (
  rowIndex: number,
  columnIndex: number
): NormalizedSignatureCellMarker | undefined => {
  const layout = parsedSnapshot.value.parsed?.layout as RawExecutionSnapshotLayout | undefined
  const row = layout?.rows?.[String(rowIndex)]
  const marker = row?.cells?.[String(columnIndex)]?.edhrSignature
  return normalizeSignatureCellMarker(marker, rowIndex, columnIndex)
}

const resolveExecutionSignatureMarker = (
  rowIndex: number,
  columnIndex: number
): NormalizedSignatureCellMarker | undefined => {
  const directMarkers = (execution.value as any)?.signatureCellMarkers
  if (!Array.isArray(directMarkers)) {
    return undefined
  }
  const marker = directMarkers.find((item) => {
    const itemRowIndex = parseNonNegativeInteger(item?.rowIndex)
    const itemColumnIndex = parseNonNegativeInteger(item?.columnIndex)
    return itemRowIndex === rowIndex && itemColumnIndex === columnIndex
  }) as RawSignatureCellMarker | undefined
  return normalizeSignatureCellMarker(marker, rowIndex, columnIndex)
}

const resolveSignatureCellMarker = (
  rowIndex: number,
  columnIndex: number
): NormalizedSignatureCellMarker | undefined => {
  return (
    resolveSnapshotLayoutSignatureMarker(rowIndex, columnIndex) ||
    resolveExecutionSignatureMarker(rowIndex, columnIndex)
  )
}

const formatAttachmentRule = (rule?: SnapshotAttachmentRule) => {
  if (!rule) return ''
  const parts = [
    rule.required ? '必需附件' : '可选附件',
    rule.minCount ? `至少 ${rule.minCount} 个` : '',
    rule.maxCount ? `最多 ${rule.maxCount} 个` : '',
    rule.attachmentType ? `类型 ${rule.attachmentType}` : '',
    rule.groupKey ? `组 ${rule.groupKey}` : ''
  ].filter(Boolean)
  return parts.join('，')
}

const resolveSnapshotComponentKind = (
  field: ProFeedbackEdhrSnapshotFieldVO
): NormalizedSnapshotField['componentKind'] => {
  const valueType = String(field.valueType || '').toUpperCase()
  if (valueType === 'NUMBER') return 'number'
  if (valueType === 'DATE') return 'date'
  if (valueType === 'DATETIME') return 'datetime'
  if (valueType === 'BOOLEAN') return 'checkbox'
  if (valueType === 'SIGNATURE') return 'signature'
  const rawType = String(
    field.inputType || field.componentType || field.component || field.componentFlag || field.type || ''
  ).toLowerCase()
  if (rawType.includes('upload-images') || rawType.includes('image-list')) {
    return 'upload-images'
  }
  if (rawType.includes('upload-image') || rawType.includes('image')) {
    return 'upload-image'
  }
  if (rawType.includes('upload-file') || rawType.includes('attachment') || rawType.includes('file')) {
    return 'upload-file'
  }
  if (rawType.includes('signature') || rawType.includes('sign')) {
    return 'signature'
  }
  if (rawType.includes('textarea') || rawType.includes('multiline') || rawType.includes('remark')) {
    return 'textarea'
  }
  if (rawType.includes('number') || rawType.includes('digit') || rawType.includes('decimal')) {
    return 'number'
  }
  if (rawType.includes('datetime') || rawType.includes('date-time')) {
    return 'datetime'
  }
  if (rawType.includes('date')) {
    return 'date'
  }
  if (rawType.includes('switch') || rawType.includes('boolean') || rawType.includes('checkbox')) {
    return 'checkbox'
  }
  const options = resolveSnapshotFieldOptions(field)
  if (options.length > 0 || rawType.includes('select') || rawType.includes('enum')) {
    return 'select'
  }
  return 'text'
}

const resolveSnapshotDefaultValue = (
  field: ProFeedbackEdhrSnapshotFieldVO,
  componentKind: NormalizedSnapshotField['componentKind'],
  options: SnapshotFieldOption[] = []
): DraftFieldValue => {
  const seed = field.defaultValue ?? field.value ?? ''
  if (componentKind === 'checkbox' && options.length > 1) {
    return seed == null || typeof seed === 'boolean' ? '' : String(seed)
  }
  if (componentKind === 'checkbox') {
    return seed === true || String(seed).toLowerCase() === 'true'
  }
  if (componentKind === 'number') {
    if (seed == null || (typeof seed === 'string' && seed.trim() === '') || seed === '') {
      return null
    }
    const numericValue = Number(seed)
    if (!Number.isFinite(numericValue)) {
      throw new Error('NUMBER 字段默认值不是有效数字，不能渲染或保存字段审计。')
    }
    return numericValue
  }
  if (componentKind === 'date' || componentKind === 'datetime') {
    return seed == null ? '' : String(seed)
  }
  return seed == null ? '' : String(seed)
}

const isAttachmentComponentKind = (componentKind: AssistFillField['componentKind']) =>
  componentKind === 'upload-file' ||
  componentKind === 'upload-image' ||
  componentKind === 'upload-images'

const isSingleChoiceCheckboxField = (
  field: Pick<NormalizedSnapshotField, 'componentKind' | 'options'>
) => field.componentKind === 'checkbox' && field.options.length > 1

const buildFieldIdentity = (
  fieldPath: string,
  fieldKey: string,
  rowIndex: number,
  columnIndex: number
) => {
  return `${fieldPath}::${fieldKey}::${rowIndex}:${columnIndex}`
}

const resolveSnapshotFieldContractError = (
  field: ProFeedbackEdhrSnapshotFieldVO,
  index: number
) => {
  const rowIndex = parseNonNegativeInteger(field.rowIndex ?? (field.position as any)?.rowIndex)
  const columnIndex = parseNonNegativeInteger(
    field.columnIndex ?? (field.position as any)?.columnIndex
  )
  if (rowIndex == null || columnIndex == null) {
    return `第 ${index + 1} 个字段缺少有效 rowIndex / columnIndex，不能保存字段审计。`
  }
  if (typeof field.fieldKey !== 'string' || !field.fieldKey.trim()) {
    return `第 ${index + 1} 个字段缺少 fieldKey，不能保存字段审计。`
  }
  if (typeof field.fieldPath !== 'string' || !field.fieldPath.trim()) {
    return `第 ${index + 1} 个字段缺少 fieldPath，不能保存字段审计。`
  }
  const componentKind = resolveSnapshotComponentKind(field)
  const seed = field.defaultValue ?? field.value ?? ''
  if (
    componentKind === 'number' &&
    seed != null &&
    !(typeof seed === 'string' && seed.trim() === '') &&
    seed !== ''
  ) {
    const numericValue = Number(seed)
    if (!Number.isFinite(numericValue)) {
      return `第 ${index + 1} 个 NUMBER 字段默认值不是有效数字，不能渲染或保存字段审计。`
    }
  }
  return ''
}

const isEdhrFieldValueType = (value: unknown): value is EdhrFieldValueType =>
  ['STRING', 'NUMBER', 'BOOLEAN', 'DATE', 'DATETIME', 'SIGNATURE', 'JSON', 'NULL'].includes(
    String(value)
  )

const resolveFieldValueType = (
  field: ProFeedbackEdhrSnapshotFieldVO,
  componentKind: NormalizedSnapshotField['componentKind'],
  options: SnapshotFieldOption[] = []
): EdhrFieldValueType => {
  if (componentKind === 'checkbox' && options.length > 1) {
    return 'STRING'
  }
  const rawValueType = String(field.valueType || field.dataType || field.type || '').toUpperCase()
  if (isEdhrFieldValueType(rawValueType)) {
    return rawValueType
  }
  if (componentKind === 'number') {
    return 'NUMBER'
  }
  if (componentKind === 'checkbox') {
    return 'BOOLEAN'
  }
  if (componentKind === 'date') {
    return 'DATE'
  }
  if (componentKind === 'datetime') {
    return 'DATETIME'
  }
  return 'STRING'
}

const normalizeSnapshotField = (
  field: ProFeedbackEdhrSnapshotFieldVO
): NormalizedSnapshotField | null => {
  const rowIndex = parseNonNegativeInteger(field.rowIndex ?? (field.position as any)?.rowIndex)
  const columnIndex = parseNonNegativeInteger(
    field.columnIndex ?? (field.position as any)?.columnIndex
  )
  if (rowIndex == null || columnIndex == null) {
    return null
  }

  const componentKind = resolveSnapshotComponentKind(field)
  const options = resolveSnapshotFieldOptions(field)
  const label = resolveSnapshotFieldLabel(field, rowIndex, columnIndex)
  const fieldKey = typeof field.fieldKey === 'string' ? field.fieldKey.trim() : ''
  const fieldPath = typeof field.fieldPath === 'string' ? field.fieldPath.trim() : ''
  if (!fieldKey || !fieldPath) {
    return null
  }
  const fieldIdentity = buildFieldIdentity(fieldPath, fieldKey, rowIndex, columnIndex)
  const valueType = resolveFieldValueType(field, componentKind, options)
  const signatureMarker = resolveSignatureCellMarker(rowIndex, columnIndex)

  return {
    fieldIdentity,
    fieldKey,
    fieldPath,
    rowIndex,
    columnIndex,
    label,
    placeholder: resolveSnapshotFieldPlaceholder(field, label, componentKind),
    helpText: resolveSnapshotFieldHelpText(field),
    required: Boolean(field.required),
    readonly: Boolean(field.readonly || field.disabled || componentKind === 'signature'),
    valueType,
    componentKind,
    options,
    constraints:
      field.constraints && typeof field.constraints === 'object' && !Array.isArray(field.constraints)
        ? (field.constraints as Record<string, unknown>)
        : {},
    attachmentRule: resolveSnapshotAttachmentRule(field),
    unit: typeof field.unit === 'string' && field.unit.trim() ? field.unit.trim() : undefined,
    defaultValue: resolveSnapshotDefaultValue(field, componentKind, options),
    expectedOldValueHash:
      typeof field.expectedOldValueHash === 'string' ? field.expectedOldValueHash : undefined,
    signatureMarker,
    signatureActionType: signatureMarker?.actionType,
    signatureLabel: signatureMarker?.label,
    signatureCellKey: signatureMarker?.signatureCellKey
  }
}

const executionId = computed(() => parsePositiveRouteQueryId(route.query.id) || undefined)
const isTrackingReadonlyMode = computed(() => route.query.viewMode === TRACKING_VIEW_MODE)
const workTaskId = computed(() => parsePositiveRouteQueryId(route.query.workTaskId) || undefined)
const hasFillTaskContext = computed(() => workTaskId.value !== undefined)
const resolveExecutionContextKey = () =>
  executionId.value ? `${executionId.value}:${workTaskId.value || ''}` : ''
const hasArchiveQueryPermission = computed(() => hasPermission([ARCHIVE_QUERY_PERMISSION]))
const hasArchiveCreatePermission = computed(() => hasPermission([ARCHIVE_CREATE_PERMISSION]))
const hasArchiveDownloadPermission = computed(() => hasPermission([ARCHIVE_DOWNLOAD_PERMISSION]))
const hasExecutionUpdatePermission = computed(() => hasPermission([EXECUTION_UPDATE_PERMISSION]))
const hasGoldenFingerPermission = computed(
  () => userStore.permissions.has(GOLDEN_FINGER_PERMISSION)
)
const hasFieldAuditUpdatePermission = computed(
  () =>
    hasPermission([FIELD_AUDIT_UPDATE_PERMISSION]) ||
    (workTaskId.value !== undefined && hasExecutionUpdatePermission.value) ||
    hasGoldenFingerPermission.value
)

const createEdhrAttachmentUploadRequest =
  (field: NormalizedSnapshotField) => async (options: UploadRequestOptions) => {
    if (!execution.value?.id) {
      throw new Error('执行记录编号缺失，不能上传 eDHR 受控附件。')
    }
    if (!workTaskId.value) {
      throw new Error('工作任务编号缺失，不能上传 eDHR 受控附件。')
    }
    if (!isFieldInCurrentFillScope(field)) {
      throw new Error('当前附件不在本工序填写范围内，不能上传。')
    }
    const result = await prepareEdhrAttachmentUpload(
      {
        executionId: execution.value.id,
        workTaskId: workTaskId.value,
        file: options.file
      },
      (event) => options.onProgress(event)
    )
    if (!result.fileUrl) {
      throw new Error(`附件字段 ${field.label} 上传成功但缺少 fileUrl，不能进入 eDHR 审计链。`)
    }
    attachmentMetadataByUrl.value = {
      ...attachmentMetadataByUrl.value,
      [result.fileUrl]: result
    }
    return { data: result.fileUrl }
  }

const parsedSnapshot = computed(() => {
  const rawSnapshot = execution.value?.executionSnapshotJson
  if (typeof rawSnapshot !== 'string' || !rawSnapshot.trim()) {
    return {
      error: 'eDHR 执行快照缺少 executionSnapshotJson，无法渲染执行页。',
      parsed: undefined as ProFeedbackEdhrExecutionSnapshotVO | undefined
    }
  }

  try {
    return {
      error: '',
      parsed: JSON.parse(rawSnapshot) as ProFeedbackEdhrExecutionSnapshotVO
    }
  } catch (error) {
    const errorMessage =
      error instanceof Error && error.message.trim() ? error.message : '未知解析错误'
    return {
      error: `executionSnapshotJson 解析失败：${errorMessage}`,
      parsed: undefined as ProFeedbackEdhrExecutionSnapshotVO | undefined
    }
  }
})

const snapshotFieldContractErrors = computed(() => {
  const fields = parsedSnapshot.value.parsed?.fields
  if (!Array.isArray(fields)) {
    return [] as string[]
  }
  return fields
    .map((field, index) => resolveSnapshotFieldContractError(field, index))
    .filter((error) => Boolean(error))
})

const snapshotFields = computed(() => {
  const fields = parsedSnapshot.value.parsed?.fields
  if (!Array.isArray(fields)) {
    return [] as NormalizedSnapshotField[]
  }
  if (snapshotFieldContractErrors.value.length > 0) {
    return [] as NormalizedSnapshotField[]
  }
  return fields
    .map((field) => normalizeSnapshotField(field))
    .filter((field): field is NormalizedSnapshotField => Boolean(field))
})

const templateFieldByCell = computed(() => {
  const map = new Map<string, NormalizedSnapshotField>()
  snapshotFields.value.forEach((field) => {
    map.set(buildCellValueKey(field.rowIndex, field.columnIndex), field)
  })
  return map
})

const readSnapshotCellText = (cell?: RawExecutionSnapshotCell) => {
  if (!cell) return ''
  const directCandidates = [cell.text, cell.displayText, cell.content, cell.value]
  for (const candidate of directCandidates) {
    if (typeof candidate === 'string' && candidate.trim()) {
      return candidate.trim()
    }
  }
  const fillLabel = cell.fillForm?.labelText || cell.fillForm?.label
  if (typeof fillLabel === 'string' && fillLabel.trim()) {
    return fillLabel.trim()
  }
  const ruleLabel = cell.edhrCellRule?.label
  if (typeof ruleLabel === 'string' && ruleLabel.trim()) {
    return ruleLabel.trim()
  }
  return ''
}

const normalizeSnapshotCellMerge = (cell?: RawExecutionSnapshotCell) => {
  if (!Array.isArray(cell?.merge)) return { rowSpan: 1, colSpan: 1 }
  const rowDelta = Number(cell.merge[0])
  const columnDelta = Number(cell.merge[1])
  return {
    rowSpan: Number.isInteger(rowDelta) && rowDelta >= 0 ? rowDelta + 1 : 1,
    colSpan: Number.isInteger(columnDelta) && columnDelta >= 0 ? columnDelta + 1 : 1
  }
}

const normalizeAssistCheckboxOptionLabel = (value: unknown) =>
  String(value ?? '')
    .replace(/[□☐☑☒✓√]/g, '')
    .replace(/[_＿]{2,}/g, '')
    .replace(/\s+/g, ' ')
    .trim()

const resolveAssistCheckboxOptionLabel = (field: NormalizedSnapshotField) => {
  if (field.options.length === 1) {
    return normalizeAssistCheckboxOptionLabel(field.options[0].label)
  }
  return normalizeAssistCheckboxOptionLabel(field.label)
}

const resolveAssistChoiceGroupHeaderLabel = (fields: NormalizedSnapshotField[]) => {
  const layout = parsedSnapshot.value.parsed?.layout as RawExecutionSnapshotLayout | undefined
  const rows = layout?.rows
  if (!rows || fields.length === 0) {
    return ''
  }
  const rowIndex = Math.min(...fields.map((field) => field.rowIndex))
  const minColumnIndex = Math.min(...fields.map((field) => field.columnIndex))
  const maxColumnIndex = Math.max(...fields.map((field) => field.columnIndex))
  const optionLabels = new Set(fields.map(resolveAssistCheckboxOptionLabel).filter(Boolean))
  for (let headerRowIndex = rowIndex - 1; headerRowIndex >= 0; headerRowIndex -= 1) {
    const row = rows[String(headerRowIndex)]
    if (!row?.cells) continue
    const candidates = Object.entries(row.cells)
      .map(([columnKey, cell]) => {
        const columnIndex = parseNonNegativeInteger(columnKey)
        if (columnIndex == null) return null
        const merge = normalizeSnapshotCellMerge(cell)
        const startColumnIndex = columnIndex
        const endColumnIndex = columnIndex + merge.colSpan - 1
        if (endColumnIndex < minColumnIndex || startColumnIndex > maxColumnIndex) return null
        const text = normalizeAssistCheckboxOptionLabel(readSnapshotCellText(cell))
        if (!text || optionLabels.has(text)) return null
        return {
          text,
          coversAll: startColumnIndex <= minColumnIndex && endColumnIndex >= maxColumnIndex,
          distance: rowIndex - headerRowIndex,
          span: merge.colSpan
        }
      })
      .filter((candidate): candidate is { text: string; coversAll: boolean; distance: number; span: number } =>
        Boolean(candidate)
      )
    const coveringCandidate = candidates.find((candidate) => candidate.coversAll)
    if (coveringCandidate) {
      return coveringCandidate.text
    }
    if (candidates.length === 1) {
      return candidates[0].text
    }
  }
  return ''
}

const resolveAssistChoiceGroupLabel = (fields: NormalizedSnapshotField[]) => {
  const headerLabel = resolveAssistChoiceGroupHeaderLabel(fields)
  if (headerLabel) {
    return headerLabel
  }
  const sharedHelpText = fields[0]?.helpText
  if (sharedHelpText && fields.every((field) => field.helpText === sharedHelpText)) {
    return sharedHelpText
  }
  return fields.length === 1 ? fields[0].label : '选择项'
}

const buildAssistChoiceGroupField = (
  fields: NormalizedSnapshotField[],
  storageMode: AssistChoiceGroupField['storageMode']
): AssistChoiceGroupField => {
  const label = resolveAssistChoiceGroupLabel(fields)
  const options: AssistChoiceGroupOption[] =
    storageMode === 'single-choice-value'
      ? fields[0].options.map((option) => ({
          label: normalizeAssistCheckboxOptionLabel(option.label),
          value: String(option.value),
          fieldIdentity: fields[0].fieldIdentity,
          rowIndex: fields[0].rowIndex,
          columnIndex: fields[0].columnIndex
        }))
      : fields.map((field) => ({
          label: resolveAssistCheckboxOptionLabel(field),
          value: field.fieldIdentity,
          fieldIdentity: field.fieldIdentity,
          rowIndex: field.rowIndex,
          columnIndex: field.columnIndex
        }))

  const firstField = fields[0]
  return {
    ...firstField,
    type: 'choice-group',
    componentKind: 'choice-group',
    valueType: 'STRING',
    fieldIdentity: `choice-group::${firstField.rowIndex}:${fields
      .map((field) => field.columnIndex)
      .join(',')}::${fields.map((field) => field.fieldIdentity).join('|')}`,
    fieldKey: `choiceGroup:${fields.map((field) => field.fieldKey).join('|')}`,
    label,
    placeholder: `请选择${label}`,
    helpText: fields.find((field) => field.helpText)?.helpText || firstField.helpText,
    required: fields.some((field) => field.required),
    readonly: fields.every((field) => field.readonly),
    defaultValue: '',
    options: options.filter((option) => option.label),
    fields,
    storageMode
  }
}

const isMultipleCheckboxChoiceCandidate = (field: NormalizedSnapshotField) =>
  field.componentKind === 'checkbox' && field.options.length <= 1 && Boolean(resolveAssistCheckboxOptionLabel(field))

const hasDistinctChoiceOptions = (fields: NormalizedSnapshotField[]) => {
  const labels = fields.map(resolveAssistCheckboxOptionLabel).filter(Boolean)
  return labels.length === fields.length && new Set(labels).size === labels.length
}

const buildAssistChoiceGroupItems = (fields: NormalizedSnapshotField[]): AssistFillField[] => {
  const orderedFields = [...fields].sort(
    (left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex
  )
  const used = new Set<string>()
  const items: AssistFillField[] = []

  orderedFields.forEach((field, index) => {
    if (used.has(field.fieldIdentity)) {
      return
    }
    if (field.componentKind === 'checkbox' && field.options.length > 1) {
      used.add(field.fieldIdentity)
      items.push(buildAssistChoiceGroupField([field], 'single-choice-value'))
      return
    }
    if (!isMultipleCheckboxChoiceCandidate(field)) {
      items.push(field)
      return
    }

    const headerLabel = resolveAssistChoiceGroupHeaderLabel([field])
    const group = orderedFields
      .slice(index)
      .filter(
        (candidate) =>
          !used.has(candidate.fieldIdentity) &&
          candidate.rowIndex === field.rowIndex &&
          isMultipleCheckboxChoiceCandidate(candidate) &&
          resolveAssistChoiceGroupHeaderLabel([candidate]) === headerLabel &&
          Boolean(headerLabel)
      )

    if (group.length > 1 && hasDistinctChoiceOptions(group)) {
      group.forEach((candidate) => used.add(candidate.fieldIdentity))
      items.push(buildAssistChoiceGroupField(group, 'multiple-checkbox'))
      return
    }

    items.push(field)
  })

  return items
}

const resolveTemplateRuleValueType = (
  valueType: EdhrFieldValueType
): BatchRecordReportCellValueType => {
  if (
    valueType === 'NUMBER' ||
    valueType === 'DATE' ||
    valueType === 'DATETIME' ||
    valueType === 'BOOLEAN' ||
    valueType === 'SIGNATURE'
  ) {
    return valueType
  }
  return 'STRING'
}

const templateCellRules = computed<BatchRecordReportCellRuleVO[]>(() =>
  snapshotFields.value.map((field) => ({
    rowIndex: field.rowIndex,
    columnIndex: field.columnIndex,
    valueType: resolveTemplateRuleValueType(field.valueType),
    componentFlag: field.componentKind,
    required: isFieldRequiredForCurrentMode(field),
    label: field.label,
    placeholder: field.placeholder,
    helpText: field.helpText,
    constraints: field.constraints,
    unit: field.unit,
    attachmentRule: field.attachmentRule
  }))
)

const templateSignatureMarkers = computed<BatchRecordReportSignatureCellMarkerVO[]>(() => {
  const markers: BatchRecordReportSignatureCellMarkerVO[] = []
  snapshotFields.value
    .filter((field) => field.componentKind === 'signature')
    .forEach((field) => {
      const actionType = normalizeSignatureActionType(field.signatureActionType)
      if (!['FORM_REVIEW', 'SUBMIT', 'APPROVE'].includes(actionType)) {
        return
      }
      markers.push({
        rowIndex: field.rowIndex,
        columnIndex: field.columnIndex,
        enabled: true,
        ...(field.signatureCellKey ? { signatureCellKey: field.signatureCellKey } : {}),
        actionType: actionType as BatchRecordReportSignatureCellMarkerVO['actionType'],
        ...(field.signatureLabel ? { label: field.signatureLabel } : {})
      })
    })
  return markers
})

const templateModelValue = computed<TemplateSimulationValueMap>(() => {
  const values: TemplateSimulationValueMap = {}
  snapshotFields.value.forEach((field) => {
    values[buildCellValueKey(field.rowIndex, field.columnIndex)] =
      draftFieldValues.value[field.fieldIdentity]
  })
  return values
})

const assistFillFields = computed<AssistFillField[]>(() =>
  buildAssistChoiceGroupItems(
    snapshotFields.value
      .filter(isFieldInCurrentFillScope)
      .filter((field) => !field.readonly || field.componentKind === 'signature')
  )
)

const resolveAssistChoiceGroupValue = (field: AssistChoiceGroupField) => {
  if (field.storageMode === 'single-choice-value') {
    return String(draftFieldValues.value[field.fields[0].fieldIdentity] || '')
  }
  const selectedOption = field.options.find(
    (option) => draftFieldValues.value[option.fieldIdentity] === true
  )
  return selectedOption?.value || ''
}

const updateAssistChoiceGroupValue = (field: AssistChoiceGroupField, value: unknown) => {
  const selectedValue = String(value || '')
  if (field.storageMode === 'single-choice-value') {
    draftFieldValues.value[field.fields[0].fieldIdentity] = selectedValue
    return
  }
  field.options.forEach((option) => {
    draftFieldValues.value[option.fieldIdentity] = option.value === selectedValue
  })
}

const assistTaskSwitchLabel = computed(() => {
  const parts = [
    execution.value?.workOrderCode ||
      execution.value?.executionCode ||
      (executionId.value ? `执行 ${executionId.value}` : ''),
    execution.value?.batchCode
  ].filter((part): part is string => Boolean(part))
  return parts.length ? parts.join(' / ') : '当前任务'
})

const assistProcessSwitchLabel = computed(
  () => execution.value?.processName || execution.value?.processCode || '当前工序'
)

const assistFillerSwitchLabel = computed(() => {
  const routeFillerName = readRouteQueryString(route.query.fillerName)
  const user = userStore.getUser || userStore.user
  return routeFillerName || user?.nickname || (user?.id ? `用户 ${user.id}` : '当前填写人')
})

const normalizeFillActionResultText = (value: string) => {
  const text = value.trim()
  return text || '--'
}

const resolveFillActionResultOrderText = () =>
  normalizeFillActionResultText(execution.value?.workOrderCode || execution.value?.executionCode || '')

const resolveFillActionResultProcessText = () =>
  normalizeFillActionResultText(execution.value?.processName || execution.value?.processCode || '')

const resolveFillActionResultFillerText = () => {
  const routeFillerName = readRouteQueryString(route.query.fillerName)
  const user = userStore.getUser || userStore.user
  return normalizeFillActionResultText(routeFillerName || user?.nickname || (user?.id ? String(user.id) : ''))
}

const submitSignatureUserName = computed(resolveFillActionResultFillerText)

const showFillActionResultDialog = (type: FillActionResultType) => {
  const result = FILL_ACTION_RESULT_MAP[type]
  fillActionResultDialog.statusText = result.statusText
  fillActionResultDialog.tone = result.tone
  fillActionResultDialog.orderText = resolveFillActionResultOrderText()
  fillActionResultDialog.processText = resolveFillActionResultProcessText()
  fillActionResultDialog.fillerText = resolveFillActionResultFillerText()
  fillActionResultDialogVisible.value = true
}

const resolveAssistFieldTypeLabel = (field: AssistFillField) => {
  const labels: Record<AssistFillField['componentKind'], string> = {
    text: '文本',
    textarea: '长文本',
    number: '数字',
    date: '日期',
    datetime: '日期时间',
    select: '选项',
    checkbox: '勾选',
    'choice-group': '选项组',
    signature: '签名',
    'upload-file': '附件',
    'upload-image': '图片',
    'upload-images': '多图片'
  }
  return labels[field.componentKind] || field.valueType
}

const resolveTemplateSnapshotField = (context: TemplateEditableCellContext) =>
  templateFieldByCell.value.get(context.fieldIdentity)

const isFieldInCurrentFillScope = (field: NormalizedSnapshotField) => {
  if (!isBatchSharedExecutionTask.value) {
    return true
  }
  if (hasGoldenFingerPermission.value) {
    return true
  }
  if (sharedFillScopeGateError.value) {
    return false
  }
  const sourceTableIndex = sharedExecutionSourceTableIndex.value
  if (sourceTableIndex === undefined) {
    return false
  }
  return sharedFillScopeParseResult.value.ranges.some(
    (range) =>
      range.sourceTableIndex === sourceTableIndex &&
      field.rowIndex >= range.startRow &&
      field.rowIndex <= range.endRow
  )
}

const isTemplateContextOutOfCurrentFillScope = (context: TemplateEditableCellContext) => {
  const field = resolveTemplateSnapshotField(context)
  return Boolean(field && !isFieldInCurrentFillScope(field))
}

const isTemplateContextReadonlyForCurrentTask = (context: TemplateEditableCellContext) => {
  const field = resolveTemplateSnapshotField(context)
  return isReadonly.value || !field || !isFieldInCurrentFillScope(field)
}

const resolveTemplateFieldValue = (context: TemplateEditableCellContext) => {
  const field = resolveTemplateSnapshotField(context)
  return field ? draftFieldValues.value[field.fieldIdentity] : null
}

const resolveTemplateNumberValue = (context: TemplateEditableCellContext) => {
  const value = resolveTemplateFieldValue(context)
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

const updateTemplateFieldValue = (
  context: TemplateEditableCellContext,
  value: DraftFieldValue
) => {
  const field = resolveTemplateSnapshotField(context)
  if (!field) return
  if (!isFieldInCurrentFillScope(field)) {
    message.error('当前单元格不在本工序填写范围内，不能修改。')
    return
  }
  draftFieldValues.value[field.fieldIdentity] = value
}

const resolveTemplateAttachmentValue = (context: TemplateEditableCellContext) => {
  const field = resolveTemplateSnapshotField(context)
  return field ? draftAttachmentValues.value[field.fieldIdentity] : ''
}

const updateTemplateAttachmentValue = (
  context: TemplateEditableCellContext,
  value: DraftAttachmentValue
) => {
  const field = resolveTemplateSnapshotField(context)
  if (!field) return
  if (!isFieldInCurrentFillScope(field)) {
    message.error('当前附件不在本工序填写范围内，不能修改。')
    return
  }
  draftAttachmentValues.value[field.fieldIdentity] = value
}

const resolveTemplateImageAttachmentValue = (context: TemplateEditableCellContext) => {
  const field = resolveTemplateSnapshotField(context)
  return field ? draftImageAttachmentValues.value[field.fieldIdentity] : ''
}

const updateTemplateImageAttachmentValue = (
  context: TemplateEditableCellContext,
  value: string
) => {
  const field = resolveTemplateSnapshotField(context)
  if (!field) return
  if (!isFieldInCurrentFillScope(field)) {
    message.error('当前图片附件不在本工序填写范围内，不能修改。')
    return
  }
  draftImageAttachmentValues.value[field.fieldIdentity] = value
}

const requireTemplateSnapshotField = (context: TemplateEditableCellContext) => {
  const field = resolveTemplateSnapshotField(context)
  if (!field) {
    throw new Error(`模板单元格 ${context.fieldIdentity} 缺少执行字段定义。`)
  }
  return field
}

const valuesAreEqual = (left: DraftFieldValue | undefined, right: DraftFieldValue | undefined) => {
  return JSON.stringify(left ?? null) === JSON.stringify(right ?? null)
}

const resolveTypedJsonValue = (
  value: DraftFieldValue | undefined,
  field: NormalizedSnapshotField
): EdhrFieldTypedJsonValue => {
  if (field.valueType === 'NULL') {
    return null
  }
  if (field.valueType === 'NUMBER') {
    const numericValue = Number(value)
    if (!Number.isFinite(numericValue)) {
      throw new Error(`${field.label} 必须是数字，不能保存字段审计。`)
    }
    return numericValue
  }
  if (field.valueType === 'BOOLEAN') {
    if (typeof value !== 'boolean') {
      throw new Error(`${field.label} 必须是布尔值，不能保存字段审计。`)
    }
    return value
  }
  if (field.valueType === 'DATE' || field.valueType === 'DATETIME') {
    const text = value == null ? '' : String(value).trim()
    return text ? text : null
  }
  if (field.valueType === 'JSON') {
    if (value != null && typeof value === 'object') {
      return value as Record<string, unknown> | unknown[]
    }
    throw new Error(`${field.label} 必须是 JSON 对象或数组，不能保存字段审计。`)
  }
  if (value == null) {
    return ''
  }
  return String(value)
}

const resolveDisplayValue = (value: EdhrFieldTypedJsonValue) => {
  if (value == null || value === '') {
    return '--'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

const resolveJsonPreview = (value: EdhrFieldTypedJsonValue) => {
  if (value === undefined) {
    return '--'
  }
  return JSON.stringify(value)
}

const formatFieldPosition = (row: Pick<PendingFieldChange, 'rowIndex' | 'columnIndex'>) => {
  const rowIndex = Number(row.rowIndex)
  const columnIndex = Number(row.columnIndex)
  if (!Number.isInteger(rowIndex) || !Number.isInteger(columnIndex)) {
    return '未定位到模板位置'
  }
  return `第 ${rowIndex + 1} 行 / 第 ${columnIndex + 1} 列`
}

const resolveExpectedOldTypedJsonValue = (
  field: NormalizedSnapshotField
): EdhrFieldTypedJsonValue => {
  const savedHash = baselineFieldValueHashes.value[field.fieldIdentity]
  if (savedHash) {
    return resolveTypedJsonValue(baselineFieldValues.value[field.fieldIdentity], field)
  }
  if (
    field.valueType === 'NUMBER' ||
    field.valueType === 'DATE' ||
    field.valueType === 'DATETIME' ||
    field.valueType === 'NULL' ||
    field.valueType === 'JSON'
  ) {
    return null
  }
  if (field.valueType === 'BOOLEAN') {
    return false
  }
  return resolveTypedJsonValue(field.defaultValue, field)
}

const readNumberConstraint = (field: NormalizedSnapshotField, key: string) => {
  const value = field.constraints[key]
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined
}

const readStringConstraint = (field: NormalizedSnapshotField, key: string) => {
  const value = field.constraints[key]
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

const countDecimalScale = (value: number) => {
  const text = String(value)
  if (!text.includes('.')) return 0
  return text.split('.')[1]?.length || 0
}

const formatToRegex = (format: string) => {
  const escaped = format.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return new RegExp(
    `^${escaped
      .replace(/yyyy/g, '\\d{4}')
      .replace(/MM/g, '\\d{2}')
      .replace(/dd/g, '\\d{2}')
      .replace(/HH/g, '\\d{2}')
      .replace(/mm/g, '\\d{2}')
      .replace(/ss/g, '\\d{2}')}$`
  )
}

const resolveRuleConstraintValidation = (
  field: NormalizedSnapshotField,
  newValueJson: EdhrFieldTypedJsonValue
) => {
  if (field.valueType === 'NUMBER') {
    if (typeof newValueJson !== 'number' || !Number.isFinite(newValueJson)) {
      return `${field.label} 必须是数字。`
    }
    const min = readNumberConstraint(field, 'min')
    if (min != null && newValueJson < min) return `${field.label} 不能小于 ${min}。`
    const max = readNumberConstraint(field, 'max')
    if (max != null && newValueJson > max) return `${field.label} 不能大于 ${max}。`
    const scale = readNumberConstraint(field, 'scale')
    if (scale != null && countDecimalScale(newValueJson) > scale) {
      return `${field.label} 小数位不能超过 ${scale}。`
    }
    return ''
  }
  if (field.valueType === 'STRING') {
    const text = String(newValueJson ?? '')
    const minLength = readNumberConstraint(field, 'minLength')
    if (minLength != null && text.length < minLength) return `${field.label} 长度不能小于 ${minLength}。`
    const maxLength = readNumberConstraint(field, 'maxLength')
    if (maxLength != null && text.length > maxLength) return `${field.label} 长度不能超过 ${maxLength}。`
    return ''
  }
  if (field.valueType === 'DATE' || field.valueType === 'DATETIME') {
    const text = String(newValueJson ?? '')
    const fallbackFormat = field.valueType === 'DATE' ? 'yyyy-MM-dd' : 'yyyy-MM-dd HH:mm:ss'
    const format = readStringConstraint(field, 'format') || fallbackFormat
    if (!formatToRegex(format).test(text)) return `${field.label} 格式必须为 ${format}。`
  }
  if (field.valueType === 'BOOLEAN' && typeof newValueJson !== 'boolean') {
    return `${field.label} 必须是勾选值。`
  }
  return ''
}

const resolvePendingChangeValidation = (
  field: NormalizedSnapshotField,
  newValueJson: EdhrFieldTypedJsonValue
) => {
  if (!field.fieldKey) {
    return '字段缺少 fieldKey，不能保存字段审计。'
  }
  if (!field.fieldPath) {
    return '字段缺少 fieldPath，不能保存字段审计。'
  }
  if (field.readonly) {
    return '只读字段不能保存变更。'
  }
  if (isFieldRequiredForCurrentMode(field) && (newValueJson == null || newValueJson === '')) {
    return '必填字段不能为空。'
  }
  if (!isRecordbookUnrestrictedMode.value) {
    const constraintValidation = resolveRuleConstraintValidation(field, newValueJson)
    if (constraintValidation) {
      return constraintValidation
    }
  }
  return ''
}

const pendingFieldChanges = computed<PendingFieldChange[]>(() => {
  return snapshotFields.value
    .filter(isFieldInCurrentFillScope)
    .filter(
      (field) =>
        !isAttachmentComponentKind(field.componentKind) &&
        !valuesAreEqual(
          draftFieldValues.value[field.fieldIdentity],
          baselineFieldValues.value[field.fieldIdentity]
        )
    )
    .map((field) => {
      try {
        const oldValueJson = resolveExpectedOldTypedJsonValue(field)
        const newValueJson = resolveTypedJsonValue(
          draftFieldValues.value[field.fieldIdentity],
          field
        )
        const newValueDisplay = resolveDisplayValue(newValueJson)
        const expectedOldValueHash = baselineFieldValueHashes.value[field.fieldIdentity]
        return {
          fieldIdentity: field.fieldIdentity,
          fieldPath: field.fieldPath,
          fieldKey: field.fieldKey,
          rowIndex: field.rowIndex,
          columnIndex: field.columnIndex,
          valueType: field.valueType,
          newValueJson,
          newValueDisplay,
          expectedOldValueJson: oldValueJson,
          expectedOldValueHash,
          fieldLabel: field.label,
          oldValueDisplay: resolveDisplayValue(oldValueJson),
          oldValueJson,
          validationMessage: resolvePendingChangeValidation(field, newValueJson)
        }
      } catch (error) {
        return {
          fieldIdentity: field.fieldIdentity,
          fieldPath: field.fieldPath,
          fieldKey: field.fieldKey,
          rowIndex: field.rowIndex,
          columnIndex: field.columnIndex,
          valueType: field.valueType,
          newValueJson: '',
          newValueDisplay: '--',
          expectedOldValueJson: '',
          expectedOldValueHash: baselineFieldValueHashes.value[field.fieldIdentity],
          fieldLabel: field.label,
          oldValueDisplay: '--',
          oldValueJson: '',
          validationMessage: resolveErrorMessage(error, '字段值类型不符合审计合同。')
        }
      }
    })
})

const formRenderError = computed(() => {
  if (!execution.value) {
    return ''
  }
  if (recordbookGlobalDisabledNotice.value) {
    return recordbookGlobalDisabledNotice.value
  }
  if (sharedFillScopeGateError.value) {
    return sharedFillScopeGateError.value
  }
  if (parsedSnapshot.value.error) {
    return parsedSnapshot.value.error
  }
  if (!Array.isArray(parsedSnapshot.value.parsed?.fields)) {
    return 'eDHR 执行快照缺少 fields 数组，无法渲染可编辑表单。'
  }
  if (snapshotFieldContractErrors.value.length > 0) {
    return snapshotFieldContractErrors.value[0]
  }
  if (snapshotFields.value.length === 0) {
    return 'eDHR 执行快照 fields 缺少有效 rowIndex / columnIndex，无法生成最小表单。'
  }
  return ''
})

const resolveAttachmentDraftUrls = (field: NormalizedSnapshotField) => {
  const rawValue =
    field.componentKind === 'upload-image'
      ? draftImageAttachmentValues.value[field.fieldIdentity]
      : draftAttachmentValues.value[field.fieldIdentity]
  if (Array.isArray(rawValue)) {
    return rawValue.map((item) => String(item || '').trim()).filter(Boolean)
  }
  const value = String(rawValue || '').trim()
  if (!value) {
    return []
  }
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

const resolveAttachmentType = (componentKind: NormalizedSnapshotField['componentKind']) => {
  if (componentKind === 'upload-file') {
    return 'FILE'
  }
  if (componentKind === 'upload-image' || componentKind === 'upload-images') {
    return 'IMAGE'
  }
  throw new Error(`不支持的附件控件类型：${componentKind}`)
}

const resolveAttachmentDraftValidation = (field: NormalizedSnapshotField, fileUrl: string) => {
  if (!workTaskId.value) {
    return '工作任务编号缺失，不能保存附件审计。'
  }
  if (!field.fieldKey || !field.fieldPath) {
    return '附件字段缺少 fieldKey 或 fieldPath，不能保存附件审计。'
  }
  if (!fileUrl) {
    return '附件 URL 不能为空。'
  }
  const metadata = attachmentMetadataByUrl.value[fileUrl]
  if (
    !metadata?.uploadToken ||
    !metadata.fileId ||
    !metadata.storageConfigId ||
    !metadata.storagePath ||
    !metadata.fileName ||
    !metadata.contentType ||
    !metadata.fileSize ||
    !metadata.sha256 ||
    !metadata.storageRetentionJson ||
    !metadata.storageRetentionHash
  ) {
    return '附件元数据不完整：必须通过 eDHR 专用 prepareUpload 获取 fileId、storageConfigId、storagePath、fileName、contentType、fileSize、sha256、storageRetentionJson 与 storageRetentionHash。'
  }
  return ''
}

const pendingAttachmentChanges = computed<PendingAttachmentChange[]>(() => {
  return snapshotFields.value
    .filter(isFieldInCurrentFillScope)
    .filter((field) => isAttachmentComponentKind(field.componentKind))
    .flatMap((field) =>
      resolveAttachmentDraftUrls(field).map((fileUrl, index) => {
        const metadata = attachmentMetadataByUrl.value[fileUrl]
        return {
          fieldIdentity: `${field.fieldIdentity}::${index}`,
          workTaskId: workTaskId.value,
          fieldPath: field.fieldPath,
          fieldKey: field.fieldKey,
          rowIndex: field.rowIndex,
          columnIndex: field.columnIndex,
          attachmentType: resolveAttachmentType(field.componentKind),
          attachmentAction: 'ADD',
          attachmentGroupKey: field.attachmentRule?.groupKey || `${field.fieldKey || 'attachment'}-${index + 1}`,
          uploadToken: metadata?.uploadToken,
          fileId: metadata?.fileId,
          storageConfigId: metadata?.storageConfigId,
          storagePath: metadata?.storagePath,
          fileUrl,
          fileName: metadata?.fileName,
          contentType: metadata?.contentType,
          fileSize: metadata?.fileSize,
          sha256: metadata?.sha256,
          storageRetentionJson: metadata?.storageRetentionJson,
          storageRetentionHash: metadata?.storageRetentionHash,
          fieldLabel: field.label,
          validationMessage: resolveAttachmentDraftValidation(field, fileUrl)
        }
      })
    )
})

const requiredAttachmentRequirementFields = computed(() => {
  if (formRenderError.value || isRecordbookUnrestrictedMode.value) {
    return [] as NormalizedSnapshotField[]
  }
  return snapshotFields.value
    .filter(isFieldInCurrentFillScope)
    .filter((field) => isAttachmentComponentKind(field.componentKind))
    .filter((field) => Boolean(field.attachmentRule))
    .filter((field) => Boolean(field.attachmentRule?.required) || Number(field.attachmentRule?.minCount || 0) > 0)
})

const matchesAttachmentRequirement = (
  field: NormalizedSnapshotField,
  summary: NonNullable<ProFeedbackEdhrExecutionVO['attachmentSummaries']>[number]
) => {
  if (field.attachmentRule?.groupKey && summary.attachmentGroupKey === field.attachmentRule.groupKey) {
    return true
  }
  if (summary.fieldKey && summary.fieldKey === field.fieldKey && summary.fieldPath === field.fieldPath) {
    return true
  }
  return summary.rowIndex === field.rowIndex && summary.columnIndex === field.columnIndex
}

const resolveAttachmentSatisfiedCount = (field: NormalizedSnapshotField) => {
  const matchedAttachmentKeys = new Set<string>()
  for (const summary of execution.value?.attachmentSummaries || []) {
    if (summary.attachmentAction === 'VOID' || !matchesAttachmentRequirement(field, summary)) {
      continue
    }
    const attachmentKey =
      summary.attachmentHash ||
      summary.fileUrl ||
      summary.fileId ||
      summary.id ||
      `${summary.rowIndex}:${summary.columnIndex}:${summary.versionNo}`
    matchedAttachmentKeys.add(String(attachmentKey))
  }
  for (const fileUrl of resolveAttachmentDraftUrls(field)) {
    matchedAttachmentKeys.add(`pending:${fileUrl}`)
  }
  return matchedAttachmentKeys.size
}

const attachmentRequirementCompletion = computed(() => {
  const missingFields: string[] = []
  let satisfied = 0
  for (const field of requiredAttachmentRequirementFields.value) {
    const minCount = field.attachmentRule?.minCount || 1
    const currentCount = resolveAttachmentSatisfiedCount(field)
    if (currentCount >= minCount) {
      satisfied += 1
      continue
    }
    missingFields.push(`${field.label}（需 ${minCount} 个，已有 ${currentCount} 个）`)
  }
  return {
    total: requiredAttachmentRequirementFields.value.length,
    satisfied,
    missingFields
  }
})

const isRequiredTypedValueMissing = (value: EdhrFieldTypedJsonValue) => {
  if (value == null) {
    return true
  }
  if (typeof value === 'string') {
    return value.trim() === ''
  }
  if (Array.isArray(value)) {
    return value.length === 0
  }
  if (typeof value === 'object') {
    return Object.keys(value).length === 0
  }
  return false
}

const formatRequiredFieldLocation = (field: NormalizedSnapshotField) => {
  return `${field.label}（第 ${field.rowIndex + 1} 行第 ${field.columnIndex + 1} 列）`
}

const isFieldRequiredForCurrentMode = (field: Pick<AssistFillField, 'required'>) => {
  if (isRecordbookUnrestrictedMode.value) return false
  return field.required
}

const requiredEditableFields = computed(() => {
  if (formRenderError.value || isRecordbookUnrestrictedMode.value) {
    return [] as NormalizedSnapshotField[]
  }
  return snapshotFields.value
    .filter(isFieldInCurrentFillScope)
    .filter((field) => isFieldRequiredForCurrentMode(field))
    .filter((field) => !field.readonly)
    .filter((field) => field.componentKind !== 'signature')
    .filter((field) => !isAttachmentComponentKind(field.componentKind))
})

const missingRequiredFields = computed(() => {
  return requiredEditableFields.value
    .filter((field) => {
      const currentValue = draftFieldValues.value[field.fieldIdentity]
      try {
        return isRequiredTypedValueMissing(resolveTypedJsonValue(currentValue, field))
      } catch {
        return true
      }
    })
    .map(formatRequiredFieldLocation)
})

const missingRequiredFieldsSubmitError = computed(() => {
  if (missingRequiredFields.value.length === 0) {
    return ''
  }
  const shownFields = missingRequiredFields.value.slice(0, 5).join('、')
  return `eDHR 必填字段未填写：${shownFields}`
})

const formSubmitGateError = computed(() => {
  if (!execution.value) {
    return ''
  }
  if (execution.value.activeRevisionFlag === false) {
    return '当前执行记录不是活动修订版本，不能提交。'
  }
  if (parsedSnapshot.value.error) {
    return parsedSnapshot.value.error
  }
  if (!hasGoldenFingerPermission.value && attachmentRequirementCompletion.value.missingFields.length > 0) {
    const shownFields = attachmentRequirementCompletion.value.missingFields.slice(0, 5).join('、')
    return `eDHR 附件要求未满足：${shownFields}`
  }
  if (!hasGoldenFingerPermission.value && missingRequiredFieldsSubmitError.value) {
    return missingRequiredFieldsSubmitError.value
  }
  return ''
})

const isAssistAttachmentIncomplete = (field: AssistFillField) => {
  if (isAssistChoiceGroupField(field)) {
    return false
  }
  if (!isAttachmentComponentKind(field.componentKind)) {
    return false
  }
  if (!field.attachmentRule?.required && Number(field.attachmentRule?.minCount || 0) <= 0) {
    return false
  }
  return resolveAttachmentSatisfiedCount(field) < (field.attachmentRule?.minCount || 1)
}

const isAssistTypedFieldMissing = (field: AssistFillField) => {
  if (!isFieldRequiredForCurrentMode(field) || isAttachmentComponentKind(field.componentKind)) {
    return false
  }
  if (isAssistChoiceGroupField(field)) {
    return !resolveAssistChoiceGroupValue(field)
  }
  try {
    return isRequiredTypedValueMissing(
      resolveTypedJsonValue(draftFieldValues.value[field.fieldIdentity], field)
    )
  } catch {
    return true
  }
}

const isAssistSignatureMissing = (field: AssistFillField) =>
  !isAssistChoiceGroupField(field) &&
  field.componentKind === 'signature' &&
  !findSignatureCellRecord(field)

const isAssistFieldIncomplete = (field: AssistFillField) =>
  isAssistSignatureMissing(field) || isAssistAttachmentIncomplete(field) || isAssistTypedFieldMissing(field)

const resolveAssistFieldValidationMessage = (field: AssistFillField) => {
  if (isAssistChoiceGroupField(field)) {
    if (isAssistTypedFieldMissing(field)) {
      return ''
    }
    return (
      field.fields
        .map((childField) => resolveAssistFieldValidationMessage(childField))
        .find((message) => Boolean(message)) || ''
    )
  }
  if (field.componentKind === 'signature') {
    return ''
  }
  if (isAttachmentComponentKind(field.componentKind)) {
    const currentChange = pendingAttachmentChanges.value.find(
      (change) => change.fieldIdentity.startsWith(`${field.fieldIdentity}::`) && change.validationMessage
    )
    return currentChange?.validationMessage || ''
  }
  if (isAssistTypedFieldMissing(field)) {
    return ''
  }
  const currentChange = pendingFieldChanges.value.find(
    (change) => change.fieldIdentity === field.fieldIdentity && change.validationMessage
  )
  if (currentChange?.validationMessage) {
    return currentChange.validationMessage
  }
  try {
    const currentValueJson = resolveTypedJsonValue(draftFieldValues.value[field.fieldIdentity], field)
    if (isRequiredTypedValueMissing(currentValueJson)) {
      return ''
    }
    return resolvePendingChangeValidation(field, currentValueJson)
  } catch (error) {
    return resolveErrorMessage(error, '字段值类型不符合审计合同。')
  }
}

const isAssistFieldComplete = (field: AssistFillField) =>
  !isAssistFieldIncomplete(field) && !resolveAssistFieldValidationMessage(field)

const resolveAssistIncompleteReason = (field: AssistFillField) => {
  const validationMessage = resolveAssistFieldValidationMessage(field)
  if (validationMessage) return '异常'
  if (isAssistSignatureMissing(field)) return '未签名'
  if (isAssistAttachmentIncomplete(field)) return '附件未满足'
  if (isAssistTypedFieldMissing(field)) return '未填写'
  return ''
}

const assistIncompleteItems = computed<AssistIncompleteItem[]>(() =>
  assistFillFields.value
    .map((field) => ({
      fieldIdentity: field.fieldIdentity,
      label: field.label,
      reason: resolveAssistIncompleteReason(field)
    }))
    .filter((item) => Boolean(item.reason))
)

const assistMissingFieldCount = computed(() => assistIncompleteItems.value.length)

const resolveAssistFieldStatusLabel = (field: AssistFillField) => {
  if (resolveAssistFieldValidationMessage(field)) return '异常'
  if (isAssistSignatureMissing(field)) return '未签'
  if (isAssistFieldIncomplete(field)) return '未填'
  if (isAssistChoiceGroupField(field)) return '已选'
  return field.componentKind === 'signature' ? '已签' : '已填'
}

const resolveAssistFieldStatusTagType = (field: AssistFillField) => {
  if (resolveAssistFieldValidationMessage(field)) return 'danger'
  if (isAssistFieldIncomplete(field)) return 'warning'
  return 'success'
}

const isInactiveRevisionDraft = computed(
  () =>
    execution.value?.status === EDHR_EXECUTION_STATUS.DRAFT &&
    execution.value?.activeRevisionFlag === false
)
const isPreReleaseEditable = computed(() => execution.value?.preReleaseEditable === true)
const isReadonly = computed(() => {
  if (!hasFillTaskContext.value || isInactiveRevisionDraft.value) {
    return true
  }
  if (execution.value?.status === EDHR_EXECUTION_STATUS.DRAFT || isPreReleaseEditable.value) {
    return false
  }
  return true
})
const executionStatusText = computed(() => {
  if (execution.value?.status === EDHR_EXECUTION_STATUS.SUBMITTED) {
    return '待审批（审批关闭后才可归档）'
  }
  if (execution.value?.status === EDHR_EXECUTION_STATUS.REJECTED) {
    return '已驳回（不可归档）'
  }
  if (execution.value?.status === EDHR_EXECUTION_STATUS.APPROVED) {
    return '已关闭（可按门槛归档）'
  }
  if (execution.value?.status === EDHR_EXECUTION_STATUS.FILL_COMPLETED) {
    return '填写完成（已提交电子签名）'
  }
  return '草稿（可编辑）'
})
const readonlySubmitReason = computed(() => {
  if (isPreReleaseEditable.value) {
    return '关闭前可修改，重新提交将更新提交签名证据。'
  }
  if (isInactiveRevisionDraft.value) {
    return '当前执行记录不是活动修订版本，不能提交。'
  }
  if (execution.value?.status === EDHR_EXECUTION_STATUS.REJECTED) {
    return '被驳回原版本已锁定，请从返工任务进入新修订草稿。'
  }
  return '当前执行记录已提交，不能重复提交。'
})
const revisionLockNotice = computed(() => {
  if (!execution.value) {
    return ''
  }
  if (execution.value.status === EDHR_EXECUTION_STATUS.REJECTED) {
    return '被驳回原版本已锁定；返工修改只能在系统创建的新修订草稿中完成。'
  }
  if (isInactiveRevisionDraft.value) {
    return '当前草稿不是活动修订版本，已禁止保存和提交。'
  }
  if (execution.value.sourceRejectedExecutionId) {
    return '当前记录是驳回后创建的受控修订草稿。'
  }
  return ''
})
const revisionLockNoticeType = computed(() =>
  execution.value?.status === EDHR_EXECUTION_STATUS.REJECTED || isInactiveRevisionDraft.value
    ? 'warning'
    : 'info'
)
const trackingReadonlyFormViewModel = computed(() => {
  if (!execution.value) {
    return undefined
  }
  return {
    sheetLayoutJson: execution.value.sheetLayoutJson,
    executionSnapshotJson: execution.value.executionSnapshotJson,
    cellValuesJson: JSON.stringify(execution.value.cellValues),
    remark: execution.value.remark
  }
})
const trackingReadonlyFormRenderError = computed(() => {
  if (!isTrackingReadonlyMode.value || !execution.value) {
    return ''
  }
  if (typeof execution.value.sheetLayoutJson !== 'string' || !execution.value.sheetLayoutJson.trim()) {
    return '缺少电子批记录模板布局，无法按原模板展示填写结果。'
  }
  if (typeof execution.value.executionSnapshotJson !== 'string' || !execution.value.executionSnapshotJson.trim()) {
    return '缺少 executionSnapshotJson，无法按原模板展示填写结果。'
  }
  if (!Array.isArray(execution.value.cellValues)) {
    return '缺少电子批记录单元格填写值，无法按原模板展示填写结果。'
  }
  try {
    JSON.stringify(execution.value.cellValues)
  } catch (error) {
    return resolveErrorMessage(error, '单元格值序列化失败，无法按原模板展示填写结果。')
  }
  return ''
})
const formatTrackingReadonlyDate = (value?: string | number | Date) => {
  if (!value) {
    return '--'
  }
  return formatEdhrDateTime(value)
}

const hasPendingFieldChanges = computed(() => pendingFieldChanges.value.length > 0)
const hasPendingAttachmentChanges = computed(() => pendingAttachmentChanges.value.length > 0)
const hasPendingFieldAuditChanges = computed(
  () => hasPendingFieldChanges.value || hasPendingAttachmentChanges.value
)
const resolveFieldAuditDraftReasonCategory = () =>
  fieldAuditReasonForm.reasonCategory || DEFAULT_FIELD_AUDIT_DRAFT_REASON_CATEGORY
const resolveFieldAuditDraftReasonText = () =>
  fieldAuditReasonForm.reasonText.trim() || DEFAULT_FIELD_AUDIT_DRAFT_REASON_TEXT
const fieldAuditBaseReady = computed(() => {
  return Boolean(
    execution.value?.cellValuesHash &&
      typeof execution.value.fieldAuditRevision === 'number' &&
      execution.value?.fieldAuditHeadHash
  )
})
const fieldAuditOpenGateError = computed(() => {
  if (!execution.value?.id) {
    return '当前执行记录不存在，无法保存字段变更。'
  }
  if (workTaskId.value === undefined) {
    return '工作任务编号缺失，不能保存字段变更。'
  }
  if (isInactiveRevisionDraft.value) {
    return '当前执行记录不是活动修订版本，不能保存字段变更。'
  }
  if (isReadonly.value) {
    return '当前状态不允许编辑字段，只有草稿或关闭前可修改的已提交普通表单可以保存字段变更。'
  }
  if (!hasFieldAuditUpdatePermission.value) {
    return '当前账号没有字段审计保存权限。'
  }
  if (formRenderError.value) {
    return formRenderError.value
  }
  if (!hasPendingFieldAuditChanges.value) {
    return '没有待保存字段变更。'
  }
  if (!fieldAuditBaseReady.value) {
    return '缺少 baseCellValuesHash、baseFieldAuditRevision 或 baseFieldAuditHeadHash，不能保存字段审计。'
  }
  const invalidChange = pendingFieldChanges.value.find((change) => change.validationMessage)
  if (invalidChange) {
    return invalidChange.validationMessage
  }
  const invalidAttachmentChange = pendingAttachmentChanges.value.find(
    (change) => change.validationMessage
  )
  if (invalidAttachmentChange) {
    return invalidAttachmentChange.validationMessage
  }
  return ''
})
const fieldAuditSaveGateError = computed(() => fieldAuditOpenGateError.value)
const canSaveFieldAuditChanges = computed(() => !fieldAuditSaveGateError.value)

const normalizeSignatureActionType = (value?: string) => (value || '').trim().toUpperCase()

const resolveSignatureCellActionLabel = (field: NormalizedSnapshotField) => {
  const actionType = normalizeSignatureActionType(field.signatureActionType)
  return field.signatureLabel || SIGNATURE_ACTION_LABELS[actionType] || '电子签名'
}

const toSignatureTime = (value?: string) => {
  return toEdhrDateTime(value)?.getTime() ?? 0
}

const formatSignatureCellTime = (value?: string) => {
  return formatEdhrDateTime(value)
}

const findSignatureCellRecord = (field: NormalizedSnapshotField) => {
  const actionType = normalizeSignatureActionType(field.signatureActionType)
  if (!actionType) return undefined
  const signatureCellKey = field.signatureCellKey
  const exactMatches = signatureRows.value.filter((record) => {
    if (record.actionType !== actionType) return false
    if (signatureCellKey && record.signatureCellKey === signatureCellKey) return true
    return (
      record.signatureRowIndex === field.rowIndex &&
      record.signatureColumnIndex === field.columnIndex
    )
  })
  const candidates = exactMatches.length
    ? exactMatches
    : signatureRows.value.filter((record) => record.actionType === actionType)
  return [...candidates].sort(
    (left, right) =>
      toSignatureTime(left.signatureDisplayAt || left.selectedSignedAt || left.signedAt) -
      toSignatureTime(right.signatureDisplayAt || right.selectedSignedAt || right.signedAt)
  )[candidates.length - 1]
}

const resolveSignatureCellDisplay = (field: NormalizedSnapshotField) => {
  const signature = findSignatureCellRecord(field)
  if (!signature) return '未签名'
  const actor = signature.actorName || signature.actorNickname || String(signature.actorId || '未知签名人')
  const signedAt = formatSignatureCellTime(
    signature.signatureDisplayAt || signature.selectedSignedAt || signature.signedAt
  )
  return `${actor} ${signedAt}`
}

const resolveSignatureCellActionDisabledReason = (field: NormalizedSnapshotField) => {
  const actionType = normalizeSignatureActionType(field.signatureActionType)
  if (!actionType) {
    return '签名格缺少签名动作配置，不能发起电子签名。'
  }
  switch (actionType) {
    case 'FIELD_CHANGE':
      return fieldAuditOpenGateError.value
    case 'FORM_REVIEW':
      return formReviewSignGateError.value
    case 'SUBMIT':
      return '提交签名必须通过页面“提交执行”流程完成。'
    case 'APPROVE':
      return '审批签名必须通过审批任务页面完成。'
    case 'REJECT':
      return '驳回签名必须通过审批任务页面完成。'
    case 'ARCHIVE_SEAL':
      return '归档封存签名必须通过归档生成流程完成。'
    default:
      return `签名动作 ${actionType} 当前页面不支持直接发起。`
  }
}

const handleSignatureCellAction = (field: NormalizedSnapshotField) => {
  const disabledReason = resolveSignatureCellActionDisabledReason(field)
  if (disabledReason) {
    message.error(disabledReason)
    return
  }
  switch (normalizeSignatureActionType(field.signatureActionType)) {
    case 'FIELD_CHANGE':
      handleSaveFieldAuditChanges()
      return
    case 'FORM_REVIEW':
      openFormReviewSignDialog()
      return
    default:
      message.error('该签名动作必须通过对应业务流程完成。')
  }
}

const isFormReviewSignStatusAllowed = computed(() => {
  return (
    typeof execution.value?.status === 'number' &&
    FORM_REVIEW_ALLOWED_STATUSES.includes(execution.value.status)
  )
})
const formReviewSignGateError = computed(() => {
  if (!execution.value?.id) {
    return '当前执行记录不存在，无法复核签名。'
  }
  if (workTaskId.value === undefined) {
    return '工作任务编号缺失，不能保存表单复核签名。'
  }
  if (!hasExecutionUpdatePermission.value) {
    return '当前账号没有 eDHR 执行更新权限。'
  }
  if (!isFormReviewSignStatusAllowed.value) {
    return '当前状态不允许追加表单复核签名。'
  }
  if (formSubmitGateError.value) {
    return formSubmitGateError.value
  }
  if (hasPendingFieldChanges.value) {
    return '存在未保存字段变更，请先完成字段审计签名保存后再复核签名。'
  }
  if (!fieldAuditBaseReady.value) {
    return '缺少 cellValuesHash、fieldAuditRevision 或 fieldAuditHeadHash，不能复核签名。'
  }
  return ''
})
const canOpenFormReviewSignDialog = computed(() => !formReviewSignGateError.value)
const showFormReviewSignAction = computed(() => {
  return Boolean(execution.value?.id) && hasExecutionUpdatePermission.value && isFormReviewSignStatusAllowed.value
})

const semanticSummary = computed(() => {
  const currentExecution = execution.value
  const resolveSemanticValue = (
    name?: string,
    code?: string,
    id?: string | number,
    fallbackLabel?: string
  ) => {
    if (typeof name === 'string' && name.trim()) {
      return name.trim()
    }
    if (typeof code === 'string' && code.trim()) {
      return code.trim()
    }
    return id == null || id === '' ? '--' : `${fallbackLabel || '记录'}#${id}`
  }

  return {
    route: resolveSemanticValue(
      currentExecution?.routeName,
      currentExecution?.routeCode,
      currentExecution?.routeId,
      '路线'
    ),
    process: resolveSemanticValue(
      currentExecution?.processName,
      currentExecution?.processCode,
      currentExecution?.processId,
      '工序'
    ),
    workstation: resolveSemanticValue(
      currentExecution?.workstationName,
      currentExecution?.workstationCode,
      currentExecution?.workstationId,
      '工作站'
    ),
    report: resolveSemanticValue(
      currentExecution?.batchRecordReportName,
      currentExecution?.batchRecordReportCode,
      currentExecution?.batchRecordReportId,
      '报表'
    )
  }
})

const executionPageTitle = computed(() => {
  if (isTrackingReadonlyMode.value) return 'eDHR 追踪详情'
  const reportName =
    semanticSummary.value.report && semanticSummary.value.report !== '--'
      ? semanticSummary.value.report
      : '主生产表'
  return `${reportName}填写`
})

const executionPageSubtitle = computed(() =>
  isTrackingReadonlyMode.value
    ? '展示执行参数、原始表单和追踪时间线'
    : '填写当前工序表单，保存字段变更后提交执行'
)

const currentBatchExecutionId = computed(() => readRouteQueryString(route.query.batchExecutionId))
const backToBatchLabel = computed(() =>
  currentBatchExecutionId.value ? '返回批次详情' : '返回批次执行'
)

const buildCellValueKey = (rowIndex: number, columnIndex: number) => `${rowIndex}:${columnIndex}`

const cellLinkPrefillByCell = computed(() => {
  const map = new Map<string, BatchRecordCellLinkPrefillItemVO>()
  cellLinkPrefills.value.forEach((item) => {
    map.set(buildCellValueKey(item.targetRowIndex, item.targetColumnIndex), item)
  })
  return map
})

const cellLinkPrefillNotice = computed(() => {
  const count = cellLinkPrefills.value.length
  return count > 0
    ? `已根据跨表单链接预填 ${count} 个单元格，请填写变更原因并完成字段审计签名保存。`
    : ''
})

const cellLinkPrefillConflictNotice = computed(() => {
  const count = cellLinkConflicts.value.length
  return count > 0 ? `有 ${count} 条跨表单链接未带入，请检查源值为空或目标已有人工值。` : ''
})

const resolveCellLinkPrefill = (context: TemplateEditableCellContext) => {
  const field = resolveTemplateSnapshotField(context)
  return field ? cellLinkPrefillByCell.value.get(buildCellValueKey(field.rowIndex, field.columnIndex)) : undefined
}

const formatCellLinkPrefillSource = (item?: BatchRecordCellLinkPrefillItemVO) => {
  if (!item) {
    return ''
  }
  if (item.sourceType === 'PRODUCTION_WORK_ORDER') {
    const sourceField = item.sourceFieldName || item.sourceLabel || item.sourceFieldCode || ''
    return sourceField ? `生产工单字段 / ${sourceField}` : '生产工单字段'
  }
  const sourceName = item.sourceReportName || '来源表单'
  const sourceCell = item.sourceLabel || item.sourceCellKey || ''
  return sourceCell ? `${sourceName} / ${sourceCell}` : sourceName
}

const normalizeCellLinkPrefillDraftValue = (
  item: BatchRecordCellLinkPrefillItemVO,
  field: NormalizedSnapshotField
): DraftFieldValue => {
  if (item.value == null) {
    return null
  }
  if (isSingleChoiceCheckboxField(field)) {
    return String(item.value)
  }
  if (field.componentKind === 'checkbox') {
    return String(item.value).toLowerCase() === 'true'
  }
  if (field.componentKind === 'number') {
    const numericValue = Number(item.value)
    if (!Number.isFinite(numericValue)) {
      throw new Error(
        `跨表单链接规则 ${item.ruleId || '--'} 带入 ${field.label} 时返回非数字值，不能预填。`
      )
    }
    return numericValue
  }
  if (field.valueType === 'DATE' || field.valueType === 'DATETIME') {
    const text = String(item.value).trim()
    return text ? text : null
  }
  return String(item.value)
}

const hydrateStoredDraftValue = (
  value: unknown,
  field: NormalizedSnapshotField
): DraftFieldValue => {
  if (isSingleChoiceCheckboxField(field)) {
    return value == null ? '' : String(value)
  }
  if (field.componentKind === 'checkbox') {
    return String(value).toLowerCase() === 'true'
  }
  if (field.componentKind === 'number') {
    return Number.isFinite(Number(value)) ? Number(value) : field.defaultValue
  }
  return value == null ? '' : String(value)
}

const hydrateDraftState = (
  detail: ProFeedbackEdhrExecutionVO,
  prefills: BatchRecordCellLinkPrefillItemVO[] = [],
  conflicts: BatchRecordCellLinkPrefillItemVO[] = []
) => {
  const cellValueMap = new Map(
    (detail.cellValues || []).map((cellValue) => [
      buildCellValueKey(cellValue.rowIndex, cellValue.columnIndex),
      {
        value: cellValue.value,
        valueHash: cellValue.valueHash
      }
    ])
  )
  const prefillMap = new Map(
    prefills
      .filter((item) => item.value != null)
      .map((item) => [buildCellValueKey(item.targetRowIndex, item.targetColumnIndex), item])
  )
  const appliedPrefills: BatchRecordCellLinkPrefillItemVO[] = []
  const nextDraftValues: Record<string, DraftFieldValue> = {}
  const nextBaselineValues: Record<string, DraftFieldValue> = {}
  const nextDraftAttachmentValues: Record<string, DraftAttachmentValue> = {}
  const nextDraftImageAttachmentValues: Record<string, string> = {}
  const nextFieldHashes: Record<string, string> = {}
  for (const field of snapshotFields.value) {
    const cellKey = buildCellValueKey(field.rowIndex, field.columnIndex)
    if (isAttachmentComponentKind(field.componentKind)) {
      if (field.componentKind === 'upload-image') {
        nextDraftImageAttachmentValues[field.fieldIdentity] = ''
      } else {
        nextDraftAttachmentValues[field.fieldIdentity] =
          field.componentKind === 'upload-images' ? [] : ''
      }
      nextDraftValues[field.fieldIdentity] = field.defaultValue
      nextBaselineValues[field.fieldIdentity] = field.defaultValue
      continue
    }
    const storedValue = cellValueMap.get(cellKey)
    if (storedValue?.value != null) {
      const hydratedStoredValue = hydrateStoredDraftValue(storedValue.value, field)
      nextDraftValues[field.fieldIdentity] = hydratedStoredValue
      nextBaselineValues[field.fieldIdentity] = hydratedStoredValue
      if (storedValue.valueHash) {
        nextFieldHashes[field.fieldIdentity] = storedValue.valueHash
      }
      continue
    }
    nextBaselineValues[field.fieldIdentity] = field.defaultValue
    const cellLinkPrefill = prefillMap.get(cellKey)
    if (cellLinkPrefill && !field.readonly) {
      nextDraftValues[field.fieldIdentity] = normalizeCellLinkPrefillDraftValue(cellLinkPrefill, field)
      appliedPrefills.push(cellLinkPrefill)
      continue
    }
    nextDraftValues[field.fieldIdentity] = field.defaultValue
  }
  draftFieldValues.value = nextDraftValues
  draftAttachmentValues.value = nextDraftAttachmentValues
  draftImageAttachmentValues.value = nextDraftImageAttachmentValues
  attachmentMetadataByUrl.value = {}
  baselineFieldValues.value = nextBaselineValues
  baselineFieldValueHashes.value = nextFieldHashes
  cellLinkPrefills.value = appliedPrefills
  cellLinkConflicts.value = conflicts
  draftRemark.value = detail.remark || ''
  fieldAuditSaveError.value = ''
  fieldAuditLastResult.value = undefined
  fieldAuditIdempotencyKey.value = ''
}

const resolveArchiveStatusLabel = (archiveStatus?: string) => {
  if (!archiveStatus) {
    return '未归档'
  }
  return ARCHIVE_STATUS_LABEL_MAP[archiveStatus] || archiveStatus
}

const resolveArchiveStatusType = (archiveStatus?: string) => {
  if (archiveStatus === EDHR_EXECUTION_ARCHIVE_STATUS_SEALED) {
    return 'success'
  }
  if (archiveStatus === EDHR_EXECUTION_ARCHIVE_STATUS_FAILED) {
    return 'danger'
  }
  if (archiveStatus === EDHR_EXECUTION_ARCHIVE_STATUS_GENERATING) {
    return 'warning'
  }
  return 'info'
}

const canGenerateCurrentArchive = computed(() => {
  const currentExecution = execution.value
  if (
    !currentExecution?.id ||
    !hasArchiveCreatePermission.value ||
    !currentExecution.closedAt ||
    currentExecution.canGenerateArchive !== true ||
    latestArchive.value?.archiveStatus === EDHR_EXECUTION_ARCHIVE_STATUS_GENERATING
  ) {
    return false
  }
  if (currentExecution.status === EDHR_EXECUTION_STATUS.FILL_COMPLETED) {
    return true
  }
  return (
    currentExecution.status === EDHR_EXECUTION_STATUS.APPROVED &&
    currentExecution.approvalSnapshotStatus === 'APPROVED'
  )
})

const canDownloadCurrentArchive = computed(
  () =>
    hasArchiveDownloadPermission.value &&
    (execution.value?.canDownloadArchive === true || latestArchive.value?.canDownloadArchive === true) &&
    latestArchive.value?.archiveStatus === EDHR_EXECUTION_ARCHIVE_STATUS_SEALED &&
    Boolean(latestArchive.value?.id)
)

const archiveGenerateActionLabel = computed(() =>
  latestArchive.value?.archiveStatus === EDHR_EXECUTION_ARCHIVE_STATUS_SEALED
    ? '重新生成归档打印件'
    : '生成归档打印件'
)

const archiveGateHint = computed(() => {
  const currentExecution = execution.value
  if (!currentExecution?.id) {
    return '当前执行记录不存在，不能生成或下载归档。'
  }
  if (latestArchive.value?.archiveStatus === EDHR_EXECUTION_ARCHIVE_STATUS_GENERATING) {
    return '归档打印件正在生成，请稍后刷新。'
  }
  if (latestArchive.value?.archiveStatus === EDHR_EXECUTION_ARCHIVE_STATUS_SEALED) {
    return '归档已封存，可下载；如需更新版本，请重新生成归档打印件。'
  }
  if (currentExecution.status === EDHR_EXECUTION_STATUS.SUBMITTED) {
    return '历史审批型记录需等待审批关闭后才可归档。'
  }
  if (currentExecution.status === EDHR_EXECUTION_STATUS.REJECTED) {
    return '已驳回记录不可归档，请从返工任务进入新修订草稿。'
  }
  if (!currentExecution.closedAt) {
    return '执行记录关闭后才可生成归档打印件。'
  }
  if (currentExecution.canGenerateArchive !== true) {
    return '后端未放行当前执行记录的归档生成门槛。'
  }
  if (
    currentExecution.status === EDHR_EXECUTION_STATUS.APPROVED &&
    currentExecution.approvalSnapshotStatus !== 'APPROVED'
  ) {
    return '历史审批型记录需审批快照通过后才可归档。'
  }
  return ''
})

const resolveHashStatusLabel = (status?: string) => {
  return status && status in EDHR_HASH_STATUS_LABEL_MAP
    ? EDHR_HASH_STATUS_LABEL_MAP[status as keyof typeof EDHR_HASH_STATUS_LABEL_MAP]
    : status || '--'
}

const createFieldAuditIdempotencyKey = () => {
  const randomUUID = globalThis.crypto?.randomUUID
  if (typeof randomUUID !== 'function') {
    throw new Error('当前浏览器缺少 crypto.randomUUID，不能生成字段审计幂等键。')
  }
  return randomUUID.call(globalThis.crypto)
}

const buildFieldAuditChangeRequest = (change: PendingFieldChange): EdhrFieldChangeItemReqVO => {
  if (change.validationMessage) {
    throw new Error(change.validationMessage)
  }
  return {
    fieldPath: change.fieldPath,
    fieldKey: change.fieldKey,
    rowIndex: change.rowIndex,
    columnIndex: change.columnIndex,
    valueType: change.valueType,
    newValueJson: change.newValueJson,
    newValueDisplay: change.newValueDisplay,
    expectedOldValueJson: change.expectedOldValueJson,
    expectedOldValueHash: change.expectedOldValueHash
  }
}

const handleRevertPendingFieldChange = (change: PendingFieldChange) => {
  draftFieldValues.value = {
    ...draftFieldValues.value,
    [change.fieldIdentity]: baselineFieldValues.value[change.fieldIdentity]
  }
}

const handleSaveFieldAuditChanges = async () => {
  if (fieldAuditSaveGateError.value) {
    fieldAuditSaveError.value = fieldAuditSaveGateError.value
    message.error(fieldAuditSaveGateError.value)
    return
  }
  if (workTaskId.value === undefined) {
    message.error('工作任务编号缺失，不能保存字段变更。')
    return
  }
  if (!fieldAuditIdempotencyKey.value) {
    fieldAuditIdempotencyKey.value = createFieldAuditIdempotencyKey()
  }

  fieldAuditSaveLoading.value = true
  fieldAuditSaveError.value = ''
  try {
    const response = await saveEdhrFieldChanges({
      executionId: execution.value!.id,
      workTaskId: workTaskId.value,
      ...(isRecordbookUnrestrictedMode.value
        ? {
            fillCarrier: 'RECORDBOOK',
            fillMode: RECORDBOOK_UNRESTRICTED_FILL_MODE
          }
        : {}),
      idempotencyKey: fieldAuditIdempotencyKey.value,
      baseCellValuesHash: String(execution.value!.cellValuesHash),
      baseFieldAuditRevision: Number(execution.value!.fieldAuditRevision),
      baseFieldAuditHeadHash: String(execution.value!.fieldAuditHeadHash),
      reasonCategory: resolveFieldAuditDraftReasonCategory(),
      reasonText: resolveFieldAuditDraftReasonText(),
      changes: pendingFieldChanges.value.map(buildFieldAuditChangeRequest),
      attachmentChanges: pendingAttachmentChanges.value.map(buildAttachmentChangeRequest)
    })
    if (response.hashVerification.status !== 'VALID') {
      throw new Error(
        `字段审计链校验未通过：${resolveHashStatusLabel(response.hashVerification.status)}`
      )
    }
    fieldAuditLastResult.value = response
    fieldAuditIdempotencyKey.value = ''
    showFillActionResultDialog('save-success')
    await loadExecution()
  } catch (error) {
    fieldAuditSaveError.value = resolveErrorMessage(error, '字段变更保存失败，请联系管理员。')
    message.error(fieldAuditSaveError.value)
  } finally {
    fieldAuditSaveLoading.value = false
  }
}

const submitReviewAssigneeOptions = computed(() => execution.value?.reviewAssigneeOptions || [])
const hasSubmitReviewAssigneeOptions = computed(() => submitReviewAssigneeOptions.value.length > 0)

const formatSubmitReviewCandidateLabel = (candidate: ProFeedbackEdhrReviewCandidateUserVO) => {
  const userName = typeof candidate.userName === 'string' ? candidate.userName.trim() : ''
  return userName ? `${userName}（${candidate.userId}）` : String(candidate.userId)
}

const formatSubmitReviewAssigneeLabel = (option: ProFeedbackEdhrReviewAssigneeOptionVO) => {
  const sourceName = typeof option.reviewSourceName === 'string' ? option.reviewSourceName.trim() : ''
  if (sourceName) return sourceName
  return option.signatureCellKey ? `签名 ${option.signatureCellKey}` : '审核/批准人'
}

const resetSubmitReviewAssigneeSelections = () => {
  Object.keys(submitReviewAssigneeSelections).forEach((key) => {
    delete submitReviewAssigneeSelections[key]
  })
  submitReviewAssigneeOptions.value.forEach((option) => {
    if (option.candidates?.length === 1) {
      submitReviewAssigneeSelections[option.signatureCellKey] = option.candidates[0].userId
    }
  })
}

const buildSubmitReviewAssigneeSelections = (): ProFeedbackEdhrReviewAssigneeSelectionVO[] | undefined => {
  if (!hasSubmitReviewAssigneeOptions.value) {
    return undefined
  }
  return submitReviewAssigneeOptions.value.map((option) => ({
    signatureCellKey: option.signatureCellKey,
    selectedUserId: Number(submitReviewAssigneeSelections[option.signatureCellKey])
  }))
}

const hasMissingSubmitReviewAssigneeSelection = () =>
  submitReviewAssigneeOptions.value.some((option) => {
    const selectedUserId = submitReviewAssigneeSelections[option.signatureCellKey]
    return !Number.isFinite(Number(selectedUserId)) || Number(selectedUserId) <= 0
  })

const resetSubmitForm = () => {
  submitForm.password = ''
  resetSubmitReviewAssigneeSelections()
}

const openSubmitDialog = () => {
  if (isReadonly.value) {
    message.error(readonlySubmitReason.value)
    return
  }
  if (hasSlotContextBlockers.value) {
    message.error(slotContextBlockers.value.join('；'))
    return
  }
  if (formSubmitGateError.value) {
    message.error(formSubmitGateError.value)
    return
  }
  if (hasPendingFieldChanges.value) {
    message.error('存在未保存字段变更，请先完成字段审计签名保存后再提交。')
    return
  }
  resetSubmitForm()
  submitDialogVisible.value = true
}

const handleSubmitExecution = async () => {
  if (!execution.value?.id) {
    message.error('当前执行记录不存在，无法提交。')
    return
  }
  if (hasSlotContextBlockers.value) {
    message.error(slotContextBlockers.value.join('；'))
    return
  }
  if (formSubmitGateError.value) {
    message.error(formSubmitGateError.value)
    return
  }
  if (!submitForm.password.trim()) {
    message.error('提交密码不能为空。')
    return
  }
  if (hasMissingSubmitReviewAssigneeSelection()) {
    message.error('请选择审核/批准人。')
    return
  }
  if (hasPendingFieldChanges.value) {
    message.error('存在未保存字段变更，请先完成字段审计签名保存后再提交。')
    return
  }
  submitLoading.value = true
  try {
    if (!workTaskId.value) {
      throw new Error('缺少 eDHR 工作任务上下文，不能提交。')
    }
    await ProFeedbackApi.submitEdhrExecution({
      id: execution.value.id,
      workTaskId: workTaskId.value,
      password: submitForm.password.trim(),
      reviewAssigneeSelections: buildSubmitReviewAssigneeSelections()
    })
    submitDialogVisible.value = false
    resetSubmitForm()
    showFillActionResultDialog('submit-success')
    await loadExecution()
  } catch (error) {
    const submitErrorMessage = resolveErrorMessage(error, 'eDHR 执行提交失败，请联系管理员。')
    message.error(submitErrorMessage)
    showFillActionResultDialog('submit-failed')
  } finally {
    submitLoading.value = false
  }
}

const resetFormReviewSignForm = () => {
  formReviewSignForm.password = ''
  formReviewSignForm.comment = ''
  resetSignatureTimeForm(formReviewSignatureTimeForm)
}

const closeFormReviewSignDialog = () => {
  formReviewSignDialogVisible.value = false
  formReviewSignError.value = ''
  resetFormReviewSignForm()
}

const openFormReviewSignDialog = () => {
  if (formReviewSignGateError.value) {
    message.error(formReviewSignGateError.value)
    return
  }
  formReviewSignError.value = ''
  resetFormReviewSignForm()
  formReviewSignDialogVisible.value = true
}

const handleFormReviewSign = async () => {
  if (formReviewSignGateError.value) {
    formReviewSignError.value = formReviewSignGateError.value
    message.error(formReviewSignError.value)
    return
  }
  if (workTaskId.value === undefined) {
    formReviewSignError.value = '工作任务编号缺失，不能保存表单复核签名。'
    message.error(formReviewSignError.value)
    return
  }
  if (!formReviewSignForm.password.trim()) {
    formReviewSignError.value = '签名密码不能为空。'
    return
  }

  formReviewSignLoading.value = true
  formReviewSignError.value = ''
  try {
    await ProFeedbackApi.cosignEdhrExecution({
      executionId: execution.value!.id,
      workTaskId: workTaskId.value,
      password: formReviewSignForm.password.trim(),
      comment: formReviewSignForm.comment.trim() || undefined,
      signatureTime: buildSignatureTimePayload(formReviewSignatureTimeForm)
    })
    formReviewSignDialogVisible.value = false
    resetFormReviewSignForm()
    message.success('表单复核签名已保存')
    await loadExecution()
  } catch (error) {
    formReviewSignError.value = resolveErrorMessage(error, '表单复核签名失败，请联系管理员。')
    message.error(formReviewSignError.value)
  } finally {
    formReviewSignLoading.value = false
  }
}

const isStaleExecutionPageRequest = (requestSerial: number) =>
  requestSerial !== executionPageRequestSerial

const isOptionalStaleExecutionPageRequest = (requestSerial?: number) =>
  requestSerial !== undefined && isStaleExecutionPageRequest(requestSerial)

const cancelDeferredExecutionSecondaryLoad = () => {
  if (executionSecondaryFrameId !== undefined) {
    cancelAnimationFrame(executionSecondaryFrameId)
    executionSecondaryFrameId = undefined
  }
}

const clearExecutionSecondaryState = () => {
  latestArchive.value = undefined
  trackingTimeline.value = []
  signatureRows.value = []
  archiveError.value = ''
  trackingError.value = ''
  signatureError.value = ''
  archiveLoading.value = false
}

const loadLatestArchive = async (requestSerial?: number) => {
  const targetExecutionId = execution.value?.id
  if (!targetExecutionId || !hasArchiveQueryPermission.value) {
    if (!isOptionalStaleExecutionPageRequest(requestSerial)) {
      latestArchive.value = undefined
      archiveError.value = ''
      archiveLoading.value = false
    }
    return
  }

  archiveLoading.value = true
  archiveError.value = ''
  try {
    const archive = await getLatestEdhrExecutionArchive(targetExecutionId, archiveArtifactType)
    if (isOptionalStaleExecutionPageRequest(requestSerial)) return
    latestArchive.value = archive
  } catch (error) {
    if (isOptionalStaleExecutionPageRequest(requestSerial)) return
    const errorMessage = resolveErrorMessage(error, '归档状态加载失败，请联系管理员。')
    if (isEdhrExecutionArchiveNotExistsMessage(errorMessage)) {
      latestArchive.value = undefined
      archiveError.value = ''
      return
    }
    latestArchive.value = undefined
    archiveError.value = errorMessage
  } finally {
    if (!isOptionalStaleExecutionPageRequest(requestSerial)) {
      archiveLoading.value = false
    }
  }
}

const resetArchiveForm = () => {
  archiveForm.sealPassword = ''
  archiveForm.comment = ''
  archiveForm.regenerate = latestArchive.value?.archiveStatus === EDHR_EXECUTION_ARCHIVE_STATUS_SEALED
}

const closeArchiveGenerateDialog = () => {
  archiveDialogVisible.value = false
  resetArchiveForm()
}

const openArchiveGenerateDialog = () => {
  if (!canGenerateCurrentArchive.value) {
    message.error(
      archiveGateHint.value ||
        '当前记录未满足归档门槛：普通工序需填写完成并关闭，历史审批型记录需审批关闭且审批快照通过。'
    )
    return
  }
  resetArchiveForm()
  archiveDialogVisible.value = true
}

const handleGenerateArchive = async () => {
  if (!execution.value?.id) {
    message.error('当前执行记录不存在，无法生成归档。')
    return
  }
  if (!canGenerateCurrentArchive.value) {
    message.error(
      archiveGateHint.value ||
        '当前记录未满足归档门槛：普通工序需填写完成并关闭，历史审批型记录需审批关闭且审批快照通过。'
    )
    return
  }
  if (!archiveForm.sealPassword.trim()) {
    message.error('封存密码不能为空。')
    return
  }

  archiveGenerateLoading.value = true
  archiveError.value = ''
  try {
    latestArchive.value = await generateEdhrExecutionArchive({
      executionId: execution.value.id,
      artifactType: archiveArtifactType,
      sealPassword: archiveForm.sealPassword.trim(),
      comment: archiveForm.comment.trim() || undefined,
      regenerate: archiveForm.regenerate
    })
    archiveDialogVisible.value = false
    resetArchiveForm()
    message.success(latestArchive.value?.created === false ? '已返回现有归档版本' : '归档生成成功')
    await loadLatestArchive()
  } catch (error) {
    archiveError.value = resolveErrorMessage(error, '归档生成失败，请联系管理员。')
    message.error(archiveError.value)
    await loadLatestArchive()
  } finally {
    archiveGenerateLoading.value = false
  }
}

const handleDownloadArchive = async () => {
  const archive = latestArchive.value
  if (!archive?.id || !canDownloadCurrentArchive.value) {
    message.error('当前归档未封存或无下载权限，无法下载。')
    return
  }
  archiveDownloadLoading.value = true
  archiveError.value = ''
  try {
    await downloadEdhrExecutionArchive(
      archive.id,
      archive.fileName,
      archive.artifactType,
      archive.contentType
    )
    message.success('归档打印件下载已开始')
  } catch (error) {
    archiveError.value = resolveErrorMessage(error, '归档下载失败，请联系管理员。')
    message.error(archiveError.value)
  } finally {
    archiveDownloadLoading.value = false
  }
}

const loadTrackingAndSignatures = async (requestSerial?: number) => {
  const targetExecutionId = execution.value?.id
  if (!targetExecutionId) {
    if (!isOptionalStaleExecutionPageRequest(requestSerial)) {
      trackingTimeline.value = []
      signatureRows.value = []
    }
    return
  }
  trackingError.value = ''
  signatureError.value = ''
  const [timelineResult, signatureResult] = await Promise.allSettled([
    getEdhrTrackingTimeline(targetExecutionId),
    getEdhrExecutionSignaturePage({
      pageNo: 1,
      pageSize: 20,
      executionId: targetExecutionId
    })
  ])
  if (isOptionalStaleExecutionPageRequest(requestSerial)) return
  if (timelineResult.status === 'fulfilled') {
    trackingTimeline.value = timelineResult.value
  } else {
    trackingTimeline.value = []
    trackingError.value = resolveErrorMessage(
      timelineResult.reason,
      'eDHR 追踪时间线加载失败，请联系管理员。'
    )
  }
  if (signatureResult.status === 'fulfilled') {
    signatureRows.value = signatureResult.value.list || []
  } else {
    signatureRows.value = []
    signatureError.value = resolveErrorMessage(
      signatureResult.reason,
      'eDHR 签名记录加载失败，请联系管理员。'
    )
  }
}

const loadExecutionSecondaryData = async (requestSerial: number) => {
  if (isStaleExecutionPageRequest(requestSerial)) return
  await Promise.all([
    loadLatestArchive(requestSerial),
    loadTrackingAndSignatures(requestSerial)
  ])
}

const deferExecutionSecondaryLoad = (requestSerial: number) => {
  cancelDeferredExecutionSecondaryLoad()
  executionSecondaryFrameId = requestAnimationFrame(() => {
    executionSecondaryFrameId = undefined
    if (isStaleExecutionPageRequest(requestSerial)) return
    void loadExecutionSecondaryData(requestSerial)
  })
}

const loadExecution = async () => {
  const requestSerial = ++executionPageRequestSerial
  cancelDeferredExecutionSecondaryLoad()
  const currentExecutionId = executionId.value
  const currentExecutionContextKey = resolveExecutionContextKey()
  if (!currentExecutionId) {
    execution.value = undefined
    loadedExecutionContextKey.value = ''
    clearExecutionSecondaryState()
    loadError.value = '缺少 eDHR 执行记录 ID，无法加载执行页。'
    return
  }

  loading.value = true
  loadError.value = ''
  archiveError.value = ''
  formReviewSignError.value = ''
  clearExecutionSecondaryState()
  try {
    const detail = await ProFeedbackApi.getEdhrExecution(currentExecutionId, workTaskId.value)
    if (isStaleExecutionPageRequest(requestSerial)) return
    if (!detail?.id) {
      throw new Error('eDHR 执行记录未返回有效执行记录 ID。')
    }
    if (typeof detail.executionSnapshotJson !== 'string' || !detail.executionSnapshotJson.trim()) {
      throw new Error('eDHR 执行记录缺少 executionSnapshotJson，无法渲染执行表单。')
    }
    const shouldLoadCellLinkPrefill =
      !isTrackingReadonlyMode.value && detail.status === EDHR_EXECUTION_STATUS.DRAFT
    const prefillResponse = shouldLoadCellLinkPrefill
      ? await BatchRecordCellLinkApi.getPrefill(currentExecutionId, workTaskId.value)
      : undefined
    if (isStaleExecutionPageRequest(requestSerial)) return
    execution.value = detail
    hydrateDraftState(detail, prefillResponse?.prefills || [], prefillResponse?.conflicts || [])
    loadedExecutionContextKey.value = currentExecutionContextKey
    deferExecutionSecondaryLoad(requestSerial)
  } catch (error) {
    if (isStaleExecutionPageRequest(requestSerial)) return
    execution.value = undefined
    loadedExecutionContextKey.value = ''
    clearExecutionSecondaryState()
    draftFieldValues.value = {}
    draftAttachmentValues.value = {}
    draftImageAttachmentValues.value = {}
    attachmentMetadataByUrl.value = {}
    baselineFieldValues.value = {}
    baselineFieldValueHashes.value = {}
    cellLinkPrefills.value = []
    cellLinkConflicts.value = []
    draftRemark.value = ''
    loadError.value = resolveErrorMessage(error, 'eDHR 执行页加载失败，请联系管理员。')
  } finally {
    if (!isStaleExecutionPageRequest(requestSerial)) {
      loading.value = false
    }
  }
}

const loadRecordbookGlobalSetting = async () => {
  if (!hasGoldenFingerPermission.value) return
  const setting = await getEdhrRecordbookGlobalSetting()
  recordbookGlobalEnabled.value = setting.enabled === true
}

const initializeExecutionPage = async () => {
  try {
    await loadRecordbookGlobalSetting()
    await loadExecution()
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '记录本全局开关加载失败。')
  }
}

const handleBackToList = async () => {
  if (currentBatchExecutionId.value) {
    await router.push({
      path: '/mes/pro/feedback/edhr-batch-execution/detail',
      query: {
        id: currentBatchExecutionId.value,
        batchTaskId: readRouteQueryString(route.query.batchTaskId) || undefined,
        workTaskId: readRouteQueryString(route.query.workTaskId) || undefined
      }
    })
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-execution',
    query: {
      workOrderCode:
        execution.value?.workOrderCode || readRouteQueryString(route.query.workOrderCode) || undefined,
      batchCode: execution.value?.batchCode || readRouteQueryString(route.query.batchCode) || undefined,
      batchExecutionCode: readRouteQueryString(route.query.batchExecutionCode) || undefined
    }
  })
}

const closeAssistSwitchDialog = () => {
  assistSwitchDialogVisible.value = false
}

const isAssistFillWorkTask = (row: EdhrWorkTaskRespVO) =>
  row.taskType === EDHR_WORK_TASK_TYPE_FILL || row.taskType === EDHR_WORK_TASK_TYPE_REWORK

const isAssistSpecialBatchTask = (row?: EdhrBatchExecutionTaskRespVO) =>
  Boolean(row?.nodeType && row.nodeType !== EDHR_BATCH_NODE_ROUTE_FORM)

const hasAssistOpenFormAction = (row: EdhrBatchExecutionTaskRespVO) =>
  Array.isArray(row.allowedActions) && row.allowedActions.includes('OPEN_FORM')

const isAssistBatchTaskOpenable = (row: EdhrBatchExecutionTaskRespVO) =>
  !isAssistSpecialBatchTask(row) &&
  !row.slotBlockerMessage &&
  row.available !== false &&
  row.status !== EDHR_BATCH_TASK_STATUS_BLOCKED &&
  row.status !== EDHR_BATCH_TASK_STATUS_APPROVED &&
  row.status !== EDHR_BATCH_TASK_STATUS_SKIPPED &&
  Boolean(row.activeWorkTaskId) &&
  hasAssistOpenFormAction(row)

const loadAssistWorkTaskSwitchItems = async () => {
  const data = await getEdhrWorkTaskMyPage({
    pageNo: 1,
    pageSize: ASSIST_SWITCH_PAGE_SIZE,
    status: EDHR_WORK_TASK_STATUS_TODO
  })
  return (data.list || []).filter(isAssistFillWorkTask)
}

const loadAssistTaskSwitchItems = async () => {
  assistTaskSwitchLoading.value = true
  assistTaskSwitchError.value = ''
  try {
    assistTaskSwitchItems.value = await loadAssistWorkTaskSwitchItems()
  } catch (error) {
    assistTaskSwitchItems.value = []
    assistTaskSwitchError.value = resolveErrorMessage(error, '辅助模式任务列表加载失败。')
  } finally {
    assistTaskSwitchLoading.value = false
  }
}

const requireAssistBatchExecutionId = () => {
  const batchExecutionId = parsePositiveNumber(currentBatchExecutionId.value)
  if (!batchExecutionId) {
    throw new Error('当前填写页缺少批次执行编号，不能切换工序。')
  }
  return batchExecutionId
}

const loadAssistProcessSwitchItems = async () => {
  assistProcessSwitchLoading.value = true
  assistProcessSwitchError.value = ''
  try {
    const batchExecutionId = requireAssistBatchExecutionId()
    const batchDetail = await getEdhrBatchExecution(batchExecutionId)
    assistProcessSwitchItems.value = [...(batchDetail.tasks || [])]
      .filter(isAssistBatchTaskOpenable)
      .sort(
        (first, second) =>
          (first.routeProcessSort || 0) - (second.routeProcessSort || 0) ||
          (first.batchRecordSort || 0) - (second.batchRecordSort || 0) ||
          first.id - second.id
      )
  } catch (error) {
    assistProcessSwitchItems.value = []
    assistProcessSwitchError.value = resolveErrorMessage(error, '辅助模式工序列表加载失败。')
  } finally {
    assistProcessSwitchLoading.value = false
  }
}

const resolveCurrentAssistBatchTask = (tasks: EdhrBatchExecutionTaskRespVO[]) => {
  const batchTaskId = parsePositiveRouteQueryId(route.query.batchTaskId)
  if (batchTaskId) {
    const batchTask = tasks.find((task) => sameRouteQueryId(task.id, batchTaskId))
    if (batchTask) return batchTask
  }
  if (executionId.value) {
    const executionTask = tasks.find((task) => sameRouteQueryId(task.executionId, executionId.value))
    if (executionTask) return executionTask
  }
  if (workTaskId.value) {
    const workTask = tasks.find((task) => sameRouteQueryId(task.activeWorkTaskId, workTaskId.value))
    if (workTask) return workTask
  }
  const routeProcessId = parsePositiveNumber(execution.value?.routeProcessId)
  if (routeProcessId) {
    const reportId = execution.value?.batchRecordReportId
    const executionTask = tasks.find(
      (task) =>
        task.routeProcessId === routeProcessId &&
        (!reportId || task.batchRecordReportId === reportId)
    )
    if (executionTask) return executionTask
  }
  throw new Error('当前填写页无法识别所属批次任务，不能解析当前工序填写人。')
}

const loadAssistFillerSwitchItems = async () => {
  assistFillerSwitchLoading.value = true
  assistFillerSwitchError.value = ''
  try {
    const batchExecutionId = requireAssistBatchExecutionId()
    const batchDetail = await getEdhrBatchExecution(batchExecutionId)
    const currentTask = resolveCurrentAssistBatchTask(batchDetail.tasks || [])
    const routeProcessId = parsePositiveNumber(currentTask.routeProcessId)
    if (!routeProcessId) {
      throw new Error(`当前批次任务 ${currentTask.id} 缺少 routeProcessId，不能解析填写人。`)
    }
    const currentProcessTasks = [...(batchDetail.tasks || [])]
      .filter(
        (task) =>
          !isAssistSpecialBatchTask(task) &&
          task.routeProcessId === routeProcessId
      )
      .sort(
        (first, second) =>
          (!first.formTemplateId && first.formSlotType === 'MAIN' ? 0 : 1) -
            (!second.formTemplateId && second.formSlotType === 'MAIN' ? 0 : 1) ||
          (first.batchRecordSort || 0) - (second.batchRecordSort || 0) ||
          first.id - second.id
      )
    assistFillerSwitchItems.value = currentProcessTasks.flatMap((task) =>
      (task.fillableUsers || []).map((user) => ({
        key: `${task.id}:${user.userId}`,
        task,
        userId: user.userId,
        displayName: user.displayName || `用户 ${user.userId}`
      }))
    )
  } catch (error) {
    assistFillerSwitchItems.value = []
    assistFillerSwitchError.value = resolveErrorMessage(error, '辅助模式当前工序填写人加载失败。')
  } finally {
    assistFillerSwitchLoading.value = false
  }
}

const openAssistSwitchDialog = (
  type: AssistSwitchDialogType,
  loadItems: () => Promise<void>
) => {
  assistSwitchDialogType.value = type
  assistSwitchDialogVisible.value = true
  void loadItems()
}

const handleAssistTaskSwitch = () => {
  openAssistSwitchDialog('task', loadAssistTaskSwitchItems)
}

const handleAssistProcessSwitch = () => {
  openAssistSwitchDialog('process', loadAssistProcessSwitchItems)
}

const handleAssistFillerSwitch = () => {
  openAssistSwitchDialog('filler', loadAssistFillerSwitchItems)
}

const resolveAssistWorkTaskPrimaryLabel = (row: EdhrWorkTaskRespVO) => {
  const parts = [row.workOrderCode || row.taskCode || `任务 ${row.id}`, row.batchCode].filter(Boolean)
  return parts.length ? parts.join(' / ') : `任务 ${row.id}`
}

const resolveAssistWorkTaskTypeLabel = (row: EdhrWorkTaskRespVO) =>
  row.taskType === EDHR_WORK_TASK_TYPE_REWORK ? '返工' : '填写'

const resolveAssistWorkTaskStatusLabel = (row: EdhrWorkTaskRespVO) =>
  row.status === EDHR_WORK_TASK_STATUS_TODO ? '待处理' : row.status || '状态未知'

const resolveAssistWorkTaskSecondaryLabel = (row: EdhrWorkTaskRespVO) => {
  const parts = [
    row.processName || '当前工序',
    resolveAssistWorkTaskTypeLabel(row),
    resolveAssistWorkTaskStatusLabel(row),
    row.taskCode ? `编号 ${row.taskCode}` : ''
  ].filter(Boolean)
  return parts.join(' · ')
}

const resolveAssistBatchTaskPrimaryLabel = (row: EdhrBatchExecutionTaskRespVO) =>
  row.processName || row.processCode || `工序任务 ${row.id}`

const resolveAssistBatchTaskStatusLabel = (row: EdhrBatchExecutionTaskRespVO) => {
  if (row.status === EDHR_BATCH_TASK_STATUS_APPROVED) return '已完成'
  if (row.status === EDHR_BATCH_TASK_STATUS_SKIPPED) return '已跳过'
  if (row.status === EDHR_BATCH_TASK_STATUS_BLOCKED) return '已阻塞'
  return '可填写'
}

const resolveAssistBatchTaskSecondaryLabel = (row: EdhrBatchExecutionTaskRespVO) => {
  const parts = [
    row.routeProcessSort == null ? '' : `序号 ${row.routeProcessSort}`,
    row.batchRecordReportName || row.formTemplateName || row.executionCode || '',
    resolveAssistBatchTaskStatusLabel(row)
  ].filter(Boolean)
  return parts.join(' · ')
}

const isAssistWorkTaskActive = (row: EdhrWorkTaskRespVO) => sameRouteQueryId(workTaskId.value, row.id)

const isAssistBatchTaskActive = (row: EdhrBatchExecutionTaskRespVO) =>
  sameRouteQueryId(workTaskId.value, row.activeWorkTaskId) ||
  sameRouteQueryId(executionId.value, row.executionId) ||
  readRouteQueryString(route.query.batchTaskId) === String(row.id)

const resolveAssistFillerFormName = (row: EdhrBatchExecutionTaskRespVO) =>
  row.batchRecordReportName ||
  row.formTemplateName ||
  row.batchRecordReportCode ||
  row.executionCode ||
  `表单任务 ${row.id}`

const resolveAssistFillerSwitchItemSecondaryLabel = (item: AssistFillerSwitchItem) =>
  resolveAssistFillerFormName(item.task)

const currentAssistUserId = () => {
  const user = userStore.getUser || userStore.user
  return parsePositiveNumber(user?.id)
}

const isAssistFillerSwitchItemSelectable = (item: AssistFillerSwitchItem) =>
  currentAssistUserId() === item.userId && isAssistBatchTaskOpenable(item.task)

const isAssistFillerSwitchItemActive = (item: AssistFillerSwitchItem) =>
  currentAssistUserId() === item.userId && isAssistBatchTaskActive(item.task)

const resolveAssistRecordCategory = (row: EdhrBatchExecutionTaskRespVO) =>
  row.recordCategory === 'INTERNAL_RECORD' ? 'INTERNAL_RECORD' : 'BATCH_RECORD'

const buildAssistFillCarrierExecutionQuery = (row: EdhrBatchExecutionTaskRespVO) => {
  const recordCategory = resolveAssistRecordCategory(row)
  const fillCarrier =
    recordCategory === 'INTERNAL_RECORD' && isGlobalRecordbookEnabled.value ? 'RECORDBOOK' : 'FORM'
  const query: Record<string, string> = {
    fillCarrier,
    recordCategory
  }
  if (fillCarrier === 'RECORDBOOK') {
    query.fillMode = RECORDBOOK_UNRESTRICTED_FILL_MODE
  }
  return query
}

const stringifyAssistRouteQuery = (value?: Record<string, string | number | null | undefined>) => {
  const query: Record<string, string> = {}
  Object.entries(value || {}).forEach(([key, entryValue]) => {
    if (entryValue !== undefined && entryValue !== null && entryValue !== '') {
      query[key] = String(entryValue)
    }
  })
  return query
}

const navigateToAssistWorkTask = async (
  row: EdhrWorkTaskRespVO,
  setError: (message: string) => void
) => {
  try {
    if (!row.id) {
      throw new Error('工作任务缺少 workTaskId，不能切换。')
    }
    if (!row.actionUrl) {
      throw new Error(`工作任务 ${row.id} 缺少处理入口，不能切换。`)
    }
    if (!row.batchTaskId) {
      throw new Error(`工作任务 ${row.id} 缺少批次任务编号，不能切换。`)
    }
    fillViewMode.value = 'assist'
    await navigateToEdhrWorkTask(router, row)
    fillViewMode.value = 'assist'
    closeAssistSwitchDialog()
  } catch (error) {
    const errorMessage = resolveErrorMessage(error, '辅助模式任务切换失败。')
    setError(errorMessage)
    message.error(errorMessage)
  }
}

const handleSelectAssistTaskSwitchItem = async (row: EdhrWorkTaskRespVO) => {
  await navigateToAssistWorkTask(row, (errorMessage) => {
    assistTaskSwitchError.value = errorMessage
  })
}

const navigateToAssistBatchTask = async (
  row: EdhrBatchExecutionTaskRespVO,
  setError: (message: string) => void,
  fallbackMessage: string
) => {
  try {
    const batchExecutionId = requireAssistBatchExecutionId()
    if (!row.activeWorkTaskId) {
      throw new Error(`工序任务 ${row.id} 缺少工作任务编号，不能切换。`)
    }
    const opened = await openEdhrBatchTask({
      batchExecutionId,
      taskId: row.id,
      workTaskId: row.activeWorkTaskId
    })
    if (!opened.executionId) {
      throw new Error('打开工序任务后端未返回 executionId，不能进入 eDHR 填写页。')
    }
    const openedWorkTaskId = opened.workTaskId || opened.executionPageQuery?.workTaskId || row.activeWorkTaskId
    const query = {
      ...stringifyAssistRouteQuery(opened.executionPageQuery),
      id: String(opened.executionId),
      executionId: String(opened.executionId),
      batchExecutionId: String(batchExecutionId),
      batchTaskId: String(row.id),
      workTaskId: String(openedWorkTaskId),
      ...buildAssistFillCarrierExecutionQuery(row)
    }
    fillViewMode.value = 'assist'
    await router.push({
      path: '/mes/pro/feedback/edhr-execution/form',
      query
    })
    fillViewMode.value = 'assist'
    closeAssistSwitchDialog()
  } catch (error) {
    const errorMessage = resolveErrorMessage(error, fallbackMessage)
    setError(errorMessage)
    message.error(errorMessage)
  }
}

const handleSelectAssistFillerSwitchItem = async (item: AssistFillerSwitchItem) => {
  if (!isAssistFillerSwitchItemSelectable(item)) {
    assistFillerSwitchError.value = '该填写人不属于当前账号可处理项，不能代填或切换责任人。'
    message.error(assistFillerSwitchError.value)
    return
  }
  await navigateToAssistBatchTask(
    item.task,
    (errorMessage) => {
      assistFillerSwitchError.value = errorMessage
    },
    '辅助模式填写人切换失败。'
  )
}

const handleSelectAssistProcessSwitchItem = async (row: EdhrBatchExecutionTaskRespVO) => {
  await navigateToAssistBatchTask(
    row,
    (errorMessage) => {
      assistProcessSwitchError.value = errorMessage
    },
    '辅助模式工序切换失败。'
  )
}

const resolveAssistFieldElement = (fieldIdentity: string) => {
  const elements = fillWorkspaceRef.value?.querySelectorAll<HTMLElement>('[data-assist-field-id]') || []
  return Array.from(elements).find((element) => element.dataset.assistFieldId === fieldIdentity)
}

const scrollToFirstIncompleteAssistField = async () => {
  const firstIncomplete = assistIncompleteItems.value[0]
  if (!firstIncomplete) {
    message.success('当前没有未完成填写项。')
    return
  }
  await nextTick()
  const target = resolveAssistFieldElement(firstIncomplete.fieldIdentity)
  if (!target) {
    message.warning('未找到未完成填写项，请刷新后重试。')
    return
  }
  target.scrollIntoView({ behavior: 'smooth', block: 'center' })
  highlightedAssistFieldIdentity.value = firstIncomplete.fieldIdentity
  if (assistHighlightTimer) {
    window.clearTimeout(assistHighlightTimer)
  }
  assistHighlightTimer = window.setTimeout(() => {
    if (highlightedAssistFieldIdentity.value === firstIncomplete.fieldIdentity) {
      highlightedAssistFieldIdentity.value = ''
    }
  }, 1800)
}

const buildAttachmentChangeRequest = (
  change: PendingAttachmentChange
): EdhrFieldAttachmentChangeReqVO => {
  if (change.validationMessage) {
    throw new Error(change.validationMessage)
  }
  if (change.workTaskId === undefined) {
    throw new Error('工作任务编号缺失，不能保存附件审计。')
  }
  return {
    workTaskId: change.workTaskId,
    fieldPath: change.fieldPath,
    fieldKey: change.fieldKey,
    rowIndex: change.rowIndex,
    columnIndex: change.columnIndex,
    attachmentType: change.attachmentType,
    attachmentAction: change.attachmentAction,
    attachmentGroupKey: change.attachmentGroupKey,
    fileUrl: change.fileUrl,
    fileId: change.fileId,
    storageConfigId: change.storageConfigId,
    storagePath: change.storagePath,
    fileName: change.fileName,
    contentType: change.contentType,
    fileSize: change.fileSize,
    sha256: change.sha256,
    storageRetentionJson: change.storageRetentionJson,
    storageRetentionHash: change.storageRetentionHash,
    expectedPreviousAttachmentHash: change.expectedPreviousAttachmentHash
  }
}

const syncFillWorkspaceFullscreenState = () => {
  isFillWorkspaceFullscreen.value = document.fullscreenElement === fillWorkspaceRef.value
}

const toggleFillWorkspaceFullscreen = async () => {
  const workspace = fillWorkspaceRef.value
  if (!workspace) {
    message.error('填写工作区尚未加载，无法进入全屏')
    return
  }
  try {
    if (document.fullscreenElement === workspace) {
      await document.exitFullscreen()
      return
    }
    await workspace.requestFullscreen()
  } catch (error) {
    const detail = error instanceof Error ? `：${error.message}` : ''
    message.error(`切换全屏失败${detail}`)
  }
}

watch(
  () => [route.name, route.query.id, route.query.workTaskId] as const,
  ([routeName]) => {
    if (routeName !== 'MesProFeedbackEdhrExecutionForm') {
      return
    }
    const nextExecutionContextKey = resolveExecutionContextKey()
    if (
      !nextExecutionContextKey ||
      loadedExecutionContextKey.value === nextExecutionContextKey
    ) {
      return
    }
    void initializeExecutionPage()
  }
)

onMounted(() => {
  document.addEventListener('fullscreenchange', syncFillWorkspaceFullscreenState)
  void initializeExecutionPage()
})

onBeforeUnmount(() => {
  cancelDeferredExecutionSecondaryLoad()
  document.removeEventListener('fullscreenchange', syncFillWorkspaceFullscreenState)
})
</script>

<style scoped>
.edhr-page-shell__content.is-fill-workspace {
  gap: 0;
  min-height: calc(100vh - 142px);
}

.edhr-fill-workspace {
  display: grid;
  grid-template-columns: 136px minmax(0, 1fr);
  width: 100%;
  min-width: 1024px;
  height: calc(100vh - 142px);
  min-height: 620px;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f7f9fc;
}

.edhr-fill-workspace:fullscreen {
  width: 100vw;
  min-width: 1024px;
  height: 100vh;
  min-height: 0;
  border: 0;
  border-radius: 0;
}

.edhr-fill-workspace__rail {
  display: flex;
  flex-direction: column;
  width: 136px;
  min-width: 136px;
  height: 100%;
  min-height: 0;
  border-right: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-fill-workspace__rail-scroll {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 12px 8px;
  overflow-y: auto;
}

.edhr-fill-workspace__section-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.edhr-fill-workspace__meta {
  display: grid;
  gap: 0;
  margin: 0;
  border-top: 1px solid #edf1f6;
}

.edhr-fill-workspace__meta > div {
  padding: 10px 0;
  border-bottom: 1px solid #edf1f6;
}

.edhr-fill-workspace__meta dt {
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}

.edhr-fill-workspace__meta dd {
  margin: 0;
  color: #263247;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.edhr-fill-workspace__section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edhr-fill-workspace__fit-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.edhr-fill-workspace__fit-actions button {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
  min-height: 58px;
  padding: 8px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  color: #4b5563;
  background: #ffffff;
  cursor: pointer;
}

.edhr-fill-workspace__fit-actions button:hover {
  border-color: #91caff;
  color: #1677ff;
}

.edhr-fill-workspace__fit-actions button.is-active {
  border-color: #1677ff;
  color: #1677ff;
  background: #eaf4ff;
  box-shadow: inset 0 0 0 1px #1677ff;
}

.edhr-fill-workspace__view-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.edhr-fill-workspace__view-actions button {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
  min-height: 58px;
  padding: 8px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  color: #4b5563;
  background: #ffffff;
  cursor: pointer;
}

.edhr-fill-workspace__view-actions button:hover {
  border-color: #91caff;
  color: #1677ff;
}

.edhr-fill-workspace__view-actions button.is-active {
  border-color: #1677ff;
  color: #1677ff;
  background: #eaf4ff;
  box-shadow: inset 0 0 0 1px #1677ff;
}

.edhr-fill-workspace__assist-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 100%;
  padding: 0;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
}

.edhr-fill-workspace__assist-topbar {
  position: sticky;
  top: 0;
  z-index: 5;
  display: grid;
  grid-template-columns: minmax(180px, 0.8fr) minmax(360px, 1.4fr) 132px;
  gap: 10px;
  align-items: stretch;
  padding: 12px;
  border-bottom: 1px solid #dbe3ef;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(23, 32, 51, 0.06);
}

.edhr-fill-workspace__assist-heading {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.edhr-fill-workspace__assist-title {
  color: #172033;
  font-size: 18px;
  font-weight: 800;
}

.edhr-fill-workspace__assist-subtitle {
  margin-top: 4px;
  color: #5b6678;
  font-size: 12px;
  line-height: 1.35;
}

.edhr-fill-workspace__assist-switch-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.edhr-fill-workspace__assist-switch,
.edhr-fill-workspace__assist-missing-jump {
  min-width: 0;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fbff;
  cursor: pointer;
}

.edhr-fill-workspace__assist-switch {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-rows: auto auto;
  gap: 4px 8px;
  padding: 8px 10px;
  text-align: left;
}

.edhr-fill-workspace__assist-switch span,
.edhr-fill-workspace__assist-switch em {
  color: #5b6678;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.edhr-fill-workspace__assist-switch strong {
  min-width: 0;
  color: #172033;
  font-size: 15px;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-fill-workspace__assist-switch em {
  grid-row: 1 / 3;
  grid-column: 2;
  align-self: center;
  padding: 2px 8px;
  border-radius: 999px;
  color: #0f62d6;
  background: #eaf3ff;
}

.edhr-fill-workspace__assist-switch:hover {
  border-color: #91caff;
  color: #1677ff;
}

:global(.edhr-fill-workspace__assist-switch-dialog) {
  max-width: calc(100vw - 32px);
}

:global(.edhr-fill-workspace__assist-switch-dialog .el-dialog__body) {
  padding-top: 8px;
}

.edhr-fill-workspace__assist-switch-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: min(60vh, 520px);
  overflow: auto;
}

.edhr-fill-workspace__assist-switch-menu-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  padding-bottom: 6px;
  border-bottom: 1px solid #edf1f6;
}

.edhr-fill-workspace__assist-switch-menu-head strong {
  color: #172033;
  font-size: 14px;
  font-weight: 800;
}

.edhr-fill-workspace__assist-switch-menu-head span,
.edhr-fill-workspace__assist-switch-loading {
  color: #6b7280;
  font-size: 12px;
}

.edhr-fill-workspace__assist-switch-option {
  display: flex;
  flex-direction: column;
  gap: 3px;
  width: 100%;
  padding: 9px 10px;
  text-align: left;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #ffffff;
  cursor: pointer;
}

.edhr-fill-workspace__assist-switch-option:hover,
.edhr-fill-workspace__assist-switch-option:focus-visible,
.edhr-fill-workspace__assist-switch-option.is-active {
  border-color: #91caff;
  background: #f4f9ff;
  outline: none;
}

.edhr-fill-workspace__assist-switch-option:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.edhr-fill-workspace__assist-switch-option:disabled:hover {
  border-color: #e5ebf3;
  background: #ffffff;
}

.edhr-fill-workspace__assist-switch-option-main {
  color: #172033;
  font-size: 13px;
  font-weight: 800;
}

.edhr-fill-workspace__assist-switch-option-sub {
  color: #5b6678;
  font-size: 12px;
  line-height: 1.35;
}

.edhr-fill-workspace__assist-missing-jump {
  display: grid;
  place-items: center;
  padding: 6px 8px;
  color: #d92d20;
  background: #fff7f6;
  border-color: #ffd0ca;
  font-weight: 800;
}

.edhr-fill-workspace__assist-missing-jump strong {
  font-size: 30px;
  line-height: 1;
}

.edhr-fill-workspace__assist-missing-jump.is-done {
  color: #13a36b;
  background: #e9f8f1;
  border-color: #c9eadb;
}

.edhr-fill-workspace__assist-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: 0 12px;
  padding: 8px 10px;
  border: 1px solid #ffe1a8;
  border-radius: 8px;
  color: #8a4b00;
  background: #fffaf0;
  font-size: 13px;
  font-weight: 700;
}

.edhr-fill-workspace__assist-summary.is-complete {
  border-color: #c9eadb;
  color: #0f7a4c;
  background: #f0fbf6;
}

.edhr-fill-workspace__assist-summary-title {
  color: #172033;
  font-weight: 800;
}

.edhr-fill-workspace__assist-summary-item {
  padding: 2px 8px;
  border-radius: 999px;
  background: #ffffff;
}

.edhr-fill-workspace__assist-list {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
}

.edhr-fill-workspace__assist-row {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(360px, 1.2fr);
  gap: 14px;
  align-items: center;
  min-height: 96px;
  padding: 12px 14px;
  border-top: 1px solid #edf1f6;
  background: #ffffff;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

.edhr-fill-workspace__assist-row:first-child {
  border-top: 0;
}

.edhr-fill-workspace__assist-row.is-missing {
  background: #fffaf6;
  box-shadow: inset 5px 0 0 #d97706;
}

.edhr-fill-workspace__assist-row.is-error {
  background: #fff7f6;
  box-shadow: inset 5px 0 0 #d92d20;
}

.edhr-fill-workspace__assist-row.is-complete {
  box-shadow: inset 5px 0 0 #13a36b;
}

.edhr-fill-workspace__assist-row.is-highlighted {
  background: #eaf4ff;
  box-shadow: inset 6px 0 0 #1677ff, 0 0 0 2px #91caff inset;
}

.edhr-fill-workspace__assist-row-meta {
  min-width: 0;
}

.edhr-fill-workspace__assist-label {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  min-width: 0;
  color: #172033;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.35;
}

.edhr-fill-workspace__assist-label > span:first-child {
  min-width: 0;
  overflow-wrap: anywhere;
}

.edhr-fill-workspace__assist-help {
  margin-top: 6px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.edhr-fill-workspace__assist-source {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 5px;
  color: #6b7280;
  font-size: 12px;
}

.edhr-fill-workspace__assist-control {
  min-width: 0;
}

.edhr-fill-workspace__choice-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  min-height: 38px;
  padding: 7px 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fafcff;
}

.edhr-fill-workspace__choice-group :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.edhr-fill-workspace__choice-group :deep(.el-radio) {
  margin-right: 0;
  color: #263247;
  font-weight: 700;
}

.edhr-fill-workspace__assist-signature {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 36px;
  padding: 8px 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fbff;
}

.edhr-fill-workspace__assist-validation {
  margin-top: 6px;
  color: #c02626;
  font-size: 12px;
  font-weight: 700;
}

.edhr-fill-workspace__assist-typed-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.edhr-fill-workspace__change-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  color: #4b5563;
  background: #fafcff;
  font-size: 13px;
}

.edhr-fill-workspace__change-summary strong {
  color: #1677ff;
  font-size: 18px;
}

.edhr-fill-workspace__field-audit-reason {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fbff;
}

.edhr-fill-workspace__field-audit-reason :deep(.el-select),
.edhr-fill-workspace__field-audit-reason :deep(.el-input) {
  width: 100%;
}

.edhr-fill-workspace__rail-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  padding: 8px;
  border-top: 1px solid #dbe3ef;
  background: #fafcff;
}

.edhr-fill-workspace__rail-actions :deep(.el-button) {
  width: 100%;
  margin: 0;
  padding-inline: 8px;
}

.edhr-fill-workspace__canvas {
  min-width: 0;
  min-height: 0;
  padding: 16px;
  background: #f7f9fc;
}

.edhr-fill-workspace__canvas.is-fit-width {
  overflow-x: hidden;
  overflow-y: auto;
}

.edhr-fill-workspace__canvas.is-fit-height {
  overflow: hidden;
}

.edhr-fill-workspace__form {
  width: 100%;
  min-height: 100%;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-fill-workspace__form-error,
.edhr-fill-workspace__load-error {
  margin: 16px;
}

.edhr-fill-workspace__prefill-alert {
  margin-bottom: 10px;
}

.edhr-fill-workspace__field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
  text-align: left;
}

.edhr-fill-workspace__field.is-out-of-scope {
  opacity: 0.72;
}

.edhr-fill-workspace__field-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 6px;
  color: #172033;
  font-size: 11px;
  font-weight: 600;
}

.edhr-fill-workspace__required {
  flex: 0 0 auto;
  color: #c00000;
}

.edhr-fill-workspace__cell-link-tag {
  flex-shrink: 0;
}

.edhr-fill-workspace__signature {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 6px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-page-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-page-shell__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-page-shell__title,
.edhr-page-shell__section-title {
  color: #172033;
  font-size: 18px;
  font-weight: 600;
}

.edhr-page-shell__subtitle,
.edhr-page-shell__section-subtitle {
  margin-top: 4px;
  color: #4b5563;
  font-size: 13px;
}

.edhr-page-shell__actions,
.edhr-page-shell__section-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.edhr-page-shell__more-actions {
  display: inline-flex;
}

.edhr-page-shell__more-icon {
  margin-left: 4px;
}

.edhr-page-shell__alert,
.edhr-page-shell__archive-alert {
  margin-bottom: 0;
}

.edhr-page-shell__content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-page-shell__summary,
.edhr-page-shell__status-overview,
.edhr-page-shell__archive,
.edhr-page-shell__compat,
.edhr-page-shell__slot-context,
.edhr-page-shell__attachments,
.edhr-page-shell__form,
.edhr-page-shell__field-audit,
.edhr-page-shell__audit-tabs {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-page-shell__status-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0;
  padding: 0;
  overflow: hidden;
}

.edhr-page-shell__status-item {
  min-width: 0;
  padding: 14px 16px;
  border-left: 1px solid #edf1f6;
}

.edhr-page-shell__status-item:first-child {
  border-left: 0;
}

.edhr-page-shell__status-item--state {
  background: #fafcff;
}

.edhr-page-shell__status-item--completion {
  background: #fcfdff;
}

.edhr-page-shell__status-label {
  margin-bottom: 8px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1;
}

.edhr-page-shell__status-value {
  color: #172033;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.edhr-page-shell__status-hint {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.edhr-page-shell__status-blockers {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.edhr-page-shell__status-blockers :deep(.el-tag) {
  max-width: 100%;
  height: auto;
  min-height: 24px;
  padding: 3px 8px;
  white-space: normal;
  line-height: 1.35;
}

.edhr-page-shell__status-more {
  color: #4b5563;
  font-size: 12px;
  line-height: 24px;
}

.edhr-page-shell__archive {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-page-shell__technical-evidence {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-page-shell__technical-evidence :deep(.el-collapse-item__header) {
  min-height: 54px;
  padding: 0 16px;
  border-bottom-color: #edf1f6;
}

.edhr-page-shell__technical-evidence :deep(.el-collapse-item__wrap) {
  border-bottom: 0;
}

.edhr-page-shell__technical-evidence-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.edhr-page-shell__technical-evidence-title {
  flex: 0 0 auto;
  color: #172033;
  font-size: 15px;
  font-weight: 600;
}

.edhr-page-shell__technical-evidence-hint {
  min-width: 0;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.edhr-page-shell__technical-evidence-body {
  padding: 0 16px 16px;
}

.edhr-page-shell__tracking-detail {
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.edhr-page-shell__tracking-params,
.edhr-page-shell__tracking-form,
.edhr-page-shell__tracking-audit {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-page-shell__tracking-params {
  position: sticky;
  top: 12px;
  min-width: 0;
}

.edhr-page-shell__tracking-param-list {
  margin-top: 12px;
}

.edhr-page-shell__tracking-param-list :deep(.el-descriptions__label),
.edhr-page-shell__tracking-param-list :deep(.el-descriptions__content) {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  line-height: 1.5;
}

.edhr-page-shell__tracking-form {
  min-width: 0;
}

.edhr-page-shell__tracking-audit {
  grid-column: 1 / -1;
  min-width: 0;
}

.edhr-page-shell__tracking-form :deep(.edhr-readonly-form) {
  margin-top: 12px;
}

.edhr-page-shell__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.edhr-page-shell__archive-summary {
  margin-top: 0;
}

.edhr-page-shell__archive-empty {
  padding: 8px 0 0;
}

.edhr-page-shell__form {
  padding-bottom: 0;
}

.edhr-page-shell__field-audit {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-page-shell__field-audit-reason {
  padding: 12px 12px 0;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #fafcff;
}

.edhr-page-shell__field-audit-empty {
  padding: 12px 0 4px;
  border: 1px dashed #dbe3ef;
  border-radius: 6px;
  background: #fafcff;
}

.edhr-page-shell__field-audit-empty-title {
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
}

.edhr-page-shell__field-audit-empty-desc {
  margin-top: 4px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.5;
}

.edhr-page-shell__pending-attachments {
  padding: 12px;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #ffffff;
}

.edhr-page-shell__pending-attachments-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-page-shell__pending-attachments-list {
  display: grid;
  gap: 8px;
}

.edhr-page-shell__pending-attachment-item {
  display: grid;
  grid-template-columns: minmax(160px, 0.8fr) minmax(220px, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #fafcff;
}

.edhr-page-shell__attachment-evidence {
  padding: 12px 16px;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #fafcff;
}

.edhr-page-shell__field-audit-table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-page-shell__field-audit-table :deep(.el-table__row) {
  height: 52px;
}

.edhr-page-shell__field-name {
  color: #172033;
  font-weight: 600;
}

.edhr-page-shell__field-path {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-page-shell__change-value {
  color: #172033;
  font-weight: 600;
}

.edhr-page-shell__change-arrow {
  margin: 2px 0;
  color: #4b5563;
  font-size: 12px;
}

.edhr-page-shell__evidence {
  padding: 12px 16px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #fafcff;
}

.edhr-page-shell__evidence-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-page-shell__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.edhr-page-shell__evidence-item {
  min-width: 0;
}

.edhr-page-shell__evidence-label {
  color: #4b5563;
  font-size: 12px;
}

.edhr-page-shell__evidence-value {
  margin-top: 4px;
  color: #172033;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.edhr-page-shell__typed-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.edhr-page-shell__signature-cell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fafcff;
}

.edhr-page-shell__signature-cell-status {
  min-width: 0;
}

.edhr-page-shell__signature-cell-text {
  display: block;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.edhr-page-shell__signature-cell-hint,
.edhr-page-shell__signature-cell-reason {
  display: block;
  margin-top: 3px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.edhr-page-shell__signature-cell-reason {
  grid-column: 1 / -1;
  color: #b45309;
}

.edhr-page-shell__attachment-field {
  width: 100%;
}

.edhr-page-shell__attachment-hint {
  margin-top: 8px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.6;
}

.edhr-page-shell__attachment-rule {
  display: block;
  margin-top: 4px;
  color: #b45309;
}

.edhr-page-shell__attachment-summary {
  display: grid;
  gap: 8px;
}

.edhr-page-shell__attachment-summary-item {
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #f9fafb;
}

.edhr-page-shell__unit {
  max-width: 80px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edhr-page-shell__signature-summary,
.edhr-page-shell__signature-form {
  margin-top: 12px;
}

:global(.edhr-fill-workspace__submit-sign-dialog .el-dialog) {
  max-width: calc(100vw - 32px);
  border-radius: 14px;
}

:global(.edhr-fill-workspace__submit-sign-dialog .el-dialog__header) {
  display: none;
}

:global(.edhr-fill-workspace__submit-sign-dialog .el-dialog__body) {
  padding: 34px 34px 22px;
}

:global(.edhr-fill-workspace__submit-sign-dialog .el-dialog__footer) {
  padding: 0 34px 32px;
}

.edhr-fill-workspace__submit-sign-form {
  display: grid;
  gap: 22px;
}

.edhr-fill-workspace__submit-sign-row {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  gap: 26px;
  align-items: center;
}

.edhr-fill-workspace__submit-sign-label {
  position: relative;
  color: #172033;
  font-size: 20px;
  font-weight: 800;
  line-height: 48px;
  text-align: left;
  white-space: nowrap;
}

.edhr-fill-workspace__submit-sign-label.is-required::before {
  content: '*';
  position: absolute;
  left: -14px;
  color: #f56c6c;
  font-size: 18px;
  line-height: 48px;
}

.edhr-fill-workspace__submit-sign-form :deep(.el-input__wrapper) {
  min-height: 48px;
  font-size: 20px;
}

.edhr-fill-workspace__submit-sign-name {
  min-height: 48px;
  display: flex;
  align-items: center;
  color: #172033;
  font-size: 24px;
  font-weight: 900;
}

.edhr-fill-workspace__submit-sign-confirm {
  width: 100%;
  height: 56px;
  border-radius: 10px;
  font-size: 20px;
  font-weight: 900;
}

:global(.edhr-fill-workspace__result-dialog .el-dialog) {
  max-width: calc(100vw - 32px);
  border-radius: 14px;
}

:global(.edhr-fill-workspace__result-dialog .el-dialog__header) {
  display: none;
}

:global(.edhr-fill-workspace__result-dialog .el-dialog__body) {
  padding: 36px 36px 24px;
}

:global(.edhr-fill-workspace__result-dialog .el-dialog__footer) {
  padding: 0 36px 34px;
}

.edhr-fill-workspace__result-body {
  display: grid;
  gap: 22px;
  text-align: center;
}

.edhr-fill-workspace__result-status {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  padding: 22px 18px;
  border: 2px solid #b7ebc6;
  border-radius: 12px;
  background: #f0fff4;
  color: #128044;
  font-size: 34px;
  font-weight: 900;
  line-height: 1.2;
}

.edhr-fill-workspace__result-body.is-danger .edhr-fill-workspace__result-status {
  border-color: #ffccc7;
  background: #fff1f0;
  color: #c00000;
}

.edhr-fill-workspace__result-context {
  display: grid;
  gap: 14px;
  color: #172033;
  font-size: 22px;
  line-height: 1.35;
  text-align: left;
}

.edhr-fill-workspace__result-context > div {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  padding: 14px 18px;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #fafcff;
}

.edhr-fill-workspace__result-context span {
  color: #4b5563;
  font-weight: 700;
}

.edhr-fill-workspace__result-context strong {
  min-width: 0;
  color: #172033;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.edhr-fill-workspace__result-confirm {
  width: 100%;
  height: 64px;
  border-radius: 10px;
  font-size: 24px;
  font-weight: 900;
}

@media (max-width: 1200px) {
  .edhr-page-shell__status-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .edhr-page-shell__status-item:nth-child(odd) {
    border-left: 0;
  }

  .edhr-page-shell__status-item:nth-child(n + 3) {
    border-top: 1px solid #edf1f6;
  }

  .edhr-page-shell__tracking-detail {
    grid-template-columns: 1fr;
  }

  .edhr-page-shell__tracking-params {
    position: static;
  }
}

@media (max-width: 720px) {
  .edhr-page-shell__status-overview {
    grid-template-columns: 1fr;
  }

  .edhr-page-shell__pending-attachment-item {
    grid-template-columns: 1fr;
  }

  .edhr-page-shell__signature-cell {
    grid-template-columns: 1fr;
  }

  .edhr-page-shell__status-item {
    border-top: 1px solid #edf1f6;
    border-left: 0;
  }

  .edhr-page-shell__status-item:first-child {
    border-top: 0;
  }
}
</style>
