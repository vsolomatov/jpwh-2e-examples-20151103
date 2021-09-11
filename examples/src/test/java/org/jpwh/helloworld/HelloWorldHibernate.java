package org.jpwh.helloworld;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.service.ServiceRegistry;
import org.jpwh.env.TransactionManagerTest;
import org.jpwh.model.helloworld.Message;
import org.testng.annotations.Test;

import javax.transaction.UserTransaction;
import java.util.List;

import static org.testng.Assert.*;

public class HelloWorldHibernate extends TransactionManagerTest {

    /*
        В наиболее кратком виде построение SessionFactory выглядит следующим образом
     */
    protected void unusedSimpleBoot() {
        SessionFactory sessionFactory = new MetadataSources(
            new StandardServiceRegistryBuilder()
                .configure("hibernate.cfg.xml").build()
        ).buildMetadata().buildSessionFactory();
    }

    /*
        Метод для создания SessionFactory который используется (здесь, в этом классе) в нашем примере
        (в отличие от метода приведенного выше)
     */
    protected SessionFactory createSessionFactory() {

        /* 
            Этот конструктор поможет вам создать неизменяемый реестр служб с помощью связанных вызовов методов.
         */
        StandardServiceRegistryBuilder serviceRegistryBuilder =
            new StandardServiceRegistryBuilder();

        /* 
            Настройте (сконфигурируйте) реестр служб, применив настройки.
         */
        serviceRegistryBuilder
            .applySetting("hibernate.connection.datasource", "myDS")
            .applySetting("hibernate.format_sql", "true")
            .applySetting("hibernate.use_sql_comments", "true")
            .applySetting("hibernate.hbm2ddl.auto", "create-drop");

        // Включите JTA (это немного грубо, потому что разработчики Hibernate все еще считают,
        // что JTA используется только в чудовищных серверах приложений, и вы никогда не увидите этот код).
        serviceRegistryBuilder.applySetting(
            Environment.TRANSACTION_COORDINATOR_STRATEGY,
            JtaTransactionCoordinatorBuilderImpl.class
        );
        ServiceRegistry serviceRegistry = serviceRegistryBuilder.build();

        /* 
            На этот этап настройки можно войти только с существующим реестром служб.
         */
        MetadataSources metadataSources = new MetadataSources(serviceRegistry);

        /* 
            Добавьте свои постоянные классы в источники метаданных (сопоставление).
            Т.е. сообщите Hibernate, какие из хранимых классов являются частью метаданных отображения.
         */
        metadataSources.addAnnotatedClass(
            org.jpwh.model.helloworld.Message.class
        );
        // В прикладном программном интерфейсе MetadataSources имеется
        // множество методов для добавления источников отображений;
        // за дополнительной информацией обращайтесь к документации JavaDoc
        // Add hbm.xml mapping files
        // metadataSources.addFile(...);
        // Read all hbm.xml mapping files from a JAR
        // metadataSources.addJar(...)

        // Построение всех метаданных, нужных фреймворку Hibernate, с помощью экземпляра MetadataBuilder,
        // полученного из источников метаданных
        MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();

        // Можно запросить метаданные для программного взаимодействия с конфигурацией Hibernate
        Metadata metadata = metadataBuilder.build();
        assertEquals(metadata.getEntityBindings().size(), 1);

        // И наконец, создаем SessionFactory
        SessionFactory sessionFactory = metadata.buildSessionFactory();

        return sessionFactory;
    }

    @Test
    public void storeLoadMessage() throws Exception {
        SessionFactory sessionFactory = createSessionFactory();
        try {
            {
                /* 
                    Получите доступ к стандартному API транзакций UserTransaction
                    и начните транзакцию в этом потоке выполнения.
                 */
                UserTransaction tx = TM.getUserTransaction();
                tx.begin();

                /* 
                    Всякий раз, когда вы вызываете getCurrentSession() в том же потоке,
                    вы получаете тот же org.hibernate.Session.
                    Он автоматически привязывается к текущей транзакции и автоматически закрывается для вас,
                    когда транзакция фиксируется или откатывается.
                 */
                Session session = sessionFactory.getCurrentSession();

                Message message = new Message();
                message.setText("Hello World!");

                /* 
                    Собственный Hibernate API очень похож на стандартный Java Persistence API,
                    и большинство методов имеют то же имя.
                 */
                session.persist(message);

                /* 
                    Hibernate синхронизирует сеанс с базой данных
                    и автоматически закрывает «текущий» сеанс при фиксации связанной транзакции.
                 */
                tx.commit();
                // INSERT into MESSAGE (ID, TEXT) values (1, 'Hello World!')
            }

            {
                UserTransaction tx = TM.getUserTransaction();
                tx.begin();

                /* 
                    Запрос критериев Hibernate - это типобезопасный программный способ выражения запросов,
                    автоматически переводимых в SQL.
                 */
                List<Message> messages =
                    sessionFactory.getCurrentSession().createCriteria(
                        Message.class
                    ).list();
                // SELECT * from MESSAGE

                assertEquals(messages.size(), 1);
                assertEquals(messages.get(0).getText(), "Hello World!");

                tx.commit();
            }

        } finally {
            TM.rollback();
        }
    }
}
