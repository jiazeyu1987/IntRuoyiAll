<template>
  <Dialog v-model="visible" title="项目默认属性与负责人账号" width="820px">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-alert v-if="repairNotice" :title="repairNotice" type="warning" :closable="false" />
    <el-alert v-if="historicalMissing" title="历史未记录，请主动补齐项目默认属性与负责人账号" type="info" :closable="false" />
    <el-form v-loading="loading" label-width="140px" :disabled="loading">
      <el-form-item label="项目负责人账号" required>
        <UserSelectV2 v-model="leaderId" :disabled="loading" placeholder="选择正式系统账号" :clearable="false" :user-options="users" />
      </el-form-item>
      <ProjectAttributesFields v-model="attributes" />
      <el-form-item label="修改原因" required><el-input v-model="changeReason" maxlength="500" /></el-form-item>
      <el-alert title="修改仅影响以后新申请，已保存草稿、在途申请与历史版本保留原值。" type="info" :closable="false" />
    </el-form>
    <template #footer>
      <el-button v-hasPermi="['dcc:project-code:update']" type="primary" :loading="loading" :disabled="!ready" @click="save">保存</el-button>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { ref, watch, onBeforeUnmount } from 'vue'
import { getSimpleUserList } from '@/api/system/user'
import { configureProjectAttributes, getProjectAttributeConfiguration } from '@/api/dcc/controlledFile/projectAttributes'
import ProjectAttributesFields from '../../project-attributes/ProjectAttributesFields.vue'
import { emptyAttributes, validateAttributes, type ProjectAttributes } from '../../project-attributes/state'
import { projectConfigurationIdentity, projectConfigurationAccounts, type ProjectConfigurationAccount } from './project-attribute-configuration'
const emit = defineEmits<{ saved: [projectId: number | string] }>()
const message = useMessage()
const visible = ref(false), loading = ref(false), ready = ref(false), historicalMissing = ref(false), error = ref('')
const repairNotice = ref(''), configurationToken = ref<string>()
let contextRevision = 0
let mounted = true
const changeReason = ref('')
const projectId = ref<string>(), leaderId = ref<string>()
const loadedProjectId = ref<string>()
const users = ref<ProjectConfigurationAccount[]>([]), attributes = ref<ProjectAttributes>(emptyAttributes())
const invalidate = () => { contextRevision++; ready.value = false; loading.value = false; loadedProjectId.value = undefined; leaderId.value = undefined; users.value = []; configurationToken.value = undefined; repairNotice.value = '' }
watch(visible, value => { if (!value) invalidate() }, { flush: 'sync' })
onBeforeUnmount(() => { mounted = false; invalidate() })
const open = async (id: number | string) => {
  if (!mounted) return
  const revision = ++contextRevision
  visible.value = true; loading.value = true; ready.value = false; error.value = ''
  repairNotice.value = ''; configurationToken.value = undefined
  changeReason.value = ''; projectId.value = undefined; loadedProjectId.value = undefined; leaderId.value = undefined; users.value = []; attributes.value = emptyAttributes(); historicalMissing.value = false
  try {
    const selectedProject = projectConfigurationIdentity(id)
    projectId.value = selectedProject
    const [project, accounts] = await Promise.all([getProjectAttributeConfiguration(selectedProject), getSimpleUserList()])
    if (revision !== contextRevision || !visible.value) return
    if (!project || projectConfigurationIdentity(project.id) !== selectedProject) throw new Error('项目属性响应与当前所选项目身份不一致')
    const options = projectConfigurationAccounts(accounts)
    const leader = project.projectLeaderUserId == null ? undefined : projectConfigurationIdentity(project.projectLeaderUserId)
    const notices: string[] = []
    const enabledLeader = leader && options.some(user => user.id === leader) ? leader : undefined
    if (leader && !enabledLeader) notices.push('原项目负责人已失效，请重新选择正式启用账号。')
    users.value = options; leaderId.value = enabledLeader; loadedProjectId.value = selectedProject
    configurationToken.value = project.configurationToken
    historicalMissing.value = !project.defaultAttributesJson || !project.projectLeaderUserId
    if (project.defaultAttributesJson) {
      try { attributes.value = validateAttributes(JSON.parse(project.defaultAttributesJson)) }
      catch (cause) {
        attributes.value = emptyAttributes()
        notices.push('已保存的项目属性不合法，请完整重新填写；已有申请和历史记录不会改变。原因：' + (cause instanceof Error ? cause.message : String(cause)))
      }
    }
    repairNotice.value = notices.join(' ')
    ready.value = true
  } catch (cause) { if (revision === contextRevision) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (revision === contextRevision) loading.value = false }
}
const save = async () => {
  if (!mounted || !visible.value || !ready.value || loading.value || projectId.value === undefined) return
  const revision = contextRevision, id = projectId.value
  loading.value = true; error.value = ''
  try {
    if (projectConfigurationIdentity(id) !== loadedProjectId.value) throw new Error('当前项目配置身份已变化，请重新打开项目')
    const leader = projectConfigurationIdentity(leaderId.value)
    if (!users.value.some(user => user.id === leader)) throw new Error('请选择正式启用账号目录中的负责人')
    validateAttributes(attributes.value)
    const payload = { projectLeaderUserId: leader, defaultAttributes: JSON.parse(JSON.stringify(attributes.value)), changeReason: changeReason.value, expectedConfigurationToken: configurationToken.value! }
    await configureProjectAttributes(id, payload)
    if (!mounted) return
    if (revision !== contextRevision || !visible.value || projectId.value !== id) { message.success(`项目 #${id} 配置已保存，请到原项目记录核对；当前弹框已保留。`); return }
    emit('saved', id)
    if (revision === contextRevision) visible.value = false
  } catch (cause) { if (revision === contextRevision) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (revision === contextRevision) loading.value = false }
}

defineExpose({ open })
</script>
