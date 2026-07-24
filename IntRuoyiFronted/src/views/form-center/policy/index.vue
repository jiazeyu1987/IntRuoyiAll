<template>
  <ContentWrap>
    <div class="form-center-policy__toolbar">
      <el-input v-model="queryParams.actionCode" clearable placeholder="动作编码" />
      <el-select v-model="queryParams.status" clearable placeholder="状态">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
      </el-select>
      <el-button type="primary" @click="getList">
        <Icon class="mr-5px" icon="ep:search" />
        查询
      </el-button>
      <el-button @click="resetQuery">
        <Icon class="mr-5px" icon="ep:refresh" />
        重置
      </el-button>
      <el-button v-hasPermi="['form:policy:create']" plain type="primary" @click="openCreate">
        <Icon class="mr-5px" icon="ep:plus" />
        新增
      </el-button>
    </div>

    <div class="form-center-policy__table">
      <el-table v-loading="loading" :data="list" border :show-overflow-tooltip="true">
        <el-table-column label="数据域" prop="dataDomain" width="100" />
        <el-table-column label="系统编码" prop="systemCode" width="110" />
        <el-table-column label="对象类型" prop="objectType" min-width="140" />
        <el-table-column label="动作编码" prop="actionCode" width="120" />
        <el-table-column label="对象状态" prop="objectState" width="110" />
        <el-table-column label="审批模式" prop="approvalMode" width="110">
          <template #default="{ row }">
            <el-tag :type="formatApprovalModeTagType(row.approvalMode)">
              {{ formatApprovalMode(row.approvalMode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批开关" prop="approvalSwitch" width="130">
          <template #default="{ row }">
            <el-switch
              v-if="row.status === 'PUBLISHED'"
              v-hasPermi="['form:policy:publish']"
              :model-value="row.approvalMode === 'BPM_REQUIRED'"
              :loading="switchingPolicyId === row.id"
              :disabled="switchingPolicyId !== undefined"
              active-text="开启"
              inactive-text="关闭"
              inline-prompt
              @change="(enabled) => handleApprovalModeSwitch(row, Boolean(enabled))"
            />
            <el-tag v-else type="info">仅已发布可切换</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="流程Key" prop="bpmProcessKey" min-width="180">
          <template #default="{ row }">
            {{ row.bpmProcessKey || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="执行器" prop="effectExecutorCode" width="140" />
        <el-table-column label="状态" prop="status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="槽位" min-width="180">
          <template #default="{ row }">
            <span>{{ slotSummary(row.slots) }}</span>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'PUBLISHED'"
              v-hasPermi="['form:policy:publish']"
              link
              type="primary"
              @click="publishPolicy(row)"
            >
              发布
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-model:limit="queryParams.pageSize"
        v-model:page="queryParams.pageNo"
        :total="total"
        @pagination="getList"
      />
    </div>
  </ContentWrap>

  <Dialog v-model="dialogVisible" title="表单策略" width="720px">
    <el-form :model="policyForm" label-width="110px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="数据域">
            <el-input v-model="policyForm.dataDomain" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="系统编码">
            <el-input v-model="policyForm.systemCode" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="对象类型">
            <el-input v-model="policyForm.objectType" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="动作编码">
            <el-input v-model="policyForm.actionCode" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="对象状态">
            <el-input v-model="policyForm.objectState" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="策略类型">
            <el-select v-model="policyForm.policyType">
              <el-option label="包表单" value="PACKAGE" />
              <el-option label="必填表单" value="REQUIRED" />
              <el-option label="可选表单" value="OPTIONAL" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="审批模式">
            <el-select v-model="policyForm.approvalMode">
              <el-option
                v-for="option in approvalModeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="流程Key">
            <el-input
              v-model="policyForm.bpmProcessKey"
              :disabled="policyForm.approvalMode === 'DIRECT'"
              placeholder="开启审批时必须填写流程Key"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="执行器编码">
            <el-input v-model="policyForm.effectExecutorCode" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="槽位编码">
            <el-input v-model="policyForm.slotCode" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="表单模板">
            <el-select v-model="policyForm.templateId" filterable>
              <el-option
                v-for="template in templateOptions"
                :key="template.templateId"
                :label="`${template.templateName} ${template.versionNo}`"
                :value="template.templateId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="policyForm.remark" type="textarea" :rows="2" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button :loading="saving" type="primary" @click="savePolicy">保存</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import * as PolicyApi from '@/api/form-center/policy'
import * as TemplateApi from '@/api/form-center/template'
import type { FormApprovalMode, FormPolicyListItemVO, FormPolicySlotVO } from '@/api/form-center/policy'
import type { FormTemplateListItemVO } from '@/api/form-center/template'

defineOptions({ name: 'FormCenterPolicy' })

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const total = ref(0)
const list = ref<FormPolicyListItemVO[]>([])
const templateOptions = ref<FormTemplateListItemVO[]>([])
const switchingPolicyId = ref<number>()
const approvalModeOptions: Array<{ label: string; value: FormApprovalMode }> = [
  { label: '开启审批', value: 'BPM_REQUIRED' },
  { label: '直接生效', value: 'DIRECT' }
]
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  actionCode: undefined as string | undefined,
  status: undefined as string | undefined
})
const policyForm = reactive({
  dataDomain: 'DCC',
  systemCode: 'DCC',
  objectType: 'CONTROLLED_FILE',
  actionCode: 'UPLOAD',
  objectState: 'DRAFT',
  policyType: 'PACKAGE' as const,
  approvalMode: 'BPM_REQUIRED' as FormApprovalMode,
  bpmProcessKey: 'dcc-controlled-file-approval',
  effectExecutorCode: 'DCC_UPLOAD',
  slotCode: 'CHANGE_FORM',
  templateId: undefined as number | undefined,
  remark: ''
})

const resolvePolicyErrorMessage = (error: unknown) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (responseMessage) return responseMessage
  return (error as Error)?.message || '表单策略请求失败'
}

const formatApprovalMode = (approvalMode: FormApprovalMode | undefined) =>
  approvalModeOptions.find((option) => option.value === approvalMode)?.label || approvalMode || '--'

const formatApprovalModeTagType = (approvalMode: FormApprovalMode | undefined) =>
  approvalMode === 'BPM_REQUIRED' ? 'warning' : 'success'

const getList = async () => {
  loading.value = true
  try {
    const data = await PolicyApi.getPolicyPage(queryParams)
    list.value = data.list
    total.value = data.total
  } catch (error) {
    message.error(resolvePolicyErrorMessage(error))
    throw error
  } finally {
    loading.value = false
  }
}

const resetQuery = async () => {
  queryParams.actionCode = undefined
  queryParams.status = undefined
  queryParams.pageNo = 1
  await getList()
}

const loadPublishedTemplates = async () => {
  try {
    const data = await TemplateApi.getTemplatePool({
      pageNo: 1,
      pageSize: 100,
      status: 'PUBLISHED'
    })
    templateOptions.value = data.list
  } catch (error) {
    message.error(resolvePolicyErrorMessage(error))
    throw error
  }
}

const openCreate = async () => {
  await loadPublishedTemplates()
  policyForm.approvalMode = 'BPM_REQUIRED'
  policyForm.bpmProcessKey = 'dcc-controlled-file-approval'
  policyForm.templateId = templateOptions.value[0]?.templateId
  dialogVisible.value = true
}

const savePolicy = async () => {
  if (!policyForm.templateId) {
    message.error('请选择表单模板')
    return
  }
  if (policyForm.approvalMode === 'BPM_REQUIRED' && !policyForm.bpmProcessKey?.trim()) {
    message.error('审批开启时必须填写流程Key')
    return
  }
  saving.value = true
  try {
    await PolicyApi.savePolicy({
      dataDomain: policyForm.dataDomain,
      systemCode: policyForm.systemCode,
      objectType: policyForm.objectType,
      actionCode: policyForm.actionCode,
      objectState: policyForm.objectState,
      policyType: policyForm.policyType,
      approvalMode: policyForm.approvalMode,
      bpmProcessKey:
        policyForm.approvalMode === 'BPM_REQUIRED' ? policyForm.bpmProcessKey.trim() : undefined,
      effectExecutorCode: policyForm.effectExecutorCode,
      slots: [
        {
          slotCode: policyForm.slotCode,
          required: true,
          templateId: policyForm.templateId
        }
      ],
      remark: policyForm.remark
    })
    message.success('已保存')
    dialogVisible.value = false
    await getList()
  } catch (error) {
    message.error(resolvePolicyErrorMessage(error))
    throw error
  } finally {
    saving.value = false
  }
}

const publishPolicy = async (row: FormPolicyListItemVO) => {
  try {
    await PolicyApi.publishPolicy(row.id)
    message.success('已发布')
    await getList()
  } catch (error) {
    message.error(resolvePolicyErrorMessage(error))
    throw error
  }
}

const handleApprovalModeSwitch = async (row: FormPolicyListItemVO, enabled: boolean) => {
  const targetMode: FormApprovalMode = enabled ? 'BPM_REQUIRED' : 'DIRECT'
  if (row.approvalMode === targetMode) return
  if (targetMode === 'BPM_REQUIRED' && !row.bpmProcessKey?.trim()) {
    message.error('审批开启时必须填写流程Key')
    await getList()
    return
  }
  try {
    await message.confirm(
      targetMode === 'BPM_REQUIRED'
        ? '确认开启审批？开启后新提交表单动作将进入 BPM 审批流程。'
        : '确认关闭审批？关闭后新提交表单动作将直接生效。'
    )
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      await getList()
      return
    }
    throw error
  }
  switchingPolicyId.value = row.id
  try {
    await PolicyApi.switchPolicyApprovalMode(row.id, {
      approvalMode: targetMode,
      bpmProcessKey: targetMode === 'BPM_REQUIRED' ? row.bpmProcessKey?.trim() : undefined
    })
    message.success(targetMode === 'BPM_REQUIRED' ? '审批已开启' : '审批已关闭')
    await getList()
  } catch (error) {
    message.error(resolvePolicyErrorMessage(error))
    await getList()
    throw error
  } finally {
    switchingPolicyId.value = undefined
  }
}

const slotSummary = (slots: FormPolicySlotVO[]) => {
  return slots.map((slot) => `${slot.slotCode}:${slot.templateVersionRef.templateName}`).join('、')
}

onMounted(getList)
</script>

<style scoped>
.form-center-policy__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  background: #ffffff;
}

.form-center-policy__toolbar .el-input,
.form-center-policy__toolbar .el-select {
  width: 180px;
}

.form-center-policy__table {
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 0 0 8px 8px;
  background: #ffffff;
}
</style>
