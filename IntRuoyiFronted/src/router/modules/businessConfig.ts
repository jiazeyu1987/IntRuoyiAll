import { Layout } from '@/utils/routerHelper'

// Named routes for explicit module links. Published domain menus use the same
// leaf identity; the permission store removes the duplicate static shell.
// These hidden routes do not establish menu activation or release acceptance.
const businessConfigRoutes: AppRouteRecordRaw[] = [
  {
    path: '/mes/pro/edhr-config',
    component: Layout,
    name: 'MesEdhrGlobalConfigRoot',
    meta: { hidden: true },
    children: [
      {
        path: '',
        name: 'MesEdhrGlobalConfig',
        component: () => import('@/views/mes/pro/edhr-config/index.vue'),
        meta: {
          hidden: true,
          canTo: true,
          noCache: true,
          title: 'eDHR配置',
          activeMenu: '/mes/pro/edhr-config'
        }
      }
    ]
  },
  {
    path: '/erp/sync/config',
    component: Layout,
    name: 'ErpTableAutoSyncConfigRoot',
    meta: { hidden: true },
    children: [
      {
        path: '',
        name: 'ErpTableAutoSyncConfig',
        component: () => import('@/views/erp/sync/config/index.vue'),
        meta: {
          hidden: true,
          canTo: true,
          noCache: true,
          title: '自动同步配置',
          activeMenu: '/erp/sync/config'
        }
      }
    ]
  },
  {
    path: '/dcc/registration-certificate/config',
    component: Layout,
    name: 'DccRegistrationCertificateConfigRoot',
    meta: { hidden: true },
    children: [
      {
        path: '',
        name: 'DccRegistrationCertificateConfig',
        component: () => import('@/views/dcc/registration-certificate/config/index.vue'),
        meta: {
          hidden: true,
          canTo: true,
          noCache: true,
          title: '注册证配置',
          activeMenu: '/dcc/registration-certificate/config'
        }
      }
    ]
  },
  {
    path: '/infra/config/basic',
    component: Layout,
    name: 'InfraBasicConfigRoot',
    meta: { hidden: true },
    children: [
      {
        path: '',
        name: 'InfraBasicConfig',
        component: () => import('@/views/infra/config/basic/index.vue'),
        meta: {
          hidden: true,
          canTo: true,
          noCache: true,
          title: '基础配置',
          activeMenu: '/infra/config/basic'
        }
      }
    ]
  }
]
export default businessConfigRoutes
