package at.rewrite.gui.dsl;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** All the option types you can place inside a Tab. */
public sealed interface Option permits
        Option.Toggle,
        Option.Hotkey,
        Option.EntryList,
        Option.NumInput,
        Option.Label,
        Option.Spacer {

    /** A simple on/off toggle. */
    record Toggle(
            String label,
            Supplier<Boolean> get,
            Consumer<Boolean> set
    ) implements Option {}

    /** Displays the current keybind and lets the user rebind it. */
    record Hotkey(
            String label,
            Supplier<String> getKeyName,
            Runnable startRebind
    ) implements Option {}

    /**
     * An editable list of strings (e.g. block IDs).
     * Backed by a live List — mutations are reflected immediately.
     */
    record EntryList(
            String label,
            Supplier<List<String>> getList,
            Consumer<List<String>> onChanged
    ) implements Option {}

    /** Plain info text — no interaction. */
    record Label(String text, int color) implements Option {
        public Label(String text) { this(text, 0xFF888899); }
    }

    /**
     * A numeric input for float config values.
     * Renders the current value with [-] and [+] buttons and a type-in field.
     * step  — how much each button click changes the value.
     * min/max — clamped bounds (use Float.MIN_VALUE / MAX_VALUE for none).
     */
    record NumInput(
            String label,
            Supplier<Float> get,
            Consumer<Float> set,
            float step,
            float min,
            float max
    ) implements Option {
        /** Convenience constructor: step=0.1, no bounds. */
        public NumInput(String label, Supplier<Float> get, Consumer<Float> set) {
            this(label, get, set, 0.1f, -Float.MAX_VALUE, Float.MAX_VALUE);
        }
        /** Convenience constructor: custom step, no bounds. */
        public NumInput(String label, Supplier<Float> get, Consumer<Float> set, float step) {
            this(label, get, set, step, -Float.MAX_VALUE, Float.MAX_VALUE);
        }
    }

    /** Vertical whitespace between options. */
    record Spacer(int pixels) implements Option {}
}