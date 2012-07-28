/**
 * Copyright (C) 2007 Robbie Vanbrabant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.garbagecollected.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates objects that fake a builder object for a given {@link Builder} type
 * based interface.
 * <p>
 * Returned builder objects implement {@link Object#toString()} and
 * forward calls to <code>equals</code> or <code>hashCode</code>
 * to the {@link Object} class' default {@link Object#equals(Object)} and
 * {@link Object#hashCode()} implementations.
 * 
 * @author Robbie Vanbrabant (robbie.vanbrabant@gmail.com)
 */
public class BuilderFactory {  
  @SuppressWarnings("serial")
  private static Map<Class<?>, ?> PRIMITIVE_DEFAULTS = 
    new HashMap<Class<?>, Object>() {{
      put(int.class, 0);
      put(long.class, 0L);
      put(boolean.class, false);
      put(byte.class, 0);
      put(short.class, 0);
      put(float.class, 0.0F);
      put(double.class, 0.0D);
      put(char.class, '\u0000');
    }};

  private final BuilderType type;
  
  public BuilderFactory(BuilderType type) {
    this.type = type;
  }
  
  public BuilderFactory() {
    this.type = BuilderType.GETTER_SETTER;
  }
   
  @SuppressWarnings("unchecked") // java.lang.reflect.Proxy is not generic
  public <T extends Builder<V>, V> T make(Class<T> spec,
                                          BuilderCallback<T, V> callback) {
    return (T) Proxy.newProxyInstance(spec.getClassLoader(),
        new Class[] { spec },
        new BuilderInvocationHandler<T, V>(getSpecification(type, spec), callback));
  }
  
  /**
   * Trades compile time error checking for run time error checking for the
   * sake of code brevity.
   */
  @SuppressWarnings("unchecked") // java.lang.reflect.Proxy is not generic
  public <T extends Builder<V>, V> T makeRisky(final Class<T> spec, final Class<V> target, final Object... constructorArgs) {
    BuilderCallback<T, V> callback = new BuilderCallback<T, V>() {
      public V call(T builder) throws Exception {
        Class<?>[] classes = new Class<?>[constructorArgs.length+1];
        classes[0] = spec;
        for (int i = 0; i < constructorArgs.length; i++) {
          classes[i+1]  = constructorArgs[i].getClass();
        }
        
        Object[] actualArgs = new Object[constructorArgs.length+1];
        actualArgs[0] = builder;
        System.arraycopy(constructorArgs, 0, actualArgs, 1, constructorArgs.length);
        
        Constructor<V> constructor = target.getDeclaredConstructor(classes);
        constructor.setAccessible(true); // Eww..
        try {
          return constructor.newInstance(actualArgs);
        } finally {
          constructor.setAccessible(false);
        }
      }
    };
    return make(spec, callback);
  }
  
  private BuilderSpecification getSpecification(BuilderType type, Class<?> spec) {
    switch(type) {
      case SIMPLE: return new SimpleBuilderSpecification(spec);
      case SIMPLE_SETTER: return new SimpleSetterBuilderSpecification(spec);
      case GETTER_SETTER: return new GetterSetterBuilderSpecification(spec);
    }
    throw new IllegalArgumentException();
  }

  private static class BuilderInvocationHandler<T extends Builder<V>, V>
      implements InvocationHandler {
    private final BuilderSpecification spec;
    private final BuilderCallback<T, V> callback;
    private Map<String, Object> methodsToValues = new HashMap<String, Object>();

    private BuilderInvocationHandler(BuilderSpecification spec,
                                     BuilderCallback<T, V> callback) {
      this.callback = callback;
      this.spec = spec;
    }

    @SuppressWarnings("unchecked") // java.lang.reflect.Proxy is not generic
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
      if (isObjectToString(method)) {
        return toString();
      } else if (isObjectHashCode(method)) {
        return hashCode();
      } else if (isObjectEquals(method)) {
        return equals(args[0]);
      } else if (spec.isWriter(method, args)) {
        methodsToValues.put(spec.writerIdentity(method), args[0]);
        return proxy;
      } else if (isBuilder(method)) {
        return callback.call((T) proxy);
      } else if (spec.isReader(method, args)) {
        Object value = methodsToValues.get(spec.readerIdentity(method));
        if (value == null && method.getReturnType().isPrimitive()) {
          return PRIMITIVE_DEFAULTS.get(method.getReturnType());
        }
        return value;
      } else {
        throw new IllegalStateException(String.format(
            "method '%s' is not a reader or a writer", method.getName()));
      }
    }

    private boolean isBuilder(Method method) {
      return method.equals(Builder.class.getMethods()[0]);
    }
    
    private boolean isObjectToString(Method method) {
      return "toString".equals(method.getName()) 
             && method.getParameterTypes().length == 0;
    }
    
    private boolean isObjectHashCode(Method method) {
      return "hashCode".equals(method.getName()) 
             && method.getParameterTypes().length == 0;
    }
    
    private boolean isObjectEquals(Method method) {
      return "equals".equals(method.getName()) 
             && method.getParameterTypes().length == 1
             && method.getParameterTypes()[0].equals(Object.class);
    }
    
    public boolean equals(Object that) {
      if (that == null) return false;
      if (Proxy.isProxyClass(that.getClass()))
        return super.equals(Proxy.getInvocationHandler(that));
      return false;
    }
    
    public int hashCode() {
      return super.hashCode();
    }
    
    public String toString() {
      return methodsToValues.toString();
    }
  }
}