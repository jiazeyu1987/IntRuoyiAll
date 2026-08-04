<template>
  <ContentWrap :bodyStyle="{ padding: '10px 20px 0' }" class="position-relative">
    <div class="processInstance-wrap-main">
      <el-scrollbar>
        <img
          class="position-absolute right-20px"
          width="150"
          :src="auditIconsMap[processInstance.status]"
          alt=""
        />
        <div class="flex">
          <div class="text-#878c93 h-15px">编号：{{ id }}</div>
          <Icon icon="ep:printer" class="ml-15px cursor-pointer" @click="handlePrint" />
        </div>
        <el-divider class="!my-8px" />
        <div class="flex items-center gap-5 mb-10px h-40px">
          <div class="text-26px font-bold mb-5px">{{ processInstanceDisplayName }}</div>
          <dict-tag
            v-if="processInstance.status"
            :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS"
            :value="processInstance.status"
          />
        </div>

        <div class="flex items-center gap-5 mb-10px text-13px h-35px">
          <div
            class="bg-gray-100 h-35px rounded-3xl flex items-center p-8px gap-2 dark:color-gray-600"
          >
            <el-avatar
              :size="28"
              v-if="processInstance?.startUser?.avatar"
              :src="processInstance?.startUser?.avatar"
            />
            <el-avatar :size="28" v-else-if="processInstance?.startUser?.nickname">
              {{ processInstance?.startUser?.nickname.substring(0, 1) }}
            </el-avatar>
            {{ processInstance?.startUser?.nickname }}
          </div>
          <div class="text-#878c93"> {{ formatDate(processInstance.startTime) }} 提交 </div>
        </div>

        <div class="form-scroll-area">
              <el-scrollbar>
                <el-row>
                  <el-col :span="17" class="!flex !flex-col formCol">
                    <!-- 表单信息 -->
                    <div
                      v-loading="processInstanceLoading"
                      class="form-box flex flex-col mb-30px flex-1"
                    >
                      <!-- 情况一：流程表单 -->
                      <el-col v-if="processDefinition?.formType === BpmModelFormType.NORMAL">
                        <form-create
                          v-model="detailForm.value"
                          v-model:api="fApi"
                          :option="detailForm.option"
                          :rule="detailForm.rule"
                        />
                      </el-col>
                      <!-- 情况二：业务表单 -->
                      <div v-if="processDefinition?.formType === BpmModelFormType.CUSTOM">
                        <div
                          v-if="isDccControlledFileCustomForm"
                          class="bpm-dcc-approval-summary"
                          data-testid="bpm-dcc-approval-compact-summary"
                          v-loading="dccApprovalFileLoading"
                        >
                          <div class="bpm-dcc-approval-summary__head">
                            <div>
                              <div class="bpm-dcc-approval-summary__eyebrow">审核内容</div>
                              <div class="bpm-dcc-approval-summary__title">
                                {{
                                  dccApprovalFileDetail?.title ||
                                  processInstanceDisplayName ||
                                  '文控受控文件审批'
                                }}
                              </div>
                              <div class="bpm-dcc-approval-summary__subtitle">
                                审核页默认只展示审批判断所需信息；项目代码联动、受控浏览落位和排障详情请在文控处理页查看。
                              </div>
                            </div>
                            <el-tag type="success" effect="plain">精简审核视图</el-tag>
                          </div>
                          <el-alert
                            v-if="dccApprovalFileError"
                            class="mb-12px"
                            type="error"
                            show-icon
                            :closable="false"
                            title="审核内容加载失败"
                            :description="dccApprovalFileError"
                          />
                          <el-descriptions :column="2" border>
                            <el-descriptions-item label="文件标题">
                              {{ dccApprovalFileDetail?.title || '-' }}
                            </el-descriptions-item>
                            <el-descriptions-item label="文件编号">
                              {{ dccApprovalFileDetail?.fileNumber || '-' }}
                            </el-descriptions-item>
                            <el-descriptions-item label="版本">
                              {{ dccApprovalFileDetail?.versionNo || '-' }}
                            </el-descriptions-item>
                            <el-descriptions-item label="生效日期">
                              {{ dccApprovalFileDetail?.effectiveDate || '-' }}
                            </el-descriptions-item>
                            <el-descriptions-item label="提交人">
                              {{ processInstance?.startUser?.nickname || '-' }}
                            </el-descriptions-item>
                            <el-descriptions-item label="提交时间">
                              {{ formatDate(processInstance.startTime) || '-' }}
                            </el-descriptions-item>
                            <el-descriptions-item label="当前步骤">
                              {{ currentApprovalStepLabel }}
                            </el-descriptions-item>
                            <el-descriptions-item label="当前处理人">
                              {{ currentApprovalActorText }}
                            </el-descriptions-item>
                          </el-descriptions>
                          <div class="bpm-dcc-approval-summary__actions">
                            <el-button
                              type="primary"
                              :disabled="!dccControlledFileBusinessId"
                              @click="openDccControlledFileApprovalDetail"
                            >
                              进入文控审批处理页
                            </el-button>
                            <span>需要预览文件、电子签名、通过或拒绝时，从这里进入正式处理入口。</span>
                          </div>
                        </div>
                        <BusinessFormComponent v-else :id="processInstance.businessKey" />
                      </div>
                    </div>
                  </el-col>
                  <el-col :span="7">
                    <!-- 审批记录时间线 -->
                    <ProcessInstanceTimeline :activity-nodes="activityNodes" />
                  </el-col>
                </el-row>
              </el-scrollbar>
            </div>

        <div class="b-t-solid border-t-1px border-[var(--el-border-color)]">
          <!-- 操作栏按钮 -->
          <ProcessInstanceOperationButton
            ref="operationButtonRef"
            :process-instance="processInstance"
            :process-definition="processDefinition"
            :userOptions="userOptions"
            :normal-form="detailForm"
            :normal-form-api="fApi"
            :writable-fields="writableFields"
            @success="refresh"
          />
        </div>
      </el-scrollbar>
    </div>
  </ContentWrap>

  <!-- 打印预览弹窗 -->
  <PrintDialog ref="printRef" />
</template>
<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import { DICT_TYPE } from '@/utils/dict'
import { BpmModelFormType } from '@/utils/constants'
import { setConfAndFields2 } from '@/utils/formCreate'
import { registerComponent } from '@/utils/routerHelper'
import type { ApiAttrs } from '@form-create/element-ui/types/config'
import * as ProcessInstanceApi from '@/api/bpm/processInstance'
import * as UserApi from '@/api/system/user'
import { getControlledFile, type ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import ProcessInstanceOperationButton from './ProcessInstanceOperationButton.vue'
import ProcessInstanceTimeline from './ProcessInstanceTimeline.vue'
import { FieldPermissionType } from '@/components/SimpleProcessDesignerV2/src/consts'
import { TaskStatusEnum } from '@/api/bpm/task'
import runningSvg from '@/assets/svgs/bpm/running.svg'
import approveSvg from '@/assets/svgs/bpm/approve.svg'
import rejectSvg from '@/assets/svgs/bpm/reject.svg'
import cancelSvg from '@/assets/svgs/bpm/cancel.svg'
import PrintDialog from './PrintDialog.vue'
import { resolveDccTimelineActivityName } from '@/views/dcc/controlled-file/shared/stage-name'

defineOptions({ name: 'BpmProcessInstanceDetail' })
const props = defineProps<{
  id: string // 流程实例的编号
  taskId?: string // 任务编号
  activityId?: string //流程活动编号，用于抄送查看
}>()
const message = useMessage() // 消息弹窗
const route = useRoute()
const router = useRouter()
const processInstanceLoading = ref(false) // 流程实例的加载中
const processInstance = ref<any>({}) // 流程实例
const processDefinition = ref<any>({}) // 流程定义
const operationButtonRef = ref() // 操作按钮组件 ref
const auditIconsMap = {
  [TaskStatusEnum.RUNNING]: runningSvg,
  [TaskStatusEnum.APPROVE]: approveSvg,
  [TaskStatusEnum.REJECT]: rejectSvg,
  [TaskStatusEnum.CANCEL]: cancelSvg
}

const DCC_APPROVAL_PROCESS_TITLE_LABELS: Record<string, string> = {
  'DCC Controlled File Approval': '文控受控文件审批'
}

const resolveProcessInstanceDisplayName = (value?: string) => {
  const normalized = String(value || '').trim()
  return normalized ? DCC_APPROVAL_PROCESS_TITLE_LABELS[normalized] || normalized : ''
}

const processInstanceDisplayName = computed(() =>
  resolveProcessInstanceDisplayName(processInstance.value?.name)
)

// ========== 申请信息 ==========
const fApi = ref<ApiAttrs>() //
const detailForm = ref({
  rule: [],
  option: {},
  value: {}
}) // 流程实例的表单详情

const writableFields: Array<string> = [] // 表单可以编辑的字段

/** 获得详情 */
const getDetail = () => {
  // 获得审批详情
  getApprovalDetail()
}

/** 加载流程实例 */
const BusinessFormComponent = ref<any>(null) // 异步组件
const dccApprovalFileDetail = ref<ControlledFileVO>()
const dccApprovalFileLoading = ref(false)
const dccApprovalFileError = ref('')

const normalizeCustomViewPath = (value?: string) =>
  String(value || '')
    .trim()
    .replace(/^@\/views\//, '')
    .replace(/^src\/views\//, '')
    .replace(/^views\//, '')
    .replace(/^\/+/, '')
    .replace(/\.vue$/, '')
    .replace(/\/index$/, '')

const isDccControlledFileCustomForm = computed(
  () =>
    normalizeCustomViewPath(processDefinition.value?.formCustomViewPath) ===
    'dcc/controlled-file/detail'
)

const dccControlledFileBusinessId = computed(() => {
  const value = String(processInstance.value?.businessKey || '').trim()
  return /^\d+$/.test(value) ? value : ''
})

const currentApprovalNodes = computed(() =>
  activityNodes.value.filter((node) =>
    [TaskStatusEnum.WAIT, TaskStatusEnum.RUNNING, TaskStatusEnum.APPROVING].includes(
      node.status as TaskStatusEnum
    )
  )
)

const currentApprovalStepLabel = computed(() => {
  const currentNode = currentApprovalNodes.value[0]
  if (currentNode) {
    return resolveDccTimelineActivityName(currentNode.id, currentNode.name)
  }
  if (processInstance.value?.status === TaskStatusEnum.APPROVE) {
    return '流程已完成'
  }
  return '-'
})

const getUserDisplayName = (user?: ProcessInstanceApi.User) => {
  if (!user) {
    return ''
  }
  return user.nickname || (user.id ? `用户#${user.id}` : '')
}

const currentApprovalActorText = computed(() => {
  const actorNames = currentApprovalNodes.value.flatMap((node) => {
    const taskActors = (node.tasks || [])
      .flatMap((task) => [getUserDisplayName(task.assigneeUser), getUserDisplayName(task.ownerUser)])
      .filter(Boolean)
    const candidateActors = (node.candidateUsers || []).map(getUserDisplayName).filter(Boolean)
    return [...taskActors, ...candidateActors]
  })
  return Array.from(new Set(actorNames)).join('、') || '-'
})

const resetDccApprovalFileSummary = () => {
  dccApprovalFileDetail.value = undefined
  dccApprovalFileError.value = ''
  dccApprovalFileLoading.value = false
}

const resolveDccApprovalFileError = (error: unknown) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return '受控文件审核内容加载失败，请联系文控管理员。'
}

const loadDccApprovalFileSummary = async () => {
  resetDccApprovalFileSummary()
  if (!dccControlledFileBusinessId.value) {
    dccApprovalFileError.value = '流程业务单据缺少受控文件 ID，无法展示审核内容。'
    return
  }
  dccApprovalFileLoading.value = true
  try {
    dccApprovalFileDetail.value = await getControlledFile(dccControlledFileBusinessId.value)
  } catch (error) {
    dccApprovalFileError.value = resolveDccApprovalFileError(error)
  } finally {
    dccApprovalFileLoading.value = false
  }
}

const openDccControlledFileApprovalDetail = () => {
  if (!dccControlledFileBusinessId.value) {
    message.warning('缺少受控文件 ID，无法进入文控审批处理页。')
    return
  }
  router.push({
    path: `/dcc/controlled-file/detail/${dccControlledFileBusinessId.value}`,
    query: {
      handling: 'approval',
      from: 'approval-center',
      processInstanceId: props.id,
      ...(props.taskId ? { taskId: props.taskId } : {}),
      ...(route.fullPath ? { returnTo: encodeURIComponent(route.fullPath) } : {})
    }
  })
}

/** 获取审批详情 */
const activityNodes = ref<ProcessInstanceApi.ApprovalNodeInfo[]>([]) // 审批节点信息
const getApprovalDetail = async () => {
  processInstanceLoading.value = true
  try {
    const param = {
      processInstanceId: props.id,
      activityId: props.activityId,
      taskId: props.taskId
    }
    const data = await ProcessInstanceApi.getApprovalDetail(param)
    if (!data) {
      message.error('查询不到审批详情信息！')
      return
    }
    if (!data.processDefinition || !data.processInstance) {
      message.error('查询不到流程信息！')
      return
    }
    processInstance.value = data.processInstance
    processDefinition.value = data.processDefinition
    activityNodes.value = data.activityNodes || []

    // 设置表单信息
    if (processDefinition.value.formType === BpmModelFormType.NORMAL) {
      resetDccApprovalFileSummary()
      // 获取表单字段权限
      const formFieldsPermission = data.formFieldsPermission
      // 清空可编辑字段为空
      writableFields.splice(0)
      if (detailForm.value.rule?.length > 0) {
        // 避免刷新 form-create 显示不了
        detailForm.value.value = processInstance.value.formVariables
      } else {
        setConfAndFields2(
          detailForm,
          processDefinition.value.formConf,
          processDefinition.value.formFields,
          processInstance.value.formVariables
        )
      }
      nextTick().then(() => {
        fApi.value?.btn.show(false)
        fApi.value?.resetBtn.show(false)
        //@ts-ignore
        fApi.value?.disabled(true)
        // 设置表单字段权限
        if (formFieldsPermission) {
          Object.keys(data.formFieldsPermission).forEach((item) => {
            setFieldPermission(item, formFieldsPermission[item])
          })
        }
      })
    } else if (isDccControlledFileCustomForm.value) {
      BusinessFormComponent.value = null
      void loadDccApprovalFileSummary()
    } else {
      resetDccApprovalFileSummary()
      // 注意：data.processDefinition.formCustomViewPath 是组件的全路径，例如说：/crm/contract/detail/index.vue
      BusinessFormComponent.value = registerComponent(data.processDefinition.formCustomViewPath)
    }

    // 获取审批节点，显示 Timeline 的数据
    activityNodes.value = data.activityNodes || []

    // 获取待办任务显示操作按钮
    operationButtonRef.value?.loadTodoTask(data.todoTask)
  } finally {
    processInstanceLoading.value = false
  }
}

/** 设置表单权限 */
const setFieldPermission = (field: string, permission: string) => {
  if (permission === FieldPermissionType.READ) {
    //@ts-ignore
    fApi.value?.disabled(true, field)
  }
  if (permission === FieldPermissionType.WRITE) {
    //@ts-ignore
    fApi.value?.disabled(false, field)
    // 加入可以编辑的字段
    writableFields.push(field)
  }
  if (permission === FieldPermissionType.NONE) {
    //@ts-ignore
    fApi.value?.hidden(true, field)
  }
}

/** 操作成功后刷新 */
const refresh = () => {
  // 重新获取详情
  getDetail()
}

/** 处理打印 */
const printRef = ref()
const handlePrint = async () => {
  printRef.value.open(props.id)
}

/** 初始化 */
const userOptions = ref<UserApi.UserVO[]>([]) // 用户列表
onMounted(async () => {
  getDetail()
  // 获得用户列表
  userOptions.value = await UserApi.getSimpleUserList()
})
</script>

<style lang="scss" scoped>
$wrap-padding-height: 20px;
$wrap-margin-height: 15px;
$button-height: 51px;
$process-header-height: 194px;

.processInstance-wrap-main {
  height: calc(
    100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-footer-height) - 35px
  );
  max-height: calc(
    100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-footer-height) - 35px
  );
  overflow: auto;

  .form-scroll-area {
    display: flex;
    height: calc(
      100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-footer-height) - 35px -
        $process-header-height - 40px
    );
    max-height: calc(
      100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-footer-height) - 35px -
        $process-header-height - 40px
    );
    overflow: auto;
    flex-direction: column;

    :deep(.box-card) {
      height: 100%;
      flex: 1;

      .el-card__body {
        height: 100%;
        padding: 0;
      }
    }
  }
}

.form-box {
  :deep(.el-card) {
    border: none;
  }
}

.bpm-dcc-approval-summary {
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: #fff;
  padding: 18px;
}

.bpm-dcc-approval-summary__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.bpm-dcc-approval-summary__eyebrow {
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 6px;
}

.bpm-dcc-approval-summary__title {
  color: var(--el-text-color-primary);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.35;
}

.bpm-dcc-approval-summary__subtitle {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-top: 8px;
}

.bpm-dcc-approval-summary__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-top: 16px;
}
</style>
