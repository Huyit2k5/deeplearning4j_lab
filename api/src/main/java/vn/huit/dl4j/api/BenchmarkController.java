package vn.huit.dl4j.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Doc cac file JSON ket qua benchmark (sinh boi module `benchmark`) tu thu muc results\
 * va tra ve cho web demo ve chart, tranh phai hardcode so lieu vao HTML.
 */
@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private static final String[] KNOWN_FILES = {
            "workspace_benchmark", "spark_scaling_benchmark",
            "inference_latency_inprocess", "inference_latency_remote_springboot",
            "inference_latency_remote_fastapi"
    };

    @GetMapping(value = "/results/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getResult(@PathVariable String name) throws IOException {
        Path resultsDir = Path.of("results");
        Path jsonFile = resultsDir.resolve(name + ".json");
        if (!Files.exists(jsonFile)) {
            return "[]";
        }
        return Files.readString(jsonFile, StandardCharsets.UTF_8);
    }

    @GetMapping(value = "/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAllResults() throws IOException {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < KNOWN_FILES.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(KNOWN_FILES[i]).append("\": ").append(getResult(KNOWN_FILES[i]));
        }
        sb.append("}");
        return sb.toString();
    }
}
