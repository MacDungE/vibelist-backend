package org.example.vibelist.domain.batch.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class BatchLoggingAspect {

    // BatchService 인터페이스를 구현한 클래스들의 executeBatch() 메서드만 타겟팅
    @Around("execution(* org.example.vibelist.domain..*.BatchService.executeBatch(..))")
    public Object logBatchExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        long start = System.currentTimeMillis();

        log.info("▶️ [배치 시작] {}.executeBatch", className);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("✅ [배치 완료] {}.executeBatch ({}ms)", className, elapsed);
            return result;
        } catch (Throwable e) {
            log.error("❌ [배치 실패] {}.executeBatch - {}", className, e.getMessage());
            throw e;
        }
    }
}
