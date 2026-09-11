package org.vector.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

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

    @Bean(destroyMethod = "close")
    public DataSource vectorStoreDataSource() {
        // Malý, tvrdě omezený pool - Supabase pooler má pro celý projekt strop na počet
        // současných spojení (sdílený se všemi ostatními službami), takže tu záměrně
        // držíme jen pár spojení místo neomezeného/nepoolovaného přístupu.
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setPoolName("vector-pgvector-pool");
        hikariConfig.setMaximumPoolSize(3);
        hikariConfig.setMinimumIdle(0);
        hikariConfig.setIdleTimeout(30_000);
        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel, DataSource vectorStoreDataSource) {
        // useIndex(false): při tomto rozsahu dat (řádově stovky až tisíce chunků na thesis-scale
        // projekt) je sekvenční scan dost rychlý a nevyžaduje ladění IVFFlat parametru indexListSize
        // (ten navíc dává smysl trénovat až na neprázdné tabulce, ne při startu na prázdné).
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(vectorStoreDataSource)
                .table(table)
                .dimension(embeddingModel.dimension())
                .useIndex(false)
                .createTable(true)
                .build();
    }
}
