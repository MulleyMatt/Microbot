package net.runelite.client.plugins.microbot.MullyScripts.DebugRecorder;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.coords.WorldPoint;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Recording -> Java generator with:
 *  - pattern detection (extract repeated sequences into helper methods)
 *  - widgetID mapping to Rs2Widget helper calls
 *  - sleepUntil checks for interactions
 *
 * Tweak constants below (TICK_MS, PATTERN_MIN_REPEATS, PATTERN_MIN_LENGTH) as needed.
 */
public class RecordingScriptGenerator
{
    private static final long TICK_MS = 600L; // ms per tick when replaying
    private static final int PATTERN_MIN_REPEATS = 2;
    private static final int PATTERN_MIN_LENGTH = 2;
    private static final int PATTERN_MAX_LENGTH = 8;

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static class RecAction {
        public Integer tick;
        public Long timestamp;
        public String event;
        public String option;
        public String target;
        public int itemID = -1;
        public int objectID = -1;
        public int widgetID = -1;
        public Position position;
        public Context context;

        static class Position { Integer plane; Integer x; Integer y; }
        static class Context { String state; String movement; Position destination; }
    }

    // mapping table for commonly-seen widgetIDs -> method to call (string template)
    // update/extend this map as you learn more widget IDs and appropriate Rs2Widget helpers
    private static final Map<Integer, String> WIDGET_MAPPING = new HashMap<>();
    static {
        // Example mappings from your sample / common RuneLite widgets.
        // Key = widgetId, Value = a Rs2Widget helper call snippet (without trailing semicolon)
        // NOTE: these are examples — adjust to your environment.
        WIDGET_MAPPING.put(9764864, "Rs2Widget.clickItemInInventory(%d)"); // 9764864 seems to be inventory container in your sample (use with itemId)
        WIDGET_MAPPING.put(48, "Rs2Widget.clickWidget(%d)"); // fallback generic widget click template
        // Add more mappings here as you discover them...
    }

    public static String generateFromJson(String recordingJsonPath)
    {
        try {
            Path jsonPath = Path.of(recordingJsonPath);
            if (!Files.exists(jsonPath)) return null;
            String json = Files.readString(jsonPath);
            Type listType = new TypeToken<List<RecAction>>(){}.getType();
            List<RecAction> actions = gson.fromJson(json, listType);
            if (actions == null || actions.isEmpty()) return null;

            String baseFileName = jsonPath.getFileName().toString().replaceAll("\\.json$", "");
            String className = sanitizeToJavaIdentifier(baseFileName) + "_Generated";
            String packageLine = "package net.runelite.client.plugins.microbot.generated;\n\n";

            // run pattern detection to replace repeated sequences with helpers
            PatternExtractionResult patternResult = extractPatterns(actions);

            String fullSource = buildSource(packageLine, className, patternResult);

            Path outDir = jsonPath.getParent();
            String outFileName = className + ".java";
            Path outPath = outDir.resolve(outFileName);
            Files.writeString(outPath, fullSource);

            System.out.println("Generated script: " + outPath.toAbsolutePath());
            return outPath.toAbsolutePath().toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    // structure to hold actions and detected patterns
    private static class PatternExtractionResult {
        public List<RecAction> actions;
        // map of helperName -> sequence
        public Map<String, List<RecAction>> helpers = new LinkedHashMap<>();
        // list of tokens representing final sequence: either "CALL_HELPER:n" or "ACTION:i"
        public List<Object> tokenSequence = new ArrayList<>();
    }

    // naive sliding-window pattern detection: look for repeated subsequences and replace them with helper calls
    private static PatternExtractionResult extractPatterns(List<RecAction> actions) {
        PatternExtractionResult res = new PatternExtractionResult();
        res.actions = actions;

        int n = actions.size();
        // Stringify small windows to identify repeats
        Map<String, List<Integer>> windowPositions = new HashMap<>();

        for (int len = PATTERN_MIN_LENGTH; len <= Math.min(PATTERN_MAX_LENGTH, n); len++) {
            for (int i = 0; i + len <= n; i++) {
                String key = windowKey(actions, i, i + len);
                windowPositions.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            }
        }

        // find keys with enough repeats; sort by length*repeats descending to pick largest wins first
        List<Map.Entry<String, List<Integer>>> candidates = new ArrayList<>();
        for (var e : windowPositions.entrySet()) {
            if (e.getValue().size() >= PATTERN_MIN_REPEATS) {
                candidates.add(e);
            }
        }
        candidates.sort((a,b) -> {
            int la = windowLengthFromKey(a.getKey()), lb = windowLengthFromKey(b.getKey());
            int va = a.getValue().size(), vb = b.getValue().size();
            return Integer.compare(la * va, lb * vb) * -1;
        });

        boolean[] covered = new boolean[n];
        int helperIndex = 1;

        // result tokenization: mark helpers and actions
        // we'll iterate positions, and when we detect a helper sequence starting at index i, emit CALL_HELPER and skip forward.
        for (int i = 0; i < n; ) {
            boolean matched = false;
            for (var cand : candidates) {
                int len = windowLengthFromKey(cand.getKey());
                // check if cand occurs at i
                List<Integer> positions = cand.getValue();
                if (positions.contains(i)) {
                    // ensure none of the covered positions overlap this block
                    boolean overlap = false;
                    for (int k = i; k < i + len; k++) if (covered[k]) { overlap = true; break; }
                    if (overlap) continue;

                    // create helper
                    String helperName = "helper_" + helperIndex++;
                    List<RecAction> seq = new ArrayList<>();
                    for (int k = i; k < i + len; k++) { seq.add(actions.get(k)); covered[k] = true; }
                    res.helpers.put(helperName, seq);

                    // mark the positions where this sequence appears and replace in tokenization later.
                    // For now, emit the helper and move on
                    res.tokenSequence.add("CALL_" + helperName);
                    i += len;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                // single action
                res.tokenSequence.add(actions.get(i));
                covered[i] = true;
                i++;
            }
        }

        // Note: This is a simple greedy approach: more advanced deduplication can be added later.
        return res;
    }

    // helper: return "window" key string based on actions between [start, end)
    private static String windowKey(List<RecAction> actions, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            RecAction a = actions.get(i);
            sb.append(a.option == null ? "evt:" + a.event : "opt:" + a.option)
                    .append("|obj:").append(a.objectID)
                    .append("|item:").append(a.itemID)
                    .append("|wid:").append(a.widgetID)
                    .append(";"); // delimiter
        }
        return sb.toString();
    }

    private static int windowLengthFromKey(String key) {
        // count number of ';' entries
        int c = 0;
        for (char ch : key.toCharArray()) if (ch == ';') ++c;
        return c;
    }

    private static String buildSource(String packageLine, String className, PatternExtractionResult patternResult) {
        StringBuilder sb = new StringBuilder();
        sb.append(packageLine);
        sb.append("import net.runelite.api.coords.WorldPoint;\n");
        sb.append("import net.runelite.client.plugins.microbot.Microbot;\n");
        sb.append("import net.runelite.client.plugins.microbot.Script;\n");
        sb.append("import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;\n");
        sb.append("import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;\n");
        sb.append("import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;\n");
        sb.append("import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;\n");
        sb.append("import net.runelite.client.plugins.microbot.util.player.Rs2Player;\n");
        sb.append("import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;\n");
        sb.append("import java.util.concurrent.TimeUnit;\n\n");

        sb.append("/** Generated by RecordingScriptGenerator on ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append(" */\n");
        sb.append("public class ").append(className).append(" extends Script {\n\n");

        // run()
        sb.append("    @Override\n    public boolean run() {\n");
        sb.append("        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {\n");
        sb.append("            try {\n");
        sb.append("                if (!Microbot.isLoggedIn()) return;\n");
        sb.append("                if (!super.run()) return;\n");
        sb.append("                playRecording();\n");
        sb.append("                shutdown();\n");
        sb.append("            } catch (Exception ex) {\n");
        sb.append("                ex.printStackTrace();\n");
        sb.append("            }\n");
        sb.append("        }, 0, 600, TimeUnit.MILLISECONDS);\n");
        sb.append("        return true;\n    }\n\n");

        // playRecording
        sb.append("    private void playRecording() {\n");
        sb.append("        try {\n");

        // We'll walk through the tokenSequence
        // keep an index to map ticks for timing
        int lastTick = -1;
        for (Object token : patternResult.tokenSequence) {
            if (token instanceof String && ((String) token).startsWith("CALL_")) {
                String helperName = ((String) token).substring("CALL_".length());
                sb.append("            ").append(helperName).append("();\n");
                // no tick update here (helper will contain actions with internal waits)
            } else if (token instanceof RecAction) {
                RecAction a = (RecAction) token;
                // emit sleep by ticks since lastTick
                if (a.tick != null) {
                    if (lastTick >= 0) {
                        int delta = Math.max(0, a.tick - lastTick);
                        if (delta > 0) sb.append("            sleep(").append(delta).append("L * ").append(TICK_MS).append("L);\n");
                    }
                    lastTick = a.tick;
                }

                // map action
                sb.append(mapActionToCodeWithSleepUntil(a));
            }
        }

        sb.append("        } catch (Exception ex) { ex.printStackTrace(); }\n");
        sb.append("    }\n\n");

        // emit helpers
        int helperIdx = 1;
        for (Map.Entry<String, List<RecAction>> e : patternResult.helpers.entrySet()) {
            String name = e.getKey();
            List<RecAction> seq = e.getValue();
            sb.append("    private void ").append(name).append("() {\n");
            sb.append("        try {\n");
            int localLastTick = -1;
            for (RecAction a : seq) {
                if (a.tick != null) {
                    if (localLastTick >= 0) {
                        int delta = Math.max(0, a.tick - localLastTick);
                        if (delta > 0) sb.append("            sleep(").append(delta).append("L * ").append(TICK_MS).append("L);\n");
                    }
                    localLastTick = a.tick;
                }
                sb.append(mapActionToCodeWithSleepUntil(a));
            }
            sb.append("        } catch (Exception ex) { ex.printStackTrace(); }\n");
            sb.append("    }\n\n");
            helperIdx++;
        }

        // helper utility methods
        sb.append("    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }\n\n");

        sb.append("    private boolean sleepUntilTrue(java.util.function.Supplier<Boolean> cond, long pollMs, long timeoutMs) {\n");
        sb.append("        long start = System.currentTimeMillis();\n");
        sb.append("        while (System.currentTimeMillis() - start < timeoutMs) {\n");
        sb.append("            try { if (cond.get()) return true; Thread.sleep(pollMs); } catch (Exception ignored) {}\n");
        sb.append("        }\n");
        sb.append("        return false;\n");
        sb.append("    }\n");

        sb.append("}\n");
        return sb.toString();
    }

    // Build code for an action with sleepUntil where applicable
    private static String mapActionToCodeWithSleepUntil(RecAction a) {
        StringBuilder out = new StringBuilder();
        String opt = a.option == null ? "" : a.option.toLowerCase(Locale.ROOT);
        int itemId = a.itemID;
        int objectId = a.objectID;
        int widgetId = a.widgetID;
        RecAction.Position pos = a.position;
        RecAction.Context ctx = a.context;

        // Walk
        if (opt.contains("walk") || opt.equalsIgnoreCase("walk here")) {
            if (ctx != null && ctx.destination != null) {
                out.append("            Rs2Walker.walkTo(new WorldPoint(")
                        .append(ctx.destination.x).append(", ").append(ctx.destination.y).append(", ").append(ctx.destination.plane).append("));\n");
                out.append("            sleepUntilTrue(() -> !Rs2Player.isMoving(), 100, 5000);\n");
            } else if (pos != null) {
                out.append("            Rs2Walker.walkTo(new WorldPoint(").append(pos.x).append(", ").append(pos.y).append(", ").append(pos.plane == null ? 0 : pos.plane).append("));\n");
                out.append("            sleepUntilTrue(() -> !Rs2Player.isMoving(), 100, 5000);\n");
            } else {
                out.append("            // walk action without coords\n");
            }
            return out.toString();
        }

        // Chop / Interact with object
        if (!opt.isEmpty() && (opt.contains("chop") || opt.contains("smelt") || opt.contains("attack") || opt.contains("pick") || opt.contains("enter") || opt.contains("use") || opt.contains("open") || opt.contains("douse") || opt.contains("melt"))) {
            if (objectId > 0) {
                out.append("            Rs2GameObject.interact(").append(objectId).append(", ").append("\"").append(escapeForJava(a.option)).append("\");\n");
                // wait until animation or location change
                out.append("            sleepUntilTrue(() -> Rs2Player.isAnimating() || !Rs2Player.getWorldLocation().equals(new WorldPoint(")
                        .append(a.position != null ? a.position.x : 0).append(", ")
                        .append(a.position != null ? a.position.y : 0).append(", ")
                        .append(a.position != null ? (a.position.plane == null ? 0 : a.position.plane) : 0).append(")), 100, 8000);\n");
                return out.toString();
            }
            if (widgetId > 0) {
                // try mapping
                String mapped = tryWidgetMapping(widgetId, itemId);
                out.append("            ").append(mapped).append(";\n");
                out.append("            sleepUntilTrue(() -> Rs2Player.isAnimating() || Rs2Widget.isWidgetOpen(").append(widgetId).append(") , 100, 5000);\n");
                return out.toString();
            }
        }

        // Eat/Drink/Use inventory
        if (opt.contains("eat") || opt.contains("drink") || opt.contains("use") || opt.contains("consume")) {
            if (itemId > 0) {
                out.append("            if (Rs2Inventory.hasItem(").append(itemId).append(")) {\n");
                out.append("                Rs2Inventory.useItem(").append(itemId).append(");\n");
                out.append("                Rs2Inventory.waitForInventoryChanges(1200);\n");
                out.append("            }\n");
                return out.toString();
            } else {
                out.append("            // eat/drink but itemId missing\n");
                return out.toString();
            }
        }

        // Drop item
        if (opt.contains("drop")) {
            if (itemId > 0) {
                out.append("            if (Rs2Inventory.hasItem(").append(itemId).append(")) {\n");
                out.append("                Rs2Inventory.dropItem(").append(itemId).append(");\n");
                out.append("                Rs2Inventory.waitForInventoryChanges(1200);\n");
                out.append("            }\n");
                return out.toString();
            }
        }

        // NPC interactions
        if (objectId > 0 && (opt.contains("attack") || opt.contains("talk") || opt.contains("trade"))) {
            out.append("            Rs2Npc.interact(").append(objectId).append(", ").append("\"").append(escapeForJava(a.option)).append("\");\n");
            out.append("            sleepUntilTrue(() -> Rs2Player.isAnimating() || !Rs2Player.isIdle(), 100, 7000);\n");
            return out.toString();
        }

        // Widget fallback (try mapping)
        if (widgetId > 0) {
            String mapped = tryWidgetMapping(widgetId, itemId);
            out.append("            ").append(mapped).append(";\n");
            return out.toString();
        }

        // fallback comment
        out.append("            // Unmapped action: option='").append(escapeForJava(a.option)).append("' target='").append(escapeForJava(a.target)).append("' itemId=").append(a.itemID).append(" objectId=").append(a.objectID).append("\n");
        return out.toString();
    }

    private static String tryWidgetMapping(int widgetId, int itemId) {
        if (WIDGET_MAPPING.containsKey(widgetId)) {
            String tmpl = WIDGET_MAPPING.get(widgetId);
            if (tmpl.contains("%d")) {
                return String.format(tmpl, itemId > 0 ? itemId : widgetId);
            } else return tmpl;
        }
        // fallback: generic click
        return "Rs2Widget.clickWidget(" + widgetId + ")";
    }

    private static String sanitizeToJavaIdentifier(String name) {
        String s = name.replaceAll("[^A-Za-z0-9_]", "_");
        if (s.isEmpty() || !Character.isJavaIdentifierStart(s.charAt(0))) s = "_" + s;
        return s;
    }

    private static String escapeForJava(String in) {
        if (in == null) return "";
        return in.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
