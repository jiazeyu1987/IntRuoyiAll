<template>
  <section class="route-resource-workbench">
    <div class="resource-toolbar">
      <el-form
        ref="queryFormRef"
        :model="queryParams"
        :inline="true"
        label-width="76px"
        class="resource-toolbar__form"
      >
        <el-form-item label="关键词" prop="keyword">
          <el-input
            v-model="queryParams.keyword"
            class="resource-toolbar__search"
            clearable
            placeholder="产品 / 路线 / 工序 / 设备"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="资源类型" prop="resourceType">
          <el-select
            v-model="queryParams.resourceType"
            class="resource-toolbar__select"
            clearable
            placeholder="全部"
          >
            <el-option label="设备" value="MACHINE" />
            <el-option label="人工" value="WORKER" />
            <el-option label="未配置" value="UNCONFIGURED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-5px" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-5px" /> 重置
          </el-button>
        </el-form-item>
      </el-form>
      <div v-if="props.routeId" class="resource-toolbar__hint">
        当前仅显示该工艺路线由工作站派生的设备与人工资源信息；维护请进入工作站详情。
      </div>
    </div>

    <div class="resource-table-shell">
      <el-table
        v-loading="loading"
        :data="list"
        :show-overflow-tooltip="true"
        row-key="rowKey"
        class="resource-table"
        height="620"
      >
        <el-table-column label="产品编码" prop="productCode" min-width="150" fixed="left" />
        <el-table-column label="产品名称" prop="productName" min-width="180" />
        <el-table-column label="路线" min-width="190">
          <template #default="{ row }">
            <span class="cell-main">{{ row.routeCode }}</span>
            <span class="cell-sub">{{ row.routeName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="序号" prop="sort" width="70" align="center" />
        <el-table-column label="工序" min-width="190">
          <template #default="{ row }">
            <span class="cell-main">{{ row.processCode }}</span>
            <span class="cell-sub">{{ row.processName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="资源" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="resourceTagType(row.resourceType)" effect="light">
              {{ resourceTypeLabel(row.resourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="工位" min-width="190">
          <template #default="{ row }">
            <span class="cell-main">{{ row.workstationCode || '-' }}</span>
            <span class="cell-sub">{{ row.workstationName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="设备" min-width="210">
          <template #default="{ row }">
            <span class="cell-main">{{ row.machineryCode || '-' }}</span>
            <span class="cell-sub">{{ row.machineryName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="设备数" width="132" align="center">
          <template #default="{ row }">
            <span v-if="row.resourceType === 'MACHINE'">{{ row.machineryQuantity ?? '-' }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="单台产能/h" width="148" align="center">
          <template #default="{ row }">
            <span v-if="row.resourceType === 'MACHINE'">
              {{ formatNumber(row.machineryStandardHourlyCapacity) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="人数" width="118" align="center">
          <template #default="{ row }">
            <span v-if="row.resourceType === 'WORKER'">{{ row.workerQuantity ?? '-' }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="单人产能/h" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.resourceType === 'WORKER'">
              {{ formatNumber(row.singleStandardHourlyCapacity) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="预算/h" width="118" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.budgetHourlyCapacity) }}
          </template>
        </el-table-column>
        <el-table-column label="预算/日" width="118" align="right">
          <template #default="{ row }">
            {{ formatNumber(row.budgetDailyCapacity) }}
          </template>
        </el-table-column>
        <el-table-column label="来源" prop="capacitySource" width="105" align="center" />
      </el-table>
      <div class="resource-footer">
        <span class="resource-total">共 {{ total }} 条</span>
        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import { ProRouteResourceApi, ProRouteResourceVO } from '@/api/mes/pro/route/resource'

defineOptions({ name: 'MesProRouteResourceTable' })

const props = withDefaults(
  defineProps<{ routeId?: number; readonly?: boolean }>(),
  {
    routeId: undefined,
    readonly: false
  }
)

const loading = ref(false)
const list = ref<ProRouteResourceVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  routeId: undefined as number | undefined,
  keyword: undefined as string | undefined,
  resourceType: undefined as string | undefined
})

const getList = async () => {
  loading.value = true
  try {
    queryParams.routeId = props.routeId
    const data = await ProRouteResourceApi.getResourcePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery(true)
}

const resourceTypeLabel = (type: ProRouteResourceVO['resourceType']) => {
  if (type === 'MACHINE') return '设备'
  if (type === 'WORKER') return '人工'
  return '未配置'
}

const resourceTagType = (type: ProRouteResourceVO['resourceType']) => {
  if (type === 'MACHINE') return 'primary'
  if (type === 'WORKER') return 'success'
  return 'warning'
}

const formatNumber = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}

onMounted(() => {
  getList()
})

watch(
  () => props.routeId,
  () => {
    queryParams.pageNo = 1
    getList()
  }
)
</script>

<style scoped>
.route-resource-workbench {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
}

.resource-toolbar {
  border-bottom: 1px solid #dbe3ef;
  background: #fff;
  padding: 14px 16px 0;
}

.resource-toolbar__form {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
}

.resource-toolbar__search {
  width: 320px;
}

.resource-toolbar__select {
  width: 168px;
}

.resource-toolbar__hint {
  padding: 0 0 14px;
  color: #4b5563;
  font-size: 13px;
  line-height: 20px;
}

.resource-table-shell {
  background: #fff;
}

.resource-table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
  color: #263247;
  font-size: 0.9rem;
  font-weight: 600;
}

.resource-table :deep(.el-table__row) {
  height: 52px;
}

.resource-table :deep(.cell) {
  padding: 7px 10px;
  line-height: 1.28;
}

.cell-main,
.cell-sub {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-main {
  color: #172033;
  font-weight: 600;
}

.cell-sub {
  margin-top: 3px;
  color: #4b5563;
  font-size: 12px;
}

.resource-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  border-top: 1px solid #edf1f6;
  padding: 0 14px;
}

.resource-total {
  color: #4b5563;
  font-size: 13px;
}
</style>
