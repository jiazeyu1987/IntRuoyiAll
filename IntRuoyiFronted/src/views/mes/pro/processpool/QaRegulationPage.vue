<template>
  <div class="qa-regulation-page" data-qa-regulation-page>
    <ContentWrap>
      <div class="qa-regulation-page__header">
        <div>
          <div class="qa-regulation-page__title">QA 规程配置</div>
          <div class="qa-regulation-page__subtitle">
            QA 按 DCC 项目代码维护产品规程，制定 PQC 的首检、巡检、末检和检验项目规则。
          </div>
        </div>
        <el-tag type="warning" effect="plain">{{ qaRegulationDraft.lifecycleStatus }}</el-tag>
      </div>
      <el-alert
        title="正式保存/发布接口已接入；发布时由后端校验首检、巡检、末检、项目字段和不可变版本。"
        type="success"
        :closable="false"
        show-icon
        data-qa-regulation-api-ready
      />
    </ContentWrap>

    <ContentWrap data-qa-regulation-dcc-project>
      <el-form label-width="112px" class="qa-regulation-page__form">
        <el-form-item label="DCC 项目代码" required>
          <el-select
            v-model="qaRegulationDraft.dccProjectCodeId"
            clearable
            filterable
            remote
            reserve-keyword
            :loading="dccProjectCodeOptionsLoading"
            :remote-method="loadDccProjectCodeOptions"
            placeholder="请选择 DCC 项目代码"
            class="!w-100%"
            @change="handleDccProjectCodeChange"
            @visible-change="handleDccProjectCodeVisibleChange"
          >
            <el-option
              v-for="project in dccProjectCodeOptions"
              :key="project.id"
              :label="formatDccProjectCodeOption(project)"
              :value="project.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

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
    <ContentWrap>
      <el-tabs v-model="qaActiveTab" data-qa-regulation-tabs>
        <el-tab-pane label="总览" name="overview" />
        <el-tab-pane label="检验规则" name="rules" />
        <el-tab-pane label="检验项目" name="items" />
        <el-tab-pane label="发布检查" name="verification" />
      </el-tabs>
    </ContentWrap>

    <ContentWrap v-show="selectedDccProjectCode && qaActiveTab === 'overview'">
      <el-card shadow="never" data-qa-regulation-scope>
          <template #header>适用范围</template>
          <el-form :model="qaRegulationDraft" label-width="112px" class="qa-regulation-page__form">
            <el-form-item label="规程编号">
              <el-input v-model="qaRegulationDraft.regulationCode" />
            </el-form-item>
            <el-form-item label="规程名称">
              <el-input v-model="qaRegulationDraft.regulationName" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :xs="24" :md="12">
                <el-form-item label="版本">
                  <el-input v-model="qaRegulationDraft.versionNo" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="生效日期">
                  <el-date-picker
                    v-model="qaRegulationDraft.effectiveDate"
                    value-format="YYYY-MM-DD"
                    type="date"
                    class="!w-100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="产品">
              <el-input
                v-model="qaRegulationDraft.productName"
                disabled
                placeholder="选择 DCC 项目代码后自动带出"
              />
            </el-form-item>
          </el-form>
          <div
            v-loading="qaRouteScopeLoading"
            class="qa-regulation-page__route-scope"
            data-qa-regulation-route-scope-auto
          >
            <el-alert
              class="mb-12px"
              title="工艺路线来源"
              description="优先读取产品当前绑定的工艺路线；如产品未绑定或需修正，可在此显式选择工艺路线并绑定到产品。路线版本、质检工序、SOP/工艺要求和正式批记录表单仍由路线自动带出。"
              type="info"
              :closable="false"
              show-icon
            />
            <div class="qa-regulation-page__manual-route-bind" data-qa-regulation-manual-route-bind>
              <el-form label-width="112px" class="qa-regulation-page__form">
                <el-row :gutter="12">
                  <el-col :xs="24" :md="18">
                    <el-form-item label="工艺路线">
                      <el-select
                        v-model="manualQaRouteBinding.routeId"
                        clearable
                        filterable
                        :loading="manualQaRouteOptionsLoading"
                        placeholder="选择要绑定到当前产品的已发布工艺路线"
                        class="!w-100%"
                        @visible-change="handleManualQaRouteVisibleChange"
                      >
                        <el-option
                          v-for="route in manualQaRouteOptions"
                          :key="route.id"
                          :label="formatManualQaRouteOption(route)"
                          :value="route.id"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :md="6">
                    <el-button
                      type="primary"
                      plain
                      class="qa-regulation-page__manual-route-button"
                      :loading="manualQaRouteBindingSaving"
                      @click="handleManualQaRouteBind"
                    >
                      手动绑定工艺路线
                    </el-button>
                  </el-col>
                </el-row>
              </el-form>
              <el-alert
                v-if="manualQaRouteLoadError"
                class="mb-12px"
                :title="manualQaRouteLoadError"
                type="error"
                :closable="false"
                show-icon
                data-qa-regulation-manual-route-error
              />
            </div>
            <el-alert
              v-if="qaRouteScopeLoadError"
              :title="qaRouteScopeLoadError"
              type="error"
              :closable="false"
              show-icon
              data-qa-regulation-route-scope-error
            />
            <div
              v-else
              class="qa-regulation-page__scope-grid"
              data-qa-regulation-route-scope-summary
            >
              <div
                v-for="row in qaRouteScopeRows"
                :key="row.key"
                class="qa-regulation-page__scope-row"
              >
                <span class="qa-regulation-page__scope-label">{{ row.label }}</span>
                <span class="qa-regulation-page__scope-value">{{ row.value || '--' }}</span>
              </div>
            </div>
          </div>
      </el-card>
    </ContentWrap>

    <ContentWrap v-show="qaActiveTab === 'rules'">
      <el-card shadow="never" data-qa-regulation-inspection-rules>
        <template #header>
          <div class="qa-regulation-page__card-head">
            <span>检验类型规则</span>
            <div class="qa-regulation-page__rule-tags" data-qa-regulation-rule-types>
              <el-tag effect="plain">首检</el-tag>
              <el-tag effect="plain">上午巡检</el-tag>
              <el-tag effect="plain">下午巡检</el-tag>
              <el-tag effect="plain">末检</el-tag>
            </div>
          </div>
        </template>
        <UnifiedListTemplate
          table-key="mes.qa.regulation.rules"
          :query-model="qaRulesQuery"
          :filter-definitions="qaEmptyFilterDefinitions"
          :show-quick-filter="false"
          :quick-filter-state="qaEmptyQuickFilterState"
          :selected-filter-definition="qaEmptySelectedFilterDefinition"
          :operator-options="qaEmptyOperatorOptions"
          :columns="qaRulesColumns"
          :column-saving="qaRulesColumnSaving"
          :total="qaInspectionTypeRules.length"
          v-model:page="qaRulesQuery.pageNo"
          v-model:limit="qaRulesQuery.pageSize"
          @column-change="saveQaRulesColumnConfig"
          @column-reset="resetQaRulesColumnConfig"
        >
          <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
            <el-table
              :data="pagedQaInspectionTypeRules"
              border
              size="small"
              data-user-table-column-explicit
              data-user-table-key="mes.qa.regulation.rules"
              @header-dragend="handleQaRulesHeaderDragend"
              @sort-change="handleTemplateSortChange"
            >
              <el-table-column
                v-if="isQaRulesColumnVisible('rule')"
                label="规则"
                prop="rule"
                :min-width="getQaRulesColumnMinWidthString('rule', 120)"
              v-bind="sortColumnAttrs('rule')"
              >
                <template #default="{ row }">
                  <div class="qa-regulation-page__rule-name">{{ row.label }}</div>
                  <div class="qa-regulation-page__hint">{{ row.roundLabel }}</div>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isQaRulesColumnVisible('required')"
                label="是否适用"
                prop="required"
                :width="getQaRulesColumnWidthString('required', 110)"
              v-bind="sortColumnAttrs('required')"
              >
                <template #default="{ row }">
                  <el-switch v-model="row.required" />
                </template>
              </el-table-column>
              <el-table-column
                v-if="isQaRulesColumnVisible('notApplicableReason')"
                label="不适用依据"
                prop="notApplicableReason"
                :min-width="getQaRulesColumnMinWidthString('notApplicableReason', 260)"
              v-bind="sortColumnAttrs('notApplicableReason')"
              >
                <template #default="{ row }">
                  <el-input
                    v-if="row.key === 'FINAL' && !row.required"
                    v-model="row.notApplicableReason"
                    placeholder="填写末检不适用的正式依据"
                    clearable
                  />
                  <el-tag v-else effect="plain" type="info">
                    {{ row.key === 'FINAL' ? '末检适用' : '不适用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isQaRulesColumnVisible('fixedQuantity')"
                label="固定数量"
                prop="fixedQuantity"
                :width="getQaRulesColumnWidthString('fixedQuantity', 140)"
              v-bind="sortColumnAttrs('fixedQuantity')"
              >
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.fixedQuantity"
                    :disabled="!row.required || row.sampleRatio !== undefined"
                    :min="0"
                    :controls="false"
                    class="!w-100%"
                  />
                </template>
              </el-table-column>
              <el-table-column
                v-if="isQaRulesColumnVisible('sampleRatio')"
                label="抽样比例"
                prop="sampleRatio"
                :width="getQaRulesColumnWidthString('sampleRatio', 140)"
              v-bind="sortColumnAttrs('sampleRatio')"
              >
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.sampleRatio"
                    :disabled="!row.required || row.fixedQuantity !== undefined"
                    :min="0"
                    :max="100"
                    :precision="1"
                    :controls="false"
                    class="!w-100%"
                  />
                </template>
              </el-table-column>
              <el-table-column
                v-if="isQaRulesColumnVisible('plannedQuantity')"
                label="PQC 计划数量"
                prop="plannedQuantity"
                :width="getQaRulesColumnWidthString('plannedQuantity', 150)"
              v-bind="sortColumnAttrs('plannedQuantity')"
              >
                <template #default="{ row }">
                  <el-tag effect="plain">{{ formatQaRulePlannedQuantity(row) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isQaRulesColumnVisible('taskRule')"
                label="任务生成规则"
                prop="taskRule"
                :min-width="getQaRulesColumnMinWidthString('taskRule', 240)"
              v-bind="sortColumnAttrs('taskRule')"
              />
              <el-table-column
                v-if="isQaRulesColumnVisible('releaseGate')"
                label="发布门禁"
                prop="releaseGate"
                :min-width="getQaRulesColumnMinWidthString('releaseGate', 240)"
              v-bind="sortColumnAttrs('releaseGate')"
              />
            </el-table>
          </template>
        </UnifiedListTemplate>
        <div class="qa-regulation-page__hint mt-8px">
          巡检预览：当前用 {{ qaRegulationDraft.sampleOrderQuantity }} 件示例数量演示 5% 抽样 =
          {{ Math.ceil(qaRegulationDraft.sampleOrderQuantity * 0.05) }}；正式 PQC 任务按实际工单/批次数量生成。
        </div>
      </el-card>
    </ContentWrap>

    <ContentWrap v-show="qaActiveTab === 'items'">
      <el-card shadow="never" data-qa-regulation-items>
        <template #header>
          <div class="qa-regulation-page__card-head">
            <span>工序检验方法与抽样方案</span>
            <el-button
              type="primary"
              plain
              :disabled="!selectedDccProjectCode"
              @click="addQaRegulationItem"
            >
              新增检验方法
            </el-button>
          </div>
        </template>
        <UnifiedListTemplate
          table-key="mes.qa.regulation.items.processMethods"
          :query-model="qaItemsQuery"
          :filter-definitions="qaEmptyFilterDefinitions"
          :show-quick-filter="false"
          :quick-filter-state="qaEmptyQuickFilterState"
          :selected-filter-definition="qaEmptySelectedFilterDefinition"
          :operator-options="qaEmptyOperatorOptions"
          :columns="qaItemsColumns"
          :column-saving="qaItemsColumnSaving"
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
              data-user-table-key="mes.qa.regulation.items.processMethods"
              @header-dragend="handleQaItemsHeaderDragend"
              @sort-change="handleTemplateSortChange"
            >
              <el-table-column
                v-if="isQaItemsColumnVisible('routeProcessName')"
                label="工序"
                prop="routeProcessName"
                :min-width="getQaItemsColumnMinWidthString('routeProcessName', 170)"
              v-bind="sortColumnAttrs('routeProcessName')"
              >
                <template #default="{ row }">
                  <span class="qa-regulation-page__process-name">
                    {{ formatQaItemProcessName(row) }}
                  </span>
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
                  <el-select v-model="row.applicableTypes" multiple collapse-tags collapse-tags-tooltip>
                    <el-option
                      v-for="option in qaInspectionTypeOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
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
                  <el-input v-model="row.inspectionTool" />
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
          <template #header>发布完整性检查</template>
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
            :total="qaRegulationCompletenessChecks.length"
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
            <el-button
              type="primary"
              :loading="qaRegulationPublishing"
              @click="runQaPublishPrecheck"
            >
              发布规程
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
            title="PQC 任务必须来自 QA 发布规程快照；缺产品、路线、工序、规则或项目时阻塞生成。"
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
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  type TableQuickFilterDefinition,
  type TableQuickFilterOperator
} from '@/hooks/web/useTableQuickFilter'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import {
  QcTemplateApi,
  type QaInspectionRegulationSaveItemVO,
  type QaInspectionRegulationSaveReqVO
} from '@/api/mes/qc/template'
import { ProRouteApi, type ProRouteVO, type ProRouteVersionVO } from '@/api/mes/pro/route'
import {
  ProRouteProductApi,
  type ProRouteProductVO
} from '@/api/mes/pro/route/product'
import {
  ProRouteProcessApi,
  type ProRouteProcessVO
} from '@/api/mes/pro/route/process'
import {
  ProRouteFlowConfigApi,
  type ProRouteFlowProcessConfigVO
} from '@/api/mes/pro/route/flowconfig'

defineOptions({ name: 'MesProProcessPoolQaRegulation' })

type QaInspectionTypeValue = 'FIRST' | 'PATROL_AM' | 'PATROL_PM' | 'FINAL'
type QaInspectionResultType = 'BOOLEAN' | 'NUMERIC' | 'TEXT'

const PRESSURE_PUMP_PROJECT_CODE = 'IDI'
const DCC_PROJECT_CODE_PAGE_SIZE = 50

interface QaInspectionTypeRule {
  key: QaInspectionTypeValue
  inspectionType: 'FIRST' | 'PATROL' | 'FINAL'
  label: string
  roundLabel: string
  required: boolean
  fixedQuantity?: number
  sampleRatio?: number
  notApplicableReason?: string
  taskRule: string
  releaseGate: string
}

interface QaRegulationItem {
  itemCode: string
  processName?: string
  itemName: string
  applicableTypes: QaInspectionTypeValue[]
  inspectionMethod: string
  inspectionTool: string
  samplingPlanText?: string
  resultType: QaInspectionResultType
  standardText: string
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
  dccProjectCodeId?: number
  regulationCode: string
  regulationName: string
  versionNo: string
  effectiveDate: string
  lifecycleStatus: string
  productName: string
  routeId?: number
  routeName: string
  routeVersionId?: number
  routeVersionName: string
  routeProcessId?: number
  processId?: number
  routeProcessName: string
  sopName: string
  productionFactor: number
  sampleOrderQuantity: number
  batchRecordBinding: string
}

type QaRegulationTabName = 'overview' | 'rules' | 'items' | 'verification'

interface QaLocalListQuery {
  pageNo: number
  pageSize: number
}

interface QaRouteScopeRow {
  key: string
  label: string
  value: string
}

interface QaRouteScopeAutoSource {
  routeProduct?: ProRouteProductVO
  route: ProRouteVO
  routeVersion: ProRouteVersionVO
  routeProcess: ProRouteProcessVO
  scheduleConfig?: ProRouteFlowProcessConfigVO
  batchConfig?: ProRouteFlowProcessConfigVO
}

interface QaRouteScopeBindingSource {
  routeId: number
  routeVersionId?: number
  routeProduct?: ProRouteProductVO
}

const qaActiveTab = ref<QaRegulationTabName>('overview')
const qaRulesQuery = reactive<QaLocalListQuery>({ pageNo: 1, pageSize: 10 })
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
  const start = (pageNo - 1) * pageSize
  return rows.slice(start, start + pageSize)
}

const keepQaLocalPageInRange = (query: QaLocalListQuery, total: number) => {
  const pageSize = Math.max(1, Number(query.pageSize) || 10)
  const maxPage = Math.max(1, Math.ceil(total / pageSize))
  if (query.pageNo > maxPage) {
    query.pageNo = maxPage
  }
}

const qaRulesDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'rule', label: '规则', minWidth: 120 },
  { key: 'required', label: '是否适用', width: 110 },
  { key: 'notApplicableReason', label: '不适用依据', minWidth: 260 },
  { key: 'fixedQuantity', label: '固定数量', width: 140 },
  { key: 'sampleRatio', label: '抽样比例', width: 140 },
  { key: 'plannedQuantity', label: 'PQC 计划数量', width: 150 },
  { key: 'taskRule', label: '任务生成规则', minWidth: 240 },
  { key: 'releaseGate', label: '发布门禁', minWidth: 240 }
]

const qaItemsDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'routeProcessName', label: '工序', minWidth: 170 },
  { key: 'itemName', label: '检验项目', minWidth: 170 },
  { key: 'standardText', label: '接受标准', minWidth: 280 },
  { key: 'inspectionMethod', label: '检验方法', minWidth: 240 },
  { key: 'inspectionTool', label: '检验器具及设备', minWidth: 170 },
  { key: 'samplingPlan', label: '抽样方案', minWidth: 240, sortable: false },
  { key: 'itemCode', label: '检验项目编码', width: 130, visible: false },
  { key: 'applicableTypes', label: '适用检验类型', minWidth: 210, visible: false },
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
  columns: qaRulesColumns,
  saving: qaRulesColumnSaving,
  isColumnVisible: isQaRulesColumnVisible,
  getColumnWidthString: getQaRulesColumnWidthString,
  getColumnMinWidthString: getQaRulesColumnMinWidthString,
  handleHeaderDragend: handleQaRulesHeaderDragend,
  saveConfig: saveQaRulesColumnConfig,
  resetConfig: resetQaRulesColumnConfig
} = useUserTableColumns('mes.qa.regulation.rules', qaRulesDefaultColumns)

const {
  columns: qaItemsColumns,
  saving: qaItemsColumnSaving,
  isColumnVisible: isQaItemsColumnVisible,
  getColumnWidthString: getQaItemsColumnWidthString,
  getColumnMinWidthString: getQaItemsColumnMinWidthString,
  handleHeaderDragend: handleQaItemsHeaderDragend,
  saveConfig: saveQaItemsColumnConfig,
  resetConfig: resetQaItemsColumnConfig
} = useUserTableColumns('mes.qa.regulation.items.processMethods', qaItemsDefaultColumns)

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

const qaInspectionTypeOptions: Array<{ label: string; value: QaInspectionTypeValue }> = [
  { label: '首检', value: 'FIRST' },
  { label: '上午巡检', value: 'PATROL_AM' },
  { label: '下午巡检', value: 'PATROL_PM' },
  { label: '末检', value: 'FINAL' }
]

const createEmptyQaRegulationDraft = (): QaRegulationDraft => ({
  dccProjectCodeId: undefined,
  regulationCode: '',
  regulationName: '',
  versionNo: '',
  effectiveDate: '',
  lifecycleStatus: 'DRAFT',
  productName: '',
  routeId: undefined,
  routeName: '',
  routeVersionId: undefined,
  routeVersionName: '',
  routeProcessId: undefined,
  processId: undefined,
  routeProcessName: '',
  sopName: '',
  productionFactor: 1,
  sampleOrderQuantity: 301,
  batchRecordBinding: ''
})

const createPressurePumpQaRegulationDraft = (): QaRegulationDraft => ({
  ...createEmptyQaRegulationDraft(),
  regulationCode: 'PQC-IDI-001',
  regulationName: '按压式球囊扩充压力泵组装过程检验规程',
  versionNo: 'B/0',
  effectiveDate: '2026-01-04'
})

const qaRegulationDraft = reactive<QaRegulationDraft>(createEmptyQaRegulationDraft())

const qaInspectionTypeRules = reactive<QaInspectionTypeRule[]>([
  {
    key: 'FIRST',
    inspectionType: 'FIRST',
    label: '首检',
    roundLabel: '每个适用订单工序开始前',
    required: true,
    fixedQuantity: 5,
    taskRule: '按发布规程固定数量生成首检任务',
    releaseGate: '缺固定数量或项目时不能发布'
  },
  {
    key: 'PATROL_AM',
    inspectionType: 'PATROL',
    label: '上午巡检',
    roundLabel: '上午班次独立轮次',
    required: true,
    sampleRatio: 5,
    taskRule: '按订单数量 × 上午比例向上取整',
    releaseGate: '上午比例需独立配置'
  },
  {
    key: 'PATROL_PM',
    inspectionType: 'PATROL',
    label: '下午巡检',
    roundLabel: '下午班次独立轮次',
    required: true,
    sampleRatio: 5,
    taskRule: '按订单数量 × 下午比例向上取整',
    releaseGate: '下午比例需独立配置'
  },
  {
    key: 'FINAL',
    inspectionType: 'FINAL',
    label: '末检',
    roundLabel: '订单工序结束前',
    required: true,
    fixedQuantity: 3,
    notApplicableReason: '',
    taskRule: '需要末检时生成末检任务；不适用必须显式关闭',
    releaseGate: '需要/不适用必须明确保存'
  }
])

const pagedQaInspectionTypeRules = computed(() =>
  paginateQaRows(qaInspectionTypeRules, qaRulesQuery)
)

const createPressurePumpQaRegulationItems = (): QaRegulationItem[] => [
  {
    itemCode: 'PP-001-WASH-APP',
    processName: '清洗',
    itemName: '外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '活塞环、弹簧、杠杆、螺纹块、杠杆架、按钮、活塞清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 3,
    sourceOriginalItem: '清洗 / 外观',
    sourceOriginalExcerpt: '活塞环、弹簧、杠杆、螺纹块、杠杆架、按钮、活塞清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-002-CLEAN-APP',
    processName: '清洁',
    itemName: '外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '用清洁、无尘布，蘸取 75% 酒精擦拭产品表面。正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '压力表、外套、后盖、螺杆清洁后表面应清洁，无黑点、无浮尘、无异物等。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 3,
    sourceOriginalItem: '清洁 / 外观',
    sourceOriginalExcerpt: '压力表、外套、后盖、螺杆清洁后表面应清洁，无黑点、无浮尘、无异物等。',
    sourceOriginalMethod: '用清洁、无尘布，蘸取 75% 酒精擦拭产品表面。正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-003-SCREW-APP',
    processName: '组装螺杆八组件',
    itemName: '外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '1）硅化后杠杆架表面应无多余硅油；2）组装杠杆架后组件表面应清洁、无黑点、异物、无划伤、无注塑缺陷；3）组装后芯杆应无多余毛屑；4）硅化后螺杆表面应无成滴的硅油汇聚；5）组装螺杆后组件表面应清洁、无黑点、异物、无划伤、无注塑缺陷；表面无成滴的硅油。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 3,
    sourceOriginalItem: '组装螺杆八组件 / 外观',
    sourceOriginalExcerpt: '1）硅化后杠杆架表面应无多余硅油；2）组装杠杆架后组件表面应清洁、无黑点、异物、无划伤、无注塑缺陷；3）组装后芯杆应无多余毛屑；4）硅化后螺杆表面应无成滴的硅油汇聚；5）组装螺杆后组件表面应清洁、无黑点、异物、无划伤、无注塑缺陷；表面无成滴的硅油。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-004-SCREW-NOJUMP',
    processName: '组装螺杆八组件',
    itemName: '无跳压',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '将推杆装到检测专用泵筒（吸入 10ML 水）上，将压力打至 20atm/30atm/40atm 应无跳压现象。',
    inspectionTool: '检测专用泵筒',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '20atm 压力打至 20atm 应无跳压现象；30atm 压力打至 30atm 应无跳压现象；40atm 压力泵需打压至 40atm 无跳压现象。',
    critical: true,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 3,
    sourceOriginalItem: '组装螺杆八组件 / 无跳压',
    sourceOriginalExcerpt: '20atm 压力打至 20atm 应无跳压现象；30atm 压力打至 30atm 应无跳压现象；40atm 压力泵需打压至 40atm 无跳压现象。',
    sourceOriginalMethod: '将推杆装到检测专用泵筒（吸入 10ML 水）上，将压力打至 20atm/30atm/40atm 应无跳压现象。'
  },
  {
    itemCode: 'PP-005-UV-SWIVEL-APP',
    processName: '光固外套四组件',
    itemName: '光固旋转接头 / 外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '延长管和旋转接头：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 4,
    sourceOriginalItem: '光固外套四组件 / 光固旋转接头 / 外观',
    sourceOriginalExcerpt: '延长管和旋转接头：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-006-UV-SWIVEL-STRENGTH',
    processName: '光固外套四组件',
    itemName: '光固旋转接头 / 牢固度',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    critical: true,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 4,
    sourceOriginalItem: '光固外套四组件 / 光固旋转接头 / 牢固度',
    sourceOriginalExcerpt: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    sourceOriginalMethod: '用 15N 的砝码悬挂，停留 15s。'
  },
  {
    itemCode: 'PP-007-UV-GAUGE-APP',
    processName: '光固外套四组件',
    itemName: '光固压力表 / 外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '外套与压力表：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 4,
    sourceOriginalItem: '光固外套四组件 / 光固压力表 / 外观',
    sourceOriginalExcerpt: '外套与压力表：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-008-UV-GAUGE-STRENGTH',
    processName: '光固外套四组件',
    itemName: '光固压力表 / 牢固度',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    critical: true,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 4,
    sourceOriginalItem: '光固外套四组件 / 光固压力表 / 牢固度',
    sourceOriginalExcerpt: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    sourceOriginalMethod: '用 15N 的砝码悬挂，停留 15s。'
  },
  {
    itemCode: 'PP-009-UV-TUBE-APP',
    processName: '光固外套四组件',
    itemName: '光固延长管 / 外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '延长管与外套：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 4,
    sourceOriginalItem: '光固外套四组件 / 光固延长管 / 外观',
    sourceOriginalExcerpt: '延长管与外套：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-010-UV-TUBE-STRENGTH',
    processName: '光固外套四组件',
    itemName: '光固延长管 / 牢固度',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    critical: true,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 5,
    sourceOriginalItem: '光固外套四组件 / 光固延长管 / 牢固度',
    sourceOriginalExcerpt: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    sourceOriginalMethod: '用 15N 的砝码悬挂，停留 15s。'
  },
  {
    itemCode: 'PP-011-ASSEMBLE-PISTON-APP',
    processName: '装配',
    itemName: '装配活塞 / 外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '1）表面清洁、无黑点、异物、无划伤、无注塑缺陷；2）放置活塞时应注意活塞是否放正，避免压偏侧倒，活塞应卡紧。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 5,
    sourceOriginalItem: '装配 / 装配活塞 / 外观',
    sourceOriginalExcerpt: '1）表面清洁、无黑点、异物、无划伤、无注塑缺陷；2）放置活塞时应注意活塞是否放正，避免压偏侧倒，活塞应卡紧。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-012-SILICONE-RING-APP',
    processName: '装配',
    itemName: '硅化活塞环 / 外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '硅化后活塞环表面无成滴的硅油汇聚。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 5,
    sourceOriginalItem: '装配 / 硅化活塞环 / 外观',
    sourceOriginalExcerpt: '硅化后活塞环表面无成滴的硅油汇聚。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-013-ASSEMBLE-RING-APP',
    processName: '装配',
    itemName: '装配活塞环 / 外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '活塞环表面应干净无异物，边缘无缺损。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 5,
    sourceOriginalItem: '装配 / 装配活塞环 / 外观',
    sourceOriginalExcerpt: '活塞环表面应干净无异物，边缘无缺损。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-014-ASSEMBLE-RING-FIT',
    processName: '装配',
    itemName: '装配活塞环 / 配合',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '目测、手感。',
    inspectionTool: '目测、手感',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '活塞环套进活塞的槽中，套上后活塞环应轻松适度。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 5,
    sourceOriginalItem: '装配 / 装配活塞环 / 配合',
    sourceOriginalExcerpt: '活塞环套进活塞的槽中，套上后活塞环应轻松适度。',
    sourceOriginalMethod: '目测、手感。'
  },
  {
    itemCode: 'PP-015-BOND-SLEEVE-APP',
    processName: '整体粘结',
    itemName: '外套组件与套筒组件装配 / 外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；压力泵内腔无异物、毛丝等活动异物；压力泵外套应有足够的透明度，能清晰地看到基准线；压力泵的第一条刻度线（泵体排空时）应与活塞重合。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 6,
    sourceOriginalItem: '整体粘结 / 外套组件与套筒组件装配 / 外观',
    sourceOriginalExcerpt: '压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；压力泵内腔无异物、毛丝等活动异物；压力泵外套应有足够的透明度，能清晰地看到基准线；压力泵的第一条刻度线（泵体排空时）应与活塞重合。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-016-BOND-SLEEVE-FIT',
    processName: '整体粘结',
    itemName: '外套组件与套筒组件装配 / 配合',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '目测、手感。',
    inspectionTool: '目测、手感',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '1）推杆组件推入外套，后盖与外套的卡槽扣到位，旋转后盖使得后盖与外套的缺口完全一致，不能偏掉；2）旋转螺杆检查扭力不应偏大，按下按钮推拉螺杆看应无干涉及推拉力偏大。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 6,
    sourceOriginalItem: '整体粘结 / 外套组件与套筒组件装配 / 配合',
    sourceOriginalExcerpt: '1）推杆组件推入外套，后盖与外套的卡槽扣到位，旋转后盖使得后盖与外套的缺口完全一致，不能偏掉；2）旋转螺杆检查扭力不应偏大，按下按钮推拉螺杆看应无干涉及推拉力偏大。',
    sourceOriginalMethod: '目测、手感。'
  },
  {
    itemCode: 'PP-017-BOND-AIRTIGHT-APP',
    processName: '整体粘结',
    itemName: '气密性 / 外观',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s，对气密性合格的产品进行观察。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '对气密性检测合格的产品进行外观检查应无黑点、杂质、花纹、划痕、缺损、裂纹等外观缺陷；不应有多余胶水外露。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 6,
    sourceOriginalItem: '整体粘结 / 气密性 / 外观',
    sourceOriginalExcerpt: '对气密性检测合格的产品进行外观检查应无黑点、杂质、花纹、划痕、缺损、裂纹等外观缺陷；不应有多余胶水外露。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s，对气密性合格的产品进行观察。'
  },
  {
    itemCode: 'PP-018-BOND-NO-BLOCK',
    processName: '整体粘结',
    itemName: '无卡阻',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '手感。',
    inspectionTool: '手感',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '将粘接完成 12 小时后按压按钮应无卡死现象，来回抽拉推杆应顺畅无卡阻。',
    critical: true,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 6,
    sourceOriginalItem: '整体粘结 / 无卡阻',
    sourceOriginalExcerpt: '将粘接完成 12 小时后按压按钮应无卡死现象，来回抽拉推杆应顺畅无卡阻。',
    sourceOriginalMethod: '手感。'
  },
  {
    itemCode: 'PP-019-BOND-STRENGTH',
    processName: '整体粘结',
    itemName: '牢固度',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    critical: true,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 6,
    sourceOriginalItem: '整体粘结 / 牢固度',
    sourceOriginalExcerpt: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    sourceOriginalMethod: '用 15N 的砝码悬挂，停留 15s。'
  },
  {
    itemCode: 'PP-020-AIRTIGHT-NEGATIVE',
    processName: '整体粘结',
    itemName: '气密性 / 负压检测',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '将粘接完成 12 小时后的压力泵接上气密性检测工装，抽负压-80±5kpa，观察有无泄漏。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '负压检测：抽负压-80±5kpa，不应有泄漏。',
    critical: true,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 7,
    sourceOriginalItem: '整体粘结 / 气密性 / 负压检测',
    sourceOriginalExcerpt: '负压检测：抽负压-80±5kpa，不应有泄漏。',
    sourceOriginalMethod: '将粘接完成 12 小时后的压力泵接上气密性检测工装，抽负压-80±5kpa，观察有无泄漏。'
  },
  {
    itemCode: 'PP-021-AIRTIGHT-HIGH',
    processName: '整体粘结',
    itemName: '气密性 / 高压检测',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '将负压检测合格的压力泵装到气密性检测工装上，进行检测。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '高压检测：将负压检测合格的压力泵装到气密性检测工装上，通过旋转接头接上气源（其中 20atm 量程：20atm 气源；30atm 量程：30atm 气源；40atm 量程：40atm 气源），打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后 10s 内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零。',
    critical: true,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 7,
    sourceOriginalItem: '整体粘结 / 气密性 / 高压检测',
    sourceOriginalExcerpt: '高压检测：将负压检测合格的压力泵装到气密性检测工装上，通过旋转接头接上气源（其中 20atm 量程：20atm 气源；30atm 量程：30atm 气源；40atm 量程：40atm 气源），打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后 10s 内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零。',
    sourceOriginalMethod: '将负压检测合格的压力泵装到气密性检测工装上，进行检测。'
  },
  {
    itemCode: 'PP-022-AIRTIGHT-LOW',
    processName: '整体粘结',
    itemName: '气密性 / 低压检测',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
    inspectionMethod: '将高压检测合格的压力泵装到气密性检测工装上，进行检测。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '低压检测：将高压检测合格的压力泵装到气密性检测工装上，通过旋转接头接上 8atm 气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到 8atm 现象；撤掉气源后，压力表应可以迅速回零。',
    critical: true,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 7,
    sourceOriginalItem: '整体粘结 / 气密性 / 低压检测',
    sourceOriginalExcerpt: '低压检测：将高压检测合格的压力泵装到气密性检测工装上，通过旋转接头接上 8atm 气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到 8atm 现象；撤掉气源后，压力表应可以迅速回零。',
    sourceOriginalMethod: '将高压检测合格的压力泵装到气密性检测工装上，进行检测。'
  }
]

const qaRegulationItems = ref<QaRegulationItem[]>([])
const pagedQaRegulationItems = computed(() =>
  paginateQaRows(qaRegulationItems.value, qaItemsQuery)
)
const dccProjectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const dccProjectCodeOptionsLoading = ref(false)
const dccProjectCodeLoadError = ref('')
const selectedDccProjectCode = ref<DccProjectCodeRespVO>()
const qaRouteScopeLoading = ref(false)
const qaRouteScopeLoadError = ref('')
const qaRouteScopeAutoSource = ref<QaRouteScopeAutoSource>()
const manualQaRouteOptions = ref<ProRouteVO[]>([])
const manualQaRouteOptionsLoading = ref(false)
const manualQaRouteLoadError = ref('')
const manualQaRouteBinding = reactive<{ routeId?: number }>({ routeId: undefined })
const manualQaRouteBindingSaving = ref(false)
const qaRegulationSaving = ref(false)
const qaRegulationPublishing = ref(false)

const normalizeDccProjectCode = (projectCode: string) => projectCode.trim().toUpperCase()

const resolveDccProjectProductId = (project: DccProjectCodeRespVO) => {
  const productId = Number(project.productMasterId)
  return Number.isFinite(productId) && productId > 0 ? productId : undefined
}

const formatDccProjectCodeOption = (project: DccProjectCodeRespVO) =>
  [project.projectCode, project.projectName, project.docControlNo].filter(Boolean).join(' / ')

const formatManualQaRouteOption = (route: ProRouteVO) =>
  [
    route.code,
    route.name,
    route.activeRouteVersionNo ? `当前版本：${route.activeRouteVersionNo}` : '',
    '可绑定'
  ]
    .filter(Boolean)
    .join(' / ')

const resolveDccProjectCodeErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message.trim()
  }
  return String(error)
}

const normalizeQaRouteScopeText = (value: unknown) => {
  const text = String(value ?? '').trim()
  return text || ''
}

const resolveQaRouteScopePositiveNumber = (value: unknown) => {
  const normalized = Number(value)
  return Number.isFinite(normalized) && normalized > 0 ? normalized : undefined
}

const requireQaRouteScopePositiveNumber = (value: unknown, label: string) => {
  const normalized = resolveQaRouteScopePositiveNumber(value)
  if (!normalized) {
    throw new Error(`${label}缺少正式来源。`)
  }
  return normalized
}

const resetFormalQaRouteScopeFields = () => {
  qaRouteScopeAutoSource.value = undefined
  Object.assign(qaRegulationDraft, {
    routeId: undefined,
    routeName: '',
    routeVersionId: undefined,
    routeVersionName: '',
    routeProcessId: undefined,
    processId: undefined,
    routeProcessName: '',
    sopName: '',
    productionFactor: 1,
    batchRecordBinding: ''
  })
}

const formatQaBatchRecordReportName = (
  report: NonNullable<ProRouteFlowProcessConfigVO['batchRecordReports']>[number]
) =>
  [
    normalizeQaRouteScopeText(report.batchRecordReportName),
    normalizeQaRouteScopeText(report.batchRecordReportCode),
    normalizeQaRouteScopeText(report.batchRecordReportId)
  ].filter(Boolean).join(' / ')

const resolveFormalBatchRecordBindingSummary = (
  batchConfig: ProRouteFlowProcessConfigVO | undefined,
  routeProcess: ProRouteProcessVO
) => {
  const reports = (batchConfig?.batchRecordReports || [])
    .slice()
    .sort((left, right) => Number(left.reportSort || 0) - Number(right.reportSort || 0))
    .map(formatQaBatchRecordReportName)
    .filter(Boolean)
  if (reports.length > 0) {
    return reports.join('、')
  }
  return [
    normalizeQaRouteScopeText(routeProcess.batchRecordReportName),
    normalizeQaRouteScopeText(routeProcess.batchRecordReportCode),
    normalizeQaRouteScopeText(routeProcess.batchRecordReportId)
  ].filter(Boolean).join(' / ')
}

const findQaRouteProcessConfig = (
  configs: ProRouteFlowProcessConfigVO[],
  routeProcessId: number
) => configs.find((config) => Number(config.routeProcessId) === Number(routeProcessId))

const resolveQaRouteProcessFromRoute = (routeProcesses: ProRouteProcessVO[]) => {
  const formalProcesses = routeProcesses.filter(
    (process) =>
      resolveQaRouteScopePositiveNumber(process.id) &&
      resolveQaRouteScopePositiveNumber(process.processId)
  )
  const checkProcesses = formalProcesses.filter((process) => process.checkFlag === true)
  if (checkProcesses.length === 1) {
    return checkProcesses[0]
  }
  if (checkProcesses.length === 0 && formalProcesses.length === 1) {
    return formalProcesses[0]
  }
  if (checkProcesses.length === 0) {
    throw new Error('当前工艺路线未标记唯一质检工序，请先在工艺路线中维护 checkFlag。')
  }
  throw new Error('当前工艺路线存在多个质检工序，请先在工艺路线中明确 QA 规程适用工序。')
}

const applyFormalQaRouteScope = (source: QaRouteScopeAutoSource) => {
  const routeProcessId = requireQaRouteScopePositiveNumber(source.routeProcess.id, '路线工序')
  const processId = requireQaRouteScopePositiveNumber(source.routeProcess.processId, '工序')
  const productionFactor = resolveQaRouteScopePositiveNumber(source.scheduleConfig?.productionQuantityFactor)
  Object.assign(qaRegulationDraft, {
    routeId: requireQaRouteScopePositiveNumber(source.route.id, '工艺路线'),
    routeName: normalizeQaRouteScopeText(source.route.name || source.route.code || source.route.id),
    routeVersionId: requireQaRouteScopePositiveNumber(source.routeVersion.id, '路线版本'),
    routeVersionName: normalizeQaRouteScopeText(source.routeVersion.versionNo || source.routeVersion.id),
    routeProcessId,
    processId,
    routeProcessName: normalizeQaRouteScopeText(
      source.routeProcess.processName || source.routeProcess.processCode || routeProcessId
    ),
    sopName: normalizeQaRouteScopeText(source.routeProcess.processAttention),
    productionFactor: productionFactor || 1,
    batchRecordBinding: resolveFormalBatchRecordBindingSummary(source.batchConfig, source.routeProcess)
  })
  qaRouteScopeAutoSource.value = source
}

let qaRouteScopeLoadSerial = 0

const loadQaRouteScopeFromProject = async (project: DccProjectCodeRespVO) => {
  const loadSerial = ++qaRouteScopeLoadSerial
  qaRouteScopeLoading.value = true
  qaRouteScopeLoadError.value = ''
  resetFormalQaRouteScopeFields()
  try {
    const productId = resolveDccProjectProductId(project)
    if (!productId) {
      throw new Error('当前 DCC 项目代码未绑定 MDM 产品，无法读取产品工艺路线绑定。')
    }
    const routeProduct = (await ProRouteProductApi.getRouteProductByItem(productId)) as ProRouteProductVO | null
    if (!routeProduct?.routeId) {
      throw new Error('当前 MDM 产品未绑定工艺路线，请在下方选择工艺路线并手动绑定。')
    }
    const routeScopeSource = await loadQaRouteScopeFromRouteBinding({
      routeId: routeProduct.routeId,
      routeVersionId: routeProduct.routeVersionId,
      routeProduct
    })
    if (loadSerial !== qaRouteScopeLoadSerial) {
      return
    }
    applyFormalQaRouteScope(routeScopeSource)
  } catch (error) {
    if (loadSerial === qaRouteScopeLoadSerial) {
      resetFormalQaRouteScopeFields()
      qaRouteScopeLoadError.value = `工艺路线范围加载失败：${resolveDccProjectCodeErrorMessage(error)}`
    }
  } finally {
    if (loadSerial === qaRouteScopeLoadSerial) {
      qaRouteScopeLoading.value = false
    }
  }
}

async function loadQaRouteScopeFromRouteBinding(
  bindingSource: QaRouteScopeBindingSource
): Promise<QaRouteScopeAutoSource> {
  const routeId = requireQaRouteScopePositiveNumber(bindingSource.routeId, '工艺路线')
  const route = await ProRouteApi.getRoute(routeId)
  const routeVersionId = bindingSource.routeVersionId || route.activeRouteVersionId
  if (!routeVersionId) {
    throw new Error('当前工艺路线缺少激活版本，请先发布工艺路线版本。')
  }
  const routeVersion = await ProRouteApi.getRouteVersion(routeVersionId)
  const routeProcesses = await ProRouteProcessApi.getRouteProcessListByRoute(routeId)
  const routeProcess = resolveQaRouteProcessFromRoute(routeProcesses)
  const routeProcessId = requireQaRouteScopePositiveNumber(routeProcess.id, '路线工序')
  const [scheduleConfigs, batchConfigs] = await Promise.all([
    ProRouteFlowConfigApi.getProcessConfigList(routeId, 'SCHEDULE', routeVersionId),
    ProRouteFlowConfigApi.getProcessConfigList(routeId, 'BATCH', routeVersionId)
  ])
  return {
    routeProduct: bindingSource.routeProduct,
    route,
    routeVersion,
    routeProcess,
    scheduleConfig: findQaRouteProcessConfig(scheduleConfigs, routeProcessId),
    batchConfig: findQaRouteProcessConfig(batchConfigs, routeProcessId)
  }
}

const loadManualQaRouteOptions = async () => {
  manualQaRouteOptionsLoading.value = true
  manualQaRouteLoadError.value = ''
  try {
    manualQaRouteOptions.value = await ProRouteApi.getRouteSimpleList()
  } catch (error) {
    manualQaRouteOptions.value = []
    manualQaRouteLoadError.value = `工艺路线候选加载失败：${resolveDccProjectCodeErrorMessage(error)}`
  } finally {
    manualQaRouteOptionsLoading.value = false
  }
}

const handleManualQaRouteVisibleChange = (visible: boolean) => {
  if (visible && manualQaRouteOptions.value.length === 0 && !manualQaRouteOptionsLoading.value) {
    void loadManualQaRouteOptions()
  }
}

const handleManualQaRouteBind = async () => {
  if (!selectedDccProjectCode.value) {
    ElMessage.warning('请先选择 DCC 项目代码，再绑定工艺路线。')
    return
  }
  const productId = resolveDccProjectProductId(selectedDccProjectCode.value)
  if (!productId) {
    ElMessage.warning('当前 DCC 项目代码未绑定 MDM 产品，不能绑定工艺路线。')
    return
  }
  const routeId = resolveQaRouteScopePositiveNumber(manualQaRouteBinding.routeId)
  if (!routeId) {
    ElMessage.warning('请选择要绑定到当前产品的工艺路线。')
    return
  }
  const routeOption = manualQaRouteOptions.value.find((route) => Number(route.id) === routeId)
  if (!routeOption) {
    ElMessage.warning('所选工艺路线不在正式产品绑定候选中，请重新加载。')
    return
  }
  const loadSerial = ++qaRouteScopeLoadSerial
  manualQaRouteBindingSaving.value = true
  qaRouteScopeLoading.value = true
  qaRouteScopeLoadError.value = ''
  manualQaRouteLoadError.value = ''
  resetFormalQaRouteScopeFields()
  try {
    await ProRouteProductApi.saveQaRegulationRouteProductByItem({ itemId: productId, routeId })
    const routeProduct = (await ProRouteProductApi.getRouteProductByItem(productId)) as ProRouteProductVO | null
    if (!routeProduct?.routeId) {
      throw new Error('绑定提交后未读取到产品当前工艺路线，请刷新后重试。')
    }
    const routeScopeSource = await loadQaRouteScopeFromRouteBinding({
      routeId: routeProduct.routeId,
      routeVersionId: routeProduct.routeVersionId,
      routeProduct
    })
    if (loadSerial !== qaRouteScopeLoadSerial) {
      return
    }
    applyFormalQaRouteScope(routeScopeSource)
    ElMessage.success('已绑定工艺路线并带出 QA 适用范围。')
  } catch (error) {
    if (loadSerial === qaRouteScopeLoadSerial) {
      resetFormalQaRouteScopeFields()
      qaRouteScopeLoadError.value = `手动绑定工艺路线失败：${resolveDccProjectCodeErrorMessage(error)}`
    }
  } finally {
    if (loadSerial === qaRouteScopeLoadSerial) {
      qaRouteScopeLoading.value = false
    }
    manualQaRouteBindingSaving.value = false
  }
}

const qaFormalRouteScopeReady = computed(
  () =>
    Boolean(
      qaRegulationDraft.routeId &&
        qaRegulationDraft.routeName.trim() &&
        qaRegulationDraft.routeVersionId &&
        qaRegulationDraft.routeVersionName.trim() &&
        qaRegulationDraft.routeProcessId &&
        qaRegulationDraft.processId &&
        qaRegulationDraft.routeProcessName.trim()
    )
)

const qaRouteScopeRows = computed<QaRouteScopeRow[]>(() => [
  {
    key: 'route',
    label: '工艺路线来源',
    value: qaRegulationDraft.routeName
  },
  {
    key: 'version',
    label: '路线版本',
    value: qaRegulationDraft.routeVersionName
  },
  {
    key: 'process',
    label: '质检工序',
    value: qaRegulationDraft.routeProcessName
  },
  {
    key: 'batch-record',
    label: '正式批记录表单',
    value: qaRegulationDraft.batchRecordBinding || '工艺路线未配置正式批记录表单'
  },
  {
    key: 'sop',
    label: 'SOP/工艺要求',
    value: qaRegulationDraft.sopName || '工艺路线未提供 SOP/工艺要求'
  }
])

const loadDccProjectCodeOptions = async (keyword = '') => {
  dccProjectCodeOptionsLoading.value = true
  dccProjectCodeLoadError.value = ''
  try {
    const data = await getProjectCodePage({
      pageNo: 1,
      pageSize: DCC_PROJECT_CODE_PAGE_SIZE,
      status: DCC_PROJECT_CODE_STATUS_ENABLE,
      keyword: keyword.trim() || undefined
    })
    const options = [...data.list]
    const selectedProject = selectedDccProjectCode.value
    if (selectedProject && !options.some((project) => project.id === selectedProject.id)) {
      options.unshift(selectedProject)
    }
    dccProjectCodeOptions.value = options
  } catch (error) {
    dccProjectCodeOptions.value = selectedDccProjectCode.value
      ? [selectedDccProjectCode.value]
      : []
    dccProjectCodeLoadError.value = `DCC 项目代码加载失败：${resolveDccProjectCodeErrorMessage(error)}`
  } finally {
    dccProjectCodeOptionsLoading.value = false
  }
}

const retryLoadDccProjectCodes = () => {
  void loadDccProjectCodeOptions()
}

const handleDccProjectCodeVisibleChange = (visible: boolean) => {
  if (
    visible &&
    dccProjectCodeOptions.value.length === 0 &&
    !dccProjectCodeOptionsLoading.value
  ) {
    void loadDccProjectCodeOptions()
  }
}

const applyDccProjectToQaDraft = (project?: DccProjectCodeRespVO) => {
  selectedDccProjectCode.value = project
  if (!project) {
    qaRouteScopeLoadSerial += 1
    qaRouteScopeLoading.value = false
    qaRouteScopeLoadError.value = ''
    manualQaRouteLoadError.value = ''
    manualQaRouteBinding.routeId = undefined
    resetFormalQaRouteScopeFields()
    Object.assign(qaRegulationDraft, createEmptyQaRegulationDraft())
    qaRegulationItems.value = []
    return
  }

  const projectCode = normalizeDccProjectCode(project.projectCode)
  const draft =
    projectCode === PRESSURE_PUMP_PROJECT_CODE
      ? createPressurePumpQaRegulationDraft()
      : createEmptyQaRegulationDraft()
  manualQaRouteLoadError.value = ''
  manualQaRouteBinding.routeId = undefined
  Object.assign(qaRegulationDraft, draft, {
    dccProjectCodeId: project.id,
    productName: project.projectName.trim()
  })
  qaRegulationItems.value =
    projectCode === PRESSURE_PUMP_PROJECT_CODE ? createPressurePumpQaRegulationItems() : []
  qaItemsQuery.pageNo = 1
  void loadQaRouteScopeFromProject(project)
}

const handleDccProjectCodeChange = (projectId?: number) => {
  if (!projectId) {
    applyDccProjectToQaDraft()
    return
  }
  const project = dccProjectCodeOptions.value.find((item) => item.id === projectId)
  if (!project) {
    dccProjectCodeLoadError.value = '所选 DCC 项目代码不在当前正式候选中，请重新加载。'
    applyDccProjectToQaDraft()
    return
  }
  applyDccProjectToQaDraft(project)
}

onMounted(() => {
  void loadDccProjectCodeOptions()
})

const resolveQaRulePlannedQuantity = (rule: QaInspectionTypeRule) => {
  if (!rule.required) return 0
  if (Number.isFinite(Number(rule.fixedQuantity)) && Number(rule.fixedQuantity) > 0) {
    return Number(rule.fixedQuantity)
  }
  if (Number.isFinite(Number(rule.sampleRatio)) && Number(rule.sampleRatio) > 0) {
    return Math.ceil((qaRegulationDraft.sampleOrderQuantity * Number(rule.sampleRatio)) / 100)
  }
  return 0
}

const formatQaRulePlannedQuantity = (rule: QaInspectionTypeRule) => {
  if (!rule.required) return '不适用'
  const quantity = resolveQaRulePlannedQuantity(rule)
  return quantity > 0 ? `${quantity} 件` : '需补齐'
}

const formatQaItemProcessName = (item: QaRegulationItem) =>
  item.processName?.trim() || qaRegulationDraft.routeProcessName.trim() || '待加载正式工序'

const formatQaItemSamplingPlan = (item: QaRegulationItem) => {
  const pdfSamplingPlan = item.samplingPlanText?.trim()
  if (pdfSamplingPlan) {
    return pdfSamplingPlan
  }

  const rules = item.applicableTypes
    .map((inspectionType) => qaInspectionTypeRules.find((rule) => rule.key === inspectionType))
    .filter((rule): rule is QaInspectionTypeRule => Boolean(rule))

  if (rules.length === 0) {
    return '未选择检验类型'
  }

  return rules
    .map((rule) => {
      if (!rule.required) {
        return `${rule.label}：不适用`
      }
      if (Number.isFinite(Number(rule.fixedQuantity)) && Number(rule.fixedQuantity) > 0) {
        return `${rule.label}：${Number(rule.fixedQuantity)} 件`
      }
      if (Number.isFinite(Number(rule.sampleRatio)) && Number(rule.sampleRatio) > 0) {
        return `${rule.label}：${Number(rule.sampleRatio)}% 抽样，当前示例 ${resolveQaRulePlannedQuantity(rule)} 件`
      }
      return `${rule.label}：需补齐数量或比例`
    })
    .join('；')
}

const qaRegulationCompletenessChecks = computed(() => {
  const dccProjectReady = Boolean(
    qaRegulationDraft.dccProjectCodeId &&
      selectedDccProjectCode.value &&
      selectedDccProjectCode.value.productMasterId &&
      qaRegulationDraft.productName.trim()
  )
  const processScopeReady = qaFormalRouteScopeReady.value
  const versionReady = Boolean(
    qaRegulationDraft.regulationCode.trim() &&
      qaRegulationDraft.regulationName.trim() &&
      qaRegulationDraft.versionNo.trim() &&
      qaRegulationDraft.effectiveDate
  )
  const ruleReady = qaInspectionTypeRules.every(
    (rule) => !rule.required || resolveQaRulePlannedQuantity(rule) > 0
  )
  const finalRule = qaInspectionTypeRules.find((rule) => rule.key === 'FINAL')
  const finalApplicabilityReady = Boolean(
    finalRule?.required || finalRule?.notApplicableReason?.trim()
  )
  const itemReady =
    qaRegulationItems.value.length > 0 &&
    qaRegulationItems.value.every(
      (item) =>
        item.itemCode.trim() &&
        item.itemName.trim() &&
        item.applicableTypes.length > 0 &&
        item.inspectionMethod.trim() &&
        item.inspectionTool.trim() &&
        item.resultType &&
        item.standardText.trim() &&
        item.failureRule.trim()
    )
  const numericLimitReady = qaRegulationItems.value.every(
    (item) =>
      item.resultType !== 'NUMERIC' ||
      (Number.isFinite(Number(item.lowerLimit)) && Number.isFinite(Number(item.upperLimit)))
  )
  const sourceExcerptReady = qaRegulationItems.value.every(
    (item) =>
      Number.isFinite(Number(item.sourceOriginalPage)) &&
      Boolean(item.sourceOriginalItem?.trim()) &&
      Boolean(item.sourceOriginalExcerpt?.trim())
  )
  return [
    {
      key: 'dcc-project',
      label: 'DCC 项目代码与产品',
      passed: dccProjectReady,
      detail: dccProjectReady
        ? `已绑定 ${selectedDccProjectCode.value?.projectCode} / ${qaRegulationDraft.productName}`
        : '请选择已绑定 MDM 产品的正式 DCC 项目代码'
    },
    {
      key: 'scope',
      label: '路线/工序范围',
      passed: processScopeReady,
      detail: processScopeReady
        ? '已从产品当前工艺路线绑定带出路线版本和质检工序'
        : qaRouteScopeLoadError.value || '需先从正式工艺路线绑定带出路线版本和质检工序'
    },
    {
      key: 'version',
      label: '规程版本信息',
      passed: versionReady,
      detail: versionReady ? '编号、名称、版本和生效日期已填写' : '需补齐编号、名称、版本或生效日期'
    },
    {
      key: 'rules',
      label: '首检/巡检/末检规则',
      passed: ruleReady && finalApplicabilityReady,
      detail:
        ruleReady && finalApplicabilityReady
          ? '适用的检验类型均有数量或比例，末检不适用时已有正式依据'
          : '适用检验类型缺少固定数量/抽样比例，或末检不适用依据未填写'
    },
    {
      key: 'items',
      label: '检验项目字段',
      passed: itemReady,
      detail: itemReady ? '项目、方法、工具、标准和失败规则齐全' : '需补齐检验项目、方法、工具、标准或失败规则'
    },
    {
      key: 'numeric-limits',
      label: '数值上下限',
      passed: numericLimitReady,
      detail: numericLimitReady ? '数值类项目已有上下限' : '数值类项目必须填写上下限'
    },
    {
      key: 'source-excerpts',
      label: '原文依据摘录',
      passed: sourceExcerptReady,
      detail: sourceExcerptReady
        ? '每个检验项目均已关联 PDF 页码和相关原文摘录'
        : '每个检验项目都必须补齐对应 PDF 页码、原文项目和相关原文摘录'
    }
  ]
})

const qaPublishBlockers = computed(() =>
  qaRegulationCompletenessChecks.value.filter((check) => !check.passed)
)

const pagedQaRegulationCompletenessChecks = computed(() =>
  paginateQaRows(qaRegulationCompletenessChecks.value, qaChecksQuery)
)

const qaPqcTaskPreviewRows = computed(() =>
  qaInspectionTypeRules.map((rule) => ({
    inspectionTypeText: rule.label.includes('巡检') ? '巡检' : rule.label,
    roundText: rule.roundLabel,
    plannedQuantityText: formatQaRulePlannedQuantity(rule),
    regulationVersionNo: qaRegulationDraft.versionNo || '--',
    taskIdentity: `${selectedDccProjectCode.value?.projectCode || '--'} / ${
      qaRegulationDraft.productName || '--'
    } / ${qaRegulationDraft.routeProcessName || '--'} / ${rule.key}`
  }))
)

const pagedQaPqcTaskPreviewRows = computed(() =>
  paginateQaRows(qaPqcTaskPreviewRows.value, qaPqcPreviewQuery)
)

const addQaRegulationItem = () => {
  if (!selectedDccProjectCode.value) {
    ElMessage.warning('请先选择 DCC 项目代码')
    return
  }
  const nextIndex = qaRegulationItems.value.length + 1
  qaRegulationItems.value.push({
    itemCode: `QA-ITEM-${String(nextIndex).padStart(2, '0')}`,
    processName: qaRegulationDraft.routeProcessName.trim(),
    itemName: '新增检验项目',
    applicableTypes: ['FIRST', 'PATROL_AM', 'PATROL_PM'],
    inspectionMethod: '',
    inspectionTool: '',
    samplingPlanText: '',
    resultType: 'BOOLEAN',
    standardText: '',
    critical: false,
    failureRule: '',
    sourceNote: 'QA 手工新增，发布前需确认',
    sourceOriginalItem: '',
    sourceOriginalExcerpt: '',
    sourceOriginalMethod: ''
  })
}

const removeQaRegulationItem = (index: number) => {
  qaRegulationItems.value.splice(index, 1)
  keepQaLocalPageInRange(qaItemsQuery, qaRegulationItems.value.length)
}

const removeQaRegulationItemByRow = (row: QaRegulationItem) => {
  const index = qaRegulationItems.value.indexOf(row)
  if (index >= 0) {
    removeQaRegulationItem(index)
  }
}

const resolvePositiveId = (value: number | undefined, label: string) => {
  const normalized = Number(value)
  if (!Number.isFinite(normalized) || normalized <= 0) {
    throw new Error(`${label}必须填写正式 ID`)
  }
  return normalized
}

const normalizeQaInspectionType = (
  inspectionType: QaInspectionTypeValue
): QaInspectionRegulationSaveItemVO['inspectionType'] => {
  if (inspectionType === 'PATROL_AM' || inspectionType === 'PATROL_PM') {
    return 'PATROL'
  }
  return inspectionType
}

const resolveRuleForInspectionType = (
  inspectionType: QaInspectionRegulationSaveItemVO['inspectionType']
) => qaInspectionTypeRules.find((rule) => rule.inspectionType === inspectionType && rule.required)

const buildQaRegulationSaveItems = (): QaInspectionRegulationSaveItemVO[] =>
  qaRegulationItems.value.flatMap((item) => {
    const inspectionTypes = Array.from(new Set(item.applicableTypes.map(normalizeQaInspectionType)))
    return inspectionTypes.flatMap((inspectionType) => {
      const rule = resolveRuleForInspectionType(inspectionType)
      if (!rule) {
        return []
      }
      return [{
        inspectionType,
        itemCode: item.itemCode,
        itemName: item.itemName,
        inspectionMethod: item.inspectionMethod,
        standardText: item.standardText,
        standardLowerLimit: item.resultType === 'NUMERIC' ? item.lowerLimit : undefined,
        standardUpperLimit: item.resultType === 'NUMERIC' ? item.upperLimit : undefined,
        equipmentRequired: Boolean(item.inspectionTool.trim()),
        resultType: item.resultType,
        firstInspectionQuantity:
          inspectionType === 'PATROL' ? undefined : rule?.fixedQuantity || undefined,
        patrolInspectionRatio:
          inspectionType === 'PATROL' && rule?.sampleRatio
            ? Number((rule.sampleRatio / 100).toFixed(6))
            : undefined
      }]
    })
  })

const buildQaRegulationSavePayload = (): QaInspectionRegulationSaveReqVO | undefined => {
  if (!selectedDccProjectCode.value) {
    ElMessage.warning('请先选择 DCC 项目代码，再保存 QA 规程草稿。')
    return undefined
  }
  const productId = resolveDccProjectProductId(selectedDccProjectCode.value)
  if (!productId) {
    ElMessage.warning('当前 DCC 项目代码未绑定 MDM 产品，不能保存 QA 规程。')
    return undefined
  }
  if (!qaFormalRouteScopeReady.value) {
    ElMessage.warning(qaRouteScopeLoadError.value || '当前产品未加载到正式工艺路线/工序范围，不能保存 QA 规程。')
    return undefined
  }
  const finalRule = qaInspectionTypeRules.find((rule) => rule.key === 'FINAL')
  const finalInspectionApplicable = Boolean(finalRule?.required)
  const finalInspectionNotApplicableReason =
    finalInspectionApplicable ? undefined : finalRule?.notApplicableReason?.trim()
  if (!finalInspectionApplicable && !finalInspectionNotApplicableReason) {
    ElMessage.warning('末检不适用时必须填写正式依据。')
    return undefined
  }
  try {
    return {
      productId,
      productName: qaRegulationDraft.productName.trim(),
      routeId: resolvePositiveId(qaRegulationDraft.routeId, '路线 ID'),
      routeName: qaRegulationDraft.routeName.trim(),
      routeVersionId: resolvePositiveId(qaRegulationDraft.routeVersionId, '路线版本 ID'),
      routeVersionNo: qaRegulationDraft.routeVersionName.trim(),
      routeProcessId: resolvePositiveId(qaRegulationDraft.routeProcessId, '路线工序 ID'),
      processId: resolvePositiveId(qaRegulationDraft.processId, '工序 ID'),
      routeProcessName: qaRegulationDraft.routeProcessName.trim(),
      batchRecordBindingSummary: qaRegulationDraft.batchRecordBinding.trim() || undefined,
      regulationCode: qaRegulationDraft.regulationCode.trim(),
      regulationName: qaRegulationDraft.regulationName.trim(),
      versionNo: qaRegulationDraft.versionNo.trim(),
      effectiveDate: qaRegulationDraft.effectiveDate || undefined,
      finalInspectionApplicable,
      finalInspectionNotApplicableReason,
      items: buildQaRegulationSaveItems()
    }
  } catch (error) {
    ElMessage.warning(resolveDccProjectCodeErrorMessage(error))
    return undefined
  }
}

const previewQaRegulationDraft = async () => {
  const payload = buildQaRegulationSavePayload()
  if (!payload) {
    return
  }
  qaRegulationSaving.value = true
  try {
    const result = await QcTemplateApi.saveQaRegulationDraft(payload)
    qaRegulationDraft.lifecycleStatus = result.lifecycleStatus
    ElMessage.success(`QA 规程草稿已保存：${result.versionNo}`)
  } catch (error) {
    ElMessage.error(`QA 规程草稿保存失败：${resolveDccProjectCodeErrorMessage(error)}`)
  } finally {
    qaRegulationSaving.value = false
  }
}

const runQaPublishPrecheck = async () => {
  if (!selectedDccProjectCode.value) {
    ElMessage.warning('请先选择 DCC 项目代码，再执行发布前检查。')
    return
  }
  if (qaPublishBlockers.value.length > 0) {
    ElMessage.warning(`发布前仍有 ${qaPublishBlockers.value.length} 项规则需补齐`)
    return
  }
  const payload = buildQaRegulationSavePayload()
  if (!payload) {
    return
  }
  qaRegulationPublishing.value = true
  try {
    const result = await QcTemplateApi.publishQaRegulation(payload)
    qaRegulationDraft.lifecycleStatus = 'PUBLISHED'
    ElMessage.success(`QA 规程已发布为不可变版本：${result.versionNo}`)
  } catch (error) {
    ElMessage.error(`QA 规程发布失败：${resolveDccProjectCodeErrorMessage(error)}`)
  } finally {
    qaRegulationPublishing.value = false
  }
}
</script>

<style scoped>
.qa-regulation-page {
  display: grid;
  gap: 16px;
}

.qa-regulation-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.qa-regulation-page__title {
  color: #172033;
  font-size: 20px;
  font-weight: 700;
}

.qa-regulation-page__subtitle,
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

.qa-regulation-page__load-error {
  display: grid;
  gap: 10px;
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

.qa-regulation-page__card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.qa-regulation-page__rule-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.qa-regulation-page__rule-name {
  color: #172033;
  font-weight: 700;
}

.qa-regulation-page__process-name {
  color: #172033;
  font-weight: 700;
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

@media (max-width: 1180px) {
  .qa-regulation-page__layout {
    grid-template-columns: 1fr;
  }

  .qa-regulation-page__scope-grid {
    grid-template-columns: 1fr;
  }
}

</style>
