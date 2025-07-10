package com.xm.sanvanfo.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;


import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ObjectValidateConverter {
    private final Validator validator;
    private final ObjectMapper mapper;

    public ObjectValidateConverter(ObjectMapper mapper) {
        this.mapper = mapper;
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }


    public <T> T convert(Object o, Class<T> clazz) throws IllegalArgumentException {
       T t = mapper.convertValue(o, clazz);
        doValidate(t);
        return t;
    }

    public <T> T convert(Object o, TypeReference<T> type) {
        T t = mapper.convertValue(o, type);
        doValidate(t);
        return t;
    }

    public <T> T convert(Object o, JavaType type) {
        T t = mapper.convertValue(o, type);
        doValidate(t);
        return t;
    }

    public void doValidate(Object o) {
        Set<ConstraintViolation<Object>> result = this.validator.validate(o);
        if(!result.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<ConstraintViolation<Object>> it = result.iterator(); it.hasNext();) {
                ConstraintViolation<Object> violation = it.next();
                sb.append(violation.getPropertyPath()).append(" - ").append(violation.getMessage());
                if (it.hasNext()) {
                    sb.append("; ");
                }
            }
            throw new IllegalArgumentException(sb.toString());
        }
    }
}
