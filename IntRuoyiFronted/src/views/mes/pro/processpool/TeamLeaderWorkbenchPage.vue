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
    <div
      v-if="isProductionLeader"
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
        {{ responsibleRouteLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
      </span>
    </div>
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
      <el-tab-pane
        label="报工历史"
        name="reportHistory"
        data-production-leader-module-tab-report-history
      />
      <el-tab-pane
        label="活跃订单池"
        name="activeOrder"
        data-production-leader-module-tab-active-order
      />
      <el-tab-pane
        label="工序配置"
        name="processConfig"
        data-production-leader-module-tab-process-config
      />
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
        {{ responsibleRouteLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
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
                  <el-tag
                    :type="row.employeeType === 'TEMPORARY' ? 'warning' : 'success'"
                    effect="plain"
                  >
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
          <div
            class="team-leader-workbench__personnel-actions team-leader-workbench__personnel-actions--dialog"
          >
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
      <el-tab-pane label="历史表单" name="history" data-pqc-leader-module-tab-history />
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
                'team-leader-workbench__pqc-candidate-option--occupied':
                  candidate.occupiedByOtherPqcLeader
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
      <el-tab-pane
        label="报工历史"
        name="reportHistory"
        data-production-leader-module-tab-report-history
      />
      <el-tab-pane
        label="活跃订单池"
        name="activeOrder"
        data-production-leader-module-tab-active-order
      />
      <el-tab-pane
        label="工序配置"
        name="processConfig"
        data-production-leader-module-tab-process-config
      />
    </el-tabs>
    <div
      v-if="showProductionResponsibleRoutes"
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
        {{ responsibleRouteLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
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
      <el-tab-pane label="历史表单" name="history" data-pqc-leader-module-tab-history />
    </el-tabs>
    <div
      v-if="!showPqcModuleTabs && !showProductionModuleTabs"
      class="team-leader-workbench__section-head"
    >
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
          :class="{ 'team-leader-workbench__submission-table--single-line': activeLeaderTab === 'PQC' }"
          :data="submissionList"
          border
          stripe
          :show-overflow-tooltip="true"
          @header-dragend="handleSubmissionHeaderDragend"
        >
          <el-table-column v-if="isProductionLeader" type="expand" width="52">
            <template #default="{ row }">
              <div
                class="team-leader-workbench__submission-detail"
                data-team-leader-submission-expand-detail
              >
                <section
                  class="team-leader-workbench__submission-detail-section"
                  data-team-leader-submission-material-detail
                >
                  <div class="team-leader-workbench__submission-detail-title">物料明细</div>
                  <div
                    class="team-leader-workbench__submission-material-cards"
                    data-team-leader-submission-material-card
                  >
                    <div
                      v-for="material in resolveSubmissionMaterialDetailRows(row)"
                      :key="material.key"
                      class="team-leader-workbench__submission-material-card"
                    >
                      <div class="team-leader-workbench__submission-material-head">
                        <strong>{{ material.materialText }}</strong>
                        <span>完成 {{ material.outputQuantityText }}</span>
                        <span>损耗 {{ material.lossQuantityText }}</span>
                        <span>{{ material.lossDetailText }}</span>
                      </div>
                      <el-table
                        :data="material.devices"
                        border
                        size="small"
                        class="team-leader-workbench__submission-detail-table"
                        data-team-leader-submission-material-device-row
                      >
                        <el-table-column label="设备名称" prop="deviceNameText" min-width="180" />
                        <el-table-column label="设备编号" prop="deviceCodeText" min-width="160" />
                        <el-table-column label="计量状态" prop="meteringValidityText" min-width="130" />
                        <el-table-column label="设备参数" prop="parameterText" min-width="520">
                          <template #default="{ row: device }">
                            <span
                              class="team-leader-workbench__submission-device-parameter-inline"
                              :class="{ 'is-out-of-range': device.hasAbnormalParameter }"
                              :data-parameter-status="
                                device.hasAbnormalParameter ? 'ABNORMAL' : 'NORMAL'
                              "
                              data-team-leader-submission-device-parameter-detail
                            >
                              {{ device.parameterText }}
                            </span>
                          </template>
                        </el-table-column>
                      </el-table>
                      <div
                        v-if="!material.devices.length"
                        class="team-leader-workbench__submission-material-empty"
                      >
                        当前物料没有设备快照
                      </div>
                    </div>
                  </div>
                </section>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isSubmissionColumnVisible('submittedAt')"
            label="提交时间"
            prop="submittedAt"
            :width="getSubmissionColumnWidthString('submittedAt')"
            :min-width="getSubmissionColumnMinWidthString('submittedAt', 160)"
          >
            <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isSubmissionColumnVisible('employeeUser')"
            :label="employeeColumnLabel"
            prop="employeeUser"
            :width="getSubmissionColumnWidthString('employeeUser')"
            :min-width="getSubmissionColumnMinWidthString('employeeUser', 140)"
          >
            <template #default="{ row }">
              {{ row.actualEmployeeUserName || '--' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isSubmissionColumnVisible('process')"
            label="工序"
            prop="process"
            :width="getSubmissionColumnWidthString('process')"
            :min-width="getSubmissionColumnMinWidthString('process', 150)"
          >
            <template #default="{ row }">{{ row.processName || row.processCode || '--' }}</template>
          </el-table-column>
          <el-table-column
            v-if="isSubmissionColumnVisible('clearanceWorkplace')"
            label="清场"
            prop="clearanceWorkplace"
            align="center"
            :width="getSubmissionColumnWidthString('clearanceWorkplace')"
            :min-width="getSubmissionColumnMinWidthString('clearanceWorkplace', 90)"
          >
            <template #default="{ row }">
              {{ resolveProductionClearanceConfirmationText(row, 'workplace') }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isSubmissionColumnVisible('clearanceMaterial')"
            label="物料"
            prop="clearanceMaterial"
            align="center"
            :width="getSubmissionColumnWidthString('clearanceMaterial')"
            :min-width="getSubmissionColumnMinWidthString('clearanceMaterial', 90)"
          >
            <template #default="{ row }">
              {{ resolveProductionClearanceConfirmationText(row, 'material') }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isSubmissionColumnVisible('clearanceCleaning')"
            label="清洁"
            prop="clearanceCleaning"
            align="center"
            :width="getSubmissionColumnWidthString('clearanceCleaning')"
            :min-width="getSubmissionColumnMinWidthString('clearanceCleaning', 90)"
          >
            <template #default="{ row }">
              {{ resolveProductionClearanceConfirmationText(row, 'cleaning') }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isSubmissionColumnVisible('workOrder')"
            label="生产工单"
            prop="workOrder"
            :width="getSubmissionColumnWidthString('workOrder')"
            :min-width="getSubmissionColumnMinWidthString('workOrder', 160)"
          >
            <template #default="{ row }">
              <span :data-pqc-leader-work-order="activeLeaderTab === 'PQC' ? '' : undefined">
                {{ row.workOrderCode || row.workOrderName || '--' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="activeLeaderTab !== 'PRODUCTION' && isSubmissionColumnVisible('completionQuantity')"
            :label="completionQuantityColumnLabel"
            prop="completionQuantity"
            :width="getSubmissionColumnWidthString('completionQuantity')"
            :min-width="getSubmissionColumnMinWidthString('completionQuantity', 130)"
          >
            <template #default="{ row }">
              <span data-team-leader-completion-quantity>
                {{ resolveSubmissionCompletionQuantity(row) }}
              </span>
              <el-tag
                v-if="isProductionLeader && resolveProductionReportOverageQuantity(row) > 0"
                class="team-leader-workbench__report-overage"
                data-team-leader-report-overage
                type="danger"
                effect="dark"
              >
                待调整 {{ resolveProductionReportOverageQuantity(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="activeLeaderTab !== 'PRODUCTION' && isSubmissionColumnVisible('lossQuantity')"
            label="损耗数量"
            prop="lossQuantity"
            :width="getSubmissionColumnWidthString('lossQuantity')"
            :min-width="getSubmissionColumnMinWidthString('lossQuantity', 120)"
          >
            <template #default="{ row }">
              <span data-team-leader-loss-quantity>
                {{ resolveSubmissionLossQuantity(row) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProductionLeader && isSubmissionColumnVisible('reportAllocations')"
            label="分配订单"
            prop="reportAllocations"
            :width="getSubmissionColumnWidthString('reportAllocations')"
            :min-width="getSubmissionColumnMinWidthString('reportAllocations', 240)"
          >
            <template #default="{ row }">
              <div
                class="team-leader-workbench__structured-list"
                data-team-leader-report-allocations
              >
                <el-tag
                  v-for="item in row.reportAllocations || []"
                  :key="item.allocationId"
                  :type="item.released ? 'success' : 'warning'"
                  effect="plain"
                >
                  {{ item.workOrderCode || item.workOrderId }}：{{ item.allocatedQuantity }}（{{
                    item.released ? '已放行' : '未放行'
                  }}）
                </el-tag>
                <span v-if="!row.reportAllocations?.length">--</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="
              activeLeaderTab !== 'PRODUCTION' &&
              isProductionLeader &&
              isSubmissionColumnVisible('reportUnallocatedQuantity')
            "
            label="未分配数量"
            prop="reportUnallocatedQuantity"
            :width="getSubmissionColumnWidthString('reportUnallocatedQuantity')"
            :min-width="getSubmissionColumnMinWidthString('reportUnallocatedQuantity', 130)"
          >
            <template #default="{ row }">
              {{ row.reportUnallocatedQuantity ?? row.outputQuantity ?? '--' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="
              activeLeaderTab !== 'PRODUCTION' &&
              isProductionLeader &&
              isSubmissionColumnVisible('submissionMaterialSummary')
            "
            label="物料明细"
            prop="submissionMaterialSummary"
            :width="getSubmissionColumnWidthString('submissionMaterialSummary')"
            :min-width="getSubmissionColumnMinWidthString('submissionMaterialSummary', 180)"
          >
            <template #default="{ row }">
              <div
                class="team-leader-workbench__structured-list"
                data-team-leader-submission-material-summary
              >
                <span
                  v-for="item in resolveSubmissionMaterialSummaryItems(row)"
                  :key="item.key"
                  class="team-leader-workbench__structured-pill"
                >
                  {{ item.valueText }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="
              activeLeaderTab !== 'PRODUCTION' &&
              isProductionLeader &&
              isSubmissionColumnVisible('submissionDeviceSummary')
            "
            label="选用设备"
            prop="submissionDeviceSummary"
            :width="getSubmissionColumnWidthString('submissionDeviceSummary')"
            :min-width="getSubmissionColumnMinWidthString('submissionDeviceSummary', 180)"
          >
            <template #default="{ row }">
              <div
                class="team-leader-workbench__structured-list"
                data-team-leader-submission-device-summary
              >
                <span
                  v-for="item in resolveSubmissionDeviceSummaryItems(row)"
                  :key="item.key"
                  class="team-leader-workbench__structured-pill"
                >
                  {{ item.valueText }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="
              activeLeaderTab !== 'PRODUCTION' &&
              isProductionLeader &&
              isSubmissionColumnVisible('submissionParameterSummary')
            "
            label="设备参数"
            prop="submissionParameterSummary"
            :width="getSubmissionColumnWidthString('submissionParameterSummary')"
            :min-width="getSubmissionColumnMinWidthString('submissionParameterSummary', 180)"
          >
            <template #default="{ row }">
              <div
                class="team-leader-workbench__structured-list"
                data-team-leader-submission-parameter-summary
              >
                <span
                  v-for="item in resolveSubmissionParameterSummaryItems(row)"
                  :key="item.key"
                  class="team-leader-workbench__structured-pill"
                >
                  {{ item.valueText }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="activeLeaderTab !== 'PRODUCTION' && isSubmissionColumnVisible('lossBreakdown')"
            label="损耗明细"
            prop="lossBreakdown"
            :width="getSubmissionColumnWidthString('lossBreakdown')"
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
            :width="getSubmissionColumnWidthString('product')"
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
            :width="getSubmissionColumnWidthString('inspectionTask')"
            :min-width="getSubmissionColumnMinWidthString('inspectionTask', 150)"
          >
            <template #default="{ row }">
              <span data-pqc-leader-submission-task>
                {{ resolvePqcInspectionTypeText(row.inspectionType) }} / 第
                {{ row.roundNo || '--' }} 轮
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="activeLeaderTab === 'PQC' && isSubmissionColumnVisible('inspectionItems')"
            label="检验项"
            prop="inspectionItems"
            :width="getSubmissionColumnWidthString('inspectionItems')"
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
            :width="getSubmissionColumnWidthString('equipmentSnapshot')"
            :min-width="getSubmissionColumnMinWidthString('equipmentSnapshot', 220)"
          >
            <template #default="{ row }">
              <div
                class="team-leader-workbench__structured-list"
                data-team-leader-equipment-snapshot
              >
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
            v-if="activeLeaderTab !== 'PRODUCTION' && isSubmissionColumnVisible('selectedDevice')"
            label="选用设备"
            prop="selectedDevice"
            :width="getSubmissionColumnWidthString('selectedDevice')"
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
            :width="getSubmissionColumnWidthString('equipmentNumber')"
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
            :width="getSubmissionColumnWidthString('acceptanceStandard')"
            :min-width="getSubmissionColumnMinWidthString('acceptanceStandard', 220)"
          >
            <template #default="{ row }">
              <div
                class="team-leader-workbench__structured-list"
                data-pqc-leader-acceptance-standard
              >
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
            :width="getSubmissionColumnWidthString('inspectionMethod')"
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
            :width="getSubmissionColumnWidthString('inspectionJudgement')"
            :min-width="getSubmissionColumnMinWidthString('inspectionJudgement', 150)"
          >
            <template #default="{ row }">
              <div
                class="team-leader-workbench__structured-list"
                data-pqc-leader-inspection-judgement
              >
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
            v-if="activeLeaderTab !== 'PRODUCTION' && isSubmissionColumnVisible('parameterSnapshot')"
            label="参数明细"
            prop="parameterSnapshot"
            :width="getSubmissionColumnWidthString('parameterSnapshot')"
            :min-width="getSubmissionColumnMinWidthString('parameterSnapshot', 280)"
            :show-overflow-tooltip="false"
          >
            <template #default="{ row }">
              <div
                class="team-leader-workbench__parameter-list"
                data-team-leader-parameter-snapshot
              >
                <div
                  v-for="item in resolveSubmissionParameterItems(row)"
                  :key="item.key"
                  class="team-leader-workbench__parameter-item"
                >
                  <span class="team-leader-workbench__parameter-label">{{ item.label }}</span>
                  <span
                    class="team-leader-workbench__parameter-value"
                    :class="{
                      'is-parameter-out-of-range': item.outOfRange,
                      'is-out-of-range': item.outOfRange
                    }"
                    :data-parameter-status="
                      item.parameterStatus || (item.outOfRange ? 'ABNORMAL' : 'NORMAL')
                    "
                    :aria-label="
                      item.outOfRange ? `参数异常：${item.label} ${item.valueText}` : item.label
                    "
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
            v-if="
              activeLeaderTab !== 'PRODUCTION' &&
              isSubmissionColumnVisible('deviceParameterReadings')
            "
            label="设备参数"
            prop="deviceParameterReadings"
            :width="getSubmissionColumnWidthString('deviceParameterReadings')"
            :min-width="getSubmissionColumnMinWidthString('deviceParameterReadings', 280)"
            :show-overflow-tooltip="false"
          >
            <template #default="{ row }">
              <div
                class="team-leader-workbench__parameter-list"
                data-team-leader-device-parameter-readings
              >
                <div
                  v-for="item in resolveSubmissionParameterItems(row)"
                  :key="item.key"
                  class="team-leader-workbench__parameter-item"
                >
                  <span class="team-leader-workbench__parameter-label">{{ item.label }}</span>
                  <span
                    class="team-leader-workbench__parameter-value"
                    :class="{
                      'is-parameter-out-of-range': item.outOfRange,
                      'is-out-of-range': item.outOfRange
                    }"
                    :data-parameter-status="
                      item.parameterStatus || (item.outOfRange ? 'ABNORMAL' : 'NORMAL')
                    "
                    :aria-label="
                      item.outOfRange ? `参数异常：${item.label} ${item.valueText}` : item.label
                    "
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
            v-if="isProductionReportHistoryTab && isSubmissionColumnVisible('approvedBy')"
            label="处理人"
            prop="approvedBy"
            :width="getSubmissionColumnWidthString('approvedBy')"
            :min-width="getSubmissionColumnMinWidthString('approvedBy', 140)"
          >
            <template #default="{ row }">
              <span data-team-leader-report-history-handler>
                {{ row.submissionReviewLeaderUserName || row.submissionReviewLeaderUserId || '--' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProductionReportHistoryTab && isSubmissionColumnVisible('approvedAt')"
            label="处理时间"
            prop="approvedAt"
            :width="getSubmissionColumnWidthString('approvedAt')"
            :min-width="getSubmissionColumnMinWidthString('approvedAt', 160)"
          >
            <template #default="{ row }">
              <span data-team-leader-report-history-handled-at>
                {{ formatDateTime(row.submissionReviewedAt) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProductionReportHistoryTab && isSubmissionColumnVisible('reviewStatus')"
            label="处理结果"
            prop="reviewStatus"
            :width="getSubmissionColumnWidthString('reviewStatus')"
            :min-width="getSubmissionColumnMinWidthString('reviewStatus', 120)"
          >
            <template #default="{ row }">
              <el-tag :type="resolveSubmissionReviewTagType(row.submissionReviewStatus)" effect="plain">
                {{ resolveSubmissionReviewStatusText(row.submissionReviewStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProductionReportHistoryTab && isSubmissionColumnVisible('reviewRemark')"
            label="处理说明"
            prop="reviewRemark"
            :width="getSubmissionColumnWidthString('reviewRemark')"
            :min-width="getSubmissionColumnMinWidthString('reviewRemark', 220)"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.submissionReviewRemark || '--' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isPqcFormHistoryTab && isSubmissionColumnVisible('approvedBy')"
            label="审核通过人"
            prop="approvedBy"
            :width="getSubmissionColumnWidthString('approvedBy')"
            :min-width="getSubmissionColumnMinWidthString('approvedBy', 140)"
          >
            <template #default="{ row }">
              <span data-pqc-leader-history-approved-by>
                {{ row.submissionReviewLeaderUserName || row.submissionReviewLeaderUserId || '--' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isPqcFormHistoryTab && isSubmissionColumnVisible('approvedAt')"
            label="审核通过时间"
            prop="approvedAt"
            :width="getSubmissionColumnWidthString('approvedAt')"
            :min-width="getSubmissionColumnMinWidthString('approvedAt', 160)"
          >
            <template #default="{ row }">
              <span data-pqc-leader-history-approved-at>
                {{ formatDateTime(row.submissionReviewedAt) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isSubmissionColumnVisible('operation')"
            label="操作"
            prop="operation"
            :width="
              activeLeaderTab === 'PRODUCTION' ? '190' : getSubmissionColumnWidthString('operation')
            "
            :align="showProductionReportModule ? 'center' : undefined"
            :header-align="showProductionReportModule ? 'center' : undefined"
            fixed="right"
          >
            <template #default="{ row }">
              <div
                :class="{ 'team-leader-workbench__submission-actions': showProductionReportModule }"
              >
                <el-button
                  v-if="!isProductionLeader"
                  link
                  type="primary"
                  :data-team-leader-detail-event-id="String(row.id)"
                  @click="openDetail(row)"
                >
                  详情
                </el-button>
                <el-button
                  v-if="!isProductionLeader && canReviewSubmission(row)"
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
                  修改
                </el-button>
                <el-button
                  v-if="canOpenPqcSubmissionNonconformanceReview(row)"
                  v-hasPermi="['mes:pro-edhr-nonconformance-review:create']"
                  link
                  type="danger"
                  :data-pqc-submission-nonconformance-review-event-id="String(row.id)"
                  @click="openPqcSubmissionNonconformanceReview(row)"
                >
                  不合格审查
                </el-button>
                <el-button
                  v-if="canRejectProductionSubmission(row)"
                  link
                  type="danger"
                  :data-production-report-reject-event-id="String(row.id)"
                  @click="openProductionReject(row)"
                >
                  驳回
                </el-button>
                <el-button
                  v-if="canAllocateSubmission(row)"
                  link
                  type="success"
                  :data-production-report-allocation-event-id="String(row.id)"
                  @click="openAllocation(row)"
                >
                  分配
                </el-button>
              </div>
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
      <el-tab-pane label="历史表单" name="history" data-pqc-leader-module-tab-history />
    </el-tabs>

    <div v-loading="detailLoading" class="team-leader-workbench__detail-tab-body">
      <el-empty v-if="!detail && !detailLoading" description="请先在 PQC管理 列表点击详情" />
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
            {{ detail.actualEmployeeUserName || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="工序">
            {{ detail.processName || detail.processCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="生产工单">
            {{ detail.workOrderCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="!isPqcSubmissionRow(detail)" label="复核日志">
            <div class="team-leader-workbench__review-log" data-team-leader-review-log>
              <el-tag
                :type="resolveSubmissionReviewTagType(detail.submissionReviewStatus)"
                effect="plain"
              >
                {{ resolveSubmissionReviewStatusText(detail.submissionReviewStatus) }}
              </el-tag>
              <span v-if="detail.submissionReviewRemark" class="team-leader-workbench__review-text">
                {{ detail.submissionReviewRemark }}
              </span>
              <span v-if="detail.submissionReviewedAt" class="team-leader-workbench__review-meta">
                复核人 {{ detail.submissionReviewLeaderUserId || '--' }} ·
                {{ formatDateTime(detail.submissionReviewedAt) }}
              </span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.pqcResult || detail.pqcSummary" label="PQC检验内容">
            <el-tag :type="resolvePqcTagType(detail.pqcResult)" effect="plain">
              {{ detail.pqcSummary || detail.pqcResult }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="isPqcSubmissionRow(detail)" class="team-leader-workbench__detail-standard-list">
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
                  <template #default="{ row }">{{
                    row.judgement || row.itemResult || '--'
                  }}</template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
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
      <el-tab-pane
        label="报工历史"
        name="reportHistory"
        data-production-leader-module-tab-report-history
      />
      <el-tab-pane
        label="活跃订单池"
        name="activeOrder"
        data-production-leader-module-tab-active-order
      />
      <el-tab-pane
        label="工序配置"
        name="processConfig"
        data-production-leader-module-tab-process-config
      />
    </el-tabs>
    <div
      v-if="showProductionResponsibleRoutes"
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
        {{ responsibleRouteLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
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
          :row-class-name="resolveActiveOrderRowClassName"
          :show-overflow-tooltip="true"
          data-team-leader-active-order-list
          data-user-table-key="mes.pro.processPool.teamLeader.activeOrders"
        >
          <el-table-column label="活跃池ID" prop="id" width="110">
            <template #default="{ row }">
              <span :data-team-leader-active-order-id="String(row.id)">{{ row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column label="生产订单号" prop="workOrderCode" min-width="200">
            <template #default="{ row }">
              <div class="team-leader-workbench__active-order-code-cell">
                <span
                  data-team-leader-active-order-work-order-code
                  :class="{ 'team-leader-workbench__abnormal-work-order-id': row.abnormal }"
                  :title="row.abnormal ? row.abnormalReason : undefined"
                >
                  {{ row.workOrderCode }}
                </span>
                <el-tag
                  v-if="row.hasQuantityConflict || row.quantityConflict"
                  type="danger"
                  effect="plain"
                  class="team-leader-workbench__active-order-conflict-tag"
                  data-team-leader-active-order-quantity-conflict
                  role="button"
                  tabindex="0"
                  title="点击处理数量冲突"
                  @click.stop="openActiveOrderConflictDrawer(row)"
                  @keydown.enter.prevent.stop="openActiveOrderConflictDrawer(row)"
                  @keydown.space.prevent.stop="openActiveOrderConflictDrawer(row)"
                >
                  数量冲突{{ row.overageQuantity ? ` +${formatTraceQuantity(row.overageQuantity)}` : '' }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="路线名称" prop="routeName" min-width="200" />
          <el-table-column label="版本号" prop="routeVersionNo" min-width="100" />
          <el-table-column label="订单类型" min-width="110">
            <template #default="{ row }">
              <el-tag v-if="row.simulated" type="warning" effect="plain">测试模拟</el-tag>
              <span v-else>正式订单</span>
            </template>
          </el-table-column>
          <el-table-column label="ERP生产数量" min-width="130">
            <template #default="{ row }">
              {{ formatTraceQuantity(row.erpFixedQuantitySnapshot) }}
            </template>
          </el-table-column>
          <el-table-column label="生产进度" prop="productionProgressPercent" min-width="120">
            <template #default="{ row }">
              <span data-team-leader-active-order-production-progress>
                {{ formatActiveOrderProgressPercent(row.productionProgressPercent) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="检验进度" prop="inspectionProgressPercent" min-width="120">
            <template #default="{ row }">
              <span data-team-leader-active-order-inspection-progress>
                {{ formatActiveOrderProgressPercent(row.inspectionProgressPercent) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="放行申请" prop="releaseApplicationStatus" min-width="150">
            <template #default="{ row }">
              <el-tag
                :type="formatActiveOrderReleaseStatusTag(row.releaseApplicationStatus)"
                effect="plain"
                :title="row.releaseSourceSnapshotHash || undefined"
              >
                {{ formatActiveOrderReleaseStatus(row.releaseApplicationStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="加入时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.joinedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="790" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="上移" placement="top">
                <el-button
                  link
                  type="primary"
                  aria-label="上移"
                  title="上移"
                  :disabled="
                    isFirstActiveOrder(row) ||
                    activeOrderMoveSubmittingId !== undefined ||
                    activeOrderSimulationSubmittingId !== undefined
                  "
                  :loading="
                    activeOrderMoveSubmittingId === row.id && activeOrderMoveDirection === 'UP'
                  "
                  data-team-leader-move-active-order-up
                  @click="submitMoveActiveOrder(row, 'UP')"
                >
                  <Icon icon="ep:arrow-up-bold" />
                </el-button>
              </el-tooltip>
              <el-tooltip content="下移" placement="top">
                <el-button
                  link
                  type="primary"
                  aria-label="下移"
                  title="下移"
                  :disabled="
                    isLastActiveOrder(row) ||
                    activeOrderMoveSubmittingId !== undefined ||
                    activeOrderSimulationSubmittingId !== undefined
                  "
                  :loading="
                    activeOrderMoveSubmittingId === row.id && activeOrderMoveDirection === 'DOWN'
                  "
                  data-team-leader-move-active-order-down
                  @click="submitMoveActiveOrder(row, 'DOWN')"
                >
                  <Icon icon="ep:arrow-down-bold" />
                </el-button>
              </el-tooltip>
              <el-button
                link
                type="primary"
                data-team-leader-active-order-detail
                @click="openActiveOrderSubmissionDetail(row)"
              >
                详情
              </el-button>
              <el-button
                link
                type="danger"
                :loading="maintenanceSubmitting"
                :disabled="
                  activeOrderRebuildSubmittingId !== undefined ||
                  activeOrderSimulationSubmittingId !== undefined
                "
                data-team-leader-remove-active-order
                @click="handleRemoveActiveOrder(row)"
              >
                移除
              </el-button>
              <el-button
                link
                type="warning"
                :disabled="row.abnormal || activeOrderSimulationSubmittingId !== undefined"
                :loading="abnormalSubmitting && abnormalForm.workOrderId === row.workOrderId"
                :title="
                  row.abnormal ? row.abnormalReason || '该订单已报异常' : '针对该活跃订单报异常'
                "
                data-team-leader-report-active-order-abnormal
                @click="openAbnormalDialog(row)"
              >
                异常
              </el-button>
              <el-button
                link
                type="primary"
                :disabled="
                  !canApplyActiveOrderRelease(row) || isActiveOrderReleaseApplicationLocked(row.id)
                    || activeOrderSimulationSubmittingId !== undefined
                "
                :loading="releaseApplicationSubmittingId === row.id"
                :title="resolveActiveOrderReleaseApplyDisabledReason(row)"
                data-team-leader-active-order-release-apply
                @click="submitActiveOrderReleaseApplication(row)"
              >
                完工
              </el-button>
              <el-button
                link
                type="success"
                :loading="activeOrderRebuildSubmittingId === row.id"
                :disabled="
                  maintenanceSubmitting ||
                  activeOrderRebuildSubmittingId !== undefined ||
                  activeOrderSimulationSubmittingId !== undefined
                "
                data-team-leader-rebuild-active-order
                @click="handleRebuildActiveOrder(row)"
              >
                重建
              </el-button>
              <el-button
                v-hasPermi="['mes:pro-process-pool-team-leader:version-upgrade']"
                link
                type="warning"
                :loading="activeOrderVersionUpgradeSubmittingId === row.id"
                :disabled="
                  maintenanceSubmitting ||
                  row.activeStatus !== 'ACTIVE' ||
                  activeOrderRebuildSubmittingId !== undefined ||
                  activeOrderSimulationSubmittingId !== undefined ||
                  activeOrderVersionUpgradeSubmittingId !== undefined
                "
                title="按全部最新正式版本发起版本升级重启审批"
                data-team-leader-active-order-version-upgrade
                @click="handleActiveOrderVersionUpgrade(row)"
              >升级</el-button>
              <el-button
                link
                type="primary"
                :loading="activeOrderSimulationSubmittingId === row.id"
                :disabled="
                  maintenanceSubmitting ||
                  activeOrderRebuildSubmittingId !== undefined ||
                  activeOrderSimulationSubmittingId !== undefined
                "
                data-team-leader-copy-latest-simulation-order
                @click="handleCopyLatestSimulationActiveOrder(row)"
              >
                <Icon icon="ep:document-copy" />
                复制测试单
              </el-button>
              <el-button
                v-if="row.simulated && row.simulationStage === 'LATEST_VERSION_COPY'"
                link
                type="danger"
                :loading="activeOrderSimulationSubmittingId === row.id"
                :disabled="
                  maintenanceSubmitting ||
                  activeOrderRebuildSubmittingId !== undefined ||
                  activeOrderSimulationSubmittingId !== undefined
                "
                data-team-leader-cleanup-latest-simulation-order
                @click="handleCleanupLatestSimulationActiveOrder(row)"
              >
                清理测试单
              </el-button>
              <el-button
                link
                type="primary"
                :loading="activeOrderSimulationSubmittingId === row.id"
                :disabled="
                  maintenanceSubmitting ||
                  activeOrderRebuildSubmittingId !== undefined ||
                  activeOrderSimulationSubmittingId !== undefined
                "
                data-team-leader-simulate-active-order-stage1
                @click="handleSimulateStage1(row)"
              >
                <Icon icon="ep:refresh" />
                Stage1模拟
              </el-button>
              <el-button
                link
                type="success"
                :loading="activeOrderSimulationSubmittingId === row.id"
                :disabled="
                  maintenanceSubmitting ||
                  activeOrderRebuildSubmittingId !== undefined ||
                  activeOrderSimulationSubmittingId !== undefined
                "
                data-team-leader-simulate-active-order-stage2-5
                title="模拟完工并打开真实批次执行详情"
                @click="handleSimulateStage2_5(row)"
              >
                <Icon icon="ep:finished" />
                模拟完工
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
    <el-alert
      v-if="releaseApplicationBlockers.length"
      class="mt-12px"
      title="申请放行阻塞"
      type="warning"
      :closable="true"
      show-icon
      @close="releaseApplicationBlockers = []"
    >
      <div
        v-for="blocker in releaseApplicationBlockers"
        :key="`${blocker.blockerType}-${blocker.objectType}-${blocker.objectId}`"
        data-team-leader-active-order-release-blocker
      >
        <el-tag
          size="small"
          type="warning"
          effect="plain"
          data-team-leader-active-order-release-blocker-type
        >
          {{ blocker.blockerType }}
        </el-tag>
        <span> {{ blocker.reason }}；{{ blocker.suggestion }}</span>
        <span data-team-leader-active-order-release-blocker-locator>
          ；{{ resolveActiveOrderReleaseBlockerLocator(blocker) }}
        </span>
      </div>
    </el-alert>
    <el-alert
      v-if="releaseApplicationUncertainMessage"
      class="mt-12px"
      :title="releaseApplicationUncertainMessage"
      type="error"
      :closable="false"
      show-icon
      data-team-leader-active-order-release-uncertain
    />

    <el-drawer
      v-model="activeOrderConflictDrawerVisible"
      data-team-leader-active-order-conflict-drawer
      class="team-leader-workbench__active-order-conflict-drawer-panel"
      :title="
        activeOrderConflictDetail
          ? `订单 ${activeOrderConflictDetail.workOrderCode} · 数量冲突处理`
          : '数量冲突处理'
      "
      size="760px"
      destroy-on-close
    >
      <div
        v-loading="activeOrderConflictLoading"
        class="team-leader-workbench__active-order-conflict-drawer"
      >
        <el-alert
          v-if="activeOrderConflictError"
          :title="activeOrderConflictError"
          type="error"
          :closable="false"
          show-icon
        >
          <template #default>
            <el-button link type="primary" @click="retryActiveOrderConflictDetail">
              重新加载
            </el-button>
          </template>
        </el-alert>

        <template v-else-if="activeOrderConflictDetail">
          <div
            class="team-leader-workbench__active-order-conflict-summary"
            data-team-leader-active-order-conflict-summary
          >
            <div>
              <span>生产订单</span>
              <strong>{{ activeOrderConflictDetail.workOrderCode }}</strong>
            </div>
            <div>
              <span>工艺路线</span>
              <strong>{{ activeOrderConflictDetail.routeName }}</strong>
            </div>
            <div>
              <span>冲突工序</span>
              <strong class="team-leader-workbench__quantity-conflict-text">
                {{ resolveActiveOrderConflictCount(activeOrderConflictDetail) }}
              </strong>
            </div>
            <div>
              <span>正常工序</span>
              <strong>{{ resolveActiveOrderNormalProcessCount(activeOrderConflictDetail) }}</strong>
            </div>
          </div>

          <div class="team-leader-workbench__active-order-conflict-actions">
            <el-button
              type="primary"
              :loading="activeOrderConflictSubmitting"
              :disabled="resolveActiveOrderConflictCount(activeOrderConflictDetail) === 0"
              data-team-leader-active-order-conflict-recommended-action
              @click="handleRecommendedActiveOrderConflictResolution"
            >
              按推荐方案修复
            </el-button>
          </div>

          <div class="team-leader-workbench__active-order-conflict-process-list">
            <section
              v-for="process in resolveActiveOrderConflictProcesses(activeOrderConflictDetail)"
              :key="`${process.routeProcessId}-${process.processId}`"
              class="team-leader-workbench__active-order-conflict-process"
              :class="{ 'is-quantity-conflict': process.quantityConflict }"
              data-team-leader-active-order-conflict-process
            >
              <div class="team-leader-workbench__active-order-conflict-process-header">
                <div class="team-leader-workbench__active-order-process-title">
                  <strong>{{ process.processName }}</strong>
                  <span v-if="process.processCode">{{ process.processCode }}</span>
                </div>
                <el-tag
                  :type="process.quantityConflict ? 'danger' : 'success'"
                  effect="plain"
                  size="small"
                >
                  {{ process.quantityConflict ? '冲突工序' : '无冲突' }}
                </el-tag>
              </div>
              <div class="team-leader-workbench__active-order-conflict-process-metrics">
                <div>
                  <span>应提数量</span>
                  <strong>{{ formatTraceQuantity(process.requiredQuantity) }}</strong>
                </div>
                <div>
                  <span>已提交</span>
                  <strong>{{ formatTraceQuantity(process.submittedQuantity) }}</strong>
                </div>
                <div>
                  <span>超出数量</span>
                  <strong :class="{ 'team-leader-workbench__quantity-conflict-text': process.quantityConflict }">
                    {{ process.quantityConflict ? formatTraceQuantity(process.overageQuantity) : '-' }}
                  </strong>
                </div>
                <div>
                  <span>提交记录</span>
                  <strong>{{ process.submissionCount }}</strong>
                </div>
              </div>
            </section>
          </div>
        </template>
      </div>
    </el-drawer>

    <el-dialog
      v-model="activeOrderAddDialogVisible"
      data-team-leader-active-order-add-dialog
      title="新增活跃订单"
      width="560px"
      :close-on-click-modal="!maintenanceSubmitting"
      @closed="resetActiveOrderForm"
    >
      <el-form :model="activeOrderForm" label-width="110px">
        <el-form-item label="订单号/产品" data-team-leader-active-order-work-order-code>
          <el-select
            v-model="activeOrderForm.workOrderId"
            filterable
            remote
            clearable
            reserve-keyword
            :teleported="false"
            :remote-method="searchActiveOrderCandidates"
            :loading="activeOrderCandidateLoading"
            placeholder="请输入订单号、产品编码或产品名称"
            class="team-leader-workbench__full-control"
            popper-class="team-leader-workbench__active-order-candidate-popper"
            @change="handleActiveOrderCandidateChange"
            @clear="handleActiveOrderCandidateClear"
          >
            <el-option
              v-for="candidate in activeOrderCandidateOptions"
              :key="candidate.workOrderId"
              :label="candidate.workOrderCode"
              :value="candidate.workOrderId"
              :disabled="!candidate.eligible"
            >
              <div
                class="team-leader-workbench__active-order-candidate"
                :class="{ 'is-eligible': candidate.eligible, 'is-blocked': !candidate.eligible }"
              >
                <span class="team-leader-workbench__active-order-candidate-code">
                  {{ candidate.workOrderCode }}
                </span>
                <span
                  v-if="candidate.candidateState === 'REUSABLE'"
                  class="team-leader-workbench__active-order-candidate-badge"
                >
                  可复用
                </span>
                <span
                  v-else-if="candidate.candidateState === 'RECOVERABLE'"
                  class="team-leader-workbench__active-order-candidate-badge"
                >
                  可恢复
                </span>
                <span
                  v-else-if="candidate.eligible"
                  class="team-leader-workbench__active-order-candidate-badge"
                >
                  符合要求
                </span>
                <el-tooltip
                  v-else
                  :content="candidate.ineligibleReason || '暂不符合'"
                  placement="top"
                  :show-after="200"
                >
                  <span
                    tabindex="0"
                    :aria-label="candidate.ineligibleReason || '暂不符合'"
                    data-team-leader-active-order-blocked-reason
                    class="team-leader-workbench__active-order-candidate-reason"
                  >
                    {{ candidate.ineligibleReason || '暂不符合' }}
                  </span>
                </el-tooltip>
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
          {{ activeOrderSubmitLabel }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="abnormalDialogVisible"
      data-team-leader-abnormal-report-dialog
      title="报异常"
      width="520px"
      :close-on-click-modal="!abnormalSubmitting"
      @closed="resetAbnormalForm"
    >
      <el-form
        ref="abnormalFormRef"
        :model="abnormalForm"
        :rules="abnormalRules"
        label-width="100px"
      >
        <el-form-item label="生产订单ID">
          <el-input :model-value="abnormalForm.workOrderId" disabled />
        </el-form-item>
        <el-form-item label="异常原因" prop="abnormalDescription">
          <el-input
            v-model="abnormalForm.abnormalDescription"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入异常原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="abnormalSubmitting" @click="abnormalDialogVisible = false">
          取消
        </el-button>
        <el-button type="warning" :loading="abnormalSubmitting" @click="submitAbnormal">
          确认报异常
        </el-button>
      </template>
    </el-dialog>
  </ContentWrap>

  <ContentWrap
    v-if="showLegacyDailyCloseDashboardModule"
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
      <el-tab-pane
        label="报工历史"
        name="reportHistory"
        data-production-leader-module-tab-report-history
      />
      <el-tab-pane
        label="活跃订单池"
        name="activeOrder"
        data-production-leader-module-tab-active-order
      />
      <el-tab-pane
        label="工序配置"
        name="processConfig"
        data-production-leader-module-tab-process-config
      />
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
        {{ responsibleRouteLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
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
      <el-tab-pane label="历史表单" name="history" data-pqc-leader-module-tab-history />
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
      <el-tab-pane
        label="报工历史"
        name="reportHistory"
        data-production-leader-module-tab-report-history
      />
      <el-tab-pane
        label="活跃订单池"
        name="activeOrder"
        data-production-leader-module-tab-active-order
      />
      <el-tab-pane
        label="工序配置"
        name="processConfig"
        data-production-leader-module-tab-process-config
      />
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
        {{ responsibleRouteLoading ? '工艺路线加载中' : '暂无负责工艺路线' }}
      </span>
    </div>
    <div
      class="team-leader-workbench__section-head team-leader-workbench__process-config-filter-head"
    >
      <TableMultiFilter
        class="team-leader-workbench__process-config-filter"
        table-key="mes.processPool.teamLeader.processConfig"
        :filter-definitions="processConfigFilterDefinitions"
        :state="processConfigFilterState"
        :show-operators="false"
        @update:state="updateProcessConfigFilterState"
        @query="applyProcessConfigFilter"
        @reset="resetProcessConfigFilter"
        @remove="removeProcessConfigFilterCondition"
      />
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
      :data="processConfigDisplayRows"
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
      <el-table-column label="允许超量比例" width="180">
        <template #default="{ row }">
          <div class="team-leader-workbench__overage-limit" data-team-leader-process-config-overage-limit>
            <el-input-number
              v-model="row.overagePercent"
              :min="0"
              :max="100"
              :precision="2"
              :step="1"
              controls-position="right"
              @change="saveProcessConfigOverageLimit(row)"
            />
            <span>%</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="损耗原因" min-width="260">
        <template #default="{ row }">
          <div
            class="team-leader-workbench__loss-reasons"
            data-team-leader-process-config-loss-reasons
          >
            <el-tag
              v-for="reason in row.lossReasons"
              :key="reason.id"
              :type="reason.enabled ? 'success' : 'info'"
              effect="plain"
            >
              {{ reason.reasonName }}{{ reason.enabled ? '' : '（停用）' }}
            </el-tag>
            <span v-if="!row.lossReasons?.length" class="team-leader-workbench__hint"
              >暂无损耗原因</span
            >
          </div>
        </template>
      </el-table-column>
      <el-table-column label="映射设备" min-width="280">
        <template #default="{ row }">
          <div
            class="team-leader-workbench__process-config-devices"
            data-team-leader-process-config-devices
          >
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
          <div
            class="team-leader-workbench__process-config-parameters"
            data-team-leader-process-config-parameters
          >
            <template v-for="device in row.devices" :key="`params-${device.deviceId}`">
              <div
                v-for="parameter in device.parameters"
                :key="`${device.deviceId}-${parameter.parameterCode}`"
                class="team-leader-workbench__process-config-parameter"
              >
                <span class="team-leader-workbench__process-config-parameter-name">
                  {{ parameter.parameterName || parameter.parameterCode }}
                </span>
                <span data-team-leader-process-config-standard-text>
                  {{ parameter.standardText }}
                </span>
                <template v-if="isProcessConfigNumericParameter(parameter)">
                  <span>平均 {{ formatProcessConfigAverage(parameter) }}</span>
                  <span>样本 {{ parameter.sampleCount ?? 0 }}</span>
                  <span>{{ formatProcessConfigStatisticsWindow(parameter) }}</span>
                </template>
                <template v-else-if="parameter.valueType === 'SELECT'">
                  <span>默认 {{ parameter.defaultText || '--' }}</span>
                  <span>选项 {{ formatProcessConfigOptionValues(parameter) }}</span>
                </template>
                <template v-else-if="parameter.valueType === 'BOOLEAN'">
                  <span>默认 {{ formatProcessConfigBooleanDefault(parameter) }}</span>
                </template>
              </div>
            </template>
            <span v-if="!hasProcessConfigParameters(row)" class="team-leader-workbench__hint">
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
              data-team-leader-process-config-manage-loss
              :data-route-process-id="row.routeProcessId"
              @click="openLossReasonMaintenanceDialog(row)"
            >
              损耗
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
              @click="openProcessConfigParameterMaintenance(row, device)"
            >
              {{ resolveCleaningWashProcessConfig(row, device)?.buttonLabel || '参数标准' }}
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
          title="选择后将打开对应维护弹窗；保存时继续使用正式设备映射和设备参数接口。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="processConfigCreateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreateProcessConfigData"> 下一步 </el-button>
      </template>
    </el-dialog>
  </ContentWrap>
  <ContentWrap v-if="showProductionConfigModule" data-team-leader-config-center>
    <div class="team-leader-workbench__section-head">
      <div>
        <div class="team-leader-workbench__section-title">班组配置中心</div>
        <div class="team-leader-workbench__hint"> 维护设备、参数和工序异常关系。 </div>
      </div>
    </div>
    <div class="team-leader-workbench__maintenance-grid">
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
            <el-button
              type="warning"
              :loading="maintenanceSubmitting"
              @click="submitTeamDeviceStatus"
            >
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
          {{ detail.actualEmployeeUserName || '--' }}
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
            {{ detail.actualEmployeeUserName || '--' }}
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
    v-model="lossReasonMaintenanceDialogVisible"
    title="维护损耗"
    width="min(760px, calc(100vw - 32px))"
    destroy-on-close
    :close-on-click-modal="!lossReasonSubmitting"
    :close-on-press-escape="!lossReasonSubmitting"
    :show-close="!lossReasonSubmitting"
    data-loss-reason-maintenance-dialog
    @closed="resetLossReasonMaintenance"
  >
    <div class="team-leader-workbench__loss-maintenance-context">
      <span>
        <strong>工艺路线：</strong>
        {{ lossReasonMaintenanceRow?.routeName || lossReasonMaintenanceRow?.routeCode || '--' }}
      </span>
      <span>
        <strong>工序：</strong>
        {{ lossReasonMaintenanceRow ? formatProcessConfigProcess(lossReasonMaintenanceRow) : '--' }}
      </span>
    </div>
    <el-table
      :data="lossReasonMaintenanceReasons"
      row-key="id"
      border
      size="small"
      empty-text="当前工序暂无损耗原因"
      data-loss-reason-maintenance-table
    >
      <el-table-column label="损耗描述" min-width="310">
        <template #default="{ row }">
          <div
            v-if="isLossReasonEditing(row)"
            class="team-leader-workbench__loss-maintenance-editor"
          >
            <el-input
              v-model="lossReasonForm.reasonName"
              maxlength="255"
              aria-label="损耗描述"
              placeholder="请输入损耗描述"
              data-loss-reason-inline-name
            />
            <el-input
              v-model="lossReasonForm.remark"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              aria-label="维护说明"
              placeholder="请输入维护说明（选填）"
              data-loss-reason-inline-remark
            />
          </div>
          <span v-else>{{ row.reasonName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="启用状态" width="120" align="center">
        <template #default="{ row }">
          <el-switch
            v-if="isLossReasonEditing(row)"
            v-model="lossReasonForm.enabled"
            active-text="启用"
            inactive-text="停用"
            aria-label="启用状态"
            data-loss-reason-inline-enabled
          />
          <el-tag v-else :type="row.enabled ? 'success' : 'info'" effect="plain">
            {{ row.enabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <div class="team-leader-workbench__loss-maintenance-actions">
            <template v-if="isLossReasonEditing(row)">
              <el-button
                link
                type="primary"
                :loading="lossReasonSubmitting"
                data-loss-reason-inline-save-edit
                @click="submitLossReason"
              >
                保存
              </el-button>
              <el-button
                link
                :disabled="lossReasonSubmitting"
                data-loss-reason-inline-cancel-edit
                @click="cancelLossReasonEditor"
              >
                取消
              </el-button>
            </template>
            <template v-else>
              <el-button
                link
                type="warning"
                :disabled="lossReasonEditorActive || lossReasonSubmitting"
                data-loss-reason-inline-edit
                @click="startEditLossReason(row)"
              >
                修改
              </el-button>
              <el-button
                link
                type="danger"
                :disabled="lossReasonEditorActive || lossReasonSubmitting"
                data-loss-reason-inline-delete
                @click="handleDeleteLossReason(row)"
              >
                删除
              </el-button>
            </template>
          </div>
        </template>
      </el-table-column>
      <template #append>
        <div
          v-if="lossReasonDialogMode === 'create'"
          class="team-leader-workbench__loss-maintenance-create-row"
          data-loss-reason-inline-create-row
        >
          <el-input
            v-model="lossReasonForm.reasonName"
            maxlength="255"
            aria-label="新增损耗描述"
            placeholder="请输入损耗描述"
            data-loss-reason-inline-create-name
          />
          <el-tag type="success" effect="plain">启用</el-tag>
          <div class="team-leader-workbench__loss-maintenance-actions">
            <el-button
              link
              type="primary"
              :loading="lossReasonSubmitting"
              data-loss-reason-inline-save-create
              @click="submitLossReason"
            >
              保存
            </el-button>
            <el-button
              link
              :disabled="lossReasonSubmitting"
              data-loss-reason-inline-cancel-create
              @click="cancelLossReasonEditor"
            >
              取消
            </el-button>
          </div>
        </div>
      </template>
    </el-table>
    <div class="team-leader-workbench__loss-maintenance-toolbar">
      <el-button
        type="primary"
        :disabled="lossReasonEditorActive || lossReasonSubmitting"
        data-loss-reason-inline-add
        @click="startCreateLossReason"
      >
        <Icon icon="ep:plus" class="mr-5px" />
        新增
      </el-button>
    </div>
    <template #footer>
      <el-button
        :disabled="lossReasonSubmitting"
        @click="lossReasonMaintenanceDialogVisible = false"
      >
        关闭
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
        <span>{{
          processConfigSelectedRow?.routeName || processConfigSelectedRow?.routeCode || '--'
        }}</span>
      </el-form-item>
      <el-form-item label="工序">
        <span>{{
          processConfigSelectedRow ? formatProcessConfigProcess(processConfigSelectedRow) : '--'
        }}</span>
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
    v-model="roughWashParameterDialogVisible"
    :title="activeCleaningWashProcessConfig?.dialogTitle || '清洗参数配置'"
    width="min(780px, calc(100vw - 32px))"
    top="16px"
    class="team-leader-workbench__rough-wash-dialog"
    :close-on-click-modal="!processConfigSubmitting"
    destroy-on-close
    data-team-leader-rough-wash-config-dialog
    data-team-leader-cleaning-wash-config-dialog
    :data-cleaning-wash-kind="activeCleaningWashProcessConfig?.kind"
  >
    <div class="team-leader-workbench__rough-wash-context">
      <span>
        <b>工艺路线</b>
        {{ processConfigSelectedRow?.routeName || processConfigSelectedRow?.routeCode || '--' }}
      </span>
      <span>
        <b>工序</b>
        {{ processConfigSelectedRow ? formatProcessConfigProcess(processConfigSelectedRow) : '--' }}
      </span>
      <span>
        <b>设备</b>
        {{
          processConfigSelectedDevice
            ? formatProcessConfigDevice(processConfigSelectedDevice)
            : '--'
        }}
      </span>
    </div>

    <section
      class="team-leader-workbench__rough-wash-section"
      aria-labelledby="rough-wash-config-title"
    >
      <h3 id="rough-wash-config-title">参数配置</h3>
      <div class="team-leader-workbench__rough-wash-config">
        <div class="team-leader-workbench__rough-wash-row">
          <div class="team-leader-workbench__rough-wash-label">
            <strong>清洗次数</strong>
            <span>整数 · 无上下限</span>
          </div>
          <div class="team-leader-workbench__rough-wash-control">
            <el-input-number
              v-model="roughWashParameterForm.cleaningCount"
              :precision="0"
              :step="1"
              aria-label="清洗次数默认"
              data-rough-wash-cleaning-count
            />
            <span class="team-leader-workbench__rough-wash-unit">次</span>
          </div>
        </div>

        <div class="team-leader-workbench__rough-wash-row">
          <div class="team-leader-workbench__rough-wash-label">
            <strong>清洗介质</strong>
            <span>默认选项</span>
          </div>
          <div class="team-leader-workbench__rough-wash-control">
            <el-select
              v-model="roughWashParameterForm.cleaningMedium"
              aria-label="清洗介质默认选项"
              data-rough-wash-cleaning-medium
            >
              <el-option label="自来水" value="自来水" />
              <el-option label="纯化水" value="纯化水" />
            </el-select>
          </div>
        </div>

        <div class="team-leader-workbench__rough-wash-row">
          <div class="team-leader-workbench__rough-wash-label">
            <strong>清洗功率</strong>
            <span>整数范围</span>
          </div>
          <div
            class="team-leader-workbench__rough-wash-range team-leader-workbench__rough-wash-range--three"
          >
            <label>
              <span>下限</span>
              <el-input-number
                v-model="roughWashParameterForm.powerLower"
                :controls="false"
                :precision="0"
                aria-label="清洗功率下限"
                data-rough-wash-power-lower
              />
            </label>
            <label>
              <span>默认</span>
              <el-input-number
                v-model="roughWashParameterForm.powerDefault"
                :controls="false"
                :precision="0"
                aria-label="清洗功率默认"
                data-rough-wash-power-default
              />
            </label>
            <label>
              <span>上限</span>
              <el-input-number
                v-model="roughWashParameterForm.powerUpper"
                :controls="false"
                :precision="0"
                aria-label="清洗功率上限"
                data-rough-wash-power-upper
              />
            </label>
            <span class="team-leader-workbench__rough-wash-unit">%</span>
          </div>
        </div>

        <div class="team-leader-workbench__rough-wash-row">
          <div class="team-leader-workbench__rough-wash-label">
            <strong>室温</strong>
            <span>保留 1 位小数</span>
          </div>
          <div
            class="team-leader-workbench__rough-wash-range team-leader-workbench__rough-wash-range--three"
          >
            <label>
              <span>下限</span>
              <el-input-number
                v-model="roughWashParameterForm.roomTemperatureLower"
                :controls="false"
                :precision="1"
                :step="0.1"
                aria-label="室温下限"
                data-rough-wash-room-temperature-lower
              />
            </label>
            <label>
              <span>默认</span>
              <el-input-number
                v-model="roughWashParameterForm.roomTemperatureDefault"
                :controls="false"
                :precision="1"
                :step="0.1"
                aria-label="室温默认"
                data-rough-wash-room-temperature-default
              />
            </label>
            <label>
              <span>上限</span>
              <el-input-number
                v-model="roughWashParameterForm.roomTemperatureUpper"
                :controls="false"
                :precision="1"
                :step="0.1"
                aria-label="室温上限"
                data-rough-wash-room-temperature-upper
              />
            </label>
            <span class="team-leader-workbench__rough-wash-unit">℃</span>
          </div>
        </div>

        <div class="team-leader-workbench__rough-wash-row">
          <div class="team-leader-workbench__rough-wash-label">
            <strong>清洗时间</strong>
            <span>整数 · 无上下限</span>
          </div>
          <div class="team-leader-workbench__rough-wash-control">
            <el-input-number
              v-model="roughWashParameterForm.cleaningTime"
              :precision="0"
              :step="1"
              aria-label="清洗时间默认"
              data-rough-wash-cleaning-time
            />
            <span class="team-leader-workbench__rough-wash-unit">min</span>
          </div>
        </div>
      </div>
    </section>

    <section
      class="team-leader-workbench__rough-wash-section team-leader-workbench__rough-wash-preview"
      aria-labelledby="rough-wash-preview-title"
      data-team-leader-rough-wash-frontline-preview
    >
      <h3 id="rough-wash-preview-title">一线填设备预览</h3>
      <div class="team-leader-workbench__rough-wash-preview-grid">
        <label>
          <span>清洗次数</span>
          <el-input-number
            :model-value="roughWashParameterForm.cleaningCount"
            :precision="0"
            disabled
            aria-label="预览清洗次数"
          />
        </label>
        <label>
          <span>清洗介质</span>
          <el-select
            :model-value="roughWashParameterForm.cleaningMedium"
            disabled
            aria-label="预览清洗介质"
          >
            <el-option label="自来水" value="自来水" />
            <el-option label="纯化水" value="纯化水" />
          </el-select>
        </label>
        <label>
          <span>清洗功率</span>
          <el-input-number
            :model-value="roughWashParameterForm.powerDefault"
            :controls="false"
            :precision="0"
            disabled
            aria-label="预览清洗功率"
          />
        </label>
        <label>
          <span>室温</span>
          <el-input-number
            :model-value="roughWashParameterForm.roomTemperatureDefault"
            :controls="false"
            :precision="1"
            disabled
            aria-label="预览室温"
          />
        </label>
        <label>
          <span>清洗时间</span>
          <el-input-number
            :model-value="roughWashParameterForm.cleaningTime"
            :precision="0"
            disabled
            aria-label="预览清洗时间"
          />
        </label>
      </div>
    </section>

    <template #footer>
      <el-button
        :disabled="processConfigSubmitting"
        @click="roughWashParameterDialogVisible = false"
      >
        取消
      </el-button>
      <el-button
        type="primary"
        :loading="processConfigSubmitting"
        data-team-leader-rough-wash-save
        @click="submitRoughWashParameterConfig"
      >
        保存配置
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
        <span>{{
          processConfigSelectedRow?.routeName || processConfigSelectedRow?.routeCode || '--'
        }}</span>
      </el-form-item>
      <el-form-item label="工序">
        <span>{{
          processConfigSelectedRow ? formatProcessConfigProcess(processConfigSelectedRow) : '--'
        }}</span>
      </el-form-item>
      <el-form-item label="设备">
        <span>{{
          processConfigSelectedDevice
            ? formatProcessConfigDevice(processConfigSelectedDevice)
            : '--'
        }}</span>
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
          <el-option label="下拉框" value="SELECT" />
          <el-option label="勾选" value="BOOLEAN" />
          <el-option label="文本标准" value="TEXT_STANDARD" />
        </el-select>
      </el-form-item>
      <el-form-item label="原文标准" required>
        <el-input
          v-model="processConfigParameterForm.standardText"
          type="textarea"
          :rows="2"
          maxlength="1000"
          show-word-limit
          data-team-leader-process-config-standard-text-input
        />
      </el-form-item>
      <el-form-item
        v-if="processConfigParameterForm.valueType === 'SELECT'"
        label="下拉选项"
        required
      >
        <el-select
          v-model="processConfigParameterForm.optionValues"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="请输入或选择选项"
          data-team-leader-process-config-option-values
        >
          <el-option
            v-for="option in processConfigParameterForm.optionValues"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="processConfigParameterForm.valueType === 'SELECT'" label="默认文本">
        <el-select
          v-model="processConfigParameterForm.defaultText"
          clearable
          filterable
          allow-create
          default-first-option
          placeholder="请选择默认文本"
          data-team-leader-process-config-default-text
        >
          <el-option
            v-for="option in processConfigParameterForm.optionValues"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="processConfigParameterForm.valueType === 'BOOLEAN'" label="默认状态">
        <el-checkbox
          v-model="processConfigParameterForm.booleanDefault"
          data-team-leader-process-config-boolean-default
        >
          默认勾选
        </el-checkbox>
      </el-form-item>
      <el-form-item
        v-if="isProcessConfigNumericValueType(processConfigParameterForm.valueType)"
        label="下限"
      >
        <el-input-number
          v-model="processConfigParameterForm.lowerLimit"
          :controls="false"
          data-team-leader-process-config-lower-limit
        />
      </el-form-item>
      <el-form-item
        v-if="isProcessConfigNumericValueType(processConfigParameterForm.valueType)"
        label="目标值"
      >
        <el-input-number
          v-model="processConfigParameterForm.targetValue"
          :controls="false"
          data-team-leader-process-config-target-value
        />
      </el-form-item>
      <el-form-item
        v-if="isProcessConfigNumericValueType(processConfigParameterForm.valueType)"
        label="上限"
      >
        <el-input-number
          v-model="processConfigParameterForm.upperLimit"
          :controls="false"
          data-team-leader-process-config-upper-limit
        />
      </el-form-item>
      <el-form-item v-if="processConfigParameterForm.valueType === 'DECIMAL'" label="小数位数">
        <el-input-number
          v-model="processConfigParameterForm.decimalScale"
          :min="0"
          :max="6"
          :precision="0"
          data-team-leader-process-config-decimal-scale
        />
      </el-form-item>
      <el-form-item
        v-if="isProcessConfigNumericValueType(processConfigParameterForm.valueType)"
        label="实际平均值"
      >
        <span data-team-leader-process-config-average-readonly>
          {{
            processConfigEditingParameter
              ? formatProcessConfigAverage(processConfigEditingParameter)
              : '暂无样本'
          }}
        </span>
      </el-form-item>
      <el-form-item
        v-if="isProcessConfigNumericValueType(processConfigParameterForm.valueType)"
        label="样本数"
      >
        <span>{{ processConfigEditingParameter?.sampleCount ?? 0 }}</span>
      </el-form-item>
      <el-form-item
        v-if="isProcessConfigNumericValueType(processConfigParameterForm.valueType)"
        label="统计周期"
      >
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

  <el-dialog
    v-model="reviewVisible"
    :title="reviewDialogTitle"
    width="min(1120px, calc(100vw - 32px))"
    class="team-leader-workbench__review-dialog"
  >
    <el-form v-if="reviewDialogMode !== 'ALLOCATION'" :model="reviewForm" label-width="92px">
      <el-form-item v-if="reviewDialogMode === 'REVIEW'" label="判定结果">
        <el-select v-model="reviewForm.reviewStatus">
          <el-option label="正确" value="APPROVED" />
          <el-option label="不正确" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item :label="reviewDialogMode === 'REJECTION' ? '驳回原因' : '复核说明'">
        <el-input
          v-model="reviewForm.reviewRemark"
          type="textarea"
          :rows="4"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="电子签名" required data-team-leader-review-signature>
        <el-input
          v-model="reviewForm.reviewSignaturePassword"
          type="password"
          show-password
          autocomplete="new-password"
          placeholder="请输入当前登录密码完成电子签名"
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
          <el-button @click="startBlankAllocation">从空白开始</el-button>
        </div>
      </div>
      <div
        v-if="reviewMaterialRows.length || reviewDeviceItems.length || reviewParameterItems.length"
        class="team-leader-workbench__allocation-context"
      >
        <div
          v-if="reviewMaterialRows.length"
          class="team-leader-workbench__context-block"
          data-team-leader-allocation-material-context
        >
          <strong>物料</strong>
          <span
            v-for="materialRow in reviewMaterialRows"
            :key="materialRow.materialId"
            class="team-leader-workbench__structured-pill"
          >
            {{ materialRow.materialName || materialRow.materialCode }}（{{
              correctionValueText(materialRow.outputQuantity)
            }}/{{ correctionValueText(materialRow.lossQuantity) }}）
          </span>
        </div>
        <div
          v-if="reviewDeviceItems.length"
          class="team-leader-workbench__context-block"
          data-team-leader-allocation-devices
        >
          <strong>设备</strong>
          <span
            v-for="item in reviewDeviceItems"
            :key="item.key"
            class="team-leader-workbench__structured-pill"
          >
            {{ item.valueText }}
          </span>
        </div>
        <div
          v-if="reviewParameterItems.length"
          class="team-leader-workbench__context-block"
          data-team-leader-allocation-parameters
        >
          <strong>设备参数</strong>
          <span
            v-for="item in reviewParameterItems"
            :key="item.key"
            class="team-leader-workbench__structured-pill"
          >
            {{ item.label }}：{{ item.valueText }}
          </span>
        </div>
      </div>
      <el-table
        data-team-leader-allocation-table
        :data="allocationRows"
        class="team-leader-workbench__allocation-table"
        border
        size="small"
        table-layout="fixed"
        empty-text="请点击 FIFO 自动分配或手动新增分配行"
      >
        <el-table-column label="活跃订单" min-width="360">
          <template #default="{ row }">
            <el-select
              v-model="row.activeOrderId"
              class="team-leader-workbench__allocation-order-select"
              :disabled="row.editable === false"
              filterable
              popper-class="team-leader-workbench__allocation-order-popper"
              placeholder="请选择活跃订单"
              @change="markManualAllocation"
            >
              <template #label="{ label }">
                <span class="team-leader-workbench__allocation-order-label">{{ label }}</span>
              </template>
              <el-option
                v-for="order in getAvailableAllocationOrderOptions(row)"
                :key="order.id"
                :label="formatActiveOrderOption(order)"
                :value="order.id"
              >
                <div
                  class="team-leader-workbench__active-order-option"
                  data-team-leader-active-order-option
                >
                  <div>
                    <span>编码</span>
                    <strong>{{ formatActiveOrderCode(order) }}</strong>
                  </div>
                  <div>
                    <span>产品</span>
                    <strong>{{ formatActiveOrderProduct(order) }}</strong>
                  </div>
                  <div>
                    <span>数量</span>
                    <strong>{{ formatActiveOrderQuantity(order) }}</strong>
                  </div>
                </div>
              </el-option>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="要生产数量" width="110" align="right">
          <template #default="{ row }">
            {{ formatAllocationOrderProductionQuantity(row) }}
          </template>
        </el-table-column>
        <el-table-column label="生产系数" width="90" align="right">
          <template #default="{ row }">
            {{ formatAllocationOrderProductionCoefficient(row) }}
          </template>
        </el-table-column>
        <el-table-column label="分配数量" min-width="340">
          <template #default="{ row }">
            <div class="team-leader-workbench__allocation-quantity-cell">
              <el-input-number
                v-model="row.allocatedQuantity"
                :disabled="row.editable === false"
                :min="0"
                :precision="0"
                :step="1"
                step-strictly
                :controls="false"
                class="team-leader-workbench__allocation-quantity-input"
                @change="markManualAllocation(row)"
              />
              <el-button
                size="small"
                data-team-leader-allocation-max
                :disabled="row.editable === false"
                @click="applyAllocationShortcut(row, 'MAX')"
              >
                最大
              </el-button>
              <el-button
                size="small"
                data-team-leader-allocation-half
                :disabled="row.editable === false"
                @click="applyAllocationShortcut(row, 'HALF')"
              >
                一半
              </el-button>
              <el-button
                size="small"
                data-team-leader-allocation-clear
                :disabled="row.editable === false"
                @click="clearAllocationQuantity(row)"
              >
                清除
              </el-button>
              <el-tag
                v-if="resolveAllocationOverageQuantity(row) > 0"
                class="team-leader-workbench__allocation-overage"
                data-team-leader-allocation-overage
                type="danger"
                effect="plain"
              >
                待调整 {{ resolveAllocationOverageQuantity(row) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="88" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.released" type="success" effect="light">已放行</el-tag>
            <el-tag v-else type="warning" effect="plain">未放行</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="72" align="center">
          <template #default="{ row, $index }">
            <el-button
              link
              type="danger"
              :disabled="row.editable === false"
              @click="removeAllocationLine($index)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <div class="team-leader-workbench__hint mt-8px" data-team-leader-allocation-summary>
        超过允许上限的提交不会增加活跃订单生产进度。
        池总量：{{
          allocationSnapshot?.poolQuantity ?? reviewEvent?.outputQuantity ?? 0
        }}，已分配：{{ allocationTotalQuantity }}，未分配：{{
          allocationUnallocatedQuantity
        }}，当前模式：{{ reviewForm.allocationMode }}
      </div>
    </div>
    <template #footer>
      <el-button @click="reviewVisible = false">取消</el-button>
      <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">{{
        reviewDialogSubmitText
      }}</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="correctionVisible"
    :title="correctionForm.correctionMode === 'PQC' ? '修改PQC表单' : '修改报工内容'"
    width="min(760px, calc(100vw - 24px))"
    class="team-leader-workbench__correction-dialog"
    destroy-on-close
    data-production-report-correction-dialog
    data-pqc-inspection-correction-dialog
  >
    <section
      class="team-leader-workbench__correction-section"
      aria-labelledby="correction-context-title"
    >
      <h3 id="correction-context-title" class="team-leader-workbench__correction-title"
        >报工信息</h3
      >
      <div class="team-leader-workbench__correction-context">
        <div>
          <span>生产工单</span>
          <strong>{{
            correctionEvent?.workOrderCode || correctionEvent?.workOrderName || '--'
          }}</strong>
        </div>
        <div>
          <span>工序</span>
          <strong>{{
            correctionEvent?.processName || correctionEvent?.processCode || '--'
          }}</strong>
        </div>
        <div>
          <span>报工人</span>
          <strong>{{ correctionEvent?.actualEmployeeUserName || '--' }}</strong>
        </div>
        <div>
          <span>提交时间</span>
          <strong>{{ formatDateTimeValue(correctionEvent?.submittedAt, '--') }}</strong>
        </div>
      </div>
    </section>

    <el-form
      ref="correctionFormRef"
      class="team-leader-workbench__correction-form"
      :model="correctionForm"
      :rules="correctionFormRules"
      label-position="top"
    >
      <section
        v-if="
          correctionForm.correctionMode === 'PRODUCTION' &&
          !correctionForm.materialDetails.length
        "
        class="team-leader-workbench__correction-section"
      >
        <h3 class="team-leader-workbench__correction-title">生产数量</h3>
        <div class="team-leader-workbench__correction-quantity-grid">
          <el-form-item label="完成数量" prop="outputQuantity">
            <el-input-number
              v-model="correctionForm.outputQuantity"
              :min="0.001"
              :precision="3"
              :controls="false"
              class="team-leader-workbench__full-control"
              data-production-report-correction-output
            />
          </el-form-item>
          <el-form-item label="损耗合计">
            <el-input-number
              :model-value="correctionLossQuantity"
              :precision="3"
              :controls="false"
              disabled
              class="team-leader-workbench__full-control"
            />
          </el-form-item>
        </div>
      </section>

      <section
        v-if="correctionForm.correctionMode === 'PRODUCTION' && correctionForm.materialDetails.length"
        class="team-leader-workbench__correction-section"
        data-production-report-correction-materials
      >
        <h3 class="team-leader-workbench__correction-title">物料明细</h3>
        <el-tabs
          v-model="correctionActiveMaterialKey"
          class="team-leader-workbench__correction-material-tabs"
          data-production-report-correction-material-tabs
        >
          <el-tab-pane
            v-for="materialRow in correctionForm.materialDetails"
            :key="resolveCorrectionMaterialKey(materialRow)"
            :label="formatCorrectionMaterialTabLabel(materialRow)"
            :name="resolveCorrectionMaterialKey(materialRow)"
          >
            <div class="team-leader-workbench__correction-material-card">
              <div class="team-leader-workbench__correction-material-title">
                <strong>{{ materialRow.materialName || materialRow.materialCode }}</strong>
                <small v-if="materialRow.materialCode">{{ materialRow.materialCode }}</small>
              </div>
              <div class="team-leader-workbench__correction-material-summary">
                <div class="team-leader-workbench__correction-field-row">
                  <span class="team-leader-workbench__correction-field-label">完成数量</span>
                  <div class="team-leader-workbench__correction-field-control">
                    <el-input-number
                      v-model="materialRow.outputQuantity"
                      :min="0"
                      :precision="3"
                      :controls="false"
                      class="team-leader-workbench__full-control"
                      @change="syncCorrectionOutputFromMaterials"
                    />
                  </div>
                </div>
                <div class="team-leader-workbench__correction-field-row">
                  <span class="team-leader-workbench__correction-field-label">损耗合计</span>
                  <div class="team-leader-workbench__correction-field-control">
                    <el-input-number
                      :model-value="resolveCorrectionMaterialLossTotal(materialRow)"
                      :precision="3"
                      :controls="false"
                      disabled
                      class="team-leader-workbench__full-control"
                    />
                  </div>
                </div>
                <div class="team-leader-workbench__correction-field-row">
                  <span class="team-leader-workbench__correction-field-label">总数量</span>
                  <div class="team-leader-workbench__correction-field-control">
                    <el-input-number
                      :model-value="resolveCorrectionMaterialTotalQuantity(materialRow)"
                      :precision="3"
                      :controls="false"
                      disabled
                      class="team-leader-workbench__full-control"
                    />
                  </div>
                </div>
              </div>
              <div class="team-leader-workbench__material-detail-list">
                <strong>损耗原因</strong>
                <div
                  v-for="detailRow in materialRow.lossDetails"
                  :key="`${materialRow.materialId}:${detailRow.reasonId}`"
                  class="team-leader-workbench__correction-field-row"
                >
                  <span class="team-leader-workbench__correction-field-label">{{ detailRow.reasonName }}</span>
                  <div class="team-leader-workbench__correction-field-control">
                    <el-input-number
                      v-model="detailRow.quantity"
                      :min="0"
                      :precision="3"
                      :controls="false"
                      aria-label="物料损耗数量"
                      class="team-leader-workbench__full-control"
                      @change="syncCorrectionMaterialLossQuantity(materialRow)"
                    />
                  </div>
                </div>
                <span v-if="!materialRow.lossDetails.length" class="team-leader-workbench__correction-empty">
                  当前物料未配置损耗原因
                </span>
              </div>
              <div
                class="team-leader-workbench__material-detail-list"
                data-production-report-correction-devices
              >
                <strong>使用的设备</strong>
                <el-tabs
                  v-if="resolveCorrectionMaterialDevices(materialRow).length"
                  v-model="correctionActiveDeviceKeys[resolveCorrectionMaterialKey(materialRow)]"
                  class="team-leader-workbench__correction-device-tabs"
                >
                  <el-tab-pane
                    v-for="deviceRow in resolveCorrectionMaterialDevices(materialRow)"
                    :key="resolveCorrectionDeviceKey(deviceRow)"
                    :label="formatCorrectionDeviceTabLabel(deviceRow)"
                    :name="resolveCorrectionDeviceKey(deviceRow)"
                  >
                    <div
                      class="team-leader-workbench__material-detail-list"
                      data-production-report-correction-parameters
                    >
                      <strong>设备参数</strong>
                      <div
                        v-for="parameterRow in resolveCorrectionDeviceParameters(materialRow, deviceRow)"
                        :key="`${materialRow.materialId}:${parameterRow.deviceId}:${parameterRow.parameterCode}`"
                        class="team-leader-workbench__correction-field-row"
                      >
                        <span class="team-leader-workbench__correction-field-label">{{ parameterRow.parameterName || parameterRow.parameterCode }}<small v-if="parameterRow.unit">（{{ parameterRow.unit }}）</small></span>
                        <div class="team-leader-workbench__correction-field-control">
                          <el-select
                            v-if="isCorrectionSelectParameter(parameterRow)"
                            v-model="parameterRow.value"
                            filterable
                            allow-create
                            default-first-option
                            class="team-leader-workbench__full-control"
                            aria-label="物料设备选择参数"
                          >
                            <el-option label="请选择" value="" />
                            <el-option
                              v-for="option in resolveCorrectionParameterOptions(parameterRow)"
                              :key="option"
                              :label="option"
                              :value="option"
                            />
                          </el-select>
                          <el-input-number
                            v-else
                            v-model="parameterRow.value"
                            :precision="3"
                            :controls="false"
                            aria-label="物料设备参数"
                            class="team-leader-workbench__full-control"
                          />
                        </div>
                      </div>
                      <span
                        v-if="!resolveCorrectionDeviceParameters(materialRow, deviceRow).length"
                        class="team-leader-workbench__correction-empty"
                      >
                        当前设备没有参数快照
                      </span>
                    </div>
                  </el-tab-pane>
                </el-tabs>
                <span v-else class="team-leader-workbench__correction-empty">当前物料没有设备快照</span>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>

      <section
        v-if="correctionForm.correctionMode === 'PQC'"
        class="team-leader-workbench__correction-section"
        data-pqc-inspection-correction-form
      >
        <h3 class="team-leader-workbench__correction-title">PQC表单数据</h3>
        <div class="team-leader-workbench__correction-quantity-grid">
          <el-form-item label="检验数量">
            <el-input-number
              v-model="correctionForm.pqcActualInspectionQuantity"
              :min="1"
              :precision="0"
              step-strictly
              :controls="false"
              class="team-leader-workbench__full-control"
              data-pqc-inspection-correction-quantity
            />
          </el-form-item>
          <el-form-item label="损耗数量">
            <el-input-number
              v-model="correctionForm.pqcScrapQuantity"
              :min="0"
              :precision="0"
              step-strictly
              :controls="false"
              class="team-leader-workbench__full-control"
              data-pqc-inspection-correction-scrap
            />
          </el-form-item>
        </div>
        <div class="team-leader-workbench__correction-rows">
          <div
            v-for="itemRow in correctionForm.pqcItemResults"
            :key="itemRow.itemCode"
            class="team-leader-workbench__correction-row team-leader-workbench__correction-row--stacked"
          >
            <span>{{ itemRow.itemName || itemRow.itemCode }}</span>
            <el-input
              v-model="itemRow.selectedEquipmentNumber"
              placeholder="设备编号"
              data-pqc-inspection-correction-equipment-number
            />
            <el-input
              v-model="itemRow.sampleValuesText"
              type="textarea"
              :rows="3"
              placeholder="逐件样本值，每行或逗号分隔"
              data-pqc-inspection-correction-samples
            />
          </div>
        </div>
      </section>

      <section
        class="team-leader-workbench__correction-section team-leader-workbench__correction-confirm"
      >
        <el-form-item label="修改原因" prop="changeReason">
          <el-input
            v-model="correctionForm.changeReason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请说明本次修改原因"
          />
        </el-form-item>
        <el-form-item label="签名密码" prop="signaturePassword">
          <el-input
            v-model="correctionForm.signaturePassword"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入当前登录账号的签名密码"
            data-production-report-correction-signature-password
            @keyup.enter="submitCorrection"
          />
        </el-form-item>
      </section>
    </el-form>
    <template #footer>
      <el-button @click="correctionVisible = false">取消</el-button>
      <el-button type="primary" :loading="correctionSubmitting" @click="submitCorrection">
        确认修改
      </el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="activeOrderVersionUpgradeDialogVisible" title="版本升级重启" width="760px">
    <div v-if="activeOrderVersionUpgradePreview" class="active-order-version-upgrade">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="本流程将按全部最新正式版本提交审批；不提供逐项版本选择；审批通过后整单从头执行。"
      />
      <div class="upgrade-summary">
        <div>来源工单：{{ activeOrderVersionUpgradePreview.workOrderCode || '-' }}</div>
        <div>活跃订单：{{ activeOrderVersionUpgradePreview.activeOrderId }}</div>
        <div>目标策略：全部最新正式版本</div>
      </div>
      <el-table :data="activeOrderVersionUpgradePreview.targetVersions" size="small" border>
        <el-table-column prop="objectName" label="受控对象" min-width="180" />
        <el-table-column prop="currentVersionNo" label="当前锁定版本" width="140" />
        <el-table-column prop="targetVersionNo" label="最新正式版本" width="140" />
        <el-table-column label="是否变化" width="100">
          <template #default="{ row }">
            <el-tag :type="row.changed ? 'warning' : 'info'">
              {{ row.changed ? '变化' : '不变' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-alert
        v-if="activeOrderVersionUpgradePreview.blockers?.length"
        type="error"
        :closable="false"
        class="mt-12px"
        :title="activeOrderVersionUpgradePreview.blockers.join('；')"
      />
      <el-form label-width="96px" class="mt-12px">
        <el-form-item label="升级原因" required>
          <el-input
            v-model="activeOrderVersionUpgradeReason"
            type="textarea"
            :rows="3"
            placeholder="请说明为什么需要按最新版本重启执行"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="activeOrderVersionUpgradeConfirmRestart">
            我确认审批通过后旧执行停止，新订单按全部最新正式版本整单从头执行
          </el-checkbox>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="activeOrderVersionUpgradeDialogVisible = false">取消</el-button>
      <el-button
        type="warning"
        :loading="activeOrderVersionUpgradeSubmittingId !== undefined"
        :disabled="
          !activeOrderVersionUpgradePreview?.submittable ||
          !activeOrderVersionUpgradeReason.trim() ||
          !activeOrderVersionUpgradeConfirmRestart
        "
        @click="submitActiveOrderVersionUpgrade"
      >
        提交升级审批
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import TableMultiFilter from '@/components/TableMultiFilter/index.vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableMultiFilter,
  type ListMultiFilterDefinition
} from '@/hooks/web/useTableMultiFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition,
  type UserTableColumnState
} from '@/hooks/web/useUserTableColumns'
import {
  addTeamLeaderActiveOrder,
  applyTeamLeaderActiveOrderRelease,
  confirmTeamLeaderReportAllocation,
  createTemporaryTeamEmployee,
  createTeamDevice,
  createTeamLeaderLossReason,
  deleteTeamLeaderLossReason,
  getPqcPersonnelList,
  getTeamDeviceList,
  getTeamLeaderProcessConfigList,
  saveTeamLeaderProcessOverageLimit,
  getTeamLeaderResponsibleRouteList,
  getProductionPersonnelList,
  getTeamLeaderActiveOrderDetail,
  getTeamLeaderActiveOrderList,
  getTeamLeaderActiveOrderRelease,
  getCurrentTeamLeaderReportAllocation,
  getTeamLeaderSubmissionDetail,
  getTeamLeaderSubmissionPage,
  linkPqcFormalEmployee,
  linkFormalTeamEmployee,
  markAndReportWorkOrderAbnormal,
  moveTeamLeaderActiveOrder,
  previewTeamLeaderReportFifoAllocation,
  previewTeamLeaderActiveOrderRebuild,
  previewTeamLeaderActiveOrderVersionUpgrade,
  rebuildTeamLeaderActiveOrder,
  submitTeamLeaderActiveOrderVersionUpgrade,
  copyLatestTeamLeaderSimulationActiveOrder,
  cleanupLatestTeamLeaderSimulationActiveOrder,
  simulateStage1ActiveOrderCompletion,
  simulateStage2_5BackfillBatchExecution,
  simulateTeamLeaderActiveOrderCompletion,
  removeTeamLeaderActiveOrder,
  resetTemporaryTeamEmployeeSignaturePassword,
  reviewTeamLeaderSubmission,
  saveTeamProcessDefectReason,
  saveTeamProcessConfigDeviceBinding,
  saveTeamProcessConfigDeviceParameterRule,
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
  type TeamLeaderActiveOrderCommitAction,
  type TeamLeaderActiveOrderDetailRespVO,
  type TeamLeaderActiveOrderReleaseApplyRespVO,
  type TeamLeaderActiveOrderReleaseApplicationStatus,
  type TeamLeaderActiveOrderReleaseBlockerRespVO,
  type TeamLeaderActiveOrderReleaseFailureRespVO,
  type TeamLeaderActiveOrderRespVO,
  type TeamLeaderActiveOrderSimulationCopyRespVO,
  type TeamLeaderActiveOrderVersionUpgradePreviewRespVO,
  type TeamLeaderLossReasonVO,
  type TeamDeviceParameterRuleSaveReqVO,
  type TeamLeaderProcessConfigDeviceVO,
  type TeamLeaderProcessConfigListReqVO,
  type TeamLeaderProcessConfigParameterVO,
  type TeamLeaderProcessConfigRowRespVO,
  type TeamLeaderResponsibleRouteRespVO,
  type TeamLeaderReportAllocationLine,
  type TeamLeaderReportAllocationSnapshotRespVO,
  type TeamLeaderSubmissionPageReqVO,
  type TeamLeaderType,
  type TeamPqcPersonnelRespVO,
  type TeamDeviceRespVO,
  type DeviceParameterValueType,
  type TeamProductionEmployeeRespVO
} from '@/api/mes/pro/processpool/teamLeader'
import type {
  ProcessPoolTimelineDetailVO,
  ProcessPoolTimelineEventVO,
  ProcessPoolTimelineSelectedDeviceVO
} from '@/api/mes/pro/processpool'
import {
  correctProcessPoolPqcInspection,
  correctProcessPoolProductionReport
} from '@/api/mes/pro/processpool/eventRevision'
import { MdItemApi } from '@/api/mes/md/item'
import { SOURCE_TYPE_PQC_SUBMISSION } from '@/api/mes/pro/edhr/nonconformanceReview'
import { formatDateTimeValue, formatDate } from '@/utils/formatTime'

defineOptions({ name: 'MesProProcessPoolTeamLeaderWorkbench' })

type ProcessConfigDeviceParameterTemplate = {
  parameterCode: string
  parameterName: string
  unit?: string
  standardText: string
  valueType: DeviceParameterValueType
  targetValue?: number
  lowerLimit?: number
  upperLimit?: number
  optionValues?: string[]
  defaultText?: string
  decimalScale?: number
}

type CleaningWashProcessKind = 'ROUGH_WASH' | 'FINE_WASH' | 'CLEANING'
type CleaningWashMedium = '自来水' | '纯化水'

type CleaningWashProcessConfig = {
  kind: CleaningWashProcessKind
  processKeyword: string
  parameterCodePrefix: string
  defaultCleaningMedium: CleaningWashMedium
  cleaningMediumOptions: readonly CleaningWashMedium[]
  buttonLabel: string
  dialogTitle: string
}

const CLEANING_WASH_PROCESS_CONFIGS: CleaningWashProcessConfig[] = [
  {
    kind: 'ROUGH_WASH',
    processKeyword: '粗洗',
    parameterCodePrefix: 'ROUGH_WASH',
    defaultCleaningMedium: '自来水',
    cleaningMediumOptions: ['自来水', '纯化水'],
    buttonLabel: '粗洗参数',
    dialogTitle: '粗洗参数配置'
  },
  {
    kind: 'FINE_WASH',
    processKeyword: '精洗',
    parameterCodePrefix: 'FINE_WASH',
    defaultCleaningMedium: '纯化水',
    cleaningMediumOptions: ['自来水', '纯化水'],
    buttonLabel: '精洗参数',
    dialogTitle: '精洗参数配置'
  },
  {
    kind: 'CLEANING',
    processKeyword: '清洗',
    parameterCodePrefix: 'CLEANING',
    defaultCleaningMedium: '纯化水',
    cleaningMediumOptions: ['纯化水', '自来水'],
    buttonLabel: '清洗参数',
    dialogTitle: '清洗参数配置'
  }
]

const CLEANING_PROCESS_TEMPERATURE_LABEL_DEVICE_CODES = new Set(['B04091', 'B09353'])

const resolveCleaningWashRoomTemperatureParameterName = (
  config: CleaningWashProcessConfig,
  device: TeamLeaderProcessConfigDeviceVO
) =>
  config.kind === 'CLEANING' &&
  CLEANING_PROCESS_TEMPERATURE_LABEL_DEVICE_CODES.has(device.deviceCode?.trim() || '')
    ? '清洗温度'
    : '室温'

const requireCleaningWashProcessConfig = (kind: CleaningWashProcessKind) => {
  const config = CLEANING_WASH_PROCESS_CONFIGS.find((item) => item.kind === kind)
  if (!config) {
    throw new Error(`缺少固定清洗工序配置：${kind}`)
  }
  return config
}

const createCleaningWashDeviceParameterTemplates = (
  config: CleaningWashProcessConfig
): ProcessConfigDeviceParameterTemplate[] => {
  const { parameterCodePrefix, defaultCleaningMedium, cleaningMediumOptions } = config
  return [
    {
      parameterCode: parameterCodePrefix + '_COUNT',
      parameterName: '清洗次数',
      unit: '次',
      standardText: '清洗次数默认 2 次',
      valueType: 'INTEGER',
      targetValue: 2,
      lowerLimit: undefined,
      upperLimit: undefined
    },
    {
      parameterCode: parameterCodePrefix + '_MEDIUM',
      parameterName: '清洗介质',
      standardText: `清洗介质可选${cleaningMediumOptions.join('或')}，默认${defaultCleaningMedium}`,
      valueType: 'SELECT',
      optionValues: [...cleaningMediumOptions],
      defaultText: defaultCleaningMedium
    },
    {
      parameterCode: parameterCodePrefix + '_POWER',
      parameterName: '清洗功率',
      unit: '%',
      standardText: '清洗功率 20-30%',
      valueType: 'INTEGER',
      lowerLimit: 20,
      targetValue: 25,
      upperLimit: 30
    },
    {
      parameterCode: parameterCodePrefix + '_ROOM_TEMPERATURE',
      parameterName: '室温',
      unit: '℃',
      standardText: '室温 20.0-30.0℃',
      valueType: 'DECIMAL',
      lowerLimit: 20,
      targetValue: 26,
      upperLimit: 30,
      decimalScale: 1
    },
    {
      parameterCode: parameterCodePrefix + '_TIME',
      parameterName: '清洗时间',
      unit: 'min',
      standardText: '清洗时间默认 30 min',
      valueType: 'INTEGER',
      targetValue: 30,
      lowerLimit: undefined,
      upperLimit: undefined
    }
  ]
}

const ROUGH_WASH_DEVICE_PARAMETER_TEMPLATES = createCleaningWashDeviceParameterTemplates(
  requireCleaningWashProcessConfig('ROUGH_WASH')
)
const FINE_WASH_DEVICE_PARAMETER_TEMPLATES = createCleaningWashDeviceParameterTemplates(
  requireCleaningWashProcessConfig('FINE_WASH')
)
const CLEANING_DEVICE_PARAMETER_TEMPLATES = createCleaningWashDeviceParameterTemplates(
  requireCleaningWashProcessConfig('CLEANING')
)
const CLEANING_WASH_DEVICE_PARAMETER_TEMPLATES_BY_KIND: Record<
  CleaningWashProcessKind,
  ProcessConfigDeviceParameterTemplate[]
> = {
  ROUGH_WASH: ROUGH_WASH_DEVICE_PARAMETER_TEMPLATES,
  FINE_WASH: FINE_WASH_DEVICE_PARAMETER_TEMPLATES,
  CLEANING: CLEANING_DEVICE_PARAMETER_TEMPLATES
}

type WorkbenchLeaderTab = TeamLeaderType
type ProcessConfigCreateType = 'DEVICE_BINDING' | 'PARAMETER_RULE'
type ProductionClearanceConfirmationColumnKey = 'workplace' | 'material' | 'cleaning'
type AllocationShortcutMode = 'MAX' | 'HALF'
type ActiveOrderReleaseApplicationLockState =
  | 'CONFIRMED'
  | 'CONFIRMED_REFRESH_FAILED'
  | 'CONFIRMED_NOT_PROJECTED'
  | 'RECOVERED'
  | 'UNCERTAIN'

interface TeamLeaderReportAllocationDraftLine
  extends Omit<TeamLeaderReportAllocationLine, 'activeOrderId'> {
  activeOrderId?: number
}

interface ProductionReportCorrectionLossDetailRow {
  reasonId: number
  reasonCode?: string
  reasonName: string
  quantity: number
}

interface ProductionReportCorrectionMaterialRow {
  materialId: number
  materialCode?: string
  materialName?: string
  outputQuantity: number
  lossQuantity: number
  lossDetails: ProductionReportCorrectionLossDetailRow[]
  selectedDevice?: ProcessPoolTimelineSelectedDeviceVO
  selectedDevices: ProcessPoolTimelineSelectedDeviceVO[]
  deviceParameterReadings: ProductionReportCorrectionParameterRow[]
}

interface ProductionReportCorrectionParameterRow {
  deviceId: number
  deviceCode?: string
  deviceName?: string
  parameterCode: string
  parameterName?: string
  unit?: string
  value?: number | string | boolean
  textValue?: string
  valueType?: string
  optionValues?: string[]
  displayConfig?: Record<string, unknown>
  lowerLimit?: number | string
  upperLimit?: number | string
  parameterStatus?: 'NORMAL' | 'BELOW_LOWER' | 'ABOVE_UPPER'
}

interface PqcInspectionCorrectionItemRow {
  itemCode: string
  itemName?: string
  selectedEquipmentId?: number
  selectedEquipmentNumber?: string
  sampleValuesText: string
}

interface ProductionReportCorrectionPreviewItem {
  key: string
  label: string
  beforeValue: string
  afterValue: string
}

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
const router = useRouter()
const activePqcModuleTab = ref<'personnel' | 'management' | 'equipment' | 'detail' | 'history'>(
  'management'
)
const activeProductionModuleTab = ref<
  'personnel' | 'report' | 'reportHistory' | 'activeOrder' | 'processConfig'
>('report')

const getDefaultSubmissionDate = () => formatDate(new Date(), 'YYYY-MM-DD')
const getInitialSubmissionDate = (_leaderType: TeamLeaderType) => undefined
const loading = ref(false)
const detailLoading = ref(false)
const reviewSubmitting = ref(false)
const allocationPreviewLoading = ref(false)
const abnormalSubmitting = ref(false)
const maintenanceSubmitting = ref(false)
const activeOrderLoading = ref(false)
const activeOrderConflictSubmitting = ref(false)
const activeOrderMoveSubmittingId = ref<number>()
const activeOrderMoveDirection = ref<'UP' | 'DOWN'>()
const activeOrderRebuildSubmittingId = ref<number>()
const activeOrderVersionUpgradeSubmittingId = ref<number>()
const activeOrderSimulationSubmittingId = ref<number>()
const stage1GeneratedDetailTargets = ref(
  new Map<number, { activeOrderId: number; sourceWorkOrderCode: string }>()
)
const correctionSubmitting = ref(false)
const detailVisible = ref(false)
const reviewVisible = ref(false)
const correctionVisible = ref(false)
const activeOrderAddDialogVisible = ref(false)
const activeOrderConflictDrawerVisible = ref(false)
const abnormalDialogVisible = ref(false)
const loadError = ref('')
const submissionTotal = ref(0)
const submissionList = ref<ProcessPoolTimelineEventVO[]>([])
const submissionMaterialNameCache = ref<Record<string, string>>({})
const detail = ref<ProcessPoolTimelineDetailVO>()
const reviewEvent = ref<ProcessPoolTimelineEventVO>()
const correctionEvent = ref<ProcessPoolTimelineEventVO>()
const activeOrderOptions = ref<TeamLeaderActiveOrderRespVO[]>([])
const activeOrderConflictSelectedOrder = ref<TeamLeaderActiveOrderRespVO>()
const activeOrderConflictDetail = ref<TeamLeaderActiveOrderDetailRespVO>()
const activeOrderConflictLoading = ref(false)
const activeOrderConflictError = ref('')
const activeOrderConflictActiveOrderId = ref<number>()
const activeOrderVersionUpgradeDialogVisible = ref(false)
const activeOrderVersionUpgradePreview = ref<TeamLeaderActiveOrderVersionUpgradePreviewRespVO>()
const activeOrderVersionUpgradeReason = ref('')
const activeOrderVersionUpgradeConfirmRestart = ref(false)
const activeOrderCandidateOptions = ref<TeamLeaderActiveOrderCandidateRespVO[]>([])
const activeOrderSelectedCandidate = ref<TeamLeaderActiveOrderCandidateRespVO>()
const activeOrderCandidateKeyword = ref('')
const activeOrderCandidateLoading = ref(false)
const activeOrderCandidateError = ref('')
const releaseApplicationSubmittingId = ref<number>()
const releaseApplicationBlockers = ref<TeamLeaderActiveOrderReleaseBlockerRespVO[]>([])
const releaseApplicationIdempotencyKeys = new Map<number, string>()
const releaseApplicationLocks = reactive(new Map<number, ActiveOrderReleaseApplicationLockState>())
const releaseApplicationUncertainMessage = ref('')
const processConfigRows = ref<TeamLeaderProcessConfigRowRespVO[]>([])
const processConfigDisplayRows = ref<TeamLeaderProcessConfigRowRespVO[]>([])
const responsibleRouteRows = ref<TeamLeaderResponsibleRouteRespVO[]>([])
const teamDeviceOptions = ref<TeamDeviceRespVO[]>([])
const processConfigLoading = ref(false)
const responsibleRouteLoading = ref(false)
const processConfigSubmitting = ref(false)
const processConfigCreateDialogVisible = ref(false)
const processConfigDeviceDialogVisible = ref(false)
const processConfigParameterDialogVisible = ref(false)
const roughWashParameterDialogVisible = ref(false)
const activeCleaningWashProcessConfig = ref<CleaningWashProcessConfig>()
const processConfigSelectedRow = ref<TeamLeaderProcessConfigRowRespVO>()
const processConfigSelectedDevice = ref<TeamLeaderProcessConfigDeviceVO>()
const processConfigEditingParameter = ref<TeamLeaderProcessConfigParameterVO>()
const PROCESS_CONFIG_TABLE_KEY = 'mes.processPool.teamLeader.processConfig'
const processConfigQuery = reactive<TeamLeaderProcessConfigListReqVO & { pageNo?: number }>({
  pageNo: 1
})
const processConfigFilterDefinitions: ListMultiFilterDefinition[] = [
  {
    key: 'route',
    label: '工艺路线',
    type: 'text',
    queryParamKey: 'routeKeyword',
    operators: ['contains'],
    placeholder: '请输入路线编码或名称'
  },
  {
    key: 'process',
    label: '工序',
    type: 'text',
    queryParamKey: 'processKeyword',
    operators: ['contains'],
    placeholder: '请输入工序编码或名称'
  },
  {
    key: 'lossReason',
    label: '损耗原因',
    type: 'text',
    queryParamKey: 'lossReasonKeyword',
    operators: ['contains'],
    placeholder: '请输入损耗原因描述'
  },
  {
    key: 'device',
    label: '映射设备',
    type: 'text',
    queryParamKey: 'deviceKeyword',
    operators: ['contains'],
    placeholder: '请输入设备编码或名称'
  },
  {
    key: 'parameter',
    label: '设备参数标准',
    type: 'text',
    queryParamKey: 'parameterKeyword',
    operators: ['contains'],
    placeholder: '请输入参数编码或名称'
  }
]
const lossReasonSubmitting = ref(false)
const lossReasonMaintenanceDialogVisible = ref(false)
const lossReasonDialogMode = ref<'idle' | 'create' | 'edit'>('idle')
const lossReasonMaintenanceRouteProcessId = ref<number>()
const lossReasonEditingReasonId = ref<number>()
const allocationRows = ref<TeamLeaderReportAllocationDraftLine[]>([])
const allocationSnapshot = ref<TeamLeaderReportAllocationSnapshotRespVO>()
const allocationSaveIdempotencyState = ref<{ requestIdentity: string; key: string }>()
const reviewDialogMode = ref<'REVIEW' | 'ALLOCATION' | 'REJECTION'>('REVIEW')
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
  { key: 'workOrderCode', label: '生产订单号', visible: true },
  { key: 'routeName', label: '路线名称', visible: true },
  { key: 'routeVersionNo', label: '版本号', visible: true },
  { key: 'erpFixedQuantitySnapshot', label: 'ERP生产数量', visible: true },
  { key: 'productionProgressPercent', label: '生产进度', visible: true },
  { key: 'inspectionProgressPercent', label: '检验进度', visible: true },
  { key: 'releaseApplicationStatus', label: '放行申请', visible: true },
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
const PRODUCTION_SUBMISSION_TABLE_KEY = `${SUBMISSION_TABLE_KEY}.production.clearance-columns-v2`
const PRODUCTION_REPORT_HISTORY_TABLE_KEY = `${SUBMISSION_TABLE_KEY}.productionHistory.reject-v1`
const PQC_SUBMISSION_TABLE_KEY = `${SUBMISSION_TABLE_KEY}.pqc`
const PQC_FORM_HISTORY_TABLE_KEY = `${PQC_SUBMISSION_TABLE_KEY}.history`
const submissionQuickFilterDefinitions: any[] = []
const submissionQuickFilterState = reactive({})
const submissionOperatorOptions: any[] = []
const productionSubmissionDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'submittedAt', label: '提交时间', minWidth: 160 },
  { key: 'employeeUser', label: '员工', minWidth: 140 },
  { key: 'process', label: '工序', minWidth: 150 },
  { key: 'clearanceWorkplace', label: '清场', minWidth: 90 },
  { key: 'clearanceMaterial', label: '物料', minWidth: 90 },
  { key: 'clearanceCleaning', label: '清洁', minWidth: 90 },
  { key: 'workOrder', label: '生产工单', minWidth: 160 },
  { key: 'reportAllocations', label: '分配订单', minWidth: 240 },
  { key: 'operation', label: '操作', width: 190, hideable: false, business: false }
]
const productionReportHistoryDefaultColumns: UserTableColumnDefinition[] = [
  ...productionSubmissionDefaultColumns.filter((column) => column.key !== 'operation'),
  { key: 'approvedBy', label: '处理人', minWidth: 140 },
  { key: 'approvedAt', label: '处理时间', minWidth: 160 },
  { key: 'reviewStatus', label: '处理结果', minWidth: 120 },
  { key: 'reviewRemark', label: '处理说明', minWidth: 220 },
  { key: 'operation', label: '操作', width: 110, hideable: false, business: false }
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
  { key: 'operation', label: '操作', width: 360, hideable: false, business: false }
]
const pqcFormHistoryDefaultColumns: UserTableColumnDefinition[] = [
  ...pqcSubmissionDefaultColumns.filter((column) => column.key !== 'operation'),
  { key: 'approvedBy', label: '审核通过人', minWidth: 140 },
  { key: 'approvedAt', label: '审核通过时间', minWidth: 160 },
  { key: 'operation', label: '操作', width: 110, hideable: false, business: false }
]
const productionSubmissionColumnControl = useUserTableColumns(
  PRODUCTION_SUBMISSION_TABLE_KEY,
  productionSubmissionDefaultColumns
)
const productionReportHistoryColumnControl = useUserTableColumns(
  PRODUCTION_REPORT_HISTORY_TABLE_KEY,
  productionReportHistoryDefaultColumns
)
const pqcSubmissionColumnControl = useUserTableColumns(
  PQC_SUBMISSION_TABLE_KEY,
  pqcSubmissionDefaultColumns
)
const pqcFormHistoryColumnControl = useUserTableColumns(
  PQC_FORM_HISTORY_TABLE_KEY,
  pqcFormHistoryDefaultColumns
)
const isPqcFormHistoryTab = computed(
  () => activeLeaderTab.value === 'PQC' && activePqcModuleTab.value === 'history'
)
const activeSubmissionColumnControl = computed(() =>
  activeLeaderTab.value === 'PQC'
    ? isPqcFormHistoryTab.value
      ? pqcFormHistoryColumnControl
      : pqcSubmissionColumnControl
    : activeProductionModuleTab.value === 'reportHistory'
      ? productionReportHistoryColumnControl
      : productionSubmissionColumnControl
)
const submissionColumnSaving = computed(() => activeSubmissionColumnControl.value.saving.value)
const submissionColumns = computed<UserTableColumnState[]>(
  () => activeSubmissionColumnControl.value.columns.value
)
const isSubmissionColumnVisible = (key: string) =>
  submissionColumns.value.some((column) => column.key === key) &&
  activeSubmissionColumnControl.value.isColumnVisible(key)
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
const showPqcModuleTabs = computed(() => props.showPqcModuleTabs && activeLeaderTab.value === 'PQC')
const showPqcDetailAsTab = computed(
  () => activeLeaderTab.value === 'PQC' && showPqcModuleTabs.value
)
const showProductionModuleTabs = computed(
  () => props.showProductionModuleTabs && activeLeaderTab.value === 'PRODUCTION'
)
const showProductionResponsibleRoutes = computed(
  () =>
    showProductionModuleTabs.value &&
    (responsibleRouteLoading.value ||
      responsibleRouteRows.value.length > 0 ||
      activeProductionModuleTab.value === 'personnel' ||
      activeProductionModuleTab.value === 'activeOrder' ||
      activeProductionModuleTab.value === 'processConfig')
)
const pageTitle = computed(() => props.title)
const pageSubtitle = computed(() => props.subtitle)
const isProductionLeader = computed(() => activeLeaderTab.value === 'PRODUCTION')
const showProductionPersonnelModule = computed(
  () =>
    isProductionLeader.value &&
    (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'personnel')
)
const showProductionReportModule = computed(
  () =>
    isProductionLeader.value &&
    (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'report')
)
const showProductionReportHistoryModule = computed(
  () =>
    isProductionLeader.value &&
    showProductionModuleTabs.value &&
    activeProductionModuleTab.value === 'reportHistory'
)
const isProductionReportHistoryTab = computed(
  () => isProductionLeader.value && activeProductionModuleTab.value === 'reportHistory'
)
const showProductionActiveOrderModule = computed(
  () =>
    isProductionLeader.value &&
    (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'activeOrder')
)
const showProductionProcessConfigModule = computed(
  () =>
    isProductionLeader.value &&
    (!showProductionModuleTabs.value || activeProductionModuleTab.value === 'processConfig')
)
const showProductionConfigModule = computed(
  () => isProductionLeader.value && !showProductionModuleTabs.value
)
const showPqcPersonnelModule = computed(
  () =>
    activeLeaderTab.value === 'PQC' &&
    (!showPqcModuleTabs.value || activePqcModuleTab.value === 'personnel')
)
const showPqcFormHistoryModule = computed(
  () =>
    activeLeaderTab.value === 'PQC' &&
    showPqcModuleTabs.value &&
    activePqcModuleTab.value === 'history'
)
const showPqcManagementModule = computed(
  () =>
    showProductionReportModule.value ||
    showProductionReportHistoryModule.value ||
    showPqcFormHistoryModule.value ||
    (activeLeaderTab.value === 'PQC' &&
      (!showPqcModuleTabs.value || activePqcModuleTab.value === 'management'))
)
const showLegacyDailyCloseDashboardModule = computed(
  () => isProductionLeader.value && !showProductionModuleTabs.value
)
const showPqcDetailModule = computed(
  () => showPqcDetailAsTab.value && activePqcModuleTab.value === 'detail'
)
const employeeColumnLabel = computed(() => (activeLeaderTab.value === 'PQC' ? 'PQC检验员' : '员工'))
const employeeDetailLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员' : '实际员工'
)
const completionQuantityColumnLabel = computed(() =>
  activeLeaderTab.value === 'PQC' ? '检验数量' : '完成数量'
)
const detailDrawerTitle = computed(() =>
  activeLeaderTab.value === 'PQC' ? 'PQC检验员提交详情' : '员工提交详情'
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
  () =>
    dailyClosePendingReviewCount.value + dailyCloseRejectedCount.value + (loadError.value ? 1 : 0)
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
const allocatableActiveOrderOptions = computed(() =>
  activeOrderOptions.value.filter((order) => normalizePositiveNumber(order.id))
)
const pagedActiveOrderRows = computed(() => {
  const pageNo = Math.max(1, Number(activeOrderQuery.pageNo) || 1)
  const pageSize = Math.max(1, Number(activeOrderQuery.pageSize) || 10)
  const start = (pageNo - 1) * pageSize
  return activeOrderOptions.value.slice(start, start + pageSize)
})
const isFirstActiveOrder = (row: TeamLeaderActiveOrderRespVO) =>
  activeOrderOptions.value[0]?.id === row.id
const isLastActiveOrder = (row: TeamLeaderActiveOrderRespVO) =>
  activeOrderOptions.value[activeOrderOptions.value.length - 1]?.id === row.id
const pqcPersonnelTotal = computed(() => pqcPersonnelRows.value.length)
const pagedPqcPersonnelRows = computed(() => {
  const pageNo = Math.max(1, Number(pqcPersonnelQuery.pageNo) || 1)
  const pageSize = Math.max(1, Number(pqcPersonnelQuery.pageSize) || 10)
  const start = (pageNo - 1) * pageSize
  return pqcPersonnelRows.value.slice(start, start + pageSize)
})

const canReviewSubmission = (row: ProcessPoolTimelineEventVO) =>
  !(isProductionReportHistoryTab.value || isPqcFormHistoryTab.value) &&
  !isProductionLeader.value &&
  !row.released &&
  row.submissionReviewStatus !== 'APPROVED' &&
  row.processInspectionAggregationStatus !== 'AGGREGATED' &&
  Boolean(row.id)

const canOpenPqcSubmissionNonconformanceReview = (row: ProcessPoolTimelineEventVO) =>
  canReviewSubmission(row)

const canCorrectSubmission = (row: ProcessPoolTimelineEventVO) =>
  !(isProductionReportHistoryTab.value || isPqcFormHistoryTab.value) &&
  (isProductionLeader.value || !row.released) &&
  Boolean(row.id)

const canAllocateSubmission = (row: ProcessPoolTimelineEventVO) =>
  isProductionLeader.value && !isProductionReportHistoryTab.value && Boolean(row.id)

const canRejectProductionSubmission = (row: ProcessPoolTimelineEventVO) =>
  isProductionLeader.value &&
  !isProductionReportHistoryTab.value &&
  row.submissionReviewStatus !== 'REJECTED' &&
  Boolean(row.id) &&
  !(row.reportAllocations || []).some((allocation) => allocation.released)

const findReportSelectedActiveOrder = (event: ProcessPoolTimelineEventVO) => {
  const workOrderId = Number(event.workOrderId)
  if (!Number.isFinite(workOrderId) || workOrderId <= 0) return undefined
  return activeOrderOptions.value.find((order) => Number(order.workOrderId) === workOrderId)
}

const resolveActiveOrderFormalQuantity = (order?: TeamLeaderActiveOrderRespVO) => {
  const quantity = Number(order?.erpFixedQuantitySnapshot)
  return Number.isFinite(quantity) && quantity > 0 ? quantity : undefined
}

const resolveProductionReportOverageQuantity = (event: ProcessPoolTimelineEventVO) => {
  const outputQuantity = Number(event.outputQuantity)
  if (!Number.isFinite(outputQuantity) || outputQuantity <= 0) return 0
  if (event.reportAllocations?.length) {
    const unallocatedQuantity = Number(event.reportUnallocatedQuantity)
    return Number.isFinite(unallocatedQuantity) && unallocatedQuantity >= 0
      ? unallocatedQuantity
      : outputQuantity
  }
  const orderQuantity = resolveActiveOrderFormalQuantity(findReportSelectedActiveOrder(event))
  return orderQuantity === undefined ? outputQuantity : Math.max(0, outputQuantity - orderQuantity)
}

const allocationTotalQuantity = computed(() =>
  allocationRows.value.reduce(
    (total, line) => total + normalizeAllocationInteger(line.allocatedQuantity),
    0
  )
)

const allocationUnallocatedQuantity = computed(() => {
  const pool = Number(
    allocationSnapshot.value?.poolQuantity ?? reviewEvent.value?.outputQuantity ?? 0
  )
  return Math.max(0, pool - allocationTotalQuantity.value)
})

const reviewDialogTitle = computed(() => {
  if (reviewDialogMode.value === 'ALLOCATION') return '分配报工'
  if (reviewDialogMode.value === 'REJECTION') return '驳回生产报工'
  return '复核员工提交'
})
const reviewDialogSubmitText = computed(() => {
  if (reviewDialogMode.value === 'ALLOCATION') return '确认分配'
  if (reviewDialogMode.value === 'REJECTION') return '确认驳回'
  return '提交复核'
})

const queryParams = reactive<TeamLeaderSubmissionPageReqVO & { pageNo: number; pageSize: number }>({
  pageNo: 1,
  pageSize: 10,
  leaderType: activeLeaderTab.value,
  submitDate: getInitialSubmissionDate(activeLeaderTab.value),
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
  reviewSignaturePassword: ''
})

const correctionForm = reactive({
  correctionMode: 'PRODUCTION' as 'PRODUCTION' | 'PQC',
  eventId: undefined as number | undefined,
  outputQuantity: undefined as number | undefined,
  materialDetails: [] as ProductionReportCorrectionMaterialRow[],
  lossDetails: [] as ProductionReportCorrectionLossDetailRow[],
  deviceParameterReadings: [] as ProductionReportCorrectionParameterRow[],
  pqcActualInspectionQuantity: undefined as number | undefined,
  pqcScrapQuantity: 0,
  pqcItemResults: [] as PqcInspectionCorrectionItemRow[],
  changeReason: '',
  signaturePassword: ''
})

const correctionFormRef = ref()
const correctionActiveMaterialKey = ref('')
const correctionActiveDeviceKeys = reactive<Record<string, string>>({})
const correctionFormRules = {
  changeReason: [{ required: true, message: '请输入修改原因', trigger: 'blur' }],
  signaturePassword: [{ required: true, message: '请输入签名密码', trigger: 'blur' }]
}

const correctionLossQuantity = computed(() =>
  correctionForm.lossDetails.reduce((total, item) => total + Number(item.quantity || 0), 0)
)

const syncCorrectionOutputFromMaterials = () => {
  if (!correctionForm.materialDetails.length) return
  const total = correctionForm.materialDetails.reduce((sum, item) => {
    const outputQuantity = Number(item.outputQuantity)
    return Number.isFinite(outputQuantity) ? sum + outputQuantity : sum
  }, 0)
  correctionForm.outputQuantity = Number(total.toFixed(3))
}

const resolveCorrectionMaterialKey = (materialRow: ProductionReportCorrectionMaterialRow) =>
  String(materialRow.materialId)

const formatCorrectionMaterialTabLabel = (materialRow: ProductionReportCorrectionMaterialRow) =>
  materialRow.materialName || materialRow.materialCode || `物料 ${materialRow.materialId}`

const resolveCorrectionMaterialLossTotal = (
  materialRow: ProductionReportCorrectionMaterialRow
) => {
  const detailTotal = materialRow.lossDetails.reduce((sum, item) => {
    const quantity = Number(item.quantity)
    return Number.isFinite(quantity) ? sum + quantity : sum
  }, 0)
  const lossQuantity = materialRow.lossDetails.length ? detailTotal : Number(materialRow.lossQuantity)
  return Number((Number.isFinite(lossQuantity) ? lossQuantity : 0).toFixed(3))
}

const resolveCorrectionMaterialTotalQuantity = (
  materialRow: ProductionReportCorrectionMaterialRow
) => {
  const outputQuantity = Number(materialRow.outputQuantity)
  const total =
    (Number.isFinite(outputQuantity) ? outputQuantity : 0) +
    resolveCorrectionMaterialLossTotal(materialRow)
  return Number(total.toFixed(3))
}

const syncCorrectionMaterialLossQuantity = (
  materialRow: ProductionReportCorrectionMaterialRow
) => {
  materialRow.lossQuantity = resolveCorrectionMaterialLossTotal(materialRow)
}

const resolveCorrectionDeviceKey = (deviceRow: ProcessPoolTimelineSelectedDeviceVO) =>
  String(deviceRow.deviceId || deviceRow.deviceCode || deviceRow.deviceName || 'device')

const formatCorrectionDeviceTabLabel = (deviceRow: ProcessPoolTimelineSelectedDeviceVO) =>
  deviceRow.deviceCode || deviceRow.deviceName || `设备 ${deviceRow.deviceId || '--'}`

const normalizeCorrectionOptionValues = (value: unknown): string[] => {
  if (Array.isArray(value)) {
    return value.map((item) => String(item).trim()).filter(Boolean)
  }
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) return []
    if (trimmed.startsWith('[')) {
      const parsed = JSON.parse(trimmed)
      if (!Array.isArray(parsed)) {
        throw new Error('设备参数选项快照必须是数组')
      }
      return parsed.map((item) => String(item).trim()).filter(Boolean)
    }
    return trimmed
      .split(/[,\n，、]/)
      .map((item) => item.trim())
      .filter(Boolean)
  }
  return []
}

const isCorrectionSelectParameter = (parameterRow: ProductionReportCorrectionParameterRow) =>
  parameterRow.valueType === 'SELECT'

const resolveCorrectionParameterOptions = (
  parameterRow: ProductionReportCorrectionParameterRow
) => {
  const displayOptions = isRecord(parameterRow.displayConfig)
    ? parameterRow.displayConfig.optionValues ?? parameterRow.displayConfig.options
    : undefined
  const options = normalizeCorrectionOptionValues(
    parameterRow.optionValues?.length ? parameterRow.optionValues : displayOptions
  )
  const currentValue = String(parameterRow.value ?? '').trim()
  if (currentValue && !options.includes(currentValue)) {
    return [...options, currentValue]
  }
  return options
}

const normalizeCorrectionParameterSubmitValue = (
  parameterRow: ProductionReportCorrectionParameterRow
) => {
  if (isCorrectionSelectParameter(parameterRow)) {
    const value = String(parameterRow.value ?? '').trim()
    return value ? { textValue: value } : undefined
  }
  const value = optionalCorrectionNumber(parameterRow.value)
  return value === undefined ? undefined : { value }
}

const resolveCorrectionMaterialDevices = (
  materialRow: ProductionReportCorrectionMaterialRow
) => {
  const devices = materialRow.selectedDevices.length
    ? materialRow.selectedDevices
    : materialRow.selectedDevice
      ? [materialRow.selectedDevice]
      : materialRow.deviceParameterReadings.map((item) => ({
          deviceId: item.deviceId,
          deviceCode: item.deviceCode,
          deviceName: item.deviceName
        }))
  const seen = new Set<string>()
  return devices.filter((item) => {
    const key = resolveCorrectionDeviceKey(item)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

const resolveCorrectionDeviceParameters = (
  materialRow: ProductionReportCorrectionMaterialRow,
  deviceRow: ProcessPoolTimelineSelectedDeviceVO
) => {
  const deviceId = Number(deviceRow.deviceId)
  const deviceCode = String(deviceRow.deviceCode || '').trim()
  return materialRow.deviceParameterReadings.filter((item) => {
    if (Number.isFinite(deviceId) && deviceId > 0) {
      return Number(item.deviceId) === deviceId
    }
    return deviceCode ? String(item.deviceCode || '').trim() === deviceCode : false
  })
}

const initializeCorrectionMaterialDeviceTabs = (
  materialRows: ProductionReportCorrectionMaterialRow[]
) => {
  correctionActiveMaterialKey.value = materialRows.length
    ? resolveCorrectionMaterialKey(materialRows[0])
    : ''
  Object.keys(correctionActiveDeviceKeys).forEach((key) => {
    delete correctionActiveDeviceKeys[key]
  })
  materialRows.forEach((materialRow) => {
    const devices = resolveCorrectionMaterialDevices(materialRow)
    if (devices.length) {
      correctionActiveDeviceKeys[resolveCorrectionMaterialKey(materialRow)] =
        resolveCorrectionDeviceKey(devices[0])
    }
  })
}

const correctionValueText = (value: unknown, unit?: string) => {
  const number = Number(value)
  if (!Number.isFinite(number)) return '--'
  const normalized = number.toFixed(3).replace(/\.?0+$/, '')
  return unit ? `${normalized} ${unit}` : normalized
}

const optionalCorrectionNumber = (value: unknown): number | undefined => {
  if (value === null || value === undefined || value === '') return undefined
  const number = Number(value)
  return Number.isFinite(number) ? number : undefined
}

const normalizePqcSampleText = (values: string[]) => values.join('\n')

const parsePqcSampleText = (value: string) =>
  value
    .split(/[\n,，]+/)
    .map((item) => item.trim())
    .filter(Boolean)

const correctionChangePreview = computed<ProductionReportCorrectionPreviewItem[]>(() => {
  const event = correctionEvent.value
  if (!event) return []
  if (correctionForm.correctionMode === 'PQC') {
    const { rootPayload } = resolvePqcPayloadPair(event)
    const changes: ProductionReportCorrectionPreviewItem[] = []
    const beforeInspectionQuantity = Number(
      readSubmissionPayloadValue(rootPayload, ['inspectionQuantity', 'actualInspectionQuantity'])
    )
    const afterInspectionQuantity = Number(correctionForm.pqcActualInspectionQuantity)
    if (
      Number.isFinite(beforeInspectionQuantity) &&
      Number.isFinite(afterInspectionQuantity) &&
      beforeInspectionQuantity !== afterInspectionQuantity
    ) {
      changes.push({
        key: 'PQC_ACTUAL_INSPECTION_QUANTITY',
        label: 'PQC检验数量',
        beforeValue: correctionValueText(beforeInspectionQuantity),
        afterValue: correctionValueText(afterInspectionQuantity)
      })
    }
    const beforeScrap = Number(
      readSubmissionPayloadValue(rootPayload, ['scrapQuantity', 'lossQuantity'])
    )
    const afterScrap = Number(correctionForm.pqcScrapQuantity)
    if (Number.isFinite(beforeScrap) && Number.isFinite(afterScrap) && beforeScrap !== afterScrap) {
      changes.push({
        key: 'PQC_SCRAP_QUANTITY',
        label: 'PQC损耗数量',
        beforeValue: correctionValueText(beforeScrap),
        afterValue: correctionValueText(afterScrap)
      })
    }
    const beforeItems = new Map(
      resolvePqcItemSnapshotDetails(event).map((item) => [
        item.itemCode || item.itemName || '',
        (item.sampleValues || []).join(',')
      ])
    )
    correctionForm.pqcItemResults.forEach((item) => {
      const before = beforeItems.get(item.itemCode) || ''
      const after = parsePqcSampleText(item.sampleValuesText).join(',')
      if (before !== after) {
        changes.push({
          key: `PQC_ITEM:${item.itemCode}`,
          label: item.itemName || item.itemCode,
          beforeValue: before || '--',
          afterValue: after || '--'
        })
      }
    })
    return changes
  }
  const changes: ProductionReportCorrectionPreviewItem[] = []
  const beforeOutput = Number(event.outputQuantity)
  const afterOutput = Number(correctionForm.outputQuantity)
  if (
    Number.isFinite(beforeOutput) &&
    Number.isFinite(afterOutput) &&
    beforeOutput !== afterOutput
  ) {
    changes.push({
      key: 'OUTPUT_QUANTITY',
      label: '完成数量',
      beforeValue: correctionValueText(beforeOutput),
      afterValue: correctionValueText(afterOutput)
    })
  }

  const originalLossMap = new Map(
    (event.lossDetails || []).map((item) => [Number(item.reasonId), Number(item.quantity || 0)])
  )
  correctionForm.lossDetails.forEach((item) => {
    const before = originalLossMap.get(item.reasonId) || 0
    const after = Number(item.quantity || 0)
    if (before !== after) {
      changes.push({
        key: `LOSS:${item.reasonId}`,
        label: item.reasonName,
        beforeValue: correctionValueText(before),
        afterValue: correctionValueText(after)
      })
    }
  })

  const originalMaterialMap = new Map(
    resolveProductionMaterialRows(event).map((item) => [item.materialId, item])
  )
  correctionForm.materialDetails.forEach((item) => {
    const before = originalMaterialMap.get(item.materialId)
    if (!before) return
    if (before.outputQuantity !== item.outputQuantity) {
      changes.push({
        key: `MATERIAL_OUTPUT:${item.materialId}`,
        label: `${item.materialName || item.materialCode || item.materialId} 完成数量`,
        beforeValue: correctionValueText(before.outputQuantity),
        afterValue: correctionValueText(item.outputQuantity)
      })
    }
    if (before.lossQuantity !== item.lossQuantity) {
      changes.push({
        key: `MATERIAL_LOSS:${item.materialId}`,
        label: `${item.materialName || item.materialCode || item.materialId} 损耗数量`,
        beforeValue: correctionValueText(before.lossQuantity),
        afterValue: correctionValueText(item.lossQuantity)
      })
    }
  })

  const originalParameterMap = new Map(
    (event.deviceParameterReadings || []).map((item) => [
      `${item.deviceId}:${item.parameterCode}`,
      optionalCorrectionNumber(item.value)
    ])
  )
  correctionForm.deviceParameterReadings.forEach((item) => {
    const key = `${item.deviceId}:${item.parameterCode}`
    const before = originalParameterMap.get(key)
    const after = optionalCorrectionNumber(item.value)
    if (before !== undefined && after !== undefined && before !== after) {
      changes.push({
        key: `PARAMETER:${key}`,
        label: item.parameterName || item.parameterCode,
        beforeValue: correctionValueText(before, item.unit),
        afterValue: correctionValueText(after, item.unit)
      })
    } else if (before === undefined && after !== undefined) {
      changes.push({
        key: `PARAMETER:${key}`,
        label: item.parameterName || item.parameterCode,
        beforeValue: '--',
        afterValue: correctionValueText(after, item.unit)
      })
    }
  })
  return changes
})

const abnormalForm = reactive({
  workOrderId: 0,
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
  createType: 'DEVICE_BINDING' as ProcessConfigCreateType,
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
  standardText: '',
  lowerLimit: undefined as number | undefined,
  targetValue: undefined as number | undefined,
  upperLimit: undefined as number | undefined,
  valueType: 'DECIMAL' as DeviceParameterValueType,
  optionValues: [] as string[],
  defaultText: '',
  booleanDefault: false,
  decimalScale: undefined as number | undefined
})

const roughWashParameterForm = reactive({
  cleaningCount: 2,
  cleaningMedium: '自来水',
  powerLower: 20,
  powerDefault: 25,
  powerUpper: 30,
  roomTemperatureLower: 20,
  roomTemperatureDefault: 26,
  roomTemperatureUpper: 30,
  cleaningTime: 30
})

const abnormalRules = {
  abnormalDescription: [{ required: true, message: '异常原因不能为空', trigger: 'blur' }]
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

const resolveCurrentLeaderType = (): TeamLeaderType => {
  const leaderType = activeLeaderTab.value
  if (leaderType !== 'PRODUCTION' && leaderType !== 'PQC') {
    throw new Error('班组长类型不能为空')
  }
  return leaderType
}

const requirePositiveInteger = (value: unknown, message: string) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0 || !Number.isInteger(parsed)) {
    throw new Error(message)
  }
  return parsed
}

const normalizeAllocationSubmitQuantity = (value: unknown, message: string) => {
  if (value === undefined || value === null || String(value).trim() === '') {
    return 0
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed < 0 || !Number.isInteger(parsed)) {
    throw new Error(message)
  }
  return parsed
}

const formatActiveOrderCode = (order: TeamLeaderActiveOrderRespVO) =>
  order.workOrderCode?.trim() || '未返回订单编号'

const formatActiveOrderProduct = (order: TeamLeaderActiveOrderRespVO) => {
  const productParts = [order.productName, order.productCode]
    .map((value) => String(value || '').trim())
    .filter(Boolean)
  return productParts.length > 0 ? productParts.join(' / ') : '未返回产品'
}

const formatActiveOrderQuantity = (order: TeamLeaderActiveOrderRespVO) => {
  const value = order.quantity
  if (value === undefined || value === null || String(value).trim() === '') {
    return '未返回数量'
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return String(value).trim()
  return parsed.toFixed(3).replace(/\.?0+$/, '')
}

const formatActiveOrderOption = (order: TeamLeaderActiveOrderRespVO) => {
  return `编码 ${formatActiveOrderCode(order)} / 产品 ${formatActiveOrderProduct(order)} / 数量 ${formatActiveOrderQuantity(order)}`
}

const formatActiveOrderProgressPercent = (value: number | string | undefined) => {
  if (value === undefined || value === null || String(value).trim() === '') {
    return '未返回进度'
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return String(value).trim()
  return `${parsed.toFixed(1).replace(/\.0$/, '')}%`
}

const PRO_REPORT_ALLOCATION_VERSION_CONFLICT_CODE = 1040760357

const resolveErrorCode = (error: unknown) => {
  const value =
    (error as any)?.response?.data?.code ?? (error as any)?.data?.code ?? (error as any)?.code
  const code = Number(value)
  return Number.isFinite(code) ? code : undefined
}

const isReportAllocationVersionConflict = (error: unknown) =>
  resolveErrorCode(error) === PRO_REPORT_ALLOCATION_VERSION_CONFLICT_CODE

const isActiveOrderProgressComplete = (value: number | string | undefined) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 100
}

const formatActiveOrderReleaseStatus = (status?: string) => {
  if (status === 'PQC_RELEASE_PENDING') return '待PQC放行'
  if (status === 'PQC_RELEASE_REJECTED') return 'PQC已拒绝'
  if (status === 'REPORT_UPLOAD_PENDING') return '待上传放行报告'
  if (status === 'MANAGER_RELEASE_PENDING') return '待管理者代表放行'
  if (status === 'RELEASED') return '已放行'
  return '未申请'
}

const formatActiveOrderReleaseStatusTag = (status?: string) => {
  if (status === 'PQC_RELEASE_PENDING') return 'warning'
  if (status === 'PQC_RELEASE_REJECTED') return 'danger'
  if (status === 'REPORT_UPLOAD_PENDING') return 'primary'
  if (status === 'MANAGER_RELEASE_PENDING') return 'primary'
  if (status === 'RELEASED') return 'success'
  return 'info'
}

const resolveActiveOrderReleaseBlockerLocator = (
  blocker: TeamLeaderActiveOrderReleaseBlockerRespVO
) => {
  const locatorParts = [
    `对象类型：${blocker.objectType}`,
    `对象ID：${blocker.objectId}`,
    `对象编码：${blocker.objectCode}`
  ]
  if (blocker.routeProcessId !== undefined) {
    locatorParts.push(`路线工序ID：${blocker.routeProcessId}`)
  }
  if (blocker.processId !== undefined) {
    locatorParts.push(`工序ID：${blocker.processId}`)
  }
  if (blocker.fieldCode) {
    locatorParts.push(`字段：${blocker.fieldCode}`)
  }
  if (blocker.cellKey) {
    locatorParts.push(`单元格：${blocker.cellKey}`)
  }
  return locatorParts.join('；')
}

const isActiveOrderReleaseApplicationLocked = (activeOrderId: number) =>
  releaseApplicationLocks.has(activeOrderId)

const resolveActiveOrderRowClassName = ({ row }: { row: TeamLeaderActiveOrderRespVO }) =>
  row.hasQuantityConflict || row.quantityConflict
    ? 'team-leader-workbench__active-order-row--quantity-conflict'
    : ''

const canApplyActiveOrderRelease = (row: TeamLeaderActiveOrderRespVO) => {
  if (row.abnormal) return false
  if (row.hasQuantityConflict || row.quantityConflict) return false
  if (row.releaseApplicationStatus) return false
  return (
    isActiveOrderProgressComplete(row.productionProgressPercent) &&
    isActiveOrderProgressComplete(row.inspectionProgressPercent)
  )
}

const resolveActiveOrderReleaseApplyDisabledReason = (row: TeamLeaderActiveOrderRespVO) => {
  if (releaseApplicationLocks.get(row.id) === 'UNCERTAIN') {
    return '申请结果未确认，请人工核对后刷新页面'
  }
  if (releaseApplicationLocks.has(row.id)) return '本次申请已提交，请先刷新列表'
  if (row.hasQuantityConflict || row.quantityConflict) return '生产数量冲突未解决，需先由组长纠错'
  if (row.abnormal) return row.abnormalReason || '异常订单不能申请放行'
  if (row.releaseApplicationStatus) {
    return `已进入${formatActiveOrderReleaseStatus(row.releaseApplicationStatus)}`
  }
  if (!isActiveOrderProgressComplete(row.productionProgressPercent)) return '生产进度未达到100%'
  if (!isActiveOrderProgressComplete(row.inspectionProgressPercent)) return '检验进度未达到100%'
  return '提交生产放行申请'
}

const formatTraceQuantity = (value: number | string | undefined) => {
  if (value === undefined || value === null || value === '') return '-'
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed.toFixed(3) : String(value)
}

const navigateActiveOrderSubmissionDetail = (
  activeOrderId: number,
  sourceWorkOrderCode?: string
) => {
  router.push({
    name: 'MesProcessPoolActiveOrderSubmissionDetail',
    params: { activeOrderId },
    query: sourceWorkOrderCode
      ? { sourceWorkOrderCode }
      : undefined
  })
}

const resolveStage1GeneratedDetailTarget = (row: TeamLeaderActiveOrderRespVO) => {
  const sourceActiveOrderId = requirePositiveNumber(row.id, '活跃订单记录ID不能为空')
  const persistedGeneratedActiveOrderId = normalizePositiveNumber(row.stage1GeneratedActiveOrderId)
  if (persistedGeneratedActiveOrderId !== undefined) {
    return {
      activeOrderId: persistedGeneratedActiveOrderId,
      sourceWorkOrderCode: row.workOrderCode || row.stage1GeneratedWorkOrderCode || ''
    }
  }
  return stage1GeneratedDetailTargets.value.get(sourceActiveOrderId)
}

const openActiveOrderSubmissionDetail = (row: TeamLeaderActiveOrderRespVO) => {
  const sourceActiveOrderId = requirePositiveNumber(row.id, '活跃订单记录ID不能为空')
  const stage1GeneratedTarget = resolveStage1GeneratedDetailTarget(row)
  navigateActiveOrderSubmissionDetail(
    stage1GeneratedTarget?.activeOrderId ?? sourceActiveOrderId,
    stage1GeneratedTarget?.sourceWorkOrderCode
  )
}
const resolveActiveOrderConflictProcesses = (
  detailResult?: TeamLeaderActiveOrderDetailRespVO
) =>
  [...(detailResult?.processes ?? [])].sort(
    (left, right) => Number(Boolean(right.quantityConflict)) - Number(Boolean(left.quantityConflict))
  )

const resolveActiveOrderConflictCount = (detailResult?: TeamLeaderActiveOrderDetailRespVO) =>
  (detailResult?.processes ?? []).filter((process) => process.quantityConflict).length

const resolveActiveOrderNormalProcessCount = (
  detailResult?: TeamLeaderActiveOrderDetailRespVO
) => (detailResult?.processes ?? []).filter((process) => !process.quantityConflict).length

const loadActiveOrderConflictDetail = async (activeOrderId: number) => {
  activeOrderConflictLoading.value = true
  activeOrderConflictError.value = ''
  activeOrderConflictDetail.value = undefined
  try {
    const detailResult = await getTeamLeaderActiveOrderDetail(activeOrderId)
    if (!detailResult.processes?.length) {
      throw new Error('活跃订单缺少正式工序目标，无法处理数量冲突')
    }
    activeOrderConflictDetail.value = detailResult
  } catch (error) {
    activeOrderConflictError.value = resolveErrorMessage(error, '数量冲突详情加载失败')
    ElMessage.error(activeOrderConflictError.value)
  } finally {
    activeOrderConflictLoading.value = false
  }
}

const openActiveOrderConflictDrawer = async (row: TeamLeaderActiveOrderRespVO) => {
  const activeOrderId = requirePositiveNumber(row.id, '活跃订单记录ID不能为空')
  activeOrderConflictSelectedOrder.value = row
  activeOrderConflictActiveOrderId.value = activeOrderId
  activeOrderConflictDrawerVisible.value = true
  await loadActiveOrderConflictDetail(activeOrderId)
}

const retryActiveOrderConflictDetail = async () => {
  const activeOrderId = requirePositiveNumber(
    activeOrderConflictActiveOrderId.value,
    '活跃订单记录ID不能为空'
  )
  await loadActiveOrderConflictDetail(activeOrderId)
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
    const maxPage = Math.max(
      1,
      Math.ceil(pqcPersonnelRows.value.length / pqcPersonnelQuery.pageSize)
    )
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
    const maxPage = Math.max(
      1,
      Math.ceil(productionPersonnelRows.value.length / productionPersonnelQuery.pageSize)
    )
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
  allocationSnapshot.value = undefined
  allocationSaveIdempotencyState.value = undefined
}

const loadActiveOrders = async () => {
  activeOrderLoading.value = true
  try {
    activeOrderOptions.value = await getTeamLeaderActiveOrderList()
    const maxPage = Math.max(
      1,
      Math.ceil(activeOrderOptions.value.length / activeOrderQuery.pageSize)
    )
    if (activeOrderQuery.pageNo > maxPage) {
      activeOrderQuery.pageNo = maxPage
    }
  } catch (error) {
    activeOrderOptions.value = []
    throw error
  } finally {
    activeOrderLoading.value = false
  }
}

const buildProcessConfigQuery = (): TeamLeaderProcessConfigListReqVO => ({
  routeKeyword: processConfigQuery.routeKeyword?.trim() || undefined,
  processKeyword: processConfigQuery.processKeyword?.trim() || undefined,
  lossReasonKeyword: processConfigQuery.lossReasonKeyword?.trim() || undefined,
  deviceKeyword: processConfigQuery.deviceKeyword?.trim() || undefined,
  parameterKeyword: processConfigQuery.parameterKeyword?.trim() || undefined
})

const hasProcessConfigFilter = () => Object.values(buildProcessConfigQuery()).some(Boolean)

async function queryProcessConfigRows() {
  if (!isProductionLeader.value) {
    processConfigDisplayRows.value = []
    return
  }
  processConfigLoading.value = true
  try {
    const rows = await getTeamLeaderProcessConfigList(buildProcessConfigQuery())
    processConfigDisplayRows.value = rows
    if (!hasProcessConfigFilter()) {
      processConfigRows.value = rows
    }
  } catch (error) {
    processConfigDisplayRows.value = []
    ElMessage.error(resolveErrorMessage(error, '工序配置筛选查询失败'))
  } finally {
    processConfigLoading.value = false
  }
}

const saveProcessConfigOverageLimit = async (row: TeamLeaderProcessConfigRowRespVO) => {
  const overagePercent = Number(row.overagePercent)
  if (!Number.isFinite(overagePercent) || overagePercent < 0 || overagePercent > 100) {
    ElMessage.error('允许超量比例必须在 0% 到 100% 之间')
    return
  }
  processConfigSubmitting.value = true
  try {
    const saved = await saveTeamLeaderProcessOverageLimit({
      routeProcessId: row.routeProcessId,
      processId: row.processId,
      overagePercent
    })
    row.overagePercent = saved.overagePercent
    ElMessage.success('允许超量比例已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '允许超量比例保存失败'))
    await loadProcessConfigRows()
  } finally {
    processConfigSubmitting.value = false
  }
}

const {
  state: processConfigFilterState,
  applyMultiFilter: applyProcessConfigFilter,
  resetMultiFilter: resetProcessConfigFilter,
  updateState: updateProcessConfigFilterState,
  removeCondition: removeProcessConfigFilterCondition
} = useTableMultiFilter(
  PROCESS_CONFIG_TABLE_KEY,
  processConfigFilterDefinitions,
  processConfigQuery,
  queryProcessConfigRows
)

const loadProcessConfigRows = async () => {
  if (!isProductionLeader.value) {
    processConfigRows.value = []
    processConfigDisplayRows.value = []
    return
  }
  processConfigLoading.value = true
  let baselineLoaded = false
  try {
    const baselineRows = await getTeamLeaderProcessConfigList()
    processConfigRows.value = baselineRows
    baselineLoaded = true
    processConfigDisplayRows.value = hasProcessConfigFilter()
      ? await getTeamLeaderProcessConfigList(buildProcessConfigQuery())
      : baselineRows
  } catch (error) {
    if (!baselineLoaded) {
      processConfigRows.value = []
    }
    processConfigDisplayRows.value = []
    throw error
  } finally {
    processConfigLoading.value = false
  }
}

const processConfigDeviceOptions = computed(() => teamDeviceOptions.value)

const loadTeamDeviceOptions = async () => {
  teamDeviceOptions.value = await getTeamDeviceList(true)
}

const loadResponsibleRoutes = async () => {
  if (!isProductionLeader.value) {
    responsibleRouteRows.value = []
    return
  }
  responsibleRouteLoading.value = true
  try {
    responsibleRouteRows.value = await getTeamLeaderResponsibleRouteList()
  } catch (error) {
    responsibleRouteRows.value = []
    throw error
  } finally {
    responsibleRouteLoading.value = false
  }
}

const productionResponsibleRouteNames = computed(() => {
  const seen = new Set<string>()
  const routeNames: string[] = []
  responsibleRouteRows.value.forEach((row) => {
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

const lossReasonMaintenanceRow = computed(() =>
  processConfigRows.value.find(
    (row) => row.routeProcessId === lossReasonMaintenanceRouteProcessId.value
  )
)

const lossReasonMaintenanceReasons = computed(
  () => lossReasonMaintenanceRow.value?.lossReasons ?? []
)

const lossReasonEditorActive = computed(() => lossReasonDialogMode.value !== 'idle')

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

const formatProcessConfigDevice = (
  device: Pick<TeamLeaderProcessConfigDeviceVO, 'deviceId' | 'deviceCode' | 'deviceName'>
) => {
  const code = device.deviceCode ? `${device.deviceCode} / ` : ''
  const name = device.deviceName || device.deviceId || '--'
  return `${code}${name}`
}

const hasProcessConfigParameters = (row: TeamLeaderProcessConfigRowRespVO) =>
  row.devices?.some((device) => device.parameters?.length) ?? false

const isProcessConfigNumericValueType = (valueType?: DeviceParameterValueType | string) =>
  valueType === 'INTEGER' || valueType === 'DECIMAL'

const isProcessConfigNumericParameter = (parameter: TeamLeaderProcessConfigParameterVO) =>
  isProcessConfigNumericValueType(parameter.valueType)

const normalizeProcessConfigOptionValues = (values: string[]) =>
  Array.from(new Set(values.map((value) => value.trim()).filter(Boolean)))

const formatProcessConfigOptionValues = (parameter: TeamLeaderProcessConfigParameterVO) =>
  parameter.optionValues?.length ? parameter.optionValues.join('、') : '--'

const resolveProcessConfigBooleanDefault = (parameter: TeamLeaderProcessConfigParameterVO) => {
  const targetValue = toOptionalProcessConfigNumber(parameter.targetValue)
  if (targetValue !== 0 && targetValue !== 1) {
    throw new Error(`BOOLEAN 设备参数缺少 0/1 目标值：${parameter.parameterCode}`)
  }
  return targetValue === 1
}

const formatProcessConfigBooleanDefault = (parameter: TeamLeaderProcessConfigParameterVO) =>
  resolveProcessConfigBooleanDefault(parameter) ? '已选' : '未选'

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

const resolveCleaningWashProcessConfig = (
  row: TeamLeaderProcessConfigRowRespVO,
  device: TeamLeaderProcessConfigDeviceVO
) => {
  const processText = [row.processName, row.processCode].filter(Boolean).join(' ')
  const deviceText = [device.deviceName, device.deviceCode].filter(Boolean).join(' ')
  if (!deviceText.includes('超声波清洗机')) return undefined
  return CLEANING_WASH_PROCESS_CONFIGS.find((config) => processText.includes(config.processKeyword))
}

const syncProcessConfigCreateDevice = () => {
  processConfigCreateForm.deviceId = processConfigCreateDeviceOptions.value[0]?.deviceId
}

const resetProcessConfigCreateForm = () => {
  processConfigCreateForm.routeProcessId = processConfigRows.value[0]?.routeProcessId
  processConfigCreateForm.createType = 'DEVICE_BINDING'
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
  openProcessConfigParameterMaintenance(row, device, { create: true })
}

const resetLossReasonForm = () => {
  lossReasonForm.reasonCode = ''
  lossReasonForm.reasonName = ''
  lossReasonForm.enabled = true
  lossReasonForm.remark = ''
}

const cancelLossReasonEditor = () => {
  lossReasonDialogMode.value = 'idle'
  lossReasonEditingReasonId.value = undefined
  resetLossReasonForm()
}

const resetLossReasonMaintenance = () => {
  cancelLossReasonEditor()
  lossReasonMaintenanceRouteProcessId.value = undefined
}

const openLossReasonMaintenanceDialog = (row: TeamLeaderProcessConfigRowRespVO) => {
  lossReasonMaintenanceRouteProcessId.value = row.routeProcessId
  cancelLossReasonEditor()
  lossReasonMaintenanceDialogVisible.value = true
}

const startCreateLossReason = () => {
  if (lossReasonSubmitting.value || lossReasonEditorActive.value) return
  lossReasonDialogMode.value = 'create'
  lossReasonEditingReasonId.value = undefined
  resetLossReasonForm()
}

const startEditLossReason = (reason: TeamLeaderLossReasonVO) => {
  if (lossReasonSubmitting.value || lossReasonEditorActive.value) return
  lossReasonDialogMode.value = 'edit'
  lossReasonEditingReasonId.value = reason.id
  lossReasonForm.reasonCode = reason.reasonCode
  lossReasonForm.reasonName = reason.reasonName
  lossReasonForm.enabled = reason.enabled
  lossReasonForm.remark = ''
}

const isLossReasonEditing = (reason: TeamLeaderLossReasonVO) =>
  lossReasonDialogMode.value === 'edit' && lossReasonEditingReasonId.value === reason.id

const submitLossReason = async () => {
  const row = lossReasonMaintenanceRow.value
  if (!row) {
    ElMessage.error('请先选择工序')
    return
  }
  if (!lossReasonEditorActive.value) {
    ElMessage.error('请先新增或选择要修改的损耗原因')
    return
  }
  const reasonName = lossReasonForm.reasonName.trim()
  if (!reasonName) {
    ElMessage.error('请输入损耗原因名称')
    return
  }
  lossReasonSubmitting.value = true
  try {
    if (lossReasonDialogMode.value === 'create') {
      await createTeamLeaderLossReason({
        routeProcessId: row.routeProcessId,
        reasonName
      })
    } else {
      const reasonId = requirePositiveNumber(
        lossReasonEditingReasonId.value,
        '损耗原因编号不能为空'
      )
      await updateTeamLeaderLossReason(reasonId, {
        reasonName,
        enabled: lossReasonForm.enabled,
        remark: lossReasonForm.remark.trim() || undefined
      })
    }
    cancelLossReasonEditor()
    try {
      await loadProcessConfigRows()
    } catch (error) {
      ElMessage.error(
        `损耗原因已保存，但列表刷新失败：${resolveErrorMessage(error, '列表刷新失败')}`
      )
      return
    }
    ElMessage.success('损耗原因已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '损耗原因保存失败'))
  } finally {
    lossReasonSubmitting.value = false
  }
}

const handleDeleteLossReason = async (reason: TeamLeaderLossReasonVO) => {
  if (lossReasonEditorActive.value || lossReasonSubmitting.value) {
    ElMessage.warning('请先保存或取消当前编辑')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除损耗原因「${reason.reasonCode} / ${reason.reasonName}」？删除后不能用于新报工，历史报工快照不受影响。`,
      '删除损耗原因',
      { type: 'warning' }
    )
    lossReasonSubmitting.value = true
    await deleteTeamLeaderLossReason(reason.id)
    try {
      await loadProcessConfigRows()
    } catch (error) {
      ElMessage.error(
        `损耗原因已删除，但列表刷新失败：${resolveErrorMessage(error, '列表刷新失败')}`
      )
      return
    }
    ElMessage.success('损耗原因已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(resolveErrorMessage(error, '损耗原因删除失败'))
    }
  } finally {
    lossReasonSubmitting.value = false
  }
}

const toOptionalProcessConfigNumber = (value: unknown) => {
  if (value === undefined || value === null || value === '') return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const resetProcessConfigParameterForm = () => {
  processConfigParameterForm.deviceId = processConfigSelectedDevice.value?.deviceId
  processConfigParameterForm.parameterCode = ''
  processConfigParameterForm.parameterName = ''
  processConfigParameterForm.unit = ''
  processConfigParameterForm.standardText = ''
  processConfigParameterForm.lowerLimit = undefined
  processConfigParameterForm.targetValue = undefined
  processConfigParameterForm.upperLimit = undefined
  processConfigParameterForm.valueType = 'DECIMAL'
  processConfigParameterForm.optionValues = []
  processConfigParameterForm.defaultText = ''
  processConfigParameterForm.booleanDefault = false
  processConfigParameterForm.decimalScale = undefined
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
  processConfigEditingParameter.value = options.create
    ? undefined
    : (parameter ?? device.parameters?.[0])
  resetProcessConfigParameterForm()
  if (processConfigEditingParameter.value) {
    processConfigParameterForm.parameterCode = processConfigEditingParameter.value.parameterCode
    processConfigParameterForm.parameterName =
      processConfigEditingParameter.value.parameterName || ''
    processConfigParameterForm.unit = processConfigEditingParameter.value.unit || ''
    processConfigParameterForm.standardText = processConfigEditingParameter.value.standardText || ''
    processConfigParameterForm.lowerLimit = toOptionalProcessConfigNumber(
      processConfigEditingParameter.value.lowerLimit
    )
    processConfigParameterForm.targetValue = toOptionalProcessConfigNumber(
      processConfigEditingParameter.value.targetValue
    )
    processConfigParameterForm.upperLimit = toOptionalProcessConfigNumber(
      processConfigEditingParameter.value.upperLimit
    )
    processConfigParameterForm.valueType =
      processConfigEditingParameter.value.valueType || 'DECIMAL'
    processConfigParameterForm.booleanDefault =
      processConfigParameterForm.valueType === 'BOOLEAN'
        ? resolveProcessConfigBooleanDefault(processConfigEditingParameter.value)
        : false
    processConfigParameterForm.optionValues = [
      ...(processConfigEditingParameter.value.optionValues || [])
    ]
    processConfigParameterForm.defaultText = processConfigEditingParameter.value.defaultText || ''
    processConfigParameterForm.decimalScale = toOptionalProcessConfigNumber(
      processConfigEditingParameter.value.decimalScale
    )
  }
  processConfigParameterDialogVisible.value = true
}

const resetRoughWashParameterForm = (config: CleaningWashProcessConfig) => {
  roughWashParameterForm.cleaningCount = 2
  roughWashParameterForm.cleaningMedium = config.defaultCleaningMedium
  roughWashParameterForm.powerLower = 20
  roughWashParameterForm.powerDefault = 25
  roughWashParameterForm.powerUpper = 30
  roughWashParameterForm.roomTemperatureLower = 20
  roughWashParameterForm.roomTemperatureDefault = 26
  roughWashParameterForm.roomTemperatureUpper = 30
  roughWashParameterForm.cleaningTime = 30
}

const findRoughWashParameter = (device: TeamLeaderProcessConfigDeviceVO, parameterCode: string) =>
  device.parameters?.find((parameter) => parameter.parameterCode === parameterCode)

const getCleaningWashDeviceParameterTemplates = (config: CleaningWashProcessConfig) =>
  CLEANING_WASH_DEVICE_PARAMETER_TEMPLATES_BY_KIND[config.kind]

const cleaningWashParameterCode = (config: CleaningWashProcessConfig, suffix: string) =>
  config.parameterCodePrefix + '_' + suffix

const applyExistingRoughWashParameterValues = (
  device: TeamLeaderProcessConfigDeviceVO,
  config: CleaningWashProcessConfig
) => {
  const cleaningCount = findRoughWashParameter(device, cleaningWashParameterCode(config, 'COUNT'))
  const cleaningMedium = findRoughWashParameter(device, cleaningWashParameterCode(config, 'MEDIUM'))
  const cleaningPower = findRoughWashParameter(device, cleaningWashParameterCode(config, 'POWER'))
  const roomTemperature = findRoughWashParameter(
    device,
    cleaningWashParameterCode(config, 'ROOM_TEMPERATURE')
  )
  const cleaningTime = findRoughWashParameter(device, cleaningWashParameterCode(config, 'TIME'))

  roughWashParameterForm.cleaningCount =
    toOptionalProcessConfigNumber(cleaningCount?.targetValue) ??
    roughWashParameterForm.cleaningCount
  roughWashParameterForm.cleaningMedium =
    cleaningMedium?.defaultText ?? roughWashParameterForm.cleaningMedium
  roughWashParameterForm.powerLower =
    toOptionalProcessConfigNumber(cleaningPower?.lowerLimit) ?? roughWashParameterForm.powerLower
  roughWashParameterForm.powerDefault =
    toOptionalProcessConfigNumber(cleaningPower?.targetValue) ?? roughWashParameterForm.powerDefault
  roughWashParameterForm.powerUpper =
    toOptionalProcessConfigNumber(cleaningPower?.upperLimit) ?? roughWashParameterForm.powerUpper
  roughWashParameterForm.roomTemperatureLower =
    toOptionalProcessConfigNumber(roomTemperature?.lowerLimit) ??
    roughWashParameterForm.roomTemperatureLower
  roughWashParameterForm.roomTemperatureDefault =
    toOptionalProcessConfigNumber(roomTemperature?.targetValue) ??
    roughWashParameterForm.roomTemperatureDefault
  roughWashParameterForm.roomTemperatureUpper =
    toOptionalProcessConfigNumber(roomTemperature?.upperLimit) ??
    roughWashParameterForm.roomTemperatureUpper
  roughWashParameterForm.cleaningTime =
    toOptionalProcessConfigNumber(cleaningTime?.targetValue) ?? roughWashParameterForm.cleaningTime
}

const openRoughWashParameterConfigDialog = (
  row: TeamLeaderProcessConfigRowRespVO,
  device: TeamLeaderProcessConfigDeviceVO,
  config: CleaningWashProcessConfig
) => {
  processConfigSelectedRow.value = row
  processConfigSelectedDevice.value = device
  activeCleaningWashProcessConfig.value = config
  processConfigEditingParameter.value = undefined
  resetRoughWashParameterForm(config)
  applyExistingRoughWashParameterValues(device, config)
  roughWashParameterDialogVisible.value = true
}

const openProcessConfigParameterMaintenance = (
  row: TeamLeaderProcessConfigRowRespVO,
  device: TeamLeaderProcessConfigDeviceVO,
  options: { create?: boolean } = {}
) => {
  const cleaningWashConfig = resolveCleaningWashProcessConfig(row, device)
  if (cleaningWashConfig) {
    openRoughWashParameterConfigDialog(row, device, cleaningWashConfig)
    return
  }
  openProcessConfigParameterDialog(row, device, undefined, options)
}

const requireRoughWashNumber = (value: unknown, label: string) => {
  const parsed = toOptionalProcessConfigNumber(value)
  if (parsed === undefined) {
    throw new Error(`${label}不能为空`)
  }
  return parsed
}

const requireRoughWashInteger = (value: unknown, label: string) => {
  const parsed = requireRoughWashNumber(value, label)
  if (!Number.isInteger(parsed)) {
    throw new Error(`${label}必须是整数`)
  }
  return parsed
}

const requireRoughWashSingleDecimal = (value: unknown, label: string) => {
  const parsed = requireRoughWashNumber(value, label)
  if (Math.abs(parsed * 10 - Math.round(parsed * 10)) > Number.EPSILON * 10) {
    throw new Error(`${label}最多保留 1 位小数`)
  }
  return parsed
}

const buildRoughWashParameterSavePayloads = (
  row: TeamLeaderProcessConfigRowRespVO,
  device: TeamLeaderProcessConfigDeviceVO
): TeamDeviceParameterRuleSaveReqVO[] => {
  const cleaningWashConfig =
    activeCleaningWashProcessConfig.value ?? resolveCleaningWashProcessConfig(row, device)
  if (!cleaningWashConfig) {
    throw new Error('当前工序设备不支持固定清洗参数配置')
  }
  const routeProcessId = requirePositiveNumber(row.routeProcessId, '路线工序不能为空')
  const deviceId = requirePositiveNumber(device.deviceId, '设备不能为空')
  const cleaningCount = requireRoughWashInteger(roughWashParameterForm.cleaningCount, '清洗次数')
  const cleaningTime = requireRoughWashInteger(roughWashParameterForm.cleaningTime, '清洗时间')
  const powerLower = requireRoughWashInteger(roughWashParameterForm.powerLower, '清洗功率下限')
  const powerDefault = requireRoughWashInteger(roughWashParameterForm.powerDefault, '清洗功率默认')
  const powerUpper = requireRoughWashInteger(roughWashParameterForm.powerUpper, '清洗功率上限')
  const roomTemperatureLower = requireRoughWashSingleDecimal(
    roughWashParameterForm.roomTemperatureLower,
    '室温下限'
  )
  const roomTemperatureDefault = requireRoughWashSingleDecimal(
    roughWashParameterForm.roomTemperatureDefault,
    '室温默认'
  )
  const roomTemperatureUpper = requireRoughWashSingleDecimal(
    roughWashParameterForm.roomTemperatureUpper,
    '室温上限'
  )
  if (powerLower > powerDefault || powerDefault > powerUpper) {
    throw new Error('清洗功率必须满足下限不大于默认且默认不大于上限')
  }
  if (
    roomTemperatureLower > roomTemperatureDefault ||
    roomTemperatureDefault > roomTemperatureUpper
  ) {
    throw new Error('室温必须满足下限不大于默认且默认不大于上限')
  }
  if (!['自来水', '纯化水'].includes(roughWashParameterForm.cleaningMedium)) {
    throw new Error('清洗介质必须选择自来水或纯化水')
  }

  return getCleaningWashDeviceParameterTemplates(cleaningWashConfig).map((template) => {
    const common = {
      routeProcessId,
      deviceId,
      parameterCode: template.parameterCode,
      parameterName: template.parameterName,
      unit: template.unit,
      valueType: template.valueType,
      optionValues: template.optionValues ? [...template.optionValues] : undefined,
      defaultText: template.defaultText,
      decimalScale: template.decimalScale,
      lowerLimit: template.lowerLimit,
      targetValue: template.targetValue,
      upperLimit: template.upperLimit,
      standardText: template.standardText
    }
    switch (template.parameterCode) {
      case 'ROUGH_WASH_COUNT':
      case 'FINE_WASH_COUNT':
      case 'CLEANING_COUNT':
        return {
          ...common,
          lowerLimit: undefined,
          targetValue: cleaningCount,
          upperLimit: undefined,
          standardText: `清洗次数默认 ${cleaningCount} 次`
        }
      case 'ROUGH_WASH_MEDIUM':
      case 'FINE_WASH_MEDIUM':
      case 'CLEANING_MEDIUM':
        return {
          ...common,
          optionValues: template.optionValues ? [...template.optionValues] : undefined,
          defaultText: roughWashParameterForm.cleaningMedium,
          standardText: `清洗介质可选${template.optionValues?.join('或')}，默认${roughWashParameterForm.cleaningMedium}`
        }
      case 'ROUGH_WASH_POWER':
      case 'FINE_WASH_POWER':
      case 'CLEANING_POWER':
        return {
          ...common,
          lowerLimit: powerLower,
          targetValue: powerDefault,
          upperLimit: powerUpper,
          standardText: `清洗功率 ${powerLower}-${powerUpper}%，默认 ${powerDefault}%`
        }
      case 'ROUGH_WASH_ROOM_TEMPERATURE':
      case 'FINE_WASH_ROOM_TEMPERATURE':
      case 'CLEANING_ROOM_TEMPERATURE': {
        const roomTemperatureParameterName = resolveCleaningWashRoomTemperatureParameterName(
          cleaningWashConfig,
          device
        )
        return {
          ...common,
          parameterName: roomTemperatureParameterName,
          lowerLimit: roomTemperatureLower,
          targetValue: roomTemperatureDefault,
          upperLimit: roomTemperatureUpper,
          decimalScale: 1,
          standardText: `${roomTemperatureParameterName} ${roomTemperatureLower.toFixed(1)}-${roomTemperatureUpper.toFixed(1)}℃，默认 ${roomTemperatureDefault.toFixed(1)}℃`
        }
      }
      case 'ROUGH_WASH_TIME':
      case 'FINE_WASH_TIME':
      case 'CLEANING_TIME':
        return {
          ...common,
          lowerLimit: undefined,
          targetValue: cleaningTime,
          upperLimit: undefined,
          standardText: `清洗时间默认 ${cleaningTime} min`
        }
      default:
        throw new Error(`不支持的清洗参数编码：${template.parameterCode}`)
    }
  })
}

const submitRoughWashParameterConfig = async () => {
  const row = processConfigSelectedRow.value
  const device = processConfigSelectedDevice.value
  const cleaningWashConfig =
    activeCleaningWashProcessConfig.value ??
    (row && device ? resolveCleaningWashProcessConfig(row, device) : undefined)
  const cleaningWashLabel = cleaningWashConfig?.buttonLabel || '清洗参数'
  if (!row || !device) {
    ElMessage.error('请先选择清洗工序和超声波清洗机')
    return
  }
  let payloads: TeamDeviceParameterRuleSaveReqVO[]
  try {
    payloads = buildRoughWashParameterSavePayloads(row, device)
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, `${cleaningWashLabel}配置校验失败`))
    return
  }

  processConfigSubmitting.value = true
  try {
    try {
      await Promise.all(
        payloads.map((payload) => saveTeamProcessConfigDeviceParameterRule(payload))
      )
    } catch (error) {
      ElMessage.error(
        `${cleaningWashLabel}未全部保存，请重新保存：${resolveErrorMessage(error, '参数保存失败')}`
      )
      return
    }
    try {
      await loadProcessConfigRows()
    } catch (error) {
      ElMessage.error(
        `${cleaningWashLabel}已保存，但列表刷新失败：${resolveErrorMessage(error, '列表刷新失败')}`
      )
      return
    }
    roughWashParameterDialogVisible.value = false
    ElMessage.success(`${cleaningWashLabel}配置已保存`)
  } finally {
    processConfigSubmitting.value = false
  }
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
  const standardText = processConfigParameterForm.standardText
  if (!standardText.trim()) {
    ElMessage.error('参数原文标准不能为空')
    return
  }
  const valueType = processConfigParameterForm.valueType
  const selectStandard = valueType === 'SELECT'
  const booleanStandard = valueType === 'BOOLEAN'
  const numericStandard = isProcessConfigNumericValueType(valueType)
  const optionValues = selectStandard
    ? normalizeProcessConfigOptionValues(processConfigParameterForm.optionValues)
    : undefined
  const defaultText = selectStandard ? processConfigParameterForm.defaultText.trim() : undefined
  if (selectStandard && !optionValues?.length) {
    ElMessage.error('下拉参数至少需要配置一个选项')
    return
  }
  if (selectStandard && defaultText && !optionValues?.includes(defaultText)) {
    ElMessage.error('下拉参数默认文本必须来自下拉选项')
    return
  }
  const lowerLimit = numericStandard
    ? toOptionalProcessConfigNumber(processConfigParameterForm.lowerLimit)
    : undefined
  const targetValue = booleanStandard
    ? processConfigParameterForm.booleanDefault
      ? 1
      : 0
    : numericStandard
      ? toOptionalProcessConfigNumber(processConfigParameterForm.targetValue)
      : undefined
  const upperLimit = numericStandard
    ? toOptionalProcessConfigNumber(processConfigParameterForm.upperLimit)
    : undefined
  const decimalScale =
    valueType === 'DECIMAL'
      ? toOptionalProcessConfigNumber(processConfigParameterForm.decimalScale)
      : undefined
  if (valueType === 'INTEGER') {
    const integerValues = [lowerLimit, targetValue, upperLimit].filter(
      (value): value is number => value !== undefined
    )
    if (integerValues.some((value) => !Number.isInteger(value))) {
      ElMessage.error('整数参数的上下限和目标值必须为整数')
      return
    }
  }
  if (valueType === 'DECIMAL' && decimalScale !== undefined) {
    if (!Number.isInteger(decimalScale) || decimalScale < 0 || decimalScale > 6) {
      ElMessage.error('小数位数必须是 0 到 6 的整数')
      return
    }
  }
  if (lowerLimit !== undefined && upperLimit !== undefined && lowerLimit > upperLimit) {
    ElMessage.error('参数区间必须满足下限 <= 上限')
    return
  }
  if (targetValue !== undefined && lowerLimit !== undefined && lowerLimit > targetValue) {
    ElMessage.error('参数区间必须满足下限 <= 目标值')
    return
  }
  if (targetValue !== undefined && upperLimit !== undefined && targetValue > upperLimit) {
    ElMessage.error('参数区间必须满足目标值 <= 上限')
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
      standardText,
      lowerLimit,
      targetValue,
      upperLimit,
      valueType,
      optionValues,
      defaultText: defaultText || undefined,
      decimalScale
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

const markManualAllocation = (_row?: TeamLeaderReportAllocationDraftLine) => {
  reviewForm.allocationMode = 'MANUAL'
}

const addAllocationLine = () => {
  reviewForm.allocationMode = 'MANUAL'
  allocationRows.value.push({
    activeOrderId: undefined,
    allocatedQuantity: 0,
    editable: true,
    released: false
  })
}

const removeAllocationLine = (index: number) => {
  if (allocationRows.value[index]?.editable === false) return
  reviewForm.allocationMode = 'MANUAL'
  allocationRows.value.splice(index, 1)
}

const startBlankAllocation = () => {
  reviewForm.allocationMode = 'MANUAL'
  allocationRows.value = allocationRows.value.filter((line) => line.editable === false)
}

const getAvailableAllocationOrderOptions = (line: TeamLeaderReportAllocationDraftLine) => {
  const selectedWorkOrderIds = new Set(
    allocationRows.value
      .filter((candidate) => candidate !== line)
      .map((candidate) =>
        allocatableActiveOrderOptions.value.find(
          (order) => Number(order.id) === Number(candidate.activeOrderId)
        )
      )
      .map((order) => normalizePositiveNumber(order?.workOrderId))
      .filter((workOrderId): workOrderId is number => workOrderId !== undefined)
  )
  return allocatableActiveOrderOptions.value.filter((order) => {
    const workOrderId = normalizePositiveNumber(order.workOrderId)
    return workOrderId !== undefined && !selectedWorkOrderIds.has(workOrderId)
  })
}

const normalizeAllocationInteger = (value: unknown) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) return 0
  return Math.floor(parsed)
}

const findAllocationActiveOrder = (line: TeamLeaderReportAllocationDraftLine) => {
  const activeOrderId = Number(line.activeOrderId)
  if (!Number.isFinite(activeOrderId)) {
    return undefined
  }
  return allocatableActiveOrderOptions.value.find((item) => Number(item.id) === activeOrderId)
}

const resolveAllocationOverageQuantity = (line: TeamLeaderReportAllocationDraftLine) => {
  if (line.editable === false) return 0
  const persistedOverageQuantity = normalizeAllocationInteger(line.overageQuantity)
  if (line.needsAdjustment && persistedOverageQuantity > 0) return persistedOverageQuantity
  const allocatedQuantity = normalizeAllocationInteger(line.allocatedQuantity)
  if (allocatedQuantity === 0) return 0
  const orderQuantity = resolveActiveOrderFormalQuantity(findAllocationActiveOrder(line))
  return orderQuantity === undefined
    ? allocatedQuantity
    : Math.max(0, allocatedQuantity - orderQuantity)
}

const prefillSelectedOrderAllocation = (
  event: ProcessPoolTimelineEventVO,
  snapshot: TeamLeaderReportAllocationSnapshotRespVO
) => {
  if (snapshot.lines?.length) return
  const workOrderId = Number(event.workOrderId)
  const outputQuantity = requirePositiveInteger(event.outputQuantity, '本次报工数量必须为正整数')
  const selectedOrder = activeOrderOptions.value.find(
    (order) => Number(order.workOrderId) === workOrderId
  )
  if (!selectedOrder) return
  reviewForm.allocationMode = 'MANUAL'
  allocationRows.value = [
    {
      activeOrderId: requirePositiveNumber(selectedOrder.id, '原报工活跃订单不能为空'),
      workOrderId: selectedOrder.workOrderId,
      workOrderCode: selectedOrder.workOrderCode,
      allocatedQuantity: outputQuantity,
      editable: true,
      released: false
    }
  ]
}

const formatAllocationOrderProductionQuantity = (line: TeamLeaderReportAllocationDraftLine) => {
  const order = findAllocationActiveOrder(line)
  return order ? formatTraceQuantity(order.erpFixedQuantitySnapshot ?? order.quantity) : '--'
}

const formatAllocationOrderProductionCoefficient = (line: TeamLeaderReportAllocationDraftLine) => {
  const order = findAllocationActiveOrder(line)
  if (!order) {
    return '--'
  }
  const value = order.productionCoefficient
  if (value === undefined || value === null || String(value).trim() === '') {
    return '1.0'
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return String(value)
  }
  return parsed.toFixed(3).replace(/\.?0+$/, '')
}

const resolveAllocationActiveOrder = (line: TeamLeaderReportAllocationDraftLine) => {
  const order = findAllocationActiveOrder(line)
  if (!order) {
    throw new Error('请选择活跃订单后再快捷分配')
  }
  return order
}

const resolveCurrentAllocationRemainingQuantity = (line: TeamLeaderReportAllocationDraftLine) => {
  const totalQuantity = requirePositiveInteger(
    allocationSnapshot.value?.poolQuantity ?? reviewEvent.value?.outputQuantity,
    '本次报工数量必须为正整数'
  )
  const allocatedExceptCurrent = allocationRows.value.reduce((total, item) => {
    if (item === line) return total
    return total + normalizeAllocationInteger(item.allocatedQuantity)
  }, 0)
  const currentRemainingQuantity = totalQuantity - allocatedExceptCurrent
  if (currentRemainingQuantity <= 0) {
    throw new Error('本次报工剩余可分配数量必须大于 0')
  }
  return currentRemainingQuantity
}

const resolveAllocationOrderProcessRemainingQuantity = (order: TeamLeaderActiveOrderRespVO) => {
  const processId = requirePositiveNumber(reviewEvent.value?.processId, '当前工序不能为空')
  const processMatches =
    order.processRemainingQuantities?.filter((item) => Number(item.processId) === processId) ?? []
  if (processMatches.length !== 1) {
    throw new Error('当前工序剩余可分配数量必须为正整数')
  }
  const remainingQuantity = processMatches[0].remainingQuantity
  return requirePositiveInteger(remainingQuantity, '当前工序剩余可分配数量必须为正整数')
}

const resolveAllocationShortcutQuantity = (
  line: TeamLeaderReportAllocationDraftLine,
  mode: AllocationShortcutMode
) => {
  const order = resolveAllocationActiveOrder(line)
  const orderProcessRemainingQuantity = resolveAllocationOrderProcessRemainingQuantity(order)
  const currentRemainingQuantity = resolveCurrentAllocationRemainingQuantity(line)
  if (mode === 'MAX') {
    return Math.min(orderProcessRemainingQuantity, currentRemainingQuantity)
  }
  const halfOrderQuantity = Math.floor(orderProcessRemainingQuantity / 2)
  const shortcutQuantity = Math.min(halfOrderQuantity, currentRemainingQuantity)
  if (shortcutQuantity <= 0) {
    throw new Error('订单一半数量必须大于 0')
  }
  return shortcutQuantity
}

const applyAllocationShortcut = (
  line: TeamLeaderReportAllocationDraftLine,
  mode: AllocationShortcutMode
) => {
  try {
    line.allocatedQuantity = resolveAllocationShortcutQuantity(line, mode)
    markManualAllocation()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '快捷分配失败'))
  }
}

const clearAllocationQuantity = (line: TeamLeaderReportAllocationDraftLine) => {
  if (line.editable === false) return
  line.allocatedQuantity = 0
  markManualAllocation()
}

const previewFifoAllocation = async () => {
  const eventId = requirePositiveNumber(reviewEvent.value?.id, '工序池提交事件编号不能为空')
  allocationPreviewLoading.value = true
  try {
    const preview = await previewTeamLeaderReportFifoAllocation({
      eventId,
      leaderType: resolveCurrentLeaderType()
    })
    reviewForm.allocationMode = 'FIFO'
    allocationSnapshot.value = preview
    allocationRows.value = (preview.lines || []).map((line) => ({
      activeOrderId: requirePositiveNumber(line.activeOrderId, 'FIFO 分配返回活跃订单不能为空'),
      workOrderId: line.workOrderId,
      workOrderCode: line.workOrderCode,
      allocatedQuantity: line.allocatedQuantity,
      remainingQuantityBeforeAllocation: line.remainingQuantityBeforeAllocation,
      allocationId: line.allocationId,
      routeProcessId: line.routeProcessId,
      processId: line.processId,
      allocationMode: line.allocationMode,
      released: line.released === true,
      editable: line.editable !== false
    }))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'FIFO 自动分配失败'))
  } finally {
    allocationPreviewLoading.value = false
  }
}

const assertUniqueAllocationActiveOrders = () => {
  const selectedWorkOrderIds = new Set<number>()
  for (const line of allocationRows.value) {
    const activeOrderId = normalizePositiveNumber(line.activeOrderId)
    if (activeOrderId === undefined) continue
    const selectedOrder = allocatableActiveOrderOptions.value.find(
      (order) => Number(order.id) === activeOrderId
    )
    if (!selectedOrder) {
      throw new Error('所选活跃订单已不在当前订单池中')
    }
    const workOrderId = requirePositiveNumber(selectedOrder.workOrderId, '活跃订单缺少正式生产订单')
    if (selectedWorkOrderIds.has(workOrderId)) {
      throw new Error('同一订单编号不能重复分配')
    }
    selectedWorkOrderIds.add(workOrderId)
  }
}

const buildAllocationSubmitLines = (): TeamLeaderReportAllocationLine[] => {
  assertUniqueAllocationActiveOrders()
  return allocationRows.value
    .filter((line) => line.editable !== false)
    .flatMap((line) => {
      const allocatedQuantity = normalizeAllocationSubmitQuantity(line.allocatedQuantity, '分配数量必须为0或正整数')
      const activeOrderId = normalizePositiveNumber(line.activeOrderId)
      if (allocatedQuantity === 0 && activeOrderId === undefined) return []
      if (allocatedQuantity === 0) return []
      return [
        {
          activeOrderId: requirePositiveNumber(activeOrderId, '活跃订单不能为空'),
          allocatedQuantity
        }
      ]
    })
}

const allocationSaveRequestIdentity = (request: {
  eventId: number
  leaderType: TeamLeaderType
  allocationMode: 'FIFO' | 'MANUAL'
  expectedVersion?: number
  reviewRemark?: string
  allocations: TeamLeaderReportAllocationLine[]
}) => JSON.stringify(request)

const getOrCreateAllocationSaveIdempotencyKey = (request: {
  eventId: number
  leaderType: TeamLeaderType
  allocationMode: 'FIFO' | 'MANUAL'
  expectedVersion?: number
  reviewRemark?: string
  allocations: TeamLeaderReportAllocationLine[]
}) => {
  const requestIdentity = allocationSaveRequestIdentity(request)
  if (allocationSaveIdempotencyState.value?.requestIdentity !== requestIdentity) {
    allocationSaveIdempotencyState.value = {
      requestIdentity,
      key: crypto.randomUUID()
    }
  }
  return allocationSaveIdempotencyState.value.key
}

const applyAllocationSnapshot = (snapshot: TeamLeaderReportAllocationSnapshotRespVO) => {
  allocationSnapshot.value = snapshot
  allocationRows.value = (snapshot.lines || []).map((line) => ({ ...line }))
}

const buildReviewSignaturePayload = () => {
  const signaturePassword = reviewForm.reviewSignaturePassword.trim()
  if (!signaturePassword) {
    throw new Error('请输入电子签名密码')
  }
  return { signaturePassword }
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

interface SubmissionMaterialDetailRow {
  key: string
  materialText: string
  outputQuantityText: string
  lossQuantityText: string
  lossDetailText: string
  devices: SubmissionMaterialDeviceRow[]
}

interface SubmissionMaterialDeviceRow {
  key: string
  deviceNameText: string
  deviceCodeText: string
  meteringValidityText: string
  parameterText: string
  hasAbnormalParameter: boolean
}

interface ProductionParameterRuleSnapshot {
  parameterCode?: string
  parameterName?: string
  unit?: string
  lowerLimit?: number | string
  upperLimit?: number | string
}

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
  const sourceItems = Array.isArray(value) ? value : isRecord(value) ? Object.values(value) : []
  return sourceItems
    .map(toPqcItemSnapshotDetail)
    .filter((item): item is PqcItemSnapshotDetail => Boolean(item))
}

const resolvePqcPayloadPair = (row: ProcessPoolTimelineEventVO) => {
  const payload = parsePqcOriginalPayload(row.originalPayloadJson)
  const rootPayload = payload && isRecord(payload.rawPayload) ? payload.rawPayload : payload
  return { payload, rootPayload }
}

const normalizeProductionParameterStatus = (
  value: unknown
): ProductionReportCorrectionParameterRow['parameterStatus'] => {
  return value === 'BELOW_LOWER' || value === 'ABOVE_UPPER' ? value : 'NORMAL'
}

const toCorrectionParameterRow = (
  reading: unknown
): ProductionReportCorrectionParameterRow | undefined => {
  if (!isRecord(reading)) {
    return undefined
  }
  const deviceId = Number(reading.deviceId)
  const parameterCode = String(reading.parameterCode ?? '').trim()
  if (!Number.isFinite(deviceId) || deviceId <= 0 || !parameterCode) {
    return undefined
  }
  const valueType = String(reading.valueType ?? '').trim() || undefined
  return {
    deviceId,
    deviceCode: String(reading.deviceCode ?? '').trim() || undefined,
    deviceName: String(reading.deviceName ?? '').trim() || undefined,
    parameterCode,
    parameterName: String(reading.parameterName ?? '').trim() || undefined,
    unit: String(reading.unit ?? '').trim() || undefined,
    value:
      valueType === 'SELECT'
        ? String(reading.textValue ?? reading.value ?? '').trim()
        : optionalCorrectionNumber(reading.value),
    textValue: String(reading.textValue ?? '').trim() || undefined,
    valueType,
    optionValues: normalizeCorrectionOptionValues(reading.optionValues ?? reading.optionValuesJson),
    displayConfig: isRecord(reading.displayConfig) ? reading.displayConfig : undefined,
    lowerLimit: reading.lowerLimit as number | string | undefined,
    upperLimit: reading.upperLimit as number | string | undefined,
    parameterStatus: normalizeProductionParameterStatus(reading.parameterStatus)
  }
}

const resolveCorrectionParameterSnapshotKey = (
  parameterRow: Pick<ProductionReportCorrectionParameterRow, 'deviceId' | 'parameterCode'>
) => `${parameterRow.deviceId}:${parameterRow.parameterCode}`

const buildCorrectionParameterSnapshotMap = (event: ProcessPoolTimelineEventVO) => {
  const { rootPayload } = resolvePqcPayloadPair(event)
  const snapshotRows = [
    ...(event.deviceParameterReadings || []),
    ...normalizeSubmissionArray(rootPayload?.deviceParameterReadings)
  ]
  const snapshotMap = new Map<string, ProductionReportCorrectionParameterRow>()
  snapshotRows
    .map(toCorrectionParameterRow)
    .filter((item): item is ProductionReportCorrectionParameterRow => Boolean(item))
    .forEach((item) => {
      const key = resolveCorrectionParameterSnapshotKey(item)
      if (!snapshotMap.has(key)) {
        snapshotMap.set(key, item)
      }
    })
  return snapshotMap
}

const resolveCorrectionParameterRule = (
  event: ProcessPoolTimelineEventVO,
  parameterRow: ProductionReportCorrectionParameterRow
) => {
  const routeProcessId = Number(event.routeProcessId)
  if (!Number.isFinite(routeProcessId) || routeProcessId <= 0) {
    return undefined
  }
  const processConfig = processConfigRows.value.find(
    (row) => row.routeProcessId === routeProcessId
  )
  return processConfig?.devices
    .find((device) => device.deviceId === parameterRow.deviceId)
    ?.parameters.find((parameter) => parameter.parameterCode === parameterRow.parameterCode)
}

const mergeCorrectionParameterSnapshot = (
  event: ProcessPoolTimelineEventVO,
  parameterRow: ProductionReportCorrectionParameterRow,
  snapshotMap: Map<string, ProductionReportCorrectionParameterRow>
) => {
  const rule = resolveCorrectionParameterRule(event, parameterRow)
  const snapshot = snapshotMap.get(resolveCorrectionParameterSnapshotKey(parameterRow))
  const snapshotTextValue = String(snapshot?.textValue ?? '').trim()
  const snapshotValue = String(snapshot?.value ?? '').trim()
  const currentValue = String(parameterRow.value ?? '').trim()
  const valueType = parameterRow.valueType || snapshot?.valueType || rule?.valueType
  const optionValues = parameterRow.optionValues?.length
    ? parameterRow.optionValues
    : snapshot?.optionValues?.length
      ? snapshot.optionValues
      : rule?.optionValues || []
  const enriched: ProductionReportCorrectionParameterRow = {
    ...parameterRow,
    parameterName: parameterRow.parameterName || snapshot?.parameterName || rule?.parameterName,
    unit: parameterRow.unit || snapshot?.unit || rule?.unit,
    valueType,
    optionValues,
    value:
      isCorrectionSelectParameter({ ...parameterRow, valueType })
        ? currentValue || snapshotTextValue || snapshotValue || rule?.defaultText || ''
        : parameterRow.value ?? snapshot?.value,
    textValue: parameterRow.textValue || snapshot?.textValue,
    displayConfig: parameterRow.displayConfig || snapshot?.displayConfig,
    lowerLimit: parameterRow.lowerLimit ?? snapshot?.lowerLimit ?? rule?.lowerLimit ?? undefined,
    upperLimit: parameterRow.upperLimit ?? snapshot?.upperLimit ?? rule?.upperLimit ?? undefined,
    parameterStatus: parameterRow.parameterStatus || snapshot?.parameterStatus || 'NORMAL'
  }
  if (isCorrectionSelectParameter(enriched) && String(enriched.value ?? '').trim()) {
    enriched.textValue = String(enriched.value).trim()
  }
  return enriched
}

const enrichCorrectionMaterialParameterRows = (
  event: ProcessPoolTimelineEventVO,
  materialRows: ProductionReportCorrectionMaterialRow[]
) => {
  const snapshotMap = buildCorrectionParameterSnapshotMap(event)
  return materialRows.map((materialRow) => ({
    ...materialRow,
    deviceParameterReadings: materialRow.deviceParameterReadings.map((parameterRow) =>
      mergeCorrectionParameterSnapshot(event, parameterRow, snapshotMap)
    )
  }))
}

const isPlaceholderMaterialName = (value?: string) =>
  /^物料\s*\d+$/.test(String(value ?? '').trim())

const resolveSubmittedMaterialName = (value: PqcSubmissionPayloadRecord) => {
  const material = isRecord(value.material) ? value.material : undefined
  return (
    String(
      value.materialName ??
        value.itemName ??
        value.productName ??
        value.outputMaterialName ??
        material?.materialName ??
        material?.name ??
        ''
    ).trim() || undefined
  )
}

const toProductionMaterialRow = (value: unknown): ProductionReportCorrectionMaterialRow | undefined => {
  if (!isRecord(value)) {
    return undefined
  }
  const materialId = Number(value.materialId)
  const outputQuantity = optionalCorrectionNumber(value.outputQuantity)
  const lossQuantity = optionalCorrectionNumber(value.lossQuantity) ?? 0
  if (!Number.isFinite(materialId) || materialId <= 0 || outputQuantity === undefined) {
    return undefined
  }
  return {
    materialId,
    materialCode: String(value.materialCode ?? '').trim() || undefined,
    materialName: resolveSubmittedMaterialName(value),
    outputQuantity,
    lossQuantity,
    lossDetails: normalizeSubmissionArray(value.lossDetails)
      .filter((detail): detail is Record<string, unknown> => isRecord(detail))
      .map((detail) => ({
        reasonId: Number(detail.reasonId),
        reasonCode: String(detail.reasonCode ?? '').trim() || undefined,
        reasonName:
          String(detail.reasonName ?? detail.reasonCode ?? '').trim() ||
          `损耗原因 ${detail.reasonId}`,
        quantity: optionalCorrectionNumber(detail.quantity) ?? 0
      }))
      .filter((detail) => Number.isFinite(detail.reasonId) && detail.reasonId > 0),
    selectedDevice: isRecord(value.selectedDevice)
      ? {
          deviceId: Number(value.selectedDevice.deviceId),
          deviceCode: String(value.selectedDevice.deviceCode ?? '').trim() || undefined,
          deviceName: String(value.selectedDevice.deviceName ?? '').trim() || undefined
        }
      : undefined,
    selectedDevices: normalizeSubmissionArray(value.selectedDevices)
      .filter((device): device is Record<string, unknown> => isRecord(device))
      .map((device) => ({
        deviceId: Number(device.deviceId),
        deviceCode: String(device.deviceCode ?? '').trim() || undefined,
        deviceName: String(device.deviceName ?? '').trim() || undefined
      }))
      .filter((device) => Number.isFinite(device.deviceId) && device.deviceId > 0),
    deviceParameterReadings: normalizeSubmissionArray(value.deviceParameterReadings)
      .map(toCorrectionParameterRow)
      .filter((reading): reading is ProductionReportCorrectionParameterRow => Boolean(reading))
  }
}

const mergeSubmittedMaterialIdentity = (
  item: ProductionReportCorrectionMaterialRow,
  snapshot?: ProductionReportCorrectionMaterialRow
): ProductionReportCorrectionMaterialRow => {
  if (!snapshot) {
    return item
  }
  return {
    ...item,
    materialCode: item.materialCode || snapshot.materialCode,
    materialName:
      item.materialName && !isPlaceholderMaterialName(item.materialName)
        ? item.materialName
        : snapshot.materialName
  }
}

const resolveProductionMaterialRows = (row: ProcessPoolTimelineEventVO) => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  const rootPayloadMaterialRows = normalizeSubmissionArray(rootPayload?.materialDetails)
    .map(toProductionMaterialRow)
    .filter((item): item is ProductionReportCorrectionMaterialRow => Boolean(item))
  const rootPayloadMaterialById = new Map(
    rootPayloadMaterialRows.map((item) => [item.materialId, item])
  )
  const source =
    row.materialDetails?.length
      ? row.materialDetails
      : normalizeSubmissionArray(rootPayload?.materialDetails)
  const materialRows: ProductionReportCorrectionMaterialRow[] = source
    .map(toProductionMaterialRow)
    .filter((item): item is ProductionReportCorrectionMaterialRow => Boolean(item))
    .map((item) => mergeSubmittedMaterialIdentity(item, rootPayloadMaterialById.get(item.materialId)))
    .map((item) => ({
      materialId: item.materialId,
      materialCode: item.materialCode,
      materialName: item.materialName,
      outputQuantity: item.outputQuantity,
      lossQuantity: item.lossQuantity,
      lossDetails: item.lossDetails,
      selectedDevice: item.selectedDevice,
      selectedDevices: item.selectedDevices,
      deviceParameterReadings: item.deviceParameterReadings
    }))
  return materialRows
}

const resolveSubmissionMaterialTitle = (item: ProductionReportCorrectionMaterialRow) =>
  formatSubmissionText(item.materialName, '物料名称未记录')

const resolveCorrectionMaterialNames = async (
  materialRows: ProductionReportCorrectionMaterialRow[]
) => {
  return await Promise.all(
    materialRows.map(async (item) => {
      const hasRealName = item.materialName && !/^物料\s*\d+$/.test(item.materialName)
      if (hasRealName) {
        return item
      }
      const material = await MdItemApi.getItem(item.materialId)
      const materialName = String(material?.name || '').trim()
      if (!materialName) {
        throw new Error(`物料 ${item.materialId} 缺少真实物料名称，不能修改`)
      }
      return { ...item, materialName }
    })
  )
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

const normalizeSubmissionArray = (value: unknown) => (Array.isArray(value) ? value : [])

const resolveProductionClearanceConfirmationText = (
  row: ProcessPoolTimelineEventVO,
  key: ProductionClearanceConfirmationColumnKey
) => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  const confirmation = normalizeSubmissionArray(rootPayload?.clearanceConfirmations).find(
    (item) => isRecord(item) && item.key === key
  )
  if (!isRecord(confirmation)) {
    return '--'
  }
  if (confirmation.confirmed === true) {
    return '已选'
  }
  if (confirmation.confirmed === false) {
    return '未选'
  }
  return '--'
}

const resolveSubmissionLossBreakdownItems = (
  row: ProcessPoolTimelineEventVO
): SubmissionStructuredItem[] => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  const lossQuantity = resolveSubmissionLossQuantityValue(row)
  if (isPqcSubmissionRow(row)) {
    return [
      {
        key: 'pqc-loss',
        label: '不良/损耗',
        valueText: formatSubmissionQuantity(lossQuantity)
      }
    ]
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
  return [
    {
      key: 'production-loss',
      label: formatSubmissionText(reasonName, '损耗原因'),
      valueText: formatSubmissionQuantity(lossQuantity)
    }
  ]
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
  const { rootPayload } = resolvePqcPayloadPair(row)
  const parameterDeviceMap = new Map<string, SubmissionStructuredItem>()
  const parameterReadings = row.deviceParameterReadings?.length
    ? row.deviceParameterReadings
    : normalizeSubmissionArray(rootPayload?.deviceParameterReadings)
  parameterReadings.forEach((reading, index) => {
    if (!isRecord(reading)) {
      return
    }
    const deviceText = formatSubmissionText(reading.deviceName || reading.deviceCode, '')
    if (!deviceText) {
      return
    }
    const key = String(reading.deviceId || reading.deviceCode || `${deviceText}-${index}`)
    if (!parameterDeviceMap.has(key)) {
      parameterDeviceMap.set(key, {
        key,
        label: '设备',
        valueText: deviceText
      })
    }
  })
  if (parameterDeviceMap.size) {
    return [...parameterDeviceMap.values()]
  }
  if (row.selectedDevice) {
    const deviceText = [
      row.selectedDevice.deviceName || row.selectedDevice.deviceCode,
      row.selectedDevice.deviceId ? `#${row.selectedDevice.deviceId}` : ''
    ]
      .filter(Boolean)
      .join(' / ')
    return [
      {
        key: String(
          row.selectedDevice.deviceId || row.selectedDevice.deviceCode || 'selected-device'
        ),
        label: '选用设备',
        valueText: deviceText || '--'
      }
    ]
  }
  const rawSelectedDevice = isRecord(rootPayload?.selectedDevice)
    ? rootPayload.selectedDevice
    : undefined
  if (rawSelectedDevice) {
    const deviceText = [
      rawSelectedDevice.deviceName || rawSelectedDevice.deviceCode,
      rawSelectedDevice.deviceId ? `#${rawSelectedDevice.deviceId}` : ''
    ]
      .filter(Boolean)
      .join(' / ')
    return [
      {
        key: String(
          rawSelectedDevice.deviceId || rawSelectedDevice.deviceCode || 'selected-device'
        ),
        label: '选用设备',
        valueText: deviceText || '--'
      }
    ]
  }
  const equipmentParameters = readSubmissionNestedRecord(rootPayload, 'equipmentParameters')
  const deviceText = readSubmissionPayloadValue(rootPayload, ['DEVICE'])
  const deviceLabels = equipmentParameters
    ? Object.keys(equipmentParameters)
    : String(deviceText || '')
        .split('、')
        .map((item) => item.trim())
        .filter(Boolean)
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

const isPqcSampleOutOfRange = (detail: PqcItemSnapshotDetail) => {
  const lower = detail.standardLowerLimit
  const upper = detail.standardUpperLimit
  return (detail.sampleValues || []).some((value) => isValueOutOfRange(value, lower, upper))
}

const formatParameterRangeText = (lower?: unknown, upper?: unknown, unit = '') => {
  if (
    (lower === undefined || lower === null || lower === '') &&
    (upper === undefined || upper === null || upper === '')
  ) {
    return ''
  }
  return `范围 ${lower ?? '--'} ~ ${upper ?? '--'}${unit}`
}

const formatSubmissionParameterValueText = (item: PqcSubmissionPayloadRecord) =>
  formatSubmissionText(item.textValue ?? item.value)

const formatSubmissionDeviceMeteringValidityText = (value?: boolean) => {
  if (value === true) {
    return '计量有效'
  }
  if (value === false) {
    return '计量超期'
  }
  return '计量状态未记录'
}

const resolveSubmissionDeviceInlineMeteringValidity = (
  device: ProcessPoolTimelineSelectedDeviceVO | ProductionReportCorrectionParameterRow
) => ('inMeteringValidityPeriod' in device ? device.inMeteringValidityPeriod : undefined)

const resolveSubmissionDeviceMeteringValidityMap = (
  row: ProcessPoolTimelineEventVO
): Map<string, boolean> => {
  const { rootPayload } = resolvePqcPayloadPair(row)
  const rows = normalizeSubmissionArray(rootPayload?.deviceMeteringValidity)
  const result = new Map<string, boolean>()
  rows.forEach((item) => {
    if (!isRecord(item) || typeof item.inMeteringValidityPeriod !== 'boolean') {
      return
    }
    const keys = [item.deviceId, item.deviceCode]
      .map((value) => String(value ?? '').trim())
      .filter(Boolean)
    keys.forEach((key) => result.set(key, item.inMeteringValidityPeriod as boolean))
  })
  return result
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
          ]
            .filter(Boolean)
            .join(' · '),
          valueText: `${formatSubmissionParameterValueText(item)}${unit}`,
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
    const outOfRange = isPqcSampleOutOfRange(detail)
    return {
      key: detail.itemCode || `${detail.itemName || 'pqc-parameter'}-${detailIndex}`,
      label: detail.itemName || detail.itemCode || '检验项目',
      valueText: formatPqcSnapshotSampleValues(detail),
      metaText: [
        `标准：${formatPqcSnapshotStandard(detail)}`,
        equipmentText ? `设备：${equipmentText}` : '',
        detail.selectedEquipmentNumber ? `设备编号：${detail.selectedEquipmentNumber}` : '',
        detail.inspectionMethod ? `方法：${detail.inspectionMethod}` : '',
        judgementText ? `判定：${judgementText}` : ''
      ]
        .filter(Boolean)
        .join('；'),
      outOfRange,
      parameterStatus: outOfRange ? 'ABNORMAL' : 'NORMAL'
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

const isMeaningfulSubmissionText = (value?: string) => {
  const text = String(value ?? '').trim()
  return Boolean(text) && text !== '--'
}

const formatSubmissionDeviceParameterText = (items: SubmissionStructuredItem[]) => {
  const text = items
    .filter((item) => isMeaningfulSubmissionText(item.valueText))
    .map((item) => {
      const statusText = item.outOfRange ? '异常' : ''
      const metaText = [statusText, item.metaText].filter(Boolean).join('，')
      return `${item.label}：${item.valueText}${metaText ? `（${metaText}）` : ''}`
    })
    .join('；')
  return text || '无参数'
}

const toSubmissionMaterialDeviceRow = (
  device: ProcessPoolTimelineSelectedDeviceVO | ProductionReportCorrectionParameterRow,
  key: string,
  parameterItems: SubmissionStructuredItem[],
  inMeteringValidityPeriod?: boolean
): SubmissionMaterialDeviceRow => ({
  key,
  deviceNameText: formatSubmissionText(device.deviceName, '未命名设备'),
  deviceCodeText: formatSubmissionText(
    device.deviceCode || (device.deviceId ? `#${device.deviceId}` : '')
  ),
  meteringValidityText: formatSubmissionDeviceMeteringValidityText(
    inMeteringValidityPeriod ?? resolveSubmissionDeviceInlineMeteringValidity(device)
  ),
  parameterText: formatSubmissionDeviceParameterText(parameterItems),
  hasAbnormalParameter: parameterItems.some((item) => item.outOfRange)
})

const resolveSubmissionMaterialDeviceRows = (
  materialRow: ProductionReportCorrectionMaterialRow,
  materialIndex: number,
  meteringValidityMap: Map<string, boolean>
): SubmissionMaterialDeviceRow[] => {
  const deviceMap = new Map<
    string,
    ProcessPoolTimelineSelectedDeviceVO | ProductionReportCorrectionParameterRow
  >()
  const parameterMap = new Map<string, SubmissionStructuredItem[]>()
  const resolveDeviceKey = (
    device: ProcessPoolTimelineSelectedDeviceVO | ProductionReportCorrectionParameterRow,
    deviceIndex: number
  ) => String(device.deviceId || device.deviceCode || `${materialIndex}-${deviceIndex}`)
  const ensureDevice = (
    device: ProcessPoolTimelineSelectedDeviceVO | ProductionReportCorrectionParameterRow,
    deviceIndex: number
  ) => {
    const key = resolveDeviceKey(device, deviceIndex)
    if (!deviceMap.has(key)) {
      deviceMap.set(key, device)
    }
    return key
  }
  const selectedDevices = [
    ...(materialRow.selectedDevices || []),
    ...(materialRow.selectedDevice ? [materialRow.selectedDevice] : [])
  ]
  selectedDevices
    .filter((device) => device.deviceId || device.deviceCode || device.deviceName)
    .forEach((device, deviceIndex) => ensureDevice(device, deviceIndex))
  materialRow.deviceParameterReadings.forEach((reading, parameterIndex) => {
    const deviceKey = ensureDevice(reading, parameterIndex)
    const parameterItem = resolveProductionParameterItems(undefined, [reading])[0]
    if (!parameterItem || !isMeaningfulSubmissionText(parameterItem.valueText)) {
      return
    }
    const parameterItems = parameterMap.get(deviceKey) || []
    parameterItems.push({
      ...parameterItem,
      key: `${deviceKey}-${parameterItem.key}-${parameterIndex}`,
      label: formatSubmissionText(reading.parameterName || reading.parameterCode, '参数')
    })
    parameterMap.set(deviceKey, parameterItems)
  })
  return [...deviceMap.entries()].map(([key, device]) => {
    const inMeteringValidityPeriod =
      meteringValidityMap.get(String(device.deviceId || '')) ??
      meteringValidityMap.get(String(device.deviceCode || ''))
    return toSubmissionMaterialDeviceRow(
      device,
      key,
      parameterMap.get(key) || [],
      inMeteringValidityPeriod
    )
  })
}

const resolveSubmissionMaterialDetailRows = (
  row: ProcessPoolTimelineEventVO
): SubmissionMaterialDetailRow[] => {
  const materialRows = resolveProductionMaterialRows(row)
  const meteringValidityMap = resolveSubmissionDeviceMeteringValidityMap(row)
  if (!materialRows.length) {
    const parameterItems = resolveSubmissionParameterItems(row).filter((item) =>
      isMeaningfulSubmissionText(item.valueText)
    )
    return [
      {
        key: `submission-${row.id}`,
        materialText: formatSubmissionText(row.productName || row.productCode, '提交物料'),
        outputQuantityText: resolveSubmissionCompletionQuantity(row),
        lossQuantityText: resolveSubmissionLossQuantity(row),
        lossDetailText: resolveSubmissionLossBreakdownItems(row)
          .map((item) => `${item.label}：${item.valueText}`)
          .join('；'),
        devices: resolveSubmissionEquipmentItems(row)
          .filter((item) => isMeaningfulSubmissionText(item.valueText))
          .map((item) =>
            toSubmissionMaterialDeviceRow(
              { deviceName: item.valueText },
              item.key,
              parameterItems,
              meteringValidityMap.get(item.key)
            )
          )
      }
    ]
  }
  return materialRows.map((item, index) => {
    return {
      key: String(item.materialId || index),
      materialText: resolveSubmissionMaterialTitle(item),
      outputQuantityText: formatSubmissionQuantity(item.outputQuantity),
      lossQuantityText: formatSubmissionQuantity(item.lossQuantity),
      lossDetailText:
        item.lossDetails
          .map((detail) => `${detail.reasonName}：${formatSubmissionQuantity(detail.quantity)}`)
          .join('；') || '--',
      devices: resolveSubmissionMaterialDeviceRows(item, index, meteringValidityMap)
    }
  })
}

const resolveSubmissionMaterialSummaryItems = (
  row: ProcessPoolTimelineEventVO
): SubmissionStructuredItem[] => {
  const materialRows = resolveProductionMaterialRows(row)
  const materialCount = materialRows.length || 1
  const lossReasonCount = resolveSubmissionLossBreakdownItems(row).filter((item) =>
    isMeaningfulSubmissionText(item.valueText)
  ).length
  return [
    {
      key: 'material-count',
      label: '物料',
      valueText: `${materialCount} 个物料`
    },
    {
      key: 'loss-summary',
      label: '损耗',
      valueText: `损耗 ${resolveSubmissionLossQuantity(row)}${lossReasonCount ? ` / ${lossReasonCount} 类` : ''}`
    }
  ]
}

const resolveSubmissionDeviceSummaryItems = (
  row: ProcessPoolTimelineEventVO
): SubmissionStructuredItem[] => {
  const devices = resolveSubmissionEquipmentItems(row)
    .map((item) => item.valueText)
    .filter(isMeaningfulSubmissionText)
  if (!devices.length) {
    return [{ key: 'empty-device-summary', label: '设备', valueText: '--' }]
  }
  const uniqueDevices = [...new Set(devices)]
  return [
    {
      key: 'device-summary',
      label: '设备',
      valueText:
        uniqueDevices.length === 1
          ? uniqueDevices[0]
          : `${uniqueDevices.length} 台：${uniqueDevices.slice(0, 2).join('、')}${
              uniqueDevices.length > 2 ? '等' : ''
            }`
    }
  ]
}

const resolveSubmissionParameterSummaryItems = (
  row: ProcessPoolTimelineEventVO
): SubmissionStructuredItem[] => {
  const parameters = resolveSubmissionParameterItems(row).filter((item) =>
    isMeaningfulSubmissionText(item.valueText)
  )
  if (!parameters.length) {
    return [{ key: 'empty-parameter-summary', label: '参数', valueText: '--' }]
  }
  const abnormalCount = parameters.filter((item) => item.outOfRange).length
  return [
    {
      key: 'parameter-summary',
      label: '参数',
      valueText: `${parameters.length} 项${abnormalCount ? ` / 异常 ${abnormalCount} 项` : ''}`
    }
  ]
}

const reviewMaterialRows = computed(() =>
  reviewEvent.value ? resolveProductionMaterialRows(reviewEvent.value) : []
)
const reviewDeviceItems = computed(() =>
  reviewEvent.value ? resolveSubmissionEquipmentItems(reviewEvent.value) : []
)
const reviewParameterItems = computed(() =>
  reviewEvent.value ? resolveSubmissionParameterItems(reviewEvent.value) : []
)

const formatPqcSnapshotSampleValues = (detail: PqcItemSnapshotDetail) =>
  detail.sampleValues?.length ? detail.sampleValues.join('、') : '未填写'

const formatPqcSnapshotStandard = (detail: PqcItemSnapshotDetail) => {
  const lower = detail.standardLowerLimit
  const upper = detail.standardUpperLimit
  const unit = detail.standardUnit || ''
  const range =
    lower !== undefined || upper !== undefined ? `${lower ?? '--'} ~ ${upper ?? '--'}${unit}` : ''
  return [detail.standardText, range].filter(Boolean).join('；') || '未配置'
}

const resolvePqcInspectionTypeText = (value: unknown) => {
  if (value === 'FIRST') return '首检'
  if (value === 'PATROL') return '巡检'
  if (value === 'FINAL') return '末检'
  return String(value ?? '').trim()
}

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

const buildSubmissionParams = (): TeamLeaderSubmissionPageReqVO => {
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    leaderType: resolveCurrentLeaderType(),
    submitDate:
      typeof queryParams.submitDate === 'string'
        ? queryParams.submitDate.trim() || undefined
        : undefined,
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
    submissionReviewStatus: isPqcFormHistoryTab.value
      ? 'APPROVED'
      : queryParams.submissionReviewStatus || undefined,
    pqcFormView: isPqcFormHistoryTab.value
      ? 'HISTORY'
      : activeLeaderTab.value === 'PQC'
        ? 'CURRENT'
        : undefined,
    allocationView: isProductionLeader.value
      ? isProductionReportHistoryTab.value
        ? 'HISTORY'
        : 'WORKBENCH'
      : undefined
  }
}

async function getSubmissionList() {
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
  if (isPqcFormHistoryTab.value) {
    queryParams.submissionReviewStatus = 'APPROVED'
    return
  }
  if (isProductionReportHistoryTab.value) {
    queryParams.submissionReviewStatus = undefined
    return
  }
  queryParams.submissionReviewStatus = undefined
}

const resetSubmissionQueryParams = (leaderType: TeamLeaderType) => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.leaderType = leaderType
  queryParams.submitDate = getInitialSubmissionDate(leaderType)
}

const clearSubmissionVisibleFilterState = () => {
  updateSubmissionMultiFilterState({
    conditions: [],
    appliedConditions: [],
    activeConditionId: undefined
  })
}

const isInitialDefaultSubmitDateCondition = (condition: {
  id?: string
  key?: string
  operator?: string
  value?: unknown
}) => {
  const conditionIdentity = condition.id || condition.key
  const conditionValue = typeof condition.value === 'string' ? condition.value.trim() : ''
  return (
    conditionIdentity === 'submitDate' &&
    condition.key === 'submitDate' &&
    (condition.operator || 'eq') === 'eq' &&
    conditionValue === (queryParams.submitDate || getDefaultSubmissionDate())
  )
}

const clearInitialSubmissionVisibleDefaultFilter = () => {
  if (submissionMultiFilterState.appliedConditions.length > 0) return
  const isDefaultSubmitDateOnly =
    submissionMultiFilterState.conditions.length === 1 &&
    isInitialDefaultSubmitDateCondition(submissionMultiFilterState.conditions[0])
  if (!isDefaultSubmitDateOnly) return
  clearSubmissionVisibleFilterState()
}

const applySubmissionMultiFilter = async () => {
  await applySubmissionMultiFilterState()
}

const resetSubmissionMultiFilter = async () => {
  const leaderType = activeLeaderTab.value
  clearSubmissionVisibleFilterState()
  clearSubmissionMultiFilterParams()
  clearSubmissionFilterParams()
  resetSubmissionQueryParams(leaderType)
  submissionList.value = []
  submissionTotal.value = 0
  loadError.value = ''
  await getSubmissionList()
}

watch(activePqcModuleTab, async (tab) => {
  if ((tab === 'management' || tab === 'history') && activeLeaderTab.value === 'PQC') {
    queryParams.leaderType = 'PQC'
    queryParams.pageNo = 1
    queryParams.submissionReviewStatus = tab === 'history' ? 'APPROVED' : undefined
    clearInitialSubmissionVisibleDefaultFilter()
    await getSubmissionList()
  }
})

watch(activeProductionModuleTab, async (tab) => {
  if ((tab === 'report' || tab === 'reportHistory') && activeLeaderTab.value === 'PRODUCTION') {
    queryParams.leaderType = 'PRODUCTION'
    queryParams.pageNo = 1
    queryParams.submissionReviewStatus = undefined
    clearInitialSubmissionVisibleDefaultFilter()
    await getSubmissionList()
  }
  if (tab === 'personnel' && activeLeaderTab.value === 'PRODUCTION') {
    await refreshProductionPersonnel()
    await loadResponsibleRoutes().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '负责工艺路线加载失败'))
    })
  }
  if (tab === 'activeOrder' && activeLeaderTab.value === 'PRODUCTION') {
    await loadResponsibleRoutes().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '负责工艺路线加载失败'))
    })
    await loadActiveOrders().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '活跃订单加载失败'))
    })
  }
  if (tab === 'processConfig' && activeLeaderTab.value === 'PRODUCTION') {
    await loadResponsibleRoutes().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '负责工艺路线加载失败'))
    })
    await loadTeamDeviceOptions().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '班组设备列表加载失败'))
    })
    await loadProcessConfigRows().catch((error) => {
      ElMessage.error(resolveErrorMessage(error, '工序配置列表加载失败'))
    })
  }
})

const loadLegacyProductionWorkbenchData = () => {
  refreshProductionPersonnel()
  loadResponsibleRoutes().catch((error) => {
    ElMessage.error(resolveErrorMessage(error, '负责工艺路线加载失败'))
  })
  loadTeamDeviceOptions().catch((error) => {
    ElMessage.error(resolveErrorMessage(error, '班组设备列表加载失败'))
  })
  loadActiveOrders().catch((error) => {
    ElMessage.error(resolveErrorMessage(error, '活跃订单加载失败'))
  })
  loadProcessConfigRows().catch((error) => {
    ElMessage.error(resolveErrorMessage(error, '工序配置列表加载失败'))
  })
}

const handleLeaderTypeChange = async (value: string | number) => {
  const selectedTab = String(value) as WorkbenchLeaderTab
  const leaderType = selectedTab as TeamLeaderType
  activeLeaderTab.value = leaderType
  if (leaderType === 'PRODUCTION') {
    if (!showProductionModuleTabs.value) {
      loadLegacyProductionWorkbenchData()
    }
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
    detail.value = await getTeamLeaderSubmissionDetail(eventId, resolveCurrentLeaderType())
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
  reviewDialogMode.value = 'REVIEW'
  reviewEvent.value = event
  reviewForm.reviewStatus = 'APPROVED'
  resetReviewAllocation()
  reviewForm.reviewRemark = ''
  reviewForm.reviewSignaturePassword = ''
  reviewVisible.value = true
  if (isProductionLeader.value) {
    try {
      await loadActiveOrders()
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, '活跃订单加载失败'))
    }
  }
}

const openPqcSubmissionNonconformanceReview = (row: ProcessPoolTimelineEventVO) => {
  requirePositiveNumber(row.id, '工序池提交事件编号不能为空')
  if (!canOpenPqcSubmissionNonconformanceReview(row)) {
    ElMessage.error('当前PQC提交记录不能发起不合格审查')
    return
  }
  const query: Record<string, string> = {
    sourceType: SOURCE_TYPE_PQC_SUBMISSION,
    sourceId: String(row.id)
  }
  router.push({
    name: 'MesProFeedbackEdhrNonconformanceReview',
    query
  })
}

const openProductionReject = (event: ProcessPoolTimelineEventVO) => {
  requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
  if (!canRejectProductionSubmission(event)) {
    ElMessage.error('已放行或已处理的生产报工不能驳回')
    return
  }
  reviewDialogMode.value = 'REJECTION'
  reviewEvent.value = event
  reviewForm.reviewStatus = 'REJECTED'
  reviewForm.reviewRemark = ''
  reviewForm.reviewSignaturePassword = ''
  resetReviewAllocation()
  reviewVisible.value = true
}

const openAllocation = async (event: ProcessPoolTimelineEventVO) => {
  requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
  if (!canAllocateSubmission(event)) {
    ElMessage.error('当前报工不在可分配列表中')
    return
  }
  reviewDialogMode.value = 'ALLOCATION'
  reviewEvent.value = event
  reviewForm.reviewStatus = 'APPROVED'
  resetReviewAllocation()
  reviewForm.reviewRemark = ''
  reviewForm.reviewSignaturePassword = ''
  reviewVisible.value = true
  try {
    const [snapshot] = await Promise.all([
      getCurrentTeamLeaderReportAllocation(
        requirePositiveNumber(event.id, '工序池提交事件编号不能为空'),
        resolveCurrentLeaderType()
      ),
      loadActiveOrders()
    ])
    applyAllocationSnapshot(snapshot)
    prefillSelectedOrderAllocation(event, snapshot)
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '报工分配加载失败'))
  }
}

const submitReview = async () => {
  const eventId = requirePositiveNumber(reviewEvent.value?.id, '工序池提交事件编号不能为空')
  if (reviewForm.reviewStatus === 'REJECTED' && !reviewForm.reviewRemark.trim()) {
    ElMessage.error('退回复核必须填写复核说明')
    return
  }
  reviewSubmitting.value = true
  let writeCompleted = false
  try {
    const leaderType = resolveCurrentLeaderType()
    const reviewRemark = reviewForm.reviewRemark.trim() || undefined
    if (reviewDialogMode.value === 'REJECTION') {
      const reviewSignaturePayload = buildReviewSignaturePayload()
      await reviewTeamLeaderSubmission({
        leaderType: 'PRODUCTION',
        eventId,
        reviewStatus: 'REJECTED',
        reviewRemark,
        ...reviewSignaturePayload
      })
    } else if (isProductionLeader.value && reviewForm.reviewStatus === 'APPROVED') {
      if (reviewDialogMode.value === 'ALLOCATION') {
        const allocations = buildAllocationSubmitLines()
        const snapshot = await confirmTeamLeaderReportAllocation({
          eventId,
          leaderType,
          allocationMode: reviewForm.allocationMode,
          expectedVersion: allocationSnapshot.value?.version,
          idempotencyKey: getOrCreateAllocationSaveIdempotencyKey({
            eventId,
            leaderType,
            allocationMode: reviewForm.allocationMode,
            expectedVersion: allocationSnapshot.value?.version,
            reviewRemark,
            allocations
          }),
          reviewRemark,
          allocations
        })
        applyAllocationSnapshot(snapshot)
      } else {
        const reviewSignaturePayload = buildReviewSignaturePayload()
        await confirmTeamLeaderReportAllocation({
          eventId,
          leaderType,
          allocationMode: reviewForm.allocationMode,
          reviewRemark,
          ...reviewSignaturePayload,
          allocations: buildAllocationSubmitLines()
        })
      }
    } else {
      const reviewSignaturePayload = buildReviewSignaturePayload()
      await reviewTeamLeaderSubmission({
        leaderType,
        eventId,
        reviewStatus: reviewForm.reviewStatus,
        reviewRemark,
        ...reviewSignaturePayload
      })
    }
    writeCompleted = true
    ElMessage.success(
      reviewDialogMode.value === 'ALLOCATION'
        ? '分配已保存'
        : reviewDialogMode.value === 'REJECTION'
          ? '生产报工已驳回'
          : '复核已提交'
    )
    reviewVisible.value = false
    if (
      isProductionLeader.value &&
      (reviewForm.reviewStatus === 'APPROVED' || reviewDialogMode.value === 'REJECTION')
    ) {
      await Promise.all([getSubmissionList(), loadActiveOrders()])
    } else {
      await getSubmissionList()
    }
  } catch (error) {
    if (reviewDialogMode.value === 'ALLOCATION' && isReportAllocationVersionConflict(error)) {
      try {
        const latest = await getCurrentTeamLeaderReportAllocation(
          eventId,
          resolveCurrentLeaderType()
        )
        applyAllocationSnapshot(latest)
        ElMessage.error('分配版本已更新，已加载最新分配，请确认后重新保存')
      } catch (refreshError) {
        ElMessage.error(resolveErrorMessage(refreshError, '分配版本冲突，最新分配加载失败'))
      }
      return
    }
    ElMessage.error(
      resolveErrorMessage(
        error,
        writeCompleted
          ? reviewDialogMode.value === 'ALLOCATION'
            ? '分配已保存，但列表刷新失败'
            : reviewDialogMode.value === 'REJECTION'
              ? '生产报工已驳回，但列表刷新失败'
              : '复核已提交，但列表刷新失败'
          : reviewDialogMode.value === 'REJECTION'
            ? '生产报工驳回失败'
            : '复核提交失败'
      )
    )
  } finally {
    reviewSubmitting.value = false
  }
}

const resetCorrectionFormForEvent = (
  event: ProcessPoolTimelineEventVO,
  eventId: number,
  correctionMode: 'PRODUCTION' | 'PQC'
) => {
  correctionForm.correctionMode = correctionMode
  correctionForm.eventId = eventId
  correctionForm.outputQuantity = undefined
  correctionForm.materialDetails = []
  correctionForm.lossDetails = []
  correctionForm.deviceParameterReadings = []
  correctionForm.pqcActualInspectionQuantity = undefined
  correctionForm.pqcScrapQuantity = 0
  correctionForm.pqcItemResults = []
  correctionForm.changeReason = ''
  correctionForm.signaturePassword = ''
  initializeCorrectionMaterialDeviceTabs([])
  correctionEvent.value = event
}

const openProductionCorrection = async (event: ProcessPoolTimelineEventVO, eventId: number) => {
  const outputQuantity = Number(event.outputQuantity)
  if (!Number.isFinite(outputQuantity) || outputQuantity <= 0) {
    throw new Error('报工记录缺少有效的完成数量，不能修改')
  }
  const routeProcessId = requirePositiveNumber(event.routeProcessId, '报工记录缺少路线工序快照')
  const configuredReasons =
    processConfigRows.value.find((row) => row.routeProcessId === routeProcessId)?.lossReasons || []
  const lossRowMap = new Map<number, ProductionReportCorrectionLossDetailRow>()
  ;(event.lossDetails || []).forEach((item) => {
    const reasonId = requirePositiveNumber(item.reasonId, '报工损耗明细缺少原因编号')
    const quantity = Number(item.quantity)
    if (!Number.isFinite(quantity) || quantity < 0) {
      throw new Error('报工损耗明细数量无效，不能修改')
    }
    lossRowMap.set(reasonId, {
      reasonId,
      reasonCode: item.reasonCode,
      reasonName: item.reasonName || item.reasonCode || `损耗原因 ${reasonId}`,
      quantity
    })
  })
  configuredReasons
    .filter((item) => item.enabled)
    .forEach((item) => {
      if (!lossRowMap.has(item.id)) {
        lossRowMap.set(item.id, {
          reasonId: item.id,
          reasonCode: item.reasonCode,
          reasonName: item.reasonName,
          quantity: 0
        })
      }
    })

  const parameterRows = (event.deviceParameterReadings || [])
    .map(toCorrectionParameterRow)
    .filter((item): item is ProductionReportCorrectionParameterRow => Boolean(item))

  resetCorrectionFormForEvent(event, eventId, 'PRODUCTION')
  correctionForm.outputQuantity = outputQuantity
  correctionForm.materialDetails = enrichCorrectionMaterialParameterRows(
    event,
    await resolveCorrectionMaterialNames(resolveProductionMaterialRows(event))
  )
  initializeCorrectionMaterialDeviceTabs(correctionForm.materialDetails)
  correctionForm.lossDetails = [...lossRowMap.values()]
  correctionForm.deviceParameterReadings = parameterRows
  correctionVisible.value = true
}

const openPqcCorrection = (event: ProcessPoolTimelineEventVO, eventId: number) => {
  const { rootPayload } = resolvePqcPayloadPair(event)
  const actualInspectionQuantity = Number(
    readSubmissionPayloadValue(rootPayload, ['inspectionQuantity', 'actualInspectionQuantity'])
  )
  if (!Number.isInteger(actualInspectionQuantity) || actualInspectionQuantity <= 0) {
    throw new Error('PQC表单缺少有效的检验数量，不能修改')
  }
  const scrapQuantityValue = readSubmissionPayloadValue(rootPayload, [
    'scrapQuantity',
    'lossQuantity',
    'SCRAP_QUANTITY'
  ])
  const scrapQuantity = scrapQuantityValue === undefined ? 0 : Number(scrapQuantityValue)
  if (!Number.isInteger(scrapQuantity) || scrapQuantity < 0) {
    throw new Error('PQC表单损耗数量无效，不能修改')
  }
  const pqcItems = resolvePqcItemSnapshotDetails(event)
  if (!pqcItems.length) {
    throw new Error('PQC表单缺少检验项目明细，不能修改')
  }

  resetCorrectionFormForEvent(event, eventId, 'PQC')
  correctionForm.pqcActualInspectionQuantity = actualInspectionQuantity
  correctionForm.pqcScrapQuantity = scrapQuantity
  correctionForm.pqcItemResults = pqcItems.map((item) => {
    const itemCode = String(item.itemCode || '').trim()
    if (!itemCode) {
      throw new Error('PQC表单检验项目缺少项目编码，不能修改')
    }
    const sampleValues = item.sampleValues || []
    if (!sampleValues.length) {
      throw new Error(`PQC项目 ${item.itemName || itemCode} 缺少样本值，不能修改`)
    }
    return {
      itemCode,
      itemName: item.itemName,
      selectedEquipmentId: item.selectedEquipmentId,
      selectedEquipmentNumber: item.selectedEquipmentNumber,
      sampleValuesText: normalizePqcSampleText(sampleValues)
    }
  })
  correctionVisible.value = true
}

const openCorrection = async (event: ProcessPoolTimelineEventVO) => {
  try {
    const eventId = requirePositiveNumber(event.id, '工序池提交事件编号不能为空')
    if (isPqcSubmissionRow(event) && event.released) {
      ElMessage.error('已放行的PQC表单只能在历史中查看')
      return
    }
    if (!canCorrectSubmission(event)) {
      ElMessage.error(isPqcSubmissionRow(event) ? 'PQC历史表单不能修改' : '当前报工不能修改')
      return
    }
    if (isPqcSubmissionRow(event)) {
      openPqcCorrection(event, eventId)
      return
    }
    await openProductionCorrection(event, eventId)
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '修改入口打开失败'))
  }
}

const requireCorrectionAuditFields = () => {
  const changeReason = correctionForm.changeReason.trim()
  if (!changeReason) {
    throw new Error('修改原因不能为空')
  }
  if (!correctionForm.signaturePassword) {
    throw new Error('签名密码不能为空')
  }
  if (correctionChangePreview.value.length === 0) {
    throw new Error('没有可提交的变更')
  }
  return {
    changeReason,
    signaturePassword: correctionForm.signaturePassword
  }
}

const buildProductionCorrectionRequest = () => {
  const outputQuantity = Number(correctionForm.outputQuantity)
  if (!Number.isFinite(outputQuantity) || outputQuantity <= 0) {
    throw new Error('完成数量必须大于 0')
  }
  const auditFields = requireCorrectionAuditFields()
  const hasMaterialLossDetails = correctionForm.materialDetails.some(
    (item) => item.lossDetails.length > 0
  )
  const hasMaterialParameters = correctionForm.materialDetails.some(
    (item) => item.deviceParameterReadings.length > 0
  )
  const materialLossDetails = hasMaterialLossDetails
    ? correctionForm.materialDetails.flatMap((item) => item.lossDetails)
    : correctionForm.lossDetails
  const materialParameterReadings = hasMaterialParameters
    ? correctionForm.materialDetails.flatMap((item) => item.deviceParameterReadings)
    : correctionForm.deviceParameterReadings
  return {
    eventId: requirePositiveNumber(correctionForm.eventId, '工序池提交事件编号不能为空'),
    outputQuantity,
    materialDetails: correctionForm.materialDetails.map((item) => {
      const materialOutputQuantity = optionalCorrectionNumber(item.outputQuantity)
      const materialLossQuantity = optionalCorrectionNumber(item.lossQuantity)
      if (materialOutputQuantity === undefined || materialOutputQuantity < 0) {
        throw new Error('物料完成数量不能小于 0')
      }
      if (materialLossQuantity === undefined || materialLossQuantity < 0) {
        throw new Error('物料损耗数量不能小于 0')
      }
      return {
        materialId: requirePositiveNumber(item.materialId, '物料不能为空'),
        outputQuantity: materialOutputQuantity,
        lossQuantity: materialLossQuantity,
        lossDetails: item.lossDetails
          .filter((detail) => Number(detail.quantity) > 0)
          .map((detail) => ({
            reasonId: requirePositiveNumber(detail.reasonId, '物料损耗原因不能为空'),
            quantity: Number(detail.quantity)
          })),
        selectedDevice: item.selectedDevice
          ? { deviceId: requirePositiveNumber(item.selectedDevice.deviceId, '物料使用设备不能为空') }
          : undefined,
        selectedDevices: resolveCorrectionMaterialDevices(item).map((device) => ({
          deviceId: requirePositiveNumber(device.deviceId, '物料使用设备不能为空')
        })),
        deviceParameterReadings: item.deviceParameterReadings.flatMap((parameter) => {
          const submittedValue = normalizeCorrectionParameterSubmitValue(parameter)
          if (submittedValue === undefined) return []
          return {
            deviceId: requirePositiveNumber(parameter.deviceId, '物料设备参数所属设备不能为空'),
            parameterCode: parameter.parameterCode,
            ...submittedValue
          }
        })
      }
    }),
    lossDetails: materialLossDetails
      .filter((item) => Number(item.quantity) > 0)
      .map((item) => ({
        reasonId: requirePositiveNumber(item.reasonId, '损耗原因不能为空'),
        quantity: Number(item.quantity)
      })),
    deviceParameterReadings: materialParameterReadings.flatMap((item) => {
      const submittedValue = normalizeCorrectionParameterSubmitValue(item)
      if (submittedValue === undefined) return []
      return {
        deviceId: requirePositiveNumber(item.deviceId, '设备参数所属设备不能为空'),
        parameterCode: item.parameterCode,
        ...submittedValue
      }
    }),
    ...auditFields
  }
}

const buildPqcCorrectionRequest = () => {
  const actualInspectionQuantity = Number(correctionForm.pqcActualInspectionQuantity)
  if (!Number.isInteger(actualInspectionQuantity) || actualInspectionQuantity <= 0) {
    throw new Error('PQC检验数量必须为正整数')
  }
  const scrapQuantity = Number(correctionForm.pqcScrapQuantity)
  if (!Number.isInteger(scrapQuantity) || scrapQuantity < 0) {
    throw new Error('PQC损耗数量必须为0或正整数')
  }
  const auditFields = requireCorrectionAuditFields()
  const itemResults = correctionForm.pqcItemResults.map((item) => {
    const itemCode = item.itemCode.trim()
    if (!itemCode) {
      throw new Error('PQC检验项目编码不能为空')
    }
    const sampleValues = parsePqcSampleText(item.sampleValuesText)
    if (sampleValues.length !== actualInspectionQuantity) {
      throw new Error(`PQC项目 ${item.itemName || itemCode} 的样本数量必须等于检验数量`)
    }
    return {
      itemCode,
      selectedEquipmentId: normalizePositiveNumber(item.selectedEquipmentId),
      selectedEquipmentNumber: item.selectedEquipmentNumber?.trim() || undefined,
      sampleValues
    }
  })
  if (!itemResults.length) {
    throw new Error('PQC检验项目不能为空')
  }
  return {
    eventId: requirePositiveNumber(correctionForm.eventId, '工序池提交事件编号不能为空'),
    actualInspectionQuantity,
    scrapQuantity,
    itemResults,
    ...auditFields
  }
}

const submitCorrection = async () => {
  requirePositiveNumber(correctionEvent.value?.id, '工序池提交事件编号不能为空')
  const valid = await correctionFormRef.value?.validate?.().catch(() => false)
  if (valid === false) return
  correctionSubmitting.value = true
  try {
    const isPqcCorrection = correctionForm.correctionMode === 'PQC'
    if (isPqcCorrection) {
      await correctProcessPoolPqcInspection(buildPqcCorrectionRequest())
    } else {
      await correctProcessPoolProductionReport(buildProductionCorrectionRequest())
    }
    ElMessage.success(
      isPqcCorrection ? 'PQC表单修改已提交，修改日志已记录' : '修改已提交，修改日志已记录'
    )
    correctionVisible.value = false
    await getSubmissionList()
  } catch (error) {
    ElMessage.error(
      resolveErrorMessage(
        error,
        correctionForm.correctionMode === 'PQC' ? 'PQC表单修改失败' : '报工修改失败'
      )
    )
  } finally {
    correctionSubmitting.value = false
  }
}

const resetAbnormalForm = () => {
  abnormalForm.workOrderId = 0
  abnormalForm.abnormalDescription = ''
  abnormalFormRef.value?.clearValidate?.()
}

const openAbnormalDialog = (row: TeamLeaderActiveOrderRespVO) => {
  if (row.abnormal) {
    ElMessage.warning(row.abnormalReason || '该订单已报异常')
    return
  }
  resetAbnormalForm()
  abnormalForm.workOrderId = row.workOrderId
  abnormalDialogVisible.value = true
}

const submitAbnormal = async () => {
  const valid = await abnormalFormRef.value?.validate?.()
  if (valid === false) return
  requirePositiveNumber(abnormalForm.workOrderId, '生产订单ID不能为空')
  abnormalSubmitting.value = true
  try {
    await markAndReportWorkOrderAbnormal({
      workOrderId: abnormalForm.workOrderId,
      abnormalDescription: abnormalForm.abnormalDescription.trim()
    })
    await loadActiveOrders()
    abnormalDialogVisible.value = false
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

const activeOrderSubmitLabel = computed(() => {
  if (activeOrderSelectedCandidate.value?.candidateState === 'REUSABLE') return '确认复用'
  if (activeOrderSelectedCandidate.value?.candidateState === 'RECOVERABLE') return '恢复活跃订单'
  return '加入活跃订单'
})

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
    activeOrderCandidateError.value = '请选择订单号/产品候选'
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
    activeOrderSelectedCandidate.value &&
    activeOrderSelectedCandidate.value.workOrderCode !== searchText
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
    activeOrderCandidateError.value = resolveErrorMessage(error, '订单号/产品候选搜索失败')
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
    activeOrderCandidateError.value = resolveErrorMessage(error, '订单号/产品候选搜索失败')
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
    workOrderId &&
    selectedCandidate &&
    Number(selectedCandidate.workOrderId) === Number(workOrderId) &&
    findActiveOrderCandidateById(workOrderId)
  ) {
    if (!selectedCandidate.eligible) {
      throw new Error(selectedCandidate.ineligibleReason || '当前订单不可加入活跃订单池')
    }
    return workOrderId
  }
  selectedCandidate = await resolveActiveOrderCandidateByKeyword()
  if (!selectedCandidate) {
    throw new Error('请选择订单号/产品候选')
  }
  if (!selectedCandidate.eligible) {
    throw new Error(selectedCandidate.ineligibleReason || '当前订单不可加入活跃订单池')
  }
  activeOrderForm.workOrderId = selectedCandidate.workOrderId
  activeOrderSelectedCandidate.value = selectedCandidate
  activeOrderCandidateKeyword.value = selectedCandidate.workOrderCode
  activeOrderCandidateError.value = ''
  return requirePositiveNumber(selectedCandidate.workOrderId, '请选择订单号/产品候选')
}

const activeOrderCommitSuccessMessage = (action: TeamLeaderActiveOrderCommitAction) => {
  if (action === 'ADD') return '活跃订单已加入'
  if (action === 'REUSE') return '活跃订单已存在'
  if (action === 'RECOVER') return '活跃订单已恢复'
  throw new Error(`活跃订单提交回执动作无效：${String(action)}`)
}

const submitAddActiveOrder = async () => {
  maintenanceSubmitting.value = true
  let writeCompleted = false
  try {
    const workOrderId = await requireSelectedActiveOrderCandidateWorkOrderId()
    const receipt = await addTeamLeaderActiveOrder({
      workOrderId
    })
    writeCompleted = true
    requirePositiveNumber(receipt.activeOrderId, '活跃订单提交回执缺少订单ID')
    const successMessage = activeOrderCommitSuccessMessage(receipt.action)
    ElMessage.success(successMessage)
    activeOrderAddDialogVisible.value = false
    resetActiveOrderForm()
    await loadActiveOrders()
  } catch (error) {
    ElMessage.error(
      resolveErrorMessage(
        error,
        writeCompleted ? '活跃订单已提交，但结果反馈或列表刷新失败' : '活跃订单加入失败'
      )
    )
  } finally {
    maintenanceSubmitting.value = false
  }
}

const getOrCreateActiveOrderReleaseIdempotencyKey = (row: TeamLeaderActiveOrderRespVO) => {
  const existingKey = releaseApplicationIdempotencyKeys.get(row.id)
  if (existingKey) return existingKey
  const idempotencyKey = `team-leader-active-order-release-${row.id}-${Date.now()}`
  releaseApplicationIdempotencyKeys.set(row.id, idempotencyKey)
  return idempotencyKey
}

const ACTIVE_ORDER_RELEASE_STATUSES = new Set<TeamLeaderActiveOrderReleaseApplicationStatus>([
  'PQC_RELEASE_PENDING',
  'PQC_RELEASE_REJECTED',
  'REPORT_UPLOAD_PENDING',
  'MANAGER_RELEASE_PENDING',
  'RELEASED'
])

const isActiveOrderReleaseStatus = (
  value: unknown
): value is TeamLeaderActiveOrderReleaseApplicationStatus =>
  typeof value === 'string' &&
  ACTIVE_ORDER_RELEASE_STATUSES.has(value as TeamLeaderActiveOrderReleaseApplicationStatus)

const resolveActiveOrderReleaseFailure = (
  error: unknown
): TeamLeaderActiveOrderReleaseFailureRespVO | undefined => {
  const candidate = (error as any)?.details ?? (error as any)?.response?.data?.data
  if (!candidate || typeof candidate !== 'object' || Array.isArray(candidate)) return undefined
  if (!Array.isArray(candidate.blockers) || candidate.blockers.length === 0) return undefined
  const blockersAreComplete = candidate.blockers.every((blocker: unknown) => {
    if (!blocker || typeof blocker !== 'object' || Array.isArray(blocker)) return false
    const record = blocker as Record<string, unknown>
    return (
      typeof record.blockerType === 'string' &&
      record.blockerType.trim().length > 0 &&
      typeof record.objectType === 'string' &&
      record.objectType.trim().length > 0 &&
      typeof record.reason === 'string' &&
      record.reason.trim().length > 0 &&
      typeof record.suggestion === 'string' &&
      record.suggestion.trim().length > 0
    )
  })
  return blockersAreComplete ? (candidate as TeamLeaderActiveOrderReleaseFailureRespVO) : undefined
}

const isDefinitiveActiveOrderReleaseBusinessFailure = (error: unknown) => {
  const code = (error as { code?: unknown } | null)?.code
  return typeof code === 'number' && Number.isFinite(code)
}

const confirmActiveOrderReleaseApplicationReceipt = async (
  row: TeamLeaderActiveOrderRespVO
): Promise<TeamLeaderActiveOrderReleaseApplyRespVO> => {
  const receipt = await getTeamLeaderActiveOrderRelease(row.id)
  assertActiveOrderReleaseApplicationReceipt(receipt, row.id)
  return receipt
}

const recoverUncertainActiveOrderReleaseApplication = async (
  row: TeamLeaderActiveOrderRespVO,
  writeError: unknown
) => {
  let receipt: TeamLeaderActiveOrderReleaseApplyRespVO
  try {
    receipt = await confirmActiveOrderReleaseApplicationReceipt(row)
  } catch (confirmationError) {
    releaseApplicationLocks.set(row.id, 'UNCERTAIN')
    releaseApplicationUncertainMessage.value =
      `申请响应不确定，正式回执确认失败，请人工核对后刷新页面：` +
      `写入错误 ${resolveErrorMessage(writeError, '申请响应异常')}；` +
      `回执错误 ${resolveErrorMessage(confirmationError, '正式回执确认失败')}`
    ElMessage.error(releaseApplicationUncertainMessage.value)
    return
  }

  releaseApplicationIdempotencyKeys.delete(row.id)
  releaseApplicationLocks.set(row.id, 'RECOVERED')
  releaseApplicationUncertainMessage.value = ''
  ElMessage.warning(
    `申请响应异常，但正式回执已确认：${formatActiveOrderReleaseStatus(receipt.status)}`
  )
}

const assertActiveOrderReleaseApplicationReceipt = (
  result: TeamLeaderActiveOrderReleaseApplyRespVO,
  activeOrderId: number,
  requireInitialStatus = false
) => {
  if (String(result.activeOrderId) !== String(activeOrderId)) {
    throw new Error('放行申请回执的活跃订单与当前订单不一致')
  }
  requirePositiveNumber(result.applicationId, '放行申请回执缺少申请记录ID')
  requirePositiveNumber(result.workOrderId, '放行申请回执缺少生产工单ID')
  requirePositiveNumber(result.routeId, '放行申请回执缺少工艺路线ID')
  requirePositiveNumber(result.routeVersionId, '放行申请回执缺少工艺路线版本ID')
  requirePositiveNumber(result.pqcReleaseWorkTaskId, '放行申请回执缺少PQC放行待办ID')
  if (!isActiveOrderReleaseStatus(result.status)) {
    throw new Error(`不支持的放行申请状态：${String(result.status)}`)
  }
  if (requireInitialStatus && result.status !== 'PQC_RELEASE_PENDING') {
    throw new Error(`首次申请回执状态必须为待PQC放行，实际为：${result.status}`)
  }
  if (!result.sourceSnapshotHash?.trim()) {
    throw new Error('放行申请回执缺少正式来源快照哈希')
  }
  requirePositiveNumber(result.version, '放行申请回执缺少正式版本号')
  if (!result.appliedAt) throw new Error('放行申请回执缺少申请时间')
}

const submitActiveOrderReleaseApplication = async (row: TeamLeaderActiveOrderRespVO) => {
  if (!canApplyActiveOrderRelease(row)) {
    ElMessage.warning(resolveActiveOrderReleaseApplyDisabledReason(row))
    return
  }
  try {
    await ElMessageBox.confirm(
      '系统将先完成生产订单和正式资料回填，再提交生产放行申请并生成一个PQC待办；不会创建批次、报告上传任务或最终放行事务。',
      '提交生产放行申请',
      { type: 'warning', confirmButtonText: '申请放行', cancelButtonText: '取消' }
    )
  } catch (confirmationAction) {
    if (confirmationAction === 'cancel' || confirmationAction === 'close') return
    ElMessage.error(resolveErrorMessage(confirmationAction, '申请确认弹窗打开失败'))
    return
  }
  const activeOrderId = requirePositiveNumber(row.id, '活跃订单记录ID不能为空')
  const idempotencyKey = getOrCreateActiveOrderReleaseIdempotencyKey(row)
  releaseApplicationSubmittingId.value = row.id
  releaseApplicationBlockers.value = []
  releaseApplicationUncertainMessage.value = ''
  let result: TeamLeaderActiveOrderReleaseApplyRespVO
  try {
    result = await applyTeamLeaderActiveOrderRelease({
      activeOrderId,
      idempotencyKey,
      applyRemark: '生产组长提交生产放行申请'
    })
  } catch (writeError) {
    const failure = resolveActiveOrderReleaseFailure(writeError)
    if (failure) {
      releaseApplicationBlockers.value = failure.blockers
      releaseApplicationLocks.delete(row.id)
      releaseApplicationUncertainMessage.value = ''
      ElMessage.error(resolveErrorMessage(writeError, failure.blockers[0].reason))
    } else if (isDefinitiveActiveOrderReleaseBusinessFailure(writeError)) {
      releaseApplicationLocks.delete(row.id)
      releaseApplicationUncertainMessage.value = ''
      ElMessage.error(resolveErrorMessage(writeError, '生产完工或放行申请失败'))
    } else {
      await recoverUncertainActiveOrderReleaseApplication(row, writeError)
    }
    releaseApplicationSubmittingId.value = undefined
    return
  }

  try {
    assertActiveOrderReleaseApplicationReceipt(result, row.id, true)
  } catch (receiptError) {
    releaseApplicationLocks.set(row.id, 'UNCERTAIN')
    releaseApplicationUncertainMessage.value =
      `申请接口已返回，但正式回执不完整，请人工核对后刷新页面：` +
      resolveErrorMessage(receiptError, '放行申请回执校验失败')
    ElMessage.error(releaseApplicationUncertainMessage.value)
    releaseApplicationSubmittingId.value = undefined
    return
  }

  releaseApplicationIdempotencyKeys.delete(row.id)
  releaseApplicationLocks.set(row.id, 'CONFIRMED')
  ElMessage.success('生产放行申请已提交，待PQC放行')
  try {
    await loadActiveOrders()
    const refreshedReceipt = activeOrderOptions.value.find((candidate) => candidate.id === row.id)
    if (
      refreshedReceipt?.releaseApplicationId === result.applicationId &&
      refreshedReceipt.releaseApplicationStatus === result.status
    ) {
      releaseApplicationLocks.delete(row.id)
    } else {
      releaseApplicationLocks.set(row.id, 'CONFIRMED_NOT_PROJECTED')
      releaseApplicationUncertainMessage.value = '申请已提交，但列表状态尚未同步，请刷新页面'
      ElMessage.warning(releaseApplicationUncertainMessage.value)
    }
  } catch (refreshError) {
    releaseApplicationLocks.set(row.id, 'CONFIRMED_REFRESH_FAILED')
    releaseApplicationUncertainMessage.value = `申请已提交，但列表刷新失败：${resolveErrorMessage(refreshError, '列表刷新失败')}`
    ElMessage.error(releaseApplicationUncertainMessage.value)
  } finally {
    releaseApplicationSubmittingId.value = undefined
  }
}

const submitMoveActiveOrder = async (
  row: TeamLeaderActiveOrderRespVO,
  direction: 'UP' | 'DOWN'
) => {
  activeOrderMoveSubmittingId.value = row.id
  activeOrderMoveDirection.value = direction
  let writeCompleted = false
  try {
    await moveTeamLeaderActiveOrder({
      activeOrderId: requirePositiveNumber(row.id, '活跃订单记录ID不能为空'),
      direction
    })
    writeCompleted = true
    await loadActiveOrders()
    ElMessage.success(direction === 'UP' ? '活跃订单已上移' : '活跃订单已下移')
  } catch (error) {
    ElMessage.error(
      resolveErrorMessage(
        error,
        writeCompleted
          ? '活跃订单排序已保存，但列表刷新失败'
          : direction === 'UP'
            ? '活跃订单上移失败'
            : '活跃订单下移失败'
      )
    )
  } finally {
    activeOrderMoveSubmittingId.value = undefined
    activeOrderMoveDirection.value = undefined
  }
}

const handleRebuildActiveOrder = async (row: TeamLeaderActiveOrderRespVO) => {
  activeOrderRebuildSubmittingId.value = row.id
  let writeCompleted = false
  try {
    const preview = await previewTeamLeaderActiveOrderRebuild(
      requirePositiveNumber(row.id, '活跃订单记录ID不能为空')
    )
    const confirmMessage = preview.hasHistoricalRuntimeData
      ? `当前活跃订单已有 ${preview.productionReportCount} 条报工记录、${preview.productionProgressCount} 条生产进度、${preview.pqcInspectionResultCount} 条 PQC 检验结果。确认后会先删除这些历史业务结果，并删除生产快照、PQC 快照，再按当前最新数据重建。${preview.releaseApplicationCount > 0 ? ` 另外将删除 ${preview.releaseApplicationCount} 条放行申请历史。` : ''}`
      : '确认重建当前活跃订单的生产快照和 PQC 快照？系统会删除旧生产快照、PQC 快照，并按当前最新数据重新生成。'
    await ElMessageBox.confirm(confirmMessage, '重建活跃订单', {
      type: preview.hasHistoricalRuntimeData ? 'warning' : 'info',
      confirmButtonText: '确认重建',
      cancelButtonText: '取消'
    })
    const result = await rebuildTeamLeaderActiveOrder({
      activeOrderId: requirePositiveNumber(row.id, '活跃订单记录ID不能为空'),
      confirmDeleteHistoricalRuntimeData: preview.hasHistoricalRuntimeData
    })
    writeCompleted = true
    ElMessage.success(
      `活跃订单已重建：生产快照 ${result.rebuiltProcessSnapshotCount} 条，PQC 快照 ${result.rebuiltPqcTaskCount} 条`
    )
    await loadActiveOrders()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(
      resolveErrorMessage(
        error,
        writeCompleted ? '活跃订单已重建，但列表刷新失败' : '活跃订单重建失败'
      )
    )
  } finally {
    activeOrderRebuildSubmittingId.value = undefined
  }
}

const handleActiveOrderVersionUpgrade = async (row: TeamLeaderActiveOrderRespVO) => {
  activeOrderVersionUpgradeSubmittingId.value = row.id
  try {
    const preview = await previewTeamLeaderActiveOrderVersionUpgrade(
      requirePositiveNumber(row.id, '活跃订单记录ID不能为空')
    )
    activeOrderVersionUpgradePreview.value = preview
    activeOrderVersionUpgradeReason.value = ''
    activeOrderVersionUpgradeConfirmRestart.value = false
    activeOrderVersionUpgradeDialogVisible.value = true
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '活跃订单版本升级预览失败'))
  } finally {
    activeOrderVersionUpgradeSubmittingId.value = undefined
  }
}

const submitActiveOrderVersionUpgrade = async () => {
  const preview = activeOrderVersionUpgradePreview.value
  if (!preview) {
    ElMessage.error('缺少活跃订单版本升级预览结果')
    return
  }
  activeOrderVersionUpgradeSubmittingId.value = preview.activeOrderId
  try {
    const result = await submitTeamLeaderActiveOrderVersionUpgrade({
      activeOrderId: requirePositiveNumber(preview.activeOrderId, '活跃订单记录ID不能为空'),
      idempotencyKey: `active-order-version-upgrade-${preview.activeOrderId}-${Date.now()}`,
      upgradeReason: activeOrderVersionUpgradeReason.value.trim(),
      confirmRestartFromBeginning: activeOrderVersionUpgradeConfirmRestart.value
    })
    ElMessage.success(`版本升级审批已提交：${result.requestCode || result.approvalStatus || '待审批'}`)
    activeOrderVersionUpgradeDialogVisible.value = false
    await loadActiveOrders()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '提交版本升级审批失败'))
  } finally {
    activeOrderVersionUpgradeSubmittingId.value = undefined
  }
}

const createSimulationCopyRunId = () => `SIMCOPY-${Date.now()}-${crypto.randomUUID()}`

const handleCopyLatestSimulationActiveOrder = async (row: TeamLeaderActiveOrderRespVO) => {
  activeOrderSimulationSubmittingId.value = row.id
  let result: TeamLeaderActiveOrderSimulationCopyRespVO | undefined
  try {
    await ElMessageBox.confirm(
      '系统将创建一张仅供测试、可清理的新工单，复制产品、数量、客户等基础信息，并重新绑定最新生效工艺路线和最新正式发布 QA 规程。不复制原订单的报工、进度、PQC 结果、批记录、领料、异常或放行历史。',
      '确认复制测试单',
      { type: 'warning', confirmButtonText: '确认复制', cancelButtonText: '取消' }
    )
    result = await copyLatestTeamLeaderSimulationActiveOrder({
      sourceActiveOrderId: requirePositiveNumber(row.id, '来源活跃订单ID不能为空'),
      simulationRunId: createSimulationCopyRunId()
    })
    ElMessage.success(
      `测试单 ${result.workOrderCode} 已创建，路线 ${result.routeVersionNo}，QA版本 ${result.qaRegulationVersionId}`
    )
    try {
      await loadActiveOrders()
    } catch (refreshError) {
      ElMessage.error(
        `测试单 ${result.workOrderCode} 已创建，但列表刷新失败：${resolveErrorMessage(refreshError, '列表刷新失败')}`
      )
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, '复制测试单失败'))
  } finally {
    activeOrderSimulationSubmittingId.value = undefined
  }
}

const handleCleanupLatestSimulationActiveOrder = async (row: TeamLeaderActiveOrderRespVO) => {
  activeOrderSimulationSubmittingId.value = row.id
  let writeCompleted = false
  try {
    await ElMessageBox.confirm(
      `确认清理测试模拟订单 ${row.workOrderCode || row.id}？系统只允许清理当前账号创建且带完整模拟标识的副本。`,
      '清理测试单',
      { type: 'warning', confirmButtonText: '确认清理', cancelButtonText: '取消' }
    )
    await cleanupLatestTeamLeaderSimulationActiveOrder(
      requirePositiveNumber(row.id, '测试活跃订单ID不能为空')
    )
    writeCompleted = true
    ElMessage.success('测试模拟订单已清理')
    try {
      await loadActiveOrders()
    } catch (refreshError) {
      ElMessage.error(
        `测试模拟订单已清理，但列表刷新失败：${resolveErrorMessage(refreshError, '列表刷新失败')}`
      )
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(
      resolveErrorMessage(error, writeCompleted ? '测试单已清理，但列表刷新失败' : '清理测试单失败')
    )
  } finally {
    activeOrderSimulationSubmittingId.value = undefined
  }
}

const handleSimulateStage1 = async (row: TeamLeaderActiveOrderRespVO) => {
  activeOrderSimulationSubmittingId.value = row.id
  let writeCompleted = false
  try {
    await ElMessageBox.confirm(
      '系统将从当前活跃订单创建一份可清理的测试订单，并通过正式生产和PQC提交、复核链路形成生产进度和检验进度均为100%的事实。不会完工、回填、创建批次、上传资料或放行。',
      '确认模拟生产和PQC',
      { type: 'warning', confirmButtonText: '开始模拟', cancelButtonText: '取消' }
    )
    const templateActiveOrderId = requirePositiveNumber(row.id, '活跃订单模板ID不能为空')
    const result = await simulateStage1ActiveOrderCompletion({
      simulationRunId: `STAGE1-${Date.now()}`,
      templateActiveOrderId
    })
    writeCompleted = true
    ElMessage.success(
      `Stage1 已完成：新活跃订单 ${result.activeOrderId}，生产进度 ${formatActiveOrderProgressPercent(result.productionProgressPercent)}，检验进度 ${formatActiveOrderProgressPercent(result.inspectionProgressPercent)}。`
    )
    const generatedActiveOrderId = requirePositiveNumber(
      result.activeOrderId,
      'Stage1 新活跃订单ID不能为空'
    )
    stage1GeneratedDetailTargets.value.set(templateActiveOrderId, {
      activeOrderId: generatedActiveOrderId,
      sourceWorkOrderCode: row.workOrderCode || ''
    })
    await loadActiveOrders()
    navigateActiveOrderSubmissionDetail(generatedActiveOrderId, row.workOrderCode || '')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(
      resolveErrorMessage(error, writeCompleted ? 'Stage1 已生成，但列表刷新失败' : 'Stage1 模拟失败')
    )
  } finally {
    activeOrderSimulationSubmittingId.value = undefined
  }
}

const handleSimulateStage2_5 = async (row: TeamLeaderActiveOrderRespVO) => {
  if (!row.version && row.version !== 0) {
    ElMessage.error('活跃订单版本缺失，无法安全执行模拟完工')
    return
  }
  activeOrderSimulationSubmittingId.value = row.id
  try {
    await ElMessageBox.confirm(
      '本次将复用真实活跃订单完工逻辑，统一回填批记录、过程检验记录和必要的损耗单，随后创建或打开真实批次执行并进入详情页。不会上传来料检、灭菌或成品检文件，也不会最终放行。',
      '确认模拟完工',
      { type: 'warning', confirmButtonText: '开始模拟', cancelButtonText: '取消' }
    )
    const result = await simulateStage2_5BackfillBatchExecution({
      simulationRunId: 'STAGE2_5-' + Date.now(),
      activeOrderId: row.id,
      expectedVersion: row.version
    })
    ElMessage.success('模拟完工已完成，正在打开真实批次执行详情')
    await router.push(result.detailPath)
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, '模拟完工失败'))
  } finally {
    activeOrderSimulationSubmittingId.value = undefined
  }
}

const handleRecommendedActiveOrderConflictResolution = async () => {
  const row = activeOrderConflictSelectedOrder.value
  if (!row) {
    ElMessage.error('请选择需要处理的活跃订单')
    return
  }

  const activeOrderId = requirePositiveNumber(row.id, '活跃订单记录ID不能为空')
  activeOrderConflictSubmitting.value = true
  activeOrderRebuildSubmittingId.value = row.id
  let writeCompletedPhase: 'NONE' | 'REBUILT' | 'SIMULATED' = 'NONE'
  try {
    const preview = await previewTeamLeaderActiveOrderRebuild(activeOrderId)
    await rebuildTeamLeaderActiveOrder({
      activeOrderId,
      confirmDeleteHistoricalRuntimeData: preview.hasHistoricalRuntimeData
    })
    writeCompletedPhase = 'REBUILT'
    activeOrderRebuildSubmittingId.value = undefined
    activeOrderSimulationSubmittingId.value = row.id

    const result = await simulateTeamLeaderActiveOrderCompletion({ activeOrderId })
    writeCompletedPhase = 'SIMULATED'
    ElMessage.success(
      `推荐修复已完成：生产进度 ${formatActiveOrderProgressPercent(result.productionProgressPercent)}，检验进度 ${formatActiveOrderProgressPercent(result.inspectionProgressPercent)}`
    )
    await loadActiveOrders()
    activeOrderConflictDrawerVisible.value = false
    activeOrderConflictDetail.value = undefined
  } catch (error) {
    const phaseMessage =
      writeCompletedPhase === 'REBUILT'
        ? '重建已完成，但模拟完成失败'
        : writeCompletedPhase === 'SIMULATED'
          ? '写入已完成，但列表刷新失败'
          : '推荐修复失败'
    const errorDetail = resolveErrorMessage(error, phaseMessage)
    ElMessage.error(errorDetail === phaseMessage ? phaseMessage : `${phaseMessage}：${errorDetail}`)
  } finally {
    activeOrderConflictSubmitting.value = false
    activeOrderRebuildSubmittingId.value = undefined
    activeOrderSimulationSubmittingId.value = undefined
  }
}

const handleRemoveActiveOrder = async (row: TeamLeaderActiveOrderRespVO) => {
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
      resolveErrorMessage(
        error,
        writeCompleted ? '活跃订单已移出，但列表刷新失败' : '活跃订单移出失败'
      )
    )
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
    await loadTeamDeviceOptions()
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
    if (!showProductionModuleTabs.value) {
      loadLegacyProductionWorkbenchData()
    }
    if (
      !showProductionModuleTabs.value ||
      activeProductionModuleTab.value === 'report' ||
      activeProductionModuleTab.value === 'reportHistory'
    ) {
      queryParams.leaderType = 'PRODUCTION'
      queryParams.submissionReviewStatus = undefined
      clearInitialSubmissionVisibleDefaultFilter()
      getSubmissionList()
    }
  } else {
    refreshPqcPersonnel()
    if (
      !showPqcModuleTabs.value ||
      activePqcModuleTab.value === 'management' ||
      activePqcModuleTab.value === 'history'
    ) {
      clearInitialSubmissionVisibleDefaultFilter()
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

.team-leader-workbench__module-tabs :deep(.el-tabs__nav-wrap) {
  min-width: 0;
}

.team-leader-workbench__module-tabs :deep(.el-tabs__item) {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.team-leader-workbench__production-module-card
  .team-leader-workbench__module-tabs--flat
  :deep(.el-tabs__header) {
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
  .team-leader-workbench__production-module-card
    .team-leader-workbench__module-tabs--flat
    :deep(.el-tabs__header) {
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

.team-leader-workbench__abnormal-work-order-id {
  color: var(--el-color-danger);
  font-weight: 600;
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

.team-leader-workbench__active-order-candidate.is-blocked {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
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
  display: block;
  width: 100%;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
  white-space: normal;
}

:global(.team-leader-workbench__active-order-candidate-popper) {
  width: min(520px, calc(100vw - 32px)) !important;
  max-width: calc(100vw - 32px);
}

:global(.team-leader-workbench__active-order-candidate-popper .el-select-dropdown__item) {
  height: auto;
  min-height: 48px;
  padding: 8px 12px;
  line-height: normal;
  white-space: normal;
}

.team-leader-workbench__active-order-option {
  display: grid;
  min-width: 220px;
  padding: 6px 0;
  gap: 4px;
  line-height: 1.25;
}

:global(.team-leader-workbench__allocation-order-popper) {
  width: min(420px, calc(100vw - 48px)) !important;
  min-width: min(360px, calc(100vw - 48px)) !important;
  max-width: calc(100vw - 48px);
}

:global(.team-leader-workbench__allocation-order-popper .el-select-dropdown__item) {
  height: auto;
  line-height: normal;
  min-height: 68px;
  padding: 6px 12px;
}

:global(
  .team-leader-workbench__allocation-order-popper
    .el-select-dropdown__item
    + .el-select-dropdown__item
) {
  border-top: 1px solid #eef2f7;
}

.team-leader-workbench__active-order-option > div {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 8px;
  align-items: baseline;
}

.team-leader-workbench__active-order-option span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.team-leader-workbench__active-order-option strong {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  font-size: 13px;
  font-weight: 700;
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

.team-leader-workbench__process-config-filter {
  flex: 1 1 auto;
  min-width: 0;
}

.team-leader-workbench__process-config-filter-head > .el-button {
  flex: 0 0 auto;
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

.team-leader-workbench__rough-wash-context {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-regular);
  font-size: 13px;
}

:global(.team-leader-workbench__rough-wash-dialog) {
  display: flex;
  max-height: calc(100vh - 32px);
  flex-direction: column;
  margin-bottom: 16px;
}

:global(.team-leader-workbench__rough-wash-dialog .el-dialog__body) {
  min-height: 0;
  overflow-y: auto;
}

:global(.team-leader-workbench__rough-wash-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding-top: 14px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.team-leader-workbench__rough-wash-context span {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.team-leader-workbench__rough-wash-context b {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 600;
}

.team-leader-workbench__rough-wash-section {
  padding-top: 18px;
}

.team-leader-workbench__rough-wash-section h3 {
  margin: 0 0 12px;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0;
}

.team-leader-workbench__rough-wash-config {
  border-top: 1px solid var(--el-border-color-lighter);
}

.team-leader-workbench__rough-wash-row {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  min-height: 66px;
  align-items: center;
  gap: 20px;
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.team-leader-workbench__rough-wash-label {
  display: grid;
  gap: 3px;
}

.team-leader-workbench__rough-wash-label strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.team-leader-workbench__rough-wash-label span,
.team-leader-workbench__rough-wash-range label > span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__rough-wash-control {
  display: flex;
  width: min(280px, 100%);
  align-items: center;
  gap: 10px;
}

.team-leader-workbench__rough-wash-control :deep(.el-input-number),
.team-leader-workbench__rough-wash-control :deep(.el-select) {
  width: 220px;
}

.team-leader-workbench__rough-wash-range {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) auto minmax(120px, 1fr) auto;
  align-items: end;
  gap: 10px;
}

.team-leader-workbench__rough-wash-range--three {
  grid-template-columns: repeat(3, minmax(110px, 1fr)) auto;
}

.team-leader-workbench__rough-wash-range label {
  display: grid;
  min-width: 0;
  gap: 5px;
}

.team-leader-workbench__rough-wash-range :deep(.el-input-number) {
  width: 100%;
}

.team-leader-workbench__rough-wash-separator,
.team-leader-workbench__rough-wash-unit {
  padding-bottom: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.team-leader-workbench__rough-wash-control .team-leader-workbench__rough-wash-unit {
  padding-bottom: 0;
}

.team-leader-workbench__rough-wash-preview {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}

.team-leader-workbench__rough-wash-preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.team-leader-workbench__rough-wash-preview-grid label {
  display: grid;
  min-width: 0;
  gap: 6px;
  color: var(--el-text-color-regular);
  font-size: 12px;
  font-weight: 600;
}

.team-leader-workbench__rough-wash-preview-grid :deep(.el-input-number),
.team-leader-workbench__rough-wash-preview-grid :deep(.el-select) {
  width: 100%;
}

@media (max-width: 720px) {
  .team-leader-workbench__rough-wash-context,
  .team-leader-workbench__rough-wash-preview-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .team-leader-workbench__rough-wash-row {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
  }

  .team-leader-workbench__rough-wash-range,
  .team-leader-workbench__rough-wash-range--three {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }

  .team-leader-workbench__rough-wash-range--three label:last-of-type {
    grid-column: 1 / -1;
  }

  .team-leader-workbench__rough-wash-separator {
    display: none;
  }
}

.team-leader-workbench__allocation-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.team-leader-workbench__allocation-toolbar > div:last-child {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.team-leader-workbench__allocation-table {
  width: 100%;
  font-size: 13px;
}

.team-leader-workbench__allocation-table :deep(.el-table__cell) {
  padding: 6px 0;
}

.team-leader-workbench__allocation-table :deep(.cell) {
  padding: 0 8px;
}

.team-leader-workbench__allocation-table :deep(.el-table__header .cell) {
  color: #667085;
  font-weight: 700;
}

.team-leader-workbench__allocation-order-select {
  width: 100%;
}

.team-leader-workbench__allocation-order-select :deep(.el-select__wrapper) {
  height: auto;
  min-height: 32px;
  padding-top: 5px;
  padding-bottom: 5px;
}

.team-leader-workbench__allocation-order-label {
  display: block;
  width: 100%;
  color: var(--el-text-color-regular);
  line-height: 20px;
  overflow-wrap: anywhere;
  white-space: normal;
}

.team-leader-workbench__allocation-quantity-cell {
  display: grid;
  align-items: center;
  grid-template-columns: minmax(100px, 1fr) repeat(3, max-content);
  gap: 6px;
}

.team-leader-workbench__report-overage,
.team-leader-workbench__allocation-overage {
  color: #fff;
  font-weight: 700;
}

.team-leader-workbench__allocation-overage {
  grid-column: 1 / -1;
  width: 100%;
  justify-content: center;
  color: var(--el-color-danger);
}

.team-leader-workbench__allocation-quantity-input {
  width: 100%;
}

.team-leader-workbench__allocation-quantity-cell .el-button + .el-button {
  margin-left: 0;
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

.team-leader-workbench__active-order-conflict-tag {
  flex: 0 0 auto;
  cursor: pointer;
  user-select: none;
}

.team-leader-workbench__active-order-conflict-tag:focus-visible {
  outline: 2px solid var(--el-color-danger);
  outline-offset: 2px;
}

.team-leader-workbench__active-order-conflict-drawer {
  display: grid;
  gap: 14px;
  min-height: 180px;
}

.team-leader-workbench__active-order-conflict-drawer-panel {
  max-width: calc(100vw - 24px);
}

.team-leader-workbench__active-order-conflict-summary {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) minmax(190px, 1.4fr) 88px 88px;
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-border-color-light);
}

.team-leader-workbench__active-order-conflict-summary > div {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 11px 12px;
  background: var(--el-bg-color);
}

.team-leader-workbench__active-order-conflict-summary span,
.team-leader-workbench__active-order-conflict-process-metrics span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__active-order-conflict-summary strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__active-order-conflict-actions {
  display: flex;
  justify-content: flex-end;
}

.team-leader-workbench__active-order-conflict-process-list {
  display: grid;
  gap: 10px;
}

.team-leader-workbench__active-order-conflict-process {
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.team-leader-workbench__active-order-conflict-process.is-quantity-conflict {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}

.team-leader-workbench__active-order-conflict-process-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-lighter);
}

.team-leader-workbench__active-order-conflict-process-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(80px, 1fr));
  gap: 1px;
  background: var(--el-border-color-lighter);
}

.team-leader-workbench__active-order-conflict-process-metrics > div {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  background: var(--el-bg-color);
}

.team-leader-workbench__active-order-conflict-process-metrics strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.team-leader-workbench__active-order-detail {
  display: grid;
  gap: 16px;
  min-height: 180px;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
}

.team-leader-workbench__active-order-detail-summary {
  display: grid;
  grid-template-columns: minmax(180px, 0.9fr) minmax(280px, 1.5fr) 90px;
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-border-color-light);
}

.team-leader-workbench__active-order-detail-summary > div {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 12px 14px;
  background: var(--el-bg-color);
}

.team-leader-workbench__active-order-detail-summary span,
.team-leader-workbench__active-order-process-metrics span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__active-order-detail-summary strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__active-order-process-detail {
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
}

:deep(.team-leader-workbench__active-order-row--quantity-conflict > td) {
  background: var(--el-color-danger-light-9) !important;
}

.team-leader-workbench__active-order-code-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.team-leader-workbench__active-order-code-cell > span:first-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__active-order-process-detail.is-quantity-conflict {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}

.team-leader-workbench__quantity-conflict-text {
  color: var(--el-color-danger);
}

.team-leader-workbench__active-order-process-header {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 20px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-lighter);
}

.team-leader-workbench__active-order-process-title {
  display: flex;
  flex: 1 1 auto;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.team-leader-workbench__active-order-process-title strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__active-order-process-title span {
  flex: 0 0 auto;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__active-order-process-metrics {
  display: grid;
  flex: 0 0 360px;
  grid-template-columns: repeat(3, minmax(100px, 1fr));
  gap: 18px;
}

.team-leader-workbench__active-order-process-metrics > div {
  display: grid;
  gap: 3px;
}

.team-leader-workbench__active-order-process-metrics strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.team-leader-workbench__active-order-submission-table {
  width: 100%;
}

.team-leader-workbench__active-order-detail-section-title {
  padding: 12px 14px 8px;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.team-leader-workbench__active-order-pqc-card {
  display: grid;
  gap: 8px;
  padding-bottom: 10px;
}

.team-leader-workbench__active-order-pqc-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.team-leader-workbench__active-order-pqc-title strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
}

:deep(.team-leader-workbench__active-order-submission-row--quantity-conflict > td) {
  background: var(--el-color-danger-light-9) !important;
  color: var(--el-color-danger);
}

.team-leader-workbench__active-order-submission-table .is-pending {
  color: var(--el-color-warning);
}

@media (max-width: 760px) {
  .team-leader-workbench__active-order-conflict-summary,
  .team-leader-workbench__active-order-conflict-process-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .team-leader-workbench__active-order-conflict-process-header {
    align-items: flex-start;
  }

  .team-leader-workbench__active-order-detail-summary {
    grid-template-columns: 1fr;
  }

  .team-leader-workbench__active-order-process-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .team-leader-workbench__active-order-process-metrics {
    width: 100%;
    flex-basis: auto;
  }
}

.team-leader-workbench__loss-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.team-leader-workbench__loss-maintenance-context {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 20px;
  margin-bottom: 14px;
  color: var(--el-text-color-regular);
}

.team-leader-workbench__loss-maintenance-editor {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.team-leader-workbench__loss-maintenance-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.team-leader-workbench__loss-maintenance-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.team-leader-workbench__loss-maintenance-create-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px 190px;
  align-items: center;
  gap: 0;
  padding: 10px 12px;
  border-top: 1px solid var(--el-table-border-color);
}

.team-leader-workbench__loss-maintenance-create-row > :nth-child(2) {
  justify-self: center;
}

.team-leader-workbench__loss-maintenance-toolbar {
  display: flex;
  justify-content: flex-start;
  margin-top: 12px;
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
  display: grid;
  gap: 0;
}

:global(.team-leader-workbench__correction-dialog) {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 24px);
  margin: 12px auto;
}

:global(.team-leader-workbench__correction-dialog .el-dialog__header),
:global(.team-leader-workbench__correction-dialog .el-dialog__footer) {
  flex: 0 0 auto;
}

:global(.team-leader-workbench__correction-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0 24px;
  overflow-y: auto;
}

.team-leader-workbench__correction-section {
  padding: 18px 0;
  border-bottom: 1px solid #e5e7eb;
}

.team-leader-workbench__correction-section:last-child {
  border-bottom: 0;
}

.team-leader-workbench__correction-title {
  margin: 0 0 12px;
  color: #18212f;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.team-leader-workbench__correction-context {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
}

.team-leader-workbench__correction-context > div {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  align-items: baseline;
  min-width: 0;
}

.team-leader-workbench__correction-context span,
.team-leader-workbench__correction-empty {
  color: #667085;
}

.team-leader-workbench__correction-context strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #1f2937;
  font-weight: 600;
}

.team-leader-workbench__correction-quantity-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.team-leader-workbench__correction-quantity-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.team-leader-workbench__correction-material-tabs,
.team-leader-workbench__correction-device-tabs {
  min-width: 0;
}

.team-leader-workbench__correction-material-card {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.team-leader-workbench__correction-material-title {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.team-leader-workbench__correction-material-title > strong,
.team-leader-workbench__correction-material-title > small {
  min-width: 0;
  overflow-wrap: anywhere;
}

.team-leader-workbench__correction-material-title > strong {
  color: #18212f;
  font-size: 14px;
}

.team-leader-workbench__correction-material-title > small {
  color: #667085;
}

.team-leader-workbench__correction-material-summary {
  display: grid;
  gap: 8px;
}

.team-leader-workbench__correction-material-card :deep(.el-form-item),
.team-leader-workbench__correction-device-tabs :deep(.el-form-item) {
  margin-bottom: 0;
}

.team-leader-workbench__material-detail-list {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.team-leader-workbench__material-detail-list > strong {
  color: #344054;
  font-size: 13px;
  font-weight: 700;
}

.team-leader-workbench__correction-field-row {
  display: grid;
  grid-template-columns: 128px minmax(180px, 220px);
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.team-leader-workbench__correction-field-label {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #667085;
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
}

.team-leader-workbench__correction-field-label small {
  margin-left: 4px;
  color: #7b8494;
  font-size: 12px;
  font-weight: 500;
}

.team-leader-workbench__correction-field-control {
  min-width: 0;
}

.team-leader-workbench__correction-field-control :deep(.el-input-number),
.team-leader-workbench__correction-field-control :deep(.el-select),
.team-leader-workbench__correction-field-control :deep(.el-input) {
  width: 100%;
}

.team-leader-workbench__correction-rows {
  display: grid;
  gap: 8px;
}

.team-leader-workbench__correction-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 16px;
  align-items: center;
  min-height: 40px;
}

.team-leader-workbench__correction-row--stacked {
  grid-template-columns: minmax(120px, 0.8fr) minmax(140px, 1fr) minmax(220px, 2fr);
  align-items: start;
}

.team-leader-workbench__correction-row--stacked > span {
  padding-top: 8px;
}

.team-leader-workbench__correction-row > span {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #344054;
}

.team-leader-workbench__correction-row small {
  margin-left: 6px;
  color: #7b8494;
  font-size: 12px;
}

.team-leader-workbench__correction-row :deep(.el-input-number) {
  width: 100%;
}

.team-leader-workbench__correction-empty {
  min-height: 40px;
  padding: 10px 12px;
  border-left: 3px solid #cbd5e1;
  background: #f7f8fa;
  font-size: 13px;
  line-height: 20px;
}

.team-leader-workbench__correction-preview {
  display: grid;
  gap: 1px;
  overflow: hidden;
  border: 1px solid #dfe3e8;
  border-radius: 4px;
  background: #dfe3e8;
}

.team-leader-workbench__correction-preview > div {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(100px, 1fr) 20px minmax(100px, 1fr);
  gap: 10px;
  align-items: center;
  min-height: 42px;
  padding: 8px 12px;
  background: #fff;
}

.team-leader-workbench__correction-preview > div > span:first-child {
  color: #344054;
  font-weight: 600;
}

.team-leader-workbench__correction-preview svg {
  color: #87909e;
}

.team-leader-workbench__correction-before {
  color: #8a4b3a;
  text-decoration: line-through;
}

.team-leader-workbench__correction-preview strong {
  color: #176b4d;
  font-weight: 700;
}

.team-leader-workbench__correction-confirm :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

@media (max-width: 640px) {
  :global(.team-leader-workbench__correction-dialog .el-dialog__body) {
    padding: 0 16px;
  }

  .team-leader-workbench__correction-context,
  .team-leader-workbench__correction-quantity-grid,
  .team-leader-workbench__correction-material-summary {
    grid-template-columns: minmax(0, 1fr);
  }

  .team-leader-workbench__correction-row {
    grid-template-columns: minmax(0, 1fr);
    gap: 6px;
  }

  .team-leader-workbench__correction-preview > div {
    grid-template-columns: minmax(90px, 1fr) minmax(72px, 1fr) 16px minmax(72px, 1fr);
    gap: 6px;
    padding: 8px;
    font-size: 12px;
  }
}

.team-leader-workbench__number {
  width: 100%;
}

.team-leader-workbench__allocation-context {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #dfe7e2;
  border-radius: 6px;
  background: #f8fbf9;
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

.team-leader-workbench__submission-detail {
  display: grid;
  gap: 12px;
  padding: 10px 12px 12px;
  background: #fbfcfd;
}

.team-leader-workbench__submission-detail-section {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.team-leader-workbench__submission-detail-title {
  color: #172033;
  font-size: 14px;
  font-weight: 700;
}

.team-leader-workbench__submission-material-cards {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.team-leader-workbench__submission-material-card {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.team-leader-workbench__submission-material-head {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: center;
  min-width: 0;
  color: #475467;
  font-size: 12px;
}

.team-leader-workbench__submission-material-head > strong {
  color: #18212f;
  font-size: 13px;
}

.team-leader-workbench__submission-detail-table :deep(.el-table__cell) {
  vertical-align: top;
}

.team-leader-workbench__submission-device-parameter-inline {
  display: block;
  color: #1f2937;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.team-leader-workbench__submission-device-parameter-inline.is-out-of-range {
  color: #c00000;
}

.team-leader-workbench__submission-material-empty {
  color: #64748b;
  font-size: 12px;
}

.team-leader-workbench__submission-table--single-line:deep(.el-table__body .el-table__cell .cell) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.team-leader-workbench__submission-table--single-line .team-leader-workbench__structured-list {
  flex-wrap: nowrap;
  overflow: hidden;
  min-width: 0;
}

.team-leader-workbench__submission-table--single-line .team-leader-workbench__structured-pill {
  flex: 0 0 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: keep-all;
}

.team-leader-workbench__submission-table--single-line .team-leader-workbench__parameter-list {
  display: flex;
  flex-wrap: nowrap;
  overflow: hidden;
  min-width: 0;
}

.team-leader-workbench__submission-table--single-line .team-leader-workbench__parameter-item {
  flex: 0 0 auto;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-leader-workbench__context-block {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.team-leader-workbench__context-block > strong {
  flex: 0 0 64px;
  color: #18212f;
  font-size: 13px;
}

.team-leader-workbench__submission-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  white-space: nowrap;
}

.team-leader-workbench__submission-actions :deep(.el-button + .el-button) {
  margin-left: 0;
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
  min-width: 0;
  gap: 6px;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
  white-space: normal;
}

.team-leader-workbench__parameter-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(72px, auto);
  gap: 6px 8px;
  align-items: start;
}

.team-leader-workbench__parameter-item > * {
  min-width: 0;
}

.team-leader-workbench__parameter-label {
  color: #0f172a;
  font-weight: 600;
  overflow-wrap: anywhere;
  word-break: break-word;
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
  .team-leader-workbench__loss-maintenance-context,
  .team-leader-workbench__loss-maintenance-create-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .team-leader-workbench__loss-maintenance-create-row > :nth-child(2) {
    justify-self: start;
  }

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

