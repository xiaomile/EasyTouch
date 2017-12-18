package com.skkk.easytouch.Services;

import android.accessibilityservice.AccessibilityService;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.skkk.easytouch.AppSelectActivity;
import com.skkk.easytouch.Configs;
import com.skkk.easytouch.R;
import com.skkk.easytouch.Receiver.AdminManageReceiver;
import com.skkk.easytouch.Utils.IntentUtils;
import com.skkk.easytouch.Utils.PackageUtils;
import com.skkk.easytouch.Utils.SpUtils;

import static com.skkk.easytouch.Configs.DEFAULT_ALPHA;
import static com.skkk.easytouch.Configs.DEFAULT_THEME;
import static com.skkk.easytouch.Configs.DEFAULT_TOUCH_HEIGHT;
import static com.skkk.easytouch.Configs.DEFAULT_TOUCH_WIDTH;
import static com.skkk.easytouch.Configs.DEFAULT_VIBRATE_LEVEL;
import static com.skkk.easytouch.Configs.TOUCH_UI_DIRECTION_LEFT;

public class EasyTouchLinearService extends EasyTouchBaseService implements View.OnTouchListener {
    private static final String TAG = "EasyTouchLinearService";
    private WindowManager.LayoutParams mParams;
    private WindowManager.LayoutParams mMenuDetailParams;
    private WindowManager.LayoutParams mMenuParams;

    private View touchView;
    private float lastX;
    private float lastY;
    private boolean isMove;
    private ImageView ivTouchBottom;
    private ImageView ivTouchMid;
    private ImageView ivTouchTop;
    private LinearLayout llTouchContainer;
    private GestureDetector midDetector;
    private GestureDetector topDetector;

    private GestureDetector bottomDetector;
    private float dx;
    private float dy;

    private int touchWidth = DEFAULT_TOUCH_WIDTH;//悬浮条的宽度 单位dp
    private int touchHeight = DEFAULT_TOUCH_HEIGHT;//悬浮条的高度 单位dp
    private
    @DrawableRes
    int topDrawable = R.drawable.shape_react_corners_top;//上方触摸块背景
    private
    @DrawableRes
    int midDrawable = R.drawable.shape_react_corners_mid;//中部触摸块背景
    private
    @DrawableRes
    int bottomDrawable = R.drawable.shape_react_corners_bottom;//下方触摸块背景
    private int topColor;
    private int midColor;
    private int bottomColor;
    private int colorAlpha;
    private int screenWidth;
    private int screenHeight;
    private int leftBorder;
    private int rightBorder;
    private int direction;
    private int directionX;

    private int minTouchSlop = 10;    //触摸滑动最小触发距离
    private LinearLayout llMenuContainer;
    private SeekBar sbSystemAudio, sbMediaAudio, sbAlarmAudio;


    private ImageView ivAudioSystem;
    private ImageView ivAudioMedia;
    private ImageView ivAudioAlarm;
    private TextView tvAudioMode;
    private Switch switchMode;
    private LinearLayout llMenuBottomContainer;
    private ImageView ivAlipayScan;
    private ImageView ivAlipayPay;
    private ImageView ivWeixinScan;
    private View menuDetailView;
    private RelativeLayout containerMenuDetailVoice;
    private LinearLayout containerMenuDetailVoiceContent;
    private RelativeLayout containerMenuDetailVoiceBack;
    private ImageView ivMenuDetailVoiceBack;
    private RelativeLayout containerMenuDetailPay;
    private LinearLayout containerMenuDetailPayContent;
    private RelativeLayout containerMenuDetailPayBack;
    private ImageView ivMenuDetailPayBack;
    private RelativeLayout containerMenuDetailApps;
    private ImageView ivMenuDetailBack;
    private RelativeLayout containerMenuDetailAppsBack;
    private ImageView ivMenuDetailAppBack;
    private LinearLayout containerMenuDetailAppsContent;
    private LinearLayout containerMenuDetailAppsTop;
    private LinearLayout containerMenuDetailAppsBottom;

    private boolean isMenuDetailShow = false;

    private ObjectAnimator hideMenuDetailAnim;
    private final int HIDE_MENU_DETAIL_FAST = 100;
    private final int HIDE_MENU_DETAIL_SLOW = 200;

    private View menuView;
    private RelativeLayout menuContainer;
    private ImageView ivMenuBall0, ivMenuBall1, ivMenuBall2, ivMenuBall3, ivMenuBall4;
    private boolean isMenuShow = false;
    private int menuWidth, menuHeight;
    private AnimatorSet ballMenuAnimSet;
    private ObjectAnimator menuBallScaleXAnim;
    private ObjectAnimator menuBallScaleYAnim;
    private ObjectAnimator menuBallAlphAnim;
    private ObjectAnimator transXAnimShow;
    private ObjectAnimator transYAnimShow;
    private int lastAnimX;
    private int lastAnimY;


    @Override
    public void onCreate() {
        super.onCreate();


        //设置界面窗口管理器
        if (windowManager == null) {
            windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        }


        //设置音量管理器
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        //设置设置管理器
        mAdminName = new ComponentName(this, AdminManageReceiver.class);
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        //设置震动管理器
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        //设置屏幕尺寸
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        //设置左右边界
        leftBorder = 0;
        rightBorder = screenWidth;

        menuWidth = dp2px(200f);
        menuHeight = dp2px(300f);

        //设置布局管理器
        mParams = new WindowManager.LayoutParams();
        mParams.packageName = getPackageName();
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

        mParams.format = PixelFormat.RGBA_8888;
        mParams.gravity = Gravity.LEFT | Gravity.TOP;

        //设置一级菜单的LP
        mMenuParams = new WindowManager.LayoutParams();
        mMenuParams.packageName = getPackageName();
        mMenuParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mMenuParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mMenuParams.flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mMenuParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        mMenuParams.format = PixelFormat.RGBA_8888;
        mMenuParams.gravity = Gravity.LEFT | Gravity.TOP;

        //设置二级菜单的LP
        mMenuDetailParams = new WindowManager.LayoutParams();
        mMenuDetailParams.packageName = getPackageName();
        mMenuDetailParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mMenuDetailParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mMenuDetailParams.flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mMenuDetailParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        mMenuDetailParams.format = PixelFormat.RGBA_8888;
        mMenuDetailParams.gravity = Gravity.LEFT | Gravity.TOP;

        //设置左右方向
        direction = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_DIRECTION, TOUCH_UI_DIRECTION_LEFT);
        if (direction == TOUCH_UI_DIRECTION_LEFT) {
            directionX = leftBorder;
        } else {
            directionX = rightBorder;
        }

        //设置悬浮窗的位置
        mParams.x = directionX;
        mParams.y = screenHeight - dp2px(200f);

        //初始化悬浮窗的控件内容
        touchView = View.inflate(getApplicationContext(), R.layout.layout_easy_touch, null);
        llTouchContainer = (LinearLayout) touchView.findViewById(R.id.ll_touch_container);
        ivTouchTop = (ImageView) touchView.findViewById(R.id.iv_touch_top);
        ivTouchMid = (ImageView) touchView.findViewById(R.id.iv_touch_mid);
        ivTouchBottom = (ImageView) touchView.findViewById(R.id.iv_touch_bottom);

        llMenuContainer = (LinearLayout) touchView.findViewById(R.id.ll_menu_container);
        sbSystemAudio = (SeekBar) touchView.findViewById(R.id.sb_system_audio);
        sbMediaAudio = (SeekBar) touchView.findViewById(R.id.sb_media_audio);
        sbAlarmAudio = (SeekBar) touchView.findViewById(R.id.sb_alarm_audio);
        ivAudioSystem = (ImageView) touchView.findViewById(R.id.iv_audio_system);
        ivAudioMedia = (ImageView) touchView.findViewById(R.id.iv_audio_media);
        ivAudioAlarm = (ImageView) touchView.findViewById(R.id.iv_audio_alarm);
        switchMode = (Switch) touchView.findViewById(R.id.switch_mode);
        tvAudioMode = (TextView) touchView.findViewById(R.id.tv_audio_mode);

        llMenuBottomContainer = (LinearLayout) touchView.findViewById(R.id.ll_menu_bottom_container);
        ivAlipayScan = (ImageView) touchView.findViewById(R.id.iv_scan_alipay);
        ivAlipayPay = (ImageView) touchView.findViewById(R.id.iv_pay_alipay);
        ivWeixinScan = (ImageView) touchView.findViewById(R.id.iv_scan_weixin);


        menuView = View.inflate(getApplicationContext(), R.layout.layout_easy_touch_linear_menu, null);
        menuContainer = (RelativeLayout) menuView.findViewById(R.id.container_menu_ball);
        ivMenuBall0 = (ImageView) menuView.findViewById(R.id.iv_menu_ball_0);
        ivMenuBall1 = (ImageView) menuView.findViewById(R.id.iv_menu_ball_1);
        ivMenuBall2 = (ImageView) menuView.findViewById(R.id.iv_menu_ball_2);
        ivMenuBall3 = (ImageView) menuView.findViewById(R.id.iv_menu_ball_3);
        ivMenuBall4 = (ImageView) menuView.findViewById(R.id.iv_menu_ball_4);


        menuDetailView = View.inflate(getApplicationContext(), R.layout.layout_easy_touch_ball_menu_detail, null);

        containerMenuDetailVoice = (RelativeLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_voice);
        sbSystemAudio = (SeekBar) menuDetailView.findViewById(R.id.sb_system_audio);
        sbMediaAudio = (SeekBar) menuDetailView.findViewById(R.id.sb_media_audio);
        sbAlarmAudio = (SeekBar) menuDetailView.findViewById(R.id.sb_alarm_audio);
        ivAudioSystem = (ImageView) menuDetailView.findViewById(R.id.iv_audio_system);
        ivAudioMedia = (ImageView) menuDetailView.findViewById(R.id.iv_audio_media);
        ivAudioAlarm = (ImageView) menuDetailView.findViewById(R.id.iv_audio_alarm);
        switchMode = (Switch) menuDetailView.findViewById(R.id.switch_mode);
        tvAudioMode = (TextView) menuDetailView.findViewById(R.id.tv_audio_mode);
        containerMenuDetailVoiceContent = (LinearLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_voice_content);
        containerMenuDetailVoiceBack = (RelativeLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_voice_back);
        ivMenuDetailVoiceBack = (ImageView) menuDetailView.findViewById(R.id.iv_menu_detail_voice_back);

        containerMenuDetailPay = (RelativeLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_pay);
        ivAlipayScan = (ImageView) menuDetailView.findViewById(R.id.iv_scan_alipay);
        ivAlipayPay = (ImageView) menuDetailView.findViewById(R.id.iv_pay_alipay);
        ivWeixinScan = (ImageView) menuDetailView.findViewById(R.id.iv_scan_weixin);
        containerMenuDetailPayContent = (LinearLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_pay_content);
        containerMenuDetailPayBack = (RelativeLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_pay_back);
        ivMenuDetailPayBack = (ImageView) menuDetailView.findViewById(R.id.iv_menu_detail_pay_back);


        containerMenuDetailApps = (RelativeLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_apps);
        ivMenuDetailBack = (ImageView) menuDetailView.findViewById(R.id.iv_menu_detail_back);

        containerMenuDetailAppsBack = (RelativeLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_app_back);
        ivMenuDetailAppBack = (ImageView) menuDetailView.findViewById(R.id.iv_menu_detail_app_back);
        containerMenuDetailAppsContent = (LinearLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_app_content);

        containerMenuDetailAppsTop = (LinearLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_app_top);
        containerMenuDetailAppsBottom = (LinearLayout) menuDetailView.findViewById(R.id.container_ball_menu_detail_app_bottom);


        windowManager.addView(touchView, mParams);
    }

    /**
     * 设置触摸块UI
     */
    private void initTouchUI() {
        //设置宽高
        touchWidth = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_WIDTH, DEFAULT_TOUCH_WIDTH);
        touchHeight = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_HEIGHT, DEFAULT_TOUCH_HEIGHT);
        vibrateLevel = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_VIBRATE_LEVEL, DEFAULT_VIBRATE_LEVEL);

        topDrawable = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_TOP_DRAWABLE, R.drawable.shape_react_corners_top);
        midDrawable = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_MID_DRAWABLE, R.drawable.shape_react_corners_mid);
        bottomDrawable = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_BOTTOM_DRAWABLE, R.drawable.shape_react_corners_bottom);

        topColor = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_TOP_COLOR, R.color.colorRecent);
        midColor = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_MID_COLOR, R.color.colorHome);
        bottomColor = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_BOTTOM_COLOR, R.color.colorBack);

        colorAlpha = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_COLOR_ALPHA, DEFAULT_ALPHA);

        int theme = SpUtils.getInt(getApplicationContext(), Configs.KEY_TOUCH_UI_THEME, DEFAULT_THEME);

        if (theme == Configs.TOUCH_UI_THEME_0) {
            topDrawable = R.drawable.shape_react_corners_top;
            midDrawable = R.drawable.shape_react_corners_mid;
            bottomDrawable = R.drawable.shape_react_corners_bottom;
        } else if (theme == Configs.TOUCH_UI_THEME_1) {
            topDrawable = R.drawable.shape_react_top;
            midDrawable = R.drawable.shape_react_mid;
            bottomDrawable = R.drawable.shape_react_bottom;
        }

        ViewGroup.LayoutParams containerLp = llTouchContainer.getLayoutParams();
        containerLp.width = dp2px(touchWidth);
        containerLp.height = dp2px(touchHeight);
        llTouchContainer.setLayoutParams(containerLp);

        ivTouchTop.setImageResource(topDrawable);
        ivTouchMid.setImageResource(midDrawable);
        ivTouchBottom.setImageResource(bottomDrawable);


        setImageViewDrawableColor(ivTouchTop, topDrawable, topColor, colorAlpha);
        setImageViewDrawableColor(ivTouchMid, midDrawable, midColor, colorAlpha);
        setImageViewDrawableColor(ivTouchBottom, bottomDrawable, bottomColor, colorAlpha);

        initEvent();

        windowManager.updateViewLayout(touchView, mParams);
    }

    /**
     * 设置自定义颜色的drawable
     *
     * @param iv
     * @param drawableRes
     * @param color
     */
    private void setImageViewDrawableColor(ImageView iv, @DrawableRes int drawableRes, int color, int alpha) {
        GradientDrawable drawable = (GradientDrawable) getResources().getDrawable(drawableRes, getTheme());
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        int argb = Color.argb(alpha, red, green, blue);

        drawable.setColor(argb);
        iv.setImageDrawable(drawable);
    }

    /**
     * 设置事件
     */
    private void initEvent() {
        setTouchBarEvent();
        initMenuEvent();
        initMenuDetailVoiceEvent();
        initMenuDetailPayEvent();
        initMenuDetailAppEvent();
    }

    /**
     * 根据横竖屏幕切换悬浮球对应的位置
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //横屏
                // 1.获取当前的位置
                if (isMenuDetailShow) {
                    mMenuDetailParams.y = mMenuDetailParams.y * screenWidth / screenHeight;
                    windowManager.updateViewLayout(menuDetailView, mMenuDetailParams);
                } else if (isMenuShow) {
                    mMenuParams.y = mMenuParams.y * screenWidth / screenHeight;
                    windowManager.updateViewLayout(menuView, mMenuParams);
                } else {


                    mParams.y = mParams.y * screenWidth / screenHeight;
                    windowManager.updateViewLayout(touchView, mParams);
                }

            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                //竖屏
                if (isMenuDetailShow) {
                    mMenuDetailParams.y = mMenuDetailParams.y * screenHeight / screenWidth;
                    windowManager.updateViewLayout(menuDetailView, mMenuDetailParams);
                } else if (isMenuShow) {
                    mMenuParams.y = mMenuParams.y * screenHeight / screenWidth;
                    windowManager.updateViewLayout(menuView, mMenuParams);
                } else {
                    mParams.y = mParams.y * screenHeight / screenWidth;
                    windowManager.updateViewLayout(touchView, mParams);
                }

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void initMenuEvent() {
        menuContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideMenuContainer(-1, null);
                return false;
            }
        });

        /*
        * 点击切换左右位置
        * */
        ivMenuBall0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //隐藏菜单按钮动画
                hideMenuContainer(-1, new Configs.OnAnimEndListener() {
                    @Override
                    public void onAnimEnd() {
                        //切换左右位置
                        switchTouchPos();
                    }
                });

            }
        });


        ivMenuBall1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //隐藏菜单按钮动画
                hideMenuContainer(1, new Configs.OnAnimEndListener() {
                    @Override
                    public void onAnimEnd() {
                        //显示声音二级菜单
                        showVoiceAdjustView();
                    }
                });

            }
        });

        ivMenuBall2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenuContainer(2, new Configs.OnAnimEndListener() {
                    @Override
                    public void onAnimEnd() {
                        //显示快捷App选择界面
                        showAppsSelectView();
                    }
                });
            }
        });

        ivMenuBall3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenuContainer(3, new Configs.OnAnimEndListener() {
                    @Override
                    public void onAnimEnd() {
                        //选择快捷支付界面
                        showPaySelectView();
                    }
                });
            }
        });

        ivMenuBall4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenuContainer(-1, new Configs.OnAnimEndListener() {
                    @Override
                    public void onAnimEnd() {
                        //显示锁屏界面
                        showLockScreenView();
                    }
                });
            }
        });
    }

    /**
     * 设置悬浮窗事件
     */
    private void setTouchBarEvent() {
        ivTouchTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


        ivTouchMid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        ivTouchBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        ivTouchTop.setOnTouchListener(this);
        ivTouchMid.setOnTouchListener(this);
        ivTouchBottom.setOnTouchListener(this);

        topDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                Log.d(TAG, "onDown() called with: e = [" + e + "]");
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {
                Log.d(TAG, "onShowPress() called with: e = [" + e + "]");

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.d(TAG, "onSingleTapUp() called with: e = [" + e + "]");
                //震动30毫秒
                vibrator.vibrate(vibrateLevel);
                if (isMenuDetailShow) {
                    hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_SLOW, new Configs.OnAnimEndListener() {
                        @Override
                        public void onAnimEnd() {
                            hideMenuDetailContainer();
                        }
                    }, true);
                } else if (isMenuShow) {
                    isMenuShow = false;
                    hideMenuContainer(-1, null);
                } else {
                    enterRecents();
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Log.d(TAG, "onLongPress() called with: e = [" + e + "]");
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e2.getY() - e1.getY() > minTouchSlop && Math.abs(e1.getY() - e2.getY()) / 3 > Math.abs(e1.getX() - e2.getX())) {
                    //下滑
                    if (isMenuDetailShow) {
                        hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_SLOW, new Configs.OnAnimEndListener() {
                            @Override
                            public void onAnimEnd() {
                                hideMenuDetailContainer();
                            }
                        }, true);
                    } else if (isMenuShow) {
                        isMenuShow = false;
                        hideMenuContainer(-1, null);
                    } else {
                        enterNotification();
                    }
                } else if (e1.getY() - e2.getY() > minTouchSlop && Math.abs(e1.getY() - e2.getY()) / 3 > Math.abs(e1.getX() - e2.getX())) {
                    //上滑
                    if (isMenuDetailShow) {
                        hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_SLOW, new Configs.OnAnimEndListener() {
                            @Override
                            public void onAnimEnd() {
                                hideMenuDetailContainer();
                            }
                        }, true);
                    } else if (isMenuShow) {
                        isMenuShow = false;
                        hideMenuContainer(-1, null);
                    } else {
                        jump2LastApp();
                    }
                }
                return false;
            }
        });
        //设置mid按键事件触发
        midDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                //记录move down坐标
                //刷新悬浮条位置
                if (isMenuDetailShow) {
                    hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_SLOW, new Configs.OnAnimEndListener() {
                        @Override
                        public void onAnimEnd() {
                            hideMenuDetailContainer();
                        }
                    }, true);
                } else if (isMenuShow) {
                    isMenuShow = false;
                    hideMenuContainer(-1, null);
                }
                setMoveDownXY(e);
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                //震动30毫秒
                vibrator.vibrate(vibrateLevel);
                if (isMenuDetailShow) {
                    hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_SLOW, new Configs.OnAnimEndListener() {
                        @Override
                        public void onAnimEnd() {
                            hideMenuDetailContainer();
                        }
                    }, true);
                } else if (isMenuShow) {
                    isMenuShow = false;
                    hideMenuContainer(-1, null);
                } else {
                    recentApps(FloatService.getService(), AccessibilityService.GLOBAL_ACTION_HOME);
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

                refreshMovePlace(e2);
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
        //设置bottom按键事件触发
        bottomDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (isMenuDetailShow) {
                    hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_SLOW, new Configs.OnAnimEndListener() {
                        @Override
                        public void onAnimEnd() {
                            hideMenuDetailContainer();
                        }
                    }, true);
                } else if (isMenuShow) {
                    isMenuShow = false;
                    hideMenuContainer(-1, null);
                } else {
                    enterBack();
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1.getY() - e2.getY() > minTouchSlop && Math.abs(e1.getY() - e2.getY()) / 3 > Math.abs(e1.getX() - e2.getX())) {
                    //上滑
                    //锁屏
                    if (isMenuDetailShow) {
                        hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_SLOW, new Configs.OnAnimEndListener() {
                            @Override
                            public void onAnimEnd() {
                                hideMenuDetailContainer();
                            }
                        }, true);
                    } else if (isMenuShow) {
                        isMenuShow = false;
                        hideMenuContainer(-1, null);
                    } else {
                        showMenuContainer();
                    }
                }
                return false;
            }
        });

//        topDetector.setIsLongpressEnabled(false);
//        midDetector.setIsLongpressEnabled(false);
//        bottomDetector.setIsLongpressEnabled(false);
    }


    /**
     * 设置声音设置详情菜单事件
     */
    private void initMenuDetailVoiceEvent() {
        containerMenuDetailVoice.post(new Runnable() {
            @Override
            public void run() {
                containerMenuDetailVoiceBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_SLOW, new Configs.OnAnimEndListener() {
                            @Override
                            public void onAnimEnd() {
                                hideMenuDetailContainer();
                            }
                        }, true);
                    }
                });
            }
        });
        //设置静音模式切换监听
        switchMode.post(new Runnable() {
            @Override
            public void run() {
                switchMode.setChecked(audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT);
                switchMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                            tvAudioMode.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlackBody));
                        } else {
                            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            tvAudioMode.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorWhite));
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, sbSystemAudio.getProgress(), 0);
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, sbMediaAudio.getProgress(), 0);
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, sbAlarmAudio.getProgress(), 0);
                        }
                    }
                });
            }
        });

        //设置系统音量
        sbSystemAudio.post(new Runnable() {
            @Override
            public void run() {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
                int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
                sbSystemAudio.setMax(maxVolume);
                sbSystemAudio.setProgress(curVolume);
                //设置音量控制变化监听
                sbSystemAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        //如果在静音模式下拖动不实际设置音量
                        if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, progress, 0);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            }
        });

        //闹钟音量调节
        sbAlarmAudio.post(new Runnable() {
            @Override
            public void run() {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                sbAlarmAudio.setMax(maxVolume);
                sbAlarmAudio.setProgress(curVolume);
                //设置音量控制变化监听
                sbAlarmAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, progress, 0);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            }
        });

        //媒体音量调节
        sbMediaAudio.post(new Runnable() {
            @Override
            public void run() {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                sbMediaAudio.setMax(maxVolume);
                sbMediaAudio.setProgress(curVolume);
                //设置音量控制变化监听
                sbMediaAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            }
        });
    }

    /**
     * 隐藏二级菜单显示悬浮球
     */
    private void hideMenuDetailContainer() {
        menuDetailView.post(new Runnable() {
            @Override
            public void run() {
                if (isMenuDetailShow) {
                    //移除二级菜单
                    windowManager.removeView(menuDetailView);
                    //加入悬浮球
//                    windowManager.addView(touchView, mParams);
//                    //显示悬浮球加入动画
//                    showTouchBallEnterAnim(ivTouchBall, HIDE_MENU_DETAIL_FAST, 0);
                    isMenuDetailShow = false;
                }
            }
        });
    }

    /**
     * 设置APP点击事件
     */
    private void initMenuDetailAppEvent() {
        containerMenuDetailApps.post(new Runnable() {
            @Override
            public void run() {
                containerMenuDetailAppsBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_SLOW, new Configs.OnAnimEndListener() {
                            @Override
                            public void onAnimEnd() {
                                hideMenuDetailContainer();
                            }
                        }, true);
                    }
                });
            }
        });
        for (int i = 0; i <= 4; i++) {
            ImageView ivApp = (ImageView) containerMenuDetailAppsTop.getChildAt(i);
            String shortCutStr = SpUtils.getString(getApplicationContext(), Configs.KEY_LINEAR_MENU_TOP_APPS_ + i, "");
            final int finalIndex = i;
            if (TextUtils.isEmpty(shortCutStr)) {
                ivApp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startSelectAppActivity(finalIndex, Configs.AppType.APP.getValue());
                    }
                });

            } else {
                final ResolveInfo appInfo = new Gson().fromJson(shortCutStr, ResolveInfo.class);
                if (appInfo != null) {
                    ivApp.setImageDrawable(PackageUtils.getInstance(getApplicationContext()).getShortCutIcon(appInfo));
                    ivApp.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_FAST, new Configs.OnAnimEndListener() {
                                @Override
                                public void onAnimEnd() {
                                    hideMenuDetailContainer();
                                }
                            }, false);
                            PackageUtils.getInstance(getApplicationContext()).startAppActivity(appInfo);
                        }
                    });
                    ivApp.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            startSelectAppActivity(finalIndex, Configs.AppType.APP.getValue());
                            return true;
                        }
                    });
                }
            }
        }

        for (int i = 0; i <= 4; i++) {
            ImageView ivApp = (ImageView) containerMenuDetailAppsBottom.getChildAt(i);
            String shortCutStr = SpUtils.getString(getApplicationContext(), Configs.KEY_LINEAR_MENU_BOTTOM_APPS_ + i, "");
            final int finalIndex = i;
            if (TextUtils.isEmpty(shortCutStr)) {
                ivApp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startSelectAppActivity(finalIndex, Configs.AppType.SHORTCUT.getValue());
                    }
                });
            } else {
                final ResolveInfo appInfo = new Gson().fromJson(shortCutStr, ResolveInfo.class);
                if (appInfo != null) {
                    ivApp.setImageDrawable(PackageUtils.getInstance(getApplicationContext()).getShortCutIcon(appInfo));
                    ivApp.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_FAST, new Configs.OnAnimEndListener() {
                                @Override
                                public void onAnimEnd() {
                                    hideMenuDetailContainer();
                                }
                            }, false);
                            PackageUtils.getInstance(getApplicationContext()).startAppActivity(appInfo);
                        }
                    });
                    ivApp.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            startSelectAppActivity(finalIndex, Configs.AppType.SHORTCUT.getValue());
                            return true;
                        }
                    });
                }
            }
        }
    }

    /**
     * 打开App选择界面
     *
     * @param finalIndex
     * @param value
     */
    private void startSelectAppActivity(int finalIndex, int value) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(getApplicationContext(), AppSelectActivity.class);
        intent.putExtra(Configs.KEY_APP_TYPE, value);
        intent.putExtra(Configs.KEY_BALL_MENU_SELECT_APP_INDEX, finalIndex);
        intent.putExtra(Configs.KEY_TOUCH_TYPE, Configs.TouchType.LINEAR.getValue());
        startActivity(intent);
    }

    /**
     * 设置支付点击事件
     */
    private void initMenuDetailPayEvent() {
        /*
        * 设置支付事件监听
        * */
        containerMenuDetailPay.post(new Runnable() {
            @Override
            public void run() {
                containerMenuDetailPayBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideMenuDetailEnterAnim(menuDetailView, HIDE_MENU_DETAIL_SLOW, new Configs.OnAnimEndListener() {
                            @Override
                            public void onAnimEnd() {
                                hideMenuDetailContainer();
                            }
                        }, false);
                    }
                });
            }
        });
        ivAlipayScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentUtils.toAliPayScan(getApplicationContext());
            }
        });

        ivAlipayPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentUtils.toAliPayCode(getApplicationContext());
            }
        });

        ivWeixinScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentUtils.toWeChatScanDirect(getApplicationContext());
            }
        });


    }


    /**
     * 显示隐藏菜单
     */
    private void showMenuContainer() {
        mMenuParams.x = mParams.x + dp2px(touchWidth + 5);
        mMenuParams.y = mParams.y;
        windowManager.addView(menuView, mMenuParams);
        isMenuShow = true;
        menuView.post(new Runnable() {
            @Override
            public void run() {
                showMenuBallAnim(ivMenuBall0, 0);
                showMenuBallAnim(ivMenuBall1, 1);
                showMenuBallAnim(ivMenuBall2, 2);
                showMenuBallAnim(ivMenuBall3, 3);
                showMenuBallAnim(ivMenuBall4, 4);
            }
        });
    }


    /**
     * 隐藏菜单
     */
    private void hideMenuContainer(final int index, final Configs.OnAnimEndListener onAnimEndListener) {
        menuView.post(new Runnable() {
            @Override
            public void run() {
//                if (index != -1) {
//                    hideMenuBallAnim(ivTouchBall, -1, false, null);
//                }
                try {
                    hideMenuBallAnim(ivMenuBall0, 0, index == 0, null);
                    hideMenuBallAnim(ivMenuBall1, 1, index == 1, null);
                    hideMenuBallAnim(ivMenuBall2, 2, index == 2, null);
                    hideMenuBallAnim(ivMenuBall3, 3, index == 3, null);
                    hideMenuBallAnim(ivMenuBall4, 4, index == 4, onAnimEndListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * 针对不同的位置的菜单按钮进行不同的动画弹出方式
     *
     * @param view
     * @param index
     */
    private void showMenuBallAnim(final View view, final int index) {
        ballMenuAnimSet = new AnimatorSet();
        float[] transXFloat = new float[180];
        float[] transYFloat = new float[180];

        final int count = menuContainer.getChildCount();
        float radius = (float) (index * Math.PI / (count - 1));

        for (int i = 0; i < 180; i++) {
            transXFloat[i] = (float) (Math.sin(radius * i / 180) * menuWidth / 2);
            transYFloat[i] = (float) (menuWidth / 2 - Math.cos(radius * i / 180) * menuWidth / 2);
        }

        transXAnimShow = ObjectAnimator.ofFloat(view, "translationX", transXFloat);
        transYAnimShow = ObjectAnimator.ofFloat(view, "translationY", transYFloat);

        ballMenuAnimSet.play(transXAnimShow).with(transYAnimShow);
        ballMenuAnimSet.setDuration(200);
        ballMenuAnimSet.start();

    }

    /**
     * 根据不同位置的菜单按钮进行动画执行
     *
     * @param view
     * @param index
     */
    private void hideMenuBallAnim(final View view, int index, boolean isSelected, final Configs.OnAnimEndListener onAnimEndListener) throws Exception {

        ballMenuAnimSet = new AnimatorSet();
        float transXFrom = 0;
        float transYFrom = 0;
        float transXTo = 0;
        float transYTo = 0;

        float scaleXFrom = 0;
        float scaleYFrom = 0;
        float scaleXTo = 0;
        float scaleYTo = 0;
        float alphFrom = 0;
        float alphTo = 0;

        if (isSelected) {
            scaleXFrom = 1f;
            scaleYFrom = 1f;
            scaleXTo = 1.5f;
            scaleYTo = 1.5f;
        } else {
            scaleXFrom = 1f;
            scaleYFrom = 1f;
            scaleXTo = 0f;
            scaleYTo = 0f;
        }
        alphFrom = 1f;
        alphTo = 0f;


        menuBallScaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", scaleXFrom, scaleXTo);
        menuBallScaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", scaleYFrom, scaleYTo);
        menuBallAlphAnim = ObjectAnimator.ofFloat(view, "alpha", alphFrom, alphTo);

        final int count = menuContainer.getChildCount();

        transXFrom = 0;
        transYFrom = transYFrom + index * menuHeight / count;
        transXTo = 0;
        transYTo = 0;


        final int curIndex = index;
        ballMenuAnimSet.play(menuBallScaleXAnim).with(menuBallScaleYAnim).with(menuBallAlphAnim);
        ballMenuAnimSet.setDuration(200);
        final float finalTransXFrom = transXFrom;
        final float finalTransYFrom = transYFrom;
        final float finalTransXTo = transXTo;
        final float finalTransYTo = transYTo;
        final float finalScaleXFrom = scaleXFrom;
        final float finalScaleYFrom = scaleYFrom;
        final float finalScaleXTo = scaleXTo;
        final float finalScaleYTo = scaleYTo;
        final float finalAlphTo = alphTo;
        final float finalAlphFrom = alphFrom;
        ballMenuAnimSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                ObjectAnimator.ofFloat(view, "scaleX", finalScaleXTo, finalScaleXFrom).start();
                ObjectAnimator.ofFloat(view, "scaleY", finalScaleYTo, finalScaleYFrom).start();
                ObjectAnimator.ofFloat(view, "alpha", finalAlphTo, finalAlphFrom).start();

                if (curIndex != -1) {
                    ObjectAnimator.ofFloat(view, "translationX", finalTransXFrom, finalTransXTo).start();
                    ObjectAnimator.ofFloat(view, "translationY", finalTransYFrom, finalTransYTo).start();
                    if (curIndex == (count - 1)) {
                        windowManager.removeView(menuView);
                        isMenuShow = false;
                    }
                } else {
                    windowManager.removeView(touchView);
                    isMenuShow = false;
                }
                if (onAnimEndListener != null) {
                    onAnimEndListener.onAnimEnd();
                }

            }
        });
        ballMenuAnimSet.start();

    }


    private void refreshMovePlace(MotionEvent e2) {
        dx = e2.getRawX() - lastX;
        dy = e2.getRawY() - lastY;
        mParams.y += dy;
        windowManager.updateViewLayout(touchView, mParams);
        lastX = e2.getRawX();
        lastY = e2.getRawY();
    }

    private void setMoveDownXY(MotionEvent e) {
        lastY = e.getRawY();
        lastX = e.getRawX();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        llTouchContainer.post(new Runnable() {
            @Override
            public void run() {
                initTouchUI();
            }
        });
        windowManager.updateViewLayout(touchView, mParams);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 模拟全局按键
     *
     * @param service
     */
    public static void recentApps(AccessibilityService service, int action) {
        if (Build.VERSION.SDK_INT < 16) {
            Toast.makeText(service, "Android 4.1及以上系统才支持此功能，请升级后重试", Toast.LENGTH_SHORT).show();
        } else {
            service.performGlobalAction(action);
        }
    }

    private void showVoiceAdjustView() {
        if (!isMenuDetailShow) {
            //显示声音二级菜单
            Log.i(TAG, "onAnimEnd: 显示声音二级菜单");
            showMenuDetailVoice();
        }
    }

    private void showAppsSelectView() {
        if (!isMenuDetailShow) {
            //显示快捷APP二级菜单
            Log.i(TAG, "onAnimEnd: 显示快捷APP二级菜单");
            showMenuDetailApp();
        }
    }

    private void showPaySelectView() {
        if (!isMenuDetailShow) {
            //显示快捷支付二级菜单
            Log.i(TAG, "onAnimEnd: 显示快捷支付二级菜单");
            showMenuDetailPay();
        }
    }

    /**
     * 显示详细菜单-声音设置
     */
    private void showMenuDetailVoice() {
        mMenuDetailParams.x = mParams.x + dp2px(touchWidth);
        mMenuDetailParams.y = mParams.y + dp2px((touchHeight - 200) / 2);

        windowManager.addView(menuDetailView, mMenuDetailParams);
        menuDetailView.post(new Runnable() {
            @Override
            public void run() {
                //显示二级菜单
                containerMenuDetailVoice.setVisibility(View.VISIBLE);
                containerMenuDetailApps.setVisibility(View.GONE);
                containerMenuDetailPay.setVisibility(View.GONE);
                if (direction == Configs.Position.LEFT.getValue()) {
                    setMenuBallDetailAlignStartLayoutParams(containerMenuDetailVoiceContent);
                    setMenuBallDetailAlignEndLayoutParams(containerMenuDetailVoiceBack);
                    ivMenuDetailVoiceBack.setImageResource(R.drawable.ic_arrow_left);
                } else if (direction == Configs.Position.RIGHT.getValue()) {
                    setMenuBallDetailAlignEndLayoutParams(containerMenuDetailVoiceContent);
                    setMenuBallDetailAlignStartLayoutParams(containerMenuDetailVoiceBack);
                    ivMenuDetailVoiceBack.setImageResource(R.drawable.ic_arrwo_right);

                }
                enterMenuDetailAnim(menuDetailView);
                isMenuDetailShow = true;
            }
        });
    }

    /**
     * 显示详细菜单-App设置
     */
    private void showMenuDetailApp() {
        mMenuDetailParams.x = mParams.x + dp2px(touchWidth);
        mMenuDetailParams.y = mParams.y + dp2px((touchHeight - 120) / 2);

        windowManager.addView(menuDetailView, mMenuDetailParams);
        containerMenuDetailApps.post(new Runnable() {
            @Override
            public void run() {
                //显示二级菜单
                containerMenuDetailVoice.setVisibility(View.GONE);
                containerMenuDetailApps.setVisibility(View.VISIBLE);
                containerMenuDetailPay.setVisibility(View.GONE);
                if (direction == Configs.Position.LEFT.getValue()) {
                    setMenuBallDetailAlignStartLayoutParams(containerMenuDetailAppsContent);
                    setMenuBallDetailAlignEndLayoutParams(containerMenuDetailAppsBack);
                    ivMenuDetailAppBack.setImageResource(R.drawable.ic_arrow_left);

                } else if (direction == Configs.Position.RIGHT.getValue()) {
                    setMenuBallDetailAlignEndLayoutParams(containerMenuDetailAppsContent);
                    setMenuBallDetailAlignStartLayoutParams(containerMenuDetailAppsBack);
                    ivMenuDetailAppBack.setImageResource(R.drawable.ic_arrwo_right);

                }
                enterMenuDetailAnim(menuDetailView);
                isMenuDetailShow = true;
            }
        });
    }

    /**
     * 显示详细菜单-支付设置
     */
    private void showMenuDetailPay() {
        mMenuDetailParams.x = mParams.x + dp2px(touchWidth);
        mMenuDetailParams.y = mParams.y + dp2px((touchHeight - 200) / 2);

        windowManager.addView(menuDetailView, mMenuDetailParams);
        containerMenuDetailPay.post(new Runnable() {
            @Override
            public void run() {
                //显示二级菜单
                containerMenuDetailVoice.setVisibility(View.GONE);
                containerMenuDetailApps.setVisibility(View.GONE);
                containerMenuDetailPay.setVisibility(View.VISIBLE);
                if (direction == Configs.Position.LEFT.getValue()) {
                    setMenuBallDetailAlignStartLayoutParams(containerMenuDetailPayContent);
                    setMenuBallDetailAlignEndLayoutParams(containerMenuDetailPayBack);
                    ivMenuDetailPayBack.setImageResource(R.drawable.ic_arrow_left);
                } else if (direction == Configs.Position.RIGHT.getValue()) {
                    setMenuBallDetailAlignEndLayoutParams(containerMenuDetailPayContent);
                    setMenuBallDetailAlignStartLayoutParams(containerMenuDetailPayBack);
                    ivMenuDetailPayBack.setImageResource(R.drawable.ic_arrwo_right);

                }
                enterMenuDetailAnim(menuDetailView);
                isMenuDetailShow = true;
            }
        });
    }

    /**
     * 二级菜单的进入动画
     *
     * @param containerMenuDetail
     */
    private void enterMenuDetailAnim(View containerMenuDetail) {
        if (direction == Configs.Position.LEFT.getValue()) {
            ObjectAnimator.ofFloat(containerMenuDetail, "translationX", dp2px(-300f), 0).start();
        } else if (direction == Configs.Position.RIGHT.getValue()) {
            ObjectAnimator.ofFloat(containerMenuDetail, "translationX", dp2px(300f), 0).start();
        }
    }

    /**
     * 设置悬浮窗在左边的时候的Detail布局
     *
     * @param v
     */
    private void setMenuBallDetailAlignStartLayoutParams(View v) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
        lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
        lp.addRule(RelativeLayout.ALIGN_PARENT_START);
        v.setLayoutParams(lp);
    }

    /**
     * 设置悬浮窗在右边的时候的Detail布局
     *
     * @param v
     */
    private void setMenuBallDetailAlignEndLayoutParams(View v) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
        lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
        lp.addRule(RelativeLayout.ALIGN_PARENT_END);
        v.setLayoutParams(lp);
    }

    /**
     * 二级菜单的退出动画
     *
     * @param containerMenuDetail
     */
    private void hideMenuDetailEnterAnim(View containerMenuDetail, int duration, final Configs.OnAnimEndListener onAnimEndListener, boolean isAppsMenu) {
        int transFromX = 0;
        int transToX = dp2px(-200);
        if (direction == Configs.Position.LEFT.getValue()) {
            transFromX = 0;
            transToX = dp2px(-200);
            if (isAppsMenu) {
                transToX = dp2px(-300f);
            }
        } else if (direction == Configs.Position.RIGHT.getValue()) {
            transFromX = 0;
            transToX = dp2px(200f);
            if (isAppsMenu) {
                transToX = dp2px(300f);
            }
        }

        hideMenuDetailAnim = ObjectAnimator.ofFloat(containerMenuDetail, "translationX", transFromX, transToX);
        hideMenuDetailAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (onAnimEndListener != null) {
                    onAnimEndListener.onAnimEnd();
                }
            }
        });
        hideMenuDetailAnim.setDuration(duration);
        hideMenuDetailAnim.start();
    }

    private void switchTouchPos() {
        if (!isMenuDetailShow) {
            //切换位置
            Log.i(TAG, "onAnimEnd: 切换位置");
            //切换容器位置
            if (direction == Configs.Position.LEFT.getValue()) {
                direction = Configs.Position.RIGHT.getValue();
                SpUtils.saveInt(getApplicationContext(), Configs.KEY_TOUCH_UI_DIRECTION, direction);
                mParams.x = screenWidth;
                mMenuParams.x = screenWidth;
                mMenuDetailParams.x = screenWidth;
            } else if (direction == Configs.Position.RIGHT.getValue()) {
                direction = Configs.Position.LEFT.getValue();
                SpUtils.saveInt(getApplicationContext(), Configs.KEY_TOUCH_UI_DIRECTION, direction);
                mParams.x = 0;
                mMenuParams.x = 0;
                mMenuDetailParams.x = 0;
            }

            //切换菜单按钮位置布局
            if (direction == Configs.Position.LEFT.getValue()) {
                setMenuBallLeftLayoutParams(ivMenuBall0);
                setMenuBallLeftLayoutParams(ivMenuBall1);
                setMenuBallLeftLayoutParams(ivMenuBall2);
                setMenuBallLeftLayoutParams(ivMenuBall3);
                setMenuBallLeftLayoutParams(ivMenuBall4);
            } else if (direction == Configs.Position.RIGHT.getValue()) {
                setMenuBallRightLayoutParams(ivMenuBall0);
                setMenuBallRightLayoutParams(ivMenuBall1);
                setMenuBallRightLayoutParams(ivMenuBall2);
                setMenuBallRightLayoutParams(ivMenuBall3);
                setMenuBallRightLayoutParams(ivMenuBall4);
            }
            //更新布局
            windowManager.updateViewLayout(touchView, mParams);
        }
    }


    /**
     * 设置悬浮窗在左边的时候的布局
     *
     * @param v
     */
    private void setMenuBallLeftLayoutParams(View v) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
        lp.addRule(RelativeLayout.CENTER_VERTICAL | RelativeLayout.ALIGN_PARENT_LEFT);
        v.setLayoutParams(lp);
    }

    /**
     * 设置悬浮窗在右边的时候的布局
     *
     * @param v
     */
    private void setMenuBallRightLayoutParams(View v) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
        lp.addRule(RelativeLayout.CENTER_VERTICAL | RelativeLayout.ALIGN_PARENT_RIGHT);

        v.setLayoutParams(lp);
    }

    private void showLockScreenView() {
        if (!isMenuDetailShow) {
            lockScreen();//锁屏
        }
    }


    private int dp2px(float dp) {
        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.1f);
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        if (v.getId() == R.id.iv_touch_top) {
            return topDetector.onTouchEvent(e);
        } else if (v.getId() == R.id.iv_touch_mid) {
            return midDetector.onTouchEvent(e);
        } else if (v.getId() == R.id.iv_touch_bottom) {
            return bottomDetector.onTouchEvent(e);
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            windowManager.removeView(touchView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}