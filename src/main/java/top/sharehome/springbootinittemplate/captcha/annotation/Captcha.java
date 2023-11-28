package top.sharehome.springbootinittemplate.captcha.annotation;

import java.lang.annotation.*;

/**
 * 需要验证码的方法注解
 *
 * @author AntonyCheng
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Captcha {

}