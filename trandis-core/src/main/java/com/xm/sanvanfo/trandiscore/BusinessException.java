package com.xm.sanvanfo.trandiscore;

import lombok.Getter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = -1752057476762201416L;
    private String stackMessage;
    public BusinessException(String message) {
      super(message);
      stackMessage = exceptionFullMessage(this);
    }

    public BusinessException(Throwable e, String message) {
        super(message + (null == e.getMessage() ? "" : ("-" + e.getMessage())), e);
        stackMessage = exceptionFullMessage(this);
    }

    public static String exceptionFullMessage(Throwable e) {
        e.printStackTrace();
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.fillInStackTrace().printStackTrace(printWriter);
        return " error:" + e.getMessage() + " stack:" + result.toString();
    }

}
