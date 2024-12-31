package fr.codeonce.grizzly.core.service.datasource.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.model.IndexOptions;

import java.util.Map;

public class IndexCollection {
    @JsonProperty("indexKeys")
    private Map<String, Integer> indexKeys;
     private IndexOptions indexOptions;

    public Map<String, Integer> getIndexKeys() {
        return indexKeys;
    }

    public void setIndexKeys(Map<String, Integer> indexKeys) {
        this.indexKeys = indexKeys;
    }

    public IndexOptions getIndexOptions() {
        return indexOptions;
    }

    public void setIndexOptions(IndexOptions indexOptions) {
        this.indexOptions = indexOptions;
    }
}
