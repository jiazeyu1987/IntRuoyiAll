<template>
  <ContentWrap v-if="!showPqcModuleTabs && !showProductionModuleTabs">
    <div class="team-leader-workbench__header">
      <div>
        <div class="team-leader-workbench__title">{{ pageTitle }}</div>
        <div class="team-leader-workbench__subtitle">
          {{ pageSubtitle }}
        </div>
      </div>
    </div>

    <el-tabs
      v-if="showLeaderTypeTabs"
      v-model="activeLeaderTab"
      data-team-leader-type-tabs
      @tab-change="handleLeaderTypeChange"
    >
      <el-tab-pane label="生产组长" name="PRODUCTION" />
      <el-tab-pane label="PQC 组长" name="PQC" />
    </el-tabs>
  </ContentWrap>

  <ContentWrap v-if="loadError">
      <el-alert :title="loadError" type="error" :closable="false" show-icon />
    </ContentWrap>

  <ContentWrap
    v-if="showProductionPersonnelModule"
    :class="{ 'team-leader-workbench__production-module-card': showProductionModuleTabs }"
    data-team-leader-production-personnel-tab
  >
    <el-tabs
      v-if="showProductionModuleTabs"
      v-model="activeProductionModuleTab"
      class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
      data-production-leader-module-tabs
    >
      <el-tab-pane label="人员管理" name="personnel" data-production-leader-module-tab-personnel />
      <el-tab-pane label="报工管理" name="report" data-production-leader-module-tab-report />
      <el-tab-pane label="活跃订单池" name="activeOrder" data-production-leader-module-tab-active-order />
      <el-tab-pane label="看板" name="dashboard" data-production-leader-module-tab-dashboard />
      <el-tab-pane label="异常" name="exception" data-production-leader-module-tab-exception />
      <el-tab-pane label="工序配置" name="processConfig" data-production-leader-module-tab-process-config />
      <el-tab-pane label="班组配置" name="config" data-production-leader-module-tab-config />
    </el-tabs>
    <div
      v-if="showProductionModuleTabs"
      class="team-leader-workbench__responsible-routes"
      data-production-leader-responsible-routes
      aria-label="生产组长负责的工艺路线"
    >
      <span class="team-leader-workbench__responsible-routes-label">负责工艺路线</span>
      <template v-if="productionResponsibleRouteNames.length">
        <el-tag
          v-for="routeName in productionResponsibleRouteNames"
          :key="routeName"
          class="team-leader-workbench__responsible-route-tag"
          type="success"
          effect="plain"
          :title="routeName"
        >
          {{ routeName }}
        </el-tag>
      </template>
      <span v-else class="team-leader-workbench__responsible-routes-empty">
        {{ processConfigLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
      </span>
    </div>
    <el-tabs
      v-model="productionPersonnelActiveTab"
      :class="[
        'team-leader-workbench__personnel-tabs',
        { 'team-leader-workbench__personnel-tabs--embedded': showProductionModuleTabs }
      ]"
    >
      <el-tab-pane label="生产人员档案" name="productionPersonnel">
        <UnifiedListTemplate
          table-key="mes.processPool.teamLeader.productionPersonnel"
          :query-model="productionPersonnelQuery"
          :filter-definitions="productionPersonnelFilterDefinitions"
          :quick-filter-state="productionPersonnelQuickFilterState"
          :operator-options="productionPersonnelOperatorOptions"
          :columns="productionPersonnelColumns"
          :show-quick-filter="false"
          :show-column-settings="false"
          :total="productionPersonnelTotal"
          :page="productionPersonnelQuery.pageNo"
          :limit="productionPersonnelQuery.pageSize"
          @update:page="handleProductionPersonnelPageChange"
          @update:limit="handleProductionPersonnelPageSizeChange"
          @pagination="refreshProductionPersonnel"
        >
          <template #extra-filters>
            <el-form-item>
              <el-button
                type="primary"
                data-team-leader-open-personnel-dialog
                @click="productionPersonnelAddDialogVisible = true"
              >
                <Icon icon="ep:plus" class="mr-5px" />
                新增人员
              </el-button>
            </el-form-item>
          </template>
          <template #table>
            <el-table
              v-loading="productionPersonnelLoading"
              :data="pagedProductionPersonnelRows"
              border
              stripe
              data-team-leader-production-personnel-list
            >
              <el-table-column label="显示名" min-width="140">
                <template #default="{ row }">
                  <span
                    class="team-leader-workbench__personnel-name"
                    :class="{ 'is-disabled': row.enabled === false }"
                  >
                    {{ row.displayName || row.employeeName || '--' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="来源" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.employeeType === 'TEMPORARY' ? 'warning' : 'success'" effect="plain">
                    {{ formatEmployeeType(row.employeeType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="员工编码" min-width="120">
                <template #default="{ row }">{{ row.employeeCode || '-' }}</template>
              </el-table-column>
              <el-table-column label="签名密码" min-width="140">
                <template #default="{ row }">
                  {{ formatSignaturePasswordManager(row) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.enabled === false ? 'danger' : 'success'" effect="plain">
                    {{ row.enabled === false ? '已禁用' : '可选择' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="260" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="updateEmployeeDisplayName(row)">
                    修改显示名
                  </el-button>
                  <el-button
                    link
                    :type="row.enabled === false ? 'success' : 'warning'"
                    @click="updateEmployeeStatus(row, row.enabled === false)"
                  >
                    {{ row.enabled === false ? '启用' : '禁用' }}
                  </el-button>
                  <el-button
                    v-if="row.employeeType === 'TEMPORARY'"
                    link
                    type="primary"
                    @click="resetTemporarySignaturePassword(row)"
                  >
                    重置签名密码
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </UnifiedListTemplate>

        <el-dialog
          data-team-leader-personnel-add-dialog
          v-model="productionPersonnelAddDialogVisible"
          width="960px"
          class="team-leader-workbench__personnel-dialog"
          :close-on-click-modal="!productionPersonnelSubmitting"
          @closed="clearProductionPersonnelDialogError"
        >
          <template #header>
            <div class="team-leader-workbench__personnel-dialog-header">
              <span class="team-leader-workbench__personnel-dialog-title">新增人员</span>
              <Transition name="team-leader-workbench__personnel-dialog-error">
                <div
                  v-if="productionPersonnelDialogError"
                  class="team-leader-workbench__personnel-dialog-error"
                  data-team-leader-personnel-dialog-error
                  role="alert"
                  aria-live="assertive"
                >
                  <span class="team-leader-workbench__personnel-dialog-error-text">
                    {{ productionPersonnelDialogError }}
                  </span>
                  <button
                    type="button"
                    class="team-leader-workbench__personnel-dialog-error-close"
                    data-team-leader-personnel-dialog-error-close
                    aria-label="关闭错误提示"
                    @click="clearProductionPersonnelDialogError"
                  >
                    <Icon icon="ep:close" />
                  </button>
                </div>
              </Transition>
            </div>
          </template>
          <div class="team-leader-workbench__personnel-actions team-leader-workbench__personnel-actions--dialog">
            <el-card shadow="never">
              <template #header>搜索选择正式工</template>
              <el-form :model="formalEmployeeForm" label-width="108px">
                <el-form-item label="正式工姓名">
                  <el-select
                    v-model="formalEmployeeForm.systemUserId"
                    filterable
                    remote
                    clearable
                    reserve-keyword
                    placeholder="输入姓名搜索"
                    :remote-method="searchFormalEmployeeCandidatesForSelect"
                    :loading="formalCandidateLoading"
                    class="team-leader-workbench__full-control"
                    data-team-leader-formal-employee-select
                  >
                    <!-- static contract anchor: remote-method="searchFormalEmployeeCandidatesForSelect" -->
                    <el-option
                      v-for="candidate in formalEmployeeCandidateOptions"
                      :key="candidate.systemUserId"
                      :label="candidate.displayName"
                      :value="candidate.systemUserId"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="显示名">
                  <el-input
                    v-model="formalEmployeeForm.displayName"
                    clearable
                    placeholder="可选；重名时请加后缀"
                  />
                </el-form-item>
                <el-alert
                  title="正式工电子签名密码继续使用原账号配置，本页不设置或重置。"
                  type="info"
                  :closable="false"
                  show-icon
                />
                <el-form-item class="team-leader-workbench__form-actions">
                  <el-button
                    type="primary"
                    :loading="productionPersonnelSubmitting"
                    @click="submitLinkFormalEmployee"
                  >
                    关联正式工
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <el-card shadow="never">
              <template #header>手动录入临时工</template>
              <el-form
                :model="temporaryEmployeeForm"
                label-width="108px"
                data-team-leader-temporary-employee-form
              >
                <el-form-item label="显示名">
                  <el-input
                    v-model="temporaryEmployeeForm.displayName"
                    clearable
                    placeholder="同组长有效员工不能重名，重名请加后缀"
                    @input="clearProductionPersonnelDialogError"
                  />
                </el-form-item>
                <el-form-item label="签名密码">
                  <el-input
                    v-model="temporaryEmployeeForm.signaturePassword"
                    show-password
                    clearable
                    placeholder="用于统一电子签名流程"
                  />
                </el-form-item>
                <el-alert
                  title="临时工只创建生产人员档案，不创建系统登录账号。"
                  type="info"
                  :closable="false"
                  show-icon
                />
                <el-form-item class="team-leader-workbench__form-actions">
                  <el-button
                    type="primary"
                    :loading="productionPersonnelSubmitting"
                    @click="submitCreateTemporaryEmployee"
                  >
                    新增临时工
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </div>
        </el-dialog>

      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <ContentWrap
    v-if="showPqcPersonnelModule"
    :class="{ 'team-leader-workbench__pqc-module-card': showPqcModuleTabs }"
    data-pqc-leader-personnel-tab
  >
    <div v-if="showPqcModuleTabs" class="team-leader-workbench__embedded-header">
      <div class="team-leader-workbench__title">{{ pageTitle }}</div>
      <div class="team-leader-workbench__subtitle">
        {{ pageSubtitle }}
      </div>
    </div>
    <el-tabs
      v-if="showPqcModuleTabs"
      v-model="activePqcModuleTab"
      class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
      data-pqc-leader-module-tabs
    >
      <el-tab-pane label="人员管理" name="personnel" data-pqc-leader-module-tab-personnel />
      <el-tab-pane label="PQC管理" name="management" data-pqc-leader-module-tab-management />
      <el-tab-pane label="详情" name="detail" data-pqc-leader-module-tab-detail />
      <el-tab-pane label="看板" name="dashboard" data-pqc-leader-module-tab-dashboard />
    </el-tabs>

    <UnifiedListTemplate
      table-key="mes.processPool.teamLeader.pqcPersonnel"
      :query-model="pqcPersonnelQuery"
      :filter-definitions="pqcPersonnelFilterDefinitions"
      :quick-filter-state="pqcPersonnelQuickFilterState"
      :operator-options="pqcPersonnelOperatorOptions"
      :columns="pqcPersonnelColumns"
      :show-quick-filter="false"
      :show-column-settings="false"
      :total="pqcPersonnelTotal"
      :page="pqcPersonnelQuery.pageNo"
      :limit="pqcPersonnelQuery.pageSize"
      @update:page="handlePqcPersonnelPageChange"
      @update:limit="handlePqcPersonnelPageSizeChange"
      @pagination="refreshPqcPersonnel"
    >
      <template #actions>
        <el-button
          type="primary"
          data-pqc-personnel-add-button
          @click="pqcPersonnelAddDialogVisible = true"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增
        </el-button>
      </template>
      <template #table>
        <el-table
          v-loading="pqcPersonnelLoading"
          :data="pagedPqcPersonnelRows"
          border
          stripe
          data-pqc-leader-personnel-list
        >
          <el-table-column label="PQC检验员" min-width="180">
            <template #default="{ row }">
              <span
                class="team-leader-workbench__pqc-personnel-name"
                :class="{ 'is-disabled': row.enabled === false }"
              >
                {{ row.displayName || row.username || '--' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="账号" min-width="160">
            <template #default="{ row }">{{ row.username }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.enabled === false ? 'danger' : 'success'" effect="plain">
                {{ row.enabled === false ? '已禁用' : '已启用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                :type="row.enabled === false ? 'success' : 'warning'"
                @click="updatePqcInspectorStatus(row, row.enabled === false)"
              >
                {{ row.enabled === false ? '启用' : '禁用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>

    <el-dialog
      v-model="pqcPersonnelAddDialogVisible"
      data-pqc-personnel-add-dialog
      title="新增 PQC 检验员"
      width="520px"
      :close-on-click-modal="!pqcPersonnelSubmitting"
    >
      <el-form :model="pqcPersonnelForm" label-width="110px">
        <el-form-item label="PQC检验员">
          <el-select
            v-model="pqcPersonnelForm.systemUserId"
            filterable
            remote
            clearable
            reserve-keyword
            automatic-dropdown
            remote-show-suffix
            placeholder="点击或输入姓名、账号搜索"
            :remote-method="searchPqcFormalEmployeeCandidatesForSelect"
            :loading="pqcCandidateLoading"
            class="team-leader-workbench__full-control"
            @focus="loadPqcFormalEmployeeCandidatesForSelect"
            @visible-change="handlePqcCandidateDropdownVisibleChange"
          >
            <el-option
              v-for="candidate in pqcCandidateOptions"
              :key="candidate.systemUserId"
              :label="candidate.displayName"
              :value="candidate.systemUserId"
              :disabled="candidate.disabled"
              :class="{
                'team-leader-workbench__pqc-candidate-option--occupied': candidate.occupiedByOtherPqcLeader
              }"
            >
              <div class="team-leader-workbench__pqc-candidate-option">
                <span>{{ candidate.displayName }}</span>
                <span
                  v-if="candidate.occupiedByOtherPqcLeader"
                  class="team-leader-workbench__pqc-candidate-disabled-reason"
                >
                  {{ candidate.disabledReason || '已被其他PQC组长选择' }}
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="pqcPersonnelSubmitting" @click="pqcPersonnelAddDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="pqcPersonnelSubmitting"
          @click="submitLinkPqcFormalEmployee"
        >
          确认关联
        </el-button>
      </template>
    </el-dialog>
  </ContentWrap>

  <ContentWrap
    v-if="showPqcManagementModule"
    :class="{
      'team-leader-workbench__pqc-module-card': showPqcModuleTabs,
      'team-leader-workbench__production-module-card': showProductionModuleTabs
    }"
    data-team-leader-report-workbench
  >
      <el-tabs
        v-if="showProductionModuleTabs"
        v-model="activeProductionModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
        data-production-leader-module-tabs
      >
        <el-tab-pane label="人员管理" name="personnel" data-production-leader-module-tab-personnel />
        <el-tab-pane label="报工管理" name="report" data-production-leader-module-tab-report />
        <el-tab-pane label="活跃订单池" name="activeOrder" data-production-leader-module-tab-active-order />
        <el-tab-pane label="看板" name="dashboard" data-production-leader-module-tab-dashboard />
        <el-tab-pane label="异常" name="exception" data-production-leader-module-tab-exception />
        <el-tab-pane label="工序配置" name="processConfig" data-production-leader-module-tab-process-config />
        <el-tab-pane label="班组配置" name="config" data-production-leader-module-tab-config />
      </el-tabs>
      <div
        v-if="showProductionModuleTabs"
        class="team-leader-workbench__responsible-routes"
        data-production-leader-responsible-routes
        aria-label="生产组长负责的工艺路线"
      >
        <span class="team-leader-workbench__responsible-routes-label">负责工艺路线</span>
        <template v-if="productionResponsibleRouteNames.length">
          <el-tag
            v-for="routeName in productionResponsibleRouteNames"
            :key="routeName"
            class="team-leader-workbench__responsible-route-tag"
            type="success"
            effect="plain"
            :title="routeName"
          >
            {{ routeName }}
          </el-tag>
        </template>
        <span v-else class="team-leader-workbench__responsible-routes-empty">
          {{ processConfigLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
        </span>
      </div>
      <div v-if="showPqcModuleTabs" class="team-leader-workbench__embedded-header">
        <div class="team-leader-workbench__title">{{ pageTitle }}</div>
        <div class="team-leader-workbench__subtitle">
          {{ pageSubtitle }}
        </div>
      </div>
      <el-tabs
        v-if="showPqcModuleTabs"
        v-model="activePqcModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
      data-pqc-leader-module-tabs
      >
        <el-tab-pane label="人员管理" name="personnel" data-pqc-leader-module-tab-personnel />
        <el-tab-pane label="PQC管理" name="management" data-pqc-leader-module-tab-management />
        <el-tab-pane label="详情" name="detail" data-pqc-leader-module-tab-detail />
        <el-tab-pane label="看板" name="dashboard" data-pqc-leader-module-tab-dashboard />
      </el-tabs>
      <div v-if="!showPqcModuleTabs && !showProductionModuleTabs" class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">报工确认工作台</div>
          <div class="team-leader-workbench__hint">
            查看员工结构化报工，确认后按 FIFO 或手动分配到活跃订单。
          </div>
        </div>
      </div>
      <UnifiedListTemplate
        table-key="mes.processPool.teamLeader.submissions"
        :query-model="queryParams"
        label-width="88px"
        :filter-definitions="submissionQuickFilterDefinitions"
        :show-quick-filter="false"
        single-line-toolbar
        :quick-filter-state="submissionQuickFilterState"
        :operator-options="submissionOperatorOptions"
        :show-multi-filter="true"
        :multi-filter-definitions="submissionMultiFilterDefinitions"
        :multi-filter-state="submissionMultiFilterState"
        :columns="submissionColumns"
        :column-saving="submissionColumnSaving"
        :total="submissionTotal"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @update:multi-filter-state="updateSubmissionMultiFilterState"
        @multi-filter-query="applySubmissionMultiFilter"
        @multi-filter-reset="resetSubmissionMultiFilter"
        @multi-filter-remove="removeSubmissionMultiFilterCondition"
        @column-change="saveSubmissionColumnConfig"
        @column-reset="resetSubmissionColumnConfig"
        @pagination="getSubmissionList"
      >
        <template #table>
          <el-table
            v-loading="loading"
            data-user-table-column-explicit
            data-user-table-key="mes.processPool.teamLeader.submissions"
            :data="submissionList"
            border
            stripe
            :show-overflow-tooltip="true"
            @header-dragend="handleSubmissionHeaderDragend"
          >
            <el-table-column
              v-if="isSubmissionColumnVisible('submittedAt')"
              label="提交时间"
              prop="submittedAt"
              :min-width="getSubmissionColumnMinWidthString('submittedAt', 160)"
            >
              <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
            </el-table-column>
            <el-table-column
              v-if="isSubmissionColumnVisible('employeeUser')"
              :label="employeeColumnLabel"
              prop="employeeUser"
              :min-width="getSubmissionColumnMinWidthString('employeeUser', 140)"
            >
              <template #default="{ row }">
                {{ row.actualEmployeeUserName || row.actualEmployeeUserId || '--' }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="isSubmissionColumnVisible('process')"
              label="工序"
              prop="process"
              :min-width="getSubmissionColumnMinWidthString('process', 150)"
            >
              <template #default="{ row }">{{ row.processName || row.processCode || '--' }}</template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab === 'PQC' && isSubmissionColumnVisible('workOrder')"
              label="生产工单"
              prop="workOrder"
              :min-width="getSubmissionColumnMinWidthString('workOrder', 160)"
            >
              <template #default="{ row }">
                <span data-pqc-leader-work-order>
                  {{ row.workOrderCode || row.workOrderName || '--' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isSubmissionColumnVisible('completionQuantity')"
              :label="completionQuantityColumnLabel"
              prop="completionQuantity"
              :min-width="getSubmissionColumnMinWidthString('completionQuantity', 130)"
            >
              <template #default="{ row }">
                <span data-team-leader-completion-quantity>
                  {{ resolveSubmissionCompletionQuantity(row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isSubmissionColumnVisible('lossQuantity')"
              label="损耗数量"
              prop="lossQuantity"
              :min-width="getSubmissionColumnMinWidthString('lossQuantity', 120)"
            >
              <template #default="{ row }">
                <span data-team-leader-loss-quantity>
                  {{ resolveSubmissionLossQuantity(row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isSubmissionColumnVisible('lossBreakdown')"
              label="损耗明细"
              prop="lossBreakdown"
              :min-width="getSubmissionColumnMinWidthString('lossBreakdown', 210)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__structured-list" data-team-leader-loss-breakdown>
                  <span
                    v-for="item in resolveSubmissionLossBreakdownItems(row)"
                    :key="item.key"
                    class="team-leader-workbench__structured-pill"
                  >
                    {{ item.label }}：{{ item.valueText }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab === 'PQC' && isSubmissionColumnVisible('product')"
              label="产品"
              prop="product"
              :min-width="getSubmissionColumnMinWidthString('product', 180)"
            >
              <template #default="{ row }">
                <span data-pqc-leader-submission-product>
                  {{ row.productCode || row.productName || '--' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab === 'PQC' && isSubmissionColumnVisible('inspectionTask')"
              label="检验类型/轮次"
              prop="inspectionTask"
              :min-width="getSubmissionColumnMinWidthString('inspectionTask', 150)"
            >
              <template #default="{ row }">
                <span data-pqc-leader-submission-task>
                  {{ resolvePqcInspectionTypeText(row.inspectionType) }} / 第 {{ row.roundNo || '--' }} 轮
                </span>
              </template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab === 'PQC' && isSubmissionColumnVisible('inspectionItems')"
              label="检验项"
              prop="inspectionItems"
              :min-width="getSubmissionColumnMinWidthString('inspectionItems', 190)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__structured-list" data-pqc-leader-inspection-items>
                  <span
                    v-for="item in resolvePqcInspectionItemItems(row)"
                    :key="item.key"
                    class="team-leader-workbench__structured-pill"
                  >
                    {{ item.valueText }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isSubmissionColumnVisible('equipmentSnapshot')"
              label="设备"
              prop="equipmentSnapshot"
              :min-width="getSubmissionColumnMinWidthString('equipmentSnapshot', 220)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__structured-list" data-team-leader-equipment-snapshot>
                  <span
                    v-for="item in resolveSubmissionEquipmentItems(row)"
                    :key="item.key"
                    class="team-leader-workbench__structured-pill"
                  >
                    {{ item.valueText }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isSubmissionColumnVisible('selectedDevice')"
              label="选用设备"
              prop="selectedDevice"
              :min-width="getSubmissionColumnMinWidthString('selectedDevice', 220)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__structured-list" data-team-leader-selected-device>
                  <span
                    v-for="item in resolveSubmissionEquipmentItems(row)"
                    :key="item.key"
                    class="team-leader-workbench__structured-pill"
                  >
                    {{ item.valueText }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab === 'PQC' && isSubmissionColumnVisible('equipmentNumber')"
              label="设备编号"
              prop="equipmentNumber"
              :min-width="getSubmissionColumnMinWidthString('equipmentNumber', 150)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__structured-list" data-pqc-leader-equipment-number>
                  <span
                    v-for="item in resolvePqcEquipmentNumberItems(row)"
                    :key="item.key"
                    class="team-leader-workbench__structured-pill"
                  >
                    {{ item.label }}：{{ item.valueText }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab === 'PQC' && isSubmissionColumnVisible('acceptanceStandard')"
              label="接收标准"
              prop="acceptanceStandard"
              :min-width="getSubmissionColumnMinWidthString('acceptanceStandard', 220)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__structured-list" data-pqc-leader-acceptance-standard>
                  <span
                    v-for="item in resolvePqcAcceptanceStandardItems(row)"
                    :key="item.key"
                    class="team-leader-workbench__structured-pill"
                  >
                    {{ item.label }}：{{ item.valueText }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab === 'PQC' && isSubmissionColumnVisible('inspectionMethod')"
              label="检验方法"
              prop="inspectionMethod"
              :min-width="getSubmissionColumnMinWidthString('inspectionMethod', 180)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__structured-list" data-pqc-leader-inspection-method>
                  <span
                    v-for="item in resolvePqcInspectionMethodItems(row)"
                    :key="item.key"
                    class="team-leader-workbench__structured-pill"
                  >
                    {{ item.label }}：{{ item.valueText }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab === 'PQC' && isSubmissionColumnVisible('inspectionJudgement')"
              label="检验判定"
              prop="inspectionJudgement"
              :min-width="getSubmissionColumnMinWidthString('inspectionJudgement', 150)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__structured-list" data-pqc-leader-inspection-judgement>
                  <span
                    v-for="item in resolvePqcInspectionJudgementItems(row)"
                    :key="item.key"
                    class="team-leader-workbench__structured-pill"
                  >
                    {{ item.label }}：{{ item.valueText }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isSubmissionColumnVisible('parameterSnapshot')"
              label="参数明细"
              prop="parameterSnapshot"
              :min-width="getSubmissionColumnMinWidthString('parameterSnapshot', 280)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__parameter-list" data-team-leader-parameter-snapshot>
                  <div
                    v-for="item in resolveSubmissionParameterItems(row)"
                    :key="item.key"
                    class="team-leader-workbench__parameter-item"
                  >
                    <span class="team-leader-workbench__parameter-label">{{ item.label }}</span>
                    <span
                      class="team-leader-workbench__parameter-value"
                      :class="{ 'is-parameter-out-of-range': item.outOfRange }"
                      :data-parameter-status="item.parameterStatus || (item.outOfRange ? 'ABNORMAL' : 'NORMAL')"
                      :aria-label="item.outOfRange ? `参数异常：${item.label} ${item.valueText}` : item.label"
                    >
                      {{ item.valueText }}
                    </span>
                    <span v-if="item.metaText" class="team-leader-workbench__parameter-meta">
                      {{ item.metaText }}
                    </span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isSubmissionColumnVisible('deviceParameterReadings')"
              label="设备参数"
              prop="deviceParameterReadings"
              :min-width="getSubmissionColumnMinWidthString('deviceParameterReadings', 280)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__parameter-list" data-team-leader-device-parameter-readings>
                  <div
                    v-for="item in resolveSubmissionParameterItems(row)"
                    :key="item.key"
                    class="team-leader-workbench__parameter-item"
                  >
                    <span class="team-leader-workbench__parameter-label">{{ item.label }}</span>
                    <span
                      class="team-leader-workbench__parameter-value"
                      :class="{ 'is-parameter-out-of-range': item.outOfRange }"
                      :data-parameter-status="item.parameterStatus || (item.outOfRange ? 'ABNORMAL' : 'NORMAL')"
                      :aria-label="item.outOfRange ? `参数异常：${item.label} ${item.valueText}` : item.label"
                    >
                      {{ item.valueText }}
                    </span>
                    <span v-if="item.metaText" class="team-leader-workbench__parameter-meta">
                      {{ item.metaText }}
                    </span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab === 'PQC' && isSubmissionColumnVisible('defectDescription')"
              label="不良说明"
              prop="defectDescription"
              :min-width="getSubmissionColumnMinWidthString('defectDescription', 180)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__structured-list" data-pqc-leader-defect-description>
                  <span class="team-leader-workbench__structured-pill">
                    {{ resolvePqcDefectDescriptionText(row) }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab !== 'PQC' && isSubmissionColumnVisible('auditCopyStatus')"
              label="审核副本"
              prop="auditCopyStatus"
              :min-width="getSubmissionColumnMinWidthString('auditCopyStatus', 130)"
            >
              <template #default="{ row }">{{ row.auditCopyStatus || '--' }}</template>
            </el-table-column>
            <el-table-column
              v-if="activeLeaderTab !== 'PQC' && isSubmissionColumnVisible('submissionReviewStatus')"
              label="复核判定"
              prop="submissionReviewStatus"
              :min-width="getSubmissionColumnMinWidthString('submissionReviewStatus', 190)"
            >
              <template #default="{ row }">
                <div class="team-leader-workbench__review-log" data-team-leader-review-log>
                  <el-tag :type="resolveSubmissionReviewTagType(row.submissionReviewStatus)" effect="plain">
                    {{ resolveSubmissionReviewStatusText(row.submissionReviewStatus) }}
                  </el-tag>
                  <span v-if="row.submissionReviewRemark" class="team-leader-workbench__review-text">
                    {{ row.submissionReviewRemark }}
                  </span>
                  <span v-if="row.submissionReviewedAt" class="team-leader-workbench__review-meta">
                    复核人 {{ row.submissionReviewLeaderUserId || '--' }} ·
                    {{ formatDateTime(row.submissionReviewedAt) }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              v-if="isSubmissionColumnVisible('operation')"
              label="操作"
              prop="operation"
              :width="getSubmissionColumnWidthString('operation', 270)"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  :data-team-leader-detail-event-id="String(row.id)"
                  @click="openDetail(row)"
                >
                  详情
                </el-button>
                <el-button
                  v-if="canReviewSubmission(row)"
                  link
                  type="success"
                  :data-team-leader-review-event-id="String(row.id)"
                  @click="openReview(row)"
                >
                  复核
                </el-button>
                <el-button
                  v-if="canCorrectSubmission(row)"
                  link
                  type="warning"
                  :data-team-leader-correction-event-id="String(row.id)"
                  @click="openCorrection(row)"
                >
                  修正
                </el-button>
                <el-button v-if="isProductionLeader" link type="warning" @click="prefillAbnormal(row)">
                  标记异常
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </UnifiedListTemplate>
    </ContentWrap>

    <ContentWrap
      v-if="showPqcDetailModule"
      class="team-leader-workbench__pqc-module-card"
      data-pqc-leader-detail-tab
    >
      <div class="team-leader-workbench__embedded-header">
        <div class="team-leader-workbench__title">{{ pageTitle }}</div>
        <div class="team-leader-workbench__subtitle">
          {{ pageSubtitle }}
        </div>
      </div>
      <el-tabs
        v-model="activePqcModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
        data-pqc-leader-module-tabs
      >
        <el-tab-pane label="人员管理" name="personnel" data-pqc-leader-module-tab-personnel />
        <el-tab-pane label="PQC管理" name="management" data-pqc-leader-module-tab-management />
        <el-tab-pane label="详情" name="detail" data-pqc-leader-module-tab-detail />
        <el-tab-pane label="看板" name="dashboard" data-pqc-leader-module-tab-dashboard />
      </el-tabs>

      <div v-loading="detailLoading" class="team-leader-workbench__detail-tab-body">
        <el-empty
          v-if="!detail && !detailLoading"
          description="请先在 PQC管理 列表点击详情"
        />
        <template v-else-if="detail">
          <el-descriptions
            :column="1"
            border
            class="team-leader-workbench__detail-descriptions"
            label-width="400px"
            data-team-leader-structured-detail
          >
            <el-descriptions-item label="服务端提交时间">
              {{ formatDateTime(detail.submittedAt) }}
            </el-descriptions-item>
            <el-descriptions-item :label="employeeDetailLabel">
              {{ detail.actualEmployeeUserName || detail.actualEmployeeUserId || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="工序">
              {{ detail.processName || detail.processCode || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="生产工单">
              {{ detail.workOrderCode || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="提交摘要">
              {{ detail.submittedSummary || '--' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.pqcResult || detail.pqcSummary" label="PQC检验内容">
              <el-tag :type="resolvePqcTagType(detail.pqcResult)" effect="plain">
                {{ detail.pqcSummary || detail.pqcResult }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div
            v-if="isPqcSubmissionRow(detail)"
            class="team-leader-workbench__detail-standard-list"
          >
            <div class="team-leader-workbench__submission-log-title">PQC项目明细</div>
            <UnifiedListTemplate
              table-key="mes.processPool.teamLeader.pqcSubmissionDetailItems"
              :query-model="pqcDetailQuery"
              :filter-definitions="pqcDetailFilterDefinitions"
              :quick-filter-state="pqcDetailQuickFilterState"
              :operator-options="pqcDetailOperatorOptions"
              :columns="pqcDetailColumns"
              :show-query-form="false"
              :show-column-settings="false"
              :total="pqcDetailTotal"
              v-model:page="pqcDetailQuery.pageNo"
              v-model:limit="pqcDetailQuery.pageSize"
            >
              <template #table>
                <el-table
                  :data="pagedPqcDetailRows"
                  border
                  size="small"
                  data-pqc-leader-item-snapshot-table
                  empty-text="PQC提交内容缺少正式项目明细"
                >
                  <el-table-column label="检验项目" min-width="120">
                    <template #default="{ row }">{{ row.itemName || row.itemCode || '--' }}</template>
                  </el-table-column>
                  <el-table-column label="检验设备" min-width="140">
                    <template #default="{ row }">
                      {{ row.selectedEquipmentName || row.selectedEquipmentCode || '--' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="设备编号" prop="selectedEquipmentNumber" min-width="130" />
                  <el-table-column label="接收标准" min-width="180">
                    <template #default="{ row }">{{ formatPqcSnapshotStandard(row) }}</template>
                  </el-table-column>
                  <el-table-column label="检验方法" prop="inspectionMethod" min-width="180" />
                  <el-table-column label="样本值" min-width="180">
                    <template #default="{ row }">
                      <span data-pqc-leader-detail-sample-values>
                        {{ formatPqcSnapshotSampleValues(row) }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="判定" min-width="100">
                    <template #default="{ row }">{{ row.judgement || row.itemResult || '--' }}</template>
                  </el-table-column>
                </el-table>
              </template>
            </UnifiedListTemplate>
          </div>

          <div
            v-if="isPqcSubmissionRow(detail)"
            class="team-leader-workbench__submission-log"
            data-pqc-submission-log
          >
            <div class="team-leader-workbench__submission-log-title">PQC提交日志</div>
            <el-descriptions
              :column="1"
              border
              class="team-leader-workbench__detail-descriptions"
              label-width="400px"
            >
              <el-descriptions-item label="提交事件编号">
                {{ detail.id || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="PQC检验员">
                {{ detail.actualEmployeeUserName || detail.actualEmployeeUserId || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="服务端提交时间">
                {{ formatDateTime(detail.submittedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="签名编号">
                <span data-pqc-submission-signature-id>
                  {{ detail.electronicSignatureId || '--' }}
                </span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </template>
      </div>
    </ContentWrap>

    <ContentWrap
      v-if="showProductionActiveOrderModule"
      :class="{ 'team-leader-workbench__production-module-card': showProductionModuleTabs }"
      data-team-leader-active-order-config
      data-team-leader-active-order-pool-tab
    >
      <el-tabs
        v-if="showProductionModuleTabs"
        v-model="activeProductionModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
        data-production-leader-module-tabs
      >
        <el-tab-pane label="人员管理" name="personnel" data-production-leader-module-tab-personnel />
        <el-tab-pane label="报工管理" name="report" data-production-leader-module-tab-report />
        <el-tab-pane label="活跃订单池" name="activeOrder" data-production-leader-module-tab-active-order />
        <el-tab-pane label="看板" name="dashboard" data-production-leader-module-tab-dashboard />
        <el-tab-pane label="异常" name="exception" data-production-leader-module-tab-exception />
        <el-tab-pane label="工序配置" name="processConfig" data-production-leader-module-tab-process-config />
        <el-tab-pane label="班组配置" name="config" data-production-leader-module-tab-config />
      </el-tabs>
      <div
        v-if="showProductionModuleTabs"
        class="team-leader-workbench__responsible-routes"
        data-production-leader-responsible-routes
        aria-label="生产组长负责的工艺路线"
      >
        <span class="team-leader-workbench__responsible-routes-label">负责工艺路线</span>
        <template v-if="productionResponsibleRouteNames.length">
          <el-tag
            v-for="routeName in productionResponsibleRouteNames"
            :key="routeName"
            class="team-leader-workbench__responsible-route-tag"
            type="success"
            effect="plain"
            :title="routeName"
          >
            {{ routeName }}
          </el-tag>
        </template>
        <span v-else class="team-leader-workbench__responsible-routes-empty">
          {{ processConfigLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
        </span>
      </div>

      <UnifiedListTemplate
        table-key="mes.processPool.teamLeader.activeOrders"
        :query-model="activeOrderQuery"
        :filter-definitions="activeOrderFilterDefinitions"
        :quick-filter-state="activeOrderQuickFilterState"
        :operator-options="activeOrderOperatorOptions"
        :columns="activeOrderColumns"
        :show-quick-filter="false"
        :show-column-settings="false"
        single-line-toolbar
        :total="activeOrderTotal"
        v-model:page="activeOrderQuery.pageNo"
        v-model:limit="activeOrderQuery.pageSize"
      >
        <template #actions>
          <el-button
            type="primary"
            data-team-leader-open-active-order-dialog
            @click="openActiveOrderDialog"
          >
            <Icon icon="ep:plus" class="mr-5px" />
            新增活跃订单
          </el-button>
        </template>
        <template #table>
          <el-table
            v-loading="activeOrderLoading"
            :data="pagedActiveOrderRows"
            border
            stripe
            :show-overflow-tooltip="true"
            data-team-leader-active-order-list
          >
            <el-table-column label="活跃池ID" prop="id" width="110">
              <template #default="{ row }">
                <span :data-team-leader-active-order-id="String(row.id)">{{ row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column label="生产订单ID" prop="workOrderId" min-width="130" />
            <el-table-column label="路线ID" prop="routeId" min-width="120" />
            <el-table-column label="路线版本ID" prop="routeVersionId" min-width="130" />
            <el-table-column label="ERP生产数量" min-width="130">
              <template #default="{ row }">
                {{ formatTraceQuantity(row.erpFixedQuantitySnapshot) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="resolveActiveOrderStatusType(row.activeStatus)" effect="plain">
                  {{ resolveActiveOrderStatusText(row.activeStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="加入时间" min-width="170">
              <template #default="{ row }">{{ formatDateTime(row.joinedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="danger"
                  :loading="maintenanceSubmitting"
                  @click="submitRemoveActiveOrder(row)"
                >
                  移出活跃订单
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </UnifiedListTemplate>

      <el-divider>调拨库存追溯</el-divider>
      <el-alert
        v-if="activeOrderTransferTraceError"
        :title="activeOrderTransferTraceError"
        type="error"
        :closable="false"
        show-icon
        data-team-leader-active-order-transfer-trace-error
      />
      <el-table
        v-else
        :data="activeOrderTransferTraceRows"
        v-loading="activeOrderTransferTraceLoading"
        size="small"
        border
        class="team-leader-workbench__transfer-trace"
        empty-text="暂无正式调拨/发货/补料/退料追溯"
        data-team-leader-active-order-transfer-trace
      >
        <el-table-column label="活跃池" width="76">
          <template #default="{ row }">
            <span data-transfer-trace-active-order-id>{{ row.activeOrderId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源类型" min-width="92">
          <template #default="{ row }">
            <span data-transfer-trace-source-type>{{ row.sourceType }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源单号" min-width="116">
          <template #default="{ row }">
            <span data-transfer-trace-source-object-code>
              {{ row.sourceObjectCode || row.sourceObjectId || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="88">
          <template #default="{ row }">
            <span data-transfer-trace-source-status>{{ row.sourceStatus || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" min-width="82">
          <template #default="{ row }">
            <span data-transfer-trace-quantity>{{ formatTraceQuantity(row.quantity) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存ID" min-width="86">
          <template #default="{ row }">
            <span data-transfer-trace-material-stock-id>{{ row.materialStockId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="批次ID" min-width="86">
          <template #default="{ row }">
            <span data-transfer-trace-batch-id>{{ row.batchId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="幂等键" min-width="160">
          <template #default="{ row }">
            <span data-transfer-trace-idempotency-key>{{ row.idempotencyKey }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog
        v-model="activeOrderAddDialogVisible"
        data-team-leader-active-order-add-dialog
        title="新增活跃订单"
        width="560px"
        :close-on-click-modal="!maintenanceSubmitting"
        @closed="resetActiveOrderForm"
      >
        <el-form :model="activeOrderForm" label-width="110px">
          <el-form-item label="订单号" data-team-leader-active-order-work-order-code>
            <el-select
              v-model="activeOrderForm.workOrderId"
              filterable
              remote
              clearable
              reserve-keyword
              :remote-method="searchActiveOrderCandidates"
              :loading="activeOrderCandidateLoading"
              placeholder="请输入并选择订单号"
              class="team-leader-workbench__full-control"
              @change="handleActiveOrderCandidateChange"
              @clear="handleActiveOrderCandidateClear"
            >
              <el-option
                v-for="candidate in activeOrderCandidateOptions"
                :key="candidate.workOrderId"
                :label="candidate.workOrderCode"
                :value="candidate.workOrderId"
              >
                <div
                  class="team-leader-workbench__active-order-candidate"
                  :class="{ 'is-eligible': candidate.eligible }"
                >
                  <span class="team-leader-workbench__active-order-candidate-code">
                    {{ candidate.workOrderCode }}
                  </span>
                  <span
                    v-if="candidate.eligible"
                    class="team-leader-workbench__active-order-candidate-badge"
                  >
                    符合要求
                  </span>
                  <span v-else class="team-leader-workbench__active-order-candidate-reason">
                    {{ candidate.ineligibleReason || '暂不符合' }}
                  </span>
                </div>
              </el-option>
            </el-select>
            <div
              v-if="activeOrderCandidateError"
              class="team-leader-workbench__form-error"
              data-team-leader-active-order-candidate-error
            >
              {{ activeOrderCandidateError }}
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button :disabled="maintenanceSubmitting" @click="activeOrderAddDialogVisible = false">
            取消
          </el-button>
          <el-button type="primary" :loading="maintenanceSubmitting" @click="submitAddActiveOrder">
            加入活跃订单
          </el-button>
        </template>
      </el-dialog>
    </ContentWrap>

    <ContentWrap
      v-if="showPqcDashboardModule"
      :class="{
        'team-leader-workbench__pqc-module-card': showPqcModuleTabs,
        'team-leader-workbench__production-module-card': showProductionModuleTabs
      }"
      data-role-matrix-daily-close
    >
      <el-tabs
        v-if="showProductionModuleTabs"
        v-model="activeProductionModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
        data-production-leader-module-tabs
      >
        <el-tab-pane label="人员管理" name="personnel" data-production-leader-module-tab-personnel />
        <el-tab-pane label="报工管理" name="report" data-production-leader-module-tab-report />
        <el-tab-pane label="活跃订单池" name="activeOrder" data-production-leader-module-tab-active-order />
        <el-tab-pane label="看板" name="dashboard" data-production-leader-module-tab-dashboard />
        <el-tab-pane label="异常" name="exception" data-production-leader-module-tab-exception />
        <el-tab-pane label="工序配置" name="processConfig" data-production-leader-module-tab-process-config />
        <el-tab-pane label="班组配置" name="config" data-production-leader-module-tab-config />
      </el-tabs>
      <div
        v-if="showProductionModuleTabs"
        class="team-leader-workbench__responsible-routes"
        data-production-leader-responsible-routes
        aria-label="生产组长负责的工艺路线"
      >
        <span class="team-leader-workbench__responsible-routes-label">负责工艺路线</span>
        <template v-if="productionResponsibleRouteNames.length">
          <el-tag
            v-for="routeName in productionResponsibleRouteNames"
            :key="routeName"
            class="team-leader-workbench__responsible-route-tag"
            type="success"
            effect="plain"
            :title="routeName"
          >
            {{ routeName }}
          </el-tag>
        </template>
        <span v-else class="team-leader-workbench__responsible-routes-empty">
          {{ processConfigLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
        </span>
      </div>
      <div v-if="showPqcModuleTabs" class="team-leader-workbench__embedded-header">
        <div class="team-leader-workbench__title">{{ pageTitle }}</div>
        <div class="team-leader-workbench__subtitle">
          {{ pageSubtitle }}
        </div>
      </div>
      <el-tabs
        v-if="showPqcModuleTabs"
        v-model="activePqcModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
      data-pqc-leader-module-tabs
      >
        <el-tab-pane label="人员管理" name="personnel" data-pqc-leader-module-tab-personnel />
        <el-tab-pane label="PQC管理" name="management" data-pqc-leader-module-tab-management />
        <el-tab-pane label="详情" name="detail" data-pqc-leader-module-tab-detail />
        <el-tab-pane label="看板" name="dashboard" data-pqc-leader-module-tab-dashboard />
      </el-tabs>
      <div v-if="!showPqcModuleTabs" class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">日结待处理看板</div>
          <div class="team-leader-workbench__hint">
            汇总当前筛选范围内真实报工、复核和活跃订单状态，日结前未关闭项必须先处理。
          </div>
        </div>
        <el-tag :type="dailyCloseStatusType" effect="dark" data-role-matrix-daily-close-status>
          {{ dailyCloseStatusText }}
        </el-tag>
      </div>
      <div class="team-leader-workbench__daily-close-grid" data-role-matrix-daily-close-summary>
        <el-card
          v-for="item in dailyCloseSummaryCards"
          :key="item.key"
          shadow="never"
          class="team-leader-workbench__daily-close-card"
          :data-role-matrix-daily-close-card="item.key"
        >
          <div class="team-leader-workbench__daily-close-label">{{ item.label }}</div>
          <div class="team-leader-workbench__daily-close-value">{{ item.value }}</div>
          <div class="team-leader-workbench__daily-close-hint">{{ item.hint }}</div>
        </el-card>
      </div>
      <el-alert
        v-if="loadError"
        :title="`日结阻塞：${loadError}`"
        type="error"
        :closable="false"
        show-icon
      />
      <el-alert
        v-else-if="dailyCloseOpenItemCount > 0"
        :title="`日结前仍有 ${dailyCloseOpenItemCount} 项待处理，请先完成复核或异常闭环。`"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-alert
        v-else
        title="当前筛选范围没有未关闭项，可进入后续日结核对。"
        type="success"
        :closable="false"
        show-icon
      />
    </ContentWrap>

    <ContentWrap
      v-if="showProductionExceptionModule"
      :class="{ 'team-leader-workbench__production-module-card': showProductionModuleTabs }"
      data-team-leader-abnormal-report
    >
      <el-tabs
        v-if="showProductionModuleTabs"
        v-model="activeProductionModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
        data-production-leader-module-tabs
      >
        <el-tab-pane label="人员管理" name="personnel" data-production-leader-module-tab-personnel />
        <el-tab-pane label="报工管理" name="report" data-production-leader-module-tab-report />
        <el-tab-pane label="活跃订单池" name="activeOrder" data-production-leader-module-tab-active-order />
        <el-tab-pane label="看板" name="dashboard" data-production-leader-module-tab-dashboard />
        <el-tab-pane label="异常" name="exception" data-production-leader-module-tab-exception />
        <el-tab-pane label="工序配置" name="processConfig" data-production-leader-module-tab-process-config />
        <el-tab-pane label="班组配置" name="config" data-production-leader-module-tab-config />
      </el-tabs>
      <div
        v-if="showProductionModuleTabs"
        class="team-leader-workbench__responsible-routes"
        data-production-leader-responsible-routes
        aria-label="生产组长负责的工艺路线"
      >
        <span class="team-leader-workbench__responsible-routes-label">负责工艺路线</span>
        <template v-if="productionResponsibleRouteNames.length">
          <el-tag
            v-for="routeName in productionResponsibleRouteNames"
            :key="routeName"
            class="team-leader-workbench__responsible-route-tag"
            type="success"
            effect="plain"
            :title="routeName"
          >
            {{ routeName }}
          </el-tag>
        </template>
        <span v-else class="team-leader-workbench__responsible-routes-empty">
          {{ processConfigLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
        </span>
      </div>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">订单异常上报</div>
          <div class="team-leader-workbench__hint">
            选择订单号并填写异常说明即可完成上报。
          </div>
        </div>
      </div>
      <el-form
        ref="abnormalFormRef"
        :model="abnormalForm"
        :rules="abnormalRules"
        label-width="120px"
        class="team-leader-workbench__form"
      >
        <el-form-item label="订单号" prop="activeOrderId" data-team-leader-active-order-select>
          <el-select
            v-model="abnormalForm.activeOrderId"
            filterable
            placeholder="请选择订单号"
            @change="handleAbnormalActiveOrderChange"
          >
            <el-option
              v-for="order in activeOrderOptions"
              :key="order.id"
              :label="formatActiveOrderOption(order)"
              :value="order.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="异常原因" prop="abnormalReasonCode">
          <el-select
            v-model="abnormalForm.abnormalReasonCode"
            filterable
            placeholder="请选择已配置的异常原因"
            data-team-leader-abnormal-reason-select
          >
            <el-option
              v-for="reason in configuredDefectReasonOptions"
              :key="reason.reasonCode"
              :label="`${reason.reasonName}（${reason.reasonCode}）`"
              :value="reason.reasonCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="异常说明" prop="abnormalDescription">
          <el-input
            v-model="abnormalForm.abnormalDescription"
            type="textarea"
            :rows="4"
            placeholder="请输入异常说明"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="abnormalSubmitting" @click="submitAbnormal">
            <Icon icon="ep:warning-filled" class="mr-5px" />
            标记并上报
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>


    <ContentWrap
      v-if="showProductionProcessConfigModule"
      :class="{ 'team-leader-workbench__production-module-card': showProductionModuleTabs }"
      data-team-leader-process-config-tab
    >
      <el-tabs
        v-if="showProductionModuleTabs"
        v-model="activeProductionModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
        data-production-leader-module-tabs
      >
        <el-tab-pane label="人员管理" name="personnel" data-production-leader-module-tab-personnel />
        <el-tab-pane label="报工管理" name="report" data-production-leader-module-tab-report />
        <el-tab-pane label="活跃订单池" name="activeOrder" data-production-leader-module-tab-active-order />
        <el-tab-pane label="看板" name="dashboard" data-production-leader-module-tab-dashboard />
        <el-tab-pane label="异常" name="exception" data-production-leader-module-tab-exception />
        <el-tab-pane label="工序配置" name="processConfig" data-production-leader-module-tab-process-config />
        <el-tab-pane label="班组配置" name="config" data-production-leader-module-tab-config />
      </el-tabs>
      <div
        v-if="showProductionModuleTabs"
        class="team-leader-workbench__responsible-routes"
        data-production-leader-responsible-routes
        aria-label="生产组长负责的工艺路线"
      >
        <span class="team-leader-workbench__responsible-routes-label">负责工艺路线</span>
        <template v-if="productionResponsibleRouteNames.length">
          <el-tag
            v-for="routeName in productionResponsibleRouteNames"
            :key="routeName"
            class="team-leader-workbench__responsible-route-tag"
            type="success"
            effect="plain"
            :title="routeName"
          >
            {{ routeName }}
          </el-tag>
        </template>
        <span v-else class="team-leader-workbench__responsible-routes-empty">
          {{ processConfigLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
        </span>
      </div>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">工序配置</div>
          <div class="team-leader-workbench__hint">
            以路线工序串联损耗原因、设备映射和设备参数标准；平均值来自近 30 天正式报工，只读展示。
          </div>
        </div>
        <el-button
          type="primary"
          :loading="processConfigLoading"
          data-team-leader-process-config-create-entry
          @click="openCreateProcessConfigDataDialog"
        >
          新增
        </el-button>
      </div>
      <el-table
        v-loading="processConfigLoading"
        :data="processConfigRows"
        :row-key="(row) => String(row.routeProcessId)"
        border
        stripe
        data-team-leader-process-config-table
      >
        <el-table-column label="工艺路线" min-width="180">
          <template #default="{ row }">
            {{ row.routeName || row.routeCode || row.routeId || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="工序" min-width="180">
          <template #default="{ row }">
            <span data-team-leader-process-config-row-key>
              {{ formatProcessConfigProcess(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="损耗原因" min-width="260">
          <template #default="{ row }">
            <div class="team-leader-workbench__loss-reasons" data-team-leader-process-config-loss-reasons>
              <el-tag
                v-for="reason in row.lossReasons"
                :key="reason.id"
                :type="reason.enabled ? 'success' : 'info'"
                effect="plain"
              >
                {{ reason.reasonCode }} / {{ reason.reasonName }}{{ reason.enabled ? '' : '（停用）' }}
              </el-tag>
              <span v-if="!row.lossReasons?.length" class="team-leader-workbench__hint">暂无损耗原因</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="映射设备" min-width="280">
          <template #default="{ row }">
            <div class="team-leader-workbench__process-config-devices" data-team-leader-process-config-devices>
              <el-tag
                v-for="device in row.devices"
                :key="device.deviceId"
                type="success"
                effect="plain"
              >
                {{ formatProcessConfigDevice(device) }}
              </el-tag>
              <span v-if="!row.devices?.length" class="team-leader-workbench__hint">未映射设备</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="设备参数标准" min-width="360">
          <template #default="{ row }">
            <div class="team-leader-workbench__process-config-parameters" data-team-leader-process-config-parameters>
              <template v-for="device in row.devices" :key="`params-${device.deviceId}`">
                <div
                  v-for="parameter in device.parameters"
                  :key="`${device.deviceId}-${parameter.parameterCode}`"
                  class="team-leader-workbench__process-config-parameter"
                >
                  <span class="team-leader-workbench__process-config-parameter-name">
                    {{ parameter.parameterName || parameter.parameterCode }}
                  </span>
                  <span>
                    下限 {{ parameter.lowerLimit }} / 目标 {{ parameter.targetValue }} / 上限 {{ parameter.upperLimit }}
                    {{ parameter.unit || '' }}
                  </span>
                  <span>平均 {{ formatProcessConfigAverage(parameter) }}</span>
                  <span>样本 {{ parameter.sampleCount ?? 0 }}</span>
                  <span>
                    {{ formatProcessConfigStatisticsWindow(parameter) }}
                  </span>
                </div>
              </template>
              <span
                v-if="!hasProcessConfigParameters(row)"
                class="team-leader-workbench__hint"
              >
                暂无参数标准
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作面板" width="360" fixed="right">
          <template #default="{ row }">
            <div class="team-leader-workbench__process-config-actions">
              <el-button
                link
                type="primary"
                data-team-leader-process-config-add-loss
                @click="openCreateLossReason(row)"
              >
                新增损耗
              </el-button>
              <el-button
                v-for="reason in row.lossReasons"
                :key="`edit-${reason.id}`"
                link
                type="warning"
                @click="openEditLossReason(row, reason)"
              >
                修改损耗
              </el-button>
              <el-button
                v-for="reason in row.lossReasons"
                :key="`delete-${reason.id}`"
                link
                type="danger"
                @click="handleDeleteLossReason(reason)"
              >
                删除损耗
              </el-button>
              <el-button
                link
                type="primary"
                data-team-leader-process-config-bind-device
                @click="openProcessConfigDeviceDialog(row)"
              >
                映射设备
              </el-button>
              <el-button
                v-for="device in row.devices"
                :key="`parameter-${device.deviceId}`"
                link
                type="primary"
                data-team-leader-process-config-edit-parameter
                @click="openProcessConfigParameterDialog(row, device)"
              >
                参数标准
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog
        v-model="processConfigCreateDialogVisible"
        title="新增工序配置数据"
        width="560px"
        destroy-on-close
        data-team-leader-process-config-create-dialog
      >
        <el-form :model="processConfigCreateForm" label-width="108px">
          <el-form-item label="路线工序" required>
            <el-select
              v-model="processConfigCreateForm.routeProcessId"
              filterable
              placeholder="请选择路线工序"
              data-team-leader-process-config-create-process
              @change="handleProcessConfigCreateRouteChange"
            >
              <el-option
                v-for="row in processConfigRows"
                :key="row.routeProcessId"
                :label="formatProcessConfigCreateProcessOption(row)"
                :value="row.routeProcessId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="新增类型" required>
            <el-radio-group
              v-model="processConfigCreateForm.createType"
              data-team-leader-process-config-create-type
              @change="handleProcessConfigCreateTypeChange"
            >
              <el-radio-button label="LOSS_REASON">损耗原因</el-radio-button>
              <el-radio-button label="DEVICE_BINDING">设备映射</el-radio-button>
              <el-radio-button label="PARAMETER_RULE">设备参数标准</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            v-if="processConfigCreateForm.createType === 'PARAMETER_RULE'"
            label="设备"
            required
          >
            <el-select
              v-model="processConfigCreateForm.deviceId"
              filterable
              placeholder="请选择当前工序已映射设备"
              data-team-leader-process-config-create-device
            >
              <el-option
                v-for="device in processConfigCreateDeviceOptions"
                :key="device.deviceId"
                :label="formatProcessConfigDevice(device)"
                :value="device.deviceId"
              />
            </el-select>
          </el-form-item>
          <el-alert
            title="选择后将打开对应维护弹窗；保存时继续使用正式损耗、设备映射和设备参数接口。"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form>
        <template #footer>
          <el-button @click="processConfigCreateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmCreateProcessConfigData">
            下一步
          </el-button>
        </template>
      </el-dialog>
    </ContentWrap>
    <ContentWrap
      v-if="showProductionConfigModule"
      :class="{ 'team-leader-workbench__production-module-card': showProductionModuleTabs }"
      data-team-leader-config-center
    >
      <el-tabs
        v-if="showProductionModuleTabs"
        v-model="activeProductionModuleTab"
        class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"
        data-production-leader-module-tabs
      >
        <el-tab-pane label="人员管理" name="personnel" data-production-leader-module-tab-personnel />
        <el-tab-pane label="报工管理" name="report" data-production-leader-module-tab-report />
        <el-tab-pane label="活跃订单池" name="activeOrder" data-production-leader-module-tab-active-order />
        <el-tab-pane label="看板" name="dashboard" data-production-leader-module-tab-dashboard />
        <el-tab-pane label="异常" name="exception" data-production-leader-module-tab-exception />
        <el-tab-pane label="工序配置" name="processConfig" data-production-leader-module-tab-process-config />
        <el-tab-pane label="班组配置" name="config" data-production-leader-module-tab-config />
      </el-tabs>
      <div
        v-if="showProductionModuleTabs"
        class="team-leader-workbench__responsible-routes"
        data-production-leader-responsible-routes
        aria-label="生产组长负责的工艺路线"
      >
        <span class="team-leader-workbench__responsible-routes-label">负责工艺路线</span>
        <template v-if="productionResponsibleRouteNames.length">
          <el-tag
            v-for="routeName in productionResponsibleRouteNames"
            :key="routeName"
            class="team-leader-workbench__responsible-route-tag"
            type="success"
            effect="plain"
            :title="routeName"
          >
            {{ routeName }}
          </el-tag>
        </template>
        <span v-else class="team-leader-workbench__responsible-routes-empty">
          {{ processConfigLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
        </span>
      </div>
      <div class="team-leader-workbench__section-head">
        <div>
          <div class="team-leader-workbench__section-title">班组配置中心</div>
          <div class="team-leader-workbench__hint">
            维护员工、设备、参数和工序关系，员工端填报从这里读取配置。
          </div>
        </div>
      </div>
      <div class="team-leader-workbench__maintenance-grid">
        <el-card shadow="never" data-team-leader-employee-config>
          <template #header>生产人员工序绑定</template>
          <el-alert
            title="员工档案请在上方生产人员档案 tab 维护；这里仅把已关联当前组长的生产人员档案绑定到工序。"
            type="info"
            :closable="false"
            show-icon
          />
          <el-form :model="processEmployeeBindingForm" label-width="108px">
            <el-form-item label="工序ID">
              <el-input-number
                v-model="processEmployeeBindingForm.processId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item label="员工档案ID">
              <el-input-number
                v-model="processEmployeeBindingForm.employeeProfileId"
                :min="1"
                :controls="false"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="maintenanceSubmitting"
                @click="submitProcessEmployeeBinding"
              >
                绑定工序员工
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" data-team-leader-device-config>
          <template #header>设备档案与状态</template>
          <el-form :model="teamDeviceForm" label-width="98px">
            <el-form-item label="设备编号">
              <el-input v-model="teamDeviceForm.deviceCode" />
            </el-form-item>
            <el-form-item label="设备名称">
              <el-input v-model="teamDeviceForm.deviceName" />
            </el-form-item>
            <el-form-item label="设备状态">
              <el-select v-model="teamDeviceForm.deviceStatus">
                <el-option label="启用" value="ENABLED" />
                <el-option label="报修" value="REPAIRING" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="maintenanceSubmitting" @click="submitTeamDevice">
                新增设备
              </el-button>
            </el-form-item>
          </el-form>
          <el-divider />
          <el-form :model="teamDeviceStatusForm" label-width="98px">
            <el-form-item label="设备ID">
              <el-input-number v-model="teamDeviceStatusForm.deviceId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="teamDeviceStatusForm.deviceStatus">
                <el-option label="启用" value="ENABLED" />
                <el-option label="报修" value="REPAIRING" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="maintenanceSubmitting" @click="submitTeamDeviceStatus">
                更新状态
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" data-team-leader-process-relation-config>
          <template #header>工序异常关系</template>
          <el-alert
            title="设备映射与设备参数标准已合并到“工序配置”统一表维护。"
            type="info"
            :closable="false"
            show-icon
          />
          <el-form :model="defectReasonForm" label-width="108px">
            <el-form-item label="工序ID">
              <el-input-number v-model="defectReasonForm.processId" :min="1" :controls="false" />
            </el-form-item>
            <el-form-item label="原因类型">
              <el-select v-model="defectReasonForm.reasonType" data-team-leader-defect-reason-select>
                <el-option label="不合格" value="UNQUALIFIED" />
                <el-option label="PQC 失败" value="PQC_FAILURE" />
              </el-select>
            </el-form-item>
            <el-form-item label="原因编码">
              <el-input v-model="defectReasonForm.reasonCode" />
            </el-form-item>
            <el-form-item label="原因名称">
              <el-input v-model="defectReasonForm.reasonName" />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="maintenanceSubmitting"
                @click="submitProcessDefectReason"
              >
                保存工序异常原因
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </ContentWrap>

    <el-drawer
      v-if="!showPqcDetailAsTab"
      v-model="detailVisible"
      :title="detailDrawerTitle"
      size="1240px"
      destroy-on-close
      data-team-leader-submission-detail-drawer
    >
      <div v-loading="detailLoading">
        <el-descriptions
          v-if="detail"
          :column="1"
          border
          class="team-leader-workbench__detail-descriptions"
          label-width="400px"
          data-team-leader-structured-detail
        >
          <el-descriptions-item label="服务端提交时间">
            {{ formatDateTime(detail.submittedAt) }}
          </el-descriptions-item>
          <el-descriptions-item :label="employeeDetailLabel">
            {{ detail.actualEmployeeUserName || detail.actualEmployeeUserId || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="工序">
            {{ detail.processName || detail.processCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="生产工单">
            {{ detail.workOrderCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="提交摘要">
            {{ detail.submittedSummary || '--' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.pqcResult || detail.pqcSummary" label="PQC检验内容">
            <el-tag :type="resolvePqcTagType(detail.pqcResult)" effect="plain">
              {{ detail.pqcSummary || detail.pqcResult }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail && isPqcSubmissionRow(detail)" label="PQC项目明细">
            <el-table
              :data="resolvePqcItemSnapshotDetails(detail)"
              border
              size="small"
              data-pqc-leader-item-snapshot-table
              empty-text="PQC提交内容缺少正式项目明细"
            >
              <el-table-column label="检验项目" min-width="120">
                <template #default="{ row }">{{ row.itemName || row.itemCode || '--' }}</template>
              </el-table-column>
              <el-table-column label="检验设备" min-width="140">
                <template #default="{ row }">
                  {{ row.selectedEquipmentName || row.selectedEquipmentCode || '--' }}
                </template>
              </el-table-column>
              <el-table-column label="设备编号" prop="selectedEquipmentNumber" min-width="130" />
              <el-table-column label="接收标准" min-width="180">
                <template #default="{ row }">{{ formatPqcSnapshotStandard(row) }}</template>
              </el-table-column>
              <el-table-column label="检验方法" prop="inspectionMethod" min-width="180" />
              <el-table-column label="样本值" min-width="180">
                <template #default="{ row }">
                  <span data-pqc-leader-detail-sample-values>
                    {{ formatPqcSnapshotSampleValues(row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="判定" min-width="100">
                <template #default="{ row }">{{ row.judgement || row.itemResult || '--' }}</template>
              </el-table-column>
            </el-table>
          </el-descriptions-item>
        </el-descriptions>
        <div
          v-if="detail && isPqcSubmissionRow(detail)"
          class="team-leader-workbench__submission-log"
          data-pqc-submission-log
        >
          <div class="team-leader-workbench__submission-log-title">PQC提交日志</div>
          <el-descriptions
            :column="1"
            border
            class="team-leader-workbench__detail-descriptions"
            label-width="400px"
          >
            <el-descriptions-item label="提交事件编号">
              {{ detail.id || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="PQC检验员">
              {{ detail.actualEmployeeUserName || detail.actualEmployeeUserId || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="服务端提交时间">
              {{ formatDateTime(detail.submittedAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="签名编号">
              <span data-pqc-submission-signature-id>
                {{ detail.electronicSignatureId || '--' }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-drawer>

    <el-dialog
      v-model="lossReasonDialogVisible"
      :title="lossReasonDialogTitle"
      width="560px"
      destroy-on-close
      data-loss-reason-edit-dialog
    >
      <el-form :model="lossReasonForm" label-width="108px">
        <el-form-item label="工艺路线">
          <span>{{ lossReasonEditingRow?.routeName || lossReasonEditingRow?.routeCode || '--' }}</span>
        </el-form-item>
        <el-form-item label="工序">
          <span>{{ lossReasonEditingRow ? formatProcessConfigProcess(lossReasonEditingRow) : '--' }}</span>
        </el-form-item>
        <el-form-item label="原因编码" required>
          <el-input
            v-model="lossReasonForm.reasonCode"
            :disabled="lossReasonDialogMode === 'edit'"
            maxlength="64"
            placeholder="请输入损耗原因编码"
          />
        </el-form-item>
        <el-form-item label="原因名称" required>
          <el-input
            v-model="lossReasonForm.reasonName"
            maxlength="255"
            placeholder="请输入损耗原因名称"
          />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch
            v-model="lossReasonForm.enabled"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item label="维护说明">
          <el-input
            v-model="lossReasonForm.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="记录新增、修改或删除原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lossReasonDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="lossReasonSubmitting"
          @click="submitLossReason"
        >
          保存损耗原因
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="processConfigDeviceDialogVisible"
      title="映射工序设备"
      width="520px"
      destroy-on-close
      data-team-leader-process-config-device-dialog
    >
      <el-form :model="processConfigDeviceForm" label-width="108px">
        <el-form-item label="工艺路线">
          <span>{{ processConfigSelectedRow?.routeName || processConfigSelectedRow?.routeCode || '--' }}</span>
        </el-form-item>
        <el-form-item label="工序">
          <span>{{ processConfigSelectedRow ? formatProcessConfigProcess(processConfigSelectedRow) : '--' }}</span>
        </el-form-item>
        <el-form-item label="设备" required>
          <el-select
            v-model="processConfigDeviceForm.deviceId"
            filterable
            placeholder="请选择当前组长设备"
            data-team-leader-process-config-device-select
          >
            <el-option
              v-for="device in processConfigDeviceOptions"
              :key="device.deviceId"
              :label="formatProcessConfigDevice(device)"
              :value="device.deviceId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processConfigDeviceDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="processConfigSubmitting"
          @click="submitProcessConfigDeviceBinding"
        >
          保存设备映射
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="processConfigParameterDialogVisible"
      title="维护设备参数标准"
      width="620px"
      destroy-on-close
      data-team-leader-process-config-parameter-dialog
    >
      <el-form :model="processConfigParameterForm" label-width="108px">
        <el-form-item label="工艺路线">
          <span>{{ processConfigSelectedRow?.routeName || processConfigSelectedRow?.routeCode || '--' }}</span>
        </el-form-item>
        <el-form-item label="工序">
          <span>{{ processConfigSelectedRow ? formatProcessConfigProcess(processConfigSelectedRow) : '--' }}</span>
        </el-form-item>
        <el-form-item label="设备">
          <span>{{ processConfigSelectedDevice ? formatProcessConfigDevice(processConfigSelectedDevice) : '--' }}</span>
        </el-form-item>
        <el-form-item label="参数编码" required>
          <el-input
            v-model="processConfigParameterForm.parameterCode"
            maxlength="64"
            placeholder="请输入参数编码"
            data-team-leader-process-config-parameter-code
          />
        </el-form-item>
        <el-form-item label="参数名称">
          <el-input v-model="processConfigParameterForm.parameterName" maxlength="128" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="processConfigParameterForm.unit" maxlength="32" />
        </el-form-item>
        <el-form-item label="值类型" required>
          <el-select v-model="processConfigParameterForm.valueType">
            <el-option label="数值" value="DECIMAL" />
            <el-option label="整数" value="INTEGER" />
          </el-select>
        </el-form-item>
        <el-form-item label="下限" required>
          <el-input-number
            v-model="processConfigParameterForm.lowerLimit"
            :controls="false"
            data-team-leader-process-config-lower-limit
          />
        </el-form-item>
        <el-form-item label="目标值" required>
          <el-input-number
            v-model="processConfigParameterForm.targetValue"
            :controls="false"
            data-team-leader-process-config-target-value
          />
        </el-form-item>
        <el-form-item label="上限" required>
          <el-input-number
            v-model="processConfigParameterForm.upperLimit"
            :controls="false"
            data-team-leader-process-config-upper-limit
          />
        </el-form-item>
        <el-form-item label="实际平均值">
          <span data-team-leader-process-config-average-readonly>
            {{ processConfigEditingParameter ? formatProcessConfigAverage(processConfigEditingParameter) : '暂无样本' }}
          </span>
        </el-form-item>
        <el-form-item label="样本数">
          <span>{{ processConfigEditingParameter?.sampleCount ?? 0 }}</span>
        </el-form-item>
        <el-form-item label="统计周期">
          <span>
            {{
              processConfigEditingParameter
                ? formatProcessConfigStatisticsWindow(processConfigEditingParameter)
                : '--'
            }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processConfigParameterDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="processConfigSubmitting"
          @click="submitProcessConfigParameterRule"
        >
          保存参数标准
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="复核员工提交" width="760px">
      <el-form :model="reviewForm" label-width="92px">
        <el-form-item label="判定结果">
          <el-select v-model="reviewForm.reviewStatus">
            <el-option label="正确" value="APPROVED" />
            <el-option label="不正确" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="复核说明">
          <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="复核签名ID" data-team-leader-review-signature>
          <el-input-number
            v-model="reviewForm.reviewSignatureId"
            :min="1"
            :controls="false"
            class="team-leader-workbench__number"
          />
        </el-form-item>
        <el-form-item label="签名员工ID">
          <el-input-number
            v-model="reviewForm.reviewSignatureEmployeeUserId"
            :min="1"
            :controls="false"
            class="team-leader-workbench__number"
          />
        </el-form-item>
        <el-form-item label="签名快照">
          <el-input
            v-model="reviewForm.reviewSignatureSnapshotJson"
            type="textarea"
            :rows="3"
            resize="vertical"
            placeholder="请输入电子签名快照 JSON 或签名服务返回引用"
          />
        </el-form-item>
      </el-form>
      <div
        v-if="isProductionLeader && reviewForm.reviewStatus === 'APPROVED'"
        class="team-leader-workbench__allocation"
      >
        <div class="team-leader-workbench__allocation-toolbar">
          <div>
            <div class="team-leader-workbench__section-title">活跃订单分配</div>
            <div class="team-leader-workbench__hint">
              可先按 FIFO 自动分配，再根据现场情况手动调整。
            </div>
          </div>
          <div>
            <el-button
              data-team-leader-fifo-allocation
              type="primary"
              plain
              :loading="allocationPreviewLoading"
              @click="previewFifoAllocation"
            >
              FIFO 自动分配
            </el-button>
            <el-button @click="addAllocationLine">新增分配行</el-button>
          </div>
        </div>
        <el-table
          data-team-leader-allocation-table
          :data="allocationRows"
          border
          size="small"
          empty-text="请点击 FIFO 自动分配或手动新增分配行"
        >
          <el-table-column label="活跃订单" min-width="220">
            <template #default="{ row }">
              <el-select
                v-model="row.activeOrderId"
                filterable
                placeholder="请选择活跃订单"
                @change="markManualAllocation"
              >
                <el-option
                  v-for="order in activeOrderOptions"
                  :key="order.id"
                  :label="formatActiveOrderOption(order)"
                  :value="order.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="分配数量" width="180">
            <template #default="{ row }">
              <el-input-number
                v-model="row.allocatedQuantity"
                :min="0"
                :precision="3"
                :controls="false"
                class="!w-140px"
                @change="markManualAllocation"
              />
            </template>
          </el-table-column>
          <el-table-column label="FIFO 剩余" width="140">
            <template #default="{ row }">
              {{ row.remainingQuantityBeforeAllocation ?? '--' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeAllocationLine($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="team-leader-workbench__hint mt-8px">
          当前分配模式：{{ reviewForm.allocationMode }}
        </div>
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview"
          >提交复核</el-button
        >
      </template>
    </el-dialog>

    <el-dialog v-model="correctionVisible" title="修正不正确内容" width="760px" destroy-on-close>
      <el-alert
        title="修正将调用原始记录修改接口，系统会记录修改前、修改后、原因、修改人、签名和字段差异日志。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form class="team-leader-workbench__correction-form" :model="correctionForm" label-width="150px">
        <el-form-item label="提交事件编号">
          <el-input-number
            v-model="correctionForm.eventId"
            :min="1"
            :controls="false"
            disabled
            class="team-leader-workbench__number"
          />
        </el-form-item>
        <el-form-item label="修改原因">
          <el-input v-model="correctionForm.changeReason" maxlength="500" show-word-limit />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :md="8">
            <el-form-item label="修改人用户ID">
              <el-input-number
                v-model="correctionForm.modifiedByUserId"
                :min="1"
                :controls="false"
                class="team-leader-workbench__number"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="修正签名ID">
              <el-input-number
                v-model="correctionForm.revisionSignatureId"
                :min="1"
                :controls="false"
                class="team-leader-workbench__number"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="签名用户ID">
              <el-input-number
                v-model="correctionForm.revisionSignatureUserId"
                :min="1"
                :controls="false"
                class="team-leader-workbench__number"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="修改后payload JSON">
          <el-input v-model="correctionForm.afterPayloadJson" type="textarea" :rows="8" resize="vertical" />
        </el-form-item>
        <el-form-item label="修正签名快照JSON">
          <el-input
            v-model="correctionForm.revisionSignatureSnapshotJson"
            type="textarea"
            :rows="4"
            resize="vertical"
          />
        </el-form-item>
        <el-form-item label="字段变更JSON">
          <el-input
            v-model="correctionForm.changedFieldsJson"
            type="textarea"
            :rows="8"
            resize="vertical"
            placeholder="请输入非空数组，逐项记录 fieldCode/fieldName/beforeValue/afterValue/affectsQuantityFragment/originalField"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="correctionVisible = false">取消</el-button>
        <el-button type="primary" :loading="correctionSubmitting" @click="submitCorrection">
          提交修正并记录日志
        </el-button>
      </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableMultiFilter,
  type ListMultiFilterCondition,
  type ListMultiFilterDefinition
} from '@/hooks/web/useTableMultiFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition,
  type UserTableColumnState
} from '@/hooks/web/useUserTableColumns'
import {
  addTeamLeaderActiveOrder,
  confirmTeamLeaderReportAllocation,
  createTemporaryTeamEmployee,
  createTeamDevice,
  createTeamEmployeeProfile,
  createTeamLeaderLossReason,
  deleteTeamLeaderLossReason,
  getPqcPersonnelList,
  getTeamLeaderProcessConfigList,
  getProductionPersonnelList,
  getTeamLeaderActiveOrderList,
  getTeamLeaderActiveOrderTransferTrace,
  getTeamLeaderSubmissionDetail,
  getTeamLeaderSubmissionPage,
  linkPqcFormalEmployee,
  linkFormalTeamEmployee,
  markAndReportWorkOrderAbnormal,
  previewTeamLeaderReportFifoAllocation,
  removeTeamLeaderActiveOrder,
  resetTemporaryTeamEmployeeSignaturePassword,
  reviewTeamLeaderSubmission,
  saveTeamProcessDefectReason,
  saveTeamProcessConfigDeviceBinding,
  saveTeamProcessConfigDeviceParameterRule,
  saveTeamProcessEmployeeBinding,
  searchPqcFormalEmployeeCandidates,
  searchTeamLeaderActiveOrderCandidates,
  searchTeamFormalEmployeeCandidates,
  updateTeamLeaderLossReason,
  updateTeamDeviceStatus,
  updateTeamEmployeeDisplayName as updateTeamEmployeeDisplayNameRequest,
  updateTeamEmployeeStatus as updateTeamEmployeeStatusRequest,
  updatePqcPersonnelStatus,
  type TeamFormalEmployeeCandidateRespVO,
  type TeamLeaderActiveOrderCandidateRespVO,
  type TeamLeaderActiveOrderRespVO,
  type TeamLeaderActiveOrderTransferTraceRespVO,
  type TeamLeaderLossReasonVO,
  type TeamLeaderProcessConfigDeviceVO,
  type TeamLeaderProcessConfigParameterVO,
  type TeamLeaderProcessConfigRowRespVO,
  type TeamLeaderReportAllocationLine,
  type TeamLeaderSubmissionPageReqVO,
  type TeamLeaderType,
  type TeamPqcPersonnelRespVO,
  type TeamProductionEmployeeRespVO
} from '@/api/mes/pro/processpool/teamLeader'
import type {
  ProcessPoolTimelineDetailVO,
  ProcessPoolTimelineEventVO
} from '@/api/mes/pro/processpool'
import {
  updateProcessPoolOriginalRecord,
  type ProcessPoolEventRevisionFieldChangeVO
} from '@/api/mes/pro/processpool/eventRevision'
import { formatDateTimeValue, formatDate } from '@/utils/formatTime'

defineOptions({ name: 'MesProProcessPoolTeamLeaderWorkbench' })

type WorkbenchLeaderTab = TeamLeaderType
type ProcessConfigCreateType = 'LOSS_REASON' | 'DEVICE_BINDING' | 'PARAMETER_RULE'

const props = withDefaults(
  defineProps<{
    leaderType?: TeamLeaderType
    showLeaderTypeTabs?: boolean
    showPqcModuleTabs?: boolean
    showProductionModuleTabs?: boolean
    title?: string
    subtitle?: string
  }>(),
  {
    leaderType: 'PRODUCTION',
    showLeaderTypeTabs: false,
    showPqcModuleTabs: false,
    showProductionModuleTabs: false,
    title: '工序池班组长工作台',
    subtitle: '负责生产报工确认、活跃订单分配、异常上报和班组配置中心维护'
  }
)

const abnormalFormRef = ref()
const activeLeaderTab = ref<WorkbenchLeaderTab>(props.leaderType)
const activePqcModuleTab = ref<'personnel' | 'management' | 'dashboard' | 'detail'>('personnel')
const activeProductionModuleTab = ref<
  'personnel' | 'report' | 'activeOrder' | 'dashboard' | 'exception' | 'processConfig' | 'config'
>('personnel')

const DEFAULT_SUBMISSION_DATE_CONDITION_ID = 'submitDate'
const getDefaultSubmissionDate = () => formatDate(new Date(), 'YYYY-MM-DD')
const loading = ref(false)
const detailLoading = ref(false)
const reviewSubmitting = ref(false)
const allocationPreviewLoading = ref(false)
const abnormalSubmitting = ref(false)
const maintenanceSubmitting = ref(false)
const activeOrderLoading = ref(false)
const correctionSubmitting = ref(false)
const detailVisible = ref(false)
const reviewVisible = ref(false)
const correctionVisible = ref(false)
const activeOrderAddDialogVisible = ref(false)
const loadError = ref('')
const submissionTotal = ref(0)
const submissionList = ref<ProcessPoolTimelineEventVO[]>([])
const detail = ref<ProcessPoolTimelineDetailVO>()
const reviewEvent = ref<ProcessPoolTimelineEventVO>()
const correctionEvent = ref<ProcessPoolTimelineEventVO>()
const activeOrderOptions = ref<TeamLeaderActiveOrderRespVO[]>([])
const activeOrderCandidateOptions = ref<TeamLeaderActiveOrderCandidateRespVO[]>([])
const activeOrderSelectedCandidate = ref<TeamLeaderActiveOrderCandidateRespVO>()
const activeOrderCandidateKeyword = ref('')
const activeOrderCandidateLoading = ref(false)
const activeOrderCandidateError = ref('')
const activeOrderTransferTraceRows = ref<TeamLeaderActiveOrderTransferTraceRespVO[]>([])
const activeOrderTransferTraceLoading = ref(false)
const activeOrderTransferTraceError = ref('')
const processConfigRows = ref<TeamLeaderProcessConfigRowRespVO[]>([])
const processConfigLoading = ref(false)
const processConfigSubmitting = ref(false)
const processConfigCreateDialogVisible = ref(false)
const processConfigDeviceDialogVisible = ref(false)
const processConfigParameterDialogVisible = ref(false)
const processConfigSelectedRow = ref<TeamLeaderProcessConfigRowRespVO>()
const processConfigSelectedDevice = ref<TeamLeaderProcessConfigDeviceVO>()
const processConfigEditingParameter = ref<TeamLeaderProcessConfigParameterVO>()
const lossReasonSubmitting = ref(false)
const lossReasonDialogVisible = ref(false)
const lossReasonDialogMode = ref<'create' | 'edit'>('create')
const lossReasonEditingRow = ref<TeamLeaderProcessConfigRowRespVO>()
const lossReasonEditingReason = ref<TeamLeaderLossReasonVO>()
const allocationRows = ref<TeamLeaderReportAllocationLine[]>([])
const configuredDefectReasonOptions = ref<
  Array<{ reasonType: string; reasonCode: string; reasonName: string }>
>([])
const productionPersonnelActiveTab = ref('productionPersonnel')
const productionPersonnelAddDialogVisible = ref(false)
const productionPersonnelDialogError = ref('')
const PRODUCTION_PERSONNEL_DIALOG_ERROR_DURATION = 6000
let productionPersonnelDialogErrorTimer: ReturnType<typeof setTimeout> | undefined
const productionPersonnelLoading = ref(false)
const productionPersonnelSubmitting = ref(false)
const formalCandidateLoading = ref(false)
const productionPersonnelRows = ref<TeamProductionEmployeeRespVO[]>([])
const formalEmployeeCandidateOptions = ref<TeamFormalEmployeeCandidateRespVO[]>([])
const pqcPersonnelAddDialogVisible = ref(false)
const pqcPersonnelLoading = ref(false)
const pqcPersonnelSubmitting = ref(false)
const pqcCandidateLoading = ref(false)
const pqcPersonnelRows = ref<TeamPqcPersonnelRespVO[]>([])
const pqcCandidateOptions = ref<TeamFormalEmployeeCandidateRespVO[]>([])

const productionPersonnelQuery = reactive({
  pageNo: 1,
  pageSize: 10
})
const productionPersonnelFilterDefinitions: any[] = []
const productionPersonnelQuickFilterState = reactive({})
const productionPersonnelOperatorOptions: any[] = []
const productionPersonnelColumns: any[] = [
  { key: 'displayName', label: '显示名', visible: true },
  { key: 'employeeType', label: '来源', visible: true },
  { key: 'employeeCode', label: '员工编码', visible: true },
  { key: 'enabled', label: '状态', visible: true }
]
const activeOrderQuery = reactive({
  pageNo: 1,
  pageSize: 10
})
const activeOrderFilterDefinitions: any[] = []
const activeOrderQuickFilterState = reactive({})
const activeOrderOperatorOptions: any[] = []
const activeOrderColumns: any[] = [
  { key: 'id', label: '活跃池ID', visible: true },
  { key: 'workOrderId', label: '生产订单ID', visible: true },
  { key: 'routeId', label: '路线ID', visible: true },
  { key: 'routeVersionId', label: '路线版本ID', visible: true },
  { key: 'erpFixedQuantitySnapshot', label: 'ERP生产数量', visible: true },
  { key: 'activeStatus', label: '状态', visible: true },
  { key: 'joinedAt', label: '加入时间', visible: true }
]
const pqcPersonnelQuery = reactive({
  pageNo: 1,
  pageSize: 10
})
const pqcPersonnelFilterDefinitions: any[] = []
const pqcPersonnelQuickFilterState = reactive({})
const pqcPersonnelOperatorOptions: any[] = []
const pqcPersonnelColumns: any[] = [
  { key: 'displayName', label: 'PQC检验员', visible: true },
  { key: 'username', label: '账号', visible: true },
  { key: 'enabled', label: '状态', visible: true }
]
const pqcDetailQuery = reactive({
  pageNo: 1,
  pageSize: 10
})
const pqcDetailFilterDefinitions: any[] = []
const pqcDetailQuickFilterState = reactive({})
const pqcDetailOperatorOptions: any[] = []
const pqcDetailColumns: any[] = [
  { key: 'itemName', label: '检验项目', visible: true },
  { key: 'selectedEquipmentName', label: '检验设备', visible: true },
  { key: 'selectedEquipmentNumber', label: '设备编号', visible: true },
  { key: 'standardText', label: '接收标准', visible: true },
  { key: 'inspectionMethod', label: '检验方法', visible: true },
  { key: 'sampleValues', label: '样本值', visible: true },
  { key: 'judgement', label: '判定', visible: true }
]
const SUBMISSION_TABLE_KEY = 'mes.processPool.teamLeader.submissions'
const PRODUCTION_SUBMISSION_TABLE_KEY = `${SUBMISSION_TABLE_KEY}.production`
const PQC_SUBMISSION_TABLE_KEY = `${SUBMISSION_TABLE_KEY}.pqc`
const submissionQuickFilterDefinitions: any[] = []
const submissionQuickFilterState = reactive({})
const submissionOperatorOptions: any[] = []
const productionSubmissionDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'submittedAt', label: '提交时间', minWidth: 160 },
  { key: 'employeeUser', label: '员工', minWidth: 140 },
  { key: 'process', label: '工序', minWidth: 150 },
  { key: 'workOrder', label: '生产工单', minWidth: 160 },
  { key: 'completionQuantity', label: '完成数量', minWidth: 130 },
  { key: 'lossQuantity', label: '损耗数量', minWidth: 120 },
  { key: 'lossBreakdown', label: '损耗明细', minWidth: 210 },
  { key: 'selectedDevice', label: '选用设备', minWidth: 220 },
  { key: 'deviceParameterReadings', label: '设备参数', minWidth: 280 },
  { key: 'auditCopyStatus', label: '审核副本', minWidth: 130 },
  { key: 'submissionReviewStatus', label: '复核判定', minWidth: 190 },
  { key: 'operation', label: '操作', width: 270, hideable: false, business: false }
]
const pqcSubmissionDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'submittedAt', label: '提交时间', minWidth: 160 },
  { key: 'employeeUser', label: 'PQC检验员', minWidth: 140 },
  { key: 'process', label: '工序', minWidth: 150 },
  { key: 'workOrder', label: '生产工单', minWidth: 160 },
  { key: 'completionQuantity', label: '检验数量', minWidth: 130 },
  { key: 'lossQuantity', label: '损耗数量', minWidth: 120 },
  { key: 'lossBreakdown', label: '损耗明细', minWidth: 210 },
  { key: 'product', label: '产品', minWidth: 180 },
  { key: 'inspectionTask', label: '检验类型/轮次', minWidth: 150 },
  { key: 'inspectionItems', label: '检验项', minWidth: 190 },
  { key: 'equipmentSnapshot', label: '设备', minWidth: 220 },
  { key: 'selectedDevice', label: '选用设备', minWidth: 220 },
  { key: 'equipmentNumber', label: '设备编号', minWidth: 150 },
  { key: 'acceptanceStandard', label: '接收标准', minWidth: 220 },
  { key: 'inspectionMethod', label: '检验方法', minWidth: 180 },
  { key: 'inspectionJudgement', label: '检验判定', minWidth: 150 },
  { key: 'parameterSnapshot', label: '参数明细', minWidth: 280 },
  { key: 'deviceParameterReadings', label: '设备参数', minWidth: 280 },
  { key: 'defectDescription', label: '不良说明', minWidth: 180 },
  { key: 'operation', label: '操作', width: 270, hideable: false, business: false }
]
const productionSubmissionColumnControl = useUserTableColumns(
  PRODUCTION_SUBMISSION_TABLE_KEY,
  productionSubmissionDefaultColumns
)
const pqcSubmissionColumnControl = useUserTableColumns(
  PQC_SUBMISSION_TABLE_KEY,
  pqcSubmissionDefaultColumns
)
const activeSubmissionColumnControl = computed(() =>
  activeLeaderTab.value === 'PQC' ? pqcSubmissionColumnControl : productionSubmissionColumnControl
)
const submissionColumnSaving = computed(() => activeSubmissionColumnControl.value.saving.value)
const submissionColumns = computed<UserTableColumnState[]>(
  () => activeSubmissionColumnControl.value.columns.value
)
const isSubmissionColumnVisible = (key: string) =>
  submissionColumns.value.some((column) => column.key === key)
  && activeSubmissionColumnControl.value.isColumnVisible(key)
const getSubmissionColumnWidthString = (key: string, fallback?: number) =>
  activeSubmissionColumnControl.value.getColumnWidthString(key, fallback)
const getSubmissionColumnMinWidthString = (key: string, fallback?: number) =>
  activeSubmissionColumnControl.value.getColumnMinWidthString(key, fallback)
const handleSubmissionHeaderDragend = async (newWidth: number, oldWidth: number, column: any) => {
  await activeSubmissionColumnControl.value.handleHeaderDragend(newWidth, oldWidth, column)
}
const saveSubmissionColumnConfig = async () => {
  await activeSubmissionColumnControl.value.saveConfig()
}
const resetSubmissionColumnConfig = async () => {
  await activeSubmissionColumnControl.value.resetConfig()
}

const showLeaderTypeTabs = computed(() => props.showLeaderTypeTabs)
const showPqcModuleTabs = computed(
  () => props.showPqcModuleTabs && activeLeaderTab.value === 'PQC'
)
const showPqcDetailAsTab = computed(
  () => activeLeaderTab.value === 'PQC' && showPqcModuleTabs.value
)
const showProductionModuleTabs = computed(
  () => props.showProductionModuleTabs && activeLeaderTab.value === 'PRODUCTION'
)
const pageTitle = computed(() => props.title)
const pageSubtitle = computed(() => props.subtitle)
const isProductionLeader = computed(() => activeLeaderTab.value === 'PRODUCTION')
const showProductionPersonnelModule = computed(
  () => isProductionLeader.value && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'personnel')
)
const showProductionReportModule = computed(
  () => isProductionLeader.value && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'report')
)
const showProductionActiveOrderModule = computed(
  () =>
    isProductionLeader.value
    && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'activeOrder')
)
const showProductionDashboardModule = computed(
  () =>
    isProductionLeader.value && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'dashboard')
)
const showProductionExceptionModule = computed(
  () =>
    isProductionLeader.value && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'exception')
)
const showProductionProcessConfigModule = computed(
  () => isProductionLeader.value && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'processConfig')
)
const showProductionConfigModule = computed(
  () => isProductionLeader.value && (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'config')
)
const showPqcPersonnelModule = computed(
  () =>
    activeLeaderTab.value === 'PQC'
    && (!showPqcModuleTabs.value || activePqcModuleTab.value === 'personnel')
)
const showPqcManagementModule = computed(
  () =>
    showProductionReportModule.value ||
    (activeLeaderTab.value === 'PQC' && (!showPqcModuleTabs.value || activePqcModuleTab.value === 'management'))
)
const showPqcDashboardModule = computed(
  () =>
    showProductionDashboardModule.value ||
    (activeLeaderTab.value === 'PQC' && (!showPqcModuleTabs.value || activePqcModuleTab.value === 'dashboard'))
)
const showPqcDetailModule = computed(
  () => showPqcDetailAsTab.value && activePqcModuleTab.value === 'detail'
)
const employeeColumnLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员' : '员工'
)
const employeeDetailLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员' : '实际员工'
)
const completionQuantityColumnLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? '检验数量' : '完成数量'
)
const detailDrawerTitle = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员提交详情' : '员工提交详情'
)
const lossReasonDialogTitle = computed(() =>
  lossReasonDialogMode.value === 'create' ? '新增损耗原因' : '修改损耗原因'
)
const dailyClosePendingReviewCount = computed(
  () =>
    submissionList.value.filter(
      (row) => !row.submissionReviewStatus || row.submissionReviewStatus === 'PENDING'
    ).length
)
const dailyCloseRejectedCount = computed(
  () => submissionList.value.filter((row) => row.submissionReviewStatus === 'REJECTED').length
)
const dailyCloseOpenItemCount = computed(
  () => dailyClosePendingReviewCount.value + dailyCloseRejectedCount.value + (loadError.value ? 1 : 0)
)
const dailyCloseStatusType = computed(() =>
  loadError.value || dailyCloseOpenItemCount.value > 0 ? 'warning' : 'success'
)
const dailyCloseStatusText = computed(() => {
  if (loadError.value) return '加载阻塞'
  return dailyCloseOpenItemCount.value > 0 ? '待处理' : '可日结'
})
const dailyCloseSummaryCards = computed(() => [
  {
    key: 'pending-review',
    label: '待复核提交',
    value: dailyClosePendingReviewCount.value,
    hint: '来自当前筛选提交列表，未判定记录不得日结'
  },
  {
    key: 'rejected-review',
    label: '复核不正确',
    value: dailyCloseRejectedCount.value,
    hint: '复核退回后需先修正或重新确认'
  },
  {
    key: 'active-orders',
    label: '活跃订单',
    value: activeOrderOptions.value.length,
    hint: '来自活跃订单池，日结前需确认分配与异常状态'
  },
  {
    key: 'load-blocker',
    label: '加载阻塞',
    value: loadError.value ? 1 : 0,
    hint: loadError.value || '当前看板数据已加载'
  }
])
const productionPersonnelTotal = computed(() => productionPersonnelRows.value.length)
const pagedProductionPersonnelRows = computed(() => {
  const pageNo = Math.max(1, Number(productionPersonnelQuery.pageNo) || 1)
  const pageSize = Math.max(1, Number(productionPersonnelQuery.pageSize) || 10)
  const start = (pageNo - 1) * pageSize
  return productionPersonnelRows.value.slice(start, start + pageSize)
})
const activeOrderTotal = computed(() => activeOrderOptions.value.length)
const pagedActiveOrderRows = computed(() => {
  const pageNo = Math.max(1, Number(activeOrderQuery.pageNo) || 1)
  const pageSize = Math.max(1, Number(activeOrderQuery.pageSize) || 10)
  const start = (pageNo - 1) * pageSize
  return activeOrderOptions.value.slice(start, start + pageSize)
})
const pqcPersonnelTotal = computed(() => pqcPersonnelRows.value.length)
const pagedPqcPersonnelRows = computed(() => {
  const pageNo = Math.max(1, Number(pqcPersonnelQuery.pageNo) || 1)
  const pageSize = Math.max(1, Number(pqcPersonnelQuery.pageSize) || 10)
  const start = (pageNo - 1) * pageSize
  return pqcPersonnelRows.value.slice(start, start + pageSize)
})

const canReviewSubmission = (row: ProcessPoolTimelineEventVO) =>
  !row.submissionReviewStatus || row.submissionReviewStatus === 'PENDING'

const canCorrectSubmission = (row: ProcessPoolTimelineEventVO) =>
  row.submissionReviewStatus === 'REJECTED'

const queryParams = reactive<TeamLeaderSubmissionPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  leaderType: activeLeaderTab.value,
  submitDate: getDefaultSubmissionDate(),
  employeeUserId: undefined,
  processId: undefined,
  deviceId: undefined,
  templateType: undefined,
  workOrderId: undefined,
  workOrderCode: undefined,
  productId: undefined,
  productKeyword: undefined,
  inspectionType: undefined,
  roundNo: undefined,
  submissionReviewStatus: undefined
})

const submissionMultiFilterDefinitions = computed<ListMultiFilterDefinition[]>(() => {
  const baseDefinitions: ListMultiFilterDefinition[] = [
    {
      key: 'submitDate',
      label: '提交日期',
      type: 'date',
      queryParamKey: 'submitDate',
      placeholder: '请选择提交日期'
    },
    ...(activeLeaderTab.value === 'PQC'
      ? [
          {
            key: 'employeeUserId',
            label: 'PQC检验员',
            type: 'text' as const,
            queryParamKey: 'employeeUserId',
            operators: ['eq' as const],
            placeholder: '员工编号'
          }
        ]
      : [
          {
            key: 'employeeUserId',
            label: '员工',
            type: 'text' as const,
            queryParamKey: 'employeeUserId',
            operators: ['eq' as const],
            placeholder: '员工编号'
          }
        ]),
    {
      key: 'processId',
      label: '工序',
      type: 'text',
      queryParamKey: 'processId',
      operators: ['eq'],
      placeholder: '工序编号'
    },
    {
      key: 'templateType',
      label: '模板类型',
      type: 'select',
      queryParamKey: 'templateType',
      options: [
        { label: '生产简化模板', value: 'PRODUCTION_SIMPLIFIED' },
        { label: 'PQC 简化模板', value: 'PQC_SIMPLIFIED' }
      ],
      placeholder: '请选择模板'
    },
    {
      key: 'workOrderCode',
      label: '生产工单',
      type: 'text',
      queryParamKey: 'workOrderCode',
      placeholder: '工单编码'
    }
  ]

  if (activeLeaderTab.value !== 'PQC') {
    return baseDefinitions
  }

  return [
    ...baseDefinitions,
    {
      key: 'productKeyword',
      label: '产品',
      type: 'text',
      queryParamKey: 'productKeyword',
      placeholder: '产品编码/名称'
    },
    {
      key: 'inspectionType',
      label: '检验类型',
      type: 'select',
      queryParamKey: 'inspectionType',
      options: [
        { label: '首检', value: 'FIRST' },
        { label: '巡检', value: 'PATROL' },
        { label: '末检', value: 'FINAL' }
      ],
      placeholder: '检验类型'
    },
    {
      key: 'roundNo',
      label: '轮次',
      type: 'text',
      queryParamKey: 'roundNo',
      operators: ['eq'],
      placeholder: '轮次'
    },
    {
      key: 'submissionReviewStatus',
      label: '复核状态',
      type: 'select',
      queryParamKey: 'submissionReviewStatus',
      options: [
        { label: '待判定', value: 'PENDING' },
        { label: '正确', value: 'APPROVED' },
        { label: '不正确', value: 'REJECTED' }
      ],
      placeholder: '复核状态'
    }
  ]
})

const reviewForm = reactive({
  reviewStatus: 'APPROVED' as 'APPROVED' | 'REJECTED',
  allocationMode: 'FIFO' as 'FIFO' | 'MANUAL',
  reviewRemark: '',
  reviewSignatureId: undefined as number | undefined,
  reviewSignatureEmployeeUserId: undefined as number | undefined,
  reviewSignatureSnapshotJson: ''
})

const correctionForm = reactive({
  eventId: undefined as number | undefined,
  modifiedByUserId: undefined as number | undefined,
  revisionSignatureId: undefined as number | undefined,
  revisionSignatureUserId: undefined as number | undefined,
  changeReason: '',
  afterPayloadJson: '',
  revisionSignatureSnapshotJson: '',
  changedFieldsJson: ''
})

const abnormalForm = reactive({
  activeOrderId: undefined as number | undefined,
  workOrderId: undefined as number | undefined,
  abnormalReasonCode: '',
  abnormalDescription: ''
})

const activeOrderForm = reactive({
  workOrderId: undefined as number | undefined
})

const formalEmployeeForm = reactive({
  systemUserId: undefined as number | undefined,
  displayName: ''
})

const pqcPersonnelForm = reactive({
  systemUserId: undefined as number | undefined
})

const temporaryEmployeeForm = reactive({
  displayName: '',
  signaturePassword: ''
})

const processEmployeeBindingForm = reactive({
  processId: undefined as number | undefined,
  employeeProfileId: undefined as number | undefined
})

const teamDeviceForm = reactive({
  deviceCode: '',
  deviceName: '',
  deviceStatus: 'ENABLED' as 'ENABLED' | 'REPAIRING' | 'DISABLED'
})

const teamDeviceStatusForm = reactive({
  deviceId: undefined as number | undefined,
  deviceStatus: 'REPAIRING' as 'ENABLED' | 'REPAIRING' | 'DISABLED'
})

const defectReasonForm = reactive({
  processId: undefined as number | undefined,
  reasonType: 'UNQUALIFIED',
  reasonCode: '',
  reasonName: ''
})

const lossReasonForm = reactive({
  reasonCode: '',
  reasonName: '',
  enabled: true,
  remark: ''
})

const processConfigCreateForm = reactive({
  routeProcessId: undefined as number | undefined,
  createType: 'LOSS_REASON' as ProcessConfigCreateType,
  deviceId: undefined as number | undefined
})

const processConfigDeviceForm = reactive({
  deviceId: undefined as number | undefined
})

const processConfigParameterForm = reactive({
  deviceId: undefined as number | undefined,
  parameterCode: '',
  parameterName: '',
  unit: '',
  lowerLimit: undefined as number | undefined,
  targetValue: undefined as number | undefined,
  upperLimit: undefined as number | undefined,
  valueType: 'DECIMAL'
})

const abnormalRules = {
  activeOrderId: [{ required: true, message: '订单号不能为空', trigger: 'change' }],
  abnormalReasonCode: [{ required: true, message: '异常原因不能为空', trigger: 'change' }],
  abnormalDescription: [{ required: true, message: '异常说明不能为空', trigger: 'blur' }]
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage =
    (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const normalizePositiveNumber = (value?: unknown) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const requirePositiveNumber = (value: unknown, message: string) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(message)
  }
  return parsed
}

const requireFiniteNumber = (value: unknown, message: string) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    throw new Error(message)
  }
  return parsed
}

const formatActiveOrderOption = (order: TeamLeaderActiveOrderRespVO) => {
  return `订单 ${order.workOrderId} / 活跃池 ${order.id}`
}

const resolveActiveOrderStatusText = (status?: string) => {
  if (status === 'ACTIVE') return '活跃'
  return status || '--'
}

const resolveActiveOrderStatusType = (status?: string) =>
  status === 'ACTIVE' ? 'success' : 'warning'

const formatTraceQuantity = (value: number | string | undefined) => {
  if (value === undefined || value === null || value === '') return '-'
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed.toFixed(3) : String(value)
}

const formatEmployeeType = (employeeType?: string) => {
  if (employeeType === 'TEMPORARY') return '临时工'
  if (employeeType === 'FORMAL') return '正式工'
  return employeeType || '--'
}

const formatSignaturePasswordManager = (row: TeamProductionEmployeeRespVO) => {
  if (row.employeeType === 'TEMPORARY') return '临时工档案密码'
  if (row.employeeType === 'FORMAL') return '原账号电子签名密码'
  return row.signaturePasswordManagedBy || '--'
}

const refreshPqcPersonnel = async () => {
  pqcPersonnelLoading.value = true
  try {
    pqcPersonnelRows.value = await getPqcPersonnelList()
    const maxPage = Math.max(1, Math.ceil(pqcPersonnelRows.value.length / pqcPersonnelQuery.pageSize))
    if (pqcPersonnelQuery.pageNo > maxPage) {
      pqcPersonnelQuery.pageNo = maxPage
    }
  } catch (error) {
    pqcPersonnelRows.value = []
    ElMessage.error(resolveErrorMessage(error, 'PQC 检验员列表加载失败'))
  } finally {
    pqcPersonnelLoading.value = false
  }
}

const handlePqcPersonnelPageChange = (page: number) => {
  pqcPersonnelQuery.pageNo = page
}

const handlePqcPersonnelPageSizeChange = (limit: number) => {
  pqcPersonnelQuery.pageSize = limit
  pqcPersonnelQuery.pageNo = 1
}

const searchPqcFormalEmployeeCandidatesForSelect = async (keyword: string) => {
  const searchText = keyword.trim()
  pqcCandidateLoading.value = true
  try {
    pqcCandidateOptions.value = await searchPqcFormalEmployeeCandidates(searchText)
  } catch (error) {
    pqcCandidateOptions.value = []
    ElMessage.error(resolveErrorMessage(error, 'PQC 检验员候选搜索失败'))
  } finally {
    pqcCandidateLoading.value = false
  }
}

const loadPqcFormalEmployeeCandidatesForSelect = async () => {
  await searchPqcFormalEmployeeCandidatesForSelect('')
}

const handlePqcCandidateDropdownVisibleChange = (visible: boolean) => {
  if (!visible) return
  void loadPqcFormalEmployeeCandidatesForSelect()
}

const submitLinkPqcFormalEmployee = async () => {
  pqcPersonnelSubmitting.value = true
  try {
    const systemUserId = requirePositiveNumber(pqcPersonnelForm.systemUserId, '请选择 PQC 检验员')
    const selectedCandidate = pqcCandidateOptions.value.find(
      (candidate) => candidate.systemUserId === systemUserId
    )
    if (selectedCandidate?.disabled) {
      throw new Error(selectedCandidate.disabledReason || '该 PQC 检验员当前不可选择')
    }
    await linkPqcFormalEmployee({
      systemUserId
    })
    pqcPersonnelForm.systemUserId = undefined
    pqcCandidateOptions.value = []
    pqcPersonnelAddDialogVisible.value = false
    ElMessage.success('PQC 检验员已关联当前组长')
    await refreshPqcPersonnel()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'PQC 检验员关联失败'))
  } finally {
    pqcPersonnelSubmitting.value = false
  }
}

const updatePqcInspectorStatus = async (row: TeamPqcPersonnelRespVO, enabled: boolean) => {
  try {
    await ElMessageBox.confirm(
      enabled
        ? '启用后，该检验员重新进入当前 PQC 组长的负责范围。'
        : '禁用后，该检验员不再进入当前 PQC 组长的负责范围。',
      enabled ? '启用 PQC 检验员' : '禁用 PQC 检验员',
      {
        type: enabled ? 'success' : 'warning',
        confirmButtonText: enabled ? '启用' : '禁用',
        cancelButtonText: '取消'
      }
    )
    await updatePqcPersonnelStatus({
      scopeId: requirePositiveNumber(row.scopeId, 'PQC 人员关联ID不能为空'),
      enabled
    })
    ElMessage.success(enabled ? 'PQC 检验员已启用' : 'PQC 检验员已禁用')
    await refreshPqcPersonnel()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, 'PQC 检验员状态更新失败'))
  }
}

const refreshProductionPersonnel = async () => {
  productionPersonnelLoading.value = true
  try {
    productionPersonnelRows.value = await getProductionPersonnelList()
    const maxPage = Math.max(1, Math.ceil(productionPersonnelRows.value.length / productionPersonnelQuery.pageSize))
    if (productionPersonnelQuery.pageNo > maxPage) {
      productionPersonnelQuery.pageNo = maxPage
    }
  } catch (error) {
    productionPersonnelRows.value = []
    ElMessage.error(resolveErrorMessage(error, '生产人员档案加载失败'))
  } finally {
    productionPersonnelLoading.value = false
  }
}

const handleProductionPersonnelPageChange = (page: number) => {
  productionPersonnelQuery.pageNo = page
}

const handleProductionPersonnelPageSizeChange = (limit: number) => {
  productionPersonnelQuery.pageSize = limit
  productionPersonnelQuery.pageNo = 1
}

const clearProductionPersonnelDialogError = () => {
  if (productionPersonnelDialogErrorTimer !== undefined) {
    clearTimeout(productionPersonnelDialogErrorTimer)
    productionPersonnelDialogErrorTimer = undefined
  }
  productionPersonnelDialogError.value = ''
}

const showProductionPersonnelDialogError = (message: string) => {
  clearProductionPersonnelDialogError()
  productionPersonnelDialogError.value = message
  productionPersonnelDialogErrorTimer = setTimeout(
    clearProductionPersonnelDialogError,
    PRODUCTION_PERSONNEL_DIALOG_ERROR_DURATION
  )
}

const searchFormalEmployeeCandidatesForSelect = async (keyword: string) => {
  const searchText = keyword.trim()
  if (!searchText) {
    formalEmployeeCandidateOptions.value = []
    return
  }
  formalCandidateLoading.value = true
  try {
    formalEmployeeCandidateOptions.value = await searchTeamFormalEmployeeCandidates(searchText)
  } catch (error) {
    formalEmployeeCandidateOptions.value = []
    ElMessage.error(resolveErrorMessage(error, '正式工候选搜索失败'))
  } finally {
    formalCandidateLoading.value = false
  }
}

const submitLinkFormalEmployee = async () => {
  productionPersonnelSubmitting.value = true
  try {
    await linkFormalTeamEmployee({
      systemUserId: requirePositiveNumber(formalEmployeeForm.systemUserId, '请选择正式工'),
      displayName: formalEmployeeForm.displayName.trim() || undefined
    })
    formalEmployeeForm.systemUserId = undefined
    formalEmployeeForm.displayName = ''
    formalEmployeeCandidateOptions.value = []
    ElMessage.success('正式工已关联当前生产组长')
    await refreshProductionPersonnel()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '正式工关联失败，请确认是否重名并按提示加后缀'))
  } finally {
    productionPersonnelSubmitting.value = false
  }
}

const submitCreateTemporaryEmployee = async () => {
  clearProductionPersonnelDialogError()
  productionPersonnelSubmitting.value = true
  try {
    await createTemporaryTeamEmployee({
      displayName: temporaryEmployeeForm.displayName.trim(),
      signaturePassword: temporaryEmployeeForm.signaturePassword
    })
    temporaryEmployeeForm.displayName = ''
    temporaryEmployeeForm.signaturePassword = ''
    ElMessage.success('临时工已新增并关联当前生产组长')
    await refreshProductionPersonnel()
  } catch (error) {
    showProductionPersonnelDialogError(
      resolveErrorMessage(error, '临时工新增失败，请确认是否重名并按提示加后缀')
    )
  } finally {
    productionPersonnelSubmitting.value = false
  }
}

const updateEmployeeDisplayName = async (row: TeamProductionEmployeeRespVO) => {
  try {
    const result = await ElMessageBox.prompt('请输入新的显示名；重名时请加后缀区分', '修改显示名', {
      inputValue: row.displayName || row.employeeName || '',
      inputPattern: /\S+/,
      inputErrorMessage: '显示名不能为空',
      confirmButtonText: '保存',
      cancelButtonText: '取消'
    })
    const displayName = String(result.value || '').trim()
    await updateTeamEmployeeDisplayNameRequest({
      employeeProfileId: requirePositiveNumber(row.id, '生产人员档案ID不能为空'),
      displayName
    })
    ElMessage.success('显示名已修改')
    await refreshProductionPersonnel()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, '显示名修改失败，请确认是否重名并按提示加后缀'))
  }
}

const updateEmployeeStatus = async (row: TeamProductionEmployeeRespVO, enabled: boolean) => {
  try {
    await ElMessageBox.confirm(
      enabled
        ? '启用后该员工可重新进入新报工选择。'
        : '禁用后该员工不再进入新报工选择，历史报工和签名继续保留姓名快照。',
      enabled ? '启用生产人员' : '禁用生产人员',
      {
        type: enabled ? 'success' : 'warning',
        confirmButtonText: enabled ? '启用' : '禁用',
        cancelButtonText: '取消'
      }
    )
    await updateTeamEmployeeStatusRequest({
      employeeProfileId: requirePositiveNumber(row.id, '生产人员档案ID不能为空'),
      enabled
    })
    ElMessage.success(enabled ? '员工已启用' : '员工已禁用')
    await refreshProductionPersonnel()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, enabled ? '员工启用失败' : '员工禁用失败'))
  }
}

const resetTemporarySignaturePassword = async (row: TeamProductionEmployeeRespVO) => {
  try {
    const result = await ElMessageBox.prompt('请输入新的临时工电子签名密码', '重置签名密码', {
      inputType: 'password',
      inputPattern: /\S+/,
      inputErrorMessage: '签名密码不能为空',
      confirmButtonText: '重置',
      cancelButtonText: '取消'
    })
    await resetTemporaryTeamEmployeeSignaturePassword({
      employeeProfileId: requirePositiveNumber(row.id, '生产人员档案ID不能为空'),
      signaturePassword: String(result.value || '')
    })
    ElMessage.success('临时工签名密码已重置')
    await refreshProductionPersonnel()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, '临时工签名密码重置失败'))
  }
}

const resetReviewAllocation = () => {
  reviewForm.allocationMode = 'FIFO'
  allocationRows.value = []
}

const loadActiveOrderTransferTraces = async () => {
  activeOrderTransferTraceError.value = ''
  activeOrderTransferTraceRows.value = []
  const activeOrders = activeOrderOptions.value.filter((order) => normalizePositiveNumber(order.id))
  if (activeOrders.length === 0) {
    return
  }
  activeOrderTransferTraceLoading.value = true
  try {
    const traceGroups = await Promise.all(
      activeOrderOptions.value.map((order) => getTeamLeaderActiveOrderTransferTrace(order.id))
    )
    activeOrderTransferTraceRows.value = traceGroups.flat()
  } catch (error) {
    activeOrderTransferTraceError.value = resolveErrorMessage(error, '活跃订单调拨库存追溯加载失败')
    activeOrderTransferTraceRows.value = []
    throw error
  } finally {
    activeOrderTransferTraceLoading.value = false
  }
}

const loadActiveOrders = async () => {
  activeOrderLoading.value = true
  let listLoaded = false
  try {
    activeOrderOptions.value = await getTeamLeaderActiveOrderList()
    listLoaded = true
    const maxPage = Math.max(1, Math.ceil(activeOrderOptions.value.length / activeOrderQuery.pageSize))
    if (activeOrderQuery.pageNo > maxPage) {
      activeOrderQuery.pageNo = maxPage
    }
    await loadActiveOrderTransferTraces()
  } catch (error) {
    if (!listLoaded) {
      activeOrderOptions.value = []
    }
    throw error
  } finally {
    activeOrderLoading.value = false
  }
}

const loadProcessConfigRows = async () => {
  if (!isProductionLeader.value) {
    processConfigRows.value = []
    return
  }
  processConfigLoading.value = true
  try {
    processConfigRows.value = await getTeamLeaderProcessConfigList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '工序配置列表加载失败'))
    throw error
  } finally {
    processConfigLoading.value = false
  }
}

const processConfigDeviceOptions = computed(() => {
  const optionMap = new Map<number, TeamLeaderProcessConfigDeviceVO>()
  processConfigRows.value.forEach((row) => {
    row.devices?.forEach((device) => {
      if (device.deviceId && !optionMap.has(device.deviceId)) {
        optionMap.set(device.deviceId, device)
      }
    })
  })
  return [...optionMap.values()]
})

const productionResponsibleRouteNames = computed(() => {
  const seen = new Set<string>()
  const routeNames: string[] = []
  processConfigRows.value.forEach((row) => {
    const routeName = String(row.routeName || '').trim()
    if (!routeName || seen.has(routeName)) {
      return
    }
    seen.add(routeName)
    routeNames.push(routeName)
  })
  return routeNames
})

const processConfigCreateSelectedRow = computed(() =>
  processConfigRows.value.find(
    (row) => row.routeProcessId === processConfigCreateForm.routeProcessId
  )
)

const processConfigCreateDeviceOptions = computed(
  () => processConfigCreateSelectedRow.value?.devices ?? []
)

const formatProcessConfigProcess = (row: TeamLeaderProcessConfigRowRespVO) => {
  const sortText = Number.isFinite(Number(row.sort)) ? `${row.sort} - ` : ''
  const processText = row.processName || row.processCode || row.processId || '--'
  return `${sortText}${processText}`
}

const formatProcessConfigCreateProcessOption = (row: TeamLeaderProcessConfigRowRespVO) => {
  const routeText = row.routeName || row.routeCode || row.routeId || '--'
  return `${routeText} / ${formatProcessConfigProcess(row)}`
}

const formatProcessConfigDevice = (device: TeamLeaderProcessConfigDeviceVO) => {
  const code = device.deviceCode ? `${device.deviceCode} / ` : ''
  const name = device.deviceName || device.deviceId || '--'
  return `${code}${name}`
}

const hasProcessConfigParameters = (row: TeamLeaderProcessConfigRowRespVO) =>
  row.devices?.some((device) => device.parameters?.length) ?? false

const formatProcessConfigAverage = (parameter: TeamLeaderProcessConfigParameterVO) => {
  if (parameter.actualAverage === null || parameter.actualAverage === undefined) {
    return '暂无样本'
  }
  const unit = parameter.unit ? ` ${parameter.unit}` : ''
  return `${parameter.actualAverage}${unit}`
}

const formatProcessConfigStatisticsWindow = (parameter: TeamLeaderProcessConfigParameterVO) => {
  const start = formatDateTimeValue(parameter.statisticsStartTime, '--')
  const end = formatDateTimeValue(parameter.statisticsEndTime, '--')
  return `${start} ~ ${end}（${parameter.statisticsWindowDays || 30}天）`
}

const syncProcessConfigCreateDevice = () => {
  processConfigCreateForm.deviceId = processConfigCreateDeviceOptions.value[0]?.deviceId
}

const resetProcessConfigCreateForm = () => {
  processConfigCreateForm.routeProcessId = processConfigRows.value[0]?.routeProcessId
  processConfigCreateForm.createType = 'LOSS_REASON'
  syncProcessConfigCreateDevice()
}

const handleProcessConfigCreateRouteChange = () => {
  syncProcessConfigCreateDevice()
}

const handleProcessConfigCreateTypeChange = () => {
  if (processConfigCreateForm.createType === 'PARAMETER_RULE') {
    syncProcessConfigCreateDevice()
  }
}

const ensureProcessConfigRowsLoadedForCreate = async () => {
  if (processConfigRows.value.length === 0) {
    await loadProcessConfigRows()
  }
  if (processConfigRows.value.length === 0) {
    ElMessage.error('当前账号没有可新增的路线工序，请先在工艺路线的工序开始配置中授权生产组长')
    return false
  }
  return true
}

const openCreateProcessConfigDataDialog = async () => {
  if (!(await ensureProcessConfigRowsLoadedForCreate())) return
  resetProcessConfigCreateForm()
  processConfigCreateDialogVisible.value = true
}

const confirmCreateProcessConfigData = () => {
  const row = processConfigCreateSelectedRow.value
  if (!row) {
    ElMessage.error('请先选择路线工序')
    return
  }
  if (processConfigCreateForm.createType === 'LOSS_REASON') {
    processConfigCreateDialogVisible.value = false
    openCreateLossReason(row)
    return
  }
  if (processConfigCreateForm.createType === 'DEVICE_BINDING') {
    processConfigCreateDialogVisible.value = false
    openProcessConfigDeviceDialog(row)
    return
  }
  const deviceId = Number(processConfigCreateForm.deviceId)
  if (!Number.isFinite(deviceId) || deviceId <= 0) {
    ElMessage.error('请选择设备')
    return
  }
  const device = row.devices?.find((item) => item.deviceId === deviceId)
  if (!device) {
    ElMessage.error('请选择当前工序已映射设备；新增参数标准前需先完成设备映射')
    return
  }
  processConfigCreateDialogVisible.value = false
  openProcessConfigParameterDialog(row, device, undefined, { create: true })
}

const resetLossReasonForm = () => {
  lossReasonForm.reasonCode = ''
  lossReasonForm.reasonName = ''
  lossReasonForm.enabled = true
  lossReasonForm.remark = ''
}

const openCreateLossReason = (row: TeamLeaderProcessConfigRowRespVO) => {
  lossReasonDialogMode.value = 'create'
  lossReasonEditingRow.value = row
  lossReasonEditingReason.value = undefined
  resetLossReasonForm()
  lossReasonDialogVisible.value = true
}

const openEditLossReason = (
  row: TeamLeaderProcessConfigRowRespVO,
  reason: TeamLeaderLossReasonVO
) => {
  lossReasonDialogMode.value = 'edit'
  lossReasonEditingRow.value = row
  lossReasonEditingReason.value = reason
  lossReasonForm.reasonCode = reason.reasonCode
  lossReasonForm.reasonName = reason.reasonName
  lossReasonForm.enabled = reason.enabled
  lossReasonForm.remark = ''
  lossReasonDialogVisible.value = true
}

const submitLossReason = async () => {
  const row = lossReasonEditingRow.value
  if (!row) {
    ElMessage.error('请先选择工序')
    return
  }
  const reasonName = lossReasonForm.reasonName.trim()
  const reasonCode = lossReasonForm.reasonCode.trim()
  if (!reasonName || (lossReasonDialogMode.value === 'create' && !reasonCode)) {
    ElMessage.error('损耗原因编码和名称不能为空')
    return
  }
  lossReasonSubmitting.value = true
  try {
    if (lossReasonDialogMode.value === 'create') {
      await createTeamLeaderLossReason({
        routeProcessId: row.routeProcessId,
        reasonCode,
        reasonName,
        enabled: lossReasonForm.enabled,
        remark: lossReasonForm.remark.trim() || undefined
      })
    } else {
      const reasonId = requirePositiveNumber(
        lossReasonEditingReason.value?.id,
        '损耗原因编号不能为空'
      )
      await updateTeamLeaderLossReason(reasonId, {
        reasonName,
        enabled: lossReasonForm.enabled,
        remark: lossReasonForm.remark.trim() || undefined
      })
    }
    ElMessage.success('损耗原因已保存')
    lossReasonDialogVisible.value = false
    await loadProcessConfigRows()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '损耗原因保存失败'))
  } finally {
    lossReasonSubmitting.value = false
  }
}

const handleDeleteLossReason = async (reason: TeamLeaderLossReasonVO) => {
  try {
    await ElMessageBox.confirm(
      `确认删除损耗原因「${reason.reasonCode} / ${reason.reasonName}」？删除后不能用于新报工，历史报工快照不受影响。`,
      '删除损耗原因',
      { type: 'warning' }
    )
    lossReasonSubmitting.value = true
    await deleteTeamLeaderLossReason(reason.id)
    ElMessage.success('损耗原因已删除')
    await loadProcessConfigRows()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(resolveErrorMessage(error, '损耗原因删除失败'))
    }
  } finally {
    lossReasonSubmitting.value = false
  }
}

const resetProcessConfigParameterForm = () => {
  processConfigParameterForm.deviceId = processConfigSelectedDevice.value?.deviceId
  processConfigParameterForm.parameterCode = ''
  processConfigParameterForm.parameterName = ''
  processConfigParameterForm.unit = ''
  processConfigParameterForm.lowerLimit = undefined
  processConfigParameterForm.targetValue = undefined
  processConfigParameterForm.upperLimit = undefined
  processConfigParameterForm.valueType = 'DECIMAL'
}

const openProcessConfigDeviceDialog = (row: TeamLeaderProcessConfigRowRespVO) => {
  processConfigSelectedRow.value = row
  processConfigSelectedDevice.value = undefined
  processConfigEditingParameter.value = undefined
  const mappedDeviceIds = new Set((row.devices ?? []).map((device) => device.deviceId))
  processConfigDeviceForm.deviceId =
    processConfigDeviceOptions.value.find((device) => !mappedDeviceIds.has(device.deviceId))
      ?.deviceId ?? processConfigDeviceOptions.value[0]?.deviceId
  processConfigDeviceDialogVisible.value = true
}

const openProcessConfigParameterDialog = (
  row: TeamLeaderProcessConfigRowRespVO,
  device: TeamLeaderProcessConfigDeviceVO,
  parameter?: TeamLeaderProcessConfigParameterVO,
  options: { create?: boolean } = {}
) => {
  processConfigSelectedRow.value = row
  processConfigSelectedDevice.value = device
  processConfigEditingParameter.value = options.create ? undefined : parameter ?? device.parameters?.[0]
  resetProcessConfigParameterForm()
  if (processConfigEditingParameter.value) {
    processConfigParameterForm.parameterCode = processConfigEditingParameter.value.parameterCode
    processConfigParameterForm.parameterName = processConfigEditingParameter.value.parameterName || ''
    processConfigParameterForm.unit = processConfigEditingParameter.value.unit || ''
    processConfigParameterForm.lowerLimit = Number(processConfigEditingParameter.value.lowerLimit)
    processConfigParameterForm.targetValue = Number(processConfigEditingParameter.value.targetValue)
    processConfigParameterForm.upperLimit = Number(processConfigEditingParameter.value.upperLimit)
    processConfigParameterForm.valueType = processConfigEditingParameter.value.valueType || 'DECIMAL'
  }
  processConfigParameterDialogVisible.value = true
}

const submitProcessConfigDeviceBinding = async () => {
  const row = processConfigSelectedRow.value
  if (!row) {
    ElMessage.error('请先选择路线工序')
    return
  }
  processConfigSubmitting.value = true
  try {
    await saveTeamProcessConfigDeviceBinding({
      routeProcessId: requirePositiveNumber(row.routeProcessId, '路线工序不能为空'),
      deviceId: requirePositiveNumber(processConfigDeviceForm.deviceId, '设备不能为空')
    })
    ElMessage.success('设备映射已保存')
    processConfigDeviceDialogVisible.value = false
    await loadProcessConfigRows()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '设备映射保存失败'))
  } finally {
    processConfigSubmitting.value = false
  }
}

const submitProcessConfigParameterRule = async () => {
  const row = processConfigSelectedRow.value
  const device = processConfigSelectedDevice.value
  if (!row || !device) {
    ElMessage.error('请先选择路线工序和设备')
    return
  }
  const parameterCode = processConfigParameterForm.parameterCode.trim()
  if (!parameterCode) {
    ElMessage.error('参数编码不能为空')
    return
  }
  const lowerLimit = requireFiniteNumber(processConfigParameterForm.lowerLimit, '参数下限不能为空')
  const targetValue = requireFiniteNumber(processConfigParameterForm.targetValue, '参数目标值不能为空')
  const upperLimit = requireFiniteNumber(processConfigParameterForm.upperLimit, '参数上限不能为空')
  if (lowerLimit > targetValue || targetValue > upperLimit) {
    ElMessage.error('参数区间必须满足下限 <= 目标值 <= 上限')
    return
  }
  processConfigSubmitting.value = true
  try {
    await saveTeamProcessConfigDeviceParameterRule({
      routeProcessId: requirePositiveNumber(row.routeProcessId, '路线工序不能为空'),
      deviceId: requirePositiveNumber(device.deviceId, '设备不能为空'),
      parameterCode,
      parameterName: processConfigParameterForm.parameterName.trim() || undefined,
      unit: processConfigParameterForm.unit.trim() || undefined,
      lowerLimit,
      targetValue,
      upperLimit,
      valueType: processConfigParameterForm.valueType
    })
    ElMessage.success('设备参数标准已保存')
    processConfigParameterDialogVisible.value = false
    await loadProcessConfigRows()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '设备参数标准保存失败'))
  } finally {
    processConfigSubmitting.value = false
  }
}

const markManualAllocation = () => {
  reviewForm.allocationMode = 'MANUAL'
}

const addAllocationLine = () => {
  reviewForm.allocationMode = 'MANUAL'
  allocationRows.value.push({
    activeOrderId: activeOrderOptions.value[0]?.id ?? 0,
    allocatedQuantity: 0
  })
}

const removeAllocationLine = (index: number) => {
  reviewForm.allocationMode = 'MANUAL'
  allocationRows.value.splice(index, 1)
}

const previewFifoAllocation = async () => {
  const eventId = requirePositiveNumber(reviewEvent.value?.id, '工序池提交事件编号不能为空')
  allocationPreviewLoading.value = true
  try {
    const preview = await previewTeamLeaderReportFifoAllocation({
      eventId,
      leaderType: queryParams.leaderType as TeamLeaderType
    })
    reviewForm.allocationMode = 'FIFO'
    allocationRows.value = (preview.lines || []).map((line) => ({
      activeOrderId: line.activeOrderId,
      workOrderId: line.workOrderId,
      workOrderCode: line.workOrderCode,
      allocatedQuantity: line.allocatedQuantity,
      remainingQuantityBeforeAllocation: line.remainingQuantityBeforeAllocation
    }))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'FIFO 自动分配失败'))
  } finally {
    allocationPreviewLoading.value = false
  }
}

const buildAllocationSubmitLines = (): TeamLeaderReportAllocationLine[] => {
  const lines = allocationRows.value.map((line) => ({
    activeOrderId: requirePositiveNumber(line.activeOrderId, '活跃订单不能为空'),
    allocatedQuantity: requirePositiveNumber(line.allocatedQuantity, '分配数量必须大于 0')
  }))
  if (lines.length === 0) {
    throw new Error('生产组长确认报工前必须分配到活跃订单')
  }
  return lines
}

const buildReviewSignaturePayload = () => ({
  reviewSignatureId: requirePositiveNumber(reviewForm.reviewSignatureId, '复核电子签名不能为空'),
  reviewSignatureEmployeeUserId: requirePositiveNumber(
    reviewForm.reviewSignatureEmployeeUserId,
    '复核签名员工不能为空'
  ),
  reviewSignatureSnapshotJson: reviewForm.reviewSignatureSnapshotJson.trim() || undefined
})

function parseJsonField<T>(value: string, label: string): T {
  if (!value || !value.trim()) {
    throw new Error(`${label}不能为空`)
  }
  try {
    return JSON.parse(value) as T
  } catch (error) {
    throw new Error(`${label}必须是合法 JSON`)
  }
}

const normalizePayloadJsonForCorrection = (payloadJson?: string) => {
  const text = payloadJson?.trim()
  if (!text) {
    throw new Error('原始payload缺失，不能发起修正')
  }
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch (error) {
    throw new Error('原始payload不是合法 JSON，不能发起修正')
  }
}

type PqcSubmissionContentItemKey = string

interface PqcSubmissionContentDefinition {
  key: PqcSubmissionContentItemKey
  label: string
  unit?: string
}

interface PqcSubmissionContentItem extends PqcSubmissionContentDefinition {
  valueText: string
}

type PqcSubmissionPayloadRecord = Record<string, unknown>

interface PqcItemSnapshotDetail {
  itemCode?: string
  itemName?: string
  selectedEquipmentId?: number
  selectedEquipmentCode?: string
  selectedEquipmentName?: string
  selectedEquipmentNumber?: string
  standardText?: string
  standardLowerLimit?: number | string
  standardUpperLimit?: number | string
  standardUnit?: string
  standardPrecision?: number
  inspectionMethod?: string
  resultType?: string
  sampleValues?: string[]
  itemResult?: string
  judgement?: string
}

interface SubmissionStructuredItem {
  key: string
  label: string
  valueText: string
  metaText?: string
  outOfRange?: boolean
  parameterStatus?: string
}

interface PqcFillFormSampleItem {
  key: string
  valueText: string
  outOfRange?: boolean
}

interface PqcFillFormSnapshotItem {
  key: string
  inspectionItemText: string
  inspectionStageText: string
  equipmentText: string
  equipmentNumberText: string
  standardText: string
  methodText: string
  judgementText: string
  quantityText: string
  scrapText: string
  defectDescriptionText: string
  samples: PqcFillFormSampleItem[]
}

interface ProductionParameterRuleSnapshot {
  parameterCode?: string
  parameterName?: string
  unit?: string
  lowerLimit?: number | string
  upperLimit?: number | string
}

const PQC_SUBMISSION_CONTENT_MISSING_ITEMS: PqcSubmissionContentItem[] = [
  {
    key: 'missing',
    label: 'PQC明细',
    valueText: 'PQC提交内容缺少正式项目明细'
  }
]

const isRecord = (value: unknown): value is PqcSubmissionPayloadRecord =>
  Boolean(value) && typeof value === 'object' && !Array.isArray(value)

const parsePqcOriginalPayload = (payloadJson?: string) => {
  const text = payloadJson?.trim()
  if (!text) {
    return undefined
  }
  try {
    const parsed = JSON.parse(text)
    return isRecord(parsed) ? parsed : undefined
  } catch (error) {
    console.warn('PQC提交原始payload解析失败', error)
    return undefined
  }
}

const isPqcSubmissionRow = (row: ProcessPoolTimelineEventVO) =>
  String(row.templateType || '').includes('PQC') || activeLeaderTab.value === 'PQC'

const readPqcPayloadField = (payload: PqcSubmissionPayloadRecord, key: string) => {
  const draft = isRecord(payload.pqcDraft) ? payload.pqcDraft : undefined
  return draft?.[key] ?? payload[key]
}

const normalizePqcSubmittedValues = (value: unknown): string[] => {
  if (Array.isArray(value)) {
    return value.map((item) => String(item ?? '').trim()).filter(Boolean)
  }
  if (isRecord(value)) {
    for (const nestedKey of ['values', 'pieceValues', 'results', 'value']) {
      const nestedValues = normalizePqcSubmittedValues(value[nestedKey])
      if (nestedValues.length) {
        return nestedValues
      }
    }
    return []
  }
  if (value === undefined || value === null) {
    return []
  }
  const text = String(value).trim()
  return text ? [text] : []
}

const toPqcItemSnapshotDetail = (value: unknown): PqcItemSnapshotDetail | undefined => {
  if (!isRecord(value)) {
    return undefined
  }
  const detail: PqcItemSnapshotDetail = {
    itemCode: String(value.itemCode ?? '').trim() || undefined,
    itemName: String(value.itemName ?? '').trim() || undefined,
    selectedEquipmentId: Number(value.selectedEquipmentId) || undefined,
    selectedEquipmentCode: String(value.selectedEquipmentCode ?? '').trim() || undefined,
    selectedEquipmentName: String(value.selectedEquipmentName ?? '').trim() || undefined,
    selectedEquipmentNumber: String(value.selectedEquipmentNumber ?? '').trim() || undefined,
    standardText: String(value.standardText ?? '').trim() || undefined,
    standardLowerLimit: value.standardLowerLimit as number | string | undefined,
    standardUpperLimit: value.standardUpperLimit as number | string | undefined,
    standardUnit: String(value.standardUnit ?? '').trim() || undefined,
    standardPrecision: Number(value.standardPrecision) || undefined,
    inspectionMethod: String(value.inspectionMethod ?? '').trim() || undefined,
    resultType: String(value.resultType ?? '').trim() || undefined,
    sampleValues: normalizePqcSubmittedValues(
      value.sampleValues ?? value.samples ?? value.values ?? value.measuredValue
    ),
    itemResult: String(value.itemResult ?? '').trim() || undefined,
    judgement: String(value.judgement ?? '').trim() || undefined
  }
  return detail.itemCode || detail.itemName ? detail : undefined
}

const normalizePqcItemSnapshotDetails = (value: unknown): PqcItemSnapshotDetail[] => {
  const sourceItems = Array.isArray(value)
    ? value
    : isRecord(value)
      ? Object.values(value)
      : []
  return sourceItems
    .map(toPqcItemSnapshotDetail)
    .filter((item): item is PqcItemSnapshotDetail => Boolean(item))
}

const resolvePqcPayloadPair = (row: ProcessPoolTimelineEventVO) => {
  const payload = parsePqcOriginalPayload(row.originalPayloadJson)
  const rootPayload = payload && isRecord(payload.rawPayload) ? payload.rawPayload : payload
  return { payload, rootPayload }
}

const resolvePqcItemSnapshotDetails = (row: ProcessPoolTimelineEventVO) => {
  const { payload, rootPayload } = resolvePqcPayloadPair(row)
  const sources = [
    rootPayload?.pqcItemDetails,
    payload?.pqcItemDetails,
    rootPayload?.itemResults,
    payload?.itemResults
  ]
  for (const source of sources) {
    const details = normalizePqcItemSnapshotDetails(source)
    if (details.length) {
      return details
    }
  }
  return []
}

const pqcDetailRows = computed<PqcItemSnapshotDetail[]>(() => {
  const currentDetail = detail.value
  if (!currentDetail || !isPqcSubmissionRow(currentDetail as ProcessPoolTimelineEventVO)) {
    return []
  }
  return resolvePqcItemSnapshotDetails(currentDetail as ProcessPoolTimelineEventVO)
})
const pqcDetailTotal = computed(() => pqcDetailRows.value.length)
const pagedPqcDetailRows = computed(() => {
  const pageNo = Math.max(1, Number(pqcDetailQuery.pageNo) || 1)
  const pageSize = Math.max(1, Number(pqcDetailQuery.pageSize) || 10)
  const start = (pageNo - 1) * pageSize
  return pqcDetailRows.value.slice(start, start + pageSize)
})

const formatSubmissionQuantity = (value: unknown) => {
  if (value === undefined || value === null || String(value).trim() === '') {
    return '--'
  }
  return `${String(value).trim()} 件`
}

const formatSubmissionText = (value: unknown, emptyText = '--') => {
  if (value === undefined || value === null || String(value).trim() === '') {
    return emptyText
  }
  return String(value).trim()
}

const readSubmissionNestedRecord = (
  payload: PqcSubmissionPayloadRecord | undefined,
  key: string
) => {
  const value = payload?.[key]
  return isRecord(value) ? value : undefined
}

const readSubmissionPayloadValue = (
  payload: PqcSubmissionPayloadRecord | undefined,
  keys: string[]
) => {
  const fieldValues = readSubmissionNestedRecord(payload, 'fieldValues')
  const pqcDraft = readSubmissionNestedRecord(payload, 'pqcDraft')
  for (const key of keys) {
    const directValue = payload?.[key]
    if (directValue !== undefined && directValue !== null && String(directValue).trim() !== '') {
      return directValue
    }
    const fieldValue = fieldValues?.[key]
    if (fieldValue !== undefined && fieldValue !== null && String(fieldValue).trim() !== '') {
      return fieldValue
    }
    const draftValue = pqcDraft?.[key]
    if (draftValue !== undefined && draftValue !== null && String(draftValue).trim() !== '') {
      return draftValue
    }
  }
  return undefined
}

const resolveSubmissionCompletionQuantity = (row: ProcessPoolTimelineEventVO) => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  const value = isPqcSubmissionRow(row)
    ? readSubmissionPayloadValue(rootPayload, ['inspectionQuantity', 'actualInspectionQuantity'])
    : readSubmissionPayloadValue(rootPayload, ['outputQuantity', 'OUTPUT_QUANTITY'])
  return formatSubmissionQuantity(value)
}

const resolveSubmissionLossQuantityValue = (row: ProcessPoolTimelineEventVO) => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  return isPqcSubmissionRow(row)
    ? readSubmissionPayloadValue(rootPayload, ['scrapQuantity', 'lossQuantity', 'SCRAP_QUANTITY'])
    : readSubmissionPayloadValue(rootPayload, ['lossQuantity', 'SCRAP_QUANTITY'])
}

const resolveSubmissionLossQuantity = (row: ProcessPoolTimelineEventVO) =>
  formatSubmissionQuantity(resolveSubmissionLossQuantityValue(row))

const normalizeSubmissionArray = (value: unknown) => Array.isArray(value) ? value : []

const resolveSubmissionLossBreakdownItems = (
  row: ProcessPoolTimelineEventVO
): SubmissionStructuredItem[] => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  const lossQuantity = resolveSubmissionLossQuantityValue(row)
  if (isPqcSubmissionRow(row)) {
    const description = readSubmissionPayloadValue(rootPayload, [
      'defectDescription',
      'nonconformanceDescription'
    ])
    return [{
      key: 'pqc-loss',
      label: formatSubmissionText(description, '不良/损耗'),
      valueText: formatSubmissionQuantity(lossQuantity)
    }]
  }
  const structuredLossDetails = row.lossDetails?.length
    ? row.lossDetails
    : normalizeSubmissionArray(rootPayload?.lossDetails || rootPayload?.lossReasonDetails)
  const details = structuredLossDetails
    .map((item, index): SubmissionStructuredItem | undefined => {
      if (!isRecord(item)) {
        return undefined
      }
      const quantity = item.quantity ?? item.lossQuantity
      return {
        key: String(item.reasonId ?? item.reasonCode ?? index),
        label: formatSubmissionText(item.reasonName ?? item.reasonCode, '损耗原因'),
        valueText: formatSubmissionQuantity(quantity)
      }
    })
    .filter((item): item is SubmissionStructuredItem => Boolean(item))
  if (details.length) {
    return details
  }
  const reasonName = readSubmissionPayloadValue(rootPayload, [
    'lossReasonNameSnapshot',
    'lossReasonCodeSnapshot'
  ])
  return [{
    key: 'production-loss',
    label: formatSubmissionText(reasonName, '损耗原因'),
    valueText: formatSubmissionQuantity(lossQuantity)
  }]
}

const resolveSubmissionEquipmentItems = (
  row: ProcessPoolTimelineEventVO
): SubmissionStructuredItem[] => {
  if (isPqcSubmissionRow(row)) {
    const seen = new Set<string>()
    const items = resolvePqcItemSnapshotDetails(row)
      .map((detail, index): SubmissionStructuredItem | undefined => {
        const equipment = detail.selectedEquipmentName || detail.selectedEquipmentCode
        if (!equipment) {
          return undefined
        }
        const key = String(detail.selectedEquipmentId || detail.selectedEquipmentCode || index)
        if (seen.has(key)) {
          return undefined
        }
        seen.add(key)
        return {
          key,
          label: detail.itemName || detail.itemCode || '检验项目',
          valueText: equipment
        }
      })
      .filter((item): item is SubmissionStructuredItem => Boolean(item))
    return items.length ? items : [{ key: 'empty-equipment', label: '设备', valueText: '--' }]
  }
  if (row.selectedDevice) {
    const deviceText = [
      row.selectedDevice.deviceName || row.selectedDevice.deviceCode,
      row.selectedDevice.deviceId ? `#${row.selectedDevice.deviceId}` : ''
    ].filter(Boolean).join(' / ')
    return [{
      key: String(row.selectedDevice.deviceId || row.selectedDevice.deviceCode || 'selected-device'),
      label: '选用设备',
      valueText: deviceText || '--'
    }]
  }
  const { rootPayload } = resolvePqcPayloadPair(row)
  const rawSelectedDevice = isRecord(rootPayload?.selectedDevice) ? rootPayload.selectedDevice : undefined
  if (rawSelectedDevice) {
    const deviceText = [
      rawSelectedDevice.deviceName || rawSelectedDevice.deviceCode,
      rawSelectedDevice.deviceId ? `#${rawSelectedDevice.deviceId}` : ''
    ].filter(Boolean).join(' / ')
    return [{
      key: String(rawSelectedDevice.deviceId || rawSelectedDevice.deviceCode || 'selected-device'),
      label: '选用设备',
      valueText: deviceText || '--'
    }]
  }
  const equipmentParameters = readSubmissionNestedRecord(rootPayload, 'equipmentParameters')
  const deviceText = readSubmissionPayloadValue(rootPayload, ['DEVICE'])
  const deviceLabels = equipmentParameters
    ? Object.keys(equipmentParameters)
    : String(deviceText || '').split('、').map((item) => item.trim()).filter(Boolean)
  return deviceLabels.length
    ? deviceLabels.map((label) => ({ key: label, label: '设备', valueText: label }))
    : [{ key: 'empty-equipment', label: '设备', valueText: '--' }]
}

const toFiniteDisplayNumber = (value: unknown) => {
  if (value === undefined || value === null || String(value).trim() === '') {
    return undefined
  }
  const normalized = String(value).replace(/,/g, '').trim()
  const numericValue = Number(normalized)
  return Number.isFinite(numericValue) ? numericValue : undefined
}

const isValueOutOfRange = (value: unknown, lower?: unknown, upper?: unknown) => {
  const numericValue = toFiniteDisplayNumber(value)
  if (numericValue === undefined) {
    return false
  }
  const lowerValue = toFiniteDisplayNumber(lower)
  const upperValue = toFiniteDisplayNumber(upper)
  return (
    (lowerValue !== undefined && numericValue < lowerValue) ||
    (upperValue !== undefined && numericValue > upperValue)
  )
}

const isPqcSampleOutOfRange = (value: unknown, detail: PqcItemSnapshotDetail) =>
  isValueOutOfRange(value, detail.standardLowerLimit, detail.standardUpperLimit)

const formatParameterRangeText = (lower?: unknown, upper?: unknown, unit = '') => {
  if ((lower === undefined || lower === null || lower === '') &&
    (upper === undefined || upper === null || upper === '')) {
    return ''
  }
  return `范围 ${lower ?? '--'} ~ ${upper ?? '--'}${unit}`
}

const resolvePqcDetailStructuredItems = (
  row: ProcessPoolTimelineEventVO,
  resolveValueText: (detail: PqcItemSnapshotDetail) => string
): SubmissionStructuredItem[] => {
  const details = resolvePqcItemSnapshotDetails(row)
  if (!details.length) {
    return [{ key: 'missing-pqc-detail', label: 'PQC明细', valueText: '--' }]
  }
  return details.map((detail, index) => ({
    key: detail.itemCode || `${detail.itemName || 'pqc-item'}-${index}`,
    label: detail.itemName || detail.itemCode || '检验项',
    valueText: resolveValueText(detail)
  }))
}

const resolvePqcInspectionItemItems = (row: ProcessPoolTimelineEventVO) =>
  resolvePqcDetailStructuredItems(row, (detail) =>
    formatSubmissionText(detail.itemName || detail.itemCode, '--')
  )

const resolvePqcEquipmentNumberItems = (row: ProcessPoolTimelineEventVO) =>
  resolvePqcDetailStructuredItems(row, (detail) =>
    formatSubmissionText(detail.selectedEquipmentNumber, '--')
  )

const resolvePqcAcceptanceStandardItems = (row: ProcessPoolTimelineEventVO) =>
  resolvePqcDetailStructuredItems(row, (detail) => formatPqcSnapshotStandard(detail))

const resolvePqcInspectionMethodItems = (row: ProcessPoolTimelineEventVO) =>
  resolvePqcDetailStructuredItems(row, (detail) =>
    formatSubmissionText(detail.inspectionMethod, '--')
  )

const resolvePqcInspectionJudgementItems = (row: ProcessPoolTimelineEventVO) =>
  resolvePqcDetailStructuredItems(row, (detail) =>
    formatSubmissionText(detail.judgement || detail.itemResult || detail.resultType, '--')
  )

const resolvePqcDefectDescriptionText = (row: ProcessPoolTimelineEventVO) => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  const value = readSubmissionPayloadValue(rootPayload, ['defectDescription', 'nonconformanceDescription'])
  return formatSubmissionText(value, '--')
}

const resolvePqcPieceSampleItems = (
  row: ProcessPoolTimelineEventVO
): SubmissionStructuredItem[] => {
  const details = resolvePqcItemSnapshotDetails(row)
  if (!details.length) {
    return [{ key: 'missing-pqc-sample', label: '样本', valueText: '--' }]
  }
  return details.flatMap((detail, detailIndex) => {
    const values = detail.sampleValues?.length ? detail.sampleValues : ['未填写']
    return values.map((value, sampleIndex) => ({
      key: `${detail.itemCode || detail.itemName || detailIndex}-${sampleIndex}`,
      label: `${detail.itemName || detail.itemCode || '检验项'}#${sampleIndex + 1}`,
      valueText: `${formatSubmissionText(value)}${detail.standardUnit || ''}`,
      outOfRange: isPqcSampleOutOfRange(value, detail)
    }))
  })
}

const normalizeProductionParameterRules = (value: unknown): ProductionParameterRuleSnapshot[] => {
  if (Array.isArray(value)) {
    return value.filter(isRecord).map((item) => ({
      parameterCode: formatSubmissionText(item.parameterCode, ''),
      parameterName: formatSubmissionText(item.parameterName, ''),
      unit: formatSubmissionText(item.unit, ''),
      lowerLimit: item.lowerLimit as number | string | undefined,
      upperLimit: item.upperLimit as number | string | undefined
    }))
  }
  if (isRecord(value)) {
    return Object.entries(value).map(([parameterCode, item]) => {
      const record = isRecord(item) ? item : {}
      return {
        parameterCode,
        parameterName: formatSubmissionText(record.parameterName, ''),
        unit: formatSubmissionText(record.unit, ''),
        lowerLimit: record.lowerLimit as number | string | undefined,
        upperLimit: record.upperLimit as number | string | undefined
      }
    })
  }
  return []
}

const resolveProductionParameterRule = (
  payload: PqcSubmissionPayloadRecord | undefined,
  deviceLabel: string,
  parameterCode: string
) => {
  const ruleRoot = readSubmissionNestedRecord(payload, 'equipmentParameterRules')
  const rules = normalizeProductionParameterRules(ruleRoot?.[deviceLabel])
  return rules.find((rule) => rule.parameterCode === parameterCode)
}

const resolveProductionParameterItems = (
  payload: PqcSubmissionPayloadRecord | undefined,
  deviceParameterReadings?: unknown[]
): SubmissionStructuredItem[] => {
  if (deviceParameterReadings?.length) {
    const items = deviceParameterReadings
      .map((item, index): SubmissionStructuredItem | undefined => {
        if (!isRecord(item)) {
          return undefined
        }
        const parameterStatus = formatSubmissionText(item.parameterStatus, 'NORMAL')
        const unit = formatSubmissionText(item.unit, '')
        const abnormal =
          parameterStatus === 'ABOVE_UPPER' ||
          parameterStatus === 'BELOW_LOWER' ||
          isValueOutOfRange(item.value, item.lowerLimit, item.upperLimit)
        return {
          key: String(item.parameterCode || index),
          label: [
            formatSubmissionText(item.deviceName || item.deviceCode, ''),
            formatSubmissionText(item.parameterName || item.parameterCode, '参数')
          ].filter(Boolean).join(' · '),
          valueText: `${formatSubmissionText(item.value)}${unit}`,
          metaText: formatParameterRangeText(item.lowerLimit, item.upperLimit, unit),
          outOfRange: abnormal,
          parameterStatus
        }
      })
      .filter((item): item is SubmissionStructuredItem => Boolean(item))
    if (items.length) {
      return items
    }
  }
  const equipmentParameters = readSubmissionNestedRecord(payload, 'equipmentParameters')
  if (!equipmentParameters) {
    return [{ key: 'empty-parameter', label: '参数', valueText: '--' }]
  }
  const items: SubmissionStructuredItem[] = []
  Object.entries(equipmentParameters).forEach(([deviceLabel, parameterValues]) => {
    if (!isRecord(parameterValues)) {
      items.push({
        key: deviceLabel,
        label: deviceLabel,
        valueText: formatSubmissionText(parameterValues)
      })
      return
    }
    Object.entries(parameterValues).forEach(([parameterCode, value]) => {
      const rule = resolveProductionParameterRule(payload, deviceLabel, parameterCode)
      const unit = rule?.unit || ''
      items.push({
        key: `${deviceLabel}-${parameterCode}`,
        label: [deviceLabel, rule?.parameterName || parameterCode].filter(Boolean).join(' · '),
        valueText: `${formatSubmissionText(value)}${unit}`,
        metaText: formatParameterRangeText(rule?.lowerLimit, rule?.upperLimit, unit),
        outOfRange: isValueOutOfRange(value, rule?.lowerLimit, rule?.upperLimit),
        parameterStatus: isValueOutOfRange(value, rule?.lowerLimit, rule?.upperLimit)
          ? 'ABNORMAL'
          : 'NORMAL'
      })
    })
  })
  return items.length ? items : [{ key: 'empty-parameter', label: '参数', valueText: '--' }]
}

const resolvePqcParameterItems = (row: ProcessPoolTimelineEventVO): SubmissionStructuredItem[] => {
  const details = resolvePqcItemSnapshotDetails(row)
  const items = details.map((detail, detailIndex) => {
    const equipmentText = detail.selectedEquipmentName || detail.selectedEquipmentCode
    const judgementText = detail.judgement || detail.itemResult || detail.resultType
    return {
      key: detail.itemCode || `${detail.itemName || 'pqc-parameter'}-${detailIndex}`,
      label: detail.itemName || detail.itemCode || '检验项目',
      valueText: formatPqcSnapshotStandard(detail),
      metaText: [
        equipmentText ? `设备：${equipmentText}` : '',
        detail.selectedEquipmentNumber ? `设备编号：${detail.selectedEquipmentNumber}` : '',
        detail.inspectionMethod ? `方法：${detail.inspectionMethod}` : '',
        judgementText ? `判定：${judgementText}` : ''
      ].filter(Boolean).join('；')
    }
  })
  return items.length ? items : [{ key: 'empty-parameter', label: '参数', valueText: '--' }]
}

const resolveSubmissionParameterItems = (
  row: ProcessPoolTimelineEventVO
): SubmissionStructuredItem[] => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  return isPqcSubmissionRow(row)
    ? resolvePqcParameterItems(row)
    : resolveProductionParameterItems(
        rootPayload,
        row.deviceParameterReadings?.length
          ? row.deviceParameterReadings
          : normalizeSubmissionArray(rootPayload?.deviceParameterReadings)
      )
}

const formatPqcSnapshotSampleValues = (detail: PqcItemSnapshotDetail) =>
  detail.sampleValues?.length ? detail.sampleValues.join('、') : '未填写'

const formatPqcSnapshotStandard = (detail: PqcItemSnapshotDetail) => {
  const lower = detail.standardLowerLimit
  const upper = detail.standardUpperLimit
  const unit = detail.standardUnit || ''
  const range = lower !== undefined || upper !== undefined
    ? `${lower ?? '--'} ~ ${upper ?? '--'}${unit}`
    : ''
  return [detail.standardText, range].filter(Boolean).join('；') || '未配置'
}

const resolvePqcInspectionTypeText = (value: unknown) => {
  if (value === 'FIRST') return '首检'
  if (value === 'PATROL') return '巡检'
  if (value === 'FINAL') return '末检'
  return String(value ?? '').trim()
}

const resolvePqcSubmissionOverviewItem = (
  payload: PqcSubmissionPayloadRecord
): PqcSubmissionContentItem | undefined => {
  const inspectionType = resolvePqcInspectionTypeText(readPqcPayloadField(payload, 'inspectionType'))
  const patrolRound = readPqcPayloadField(payload, 'patrolRound')
  const inspectionQuantity = readPqcPayloadField(payload, 'inspectionQuantity')
  const scrapQuantity = readPqcPayloadField(payload, 'scrapQuantity')
  const parts = [
    inspectionType,
    patrolRound ? `第${patrolRound}轮` : '',
    inspectionQuantity ? `检验${inspectionQuantity}件` : '',
    scrapQuantity ? `报废${scrapQuantity}件` : ''
  ].filter(Boolean)
  if (!parts.length) {
    return undefined
  }
  return {
    key: 'inspectionOverview',
    label: '检验信息',
    valueText: parts.join('，')
  }
}

const resolvePqcSubmissionContentItems = (
  row: ProcessPoolTimelineEventVO
): PqcSubmissionContentItem[] => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  const details = resolvePqcItemSnapshotDetails(row)
  if (!rootPayload || !details.length) {
    return PQC_SUBMISSION_CONTENT_MISSING_ITEMS
  }
  const contentItems = details.map((detail, index) => ({
    key: detail.itemCode || `pqc-item-${index}`,
    label: detail.itemName || detail.itemCode || '检验项目',
    valueText: [
      detail.selectedEquipmentNumber ? `设备编号：${detail.selectedEquipmentNumber}` : '',
      `样本：${formatPqcSnapshotSampleValues(detail)}`,
      detail.judgement ? `判定：${detail.judgement}` : ''
    ].filter(Boolean).join('；')
  }))
  const overviewItem = resolvePqcSubmissionOverviewItem(rootPayload)
  return overviewItem ? [overviewItem, ...contentItems] : contentItems
}

const resolveProductionSubmissionSummary = (row: ProcessPoolTimelineEventVO) =>
  row.submittedSummary || row.pqcSummary || '--'

const resolveSubmissionReviewStatusText = (status?: string) => {
  if (status === 'APPROVED') return '正确'
  if (status === 'REJECTED') return '不正确'
  return '待判定'
}

const resolveSubmissionReviewTagType = (status?: string) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

const resolveProcessInspectionAggregationStatusText = (status?: string) => {
  if (status === 'AGGREGATED') return '已汇集'
  if (status === 'FAILED') return '汇集失败'
  return '待汇集'
}

const resolveProcessInspectionAggregationTagType = (status?: string) => {
  if (status === 'AGGREGATED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

const buildSubmissionParams = (): TeamLeaderSubmissionPageReqVO => {
  if (!queryParams.submitDate) {
    throw new Error('提交日期不能为空')
  }
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    leaderType: queryParams.leaderType,
    submitDate: queryParams.submitDate,
    employeeUserId: normalizePositiveNumber(queryParams.employeeUserId),
    processId: normalizePositiveNumber(queryParams.processId),
    deviceId: normalizePositiveNumber(queryParams.deviceId),
    templateType: queryParams.templateType || undefined,
    workOrderId: normalizePositiveNumber(queryParams.workOrderId),
    workOrderCode: queryParams.workOrderCode?.trim() || undefined,
    productId: normalizePositiveNumber(queryParams.productId),
    productKeyword: queryParams.productKeyword?.trim() || undefined,
    inspectionType: queryParams.inspectionType || undefined,
    roundNo: normalizePositiveNumber(queryParams.roundNo),
    submissionReviewStatus: queryParams.submissionReviewStatus || undefined
  }
}

async function getSubmissionList() {
  ensureSubmissionDateCondition()
  loading.value = true
  loadError.value = ''
  try {
    const data = await getTeamLeaderSubmissionPage(buildSubmissionParams())
    submissionList.value = data.list || []
    submissionTotal.value = data.total || 0
  } catch (error) {
    submissionList.value = []
    submissionTotal.value = 0
    loadError.value = resolveErrorMessage(error, '班组长提交看板加载失败')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const {
  state: submissionMultiFilterState,
  applyMultiFilter: applySubmissionMultiFilterState,
  updateState: updateSubmissionMultiFilterState,
  removeCondition: removeSubmissionMultiFilterCondition,
  clearMultiFilterParams: clearSubmissionMultiFilterParams
} = useTableMultiFilter(
  'mes.processPool.teamLeader.submissions',
  submissionMultiFilterDefinitions,
  queryParams,
  getSubmissionList
)

const ensureSubmissionDateCondition = () => {
  const currentSubmitDate =
    typeof queryParams.submitDate === 'string' && queryParams.submitDate.trim()
      ? queryParams.submitDate.trim()
      : getDefaultSubmissionDate()
  queryParams.submitDate = currentSubmitDate

  const submitDateCondition: ListMultiFilterCondition = {
    id: DEFAULT_SUBMISSION_DATE_CONDITION_ID,
    key: 'submitDate',
    operator: 'eq',
    value: currentSubmitDate
  }
  const nextConditions = [
    submitDateCondition,
    ...submissionMultiFilterState.conditions.filter(
      (condition) =>
        (condition.id || condition.key) !== DEFAULT_SUBMISSION_DATE_CONDITION_ID &&
        condition.key !== 'submitDate'
    )
  ]
  const activeConditionStillExists = nextConditions.some(
    (condition) => (condition.id || condition.key) === submissionMultiFilterState.activeConditionId
  )
  updateSubmissionMultiFilterState({
    conditions: nextConditions,
    activeConditionId: activeConditionStillExists
      ? submissionMultiFilterState.activeConditionId
      : DEFAULT_SUBMISSION_DATE_CONDITION_ID
  })
}

const clearSubmissionFilterParams = () => {
  queryParams.employeeUserId = undefined
  queryParams.processId = undefined
  queryParams.deviceId = undefined
  queryParams.templateType = undefined
  queryParams.workOrderId = undefined
  queryParams.workOrderCode = undefined
  queryParams.productId = undefined
  queryParams.productKeyword = undefined
  queryParams.inspectionType = undefined
  queryParams.roundNo = undefined
  queryParams.submissionReviewStatus = undefined
}

const resetSubmissionQueryParams = (leaderType: TeamLeaderType) => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.leaderType = leaderType
  queryParams.submitDate = getDefaultSubmissionDate()
}

const hasSubmissionDateCondition = () => {
  const condition = submissionMultiFilterState.conditions.find(
    (item) => item.key === 'submitDate'
  )
  const value = condition?.value
  if (typeof value === 'string' && value.trim()) {
    return true
  }
  ElMessage.warning('提交日期是必填筛选条件，请先在标准多条件搜索中选择提交日期。')
  return false
}

const applySubmissionMultiFilter = async () => {
  if (!hasSubmissionDateCondition()) return
  await applySubmissionMultiFilterState()
}

const resetSubmissionMultiFilter = async () => {
  const leaderType = activeLeaderTab.value
  updateSubmissionMultiFilterState({ conditions: [], activeConditionId: undefined })
  clearSubmissionMultiFilterParams()
  clearSubmissionFilterParams()
  resetSubmissionQueryParams(leaderType)
  ensureSubmissionDateCondition()
  submissionList.value = []
  submissionTotal.value = 0
  loadError.value = ''
  await getSubmissionList()
}

watch(activePqcModuleTab, async (tab) => {
  if (tab === 'management' && activeLeaderTab.value === 'PQC') {
    queryParams.leaderType = 'PQC'
    ensureSubmissionDateCondition()
    await getSubmissionList()
  }
})

watch(activeProductionModuleTab, async (tab) => {
  if (tab === 'report' && activeLeaderTab.value === 'PRODUCTION') {
    queryParams.leaderType = 'PRODUCTION'
    queryParams.pageNo = 1
    ensureSubmissionDateCondition()
    await getSubmissionList()
  }
})

const handleLeaderTypeChange = async (value: string | number) => {
  const selectedTab = String(value) as WorkbenchLeaderTab
  const leaderType = selectedTab as TeamLeaderType
  activeLeaderTab.value = leaderType
  if (leaderType === 'PRODUCTION') {
    refreshProductionPersonnel()
    loadActiveOrders().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '活跃订单加载失败'))
    })
    loadProcessConfigRows().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '工序配置列表加载失败'))
    })
  } else {
    refreshPqcPersonnel()
  }
  await resetSubmissionMultiFilter()
}

const loadSubmissionDetail = async (eventId: number) => {
  detailLoading.value = true
  detail.value = undefined
  pqcDetailQuery.pageNo = 1
  try {
    detail.value = await getTeamLeaderSubmissionDetail(
      eventId,
      queryParams.leaderType as TeamLeaderType
    )
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '员工提交详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

const openDetail = async (event: ProcessPoolTimelineEventVO) => {
  const eventId = requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
  if (activeLeaderTab.value === 'PQC' && showPqcModuleTabs.value) {
    detailVisible.value = false
    activePqcModuleTab.value = 'detail'
    await loadSubmissionDetail(eventId)
    return
  }
  detailVisible.value = true
  await loadSubmissionDetail(eventId)
}

const openReview = async (event: ProcessPoolTimelineEventVO) => {
  requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
  if (!canReviewSubmission(event)) {
    ElMessage.error('已完成复核的提交不能重复复核')
    return
  }
  reviewEvent.value = event
  reviewForm.reviewStatus = 'APPROVED'
  resetReviewAllocation()
  reviewForm.reviewRemark = ''
  reviewForm.reviewSignatureId = undefined
  reviewForm.reviewSignatureEmployeeUserId = undefined
  reviewForm.reviewSignatureSnapshotJson = ''
  reviewVisible.value = true
  if (isProductionLeader.value) {
    try {
      await loadActiveOrders()
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, '活跃订单加载失败'))
    }
  }
}

const submitReview = async () => {
  const eventId = requirePositiveNumber(reviewEvent.value?.id, '工序池提交事件编号不能为空')
  if (reviewForm.reviewStatus === 'REJECTED' && !reviewForm.reviewRemark.trim()) {
    ElMessage.error('退回复核必须填写复核说明')
    return
  }
  reviewSubmitting.value = true
  try {
    const leaderType = queryParams.leaderType as TeamLeaderType
    const reviewRemark = reviewForm.reviewRemark.trim() || undefined
    const reviewSignaturePayload = buildReviewSignaturePayload()
    if (isProductionLeader.value && reviewForm.reviewStatus === 'APPROVED') {
      await confirmTeamLeaderReportAllocation({
        eventId,
        leaderType,
        allocationMode: reviewForm.allocationMode,
        reviewRemark,
        ...reviewSignaturePayload,
        allocations: buildAllocationSubmitLines()
      })
    } else {
      await reviewTeamLeaderSubmission({
        leaderType,
        eventId,
        reviewStatus: reviewForm.reviewStatus,
        reviewRemark,
        ...reviewSignaturePayload
      })
    }
    ElMessage.success('复核已提交')
    reviewVisible.value = false
    await getSubmissionList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '复核提交失败'))
  } finally {
    reviewSubmitting.value = false
  }
}

const openCorrection = (event: ProcessPoolTimelineEventVO) => {
  try {
    const eventId = requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
    if (!canCorrectSubmission(event)) {
      ElMessage.error('只有复核不正确的提交可以修正')
      return
    }
    correctionEvent.value = event
    correctionForm.eventId = eventId
    correctionForm.modifiedByUserId = undefined
    correctionForm.revisionSignatureId = undefined
    correctionForm.revisionSignatureUserId = undefined
    correctionForm.changeReason = ''
    correctionForm.afterPayloadJson = normalizePayloadJsonForCorrection(event.originalPayloadJson)
    correctionForm.revisionSignatureSnapshotJson = ''
    correctionForm.changedFieldsJson = ''
    correctionVisible.value = true
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '原始记录修正入口打开失败'))
  }
}

const buildCorrectionRequest = () => {
  parseJsonField<Record<string, unknown>>(correctionForm.afterPayloadJson, '修改后payload JSON')
  parseJsonField<Record<string, unknown>>(
    correctionForm.revisionSignatureSnapshotJson,
    '修正签名快照JSON'
  )
  const changedFields = parseJsonField<ProcessPoolEventRevisionFieldChangeVO[]>(
    correctionForm.changedFieldsJson,
    '字段变更JSON'
  )
  if (!Array.isArray(changedFields) || changedFields.length === 0) {
    throw new Error('字段变更JSON必须是非空数组')
  }
  if (changedFields.some((item) => typeof item.affectsQuantityFragment !== 'boolean')) {
    throw new Error('字段变更JSON中 affectsQuantityFragment 必须是 true 或 false')
  }
  if (!correctionForm.changeReason.trim()) {
    throw new Error('修改原因不能为空')
  }
  return {
    eventId: requirePositiveNumber(correctionForm.eventId, '工序池提交事件编号不能为空'),
    afterPayload: correctionForm.afterPayloadJson.trim(),
    changeReason: correctionForm.changeReason.trim(),
    revisionSignatureId: requirePositiveNumber(correctionForm.revisionSignatureId, '修正签名ID不能为空'),
    revisionSignatureUserId: requirePositiveNumber(
      correctionForm.revisionSignatureUserId,
      '签名用户ID不能为空'
    ),
    revisionSignatureSnapshot: correctionForm.revisionSignatureSnapshotJson.trim(),
    modifiedByUserId: requirePositiveNumber(correctionForm.modifiedByUserId, '修改人用户ID不能为空'),
    changedFields
  }
}

const submitCorrection = async () => {
  requirePositiveNumber(correctionEvent.value?.id, '工序池提交事件编号不能为空')
  correctionSubmitting.value = true
  try {
    await updateProcessPoolOriginalRecord(buildCorrectionRequest())
    ElMessage.success('修正已提交，修改日志已记录')
    correctionVisible.value = false
    await getSubmissionList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '原始记录修正失败'))
  } finally {
    correctionSubmitting.value = false
  }
}

const prefillAbnormal = (event: ProcessPoolTimelineEventVO) => {
  abnormalForm.workOrderId = normalizePositiveNumber(event.workOrderId)
  const matchedActiveOrder = activeOrderOptions.value.find(
    (order) => order.workOrderId === abnormalForm.workOrderId
  )
  abnormalForm.activeOrderId = matchedActiveOrder?.id
}

const handleAbnormalActiveOrderChange = (activeOrderId?: number) => {
  const activeOrder = activeOrderOptions.value.find((order) => order.id === activeOrderId)
  abnormalForm.workOrderId = activeOrder?.workOrderId
}

const requireSelectedActiveOrderWorkOrderId = () => {
  const activeOrderId = requirePositiveNumber(abnormalForm.activeOrderId, '订单号不能为空')
  const activeOrder = activeOrderOptions.value.find((order) => order.id === activeOrderId)
  if (!activeOrder) {
    throw new Error('订单号不存在或已移出')
  }
  return activeOrder.workOrderId
}

const submitAbnormal = async () => {
  const valid = await abnormalFormRef.value?.validate?.()
  if (valid === false) return
  abnormalSubmitting.value = true
  try {
    await markAndReportWorkOrderAbnormal({
      workOrderId: requireSelectedActiveOrderWorkOrderId(),
      abnormalReasonCode: abnormalForm.abnormalReasonCode.trim(),
      abnormalDescription: abnormalForm.abnormalDescription.trim()
    })
    ElMessage.success('异常已上报')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '异常上报失败'))
  } finally {
    abnormalSubmitting.value = false
  }
}

const resetActiveOrderForm = () => {
  activeOrderForm.workOrderId = undefined
  activeOrderSelectedCandidate.value = undefined
  activeOrderCandidateKeyword.value = ''
  activeOrderCandidateOptions.value = []
  activeOrderCandidateError.value = ''
  activeOrderCandidateLoading.value = false
}

const openActiveOrderDialog = () => {
  resetActiveOrderForm()
  activeOrderAddDialogVisible.value = true
}

const findActiveOrderCandidateById = (workOrderId: number) =>
  activeOrderCandidateOptions.value.find(
    (candidate) => Number(candidate.workOrderId) === Number(workOrderId)
  )

const findActiveOrderCandidateByCode = (workOrderCode: string) =>
  activeOrderCandidateOptions.value.find(
    (candidate) => candidate.workOrderCode.trim() === workOrderCode.trim()
  )

const handleActiveOrderCandidateClear = () => {
  activeOrderForm.workOrderId = undefined
  activeOrderSelectedCandidate.value = undefined
  activeOrderCandidateKeyword.value = ''
  activeOrderCandidateError.value = ''
}

const handleActiveOrderCandidateChange = (value?: number | string) => {
  const workOrderId = normalizePositiveNumber(value)
  if (!workOrderId) {
    handleActiveOrderCandidateClear()
    return
  }
  const selectedCandidate = findActiveOrderCandidateById(workOrderId)
  if (!selectedCandidate) {
    activeOrderForm.workOrderId = undefined
    activeOrderSelectedCandidate.value = undefined
    activeOrderCandidateError.value = '请选择订单号'
    return
  }
  activeOrderForm.workOrderId = selectedCandidate.workOrderId
  activeOrderSelectedCandidate.value = selectedCandidate
  activeOrderCandidateKeyword.value = selectedCandidate.workOrderCode
  activeOrderCandidateError.value = ''
}

const searchActiveOrderCandidates = async (keyword: string) => {
  const searchText = keyword.trim()
  activeOrderCandidateKeyword.value = searchText
  activeOrderCandidateError.value = ''
  if (!searchText) {
    activeOrderCandidateOptions.value = []
    handleActiveOrderCandidateClear()
    return
  }
  if (
    activeOrderSelectedCandidate.value
    && activeOrderSelectedCandidate.value.workOrderCode !== searchText
  ) {
    activeOrderForm.workOrderId = undefined
    activeOrderSelectedCandidate.value = undefined
  }
  activeOrderCandidateLoading.value = true
  try {
    activeOrderCandidateOptions.value = await searchTeamLeaderActiveOrderCandidates(searchText)
    const workOrderId = normalizePositiveNumber(activeOrderForm.workOrderId)
    if (workOrderId && !findActiveOrderCandidateById(workOrderId)) {
      activeOrderForm.workOrderId = undefined
      activeOrderSelectedCandidate.value = undefined
    }
  } catch (error) {
    activeOrderCandidateOptions.value = []
    activeOrderForm.workOrderId = undefined
    activeOrderSelectedCandidate.value = undefined
    activeOrderCandidateError.value = resolveErrorMessage(error, '订单号候选搜索失败')
    ElMessage.error(activeOrderCandidateError.value)
  } finally {
    activeOrderCandidateLoading.value = false
  }
}

const resolveActiveOrderCandidateByKeyword = async () => {
  const keyword = activeOrderCandidateKeyword.value.trim()
  if (!keyword) {
    return undefined
  }
  const localCandidate = findActiveOrderCandidateByCode(keyword)
  if (localCandidate) {
    return localCandidate
  }
  activeOrderCandidateLoading.value = true
  try {
    activeOrderCandidateOptions.value = await searchTeamLeaderActiveOrderCandidates(keyword)
    return findActiveOrderCandidateByCode(keyword)
  } catch (error) {
    activeOrderCandidateOptions.value = []
    activeOrderCandidateError.value = resolveErrorMessage(error, '订单号候选搜索失败')
    ElMessage.error(activeOrderCandidateError.value)
    return undefined
  } finally {
    activeOrderCandidateLoading.value = false
  }
}

const requireSelectedActiveOrderCandidateWorkOrderId = async () => {
  const workOrderId = normalizePositiveNumber(activeOrderForm.workOrderId)
  let selectedCandidate = activeOrderSelectedCandidate.value
  if (
    workOrderId
    && selectedCandidate
    && Number(selectedCandidate.workOrderId) === Number(workOrderId)
    && findActiveOrderCandidateById(workOrderId)
  ) {
    return workOrderId
  }
  selectedCandidate = await resolveActiveOrderCandidateByKeyword()
  if (!selectedCandidate) {
    throw new Error('请选择订单号')
  }
  activeOrderForm.workOrderId = selectedCandidate.workOrderId
  activeOrderSelectedCandidate.value = selectedCandidate
  activeOrderCandidateKeyword.value = selectedCandidate.workOrderCode
  activeOrderCandidateError.value = ''
  return requirePositiveNumber(selectedCandidate.workOrderId, '请选择订单号')
}

const submitAddActiveOrder = async () => {
  maintenanceSubmitting.value = true
  let writeCompleted = false
  try {
    await addTeamLeaderActiveOrder({
      workOrderId: await requireSelectedActiveOrderCandidateWorkOrderId()
    })
    writeCompleted = true
    ElMessage.success('活跃订单已加入')
    activeOrderAddDialogVisible.value = false
    resetActiveOrderForm()
    await loadActiveOrders()
  } catch (error) {
    ElMessage.error(
      resolveErrorMessage(error, writeCompleted ? '活跃订单已加入，但列表刷新失败' : '活跃订单加入失败')
    )
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitRemoveActiveOrder = async (row: TeamLeaderActiveOrderRespVO) => {
  maintenanceSubmitting.value = true
  let writeCompleted = false
  try {
    await removeTeamLeaderActiveOrder({
      activeOrderId: requirePositiveNumber(row.id, '活跃订单记录ID不能为空')
    })
    writeCompleted = true
    ElMessage.success('活跃订单已移出')
    await loadActiveOrders()
  } catch (error) {
    ElMessage.error(
      resolveErrorMessage(error, writeCompleted ? '活跃订单已移出，但列表刷新失败' : '活跃订单移出失败')
    )
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitProcessEmployeeBinding = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamProcessEmployeeBinding({
      processId: requirePositiveNumber(processEmployeeBindingForm.processId, '工序ID不能为空'),
      employeeProfileId: requirePositiveNumber(
        processEmployeeBindingForm.employeeProfileId,
        '员工档案ID不能为空'
      )
    })
    ElMessage.success('工序员工关系已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '工序员工关系保存失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitTeamDevice = async () => {
  maintenanceSubmitting.value = true
  try {
    await createTeamDevice({
      deviceCode: teamDeviceForm.deviceCode.trim(),
      deviceName: teamDeviceForm.deviceName.trim(),
      deviceStatus: teamDeviceForm.deviceStatus
    })
    ElMessage.success('设备已新增')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '设备新增失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitTeamDeviceStatus = async () => {
  maintenanceSubmitting.value = true
  try {
    await updateTeamDeviceStatus({
      deviceId: requirePositiveNumber(teamDeviceStatusForm.deviceId, '设备ID不能为空'),
      deviceStatus: teamDeviceStatusForm.deviceStatus
    })
    ElMessage.success('设备状态已更新')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '设备状态更新失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const submitProcessDefectReason = async () => {
  maintenanceSubmitting.value = true
  try {
    await saveTeamProcessDefectReason({
      processId: requirePositiveNumber(defectReasonForm.processId, '工序ID不能为空'),
      reasonType: defectReasonForm.reasonType,
      reasonCode: defectReasonForm.reasonCode.trim(),
      reasonName: defectReasonForm.reasonName.trim()
    })
    const nextReason = {
      reasonType: defectReasonForm.reasonType,
      reasonCode: defectReasonForm.reasonCode.trim(),
      reasonName: defectReasonForm.reasonName.trim()
    }
    configuredDefectReasonOptions.value = [
      ...configuredDefectReasonOptions.value.filter(
        (reason) => reason.reasonCode !== nextReason.reasonCode
      ),
      nextReason
    ]
    ElMessage.success('工序异常原因已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '工序异常原因保存失败'))
  } finally {
    maintenanceSubmitting.value = false
  }
}

const formatDateTime = (value?: string | number | Date) => formatDateTimeValue(value, '--')

const resolvePqcTagType = (pqcResult?: string) => {
  if (pqcResult === 'SUCCESS' || pqcResult === 'PASS') return 'success'
  if (pqcResult === 'FAILURE' || pqcResult === 'FAIL') return 'danger'
  return 'info'
}

onBeforeUnmount(clearProductionPersonnelDialogError)

onMounted(() => {
  if (isProductionLeader.value) {
    refreshProductionPersonnel()
    loadActiveOrders().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '活跃订单调拨库存追溯加载失败'))
    })
    loadProcessConfigRows().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '工序配置列表加载失败'))
    })
    if (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'report') {
      queryParams.leaderType = 'PRODUCTION'
      ensureSubmissionDateCondition()
      getSubmissionList()
    }
  } else {
    refreshPqcPersonnel()
    if (!showPqcModuleTabs.value) {
      ensureSubmissionDateCondition()
      getSubmissionList()
    }
  }
})
</script>

<style scoped>
.team-leader-workbench__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.team-leader-workbench__title {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.team-leader-workbench__subtitle {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.team-leader-workbench__embedded-header {
  margin-bottom: 14px;
}

.team-leader-workbench__pqc-module-card :deep(.el-card__body),
.team-leader-workbench__production-module-card :deep(.el-card__body) {
  position: relative;
  padding-top: 12px;
}

.team-leader-workbench__personnel-tabs--embedded :deep(.el-tabs__header) {
  display: none;
}

.team-leader-workbench__module-tabs--flat :deep(.el-tabs__header) {
  margin: 0 0 12px;
}

.team-leader-workbench__module-tabs--flat :deep(.el-tabs__item) {
  color: #172033;
  font-weight: 600;
}

.team-leader-workbench__module-tabs--flat :deep(.el-tabs__item.is-active) {
  color: #00a896;
}

.team-leader-workbench__module-tabs--flat :deep(.el-tabs__active-bar) {
  background-color: #00a896;
}

.team-leader-workbench__production-module-card .team-leader-workbench__module-tabs--flat :deep(.el-tabs__header) {
  padding-right: min(560px, 42vw);
}

.team-leader-workbench__responsible-routes {
  position: absolute;
  top: 12px;
  right: 18px;
  display: flex;
  max-width: min(560px, 42vw);
  min-height: 40px;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  overflow: hidden;
  white-space: nowrap;
}

.team-leader-workbench__responsible-routes-label {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.team-leader-workbench__responsible-route-tag {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.team-leader-workbench__responsible-route-tag :deep(.el-tag__content) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__responsible-routes-empty {
  color: #94a3b8;
  font-size: 12px;
}

@media (max-width: 1180px) {
  .team-leader-workbench__production-module-card .team-leader-workbench__module-tabs--flat :deep(.el-tabs__header) {
    padding-right: 0;
  }

  .team-leader-workbench__responsible-routes {
    position: static;
    max-width: 100%;
    justify-content: flex-start;
    margin: -4px 0 12px;
    flex-wrap: wrap;
    white-space: normal;
  }
}

.team-leader-workbench__query {
  margin-bottom: -15px;
}

.team-leader-workbench__form {
  max-width: 760px;
}

.team-leader-workbench__personnel-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.team-leader-workbench__personnel-actions--dialog {
  margin-bottom: 0;
}

.team-leader-workbench__personnel-name.is-disabled {
  color: #f56c6c;
}

.team-leader-workbench__pqc-personnel-name.is-disabled {
  color: #f56c6c;
}

:deep(.team-leader-workbench__pqc-candidate-option--occupied) {
  color: #f56c6c;
}

.team-leader-workbench__pqc-candidate-option {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.team-leader-workbench__pqc-candidate-disabled-reason {
  color: #f56c6c;
  font-size: 12px;
  font-weight: 600;
}

.team-leader-workbench__active-order-candidate {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #64748b;
}

.team-leader-workbench__active-order-candidate.is-eligible {
  color: #16a34a;
  font-weight: 700;
}

.team-leader-workbench__active-order-candidate-code {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__active-order-candidate-badge {
  flex: 0 0 auto;
  border-radius: 999px;
  background: #dcfce7;
  color: #16a34a;
  padding: 1px 7px;
  font-size: 12px;
  font-weight: 700;
}

.team-leader-workbench__active-order-candidate-reason {
  flex: 0 1 auto;
  overflow: hidden;
  color: #94a3b8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__personnel-dialog-header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 24px;
  padding-right: 36px;
}

.team-leader-workbench__personnel-dialog-title {
  color: #172033;
  font-size: 18px;
  font-weight: 500;
  white-space: nowrap;
}

.team-leader-workbench__personnel-dialog-error {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #f56c6c;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
}

.team-leader-workbench__personnel-dialog-error-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__personnel-dialog-error-close {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  padding: 2px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.team-leader-workbench__personnel-dialog-error-enter-active,
.team-leader-workbench__personnel-dialog-error-leave-active {
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}

.team-leader-workbench__personnel-dialog-error-enter-from,
.team-leader-workbench__personnel-dialog-error-leave-to {
  opacity: 0;
  transform: translateY(-3px);
}

.team-leader-workbench__full-control {
  width: 100%;
}

.team-leader-workbench__form-actions {
  margin-top: 14px;
}

.team-leader-workbench__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.team-leader-workbench__section-title {
  color: #172033;
  font-size: 15px;
  font-weight: 700;
}

.team-leader-workbench__hint {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.team-leader-workbench__qa-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 16px;
  margin-top: 16px;
}

.team-leader-workbench__qa-card {
  margin-top: 16px;
}

.team-leader-workbench__qa-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.team-leader-workbench__qa-rule-name {
  color: #172033;
  font-weight: 700;
}

.team-leader-workbench__qa-source {
  display: grid;
  gap: 6px;
  padding: 8px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.team-leader-workbench__qa-source-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  color: #172033;
  font-size: 12px;
  font-weight: 700;
}

.team-leader-workbench__qa-source-label {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.team-leader-workbench__qa-source-text {
  color: #172033;
  font-size: 12px;
  line-height: 1.55;
  white-space: normal;
}

.team-leader-workbench__qa-check-list {
  display: grid;
  gap: 10px;
}

.team-leader-workbench__qa-check {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
  padding: 10px;
  border: 1px solid #f2c6c6;
  border-radius: 8px;
  background: #fff7f7;
}

.team-leader-workbench__qa-check.is-passed {
  border-color: #b7e1c0;
  background: #f5fff7;
}

.team-leader-workbench__qa-check-title {
  color: #172033;
  font-weight: 700;
}

.team-leader-workbench__qa-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.team-leader-workbench__maintenance-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.team-leader-workbench__daily-close-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.team-leader-workbench__daily-close-card {
  border-color: #d9e2f1;
}

.team-leader-workbench__daily-close-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.team-leader-workbench__daily-close-value {
  margin-top: 6px;
  color: #172033;
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.team-leader-workbench__daily-close-hint {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.team-leader-workbench__transfer-trace {
  width: 100%;
  margin-top: 8px;
}

.team-leader-workbench__loss-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.team-leader-workbench__parameter-value.is-parameter-out-of-range {
  color: #dc2626;
  font-weight: 700;
}

.team-leader-workbench__review-log {
  display: grid;
  gap: 4px;
  color: #334155;
  font-size: 12px;
  line-height: 1.5;
}

.team-leader-workbench__review-text,
.team-leader-workbench__review-meta {
  word-break: break-word;
}

.team-leader-workbench__review-meta {
  color: #64748b;
}

.team-leader-workbench__detail-tab-body,
.team-leader-workbench__detail-standard-list {
  display: grid;
  gap: 16px;
}

.team-leader-workbench__detail-standard-list {
  margin-top: 16px;
}

.team-leader-workbench__detail-descriptions:deep(.el-descriptions__label) {
  width: 400px !important;
  min-width: 400px;
  white-space: nowrap;
}

.team-leader-workbench__submission-log {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.team-leader-workbench__submission-log-title {
  color: #172033;
  font-size: 14px;
  font-weight: 700;
}

.team-leader-workbench__correction-form {
  margin-top: 16px;
}

.team-leader-workbench__number {
  width: 100%;
}

.team-leader-workbench__pqc-content {
  display: grid;
  gap: 4px;
  color: #334155;
  font-size: 12px;
  line-height: 1.5;
}

.team-leader-workbench__pqc-content-item {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 8px;
}

.team-leader-workbench__pqc-content-label {
  color: #0f172a;
  font-weight: 600;
}

.team-leader-workbench__pqc-content-value {
  word-break: break-word;
}

.team-leader-workbench__structured-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.team-leader-workbench__structured-pill {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  border: 1px solid #d7eadf;
  border-radius: 999px;
  background: #f4fbf7;
  color: #264237;
  padding: 2px 8px;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
}

.team-leader-workbench__pqc-fill-form {
  display: grid;
  gap: 8px;
  min-width: 0;
  color: #263c35;
  font-size: 12px;
  line-height: 1.5;
}

.team-leader-workbench__pqc-fill-form-item {
  display: grid;
  gap: 6px;
  border: 1px solid #d7eadf;
  border-radius: 10px;
  background: #f8fcfa;
  padding: 8px;
}

.team-leader-workbench__pqc-fill-form-title {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  color: #0f172a;
}

.team-leader-workbench__pqc-fill-form-title span {
  border-radius: 999px;
  background: #e6f4ec;
  color: #2d5a46;
  padding: 1px 7px;
  font-weight: 600;
}

.team-leader-workbench__pqc-fill-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 10px;
  word-break: break-word;
}

.team-leader-workbench__pqc-fill-form-samples {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.team-leader-workbench__pqc-fill-form-samples .team-leader-workbench__parameter-value {
  border: 1px solid #d7eadf;
  border-radius: 999px;
  background: #fff;
  padding: 1px 7px;
}
.team-leader-workbench__parameter-list {
  display: grid;
  gap: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.team-leader-workbench__parameter-item {
  display: grid;
  grid-template-columns: minmax(72px, 0.72fr) minmax(58px, auto) minmax(0, 1fr);
  gap: 6px;
  align-items: center;
}

.team-leader-workbench__parameter-label {
  color: #0f172a;
  font-weight: 600;
}

.team-leader-workbench__parameter-value {
  color: #1f2937;
  font-weight: 700;
}

.team-leader-workbench__parameter-value.is-out-of-range {
  color: #c00000;
}

.team-leader-workbench__parameter-meta {
  color: #64748b;
  word-break: break-word;
}

@media (max-width: 1180px) {
  .team-leader-workbench__qa-layout,
  .team-leader-workbench__maintenance-grid,
  .team-leader-workbench__daily-close-grid,
  .team-leader-workbench__personnel-actions--dialog {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .team-leader-workbench__personnel-dialog-header {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .team-leader-workbench__personnel-dialog-error {
    justify-content: flex-start;
  }

  .team-leader-workbench__personnel-dialog-error-text {
    white-space: normal;
  }
}
</style>
