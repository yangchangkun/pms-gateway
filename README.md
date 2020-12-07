## 平台简介



## 系统特性
- 友好的代码结构及注释，便于阅读及二次开发
- 实现前后端分离，通过token进行数据交互，前端再也不用关注后端技术
- 灵活的权限控制，可控制到页面或按钮，满足绝大部分的权限需求
- 页面交互使用Vue2.x，极大的提高了开发效率
- 引入API模板，根据token作为登录令牌，极大的方便了APP接口开发
- 引入Hibernate Validator校验框架，轻松实现后端校验
- 引入swagger文档支持，方便编写API接口文档

## 内置功能

1.  用户管理：用户是系统操作者，该功能主要完成系统用户配置。
2.  部门管理：配置系统组织机构（公司、部门、小组），树结构展现支持数据权限。
3.  岗位管理：配置系统用户所属担任职务。
4.  菜单管理：配置系统菜单，操作权限，按钮权限标识等。
5.  角色管理：角色菜单权限分配、设置角色按机构进行数据范围权限划分。
6.  字典管理：对系统中经常使用的一些较为固定的数据进行维护。
7.  参数管理：对系统动态配置常用参数。
8.  通知公告：系统通知公告信息发布维护。
9.  操作日志：系统正常操作日志记录和查询；系统异常信息日志记录和查询。
10. 登录日志：系统登录日志记录查询包含登录异常。
11. 定时任务：在线（添加、修改、删除)任务调度包含执行结果日志。
12. 连接池监视：监视当期系统数据库连接池状态，可进行分析SQL找出系统性能瓶颈。

## 项目结构
com.pms     
├── pms-activiti                            // 工作流模块，已经集成了工作流流程绘制模块
├── pms-admin                               // web应用模块
├── pms-common                              // 工具类
│       └── annotation                     // 自定义注解
│       └── base                           // 通用基类
│       └── config                         // 全局配置（主要是处理yml配置文件）
│       └── constant                       // 通用常量
│       └── enums                          // 通用枚举（通常，系统用到的枚举值都放这里）
│       └── exception                      // 全局异常
│       └── json                           // JSON数据处理
│       └── page                           // mybatis 分页插件 pageHelper 
│       └── support                        // 通用类型转换处理
│       └── utils                          // 通用工具类处理（一般，工具型的类都在这）
│       └── validator                      // hibernate-validator校验工具类
│       └── xss                            // XSS过滤处理
├── pms-core                                // 系统业务处理核心
│       └── java                       
│               └── domain                 // 存放所有和数据库表对应的实体Entity
│               └── mapper                 // 存放所有和resource.mapper对应的接口（这里存放原子操作的接口）
│               └── repository             // 存在JPA 接口（建议增删改的操作使用JPA，不用些SQL语句）
│               └── service                // 所有的业务接口定义
│                       └── impl           // 所有的业务接口实现（注意，一般情况下，事务控制在这里）
│       └── resource                     
│               └── mapper                 // mybatis sql 语句隐射配置
├── pms-framework                           // 框架核心
│       └── aspectj                        // 注解实现
│       └── config                         // 系统配置
│       └── datasource                     // 数据权限
│       └── manager                        // 异步处理
│       └── shiro                          // 权限控制
│       └── util                           // 通用工具
│       └── web                            // 前端控制
├── pms-gateway                             // 网关（如果有客户端或者h5接入，其接口都放在这）
├── pms-scheduler                           // 定时任务和批处理（新增加的定时任务代码添加在com.pms.quartz.task包下）



## 技术选型：
- 核心框架：	Spring boot 		2.1.1.RELEASE
- 视图框架：	Spring MVC	4.3	
- 安全权限框架：	Apache shiro	1.4.0	
- 数据校验框架：	Hibernate Validator		
			
- 数据库：	mysql	5.7	
- 数据库开发框架：	mybatis	1.3.2	mybatis-spring-boot-starter
- 数据持久化框架：	spring boot JPA 		spring-boot-starter-data-jpa
- 数据库连接池：	druid	1.1.10	阿里数据库连接池
- 数据库驱动：mysql-connector-java		
- 分页插件：pagehelper		
			
- 批量调度框架：quartz		
- 工作流引擎：	activiti	5.22.0	
			
- 缓存：redis		开发环境用单机，生产环境用哨兵或集群	
- 日志管理：SLF4J 1.7、Log4j
- json序列化：fastjson	1.2.45	
			
- 接口文档：swagger	2.7.0
- 前端交互框架页面交互：Vue2.x 
- 前端框架：hplus(基于Bootstrap3)
<br> 

SpringBoot框架
1、介绍
Spring Boot是一款开箱即用框架，提供各种默认配置来简化项目配置。让我们的Spring应用变的更轻量化、更快的入门。 在主程序执行main函数就可以运行。你也可以打包你的应用为jar并通过使用java -jar来运行你的Web应用。它遵循"约定优先于配置"的原则， 使用SpringBoot只需很少的配置，大部分的时候直接使用默认的配置即可。可以与Spring Cloud的微服务无缝结合。
Spring Boot2.0 环境要求必须是jdk8或以上版本，Tomcat8或以上版本

2、优点

使编码变得简单： 推荐使用注解。
使配置变得简单： 自动配置、快速构建项目、快速集成新技术能力 没有冗余代码生成和XML配置的要求
使部署变得简单： 内嵌Tomcat、Jetty、Undertow等web容器，无需以war包形式部署
使监控变得简单： 自带项目监控
Shiro安全控制
1、介绍
Apache Shiro是Java的一个安全框架。Shiro可以帮助我们完成：认证、授权、加密、会话管理、与Web集成、缓存等。其不仅可以用在 JavaSE环境，也可以用在 JavaEE 环境。

2、优点

易于理解的 Java Security API
简单的身份认证，支持多种数据源
对角色的简单的授权，支持细粒度的授权
不跟任何的框架或者容器捆绑，可以独立运行
3、特性
Authentication身份认证/登录，验证用户是不是拥有相应的身份
Authorization授权，即验证权限，验证某个已认证的用户是否拥有某个权限，即判断用户是否能做事情 SessionManagement会话管理，即用户登录后就是一次会话，在没有退出之前，它的所有信息都在会话中
Cryptography加密，保护数据的安全性，如密码加密存储到数据库，而不是明文存储
Caching缓存，比如用户登录后，其用户信息，拥有的角色/权限不必每次去查，提高效率
ConcurrencyShiro支持多线程应用的并发验证，即如在一个线程中开启另一个线程，能把权限自动传播过去
Testing提供测试支持
RunAs允许一个用户假装为另一个用户（如果他们允许）的身份进行访问
RememberMe记住我，这是非常常见的功能，即一次登录后，下次再来的话不用登录了

4、架构
Subject主体，代表了当前的“用户”，这个用户不一定是一个具体的人，与当前应用交互的任何东西都是Subject，如网络爬虫， 机器人等；即一个抽象概念；所有Subject都绑定到SercurityManager，与Subject的所有交互都会委托给SecurityManager；可以把Subject认为是一个门面；SecurityManager才是实际的执行者
SecurityManage安全管理器；即所有与安全有关的操作都会与SecurityManager交互；且它管理着所有Subject； 可以看出它是Shiro的核心，它负责与后边介绍的其他组件进行交互
Realm域，Shiro从Realm获取安全数据（如用户，角色，权限），就是说SecurityManager要验证用户身份， 那么它需要从Realm获取相应的用户进行比较以确定用户身份是否合法；也需要从Realm得到用户相应的角色/权限进行验证用户是否能进行操作；可以有1个或多个Realm，我们一般在应用中都需要实现自己的Realm
SessionManager如果写过Servlet就应该知道Session的概念，Session需要有人去管理它的生命周期，这个组件就是SessionManager
SessionDAODAO大家都用过，数据库访问对象，用于会话的CRUD，比如我们想把Session保存到数据库，那么可以实现自己的SessionDAO，也可以写入缓存，以提高性能
CacheManager缓存控制器，来管理如用户，角色，权限等的缓存的；因为这些数据基本上很少去改变，放到缓存中后可以提高访问的性能

应用代码通过Subject来进行认证和授权，而Subject又委托给SecurityManager； 我们需要给Shrio的SecurityManager注入Realm，从而让SecurityManager能得到合法的用户及其权限进行判断，Shiro不提供维护用户/权限，而是通过Realm让开发人员自己注入。

Shiro不会去维护用户，维护权限；这些需要自己去设计/提供；然后通过响应的接口注入给Shiro即可


## 本地部署
- 通过代码仓库工具下载源码
- 创建数据库pms，数据库编码为UTF-8
- 执行document/sql下的脚本，初始化数据
- 修改application-*.yml，更新MySQL账号和密码
- Eclipse、IDEA运行YckScaffoldApplication.java，则可启动项目
- 项目访问路径：http://localhost:8080,注意如果发布的时候指定了发布路径，则common.js里面的baseURL要对应的修改为发布路径
- 项目数据监控路径：http://localhost:8080/druid/index.html
- 账号密码：admin/admin
- Swagger路径：http://localhost:8080/swagger/index.html

在项目的目录下执行 bin/package.bat 
然后会在项目下生成 target文件夹包含 war 或jar （多模块生成在pms-admin）

1、jar部署方式
使用命令行执行：java –jar pms.jar 或者执行脚本：bin/run.bat

2、war部署方式
pom.xml packaging修改为war 放入tomcat服务器webapps





