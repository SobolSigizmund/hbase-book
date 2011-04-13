<%@ page contentType="text/html;charset=UTF-8"
  import="org.apache.hadoop.hbase.client.HTable"
  import="com.hbasebook.hush.ResourceManager"
  import="com.hbasebook.hush.table.ShortUrlTable"
  import="org.apache.hadoop.hbase.client.Put"
%>
<%@ page import="org.apache.hadoop.hbase.util.Bytes" %>
<%
  String url = request.getParameter("url");
  String newShortId = null;
  if (url != null && url.length() > 0) {
    ResourceManager manager = ResourceManager.getInstance();
    HTable table = manager.getTable(ShortUrlTable.NAME);
    byte[] newId = manager.getShortId();
    newShortId = Bytes.toString(newId);
    Put put = new Put(newId);
    put.add(ShortUrlTable.DATA_FAMILY, ShortUrlTable.URL, Bytes.toBytes(url));
    table.put(put);
    table.flushCommits();
    manager.putTable(table);
  }
%>
<html>
<body>
<h2>Welcome to the HBase URL Shortener</h2>

<p>Shorten your URLs! Paste or type a URL into the box below and press the "Shorten" button.</p>

<p>
<form action="index.jsp">
    URL: <input type="text" name="url"> <input type="submit" name="Shorten!">
</form>
</p>
<% if (newShortId != null) { %>
<p>Your new shortened URL is:
  <a href="http://<%=request.getHeader("Host")%>/<%= newShortId%>">http://<%=request.getHeader("Host")%>/<%= newShortId%></a>
</p>
<% } %>
<p>You can track your own URLs by <a href="signup.jsp">signing up</a> or
    <a href="/admin/home.jsp">logging in</a>. </p>
<p/>
<p/>
<p/>
<p>Visit <a href="http://hbasebook.com">HBase Book Online</a> for more information.</p>
</body>
</html>
