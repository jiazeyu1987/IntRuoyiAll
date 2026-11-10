<template>
  <el-button v-hasPermi="['mes:pro-process-pool-team-leader:maintain']" @click="open"
    >补料工序关联</el-button
  >
  <el-dialog
    v-model="visible"
    title="补料工序关联"
    width="1000px"
    :close-on-click-modal="false"
    @closed="clearSecret"
  >
    <el-alert
      title="仅补充未配置输入物料的补料所属工序。关联需要本人签名，保存后不能覆盖。"
      type="info"
      :closable="false"
    />
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="my-12px" />
    <el-table
      v-loading="loading"
      :data="data?.lines ?? []"
      highlight-current-row
      @current-change="selectLine"
    >
      <el-table-column prop="billNo" label="补料单号" min-width="140" />
      <el-table-column prop="materialCode" label="物料号" min-width="165" />
      <el-table-column prop="materialName" label="物料名称" min-width="160" />
      <el-table-column prop="lotNumber" label="批号" min-width="110" />
      <el-table-column prop="actualQuantity" label="实补数量" width="95" />
      <el-table-column label="所属工序" min-width="120">
        <template #default="{ row }">{{ processName(row) }}</template>
      </el-table-column>
    </el-table>
    <el-form v-if="selected && canBind" label-width="100px" class="mt-16px">
      <el-form-item label="补料物料"
        >{{ selected.materialName }}（{{ selected.materialCode }}）</el-form-item
      >
      <el-form-item label="所属工序" required>
        <el-select v-model="routeProcessId" placeholder="请选择订单工序" aria-label="补料所属工序">
          <el-option
            v-for="p in data?.processes"
            :key="p.routeProcessId"
            :value="p.routeProcessId"
            :label="p.name"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="关联原因" required
        ><el-input v-model="reason" type="textarea" maxlength="500" aria-label="补料关联原因"
      /></el-form-item>
      <el-form-item label="签名凭据" required
        ><el-input
          v-model="password"
          type="password"
          autocomplete="off"
          show-password
          aria-label="补料关联签名凭据（PIN 码或密码）"
      /></el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">关闭</el-button>
      <el-button
        v-if="canBind"
        type="primary"
        :loading="saving"
        :disabled="!routeProcessId || !reason.trim() || !password"
        @click="save"
        >签名并关联</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getReplenishmentBindings,
  bindReplenishmentProcess,
  type ReplenishmentBindingLine,
  type ReplenishmentBindingPage
} from '@/api/mes/pro/processpool/replenishmentBinding'

const props = defineProps<{ activeOrderId: string }>()
const emit = defineEmits<{ saved: [] }>()
const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const data = ref<ReplenishmentBindingPage>()
const selected = ref<ReplenishmentBindingLine>()
const routeProcessId = ref('')
const reason = ref('')
const password = ref('')
const canBind = computed(
  () =>
    data.value?.editable &&
    selected.value &&
    !selected.value.boundRouteProcessId &&
    !selected.value.frozenRouteProcessIds.length
)
const clearSecret = () => {
  password.value = ''
}
const selectLine = (line?: ReplenishmentBindingLine) => {
  selected.value = line
  routeProcessId.value = ''
  reason.value = ''
  clearSecret()
}
const processName = (line: ReplenishmentBindingLine) => {
  const ids = line.boundRouteProcessId ? [line.boundRouteProcessId] : line.frozenRouteProcessIds
  return ids.length
    ? ids
        .map((id) => data.value?.processes.find((p) => p.routeProcessId === id)?.name ?? id)
        .join('、')
    : '待关联（点击此行）'
}
const open = async () => {
  visible.value = true
  data.value = undefined
  selectLine()
  error.value = ''
  loading.value = true
  try {
    data.value = await getReplenishmentBindings(props.activeOrderId)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '补料工序关联加载失败'
  } finally {
    loading.value = false
  }
}
const save = async () => {
  if (!data.value || !selected.value || !canBind.value || saving.value) return
  saving.value = true
  error.value = ''
  try {
    await bindReplenishmentProcess({
      activeOrderId: props.activeOrderId,
      expectedVersion: data.value.version,
      itemId: selected.value.itemId,
      routeProcessId: routeProcessId.value,
      sourceHash: selected.value.sourceHash,
      reason: reason.value.trim(),
      password: password.value
    })
    clearSecret()
    ElMessage.success('补料工序已签名关联')
    visible.value = false
    emit('saved')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '补料工序签名关联失败'
  } finally {
    clearSecret()
    saving.value = false
  }
}
</script>
