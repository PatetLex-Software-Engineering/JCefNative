package org.cef.js;

import org.cef.ui.WebPanel;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public interface JavaScriptGenerator {

    Stack<String> cachedVars = new Stack<>();

    default String generateMethod(Object object, Class<?> c, Method method) {
        String jsMethod = object != null ? method.getName() + ": async function(" : "async " + method.getName() + "(";
        int i = 0;
        List<Parameter> params = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            if (!WebPanel.class.isAssignableFrom(parameter.getType())) {
                params.add(parameter);
            }
        }
        for (Parameter parameter : params.toArray(new Parameter[params.size()])) {
            jsMethod += parameter.getName();
            if (i + 1 < params.size()) {
                jsMethod += ",";
            }
            i++;
        }
        String innerMethod = initMethod(object, c, method);
        jsMethod += ") {" + (innerMethod != null ? innerMethod : "") + "}" + (object != null ? "," : "");
        return jsMethod;
    }

    default String generateObject(Object object, String varName) {
        String jsObject = "";
        jsObject += generateDependencies(object);
        String classMethods = "{";
        for (Method method : object.getClass().getMethods()) {
            if (!classMethods.contains(method.getName())) {
                String jsMethod = generateMethod(object, object.getClass(), method);
                classMethods += " " + jsMethod;
            }
        }
        classMethods += "}";
        ;
        jsObject += "var " + varName + " = " + classMethods + ";";
        return jsObject;
    }


    default String generateClass(Class<?> c) {
        String classMethods = "{ constructor() {} ";
        for (Method method : c.getMethods()) {
            if (!classMethods.contains(method.getName())) {
                String jsMethod = generateMethod(null, c, method);
                classMethods += " " + jsMethod;
            }
        }
        classMethods += "}";
        return classMethods;
    }

    default String generateDependencies(Object object) {
        return generateDependencies(object.getClass(), "");
    }

    default String generateDependencies(Class<?> c, String stack) {
        if (!stack.contains("java_lang_Object") && !cachedVars.contains("java_lang_Object")) {
            stack += "class java_lang_Object " + generateClass(Object.class) + ";";
            cachedVars.push("java_lang_Object");
        }
        String cName = c.getName().replaceAll("\\.", "_");
        if (!stack.contains(cName) && !cachedVars.contains(cName)) {
            stack += "class " + cName + " " + generateClass(c) + ";";
            cachedVars.push(cName);
        }
        for (Method method : c.getMethods()) {
            if (!method.getReturnType().isPrimitive() && !method.getReturnType().equals(String.class) && !method.getReturnType().isArray()) {
                String varName = method.getReturnType().getName().replaceAll("\\.", "_");
                String clone = new String(stack);
                clone = clone.replaceAll("new " + varName, "");
                if (!clone.contains(varName) && !cachedVars.contains(varName) && shouldAddMethod(method)) {
                    stack += "class " + varName + " " + generateClass(method.getReturnType()) + ";";
                    stack = generateDependencies(method.getReturnType(), stack);
                    cachedVars.push(varName);
                }
            }
        }
        return stack;
    }

    default boolean shouldAddMethod(Method method) {
        return true;
    }

    default String initMethod(Object object, Class<?> c, Method method) {
        return null;
    }
}
