package org.example.server;

import org.example.common.Request;
import org.example.service.CalcServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Dispatcher {

    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private static final Map<String, Object> services = new HashMap<>();

    //创建服务目录表
    static{
        services.put("CalcService", new CalcServiceImpl());
    }

    //处理请求的入口
    public static Object dispatch(Request req) throws Exception{
//        System.out.println("进入 dispatcher");
        Object service = services.get(req.getServiceName());

        if (service == null) {
            logger.error("服务不存在:{}",req.getServiceName());
            throw new RuntimeException("服务不存在!");
        }

        try{
            Method method = service.getClass().getMethod(req.getMethodName(),req.getParamTypes());
            return method.invoke(service,req.getArgs());
        }catch (NoSuchMethodException e){
            logger.error("方法不存在：{}#{}", req.getServiceName(),req.getMethodName(), e);
            throw new RuntimeException("方法不存在！");
        }catch (Exception e){
            logger.error("调用方法失败：{}#{}", req.getServiceName(),req.getMethodName(), e);
            throw e;
        }

    }

}
