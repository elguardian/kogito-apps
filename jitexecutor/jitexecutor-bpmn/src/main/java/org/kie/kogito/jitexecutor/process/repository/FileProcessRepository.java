package org.kie.kogito.jitexecutor.process.repository;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.drools.core.util.IoUtils;
import org.kie.kogito.jitexecutor.process.ProcessFile;
import org.kie.kogito.jitexecutor.process.ProcessRepository;

@ApplicationScoped
public class FileProcessRepository implements ProcessRepository {

    @Override
    public List<ProcessFile> processes() {
        List<ProcessFile> processes = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Paths.get("src/test/resources"))) {
            processes.addAll(
                    walk.filter(Files::isRegularFile)
                        .map(this::toProcessFile)
                        .collect(Collectors.toList()));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return processes;
    }

    private ProcessFile toProcessFile(Path path) {
        try {
            String content = new String(IoUtils.readBytesFromInputStream(new FileInputStream(path.toFile())));
            return new ProcessFile (path.getFileName().toString(), content);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
