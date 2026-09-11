package org.vector.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingStoreConfig {

    @Value("${vector-store.jdbc-url}")
    private String jdbcUrl;

    @Value("${vector-store.username}")
    private String username;

    @Value("${vector-store.password}")
    private String password;

    @Value("${vector-store.table}")
    private String table;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        // useIndex(false): při tomto rozsahu dat (řádově stovky až tisíce chunků na thesis-scale
        // projekt) je sekvenční scan dost rychlý a nevyžaduje ladění IVFFlat parametru indexListSize
        // (ten navíc dává smysl trénovat až na neprázdné tabulce, ne při startu na prázdné).
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(table)
                .dimension(embeddingModel.dimension())
                .useIndex(false)
                .createTable(true)
                .build();
    }
}
