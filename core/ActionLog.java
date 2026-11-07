package core;

import java.util.ArrayList;
import java.util.List;

public class ActionLog {
    private final List<String> entries = new ArrayList<>();
    private final int maxLines;

    public ActionLog(int maxLines) {
        this.maxLines = maxLines;
    }

    public void add(String message) {
        entries.add(0, message);
        if (entries.size() > maxLines) {
            entries.remove(entries.size() - 1);
        }
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    public String get(int index) {
        return entries.get(index);
    }
}
