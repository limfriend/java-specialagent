# SpecialAgent Rule for Java Servlet API

**Rule Name:** `servlet`

## Configuration

Following properties are supported by the Servlet Rule.

### Properties

* `-Dsa.instrumentation.plugin.servlet.skipPattern=PATTERN`

  Skip tracing on the url matching specified regex `PATTERN`.

  **Example:**

  `/health`

* `-Dsa.instrumentation.plugin.servlet.spanDecorators`

  Override default servlet span decorator, with customized decorator class names, comma separated. The customized decorators must be subclasses of `io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator`.

  **Example:**

  `com.company.my.project.MySpanDecorator1,com.company.my.project.MySpanDecorator2,io.opentracing.contrib.web.servlet.filter.StandardTagsServletFilterSpanDecorator`

* `-Dsa.instrumentation.plugin.servlet.spanDecorators.classpath`

  Indicate the casspath of JARs or directories containing customized decorator classes specified by `sa.instrumentation.plugin.servlet.spanDecorators`, delimited by `File.pathSeparatorChar`.

  **Example:**

  `/path/to/your/lib/myspandecorators1.jar:/path/to/your/lib/myspandecorators1.jar`

* `-Dsa.httpHeaderTags=a,b=c`

  Creates the following tags from HTTP headers:

  | HTTP header | tag |
  |:-|:-|
  | a | http.header.a |
  | b | c |