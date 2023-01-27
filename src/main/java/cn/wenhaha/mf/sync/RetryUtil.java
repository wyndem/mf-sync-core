package cn.wenhaha.mf.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 重试
 * --------
 *
 * @author ：wyndem
 * @Date ：Created in 2023-01-26 13:40
 */
public class RetryUtil {

    private static final Logger log = LoggerFactory.getLogger(RetryUtil.class);


    public static   <T> Optional<T> call(Supplier<T> task, int number) {
        int i = 0;
        Exception e1 = null;
        while (i < number) {
            try {
                T t = task.get();
                return Optional.ofNullable(t);
            } catch (Exception e) {
                i++;
                log.error("执行出错，重试中 {}/{}, 具体错误为： {}", i, number, e.getMessage());
                e1 = e;
            }
        }
        assert e1 != null;
        throw (RuntimeException) e1;

    }

}
