<!-- MES 生产工序表单 -->
<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="1280px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
      :disabled="isDetail"
      >
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="产品名称" prop="productName">
            <el-input v-model="formData.productName" placeholder="请输入产品名称" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工序编码" prop="code">
            <el-input v-model="formData.code" placeholder="请输入工序编码">
              <template #append>
                <el-button @click="generateCode"> 生成 </el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工序名称" prop="name">
            <el-input v-model="formData.name" placeholder="请输入工序名称" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
                :key="dict.value"
                :value="dict.value"
              >
                {{ dict.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="人工班产能" prop="manualShiftCapacity">
            <el-input-number
              v-model="formData.manualShiftCapacity"
              :min="0"
              :controls="false"
              class="w-100%"
              placeholder="请输入人工班产能"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="工序说明" prop="attention">
        <el-input
          v-model="formData.attention"
          type="textarea"
          :rows="3"
          placeholder="请输入工序说明"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" />
      </el-form-item>
      <!-- 编辑时展示操作步骤 -->
      <template v-if="formData.id">
        <el-divider content-position="left">操作步骤</el-divider>
        <ProProcessContentList :processId="formData.id" />
      </template>
    </el-form>
    <template v-if="formData.id">
      <el-divider content-position="left">批记录与填写配置</el-divider>
      <div class="process-associated-config">
        <el-alert
          v-if="!routeOptions.length"
          title="当前工序未加入工艺路线，请先在工艺流程中添加该工序后再配置批记录和填写人。"
          type="warning"
          :closable="false"
          show-icon
        />
        <template v-else>
          <el-form label-width="100px" class="process-associated-config__route-form">
            <el-form-item label="所属工艺路线">
              <el-select
                v-model="selectedRouteId"
                class="!w-360px"
                placeholder="请选择所属工艺路线"
                :loading="associatedConfigLoading"
                @change="handleRouteContextChange"
              >
                <el-option
                  v-for="routeItem in routeOptions"
                  :key="routeItem.id"
                  :label="routeItem.name"
                  :value="routeItem.id"
                />
              </el-select>
            </el-form-item>
          </el-form>
          <el-alert
            v-if="routeSelectionTip"
            :title="routeSelectionTip"
            type="warning"
            :closable="false"
            show-icon
            class="process-associated-config__alert"
          />
          <el-alert
            v-if="routeContextError"
            :title="routeContextError"
            type="error"
            :closable="false"
            show-icon
            class="process-associated-config__alert"
          />
          <div
            v-if="canShowAssociatedConfig && selectedRouteId !== undefined"
            class="process-associated-config__actions"
          >
            <el-button
              type="primary"
              plain
              :loading="associatedConfigLoading"
              @click="openAssociatedRouteConfig"
            >
              <Icon icon="ep:setting" class="mr-5px" />
              打开工序设置
            </el-button>
          </div>
        </template>
      </div>
    </template>
    <template #footer>
      <el-button v-if="!isDetail" type="primary" @click="submitForm" :loading="formLoading">
        确 定
      </el-button>
      <el-button @click="dialogVisible = false">{{ isDetail ? '关 闭' : '取 消' }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { ProProcessApi, ProProcessRouteVO, ProProcessVO } from '@/api/mes/pro/process'
import { ProRouteProcessApi } from '@/api/mes/pro/route/process'
import { AutoCodeRecordApi } from '@/api/mes/md/autocode/record'
import { MesAutoCodeRuleCode } from '@/views/mes/utils/constants'
import ProProcessContentList from './ProProcessContentList.vue'

defineOptions({ name: 'ProProcessForm' })
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
type ProProcessFormOpenContext = {
  row?: ProProcessVO
  routeId?: number
}

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗
const router = useRouter()

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改；detail - 详情
const isDetail = computed(() => formType.value === 'detail') // 是否为详情模式
const routeOptions = ref<ProProcessRouteVO[]>([])
const selectedRouteId = ref<number>()
const selectedRouteProcessId = ref<number>()
const associatedConfigLoading = ref(false)
const routeContextError = ref('')
const formData = ref<ProProcessVO>({
  id: undefined,
  productName: '',
  code: '',
  name: '',
  attention: '',
  status: 0,
  manualShiftCapacity: undefined,
  remark: ''
})
const formRules = reactive({
  code: [{ required: true, message: '工序编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '工序名称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})
const formRef = ref() // 表单 Ref
const canShowAssociatedConfig = computed(() =>
  Boolean(formData.value.id && selectedRouteId.value && selectedRouteProcessId.value && !routeContextError.value)
)
const routeSelectionTip = computed(() => {
  if (!formData.value.id || !routeOptions.value.length) return ''
  if (!selectedRouteId.value && routeOptions.value.length > 1) {
    return '该工序属于多条工艺路线，请先选择要维护的所属工艺路线。'
  }
  if (selectedRouteId.value && !selectedRouteProcessId.value && associatedConfigLoading.value) {
    return '正在加载当前路线工序配置。'
  }
  return ''
})

/** 生成工序编码 */
const generateCode = async () => {
  formData.value.code = await AutoCodeRecordApi.generateAutoCode(
    MesAutoCodeRuleCode.PRO_PROCESS_CODE,
    'ER'
  )
}

/** 打开弹窗 */
const open = async (type: string, id?: number, context: ProProcessFormOpenContext = {}) => {
  dialogVisible.value = true
  const titles: Record<string, string> = {
    create: '新增生产工序',
    update: '编辑生产工序',
    detail: '生产工序详情'
  }
  dialogTitle.value = titles[type] || type
  formType.value = type
  resetForm()
  applyRouteContext(context.row, context.routeId)
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      const selectedContextRouteId = normalizeRouteId(context.routeId)
      const fetchedData = await ProProcessApi.getProcess(
        id,
        selectedContextRouteId ? { routeId: selectedContextRouteId } : undefined
      )
      formData.value = {
        ...fetchedData,
        routeList: fetchedData.routeList?.length ? fetchedData.routeList : context.row?.routeList
      }
      applyRouteContext(formData.value, context.routeId)
      await resolveSelectedRouteProcess()
    } finally {
      formLoading.value = false
    }
  }
}

const normalizeRouteId = (routeId?: number | string | null) => {
  if (routeId === undefined || routeId === null || routeId === '') return undefined
  const value = Number(routeId)
  return Number.isFinite(value) ? value : undefined
}

const applyRouteContext = (process?: ProProcessVO, routeId?: number) => {
  routeOptions.value = [...(process?.routeList || [])]
  selectedRouteProcessId.value = undefined
  routeContextError.value = ''
  const contextRouteId = normalizeRouteId(routeId)
  if (contextRouteId && routeOptions.value.some((item) => Number(item.id) === contextRouteId)) {
    selectedRouteId.value = contextRouteId
    return
  }
  if (routeOptions.value.length === 1) {
    selectedRouteId.value = routeOptions.value[0].id
    return
  }
  selectedRouteId.value = undefined
}

const handleRouteContextChange = async () => {
  await resolveSelectedRouteProcess()
}

const resolveSelectedRouteProcess = async () => {
  selectedRouteProcessId.value = undefined
  routeContextError.value = ''
  if (!formData.value.id || !selectedRouteId.value) return
  associatedConfigLoading.value = true
  try {
    const routeProcess = await ProRouteProcessApi.getRouteProcessByRouteAndProcess(
      selectedRouteId.value,
      formData.value.id
    )
    if (!routeProcess?.id) {
      throw new Error('未找到当前工序在该工艺路线中的路线工序，请先在工艺流程中添加该工序。')
    }
    selectedRouteProcessId.value = routeProcess.id
  } catch (error) {
    routeContextError.value = resolveErrorMessage(error, '路线工序加载失败，请联系管理员。')
    message.error(routeContextError.value)
  } finally {
    associatedConfigLoading.value = false
  }
}

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) return error.message
  const responseMessage = (error as { response?: { data?: { msg?: string; message?: string } } })?.response?.data
  return responseMessage?.msg || responseMessage?.message || defaultMessage
}

const openAssociatedRouteConfig = () => {
  if (!selectedRouteId.value || !selectedRouteProcessId.value) {
    const errorMessage =
      routeContextError.value || '打开工艺路线工序设置失败：缺少有效路线工序。'
    message.error(errorMessage)
    throw new Error(errorMessage)
  }
  dialogVisible.value = false
  router.push({
    name: 'MesProRouteEdit',
    params: { id: selectedRouteId.value },
    query: {
      tab: 'flow',
      routeProcessId: String(selectedRouteProcessId.value)
    }
  })
}

/** 提交表单 */
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = { ...formData.value }
    if (formType.value === 'create') {
      await ProProcessApi.createProcess(data)
      message.success(t('common.createSuccess'))
    } else {
      await ProProcessApi.updateProcess(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    productName: '',
    code: '',
    name: '',
    attention: '',
    status: 0,
    manualShiftCapacity: undefined,
    remark: ''
  }
  routeOptions.value = []
  selectedRouteId.value = undefined
  selectedRouteProcessId.value = undefined
  routeContextError.value = ''
  associatedConfigLoading.value = false
  formRef.value?.resetFields()
}

defineExpose({ open }) // 提供 open 方法，用于打开弹窗
</script>

<style scoped>
.process-associated-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.process-associated-config__route-form {
  margin-bottom: -10px;
}

.process-associated-config__alert {
  margin-bottom: 4px;
}
</style>
