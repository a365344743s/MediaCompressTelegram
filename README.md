[![](https://jitpack.io/v/a365344743s/MediaCompressTelegram.svg)](https://jitpack.io/#a365344743s/MediaCompressTelegram)

# MediaCompressTelegram
Android 视频、图片(暂未实现)、音频(暂未实现)压缩库 from [Telegram](https://github.com/DrKLO/Telegram)

对比 [MediaCompressSignal](https://github.com/a365344743s/MediaCompressSignal)

| 项目            | Telegram | Signal |
|---------------|:--------:|:------:|
| 最低Android版本支持 |    18    |   26   |
| aar大小         |   6.8M   |  114K  |

Telegram 6.8M的大小是包含4个abi架构的so大小，实际使用单个abi会减小4.6-5M
Signal 114K的大小也包含4个abi架构的so大小，实际使用单个abi会减小6.8-11.4K

# Telegram Commit
提交： 6cb1cdf898a8cfe025b907b79d074c4903d4b424 [6cb1cdf]
父级： 43401a515c
作者： xaxtix <xardas3200@gmail.com>
日期： 2022年7月4日 15:54:30
提交者： xaxtix
update to 8.8.5

# USAGE
## Telegram(最低支持AndroidApi18)

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
        implementation 'com.github.a365344743s:MediaCompressTelegram:1.0.0'
	}

### 初始化

    org.telegram.messenger.VideoConvertUtil.init(scheduler);

### 开始转换

    Integer convertId = org.telegram.messenger.VideoConvertUtil.startVideoConvert(srcPath, dstPath, listener)

### 取消转换

    org.telegram.messenger.VideoConvertUtil.stopVideoConvert(int convertId);

# 输出视频控制

## 说明
视频码率决定了视频清晰度，码率越大视频越清晰，但是文件会变大。

视频分辨率决定了视频宽高，分辨率越大视频宽高越大。

相同视频码率下，视频分辨率越大，视频越模糊。

可以控制 输出视频码率、最大边。

## 计算方法
1.根据源视频分辨率，计算目标视频分辨率。

    VideoConvertUtil.createCompressionSettings(String videoPath)

若 源视频最大边 > 1280，目标视频最大边 = 1280。

若 1280 >= 源视频最大边 > 854, 目标视频最大边 = 848。

若 854 >= 源视频最大边 > 640, 目标视频最大边 = 640。

若 源视频最大边 <= 640, 目标视频最大边 = 432。

2.计算目标视频码率

    VideoConvertUtil.makeVideoBitrate(int originalHeight, int originalWidth, int originalBitrate, int height, int width)

以 1080p、720p、480p 分出四等码率范围，最终计算出目标视频码率。

## 视频码率控制
可以修改 VideoConvertUtil.makeVideoBitrate 中的码率计算方法。

## 视频分辨率控制
可以修改 VideoConvertUtil.createCompressionSettings 中的视频分辨率计算方法