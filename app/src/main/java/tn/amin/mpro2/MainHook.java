package tn.amin.mpro2;

import android.content.res.Resources;
import android.content.res.XModuleResources;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import tn.amin.mpro2.constants.OrcaInfo;
import tn.amin.mpro2.debug.OrcaExplorer;
import tn.amin.mpro2.orca.OrcaGateway;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private String modulePath;

    @Override
    public void initZygote(StartupParam startupParam) {
        modulePath = startupParam.modulePath;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        if (lpparam == null || lpparam.packageName == null)
            return;

        if (!OrcaInfo.ORCA_PACKAGE_NAME.equals(lpparam.packageName))
            return;

        try {
            orcaHook(lpparam);
        } catch (Throwable t) {
            XposedBridge.log("MessengerPro fatal hook error:");
            XposedBridge.log(t);
        }
    }

    /**
     * Initialize Messenger gateway and load all hooks/features.
     */
    private void orcaHook(XC_LoadPackage.LoadPackageParam lpparam) {

        try {

            Resources moduleRes = getResources();

            OrcaGateway gateway = new OrcaGateway(
                    lpparam.appInfo.sourceDir,
                    lpparam.classLoader,
                    moduleRes
            );

            MProPatcher featuresBox = new MProPatcher(gateway);
            featuresBox.init();

            OrcaExplorer.exploreEarly(lpparam.classLoader);

            XposedBridge.log("MessengerPro loaded successfully");

        } catch (Throwable t) {
            XposedBridge.log("MessengerPro initialization failed:");
            XposedBridge.log(t);
        }
    }

    private Resources getResources() {
        return XModuleResources.createInstance(modulePath, null);
    }
}
