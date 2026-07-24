<template>
  <Dialog v-model="dialogVisible" title="按人反查查看矩阵" width="1180px">
    <div v-loading="loading">
      <div class="view-lookup-toolbar">
        <el-select
          v-model="selectedUserId"
          class="view-lookup-toolbar__user"
          clearable
          filterable
          placeholder="请选择用户"
        >
          <el-option
            v-for="item in users"
            :key="item.id"
            :label="formatUserLabel(item)"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" @click="loadLookup">
          <Icon icon="ep:search" class="mr-5px" />
          查询
        </el-button>
      </div>

      <el-alert
        v-if="errorMessage"
        class="mb-12px"
        :title="errorMessage"
        type="error"
        :closable="false"
      />

      <el-table
        :data="rows"
        empty-text="请选择用户后查看文件类型查阅能力"
        data-testid="dcc-view-matrix-user-lookup-table"
      >
        <el-table-column label="类别编码" min-width="150" prop="code" show-overflow-tooltip />
        <el-table-column label="类别名称" min-width="180" prop="name" show-overflow-tooltip />
        <el-table-column label="查看来源" min-width="280">
          <template #default="{ row }">
            <div class="view-lookup-source-list">
              <div v-if="row.viewSources?.length">
                {{
                  row.viewSources
                    .map((item) => item.reason || item.subjectLabel || item.source)
                    .join(' / ')
                }}
              </div>
              <div v-else>
                {{ row.browseReason || row.downloadReason || '-' }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="浏览" width="110">
          <template #default="{ row }">
            <el-tag :type="capabilityTagType(row.browseStatus)" size="small">
              {{ capabilityLabel(row.browseStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情" width="110">
          <template #default="{ row }">
            <el-tag :type="capabilityTagType(row.detailStatus)" size="small">
              {{ capabilityLabel(row.detailStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已发布查看" width="140">
          <template #default="{ row }">
            <el-tag :type="capabilityTagType(row.publishedPreviewStatus)" size="small">
              {{ capabilityLabel(row.publishedPreviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="待审查看" width="140">
          <template #default="{ row }">
            <el-tag :type="capabilityTagType(row.pendingPreviewStatus)" size="small">
              {{ capabilityLabel(row.pendingPreviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下载" width="110">
          <template #default="{ row }">
            <el-tag :type="capabilityTagType(row.downloadStatus)" size="small">
              {{ capabilityLabel(row.downloadStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规则说明" min-width="260">
          <template #default="{ row }">
            {{
              row.browseReason ||
                row.publishedPreviewReason ||
                row.pendingPreviewReason ||
                row.downloadReason ||
                '-'
            }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </Dialog>
</template>

<script lang="ts" setup>
import {
  getViewMatrixUserLookup,
  type ControlledFileCapabilityStatus,
  type ControlledFileCategoryViewMatrixUserLookupVO
} from '@/api/dcc/controlledFile/fileCategories'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { formatDccSimpleUserLabel } from '../../shared/utils'

defineOptions({ name: 'CategoryViewMatrixUserLookupDialog' })

const dialogVisible = ref(false)
const loading = ref(false)
const users = ref<UserVO[]>([])
const rows = ref<ControlledFileCategoryViewMatrixUserLookupVO[]>([])
const selectedUserId = ref<number>()
const errorMessage = ref('')
const message = useMessage()

const formatUserLabel = (user: UserVO) => formatDccSimpleUserLabel(user)

const capabilityLabel = (status: ControlledFileCapabilityStatus) => {
  if (status === 'YES') {
    return '可用'
  }
  if (status === 'CONDITIONAL') {
    return '条件性'
  }
  return '不可用'
}

const capabilityTagType = (status: ControlledFileCapabilityStatus) => {
  if (status === 'YES') {
    return 'success'
  }
  if (status === 'CONDITIONAL') {
    return 'warning'
  }
  return 'info'
}

const ensureUsers = async () => {
  if (users.value.length > 0) {
    return
  }
  users.value = await getSimpleUserList()
}

const loadLookup = async () => {
  if (!selectedUserId.value) {
    message.warning('请先选择用户')
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    rows.value = await getViewMatrixUserLookup(selectedUserId.value)
  } catch (error) {
    rows.value = []
    errorMessage.value =
      error instanceof Error && error.message && error.message !== 'error'
        ? error.message
        : '按人反查失败，请根据后端错误提示修正后重试。'
  } finally {
    loading.value = false
  }
}

const open = async () => {
  dialogVisible.value = true
  errorMessage.value = ''
  rows.value = []
  selectedUserId.value = undefined
  await ensureUsers()
}

defineExpose({ open })
</script>

<style scoped>
.view-lookup-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.view-lookup-toolbar__user {
  width: 320px;
}

.view-lookup-source-list {
  line-height: 1.6;
}
</style>
