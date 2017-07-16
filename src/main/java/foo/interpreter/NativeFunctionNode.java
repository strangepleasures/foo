package foo.interpreter;

import foo.model.FunctionNode;
import foo.model.ParameterNode;
import foo.utils.PrintUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class NativeFunctionNode extends FunctionNode implements Callable {
    private final Method method;

    public NativeFunctionNode(Method method) {
        String methodName = method.isAnnotationPresent(Name.class) ? method.getAnnotation(Name.class).value() : method.getName();
        setName(methodName);

        String packageName;
        if (method.getDeclaringClass().isAnnotationPresent(Name.class)) {
            packageName = method.getDeclaringClass().getAnnotation(Name.class).value();
        } else {
            packageName = method.getDeclaringClass().getSimpleName().toLowerCase();
        }

        setId(packageName + ":" + methodName);

        if (method.isAnnotationPresent(Description.class)) {
            setComment(method.getAnnotation(Description.class).value());
        }

        for (Parameter parameter: method.getParameters()) {
            ParameterNode param = new ParameterNode();
            String paramName = parameter.isAnnotationPresent(Name.class) ? parameter.getAnnotation(Name.class).value() : parameter.getName();
            param.setName(paramName);
            param.setId(getId() + ":" + paramName);
            if (parameter.isAnnotationPresent(Description.class)) {
                param.setComment(parameter.getAnnotation(Description.class).value());
            }
            getParameters().add(param);
        }
        this.method = method;
    }

    @Override
    public Object call(Object[] args) {
        try {
            return method.invoke(null, args);
        } catch (IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
