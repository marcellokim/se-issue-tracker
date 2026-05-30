package com.github.marcellokim.issuetracker.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

final class SwingComponentTestSupport {

    private SwingComponentTestSupport() {
    }

    static void onEdt(ThrowingRunnable action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }

        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        rethrow(failure.get());
    }

    static <T extends Component> T find(Container root, String name, Class<T> type) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new AssertionError("Swing component lookup must run on the EDT");
        }
        T component = findOrNull(root, name, type);
        if (component == null) {
            throw new AssertionError("Component not found: " + name);
        }
        return component;
    }

    private static <T extends Component> T findOrNull(Container root, String name, Class<T> type) {
        if (name.equals(root.getName()) && type.isInstance(root)) {
            return type.cast(root);
        }
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName()) && type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T nested = findOrNull(container, name, type);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static void rethrow(Throwable throwable) throws Exception {
        if (throwable == null) {
            return;
        }
        if (throwable instanceof Exception exception) {
            throw exception;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        throw new RuntimeException(throwable);
    }

    @FunctionalInterface
    interface ThrowingRunnable {

        void run() throws Exception;
    }
}
