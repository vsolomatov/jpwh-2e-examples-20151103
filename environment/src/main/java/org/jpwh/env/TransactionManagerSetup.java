package org.jpwh.env;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import java.util.logging.Logger;

/**
 Предоставляет пул соединений с базой данных
 с менеджером транзакций Bitronix JTA (http://docs.codehaus.org/display/BTM/Home).

 Hibernate будет искать источник данных и UserTransaction через JNDI,
 поэтому вам также понадобится файл jndi.properties.
 Минимальный контекст JNDI связан с Bitronix и запускается им.
 */
public class TransactionManagerSetup {

    public static final String DATASOURCE_NAME = "myDS";

    private static final Logger logger =
        Logger.getLogger(TransactionManagerSetup.class.getName());

    protected final Context context = new InitialContext();
    protected final PoolingDataSource datasource;
    public final DatabaseProduct databaseProduct;

    public TransactionManagerSetup(DatabaseProduct databaseProduct) throws Exception {
        this(databaseProduct, null);
    }

    public TransactionManagerSetup(DatabaseProduct databaseProduct,
                                   String connectionURL) throws Exception {

        logger.fine("Starting database connection pool");

        logger.fine("Setting stable unique identifier for transaction recovery");
        TransactionManagerServices.getConfiguration().setServerId("myServer1234");

        logger.fine("Disabling JMX binding of manager in unit tests");
        TransactionManagerServices.getConfiguration().setDisableJmx(true);

        logger.fine("Disabling transaction logging for unit tests");
        TransactionManagerServices.getConfiguration().setJournal("null");

        logger.fine("Disabling warnings when the database isn't accessed in a transaction");
        TransactionManagerServices.getConfiguration().setWarnAboutZeroResourceTransaction(false);

        logger.fine("Creating connection pool");
        datasource = new PoolingDataSource();
        datasource.setUniqueName(DATASOURCE_NAME);
        datasource.setMinPoolSize(1);
        datasource.setMaxPoolSize(5);
        datasource.setPreparedStatementCacheSize(10);

        // Наши тесты блокировки/управления версиями предполагают изоляцию транзакции READ COMMITTED.
        // Это не значение по умолчанию для MySQL, поэтому мы устанавливаем его здесь явно.
        datasource.setIsolationLevel("READ_COMMITTED");

        // Генератор схемы SQL Hibernate вызывает connection.setAutoCommit(true),
        // и мы используем режим автоматической фиксации, когда EntityManager находится в приостановленном режиме
        // и не связан с транзакцией.
        datasource.setAllowLocalTransactions(true);

        logger.info("Setting up database connection: " + databaseProduct);
        this.databaseProduct = databaseProduct;
        databaseProduct.configuration.configure(datasource, connectionURL);

        logger.fine("Initializing transaction and resource management");
        datasource.init();
    }

    public Context getNamingContext() {
        return context;
    }

    public UserTransaction getUserTransaction() {
        try {
            return (UserTransaction) getNamingContext()
                .lookup("java:comp/UserTransaction");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public DataSource getDataSource() {
        try {
            return (DataSource) getNamingContext().lookup(DATASOURCE_NAME);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void rollback() {
        UserTransaction tx = getUserTransaction();
        try {
            if (tx.getStatus() == Status.STATUS_ACTIVE ||
                tx.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tx.rollback();
        } catch (Exception ex) {
            System.err.println("Rollback of transaction failed, trace follows!");
            ex.printStackTrace(System.err);
        }
    }

    public void stop() throws Exception {
        logger.fine("Stopping database connection pool");
        datasource.close();
        TransactionManagerServices.getTransactionManager().shutdown();
    }

}
