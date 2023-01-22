package com.patetlex.jcefnative.ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.patetlex.jcefnative.js.JavaScriptGenerator;
import com.patetlex.jcefnative.util.DataHelper;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class WebPanel extends JPanel implements JavaScriptGenerator {

    private static final Gson gson = new Gson();
    private static final Random rand = new Random();

    private Map<Object, String> objectToMembers = new HashMap<>();
    private List<CefLoadHandler> loadHandlers = new ArrayList<>();
    private Map<Integer, Consumer<Object>> awaitingReturns;
    private Stack<Object> memberLinks;
    private Stack<String> executingCode;
    private CefBrowser browser;
    private boolean isLoaded;

    public WebPanel() {
        this("https://www.google.com/");
    }

    public WebPanel(String defaultURL) {
        this(defaultURL, OS.isLinux());
    }

    protected WebPanel(String defaultURL, boolean useOSR) {
        this.setLayout(new BorderLayout());
        this.awaitingReturns = new HashMap<>();
        this.memberLinks = new Stack<>();
        this.executingCode = new Stack<>();
        CefApp app;
        if (CefApp.getState() == CefApp.CefAppState.NONE || CefApp.getState() == CefApp.CefAppState.NEW) {
            CefSettings settings = new CefSettings();
            settings.windowless_rendering_enabled = useOSR;
            app = CefApp.getInstance(settings);
        } else {
            app = CefApp.getInstance();
        }
        CefClient client = app.createClient();
        CefMessageRouter returnRouter = CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig("cefOutput", "cefCancelOutput"));
        returnRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                if (!request.equalsIgnoreCase("no-output")) {
                    JsonObject param = gson.fromJson(request, JsonObject.class);
                    int foundId = param.get("messageId").getAsInt();
                    int idToRemove = -1;
                    for (int id : WebPanel.this.awaitingReturns.keySet()) {
                        if (id == foundId) {
                            WebPanel.this.awaitingReturns.get(id).accept(gson.fromJson(gson.toJson(param.get("output")), Object.class));
                            idToRemove = id;
                            break;
                        }
                    }
                    if (idToRemove != -1)
                        WebPanel.this.awaitingReturns.remove(idToRemove);
                }
                return super.onQuery(browser, frame, queryId, request, persistent, callback);
            }
        }, true);
        client.addMessageRouter(returnRouter);
        CefMessageRouter objectRouter = CefMessageRouter.create(new CefMessageRouter.CefMessageRouterConfig("objectRoute", "objectCancelRoute"));
        objectRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                JsonObject object = gson.fromJson(request, JsonObject.class);
                try {
                    if (object.get("member") != null) {
                        for (Object member : WebPanel.this.objectToMembers.keySet()) {
                            if (WebPanel.this.objectToMembers.get(member).equalsIgnoreCase(object.get("member").getAsString())) {
                                Method method = new Object() {
                                    public Method getMethod(Class<?> c, String name) {
                                        for (Method method1 : c.getMethods()) {
                                            if (method1.getName().equalsIgnoreCase(name)) {
                                                return method1;
                                            }
                                        }
                                        return null;
                                    }
                                }.getMethod(member.getClass(), object.get("method").getAsString());
                                if (object.get("parameters").isJsonArray()) {
                                    Object[] params = gson.fromJson(object.get("parameters").getAsJsonArray(), Object[].class);
                                    if (WebPanel.class.isAssignableFrom(method.getParameters()[method.getParameters().length - 1].getType())) {
                                        List<Object> newParams = new ArrayList<>();
                                        for (Object p : params) {
                                            newParams.add(p);
                                        }
                                        newParams.add(WebPanel.this);
                                        params = newParams.toArray(new Object[newParams.size()]);
                                    }
                                    Object response = method.invoke(member, params);
                                    callback.success(response != null ? gson.toJson(response) : "");
                                    if (response != null)
                                        WebPanel.this.memberLinks.push(response);
                                } else {
                                    Object response = method.invoke(member);
                                    String jsResponse = gson.toJson(response);
                                    callback.success(response != null && jsResponse != null && !jsResponse.equalsIgnoreCase("null") ? jsResponse : "{}");
                                    if (response != null)
                                        WebPanel.this.memberLinks.push(response);
                                }
                                return true;
                            }
                        }
                    } else {
                        Object addition = null;
                        boolean ret = false;
                        for (Object link : WebPanel.this.memberLinks) {
                            Class<?> c = Class.forName(object.get("class").getAsString());
                            if (c.isAssignableFrom(link.getClass())) {
                                Method method = new Object() {
                                    public Method getMethod(Class<?> c, String name) {
                                        for (Method method1 : c.getMethods()) {
                                            if (method1.getName().equalsIgnoreCase(name)) {
                                                return method1;
                                            }
                                        }
                                        return null;
                                    }
                                }.getMethod(c, object.get("method").getAsString());
                                if (object.get("parameters").isJsonArray()) {
                                    Object[] params = gson.fromJson(object.get("parameters").getAsJsonArray(), Object[].class);
                                    if (WebPanel.class.isAssignableFrom(method.getParameters()[method.getParameters().length - 1].getType())) {
                                        List<Object> newParams = new ArrayList<>();
                                        for (Object p : params) {
                                            newParams.add(p);
                                        }
                                        newParams.add(WebPanel.this);
                                        params = newParams.toArray(new Object[newParams.size()]);
                                    }
                                    Object response = method.invoke(link, params);
                                    callback.success(response != null ? gson.toJson(response) : "");
                                    addition = response;
                                } else {
                                    Object response = method.invoke(link);
                                    String jsResponse = gson.toJson(response);
                                    callback.success(response != null && jsResponse != null && !jsResponse.equalsIgnoreCase("null") ? jsResponse : "{}");
                                    addition = response;
                                }
                                ret = true;
                            }
                            break;
                        }
                        WebPanel.this.memberLinks.pop();
                        if (addition != null)
                            WebPanel.this.memberLinks.push(addition);
                        if (ret)
                            return true;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return super.onQuery(browser, frame, queryId, request, persistent, callback);
            }
        }, true);
        client.addMessageRouter(objectRouter);
        this.browser = client.createBrowser(defaultURL, useOSR, false);
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                super.onLoadEnd(browser, frame, httpStatusCode);
                WebPanel.this.isLoaded = true;
                for (CefLoadHandler loadHandler : loadHandlers) {
                    loadHandler.onLoadEnd(browser, frame, httpStatusCode);
                }
            }
        });
        this.add(this.browser.getUIComponent(), BorderLayout.CENTER);
    }

    public void executeScript(String code) {
        executeScript(code, new Consumer<Object>() {
            @Override
            public void accept(Object o) {

            }
        });
    }

    public void executeScript(String code, Consumer<Object> callback) {
        int id = rand.nextInt();
        while (this.awaitingReturns.keySet().contains(id)) {
            id = rand.nextInt();
        }
        String jsCode = "var _output = (async () => {" + code + "})(); _output.then(cef_output => {window.cefOutput({request: (cef_output != null ? '{\"messageId\":" + id + ", \"output\":' + JSON.stringify(cef_output) + '}' : 'no-output'), persistent: true, onSuccess: function(response) {}, onFailure: function(error_code, error_message) {}});});";
        this.executingCode.push(jsCode);
        jsCode = generateMembers() + jsCode;
        this.browser.executeJavaScript(jsCode, this.browser.getURL(), 1);
        this.awaitingReturns.put(id, callback);
        this.executingCode.pop();
    }

    public String generateMembers() {
        String members = "";
        for (Object member : this.objectToMembers.keySet()) {
            members += generateObject(member, this.objectToMembers.get(member));
        }
        return members;
    }

    protected void loadURL(String url) {
        if (!this.isLoaded) {
            this.removeAll();
            this.browser = this.browser.getClient().createBrowser(url, OS.isLinux(), false);
            this.add(this.browser.getUIComponent(), BorderLayout.CENTER);
            return;
        }
        this.browser.loadURL(url);
    }

    protected void loadHtml(String html) {
        loadURL(DataHelper.createHtmlURL(html));
    }

    public void setMember(String member, Object object) {
        this.objectToMembers.put(object, member);
    }

    protected void addLoadHandler(CefLoadHandler loadHandler) {
        this.loadHandlers.add(loadHandler);
    }

    public CefBrowser getBrowser() {
        return browser;
    }

    @Override
    public String initMethod(Object object, Class<?> c, Method method) {
        if (method.getReturnType().isArray() || method.getReturnType().getTypeParameters().length > 0)
            return null;
        String stringifiedParams = "[";
        int i = 0;
        List<Parameter> params = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            if (!WebPanel.class.isAssignableFrom(parameter.getType())) {
                params.add(parameter);
            }
        }
        for (Parameter parameter : params.toArray(new Parameter[params.size()])) {
            stringifiedParams += parameter.getName();
            if (i + 1 < params.size()) {
                stringifiedParams += ",";
            }
            i++;
        }
        stringifiedParams += "]";
        String varName = method.getReturnType().getName().replaceAll("\\.", "_");
        if (varName.equalsIgnoreCase("boolean")) {
            varName = "Boolean(out)";
        } else if (varName.equalsIgnoreCase("int") || varName.equalsIgnoreCase("double") || varName.equalsIgnoreCase("float")) {
            varName = "Number(out)";
        } else if (varName.equalsIgnoreCase("java_lang_String") || varName.equalsIgnoreCase("char")) {
            varName = "String(out)";
        } else if (!varName.equalsIgnoreCase("void")) {
            varName = "Object.assign(new " + varName + "(), out)";
        }
        String aVoid = varName.equalsIgnoreCase("void") ? "" : "out = " + varName;
        if (object != null) {
            return "var out; await new Promise((r, re) => {window.objectRoute({request: '{ \"class\"=\"" + c.getName() + "\", \"member\"=\"" + this.objectToMembers.get(object) + "\", \"method\"=\"" + method.getName() + "\", \"parameters\"=' + JSON.stringify(" + stringifiedParams + ") + '}', persistent: true, onSuccess: function(response) {out = JSON.parse(response);" + aVoid + "; r(''); }, onFailure: function(error_code, error_message) {}});}); " + (varName.equalsIgnoreCase("void") ? "" : "return out") + ";";
        }
        return "var out; await new Promise((r, re) => {window.objectRoute({request: '{ \"class\"=\"" + c.getName() + "\", \"method\"=\"" + method.getName() + "\", \"parameters\"=' + JSON.stringify(" + stringifiedParams + ") + '}', persistent: true, onSuccess: function(response) {out = JSON.parse(response);" + aVoid + "; r(''); }, onFailure: function(error_code, error_message) {}});}); " + (varName.equalsIgnoreCase("void") ? "" : "return out") + ";";
    }

    @Override
    public boolean shouldAddMethod(Method method) {
        for (String codes : this.executingCode) {
            if (codes.contains(method.getName())) {
                return true;
            }
        }
        return false;
    }
}
