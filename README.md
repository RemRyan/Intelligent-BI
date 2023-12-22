# 智能BI平台

## 项目介绍

本项目是基于 React + Spring Boot + RabbitMQ + AIGC 的智能 BI 数据分析平台。

> AIGC :  Artificial Intelligence Generated Content

区别于传统的 BI，数据分析者只需要导入最原始的数据集，输入想要分析的目标，就能根据 AI 自动生成一个符合要求的图表以及分析结论。让不会数据分析的人也能通过输入目标快速完成数据分析，大幅节约人力成本。此外，本平台还会有图表管理、异步生成等功能。

## 业务流程

**基础流程：**

![智能BI平台基础流程图](https://663450fd.telegraph-image-ban.pages.dev/file/ddc0a638ff226efdb3673.jpg)

**优化流程（异步化）：**

![智能BI平台优化流程图（异步化）](https://663450fd.telegraph-image-ban.pages.dev/file/a12b7375f40e72ea72766.jpg)

## 快速启动

### 前端

环境要求：Node.js >= 16

安装依赖：

```shell
yarn or npm install
```

启动：

```shell
yarn run dev or npm run start:dev
```

部署：

```shell
yarn build or npm run build
```

### 后端

执行 sql 目录下的 create_table.sql

## 技术选型

#### 前端

Ant Design Pro

React

Ant Design Pro

Umi

OpenAPI 代码生成

ECharts 图表生成

#### 后端

Spring Boot

Spring MVC

MyBatis + MyBatis Plus 数据访问（分页）

Spring AOP 切面编程

Spring 事务注解

Redis：Redisson限流控制

RabbitMQ：消息队列

AI SDK

JDK 线程池及异步化

Swagger + Knife4j 接口文档

Easy Excel：表格数据处理

Hutool 工具库

## 功能展示

### 用户登录注册

![智能BI平台用户登录页面](https://663450fd.telegraph-image-ban.pages.dev/file/0b91888111a47770f99b0.jpg)

![智能BI平台用户注册页面](https://663450fd.telegraph-image-ban.pages.dev/file/bdfde0b79b0686cf1a041.jpg)

### 同步分析数据

![智能BI平台同步分析数据页面](https://663450fd.telegraph-image-ban.pages.dev/file/f3c6699c36724123143a3.jpg)

### 异步分析数据

![智能BI平台异步分析数据页面](https://663450fd.telegraph-image-ban.pages.dev/file/a60d9081ef2026e915cca.jpg)

### 图表管理

![智能BI平台图表管理页面](https://663450fd.telegraph-image-ban.pages.dev/file/0fadbfbac2640b3bb1856.jpg)

### 查看图表原始数据

![智能BI平台图表原始数据页面](https://663450fd.telegraph-image-ban.pages.dev/file/2ca64a02292bcb360a6ee.jpg)