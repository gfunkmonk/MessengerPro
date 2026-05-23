package tn.amin.mpro2.hook.all;

import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import tn.amin.mpro2.constants.OrcaClassNames;
import tn.amin.mpro2.debug.Logger;
import tn.amin.mpro2.hook.BaseHook;
import tn.amin.mpro2.hook.HookId;
import tn.amin.mpro2.hook.listener.HookListenerResult;
import tn.amin.mpro2.orca.OrcaGateway;
import tn.amin.mpro2.orca.datatype.Mention;
import tn.amin.mpro2.orca.datatype.TextMessage;

public class MessageSentHook extends BaseHook {

    public static final String DISPATCH_METHOD =
            "dispatchVIJOOOOOOOOOOOOOOOOOOOOOO";

    public MessageSentHook() {
        super();
    }

    @Override
    public HookId getId() {
        return HookId.MESSAGE_SEND;
    }

    @Override
    protected Set<XC_MethodHook.Unhook> injectInternal(
            OrcaGateway gateway
    ) {

        try {

            final Class<?> mailboxCoreJNI =
                    XposedHelpers.findClassIfExists(
                            OrcaClassNames.MAILBOX_CORE_JNI,
                            gateway.classLoader
                    );

            if (mailboxCoreJNI == null) {

                Logger.error(
                        "MailboxCoreJNI class not found"
                );

                return null;
            }

            return XposedBridge.hookAllMethods(
                    mailboxCoreJNI,
                    DISPATCH_METHOD,

                    wrap(new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(
                                MethodHookParam param
                        ) {

                            try {

                                if (param.args == null)
                                    return;

                                if (param.args.length < 12) {

                                    Logger.error(
                                            "Unexpected send args length: "
                                                    + param.args.length
                                    );

                                    return;
                                }

                                if (!(param.args[5] instanceof String))
                                    return;

                                final Long threadKey =
                                        param.args[2] instanceof Long
                                                ? (Long) param.args[2]
                                                : null;

                                String message =
                                        (String) param.args[5];

                                String rangeStartsString =
                                        safeString(param.args[7]);

                                String rangeEndsString =
                                        safeString(param.args[8]);

                                String threadKeysString =
                                        safeString(param.args[9]);

                                String typesString =
                                        safeString(param.args[10]);

                                String replyMessageId =
                                        safeString(param.args[11]);

                                TextMessage originalMessage =
                                        new TextMessage.Builder(message)

                                                .setMentions(
                                                        Mention.fromDispatchArgs(
                                                                message,
                                                                rangeStartsString,
                                                                rangeEndsString,
                                                                threadKeysString,
                                                                typesString
                                                        )
                                                )

                                                .setReplyMessageId(
                                                        replyMessageId
                                                )

                                                .build();

                                notifyListenersWithResult(listener ->

                                        ((MessageSentListener) listener)
                                                .onMessageSent(
                                                        originalMessage,
                                                        threadKey
                                                )
                                );

                                HookListenerResult<?> result =
                                        getListenersReturnValue();

                                if (result == null)
                                    return;

                                if (result.isConsumed &&
                                        result.value == null) {

                                    param.setResult(null);

                                    return;
                                }

                                if (!(result.value instanceof TextMessage))
                                    return;

                                TextMessage refinedMessage =
                                        (TextMessage) result.value;

                                if (refinedMessage == null)
                                    return;

                                Logger.logObjectRecursive(
                                        refinedMessage
                                );

                                param.args[5] =
                                        refinedMessage.content;

                                param.args[7] =
                                        Mention.joinRangeStarts(
                                                refinedMessage.mentions
                                        );

                                param.args[8] =
                                        Mention.joinRangeEnds(
                                                refinedMessage.mentions
                                        );

                                param.args[9] =
                                        Mention.joinThreadKeys(
                                                refinedMessage.mentions
                                        );

                                param.args[10] =
                                        Mention.joinTypes(
                                                refinedMessage.mentions
                                        );

                                param.args[11] =
                                        refinedMessage.replyMessageId;

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

    private String safeString(Object value) {

        if (value instanceof String) {
            return (String) value;
        }

        return null;
    }

    public interface MessageSentListener {

        HookListenerResult<TextMessage> onMessageSent(
                TextMessage message,
                Long threadKey
        );
    }
}
