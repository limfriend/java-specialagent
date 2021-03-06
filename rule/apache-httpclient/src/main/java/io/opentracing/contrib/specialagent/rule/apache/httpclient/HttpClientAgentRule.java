/* Copyright 2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentracing.contrib.specialagent.rule.apache.httpclient;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.util.Arrays;

import io.opentracing.contrib.specialagent.AgentRule;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.utility.JavaModule;

public class HttpClientAgentRule extends AgentRule {
  @Override
  public Iterable<? extends AgentBuilder> buildAgent(final AgentBuilder builder) {
    return Arrays.asList(builder
      .type(not(isInterface()).and(hasSuperType(named("org.apache.http.client.HttpClient"))))
      .transform(new Transformer() {
        @Override
        public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder
            .visit(Advice.to(HttpClientAgentRule.class).on(named("execute")))
            .visit(Advice.to(OnException.class).on(named("execute")));
        }}));
  }

  @Advice.OnMethodEnter
  public static void enter(final @Advice.Origin String origin, final @Advice.Argument(value = 0) Object arg0, @Advice.Argument(value = 1, optional = true, readOnly = false, typing = Typing.DYNAMIC) Object arg1, @Advice.Argument(value = 2, optional = true, readOnly = false, typing = Typing.DYNAMIC) Object arg2) {
    if (!isEnabled(HttpClientAgentRule.class.getName(), origin))
      return;

    final Object[] objects = HttpClientAgentIntercept.enter(arg0, arg1, arg2);
    if (objects == null)
      return;

    if (objects.length == 1)
      arg1 = objects[0];
    else if (objects.length == 2)
      arg2 = objects[1];
  }

  @Advice.OnMethodExit
  public static void exit(final @Advice.Origin String origin, final @Advice.Return Object returned) {
    if (isEnabled(HttpClientAgentRule.class.getName(), origin))
      HttpClientAgentIntercept.exit(returned);
  }

  public static class OnException {
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(final @Advice.Thrown Throwable thrown) {
      if (thrown != null)
        HttpClientAgentIntercept.onError(thrown);
    }
  }
}