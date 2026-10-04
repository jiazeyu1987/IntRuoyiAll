<template>
  <el-button v-if="canConfigure" data-testid="dcc-project-reviewer-config-open" :disabled="loading || busy" @click="open">
    配置项目审核人
  </el-button>
  <el-dialog v-model="visible" title="项目及产品创建审核人" width="560px" :close-on-click-modal="false">
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-12px" />
    <el-form v-loading="loading" label-width="100px">
      <el-form-item label="当前配置">
        <span v-if="configuration?.configured">{{ configuration.reviewerNickname }}（{{ configuration.reviewerUsername }}）{{ configuration.enabled ? '' : '，账号不可用' }}</span>
        <span v-else>尚未配置</span>
      </el-form-item>
      <el-form-item label="审核人" required>
        <el-select v-model="selectedUserId" filterable :disabled="busy || loading" placeholder="选择启用账号">
          <el-option v-for="user in users" :key="user.id" :value="user.id" :label="user.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="配置原因" required>
        <el-input v-model="reason" type="textarea" maxlength="500" show-word-limit :disabled="busy || loading" />
      </el-form-item>
    </el-form>
    <p>用于之后提交的项目及产品创建申请，每次申请保留提交时的审核人员。</p>
    <template #footer>
      <el-button :disabled="busy" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="busy" :disabled="loading || !canConfigure || !ready" @click="save">保存配置</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { getTenantId, getVisitTenantId } from '@/utils/auth'
import { getSimpleUserList } from '@/api/system/user'
import {
  configureDccProjectReviewer,
  getDccProjectReviewerConfiguration,
  type DccProjectReviewerConfiguration
} from '@/api/dcc/controlledFile/projectProductRequests'
import { reviewerIdentity } from './project-reviewer'

const emit = defineEmits<{ changed: [configuration: DccProjectReviewerConfiguration] }>()
const message = useMessage()
const userStore = useUserStore()
const readOperatorContext = () => {
  if (!userStore.getIsSetUser) return ''
  const identities = [getVisitTenantId() || getTenantId(), userStore.getUser.id]
  if (identities.some(value => (typeof value !== 'string' && typeof value !== 'number')
    || (typeof value === 'number' && !Number.isSafeInteger(value))
    || !/^[1-9][0-9]*$/.test(String(value)) || BigInt(String(value)) > 9223372036854775807n)) return ''
  return JSON.stringify([...identities.map(String), userStore.getUser.username])
}
const operatorContext = computed(readOperatorContext)
const canConfigure = computed(() => Boolean(operatorContext.value) && userStore.getIsSetUser
  && userStore.getRoles.includes('doc_control')
  && (userStore.getPermissions.has('dcc:project-code:update') || userStore.getPermissions.has('*:*:*')))
const visible = ref(false), loading = ref(false), busy = ref(false), error = ref('')
const ready = ref(false)
const selectedUserId = ref(''), reason = ref('')
const configuration = ref<DccProjectReviewerConfiguration>()
const users = ref<Array<{ id: string; name: string }>>([])
let generation = 0
let mounted = true
const clearContext = () => {
  generation++; ready.value = false; loading.value = false; busy.value = false; selectedUserId.value = ''; reason.value = ''
  users.value = []; configuration.value = undefined; error.value = ''
}
const currentContext = (token: number, context: string) => mounted && visible.value
  && canConfigure.value && token === generation && context === readOperatorContext()
watch(visible, (value) => { if (!value) clearContext() }, { flush: 'sync' })
watch([canConfigure, operatorContext], () => { visible.value = false; clearContext() }, { flush: 'sync' })
onBeforeUnmount(() => { mounted = false; clearContext() })

const open = async () => {
  if (!mounted || !canConfigure.value || busy.value || loading.value) return
  const context = readOperatorContext()
  if (!context || context !== operatorContext.value) return
  const token = ++generation
  visible.value = true; loading.value = true; error.value = ''; selectedUserId.value = ''; reason.value = ''
  ready.value = false
  configuration.value = undefined; users.value = []
  try {
    const [current, accounts] = await Promise.all([getDccProjectReviewerConfiguration(), getSimpleUserList()])
    if (!currentContext(token, context)) return
    if (!Array.isArray(accounts)) throw new Error('启用账号目录响应缺失')
    const options = accounts.map(account => {
      if (!account.nickname?.trim()) throw new Error('启用账号目录缺少人员名称')
      return { id: reviewerIdentity(account.id), name: account.nickname }
    })
    if (new Set(options.map(account => account.id)).size !== options.length) throw new Error('启用账号目录身份重复')
    configuration.value = current; users.value = options
    ready.value = true
    if (current.configured && current.enabled && options.some(account => account.id === current.reviewerUserId))
      selectedUserId.value = current.reviewerUserId!
  } catch (cause) { if (currentContext(token, context)) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally {
    if (token === generation) {
      loading.value = false
      if (context !== readOperatorContext()) { visible.value = false; clearContext() }
    }
  }
}

const save = async () => {
  if (!mounted || !visible.value || !canConfigure.value || !ready.value || busy.value || loading.value) return
  const token = generation
  const context = readOperatorContext()
  if (!context || context !== operatorContext.value) { visible.value = false; return }
  error.value = ''
  try {
    if (!canConfigure.value) throw new Error('没有审核人配置权限')
    const selected = users.value.find(account => account.id === selectedUserId.value)
    if (!selected) throw new Error('请选择目录中的启用账号')
    if (!reason.value.trim() || reason.value.length > 500) throw new Error('请填写500字以内的配置原因')
    const payload = { reviewerUserId: selected.id, reason: reason.value.trim() }
    busy.value = true
    await ElMessageBox.confirm(`将之后提交申请的审核人设置为“${selected.name}”？`, '确认审核人配置', {
      type: 'warning', modalClass: 'app-confirm-message-box-overlay'
    })
    if (!currentContext(token, context)) return
    if (selectedUserId.value !== payload.reviewerUserId || reason.value.trim() !== payload.reason)
      throw new Error('配置内容已变化，请重新确认')
    const result = await configureDccProjectReviewer(payload)
    if (!currentContext(token, context)) return
    configuration.value = result
    emit('changed', result)
    message.success('审核人配置已保存')
    visible.value = false
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close' && currentContext(token, context))
      error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (token === generation) {
      busy.value = false
      if (context !== readOperatorContext()) { visible.value = false; clearContext() }
    }
  }
}
</script>
