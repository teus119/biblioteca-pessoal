package com.biblioteca;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Classe base para todos os testes que necessitam de MongoDB real.
 * Usa Testcontainers no padrão Singleton para subir um contêiner MongoDB isolado.
 * NÃO usa mocks de banco de dados.
 */
public abstract class MongoTestBase {

    protected static final MongoDBContainer mongoDBContainer =
            new MongoDBContainer(DockerImageName.parse("mongo:4.4"))
                    .withReuse(true);

    static {
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void setMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
}
