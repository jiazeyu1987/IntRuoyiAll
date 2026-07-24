<template>
  <div class="showroom-assignment-workbench">
    <div class="showroom-assignment-workbench__toolbar">
      <div>
        <h3 class="showroom-assignment-workbench__title">补充指派工作台</h3>
        <p class="showroom-assignment-workbench__subtitle">
          展示真实指派待办、站内信追踪和自动提交结果，不提供站外降级。
        </p>
      </div>
      <div class="showroom-assignment-workbench__actions">
        <el-select v-model="filters.targetType" clearable placeholder="目标类型">
          <el-option label="公司" value="COMPANY" />
          <el-option label="产品" value="PRODUCT" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="状态">
          <el-option label="待处理" value="OPEN" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="待提交" value="PENDING" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
        <el-select v-model="filters.assigneeUserId" clearable filterable placeholder="编辑人">
          <el-option
            v-for="user in userOptions"
            :key="user.id"
            :label="`${user.nickname} #${user.id}`"
            :value="user.id"
          />
        </el-select>
        <el-button type="primary" @click="dialogVisible = true">发起指派</el-button>
        <el-button :loading="loading" @click="loadAssignments">
          <Icon class="mr-5px" icon="ep:refresh" />
          刷新
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      :closable="false"
      class="mb-12px"
      show-icon
      type="error"
      :description="loadError"
      title="指派工作台加载失败"
    />

    <div class="showroom-assignment-workbench__body">
      <div class="showroom-assignment-workbench__list-shell">
        <div class="showroom-assignment-workbench__section-title">指派记录</div>
        <el-table
          v-loading="loading"
          :data="filteredRows"
          highlight-current-row
          row-key="assignmentId"
          @current-change="handleCurrentChange"
        >
          <el-table-column label="ID" width="84" prop="assignmentId" />
          <el-table-column label="目标" width="92">
            <template #default="{ row }">
              <el-tag type="info">{{ resolveTargetTypeText(row.targetType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="对象ID" width="90" prop="targetId" />
          <el-table-column label="字段" min-width="150">
            <template #default="{ row }">{{ resolveFieldLabel(row.targetType, row.fieldCode) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="resolveAssignmentStatusTagType(row.status)">
                {{ resolveAssignmentStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="编辑人" width="100" prop="assigneeUserId" />
        </el-table>
      </div>

      <div class="showroom-assignment-workbench__detail-shell">
        <div class="showroom-assignment-workbench__detail-header">
          <div>
            <div class="showroom-assignment-workbench__section-title">自动提交</div>
            <div v-if="activeDetail" class="showroom-assignment-workbench__detail-meta">
              指派 #{{ activeDetail.assignmentId }} /
              {{ resolveTargetTypeText(activeDetail.targetType) }} {{ activeDetail.targetId }}
            </div>
          </div>
          <el-button type="primary" @click="dialogVisible = true">新建指派</el-button>
        </div>

        <el-empty v-if="!activeDetail" description="请选择一条指派记录查看详情" />

        <template v-else>
          <div class="showroom-assignment-workbench__summary">
            <div class="showroom-assignment-workbench__summary-item">
              <span class="label">通知模板</span>
              <span>{{ activeDetail.notifyTemplateCode || '未生成' }}</span>
            </div>
            <div class="showroom-assignment-workbench__summary-item">
              <span class="label">通知内容</span>
              <span>{{ activeDetail.notifyContent || '无' }}</span>
            </div>
            <div class="showroom-assignment-workbench__summary-item">
              <span class="label">最新自动提交流程</span>
              <span>{{ activeDetail.latestChangeRequestStatus || '未提交' }}</span>
            </div>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="字段代码">{{ activeDetail.fieldCode }}</el-descriptions-item>
            <el-descriptions-item label="站内信 ID">{{ activeDetail.notifyMessageId || '无' }}</el-descriptions-item>
            <el-descriptions-item label="当前草稿">{{ activeDetail.currentDraftValue || '空' }}</el-descriptions-item>
            <el-descriptions-item label="最近保存版本">
              {{ activeDetail.lastSavedRevisionId || '无' }}
            </el-descriptions-item>
          </el-descriptions>

          <template v-if="!isWholeProductAssignment">
            <el-form class="mt-16px" label-width="110px">
              <el-form-item label="完成内容">
                <el-input
                  v-model="completionForm.fieldValue"
                  :rows="4"
                  placeholder="编辑人填写后的真实字段值"
                  type="textarea"
                />
              </el-form-item>
              <el-form-item label="审批人">
                <el-input model-value="企宣角色" disabled />
              </el-form-item>
            </el-form>

            <div class="showroom-assignment-workbench__detail-actions">
              <el-button :loading="actionLoading" type="primary" @click="handleCompleteAndSubmit">
                完成并提交
              </el-button>
              <el-button :loading="loading" @click="refreshActiveDetail">刷新详情</el-button>
            </div>
          </template>

          <template v-else>
            <el-alert
              :closable="false"
              class="mt-16px"
              show-icon
              type="info"
              title="整产品指派请到产品管理页完成填写并提交审批"
            />
          </template>
        </template>
      </div>
    </div>

    <FieldAssignmentDialog
      v-model="dialogVisible"
      :company-current="companyCurrent"
      :products="products"
      :user-options="userOptions"
      @saved="handleCreated"
    />
  </div>
</template>

<script setup lang="ts">
import request from '@/config/axios'
import { ShowroomAdminApi } from '@/api/showroom-admin'
import { useUserStore } from '@/store/modules/user'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import FieldAssignmentDialog from './FieldAssignmentDialog.vue'
import {
  buildTargetOptions,
  normalizeAssignmentPage,
  resolveAssignmentStatusTagType,
  resolveAssignmentStatusText,
  resolveFieldLabel,
  resolveTargetTypeText,
  type ShowroomAssignmentRecord
} from './contracts'
import { SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE } from '@/views/showroom-admin/product/contracts'

defineOptions({ name: 'AssignmentWorkbench' })

const props = withDefaults(
  defineProps<{
    companyCurrent?: Record<string, unknown> | null
    products?: unknown[]
  }>(),
  {
    companyCurrent: null,
    products: () => []
  }
)

const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')
const dialogVisible = ref(false)
const rows = ref<ShowroomAssignmentRecord[]>([])
const activeId = ref<number | null>(null)
const activeDetail = ref<ShowroomAssignmentRecord | null>(null)
const userOptions = ref<UserVO[]>([])
const filters = reactive({
  targetType: '',
  status: '',
  assigneeUserId: null as number | null
})

const completionForm = reactive({
  fieldValue: ''
})

const filteredRows = computed(() => {
  return rows.value.filter((row) => {
    const matchesTarget = !filters.targetType || row.targetType === filters.targetType
    const matchesStatus = !filters.status || row.status === filters.status
    const matchesAssignee =
      !filters.assigneeUserId || row.assigneeUserId === filters.assigneeUserId
    return matchesTarget && matchesStatus && matchesAssignee
  })
})

const targetOptions = computed(() => buildTargetOptions(props.companyCurrent, props.products))
const isWholeProductAssignment = computed(
  () => activeDetail.value?.fieldCode === SHOWROOM_PRODUCT_WHOLE_ASSIGNMENT_FIELD_CODE
)

const loadUserOptions = async () => {
  userOptions.value = await getSimpleUserList()
}

const loadAssignments = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const page = await request.get({
      url: '/showroom/assignment/page',
      params: {
        pageNo: 1,
        pageSize: 50,
        targetType: filters.targetType || undefined,
        status: filters.status || undefined,
        assigneeUserId: filters.assigneeUserId || undefined
      }
    })
    rows.value = normalizeAssignmentPage(page)
    const nextId = activeId.value && rows.value.some((row) => row.assignmentId === activeId.value)
      ? activeId.value
      : rows.value[0]?.assignmentId || null
    activeId.value = nextId
    if (nextId) {
      await refreshActiveDetail()
    } else {
      activeDetail.value = null
    }
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    loadError.value = resolved.message
    message.error(`指派工作台加载失败：${resolved.message}`)
  } finally {
    loading.value = false
  }
}

const loadAssignmentDetail = async (assignmentId: number) => {
  const detail = await request.get({ url: `/showroom/assignment/get?id=${assignmentId}` })
  activeDetail.value = normalizeAssignmentPage([detail])[0]
  completionForm.fieldValue = activeDetail.value.currentDraftValue || ''
}

const handleCurrentChange = async (row?: ShowroomAssignmentRecord) => {
  if (!row) {
    activeId.value = null
    activeDetail.value = null
    return
  }
  activeId.value = row.assignmentId
  await loadAssignmentDetail(row.assignmentId)
}

const refreshActiveDetail = async () => {
  if (!activeId.value) {
    activeDetail.value = null
    return
  }
  await loadAssignmentDetail(activeId.value)
}

const handleCreated = async () => {
  dialogVisible.value = false
  await loadAssignments()
}

const handleCompleteAndSubmit = async () => {
  if (!activeDetail.value || !userStore.getUser.id) {
    throw new Error('当前登录用户缺失，无法执行自动提交')
  }
  if (!completionForm.fieldValue.trim()) {
    throw new Error('完成内容不能为空')
  }
  actionLoading.value = true
  try {
    await ShowroomAdminApi.completeAssignmentAndSubmit({
      assignmentId: activeDetail.value.assignmentId,
      fieldValue: completionForm.fieldValue.trim(),
      operatorUserId: userStore.getUser.id
    })
    message.success('已完成并提交审批')
    await loadAssignments()
  } finally {
    actionLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadUserOptions(), loadAssignments()])
  if (targetOptions.value.COMPANY.length > 0) {
    filters.targetType = filters.targetType || 'PRODUCT'
  }
})
</script>

<style scoped>
.showroom-assignment-workbench {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.showroom-assignment-workbench__toolbar,
.showroom-assignment-workbench__list-shell,
.showroom-assignment-workbench__detail-shell {
  background: #ffffff;
  border: 1px solid #dbe3ef;
}

.showroom-assignment-workbench__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 8px 8px 0 0;
}

.showroom-assignment-workbench__title {
  margin: 0;
  color: #172033;
  font-size: 1.05rem;
}

.showroom-assignment-workbench__subtitle {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-assignment-workbench__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.showroom-assignment-workbench__body {
  display: grid;
  grid-template-columns: minmax(380px, 42%) minmax(0, 1fr);
  gap: 12px;
}

.showroom-assignment-workbench__list-shell,
.showroom-assignment-workbench__detail-shell {
  padding: 12px;
  border-radius: 0 0 8px 8px;
}

.showroom-assignment-workbench__section-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 0.95rem;
  font-weight: 600;
}

.showroom-assignment-workbench__detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.showroom-assignment-workbench__detail-meta {
  margin-top: 4px;
  color: #4b5563;
  font-size: 0.85rem;
}

.showroom-assignment-workbench__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.showroom-assignment-workbench__summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 72px;
  padding: 10px 12px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  color: #263247;
  font-size: 0.88rem;
}

.showroom-assignment-workbench__summary-item .label {
  color: #4b5563;
  font-size: 0.8rem;
}

.showroom-assignment-workbench__detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

@media (max-width: 1100px) {
  .showroom-assignment-workbench__body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .showroom-assignment-workbench__toolbar,
  .showroom-assignment-workbench__detail-header {
    align-items: stretch;
    flex-direction: column;
  }

  .showroom-assignment-workbench__summary {
    grid-template-columns: 1fr;
  }
}
</style>
