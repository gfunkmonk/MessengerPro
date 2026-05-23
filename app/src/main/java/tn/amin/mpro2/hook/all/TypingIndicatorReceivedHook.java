package tn.amin.mpro2.hook.all;

import android.os.Parcelable;

import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;

import tn.amin.mpro2.debug.Logger;
import tn.amin.mpro2.hook.BaseHook;
import tn.amin.mpro2.hook.HookId;
import tn.amin.mpro2.hook.helper.ContextHookHelper;
import tn.amin.mpro2.orca.OrcaGateway;
import tn.amin.mpro2.orca.wrapper.ThreadKeyWrapper;
import tn.amin.mpro2.orca.wrapper.UserKeyWrapper;

public class TypingIndicatorReceivedHook extends BaseHook {

    private static final String ACTION_TYPING_INDICATOR =
            "com.facebook.presence.ACTION_OTHER_USER_TYPING_CHANGED";

    @Override
    public HookId getId() {
        return HookId.TYPING_INDICATOR_RECEIVE;
    }

    @Override
    protected Set<XC_MethodHook.Unhook> injectInternal(
            OrcaGateway gateway
    ) {

        try {

            return ContextHookHelper.interceptBroadcast(

                    ACTION_TYPING_INDICATOR,

                    wrap(intent -> {

                        try {

                            if (intent == null)
                                return false;

                            Long userKey = -1L;
                            Long threadKey = -1L;

                            boolean isTyping = false;

                            int state =
                                    intent.getIntExtra(
                                            "extra_new_state",
                                            -1
                                    );

                            Parcelable userKeyObject =
                                    intent.getParcelableExtra(
                                            "extra_user_key"
                                    );

                            Parcelable threadKeyObject =
                                    intent.getParcelableExtra(
                                            "extra_thread_key"
                                    );

                            if (userKeyObject != null) {

                                try {

                                    UserKeyWrapper userKeyWrapper =
                                            new UserKeyWrapper(
                                                    userKeyObject
                                            );

                                    userKey =
                                            userKeyWrapper.getUserKeyLong();

                                } catch (Throwable t) {

                                    Logger.error(t);
                                }
                            }

                            if (threadKeyObject != null) {

                                try {

                                    ThreadKeyWrapper threadKeyWrapper =
                                            new ThreadKeyWrapper(
                                                    threadKeyObject
                                            );

                                    if ("GROUP".equals(
                                            threadKeyWrapper.getType()
                                    )) {

                                        threadKey =
                                                threadKeyWrapper
                                                        .getGroupThreadKey();
                                    }

                                } catch (Throwable t) {

                                    Logger.error(t);
                                }
                            }

                            if (threadKey == -1L) {
                                threadKey = userKey;
                            }

                            isTyping = state == 1;

                            final Long finalUserKey =
                                    userKey;

                            final Long finalThreadKey =
                                    threadKey;

                            final boolean finalIsTyping =
                                    isTyping;

                            notifyListeners(listener -> {

                                try {

                                    ((TypingIndicatorReceivedListener) listener)
                                            .onTypingIndicatorReceived(
                                                    finalUserKey,
                                                    finalThreadKey,
                                                    finalIsTyping
                                            );

                                } catch (Throwable listenerError) {

                                    Logger.error(listenerError);
                                }
                            });

                        } catch (Throwable t) {

                            Logger.error(t);
                        }

                        return false;
                    }),

                    this::wrapIgnoreWorking

            );

        } catch (Throwable t) {

            Logger.error(t);

            return null;
        }
    }

    public interface TypingIndicatorReceivedListener {

        void onTypingIndicatorReceived(
                long userKey,
                long threadKey,
                boolean isTyping
        );
    }
}
