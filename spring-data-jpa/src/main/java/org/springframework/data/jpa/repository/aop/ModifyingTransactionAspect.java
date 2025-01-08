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
