<template>
  <div
    :class="[prefixCls, 'login-page']"
    :style="loginBackgroundStyle"
    class="relative h-[100%] lt-md:px-10px lt-sm:px-10px lt-xl:px-10px lt-xl:px-10px"
  >
    <div class="login-page__shell relative mx-auto h-full flex">
      <div class="login-page__content relative flex-1 overflow-x-hidden overflow-y-auto">
        <div class="login-page__brand">
          <span class="login-page__brand-title">{{ platformTitle }}</span>
        </div>
        <Transition appear enter-active-class="animate__animated animate__bounceInRight">
          <div class="login-page__form-wrap">
            <!-- 账号登录 -->
            <LoginForm class="login-page__panel h-auto p-20px" />
            <!-- 手机登录 -->
            <MobileForm class="login-page__panel h-auto p-20px" />
            <!-- 二维码登录 -->
            <QrCodeForm class="login-page__panel h-auto p-20px" />
            <!-- 注册 -->
            <RegisterForm class="login-page__panel h-auto p-20px" />
            <!-- 三方登录 -->
            <SSOLoginVue class="login-page__panel h-auto p-20px" />
            <!-- 忘记密码 -->
            <ForgetPasswordForm class="login-page__panel h-auto p-20px" />
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'

import { LoginForm, MobileForm, QrCodeForm, RegisterForm, SSOLoginVue, ForgetPasswordForm } from './components'

defineOptions({ name: 'Login' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('login')
const platformTitle = '瑛泰数字化平台'
const loginInterventionalMedicalBg = new URL(
  '@/assets/imgs/login-interventional-medical-bg.png',
  import.meta.url
).href
const loginBackgroundStyle = {
  backgroundImage: `linear-gradient(90deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.58)), url(${loginInterventionalMedicalBg})`
}
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-login;

.#{$prefix-cls} {
  overflow: auto;
}

.login-page {
  min-height: 100%;
  background-color: #f6f9fc;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;

  &::before {
    position: absolute;
    inset: 0;
    pointer-events: none;
    background:
      radial-gradient(circle at 74% 50%, rgba(255, 255, 255, 0.72), transparent 24%),
      linear-gradient(90deg, rgba(246, 250, 253, 0.08), rgba(246, 249, 252, 0.38));
    content: '';
  }
}

.login-page__shell {
  z-index: 1;
}

.login-page__content {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  padding: 30px;
}

.login-page__brand {
  display: flex;
  align-items: center;
  min-height: 48px;
  color: #172033;
  text-shadow: 0 2px 18px rgba(255, 255, 255, 0.78);
}

.login-page__brand-title {
  color: #172033;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0;
}

.login-page__form-wrap {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
  padding: 24px min(9vw, 144px) 72px 24px;
}

:deep(.login-page__panel) {
  width: min(460px, 100%);
  padding: 28px !important;
  border: 1px solid rgba(220, 231, 240, 0.9);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 24px 64px rgba(64, 105, 139, 0.16);
  backdrop-filter: blur(18px);
}

@media (max-width: 900px) {
  .login-page::before {
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.3), rgba(246, 249, 252, 0.66));
  }

  .login-page__content {
    padding: 16px 10px;
  }

  .login-page__brand {
    justify-content: center;
  }

  .login-page__brand-title {
    font-size: 20px;
  }

  .login-page__form-wrap {
    justify-content: center;
    padding: 24px 0 48px;
  }

  :deep(.login-page__panel) {
    width: min(460px, calc(100vw - 20px));
    padding: 22px !important;
  }
}
</style>

<style lang="scss">
.dark .login-form {
  .el-divider__text {
    background-color: var(--login-bg-color);
  }

  .el-card {
    background-color: var(--login-bg-color);
  }
}
</style>
