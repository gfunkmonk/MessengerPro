package tn.amin.mpro2.hook.all;

import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;

import tn.amin.mpro2.debug.Logger;
import tn.amin.mpro2.hook.BaseHook;
import tn.amin.mpro2.hook.HookId;
import tn.amin.mpro2.hook.HookTime;
import tn.amin.mpro2.hook.helper.OrcaHookHelper;
import tn.amin.mpro2.hook.unobfuscation.OrcaUnobfuscator;
import tn.amin.mpro2.orca.OrcaGateway;

public class MessageReceivedHook extends BaseHook {

    @Override
    public HookId getId() {
        return HookId.MESSAGE_RECEIVE;
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

            Integer apiCode =
                    gateway.unobfuscator.getAPICode(
                            OrcaUnobfuscator.API_NOTIFICATION
                    );

            if (apiCode == null) {

                Logger.error(
                        "MessageReceivedHook API code is null"
                );

                return null;
            }

            return OrcaHookHelper.hookFeature(
                    apiCode,
                    "VO",
                    "Core",
                    gateway.classLoader,

                    wrap(new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {

                            try {

                                if (param.args == null)
                                    return;

                                if (param.args.length < 9) {

                                    Logger.error(
                                            "Unexpected message receive args length: "
                                                    + param.args.length
                                    );

                                    return;
                                }

                                long convThreadKey = 0L;

                                if (param.args[2] instanceof Long) {
                                    convThreadKey =
                                            (Long) param.args[2];
                                }

                                String senderUserKey = null;

                                if (param.args[3] instanceof String) {
                                    senderUserKey =
                                            (String) param.args[3];
                                }

                                String messageId = null;

                                if (param.args[6] instanceof String) {
                                    messageId =
                                            (String) param.args[6];
                                }

                                String message = null;

                                if (param.args[8] instanceof String) {
                                    message =
                                            (String) param.args[8];
                                }

                                final long finalThreadKey =
                                        convThreadKey;

                                final String finalSenderUserKey =
                                        senderUserKey;

                                final String finalMessageId =
                                        messageId;

                                final String finalMessage =
                                        message;

                                notifyListeners(listener -> {

                                    try {

                                        ((MessageReceivedListener) listener)
                                                .onMessageReceived(
                                                        finalMessage,
                                                        finalMessageId,
                                                        finalSenderUserKey,
                                                        finalThreadKey
                                                );

                                    } catch (Throwable listenerError) {

                                        Logger.error(listenerError);
                                    }
                                });

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

    public interface MessageReceivedListener {

        void onMessageReceived(
                String message,
                String messageId,
                String senderUserKey,
                long convThreadKey
        );
    }
}
