# 数据库初始化

-- 创建库
create database if not exists Intelligent_BI;
create database if not exists intelligentbi;

-- 切换库
use Intelligent_BI;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    email        varchar(256)                           null comment '邮箱',
    gender       tinyint                                null comment '性别 0-男/1-女',
    phone        varchar(11)                            null comment '手机号',
    birthday     DATE                                   null comment '生日',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    balance      bigint       default 30                not null comment '用户积分余额 默认注册赠送30积分',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_userAccount (userAccount)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 图表信息表 chart
create table if not exists chart
(
    id          bigint auto_increment comment 'id' primary key,
    `name`      varchar(128)                       null comment '图表名称',
    goal        text                               null comment '分析目标',
    chartData   text                               null comment '图表数据',
    chartType   varchar(128)                       null comment '图表类型',
    genChart    text                               null comment '生成的图表数据',
    genResult   text                               null comment '生成的分析结论',
    status      varchar(128)                       not null default 'wait' comment 'wait,running,succeed,failed',
    execMessage text                               null comment '执行信息',
    userId      bigint                             null comment '创建用户 id',
    userAvatar  varchar(1024)                      null comment '创建用户头像',
    tags        varchar(1024)                      null comment '图表标签 json 列表',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
) comment '图表信息表' collate = utf8mb4_unicode_ci;


-- 每日签到表
create table if not exists daily_check_in
(
    id          bigint auto_increment comment 'id' primary key,
    userId      bigint                             not null comment '签到人',
    description varchar(256)                       null comment '描述',
    addPoints   bigint   default 10                not null comment '签到增加积分个数',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '每日签到表';
