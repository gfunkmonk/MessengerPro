package tn.amin.mpro2.hook.helper;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import tn.amin.mpro2.debug.Logger;

public class OrcaHookHelper {

    public static Set<XC_MethodHook.Unhook> hookFeature(
            int featureId,
            String requiredPrefix,
            String category,
            ClassLoader classLoader,
            XC_MethodHook methodHook
    ) {

        HashSet<XC_MethodHook.Unhook> unhooks =
                new HashSet<>();

        try {

            String targetClass =
                    "com.facebook."
                            + category.toLowerCase()
                            + ".mca.Mailbox"
                            + category
                            + "JNI";

            Class<?> cls =
                    XposedHelpers.findClassIfExists(
                            targetClass,
                            classLoader
                    );

            if (cls == null) {

                Logger.error(
                        "Failed finding class: "
                                + targetClass
                );

                return unhooks;
            }

            Method[] methods;

            try {

                methods = cls.getDeclaredMethods();

            } catch (Throwable t) {

                Logger.error(t);

                return unhooks;
            }

            for (Method method : methods) {

                if (method == null)
                    continue;

                try {

                    String methodName = method.getName();

                    if (methodName == null)
                        continue;

                    if (!methodName.startsWith(
                            "dispatch" + requiredPrefix
                    )) {
                        continue;
                    }

                    Logger.info(
                            "Hooking method: "
                                    + methodName
                    );

                    XC_MethodHook.Unhook unhook =
                            XposedBridge.hookMethod(
                                    method,

                                    new XC_MethodHook() {

                                        @Override
                                        protected void beforeHookedMethod(
                                                MethodHookParam param
                                        ) {

                                            try {

                                                if (param.args == null)
                                                    return;

                                                if (param.args.length == 0)
                                                    return;

                                                Logger.verbose(
                                                        Arrays.toString(
                                                                param.args
                                                        )
                                                );

                                                Object arg0 =
                                                        param.args[0];

                                                if (!(arg0 instanceof Integer))
                                                    return;

                                                int currentFeatureId =
                                                        (Integer) arg0;

                                                if (currentFeatureId
                                                        != featureId) {
                                                    return;
                                                }

                                                try {

                                                    Method beforeMethod =
                                                            methodHook.getClass()
                                                                    .getDeclaredMethod(
                                                                            "beforeHookedMethod",
                                                                            MethodHookParam.class
                                                                    );

                                                    beforeMethod.setAccessible(true);

                                                    beforeMethod.invoke(
                                                            methodHook,
                                                            param
                                                    );

                                                } catch (
                                                        NoSuchMethodException ignored
                                                ) {
                                                }

                                            } catch (Throwable t) {

                                                Logger.error(t);
                                            }
                                        }

                                        @Override
                                        protected void afterHookedMethod(
                                                MethodHookParam param
                                        ) {

                                            try {

                                                if (param.args == null)
                                                    return;

                                                if (param.args.length == 0)
                                                    return;

                                                Object arg0 =
                                                        param.args[0];

                                                if (!(arg0 instanceof Integer))
                                                    return;

                                                int currentFeatureId =
                                                        (Integer) arg0;

                                                if (currentFeatureId
                                                        != featureId) {
                                                    return;
                                                }

                                                try {

                                                    Method afterMethod =
                                                            methodHook.getClass()
                                                                    .getDeclaredMethod(
                                                                            "afterHookedMethod",
                                                                            MethodHookParam.class
                                                                    );

                                                    afterMethod.setAccessible(true);

                                                    afterMethod.invoke(
                                                            methodHook,
                                                            param
                                                    );

                                                } catch (
                                                        NoSuchMethodException ignored
                                                ) {
                                                }

                                            } catch (Throwable t) {

                                                Logger.error(t);
                                            }
                                        }
                                    }
                            );

                    if (unhook != null) {
                        unhooks.add(unhook);
                    }

                } catch (Throwable methodError) {

                    Logger.error(methodError);
                }
            }

        } catch (Throwable t) {

            Logger.error(t);
        }

        return unhooks;
    }
}
