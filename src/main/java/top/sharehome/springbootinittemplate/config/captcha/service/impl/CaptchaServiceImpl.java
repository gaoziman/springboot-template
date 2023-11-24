package top.sharehome.springbootinittemplate.config.captcha.service.impl;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReflectUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;
import top.sharehome.springbootinittemplate.common.base.ReturnCode;
import top.sharehome.springbootinittemplate.config.bean.SpringContextHolder;
import top.sharehome.springbootinittemplate.config.captcha.model.Captcha;
import top.sharehome.springbootinittemplate.config.captcha.properties.CaptchaProperties;
import top.sharehome.springbootinittemplate.config.captcha.properties.enums.CaptchaType;
import top.sharehome.springbootinittemplate.config.captcha.service.CaptchaService;
import top.sharehome.springbootinittemplate.exception.customize.CustomizeReturnException;
import top.sharehome.springbootinittemplate.utils.redisson.CacheUtils;
import top.sharehome.springbootinittemplate.utils.redisson.constants.KeyPrefixConstants;

import javax.annotation.Resource;

/**
 * 验证码服务实现类
 *
 * @author xg
 */
@EnableConfigurationProperties(CaptchaProperties.class)
@Service
public class CaptchaServiceImpl implements CaptchaService {

    @Resource
    private CaptchaProperties captchaProperties;

    @Override
    public Captcha createCaptcha() {
        Captcha captchaResponse = new Captcha();
        boolean enable = captchaProperties.isEnable();
        captchaResponse.setEnableCode(enable);
        if (!enable) {
            return captchaResponse;
        }
        String uuid = IdUtil.simpleUUID();
        String codeKeyInRedis = KeyPrefixConstants.CAPTCHA_PREFIX + uuid;
        CaptchaType captchaType = captchaProperties.getType();
        boolean isMath = CaptchaType.MATH == captchaType;
        Integer length = isMath ? captchaProperties.getNumberLength() : captchaProperties.getCharLength();
        CodeGenerator codeGenerator = ReflectUtil.newInstance(captchaType.getClazz(), length);
        AbstractCaptcha captcha = SpringContextHolder.getBean(captchaProperties.getCategory().getClazz());
        captcha.setGenerator(codeGenerator);
        captcha.createCode();
        String code = captcha.getCode();
        if (isMath) {
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(StringUtils.remove(code, "="));
            code = exp.getValue(String.class);
        }
        CacheUtils.putString(codeKeyInRedis, code, captchaProperties.getExpired());
        captchaResponse.setUuid(uuid)
                .setImgBase64(captcha.getImageBase64());
        return captchaResponse;
    }

    @Override
    public void checkCaptcha(String code, String uuid) {
        if (StringUtils.isBlank(code)) {
            throw new CustomizeReturnException(ReturnCode.CAPTCHA_IS_EMPTY);
        }
        if (StringUtils.isBlank(uuid)) {
            throw new CustomizeReturnException(ReturnCode.CAPTCHA_IS_INVALID);
        }
        String codeKeyInRedis = KeyPrefixConstants.CAPTCHA_PREFIX + uuid;
        String codeValue = CacheUtils.getString(codeKeyInRedis);
        if (StringUtils.isBlank(codeValue)) {
            throw new CustomizeReturnException(ReturnCode.CAPTCHA_HAS_EXPIRED);
        }
        CacheUtils.deleteString(codeKeyInRedis);
        if (!StringUtils.equals(code, codeValue)) {
            throw new CustomizeReturnException(ReturnCode.CAPTCHA_IS_INCORRECT);
        }
    }

}