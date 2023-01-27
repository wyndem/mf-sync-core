package cn.wenhaha.mf.sync;

import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.ReUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * sf日期处理
 * --------
 *
 * @author ：wyndem
 * @Date ：Created in 2022-12-04 19:15
 */
public class SFDateUtil {

    /**
     * 本地时间格式化后 上传到sf的时间格式类
     **/
    public static final SimpleDateFormat uploadFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssZ");
    private static final Pattern pattern = PatternPool.get("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{4}", Pattern.DOTALL);

    public static String fromStr(Object date) {
        return uploadFormatter.format(date);
    }


    public static boolean isDate(String date) {
        return ReUtil.contains(pattern, date);
    }


    public static Date parse(String date) {
        try {
            return uploadFormatter.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }


}
