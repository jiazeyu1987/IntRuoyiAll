<template>
  <el-form
    v-show="getShow"
    ref="formLogin"
    :model="loginData.loginForm"
    :rules="LoginRules"
    class="login-form"
    label-position="top"
    label-width="120px"
    size="large"
  >
    <el-row class="mx-[-10px]">
      <el-col :span="24" class="px-10px">
        <el-form-item>
          <LoginFormTitle class="w-full" />
        </el-form-item>
      </el-col>
      <el-col v-if="loginErrorMessage" :span="24" class="px-10px">
        <el-alert
          :title="loginErrorMessage"
          type="error"
          :closable="false"
          show-icon
          class="login-error-alert"
        />
      </el-col>
      <el-col :span="24" class="px-10px">
        <el-form-item v-if="loginData.tenantEnable === 'true'" prop="tenantName">
          <el-select
            v-model="loginData.loginForm.tenantName"
            :reserve-keyword="false"
            class="w-full"
            clearable
            default-first-option
            filterable
            :placeholder="t('login.tenantNamePlaceholder')"
            allow-create
            @change="handleTenantNameChange"
            @visible-change="handleTenantDropdownVisibleChange"
          >
            <template #prefix>
              <component :is="iconHouse" />
            </template>
            <el-option
              v-for="item in tenantHistoryOptions"
              :key="item.tenantName"
              :label="item.tenantName"
              :value="item.tenantName"
            >
              <div class="tenant-history-option">
                <span class="tenant-history-name">{{ item.tenantName }}</span>
                <span class="tenant-history-username">{{ item.username }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px">
        <el-form-item prop="username">
          <el-input
            v-model="loginData.loginForm.username"
            :placeholder="t('login.usernamePlaceholder')"
            :prefix-icon="iconAvatar"
          />
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px">
        <el-form-item prop="password">
          <el-input
            v-model="loginData.loginForm.password"
            :placeholder="t('login.passwordPlaceholder')"
            :prefix-icon="iconLock"
            show-password
            type="password"
            @keyup.enter="submitLogin()"
          />
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px mt-[-20px] mb-[-20px]">
        <el-form-item>
          <el-row justify="space-between" style="width: 100%">
            <el-col :span="6">
              <el-checkbox v-model="loginData.loginForm.rememberMe">
                {{ t('login.remember') }}
              </el-checkbox>
            </el-col>
            <el-col :offset="6" :span="12">
              <el-link
                class="float-right"
                type="primary"
                @click="setLoginState(LoginStateEnum.RESET_PASSWORD)"
              >
                {{ t('login.forgetPassword') }}
              </el-link>
            </el-col>
          </el-row>
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px">
        <el-form-item>
          <XButton
            :loading="loginLoading"
            :title="t('login.login')"
            class="w-full"
            type="primary"
            @click="submitLogin()"
          />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>
<script lang="ts" setup>
import { ElLoading } from 'element-plus'
import LoginFormTitle from './LoginFormTitle.vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

import { useIcon } from '@/hooks/web/useIcon'

import * as authUtil from '@/utils/auth'
import { usePermissionStore } from '@/store/modules/permission'
import * as LoginApi from '@/api/login'
import { LoginStateEnum, resolveLoginErrorMessage, useFormValid, useLoginState, useLoginTenant } from './useLogin'

defineOptions({ name: 'LoginForm' })

const { t } = useI18n()
const iconHouse = useIcon({ icon: 'ep:house' })
const iconAvatar = useIcon({ icon: 'ep:avatar' })
const iconLock = useIcon({ icon: 'ep:lock' })
const formLogin = ref()
const { validForm } = useFormValid(formLogin)
const { resolveTenantId } = useLoginTenant()
const { setLoginState, getLoginState } = useLoginState()
const { currentRoute, push } = useRouter()
const permissionStore = usePermissionStore()
const redirect = ref<string>('')
const loginLoading = ref(false)
const loginErrorMessage = ref('')

const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN)

const LoginRules = {
  tenantName: [required],
  username: [required],
  password: [required]
}
const tenantHistoryOptions = ref<authUtil.LoginTenantHistoryRecord[]>([])
const loginData = reactive({
  isShowPassword: false,
  tenantEnable: import.meta.env.VITE_APP_TENANT_ENABLE,
  loginForm: {
    tenantName: import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '',
    username: '',
    password: '',
    rememberMe: false
  }
})

// 账号密码登录不再触发图形验证码，直接进入正式登录接口。
const submitLogin = async () => {
  await handleLogin()
}
// 获取租户 ID
const getTenantId = async () => {
  return await resolveTenantId(loginData.tenantEnable, loginData.loginForm.tenantName)
}
// 记住我
const syncTenantHistoryOptions = () => {
  tenantHistoryOptions.value = authUtil.getLoginFormHistory()
}

const applyLoginFormCache = (loginForm?: authUtil.LoginFormType) => {
  if (!loginForm) {
    return
  }
  loginData.loginForm = {
    ...loginData.loginForm,
    username: loginForm.username ? loginForm.username : loginData.loginForm.username,
    password: loginForm.password ? loginForm.password : loginData.loginForm.password,
    rememberMe: loginForm.rememberMe,
    tenantName: loginForm.tenantName ? loginForm.tenantName : loginData.loginForm.tenantName
  }
}

const handleTenantNameChange = (tenantName: string) => {
  const loginForm = authUtil.getLoginFormByTenantName(tenantName)
  if (!loginForm) {
    return
  }
  loginData.loginForm = {
    ...loginData.loginForm,
    tenantName: loginForm.tenantName,
    username: loginForm.username,
    password: loginForm.password,
    rememberMe: loginForm.rememberMe
  }
}

const handleTenantDropdownVisibleChange = (visible: boolean) => {
  if (!visible) {
    handleTenantNameChange(loginData.loginForm.tenantName)
  }
}

const getLoginFormCache = () => {
  syncTenantHistoryOptions()
  applyLoginFormCache(authUtil.getLoginForm())
}
// 根据域名，获得租户信息
const getTenantByWebsite = async () => {
  if (loginData.tenantEnable === 'true') {
    const website = location.host
    const res = await LoginApi.getTenantByWebsite(website)
    if (res) {
      loginData.loginForm.tenantName = res.name
      handleTenantNameChange(res.name)
      authUtil.setTenantId(res.id)
    }
  }
}
const loading = ref() // ElLoading.service 返回的实例
// 登录
const handleLogin = async () => {
  loginLoading.value = true
  loginErrorMessage.value = ''
  try {
    const tenantId = await getTenantId()
    if (!tenantId) {
      loginErrorMessage.value = resolveLoginErrorMessage(new Error('TENANT_NOT_FOUND'), 'tenant')
      return
    }
    const data = await validForm()
    if (!data) {
      return
    }
    const loginDataLoginForm = { ...loginData.loginForm }
    const res = await LoginApi.login(loginDataLoginForm, tenantId)
    if (!res) {
      return
    }
    loading.value = ElLoading.service({
      lock: true,
      text: '正在加载系统中...',
      background: 'rgba(0, 0, 0, 0.7)'
    })
    if (loginDataLoginForm.rememberMe) {
      authUtil.setLoginForm(loginDataLoginForm)
    } else {
      authUtil.removeLoginForm(loginDataLoginForm.tenantName)
    }
    authUtil.setToken(res)
    if (!redirect.value) {
      redirect.value = '/'
    }
    // 判断是否为SSO登录
    if (redirect.value.indexOf('sso') !== -1) {
      window.location.href = window.location.href.replace('/login?redirect=', '')
    } else {
      try {
        await push({ path: redirect.value || permissionStore.addRouters[0]?.path || '/srm/portal/application' })
      } catch (error) {
        loginErrorMessage.value = resolveLoginErrorMessage(error, 'permission')
        throw error
      }
    }
  } catch (error) {
    loginErrorMessage.value = resolveLoginErrorMessage(error, 'auth')
  } finally {
    loginLoading.value = false
    loading.value?.close()
  }
}

watch(
  () => currentRoute.value,
  (route: RouteLocationNormalizedLoaded) => {
    redirect.value = route?.query?.redirect as string
  },
  {
    immediate: true
  }
)
onMounted(() => {
  getLoginFormCache()
  getTenantByWebsite()
})
</script>

<style lang="scss" scoped>
:deep(.anticon) {
  &:hover {
    color: var(--el-color-primary) !important;
  }
}

.login-code {
  float: right;
  width: 100%;
  height: 38px;

  img {
    width: 100%;
    height: auto;
    max-width: 100px;
    vertical-align: middle;
    cursor: pointer;
  }
}

.tenant-history-option {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.tenant-history-name {
  color: #172033;
}

.tenant-history-username {
  color: #6b7280;
  font-size: 12px;
}

.login-error-alert {
  margin-bottom: 8px;
  line-height: 20px;
}
</style>
