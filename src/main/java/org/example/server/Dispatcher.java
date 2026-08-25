package org.example.server;

import org.example.common.Request;
import org.example.service.CalcService;
import org.example.service.CalcServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Dispatcher {

    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private static final Map<String, Object> services = new ConcurrentHashMap<>();

    //把一个实现类挂到某个接口下
    public static void register(Class<?> interfaceClass, Object impl) {
        services.put(interfaceClass.getName(), impl);
    }

    //都提供了哪些服务，注册到 Zk 时要用
    public static Set<String> getServiceNames() {
        return services.keySet();
    }

    //处理请求的入口
    public static Object dispatch(Request req) throws Exception{
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
        }catch (InvocationTargetException e){
            Throwable cause = e.getCause();
            logger.error("业务方法出错", cause);
            throw new RuntimeException(cause.getMessage(), cause);
        }catch (Exception e){
            logger.error("调用方法失败：{}#{}", req.getServiceName(),req.getMethodName(), e);
            throw e;
        }

    }

}
