<template>
  <div class="mt-12px rounded-[6px] border border-[#dbe3ef] bg-white px-12px py-10px">
    <div class="flex flex-wrap items-center justify-between gap-10px">
      <div class="flex min-w-0 items-center gap-8px">
        <div class="text-[13px] font-600 text-[#172033]">权限快照</div>
        <el-tag v-if="snapshotSummary" :type="resolveSnapshotStatusType(snapshotSummary.snapshotStatus)">
          快照状态：{{ resolveSnapshotStatusLabel(snapshotSummary.snapshotStatus) }}
        </el-tag>
        <el-tag v-else type="info">快照状态：未加载</el-tag>
        <el-tag v-if="snapshotSummary?.restoreSupported" type="success">可恢复</el-tag>
        <el-tag v-else-if="snapshotSummary" type="danger">恢复受阻</el-tag>
      </div>
      <div class="flex items-center gap-8px">
        <el-button
          size="small"
          :loading="snapshotLoading"
          :disabled="!taskId || !canManageAccessRule"
          @click="loadPermissionSnapshotSummary"
        >
          刷新权限状态
        </el-button>
        <el-button
          size="small"
          type="primary"
          plain
          :disabled="!taskId || !canManageAccessRule"
          @click="handleOpenPermissionRestore"
          v-hasPermi="['dcc:controlled-file:access-rule:manage']"
        >
          查看恢复
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="!canManageAccessRule"
      type="warning"
      title="缺少权限"
      description="需要 DCC 目录访问规则管理权限后才能查看 NAS 权限快照和执行恢复。"
      show-icon
      :closable="false"
      class="mt-10px"
    />

    <el-alert
      v-if="permissionState.errorMessage"
      type="error"
      title="权限快照或恢复接口返回错误"
      :description="permissionState.errorMessage"
      show-icon
      :closable="false"
      class="mt-10px"
    />

    <div
      v-if="snapshotSummary"
      class="mt-10px grid grid-cols-2 gap-8px text-[12px] text-[#4b5563] md:grid-cols-6"
    >
      <div>目录快照：{{ snapshotSummary.directorySnapshotCount }}</div>
      <div>ACE 数：{{ snapshotSummary.aceCount }}</div>
      <div>未映射主体：{{ snapshotSummary.unmappedPrincipalCount }}</div>
      <div>不支持权限：{{ snapshotSummary.unsupportedAceCount }}</div>
      <div>阻断项：{{ snapshotSummary.blockerCount }}</div>
      <div v-if="snapshotSummary.capturedAt">采集时间：{{ snapshotSummary.capturedAt }}</div>
    </div>

    <div
      v-if="snapshotSummary?.lastFailureMessage"
      class="mt-10px rounded-[6px] border border-[#f4d7a3] bg-[#fffaf0] px-10px py-8px text-[12px] text-[#9a5b00]"
    >
      快照失败原因：{{ snapshotSummary.lastFailureMessage }}
    </div>

    <el-drawer
      v-model="permissionState.drawerVisible"
      title="NAS 权限恢复"
      size="1080px"
      destroy-on-close
      @close="clearPermissionRestorePolling"
    >
      <div class="flex h-full flex-col gap-12px">
        <el-alert
          v-if="permissionState.errorMessage"
          type="error"
          title="权限恢复失败"
          :description="permissionState.errorMessage"
          show-icon
          :closable="false"
        />

        <div class="rounded-[6px] border border-[#dbe3ef] bg-[#fafcff] px-12px py-10px">
          <div class="mb-8px flex flex-wrap items-center justify-between gap-10px">
            <div class="text-[14px] font-600 text-[#172033]">任务权限状态</div>
            <div class="flex items-center gap-8px">
              <el-button size="small" :loading="snapshotLoading" @click="refreshPermissionRestoreData">
                刷新
              </el-button>
              <el-button
                size="small"
                type="primary"
                plain
                :loading="previewLoading"
                :disabled="!canPreviewPermissionRestore"
                @click="handlePreviewPermissionRestore"
              >
                恢复预览
              </el-button>
            </div>
          </div>
          <div
            v-if="snapshotSummary"
            class="grid grid-cols-2 gap-8px text-[12px] text-[#4b5563] md:grid-cols-4"
          >
            <div>任务编号：{{ snapshotSummary.taskId }}</div>
            <div>快照状态：{{ resolveSnapshotStatusLabel(snapshotSummary.snapshotStatus) }}</div>
            <div>目录快照：{{ snapshotSummary.directorySnapshotCount }}</div>
            <div>ACE 数：{{ snapshotSummary.aceCount }}</div>
            <div>未映射主体：{{ snapshotSummary.unmappedPrincipalCount }}</div>
            <div>不支持权限：{{ snapshotSummary.unsupportedAceCount }}</div>
            <div>阻断项：{{ snapshotSummary.blockerCount }}</div>
            <div>是否可恢复：{{ snapshotSummary.restoreSupported ? '是' : '否' }}</div>
          </div>
          <el-empty v-else description="尚未加载权限快照状态" />
        </div>

        <el-tabs v-model="activeTab" class="flex-1">
          <el-tab-pane label="权限快照" name="snapshot">
            <div class="mb-10px flex flex-wrap items-center justify-between gap-10px">
              <el-select
                v-model="snapshotQuery.status"
                clearable
                class="!w-180px"
                placeholder="筛选状态"
                @change="handleSnapshotFilterChange"
              >
                <el-option label="已采集" value="CAPTURED" />
                <el-option label="阻断" value="BLOCKED" />
                <el-option label="失败" value="FAILED" />
              </el-select>
              <el-button size="small" :loading="snapshotItemsLoading" @click="loadPermissionSnapshotItems">
                刷新列表
              </el-button>
            </div>
            <el-table
              v-loading="snapshotItemsLoading"
              :data="snapshotItems"
              empty-text="当前快照暂无目录行"
              max-height="320"
            >
              <el-table-column label="NAS 路径" min-width="260" prop="nasPath" show-overflow-tooltip />
              <el-table-column label="DCC 目录" width="110" prop="dccDirectoryId" />
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="resolveSnapshotStatusType(row.snapshotStatus)">
                    {{ resolveSnapshotStatusLabel(row.snapshotStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="ACE 数" width="90" prop="aceCount" />
              <el-table-column label="阻断项" min-width="260">
                <template #default="{ row }">
                  <div v-if="row.blockers?.length" class="space-y-4px">
                    <div
                      v-for="blocker in row.blockers"
                      :key="`${row.taskItemId}-${blocker.code}-${blocker.aceIndex ?? 'NA'}`"
                      class="truncate text-[12px] text-[#b42318]"
                      :title="blocker.message"
                    >
                      {{ blocker.code }}：{{ blocker.message }}
                    </div>
                  </div>
                  <span v-else class="text-[#6b7280]">无</span>
                </template>
              </el-table-column>
            </el-table>
            <div class="mt-10px flex justify-end">
              <el-pagination
                v-model:current-page="snapshotQuery.pageNo"
                v-model:page-size="snapshotQuery.pageSize"
                :total="snapshotTotal"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                @size-change="loadPermissionSnapshotItems"
                @current-change="loadPermissionSnapshotItems"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="身份映射" name="mapping">
            <el-alert
              type="warning"
              title="未映射主体会阻断恢复"
              description="请选择 NAS 主体类型和 DCC 授权对象后保存映射。系统不会默认选择同名用户、当前用户或管理员。"
              show-icon
              :closable="false"
              class="mb-10px"
            />
            <el-table
              v-loading="principalLoading"
              :data="unmappedPrincipals"
              empty-text="当前任务没有未映射主体"
              max-height="360"
            >
              <el-table-column label="NAS 主体" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">
                  <div class="text-[#172033]">{{ row.sourceName || row.sourceSid }}</div>
                  <div class="text-[12px] text-[#8a94a6]">{{ row.sourceSid }}</div>
                </template>
              </el-table-column>
              <el-table-column label="NAS 类型" width="140">
                <template #default="{ row }">
                  <el-select
                    v-model="getMappingDraft(row).accountType"
                    placeholder="请选择"
                    class="w-full"
                  >
                    <el-option
                      v-for="item in nasAccountTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="DCC 主体类型" width="150">
                <template #default="{ row }">
                  <el-select
                    v-model="getMappingDraft(row).targetSubjectType"
                    placeholder="请选择"
                    class="w-full"
                    @change="getMappingDraft(row).targetSubjectId = undefined"
                  >
                    <el-option
                      v-for="item in subjectTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="DCC 授权对象" min-width="220">
                <template #default="{ row }">
                  <el-select
                    v-model="getMappingDraft(row).targetSubjectId"
                    filterable
                    clearable
                    class="w-full"
                    placeholder="请选择授权对象"
                    :disabled="!getMappingDraft(row).targetSubjectType"
                  >
                    <el-option
                      v-for="item in getSubjectOptions(getMappingDraft(row).targetSubjectType)"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="出现次数" width="90" prop="aceCount" />
              <el-table-column label="首个路径" min-width="180" prop="firstNasPath" show-overflow-tooltip />
              <el-table-column label="变更原因" min-width="200">
                <template #default="{ row }">
                  <el-input
                    v-model="getMappingDraft(row).changeReason"
                    placeholder="填写映射依据"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="96" fixed="right">
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    :loading="getMappingDraft(row).saving"
                    :disabled="!canSaveMapping(row)"
                    @click="handleSavePrincipalMapping(row)"
                    v-hasPermi="['dcc:controlled-file:access-rule:manage']"
                  >
                    保存
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="恢复预览" name="restore">
            <div class="mb-10px flex flex-wrap items-center justify-between gap-10px">
              <div class="flex flex-wrap items-center gap-8px">
                <el-tag v-if="restorePreview" :type="restorePreview.canRestore ? 'success' : 'danger'">
                  {{ restorePreview.canRestore ? '可应用' : '恢复受阻' }}
                </el-tag>
                <el-tag v-if="restorePreview" type="info">
                  runtimeEnforcementReady：{{ restorePreview.runtimeEnforcementReady ? '是' : '否' }}
                </el-tag>
                <el-tag v-if="restoreStatus" :type="resolveRestoreStatusType(restoreStatus.status)">
                  恢复状态：{{ resolveRestoreStatusLabel(restoreStatus.status) }}
                </el-tag>
              </div>
              <div class="flex items-center gap-8px">
                <el-input
                  v-model="restoreChangeReason"
                  class="!w-260px"
                  placeholder="填写应用恢复原因"
                />
                <el-button
                  type="primary"
                  :loading="restoreApplyLoading"
                  :disabled="!canApplyRestore"
                  @click="handleApplyPermissionRestore"
                  v-hasPermi="['dcc:controlled-file:access-rule:manage']"
                >
                  应用恢复
                </el-button>
              </div>
            </div>

            <el-alert
              v-if="restorePreview?.runtimeEnforcementBlocker"
              type="error"
              title="运行时权限未就绪"
              :description="restorePreview.runtimeEnforcementBlocker"
              show-icon
              :closable="false"
              class="mb-10px"
            />
            <el-alert
              v-if="restoreStatus?.lastFailureMessage"
              type="error"
              title="恢复执行失败"
              :description="restoreStatus.lastFailureMessage"
              show-icon
              :closable="false"
              class="mb-10px"
            />

            <div
              v-if="restorePreview"
              class="mb-10px grid grid-cols-2 gap-8px rounded-[6px] border border-[#dbe3ef] bg-[#fafcff] px-12px py-10px text-[12px] text-[#4b5563] md:grid-cols-4"
            >
              <div>计划哈希：{{ restorePreview.planHash }}</div>
              <div>恢复模式：{{ restorePreview.restoreMode }}</div>
              <div>影响目录：{{ restorePreview.directoryCount }}</div>
              <div>计划规则：{{ restorePreview.ruleCount }}</div>
              <div v-if="restoreStatus">已完成目录：{{ restoreStatus.completedDirectoryCount }}</div>
              <div v-if="restoreStatus">失败目录：{{ restoreStatus.failedDirectoryCount }}</div>
              <div v-if="restoreStatus?.startedAt">开始时间：{{ restoreStatus.startedAt }}</div>
              <div v-if="restoreStatus?.completedAt">完成时间：{{ restoreStatus.completedAt }}</div>
            </div>

            <el-table
              v-if="restorePreview?.blockers?.length"
              :data="restorePreview.blockers"
              class="mb-10px"
              max-height="220"
            >
              <el-table-column label="阻断代码" width="180" prop="code" />
              <el-table-column label="NAS 路径" min-width="220" prop="nasPath" show-overflow-tooltip />
              <el-table-column label="主体" min-width="180" prop="trusteeSid" show-overflow-tooltip />
              <el-table-column label="原因" min-width="260" prop="message" show-overflow-tooltip />
            </el-table>

            <el-table
              :data="restoreSampleRules"
              empty-text="请先生成恢复预览"
              max-height="280"
            >
              <el-table-column label="NAS 路径" min-width="240" prop="nasPath" show-overflow-tooltip />
              <el-table-column label="DCC 目录" width="110" prop="directoryId" />
              <el-table-column label="主体类型" width="110" prop="subjectType" />
              <el-table-column label="主体 ID" width="100" prop="subjectId" />
              <el-table-column label="可查看" width="90">
                <template #default="{ row }">{{ row.canQuery ? '是' : '否' }}</template>
              </el-table-column>
              <el-table-column label="可预览" width="90">
                <template #default="{ row }">{{ row.canPreview ? '是' : '否' }}</template>
              </el-table-column>
              <el-table-column label="可下载" width="90">
                <template #default="{ row }">{{ row.canDownload ? '是' : '否' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import {
  applyNasPermissionRestore,
  getNasPermissionRestoreStatus,
  getNasPermissionSnapshotItems,
  getNasPermissionSnapshotSummary,
  getNasUnmappedPrincipals,
  previewNasPermissionRestore,
  saveNasPrincipalMapping,
  type NasPermissionRestorePreviewVO,
  type NasPermissionRestoreStatusVO,
  type NasPermissionRestoreApplyRespVO,
  type NasPermissionSnapshotItemVO,
  type NasPermissionSnapshotSummaryVO,
  type NasUnmappedPrincipalVO
} from '@/api/dcc/controlledFile/workflow'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { getSimplePostList, type PostVO } from '@/api/system/post'
import { getSimpleRoleList, type RoleVO } from '@/api/system/role'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { generateUUID } from '@/utils'
import { checkPermi } from '@/utils/permission'

defineOptions({ name: 'NasPermissionRestorePanel' })

const props = defineProps<{
  taskId?: number | string
  transferStatus?: string
}>()

interface SubjectOption {
  label: string
  value: number
}

interface MappingDraft {
  accountType?: string
  targetSubjectType?: string
  targetSubjectId?: number
  changeReason: string
  saving: boolean
}

const message = useMessage()
const snapshotSummary = ref<NasPermissionSnapshotSummaryVO | null>(null)
const snapshotItems = ref<NasPermissionSnapshotItemVO[]>([])
const snapshotTotal = ref(0)
const unmappedPrincipals = ref<NasUnmappedPrincipalVO[]>([])
const restorePreview = ref<NasPermissionRestorePreviewVO | null>(null)
const restoreStatus = ref<NasPermissionRestoreStatusVO | null>(null)
const restoreChangeReason = ref('')
const restoreIdempotencyKey = ref('')
const activeTab = ref('snapshot')
const snapshotLoading = ref(false)
const snapshotItemsLoading = ref(false)
const principalLoading = ref(false)
const previewLoading = ref(false)
const restoreApplyLoading = ref(false)
const users = ref<UserVO[]>([])
const depts = ref<DeptVO[]>([])
const roles = ref<RoleVO[]>([])
const posts = ref<PostVO[]>([])
const mappingDrafts = reactive<Record<string, MappingDraft>>({})
const subjectDataLoaded = ref(false)
const permissionState = reactive({
  drawerVisible: false,
  errorMessage: ''
})
const snapshotQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  status: ''
})
let restorePollingTimer: number | undefined
const canManageAccessRule = computed(() =>
  checkPermi(['dcc:controlled-file:access-rule:manage'])
)

const nasAccountTypeOptions = [
  { label: 'NAS 用户', value: 'USER' },
  { label: 'NAS 组', value: 'GROUP' }
]

const subjectTypeOptions = [
  { label: '用户', value: 'USER' },
  { label: '部门', value: 'DEPT' },
  { label: '角色', value: 'ROLE' },
  { label: '岗位', value: 'POSITION' }
]

const canApplyRestore = computed(() => {
  return (
    !!restorePreview.value?.canRestore &&
    !!restorePreview.value?.runtimeEnforcementReady &&
    !!restorePreview.value?.planHash &&
    !!restorePreview.value?.restoreMode &&
    !isRestoreStatusActive(restoreStatus.value?.status)
  )
})
const restoreSampleRules = computed(() =>
  restorePreview.value === null ? [] : restorePreview.value.sampleRules
)
const canPreviewPermissionRestore = computed(
  () => !!snapshotSummary.value && snapshotSummary.value.snapshotStatus === 'CAPTURED'
)

function requireArray<T>(value: T[] | undefined | null, fieldName: string): T[] {
  if (!Array.isArray(value)) {
    throw new Error(`${fieldName} 缺失或格式错误`)
  }
  return value
}

const requireNumber = (value: number | undefined | null, fieldName: string): number => {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    throw new Error(`${fieldName} 缺失或格式错误`)
  }
  return value
}

const requireString = (value: string | undefined | null, fieldName: string): string => {
  if (typeof value !== 'string' || !value.trim()) {
    throw new Error(`${fieldName} 缺失或格式错误`)
  }
  return value
}

const requireBoolean = (value: boolean | undefined | null, fieldName: string): boolean => {
  if (typeof value !== 'boolean') {
    throw new Error(`${fieldName} 缺失或格式错误`)
  }
  return value
}

const assertOptionalNumber = (value: number | undefined | null, fieldName: string) => {
  if (value !== undefined && value !== null && (typeof value !== 'number' || Number.isNaN(value))) {
    throw new Error(`${fieldName} 格式错误`)
  }
}

const assertSnapshotSummaryContract = (summary: NasPermissionSnapshotSummaryVO) => {
  requireNumber(summary.taskId, 'permissionSnapshot.taskId')
  requireString(summary.snapshotStatus, 'permissionSnapshot.snapshotStatus')
  requireArray(summary.selectedNasPaths, 'permissionSnapshot.selectedNasPaths')
  requireNumber(summary.directorySnapshotCount, 'permissionSnapshot.directorySnapshotCount')
  requireNumber(summary.aceCount, 'permissionSnapshot.aceCount')
  requireNumber(summary.unsupportedAceCount, 'permissionSnapshot.unsupportedAceCount')
  requireNumber(summary.unmappedPrincipalCount, 'permissionSnapshot.unmappedPrincipalCount')
  requireNumber(summary.blockerCount, 'permissionSnapshot.blockerCount')
  requireBoolean(summary.restoreSupported, 'permissionSnapshot.restoreSupported')
}

const assertSnapshotItemContract = (item: NasPermissionSnapshotItemVO, index: number) => {
  requireNumber(item.taskItemId, `permissionSnapshot.items.list[${index}].taskItemId`)
  requireString(item.nasPath, `permissionSnapshot.items.list[${index}].nasPath`)
  assertOptionalNumber(item.dccDirectoryId, `permissionSnapshot.items.list[${index}].dccDirectoryId`)
  requireString(item.snapshotStatus, `permissionSnapshot.items.list[${index}].snapshotStatus`)
  requireNumber(item.aceCount, `permissionSnapshot.items.list[${index}].aceCount`)
  requireArray(item.blockers, `permissionSnapshot.items.list[${index}].blockers`).forEach(
    (blocker, blockerIndex) => {
      requireString(blocker.code, `permissionSnapshot.items.list[${index}].blockers[${blockerIndex}].code`)
      requireString(blocker.message, `permissionSnapshot.items.list[${index}].blockers[${blockerIndex}].message`)
      assertOptionalNumber(
        blocker.aceIndex,
        `permissionSnapshot.items.list[${index}].blockers[${blockerIndex}].aceIndex`
      )
    }
  )
}

const assertUnmappedPrincipalContract = (principal: NasUnmappedPrincipalVO, index: number) => {
  requireString(principal.sourceSid, `nasPermission.unmappedPrincipals.list[${index}].sourceSid`)
  requireNumber(principal.aceCount, `nasPermission.unmappedPrincipals.list[${index}].aceCount`)
}

const assertRestoreRulePreviewContract = (
  rule: NasPermissionRestorePreviewVO['sampleRules'][number],
  index: number
) => {
  requireNumber(rule.directoryId, `permissionRestore.sampleRules[${index}].directoryId`)
  requireString(rule.nasPath, `permissionRestore.sampleRules[${index}].nasPath`)
  requireString(rule.subjectType, `permissionRestore.sampleRules[${index}].subjectType`)
  requireNumber(rule.subjectId, `permissionRestore.sampleRules[${index}].subjectId`)
  requireBoolean(rule.canQuery, `permissionRestore.sampleRules[${index}].canQuery`)
  requireBoolean(rule.canPreview, `permissionRestore.sampleRules[${index}].canPreview`)
  requireBoolean(rule.canDownload, `permissionRestore.sampleRules[${index}].canDownload`)
}

const assertRestoreBlockerContract = (
  blocker: NasPermissionRestorePreviewVO['blockers'][number],
  index: number
) => {
  requireString(blocker.code, `permissionRestore.blockers[${index}].code`)
  requireString(blocker.message, `permissionRestore.blockers[${index}].message`)
  assertOptionalNumber(blocker.directorySnapshotId, `permissionRestore.blockers[${index}].directorySnapshotId`)
}

const assertRestorePreviewContract = (preview: NasPermissionRestorePreviewVO) => {
  requireNumber(preview.taskId, 'permissionRestore.taskId')
  requireBoolean(preview.canRestore, 'permissionRestore.canRestore')
  requireString(preview.planHash, 'permissionRestore.planHash')
  requireString(preview.restoreMode, 'permissionRestore.restoreMode')
  requireNumber(preview.directoryCount, 'permissionRestore.directoryCount')
  requireNumber(preview.ruleCount, 'permissionRestore.ruleCount')
  requireBoolean(preview.runtimeEnforcementReady, 'permissionRestore.runtimeEnforcementReady')
  requireArray(preview.blockers, 'permissionRestore.blockers').forEach(
    assertRestoreBlockerContract
  )
  requireArray(preview.sampleRules, 'permissionRestore.sampleRules').forEach(
    assertRestoreRulePreviewContract
  )
}

const assertRestoreApplyContract = (result: NasPermissionRestoreApplyRespVO) => {
  requireNumber(result.restoreId, 'permissionRestore.apply.restoreId')
  requireNumber(result.taskId, 'permissionRestore.apply.taskId')
  requireString(result.status, 'permissionRestore.apply.status')
  requireNumber(result.directoryCount, 'permissionRestore.apply.directoryCount')
  requireNumber(result.ruleCount, 'permissionRestore.apply.ruleCount')
  requireNumber(result.completedDirectoryCount, 'permissionRestore.apply.completedDirectoryCount')
  requireNumber(result.failedDirectoryCount, 'permissionRestore.apply.failedDirectoryCount')
}

const assertRestoreStatusContract = (result: NasPermissionRestoreStatusVO) => {
  assertRestoreApplyContract(result)
}

const resolveSnapshotStatusLabel = (status?: string | null) => {
  if (status === 'NOT_COLLECTED') return '未采集'
  if (status === 'CAPTURED') return '已采集'
  if (status === 'RUNNING') return '采集中'
  if (status === 'BLOCKED') return '阻断'
  if (status === 'FAILED') return '失败'
  if (status === 'SUCCESS') return '成功'
  return status || '未知'
}

const resolveSnapshotStatusType = (status?: string | null) => {
  if (status === 'CAPTURED' || status === 'SUCCESS') return 'success'
  if (status === 'RUNNING') return 'primary'
  if (status === 'BLOCKED') return 'warning'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

const isSnapshotReadyForDetail = (status?: string | null) =>
  ['CAPTURED', 'FAILED'].includes(status || '')

const resolveRestoreStatusLabel = (status?: string | null) => {
  if (status === 'READY' || status === 'WAITING') return '等待执行'
  if (status === 'EXECUTING') return '执行中'
  if (status === 'COMPLETED') return '已完成'
  if (status === 'FAILED') return '已失败'
  return status || '未知'
}

const resolveRestoreStatusType = (status?: string | null) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'EXECUTING') return 'primary'
  if (status === 'READY' || status === 'WAITING') return 'warning'
  return 'info'
}

const isRestoreStatusActive = (status?: string | null) =>
  ['READY', 'WAITING', 'EXECUTING'].includes(status || '')

const normalizeErrorMessage = (error: any, fallback: string) => error?.message || fallback

const loadPermissionSnapshotSummary = async () => {
  if (!props.taskId || !canManageAccessRule.value) {
    return null
  }
  snapshotLoading.value = true
  try {
    const summary = await getNasPermissionSnapshotSummary(props.taskId)
    assertSnapshotSummaryContract(summary)
    snapshotSummary.value = summary
    permissionState.errorMessage = ''
    return summary
  } catch (error: any) {
    snapshotSummary.value = null
    permissionState.errorMessage = normalizeErrorMessage(error, '权限快照状态获取失败')
    return null
  } finally {
    snapshotLoading.value = false
  }
}

const loadPermissionSnapshotItems = async () => {
  if (!props.taskId || !canManageAccessRule.value) {
    return
  }
  if (snapshotSummary.value && !isSnapshotReadyForDetail(snapshotSummary.value.snapshotStatus)) {
    snapshotItems.value = []
    snapshotTotal.value = 0
    return
  }
  snapshotItemsLoading.value = true
  try {
    const page = await getNasPermissionSnapshotItems(props.taskId, {
      pageNo: snapshotQuery.pageNo,
      pageSize: snapshotQuery.pageSize,
      status: snapshotQuery.status || undefined
    })
    const items = requireArray(page.list, 'permissionSnapshot.items.list')
    items.forEach(assertSnapshotItemContract)
    snapshotItems.value = items
    snapshotTotal.value = requireNumber(page.total, 'permissionSnapshot.items.total')
    permissionState.errorMessage = ''
  } catch (error: any) {
    snapshotItems.value = []
    snapshotTotal.value = 0
    permissionState.errorMessage = normalizeErrorMessage(error, '权限快照目录列表获取失败')
  } finally {
    snapshotItemsLoading.value = false
  }
}

const loadSubjectData = async () => {
  if (subjectDataLoaded.value) {
    return
  }
  const [userList, deptList, roleList, postList] = await Promise.all([
    getSimpleUserList(),
    getSimpleDeptList(),
    getSimpleRoleList(),
    getSimplePostList()
  ])
  users.value = userList
  depts.value = deptList
  roles.value = roleList
  posts.value = postList
  subjectDataLoaded.value = true
}

const loadUnmappedPrincipals = async () => {
  if (!props.taskId || !canManageAccessRule.value) {
    return
  }
  if (snapshotSummary.value && !isSnapshotReadyForDetail(snapshotSummary.value.snapshotStatus)) {
    unmappedPrincipals.value = []
    return
  }
  principalLoading.value = true
  try {
    const result = await getNasUnmappedPrincipals(props.taskId)
    const principals = requireArray(result.list, 'nasPermission.unmappedPrincipals.list')
    principals.forEach(assertUnmappedPrincipalContract)
    unmappedPrincipals.value = principals
    for (const principal of unmappedPrincipals.value) {
      getMappingDraft(principal)
    }
    permissionState.errorMessage = ''
  } catch (error: any) {
    unmappedPrincipals.value = []
    permissionState.errorMessage = normalizeErrorMessage(error, '未映射主体获取失败')
  } finally {
    principalLoading.value = false
  }
}

const refreshPermissionRestoreData = async () => {
  const summary = await loadPermissionSnapshotSummary()
  if (!summary || !isSnapshotReadyForDetail(summary.snapshotStatus)) {
    snapshotItems.value = []
    snapshotTotal.value = 0
    unmappedPrincipals.value = []
    return
  }
  await Promise.all([loadPermissionSnapshotItems(), loadUnmappedPrincipals()])
}

const handleOpenPermissionRestore = async () => {
  if (!canManageAccessRule.value) {
    permissionState.errorMessage = '缺少 DCC 目录访问规则管理权限'
    return
  }
  permissionState.drawerVisible = true
  activeTab.value = 'snapshot'
  try {
    await loadSubjectData()
    await refreshPermissionRestoreData()
  } catch (error: any) {
    permissionState.errorMessage = normalizeErrorMessage(error, '权限恢复基础数据加载失败')
  }
}

const handleSnapshotFilterChange = () => {
  snapshotQuery.pageNo = 1
  loadPermissionSnapshotItems()
}

const getMappingDraftKey = (principal: NasUnmappedPrincipalVO) => principal.sourceSid

const getMappingDraft = (principal: NasUnmappedPrincipalVO) => {
  const key = getMappingDraftKey(principal)
  if (!mappingDrafts[key]) {
    mappingDrafts[key] = {
      changeReason: '',
      saving: false
    }
  }
  return mappingDrafts[key]
}

const canSaveMapping = (principal: NasUnmappedPrincipalVO) => {
  const draft = getMappingDraft(principal)
  return !!draft.accountType && !!draft.targetSubjectType && !!draft.targetSubjectId && !draft.saving
}

const getUserSubjectLabel = (item: UserVO) => {
  return item.deptName ? `${item.nickname}（${item.deptName}）` : item.nickname
}

const getSubjectOptions = (targetSubjectType?: string): SubjectOption[] => {
  switch (targetSubjectType) {
    case 'USER':
      return users.value.map((item) => ({ label: getUserSubjectLabel(item), value: item.id }))
    case 'DEPT':
      return depts.value.map((item) => ({ label: item.name, value: item.id }))
    case 'ROLE':
      return roles.value.map((item) => ({ label: item.name, value: item.id }))
    case 'POSITION':
      return posts.value
        .filter((item) => item.id !== undefined)
        .map((item) => ({ label: item.name, value: Number(item.id) }))
    default:
      return []
  }
}

const handleSavePrincipalMapping = async (principal: NasUnmappedPrincipalVO) => {
  const draft = getMappingDraft(principal)
  if (!canSaveMapping(principal)) {
    message.warning('请先选择 NAS 类型和 DCC 授权对象')
    return
  }
  draft.saving = true
  try {
    await saveNasPrincipalMapping({
      sourceAuthority: principal.sourceAuthority,
      sourceSid: principal.sourceSid,
      sourceName: principal.sourceName,
      accountName: principal.sourceName,
      accountType: draft.accountType as string,
      targetSubjectType: draft.targetSubjectType as string,
      targetSubjectId: draft.targetSubjectId as number,
      active: true,
      changeReason: draft.changeReason || null
    })
    message.success('身份映射已保存')
    await Promise.all([loadPermissionSnapshotSummary(), loadUnmappedPrincipals()])
    restorePreview.value = null
    restoreIdempotencyKey.value = ''
  } catch (error: any) {
    permissionState.errorMessage = normalizeErrorMessage(error, '身份映射保存失败')
  } finally {
    draft.saving = false
  }
}

const handlePreviewPermissionRestore = async () => {
  if (!props.taskId || !canManageAccessRule.value) {
    return
  }
  if (!snapshotSummary.value) {
    await loadPermissionSnapshotSummary()
  }
  if (!snapshotSummary.value || snapshotSummary.value.snapshotStatus !== 'CAPTURED') {
    restorePreview.value = null
    activeTab.value = 'snapshot'
    permissionState.errorMessage = '权限快照尚未采集完成，无法生成恢复预览'
    return
  }
  previewLoading.value = true
  try {
    const preview = await previewNasPermissionRestore(props.taskId)
    assertRestorePreviewContract(preview)
    restorePreview.value = preview
    restoreIdempotencyKey.value = ''
    activeTab.value = 'restore'
    permissionState.errorMessage = ''
    if (!restorePreview.value.canRestore) {
      message.warning('恢复预览存在阻断项，暂不能应用恢复')
    }
  } catch (error: any) {
    restorePreview.value = null
    permissionState.errorMessage = normalizeErrorMessage(error, '权限恢复预览生成失败')
  } finally {
    previewLoading.value = false
  }
}

const buildRestoreIdempotencyKey = () => {
  if (!restoreIdempotencyKey.value) {
    restoreIdempotencyKey.value = `nas-permission-restore-${props.taskId}-${generateUUID()}`
  }
  return restoreIdempotencyKey.value
}

const handleApplyPermissionRestore = async () => {
  if (!props.taskId || !restorePreview.value || !canApplyRestore.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `即将按恢复预览写入 DCC 目录访问规则，影响 ${restorePreview.value.directoryCount} 个目录、${restorePreview.value.ruleCount} 条规则。`,
      '应用 NAS 权限恢复',
      {
        confirmButtonText: '确认应用',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  restoreApplyLoading.value = true
  try {
    const applyResult = await applyNasPermissionRestore(props.taskId, {
      idempotencyKey: buildRestoreIdempotencyKey(),
      planHash: restorePreview.value.planHash,
      restoreMode: restorePreview.value.restoreMode,
      changeReason: restoreChangeReason.value || null
    })
    assertRestoreApplyContract(applyResult)
    restoreStatus.value = applyResult
    restoreIdempotencyKey.value = ''
    permissionState.errorMessage = ''
    message.success('权限恢复任务已创建')
    pollPermissionRestoreStatus(applyResult.restoreId)
  } catch (error: any) {
    permissionState.errorMessage = normalizeErrorMessage(error, '权限恢复应用失败')
  } finally {
    restoreApplyLoading.value = false
  }
}

const clearPermissionRestorePolling = () => {
  if (restorePollingTimer) {
    window.clearTimeout(restorePollingTimer)
    restorePollingTimer = undefined
  }
}

const pollPermissionRestoreStatus = (restoreId: number | string) => {
  if (!props.taskId || !canManageAccessRule.value) {
    return
  }
  clearPermissionRestorePolling()
  restorePollingTimer = window.setTimeout(async () => {
    try {
      const statusResult = await getNasPermissionRestoreStatus(props.taskId as number | string, restoreId)
      assertRestoreStatusContract(statusResult)
      restoreStatus.value = statusResult
      permissionState.errorMessage = ''
      if (isRestoreStatusActive(restoreStatus.value.status)) {
        pollPermissionRestoreStatus(restoreId)
        return
      }
      if (restoreStatus.value.status === 'COMPLETED') {
        message.success('权限恢复已完成')
      } else if (restoreStatus.value.status === 'FAILED') {
        permissionState.errorMessage = restoreStatus.value.lastFailureMessage || '权限恢复执行失败'
      }
      await Promise.all([loadPermissionSnapshotSummary(), loadPermissionSnapshotItems()])
    } catch (error: any) {
      permissionState.errorMessage = normalizeErrorMessage(error, '权限恢复状态获取失败')
    }
  }, 3000)
}

watch(
  () => props.taskId,
  async (taskId) => {
    clearPermissionRestorePolling()
    snapshotSummary.value = null
    snapshotItems.value = []
    unmappedPrincipals.value = []
    restorePreview.value = null
    restoreStatus.value = null
    permissionState.errorMessage = ''
    if (taskId && canManageAccessRule.value) {
      await loadPermissionSnapshotSummary()
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  clearPermissionRestorePolling()
})
</script>
