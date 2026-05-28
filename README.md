# YunTongVPN - 云通VPN安卓客户端

## 项目概述
云通VPN安卓客户端，模仿快连VPN的一键连接UI风格，基于Xray-core实现，对接V2Board面板。

## 技术栈
- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3 (暗色主题)
- **VPN核心**: Xray-core (libxray.so via JNI)
- **网络**: Retrofit2 + Gson
- **数据库**: Room
- **DI**: Hilt
- **架构**: MVVM + Repository

## 支持协议
- ✅ AnyTLS (主力协议，最隐蔽)
- ✅ VLESS + WS + TLS (CF CDN备用)
- ✅ Hysteria2 (高速UDP)
- ✅ ShadowsocksR (兼容旧节点)

## 功能特性
- 🔘 一键连接/断开
- 🌍 多服务器选择，按协议分类
- 📊 实时上传/下载速度显示
- 👤 V2Board账户登录
- 📋 订阅自动更新 (6小时)
- 🔔 VPN状态通知栏
- 📱 流量使用统计

## 编译步骤

### 前置条件
1. Android Studio Hedgehog | 2023.1.1+
2. JDK 17
3. Android SDK 34
4. NDK (r25c+)

### 获取libxray
需要先编译Xray-core的Android AAR:
`
git clone https://github.com/XTLS/libxray.git
cd libxray
# 按照libxray文档编译，将生成的libxray.aar放到:
# YunTongVPN/app/libs/libxray.aar
`

### 编译
1. 用Android Studio打开YunTongVPN项目
2. 等待Gradle sync完成
3. Build > Build APK(s)

## 项目结构
`
YunTongVPN/
├── app/
│   ├── build.gradle.kts
│   ├── libs/
│   │   └── libxray.aar          # Xray-core Android库
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/yuntong/vpn/
│       │   ├── YunTongApp.kt     # Application
│       │   ├── api/              # V2Board API + Hilt模块
│       │   ├── model/            # 数据模型 + Room数据库
│       │   ├── service/          # VPN服务 + 订阅更新
│       │   ├── ui/               # Compose UI组件
│       │   │   ├── MainActivity.kt
│       │   │   ├── VpnMainScreen.kt   # 主界面(一键连接)
│       │   │   ├── ServerListScreen.kt
│       │   │   ├── LoginScreen.kt
│       │   │   ├── AccountScreen.kt
│       │   │   └── theme/Theme.kt
│       │   └── util/             # Xray配置生成器
│       └── res/                  # 资源文件
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
`

## V2Board API对接
- Base URL: https://yuntong.org/api/v1/client/
- 登录: POST passport/auth/login
- 用户信息: GET user/info
- 节点列表: GET user/server/fetch
- 订阅: GET user/subscribe

## 注意事项
1. libxray.aar需要自行编译，项目不内置
2. VPN核心运行在tun模式，需要Android VpnService权限
3. 部分协议(AnyTLS)需要最新的Xray-core版本支持
4. 生产环境需要配置签名和混淆

## License
Proprietary - 云通VPN