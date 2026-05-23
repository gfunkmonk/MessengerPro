package tn.amin.mpro2.hook.all;

import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import tn.amin.mpro2.constants.OrcaClassNames;
import tn.amin.mpro2.debug.Logger;
import tn.amin.mpro2.hook.BaseHook;
import tn.amin.mpro2.hook.HookId;
import tn.amin.mpro2.hook.HookTime;
import tn.amin.mpro2.hook.listener.HookListenerResult;
import tn.amin.mpro2.hook.unobfuscation.OrcaUnobfuscator;
import tn.amin.mpro2.orca.OrcaGateway;

public class SeenIndicatorHook extends BaseHook {

    @Override
    public HookId getId() {
        return HookId.SEEN_INDICATOR_SEND;
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

            final Class<?> mailboxCoreJNI =
                    XposedHelpers.findClassIfExists(
                            OrcaClassNames.MAILBOX_SDK_JNI,
                            gateway.classLoader
                    );

            if (mailboxCoreJNI == null) {

                Logger.error(
                        "Mailbox SDK JNI class not found"
                );

                return null;
            }

            return XposedBridge.hookAllMethods(
                    mailboxCoreJNI,
                    "dispatchVJOOOO",

                    wrap(new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {

                            try {

                                if (param.args == null)
                                    return;

                                if (param.args.length < 4)
                                    return;

                                Integer apiCode =
                                        gateway.unobfuscator
                                                .getAPICode(
                                                        OrcaUnobfuscator.API_MESSAGE_SEEN
                                                );

                                boolean insideSeenDispatch =
                                        false;

                                if (apiCode != null &&
                                        apiCode >= 0) {

                                    Object arg0 =
                                            param.args[0];

                                    if (arg0 instanceof Integer) {

                                        insideSeenDispatch =
                                                ((Integer) arg0)
                                                        .equals(apiCode);
                                    }
                                }

                                else {

                                    Object arg2 =
                                            param.args[2];

                                    Object arg3 =
                                            param.args[3];

                                    if (arg2 != null &&
                                            arg3 != null) {

                                        String arg2Name =
                                                arg2.getClass().getName();

                                        String arg3Name =
                                                arg3.getClass().getName();

                                        insideSeenDispatch =
                                                OrcaClassNames.MAILBOX.equals(
                                                        arg2Name
                                                )

                                                &&

                                                Long.class.getName().equals(
                                                        arg3Name
                                                );
                                    }
                                }

                                if (!insideSeenDispatch)
                                    return;

                                Logger.verbose(
                                        "Inside seen indicator dispatch"
                                );

                                notifyListenersWithResult(
                                        listener ->

                                                ((SeenIndicatorListener) listener)
                                                        .onSeenIndicator()
                                );

                                HookListenerResult<?> result =
                                        getListenersReturnValue();

                                boolean allowSeen =
                                        true;

                                if (result != null &&
                                        result.isConsumed) {

                                    if (result.value instanceof Boolean) {

                                        allowSeen =
                                                (Boolean) result.value;
                                    }
                                }

                                Logger.verbose(
                                        "AllowSeen: "
                                                + allowSeen
                                );

                                if (!allowSeen) {
                                    param.setResult(null);
                                }

                            } catch (Throwable t) {

                                Logger.error(t);
                            }
                        }
                    })
            );

        } catch (Throwable t) {

            Logger.error(t);

            return null;
        }
    }

    public interface SeenIndicatorListener {

        HookListenerResult<Boolean> onSeenIndicator();
    }
}
