package org.example.vibelist.domain.batch.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    @Around("execution(* org.example.vibelist.domain..client.*ApiClient.*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        long start = System.currentTimeMillis();
        log.info("🟡 [API 호출 시작] {}.{}({})", className, methodName, argsToString(args));

        try {
            Object result = joinPoint.proceed();
            long end = System.currentTimeMillis();
            log.info("🟢 [API 호출 완료] {}.{} → 반환값: {} ({}ms 소요)", className, methodName, shortResult(result), (end - start));
            return result;
        } catch (Throwable e) {
            log.error("🔴 [API 호출 실패] {}.{} 예외: {}", className, methodName, e.getMessage());
            throw e;
        }
    }

    private String argsToString(Object[] args) {
        if (args == null || args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            sb.append(arg).append(", ");
        }
        return sb.substring(0, sb.length() - 2);
    }

    private String shortResult(Object result) {
        if (result == null) return "null";
        return result.toString().length() > 100
                ? result.toString().substring(0, 100) + "..."
                : result.toString();
    }
}

