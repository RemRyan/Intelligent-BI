export default [
  {
    path: '/user', layout: false, routes: [
      {path: '/user/login', component: './User/Login'},
      {path: '/user/Register', component: './User/Register'}
    ]
  },
  {path: '/', redirect: '/add_chart'},
  {path: '/welcome', name: '首页', icon: 'smileOutlined', component: './Welcome'},
  {path: '/add_chart', name: '智能分析', icon: 'barChart', component: './AddChart'},
  {path: '/add_chart_async', name: '智能分析（异步）', icon: 'barChart', component: './AddChartAsync'},
  {path: '/my_chart', name: '我的图表', icon: 'pieChart', component: './MyChart'},
  {path: '/account/userCenter', name: '个人中心', icon: 'userOutlined', component: './User/UserCenter', hideInMenu: true,  },
  {path: '/account/userBalance', name: '个人积分', icon: 'userOutlined', component: './User/UserBalance', hideInMenu: true,  },
  {path: '/MyChartInfo/:id', icon: 'checkCircle', component: './MyChartInfo', name: '查看图表信息', hideInMenu: true,  },
  {
    path: '/admin',
    name: '管理员',
    icon: 'crown',
    access: 'canAdmin',
    routes: [
      {path: '/admin/userManager', name: '用户管理', component: './Admin/UserManager'},
      {path: '/admin/chartManager', name: '图表管理', component: './Admin/ChartManager'},
    ],
  },
  {path: '/', redirect: '/welcome'},
  {path: '*', layout: false, component: './404'},
];
