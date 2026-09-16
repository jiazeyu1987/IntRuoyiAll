<template>
  <div class="qa-regulation-page" data-qa-regulation-page>
    <ContentWrap class="qa-regulation-page__workspace-tabs-wrap">
      <el-tabs
        v-model="regulationWorkspaceTab"
        class="qa-regulation-page__workspace-tabs"
        data-qa-regulation-workspace-tabs
      >
        <el-tab-pane label="QA检验规程" name="qa" data-qa-regulation-qa-workspace-tab />
        <el-tab-pane label="通用检验规程" name="common" data-qa-regulation-common-workspace-tab />
      </el-tabs>
    </ContentWrap>

    <ContentWrap
      v-show="regulationWorkspaceTab === 'qa'"
      class="qa-regulation-page__project-wrap"
      data-qa-regulation-qa-workspace
      data-qa-regulation-dcc-project
    >
      <div class="qa-regulation-page__header">
        <div class="qa-regulation-page__title">QA检验规程</div>
        <el-form label-width="0" class="qa-regulation-page__form qa-regulation-page__project-form">
          <el-form-item class="qa-regulation-page__project-field">
            <div class="qa-regulation-page__project-selector" data-qa-regulation-project-selector>
              <el-select
                v-model="qaRegulationDraft.dccProjectCodeId"
                aria-label="DCC 项目代码"
                clearable
                automatic-dropdown
                default-first-option
                filterable
                remote
                remote-show-suffix
                reserve-keyword
                :loading="dccProjectCodeOptionsLoading"
                :remote-method="loadDccProjectCodeOptions"
                placeholder="请选择 DCC 项目代码"
                class="qa-regulation-page__project-select !w-100%"
                data-qa-regulation-project-copyable
                data-qa-regulation-project-dropdown
                @change="handleDccProjectCodeChange"
                @visible-change="handleDccProjectCodeVisibleChange"
              >
                <el-option
                  v-for="project in dccProjectCodeOptions"
                  :key="project.id"
                  :label="formatDccProjectCodeOption(project)"
                  :value="project.id"
                  :class="getDccProjectCodeOptionClass(project)"
                >
                  <span
                    class="qa-regulation-page__project-option-label"
                    :class="{ 'is-configured': isDccProjectCodeConfigured(project) }"
                  >
                    {{ formatDccProjectCodeOption(project) }}
                  </span>
                </el-option>
              </el-select>
              <el-button
                plain
                aria-label="复制 DCC 项目代码"
                title="复制 DCC 项目代码"
                class="qa-regulation-page__project-copy-button"
                data-qa-regulation-project-copy
                :disabled="!selectedDccProjectCodeLabel"
                @click="copySelectedDccProjectCode"
              >
                复制
              </el-button>
            </div>
          </el-form-item>
        </el-form>
        <div
          class="qa-regulation-page__published-version"
          data-qa-regulation-current-published-version
          aria-live="polite"
        >
          <span class="qa-regulation-page__published-version-label">当前已发布版本</span>
          <span
            v-if="!selectedDccProjectCode"
            class="qa-regulation-page__published-version-value is-empty"
          >
            请先选择项目
          </span>
          <span
            v-else-if="qaCurrentPublishedVersionLoading"
            class="qa-regulation-page__published-version-value is-loading"
          >
            加载中
          </span>
          <span
            v-else-if="qaCurrentPublishedVersionLoadError"
            class="qa-regulation-page__published-version-value is-error"
            :title="qaCurrentPublishedVersionLoadError"
          >
            {{ qaCurrentPublishedVersionLoadError }}
          </span>
          <strong
            v-else-if="qaCurrentPublishedVersion"
            class="qa-regulation-page__published-version-value"
          >
            {{ qaCurrentPublishedVersion.versionNo }}
          </strong>
          <span v-else class="qa-regulation-page__published-version-value is-empty">
            暂无已发布版本
          </span>
        </div>
        <div class="qa-regulation-page__version-publish" data-qa-regulation-version-publish>
          <label class="qa-regulation-page__header-field">
            <span class="qa-regulation-page__header-field-label">版本</span>
            <el-select
              v-model="selectedQaRegulationVersionId"
              aria-label="规程版本"
              size="small"
              filterable
              :loading="qaRegulationVersionOptionsLoading"
              :disabled="!selectedDccProjectCode || qaRegulationVersionOptionsLoading"
              placeholder="请选择版本"
              class="qa-regulation-page__version-input"
              data-qa-regulation-version-dropdown
              @change="handleQaRegulationVersionChange"
            >
              <el-option
                v-for="version in qaRegulationVersionOptions"
                :key="version.versionId"
                :label="formatQaRegulationVersionOption(version)"
                :value="version.versionId"
              >
                <span>{{ formatQaRegulationVersionOption(version) }}</span>
              </el-option>
            </el-select>
          </label>
          <label class="qa-regulation-page__header-field">
            <span class="qa-regulation-page__header-field-label">生效日期</span>
            <el-date-picker
              v-model="qaRegulationDraft.effectiveDate"
              aria-label="生效日期"
              value-format="YYYY-MM-DD"
              type="date"
              size="small"
              class="qa-regulation-page__effective-date"
            />
          </label>
          <el-tag
            data-qa-regulation-configuration-status
            data-qa-regulation-selected-version-status
            :type="resolveQaRegulationLifecycleStatusTagType(selectedQaRegulationVersionStatus)"
            effect="plain"
          >
            {{ qaSelectedVersionStatusText }}
          </el-tag>
          <el-button
            data-qa-regulation-header-save
            :loading="qaRegulationSaving"
            :disabled="
              !selectedDccProjectCode ||
              qaCurrentConfigurationLoading ||
              qaRegulationPublishing ||
              qaRegulationResetting
            "
            @click="previewQaRegulationDraft"
          >
            保存草稿
          </el-button>
          <el-button
            type="primary"
            :loading="qaRegulationPublishing"
            :disabled="
              !selectedDccProjectCode ||
              qaCurrentConfigurationLoading ||
              qaRegulationSaving ||
              qaRegulationResetting
            "
            @click="runQaPublishPrecheck"
          >
            发布规程
          </el-button>
          <el-button
            plain
            data-qa-regulation-word-import
            :loading="qaWordImportSubmitting"
            :disabled="qaRegulationSaving || qaRegulationPublishing || qaRegulationResetting"
            @click="openQaWordImportDialog"
          >
            <Icon icon="ep:document" class="mr-4px" />
            解析
          </el-button>
          <el-button
            type="danger"
            plain
            data-qa-regulation-test-reset
            v-hasPermi="['mes:qc-template:update']"
            :loading="qaRegulationResetting"
            :disabled="
              !selectedDccProjectCode ||
              qaCurrentConfigurationLoading ||
              qaRegulationSaving ||
              qaRegulationPublishing ||
              qaWordImportSubmitting
            "
            @click="resetQaRegulationForTesting"
          >
            测试重置
          </el-button>
        </div>
      </div>

      <div
        v-if="dccProjectCodeLoadError"
        class="qa-regulation-page__load-error"
        data-qa-regulation-project-load-error
      >
        <el-alert :title="dccProjectCodeLoadError" type="error" :closable="false" show-icon />
        <el-button type="primary" plain @click="retryLoadDccProjectCodes">重新加载</el-button>
      </div>
    </ContentWrap>

    <ContentWrap
      v-show="regulationWorkspaceTab === 'common'"
      class="qa-regulation-page__project-wrap"
      data-qa-common-layout-header
    >
      <div class="qa-regulation-page__header">
        <div class="qa-regulation-page__title">通用检验规程</div>
        <el-form label-width="0" class="qa-regulation-page__form qa-regulation-page__project-form">
          <el-form-item class="qa-regulation-page__project-field">
            <div class="qa-regulation-page__project-selector" data-qa-common-set-selector>
              <el-select
                v-model="selectedCommonRegulationSetId"
                aria-label="通用规程套"
                clearable
                filterable
                placeholder="请选择通用规程套"
                class="qa-regulation-page__project-select !w-100%"
                data-qa-common-set-switch
                @change="handleCommonRegulationSetSwitch"
              >
                <el-option
                  v-for="set in commonRegulationSets"
                  :key="set.id"
                  :label="`${set.setCode} / ${set.setName}`"
                  :value="set.id"
                />
              </el-select>
              <el-button
                plain
                aria-label="复制通用规程套编号"
                title="复制通用规程套编号"
                class="qa-regulation-page__project-copy-button"
                :disabled="!selectedCommonRegulationSet"
                data-qa-common-set-copy
                @click="copyCommonRegulationSetCode"
              >
                复制
              </el-button>
            </div>
          </el-form-item>
        </el-form>
        <div
          class="qa-regulation-page__published-version"
          data-qa-common-current-published-version
          aria-live="polite"
        >
          <span class="qa-regulation-page__published-version-label">当前已发布版本</span>
          <span
            v-if="commonRegulationVersionOptionsLoading"
            class="qa-regulation-page__published-version-value is-loading"
          >
            加载中
          </span>
          <strong
            v-else-if="commonRegulationCurrentPublishedVersion"
            class="qa-regulation-page__published-version-value"
          >
            {{ commonRegulationCurrentPublishedVersion.versionNo }}
          </strong>
          <span v-else class="qa-regulation-page__published-version-value is-empty">
            暂无已发布版本
          </span>
        </div>
        <div class="qa-regulation-page__version-publish" data-qa-common-version-publish>
          <label class="qa-regulation-page__header-field">
            <span class="qa-regulation-page__header-field-label">版本</span>
            <el-select
              v-model="selectedCommonRegulationSetVersionPreviewId"
              aria-label="通用规程套版本"
              size="small"
              filterable
              :disabled="!selectedCommonRegulationSet"
              placeholder="请选择版本"
              class="qa-regulation-page__version-input"
              data-qa-common-version-dropdown
              @change="handleCommonRegulationVersionChange"
            >
              <el-option
                v-for="version in selectedCommonRegulationSet?.versions || []"
                :key="version.id"
                :label="`${version.versionNo} / ${resolveQaRegulationLifecycleStatusText(version.lifecycleStatus)}`"
                :value="version.id"
              />
            </el-select>
          </label>
          <label class="qa-regulation-page__header-field">
            <span class="qa-regulation-page__header-field-label">生效日期</span>
            <el-date-picker
              :model-value="selectedCommonRegulationSetVersionPreview?.effectiveDate"
              aria-label="通用规程套版本生效日期"
              value-format="YYYY-MM-DD"
              type="date"
              size="small"
              disabled
              class="qa-regulation-page__effective-date"
              data-qa-common-effective-date
            />
          </label>
          <el-tag
            :type="
              resolveQaRegulationLifecycleStatusTagType(
                selectedCommonRegulationSetVersionPreview?.lifecycleStatus
              )
            "
            effect="plain"
            data-qa-common-selected-version-status
          >
            {{ commonRegulationSelectedVersionStatusText }}
          </el-tag>
          <el-button
            data-qa-common-save-draft
            :disabled="!selectedCommonRegulationSet"
            @click="openCommonRegulationDraftDialog"
          >
            保存草稿
          </el-button>
          <el-button
            type="primary"
            data-qa-common-publish
            :disabled="selectedCommonRegulationSetVersionPreview?.lifecycleStatus !== 'DRAFT'"
            @click="openCommonRegulationPublishDialog"
          >
            发布规程
          </el-button>
          <el-button
            plain
            data-qa-regulation-common-word-import
            :loading="qaWordImportSubmitting"
            @click="openCommonQaWordImportDialog"
          >
            <Icon icon="ep:document" class="mr-4px" />
            解析
          </el-button>
          <el-button
            plain
            :disabled="!selectedCommonRegulationSet"
            data-qa-common-set-edit
            @click="openCommonRegulationSetDialog(selectedCommonRegulationSet)"
          >
            编辑规程套
          </el-button>
        </div>
      </div>
      <el-alert
        v-if="commonRegulationVersionOptionsLoadError"
        :title="commonRegulationVersionOptionsLoadError"
        type="error"
        :closable="false"
        show-icon
        class="mt-12px"
        data-qa-common-workspace-load-error
      />
    </ContentWrap>

    <el-dialog
      v-model="qaWordImportDialogVisible"
      :title="qaWordImportDialogTitle"
      width="620px"
      destroy-on-close
      data-qa-regulation-word-import-dialog
      :close-on-click-modal="!qaWordImportSubmitting"
      :close-on-press-escape="!qaWordImportSubmitting"
      :show-close="!qaWordImportSubmitting"
      @closed="resetQaWordImportDialog"
    >
      <el-form label-position="top" class="qa-regulation-page__word-import-form">
        <el-form-item label="QA 模板文件" required>
          <el-upload
            v-model:file-list="qaWordImportFileList"
            action="#"
            accept=".docx"
            drag
            :auto-upload="false"
            :disabled="qaWordImportSubmitting"
            :limit="1"
            :on-change="handleQaWordImportFileChange"
            :on-remove="handleQaWordImportFileRemove"
            :on-exceed="handleQaWordImportFileExceed"
            class="qa-regulation-page__word-import-upload"
            data-qa-regulation-word-import-file
          >
            <Icon icon="ep:upload-filled" class="qa-regulation-page__word-import-upload-icon" />
            <div class="el-upload__text">拖放文件或<em>选择文件</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .docx 文件</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="绑定项目" required>
          <el-select
            v-model="qaWordImportDccProjectCodeId"
            aria-label="QA 模板绑定 DCC 项目代码"
            clearable
            automatic-dropdown
            default-first-option
            filterable
            remote
            remote-show-suffix
            reserve-keyword
            :disabled="qaWordImportSubmitting"
            :loading="dccProjectCodeOptionsLoading"
            :remote-method="loadDccProjectCodeOptions"
            placeholder="请选择 DCC 项目代码"
            class="!w-100%"
            data-qa-regulation-word-import-project
            @visible-change="handleQaWordImportProjectVisibleChange"
          >
            <el-option
              v-for="project in dccProjectCodeOptions"
              :key="project.id"
              :label="formatDccProjectCodeOption(project)"
              :value="project.id"
            >
              <span
                class="qa-regulation-page__project-option-label"
                :class="{ 'is-configured': isDccProjectCodeConfigured(project) }"
              >
                {{ formatDccProjectCodeOption(project) }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="qaWordImportSubmitting" @click="qaWordImportDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="qaWordImportSubmitting"
          data-qa-regulation-word-import-confirm
          @click="submitQaWordImport"
        >
          {{ qaWordImportConfirmText }}
        </el-button>
      </template>
    </el-dialog>

    <template v-if="regulationWorkspaceTab === 'qa' && !selectedDccProjectCode">
      <ContentWrap>
        <el-card shadow="never" data-qa-regulation-qa-empty>
          <template #header>QA检验规程详情</template>
          <el-empty description="请选择 DCC 项目代码后查看 QA 检验规程" />
        </el-card>
      </ContentWrap>
    </template>

    <template v-if="regulationWorkspaceTab === 'common' || selectedDccProjectCode">
      <ContentWrap
        v-if="regulationWorkspaceTab === 'qa' && selectedDccProjectCode"
        class="qa-regulation-page__tabs-wrap"
      >
        <el-tabs
          v-model="qaActiveTab"
          class="qa-regulation-page__tabs qa-regulation-page__tabs--flat"
          data-qa-regulation-tabs
          data-qa-regulation-qa-detail-tabs
        >
          <el-tab-pane label="总览" name="overview" />
          <el-tab-pane label="检验项目" name="items" />
          <el-tab-pane label="任务预览" name="verification" />
        </el-tabs>
      </ContentWrap>

      <ContentWrap v-if="regulationWorkspaceTab === 'common'" class="qa-regulation-page__tabs-wrap">
        <el-tabs
          v-model="commonRegulationActiveTab"
          class="qa-regulation-page__tabs qa-regulation-page__tabs--flat"
          data-qa-common-detail-tabs
        >
          <el-tab-pane label="总览" name="overview" />
          <el-tab-pane label="检验项目" name="items" />
          <el-tab-pane label="版本记录" name="versions" />
        </el-tabs>
      </ContentWrap>

      <ContentWrap
        v-show="regulationWorkspaceTab === 'common'"
        v-loading="commonRegulationVersionOptionsLoading || commonRegulationSetLoading"
        data-qa-regulation-common-workspace
      >
        <div class="qa-regulation-page__common-panel" data-qa-regulation-common-panel>
          <section class="qa-regulation-page__common-set-workbench">
            <div v-show="commonRegulationActiveTab === 'overview'" data-qa-common-overview>
              <div class="qa-regulation-page__overview-stack">
                <el-card
                  shadow="never"
                  class="qa-regulation-page__overview-card qa-regulation-page__common-binding-card"
                  data-qa-common-overview-summary
                >
                  <template #header>
                    <div class="qa-regulation-page__common-binding-head">
                      <div>
                        <strong>通用规程套信息</strong>
                        <p class="qa-regulation-page__common-binding-subtitle">
                          维护可复用的通用检验规程套，供产品 QA 规程引用已发布套版本。
                        </p>
                      </div>
                      <el-tag
                        data-qa-common-overview-status
                        :type="
                          selectedCommonRegulationSet?.setStatus === 'ENABLED' ? 'success' : 'info'
                        "
                        effect="plain"
                      >
                        {{
                          selectedCommonRegulationSet
                            ? selectedCommonRegulationSet.setStatus === 'ENABLED'
                              ? '已启用'
                              : '已停用'
                            : '未选择规程套'
                        }}
                      </el-tag>
                    </div>
                  </template>
                  <el-empty
                    v-if="!selectedCommonRegulationSet"
                    description="请选择一个通用规程套后查看总览"
                    :image-size="72"
                  />
                  <template v-else>
                    <div class="qa-regulation-page__common-grid">
                      <div class="qa-regulation-page__common-field" data-qa-common-overview-current>
                        <span class="qa-regulation-page__common-label">当前规程套</span>
                        <strong class="qa-regulation-page__common-value">
                          {{ selectedCommonRegulationSet.setName }}
                        </strong>
                      </div>
                      <div class="qa-regulation-page__common-field" data-qa-common-overview-version>
                        <span class="qa-regulation-page__common-label">所选版本</span>
                        <strong class="qa-regulation-page__common-value">
                          {{ selectedCommonRegulationSetVersionPreview?.versionNo || '未选择版本' }}
                        </strong>
                      </div>
                      <div
                        class="qa-regulation-page__common-field qa-regulation-page__common-field--control"
                        data-qa-common-overview-version-select
                      >
                        <span class="qa-regulation-page__common-label">版本预览</span>
                        <el-select
                          v-model="selectedCommonRegulationSetVersionPreviewId"
                          class="!w-100%"
                          filterable
                          placeholder="请选择版本"
                          data-qa-common-overview-version-dropdown
                          @change="handleCommonRegulationVersionChange"
                        >
                          <el-option
                            v-for="version in selectedCommonRegulationSet?.versions || []"
                            :key="version.id"
                            :label="`${version.versionNo} / ${resolveQaRegulationLifecycleStatusText(version.lifecycleStatus)}`"
                            :value="version.id"
                          />
                        </el-select>
                      </div>
                      <div class="qa-regulation-page__common-field" data-qa-common-overview-compose>
                        <span class="qa-regulation-page__common-label">规程组成</span>
                        <strong class="qa-regulation-page__common-value">
                          {{ commonRegulationOverviewComposeText }}
                        </strong>
                      </div>
                    </div>
                    <div class="qa-regulation-page__common-actions">
                      <el-button
                        plain
                        data-qa-common-overview-refresh
                        :loading="commonRegulationSetLoading"
                        @click="reloadCommonRegulationSets"
                      >
                        刷新
                      </el-button>
                      <el-button
                        type="primary"
                        plain
                        data-qa-common-set-create
                        @click="openCommonRegulationSetDialog()"
                      >
                        新增套
                      </el-button>
                      <el-button
                        plain
                        data-qa-common-overview-version-create
                        @click="openCommonRegulationSetVersionDialog(selectedCommonRegulationSet)"
                      >
                        新增版本
                      </el-button>
                      <el-button
                        plain
                        data-qa-common-overview-edit
                        @click="openCommonRegulationSetDialog(selectedCommonRegulationSet)"
                      >
                        编辑规程套
                      </el-button>
                      <el-button
                        type="danger"
                        plain
                        data-qa-common-overview-delete
                        @click="
                          selectedCommonRegulationSet &&
                          deleteCommonRegulationSet(selectedCommonRegulationSet)
                        "
                      >
                        删除
                      </el-button>
                    </div>
                  </template>
                </el-card>

                <el-card
                  shadow="never"
                  class="qa-regulation-page__overview-card"
                  data-qa-common-overview-info
                >
                  <template #header>规程信息</template>
                  <el-empty
                    v-if="!selectedCommonRegulationSet"
                    description="请选择一个通用规程套后查看规程信息"
                    :image-size="72"
                  />
                  <el-form
                    v-else
                    class="qa-regulation-page__form qa-regulation-page__basic-form"
                    label-width="88px"
                  >
                    <div class="qa-regulation-page__basic-grid">
                      <el-form-item
                        label="套编号"
                        class="qa-regulation-page__basic-field qa-regulation-page__basic-field--full"
                      >
                        <el-input :model-value="selectedCommonRegulationSet.setCode" disabled />
                      </el-form-item>
                      <el-form-item
                        label="套名称"
                        class="qa-regulation-page__basic-field qa-regulation-page__basic-field--full"
                      >
                        <el-input :model-value="selectedCommonRegulationSet.setName" disabled />
                      </el-form-item>
                      <el-form-item
                        label="当前版本"
                        class="qa-regulation-page__basic-field qa-regulation-page__basic-field--full"
                      >
                        <el-input :model-value="commonRegulationOverviewVersionText" disabled />
                      </el-form-item>
                      <el-form-item
                        label="版本组成"
                        class="qa-regulation-page__basic-field qa-regulation-page__basic-field--full"
                      >
                        <el-input :model-value="commonRegulationOverviewDetailText" disabled />
                      </el-form-item>
                    </div>
                  </el-form>
                </el-card>

                <el-card
                  shadow="never"
                  class="qa-regulation-page__overview-card qa-regulation-page__overview-note"
                  data-qa-common-overview-note
                >
                  <template #header>备注</template>
                  <ol class="qa-regulation-page__overview-note-list">
                    <li>这里维护的是可复用的通用检验规程主档；</li>
                    <li>产品绑定关系应绑定到正式产品与已发布版本；</li>
                    <li>不按产品名称或代际文本推断通用规程套版本。</li>
                  </ol>
                </el-card>
              </div>
            </div>

            <div
              v-show="commonRegulationActiveTab === 'versions'"
              class="qa-regulation-page__common-set-detail"
              data-qa-common-set-version-detail
              data-qa-common-versions
            >
              <el-empty
                v-if="!selectedCommonRegulationSet"
                description="请选择一个通用规程套后查看版本记录"
                :image-size="72"
              />
              <template v-else>
                <div class="qa-regulation-page__common-set-detail-head">
                  <div class="qa-regulation-page__common-set-detail-title">
                    <strong>套版本</strong>
                    <span>{{ selectedCommonRegulationSet.setName }}</span>
                    <el-tag effect="plain">{{ selectedCommonRegulationSet.setCode }}</el-tag>
                  </div>
                  <el-button
                    type="primary"
                    plain
                    data-qa-common-set-version-create
                    @click="openCommonRegulationSetVersionDialog(selectedCommonRegulationSet)"
                  >
                    <Icon class="mr-5px" icon="ep:plus" />
                    新增套版本
                  </el-button>
                </div>
                <el-table
                  :data="selectedCommonRegulationSet.versions || []"
                  row-key="id"
                  border
                  stripe
                  highlight-current-row
                  :current-row-key="selectedCommonRegulationSetVersionPreviewId"
                  empty-text="该套暂无版本"
                  max-height="320"
                  data-qa-common-set-version-table
                  @row-click="selectCommonRegulationSetVersion"
                >
                  <el-table-column label="套版本" prop="versionNo" width="120">
                    <template #default="{ row }">
                      <div class="qa-regulation-page__common-set-version-cell">
                        <strong>{{ row.versionNo }}</strong>
                        <el-tag
                          v-if="
                            Number(row.id) === Number(selectedCommonRegulationSet.currentVersionId)
                          "
                          size="small"
                          type="success"
                          effect="plain"
                        >
                          当前
                        </el-tag>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="100" align="center">
                    <template #default="{ row }">
                      <el-tag
                        :type="resolveQaRegulationLifecycleStatusTagType(row.lifecycleStatus)"
                        effect="plain"
                      >
                        {{ resolveQaRegulationLifecycleStatusText(row.lifecycleStatus) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column
                    label="生效日期"
                    prop="effectiveDate"
                    width="130"
                    align="center"
                  />
                  <el-table-column label="成员数" width="100" align="center">
                    <template #default="{ row }">{{ row.members?.length || 0 }}</template>
                  </el-table-column>
                  <el-table-column label="包含规程" min-width="320" show-overflow-tooltip>
                    <template #default="{ row }">
                      {{
                        row.members?.map((member) => member.commonRegulationName).join('、') ||
                        '未配置'
                      }}
                    </template>
                  </el-table-column>
                  <el-table-column
                    label="操作"
                    width="160"
                    :fixed="isCommonRegulationSetWideViewport ? 'right' : false"
                    align="center"
                  >
                    <template #default="{ row }">
                      <el-button
                        link
                        type="primary"
                        :disabled="!canEditCommonSetVersion(row)"
                        @click.stop="
                          openCommonRegulationSetVersionDialog(selectedCommonRegulationSet, row)
                        "
                      >
                        编辑
                      </el-button>
                      <el-button
                        link
                        type="danger"
                        :disabled="!canEditCommonSetVersion(row)"
                        @click.stop="deleteCommonRegulationSetVersion(row)"
                      >
                        删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
            </div>

            <div
              v-if="commonRegulationActiveTab === 'items'"
              class="qa-regulation-page__common-set-detail qa-regulation-page__common-set-detail--items"
              data-qa-common-set-member-detail
              data-qa-common-items
            >
              <el-empty
                v-if="!selectedCommonRegulationSetVersionPreview"
                description="请选择通用规程套和版本后查看检验项目"
                :image-size="72"
              />
              <template v-else>
                <div class="qa-regulation-page__common-items-parameter-block">
                  <div class="qa-regulation-page__card-head">
                    <div class="qa-regulation-page__common-set-detail-title">
                      <strong>工序检验方法与抽样方案</strong>
                      <span class="qa-regulation-page__common-set-summary">
                        {{ commonRegulationItemsSummaryText }}
                      </span>
                    </div>
                    <div class="qa-regulation-page__card-actions">
                      <div
                        class="qa-regulation-page__final-inspection-switch"
                        data-qa-common-final-inspection-switch
                      >
                        <span class="qa-regulation-page__final-inspection-label">
                          是否需要末检
                        </span>
                        <el-switch
                          v-model="commonFinalInspectionRequired"
                          active-text="需要"
                          inactive-text="不需要"
                        />
                        <el-tag
                          v-if="
                            !commonFinalInspectionEdited &&
                            ['MIXED', 'EMPTY'].includes(commonFinalInspectionSnapshot.state)
                          "
                          type="warning"
                          effect="plain"
                        >
                          {{ commonFinalInspectionStatusText }}
                        </el-tag>
                        <el-input
                          v-if="commonFinalInspectionReasonVisible"
                          data-qa-common-final-not-applicable-reason
                          v-model="commonFinalInspectionNotApplicableReason"
                          placeholder="填写末检不适用的正式依据"
                          clearable
                          class="qa-regulation-page__final-inspection-reason"
                        />
                      </div>
                      <UserTableColumnSettings
                        :columns="qaItemsColumns"
                        :saving="qaItemsColumnSaving"
                        :show-reset="false"
                        @change="saveQaItemsColumnConfig"
                      />
                      <el-button
                        plain
                        data-qa-common-set-document-maintain
                        :disabled="
                          !selectedCommonRegulationSet ||
                          !canEditCommonSetVersion(selectedCommonRegulationSetVersionPreview)
                        "
                        @click="openSelectedCommonRegulationSetVersionDialog"
                      >
                        维护文档组成
                      </el-button>
                      <el-button
                        type="primary"
                        plain
                        data-qa-common-items-save-version
                        :loading="commonRegulationItemsSaving"
                        :disabled="!selectedCommonRegulationSetVersionPreview"
                        @click="saveCommonRegulationItemsVersion"
                      >
                        保存并升版
                      </el-button>
                    </div>
                  </div>
                  <div
                    class="qa-regulation-page__common-items-parameter-chips"
                    data-qa-common-items-parameters
                  >
                    <el-tag effect="plain">
                      {{ selectedCommonRegulationSetVersionPreview.versionNo }}
                    </el-tag>
                    <el-tag
                      :type="
                        resolveQaRegulationLifecycleStatusTagType(
                          selectedCommonRegulationSetVersionPreview.lifecycleStatus
                        )
                      "
                      effect="plain"
                    >
                      {{
                        resolveQaRegulationLifecycleStatusText(
                          selectedCommonRegulationSetVersionPreview.lifecycleStatus
                        )
                      }}
                    </el-tag>
                    <el-tag effect="plain">
                      {{ selectedCommonRegulationSetVersionDocuments.length }} 份 Word 规程
                    </el-tag>
                    <el-tag effect="plain">{{ commonRegulationItems.length }} 个检验项目</el-tag>
                  </div>
                </div>

                <UnifiedListTemplate
                  table-key="mes.qa.common-regulation.items.processMethods.v1"
                  :query-model="commonRegulationItemsQuery"
                  :filter-definitions="qaEmptyFilterDefinitions"
                  :show-quick-filter="false"
                  :quick-filter-state="qaEmptyQuickFilterState"
                  :selected-filter-definition="qaEmptySelectedFilterDefinition"
                  :operator-options="qaEmptyOperatorOptions"
                  :columns="qaItemsColumns"
                  :column-saving="qaItemsColumnSaving"
                  :show-column-settings="false"
                  :show-query-form="false"
                  :total="commonRegulationItems.length"
                  v-model:page="commonRegulationItemsQuery.pageNo"
                  v-model:limit="commonRegulationItemsQuery.pageSize"
                  @column-change="saveQaItemsColumnConfig"
                  @column-reset="resetQaItemsColumnConfig"
                >
                  <template
                    #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }"
                  >
                    <el-table
                      :data="pagedCommonRegulationItems"
                      row-key="rowKey"
                      border
                      size="small"
                      data-qa-common-items-table
                      data-user-table-column-explicit
                      data-user-table-key="mes.qa.common-regulation.items.processMethods.v1"
                      :empty-text="commonRegulationItemsEmptyText"
                      @header-dragend="handleQaItemsHeaderDragend"
                      @sort-change="handleTemplateSortChange"
                    >
                      <el-table-column
                        v-if="isQaItemsColumnVisible('qaProcessName')"
                        label="工序"
                        prop="qaProcessName"
                        :min-width="getQaItemsColumnMinWidthString('qaProcessName', 170)"
                        v-bind="sortColumnAttrs('qaProcessName')"
                      >
                        <template #default="{ row }">
                          <el-input
                            v-model="row.processName"
                            class="qa-regulation-page__process-name"
                            :disabled="commonRegulationItemsSaving"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('qaProcessCode')"
                        label="QA工序编码"
                        prop="qaProcessCode"
                        :min-width="getQaItemsColumnMinWidthString('qaProcessCode', 150)"
                        v-bind="sortColumnAttrs('qaProcessCode')"
                      >
                        <template #default="{ row }">
                          <el-input
                            v-model="row.processCode"
                            :disabled="commonRegulationItemsSaving"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('itemCode')"
                        label="检验项目编码"
                        prop="itemCode"
                        :width="getQaItemsColumnWidthString('itemCode', 130)"
                        v-bind="sortColumnAttrs('itemCode')"
                      >
                        <template #default="{ row }">
                          <el-input v-model="row.itemCode" :disabled="commonRegulationItemsSaving" />
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('itemName')"
                        label="检验项目"
                        prop="itemName"
                        :min-width="getQaItemsColumnMinWidthString('itemName', 170)"
                        v-bind="sortColumnAttrs('itemName')"
                      >
                        <template #default="{ row }">
                          <el-input v-model="row.itemName" :disabled="commonRegulationItemsSaving" />
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('applicableTypes')"
                        label="适用检验类型"
                        prop="applicableTypes"
                        :min-width="getQaItemsColumnMinWidthString('applicableTypes', 210)"
                        v-bind="sortColumnAttrs('applicableTypes')"
                      >
                        <template #default="{ row }">
                          <div
                            class="qa-regulation-page__applicable-types"
                            data-qa-common-applicable-types
                          >
                            <el-tag
                              v-for="inspectionType in resolveCommonItemApplicableTypes(row)"
                              :key="inspectionType"
                              size="small"
                              effect="plain"
                            >
                              {{ resolveQaInspectionTypeLabel(inspectionType) }}
                            </el-tag>
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('firstInspection')"
                        label="首检"
                        prop="firstInspection"
                        :min-width="getQaItemsColumnMinWidthString('firstInspection', 220)"
                      >
                        <template #default="{ row }">
                          <div
                            class="qa-regulation-page__item-inspection-rule"
                            data-qa-common-first-inspection
                          >
                            <el-switch
                              v-model="row.firstInspectionEnabled"
                              active-text="启用"
                              inactive-text="不启用"
                              :disabled="commonRegulationItemsSaving"
                              @change="handleQaFirstInspectionEnabledChange(row)"
                            />
                            <div
                              v-if="row.firstInspectionEnabled"
                              class="qa-regulation-page__inspection-value"
                            >
                              <el-input-number
                                v-model="row.firstInspectionQuantity"
                                aria-label="首检固定数量"
                                :min="1"
                                :step="1"
                                :precision="0"
                                controls-position="right"
                                :disabled="commonRegulationItemsSaving"
                              />
                              <span>件</span>
                            </div>
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('patrolInspection')"
                        label="巡检"
                        prop="patrolInspection"
                        :min-width="getQaItemsColumnMinWidthString('patrolInspection', 240)"
                      >
                        <template #default="{ row }">
                          <div
                            class="qa-regulation-page__item-inspection-rule"
                            data-qa-common-patrol-inspection
                          >
                            <el-switch
                              v-model="row.patrolInspectionEnabled"
                              active-text="启用"
                              inactive-text="不启用"
                              :disabled="commonRegulationItemsSaving"
                              @change="handleQaPatrolInspectionEnabledChange(row)"
                            />
                            <div
                              v-if="row.patrolInspectionEnabled"
                              class="qa-regulation-page__inspection-value"
                            >
                              <span>AQL</span>
                              <el-input-number
                                v-model="row.patrolInspectionRatio"
                                aria-label="巡检比例"
                                :min="0.01"
                                :max="100"
                                :step="0.1"
                                :precision="2"
                                controls-position="right"
                                :disabled="commonRegulationItemsSaving"
                              />
                              <span>%</span>
                            </div>
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('standardText')"
                        label="接受标准"
                        prop="standardText"
                        :min-width="getQaItemsColumnMinWidthString('standardText', 280)"
                        v-bind="sortColumnAttrs('standardText')"
                      >
                        <template #default="{ row }">
                          <el-input
                            v-model="row.standardText"
                            type="textarea"
                            :autosize="{ minRows: 2, maxRows: 4 }"
                            :disabled="commonRegulationItemsSaving"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('inspectionMethod')"
                        label="检验方法"
                        prop="inspectionMethod"
                        :min-width="getQaItemsColumnMinWidthString('inspectionMethod', 240)"
                        v-bind="sortColumnAttrs('inspectionMethod')"
                      >
                        <template #default="{ row }">
                          <el-input
                            v-model="row.inspectionMethod"
                            type="textarea"
                            :autosize="{ minRows: 2, maxRows: 4 }"
                            :disabled="commonRegulationItemsSaving"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('inspectionTool')"
                        label="检验器具及设备"
                        prop="inspectionTool"
                        :min-width="getQaItemsColumnMinWidthString('inspectionTool', 170)"
                        v-bind="sortColumnAttrs('inspectionTool')"
                      >
                        <template #default="{ row }">
                          <div
                            class="qa-regulation-page__inspection-tool-cell"
                            data-qa-common-item-equipment
                          >
                            <el-input
                              v-model="row.inspectionTool"
                              type="textarea"
                              :autosize="{ minRows: 2, maxRows: 4 }"
                              :disabled="commonRegulationItemsSaving"
                            />
                            <div
                              v-for="(equipment, equipmentIndex) in row.equipmentOptions"
                              :key="`${row.rowKey}-${equipment.equipmentId || equipmentIndex}`"
                              class="qa-regulation-page__equipment-binding"
                            >
                              <el-select
                                v-model="equipment.equipmentId"
                                filterable
                                :loading="qaMachineryOptionsLoading"
                                :disabled="
                                  commonRegulationItemsSaving ||
                                  qaMachineryOptionsLoading ||
                                  qaMachineryOptions.length === 0
                                "
                                placeholder="选择设备台账"
                                class="qa-regulation-page__equipment-select"
                                :aria-label="`${row.itemName || '检验项目'}检验设备`"
                                @change="
                                  handleCommonRegulationItemEquipmentChange(
                                    row,
                                    equipmentIndex,
                                    $event
                                  )
                                "
                              >
                                <el-option
                                  v-for="machinery in getAvailableQaMachinery(row, equipmentIndex)"
                                  :key="machinery.id"
                                  :label="formatQaMachineryLabel(machinery)"
                                  :value="machinery.id"
                                />
                              </el-select>
                              <span class="qa-regulation-page__equipment-number">
                                编号：{{ equipment.equipmentNumber || '待选择' }}
                              </span>
                              <el-button
                                text
                                type="danger"
                                :disabled="commonRegulationItemsSaving"
                                :aria-label="`删除${row.itemName || '检验项目'}的检验设备`"
                                title="删除检验设备"
                                @click="removeCommonRegulationItemEquipment(row, equipmentIndex)"
                              >
                                <Icon icon="ep:delete" />
                              </el-button>
                            </div>
                            <el-button
                              link
                              type="primary"
                              :disabled="
                                commonRegulationItemsSaving ||
                                qaMachineryOptionsLoading ||
                                qaMachineryOptions.length === 0
                              "
                              data-qa-common-item-equipment-add
                              @click="addCommonRegulationItemEquipment(row)"
                            >
                              <Icon icon="ep:plus" class="mr-4px" />
                              新增设备
                            </el-button>
                            <span
                              v-if="qaMachineryOptionsLoadError"
                              class="qa-regulation-page__equipment-error"
                            >
                              {{ qaMachineryOptionsLoadError }}
                            </span>
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('samplingPlan')"
                        label="原抽样方案"
                        prop="samplingPlan"
                        :min-width="getQaItemsColumnMinWidthString('samplingPlan', 240)"
                        v-bind="sortColumnAttrs('samplingPlan')"
                      >
                        <template #default="{ row }">
                          <div class="qa-regulation-page__sampling-plan">
                            {{ formatQaItemSamplingPlan(row) }}
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('resultType')"
                        label="结果类型"
                        prop="resultType"
                        :width="getQaItemsColumnWidthString('resultType', 130)"
                        v-bind="sortColumnAttrs('resultType')"
                      >
                        <template #default="{ row }">
                          <el-select v-model="row.resultType" :disabled="commonRegulationItemsSaving">
                            <el-option label="合格/不合格" value="BOOLEAN" />
                            <el-option label="数值" value="NUMERIC" />
                            <el-option label="文本" value="TEXT" />
                          </el-select>
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('sourceOriginalExcerpt')"
                        label="原文依据"
                        prop="sourceOriginalExcerpt"
                        :min-width="getQaItemsColumnMinWidthString('sourceOriginalExcerpt', 420)"
                        v-bind="sortColumnAttrs('sourceOriginalExcerpt')"
                      >
                        <template #default="{ row }">
                          <div class="qa-regulation-page__source" data-qa-common-original-excerpt>
                            <div class="qa-regulation-page__source-meta">
                              <el-tag size="small" type="info" effect="plain">
                                PDF 第 {{ row.sourceOriginalPage || '未记录' }} 页
                              </el-tag>
                              <span>{{ row.sourceOriginalItem || row.sourceDocumentTitle }}</span>
                            </div>
                            <div class="qa-regulation-page__source-label">接受标准原文</div>
                            <div class="qa-regulation-page__source-text">
                              {{ row.sourceOriginalExcerpt || '该通用规程未记录原文摘录。' }}
                            </div>
                            <template v-if="row.sourceOriginalMethod">
                              <div class="qa-regulation-page__source-label">检验方法原文</div>
                              <div class="qa-regulation-page__source-text">
                                {{ row.sourceOriginalMethod }}
                              </div>
                            </template>
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('lowerLimit')"
                        label="下限"
                        prop="lowerLimit"
                        :width="getQaItemsColumnWidthString('lowerLimit', 120)"
                        v-bind="sortColumnAttrs('lowerLimit')"
                      >
                        <template #default="{ row }">
                          <el-input-number
                            v-model="row.lowerLimit"
                            :disabled="row.resultType !== 'NUMERIC' || commonRegulationItemsSaving"
                            :controls="false"
                            class="!w-100%"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('upperLimit')"
                        label="上限"
                        prop="upperLimit"
                        :width="getQaItemsColumnWidthString('upperLimit', 120)"
                        v-bind="sortColumnAttrs('upperLimit')"
                      >
                        <template #default="{ row }">
                          <el-input-number
                            v-model="row.upperLimit"
                            :disabled="row.resultType !== 'NUMERIC' || commonRegulationItemsSaving"
                            :controls="false"
                            class="!w-100%"
                          />
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('critical')"
                        label="关键项"
                        prop="critical"
                        :width="getQaItemsColumnWidthString('critical', 100)"
                        v-bind="sortColumnAttrs('critical')"
                      >
                        <template #default="{ row }">
                          <el-checkbox v-model="row.critical" :disabled="commonRegulationItemsSaving">
                            关键
                          </el-checkbox>
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('failureRule')"
                        label="失败规则"
                        prop="failureRule"
                        :min-width="getQaItemsColumnMinWidthString('failureRule', 220)"
                        v-bind="sortColumnAttrs('failureRule')"
                      >
                        <template #default="{ row }">
                          <el-input v-model="row.failureRule" :disabled="commonRegulationItemsSaving" />
                        </template>
                      </el-table-column>
                      <el-table-column
                        v-if="isQaItemsColumnVisible('sourceNote')"
                        label="来源说明"
                        prop="sourceNote"
                        :min-width="getQaItemsColumnMinWidthString('sourceNote', 200)"
                        v-bind="sortColumnAttrs('sourceNote')"
                        show-overflow-tooltip
                      />
                      <el-table-column
                        v-if="isQaItemsColumnVisible('actions')"
                        label="操作"
                        prop="actions"
                        :width="getQaItemsColumnWidthString('actions', 90)"
                        fixed="right"
                      >
                        <template #default="{ row }">
                          <el-button
                            link
                            type="danger"
                            :disabled="commonRegulationItemsSaving"
                            @click="removeCommonRegulationItemByRow(row)"
                          >
                            删除
                          </el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                  </template>
                </UnifiedListTemplate>
              </template>
            </div>
          </section>
        </div>
      </ContentWrap>

      <ContentWrap
        v-show="
          regulationWorkspaceTab === 'qa' && selectedDccProjectCode && qaActiveTab === 'overview'
        "
        v-loading="qaCurrentConfigurationLoading"
      >
        <div class="qa-regulation-page__overview-stack">
          <el-card
            shadow="never"
            class="qa-regulation-page__overview-card qa-regulation-page__common-binding-card"
            data-qa-regulation-common-binding-control
          >
            <template #header>
              <div class="qa-regulation-page__common-binding-head">
                <div>
                  <strong>关联通用检验规程</strong>
                  <p class="qa-regulation-page__common-binding-subtitle">
                    当前产品 QA 规程维护专属检验内容，并引用一个已发布的通用规程套版本。
                  </p>
                </div>
                <el-tag data-qa-regulation-common-binding-status type="info" effect="plain">
                  {{ commonRegulationBindingStatusText }}
                </el-tag>
              </div>
            </template>
            <div class="qa-regulation-page__common-grid">
              <div
                class="qa-regulation-page__common-field"
                data-qa-regulation-common-binding-current
              >
                <span class="qa-regulation-page__common-label">当前关联</span>
                <strong class="qa-regulation-page__common-value">
                  {{ commonRegulationBindingCurrentText }}
                </strong>
              </div>
              <div class="qa-regulation-page__common-field" data-qa-regulation-common-binding-scope>
                <span class="qa-regulation-page__common-label">引用范围</span>
                <strong class="qa-regulation-page__common-value">
                  {{ commonRegulationBindingScopeText }}
                </strong>
              </div>
              <div
                class="qa-regulation-page__common-field qa-regulation-page__common-field--control"
                data-qa-regulation-common-binding-version
              >
                <span class="qa-regulation-page__common-label">绑定套版本</span>
                <el-select
                  v-model="selectedCommonRegulationSetVersionId"
                  class="!w-100%"
                  filterable
                  :loading="commonRegulationVersionOptionsLoading"
                  :disabled="commonRegulationBindingSaving"
                  placeholder="请选择已发布通用规程套版本"
                  data-qa-regulation-common-binding-version-select
                >
                  <el-option
                    v-for="option in commonRegulationSetVersionOptions"
                    :key="option.commonRegulationSetVersionId"
                    :label="formatCommonRegulationSetVersionOption(option)"
                    :value="option.commonRegulationSetVersionId"
                  />
                </el-select>
              </div>
              <div class="qa-regulation-page__common-field">
                <span class="qa-regulation-page__common-label">一线 PQC 拼接方式</span>
                <strong class="qa-regulation-page__common-value">
                  产品专属工序 + 引用的通用包装工序
                </strong>
              </div>
            </div>
            <el-alert
              v-if="commonRegulationBindingLoadError"
              class="qa-regulation-page__common-alert"
              :title="commonRegulationBindingLoadError"
              type="error"
              :closable="false"
              show-icon
            />
            <div class="qa-regulation-page__common-actions">
              <el-button
                plain
                data-qa-regulation-common-binding-view
                :disabled="!currentCommonRegulationBinding"
                @click="viewCurrentCommonRegulation"
              >
                查看通用规程
              </el-button>
              <el-button
                type="primary"
                plain
                data-qa-regulation-common-binding-change
                :loading="commonRegulationBindingSaving"
                :disabled="!selectedCommonRegulationSetVersionId"
                @click="handleBindCommonRegulationVersion"
              >
                保存绑定
              </el-button>
              <el-button
                type="danger"
                plain
                data-qa-regulation-common-binding-disable
                :loading="commonRegulationBindingSaving"
                :disabled="!currentCommonRegulationBinding"
                @click="handleUnbindCommonRegulation"
              >
                解除关联
              </el-button>
            </div>
          </el-card>
          <el-card
            shadow="never"
            class="qa-regulation-page__overview-card"
            data-qa-regulation-scope
          >
            <template #header>规程信息</template>
            <el-form
              :model="qaRegulationDraft"
              label-width="88px"
              class="qa-regulation-page__form qa-regulation-page__basic-form"
              data-qa-regulation-basic-form
            >
              <div class="qa-regulation-page__basic-grid">
                <el-form-item
                  label="规程编号"
                  class="qa-regulation-page__basic-field qa-regulation-page__basic-field--full"
                >
                  <el-input v-model="qaRegulationDraft.regulationCode" />
                </el-form-item>
                <el-form-item
                  label="规程名称"
                  class="qa-regulation-page__basic-field qa-regulation-page__basic-field--full"
                >
                  <el-input v-model="qaRegulationDraft.regulationName" />
                </el-form-item>
                <el-form-item
                  label="DCC 项目"
                  class="qa-regulation-page__basic-field qa-regulation-page__basic-field--full"
                >
                  <el-input :model-value="selectedDccProjectCodeLabel" disabled />
                </el-form-item>
              </div>
            </el-form>
          </el-card>
          <el-card
            shadow="never"
            class="qa-regulation-page__overview-card qa-regulation-page__overview-note"
            data-qa-regulation-overview-note
          >
            <template #header>备注</template>
            <ol
              class="qa-regulation-page__overview-note-list"
              data-qa-regulation-overview-note-list
            >
              <li> 设备初次开机、模具更换、参数调整、模具维修等需要按照抽样规则进行首件检验； </li>
              <li>
                首检如果发现不合格，及时向部门主管/领导汇报，待问题得到纠正后，生产稳定之后，重新进行首检，检验全部合格后，才可转入正常生产；
              </li>
              <li>如果样本量等于或超过批量，则进行100%检验；</li>
              <li>
                过程巡检应每班记录两次，上午和下午各一次，巡检过程中若发现产品不合格，应及时向部门主管反映不合格问题，并对之前生产的产品进行隔离，问题纠正之后，进行双倍检验，确认无异常之后，转入正常抽样。然后对之前生产的产品组织评审，根据评审结果对该批次产品进行处理。
              </li>
            </ol>
          </el-card>
        </div>
      </ContentWrap>

      <ContentWrap
        v-show="
          regulationWorkspaceTab === 'qa' && selectedDccProjectCode && qaActiveTab === 'items'
        "
        v-loading="qaCurrentConfigurationLoading"
      >
        <el-card shadow="never" data-qa-regulation-items>
          <template #header>
            <div class="qa-regulation-page__card-head">
              <span>工序检验方法与抽样方案</span>
              <div class="qa-regulation-page__card-actions">
                <div
                  class="qa-regulation-page__final-inspection-switch"
                  data-qa-regulation-final-inspection-switch
                >
                  <span class="qa-regulation-page__final-inspection-label">是否需要末检</span>
                  <el-switch
                    v-model="finalInspectionRequired"
                    active-text="需要"
                    inactive-text="不需要"
                  />
                  <el-input
                    data-qa-regulation-final-not-applicable-reason
                    v-if="!finalInspectionRequired"
                    v-model="finalInspectionNotApplicableReason"
                    placeholder="填写末检不适用的正式依据"
                    clearable
                    class="qa-regulation-page__final-inspection-reason"
                  />
                </div>
                <UserTableColumnSettings
                  :columns="qaItemsColumns"
                  :saving="qaItemsColumnSaving"
                  :show-reset="false"
                  @change="saveQaItemsColumnConfig"
                />
                <el-button
                  type="primary"
                  plain
                  :disabled="!selectedDccProjectCode"
                  @click="addQaRegulationItem"
                >
                  新增 QA 工序/检验项目
                </el-button>
              </div>
            </div>
          </template>
          <UnifiedListTemplate
            table-key="mes.qa.regulation.items.processMethods.v3"
            :query-model="qaItemsQuery"
            :filter-definitions="qaEmptyFilterDefinitions"
            :show-quick-filter="false"
            :quick-filter-state="qaEmptyQuickFilterState"
            :selected-filter-definition="qaEmptySelectedFilterDefinition"
            :operator-options="qaEmptyOperatorOptions"
            :columns="qaItemsColumns"
            :column-saving="qaItemsColumnSaving"
            :show-column-settings="false"
            :show-query-form="false"
            :total="qaRegulationItems.length"
            v-model:page="qaItemsQuery.pageNo"
            v-model:limit="qaItemsQuery.pageSize"
            @column-change="saveQaItemsColumnConfig"
            @column-reset="resetQaItemsColumnConfig"
          >
            <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
              <el-table
                :data="pagedQaRegulationItems"
                border
                size="small"
                data-user-table-column-explicit
                data-user-table-key="mes.qa.regulation.items.processMethods.v3"
                :empty-text="qaRegulationItemsEmptyText"
                @header-dragend="handleQaItemsHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
                <el-table-column
                  v-if="isQaItemsColumnVisible('qaProcessName')"
                  label="工序"
                  prop="qaProcessName"
                  :min-width="getQaItemsColumnMinWidthString('qaProcessName', 170)"
                  v-bind="sortColumnAttrs('qaProcessName')"
                >
                  <template #default="{ row }">
                    <el-input
                      v-model="row.processName"
                      class="qa-regulation-page__process-name"
                      placeholder="请输入 QA 工序名称"
                    />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('qaProcessCode')"
                  label="QA工序编码"
                  prop="qaProcessCode"
                  :min-width="getQaItemsColumnMinWidthString('qaProcessCode', 150)"
                  v-bind="sortColumnAttrs('qaProcessCode')"
                >
                  <template #default="{ row }">
                    <el-input v-model="row.processCode" placeholder="请输入 QA 工序编码" />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('itemCode')"
                  label="检验项目编码"
                  prop="itemCode"
                  :width="getQaItemsColumnWidthString('itemCode', 130)"
                  v-bind="sortColumnAttrs('itemCode')"
                >
                  <template #default="{ row }">
                    <el-input v-model="row.itemCode" />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('itemName')"
                  label="检验项目"
                  prop="itemName"
                  :min-width="getQaItemsColumnMinWidthString('itemName', 170)"
                  v-bind="sortColumnAttrs('itemName')"
                >
                  <template #default="{ row }">
                    <el-input v-model="row.itemName" />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('applicableTypes')"
                  label="适用检验类型"
                  prop="applicableTypes"
                  :min-width="getQaItemsColumnMinWidthString('applicableTypes', 210)"
                  v-bind="sortColumnAttrs('applicableTypes')"
                >
                  <template #default="{ row }">
                    <div
                      class="qa-regulation-page__applicable-types"
                      data-qa-regulation-applicable-types
                    >
                      <el-tag
                        v-for="inspectionType in resolveQaItemApplicableTypes(row)"
                        :key="inspectionType"
                        size="small"
                        effect="plain"
                      >
                        {{ resolveQaInspectionTypeLabel(inspectionType) }}
                      </el-tag>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('firstInspection')"
                  label="首检"
                  prop="firstInspection"
                  :min-width="getQaItemsColumnMinWidthString('firstInspection', 220)"
                >
                  <template #default="{ row }">
                    <div
                      class="qa-regulation-page__item-inspection-rule"
                      data-qa-regulation-first-inspection
                    >
                      <el-switch
                        v-model="row.firstInspectionEnabled"
                        active-text="启用"
                        inactive-text="不启用"
                        @change="handleQaFirstInspectionEnabledChange(row)"
                      />
                      <div
                        v-if="row.firstInspectionEnabled"
                        class="qa-regulation-page__inspection-value"
                      >
                        <el-input-number
                          v-model="row.firstInspectionQuantity"
                          aria-label="首检固定数量"
                          :min="1"
                          :step="1"
                          :precision="0"
                          controls-position="right"
                        />
                        <span>件</span>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('patrolInspection')"
                  label="巡检"
                  prop="patrolInspection"
                  :min-width="getQaItemsColumnMinWidthString('patrolInspection', 240)"
                >
                  <template #default="{ row }">
                    <div
                      class="qa-regulation-page__item-inspection-rule"
                      data-qa-regulation-patrol-inspection
                    >
                      <el-switch
                        v-model="row.patrolInspectionEnabled"
                        active-text="启用"
                        inactive-text="不启用"
                        @change="handleQaPatrolInspectionEnabledChange(row)"
                      />
                      <div
                        v-if="row.patrolInspectionEnabled"
                        class="qa-regulation-page__inspection-value"
                      >
                        <span>AQL</span>
                        <el-input-number
                          v-model="row.patrolInspectionRatio"
                          aria-label="巡检比例"
                          :min="0.01"
                          :max="100"
                          :step="0.1"
                          :precision="2"
                          controls-position="right"
                        />
                        <span>%</span>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('standardText')"
                  label="接受标准"
                  prop="standardText"
                  :min-width="getQaItemsColumnMinWidthString('standardText', 280)"
                  v-bind="sortColumnAttrs('standardText')"
                >
                  <template #default="{ row }">
                    <el-input
                      v-model="row.standardText"
                      type="textarea"
                      :autosize="{ minRows: 2, maxRows: 4 }"
                    />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('inspectionMethod')"
                  label="检验方法"
                  prop="inspectionMethod"
                  :min-width="getQaItemsColumnMinWidthString('inspectionMethod', 240)"
                  v-bind="sortColumnAttrs('inspectionMethod')"
                >
                  <template #default="{ row }">
                    <el-input
                      v-model="row.inspectionMethod"
                      type="textarea"
                      :autosize="{ minRows: 2, maxRows: 4 }"
                    />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('inspectionTool')"
                  label="检验器具及设备"
                  prop="inspectionTool"
                  :min-width="getQaItemsColumnMinWidthString('inspectionTool', 170)"
                  v-bind="sortColumnAttrs('inspectionTool')"
                >
                  <template #default="{ row }">
                    <div class="qa-regulation-page__inspection-tool-cell" data-qa-item-equipment>
                      <el-input
                        v-model="row.inspectionTool"
                        placeholder="检验器具及设备说明"
                        type="textarea"
                        :autosize="{ minRows: 2, maxRows: 4 }"
                      />
                      <div
                        v-for="(equipment, equipmentIndex) in row.equipmentOptions"
                        :key="`${row.itemCode || row.itemSort}-${equipmentIndex}`"
                        class="qa-regulation-page__equipment-binding"
                      >
                        <el-select
                          v-model="equipment.equipmentId"
                          filterable
                          :loading="qaMachineryOptionsLoading"
                          :disabled="
                            qaMachineryOptionsLoading ||
                            qaEquipmentAutoSaving ||
                            qaMachineryOptions.length === 0
                          "
                          placeholder="选择设备台账"
                          class="qa-regulation-page__equipment-select"
                          :aria-label="`${row.itemName || '检验项目'}检验设备`"
                          @change="handleQaItemEquipmentChange(row, equipmentIndex, $event)"
                        >
                          <el-option
                            v-for="machinery in getAvailableQaMachinery(row, equipmentIndex)"
                            :key="machinery.id"
                            :label="formatQaMachineryLabel(machinery)"
                            :value="machinery.id"
                          />
                        </el-select>
                        <span class="qa-regulation-page__equipment-number">
                          编号：{{ equipment.equipmentNumber || '待选择' }}
                        </span>
                        <el-button
                          text
                          type="danger"
                          :aria-label="`删除${row.itemName || '检验项目'}的检验设备`"
                          title="删除检验设备"
                          @click="removeQaItemEquipment(row, equipmentIndex)"
                        >
                          <Icon icon="ep:delete" />
                        </el-button>
                      </div>
                      <el-button
                        link
                        type="primary"
                        :disabled="
                          qaMachineryOptionsLoading ||
                          qaEquipmentAutoSaving ||
                          qaMachineryOptions.length === 0
                        "
                        data-qa-item-equipment-add
                        @click="addQaItemEquipment(row)"
                      >
                        <Icon icon="ep:plus" class="mr-4px" />
                        新增设备
                      </el-button>
                      <span
                        v-if="qaMachineryOptionsLoadError"
                        class="qa-regulation-page__equipment-error"
                      >
                        {{ qaMachineryOptionsLoadError }}
                      </span>
                      <span
                        v-if="qaEquipmentBindingLoadError"
                        class="qa-regulation-page__equipment-error"
                      >
                        {{ qaEquipmentBindingLoadError }}
                      </span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('samplingPlan')"
                  label="原抽样方案"
                  prop="samplingPlan"
                  :min-width="getQaItemsColumnMinWidthString('samplingPlan', 240)"
                  v-bind="sortColumnAttrs('samplingPlan')"
                >
                  <template #default="{ row }">
                    <div class="qa-regulation-page__sampling-plan">
                      {{ formatQaItemSamplingPlan(row) }}
                    </div>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('resultType')"
                  label="结果类型"
                  prop="resultType"
                  :width="getQaItemsColumnWidthString('resultType', 130)"
                  v-bind="sortColumnAttrs('resultType')"
                >
                  <template #default="{ row }">
                    <el-select v-model="row.resultType">
                      <el-option label="合格/不合格" value="BOOLEAN" />
                      <el-option label="数值" value="NUMERIC" />
                      <el-option label="文本" value="TEXT" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('sourceOriginalExcerpt')"
                  label="原文依据"
                  prop="sourceOriginalExcerpt"
                  :min-width="getQaItemsColumnMinWidthString('sourceOriginalExcerpt', 420)"
                  v-bind="sortColumnAttrs('sourceOriginalExcerpt')"
                >
                  <template #default="{ row }">
                    <div class="qa-regulation-page__source" data-qa-regulation-original-excerpt>
                      <div class="qa-regulation-page__source-meta">
                        <el-tag size="small" type="info" effect="plain">
                          PDF 第 {{ row.sourceOriginalPage || '待补充' }} 页
                        </el-tag>
                        <span>{{ row.sourceOriginalItem || '待补充原文项目' }}</span>
                      </div>
                      <div class="qa-regulation-page__source-label">接受标准原文</div>
                      <div class="qa-regulation-page__source-text">
                        {{
                          row.sourceOriginalExcerpt ||
                          'QA 手工新增项目需补充对应 PDF/规程原文摘录。'
                        }}
                      </div>
                      <template v-if="row.sourceOriginalMethod">
                        <div class="qa-regulation-page__source-label">检验方法原文</div>
                        <div class="qa-regulation-page__source-text">
                          {{ row.sourceOriginalMethod }}
                        </div>
                      </template>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('lowerLimit')"
                  label="下限"
                  prop="lowerLimit"
                  :width="getQaItemsColumnWidthString('lowerLimit', 120)"
                  v-bind="sortColumnAttrs('lowerLimit')"
                >
                  <template #default="{ row }">
                    <el-input-number
                      v-model="row.lowerLimit"
                      :disabled="row.resultType !== 'NUMERIC'"
                      :controls="false"
                      class="!w-100%"
                    />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('upperLimit')"
                  label="上限"
                  prop="upperLimit"
                  :width="getQaItemsColumnWidthString('upperLimit', 120)"
                  v-bind="sortColumnAttrs('upperLimit')"
                >
                  <template #default="{ row }">
                    <el-input-number
                      v-model="row.upperLimit"
                      :disabled="row.resultType !== 'NUMERIC'"
                      :controls="false"
                      class="!w-100%"
                    />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('critical')"
                  label="关键项"
                  prop="critical"
                  :width="getQaItemsColumnWidthString('critical', 100)"
                  v-bind="sortColumnAttrs('critical')"
                >
                  <template #default="{ row }">
                    <el-checkbox v-model="row.critical">关键</el-checkbox>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('failureRule')"
                  label="失败规则"
                  prop="failureRule"
                  :min-width="getQaItemsColumnMinWidthString('failureRule', 220)"
                  v-bind="sortColumnAttrs('failureRule')"
                >
                  <template #default="{ row }">
                    <el-input v-model="row.failureRule" />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isQaItemsColumnVisible('sourceNote')"
                  label="来源说明"
                  prop="sourceNote"
                  :min-width="getQaItemsColumnMinWidthString('sourceNote', 200)"
                  v-bind="sortColumnAttrs('sourceNote')"
                />
                <el-table-column
                  v-if="isQaItemsColumnVisible('actions')"
                  label="操作"
                  prop="actions"
                  :width="getQaItemsColumnWidthString('actions', 90)"
                  fixed="right"
                >
                  <template #default="{ row }">
                    <el-button link type="danger" @click="removeQaRegulationItemByRow(row)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </el-card>
      </ContentWrap>

      <ContentWrap
        v-show="
          regulationWorkspaceTab === 'qa' &&
          selectedDccProjectCode &&
          qaActiveTab === 'verification'
        "
      >
        <div class="qa-regulation-page__layout">
          <el-card shadow="never" data-qa-regulation-completeness>
            <template #header>发布必要条件</template>
            <UnifiedListTemplate
              table-key="mes.qa.regulation.checks"
              :query-model="qaChecksQuery"
              :filter-definitions="qaEmptyFilterDefinitions"
              :show-quick-filter="false"
              :quick-filter-state="qaEmptyQuickFilterState"
              :selected-filter-definition="qaEmptySelectedFilterDefinition"
              :operator-options="qaEmptyOperatorOptions"
              :columns="qaChecksColumns"
              :column-saving="qaChecksColumnSaving"
              :total="qaRegulationPublishChecks.length"
              v-model:page="qaChecksQuery.pageNo"
              v-model:limit="qaChecksQuery.pageSize"
              @column-change="saveQaChecksColumnConfig"
              @column-reset="resetQaChecksColumnConfig"
            >
              <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
                <el-table
                  :data="pagedQaRegulationCompletenessChecks"
                  border
                  size="small"
                  data-user-table-column-explicit
                  data-user-table-key="mes.qa.regulation.checks"
                  @header-dragend="handleQaChecksHeaderDragend"
                  @sort-change="handleTemplateSortChange"
                >
                  <el-table-column
                    v-if="isQaChecksColumnVisible('status')"
                    label="状态"
                    prop="status"
                    :width="getQaChecksColumnWidthString('status', 110)"
                    v-bind="sortColumnAttrs('status')"
                  >
                    <template #default="{ row }">
                      <el-tag :type="row.passed ? 'success' : 'danger'" effect="plain">
                        {{ row.passed ? '已满足' : '需补齐' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isQaChecksColumnVisible('label')"
                    label="检查项"
                    prop="label"
                    :min-width="getQaChecksColumnMinWidthString('label', 180)"
                    v-bind="sortColumnAttrs('label')"
                  >
                    <template #default="{ row }">
                      <div
                        class="qa-regulation-page__check-title"
                        :class="{ 'is-passed': row.passed }"
                      >
                        {{ row.label }}
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column
                    v-if="isQaChecksColumnVisible('detail')"
                    label="说明"
                    prop="detail"
                    :min-width="getQaChecksColumnMinWidthString('detail', 260)"
                    v-bind="sortColumnAttrs('detail')"
                  />
                </el-table>
              </template>
            </UnifiedListTemplate>
            <div class="qa-regulation-page__actions">
              <el-button :loading="qaRegulationSaving" @click="previewQaRegulationDraft">
                保存草稿
              </el-button>
            </div>
          </el-card>

          <el-card shadow="never" data-qa-pqc-task-preview>
            <template #header>PQC 任务预览</template>
            <UnifiedListTemplate
              table-key="mes.qa.regulation.pqcPreview.v2"
              :query-model="qaPqcPreviewQuery"
              :filter-definitions="qaEmptyFilterDefinitions"
              :show-quick-filter="false"
              :quick-filter-state="qaEmptyQuickFilterState"
              :selected-filter-definition="qaEmptySelectedFilterDefinition"
              :operator-options="qaEmptyOperatorOptions"
              :columns="qaPqcPreviewColumns"
              :column-saving="qaPqcPreviewColumnSaving"
              :total="qaPqcTaskPreviewRows.length"
              v-model:page="qaPqcPreviewQuery.pageNo"
              v-model:limit="qaPqcPreviewQuery.pageSize"
              @column-change="saveQaPqcPreviewColumnConfig"
              @column-reset="resetQaPqcPreviewColumnConfig"
            >
              <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
                <el-table
                  :data="pagedQaPqcTaskPreviewRows"
                  border
                  size="small"
                  data-user-table-column-explicit
                  data-user-table-key="mes.qa.regulation.pqcPreview.v2"
                  empty-text="当前没有可预览的检验任务"
                  @header-dragend="handleQaPqcPreviewHeaderDragend"
                  @sort-change="handleTemplateSortChange"
                >
                  <el-table-column
                    v-if="isQaPqcPreviewColumnVisible('qaProcessName')"
                    label="QA 工序"
                    prop="qaProcessName"
                    :min-width="getQaPqcPreviewColumnMinWidthString('qaProcessName', 150)"
                    v-bind="sortColumnAttrs('qaProcessName')"
                  />
                  <el-table-column
                    v-if="isQaPqcPreviewColumnVisible('itemName')"
                    label="检验项目"
                    prop="itemName"
                    :min-width="getQaPqcPreviewColumnMinWidthString('itemName', 160)"
                    v-bind="sortColumnAttrs('itemName')"
                  />
                  <el-table-column
                    v-if="isQaPqcPreviewColumnVisible('inspectionTypeText')"
                    label="检验类型"
                    prop="inspectionTypeText"
                    :min-width="getQaPqcPreviewColumnMinWidthString('inspectionTypeText', 110)"
                    v-bind="sortColumnAttrs('inspectionTypeText')"
                  />
                  <el-table-column
                    v-if="isQaPqcPreviewColumnVisible('roundText')"
                    label="轮次"
                    prop="roundText"
                    :min-width="getQaPqcPreviewColumnMinWidthString('roundText', 110)"
                    v-bind="sortColumnAttrs('roundText')"
                  />
                  <el-table-column
                    v-if="isQaPqcPreviewColumnVisible('plannedQuantityText')"
                    label="计划数量"
                    prop="plannedQuantityText"
                    :min-width="getQaPqcPreviewColumnMinWidthString('plannedQuantityText', 110)"
                    v-bind="sortColumnAttrs('plannedQuantityText')"
                  />
                  <el-table-column
                    v-if="isQaPqcPreviewColumnVisible('regulationVersionNo')"
                    label="规程版本"
                    prop="regulationVersionNo"
                    :min-width="getQaPqcPreviewColumnMinWidthString('regulationVersionNo', 110)"
                    v-bind="sortColumnAttrs('regulationVersionNo')"
                  />
                  <el-table-column
                    v-if="isQaPqcPreviewColumnVisible('taskIdentity')"
                    label="任务身份"
                    prop="taskIdentity"
                    :min-width="getQaPqcPreviewColumnMinWidthString('taskIdentity', 260)"
                    v-bind="sortColumnAttrs('taskIdentity')"
                  />
                </el-table>
              </template>
            </UnifiedListTemplate>
            <el-alert
              class="mt-12px"
              title="QA 规程仅归属于 DCC 项目代码；QA 工序由 QA 独立维护，不与 MES 工艺路线工序进行映射或存在性校验。"
              type="info"
              :closable="false"
              show-icon
            />
          </el-card>
        </div>
      </ContentWrap>
    </template>

    <el-dialog
      v-model="commonRegulationSetDialogVisible"
      title="通用规程套"
      width="520px"
      destroy-on-close
      data-qa-common-set-dialog
    >
      <el-form label-position="top">
        <el-form-item label="套编号" required>
          <el-input
            v-model="commonRegulationSetForm.setCode"
            placeholder="例如：PKG-A"
            data-qa-common-set-code
          />
        </el-form-item>
        <el-form-item label="套名称" required>
          <el-input
            v-model="commonRegulationSetForm.setName"
            placeholder="例如：包装检验 A 套"
            data-qa-common-set-name
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="commonRegulationSetForm.setStatus" class="!w-100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="commonRegulationSetForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button
          :disabled="commonRegulationSetSaving"
          @click="commonRegulationSetDialogVisible = false"
        >
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="commonRegulationSetSaving"
          data-qa-common-set-save
          @click="saveCommonRegulationSet"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="commonRegulationSetVersionDialogVisible"
      title="通用规程套版本 / 文档组成"
      width="860px"
      destroy-on-close
      data-qa-common-set-version-dialog
    >
      <el-form label-position="top">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="套版本" required>
              <el-input
                v-model="commonRegulationSetVersionForm.versionNo"
                placeholder="例如：A、B、C 或 A/1"
                data-qa-common-set-version-no
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="commonRegulationSetVersionForm.lifecycleStatus" class="!w-100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="发布" value="PUBLISHED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生效日期">
              <el-date-picker
                v-model="commonRegulationSetVersionForm.effectiveDate"
                value-format="YYYY-MM-DD"
                type="date"
                class="!w-100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="文档组成（可添加多份 Word 规程）" required>
          <el-table
            :data="commonRegulationSetVersionForm.members"
            row-key="sort"
            border
            data-qa-common-set-version-members
          >
            <el-table-column label="排序" width="90">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.sort"
                  :min="0"
                  controls-position="right"
                  class="!w-100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="包装阶段" width="150">
              <template #default="{ row }">
                <el-input v-model="row.memberRole" placeholder="初包装/大中包装/美联初包装" />
              </template>
            </el-table-column>
            <el-table-column label="Word 规程版本" min-width="300">
              <template #default="{ row }">
                <el-select
                  v-model="row.commonRegulationVersionId"
                  filterable
                  class="!w-100%"
                  placeholder="选择已导入并发布的 Word 规程版本"
                >
                  <el-option
                    v-for="option in commonRegulationVersionOptions"
                    :key="option.commonRegulationVersionId"
                    :label="formatCommonRegulationVersionOption(option)"
                    :value="option.commonRegulationVersionId"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.remark" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ $index }">
                <el-button
                  link
                  type="danger"
                  @click="removeCommonRegulationSetVersionMember($index)"
                >
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-button plain @click="addCommonRegulationSetVersionMember">添加 Word 规程</el-button>
      </el-form>
      <template #footer>
        <el-button
          :disabled="commonRegulationSetSaving"
          @click="commonRegulationSetVersionDialogVisible = false"
        >
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="commonRegulationSetSaving"
          data-qa-common-set-version-save
          @click="saveCommonRegulationSetVersion"
        >
          保存套版本
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile, UploadUserFile } from 'element-plus'
import { useClipboard, useMediaQuery } from '@vueuse/core'
import { useRoute } from 'vue-router'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import {
  type TableQuickFilterDefinition,
  type TableQuickFilterOperator
} from '@/hooks/web/useTableQuickFilter'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCode,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import { DvMachineryApi, type DvMachineryVO } from '@/api/mes/dv/machinery'
import {
  QcTemplateApi,
  type PqcItemEquipmentConfigVO,
  type QaCommonRegulationBindingVO,
  type QaCommonRegulationSetItemsUpgradeItemReqVO,
  type QaCommonRegulationSetItemsUpgradeProcessReqVO,
  type QaCommonRegulationSetItemsUpgradeReqVO,
  type QaCommonRegulationSetMemberVO,
  type QaCommonRegulationSetSaveReqVO,
  type QaCommonRegulationSetVO,
  type QaCommonRegulationSetVersionOptionVO,
  type QaCommonRegulationSetVersionSaveReqVO,
  type QaCommonRegulationVersionOptionVO,
  type QaInspectionRegulationImportRespVO,
  type QaInspectionRegulationInspectionTypeRuleVO,
  type QaInspectionRegulationItemEquipmentVO,
  type QaInspectionRegulationItemVO,
  type QaInspectionRegulationOwnerModule,
  type QaInspectionRegulationProcessVO,
  type QaInspectionRegulationPublishedVersionVO,
  type QaInspectionRegulationProjectStatusVO,
  type QaInspectionRegulationSaveItemVO,
  type QaInspectionRegulationSaveProcessVO,
  type QaInspectionRegulationSaveReqVO,
  type QaInspectionRegulationVersionOptionVO
} from '@/api/mes/qc/template'
import {
  createQaItemInspectionState,
  isQaItemInspectionConfigurationComplete,
  resolveQaItemDisplayInspectionTypes,
  resolveQaItemInspectionPayload,
  type QaItemDisplayInspectionType,
  type QaItemInspectionState
} from './qaRegulationItemInspection'

defineOptions({ name: 'MesProProcessPoolQaRegulation' })

type QaInspectionResultType = 'BOOLEAN' | 'NUMERIC' | 'TEXT'
type RegulationWorkspaceTabName = 'qa' | 'common'
type QaRegulationTabName = 'overview' | 'items' | 'verification'
type CommonRegulationTabName = 'overview' | 'items' | 'versions'
type QaInspectionTypeValue = QaItemDisplayInspectionType

interface QaInspectionTypeRule {
  key: QaInspectionTypeValue
  inspectionType: 'FIRST' | 'PATROL' | 'FINAL'
  label: string
  roundLabel: string
  required: boolean
  fixedQuantity?: number
  notApplicableReason?: string
  taskRule: string
  releaseGate: string
}

interface QaRegulationItem extends QaItemInspectionState {
  qaProcessId?: number
  processCode: string
  processName: string
  processSort: number
  itemSort: number
  itemCode: string
  itemName: string
  inspectionMethod: string
  inspectionTool: string
  samplingPlanText?: string
  resultType: QaInspectionResultType
  standardText: string
  standardUnit?: string
  standardPrecision?: number
  lowerLimit?: number
  upperLimit?: number
  critical: boolean
  failureRule: string
  sourceNote: string
  sourceOriginalPage?: number
  sourceOriginalItem?: string
  sourceOriginalExcerpt?: string
  sourceOriginalMethod?: string
  equipmentOptions: QaInspectionRegulationItemVO['equipmentOptions']
}

interface QaRegulationDraft {
  regulationId?: number
  dccProjectCodeId?: number
  regulationCode: string
  regulationName: string
  versionNo: string
  effectiveDate: string
  lifecycleStatus: string
}

interface QaLocalListQuery {
  pageNo: number
  pageSize: number
}

interface CommonRegulationSetDocumentView extends QaCommonRegulationSetMemberVO {
  documentKey: string
  documentIndex: number
  documentTitle: string
  sourceFileName: string
  processCount: number
  itemCount: number
}

interface CommonRegulationItem extends QaRegulationItem {
  rowKey: string
  sourceNoteRaw: string
  sourceDocumentTitle: string
  sourceFileName: string
  memberRole: string
  commonDccProjectCodeId: number
  commonRegulationId: number
  commonRegulationVersionId: number
  commonRegulationCode: string
  commonRegulationName: string
  commonVersionNo: string
  memberFinalInspectionApplicable: boolean
  memberFinalInspectionNotApplicableReason: string
  memberInspectionTypeRules: QaInspectionTypeRule[]
}

type CommonFinalInspectionState = 'EMPTY' | 'REQUIRED' | 'NOT_REQUIRED' | 'MIXED'

const DCC_PROJECT_CODE_PAGE_SIZE = 200
const QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY =
  'int-ruoyi:qa-regulation:last-dcc-project-code-id'

const regulationWorkspaceTab = ref<RegulationWorkspaceTabName>('qa')
const qaActiveTab = ref<QaRegulationTabName>('overview')
const commonRegulationActiveTab = ref<CommonRegulationTabName>('overview')
const qaItemsQuery = reactive<QaLocalListQuery>({ pageNo: 1, pageSize: 10 })
const qaChecksQuery = reactive<QaLocalListQuery>({ pageNo: 1, pageSize: 10 })
const qaPqcPreviewQuery = reactive<QaLocalListQuery>({ pageNo: 1, pageSize: 10 })
const commonRegulationSetListQuery = reactive<QaLocalListQuery>({ pageNo: 1, pageSize: 10 })
const commonRegulationItemsQuery = reactive<QaLocalListQuery>({ pageNo: 1, pageSize: 10 })
const selectedCommonRegulationVersionId = ref<number>()
const selectedCommonRegulationSetVersionId = ref<number>()
const currentCommonRegulationBinding = ref<QaCommonRegulationBindingVO>()
const commonRegulationVersionOptions = ref<QaCommonRegulationVersionOptionVO[]>([])
const commonRegulationSetVersionOptions = ref<QaCommonRegulationSetVersionOptionVO[]>([])
const commonRegulationSets = ref<QaCommonRegulationSetVO[]>([])
const selectedCommonRegulationSetId = ref<number>()
const selectedCommonRegulationSetVersionPreviewId = ref<number>()
const commonRegulationVersionOptionsLoading = ref(false)
const commonRegulationSetLoading = ref(false)
const commonRegulationVersionOptionsLoadError = ref('')
const commonRegulationBindingLoading = ref(false)
const commonRegulationBindingSaving = ref(false)
const commonRegulationSetSaving = ref(false)
const commonRegulationItemsSaving = ref(false)
const commonFinalInspectionEdited = ref(false)
const commonFinalInspectionDraftRequired = ref(false)
const commonFinalInspectionDraftReason = ref('')
const commonRegulationBindingLoadError = ref('')
const commonRegulationSetDialogVisible = ref(false)
const commonRegulationSetVersionDialogVisible = ref(false)
const commonRegulationSetForm = reactive<QaCommonRegulationSetSaveReqVO>({
  setCode: '',
  setName: '',
  setStatus: 'ENABLED',
  remark: ''
})
const commonRegulationSetVersionForm = reactive<QaCommonRegulationSetVersionSaveReqVO>({
  setId: 0,
  versionNo: '',
  lifecycleStatus: 'DRAFT',
  effectiveDate: '',
  remark: '',
  members: []
})
let commonRegulationBindingLoadSerial = 0
const qaEmptyQuickFilterState = reactive({})
const qaEmptyFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [])
const qaEmptySelectedFilterDefinition = computed<TableQuickFilterDefinition | undefined>(
  () => undefined
)
const qaEmptyOperatorOptions = computed<TableQuickFilterOperator[]>(() => [])

function paginateQaRows<T>(rows: readonly T[], query: QaLocalListQuery): T[] {
  const pageSize = Math.max(1, Number(query.pageSize) || 10)
  const pageNo = Math.max(1, Number(query.pageNo) || 1)
  return rows.slice((pageNo - 1) * pageSize, pageNo * pageSize)
}

const keepQaLocalPageInRange = (query: QaLocalListQuery, total: number) => {
  const maxPage = Math.max(1, Math.ceil(total / Math.max(1, Number(query.pageSize) || 10)))
  if (query.pageNo > maxPage) {
    query.pageNo = maxPage
  }
}

const qaItemsDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'qaProcessName', label: '工序', minWidth: 170 },
  { key: 'qaProcessCode', label: 'QA工序编码', minWidth: 150 },
  { key: 'itemName', label: '检验项目', minWidth: 170 },
  { key: 'standardText', label: '接受标准', minWidth: 280 },
  { key: 'inspectionMethod', label: '检验方法', minWidth: 240 },
  { key: 'inspectionTool', label: '检验器具及设备', minWidth: 170 },
  { key: 'samplingPlan', label: '原抽样方案', minWidth: 240, sortable: false },
  { key: 'itemCode', label: '检验项目编码', width: 130, visible: false },
  { key: 'applicableTypes', label: '适用检验类型', minWidth: 210 },
  { key: 'firstInspection', label: '首检', minWidth: 220, sortable: false },
  { key: 'patrolInspection', label: '巡检', minWidth: 240, sortable: false },
  { key: 'resultType', label: '结果类型', width: 130, visible: false },
  { key: 'sourceOriginalExcerpt', label: '原文依据', minWidth: 420, visible: false },
  { key: 'lowerLimit', label: '下限', width: 120, visible: false },
  { key: 'upperLimit', label: '上限', width: 120, visible: false },
  { key: 'critical', label: '关键项', width: 100, visible: false },
  { key: 'failureRule', label: '失败规则', minWidth: 220, visible: false },
  { key: 'sourceNote', label: '来源说明', minWidth: 200, visible: false },
  { key: 'actions', label: '操作', width: 90, hideable: false, business: false }
]

const qaChecksDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'status', label: '状态', width: 110 },
  { key: 'label', label: '检查项', minWidth: 180 },
  { key: 'detail', label: '说明', minWidth: 260 }
]

const qaPqcPreviewDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'qaProcessName', label: 'QA 工序', minWidth: 150 },
  { key: 'itemName', label: '检验项目', minWidth: 160 },
  { key: 'inspectionTypeText', label: '检验类型', minWidth: 110 },
  { key: 'roundText', label: '轮次', minWidth: 110 },
  { key: 'plannedQuantityText', label: '计划数量', minWidth: 110 },
  { key: 'regulationVersionNo', label: '规程版本', minWidth: 110 },
  { key: 'taskIdentity', label: '任务身份', minWidth: 260 }
]

const commonRegulationSetDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'setCode', label: '套编号', width: 190, sortable: false },
  { key: 'setName', label: '套名称', minWidth: 240, sortable: false },
  { key: 'setStatus', label: '状态', width: 100, sortable: false },
  { key: 'currentVersion', label: '当前版本', width: 130, sortable: false },
  { key: 'versionCount', label: '版本数', width: 100, sortable: false },
  { key: 'memberCount', label: '当前成员', width: 110, sortable: false },
  { key: 'operation', label: '操作', width: 230, hideable: false, business: false }
]

const {
  columns: qaItemsColumns,
  saving: qaItemsColumnSaving,
  isColumnVisible: isQaItemsColumnVisible,
  getColumnWidthString: getQaItemsColumnWidthString,
  getColumnMinWidthString: getQaItemsColumnMinWidthString,
  handleHeaderDragend: handleQaItemsHeaderDragend,
  saveConfig: saveQaItemsColumnConfig,
  resetConfig: resetQaItemsColumnConfig
} = useUserTableColumns('mes.qa.regulation.items.processMethods.v3', qaItemsDefaultColumns)

const {
  columns: qaChecksColumns,
  saving: qaChecksColumnSaving,
  isColumnVisible: isQaChecksColumnVisible,
  getColumnWidthString: getQaChecksColumnWidthString,
  getColumnMinWidthString: getQaChecksColumnMinWidthString,
  handleHeaderDragend: handleQaChecksHeaderDragend,
  saveConfig: saveQaChecksColumnConfig,
  resetConfig: resetQaChecksColumnConfig
} = useUserTableColumns('mes.qa.regulation.checks', qaChecksDefaultColumns)

const {
  columns: qaPqcPreviewColumns,
  saving: qaPqcPreviewColumnSaving,
  isColumnVisible: isQaPqcPreviewColumnVisible,
  getColumnMinWidthString: getQaPqcPreviewColumnMinWidthString,
  handleHeaderDragend: handleQaPqcPreviewHeaderDragend,
  saveConfig: saveQaPqcPreviewColumnConfig,
  resetConfig: resetQaPqcPreviewColumnConfig
} = useUserTableColumns('mes.qa.regulation.pqcPreview.v2', qaPqcPreviewDefaultColumns)

const {
  columns: commonRegulationSetColumns,
  saving: commonRegulationSetColumnSaving,
  isColumnVisible: isCommonRegulationSetColumnVisible,
  getColumnWidthString: getCommonRegulationSetColumnWidthString,
  getColumnMinWidthString: getCommonRegulationSetColumnMinWidthString,
  handleHeaderDragend: handleCommonRegulationSetHeaderDragend,
  saveConfig: saveCommonRegulationSetColumnConfig,
  resetConfig: resetCommonRegulationSetColumnConfig
} = useUserTableColumns('mes.qa.common-regulation-set.main', commonRegulationSetDefaultColumns)

const createEmptyQaRegulationDraft = (): QaRegulationDraft => ({
  regulationId: undefined,
  dccProjectCodeId: undefined,
  regulationCode: '',
  regulationName: '',
  versionNo: '',
  effectiveDate: '',
  lifecycleStatus: 'DRAFT'
})

const createEmptyQaInspectionTypeRules = (): QaInspectionTypeRule[] => [
  {
    key: 'FIRST',
    inspectionType: 'FIRST',
    label: '首检',
    roundLabel: '每个适用订单开始前',
    required: true,
    taskRule: '按发布规程固定数量生成首检任务',
    releaseGate: '缺少适用检验项目时不能发布'
  },
  {
    key: 'PATROL_AM',
    inspectionType: 'PATROL',
    label: '上午巡检',
    roundLabel: '上午班次独立轮次',
    required: true,
    taskRule: '按订单数量与项目抽样比例生成任务',
    releaseGate: '缺少适用检验项目时不能发布'
  },
  {
    key: 'PATROL_PM',
    inspectionType: 'PATROL',
    label: '下午巡检',
    roundLabel: '下午班次独立轮次',
    required: true,
    taskRule: '按订单数量与项目抽样比例生成任务',
    releaseGate: '缺少适用检验项目时不能发布'
  },
  {
    key: 'FINAL',
    inspectionType: 'FINAL',
    label: '末检',
    roundLabel: '订单结束前',
    required: false,
    notApplicableReason: '',
    taskRule: '启用末检时生成末检任务',
    releaseGate: '末检适用性必须明确'
  }
]

const qaRegulationDraft = reactive<QaRegulationDraft>(createEmptyQaRegulationDraft())
const qaInspectionTypeRules = reactive<QaInspectionTypeRule[]>(createEmptyQaInspectionTypeRules())
const qaRegulationItems = ref<QaRegulationItem[]>([])
const qaPublishedVersionNo = ref('')
const qaRegulationVersionOptions = ref<QaInspectionRegulationVersionOptionVO[]>([])
const selectedQaRegulationVersionId = ref<number>()
const qaRegulationVersionOptionsLoading = ref(false)
const qaRegulationVersionOptionsLoadError = ref('')
let qaRegulationVersionOptionsLoadSerial = 0
const qaRegulationSaving = ref(false)
const qaRegulationPublishing = ref(false)
const qaCurrentConfigurationLoading = ref(false)
const qaCurrentConfigurationLoadError = ref('')
const qaConfigurationExists = ref(false)
let qaCurrentConfigurationLoadSerial = 0

const qaWordImportDialogVisible = ref(false)
const qaWordImportSubmitting = ref(false)
const qaWordImportDccProjectCodeId = ref<number>()
const qaWordImportFile = ref<File>()
const qaWordImportFileList = ref<UploadUserFile[]>([])
const qaWordImportOwnerModule = ref<QaInspectionRegulationOwnerModule>('MES_QA')
const qaRegulationResetting = ref(false)

const dccProjectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const dccProjectCodeOptionsLoading = ref(false)
const dccProjectCodeLoadError = ref('')
const selectedDccProjectCode = ref<DccProjectCodeRespVO>()
const qaMachineryOptions = ref<DvMachineryVO[]>([])
const qaMachineryOptionsLoading = ref(false)
const qaMachineryOptionsLoadError = ref('')
const qaEquipmentBindingLoadError = ref('')
const qaEquipmentAutoSaving = ref(false)
const QA_MACHINERY_PAGE_SIZE = 100
const qaRegulationProjectStatusByDccId = ref(
  new Map<number, QaInspectionRegulationProjectStatusVO>()
)
const qaCurrentPublishedVersion = ref<QaInspectionRegulationPublishedVersionVO>()
const qaCurrentPublishedVersionLoading = ref(false)
const qaCurrentPublishedVersionLoadError = ref('')
let qaCurrentPublishedVersionLoadSerial = 0

const route = useRoute()
const { copy: copyQaProjectSelectionToClipboard } = useClipboard({ legacy: true })
const isCommonRegulationSetWideViewport = useMediaQuery('(min-width: 900px)')

const QA_INSPECTION_TYPE_LABELS: Record<QaInspectionTypeValue, string> = {
  FIRST: '首检',
  PATROL_AM: '上午巡检',
  PATROL_PM: '下午巡检',
  FINAL: '末检'
}

const resolveQaInspectionTypeLabel = (inspectionType: QaInspectionTypeValue) =>
  QA_INSPECTION_TYPE_LABELS[inspectionType]

const qaWordImportDialogTitle = computed(() =>
  qaWordImportOwnerModule.value === 'MES_QA_COMMON' ? '导入通用检验规程' : '解析 QA 模板'
)

const qaWordImportConfirmText = computed(() =>
  qaWordImportOwnerModule.value === 'MES_QA_COMMON' ? '导入并发布' : '确定'
)

const finalInspectionRule = computed(() =>
  qaInspectionTypeRules.find((rule) => rule.key === 'FINAL')
)

const finalInspectionRequired = computed<boolean>({
  get: () => Boolean(finalInspectionRule.value?.required),
  set: (required: boolean) => {
    if (!finalInspectionRule.value) {
      throw new Error('缺少末检规则配置')
    }
    finalInspectionRule.value.required = required
  }
})

const finalInspectionNotApplicableReason = computed<string>({
  get: () => finalInspectionRule.value?.notApplicableReason ?? '',
  set: (reason: string) => {
    if (!finalInspectionRule.value) {
      throw new Error('缺少末检规则配置')
    }
    finalInspectionRule.value.notApplicableReason = reason
  }
})

const resolveQaItemApplicableTypes = (item: QaRegulationItem) =>
  resolveQaItemDisplayInspectionTypes(item, finalInspectionRequired.value)

const pagedQaRegulationItems = computed(() => paginateQaRows(qaRegulationItems.value, qaItemsQuery))

const selectedDccProjectCodeLabel = computed(() =>
  selectedDccProjectCode.value ? formatDccProjectCodeOption(selectedDccProjectCode.value) : ''
)

const commonRegulationBindingStatusText = computed(() =>
  !selectedDccProjectCode.value
    ? '未选择项目'
    : commonRegulationBindingLoading.value || commonRegulationVersionOptionsLoading.value
      ? '加载中'
      : currentCommonRegulationBinding.value
        ? '已关联'
        : '未关联'
)

const commonRegulationBindingCurrentText = computed(() =>
  currentCommonRegulationBinding.value
    ? currentCommonRegulationBinding.value.commonRegulationSetId
      ? commonRegulationSetDisplayText.value
      : [
          currentCommonRegulationBinding.value.commonRegulationName,
          currentCommonRegulationBinding.value.commonRegulationCode,
          currentCommonRegulationBinding.value.versionNo
        ]
          .filter(Boolean)
          .join(' / ')
    : selectedDccProjectCode.value
      ? '未关联通用检验规程'
      : '请先选择 DCC 项目代码'
)

const commonRegulationBindingScopeText = computed(() =>
  selectedDccProjectCodeLabel.value
    ? `当前 DCC 项目：${selectedDccProjectCodeLabel.value}`
    : '请选择 DCC 项目代码'
)

const selectedCommonRegulationSet = computed(() =>
  commonRegulationSets.value.find(
    (set) => Number(set.id) === Number(selectedCommonRegulationSetId.value)
  )
)

const selectedCommonRegulationSetVersionPreview = computed(() =>
  selectedCommonRegulationSet.value?.versions.find(
    (version) => Number(version.id) === Number(selectedCommonRegulationSetVersionPreviewId.value)
  )
)

const commonRegulationCurrentPublishedVersion = computed(() => {
  const set = selectedCommonRegulationSet.value
  if (!set) {
    return undefined
  }
  return (
    set.versions.find((version) => Number(version.id) === Number(set.currentVersionId)) ||
    set.versions.find((version) => version.currentPublished)
  )
})

const commonRegulationSelectedVersionStatusText = computed(() =>
  selectedCommonRegulationSetVersionPreview.value
    ? resolveQaRegulationLifecycleStatusText(
        selectedCommonRegulationSetVersionPreview.value.lifecycleStatus
      )
    : '未选择版本'
)

const sortCommonRegulationSetMembers = (members: QaCommonRegulationSetMemberVO[] = []) =>
  [...members].sort((left, right) => {
    const sortDiff = Number(left.sort ?? 0) - Number(right.sort ?? 0)
    if (sortDiff !== 0) {
      return sortDiff
    }
    return Number(left.id ?? 0) - Number(right.id ?? 0)
  })

const countCommonRegulationSetMemberItems = (member: QaCommonRegulationSetMemberVO) =>
  (member.processes || []).reduce((count, process) => count + (process.items || []).length, 0)

const resolveCommonRegulationSetMemberSourceFileName = (member: QaCommonRegulationSetMemberVO) => {
  for (const process of member.processes || []) {
    for (const item of process.items || []) {
      const sourceNote = item.sourceNote?.trim()
      if (!sourceNote) {
        continue
      }
      const match = sourceNote.match(/导入自\s*QA\s*模板[:：]\s*(.+)$/)
      if (match?.[1]?.trim()) {
        return match[1].trim()
      }
    }
  }
  return ''
}

const selectedCommonRegulationSetVersionDocuments = computed<CommonRegulationSetDocumentView[]>(
  () =>
    sortCommonRegulationSetMembers(
      selectedCommonRegulationSetVersionPreview.value?.members || []
    ).map((member, index) => ({
      ...member,
      documentKey: String(member.id || member.commonRegulationVersionId || index),
      documentIndex: index + 1,
      documentTitle: [member.memberRole, member.commonRegulationCode, member.commonRegulationName]
        .filter(Boolean)
        .join(' / '),
      sourceFileName: resolveCommonRegulationSetMemberSourceFileName(member),
      processCount: member.processes?.length || 0,
      itemCount: countCommonRegulationSetMemberItems(member)
    }))
)

const formatCommonRegulationSetDocumentTitle = (document: CommonRegulationSetDocumentView) =>
  document.documentTitle || document.commonRegulationName || '未命名 Word 规程'

const formatCommonRegulationSetDocumentSource = (document: CommonRegulationSetDocumentView) =>
  document.sourceFileName ||
  `${document.commonRegulationCode || '未编号'} / ${document.versionNo || '未版本'}`

const requireCommonMemberFinalInspectionApplicable = (
  member: QaCommonRegulationSetMemberVO
): boolean => {
  if (member.finalInspectionApplicable !== true && member.finalInspectionApplicable !== false) {
    throw new Error(`${member.commonRegulationName || '通用规程成员'}缺少正式末检适用性`)
  }
  return member.finalInspectionApplicable
}

const resolveCommonMemberInspectionRules = (
  member: QaCommonRegulationSetMemberVO,
  finalInspectionApplicable?: boolean,
  finalInspectionNotApplicableReason?: string
): QaInspectionTypeRule[] => {
  if (!Array.isArray(member.inspectionTypeRules)) {
    throw new Error(`${member.commonRegulationName || '通用规程成员'}缺少正式检验类型规则`)
  }
  const rules = member.inspectionTypeRules.map((rule) => ({ ...rule }))
  if (finalInspectionApplicable === undefined) {
    return rules
  }
  const finalRule = rules.find((rule) => rule.key === 'FINAL')
  if (!finalRule) {
    throw new Error(`${member.commonRegulationName || '通用规程成员'}缺少正式末检规则配置`)
  }
  finalRule.required = finalInspectionApplicable
  finalRule.notApplicableReason = finalInspectionApplicable
    ? undefined
    : finalInspectionNotApplicableReason?.trim() || undefined
  return rules
}

const selectedCommonRegulationSetVersionMembers = computed(() =>
  sortCommonRegulationSetMembers(selectedCommonRegulationSetVersionPreview.value?.members || [])
)

const commonFinalInspectionSnapshot = computed<{
  state: CommonFinalInspectionState
  required: boolean
  reason: string
}>(() => {
  const members = selectedCommonRegulationSetVersionMembers.value
  if (members.length === 0) {
    return { state: 'EMPTY', required: false, reason: '该套版本暂无成员规程' }
  }
  const memberStates = members.map(requireCommonMemberFinalInspectionApplicable)
  const uniqueStates = new Set(memberStates)
  if (uniqueStates.size > 1) {
    return {
      state: 'MIXED',
      required: false,
      reason: '成员规程的末检适用性不一致，请按来源规程查看'
    }
  }
  const required = memberStates[0] === true
  if (required) {
    return { state: 'REQUIRED', required: true, reason: '' }
  }
  const reasons = Array.from(
    new Set(
      members
        .map((member) => member.finalInspectionNotApplicableReason?.trim())
        .filter((reason): reason is string => Boolean(reason))
    )
  )
  return {
    state: 'NOT_REQUIRED',
    required: false,
    reason: reasons.length > 0 ? reasons.join('；') : '未记录末检不适用依据'
  }
})

const commonFinalInspectionRequired = computed<boolean>({
  get: () => commonFinalInspectionDraftRequired.value,
  set: (required: boolean) => applyCommonFinalInspectionRequired(required)
})

const commonFinalInspectionStatusText = computed(() => {
  const snapshot = commonFinalInspectionSnapshot.value
  if (snapshot.state === 'MIXED') {
    return '成员配置不一致'
  }
  if (snapshot.state === 'EMPTY') {
    return '未配置'
  }
  return snapshot.required ? '需要末检' : '不需要末检'
})

const commonFinalInspectionReasonVisible = computed(() => !commonFinalInspectionRequired.value)

const commonFinalInspectionNotApplicableReason = computed<string>({
  get: () => commonFinalInspectionDraftReason.value,
  set: (reason: string) => applyCommonFinalInspectionReason(reason)
})

const resetCommonFinalInspectionDraft = () => {
  const snapshot = commonFinalInspectionSnapshot.value
  commonFinalInspectionEdited.value = false
  commonFinalInspectionDraftRequired.value = snapshot.required
  commonFinalInspectionDraftReason.value = snapshot.required ? '' : snapshot.reason
}

const syncCommonFinalInspectionToRows = (required: boolean, reason: string) => {
  const nextReason = required ? '' : reason
  commonRegulationItems.value.forEach((row) => {
    row.memberFinalInspectionApplicable = required
    row.memberFinalInspectionNotApplicableReason = nextReason
    row.memberInspectionTypeRules = row.memberInspectionTypeRules.map((rule) => {
      if (rule.key !== 'FINAL') {
        return rule
      }
      return {
        ...rule,
        required,
        notApplicableReason: required ? undefined : nextReason || undefined
      }
    })
  })
}

const applyCommonFinalInspectionRequired = (required: boolean) => {
  commonFinalInspectionEdited.value = true
  commonFinalInspectionDraftRequired.value = required
  if (required) {
    commonFinalInspectionDraftReason.value = ''
  }
  syncCommonFinalInspectionToRows(required, commonFinalInspectionDraftReason.value)
}

const applyCommonFinalInspectionReason = (reason: string) => {
  commonFinalInspectionEdited.value = true
  commonFinalInspectionDraftReason.value = reason
  syncCommonFinalInspectionToRows(false, reason)
}

const createCommonRegulationItemSourceNote = (
  document: CommonRegulationSetDocumentView,
  item: QaInspectionRegulationItemVO
) =>
  [
    item.sourceNote?.trim(),
    `来源：${formatCommonRegulationSetDocumentTitle(document)} / ${formatCommonRegulationSetDocumentSource(document)}`
  ]
    .filter(Boolean)
    .join('；')

const buildCommonRegulationItemsFromDocuments = (): CommonRegulationItem[] =>
  selectedCommonRegulationSetVersionDocuments.value.flatMap((document) =>
    [...(document.processes || [])]
      .sort((left, right) => Number(left.sort ?? 0) - Number(right.sort ?? 0))
      .flatMap((process, processIndex) =>
        [...(process.items || [])]
          .sort((left, right) => Number(left.itemSort ?? 0) - Number(right.itemSort ?? 0))
          .map((item, itemIndex) => ({
            rowKey: [
              document.documentKey,
              process.qaProcessId || process.processCode || process.processName || processIndex,
              item.itemCode || item.itemName || itemIndex
            ].join('-'),
            qaProcessId: process.qaProcessId,
            processCode: process.processCode || '',
            processName: process.processName || '',
            processSort: process.sort ?? processIndex + 1,
            itemSort: item.itemSort ?? itemIndex + 1,
            itemCode: item.itemCode || '',
            itemName: item.itemName || '',
            inspectionMethod: item.inspectionMethod || '',
            inspectionTool: item.inspectionTool || '',
            samplingPlanText: item.samplingPlanText || '',
            resultType: (item.resultType || 'BOOLEAN') as QaInspectionResultType,
            standardText: item.standardText || '',
            standardUnit: item.standardUnit,
            standardPrecision: item.standardPrecision,
            lowerLimit: item.standardLowerLimit,
            upperLimit: item.standardUpperLimit,
            critical: item.critical === true,
            failureRule: item.failureRule || '',
            sourceNote: createCommonRegulationItemSourceNote(document, item),
            sourceNoteRaw: item.sourceNote || '',
            sourceOriginalPage: item.sourceOriginalPage,
            sourceOriginalItem: item.sourceOriginalItem,
            sourceOriginalExcerpt: item.sourceOriginalExcerpt,
            sourceOriginalMethod: item.sourceOriginalMethod,
            equipmentOptions: (item.equipmentOptions || []).map((equipment) => ({ ...equipment })),
            ...createQaItemInspectionState(item),
            sourceDocumentTitle: formatCommonRegulationSetDocumentTitle(document),
            sourceFileName: document.sourceFileName,
            memberRole: document.memberRole || '',
            commonDccProjectCodeId: document.commonDccProjectCodeId,
            commonRegulationId: document.commonRegulationId,
            commonRegulationVersionId: document.commonRegulationVersionId,
            commonRegulationCode: document.commonRegulationCode,
            commonRegulationName: document.commonRegulationName,
            commonVersionNo: document.versionNo,
            memberFinalInspectionApplicable: requireCommonMemberFinalInspectionApplicable(document),
            memberFinalInspectionNotApplicableReason:
              document.finalInspectionNotApplicableReason || '',
            memberInspectionTypeRules: resolveCommonMemberInspectionRules(document)
          }))
      )
  )

const commonRegulationItems = ref<CommonRegulationItem[]>([])

const resolveCommonItemApplicableTypes = (item: CommonRegulationItem) =>
  resolveQaItemDisplayInspectionTypes(item, item.memberFinalInspectionApplicable)

const commonRegulationItemsSummaryText = computed(() => {
  const version = selectedCommonRegulationSetVersionPreview.value
  if (!version) {
    return '未选择版本'
  }
  return `${version.versionNo} · ${selectedCommonRegulationSetVersionDocuments.value.length} 份 Word 规程 · ${commonRegulationItems.value.length} 个检验项目`
})

const commonRegulationItemsEmptyText = computed(() =>
  selectedCommonRegulationSetVersionPreview.value
    ? '该套版本暂无检验项目'
    : '请选择通用规程套和版本'
)

const pagedCommonRegulationItems = computed(() =>
  paginateQaRows(commonRegulationItems.value, commonRegulationItemsQuery)
)

const commonRegulationSetCurrentVersion = (set: QaCommonRegulationSetVO) =>
  set.versions?.find((version) => Number(version.id) === Number(set.currentVersionId))

const commonRegulationSetMemberCount = (set: QaCommonRegulationSetVO) =>
  commonRegulationSetCurrentVersion(set)?.members?.length || 0

const commonRegulationOverviewComposeText = computed(() => {
  const documentCount = selectedCommonRegulationSetVersionDocuments.value.length
  const itemCount = commonRegulationItems.value.length
  return `${documentCount} 份 Word 规程 + ${itemCount} 个检验项目`
})

const commonRegulationOverviewVersionText = computed(() => {
  const version = selectedCommonRegulationSetVersionPreview.value
  if (!version) {
    return '未选择版本'
  }
  return `${version.versionNo} / ${resolveQaRegulationLifecycleStatusText(version.lifecycleStatus)}`
})

const commonRegulationOverviewDetailText = computed(() => {
  const publishedVersion =
    commonRegulationCurrentPublishedVersion.value?.versionNo || '暂无已发布版本'
  const selectedVersion = selectedCommonRegulationSetVersionPreview.value?.versionNo || '未选择版本'
  return `当前发布版本 ${publishedVersion}；所选版本 ${selectedVersion}；${commonRegulationOverviewComposeText.value}`
})

const pagedCommonRegulationSets = computed(() =>
  paginateQaRows(commonRegulationSets.value, commonRegulationSetListQuery)
)

const commonRegulationSetSummaryText = computed(() => {
  const publishedVersionCount = commonRegulationSets.value.reduce(
    (count, set) =>
      count +
      (set.versions || []).filter((version) => version.lifecycleStatus === 'PUBLISHED').length,
    0
  )
  return `共 ${commonRegulationSets.value.length} 套 · ${publishedVersionCount} 个已发布版本`
})

const handleCommonRegulationSetPagination = () => {
  keepQaLocalPageInRange(commonRegulationSetListQuery, commonRegulationSets.value.length)
}

const selectCommonRegulationSet = (set: QaCommonRegulationSetVO) => {
  selectedCommonRegulationSetId.value = set.id
  selectedCommonRegulationSetVersionPreviewId.value = set.currentVersionId || set.versions?.[0]?.id
}

const handleCommonRegulationSetSwitch = (setId?: number) => {
  const set = commonRegulationSets.value.find((candidate) => Number(candidate.id) === Number(setId))
  if (!set) {
    selectedCommonRegulationSetVersionPreviewId.value = undefined
    return
  }
  selectCommonRegulationSet(set)
}

const handleCommonRegulationVersionChange = (versionId?: number) => {
  const version = selectedCommonRegulationSet.value?.versions.find(
    (candidate) => Number(candidate.id) === Number(versionId)
  )
  if (version) {
    selectCommonRegulationSetVersion(version)
  }
}

const copyCommonRegulationSetCode = async () => {
  if (!selectedCommonRegulationSet.value) {
    ElMessage.warning('请先选择通用规程套')
    return
  }
  await copyQaProjectSelectionToClipboard(selectedCommonRegulationSet.value.setCode)
  ElMessage.success('通用规程套编号已复制')
}

const selectCommonRegulationSetVersion = (version: QaCommonRegulationSetVO['versions'][number]) => {
  selectedCommonRegulationSetVersionPreviewId.value = version.id
}

const canEditCommonSetVersion = (version?: QaCommonRegulationSetVO['versions'][number]) =>
  version?.lifecycleStatus === 'DRAFT'

const openSelectedCommonRegulationSetVersionDialog = () => {
  if (!selectedCommonRegulationSet.value || !selectedCommonRegulationSetVersionPreview.value) {
    return
  }
  openCommonRegulationSetVersionDialog(
    selectedCommonRegulationSet.value,
    selectedCommonRegulationSetVersionPreview.value
  )
}

const synchronizeCommonRegulationSetSelection = (sets: QaCommonRegulationSetVO[]) => {
  handleCommonRegulationSetPagination()
  const selectedSet = sets.find(
    (set) => Number(set.id) === Number(selectedCommonRegulationSetId.value)
  )
  if (!selectedSet) {
    if (sets[0]) {
      selectCommonRegulationSet(sets[0])
    } else {
      selectedCommonRegulationSetId.value = undefined
      selectedCommonRegulationSetVersionPreviewId.value = undefined
    }
    return
  }
  if (
    !selectedSet.versions.some(
      (version) => Number(version.id) === Number(selectedCommonRegulationSetVersionPreviewId.value)
    )
  ) {
    selectedCommonRegulationSetVersionPreviewId.value =
      selectedSet.currentVersionId || selectedSet.versions[0]?.id
  }
}

const selectedCommonRegulationSetVersionOption = computed(() =>
  commonRegulationSetVersionOptions.value.find(
    (option) =>
      Number(option.commonRegulationSetVersionId) ===
      Number(selectedCommonRegulationSetVersionId.value)
  )
)

const commonRegulationSetDisplayText = computed(() =>
  currentCommonRegulationBinding.value?.commonRegulationSetId
    ? [
        currentCommonRegulationBinding.value.commonRegulationSetName,
        currentCommonRegulationBinding.value.commonRegulationSetCode,
        currentCommonRegulationBinding.value.commonRegulationSetVersionNo
      ]
        .filter(Boolean)
        .join(' / ')
    : selectedCommonRegulationSetVersionOption.value
      ? formatCommonRegulationSetVersionOption(selectedCommonRegulationSetVersionOption.value)
      : '未绑定通用规程套'
)

const QA_REGULATION_LIFECYCLE_STATUS_LABELS: Record<string, string> = {
  PUBLISHED: '已发布',
  RETIRED: '已作废',
  DRAFT: '草稿'
}

const selectedQaRegulationVersionOption = computed(() =>
  qaRegulationVersionOptions.value.find(
    (version) => Number(version.versionId) === Number(selectedQaRegulationVersionId.value)
  )
)

const selectedQaRegulationVersionStatus = computed(
  () =>
    selectedQaRegulationVersionOption.value?.lifecycleStatus || qaRegulationDraft.lifecycleStatus
)

const resolveQaRegulationLifecycleStatusText = (status?: string) => {
  if (!status) {
    return '未配置'
  }
  const label = QA_REGULATION_LIFECYCLE_STATUS_LABELS[status]
  if (!label) {
    throw new Error('未知 QA 规程版本状态：' + status)
  }
  return label
}

const resolveQaRegulationLifecycleStatusTagType = (status?: string) => {
  if (status === 'PUBLISHED') {
    return 'success'
  }
  if (status === 'RETIRED') {
    return 'danger'
  }
  if (status === 'DRAFT') {
    return 'warning'
  }
  return 'info'
}

const qaSelectedVersionStatusText = computed(() => {
  if (!selectedDccProjectCode.value) {
    return '未选择项目'
  }
  if (selectedQaRegulationVersionStatus.value) {
    return resolveQaRegulationLifecycleStatusText(selectedQaRegulationVersionStatus.value)
  }
  if (qaCurrentConfigurationLoading.value || qaRegulationVersionOptionsLoading.value) {
    return '加载中'
  }
  if (qaCurrentConfigurationLoadError.value || qaRegulationVersionOptionsLoadError.value) {
    return '加载失败'
  }
  if (!qaConfigurationExists.value) {
    return '未配置'
  }
  return resolveQaRegulationLifecycleStatusText(selectedQaRegulationVersionStatus.value)
})

const qaRegulationItemsEmptyText = computed(() =>
  qaConfigurationExists.value ? '暂无检验项目' : '当前 DCC 项目未配置 QA 规程'
)

const resolveDccProjectCodeErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message.trim()
  }
  return String(error)
}

const formatDccProjectCodeOption = (project: DccProjectCodeRespVO) =>
  [project.projectCode, project.projectName, project.docControlNo].filter(Boolean).join(' / ')

const formatQaRegulationVersionOption = (version: QaInspectionRegulationVersionOptionVO) => {
  const suffix = version.currentPublished ? ' / 当前发布' : ''
  return `${version.versionNo} / ${resolveQaRegulationLifecycleStatusText(version.lifecycleStatus)}${suffix}`
}

const formatCommonRegulationVersionOption = (option: QaCommonRegulationVersionOptionVO) =>
  [
    option.commonRegulationName,
    option.commonRegulationCode,
    option.versionNo,
    resolveQaRegulationLifecycleStatusText(option.lifecycleStatus)
  ]
    .filter(Boolean)
    .join(' / ')

const formatCommonRegulationSetVersionOption = (option: QaCommonRegulationSetVersionOptionVO) =>
  [
    option.commonRegulationSetName,
    option.commonRegulationSetCode,
    option.versionNo,
    `${option.memberCount} 个规程`
  ]
    .filter(Boolean)
    .join(' / ')

const incrementQaRegulationVersionNo = (versionNo: string) => {
  const normalizedVersionNo = versionNo.trim()
  const match = normalizedVersionNo.match(/^(.*?)(\d+)$/)
  if (!match) {
    throw new Error('已发布版本不能原地保存，请先使用带数字结尾的新版本号')
  }
  const prefix = match[1]
  const numericPart = match[2]
  const usedVersionNos = new Set(
    qaRegulationVersionOptions.value.map((version) => version.versionNo.trim()).filter(Boolean)
  )
  let nextNumber = Number(numericPart)
  let nextVersionNo = normalizedVersionNo
  do {
    nextNumber += 1
    if (!Number.isSafeInteger(nextNumber)) {
      throw new Error('无法生成下一个 QA 规程版本号')
    }
    nextVersionNo = prefix + String(nextNumber).padStart(numericPart.length, '0')
  } while (usedVersionNos.has(nextVersionNo))
  return nextVersionNo
}

const resolveQaRegulationDraftVersionNoForSave = () => {
  if (selectedQaRegulationVersionOption.value?.lifecycleStatus !== 'PUBLISHED') {
    return qaRegulationDraft.versionNo
  }
  return incrementQaRegulationVersionNo(qaRegulationDraft.versionNo)
}

const incrementVersionNo = (versionNo: string, usedVersionNos: Set<string>, targetName: string) => {
  const normalizedVersionNo = versionNo.trim()
  const match = normalizedVersionNo.match(/^(.*?)(\d+)$/)
  if (!match) {
    throw new Error(`${targetName}不能自动升版，请使用带数字结尾的版本号`)
  }
  const prefix = match[1]
  const numericPart = match[2]
  let nextNumber = Number(numericPart)
  let nextVersionNo = normalizedVersionNo
  do {
    nextNumber += 1
    if (!Number.isSafeInteger(nextNumber)) {
      throw new Error(`无法生成下一个${targetName}版本号`)
    }
    nextVersionNo = prefix + String(nextNumber).padStart(numericPart.length, '0')
  } while (usedVersionNos.has(nextVersionNo))
  return nextVersionNo
}

const resolveCommonSetNextVersionNo = () => {
  const version = selectedCommonRegulationSetVersionPreview.value
  const set = selectedCommonRegulationSet.value
  if (!version || !set) {
    throw new Error('请先选择通用规程套版本')
  }
  return incrementVersionNo(
    version.versionNo,
    new Set((set.versions || []).map((candidate) => candidate.versionNo.trim()).filter(Boolean)),
    '通用规程套'
  )
}

const resolveCommonMemberNextVersionNo = (member: QaCommonRegulationSetMemberVO) =>
  incrementVersionNo(
    member.versionNo,
    new Set(
      commonRegulationVersionOptions.value
        .filter((option) => Number(option.commonRegulationId) === Number(member.commonRegulationId))
        .map((option) => option.versionNo.trim())
        .filter(Boolean)
    ),
    member.commonRegulationName || '通用检验规程'
  )

const shouldLoadQaEquipmentBindingsForSelectedVersion = (
  configuration: QaInspectionRegulationPublishedVersionVO
) => {
  const selectedOption = qaRegulationVersionOptions.value.find(
    (version) => Number(version.versionId) === Number(configuration.publishedVersionId)
  )
  return selectedOption?.currentPublished === true || configuration.lifecycleStatus === 'DRAFT'
}

const resolvePositiveId = (value: unknown, label: string) => {
  const id = Number(value)
  if (!Number.isSafeInteger(id) || id <= 0) {
    throw new Error(label + '不能为空')
  }
  return id
}

const resolveRequiredText = (value: unknown, label: string) => {
  const text = String(value ?? '').trim()
  if (!text) {
    throw new Error(label + '不能为空')
  }
  return text
}

const replaceQaInspectionTypeRules = (rules: QaInspectionRegulationInspectionTypeRuleVO[]) => {
  const normalized = rules.map((rule) => ({
    ...rule,
    key: rule.key as QaInspectionTypeValue,
    required: rule.key === 'FINAL' ? Boolean(rule.required) : true
  }))
  qaInspectionTypeRules.splice(0, qaInspectionTypeRules.length, ...normalized)
}

const flattenQaRegulationProcesses = (
  processes: QaInspectionRegulationProcessVO[]
): QaRegulationItem[] =>
  [...processes]
    .sort((left, right) => Number(left.sort) - Number(right.sort))
    .flatMap((process) =>
      [...process.items]
        .sort((left, right) => Number(left.itemSort) - Number(right.itemSort))
        .map((item) => ({
          qaProcessId: process.qaProcessId,
          processCode: process.processCode,
          processName: process.processName,
          processSort: process.sort,
          itemSort: item.itemSort,
          itemCode: item.itemCode,
          itemName: item.itemName,
          inspectionMethod: item.inspectionMethod,
          inspectionTool: item.inspectionTool,
          samplingPlanText: item.samplingPlanText,
          resultType: item.resultType as QaInspectionResultType,
          standardText: item.standardText,
          standardUnit: item.standardUnit,
          standardPrecision: item.standardPrecision,
          lowerLimit: item.standardLowerLimit,
          upperLimit: item.standardUpperLimit,
          critical: Boolean(item.critical),
          failureRule: item.failureRule || '',
          sourceNote: item.sourceNote || '',
          sourceOriginalPage: item.sourceOriginalPage,
          sourceOriginalItem: item.sourceOriginalItem,
          sourceOriginalExcerpt: item.sourceOriginalExcerpt,
          sourceOriginalMethod: item.sourceOriginalMethod,
          equipmentOptions: [],
          ...createQaItemInspectionState(item)
        }))
    )

const resetQaRegulationConfiguration = (dccProjectCodeId?: number) => {
  Object.assign(qaRegulationDraft, createEmptyQaRegulationDraft(), { dccProjectCodeId })
  qaInspectionTypeRules.splice(
    0,
    qaInspectionTypeRules.length,
    ...createEmptyQaInspectionTypeRules()
  )
  qaRegulationItems.value = []
  qaEquipmentBindingLoadError.value = ''
  qaConfigurationExists.value = false
  qaPublishedVersionNo.value = ''
  selectedQaRegulationVersionId.value = undefined
  qaItemsQuery.pageNo = 1
}

const applyQaRegulationConfiguration = (
  configuration: QaInspectionRegulationPublishedVersionVO
) => {
  if (configuration.dccProjectCodeId !== qaRegulationDraft.dccProjectCodeId) {
    throw new Error('后端 QA 规程与当前 DCC 项目代码不一致')
  }
  Object.assign(qaRegulationDraft, {
    regulationId: configuration.regulationId,
    regulationCode: configuration.regulationCode,
    regulationName: configuration.regulationName,
    versionNo: configuration.versionNo,
    effectiveDate: configuration.effectiveDate || '',
    lifecycleStatus: configuration.lifecycleStatus
  })
  replaceQaInspectionTypeRules(configuration.inspectionTypeRules)
  qaRegulationItems.value = flattenQaRegulationProcesses(configuration.processes)
  qaConfigurationExists.value = true
  qaPublishedVersionNo.value =
    configuration.lifecycleStatus === 'PUBLISHED' ? configuration.versionNo : ''
  selectedQaRegulationVersionId.value = configuration.publishedVersionId
  qaItemsQuery.pageNo = 1
}

const toQaEquipmentOptions = (
  config: PqcItemEquipmentConfigVO
): QaInspectionRegulationItemEquipmentVO[] =>
  (config.equipmentGroups || [])
    .filter((group) => group.enabled !== false)
    .flatMap((group) =>
      (group.equipmentNumbers || [])
        .filter((number) => number.enabled !== false)
        .map((number, index) => ({
          equipmentId: group.equipmentId,
          equipmentCode: group.equipmentCode || '',
          equipmentName: group.equipmentName || '',
          equipmentNumber: number.equipmentNumber,
          defaultFlag: group.defaultFlag === true && index === 0,
          sort: group.sort ?? index + 1
        }))
    )

const getQaItemNameGroup = (row: QaRegulationItem) => {
  const itemName = row.itemName.trim()
  return qaRegulationItems.value.filter(
    (candidate) => candidate.itemName.trim() === itemName && candidate.itemCode.trim()
  )
}

const applyQaEquipmentOptionsToItemNameGroup = (
  row: QaRegulationItem,
  options: QaInspectionRegulationItemEquipmentVO[]
) => {
  getQaItemNameGroup(row).forEach((candidate) => {
    candidate.equipmentOptions = options.map((option) => ({ ...option }))
  })
}

const loadQaEquipmentBindings = async (dccProjectCodeId: number) => {
  const groups = new Map<string, QaRegulationItem[]>()
  qaRegulationItems.value.forEach((row) => {
    const key = row.itemName.trim()
    if (!key || !row.itemCode.trim()) {
      return
    }
    groups.set(key, [...(groups.get(key) || []), row])
  })
  qaEquipmentBindingLoadError.value = ''
  try {
    await Promise.all(
      Array.from(groups.values()).map(async (rows) => {
        const config = await QcTemplateApi.getPqcItemEquipmentConfigBatch(
          dccProjectCodeId,
          rows.map((row) => row.itemCode.trim())
        )
        applyQaEquipmentOptionsToItemNameGroup(rows[0], toQaEquipmentOptions(config))
      })
    )
  } catch (error) {
    qaEquipmentBindingLoadError.value =
      '检验设备配置加载失败：' + resolveDccProjectCodeErrorMessage(error)
    throw error
  }
}

const createQaRegulationProjectStatusMap = (statuses: QaInspectionRegulationProjectStatusVO[]) => {
  const statusByDccId = new Map<number, QaInspectionRegulationProjectStatusVO>()
  statuses.forEach((status) => {
    const dccProjectCodeId = Number(status.dccProjectCodeId)
    if (Number.isSafeInteger(dccProjectCodeId) && dccProjectCodeId > 0) {
      statusByDccId.set(dccProjectCodeId, status)
    }
  })
  return statusByDccId
}

const isDccProjectCodeConfigured = (project: DccProjectCodeRespVO) =>
  qaRegulationProjectStatusByDccId.value.get(Number(project.id))?.configured === true

const getDccProjectCodeOptionClass = (project: DccProjectCodeRespVO) => ({
  'qa-regulation-page__project-option': true,
  'qa-regulation-page__project-option--configured': isDccProjectCodeConfigured(project)
})

const sortDccProjectCodeOptionsByQaStatus = (projects: DccProjectCodeRespVO[]) =>
  [...projects].sort((left, right) => {
    const configuredDifference =
      Number(isDccProjectCodeConfigured(right)) - Number(isDccProjectCodeConfigured(left))
    return configuredDifference || Number(left.id) - Number(right.id)
  })

const mergeDccProjectCodeOptions = (projects: DccProjectCodeRespVO[]) => {
  const projectById = new Map<number, DccProjectCodeRespVO>()
  projects.forEach((project) => projectById.set(Number(project.id), project))
  return Array.from(projectById.values())
}

const loadCompleteDccProjectCodeOptions = async (keyword: string) => {
  const options: DccProjectCodeRespVO[] = []
  let pageNo = 1
  while (true) {
    const data = await getProjectCodePage({
      pageNo,
      pageSize: DCC_PROJECT_CODE_PAGE_SIZE,
      status: DCC_PROJECT_CODE_STATUS_ENABLE,
      keyword: keyword || undefined
    })
    options.push(...data.list)
    const total = Number(data.total)
    if (!Number.isFinite(total) || total < 0) {
      throw new Error('DCC 项目代码分页总数缺失')
    }
    if (options.length >= total || data.list.length === 0) {
      return mergeDccProjectCodeOptions(options)
    }
    pageNo += 1
  }
}

const loadDccProjectCodeOptions = async (keyword = '') => {
  dccProjectCodeOptionsLoading.value = true
  dccProjectCodeLoadError.value = ''
  try {
    const options = await loadCompleteDccProjectCodeOptions(keyword.trim())
    const mergedOptions = mergeDccProjectCodeOptions([
      ...options,
      ...(selectedDccProjectCode.value ? [selectedDccProjectCode.value] : [])
    ])
    const projectStatuses = await QcTemplateApi.getQaRegulationProjectStatuses(
      mergedOptions.map((project) => resolvePositiveId(project.id, 'DCC 项目代码 ID'))
    )
    qaRegulationProjectStatusByDccId.value = createQaRegulationProjectStatusMap(projectStatuses)
    dccProjectCodeOptions.value = sortDccProjectCodeOptionsByQaStatus(mergedOptions)
  } catch (error) {
    dccProjectCodeLoadError.value =
      'DCC 项目代码加载失败：' + resolveDccProjectCodeErrorMessage(error)
    throw error
  } finally {
    dccProjectCodeOptionsLoading.value = false
  }
}

const mergeQaMachineryOptions = (machinery: DvMachineryVO[]) => {
  const machineryById = new Map<number, DvMachineryVO>()
  machinery.forEach((row) => machineryById.set(Number(row.id), row))
  return Array.from(machineryById.values())
}

const loadQaMachineryOptions = async () => {
  qaMachineryOptionsLoading.value = true
  qaMachineryOptionsLoadError.value = ''
  try {
    const options: DvMachineryVO[] = []
    let pageNo = 1
    while (true) {
      const page = await DvMachineryApi.getMachineryPage({
        pageNo,
        pageSize: QA_MACHINERY_PAGE_SIZE
      })
      if (!Array.isArray(page?.list)) {
        throw new Error('设备台账分页数据缺失')
      }
      options.push(...page.list)
      const total = Number(page.total)
      if (!Number.isFinite(total) || total < 0) {
        throw new Error('设备台账分页总数缺失')
      }
      if (options.length >= total || page.list.length === 0) {
        qaMachineryOptions.value = mergeQaMachineryOptions(options)
        return
      }
      pageNo += 1
    }
  } catch (error) {
    qaMachineryOptionsLoadError.value =
      '设备台账加载失败：' + resolveDccProjectCodeErrorMessage(error)
    throw error
  } finally {
    qaMachineryOptionsLoading.value = false
  }
}

const formatQaMachineryLabel = (machinery: DvMachineryVO) =>
  [machinery.name, machinery.code].filter(Boolean).join(' / ')

const getAvailableQaMachinery = (row: QaRegulationItem, equipmentIndex: number) => {
  const selectedIds = new Set(
    row.equipmentOptions
      .filter((_equipment, index) => index !== equipmentIndex)
      .map((equipment) => Number(equipment.equipmentId))
      .filter((equipmentId) => equipmentId > 0)
  )
  const currentId = Number(row.equipmentOptions[equipmentIndex]?.equipmentId)
  return qaMachineryOptions.value.filter(
    (machinery) => Number(machinery.id) === currentId || !selectedIds.has(Number(machinery.id))
  )
}

const addQaItemEquipment = (row: QaRegulationItem) => {
  if (qaMachineryOptions.value.length === 0) {
    throw new Error('设备台账为空，不能添加检验设备')
  }
  const selectedIds = new Set(
    row.equipmentOptions.map((equipment) => Number(equipment.equipmentId))
  )
  const firstAvailable = qaMachineryOptions.value.find(
    (machinery) => !selectedIds.has(Number(machinery.id))
  )
  if (!firstAvailable) {
    ElMessage.warning('该检验项目已绑定全部设备台账设备')
    return
  }
  row.equipmentOptions.push({
    equipmentId: firstAvailable.id,
    equipmentCode: firstAvailable.code,
    equipmentName: firstAvailable.name,
    equipmentNumber: firstAvailable.code,
    defaultFlag: row.equipmentOptions.length === 0,
    sort: row.equipmentOptions.length + 1
  })
  return autoSaveQaItemEquipment(row)
}

const removeQaItemEquipment = (row: QaRegulationItem, equipmentIndex: number) => {
  row.equipmentOptions.splice(equipmentIndex, 1)
  row.equipmentOptions.forEach((equipment, index) => {
    equipment.sort = index + 1
  })
  return autoSaveQaItemEquipment(row)
}

const handleQaItemEquipmentChange = (
  row: QaRegulationItem,
  equipmentIndex: number,
  equipmentId: number
) => {
  const machinery = qaMachineryOptions.value.find(
    (candidate) => Number(candidate.id) === Number(equipmentId)
  )
  if (!machinery) {
    throw new Error('所选设备台账记录不存在')
  }
  const equipment = row.equipmentOptions[equipmentIndex]
  if (!equipment) {
    throw new Error('检验项目设备绑定行不存在')
  }
  const duplicate = row.equipmentOptions.some(
    (candidate, index) =>
      index !== equipmentIndex && Number(candidate.equipmentId) === Number(machinery.id)
  )
  if (duplicate) {
    throw new Error('同一检验项目不能重复绑定同一台设备')
  }
  equipment.equipmentId = machinery.id
  equipment.equipmentCode = machinery.code
  equipment.equipmentName = machinery.name
  equipment.equipmentNumber = machinery.code
  return autoSaveQaItemEquipment(row)
}

const addCommonRegulationItemEquipment = (row: CommonRegulationItem) => {
  if (qaMachineryOptions.value.length === 0) {
    throw new Error('设备台账为空，不能添加检验设备')
  }
  const selectedIds = new Set(
    row.equipmentOptions.map((equipment) => Number(equipment.equipmentId))
  )
  const firstAvailable = qaMachineryOptions.value.find(
    (machinery) => !selectedIds.has(Number(machinery.id))
  )
  if (!firstAvailable) {
    ElMessage.warning('该检验项目已绑定全部设备台账设备')
    return
  }
  row.equipmentOptions.push({
    equipmentId: firstAvailable.id,
    equipmentCode: firstAvailable.code,
    equipmentName: firstAvailable.name,
    equipmentNumber: firstAvailable.code,
    defaultFlag: row.equipmentOptions.length === 0,
    sort: row.equipmentOptions.length + 1
  })
}

const removeCommonRegulationItemEquipment = (
  row: CommonRegulationItem,
  equipmentIndex: number
) => {
  row.equipmentOptions.splice(equipmentIndex, 1)
  row.equipmentOptions.forEach((equipment, index) => {
    equipment.sort = index + 1
    equipment.defaultFlag = index === 0
  })
}

const handleCommonRegulationItemEquipmentChange = (
  row: CommonRegulationItem,
  equipmentIndex: number,
  equipmentId: number
) => {
  const machinery = qaMachineryOptions.value.find(
    (candidate) => Number(candidate.id) === Number(equipmentId)
  )
  if (!machinery) {
    throw new Error('所选设备台账记录不存在')
  }
  const duplicate = row.equipmentOptions.some(
    (candidate, index) =>
      index !== equipmentIndex && Number(candidate.equipmentId) === Number(machinery.id)
  )
  if (duplicate) {
    throw new Error('同一检验项目不能重复绑定同一台设备')
  }
  const equipment = row.equipmentOptions[equipmentIndex]
  if (!equipment) {
    throw new Error('检验项目设备绑定行不存在')
  }
  equipment.equipmentId = machinery.id
  equipment.equipmentCode = machinery.code
  equipment.equipmentName = machinery.name
  equipment.equipmentNumber = machinery.code
  equipment.defaultFlag = equipment.defaultFlag === true || equipmentIndex === 0
  equipment.sort = equipmentIndex + 1
}

const autoSaveQaItemEquipment = async (row: QaRegulationItem) => {
  const dccProjectCodeId = resolvePositiveId(qaRegulationDraft.dccProjectCodeId, 'DCC 项目代码 ID')
  const itemRows = getQaItemNameGroup(row)
  const itemCodes = itemRows.map((candidate) => candidate.itemCode.trim())
  if (!itemCodes.length) {
    throw new Error('检验项目编号不能为空')
  }
  qaEquipmentAutoSaving.value = true
  try {
    const saved = await QcTemplateApi.savePqcItemEquipmentConfigBatch({
      dccProjectCodeId,
      itemCode: itemCodes[0],
      itemCodes,
      itemNameSnapshot: row.itemName.trim(),
      equipmentGroups: row.equipmentOptions.map((equipment, index) => ({
        equipmentId: resolvePositiveId(equipment.equipmentId, row.itemName + '检验设备'),
        enabled: true,
        defaultFlag: equipment.defaultFlag === true || index === 0,
        sort: index + 1,
        equipmentNumbers: [
          {
            equipmentNumber: resolveRequiredText(
              equipment.equipmentNumber,
              row.itemName + '设备编号'
            ),
            enabled: true,
            sort: 0
          }
        ]
      }))
    })
    applyQaEquipmentOptionsToItemNameGroup(row, toQaEquipmentOptions(saved))
    ElMessage.success('检验设备配置已自动保存')
  } catch (error) {
    ElMessage.error('检验设备配置自动保存失败：' + resolveDccProjectCodeErrorMessage(error))
    throw error
  } finally {
    qaEquipmentAutoSaving.value = false
  }
}

const loadCurrentPublishedQaRegulationVersion = async (project?: DccProjectCodeRespVO) => {
  const loadSerial = ++qaCurrentPublishedVersionLoadSerial
  qaCurrentPublishedVersion.value = undefined
  qaCurrentPublishedVersionLoadError.value = ''
  qaCurrentPublishedVersionLoading.value = false
  if (!project) {
    return
  }
  const dccProjectCodeId = resolvePositiveId(project.id, 'DCC 项目代码 ID')
  const status = qaRegulationProjectStatusByDccId.value.get(dccProjectCodeId)
  if (status?.lifecycleStatus !== 'PUBLISHED') {
    return
  }
  qaCurrentPublishedVersionLoading.value = true
  try {
    const publishedVersion = await QcTemplateApi.getPublishedQaRegulationVersion(dccProjectCodeId)
    if (loadSerial !== qaCurrentPublishedVersionLoadSerial) {
      return
    }
    if (
      publishedVersion.dccProjectCodeId !== dccProjectCodeId ||
      publishedVersion.lifecycleStatus !== 'PUBLISHED'
    ) {
      throw new Error('已发布 QA 规程版本与当前 DCC 项目状态不一致')
    }
    qaCurrentPublishedVersion.value = publishedVersion
  } catch (error) {
    if (loadSerial === qaCurrentPublishedVersionLoadSerial) {
      qaCurrentPublishedVersionLoadError.value =
        '已发布版本加载失败：' + resolveDccProjectCodeErrorMessage(error)
    }
    throw error
  } finally {
    if (loadSerial === qaCurrentPublishedVersionLoadSerial) {
      qaCurrentPublishedVersionLoading.value = false
    }
  }
}

const loadQaRegulationVersionOptions = async (dccProjectCodeId: number) => {
  const loadSerial = ++qaRegulationVersionOptionsLoadSerial
  qaRegulationVersionOptions.value = []
  qaRegulationVersionOptionsLoadError.value = ''
  qaRegulationVersionOptionsLoading.value = true
  try {
    const versions = await QcTemplateApi.listQaRegulationVersions(dccProjectCodeId)
    if (loadSerial !== qaRegulationVersionOptionsLoadSerial) {
      return
    }
    qaRegulationVersionOptions.value = versions
    if (
      selectedQaRegulationVersionId.value &&
      !versions.some(
        (version) => Number(version.versionId) === Number(selectedQaRegulationVersionId.value)
      )
    ) {
      selectedQaRegulationVersionId.value = undefined
    }
  } catch (error) {
    if (loadSerial === qaRegulationVersionOptionsLoadSerial) {
      qaRegulationVersionOptionsLoadError.value =
        'QA 规程版本列表加载失败：' + resolveDccProjectCodeErrorMessage(error)
      dccProjectCodeLoadError.value = qaRegulationVersionOptionsLoadError.value
    }
    throw error
  } finally {
    if (loadSerial === qaRegulationVersionOptionsLoadSerial) {
      qaRegulationVersionOptionsLoading.value = false
    }
  }
}

const loadCommonRegulationVersionOptions = async () => {
  commonRegulationVersionOptionsLoading.value = true
  commonRegulationVersionOptionsLoadError.value = ''
  try {
    const [versionOptions, setVersionOptions, sets] = await Promise.all([
      QcTemplateApi.listCommonRegulationPublishedVersions(),
      QcTemplateApi.listCommonRegulationPublishedSetVersions(),
      QcTemplateApi.listCommonRegulationSets()
    ])
    commonRegulationVersionOptions.value = versionOptions
    commonRegulationSetVersionOptions.value = setVersionOptions
    commonRegulationSets.value = sets
    synchronizeCommonRegulationSetSelection(sets)
  } catch (error) {
    commonRegulationVersionOptionsLoadError.value =
      '通用检验规程套列表加载失败：' + resolveDccProjectCodeErrorMessage(error)
    throw error
  } finally {
    commonRegulationVersionOptionsLoading.value = false
  }
}

const loadCurrentCommonRegulationBinding = async (dccProjectCodeId: number) => {
  const loadSerial = ++commonRegulationBindingLoadSerial
  commonRegulationBindingLoading.value = true
  commonRegulationBindingLoadError.value = ''
  currentCommonRegulationBinding.value = undefined
  selectedCommonRegulationVersionId.value = undefined
  selectedCommonRegulationSetVersionId.value = undefined
  try {
    const binding = await QcTemplateApi.getCurrentCommonRegulationBinding(dccProjectCodeId)
    if (loadSerial !== commonRegulationBindingLoadSerial) {
      return
    }
    currentCommonRegulationBinding.value = binding || undefined
    selectedCommonRegulationVersionId.value = binding?.commonRegulationVersionId
    selectedCommonRegulationSetVersionId.value = binding?.commonRegulationSetVersionId
  } catch (error) {
    if (loadSerial === commonRegulationBindingLoadSerial) {
      commonRegulationBindingLoadError.value =
        '通用检验规程绑定加载失败：' + resolveDccProjectCodeErrorMessage(error)
    }
    throw error
  } finally {
    if (loadSerial === commonRegulationBindingLoadSerial) {
      commonRegulationBindingLoading.value = false
    }
  }
}

const handleBindCommonRegulationVersion = async () => {
  if (!selectedDccProjectCode.value) {
    ElMessage.warning('请先选择 DCC 项目代码')
    return
  }
  if (!selectedCommonRegulationSetVersionId.value) {
    ElMessage.warning('请选择已发布通用规程套版本')
    return
  }
  const dccProjectCodeId = resolvePositiveId(selectedDccProjectCode.value.id, 'DCC 项目代码 ID')
  commonRegulationBindingSaving.value = true
  try {
    const binding = await QcTemplateApi.bindCommonRegulationVersion({
      dccProjectCodeId,
      commonRegulationSetVersionId: selectedCommonRegulationSetVersionId.value,
      changeReason: 'QA 页面维护产品通用检验规程套绑定'
    })
    currentCommonRegulationBinding.value = binding
    selectedCommonRegulationVersionId.value = binding.commonRegulationVersionId
    selectedCommonRegulationSetVersionId.value = binding.commonRegulationSetVersionId
    ElMessage.success('通用规程套绑定已保存')
  } catch (error) {
    ElMessage.error('通用检验规程绑定保存失败：' + resolveDccProjectCodeErrorMessage(error))
  } finally {
    commonRegulationBindingSaving.value = false
  }
}

const handleUnbindCommonRegulation = async () => {
  if (!selectedDccProjectCode.value) {
    ElMessage.warning('请先选择 DCC 项目代码')
    return
  }
  try {
    await ElMessageBox.confirm(
      '解除后，新生成的一线 PQC 任务将不再拼接通用包装检验工序；已生成任务仍按各自快照执行。',
      '确认解除通用检验规程关联',
      { type: 'warning' }
    )
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    throw error
  }
  const dccProjectCodeId = resolvePositiveId(selectedDccProjectCode.value.id, 'DCC 项目代码 ID')
  commonRegulationBindingSaving.value = true
  try {
    await QcTemplateApi.unbindCommonRegulation(dccProjectCodeId)
    currentCommonRegulationBinding.value = undefined
    selectedCommonRegulationVersionId.value = undefined
    selectedCommonRegulationSetVersionId.value = undefined
    ElMessage.success('通用检验规程关联已解除')
  } catch (error) {
    ElMessage.error('通用检验规程解除关联失败：' + resolveDccProjectCodeErrorMessage(error))
  } finally {
    commonRegulationBindingSaving.value = false
  }
}

const viewCurrentCommonRegulation = () => {
  const binding = currentCommonRegulationBinding.value
  if (!binding) {
    ElMessage.warning('当前产品未关联通用检验规程')
    return
  }
  regulationWorkspaceTab.value = 'common'
  if (binding.commonRegulationSetId) {
    handleCommonRegulationSetSwitch(binding.commonRegulationSetId)
  }
}

const reloadCommonRegulationSets = async () => {
  commonRegulationSetLoading.value = true
  try {
    const [setVersionOptions, sets] = await Promise.all([
      QcTemplateApi.listCommonRegulationPublishedSetVersions(),
      QcTemplateApi.listCommonRegulationSets()
    ])
    commonRegulationSetVersionOptions.value = setVersionOptions
    commonRegulationSets.value = sets
    synchronizeCommonRegulationSetSelection(sets)
  } catch (error) {
    ElMessage.error('通用规程套加载失败：' + resolveDccProjectCodeErrorMessage(error))
    throw error
  } finally {
    commonRegulationSetLoading.value = false
  }
}

const openCommonRegulationSetDialog = (set?: QaCommonRegulationSetVO) => {
  commonRegulationSetForm.id = set?.id
  commonRegulationSetForm.setCode = set?.setCode || ''
  commonRegulationSetForm.setName = set?.setName || ''
  commonRegulationSetForm.setStatus = set?.setStatus || 'ENABLED'
  commonRegulationSetForm.remark = set?.remark || ''
  commonRegulationSetDialogVisible.value = true
}

const saveCommonRegulationSet = async () => {
  commonRegulationSetSaving.value = true
  try {
    const saved = await QcTemplateApi.saveCommonRegulationSet(commonRegulationSetForm)
    commonRegulationSetDialogVisible.value = false
    await reloadCommonRegulationSets()
    selectedCommonRegulationSetId.value = saved.id
    ElMessage.success('通用规程套已保存')
  } catch (error) {
    ElMessage.error('通用规程套保存失败：' + resolveDccProjectCodeErrorMessage(error))
  } finally {
    commonRegulationSetSaving.value = false
  }
}

const deleteCommonRegulationSet = async (set: QaCommonRegulationSetVO) => {
  if (!set.id) {
    throw new Error('通用规程套 ID 不能为空')
  }
  await ElMessageBox.confirm('删除套前必须先删除其下全部套版本。是否继续？', '删除通用规程套', {
    type: 'warning'
  })
  await QcTemplateApi.deleteCommonRegulationSet(set.id)
  await reloadCommonRegulationSets()
  ElMessage.success('通用规程套已删除')
}

const openCommonRegulationSetVersionDialog = (
  set: QaCommonRegulationSetVO,
  version?: QaCommonRegulationSetVO['versions'][number]
) => {
  if (!set.id) {
    throw new Error('通用规程套 ID 不能为空')
  }
  if (version && !canEditCommonSetVersion(version)) {
    ElMessage.warning('已发布通用规程套版本不可原地编辑，请新建草稿版本后发布')
    return
  }
  commonRegulationSetVersionForm.id = version?.id
  commonRegulationSetVersionForm.setId = set.id
  commonRegulationSetVersionForm.versionNo = version?.versionNo || ''
  commonRegulationSetVersionForm.lifecycleStatus =
    version?.lifecycleStatus === 'PUBLISHED' ? 'PUBLISHED' : 'DRAFT'
  commonRegulationSetVersionForm.effectiveDate = version?.effectiveDate || ''
  commonRegulationSetVersionForm.remark = version?.remark || ''
  commonRegulationSetVersionForm.members = version?.members?.length
    ? version.members.map((member, index) => ({
        commonRegulationVersionId: member.commonRegulationVersionId,
        sort: member.sort ?? (index + 1) * 10,
        memberRole: member.memberRole || '',
        remark: member.remark || ''
      }))
    : [{ commonRegulationVersionId: 0, sort: 10, memberRole: '', remark: '' }]
  commonRegulationSetVersionDialogVisible.value = true
}

const openCommonRegulationDraftDialog = () => {
  const set = selectedCommonRegulationSet.value
  if (!set) {
    ElMessage.warning('请先选择通用规程套')
    return
  }
  const selectedVersion = selectedCommonRegulationSetVersionPreview.value
  openCommonRegulationSetVersionDialog(
    set,
    selectedVersion?.lifecycleStatus === 'DRAFT' ? selectedVersion : undefined
  )
  commonRegulationSetVersionForm.lifecycleStatus = 'DRAFT'
}

const openCommonRegulationPublishDialog = () => {
  const set = selectedCommonRegulationSet.value
  const selectedVersion = selectedCommonRegulationSetVersionPreview.value
  if (!set || !selectedVersion || selectedVersion.lifecycleStatus !== 'DRAFT') {
    ElMessage.warning('请选择草稿版本后发布')
    return
  }
  openCommonRegulationSetVersionDialog(set, selectedVersion)
  commonRegulationSetVersionForm.lifecycleStatus = 'PUBLISHED'
}

const addCommonRegulationSetVersionMember = () => {
  commonRegulationSetVersionForm.members.push({
    commonRegulationVersionId: 0,
    sort: (commonRegulationSetVersionForm.members.length + 1) * 10,
    memberRole: '',
    remark: ''
  })
}

const removeCommonRegulationSetVersionMember = (index: number) => {
  commonRegulationSetVersionForm.members.splice(index, 1)
}

const saveCommonRegulationSetVersion = async () => {
  commonRegulationSetSaving.value = true
  try {
    const payload = {
      ...commonRegulationSetVersionForm,
      members: commonRegulationSetVersionForm.members.map((member) => ({
        ...member,
        commonRegulationVersionId: resolvePositiveId(
          member.commonRegulationVersionId,
          '通用检验规程版本'
        )
      }))
    }
    const saved = await QcTemplateApi.saveCommonRegulationSetVersion(payload)
    commonRegulationSetVersionDialogVisible.value = false
    await reloadCommonRegulationSets()
    selectedCommonRegulationSetId.value = saved.setId
    selectedCommonRegulationSetVersionPreviewId.value = saved.id
    ElMessage.success('通用规程套版本已保存')
  } catch (error) {
    ElMessage.error('通用规程套版本保存失败：' + resolveDccProjectCodeErrorMessage(error))
  } finally {
    commonRegulationSetSaving.value = false
  }
}

const saveCommonRegulationItemsVersion = async () => {
  let payload: QaCommonRegulationSetItemsUpgradeReqVO
  try {
    payload = buildCommonRegulationItemsUpgradePayload()
  } catch (error) {
    ElMessage.warning(resolveDccProjectCodeErrorMessage(error))
    return
  }
  commonRegulationItemsSaving.value = true
  try {
    const saved = await QcTemplateApi.upgradeCommonRegulationSetItems(payload)
    await loadCommonRegulationVersionOptions()
    selectedCommonRegulationSetId.value = saved.setId
    selectedCommonRegulationSetVersionPreviewId.value = saved.id
    ElMessage.success(`通用检验规程套已保存并升版为 ${saved.versionNo}`)
  } catch (error) {
    ElMessage.error('通用检验项目保存升版失败：' + resolveDccProjectCodeErrorMessage(error))
  } finally {
    commonRegulationItemsSaving.value = false
  }
}

const deleteCommonRegulationSetVersion = async (
  version: QaCommonRegulationSetVO['versions'][number]
) => {
  if (!version.id) {
    throw new Error('通用规程套版本 ID 不能为空')
  }
  if (!canEditCommonSetVersion(version)) {
    ElMessage.warning('非草稿通用规程套版本不可删除')
    return
  }
  await ElMessageBox.confirm('删除套版本后，不能再被产品绑定。是否继续？', '删除通用规程套版本', {
    type: 'warning'
  })
  await QcTemplateApi.deleteCommonRegulationSetVersion(version.id)
  await reloadCommonRegulationSets()
  ElMessage.success('通用规程套版本已删除')
}

const handleQaRegulationVersionChange = async (versionId?: number) => {
  if (!versionId || !selectedDccProjectCode.value) {
    return
  }
  const dccProjectCodeId = resolvePositiveId(selectedDccProjectCode.value.id, 'DCC 项目代码 ID')
  const selectedVersion = qaRegulationVersionOptions.value.find(
    (version) => Number(version.versionId) === Number(versionId)
  )
  if (!selectedVersion) {
    throw new Error('指定的 QA 规程版本不存在')
  }
  qaCurrentConfigurationLoading.value = true
  qaCurrentConfigurationLoadError.value = ''
  try {
    const configuration = await QcTemplateApi.getPublishedQaRegulationVersion(
      dccProjectCodeId,
      versionId
    )
    if (!configuration || Number(configuration.publishedVersionId) !== Number(versionId)) {
      throw new Error('后端返回的 QA 规程版本与所选版本不一致')
    }
    applyQaRegulationConfiguration(configuration)
    if (shouldLoadQaEquipmentBindingsForSelectedVersion(configuration)) {
      await loadQaEquipmentBindings(dccProjectCodeId)
    }
  } catch (error) {
    qaCurrentConfigurationLoadError.value =
      'QA 规程版本加载失败：' + resolveDccProjectCodeErrorMessage(error)
    ElMessage.error(qaCurrentConfigurationLoadError.value)
    throw error
  } finally {
    qaCurrentConfigurationLoading.value = false
  }
}

const loadCurrentQaRegulation = async (dccProjectCodeId: number) => {
  const loadSerial = ++qaCurrentConfigurationLoadSerial
  qaCurrentConfigurationLoading.value = true
  qaCurrentConfigurationLoadError.value = ''
  resetQaRegulationConfiguration(dccProjectCodeId)
  try {
    const configuration = await QcTemplateApi.getCurrentQaRegulation(dccProjectCodeId)
    if (loadSerial !== qaCurrentConfigurationLoadSerial) {
      return
    }
    if (configuration) {
      applyQaRegulationConfiguration(configuration)
      await loadQaEquipmentBindings(dccProjectCodeId)
    }
  } catch (error) {
    if (loadSerial === qaCurrentConfigurationLoadSerial) {
      qaCurrentConfigurationLoadError.value =
        'QA 规程加载失败：' + resolveDccProjectCodeErrorMessage(error)
      dccProjectCodeLoadError.value = qaCurrentConfigurationLoadError.value
    }
    throw error
  } finally {
    if (loadSerial === qaCurrentConfigurationLoadSerial) {
      qaCurrentConfigurationLoading.value = false
    }
  }
}

const persistLastDccProjectCodeSelection = (project?: DccProjectCodeRespVO) => {
  if (typeof window === 'undefined') {
    return
  }
  if (project) {
    window.localStorage.setItem(
      QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY,
      String(project.id)
    )
  } else {
    window.localStorage.removeItem(QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY)
  }
}

const readLastDccProjectCodeSelectionId = () => {
  if (typeof window === 'undefined') {
    return undefined
  }
  const rawId = window.localStorage.getItem(QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY)
  if (!rawId) {
    return undefined
  }
  return resolvePositiveId(rawId, '上次选择的 DCC 项目代码 ID')
}

const copySelectedDccProjectCode = async () => {
  if (!selectedDccProjectCodeLabel.value) {
    ElMessage.warning('请先选择 DCC 项目代码')
    return
  }
  await copyQaProjectSelectionToClipboard(selectedDccProjectCodeLabel.value)
  ElMessage.success('DCC 项目代码已复制')
}

const selectDccProjectCode = async (project?: DccProjectCodeRespVO) => {
  selectedDccProjectCode.value = project
  persistLastDccProjectCodeSelection(project)
  if (!project) {
    resetQaRegulationConfiguration()
    qaCurrentPublishedVersion.value = undefined
    qaRegulationVersionOptions.value = []
    qaRegulationVersionOptionsLoadError.value = ''
    currentCommonRegulationBinding.value = undefined
    selectedCommonRegulationVersionId.value = undefined
    selectedCommonRegulationSetVersionId.value = undefined
    commonRegulationBindingLoadError.value = ''
    return
  }
  const dccProjectCodeId = resolvePositiveId(project.id, 'DCC 项目代码 ID')
  qaRegulationDraft.dccProjectCodeId = dccProjectCodeId
  await Promise.all([
    loadCurrentQaRegulation(dccProjectCodeId),
    loadCurrentPublishedQaRegulationVersion(project),
    loadQaRegulationVersionOptions(dccProjectCodeId),
    loadCurrentCommonRegulationBinding(dccProjectCodeId)
  ])
}

const handleDccProjectCodeChange = async (projectId?: number) => {
  if (!projectId) {
    await selectDccProjectCode()
    return
  }
  const project =
    dccProjectCodeOptions.value.find((option) => Number(option.id) === Number(projectId)) ||
    (await getProjectCode(projectId))
  if (!project || project.status !== DCC_PROJECT_CODE_STATUS_ENABLE) {
    throw new Error('指定的 DCC 项目代码不存在或已停用')
  }
  dccProjectCodeOptions.value = mergeDccProjectCodeOptions([
    project,
    ...dccProjectCodeOptions.value
  ])
  await selectDccProjectCode(project)
}

const handleDccProjectCodeVisibleChange = (visible: boolean) => {
  if (visible && dccProjectCodeOptions.value.length === 0) {
    void loadDccProjectCodeOptions()
  }
}

const retryLoadDccProjectCodes = async () => {
  try {
    await loadDccProjectCodeOptions()
  } catch (error) {
    ElMessage.error(resolveDccProjectCodeErrorMessage(error))
  }
}

const resetQaWordImportDialog = () => {
  if (qaWordImportSubmitting.value) {
    return
  }
  qaWordImportFile.value = undefined
  qaWordImportFileList.value = []
  qaWordImportDccProjectCodeId.value = undefined
  qaWordImportOwnerModule.value = 'MES_QA'
}

const openQaWordImportDialog = () => {
  qaWordImportFile.value = undefined
  qaWordImportFileList.value = []
  qaWordImportDccProjectCodeId.value = qaRegulationDraft.dccProjectCodeId
  qaWordImportOwnerModule.value = 'MES_QA'
  qaWordImportDialogVisible.value = true
  if (dccProjectCodeOptions.value.length === 0) {
    void loadDccProjectCodeOptions().catch((error) => {
      ElMessage.error('DCC 项目代码加载失败：' + resolveDccProjectCodeErrorMessage(error))
    })
  }
}

const openCommonQaWordImportDialog = () => {
  qaWordImportFile.value = undefined
  qaWordImportFileList.value = []
  qaWordImportDccProjectCodeId.value = undefined
  qaWordImportOwnerModule.value = 'MES_QA_COMMON'
  qaWordImportDialogVisible.value = true
  if (dccProjectCodeOptions.value.length === 0) {
    void loadDccProjectCodeOptions().catch((error) => {
      ElMessage.error('DCC 项目代码加载失败：' + resolveDccProjectCodeErrorMessage(error))
    })
  }
}

const handleQaWordImportFileChange = (uploadFile: UploadFile) => {
  const rawFile = uploadFile.raw
  if (!rawFile || !uploadFile.name.toLowerCase().endsWith('.docx')) {
    qaWordImportFile.value = undefined
    qaWordImportFileList.value = []
    ElMessage.error('仅支持 .docx QA 模板文件')
    return
  }
  qaWordImportFile.value = rawFile
}

const handleQaWordImportFileRemove = () => {
  qaWordImportFile.value = undefined
}

const handleQaWordImportFileExceed = () => {
  ElMessage.warning('每次只能解析一个 QA 模板文件')
}

const handleQaWordImportProjectVisibleChange = (visible: boolean) => {
  if (visible && dccProjectCodeOptions.value.length === 0) {
    void loadDccProjectCodeOptions().catch((error) => {
      ElMessage.error('DCC 项目代码加载失败：' + resolveDccProjectCodeErrorMessage(error))
    })
  }
}

const submitQaWordImport = async () => {
  let savedResult: QaInspectionRegulationImportRespVO | undefined
  try {
    if (!qaWordImportFile.value) {
      throw new Error('请选择需要解析的 .docx QA 模板文件')
    }
    const dccProjectCodeId = resolvePositiveId(qaWordImportDccProjectCodeId.value, '绑定项目')
    const formData = new FormData()
    formData.append('file', qaWordImportFile.value)
    formData.append('dccProjectCodeId', String(dccProjectCodeId))

    qaWordImportSubmitting.value = true
    const importOwnerModule = qaWordImportOwnerModule.value
    const importCommonRegulation = importOwnerModule === 'MES_QA_COMMON'
    savedResult = await QcTemplateApi.importQaRegulationWordDraft(formData, {
      ownerModule: importOwnerModule,
      publishAfterImport: importCommonRegulation
    })
    const project =
      dccProjectCodeOptions.value.find((option) => Number(option.id) === dccProjectCodeId) ||
      (await getProjectCode(dccProjectCodeId))
    if (!project || project.status !== DCC_PROJECT_CODE_STATUS_ENABLE) {
      throw new Error('导入已保存，但绑定的 DCC 项目已停用或不存在')
    }
    dccProjectCodeOptions.value = mergeDccProjectCodeOptions([
      project,
      ...dccProjectCodeOptions.value
    ])
    if (importCommonRegulation) {
      await loadCommonRegulationVersionOptions()
      selectedCommonRegulationVersionId.value = savedResult.publishedVersionId
      regulationWorkspaceTab.value = 'common'
      qaWordImportDialogVisible.value = false
      const routeText = savedResult.route === 'CREATE' ? '新建' : '升版'
      ElMessage.success(
        `通用检验规程${routeText}并发布完成：${savedResult.versionNo}，` +
          `${savedResult.processCount} 个工序、${savedResult.itemCount} 个检验项目`
      )
      return
    }
    const statusMap = new Map(qaRegulationProjectStatusByDccId.value)
    const currentStatus = statusMap.get(dccProjectCodeId)
    statusMap.set(dccProjectCodeId, {
      dccProjectCodeId,
      configured: true,
      regulationCount: currentStatus?.regulationCount || 1,
      regulationId: savedResult.regulationId,
      currentVersionId: currentStatus?.currentVersionId,
      regulationCode: savedResult.regulationCode,
      regulationName: savedResult.regulationName,
      lifecycleStatus: currentStatus?.currentVersionId ? 'PUBLISHED' : 'DRAFT'
    })
    qaRegulationProjectStatusByDccId.value = statusMap
    await selectDccProjectCode(project)
    qaActiveTab.value = 'items'
    qaWordImportDialogVisible.value = false
    const routeText = savedResult.route === 'CREATE' ? '新建' : '升版'
    ElMessage.success(
      `QA 模板解析完成，${routeText}草稿 ${savedResult.versionNo} 已保存：` +
        `${savedResult.processCount} 个工序、${savedResult.itemCount} 个检验项目`
    )
  } catch (error) {
    const prefix = savedResult ? 'QA 草稿已保存，但页面刷新失败：' : 'QA 模板解析失败：'
    ElMessage.error(prefix + resolveDccProjectCodeErrorMessage(error))
    if (savedResult) {
      qaWordImportDialogVisible.value = false
    }
  } finally {
    qaWordImportSubmitting.value = false
  }
}

const resetQaRegulationForTesting = async () => {
  let resetCompleted = false
  try {
    const dccProjectCodeId = resolvePositiveId(
      qaRegulationDraft.dccProjectCodeId,
      'DCC 项目代码 ID'
    )
    const projectLabel = selectedDccProjectCodeLabel.value || String(dccProjectCodeId)
    await ElMessageBox.confirm(
      `将清空“${projectLabel}”当前 QA 规程草稿和已发布版本，仅用于测试阶段重新导入同版本模板。` +
        '如果该规程已被活跃订单或 PQC 检验任务引用，后端会拒绝重置。',
      '重置 QA 规程',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )
    qaRegulationResetting.value = true
    const result = await QcTemplateApi.resetQaRegulationForTesting(dccProjectCodeId)
    resetCompleted = true
    resetQaRegulationConfiguration(dccProjectCodeId)
    qaCurrentPublishedVersion.value = undefined
    qaCurrentPublishedVersionLoadError.value = ''
    qaCurrentPublishedVersionLoading.value = false
    qaActiveTab.value = 'overview'
    const statusMap = new Map(qaRegulationProjectStatusByDccId.value)
    statusMap.set(dccProjectCodeId, {
      dccProjectCodeId,
      configured: false,
      regulationCount: 0,
      lifecycleStatus: undefined
    })
    qaRegulationProjectStatusByDccId.value = statusMap
    if (selectedDccProjectCode.value) {
      await selectDccProjectCode(selectedDccProjectCode.value)
    }
    ElMessage.success(
      `测试重置完成：已清理 ${result.versionCount} 个版本、` +
        `${result.processCount} 个工序、${result.itemCount} 条检验项目`
    )
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    const prefix = resetCompleted ? 'QA 规程已重置，但页面刷新失败：' : 'QA 规程重置失败：'
    ElMessage.error(prefix + resolveDccProjectCodeErrorMessage(error))
  } finally {
    qaRegulationResetting.value = false
  }
}

const resolveInitialDccProjectCodeId = () => {
  const queryValue = Array.isArray(route.query.dccProjectCodeId)
    ? route.query.dccProjectCodeId[0]
    : route.query.dccProjectCodeId
  return queryValue
    ? resolvePositiveId(queryValue, '链接中的 DCC 项目代码 ID')
    : readLastDccProjectCodeSelectionId()
}

const initializeQaRegulationPage = async () => {
  await Promise.all([loadDccProjectCodeOptions(), loadQaMachineryOptions()])
  const projectId = resolveInitialDccProjectCodeId()
  if (projectId) {
    await handleDccProjectCodeChange(projectId)
  }
}

const initializeCommonRegulationWorkspace = async () => {
  await loadCommonRegulationVersionOptions()
}

onMounted(() => {
  void initializeQaRegulationPage().catch((error) => {
    const message = resolveDccProjectCodeErrorMessage(error)
    dccProjectCodeLoadError.value = message
    ElMessage.error(message)
  })
  void initializeCommonRegulationWorkspace().catch((error) => {
    ElMessage.error(resolveDccProjectCodeErrorMessage(error))
  })
})

const formatQaItemSamplingPlan = (item: QaRegulationItem) =>
  item.samplingPlanText?.trim() || '未填写抽样方案'

const handleQaFirstInspectionEnabledChange = (item: QaRegulationItem) => {
  if (!item.firstInspectionEnabled) {
    item.firstInspectionQuantity = undefined
  }
}

const handleQaPatrolInspectionEnabledChange = (item: QaRegulationItem) => {
  if (!item.patrolInspectionEnabled) {
    item.patrolInspectionRatio = undefined
  }
}

const addQaRegulationItem = () => {
  const nextSort = qaRegulationItems.value.length + 1
  qaRegulationItems.value.push({
    processCode: '',
    processName: '',
    processSort: nextSort,
    itemSort: 1,
    itemCode: '',
    itemName: '',
    inspectionMethod: '',
    inspectionTool: '',
    samplingPlanText: '',
    firstInspectionEnabled: false,
    firstInspectionQuantity: undefined,
    patrolInspectionEnabled: false,
    patrolInspectionRatio: undefined,
    resultType: 'BOOLEAN',
    standardText: '',
    critical: false,
    failureRule: '',
    sourceNote: '',
    equipmentOptions: []
  })
  qaItemsQuery.pageNo = Math.ceil(qaRegulationItems.value.length / qaItemsQuery.pageSize)
}

const removeQaRegulationItemByRow = (row: QaRegulationItem) => {
  const index = qaRegulationItems.value.indexOf(row)
  if (index >= 0) {
    qaRegulationItems.value.splice(index, 1)
    keepQaLocalPageInRange(qaItemsQuery, qaRegulationItems.value.length)
  }
}

const removeCommonRegulationItemByRow = (row: CommonRegulationItem) => {
  const index = commonRegulationItems.value.indexOf(row)
  if (index >= 0) {
    commonRegulationItems.value.splice(index, 1)
    keepQaLocalPageInRange(commonRegulationItemsQuery, commonRegulationItems.value.length)
  }
}

const buildQaRegulationSaveItem = (
  item: QaRegulationItem,
  itemSort: number
): QaInspectionRegulationSaveItemVO => {
  const itemName = resolveRequiredText(item.itemName, '检验项目名称')
  const inspectionConfiguration = resolveQaItemInspectionPayload(
    item,
    finalInspectionRequired.value,
    itemName
  )
  return {
    itemSort,
    itemCode: resolveRequiredText(item.itemCode, itemName + '编码'),
    itemName,
    inspectionMethod: resolveRequiredText(item.inspectionMethod, itemName + '检验方法'),
    inspectionTool: resolveRequiredText(item.inspectionTool, itemName + '检验器具及设备'),
    samplingPlanText: resolveRequiredText(item.samplingPlanText, itemName + '抽样方案'),
    standardText: resolveRequiredText(item.standardText, itemName + '接受标准'),
    standardLowerLimit: item.resultType === 'NUMERIC' ? item.lowerLimit : undefined,
    standardUpperLimit: item.resultType === 'NUMERIC' ? item.upperLimit : undefined,
    standardUnit: item.standardUnit,
    standardPrecision: item.standardPrecision,
    resultType: item.resultType,
    applicableInspectionTypes: inspectionConfiguration.applicableInspectionTypes,
    firstInspectionQuantity: inspectionConfiguration.firstInspectionQuantity,
    patrolInspectionRatio: inspectionConfiguration.patrolInspectionRatio,
    critical: item.critical,
    failureRule: item.failureRule.trim() || undefined,
    sourceNote: item.sourceNote.trim() || undefined,
    sourceOriginalPage: item.sourceOriginalPage,
    sourceOriginalItem: item.sourceOriginalItem?.trim() || undefined,
    sourceOriginalExcerpt: item.sourceOriginalExcerpt?.trim() || undefined,
    sourceOriginalMethod: item.sourceOriginalMethod?.trim() || undefined
  }
}

const buildQaRegulationProcesses = (): QaInspectionRegulationSaveProcessVO[] => {
  const groups = new Map<
    string,
    { code: string; name: string; sort: number; items: QaRegulationItem[] }
  >()
  qaRegulationItems.value.forEach((item, index) => {
    const code = resolveRequiredText(item.processCode, '第 ' + (index + 1) + ' 行 QA 工序编码')
    const name = resolveRequiredText(item.processName, '第 ' + (index + 1) + ' 行 QA 工序名称')
    const existing = groups.get(code)
    if (existing && existing.name !== name) {
      throw new Error('QA 工序编码 ' + code + ' 对应了多个工序名称')
    }
    const group = existing || {
      code,
      name,
      sort: item.processSort || groups.size + 1,
      items: []
    }
    group.items.push(item)
    groups.set(code, group)
  })
  if (groups.size === 0) {
    throw new Error('至少需要一个 QA 工序和检验项目')
  }
  return Array.from(groups.values())
    .sort((left, right) => left.sort - right.sort)
    .map((group, processIndex) => ({
      processCode: group.code,
      processName: group.name,
      sort: processIndex + 1,
    items: group.items.map((item, itemIndex) => buildQaRegulationSaveItem(item, itemIndex + 1))
  }))
}

const buildCommonRegulationItemEquipmentOptions = (
  item: CommonRegulationItem,
  itemName: string
): QaInspectionRegulationItemEquipmentVO[] =>
  item.equipmentOptions.map((equipment, index) => ({
    equipmentId: resolvePositiveId(equipment.equipmentId, itemName + '检验设备'),
    equipmentCode: equipment.equipmentCode?.trim() || '',
    equipmentName: equipment.equipmentName?.trim() || '',
    equipmentNumber: resolveRequiredText(equipment.equipmentNumber, itemName + '设备编号'),
    defaultFlag: equipment.defaultFlag === true || index === 0,
    sort: index + 1
  }))

const buildCommonRegulationSaveItem = (
  item: CommonRegulationItem,
  itemSort: number,
  finalInspectionApplicable = item.memberFinalInspectionApplicable
): QaCommonRegulationSetItemsUpgradeItemReqVO => {
  const itemName = resolveRequiredText(item.itemName, '检验项目名称')
  const inspectionConfiguration = resolveQaItemInspectionPayload(
    item,
    finalInspectionApplicable,
    itemName
  )
  return {
    itemSort,
    itemCode: resolveRequiredText(item.itemCode, itemName + '编码'),
    itemName,
    inspectionMethod: resolveRequiredText(item.inspectionMethod, itemName + '检验方法'),
    inspectionTool: resolveRequiredText(item.inspectionTool, itemName + '检验器具及设备'),
    samplingPlanText: resolveRequiredText(item.samplingPlanText, itemName + '抽样方案'),
    standardText: resolveRequiredText(item.standardText, itemName + '接受标准'),
    standardLowerLimit: item.resultType === 'NUMERIC' ? item.lowerLimit : undefined,
    standardUpperLimit: item.resultType === 'NUMERIC' ? item.upperLimit : undefined,
    standardUnit: item.standardUnit,
    standardPrecision: item.standardPrecision,
    resultType: item.resultType,
    applicableInspectionTypes: inspectionConfiguration.applicableInspectionTypes,
    firstInspectionQuantity: inspectionConfiguration.firstInspectionQuantity,
    patrolInspectionRatio: inspectionConfiguration.patrolInspectionRatio,
    critical: item.critical,
    failureRule: item.failureRule.trim() || undefined,
    sourceNote: item.sourceNoteRaw.trim() || undefined,
    sourceOriginalPage: item.sourceOriginalPage,
    sourceOriginalItem: item.sourceOriginalItem?.trim() || undefined,
    sourceOriginalExcerpt: item.sourceOriginalExcerpt?.trim() || undefined,
    sourceOriginalMethod: item.sourceOriginalMethod?.trim() || undefined,
    equipmentOptions: buildCommonRegulationItemEquipmentOptions(item, itemName)
  }
}

const buildCommonRegulationSaveProcesses = (
  member: QaCommonRegulationSetMemberVO,
  items: CommonRegulationItem[],
  finalInspectionApplicable?: boolean
): QaCommonRegulationSetItemsUpgradeProcessReqVO[] => {
  const groups = new Map<
    string,
    { code: string; name: string; sort: number; items: CommonRegulationItem[] }
  >()
  items.forEach((item, index) => {
    const code = resolveRequiredText(
      item.processCode,
      `${member.commonRegulationName} 第 ${index + 1} 行 QA 工序编码`
    )
    const name = resolveRequiredText(
      item.processName,
      `${member.commonRegulationName} 第 ${index + 1} 行 QA 工序名称`
    )
    const existing = groups.get(code)
    if (existing && existing.name !== name) {
      throw new Error(`通用规程 ${member.commonRegulationName} 的 QA 工序编码 ${code} 对应了多个工序名称`)
    }
    const group = existing || {
      code,
      name,
      sort: item.processSort || groups.size + 1,
      items: []
    }
    group.items.push(item)
    groups.set(code, group)
  })
  if (groups.size === 0) {
    throw new Error(`通用规程 ${member.commonRegulationName} 至少需要一个 QA 工序和检验项目`)
  }
  return Array.from(groups.values())
    .sort((left, right) => left.sort - right.sort)
    .map((group, processIndex) => ({
      processCode: group.code,
      processName: group.name,
      sort: processIndex + 1,
      items: group.items.map((item, itemIndex) =>
        buildCommonRegulationSaveItem(item, itemIndex + 1, finalInspectionApplicable)
      )
    }))
}

const buildCommonRegulationItemsUpgradePayload = (): QaCommonRegulationSetItemsUpgradeReqVO => {
  const set = selectedCommonRegulationSet.value
  const sourceVersion = selectedCommonRegulationSetVersionPreview.value
  if (!set?.id || !sourceVersion?.id) {
    throw new Error('请先选择通用规程套和版本')
  }
  const rowsByMemberVersionId = new Map<number, CommonRegulationItem[]>()
  commonRegulationItems.value.forEach((item) => {
    const memberVersionId = resolvePositiveId(
      item.commonRegulationVersionId,
      `${item.commonRegulationName}版本`
    )
    rowsByMemberVersionId.set(memberVersionId, [
      ...(rowsByMemberVersionId.get(memberVersionId) || []),
      item
    ])
  })
  return {
    setId: set.id,
    sourceSetVersionId: sourceVersion.id,
    versionNo: resolveCommonSetNextVersionNo(),
    effectiveDate: sourceVersion.effectiveDate || undefined,
    remark: `检验项目编辑升版自 ${sourceVersion.versionNo}`,
    members: selectedCommonRegulationSetVersionMembers.value.map((member, index) => {
      const memberVersionId = resolvePositiveId(
        member.commonRegulationVersionId,
        `${member.commonRegulationName}版本`
      )
      const memberRows = rowsByMemberVersionId.get(memberVersionId) || []
      const finalInspectionApplicable = commonFinalInspectionEdited.value
        ? commonFinalInspectionRequired.value
        : requireCommonMemberFinalInspectionApplicable(member)
      const finalInspectionNotApplicableReason = finalInspectionApplicable
        ? undefined
        : commonFinalInspectionEdited.value
          ? resolveRequiredText(
              commonFinalInspectionNotApplicableReason.value,
              '末检不适用依据'
            )
          : resolveRequiredText(member.finalInspectionNotApplicableReason, '末检不适用依据')
      return {
        commonRegulationId: resolvePositiveId(member.commonRegulationId, '通用检验规程 ID'),
        sourceCommonRegulationVersionId: memberVersionId,
        commonDccProjectCodeId: resolvePositiveId(
          member.commonDccProjectCodeId,
          '通用 DCC 项目代码 ID'
        ),
        commonRegulationCode: resolveRequiredText(member.commonRegulationCode, '通用规程编号'),
        commonRegulationName: resolveRequiredText(member.commonRegulationName, '通用规程名称'),
        versionNo: resolveCommonMemberNextVersionNo(member),
        sort: member.sort ?? (index + 1) * 10,
        memberRole: member.memberRole || undefined,
        remark: member.remark || undefined,
        finalInspectionApplicable,
        finalInspectionNotApplicableReason,
        inspectionTypeRules: resolveCommonMemberInspectionRules(
          member,
          commonFinalInspectionEdited.value ? finalInspectionApplicable : undefined,
          finalInspectionNotApplicableReason
        ),
        processes: buildCommonRegulationSaveProcesses(member, memberRows, finalInspectionApplicable)
      }
    })
  }
}

const buildQaRegulationSavePayload = (): QaInspectionRegulationSaveReqVO => {
  const finalRule = qaInspectionTypeRules.find((rule) => rule.key === 'FINAL')
  const finalInspectionApplicable = Boolean(finalRule?.required)
  const finalInspectionNotApplicableReason = finalInspectionApplicable
    ? undefined
    : resolveRequiredText(finalRule?.notApplicableReason, '末检不适用依据')
  return {
    regulationId: qaRegulationDraft.regulationId,
    dccProjectCodeId: resolvePositiveId(qaRegulationDraft.dccProjectCodeId, 'DCC 项目代码 ID'),
    ownerModule: 'MES_QA',
    regulationCode: resolveRequiredText(qaRegulationDraft.regulationCode, '规程编号'),
    regulationName: resolveRequiredText(qaRegulationDraft.regulationName, '规程名称'),
    versionNo: resolveRequiredText(resolveQaRegulationDraftVersionNoForSave(), '规程版本'),
    effectiveDate: qaRegulationDraft.effectiveDate || undefined,
    finalInspectionApplicable,
    finalInspectionNotApplicableReason,
    inspectionTypeRules: qaInspectionTypeRules.map((rule) => ({ ...rule })),
    processes: buildQaRegulationProcesses()
  }
}

const qaRegulationPublishChecks = computed(() => [
  {
    label: 'DCC 项目代码',
    passed: Boolean(qaRegulationDraft.dccProjectCodeId),
    detail: qaRegulationDraft.dccProjectCodeId ? '已选择正式 DCC 项目代码' : '请选择 DCC 项目代码'
  },
  {
    label: '规程基本信息',
    passed: Boolean(
      qaRegulationDraft.regulationCode.trim() &&
        qaRegulationDraft.regulationName.trim() &&
        qaRegulationDraft.versionNo.trim()
    ),
    detail: '规程编号、名称和版本必须完整'
  },
  {
    label: 'QA 工序与检验项目',
    passed:
      qaRegulationItems.value.length > 0 &&
      qaRegulationItems.value.every((item) =>
        Boolean(
          item.processCode.trim() &&
            item.processName.trim() &&
            item.itemCode.trim() &&
            item.itemName.trim()
        )
      ),
    detail: 'QA 工序编码、名称及检验项目必须完整'
  },
  {
    label: '项目级检验规则',
    passed:
      qaRegulationItems.value.length > 0 &&
      qaRegulationItems.value.every((item) =>
        isQaItemInspectionConfigurationComplete(item, finalInspectionRequired.value)
      ),
    detail: '每个检验项目至少启用一种检验类型；启用首检或巡检时必须填写该项目自己的数量或比例'
  },
  {
    label: '抽样方案原文',
    passed:
      qaRegulationItems.value.length > 0 &&
      qaRegulationItems.value.every((item) => Boolean(item.samplingPlanText?.trim())),
    detail: '每个检验项目必须保留正式抽样方案原文'
  }
])

const qaPublishBlockers = computed(() =>
  qaRegulationPublishChecks.value.filter((check) => !check.passed)
)

const pagedQaRegulationCompletenessChecks = computed(() =>
  paginateQaRows(qaRegulationPublishChecks.value, qaChecksQuery)
)

const resolveQaPreviewPlannedQuantity = (
  item: QaRegulationItem,
  inspectionType: QaInspectionTypeValue,
  rule: QaInspectionTypeRule
) => {
  if (inspectionType === 'FIRST') {
    return item.firstInspectionQuantity
      ? `固定 ${item.firstInspectionQuantity} 件`
      : '未配置首检数量'
  }
  if (inspectionType === 'PATROL_AM' || inspectionType === 'PATROL_PM') {
    return item.patrolInspectionRatio ? `AQL ${item.patrolInspectionRatio}%` : '未配置巡检比例'
  }
  return rule.fixedQuantity ? `固定 ${rule.fixedQuantity} 件` : '未配置末检数量'
}

const qaPqcTaskPreviewRows = computed(() => {
  if (!qaConfigurationExists.value && qaRegulationItems.value.length === 0) {
    return []
  }
  return qaRegulationItems.value.flatMap((item) =>
    resolveQaItemApplicableTypes(item).map((inspectionType) => {
      const rule = qaInspectionTypeRules.find((candidate) => candidate.key === inspectionType)
      if (!rule) {
        throw new Error(`缺少 ${resolveQaInspectionTypeLabel(inspectionType)} 规则配置`)
      }
      return {
        qaProcessName: item.processName,
        itemName: item.itemName,
        inspectionTypeText: rule.label,
        roundText: rule.roundLabel,
        plannedQuantityText: resolveQaPreviewPlannedQuantity(item, inspectionType, rule),
        regulationVersionNo: qaRegulationDraft.versionNo || '--',
        taskIdentity:
          (selectedDccProjectCode.value?.projectCode || '--') +
          ' / ' +
          item.processName +
          ' / ' +
          item.itemName +
          ' / ' +
          rule.key
      }
    })
  )
})

const pagedQaPqcTaskPreviewRows = computed(() =>
  paginateQaRows(qaPqcTaskPreviewRows.value, qaPqcPreviewQuery)
)

watch(
  () => qaRegulationItems.value.length,
  (total) => keepQaLocalPageInRange(qaItemsQuery, total)
)
watch(
  selectedCommonRegulationSetVersionDocuments,
  () => {
    commonRegulationItems.value = buildCommonRegulationItemsFromDocuments()
    resetCommonFinalInspectionDraft()
    commonRegulationItemsQuery.pageNo = 1
  },
  { immediate: true }
)
watch(
  () => commonRegulationItems.value.length,
  (total) => keepQaLocalPageInRange(commonRegulationItemsQuery, total)
)
watch(
  () => qaRegulationPublishChecks.value.length,
  (total) => keepQaLocalPageInRange(qaChecksQuery, total)
)
watch(
  () => qaPqcTaskPreviewRows.value.length,
  (total) => keepQaLocalPageInRange(qaPqcPreviewQuery, total)
)

const previewQaRegulationDraft = async () => {
  let payload: QaInspectionRegulationSaveReqVO
  try {
    payload = buildQaRegulationSavePayload()
  } catch (error) {
    ElMessage.warning(resolveDccProjectCodeErrorMessage(error))
    return
  }
  qaRegulationSaving.value = true
  try {
    const result = await QcTemplateApi.saveQaRegulationDraft(payload)
    qaRegulationDraft.regulationId = result.regulationId
    qaRegulationDraft.versionNo = result.versionNo
    qaRegulationDraft.lifecycleStatus = result.lifecycleStatus
    qaConfigurationExists.value = true
    selectedQaRegulationVersionId.value = result.draftVersionId
    const statusMap = new Map(qaRegulationProjectStatusByDccId.value)
    const currentStatus = statusMap.get(payload.dccProjectCodeId)
    statusMap.set(payload.dccProjectCodeId, {
      dccProjectCodeId: payload.dccProjectCodeId,
      configured: true,
      regulationCount: currentStatus?.regulationCount || 1,
      regulationId: result.regulationId,
      currentVersionId: currentStatus?.currentVersionId,
      regulationCode: payload.regulationCode,
      regulationName: payload.regulationName,
      lifecycleStatus: result.lifecycleStatus
    })
    qaRegulationProjectStatusByDccId.value = statusMap
    await loadQaRegulationVersionOptions(payload.dccProjectCodeId)
    ElMessage.success('QA 规程草稿已保存到后端：' + result.versionNo)
  } catch (error) {
    ElMessage.error('QA 规程草稿保存失败：' + resolveDccProjectCodeErrorMessage(error))
  } finally {
    qaRegulationSaving.value = false
  }
}

const runQaPublishPrecheck = async () => {
  if (qaPublishBlockers.value.length > 0) {
    ElMessage.warning(qaPublishBlockers.value.map((check) => check.detail).join('；'))
    return
  }
  let payload: QaInspectionRegulationSaveReqVO
  try {
    payload = buildQaRegulationSavePayload()
  } catch (error) {
    ElMessage.warning(resolveDccProjectCodeErrorMessage(error))
    return
  }
  qaRegulationPublishing.value = true
  try {
    const publishedVersion = await QcTemplateApi.publishQaRegulation(payload)
    applyQaRegulationConfiguration(publishedVersion)
    qaCurrentPublishedVersion.value = publishedVersion
    const statusMap = new Map(qaRegulationProjectStatusByDccId.value)
    statusMap.set(payload.dccProjectCodeId, {
      dccProjectCodeId: payload.dccProjectCodeId,
      configured: true,
      regulationCount: statusMap.get(payload.dccProjectCodeId)?.regulationCount || 1,
      regulationId: publishedVersion.regulationId,
      currentVersionId: publishedVersion.publishedVersionId,
      regulationCode: publishedVersion.regulationCode,
      regulationName: publishedVersion.regulationName,
      lifecycleStatus: 'PUBLISHED'
    })
    qaRegulationProjectStatusByDccId.value = statusMap
    await loadQaRegulationVersionOptions(payload.dccProjectCodeId)
    ElMessage.success('QA 规程已发布为不可变版本：' + publishedVersion.versionNo)
  } catch (error) {
    ElMessage.error('QA 规程发布失败：' + resolveDccProjectCodeErrorMessage(error))
  } finally {
    qaRegulationPublishing.value = false
  }
}
</script>

<style scoped>
.qa-regulation-page {
  display: grid;
  gap: 0;
}

.qa-regulation-page__header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 0;
}

.qa-regulation-page__title {
  flex-shrink: 0;
  color: #172033;
  font-size: 20px;
  font-weight: 700;
}

.qa-regulation-page__workspace-tabs-wrap {
  margin-bottom: 0 !important;
}

.qa-regulation-page__workspace-tabs-wrap :deep(.el-card__body) {
  padding-top: 10px !important;
  padding-bottom: 0 !important;
}

.qa-regulation-page__workspace-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.qa-regulation-page__workspace-tabs :deep(.el-tabs__content) {
  display: none;
}

.qa-regulation-page__hint {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.qa-regulation-page__layout {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 16px;
}

.qa-regulation-page__form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.qa-regulation-page__basic-form {
  margin: 0;
}

.qa-regulation-page__basic-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.qa-regulation-page__basic-field {
  margin-bottom: 0;
}

.qa-regulation-page__basic-field--full {
  grid-column: 1 / -1;
}

.qa-regulation-page__project-wrap,
.qa-regulation-page__tabs-wrap {
  margin-bottom: 0 !important;
}

.qa-regulation-page__project-form {
  flex: 0 1 720px;
  min-width: 280px;
  margin: 0;
}

.qa-regulation-page__project-field {
  margin-bottom: 0;
}

.qa-regulation-page__project-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.qa-regulation-page__project-select {
  flex: 1 1 auto;
  min-width: 0;
}

.qa-regulation-page__project-select :deep(.el-select__selected-item) {
  user-select: text;
}

.qa-regulation-page__project-select :deep(.el-select__placeholder) {
  user-select: text;
}

.qa-regulation-page__project-option-label {
  display: block;
  overflow: hidden;
  color: #344054;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-regulation-page__project-option-label.is-configured {
  color: #00a896;
  font-weight: 700;
}

.qa-regulation-page__project-copy-button {
  flex-shrink: 0;
}

.qa-regulation-page__published-version {
  display: inline-flex;
  flex: 0 1 220px;
  align-items: center;
  gap: 8px;
  min-width: 180px;
  color: #344054;
  font-size: 13px;
  white-space: nowrap;
}

.qa-regulation-page__published-version-label {
  flex-shrink: 0;
  color: #606a7b;
  font-weight: 600;
}

.qa-regulation-page__published-version-value {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-weight: 700;
  text-overflow: ellipsis;
}

.qa-regulation-page__published-version-value.is-empty,
.qa-regulation-page__published-version-value.is-loading {
  color: #8a94a6;
  font-weight: 500;
}

.qa-regulation-page__published-version-value.is-error {
  color: #d92d20;
  font-weight: 600;
}

.qa-regulation-page__version-publish {
  display: flex;
  flex: 1 1 620px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-left: auto;
  min-width: 0;
}

.qa-regulation-page__version-publish :deep([data-qa-regulation-word-import]) {
  flex-shrink: 0;
}

.qa-regulation-page__version-publish :deep([data-qa-regulation-test-reset]) {
  flex-shrink: 0;
}

.qa-regulation-page__header :deep(.el-tag) {
  flex-shrink: 0;
  margin-left: 0;
}

.qa-regulation-page__header-field {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.qa-regulation-page__header-field-label {
  flex-shrink: 0;
  color: #606a7b;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.qa-regulation-page__version-input {
  width: 92px;
}

.qa-regulation-page__effective-date {
  width: 142px;
}

.qa-regulation-page__version-publish :deep(.el-tag) {
  flex-shrink: 0;
  margin-left: 0;
}

.qa-regulation-page__tabs-wrap :deep(.el-card__body) {
  padding-top: 12px !important;
  padding-bottom: 0 !important;
}

.qa-regulation-page__tabs-wrap :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.qa-regulation-page__tabs--flat :deep(.el-tabs__header) {
  margin: 0;
}

.qa-regulation-page__tabs--flat :deep(.el-tabs__item) {
  color: #172033;
  font-weight: 600;
}

.qa-regulation-page__tabs--flat :deep(.el-tabs__item.is-active) {
  color: #00a896;
}

.qa-regulation-page__tabs--flat :deep(.el-tabs__active-bar) {
  background-color: #00a896;
}

.qa-regulation-page__tabs-wrap :deep(.el-tabs__content) {
  display: none;
}

.qa-regulation-page__overview-stack {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.qa-regulation-page__overview-card {
  min-width: 0;
}

.qa-regulation-page__overview-card :deep(.el-card__body) {
  min-width: 0;
}

.qa-regulation-page__load-error {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.qa-regulation-page__load-error .el-button {
  justify-self: flex-start;
}

.qa-regulation-page__route-scope {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.qa-regulation-page__manual-route-bind {
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.qa-regulation-page__manual-route-bind .qa-regulation-page__form {
  margin: 0;
}

.qa-regulation-page__manual-route-button {
  width: 100%;
}

.qa-regulation-page__scope-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.qa-regulation-page__scope-row {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fbfcfe;
}

.qa-regulation-page__scope-label {
  color: #667085;
  font-size: 12px;
}

.qa-regulation-page__scope-value {
  overflow: hidden;
  color: #172033;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-regulation-page__overview-note {
  margin-top: 0;
}

.qa-regulation-page__overview-note-list {
  margin: 0;
  padding-left: 24px;
  color: #344054;
  font-size: 14px;
  line-height: 1.75;
  overflow-wrap: anywhere;
}

.qa-regulation-page__overview-note-list li {
  padding-left: 4px;
}

.qa-regulation-page__overview-note-list li + li {
  margin-top: 8px;
}

.qa-regulation-page__common-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.qa-regulation-page__common-binding-control {
  padding: 16px;
  border: 1px solid #d0d5dd;
  border-radius: 12px;
  background: #f8fafc;
}

.qa-regulation-page__common-binding-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.qa-regulation-page__common-binding-title {
  margin: 0;
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.qa-regulation-page__common-binding-subtitle {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
}

.qa-regulation-page__common-set-workbench {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.qa-regulation-page__common-set-detail-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.qa-regulation-page__common-set-summary {
  display: inline-block;
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.qa-regulation-page__common-set-list {
  min-width: 0;
}

.qa-regulation-page__common-set-list :deep(.unified-list-template__toolbar-actions) {
  min-width: 0;
}

.qa-regulation-page__common-set-list :deep(.unified-list-template__toolbar) {
  min-width: 0;
  max-width: 100%;
  flex-wrap: wrap;
}

.qa-regulation-page__common-set-list :deep(.el-table__row),
.qa-regulation-page__common-set-detail :deep(.el-table__row) {
  cursor: pointer;
}

.qa-regulation-page__common-set-empty-value {
  color: #98a2b3;
}

.qa-regulation-page__common-set-detail {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding-top: 4px;
}

.qa-regulation-page__common-set-detail--documents {
  gap: 14px;
}

.qa-regulation-page__common-set-detail--items {
  gap: 14px;
}

.qa-regulation-page__common-items-parameter-block {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding-bottom: 14px;
  border-bottom: 1px solid #e4e7ed;
}

.qa-regulation-page__common-items-parameter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.qa-regulation-page__common-set-detail + .qa-regulation-page__common-set-detail {
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.qa-regulation-page__common-set-detail-title,
.qa-regulation-page__common-set-version-cell {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.qa-regulation-page__common-set-detail-title > span:not(.qa-regulation-page__common-set-summary) {
  color: #475467;
}

.qa-regulation-page__common-set-document-title {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.qa-regulation-page__common-set-document-title strong,
.qa-regulation-page__common-set-document-title span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-regulation-page__common-set-document-title span {
  color: #667085;
  font-size: 12px;
}

.qa-regulation-page__common-set-document-tabs {
  min-width: 0;
}

.qa-regulation-page__common-set-document-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 12px;
  min-width: 0;
}

.qa-regulation-page__common-set-document-card {
  min-width: 0;
}

.qa-regulation-page__common-set-document-card :deep(.el-card__body) {
  display: grid;
  gap: 10px;
}

.qa-regulation-page__common-set-document-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.qa-regulation-page__common-set-process {
  display: grid;
  grid-template-columns: minmax(110px, 0.32fr) minmax(0, 1fr);
  gap: 10px;
  line-height: 1.6;
}

.qa-regulation-page__common-set-process + .qa-regulation-page__common-set-process {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #ebeef5;
}

.qa-regulation-page__common-set-process span {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #475467;
}

.qa-regulation-page__common-set-process-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.qa-regulation-page__common-set-item-chip {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  padding: 2px 8px;
  border: 1px solid #d0d5dd;
  border-radius: 999px;
  background: #f8fafc;
  color: #344054;
  font-size: 12px;
  line-height: 1.6;
}

.qa-regulation-page__common-field {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  background: #fbfcfe;
}

.qa-regulation-page__common-field--control {
  align-content: start;
}

.qa-regulation-page__common-label {
  color: #667085;
  font-size: 12px;
}

.qa-regulation-page__common-value {
  overflow: hidden;
  color: #172033;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-regulation-page__common-alert {
  margin-top: 14px;
}

.qa-regulation-page__common-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.qa-regulation-page__card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.qa-regulation-page__card-actions {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.qa-regulation-page__final-inspection-switch {
  display: grid;
  grid-template-columns: auto auto minmax(180px, 260px);
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 6px 10px;
  border: 1px solid #d0d5dd;
  border-radius: 999px;
  background: #f8fafc;
}

.qa-regulation-page__final-inspection-label {
  color: #172033;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.qa-regulation-page__final-inspection-reason {
  width: 100%;
  min-width: 0;
}

.qa-regulation-page__process-name {
  color: #172033;
  font-weight: 700;
}

.qa-regulation-page__applicable-types {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.qa-regulation-page__item-inspection-rule {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.qa-regulation-page__inspection-value {
  display: grid;
  grid-template-columns: auto minmax(108px, 1fr) auto;
  gap: 6px;
  align-items: center;
  min-width: 0;
  color: #344054;
  font-size: 12px;
}

.qa-regulation-page__inspection-value :deep(.el-input-number) {
  width: 100%;
}

.qa-regulation-page__sampling-plan {
  color: #172033;
  font-size: 12px;
  line-height: 1.55;
  white-space: normal;
}

.qa-regulation-page__inspection-tool-cell {
  display: grid;
  gap: 8px;
}

.qa-regulation-page__equipment-binding {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 6px;
}

.qa-regulation-page__equipment-binding--readonly {
  grid-template-columns: minmax(0, 1fr) auto;
}

.qa-regulation-page__equipment-binding--readonly > span:first-child {
  min-width: 0;
  overflow: hidden;
  color: #344054;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-regulation-page__equipment-select {
  min-width: 0;
}

.qa-regulation-page__equipment-number {
  color: #667085;
  font-size: 12px;
  white-space: nowrap;
}

.qa-regulation-page__equipment-error {
  color: var(--el-color-danger);
  font-size: 12px;
}

.qa-regulation-page__source {
  display: grid;
  gap: 6px;
  padding: 8px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.qa-regulation-page__source-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  color: #172033;
  font-size: 12px;
  font-weight: 700;
}

.qa-regulation-page__source-label {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.qa-regulation-page__source-text {
  color: #172033;
  font-size: 12px;
  line-height: 1.55;
  white-space: normal;
}

.qa-regulation-page__check-list {
  display: grid;
  gap: 10px;
}

.qa-regulation-page__check {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
  padding: 10px;
  border: 1px solid #f2c6c6;
  border-radius: 8px;
  background: #fff7f7;
}

.qa-regulation-page__check.is-passed {
  border-color: #b7e1c0;
  background: #f5fff7;
}

.qa-regulation-page__check-title {
  color: #172033;
  font-weight: 700;
}

.qa-regulation-page__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

@media (max-width: 1500px) {
  .qa-regulation-page__header {
    flex-wrap: wrap;
  }

  .qa-regulation-page__project-form {
    order: 3;
    flex: 1 0 100%;
    min-width: 0;
  }

  .qa-regulation-page__version-publish {
    margin-left: auto;
  }

  .qa-regulation-page__published-version {
    margin-left: auto;
  }
}

@media (max-width: 1180px) {
  .qa-regulation-page__header {
    flex-wrap: wrap;
  }

  .qa-regulation-page__project-form {
    order: 3;
    flex: 1 0 100%;
    min-width: 0;
  }

  .qa-regulation-page__layout {
    grid-template-columns: 1fr;
  }

  .qa-regulation-page__scope-grid {
    grid-template-columns: 1fr;
  }

  .qa-regulation-page__basic-grid {
    grid-template-columns: 1fr;
  }

  .qa-regulation-page__basic-field--full {
    grid-column: auto;
  }
}

@media (max-width: 720px) {
  .qa-regulation-page__common-panel :deep(.el-card__body) {
    padding: 12px;
  }

  .qa-regulation-page__common-set-list :deep(.unified-list-template__toolbar-actions) {
    width: 100%;
    margin-left: 0;
  }

  .qa-regulation-page__common-set-list :deep(.unified-list-template__toolbar) {
    width: 100%;
    justify-content: flex-start;
  }

  .qa-regulation-page__common-set-process {
    grid-template-columns: 1fr;
    gap: 2px;
  }

  .qa-regulation-page__common-set-document-grid {
    grid-template-columns: 1fr;
  }

  .qa-regulation-page__common-set-document-card-head {
    flex-direction: column;
  }

  .qa-regulation-page__common-set-detail-head > .el-button {
    width: 100%;
  }

  .qa-regulation-page__final-inspection-switch {
    grid-template-columns: 1fr;
    justify-items: start;
    width: 100%;
    border-radius: 12px;
  }

  .qa-regulation-page__version-publish {
    flex: 1 0 100%;
    flex-wrap: wrap;
    margin-left: 0;
  }

  .qa-regulation-page__published-version {
    flex: 1 0 100%;
    margin-left: 0;
  }

  .qa-regulation-page__header-field {
    flex: 1 1 180px;
  }

  .qa-regulation-page__version-input,
  .qa-regulation-page__effective-date {
    flex: 1;
    width: auto;
    min-width: 0;
  }
}
</style>
