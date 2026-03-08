package at.rewrite.gui.dsl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One category shown in the sidebar. */
public class Tab {

    public final String name;
    public final List<Option> options = new ArrayList<>();

    private Tab(String name) { this.name = name; }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static Tab named(String name) { return new Tab(name); }

    // ── Fluent builders ───────────────────────────────────────────────────────

    public Tab add(Option... opts) {
        options.addAll(Arrays.asList(opts));
        return this;
    }
}