# BML 许可证离线签发工具

## 简介

BML License Keygen 是一个**离线运行的许可证签发工具**，用于为部署在客户服务器上的 BML 企业中台管理系统生成带 RSA 数字签名的许可证文件（`.lic`）。

许可证通过 RSA-2048 私钥签名，后端使用对应公钥验签。客户即使拥有数据库完全访问权限，也无法伪造有效许可证。

## 环境要求

- **JDK** 21+
- **Maven** 3.8.9+

## 快速开始

### 1. 构建

```bash
cd bml-license-keygen
mvn clean package -DskipTests
```

### 2. 运行

**方式一：双击 bat 脚本（推荐）**
```
双击 run-keygen.bat
```
> 首次运行时，如果尚未构建，bat 脚本会自动执行 `mvn clean package`。

**方式二：命令行运行**
```bash
java -jar target/bml-license-keygen.jar
```

### 3. 首次使用流程

1. **启动工具** → 自动生成 RSA-2048 密钥对，保存在 `keys/` 目录
2. **选择 [2] 查看 RSA 公钥** → 复制 Base64 公钥字符串
3. **配置公钥到后端** → 设置环境变量 `BML_LICENSE_PUBLIC_KEY=<公钥字符串>`
   或在 `application.yml` 中配置 `bml.license.public-key: <公钥字符串>`
4. **重新构建并部署后端** → 后端即可验证该密钥对签发的许可证
5. **选择 [1] 签发新许可证** → 交互式填写客户信息，生成 `.lic` 文件
6. **交付 `.lic` 文件给客户** → 客户在 BML 中台管理页面上传激活

## 目录结构

```
bml-license-keygen/
├── keys/                              ← RSA 密钥对（自动生成，请妥善保管）
│   ├── bml-license-private.key        ← 私钥（绝不能泄露给客户！）
│   └── bml-license-public.key         ← 公钥（需配置到后端）
├── output/                            ← 签发输出目录
│   ├── BML-LIC-CUST001-20260414.lic   ← 许可证文件（交付给客户）
│   └── BML许可证签发记录.xlsx          ← 签发历史 Excel 记录
├── src/                               ← 源代码
├── pom.xml                            ← Maven 构建配置
├── run-keygen.bat                     ← Windows 启动脚本
└── README.md                          ← 本文件
```

## 许可证文件格式

```
Base64(JSON payload)
---BML-SIG---
Base64(RSA-SHA256 signature)
```

**payload JSON 结构：**

```json
{
  "licenseId": "LIC-20260414-A1B2C3D4",
  "customerName": "龙域智能科技",
  "customerCode": "CUST-001",
  "productVersion": "2.0.0",
  "features": ["api_gateway", "enterprise", "system", "monitor", "alert"],
  "maxApiAccounts": 10,
  "maxUsersPerAccount": 50,
  "maxTotalUsers": 200,
  "issueDate": "2026-04-14",
  "expireDate": "2027-04-14",
  "remark": ""
}
```

## 安全说明

| 文件 | 保密级别 | 说明 |
|------|---------|------|
| `bml-license-private.key` | **最高机密** | RSA 私钥，泄露则客户可自行签发许可证 |
| `bml-license-public.key` | 公开 | 配置到后端，仅能验签不能签发 |
| `*.lic` | 按客户分发 | 每个客户一份，不同客户不可互用 |
| `BML许可证签发记录.xlsx` | 内部 | 签发历史追溯，建议版本控制 |

## 常见问题

### Q: 重新生成密钥对会怎样？
A: 所有已签发的许可证将失效，需要用新密钥重新签发，并更新后端的公钥配置。

### Q: 客户可以修改许可证文件吗？
A: 不能。许可证的 payload 经过 RSA-SHA256 签名，修改任何内容都会导致签名验证失败。

### Q: 后端如何验证许可证？
A: 后端 `BmlLicenseVerifier` 使用嵌入的公钥验证签名，`BmlLicenseHolder` 缓存验证后的许可证对象，`LicenseCheckInterceptor` 在每次请求前检查。
