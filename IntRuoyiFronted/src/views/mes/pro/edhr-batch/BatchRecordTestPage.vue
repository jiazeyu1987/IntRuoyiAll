<template>
  <ContentWrap>
    <div class="edhr-batch-record-test-page" data-edhr-batch-record-test-page>
      <el-tabs v-model="activeInnerTab" class="edhr-batch-record-test-page__inner-tabs">
        <el-tab-pane label="生产组长" name="productionLeader">
          <UnifiedListTemplate
            class="edhr-batch-record-test-page__list-template"
            data-edhr-batch-record-test-production-leader-list
            table-key="mes.pro.edhrBatchRecordTest.productionLeader"
            :query-model="queryParams"
            :filter-definitions="productionLeaderQuickFilterDefinitions"
            :quick-filter-state="productionLeaderQuickFilter.state"
            :selected-filter-definition="productionLeaderQuickFilter.selectedDefinition.value"
            :operator-options="productionLeaderQuickFilter.operatorOptions.value"
            :columns="productionLeaderColumns"
            :column-saving="productionLeaderColumnSaving"
            :show-column-settings="false"
            :show-column-reset="false"
            :single-line-toolbar="true"
            :total="filteredProductionLeaderRows.length"
            v-model:page="queryParams.pageNo"
            v-model:limit="queryParams.pageSize"
            @update:quick-filter-state="productionLeaderQuickFilter.updateState"
            @quick-filter-query="productionLeaderQuickFilter.applyQuickFilter"
            @column-change="saveProductionLeaderColumnConfig"
            @pagination="handleProductionLeaderPagination"
          >
            <template #actions>
              <el-form-item class="edhr-batch-record-test-page__tenant-filter" label="测试租户">
                <el-select v-model="selectedTenantId" class="!w-240px" placeholder="请选择测试租户">
                  <el-option
                    v-for="tenant in tenantOptions"
                    :key="tenant.id"
                    :label="tenant.name"
                    :value="tenant.id"
                  />
                </el-select>
              </el-form-item>
              <el-button
                v-hasPermi="['system:codex-test:execute']"
                class="edhr-batch-record-test-page__run-all-button"
                data-edhr-batch-record-test-run-all-button
                :disabled="
                  !selectedTenantId ||
                  testingRowCaseName !== undefined ||
                  testingTabListKey !== undefined
                "
                :icon="VideoPlay"
                :loading="testingTabListKey === 'productionLeader'"
                type="success"
                @click="handleTestTab('productionLeader')"
              >
                {{ getTabTestButtonText('productionLeader') }}
              </el-button>
              <el-button type="primary" @click="openCreateRowDialog('productionLeader')">
                新增
              </el-button>
            </template>

            <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
              <el-alert
                v-if="loadError"
                class="edhr-batch-record-test-page__alert"
                :title="loadError"
                type="error"
                :closable="false"
                show-icon
              />
              <el-table
                data-user-table-column-explicit
                data-user-table-key="mes.pro.edhrBatchRecordTest.productionLeader"
                :data="pagedProductionLeaderRows"
                border
                row-key="id"
                :show-overflow-tooltip="true"
                stripe
                @header-dragend="handleProductionLeaderHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
                <el-table-column
                  v-if="isProductionLeaderColumnVisible('sort')"
                  label="序号"
                  prop="sort"
                  :width="getProductionLeaderColumnWidthString('sort', 80)"
                  v-bind="sortColumnAttrs('sort')"
                />
                <el-table-column
                  v-if="isProductionLeaderColumnVisible('title')"
                  label="职责"
                  prop="title"
                  :min-width="getProductionLeaderColumnMinWidthString('title', 220)"
                />
                <el-table-column
                  v-if="isProductionLeaderColumnVisible('description')"
                  class-name="edhr-batch-record-test-page__description-column"
                  label="描述"
                  prop="description"
                  :min-width="getProductionLeaderColumnMinWidthString('description', 320)"
                  :show-overflow-tooltip="false"
                />
                <el-table-column
                  v-if="isProductionLeaderColumnVisible('actions')"
                  fixed="right"
                  label="操作"
                  prop="actions"
                  :width="getProductionLeaderColumnWidthString('actions', 180)"
                >
                  <template #default="{ row }">
                    <el-button
                      v-hasPermi="['system:codex-test:execute']"
                      :disabled="
                        !selectedTenantId ||
                        testingRowCaseName !== undefined ||
                        testingTabListKey !== undefined
                      "
                      :loading="testingRowCaseName === row.caseName"
                      link
                      type="success"
                      @click="handleTestRow(row)"
                    >
                      测试
                    </el-button>
                    <el-button
                      data-edhr-batch-record-test-history-button
                      :disabled="!isRowTestHistoryReady(row)"
                      link
                      :type="getRowTestHistoryButtonType(row)"
                      @click="openRowTestHistory(row)"
                    >
                      历史
                    </el-button>
                    <el-button
                      link
                      type="primary"
                      @click="openDescriptionEditor('productionLeader', row)"
                    >
                      修改
                    </el-button>
                    <el-button link type="danger" @click="handleDeleteRow('productionLeader', row)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </el-tab-pane>
        <el-tab-pane label="一线PQC" name="frontlinePqc">
          <UnifiedListTemplate
            class="edhr-batch-record-test-page__list-template"
            data-edhr-batch-record-test-frontline-pqc-list
            table-key="mes.pro.edhrBatchRecordTest.frontlinePqc"
            :query-model="frontlinePqcQueryParams"
            :filter-definitions="frontlinePqcQuickFilterDefinitions"
            :quick-filter-state="frontlinePqcQuickFilter.state"
            :selected-filter-definition="frontlinePqcQuickFilter.selectedDefinition.value"
            :operator-options="frontlinePqcQuickFilter.operatorOptions.value"
            :columns="frontlinePqcColumns"
            :column-saving="frontlinePqcColumnSaving"
            :show-column-settings="false"
            :show-column-reset="false"
            :single-line-toolbar="true"
            :total="filteredFrontlinePqcRows.length"
            v-model:page="frontlinePqcQueryParams.pageNo"
            v-model:limit="frontlinePqcQueryParams.pageSize"
            @update:quick-filter-state="frontlinePqcQuickFilter.updateState"
            @quick-filter-query="frontlinePqcQuickFilter.applyQuickFilter"
            @column-change="saveFrontlinePqcColumnConfig"
            @pagination="handleFrontlinePqcPagination"
          >
            <template #actions>
              <el-form-item class="edhr-batch-record-test-page__tenant-filter" label="测试租户">
                <el-select v-model="selectedTenantId" class="!w-240px" placeholder="请选择测试租户">
                  <el-option
                    v-for="tenant in tenantOptions"
                    :key="tenant.id"
                    :label="tenant.name"
                    :value="tenant.id"
                  />
                </el-select>
              </el-form-item>
              <el-button
                v-hasPermi="['system:codex-test:execute']"
                class="edhr-batch-record-test-page__run-all-button"
                data-edhr-batch-record-test-run-all-button
                :disabled="
                  !selectedTenantId ||
                  testingRowCaseName !== undefined ||
                  testingTabListKey !== undefined
                "
                :icon="VideoPlay"
                :loading="testingTabListKey === 'frontlinePqc'"
                type="success"
                @click="handleTestTab('frontlinePqc')"
              >
                {{ getTabTestButtonText('frontlinePqc') }}
              </el-button>
              <el-button type="primary" @click="openCreateRowDialog('frontlinePqc')">
                新增
              </el-button>
            </template>

            <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
              <el-alert
                v-if="loadError"
                class="edhr-batch-record-test-page__alert"
                :title="loadError"
                type="error"
                :closable="false"
                show-icon
              />
              <el-table
                data-user-table-column-explicit
                data-user-table-key="mes.pro.edhrBatchRecordTest.frontlinePqc"
                :data="pagedFrontlinePqcRows"
                border
                row-key="id"
                :show-overflow-tooltip="true"
                stripe
                @header-dragend="handleFrontlinePqcHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
                <el-table-column
                  v-if="isFrontlinePqcColumnVisible('sort')"
                  label="序号"
                  prop="sort"
                  :width="getFrontlinePqcColumnWidthString('sort', 80)"
                  v-bind="sortColumnAttrs('sort')"
                />
                <el-table-column
                  v-if="isFrontlinePqcColumnVisible('title')"
                  label="任务"
                  prop="title"
                  :min-width="getFrontlinePqcColumnMinWidthString('title', 220)"
                />
                <el-table-column
                  v-if="isFrontlinePqcColumnVisible('description')"
                  class-name="edhr-batch-record-test-page__description-column"
                  label="描述"
                  prop="description"
                  :min-width="getFrontlinePqcColumnMinWidthString('description', 320)"
                  :show-overflow-tooltip="false"
                />
                <el-table-column
                  v-if="isFrontlinePqcColumnVisible('actions')"
                  fixed="right"
                  label="操作"
                  prop="actions"
                  :width="getFrontlinePqcColumnWidthString('actions', 180)"
                >
                  <template #default="{ row }">
                    <el-button
                      v-hasPermi="['system:codex-test:execute']"
                      :disabled="
                        !selectedTenantId ||
                        testingRowCaseName !== undefined ||
                        testingTabListKey !== undefined
                      "
                      :loading="testingRowCaseName === row.caseName"
                      link
                      type="success"
                      @click="handleTestRow(row)"
                    >
                      测试
                    </el-button>
                    <el-button
                      data-edhr-batch-record-test-history-button
                      :disabled="!isRowTestHistoryReady(row)"
                      link
                      :type="getRowTestHistoryButtonType(row)"
                      @click="openRowTestHistory(row)"
                    >
                      历史
                    </el-button>
                    <el-button
                      link
                      type="primary"
                      @click="openDescriptionEditor('frontlinePqc', row)"
                    >
                      修改
                    </el-button>
                    <el-button link type="danger" @click="handleDeleteRow('frontlinePqc', row)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </el-tab-pane>
        <el-tab-pane label="一线生产" name="frontlineProduction">
          <UnifiedListTemplate
            class="edhr-batch-record-test-page__list-template"
            data-edhr-batch-record-test-frontline-production-list
            table-key="mes.pro.edhrBatchRecordTest.frontlineProduction"
            :query-model="frontlineProductionQueryParams"
            :filter-definitions="frontlineProductionQuickFilterDefinitions"
            :quick-filter-state="frontlineProductionQuickFilter.state"
            :selected-filter-definition="frontlineProductionQuickFilter.selectedDefinition.value"
            :operator-options="frontlineProductionQuickFilter.operatorOptions.value"
            :columns="frontlineProductionColumns"
            :column-saving="frontlineProductionColumnSaving"
            :show-column-settings="false"
            :show-column-reset="false"
            :single-line-toolbar="true"
            :total="filteredFrontlineProductionRows.length"
            v-model:page="frontlineProductionQueryParams.pageNo"
            v-model:limit="frontlineProductionQueryParams.pageSize"
            @update:quick-filter-state="frontlineProductionQuickFilter.updateState"
            @quick-filter-query="frontlineProductionQuickFilter.applyQuickFilter"
            @column-change="saveFrontlineProductionColumnConfig"
            @pagination="handleFrontlineProductionPagination"
          >
            <template #actions>
              <el-form-item class="edhr-batch-record-test-page__tenant-filter" label="测试租户">
                <el-select v-model="selectedTenantId" class="!w-240px" placeholder="请选择测试租户">
                  <el-option
                    v-for="tenant in tenantOptions"
                    :key="tenant.id"
                    :label="tenant.name"
                    :value="tenant.id"
                  />
                </el-select>
              </el-form-item>
              <el-button
                v-hasPermi="['system:codex-test:execute']"
                class="edhr-batch-record-test-page__run-all-button"
                data-edhr-batch-record-test-run-all-button
                :disabled="
                  !selectedTenantId ||
                  testingRowCaseName !== undefined ||
                  testingTabListKey !== undefined
                "
                :icon="VideoPlay"
                :loading="testingTabListKey === 'frontlineProduction'"
                type="success"
                @click="handleTestTab('frontlineProduction')"
              >
                {{ getTabTestButtonText('frontlineProduction') }}
              </el-button>
              <el-button type="primary" @click="openCreateRowDialog('frontlineProduction')">
                新增
              </el-button>
            </template>

            <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
              <el-alert
                v-if="loadError"
                class="edhr-batch-record-test-page__alert"
                :title="loadError"
                type="error"
                :closable="false"
                show-icon
              />
              <el-table
                data-user-table-column-explicit
                data-user-table-key="mes.pro.edhrBatchRecordTest.frontlineProduction"
                :data="pagedFrontlineProductionRows"
                border
                row-key="id"
                :show-overflow-tooltip="true"
                stripe
                @header-dragend="handleFrontlineProductionHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
                <el-table-column
                  v-if="isFrontlineProductionColumnVisible('sort')"
                  label="序号"
                  prop="sort"
                  :width="getFrontlineProductionColumnWidthString('sort', 80)"
                  v-bind="sortColumnAttrs('sort')"
                />
                <el-table-column
                  v-if="isFrontlineProductionColumnVisible('title')"
                  label="任务"
                  prop="title"
                  :min-width="getFrontlineProductionColumnMinWidthString('title', 220)"
                />
                <el-table-column
                  v-if="isFrontlineProductionColumnVisible('description')"
                  class-name="edhr-batch-record-test-page__description-column"
                  label="描述"
                  prop="description"
                  :min-width="getFrontlineProductionColumnMinWidthString('description', 320)"
                  :show-overflow-tooltip="false"
                />
                <el-table-column
                  v-if="isFrontlineProductionColumnVisible('actions')"
                  fixed="right"
                  label="操作"
                  prop="actions"
                  :width="getFrontlineProductionColumnWidthString('actions', 180)"
                >
                  <template #default="{ row }">
                    <el-button
                      v-hasPermi="['system:codex-test:execute']"
                      :disabled="
                        !selectedTenantId ||
                        testingRowCaseName !== undefined ||
                        testingTabListKey !== undefined
                      "
                      :loading="testingRowCaseName === row.caseName"
                      link
                      type="success"
                      @click="handleTestRow(row)"
                    >
                      测试
                    </el-button>
                    <el-button
                      data-edhr-batch-record-test-history-button
                      :disabled="!isRowTestHistoryReady(row)"
                      link
                      :type="getRowTestHistoryButtonType(row)"
                      @click="openRowTestHistory(row)"
                    >
                      历史
                    </el-button>
                    <el-button
                      link
                      type="primary"
                      @click="openDescriptionEditor('frontlineProduction', row)"
                    >
                      修改
                    </el-button>
                    <el-button
                      link
                      type="danger"
                      @click="handleDeleteRow('frontlineProduction', row)"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </el-tab-pane>
        <el-tab-pane label="订单分配" name="orderAllocation">
          <UnifiedListTemplate
            class="edhr-batch-record-test-page__list-template"
            data-edhr-batch-record-test-order-allocation-list
            table-key="mes.pro.edhrBatchRecordTest.orderAllocation"
            :query-model="orderAllocationQueryParams"
            :filter-definitions="orderAllocationQuickFilterDefinitions"
            :quick-filter-state="orderAllocationQuickFilter.state"
            :selected-filter-definition="orderAllocationQuickFilter.selectedDefinition.value"
            :operator-options="orderAllocationQuickFilter.operatorOptions.value"
            :columns="orderAllocationColumns"
            :column-saving="orderAllocationColumnSaving"
            :show-column-settings="false"
            :show-column-reset="false"
            :single-line-toolbar="true"
            :total="filteredOrderAllocationRows.length"
            v-model:page="orderAllocationQueryParams.pageNo"
            v-model:limit="orderAllocationQueryParams.pageSize"
            @update:quick-filter-state="orderAllocationQuickFilter.updateState"
            @quick-filter-query="orderAllocationQuickFilter.applyQuickFilter"
            @column-change="saveOrderAllocationColumnConfig"
            @pagination="handleOrderAllocationPagination"
          >
            <template #actions>
              <el-form-item class="edhr-batch-record-test-page__tenant-filter" label="测试租户">
                <el-select v-model="selectedTenantId" class="!w-240px" placeholder="请选择测试租户">
                  <el-option
                    v-for="tenant in tenantOptions"
                    :key="tenant.id"
                    :label="tenant.name"
                    :value="tenant.id"
                  />
                </el-select>
              </el-form-item>
              <el-button
                v-hasPermi="['system:codex-test:execute']"
                class="edhr-batch-record-test-page__run-all-button"
                data-edhr-batch-record-test-run-all-button
                :disabled="
                  !selectedTenantId ||
                  testingRowCaseName !== undefined ||
                  testingTabListKey !== undefined
                "
                :icon="VideoPlay"
                :loading="testingTabListKey === 'orderAllocation'"
                type="success"
                @click="handleTestTab('orderAllocation')"
              >
                {{ getTabTestButtonText('orderAllocation') }}
              </el-button>
              <el-button type="primary" @click="openCreateRowDialog('orderAllocation')">
                新增
              </el-button>
            </template>

            <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
              <el-alert
                v-if="loadError"
                class="edhr-batch-record-test-page__alert"
                :title="loadError"
                type="error"
                :closable="false"
                show-icon
              />
              <el-table
                data-user-table-column-explicit
                data-user-table-key="mes.pro.edhrBatchRecordTest.orderAllocation"
                :data="pagedOrderAllocationRows"
                border
                row-key="id"
                :show-overflow-tooltip="true"
                stripe
                @header-dragend="handleOrderAllocationHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
                <el-table-column
                  v-if="isOrderAllocationColumnVisible('sort')"
                  label="序号"
                  prop="sort"
                  :width="getOrderAllocationColumnWidthString('sort', 80)"
                  v-bind="sortColumnAttrs('sort')"
                />
                <el-table-column
                  v-if="isOrderAllocationColumnVisible('title')"
                  label="任务"
                  prop="title"
                  :min-width="getOrderAllocationColumnMinWidthString('title', 220)"
                />
                <el-table-column
                  v-if="isOrderAllocationColumnVisible('description')"
                  class-name="edhr-batch-record-test-page__description-column"
                  label="描述"
                  prop="description"
                  :min-width="getOrderAllocationColumnMinWidthString('description', 320)"
                  :show-overflow-tooltip="false"
                />
                <el-table-column
                  v-if="isOrderAllocationColumnVisible('actions')"
                  fixed="right"
                  label="操作"
                  prop="actions"
                  :width="getOrderAllocationColumnWidthString('actions', 180)"
                >
                  <template #default="{ row }">
                    <el-button
                      v-hasPermi="['system:codex-test:execute']"
                      :disabled="
                        !selectedTenantId ||
                        testingRowCaseName !== undefined ||
                        testingTabListKey !== undefined
                      "
                      :loading="testingRowCaseName === row.caseName"
                      link
                      type="success"
                      @click="handleTestRow(row)"
                    >
                      测试
                    </el-button>
                    <el-button
                      data-edhr-batch-record-test-history-button
                      :disabled="!isRowTestHistoryReady(row)"
                      link
                      :type="getRowTestHistoryButtonType(row)"
                      @click="openRowTestHistory(row)"
                    >
                      历史
                    </el-button>
                    <el-button
                      link
                      type="primary"
                      @click="openDescriptionEditor('orderAllocation', row)"
                    >
                      修改
                    </el-button>
                    <el-button link type="danger" @click="handleDeleteRow('orderAllocation', row)">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </el-tab-pane>
        <el-tab-pane label="批记录映射" name="batchRecordMapping">
          <UnifiedListTemplate
            class="edhr-batch-record-test-page__list-template"
            data-edhr-batch-record-test-mapping-list
            table-key="mes.pro.edhrBatchRecordTest.batchRecordMapping"
            :query-model="batchRecordMappingQueryParams"
            :filter-definitions="batchRecordMappingQuickFilterDefinitions"
            :quick-filter-state="batchRecordMappingQuickFilter.state"
            :selected-filter-definition="batchRecordMappingQuickFilter.selectedDefinition.value"
            :operator-options="batchRecordMappingQuickFilter.operatorOptions.value"
            :columns="batchRecordMappingColumns"
            :column-saving="batchRecordMappingColumnSaving"
            :show-column-settings="false"
            :show-column-reset="false"
            :single-line-toolbar="true"
            :total="filteredBatchRecordMappingRows.length"
            v-model:page="batchRecordMappingQueryParams.pageNo"
            v-model:limit="batchRecordMappingQueryParams.pageSize"
            @update:quick-filter-state="batchRecordMappingQuickFilter.updateState"
            @quick-filter-query="batchRecordMappingQuickFilter.applyQuickFilter"
            @column-change="saveBatchRecordMappingColumnConfig"
            @pagination="handleBatchRecordMappingPagination"
          >
            <template #actions>
              <el-form-item class="edhr-batch-record-test-page__tenant-filter" label="测试租户">
                <el-select v-model="selectedTenantId" class="!w-240px" placeholder="请选择测试租户">
                  <el-option
                    v-for="tenant in tenantOptions"
                    :key="tenant.id"
                    :label="tenant.name"
                    :value="tenant.id"
                  />
                </el-select>
              </el-form-item>
              <el-button
                v-hasPermi="['system:codex-test:execute']"
                class="edhr-batch-record-test-page__run-all-button"
                data-edhr-batch-record-test-run-all-button
                :disabled="
                  !selectedTenantId ||
                  testingRowCaseName !== undefined ||
                  testingTabListKey !== undefined
                "
                :icon="VideoPlay"
                :loading="testingTabListKey === 'batchRecordMapping'"
                type="success"
                @click="handleTestTab('batchRecordMapping')"
              >
                {{ getTabTestButtonText('batchRecordMapping') }}
              </el-button>
              <el-button type="primary" @click="openCreateRowDialog('batchRecordMapping')">
                新增
              </el-button>
            </template>

            <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
              <el-alert
                v-if="loadError"
                class="edhr-batch-record-test-page__alert"
                :title="loadError"
                type="error"
                :closable="false"
                show-icon
              />
              <el-table
                data-user-table-column-explicit
                data-user-table-key="mes.pro.edhrBatchRecordTest.batchRecordMapping"
                :data="pagedBatchRecordMappingRows"
                border
                row-key="id"
                :show-overflow-tooltip="true"
                stripe
                @header-dragend="handleBatchRecordMappingHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
                <el-table-column
                  v-if="isBatchRecordMappingColumnVisible('sort')"
                  label="序号"
                  prop="sort"
                  :width="getBatchRecordMappingColumnWidthString('sort', 80)"
                  v-bind="sortColumnAttrs('sort')"
                />
                <el-table-column
                  v-if="isBatchRecordMappingColumnVisible('title')"
                  class-name="edhr-batch-record-test-page__mapping-title-column"
                  label="业务环节"
                  prop="title"
                  :min-width="getBatchRecordMappingColumnMinWidthString('title', 180)"
                  :show-overflow-tooltip="false"
                />
                <el-table-column
                  v-if="isBatchRecordMappingColumnVisible('description')"
                  class-name="edhr-batch-record-test-page__description-column"
                  label="业务说明"
                  prop="description"
                  :min-width="getBatchRecordMappingColumnMinWidthString('description', 280)"
                  :show-overflow-tooltip="false"
                />
                <el-table-column
                  v-if="isBatchRecordMappingColumnVisible('actions')"
                  label="操作"
                  prop="actions"
                  :width="getBatchRecordMappingColumnWidthString('actions', 220)"
                >
                  <template #default="{ row }">
                    <el-button
                      v-hasPermi="['system:codex-test:execute']"
                      :disabled="
                        !selectedTenantId ||
                        testingRowCaseName !== undefined ||
                        testingTabListKey !== undefined
                      "
                      :loading="testingRowCaseName === row.caseName"
                      link
                      type="success"
                      @click="handleTestRow(row)"
                    >
                      测试
                    </el-button>
                    <el-button
                      data-edhr-batch-record-test-history-button
                      :disabled="!isRowTestHistoryReady(row)"
                      link
                      :type="getRowTestHistoryButtonType(row)"
                      @click="openRowTestHistory(row)"
                    >
                      历史
                    </el-button>
                    <el-button
                      link
                      type="primary"
                      @click="openDescriptionEditor('batchRecordMapping', row)"
                    >
                      修改
                    </el-button>
                    <el-button
                      link
                      type="danger"
                      @click="handleDeleteRow('batchRecordMapping', row)"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </el-tab-pane>
      </el-tabs>
      <el-dialog
        v-model="createEditor.visible"
        title="新增测试任务"
        width="560px"
        data-edhr-batch-record-test-create-dialog
        destroy-on-close
      >
        <el-form label-width="80px">
          <el-form-item label="任务">
            <el-input
              v-model="createEditor.title"
              maxlength="80"
              show-word-limit
              placeholder="请输入任务名称"
            />
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="createEditor.description"
              type="textarea"
              :rows="5"
              maxlength="500"
              show-word-limit
              placeholder="请输入描述"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="closeCreateEditor">取消</el-button>
          <el-button type="primary" @click="saveCreatedRow">保存</el-button>
        </template>
      </el-dialog>
      <el-dialog
        v-model="descriptionEditor.visible"
        title="修改描述"
        width="560px"
        data-edhr-batch-record-test-description-dialog
        destroy-on-close
      >
        <el-form label-width="80px">
          <el-form-item label="任务">
            <span>{{ descriptionEditor.title }}</span>
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="descriptionEditor.description"
              type="textarea"
              :rows="5"
              maxlength="500"
              show-word-limit
              placeholder="请输入描述"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="closeDescriptionEditor">取消</el-button>
          <el-button :loading="descriptionSaving" type="primary" @click="saveDescriptionEdit">
            保存
          </el-button>
        </template>
      </el-dialog>
      <el-dialog
        v-model="testResult.visible"
        class="edhr-batch-record-test-page__result-dialog"
        title="测试结果"
        width="min(1120px, calc(100vw - 32px))"
        top="4vh"
        data-edhr-batch-record-test-result-dialog
        :close-on-click-modal="false"
      >
        <el-descriptions :column="2" border>
          <el-descriptions-item label="执行编号">
            {{ testResult.executionId ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="执行状态">
            <el-tag :type="getExecutionStatusTagType(testResult.data?.status)">
              {{ getExecutionStatusText(testResult.data?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="测试任务" :span="2">
            {{ testResult.rowTitle }}
          </el-descriptions-item>
          <el-descriptions-item v-if="testResult.data?.summary" label="执行摘要" :span="2">
            {{ testResult.data.summary }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="testResult.error"
          class="edhr-batch-record-test-page__result-alert"
          :title="testResult.error"
          type="error"
          :closable="false"
          show-icon
        />

        <div
          v-for="executionCase in testResult.data?.cases || []"
          :key="executionCase.id"
          class="edhr-batch-record-test-page__result-section"
        >
          <div class="edhr-batch-record-test-page__result-heading">
            <span>Codex CLI 回复</span>
            <el-tag :type="getExecutionStatusTagType(executionCase.status)">
              {{ getExecutionStatusText(executionCase.status) }}
            </el-tag>
          </div>
          <p
            v-if="executionCase.progressMessage"
            class="edhr-batch-record-test-page__result-progress"
          >
            {{ executionCase.progressMessage }}
          </p>
          <el-alert
            v-if="executionCase.failureReason"
            :title="executionCase.failureReason"
            type="error"
            :closable="false"
            show-icon
          />
          <el-table
            class="edhr-batch-record-test-page__result-table"
            :data="executionCase.checkpointResults"
            :show-overflow-tooltip="false"
            border
          >
            <el-table-column label="检查点" prop="checkpointNameSnapshot" min-width="120" />
            <el-table-column label="状态" width="80">
              <template #default="{ row: checkpoint }">
                <el-tag :type="getExecutionStatusTagType(checkpoint.status)">
                  {{ getExecutionStatusText(checkpoint.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="预期" prop="expectedTextSnapshot" min-width="160" />
            <el-table-column
              class-name="edhr-batch-record-test-page__actual-reply-column"
              label="实际回复"
              min-width="200"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row: checkpoint }">
                {{ checkpoint.actualText || '等待 Codex CLI 返回' }}
              </template>
            </el-table-column>
            <el-table-column
              class-name="edhr-batch-record-test-page__mismatch-description-column"
              label="不符合描述"
              min-width="260"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row: checkpoint }">
                {{ checkpoint.mismatchDescription || '-' }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-empty
          v-if="!testResult.error && !testResult.data?.cases?.length"
          :description="testResult.loading ? '正在等待 Codex CLI 回复' : '尚未收到执行结果'"
        />
        <template #footer>
          <el-button @click="testResult.visible = false">关闭</el-button>
        </template>
      </el-dialog>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { VideoPlay } from '@element-plus/icons-vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition,
  type UserTableColumnState
} from '@/hooks/web/useUserTableColumns'
import { getTenantId } from '@/utils/auth'
import * as CodexTestApi from '@/api/system/codexTestManagement'
import * as TenantApi from '@/api/system/tenant'

defineOptions({ name: 'MesProEdhrBatchRecordTest' })

type BatchRecordTestRow = {
  id: number
  sort: number
  title: string
  description: string
  caseName: string
  testScope: string
}

type BatchRecordTestRowHistory = {
  historyKey: string
  rowTitle: string
  executionId?: number
  loading: boolean
  ready: boolean
  error: string
  data?: CodexTestApi.CodexTestExecutionVO
}

type BatchRecordTestListKey =
  | 'productionLeader'
  | 'frontlinePqc'
  | 'frontlineProduction'
  | 'orderAllocation'
  | 'batchRecordMapping'

type BatchRecordTestDescriptionCache = {
  version: number
  descriptions: Record<string, string>
}

const BATCH_RECORD_TEST_DESCRIPTION_CACHE_VERSION = 1
const BATCH_RECORD_TEST_DESCRIPTION_CACHE_KEY_PREFIX = 'mes.pro.edhrBatchRecordTest.descriptions'

type PaginationPayload = {
  page?: number
  limit?: number
}

const batchRecordTestListMetas: Record<
  BatchRecordTestListKey,
  { casePrefix: string; testScopePrefix: string }
> = {
  productionLeader: {
    casePrefix: '批记录测试-生产组长',
    testScopePrefix: '生产组长职责'
  },
  frontlinePqc: {
    casePrefix: '批记录测试-一线PQC',
    testScopePrefix: '一线PQC'
  },
  frontlineProduction: {
    casePrefix: '批记录测试-一线生产',
    testScopePrefix: '一线生产'
  },
  orderAllocation: {
    casePrefix: '批记录测试-订单分配',
    testScopePrefix: '订单分配'
  },
  batchRecordMapping: {
    casePrefix: '批记录测试-批记录映射',
    testScopePrefix: '批记录映射'
  }
}

const message = useMessage()
const activeInnerTab = ref<BatchRecordTestListKey>('productionLeader')
const tenantOptions = ref<TenantApi.TenantVO[]>([])
const selectedTenantId = ref<number>()
const testingRowCaseName = ref<string>()
const testingTabListKey = ref<BatchRecordTestListKey>()
const testingTabProgress = reactive({ completed: 0, total: 0 })
const descriptionSaving = ref(false)
const terminalExecutionStatuses = new Set(['PASS', 'FAIL', 'BLOCKED', 'CANCELED', 'TIMEOUT'])
const resultPollIntervalMs = 1500
let resultPollTimer: ReturnType<typeof setTimeout> | undefined
let resultPollWaitResolve: (() => void) | undefined
let resultPollToken = 0
let testRunToken = 0
const rowTestHistories = reactive<Record<string, BatchRecordTestRowHistory>>({})
const testResult = reactive({
  visible: false,
  historyKey: '',
  executionId: undefined as number | undefined,
  rowTitle: '',
  loading: false,
  error: '',
  data: undefined as CodexTestApi.CodexTestExecutionVO | undefined
})
const tenantLoadError = ref('')
const testCaseLoadError = ref('')
const descriptionCacheError = ref('')
const loadError = computed(
  () => testCaseLoadError.value || tenantLoadError.value || descriptionCacheError.value
)
const createEditor = reactive({
  visible: false,
  listKey: 'productionLeader' as BatchRecordTestListKey,
  title: '',
  description: ''
})
const descriptionEditor = reactive({
  visible: false,
  listKey: 'productionLeader' as BatchRecordTestListKey,
  rowId: undefined as number | undefined,
  title: '',
  description: ''
})

const productionLeaderRows = ref<BatchRecordTestRow[]>([
  {
    id: 1,
    sort: 1,
    title: '工艺路线生产组长配置',
    description: '在工艺路线中配置生产组长，并关联到对应工序或“工序开始”节点。',
    caseName: '批记录测试-生产组长-01-工艺路线生产组长配置',
    testScope: '生产组长职责：工艺路线生产组长配置'
  },
  {
    id: 2,
    sort: 2,
    title: '批记录解析与工序配置',
    description:
      '从 QA 给的批记录文件解析批记录表单、工序、设备、参数、上下限，并为工序分配不良原因。',
    caseName: '批记录测试-生产组长-02-批记录解析与工序配置',
    testScope: '生产组长职责：批记录解析与工序配置'
  },
  {
    id: 3,
    sort: 3,
    title: '生产人员管理',
    description: '维护正式员工和临时工，可新增临时工、设置/修改临时工密码、启用/禁用员工。',
    caseName: '批记录测试-生产组长-03-生产人员管理',
    testScope: '生产组长职责：生产人员管理'
  },
  {
    id: 4,
    sort: 4,
    title: '报工分配与生产进度',
    description:
      '查看一线报工数据，将报工数量分配给一个或多个活跃订单；某订单某工序累计分配达到订单数量后更新生产进度。',
    caseName: '批记录测试-生产组长-04-报工分配与生产进度',
    testScope: '生产组长职责：报工分配与生产进度'
  },
  {
    id: 5,
    sort: 5,
    title: '活跃订单与检验进度',
    description:
      '将生产工单加入活跃订单列表；一线 PQC 提交活跃订单工序检验结果，PQC 组长确认后更新检验进度。',
    caseName: '批记录测试-生产组长-05-活跃订单与检验进度',
    testScope: '生产组长职责：活跃订单与检验进度'
  }
])

const frontlinePqcRows = ref<BatchRecordTestRow[]>([
  {
    id: 101,
    sort: 1,
    title: '活跃订单池选择订单',
    description:
      '一线PQC填写任务从所有生产组长维护的活跃订单池中选择订单，并以所选订单作为后续产品、工序和检验项目上下文。',
    caseName: '批记录测试-一线PQC-01-活跃订单池选择订单',
    testScope: '一线PQC：从所有生产组长维护的活跃订单池选择订单'
  },
  {
    id: 102,
    sort: 2,
    title: '按产品读取工艺路线工序',
    description:
      '根据订单对应产品读取工艺路线中的全部工序，点击工序卡片时展示可选择的完整工序列表。',
    caseName: '批记录测试-一线PQC-02-按产品读取工艺路线工序',
    testScope: '一线PQC：按订单产品读取工艺路线全部工序并通过工序卡片选择'
  },
  {
    id: 103,
    sort: 3,
    title: '按工序加载QA检验项',
    description:
      '选择工序后，从该产品对应QA检测项目列表中查找该工序的全部检验项，并展示在检验项tab中。',
    caseName: '批记录测试-一线PQC-03-按工序加载QA检验项',
    testScope: '一线PQC：按产品和工序从QA检测项目列表加载全部检验项'
  },
  {
    id: 104,
    sort: 4,
    title: '检验项名称与方法展示',
    description: '检验项tab必须显示检验项名称而不是编号，并在每个检验项tab中展示对应检验方法。',
    caseName: '批记录测试-一线PQC-04-检验项名称与方法展示',
    testScope: '一线PQC：检验项tab显示检验项名称和对应检验方法'
  },
  {
    id: 105,
    sort: 5,
    title: '首检检验数量读取',
    description: '选择首检时，根据产品+工序从QA检验项目读取首检数量，并将该数量显示为检验数量。',
    caseName: '批记录测试-一线PQC-05-首检检验数量读取',
    testScope: '一线PQC：首检按产品和工序读取QA检验项目首检数量'
  },
  {
    id: 106,
    sort: 6,
    title: '巡检抽样数量计算',
    description:
      '选择巡检时，根据产品+工序读取抽样率并按生产数量计算检验数量，例如生产10000、抽样率0.4时检验数量为40。',
    caseName: '批记录测试-一线PQC-06-巡检抽样数量计算',
    testScope: '一线PQC：巡检按QA抽样率和生产数量计算检验数量'
  },
  {
    id: 107,
    sort: 7,
    title: '电子密码提交',
    description: '点击提交时必须要求输入电子密码，确认通过后才允许提交检验结果。',
    caseName: '批记录测试-一线PQC-07-电子密码提交',
    testScope: '一线PQC：提交检验结果必须经过电子密码确认'
  },
  {
    id: 108,
    sort: 8,
    title: '提交进入PQC组长管理列表',
    description: '一线PQC提交确认后，检验数据必须添加到对应PQC组长的PQC管理列表等待后续确认。',
    caseName: '批记录测试-一线PQC-08-提交进入PQC组长管理列表',
    testScope: '一线PQC：提交后的检验数据进入对应PQC组长的PQC管理列表'
  }
])

const frontlineProductionRows = ref<BatchRecordTestRow[]>([
  {
    id: 201,
    sort: 1,
    title: '一线生产入口与组长身份',
    description: '生产组长使用自己的账号进入一线生产，页面运行态按该生产组长负责范围加载。',
    caseName: '批记录测试-一线生产-01-一线生产入口与组长身份',
    testScope: '一线生产：生产组长登录自己的账号并进入一线生产'
  },
  {
    id: 202,
    sort: 2,
    title: '负责工序卡片来源',
    description: '工序卡片只显示该生产组长在工序配置列表中负责的工序，不显示未负责工序。',
    caseName: '批记录测试-一线生产-02-负责工序卡片来源',
    testScope: '一线生产：工序卡片来源于生产组长工序配置列表中的负责工序'
  },
  {
    id: 203,
    sort: 3,
    title: '负责员工卡片来源',
    description: '员工卡片只显示该生产组长在人员管理下维护的启用员工。',
    caseName: '批记录测试-一线生产-03-负责员工卡片来源',
    testScope: '一线生产：员工卡片来源于生产组长人员管理下的员工'
  },
  {
    id: 204,
    sort: 4,
    title: '工序上下文数据联动',
    description: '选择工序和员工后，不良、设备和设备参数都必须来源于所选工序对应配置。',
    caseName: '批记录测试-一线生产-04-工序上下文数据联动',
    testScope: '一线生产：选择工序和员工后联动所选工序的不良、设备和设备参数'
  },
  {
    id: 205,
    sort: 5,
    title: '设备可选性',
    description: '不同工序可以有设备或无设备；无设备工序不得被缺设备阻断，有设备工序展示对应设备。',
    caseName: '批记录测试-一线生产-05-设备可选性',
    testScope: '一线生产：按工序配置区分有设备工序和无设备工序'
  },
  {
    id: 206,
    sort: 6,
    title: '设备参数可选性',
    description: '不同设备可以有设备参数或无参数；参数区域必须按所选工序设备配置展示。',
    caseName: '批记录测试-一线生产-06-设备参数可选性',
    testScope: '一线生产：按工序设备配置区分有参数设备和无参数设备'
  },
  {
    id: 207,
    sort: 7,
    title: '设备参数限制规则',
    description: '设备参数有上下限时按配置校验并留痕；没有上下限时不得强加限制。',
    caseName: '批记录测试-一线生产-07-设备参数限制规则',
    testScope: '一线生产：设备参数上下限限制来自生产组长负责工序配置'
  },
  {
    id: 208,
    sort: 8,
    title: '电子密码与待分配报工',
    description:
      '提交时必须输入所选员工的电子密码，而不是生产组长的电子密码；本地提交内容进入该组长的报工管理页签等待分配。',
    caseName: '批记录测试-一线生产-08-电子密码与待分配报工',
    testScope: '一线生产：使用所选员工电子密码提交并进入生产组长报工管理待分配'
  }
])

const orderAllocationRows = ref<BatchRecordTestRow[]>([
  {
    id: 301,
    sort: 1,
    title: '工序共享报工池与分配范围',
    description:
      '报工按工序进入共享报工池，不存在“当前订单”；该工序组长只能把合格可分配数量分配给其可见的活跃工单。现有活跃工单需求不足时不得拒绝报工，尚未分配数量仍归属于本次报工。',
    caseName: '批记录测试-订单分配-01-工序共享报工池与分配范围',
    testScope: '订单分配：工序共享报工池与分配范围'
  },
  {
    id: 302,
    sort: 2,
    title: 'FIFO固定顺序与部分分配',
    description:
      'FIFO严格按照活跃工单列表的固定全局顺序分配，列表位置靠前的工单先于靠后的工单。报工不足时允许先部分满足最靠前工单并保留该工单未完成需求；报工超额时依次满足现有工单，超出需求的未分配数量继续保留在本次报工池。',
    caseName: '批记录测试-订单分配-02-FIFO固定顺序与部分分配',
    testScope: '订单分配：FIFO固定顺序与部分分配'
  },
  {
    id: 303,
    sort: 3,
    title: 'FIFO草稿与空白手工分配',
    description:
      'FIFO只生成可调整的分配草稿，组长可以在草稿上修改、拆分或转移未放行数量，也可以从空白状态开始手工分配；手工调整不要求继续遵循 FIFO，但仍受工序范围、可分配数量和订单当前工序剩余需求约束。',
    caseName: '批记录测试-订单分配-03-FIFO草稿与空白手工分配',
    testScope: '订单分配：FIFO草稿与空白手工分配'
  },
  {
    id: 304,
    sort: 4,
    title: '未放行调整与已放行锁定',
    description:
      '未放行分配可以修改、删除、拆分或从 A 工单转移给加急 C 工单；已放行分配必须以绿色显示并锁定，不得修改、删除或转移。部分放行时只锁定已放行部分，剩余未放行部分仍可调整。',
    caseName: '批记录测试-订单分配-04-未放行调整与已放行锁定',
    testScope: '订单分配：未放行调整与已放行锁定'
  },
  {
    id: 305,
    sort: 5,
    title: '分配订单列与管理历史视图',
    description:
      '报工管理列表必须有“分配订单”列，展示每个工单及其分配数量，并将已放行项显示为绿色。报工提交后永久保留历史；只要存在未分配数量或未放行分配就留在报工管理，未分配数量为零且全部分配均已放行后从管理列表移除并可在报工历史查询。',
    caseName: '批记录测试-订单分配-05-分配订单列与管理历史视图',
    testScope: '订单分配：分配订单列与管理历史视图'
  },
  {
    id: 306,
    sort: 6,
    title: '合格可分配数量关系',
    description:
      '报工数量必须区分合格可分配数量与不合格、冻结或待检数量；只有合格数量可以进入订单分配。必须满足“合格可分配数量 = 已放行锁定数量 + 未放行预分配数量 + 尚未分配数量”，任何分配不得突破本次报工的合格可分配数量或目标工单当前工序剩余需求。',
    caseName: '批记录测试-订单分配-06-合格可分配数量关系',
    testScope: '订单分配：合格可分配数量关系'
  },
  {
    id: 307,
    sort: 7,
    title: '调整增减审计',
    description:
      '未放行数量在工单间调整时不得覆盖原归属，必须形成可追溯的增减记录，例如 A 工单 `-100`、C 工单 `+100`，并记录操作人、操作时间、原工单、目标工单和调整原因。',
    caseName: '批记录测试-订单分配-07-调整增减审计',
    testScope: '订单分配：调整增减审计'
  },
  {
    id: 308,
    sort: 8,
    title: '并发重校验与工单状态变化',
    description:
      '确认分配时必须在并发保护下重新校验报工可用数量和工单当前工序剩余需求，防止重复占用。工单减量、暂停或取消时，未放行数量退回原报工池；已放行数量不得自动退回，后续纠错必须走正式冲销流程。',
    caseName: '批记录测试-订单分配-08-并发重校验与工单状态变化',
    testScope: '订单分配：并发重校验与工单状态变化'
  }
])

const batchRecordMappingRows = ref<BatchRecordTestRow[]>([
  {
    id: 401,
    sort: 1,
    title: '放行申请条件',
    description:
      '生产和检验均已完成并经过组长确认后，才允许申请放行。生产完成情况和检验完成情况必须能够从历史记录和历史表单中追溯，不得通过直接调整进度绕过实际业务记录。',
    caseName: '批记录测试-批记录映射-01-放行申请条件',
    testScope: '批记录映射：放行申请条件'
  },
  {
    id: 402,
    sort: 2,
    title: '生产组长发起申请',
    description:
      '生产组长在活跃订单中发起放行申请，并可填写申请说明；非当前订单的责任生产组长不得代为申请。申请提交后由系统按正式流程准备资料和审批任务，页面不得提前显示资料已完成或已经放行。',
    caseName: '批记录测试-批记录映射-02-生产组长发起申请',
    testScope: '批记录映射：生产组长发起申请'
  },
  {
    id: 403,
    sort: 3,
    title: '申请依据复核',
    description:
      '收到申请后，系统必须重新核对订单、工单、产品、工艺路线、各道工序以及生产和检验记录，确认生产与检验确已完成，并锁定本次申请所依据的业务资料。',
    caseName: '批记录测试-批记录映射-03-申请依据复核',
    testScope: '批记录映射：申请依据复核'
  },
  {
    id: 404,
    sort: 4,
    title: '批次资料统一归档',
    description:
      '申请依据复核通过后，同一活跃订单的放行资料统一归入一份批次执行档案。已有档案时继续使用原档案，不得重复建立平行档案或形成两套放行资料。',
    caseName: '批记录测试-批记录映射-04-批次资料统一归档',
    testScope: '批记录映射：批次资料统一归档'
  },
  {
    id: 405,
    sort: 5,
    title: '工序批记录对应关系',
    description:
      '每道工序必须使用工序设置中正式绑定的批记录表单，不得用补充表单槽位或工序开始配置替代正式批记录表单。补充表单槽位只承载特殊或动态表单，工序开始配置只负责开始节点的上传和附件责任。',
    caseName: '批记录测试-批记录映射-05-工序批记录对应关系',
    testScope: '批记录映射：工序批记录对应关系'
  },
  {
    id: 406,
    sort: 6,
    title: '生产批记录归集',
    description:
      '正式生产批记录应归集实际生产提交、历史表单、设备使用、工艺参数和生产组长确认信息，并完整记录生产数量、设备、工艺参数、填写人、审核人和签名时间。',
    caseName: '批记录测试-批记录映射-06-生产批记录归集',
    testScope: '批记录映射：生产批记录归集'
  },
  {
    id: 407,
    sort: 7,
    title: '过程检验记录归集',
    description:
      '正式过程检验记录应归集检验提交、历史表单、质量标准、检验明细和检验组长复核信息，并完整记录检验项目、检验方法、质量标准、实测结果、判定、填写人、审核人和签名时间。',
    caseName: '批记录测试-批记录映射-07-过程检验记录归集',
    testScope: '批记录映射：过程检验记录归集'
  },
  {
    id: 408,
    sort: 8,
    title: '生产损耗记录归集',
    description:
      '正式生产损耗记录应归集生产损耗明细和生产组长确认信息，并完整记录损耗数量、损耗原因、所属工序、产品、批号、填写人、审核人和签名时间。没有损耗时，也必须按正式表单要求完成无损耗确认。',
    caseName: '批记录测试-批记录映射-08-生产损耗记录归集',
    testScope: '批记录映射：生产损耗记录归集'
  },
  {
    id: 409,
    sort: 9,
    title: '填写审核与签名追溯',
    description:
      '三类放行资料中的签名人员和签名时间必须来自实际填写、复核和确认记录，不得用当前登录人员或当前时间代替。每一处签名都应能够追溯到对应的业务记录。',
    caseName: '批记录测试-批记录映射-09-填写审核与签名追溯',
    testScope: '批记录映射：填写审核与签名追溯'
  },
  {
    id: 410,
    sort: 10,
    title: '放行资料形成顺序',
    description:
      '放行申请必须先建立批次档案，再依次形成生产批记录、过程检验记录和生产损耗记录。三类资料全部形成并完成检查后，才能进入生产负责人审批，不得先审批后补资料。',
    caseName: '批记录测试-批记录映射-10-放行资料形成顺序',
    testScope: '批记录映射：放行资料形成顺序'
  },
  {
    id: 411,
    sort: 11,
    title: '放行资料完整性检查',
    description:
      '进入审批前必须检查三类正式资料的必填内容、签名、审核、来源追溯、适用表单和负责人。任何资料缺失时均不得进入负责人审批，也不得用临时内容或虚假资料代替。',
    caseName: '批记录测试-批记录映射-11-放行资料完整性检查',
    testScope: '批记录映射：放行资料完整性检查'
  },
  {
    id: 412,
    sort: 12,
    title: '生产负责人审批',
    description:
      '三类放行资料完整并通过检查后，系统才向生产负责人发起审批。生产组长只负责提交申请，生产负责人在正式审批任务中完成放行或驳回。',
    caseName: '批记录测试-批记录映射-12-生产负责人审批',
    testScope: '批记录映射：生产负责人审批'
  },
  {
    id: 413,
    sort: 13,
    title: '重复申请处理',
    description:
      '同一申请重复提交时沿用原处理结果；仅当申请依据发生正式变化时才允许重新处理。系统不得重复建立批次档案、重复生成放行资料或重复创建审批任务。',
    caseName: '批记录测试-批记录映射-13-重复申请处理',
    testScope: '批记录映射：重复申请处理'
  },
  {
    id: 414,
    sort: 14,
    title: '缺失资料处理',
    description:
      '缺少正式批记录表单、历史表单、设备参数、质量标准、检验明细、损耗资料、签名或负责人时，系统必须明确告知缺少的资料、不能继续的原因和处理建议，并停止生成审批任务。',
    caseName: '批记录测试-批记录映射-14-缺失资料处理',
    testScope: '批记录映射：缺失资料处理'
  },
  {
    id: 415,
    sort: 15,
    title: '全流程业务验证',
    description:
      '使用可追溯的真实生产和检验记录完成生产、检验、组长确认、放行申请和负责人审批。生产组长发起申请、生产负责人审批以及最终资料核验均通过实际业务页面完成，并确认三类资料、签名、审批任务和放行结果一致。',
    caseName: '批记录测试-批记录映射-15-全流程业务验证',
    testScope: '批记录映射：全流程业务验证'
  }
])

const defaultBatchRecordTestRows = captureDefaultBatchRecordTestRows()
hydrateBatchRecordTestDescriptionCache()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: ''
})

const frontlinePqcQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: ''
})

const frontlineProductionQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: ''
})

const orderAllocationQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: ''
})

const batchRecordMappingQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: ''
})

const productionLeaderDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sort', label: '序号', width: 80 },
  { key: 'title', label: '职责', minWidth: 220 },
  { key: 'description', label: '描述', minWidth: 320, sortable: false },
  { key: 'actions', label: '操作', width: 180, hideable: false, business: false, sortable: false }
]

const productionLeaderColumnControl = useUserTableColumns(
  'mes.pro.edhrBatchRecordTest.productionLeader',
  productionLeaderDefaultColumns
)
const productionLeaderColumns = computed(() => productionLeaderColumnControl.columns.value)
const productionLeaderColumnSaving = computed(() => productionLeaderColumnControl.saving.value)
const isProductionLeaderColumnVisible = (key: string) =>
  productionLeaderColumnControl.isColumnVisible(key)
const getProductionLeaderColumnWidthString = (key: string, fallback?: number) =>
  productionLeaderColumnControl.getColumnWidthString(key, fallback)
const getProductionLeaderColumnMinWidthString = (key: string, fallback?: number) =>
  productionLeaderColumnControl.getColumnMinWidthString(key, fallback)
const handleProductionLeaderHeaderDragend = async (
  newWidth: number,
  oldWidth: number,
  column: any
) => {
  await productionLeaderColumnControl.handleHeaderDragend(newWidth, oldWidth, column)
}
const saveProductionLeaderColumnConfig = async (columns: UserTableColumnState[]) => {
  await productionLeaderColumnControl.saveConfig(columns)
}

const frontlinePqcDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sort', label: '序号', width: 80 },
  { key: 'title', label: '任务', minWidth: 220 },
  { key: 'description', label: '描述', minWidth: 320, sortable: false },
  { key: 'actions', label: '操作', width: 180, hideable: false, business: false, sortable: false }
]

const frontlinePqcColumnControl = useUserTableColumns(
  'mes.pro.edhrBatchRecordTest.frontlinePqc',
  frontlinePqcDefaultColumns
)
const frontlinePqcColumns = computed(() => frontlinePqcColumnControl.columns.value)
const frontlinePqcColumnSaving = computed(() => frontlinePqcColumnControl.saving.value)
const isFrontlinePqcColumnVisible = (key: string) => frontlinePqcColumnControl.isColumnVisible(key)
const getFrontlinePqcColumnWidthString = (key: string, fallback?: number) =>
  frontlinePqcColumnControl.getColumnWidthString(key, fallback)
const getFrontlinePqcColumnMinWidthString = (key: string, fallback?: number) =>
  frontlinePqcColumnControl.getColumnMinWidthString(key, fallback)
const handleFrontlinePqcHeaderDragend = async (newWidth: number, oldWidth: number, column: any) => {
  await frontlinePqcColumnControl.handleHeaderDragend(newWidth, oldWidth, column)
}
const saveFrontlinePqcColumnConfig = async (columns: UserTableColumnState[]) => {
  await frontlinePqcColumnControl.saveConfig(columns)
}

const frontlineProductionColumnControl = useUserTableColumns(
  'mes.pro.edhrBatchRecordTest.frontlineProduction',
  frontlinePqcDefaultColumns
)
const frontlineProductionColumns = computed(() => frontlineProductionColumnControl.columns.value)
const frontlineProductionColumnSaving = computed(
  () => frontlineProductionColumnControl.saving.value
)
const isFrontlineProductionColumnVisible = (key: string) =>
  frontlineProductionColumnControl.isColumnVisible(key)
const getFrontlineProductionColumnWidthString = (key: string, fallback?: number) =>
  frontlineProductionColumnControl.getColumnWidthString(key, fallback)
const getFrontlineProductionColumnMinWidthString = (key: string, fallback?: number) =>
  frontlineProductionColumnControl.getColumnMinWidthString(key, fallback)
const handleFrontlineProductionHeaderDragend = async (
  newWidth: number,
  oldWidth: number,
  column: any
) => {
  await frontlineProductionColumnControl.handleHeaderDragend(newWidth, oldWidth, column)
}
const saveFrontlineProductionColumnConfig = async (columns: UserTableColumnState[]) => {
  await frontlineProductionColumnControl.saveConfig(columns)
}

const orderAllocationColumnControl = useUserTableColumns(
  'mes.pro.edhrBatchRecordTest.orderAllocation',
  frontlinePqcDefaultColumns
)
const orderAllocationColumns = computed(() => orderAllocationColumnControl.columns.value)
const orderAllocationColumnSaving = computed(() => orderAllocationColumnControl.saving.value)
const isOrderAllocationColumnVisible = (key: string) =>
  orderAllocationColumnControl.isColumnVisible(key)
const getOrderAllocationColumnWidthString = (key: string, fallback?: number) =>
  orderAllocationColumnControl.getColumnWidthString(key, fallback)
const getOrderAllocationColumnMinWidthString = (key: string, fallback?: number) =>
  orderAllocationColumnControl.getColumnMinWidthString(key, fallback)
const handleOrderAllocationHeaderDragend = async (
  newWidth: number,
  oldWidth: number,
  column: any
) => {
  await orderAllocationColumnControl.handleHeaderDragend(newWidth, oldWidth, column)
}
const saveOrderAllocationColumnConfig = async (columns: UserTableColumnState[]) => {
  await orderAllocationColumnControl.saveConfig(columns)
}

const batchRecordMappingDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sort', label: '序号', width: 80 },
  { key: 'title', label: '业务环节', minWidth: 180 },
  { key: 'description', label: '业务说明', minWidth: 280, sortable: false },
  { key: 'actions', label: '操作', width: 220, hideable: false, business: false, sortable: false }
]

const batchRecordMappingColumnControl = useUserTableColumns(
  'mes.pro.edhrBatchRecordTest.batchRecordMapping',
  batchRecordMappingDefaultColumns
)
const batchRecordMappingColumns = computed(() => batchRecordMappingColumnControl.columns.value)
const batchRecordMappingColumnSaving = computed(() => batchRecordMappingColumnControl.saving.value)
const isBatchRecordMappingColumnVisible = (key: string) =>
  batchRecordMappingColumnControl.isColumnVisible(key)
const getBatchRecordMappingColumnWidthString = (key: string, fallback?: number) =>
  batchRecordMappingColumnControl.getColumnWidthString(key, fallback)
const getBatchRecordMappingColumnMinWidthString = (key: string, fallback?: number) =>
  batchRecordMappingColumnControl.getColumnMinWidthString(key, fallback)
const handleBatchRecordMappingHeaderDragend = async (
  newWidth: number,
  oldWidth: number,
  column: any
) => {
  await batchRecordMappingColumnControl.handleHeaderDragend(newWidth, oldWidth, column)
}
const saveBatchRecordMappingColumnConfig = async (columns: UserTableColumnState[]) => {
  await batchRecordMappingColumnControl.saveConfig(columns)
}

const productionLeaderQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'keyword',
    label: '职责/描述',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '输入职责或描述关键字'
  }
])

const productionLeaderQuickFilter = useTableQuickFilter(
  'mes.pro.edhrBatchRecordTest.productionLeader',
  productionLeaderQuickFilterDefinitions,
  queryParams,
  applyProductionLeaderListFilters
)

const frontlinePqcQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'keyword',
    label: '任务/描述',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '输入任务或描述关键字'
  }
])

const frontlinePqcQuickFilter = useTableQuickFilter(
  'mes.pro.edhrBatchRecordTest.frontlinePqc',
  frontlinePqcQuickFilterDefinitions,
  frontlinePqcQueryParams,
  applyFrontlinePqcListFilters
)

const frontlineProductionQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'keyword',
    label: '任务/描述',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '输入任务或描述关键字'
  }
])

const frontlineProductionQuickFilter = useTableQuickFilter(
  'mes.pro.edhrBatchRecordTest.frontlineProduction',
  frontlineProductionQuickFilterDefinitions,
  frontlineProductionQueryParams,
  applyFrontlineProductionListFilters
)

const orderAllocationQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'keyword',
    label: '任务/描述',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '输入任务或描述关键字'
  }
])

const orderAllocationQuickFilter = useTableQuickFilter(
  'mes.pro.edhrBatchRecordTest.orderAllocation',
  orderAllocationQuickFilterDefinitions,
  orderAllocationQueryParams,
  applyOrderAllocationListFilters
)

const batchRecordMappingQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'keyword',
    label: '映射项/描述',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '输入映射项或描述关键字'
  }
])

const batchRecordMappingQuickFilter = useTableQuickFilter(
  'mes.pro.edhrBatchRecordTest.batchRecordMapping',
  batchRecordMappingQuickFilterDefinitions,
  batchRecordMappingQueryParams,
  applyBatchRecordMappingListFilters
)

const filteredProductionLeaderRows = computed(() => {
  const keyword = queryParams.keyword.trim()
  return filterBatchRecordTestRows(productionLeaderRows.value, keyword)
})

const filteredFrontlinePqcRows = computed(() => {
  const keyword = frontlinePqcQueryParams.keyword.trim()
  return filterBatchRecordTestRows(frontlinePqcRows.value, keyword)
})

const filteredFrontlineProductionRows = computed(() => {
  const keyword = frontlineProductionQueryParams.keyword.trim()
  return filterBatchRecordTestRows(frontlineProductionRows.value, keyword)
})

const filteredOrderAllocationRows = computed(() => {
  const keyword = orderAllocationQueryParams.keyword.trim()
  return filterBatchRecordTestRows(orderAllocationRows.value, keyword)
})

const filteredBatchRecordMappingRows = computed(() => {
  const keyword = batchRecordMappingQueryParams.keyword.trim()
  return filterBatchRecordTestRows(batchRecordMappingRows.value, keyword)
})

const pagedProductionLeaderRows = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredProductionLeaderRows.value.slice(start, start + queryParams.pageSize)
})

const pagedFrontlinePqcRows = computed(() => {
  const start = (frontlinePqcQueryParams.pageNo - 1) * frontlinePqcQueryParams.pageSize
  return filteredFrontlinePqcRows.value.slice(start, start + frontlinePqcQueryParams.pageSize)
})

const pagedFrontlineProductionRows = computed(() => {
  const start =
    (frontlineProductionQueryParams.pageNo - 1) * frontlineProductionQueryParams.pageSize
  return filteredFrontlineProductionRows.value.slice(
    start,
    start + frontlineProductionQueryParams.pageSize
  )
})

const pagedOrderAllocationRows = computed(() => {
  const start = (orderAllocationQueryParams.pageNo - 1) * orderAllocationQueryParams.pageSize
  return filteredOrderAllocationRows.value.slice(start, start + orderAllocationQueryParams.pageSize)
})

const pagedBatchRecordMappingRows = computed(() => {
  const start = (batchRecordMappingQueryParams.pageNo - 1) * batchRecordMappingQueryParams.pageSize
  return filteredBatchRecordMappingRows.value.slice(
    start,
    start + batchRecordMappingQueryParams.pageSize
  )
})

async function applyProductionLeaderListFilters() {
  queryParams.pageNo = 1
}

async function applyFrontlinePqcListFilters() {
  frontlinePqcQueryParams.pageNo = 1
}

async function applyFrontlineProductionListFilters() {
  frontlineProductionQueryParams.pageNo = 1
}

async function applyOrderAllocationListFilters() {
  orderAllocationQueryParams.pageNo = 1
}

async function applyBatchRecordMappingListFilters() {
  batchRecordMappingQueryParams.pageNo = 1
}

async function handleProductionLeaderPagination(payload?: PaginationPayload) {
  if (typeof payload?.page === 'number') queryParams.pageNo = payload.page
  if (typeof payload?.limit === 'number') queryParams.pageSize = payload.limit
}

async function handleFrontlinePqcPagination(payload?: PaginationPayload) {
  if (typeof payload?.page === 'number') frontlinePqcQueryParams.pageNo = payload.page
  if (typeof payload?.limit === 'number') frontlinePqcQueryParams.pageSize = payload.limit
}

async function handleFrontlineProductionPagination(payload?: PaginationPayload) {
  if (typeof payload?.page === 'number') frontlineProductionQueryParams.pageNo = payload.page
  if (typeof payload?.limit === 'number') frontlineProductionQueryParams.pageSize = payload.limit
}

async function handleOrderAllocationPagination(payload?: PaginationPayload) {
  if (typeof payload?.page === 'number') orderAllocationQueryParams.pageNo = payload.page
  if (typeof payload?.limit === 'number') orderAllocationQueryParams.pageSize = payload.limit
}

async function handleBatchRecordMappingPagination(payload?: PaginationPayload) {
  if (typeof payload?.page === 'number') batchRecordMappingQueryParams.pageNo = payload.page
  if (typeof payload?.limit === 'number') batchRecordMappingQueryParams.pageSize = payload.limit
}

function filterBatchRecordTestRows(rows: BatchRecordTestRow[], keyword: string) {
  if (!keyword) return rows
  return rows.filter((row) =>
    [row.title, row.description, row.testScope].some((text) => text.includes(keyword))
  )
}

function getBatchRecordTestRowsRef(listKey: BatchRecordTestListKey) {
  if (listKey === 'productionLeader') return productionLeaderRows
  if (listKey === 'frontlinePqc') return frontlinePqcRows
  if (listKey === 'frontlineProduction') return frontlineProductionRows
  if (listKey === 'orderAllocation') return orderAllocationRows
  if (listKey === 'batchRecordMapping') return batchRecordMappingRows
  throw new Error(`未知批记录测试列表：${listKey}`)
}

function cloneBatchRecordTestRows(rows: BatchRecordTestRow[]) {
  return rows.map((row) => ({ ...row }))
}

function captureDefaultBatchRecordTestRows(): Record<BatchRecordTestListKey, BatchRecordTestRow[]> {
  return {
    productionLeader: cloneBatchRecordTestRows(productionLeaderRows.value),
    frontlinePqc: cloneBatchRecordTestRows(frontlinePqcRows.value),
    frontlineProduction: cloneBatchRecordTestRows(frontlineProductionRows.value),
    orderAllocation: cloneBatchRecordTestRows(orderAllocationRows.value),
    batchRecordMapping: cloneBatchRecordTestRows(batchRecordMappingRows.value)
  }
}

function getDefaultBatchRecordTestRows(listKey: BatchRecordTestListKey) {
  return cloneBatchRecordTestRows(defaultBatchRecordTestRows[listKey])
}

function getBatchRecordTestDescriptionCacheKey() {
  const tenantId = Number(getTenantId())
  if (!Number.isSafeInteger(tenantId) || tenantId <= 0) {
    throw new Error('缺少当前登录租户，无法读写批记录测试描述缓存')
  }
  return `${BATCH_RECORD_TEST_DESCRIPTION_CACHE_KEY_PREFIX}.v${BATCH_RECORD_TEST_DESCRIPTION_CACHE_VERSION}.tenant.${tenantId}`
}

function readBatchRecordTestDescriptionCache(): Record<string, string> {
  const rawCache = window.localStorage.getItem(getBatchRecordTestDescriptionCacheKey())
  if (!rawCache) return {}
  const parsedCache = JSON.parse(rawCache) as Partial<BatchRecordTestDescriptionCache>
  if (
    parsedCache.version !== BATCH_RECORD_TEST_DESCRIPTION_CACHE_VERSION ||
    !parsedCache.descriptions ||
    typeof parsedCache.descriptions !== 'object' ||
    Array.isArray(parsedCache.descriptions)
  ) {
    throw new Error('批记录测试描述缓存格式无效')
  }
  const descriptions: Record<string, string> = {}
  for (const [caseName, description] of Object.entries(parsedCache.descriptions)) {
    if (typeof description !== 'string' || !description.trim()) {
      throw new Error(`批记录测试描述缓存内容无效：${caseName}`)
    }
    descriptions[caseName] = description.trim()
  }
  return descriptions
}

function syncBatchRecordTestDescriptionCache() {
  const descriptions: Record<string, string> = {}
  for (const listKey of Object.keys(batchRecordTestListMetas) as BatchRecordTestListKey[]) {
    const currentRows = getBatchRecordTestRowsRef(listKey).value
    for (const defaultRow of defaultBatchRecordTestRows[listKey]) {
      const currentRow = currentRows.find((row) => row.caseName === defaultRow.caseName)
      if (!currentRow?.description.trim()) {
        throw new Error(`批记录测试描述缓存缺少当前行：${defaultRow.caseName}`)
      }
      descriptions[currentRow.caseName] = currentRow.description.trim()
    }
  }
  const cache: BatchRecordTestDescriptionCache = {
    version: BATCH_RECORD_TEST_DESCRIPTION_CACHE_VERSION,
    descriptions
  }
  window.localStorage.setItem(getBatchRecordTestDescriptionCacheKey(), JSON.stringify(cache))
  descriptionCacheError.value = ''
}

function hydrateBatchRecordTestDescriptionCache() {
  try {
    const cachedDescriptions = readBatchRecordTestDescriptionCache()
    for (const listKey of Object.keys(batchRecordTestListMetas) as BatchRecordTestListKey[]) {
      updateBatchRecordTestRows(listKey, (rows) =>
        rows.map((row) => {
          const cachedDescription = cachedDescriptions[row.caseName]
          return cachedDescription ? { ...row, description: cachedDescription } : row
        })
      )
    }
    descriptionCacheError.value = ''
  } catch (error) {
    descriptionCacheError.value = `描述缓存读取失败：${getRequestErrorMessage(
      error,
      '未知缓存错误'
    )}`
  }
}

function getBatchRecordTestQueryParams(listKey: BatchRecordTestListKey) {
  if (listKey === 'productionLeader') return queryParams
  if (listKey === 'frontlinePqc') return frontlinePqcQueryParams
  if (listKey === 'frontlineProduction') return frontlineProductionQueryParams
  if (listKey === 'orderAllocation') return orderAllocationQueryParams
  if (listKey === 'batchRecordMapping') return batchRecordMappingQueryParams
  throw new Error(`未知批记录测试列表：${listKey}`)
}

function updateBatchRecordTestRows(
  listKey: BatchRecordTestListKey,
  updater: (rows: BatchRecordTestRow[]) => BatchRecordTestRow[]
) {
  const rowsRef = getBatchRecordTestRowsRef(listKey)
  rowsRef.value = updater(rowsRef.value)
}

function padBatchRecordTestSort(sort: number) {
  return String(sort).padStart(2, '0')
}

function openCreateRowDialog(listKey: BatchRecordTestListKey) {
  createEditor.listKey = listKey
  createEditor.title = ''
  createEditor.description = ''
  createEditor.visible = true
}

function closeCreateEditor() {
  createEditor.visible = false
  createEditor.title = ''
  createEditor.description = ''
}

function buildCreatedBatchRecordTestRow(
  listKey: BatchRecordTestListKey,
  rows: BatchRecordTestRow[],
  title: string,
  description: string
): BatchRecordTestRow {
  const sort = rows.reduce((max, row) => Math.max(max, row.sort), 0) + 1
  const id = rows.reduce((max, row) => Math.max(max, row.id), 0) + 1
  const meta = batchRecordTestListMetas[listKey]
  return {
    id,
    sort,
    title,
    description,
    caseName: `${meta.casePrefix}-${padBatchRecordTestSort(sort)}-${title}`,
    testScope: `${meta.testScopePrefix}：${title}`
  }
}

function saveCreatedRow() {
  const title = createEditor.title.trim()
  const description = createEditor.description.trim()
  if (!title) {
    message.error('任务不能为空')
    return
  }
  if (!description) {
    message.error('描述不能为空')
    return
  }
  let nextTotal = 0
  updateBatchRecordTestRows(createEditor.listKey, (rows) => {
    const nextRows = [
      ...rows,
      buildCreatedBatchRecordTestRow(createEditor.listKey, rows, title, description)
    ]
    nextTotal = nextRows.length
    return nextRows
  })
  const params = getBatchRecordTestQueryParams(createEditor.listKey)
  params.pageNo = Math.max(1, Math.ceil(nextTotal / params.pageSize))
  closeCreateEditor()
  message.success('已新增')
}

function openDescriptionEditor(listKey: BatchRecordTestListKey, row: BatchRecordTestRow) {
  descriptionEditor.visible = true
  descriptionEditor.listKey = listKey
  descriptionEditor.rowId = row.id
  descriptionEditor.title = row.title
  descriptionEditor.description = row.description
}

function closeDescriptionEditor() {
  descriptionEditor.visible = false
  descriptionEditor.rowId = undefined
  descriptionEditor.title = ''
  descriptionEditor.description = ''
}

async function saveDescriptionEdit() {
  if (descriptionEditor.rowId == null) return
  const editingListKey = descriptionEditor.listKey
  const editingRowId = descriptionEditor.rowId
  const nextDescription = descriptionEditor.description.trim()
  if (!nextDescription) {
    message.error('描述不能为空')
    return
  }
  const currentRow = getBatchRecordTestRowsRef(editingListKey).value.find(
    (row) => row.id === editingRowId
  )
  if (!currentRow) {
    message.error('待修改的测试任务不存在')
    return
  }
  const updatedRow = { ...currentRow, description: nextDescription }
  descriptionSaving.value = true
  try {
    await upsertCodeReadonlyCase(updatedRow)
    discardRowTestHistory(getRowTestHistoryKey(updatedRow))
    updateBatchRecordTestRows(editingListKey, (rows) =>
      rows.map((row) => (row.id === editingRowId ? { ...row, description: nextDescription } : row))
    )
    try {
      syncBatchRecordTestDescriptionCache()
    } catch (error) {
      descriptionCacheError.value = `描述已保存，但本地缓存更新失败：${getRequestErrorMessage(
        error,
        '未知缓存错误'
      )}`
      if (
        descriptionEditor.listKey === editingListKey &&
        descriptionEditor.rowId === editingRowId
      ) {
        closeDescriptionEditor()
      }
      message.error(descriptionCacheError.value)
      return
    }
    if (descriptionEditor.listKey === editingListKey && descriptionEditor.rowId === editingRowId) {
      closeDescriptionEditor()
    }
    message.success('描述已修改')
  } catch (error) {
    showRequestError(error, '描述保存失败')
  } finally {
    descriptionSaving.value = false
  }
}

async function handleDeleteRow(listKey: BatchRecordTestListKey, row: BatchRecordTestRow) {
  const historyKey = getRowTestHistoryKey(row)
  if (testingRowCaseName.value === historyKey) {
    message.error('当前测试正在执行，不能删除该行')
    return
  }
  await message.confirm(`确认删除“${row.title}”这行测试任务吗？`, '删除确认')
  updateBatchRecordTestRows(listKey, (rows) => rows.filter((item) => item.id !== row.id))
  discardRowTestHistory(historyKey)
  message.success('行已删除')
}

function showRequestError(error: unknown, defaultMessage: string) {
  const text =
    error instanceof Error ? error.message : typeof error === 'string' ? error : defaultMessage
  message.error(text || defaultMessage)
}

async function getTenantOptions() {
  try {
    tenantLoadError.value = ''
    tenantOptions.value = await TenantApi.getTenantList()
    selectedTenantId.value = tenantOptions.value[0]?.id
  } catch (error) {
    tenantLoadError.value = error instanceof Error ? error.message : '测试租户加载失败'
    showRequestError(error, '测试租户加载失败')
  }
}

function buildCodeReadonlyCasePayload(
  definition: BatchRecordTestRow
): CodexTestApi.CodexTestCaseVO & CodexTestApi.CodexTestCodeReadonlyCaseReqVO {
  return {
    name: definition.caseName,
    project: '批记录',
    methodText: '只读扫描当前代码，分析是否已经完整支持' + definition.testScope,
    testDataText: '测试范围：' + definition.testScope + '。描述：' + definition.description,
    analysisMode: 'CODE_READONLY',
    defaultExecutionMode: 'SEQUENTIAL',
    parallelSafe: false,
    status: 'ENABLE',
    sort: definition.sort,
    checkpoints: [
      {
        sort: 1,
        name: definition.title,
        remark: definition.description,
        expectedText:
          '当前代码、路由、API、权限、数据模型和测试能够满足' +
          definition.testScope +
          '：' +
          definition.description,
        severity: 'MAJOR'
      }
    ]
  }
}

async function upsertCodeReadonlyCase(definition: BatchRecordTestRow) {
  const pageResult = await CodexTestApi.getCodexTestCasePage({
    pageNo: 1,
    pageSize: 10,
    project: '批记录',
    name: definition.caseName
  })
  const existingCase = pageResult.list.find(
    (item) => item.name === definition.caseName && item.project === '批记录'
  )
  const casePayload = buildCodeReadonlyCasePayload(definition)
  if (existingCase?.id) {
    await CodexTestApi.updateCodexTestCase({ id: existingCase.id, ...casePayload })
    return existingCase.id
  }
  return await CodexTestApi.createCodexTestCase(casePayload)
}

async function loadPersistedBatchRecordTestRows() {
  try {
    testCaseLoadError.value = ''
    const pageResult = await CodexTestApi.getCodexTestCasePage({
      pageNo: 1,
      pageSize: 100,
      project: '批记录'
    })
    const persistedCasesByName = new Map<string, CodexTestApi.CodexTestCaseVO>()
    for (const persistedCase of pageResult.list) {
      if (!persistedCase.name.startsWith('批记录测试-')) continue
      if (persistedCasesByName.has(persistedCase.name)) {
        throw new Error(`批记录测试项名称重复：${persistedCase.name}`)
      }
      persistedCasesByName.set(persistedCase.name, persistedCase)
    }
    for (const listKey of Object.keys(batchRecordTestListMetas) as BatchRecordTestListKey[]) {
      updateBatchRecordTestRows(listKey, () =>
        getDefaultBatchRecordTestRows(listKey).map((row) => {
          const persistedCase = persistedCasesByName.get(row.caseName)
          if (!persistedCase) return row
          const checkpoint = persistedCase?.checkpoints.find((item) => item.sort === 1)
          if (!checkpoint || !checkpoint.remark?.trim()) {
            throw new Error(`批记录测试项缺少结构化描述：${row.caseName}`)
          }
          return { ...row, description: checkpoint.remark.trim() }
        })
      )
    }
  } catch (error) {
    testCaseLoadError.value = error instanceof Error ? error.message : '批记录测试任务加载失败'
    for (const listKey of Object.keys(batchRecordTestListMetas) as BatchRecordTestListKey[]) {
      updateBatchRecordTestRows(listKey, () => [])
    }
    showRequestError(error, '批记录测试任务加载失败')
    return
  }
  try {
    syncBatchRecordTestDescriptionCache()
  } catch (error) {
    descriptionCacheError.value = `正式描述已加载，但本地缓存更新失败：${getRequestErrorMessage(
      error,
      '未知缓存错误'
    )}`
    message.error(descriptionCacheError.value)
  }
}

function getRowTestHistoryKey(row: BatchRecordTestRow) {
  return row.caseName
}

function resetTestResultDialog() {
  testResult.visible = false
  testResult.historyKey = ''
  testResult.executionId = undefined
  testResult.rowTitle = ''
  testResult.loading = false
  testResult.error = ''
  testResult.data = undefined
}

function discardRowTestHistory(historyKey: string) {
  delete rowTestHistories[historyKey]
  if (testResult.historyKey === historyKey) resetTestResultDialog()
}

function clearRowTestHistory(historyKey: string, rowTitle: string) {
  discardRowTestHistory(historyKey)
  rowTestHistories[historyKey] = {
    historyKey,
    rowTitle,
    executionId: undefined,
    loading: true,
    ready: false,
    error: '',
    data: undefined
  }
}

function isRowTestHistoryReady(row: BatchRecordTestRow) {
  const history = rowTestHistories[getRowTestHistoryKey(row)]
  return Boolean(history?.ready && history.executionId != null && history.data)
}

function getRowTestHistoryButtonType(row: BatchRecordTestRow) {
  const history = rowTestHistories[getRowTestHistoryKey(row)]
  if (!history?.ready || !history.data) return 'info'
  return getExecutionStatusTagType(history.data.status)
}

function openRowTestHistory(row: BatchRecordTestRow) {
  if (!isRowTestHistoryReady(row)) return
  const history = rowTestHistories[getRowTestHistoryKey(row)]
  if (!history?.data || history.executionId == null) return
  testResult.historyKey = history.historyKey
  testResult.executionId = history.executionId
  testResult.rowTitle = history.rowTitle
  testResult.loading = false
  testResult.error = ''
  testResult.data = history.data
  testResult.visible = true
}

type BatchRecordTestInvocationSource = 'single' | 'tab'

async function handleTestRow(row: BatchRecordTestRow, source: BatchRecordTestInvocationSource = 'single'): Promise<CodexTestApi.CodexTestExecutionVO | undefined> {
  if (!selectedTenantId.value) {
    message.error('请选择测试租户')
    return
  }
  if (testingTabListKey.value !== undefined && source === 'single') {
    message.warning('已有批量测试正在执行')
    return
  }
  if (testingRowCaseName.value !== undefined) {
    message.warning('已有测试正在执行')
    return
  }
  const historyKey = getRowTestHistoryKey(row)
  const runToken = ++testRunToken
  stopResultPolling()
  clearRowTestHistory(historyKey, row.title)
  testingRowCaseName.value = historyKey
  try {
    const executionId = await CodexTestApi.startCodeReadonlyCodexTestExecution({
      targetTenantId: selectedTenantId.value,
      caseDefinition: buildCodeReadonlyCasePayload(row)
    })
    if (runToken !== testRunToken) return
    const history = rowTestHistories[historyKey]
    if (!history || history.historyKey !== historyKey) return
    history.executionId = executionId
    if (source === 'single') message.success('已创建代码分析执行批次 ' + executionId)
    const execution = await pollCodexTestExecutionResult(historyKey, executionId)
    if (runToken !== testRunToken || !execution) return
    if (source === 'single') message.success('Codex CLI 回复已返回，可点击历史查看')
    return execution
  } catch (error) {
    if (runToken !== testRunToken) return
    const errorMessage = getRequestErrorMessage(error, '代码分析测试启动失败')
    const history = rowTestHistories[historyKey]
    if (history?.historyKey === historyKey) {
      history.loading = false
      history.ready = false
      history.error = errorMessage
      history.data = undefined
    }
    if (testingRowCaseName.value === historyKey) testingRowCaseName.value = undefined
    if (source === 'tab') throw new Error(errorMessage)
    showRequestError(error, '代码分析测试启动失败')
  }
}

function getTabTestButtonText(listKey: BatchRecordTestListKey) {
  if (testingTabListKey.value !== listKey) return '测试全部'
  return `测试中 ${testingTabProgress.completed}/${testingTabProgress.total}`
}

async function handleTestTab(listKey: BatchRecordTestListKey) {
  if (!selectedTenantId.value) {
    message.error('请选择测试租户')
    return
  }
  if (testingTabListKey.value !== undefined || testingRowCaseName.value !== undefined) {
    message.warning('已有测试正在执行')
    return
  }
  const rows = [...getBatchRecordTestRowsRef(listKey).value]
  if (!rows.length) {
    message.error('当前 Tab 没有可执行的测试任务')
    return
  }

  testingTabListKey.value = listKey
  testingTabProgress.completed = 0
  testingTabProgress.total = rows.length
  let currentRowTitle = ''
  let passCount = 0
  try {
    for (const row of rows) {
      currentRowTitle = row.title
      const execution = await handleTestRow(row, 'tab')
      if (!execution) throw new Error('当前测试未返回终态结果')
      testingTabProgress.completed += 1
      if (execution.status === 'PASS') passCount += 1
    }
    if (passCount === rows.length) {
      message.success(`批量测试完成：${rows.length}/${rows.length} 项通过`)
    } else {
      message.warning(`批量测试完成：${passCount}/${rows.length} 项通过，请逐行查看历史`)
    }
  } catch (error) {
    const errorMessage = getRequestErrorMessage(error, '未知执行错误')
    message.error(`批量测试在“${currentRowTitle}”失败，已停止：${errorMessage}`)
  } finally {
    testingTabListKey.value = undefined
    testingTabProgress.completed = 0
    testingTabProgress.total = 0
  }
}

function getRequestErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) return error.message
  return fallbackMessage
}

function stopResultPolling() {
  resultPollToken += 1
  if (resultPollTimer) {
    clearTimeout(resultPollTimer)
  }
  const finishWaiting = resultPollWaitResolve
  if (finishWaiting) finishWaiting()
  else resultPollTimer = undefined
}

function waitForResultPollInterval(pollToken: number) {
  return new Promise<boolean>((resolve) => {
    const finishWaiting = () => {
      if (resultPollWaitResolve !== finishWaiting) return
      resultPollTimer = undefined
      resultPollWaitResolve = undefined
      resolve(pollToken === resultPollToken)
    }
    resultPollWaitResolve = finishWaiting
    resultPollTimer = setTimeout(finishWaiting, resultPollIntervalMs)
  })
}

async function pollCodexTestExecutionResult(historyKey: string, executionId: number): Promise<CodexTestApi.CodexTestExecutionVO | undefined> {
  const pollToken = ++resultPollToken
  while (pollToken === resultPollToken) {
    try {
      const execution = await CodexTestApi.getCodexTestExecutionResult(executionId)
      const history = rowTestHistories[historyKey]
      if (!history) return
      if (
        pollToken !== resultPollToken ||
        history?.executionId !== executionId ||
        history.historyKey !== historyKey
      )
        return
      if (terminalExecutionStatuses.has(execution.status)) {
        history.data = execution
        history.ready = true
        history.loading = false
        history.error = ''
        if (testingRowCaseName.value === historyKey) testingRowCaseName.value = undefined
        return execution
      }
      const shouldContinue = await waitForResultPollInterval(pollToken)
      if (!shouldContinue) return
    } catch (error) {
      const history = rowTestHistories[historyKey]
      if (!history) return
      if (
        pollToken !== resultPollToken ||
        history.executionId !== executionId ||
        history.historyKey !== historyKey
      )
        return
      history.error = getRequestErrorMessage(error, 'Codex CLI 执行结果读取失败')
      history.loading = false
      history.ready = false
      history.data = undefined
      if (testingRowCaseName.value === historyKey) testingRowCaseName.value = undefined
      throw error
    }
  }
}

function getExecutionStatusText(status?: string) {
  const statusTexts: Record<string, string> = {
    PENDING: '等待执行',
    CLAIMED: '已领取',
    RUNNING: '执行中',
    PASS: '通过',
    FAIL: '不通过',
    BLOCKED: '阻塞',
    CANCELED: '已取消',
    TIMEOUT: '超时',
    NOT_RUN: '未执行'
  }
  return status ? statusTexts[status] || status : '等待启动'
}

function getExecutionStatusTagType(
  status?: string
): '' | 'success' | 'warning' | 'info' | 'danger' {
  if (status === 'PASS') return 'success'
  if (status === 'FAIL' || status === 'BLOCKED' || status === 'TIMEOUT') return 'danger'
  if (status === 'CANCELED') return 'info'
  return 'warning'
}

onMounted(async () => {
  await Promise.all([getTenantOptions(), loadPersistedBatchRecordTestRows()])
})

onBeforeUnmount(() => {
  stopResultPolling()
})
</script>

<style scoped lang="scss">
.edhr-batch-record-test-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-batch-record-test-page__inner-tabs {
  :deep(.el-tabs__header) {
    margin: 0 0 12px;
  }
}

.edhr-batch-record-test-page__list-template {
  :deep(.unified-list-template__toolbar) {
    align-items: center;
  }

  :deep(.edhr-batch-record-test-page__mapping-title-column .cell),
  :deep(.edhr-batch-record-test-page__description-column .cell) {
    line-height: 1.5;
    word-break: break-word;
    white-space: normal;
    overflow-wrap: anywhere;
  }
}

.edhr-batch-record-test-page__tenant-filter {
  margin-bottom: 0;
}

.edhr-batch-record-test-page__run-all-button {
  min-width: 128px;
}

@media (min-width: 1181px) {
  .edhr-batch-record-test-page__list-template.unified-list-template--single-line-toolbar {
    :deep(.unified-list-template__query-form) {
      grid-template-columns: minmax(0, 1fr) auto;
    }

    :deep(.unified-list-template__multi-filter) {
      min-width: 0;
    }
  }
}

.edhr-batch-record-test-page__alert {
  margin-bottom: 12px;
}

.edhr-batch-record-test-page__result-alert,
.edhr-batch-record-test-page__result-section {
  margin-top: 16px;
}

.edhr-batch-record-test-page__result-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
}

.edhr-batch-record-test-page__result-progress {
  margin: 0 0 10px;
  color: var(--el-text-color-secondary);
}

:global(.edhr-batch-record-test-page__result-dialog .el-dialog__body) {
  max-height: calc(92vh - 116px);
  overflow-y: auto;
}

:deep(.edhr-batch-record-test-page__actual-reply-column .cell),
:deep(.edhr-batch-record-test-page__mismatch-description-column .cell) {
  line-height: 1.5;
  word-break: break-word;
  white-space: normal;
  overflow-wrap: anywhere;
}
</style>
