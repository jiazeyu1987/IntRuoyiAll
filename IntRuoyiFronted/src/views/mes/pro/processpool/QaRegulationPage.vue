<template>
  <div class="qa-regulation-page" data-qa-regulation-page>
    <ContentWrap class="qa-regulation-page__project-wrap" data-qa-regulation-dcc-project>
      <div class="qa-regulation-page__header">
        <div class="qa-regulation-page__title">QA 规程配置</div>
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
        <div
          class="qa-regulation-page__version-publish"
          data-qa-regulation-version-publish
        >
          <label class="qa-regulation-page__header-field">
            <span class="qa-regulation-page__header-field-label">版本</span>
            <el-input
              v-model="qaRegulationDraft.versionNo"
              aria-label="规程版本"
              size="small"
              placeholder="请输入版本"
              class="qa-regulation-page__version-input"
            />
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
          <el-tag type="warning" effect="plain">{{ qaRegulationDraft.lifecycleStatus }}</el-tag>
          <el-button
            data-qa-regulation-header-save
            :loading="qaRegulationSaving"
            :disabled="!selectedDccProjectCode || qaCurrentConfigurationLoading || qaRegulationPublishing"
            @click="previewQaRegulationDraft"
          >
            保存草稿
          </el-button>
          <el-button
            type="primary"
            :loading="qaRegulationPublishing"
            :disabled="!selectedDccProjectCode || qaCurrentConfigurationLoading || qaRegulationSaving"
            @click="runQaPublishPrecheck"
          >
            发布规程
          </el-button>
        </div>
      </div>

      <div
        v-if="dccProjectCodeLoadError"
        class="qa-regulation-page__load-error"
        data-qa-regulation-project-load-error
      >
        <el-alert
          :title="dccProjectCodeLoadError"
          type="error"
          :closable="false"
          show-icon
        />
        <el-button type="primary" plain @click="retryLoadDccProjectCodes">重新加载</el-button>
      </div>
    </ContentWrap>

    <template v-if="selectedDccProjectCode">
    <ContentWrap class="qa-regulation-page__tabs-wrap">
      <el-tabs
        v-model="qaActiveTab"
        class="qa-regulation-page__tabs qa-regulation-page__tabs--flat"
        data-qa-regulation-tabs
      >
        <el-tab-pane label="总览" name="overview" />
        <el-tab-pane label="检验项目" name="items" />
      </el-tabs>
    </ContentWrap>

    <ContentWrap
      v-show="selectedDccProjectCode && qaActiveTab === 'overview'"
      v-loading="qaCurrentConfigurationLoading"
    >
      <el-card shadow="never" data-qa-regulation-scope>
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
        class="qa-regulation-page__overview-note"
        data-qa-regulation-overview-note
      >
        <template #header>备注</template>
        <ol
          class="qa-regulation-page__overview-note-list"
          data-qa-regulation-overview-note-list
        >
          <li>
            设备初次开机、模具更换、参数调整、模具维修等需要按照抽样规则进行首件检验；
          </li>
          <li>
            首检如果发现不合格，及时向部门主管/领导汇报，待问题得到纠正后，生产稳定之后，重新进行首检，检验全部合格后，才可转入正常生产；
          </li>
          <li>如果样本量等于或超过批量，则进行100%检验；</li>
          <li>
            过程巡检应每班记录两次，上午和下午各一次，巡检过程中若发现产品不合格，应及时向部门主管反映不合格问题，并对之前生产的产品进行隔离，问题纠正之后，进行双倍检验，确认无异常之后，转入正常抽样。然后对之前生产的产品组织评审，根据评审结果对该批次产品进行处理。
          </li>
        </ol>
      </el-card>
    </ContentWrap>

    <ContentWrap
      v-show="qaActiveTab === 'items'"
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
          table-key="mes.qa.regulation.items.processMethods.v2"
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
              data-user-table-key="mes.qa.regulation.items.processMethods.v2"
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
                v-if="isQaItemsColumnVisible('standardText')"
                label="接受标准"
                prop="standardText"
                :min-width="getQaItemsColumnMinWidthString('standardText', 280)"
              v-bind="sortColumnAttrs('standardText')"
              >
                <template #default="{ row }">
                  <el-input v-model="row.standardText" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
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
                  <el-input v-model="row.inspectionMethod" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
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
                  <div class="qa-regulation-page__equipment-editor">
                    <el-input
                      v-model="row.inspectionTool"
                      placeholder="检验器具及设备说明"
                    />
                    <div
                      v-for="(option, optionIndex) in getQaRegulationItemEquipmentOptions(row)"
                      :key="`${row.itemCode}-${optionIndex}`"
                      class="qa-regulation-page__equipment-option"
                      data-qa-regulation-equipment-option
                    >
                      <el-input-number
                        v-model="option.equipmentId"
                        :controls="false"
                        :min="1"
                        placeholder="设备ID"
                        class="qa-regulation-page__equipment-id"
                      />
                      <el-input v-model="option.equipmentCode" placeholder="设备编码" />
                      <el-input v-model="option.equipmentName" placeholder="设备名称" />
                      <el-input v-model="option.equipmentNumber" placeholder="设备编号" />
                      <el-switch v-model="option.defaultFlag" active-text="默认" />
                      <el-button
                        text
                        type="danger"
                        @click="removeQaRegulationEquipmentOption(row, optionIndex)"
                      >
                        删除
                      </el-button>
                    </div>
                    <el-button
                      size="small"
                      data-qa-regulation-equipment-option-add
                      @click="addQaRegulationEquipmentOption(row)"
                    >
                      添加正式设备
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isQaItemsColumnVisible('samplingPlan')"
                label="抽样方案"
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
                      {{ row.sourceOriginalExcerpt || 'QA 手工新增项目需补充对应 PDF/规程原文摘录。' }}
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

    <ContentWrap v-show="qaActiveTab === 'verification'">
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
            table-key="mes.qa.regulation.pqcPreview"
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
                data-user-table-key="mes.qa.regulation.pqcPreview"
                @header-dragend="handleQaPqcPreviewHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
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
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { useClipboard } from '@vueuse/core'
import { useRoute } from 'vue-router'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
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
import {
  QcTemplateApi,
  type QaInspectionRegulationEquipmentOptionVO,
  type QaInspectionRegulationInspectionTypeRuleVO,
  type QaInspectionRegulationItemVO,
  type QaInspectionRegulationProcessVO,
  type QaInspectionRegulationPublishedVersionVO,
  type QaInspectionRegulationProjectStatusVO,
  type QaInspectionRegulationSaveReqVO
} from '@/api/mes/qc/template'
import {
  isQaInspectionSamplingPlanComplete,
  parseQaInspectionSamplingPlan,
  resolveQaApplicableInspectionTypes,
  type QaInspectionTypeValue
} from './qaRegulationSampling'

defineOptions({ name: 'MesProProcessPoolQaRegulation' })

type QaInspectionResultType = 'BOOLEAN' | 'NUMERIC' | 'TEXT'
type QaRegulationTabName = 'overview' | 'items' | 'verification'

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

interface QaRegulationEquipmentOptionDraft {
  equipmentId?: number
  equipmentCode: string
  equipmentName: string
  equipmentNumber: string
  defaultFlag?: boolean
  sort?: number
}

interface QaRegulationItem {
  qaProcessId?: number
  processCode: string
  processName: string
  processSort: number
  itemSort: number
  itemCode: string
  itemName: string
  inspectionMethod: string
  inspectionTool: string
  equipmentOptions?: QaRegulationEquipmentOptionDraft[]
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

const DCC_PROJECT_CODE_PAGE_SIZE = 200
const QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY =
  'int-ruoyi:qa-regulation:last-dcc-project-code-id'

const qaActiveTab = ref<QaRegulationTabName>('overview')
const qaItemsQuery = reactive<QaLocalListQuery>({ pageNo: 1, pageSize: 10 })
const qaChecksQuery = reactive<QaLocalListQuery>({ pageNo: 1, pageSize: 10 })
const qaPqcPreviewQuery = reactive<QaLocalListQuery>({ pageNo: 1, pageSize: 10 })
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
  { key: 'samplingPlan', label: '抽样方案', minWidth: 240, sortable: false },
  { key: 'itemCode', label: '检验项目编码', width: 130, visible: false },
  { key: 'applicableTypes', label: '适用检验类型', minWidth: 210 },
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
  { key: 'inspectionTypeText', label: '检验类型', minWidth: 110 },
  { key: 'roundText', label: '轮次', minWidth: 110 },
  { key: 'plannedQuantityText', label: '计划数量', minWidth: 110 },
  { key: 'regulationVersionNo', label: '规程版本', minWidth: 110 },
  { key: 'taskIdentity', label: '任务身份', minWidth: 260 }
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
} = useUserTableColumns('mes.qa.regulation.items.processMethods.v2', qaItemsDefaultColumns)

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
} = useUserTableColumns('mes.qa.regulation.pqcPreview', qaPqcPreviewDefaultColumns)

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
const qaRegulationSaving = ref(false)
const qaRegulationPublishing = ref(false)
const qaCurrentConfigurationLoading = ref(false)
const qaCurrentConfigurationLoadError = ref('')
let qaCurrentConfigurationLoadSerial = 0

const dccProjectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const dccProjectCodeOptionsLoading = ref(false)
const dccProjectCodeLoadError = ref('')
const selectedDccProjectCode = ref<DccProjectCodeRespVO>()
const qaRegulationProjectStatusByDccId = ref(
  new Map<number, QaInspectionRegulationProjectStatusVO>()
)
const qaCurrentPublishedVersion = ref<QaInspectionRegulationPublishedVersionVO>()
const qaCurrentPublishedVersionLoading = ref(false)
const qaCurrentPublishedVersionLoadError = ref('')
let qaCurrentPublishedVersionLoadSerial = 0

const route = useRoute()
const { copy: copyQaProjectSelectionToClipboard } = useClipboard({ legacy: true })

const QA_INSPECTION_TYPE_LABELS: Record<QaInspectionTypeValue, string> = {
  FIRST: '首检',
  PATROL_AM: '上午巡检',
  PATROL_PM: '下午巡检',
  FINAL: '末检'
}

const resolveQaInspectionTypeLabel = (inspectionType: QaInspectionTypeValue) =>
  QA_INSPECTION_TYPE_LABELS[inspectionType]

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
  resolveQaApplicableInspectionTypes(item.samplingPlanText, finalInspectionRequired.value)

const pagedQaRegulationItems = computed(() =>
  paginateQaRows(qaRegulationItems.value, qaItemsQuery)
)

const selectedDccProjectCodeLabel = computed(() =>
  selectedDccProjectCode.value ? formatDccProjectCodeOption(selectedDccProjectCode.value) : ''
)

const resolveDccProjectCodeErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message.trim()
  }
  return String(error)
}

const formatDccProjectCodeOption = (project: DccProjectCodeRespVO) =>
  [project.projectCode, project.projectName, project.docControlNo].filter(Boolean).join(' / ')

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

const cloneQaRegulationItems = (items: QaRegulationItem[]) =>
  items.map((item) => ({
    ...item,
    equipmentOptions: item.equipmentOptions?.map((option) => ({ ...option }))
  }))

const replaceQaInspectionTypeRules = (
  rules: QaInspectionRegulationInspectionTypeRuleVO[]
) => {
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
          equipmentOptions: item.equipmentOptions?.map((option) => ({ ...option })),
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
          sourceOriginalMethod: item.sourceOriginalMethod
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
  qaPublishedVersionNo.value = ''
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
  qaPublishedVersionNo.value =
    configuration.lifecycleStatus === 'PUBLISHED' ? configuration.versionNo : ''
  qaItemsQuery.pageNo = 1
}

const createQaRegulationProjectStatusMap = (
  statuses: QaInspectionRegulationProjectStatusVO[]
) => {
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
    qaRegulationProjectStatusByDccId.value =
      createQaRegulationProjectStatusMap(projectStatuses)
    dccProjectCodeOptions.value = sortDccProjectCodeOptionsByQaStatus(mergedOptions)
  } catch (error) {
    dccProjectCodeLoadError.value =
      'DCC 项目代码加载失败：' + resolveDccProjectCodeErrorMessage(error)
    throw error
  } finally {
    dccProjectCodeOptionsLoading.value = false
  }
}

const loadCurrentPublishedQaRegulationVersion = async (
  project?: DccProjectCodeRespVO
) => {
  const loadSerial = ++qaCurrentPublishedVersionLoadSerial
  qaCurrentPublishedVersion.value = undefined
  qaCurrentPublishedVersionLoadError.value = ''
  qaCurrentPublishedVersionLoading.value = false
  if (!project) {
    return
  }
  const dccProjectCodeId = resolvePositiveId(project.id, 'DCC 项目代码 ID')
  const status = qaRegulationProjectStatusByDccId.value.get(dccProjectCodeId)
  if (status?.lifecycleStatus !== 'PUBLISHED' || !status.currentVersionId) {
    return
  }
  qaCurrentPublishedVersionLoading.value = true
  try {
    const publishedVersion = await QcTemplateApi.getPublishedQaRegulationVersion(
      dccProjectCodeId,
      status.currentVersionId
    )
    if (loadSerial !== qaCurrentPublishedVersionLoadSerial) {
      return
    }
    if (
      publishedVersion.dccProjectCodeId !== dccProjectCodeId ||
      publishedVersion.publishedVersionId !== status.currentVersionId
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
  const rawId = window.localStorage.getItem(
    QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY
  )
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
    return
  }
  const dccProjectCodeId = resolvePositiveId(project.id, 'DCC 项目代码 ID')
  qaRegulationDraft.dccProjectCodeId = dccProjectCodeId
  await Promise.all([
    loadCurrentQaRegulation(dccProjectCodeId),
    loadCurrentPublishedQaRegulationVersion(project)
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

const resolveInitialDccProjectCodeId = () => {
  const queryValue = Array.isArray(route.query.dccProjectCodeId)
    ? route.query.dccProjectCodeId[0]
    : route.query.dccProjectCodeId
  return queryValue
    ? resolvePositiveId(queryValue, '链接中的 DCC 项目代码 ID')
    : readLastDccProjectCodeSelectionId()
}

const initializeQaRegulationPage = async () => {
  await loadDccProjectCodeOptions()
  const projectId = resolveInitialDccProjectCodeId()
  if (projectId) {
    await handleDccProjectCodeChange(projectId)
  }
}

onMounted(() => {
  void initializeQaRegulationPage().catch((error) => {
    const message = resolveDccProjectCodeErrorMessage(error)
    dccProjectCodeLoadError.value = message
    ElMessage.error(message)
  })
})

const formatQaItemProcessName = (item: QaRegulationItem) =>
  item.processName.trim() || '未填写 QA 工序'

const formatQaItemSamplingPlan = (item: QaRegulationItem) =>
  item.samplingPlanText?.trim() || '未填写抽样方案'

const getQaRegulationItemEquipmentOptions = (item: QaRegulationItem) => {
  if (!item.equipmentOptions) {
    item.equipmentOptions = []
  }
  return item.equipmentOptions
}

const addQaRegulationEquipmentOption = (item: QaRegulationItem) => {
  getQaRegulationItemEquipmentOptions(item).push({
    equipmentId: undefined,
    equipmentCode: '',
    equipmentName: '',
    equipmentNumber: '',
    defaultFlag: false,
    sort: getQaRegulationItemEquipmentOptions(item).length + 1
  })
}

const removeQaRegulationEquipmentOption = (
  item: QaRegulationItem,
  optionIndex: number
) => {
  getQaRegulationItemEquipmentOptions(item).splice(optionIndex, 1)
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
    equipmentOptions: [],
    samplingPlanText: '',
    resultType: 'BOOLEAN',
    standardText: '',
    critical: false,
    failureRule: '',
    sourceNote: ''
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

const buildQaRegulationEquipmentOptions = (
  item: QaRegulationItem,
  settings: { publishing?: boolean } = {}
): QaInspectionRegulationEquipmentOptionVO[] => {
  const optionRows = getQaRegulationItemEquipmentOptions(item)
  const publishing = Boolean(settings.publishing)
  return optionRows.map((option, index) => {
    const context = item.itemName.trim() || item.itemCode.trim() || '检验项目'
    return {
      equipmentId: resolvePositiveId(option.equipmentId, context + '检验设备 ID'),
      equipmentCode: resolveRequiredText(option.equipmentCode, context + '设备编码'),
      equipmentName: resolveRequiredText(option.equipmentName, context + '设备名称'),
      equipmentNumber: resolveRequiredText(option.equipmentNumber, context + '设备编号'),
      defaultFlag: Boolean(option.defaultFlag),
      sort: option.sort || index + 1
    }
  }).filter((option) => publishing || option.equipmentId > 0)
}

const buildQaRegulationSaveItem = (
  item: QaRegulationItem,
  itemSort: number,
  settings: { publishing?: boolean } = {}
): QaInspectionRegulationItemVO => {
  const itemName = resolveRequiredText(item.itemName, '检验项目名称')
  const samplingPlan = parseQaInspectionSamplingPlan(item.samplingPlanText, itemName)
  const applicableInspectionTypes = Array.from(
    new Set(
      resolveQaItemApplicableTypes(item).map((type) =>
        type === 'PATROL_AM' || type === 'PATROL_PM' ? 'PATROL' : type
      )
    )
  ) as Array<'FIRST' | 'PATROL' | 'FINAL'>
  const equipmentOptions = buildQaRegulationEquipmentOptions(item, settings)
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
    equipmentRequired: equipmentOptions.length > 0,
    equipmentOptions,
    resultType: item.resultType,
    applicableInspectionTypes,
    firstInspectionQuantity: applicableInspectionTypes.includes('FIRST')
      ? samplingPlan.firstInspectionQuantity
      : undefined,
    patrolInspectionRatio: applicableInspectionTypes.includes('PATROL')
      ? samplingPlan.patrolInspectionRatio
      : undefined,
    critical: item.critical,
    failureRule: item.failureRule.trim() || undefined,
    sourceNote: item.sourceNote.trim() || undefined,
    sourceOriginalPage: item.sourceOriginalPage,
    sourceOriginalItem: item.sourceOriginalItem?.trim() || undefined,
    sourceOriginalExcerpt: item.sourceOriginalExcerpt?.trim() || undefined,
    sourceOriginalMethod: item.sourceOriginalMethod?.trim() || undefined
  }
}

const buildQaRegulationProcesses = (
  settings: { publishing?: boolean } = {}
): QaInspectionRegulationProcessVO[] => {
  const groups = new Map<string, { code: string; name: string; sort: number; items: QaRegulationItem[] }>()
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
      items: group.items.map((item, itemIndex) =>
        buildQaRegulationSaveItem(item, itemIndex + 1, settings)
      )
    }))
}

const buildQaRegulationSavePayload = (
  settings: { publishing?: boolean } = {}
): QaInspectionRegulationSaveReqVO => {
  const finalRule = qaInspectionTypeRules.find((rule) => rule.key === 'FINAL')
  const finalInspectionApplicable = Boolean(finalRule?.required)
  const finalInspectionNotApplicableReason = finalInspectionApplicable
    ? undefined
    : resolveRequiredText(finalRule?.notApplicableReason, '末检不适用依据')
  return {
    regulationId: qaRegulationDraft.regulationId,
    dccProjectCodeId: resolvePositiveId(
      qaRegulationDraft.dccProjectCodeId,
      'DCC 项目代码 ID'
    ),
    regulationCode: resolveRequiredText(qaRegulationDraft.regulationCode, '规程编号'),
    regulationName: resolveRequiredText(qaRegulationDraft.regulationName, '规程名称'),
    versionNo: resolveRequiredText(qaRegulationDraft.versionNo, '规程版本'),
    effectiveDate: qaRegulationDraft.effectiveDate || undefined,
    finalInspectionApplicable,
    finalInspectionNotApplicableReason,
    inspectionTypeRules: qaInspectionTypeRules.map((rule) => ({ ...rule })),
    processes: buildQaRegulationProcesses(settings)
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
    passed: qaRegulationItems.value.length > 0 &&
      qaRegulationItems.value.every((item) =>
        Boolean(item.processCode.trim() && item.processName.trim() && item.itemCode.trim() && item.itemName.trim())
      ),
    detail: 'QA 工序编码、名称及检验项目必须完整'
  },
  {
    label: '抽样方案',
    passed: qaRegulationItems.value.length > 0 &&
      qaRegulationItems.value.every((item) =>
        isQaInspectionSamplingPlanComplete(item.samplingPlanText)
      ),
    detail: '每个检验项目必须包含有效首检数量和巡检 AQL 规则'
  }
])

const qaPublishBlockers = computed(() =>
  qaRegulationPublishChecks.value.filter((check) => !check.passed)
)

const pagedQaRegulationCompletenessChecks = computed(() =>
  paginateQaRows(qaRegulationPublishChecks.value, qaChecksQuery)
)

const qaPqcTaskPreviewRows = computed(() =>
  qaInspectionTypeRules
    .filter((rule) => rule.required)
    .map((rule) => ({
      inspectionTypeText: rule.label,
      roundText: rule.roundLabel,
      plannedQuantityText: rule.fixedQuantity ? String(rule.fixedQuantity) : '按抽样方案',
      regulationVersionNo: qaRegulationDraft.versionNo || '--',
      taskIdentity:
        (selectedDccProjectCode.value?.projectCode || '--') +
        ' / QA工序 / ' + rule.key
    }))
)

const pagedQaPqcTaskPreviewRows = computed(() =>
  paginateQaRows(qaPqcTaskPreviewRows.value, qaPqcPreviewQuery)
)

watch(
  () => qaRegulationItems.value.length,
  (total) => keepQaLocalPageInRange(qaItemsQuery, total)
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
    qaRegulationDraft.lifecycleStatus = result.lifecycleStatus
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
    payload = buildQaRegulationSavePayload({ publishing: true })
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
  gap: 16px;
  margin-bottom: 0;
}

.qa-regulation-page__title {
  flex-shrink: 0;
  color: #172033;
  font-size: 20px;
  font-weight: 700;
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
  flex-shrink: 0;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.qa-regulation-page__header :deep(.el-tag) {
  flex-shrink: 0;
  margin-left: auto;
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
  margin-top: 12px;
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

.qa-regulation-page__equipment-editor {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.qa-regulation-page__equipment-option {
  display: grid;
  grid-template-columns: 88px minmax(96px, 1fr);
  gap: 6px;
  align-items: center;
  padding: 8px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.qa-regulation-page__equipment-id {
  width: 100%;
}

.qa-regulation-page__applicable-types {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.qa-regulation-page__sampling-plan {
  color: #172033;
  font-size: 12px;
  line-height: 1.55;
  white-space: normal;
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
