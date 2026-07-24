<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :inline="true"
      :model="queryParams"
      label-width="76px"
    >
      <el-form-item label="规则编码" prop="ruleCode">
        <el-input
          v-model="queryParams.ruleCode"
          clearable
          class="!w-220px"
          placeholder="请输入规则编码"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="目标表单" prop="targetForm">
        <el-select
          v-model="queryParams.targetForm"
          clearable
          class="!w-220px"
          placeholder="请选择目标表单"
        >
          <el-option
            v-for="item in srmCodeRuleTargetFormOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="启用状态" prop="enabled">
        <el-select v-model="queryParams.enabled" clearable class="!w-160px" placeholder="全部">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['srm:code-rule:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
      row-key="id"
    >
      <el-table-column label="规则编码" prop="ruleCode" min-width="150" />
      <el-table-column label="规则名称" prop="ruleName" min-width="160" />
      <el-table-column label="目标表单" prop="targetForm" min-width="150" />
      <el-table-column label="前缀" prop="prefix" width="110" />
      <el-table-column label="日期格式" prop="datePattern" width="130" />
      <el-table-column label="日期段" prop="dateSegmentEnabled" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.dateSegmentEnabled ? 'success' : 'info'">
            {{ row.dateSegmentEnabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="流水宽度" prop="serialWidth" width="100" align="right" />
      <el-table-column label="流水步长" prop="step" width="100" align="right" />
      <el-table-column label="最小流水" prop="minSerial" width="100" align="right" />
      <el-table-column label="最大流水" prop="maxSerial" width="100" align="right" />
      <el-table-column label="分隔符" prop="separator" width="90" />
      <el-table-column label="启用状态" width="110" align="center">
        <template #default="{ row }">
          <el-switch
            v-model="row.enabled"
            :loading="enableLoadingId === row.id"
            @change="handleEnableChange(row)"
            v-hasPermi="['srm:code-rule:enable']"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" width="110" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openForm('update', row.id)"
            v-hasPermi="['srm:code-rule:update']"
          >
            编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <Dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="96px"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="规则编码" prop="ruleCode">
            <el-input v-model="formData.ruleCode" placeholder="请输入规则编码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="规则名称" prop="ruleName">
            <el-input v-model="formData.ruleName" placeholder="请输入规则名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="目标表单" prop="targetForm">
            <el-select
              v-model="formData.targetForm"
              class="!w-1/1"
              placeholder="请选择目标表单"
            >
              <el-option
                v-for="item in srmCodeRuleTargetFormOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="前缀" prop="prefix">
            <el-input v-model="formData.prefix" placeholder="请输入编码前缀" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="日期格式" prop="datePattern">
            <el-input v-model="formData.datePattern" placeholder="例如 yyyyMMdd" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="日期段" prop="dateSegmentEnabled">
            <el-switch v-model="formData.dateSegmentEnabled" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="流水宽度" prop="serialWidth">
            <el-input-number
              v-model="formData.serialWidth"
              :min="1"
              :max="18"
              controls-position="right"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="流水步长" prop="step">
            <el-input-number
              v-model="formData.step"
              :min="1"
              controls-position="right"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="最小流水" prop="minSerial">
            <el-input-number
              v-model="formData.minSerial"
              :min="0"
              controls-position="right"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="最大流水" prop="maxSerial">
            <el-input-number
              v-model="formData.maxSerial"
              :min="1"
              controls-position="right"
              class="!w-1/1"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分隔符" prop="separator">
            <el-input v-model="formData.separator" placeholder="请输入分隔符" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="启用状态" prop="enabled">
            <el-switch v-model="formData.enabled" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import {
  SrmCodeRuleApi,
  srmCodeRuleTargetFormOptions,
  type SrmCodeRuleVO
} from '@/api/srm/code-rule'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'SrmCodeRule' })

const message = useMessage()

const loading = ref(false)
const list = ref<SrmCodeRuleVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  ruleCode: undefined as string | undefined,
  targetForm: undefined as string | undefined,
  enabled: undefined as boolean | undefined
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formType = ref<'create' | 'update'>('create')
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const enableLoadingId = ref<number>()

const defaultFormData = (): SrmCodeRuleVO => ({
  ruleCode: '',
  ruleName: '',
  targetForm: '',
  prefix: '',
  datePattern: 'yyyyMMdd',
  dateSegmentEnabled: true,
  serialWidth: 4,
  step: 1,
  minSerial: 1,
  maxSerial: 9999,
  separator: '-',
  enabled: true,
  remark: ''
})

const formData = reactive<SrmCodeRuleVO>(defaultFormData())
const formRules = reactive<FormRules>({
  ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
  targetForm: [{ required: true, message: '请选择目标表单', trigger: 'change' }],
  prefix: [{ required: true, message: '请输入编码前缀', trigger: 'blur' }],
  datePattern: [{ required: true, message: '请输入日期格式', trigger: 'blur' }],
  dateSegmentEnabled: [{ required: true, message: '请选择日期段状态', trigger: 'change' }],
  serialWidth: [{ required: true, message: '请输入流水宽度', trigger: 'blur' }],
  step: [{ required: true, message: '请输入流水步长', trigger: 'blur' }],
  minSerial: [{ required: true, message: '请输入最小流水', trigger: 'blur' }],
  maxSerial: [{ required: true, message: '请输入最大流水', trigger: 'blur' }]
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resetFormData = () => {
  Object.assign(formData, defaultFormData())
}

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmCodeRuleApi.getCodeRulePage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '编码规则列表加载失败，请检查后端接口。'))
    throw error
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

const openForm = async (type: 'create' | 'update', id?: number) => {
  formType.value = type
  dialogTitle.value = type === 'create' ? '新增编码规则' : '编辑编码规则'
  resetFormData()
  dialogVisible.value = true
  if (type === 'update' && id) {
    formLoading.value = true
    try {
      const data = await SrmCodeRuleApi.getCodeRule(id)
      Object.assign(formData, data)
    } catch (error) {
      message.error(resolveErrorMessage(error, '编码规则详情加载失败，请检查后端接口。'))
      dialogVisible.value = false
      throw error
    } finally {
      formLoading.value = false
    }
  }
}

const submitForm = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await SrmCodeRuleApi.createCodeRule(formData)
      message.success('编码规则已新增')
    } else {
      await SrmCodeRuleApi.updateCodeRule(formData)
      message.success('编码规则已更新')
    }
    dialogVisible.value = false
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '编码规则保存失败，请检查填写内容和后端接口。'))
    throw error
  } finally {
    formLoading.value = false
  }
}

const handleEnableChange = async (row: SrmCodeRuleVO) => {
  if (!row.id) {
    message.error('编码规则缺少编号，无法更新启用状态。')
    row.enabled = !row.enabled
    return
  }
  const nextEnabled = row.enabled
  enableLoadingId.value = row.id
  try {
    await SrmCodeRuleApi.enableCodeRule({ id: row.id, enabled: nextEnabled })
    message.success(nextEnabled ? '编码规则已启用' : '编码规则已停用')
  } catch (error) {
    row.enabled = !nextEnabled
    message.error(resolveErrorMessage(error, '编码规则启用状态更新失败，请检查后端接口。'))
    throw error
  } finally {
    enableLoadingId.value = undefined
  }
}

onMounted(() => {
  getList()
})
</script>
