package vn.huit.dl4j.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Nap san mot vai dong mau tu creditcard.csv (ca Class=0 va Class=1 that) luc startup,
 * de web demo co the "lay vi du ngau nhien" thay vi bat nguoi dung tu nhap 30 feature.
 */
@RestController
@RequestMapping("/api/sample")
public class SampleController {

    private final List<double[]> normalSamples = new ArrayList<>();
    private final List<double[]> fraudSamples = new ArrayList<>();
    private final Random random = new Random();

    @PostConstruct
    public void loadSamples() throws Exception {
        Path csvPath = Path.of("data/creditcard.csv");
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath.toFile()))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null
                    && (normalSamples.size() < 20 || fraudSamples.size() < 20)) {
                String[] parts = line.replace("\"", "").split(",");
                int classLabel = Integer.parseInt(parts[parts.length - 1]);
                double[] features = new double[30];
                for (int i = 0; i < 30; i++) {
                    features[i] = Double.parseDouble(parts[i]);
                }
                if (classLabel == 1 && fraudSamples.size() < 20) {
                    fraudSamples.add(features);
                } else if (classLabel == 0 && normalSamples.size() < 20) {
                    normalSamples.add(features);
                }
            }
        }
    }

    @GetMapping
    public double[] getSample(@RequestParam(defaultValue = "0") int classLabel) {
        List<double[]> pool = classLabel == 1 ? fraudSamples : normalSamples;
        return pool.get(random.nextInt(pool.size()));
    }
}
