Android activity滑动返回原理
=====================

###最底部的Activity不能是透明的，可能会出现奇怪的问题

像fragment一样，activity本身是不可以滑动的，但是我们可以制造一个正在滑动activity的假象，使得看起来这个activity正在被手指滑动。其原理其实很简单，我们滑动的其实是activity里面的可见view元素，而我们将activity设置为透明的，这样当view滑过的时候，由于activity的底部是透明的，我们就可以在滑动过程中看到下面的activity，这样看起来就是在滑动activity。所以activity滑动效果分两步，1，设置透明，2，滑动view


1. 设置透明：
	很简单，建立一个Style，在Style里面添加下面两行并将这个style应用在activity上就可以了
	```
	<item name="android:windowBackground">@*android:color/transparent</item>
	<item name="android:windowIsTranslucent">true</item>
	```
	
2. 先看看activity的层次结构：我们用的activity的xml的根view并不是activity的根view，在它上面还有一个父view，id是android.R.id.content，再向上一层，还有一个view，它是一个LinearLayout，它除了放置我们创建的view之外，还放置我们的xml之外的一些东西比如放ActionBar或者标题栏什么的。而再往上一级，就到了activity的根view——DecorView。如下图
	![](http://i.imgur.com/kBuvnRM.png)

	要做到像iOS那样可以滑动整个activity，只滑动我们在xml里面创建的view显然是不对的，因为我们还有标题栏、ActionBar什么的，所以我们要滑动的应该是DecorView或者倒数第二层的那个view。

	而要滑动view的话，我们要重写其父窗口的onInterceptTouchEvent以及onTouchEvent【当然使用setOnTouchListener不是不可能，但是如果子view里面有一个消费了onTouch事件，那么也就接收不到了】，但是窗口的创建过程不是我们能控制的，DecorView的创建都不是我们能干预的。解决办法就是，我们自己创建一个SwipeLayout，然后人为地插入顶层view中，放置在DecorView和其下面的LinearLayout中间，随着手指的滑动，不断改变SwipeLayout的子view——曾经是DecorView的子view——的位置，这样我们就可以控制activity的滑动啦。我们在activity的onPostCreate方法中调用swipeLayout.replaceLayer替换我们的SwipeLayout，代码如下

```
	public void replaceLayer(Activity activity) {
	    mActivity = activity;
	    screenWidth = getScreenWidth(activity);
	    setClickable(true);
	    ViewGroup root = (ViewGroup) activity.getWindow().getDecorView();
	    content = root.getChildAt(0);
	    ViewGroup.LayoutParams params = content.getLayoutParams();
	    ViewGroup.LayoutParams params2 = new ViewGroup.LayoutParams(-1, -1);
	    root.removeView(content);
	    this.addView(content, params2);
	    root.addView(this, params);
	    sideWidth = (int) (sideWidthInDP * activity.getResources().getDisplayMetrics().density);
	}
```

然后我们把这些写成一个SwipeActivity，其它activity只要继承这个SwipeActivity就可以实现滑动返回功能（当然Style仍然要设置的）
这里只说滑动activity的原理，剩下的都是控制滑动的事了，详见代码

如果开启了混淆，添加如下代码
```
-keep class net.nashlegend.swipetofinishactivity.SwipeActivity$* { *; }
```

----------

BTW，滑动Fragment原理其实一样，只不过更加简单，Fragment在view树中就是它inflate的元素，用fragment.getView可以取得，滑动fragment其实滑动的就是fragment.getView。只要把滑动方法写在它父view中就可以了
