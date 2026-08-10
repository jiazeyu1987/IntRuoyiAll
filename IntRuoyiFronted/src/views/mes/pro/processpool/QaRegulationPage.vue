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
            type="primary"
            :loading="qaRegulationPublishing"
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

    <ContentWrap v-show="selectedDccProjectCode && qaActiveTab === 'overview'">
      <el-card shadow="never" data-qa-regulation-scope>
          <template #header>适用范围</template>
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
                label="产品"
                class="qa-regulation-page__basic-field qa-regulation-page__basic-field--full"
              >
                <el-input
                  v-model="qaRegulationDraft.productName"
                  disabled
                  placeholder="选择 DCC 项目代码后自动带出"
                />
              </el-form-item>
            </div>
          </el-form>
          <div
            v-loading="qaRouteScopeLoading"
            class="qa-regulation-page__route-scope"
            data-qa-regulation-route-scope-auto
          >
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
                          :disabled="false"
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

    <ContentWrap v-show="qaActiveTab === 'items'">
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
                新增检验方法
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
            title="发布按钮仅校验 DCC 项目代码、MDM 产品绑定、正式工艺路线和同版本不可变；PQC 任务仍来自发布后的规程快照。"
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
  type QaInspectionRegulationPublishedVersionVO,
  type QaInspectionRegulationProjectStatusVO,
  type QaInspectionRegulationSaveEquipmentOptionVO,
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
import {
  isQaInspectionSamplingPlanComplete,
  parseQaInspectionSamplingPlan,
  resolveQaApplicableInspectionTypes,
  type QaInspectionTypeValue
} from './qaRegulationSampling'

defineOptions({ name: 'MesProProcessPoolQaRegulation' })

type QaInspectionResultType = 'BOOLEAN' | 'NUMERIC' | 'TEXT'

const PRESSURE_PUMP_PROJECT_CODE = 'IDI'
const BALLOON_PRESSURE_PUMP_PROJECT_CODE = 'ID'
const DCC_PROJECT_CODE_PAGE_SIZE = 200
const QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY =
  'int-ruoyi:qa-regulation:last-dcc-project-code-id'
const QA_PDF_ITEM_FAILURE_RULE =
  '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。'
const BALLOON_PRESSURE_PUMP_SOURCE_NOTE =
  '用户指定 PDF PQC-ID-001（G/0）5.1 检验内容。'
const QA_PROCESS_SCOPE_BINDINGS_BY_PROJECT_CODE: Record<string, Record<string, string[]>> = {
  ID: {
    清洗: ['清洗', '清洗/精洗'],
    精洗: ['精洗', '清洗/精洗'],
    清洁: ['清洁'],
    组装Ⅰ: ['组装Ⅰ'],
    光固Ⅰ: ['光固Ⅰ'],
    '组装Ⅱ / 硅化Ⅰ': ['组装Ⅱ', '硅化Ⅰ'],
    检测: ['检测'],
    光固Ⅱ: ['光固Ⅱ']
  },
  IDI: {
    清洗: ['清洗'],
    清洁: ['清洁'],
    组装螺杆八组件: ['组装螺杆八组件'],
    光固外套四组件: ['光固外套四组件'],
    装配: ['装配'],
    整体粘结: ['整体粘结']
  }
}

const QA_UNBOUND_BATCH_RECORD_PROCESS_NAMES_BY_PROJECT_CODE: Record<string, string[]> = {
  IDI: ['组装螺杆八组件', '光固外套四组件', '装配', '整体粘结']
}

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

interface QaRegulationItem {
  itemCode: string
  processName?: string
  itemName: string
  inspectionMethod: string
  inspectionTool: string
  equipmentOptions?: QaRegulationEquipmentOptionDraft[]
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

interface QaRegulationEquipmentOptionDraft {
  equipmentId?: number
  equipmentCode: string
  equipmentName: string
  equipmentNumber: string
  defaultFlag?: boolean
  sort?: number
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
  batchRecordBinding: string
}

interface QaProductRuleDraftSnapshot {
  regulationDraft: Pick<
    QaRegulationDraft,
    | 'regulationCode'
    | 'regulationName'
    | 'versionNo'
    | 'effectiveDate'
    | 'lifecycleStatus'
  >
  inspectionTypeRules: QaInspectionTypeRule[]
  regulationItems: QaRegulationItem[]
}

type QaRegulationTabName = 'overview' | 'items' | 'verification'

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
  routeProcesses: ProRouteProcessVO[]
  scheduleConfig?: ProRouteFlowProcessConfigVO
  scheduleConfigs: ProRouteFlowProcessConfigVO[]
  batchConfig?: ProRouteFlowProcessConfigVO
  batchConfigs: ProRouteFlowProcessConfigVO[]
}

interface QaRouteScopeBindingSource {
  routeId: number
  routeVersionId?: number
  routeProduct?: ProRouteProductVO
}

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

const qaItemsDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'routeProcessName', label: '工序', minWidth: 170 },
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

const QA_INSPECTION_TYPE_LABELS: Record<QaInspectionTypeValue, string> = {
  FIRST: '首检',
  PATROL_AM: '上午巡检',
  PATROL_PM: '下午巡检',
  FINAL: '末检'
}

const resolveQaInspectionTypeLabel = (inspectionType: QaInspectionTypeValue) =>
  QA_INSPECTION_TYPE_LABELS[inspectionType]

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
  batchRecordBinding: ''
})

const createPressurePumpQaRegulationDraft = (): QaRegulationDraft => ({
  ...createEmptyQaRegulationDraft(),
  regulationCode: 'PQC-IDI-001',
  regulationName: '按压式球囊扩充压力泵组装过程检验规程',
  versionNo: 'B/0',
  effectiveDate: '2026-01-04'
})

const createBalloonPressurePumpQaRegulationDraft = (): QaRegulationDraft => ({
  ...createEmptyQaRegulationDraft(),
  regulationCode: 'PQC-ID-001',
  regulationName: '（椎体）球囊扩张压力泵组装过程检验规程',
  versionNo: 'G/0',
  effectiveDate: '2025-09-30'
})

const qaRegulationDraft = reactive<QaRegulationDraft>(createEmptyQaRegulationDraft())
const qaPublishedVersionNo = ref('')

const createEmptyQaInspectionTypeRules = (): QaInspectionTypeRule[] => [
  {
    key: 'FIRST',
    inspectionType: 'FIRST',
    label: '首检',
    roundLabel: '每个适用订单工序开始前',
    required: true,
    taskRule: '按发布规程固定数量生成首检任务',
    releaseGate: '缺固定数量或项目时不能发布'
  },
  {
    key: 'PATROL_AM',
    inspectionType: 'PATROL',
    label: '上午巡检',
    roundLabel: '上午班次独立轮次',
    required: true,
    taskRule: '按订单数量 × 上午比例向上取整',
    releaseGate: '上午比例需独立配置'
  },
  {
    key: 'PATROL_PM',
    inspectionType: 'PATROL',
    label: '下午巡检',
    roundLabel: '下午班次独立轮次',
    required: true,
    taskRule: '按订单数量 × 下午比例向上取整',
    releaseGate: '下午比例需独立配置'
  },
  {
    key: 'FINAL',
    inspectionType: 'FINAL',
    label: '末检',
    roundLabel: '订单工序结束前',
    required: false,
    notApplicableReason: '',
    taskRule: '需要末检时生成末检任务；不适用必须显式关闭',
    releaseGate: '需要/不适用必须明确保存'
  }
]

const createPressurePumpQaInspectionTypeRules = (): QaInspectionTypeRule[] =>
  createEmptyQaInspectionTypeRules().map((rule) => {
    return rule.key === 'FINAL' ? { ...rule, required: true, fixedQuantity: 3 } : rule
  })

const createBalloonPressurePumpQaInspectionTypeRules = (): QaInspectionTypeRule[] =>
  createPressurePumpQaInspectionTypeRules()

const qaInspectionTypeRules = reactive<QaInspectionTypeRule[]>(createEmptyQaInspectionTypeRules())

const normalizeQaInspectionTypeRules = (rules: QaInspectionTypeRule[]): QaInspectionTypeRule[] =>
  rules.map((rule) => {
    if (rule.key === 'FINAL') {
      return { ...rule }
    }
    return { ...rule, required: true }
  })

const replaceQaInspectionTypeRules = (rules: QaInspectionTypeRule[]) => {
  qaInspectionTypeRules.splice(
    0,
    qaInspectionTypeRules.length,
    ...normalizeQaInspectionTypeRules(rules)
  )
}

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

const createPressurePumpQaRegulationItems = (): QaRegulationItem[] => [
  {
    itemCode: 'PP-001-WASH-APP',
    processName: '清洗',
    itemName: '外观',
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
    itemCode: 'PP-015-ASSEMBLE-SLEEVE-APP',
    processName: '装配',
    itemName: '外套组件与套筒组件装配 / 外观',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；压力泵内腔无异物、毛丝等活动异物；压力泵外套应有足够的透明度，能清晰地看到基准线；压力泵的第一条刻度线（泵体排空时）应与活塞重合。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 6,
    sourceOriginalItem: '装配 / 外套组件与套筒组件装配 / 外观',
    sourceOriginalExcerpt: '压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；压力泵内腔无异物、毛丝等活动异物；压力泵外套应有足够的透明度，能清晰地看到基准线；压力泵的第一条刻度线（泵体排空时）应与活塞重合。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'PP-016-ASSEMBLE-SLEEVE-FIT',
    processName: '装配',
    itemName: '外套组件与套筒组件装配 / 配合',
    inspectionMethod: '目测、手感。',
    inspectionTool: '目测、手感',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '1）推杆组件推入外套，后盖与外套的卡槽扣到位，旋转后盖使得后盖与外套的缺口完全一致，不能偏掉；2）旋转螺杆检查扭力不应偏大，按下按钮推拉螺杆看应无干涉及推拉力偏大。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 6,
    sourceOriginalItem: '装配 / 外套组件与套筒组件装配 / 配合',
    sourceOriginalExcerpt: '1）推杆组件推入外套，后盖与外套的卡槽扣到位，旋转后盖使得后盖与外套的缺口完全一致，不能偏掉；2）旋转螺杆检查扭力不应偏大，按下按钮推拉螺杆看应无干涉及推拉力偏大。',
    sourceOriginalMethod: '目测、手感。'
  },
  {
    itemCode: 'PP-017-BOND-AIRTIGHT-APP',
    processName: '整体粘结',
    itemName: '外观',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s，对气密性合格的产品进行观察。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '对气密性检测合格的产品进行外观检查应无黑点、杂质、花纹、划痕、缺损、裂纹等外观缺陷；不应有多余胶水外露。',
    critical: false,
    failureRule: '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
    sourceNote: '用户指定 PDF PQC-IDI-001（B/0）5.1 检验内容。',
    sourceOriginalPage: 6,
    sourceOriginalItem: '整体粘结 / 外观',
    sourceOriginalExcerpt: '对气密性检测合格的产品进行外观检查应无黑点、杂质、花纹、划痕、缺损、裂纹等外观缺陷；不应有多余胶水外露。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s，对气密性合格的产品进行观察。'
  },
  {
    itemCode: 'PP-018-BOND-NO-BLOCK',
    processName: '整体粘结',
    itemName: '无卡阻',
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

const createBalloonPressurePumpQaRegulationItems = (): QaRegulationItem[] => [
  {
    itemCode: 'ID-001-WASH-APP',
    processName: '清洗',
    itemName: '外观',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
    critical: false,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 4,
    sourceOriginalItem: '清洗/精洗 / 外观',
    sourceOriginalExcerpt: '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'ID-001-FINE-WASH-APP',
    processName: '精洗',
    itemName: '外观',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
    critical: false,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 4,
    sourceOriginalItem: '清洗/精洗 / 外观',
    sourceOriginalExcerpt: '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'ID-002-CLEAN-APP',
    processName: '清洁',
    itemName: '外观',
    inspectionMethod: '用清洁、无尘布，蘸取 75% 酒精擦拭产品表面。正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '压力表等清洁后应清洁、无异物、浮尘。',
    critical: false,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 4,
    sourceOriginalItem: '清洁 / 外观',
    sourceOriginalExcerpt: '压力表等清洁后应清洁、无异物、浮尘。',
    sourceOriginalMethod: '用清洁、无尘布，蘸取 75% 酒精擦拭产品表面。正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'ID-003-ASSEMBLY-I-APP',
    processName: '组装Ⅰ',
    itemName: '外观',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '1）表面应清洁、无黑点、异物、无划伤、无注塑缺陷；2）硅化后齿条、螺盖表面应无成滴的多余硅油；3）组装后芯杆应无多余毛屑。',
    critical: false,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 4,
    sourceOriginalItem: '组装Ⅰ / 外观',
    sourceOriginalExcerpt: '1）表面应清洁、无黑点、异物、无划伤、无注塑缺陷；2）硅化后齿条、螺盖表面应无成滴的多余硅油；3）组装后芯杆应无多余毛屑。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'ID-004-ASSEMBLY-I-RELEASE',
    processName: '组装Ⅰ',
    itemName: '撤压',
    inspectionMethod: '将待检推杆与专用套筒（吸入 10ML 检测用纯化水）组装，将压力打至 25atm，放到撤压机（气压：2atm，缸径 20MM）上，观察能否顺利撤压。',
    inspectionTool: '撤压机',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '将压力打至 25atm，放到撤压机（气压：2atm，缸径 20MM）上应能顺利撤压。',
    critical: true,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 4,
    sourceOriginalItem: '组装Ⅰ / 撤压',
    sourceOriginalExcerpt: '将压力打至 25atm，放到撤压机（气压：2atm，缸径 20MM）上应能顺利撤压。',
    sourceOriginalMethod: '将待检推杆与专用套筒（吸入 10ML 检测用纯化水）组装，将压力打至 25atm，放到撤压机（气压：2atm，缸径 20MM）上，观察能否顺利撤压。'
  },
  {
    itemCode: 'ID-005-ASSEMBLY-I-NOJUMP',
    processName: '组装Ⅰ',
    itemName: '无跳压',
    inspectionMethod: '将推杆装到检测专用的泵筒（吸入 10 ml 和 20 ml 水）上，将压力打至 30 atm 应无跳压现象，加压泄压各 5 次；40atm 的压力泵则将推杆装到检测专用的泵筒（吸入 10 ml 和 20 ml 水），压力打至 40 atm 无跳压现象，加压泄压各 5 次。',
    inspectionTool: '/',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=1.0',
    resultType: 'BOOLEAN',
    standardText: '30atm 的压力泵压力打至 30atm 应无跳压现象；40atm 的压力泵则压力打至 40 atm 无跳压现象。',
    critical: true,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 4,
    sourceOriginalItem: '组装Ⅰ / 无跳压',
    sourceOriginalExcerpt: '30atm 的压力泵压力打至 30atm 应无跳压现象；40atm 的压力泵则压力打至 40 atm 无跳压现象。',
    sourceOriginalMethod: '将推杆装到检测专用的泵筒（吸入 10 ml 和 20 ml 水）上，将压力打至 30 atm 应无跳压现象，加压泄压各 5 次；40atm 的压力泵则将推杆装到检测专用的泵筒（吸入 10 ml 和 20 ml 水），压力打至 40 atm 无跳压现象，加压泄压各 5 次。'
  },
  {
    itemCode: 'ID-006-UV-I-SWIVEL-APP',
    processName: '光固Ⅰ',
    itemName: '光固旋转接头 / 外观',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '延长管和旋转接头：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    critical: false,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 5,
    sourceOriginalItem: '光固Ⅰ / 光固旋转接头 / 外观',
    sourceOriginalExcerpt: '延长管和旋转接头：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'ID-007-UV-I-SWIVEL-STRENGTH',
    processName: '光固Ⅰ',
    itemName: '光固旋转接头 / 牢固度',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    critical: true,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 5,
    sourceOriginalItem: '光固Ⅰ / 光固旋转接头 / 牢固度',
    sourceOriginalExcerpt: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    sourceOriginalMethod: '用 15N 的砝码悬挂，停留 15s。'
  },
  {
    itemCode: 'ID-008-UV-I-GAUGE-APP',
    processName: '光固Ⅰ',
    itemName: '光固压力表 / 外观',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '外套与压力表：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    critical: false,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 5,
    sourceOriginalItem: '光固Ⅰ / 光固压力表 / 外观',
    sourceOriginalExcerpt: '外套与压力表：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'ID-009-UV-I-GAUGE-STRENGTH',
    processName: '光固Ⅰ',
    itemName: '光固压力表 / 牢固度',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    critical: true,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 5,
    sourceOriginalItem: '光固Ⅰ / 光固压力表 / 牢固度',
    sourceOriginalExcerpt: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    sourceOriginalMethod: '用 15N 的砝码悬挂，停留 15s。'
  },
  {
    itemCode: 'ID-010-UV-I-GAUGE-TORQUE',
    processName: '光固Ⅰ',
    itemName: '光固压力表 / 扭力值',
    inspectionMethod: '使用 5N·m 扭力扳手对连接处进行测试，无松动情况判定合格。',
    inspectionTool: '扭力扳手',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '压力表固化后扭力值＞5N·m。',
    critical: true,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 5,
    sourceOriginalItem: '光固Ⅰ / 光固压力表 / 扭力值',
    sourceOriginalExcerpt: '压力表固化后扭力值＞5N·m。',
    sourceOriginalMethod: '使用 5N·m 扭力扳手对连接处进行测试，无松动情况判定合格。'
  },
  {
    itemCode: 'ID-011-UV-I-TUBE-APP',
    processName: '光固Ⅰ',
    itemName: '光固延长管 / 外观',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '延长管与外套：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    critical: false,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 6,
    sourceOriginalItem: '光固Ⅰ / 光固延长管 / 外观',
    sourceOriginalExcerpt: '延长管与外套：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'ID-012-UV-I-TUBE-STRENGTH',
    processName: '光固Ⅰ',
    itemName: '光固延长管 / 牢固度',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=1.0',
    resultType: 'BOOLEAN',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    critical: true,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 6,
    sourceOriginalItem: '光固Ⅰ / 光固延长管 / 牢固度',
    sourceOriginalExcerpt: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    sourceOriginalMethod: '用 15N 的砝码悬挂，停留 15s。'
  },
  {
    itemCode: 'ID-013-ASSEMBLY-II-APP',
    processName: '组装Ⅱ / 硅化Ⅰ',
    itemName: '外观',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=1.0',
    resultType: 'BOOLEAN',
    standardText: '组装后产品表面应无黑点、杂质、花纹、划痕等外观缺陷；产品内腔无异物、毛丝等活动异物；配件组装后无挤压形成的多余料丝等现象；胶塞表面应无成滴的硅油汇聚。',
    critical: false,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 6,
    sourceOriginalItem: '组装Ⅱ / 硅化Ⅰ / 外观',
    sourceOriginalExcerpt: '组装后产品表面应无黑点、杂质、花纹、划痕等外观缺陷；产品内腔无异物、毛丝等活动异物；配件组装后无挤压形成的多余料丝等现象；胶塞表面应无成滴的硅油汇聚。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'ID-014-TEST-HIGH-PRESSURE',
    processName: '检测',
    itemName: '高压检测',
    inspectionMethod: '将组装产品装到气密性检测工装上进行检测。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '将整体组装产品装到气密性检测工装上，通过大脚接头接上 30atm（30atm 压力泵）/38atm（40atm 压力泵）气源，打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后 10s 内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零。',
    critical: true,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 6,
    sourceOriginalItem: '检测 / 高压检测',
    sourceOriginalExcerpt: '将整体组装产品装到气密性检测工装上，通过大脚接头接上 30atm（30atm 压力泵）/38atm（40atm 压力泵）气源，打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后 10s 内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零。',
    sourceOriginalMethod: '将组装产品装到气密性检测工装上进行检测。'
  },
  {
    itemCode: 'ID-015-TEST-LOW-PRESSURE',
    processName: '检测',
    itemName: '低压检测',
    inspectionMethod: '将高压检测合格的压力泵装到气密性检测工装上进行检测。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '将高压检测合格的压力泵装到气密性检测工装上，通过大脚接头接上 8atm 气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到 8atm 现象；撤掉气源后，压力表应可以迅速回零。',
    critical: true,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 6,
    sourceOriginalItem: '检测 / 低压检测',
    sourceOriginalExcerpt: '将高压检测合格的压力泵装到气密性检测工装上，通过大脚接头接上 8atm 气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到 8atm 现象；撤掉气源后，压力表应可以迅速回零。',
    sourceOriginalMethod: '将高压检测合格的压力泵装到气密性检测工装上进行检测。'
  },
  {
    itemCode: 'ID-016-UV-II-APP',
    processName: '光固Ⅱ',
    itemName: '外观',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '光固位置应整洁均匀圆滑美观；胶水没有污染到其它地方；压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；不应有多余胶水外露。',
    critical: false,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 7,
    sourceOriginalItem: '光固Ⅱ / 外观',
    sourceOriginalExcerpt: '光固位置应整洁均匀圆滑美观；胶水没有污染到其它地方；压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；不应有多余胶水外露。',
    sourceOriginalMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'
  },
  {
    itemCode: 'ID-017-UV-II-STRENGTH',
    processName: '光固Ⅱ',
    itemName: '牢固度',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    resultType: 'BOOLEAN',
    standardText: '对连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    critical: true,
    failureRule: QA_PDF_ITEM_FAILURE_RULE,
    sourceNote: BALLOON_PRESSURE_PUMP_SOURCE_NOTE,
    sourceOriginalPage: 7,
    sourceOriginalItem: '光固Ⅱ / 牢固度',
    sourceOriginalExcerpt: '对连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    sourceOriginalMethod: '用 15N 的砝码悬挂，停留 15s。'
  }
]

const qaRegulationItems = ref<QaRegulationItem[]>([])
const pagedQaRegulationItems = computed(() =>
  paginateQaRows(qaRegulationItems.value, qaItemsQuery)
)
const qaProductRuleDrafts = new Map<number, QaProductRuleDraftSnapshot>()
const qaRegulationProjectStatusByProductId = ref(new Map<number, QaInspectionRegulationProjectStatusVO>())
const qaCurrentPublishedVersion = ref<QaInspectionRegulationPublishedVersionVO>()
const qaCurrentPublishedVersionLoading = ref(false)
const qaCurrentPublishedVersionLoadError = ref('')
let qaCurrentPublishedVersionLoadSerial = 0
const dccProjectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const dccProjectCodeOptionsLoading = ref(false)
const dccProjectCodeLoadError = ref('')
const selectedDccProjectCode = ref<DccProjectCodeRespVO>()
const activeQaRegulationProductId = ref<number | undefined>()
const pressurePumpProductId = ref<number | undefined>()
const balloonPressurePumpProductId = ref<number | undefined>()
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
const { copy: copyQaProjectSelectionToClipboard } = useClipboard({ legacy: true })

const normalizeDccProjectCode = (projectCode: string) => projectCode.trim().toUpperCase()

const resolveDccProjectProductId = (project: DccProjectCodeRespVO) => {
  const productId = Number(project.productMasterId)
  return Number.isFinite(productId) && productId > 0 ? productId : undefined
}

const resolveDccProjectCodeProductIds = (projects: DccProjectCodeRespVO[]) =>
  Array.from(
    new Set(
      projects
        .map((project) => resolveDccProjectProductId(project))
        .filter((productId): productId is number => Boolean(productId))
    )
  )

const createQaRegulationProjectStatusMap = (
  projectStatuses: QaInspectionRegulationProjectStatusVO[]
) => {
  const statusMap = new Map<number, QaInspectionRegulationProjectStatusVO>()
  projectStatuses.forEach((status) => {
    const productId = Number(status.productId)
    if (Number.isFinite(productId) && productId > 0) {
      statusMap.set(productId, status)
    }
  })
  return statusMap
}

const isDccProjectCodeConfigured = (project: DccProjectCodeRespVO) => {
  const productId = resolveDccProjectProductId(project)
  return Boolean(
    productId &&
      (qaRegulationProjectStatusByProductId.value.get(productId)?.configured === true ||
        hasQaProductRuleDraftConfiguration(resolveQaProductRuleDraftSnapshot(productId, project)))
  )
}

const getDccProjectCodeOptionClass = (project: DccProjectCodeRespVO) => ({
  'qa-regulation-page__project-option': true,
  'qa-regulation-page__project-option--configured': isDccProjectCodeConfigured(project)
})

const sortDccProjectCodeOptionsByQaStatus = (projects: DccProjectCodeRespVO[]) =>
  [...projects].sort((left, right) => {
    const leftConfiguredScore = isDccProjectCodeConfigured(left) ? 1 : 0
    const rightConfiguredScore = isDccProjectCodeConfigured(right) ? 1 : 0
    if (leftConfiguredScore !== rightConfiguredScore) {
      return rightConfiguredScore - leftConfiguredScore
    }
    return 0
  })

const mergeDccProjectCodeOptions = (projects: DccProjectCodeRespVO[]) => {
  const projectById = new Map<number, DccProjectCodeRespVO>()
  projects.forEach((project) => {
    projectById.set(project.id, project)
  })
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
      throw new Error('DCC 项目代码分页总数缺失，无法完整排序已配置 QA 规程候选。')
    }
    if (options.length >= total || data.list.length === 0) {
      break
    }
    pageNo += 1
  }
  return mergeDccProjectCodeOptions(options)
}

const cloneQaRegulationItems = (items: QaRegulationItem[]) =>
  items.map((item) => ({
    ...item,
    equipmentOptions: item.equipmentOptions?.map((option) => ({ ...option }))
  }))

const createQaProductRuleDraftSnapshot = (
  draft: QaRegulationDraft,
  inspectionTypeRules: QaInspectionTypeRule[],
  regulationItems: QaRegulationItem[]
): QaProductRuleDraftSnapshot => ({
  regulationDraft: {
    regulationCode: draft.regulationCode,
    regulationName: draft.regulationName,
    versionNo: draft.versionNo,
    effectiveDate: draft.effectiveDate,
    lifecycleStatus: draft.lifecycleStatus
  },
  inspectionTypeRules: inspectionTypeRules.map((rule) => ({ ...rule })),
  regulationItems: cloneQaRegulationItems(regulationItems)
})

const createDefaultQaProductRuleDraftSnapshot = (
  productId: number,
  project: DccProjectCodeRespVO
) => {
  const projectCode = normalizeDccProjectCode(project.projectCode)
  const isPressurePumpProduct =
    productId === pressurePumpProductId.value || projectCode === PRESSURE_PUMP_PROJECT_CODE
  const isBalloonPressurePumpProduct =
    productId === balloonPressurePumpProductId.value ||
    projectCode === BALLOON_PRESSURE_PUMP_PROJECT_CODE
  if (!isPressurePumpProduct && !isBalloonPressurePumpProduct) {
    return undefined
  }
  return createQaProductRuleDraftSnapshot(
    isBalloonPressurePumpProduct
      ? createBalloonPressurePumpQaRegulationDraft()
      : createPressurePumpQaRegulationDraft(),
    isBalloonPressurePumpProduct
      ? createBalloonPressurePumpQaInspectionTypeRules()
      : createPressurePumpQaInspectionTypeRules(),
    isBalloonPressurePumpProduct
      ? createBalloonPressurePumpQaRegulationItems()
      : createPressurePumpQaRegulationItems()
  )
}

const resolveQaProductRuleDraftSnapshot = (
  productId: number,
  project: DccProjectCodeRespVO
) => qaProductRuleDrafts.get(productId) || createDefaultQaProductRuleDraftSnapshot(productId, project)

const hasQaProductRuleDraftConfiguration = (snapshot: QaProductRuleDraftSnapshot | undefined) =>
  Boolean(snapshot && snapshot.regulationItems.length > 0)

const registerPressurePumpProductBinding = (projects: DccProjectCodeRespVO[]) => {
  const pressurePumpProject = projects.find(
    (project) => normalizeDccProjectCode(project.projectCode) === PRESSURE_PUMP_PROJECT_CODE
  )
  if (pressurePumpProject) {
    const productId = resolveDccProjectProductId(pressurePumpProject)
    pressurePumpProductId.value = productId
  }

  const balloonPressurePumpProject = projects.find(
    (project) =>
      normalizeDccProjectCode(project.projectCode) === BALLOON_PRESSURE_PUMP_PROJECT_CODE
  )
  if (balloonPressurePumpProject) {
    const productId = resolveDccProjectProductId(balloonPressurePumpProject)
    balloonPressurePumpProductId.value = productId
  }
}

const saveCurrentQaProductRuleDraft = () => {
  const productId = activeQaRegulationProductId.value
  if (!productId) {
    return
  }
  qaProductRuleDrafts.set(
    productId,
    createQaProductRuleDraftSnapshot(
      qaRegulationDraft,
      qaInspectionTypeRules,
      qaRegulationItems.value
    )
  )
}

const loadQaProductRuleDraft = (productId: number, project: DccProjectCodeRespVO) => {
  let snapshot = qaProductRuleDrafts.get(productId)
  if (!snapshot) {
    snapshot =
      resolveQaProductRuleDraftSnapshot(productId, project) ||
      createQaProductRuleDraftSnapshot(
        createEmptyQaRegulationDraft(),
        createEmptyQaInspectionTypeRules(),
        []
      )
    qaProductRuleDrafts.set(productId, snapshot)
  }

  Object.assign(qaRegulationDraft, createEmptyQaRegulationDraft(), snapshot.regulationDraft, {
    dccProjectCodeId: project.id,
    productName: project.projectName.trim()
  })
  qaPublishedVersionNo.value =
    snapshot.regulationDraft.lifecycleStatus === 'PUBLISHED'
      ? snapshot.regulationDraft.versionNo.trim()
      : ''
  replaceQaInspectionTypeRules(snapshot.inspectionTypeRules)
  qaRegulationItems.value = cloneQaRegulationItems(snapshot.regulationItems)
  activeQaRegulationProductId.value = productId
}

const formatDccProjectCodeOption = (project: DccProjectCodeRespVO) =>
  [project.projectCode, project.projectName, project.docControlNo].filter(Boolean).join(' / ')

const selectedDccProjectCodeLabel = computed(() =>
  selectedDccProjectCode.value ? formatDccProjectCodeOption(selectedDccProjectCode.value) : ''
)

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

const loadCurrentPublishedQaRegulationVersion = async (project?: DccProjectCodeRespVO) => {
  const loadSerial = ++qaCurrentPublishedVersionLoadSerial
  qaCurrentPublishedVersion.value = undefined
  qaCurrentPublishedVersionLoadError.value = ''
  qaCurrentPublishedVersionLoading.value = false
  if (!project) {
    return
  }

  const productId = resolveDccProjectProductId(project)
  if (!productId) {
    qaCurrentPublishedVersionLoadError.value =
      '当前 DCC 项目未绑定 MDM 产品，无法查询已发布 QA 规程版本。'
    return
  }

  const publishedStatus = qaRegulationProjectStatusByProductId.value.get(productId)
  const currentVersionId = publishedStatus?.currentVersionId
  if (publishedStatus?.lifecycleStatus !== 'PUBLISHED' || !currentVersionId) {
    return
  }

  qaCurrentPublishedVersionLoading.value = true
  try {
    const publishedVersion =
      await QcTemplateApi.getPublishedQaRegulationVersion(currentVersionId)
    if (loadSerial !== qaCurrentPublishedVersionLoadSerial) {
      return
    }
    if (
      publishedVersion.publishedVersionId !== currentVersionId ||
      publishedVersion.productId !== productId
    ) {
      throw new Error('已发布 QA 规程版本与当前项目正式状态不一致。')
    }
    qaCurrentPublishedVersion.value = publishedVersion
    qaPublishedVersionNo.value = publishedVersion.versionNo.trim()
  } catch (error) {
    if (loadSerial === qaCurrentPublishedVersionLoadSerial) {
      qaCurrentPublishedVersionLoadError.value =
        `已发布版本加载失败：${resolveDccProjectCodeErrorMessage(error)}`
    }
  } finally {
    if (loadSerial === qaCurrentPublishedVersionLoadSerial) {
      qaCurrentPublishedVersionLoading.value = false
    }
  }
}

const reportDccProjectSelectionStorageError = (action: string, error: unknown) => {
  const message = `DCC 项目代码上次选择${action}失败：${resolveDccProjectCodeErrorMessage(error)}`
  dccProjectCodeLoadError.value = message
  ElMessage.error(message)
}

const persistLastDccProjectCodeSelection = (project?: DccProjectCodeRespVO) => {
  if (typeof window === 'undefined') {
    return
  }
  try {
    if (project) {
      window.localStorage.setItem(
        QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY,
        String(project.id)
      )
      return
    }
    window.localStorage.removeItem(QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY)
  } catch (error) {
    reportDccProjectSelectionStorageError('保存', error)
  }
}

const readLastDccProjectCodeSelectionId = () => {
  if (typeof window === 'undefined') {
    return undefined
  }
  try {
    const rawProjectId = window.localStorage.getItem(
      QA_REGULATION_LAST_DCC_PROJECT_CODE_ID_STORAGE_KEY
    )
    if (!rawProjectId) {
      return undefined
    }
    const lastProjectId = Number(rawProjectId)
    if (!Number.isFinite(lastProjectId) || lastProjectId <= 0) {
      throw new Error('本地记录的 DCC 项目代码 ID 非法。')
    }
    return lastProjectId
  } catch (error) {
    reportDccProjectSelectionStorageError('读取', error)
    return undefined
  }
}

const copySelectedDccProjectCode = async () => {
  const copyableProjectLabel = selectedDccProjectCodeLabel.value.trim()
  if (!copyableProjectLabel) {
    ElMessage.warning('请先选择 DCC 项目代码再复制。')
    return
  }
  try {
    await copyQaProjectSelectionToClipboard(copyableProjectLabel)
    ElMessage.success('DCC 项目代码已复制')
  } catch (error) {
    ElMessage.error('DCC 项目代码复制失败，请检查浏览器剪贴板权限或浏览器限制。')
    throw error
  }
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

const hasFormalQaBatchRecordBinding = (config: ProRouteFlowProcessConfigVO) =>
  config.enabled === true &&
  (config.batchRecordReports || []).some((report) =>
    Boolean(
      normalizeQaRouteScopeText(report.batchRecordReportId) ||
      normalizeQaRouteScopeText(report.batchRecordReportCode) ||
      normalizeQaRouteScopeText(report.batchRecordReportName)
    )
  )

const hasFormalQaRouteProcessBatchRecordBinding = (process: ProRouteProcessVO) =>
  Boolean(
    normalizeQaRouteScopeText(process.batchRecordReportId) ||
    normalizeQaRouteScopeText(process.batchRecordReportCode) ||
    normalizeQaRouteScopeText(process.batchRecordReportName)
  )

const hasFormalQaKeyRouteProcess = (process: ProRouteProcessVO) => process.keyFlag === true

const normalizeQaProcessBindingName = (value: unknown) =>
  normalizeQaRouteScopeText(value)
    .replace(/\s+/g, '')
    .replace(/III/g, 'Ⅲ')
    .replace(/II/g, 'Ⅱ')
    .replace(/I/g, 'Ⅰ')
    .replace(/工序$/, '')

const resolveQaVersionRouteProcesses = (
  routeId: number,
  currentRouteProcesses: ProRouteProcessVO[],
  scheduleConfigs: ProRouteFlowProcessConfigVO[],
  batchConfigs: ProRouteFlowProcessConfigVO[]
): ProRouteProcessVO[] => {
  const versionConfigByRouteProcessId = new Map<number, ProRouteFlowProcessConfigVO>()
  for (const config of [...scheduleConfigs, ...batchConfigs]) {
    const routeProcessId = requireQaRouteScopePositiveNumber(
      config.routeProcessId,
      '激活路线版本工序'
    )
    const existing = versionConfigByRouteProcessId.get(routeProcessId)
    if (
      existing &&
      (normalizeQaRouteScopeText(existing.processCode) !==
        normalizeQaRouteScopeText(config.processCode) ||
        normalizeQaProcessBindingName(existing.processName) !==
          normalizeQaProcessBindingName(config.processName))
    ) {
      throw new Error(`激活路线版本工序 ${routeProcessId} 在排产与批记录快照中的身份不一致。`)
    }
    versionConfigByRouteProcessId.set(routeProcessId, existing || config)
  }
  if (versionConfigByRouteProcessId.size === 0) {
    throw new Error('激活路线版本缺少正式工序配置快照。')
  }

  return Array.from(versionConfigByRouteProcessId.entries())
    .map(([routeProcessId, config]) => {
      const configCode = normalizeQaRouteScopeText(config.processCode)
      const configName = normalizeQaProcessBindingName(config.processName)
      const identityMatches = currentRouteProcesses.filter((process) => {
        const currentCode = normalizeQaRouteScopeText(process.processCode)
        const currentName = normalizeQaProcessBindingName(process.processName)
        return configCode ? currentCode === configCode : currentName === configName
      })
      const processIds = Array.from(
        new Set(
          identityMatches
            .map((process) => resolveQaRouteScopePositiveNumber(process.processId))
            .filter((processId): processId is number => Boolean(processId))
        )
      )
      if (processIds.length !== 1) {
        throw new Error(
          `激活路线版本工序 ${config.processName || config.processCode || routeProcessId} 无法唯一解析正式工序 ID。`
        )
      }
      const currentProcess = identityMatches.find(
        (process) => Number(process.processId) === processIds[0]
      )
      if (!currentProcess) {
        throw new Error(`激活路线版本工序 ${routeProcessId} 缺少正式工序主数据。`)
      }
      return {
        ...currentProcess,
        id: routeProcessId,
        routeId,
        processId: processIds[0],
        processCode: normalizeQaRouteScopeText(config.processCode || currentProcess.processCode),
        processName: normalizeQaRouteScopeText(config.processName || currentProcess.processName),
        sort: Number(config.sort ?? currentProcess.sort),
        keyFlag: config.keyFlag ?? currentProcess.keyFlag
      }
    })
    .sort((left, right) => Number(left.sort) - Number(right.sort))
}

const resolveQaRouteProcessFromRoute = (
  routeProcesses: ProRouteProcessVO[],
  batchConfigs: ProRouteFlowProcessConfigVO[] = []
) => {
  const formalProcesses = routeProcesses.filter(
    (process) =>
      resolveQaRouteScopePositiveNumber(process.id) &&
      resolveQaRouteScopePositiveNumber(process.processId)
  )
  const checkProcesses = formalProcesses.filter((process) => process.checkFlag === true)
  if (checkProcesses.length === 1) {
    return checkProcesses[0]
  }
  if (checkProcesses.length > 1) {
    throw new Error('当前工艺路线存在多个质检工序，请先在工艺路线中明确 QA 规程适用工序。')
  }
  if (checkProcesses.length === 0 && formalProcesses.length === 1) {
    return formalProcesses[0]
  }
  const batchRecordProcessIds = new Set(
    batchConfigs
      .filter(hasFormalQaBatchRecordBinding)
      .map((config) => resolveQaRouteScopePositiveNumber(config.routeProcessId))
      .filter((routeProcessId): routeProcessId is number => Boolean(routeProcessId))
  )
  const batchRecordProcesses = formalProcesses.filter((process) =>
    batchRecordProcessIds.has(Number(process.id))
  )
  if (batchRecordProcesses.length === 1) {
    return batchRecordProcesses[0]
  }
  if (batchRecordProcesses.length > 1) {
    throw new Error('当前工艺路线存在多个正式批记录绑定工序，请先在工艺路线中维护唯一 checkFlag。')
  }
  const routeProcessBatchRecordProcesses = formalProcesses.filter(
    hasFormalQaRouteProcessBatchRecordBinding
  )
  if (routeProcessBatchRecordProcesses.length === 1) {
    return routeProcessBatchRecordProcesses[0]
  }
  if (routeProcessBatchRecordProcesses.length > 1) {
    throw new Error('当前工艺路线存在多个默认批记录报表工序，请先在工艺路线中维护唯一 checkFlag。')
  }
  const keyRouteProcesses = formalProcesses.filter(hasFormalQaKeyRouteProcess)
  if (keyRouteProcesses.length === 1) {
    return keyRouteProcesses[0]
  }
  if (keyRouteProcesses.length > 1) {
    throw new Error('当前工艺路线存在多个关键工序，无法唯一确定 QA 规程适用工序，请先在工艺路线中维护唯一 checkFlag。')
  }
  throw new Error('当前工艺路线未标记唯一质检工序，请先在工艺路线中维护 checkFlag。')
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
    if (loadSerial !== qaRouteScopeLoadSerial) {
      return
    }
    const boundRouteId = requireQaRouteScopePositiveNumber(routeProduct.routeId, '产品当前绑定工艺路线')
    manualQaRouteBinding.routeId = boundRouteId
    if (manualQaRouteOptions.value.length === 0 && !manualQaRouteOptionsLoading.value) {
      void loadManualQaRouteOptions()
    }
    const routeScopeSource = await loadQaRouteScopeFromRouteBinding({
      routeId: boundRouteId,
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
  const [currentRouteProcesses, scheduleConfigs, batchConfigs] = await Promise.all([
    ProRouteProcessApi.getRouteProcessListByRoute(routeId),
    ProRouteFlowConfigApi.getProcessConfigList(routeId, 'SCHEDULE', routeVersionId),
    ProRouteFlowConfigApi.getProcessConfigList(routeId, 'BATCH', routeVersionId)
  ])
  const routeProcesses = resolveQaVersionRouteProcesses(
    routeId,
    currentRouteProcesses,
    scheduleConfigs,
    batchConfigs
  )
  const routeProcess = resolveQaRouteProcessFromRoute(routeProcesses, batchConfigs)
  const routeProcessId = requireQaRouteScopePositiveNumber(routeProcess.id, '路线工序')
  return {
    routeProduct: bindingSource.routeProduct,
    route,
    routeVersion,
    routeProcess,
    routeProcesses,
    scheduleConfig: findQaRouteProcessConfig(scheduleConfigs, routeProcessId),
    scheduleConfigs,
    batchConfig: findQaRouteProcessConfig(batchConfigs, routeProcessId),
    batchConfigs
  }
}

const loadManualQaRouteOptions = async () => {
  manualQaRouteOptionsLoading.value = true
  manualQaRouteLoadError.value = ''
  try {
    manualQaRouteOptions.value = await ProRouteApi.getRouteItemBindingList()
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
    const boundRouteId = requireQaRouteScopePositiveNumber(routeProduct.routeId, '产品当前绑定工艺路线')
    const routeScopeSource = await loadQaRouteScopeFromRouteBinding({
      routeId: boundRouteId,
      routeVersionId: routeProduct.routeVersionId,
      routeProduct
    })
    if (loadSerial !== qaRouteScopeLoadSerial) {
      return
    }
    manualQaRouteBinding.routeId = boundRouteId
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
        qaRegulationDraft.routeProcessName.trim() &&
        qaRouteScopeAutoSource.value?.routeProcesses.length
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
    const options = await loadCompleteDccProjectCodeOptions(keyword.trim())
    const selectedProject = selectedDccProjectCode.value
    const mergedOptions = mergeDccProjectCodeOptions([
      ...(selectedProject ? [selectedProject] : []),
      ...options
    ])
    const productIds = resolveDccProjectCodeProductIds(mergedOptions)
    const projectStatuses = await QcTemplateApi.getQaRegulationProjectStatuses(productIds)
    qaRegulationProjectStatusByProductId.value = createQaRegulationProjectStatusMap(projectStatuses)
    registerPressurePumpProductBinding(mergedOptions)
    dccProjectCodeOptions.value = sortDccProjectCodeOptionsByQaStatus(mergedOptions)
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

const restoreLastDccProjectCodeSelection = async () => {
  const lastProjectId = readLastDccProjectCodeSelectionId()
  if (!lastProjectId || selectedDccProjectCode.value) {
    return
  }
  try {
    let project = dccProjectCodeOptions.value.find(
      (item) => Number(item.id) === lastProjectId
    )
    if (!project) {
      project = await getProjectCode(lastProjectId)
    }
    if (project.status !== DCC_PROJECT_CODE_STATUS_ENABLE) {
      throw new Error('上次选择的 DCC 项目代码已停用，请重新选择。')
    }
    if (!dccProjectCodeOptions.value.some((item) => Number(item.id) === Number(project.id))) {
      dccProjectCodeOptions.value = [project, ...dccProjectCodeOptions.value]
    }
    const productIds = resolveDccProjectCodeProductIds(dccProjectCodeOptions.value)
    const projectStatuses = await QcTemplateApi.getQaRegulationProjectStatuses(productIds)
    qaRegulationProjectStatusByProductId.value = createQaRegulationProjectStatusMap(projectStatuses)
    dccProjectCodeOptions.value = sortDccProjectCodeOptionsByQaStatus(
      dccProjectCodeOptions.value
    )
    qaRegulationDraft.dccProjectCodeId = project.id
    applyDccProjectToQaDraft(project)
  } catch (error) {
    const message = `DCC 项目代码上次选择恢复失败：${resolveDccProjectCodeErrorMessage(error)}`
    dccProjectCodeLoadError.value = message
    ElMessage.error(message)
  }
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
  saveCurrentQaProductRuleDraft()
  selectedDccProjectCode.value = project
  void loadCurrentPublishedQaRegulationVersion(project)
  persistLastDccProjectCodeSelection(project)
  activeQaRegulationProductId.value = undefined
  if (!project) {
    qaRouteScopeLoadSerial += 1
    qaRouteScopeLoading.value = false
    qaRouteScopeLoadError.value = ''
    manualQaRouteLoadError.value = ''
    manualQaRouteBinding.routeId = undefined
    resetFormalQaRouteScopeFields()
    Object.assign(qaRegulationDraft, createEmptyQaRegulationDraft())
    qaRegulationItems.value = []
    replaceQaInspectionTypeRules(createEmptyQaInspectionTypeRules())
    return
  }

  registerPressurePumpProductBinding([project])
  const productId = resolveDccProjectProductId(project)
  manualQaRouteLoadError.value = ''
  manualQaRouteBinding.routeId = undefined
  if (!productId) {
    Object.assign(qaRegulationDraft, createEmptyQaRegulationDraft(), {
      dccProjectCodeId: project.id,
      productName: project.projectName.trim()
    })
    qaRegulationItems.value = []
    replaceQaInspectionTypeRules(createEmptyQaInspectionTypeRules())
    qaItemsQuery.pageNo = 1
    void loadQaRouteScopeFromProject(project)
    return
  }

  loadQaProductRuleDraft(productId, project)
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

onMounted(async () => {
  await loadDccProjectCodeOptions()
  await restoreLastDccProjectCodeSelection()
})

const resolveQaRulePlannedQuantity = (rule: QaInspectionTypeRule) => {
  if (!rule.required) return 0
  if (Number.isFinite(Number(rule.fixedQuantity)) && Number(rule.fixedQuantity) > 0) {
    return Number(rule.fixedQuantity)
  }
  return 0
}

const formatQaRulePlannedQuantity = (rule: QaInspectionTypeRule) => {
  if (!rule.required) return '不适用'
  if (rule.key === 'FIRST') return '按项目首检数量'
  if (rule.key === 'PATROL_AM' || rule.key === 'PATROL_PM') return '按项目 AQL'
  const quantity = resolveQaRulePlannedQuantity(rule)
  return quantity > 0 ? `${quantity} 件` : '需补齐'
}

const formatQaItemProcessName = (item: QaRegulationItem) =>
  item.processName?.trim() || qaRegulationDraft.routeProcessName.trim() || '待加载正式工序'

const formatQaItemSamplingPlan = (item: QaRegulationItem) => {
  const pdfSamplingPlan = item.samplingPlanText?.trim()
  return pdfSamplingPlan || '未配置抽样方案'
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
    (rule) => rule.key !== 'FINAL' || !rule.required || resolveQaRulePlannedQuantity(rule) > 0
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
        isQaInspectionSamplingPlanComplete(item.samplingPlanText) &&
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
          ? '首检数量和巡检 AQL 取自各检验项目抽样方案，末检设置已明确'
          : '末检数量缺失，或末检不适用依据未填写'
    },
    {
      key: 'items',
      label: '检验项目字段',
      passed: itemReady,
      detail: itemReady
        ? '项目、抽样方案、方法、工具、标准和失败规则齐全'
        : '需补齐检验项目、抽样方案 AQL、方法、工具、标准或失败规则'
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

const qaRegulationPublishChecks = computed(() => {
  const dccProjectSelected = Boolean(selectedDccProjectCode.value)
  const mdmProductBound = Boolean(
    selectedDccProjectCode.value && selectedDccProjectCode.value.productMasterId
  )
  const formalRouteBound = Boolean(qaRegulationDraft.routeId)
  const sameVersionAlreadyPublished =
    qaRegulationDraft.lifecycleStatus === 'PUBLISHED' &&
    qaRegulationDraft.versionNo.trim() === qaPublishedVersionNo.value
  return [
    {
      key: 'dcc-project-code',
      label: 'DCC 项目代码',
      passed: dccProjectSelected,
      detail: dccProjectSelected ? '已选择 DCC 项目代码' : '必须先选择 DCC 项目代码'
    },
    {
      key: 'mdm-product-binding',
      label: 'MDM 产品绑定',
      passed: mdmProductBound,
      detail: mdmProductBound ? 'DCC 项目代码已绑定 MDM 产品' : 'DCC 项目代码必须绑定 MDM 产品'
    },
    {
      key: 'formal-route',
      label: '正式工艺路线',
      passed: formalRouteBound,
      detail: formalRouteBound ? '产品已绑定正式工艺路线' : '产品必须绑定正式工艺路线'
    },
    {
      key: 'immutable-version',
      label: '版本不可变',
      passed: !sameVersionAlreadyPublished,
      detail: sameVersionAlreadyPublished
        ? '同一个规程的同一个版本号已经发布，不能原地修改再发布'
        : '当前版本尚未发布'
    }
  ]
})

const qaPublishBlockers = computed(() =>
  qaRegulationPublishChecks.value.filter((check) => !check.passed)
)

const pagedQaRegulationCompletenessChecks = computed(() =>
  paginateQaRows(qaRegulationPublishChecks.value, qaChecksQuery)
)

const qaApplicableInspectionTypes = computed(() => {
  const inspectionTypes = new Set<QaInspectionTypeValue>(['PATROL_AM', 'PATROL_PM'])
  for (const item of qaRegulationItems.value) {
    for (const inspectionType of resolveQaItemApplicableTypes(item)) {
      inspectionTypes.add(inspectionType)
    }
  }
  const orderedTypes: QaInspectionTypeValue[] = ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL']
  return orderedTypes.filter((inspectionType) => inspectionTypes.has(inspectionType))
})

const qaPqcTaskPreviewRows = computed(() =>
  qaApplicableInspectionTypes.value.map((inspectionType) => {
    const rule = qaInspectionTypeRules.find((item) => item.key === inspectionType)
    if (!rule) {
      throw new Error(`缺少${resolveQaInspectionTypeLabel(inspectionType)}规则配置`)
    }
    return {
      inspectionTypeText: rule.label.includes('巡检') ? '巡检' : rule.label,
      roundText: rule.roundLabel,
      plannedQuantityText: formatQaRulePlannedQuantity(rule),
      regulationVersionNo: qaRegulationDraft.versionNo || '--',
      taskIdentity: `${selectedDccProjectCode.value?.projectCode || '--'} / ${
        qaRegulationDraft.productName || '--'
      } / ${qaRegulationDraft.routeProcessName || '--'} / ${rule.key}`
    }
  })
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
    inspectionMethod: '',
    inspectionTool: '',
    equipmentOptions: [],
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

const getQaRegulationItemEquipmentOptions = (item: QaRegulationItem) => {
  if (!item.equipmentOptions) {
    item.equipmentOptions = []
  }
  return item.equipmentOptions
}

const addQaRegulationEquipmentOption = (item: QaRegulationItem) => {
  const options = getQaRegulationItemEquipmentOptions(item)
  options.push({
    equipmentCode: '',
    equipmentName: '',
    equipmentNumber: '',
    defaultFlag: options.length === 0,
    sort: options.length + 1
  })
}

const removeQaRegulationEquipmentOption = (item: QaRegulationItem, index: number) => {
  getQaRegulationItemEquipmentOptions(item).splice(index, 1)
}

const resolvePositiveId = (value: number | undefined, label: string) => {
  const normalized = Number(value)
  if (!Number.isFinite(normalized) || normalized <= 0) {
    throw new Error(`${label}必须填写正式 ID`)
  }
  return normalized
}

const resolveRequiredText = (value: string | undefined, label: string) => {
  const normalized = value?.trim()
  if (!normalized) {
    throw new Error(`${label}不能为空`)
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

const buildQaRegulationItemEquipmentOptions = (
  item: QaRegulationItem,
  settings: { publishing?: boolean } = {}
): QaInspectionRegulationSaveEquipmentOptionVO[] => {
  const optionRows = getQaRegulationItemEquipmentOptions(item)
  const publishing = Boolean(settings.publishing)
  const publishableOptions = publishing
    ? optionRows.filter(
        (option) =>
          option.equipmentId &&
          option.equipmentCode?.trim() &&
          option.equipmentName?.trim() &&
          option.equipmentNumber?.trim()
      )
    : optionRows
  return publishableOptions.map((option, index) => ({
    equipmentId: resolvePositiveId(option.equipmentId, `${item.itemName}设备 ID`),
    equipmentCode: resolveRequiredText(option.equipmentCode, `${item.itemName}设备编码`),
    equipmentName: resolveRequiredText(option.equipmentName, `${item.itemName}设备名称`),
    equipmentNumber: resolveRequiredText(option.equipmentNumber, `${item.itemName}设备编号`),
    defaultFlag: option.defaultFlag,
    sort: option.sort || index + 1
  }))
}

const buildQaRegulationSaveItems = (
  regulationItems: QaRegulationItem[],
  settings: { publishing?: boolean } = {}
): QaInspectionRegulationSaveItemVO[] =>
  regulationItems.flatMap((item) => {
    const publishing = Boolean(settings.publishing)
    const samplingPlan =
      publishing && !isQaInspectionSamplingPlanComplete(item.samplingPlanText)
        ? undefined
        : parseQaInspectionSamplingPlan(item.samplingPlanText, item.itemName)
    const inspectionTypes = Array.from(
      new Set(
        resolveQaApplicableInspectionTypes(
          item.samplingPlanText,
          finalInspectionRequired.value
        ).map(normalizeQaInspectionType)
      )
    )
    const equipmentOptions = buildQaRegulationItemEquipmentOptions(item, { publishing })
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
        inspectionTool: item.inspectionTool,
        samplingPlanText: item.samplingPlanText,
        standardText: item.standardText,
        standardLowerLimit: item.resultType === 'NUMERIC' ? item.lowerLimit : undefined,
        standardUpperLimit: item.resultType === 'NUMERIC' ? item.upperLimit : undefined,
        equipmentRequired: equipmentOptions.length > 0,
        equipmentOptions,
        resultType: item.resultType,
        firstInspectionQuantity:
          inspectionType === 'FIRST'
            ? samplingPlan?.firstInspectionQuantity
            : inspectionType === 'FINAL'
              ? rule.fixedQuantity || undefined
              : undefined,
        patrolInspectionRatio:
          inspectionType === 'PATROL' ? samplingPlan?.patrolInspectionRatio : undefined
      }]
    })
  })

const resolveQaProcessBindingGroups = (
  processName: string,
  configuredBindings?: string[]
) => {
  if (!configuredBindings || configuredBindings.length === 0) {
    return [[processName]]
  }
  if (configuredBindings[0] === processName) {
    return configuredBindings.map((binding) => [binding])
  }
  return [configuredBindings]
}

const resolveQaRegulationItemRouteProcesses = (
  item: QaRegulationItem,
  source: QaRouteScopeAutoSource,
  settings: { publishing?: boolean } = {}
) => {
  const publishing = Boolean(settings.publishing)
  const processName = resolveRequiredText(item.processName, `${item.itemName}适用工序`)
  const projectCode = normalizeDccProjectCode(selectedDccProjectCode.value?.projectCode || '')
  const configuredBindings = QA_PROCESS_SCOPE_BINDINGS_BY_PROJECT_CODE[projectCode]?.[processName]
  const matchedProcesses =
    resolveQaProcessBindingGroups(processName, configuredBindings)
      .map((group) => {
        const acceptedNormalizedNames = new Set(group.map(normalizeQaProcessBindingName))
        return source.routeProcesses.filter((routeProcess) =>
          acceptedNormalizedNames.has(normalizeQaProcessBindingName(routeProcess.processName))
        )
      })
      .find((matches) => matches.length > 0) || []
  if (matchedProcesses.length === 0) {
    if (publishing) {
      return [{ routeProcess: source.routeProcess, batchRecordBindingResolved: false }]
    }
    const allowedUnboundProcessNames = new Set(
      (QA_UNBOUND_BATCH_RECORD_PROCESS_NAMES_BY_PROJECT_CODE[projectCode] || []).map(
        normalizeQaProcessBindingName
      )
    )
    if (allowedUnboundProcessNames.has(normalizeQaProcessBindingName(processName))) {
      return [{ routeProcess: source.routeProcess, batchRecordBindingResolved: false }]
    }
    throw new Error(
      `${item.itemName}的正式工序“${processName}”未匹配激活路线版本中的任何路线工序。`
    )
  }
  const duplicateNames = matchedProcesses
    .map((routeProcess) => normalizeQaProcessBindingName(routeProcess.processName))
    .filter((name, index, names) => names.indexOf(name) !== index)
  if (duplicateNames.length > 0) {
    if (publishing) {
      return [{ routeProcess: source.routeProcess, batchRecordBindingResolved: false }]
    }
    throw new Error(
      `${item.itemName}的正式工序“${processName}”在激活路线版本中不唯一，必须按路线工序明确配置。`
    )
  }
  return matchedProcesses.map((routeProcess) => ({
    routeProcess,
    batchRecordBindingResolved: true
  }))
}

const buildQaProcessRegulationCode = (baseCode: string, routeProcessId: number) => {
  const processCode = `${resolveRequiredText(baseCode, '规程编号')}-RP${routeProcessId}`
  if (processCode.length > 64) {
    throw new Error(`工序规程编号 ${processCode} 超过 64 个字符，请缩短基础规程编号。`)
  }
  return processCode
}

const buildQaRegulationSavePayloads = (
  settings: { publishing?: boolean } = {}
): QaInspectionRegulationSaveReqVO[] | undefined => {
  const publishing = Boolean(settings.publishing)
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
  const source = qaRouteScopeAutoSource.value
  if (!source) {
    ElMessage.warning('当前产品未加载到激活路线版本工序快照，不能保存 QA 规程。')
    return undefined
  }
  const finalRule = qaInspectionTypeRules.find((rule) => rule.key === 'FINAL')
  const finalInspectionApplicable = Boolean(finalRule?.required)
  const finalInspectionNotApplicableReason =
    finalInspectionApplicable ? undefined : finalRule?.notApplicableReason?.trim()
  if (!publishing && !finalInspectionApplicable && !finalInspectionNotApplicableReason) {
    ElMessage.warning('末检不适用时必须填写正式依据。')
    return undefined
  }
  try {
    const itemsByRouteProcessId = new Map<
      number,
      {
        routeProcess: ProRouteProcessVO
        items: QaRegulationItem[]
        batchRecordBindingResolved: boolean
      }
    >()
    for (const item of qaRegulationItems.value) {
      for (const binding of resolveQaRegulationItemRouteProcesses(item, source, { publishing })) {
        const routeProcess = binding.routeProcess
        const routeProcessId = resolvePositiveId(routeProcess.id, '路线工序 ID')
        const group = itemsByRouteProcessId.get(routeProcessId) || {
          routeProcess,
          items: [],
          batchRecordBindingResolved: true
        }
        group.items.push(item)
        group.batchRecordBindingResolved =
          group.batchRecordBindingResolved && binding.batchRecordBindingResolved
        itemsByRouteProcessId.set(routeProcessId, group)
      }
    }
    if (itemsByRouteProcessId.size === 0) {
      if (!publishing) {
        throw new Error('当前 QA 规程没有可发布到激活路线版本的正式检验项目。')
      }
      const routeProcessId = resolvePositiveId(source.routeProcess.id, '路线工序 ID')
      itemsByRouteProcessId.set(routeProcessId, {
        routeProcess: source.routeProcess,
        items: [],
        batchRecordBindingResolved: false
      })
    }

    const routeId = resolvePositiveId(qaRegulationDraft.routeId, '路线 ID')
    const routeVersionId = resolvePositiveId(qaRegulationDraft.routeVersionId, '路线版本 ID')
    return Array.from(itemsByRouteProcessId.values())
      .sort((left, right) => Number(left.routeProcess.sort) - Number(right.routeProcess.sort))
      .map(({ routeProcess, items, batchRecordBindingResolved }) => {
        const routeProcessId = resolvePositiveId(routeProcess.id, '路线工序 ID')
        const processId = resolvePositiveId(routeProcess.processId, '工序 ID')
        const routeProcessName = resolveRequiredText(
          routeProcess.processName || routeProcess.processCode,
          '路线工序名称'
        )
        const batchConfig = findQaRouteProcessConfig(source.batchConfigs, routeProcessId)
        return {
          productId,
          productName: resolveRequiredText(qaRegulationDraft.productName, '产品名称'),
          routeId,
          routeName: resolveRequiredText(qaRegulationDraft.routeName, '路线名称'),
          routeVersionId,
          routeVersionNo: resolveRequiredText(qaRegulationDraft.routeVersionName, '路线版本号'),
          routeProcessId,
          processId,
          routeProcessName,
          batchRecordBindingSummary:
            batchRecordBindingResolved
              ? resolveFormalBatchRecordBindingSummary(batchConfig, routeProcess) || undefined
              : undefined,
          regulationCode: buildQaProcessRegulationCode(
            qaRegulationDraft.regulationCode,
            routeProcessId
          ),
          regulationName: `${resolveRequiredText(
            qaRegulationDraft.regulationName,
            '规程名称'
          )}-${routeProcessName}`,
          versionNo: resolveRequiredText(qaRegulationDraft.versionNo, '规程版本'),
          effectiveDate: qaRegulationDraft.effectiveDate || undefined,
          finalInspectionApplicable,
          finalInspectionNotApplicableReason,
          items: buildQaRegulationSaveItems(items, { publishing })
        }
      })
  } catch (error) {
    ElMessage.warning(resolveDccProjectCodeErrorMessage(error))
    return undefined
  }
}

const previewQaRegulationDraft = async () => {
  const payloads = buildQaRegulationSavePayloads()
  if (!payloads) {
    return
  }
  qaRegulationSaving.value = true
  try {
    let savedVersionNo = qaRegulationDraft.versionNo
    for (const payload of payloads) {
      const result = await QcTemplateApi.saveQaRegulationDraft(payload)
      qaRegulationDraft.lifecycleStatus = result.lifecycleStatus
      savedVersionNo = result.versionNo
    }
    if (qaRegulationDraft.lifecycleStatus !== 'PUBLISHED') {
      qaPublishedVersionNo.value = ''
    }
    ElMessage.success(`QA 规程草稿已按 ${payloads.length} 个工序保存：${savedVersionNo}`)
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
    ElMessage.warning(qaPublishBlockers.value.map((check) => check.detail).join('；'))
    return
  }
  const payloads = buildQaRegulationSavePayloads({ publishing: true })
  if (!payloads) {
    return
  }
  qaRegulationPublishing.value = true
  try {
    let publishedVersionNo = qaRegulationDraft.versionNo
    let latestPublishedVersion: QaInspectionRegulationPublishedVersionVO | undefined
    for (const payload of payloads) {
      const publishedVersion = await QcTemplateApi.publishQaRegulation(payload)
      publishedVersionNo = publishedVersion.versionNo
      latestPublishedVersion = publishedVersion
    }
    if (!latestPublishedVersion) {
      throw new Error('发布完成后未返回正式已发布 QA 规程版本。')
    }
    qaRegulationDraft.lifecycleStatus = 'PUBLISHED'
    qaPublishedVersionNo.value = publishedVersionNo.trim()
    qaCurrentPublishedVersionLoadSerial += 1
    qaCurrentPublishedVersion.value = latestPublishedVersion
    qaCurrentPublishedVersionLoading.value = false
    qaCurrentPublishedVersionLoadError.value = ''
    const publishedStatusByProductId = new Map(qaRegulationProjectStatusByProductId.value)
    const currentStatus = publishedStatusByProductId.get(latestPublishedVersion.productId)
    publishedStatusByProductId.set(latestPublishedVersion.productId, {
      productId: latestPublishedVersion.productId,
      configured: true,
      regulationCount: currentStatus?.regulationCount ?? payloads.length,
      regulationId: latestPublishedVersion.regulationId,
      currentVersionId: latestPublishedVersion.publishedVersionId,
      regulationCode: latestPublishedVersion.regulationCode,
      regulationName: latestPublishedVersion.regulationName,
      lifecycleStatus: 'PUBLISHED'
    })
    qaRegulationProjectStatusByProductId.value = publishedStatusByProductId
    ElMessage.success(
      `QA 规程已按 ${payloads.length} 个工序发布为不可变版本：${publishedVersionNo}`
    )
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
