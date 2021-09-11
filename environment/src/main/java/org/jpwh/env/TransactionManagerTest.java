package org.jpwh.env;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.util.Locale;

/**
 Запускает и останавливает диспетчер транзакций(пул базы данных) до/после набора тестов.
 Все тесты в наборе выполняются с помощью одного TransactionManagerSetup,
 вызовите статический TransactionManagerTest.TM в своем тесте,
 чтобы получить доступ к диспетчеру транзакций JTA и соединениям с базой данных.

 Параметры теста:
    база данных (с указанием поддерживаемого DatabaseProduct)
    и URL-адрес подключения
 являются необязательными.

 По умолчанию используется экземпляр базы данных H2 в памяти, который автоматически создается
 и уничтожается для каждого набора тестов.
 */
public class TransactionManagerTest {

    // Статический единый диспетчер соединений с базой данных для каждого набора тестов
    static public TransactionManagerSetup TM;

    @Parameters({"database", "connectionURL"})
    @BeforeSuite()
    public void beforeSuite(@Optional String database,
                            @Optional String connectionURL) throws Exception {
        TM = new TransactionManagerSetup(
            database != null
                ? DatabaseProduct.valueOf(database.toUpperCase(Locale.US))
                : DatabaseProduct.H2,
            connectionURL
        );
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() throws Exception {
        if (TM != null)
            TM.stop();
    }
}
