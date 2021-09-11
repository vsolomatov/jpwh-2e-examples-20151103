package org.jpwh.env;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.*;

/**
 Запускает и останавливает среду JPA до/после тестового класса.

     Создайте подкласс для написания модульных тестов.
     Получите доступ к EntityManagerFactory с помощью JPATest.JPA и создавайте экземпляры EntityManager.

 Сбрасывает и создает схему базы данных SQL единицы персистентности до и после каждого метода тестирования.
 Это означает, что ваша база данных будет очищаться для каждого метода тестирования.

     Переопределите метод configurePersistenceUnit, чтобы указать пользовательское имя единицы персистентности
     или дополнительные имена файлов hbm.xml для загрузки для вашего тестового класса.

     Переопределите метод afterJPABootstrap(), чтобы выполнить операции до метода тестирования,
     но после того, как EntityManagerFactory будет готова.
     В этот момент вы можете создать EntityManager или Session#doWork(JDBC).

     Если требуется очистка, переопределите метод beforeJPAClose().

 */
public class JPATest extends TransactionManagerTest {

    public String persistenceUnitName;
    public String[] hbmResources;
    public JPASetup JPA;

    @BeforeClass
    public void beforeClass() throws Exception {
        configurePersistenceUnit();
    }

    public void configurePersistenceUnit() throws Exception {
        configurePersistenceUnit(null);
    }

    public void configurePersistenceUnit(String persistenceUnitName,
                                         String... hbmResources) throws Exception {
        this.persistenceUnitName = persistenceUnitName;
        this.hbmResources = hbmResources;
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        JPA = new JPASetup(TM.databaseProduct, persistenceUnitName, hbmResources);
        // Всегда сбрасывайте схему, очищая хотя бы некоторые артефакты,
        // которые могли остаться после последнего запуска,
        // если вдруг очистка не была выполнена правильно
        JPA.dropSchema();

        JPA.createSchema();
        afterJPABootstrap();
    }

    public void afterJPABootstrap() throws Exception {
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() throws Exception {
        if (JPA != null) {
            beforeJPAClose();
            if (!"true".equals(System.getProperty("keepSchema"))) {
                JPA.dropSchema();
            }
            JPA.getEntityManagerFactory().close();
        }
    }

    public void beforeJPAClose() throws Exception {

    }

    protected long copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[4096];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    protected String getTextResourceAsString(String resource) throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(resource);
        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + resource);
        }
        StringWriter sw = new StringWriter();
        copy(new InputStreamReader(is), sw);
        return sw.toString();
    }

    protected Throwable unwrapRootCause(Throwable throwable) {
        return unwrapCauseOfType(throwable, null);
    }

    protected Throwable unwrapCauseOfType(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (type != null && type.isAssignableFrom(current.getClass()))
                return current;
            throwable = current;
        }
        return throwable;
    }
}
