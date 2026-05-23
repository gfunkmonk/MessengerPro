package tn.amin.mpro2.hook;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import tn.amin.mpro2.debug.Logger;
import tn.amin.mpro2.hook.state.HookState;
import tn.amin.mpro2.hook.state.HookStateTracker;
import tn.amin.mpro2.orca.OrcaGateway;

public class HookManager {

    public HookManager() {
    }

    private final Map<BaseHook, HookStateTracker> mHookStateTrackers =
            new LinkedHashMap<>();

    private final Map<HookId, BaseHook> mHooks =
            new HashMap<>();

    public synchronized void addHook(BaseHook hook) {

        if (hook == null)
            return;

        try {

            mHooks.put(hook.getId(), hook);

        } catch (Throwable t) {

            Logger.error(t);
        }
    }

    public BaseHook getHook(HookId id) {
        return mHooks.get(id);
    }

    public synchronized void inject(
            OrcaGateway gateway,
            Predicate<BaseHook> filter
    ) {

        if (gateway == null || filter == null)
            return;

        for (BaseHook hook : mHooks.values()) {

            if (hook == null)
                continue;

            try {

                if (filter.test(hook)) {

                    hook.setToaster(gateway.getToaster());

                    Logger.info(
                            "Injecting hook: "
                                    + hook.getClass().getSimpleName()
                    );

                    hook.inject(gateway);
                }

            } catch (Throwable t) {

                Logger.error(
                        "Failed injecting hook: "
                                + hook.getClass().getSimpleName()
                );

                Logger.error(t);

                try {

                    HookStateTracker tracker =
                            mHookStateTrackers.get(hook);

                    if (tracker != null) {
                        tracker.updateState(HookState.PENDING);
                    }

                } catch (Throwable ignored) {
                }
            }
        }
    }

    private HookStateTracker addStateTracker(
            BaseHook hook,
            SharedPreferences hookStatePref
    ) {

        HookStateTracker tracker =
                new PreferencesHookStateTracker(
                        hookStatePref,
                        hook.getId().name()
                );

        mHookStateTrackers.put(hook, tracker);

        return tracker;
    }

    public synchronized void initStateTracking(
            SharedPreferences hookStatePref
    ) {

        if (hookStatePref == null)
            return;

        for (BaseHook hook : mHooks.values()) {

            if (hook == null)
                continue;

            try {

                hook.setStateTracker(
                        addStateTracker(hook, hookStatePref)
                );

            } catch (Throwable t) {

                Logger.error(t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void registerListener(
            HookId hookId,
            Object hookListener
    ) {

        try {

            BaseHook hook = mHooks.get(hookId);

            if (hook == null) {

                throw new RuntimeException(
                        "Non-existent hook "
                                + hookId.name()
                );
            }

            hook.addListener(hookListener);

        } catch (Throwable t) {

            Logger.error(t);
        }
    }

    public synchronized void reloadPending(
            OrcaGateway gateway
    ) {

        for (Map.Entry<BaseHook, HookStateTracker> entries
                : mHookStateTrackers.entrySet()) {

            try {

                HookStateTracker tracker = entries.getValue();

                if (tracker == null)
                    continue;

                if (tracker.getState().equals(HookState.PENDING)) {

                    BaseHook hook = entries.getKey();

                    if (hook != null) {

                        Logger.info(
                                "Reloading pending hook: "
                                        + hook.getClass().getSimpleName()
                        );

                        hook.inject(gateway);
                    }
                }

            } catch (Throwable t) {

                Logger.error(t);
            }
        }
    }

    public synchronized void resetStates() {

        for (HookStateTracker stateTracker
                : mHookStateTrackers.values()) {

            try {

                if (stateTracker != null) {

                    stateTracker.updateState(
                            HookState.PENDING
                    );
                }

            } catch (Throwable t) {

                Logger.error(t);
            }
        }
    }

    private static class PreferencesHookStateTracker
            extends HookStateTracker {

        private final SharedPreferences mSharedPreferences;
        private final String mKey;

        public PreferencesHookStateTracker(
                SharedPreferences sharedPreferences,
                String key
        ) {

            mSharedPreferences = sharedPreferences;
            mKey = key;
        }

        @Override
        public void updateState(HookState state) {

            try {

                mSharedPreferences.edit()
                        .putInt(
                                "HOOK/" + mKey,
                                state.getValue()
                        )
                        .apply();

            } catch (Throwable ignored) {
            }
        }

        @Override
        public HookState getState() {

            try {

                return HookState.fromValue(
                        getStateValue()
                );

            } catch (Throwable ignored) {

                return HookState.PENDING;
            }
        }

        @Override
        public int getStateValue() {

            try {

                return mSharedPreferences.getInt(
                        "HOOK/" + mKey,
                        HookState.PENDING.getValue()
                );

            } catch (Throwable ignored) {

                return HookState.PENDING.getValue();
            }
        }
    }
}
