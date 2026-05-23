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

public class ConversationEnterHook extends BaseHook {

    @Override
    public HookId getId() {
        return HookId.CONVERSATION_ENTER;
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
                            OrcaUnobfuscator.API_CONVERSATION_ENTER
                    );

            if (apiCode == null) {

                Logger.error(
                        "Conversation enter API code is null"
                );

                return null;
            }

            return OrcaHookHelper.hookFeature(

                    apiCode,

                    "O",

                    "Orca",

                    gateway.classLoader,

                    wrap(new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {

                            try {

                                if (param.args == null)
                                    return;

                                if (param.args.length < 7) {

                                    Logger.error(
                                            "Unexpected conversation enter args length: "
                                                    + param.args.length
                                    );

                                    return;
                                }

                                Object arg =
                                        param.args[6];

                                if (!(arg instanceof String))
                                    return;

                                long threadKey;

                                try {

                                    threadKey =
                                            Long.parseLong(
                                                    (String) arg
                                            );

                                } catch (Throwable parseError) {

                                    Logger.error(parseError);

                                    return;
                                }

                                final long finalThreadKey =
                                        threadKey;

                                notifyListeners(listener -> {

                                    try {

                                        ((ConversationEnterListener) listener)
                                                .onConversationEnter(
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

    public interface ConversationEnterListener {

        void onConversationEnter(Long threadKey);
    }
}
