package org.jpwh.env;

import org.hibernate.internal.util.StringHelper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

/**
 Создает фабрику EntityManagerFactory.

 Конфигурация единиц персистентности берется из META-INF/persistence.xml и других источников.

 Дополнительные имена файлов hbm.xml могут быть переданы конструктору.

 */
public class JPASetup {

    protected final String persistenceUnitName;
    protected final Map<String, String> properties = new HashMap<>();
    protected final EntityManagerFactory entityManagerFactory;

    public JPASetup(DatabaseProduct databaseProduct,
                    String persistenceUnitName,
                    String... hbmResources) throws Exception {

        this.persistenceUnitName = persistenceUnitName;

        // Отсутствие автоматического сканирования Hibernate, все единицы сохранения перечисляют явные классы/пакеты
        properties.put(
            "hibernate.archive.autodetection",
            "none"
        );

        // Действительно, единственный способ получить файлы hbm.xml в явную единицу персистентности
        // (где сканирование Hibernate отключено).
        properties.put(
            "hibernate.hbmxml.files",
            StringHelper.join(",", hbmResources != null ? hbmResources : new String[0])
        );

        // Мы не хотим повторять эти настройки для всех блоков в persistence.xml,
        // поэтому они устанавливаются здесь программно
        properties.put(
            "hibernate.format_sql",
            "true"
        );
        properties.put(
            "hibernate.use_sql_comments",
            "true"
        );

        // Select database SQL dialect
        properties.put(
            "hibernate.dialect",
            databaseProduct.hibernateDialect
        );

        entityManagerFactory =
            Persistence.createEntityManagerFactory(getPersistenceUnitName(), properties);
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public void createSchema() {
        generateSchema("create");
    }

    public void dropSchema() {
        generateSchema("drop");
    }

    public void generateSchema(String action) {
        // Взять свойства выходящего EMF, переопределить настройки генерации схемы для копии
        Map<String, String> createSchemaProperties = new HashMap<>(properties);
        createSchemaProperties.put(
            "javax.persistence.schema-generation.database.action",
            action
        );
        Persistence.generateSchema(getPersistenceUnitName(), createSchemaProperties);
    }
}
