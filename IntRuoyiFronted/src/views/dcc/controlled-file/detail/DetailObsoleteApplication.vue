<template>
  <el-dialog
    :model-value="modelValue"
    title="作废申请"
    width="820px"
    :close-on-click-modal="false"
    :before-close="beforeClose"
    @update:model-value="close"
  >
    <section v-loading="loading">
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <p
        >{{ file.sourceOriginalFileName || file.fileName || file.title }} · 文件编号
        {{ file.fileNumber }} · 版本 {{ file.versionNo }}</p
      >
      <p>本次作废申请会签、批准后结束。</p>
      <el-form label-width="110px" :disabled="busy">
        <el-form-item label="作废原因"><el-input v-model="reason" type="textarea" /></el-form-item>
        <el-form-item label="会签部门"
          ><el-select v-model="selectedDepartments" multiple filterable
            ><el-option
              v-for="department in departments"
              :key="String(department.id)"
              :label="department.name"
              :value="String(department.id)" /></el-select
        ></el-form-item>
        <el-form-item label="批准人">{{ approverNames }}</el-form-item>
      </el-form>
      <ProjectApplicationAttributes
        ref="attributesRef"
        action="OBSOLETE"
        :readonly="busy || loading"
        @change="snapshot = $event"
        @restore-defaults="snapshot = $event"
      />
      <el-collapse v-if="snapshot" class="my-12px"
        ><el-collapse-item title="本次默认来源（只读）" name="defaults"
          ><ProjectAttributesFields
            :model-value="snapshot.defaultSource"
            readonly /></el-collapse-item
      ></el-collapse>
    </section>
    <template #footer
      ><el-button :disabled="busy" @click="close(false)">取消</el-button
      ><el-button type="primary" :loading="busy" :disabled="loading || !ready" @click="submit"
        >核对并提交作废申请</el-button
      ></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import { getSimpleUserList } from '@/api/system/user'
import { previewApprovalRoute } from '@/api/dcc/controlledFile/approvalRoutes'
import { obsoleteControlledFile, type ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import { generateUUID } from '@/utils'
import ProjectApplicationAttributes from '../project-attributes/ProjectApplicationAttributes.vue'
import ProjectAttributesFields from '../project-attributes/ProjectAttributesFields.vue'
import type { AttributeSnapshot } from '../project-attributes/state'
const props = defineProps<{ modelValue: boolean; file: ControlledFileVO; allowed: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; submitted: [] }>()
const attributesRef = ref<InstanceType<typeof ProjectApplicationAttributes>>()
const snapshot = ref<AttributeSnapshot>(),
  departments = ref<DeptVO[]>([]),
  selectedDepartments = ref<string[]>([])
const loading = ref(false),
  busy = ref(false),
  ready = ref(false),
  error = ref(''),
  reason = ref(''),
  approverNames = ref('')
let generation = 0,
  idempotencyKey = ''
onBeforeUnmount(() => {
  generation++
})
watch(
  () => [props.modelValue, String(props.file.id)] as const,
  async ([visible]) => {
    const token = ++generation
    ready.value = false
    snapshot.value = undefined
    error.value = ''
    busy.value = false
    reason.value = ''
    if (!visible) return
    idempotencyKey = generateUUID()
    loading.value = true
    try {
      if (!props.allowed || !props.file.dccProjectCodeId)
        throw new Error('当前文件不具备作废申请资格或正式项目身份')
      const [deptRows, users, route] = await Promise.all([
        getSimpleDeptList(),
        getSimpleUserList(),
        previewApprovalRoute({ categoryId: props.file.categoryId, actionType: 'OBSOLETE' })
      ])
      if (token !== generation) return
      const review = route.find((row) => row.stageCode === 'MATRIX_REVIEW'),
        approval = route.find((row) => row.stageCode === 'MATRIX_APPROVAL')
      if (
        !review ||
        review.candidateSourceType !== 'DEPT' ||
        !approval?.resolvedUserIds.length ||
        approval.resolvedUserIds.some((id) => !users.some((user) => String(user.id) === String(id)))
      )
        throw new Error('作废会签部门或批准人未正式解析')
      departments.value = deptRows
      selectedDepartments.value = review.candidateSourceIds.map(String)
      approverNames.value = approval.resolvedUserIds
        .map((id) => users.find((user) => String(user.id) === String(id))!.nickname)
        .join('、')
      // 先解除读取锁，再调用组件正式项目默认接口；不复制受控版本的旧属性。
      loading.value = false
      await nextTick()
      if (
        !attributesRef.value ||
        !(await attributesRef.value.selectProject(props.file.dccProjectCodeId))
      )
        throw new Error('本次作废项目默认属性未读取成功')
      if (token === generation) ready.value = true
    } catch (cause) {
      if (token === generation) error.value = cause instanceof Error ? cause.message : String(cause)
    } finally {
      if (token === generation) loading.value = false
    }
  },
  { immediate: true }
)
const beforeClose = (done: () => void) => {
  if (!busy.value) done()
}
const close = (value: boolean) => {
  if (!busy.value) emit('update:modelValue', value)
}
const submit = async () => {
  if (busy.value || !ready.value) return
  const token = generation,
    fileId = props.file.id
  try {
    if (!props.allowed || !reason.value.trim()) throw new Error('请填写作废原因并核对当前办理资格')
    const actualSnapshot = attributesRef.value?.getSnapshot()
    if (!actualSnapshot || actualSnapshot.projectId !== String(props.file.dccProjectCodeId))
      throw new Error('作废属性与当前项目不一致')
    if (
      !selectedDepartments.value.length ||
      selectedDepartments.value.some(
        (id) => !departments.value.some((dept) => String(dept.id) === id)
      )
    )
      throw new Error('请选择有效的本次会签部门')
    const payload = {
      reason: reason.value.trim(),
      idempotencyKey,
      projectAttributes: actualSnapshot.actual,
      selectedSignoffDepartmentIds: [...selectedDepartments.value]
    }
    const fingerprint = JSON.stringify([reason.value, selectedDepartments.value, actualSnapshot])
    const actual = actualSnapshot.actual
    await ElMessageBox.confirm(
      `确认作废 ${props.file.sourceOriginalFileName || props.file.title}（${props.file.fileNumber} / ${props.file.versionNo}）？原因：${payload.reason}；会签部门：${selectedDepartments.value.map((id) => departments.value.find((dept) => String(dept.id) === id)!.name).join('、')}；批准人：${approverNames.value}；目标市场：${actual.targetMarkets.join('、')}${actual.otherMarket ? `（${actual.otherMarket}）` : ''}；注册人：${actual.licenseHolder}；生产方：${actual.actualManufacturer}；文件转移：${actual.documentTransfer}${actual.transferTo ? `，转移至 ${actual.transferTo}` : ''}。批准后作废并结束。`,
      '确认提交作废申请',
      { type: 'warning' }
    )
    if (
      token !== generation ||
      fingerprint !==
        JSON.stringify([
          reason.value,
          selectedDepartments.value,
          attributesRef.value?.getSnapshot()
        ])
    )
      throw new Error('申请内容已改变，请重新确认')
    busy.value = true
    const result = await obsoleteControlledFile(fileId, payload)
    if (token !== generation) return
    if (
      !result?.bpmProcessInstanceId ||
      result.context?.objectId !== String(fileId) ||
      result.context?.actionCode !== 'OBSOLETE'
    )
      throw new Error('作废提交未返回本文件正式申请事实，请刷新核对')
    emit('submitted')
    emit('update:modelValue', false)
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close' && token === generation)
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (token === generation) busy.value = false
  }
}
</script>
