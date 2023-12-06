InputView是一个通用的输入控件
* 支持Activity和Dialog。
* 调整IME高度后运行动画。
* 可自定义编辑器的切换动画。
* 可自定义编辑器的View或Fragment。
* 实现Edge-to-Edge并提供相关的辅助函数。
* 解决EditText水滴状指示器导致动画卡顿的问题。  
* 修改Android 11及以上IME动画的时长和插值器。
* 提供Android 11以下Window属性包含FLAG_FULLSCREEN的兼容方案。
> IME的全称是Input Method Editors（输入法编辑器），因此将IME和底部视图统一称为Editor（编辑器）。
<br/> 

[InputView的使用说明](https://www.yuque.com/u12192380/khwdgb/pi0b7rdhvr16z7gm)

1. 在根目录的build.gradle添加
```
allprojects {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

2. 在module的build.gradle添加
```
dependencies {
    // 修改IME动画的属性、调用FLAG_FULLSCREEN的兼容方案，需要依赖inputview-reflect
    def version = "1.1.0"
    implementation "com.github.xiaocydx.InputView:inputview:${version}"
    implementation "com.github.xiaocydx.InputView:inputview-reflect:${version}"
}
```

&nbsp;
### 示例代码在Android 12上的效果
#### 切换编辑器和调整IME高度
https://user-images.githubusercontent.com/43429149/213995075-bfdf23a0-e758-4230-bca4-97701cb9a234.mp4

&nbsp;
#### 手势导航栏Edge-to-Edge
当前是手势导航栏时，对Emoji编辑器添加paddingBottom和增加高度，滚动时内容绘制在paddingBottom区域，滚动到底部时留出paddingBottom区域，内容不会被手势导航栏遮挡。

https://user-images.githubusercontent.com/43429149/213995177-2b7cc060-d44a-463c-856b-2aaa28c44dcf.mp4

&nbsp;
#### 复杂的切换场景
InputView可用于视频编辑这类复杂的切换场景。

https://github.com/xiaocydx/InputView/assets/43429149/2d2931cb-c585-4c34-aaa2-43ed44268d5e



