package it.barizone.integration;

import it.barizone.model.entity.MediaFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "barizone.media", name = "storage", havingValue = "db", matchIfMissing = true)
public class DatabaseMediaStorage implements MediaStorage {

    @Override
    public void store(MediaFile file, byte[] content) {
        file.setContent(content);
    }

    @Override
    public byte[] load(MediaFile file) {
        return file.getContent();
    }
}
