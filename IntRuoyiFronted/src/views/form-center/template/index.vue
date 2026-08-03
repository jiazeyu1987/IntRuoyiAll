<template>
  <ContentWrap
    v-if="!isDesignerMode && !isTemplateSimulationMode"
    :body-style="{ padding: '0px' }"
    class="!mb-0 form-template-page scheme-d-basic-data-page scheme-d-basic-data-page--form-template"
  >
    <div class="form-template-workbench">
      <section class="form-template-workbench__list">
        <UnifiedListTemplate
          table-key="form.center.template"
          :query-model="queryParams"
          label-width="88px"
          :filter-definitions="quickFilterDefinitions"
          :show-quick-filter-label="false"
          :quick-filter-state="quickFilter.state"
          :selected-filter-definition="quickFilter.selectedDefinition.value"
          :operator-options="quickFilter.operatorOptions.value"
          :columns="templateColumns"
          :column-saving="templateColumnSaving"
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @update:quick-filter-state="quickFilter.updateState"
          @quick-filter-query="handleQuery"
          @column-change="saveTemplateColumnConfig"
          @column-reset="resetTemplateColumnConfig"
          @pagination="getList"
        >
          <template #actions>
            <el-button class="scheme-d-btn scheme-d-btn--warning" @click="resetQuery">
              <Icon class="mr-5px" icon="ep:refresh" />
              重置
            </el-button>
            <el-button
              v-hasPermi="['form:template:create']"
              class="scheme-d-btn scheme-d-btn--primary"
              plain
              type="primary"
              @click="openImport"
            >
              <Icon class="mr-5px" icon="ep:upload" />
              导入
            </el-button>
          </template>

          <template #table="{ sortColumnAttrs, handleSortChange }">
            <el-table
              v-loading="loading"
              :data="list"
              :row-key="templateRowKey"
              height="calc(100vh - 260px)"
              highlight-current-row
              border
              :stripe="true"
              :show-overflow-tooltip="true"
              data-user-table-column-explicit
              data-user-table-key="form.center.template"
              :row-class-name="templateRowClassName"
              @row-click="selectTemplate"
              @header-dragend="handleTemplateHeaderDragend"
              @sort-change="handleSortChange"
            >
              <el-table-column
                v-if="isTemplateColumnVisible('templateName')"
                align="center"
                label="模板名称"
                prop="templateName"
                :min-width="getTemplateColumnMinWidthString('templateName', 165)"
                v-bind="sortColumnAttrs('templateName')"
              />
              <el-table-column
                v-if="isTemplateColumnVisible('currentEffectiveVersion')"
                align="center"
                label="当前生效版本"
                prop="currentEffectiveVersion"
                :width="getTemplateColumnWidthString('currentEffectiveVersion', 130)"
              >
                <template #default="{ row }">
                  <span
                    v-if="isCurrentEffectiveVersion(row.status)"
                    class="template-version-current"
                  >
                    {{ row.versionNo }}
                  </span>
                  <span v-else class="template-version-empty">无</span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isTemplateColumnVisible('pendingVersion')"
                align="center"
                label="待发布版本"
                prop="pendingVersion"
                :width="getTemplateColumnWidthString('pendingVersion', 130)"
              >
                <template #default="{ row }">
                  <el-tag
                    v-if="isPendingTemplateVersion(row.status)"
                    class="template-version-tag scheme-d-tag"
                    :type="pendingVersionTagType(row.status)"
                    effect="plain"
                  >
                    {{ row.versionNo }} {{ statusLabel(row.status) }}
                  </el-tag>
                  <span v-else class="template-version-empty">无</span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isTemplateColumnVisible('status')"
                align="center"
                label="状态"
                prop="status"
                :width="getTemplateColumnWidthString('status', 105)"
                v-bind="sortColumnAttrs('status')"
              >
                <template #default="{ row }">
                  <el-tag class="scheme-d-tag" :type="statusTagType(row.status)" effect="plain">
                    {{ statusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isTemplateColumnVisible('updatedTime')"
                align="center"
                label="修改时间"
                prop="updatedTime"
                :width="getTemplateColumnWidthString('updatedTime', 180)"
                min-width="180"
                v-bind="sortColumnAttrs('updatedTime')"
              >
                <template #default="{ row }">
                  {{ formatTemplateUpdatedTime(row.updatedTime) }}
                </template>
              </el-table-column>
              <el-table-column
                v-if="isTemplateColumnVisible('remark')"
                align="center"
                label="备注"
                prop="remark"
                :min-width="getTemplateColumnMinWidthString('remark', 180)"
                v-bind="sortColumnAttrs('remark')"
              />
            </el-table>
          </template>
        </UnifiedListTemplate>
      </section>

      <section class="form-template-preview" data-form-template-preview>
        <div class="form-template-preview__header">
          <div class="form-template-preview__heading">
            <span class="form-template-preview__eyebrow">表单预览</span>
            <strong class="form-template-preview__title">
              {{ selectedTemplate?.templateName || '未选择模板' }}
            </strong>
          </div>
          <div v-if="selectedTemplate" class="form-template-preview__actions">
            <el-button
              link
              class="scheme-d-row-action scheme-d-row-action--primary"
              type="primary"
              @click="enterPreviewMaximize"
            >
              最大化
            </el-button>
            <el-button
              link
              class="scheme-d-row-action scheme-d-row-action--primary"
              type="primary"
              @click="openSelectedTemplate"
            >
              打开
            </el-button>
            <el-button
              v-if="canUseTemplateInteractiveAction(selectedTemplate)"
              v-hasPermi="['form:template:create']"
              link
              class="scheme-d-row-action scheme-d-row-action--primary"
              type="primary"
              @click="editSelectedTemplate"
            >
              编辑
            </el-button>
            <el-button
              v-if="canUseTemplateInteractiveAction(selectedTemplate)"
              link
              class="scheme-d-row-action scheme-d-row-action--primary"
              type="primary"
              @click="openSelectedTemplateFill"
            >
              填写
            </el-button>
            <el-button
              v-if="canUseTemplateInteractiveAction(selectedTemplate)"
              @click="openSelectedTemplateFillConfig"
              v-hasPermi="['form:template:update']"
              link
              class="scheme-d-row-action scheme-d-row-action--primary"
              type="primary"
            >
              填写配置
            </el-button>
            <el-button
              v-if="canUseTemplateInteractiveAction(selectedTemplate)"
              link
              class="scheme-d-row-action scheme-d-row-action--primary"
              type="primary"
              @click="openSelectedTemplateCellLinks"
            >
              链接
            </el-button>
            <el-button
              v-hasPermi="['form:template-source:download']"
              link
              class="scheme-d-row-action scheme-d-row-action--warning"
              type="primary"
              @click="downloadSelectedTemplateSource"
            >
              下载
            </el-button>
            <el-button
              v-if="canPublishTemplate(selectedTemplate)"
              v-hasPermi="['form:template:publish']"
              link
              class="scheme-d-row-action scheme-d-row-action--success"
              type="primary"
              @click="publishSelectedTemplate"
            >
              发布
            </el-button>
            <el-button
              v-if="canDisableTemplate(selectedTemplate)"
              v-hasPermi="['form:template:disable']"
              link
              class="scheme-d-row-action scheme-d-row-action--warning"
              type="warning"
              @click="disableSelectedTemplate"
            >
              停用
            </el-button>
            <el-button
              v-if="canEnableTemplate(selectedTemplate)"
              v-hasPermi="['form:template:disable']"
              link
              class="scheme-d-row-action scheme-d-row-action--success"
              type="primary"
              @click="enableSelectedTemplate"
            >
              启用
            </el-button>
            <el-button
              v-if="resolveTemplateObsoleteOperationState(selectedTemplate) === 'pending-withdrawable'"
              v-hasPermi="['form:template:obsolete']"
              link
              class="scheme-d-row-action scheme-d-row-action--warning"
              type="warning"
              @click="withdrawSelectedTemplateObsoleteRequest"
            >
              撤回作废申请
            </el-button>
            <el-button
              v-else-if="resolveTemplateObsoleteOperationState(selectedTemplate) === 'pending-readonly'"
              link
              class="scheme-d-row-action scheme-d-row-action--warning"
              type="warning"
              disabled
            >
              作废申请中
            </el-button>
            <el-button
              v-else-if="canObsoleteTemplate(selectedTemplate)"
              v-hasPermi="['form:template:obsolete']"
              link
              class="scheme-d-row-action scheme-d-row-action--danger"
              type="danger"
              @click="obsoleteSelectedTemplate"
            >
              作废
            </el-button>
          </div>
        </div>

        <div class="form-template-preview__body">
          <el-empty v-if="!selectedTemplate" description="请选择左侧模板查看预览" />
          <div v-else-if="visualPreviewFormViewModel" class="form-template-visual-preview">
            <EdhrExecutionReadonlyForm
              :form-view-model="visualPreviewFormViewModel"
              :signature-records="[]"
              fit-to-viewport
              fit-mode="width"
              embedded
            />
          </div>
          <el-empty v-else description="当前模板暂无识别字段，可下载源文件查看原始表单" />
        </div>
      </section>
    </div>

    <Teleport to="body">
      <div v-if="previewMaximized" class="form-template-focused-preview">
        <aside class="form-template-focused-preview__control">
          <span>当前模板</span>
          <strong>{{ selectedTemplate?.templateName || '未选择模板' }}</strong>
          <el-button
            class="scheme-d-btn scheme-d-btn--primary"
            plain
            type="primary"
            @click="restorePreviewLayout"
          >
            恢复
          </el-button>
        </aside>
        <main class="form-template-focused-preview__stage">
          <div class="form-template-focused-preview__body">
            <el-empty v-if="!selectedTemplate" description="请选择模板查看预览" />
            <div v-else-if="visualPreviewFormViewModel" class="form-template-visual-preview">
              <EdhrExecutionReadonlyForm
                :form-view-model="visualPreviewFormViewModel"
                :signature-records="[]"
                fit-to-viewport
                fit-mode="width"
                embedded
              />
            </div>
            <el-empty v-else description="当前模板暂无识别字段" />
          </div>
        </main>
      </div>
    </Teleport>
  </ContentWrap>

  <FormTemplateDesignerWrapper
    v-if="isDesignerMode && templateDesignerMode === 'preview'"
    class="!mb-0 form-template-route-workspace"
  >
    <div class="form-template-route-workspace__header">
      <el-button link type="primary" @click="returnToTemplateList">返回表单模板</el-button>
      <div class="form-template-route-workspace__heading">
        <span>查看表单模板</span>
        <strong>{{ selectedTemplate?.templateName || '未加载模板' }}</strong>
      </div>
      <el-tag type="primary" effect="plain">只读</el-tag>
    </div>
    <el-alert
      v-if="templateRouteLoadError"
      :title="templateRouteLoadError"
      type="error"
      :closable="false"
      show-icon
      class="m-16px"
    />
    <div v-else-if="visualPreviewFormViewModel" class="form-template-route-workspace__body">
      <EdhrExecutionReadonlyForm
        :form-view-model="visualPreviewFormViewModel"
        :signature-records="[]"
        fit-to-viewport
        fit-mode="width"
      />
    </div>
    <el-empty v-else description="当前模板暂无识别字段" />
  </FormTemplateDesignerWrapper>

  <TemplateImportDialog
    v-if="!isDesignerMode && !isTemplateSimulationMode"
    ref="importDialogRef"
    @success="getList"
  />
  <FormTemplateFillConfigDialog
    v-if="!isDesignerMode && !isTemplateSimulationMode"
    v-model="fillConfigDialogVisible"
    :template="selectedTemplate"
    :sheet-layout-json="visualPreviewFormViewModel?.sheetLayoutJson || parsedTemplateJimuSchema?.sheetLayoutJson"
    :cell-rules="templatePreviewCellRules"
    :signature-cell-markers="templatePreviewSignatureMarkers"
    :assist-rows="parsedTemplateJimuSchema?.assistRows || []"
    :fill-assignments="parsedTemplateJimuSchema?.fillAssignments || []"
    :readonly="selectedTemplate?.status !== 'DRAFT'"
    :saving="fillConfigSaving"
    @save="saveSelectedTemplateFillConfig"
  />
  <Dialog v-model="obsoleteRequestDialogVisible" title="作废表单模板" width="560px">
    <el-alert
      title="提交后进入 BPM 审核，审批通过后才会变为已作废；审批中会锁定普通操作。"
      type="warning"
      :closable="false"
      show-icon
      class="mb-12px"
    />
    <el-descriptions v-if="obsoleteRequestTarget" :column="1" border class="mb-12px">
      <el-descriptions-item label="模板名称">
        {{ obsoleteRequestTarget.templateName }}
      </el-descriptions-item>
      <el-descriptions-item label="版本号">
        {{ obsoleteRequestTarget.versionNo }}
      </el-descriptions-item>
      <el-descriptions-item label="当前状态">
        {{ statusLabel(obsoleteRequestTarget.status) }}
      </el-descriptions-item>
    </el-descriptions>
    <el-form label-position="top">
      <el-form-item label="作废原因" required>
        <el-input
          v-model="obsoleteRequestForm.reason"
          type="textarea"
          :rows="4"
          maxlength="300"
          show-word-limit
          placeholder="请输入作废原因"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="obsoleteRequestDialogVisible = false">取消</el-button>
      <el-button
        type="danger"
        :loading="obsoleteRequestSubmitting"
        @click="submitSelectedTemplateObsoleteRequest"
      >
        提交作废申请
      </el-button>
    </template>
  </Dialog>
  <ContentWrap
    v-if="isTemplateSimulationMode"
    :body-style="{ padding: '0px' }"
    class="!mb-0 form-template-route-workspace"
  >
    <div class="form-template-route-workspace__header">
      <el-button link type="primary" @click="returnFromTemplateSimulation">
        {{ templateSimulationBackLabel }}
      </el-button>
      <div class="form-template-route-workspace__heading">
        <span>模拟填写</span>
        <strong>{{ selectedTemplate?.templateName || '未加载模板' }}</strong>
      </div>
      <el-tag type="primary">模拟填写</el-tag>
    </div>
    <el-alert
      v-if="templateRouteLoadError"
      :title="templateRouteLoadError"
      type="error"
      :closable="false"
      show-icon
      class="m-16px"
    />
    <div
      v-else-if="visualPreviewFormViewModel && simulatedPreviewFormViewModel"
      class="form-template-fill-workspace form-template-route-workspace__body"
    >
      <section class="form-template-fill-workspace__panel">
        <div class="form-template-dialog-panel-head">
          <strong>模板内填写</strong>
          <span>左侧直接在模板格内模拟填写。</span>
        </div>
        <div class="form-template-fill-workspace__surface fit-to-viewport width-only">
          <EdhrExecutionTemplateEditableForm
            v-model="templateFillValues"
            :sheet-layout-json="visualPreviewFormViewModel.sheetLayoutJson"
            :cell-rules="templatePreviewCellRules"
            :signature-markers="templatePreviewSignatureMarkers"
            fit-to-viewport
            fit-mode="width"
            @signature-action="handleTemplatePreviewSignatureAction"
          />
        </div>
      </section>
      <section class="form-template-fill-workspace__panel">
        <div class="form-template-dialog-panel-head">
          <strong>表单显示</strong>
          <span>右侧同步展示填写后的只读效果。</span>
        </div>
        <div class="form-template-fill-workspace__surface fit-to-viewport width-only">
          <EdhrExecutionReadonlyForm
            :form-view-model="simulatedPreviewFormViewModel"
            :signature-records="[]"
            fit-to-viewport
            fit-mode="width"
          />
        </div>
      </section>
    </div>
    <el-empty v-else description="当前模板暂无可填写字段" />
  </ContentWrap>
  <FormTemplateDesignerWrapper
    v-if="isDesignerMode && templateDesignerMode === 'edit'"
    class="!mb-0 form-template-route-workspace"
  >
    <div class="form-template-route-workspace__header">
      <el-button link type="primary" @click="returnToTemplateList">返回表单模板</el-button>
      <div class="form-template-route-workspace__heading">
        <span>{{ rulesDialogTitle }}</span>
        <strong>{{ selectedTemplate?.templateName || '未加载模板' }}</strong>
      </div>
      <el-tag :type="selectedTemplate?.status === 'DRAFT' ? 'primary' : 'info'" effect="plain">
        {{ selectedTemplate?.status === 'DRAFT' ? '编辑' : '只读' }}
      </el-tag>
    </div>
    <el-alert
      v-if="templateRouteLoadError"
      :title="templateRouteLoadError"
      type="error"
      :closable="false"
      show-icon
      class="m-16px"
    />
    <div v-else class="batch-record-cell-rules-editor form-template-rule-workspace">
      <section class="batch-record-cell-rules-editor__summary">
        <span class="batch-record-cell-rules-editor__name">
          {{ selectedTemplate?.templateName || '-' }}
        </span>
        <el-tag type="primary" effect="plain">规则 {{ editableTemplateCellRules.length }}</el-tag>
        <el-tag :type="pendingTemplateRuleCount > 0 ? 'warning' : 'success'" effect="plain">
          待确认 {{ pendingTemplateRuleCount }}
        </el-tag>
        <span class="batch-record-cell-rules-editor__mode">
          规则编辑模式：左侧只选单元格，右侧切换可填写/不可填写
        </span>
      </section>

      <section class="batch-record-cell-rules-editor__workspace">
        <div class="batch-record-cell-rules-editor__preview">
          <div class="batch-record-cell-rules-editor__panel-head">
            <div>
              <strong>{{ rulesDialogPreviewTitle }}</strong>
              <p>{{ rulesDialogPreviewTip }}</p>
            </div>
            <el-tag type="info" effect="plain">只读</el-tag>
          </div>
          <el-alert
            v-if="editableRulesSheetLayoutError"
            :title="editableRulesSheetLayoutError"
            type="error"
            :closable="false"
            show-icon
          />
          <div v-else-if="renderedTemplateRuleRows.length" class="batch-record-cell-rules-editor__sheet-scroll">
            <table class="batch-record-cell-rules-editor__sheet">
              <colgroup>
                <col
                  v-for="column in renderedTemplateRuleColumns"
                  :key="column.columnIndex"
                  :style="{ width: `${column.widthPercent}%` }"
                />
              </colgroup>
              <tbody>
                <tr
                  v-for="row in renderedTemplateRuleRows"
                  :key="row.rowIndex"
                  :style="{ height: `${row.height}px` }"
                >
                  <td
                    v-for="cell in row.cells"
                    :key="cell.identity"
                    :rowspan="cell.rowSpan"
                    :colspan="cell.colSpan"
                    :class="cell.classNames"
                  >
                    <button
                      type="button"
                      class="batch-record-cell-rules-editor__cell-button"
                      aria-label="选择单元格规则"
                      :aria-pressed="cell.identity === selectedRuleKey"
                      @click="selectRuleCell(cell)"
                    >
                      <span v-if="cell.text" class="batch-record-cell-rules-editor__cell-text">
                        {{ cell.text }}
                      </span>
                      <span v-else class="batch-record-cell-rules-editor__cell-placeholder">
                        第 {{ cell.rowIndex + 1 }} 行第 {{ cell.columnIndex + 1 }} 列
                      </span>
                      <span v-if="cell.rule" class="batch-record-cell-rules-editor__cell-rule">
                        <span>{{ valueTypeLabelMap[cell.rule.valueType] || cell.rule.valueType }}</span>
                        <b v-if="cell.rule.required">必填</b>
                      </span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <el-empty v-else description="暂无可展示的表单布局" />
        </div>

        <aside class="batch-record-cell-rules-editor__side-panel">
          <el-alert
            v-if="selectedTemplate?.status !== 'DRAFT'"
            :title="rulesReadonlyTitle"
            type="warning"
            :closable="false"
            show-icon
          />
          <template v-if="selectedRuleCell">
            <div class="batch-record-cell-rules-editor__fillable-toggle">
              <strong>是否可填写</strong>
              <el-switch
                v-model="isSelectedCellFillable"
                active-text="可填写"
                inactive-text="不可填写"
              />
            </div>

            <template v-if="selectedRule">
              <el-form label-position="top" class="batch-record-cell-rules-editor__form">
                <el-form-item label="字段名称">
                  <el-input
                    v-model="selectedRule.label"
                    maxlength="80"
                    show-word-limit
                    placeholder="请输入字段名称"
                  />
                </el-form-item>
                <el-form-item label="单元格提示词">
                  <el-input
                    v-model="selectedRule.placeholder"
                    maxlength="120"
                    show-word-limit
                    placeholder="请输入单元格空值提示"
                  />
                </el-form-item>
                <el-form-item label="字段说明">
                  <el-input
                    v-model="selectedRule.helpText"
                    type="textarea"
                    :rows="3"
                    maxlength="300"
                    show-word-limit
                    placeholder="说明这个单元格要填写什么内容"
                  />
                </el-form-item>
                <el-form-item label="是否必填">
                  <el-switch
                    v-model="selectedRule.required"
                    active-text="必填"
                    inactive-text="可选"
                  />
                </el-form-item>
                <el-form-item label="字段类型">
                  <el-select
                    v-model="selectedRule.valueType"
                    class="!w-1/1"
                    @change="handleSelectedValueTypeChange"
                  >
                    <el-option
                      v-for="option in cellRuleValueTypeOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="控件类型">
                  <el-select
                    v-model="selectedRule.componentFlag"
                    class="!w-1/1"
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请选择或输入控件类型"
                  >
                    <el-option
                      v-for="option in componentFlagOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </el-form-item>
              </el-form>
            </template>
          </template>
          <el-empty v-else description="请在左侧表单中点击一个单元格" />
        </aside>
      </section>
    </div>
    <div class="form-template-route-workspace__actions">
      <el-button @click="returnToTemplateList">关闭</el-button>
      <el-button @click="reloadEditableTemplateRules">重新读取</el-button>
      <el-button
        type="primary"
        :loading="rulesSaving"
        :disabled="!canSaveEditableRules"
        @click="saveEditableTemplateRules"
      >
        {{ rulesSaveButtonText }}
      </el-button>
    </div>
  </FormTemplateDesignerWrapper>
  <Dialog
    v-model="signatureDialogVisible"
    class="form-template-signature-dialog"
    title="签名位"
    width="720px"
  >
    <el-alert
      title="签名位来自单元格规则：控件类型为 signature 或值类型为 SIGNATURE 的单元格会进入签名清单。"
      type="info"
      :closable="false"
      show-icon
    />
    <el-table :data="templateSignatureRows" border size="small" class="mt-12px">
      <el-table-column label="字段" prop="label" min-width="180" />
      <el-table-column label="位置" width="120">
        <template #default="{ row }">
          第 {{ row.rowIndex + 1 }} 行 / 第 {{ row.columnIndex + 1 }} 列
        </template>
      </el-table-column>
      <el-table-column label="签名动作" prop="actionType" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">
            {{ row.enabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="signatureDialogVisible = false">关闭</el-button>
      <el-button type="primary" @click="openEditorFromSignatureDialog">去编辑配置</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import download from '@/utils/download'
import * as TemplateApi from '@/api/form-center/template'
import type {
  FormRecognizedFieldVO,
  FormTemplateListItemVO,
  FormTemplateObsoletePendingRespVO,
  FormTemplateStatus
} from '@/api/form-center/template'
import type {
  BatchRecordReportAssistRowVO,
  BatchRecordReportCellRuleVO,
  BatchRecordReportSignatureCellMarkerVO,
  BatchRecordReportCellValueType
} from '@/api/mes/pro/batchrecordreport'
import type { EdhrProcessFormFillAssignment } from '@/api/mes/pro/edhr/processFormPermissionRule'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import TemplateImportDialog from './components/TemplateImportDialog.vue'
import FormTemplateDesignerWrapper from './components/FormTemplateDesignerWrapper.vue'
import FormTemplateFillConfigDialog, {
  type FormTemplateFillConfigSavePayload
} from './components/FormTemplateFillConfigDialog.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { formatDate } from '@/utils/formatTime'
import EdhrExecutionReadonlyForm from '@/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'
import EdhrExecutionTemplateEditableForm from '@/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue'
import type { EdhrBatchExecutionReviewFormViewModel } from '@/api/mes/pro/edhr/batchExecution'
import {
  buildTemplateFieldIdentity,
  cellRuleDefaultComponentMap,
  cellRuleValueTypeOptions,
  normalizeCellRule,
  normalizeTemplateCellMerge,
  stringifyTemplateCell,
  type TemplateEditableCellContext,
  type TemplateSimulationValueMap
} from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'

defineOptions({ name: 'FormCenterTemplate' })

const props = defineProps<{
  simulationOnly?: boolean
}>()

const message = useMessage()
const route = useRoute()
const router = useRouter()
const importDialogRef = ref()
const loading = ref(false)
const total = ref(0)
const list = ref<FormTemplateListItemVO[]>([])
const selectedTemplateKey = ref('')
const previewMaximized = ref(false)
const signatureDialogVisible = ref(false)
const fillConfigDialogVisible = ref(false)
const obsoleteRequestDialogVisible = ref(false)
const obsoleteRequestSubmitting = ref(false)
const rulesSaving = ref(false)
const fillConfigSaving = ref(false)
const templateRouteLoadError = ref('')
const templateFillValues = ref<TemplateSimulationValueMap>({})
const obsoletePendingByTemplateKey = ref<Record<string, FormTemplateObsoletePendingRespVO | null>>({})
const obsoleteRequestTarget = ref<FormTemplateListItemVO | null>(null)
const obsoleteRequestForm = reactive({
  reason: ''
})
const editableTemplateCellRules = ref<BatchRecordReportCellRuleVO[]>([])
const editableTemplateSheetLayoutJson = ref('')
const selectedRuleKey = ref('')
const consumedTemplateActionKey = ref('')
const isDesignerMode = computed(() => route.query.mode === 'designer')
const templateDesignerMode = computed<'preview' | 'edit'>(() =>
  route.query.templateMode === 'edit' ? 'edit' : 'preview'
)
const isTemplateSimulationMode = computed(() => props.simulationOnly)
const templateSimulationBackLabel = computed(() => {
  const value = Array.isArray(route.query.returnLabel) ? route.query.returnLabel[0] : route.query.returnLabel
  return typeof value === 'string' && value.trim() ? value.trim() : '返回表单模板'
})
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  templateName: undefined as string | undefined,
  status: undefined as FormTemplateStatus | undefined
})

const templateDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'templateName', label: '模板名称', minWidth: 165 },
  { key: 'currentEffectiveVersion', label: '当前生效版本', width: 120 },
  { key: 'pendingVersion', label: '待发布版本', width: 125 },
  { key: 'status', label: '状态', width: 95 },
  { key: 'updatedTime', label: '修改时间', width: 180 },
  { key: 'remark', label: '备注', minWidth: 180 }
]

const {
  columns: templateColumns,
  saving: templateColumnSaving,
  isColumnVisible: isTemplateColumnVisible,
  getColumnWidthString: getTemplateColumnWidthString,
  getColumnMinWidthString: getTemplateColumnMinWidthString,
  handleHeaderDragend: handleTemplateHeaderDragend,
  saveConfig: saveTemplateColumnConfig,
  resetConfig: resetTemplateColumnConfig
} = useUserTableColumns('form.center.template', templateDefaultColumns)

const quickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'templateName',
    label: '模板名称',
    type: 'text',
    queryParamKey: 'templateName',
    placeholder: '请输入模板名称'
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: [
      { label: '草稿', value: 'DRAFT' },
      { label: '审批中', value: 'PENDING_APPROVAL' },
      { label: '已驳回', value: 'REJECTED' },
      { label: '已发布', value: 'PUBLISHED' },
      { label: '已停用', value: 'DISABLED' },
      { label: '已作废', value: 'OBSOLETE' }
    ]
  }
])

const templateRowKey = (row: FormTemplateListItemVO) => `${row.templateId}:${row.versionNo}`
const ruleIdentity = (rule: Pick<BatchRecordReportCellRuleVO, 'rowIndex' | 'columnIndex'>) =>
  buildTemplateFieldIdentity(rule.rowIndex, rule.columnIndex)

const normalizeRuleSource = (source?: string) => {
  const normalized = String(source || '').trim().toUpperCase()
  return normalized || 'MANUAL'
}

const isConfirmedRule = (rule: BatchRecordReportCellRuleVO) =>
  Boolean(rule.reviewed) && normalizeRuleSource(rule.source) !== 'AUTO'

const selectedTemplate = computed(() =>
  list.value.find((item) => templateRowKey(item) === selectedTemplateKey.value)
)

const recognizedFields = computed(() => selectedTemplate.value?.recognizedFields || [])
const parsedTemplateJimuSchema = computed(() => parseTemplateJimuSchema(selectedTemplate.value?.jimuSchemaJson))
const baseTemplatePreviewCellRules = computed<BatchRecordReportCellRuleVO[]>(() =>
  buildRecognizedFieldCellRules(recognizedFields.value)
)
const templatePreviewCellRules = computed<BatchRecordReportCellRuleVO[]>(() => {
  const persistedRules = parsedTemplateJimuSchema.value?.cellRules || []
  const rules = persistedRules.length ? persistedRules : baseTemplatePreviewCellRules.value
  return sortCellRules(rules.map(normalizeCellRule))
})
const templatePreviewSignatureMarkers = computed<BatchRecordReportSignatureCellMarkerVO[]>(() => {
  const persistedMarkers = parsedTemplateJimuSchema.value?.signatureCellMarkers || []
  return persistedMarkers.length ? persistedMarkers : buildSignatureMarkersFromRules(templatePreviewCellRules.value)
})
const visualPreviewFormViewModel = computed(() =>
  buildTemplateVisualPreviewModel(
    selectedTemplate.value,
    templatePreviewCellRules.value,
    templatePreviewSignatureMarkers.value,
    parsedTemplateJimuSchema.value?.sheetLayoutJson
  )
)
const simulatedPreviewFormViewModel = computed(() =>
  buildSimulatedPreviewFormViewModel(
    visualPreviewFormViewModel.value,
    templatePreviewCellRules.value,
    templatePreviewSignatureMarkers.value,
    templateFillValues.value
  )
)
const editableTemplateSignatureMarkers = computed(() =>
  buildSignatureMarkersFromRules(editableTemplateCellRules.value)
)
const editableRulesPreviewFormViewModel = computed(() =>
  buildTemplateVisualPreviewModel(
    selectedTemplate.value,
    editableTemplateCellRules.value,
    editableTemplateSignatureMarkers.value,
    editableTemplateSheetLayoutJson.value || parsedTemplateJimuSchema.value?.sheetLayoutJson
  )
)
const templateSignatureRows = computed(() => templatePreviewSignatureMarkers.value)
const valueTypeLabelMap = Object.fromEntries(
  cellRuleValueTypeOptions.map((option) => [option.value, option.label])
) as Record<string, string>
const pendingTemplateRuleCount = computed(
  () => editableTemplateCellRules.value.filter((rule) => !isConfirmedRule(rule)).length
)
const editableRulesSheetLayoutState = computed(() =>
  parseRuleEditorSheetLayout(editableRulesPreviewFormViewModel.value?.sheetLayoutJson)
)
const editableRulesSheetLayout = computed(() => editableRulesSheetLayoutState.value.layout)
const editableRulesSheetLayoutError = computed(() => editableRulesSheetLayoutState.value.error)
const ruleMap = computed(() => {
  const map = new Map<string, BatchRecordReportCellRuleVO>()
  editableTemplateCellRules.value.forEach((rule) => map.set(ruleIdentity(rule), rule))
  return map
})
const selectedRule = computed(() =>
  editableTemplateCellRules.value.find((rule) => ruleIdentity(rule) === selectedRuleKey.value)
)
const renderedTemplateRuleColumns = computed<RuleEditorColumn[]>(() =>
  buildRuleEditorColumns(editableRulesSheetLayout.value, editableTemplateCellRules.value)
)
const renderedTemplateRuleRows = computed<RuleEditorRow[]>(() =>
  buildRuleEditorRows(
    editableRulesSheetLayout.value,
    editableTemplateCellRules.value,
    ruleMap.value,
    selectedRuleKey.value
  )
)
const selectedRuleCell = computed(() => {
  if (!selectedRuleKey.value) return null
  for (const row of renderedTemplateRuleRows.value) {
    const cell = row.cells.find((item) => item.identity === selectedRuleKey.value)
    if (cell) return cell
  }
  return null
})
const canSaveEditableRules = computed(
  () =>
    Boolean(selectedTemplate.value) &&
    selectedTemplate.value?.status === 'DRAFT' &&
    Boolean(editableRulesSheetLayout.value) &&
    !editableRulesSheetLayoutError.value &&
    !rulesSaving.value
)
const rulesDialogTitle = computed(() => '编辑表单模板')
const rulesDialogPreviewTitle = computed(() => '模板编辑预览')
const rulesDialogPreviewTip = computed(() => '编辑单元格填写规则后会同步刷新模板预览、徽标和签名位。')
const rulesReadonlyTitle = computed(() => '当前版本只读：只有草稿版本可以保存模板编辑。')
const rulesSaveButtonText = computed(() => '保存模板')

const componentFlagBaseOptions = [
  { label: '文本输入 input-text', value: 'input-text' },
  { label: '数字输入 input-number', value: 'input-number' },
  { label: '日期 date', value: 'date' },
  { label: '日期时间 datetime', value: 'datetime' },
  { label: '复选框 checkbox', value: 'checkbox' },
  { label: '电子签名 signature', value: 'signature' },
  { label: '多行文本 textarea', value: 'textarea' },
  { label: '文件上传 upload-file', value: 'upload-file' },
  { label: '图片上传 upload-image', value: 'upload-image' }
]

const componentFlagOptions = computed(() => {
  const optionMap = new Map(componentFlagBaseOptions.map((option) => [option.value, option]))
  editableTemplateCellRules.value.forEach((rule) => {
    const value = String(rule.componentFlag || '').trim()
    if (value && !optionMap.has(value)) {
      optionMap.set(value, { label: value, value })
    }
  })
  return Array.from(optionMap.values())
})

const isSelectedTemplateRow = (row: FormTemplateListItemVO) =>
  templateRowKey(row) === selectedTemplateKey.value

const templateRowClassName = ({ row }: { row: FormTemplateListItemVO }) =>
  isSelectedTemplateRow(row) ? 'is-selected-template' : ''

const syncSelectedTemplate = () => {
  if (selectedTemplate.value) return
  selectedTemplateKey.value = list.value.length ? templateRowKey(list.value[0]) : ''
}

const getList = async () => {
  loading.value = true
  templateRouteLoadError.value = ''
  try {
    if (isDesignerMode.value || isTemplateSimulationMode.value) {
      list.value = []
      total.value = 0
      await syncTemplateRouteContext()
      return
    }
    const data = await TemplateApi.getTemplatePool(queryParams)
    list.value = data.list
    total.value = data.total
    syncSelectedTemplate()
    await syncTemplateRouteContext()
    await refreshSelectedTemplateObsoletePending()
    await handleTemplateActionQuery()
  } catch (error) {
    if (isDesignerMode.value || isTemplateSimulationMode.value) {
      templateRouteLoadError.value = resolveErrorMessage(error, '表单模板加载失败，请联系管理员。')
      return
    }
    throw error
  } finally {
    loading.value = false
  }
}

const quickFilter = useTableQuickFilter('form.center.template', quickFilterDefinitions, queryParams, getList)

const handleQuery = async () => {
  await quickFilter.applyQuickFilter()
}

const resetQuery = async () => {
  await quickFilter.resetQuickFilter()
}

const openImport = () => {
  importDialogRef.value?.open()
}

const selectTemplate = (row: FormTemplateListItemVO) => {
  selectedTemplateKey.value = templateRowKey(row)
  void refreshSelectedTemplateObsoletePending()
}

const refreshSelectedTemplateObsoletePending = async () => {
  if (!selectedTemplate.value) return
  await loadTemplateObsoletePending(selectedTemplate.value)
}

const loadTemplateObsoletePending = async (row: FormTemplateListItemVO) => {
  const key = templateRowKey(row)
  if (row.status !== 'PENDING_APPROVAL') {
    obsoletePendingByTemplateKey.value = {
      ...obsoletePendingByTemplateKey.value,
      [key]: null
    }
    return
  }
  const pending = await TemplateApi.findTemplateObsoletePendingRequest(row.templateId, row.versionNo)
  obsoletePendingByTemplateKey.value = {
    ...obsoletePendingByTemplateKey.value,
    [key]: pending
  }
}

const selectedTemplateObsoletePending = computed(() => {
  if (!selectedTemplate.value) return null
  return obsoletePendingByTemplateKey.value[templateRowKey(selectedTemplate.value)] || null
})

const openSelectedTemplateWorkspace = async (templateMode: 'preview' | 'edit') => {
  if (!selectedTemplate.value) return
  const row = selectedTemplate.value
  await router.push({
    path: route.path,
    query: {
      templateId: row.templateId,
      versionNo: row.versionNo,
      mode: 'designer',
      templateMode
    }
  })
}

const openSelectedTemplate = async () => {
  await openSelectedTemplateWorkspace('preview')
}

const editSelectedTemplate = async () => {
  await openSelectedTemplateWorkspace('edit')
}

const openSelectedTemplateFill = async () => {
  if (!selectedTemplate.value) return
  const row = selectedTemplate.value
  await router.push({
    path: '/mdm/form-center/template/simulate',
    query: {
      templateId: row.templateId,
      versionNo: row.versionNo,
      returnTo: route.fullPath,
      returnLabel: '返回表单模板'
    }
  })
}

const openSelectedTemplateCellLinks = async () => {
  if (!selectedTemplate.value) return
  const row = selectedTemplate.value
  await router.push({
    path: '/mes/pro/batch-record-cell-link',
    query: {
      templateId: row.templateId,
      versionNo: row.versionNo,
      returnTo: route.fullPath,
      returnLabel: '返回表单模板'
    }
  })
}

const openSelectedTemplateFillConfig = () => {
  if (!selectedTemplate.value) return
  fillConfigDialogVisible.value = true
}

type FormTemplateAction = 'signature'
type FormTemplateObsoleteOperationState = 'normal' | 'pending-withdrawable' | 'pending-readonly' | 'voided'

const normalizeRouteQueryText = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' && rawValue.trim() ? rawValue.trim() : ''
}

const normalizeTemplateAction = (value: unknown): FormTemplateAction | '' => {
  const action = normalizeRouteQueryText(value)
  return action === 'signature' ? action : ''
}

const buildTemplateActionKey = (row: FormTemplateListItemVO, action: FormTemplateAction) =>
  `${row.templateId}:${row.versionNo}:${action}`

const findTemplateFromRoute = () => {
  const templateId = Number(normalizeRouteQueryText(route.query.templateId))
  const versionNo = normalizeRouteQueryText(route.query.versionNo)
  if (!Number.isInteger(templateId) || templateId <= 0 || !versionNo) return undefined
  return list.value.find((item) => item.templateId === templateId && item.versionNo === versionNo)
}

const syncTemplateRouteContext = async () => {
  const templateId = Number(normalizeRouteQueryText(route.query.templateId))
  const versionNo = normalizeRouteQueryText(route.query.versionNo)
  const routeNeedsTemplate = isDesignerMode.value || isTemplateSimulationMode.value
  if (!Number.isInteger(templateId) || templateId <= 0 || !versionNo) {
    if (routeNeedsTemplate) {
      templateRouteLoadError.value = '缺少有效模板 ID 或版本号，无法打开表单模板工作区。'
      selectedTemplateKey.value = ''
    }
    return
  }

  const requiresExactTemplateVersion = isDesignerMode.value || isTemplateSimulationMode.value
  let row = requiresExactTemplateVersion
    ? await TemplateApi.getTemplateVersion(templateId, versionNo)
    : findTemplateFromRoute()
  if (!row) {
    row = await TemplateApi.getTemplateVersion(templateId, versionNo)
  }
  list.value = [
    row,
    ...list.value.filter((item) => templateRowKey(item) !== templateRowKey(row))
  ]
  selectedTemplateKey.value = templateRowKey(row)
  if (isDesignerMode.value && templateDesignerMode.value === 'edit') {
    reloadEditableTemplateRules()
  }
  if (isTemplateSimulationMode.value) {
    resetTemplateFillValues()
  }
}

const returnToTemplateList = async () => {
  const row = selectedTemplate.value
  await router.push({
    path: '/mdm/form-center/template',
    query: row
      ? {
          templateId: row.templateId,
          versionNo: row.versionNo
        }
      : {}
  })
}

const returnFromTemplateSimulation = async () => {
  const returnTo = normalizeRouteQueryText(route.query.returnTo)
  if (returnTo) {
    await router.push(returnTo)
    return
  }
  await returnToTemplateList()
}

const openTemplateActionDialog = (row: FormTemplateListItemVO, action: FormTemplateAction) => {
  selectedTemplateKey.value = templateRowKey(row)
  consumedTemplateActionKey.value = buildTemplateActionKey(row, action)
  signatureDialogVisible.value = true
}

const openSelectedTemplateAction = async (action: FormTemplateAction) => {
  if (!selectedTemplate.value) return
  const row = selectedTemplate.value
  await router.push({
    path: route.path,
    query: {
      ...route.query,
      templateId: row.templateId,
      versionNo: row.versionNo,
      action
    }
  })
  openTemplateActionDialog(row, action)
}

const handleTemplateActionQuery = async () => {
  const action = normalizeTemplateAction(route.query.action)
  if (!action) {
    consumedTemplateActionKey.value = ''
    return
  }
  const row = findTemplateFromRoute()
  if (!row) return
  const actionKey = buildTemplateActionKey(row, action)
  if (consumedTemplateActionKey.value === actionKey) return
  openTemplateActionDialog(row, action)
}

const reloadEditableTemplateRules = () => {
  const nextRules = templatePreviewCellRules.value.map((rule) => ({ ...rule }))
  editableTemplateSheetLayoutJson.value = visualPreviewFormViewModel.value?.sheetLayoutJson || ''
  editableTemplateCellRules.value = nextRules
  selectedRuleKey.value = nextRules.length ? ruleIdentity(nextRules[0]) : ''
}

const selectRuleCell = (cell: RuleEditorCell) => {
  selectedRuleKey.value = cell.identity
}

const buildManualRuleFromCell = (cell: RuleEditorCell): BatchRecordReportCellRuleVO =>
  normalizeCellRule({
    rowIndex: cell.rowIndex,
    columnIndex: cell.columnIndex,
    valueType: 'STRING',
    componentFlag: cellRuleDefaultComponentMap.STRING,
    required: false,
    label: cell.text.trim() || `第 ${cell.rowIndex + 1} 行第 ${cell.columnIndex + 1} 列`,
    placeholder: '',
    helpText: '',
    constraints: {},
    unit: '',
    source: 'MANUAL',
    confidence: 1,
    reviewed: true
  })

const enableSelectedCellRule = () => {
  const cell = selectedRuleCell.value
  if (!cell || ruleMap.value.has(cell.identity)) return
  editableTemplateCellRules.value = sortCellRules([
    ...editableTemplateCellRules.value,
    buildManualRuleFromCell(cell)
  ])
  selectedRuleKey.value = cell.identity
}

const disableSelectedCellRule = () => {
  const key = selectedRuleKey.value
  if (!key || !ruleMap.value.has(key)) return
  editableTemplateCellRules.value = editableTemplateCellRules.value.filter(
    (rule) => ruleIdentity(rule) !== key
  )
  selectedRuleKey.value = key
}

const isSelectedCellFillable = computed({
  get: () => Boolean(selectedRule.value),
  set: (value: boolean) => {
    if (value) {
      enableSelectedCellRule()
      return
    }
    disableSelectedCellRule()
  }
})

const handleSelectedValueTypeChange = (value: BatchRecordReportCellValueType) => {
  if (!selectedRule.value) return
  selectedRule.value.componentFlag = cellRuleDefaultComponentMap[value]
}

const saveEditableTemplateRules = async () => {
  if (!selectedTemplate.value) return
  if (selectedTemplate.value.status !== 'DRAFT') {
    message.warning('只有草稿版本可以保存模板编辑。')
    return
  }
  const rules = sortCellRules(editableTemplateCellRules.value.map((rule) => ({
    ...normalizeCellRule(rule),
    source: 'MANUAL',
    confidence: 1,
    reviewed: true
  })))
  const markers = buildSignatureMarkersFromRules(rules)
  const formViewModel = buildTemplateVisualPreviewModel(
    selectedTemplate.value,
    rules,
    markers,
    editableRulesPreviewFormViewModel.value?.sheetLayoutJson
  )
  if (!formViewModel) {
    message.error('当前模板缺少可保存的规则布局。')
    return
  }
  const payload = buildTemplateJimuSchemaPayload({
    sheetLayoutJson: formViewModel.sheetLayoutJson,
    cellRules: rules,
    signatureCellMarkers: markers,
    assistRows: parsedTemplateJimuSchema.value?.assistRows,
    fillAssignments: parsedTemplateJimuSchema.value?.fillAssignments
  })
  rulesSaving.value = true
  try {
    await TemplateApi.saveTemplateJimuSchema(
      selectedTemplate.value.templateId,
      selectedTemplate.value.versionNo,
      payload
    )
    selectedTemplate.value.jimuSchemaJson = payload
    message.success('模板保存成功')
    await getList()
  } finally {
    rulesSaving.value = false
  }
}

const saveSelectedTemplateFillConfig = async (data: FormTemplateFillConfigSavePayload) => {
  if (!selectedTemplate.value) return
  if (selectedTemplate.value.status !== 'DRAFT') {
    message.warning('只有草稿版本可以保存填写配置。')
    return
  }
  const rules = sortCellRules(data.cellRules.map(normalizeCellRule))
  const markers = data.signatureCellMarkers || buildSignatureMarkersFromRules(rules)
  const formViewModel = buildTemplateVisualPreviewModel(
    selectedTemplate.value,
    rules,
    markers,
    data.sheetLayoutJson
  )
  if (!formViewModel) {
    message.error('当前模板缺少可保存的规则布局。')
    return
  }
  const payload = buildTemplateJimuSchemaPayload({
    sheetLayoutJson: formViewModel.sheetLayoutJson,
    cellRules: rules,
    signatureCellMarkers: markers,
    assistRows: data.assistRows,
    fillAssignments: data.fillAssignments
  })
  fillConfigSaving.value = true
  try {
    await TemplateApi.saveTemplateJimuSchema(
      selectedTemplate.value.templateId,
      selectedTemplate.value.versionNo,
      payload
    )
    selectedTemplate.value.jimuSchemaJson = payload
    fillConfigDialogVisible.value = false
    message.success('填写配置已保存')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '填写配置保存失败，请联系管理员。'))
  } finally {
    fillConfigSaving.value = false
  }
}

const openEditorFromSignatureDialog = () => {
  if (!selectedTemplate.value) return
  signatureDialogVisible.value = false
  void openSelectedTemplateWorkspace('edit')
}

const handleTemplatePreviewSignatureAction = (_context: TemplateEditableCellContext) => {
  message.warning('模板模拟填写不保存电子签名；请在具体业务表单实例中完成签名。')
}

const enterPreviewMaximize = () => {
  if (!selectedTemplate.value) return
  previewMaximized.value = true
}

const restorePreviewLayout = () => {
  previewMaximized.value = false
}

const downloadSource = async (row: FormTemplateListItemVO) => {
  const blob = await TemplateApi.downloadTemplateSourceFile(row.templateId, row.versionNo)
  download.word(blob, `${row.templateName}-${row.versionNo}.docx`)
}

const downloadSelectedTemplateSource = async () => {
  if (!selectedTemplate.value) return
  await downloadSource(selectedTemplate.value)
}

const publishTemplate = async (row: FormTemplateListItemVO) => {
  await TemplateApi.publishTemplateVersion(row.templateId, row.versionNo)
  message.success('已发布')
  await getList()
}

const publishSelectedTemplate = async () => {
  if (!selectedTemplate.value) return
  await publishTemplate(selectedTemplate.value)
}

const disableTemplate = async (row: FormTemplateListItemVO) => {
  await TemplateApi.disableTemplateVersion(row.templateId, row.versionNo)
  message.success('已停用')
  await getList()
}

const disableSelectedTemplate = async () => {
  if (!selectedTemplate.value) return
  await disableTemplate(selectedTemplate.value)
}

const enableTemplate = async (row: FormTemplateListItemVO) => {
  await TemplateApi.enableTemplateVersion(row.templateId, row.versionNo)
  message.success('已启用')
  await getList()
}

const enableSelectedTemplate = async () => {
  if (!selectedTemplate.value) return
  await enableTemplate(selectedTemplate.value)
}

const obsoleteSelectedTemplate = async () => {
  if (!selectedTemplate.value) return
  obsoleteRequestTarget.value = selectedTemplate.value
  obsoleteRequestForm.reason = ''
  obsoleteRequestDialogVisible.value = true
}

const submitSelectedTemplateObsoleteRequest = async () => {
  const target = obsoleteRequestTarget.value
  const reason = obsoleteRequestForm.reason.trim()
  if (!target) return
  if (!reason) {
    message.warning('请输入作废原因。')
    return
  }
  obsoleteRequestSubmitting.value = true
  try {
    await TemplateApi.submitTemplateObsoleteRequest(target.templateId, target.versionNo, { reason })
    message.success('已提交作废申请，等待审批通过后生效')
    obsoleteRequestDialogVisible.value = false
    await getList()
  } finally {
    obsoleteRequestSubmitting.value = false
  }
}

const withdrawSelectedTemplateObsoleteRequest = async () => {
  if (!selectedTemplate.value || !selectedTemplateObsoletePending.value?.approvalProcessInstanceId) return
  try {
    await message.delConfirm('确认撤回当前表单模板作废申请吗？撤回后模板会恢复申请前状态。')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
  await TemplateApi.withdrawTemplateObsoleteRequest(
    selectedTemplate.value.templateId,
    selectedTemplate.value.versionNo,
    '申请人撤回表单模板作废申请'
  )
  message.success('已撤回作废申请')
  await getList()
}

const statusLabel = (status: FormTemplateStatus) => {
  const labels: Record<FormTemplateStatus, string> = {
    DRAFT: '草稿',
    PENDING_APPROVAL: '审批中',
    REJECTED: '已驳回',
    READY: '待发布',
    PUBLISHED: '已发布',
    DISABLED: '已停用',
    OBSOLETE: '已作废'
  }
  return labels[status]
}

const statusTagType = (status: FormTemplateStatus) => {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'DISABLED' || status === 'OBSOLETE') return 'info'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

const isCurrentEffectiveVersion = (status: FormTemplateStatus) => status === 'PUBLISHED'

const isPendingTemplateVersion = (status: FormTemplateStatus) => {
  return ['DRAFT', 'READY', 'PENDING_APPROVAL', 'REJECTED'].includes(status)
}

const pendingVersionTagType = (status: FormTemplateStatus) => {
  if (status === 'DRAFT' || status === 'READY') return 'info'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

const canPublishTemplate = (row: FormTemplateListItemVO) => {
  return row.status !== 'PUBLISHED' && row.status !== 'OBSOLETE' && row.status !== 'PENDING_APPROVAL'
}

const canDisableTemplate = (row: FormTemplateListItemVO) => {
  return !['PENDING_APPROVAL', 'DISABLED', 'OBSOLETE'].includes(row.status)
}

const canEnableTemplate = (row: FormTemplateListItemVO) => {
  return row.status === 'DISABLED'
}

const resolveTemplateObsoleteOperationState = (
  row: FormTemplateListItemVO
): FormTemplateObsoleteOperationState => {
  if (row.status === 'OBSOLETE') return 'voided'
  const pending = obsoletePendingByTemplateKey.value[templateRowKey(row)]
  if (pending?.canWithdraw) return 'pending-withdrawable'
  if (pending || row.status === 'PENDING_APPROVAL') return 'pending-readonly'
  return 'normal'
}

const canUseTemplateInteractiveAction = (row: FormTemplateListItemVO) => {
  return row.status !== 'OBSOLETE' && resolveTemplateObsoleteOperationState(row) === 'normal'
}

const canObsoleteTemplate = (row: FormTemplateListItemVO) => {
  return row.status !== 'PENDING_APPROVAL' && row.status !== 'OBSOLETE' && resolveTemplateObsoleteOperationState(row) === 'normal'
}

const formatTemplateUpdatedTime = (value?: string | number | Date) => {
  if (!value) return '-'
  const normalizedValue = typeof value === 'string' && /^\d+$/.test(value) ? Number(value) : value
  return formatDate(normalizedValue as Date, 'YYYY-MM-DD HH:mm:ss')
}

type TemplateVisualPreviewCell = {
  value?: unknown
  text?: string
  merge?: [number, number]
  fillForm?: Record<string, unknown>
  edhrCellRule?: Record<string, unknown>
  edhrSignature?: BatchRecordReportSignatureCellMarkerVO
}

type TemplateVisualPreviewRow = {
  height?: number
  cells: Record<string, TemplateVisualPreviewCell>
}

type RuleEditorRawLayout = {
  cols?: Record<string, { width?: number }>
  rows?: Record<string, TemplateVisualPreviewRow>
}

type RuleEditorColumn = {
  columnIndex: number
  widthPercent: number
}

type RuleEditorCell = {
  identity: string
  rowIndex: number
  columnIndex: number
  text: string
  rowSpan: number
  colSpan: number
  rule?: BatchRecordReportCellRuleVO
  classNames: Record<string, boolean>
}

type RuleEditorRow = {
  rowIndex: number
  height: number
  cells: RuleEditorCell[]
}

type FormTemplateJimuSchemaPayload = {
  [key: string]: unknown
  sheetLayoutJson?: string
  cellRules?: BatchRecordReportCellRuleVO[]
  signatureCellMarkers?: BatchRecordReportSignatureCellMarkerVO[]
  assistRows?: BatchRecordReportAssistRowVO[]
  fillAssignments?: EdhrProcessFormFillAssignment[]
}

const fieldValueType = (fieldType?: string): BatchRecordReportCellValueType => {
  const normalized = String(fieldType || '').toLowerCase()
  if (normalized === 'number') return 'NUMBER'
  if (normalized === 'date') return 'DATE'
  if (normalized === 'datetime') return 'DATETIME'
  if (normalized === 'checkbox') return 'BOOLEAN'
  if (normalized === 'signature') return 'SIGNATURE'
  return 'STRING'
}

const fieldComponentFlag = (fieldType?: string) => {
  const normalized = String(fieldType || '').toLowerCase()
  if (normalized === 'number') return 'input-number'
  if (normalized === 'date') return 'date'
  if (normalized === 'datetime') return 'datetime'
  if (normalized === 'checkbox') return 'checkbox'
  if (normalized === 'signature') return 'signature'
  if (normalized === 'textarea') return 'textarea'
  return 'input-text'
}

const fieldPlaceholder = (field: FormRecognizedFieldVO) => {
  if (field.fieldType === 'checkbox') return '□'
  return '?'
}

const buildRecognizedFieldCellRules = (fields: FormRecognizedFieldVO[]) =>
  fields.map((field, index) => {
    const rowIndex = Math.floor(index / 2) + 3
    const labelColumnIndex = index % 2 === 0 ? 0 : 2
    const inputColumnIndex = labelColumnIndex + 1
    return {
      rowIndex,
      columnIndex: inputColumnIndex,
      valueType: fieldValueType(field.fieldType),
      componentFlag: fieldComponentFlag(field.fieldType),
      required: field.required,
      label: field.label || field.fieldCode,
      placeholder: fieldPlaceholder(field),
      source: 'AUTO',
      reviewed: false
    } as BatchRecordReportCellRuleVO
  })

const parseTemplateJimuSchema = (schema?: string): FormTemplateJimuSchemaPayload | undefined => {
  if (!schema?.trim()) return undefined
  const parsed = JSON.parse(schema) as FormTemplateJimuSchemaPayload
  return {
    ...parsed,
    sheetLayoutJson: typeof parsed.sheetLayoutJson === 'string' ? parsed.sheetLayoutJson : undefined,
    cellRules: Array.isArray(parsed.cellRules) ? parsed.cellRules : undefined,
    signatureCellMarkers: Array.isArray(parsed.signatureCellMarkers)
      ? parsed.signatureCellMarkers
      : undefined,
    assistRows: Array.isArray(parsed.assistRows) ? parsed.assistRows : undefined,
    fillAssignments: Array.isArray(parsed.fillAssignments) ? parsed.fillAssignments : undefined
  }
}

const buildTemplateJimuSchemaPayload = (payload: FormTemplateJimuSchemaPayload) =>
  JSON.stringify({
    ...(parsedTemplateJimuSchema.value || {}),
    ...payload
  })

const sortCellRules = (rules: BatchRecordReportCellRuleVO[]) =>
  [...rules].sort((left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex)

const DEFAULT_RULE_EDITOR_COLUMN_WIDTH = 150
const DEFAULT_RULE_EDITOR_ROW_HEIGHT = 34

const resolveErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message.trim()) return error.message
  if (typeof error === 'string' && error.trim()) return error
  const dataMessage = (error as any)?.msg || (error as any)?.message
  if (typeof dataMessage === 'string' && dataMessage.trim()) return dataMessage
  return fallback
}

const parseRuleEditorSheetLayout = (sheetLayoutJson?: string) => {
  if (!sheetLayoutJson?.trim()) {
    return { layout: null, error: '当前模板缺少表单布局，无法进入可视化规则编辑。' }
  }
  try {
    const parsed = JSON.parse(sheetLayoutJson) as RuleEditorRawLayout
    if (!parsed?.rows || !Object.keys(parsed.rows).length) {
      throw new Error('当前模板缺少有效表单布局，无法进入可视化规则编辑。')
    }
    return { layout: parsed, error: '' }
  } catch (error) {
    return { layout: null, error: resolveErrorMessage(error, '表单布局解析失败，无法进入可视化规则编辑。') }
  }
}

const collectRuleEditorColumnIndexes = (
  layout: RuleEditorRawLayout | null,
  rules: BatchRecordReportCellRuleVO[]
) => {
  const columns = new Set<number>()
  Object.keys(layout?.cols || {}).forEach((key) => {
    const columnIndex = Number(key)
    if (Number.isInteger(columnIndex)) columns.add(columnIndex)
  })
  Object.values(layout?.rows || {}).forEach((row) => {
    Object.keys(row.cells || {}).forEach((key) => {
      const columnIndex = Number(key)
      if (Number.isInteger(columnIndex)) columns.add(columnIndex)
    })
  })
  rules.forEach((rule) => columns.add(rule.columnIndex))
  return Array.from(columns).sort((a, b) => a - b)
}

const collectRuleEditorRowIndexes = (
  layout: RuleEditorRawLayout | null,
  rules: BatchRecordReportCellRuleVO[]
) => {
  const rows = new Set<number>()
  Object.keys(layout?.rows || {}).forEach((key) => {
    const rowIndex = Number(key)
    if (Number.isInteger(rowIndex)) rows.add(rowIndex)
  })
  rules.forEach((rule) => rows.add(rule.rowIndex))
  return Array.from(rows).sort((a, b) => a - b)
}

const buildRuleEditorColumns = (
  layout: RuleEditorRawLayout | null,
  rules: BatchRecordReportCellRuleVO[]
): RuleEditorColumn[] => {
  if (!layout) return []
  const columnIndexes = collectRuleEditorColumnIndexes(layout, rules)
  const widths = columnIndexes.map((columnIndex) => {
    const configuredWidth = Number(layout.cols?.[String(columnIndex)]?.width)
    return Number.isFinite(configuredWidth) && configuredWidth > 0
      ? configuredWidth
      : DEFAULT_RULE_EDITOR_COLUMN_WIDTH
  })
  const totalWidth = widths.reduce((sum, width) => sum + width, 0)
  return columnIndexes.map((columnIndex, index) => ({
    columnIndex,
    widthPercent: totalWidth > 0 ? (widths[index] / totalWidth) * 100 : 100
  }))
}

const buildCoveredRuleEditorCellSet = (layout: RuleEditorRawLayout | null) => {
  const covered = new Set<string>()
  Object.entries(layout?.rows || {}).forEach(([rowKey, row]) => {
    const rowIndex = Number(rowKey)
    if (!Number.isInteger(rowIndex)) return
    Object.entries(row.cells || {}).forEach(([columnKey, cell]) => {
      const columnIndex = Number(columnKey)
      if (!Number.isInteger(columnIndex)) return
      const merge = normalizeTemplateCellMerge(cell)
      for (let rowOffset = 0; rowOffset < merge.rowSpan; rowOffset += 1) {
        for (let columnOffset = 0; columnOffset < merge.colSpan; columnOffset += 1) {
          if (rowOffset === 0 && columnOffset === 0) continue
          covered.add(`${rowIndex + rowOffset}:${columnIndex + columnOffset}`)
        }
      }
    })
  })
  return covered
}

const resolveRuleEditorRowHeight = (height: unknown) => {
  const numericHeight = Number(height)
  return Math.max(
    Number.isFinite(numericHeight) && numericHeight > 0
      ? numericHeight
      : DEFAULT_RULE_EDITOR_ROW_HEIGHT,
    28
  )
}

const buildRuleEditorRows = (
  layout: RuleEditorRawLayout | null,
  rules: BatchRecordReportCellRuleVO[],
  rulesByCell: Map<string, BatchRecordReportCellRuleVO>,
  activeRuleKey: string
): RuleEditorRow[] => {
  if (!layout) return []
  const columnIndexes = collectRuleEditorColumnIndexes(layout, rules)
  const coveredCells = buildCoveredRuleEditorCellSet(layout)
  return collectRuleEditorRowIndexes(layout, rules).map((rowIndex) => {
    const rawRow = layout.rows?.[String(rowIndex)] || { cells: {} }
    const cells: RuleEditorCell[] = []
    columnIndexes.forEach((columnIndex) => {
      const identity = `${rowIndex}:${columnIndex}`
      if (coveredCells.has(identity)) return
      const rawCell = rawRow.cells?.[String(columnIndex)]
      const merge = normalizeTemplateCellMerge(rawCell)
      const text = stringifyTemplateCell(rawCell?.value ?? rawCell?.text)
      const rule = rulesByCell.get(identity)
      cells.push({
        identity,
        rowIndex,
        columnIndex,
        text,
        rowSpan: merge.rowSpan,
        colSpan: merge.colSpan,
        rule,
        classNames: {
          'batch-record-cell-rules-editor__cell': true,
          'is-empty': !text.trim(),
          'is-rule': Boolean(rule),
          'is-required': Boolean(rule?.required),
          'is-selected': activeRuleKey === identity
        }
      })
    })
    return {
      rowIndex,
      height: resolveRuleEditorRowHeight(rawRow.height),
      cells
    }
  })
}

const isSignatureRule = (rule: BatchRecordReportCellRuleVO) =>
  rule.valueType === 'SIGNATURE' || String(rule.componentFlag || '').toLowerCase().includes('signature')

const buildSignatureMarkersFromRules = (rules: BatchRecordReportCellRuleVO[]) =>
  sortCellRules(rules)
    .filter(isSignatureRule)
    .map((rule) => ({
      rowIndex: rule.rowIndex,
      columnIndex: rule.columnIndex,
      enabled: true,
      actionType: 'FORM_REVIEW',
      label: rule.label || '签名',
      signatureCellKey: buildTemplateFieldIdentity(rule.rowIndex, rule.columnIndex)
    }) as BatchRecordReportSignatureCellMarkerVO)

const mergeTemplateRulesIntoSheetLayout = (
  sheetLayoutJson: string,
  rules: BatchRecordReportCellRuleVO[],
  markers: BatchRecordReportSignatureCellMarkerVO[]
) => {
  const layout = JSON.parse(sheetLayoutJson) as RuleEditorRawLayout
  if (!layout?.rows || !Object.keys(layout.rows).length) {
    throw new Error('当前模板缺少有效表单布局，无法保存规则。')
  }
  Object.values(layout.rows).forEach((row) => {
    Object.values(row.cells || {}).forEach((cell) => {
      delete cell.fillForm
      delete cell.edhrCellRule
      delete cell.edhrSignature
    })
  })
  const markerMap = new Map(
    markers.map((marker) => [buildTemplateFieldIdentity(marker.rowIndex, marker.columnIndex), marker])
  )
  sortCellRules(rules).forEach((rule) => {
    const rowKey = String(rule.rowIndex)
    const columnKey = String(rule.columnIndex)
    if (!layout.rows![rowKey]) {
      layout.rows![rowKey] = { height: 36, cells: {} }
    }
    if (!layout.rows![rowKey].cells) {
      layout.rows![rowKey].cells = {}
    }
    const existingCell = layout.rows![rowKey].cells[columnKey] || {}
    const marker = markerMap.get(ruleIdentity(rule))
    layout.rows![rowKey].cells[columnKey] = {
      ...existingCell,
      fillForm: { ...rule },
      edhrCellRule: { ...rule },
      ...(marker ? { edhrSignature: marker } : {})
    }
  })
  markers.forEach((marker) => {
    const rowKey = String(marker.rowIndex)
    const columnKey = String(marker.columnIndex)
    if (!layout.rows![rowKey]) {
      layout.rows![rowKey] = { height: 36, cells: {} }
    }
    if (!layout.rows![rowKey].cells) {
      layout.rows![rowKey].cells = {}
    }
    layout.rows![rowKey].cells[columnKey] = {
      ...(layout.rows![rowKey].cells[columnKey] || {}),
      edhrSignature: marker
    }
  })
  return JSON.stringify(layout)
}

const resetTemplateFillValues = () => {
  const nextValues: TemplateSimulationValueMap = {}
  templatePreviewCellRules.value.forEach((rule) => {
    const fieldIdentity = buildTemplateFieldIdentity(rule.rowIndex, rule.columnIndex)
    if (isSignatureRule(rule)) return
    if (rule.valueType === 'BOOLEAN') {
      nextValues[fieldIdentity] = false
      return
    }
    if (rule.valueType === 'NUMBER') {
      nextValues[fieldIdentity] = null
      return
    }
    nextValues[fieldIdentity] = ''
  })
  templateFillValues.value = nextValues
}

const buildSimulatedPreviewFormViewModel = (
  baseViewModel: EdhrBatchExecutionReviewFormViewModel | undefined,
  rules: BatchRecordReportCellRuleVO[],
  markers: BatchRecordReportSignatureCellMarkerVO[],
  values: TemplateSimulationValueMap
): EdhrBatchExecutionReviewFormViewModel | undefined => {
  if (!baseViewModel) return undefined
  const cellValues = rules
    .filter((rule) => !isSignatureRule(rule))
    .map((rule) => {
      const rawValue = values[buildTemplateFieldIdentity(rule.rowIndex, rule.columnIndex)]
      if (rule.valueType === 'BOOLEAN') {
        return {
          rowIndex: rule.rowIndex,
          columnIndex: rule.columnIndex,
          valueType: rule.valueType,
          value: rawValue === true,
          valueDisplay: rawValue === true ? 'true' : 'false'
        }
      }
      return {
        rowIndex: rule.rowIndex,
        columnIndex: rule.columnIndex,
        valueType: rule.valueType,
        value: rawValue ?? '',
        valueDisplay: rawValue == null ? '' : String(rawValue),
        unit: rule.unit || undefined
      }
    })
  return {
    ...baseViewModel,
    cellValuesJson: JSON.stringify(cellValues),
    signatureCellMarkers: markers
  }
}

const buildTemplateVisualPreviewModel = (
  template: FormTemplateListItemVO | undefined,
  rules: BatchRecordReportCellRuleVO[],
  markers: BatchRecordReportSignatureCellMarkerVO[] = [],
  sheetLayoutJson?: string
): EdhrBatchExecutionReviewFormViewModel | undefined => {
  if (!template || (!rules.length && !sheetLayoutJson?.trim())) return undefined
  if (sheetLayoutJson?.trim()) {
    return {
      sheetLayoutJson: mergeTemplateRulesIntoSheetLayout(sheetLayoutJson, rules, markers),
      executionSnapshotJson: JSON.stringify({ fields: [] }),
      cellValuesJson: JSON.stringify([]),
      remark: template.remark || '',
      signatureCellMarkers: markers
    }
  }
  const rows: Record<string, TemplateVisualPreviewRow> = {
    '0': {
      height: 28,
      cells: {
        '0': { text: template.templateName, merge: [0, 1] },
        '2': { text: '记录编号' },
        '3': { text: `TPL-${template.templateId}` }
      }
    },
    '1': {
      height: 28,
      cells: {
        '0': { text: '版本' },
        '1': { text: template.versionNo },
        '2': { text: '版本状态' },
        '3': { text: statusLabel(template.status) }
      }
    },
    '2': {
      height: 26,
      cells: {
        '0': { text: '识别字段', merge: [0, 3] }
      }
    }
  }

  const markerMap = new Map(
    markers.map((marker) => [buildTemplateFieldIdentity(marker.rowIndex, marker.columnIndex), marker])
  )
  sortCellRules(rules).forEach((rule) => {
    const rowIndex = rule.rowIndex
    const labelColumnIndex = Math.max(0, rule.columnIndex - 1)
    const inputColumnIndex = rule.columnIndex
    const rowKey = String(rowIndex)
    if (!rows[rowKey]) {
      rows[rowKey] = { height: 36, cells: {} }
    }
    rows[rowKey].cells[String(labelColumnIndex)] = {
      text: `${rule.label || '字段'}${rule.required ? ' *' : ''}`
    }
    const marker = markerMap.get(buildTemplateFieldIdentity(rule.rowIndex, rule.columnIndex))
    rows[rowKey].cells[String(inputColumnIndex)] = {
      fillForm: {
        ...rule
      },
      edhrCellRule: {
        ...rule
      },
      ...(marker ? { edhrSignature: marker } : {})
    }
  })

  return {
    sheetLayoutJson: JSON.stringify({
      cols: {
        '0': { width: 140 },
        '1': { width: 220 },
        '2': { width: 140 },
        '3': { width: 220 }
      },
      rows
    }),
    executionSnapshotJson: JSON.stringify({ fields: [] }),
    cellValuesJson: JSON.stringify([]),
    remark: template.remark || '',
    signatureCellMarkers: markers
  }
}

onMounted(getList)

watch(
  () => [
    route.name,
    route.query.templateId,
    route.query.versionNo,
    route.query.mode,
    route.query.templateMode,
    route.query.action
  ] as const,
  async () => {
    templateRouteLoadError.value = ''
    try {
      await syncTemplateRouteContext()
      await handleTemplateActionQuery()
    } catch (error) {
      if (isDesignerMode.value || isTemplateSimulationMode.value) {
        templateRouteLoadError.value = resolveErrorMessage(error, '表单模板加载失败，请联系管理员。')
        return
      }
      throw error
    }
  }
)
</script>

<style scoped>
.form-template-page {
  border: none;
  background: transparent;
}

.form-template-page :deep(.el-card__body) {
  padding: 0 !important;
}

.form-template-route-workspace {
  border: none;
  background: transparent;
}

.form-template-route-workspace :deep(.el-card__body) {
  padding: 0 !important;
}

.form-template-route-workspace__header {
  display: grid;
  min-height: 58px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
  background: #f7f9fc;
}

.form-template-route-workspace__heading {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.form-template-route-workspace__heading span {
  color: #64748b;
  font-size: 12px;
}

.form-template-route-workspace__heading strong {
  overflow: hidden;
  color: #172033;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-template-route-workspace__body {
  min-height: calc(100vh - 190px);
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-top: 0;
  border-radius: 0 0 8px 8px;
  background: #ffffff;
  overflow: auto;
}

.form-template-route-workspace > .form-template-rule-workspace {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-top: 0;
  background: #ffffff;
}

.form-template-route-workspace__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border: 1px solid #dbe3ef;
  border-top: 0;
  border-radius: 0 0 8px 8px;
  background: #ffffff;
}

.form-template-workbench {
  display: grid;
  grid-template-columns: minmax(620px, 58%) minmax(420px, 42%);
  gap: 14px;
  min-height: 640px;
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.form-template-workbench__list,
.form-template-preview {
  min-width: 0;
}

.form-template-workbench__list :deep(.is-selected-template > td) {
  background: #eef7ff !important;
}

.form-template-preview {
  display: flex;
  flex-direction: column;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.form-template-preview__header {
  display: flex;
  min-height: 48px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px 14px;
  padding: 10px 12px;
  border-bottom: 1px solid #edf1f6;
  background: #f7f9fc;
}

.form-template-preview__heading {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.form-template-preview__eyebrow {
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
}

.form-template-preview__title {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-template-preview__actions {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 2px 8px;
  flex-wrap: wrap;
}

.form-template-preview__actions :deep(.el-button) {
  margin-left: 0;
}

.form-template-preview__body {
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.form-template-visual-preview {
  min-width: 0;
  overflow: hidden;
}

.form-template-visual-preview :deep(.edhr-readonly-form__sheet-wrap) {
  border-color: #8a94a6;
}

.form-template-visual-preview :deep(.edhr-template-sheet) {
  min-width: 680px;
  width: max(100%, 680px);
}

.template-version-current {
  color: #009688;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.template-version-empty {
  color: #8c8c8c;
  font-weight: 500;
}

.template-version-tag {
  min-width: 72px;
  justify-content: center;
  font-variant-numeric: tabular-nums;
}

.form-template-fill-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 42%);
  gap: 12px;
  min-height: 560px;
}

.form-template-fill-workspace__panel,
.form-template-rule-workspace__preview,
.form-template-rule-workspace__side {
  min-width: 0;
  min-height: 0;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.form-template-fill-workspace__panel,
.form-template-rule-workspace__preview {
  display: flex;
  flex-direction: column;
}

.form-template-dialog-panel-head {
  display: flex;
  min-height: 44px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #edf1f6;
  background: #f7f9fc;
}

.form-template-dialog-panel-head strong {
  color: #172033;
  font-size: 14px;
}

.form-template-dialog-panel-head span {
  color: #64748b;
  font-size: 12px;
}

.form-template-fill-workspace__surface,
.form-template-rule-workspace__preview {
  min-width: 0;
  min-height: 0;
  overflow: auto;
}

.form-template-fill-workspace__surface {
  flex: 1;
  padding: 10px;
}

.form-template-rule-workspace__preview {
  padding-bottom: 10px;
}

.form-template-rule-workspace__preview :deep(.edhr-readonly-form__sheet-wrap) {
  margin: 10px;
}

.form-template-rule-workspace__side {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  overflow: auto;
}

.batch-record-cell-rules-editor.form-template-rule-workspace {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
}

.batch-record-cell-rules-editor__summary {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 8px;
}

.batch-record-cell-rules-editor__name {
  min-width: 0;
  max-width: 360px;
  overflow: hidden;
  color: #172033;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-cell-rules-editor__mode {
  margin-left: auto;
  color: #5d667a;
  font-size: 12px;
}

.batch-record-cell-rules-editor__workspace {
  display: grid;
  height: clamp(520px, calc(100vh - 220px), 880px);
  min-height: 0;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 14px;
}

.batch-record-cell-rules-editor__preview,
.batch-record-cell-rules-editor__side-panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.batch-record-cell-rules-editor__preview {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.batch-record-cell-rules-editor__side-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  padding: 12px;
}

.batch-record-cell-rules-editor__panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #e8eef6;
  background: #f7f9fc;
}

.batch-record-cell-rules-editor__panel-head strong {
  display: block;
  color: #172033;
  font-size: 14px;
}

.batch-record-cell-rules-editor__panel-head p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.4;
}

.batch-record-cell-rules-editor__sheet-scroll {
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding: 12px;
  background: #f3f6fb;
}

.batch-record-cell-rules-editor__sheet {
  min-width: 760px;
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  background: #ffffff;
  color: #172033;
  font-size: 12px;
}

.batch-record-cell-rules-editor__cell {
  min-width: 72px;
  border: 1px solid #cfd8e6;
  background: #ffffff;
  padding: 0;
  vertical-align: stretch;
}

.batch-record-cell-rules-editor__cell.is-empty {
  background: #fbfcfe;
}

.batch-record-cell-rules-editor__cell.is-rule {
  background: #eff6ff;
}

.batch-record-cell-rules-editor__cell.is-required {
  background: #fff8ed;
}

.batch-record-cell-rules-editor__cell.is-rule.is-required {
  background: #eff6ff;
}

.batch-record-cell-rules-editor__cell.is-selected {
  outline: 2px solid #2563eb;
  outline-offset: -2px;
}

.batch-record-cell-rules-editor__cell-button {
  display: flex;
  width: 100%;
  min-height: 100%;
  align-items: stretch;
  justify-content: space-between;
  gap: 8px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
  padding: 8px;
  text-align: left;
}

.batch-record-cell-rules-editor__cell-button:hover {
  background: rgba(37, 99, 235, 0.08);
}

.batch-record-cell-rules-editor__cell-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: pre-wrap;
}

.batch-record-cell-rules-editor__cell-placeholder {
  color: #a0a8b8;
}

.batch-record-cell-rules-editor__cell-rule {
  display: inline-flex;
  flex: 0 0 auto;
  align-self: flex-start;
  align-items: center;
  gap: 4px;
  color: #2563eb;
  font-size: 11px;
  white-space: nowrap;
}

.batch-record-cell-rules-editor__cell-rule b {
  color: #c2410c;
  font-weight: 600;
}

.batch-record-cell-rules-editor__fillable-toggle {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fafcff;
}

.batch-record-cell-rules-editor__fillable-toggle strong {
  display: block;
  color: #172033;
  font-size: 13px;
}

.batch-record-cell-rules-editor__form {
  flex: 0 0 auto;
}

.form-template-focused-preview {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  background: #f7f9fc;
  color: #172033;
}

.form-template-focused-preview__control {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  padding: 18px 14px;
  border-right: 1px solid #dbe3ef;
  background: #ffffff;
}

.form-template-focused-preview__control span {
  color: #64748b;
  font-size: 12px;
}

.form-template-focused-preview__control strong {
  min-width: 0;
  overflow: hidden;
  color: #172033;
  font-size: 15px;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-template-focused-preview__stage {
  min-width: 0;
  min-height: 0;
  padding: 14px;
  overflow: hidden;
}

.form-template-focused-preview__body {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}
</style>
