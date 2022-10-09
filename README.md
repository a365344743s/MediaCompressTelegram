[![](https://jitpack.io/v/a365344743s/MediaCompressTelegram.svg)](https://jitpack.io/#a365344743s/MediaCompressTelegram)

# MediaCompressTelegram
 Android 视频、图片(暂未实现)、音频(暂未实现)压缩库 from [Telegram](https://github.com/DrKLO/Telegram)

# Telegram Commit
 提交： 6cb1cdf898a8cfe025b907b79d074c4903d4b424 [6cb1cdf]
 父级： 43401a515c
 作者： xaxtix <xardas3200@gmail.com>
 日期： 2022年7月4日 15:54:30
 提交者： xaxtix
 update to 8.8.5

# USAGE
## Telegram(最低支持AndroidApi16)

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