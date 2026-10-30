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
      <template v-if="!formData.id">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入名称" :disabled="isHeaderReadonly" />
        </el-form-item>
        <el-form-item v-if="formType === 'create'" label="工序" prop="processIds" required>
          <div class="route-create-processes" data-route-create-processes>
            <el-select
              v-model="formData.processIds"
              multiple
              filterable
              :loading="processOptionsLoading"
              :disabled="processOptionsLoading || Boolean(processOptionsError)"
              placeholder="请选择工序，按选择先后排列"
              data-route-create-process-select
            >
              <el-option
                v-for="process in processOptions"
                :key="process.id"
                :value="process.id!"
                :label="`${process.name}（${process.code}）`"
                :data-process-id="process.id"
                :data-process-name="process.name"
                :data-process-code="process.code"
              />
            </el-select>
            <el-alert
              v-if="processOptionsError"
              :title="processOptionsError"
              type="error"
              :closable="false"
              data-route-create-process-error
            />
            <ol v-if="selectedProcesses.length" data-route-create-process-order>
              <li
                v-for="process in selectedProcesses"
                :key="process.id"
                :data-process-id="process.id"
              >
                {{ process.name }}（{{ process.code }}）
              </li>
            </ol>
          </div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            placeholder="请输入备注"
            :disabled="isHeaderReadonly"
          />
        </el-form-item>
      </template>
      <template v-else>
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
          @select-start-setting="handleStartSettingSelect"
          :initial-start-setting="initialStartSetting"
        >
          <template #edit-action>
            <el-button
              v-if="canRequestCandidateEdit"
              type="primary"
              :loading="routeVersionActionLoading"
              @click="emit('request-candidate-edit')"
            >
              修改
            </el-button>
          </template>
          <template #start-setting="{ setting }">
            <div
              v-if="setting === 'basic'"
              class="route-start-basic"
              data-route-start-setting="basic"
            >
              <el-form-item
                v-if="formType !== 'create'"
                label="编码"
                prop="code"
                label-position="top"
                label-width="auto"
              >
                <el-input
                  v-model="formData.code"
                  placeholder="请输入编码"
                  :disabled="isHeaderReadonly"
                />
              </el-form-item>
              <el-form-item label="名称" prop="name" label-position="top" label-width="auto">
                <el-input
                  v-model="formData.name"
                  placeholder="请输入名称"
                  :disabled="isRouteNameReadonly"
                />
              </el-form-item>
              <el-button
                v-if="hasRouteVersionPageContext && !isRouteNameReadonly"
                class="route-start-basic__save"
                type="primary"
                size="small"
                :loading="formLoading"
                :disabled="!hasRouteNameDraftChanges"
                @click="handleSaveRouteNameDraft"
              >
                保存
              </el-button>
            </div>
            <div v-else-if="setting === 'product'" data-route-start-setting="product">
              <h4>关联产品</h4>

              <RouteProductList
                :routeId="formData.id"
                :form-type="productionConfigFormType"
                :route-version-edit-context="routeVersionEditContext"
                :submitting="formLoading"
                @request-submit="handleSubmitRequest"
              />
            </div>
            <div v-else-if="setting === 'dcc'" data-route-start-setting="dcc">
              <div class="route-dcc-project-binding" v-loading="dccProjectBindingLoading">
                <el-alert
                  v-if="dccProjectBindingError"
                  :title="dccProjectBindingError"
                  type="error"
                  :closable="false"
                  show-icon
                >
                  <template #default>
                    <el-button type="primary" link @click="loadDccProjectData">重新加载</el-button>
                  </template>
                </el-alert>
                <template v-else>
                  <el-form-item
                    label="项目代码"
                    label-position="top"
                    label-width="auto"
                    class="route-dcc-project-binding__field"
                  >
                    <el-select
                      v-model="dccProjectBindingForm.dccProjectCodeId"
                      class="route-dcc-project-binding__select"
                      filterable
                      remote
                      clearable
                      reserve-keyword
                      :remote-method="loadDccProjectCodeOptions"
                      :loading="dccProjectCodeLoading"
                      :disabled="!canEditStartSettings"
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
                  <el-button
                    class="route-dcc-project-binding__save"
                    type="primary"
                    size="small"
                    :disabled="!canEditStartSettings || dccProjectBindingLoading || !dccProjectBindingForm.dccProjectCodeId"
                    @click="saveDccProjectBinding"
                  >
                    保存
                  </el-button>
                  <dl
                    v-if="savedDccProjectCode"
                    class="route-dcc-project-binding__saved"
                    aria-label="已保存的关联项目"
                    data-route-saved-dcc-project
                  >
                    <div>
                      <dt>项目代码</dt>
                      <dd>{{ savedDccProjectCode.projectCode }}</dd>
                    </div>
                    <div>
                      <dt>名称</dt>
                      <dd>{{ savedDccProjectCode.projectName }}</dd>
                    </div>
                    <div>
                      <dt>编号</dt>
                      <dd>{{ savedDccProjectCode.id }}</dd>
                    </div>
                  </dl>
                  <p v-else class="route-dcc-project-binding__empty">暂未保存关联项目</p>
                </template>
              </div>
            </div>
          </template>
        </RouteFlowGraphDesigner>
      </template>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import { CommonStatusEnum } from '@/utils/constants'
import {
  ProRouteApi,
  ProRouteVO,
  type MesRouteId,
  type ProRouteVersionVO,
  type RouteDccProjectBindingVO,
  type RouteVersionEditContext
} from '@/api/mes/pro/route'
import {
  DCC_PROJECT_CODE_STATUS_ENABLE,
  getProjectCode,
  getProjectCodePage,
  type DccProjectCodeRespVO
} from '@/api/dcc/controlledFile/projectCodes'
import { ProProcessApi, type ProProcessVO } from '@/api/mes/pro/process'
import { ProRouteProductApi, type ProRouteProductVO } from '@/api/mes/pro/route/product'
import { isRouteConfirmCancel, resolveRouteOperationErrorMessage } from './routeError'
import { resolveSameSourceDraftCandidateForProductionConfig } from './routeCandidateEntry'
import { checkPermi } from '@/utils/permission'

defineOptions({ name: 'RouteFormContent' })

const RouteFlowGraphDesigner = defineAsyncComponent(() => import('./RouteFlowGraphDesigner.vue'))
const RouteProductList = defineAsyncComponent(() => import('./RouteProductList.vue'))

const props = withDefaults(
  defineProps<{
    mode?: 'dialog' | 'page'
    basicReadonly?: boolean
    pageEditing?: boolean
    targetRouteProcessId?: number
    routeVersionEditContext?: RouteVersionEditContext
    routeVersionActionLoading?: boolean
  }>(),
  {
    mode: 'dialog',
    basicReadonly: false,
    pageEditing: false,
    routeVersionActionLoading: false
  }
)
const emit = defineEmits([
  'success',
  'back',
  'request-upgrade',
  'request-candidate-edit',
  'request-route-version-submit',
  'name-draft-saved'
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
const isHeaderReadonly = computed(
  () => basicReadonly.value || ['enable', 'detail'].includes(formType.value)
)
const isProductTabActive = computed(() => selectedStartSetting.value === 'product')
type RouteFormInitialTab = 'basic' | 'flow' | 'product' | 'dcc'

const activeTab = ref<RouteFormInitialTab>('basic')
const initialStartSetting = ref<'basic' | 'product' | 'dcc'>()
const selectedStartSetting = ref<'basic' | 'product' | 'dcc'>()
const formData = ref<ProRouteVO & { processIds: number[] }>({
  id: undefined,
  code: '',
  name: '',
  ownerName: '',
  description: '',
  remark: '',
  processIds: []
})
const processOptions = ref<ProProcessVO[]>([])
const processOptionsLoading = ref(false)
const processOptionsError = ref('')
const selectedProcesses = computed(() =>
  formData.value.processIds.map((id) => processOptions.value.find((process) => process.id === id)!)
)
const formRules = reactive({
  code: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        callback(
          formType.value !== 'create' && !value?.trim() ? new Error('编码不能为空') : undefined
        )
      },
      trigger: 'blur'
    }
  ],
  name: [{ required: true, whitespace: true, message: '名称不能为空', trigger: 'blur' }],
  processIds: [
    {
      validator: (_rule: unknown, value: number[], callback: (error?: Error) => void) => {
        callback(
          formType.value === 'create' && !value?.length
            ? new Error('请至少选择一个工序')
            : undefined
        )
      },
      trigger: 'change'
    }
  ]
})
const formRef = ref()
const routeFlowGraphDesignerRef =
  ref<InstanceType<(typeof import('./RouteFlowGraphDesigner.vue'))['default']>>()
const pendingFlowAutoLayout = ref(false)
const pendingFlowAutoLayoutKey = ref('')
const completedFlowAutoLayoutEntryKey = ref('')
const dccProjectBinding = ref<RouteDccProjectBindingVO>({
  routeId: 0,
  dccProjectCodeId: undefined,
  version: 0,
  bound: false
})
const dccProjectBindingForm = reactive<{ dccProjectCodeId?: number }>({})
const dccProjectBindingLoading = ref(false)
const dccProjectBindingError = ref('')
const dccProjectCodeLoading = ref(false)
const dccProjectCodeOptions = ref<DccProjectCodeRespVO[]>([])
const savedDccProjectCode = ref<DccProjectCodeRespVO>()

const isDraftCandidateVersion = computed(
  () => routeVersionEditContext.value?.lifecycleStatus === 'DRAFT'
)
const hasRouteVersionPageContext = computed(
  () => mode.value === 'page' && Boolean(formData.value.id)
)
const productionConfigFormType = computed(() =>
  hasRouteVersionPageContext.value && (!props.pageEditing || !isDraftCandidateVersion.value)
    ? 'detail' : formType.value
)
const canEditStartSettings = computed(
  () => productionConfigFormType.value === 'update' && checkPermi(['mes:pro-route:update'])
)
const canRequestCandidateEdit = computed(() => {
  if (!hasRouteVersionPageContext.value || props.pageEditing || isDetail.value ||
      !checkPermi(['mes:pro-route:version-update'])) return false
  const context = routeVersionEditContext.value
  return !context || isDraftCandidateVersion.value ||
    (context.lifecycleStatus === 'ACTIVE' &&
      String(context.routeVersionId) === String(formData.value.activeRouteVersionId))
})
const savedRouteName = ref('')
const hasRouteNameDraftChanges = computed(() => formData.value.name !== savedRouteName.value)
const isRouteNameReadonly = computed(() => {
  if (!hasRouteVersionPageContext.value) return isHeaderReadonly.value
  return isDetail.value ||
    !props.pageEditing || !isDraftCandidateVersion.value ||
    !checkPermi(['mes:pro-route:version-update']) ||
    !isEditable.value
})

const persistRouteNameDraft = async () => {
  if (!hasRouteVersionPageContext.value || !hasRouteNameDraftChanges.value) return
  if (isRouteNameReadonly.value) throw new Error('当前版本不允许修改名称')
  await formRef.value.validateField('name')
  const name = formData.value.name.trim()
  let candidate: ProRouteVersionVO
  if (isDraftCandidateVersion.value) {
    candidate = await ProRouteApi.getRouteVersion(routeVersionEditContext.value!.routeVersionId)
  } else {
    const versions = await ProRouteApi.getRouteVersionList(formData.value.id!)
    const existing = resolveSameSourceDraftCandidateForProductionConfig(
      formData.value, versions, '保存名称草稿'
    )
    candidate = existing || await ProRouteApi.createRouteCandidateVersion({
      routeId: formData.value.id!,
      sourceRouteVersionId: formData.value.activeRouteVersionId,
      changeReason: '修改工艺路线名称'
    })
  }
  if (String(candidate.routeId) !== String(formData.value.id) || candidate.lifecycleStatus !== 'DRAFT') {
    throw new Error('保存名称失败：版本不属于当前路线或已离开草稿状态')
  }
  const current = await ProRouteApi.getRouteVersionName(candidate.id)
  if (isDraftCandidateVersion.value && current.name !== savedRouteName.value) {
    throw new Error('草稿名称已被修改，请重新进入页面后再保存')
  }
  await ProRouteApi.saveRouteVersionName({
    routeVersionId: candidate.id,
    name,
    expectedRouteSnapshotSha256: current.routeSnapshotSha256
  })
  formData.value.name = name
  savedRouteName.value = name
  if (!isDraftCandidateVersion.value) emit('name-draft-saved', candidate)
}

const handleSaveRouteNameDraft = async () => {
  formLoading.value = true
  try {
    await persistRouteNameDraft()
    message.success('名称已保存到草稿，发布后生效')
    emit('success')
    if (props.mode === 'page') emit('back')
  } catch (error) {
    message.error(resolveRouteOperationErrorMessage(error, '保存名称草稿失败'))
  } finally {
    formLoading.value = false
  }
}

const open = async (type: string, id?: MesRouteId, initialTab: RouteFormInitialTab = 'basic') => {
  formType.value = type
  activeTab.value = id ? 'flow' : 'basic'
  initialStartSetting.value = id && initialTab !== 'flow' ? initialTab : undefined
  selectedStartSetting.value = initialStartSetting.value
  resetForm()
  formLoading.value = true
  let shouldTriggerFlowAutoLayout = false
  try {
    if (id) {
      formData.value = { ...(await ProRouteApi.getRoute(id)), processIds: [] }
      if (routeVersionEditContext.value) {
        const versionName = await ProRouteApi.getRouteVersionName(routeVersionEditContext.value.routeVersionId)
        if (String(versionName.routeId) !== String(id)) throw new Error('加载名称失败：版本不属于当前路线')
        formData.value.name = versionName.name
      }
      savedRouteName.value = formData.value.name
    } else if (type === 'create') {
      processOptionsLoading.value = true
      try {
        processOptions.value = await ProProcessApi.getProcessSimpleList()
        if (!processOptions.value.length) {
          processOptionsError.value = '没有可选工序，请先维护工序'
        }
      } catch (error) {
        processOptionsError.value = resolveRouteOperationErrorMessage(error, '加载工序失败')
        throw error
      } finally {
        processOptionsLoading.value = false
      }
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
  if (hasRouteVersionPageContext.value && !props.pageEditing) {
    throw new Error('当前为只读浏览，请先点击修改。')
  }
  const canSaveNameDraft = hasRouteNameDraftChanges.value && !isRouteNameReadonly.value
  if (routeVersionEditContext.value && !isDraftCandidateVersion.value && !canSaveNameDraft) {
    const errorMessage = '当前候选版本已离开草稿状态，仅允许查看。'
    message.error(errorMessage)
    throw new Error(errorMessage)
  }
}

const submitForm = async () => {
  assertRouteCandidateVersionWritable()
  await formRef.value.validate()
  const shouldSaveFlowGraph = shouldSaveFlowGraphOnSubmit()
  const creating = formType.value === 'create'
  formLoading.value = true
  try {
    await validateFlowGraphBeforeSubmit(shouldSaveFlowGraph)
    const successMessage = formType.value === 'create' ? '新增成功' : '保存成功'
    const data = { ...formData.value }
    if (formType.value === 'create') {
      const res = await ProRouteApi.createRoute(
        {
          name: data.name.trim(),
          remark: data.remark,
          processIds: [...data.processIds]
        },
        { ignoreErrorMessage: true }
      )
      formData.value.id = res
      formType.value = 'update'
      formData.value = { ...(await ProRouteApi.getRoute(res)), processIds: [] }
      savedRouteName.value = formData.value.name
      activeTab.value = 'flow'
      initialStartSetting.value = 'basic'
    } else if (shouldPersistRouteHeaderOnSubmit()) {
      await ProRouteApi.updateRoute(data)
    }
    await saveFlowGraphAfterRouteSave(shouldSaveFlowGraph)
    await persistRouteNameDraft()
    message.success(successMessage)
    emit('success')
  } catch (error) {
    if (creating && isDuplicateRouteNameError(error)) {
      message.error('工艺流程名称已存在')
      return
    }
    if (creating) {
      message.error(resolveRouteOperationErrorMessage(error, '新增工艺流程失败'))
    }
    throw error
  } finally {
    formLoading.value = false
  }
}

const handleSubmitRequest = async () => {
  try {
    await submitForm()
    if (props.mode === 'page') emit('back')
  } catch (error) {
    if (isRouteConfirmCancel(error)) return
    message.error(resolveRouteOperationErrorMessage(error, '保存工艺路线失败'))
  }
}

const isDuplicateRouteNameError = (error: unknown) => {
  const apiError = error as { code?: number | string; message?: string }
  return Number(apiError?.code) === 1040501006 || apiError?.message === '工艺路线名称已存在'
}

const shouldSaveFlowGraphOnSubmit = () =>
  formType.value === 'update' &&
  Boolean(formData.value.id) &&
  activeTab.value === 'flow' &&
  routeFlowVersionEditContext.value?.lifecycleStatus === 'DRAFT'
const shouldPersistRouteHeaderOnSubmit = () => !routeVersionEditContext.value && !hasRouteVersionPageContext.value

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
  (hasRouteVersionPageContext.value && hasRouteNameDraftChanges.value) || (activeTab.value === 'flow' &&
  Boolean(routeFlowGraphDesignerRef.value?.hasWorkspaceDraftChanges?.()))

const hasRouteCandidateDraftChanges = () =>
  isDraftCandidateVersion.value && hasFlowGraphWorkspaceDraftChanges()

const discardRouteCandidateDraftChanges = () => {
  formData.value.name = savedRouteName.value
  if (activeTab.value === 'flow') {
    routeFlowGraphDesignerRef.value?.discardWorkspaceDraftChanges?.()
  }
}

const confirmFlowGraphDraftSaveBeforeExit = async () => {
  const designer = routeFlowGraphDesignerRef.value
  if (!hasFlowGraphWorkspaceDraftChanges()) return true
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
      formData.value.name = savedRouteName.value
      designer?.discardWorkspaceDraftChanges()
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
  savedRouteName.value = ''
  formData.value = {
    id: undefined,
    code: '',
    name: '',
    ownerName: '',
    description: '',
    remark: '',
    processIds: []
  }
  processOptions.value = []
  processOptionsError.value = ''
  formRef.value?.resetFields()
  dccProjectBinding.value = {
    routeId: 0,
    dccProjectCodeId: undefined,
    version: 0,
    bound: false
  }
  dccProjectBindingForm.dccProjectCodeId = undefined
  dccProjectBindingError.value = ''
  dccProjectCodeOptions.value = []
  savedDccProjectCode.value = undefined
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

const handleStartSettingSelect = (setting: 'basic' | 'product' | 'dcc' | undefined) => {
  selectedStartSetting.value = setting
  if (setting === 'dcc') void loadDccProjectData()
}

const loadDccProjectData = async () => {
  if (!formData.value.id) return
  dccProjectBindingError.value = ''
  try {
    await loadDccProjectBinding(formData.value.id)
    await loadDccProjectCodeOptions('')
  } catch (error) {
    const errorMessage = resolveRouteOperationErrorMessage(
      error,
      '加载DCC项目代码配置失败，请查看后端返回错误'
    )
    dccProjectBindingError.value = errorMessage
    message.error(errorMessage)
  }
}

const loadDccProjectBinding = async (routeId: MesRouteId) => {
  dccProjectBindingLoading.value = true
  try {
    const data = await ProRouteApi.getRouteDccProjectBinding(routeId)
    const projectCode = data.bound && data.dccProjectCodeId
      ? await getProjectCode(data.dccProjectCodeId)
      : undefined
    dccProjectBinding.value = data
    savedDccProjectCode.value = projectCode
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
  if (dccProjectBindingLoading.value || !formData.value.id || !dccProjectBindingForm.dccProjectCodeId) return
  dccProjectBindingLoading.value = true
  try {
    if (!canEditStartSettings.value) throw new Error('当前为只读浏览，请先点击修改。')
    const routeId = formData.value.id
    const routeVersionId = isDraftCandidateVersion.value
      ? routeVersionEditContext.value?.routeVersionId
      : undefined
    const projectCode = await getProjectCode(dccProjectBindingForm.dccProjectCodeId)
    const formalProducts: ProRouteProductVO[] = await ProRouteProductApi.getRouteProductListByRoute(routeId)
    const draftProducts: ProRouteProductVO[] = routeVersionId
      ? await ProRouteProductApi.getRouteProductListByRoute(routeId, routeVersionId)
      : []
    const products = [...new Map([...formalProducts, ...draftProducts]
      .map(product => [product.itemId, product])).values()]
    if (products.length > 0) {
      await ElMessageBox.confirm(
        `绑定项目“${projectCode.projectCode} / ${projectCode.projectName}”将解除${routeVersionId ? '正式关联及当前草稿中的' : '当前'} ${products.length} 个产品的关联，并清除对应的路线产品 BOM 配置。产品主数据保留。是否继续？`,
        '确认解除产品关联',
        { confirmButtonText: '确认解除并保存', cancelButtonText: '取消', type: 'warning' }
      )
    }
    const data = await ProRouteApi.saveRouteDccProjectBinding({
      routeId,
      routeVersionId,
      dccProjectCodeId: projectCode.id,
      expectedVersion: dccProjectBinding.value.version,
      confirmedProductItemIds: products.map(product => product.itemId)
    })
    dccProjectBinding.value = data
    savedDccProjectCode.value = projectCode
    dccProjectBindingForm.dccProjectCodeId = data.dccProjectCodeId ?? undefined
    message.success(products.length > 0 ? '项目已保存，原产品关联已解除' : 'DCC项目代码已保存')
  } catch (error) {
    if (isRouteConfirmCancel(error)) return
    message.error(resolveRouteOperationErrorMessage(error, '保存DCC项目代码失败'))
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

defineExpose({
  open,
  submitForm,
  handleSubmitRequest,
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

.route-create-processes {
  width: 100%;
  ol {
    margin: 8px 0 0;
    padding-left: 24px;
    list-style: decimal;
  }
}

.route-form-content.is-page {
  padding: 16px 18px 0;
}

.route-start-basic {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-top: 4px;
}

.route-start-basic :deep(.el-form-item) {
  margin-bottom: 0;
}

.route-start-basic__save {
  align-self: flex-start;
}

.route-start-basic :deep(.el-form-item__label) {
  margin-bottom: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 20px;
}

.route-start-basic :deep(.el-input__inner),
.route-start-basic :deep(.el-textarea__inner) {
  font-size: 13px;
}

.route-dcc-project-binding {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 760px;
  padding-top: 12px;
}

.route-dcc-project-binding__field {
  margin-bottom: 0;
}

.route-dcc-project-binding__select {
  width: 100%;
}

.route-dcc-project-binding__select :deep(.el-select__wrapper) {
  font-size: 12px;
}

.route-dcc-project-binding__save {
  align-self: flex-start;
}

.route-dcc-project-binding__saved {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
}

.route-dcc-project-binding__saved > div {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.route-dcc-project-binding__saved dt {
  flex: 0 0 52px;
  color: #606266;
}

.route-dcc-project-binding__saved dd {
  min-width: 0;
  margin: 0;
  color: #303133;
  overflow-wrap: anywhere;
}

.route-dcc-project-binding__empty {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

</style>
