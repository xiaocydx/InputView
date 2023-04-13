InputView是一个通用的输入控件
* 支持Activity和Dialog。
* 在调整IME高度后运行过渡动画。
* 可自定义编辑器的切换过渡动画。
* 可自定义编辑器的类型和视图创建。
* 实现Edge-to-Edge并提供相关的辅助函数。
* 修改Android 11及以上IME动画的时长和插值器。
* 解决EditText的水滴状指示器导致动画卡顿的问题。  
* 提供Android 11以下Window属性包含FLAG_FULLSCREEN的兼容方案。

&nbsp;
### 示例代码在Android 12上的效果
#### 切换编辑器和调整IME高度
IME的全称是Input Method Editors（输入法编辑器），因此将底部视图统一称为Editor（编辑器）。

https://user-images.githubusercontent.com/43429149/213995075-bfdf23a0-e758-4230-bca4-97701cb9a234.mp4

&nbsp;
#### 手势导航栏Edge-to-Edge
当前是手势导航栏时，对Emoji编辑器添加paddingBottom和增加高度，滚动时内容绘制在paddingBottom区域，滚动到底部时留出paddingBottom区域，内容不会被手势导航栏遮挡。

https://user-images.githubusercontent.com/43429149/213995177-2b7cc060-d44a-463c-856b-2aaa28c44dcf.mp4
