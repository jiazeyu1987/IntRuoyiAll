import { Layout } from '@/utils/routerHelper'

const showroomAdminView = () => import('@/views/showroom-admin/index.vue')
const showroomVersionCenterView = () =>
  import('@/views/showroom-admin/version-center/VersionCenterPage.vue')

const showroomRoutes: AppRouteRecordRaw[] = [
  {
    path: '/showroom',
    component: Layout,
    name: 'Showroom',
    redirect: '/showroom/company',
    meta: {
      title: '展柜',
      alwaysShow: true,
      icon: 'ep:monitor'
    },
    children: [
      {
        path: 'company',
        component: showroomAdminView,
        name: 'ShowroomAdminCompany',
        meta: { title: '公司信息', icon: 'ep:office-building', noCache: true, canTo: true }
      },
      {
        path: 'company-version',
        component: showroomAdminView,
        name: 'ShowroomAdminCompanyVersion',
        meta: { title: '公司版本', icon: 'ep:clock', noCache: true, canTo: true }
      },
      {
        path: 'product',
        component: showroomAdminView,
        name: 'ShowroomAdminProduct',
        meta: { title: '产品管理', icon: 'ep:goods', noCache: true, canTo: true }
      },
      {
        path: 'company/version-center/:companyId(\\d+)',
        component: showroomVersionCenterView,
        name: 'ShowroomAdminCompanyVersionCenter',
        meta: {
          title: '公司版本中心',
          hidden: true,
          noCache: true,
          canTo: true,
          activeMenu: '/showroom/company',
          versionTargetType: 'COMPANY'
        }
      },
      {
        path: 'product/version-center/:productId(\\d+)',
        component: showroomVersionCenterView,
        name: 'ShowroomAdminProductVersionCenter',
        meta: {
          title: '产品版本中心',
          hidden: true,
          noCache: true,
          canTo: true,
          activeMenu: '/showroom/product',
          versionTargetType: 'PRODUCT'
        }
      },
      {
        path: 'keyword',
        component: showroomAdminView,
        name: 'ShowroomAdminKeyword',
        meta: { title: '关键词中英对照', icon: 'ep:connection', noCache: true, canTo: true }
      },
      {
        path: 'prompt',
        component: showroomAdminView,
        name: 'ShowroomAdminPrompt',
        meta: { title: '提示管理', icon: 'ep:edit-pen', noCache: true, canTo: true }
      },
      {
        path: 'hall',
        component: showroomAdminView,
        name: 'ShowroomAdminHall',
        meta: { title: '展柜管理', icon: 'ep:grid', noCache: true, canTo: true }
      },
      {
        path: 'approval',
        component: showroomAdminView,
        name: 'ShowroomAdminApproval',
        meta: {
          title: '审批处理',
          icon: 'ep:checked',
          hidden: true,
          noCache: true,
          canTo: true,
          activeMenu: '/approval-center'
        }
      },
      {
        path: 'history',
        component: showroomAdminView,
        name: 'ShowroomAdminHistory',
        meta: { title: '版本历史', icon: 'ep:clock', noCache: true, canTo: true, hidden: true }
      },
      {
        path: 'assignment',
        component: showroomAdminView,
        name: 'ShowroomAdminAssignment',
        meta: { title: '补充指派', icon: 'ep:message', noCache: true, canTo: true, hidden: true }
      },
      {
        path: 'discussion',
        component: showroomAdminView,
        name: 'ShowroomAdminDiscussion',
        meta: { title: '产品讨论', icon: 'ep:chat-line-round', noCache: true, canTo: true, hidden: true }
      },
      {
        path: 'narration-workbench',
        component: showroomAdminView,
        name: 'ShowroomAdminNarration',
        meta: { title: '讲解工作台', icon: 'ep:headset', noCache: true, canTo: true }
      }
    ]
  }
]

export default showroomRoutes
