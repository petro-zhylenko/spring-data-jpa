package org.springframework.data.jpa.repository.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Aspect for handling @Modifying annotations with conditional transactional support.
 * <p>
 * This aspect intercepts methods annotated with @Modifying and determines whether a transaction
 * should be opened based on the `transactional` parameter of the annotation.
 * <p>
 * - If `transactional` is set to `true`, a new transaction is explicitly opened using
 * {@link PlatformTransactionManager}, and the method execution is wrapped within this transaction.
 * - If `transactional` is set to `false`, the method executes without opening a transaction.
 * <p>
 * Usage:
 * This aspect works with Spring Data JPA's @Modifying annotation, allowing for fine-grained control
 * over transaction management for specific methods.
 * <p>
 * Example:
 * <pre>
 * {@code
 * @Modifying(transactional = true)
 * @Query("UPDATE YourEntity y SET y.field = :value WHERE y.id = :id")
 * void updateEntity(@Param("value") String value, @Param("id") Long id);
 * }
 * </pre>
 * <p>
 * Dependencies:
 * - Requires a {@link PlatformTransactionManager} bean to be configured in the application context.
 * - Should be used in conjunction with Spring AOP or AspectJ for proper weaving.
 *
 * @author Petro Zhylenko
 */
@Aspect
@Component
public class ModifyingTransactionAspect {

  private final PlatformTransactionManager transactionManager;

  public ModifyingTransactionAspect(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  @Around("@annotation(modifying)")
  public Object handleModifyingWithTransactional(ProceedingJoinPoint joinPoint, Modifying modifying) throws Throwable {
    if (modifying.transactional()) { // if we need to open transaction 
      // with transaction
      DefaultTransactionDefinition def = new DefaultTransactionDefinition();
      def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
      TransactionStatus status = transactionManager.getTransaction(def);

      try {
        Object result = joinPoint.proceed();
        transactionManager.commit(status);
        return result;
      } catch (Throwable ex) {
        transactionManager.rollback(status);
        throw ex;
      }
    } else {
      // without transaction
      return joinPoint.proceed();
    }
  }
}
