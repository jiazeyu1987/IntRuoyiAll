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
                  label="描述"
                  prop="description"
                  :min-width="getProductionLeaderColumnMinWidthString('description', 520)"
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
                      :disabled="!selectedTenantId || testingRowId === row.id"
                      :loading="testingRowId === row.id"
                      link
                      type="success"
                      @click="handleTestRow(row)"
                    >
                      测试
                    </el-button>
                    <el-button link type="primary" @click="openDescriptionEditor('productionLeader', row)">
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
                  label="描述"
                  prop="description"
                  :min-width="getFrontlinePqcColumnMinWidthString('description', 560)"
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
                      :disabled="!selectedTenantId || testingRowId === row.id"
                      :loading="testingRowId === row.id"
                      link
                      type="success"
                      @click="handleTestRow(row)"
                    >
                      测试
                    </el-button>
                    <el-button link type="primary" @click="openDescriptionEditor('frontlinePqc', row)">
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
                  label="描述"
                  prop="description"
                  :min-width="getFrontlineProductionColumnMinWidthString('description', 560)"
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
                      :disabled="!selectedTenantId || testingRowId === row.id"
                      :loading="testingRowId === row.id"
                      link
                      type="success"
                      @click="handleTestRow(row)"
                    >
                      测试
                    </el-button>
                    <el-button link type="primary" @click="openDescriptionEditor('frontlineProduction', row)">
                      修改
                    </el-button>
                    <el-button link type="danger" @click="handleDeleteRow('frontlineProduction', row)">
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
          <el-button type="primary" @click="saveDescriptionEdit">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  useUserTableColumns,
  type UserTableColumnDefinition,
  type UserTableColumnState
} from '@/hooks/web/useUserTableColumns'
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

type BatchRecordTestListKey = 'productionLeader' | 'frontlinePqc' | 'frontlineProduction'

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
  }
}

const message = useMessage()
const activeInnerTab = ref<BatchRecordTestListKey>('productionLeader')
const tenantOptions = ref<TenantApi.TenantVO[]>([])
const selectedTenantId = ref<number>()
const testingRowId = ref<number>()
const loadError = ref('')
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
    description: '从 QA 给的批记录文件解析批记录表单、工序、设备、参数、上下限，并为工序分配不良原因。',
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
    description: '查看一线报工数据，将报工数量分配给一个或多个活跃订单；某订单某工序累计分配达到订单数量后更新生产进度。',
    caseName: '批记录测试-生产组长-04-报工分配与生产进度',
    testScope: '生产组长职责：报工分配与生产进度'
  },
  {
    id: 5,
    sort: 5,
    title: '活跃订单与检验进度',
    description: '将生产工单加入活跃订单列表；一线 PQC 提交活跃订单工序检验结果，PQC 组长确认后更新检验进度。',
    caseName: '批记录测试-生产组长-05-活跃订单与检验进度',
    testScope: '生产组长职责：活跃订单与检验进度'
  }
])

const frontlinePqcRows = ref<BatchRecordTestRow[]>([
  {
    id: 101,
    sort: 1,
    title: '活跃订单池选择订单',
    description: '一线PQC填写任务从所有生产组长维护的活跃订单池中选择订单，并以所选订单作为后续产品、工序和检验项目上下文。',
    caseName: '批记录测试-一线PQC-01-活跃订单池选择订单',
    testScope: '一线PQC：从所有生产组长维护的活跃订单池选择订单'
  },
  {
    id: 102,
    sort: 2,
    title: '按产品读取工艺路线工序',
    description: '根据订单对应产品读取工艺路线中的全部工序，点击工序卡片时展示可选择的完整工序列表。',
    caseName: '批记录测试-一线PQC-02-按产品读取工艺路线工序',
    testScope: '一线PQC：按订单产品读取工艺路线全部工序并通过工序卡片选择'
  },
  {
    id: 103,
    sort: 3,
    title: '按工序加载QA检验项',
    description: '选择工序后，从该产品对应QA检测项目列表中查找该工序的全部检验项，并展示在检验项tab中。',
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
    description: '选择巡检时，根据产品+工序读取抽样率并按生产数量计算检验数量，例如生产10000、抽样率0.4时检验数量为40。',
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
    description: '提交时必须输入所选员工的电子密码，而不是生产组长的电子密码；本地提交内容进入该组长的报工管理页签等待分配。',
    caseName: '批记录测试-一线生产-08-电子密码与待分配报工',
    testScope: '一线生产：使用所选员工电子密码提交并进入生产组长报工管理待分配'
  }
])

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

const productionLeaderDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sort', label: '序号', width: 80 },
  { key: 'title', label: '职责', minWidth: 220 },
  { key: 'description', label: '描述', minWidth: 520, sortable: false },
  { key: 'actions', label: '操作', width: 180, hideable: false, business: false, sortable: false }
]

const productionLeaderColumnControl = useUserTableColumns(
  'mes.pro.edhrBatchRecordTest.productionLeader',
  productionLeaderDefaultColumns
)
const productionLeaderColumns = computed(() => productionLeaderColumnControl.columns.value)
const productionLeaderColumnSaving = computed(() => productionLeaderColumnControl.saving.value)
const isProductionLeaderColumnVisible = (key: string) => productionLeaderColumnControl.isColumnVisible(key)
const getProductionLeaderColumnWidthString = (key: string, fallback?: number) =>
  productionLeaderColumnControl.getColumnWidthString(key, fallback)
const getProductionLeaderColumnMinWidthString = (key: string, fallback?: number) =>
  productionLeaderColumnControl.getColumnMinWidthString(key, fallback)
const handleProductionLeaderHeaderDragend = async (newWidth: number, oldWidth: number, column: any) => {
  await productionLeaderColumnControl.handleHeaderDragend(newWidth, oldWidth, column)
}
const saveProductionLeaderColumnConfig = async (columns: UserTableColumnState[]) => {
  await productionLeaderColumnControl.saveConfig(columns)
}

const frontlinePqcDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'sort', label: '序号', width: 80 },
  { key: 'title', label: '任务', minWidth: 220 },
  { key: 'description', label: '描述', minWidth: 560, sortable: false },
  { key: 'actions', label: '操作', width: 180, hideable: false, business: false, sortable: false }
]

const frontlinePqcColumnControl = useUserTableColumns(
  'mes.pro.edhrBatchRecordTest.frontlinePqc',
  frontlinePqcDefaultColumns
)
const frontlinePqcColumns = computed(() => frontlinePqcColumnControl.columns.value)
const frontlinePqcColumnSaving = computed(() => frontlinePqcColumnControl.saving.value)
const isFrontlinePqcColumnVisible = (key: string) =>
  frontlinePqcColumnControl.isColumnVisible(key)
const getFrontlinePqcColumnWidthString = (key: string, fallback?: number) =>
  frontlinePqcColumnControl.getColumnWidthString(key, fallback)
const getFrontlinePqcColumnMinWidthString = (key: string, fallback?: number) =>
  frontlinePqcColumnControl.getColumnMinWidthString(key, fallback)
const handleFrontlinePqcHeaderDragend = async (
  newWidth: number,
  oldWidth: number,
  column: any
) => {
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
const frontlineProductionColumnSaving = computed(() => frontlineProductionColumnControl.saving.value)
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

const pagedProductionLeaderRows = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredProductionLeaderRows.value.slice(start, start + queryParams.pageSize)
})

const pagedFrontlinePqcRows = computed(() => {
  const start = (frontlinePqcQueryParams.pageNo - 1) * frontlinePqcQueryParams.pageSize
  return filteredFrontlinePqcRows.value.slice(
    start,
    start + frontlinePqcQueryParams.pageSize
  )
})

const pagedFrontlineProductionRows = computed(() => {
  const start = (frontlineProductionQueryParams.pageNo - 1) * frontlineProductionQueryParams.pageSize
  return filteredFrontlineProductionRows.value.slice(
    start,
    start + frontlineProductionQueryParams.pageSize
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

function filterBatchRecordTestRows(rows: BatchRecordTestRow[], keyword: string) {
  if (!keyword) return rows
  return rows.filter((row) =>
    [row.title, row.description, row.testScope].some((text) => text.includes(keyword))
  )
}

function getBatchRecordTestRowsRef(listKey: BatchRecordTestListKey) {
  if (listKey === 'productionLeader') return productionLeaderRows
  if (listKey === 'frontlinePqc') return frontlinePqcRows
  return frontlineProductionRows
}

function getBatchRecordTestQueryParams(listKey: BatchRecordTestListKey) {
  if (listKey === 'productionLeader') return queryParams
  if (listKey === 'frontlinePqc') return frontlinePqcQueryParams
  return frontlineProductionQueryParams
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

function saveDescriptionEdit() {
  if (descriptionEditor.rowId == null) return
  const nextDescription = descriptionEditor.description.trim()
  if (!nextDescription) {
    message.error('描述不能为空')
    return
  }
  updateBatchRecordTestRows(descriptionEditor.listKey, (rows) =>
    rows.map((row) =>
      row.id === descriptionEditor.rowId ? { ...row, description: nextDescription } : row
    )
  )
  closeDescriptionEditor()
  message.success('描述已修改')
}

async function handleDeleteRow(listKey: BatchRecordTestListKey, row: BatchRecordTestRow) {
  await message.confirm(`确认删除“${row.title}”这行测试任务吗？`, '删除确认')
  updateBatchRecordTestRows(listKey, (rows) => rows.filter((item) => item.id !== row.id))
  message.success('行已删除')
}

function showRequestError(error: unknown, defaultMessage: string) {
  const text = error instanceof Error ? error.message : typeof error === 'string' ? error : defaultMessage
  message.error(text || defaultMessage)
}

async function getTenantOptions() {
  try {
    loadError.value = ''
    tenantOptions.value = await TenantApi.getTenantList()
    selectedTenantId.value = tenantOptions.value[0]?.id
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '测试租户加载失败'
    showRequestError(error, '测试租户加载失败')
  }
}

function buildCodeReadonlyCasePayload(definition: BatchRecordTestRow): CodexTestApi.CodexTestCaseVO {
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

async function handleTestRow(row: BatchRecordTestRow) {
  if (!selectedTenantId.value) {
    message.error('请选择测试租户')
    return
  }
  testingRowId.value = row.id
  try {
    const caseId = await upsertCodeReadonlyCase(row)
    const executionId = await CodexTestApi.startCodexTestExecution({
      targetTenantId: selectedTenantId.value,
      executionMode: 'SEQUENTIAL',
      caseIds: [caseId]
    })
    message.success('已创建代码分析执行批次 ' + executionId)
  } catch (error) {
    showRequestError(error, '代码分析测试启动失败')
  } finally {
    testingRowId.value = undefined
  }
}

onMounted(async () => {
  await getTenantOptions()
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
}

.edhr-batch-record-test-page__tenant-filter {
  margin-bottom: 0;
}

.edhr-batch-record-test-page__alert {
  margin-bottom: 12px;
}
</style>
