package tn.amin.mpro2.hook.all;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

import tn.amin.mpro2.debug.Logger;
import tn.amin.mpro2.hook.BaseHook;
import tn.amin.mpro2.hook.HookId;
import tn.amin.mpro2.hook.HookTime;
import tn.amin.mpro2.hook.listener.HookListenerResult;
import tn.amin.mpro2.hook.unobfuscation.OrcaUnobfuscator;
import tn.amin.mpro2.orca.OrcaGateway;

public class TypingIndicatorSentHook extends BaseHook {

    @Override
    public HookId getId() {
        return HookId.TYPING_INDICATOR_SEND;
    }

    @Override
    public HookTime getHookTime() {
        return HookTime.AFTER_DEOBFUSCATION;
    }

    @Override
    protected Set<XC_MethodHook.Unhook> injectInternal(
            OrcaGateway gateway
    ) {

        try {

            Class<?> typingIndicatorDispatcher =
                    gateway.unobfuscator.getClass(
                            OrcaUnobfuscator.CLASS_TYPING_INDICATOR_DISPATCHER
                    );

            if (typingIndicatorDispatcher == null) {

                Logger.error(
                        OrcaUnobfuscator.CLASS_TYPING_INDICATOR_DISPATCHER
                                + " is null"
                );

                return null;
            }

            Method[] methods;

            try {

                methods =
                        typingIndicatorDispatcher.getDeclaredMethods();

            } catch (Throwable t) {

                Logger.error(t);

                return null;
            }

            if (methods == null || methods.length == 0) {

                Logger.error(
                        "No typing indicator dispatcher methods found"
                );

                return null;
            }

            Method dispatchTypingIndicator =
                    null;

            for (Method method : methods) {

                if (method == null)
                    continue;

                try {

                    dispatchTypingIndicator = method;

                    break;

                } catch (Throwable ignored) {
                }
            }

            if (dispatchTypingIndicator == null) {

                Logger.error(
                        "dispatchTypingIndicator method is null"
                );

                return null;
            }

            Method finalDispatchTypingIndicator =
                    dispatchTypingIndicator;

            return Collections.singleton(

                    XposedBridge.hookMethod(

                            finalDispatchTypingIndicator,

                            wrap(new XC_MethodHook() {

                                @Override
                                protected void beforeHookedMethod(
                                        MethodHookParam param
                                ) {

                                    try {

                                        notifyListenersWithResult(
                                                listener ->

                                                        ((TypingIndicatorSentListener) listener)
                                                                .onTypingIndicatorSent()
                                        );

                                        HookListenerResult<?> result =
                                                getListenersReturnValue();

                                        boolean allowTypingIndicator =
                                                true;

                                        if (result != null &&
                                                result.isConsumed) {

                                            if (result.value instanceof Boolean) {

                                                allowTypingIndicator =
                                                        (Boolean) result.value;
                                            }
                                        }

                                        if (!allowTypingIndicator) {

                                            param.setResult(null);
                                        }

                                    } catch (Throwable t) {

                                        Logger.error(t);
                                    }
                                }
                            })
                    )
            );

        } catch (Throwable t) {

            Logger.error(t);

            return null;
        }
    }

    public interface TypingIndicatorSentListener {

        HookListenerResult<Boolean> onTypingIndicatorSent();
    }
}
