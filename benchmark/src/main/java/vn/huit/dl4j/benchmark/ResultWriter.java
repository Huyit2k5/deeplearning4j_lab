package vn.huit.dl4j.benchmark;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Ghi ket qua benchmark ra ca CSV (de mo bang Excel) va JSON (de web demo doc ve ve chart). */
public final class ResultWriter {

    private ResultWriter() {
    }

    public static void writeCsv(File file, List<String> header, List<List<Object>> rows) throws IOException {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(String.join(",", header));
            writer.write("\n");
            for (List<Object> row : rows) {
                writer.write(row.stream().map(String::valueOf).collect(Collectors.joining(",")));
                writer.write("\n");
            }
        }
    }

    public static void writeJson(File file, List<Map<String, Object>> records) throws IOException {
        file.getParentFile().mkdirs();
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < records.size(); i++) {
            sb.append("  ").append(toJsonObject(records.get(i)));
            if (i < records.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]\n");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(sb.toString());
        }
    }

    private static String toJsonObject(Map<String, Object> record) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (i++ > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(value).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
