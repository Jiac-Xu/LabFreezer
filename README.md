# LabFreezer

<p align="center">
  <img src="icon.png" width="120" />
</p>

<h3 align="center">A local-first freezer box management app for laboratory samples</h3>

<p align="center">
  <a href="#english">English</a> | <a href="#中文">中文</a>
</p>

---

<a id="english"></a>

## English

## Introduction

**LabFreezer** is an Android application designed for researchers to manage samples stored in laboratory freezer boxes.

It provides:

- Freezer box and sample management
- Fast sample search
- Photo records and notes
- OCR-based information auto fill
- Local data export and import

LabFreezer follows a **local-first design philosophy**:

- All data is stored locally
- No cloud dependency
- No mandatory account system
- Suitable for laboratories with privacy requirements

## Features

### 🧊 Freezer Box Management

- Manage multiple freezers and boxes
- Organize samples with hierarchical structures
- Quickly locate stored samples

### 🔍 Smart Search

Search by sample name, alias, notes, dates and storage locations.

### 📷 Photo Recording

Attach local images for freezer boxes, sample labels and experimental notes.

### 🔤 OCR Auto Fill

Extract information from sample labels using local OCR processing. Images are not uploaded to external services.

### 📦 Data Export & Import

Support local migration of sample information and related images.

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Hilt |
| Database | Room |
| Image Loading | Coil |
| OCR | PaddleOCR |

## Privacy

LabFreezer is designed for laboratory data privacy.

- No experimental data upload
- No cloud storage required
- No user tracking
- All records remain on the device

## Installation

```bash
git clone https://github.com/Jiac-Xu/LabFreezer.git
```

Build with Android Studio or:

```bash
./gradlew assembleDebug
```

Requirements:

- Android Studio
- JDK 17+
- Android SDK

---

<a id="中文"></a>

## 中文

## 项目简介

**冰盒（LabFreezer）** 是一款面向科研人员的 Android 冰箱样本管理工具，用于记录和管理实验室冻存样本。

主要功能包括：

- 冰箱、盒子、样本位置管理
- 样本快速搜索
- 图片和备注记录
- OCR 自动识别与信息填充
- 本地数据导入导出

LabFreezer 遵循 **本地优先（Local-first）** 设计理念：

- 所有数据保存在本地设备
- 不依赖云服务
- 无需注册账号
- 适用于对实验数据隐私有要求的环境

## 功能特点

### 🧊 冰盒管理

- 管理多个冰箱和冻存盒
- 支持层级化组织样本
- 快速定位样本位置

### 🔍 智能搜索

支持通过以下信息查找样本：

- 样本名称
- 别名
- 备注
- 日期
- 存储位置

### 📷 图片记录

支持记录：

- 冰盒照片
- 样本标签照片
- 实验备注图片

图片均保存在本地。

### 🔤 OCR 自动填充

通过本地 OCR 识别样本标签信息，减少手动录入。

识别过程不上传图片至服务器。

### 📦 数据导入导出

支持：

- 导出样本信息
- 导出关联图片
- 跨设备迁移数据
- 合并数据并避免 ID 冲突

## 技术栈

| 模块 | 技术 |
|---|---|
| 开发语言 | Kotlin |
| UI | Jetpack Compose |
| 架构 | MVVM + Hilt |
| 数据库 | Room |
| 图片加载 | Coil |
| OCR | PaddleOCR |

## 隐私保护

LabFreezer 专注于实验数据隐私：

- 不上传实验数据
- 不依赖云端存储
- 不收集用户信息
- 数据完全由用户掌控

## 开源构建

```bash
git clone https://github.com/Jiac-Xu/LabFreezer.git
```

使用 Android Studio 打开项目即可构建。

环境要求：

- Android Studio
- JDK 17+
- Android SDK

---

## Author

Created by **Jiac-Xu**
