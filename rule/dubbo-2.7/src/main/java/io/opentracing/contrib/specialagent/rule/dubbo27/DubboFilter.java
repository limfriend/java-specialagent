package io.opentracing.contrib.specialagent.rule.dubbo27;


import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.specialagent.rule.DubboUtils;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.support.RpcUtils;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;

public class DubboFilter implements Filter {

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        if (!GlobalTracer.isRegistered()) {
            return invoker.invoke(invocation);
        }
        RpcContext rpcContext = RpcContext.getContext();
        Tracer tracer = GlobalTracer.get();
        String service = invoker.getInterface().getSimpleName();
        String name = service + "/" + RpcUtils.getMethodName(invocation);
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(name);
        Span span = null;
        if (rpcContext.isProviderSide()) {
            spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
            spanBuilder.withTag(Tags.COMPONENT.getKey(), "java-dubbo");
            SpanContext spanContext = tracer.extract(Format.Builtin.TEXT_MAP, new DubboFilter.DubboAdapter(rpcContext));
            if (spanContext != null) {
                spanBuilder.asChildOf(spanContext);
            }
            span = spanBuilder.start();
        } else {
            spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
            spanBuilder.withTag(Tags.COMPONENT.getKey(), "java-dubbo");
            span = spanBuilder.start();
            tracer.inject(span.context(), Format.Builtin.TEXT_MAP, new DubboFilter.DubboAdapter(rpcContext));
        }
        InetSocketAddress remoteAddress = rpcContext.getRemoteAddress();
        if (remoteAddress != null) {
            span.setTag("remoteAddress", remoteAddress.getHostString() + ":" + remoteAddress.getPort());
        }

        try (Scope scope = tracer.scopeManager().activate(span)) {
            Result result = invoker.invoke(invocation);
            if (result.hasException()) {
                Throwable t = result.getException();
                DubboUtils.errorLogs(t, span);
                if (t instanceof RpcException) {
                    span.setTag("dubbo.error_code", Integer.toString(((RpcException) t).getCode()));
                }
            }
            return result;
        } catch (Throwable t) {
            DubboUtils.errorLogs(t, span);
            if (t instanceof RpcException) {
                span.setTag("dubbo.error_code", Integer.toString(((RpcException) t).getCode()));
            }
            throw t;
        } finally {
            span.finish();
        }
    }

    class DubboAdapter implements TextMap {

        private RpcContext rpcContext;

        public DubboAdapter(RpcContext rpcContext) {
            this.rpcContext = rpcContext;
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return this.rpcContext.getAttachments().entrySet().iterator();
        }

        @Override
        public void put(String key, String value) {
            rpcContext.getAttachments().put(key, value);
        }
    }

}
