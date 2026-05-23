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

public class ConversationLeaveHook extends BaseHook {

    @Override
    public HookId getId() {
        return HookId.CONVERSATION_LEAVE;
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
                            OrcaUnobfuscator.API_CONVERSATION_LEAVE
                    );

            if (apiCode == null) {

                Logger.error(
                        "Conversation leave API code is null"
                );

                return null;
            }

            return OrcaHookHelper.hookFeature(

                    apiCode,

                    "V",

                    "Orca",

                    gateway.classLoader,

                    wrap(new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {

                            try {

                                notifyListeners(listener -> {

                                    try {

                                        ((ConversationLeaveListener) listener)
                                                .onConversationLeave();

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

    public interface ConversationLeaveListener {

        void onConversationLeave();
    }
}
