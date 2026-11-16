<template>
  <Dialog v-model="dialogVisible" title="按人反查查阅能力" width="1180px">
    <div v-loading="loading">
      <div class="lookup-toolbar">
        <UserSelectV2 allow-inactive
          v-model="selectedUserId"
          class="lookup-toolbar__user"
          clearable
          placeholder="请选择用户"
          :user-options="users"
        />
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
        max-height="560"
        empty-text="请选择用户后查看文件类型查阅能力"
        data-testid="dcc-user-lookup-table"
      >
        <el-table-column label="类别编码" min-width="150" prop="code" show-overflow-tooltip />
        <el-table-column label="类别名称" min-width="180" prop="name" show-overflow-tooltip />
        <el-table-column label="查阅来源" min-width="260">
          <template #default="{ row }">
            <div class="lookup-source-list">
              <details v-if="row.viewSources?.length">
                <summary>查看 {{ row.viewSources.length }} 项来源</summary>
                <section v-for="group in groupReviewSources(row.viewSources)" :key="group.label" class="lookup-source-group">
                  <strong>{{ group.label }}</strong>
                  <ol>
                    <li v-for="(source, index) in group.sources" :key="index">
                      <div v-if="source.positionName">岗位：{{ source.positionName }}</div>
                      <div>{{ source.reason || '来源说明未记录' }}</div>
                      <small>来源编码：{{ source.source }}</small>
                    </li>
                  </ol>
                </section>
              </details>
              <div v-else>
                {{ row.browseReason || row.downloadReason || '-' }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="浏览" width="120">
          <template #default="{ row }">
            <el-tag :type="capabilityTagType(row.browseStatus)" size="small">
              {{ capabilityLabel(row.browseStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情" width="120">
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
        <el-table-column label="下载" width="120">
          <template #default="{ row }">
            <el-tag :type="capabilityTagType(row.downloadStatus)" size="small">
              {{ capabilityLabel(row.downloadStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规则说明" min-width="260">
          <template #default="{ row }">
            {{
              row.downloadReason ||
                row.pendingPreviewReason ||
                row.publishedPreviewReason ||
                row.detailReason ||
                row.browseReason ||
                '-'
            }}
          </template>
        </el-table-column>
        <el-table-column label="风险" min-width="280">
          <template #default="{ row }">
            <div class="lookup-risk-list">
              <el-tag
                v-if="!row.risks?.length"
                size="small"
                type="success"
              >
                无风险
              </el-tag>
              <template v-else>
                <p class="lookup-risk-summary">{{ row.risks[0].message || '风险说明未记录，请核对风险详情' }}</p>
                <details>
                  <summary>查看全部 {{ row.risks.length }} 项风险</summary>
                  <ul class="lookup-risk-details">
                    <li v-for="(risk, index) in row.risks" :key="index">
                      <el-tag size="small" :type="risk.blocking ? 'danger' : 'warning'">
                        {{ risk.blocking ? '阻塞' : '提示' }}
                      </el-tag>
                      <p>{{ risk.message || '风险说明未记录' }}</p>
                      <small>风险编码：{{ risk.code }}</small>
                    </li>
                  </ul>
                </details>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </Dialog>
</template>

<script lang="ts" setup>
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import {
  getReviewMatrixUserLookup,
  type ControlledFileCapabilityStatus,
  type ControlledFileCategoryReviewMatrixUserLookupVO
} from '@/api/dcc/controlledFile/fileCategories'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { groupReviewSources } from './reviewMatrixDisplay'

defineOptions({ name: 'CategoryReviewMatrixUserLookupDialog' })

const dialogVisible = ref(false)
const loading = ref(false)
const users = ref<UserVO[]>([])
const rows = ref<ControlledFileCategoryReviewMatrixUserLookupVO[]>([])
const selectedUserId = ref<number>()
const errorMessage = ref('')
const message = useMessage()


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
    rows.value = await getReviewMatrixUserLookup(selectedUserId.value)
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
.lookup-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.lookup-toolbar__user {
  width: 320px;
}

.lookup-source-list {
  color: #263247;
  font-size: 12px;
  line-height: 18px;
}

.lookup-risk-list {
  overflow-wrap: anywhere;
}

.lookup-source-list summary,
.lookup-risk-list summary {
  cursor: pointer;
  color: var(--el-color-primary);
}

.lookup-source-group {
  margin-top: 12px;
  overflow-wrap: anywhere;
}

.lookup-source-group ol,
.lookup-risk-details {
  padding-left: 20px;
  margin: 8px 0;
}

.lookup-source-group li,
.lookup-risk-details li {
  margin-bottom: 12px;
}

.lookup-risk-summary {
  margin: 0 0 6px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.lookup-risk-details p {
  margin: 6px 0;
}

.lookup-source-group small,
.lookup-risk-details small {
  color: var(--el-text-color-secondary);
}

@media (max-width: 960px) {
  .lookup-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .lookup-toolbar__user {
    width: 100%;
  }
}
</style>
