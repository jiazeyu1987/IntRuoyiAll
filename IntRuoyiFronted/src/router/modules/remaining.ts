import { Layout } from '@/utils/routerHelper'

const { t } = useI18n()
const signatureGovernanceView = () => import('@/views/signature-governance/index.vue')
/**
 * redirect: noredirect        当设置 noredirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'          设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * meta : {
 hidden: true              当设置 true 的时候该路由不会再侧边栏出现 如404，login等页面(默认 false)

 alwaysShow: true          当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式，
 只有一个时，会将那个子路由当做根路由显示在侧边栏，
 若你想不管路由下面的 children 声明的个数都显示你的根路由，
 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，
 一直显示根路由(默认 false)

 title: 'title'            设置该路由在侧边栏和面包屑中展示的名字

 icon: 'svg-name'          设置该路由的图标

 noCache: true             如果设置为true，则不会被 <keep-alive> 缓存(默认 false)

 breadcrumb: false         如果设置为false，则不会在breadcrumb面包屑中显示(默认 true)

 affix: true               如果设置为true，则会一直固定在tag项中(默认 false)

 noTagsView: true          如果设置为true，则不会出现在tag中(默认 false)

 activeMenu: '/dashboard'  显示高亮的路由路径

 followAuth: '/dashboard'  跟随哪个路由进行权限过滤

 canTo: true               设置为true即使hidden为true，也依然可以进行路由跳转(默认 false)
 }
 **/
const remainingRouter: AppRouteRecordRaw[] = [
  {
    path: '/redirect',
    component: Layout,
    name: 'Redirect',
    children: [
      {
        path: '/redirect/:path(.*)',
        name: 'Redirect',
        component: () => import('@/views/Redirect/Redirect.vue'),
        meta: {}
      }
    ],
    meta: {
      hidden: true,
      noTagsView: true
    }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/user/profile',
    name: 'Home',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'index',
        component: () => import('@/views/Home/Index.vue'),
        name: 'Index',
        meta: {
          hidden: true,
          title: t('router.home'),
          icon: 'ep:home-filled',
          noCache: false,
          noTagsView: true
        }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    name: 'UserInfo',
    meta: {
      title: t('common.profile'),
      icon: 'ep:user',
      personalWorkbenchTodoBadge: true
    },
    children: [
      {
        path: 'profile',
        component: () => import('@/views/Profile/Index.vue'),
        name: 'Profile',
        meta: {
          canTo: true,
          noTagsView: false,
          tagsViewKeyMode: 'path',
          icon: 'ep:user',
          title: t('common.profile'),
          personalWorkbenchTodoBadge: true
        }
      },
      {
        path: 'notify-message',
        component: () => import('@/views/system/notify/my/index.vue'),
        name: 'MyNotifyMessage',
        meta: {
          canTo: true,
          hidden: true,
          noTagsView: false,
          icon: 'ep:message',
          title: '我的站内信'
        }
      }
    ]
  },
  {
    path: '/dict',
    component: Layout,
    name: 'dict',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'type/data/:dictType',
        component: () => import('@/views/system/dict/data/index.vue'),
        name: 'SystemDictData',
        meta: {
          title: '字典数据',
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          activeMenu: '/system/dict'
        }
      }
    ]
  },
  {
    path: '/system/role',
    component: Layout,
    name: 'SystemRoleCompatibility',
    meta: {
      hidden: true
    },
    children: [
      {
        path: '',
        component: () => import('@/views/system/role/index.vue'),
        name: 'SystemRoleCompatibilityIndex',
        meta: {
          title: '权限角色',
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'ep:user',
          activeMenu: '/system/role/permission-role',
          permission: ['system:role:query']
        }
      }
    ]
  },
  {
    path: '/system/post',
    component: Layout,
    name: 'SystemPostCompatibility',
    meta: {
      hidden: true
    },
    children: [
      {
        path: '',
        component: () => import('@/views/system/post/index.vue'),
        name: 'SystemPostCompatibilityIndex',
        meta: {
          title: '组织角色',
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'fa:address-book-o',
          activeMenu: '/system/role/organization-role',
          permission: ['system:post:query']
        }
      }
    ]
  },
  {
    path: '/codegen',
    component: Layout,
    name: 'CodegenEdit',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'edit',
        component: () => import('@/views/infra/codegen/EditTable.vue'),
        name: 'InfraCodegenEditTable',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'ep:edit',
          title: '修改生成配置',
          activeMenu: 'infra/codegen/index'
        }
      }
    ]
  },
  {
    path: '/job',
    component: Layout,
    name: 'JobL',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'job-log',
        component: () => import('@/views/infra/job/logger/index.vue'),
        name: 'InfraJobLog',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'ep:edit',
          title: '调度日志',
          activeMenu: 'infra/job/index'
        }
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/Login/Login.vue'),
    name: 'Login',
    meta: {
      hidden: true,
      title: t('router.login'),
      noTagsView: true
    }
  },
  {
    path: '/sso',
    component: () => import('@/views/Login/Login.vue'),
    name: 'SSOLogin',
    meta: {
      hidden: true,
      title: t('router.login'),
      noTagsView: true
    }
  },
  {
    path: '/social-login',
    component: () => import('@/views/Login/SocialLogin.vue'),
    name: 'SocialLogin',
    meta: {
      hidden: true,
      title: t('router.socialLogin'),
      noTagsView: true
    }
  },
  {
    path: '/approval-center',
    component: Layout,
    name: 'ApprovalCenter',
    redirect: '/approval-center/todo',
    meta: {
      title: '审批中心',
      icon: 'ep:finished',
      alwaysShow: true,
      approvalTodoBadge: true
    },
    children: [
      {
        path: 'todo',
        component: () => import('@/views/approval-center/index.vue'),
        name: 'ApprovalCenterTodo',
        meta: {
          noCache: false,
          keepAliveName: 'ApprovalCenterWorkbench',
          canTo: true,
          title: '待办',
          approvalTodoBadge: true,
          tagsViewKey: '/approval-center',
          tagsViewTitle: '审批中心',
          activeMenu: '/approval-center/todo',
          permission: ['bpm:task:query']
        }
      },
      {
        path: 'done',
        component: () => import('@/views/approval-center/index.vue'),
        name: 'ApprovalCenterDone',
        meta: {
          noCache: false,
          keepAliveName: 'ApprovalCenterWorkbench',
          canTo: true,
          title: '已办',
          tagsViewKey: '/approval-center',
          tagsViewTitle: '审批中心',
          activeMenu: '/approval-center/done',
          permission: ['bpm:task:query']
        }
      },
      {
        path: 'my-initiated',
        component: () => import('@/views/approval-center/index.vue'),
        name: 'ApprovalCenterMyInitiated',
        meta: {
          noCache: false,
          keepAliveName: 'ApprovalCenterWorkbench',
          canTo: true,
          title: '我发起的',
          tagsViewKey: '/approval-center',
          tagsViewTitle: '审批中心',
          activeMenu: '/approval-center/my-initiated',
          permission: ['bpm:task:query']
        }
      },
      {
        path: 'cc',
        component: () => import('@/views/approval-center/index.vue'),
        name: 'ApprovalCenterCc',
        meta: {
          noCache: false,
          keepAliveName: 'ApprovalCenterWorkbench',
          canTo: true,
          title: '抄送我的',
          tagsViewKey: '/approval-center',
          tagsViewTitle: '审批中心',
          activeMenu: '/approval-center/cc',
          permission: ['bpm:task:query']
        }
      },
      {
        path: 'manager',
        redirect: '/approval-center/manager/model',
        name: 'ApprovalCenterWorkflowManagement',
        meta: {
          title: '流程管理',
          icon: 'fa:dedent',
          alwaysShow: false
        },
        children: [
          {
            path: 'model',
            component: () => import('@/views/bpm/model/index.vue'),
            name: 'ApprovalCenterBpmModel',
            meta: {
              noCache: true,
              canTo: true,
              title: '流程模型',
              activeMenu: '/approval-center/manager/model',
              permission: ['bpm:model:query']
            }
          },
          {
            path: 'form',
            component: () => import('@/views/bpm/form/index.vue'),
            name: 'ApprovalCenterBpmForm',
            meta: {
              noCache: true,
              hidden: true,
              canTo: true,
              title: '流程表单',
              activeMenu: '/approval-center/manager/form',
              permission: ['bpm:form:query']
            }
          },
          {
            path: 'category',
            component: () => import('@/views/bpm/category/index.vue'),
            name: 'ApprovalCenterBpmCategory',
            meta: {
              noCache: true,
              hidden: true,
              canTo: true,
              title: '流程分类',
              activeMenu: '/approval-center/manager/category',
              permission: ['bpm:category:query']
            }
          },
          {
            path: 'business-approval-policy',
            component: () => import('@/views/bpm/businessApprovalPolicy/index.vue'),
            name: 'ApprovalCenterBpmBusinessApprovalPolicy',
            meta: {
              noCache: true,
              hidden: true,
              canTo: true,
              title: '业务审批策略',
              activeMenu: '/approval-center/manager/business-approval-policy',
              permission: ['bpm:business-approval-policy:query']
            }
          },
          {
            path: 'user-group',
            component: () => import('@/views/bpm/group/index.vue'),
            name: 'ApprovalCenterBpmUserGroup',
            meta: {
              noCache: true,
              hidden: true,
              canTo: true,
              title: '用户分组',
              activeMenu: '/approval-center/manager/user-group',
              permission: ['bpm:user-group:query']
            }
          },
          {
            path: 'process-expression',
            component: () => import('@/views/bpm/processExpression/index.vue'),
            name: 'ApprovalCenterBpmProcessExpression',
            meta: {
              noCache: true,
              hidden: true,
              canTo: true,
              title: '流程表达式',
              activeMenu: '/approval-center/manager/process-expression',
              permission: ['bpm:process-expression:query']
            }
          },
          {
            path: 'form-center',
            redirect: '/approval-center/manager/form-center/template',
            name: 'ApprovalCenterFormCenter',
            meta: {
              hidden: true,
              title: '表单中心',
              icon: 'ep:document',
              alwaysShow: false
            },
            children: [
              {
                path: 'template',
                component: () => import('@/views/form-center/template/index.vue'),
                name: 'ApprovalCenterFormCenterTemplate',
                meta: {
                  noCache: true,
                  hidden: true,
                  canTo: true,
                  title: '表单模板',
                  activeMenu: '/mdm/form-center/template',
                  permission: ['form:template:query']
                }
              },
              {
                path: 'policy',
                component: () => import('@/views/form-center/policy/index.vue'),
                name: 'ApprovalCenterFormCenterPolicy',
                meta: {
                  noCache: true,
                  hidden: true,
                  canTo: true,
                  title: '表单策略',
                  activeMenu: '/mdm/form-center/policy',
                  permission: ['form:policy:query']
                }
              },
              {
                path: 'effect',
                component: () => import('@/views/form-center/effect/index.vue'),
                name: 'ApprovalCenterFormCenterEffect',
                meta: {
                  noCache: true,
                  hidden: true,
                  canTo: true,
                  title: '生效待处理',
                  activeMenu: '/mdm/form-center/effect',
                  permission: ['form:effect:query']
                }
              }
            ]
          }
        ]
      },
      {
        path: 'oa',
        redirect: '/approval-center/oa/leave',
        name: 'ApprovalCenterOaExample',
        meta: {
          hidden: true,
          title: 'OA 示例',
          icon: 'fa:road'
        },
        children: [
          {
            path: 'leave',
            component: () => import('@/views/bpm/oa/leave/index.vue'),
            name: 'ApprovalCenterOALeave',
            meta: {
              noCache: true,
              hidden: true,
              canTo: true,
              title: '请假查询',
              permission: ['bpm:oa-leave:query']
            }
          }
        ]
      }
    ]
  },
  {
    path: '/signature-governance',
    component: Layout,
    name: 'SignatureGovernance',
    redirect: '/signature-governance/signature-records',
    meta: {
      hidden: true,
      title: '电子签名'
    },
    children: [
      {
        path: 'signature-records',
        component: signatureGovernanceView,
        name: 'SignatureGovernanceSignatureRecords',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '签名记录',
          activeMenu: '/signature-governance/signature-records',
          permission: ['signature-governance:policy:query']
        }
      },
      {
        path: 'my-signature',
        component: signatureGovernanceView,
        name: 'SignatureGovernanceMySignature',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '我的签名',
          activeMenu: '/signature-governance/my-signature',
          permission: ['signature-governance:policy:query']
        }
      },
      {
        path: 'file-signatures',
        redirect: '/signature-governance/signature-records',
        name: 'SignatureGovernanceFileSignatures',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '文件签名记录',
          activeMenu: '/signature-governance/signature-records',
          permission: ['signature-governance:policy:query']
        }
      },
      {
        path: 'batch-signatures',
        redirect: '/signature-governance/signature-records',
        name: 'SignatureGovernanceBatchSignatures',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '批记录签名记录',
          activeMenu: '/signature-governance/signature-records',
          permission: ['signature-governance:policy:query']
        }
      },
      {
        path: 'authorizations',
        component: signatureGovernanceView,
        name: 'SignatureGovernanceAuthorizations',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '用户授权',
          activeMenu: '/signature-governance/authorizations',
          permission: ['dcc:controlled-file:signature:manage']
        }
      },
      {
        path: 'retention',
        component: signatureGovernanceView,
        name: 'SignatureGovernanceRetention',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '长期留存',
          activeMenu: '/signature-governance/retention',
          permission: ['signature-governance:policy:query']
        }
      },
      {
        path: 'periodic-review',
        component: signatureGovernanceView,
        name: 'SignatureGovernancePeriodicReview',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '周期复核',
          activeMenu: '/signature-governance/periodic-review',
          permission: ['signature-governance:policy:query']
        }
      },
      {
        path: 'csv-package',
        component: signatureGovernanceView,
        name: 'SignatureGovernanceCsvPackage',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: 'CSV质量包',
          activeMenu: '/signature-governance/csv-package',
          permission: ['signature-governance:policy:query']
        }
      },
      {
        path: 'policy',
        component: signatureGovernanceView,
        name: 'SignatureGovernancePolicy',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '统一策略',
          activeMenu: '/signature-governance/policy',
          permission: ['signature-governance:policy:query']
        }
      }
    ]
  },
  {
    path: '/srm',
    component: Layout,
    name: 'SrmPortalHidden',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'portal/application',
        component: () => import('@/views/srm/supplier-portal/application/index.vue'),
        name: 'SrmSupplierPortalApplication',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '供应商资料提交'
        }
      }
    ]
  },
  {
    path: '/403',
    component: () => import('@/views/Error/403.vue'),
    name: 'NoAccess',
    meta: {
      hidden: true,
      title: '403',
      noTagsView: true
    }
  },
  {
    path: '/404',
    component: () => import('@/views/Error/404.vue'),
    name: 'NoFound',
    meta: {
      hidden: true,
      title: '404',
      noTagsView: true
    }
  },
  {
    path: '/500',
    component: () => import('@/views/Error/500.vue'),
    name: 'Error',
    meta: {
      hidden: true,
      title: '500',
      noTagsView: true
    }
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/Error/404.vue'),
    name: '',
    meta: {
      title: '404',
      hidden: true,
      breadcrumb: false
    }
  },
  {
    path: '/bpm',
    component: Layout,
    name: 'bpm',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'manager/model',
        component: () => import('@/views/bpm/model/index.vue'),
        name: 'BpmModel',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '流程模型',
          activeMenu: '/approval-center/manager/model'
        }
      },
      {
        path: 'manager/form',
        component: () => import('@/views/bpm/form/index.vue'),
        name: 'BpmForm',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '流程表单',
          activeMenu: '/approval-center/manager/form'
        }
      },
      {
        path: 'manager/category',
        component: () => import('@/views/bpm/category/index.vue'),
        name: 'BpmCategory',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '流程分类',
          activeMenu: '/approval-center/manager/category'
        }
      },
      {
        path: 'manager/business-approval-policy',
        component: () => import('@/views/bpm/businessApprovalPolicy/index.vue'),
        name: 'BpmBusinessApprovalPolicy',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '业务审批策略',
          activeMenu: '/approval-center/manager/business-approval-policy'
        }
      },
      {
        path: 'manager/user-group',
        component: () => import('@/views/bpm/group/index.vue'),
        name: 'BpmUserGroup',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '用户分组',
          activeMenu: '/approval-center/manager/user-group'
        }
      },
      {
        path: 'manager/process-expression',
        component: () => import('@/views/bpm/processExpression/index.vue'),
        name: 'BpmProcessExpression',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '流程表达式',
          activeMenu: '/approval-center/manager/process-expression'
        }
      },
      {
        path: 'manager/form/edit',
        component: () => import('@/views/bpm/form/editor/index.vue'),
        name: 'BpmFormEditor',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '设计流程表单',
          activeMenu: '/approval-center/manager/form'
        }
      },
      {
        path: 'manager/definition',
        component: () => import('@/views/bpm/model/definition/index.vue'),
        name: 'BpmProcessDefinition',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '流程定义',
          activeMenu: '/approval-center/manager/model'
        }
      },
      {
        path: 'process-instance/detail',
        component: () => import('@/views/bpm/processInstance/detail/index.vue'),
        name: 'BpmProcessInstanceDetail',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '流程详情',
          activeMenu: '/approval-center/todo'
        },
        props: (route) => ({
          id: route.query.id,
          taskId: route.query.taskId,
          activityId: route.query.activityId
        })
      },
      {
        path: 'process-instance/create',
        component: () => import('@/views/bpm/processInstance/create/index.vue'),
        name: 'BpmProcessInstanceCreate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '发起流程',
          activeMenu: '/approval-center/my-initiated'
        }
      },
      {
        path: 'process-instance/my',
        redirect: {
          name: 'ApprovalCenterMyInitiated',
          query: { moduleCode: 'BPM', viewType: 'MY_INITIATED' }
        },
        name: 'BpmProcessInstanceMy',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '我的申请',
          activeMenu: '/approval-center/my-initiated'
        }
      },
      {
        path: 'task/todo',
        redirect: {
          name: 'ApprovalCenterTodo',
          query: { moduleCode: 'BPM', viewType: 'TODO' }
        },
        name: 'BpmTodoTask',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '待办任务',
          activeMenu: '/approval-center/todo'
        }
      },
      {
        path: 'task/done',
        redirect: {
          name: 'ApprovalCenterDone',
          query: { moduleCode: 'BPM', viewType: 'DONE' }
        },
        name: 'BpmDoneTask',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '已办任务',
          activeMenu: '/approval-center/done'
        }
      },
      {
        path: 'task/copy',
        redirect: {
          name: 'ApprovalCenterCc',
          query: { moduleCode: 'BPM', viewType: 'CC' }
        },
        name: 'BpmProcessInstanceCopy',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '抄送我的',
          activeMenu: '/approval-center/cc'
        }
      },
      {
        path: 'process-instance/report',
        component: () => import('@/views/bpm/processInstance/report/index.vue'),
        name: 'BpmProcessInstanceReport',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '数据报表',
          activeMenu: '/approval-center/manager/model'
        }
      },
      {
        path: 'oa/leave',
        redirect: {
          name: 'ApprovalCenterOALeave'
        },
        name: 'BpmOALeave',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '请假查询'
        }
      },
      {
        path: 'oa/leave/create',
        component: () => import('@/views/bpm/oa/leave/create.vue'),
        name: 'OALeaveCreate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '发起 OA 请假'
        }
      },
      {
        path: 'oa/leave/detail',
        component: () => import('@/views/bpm/oa/leave/detail.vue'),
        name: 'OALeaveDetail',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '查看 OA 请假'
        }
      },
      {
        path: 'manager/model/create',
        component: () => import('@/views/bpm/model/form/index.vue'),
        name: 'BpmModelCreate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '创建流程',
          activeMenu: '/approval-center/manager/model'
        }
      },
      {
        path: 'manager/model/:type/:id',
        component: () => import('@/views/bpm/model/form/index.vue'),
        name: 'BpmModelUpdate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '修改流程',
          activeMenu: '/approval-center/manager/model'
        }
      }
    ]
  },
  {
    path: '/mall/product', // 商品中心
    component: Layout,
    name: 'ProductCenter',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'spu/add',
        component: () => import('@/views/mall/product/spu/form/index.vue'),
        name: 'ProductSpuAdd',
        meta: {
          noCache: false, // 需要缓存
          hidden: true,
          canTo: true,
          icon: 'ep:edit',
          title: '商品添加',
          activeMenu: '/mall/product/spu'
        }
      },
      {
        path: 'spu/edit/:id(\\d+)',
        component: () => import('@/views/mall/product/spu/form/index.vue'),
        name: 'ProductSpuEdit',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'ep:edit',
          title: '商品编辑',
          activeMenu: '/mall/product/spu'
        }
      },
      {
        path: 'spu/detail/:id(\\d+)',
        component: () => import('@/views/mall/product/spu/form/index.vue'),
        name: 'ProductSpuDetail',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'ep:view',
          title: '商品详情',
          activeMenu: '/mall/product/spu'
        }
      },
      {
        path: 'property/value/:propertyId(\\d+)',
        component: () => import('@/views/mall/product/property/value/index.vue'),
        name: 'ProductPropertyValue',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'ep:view',
          title: '商品属性值',
          activeMenu: '/product/property'
        }
      }
    ]
  },
  {
    path: '/mall/trade', // 交易中心
    component: Layout,
    name: 'TradeCenter',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'order/detail/:id(\\d+)',
        component: () => import('@/views/mall/trade/order/detail/index.vue'),
        name: 'TradeOrderDetail',
        meta: { title: '订单详情', icon: 'ep:view', activeMenu: '/mall/trade/order' }
      },
      {
        path: 'after-sale/detail/:id(\\d+)',
        component: () => import('@/views/mall/trade/afterSale/detail/index.vue'),
        name: 'TradeAfterSaleDetail',
        meta: { title: '退款详情', icon: 'ep:view', activeMenu: '/mall/trade/after-sale' }
      }
    ]
  },
  {
    path: '/dcc',
    component: Layout,
    name: 'DccCenterHidden',
    meta: { hidden: true },
    children: [
      {
        path: 'controlled-file/workbench',
        component: () => import('@/views/dcc/controlled-file/workbench/index.vue'),
        name: 'DccControlledFileWorkbench',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: 'DCC 工作台',
          activeMenu: '/dcc/controlled-file/workbench'
        }
      },
      {
        path: 'controlled-file/admin',
        component: () => import('@/views/dcc/controlled-file/admin/index.vue'),
        name: 'DccControlledFileAdmin',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '文控管理员',
          activeMenu: '/dcc/controlled-file/admin',
          permission: ['dcc:controlled-file:category:manage']
        }
      },
      {
        path: 'controlled-file/distribution',
        component: () => import('@/views/dcc/controlled-file/distribution/index.vue'),
        name: 'DccControlledFileDistribution',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '分发规则',
          activeMenu: '/dcc/controlled-file/categories'
        }
      },
      {
        path: 'controlled-file/training',
        component: () => import('@/views/dcc/controlled-file/training/index.vue'),
        name: 'DccControlledFileTraining',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '培训规则',
          activeMenu: '/dcc/controlled-file/categories'
        }
      },
      {
        path: 'controlled-file/training-mine',
        component: () => import('@/views/dcc/controlled-file/training/mine/index.vue'),
        name: 'DccControlledFileTrainingMineHidden',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '我的培训',
          activeMenu: '/dcc/controlled-file/training-mine'
        }
      },
      {
        path: 'controlled-file/print-template',
        component: () => import('@/views/dcc/controlled-file/print-template/index.vue'),
        name: 'DccApprovalPrintTemplate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '模板配置',
          activeMenu: '/dcc/controlled-file/print-template'
        }
      },
      {
        path: 'controlled-file/external-review',
        component: () => import('@/views/dcc/controlled-file/external-review/index.vue'),
        name: 'DccExternalFileReview',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '外来文件评审',
          activeMenu: '/dcc/controlled-file/upload'
        }
      },
      {
        path: 'controlled-file/external',
        component: () => import('@/views/dcc/controlled-file/external-review/index.vue'),
        name: 'DccExternalFileReviewAlias',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '外来文件评审',
          activeMenu: '/dcc/controlled-file/upload'
        }
      },
      {
        path: 'external-file-review',
        component: () => import('@/views/dcc/controlled-file/external-review/index.vue'),
        name: 'DccExternalFileReviewShortAlias',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '外来文件评审',
          activeMenu: '/dcc/controlled-file/upload'
        }
      },
      {
        path: 'controlled-file/detail/:id(\\d+)',
        component: () => import('@/views/dcc/controlled-file/detail/index.vue'),
        name: 'DccControlledFileDetail',
        beforeEnter: (to) => {
          const isApprovalHandling =
            String(to.query.handling || '') === 'approval' &&
            String(to.query.from || '') === 'approval-center' &&
            Boolean(String(to.query.processInstanceId || '') || String(to.query.taskId || ''))
          const isBrowserTraceability =
            String(to.query.traceability || '') === '1' &&
            String(to.query.from || '') === 'browser' &&
            Boolean(String(to.query.returnTo || ''))
          if (String(to.query.viewer || '') === '1' || isApprovalHandling || isBrowserTraceability) {
            return true
          }
          return { name: 'DccControlledFileBrowser' }
        },
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '受控文件详情',
          activeMenu: '/dcc/controlled-file/browser'
        }
      },
      {
        path: 'controlled-file/logs',
        component: () => import('@/views/dcc/controlled-file/logs/index.vue'),
        name: 'DccControlledFileLogs',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '文控日志',
          activeMenu: '/dcc/controlled-file/logs',
          permission: ['dcc:controlled-file:log:query']
        }
      },
      {
        path: 'controlled-file/positions',
        component: () => import('@/views/dcc/controlled-file/positions/index.vue'),
        name: 'DccControlledFilePositionsCompatibility',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '审批角色',
          activeMenu: '/system/role/approval-role',
          permission: ['dcc:controlled-file:position:manage']
        }
      },
      {
        path: 'controlled-file/training-task/:progressId(\\d+)',
        component: () => import('@/views/dcc/controlled-file/training/task/index.vue'),
        name: 'DccTrainingTask',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '培训任务',
          activeMenu: '/dcc/controlled-file/training-mine'
        }
      }
    ]
  },
  {
    path: '/member',
    component: Layout,
    name: 'MemberCenter',
    meta: { hidden: true },
    children: [
      {
        path: 'user/detail/:id',
        name: 'MemberUserDetail',
        meta: {
          title: '会员详情',
          noCache: true,
          hidden: true
        },
        component: () => import('@/views/member/user/detail/index.vue')
      }
    ]
  },
  {
    path: '/pay',
    component: Layout,
    name: 'pay',
    meta: { hidden: true },
    children: [
      {
        path: 'cashier',
        name: 'PayCashier',
        meta: {
          title: '收银台',
          noCache: true,
          hidden: true
        },
        component: () => import('@/views/pay/cashier/index.vue')
      }
    ]
  },
  {
    path: '/diy',
    name: 'DiyCenter',
    meta: { hidden: true },
    component: Layout,
    children: [
      {
        path: 'template/decorate/:id',
        name: 'DiyTemplateDecorate',
        meta: {
          title: '模板装修',
          noCache: false,
          hidden: true,
          activeMenu: '/mall/promotion/diy-template/diy-template'
        },
        component: () => import('@/views/mall/promotion/diy/template/decorate.vue')
      },
      {
        path: 'page/decorate/:id',
        name: 'DiyPageDecorate',
        meta: {
          title: '页面装修',
          noCache: false,
          hidden: true,
          activeMenu: '/mall/promotion/diy-template/diy-page'
        },
        component: () => import('@/views/mall/promotion/diy/page/decorate.vue')
      }
    ]
  },
  {
    path: '/crm',
    component: Layout,
    name: 'CrmCenter',
    meta: { hidden: true },
    children: [
      {
        path: 'clue/detail/:id',
        name: 'CrmClueDetail',
        meta: {
          title: '线索详情',
          noCache: true,
          hidden: true,
          activeMenu: '/crm/clue'
        },
        component: () => import('@/views/crm/clue/detail/index.vue')
      },
      {
        path: 'customer/detail/:id',
        name: 'CrmCustomerDetail',
        meta: {
          title: '客户详情',
          noCache: true,
          hidden: true,
          activeMenu: '/crm/customer'
        },
        component: () => import('@/views/crm/customer/detail/index.vue')
      },
      {
        path: 'business/detail/:id',
        name: 'CrmBusinessDetail',
        meta: {
          title: '商机详情',
          noCache: true,
          hidden: true,
          activeMenu: '/crm/business'
        },
        component: () => import('@/views/crm/business/detail/index.vue')
      },
      {
        path: 'contract/detail/:id',
        name: 'CrmContractDetail',
        meta: {
          title: '合同详情',
          noCache: true,
          hidden: true,
          activeMenu: '/crm/contract'
        },
        component: () => import('@/views/crm/contract/detail/index.vue')
      },
      {
        path: 'receivable-plan/detail/:id',
        name: 'CrmReceivablePlanDetail',
        meta: {
          title: '回款计划详情',
          noCache: true,
          hidden: true,
          activeMenu: '/crm/receivable-plan'
        },
        component: () => import('@/views/crm/receivable/plan/detail/index.vue')
      },
      {
        path: 'receivable/detail/:id',
        name: 'CrmReceivableDetail',
        meta: {
          title: '回款详情',
          noCache: true,
          hidden: true,
          activeMenu: '/crm/receivable'
        },
        component: () => import('@/views/crm/receivable/detail/index.vue')
      },
      {
        path: 'contact/detail/:id',
        name: 'CrmContactDetail',
        meta: {
          title: '联系人详情',
          noCache: true,
          hidden: true,
          activeMenu: '/crm/contact'
        },
        component: () => import('@/views/crm/contact/detail/index.vue')
      },
      {
        path: 'product/detail/:id',
        name: 'CrmProductDetail',
        meta: {
          title: '产品详情',
          noCache: true,
          hidden: true,
          activeMenu: '/crm/product'
        },
        component: () => import('@/views/crm/product/detail/index.vue')
      }
    ]
  },
  {
    path: '/ai',
    component: Layout,
    name: 'Ai',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'image/square',
        component: () => import('@/views/ai/image/square/index.vue'),
        name: 'AiImageSquare',
        meta: {
          title: '绘图作品',
          icon: 'ep:home-filled',
          noCache: false
        }
      },
      {
        path: 'knowledge/document',
        component: () => import('@/views/ai/knowledge/document/index.vue'),
        name: 'AiKnowledgeDocument',
        meta: {
          title: '知识库文档',
          icon: 'ep:document',
          noCache: false,
          activeMenu: '/ai/knowledge'
        }
      },
      {
        path: 'knowledge/document/create',
        component: () => import('@/views/ai/knowledge/document/form/index.vue'),
        name: 'AiKnowledgeDocumentCreate',
        meta: {
          title: '创建文档',
          icon: 'ep:plus',
          noCache: true,
          hidden: true,
          activeMenu: '/ai/knowledge'
        }
      },
      {
        path: 'knowledge/document/update',
        component: () => import('@/views/ai/knowledge/document/form/index.vue'),
        name: 'AiKnowledgeDocumentUpdate',
        meta: {
          title: '修改文档',
          icon: 'ep:edit',
          noCache: true,
          hidden: true,
          activeMenu: '/ai/knowledge'
        }
      },
      {
        path: 'knowledge/retrieval',
        component: () => import('@/views/ai/knowledge/knowledge/retrieval/index.vue'),
        name: 'AiKnowledgeRetrieval',
        meta: {
          title: '文档召回测试',
          icon: 'ep:search',
          noCache: true,
          hidden: true,
          activeMenu: '/ai/knowledge'
        }
      },
      {
        path: 'knowledge/segment',
        component: () => import('@/views/ai/knowledge/segment/index.vue'),
        name: 'AiKnowledgeSegment',
        meta: {
          title: '知识库分段',
          icon: 'ep:tickets',
          noCache: true,
          hidden: true,
          activeMenu: '/ai/knowledge'
        }
      },
      {
        path: 'console/workflow/create',
        component: () => import('@/views/ai/workflow/form/index.vue'),
        name: 'AiWorkflowCreate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '设计 AI 工作流',
          activeMenu: '/ai/console/workflow'
        }
      },
      {
        path: 'console/workflow/:type/:id',
        component: () => import('@/views/ai/workflow/form/index.vue'),
        name: 'AiWorkflowUpdate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '设计 AI 工作流',
          activeMenu: '/ai/console/workflow'
        }
      }
    ]
  },
  {
    path: '/iot',
    component: Layout,
    name: 'IOT',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'product/product/detail/:id',
        name: 'IoTProductDetail',
        meta: {
          title: '产品详情',
          noCache: true,
          tagsViewKeyMode: 'path',
          hidden: true,
          activeMenu: '/iot/device/product'
        },
        component: () => import('@/views/iot/product/product/detail/index.vue')
      },
      {
        path: 'device/detail/:id',
        name: 'IoTDeviceDetail',
        meta: {
          title: '设备详情',
          noCache: true,
          tagsViewKeyMode: 'path',
          hidden: true,
          activeMenu: '/iot/device/device'
        },
        component: () => import('@/views/iot/device/device/detail/index.vue')
      },
      {
        path: 'ota/operation/firmware/detail/:id',
        name: 'IoTOtaFirmwareDetail',
        meta: {
          title: '固件详情',
          noCache: true,
          hidden: true,
          activeMenu: '/iot/operation/ota/firmware'
        },
        component: () => import('@/views/iot/ota/firmware/detail/index.vue')
      }
    ]
  },
  {
    path: '/mdm',
    component: Layout,
    name: 'MdmHiddenRoutes',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'form-center/template/simulate',
        component: () => import('@/views/form-center/template/FormTemplateSimulatePage.vue'),
        name: 'MdmFormCenterTemplateSimulate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          title: '表单模板模拟填写',
          activeMenu: '/mdm/form-center/template',
          permission: ['form:template:query']
        }
      }
    ]
  },
  {
    path: '/mes',
    component: Layout,
    name: 'MesWmRouter',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'wm/warehouse/location',
        component: () => import('@/views/mes/wm/warehouse/location/index.vue'),
        name: 'MesWmLocation',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '库区设置',
          activeMenu: '/mes/wm/warehouse'
        }
      },
      {
        path: 'wm/warehouse/area',
        component: () => import('@/views/mes/wm/warehouse/area/index.vue'),
        name: 'MesWmArea',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '库位设置',
          activeMenu: '/mes/wm/warehouse'
        }
      },
      {
        path: 'pro/task/gantt-edit',
        component: () => import('@/views/mes/pro/task/edit/index.vue'),
        name: 'MesProTaskGanttEdit',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '甘特图编辑',
          activeMenu: '/mes/pro/task'
        }
      },
      {
        path: 'pro/schedule-calendar',
        component: () => import('@/views/mes/pro/task/calendar/index.vue'),
        name: 'MesProScheduleCalendar',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '排程日历',
          activeMenu: '/mes/pro/task'
        }
      },
      {
        path: 'pro/route/edit/:id',
        component: () => import('@/views/mes/pro/route/RouteEditPage.vue'),
        name: 'MesProRouteEdit',
        meta: {
          noCache: false,
          hidden: true,
          canTo: true,
          icon: '',
          title: '工艺流程',
          noTagsView: true,
          tagsViewKeyMode: 'path',
          activeMenu: '/mes/pro/route'
        }
      },
      {
        path: 'pro/feedback/edhr-work-task',
        component: () => import('@/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue'),
        name: 'MesProEdhrWorkTaskBoard',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR 工作任务',
          activeMenu: '/mes/pro/feedback/edhr-work-task',
          permission: ['mes:pro-edhr-work-task:query']
        }
      },
      {
        path: 'pro/feedback/edhr-execution/form',
        component: () => import('@/views/mes/pro/edhr/ExecutionPage.vue'),
        name: 'MesProFeedbackEdhrExecutionForm',
        meta: {
          noCache: false,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR执行表单',
          activeMenu: '/mes/pro/feedback/edhr-batch-execution'
        }
      },
      {
        path: 'pro/feedback/edhr-nonconformance-review',
        component: () => import('@/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'),
        name: 'MesProFeedbackEdhrNonconformanceReview',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '不合格评审',
          activeMenu: '/mes/pro/feedback/edhr-batch-execution',
          permission: ['mes:pro-edhr-nonconformance-review:query']
        }
      },
      {
        path: 'pro/feedback/edhr-domain-trace',
        component: () => import('@/views/mes/pro/edhr/DomainTracePage.vue'),
        name: 'MesProFeedbackEdhrDomainTrace',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '主数据追溯',
          activeMenu: '/mes/pro/feedback/edhr-domain-trace',
          permission: ['mes:pro-batch-record-execution:domain-trace-query']
        }
      },
      {
        path: 'pro/feedback/edhr-domain-trace/detail',
        component: () => import('@/views/mes/pro/edhr/DomainTraceDetailPage.vue'),
        name: 'MesProFeedbackEdhrDomainTraceDetail',
        meta: {
          noCache: false,
          hidden: true,
          canTo: true,
          icon: '',
          title: '主数据追溯详情',
          activeMenu: '/mes/pro/feedback/edhr-domain-trace',
          permission: ['mes:pro-batch-record-execution:domain-trace-query']
        }
      },
      {
        path: 'pro/feedback/edhr-form-trace',
        component: () => import('@/views/mes/pro/edhr/FormTracePage.vue'),
        name: 'MesProFeedbackEdhrFormTrace',
        meta: {
          noCache: true,
          tagsViewKeyMode: 'path',
          hidden: true,
          canTo: true,
          icon: '',
          title: '表单追溯',
          activeMenu: '/mes/pro/feedback/edhr-form-trace',
          permission: ['mes:pro-batch-record-execution:track', 'mes:pro-edhr-change:query', 'mes:pro-edhr-release:query']
        }
      },
      {
        path: 'pro/feedback/edhr-release',
        component: () => import('@/views/mes/pro/edhr-release/ReleasePage.vue'),
        name: 'MesProEdhrReleasePage',
        meta: {
          noCache: true,
          tagsViewKeyMode: 'path',
          hidden: true,
          canTo: true,
          icon: '',
          title: '放行追溯',
          activeMenu: '/mes/pro/feedback/edhr-release',
          permission: ['mes:pro-edhr-release:query']
        }
      },
      {
        path: 'pro/feedback/edhr-field-audit',
        component: () => import('@/views/mes/pro/edhr/FieldAuditPage.vue'),
        name: 'MesProFeedbackEdhrFieldAudit',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '字段审计链',
          activeMenu: '/mes/pro/feedback/edhr-field-audit',
          permission: ['mes:pro-batch-record-execution:field-audit-query']
        }
      },
      {
        path: 'pro/feedback/edhr-field-audit/detail',
        component: () => import('@/views/mes/pro/edhr/FieldAuditDetailPage.vue'),
        name: 'MesProFeedbackEdhrFieldAuditDetail',
        meta: {
          noCache: false,
          hidden: true,
          canTo: true,
          icon: '',
          title: '字段审计详情',
          activeMenu: '/mes/pro/feedback/edhr-field-audit',
          permission: ['mes:pro-batch-record-execution:field-audit-query']
        }
      },
      {
        path: 'pro/feedback/edhr-change',
        component: () => import('@/views/mes/pro/edhr/RecordChangePage.vue'),
        name: 'MesProFeedbackEdhrRecordChange',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR变更记录',
          activeMenu: '/mes/pro/feedback/edhr-change',
          permission: ['mes:pro-edhr-change:query']
        }
      },
      {
        path: 'pro/feedback/edhr-operation-audit',
        component: () => import('@/views/mes/pro/edhr/OperationAuditPage.vue'),
        name: 'MesProFeedbackEdhrOperationAudit',
        meta: {
          noCache: false,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR操作审计',
          activeMenu: '/mes/pro/feedback/edhr-operation-audit',
          permission: ['mes:pro-edhr-operation-audit:query']
        }
      },
      {
        path: 'pro/feedback/edhr-permission-matrix',
        component: () => import('@/views/mes/pro/edhr/PermissionMatrixPage.vue'),
        name: 'MesProFeedbackEdhrPermissionMatrix',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR对象权限',
          activeMenu: '/mes/pro/feedback/edhr-permission-matrix',
          permission: ['mes:pro-edhr-permission-scope:evaluate']
        }
      },
      {
        path: 'pro/feedback/edhr-approval/detail',
        component: () => import('@/views/mes/pro/edhr/ApprovalDetailPage.vue'),
        name: 'MesProFeedbackEdhrApprovalDetail',
        meta: {
          noCache: false,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR 审批详情',
          activeMenu: '/mes/pro/feedback/edhr-approval',
          permission: ['mes:pro-batch-record-execution:approve']
        }
      },
      {
        path: 'pro/feedback/edhr-batch-execution',
        component: () => import('@/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'),
        name: 'MesProEdhrBatchExecutionListPage',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR批次执行',
          activeMenu: '/mes/pro/feedback/edhr-batch-execution',
          permission: ['mes:pro-edhr-batch-execution:query']
        }
      },
      {
        path: 'pro/feedback/edhr-batch-execution/detail',
        component: () => import('@/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
        name: 'MesProEdhrBatchExecutionDetail',
        meta: {
          noCache: false,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR批次详情',
          activeMenu: '/mes/pro/feedback/edhr-batch-execution',
          permission: ['mes:pro-edhr-batch-execution:query']
        }
      },
      {
        path: 'pro/feedback/edhr-form-fill-log',
        component: () => import('@/views/mes/pro/edhr/FormFillLogPage.vue'),
        name: 'MesProEdhrFormFillLogPage',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '表单日志',
          activeMenu: '/mes/pro/feedback/edhr-form-fill-log',
          permission: ['mes:pro-edhr-form-fill-log:query']
        }
      },
      {
        path: 'pro/feedback/edhr-batch-execution/review',
        component: () => import('@/views/mes/pro/edhr-batch/BatchExecutionReviewPage.vue'),
        name: 'MesProEdhrBatchExecutionReview',
        meta: {
          noCache: false,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR批次复盘',
          activeMenu: '/mes/pro/feedback/edhr-batch-execution',
          permission: ['mes:pro-edhr-batch-execution:query']
        }
      },
      {
        path: 'pro/feedback/edhr-batch-execution/template',
        component: () => import('@/views/mes/pro/edhr-batch/BatchExecutionTemplatePage.vue'),
        name: 'MesProEdhrBatchExecutionTemplate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR批次模板',
          activeMenu: '/mes/pro/feedback/edhr-batch-execution',
          permission: ['mes:pro-edhr-batch-execution:query']
        }
      },
      {
        path: 'pro/feedback/edhr-batch-execution/template-simulate',
        component: () => import('@/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue'),
        name: 'MesProEdhrBatchExecutionTemplateSimulate',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR模板模拟填写',
          activeMenu: '/mes/pro/feedback/edhr-batch-execution',
          permission: ['mes:pro-edhr-batch-execution:query']
        }
      },
      {
        path: 'pro/feedback/edhr-batch-production-fill',
        component: () => import('@/views/mes/pro/edhr-batch/BatchProductionFillPage.vue'),
        name: 'MesProEdhrBatchProductionFill',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '一线生产',
          activeMenu: '/mes/pro/feedback/edhr-batch-production-fill',
          permission: ['mes:pro-edhr-batch-execution:query']
        }
      },
      {
        path: 'pro/feedback/edhr-batch-pqc-fill',
        component: () => import('@/views/mes/pro/edhr-batch/BatchPqcFillPage.vue'),
        name: 'MesProEdhrBatchPqcFill',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '一线PQC',
          activeMenu: '/mes/pro/feedback/edhr-batch-pqc-fill',
          permission: ['mes:pro-edhr-batch-execution:query']
        }
      },
      {
        path: 'pro/feedback/edhr-batch-test',
        component: () => import('@/views/mes/pro/edhr-batch/BatchRecordTestPage.vue'),
        name: 'MesProEdhrBatchRecordTest',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '批记录测试',
          activeMenu: '/mes/pro/feedback/edhr-batch-test',
          permission: ['mes:pro-edhr-batch-execution:query']
        }
      },
      {
        path: 'pro/feedback/edhr-batch-page-graph',
        component: () => import('@/views/mes/pro/edhr-batch/BatchPageGraphPage.vue'),
        name: 'MesProEdhrBatchPageGraph',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '批记录页面关系图',
          activeMenu: '/mes/pro/feedback/edhr-batch-page-graph',
          permission: ['mes:pro-edhr-batch-execution:query']
        }
      },
      {
        path: 'pro/feedback/edhr-signatures',
        component: () => import('@/views/mes/pro/edhr/SignaturePage.vue'),
        name: 'MesProFeedbackEdhrSignatures',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR签名记录',
          activeMenu: '/mes/pro/feedback/edhr-signatures',
          permission: ['mes:pro-batch-record-execution:signature-query']
        }
      },
      {
        path: 'pro/feedback/edhr-label',
        component: () => import('@/views/mes/pro/edhr-label-print/LabelPrintQueuePage.vue'),
        name: 'MesProFeedbackEdhrLabel',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR标签管理',
          activeMenu: '/mes/pro/feedback/edhr-label',
          permission: ['mes:pro-edhr-label:query']
        }
      },
      {
        path: 'pro/feedback/edhr-unified-change',
        component: () => import('@/views/mes/pro/edhr-unified-change/UnifiedChangePage.vue'),
        name: 'MesProFeedbackEdhrUnifiedChange',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR统一变更',
          activeMenu: '/mes/pro/feedback/edhr-unified-change',
          permission: ['mes:pro-edhr-unified-change:query']
        }
      },
      {
        path: 'pro/process-pool/review-copy',
        component: () => import('@/views/mes/pro/processpool/ReviewCopyPage.vue'),
        name: 'MesProProcessPoolReviewCopy',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '工序池审核副本',
          activeMenu: '/mes/pro/process-pool/review-copy',
          permission: ['mes:pro-process-pool-review-copy:generate-submit']
        }
      },
      {
        path: 'pro/process-pool/event-revision',
        component: () => import('@/views/mes/pro/processpool/EventRevisionPage.vue'),
        name: 'MesProProcessPoolEventRevision',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '工序池原始记录修改',
          activeMenu: '/mes/pro/process-pool/event-revision',
          permission: ['mes:pro-process-pool:event-revision:update']
        }
      },
      {
        path: 'pro/process-pool/team-leader',
        component: () => import('@/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
        name: 'MesProProcessPoolTeamLeader',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '工序池班组长工作台',
          activeMenu: '/mes/pro/process-pool/team-leader',
          permission: ['mes:pro-process-pool-team-leader:query']
        }
      },
      {
        path: 'pro/process-pool/qa-regulation',
        component: () => import('@/views/mes/pro/processpool/QaRegulationPage.vue'),
        name: 'MesProProcessPoolQaRegulation',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'QA 规程配置',
          activeMenu: '/mes/pro/process-pool/qa-regulation',
          permission: ['mes:qa-inspection-regulation:query']
        }
      },
      {
        path: 'pro/process-pool/production-leader',
        component: () => import('@/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue'),
        name: 'MesProProcessPoolProductionLeaderWorkbench',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '生产组长',
          activeMenu: '/mes/pro/process-pool/production-leader',
          permission: ['mes:pro-process-pool-team-leader:query']
        }
      },
      {
        path: 'pro/process-pool/pqc-leader',
        component: () => import('@/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue'),
        name: 'MesProProcessPoolPqcLeaderWorkbench',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'PQC组长',
          activeMenu: '/mes/pro/process-pool/pqc-leader',
          permission: ['mes:pro-process-pool-pqc-leader:query']
        }
      },
      {
        path: 'pro/batch-record-cell-link',
        component: () => import('@/views/mes/pro/batchrecordcelllink/index.vue'),
        name: 'MesProBatchRecordCellLink',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: '批记录单元格链接',
          activeMenu: '/mes/pro/batch-record-form-list',
          permission: ['mes:pro-batch-record-cell-link:query']
        }
      },
      {
        path: 'pro/feedback/edhr-flow-intervention',
        component: () => import('@/views/mes/pro/edhr-flow-intervention/FlowInterventionPage.vue'),
        name: 'MesProFeedbackEdhrFlowIntervention',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR流程干预',
          activeMenu: '/mes/pro/feedback/edhr-flow-intervention',
          permission: ['mes:pro-edhr-flow-intervention:query']
        }
      },
      {
        path: 'pro/feedback/edhr-dhr-template',
        component: () => import('@/views/mes/pro/edhr-dhr-template/DhrTemplatePage.vue'),
        name: 'MesProFeedbackEdhrDhrTemplate',
        meta: {
          noCache: false,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'DHR模板目录',
          activeMenu: '/mes/pro/feedback/edhr-dhr-template',
          permission: ['mes:pro-edhr-dhr-template:query']
        }
      },
      {
        path: 'pro/feedback/edhr-form',
        component: () => import('@/views/mes/pro/edhr-form/FormPage.vue'),
        name: 'MesProFeedbackEdhrForm',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR独立表单',
          activeMenu: '/mes/pro/feedback/edhr-form',
          permission: ['mes:pro-edhr-form-instance:query']
        }
      },
      {
        path: 'pro/feedback/edhr-deployment-evidence',
        component: () => import('@/views/mes/pro/edhr-deployment/DeploymentPage.vue'),
        name: 'MesProFeedbackEdhrDeploymentEvidence',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          title: 'eDHR部署证据',
          activeMenu: '/mes/pro/feedback/edhr-deployment',
          permission: ['mes:pro-edhr-deployment:query']
        }
      },
    ]
  }
]

export default remainingRouter
