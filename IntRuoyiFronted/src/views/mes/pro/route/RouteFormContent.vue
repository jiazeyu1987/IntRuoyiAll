<!-- MES 工艺路线表单内容 -->
<template>
  <div class="route-form-content" :class="{ 'is-page': mode === 'page' }">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
      :disabled="isDetail"
    >
      <el-tabs
        v-model="activeTab"
        class="route-form-content__tabs"
        @tab-change="handleRouteTabChange"
      >
        <el-tab-pane label="基础信息" name="basic">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="编码" prop="code">
                <el-input v-model="formData.code" placeholder="请输入编码" :disabled="isHeaderReadonly">
                  <template #append>
                    <el-button :disabled="isHeaderReadonly" @click="generateCode"> 生成 </el-button>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="名称" prop="name">
                <el-input
                  v-model="formData.name"
                  placeholder="请输入名称"
                  :disabled="isHeaderReadonly"
                />
              </el-form-item>
            </el-col>
            <el-col v-if="formData.id" :span="12">
              <el-form-item label="负责人" prop="ownerName">
                <el-autocomplete
                  v-model="formData.ownerName"
                  :fetch-suggestions="fetchOwnerSuggestions"
                  placeholder="请输入负责人"
                  clearable
                  trigger-on-focus
                  :disabled="isHeaderReadonly"
                  @select="handleOwnerCandidateSelect"
                >
                  <template #default="{ item }">
                    <div class="route-owner-suggestion">
                      <span class="route-owner-suggestion__name">{{ item.value }}</span>
                      <span class="route-owner-suggestion__dept">{{ item.deptPathText }}</span>
                    </div>
                  </template>
                </el-autocomplete>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="说明" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="3"
              placeholder="请输入工艺路线说明"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              placeholder="请输入备注"
              :disabled="isHeaderReadonly"
            />
          </el-form-item>
        </el-tab-pane>
        <template v-if="formData.id">
          <el-tab-pane label="流转关系图" name="flow" lazy>
            <RouteFlowGraphDesigner
              ref="routeFlowGraphDesignerRef"
              :route-id="formData.id"
              :route-name="formData.name"
              :active-route-version-no="formData.activeRouteVersionNo"
              :form-type="productionConfigFormType"
              :submitting="formLoading"
              :target-route-process-id="targetRouteProcessId"
              :route-version-edit-context="routeFlowVersionEditContext"
              @request-back="handleFlowGraphBackRequest"
              @request-submit="handleSubmitRequest"
            />
          </el-tab-pane>
          <el-tab-pane label="关联产品" name="product" lazy>
            <RouteProductList
              :routeId="formData.id"
              :form-type="productionConfigFormType"
              :route-version-edit-context="routeVersionEditContext"
              :submitting="formLoading"
              @request-submit="handleSubmitRequest"
            />
          </el-tab-pane>
          <el-tab-pane label="DCC项目代码" name="dcc" lazy>
            <div class="route-dcc-project-binding" v-loading="dccProjectBindingLoading">
              <el-alert
                title="QA 规程只通过 DCC 项目代码关联。这里保存的是工艺路线与 DCC 项目代码的正式关系，不保存 QA 规程。"
                type="info"
                :closable="false"
                show-icon
              />
              <el-form-item label="项目代码">
                <el-select
                  v-model="dccProjectBindingForm.dccProjectCodeId"
                  class="route-dcc-project-binding__select"
                  filterable
                  remote
                  clearable
                  reserve-keyword
                  :remote-method="loadDccProjectCodeOptions"
                  :loading="dccProjectCodeLoading"
                  placeholder="请输入项目名称或项目代码搜索"
                >
                  <el-option
                    v-for="projectCode in dccProjectCodeOptions"
                    :key="projectCode.id"
                    :label="formatDccProjectCodeOption(projectCode)"
                    :value="projectCode.id"
                  />
                </el-select>
              </el-form-item>
              <el-space>
                <el-button
                  type="primary"
                  :disabled="!dccProjectBindingForm.dccProjectCodeId"
                  @click="saveDccProjectBinding"
                >
                  保存DCC项目代码
                </el-button>
                <el-button
                  :disabled="!dccProjectBinding.bound"
                  @click="deleteDccProjectBinding"
                >
                  解除绑定
                </el-button>
                <span class="route-dcc-project-binding__version">
                  当前关系版本：{{ dccProjectBinding.version }}
                </span>
              </el-space>
            </div>
          </el-tab-pane>
        </template>
      </el-tabs>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import { CommonStatusEnum } from '@/utils/constants'
import {
  ProRouteApi,
  ProRouteVO,
  type RouteDccProjectBindingVO,
  type RouteVersionEditContext
} from '@/api/mes/pro/route'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import { AutoCodeRecordApi } from '@/api/mes/md/autocode/record'
import * as DeptApi from '@/api/system/dept'
import * as UserApi from '@/api/system/user'
import { MesAutoCodeRuleCode } from '@/views/mes/utils/constants'
import { isRouteConfirmCancel, resolveRouteOperationErrorMessage } from './routeError'

defineOptions({ name: 'RouteFormContent' })

const RouteFlowGraphDesigner = defineAsyncComponent(() => import('./RouteFlowGraphDesigner.vue'))
const RouteProductList = defineAsyncComponent(() => import('./RouteProductList.vue'))

const props = withDefaults(
  defineProps<{
    mode?: 'dialog' | 'page'
    basicReadonly?: boolean
    targetRouteProcessId?: number
    routeVersionEditContext?: RouteVersionEditContext
    routeVersionActionLoading?: boolean
  }>(),
  {
    mode: 'dialog',
    basicReadonly: false,
    routeVersionActionLoading: false
  }
)
const emit = defineEmits([
  'success',
  'back',
  'request-upgrade',
  'request-candidate-edit',
  'request-route-version-submit'
])

const message = useMessage()
const formLoading = ref(false)
const formType = ref<string>('create')
const mode = computed(() => props.mode)
const basicReadonly = computed(() => props.basicReadonly)
const targetRouteProcessId = computed(() => props.targetRouteProcessId)
const routeVersionEditContext = computed(() => props.routeVersionEditContext)
const routeFlowVersionEditContext = computed<RouteVersionEditContext | undefined>(() => {
  if (routeVersionEditContext.value) return routeVersionEditContext.value
  const routeVersionId = Number(formData.value.activeRouteVersionId)
  if (!Number.isFinite(routeVersionId) || routeVersionId <= 0) return undefined
  const versionNo = String(formData.value.activeRouteVersionNo || '').trim()
  if (!versionNo) {
    throw new Error('流转关系图加载失败：缺少生效路线版本号。')
  }
  return { routeVersionId, versionNo, lifecycleStatus: 'ACTIVE' }
})
const isEditable = computed(() => ['create', 'update'].includes(formType.value))
const isEnable = computed(() => formType.value === 'enable')
const isDetail = computed(() => ['detail', 'enable'].includes(formType.value))
const isHeaderReadonly = computed(() => basicReadonly.value || ['enable', 'detail'].includes(formType.value))
const isProductTabActive = computed(() => activeTab.value === 'product')
const YINGTAI_ROOT_NAME = '瑛泰医疗'
const PRODUCTION_CENTER_NAME = '生产制造中心'
type RouteFormInitialTab =
  | 'basic'
  | 'flow'
  | 'product'
  | 'dcc'

const activeTab = ref<RouteFormInitialTab>('basic')
const formData = ref<ProRouteVO>({
  id: undefined,
  code: '',
  name: '',
  ownerName: '',
  description: '',
  remark: ''
})
const formRules = reactive({
  code: [{ required: true, message: '编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
})
const formRef = ref()
const routeFlowGraphDesignerRef =
  ref<InstanceType<typeof import('./RouteFlowGraphDesigner.vue')['default']>>()
const pendingFlowAutoLayout = ref(false)
const pendingFlowAutoLayoutKey = ref('')
const completedFlowAutoLayoutEntryKey = ref('')
const ownerLeaderCandidates = ref<RouteOwnerCandidate[]>([])
let ownerLeaderCandidatesPromise: Promise<void> | undefined
const dccProjectBinding = ref<RouteDccProjectBindingVO>({
  routeId: 0,
  dccProjectCodeId: undefined,
  version: 0,
  bound: false
})
const dccProjectBindingForm = reactive<{ dccProjectCodeId?: number }>({})
const dccProjectBindingLoading = ref(false)
const dccProjectCodeLoading = ref(false)
const dccProjectCodeOptions = ref<DccProjectCodeRespVO[]>([])

const isDraftCandidateVersion = computed(
  () => routeVersionEditContext.value?.lifecycleStatus === 'DRAFT'
)
const hasRouteVersionPageContext = computed(() => mode.value === 'page' && Boolean(formData.value.id))
const productionConfigFormType = computed(() =>
  hasRouteVersionPageContext.value && !isDraftCandidateVersion.value ? 'detail' : formType.value
)

type RouteOwnerCandidate = {
  value: string
  userId: number
  deptPathText: string
}

const generateCode = async () => {
  formData.value.code = await AutoCodeRecordApi.generateAutoCode(MesAutoCodeRuleCode.PRO_ROUTE_CODE)
}

const open = async (type: string, id?: number, initialTab: RouteFormInitialTab = 'basic') => {
  formType.value = type
  activeTab.value = id ? initialTab : 'basic'
  resetForm()
  formLoading.value = true
  let shouldTriggerFlowAutoLayout = false
  try {
    if (id) {
      formData.value = await ProRouteApi.getRoute(id)
      await loadDccProjectBinding(id)
    }
    if (id && initialTab === 'basic') {
      await ensureOwnerLeaderCandidatesLoaded()
    }
    shouldTriggerFlowAutoLayout = activeTab.value === 'flow'
  } finally {
    formLoading.value = false
  }
  if (shouldTriggerFlowAutoLayout) {
    triggerFlowAutoLayout()
  }
}

const assertRouteCandidateVersionWritable = () => {
  if (routeVersionEditContext.value && !isDraftCandidateVersion.value) {
    const errorMessage = '当前候选版本已离开草稿状态，仅允许查看。'
    message.error(errorMessage)
    throw new Error(errorMessage)
  }
}

const submitForm = async () => {
  assertRouteCandidateVersionWritable()
  await formRef.value.validate()
  const shouldSaveFlowGraph = shouldSaveFlowGraphOnSubmit()
  formLoading.value = true
  try {
    await validateFlowGraphBeforeSubmit(shouldSaveFlowGraph)
    const successMessage = formType.value === 'create' ? '新增成功' : '保存成功'
    const data = { ...formData.value }
    if (formType.value === 'create') {
      const res = await ProRouteApi.createRoute(data, { ignoreErrorMessage: true })
      formData.value.id = res
      formType.value = 'update'
    } else if (shouldPersistRouteHeaderOnSubmit()) {
      await ProRouteApi.updateRoute(data)
    }
    await saveFlowGraphAfterRouteSave(shouldSaveFlowGraph)
    message.success(successMessage)
    emit('success')
  } catch (error) {
    if (formType.value === 'create' && isDuplicateRouteNameError(error)) {
      if (await confirmDuplicateRouteVersionUpgrade(formData.value.name)) {
        emit('request-upgrade', { routeName: formData.value.name })
      }
      return
    }
    throw error
  } finally {
    formLoading.value = false
  }
}

const handleSubmitRequest = async () => {
  try {
    await submitForm()
  } catch (error) {
    if (isRouteConfirmCancel(error)) return
    message.error(resolveRouteOperationErrorMessage(error, '保存工艺路线失败'))
  }
}

const confirmDuplicateRouteVersionUpgrade = async (routeName: string) => {
  const normalizedName = String(routeName || '').trim()
  try {
    await message.confirm(
      `同一个路线名称只能有一个工艺路线，已存在“${normalizedName}”。是否升版本？`,
      '升版本确认'
    )
    return true
  } catch (_cancel) {
    return false
  }
}

const isDuplicateRouteNameError = (error: unknown) => {
  const apiError = error as { code?: number | string; message?: string }
  return Number(apiError?.code) === 1040501006 || apiError?.message === '工艺路线名称已存在'
}

const shouldSaveFlowGraphOnSubmit = () =>
  formType.value === 'update' && Boolean(formData.value.id) && activeTab.value === 'flow'
const shouldPersistRouteHeaderOnSubmit = () => !routeVersionEditContext.value

const requireFlowGraphDesigner = () => {
  const designer = routeFlowGraphDesignerRef.value
  if (!designer) {
    const errorMessage = '保存工艺路线失败：流转关系图组件未加载'
    message.error(errorMessage)
    throw new Error(errorMessage)
  }
  return designer
}

const validateFlowGraphBeforeSubmit = async (shouldSaveFlowGraph: boolean) => {
  if (!shouldSaveFlowGraph) return
  await requireFlowGraphDesigner().validateBeforeSubmit()
}

const saveFlowGraphAfterRouteSave = async (shouldSaveFlowGraph: boolean) => {
  if (!shouldSaveFlowGraph) return
  await requireFlowGraphDesigner().saveFromParent()
}

const hasFlowGraphWorkspaceDraftChanges = () =>
  activeTab.value === 'flow' &&
  Boolean(routeFlowGraphDesignerRef.value?.hasWorkspaceDraftChanges?.())

const hasRouteCandidateDraftChanges = () =>
  isDraftCandidateVersion.value && hasFlowGraphWorkspaceDraftChanges()

const discardRouteCandidateDraftChanges = () => {
  if (activeTab.value === 'flow') {
    routeFlowGraphDesignerRef.value?.discardWorkspaceDraftChanges?.()
  }
}

const confirmFlowGraphDraftSaveBeforeExit = async () => {
  const designer = routeFlowGraphDesignerRef.value
  if (activeTab.value !== 'flow' || !designer || !designer.hasWorkspaceDraftChanges()) return true
  try {
    await ElMessageBox.confirm('工作区有变动，是否保存？', '退出确认', {
      confirmButtonText: '保存',
      cancelButtonText: '不保存',
      distinguishCancelAndClose: true,
      type: 'warning'
    })
    await submitForm()
    return true
  } catch (error) {
    if (error === 'cancel') {
      designer.discardWorkspaceDraftChanges()
      return true
    }
    return false
  }
}

const handleFlowGraphBackRequest = async () => {
  const canExit = await confirmFlowGraphDraftSaveBeforeExit()
  if (canExit) {
    emit('back')
  }
}

const handleEnable = async () => {
  try {
    await message.confirm(
      '确认启用"' + formData.value.name + '"工艺路线吗？启用前请确认工序和产品 BOM 配置完整。'
    )
    formLoading.value = true
    await ProRouteApi.updateRouteStatus(formData.value.id!, CommonStatusEnum.ENABLE)
    message.success('启用成功')
    emit('success')
  } catch (error) {
    if (isRouteConfirmCancel(error)) {
      return
    }
    message.error(resolveRouteOperationErrorMessage(error, '启用工艺路线失败，请查看后端返回错误'))
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    code: '',
    name: '',
    ownerName: '',
    description: '',
    remark: ''
  }
  formRef.value?.resetFields()
  dccProjectBinding.value = {
    routeId: 0,
    dccProjectCodeId: undefined,
    version: 0,
    bound: false
  }
  dccProjectBindingForm.dccProjectCodeId = undefined
  dccProjectCodeOptions.value = []
}

const buildFlowAutoLayoutEntryKey = () => {
  if (!formData.value.id) return ''
  const routeVersionKey = routeVersionEditContext.value
    ? [
        routeVersionEditContext.value.routeVersionId,
        routeVersionEditContext.value.versionNo,
        routeVersionEditContext.value.lifecycleStatus
      ].join(':')
    : 'active'
  return `${formData.value.id}:${routeVersionKey}`
}

const shouldQueueFlowAutoLayout = () => {
  const entryKey = buildFlowAutoLayoutEntryKey()
  return (
    Boolean(entryKey) &&
    completedFlowAutoLayoutEntryKey.value !== entryKey &&
    pendingFlowAutoLayoutKey.value !== entryKey
  )
}

const triggerFlowAutoLayout = () => {
  if (!shouldQueueFlowAutoLayout()) return
  pendingFlowAutoLayout.value = true
  pendingFlowAutoLayoutKey.value = buildFlowAutoLayoutEntryKey()
  void runPendingFlowAutoLayout()
}

const runPendingFlowAutoLayout = async () => {
  if (!pendingFlowAutoLayout.value) return
  await nextTick()
  await nextTick()
  if (activeTab.value !== 'flow') {
    pendingFlowAutoLayout.value = false
    pendingFlowAutoLayoutKey.value = ''
    return
  }
  if (formLoading.value || !formData.value.id) return
  const designer = routeFlowGraphDesignerRef.value
  if (!designer) return
  const pendingKey = pendingFlowAutoLayoutKey.value
  if (!pendingKey || pendingKey !== buildFlowAutoLayoutEntryKey()) {
    pendingFlowAutoLayout.value = false
    pendingFlowAutoLayoutKey.value = ''
    return
  }
  pendingFlowAutoLayout.value = false
  pendingFlowAutoLayoutKey.value = ''
  await designer.autoLayoutOnEntry()
  completedFlowAutoLayoutEntryKey.value = pendingKey
}

const handleRouteTabChange = (tabName: string | number) => {
  if (tabName === 'flow') {
    triggerFlowAutoLayout()
  }
  if (tabName === 'dcc') {
    void loadDccProjectCodeOptions('')
  }
}

const loadDccProjectBinding = async (routeId: number) => {
  dccProjectBindingLoading.value = true
  try {
    const data = await ProRouteApi.getRouteDccProjectBinding(routeId)
    dccProjectBinding.value = data
    dccProjectBindingForm.dccProjectCodeId = data.dccProjectCodeId ?? undefined
  } finally {
    dccProjectBindingLoading.value = false
  }
}

const loadDccProjectCodeOptions = async (keyword = '') => {
  dccProjectCodeLoading.value = true
  try {
    const page = await getProjectCodePage({
      pageNo: 1,
      pageSize: 20,
      keyword,
      status: DCC_PROJECT_CODE_STATUS_ENABLE
    })
    dccProjectCodeOptions.value = page.list || []
  } catch (error) {
    message.error(resolveRouteOperationErrorMessage(error, '加载DCC项目代码失败'))
    throw error
  } finally {
    dccProjectCodeLoading.value = false
  }
}

const formatDccProjectCodeOption = (projectCode: DccProjectCodeRespVO) =>
  [projectCode.projectCode, projectCode.projectName, projectCode.id].filter(Boolean).join(' / ')

const saveDccProjectBinding = async () => {
  if (!formData.value.id || !dccProjectBindingForm.dccProjectCodeId) return
  dccProjectBindingLoading.value = true
  try {
    const data = await ProRouteApi.saveRouteDccProjectBinding({
      routeId: formData.value.id,
      dccProjectCodeId: dccProjectBindingForm.dccProjectCodeId,
      expectedVersion: dccProjectBinding.value.version
    })
    dccProjectBinding.value = data
    dccProjectBindingForm.dccProjectCodeId = data.dccProjectCodeId ?? undefined
    message.success('DCC项目代码已保存')
  } catch (error) {
    message.error(resolveRouteOperationErrorMessage(error, '保存DCC项目代码失败'))
    throw error
  } finally {
    dccProjectBindingLoading.value = false
  }
}

const deleteDccProjectBinding = async () => {
  if (!formData.value.id || !dccProjectBinding.value.bound) return
  try {
    await message.confirm('确认解除当前工艺路线与DCC项目代码的关系吗？')
  } catch (error) {
    if (isRouteConfirmCancel(error)) return
    throw error
  }
  dccProjectBindingLoading.value = true
  try {
    const data = await ProRouteApi.deleteRouteDccProjectBinding(
      formData.value.id,
      dccProjectBinding.value.version
    )
    dccProjectBinding.value = data
    dccProjectBindingForm.dccProjectCodeId = undefined
    message.success('DCC项目代码已解除')
  } catch (error) {
    message.error(resolveRouteOperationErrorMessage(error, '解除DCC项目代码失败'))
    throw error
  } finally {
    dccProjectBindingLoading.value = false
  }
}

const getActiveTab = () => activeTab.value

watch(
  routeFlowGraphDesignerRef,
  () => {
    void runPendingFlowAutoLayout()
  },
  { flush: 'post' }
)

const ensureOwnerLeaderCandidatesLoaded = async () => {
  if (ownerLeaderCandidates.value.length > 0) return
  if (!ownerLeaderCandidatesPromise) {
    ownerLeaderCandidatesPromise = loadOwnerLeaderCandidates().finally(() => {
      ownerLeaderCandidatesPromise = undefined
    })
  }
  await ownerLeaderCandidatesPromise
}

const loadOwnerLeaderCandidates = async () => {
  const [depts, users] = await Promise.all([DeptApi.getDeptList({}), UserApi.getSimpleUserList()])
  ownerLeaderCandidates.value = buildOwnerLeaderCandidates(depts, users)
}

const buildOwnerLeaderCandidates = (depts: DeptApi.DeptVO[], users: UserApi.UserVO[]) => {
  const rootDept = depts.find((dept) => dept.parentId === 0 && dept.name === YINGTAI_ROOT_NAME)
  if (!rootDept) {
    console.error(`[RouteFormContent] root department not found: ${YINGTAI_ROOT_NAME}`)
    return []
  }
  const productionCenterDept = depts.find(
    (dept) => dept.parentId === rootDept.id && dept.name === PRODUCTION_CENTER_NAME
  )
  if (!productionCenterDept) {
    console.error(`[RouteFormContent] production center not found: ${PRODUCTION_CENTER_NAME}`)
    return []
  }

  const childrenByParentId = new Map<number, DeptApi.DeptVO[]>()
  for (const dept of depts) {
    const children = childrenByParentId.get(dept.parentId) || []
    children.push(dept)
    childrenByParentId.set(dept.parentId, children)
  }
  const userById = new Map(users.map((user) => [user.id, user]))
  const candidateByUserId = new Map<number, RouteOwnerCandidate>()

  const walkDept = (deptId: number, pathParts: string[]) => {
    const children = childrenByParentId.get(deptId) || []
    for (const child of children) {
      const childPathText = [...pathParts, child.name].join(' / ')
      if (typeof child.leaderUserId === 'number') {
        const leaderUser = userById.get(child.leaderUserId)
        if (leaderUser?.nickname) {
          const existed = candidateByUserId.get(leaderUser.id)
          if (existed) {
            if (!existed.deptPathText.includes(childPathText)) {
              existed.deptPathText = `${existed.deptPathText}；${childPathText}`
            }
          } else {
            candidateByUserId.set(leaderUser.id, {
              value: leaderUser.nickname,
              userId: leaderUser.id,
              deptPathText: childPathText
            })
          }
        }
      }
      walkDept(child.id, [...pathParts, child.name])
    }
  }

  walkDept(productionCenterDept.id, [rootDept.name, productionCenterDept.name])

  return [...candidateByUserId.values()].sort((a, b) =>
    a.deptPathText.localeCompare(b.deptPathText, 'zh-CN')
  )
}

const fetchOwnerSuggestions = async (
  queryString: string,
  cb: (items: RouteOwnerCandidate[]) => void
) => {
  await ensureOwnerLeaderCandidatesLoaded()
  const keyword = queryString.trim().toLowerCase()
  if (!keyword) {
    cb(ownerLeaderCandidates.value)
    return
  }
  cb(
    ownerLeaderCandidates.value.filter((candidate) => {
      return (
        candidate.value.toLowerCase().includes(keyword) ||
        candidate.deptPathText.toLowerCase().includes(keyword)
      )
    })
  )
}

const handleOwnerCandidateSelect = (item: RouteOwnerCandidate) => {
  formData.value.ownerName = item.value
}

defineExpose({
  open,
  submitForm,
  handleEnable,
  formLoading,
  isEditable,
  isEnable,
  isProductTabActive,
  getActiveTab,
  hasRouteCandidateDraftChanges,
  discardRouteCandidateDraftChanges,
  hasFlowGraphWorkspaceDraftChanges,
  confirmFlowGraphDraftSaveBeforeExit
})
</script>

<style scoped lang="scss">
.route-form-content {
  width: 100%;
}

.route-form-content.is-page {
  padding: 16px 18px 0;
}

.route-form-content__tabs {
  margin-top: 6px;
}

.route-owner-suggestion {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.route-owner-suggestion__name {
  color: #172033;
  font-size: 14px;
}

.route-owner-suggestion__dept {
  color: #6b7280;
  font-size: 12px;
}

.route-dcc-project-binding {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 760px;
  padding-top: 12px;
}

.route-dcc-project-binding__select {
  width: 420px;
}

.route-dcc-project-binding__version {
  color: #64748b;
  font-size: 13px;
}
</style>
