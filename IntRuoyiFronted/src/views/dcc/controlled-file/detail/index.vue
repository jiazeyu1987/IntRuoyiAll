<template>
  <ContentWrap v-if="viewerMode">
    <div class="detail-viewer-page">
      <div class="detail-viewer-page__toolbar">
        <el-button @click="closeViewerMode">
          <Icon icon="ep:back" class="mr-5px" />
          返回
        </el-button>
        <el-tag type="info" effect="plain">只读预览态</el-tag>
        <el-tag v-if="isCurrentActiveVersion" type="success" effect="dark">
          当前有效版 / ACTIVE / {{ fileDetail?.versionNo || '-' }}
        </el-tag>
      </div>
      <div class="detail-viewer-split" data-testid="dcc-controlled-preview-layout">
        <section class="detail-viewer-split__file" data-testid="dcc-controlled-preview-file-pane">
          <ProtectedPdfViewer
            :controlled-file-id="controlledFileId"
            :title="fileDetail?.title || '受控文件预览'"
          />
        </section>
        <aside class="detail-viewer-split__detail" data-testid="dcc-controlled-preview-detail-pane">
          <ControlledFileBasicInfoPanel
            :file="fileDetail"
            :category-name="categoryNameMap.get(fileDetail?.categoryId || 0) || '-'"
            :directory-name="directoryNameMap.get(fileDetail?.directoryId || 0) || '-'"
            :requester-name="userNameMap.get(fileDetail?.requesterId || 0) || ''"
            :column="1"
            compact
            :show-edit="canEditMetadata && !!fileDetail"
            edit-button-text="修改"
            edit-test-id="dcc-controlled-preview-detail-edit"
            @open-dcc-project-code="openDccProjectCode"
            @edit="openMetadataDialog"
          />
        </aside>
      </div>
    </div>
  </ContentWrap>

  <template v-else>
    <ContentWrap>
      <div class="detail-header-shell">
        <div>
          <div class="flex items-center gap-10px">
            <div class="text-24px font-700">{{ fileDetail?.title || '受控文件详情' }}</div>
            <el-tag :type="getDetailStatusTagType(fileDetail?.status)">
              {{ getDetailStatusLabel(fileDetail?.status) }}
            </el-tag>
            <el-tag v-if="fileDetail?.modifying" type="warning">修改中</el-tag>
            <el-tag v-if="fileDetail?.status === 'SUPERSEDED'" type="info">历史版</el-tag>
            <el-tag v-if="isCurrentActiveVersion" type="success" effect="dark">
              当前有效版 / ACTIVE / {{ fileDetail?.versionNo || '-' }}
            </el-tag>
          </div>
          <div class="mt-8px flex flex-wrap items-center gap-10px text-13px text-[var(--el-text-color-secondary)]">
            <span>文件编号：{{ fileDetail?.fileNumber || '-' }}</span>
            <span>版本：{{ fileDetail?.versionNo || '-' }}</span>
            <span>生效日期：{{ formatControlledFileDate(fileDetail?.effectiveDate) }}</span>
          </div>
        </div>
        <div class="detail-action-bar">
          <div class="detail-action-group detail-action-group--primary">
            <el-button @click="router.back()">
              <Icon icon="ep:back" class="mr-5px" />
              返回
            </el-button>
            <el-button
              v-if="!isBrowserTraceabilityPage && canUploadApplicantTrainingRecord"
              type="primary"
              plain
              :loading="applicantTrainingRecordDialog.submitting"
              @click="openApplicantTrainingRecordDialog"
            >
              <Icon icon="ep:upload" class="mr-5px" />
              上传培训记录
            </el-button>
            <el-button
              v-if="!isBrowserTraceabilityPage && detailActionState.canAcknowledgeTraining"
              type="success"
              plain
              :loading="trainingAckLoading"
              @click="handleAcknowledgeTraining"
            >
              <Icon icon="ep:select" class="mr-5px" />
              确认培训
            </el-button>
            <el-button
              v-if="!isBrowserTraceabilityPage && detailActionState.canManualRelease"
              type="primary"
              plain
              :loading="manualReleaseLoading"
              @click="handleManualRelease"
            >
              <Icon icon="ep:promotion" class="mr-5px" />
              正式下发
            </el-button>
            <el-button
              v-if="!isBrowserTraceabilityPage && canSubmitPublishAction"
              type="primary"
              plain
              :loading="publishDialog.submitting"
              @click="openPublishDialog"
            >
              <Icon icon="ep:promotion" class="mr-5px" />
              发布申请
            </el-button>
            <el-button v-if="!isBrowserTraceabilityPage && detailActionState.canPreview" type="primary" plain @click="openPreview">
              <Icon icon="ep:view" class="mr-5px" />
              预览受控文件
            </el-button>
            <el-button
              v-if="!isBrowserTraceabilityPage && controlledPrintAllowed"
              v-hasPermi="['dcc:controlled-file:print']"
              type="primary"
              plain
              :loading="controlledPrintDialog.submitting"
              @click="openControlledPrintDialog"
            >
              <Icon icon="ep:printer" class="mr-5px" />
              受控打印
            </el-button>
          </div>
          <div class="detail-action-group detail-action-group--more" v-if="!isBrowserTraceabilityPage && hasDetailMoreActions">
            <el-dropdown trigger="click" @command="handleDetailMoreCommand">
              <el-button plain :loading="detailMoreActionLoading">
                更多
                <Icon icon="ep:arrow-down" class="ml-4px" />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="canOpenBpmDetail" command="bpm-detail">BPM 详情</el-dropdown-item>
                  <el-dropdown-item v-if="canEditMetadata && fileDetail" command="edit-metadata">
                    修改基础信息
                  </el-dropdown-item>
                  <el-dropdown-item v-if="fileDetail" command="print-process">流程打印</el-dropdown-item>
                  <el-dropdown-item v-if="fileDetail" command="export-process-word">
                    流程导出 Word
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="detail-action-group detail-action-group--danger" v-if="!isBrowserTraceabilityPage && hasDetailDangerActions">
            <el-dropdown trigger="click" @command="handleDetailDangerCommand">
              <el-button type="danger" plain :loading="detailDangerActionLoading">
                风险操作
                <Icon icon="ep:arrow-down" class="ml-4px" />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="canWithdraw" command="withdraw">撤回申请</el-dropdown-item>
                  <el-dropdown-item
                    v-if="detailActionState.canRetryFinalization && canRetryStampPermission"
                    command="retry-stamp"
                  >
                    重试发布
                  </el-dropdown-item>
                  <el-dropdown-item v-if="canSubmitObsoleteAction" command="obsolete">
                    作废当前版本
                  </el-dropdown-item>
                  <el-dropdown-item v-if="canHandleWithdrawnFlow" command="delete-withdrawn-flow" divided>
                    删除流程
                  </el-dropdown-item>
                  <el-dropdown-item v-if="canHandleWithdrawnFlow" command="resubmit-withdrawn-flow">
                    重新提交
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
      <div
        v-if="fileDetail && !isBrowserTraceabilityPage"
        class="detail-handling-summary"
        data-testid="dcc-detail-handling-summary"
      >
        <div class="detail-handling-summary__item">
          <div class="detail-handling-summary__label">页面模式</div>
          <div class="detail-handling-summary__value">
            {{ approvalTodoTask ? '待我审批/签名处理态' : '详情查看态' }}
          </div>
        </div>
        <div class="detail-handling-summary__item">
          <div class="detail-handling-summary__label">下一步</div>
          <div class="detail-handling-summary__value">
            {{ detailHandlingSummary.nextStep }}
          </div>
        </div>
        <div class="detail-handling-summary__item">
          <div class="detail-handling-summary__label">责任面</div>
          <div class="detail-handling-summary__value">
            {{ detailHandlingSummary.responsibilityHint }}
          </div>
        </div>
        <div class="detail-handling-summary__item">
          <div class="detail-handling-summary__label">当前阶段</div>
          <div class="detail-handling-summary__value">
            {{ currentStageLabel }}
          </div>
        </div>
        <div class="detail-handling-summary__item">
          <div class="detail-handling-summary__label">阻塞原因</div>
          <div class="detail-handling-summary__value detail-handling-summary__value--blocker">
            {{ detailBlockingReason }}
          </div>
        </div>
      </div>
      <el-alert
        v-if="!isBrowserTraceabilityPage && detailActionProjectionMessages.length"
        class="mt-12px"
        type="warning"
        :closable="false"
        show-icon
        :title="detailActionProjectionMessages.join('；')"
      />
      <el-alert
        v-if="!isBrowserTraceabilityPage && obsoleteActionLocked"
        class="mt-12px"
        data-testid="dcc-obsolete-action-lock"
        :closable="false"
        show-icon
        type="warning"
        :title="obsoleteActionLockTitle"
        :description="obsoleteActionLockDescription"
      >
        <template v-if="activeObsoleteAction?.bpmProcessInstanceId" #default>
          <el-button link type="primary" @click="openBpmDetail">查看 BPM 详情</el-button>
          <el-button
            link
            type="warning"
            :loading="obsoleteCancelLoading"
            @click="cancelActiveObsoleteAction"
          >
            撤回作废申请
          </el-button>
        </template>
      </el-alert>
      <el-alert
        v-if="!isBrowserTraceabilityPage && activeObsoleteActionError"
        class="mt-12px"
        data-testid="dcc-obsolete-action-lock-error"
        :closable="false"
        show-icon
        type="error"
        title="作废动作状态加载失败"
        :description="activeObsoleteActionError"
      />
      <el-alert
        v-if="!isBrowserTraceabilityPage && publishActionLocked"
        class="mt-12px"
        data-testid="dcc-publish-action-lock"
        :closable="false"
        show-icon
        type="warning"
        :title="publishActionLockTitle"
        :description="publishActionLockDescription"
      >
        <template v-if="activePublishAction?.bpmProcessInstanceId" #default>
          <el-button link type="primary" @click="openBpmDetail">查看 BPM 详情</el-button>
          <el-button
            link
            type="warning"
            :loading="publishCancelLoading"
            @click="cancelActivePublishAction"
          >
            撤回发布申请
          </el-button>
        </template>
      </el-alert>
      <el-alert
        v-if="!isBrowserTraceabilityPage && activePublishActionError"
        class="mt-12px"
        data-testid="dcc-publish-action-lock-error"
        :closable="false"
        show-icon
        type="error"
        title="发布动作状态加载失败"
        :description="activePublishActionError"
      />
      <el-alert
        v-if="!isBrowserTraceabilityPage && manualReleasePermissionGapVisible"
        class="mt-12px"
        data-testid="dcc-manual-release-permission-gap"
        :closable="false"
        show-icon
        type="warning"
        title="待正式下发：当前账号缺少正式下发权限"
        description="当前版本已进入待正式下发，但页面没有可用的正式下发动作。请为当前文控角色配置该文件类别的 DISTRIBUTE 分发规则和正式下发权限后再操作。"
      />
      <el-alert
        v-if="!isBrowserTraceabilityPage && controlledPrintPermissionHintVisible"
        class="mt-12px"
        data-testid="dcc-controlled-print-permission-hint"
        :closable="false"
        show-icon
        type="info"
        :title="controlledPrintPermissionHintTitle"
        :description="controlledPrintPermissionHintDescription"
      />
      <div
        v-if="showLifecycleTraceSections && fileAccessExplanation"
        class="detail-access-explanation"
        data-testid="dcc-detail-access-explanation"
      >
        <div class="detail-access-explanation__title">查阅权限说明</div>
        <div class="detail-access-explanation__grid">
          <div class="detail-access-explanation__item">
            <span class="detail-access-explanation__label">详情</span>
            <span class="detail-access-explanation__value">
              {{
                formatAccessExplanation(
                  fileAccessExplanation.detailSource,
                  fileAccessExplanation.detailReason || fileAccessExplanation.detailDeniedReason
                )
              }}
            </span>
          </div>
          <div class="detail-access-explanation__item">
            <span class="detail-access-explanation__label">已发布查看</span>
            <span class="detail-access-explanation__value">
              {{
                formatAccessExplanation(
                  fileAccessExplanation.publishedPreviewSource,
                  fileAccessExplanation.publishedPreviewReason
                )
              }}
            </span>
          </div>
          <div class="detail-access-explanation__item">
            <span class="detail-access-explanation__label">待审原件</span>
            <span class="detail-access-explanation__value">
              {{
                formatAccessExplanation(
                  fileAccessExplanation.pendingPreviewSource,
                  fileAccessExplanation.pendingPreviewReason
                )
              }}
            </span>
          </div>
          <div class="detail-access-explanation__item">
            <span class="detail-access-explanation__label">下载</span>
            <span class="detail-access-explanation__value">
              {{
                formatAccessExplanation(
                  fileAccessExplanation.downloadSource,
                  fileAccessExplanation.downloadReason || fileAccessExplanation.downloadDeniedReason
                )
              }}
            </span>
          </div>
        </div>
      </div>
      <el-alert
        v-if="showLifecycleTraceSections && accessExplanationError"
        class="mt-12px"
        :title="accessExplanationError"
        type="warning"
        :closable="false"
      />
    </ContentWrap>

    <template v-if="showLifecycleTraceSections">
    <ContentWrap data-testid="dcc-detail-project-code-linkage" class="mt-16px">
      <div class="detail-table-header mb-12px">
        <div>
          <div class="text-15px font-600">DCC 项目代码联动</div>
          <div class="mt-4px text-12px text-[var(--el-text-color-secondary)]">
            展示当前文件在 DCC 项目代码和文件分类树中的关联文档入口。
          </div>
        </div>
        <div class="detail-project-code-linkage-actions">
          <el-button
            type="primary"
            plain
            :disabled="!fileDetail?.dccProjectCodeId"
            @click="fileDetail?.dccProjectCodeId && openDccProjectCode(fileDetail.dccProjectCodeId)"
          >
            <Icon icon="ep:connection" class="mr-5px" />
            关联文档入口
          </el-button>
          <el-button plain :disabled="!fileDetail" @click="openDccProjectCodeTrace">
            <Icon icon="ep:document" class="mr-5px" />
            修正追溯入口
          </el-button>
        </div>
      </div>
      <div class="detail-project-code-linkage-grid">
        <div class="detail-project-code-linkage-card">
          <div class="detail-project-code-linkage-card__label">当前 DCC 项目</div>
          <div class="detail-project-code-linkage-card__value">{{ currentDccProjectCodeText }}</div>
        </div>
        <div class="detail-project-code-linkage-card">
          <div class="detail-project-code-linkage-card__label">当前文件分类</div>
          <div class="detail-project-code-linkage-card__value">{{ currentFileTypeTaxonomyText }}</div>
        </div>
        <div class="detail-project-code-linkage-card">
          <div class="detail-project-code-linkage-card__label">关联文件 ID</div>
          <div class="detail-project-code-linkage-card__value">{{ fileDetail?.id || '-' }}</div>
        </div>
        <div class="detail-project-code-linkage-card">
          <div class="detail-project-code-linkage-card__label">关联文档定位</div>
          <div class="detail-project-code-linkage-card__value">
            {{ fileDetail?.dccProjectCodeId ? '可跳转并定位当前文件类型' : '未绑定 DCC 项目代码' }}
          </div>
        </div>
      </div>
    </ContentWrap>

    <ContentWrap data-testid="dcc-detail-controlled-browser-linkage" class="mt-16px">
      <div class="detail-table-header mb-12px">
        <div>
          <div class="text-15px font-600">受控浏览入口</div>
          <div class="mt-4px text-12px text-[var(--el-text-color-secondary)]">
            原版审批生效后展示最终受控浏览目录、发布文件和盖章文件落位。
          </div>
        </div>
        <el-button type="primary" plain :disabled="!fileDetail" @click="openControlledBrowserLocation">
          <Icon icon="ep:position" class="mr-5px" />
          查看受控浏览当前有效版
        </el-button>
      </div>
      <div class="controlled-browser-linkage-grid">
        <div class="controlled-browser-linkage-card">
          <div class="controlled-browser-linkage-card__label">最终目录路径</div>
          <div class="controlled-browser-linkage-card__value">{{ controlledBrowserDirectoryPath }}</div>
        </div>
        <div class="controlled-browser-linkage-card">
          <div class="controlled-browser-linkage-card__label">发布文件</div>
          <div class="controlled-browser-linkage-card__value">{{ publishedFileBusinessText }}</div>
          <div class="controlled-browser-linkage-card__meta">高级信息：publishedFileId {{ fileDetail?.publishedFileId || '-' }}</div>
        </div>
        <div class="controlled-browser-linkage-card">
          <div class="controlled-browser-linkage-card__label">盖章文件</div>
          <div class="controlled-browser-linkage-card__value">{{ stampedFileBusinessText }}</div>
          <div class="controlled-browser-linkage-card__meta">高级信息：stampedFileId {{ fileDetail?.stampedFileId || '-' }}</div>
        </div>
        <div class="controlled-browser-linkage-card">
          <div class="controlled-browser-linkage-card__label">当前有效版来源（master 当前生效版本）</div>
          <div class="controlled-browser-linkage-card__value">{{ currentActiveVersionSourceText }}</div>
                <div class="controlled-browser-linkage-card__meta">高级信息：currentActiveVersionNo {{ fileDetail?.currentActiveVersionNo || '-' }}</div>
        </div>
      </div>
    </ContentWrap>

    <ContentWrap
      v-if="isPublishCompletionSummaryVisible"
      data-testid="dcc-detail-publish-completion-summary"
      class="mt-16px"
    >
      <div class="detail-table-header mb-12px">
        <div>
          <div class="text-15px font-600">发布完成结果</div>
          <div class="mt-4px text-12px text-[var(--el-text-color-secondary)]">
            新版 ACTIVE · 旧版 SUPERSEDED · master 当前生效版本 · 受控浏览落位 · 可见范围说明
          </div>
        </div>
        <el-button type="primary" plain :disabled="!fileDetail" @click="openControlledBrowserLocation">
          <Icon icon="ep:position" class="mr-5px" />
          查看受控浏览当前有效版
        </el-button>
      </div>
      <div class="publish-completion-summary-grid">
        <div
          v-for="item in publishCompletionSummaryItems"
          :key="item.key"
          class="publish-completion-summary-card"
          :class="{ 'is-ok': item.ok, 'is-warning': !item.ok }"
        >
          <div class="publish-completion-summary-card__header">
            <span>{{ item.label }}</span>
            <el-tag size="small" :type="item.ok ? 'success' : 'warning'">
              {{ item.ok ? '已确认' : '需核验' }}
            </el-tag>
          </div>
          <div class="publish-completion-summary-card__value">{{ item.value }}</div>
          <div class="publish-completion-summary-card__description">{{ item.description }}</div>
        </div>
      </div>
    </ContentWrap>

    <ContentWrap v-if="!isBrowserTraceabilityPage" v-loading="approvalLoading" class="mt-16px">
      <div class="mb-12px flex items-center justify-between gap-12px">
        <div class="text-15px font-600">审批阶段进度</div>
        <div class="text-13px text-[var(--el-text-color-secondary)]">
          当前阶段：{{ currentStageLabel }}
        </div>
      </div>
      <div class="stage-grid">
        <div
          v-for="stage in displayStageProgressList"
          :key="stage.stageCode"
          class="stage-card"
          :class="{
            'is-current': stage.isCurrent,
            'is-completed': stage.isCompleted
          }"
        >
          <div class="flex items-center justify-between gap-8px">
            <div class="font-600">{{ stage.stageName }}</div>
            <el-tag :type="stage.isCompleted ? 'success' : stage.isCurrent ? 'primary' : 'info'" size="small">
              {{ stage.isCompleted ? '已完成' : stage.isCurrent ? '当前阶段' : '待处理' }}
            </el-tag>
          </div>
          <div class="mt-10px text-22px font-700">{{ stage.completionText }}</div>
          <div class="mt-4px text-13px text-[var(--el-text-color-secondary)]">
            {{ stage.sameLayerHint }}
          </div>
          <div class="stage-card__meta">
            <span>处理人：{{ formatStageProgressActors(stage) }}</span>
            <span>处理时间：{{ formatStageProgressTime(stage) }}</span>
            <span>签名状态：{{ formatStageSignatureStatus(stage) }}</span>
          </div>
        </div>
      </div>

      <div
        v-if="approvalTodoTask"
        class="mt-16px rounded-8px border border-solid border-[var(--el-border-color)] p-16px"
      >
        <div class="text-15px font-600">待我审批/签名处理态：{{ approvalActionLabels.dialogTitle }}</div>
        <div class="mt-6px text-13px text-[var(--el-text-color-secondary)]">
          当前任务：{{ approvalTodoTask.name || '-' }}
        </div>
        <div class="mt-6px text-13px text-[var(--el-text-color-secondary)]">
          {{ currentStageSameLayerHint }}
        </div>
        <div v-if="isReturnedApplicantTask" class="mt-6px text-13px text-[var(--el-color-warning)]">
          有流程回退，需处理；处理后将继续提交原流程。
        </div>
        <div class="mt-14px flex flex-wrap gap-8px">
          <el-button type="primary" @click="openActionDialog('approve')">
            {{ approvalActionLabels.approveText }}
          </el-button>
          <el-button v-if="!isReturnedApplicantTask" type="danger" plain @click="openActionDialog('reject')">
            {{ approvalActionLabels.rejectText }}
          </el-button>
          <el-button v-if="returnTargetOptions.length > 0" plain @click="openTaskActionDialog('return')">
            <Icon icon="ep:back" class="mr-5px" />
            回退
          </el-button>
          <el-button v-if="!isReturnedApplicantTask" plain @click="openTaskActionDialog('transfer')">
            <Icon icon="fa:share-square-o" class="mr-5px" />
            转办
          </el-button>
          <el-button v-if="!isReturnedApplicantTask" plain @click="openTaskActionDialog('sign')">
            <Icon icon="ep:plus" class="mr-5px" />
            加签
          </el-button>
        </div>
      </div>
    </ContentWrap>

    <ContentWrap>
      <ControlledFileBasicInfoPanel
        :file="fileDetail"
        :category-name="categoryNameMap.get(fileDetail?.categoryId || 0) || '-'"
        :directory-name="directoryNameMap.get(fileDetail?.directoryId || 0) || '-'"
        :requester-name="userNameMap.get(fileDetail?.requesterId || 0) || ''"
        :column="2"
        :show-product-recognition="canEditMetadata && !!fileDetail"
        :project-code-recognition-loading="projectCodeRecognitionLoading"
        @recognize-project-code="handleRecognizeProjectCode"
        @open-dcc-project-code="openDccProjectCode"
      />
    </ContentWrap>

    <ContentWrap>
      <div class="mb-12px flex items-center justify-between gap-12px">
        <div class="text-15px font-600">关键记录时间线</div>
        <el-tag size="small" type="info">汇总 {{ detailLifecycleTimelineItems.length }} 项</el-tag>
      </div>
      <div class="detail-lifecycle-timeline" data-testid="dcc-detail-lifecycle-timeline">
        <template v-if="detailLifecycleTimelineItems.length">
          <div
            v-for="item in detailLifecycleTimelineItems"
            :key="item.key"
            class="detail-lifecycle-timeline__item"
          >
            <div class="detail-lifecycle-timeline__marker"></div>
            <div class="detail-lifecycle-timeline__content">
              <div class="detail-lifecycle-timeline__heading">
                <div class="detail-lifecycle-timeline__title">
                  {{ item.title }}
                </div>
                <el-tag :type="item.tagType" size="small">{{ item.categoryLabel }}</el-tag>
              </div>
              <div class="detail-lifecycle-timeline__time">{{ item.timeText }}</div>
              <div class="detail-lifecycle-timeline__description">{{ item.description }}</div>
              <div v-if="item.actorText" class="detail-lifecycle-timeline__actor">
                责任人：{{ item.actorText }}
              </div>
            </div>
          </div>
        </template>
        <el-empty v-else :image-size="72" description="暂无可汇总的时间记录" />
      </div>
    </ContentWrap>

    <ContentWrap v-if="fileDetail?.externalReview">
      <div class="mb-12px text-15px font-600">外来文件评审信息</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="外来来源">
          {{ fileDetail.externalReview.externalSource || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="外来归属">
          {{ fileDetail.externalReview.externalOwner || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="评审原因" :span="2">
          {{ fileDetail.externalReview.reviewReason || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="参与人" :span="2">
          {{ resolveUserNames(fileDetail.externalReview.participantUserIds || []) }}
        </el-descriptions-item>
        <el-descriptions-item label="评审结论">
          {{ fileDetail.externalReview.reviewConclusion || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="输出文件">
          {{ fileDetail.externalReview.outputFileName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="结论说明" :span="2">
          {{ fileDetail.externalReview.conclusionComment || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="闭环时间">
          {{ formatControlledFileDateTime(fileDetail.externalReview.closedTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </ContentWrap>

    <ContentWrap data-testid="dcc-detail-route-snapshot-section">
      <UnifiedListTemplate
        table-key="dcc.controlledFile.detail.routeSnapshot"
        :query-model="detailListQueryModel"
        :filter-definitions="detailListFilterDefinitions"
        :show-quick-filter="false"
        :quick-filter-state="detailListQuickFilterState"
        :operator-options="detailListOperatorOptions"
        :columns="routeSnapshotColumns"
        :column-saving="routeSnapshotColumnSaving"
        show-column-reset
        :total="routeSnapshotRows.length"
        v-model:page="routeSnapshotListState.pageNo"
        v-model:limit="routeSnapshotListState.pageSize"
        @column-change="saveRouteSnapshotColumnConfig"
        @column-reset="resetRouteSnapshotColumnConfig"
      >
        <template #actions>
          <div class="text-15px font-600">审批路线快照</div>
        </template>
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
      <el-table
        data-user-table-column-explicit
        data-user-table-key="dcc.controlledFile.detail.routeSnapshot"
        :data="pagedRouteSnapshotRows"
        border
        :stripe="true"
        :show-overflow-tooltip="true"
        empty-text="暂无路线快照"
        @header-dragend="handleRouteSnapshotHeaderDragend"
        @sort-change="handleTemplateSortChange"
      >
        <el-table-column
          v-if="isRouteSnapshotColumnVisible('stageDisplayName')"
          label="阶段"
          prop="stageDisplayName"
          :width="getRouteSnapshotColumnWidthString('stageDisplayName')"
          :min-width="getRouteSnapshotColumnMinWidthString('stageDisplayName', 240)"
          v-bind="sortColumnAttrs('stageDisplayName')"
        >
          <template #default="{ row }">
            <div class="route-snapshot-summary" data-testid="dcc-detail-route-snapshot-stage">
              <div class="route-snapshot-summary__title">{{ row.stageDisplayName }}</div>
              <div class="route-snapshot-summary__meta">{{ row.stageMetaText }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isRouteSnapshotColumnVisible('candidateText')"
          label="候选摘要"
          prop="candidateText"
          :width="getRouteSnapshotColumnWidthString('candidateText')"
          :min-width="getRouteSnapshotColumnMinWidthString('candidateText', 260)"
          v-bind="sortColumnAttrs('candidateText')"
        >
          <template #default="{ row }">
            <div class="route-snapshot-summary" data-testid="dcc-detail-route-snapshot-candidate">
              <div class="route-snapshot-summary__line">
                <el-tag size="small" type="primary">{{ row.candidateSourceLabel }}</el-tag>
                <span>{{ row.candidateText }}</span>
              </div>
              <div class="route-snapshot-summary__meta">来源：{{ row.candidateSourceLabel }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isRouteSnapshotColumnVisible('approvalRequirementText')"
          label="审批要求"
          prop="approvalRequirementText"
          :width="getRouteSnapshotColumnWidthString('approvalRequirementText')"
          :min-width="getRouteSnapshotColumnMinWidthString('approvalRequirementText', 260)"
          v-bind="sortColumnAttrs('approvalRequirementText')"
        >
          <template #default="{ row }">
            <div class="route-snapshot-summary" data-testid="dcc-detail-route-snapshot-requirement">
              <div class="route-snapshot-summary__line">
                <el-tag :type="row.approvalStatusTagType" size="small">
                  {{ row.approvalStatusLabel }}
                </el-tag>
                <span>{{ row.approveMethodLabel }}</span>
                <span v-if="row.approveRatioText !== '-'">比例 {{ row.approveRatioText }}</span>
              </div>
              <div class="route-snapshot-summary__meta">
                {{ row.approvalProgressText }} 已完成，{{ row.approvalHint }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isRouteSnapshotColumnVisible('resolvedUserNames')"
          label="解析审批人"
          prop="resolvedUserNames"
          :width="getRouteSnapshotColumnWidthString('resolvedUserNames')"
          :min-width="getRouteSnapshotColumnMinWidthString('resolvedUserNames', 260)"
          v-bind="sortColumnAttrs('resolvedUserNames')"
        >
          <template #default="{ row }">
            {{ row.resolvedUserNames }}
          </template>
        </el-table-column>
      </el-table>
        </template>
      </UnifiedListTemplate>
    </ContentWrap>

    <ContentWrap v-if="isVersionHistoryVisibleToReader(fileDetail?.status)">
      <UnifiedListTemplate
        table-key="dcc.controlledFile.detail.versionHistory"
        :query-model="detailListQueryModel"
        :filter-definitions="detailListFilterDefinitions"
        :show-quick-filter="false"
        :quick-filter-state="detailListQuickFilterState"
        :operator-options="detailListOperatorOptions"
        :columns="versionHistoryColumns"
        :column-saving="versionHistoryColumnSaving"
        show-column-reset
        :total="versionHistoryRows.length"
        v-model:page="versionHistoryListState.pageNo"
        v-model:limit="versionHistoryListState.pageSize"
        @column-change="saveVersionHistoryColumnConfig"
        @column-reset="resetVersionHistoryColumnConfig"
      >
        <template #actions>
          <div class="text-15px font-600">版本历史</div>
        </template>
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
      <el-table
        data-user-table-column-explicit
        data-user-table-key="dcc.controlledFile.detail.versionHistory"
        :data="pagedVersionHistoryRows"
        border
        :stripe="true"
        :show-overflow-tooltip="true"
        empty-text="暂无历史版本"
        @header-dragend="handleVersionHistoryHeaderDragend"
        @sort-change="handleTemplateSortChange"
      >
        <el-table-column
          v-if="isVersionHistoryColumnVisible('title')"
          label="标题"
          prop="title"
          show-overflow-tooltip
          :width="getVersionHistoryColumnWidthString('title')"
          :min-width="getVersionHistoryColumnMinWidthString('title', 220)"
          v-bind="sortColumnAttrs('title')"
        />
        <el-table-column
          v-if="isVersionHistoryColumnVisible('fileNumber')"
          label="文件编号"
          prop="fileNumber"
          :width="getVersionHistoryColumnWidthString('fileNumber')"
          :min-width="getVersionHistoryColumnMinWidthString('fileNumber', 150)"
          v-bind="sortColumnAttrs('fileNumber')"
        />
        <el-table-column
          v-if="isVersionHistoryColumnVisible('versionNo')"
          label="版本"
          align="center"
          prop="versionNo"
          :width="getVersionHistoryColumnWidthString('versionNo', 100)"
          v-bind="sortColumnAttrs('versionNo')"
        />
        <el-table-column
          v-if="isVersionHistoryColumnVisible('changeReasonText')"
          label="升版原因/变更说明"
          prop="changeReasonText"
          show-overflow-tooltip
          :width="getVersionHistoryColumnWidthString('changeReasonText')"
          :min-width="getVersionHistoryColumnMinWidthString('changeReasonText', 220)"
          v-bind="sortColumnAttrs('changeReasonText')"
        >
          <template #default="{ row }">
            <span data-testid="dcc-detail-version-history-change-reason">
              {{ row.changeReasonText }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isVersionHistoryColumnVisible('distributionMedium')"
          label="发放方式"
          prop="distributionMedium"
          :width="getVersionHistoryColumnWidthString('distributionMedium')"
          :min-width="getVersionHistoryColumnMinWidthString('distributionMedium', 140)"
          v-bind="sortColumnAttrs('distributionMedium')"
        >
          <template #default="{ row }">
            {{ getDistributionMediumLabel(row.distributionMedium) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="isVersionHistoryColumnVisible('status')"
          label="状态"
          align="center"
          prop="status"
          :width="getVersionHistoryColumnWidthString('status', 120)"
          v-bind="sortColumnAttrs('status')"
        >
          <template #default="{ row }">
            <el-tag :type="getDetailStatusTagType(row.status)">
              {{ getDetailStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isVersionHistoryColumnVisible('publishedTime')"
          label="发布时间"
          align="center"
          prop="publishedTime"
          :width="getVersionHistoryColumnWidthString('publishedTime', 180)"
          v-bind="sortColumnAttrs('publishedTime')"
        >
          <template #default="{ row }">
            {{ formatControlledFileDateTime(row.publishedTime) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="isVersionHistoryColumnVisible('obsoletedTime')"
          label="作废时间"
          align="center"
          prop="obsoletedTime"
          :width="getVersionHistoryColumnWidthString('obsoletedTime', 180)"
          v-bind="sortColumnAttrs('obsoletedTime')"
        >
          <template #default="{ row }">
            {{ formatControlledFileDateTime(row.obsoletedTime) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="isVersionHistoryColumnVisible('successorVersionSummary')"
          label="后继版本"
          prop="successorVersionSummary"
          show-overflow-tooltip
          :width="getVersionHistoryColumnWidthString('successorVersionSummary')"
          :min-width="getVersionHistoryColumnMinWidthString('successorVersionSummary', 220)"
          v-bind="sortColumnAttrs('successorVersionSummary')"
        >
          <template #default="{ row }">
            <span data-testid="dcc-detail-version-successor-summary">
              {{ getSuccessorVersionSummary(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isVersionHistoryColumnVisible('operation')"
          label="操作"
          align="center"
          prop="operation"
          :width="getVersionHistoryColumnWidthString('operation', 120)"
        >
          <template #default="{ row }">
            <el-button link type="primary" @click="openHistoryDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
        </template>
      </UnifiedListTemplate>
    </ContentWrap>

    <ContentWrap>
      <UnifiedListTemplate
        table-key="dcc.controlledFile.detail.distributionStatus"
        :query-model="detailListQueryModel"
        :filter-definitions="detailListFilterDefinitions"
        :show-quick-filter="false"
        :quick-filter-state="detailListQuickFilterState"
        :operator-options="detailListOperatorOptions"
        :columns="distributionStatusColumns"
        :column-saving="distributionStatusColumnSaving"
        show-column-reset
        :total="distributionStatusRows.length"
        v-model:page="distributionStatusListState.pageNo"
        v-model:limit="distributionStatusListState.pageSize"
        @column-change="saveDistributionStatusColumnConfig"
        @column-reset="resetDistributionStatusColumnConfig"
      >
        <template #actions>
          <div class="mb-12px flex items-center justify-between gap-12px">
            <div class="text-15px font-600">分发状态</div>
            <div class="flex flex-wrap gap-8px">
              <el-button
                plain
                :disabled="!distributionReceiptRows.length"
                @click="handleExportDistributionReceipts"
              >
                <Icon icon="ep:download" class="mr-5px" />
                导出回执
              </el-button>
              <el-button
                plain
                :disabled="!distributionReceiptRows.length"
                @click="handlePrintDistributionReceipts"
              >
                <Icon icon="ep:printer" class="mr-5px" />
                打印回执
              </el-button>
            </div>
          </div>
        </template>
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
      <el-table
        data-user-table-column-explicit
        data-user-table-key="dcc.controlledFile.detail.distributionStatus"
        :data="pagedDistributionStatusRows"
        data-testid="dcc-detail-distribution-section"
        border
        :stripe="true"
        :show-overflow-tooltip="true"
        empty-text="当前版本暂无分发记录"
        @header-dragend="handleDistributionStatusHeaderDragend"
        @sort-change="handleTemplateSortChange"
      >
        <el-table-column
          v-if="isDistributionStatusColumnVisible('departmentName')"
          label="部门"
          prop="departmentName"
          :width="getDistributionStatusColumnWidthString('departmentName')"
          :min-width="getDistributionStatusColumnMinWidthString('departmentName', 180)"
          v-bind="sortColumnAttrs('departmentName')"
        >
          <template #default="{ row }">
            {{ row.departmentName }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="isDistributionStatusColumnVisible('recipientText')"
          label="接收人"
          prop="recipientText"
          :width="getDistributionStatusColumnWidthString('recipientText')"
          :min-width="getDistributionStatusColumnMinWidthString('recipientText', 280)"
          v-bind="sortColumnAttrs('recipientText')"
        >
          <template #default="{ row }">
            {{ row.recipientText }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="isDistributionStatusColumnVisible('distributionSummaryText')"
          label="分发摘要"
          prop="distributionSummaryText"
          :width="getDistributionStatusColumnWidthString('distributionSummaryText')"
          :min-width="getDistributionStatusColumnMinWidthString('distributionSummaryText', 300)"
          v-bind="sortColumnAttrs('distributionSummaryText')"
        >
          <template #default="{ row }">
            <div class="detail-distribution-summary" data-testid="dcc-detail-distribution-summary">
              <div class="detail-distribution-summary__line">
                <el-tag :type="getDistributionStatusTagType(row.status)" size="small">
                  {{ getDistributionStatusLabel(row.status) }}
                </el-tag>
                <span class="detail-distribution-summary__medium">
                  {{ getDistributionMediumLabel(row.distributionMedium) }}
                </span>
              </div>
              <div class="detail-distribution-summary__meta">
                发放：{{ getDistributionAckUserSummary(row, userNameMap) }}
              </div>
              <div class="detail-distribution-summary__meta">
                时间：{{ formatControlledFileDateTime(row.acknowledgedAt) }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isDistributionStatusColumnVisible('recoverySummaryText')"
          label="回收摘要"
          prop="recoverySummaryText"
          :width="getDistributionStatusColumnWidthString('recoverySummaryText')"
          :min-width="getDistributionStatusColumnMinWidthString('recoverySummaryText', 260)"
          v-bind="sortColumnAttrs('recoverySummaryText')"
        >
          <template #default="{ row }">
            <div class="detail-recovery-summary" data-testid="dcc-detail-recovery-summary">
              <div class="detail-recovery-summary__meta">
                回收：{{ getDistributionRecoverUserSummary(row) }}
              </div>
              <div class="detail-recovery-summary__meta">
                时间：{{ formatControlledFileDateTime(row.recoveredAt) }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isDistributionStatusColumnVisible('operation')"
          label="操作"
          align="center"
          prop="operation"
          :width="getDistributionStatusColumnWidthString('operation', 300)"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.distributionMedium === 'PUBLIC_FOLDER' && getCurrentElectronicReceiptRecipient(row)"
              link
              type="primary"
              :loading="electronicReceiptLoadingRecipientId === getCurrentElectronicReceiptRecipient(row)?.id"
              @click="openElectronicReceiptDialog(row)"
            >
              确认签收
            </el-button>
            <el-button
              v-if="row.distributionMedium === 'PUBLIC_FOLDER' && getCurrentDistributionRecipient(row)"
              link
              type="primary"
              :loading="distributionSignLoadingRecipientId === getCurrentDistributionRecipient(row)?.id"
              @click="openDistributionSignDialog(row)"
            >
              接收人加签
            </el-button>
            <el-button
              v-if="row.distributionMedium === 'PAPER' && row.status !== 'ACKNOWLEDGED' && row.status !== 'RECOVERED'"
              link
              type="primary"
              :loading="paperDistributionAckLoadingId === row.id"
              @click="handleAcknowledgePaperDistribution(row.id)"
            >
              确认纸质发放
            </el-button>
            <el-button
              v-if="row.distributionMedium === 'PAPER' && row.status === 'ACKNOWLEDGED'"
              link
              type="warning"
              :loading="paperDistributionRecoverLoadingId === row.id"
              @click="handleRecoverPaperDistribution(row.id)"
            >
              确认回收
            </el-button>
            <span v-if="!hasDistributionRowAction(row)">-</span>
          </template>
        </el-table-column>
      </el-table>
        </template>
      </UnifiedListTemplate>
    </ContentWrap>

    <ContentWrap data-testid="dcc-controlled-print-records">
      <UnifiedListTemplate
        table-key="dcc.controlledFile.detail.controlledPrintRecords"
        :query-model="detailListQueryModel"
        :filter-definitions="detailListFilterDefinitions"
        :show-quick-filter="false"
        :quick-filter-state="detailListQuickFilterState"
        :operator-options="detailListOperatorOptions"
        :columns="controlledPrintRecordColumns"
        :column-saving="controlledPrintRecordColumnSaving"
        show-column-reset
        :total="controlledPrintRecordRows.length"
        v-model:page="controlledPrintRecordListState.pageNo"
        v-model:limit="controlledPrintRecordListState.pageSize"
        @column-change="saveControlledPrintRecordColumnConfig"
        @column-reset="resetControlledPrintRecordColumnConfig"
      >
        <template #actions>
          <div class="detail-table-header">
            <div>
              <div class="text-15px font-600">受控打印记录</div>
              <div class="mt-4px text-12px text-[var(--el-text-color-secondary)]">
                仅当前有效版本可登记受控打印，打印件需包含打印编号、文件编号、版本、打印人和打印时间。
              </div>
            </div>
            <el-button
              v-if="controlledPrintAllowed"
              v-hasPermi="['dcc:controlled-file:print']"
              type="primary"
              plain
              :loading="controlledPrintDialog.submitting"
              @click="openControlledPrintDialog"
            >
              <Icon icon="ep:printer" class="mr-5px" />
              受控打印
            </el-button>
          </div>
        </template>
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
          <el-alert
            class="mb-12px"
            data-testid="dcc-controlled-print-policy-hint"
            type="info"
            title="当前策略：直接受控打印"
            description="当前文件类别无需打印审批，提交后直接生成受控打印件，并以 DIRECT_PRINTED 状态写入打印记录。"
            show-icon
            :closable="false"
          />
          <el-alert
            v-if="controlledPrintRecordsError"
            type="error"
            :title="controlledPrintRecordsError"
            show-icon
            :closable="false"
            class="mb-12px"
          />
          <el-table
            ref="controlledPrintRecordsTableRef"
            data-user-table-column-explicit
            data-user-table-key="dcc.controlledFile.detail.controlledPrintRecords"
            :data="pagedControlledPrintRecordRows"
            :loading="controlledPrintRecordsLoading"
            :row-key="(row) => row.id"
            :row-class-name="getControlledPrintRecordRowClassName"
            border
            :stripe="true"
            :show-overflow-tooltip="true"
            empty-text="当前版本暂无受控打印记录"
            @header-dragend="handleControlledPrintRecordHeaderDragend"
            @sort-change="handleTemplateSortChange"
          >
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('printNo')"
              label="打印编号"
              prop="printNo"
              show-overflow-tooltip
              :width="getControlledPrintRecordColumnWidthString('printNo')"
              :min-width="getControlledPrintRecordColumnMinWidthString('printNo', 210)"
              v-bind="sortColumnAttrs('printNo')"
            >
              <template #default="{ row }">
                <span :data-controlled-print-record-id="row.id">{{ row.printNo }}</span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('fileNumber')"
              label="文件编号"
              prop="fileNumber"
              show-overflow-tooltip
              :width="getControlledPrintRecordColumnWidthString('fileNumber')"
              :min-width="getControlledPrintRecordColumnMinWidthString('fileNumber', 160)"
              v-bind="sortColumnAttrs('fileNumber')"
            />
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('versionNo')"
              label="版本"
              align="center"
              prop="versionNo"
              :width="getControlledPrintRecordColumnWidthString('versionNo', 100)"
              v-bind="sortColumnAttrs('versionNo')"
            />
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('copies')"
              label="份数"
              align="center"
              prop="copies"
              :width="getControlledPrintRecordColumnWidthString('copies', 90)"
              v-bind="sortColumnAttrs('copies')"
            />
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('copyNumbers')"
              label="副本编号"
              prop="copyNumbers"
              show-overflow-tooltip
              :width="getControlledPrintRecordColumnWidthString('copyNumbers')"
              :min-width="getControlledPrintRecordColumnMinWidthString('copyNumbers', 220)"
              v-bind="sortColumnAttrs('copyNumbers')"
            >
              <template #default="{ row }">
                <span data-testid="dcc-controlled-print-record-copy-nos">
                  {{ row.copyNumbers }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('purpose')"
              label="打印用途"
              prop="purpose"
              show-overflow-tooltip
              :width="getControlledPrintRecordColumnWidthString('purpose')"
              :min-width="getControlledPrintRecordColumnMinWidthString('purpose', 180)"
              v-bind="sortColumnAttrs('purpose')"
            />
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('receivingDepartment')"
              label="接收部门"
              prop="receivingDepartment"
              show-overflow-tooltip
              :width="getControlledPrintRecordColumnWidthString('receivingDepartment')"
              :min-width="getControlledPrintRecordColumnMinWidthString('receivingDepartment', 140)"
              v-bind="sortColumnAttrs('receivingDepartment')"
            />
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('useLocation')"
              label="使用位置"
              prop="useLocation"
              show-overflow-tooltip
              :width="getControlledPrintRecordColumnWidthString('useLocation')"
              :min-width="getControlledPrintRecordColumnMinWidthString('useLocation', 140)"
              v-bind="sortColumnAttrs('useLocation')"
            />
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('printUserName')"
              label="打印人"
              prop="printUserName"
              show-overflow-tooltip
              :width="getControlledPrintRecordColumnWidthString('printUserName')"
              :min-width="getControlledPrintRecordColumnMinWidthString('printUserName', 180)"
              v-bind="sortColumnAttrs('printUserName')"
            />
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('printTime')"
              label="打印时间"
              align="center"
              prop="printTime"
              :width="getControlledPrintRecordColumnWidthString('printTime', 180)"
              v-bind="sortColumnAttrs('printTime')"
            >
              <template #default="{ row }">
                {{ formatControlledFileDateTime(row.printTime) }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="isControlledPrintRecordColumnVisible('approvalStatus')"
              label="审批/打印状态"
              align="center"
              prop="approvalStatus"
              :width="getControlledPrintRecordColumnWidthString('approvalStatus', 150)"
              v-bind="sortColumnAttrs('approvalStatus')"
            >
              <template #default="{ row }">
                <el-tag type="success">{{ row.approvalStatusText }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </UnifiedListTemplate>
    </ContentWrap>

    <ContentWrap data-testid="dcc-detail-training-section">
      <UnifiedListTemplate
        table-key="dcc.controlledFile.detail.trainingStatus"
        :query-model="detailListQueryModel"
        :filter-definitions="detailListFilterDefinitions"
        :show-quick-filter="false"
        :quick-filter-state="detailListQuickFilterState"
        :operator-options="detailListOperatorOptions"
        :columns="trainingStatusColumns"
        :column-saving="trainingStatusColumnSaving"
        show-column-reset
        :total="trainingStatusRows.length"
        v-model:page="trainingStatusListState.pageNo"
        v-model:limit="trainingStatusListState.pageSize"
        @column-change="saveTrainingStatusColumnConfig"
        @column-reset="resetTrainingStatusColumnConfig"
      >
        <template #actions>
          <div class="flex items-center gap-12px">
            <div class="text-15px font-600">培训状态</div>
            <el-tag v-if="pendingTrainingAssignments.length" type="warning">
              当前用户待确认 {{ pendingTrainingAssignments.length }} 项
            </el-tag>
          </div>
        </template>
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
          <div class="detail-training-overview" data-testid="dcc-detail-training-completion-overview">
            <div class="detail-training-overview__item">
              <div class="detail-training-overview__label">完成进度</div>
              <div class="detail-training-overview__value">{{ trainingCompletionSummary.completionText }}</div>
            </div>
            <div class="detail-training-overview__item">
              <div class="detail-training-overview__label">最近确认时间</div>
              <div class="detail-training-overview__value">
                {{ trainingCompletionSummary.latestAcknowledgedAtText }}
              </div>
            </div>
            <div
              class="detail-training-overview__item detail-training-overview__item--wide"
              data-testid="dcc-detail-training-pending-users"
            >
              <div class="detail-training-overview__label">未完成人员</div>
              <div class="detail-training-overview__value">
                {{ trainingCompletionSummary.pendingNamesText }}
              </div>
            </div>
          </div>
          <el-table
            data-user-table-column-explicit
            data-user-table-key="dcc.controlledFile.detail.trainingStatus"
            :data="pagedTrainingStatusRows"
            border
            :stripe="true"
            :show-overflow-tooltip="true"
            empty-text="当前版本暂无培训记录"
            @header-dragend="handleTrainingStatusHeaderDragend"
            @sort-change="handleTemplateSortChange"
          >
            <el-table-column
              v-if="isTrainingStatusColumnVisible('departmentName')"
              label="部门"
              prop="departmentName"
              :width="getTrainingStatusColumnWidthString('departmentName')"
              :min-width="getTrainingStatusColumnMinWidthString('departmentName', 180)"
              v-bind="sortColumnAttrs('departmentName')"
            >
              <template #default="{ row }">
                {{ row.departmentName }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="isTrainingStatusColumnVisible('traineeName')"
              label="受训人"
              prop="traineeName"
              :width="getTrainingStatusColumnWidthString('traineeName')"
              :min-width="getTrainingStatusColumnMinWidthString('traineeName', 220)"
              v-bind="sortColumnAttrs('traineeName')"
            >
              <template #default="{ row }">
                {{ row.traineeName }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="isTrainingStatusColumnVisible('trainingSummaryText')"
              label="培训摘要"
              prop="trainingSummaryText"
              :width="getTrainingStatusColumnWidthString('trainingSummaryText')"
              :min-width="getTrainingStatusColumnMinWidthString('trainingSummaryText', 360)"
              v-bind="sortColumnAttrs('trainingSummaryText')"
            >
              <template #default="{ row }">
                <div class="detail-training-summary">
                  <div class="detail-training-summary__line">
                    <el-tag :type="row.trainingSummary.statusTagType" size="small">
                      {{ row.trainingSummary.statusLabel }}
                    </el-tag>
                    <span class="detail-training-summary__progress">
                      {{ row.trainingSummary.progressText }}
                    </span>
                  </div>
                  <div class="detail-training-summary__line">
                    <el-tag :type="row.trainingSummary.eligibilityTagType" size="small">
                      {{ row.trainingSummary.eligibilityLabel }}
                    </el-tag>
                    <el-tag :type="row.trainingSummary.departmentStatusTagType" size="small">
                      部门：{{ row.trainingSummary.departmentStatusLabel }}
                    </el-tag>
                    <span class="detail-training-summary__time">
                      确认：{{ row.trainingSummary.acknowledgedAtText }}
                    </span>
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </UnifiedListTemplate>
    </ContentWrap>
    </template>

    <template v-if="showSignatureTraceSections">
    <ContentWrap data-testid="dcc-detail-signature-trace-section" data-source="fileDetail?.signatureSummaries">
      <UnifiedListTemplate
        table-key="dcc.controlledFile.detail.signatureTrace"
        :query-model="detailListQueryModel"
        :filter-definitions="detailListFilterDefinitions"
        :show-query-form="false"
        :show-quick-filter="false"
        :quick-filter-state="detailListQuickFilterState"
        :operator-options="detailListOperatorOptions"
        :columns="signatureTraceColumns"
        :total="signatureTraceRows.length"
        v-model:page="signatureTraceListState.pageNo"
        v-model:limit="signatureTraceListState.pageSize"
      >
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
          <el-table
            data-user-table-column-explicit
            data-user-table-key="dcc.controlledFile.detail.signatureTrace"
            :data="pagedSignatureTraceRows"
            border
            :stripe="true"
            :show-overflow-tooltip="true"
            empty-text="暂无签核追溯记录"
            @header-dragend="handleSignatureTraceHeaderDragend"
            @sort-change="handleTemplateSortChange"
          >
            <el-table-column
              v-if="isSignatureTraceColumnVisible('traceRole')"
              label="角色"
              prop="traceRole"
              :width="getSignatureTraceColumnWidthString('traceRole')"
              :min-width="getSignatureTraceColumnMinWidthString('traceRole', 120)"
              v-bind="sortColumnAttrs('traceRole')"
            />
            <el-table-column
              v-if="isSignatureTraceColumnVisible('actorName')"
              label="上传人 / 四级审批人"
              prop="actorName"
              show-overflow-tooltip
              :width="getSignatureTraceColumnWidthString('actorName')"
              :min-width="getSignatureTraceColumnMinWidthString('actorName', 220)"
              v-bind="sortColumnAttrs('actorName')"
            />
            <el-table-column
              v-if="isSignatureTraceColumnVisible('approvalCommentText')"
              label="审批意见"
              prop="approvalCommentText"
              show-overflow-tooltip
              :width="getSignatureTraceColumnWidthString('approvalCommentText')"
              :min-width="getSignatureTraceColumnMinWidthString('approvalCommentText', 220)"
              v-bind="sortColumnAttrs('approvalCommentText')"
            />
            <el-table-column
              v-if="isSignatureTraceColumnVisible('signedAtText')"
              label="签名时间"
              align="center"
              prop="signedAtText"
              :width="getSignatureTraceColumnWidthString('signedAtText', 180)"
              v-bind="sortColumnAttrs('signedAtText')"
            />
            <el-table-column
              v-if="isSignatureTraceColumnVisible('signatureModeText')"
              label="签名方式"
              align="center"
              prop="signatureModeText"
              :width="getSignatureTraceColumnWidthString('signatureModeText', 140)"
              v-bind="sortColumnAttrs('signatureModeText')"
            />
            <el-table-column
              v-if="isSignatureTraceColumnVisible('evidenceStatusText')"
              label="证据状态"
              align="center"
              prop="evidenceStatusText"
              :width="getSignatureTraceColumnWidthString('evidenceStatusText', 140)"
              v-bind="sortColumnAttrs('evidenceStatusText')"
            />
            <el-table-column
              v-if="isSignatureTraceColumnVisible('fileHashText')"
              label="文件哈希"
              prop="fileHashText"
              show-overflow-tooltip
              :width="getSignatureTraceColumnWidthString('fileHashText')"
              :min-width="getSignatureTraceColumnMinWidthString('fileHashText', 160)"
              v-bind="sortColumnAttrs('fileHashText')"
            />
            <el-table-column
              v-if="isSignatureTraceColumnVisible('fileEvidenceText')"
              label="盖章文件 / 发布文件证据"
              prop="fileEvidenceText"
              show-overflow-tooltip
              :width="getSignatureTraceColumnWidthString('fileEvidenceText')"
              :min-width="getSignatureTraceColumnMinWidthString('fileEvidenceText', 260)"
              v-bind="sortColumnAttrs('fileEvidenceText')"
            >
              <template #default="{ row }">
                <div class="trace-file-evidence">
                  <span>{{ row.fileEvidenceText }}</span>
                  <el-button
                    v-if="row.hasFileEvidence"
                    link
                    type="primary"
                    data-testid="dcc-signature-trace-file-evidence"
                    @click="openTraceFileEvidence(row)"
                  >
                    查看盖章/发布文件
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </UnifiedListTemplate>
    </ContentWrap>

    <ContentWrap data-testid="dcc-detail-signature-section">
      <div class="mb-12px text-15px font-600">签名留痕</div>
      <el-alert
        v-if="dccSignatureEvidenceError"
        class="mb-12px"
        type="warning"
        show-icon
        :closable="false"
        :title="dccSignatureEvidenceError"
      />
      <el-table
        v-loading="dccSignatureEvidenceLoading"
        data-user-table-column-explicit
        data-user-table-key="dcc.controlledFile.detail.signatureEvidence"
        :data="dccSignatureEvidenceList"
        empty-text="暂无签名记录"
        @header-dragend="handleDccSignatureEvidenceHeaderDragend"
      >
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('versionNo')" label="版本" prop="versionNo" align="center" :width="getDccSignatureEvidenceColumnWidthString('versionNo', 110)">
          <template #default="{ row }">
            {{ row.versionNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('signer')" label="签名人" prop="signer" :min-width="getDccSignatureEvidenceColumnMinWidthString('signer', 220)">
          <template #default="{ row }">
            <div>{{ getSignatureActorSummary(row, userNameMap) }}</div>
            <div class="signature-snapshot-muted">
              {{ row.actorUsernameSnapshot || '旧版证据未记录' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('departmentPost')" label="部门/岗位" prop="departmentPost" :min-width="getDccSignatureEvidenceColumnMinWidthString('departmentPost', 190)">
          <template #default="{ row }">
            <div>{{ formatSignatureSnapshotValue(row.actorDeptNameSnapshot) }}</div>
            <div class="signature-snapshot-muted">
              {{ formatSignatureSnapshotValue(row.actorPostNamesSnapshot) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('role')" label="角色" prop="role" :min-width="getDccSignatureEvidenceColumnMinWidthString('role', 150)">
          <template #default="{ row }">
            {{ formatSignatureSnapshotValue(row.actorRoleNamesSnapshot) }}
          </template>
        </el-table-column>
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('action')" label="动作" prop="action" align="center" :width="getDccSignatureEvidenceColumnWidthString('action', 120)">
          <template #default="{ row }">
            {{ getSignatureActionLabel(row.taskActionResult) }}
          </template>
        </el-table-column>
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('meaning')" label="签名含义" prop="meaning" align="center" :width="getDccSignatureEvidenceColumnWidthString('meaning', 140)">
          <template #default="{ row }">
            <el-tag size="small" type="primary">
              {{ getSignatureMeaningLabel(row.meaningCode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('purpose')" label="签名目的" prop="purpose" :min-width="getDccSignatureEvidenceColumnMinWidthString('purpose', 150)">
          <template #default="{ row }">
            {{ formatSignatureSnapshotValue(row.signaturePurpose || row.meaningCode) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="isDccSignatureEvidenceColumnVisible('authorizationBasis')"
          prop="authorizationBasis"
          label="权限依据"
          min-width="240"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ formatSignatureSnapshotValue(row.authorizationBasis) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="isDccSignatureEvidenceColumnVisible('signatureMode')"
          label="签名方式"
          align="center"
          width="120"
          prop="signatureMode"
        />
        <el-table-column
          v-if="isDccSignatureEvidenceColumnVisible('sourceFileHash')"
          prop="sourceFileHash"
          label="源文件 hash"
          align="center"
          width="150"
        >
          <template #default="{ row }">
            <span class="signature-hash">{{ formatSignatureHashShort(row.sourceFileHashShort) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('controlledCopy')" label="受控副本" prop="controlledCopy" align="center" :width="getDccSignatureEvidenceColumnWidthString('controlledCopy', 130)">
          <template #default="{ row }">
            <el-tag :type="getControlledCopyHashStatusTagType(row.controlledCopyHashStatus)" size="small">
              {{ getControlledCopyHashStatusLabel(row.controlledCopyHashStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isDccSignatureEvidenceColumnVisible('controlledCopyHash')"
          prop="controlledCopyHash"
          label="副本 hash"
          align="center"
          width="150"
        >
          <template #default="{ row }">
            <span class="signature-hash">{{ formatSignatureHashShort(row.controlledCopyHashShort) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('evidenceStatus')" label="证据状态" prop="evidenceStatus" align="center" :width="getDccSignatureEvidenceColumnWidthString('evidenceStatus', 120)">
          <template #default="{ row }">
            <el-tag :type="getSignatureEvidenceStatusTagType(row.evidenceStatus)" size="small">
              {{ getSignatureEvidenceStatusLabel(row.evidenceStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isDccSignatureEvidenceColumnVisible('evidenceHash')"
          prop="evidenceHash"
          label="证据 hash"
          align="center"
          width="150"
        >
          <template #default="{ row }">
            <span class="signature-hash">{{ formatSignatureHashShort(row.evidenceHashShort) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('comment')" label="签名意见" :min-width="getDccSignatureEvidenceColumnMinWidthString('comment', 240)" prop="comment" show-overflow-tooltip />
        <el-table-column v-if="isDccSignatureEvidenceColumnVisible('signedAt')" label="签名时间" prop="signedAt" align="center" :width="getDccSignatureEvidenceColumnWidthString('signedAt', 180)">
          <template #default="{ row }">
            {{ formatControlledFileDateTime(row.signedAt) }}
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        :total="dccSignatureEvidenceTotal"
        v-model:page="dccSignatureEvidenceQueryParams.pageNo"
        v-model:limit="dccSignatureEvidenceQueryParams.pageSize"
        @pagination="loadDccSignatureEvidenceList"
      />
    </ContentWrap>
    </template>

    <el-dialog
      v-model="applicantTrainingRecordDialog.visible"
      title="上传培训记录"
      width="520px"
      destroy-on-close
    >
      <el-alert
        v-if="applicantTrainingRecordDialog.inlineError"
        :closable="false"
        class="mb-16px"
        show-icon
        type="error"
        :title="applicantTrainingRecordDialog.inlineError"
      />
      <el-form label-width="96px">
        <el-form-item label="培训记录" :error="applicantTrainingRecordDialog.fieldErrors.trainingRecordUploadTicket">
          <div class="w-full">
            <el-upload
              ref="applicantTrainingRecordUploadRef"
              action="#"
              :auto-upload="false"
              :limit="1"
              :file-list="applicantTrainingRecordFileList"
              :on-change="handleApplicantTrainingRecordChange"
              :before-remove="handleBeforeApplicantTrainingRecordRemove"
              :on-remove="handleApplicantTrainingRecordRemove"
              :on-exceed="handleApplicantTrainingRecordExceed"
            >
              <el-button plain :loading="applicantTrainingRecordDialog.uploading">
                <Icon icon="ep:upload" class="mr-5px" />
                选择文件
              </el-button>
            </el-upload>
            <div
              v-if="applicantTrainingRecordDialog.file"
              class="mt-8px text-13px text-[var(--el-text-color-secondary)]"
            >
              {{ applicantTrainingRecordDialog.file.fileName }}
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeApplicantTrainingRecordDialog">取消</el-button>
        <el-button
          type="primary"
          :loading="applicantTrainingRecordDialog.submitting"
          @click="submitApplicantTrainingRecordDialog"
        >
          确认上传
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="actionDialog.visible"
      :title="approvalActionLabels.dialogTitle"
      width="520px"
      destroy-on-close
    >
      <el-alert
        v-if="actionDialog.inlineError"
        :closable="false"
        class="mb-16px"
        show-icon
        type="error"
        :title="actionDialog.inlineError"
      />
      <el-descriptions class="mb-16px" :column="2" border>
        <el-descriptions-item label="文件编号">
          {{ fileDetail?.fileNumber || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="版本">
          {{ fileDetail?.versionNo || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="当前阶段">
          {{ currentStageLabel }}
        </el-descriptions-item>
        <el-descriptions-item label="签名含义">
          {{ actionDialogSignatureMeaning }}
        </el-descriptions-item>
        <el-descriptions-item label="签名人">
          {{ currentSignerLabel }}
        </el-descriptions-item>
        <el-descriptions-item label="签名动作">
          {{ actionDialog.mode === 'reject' ? '驳回签名' : '通过签名' }}
        </el-descriptions-item>
        <el-descriptions-item label="任务ID" :span="2">
          {{ approvalTodoTask?.id || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="提交后流转" :span="2">
          {{ actionDialogSubmitFlowText }}
        </el-descriptions-item>
        <el-descriptions-item label="电子签名审计证据" :span="2">
          本次签名会写入电子签名审计证据，包含签名人、节点、动作、时间、哈希与审计快照。
        </el-descriptions-item>
        <el-descriptions-item label="证据前置" :span="2">
          版本、源文件摘要、受控副本摘要状态
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="96px">
        <el-form-item label="登录密码" :error="actionDialog.fieldErrors.password">
          <el-input
            v-model="actionDialog.form.password"
            clearable
            placeholder="请输入当前登录密码"
            show-password
            type="password"
          />
        </el-form-item>
        <el-form-item
          :label="actionDialog.mode === 'reject' ? '驳回原因' : '审批意见'"
          :error="actionDialog.fieldErrors.reason"
        >
          <el-input
            v-model="actionDialog.form.reason"
            :autosize="{ minRows: 3, maxRows: 6 }"
            :placeholder="actionDialog.mode === 'reject' ? '请输入驳回原因' : '请输入审批意见（选填）'"
            type="textarea"
          />
        </el-form-item>
        <template v-if="shouldCollectFourthNodeFiles">
          <el-form-item label="存入路径确认" :error="actionDialog.fieldErrors.confirmedDirectoryId">
            <el-tree-select
              v-model="fourthNodeUpload.confirmedDirectoryId"
              class="!w-full"
              data-testid="dcc-doc-control-confirmed-directory"
              :data="docControlDirectoryTreeOptions"
              :props="docControlDirectoryTreeProps"
              node-key="id"
              filterable
              default-expand-all
              :loading="docControlDirectoryTreeLoading"
              placeholder="请选择存入路径"
              @change="clearActionDialogFieldError('confirmedDirectoryId')"
            />
          </el-form-item>
          <el-form-item label="盖章 PDF" :error="actionDialog.fieldErrors.stampedPdfUploadTicket">
            <div class="w-full">
              <el-upload
                ref="stampedPdfUploadRef"
                action="#"
                accept=".pdf,application/pdf"
                :auto-upload="false"
                :limit="1"
                :file-list="stampedPdfFileList"
                :on-change="handleStampedPdfChange"
                :before-remove="handleBeforeStampedPdfRemove"
                :on-remove="handleStampedPdfRemove"
                :on-exceed="handleStampedPdfExceed"
              >
                <el-button plain :loading="fourthNodeUpload.stampedLoading">
                  <Icon icon="ep:document" class="mr-5px" />
                  选择 PDF
                </el-button>
              </el-upload>
              <div v-if="fourthNodeUpload.stampedPdf" class="mt-8px text-13px text-[var(--el-text-color-secondary)]">
                {{ fourthNodeUpload.stampedPdf.fileName }}
              </div>
            </div>
          </el-form-item>
          <el-form-item
            label="文件下发范围"
            :error="actionDialog.fieldErrors.selectedDistributionScopes"
          >
            <el-tree-select
              v-model="selectedDistributionDepartmentIds"
              class="!w-full"
              data-testid="dcc-doc-control-distribution-departments"
              :data="departmentTreeOptions"
              :props="departmentTreeProps"
              node-key="id"
              multiple
              show-checkbox
              collapse-tags
              collapse-tags-tooltip
              filterable
              default-expand-all
              placeholder="请选择下发部门"
            />
            <el-table
              v-if="fourthNodeUpload.selectedDistributionScopes.length"
              class="mt-8px"
              data-testid="dcc-doc-control-distribution-scopes"
              :data="fourthNodeUpload.selectedDistributionScopes"
              border
              size="small"
            >
              <el-table-column label="部门" min-width="180">
                <template #default="{ row }">
                  {{ deptNameMap.get(row.departmentId) || `部门#${row.departmentId}` }}
                </template>
              </el-table-column>
              <el-table-column label="下发介质" width="220">
                <template #default="{ row }">
                  <el-radio-group
                    v-model="row.distributionMedium"
                    size="small"
                    @change="clearActionDialogFieldError('selectedDistributionScopes')"
                  >
                    <el-radio-button
                      v-for="option in docControlDistributionMediumOptions"
                      :key="option.value"
                      :label="option.value"
                    >
                      {{ option.label }}
                    </el-radio-button>
                  </el-radio-group>
                </template>
              </el-table-column>
            </el-table>
          </el-form-item>
        </template>
        <template v-if="shouldCollectExternalReviewConclusion">
          <el-form-item label="评审结论" :error="actionDialog.fieldErrors.reviewConclusion">
            <el-select v-model="externalReviewAction.reviewConclusion" class="!w-full" placeholder="请选择评审结论">
              <el-option label="接收" value="ACCEPTED" />
              <el-option label="带意见接收" value="ACCEPTED_WITH_NOTES" />
              <el-option label="不接收" value="REJECTED" />
            </el-select>
          </el-form-item>
          <el-form-item label="输出文件" :error="actionDialog.fieldErrors.outputUploadTicket">
            <div class="w-full">
              <el-upload
                ref="externalOutputUploadRef"
                action="#"
                accept=".doc,.docx,.xls,.xlsx,.dwg,.sldprt,.sldasm,.slddrw"
                :auto-upload="false"
                :limit="1"
                :file-list="externalOutputFileList"
                :on-change="handleExternalOutputFileChange"
                :before-remove="handleBeforeExternalOutputFileRemove"
                :on-remove="handleExternalOutputFileRemove"
              >
                <el-button plain :loading="externalReviewAction.outputLoading">
                  <Icon icon="ep:upload" class="mr-5px" />
                  选择输出文件
                </el-button>
              </el-upload>
              <div v-if="externalReviewAction.outputFile" class="mt-8px text-13px text-[var(--el-text-color-secondary)]">
                {{ externalReviewAction.outputFile.fileName }}
              </div>
            </div>
          </el-form-item>
          <el-form-item label="结论说明">
            <el-input
              v-model="externalReviewAction.conclusionComment"
              :autosize="{ minRows: 3, maxRows: 6 }"
              placeholder="请输入评审结论说明"
              type="textarea"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="closeActionDialog">取消</el-button>
        <el-button type="primary" :loading="actionDialog.submitting" @click="submitActionDialog">
          确认签名
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="taskActionDialog.visible"
      :title="taskActionDialogTitle"
      width="560px"
      destroy-on-close
    >
      <el-alert
        v-if="taskActionDialog.inlineError"
        :closable="false"
        class="mb-16px"
        show-icon
        type="error"
        :title="taskActionDialog.inlineError"
      />
      <el-form label-width="96px">
        <el-form-item
          v-if="taskActionDialog.mode === 'return'"
          label="回退节点"
          :error="taskActionDialog.fieldErrors.targetTaskDefinitionKey"
        >
          <el-select
            v-model="taskActionDialog.form.targetTaskDefinitionKey"
            class="!w-full"
            placeholder="请选择回退节点"
          >
            <el-option
              v-for="item in returnTargetOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="taskActionDialog.mode === 'transfer'"
          label="转办人"
          :error="taskActionDialog.fieldErrors.assigneeUserId"
        >
          <UserSelectV2
            v-model="taskActionDialog.form.assigneeUserId"
            class="!w-full"
            placeholder="请选择转办人"
          />
        </el-form-item>
        <template v-if="taskActionDialog.mode === 'sign'">
          <el-form-item label="加签方式">
            <el-radio-group v-model="taskActionDialog.form.signType">
              <el-radio-button label="before">前加签</el-radio-button>
              <el-radio-button label="after">后加签</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="加签人" :error="taskActionDialog.fieldErrors.userIds">
            <UserSelectV2
              v-model="taskActionDialog.form.userIds"
              class="!w-full"
              :multiple="true"
              placeholder="请选择加签人"
            />
          </el-form-item>
        </template>
        <el-form-item label="登录密码" :error="taskActionDialog.fieldErrors.password">
          <el-input
            v-model="taskActionDialog.form.password"
            clearable
            placeholder="请输入当前登录密码"
            show-password
            type="password"
          />
        </el-form-item>
        <el-form-item label="处理意见" :error="taskActionDialog.fieldErrors.reason">
          <el-input
            v-model="taskActionDialog.form.reason"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请输入处理意见"
            type="textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeTaskActionDialog">取消</el-button>
        <el-button type="primary" :loading="taskActionDialog.submitting" @click="submitTaskActionDialog">
          确认签名
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="paperDistributionIssueDialog.visible"
      title="纸质发放登记"
      width="560px"
      destroy-on-close
    >
      <el-alert
        v-if="paperDistributionIssueDialog.inlineError"
        :closable="false"
        class="mb-16px"
        show-icon
        type="error"
        :title="paperDistributionIssueDialog.inlineError"
      />
      <el-form label-width="112px">
        <el-form-item label="纸质接收人" :error="paperDistributionIssueDialog.fieldErrors.recipientUserIds">
          <UserSelectV2
            v-model="paperDistributionIssueDialog.form.recipientUserIds"
            class="!w-full"
            :multiple="true"
            placeholder="请选择纸质接收人"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closePaperDistributionIssueDialog">取消</el-button>
        <el-button
          type="primary"
          :loading="paperDistributionIssueDialog.submitting"
          @click="submitPaperDistributionIssueDialog"
        >
          确认发放
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="electronicReceiptDialog.visible"
      title="电子发放签收"
      width="520px"
      destroy-on-close
    >
      <el-alert
        v-if="electronicReceiptDialog.inlineError"
        :closable="false"
        class="mb-16px"
        show-icon
        type="error"
        :title="electronicReceiptDialog.inlineError"
      />
      <el-form label-width="96px">
        <el-form-item label="登录密码" :error="electronicReceiptDialog.fieldErrors.password">
          <el-input
            v-model="electronicReceiptDialog.form.password"
            clearable
            placeholder="请输入当前登录密码"
            show-password
            type="password"
          />
        </el-form-item>
        <el-form-item label="签收意见">
          <el-input
            v-model="electronicReceiptDialog.form.comment"
            :autosize="{ minRows: 3, maxRows: 6 }"
            maxlength="1000"
            placeholder="请输入签收意见（选填）"
            show-word-limit
            type="textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeElectronicReceiptDialog">取消</el-button>
        <el-button
          type="primary"
          :loading="electronicReceiptDialog.submitting"
          @click="submitElectronicReceiptDialog"
        >
          确认签收
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="distributionSignDialog.visible"
      title="接收人加签"
      width="560px"
      destroy-on-close
    >
      <el-alert
        v-if="distributionSignDialog.inlineError"
        :closable="false"
        class="mb-16px"
        show-icon
        type="error"
        :title="distributionSignDialog.inlineError"
      />
      <el-form label-width="96px">
        <el-form-item label="加签接收人" :error="distributionSignDialog.fieldErrors.userIds">
          <UserSelectV2
            v-model="distributionSignDialog.form.userIds"
            class="!w-full"
            :multiple="true"
            placeholder="请选择加签接收人"
          />
        </el-form-item>
        <el-form-item label="登录密码" :error="distributionSignDialog.fieldErrors.password">
          <el-input
            v-model="distributionSignDialog.form.password"
            clearable
            placeholder="请输入当前登录密码"
            show-password
            type="password"
          />
        </el-form-item>
        <el-form-item label="加签意见">
          <el-input
            v-model="distributionSignDialog.form.comment"
            :autosize="{ minRows: 3, maxRows: 6 }"
            maxlength="1000"
            placeholder="请输入加签意见（选填）"
            show-word-limit
            type="textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDistributionSignDialog">取消</el-button>
        <el-button
          type="primary"
          :loading="distributionSignDialog.submitting"
          @click="submitDistributionSignDialog"
        >
          确认加签
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="obsoleteDialog.visible" title="作废当前版本" width="720px" destroy-on-close>
      <el-alert
        v-if="obsoleteDialog.inlineError"
        :closable="false"
        class="mb-16px"
        show-icon
        type="error"
        :title="obsoleteDialog.inlineError"
      />
      <el-form label-width="96px">
        <el-form-item label="作废原因">
          <el-input
            v-model="obsoleteDialog.reason"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请输入作废原因"
            type="textarea"
          />
        </el-form-item>
        <el-form-item
          v-for="task in obsoleteDialog.startUserSelectTasks"
          :key="task.id"
          :label="`${task.name}审批人`"
        >
          <UserSelectV2
            v-model="obsoleteDialog.startUserSelectAssignees[task.id]"
            multiple
            :placeholder="`请选择${task.name}审批人`"
          />
        </el-form-item>
      </el-form>
      <section data-testid="dcc-obsolete-form-center-panel">
        <ActionFormPanel
          v-if="dccObsoleteFormCenterContext"
          :context="dccObsoleteFormCenterContext"
          :disabled="
            obsoleteDialog.submitting || !canSubmitObsoleteAction || !obsoleteDialog.reason.trim()
          "
          :form-data="dccObsoleteFormCenterFormData"
          :idempotency-key="obsoleteDialog.idempotencyKey"
        />
        <el-alert
          v-else
          :closable="false"
          show-icon
          title="当前文件缺少平台动作上下文，无法发起作废申请。"
          type="error"
        />
      </section>
      <template #footer>
        <el-button @click="closeObsoleteDialog">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishDialog.visible" title="提交发布申请" width="520px" destroy-on-close>
      <el-alert
        v-if="publishDialog.inlineError"
        :closable="false"
        class="mb-16px"
        show-icon
        type="error"
        :title="publishDialog.inlineError"
      />
      <el-form label-width="96px">
        <el-form-item label="发布说明">
          <el-input
            v-model="publishDialog.reason"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请输入发布说明"
            type="textarea"
          />
        </el-form-item>
        <el-form-item
          v-for="task in publishDialog.startUserSelectTasks"
          :key="task.id"
          :label="`${task.name}审批人`"
        >
          <UserSelectV2
            v-model="publishDialog.startUserSelectAssignees[task.id]"
            multiple
            :placeholder="`请选择${task.name}审批人`"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closePublishDialog">取消</el-button>
        <el-button type="primary" :loading="publishDialog.submitting" @click="submitPublishDialog">
          提交发布申请
        </el-button>
      </template>
    </el-dialog>

  </template>

  <el-dialog
    v-model="previewInfoDialogs.approval"
    title="审批矩阵批准情况"
    data-testid="dcc-controlled-preview-approval-dialog"
    width="900px"
    destroy-on-close
  >
    <el-table
      :data="previewApprovalRows"
      empty-text="当前文件暂无审批矩阵批准情况"
      max-height="520"
    >
      <el-table-column label="阶段" min-width="150" prop="stageDisplayName" />
      <el-table-column label="批准进度" align="center" width="110" prop="approvalProgressText" />
      <el-table-column label="批准状态" align="center" width="120">
        <template #default="{ row }">
          <el-tag :type="row.approvalStatusTagType">
            {{ row.approvalStatusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批方式" width="120" prop="approveMethodLabel" />
      <el-table-column label="通过比例" align="center" width="100" prop="approveRatioText" />
      <el-table-column label="解析审批人" min-width="260" prop="resolvedUserNames" show-overflow-tooltip />
      <el-table-column label="同层情况" min-width="140" prop="approvalHint" show-overflow-tooltip />
    </el-table>
  </el-dialog>

  <el-dialog
    v-model="previewInfoDialogs.distribution"
    title="分发信息"
    data-testid="dcc-controlled-preview-distribution-dialog"
    width="1040px"
    destroy-on-close
  >
    <el-table
      :data="distributionStatusRows"
      empty-text="当前版本暂无分发记录"
      max-height="520"
    >
      <el-table-column label="部门" min-width="180">
        <template #default="{ row }">
          {{ deptNameMap.get(row.departmentId) || `部门#${row.departmentId}` }}
        </template>
      </el-table-column>
      <el-table-column label="发放方式" width="130">
        <template #default="{ row }">
          {{ getDistributionMediumLabel(row.distributionMedium) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="120">
        <template #default="{ row }">
          <el-tag :type="getDistributionStatusTagType(row.status)">
            {{ getDistributionStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="接收人" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          {{ getDistributionRecipientDisplay(row) }}
        </template>
      </el-table-column>
      <el-table-column label="发放人" min-width="160">
        <template #default="{ row }">
          {{ getDistributionAckUserSummary(row, userNameMap) }}
        </template>
      </el-table-column>
      <el-table-column label="发放日期" align="center" width="170">
        <template #default="{ row }">
          {{ formatControlledFileDateTime(row.acknowledgedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="回收人" min-width="160">
        <template #default="{ row }">
          {{ getDistributionRecoverUserSummary(row) }}
        </template>
      </el-table-column>
      <el-table-column label="回收日期" align="center" width="170">
        <template #default="{ row }">
          {{ formatControlledFileDateTime(row.recoveredAt) }}
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog
    v-model="previewInfoDialogs.version"
    title="版本历史"
    data-testid="dcc-controlled-preview-version-dialog"
    width="980px"
    destroy-on-close
  >
    <el-table
      :data="versionHistoryRows"
      empty-text="当前文件暂无版本历史"
      max-height="520"
    >
      <el-table-column label="标题" min-width="220" prop="title" show-overflow-tooltip />
      <el-table-column label="文件编号" min-width="150" prop="fileNumber" show-overflow-tooltip />
      <el-table-column label="版本" align="center" width="100" prop="versionNo" />
      <el-table-column label="升版原因/变更说明" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span data-testid="dcc-detail-version-history-change-reason">
            {{ getVersionChangeReasonText(row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="120">
        <template #default="{ row }">
          <el-tag :type="getDetailStatusTagType(row.status)">
            {{ getDetailStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生效日期" align="center" width="130">
        <template #default="{ row }">
          {{ formatControlledFileDate(row.effectiveDate) }}
        </template>
      </el-table-column>
      <el-table-column label="发布时间" align="center" width="170">
        <template #default="{ row }">
          {{ formatControlledFileDateTime(row.publishedTime) }}
        </template>
      </el-table-column>
      <el-table-column label="作废时间" align="center" width="170">
        <template #default="{ row }">
          {{ formatControlledFileDateTime(row.obsoletedTime) }}
        </template>
      </el-table-column>
      <el-table-column label="后继版本" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span data-testid="dcc-detail-version-successor-summary">
            {{ getSuccessorVersionSummary(row) }}
          </span>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog
    v-model="controlledPrintDialog.visible"
    title="受控打印"
    data-testid="dcc-controlled-print-dialog"
    width="560px"
    destroy-on-close
  >
    <el-alert
      class="mb-12px"
      data-testid="dcc-controlled-print-policy-dialog"
      type="info"
      title="当前文件类别无需打印审批"
      description="提交后将直接生成受控打印件，审批/打印状态记录为 DIRECT_PRINTED。"
      show-icon
      :closable="false"
    />
    <el-alert
      v-if="controlledPrintDialog.inlineError"
      type="error"
      :title="controlledPrintDialog.inlineError"
      show-icon
      :closable="false"
      class="mb-12px"
    />
    <el-form label-width="96px" :model="controlledPrintDialog.form">
      <el-form-item label="打印用途" :error="controlledPrintDialog.fieldErrors.purpose">
        <el-input
          v-model="controlledPrintDialog.form.purpose"
          type="textarea"
          :rows="3"
          maxlength="255"
          show-word-limit
          placeholder="请输入本次受控打印用途"
        />
      </el-form-item>
      <el-form-item label="份数" :error="controlledPrintDialog.fieldErrors.copies">
        <el-input-number
          v-model="controlledPrintDialog.form.copies"
          :min="1"
          :max="999"
          :precision="0"
          controls-position="right"
          class="w-100%"
        />
      </el-form-item>
      <el-form-item label="接收部门" :error="controlledPrintDialog.fieldErrors.receivingDepartment">
        <div class="w-full" data-testid="dcc-controlled-print-receiving-department-select">
          <el-select
            v-model="controlledPrintDialog.form.receivingDepartment"
            class="w-100%"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="请选择或输入接收部门"
          >
            <el-option
              v-for="department in controlledPrintReceivingDepartmentOptions"
              :key="department"
              :label="department"
              :value="department"
            />
          </el-select>
        </div>
      </el-form-item>
      <el-form-item label="使用位置" :error="controlledPrintDialog.fieldErrors.useLocation">
        <div class="w-full" data-testid="dcc-controlled-print-use-location-select">
          <el-select
            v-model="controlledPrintDialog.form.useLocation"
            class="w-100%"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="请选择或输入使用位置"
          >
            <el-option
              v-for="location in controlledPrintUseLocationOptions"
              :key="location"
              :label="location"
              :value="location"
            />
          </el-select>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeControlledPrintDialog">取消</el-button>
      <el-button
        type="primary"
        :loading="controlledPrintDialog.submitting"
        @click="submitControlledPrint"
      >
        生成受控打印件
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="controlledPrintResultDialog.visible"
    title="受控打印已生成"
    data-testid="dcc-controlled-print-result-dialog"
    width="640px"
  >
    <el-alert
      class="mb-12px"
      type="success"
      title="本次受控打印已形成可追溯记录"
      description="以下信息来自本次打印记录，可用于现场核对、审计抽查和后续回收盘点。"
      show-icon
      :closable="false"
    />
    <el-descriptions v-if="controlledPrintResultDialog.record" :column="2" border>
      <el-descriptions-item label="打印编号">
        {{ controlledPrintResultDialog.record.printNo }}
      </el-descriptions-item>
      <el-descriptions-item label="份数">
        {{ controlledPrintResultDialog.record.copies }}
      </el-descriptions-item>
      <el-descriptions-item label="打印人">
        {{
          controlledPrintResultDialog.record.printUserName ||
          userNameMap.get(controlledPrintResultDialog.record.printUserId) ||
          `用户#${controlledPrintResultDialog.record.printUserId}`
        }}
      </el-descriptions-item>
      <el-descriptions-item label="打印时间">
        {{ formatControlledFileDateTime(controlledPrintResultDialog.record.printTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="审批策略" :span="2">
        直接受控打印（当前文件类别无需打印审批，状态 DIRECT_PRINTED）
      </el-descriptions-item>
      <el-descriptions-item label="副本编号" :span="2">
        <span data-testid="dcc-controlled-print-result-copy-nos">
          {{ formatControlledPrintCopyNumberList(controlledPrintResultDialog.record) }}
        </span>
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="controlledPrintResultDialog.visible = false">关闭</el-button>
      <el-button type="primary" @click="handleViewLatestControlledPrintRecord">
        查看打印记录
      </el-button>
    </template>
  </el-dialog>

  <ControlledFileMetadataDialog
    v-model="metadataDialogVisible"
    :file="fileDetail"
    :categories="categories"
    :directories="directories"
    @saved="handleMetadataSaved"
  />
</template>

<script lang="ts" setup>
import type { UploadProps, UploadUserFile } from 'element-plus'
import * as DefinitionApi from '@/api/bpm/definition'
import * as ProcessInstanceApi from '@/api/bpm/processInstance'
import * as TaskApi from '@/api/bpm/task'
import { CandidateStrategy, NodeId } from '@/components/SimpleProcessDesignerV2/src/consts'
import { getFileCategoryList, type ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import { getDirectoryTree, type ControlledFileDirectoryVO } from '@/api/dcc/controlledFile/directories'
import {
  acknowledgeElectronicDistribution,
  acknowledgePaperDistribution,
  acknowledgeControlledFileTraining,
  approveExternalFileReviewTask,
  cleanupControlledFileUploadSession,
  createControlledFilePrintRecord,
  createControlledFileUploadSessionId,
  createControlledFileSignTask,
  createDistributionRecipientSignTask,
  createExternalFileReviewSignTask,
  deleteWithdrawnControlledFile,
  getControlledFile,
  getControlledFileAccessExplanation,
  getControlledFilePrintHtml,
  getControlledFilePrintRecords,
  getControlledFileUploadDirectoryTree,
  getPaperDistributionRecords,
  isControlledFileTaskPasswordInvalidError,
  manualReleaseControlledFile,
  publishControlledFile,
  recoverPaperDistribution,
  recognizeControlledFileProjectCode,
  resubmitWithdrawnControlledFile,
  rejectExternalFileReviewTask,
  retryControlledFileStamp,
  returnExternalFileReviewTask,
  returnControlledFileTask,
  transferExternalFileReviewTask,
  transferControlledFileTask,
  uploadControlledFileTrainingRecord,
  uploadControlledFilePreview,
  withdrawControlledFile,
  type ControlledFileDistributionRecipientStatusVO,
  type ControlledFileDistributionStatusVO,
  type ControlledFileDistributionMedium,
  type ControlledFileDistributionScopeVO,
  type ControlledFilePaperDistributionRecordVO,
  type ControlledFilePrintCreateReqVO,
  type ControlledFilePrintRecordVO,
  type ControlledFileAccessExplanationVO,
  type ControlledFileRouteSnapshotVO,
  type ControlledFileUploadDirectoryNodeVO,
  type ControlledFileUploadRespVO,
  type ControlledFileSignatureSummaryVO,
  type ControlledFileVersionHistoryVO,
  type ControlledFileVO
} from '@/api/dcc/controlledFile/workflow'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import ActionFormPanel from '@/views/form-center/business-action/ActionFormPanel.vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import type { TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { checkPermi } from '@/utils/permission'
import { downloadByData } from '@/utils/filt'
import { generateUUID } from '@/utils'
import { findActiveBusinessAction, type FormInstanceVO } from '@/api/form-center/instance'
import { resolveBusinessAction, type BusinessActionContextVO } from '@/api/form-center/businessAction'
import { resolveControlledActionProjection } from '@/api/form-center/actionProjection'
import {
  getDccElectronicSignaturePage,
  type DccElectronicSignatureVO
} from '@/api/dcc/controlledFile/signatures'
import {
  exportControlledFileApprovalWord,
  getActiveApprovalPrintTemplate,
  getControlledFileApprovalPrintHtml,
  type ApprovalPrintTemplateVO
} from '@/api/dcc/controlledFile/approvalPrintTemplate'
import {
  DCC_APPROVAL_WRONG_PASSWORD_MESSAGE,
  getDccApprovalActionLabels,
  getDccApprovalSignatureMeaningPreview,
  resolveDccApprovalSignatureErrorMessage,
  submitDccApprovalAction,
  type DccApprovalActionMode
} from './approval-actions'
import {
  DCC_BPM_TASK_STATUS,
  buildDccTaskStageProgress,
  type DccTaskLike,
  type DccTaskStageProgress
} from '../shared/approval'
import {
  isDccControlledFileWithdrawableStatus,
  type DccControlledFileStatus
} from '../shared/lifecycle'
import { getControlledFileHandlingSummary } from '../shared/handlingSummary'
import { resolveDccStageDisplayName } from '../shared/stage-name'
import { ROUTE_APPROVE_METHOD_OPTIONS, ROUTE_CANDIDATE_SOURCE_OPTIONS, getOptionLabel } from '../shared/options'
import { flattenTree } from '../shared/utils'
import ControlledFileBasicInfoPanel from '../shared/ControlledFileBasicInfoPanel.vue'
import ControlledFileMetadataDialog from '../shared/ControlledFileMetadataDialog.vue'
import ProtectedPdfViewer from '../view/index.vue'
import {
  buildControlledFileViewerPath,
  isControlledFileViewerMode,
  resolveControlledFileTraceabilityScope,
  resolveControlledFileViewerReturnTo
} from '../view/presentation'
import {
  flattenTrainingAssignments,
  formatControlledFileDate,
  formatControlledFileDateTime,
  buildDetailLifecycleTimelineItems,
  buildDetailUserDisplayName,
  getDetailActionState,
  getDistributionMediumLabel,
  getDistributionAckUserSummary,
  getDetailStatusLabel,
  getDetailStatusTagType,
  getDistributionRecipientSummary,
  getDistributionStatusLabel,
  getDetailTrainingCompletionSummary,
  getDetailTrainingAssignmentSummary,
  getPendingTrainingAssignments,
  getControlledCopyHashStatusLabel,
  getControlledCopyHashStatusTagType,
  getSignatureActionLabel,
  getSignatureActorSummary,
  getSignatureEvidenceStatusLabel,
  getSignatureEvidenceStatusTagType,
  getSignatureMeaningLabel,
  formatSignatureSnapshotValue,
  getTrainingAssignmentUserSummary,
  formatSignatureHashShort,
  isVersionHistoryVisibleToReader,
  resolveReadSideErrorMessage
} from './presentation'

defineOptions({ name: 'DccControlledFileDetail' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

const DOC_CONTROL_ROLE_CODE = 'doc_control'
const APPLICANT_REWORK_TASK_DEFINITION_KEY = 'APPLICANT_REWORK'
const DCC_OBSOLETE_ACTION_CODE = 'OBSOLETE'
const DCC_PUBLISH_ACTION_CODE = 'PUBLISH'
const hasMetadataEditorRole = (roles: string[]) => roles.includes(DOC_CONTROL_ROLE_CODE)

const fileDetail = ref<ControlledFileVO>()
const fileAccessExplanation = ref<ControlledFileAccessExplanationVO | null>(null)
const accessExplanationError = ref('')
const activeObsoleteAction = ref<FormInstanceVO | null>(null)
const activeObsoleteActionError = ref('')
const activePublishAction = ref<FormInstanceVO | null>(null)
const activePublishActionError = ref('')
const categories = ref<ControlledFileCategoryVO[]>([])
const directories = ref<ControlledFileDirectoryVO[]>([])
const activeApprovalPrintTemplate = ref<ApprovalPrintTemplateVO | null>(null)
const withdrawLoading = ref(false)
const obsoleteCancelLoading = ref(false)
const publishCancelLoading = ref(false)
const deleteWithdrawnLoading = ref(false)
const resubmitWithdrawnLoading = ref(false)
const retryStampLoading = ref(false)
const processPrintLoading = ref(false)
const processExportLoading = ref(false)
const controlledPrintRecordsLoading = ref(false)
const controlledPrintRecordsError = ref('')
const controlledPrintRecords = ref<ControlledFilePrintRecordVO[]>([])
const controlledPrintRecordsTableRef = ref()
const controlledPrintAutoOpenKey = ref('')
const latestControlledPrintRecordId = ref<number>()
const controlledPrintHighlightRecordId = ref<number>()
let controlledPrintHighlightTimer: ReturnType<typeof setTimeout> | undefined
const projectCodeRecognitionLoading = ref(false)
const trainingAckLoading = ref(false)
const manualReleaseLoading = ref(false)
const paperDistributionAckLoadingId = ref<number>()
const paperDistributionRecoverLoadingId = ref<number>()
const electronicReceiptLoadingRecipientId = ref<number>()
const distributionSignLoadingRecipientId = ref<number>()
const metadataDialogVisible = ref(false)
const previewInfoDialogs = reactive({
  approval: false,
  distribution: false,
  version: false
})
const approvalLoading = ref(false)
const categoryNameMap = ref(new Map<number, string>())
const directoryNameMap = ref(new Map<number, string>())
const directoryPathMap = ref(new Map<number, string>())
const userNameMap = ref(new Map<number, string>())
const deptNameMap = ref(new Map<number, string>())
const departmentList = ref<DeptVO[]>([])
const approvalTodoTask = ref<any>()
const approvalTaskList = ref<DccTaskLike[]>([])
const stageProgressList = ref<DccTaskStageProgress[]>([])
const paperDistributionRecords = ref<ControlledFilePaperDistributionRecordVO[]>([])
const dccSignatureEvidenceLoading = ref(false)
const dccSignatureEvidenceList = ref<DccElectronicSignatureVO[]>([])
const dccSignatureEvidenceTotal = ref(0)
const dccSignatureEvidenceError = ref('')
const dccSignatureEvidenceQueryParams = reactive({
  pageNo: 1,
  pageSize: 10
})
const stampedPdfUploadRef = ref()
const applicantTrainingRecordUploadRef = ref()
const externalOutputUploadRef = ref()
const stampedPdfFileList = ref<UploadUserFile[]>([])
const applicantTrainingRecordFileList = ref<UploadUserFile[]>([])
const externalOutputFileList = ref<UploadUserFile[]>([])
const fourthNodeUploadSessionId = ref(createControlledFileUploadSessionId())
const applicantTrainingRecordUploadSessionId = ref(createControlledFileUploadSessionId())
const externalOutputUploadSessionId = ref(createControlledFileUploadSessionId())
const docControlDirectoryTreeOptions = ref<ControlledFileUploadDirectoryNodeVO[]>([])
const docControlDirectoryTreeLoading = ref(false)

type DccTaskActionMode = 'return' | 'transfer' | 'sign'

const dccSignatureEvidenceDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'versionNo', label: '版本', width: 110 },
  { key: 'signer', label: '签名人', minWidth: 220 },
  { key: 'departmentPost', label: '部门/岗位', minWidth: 190 },
  { key: 'role', label: '角色', minWidth: 150 },
  { key: 'action', label: '动作', width: 120 },
  { key: 'meaning', label: '签名含义', width: 140 },
  { key: 'purpose', label: '签名目的', minWidth: 150 },
  { key: 'authorizationBasis', label: '权限依据', minWidth: 240 },
  { key: 'signatureMode', label: '签名方式', width: 120 },
  { key: 'sourceFileHash', label: '源文件 hash', width: 150 },
  { key: 'controlledCopy', label: '受控副本', width: 130 },
  { key: 'controlledCopyHash', label: '副本 hash', width: 150 },
  { key: 'evidenceStatus', label: '证据状态', width: 120 },
  { key: 'evidenceHash', label: '证据 hash', width: 150 },
  { key: 'comment', label: '签名意见', minWidth: 240 },
  { key: 'signedAt', label: '签名时间', width: 180 }
]
const {
  isColumnVisible: isDccSignatureEvidenceColumnVisible,
  getColumnWidthString: getDccSignatureEvidenceColumnWidthString,
  getColumnMinWidthString: getDccSignatureEvidenceColumnMinWidthString,
  handleHeaderDragend: handleDccSignatureEvidenceHeaderDragend
} = useUserTableColumns('dcc.controlledFile.detail.signatureEvidence', dccSignatureEvidenceDefaultColumns)

const detailListQueryModel = reactive({})
const detailListFilterDefinitions: TableQuickFilterDefinition[] = []
const detailListQuickFilterState = reactive({})
const detailListOperatorOptions = []
const createDetailListState = () => reactive({ pageNo: 1, pageSize: 10 })
const routeSnapshotListState = createDetailListState()
const versionHistoryListState = createDetailListState()
const distributionStatusListState = createDetailListState()
const controlledPrintRecordListState = createDetailListState()
const trainingStatusListState = createDetailListState()
const signatureTraceListState = createDetailListState()
function getPagedDetailRows<T>(rows: T[], pageNo: number, pageSize: number) {
  if (!rows.length) {
    return []
  }
  const safePageSize = Math.max(1, Number(pageSize) || 10)
  const safePageNo = Math.min(
    Math.max(1, Number(pageNo) || 1),
    Math.max(1, Math.ceil(rows.length / safePageSize))
  )
  const startIndex = (safePageNo - 1) * safePageSize
  return rows.slice(startIndex, startIndex + safePageSize)
}

const routeSnapshotDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'stageDisplayName', label: '阶段', minWidth: 240 },
  { key: 'candidateText', label: '候选摘要', minWidth: 260 },
  { key: 'approvalRequirementText', label: '审批要求', minWidth: 260 },
  { key: 'resolvedUserNames', label: '解析审批人', minWidth: 260 }
]
const {
  columns: routeSnapshotColumns,
  saving: routeSnapshotColumnSaving,
  isColumnVisible: isRouteSnapshotColumnVisible,
  getColumnWidthString: getRouteSnapshotColumnWidthString,
  getColumnMinWidthString: getRouteSnapshotColumnMinWidthString,
  handleHeaderDragend: handleRouteSnapshotHeaderDragend,
  saveConfig: saveRouteSnapshotColumnConfig,
  resetConfig: resetRouteSnapshotColumnConfig
} = useUserTableColumns('dcc.controlledFile.detail.routeSnapshot', routeSnapshotDefaultColumns)

const versionHistoryDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'title', label: '标题', minWidth: 220 },
  { key: 'fileNumber', label: '文件编号', minWidth: 150 },
  { key: 'versionNo', label: '版本', width: 100 },
  { key: 'changeReasonText', label: '升版原因/变更说明', minWidth: 220 },
  { key: 'distributionMedium', label: '发放方式', minWidth: 140 },
  { key: 'status', label: '状态', width: 120 },
  { key: 'publishedTime', label: '发布时间', width: 180 },
  { key: 'obsoletedTime', label: '作废时间', width: 180 },
  { key: 'successorVersionSummary', label: '后继版本', minWidth: 220 },
  { key: 'operation', label: '操作', width: 120, hideable: false, business: false, sortable: false }
]
const {
  columns: versionHistoryColumns,
  saving: versionHistoryColumnSaving,
  isColumnVisible: isVersionHistoryColumnVisible,
  getColumnWidthString: getVersionHistoryColumnWidthString,
  getColumnMinWidthString: getVersionHistoryColumnMinWidthString,
  handleHeaderDragend: handleVersionHistoryHeaderDragend,
  saveConfig: saveVersionHistoryColumnConfig,
  resetConfig: resetVersionHistoryColumnConfig
} = useUserTableColumns('dcc.controlledFile.detail.versionHistory', versionHistoryDefaultColumns)

const distributionStatusDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'departmentName', label: '部门', minWidth: 180 },
  { key: 'recipientText', label: '接收人', minWidth: 280 },
  { key: 'distributionSummaryText', label: '分发摘要', minWidth: 300 },
  { key: 'recoverySummaryText', label: '回收摘要', minWidth: 260 },
  { key: 'operation', label: '操作', width: 300, hideable: false, business: false, sortable: false }
]
const {
  columns: distributionStatusColumns,
  saving: distributionStatusColumnSaving,
  isColumnVisible: isDistributionStatusColumnVisible,
  getColumnWidthString: getDistributionStatusColumnWidthString,
  getColumnMinWidthString: getDistributionStatusColumnMinWidthString,
  handleHeaderDragend: handleDistributionStatusHeaderDragend,
  saveConfig: saveDistributionStatusColumnConfig,
  resetConfig: resetDistributionStatusColumnConfig
} = useUserTableColumns('dcc.controlledFile.detail.distributionStatus', distributionStatusDefaultColumns)

const controlledPrintRecordDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'printNo', label: '打印编号', minWidth: 210 },
  { key: 'fileNumber', label: '文件编号', minWidth: 160 },
  { key: 'versionNo', label: '版本', width: 100 },
  { key: 'copies', label: '份数', width: 90 },
  { key: 'copyNumbers', label: '副本编号', minWidth: 220 },
  { key: 'purpose', label: '打印用途', minWidth: 180 },
  { key: 'receivingDepartment', label: '接收部门', minWidth: 140 },
  { key: 'useLocation', label: '使用位置', minWidth: 140 },
  { key: 'printUserName', label: '打印人', minWidth: 180 },
  { key: 'printTime', label: '打印时间', width: 180 },
  { key: 'approvalStatus', label: '审批/打印状态', width: 150 }
]
const {
  columns: controlledPrintRecordColumns,
  saving: controlledPrintRecordColumnSaving,
  isColumnVisible: isControlledPrintRecordColumnVisible,
  getColumnWidthString: getControlledPrintRecordColumnWidthString,
  getColumnMinWidthString: getControlledPrintRecordColumnMinWidthString,
  handleHeaderDragend: handleControlledPrintRecordHeaderDragend,
  saveConfig: saveControlledPrintRecordColumnConfig,
  resetConfig: resetControlledPrintRecordColumnConfig
} = useUserTableColumns('dcc.controlledFile.detail.controlledPrintRecords', controlledPrintRecordDefaultColumns)

const trainingStatusDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'departmentName', label: '部门', minWidth: 180 },
  { key: 'traineeName', label: '受训人', minWidth: 220 },
  { key: 'trainingSummaryText', label: '培训摘要', minWidth: 360 }
]
const {
  columns: trainingStatusColumns,
  saving: trainingStatusColumnSaving,
  isColumnVisible: isTrainingStatusColumnVisible,
  getColumnWidthString: getTrainingStatusColumnWidthString,
  getColumnMinWidthString: getTrainingStatusColumnMinWidthString,
  handleHeaderDragend: handleTrainingStatusHeaderDragend,
  saveConfig: saveTrainingStatusColumnConfig,
  resetConfig: resetTrainingStatusColumnConfig
} = useUserTableColumns('dcc.controlledFile.detail.trainingStatus', trainingStatusDefaultColumns)

const signatureTraceDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'traceRole', label: '角色', minWidth: 120 },
  { key: 'actorName', label: '上传人 / 四级审批人', minWidth: 220 },
  { key: 'approvalCommentText', label: '审批意见', minWidth: 220 },
  { key: 'signedAtText', label: '签名时间', width: 180 },
  { key: 'signatureModeText', label: '签名方式', width: 140 },
  { key: 'evidenceStatusText', label: '证据状态', width: 140 },
  { key: 'fileHashText', label: '文件哈希', minWidth: 160 },
  { key: 'fileEvidenceText', label: '盖章文件 / 发布文件证据', minWidth: 260 }
]
const {
  columns: signatureTraceColumns,
  isColumnVisible: isSignatureTraceColumnVisible,
  getColumnWidthString: getSignatureTraceColumnWidthString,
  getColumnMinWidthString: getSignatureTraceColumnMinWidthString,
  handleHeaderDragend: handleSignatureTraceHeaderDragend
} = useUserTableColumns('dcc.controlledFile.detail.signatureTrace', signatureTraceDefaultColumns)

const fourthNodeUpload = reactive({
  stampedPdf: undefined as ControlledFileUploadRespVO | undefined,
  confirmedDirectoryId: undefined as number | undefined,
  selectedDistributionScopes: [] as ControlledFileDistributionScopeVO[],
  stampedLoading: false
})

const docControlDistributionMediumOptions: Array<{
  label: string
  value: ControlledFileDistributionMedium
}> = [
  { label: '电子', value: 'PUBLIC_FOLDER' },
  { label: '纸质', value: 'PAPER' }
]

interface DepartmentTreeOption extends DeptVO {
  children?: DepartmentTreeOption[]
}

const buildDepartmentTreeOptions = (departments: DeptVO[]): DepartmentTreeOption[] => {
  const nodeMap = new Map<number, DepartmentTreeOption>()
  departments.forEach((department) => {
    nodeMap.set(department.id, { ...department, children: [] })
  })
  const roots: DepartmentTreeOption[] = []
  nodeMap.forEach((node) => {
    const parentId = node.parentId
    const parent = parentId ? nodeMap.get(parentId) : undefined
    if (parent) {
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  nodeMap.forEach((node) => {
    if (!node.children?.length) {
      delete node.children
    }
  })
  return roots
}

const departmentTreeProps = {
  label: 'name',
  children: 'children'
} as const

const docControlDirectoryTreeProps = {
  label: 'name',
  children: 'children',
  disabled: (node: ControlledFileUploadDirectoryNodeVO) => !node.leaf
} as const

const selectedDistributionDepartmentIds = computed<number[]>({
  get: () => fourthNodeUpload.selectedDistributionScopes.map((item) => item.departmentId),
  set: (departmentIds) => {
    const mediumByDepartmentId = new Map(
      fourthNodeUpload.selectedDistributionScopes.map((item) => [
        item.departmentId,
        item.distributionMedium
      ])
    )
    const uniqueDepartmentIds = Array.from(
      new Set(departmentIds.filter((departmentId) => typeof departmentId === 'number'))
    )
    fourthNodeUpload.selectedDistributionScopes = uniqueDepartmentIds.map((departmentId) => ({
      departmentId,
      distributionMedium: mediumByDepartmentId.get(departmentId) || 'PUBLIC_FOLDER'
    }))
    clearActionDialogFieldError('selectedDistributionScopes')
  }
})

const departmentTreeOptions = computed(() => buildDepartmentTreeOptions(departmentList.value))
const controlledPrintReceivingDepartmentOptions = computed(() =>
  Array.from(
    new Set(
      departmentList.value
        .map((department) => String(department.name || '').trim())
        .filter(Boolean)
    )
  )
)
const controlledPrintUseLocationOptions = computed(() => {
  const options = [
    ...controlledPrintRecords.value.map((record) => record.useLocation),
    controlledBrowserDirectoryPath.value === '-' ? '' : controlledBrowserDirectoryPath.value
  ]
  return Array.from(new Set(options.map((item) => String(item || '').trim()).filter(Boolean))).slice(0, 20)
})

const applicantTrainingRecordDialog = reactive({
  visible: false,
  submitting: false,
  uploading: false,
  inlineError: '',
  fieldErrors: {} as Record<string, string>,
  file: undefined as ControlledFileUploadRespVO | undefined
})

const actionDialog = reactive({
  visible: false,
  submitting: false,
  mode: 'approve' as DccApprovalActionMode,
  inlineError: '',
  fieldErrors: {} as Record<string, string>,
  form: {
    password: '',
    reason: ''
  }
})

const taskActionDialog = reactive({
  visible: false,
  submitting: false,
  mode: 'return' as DccTaskActionMode,
  inlineError: '',
  fieldErrors: {} as Record<string, string>,
  form: {
    password: '',
    reason: '',
    targetTaskDefinitionKey: '',
    assigneeUserId: undefined as number | undefined,
    userIds: [] as number[],
    signType: 'after' as 'before' | 'after'
  }
})

const externalReviewAction = reactive({
  reviewConclusion: '',
  conclusionComment: '',
  outputFile: undefined as ControlledFileUploadRespVO | undefined,
  outputLoading: false
})

const paperDistributionIssueDialog = reactive({
  visible: false,
  submitting: false,
  inlineError: '',
  fieldErrors: {} as Record<string, string>,
  distributionId: undefined as number | undefined,
  form: {
    recipientUserIds: [] as number[]
  }
})

const electronicReceiptDialog = reactive({
  visible: false,
  submitting: false,
  inlineError: '',
  fieldErrors: {} as Record<string, string>,
  distributionId: undefined as number | undefined,
  recipientId: undefined as number | undefined,
  form: {
    password: '',
    comment: ''
  }
})

const distributionSignDialog = reactive({
  visible: false,
  submitting: false,
  inlineError: '',
  fieldErrors: {} as Record<string, string>,
  distributionId: undefined as number | undefined,
  recipientId: undefined as number | undefined,
  form: {
    userIds: [] as number[],
    password: '',
    comment: ''
  }
})

const controlledPrintDialog = reactive({
  visible: false,
  submitting: false,
  inlineError: '',
  fieldErrors: {} as Record<string, string>,
  form: {
    purpose: '',
    copies: 1,
    receivingDepartment: '',
    useLocation: ''
  }
})
const controlledPrintResultDialog = reactive({
  visible: false,
  record: null as ControlledFilePrintRecordVO | null
})

const obsoleteDialog = reactive({
  visible: false,
  submitting: false,
  inlineError: '',
  reason: '',
  idempotencyKey: '',
  startUserSelectTasks: [] as ProcessInstanceApi.ApprovalNodeInfo[],
  startUserSelectAssignees: {} as Record<string, number[]>
})

const publishDialog = reactive({
  visible: false,
  submitting: false,
  inlineError: '',
  reason: '',
  idempotencyKey: '',
  startUserSelectTasks: [] as ProcessInstanceApi.ApprovalNodeInfo[],
  startUserSelectAssignees: {} as Record<string, number[]>
})

const toDccControlledFileStatus = (
  status: ControlledFileVO['status'] | undefined
): DccControlledFileStatus | undefined => {
  const allStatuses: DccControlledFileStatus[] = [
    'DRAFT',
    'PENDING_DOC_CONTROL_REVIEW',
    'PENDING_MATRIX_REVIEW',
    'PENDING_MATRIX_APPROVAL',
    'PENDING_DOC_CONTROL_APPROVAL',
    'PENDING_APPLICANT_REWORK',
    'PENDING_APPLICANT_TRAINING_RECORD',
    'READY_TO_PUBLISH',
    'FINALIZING',
    'TRAINING_IN_PROGRESS',
    'PENDING_MANUAL_DISTRIBUTION',
    'ACTIVE',
    'REJECTED',
    'WITHDRAWN',
    'OBSOLETE',
    'SUPERSEDED',
    'FINALIZATION_FAILED'
  ]
  return allStatuses.includes(status as DccControlledFileStatus)
    ? (status as DccControlledFileStatus)
    : undefined
}

const controlledFileId = computed(() => String(route.params.id || ''))
const currentUserId = computed(() => userStore.getUser.id)
const fileStatus = computed(() => toDccControlledFileStatus(fileDetail.value?.status))
const obsoleteActionLocked = computed(() => Boolean(activeObsoleteAction.value))
const obsoleteActionBlocked = computed(
  () => obsoleteActionLocked.value || Boolean(activeObsoleteActionError.value)
)
const publishActionLocked = computed(() => Boolean(activePublishAction.value))
const publishActionBlocked = computed(
  () => publishActionLocked.value || Boolean(activePublishActionError.value)
)
const viewerMode = computed(() => isControlledFileViewerMode(route.query as Record<string, unknown>))
const isBrowserTraceabilityPage = computed(
  () =>
    String(route.query.traceability || '') === '1' &&
    String(route.query.from || '') === 'browser'
)
const traceabilityScope = computed(() =>
  resolveControlledFileTraceabilityScope(route.query as Record<string, unknown>)
)
const showLifecycleTraceSections = computed(
  () => !isBrowserTraceabilityPage.value || traceabilityScope.value === 'trace'
)
const showSignatureTraceSections = computed(
  () => !isBrowserTraceabilityPage.value || traceabilityScope.value === 'signature'
)
const canRetryStampPermission = computed(() => checkPermi(['dcc:controlled-file:stamp:retry']))
const canEditMetadata = computed(() => hasMetadataEditorRole(userStore.getRoles))
const detailActionState = computed(() => getDetailActionState(fileDetail.value))
const hasControlledPrintMenuPermission = computed(() => checkPermi(['dcc:controlled-file:print']))
const controlledPrintAllowed = computed(
  () => detailActionState.value.canPrint && hasControlledPrintMenuPermission.value
)
const controlledPrintPermissionHintVisible = computed(
  () => Boolean(fileDetail.value) && !controlledPrintAllowed.value
)
const controlledPrintPermissionHintTitle = computed(() =>
  hasControlledPrintMenuPermission.value ? '当前文件暂不可受控打印' : '无受控打印权限'
)
const controlledPrintPermissionHintDescription = computed(() => {
  if (!fileDetail.value) {
    return ''
  }
  if (!hasControlledPrintMenuPermission.value) {
    return '当前账号缺少受控打印菜单权限，或该文件类别未授予 PRINT 打印权限；页面已按只读方式隐藏受控打印入口。'
  }
  if (!isCurrentActiveVersion.value) {
    return '受控打印仅允许 master 指向的当前有效 ACTIVE 版本；历史版或候选版不能打印。'
  }
  return '后端动作投影未允许本文件受控打印，请核对文件类别 PRINT 权限和当前有效版本状态。'
})
const buildDccActionProjectionMessage = (
  activeAction: FormInstanceVO | null,
  activeError: string,
  actionLabel: string,
  defaultMessage: string
) => {
  if (activeAction) {
    const processInfo = activeAction.bpmProcessInstanceId
      ? `，流程 ${activeAction.bpmProcessInstanceId}`
      : ''
    return `当前文件已有${actionLabel} ${activeAction.instanceCode || activeAction.id} 正在审批${processInfo}，审批完成、驳回或撤回后才可再次发起。`
  }
  return activeError || defaultMessage
}
const canSubmitObsoleteAction = computed(
  () => dccObsoleteActionProjection.value.allowed
)
const canSubmitPublishAction = computed(
  () => dccPublishActionProjection.value.allowed
)
const dccObsoleteActionProjection = computed(() => {
  const blockerMessage = buildDccActionProjectionMessage(
    activeObsoleteAction.value,
    activeObsoleteActionError.value,
    '作废申请',
    '后端动作投影未允许作废申请。'
  )
  return resolveControlledActionProjection({
    actionCode: 'DCC_OBSOLETE',
    actionLabel: '作废申请',
    allowed: detailActionState.value.canObsolete,
    locked: Boolean(activeObsoleteActionError.value),
    pending: Boolean(activeObsoleteAction.value),
    pendingInstanceId: activeObsoleteAction.value?.id,
    pendingStatus: activeObsoleteAction.value?.status,
    effectStatus: activeObsoleteAction.value?.status === 'EFFECT_FAILED_PENDING' ? 'EFFECT_FAILED_PENDING' : undefined,
    lockReason: activeObsoleteActionError.value || undefined,
    disabledReason: blockerMessage
  })
})
const dccPublishActionProjection = computed(() => {
  const blockerMessage = buildDccActionProjectionMessage(
    activePublishAction.value,
    activePublishActionError.value,
    '发布申请',
    '后端动作投影未允许发布申请。'
  )
  return resolveControlledActionProjection({
    actionCode: 'DCC_PUBLISH',
    actionLabel: '发布申请',
    allowed: detailActionState.value.canPublish,
    locked: Boolean(activePublishActionError.value),
    pending: Boolean(activePublishAction.value),
    pendingInstanceId: activePublishAction.value?.id,
    pendingStatus: activePublishAction.value?.status,
    effectStatus: activePublishAction.value?.status === 'EFFECT_FAILED_PENDING' ? 'EFFECT_FAILED_PENDING' : undefined,
    lockReason: activePublishActionError.value || undefined,
    disabledReason: blockerMessage
  })
})
const detailActionProjectionMessages = computed(() =>
  [
    ...detailActionState.value.blockerMessages,
    dccObsoleteActionProjection.value,
    dccPublishActionProjection.value
  ]
    .filter((state) =>
      typeof state === 'string' ? Boolean(state) : state.projectionMissing || state.effectFailedPending
    )
    .map((state) => (typeof state === 'string' ? state : state.blockerMessage))
    .filter((message, index, messages) => Boolean(message) && messages.indexOf(message) === index)
)
const obsoleteActionLockTitle = computed(() => {
  if (activeObsoleteAction.value) {
    return '作废申请审批中'
  }
  return '作废动作暂不可用'
})
const obsoleteActionLockDescription = computed(() => {
  return dccObsoleteActionProjection.value.blockerMessage
})
const publishActionLockTitle = computed(() => {
  if (activePublishAction.value) {
    return '发布申请审批中'
  }
  return '发布动作暂不可用'
})
const publishActionLockDescription = computed(() => {
  return dccPublishActionProjection.value.blockerMessage
})
const canWithdraw = computed(
  () =>
    isDccControlledFileWithdrawableStatus(fileStatus.value) &&
    fileDetail.value?.requesterId === currentUserId.value
)
const canUploadApplicantTrainingRecord = computed(
  () =>
    fileStatus.value === 'PENDING_APPLICANT_TRAINING_RECORD' &&
    fileDetail.value?.requesterId === currentUserId.value &&
    Boolean(fileDetail.value?.needTraining)
)
const canHandleWithdrawnFlow = computed(
  () =>
    fileStatus.value === 'WITHDRAWN' &&
    fileDetail.value?.requesterId === currentUserId.value &&
    !fileDetail.value?.supersededByFileId
)
const canOpenBpmDetail = computed(
  () =>
    Boolean(route.query.taskId) ||
    Boolean(fileDetail.value?.processInstanceId) ||
    Boolean(activeObsoleteAction.value?.bpmProcessInstanceId) ||
    Boolean(activePublishAction.value?.bpmProcessInstanceId)
)
const hasDetailMoreActions = computed(
  () => canOpenBpmDetail.value || (canEditMetadata.value && !!fileDetail.value) || !!fileDetail.value
)
const hasDetailDangerActions = computed(
  () =>
    canWithdraw.value ||
    canHandleWithdrawnFlow.value ||
    (detailActionState.value.canRetryFinalization && canRetryStampPermission.value) ||
    canSubmitObsoleteAction.value
)
const detailMoreActionLoading = computed(
  () => processPrintLoading.value || processExportLoading.value
)
const detailDangerActionLoading = computed(
  () =>
    withdrawLoading.value ||
    deleteWithdrawnLoading.value ||
    resubmitWithdrawnLoading.value ||
    retryStampLoading.value
)
const originalReleaseApprovalStages: Array<{
  stageCode: DccTaskStageProgress['stageCode']
  stageName: string
  stageOrder: number
}> = [
  { stageCode: 'DOC_CONTROL_REVIEW', stageName: '文控审核', stageOrder: 1 },
  { stageCode: 'MATRIX_REVIEW', stageName: '会签审核', stageOrder: 2 },
  { stageCode: 'MATRIX_APPROVAL', stageName: '会签批准', stageOrder: 3 },
  { stageCode: 'DOC_CONTROL_APPROVAL', stageName: '文控批准', stageOrder: 4 }
]

const buildEmptyOriginalReleaseStage = (
  stage: (typeof originalReleaseApprovalStages)[number]
): DccTaskStageProgress => ({
  ...stage,
  totalCount: 0,
  approvedCount: 0,
  rejectedCount: 0,
  runningCount: 0,
  waitingCount: 0,
  completionText: '0/0',
  sameLayerHint: '待解析审批配置',
  isCurrent: false,
  isCompleted: false,
  isPending: true
})

const displayStageProgressList = computed(() => {
  const progressMap = new Map(stageProgressList.value.map((item) => [item.stageCode, item]))
  return originalReleaseApprovalStages.map(
    (stage) => progressMap.get(stage.stageCode) || buildEmptyOriginalReleaseStage(stage)
  )
})
const currentStage = computed(() => displayStageProgressList.value.find((item) => item.isCurrent))
const currentStageLabel = computed(() => currentStage.value?.stageName || '-')
const isCurrentActiveVersion = computed(() => {
  if (fileDetail.value?.status !== 'ACTIVE') {
    return false
  }
  const activeVersionNo = String(fileDetail.value?.currentActiveVersionNo || '').trim()
  const currentVersionNo = String(fileDetail.value?.versionNo || '').trim()
  return !activeVersionNo || activeVersionNo === currentVersionNo
})
const currentSignerLabel = computed(() => {
  const user = userStore.getUser
  return user?.nickname || (user?.id ? `用户 #${user.id}` : '-')
})
const actionDialogSubmitFlowText = computed(() => {
  if (actionDialog.mode === 'reject') {
    return '提交后流转：当前节点驳回，流程回到发起人或按后端路线规则处理。'
  }
  return `提交后流转：${currentStageLabel.value}完成后进入下一审批节点；末级文控批准后文件发布为 ACTIVE。`
})
const getStageRouteSnapshot = (stage: DccTaskStageProgress) =>
  fileDetail.value?.routeSnapshots?.find(
    (snapshot) =>
      snapshot.stageCode === stage.stageCode ||
      (snapshot.stageOrder ?? snapshot.stageNo) === stage.stageOrder
  )
const getStageTasks = (stage: DccTaskStageProgress) =>
  approvalTaskList.value.filter((task) => task.taskDefinitionKey === stage.stageCode)
const readStageTaskField = (task: DccTaskLike, keys: string[]) => {
  const record = task as DccTaskLike & Record<string, unknown>
  for (const key of keys) {
    const value = record[key]
    if (value !== undefined && value !== null && value !== '') {
      return value
    }
  }
  return undefined
}
const formatStageProgressActors = (stage: DccTaskStageProgress) => {
  const taskActorIds = getStageTasks(stage)
    .map((task) => Number(task.assigneeUserId ?? task.assignee ?? task.assigneeUser?.id ?? task.ownerUser?.id))
    .filter((id) => Number.isFinite(id) && id > 0)
  const uniqueTaskActorIds = Array.from(new Set(taskActorIds))
  if (uniqueTaskActorIds.length) {
    return resolveUserNames(uniqueTaskActorIds)
  }
  return resolveUserNames(getStageRouteSnapshot(stage)?.resolvedUserIds || [])
}
const formatStageProgressTime = (stage: DccTaskStageProgress) => {
  const timeValue = getStageTasks(stage)
    .map((task) =>
      readStageTaskField(task, ['endTime', 'finishTime', 'completeTime', 'updateTime', 'createTime'])
    )
    .find(Boolean)
  return formatControlledFileDateTime(timeValue as string | number | Date | undefined)
}
const getStageSignatures = (stage: DccTaskStageProgress) =>
  (fileDetail.value?.signatureSummaries || []).filter((signature) =>
    String(signature.meaningCode || '').startsWith(stage.stageCode)
  )
const formatStageSignatureStatus = (stage: DccTaskStageProgress) => {
  const signatureCount = getStageSignatures(stage).length
  if (signatureCount > 0) {
    return `${signatureCount} 条签名证据`
  }
  if (stage.isCompleted) {
    return '已完成，签名证据待同步'
  }
  if (stage.isCurrent) {
    return '待当前节点签名'
  }
  return '待进入节点'
}
const formatDetailPath = (items: Array<string | null | undefined>) => {
  const parts = items.map((item) => item?.trim()).filter((item): item is string => Boolean(item))
  return parts.length ? parts.join(' / ') : '-'
}
const currentDccProjectCodeText = computed(() =>
  formatDetailPath([fileDetail.value?.productName, fileDetail.value?.productCode])
);
const currentFileTypeTaxonomyText = computed(() =>
  formatDetailPath([
    fileDetail.value?.fileTypeLevel1,
    fileDetail.value?.fileTypeLevel2,
    fileDetail.value?.fileTypeLevel3,
    fileDetail.value?.fileTypeLevel4,
    fileDetail.value?.fileTypeLevel5
  ])
);
const controlledBrowserDirectoryPath = computed(() => {
  const formalDirectoryPath = fileDetail.value?.directoryPath?.trim()
  if (formalDirectoryPath) {
    return formalDirectoryPath
  }
  const directoryId = fileDetail.value?.directoryId
  if (!directoryId) {
    return '-'
  }
  return directoryPathMap.value.get(directoryId) || directoryNameMap.value.get(directoryId) || '-'
});
const publishedFileBusinessText = computed(() =>
  fileDetail.value?.publishedFileId ? '发布文件：已生成并可用于受控预览' : '发布文件：未生成或未返回'
);
const stampedFileBusinessText = computed(() =>
  fileDetail.value?.stampedFileId ? '盖章文件：已生成，预览优先打开盖章版本' : '盖章文件：未生成或未返回'
);
const currentActiveVersionSourceText = computed(() => {
  const file = fileDetail.value
  if (!file) {
    return '-'
  }
  const activeVersionNo = String(file.currentActiveVersionNo || '').trim()
  const currentVersionNo = String(file.versionNo || '').trim()
  if (file.status === 'ACTIVE' && activeVersionNo && activeVersionNo === currentVersionNo) {
    return `master 当前生效版本 ${currentVersionNo}`
  }
  if (file.status === 'ACTIVE' && !activeVersionNo) {
    return `当前详情 ACTIVE 版本 ${currentVersionNo || '-'}`
  }
  return `非受控浏览当前有效版：${getDetailStatusLabel(file.status)}`
});
const publishVisibilityScopeText = computed(() => {
  const file = fileDetail.value
  if (!file) {
    return '-'
  }
  const categoryText = categoryNameMap.value.get(file.categoryId) || `类别 #${file.categoryId}`
  const directoryText = controlledBrowserDirectoryPath.value
  const projectText = currentDccProjectCodeText.value
  return `类别：${categoryText}；目录：${directoryText}；项目：${projectText}；可见范围以受控浏览 VIEW 权限矩阵为准。`
});

const openControlledBrowserLocation = () => {
  if (!fileDetail.value) {
    return
  }
  window.open(buildControlledFileViewerPath(controlledFileId.value, 'controlled-browser', route.fullPath), '_blank')
}

const openTraceFileEvidence = (row: SignatureTraceRow) => {
  if (!row.hasFileEvidence) {
    message.warning('当前节点暂无盖章/发布文件证据。')
    return
  }
  window.open(buildControlledFileViewerPath(controlledFileId.value, 'signature-trace', route.fullPath), '_blank')
}

interface SignatureTraceRow {
  traceRole: string
  actorName: string
  approvalCommentText: string
  signedAtText: string
  signatureModeText: string
  evidenceStatusText: string
  fileHashText: string
  fileEvidenceText: string
  hasFileEvidence: boolean
}

const formatSignatureTraceComment = (comment?: string | null) => {
  const text = String(comment || '').trim()
  return text || '-'
}

const buildSignatureTraceFileEvidenceText = () => {
  const file = fileDetail.value
  const evidenceParts = [
    file?.publishedFileId ? `publishedFileId：${file.publishedFileId}` : '',
    file?.stampedFileId ? `stampedFileId：${file.stampedFileId}` : ''
  ].filter(Boolean)
  return evidenceParts.length ? evidenceParts.join('；') : '-'
}

const hasSignatureTraceFileEvidence = () =>
  Boolean(fileDetail.value?.publishedFileId || fileDetail.value?.stampedFileId)

const buildSignatureTraceRow = (signature: ControlledFileSignatureSummaryVO): SignatureTraceRow => ({
  traceRole: '四级审批人',
  actorName: getSignatureActorSummary(signature, userNameMap.value),
  approvalCommentText: formatSignatureTraceComment(signature.comment),
  signedAtText: formatControlledFileDateTime(signature.signedAt),
  signatureModeText: signature.signatureMode || signature.authenticationMethod || '-',
  evidenceStatusText: getSignatureEvidenceStatusLabel(signature.evidenceStatus),
  fileHashText: signature.controlledCopyHashShort || signature.sourceFileHashShort || signature.evidenceHashShort || '-',
  fileEvidenceText: buildSignatureTraceFileEvidenceText(),
  hasFileEvidence: hasSignatureTraceFileEvidence()
})

const signatureTraceRows = computed<SignatureTraceRow[]>(() => {
  const file = fileDetail.value
  if (!file) {
    return []
  }
  const rows: SignatureTraceRow[] = []
  if (file.requesterId) {
    rows.push({
      traceRole: '上传人',
      actorName: userNameMap.value.get(file.requesterId) || `用户#${file.requesterId}`,
      approvalCommentText: '-',
      signedAtText: formatControlledFileDateTime(file.submittedTime),
      signatureModeText: '上传提交',
      evidenceStatusText: '-',
      fileHashText: '-',
      fileEvidenceText: buildSignatureTraceFileEvidenceText(),
      hasFileEvidence: hasSignatureTraceFileEvidence()
    })
  }
  rows.push(...(fileDetail.value?.signatureSummaries || []).map(buildSignatureTraceRow))
  return rows
})
const pagedSignatureTraceRows = computed(() =>
  getPagedDetailRows(
    signatureTraceRows.value,
    signatureTraceListState.pageNo,
    signatureTraceListState.pageSize
  )
)
const currentStageSameLayerHint = computed(() =>
  currentStage.value
    ? `${currentStage.value.completionText} 已完成，${currentStage.value.sameLayerHint}`
    : '当前没有待处理的审批阶段'
)
const detailHandlingSummary = computed(() => {
  const file = fileDetail.value
  if (!file) {
    return {
      nextStep: '-',
      responsibilityHint: '-'
    }
  }
  return getControlledFileHandlingSummary({
    status: file.status,
    rejectReason: file.rejectReason,
    finalizationError: file.finalizationError,
    modifying: file.modifying,
    supersededByFileId: file.supersededByFileId,
    hasPendingTrainingAcknowledgement: file.hasPendingTrainingAcknowledgement
  })
})
const detailBlockingReason = computed(
  () => fileDetail.value?.finalizationError || fileDetail.value?.rejectReason || '-'
)
const detailLifecycleTimelineItems = computed(() =>
  buildDetailLifecycleTimelineItems(fileDetail.value, {
    userNameMap: userNameMap.value,
    deptNameMap: deptNameMap.value
  })
)
const versionHistoryById = computed(
  () => new Map((fileDetail.value?.versionHistory || []).map((item) => [item.id, item]))
)
const getVersionHistoryIdentityText = (version: ControlledFileVersionHistoryVO) => {
  const identityParts = [
    version.fileNumber,
    version.versionNo ? `版本 ${version.versionNo}` : '',
    version.title
  ].filter((item): item is string => Boolean(item))
  return identityParts.length ? identityParts.join(' · ') : `记录 #${version.id}`
}
const getVersionChangeReasonText = (version: ControlledFileVersionHistoryVO) => {
  const remark = String(version.remark || '').trim()
  return remark || '-'
}
const getSuccessorVersionSummary = (version: ControlledFileVersionHistoryVO) => {
  const successorId = Number(version.supersededByFileId || 0)
  if (!successorId) {
    return '无后继版本'
  }
  const successor = versionHistoryById.value.get(successorId)
  return successor ? getVersionHistoryIdentityText(successor) : `后继记录缺失：#${successorId}`
}
const versionHistoryRows = computed(() =>
  (fileDetail.value?.versionHistory || []).map((version) => ({
    ...version,
    changeReasonText: getVersionChangeReasonText(version),
    successorVersionSummary: getSuccessorVersionSummary(version)
  }))
)
const pagedVersionHistoryRows = computed(() =>
  getPagedDetailRows(
    versionHistoryRows.value,
    versionHistoryListState.pageNo,
    versionHistoryListState.pageSize
  )
)
const supersededPredecessorVersions = computed(() => {
  const currentId = Number(fileDetail.value?.id || 0)
  if (!currentId) {
    return []
  }
  return (fileDetail.value?.versionHistory || []).filter(
    (version) => version.status === 'SUPERSEDED' && Number(version.supersededByFileId || 0) === currentId
  )
})
const isPublishCompletionSummaryVisible = computed(() => {
  const file = fileDetail.value
  if (!file || file.status !== 'ACTIVE') {
    return false
  }
  return Boolean(
    file.currentActiveVersionNo ||
      file.publishedFileId ||
      file.stampedFileId ||
      supersededPredecessorVersions.value.length
  )
})
const publishCompletionSummaryItems = computed(() => {
  const file = fileDetail.value
  if (!file) {
    return []
  }
  const activeVersionNo = String(file.currentActiveVersionNo || '').trim()
  const currentVersionNo = String(file.versionNo || '').trim()
  const masterPointsToCurrent = Boolean(
    file.status === 'ACTIVE' && (!activeVersionNo || activeVersionNo === currentVersionNo)
  )
  const predecessorText = supersededPredecessorVersions.value.length
    ? supersededPredecessorVersions.value.map(getVersionHistoryIdentityText).join('；')
    : '未发现被当前版本替换的旧版'
  const browserLanded = Boolean(file.publishedFileId && file.stampedFileId)
  return [
    {
      key: 'new-active',
      label: '新版 ACTIVE',
      value: `${currentVersionNo || '-'} / ${getDetailStatusLabel(file.status)}`,
      description: '新版审批完成后已进入 ACTIVE 状态。',
      ok: file.status === 'ACTIVE'
    },
    {
      key: 'old-superseded',
      label: '旧版 SUPERSEDED',
      value: predecessorText,
      description: '旧版通过 supersededByFileId 指向当前生效版本。',
      ok: supersededPredecessorVersions.value.length > 0
    },
    {
      key: 'master-current',
      label: 'master 当前生效版本',
      value: activeVersionNo ? `已指向 ${activeVersionNo}` : '详情未返回 master 当前版本号',
      description: 'master 当前生效版本应与当前详情版本一致。',
      ok: masterPointsToCurrent
    },
    {
      key: 'controlled-browser-landed',
      label: '受控浏览落位',
      value: `目录：${controlledBrowserDirectoryPath.value}；publishedFileId：${file.publishedFileId || '-'}；stampedFileId：${file.stampedFileId || '-'}`,
      description: '受控浏览最终目录、发布文件和盖章文件已在详情页可追溯。',
      ok: browserLanded
    },
    {
      key: 'visibility-scope',
      label: '可见范围说明',
      value: publishVisibilityScopeText.value,
      description: '发布完成后按分类、目录、项目代码和 VIEW 权限矩阵决定可见人员。',
      ok: true
    }
  ]
})
const getPreviewApprovalProgress = (snapshot: ControlledFileRouteSnapshotVO) =>
  stageProgressList.value.find(
    (item) =>
      item.stageCode === snapshot.stageCode ||
      item.stageOrder === (snapshot.stageOrder ?? snapshot.stageNo)
  )

const getRouteSnapshotCandidateText = (snapshot: ControlledFileRouteSnapshotVO) => {
  const candidateIds =
    snapshot.candidateSourceIds?.length || !snapshot.candidateSourceId
      ? snapshot.candidateSourceIds || []
      : [snapshot.candidateSourceId]
  if (snapshot.candidateSourceType === 'USER') {
    return resolveUserNames(candidateIds)
  }
  if (snapshot.candidateSourceType === 'POSITION') {
    return candidateIds.length
      ? `按 ${candidateIds.length} 个审批角色解析`
      : '按审批角色解析'
  }
  return '按路线配置解析'
}

const routeSnapshotRows = computed(() =>
  [...(fileDetail.value?.routeSnapshots || [])]
    .sort(
      (left, right) =>
        (left.stageOrder ?? left.stageNo ?? 0) - (right.stageOrder ?? right.stageNo ?? 0)
    )
    .map((snapshot) => {
      const progress = getPreviewApprovalProgress(snapshot)
      const stageDisplayName =
        progress?.stageName ||
        resolveDccStageDisplayName(
          snapshot.stageCode,
          snapshot.stageName || snapshot.stageCode || `阶段 ${snapshot.stageNo || '-'}`
        )
      const approveRatioText =
        snapshot.approveRatio === undefined || snapshot.approveRatio === null
          ? '-'
          : `${snapshot.approveRatio}%`
      const approveMethodLabel = getOptionLabel(ROUTE_APPROVE_METHOD_OPTIONS, snapshot.approveMethod)
      const approvalProgressText = progress?.completionText || '-'
      const approvalHint = progress?.sameLayerHint || '-'
      const approvalStatusLabel = progress?.isCompleted
        ? '已完成'
        : progress?.isCurrent
          ? '当前阶段'
          : '待处理'
      return {
        ...snapshot,
        stageDisplayName,
        stageMetaText: `版本 ${snapshot.routeVersionNo || '-'} · 阶段 ${snapshot.stageNo || '-'} · ${
          snapshot.stageCode || '-'
        }`,
        candidateSourceLabel: getOptionLabel(ROUTE_CANDIDATE_SOURCE_OPTIONS, snapshot.candidateSourceType),
        candidateText: getRouteSnapshotCandidateText(snapshot),
        approveMethodLabel,
        approveRatioText,
        approvalProgressText,
        approvalHint,
        approvalStatusLabel,
        approvalRequirementText: `${approvalStatusLabel} ${approveMethodLabel} ${approveRatioText} ${approvalProgressText} ${approvalHint}`,
        resolvedUserNames: resolveUserNames(snapshot.resolvedUserIds),
        approvalStatusTagType: progress?.isCompleted
          ? 'success'
          : progress?.isCurrent
            ? 'primary'
            : 'info'
      }
    })
)
const pagedRouteSnapshotRows = computed(() =>
  getPagedDetailRows(
    routeSnapshotRows.value,
    routeSnapshotListState.pageNo,
    routeSnapshotListState.pageSize
  )
)

const previewApprovalRows = computed(() =>
  [...(fileDetail.value?.routeSnapshots || [])]
    .sort(
      (left, right) =>
        (left.stageOrder ?? left.stageNo ?? 0) - (right.stageOrder ?? right.stageNo ?? 0)
    )
    .map((snapshot) => {
      const progress = getPreviewApprovalProgress(snapshot)
      const stageName = resolveDccStageDisplayName(
        snapshot.stageCode,
        snapshot.stageName || snapshot.stageCode || `阶段 ${snapshot.stageNo || '-'}`
      )
      return {
        ...snapshot,
        stageDisplayName: progress?.stageName || stageName,
        approveMethodLabel: getOptionLabel(ROUTE_APPROVE_METHOD_OPTIONS, snapshot.approveMethod),
        approveRatioText:
          snapshot.approveRatio === undefined || snapshot.approveRatio === null
            ? '-'
            : `${snapshot.approveRatio}%`,
        resolvedUserNames: resolveUserNames(snapshot.resolvedUserIds),
        approvalProgressText: progress?.completionText || '-',
        approvalHint: progress?.sameLayerHint || '-',
        approvalStatusLabel: progress?.isCompleted
          ? '已完成'
          : progress?.isCurrent
            ? '当前阶段'
            : '待处理',
        approvalStatusTagType: progress?.isCompleted
          ? 'success'
          : progress?.isCurrent
            ? 'primary'
            : 'info'
      }
    })
)
const isReturnedApplicantTask = computed(
  () =>
    Boolean(approvalTodoTask.value?.id) &&
    fileStatus.value === 'PENDING_APPLICANT_REWORK' &&
    fileDetail.value?.requesterId === currentUserId.value &&
    Boolean(fileDetail.value?.rejectReason?.includes('流程回退'))
)
const approvalActionLabels = computed(() => {
  const labels = getDccApprovalActionLabels(approvalTodoTask.value?.taskDefinitionKey)
  if (!isReturnedApplicantTask.value) {
    return labels
  }
  return {
    ...labels,
    approveText: '处理回退',
    dialogTitle: '流程回退处理签名'
  }
})
const actionDialogSignatureMeaning = computed(() => {
  if (isReturnedApplicantTask.value && actionDialog.mode === 'approve') {
    return '流程回退处理通过'
  }
  return getDccApprovalSignatureMeaningPreview(
    approvalTodoTask.value?.taskDefinitionKey,
    actionDialog.mode
  )
})
const isFourthNodeApprovalTask = computed(
  () =>
    approvalTodoTask.value?.taskDefinitionKey === 'DOC_CONTROL_APPROVAL' ||
    fileStatus.value === 'PENDING_DOC_CONTROL_APPROVAL'
)
const isExternalReviewProcess = computed(
  () =>
    fileDetail.value?.processType === 'EXTERNAL_REVIEW' ||
    fileDetail.value?.processDefinitionKey === 'dcc-external-file-review'
)
const shouldCollectFourthNodeFiles = computed(
  () =>
    actionDialog.visible &&
    actionDialog.mode === 'approve' &&
    isFourthNodeApprovalTask.value &&
    !isExternalReviewProcess.value
)
const shouldCollectExternalReviewConclusion = computed(
  () =>
    actionDialog.visible &&
    actionDialog.mode === 'approve' &&
    isFourthNodeApprovalTask.value &&
    isExternalReviewProcess.value
)
const returnTargetOptions = computed(() => {
  const currentTaskDefinitionKey = approvalTodoTask.value?.taskDefinitionKey || currentStage.value?.stageCode
  if (currentTaskDefinitionKey === APPLICANT_REWORK_TASK_DEFINITION_KEY) {
    return []
  }
  const snapshots = [...(fileDetail.value?.routeSnapshots || [])]
    .filter((item) => item.stageCode)
    .sort(
      (left, right) =>
        (left.stageOrder ?? left.stageNo ?? 0) - (right.stageOrder ?? right.stageNo ?? 0)
    )
  const currentIndex = snapshots.findIndex((item) => item.stageCode === currentTaskDefinitionKey)
  const options = [
    {
      label: '申请人修改',
      value: APPLICANT_REWORK_TASK_DEFINITION_KEY
    }
  ]
  if (currentIndex > 0) {
    const target = snapshots[currentIndex - 1]
    if (target.stageCode) {
      options.push({
        label: resolveDccStageDisplayName(target.stageCode, target.stageName || target.stageCode),
        value: target.stageCode
      })
    }
  }
  return options
})
const taskActionDialogTitle = computed(() => {
  const titleMap: Record<DccTaskActionMode, string> = {
    return: '回退签名',
    transfer: '转办签名',
    sign: '加签签名'
  }
  return titleMap[taskActionDialog.mode]
})
const pendingTrainingAssignments = computed(() =>
  getPendingTrainingAssignments(fileDetail.value?.trainingStatuses, currentUserId.value)
)
const flattenedTrainingAssignments = computed(() =>
  flattenTrainingAssignments(fileDetail.value?.trainingStatuses)
)
const trainingCompletionSummary = computed(() =>
  getDetailTrainingCompletionSummary(flattenedTrainingAssignments.value, userNameMap.value)
)
const trainingStatusRows = computed(() =>
  flattenedTrainingAssignments.value.map((row) => {
    const summary = getDetailTrainingAssignmentSummary(row)
    return {
      ...row,
      departmentName: deptNameMap.value.get(row.departmentId) || `部门#${row.departmentId}`,
      traineeName: getTrainingAssignmentUserSummary(row, userNameMap.value),
      trainingSummary: summary,
      trainingSummaryText: [
        summary.statusLabel,
        summary.progressText,
        summary.eligibilityLabel,
        `部门：${summary.departmentStatusLabel}`,
        `确认：${summary.acknowledgedAtText}`
      ].join(' ')
    }
  })
)
const pagedTrainingStatusRows = computed(() =>
  getPagedDetailRows(
    trainingStatusRows.value,
    trainingStatusListState.pageNo,
    trainingStatusListState.pageSize
  )
)
const manualReleasePermissionGapVisible = computed(
  () => fileStatus.value === 'PENDING_MANUAL_DISTRIBUTION' && !detailActionState.value.canManualRelease
)
const distributionReceiptRows = computed(() =>
  paperDistributionRecords.value
)

const getControlledPrintStatusLabel = (status: string | undefined) => {
  const statusMap: Record<string, string> = {
    DIRECT_PRINTED: '已直接打印',
    APPROVED: '已审批',
    PENDING_APPROVAL: '待审批',
    REJECTED: '已驳回'
  }
  return status ? statusMap[status] || status : '-'
}

const buildControlledPrintCopyNumbers = (record?: ControlledFilePrintRecordVO | null) => {
  if (!record?.printNo) {
    return []
  }
  const copies = Math.max(1, Math.floor(Number(record.copies || 1)))
  const width = Math.max(2, String(copies).length)
  return Array.from({ length: copies }, (_, index) => `${record.printNo}-${String(index + 1).padStart(width, '0')}`)
}

const formatControlledPrintCopyNumberRange = (record?: ControlledFilePrintRecordVO | null) => {
  const copyNumbers = buildControlledPrintCopyNumbers(record)
  if (!copyNumbers.length) {
    return '-'
  }
  return copyNumbers.length === 1
    ? copyNumbers[0]
    : `${copyNumbers[0]} ~ ${copyNumbers[copyNumbers.length - 1]}`
}

const controlledPrintRecordRows = computed(() =>
  controlledPrintRecords.value.map((record) => ({
    ...record,
    copyNumbers: formatControlledPrintCopyNumberRange(record),
    printUserName: record.printUserName || userNameMap.value.get(record.printUserId) || `用户#${record.printUserId}`,
    approvalStatusText: getControlledPrintStatusLabel(record.approvalStatus)
  }))
)
const pagedControlledPrintRecordRows = computed(() =>
  getPagedDetailRows(
    controlledPrintRecordRows.value,
    controlledPrintRecordListState.pageNo,
    controlledPrintRecordListState.pageSize
  )
)

const formatControlledPrintCopyNumberList = (record?: ControlledFilePrintRecordVO | null) => {
  const copyNumbers = buildControlledPrintCopyNumbers(record)
  return copyNumbers.length ? copyNumbers.join('、') : '-'
}

const clearControlledPrintRecordHighlightTimer = () => {
  if (controlledPrintHighlightTimer) {
    clearTimeout(controlledPrintHighlightTimer)
    controlledPrintHighlightTimer = undefined
  }
}

const getControlledPrintRecordRowClassName = ({
  row
}: {
  row: ControlledFilePrintRecordVO
}) => (row.id === controlledPrintHighlightRecordId.value ? 'controlled-print-record-row--latest' : '')

const focusControlledPrintRecord = async (recordId: number | undefined, shouldScroll = true) => {
  if (!recordId) {
    return
  }
  latestControlledPrintRecordId.value = recordId
  controlledPrintHighlightRecordId.value = recordId
  clearControlledPrintRecordHighlightTimer()
  const recordIndex = controlledPrintRecordRows.value.findIndex((row) => row.id === recordId)
  if (recordIndex >= 0) {
    const safePageSize = Math.max(1, Number(controlledPrintRecordListState.pageSize) || 10)
    controlledPrintRecordListState.pageNo = Math.floor(recordIndex / safePageSize) + 1
  }
  await nextTick()
  if (shouldScroll) {
    const section = document.querySelector('[data-testid="dcc-controlled-print-records"]')
    section?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    const marker = document.querySelector(`[data-controlled-print-record-id="${recordId}"]`)
    marker?.closest('tr')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
  controlledPrintHighlightTimer = setTimeout(() => {
    if (controlledPrintHighlightRecordId.value === recordId) {
      controlledPrintHighlightRecordId.value = undefined
    }
  }, 8000)
}

const handleViewLatestControlledPrintRecord = async () => {
  controlledPrintResultDialog.visible = false
  await focusControlledPrintRecord(latestControlledPrintRecordId.value, true)
}

const shouldLoadControlledPrintRecords = () =>
  Boolean(controlledFileId.value) &&
  !viewerMode.value &&
  showLifecycleTraceSections.value &&
  controlledPrintAllowed.value

const loadControlledPrintRecords = async () => {
  controlledPrintRecordsError.value = ''
  if (!shouldLoadControlledPrintRecords()) {
    controlledPrintRecords.value = []
    return
  }
  controlledPrintRecordsLoading.value = true
  try {
    controlledPrintRecords.value = await getControlledFilePrintRecords(controlledFileId.value)
  } catch (error) {
    controlledPrintRecords.value = []
    controlledPrintRecordsError.value = resolveReadSideErrorMessage(
      error,
      '受控打印记录加载失败，请查看后端错误后重试。'
    )
  } finally {
    controlledPrintRecordsLoading.value = false
  }
}

const buildObsoleteBusinessActionContext = (
  detail: ControlledFileVO | undefined,
  reason = '作废当前版本'
): BusinessActionContextVO | null => {
  if (!detail?.id || !detail.versionNo || !detail.status) {
    return null
  }
  return {
    dataDomain: 'DCC',
    systemCode: 'DCC',
    objectType: 'CONTROLLED_FILE',
    objectId: String(detail.id),
    objectVersion: detail.versionNo,
    actionCode: DCC_OBSOLETE_ACTION_CODE,
    objectState: detail.status,
    orgCode: '',
    deptCode: String(userStore.getUser.deptId || ''),
    roleCodes: userStore.getRoles,
    productCode: detail.productCode || '',
    categoryCode: detail.categoryId ? String(detail.categoryId) : '',
    reason
  }
}

const dccObsoleteFormCenterContext = computed(() =>
  buildObsoleteBusinessActionContext(
    fileDetail.value,
    obsoleteDialog.reason.trim() || '作废当前版本'
  )
)
const dccObsoleteFormCenterFormData = computed(() => ({
  reason: obsoleteDialog.reason.trim(),
  obsoleteReason: obsoleteDialog.reason.trim(),
  controlledFileId: controlledFileId.value,
  fileNumber: fileDetail.value?.fileNumber || '',
  versionNo: fileDetail.value?.versionNo || '',
  startUserSelectAssignees: obsoleteDialog.startUserSelectAssignees
}))

const buildPublishBusinessActionContext = (
  detail: ControlledFileVO | undefined,
  reason = '发布候选版本'
): BusinessActionContextVO | null => {
  if (!detail?.id || !detail.versionNo || !detail.status) {
    return null
  }
  return {
    dataDomain: 'DCC',
    systemCode: 'DCC',
    objectType: 'CONTROLLED_FILE',
    objectId: String(detail.id),
    objectVersion: detail.versionNo,
    actionCode: DCC_PUBLISH_ACTION_CODE,
    objectState: detail.status,
    orgCode: '',
    deptCode: String(userStore.getUser.deptId || ''),
    roleCodes: userStore.getRoles,
    productCode: detail.productCode || '',
    categoryCode: detail.categoryId ? String(detail.categoryId) : '',
    reason
  }
}

const loadActiveObsoleteAction = async (detail: ControlledFileVO) => {
  activeObsoleteAction.value = null
  activeObsoleteActionError.value = ''
  if (viewerMode.value || isBrowserTraceabilityPage.value) {
    return
  }
  if (detail.status !== 'ACTIVE') {
    return
  }
  const context = buildObsoleteBusinessActionContext(detail)
  if (!context) {
    activeObsoleteActionError.value = '当前文件缺少平台动作上下文，作废入口已锁定。'
    return
  }
  try {
    activeObsoleteAction.value = await findActiveBusinessAction(context)
  } catch (error) {
    activeObsoleteActionError.value = resolveReadSideErrorMessage(
      error,
      '作废动作状态加载失败，请查看后端错误后重试。'
    )
  }
}

const loadActivePublishAction = async (detail: ControlledFileVO) => {
  activePublishAction.value = null
  activePublishActionError.value = ''
  if (viewerMode.value || isBrowserTraceabilityPage.value) {
    return
  }
  if (detail.status !== 'READY_TO_PUBLISH') {
    return
  }
  const context = buildPublishBusinessActionContext(detail)
  if (!context) {
    activePublishActionError.value = '当前候选版本缺少平台动作上下文，发布入口已锁定。'
    return
  }
  try {
    activePublishAction.value = await findActiveBusinessAction(context)
  } catch (error) {
    activePublishActionError.value = resolveReadSideErrorMessage(
      error,
      '发布动作状态加载失败，请查看后端错误后重试。'
    )
  }
}

const loadData = async () => {
  const [
    detail,
    categoryList,
    directoryTree,
    users,
    departments,
    paperRecords,
    approvalPrintTemplate
  ] = await Promise.all([
    getControlledFile(controlledFileId.value),
    getFileCategoryList(),
    getDirectoryTree(),
    getSimpleUserList(),
    getSimpleDeptList(),
    viewerMode.value || !showLifecycleTraceSections.value
      ? Promise.resolve([])
      : getPaperDistributionRecords(controlledFileId.value),
    viewerMode.value || !showLifecycleTraceSections.value
      ? Promise.resolve(null)
      : getActiveApprovalPrintTemplate()
  ])
  fileDetail.value = detail
  await loadActiveObsoleteAction(detail)
  await loadActivePublishAction(detail)
  fileAccessExplanation.value = detail.accessExplanation || null
  categories.value = categoryList
  directories.value = directoryTree
  activeApprovalPrintTemplate.value = approvalPrintTemplate
  paperDistributionRecords.value = paperRecords || []
  await loadControlledPrintRecords()
  categoryNameMap.value = new Map(categoryList.map((item) => [item.id as number, item.name]))
  directoryNameMap.value = new Map(
    flattenTree(directoryTree).map((item) => [item.id as number, item.name])
  )
  directoryPathMap.value = new Map(
    flattenTree(directoryTree).map((item) => [item.id as number, item.directoryPath || item.name])
  )
  userNameMap.value = new Map(
    users.map((item: UserVO) => {
      const displayName = buildDetailUserDisplayName(item)
      return [item.id, displayName === '-' ? `用户#${item.id}` : displayName]
    })
  )
  departmentList.value = departments
  deptNameMap.value = new Map(departments.map((item: DeptVO) => [item.id as number, item.name]))
}

const loadDccSignatureEvidenceList = async () => {
  dccSignatureEvidenceError.value = ''
  if (viewerMode.value || !showSignatureTraceSections.value) {
    dccSignatureEvidenceList.value = []
    dccSignatureEvidenceTotal.value = 0
    return
  }
  if (!controlledFileId.value) {
    dccSignatureEvidenceList.value = []
    dccSignatureEvidenceTotal.value = 0
    return
  }
  if (!checkPermi(['dcc:controlled-file:signature:manage'])) {
    dccSignatureEvidenceError.value =
      '当前可查看签核追溯摘要；高级签名留痕需 DCC 电子签名管理权限。'
    dccSignatureEvidenceList.value = []
    dccSignatureEvidenceTotal.value = 0
    return
  }
  dccSignatureEvidenceLoading.value = true
  try {
    const data = await getDccElectronicSignaturePage({
      controlledFileId: controlledFileId.value,
      pageNo: dccSignatureEvidenceQueryParams.pageNo,
      pageSize: dccSignatureEvidenceQueryParams.pageSize
    })
    dccSignatureEvidenceList.value = data.list || []
    dccSignatureEvidenceTotal.value = data.total || 0
  } catch (error) {
    dccSignatureEvidenceList.value = []
    dccSignatureEvidenceTotal.value = 0
    dccSignatureEvidenceError.value = resolveReadSideErrorMessage(
      error,
      '签名留痕加载失败；审批任务加载不受影响，请根据后端错误提示修正权限后重试。'
    )
  } finally {
    dccSignatureEvidenceLoading.value = false
  }
}

const loadAccessExplanationOnly = async () => {
  try {
    fileAccessExplanation.value = await getControlledFileAccessExplanation(controlledFileId.value)
    accessExplanationError.value = ''
  } catch (error) {
    fileAccessExplanation.value = null
    accessExplanationError.value = resolveReadSideErrorMessage(
      error,
      '权限说明加载失败，请根据后端错误提示修正后重试。'
    )
  }
}

const toNumericUserId = (value: unknown) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : undefined
}

const findCurrentUserTodoTask = (taskList: DccTaskLike[]) => {
  const userId = toNumericUserId(currentUserId.value)
  if (userId === undefined) {
    return null
  }
  return (
    taskList.find((task) => {
      const taskAssigneeId = toNumericUserId(task.assigneeUser?.id)
      const taskAssigneeUserId = toNumericUserId(task.assigneeUserId)
      const taskAssignee = toNumericUserId(task.assignee)
      return (
        (taskAssigneeId === userId || taskAssigneeUserId === userId || taskAssignee === userId) &&
        (task.status === DCC_BPM_TASK_STATUS.RUNNING ||
          task.status === DCC_BPM_TASK_STATUS.APPROVING)
      )
    }) || null
  )
}

const syncStageProgress = () => {
  stageProgressList.value = buildDccTaskStageProgress({
    routeSnapshots: fileDetail.value?.routeSnapshots,
    taskList: approvalTaskList.value,
    fileStatus: fileStatus.value
  })
}

const loadApprovalDetail = async () => {
  const processInstanceId = String(route.query.processInstanceId || fileDetail.value?.processInstanceId || '')
  const taskId = String(route.query.taskId || '')
  if (isBrowserTraceabilityPage.value || !processInstanceId) {
    approvalTodoTask.value = null
    approvalTaskList.value = []
    syncStageProgress()
    return
  }
  const canLoadApprovalDetail = Boolean(taskId) && checkPermi(['bpm:process-instance:query'])
  approvalLoading.value = true
  try {
    const [taskList, detail] = await Promise.all([
      TaskApi.getTaskListByProcessInstanceId(processInstanceId),
      canLoadApprovalDetail
        ? ProcessInstanceApi.getApprovalDetail({ processInstanceId, taskId })
        : Promise.resolve(null)
    ])
    const normalizedTaskList = taskList as DccTaskLike[]
    approvalTaskList.value = normalizedTaskList
    approvalTodoTask.value = detail?.todoTask || findCurrentUserTodoTask(normalizedTaskList)
    syncStageProgress()
  } finally {
    approvalLoading.value = false
  }
}

const reloadAll = async () => {
  try {
    accessExplanationError.value = ''
    await loadData()
    await loadDccSignatureEvidenceList()
    await loadApprovalDetail()
    await openControlledPrintDialogFromRoute()
  } catch (error) {
    await loadAccessExplanationOnly()
    const errorMessage = resolveReadSideErrorMessage(error, '受控文件详情加载失败，请查看权限说明后重试。')
    message.error(accessExplanationError.value ? `${errorMessage}；${accessExplanationError.value}` : errorMessage)
  }
}

const formatAccessExplanation = (source?: string, reason?: string) => {
  if (!source && !reason) {
    return '-'
  }
  const sourceLabelMap: Record<string, string> = {
    CURRENT_VIEW_MATRIX: '当前查看矩阵',
    CURRENT_REVIEW_MATRIX: '当前审阅矩阵',
    ROUTE_SNAPSHOT: 'route snapshot',
    REQUESTER_SELF: '申请人自查',
    DIRECTORY_ADMIN: '目录管理员',
    DOWNLOAD_POLICY: '下载策略',
    DENIED: '拒绝'
  }
  const sourceLabel = source ? sourceLabelMap[source] || source : '-'
  return reason ? `${sourceLabel}：${reason}` : sourceLabel
}

const resolveUserNames = (userIds: number[]) => {
  if (!userIds || userIds.length === 0) {
    return '-'
  }
  return userIds.map((item) => userNameMap.value.get(item) || `用户#${item}`).join('、')
}

const openPreview = () => {
  window.open(buildControlledFileViewerPath(controlledFileId.value, 'detail', route.fullPath), '_blank')
}

const openMetadataDialog = () => {
  metadataDialogVisible.value = true
}

const openPreviewApprovalDialog = () => {
  previewInfoDialogs.approval = true
}

const openPreviewDistributionDialog = () => {
  previewInfoDialogs.distribution = true
}

const openPreviewVersionDialog = () => {
  previewInfoDialogs.version = true
}

const handleRecognizeProjectCode = async () => {
  if (!fileDetail.value?.id || projectCodeRecognitionLoading.value) {
    return
  }
  projectCodeRecognitionLoading.value = true
  try {
    const result = await recognizeControlledFileProjectCode(fileDetail.value.id)
    if (result.recognitionStatus === 'UNKNOWN_DCC_BASIC_DATA' || result.recognitionStatus === 'NO_MATCH') {
      message.success('识别完成，未知基础数据，请人工确认')
    } else if (result.recognitionStatus === 'UNRECOGNIZED_PROJECT_NAME') {
      message.success('识别完成，未识别项目名称，请人工确认')
    } else {
      message.success(`已识别基础信息：${result.projectName || '-'} / ${result.projectCode || '-'}`)
    }
    await reloadAll()
  } catch (error) {
    message.error(resolveReadSideErrorMessage(error, '基础信息识别失败，请查看错误提示后重试。'))
  } finally {
    projectCodeRecognitionLoading.value = false
  }
}

const openDccProjectCode = (projectCodeId: number) => {
  if (!fileDetail.value?.id) {
    router.push({
      path: '/mdm/project-code',
      query: { projectCodeId: String(projectCodeId) }
    })
    return
  }
  router.push({
    path: '/mdm/project-code',
    query: {
      projectCodeId: String(projectCodeId),
      associatedFocus: '1',
      associatedFileId: String(fileDetail.value.id),
      fileTypeTaxonomyId: fileDetail.value.fileTypeTaxonomyId
        ? String(fileDetail.value.fileTypeTaxonomyId)
        : undefined
    }
  })
}

const openDccProjectCodeTrace = () => {
  if (!fileDetail.value?.id) {
    return
  }
  router.push({
    path: '/dcc/controlled-file/logs',
    query: {
      logType: 'PROJECT_CODE_CHANGE',
      controlledFileId: String(fileDetail.value.id),
      projectCodeId: fileDetail.value.dccProjectCodeId
        ? String(fileDetail.value.dccProjectCodeId)
        : undefined
    }
  })
}

const handleMetadataSaved = async () => {
  await reloadAll()
}

const openHistoryDetail = (id: number | string) => {
  router.push(buildControlledFileViewerPath(id, 'version-history', route.fullPath))
}

const closeViewerMode = () => {
  const resolvedReturnTo = resolveControlledFileViewerReturnTo(route.query.returnTo)
  if (resolvedReturnTo) {
    router.push(resolvedReturnTo)
    return
  }
  const nextQuery = { ...route.query }
  delete nextQuery.viewer
  delete nextQuery.from
  delete nextQuery.returnTo
  router.push({
    name: 'DccControlledFileBrowser',
    query: nextQuery
  })
}

const openBpmDetail = () => {
  router.push({
    name: 'BpmProcessInstanceDetail',
    query: {
      id:
        route.query.processInstanceId ||
        fileDetail.value?.processInstanceId ||
        activeObsoleteAction.value?.bpmProcessInstanceId ||
        activePublishAction.value?.bpmProcessInstanceId,
      taskId: route.query.taskId
    }
  })
}

const isDialogCancelled = (error: unknown) => error === 'cancel' || error === 'close'

const clearActionDialogFieldError = (field: string) => {
  delete actionDialog.fieldErrors[field]
}

const isPdfUploadFile = (file: { name: string; raw?: File }) =>
  file.raw?.type === 'application/pdf' || /\.pdf$/i.test(file.name)

const isEditableOutputFile = (fileName: string) =>
  /\.(doc|docx|xls|xlsx|dwg|sldprt|sldasm|slddrw)$/i.test(fileName)

const buildDetailUploadPreviewContext = (sessionId: string) => {
  if (!fileDetail.value?.categoryId) {
    throw new Error('当前文件缺少类别信息，无法创建上传凭证')
  }
  return {
    categoryId: fileDetail.value.categoryId,
    sessionId
  }
}

const hasLeafUploadDirectory = (
  nodes: ControlledFileUploadDirectoryNodeVO[],
  directoryId?: number
): boolean => {
  if (!directoryId) {
    return false
  }
  for (const node of nodes) {
    if (node.id === directoryId) {
      return node.leaf
    }
    if (node.children?.length && hasLeafUploadDirectory(node.children, directoryId)) {
      return true
    }
  }
  return false
}

const loadDocControlDirectoryTree = async () => {
  if (!fileDetail.value?.categoryId) {
    throw new Error('当前文件缺少类别信息，无法确认存入路径')
  }
  docControlDirectoryTreeLoading.value = true
  docControlDirectoryTreeOptions.value = []
  try {
    const directoryTree = await getControlledFileUploadDirectoryTree(fileDetail.value.categoryId)
    docControlDirectoryTreeOptions.value = directoryTree.children || []
    const currentDirectoryId = fileDetail.value.directoryId
    if (hasLeafUploadDirectory(docControlDirectoryTreeOptions.value, currentDirectoryId)) {
      fourthNodeUpload.confirmedDirectoryId = currentDirectoryId
    } else if (directoryTree.leafBinding) {
      fourthNodeUpload.confirmedDirectoryId = directoryTree.bindingDirectoryId
    }
  } finally {
    docControlDirectoryTreeLoading.value = false
  }
}

const cleanupDetailUploadSession = async (
  sessionId: string,
  hasTemporaryFile: boolean,
  requestId?: string,
  applyInlineError?: (message: string) => void
) => {
  if (!hasTemporaryFile) {
    return true
  }
  try {
    await cleanupControlledFileUploadSession(sessionId, requestId)
    return true
  } catch (error) {
    const errorMessage = resolveReadSideErrorMessage(error, '临时文件清理失败，请处理后再继续。')
    if (applyInlineError) {
      applyInlineError(errorMessage)
    } else {
      message.error(errorMessage)
    }
    return false
  }
}

const cleanupApplicantTrainingRecordUploadSession = async () =>
  await cleanupDetailUploadSession(
    applicantTrainingRecordUploadSessionId.value,
    Boolean(applicantTrainingRecordDialog.file?.uploadTicket),
    applicantTrainingRecordDialog.file?.requestId,
    (errorMessage) => {
      applicantTrainingRecordDialog.inlineError = errorMessage
    }
  )

const cleanupFourthNodeUploadSessions = async () => {
  const stampedCleaned = await cleanupDetailUploadSession(
    fourthNodeUploadSessionId.value,
    Boolean(fourthNodeUpload.stampedPdf?.uploadTicket),
    fourthNodeUpload.stampedPdf?.requestId,
    (errorMessage) => {
      actionDialog.inlineError = errorMessage
    }
  )
  if (!stampedCleaned) {
    return false
  }
  return await cleanupDetailUploadSession(
    externalOutputUploadSessionId.value,
    Boolean(externalReviewAction.outputFile?.uploadTicket),
    externalReviewAction.outputFile?.requestId,
    (errorMessage) => {
      actionDialog.inlineError = errorMessage
    }
  )
}

const resetFourthNodeUploads = () => {
  fourthNodeUploadSessionId.value = createControlledFileUploadSessionId()
  externalOutputUploadSessionId.value = createControlledFileUploadSessionId()
  stampedPdfFileList.value = []
  fourthNodeUpload.stampedPdf = undefined
  fourthNodeUpload.confirmedDirectoryId = undefined
  fourthNodeUpload.selectedDistributionScopes = []
  fourthNodeUpload.stampedLoading = false
  stampedPdfUploadRef.value?.clearFiles()
  externalOutputFileList.value = []
  externalReviewAction.reviewConclusion = ''
  externalReviewAction.conclusionComment = ''
  externalReviewAction.outputFile = undefined
  externalReviewAction.outputLoading = false
  externalOutputUploadRef.value?.clearFiles()
}

const handleStampedPdfExceed: UploadProps['onExceed'] = () => {
  message.error('只允许上传一个盖章 PDF')
}

const handleApplicantTrainingRecordExceed: UploadProps['onExceed'] = () => {
  message.error('只允许上传一个培训记录文件')
}

const handleStampedPdfChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  if (!file.raw) {
    return
  }
  if (!isPdfUploadFile({ name: file.name, raw: file.raw as File })) {
    actionDialog.fieldErrors.stampedPdfUploadTicket = '盖章 PDF 必须为 PDF 格式'
    stampedPdfUploadRef.value?.clearFiles()
    stampedPdfFileList.value = []
    fourthNodeUpload.stampedPdf = undefined
    return
  }
  clearActionDialogFieldError('stampedPdfUploadTicket')
  stampedPdfFileList.value = uploadFiles.slice(-1)
  fourthNodeUpload.stampedPdf = undefined
  fourthNodeUpload.stampedLoading = true
  try {
    fourthNodeUpload.stampedPdf = await uploadControlledFilePreview(
      file.raw as File,
      'DRAWING_PDF',
      buildDetailUploadPreviewContext(fourthNodeUploadSessionId.value)
    )
  } catch (error) {
    stampedPdfFileList.value = []
    fourthNodeUpload.stampedPdf = undefined
    actionDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '盖章 PDF 上传失败，请查看错误提示后重试。'
    )
  } finally {
    fourthNodeUpload.stampedLoading = false
  }
}

const handleBeforeStampedPdfRemove: UploadProps['beforeRemove'] = async () => {
  return await cleanupDetailUploadSession(
    fourthNodeUploadSessionId.value,
    Boolean(fourthNodeUpload.stampedPdf?.uploadTicket),
    fourthNodeUpload.stampedPdf?.requestId,
    (errorMessage) => {
      actionDialog.inlineError = errorMessage
    }
  )
}

const handleStampedPdfRemove: UploadProps['onRemove'] = () => {
  stampedPdfFileList.value = []
  fourthNodeUpload.stampedPdf = undefined
  clearActionDialogFieldError('stampedPdfUploadTicket')
}

const clearApplicantTrainingRecordFieldError = (field: string) => {
  delete applicantTrainingRecordDialog.fieldErrors[field]
}

const handleApplicantTrainingRecordChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  if (!file.raw) {
    return
  }
  clearApplicantTrainingRecordFieldError('trainingRecordUploadTicket')
  applicantTrainingRecordFileList.value = uploadFiles.slice(-1)
  applicantTrainingRecordDialog.file = undefined
  applicantTrainingRecordDialog.uploading = true
  try {
    applicantTrainingRecordDialog.file = await uploadControlledFilePreview(
      file.raw as File,
      'TRAINING_RECORD',
      buildDetailUploadPreviewContext(applicantTrainingRecordUploadSessionId.value)
    )
  } catch (error) {
    applicantTrainingRecordFileList.value = []
    applicantTrainingRecordDialog.file = undefined
    applicantTrainingRecordDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '培训记录上传失败，请查看错误提示后重试。'
    )
  } finally {
    applicantTrainingRecordDialog.uploading = false
  }
}

const handleBeforeApplicantTrainingRecordRemove: UploadProps['beforeRemove'] = async () => {
  return await cleanupApplicantTrainingRecordUploadSession()
}

const handleApplicantTrainingRecordRemove: UploadProps['onRemove'] = () => {
  applicantTrainingRecordFileList.value = []
  applicantTrainingRecordDialog.file = undefined
  clearApplicantTrainingRecordFieldError('trainingRecordUploadTicket')
}

const handleExternalOutputFileChange: UploadProps['onChange'] = async (file, uploadFiles) => {
  if (!file.raw) {
    return
  }
  if (!isEditableOutputFile(file.name)) {
    actionDialog.fieldErrors.outputUploadTicket = '输出文件必须为可编辑源文件'
    externalOutputUploadRef.value?.clearFiles()
    externalOutputFileList.value = []
    externalReviewAction.outputFile = undefined
    return
  }
  clearActionDialogFieldError('outputUploadTicket')
  externalOutputFileList.value = uploadFiles.slice(-1)
  externalReviewAction.outputFile = undefined
  externalReviewAction.outputLoading = true
  try {
    externalReviewAction.outputFile = await uploadControlledFilePreview(
      file.raw as File,
      'EXTERNAL_REVIEW_OUTPUT',
      buildDetailUploadPreviewContext(externalOutputUploadSessionId.value)
    )
  } catch (error) {
    externalOutputFileList.value = []
    externalReviewAction.outputFile = undefined
    actionDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '输出文件上传失败，请查看错误提示后重试。'
    )
  } finally {
    externalReviewAction.outputLoading = false
  }
}

const handleBeforeExternalOutputFileRemove: UploadProps['beforeRemove'] = async () => {
  return await cleanupDetailUploadSession(
    externalOutputUploadSessionId.value,
    Boolean(externalReviewAction.outputFile?.uploadTicket),
    externalReviewAction.outputFile?.requestId,
    (errorMessage) => {
      actionDialog.inlineError = errorMessage
    }
  )
}

const handleExternalOutputFileRemove: UploadProps['onRemove'] = () => {
  externalOutputFileList.value = []
  externalReviewAction.outputFile = undefined
  clearActionDialogFieldError('outputUploadTicket')
}

const validateFourthNodeApprovalFiles = () => {
  if (!shouldCollectFourthNodeFiles.value) {
    return true
  }
  const fieldErrors: Record<string, string> = {}
  if (!fourthNodeUpload.confirmedDirectoryId) {
    fieldErrors.confirmedDirectoryId = '请选择存入路径'
  }
  if (!fourthNodeUpload.stampedPdf?.uploadTicket) {
    fieldErrors.stampedPdfUploadTicket = '请上传盖章 PDF'
  }
  if (!fourthNodeUpload.selectedDistributionScopes.length) {
    fieldErrors.selectedDistributionScopes = '请选择文件下发范围'
  } else if (
    fourthNodeUpload.selectedDistributionScopes.some(
      (scope) => !scope.distributionMedium || !['PUBLIC_FOLDER', 'PAPER'].includes(scope.distributionMedium)
    )
  ) {
    fieldErrors.selectedDistributionScopes = '请选择下发介质'
  }
  if (Object.keys(fieldErrors).length === 0) {
    return true
  }
  actionDialog.fieldErrors = {
    ...actionDialog.fieldErrors,
    ...fieldErrors
  }
  actionDialog.inlineError = Object.values(fieldErrors)[0]
  return false
}

const resetApplicantTrainingRecordDialog = () => {
  applicantTrainingRecordUploadSessionId.value = createControlledFileUploadSessionId()
  applicantTrainingRecordFileList.value = []
  applicantTrainingRecordDialog.file = undefined
  applicantTrainingRecordDialog.uploading = false
  applicantTrainingRecordDialog.inlineError = ''
  applicantTrainingRecordDialog.fieldErrors = {}
  applicantTrainingRecordUploadRef.value?.clearFiles()
}

const openApplicantTrainingRecordDialog = () => {
  if (!canUploadApplicantTrainingRecord.value) {
    message.warning('当前状态不可上传培训记录')
    return
  }
  applicantTrainingRecordDialog.visible = true
  applicantTrainingRecordDialog.submitting = false
  resetApplicantTrainingRecordDialog()
}

const closeApplicantTrainingRecordDialog = async (submitted: boolean | MouseEvent = false) => {
  if (submitted !== true && !(await cleanupApplicantTrainingRecordUploadSession())) {
    return
  }
  applicantTrainingRecordDialog.visible = false
  applicantTrainingRecordDialog.submitting = false
  resetApplicantTrainingRecordDialog()
}

const submitApplicantTrainingRecordDialog = async () => {
  if (!applicantTrainingRecordDialog.file?.uploadTicket) {
    applicantTrainingRecordDialog.fieldErrors = { trainingRecordUploadTicket: '请上传培训记录' }
    applicantTrainingRecordDialog.inlineError = '请上传培训记录'
    return
  }
  applicantTrainingRecordDialog.submitting = true
  applicantTrainingRecordDialog.inlineError = ''
  applicantTrainingRecordDialog.fieldErrors = {}
  try {
    await uploadControlledFileTrainingRecord(controlledFileId.value, {
      sessionId: applicantTrainingRecordDialog.file.sessionId,
      trainingRecordUploadTicket: applicantTrainingRecordDialog.file.uploadTicket
    })
    message.success('培训记录已上传，流程已进入文控批准')
    await closeApplicantTrainingRecordDialog(true)
    await reloadAll()
  } catch (error) {
    applicantTrainingRecordDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '培训记录上传失败，请查看错误提示后重试。'
    )
  } finally {
    applicantTrainingRecordDialog.submitting = false
  }
}

const handleWithdraw = async () => {
  try {
    await message.confirm('确认撤回当前受控文件申请吗？')
    withdrawLoading.value = true
    await withdrawControlledFile(controlledFileId.value, { reason: '提交人主动撤回' })
    message.success('撤回成功')
    await reloadAll()
  } catch (error) {
    if (!isDialogCancelled(error)) {
      message.error(resolveReadSideErrorMessage(error, '撤回失败，请查看错误提示后重试。'))
    }
  } finally {
    withdrawLoading.value = false
  }
}

const cancelActiveObsoleteAction = async () => {
  const activeAction = activeObsoleteAction.value
  if (!activeAction?.bpmProcessInstanceId) {
    message.warning('当前作废申请缺少 BPM 流程，无法撤回')
    return
  }
  try {
    await message.confirm('确认撤回当前作废申请吗？撤回后文件保持现行状态。')
    obsoleteCancelLoading.value = true
    await ProcessInstanceApi.cancelProcessInstanceByStartUser(
      activeAction.bpmProcessInstanceId,
      '提交人主动撤回作废申请'
    )
    message.success('作废申请已撤回')
    activeObsoleteAction.value = null
    await reloadAll()
  } catch (error) {
    if (!isDialogCancelled(error)) {
      message.error(resolveReadSideErrorMessage(error, '作废申请撤回失败，请查看错误提示后重试。'))
    }
  } finally {
    obsoleteCancelLoading.value = false
  }
}

const cancelActivePublishAction = async () => {
  const activeAction = activePublishAction.value
  if (!activeAction?.bpmProcessInstanceId) {
    message.warning('当前发布申请缺少 BPM 流程，无法撤回')
    return
  }
  try {
    await message.confirm('确认撤回当前发布申请吗？撤回后候选版本保持待发布。')
    publishCancelLoading.value = true
    await ProcessInstanceApi.cancelProcessInstanceByStartUser(
      activeAction.bpmProcessInstanceId,
      '提交人主动撤回发布申请'
    )
    message.success('发布申请已撤回')
    activePublishAction.value = null
    await reloadAll()
  } catch (error) {
    if (!isDialogCancelled(error)) {
      message.error(resolveReadSideErrorMessage(error, '发布申请撤回失败，请查看错误提示后重试。'))
    }
  } finally {
    publishCancelLoading.value = false
  }
}

const validateExternalReviewConclusion = () => {
  if (!shouldCollectExternalReviewConclusion.value) {
    return true
  }
  const fieldErrors: Record<string, string> = {}
  if (!externalReviewAction.reviewConclusion) {
    fieldErrors.reviewConclusion = '请选择评审结论'
  }
  if (!externalReviewAction.outputFile?.uploadTicket) {
    fieldErrors.outputUploadTicket = '请上传输出文件'
  }
  if (Object.keys(fieldErrors).length === 0) {
    return true
  }
  actionDialog.fieldErrors = {
    ...actionDialog.fieldErrors,
    ...fieldErrors
  }
  actionDialog.inlineError = Object.values(fieldErrors)[0]
  return false
}

const handleDeleteWithdrawnFlow = async () => {
  try {
    await message.confirm('确认删除流程吗？删除后该撤回记录将不再出现在业务列表，BPM 历史仍保留。')
    deleteWithdrawnLoading.value = true
    await deleteWithdrawnControlledFile(controlledFileId.value)
    message.success('删除流程成功')
    await router.push({ name: 'DccControlledFileBrowser' })
  } catch (error) {
    if (!isDialogCancelled(error)) {
      message.error(resolveReadSideErrorMessage(error, '删除流程失败，请查看错误提示后重试。'))
    }
  } finally {
    deleteWithdrawnLoading.value = false
  }
}

const handleResubmitWithdrawnFlow = async () => {
  try {
    await message.confirm('确认重新提交该撤回流程吗？系统将创建新的 BPM 流程实例。')
    resubmitWithdrawnLoading.value = true
    const newFileId = await resubmitWithdrawnControlledFile(controlledFileId.value)
    message.success('重新提交成功')
    await router.push(buildControlledFileViewerPath(newFileId, 'resubmit-withdrawn-flow', route.fullPath))
  } catch (error) {
    if (!isDialogCancelled(error)) {
      message.error(resolveReadSideErrorMessage(error, '重新提交失败，请查看错误提示后重试。'))
    }
  } finally {
    resubmitWithdrawnLoading.value = false
  }
}

const handleRetryStamp = async () => {
  try {
    await message.confirm('确认重试当前受控文件的发布处理吗？')
    retryStampLoading.value = true
    await retryControlledFileStamp(controlledFileId.value)
    message.success('已重新发起发布处理')
    await reloadAll()
  } catch (error) {
    if (!isDialogCancelled(error)) {
      message.error(resolveReadSideErrorMessage(error, '重试发布失败，请查看错误提示后重试。'))
    }
  } finally {
    retryStampLoading.value = false
  }
}

const loadObsoleteStartUserSelectTasks = async () => {
  obsoleteDialog.startUserSelectTasks = []
  obsoleteDialog.startUserSelectAssignees = {}
  const context = buildObsoleteBusinessActionContext(fileDetail.value)
  if (!context) {
    throw new Error('当前文件缺少平台动作上下文，无法解析作废审批人。')
  }
  const resolution = await resolveBusinessAction(context)
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
      controlledFileId: controlledFileId.value,
      actionCode: 'OBSOLETE'
    })
  })
  obsoleteDialog.startUserSelectTasks =
    approvalDetail?.activityNodes?.filter(
      (node: ProcessInstanceApi.ApprovalNodeInfo) =>
        CandidateStrategy.START_USER_SELECT === node.candidateStrategy
    ) || []
  for (const task of obsoleteDialog.startUserSelectTasks) {
    obsoleteDialog.startUserSelectAssignees[task.id] = []
  }
}

const loadPublishStartUserSelectTasks = async () => {
  publishDialog.startUserSelectTasks = []
  publishDialog.startUserSelectAssignees = {}
  const context = buildPublishBusinessActionContext(fileDetail.value)
  if (!context) {
    throw new Error('当前候选版本缺少平台动作上下文，无法解析发布审批人。')
  }
  const resolution = await resolveBusinessAction(context)
  if (!resolution.requiresBpm || !resolution.bpmProcessKey) {
    return
  }
  const processDefinition = await DefinitionApi.getProcessDefinition(undefined, resolution.bpmProcessKey)
  if (!processDefinition?.id) {
    throw new Error('发布审批流程未配置，请联系管理员。')
  }
  const approvalDetail = await ProcessInstanceApi.getApprovalDetail({
    processDefinitionId: processDefinition.id,
    activityId: NodeId.START_USER_NODE_ID,
    processVariablesStr: JSON.stringify({
      controlledFileId: controlledFileId.value,
      actionCode: 'PUBLISH'
    })
  })
  publishDialog.startUserSelectTasks =
    approvalDetail?.activityNodes?.filter(
      (node: ProcessInstanceApi.ApprovalNodeInfo) =>
        CandidateStrategy.START_USER_SELECT === node.candidateStrategy
    ) || []
  for (const task of publishDialog.startUserSelectTasks) {
    publishDialog.startUserSelectAssignees[task.id] = []
  }
}

const openObsoleteDialog = async () => {
  if (!canSubmitObsoleteAction.value) {
    message.warning(obsoleteActionLockDescription.value || '当前文件暂不能发起作废申请')
    return
  }
  obsoleteDialog.visible = true
  obsoleteDialog.submitting = false
  obsoleteDialog.inlineError = ''
  obsoleteDialog.reason = ''
  obsoleteDialog.idempotencyKey = `DCC-OBSOLETE-${controlledFileId.value}-${generateUUID()}`
  obsoleteDialog.startUserSelectTasks = []
  obsoleteDialog.startUserSelectAssignees = {}
  try {
    await loadObsoleteStartUserSelectTasks()
  } catch (error) {
    obsoleteDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '作废审批人加载失败，请查看错误提示后重试。'
    )
  }
}

const closeObsoleteDialog = () => {
  obsoleteDialog.visible = false
  obsoleteDialog.submitting = false
  obsoleteDialog.inlineError = ''
  obsoleteDialog.reason = ''
  obsoleteDialog.idempotencyKey = ''
  obsoleteDialog.startUserSelectTasks = []
  obsoleteDialog.startUserSelectAssignees = {}
}

const openPublishDialog = async () => {
  if (!canSubmitPublishAction.value) {
    message.warning(publishActionLockDescription.value || '当前候选版本暂不能提交发布申请')
    return
  }
  publishDialog.visible = true
  publishDialog.submitting = false
  publishDialog.inlineError = ''
  publishDialog.reason = ''
  publishDialog.idempotencyKey = `DCC-PUBLISH-${controlledFileId.value}-${generateUUID()}`
  publishDialog.startUserSelectTasks = []
  publishDialog.startUserSelectAssignees = {}
  try {
    await loadPublishStartUserSelectTasks()
  } catch (error) {
    publishDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '发布审批人加载失败，请查看错误提示后重试。'
    )
  }
}

const closePublishDialog = () => {
  publishDialog.visible = false
  publishDialog.submitting = false
  publishDialog.inlineError = ''
  publishDialog.reason = ''
  publishDialog.idempotencyKey = ''
  publishDialog.startUserSelectTasks = []
  publishDialog.startUserSelectAssignees = {}
}

const submitPublishDialog = async () => {
  const reason = publishDialog.reason.trim()
  if (!reason) {
    publishDialog.inlineError = '请输入发布说明'
    return
  }
  if (!canSubmitPublishAction.value) {
    publishDialog.inlineError = publishActionLockDescription.value || '当前候选版本暂不能提交发布申请'
    return
  }
  for (const task of publishDialog.startUserSelectTasks) {
    const assignees = publishDialog.startUserSelectAssignees[task.id]
    if (!Array.isArray(assignees) || assignees.length === 0) {
      publishDialog.inlineError = `请选择${task.name}审批人`
      return
    }
  }
  publishDialog.submitting = true
  publishDialog.inlineError = ''
  try {
    if (!publishDialog.idempotencyKey) {
      publishDialog.idempotencyKey = `DCC-PUBLISH-${controlledFileId.value}-${generateUUID()}`
    }
    const instance = await publishControlledFile(controlledFileId.value, {
      reason,
      idempotencyKey: publishDialog.idempotencyKey,
      startUserSelectAssignees: publishDialog.startUserSelectAssignees
    })
    activePublishAction.value = instance
    message.success('发布申请已提交，等待审批通过后生效')
    closePublishDialog()
    await reloadAll()
  } catch (error) {
    publishDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '发布申请提交失败，请查看错误提示后重试。'
    )
  } finally {
    publishDialog.submitting = false
  }
}

const handleAcknowledgeTraining = async () => {
  try {
    await message.confirm('确认完成当前版本培训并提交确认吗？')
    trainingAckLoading.value = true
    await acknowledgeControlledFileTraining(controlledFileId.value)
    message.success('培训确认已提交')
    await reloadAll()
  } catch (error) {
    if (!isDialogCancelled(error)) {
      message.error(resolveReadSideErrorMessage(error, '培训确认失败，请查看错误提示后重试。'))
    }
  } finally {
    trainingAckLoading.value = false
  }
}

const handleManualRelease = async () => {
  try {
    await message.confirm('确认完成培训放行并正式下发当前版本吗？')
    manualReleaseLoading.value = true
    await manualReleaseControlledFile(controlledFileId.value)
    message.success('当前版本已正式下发')
    await reloadAll()
  } catch (error) {
    if (!isDialogCancelled(error)) {
      message.error(resolveReadSideErrorMessage(error, '正式下发失败，请查看错误提示后重试。'))
    }
  } finally {
    manualReleaseLoading.value = false
  }
}

const resetPaperDistributionIssueDialog = () => {
  paperDistributionIssueDialog.submitting = false
  paperDistributionIssueDialog.inlineError = ''
  paperDistributionIssueDialog.fieldErrors = {}
  paperDistributionIssueDialog.distributionId = undefined
  paperDistributionIssueDialog.form.recipientUserIds = []
}

const handleAcknowledgePaperDistribution = (distributionId: number) => {
  resetPaperDistributionIssueDialog()
  paperDistributionIssueDialog.distributionId = distributionId
  paperDistributionIssueDialog.visible = true
}

const closePaperDistributionIssueDialog = () => {
  paperDistributionIssueDialog.visible = false
  resetPaperDistributionIssueDialog()
}

const validatePaperDistributionIssueDialog = () => {
  const errors: Record<string, string> = {}
  if (!paperDistributionIssueDialog.form.recipientUserIds.length) {
    errors.recipientUserIds = '请选择纸质接收人'
  }
  paperDistributionIssueDialog.fieldErrors = errors
  paperDistributionIssueDialog.inlineError = Object.values(errors)[0] || ''
  return Object.keys(errors).length === 0
}

const submitPaperDistributionIssueDialog = async () => {
  if (
    !paperDistributionIssueDialog.distributionId ||
    !validatePaperDistributionIssueDialog()
  ) {
    return
  }
  const distributionId = paperDistributionIssueDialog.distributionId
  paperDistributionIssueDialog.submitting = true
  paperDistributionIssueDialog.inlineError = ''
  paperDistributionAckLoadingId.value = distributionId
  try {
    await acknowledgePaperDistribution(controlledFileId.value, distributionId, {
      recipientUserIds: paperDistributionIssueDialog.form.recipientUserIds
    })
    message.success('纸质发放已登记')
    closePaperDistributionIssueDialog()
    await reloadAll()
  } catch (error) {
    paperDistributionIssueDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '纸质发放登记失败，请查看错误提示后重试。'
    )
  } finally {
    paperDistributionIssueDialog.submitting = false
    if (paperDistributionAckLoadingId.value === distributionId) {
      paperDistributionAckLoadingId.value = undefined
    }
  }
}

const handleRecoverPaperDistribution = async (distributionId: number) => {
  try {
    await message.confirm('确认当前纸质文件已回收并更新状态吗？')
    paperDistributionRecoverLoadingId.value = distributionId
    await recoverPaperDistribution(controlledFileId.value, distributionId)
    message.success('纸质文件回收状态已更新')
    await reloadAll()
  } catch (error) {
    if (!isDialogCancelled(error)) {
      message.error(resolveReadSideErrorMessage(error, '纸质文件回收确认失败，请查看错误提示后重试。'))
    }
  } finally {
    if (paperDistributionRecoverLoadingId.value === distributionId) {
      paperDistributionRecoverLoadingId.value = undefined
    }
  }
}

const getDistributionStatusTagType = (status: string | undefined) => {
  if (status === 'ACKNOWLEDGED' || status === 'RECOVERED') {
    return 'success'
  }
  if (status === 'READ') {
    return 'primary'
  }
  if (status === 'SENT') {
    return 'warning'
  }
  return 'info'
}

const getPaperDistributionRecipientNames = (distribution: ControlledFileDistributionStatusVO) => {
  const userIds = (distribution.recipients || []).length
    ? (distribution.recipients || []).map((recipient) => recipient.userId)
    : distribution.recipientUserIds || []
  if (!userIds.length) {
    return '-'
  }
  return userIds.map((userId) => userNameMap.value.get(userId) || `用户#${userId}`).join('、')
}

const getDistributionRecipientDisplay = (distribution: ControlledFileDistributionStatusVO) => {
  if (distribution.distributionMedium === 'PAPER') {
    return getPaperDistributionRecipientNames(distribution)
  }
  return getDistributionRecipientSummary(distribution, userNameMap.value)
}

const getCurrentElectronicReceiptRecipient = (
  distribution: ControlledFileDistributionStatusVO
): ControlledFileDistributionRecipientStatusVO | undefined => {
  if (distribution.distributionMedium !== 'PUBLIC_FOLDER') {
    return undefined
  }
  return getCurrentDistributionRecipient(distribution)?.acknowledgedAt
    ? undefined
    : getCurrentDistributionRecipient(distribution)
}

const getCurrentDistributionRecipient = (
  distribution: ControlledFileDistributionStatusVO
): ControlledFileDistributionRecipientStatusVO | undefined => {
  if (distribution.distributionMedium !== 'PUBLIC_FOLDER') {
    return undefined
  }
  return (distribution.recipients || []).find((recipient) => recipient.userId === currentUserId.value)
}

const hasDistributionRowAction = (distribution: ControlledFileDistributionStatusVO) => {
  return (
    Boolean(getCurrentElectronicReceiptRecipient(distribution)) ||
    Boolean(getCurrentDistributionRecipient(distribution)) ||
    (distribution.distributionMedium === 'PAPER' &&
      distribution.status !== 'ACKNOWLEDGED' &&
      distribution.status !== 'RECOVERED') ||
    (distribution.distributionMedium === 'PAPER' && distribution.status === 'ACKNOWLEDGED')
  )
}

const getDistributionRecoverUserSummary = (distribution: ControlledFileDistributionStatusVO) => {
  if (!distribution.recoveredBy) {
    return '-'
  }
  return userNameMap.value.get(distribution.recoveredBy) || `用户#${distribution.recoveredBy}`
}

const distributionStatusRows = computed(() =>
  (fileDetail.value?.distributionStatuses || []).map((distribution) => {
    const departmentName = deptNameMap.value.get(distribution.departmentId) || `部门#${distribution.departmentId}`
    const recipientText = getDistributionRecipientDisplay(distribution)
    const distributionSummaryText = [
      getDistributionStatusLabel(distribution.status),
      getDistributionMediumLabel(distribution.distributionMedium),
      getDistributionAckUserSummary(distribution, userNameMap.value),
      formatControlledFileDateTime(distribution.acknowledgedAt)
    ].join(' ')
    const recoverySummaryText = [
      getDistributionRecoverUserSummary(distribution),
      formatControlledFileDateTime(distribution.recoveredAt)
    ].join(' ')
    return {
      ...distribution,
      departmentName,
      recipientText,
      distributionSummaryText,
      recoverySummaryText
    }
  })
)
const pagedDistributionStatusRows = computed(() =>
  getPagedDetailRows(
    distributionStatusRows.value,
    distributionStatusListState.pageNo,
    distributionStatusListState.pageSize
  )
)

const resetElectronicReceiptDialog = () => {
  electronicReceiptDialog.submitting = false
  electronicReceiptDialog.inlineError = ''
  electronicReceiptDialog.fieldErrors = {}
  electronicReceiptDialog.distributionId = undefined
  electronicReceiptDialog.recipientId = undefined
  electronicReceiptDialog.form.password = ''
  electronicReceiptDialog.form.comment = ''
}

const openElectronicReceiptDialog = (distribution: ControlledFileDistributionStatusVO) => {
  const recipient = getCurrentElectronicReceiptRecipient(distribution)
  if (!recipient?.id) {
    message.warning('当前用户没有可签收的电子发放记录')
    return
  }
  resetElectronicReceiptDialog()
  electronicReceiptDialog.distributionId = distribution.id
  electronicReceiptDialog.recipientId = recipient.id
  electronicReceiptDialog.visible = true
}

const closeElectronicReceiptDialog = () => {
  electronicReceiptDialog.visible = false
  resetElectronicReceiptDialog()
}

const validateElectronicReceiptDialog = () => {
  const errors: Record<string, string> = {}
  if (!electronicReceiptDialog.form.password.trim()) {
    errors.password = '请输入登录密码完成电子签名'
  }
  electronicReceiptDialog.fieldErrors = errors
  electronicReceiptDialog.inlineError = Object.values(errors)[0] || ''
  return Object.keys(errors).length === 0
}

const submitElectronicReceiptDialog = async () => {
  if (
    !electronicReceiptDialog.distributionId ||
    !electronicReceiptDialog.recipientId ||
    !validateElectronicReceiptDialog()
  ) {
    return
  }
  electronicReceiptDialog.submitting = true
  electronicReceiptDialog.inlineError = ''
  electronicReceiptLoadingRecipientId.value = electronicReceiptDialog.recipientId
  try {
    await acknowledgeElectronicDistribution(
      controlledFileId.value,
      electronicReceiptDialog.distributionId,
      electronicReceiptDialog.recipientId,
      {
        password: electronicReceiptDialog.form.password,
        comment: electronicReceiptDialog.form.comment.trim()
      }
    )
    message.success('电子发放签收已提交')
    closeElectronicReceiptDialog()
    await reloadAll()
  } catch (error) {
    if (isControlledFileTaskPasswordInvalidError(error)) {
      electronicReceiptDialog.fieldErrors = {
        password: DCC_APPROVAL_WRONG_PASSWORD_MESSAGE
      }
    }
    electronicReceiptDialog.inlineError = resolveDccApprovalSignatureErrorMessage(
      error,
      '电子发放签收失败，请查看错误提示后重试。'
    )
  } finally {
    electronicReceiptDialog.submitting = false
    electronicReceiptLoadingRecipientId.value = undefined
  }
}

const resetDistributionSignDialog = () => {
  distributionSignDialog.submitting = false
  distributionSignDialog.inlineError = ''
  distributionSignDialog.fieldErrors = {}
  distributionSignDialog.distributionId = undefined
  distributionSignDialog.recipientId = undefined
  distributionSignDialog.form.userIds = []
  distributionSignDialog.form.password = ''
  distributionSignDialog.form.comment = ''
}

const openDistributionSignDialog = (distribution: ControlledFileDistributionStatusVO) => {
  const recipient = getCurrentDistributionRecipient(distribution)
  if (!recipient?.id) {
    message.warning('当前用户没有可加签的电子发放记录')
    return
  }
  resetDistributionSignDialog()
  distributionSignDialog.distributionId = distribution.id
  distributionSignDialog.recipientId = recipient.id
  distributionSignDialog.visible = true
}

const closeDistributionSignDialog = () => {
  distributionSignDialog.visible = false
  resetDistributionSignDialog()
}

const validateDistributionSignDialog = () => {
  const errors: Record<string, string> = {}
  if (!distributionSignDialog.form.userIds.length) {
    errors.userIds = '请选择加签接收人'
  }
  if (!distributionSignDialog.form.password.trim()) {
    errors.password = '请输入登录密码完成电子签名'
  }
  distributionSignDialog.fieldErrors = errors
  distributionSignDialog.inlineError = Object.values(errors)[0] || ''
  return Object.keys(errors).length === 0
}

const submitDistributionSignDialog = async () => {
  if (
    !distributionSignDialog.distributionId ||
    !distributionSignDialog.recipientId ||
    !validateDistributionSignDialog()
  ) {
    return
  }
  distributionSignDialog.submitting = true
  distributionSignDialog.inlineError = ''
  distributionSignLoadingRecipientId.value = distributionSignDialog.recipientId
  try {
    await createDistributionRecipientSignTask(
      controlledFileId.value,
      distributionSignDialog.distributionId,
      distributionSignDialog.recipientId,
      {
        userIds: distributionSignDialog.form.userIds,
        password: distributionSignDialog.form.password,
        comment: distributionSignDialog.form.comment.trim()
      }
    )
    message.success('接收人加签已提交')
    closeDistributionSignDialog()
    await reloadAll()
  } catch (error) {
    if (isControlledFileTaskPasswordInvalidError(error)) {
      distributionSignDialog.fieldErrors = {
        password: DCC_APPROVAL_WRONG_PASSWORD_MESSAGE
      }
    }
    distributionSignDialog.inlineError = resolveDccApprovalSignatureErrorMessage(
      error,
      '接收人加签失败，请查看错误提示后重试。'
    )
  } finally {
    distributionSignDialog.submitting = false
    distributionSignLoadingRecipientId.value = undefined
  }
}

const buildDistributionReceiptRows = () => {
  return distributionReceiptRows.value.map((record) => ({
    fileNumber: record.fileNumber || '-',
    versionNo: record.versionNo || '-',
    fileName: record.fileName || '-',
    issuer: record.issuerName || '-',
    recipients: record.recipientNames?.length ? record.recipientNames.join('、') : '-',
    issuedAt: formatControlledFileDateTime(record.issuedAt),
    recoveredBy: record.recovererName || '-',
    recoveredAt: formatControlledFileDateTime(record.recoveredAt)
  }))
}

const escapeCsvCell = (value: unknown) => {
  const text = String(value ?? '')
  return `"${text.replace(/"/g, '""')}"`
}

const handleExportDistributionReceipts = () => {
  const rows = buildDistributionReceiptRows()
  if (!rows.length) {
    message.warning('当前版本暂无可导出的发放回执')
    return
  }
  const headers = [
    '文件编号',
    '版本',
    '名称',
    '发放人',
    '接收人',
    '发放日期',
    '回收人',
    '回收日期'
  ]
  const csvRows = [
    headers,
    ...rows.map((row) => [
      row.fileNumber,
      row.versionNo,
      row.fileName,
      row.issuer,
      row.recipients,
      row.issuedAt,
      row.recoveredBy,
      row.recoveredAt
    ])
  ]
  const csv = csvRows.map((row) => row.map(escapeCsvCell).join(',')).join('\r\n')
  downloadByData(
    csv,
    `DCC发放回执-${fileDetail.value?.fileNumber || controlledFileId.value}.csv`,
    'text/csv;charset=utf-8',
    '\ufeff'
  )
}

const escapeReceiptHtml = (value: unknown) =>
  String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

const buildDistributionReceiptPrintHtml = () => {
  const rows = buildDistributionReceiptRows()
  const bodyRows = rows
    .map(
      (row) => `<tr>
        <td>${escapeReceiptHtml(row.fileNumber)}</td>
        <td>${escapeReceiptHtml(row.versionNo)}</td>
        <td>${escapeReceiptHtml(row.fileName)}</td>
        <td>${escapeReceiptHtml(row.issuer)}</td>
        <td>${escapeReceiptHtml(row.recipients)}</td>
        <td>${escapeReceiptHtml(row.issuedAt)}</td>
        <td>${escapeReceiptHtml(row.recoveredBy)}</td>
        <td>${escapeReceiptHtml(row.recoveredAt)}</td>
      </tr>`
    )
    .join('')
  return `<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>DCC发放回执</title>
  <style>
    body { font-family: Arial, "Microsoft YaHei", sans-serif; color: #1f2329; margin: 24px; }
    h1 { font-size: 20px; margin: 0 0 16px; }
    table { border-collapse: collapse; width: 100%; font-size: 12px; }
    th, td { border: 1px solid #333; padding: 6px 8px; text-align: left; vertical-align: top; }
    th { background: #f3f4f6; }
  </style>
</head>
<body>
  <h1>DCC发放回执</h1>
  <table>
    <thead>
      <tr>
        <th>文件编号</th>
        <th>版本</th>
        <th>名称</th>
        <th>发放人</th>
        <th>接收人</th>
        <th>发放日期</th>
        <th>回收人</th>
        <th>回收日期</th>
      </tr>
    </thead>
    <tbody>${bodyRows}</tbody>
  </table>
</body>
</html>`
}

const handlePrintDistributionReceipts = () => {
  if (!distributionReceiptRows.value.length) {
    message.warning('当前版本暂无可打印的发放回执')
    return
  }
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    message.error('打印窗口打开失败，请检查浏览器弹窗拦截设置。')
    return
  }
  printWindow.document.open()
  printWindow.document.write(buildDistributionReceiptPrintHtml())
  printWindow.document.close()
  printWindow.focus()
  printWindow.print()
}

const resetControlledPrintDialog = () => {
  controlledPrintDialog.inlineError = ''
  controlledPrintDialog.fieldErrors = {}
  controlledPrintDialog.form.purpose = ''
  controlledPrintDialog.form.copies = 1
  controlledPrintDialog.form.receivingDepartment = ''
  controlledPrintDialog.form.useLocation = ''
}

const openControlledPrintDialog = () => {
  if (!controlledPrintAllowed.value) {
    message.error('当前用户没有受控打印权限，或该文件不是当前有效受控版本。')
    return
  }
  if (!controlledPrintDialog.form.receivingDepartment) {
    const currentDeptId = Number(userStore.getUser.deptId || 0)
    controlledPrintDialog.form.receivingDepartment = deptNameMap.value.get(currentDeptId) || ''
  }
  controlledPrintDialog.visible = true
  controlledPrintDialog.inlineError = ''
  controlledPrintDialog.fieldErrors = {}
}

const closeControlledPrintDialog = () => {
  controlledPrintDialog.visible = false
  resetControlledPrintDialog()
}

const validateControlledPrintForm = (): ControlledFilePrintCreateReqVO | null => {
  const form = controlledPrintDialog.form
  const errors: Record<string, string> = {}
  const purpose = form.purpose.trim()
  const copies = Number(form.copies)
  const receivingDepartment = form.receivingDepartment.trim()
  const useLocation = form.useLocation.trim()
  if (!purpose) {
    errors.purpose = '请输入打印用途'
  }
  if (!Number.isFinite(copies) || copies <= 0) {
    errors.copies = '请输入大于 0 的打印份数'
  }
  if (!receivingDepartment) {
    errors.receivingDepartment = '请输入接收部门'
  }
  if (!useLocation) {
    errors.useLocation = '请输入使用位置'
  }
  controlledPrintDialog.fieldErrors = errors
  controlledPrintDialog.inlineError = Object.values(errors)[0] || ''
  if (controlledPrintDialog.inlineError) {
    return null
  }
  return { purpose, copies, receivingDepartment, useLocation }
}

const enhanceControlledPrintHtml = (html: string, record: ControlledFilePrintRecordVO) => {
  if (html.includes('副本编号') && html.includes('直接受控打印')) {
    return html
  }
  const copyNumberText = escapeReceiptHtml(formatControlledPrintCopyNumberList(record))
  const policyText = '直接受控打印（当前文件类别无需打印审批）'
  const insertRows = `
                      <tr><th>副本编号</th><td>${copyNumberText}</td></tr>
                      <tr><th>审批策略</th><td>${escapeReceiptHtml(policyText)}</td></tr>`
  if (html.includes('<tr><th>打印用途</th>')) {
    return html.replace('<tr><th>打印用途</th>', `${insertRows}
                      <tr><th>打印用途</th>`)
  }
  return html.replace('</tbody>', `${insertRows}
                    </tbody>`)
}

const submitControlledPrint = async () => {
  const data = validateControlledPrintForm()
  if (!data) {
    return
  }
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    message.error('受控打印窗口打开失败，请检查浏览器弹窗拦截设置。')
    return
  }
  controlledPrintDialog.submitting = true
  controlledPrintDialog.inlineError = ''
  try {
    const record = await createControlledFilePrintRecord(controlledFileId.value, data)
    const printHtml = await getControlledFilePrintHtml(controlledFileId.value, record.id)
    printWindow.document.open()
    printWindow.document.write(enhanceControlledPrintHtml(printHtml.html, record))
    printWindow.document.close()
    printWindow.focus()
    printWindow.print()
    controlledPrintDialog.visible = false
    resetControlledPrintDialog()
    await loadControlledPrintRecords()
    latestControlledPrintRecordId.value = record.id
    controlledPrintResultDialog.record = record
    controlledPrintResultDialog.visible = true
    await focusControlledPrintRecord(record.id, true)
    message.success('受控打印记录已生成：' + record.printNo)
  } catch (error) {
    printWindow.close()
    controlledPrintDialog.inlineError = resolveReadSideErrorMessage(
      error,
      '受控打印失败，请查看后端错误后重试。'
    )
    message.error(controlledPrintDialog.inlineError)
  } finally {
    controlledPrintDialog.submitting = false
  }
}

const openControlledPrintDialogFromRoute = async () => {
  if (String(route.query.controlledPrint || '') !== '1') {
    return
  }
  const autoOpenKey = controlledFileId.value + ':' + route.fullPath
  if (controlledPrintAutoOpenKey.value === autoOpenKey) {
    return
  }
  controlledPrintAutoOpenKey.value = autoOpenKey
  openControlledPrintDialog()
}

const getProcessInstanceId = () => String(fileDetail.value?.processInstanceId || '')

const buildProcessPrintHtml = (printData: unknown) => {
  const file = fileDetail.value
  const processJson = escapeReceiptHtml(JSON.stringify(printData ?? {}, null, 2))
  return `<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>DCC流程打印</title>
  <style>
    body { font-family: Arial, "Microsoft YaHei", sans-serif; color: #1f2329; margin: 24px; }
    h1 { font-size: 20px; margin: 0 0 16px; }
    table { border-collapse: collapse; width: 100%; font-size: 13px; margin-bottom: 18px; }
    th, td { border: 1px solid #333; padding: 7px 9px; text-align: left; vertical-align: top; }
    th { width: 130px; background: #f3f4f6; }
    pre { white-space: pre-wrap; word-break: break-word; border: 1px solid #d8dde8; padding: 12px; }
  </style>
</head>
<body>
  <h1>DCC流程打印</h1>
  <table>
    <tbody>
      <tr><th>文件编号</th><td>${escapeReceiptHtml(file?.fileNumber || '-')}</td></tr>
      <tr><th>文件名称</th><td>${escapeReceiptHtml(file?.fileName || file?.title || '-')}</td></tr>
      <tr><th>版本</th><td>${escapeReceiptHtml(file?.versionNo || '-')}</td></tr>
      <tr><th>产品编号</th><td>${escapeReceiptHtml(file?.productCode || '-')}</td></tr>
      <tr><th>流程实例</th><td>${escapeReceiptHtml(file?.processInstanceId || '-')}</td></tr>
    </tbody>
  </table>
  <pre>${processJson}</pre>
</body>
</html>`
}

const loadProcessPrintData = async () => {
  const processInstanceId = getProcessInstanceId()
  if (!processInstanceId) {
    return null
  }
  return await ProcessInstanceApi.getProcessInstancePrintData(processInstanceId)
}

const handlePrintProcess = async () => {
  processPrintLoading.value = true
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    message.error('流程打印窗口打开失败，请检查浏览器弹窗拦截设置。')
    processPrintLoading.value = false
    return
  }
  try {
    const customTemplate = activeApprovalPrintTemplate.value?.active
      ? await getControlledFileApprovalPrintHtml(controlledFileId.value)
      : null
    const printData = customTemplate ? null : await loadProcessPrintData()
    printWindow.document.open()
    printWindow.document.write(customTemplate?.html || buildProcessPrintHtml(printData))
    printWindow.document.close()
    printWindow.focus()
    printWindow.print()
  } catch (error) {
    printWindow.close()
    message.error(resolveReadSideErrorMessage(error, '流程打印失败，请查看错误提示后重试。'))
  } finally {
    processPrintLoading.value = false
  }
}

const handleExportProcessWord = async () => {
  processExportLoading.value = true
  try {
    if (activeApprovalPrintTemplate.value?.active) {
      const word = await exportControlledFileApprovalWord(controlledFileId.value)
      downloadByData(
        word,
        `DCC流程-${fileDetail.value?.fileNumber || controlledFileId.value}.docx`,
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
      )
      return
    }
    const printData = await loadProcessPrintData()
    downloadByData(
      buildProcessPrintHtml(printData),
      `DCC流程-${fileDetail.value?.fileNumber || controlledFileId.value}.doc`,
      'application/msword;charset=utf-8',
      '\ufeff'
    )
  } catch (error) {
    message.error(resolveReadSideErrorMessage(error, '流程导出 Word 失败，请查看错误提示后重试。'))
  } finally {
    processExportLoading.value = false
  }
}

const handleDetailMoreCommand = (command: string) => {
  switch (command) {
    case 'bpm-detail':
      openBpmDetail()
      return
    case 'edit-metadata':
      openMetadataDialog()
      return
    case 'print-process':
      void handlePrintProcess()
      return
    case 'export-process-word':
      void handleExportProcessWord()
      return
    default:
      throw new Error(`未支持的详情更多操作：${command}`)
  }
}

const handleDetailDangerCommand = (command: string) => {
  switch (command) {
    case 'withdraw':
      void handleWithdraw()
      return
    case 'delete-withdrawn-flow':
      void handleDeleteWithdrawnFlow()
      return
    case 'resubmit-withdrawn-flow':
      void handleResubmitWithdrawnFlow()
      return
    case 'retry-stamp':
      void handleRetryStamp()
      return
    case 'obsolete':
      openObsoleteDialog()
      return
    default:
      throw new Error(`未支持的详情风险操作：${command}`)
  }
}

const openActionDialog = (mode: DccApprovalActionMode) => {
  actionDialog.visible = true
  actionDialog.mode = mode
  actionDialog.submitting = false
  actionDialog.inlineError = ''
  actionDialog.fieldErrors = {}
  actionDialog.form.password = ''
  actionDialog.form.reason = ''
  resetFourthNodeUploads()
  if (mode === 'approve' && isFourthNodeApprovalTask.value && !isExternalReviewProcess.value) {
    void loadDocControlDirectoryTree().catch((error) => {
      const errorMessage = resolveReadSideErrorMessage(error, '存入路径加载失败，请处理后再继续。')
      actionDialog.inlineError = errorMessage
      actionDialog.fieldErrors.confirmedDirectoryId = errorMessage
      message.error(errorMessage)
    })
  }
}

const closeActionDialog = async (submitted: boolean | MouseEvent = false) => {
  if (submitted !== true && !(await cleanupFourthNodeUploadSessions())) {
    return
  }
  actionDialog.visible = false
  actionDialog.submitting = false
  actionDialog.inlineError = ''
  actionDialog.fieldErrors = {}
  actionDialog.form.password = ''
  actionDialog.form.reason = ''
  resetFourthNodeUploads()
}

const buildActionSuccessMessage = (
  mode: DccApprovalActionMode,
  response: Awaited<ReturnType<typeof submitDccApprovalAction>>['response']
) => {
  const actionText = mode === 'approve' ? '签名通过并已提交审批' : '已签名驳回任务'
  if (!response) {
    return actionText
  }
  return `${actionText}，版本 ${response.versionNo}，证据 ${response.evidenceHashShort}`
}

const submitActionDialog = async () => {
  if (!approvalTodoTask.value?.id) {
    return
  }
  actionDialog.submitting = true
  actionDialog.inlineError = ''
  actionDialog.fieldErrors = {}
  try {
    if (!validateFourthNodeApprovalFiles() || !validateExternalReviewConclusion()) {
      return
    }
    if (isExternalReviewProcess.value) {
      if (!actionDialog.form.password.trim()) {
        actionDialog.fieldErrors = { password: DCC_APPROVAL_WRONG_PASSWORD_MESSAGE }
        actionDialog.inlineError = '请输入登录密码完成电子签名'
        return
      }
      if (actionDialog.mode === 'approve') {
        await approveExternalFileReviewTask(controlledFileId.value, {
          taskId: approvalTodoTask.value.id,
          password: actionDialog.form.password,
          reason: actionDialog.form.reason?.trim() || undefined,
          reviewConclusion: externalReviewAction.reviewConclusion || undefined,
          conclusionComment: externalReviewAction.conclusionComment?.trim() || undefined,
          sessionId: externalReviewAction.outputFile?.sessionId,
          outputUploadTicket: externalReviewAction.outputFile?.uploadTicket
        })
      } else {
        if (!actionDialog.form.reason.trim()) {
          actionDialog.fieldErrors = { reason: '请输入驳回原因' }
          actionDialog.inlineError = '请输入驳回原因'
          return
        }
        await rejectExternalFileReviewTask(controlledFileId.value, {
          taskId: approvalTodoTask.value.id,
          password: actionDialog.form.password,
          reason: actionDialog.form.reason.trim()
        })
      }
      message.success(actionDialog.mode === 'approve' ? '外来文件评审已签名通过' : '外来文件评审已驳回')
      await closeActionDialog(true)
      await reloadAll()
      return
    }
    const result = await submitDccApprovalAction({
      fileId: controlledFileId.value,
      action: actionDialog.mode,
      form: {
        password: actionDialog.form.password,
        reason: actionDialog.form.reason,
        sessionId: fourthNodeUpload.stampedPdf?.sessionId,
        stampedPdfUploadTicket: fourthNodeUpload.stampedPdf?.uploadTicket,
        confirmedDirectoryId: fourthNodeUpload.confirmedDirectoryId,
        selectedDistributionScopes: fourthNodeUpload.selectedDistributionScopes.map((scope) => ({
          departmentId: scope.departmentId,
          distributionMedium: scope.distributionMedium
        }))
      },
      taskId: String(approvalTodoTask.value.id)
    })
    if (!result.success) {
      if (result.field) {
        actionDialog.fieldErrors = {
          [result.field]: result.inlineError || ''
        }
      }
      actionDialog.inlineError = result.inlineError || ''
      return
    }
    message.success(buildActionSuccessMessage(actionDialog.mode, result.response))
    await closeActionDialog(true)
    await reloadAll()
  } catch (error) {
    if (isControlledFileTaskPasswordInvalidError(error)) {
      actionDialog.fieldErrors = {
        password: DCC_APPROVAL_WRONG_PASSWORD_MESSAGE
      }
    }
    actionDialog.inlineError = resolveDccApprovalSignatureErrorMessage(
      error,
      '签名提交失败，请查看错误提示后重试。'
    )
  } finally {
    actionDialog.submitting = false
  }
}

const resetTaskActionDialogForm = () => {
  taskActionDialog.submitting = false
  taskActionDialog.inlineError = ''
  taskActionDialog.fieldErrors = {}
  taskActionDialog.form.password = ''
  taskActionDialog.form.reason = ''
  taskActionDialog.form.targetTaskDefinitionKey = ''
  taskActionDialog.form.assigneeUserId = undefined
  taskActionDialog.form.userIds = []
  taskActionDialog.form.signType = 'after'
}

const openTaskActionDialog = (mode: DccTaskActionMode) => {
  if (!approvalTodoTask.value?.id) {
    return
  }
  if (mode === 'return' && returnTargetOptions.value.length === 0) {
    message.warning('当前任务没有可回退的上一节点')
    return
  }
  resetTaskActionDialogForm()
  taskActionDialog.mode = mode
  taskActionDialog.form.targetTaskDefinitionKey = returnTargetOptions.value[0]?.value || ''
  taskActionDialog.visible = true
}

const closeTaskActionDialog = () => {
  taskActionDialog.visible = false
  resetTaskActionDialogForm()
}

const validateTaskActionDialog = () => {
  const errors: Record<string, string> = {}
  const password = taskActionDialog.form.password.trim()
  const reason = taskActionDialog.form.reason.trim()
  if (!password) {
    errors.password = '请输入登录密码完成电子签名'
  }
  if (!reason) {
    errors.reason = '请输入处理意见'
  }
  if (taskActionDialog.mode === 'return' && !taskActionDialog.form.targetTaskDefinitionKey) {
    errors.targetTaskDefinitionKey = '请选择回退节点'
  }
  if (taskActionDialog.mode === 'transfer' && !taskActionDialog.form.assigneeUserId) {
    errors.assigneeUserId = '请选择转办人'
  }
  if (
    taskActionDialog.mode === 'sign' &&
    (!Array.isArray(taskActionDialog.form.userIds) || taskActionDialog.form.userIds.length === 0)
  ) {
    errors.userIds = '请选择加签人'
  }
  taskActionDialog.fieldErrors = errors
  taskActionDialog.inlineError = Object.values(errors)[0] || ''
  return Object.keys(errors).length === 0
}

const submitTaskActionDialog = async () => {
  const taskId = String(approvalTodoTask.value?.id || '')
  if (!taskId || !validateTaskActionDialog()) {
    return
  }
  taskActionDialog.submitting = true
  taskActionDialog.inlineError = ''
  try {
    const password = taskActionDialog.form.password
    const reason = taskActionDialog.form.reason.trim()
    if (taskActionDialog.mode === 'return') {
      const submitReturnTask = isExternalReviewProcess.value
        ? returnExternalFileReviewTask
        : returnControlledFileTask
      await submitReturnTask(controlledFileId.value, {
        taskId,
        targetTaskDefinitionKey: taskActionDialog.form.targetTaskDefinitionKey,
        password,
        reason
      })
    } else if (taskActionDialog.mode === 'transfer') {
      const submitTransferTask = isExternalReviewProcess.value
        ? transferExternalFileReviewTask
        : transferControlledFileTask
      await submitTransferTask(controlledFileId.value, {
        taskId,
        assigneeUserId: taskActionDialog.form.assigneeUserId as number,
        password,
        reason
      })
    } else {
      const submitSignTask = isExternalReviewProcess.value
        ? createExternalFileReviewSignTask
        : createControlledFileSignTask
      await submitSignTask(controlledFileId.value, {
        taskId,
        userIds: taskActionDialog.form.userIds,
        type: taskActionDialog.form.signType,
        password,
        reason
      })
    }
    message.success('操作成功')
    closeTaskActionDialog()
    await reloadAll()
  } catch (error) {
    if (isControlledFileTaskPasswordInvalidError(error)) {
      taskActionDialog.fieldErrors = {
        password: DCC_APPROVAL_WRONG_PASSWORD_MESSAGE
      }
    }
    taskActionDialog.inlineError = resolveDccApprovalSignatureErrorMessage(
      error,
      '流程动作提交失败，请查看错误提示后重试。'
    )
  } finally {
    taskActionDialog.submitting = false
  }
}

onBeforeRouteLeave(async () => {
  if (!(await cleanupApplicantTrainingRecordUploadSession())) {
    return false
  }
  if (!(await cleanupFourthNodeUploadSessions())) {
    return false
  }
  return true
})

onBeforeUnmount(() => {
  clearControlledPrintRecordHighlightTimer()
})

onMounted(() => {
  reloadAll()
})

watch(
  () => route.fullPath,
  () => {
    reloadAll()
  }
)
</script>

<style scoped>
.detail-viewer-page {
  min-height: 560px;
}

.detail-viewer-page__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-header-shell {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-action-bar {
  display: flex;
  min-width: 320px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.detail-action-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.detail-action-group--primary {
  flex: 1 1 auto;
}

.detail-action-group--more,
.detail-action-group--danger {
  flex: 0 0 auto;
}

.detail-handling-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.detail-handling-summary__item {
  min-width: 0;
  padding: 2px 0;
}

.detail-handling-summary__label {
  margin-bottom: 4px;
  font-size: 12px;
  line-height: 18px;
  color: #4b5563;
}

.detail-handling-summary__value {
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-handling-summary__value--blocker {
  color: var(--el-color-danger);
}

.controlled-browser-linkage-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.detail-project-code-linkage-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.detail-project-code-linkage-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.detail-project-code-linkage-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.detail-project-code-linkage-card__label {
  margin-bottom: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.detail-project-code-linkage-card__value {
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.controlled-browser-linkage-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.controlled-browser-linkage-card__label {
  margin-bottom: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.controlled-browser-linkage-card__value {
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.controlled-browser-linkage-card__meta {
  overflow: hidden;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trace-file-evidence {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.publish-completion-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.publish-completion-summary-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.publish-completion-summary-card.is-ok {
  border-color: var(--el-color-success-light-5);
  background: var(--el-color-success-light-9);
}

.publish-completion-summary-card.is-warning {
  border-color: var(--el-color-warning-light-5);
  background: var(--el-color-warning-light-9);
}

.publish-completion-summary-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.publish-completion-summary-card__value {
  overflow: hidden;
  margin-top: 6px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.publish-completion-summary-card__description {
  margin-top: 6px;
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}

.detail-access-explanation {
  display: grid;
  gap: 10px;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
}

.detail-access-explanation__title {
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.detail-access-explanation__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.detail-access-explanation__item {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.detail-access-explanation__label {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.detail-access-explanation__value {
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-table-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

:deep(.controlled-print-record-row--latest) {
  --el-table-tr-bg-color: #fff7db;
  outline: 2px solid var(--el-color-warning);
  outline-offset: -2px;
}

.route-snapshot-summary {
  display: grid;
  gap: 5px;
  min-width: 0;
  font-size: 12px;
  line-height: 18px;
}

.route-snapshot-summary__title {
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-snapshot-summary__line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.route-snapshot-summary__meta {
  overflow: hidden;
  color: #4b5563;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-distribution-summary,
.detail-recovery-summary {
  display: grid;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
  line-height: 18px;
}

.detail-distribution-summary__line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.detail-distribution-summary__medium {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-distribution-summary__meta,
.detail-recovery-summary__meta {
  overflow: hidden;
  color: #4b5563;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-training-summary {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.detail-training-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.detail-training-overview__item {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #f8fbff;
}

.detail-training-overview__item--wide {
  grid-column: span 2;
}

.detail-training-overview__label {
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.detail-training-overview__value {
  margin-top: 4px;
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .detail-training-overview__item--wide {
    grid-column: span 1;
  }
}

.detail-training-summary__line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.detail-training-summary__progress,
.detail-training-summary__time {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.detail-lifecycle-timeline {
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.detail-lifecycle-timeline__item {
  position: relative;
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 10px;
  padding: 8px 0 10px;
}

.detail-lifecycle-timeline__item:not(:last-child)::after {
  position: absolute;
  top: 28px;
  bottom: -2px;
  left: 8px;
  width: 1px;
  background: #dbe3ef;
  content: '';
}

.detail-lifecycle-timeline__marker {
  z-index: 1;
  width: 10px;
  height: 10px;
  margin-top: 7px;
  margin-left: 3px;
  border-radius: 50%;
  background: #1677ff;
  box-shadow: 0 0 0 3px rgb(22 119 255 / 12%);
}

.detail-lifecycle-timeline__content {
  min-width: 0;
}

.detail-lifecycle-timeline__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.detail-lifecycle-timeline__title {
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-lifecycle-timeline__time {
  margin-top: 3px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
  font-variant-numeric: tabular-nums;
}

.detail-lifecycle-timeline__description,
.detail-lifecycle-timeline__actor {
  margin-top: 4px;
  color: #263247;
  font-size: 13px;
  line-height: 20px;
  word-break: break-word;
}

.detail-lifecycle-timeline__actor {
  color: #4b5563;
}

.detail-viewer-split {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 400px);
  gap: 16px;
  align-items: start;
}

.detail-viewer-split__file,
.detail-viewer-split__detail {
  min-width: 0;
}

.detail-viewer-split__detail {
  position: sticky;
  top: 12px;
  max-height: calc(100vh - 150px);
  overflow: auto;
  padding: 18px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  box-shadow: 0 10px 24px rgb(15 23 42 / 6%);
}

.stage-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.stage-card {
  padding: 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
  background: var(--el-bg-color);
}

.stage-card.is-current {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px rgb(64 158 255 / 16%);
}

.stage-card.is-completed {
  background: var(--el-color-success-light-9);
}

.stage-card__meta {
  display: grid;
  gap: 4px;
  margin-top: 10px;
  color: var(--el-text-color-regular);
  font-size: 12px;
  line-height: 1.5;
}

.signature-hash {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.signature-snapshot-muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1180px) {
  .detail-header-shell {
    flex-direction: column;
  }

  .detail-action-bar {
    min-width: 0;
    justify-content: flex-start;
  }

  .detail-action-group {
    justify-content: flex-start;
  }

  .detail-handling-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .controlled-browser-linkage-grid,
  .detail-project-code-linkage-grid,
  .detail-access-explanation__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-viewer-split {
    grid-template-columns: 1fr;
  }

  .detail-viewer-split__detail {
    position: static;
    max-height: none;
  }
}

@media (max-width: 680px) {
  .detail-handling-summary {
    grid-template-columns: 1fr;
  }

  .detail-access-explanation__grid {
    grid-template-columns: 1fr;
  }

  .detail-handling-summary__value {
    white-space: normal;
  }

  .detail-lifecycle-timeline__heading {
    align-items: flex-start;
  }

  .detail-lifecycle-timeline__title {
    white-space: normal;
  }
}
</style>
