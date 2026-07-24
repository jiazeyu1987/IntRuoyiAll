<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="82px">
      <el-form-item label="文件类别" prop="categoryId">
        <el-select
          v-model="queryParams.categoryId"
          class="!w-220px"
          clearable
          filterable
          placeholder="全部类别"
        >
          <el-option
            v-for="item in categories"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="培训状态" prop="status">
        <el-select
          v-model="queryParams.status"
          class="!w-220px"
          clearable
          placeholder="全部状态"
        >
          <el-option
            v-for="item in TRAINING_PROGRESS_STATUS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />
          查询
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" />
          重置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" empty-text="当前暂无培训执行记录">
      <el-table-column label="文件标题" min-width="220" prop="title" show-overflow-tooltip />
      <el-table-column label="文件名称" min-width="220" prop="fileName" show-overflow-tooltip />
      <el-table-column label="文件编号" min-width="160" prop="fileNumber" />
      <el-table-column label="受训用户" min-width="220">
        <template #default="{ row }">
          {{ userNameMap.get(row.userId) || `用户#${row.userId}` }}
        </template>
      </el-table-column>
      <el-table-column label="来源部门" min-width="220">
        <template #default="{ row }">
          {{ buildDepartmentNames(row.departmentIds, deptNameMap) }}
        </template>
      </el-table-column>
      <el-table-column label="培训摘要" min-width="300">
        <template #default="{ row }">
          <div class="training-execution-summary" data-testid="dcc-training-execution-summary">
            <div class="training-execution-summary__line">
              <el-tag :type="getTrainingProgressStatusTagType(row.status)" size="small">
                状态：{{ getTrainingProgressStatusLabel(row.status) }}
              </el-tag>
              <span class="training-execution-summary__meta">
                版本：{{ row.versionNo || '-' }}
              </span>
            </div>
            <div class="training-execution-summary__meta">
              时长：{{
                formatTrainingProgressText(row.accumulatedViewSeconds, row.requiredViewSeconds)
              }}
            </div>
            <div class="training-execution-summary__meta">
              确认：{{ getTrainingExecutionAcknowledgedAt(row) }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" fixed="right" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.controlledFileId)">文件详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { isSearchFormInputEmpty } from '@/utils/search'
import { dateFormatter2 } from '@/utils/formatTime'
import { getFileCategoryList, type ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import {
  getTrainingExecutionPage,
  type TrainingExecutionRowVO
} from '@/api/dcc/controlledFile/training'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { buildDccSimpleUserLabelMap } from '../../shared/utils'
import {
  TRAINING_PROGRESS_STATUS_OPTIONS,
  buildDepartmentNames,
  formatTrainingProgressText,
  getTrainingProgressStatusLabel,
  getTrainingProgressStatusTagType,
  resolveTrainingPageErrorMessage
} from '../presentation'
import { openControlledFileViewer } from '../../shared/viewer-navigation'

defineOptions({ name: 'TrainingExecutionTab' })

const router = useRouter()
const route = useRoute()
const message = useMessage()

const queryFormRef = ref()
const loading = ref(false)
const total = ref(0)
const categories = ref<ControlledFileCategoryVO[]>([])
const users = ref<UserVO[]>([])
const departments = ref<DeptVO[]>([])
const list = ref<TrainingExecutionRowVO[]>([])

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  categoryId: undefined as number | undefined,
  status: undefined as string | undefined
})

const deptNameMap = computed(() => new Map(departments.value.map((item) => [item.id, item.name])))
const userNameMap = computed(() => buildDccSimpleUserLabelMap(users.value as UserVO[]))

const loadBaseData = async () => {
  const [categoryList, deptList, userList] = await Promise.all([
    getFileCategoryList(),
    getSimpleDeptList(),
    getSimpleUserList()
  ])
  categories.value = categoryList.filter((item) => item.active)
  departments.value = deptList
  users.value = userList
}

const getList = async () => {
  loading.value = true
  try {
    const data = await getTrainingExecutionPage(queryParams)
    list.value = data.list
    total.value = data.total
  } catch (error) {
    message.error(
      resolveTrainingPageErrorMessage(error, '培训执行记录加载失败，请查看错误提示后重试。')
    )
  } finally {
    loading.value = false
  }
}

const handleQuery = async (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    await resetQuery()
    return
  }
  queryParams.pageNo = 1
  await getList()
}

const resetQuery = async () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  await getList()
}

const getTrainingExecutionAcknowledgedAt = (row: TrainingExecutionRowVO) =>
  row.acknowledgedAt ? dateFormatter2(row, undefined as any, row.acknowledgedAt) || '-' : '-'

const openDetail = (id: number | string) => {
  openControlledFileViewer(router, route, id, 'training-execution')
}

onMounted(async () => {
  await loadBaseData()
  await getList()
})
</script>

<style scoped>
.training-execution-summary {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.training-execution-summary__line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.training-execution-summary__meta {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}
</style>
