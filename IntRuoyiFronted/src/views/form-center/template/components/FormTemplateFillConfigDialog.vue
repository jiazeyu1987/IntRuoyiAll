<template>
  <Dialog
    v-model="dialogVisible"
    class="scheme-d-basic-data-page scheme-d-basic-data-page--form-template scheme-d-form-control"
    title="填写配置"
    width="calc(100vw - 32px)"
    :fullscreen="true"
    :default-fullscreen="true"
  >
    <div v-loading="loading" class="batch-record-cell-rules-editor">
      <main class="batch-record-cell-rules-editor__main-panel">
        <el-alert
          v-if="readonlyMode"
          title="AI 自动识别可在任意版本执行；识别后会自动生成或复用草稿版本，应用并保存仍需草稿。"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          :closable="false"
          show-icon
        />

        <section class="batch-record-cell-rules-editor__summary">
          <span class="batch-record-cell-rules-editor__name">{{ templateName }}</span>
          <el-tag class="scheme-d-tag" type="primary" effect="plain">规则 {{ ruleRows.length }}</el-tag>
          <el-tag class="scheme-d-tag" :type="pendingCount > 0 ? 'warning' : 'success'" effect="plain">
            待确认 {{ pendingCount }}
          </el-tag>
          <el-radio-group
            v-model="activeConfigMode"
            size="small"
            class="batch-record-cell-rules-editor__mode-switch"
          >
            <el-radio-button label="原表单配置" value="source">原表单配置</el-radio-button>
            <el-radio-button label="辅助表单映射" value="assistMapping">辅助表单映射</el-radio-button>
          </el-radio-group>
          <span class="batch-record-cell-rules-editor__mode">
            {{
              activeConfigMode === 'assistMapping'
                ? '辅助表单映射：先选辅助格，再点未分配原表格'
                : '原表单配置：左侧选单元格，右侧维护字段类型'
            }}
          </span>
        </section>

        <section
          class="batch-record-cell-rules-editor__workspace"
          :class="{
            'batch-record-cell-rules-editor__workspace--assist-mapping':
              activeConfigMode === 'assistMapping'
          }"
        >
        <div class="batch-record-cell-rules-editor__preview" data-fill-config-panel="source-form">
          <div class="batch-record-cell-rules-editor__panel-head">
            <div>
              <strong>原表单</strong>
              <p>点击任意单元格只会选中规则目标，不会触发日期框、签名框或复选框。</p>
            </div>
            <el-tag class="scheme-d-tag" type="info" effect="plain">只读</el-tag>
          </div>

          <el-alert
            v-if="sheetLayoutError"
            :title="sheetLayoutError"
            type="error"
            :closable="false"
            show-icon
          />
          <div v-else-if="renderedRows.length" class="batch-record-cell-rules-editor__sheet-scroll">
            <table class="batch-record-cell-rules-editor__sheet">
              <colgroup>
                <col
                  v-for="column in renderedColumns"
                  :key="column.columnIndex"
                  :style="{ width: `${column.widthPercent}%` }"
                />
              </colgroup>
              <tbody>
                <tr
                  v-for="row in renderedRows"
                  :key="row.rowIndex"
                  :style="{ height: `${row.height}px` }"
                >
                  <td
                    v-for="cell in row.cells"
                    :key="cell.identity"
                    :rowspan="cell.rowSpan"
                    :colspan="cell.colSpan"
                    :class="[
                      cell.classNames,
                      { 'is-assist-mapped': isSourceCellMappedToAssistGrid(cell) }
                    ]"
                  >
                    <button
                      type="button"
                      class="batch-record-cell-rules-editor__cell-button"
                      :aria-label="
                        activeConfigMode === 'assistMapping' ? '映射原表单元格' : '选择单元格规则'
                      "
                      :aria-pressed="cell.identity === selectedRuleKey"
                      :disabled="isSourceCellDisabledForAssistMapping(cell)"
                      :title="resolveSourceCellAssistMappingTitle(cell)"
                      @click="handleSourceCellClick(cell)"
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

        <div
          v-if="activeConfigMode === 'assistMapping'"
          class="batch-record-cell-rules-editor__assist-preview-panel"
          data-fill-config-panel="assist-preview"
        >
          <div class="batch-record-cell-rules-editor__panel-head">
            <div>
              <strong>辅助表单预览</strong>
              <p>点击黄色表格单元格后，再点击左侧未灰化的原表单元格建立映射。</p>
            </div>
            <el-tag class="scheme-d-tag" type="warning" effect="plain">实时</el-tag>
          </div>

          <div class="batch-record-cell-rules-editor__assist-preview-scroll">
            <el-empty
              v-if="!selectedAssistFillerUserId"
              description="请在右侧添加并选择填写人"
            />
            <template v-else>
              <div class="batch-record-cell-rules-editor__assist-grid-meta">
                <strong>{{ selectedAssistFillerUserLabel }}</strong>
                <el-tag class="scheme-d-tag" size="small" effect="plain">
                  辅助表格 {{ assistGridRowCount }} × {{ assistGridColumnCount }}
                </el-tag>
              </div>
              <table class="batch-record-cell-rules-editor__assist-grid">
                <tbody>
                  <tr v-for="gridRow in assistGridPreviewRows" :key="gridRow.rowIndex">
                    <td v-for="gridCell in gridRow.cells" :key="gridCell.key">
                      <button
                        type="button"
                        class="batch-record-cell-rules-editor__assist-grid-cell"
                        :class="{
                          'is-selected': selectedAssistGridCellKey === gridCell.key,
                          'is-mapped': Boolean(gridCell.sourceCell)
                        }"
                        :data-assist-grid-cell="gridCell.key"
                        @click="handleAssistGridCellClick(gridCell.key)"
                        @dblclick.stop="handleAssistGridCellDoubleClick(gridCell)"
                      >
                        <span>{{ gridCell.label }}</span>
                        <small>{{ gridCell.sourceSummary || '未映射' }}</small>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </template>
          </div>
        </div>

        </section>
      </main>

      <aside
        class="batch-record-cell-rules-editor__side-panel"
        :class="{ 'is-mapping-control': activeConfigMode === 'assistMapping' }"
        data-fill-config-panel="template-config-sidebar"
      >
        <div class="batch-record-cell-rules-editor__side-scroll">
          <div class="batch-record-cell-rules-editor__side-intro">
            <strong>{{ activeConfigMode === 'assistMapping' ? '映射控制栏' : '填写配置' }}</strong>
            <p>
              {{
                activeConfigMode === 'assistMapping'
                  ? '设置辅助表格、填写人和当前原表字段类型；中间表格会实时更新。'
                  : '左侧选单元格，右侧维护字段类型、提示词。'
              }}
            </p>
          </div>

          <section
            v-if="activeConfigMode !== 'assistMapping'"
            class="batch-record-cell-rules-editor__ai-detect-panel"
          >
            <div class="batch-record-cell-rules-editor__assist-grid-control-head">
              <strong>AI 填写规则识别</strong>
              <p>基于已保存版本识别，先预览候选，人工应用后再点击保存。</p>
            </div>
            <el-button
              class="scheme-d-btn scheme-d-btn--primary"
              type="primary"
              plain
              :loading="autoDetecting"
              :disabled="aiDetectDisabled"
              @click="handleAutoDetect"
            >
              AI 自动识别
            </el-button>
            <el-alert
              v-if="aiDetectSummary"
              :title="aiDetectSummary"
              type="info"
              :closable="false"
              show-icon
            />
            <div v-if="pendingAiCandidates.length" class="batch-record-cell-rules-editor__ai-candidates">
              <el-table :data="pendingAiCandidates" size="small" border max-height="220">
                <el-table-column label="单元格" width="90">
                  <template #default="scope">
                    R{{ scope.row.rowIndex + 1 }}C{{ scope.row.columnIndex + 1 }}
                  </template>
                </el-table-column>
                <el-table-column prop="label" label="字段" min-width="110" />
                <el-table-column prop="valueType" label="类型" width="82" />
                <el-table-column prop="reason" label="识别依据" min-width="180" show-overflow-tooltip />
              </el-table>
              <div class="batch-record-cell-rules-editor__ai-actions">
                <el-button
                  size="small"
                  type="primary"
                  :disabled="readonlyMode"
                  @click="applyAiCandidates"
                >
                  应用识别结果
                </el-button>
                <el-button size="small" :disabled="readonlyMode" @click="clearAiCandidates">
                  清空候选
                </el-button>
              </div>
            </div>
          </section>

          <template v-if="activeConfigMode === 'assistMapping'">
            <section class="batch-record-cell-rules-editor__assist-grid-control">
              <div class="batch-record-cell-rules-editor__assist-grid-control-head">
                <strong>辅助表格设置</strong>
                <p>固定表格单元格；先点中间表格格子，再点左侧未灰化原表格。</p>
              </div>
              <div class="batch-record-cell-rules-editor__assist-grid-size">
                <label>
                  <span>行数</span>
                  <el-input-number
                    v-model="assistGridRowCount"
                    :min="1"
                    :max="20"
                    :controls="false"
                    :disabled="readonlyMode"
                    @change="handleAssistGridSizeChange"
                  />
                </label>
                <label>
                  <span>列数</span>
                  <el-input-number
                    v-model="assistGridColumnCount"
                    :min="1"
                    :max="20"
                    :controls="false"
                    :disabled="readonlyMode"
                    @change="handleAssistGridSizeChange"
                  />
                </label>
              </div>
            </section>

            <section class="batch-record-cell-rules-editor__assist-filler-control">
              <div class="batch-record-cell-rules-editor__assist-grid-control-head">
                <strong>填写人</strong>
                <p>每个填写人拥有自己的辅助表格；原表单元格全局只能分配一次。</p>
              </div>
              <div class="batch-record-cell-rules-editor__assist-filler-add">
                <el-select
                  v-model="pendingAssistFillerUserId"
                  filterable
                  clearable
                  placeholder="选择员工"
                  :disabled="readonlyMode"
                >
                  <el-option
                    v-for="option in availableAssistFillerUserOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-button
                  class="scheme-d-btn scheme-d-btn--success"
                  type="primary"
                  plain
                  :disabled="readonlyMode || !pendingAssistFillerUserId"
                  @click="addAssistFillerUser"
                >
                  添加
                </el-button>
              </div>
              <el-empty
                v-if="assistFillerUserIds.length === 0"
                description="请先添加填写人"
                :image-size="56"
              />
              <div v-else class="batch-record-cell-rules-editor__assist-filler-list">
                <article
                  v-for="userId in assistFillerUserIds"
                  :key="userId"
                  class="batch-record-cell-rules-editor__assist-filler-item"
                  :class="{ 'is-selected': selectedAssistFillerUserId === userId }"
                >
                  <button type="button" @click="selectAssistFillerUser(userId)">
                    <strong>{{ resolveUserLabelById(userId) }}</strong>
                    <span>{{ assistGridMappedCountByUser(userId) }} 个映射</span>
                  </button>
                  <el-button
                    size="small"
                    link
                    class="scheme-d-row-action scheme-d-row-action--danger"
                    type="danger"
                    :disabled="readonlyMode"
                    @click="removeAssistFillerUser(userId)"
                  >
                    删除
                  </el-button>
                </article>
              </div>
            </section>
          </template>

          <template v-if="selectedCell">
            <div class="batch-record-cell-rules-editor__fillable-toggle">
              <strong>是否可填写</strong>
              <el-switch
                v-model="isSelectedCellFillable"
                :disabled="readonlyMode"
                active-text="可填写"
                inactive-text="不可填写"
              />
            </div>

            <template v-if="selectedRule">
              <el-form
                label-position="top"
                class="batch-record-cell-rules-editor__form"
                :disabled="readonlyMode"
              >
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

                <el-form-item
                  v-if="selectedRule.valueType === 'NUMBER'"
                  label="字段范围"
                >
                  <div class="batch-record-cell-rules-editor__range-grid">
                    <el-input-number
                      v-model="selectedNumericMin"
                      :controls="false"
                      placeholder="最小值"
                      class="!w-1/1"
                      @change="setSelectedNumericConstraint('min', $event)"
                    />
                    <el-input-number
                      v-model="selectedNumericMax"
                      :controls="false"
                      placeholder="最大值"
                      class="!w-1/1"
                      @change="setSelectedNumericConstraint('max', $event)"
                    />
                  </div>
                </el-form-item>

                <el-form-item
                  v-if="selectedRule.valueType === 'STRING'"
                  label="下拉选项"
                >
                  <div class="batch-record-cell-rules-editor__dropdown-options">
                    <div class="batch-record-cell-rules-editor__dropdown-switch">
                      <span>作为下拉框填写</span>
                      <el-switch
                        :model-value="selectedRule.constraints?.selectionMode === 'single'"
                        active-text="启用"
                        inactive-text="关闭"
                        @change="toggleSelectedStringDropdown"
                      />
                    </div>
                    <template v-if="selectedRule.constraints?.selectionMode === 'single'">
                      <div
                        v-for="(option, optionIndex) in selectedStringOptions"
                        :key="optionIndex"
                        class="batch-record-cell-rules-editor__dropdown-option"
                      >
                        <el-input
                          :model-value="option.label"
                          maxlength="60"
                          placeholder="选项文本"
                          @input="updateSelectedStringOption(optionIndex, $event)"
                        />
                        <el-button
                          link
                          class="scheme-d-row-action scheme-d-row-action--danger"
                          type="danger"
                          @click="removeSelectedStringOption(optionIndex)"
                        >
                          删除
                        </el-button>
                      </div>
                      <el-button
                        link
                        class="scheme-d-row-action scheme-d-row-action--success"
                        type="primary"
                        @click="addSelectedStringOption"
                      >
                        新增选项
                      </el-button>
                    </template>
                  </div>
                </el-form-item>

                <el-alert
                  v-if="selectedRule.valueType === 'SIGNATURE'"
                  title="签名单元格会在执行页触发现有电子签名，不作为普通文本保存。"
                  type="info"
                  :closable="false"
                  show-icon
                />
              </el-form>
            </template>

            <section
              v-if="activeConfigMode === 'source'"
              class="batch-record-cell-rules-editor__assist-section"
            >
              <div class="batch-record-cell-rules-editor__assist-head">
                <div>
                  <strong>辅助行配置</strong>
                  <p>把同一行要填写的单元格归在一起，并写清这一行给员工看的描述。</p>
                </div>
                <el-button
                  size="small"
                  class="scheme-d-btn scheme-d-btn--success"
                  type="primary"
                  plain
                  :disabled="readonlyMode || !selectedCell"
                  @click="addAssistRowFromSelectedCell"
                >
                  当前单元格新增行
                </el-button>
              </div>

              <el-alert
                v-if="selectedCellAssistRow"
                :title="`当前单元格已在辅助行：${selectedCellAssistRow.description || selectedCellAssistRow.rowKey}`"
                type="success"
                :closable="false"
                show-icon
              />

              <el-empty
                v-if="editableAssistRows.length === 0"
                description="暂无辅助行，请选择单元格后新增"
              />

              <div v-else class="batch-record-cell-rules-editor__assist-list">
                <article
                  v-for="(assistRow, assistRowIndex) in editableAssistRows"
                  :key="assistRow.rowKey"
                  class="batch-record-cell-rules-editor__assist-row"
                  :class="{ 'is-selected': assistRow.rowKey === selectedAssistRowKey }"
                >
                  <button
                    type="button"
                    class="batch-record-cell-rules-editor__assist-select"
                    @click="selectedAssistRowKey = assistRow.rowKey"
                  >
                    <span class="batch-record-cell-rules-editor__assist-select-copy">
                      <strong>辅助行 {{ assistRowIndex + 1 }}</strong>
                      <span>{{ assistRow.description || '未填写描述' }}</span>
                    </span>
                    <span>{{ assistRow.fields.length }} 个单元格</span>
                  </button>
                  <template v-if="assistRow.rowKey === selectedAssistRowKey">
                    <el-input
                      v-model="assistRow.description"
                      :disabled="readonlyMode"
                      maxlength="120"
                      show-word-limit
                      placeholder="例如：记录本工序温度、压力、操作人"
                    />
                    <div class="batch-record-cell-rules-editor__assist-assignment">
                      <strong>辅助行填写人</strong>
                      <div class="batch-record-cell-rules-editor__assist-assignment-grid">
                        <el-select
                          v-model="assistAssignments[assistRow.rowKey].candidateSourceType"
                          :disabled="readonlyMode"
                          placeholder="来源"
                          @change="assistAssignments[assistRow.rowKey].candidateSourceIds = []"
                        >
                          <el-option label="个人" value="USERS" />
                          <el-option label="角色" value="ROLE" />
                        </el-select>
                        <el-select
                          v-model="assistAssignments[assistRow.rowKey].candidateSourceIds"
                          :disabled="readonlyMode"
                          multiple
                          filterable
                          collapse-tags
                          collapse-tags-tooltip
                          placeholder="选择员工或角色"
                        >
                          <el-option
                            v-for="option in buildAssignmentTargetOptions(assistAssignments[assistRow.rowKey].candidateSourceType)"
                            :key="`${assistAssignments[assistRow.rowKey].candidateSourceType}:${option.value}`"
                            :label="option.label"
                            :value="option.value"
                          />
                        </el-select>
                        <el-select
                          v-model="assistAssignments[assistRow.rowKey].completionPolicy"
                          :disabled="readonlyMode"
                          placeholder="完成策略"
                        >
                          <el-option label="任一人完成" value="ANY_ONE" />
                          <el-option label="全部完成" value="ALL" />
                        </el-select>
                      </div>
                    </div>
                    <div class="batch-record-cell-rules-editor__assist-actions">
                      <el-button
                        size="small"
                        class="scheme-d-btn scheme-d-btn--primary"
                        :disabled="readonlyMode || !selectedCell"
                        @click="assignSelectedCellToAssistRow(assistRow.rowKey)"
                      >
                        加入当前单元格
                      </el-button>
                      <el-button
                        size="small"
                        class="scheme-d-btn scheme-d-btn--warning"
                        :disabled="readonlyMode || assistRowIndex === 0"
                        @click="moveAssistRow(assistRow.rowKey, -1)"
                      >
                        上移
                      </el-button>
                      <el-button
                        size="small"
                        class="scheme-d-btn scheme-d-btn--warning"
                        :disabled="readonlyMode || assistRowIndex === editableAssistRows.length - 1"
                        @click="moveAssistRow(assistRow.rowKey, 1)"
                      >
                        下移
                      </el-button>
                      <el-button
                        size="small"
                        class="scheme-d-btn scheme-d-btn--danger"
                        type="danger"
                        plain
                        :disabled="readonlyMode"
                        @click="removeAssistRow(assistRow.rowKey)"
                      >
                        删除
                      </el-button>
                    </div>
                  </template>
                </article>
              </div>
            </section>
          </template>

          <el-empty v-else description="请在左侧表单中点击一个单元格" />
        </div>

        <div class="batch-record-cell-rules-editor__side-actions scheme-d-dialog-footer">
          <el-button class="scheme-d-btn scheme-d-btn--danger" @click="dialogVisible = false">关闭</el-button>
          <el-button
            class="scheme-d-btn scheme-d-btn--warning"
            :loading="loading"
            :disabled="loading || saving || autoDetecting"
            @click="reloadTemplateRules"
          >
            重新读取
          </el-button>
          <el-button
            class="scheme-d-btn scheme-d-btn--success"
            type="primary"
            :loading="saving"
            :disabled="!canConfirmRules"
            @click="confirmAllRules"
          >
            保存填写配置
          </el-button>
        </div>
      </aside>
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import type {
  BatchRecordReportAssistRowVO,
  BatchRecordReportCellRuleVO,
  BatchRecordReportCellValueType,
  BatchRecordReportSignatureCellMarkerVO
} from '@/api/mes/pro/batchrecordreport'
import {
  autoDetectTemplateFillRules,
  type FormTemplateFillRuleCandidateVO,
  type FormTemplateFillRuleAutoDetectRespVO,
  type FormTemplateListItemVO
} from '@/api/form-center/template'
import type {
  EdhrProcessFormCandidateSourceType,
  EdhrProcessFormCompletionPolicy,
  EdhrProcessFormFillAssignment
} from '@/api/mes/pro/edhr/processFormPermissionRule'
import { getSimpleRoleList, type RoleVO } from '@/api/system/role'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import {
  buildTemplateFieldIdentity,
  cellRuleDefaultComponentMap,
  cellRuleValueTypeOptions,
  cleanedRuleConstraints,
  normalizeCellRule,
  normalizeTemplateCellMerge,
  stringifyTemplateCell,
  type TemplateRawCell,
  type TemplateRawLayout
} from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'

defineOptions({ name: 'FormTemplateFillConfigDialog' })

type RuleEditorRawRow = {
  height?: unknown
  cells?: Record<string, TemplateRawCell>
}

type RuleEditorRawLayout = TemplateRawLayout & {
  rows?: Record<string, RuleEditorRawRow>
  cols?: Record<string, { width?: unknown }>
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

type NumericConstraintKey = 'min' | 'max' | 'scale' | 'precision'

type StringSelectOption = {
  label: string
  value: string
}

type AssistAssignmentDraft = {
  candidateSourceType: EdhrProcessFormCandidateSourceType
  candidateSourceIds: number[]
  completionPolicy: EdhrProcessFormCompletionPolicy
}

type ConfigMode = 'source' | 'assistMapping'

type AssistGridKey = {
  userId: number
  rowIndex: number
  columnIndex: number
}

type AssistGridPreviewCell = AssistGridKey & {
  key: string
  label: string
  sourceSummary: string
  sourceCell: RuleEditorCell | null
}

type AssistGridPreviewRow = {
  rowIndex: number
  cells: AssistGridPreviewCell[]
}

type SourceCellGridAssignment = AssistGridKey & {
  rowKey: string
  row: BatchRecordReportAssistRowVO
}

export type FormTemplateFillConfigSavePayload = {
  sheetLayoutJson: string
  cellRules: BatchRecordReportCellRuleVO[]
  signatureCellMarkers: BatchRecordReportSignatureCellMarkerVO[]
  assistRows: BatchRecordReportAssistRowVO[]
  fillAssignments: EdhrProcessFormFillAssignment[]
}

const props = defineProps<{
  modelValue: boolean
  template?: FormTemplateListItemVO | null
  sheetLayoutJson?: string
  cellRules?: BatchRecordReportCellRuleVO[]
  signatureCellMarkers?: BatchRecordReportSignatureCellMarkerVO[]
  assistRows?: BatchRecordReportAssistRowVO[]
  fillAssignments?: EdhrProcessFormFillAssignment[]
  readonly?: boolean
  saving?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'draft-version-ready': [value: FormTemplateFillRuleAutoDetectRespVO]
  save: [value: FormTemplateFillConfigSavePayload]
}>()

const message = useMessage()
const loading = ref(false)
const errorMessage = ref('')
const sheetLayoutError = ref('')
const activeConfigMode = ref<ConfigMode>('source')
const selectedRuleKey = ref('')
const selectedAssistRowKey = ref('')
const selectedAssistGridCellKey = ref('')
const selectedAssistFillerUserId = ref<number>()
const pendingAssistFillerUserId = ref<number>()
const assistGridRowCount = ref(3)
const assistGridColumnCount = ref(3)
const assistFillerUserIds = ref<number[]>([])
const ruleRows = ref<BatchRecordReportCellRuleVO[]>([])
const editableAssistRows = ref<BatchRecordReportAssistRowVO[]>([])
const assistAssignments = reactive<Record<string, AssistAssignmentDraft>>({})
const simpleUserOptions = ref<UserVO[]>([])
const simpleRoleOptions = ref<RoleVO[]>([])
const sheetLayout = ref<RuleEditorRawLayout | null>(null)
const autoDetecting = ref(false)
const pendingAiCandidates = ref<FormTemplateFillRuleCandidateVO[]>([])
const aiDetectSummary = ref('')
const preserveAiCandidatesOnReload = ref(false)

const DEFAULT_COLUMN_WIDTH = 150
const DEFAULT_ROW_HEIGHT = 34
const ASSIST_ROW_KEY_PREFIX = 'ASSIST_ROW'
const ASSIST_GRID_ROW_KEY_PREFIX = 'ASSIST_GRID'

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const templateName = computed(() =>
  props.template?.templateName || props.template?.templateId || '-'
)
const readonlyMode = computed(() => Boolean(props.readonly))
const saving = computed(() => Boolean(props.saving))
const aiDetectDisabled = computed(
  () => loading.value || saving.value || autoDetecting.value
)
const valueTypeLabelMap = Object.fromEntries(
  cellRuleValueTypeOptions.map((option) => [option.value, option.label])
) as Record<string, string>

const componentFlagBaseOptions = [
  { label: '文本输入 input-text', value: 'input-text' },
  { label: '数字输入 input-number', value: 'input-number' },
  { label: '日期 date', value: 'date' },
  { label: '日期时间 datetime', value: 'datetime' },
  { label: '复选框 checkbox', value: 'checkbox' },
  { label: '复选框组 radio-group', value: 'radio-group' },
  { label: '单选组 option-group', value: 'option-group' },
  { label: '下拉选择 select', value: 'select' },
  { label: '电子签名 signature', value: 'signature' },
  { label: '多行文本 textarea', value: 'textarea' },
  { label: '文件上传 upload-file', value: 'upload-file' },
  { label: '图片上传 upload-image', value: 'upload-image' },
  { label: '多图片上传 upload-images', value: 'upload-images' }
]

const componentFlagOptions = computed(() => {
  const optionMap = new Map(componentFlagBaseOptions.map((option) => [option.value, option]))
  ruleRows.value.forEach((rule) => {
    const value = String(rule.componentFlag || '').trim()
    if (value && !optionMap.has(value)) {
      optionMap.set(value, { label: value, value })
    }
  })
  return Array.from(optionMap.values())
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  if (typeof error === 'string' && error.trim()) return error
  const dataMessage = (error as any)?.msg || (error as any)?.message
  if (typeof dataMessage === 'string' && dataMessage.trim()) return dataMessage
  return fallback
}

const ruleIdentity = (rule: Pick<BatchRecordReportCellRuleVO, 'rowIndex' | 'columnIndex'>) =>
  `${rule.rowIndex}:${rule.columnIndex}`

const cellIdentity = (rowIndex: number, columnIndex: number) => `${rowIndex}:${columnIndex}`

const isValidCellCoordinate = (rowIndex: unknown, columnIndex: unknown) =>
  Number.isInteger(rowIndex) &&
  Number.isInteger(columnIndex) &&
  Number(rowIndex) >= 0 &&
  Number(columnIndex) >= 0

const normalizeAssistRowFields = (
  fields: BatchRecordReportAssistRowVO['fields'] = []
): BatchRecordReportAssistRowVO['fields'] => {
  const fieldMap = new Map<string, BatchRecordReportAssistRowVO['fields'][number]>()
  fields.forEach((field) => {
    if (!isValidCellCoordinate(field.rowIndex, field.columnIndex)) return
    fieldMap.set(cellIdentity(field.rowIndex, field.columnIndex), {
      rowIndex: field.rowIndex,
      columnIndex: field.columnIndex
    })
  })
  return Array.from(fieldMap.values()).sort(
    (left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex
  )
}

const normalizeAssistRows = (
  rows: BatchRecordReportAssistRowVO[] = []
): BatchRecordReportAssistRowVO[] =>
  rows
    .map((row, index) => ({
      rowKey: String(row.rowKey || `${ASSIST_ROW_KEY_PREFIX}_${index + 1}`).trim(),
      description: String(row.description || '').trim(),
      sort: Number.isFinite(Number(row.sort)) ? Number(row.sort) : index + 1,
      fields: normalizeAssistRowFields(row.fields)
    }))
    .filter((row) => row.rowKey)
    .sort((left, right) => left.sort - right.sort)
    .map((row, index) => ({
      ...row,
      sort: index + 1
    }))

const normalizeRuleSource = (source?: string) => {
  const normalized = String(source || '').trim().toUpperCase()
  return normalized || 'MANUAL'
}

const isConfirmedRule = (rule: BatchRecordReportCellRuleVO) =>
  Boolean(rule.reviewed) && normalizeRuleSource(rule.source) !== 'AUTO'

const pendingCount = computed(() => ruleRows.value.filter((rule) => !isConfirmedRule(rule)).length)
const canConfirmRules = computed(
  () =>
    !readonlyMode.value &&
    !loading.value &&
    !saving.value &&
    Boolean(sheetLayout.value) &&
    !sheetLayoutError.value
)

const cloneRecord = <T extends object | undefined>(value: T): T => (value ? ({ ...value } as T) : value)

const toManualReviewedRule = (rule: BatchRecordReportCellRuleVO): BatchRecordReportCellRuleVO => {
  const normalized = normalizeCellRule(rule)
  return {
    ...normalized,
    constraints: cleanedRuleConstraints(normalized.constraints, normalized.valueType),
    attachmentRule: cloneRecord(normalized.attachmentRule),
    source: 'MANUAL',
    confidence: 1,
    reviewed: true
  }
}

const parseSheetLayout = (sheetLayoutJson?: string): RuleEditorRawLayout | null => {
  if (!sheetLayoutJson?.trim()) return null
  const parsed = JSON.parse(sheetLayoutJson) as RuleEditorRawLayout
  if (!parsed?.rows || !Object.keys(parsed.rows).length) {
    throw new Error('当前模板缺少有效表单布局，无法进入填写配置。')
  }
  return parsed
}

const sortRules = (rules: BatchRecordReportCellRuleVO[]) =>
  [...rules].sort((left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex)

const selectedRule = computed(() =>
  ruleRows.value.find((rule) => ruleIdentity(rule) === selectedRuleKey.value)
)

const ruleMap = computed(() => {
  const map = new Map<string, BatchRecordReportCellRuleVO>()
  ruleRows.value.forEach((rule) => map.set(ruleIdentity(rule), rule))
  return map
})

const rowIndexes = computed(() => {
  const rows = sheetLayout.value?.rows || {}
  return Object.keys(rows)
    .map((key) => Number(key))
    .filter((key) => Number.isInteger(key))
    .sort((a, b) => a - b)
})

const columnIndexes = computed(() => {
  const columns = new Set<number>()
  Object.keys(sheetLayout.value?.cols || {}).forEach((key) => {
    const columnIndex = Number(key)
    if (Number.isInteger(columnIndex)) columns.add(columnIndex)
  })
  Object.values(sheetLayout.value?.rows || {}).forEach((row) => {
    Object.keys(row.cells || {}).forEach((key) => {
      const columnIndex = Number(key)
      if (Number.isInteger(columnIndex)) columns.add(columnIndex)
    })
  })
  ruleRows.value.forEach((rule) => columns.add(rule.columnIndex))
  return Array.from(columns).sort((a, b) => a - b)
})

const renderedColumns = computed<RuleEditorColumn[]>(() => {
  const widths = columnIndexes.value.map((columnIndex) => {
    const configuredWidth = Number(sheetLayout.value?.cols?.[String(columnIndex)]?.width)
    return Number.isFinite(configuredWidth) && configuredWidth > 0 ? configuredWidth : DEFAULT_COLUMN_WIDTH
  })
  const totalWidth = widths.reduce((sum, width) => sum + width, 0)
  return columnIndexes.value.map((columnIndex, index) => ({
    columnIndex,
    widthPercent: totalWidth > 0 ? (widths[index] / totalWidth) * 100 : 100
  }))
})

const coveredCellSet = computed(() => {
  const covered = new Set<string>()
  Object.entries(sheetLayout.value?.rows || {}).forEach(([rowKey, row]) => {
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
})

const resolveRowHeight = (height: unknown) => {
  const numericHeight = Number(height)
  return Math.max(Number.isFinite(numericHeight) && numericHeight > 0 ? numericHeight : DEFAULT_ROW_HEIGHT, 28)
}

const renderedRows = computed<RuleEditorRow[]>(() => {
  const rows = sheetLayout.value?.rows || {}
  return rowIndexes.value.map((rowIndex) => {
    const rawRow = rows[String(rowIndex)] || {}
    const cells: RuleEditorCell[] = []
    columnIndexes.value.forEach((columnIndex) => {
      const identity = `${rowIndex}:${columnIndex}`
      if (coveredCellSet.value.has(identity)) return
      const rawCell = rawRow.cells?.[String(columnIndex)]
      const merge = normalizeTemplateCellMerge(rawCell)
      const text = stringifyTemplateCell(rawCell?.value ?? rawCell?.text)
      const rule = ruleMap.value.get(identity)
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
          'is-selected': selectedRuleKey.value === identity
        }
      })
    })
    return {
      rowIndex,
      height: resolveRowHeight(rawRow.height),
      cells
    }
  })
})

const findRenderedCellByIdentity = (identity: string) => {
  for (const row of renderedRows.value) {
    const cell = row.cells.find((item) => item.identity === identity)
    if (cell) return cell
  }
  return null
}

const selectedCell = computed(() => {
  if (!selectedRuleKey.value) return null
  return findRenderedCellByIdentity(selectedRuleKey.value)
})

const createDefaultAssistAssignment = (): AssistAssignmentDraft => ({
  candidateSourceType: 'USERS',
  candidateSourceIds: [],
  completionPolicy: 'ANY_ONE'
})

const normalizeAssignmentSourceType = (
  value: unknown
): EdhrProcessFormCandidateSourceType => {
  const normalized = String(value || '').trim().toUpperCase()
  if (normalized === 'USER') return 'USERS'
  return normalized === 'ROLE' ? 'ROLE' : 'USERS'
}

const normalizeAssignmentPolicy = (value: unknown): EdhrProcessFormCompletionPolicy =>
  String(value || '').trim().toUpperCase() === 'ALL' ? 'ALL' : 'ANY_ONE'

const normalizeAssignmentIds = (ids: unknown): number[] =>
  Array.isArray(ids)
    ? Array.from(
        new Set(
          ids
            .map((id) => Number(id))
            .filter((id) => Number.isFinite(id) && id > 0)
        )
      )
    : []

const ensureAssistAssignment = (rowKey: string) => {
  if (!assistAssignments[rowKey]) {
    assistAssignments[rowKey] = createDefaultAssistAssignment()
  }
  return assistAssignments[rowKey]
}

const syncAssistAssignmentsWithRows = (rows = editableAssistRows.value) => {
  const rowKeys = new Set(rows.map((row) => row.rowKey))
  Object.keys(assistAssignments).forEach((rowKey) => {
    if (!rowKeys.has(rowKey)) {
      delete assistAssignments[rowKey]
    }
  })
  rows.forEach((row) => ensureAssistAssignment(row.rowKey))
}

const parseAssistGridRowKey = (rowKey: string): AssistGridKey | null => {
  const match = String(rowKey || '').match(/^ASSIST_GRID_U(\d+)_R(\d+)_C(\d+)$/)
  if (!match) return null
  const userId = Number(match[1])
  const rowIndex = Number(match[2])
  const columnIndex = Number(match[3])
  if (!Number.isInteger(userId) || userId <= 0) return null
  if (!Number.isInteger(rowIndex) || rowIndex < 0) return null
  if (!Number.isInteger(columnIndex) || columnIndex < 0) return null
  return { userId, rowIndex, columnIndex }
}

const buildAssistGridRowKey = ({ userId, rowIndex, columnIndex }: AssistGridKey) =>
  `${ASSIST_GRID_ROW_KEY_PREFIX}_U${userId}_R${rowIndex}_C${columnIndex}`

const parseAssistGridCellKey = (cellKey: string) => parseAssistGridRowKey(cellKey)

const assistUserOptions = computed(() =>
  simpleUserOptions.value.map((user) => ({
    label: user.nickname || user.username || String(user.id),
    value: Number(user.id)
  }))
)

const sortAssistFillerUserIds = (userIds: number[]) => {
  const optionOrder = new Map(
    simpleUserOptions.value.map((user, index) => [Number(user.id), index])
  )
  return Array.from(new Set(userIds.filter((id) => Number.isFinite(id) && id > 0))).sort(
    (left, right) =>
      (optionOrder.get(left) ?? Number.MAX_SAFE_INTEGER) -
        (optionOrder.get(right) ?? Number.MAX_SAFE_INTEGER) ||
      left - right
  )
}

const calculateAssistGridSort = ({ userId, rowIndex, columnIndex }: AssistGridKey) => {
  const userIndex = assistFillerUserIds.value.indexOf(userId)
  const normalizedUserIndex = userIndex >= 0 ? userIndex : assistFillerUserIds.value.length
  return normalizedUserIndex * 10000 + rowIndex * 100 + columnIndex + 1
}

const normalizeAssistGridSizeValue = (value: unknown) => {
  const numericValue = Number(value)
  return Number.isInteger(numericValue) && numericValue > 0 ? numericValue : 1
}

const orderAssistGridRows = (rows: BatchRecordReportAssistRowVO[]) =>
  [...rows]
    .map((row) => {
      const parsed = parseAssistGridRowKey(row.rowKey)
      return {
        ...row,
        sort: parsed ? calculateAssistGridSort(parsed) : row.sort
      }
    })
    .sort((left, right) => left.sort - right.sort)
    .map((row, index) => ({ ...row, sort: index + 1 }))

const ensureSelectedAssistFillerStillExists = () => {
  if (
    selectedAssistFillerUserId.value &&
    assistFillerUserIds.value.includes(selectedAssistFillerUserId.value)
  ) {
    return
  }
  selectedAssistFillerUserId.value = assistFillerUserIds.value[0]
}

const ensureSelectedAssistGridCellStillExists = () => {
  const current = selectedAssistGridCellKey.value
  const parsed = current ? parseAssistGridCellKey(current) : null
  if (
    parsed &&
    selectedAssistFillerUserId.value === parsed.userId &&
    parsed.rowIndex < assistGridRowCount.value &&
    parsed.columnIndex < assistGridColumnCount.value &&
    assistFillerUserIds.value.includes(parsed.userId)
  ) {
    return
  }
  selectedAssistGridCellKey.value = selectedAssistFillerUserId.value
    ? buildAssistGridRowKey({
        userId: selectedAssistFillerUserId.value,
        rowIndex: 0,
        columnIndex: 0
      })
    : ''
}

const syncAssistGridStateWithRows = (rows = editableAssistRows.value) => {
  const userIds = new Set(assistFillerUserIds.value)
  let nextRowCount = normalizeAssistGridSizeValue(assistGridRowCount.value)
  let nextColumnCount = normalizeAssistGridSizeValue(assistGridColumnCount.value)
  rows.forEach((row) => {
    const parsed = parseAssistGridRowKey(row.rowKey)
    if (!parsed) return
    userIds.add(parsed.userId)
    nextRowCount = Math.max(nextRowCount, parsed.rowIndex + 1)
    nextColumnCount = Math.max(nextColumnCount, parsed.columnIndex + 1)
  })
  assistFillerUserIds.value = sortAssistFillerUserIds(Array.from(userIds))
  assistGridRowCount.value = nextRowCount
  assistGridColumnCount.value = nextColumnCount
  ensureSelectedAssistFillerStillExists()
  ensureSelectedAssistGridCellStillExists()
}

const applyAssistAssignments = (fillAssignments: EdhrProcessFormFillAssignment[] = []) => {
  fillAssignments.forEach((assignment) => {
    const rowKey = String(assignment.scopeKey || '').trim()
    if (!rowKey) return
    const parsed = parseAssistGridRowKey(rowKey)
    assistAssignments[rowKey] = {
      candidateSourceType: parsed
        ? 'USERS'
        : normalizeAssignmentSourceType(assignment.candidateSourceType),
      candidateSourceIds: parsed ? [parsed.userId] : normalizeAssignmentIds(assignment.candidateSourceIds),
      completionPolicy: normalizeAssignmentPolicy(assignment.completionPolicy)
    }
  })
  syncAssistAssignmentsWithRows()
  syncAssistGridStateWithRows()
}

const buildAssignmentTargetOptions = (sourceType: EdhrProcessFormCandidateSourceType) => {
  if (normalizeAssignmentSourceType(sourceType) === 'ROLE') {
    return simpleRoleOptions.value.map((role) => ({
      label: role.name || role.code || String(role.id),
      value: Number(role.id)
    }))
  }
  return simpleUserOptions.value.map((user) => ({
    label: user.nickname || user.username || String(user.id),
    value: Number(user.id)
  }))
}

const availableAssistFillerUserOptions = computed(() =>
  assistUserOptions.value.filter((option) => !assistFillerUserIds.value.includes(option.value))
)

const resolveUserLabelById = (userId: number) =>
  assistUserOptions.value.find((option) => option.value === userId)?.label || `用户 ${userId}`

const selectedAssistFillerUserLabel = computed(() =>
  selectedAssistFillerUserId.value
    ? resolveUserLabelById(selectedAssistFillerUserId.value)
    : '未选择填写人'
)

const assistGridRowMap = computed(() => {
  const map = new Map<string, BatchRecordReportAssistRowVO>()
  editableAssistRows.value.forEach((row) => {
    if (parseAssistGridRowKey(row.rowKey)) {
      map.set(row.rowKey, row)
    }
  })
  return map
})

const sourceCellGridAssignmentMap = computed(() => {
  const map = new Map<string, SourceCellGridAssignment>()
  editableAssistRows.value.forEach((row) => {
    const parsed = parseAssistGridRowKey(row.rowKey)
    const field = row.fields[0]
    if (!parsed || !field) return
    const key = cellIdentity(field.rowIndex, field.columnIndex)
    if (!map.has(key)) {
      map.set(key, { ...parsed, rowKey: row.rowKey, row })
    }
  })
  return map
})

const isSourceCellMappedToAssistGrid = (cell: RuleEditorCell) =>
  sourceCellGridAssignmentMap.value.has(cell.identity)

const isSourceCellDisabledForAssistMapping = (cell: RuleEditorCell) =>
  activeConfigMode.value === 'assistMapping' && sourceCellGridAssignmentMap.value.has(cell.identity)

const resolveSourceCellAssistMappingTitle = (cell: RuleEditorCell) => {
  const assignment = sourceCellGridAssignmentMap.value.get(cell.identity)
  if (activeConfigMode.value !== 'assistMapping') return '选择单元格规则'
  if (assignment) {
    return `已分配给 ${resolveUserLabelById(assignment.userId)}，请先在辅助表格取消映射`
  }
  if (!selectedAssistGridCellKey.value) return '请先点击黄色辅助表格单元格'
  return '点击后映射到当前辅助表格单元格'
}

const resolveAssistGridPreviewCell = (
  userId: number,
  rowIndex: number,
  columnIndex: number
): AssistGridPreviewCell => {
  const key = buildAssistGridRowKey({ userId, rowIndex, columnIndex })
  const assistRow = assistGridRowMap.value.get(key)
  const field = assistRow?.fields[0]
  const sourceCell = field ? findRenderedCellByIdentity(cellIdentity(field.rowIndex, field.columnIndex)) : null
  const rule = field ? ruleMap.value.get(cellIdentity(field.rowIndex, field.columnIndex)) : undefined
  const sourceText = String(sourceCell?.text || '').trim()
  const label = String(rule?.label || sourceText || '点击选择原表格').trim()
  return {
    key,
    userId,
    rowIndex,
    columnIndex,
    label,
    sourceSummary: sourceCell ? `原表单：${sourceText || label}` : '',
    sourceCell
  }
}

const assistGridPreviewRows = computed<AssistGridPreviewRow[]>(() => {
  const userId = selectedAssistFillerUserId.value
  if (!userId) return []
  const rowCount = normalizeAssistGridSizeValue(assistGridRowCount.value)
  const columnCount = normalizeAssistGridSizeValue(assistGridColumnCount.value)
  return Array.from({ length: rowCount }, (_, rowIndex) => ({
    rowIndex,
    cells: Array.from({ length: columnCount }, (_, columnIndex) =>
      resolveAssistGridPreviewCell(userId, rowIndex, columnIndex)
    )
  }))
})

const selectedCellAssistRow = computed(() => {
  const cell = selectedCell.value
  if (!cell) return null
  return (
    editableAssistRows.value.find((row) =>
      row.fields.some(
        (field) => field.rowIndex === cell.rowIndex && field.columnIndex === cell.columnIndex
      )
    ) || null
  )
})

const selectedAssistRow = computed(() =>
  editableAssistRows.value.find((row) => row.rowKey === selectedAssistRowKey.value)
)

const ensureSelectedRuleStillExists = () => {
  if (selectedRuleKey.value && selectedCell.value) return
  selectedRuleKey.value = ruleRows.value.length ? ruleIdentity(ruleRows.value[0]) : ''
}

const ensureSelectedAssistRowStillExists = () => {
  if (selectedAssistRowKey.value && selectedAssistRow.value) return
  selectedAssistRowKey.value = editableAssistRows.value[0]?.rowKey || ''
}

const applyTemplateRules = () => {
  const nextRules = new Map<string, BatchRecordReportCellRuleVO>()
  ;(props.cellRules || [])
    .map(normalizeCellRule)
    .sort((left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex)
    .forEach((rule) => nextRules.set(ruleIdentity(rule), rule))
  ruleRows.value = Array.from(nextRules.values())
  editableAssistRows.value = normalizeAssistRows(props.assistRows || [])
  Object.keys(assistAssignments).forEach((rowKey) => delete assistAssignments[rowKey])
  syncAssistAssignmentsWithRows()
  applyAssistAssignments(props.fillAssignments || [])
  try {
    sheetLayout.value = parseSheetLayout(props.sheetLayoutJson)
    sheetLayoutError.value = sheetLayout.value ? '' : '当前模板缺少表单布局，无法进入填写配置。'
  } catch (error) {
    sheetLayout.value = null
    sheetLayoutError.value = resolveErrorMessage(error, '表单布局解析失败，无法进入填写配置。')
  }
  ensureSelectedRuleStillExists()
  ensureSelectedAssistRowStillExists()
  syncAssistGridStateWithRows()
}

const buildManualRuleFromCell = (cell: RuleEditorCell): BatchRecordReportCellRuleVO =>
  normalizeCellRule({
    rowIndex: cell.rowIndex,
    columnIndex: cell.columnIndex,
    valueType: 'STRING',
    componentFlag: cellRuleDefaultComponentMap.STRING,
    required: false,
    label: cell.text.trim() || `第 ${cell.rowIndex + 1} 行第 ${cell.columnIndex + 1} 列`,
    constraints: {},
    unit: '',
    source: 'MANUAL',
    confidence: 1,
    reviewed: true
  })

const selectRuleCell = (cell: RuleEditorCell) => {
  selectedRuleKey.value = cell.identity
}

const enableSelectedCellRule = () => {
  const cell = selectedCell.value
  if (readonlyMode.value || !cell || ruleMap.value.has(cell.identity)) return
  ruleRows.value = sortRules([...ruleRows.value, buildManualRuleFromCell(cell)])
  selectedRuleKey.value = cell.identity
}

const disableSelectedCellRule = () => {
  const key = selectedRuleKey.value
  if (readonlyMode.value || !key || !ruleMap.value.has(key)) return
  ruleRows.value = ruleRows.value.filter((rule) => ruleIdentity(rule) !== key)
  editableAssistRows.value = orderAssistGridRows(
    editableAssistRows.value.filter((row) =>
      row.fields.every((field) => cellIdentity(field.rowIndex, field.columnIndex) !== key)
    )
  )
  syncAssistAssignmentsWithRows()
  selectedRuleKey.value = key
}

const selectAssistFillerUser = (userId: number) => {
  if (!assistFillerUserIds.value.includes(userId)) return
  selectedAssistFillerUserId.value = userId
  ensureSelectedAssistGridCellStillExists()
}

const addAssistFillerUser = () => {
  if (readonlyMode.value) return
  const userId = Number(pendingAssistFillerUserId.value)
  if (!Number.isFinite(userId) || userId <= 0) return
  if (!assistUserOptions.value.some((option) => option.value === userId)) {
    message.warning('请选择有效填写人。')
    return
  }
  if (!assistFillerUserIds.value.includes(userId)) {
    assistFillerUserIds.value = sortAssistFillerUserIds([...assistFillerUserIds.value, userId])
  }
  pendingAssistFillerUserId.value = undefined
  selectedAssistFillerUserId.value = userId
  ensureSelectedAssistGridCellStillExists()
}

const assistGridMappedCountByUser = (userId: number) =>
  editableAssistRows.value.filter((row) => parseAssistGridRowKey(row.rowKey)?.userId === userId)
    .length

const removeAssistFillerUser = async (userId: number) => {
  if (readonlyMode.value) return
  const mappedCount = assistGridMappedCountByUser(userId)
  if (mappedCount > 0) {
    try {
      await ElMessageBox.confirm(
        `删除 ${resolveUserLabelById(userId)} 会同时移除该填写人的 ${mappedCount} 个映射，是否继续？`,
        '删除填写人',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch (error) {
      if (error === 'cancel' || error === 'close') return
      throw error
    }
  }
  assistFillerUserIds.value = assistFillerUserIds.value.filter((item) => item !== userId)
  editableAssistRows.value = orderAssistGridRows(
    editableAssistRows.value.filter((row) => parseAssistGridRowKey(row.rowKey)?.userId !== userId)
  )
  Object.keys(assistAssignments).forEach((rowKey) => {
    if (parseAssistGridRowKey(rowKey)?.userId === userId) {
      delete assistAssignments[rowKey]
    }
  })
  if (pendingAssistFillerUserId.value === userId) {
    pendingAssistFillerUserId.value = undefined
  }
  ensureSelectedAssistFillerStillExists()
  ensureSelectedAssistGridCellStillExists()
}

const handleAssistGridSizeChange = () => {
  assistGridRowCount.value = normalizeAssistGridSizeValue(assistGridRowCount.value)
  assistGridColumnCount.value = normalizeAssistGridSizeValue(assistGridColumnCount.value)
  const mappedGridKeys = editableAssistRows.value
    .map((row) => parseAssistGridRowKey(row.rowKey))
    .filter((key): key is AssistGridKey => Boolean(key))
  const minRows = Math.max(1, ...mappedGridKeys.map((key) => key.rowIndex + 1))
  const minColumns = Math.max(1, ...mappedGridKeys.map((key) => key.columnIndex + 1))
  if (assistGridRowCount.value < minRows) {
    assistGridRowCount.value = minRows
    message.warning('已有映射位于更靠后的行，需先取消映射后才能缩小行数。')
  }
  if (assistGridColumnCount.value < minColumns) {
    assistGridColumnCount.value = minColumns
    message.warning('已有映射位于更靠后的列，需先取消映射后才能缩小列数。')
  }
  ensureSelectedAssistGridCellStillExists()
}

const handleAssistGridCellClick = (cellKey: string) => {
  const parsed = parseAssistGridCellKey(cellKey)
  if (!parsed || !assistFillerUserIds.value.includes(parsed.userId)) return
  selectedAssistFillerUserId.value = parsed.userId
  selectedAssistGridCellKey.value = cellKey
}

const handleAssistGridCellDoubleClick = (gridCell: AssistGridPreviewCell) => {
  handleAssistGridCellClick(gridCell.key)
  if (!gridCell.sourceCell) return
  removeAssistGridCellMapping(gridCell.key)
}

const removeAssistGridCellMapping = (cellKey = selectedAssistGridCellKey.value) => {
  if (readonlyMode.value || !cellKey) return
  editableAssistRows.value = orderAssistGridRows(
    editableAssistRows.value.filter((row) => row.rowKey !== cellKey)
  )
  delete assistAssignments[cellKey]
  syncAssistAssignmentsWithRows()
}

const buildAssistGridCellDescription = (cell: RuleEditorCell) => {
  const rule = ruleMap.value.get(cell.identity)
  return String(rule?.helpText || rule?.label || cell.text || '辅助填写项').trim()
}

const mapSourceCellToSelectedAssistGridCell = (cell: RuleEditorCell) => {
  if (activeConfigMode.value !== 'assistMapping') return false
  if (readonlyMode.value) return true
  if (!selectedAssistFillerUserId.value || !assistFillerUserIds.value.includes(selectedAssistFillerUserId.value)) {
    message.warning('请先在右侧添加并选择填写人。')
    return true
  }
  const parsed = parseAssistGridCellKey(selectedAssistGridCellKey.value)
  if (!parsed || parsed.userId !== selectedAssistFillerUserId.value) {
    message.warning('请先点击黄色辅助表格中的一个单元格。')
    return true
  }
  if (sourceCellGridAssignmentMap.value.has(cell.identity)) {
    message.warning('该原表单元格已分配，请先在辅助表格取消映射后再重新分配。')
    return true
  }
  selectedRuleKey.value = cell.identity
  if (!selectedRule.value) {
    enableSelectedCellRule()
  }
  const rowKey = buildAssistGridRowKey(parsed)
  const nextRow: BatchRecordReportAssistRowVO = {
    rowKey,
    description: buildAssistGridCellDescription(cell),
    sort: calculateAssistGridSort(parsed),
    fields: [{ rowIndex: cell.rowIndex, columnIndex: cell.columnIndex }]
  }
  editableAssistRows.value = orderAssistGridRows([
    ...editableAssistRows.value.filter((row) => row.rowKey !== rowKey),
    nextRow
  ])
  assistAssignments[rowKey] = {
    candidateSourceType: 'USERS',
    candidateSourceIds: [parsed.userId],
    completionPolicy: 'ANY_ONE'
  }
  return true
}

const handleSourceCellClick = (cell: RuleEditorCell) => {
  if (mapSourceCellToSelectedAssistGridCell(cell)) return
  selectRuleCell(cell)
}

const removeSelectedCellFromAssistRows = () => {
  const cell = selectedCell.value
  if (!cell) return
  const key = cellIdentity(cell.rowIndex, cell.columnIndex)
  editableAssistRows.value = editableAssistRows.value.map((row) => ({
    ...row,
    fields: row.fields.filter((field) => cellIdentity(field.rowIndex, field.columnIndex) !== key)
  }))
}

const buildAssistRowDescriptionFromSelectedCell = () => {
  const cell = selectedCell.value
  const rule = selectedRule.value
  if (!cell) return ''
  return (
    String(rule?.helpText || rule?.label || cell.text || '').trim() ||
    `第 ${cell.rowIndex + 1} 行填写项`
  )
}

const addAssistRowFromSelectedCell = () => {
  if (readonlyMode.value) return
  const cell = selectedCell.value
  if (!cell) {
    throw new Error('请先选择一个单元格，再新增辅助行。')
  }
  if (!selectedRule.value) {
    enableSelectedCellRule()
  }
  const rowKey = `${ASSIST_ROW_KEY_PREFIX}_${Date.now()}_${cell.rowIndex}_${cell.columnIndex}`
  removeSelectedCellFromAssistRows()
  editableAssistRows.value = [
    ...editableAssistRows.value,
    {
      rowKey,
      description: buildAssistRowDescriptionFromSelectedCell(),
      sort: editableAssistRows.value.length + 1,
      fields: [{ rowIndex: cell.rowIndex, columnIndex: cell.columnIndex }]
    }
  ]
  ensureAssistAssignment(rowKey)
  selectedAssistRowKey.value = rowKey
}

const assignSelectedCellToAssistRow = (rowKey = selectedAssistRowKey.value) => {
  if (readonlyMode.value) return
  const cell = selectedCell.value
  const targetRow = editableAssistRows.value.find((row) => row.rowKey === rowKey)
  if (!cell || !targetRow) {
    throw new Error('请先选择单元格和辅助行，再分配归属。')
  }
  if (!selectedRule.value) {
    enableSelectedCellRule()
  }
  const key = cellIdentity(cell.rowIndex, cell.columnIndex)
  editableAssistRows.value = editableAssistRows.value.map((row) => {
    const fieldsWithoutSelectedCell = row.fields.filter(
      (field) => cellIdentity(field.rowIndex, field.columnIndex) !== key
    )
    if (row.rowKey !== rowKey) {
      return { ...row, fields: fieldsWithoutSelectedCell }
    }
    return {
      ...row,
      fields: normalizeAssistRowFields([
        ...fieldsWithoutSelectedCell,
        { rowIndex: cell.rowIndex, columnIndex: cell.columnIndex }
      ])
    }
  })
  selectedAssistRowKey.value = rowKey
}

const moveAssistRow = (rowKey: string, direction: -1 | 1) => {
  if (readonlyMode.value) return
  const currentIndex = editableAssistRows.value.findIndex((row) => row.rowKey === rowKey)
  const nextIndex = currentIndex + direction
  if (currentIndex < 0 || nextIndex < 0 || nextIndex >= editableAssistRows.value.length) return
  const nextRows = [...editableAssistRows.value]
  const currentRow = nextRows[currentIndex]
  nextRows[currentIndex] = nextRows[nextIndex]
  nextRows[nextIndex] = currentRow
  editableAssistRows.value = nextRows.map((row, index) => ({ ...row, sort: index + 1 }))
  selectedAssistRowKey.value = rowKey
}

const removeAssistRow = (rowKey: string) => {
  if (readonlyMode.value) return
  editableAssistRows.value = editableAssistRows.value
    .filter((row) => row.rowKey !== rowKey)
    .map((row, index) => ({ ...row, sort: index + 1 }))
  syncAssistAssignmentsWithRows()
  ensureSelectedAssistRowStillExists()
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
  if (readonlyMode.value || !selectedRule.value) return
  selectedRule.value.componentFlag = cellRuleDefaultComponentMap[value]
  selectedRule.value.constraints = cleanedRuleConstraints(selectedRule.value.constraints, value)
}

const ensureSelectedRuleConstraints = () => {
  if (!selectedRule.value) return null
  if (!selectedRule.value.constraints) {
    selectedRule.value.constraints = {}
  }
  return selectedRule.value.constraints
}

const setSelectedNumericConstraint = (key: NumericConstraintKey, value: number | null | undefined) => {
  if (readonlyMode.value) return
  const constraints = ensureSelectedRuleConstraints()
  if (!constraints) return
  if (value === null || value === undefined) {
    delete constraints[key]
    return
  }
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) {
    delete constraints[key]
    return
  }
  constraints[key] = numericValue
}

const selectedNumericMin = computed({
  get: () => selectedRule.value?.constraints?.min,
  set: (value: number | null | undefined) => setSelectedNumericConstraint('min', value)
})

const selectedNumericMax = computed({
  get: () => selectedRule.value?.constraints?.max,
  set: (value: number | null | undefined) => setSelectedNumericConstraint('max', value)
})

const normalizeStringSelectOptions = (options: unknown): StringSelectOption[] => {
  if (!Array.isArray(options)) return []
  return options
    .map((option) => {
      if (option == null) return null
      if (typeof option === 'string' || typeof option === 'number' || typeof option === 'boolean') {
        const value = String(option).trim()
        return value ? { label: value, value } : null
      }
      if (typeof option !== 'object') return null
      const record = option as Record<string, unknown>
      const value = String(record.value ?? record.label ?? '').trim()
      const label = String(record.label ?? value).trim()
      return value ? { label: label || value, value } : null
    })
    .filter((option): option is StringSelectOption => Boolean(option))
}

const selectedStringOptions = computed(() =>
  normalizeStringSelectOptions(selectedRule.value?.constraints?.options)
)

const setSelectedStringOptions = (options: StringSelectOption[]) => {
  if (readonlyMode.value) return
  const constraints = ensureSelectedRuleConstraints()
  if (!constraints) return
  constraints.selectionMode = 'single'
  constraints.options = options
}

const toggleSelectedStringDropdown = (value: boolean | string | number) => {
  if (readonlyMode.value) return
  const constraints = ensureSelectedRuleConstraints()
  if (!constraints) return
  if (!value) {
    delete constraints.selectionMode
    delete constraints.options
    return
  }
  const options = selectedStringOptions.value.length
    ? selectedStringOptions.value
    : [
        { label: '选项1', value: '选项1' },
        { label: '选项2', value: '选项2' }
      ]
  setSelectedStringOptions(options)
}

const addSelectedStringOption = () => {
  const nextIndex = selectedStringOptions.value.length + 1
  setSelectedStringOptions([
    ...selectedStringOptions.value,
    { label: `选项${nextIndex}`, value: `选项${nextIndex}` }
  ])
}

const updateSelectedStringOption = (optionIndex: number, value: string | number) => {
  const optionText = String(value || '').trim()
  const nextOptions = selectedStringOptions.value.map((option, index) =>
    index === optionIndex ? { label: optionText, value: optionText } : option
  )
  setSelectedStringOptions(nextOptions)
}

const removeSelectedStringOption = (optionIndex: number) => {
  setSelectedStringOptions(
    selectedStringOptions.value.filter((_, index) => index !== optionIndex)
  )
}

const normalizedAssistRowsForSave = () => {
  if (activeConfigMode.value === 'assistMapping') {
    if (ruleRows.value.length === 0) return []
    if (assistFillerUserIds.value.length === 0) {
      throw new Error('请先添加至少一个辅助表格填写人。')
    }
    const rows = orderAssistGridRows(normalizeAssistRows(editableAssistRows.value))
    if (rows.length === 0) {
      throw new Error('请先在辅助表格中完成原表单元格映射。')
    }
    const assignedCellKeys = new Set<string>()
    rows.forEach((row, rowIndex) => {
      const parsed = parseAssistGridRowKey(row.rowKey)
      if (!parsed) {
        throw new Error('存在旧版辅助映射，请切换到辅助表格后重新映射。')
      }
      if (!assistFillerUserIds.value.includes(parsed.userId)) {
        throw new Error(`辅助表格 ${rowIndex + 1} 的填写人已被删除，请重新选择填写人。`)
      }
      if (parsed.rowIndex >= assistGridRowCount.value || parsed.columnIndex >= assistGridColumnCount.value) {
        throw new Error(`辅助表格 ${rowIndex + 1} 超出当前表格范围，请先取消该映射。`)
      }
      if (!row.description.trim()) {
        throw new Error(`辅助表格 ${rowIndex + 1} 缺少描述。`)
      }
      if (row.fields.length !== 1) {
        throw new Error(`辅助表格 ${rowIndex + 1} 必须且只能映射一个原表单元格。`)
      }
      row.fields.forEach((field) => {
        const key = cellIdentity(field.rowIndex, field.columnIndex)
        if (assignedCellKeys.has(key)) {
          throw new Error(`原表单元格 R${field.rowIndex + 1}C${field.columnIndex + 1} 不能分配给多个填写人。`)
        }
        assignedCellKeys.add(key)
      })
    })
    const uncoveredRule = ruleRows.value.find((rule) => !assignedCellKeys.has(ruleIdentity(rule)))
    if (uncoveredRule) {
      throw new Error(`原表单元格 R${uncoveredRule.rowIndex + 1}C${uncoveredRule.columnIndex + 1} 尚未分配给填写人。`)
    }
    return rows
  }
  const rows = normalizeAssistRows(editableAssistRows.value)
  if (rows.length === 0) {
    return []
  }
  const assignedCellKeys = new Set<string>()
  rows.forEach((row, rowIndex) => {
    if (!row.description.trim()) {
      throw new Error(`Assist row ${rowIndex + 1} requires a description.`)
    }
    if (row.fields.length === 0) {
      throw new Error(`Assist row ${rowIndex + 1} requires at least one cell.`)
    }
    row.fields.forEach((field) => {
      const key = cellIdentity(field.rowIndex, field.columnIndex)
      if (assignedCellKeys.has(key)) {
        throw new Error(`Cell R${field.rowIndex + 1}C${field.columnIndex + 1} cannot belong to multiple assist rows.`)
      }
      assignedCellKeys.add(key)
    })
  })
  return rows
}

const normalizedAssistAssignmentsForSave = (rows: BatchRecordReportAssistRowVO[]) => {
  return rows.map((row, rowIndex) => {
    const parsed = parseAssistGridRowKey(row.rowKey)
    if (parsed) {
      const userId = parsed.userId
      return {
        scopeKey: row.rowKey,
        candidateSourceType: 'USERS' as const,
        candidateSourceIds: [userId],
        completionPolicy: 'ANY_ONE' as const,
        enabled: true,
        remark: row.description
      }
    }
    const assignment = ensureAssistAssignment(row.rowKey)
    const candidateSourceIds = normalizeAssignmentIds(assignment.candidateSourceIds)
    if (candidateSourceIds.length === 0) {
      throw new Error(`辅助行 ${rowIndex + 1} 缺少填写人或角色。`)
    }
    return {
      scopeKey: row.rowKey,
      candidateSourceType: normalizeAssignmentSourceType(assignment.candidateSourceType),
      candidateSourceIds,
      completionPolicy: normalizeAssignmentPolicy(assignment.completionPolicy),
      enabled: true,
      remark: row.description
    }
  })
}

const validateRuleRowsBeforeSave = () => {
  const invalidRule = ruleRows.value.find((rule) => {
    if (rule.valueType !== 'NUMBER') return false
    const min = rule.constraints?.min
    const max = rule.constraints?.max
    return typeof min === 'number' && typeof max === 'number' && min > max
  })
  if (!invalidRule) return
  const label = invalidRule.label || `第 ${invalidRule.rowIndex + 1} 行第 ${invalidRule.columnIndex + 1} 列`
  throw new Error(`${label} 的数字最小值不能大于最大值。`)
}

const isSignatureRule = (rule: BatchRecordReportCellRuleVO) =>
  rule.valueType === 'SIGNATURE' || String(rule.componentFlag || '').toLowerCase().includes('signature')

const normalizedSignatureMarkersForSave = (rules: BatchRecordReportCellRuleVO[]) => {
  const markerMap = new Map(
    (props.signatureCellMarkers || []).map((marker) => [
      buildTemplateFieldIdentity(marker.rowIndex, marker.columnIndex),
      marker
    ])
  )
  return sortRules(rules)
    .filter(isSignatureRule)
    .map((rule) => {
      const marker = markerMap.get(buildTemplateFieldIdentity(rule.rowIndex, rule.columnIndex))
      return {
        ...(marker || {}),
        rowIndex: rule.rowIndex,
        columnIndex: rule.columnIndex,
        enabled: true,
        actionType: marker?.actionType || 'FORM_REVIEW',
        label: marker?.label || rule.label || '签名',
        signatureCellKey:
          marker?.signatureCellKey || buildTemplateFieldIdentity(rule.rowIndex, rule.columnIndex)
      } as BatchRecordReportSignatureCellMarkerVO
    })
}

const loadCandidateOptions = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [users, roles] = await Promise.all([
      getSimpleUserList(),
      getSimpleRoleList()
    ])
    simpleUserOptions.value = users
    simpleRoleOptions.value = roles
  } catch (error) {
    const resolved = resolveErrorMessage(error, '填写候选人读取失败，请联系管理员。')
    errorMessage.value = resolved
    message.error(resolved)
  } finally {
    loading.value = false
  }
}

const reloadTemplateRules = (options?: { keepAiCandidates?: boolean }) => {
  applyTemplateRules()
  if (!options?.keepAiCandidates) {
    clearAiCandidates()
  }
  void loadCandidateOptions()
}

const handleAutoDetect = async () => {
  if (aiDetectDisabled.value) return
  const templateId = props.template?.templateId
  const versionNo = props.template?.versionNo
  if (!templateId || !versionNo) {
    errorMessage.value = '当前模板缺少有效的模板编号或版本号，无法执行 AI 识别。'
    return
  }
  autoDetecting.value = true
  errorMessage.value = ''
  clearAiCandidates()
  try {
    const response = await autoDetectTemplateFillRules(templateId, versionNo)
    pendingAiCandidates.value = response.candidates || []
    const switchedToDraft = response.versionNo !== versionNo
    aiDetectSummary.value = switchedToDraft
      ? response.candidateCount
        ? `已识别 ${response.candidateCount} 个候选规则，并已自动生成${response.draftCreated ? '新的' : '复用现有'}草稿版本 ${response.versionNo}。请切换到草稿后应用并保存。`
        : `未识别到新的未配置填写字段，但已自动生成${response.draftCreated ? '新的' : '复用现有'}草稿版本 ${response.versionNo}。请切换到草稿后继续保存。`
      : response.candidateCount
        ? `已识别 ${response.candidateCount} 个候选规则。请检查后点击“应用识别结果”，最后再保存。`
        : '未识别到新的未配置填写字段。'
    preserveAiCandidatesOnReload.value = switchedToDraft
    emit('draft-version-ready', response)
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error, 'AI 自动识别失败，现有填写配置未改变。')
    message.error(errorMessage.value)
  } finally {
    autoDetecting.value = false
  }
}

const clearAiCandidates = () => {
  pendingAiCandidates.value = []
  aiDetectSummary.value = ''
}

const applyAiCandidates = async () => {
  if (readonlyMode.value || !pendingAiCandidates.value.length) return
  await ElMessageBox.confirm(
    '候选规则只会加入当前草稿编辑状态，不会立即写入服务器。请确认后继续。',
    '应用 AI 识别结果',
    { type: 'warning', confirmButtonText: '应用', cancelButtonText: '取消' }
  )
  const currentRuleMap = new Map(ruleRows.value.map((rule) => [ruleIdentity(rule), rule]))
  pendingAiCandidates.value.forEach((candidate) => {
    const identity = cellIdentity(candidate.rowIndex, candidate.columnIndex)
    if (currentRuleMap.has(identity)) return
    currentRuleMap.set(identity, normalizeCellRule({
      rowIndex: candidate.rowIndex,
      columnIndex: candidate.columnIndex,
      label: candidate.label,
      valueType: candidate.valueType as BatchRecordReportCellValueType,
      componentFlag: candidate.componentFlag,
      required: candidate.required,
      constraints: candidate.constraints,
      unit: candidate.unit,
      placeholder: candidate.placeholder,
      helpText: candidate.helpText,
      source: 'AI',
      confidence: candidate.confidence,
      reviewed: false
    }))
  })
  const appliedCount = pendingAiCandidates.value.length
  ruleRows.value = Array.from(currentRuleMap.values()).sort((left, right) =>
    left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex
  )
  pendingAiCandidates.value = []
  aiDetectSummary.value = `已应用 ${appliedCount} 个候选规则到当前编辑状态，请检查后点击“保存填写配置”。`
}

const confirmAllRules = () => {
  if (readonlyMode.value) {
    errorMessage.value = '只有草稿版本可以保存填写配置。'
    return
  }
  errorMessage.value = ''
  try {
    validateRuleRowsBeforeSave()
    if (!sheetLayout.value) {
      throw new Error('当前模板缺少可保存的规则布局。')
    }
    const rules = ruleRows.value.map(toManualReviewedRule)
    const assistRowsForSave = normalizedAssistRowsForSave()
    emit('save', {
      sheetLayoutJson: JSON.stringify(sheetLayout.value),
      cellRules: rules,
      signatureCellMarkers: normalizedSignatureMarkersForSave(rules),
      assistRows: assistRowsForSave,
      fillAssignments: normalizedAssistAssignmentsForSave(assistRowsForSave)
    })
  } catch (error) {
    const resolved = resolveErrorMessage(error, '填写配置保存前校验失败。')
    errorMessage.value = resolved
    message.error(resolved)
  }
}

watch(
  () => [
    dialogVisible.value,
    props.template?.templateId,
    props.template?.versionNo,
    props.sheetLayoutJson,
    props.cellRules,
    props.assistRows,
    props.fillAssignments
  ] as const,
  ([visible]) => {
    if (!visible) return
    reloadTemplateRules({ keepAiCandidates: preserveAiCandidatesOnReload.value })
    preserveAiCandidatesOnReload.value = false
  },
  { immediate: true }
)
</script>

<style scoped>
.batch-record-cell-rules-editor {
  display: grid;
  height: clamp(520px, calc(100vh - 96px), 900px);
  min-height: 0;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 14px;
}

.batch-record-cell-rules-editor__main-panel {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
}

.batch-record-cell-rules-editor__summary {
  display: flex;
  flex: 0 0 auto;
  min-height: 34px;
  align-items: center;
  flex-wrap: wrap;
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

.batch-record-cell-rules-editor__mode-switch {
  flex: 0 0 auto;
}

.batch-record-cell-rules-editor__workspace {
  display: grid;
  min-width: 0;
  flex: 1 1 auto;
  min-height: 0;
  grid-template-columns: minmax(0, 1fr);
}

.batch-record-cell-rules-editor__workspace--assist-mapping {
  grid-template-columns: minmax(320px, 1fr) minmax(280px, 0.85fr);
  gap: 14px;
}

.batch-record-cell-rules-editor__preview,
.batch-record-cell-rules-editor__assist-preview-panel,
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

.batch-record-cell-rules-editor__assist-preview-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  border-color: #f1d36d;
  background: #fff8d6;
}

.batch-record-cell-rules-editor__side-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border-color: #9cc7ff;
  background: #f7fbff;
}

.batch-record-cell-rules-editor__side-panel.is-mapping-control {
  border-color: #9cc7ff;
  background: #eaf3ff;
}

.batch-record-cell-rules-editor__side-scroll {
  min-height: 0;
  flex: 1 1 auto;
  overflow: auto;
  padding: 12px;
}

.batch-record-cell-rules-editor__side-intro {
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #b9d7ff;
  border-radius: 8px;
  background: #eaf3ff;
}

.batch-record-cell-rules-editor__side-intro strong {
  display: block;
  color: #123b72;
  font-size: 14px;
}

.batch-record-cell-rules-editor__side-intro p {
  margin: 4px 0 0;
  color: #31547c;
  font-size: 12px;
  line-height: 1.4;
}

.batch-record-cell-rules-editor__side-actions {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #cfe1f8;
  background: #ffffff;
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

.batch-record-cell-rules-editor__cell.is-assist-mapped {
  background: #e5e7eb;
  color: #6b7280;
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

.batch-record-cell-rules-editor__cell-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.batch-record-cell-rules-editor__cell-button:hover {
  background: rgba(37, 99, 235, 0.08);
}

.batch-record-cell-rules-editor__cell-button:disabled:hover {
  background: transparent;
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

.batch-record-cell-rules-editor__range-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
}

.batch-record-cell-rules-editor__dropdown-options,
.batch-record-cell-rules-editor__assist-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.batch-record-cell-rules-editor__dropdown-switch,
.batch-record-cell-rules-editor__assist-head,
.batch-record-cell-rules-editor__assist-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.batch-record-cell-rules-editor__dropdown-option {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.batch-record-cell-rules-editor__assist-preview-scroll {
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.batch-record-cell-rules-editor__assist-grid-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  color: #7a5300;
}

.batch-record-cell-rules-editor__assist-grid {
  width: 100%;
  min-width: 360px;
  table-layout: fixed;
  border-collapse: collapse;
}

.batch-record-cell-rules-editor__assist-grid td {
  position: relative;
  min-width: 96px;
  height: 96px;
  border: 1px solid #e0bd45;
  background: #fffdf1;
  padding: 6px;
  vertical-align: stretch;
}

.batch-record-cell-rules-editor__assist-grid-cell {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 4px;
  border: 1px dashed transparent;
  border-radius: 6px;
  background: transparent;
  color: #3f2f09;
  cursor: pointer;
  font: inherit;
  padding: 8px;
  text-align: left;
}

.batch-record-cell-rules-editor__assist-grid-cell.is-selected {
  border-color: #b7791f;
  background: #fff1b8;
}

.batch-record-cell-rules-editor__assist-grid-cell.is-mapped {
  background: #ffffff;
}

.batch-record-cell-rules-editor__assist-grid-cell span {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  color: #271d06;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-cell-rules-editor__assist-grid-cell small {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  color: #806018;
  font-size: 11px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-cell-rules-editor__assist-grid-control,
.batch-record-cell-rules-editor__assist-filler-control {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px;
  border: 1px solid #b9d7ff;
  border-radius: 8px;
  background: #f7fbff;
}

.batch-record-cell-rules-editor__assist-grid-control-head strong {
  color: #123b72;
  font-size: 13px;
}

.batch-record-cell-rules-editor__assist-grid-control-head p {
  margin: 3px 0 0;
  color: #31547c;
  font-size: 12px;
  line-height: 1.4;
}

.batch-record-cell-rules-editor__assist-grid-size {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
}

.batch-record-cell-rules-editor__assist-grid-size label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #31547c;
  font-size: 12px;
}

.batch-record-cell-rules-editor__assist-filler-add {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.batch-record-cell-rules-editor__assist-filler-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.batch-record-cell-rules-editor__assist-filler-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 1px solid #cfe1f8;
  border-radius: 8px;
  background: #ffffff;
}

.batch-record-cell-rules-editor__assist-filler-item.is-selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.batch-record-cell-rules-editor__assist-filler-item > button {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  border: 0;
  background: transparent;
  color: #172033;
  cursor: pointer;
  font: inherit;
  padding: 0;
  text-align: left;
}

.batch-record-cell-rules-editor__assist-filler-item span {
  color: #667085;
  font-size: 12px;
}

.batch-record-cell-rules-editor__assist-section {
  padding-top: 12px;
  border-top: 1px solid #edf1f6;
}

.batch-record-cell-rules-editor__assist-head {
  align-items: flex-start;
}

.batch-record-cell-rules-editor__assist-head strong {
  color: #172033;
  font-size: 13px;
}

.batch-record-cell-rules-editor__assist-head p {
  margin: 3px 0 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.4;
}

.batch-record-cell-rules-editor__assist-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.batch-record-cell-rules-editor__assist-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.batch-record-cell-rules-editor__assist-row.is-selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.batch-record-cell-rules-editor__assist-select {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 0;
  background: transparent;
  color: #172033;
  cursor: pointer;
  font: inherit;
  padding: 0;
  text-align: left;
}

.batch-record-cell-rules-editor__assist-select span {
  color: #667085;
  font-size: 12px;
}

.batch-record-cell-rules-editor__assist-select-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.batch-record-cell-rules-editor__assist-select-copy > span {
  overflow: hidden;
  max-width: 220px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-record-cell-rules-editor__assist-assignment {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.batch-record-cell-rules-editor__assist-assignment strong {
  color: #172033;
  font-size: 13px;
}

.batch-record-cell-rules-editor__assist-assignment-grid {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) 112px;
  gap: 8px;
}

@media (max-width: 1180px) {
  .batch-record-cell-rules-editor {
    height: auto;
    max-height: none;
    grid-template-columns: minmax(0, 1fr);
  }

  .batch-record-cell-rules-editor__main-panel {
    min-height: 520px;
  }

  .batch-record-cell-rules-editor__workspace {
    min-height: 420px;
    grid-template-columns: minmax(0, 1fr);
  }

  .batch-record-cell-rules-editor__workspace--assist-mapping {
    grid-template-columns: minmax(0, 1fr);
  }

  .batch-record-cell-rules-editor__side-panel {
    height: auto;
    min-height: 420px;
  }
}
</style>
