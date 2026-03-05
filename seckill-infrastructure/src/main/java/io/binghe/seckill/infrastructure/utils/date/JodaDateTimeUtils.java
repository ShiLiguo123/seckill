/**
 * Copyright 2022-9999 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.binghe.seckill.infrastructure.utils.date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Date;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 日志工具类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class JodaDateTimeUtils {

    /**
     * 日期时间格式
     */
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 时间格式
     */
    public static final String TIME_FORMAT = "HH:mm:ss";


    /**
     * 将日期转换成字符串
     */
    public static String parseDateToString(Date date, String format){
        return new DateTime(date).toString(format);
    }

    /**
     * 将字符串转换成日期
     */
    public static Date parseStringToDate(String date, String format){
        return DateTimeFormat.forPattern(format).parseDateTime(date).toDate();
    }

}
