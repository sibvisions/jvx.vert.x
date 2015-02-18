Overview
========

A connector for JVx, implemented with vert.x 

Usage
=====

### NetSocket

**Start server**

```java
NetSocketServer server = new NetSocketServer();
server.setPort(8888);
server.setInterface("127.0.0.1");
server.start();
```

**Call an action**

```java
IConnection con = new NetSocketConnection("127.0.0.1", 8888);

MasterConnection appcon = new MasterConnection(con);
appcon.setApplicationName("application");
appcon.setUserName("username");
appcon.setPassword("password");
appcon.open();

appcon.callAction("startWorkFlow", new Integer(5));

appcon.close();
```

### Http

**Start server**

```java
server = new HttpServer();
server.setPort(8080);
server.start();
```

**Call an action**

```java
String url = "http://localhost:8080/services/";

IConnection con = new HttpConnection(url + "Server");
con.setUploadURL(url + "Upload");
con.setDownloadURL(url + "Download");

MasterConnection appcon = new MasterConnection(con);
appcon.setApplicationName("application");
appcon.setUserName("username");
appcon.setPassword("password");
appcon.open();

appcon.callAction("startWorkFlow", new Integer(5));

appcon.close();
```
 