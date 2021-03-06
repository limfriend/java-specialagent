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

package io.opentracing.contrib.specialagent.rule.kafka.client;

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

public class KafkaAgentRule extends AgentRule {
  @Override
  public Iterable<? extends AgentBuilder> buildAgent(final AgentBuilder builder) {
    return Arrays.asList(builder
      .type(named("org.apache.kafka.clients.consumer.internals.ConsumerInterceptors"))
      .transform(new Transformer() {
        @Override
        public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder.visit(Advice.to(Consumer.class).on(named("onConsume")));
        }})
      .type(named("org.apache.kafka.clients.producer.KafkaProducer"))
      .transform(new Transformer() {
        @Override
        public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder.visit(Advice.to(Producer.class).on(named("send").and(takesArguments(2))));
        }}));
  }

  public static class Consumer {
    @Advice.OnMethodExit
    public static void exit(final @Advice.Origin String origin, final @Advice.Return(typing = Typing.DYNAMIC) Object returned) {
      if (isEnabled(KafkaAgentRule.class.getName(), origin))
        KafkaAgentIntercept.onConsumerExit(returned);
    }
  }

  public static class Producer {
    @Advice.OnMethodEnter
    public static void enter(final @Advice.Origin String origin, final @Advice.Argument(value = 0) Object record, @Advice.Argument(value = 1, readOnly = false, typing = Typing.DYNAMIC) Object callback) {
      if (isEnabled(KafkaAgentRule.class.getName(), origin))
        callback = KafkaAgentIntercept.onProducerEnter(record, callback);
    }
  }
}