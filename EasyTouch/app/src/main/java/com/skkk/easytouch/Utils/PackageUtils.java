package com.skkk.easytouch.Utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import com.skkk.easytouch.Bean.AppInfoBean;
import com.skkk.easytouch.Bean.ShortCutInfoBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建于 2017/12/3
 * 作者 admin
 */
/*
* 
* 描    述：包管理工具
* 作    者：ksheng
* 时    间：2017/12/3$ 19:30$.
*/
public class PackageUtils {
    private static volatile PackageUtils instance;
    private static Context context;
    private Drawable iconDrawable;

    public PackageUtils(Context context) {
        this.context = context;
    }

    public static PackageUtils getInstance(Context context) {
        if (instance==null){
            synchronized (PackageUtils.class){
                if (instance==null){
                    return new PackageUtils(context);
                }
            }
        }
        return instance;
    }

    /**
     * 获取所有的快捷方式的入口信息
     * @return
     */
    public static List<ShortCutInfoBean> getAllResolveInfo(){
        //获取到所有快捷方式
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> shortcuts = context.getPackageManager().queryIntentActivities(
                shortcutsIntent, 0);
        List<ShortCutInfoBean> infoList=new ArrayList<>();
        ShortCutInfoBean infoBean;
        for (int i = 0; i < shortcuts.size(); i++) {
            infoBean=new ShortCutInfoBean();
            //启动方式如下
            ActivityInfo ai=shortcuts.get(i).activityInfo;
            infoBean.setPkgName(ai.applicationInfo.packageName);
            infoBean.setActivityName(ai.name);
            infoBean.setFlag(ai.flags);
            infoBean.setShortCutName(ai.applicationInfo.name);
            infoBean.setIcon(getShortCutIcon(shortcuts.get(i)));
            infoList.add(infoBean);
        }
        return infoList;
    }


    /**
     * 获取指定快捷方式的图标
     * @param resolveInfo
     * @return
     */
    public static Drawable getShortCutIcon(ResolveInfo resolveInfo){
        //获取对应icon方法如下
        //启动方式如下
        Drawable iconDrawable=null;
        PackageManager pm = context.getPackageManager();
        ActivityInfo ai=resolveInfo.activityInfo;

        String pkgName=ai.applicationInfo.packageName;
        String activityName=ai.name;
        int flag=ai.flags;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(pkgName,activityName);
        intent.addFlags(flag);
        try {
            iconDrawable= pm.getActivityIcon(intent);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return iconDrawable;
    }

    /**
     * 启动指定的curShort
     * @param shortCutInfoBean
     */
    public void startShortCutActivity(ShortCutInfoBean shortCutInfoBean){
        //启动方式如下
        String pkgName=shortCutInfoBean.getPkgName();
        String activityName=shortCutInfoBean.getActivityName();
        int flag=shortCutInfoBean.getFlag();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(pkgName,activityName);
        intent.addFlags(flag);
        context.startActivity(intent);
    }

    /**
     * 打开指定APP
     * @param appInfoBean
     */
    public void startAppActivity(AppInfoBean appInfoBean){
        //该应用的包名和主Activity
        String pkg = appInfoBean.getPkgName();
        String cls = appInfoBean.getClassName();

        ComponentName componet = new ComponentName(pkg, cls);

        Intent intent = new Intent();
        intent.setComponent(componet);
        context.startActivity(intent);
    }

    /**
     * 获取系统中所有应用信息， 并将应用软件信息保存到list列表中。
     *
     */
    public List<AppInfoBean> getAllApps() {
        PackageManager packageManager=context.getPackageManager();
        List<AppInfoBean> list = new ArrayList<AppInfoBean>();
        AppInfoBean myAppInfo;

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : resolveInfos) {
            myAppInfo = new AppInfoBean();
            //该应用的包名和主Activity
            // 拿到包名
            String pkg = info.activityInfo.packageName;
            // 拿到运行的Cls名
            String cls = info.activityInfo.name;
            // 拿到应用程序的信息
            ApplicationInfo appInfo = info.activityInfo.applicationInfo;
            // 拿到应用程序的图标
            Drawable icon = getAppIcon(info);
            // 拿到应用名
            String appName=info.loadLabel(packageManager).toString();

            myAppInfo.setPkgName(pkg);
            myAppInfo.setAppName(appName);
            myAppInfo.setIcon(icon);
            myAppInfo.setClassName(cls);
            list.add(myAppInfo);
        }
        return list;
    }

    public Drawable getAppIcon(ResolveInfo resolveInfo){
        Drawable icon=null;
        PackageManager pm = context.getPackageManager();
        ApplicationInfo appInfo = resolveInfo.activityInfo.applicationInfo;
        // 拿到应用程序的图标
        icon = appInfo.loadIcon(pm);
        return icon;
    }






}
