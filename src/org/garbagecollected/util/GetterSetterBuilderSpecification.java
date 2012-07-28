/*
 * Copyright (C) 2008 Robbie Vanbrabant
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

import java.lang.reflect.Method;

public class GetterSetterBuilderSpecification implements BuilderSpecification {
  private final Class<?> spec;

  GetterSetterBuilderSpecification(Class<?> spec) {
    this.spec = spec;
  }
  
  public boolean isWriter(Method method, Object[] args) {
    return (args != null && args.length == 1 
            && method.getName().startsWith("set")
            && spec.isAssignableFrom(method.getReturnType()));
  }

  public boolean isReader(Method method, Object[] args) {
    return args == null
        && method.getName().startsWith("get")
        && !Void.TYPE.isAssignableFrom(method.getReturnType());
  }

  public String readerIdentity(Method reader) {
    return reader.getName().substring(3, reader.getName().length());
  }

  public String writerIdentity(Method writer) {
    return writer.getName().substring(3, writer.getName().length());
  }
}
