[![](https://jitpack.io/v/a365344743s/MediaCompressSignal.svg)](https://jitpack.io/#a365344743s/MediaCompressSignal)

# MediaCompress
 Android 视频、图片(暂未实现)、音频(暂未实现)压缩库 from [Telegram](https://github.com/DrKLO/Telegram) And [Signal](https://github.com/signalapp/Signal-Android)

# Telegram Commit
 提交： 6cb1cdf898a8cfe025b907b79d074c4903d4b424 [6cb1cdf]
 父级： 43401a515c
 作者： xaxtix <xardas3200@gmail.com>
 日期： 2022年7月4日 15:54:30
 提交者： xaxtix
 update to 8.8.5

# Signal Commit
提交： 96539d70dffdff0386b80afa1c8022059405dee4 [96539d7]
父级： 07570bbfec
作者： Alex Hart <alex@signal.org>
日期： 2022年7月13日 15:59:23
提交者： Alex Hart
提交时间： 2022年7月13日 15:59:28
Bump version to 5.43.3

# USAGE
## Telegram(最低支持AndroidApi16)
### 初始化
org.telegram.messenger.VideoConvertUtil.init(scheduler);

### 开始转换
Integer convertId = org.telegram.messenger.VideoConvertUtil.startVideoConvert(srcPath, dstPath, listener)

### 取消转换
org.telegram.messenger.VideoConvertUtil.stopVideoConvert(int convertId);


## Signal(最低支持AndroidApi26)
### 初始化
org.thoughtcrime.securesms.util.VideoConvertUtil.init(context, scheduler);

### 开始转换
Integer convertId = org.thoughtcrime.securesms.util.VideoConvertUtil.startVideoConvert(srcPath, dstPath, upperSizeLimit, true/false, listener)

### 取消转换
org.thoughtcrime.securesms.util.VideoConvertUtil.stopVideoConvert(int convertId);