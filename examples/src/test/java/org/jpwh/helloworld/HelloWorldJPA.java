package org.jpwh.helloworld;

import org.jpwh.env.TransactionManagerTest;
import org.jpwh.model.helloworld.Message;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.UserTransaction;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class HelloWorldJPA extends TransactionManagerTest {

    @Test
    public void storeLoadMessage() throws Exception {

        EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("HelloWorldPU");

        try {
            {
                /* 
                    Получите доступ к стандартному API транзакций UserTransaction
                    и начните транзакцию в этом потоке выполнения.
                 */
                UserTransaction tx = TM.getUserTransaction();
                tx.begin();

                /* 
                    Начните новый сеанс с базой данных, создав EntityManager,
                    это ваш контекст для всех операций сохранения.
                 */
                EntityManager em = emf.createEntityManager();

                /* 
                    Создайте новый экземпляр сопоставленного класса модели предметной области Message
                    и задайте его текстовое свойство.
                 */
                Message message = new Message();
                message.setText("Hello World!");

                /* 
                    Подключите временный экземпляр к своему контексту постоянства, вы сделаете его постоянным.
                    Теперь Hibernate знает, что вы хотите сохранить эти данные,
                    однако он не обязательно сразу вызывает базу данных.
                 */
                em.persist(message);

                /* 
                    Зафиксируйте транзакцию, Hibernate теперь автоматически проверяет контекст сохранения
                    и выполняет необходимую инструкцию SQL INSERT.
                 */
                tx.commit();
                // INSERT into MESSAGE (ID, TEXT) values (1, 'Hello World!')

                /* 
                    Если вы создаете EntityManager, вы должны его закрыть.
                 */
                em.close();
            }

            {
                /* 
                    Каждое взаимодействие с вашей базой данных должно происходить в пределах явных границ транзакции,
                    даже если вы только читаете данные.
                 */
                UserTransaction tx = TM.getUserTransaction();
                tx.begin();

                EntityManager em = emf.createEntityManager();

                /* 
                    Выполните запрос, чтобы получить все экземпляры сообщения из базы данных.
                 */
                List<Message> messages =
                    em.createQuery("select m from Message m").getResultList();
                // SELECT * from MESSAGE


                assertEquals(messages.size(), 1);
                assertEquals(messages.get(0).getText(), "Hello World!");

                /* 
                    Вы можете изменить значение свойства, Hibernate обнаружит это автоматически,
                    потому что загруженное сообщение все еще прикреплено к контексту сохранения,
                    в который оно было загружено.
                 */
                messages.get(0).setText("Take me to your leader!");

                /* 
                    При фиксации Hibernate проверяет контекст сохранения на наличие грязного состояния
                    и автоматически выполняет SQL UPDATE,
                    чтобы синхронизировать данные в памяти с состоянием базы данных.
                 */
                tx.commit();
                // UPDATE MESSAGE set TEXT = 'Take me to your leader!' where ID = 1

                em.close();
            }

        } finally {
            TM.rollback();
            emf.close();
        }
    }

}
