/*
 ************************************
 * @项目名称: broker
 * @文件名称: JsonUtil
 * @Date 2019/05/23
 * @Author peiwei.ren@bhex.io
 * @Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 * 注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 **************************************
 */
package io.best.tool.uitl;

import java.lang.annotation.*;

/**
 * filed with this annotation will be exclude with gson serialize and deserialize
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GsonIgnore {

    /**
     * if authorize failed,
     *
     * @return True for yes and False will continue process
     */
    boolean skipSerialize() default true;

    /**
     * if authorize failed,
     *
     * @return True for yes and False will continue process
     */
    boolean skipDeserialize() default true;

}
